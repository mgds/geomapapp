package haxby.worldwind.db.xmcs;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.IconLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.UserFacingIcon;
import gov.nasa.worldwind.view.orbit.OrbitView;
import haxby.db.Database;
import haxby.db.mcs.CDP;
import haxby.db.xmcs.XMCS;
import haxby.db.xmcs.XMCruise;
import haxby.db.xmcs.XMImage;
import haxby.db.xmcs.XMLine;
import haxby.dig.Digitizer;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.worldwind.WWLayer;
import haxby.worldwind.WWOverlay;
import haxby.worldwind.fence.FenceDiagram;
import haxby.worldwind.layers.LayerSet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class WWXMCS extends XMCS implements WWOverlay, SelectListener {

	protected static final double FD_HEIGHT_PER_SECOND = 2000;

	protected WorldWindow ww;

	protected LayerSet layerSet;
	protected WWLayer layer;
	protected RenderableLayer tracklayer;
	protected IconLayer iconLayer;

	protected List<Polyline> cruiseLines = new LinkedList<Polyline>();
	protected Map<XMLine, FenceDiagram> fdS = new HashMap<XMLine, FenceDiagram>();
	protected JScrollPane fences;

	private boolean fdEnabled = true;
	private Float baseSeconds = 10f;
	private double exageration = 1;
	private float linePercent = 0;

	private JSlider opacitySlider;
	private JLabel secondsLabel;

	public WWXMCS(WorldWindow ww, XMap map) {
		super(map);
		this.ww = ww;
	}

	@Override
	protected XMImage createXMImage(Digitizer dig) {
		return new WWXMImage(dig);
	}

	@Override
	public boolean loadDB() {
		if (!super.loadDB())
			return false;

		layerSet = new LayerSet();
		layerSet.setName(getDBName());

		layer = new WWLayer(layerSet) {
			@Override
			public Database getDB() {
				return WWXMCS.this;
			}

			@Override
			public void close() {
				((MapApp)map.getApp()).closeDB(WWXMCS.this);
			}
		};

		tracklayer = new RenderableLayer();
		makeCruiseAreas();
		
		iconLayer = new IconLayer();
		
		layerSet.add(tracklayer);
		layerSet.add(iconLayer);

		ww.getInputHandler().addMouseListener(this);

		makeTrackLines();
		updateSelectedTrack();
		return true;
	}

	@Override
	public JComponent getSelectionDialog() {
		if( !initiallized )return null;
		if(panel!=null) {
			return panel;
		}
		JPanel panel = (JPanel) super.getSelectionDialog();
		JPanel main = new JPanel();
		main.setLayout(new BoxLayout(main, BoxLayout.PAGE_AXIS));
		JPanel p1 = new JPanel(new BorderLayout());

	// Add Fence Diagram border
		Font plain = new Font("Arial", Font.PLAIN, 11);
		TitledBorder titledBorder = BorderFactory.createTitledBorder("Fence Diagrams");
		titledBorder.setTitleFont(plain);
		main.setBorder(titledBorder);
		main.setPreferredSize(new Dimension(115, 200));

	// Add Enable check box
		final JCheckBox enabled = new JCheckBox("Enabled", true);
		enabled.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setFenceDiagramsEnabled(enabled.isSelected());
				layerSet.firePropertyChange(AVKey.LAYER, null, layerSet);
			}
		});
		enabled.setFont(plain);
		p1.add(enabled,BorderLayout.WEST);
		main.add(p1);

	// Add Vertical Exaggeration Section
		p1 = new JPanel(new BorderLayout());
		JLabel label = new JLabel("VE:");
		label.setFont(plain);
		p1.add(label, BorderLayout.WEST);

		final JSlider ve = new JSlider(1,1000);
		ve.setValue(500);
		ve.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				double d = ve.getValue() / 500.0;
				d = Math.pow(.25, 1 - d);
				setExageration(d);
			}
		});
		p1.add(ve);
		main.add(p1);
		ve.setPreferredSize(new Dimension(100, ve.getPreferredSize().height));

	// Add Fence Opacity Section
		p1 = new JPanel(new BorderLayout());
		label = new JLabel("Fence Opacity:");
		label.setFont(plain);
		p1.add(label, BorderLayout.WEST);
		opacitySlider = new JSlider(0,100);
		opacitySlider.setValue(100);
		opacitySlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (currentLine == null) return;

				FenceDiagram fd = fdS.get(currentLine);
				if (fd == null) return;

				float opacity = opacitySlider.getValue() / 100f;

				if (opacity != 1) {
					for (FenceDiagram fd2 : fdS.values())
						fd2.setOpacity(1);

					tracklayer.removeRenderable(fd);
					tracklayer.addRenderable(fd);
				}
				fd.setOpacity(opacity);
				layerSet.firePropertyChange(AVKey.LAYER, null, layerSet);
			}
		});

		p1.add(opacitySlider);
		main.add(p1);
		opacitySlider.setPreferredSize(new Dimension(100, opacitySlider.getPreferredSize().height));

	// Add Seconds Section
		p1 = new JPanel(new BorderLayout());
		secondsLabel = new JLabel("Seconds :");
		secondsLabel.setFont(plain);
		p1.add(secondsLabel, BorderLayout.WEST);

		final JSlider querySlider = new JSlider(0,100);
		querySlider.setValue(0);
		querySlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				float percent = querySlider.getValue() / 100f;
				setLinePercent(percent);
				layerSet.firePropertyChange(AVKey.LAYER, null, layerSet);
			}
		});

		querySlider.setPreferredSize(new Dimension(100, querySlider.getPreferredSize().height));
		p1.add(querySlider);
		main.add(p1);

	// Add Base Seconds Section
		p1 = new JPanel(new BorderLayout());
		label = new JLabel("Base Seconds");
		label.setFont(plain);
		p1.add(label, BorderLayout.WEST);

		final JComboBox baseSeconds = new JComboBox(new Float[] {.025f,.050f,.075f,.1f,.5f,1f,2f,3f,4f,5f,8f,10f,12f});
		baseSeconds.setSelectedIndex(6);
		setBaseSeconds((Float) baseSeconds.getSelectedItem());
		baseSeconds.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setBaseSeconds((Float) baseSeconds.getSelectedItem());
			}
		});
		p1.add(baseSeconds);
		main.add(p1);

	// Add Close Line Button
		p1 = new JPanel(new BorderLayout());
		JButton closeDiagram = new JButton("Close Line");
		closeDiagram.setFont(plain);
		closeDiagram.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (currentLine == null) return;
				FenceDiagram fd = fdS.get(currentLine);
				if (fd == null) return;

				fd.dispose();
				fdS.remove(currentLine);
				tracklayer.removeRenderable(fd);
				layerSet.firePropertyChange(AVKey.LAYER, null, layerSet);
			}
		});
		p1.add(closeDiagram,BorderLayout.WEST);
		main.add(p1);

		// Return Panel
		fences = new JScrollPane(main, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		panel.add(fences, "South");
		panel.setPreferredSize(new Dimension(200, 500));
		return panel;
		}

	protected void setLinePercent(float percent) {
		this.linePercent = percent;

		float seconds = baseSeconds * linePercent;
		NumberFormat fmt = NumberFormat.getNumberInstance();
		fmt.setMaximumFractionDigits(1);
		fmt.setMinimumFractionDigits(1);
		secondsLabel.setText(fmt.format(seconds) + " seconds: ");

		for (Entry<XMLine, FenceDiagram> entry : fdS.entrySet()) {
			double startT = entry.getKey().getZRange()[0] / 1000;
			double p = 1 - (seconds - startT) / (baseSeconds - startT);

			entry.getValue().setDrawLineAt((float) p);
		}
		
		layerSet.firePropertyChange(AVKey.LAYER, null, layerSet);
	}

	protected void setExageration(double d) {
		this.exageration = d;
		for (FenceDiagram fd : fdS.values())
			fd.setExageration(exageration);	
		
		layerSet.firePropertyChange(AVKey.LAYER, null, layerSet);
	}

	protected void setBaseSeconds(float baseSeconds) {
		this.baseSeconds = baseSeconds;
		
		for (Entry<XMLine, FenceDiagram> entry : fdS.entrySet())
		{
			double[] tRange = entry.getKey().getZRange();
			float yMax = (float) ((baseSeconds * 1000 - tRange[0]) / (tRange[1] - tRange[0]));
			entry.getValue().setYMax(yMax);
		}
		
		setLinePercent(linePercent);
		
		layerSet.firePropertyChange(AVKey.LAYER, null, layerSet);
	}

	protected void setFenceDiagramsEnabled(boolean fdEnabled) {
		this.fdEnabled  = fdEnabled;
		
		if (!fdEnabled) {
			for (FenceDiagram fd : fdS.values()) {
				fd.dispose();
				tracklayer.removeRenderable(fd);
			}
			fdS.clear();
		}
		
		layerSet.firePropertyChange(AVKey.LAYER, null, layerSet);
	}

	@Override
	protected void selectDataSource(Object source) {
		super.selectDataSource(source);
		
		tracklayer.removeAllRenderables();
		makeCruiseAreas();
		
		
		makeTrackLines();
		updateSelectedTrack();
	}
	@Override
	public void disposeDB() {
		super.disposeDB();
		
		tracklayer.removeAllRenderables();
		cruiseLines.clear();
		ww.getInputHandler().removeMouseListener(this);
	}

	@Override
	public void setEnabled(boolean tf) {
		if( tf && enabled) return;
		enabled = tf;
		if(enabled) {
			ww.getInputHandler().addMouseListener(this);
			map.addMouseListener( this);
		} else {
			ww.getInputHandler().removeMouseListener(this);
			map.removeMouseListener( this);
		}
	}

	protected void makeCruiseAreas() {
		for (XMCruise cruise : cruises)
		{
			Rectangle2D bounds = cruise.getBounds();
			double x0 = bounds.getMinX();
			double x1 = bounds.getMaxX();
			double y0 = bounds.getMinY();
			double y1 = bounds.getMaxY();
			
			double[] points = new double[] { x0, y0, x0, y1, x1, y1, x1, y0 };

			List<LatLon> pos = new LinkedList<LatLon>();
			for (int i = 0; i < 10; i += 2) {
				pos.add(
					LatLon.fromDegrees(
							points[(i + 1) % 8],
							points[i % 8] ));
			}

			Polyline line = new Polyline(pos, 0);
			line.setFollowTerrain( true );
			line.setLineWidth(2);
			tracklayer.addRenderable(line);
		}
	}

	public Layer getLayer() {
		return layer;
	}

	public SelectListener getSelectListener() {
		return this;
	}

	public void setArea(Rectangle2D bounds) {
	}

	public void selected(SelectEvent evt) {
		if (evt.getEventAction().equals(SelectEvent.LEFT_CLICK))
		{
			PickedObjectList pol = evt.getObjects();
			for (PickedObject po : pol)
			{
				Object obj = po.getObject();
				if (obj instanceof Polyline) {
					int i = cruiseLines.indexOf(
							obj);
					if (i == -1) continue;

					lineList.setSelectedIndex(0);
					lineList.setSelectedItem(currentCruise.getLines()[i]);
					return;
				} else if (obj instanceof FenceDiagram) {
					// Get the associated XMLine
					for (Entry<XMLine, FenceDiagram> entry : fdS.entrySet())
					{
						if (entry.getValue() != obj) continue;
						
						lineList.setSelectedItem(entry.getKey());
						return;
					}
				}
			}
		}
	}

	@Override
	public void mouseClicked(MouseEvent evt) {
		if(evt.isControlDown())return;

		OrbitView view = (OrbitView) ww.getView();

		Position pos =
			view.computePositionFromScreenPoint(evt.getX(), evt.getY());

		if (pos == null) return;

		double x = pos.getLongitude().degrees;
		double y = pos.getLatitude().degrees;

		if (currentCruise != null && 
				currentCruise.getBounds().contains(x, y)) {
			// Do nothing
		} else {
			// Select Area
			int k = cruiseList.getSelectedIndex();
			k=k%cruises.length;
			int k0 = k;
			while(true)  {	
				if(cruises[k].getBounds().contains(x, y) ) {
					mouseE = true;
					cruiseList.setSelectedItem(cruises[k]);
					evt.consume();
//					setSelectedCruise(cruises[k]);
					return;
				}
				k = (k+1)%cruises.length;
				if(k==k0)
					break;
			} 
		}
	}

	protected void setSelectedCruise(XMCruise cruise, String path) {
		super.setSelectedCruise(cruise, path);

		makeTrackLines();
	}

	@Override
	protected void setSelectedCruise(XMCruise cruise) {
		super.setSelectedCruise(cruise);

		makeTrackLines();
	}

	protected void makeTrackLines() {
		for (Polyline line : cruiseLines)
			tracklayer.removeRenderable(line);
		for (FenceDiagram fd : fdS.values()) {
			tracklayer.removeRenderable(fd);
			fd.dispose();
		}
		cruiseLines.clear();
		fdS.clear();
		
		if (currentCruise == null) return;
		
		XMLine[] lines = currentCruise.getLines();
		
		for (XMLine track : lines) {
			Point2D[] points = track.getPoints();

			List<LatLon> pos = new LinkedList<LatLon>();

			for (Point2D point : points)
				pos.add(
					LatLon.fromDegrees(
						point.getY(), point.getX()));

			Polyline line = new Polyline(pos, 0);
			line.setLineWidth(2);
			line.setFollowTerrain(true);
			line.setColor(Color.black);

			cruiseLines.add(line);
			tracklayer.addRenderable(line);
		}
		layerSet.firePropertyChange(AVKey.LAYER, null, layerSet);
	}

	@Override
	protected void setSelectedLine(XMLine line) {
		super.setSelectedLine(line);

		updateSelectedTrack();
	}
	
	protected void updateSelectedTrack() {
		if (currentCruise == null) return;

		XMLine[] lines = currentCruise.getLines();

		int i = 0;
		for (Polyline line : cruiseLines) {
			if (currentLine == lines[i++]) {
				line.setColor( Color.white );

				makeFenceDiagram(line);
			}
			else
				line.setColor( Color.black );
		}

		FenceDiagram fd = fdS.get(currentLine);
		if (fd != null) 
			opacitySlider.setValue((int) (fd.getOpacity() * 100));

		layerSet.firePropertyChange(AVKey.LAYER, null, layerSet);
	}

	private void makeFenceDiagram(final Polyline line) {
		if (!fdEnabled || fdS.containsKey(currentLine)) return;

		List<LatLon> pos = new LinkedList<LatLon>();
		for (Position latlon : line.getPositions())
			pos.add(latlon);

		final FenceDiagram fd = new FenceDiagram(ww, pos, 0, exageration);
		fd.setOpacity(1);
		fdS.put(currentLine, fd);

		Runnable r = new Runnable() {
			public void run() {
				try {
					System.out.println("Retriving Image");
					BufferedImage img = XMImage.getImage(currentLine);
					fd.setImageSource(img);

					double[] tRange = currentLine.getZRange();
					float yMax = (float) ((baseSeconds * 1000 - tRange[0]) / (tRange[1] - tRange[0]));
					fd.setYMax(yMax);

					double dT = (tRange[1] - tRange[0]) / 1000;

					fd.setBaseHeight(dT * FD_HEIGHT_PER_SECOND);

					System.out.println(tRange[0] + "\t" + tRange[1] + "\t" + img.getHeight() + "\t" + currentLine);

					if (fdEnabled) {
						tracklayer.addRenderable(fd);
						setLinePercent(linePercent); 
					} else 
						fd.dispose();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};

		if (map.getApp() instanceof MapApp)
			((MapApp)map.getApp()).
				addProcessingTask("Making MCS Fence Diagram: " + currentLine.getID(), r);
		else
			new Thread(r).start();
	}

	@Override
	protected void zoomToCruise() {
		Rectangle2D bounds = currentCruise.getBounds();
		map.zoomToWESN(new double[] {bounds.getMinX(), 
				bounds.getMaxX(),
				bounds.getMinY(), 
				bounds.getMaxY()});
	}

	private class WWXMImage extends XMImage {

		private Polyline currentSegment;
		private UserFacingIcon icon;
		public WWXMImage(Digitizer inputDig) {
			super(inputDig);

			icon = new UserFacingIcon("org/geomapapp/resources/icons/wdot.png", Position.fromDegrees(0, 0, 0));
			icon.setSize( new Dimension(16,16));
			icon.setVisible(true);
			currentSegment = new Polyline();
			currentSegment.setLineWidth(4);
			currentSegment.setColor(Color.red);
			currentSegment.setFollowTerrain(true);
		}

		public void drawVisibleSeg() {
			tracklayer.removeRenderable( currentSegment );

			if( !isVisible() || line==null || image==null ) {
				layerSet.firePropertyChange(AVKey.LAYER, null, layerSet);
				return;
			}
			int[] seg = getVisibleSeg();
			CDP[] cdp = line.getCDP();
			Point2D[] points = line.getPoints();

			List<LatLon> pos = new LinkedList<LatLon>();

			Point2D p = line.pointAtCDP( seg[0] );
			pos.add(LatLon.fromDegrees(p.getY(), p.getX()));
			int k=0;
			while( k<cdp.length-1 && cdp[k].number() < seg[0]) k++;
			while( k<cdp.length-1 && cdp[k].number() < seg[1] ) {
				pos.add(LatLon.fromDegrees(points[k].getY(), points[k].getX()));
				k++;
			}
			p = line.pointAtCDP( seg[1] );
			pos.add(LatLon.fromDegrees(p.getY(), p.getX()));

			currentSegment.setPositions(pos, 10);
			tracklayer.addRenderable(currentSegment);

			layerSet.firePropertyChange(AVKey.LAYER, null, layerSet);
		}

		public void mouseMoved(MouseEvent e) {
			super.mouseMoved(e);

			iconLayer.removeIcon(icon);
			
			int cdpN = cdpAt( e.getX() );
			if (cdpN == -1) {
				layerSet.firePropertyChange(AVKey.LAYER, null, layerSet);
				return;
			}

			Point2D p = line.pointAtCDP( cdpN );
			icon.setPosition(Position.fromDegrees(p.getY(), p.getX(), 0));

			iconLayer.addIcon(icon);
			layerSet.firePropertyChange(AVKey.LAYER, null, layerSet);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			super.mouseExited(e);

			iconLayer.removeIcon(icon);
			layerSet.firePropertyChange(AVKey.LAYER, null, layerSet);
		}

		@Override
		protected void closeLine() {
			super.closeLine();

			tracklayer.removeRenderable(currentSegment);
			currentSegment = null;
			iconLayer.removeIcon(icon);
			icon = null;
			layerSet.firePropertyChange(AVKey.LAYER, null, layerSet);
		}
	}
}
package haxby.worldwind.db.fms;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import haxby.db.Database;
import haxby.db.fms.Earthquakes;
import haxby.db.fms.Earthquakes.Earthquake;
import haxby.db.fms.FocalMechanismSolutionDB.EarthquakeItem;
import haxby.map.XMap;
import haxby.proj.IdentityProjection;
import haxby.proj.PolarStereo;
import haxby.proj.Projection;
import haxby.worldwind.WWOverlay;
import haxby.worldwind.awt.LassoSelectionHandler;
import haxby.worldwind.awt.LassoSelectionHandler.LassoSelectListener;
import haxby.worldwind.layers.FMSLayer;
import haxby.worldwind.layers.LayerSet;
import haxby.worldwind.layers.WWSceneGraph;
import haxby.worldwind.layers.DetailedIconLayer.DetailedIcon;
import haxby.worldwind.layers.WWSceneGraph.SceneItemIcon;
import haxby.worldwind.renderers.DetailedIconRenderer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.geomapapp.util.Cursors;
import org.geomapapp.util.Icons;

public class WWFocalMechanismSolutionDB implements 
	Database, 
	WWOverlay, 
	SelectListener,
	LassoSelectListener {

	private static final String ICON_PATH = "org/geomapapp/resources/icons/wdot.png";
	private static final int ICON_SIZE = 12;

	protected XMap map;

	protected boolean isEnabled;
	protected boolean isExtruded = false;

	protected JPanel selectionPanel;
	protected JToggleButton lassoTB;
	protected JCheckBox plotEQ = new JCheckBox("Plot EQs", true);

	protected List<Earthquake> all;// = new ArrayList<Earthquake>();
	protected List<DetailedIcon> icons = new ArrayList<DetailedIcon>();
	protected List<Integer> selection = new ArrayList<Integer>();
	protected List<Integer> displayedFMS = new ArrayList<Integer>();

	protected LayerSet layer = new LayerSet();
	protected WWSceneGraph iconLayer = new WWSceneGraph();
	protected FMSLayer fmsLayer = new FMSLayer();
	protected DetailedIconRenderer iconRenderer = new DetailedIconRenderer();

	protected JTextArea dataTextArea;
	protected JPanel dataDisplay;
	protected int pickedItem = -1;

	protected LassoSelectionHandler lassoSelectionHandler;
	{
		layer.add(iconLayer);
		layer.add(fmsLayer);

		dataDisplay = new JPanel(new BorderLayout());
		dataDisplay.setBorder( BorderFactory.createEmptyBorder(1, 1, 1, 1));

		dataTextArea = new JTextArea(" ");
		dataTextArea.setEditable(false);
		dataTextArea.setRows(2);
		dataTextArea.setBorder( BorderFactory.createEmptyBorder(1, 4, 1, 1));
		dataTextArea.setFont(
				dataTextArea.getFont().deriveFont(10f));
		dataDisplay.add(dataTextArea);
	}

	public WWFocalMechanismSolutionDB(XMap map) {
		this.map = map;

		layer.setName(getDBName());
		iconRenderer.setExtruded(isExtruded);
		fmsLayer.setExtruded(isExtruded);
	}

	public void disposeDB() {
		Earthquakes.dispose();
		all.clear();
		all = null;

		iconLayer.dispose();
		fmsLayer.dispose();

		icons.clear();
		selection.clear();
		displayedFMS.clear();
	}

	public void draw(Graphics2D g) {
	}

	public JComponent getDataDisplay() {
		return dataDisplay;
	}

	public String getDBName() {
		return "Earthquake Focal Mechanism Solutions";
	}

	public String getDescription() {
		return getDBName();
	}

	public Layer getLayer() {
		return layer;
	}

	public JComponent getSelectionDialog() {
		if (selectionPanel == null)
			initSelectionPanel();
		return selectionPanel;
	}

	private void initSelectionPanel() {
		JCheckBox depthExtrude = new JCheckBox("Extrude", isExtruded);
		depthExtrude.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isExtruded = !isExtruded;
				processExtrude();
			}
		});

		JButton showButton = new JButton("Show FMS");
		showButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fmsLayer.clearSolutions();
				displayedFMS.clear();

				for (int index : selection) {
					Earthquake eq = all.get(index);
					fmsLayer.addFMS(eq);

					displayedFMS.add(index);
				}

				selection.clear();

				processVisibility();
				processSelection();
			}
		});

		plotEQ.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				processVisibility();
			}
		});

		JPanel selectionPanel = new JPanel(new GridLayout(0,1));
		selectionPanel.add( createLassoPanel() );
		selectionPanel.add(depthExtrude);
		selectionPanel.add( plotEQ );
//		selectionPanel.add(showButton);

		this.selectionPanel = new JPanel();
		this.selectionPanel.add(selectionPanel);
	}

	protected JPanel createLassoPanel() {
		JPanel p = new JPanel(new BorderLayout());
		lassoTB = new JToggleButton(Icons.getIcon(Icons.LASSO, false));
		lassoTB.setSelectedIcon(Icons.getIcon(Icons.LASSO, true));
//		tb.setSelected(true);
		lassoTB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
//				((AbstractButton)e.getSource()).setSelected(true);
				if (((AbstractButton)e.getSource()).isSelected()) {
					map.setCursor(Cursors.getCursor(Cursors.LASSO));
				} else
					map.setCursor(Cursor.getDefaultCursor());
			}
		});
		lassoTB.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 0));
		p.add(lassoTB, BorderLayout.WEST);
		p.setBorder(null);
		JLabel l = new JLabel(" Lasso");
		l.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
		p.add(l);

		lassoTB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateLassoState();
			}
		});
		return p;
	}

	protected void updateLassoState() {
		if (lassoSelectionHandler != null)
			if (!isEnabled())
				lassoSelectionHandler.setLassoEnabled(false);
			else
				lassoSelectionHandler.setLassoEnabled(lassoTB.isSelected());
	}

	public SelectListener getSelectListener() {
		return this;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public boolean isLoaded() {
		return all != null && !all.isEmpty();
	}

	public void unloadDB() {
		all = null;
	}
	
	public boolean loadDB() {
		if (isLoaded() && Earthquakes.isLoaded()) return true;

		if (!Earthquakes.load()) return false;

		all = Earthquakes.getEQs();

		updateLayer();

		return true;
	}

	protected void updateLayer() {
		iconLayer.disposeSubLayer(0);

		for (Earthquake eq : all) {
			DetailedIcon icon = new DetailedIcon(ICON_PATH, Position.fromDegrees(eq.lat, eq.lon, 0));

			icon.setSize(new Dimension(ICON_SIZE, ICON_SIZE));
			icon.setHighlightScale(2);
			icon.setVisible(false);
			icon.setIconElevation((int) eq.depth);
			icon.setIconColor(Color.GREEN);

			iconLayer.addItem(
					new SceneItemIcon(icon, iconRenderer));

			icons.add(icon);
		}
		processVisibility();
	}

	public void processVisibility() {
		for (DetailedIcon icon : icons) {
			icon.setVisible(isEnabled && plotEQ.isSelected());
		}

		if (!isEnabled) return;

		for (int index : displayedFMS)
			icons.get(index).setVisible(false);

		iconLayer.clearPreviousScan();
		layer.firePropertyChange(AVKey.LAYER, null, layer);
	}

	public void processSelection() {
		fmsLayer.clearSolutions();
		displayedFMS.clear();

		for (int index : selection)
		{
			Earthquake eq = all.get(index);
			fmsLayer.addFMS(eq);

			displayedFMS.add(index);
		}

		selection.clear();

		processVisibility();
	}

	public synchronized void processExtrude() {
		iconRenderer.setExtruded(isExtruded);
		fmsLayer.setExtruded(isExtruded);
		layer.firePropertyChange(AVKey.LAYER, null, layer);
	}

	public void selected(SelectEvent event) {
		if (event.getEventAction().equals(SelectEvent.LEFT_CLICK)) {
			Object topObject = event.getTopObject();
			int index = 0;
			for (DetailedIcon icon : icons) {
				if (icon == topObject)
					break;
				index++;
			}

			if (index == icons.size())
			{
				index = 0;
				for (Earthquake eq : all)
				{
					if (eq == topObject)
						break;
						
					index++;
				}
			}

			if (pickedItem != -1) {
				icons.get(pickedItem).setHighlighted(false);
			}

			if (index == icons.size()) 
				pickedItem = -1;
			else {
				pickedItem = index;
				icons.get(pickedItem).setHighlighted(true);
			}
			selectionChanged();
		}
	}

	private void selectionChanged() {
		if (pickedItem == -1)
			dataTextArea.setText("");
		else
		{
			Earthquake eq = all.get(pickedItem);

			final StringBuffer sb = new StringBuffer();
			final String header = "Lat\tLon\tData\tTime\tDepth(km)\tMag (body)\t Mag (surface)\tNP 1 Strike\tNP 1 Dip\tNP 1 Rake\tNP 2 Strike\tNP 2 Dip\tNP 2 Rake\n";
			sb.append(header);
			sb.append(eq.lat);
			sb.append("\t");
			sb.append(eq.lon);
			sb.append("\t");
			sb.append(eq.date);
			sb.append("\t");
			sb.append(eq.time);
			sb.append("\t");
			sb.append(eq.depth / -1000.);
			sb.append("\t");
			sb.append(eq.magnitude_body);
			sb.append("\t");
			sb.append(eq.magnitude_surface);
			sb.append("\t");
			sb.append(eq.strike1);
			sb.append("\t");
			sb.append(eq.dip1);
			sb.append("\t");
			sb.append(eq.rake1);
			sb.append("\t");
			sb.append(eq.strike2);
			sb.append("\t");
			sb.append(eq.dip2);
			sb.append("\t");
			sb.append(eq.rake2);

			dataTextArea.setText(sb.toString());
			dataTextArea.setCaretPosition(header.length());
			dataTextArea.moveCaretPosition(sb.toString().length());
			dataTextArea.requestFocus();
		}
	}

	public void selectLasso(List<Position> area) {
		if (!isEnabled()) return;

		GeneralPath cylindricalPath = new GeneralPath();
		GeneralPath polarPath = new GeneralPath();

		Iterator<Position> iter = area.iterator();
		Position pos = iter.next();

		boolean northPole = pos.getLatitude().degrees > 0;
		boolean crossedDateLine = false;
		boolean positive = false;
		Projection polarProj;
		if (northPole)
			polarProj = new PolarStereo( new Point(320, 320),
					 0., 25600., 71., PolarStereo.NORTH, PolarStereo.WGS84);
		else
			 polarProj = new PolarStereo( new Point(320, 320),
					 180., 25600., -71., PolarStereo.SOUTH, PolarStereo.WGS84);

		cylindricalPath.moveTo((float) pos.getLongitude().degrees, 
				(float) pos.getLatitude().degrees);

		Point2D pnt = polarProj.getMapXY(pos.getLongitude().degrees, 
					pos.getLatitude().degrees);
		polarPath.moveTo((float) pnt.getX(),
				(float) pnt.getY());

		double lastX = pos.getLongitude().degrees;

		while (iter.hasNext()) {
			pos = iter.next();

			double x = pos.getLongitude().degrees;
			double dif = Math.abs(x - lastX);

			// Crossed the date line, make it better
			if (dif > 180)
			{
				crossedDateLine = true;

				if (lastX > x){
					positive = true;
					x += 360;
				}
				else {
					positive = false;
					x -= 360;
				}
			}

			cylindricalPath.lineTo((float) x, 
					(float) pos.getLatitude().degrees);
			lastX = x;

			pnt = polarProj.getMapXY(pos.getLongitude().degrees, 
						pos.getLatitude().degrees);
			polarPath.lineTo((float) pnt.getX(),
					(float) pnt.getY());
		}

		cylindricalPath.closePath();
		polarPath.closePath();

		if (northPole)
			pnt = polarProj.getMapXY(0,90);
		else
			pnt = polarProj.getMapXY(0,-90);

		GeneralPath lassoPath;
		Projection lassoProj;

		boolean isPolar = polarPath.contains(pnt) ;

		// We've circled a pole deal with it
		if (isPolar)
		{
			lassoPath = polarPath;
			lassoProj = polarProj;
		}
		else
		{
			lassoPath = cylindricalPath;
			lassoProj = new IdentityProjection();
		}

		Rectangle2D r = lassoPath.getBounds();

		selection.clear();

		int index = 0;
		for (DetailedIcon icon : icons) {
			 Position p = icon.getPosition();

			pnt = lassoProj.getMapXY(p.getLongitude().degrees,
					 p.getLatitude().degrees);

			double x = pnt.getX();
			double y = pnt.getY();

			if (r.contains(x, y) && lassoPath.contains(x, y))
				selection.add(index);
			else if (!isPolar && crossedDateLine)
			{
				if (positive)
					 x += 360;
				else
					 x -= 360;

				if (r.contains(x, y) && lassoPath.contains(x, y))
					 selection.add(index);
			}
			index++;
		}
		processSelection();
	}
	
	public void setArea(Rectangle2D bounds) {
		// TODO Auto-generated method stub

	}

	public void setEnabled(boolean tf) {
		isEnabled = tf;

		processVisibility();
	}

	public void setLassoSelectionHandler(
			LassoSelectionHandler lassoSelectionHandler) {
		this.lassoSelectionHandler = lassoSelectionHandler;
	}

	public String getCommand() {
		return "cmt_cmd";
	}
}
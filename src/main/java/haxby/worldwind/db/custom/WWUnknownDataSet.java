package haxby.worldwind.db.custom;

import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.WWIcon;
import haxby.db.XYGraph;
import haxby.db.custom.CustomDB;
import haxby.db.custom.DBConfigDialog;
import haxby.db.custom.DBDescription;
import haxby.db.custom.DataSetGraph;
import haxby.db.custom.UnknownData;
import haxby.db.custom.UnknownDataSet;
import haxby.proj.IdentityProjection;
import haxby.proj.PolarStereo;
import haxby.proj.Projection;
import haxby.worldwind.layers.WWSceneGraph;
import haxby.worldwind.layers.DetailedIconLayer.DetailedIcon;
import haxby.worldwind.renderers.DetailedIconRenderer;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;

import org.geomapapp.util.XML_Menu;

public class WWUnknownDataSet extends UnknownDataSet {
	private static final String ICON_PATH = "org/geomapapp/resources/icons/wdot.png";
	private static final int ICON_SIZE = 12;

	protected ExtrudeTool extrudeTool;
	protected JLabel extrudeLabel;

	protected DetailedIconRenderer iconRenderer;
	protected RenderableLayer renderableLayer;

	public Map<Integer, DetailedIcon> iconMap;
	public Map<Integer, Polyline> polylineMap;
	public WWSceneGraph sceneGraph;

	protected int layerKey;
	protected int extrudeColumnIndex = 0;

	public WWUnknownDataSet(DBDescription desc, String input, String delim, CustomDB db) {
		this(desc, input, delim, db, false, null);
	}

	public WWUnknownDataSet(DBDescription desc, String input, String delim, CustomDB db, boolean skipPrompts) {
		this(desc, input, delim, db, skipPrompts, null);
	}

	public WWUnknownDataSet(DBDescription desc, String input, String delim, CustomDB db, boolean skipPrompts, XML_Menu xml_menu) {
		super(desc, input, delim, db, skipPrompts, xml_menu);
		
		this.sceneGraph = ((WWCustomDB) db).wwSceneGraph;
		this.renderableLayer = ((WWCustomDB) db).renderableLayer;
		updateLayer();
	}

	public Map<Integer, DetailedIcon> getIconMap() {
		if (iconMap == null) iconMap  = new HashMap<Integer, DetailedIcon>();
		return iconMap;
	}

	public Map<Integer, Polyline> getPolylineMap() {
		if (polylineMap == null) polylineMap = new HashMap<Integer, Polyline>();
		return polylineMap;
	}

	public void config() {
		config(false);
	}

	@Override
	public void config(boolean skipPrompts) {
		final DBConfigDialog config = new DBConfigDialog((Frame)map.getTopLevelAncestor(),this) {
			public void ok() {
				super.ok();
				updateLayer();
			}
		};

		if (skipPrompts)
		{
			config.addWindowListener(new WindowAdapter() {
				public void windowOpened(WindowEvent e) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							config.ok();
						}
					});
				}
			});
		}

		config.setVisible(true);
		config.dispose();
		db.repaintMap();
	}

	public void dispose() {
		super.dispose();

		if (layerKey != -1)
			sceneGraph.disposeSubLayer(layerKey);

		if (extrudeTool != null)
			extrudeTool.dispose();
		extrudeTool = null;

		this.getIconMap().clear();

		for (Polyline pl : this.getPolylineMap().values())
			renderableLayer.removeRenderable(pl);

		this.getPolylineMap().clear();
	}

	@Override
	public void initTable() {
		super.initTable();

		extrudeLabel = new JLabel();
		tp.add(extrudeLabel, "Symbol Elevation - None Selected");
	}

	public void valueChanged(ListSelectionEvent evt) {
		super.valueChanged(evt);
		map.repaint();
	}

	public void selectionChangedRedraw(boolean[] os){
		//TODO: If we have an image put it in our image box

		Iterator iter2 = xypoints.iterator();
		for (Iterator iter = graphs.iterator(); iter.hasNext();) {
			XYGraph xyg = (XYGraph) iter.next();
			DataSetGraph dsg = (DataSetGraph) iter2.next();
			dsg.selectionChangedRedraw(os);
			synchronized (xyg.getTreeLock()) {
				xyg.paintComponent(xyg.getGraphics(), false);
			}
		}

		for (WWIcon icon : this.getIconMap().values()) {
			icon.setHighlighted( false );
		}

		for (Polyline pl : this.getPolylineMap().values())
			pl.setColor(this.getColor());

		for (int row : selRows) {
			int index = tm.displayToDataIndex.get(row);
			WWIcon icon = this.getIconMap().get(index); 
			if (icon != null) 
				icon.setHighlighted(true);
			Polyline pl = this.getPolylineMap().get(index);
			if (pl != null)
				pl.setColor(Color.red);
		}
	}

	public void propertyChange(PropertyChangeEvent evt) {
		processVisibility();
		map.repaint();
	}

	public void color() {
		super.color();
		processVisibility();
	}

	public void scale() {
		super.scale();
		processVisibility();
	}

	public void extrude() {
		if (!station) return;

		// Get Map owner
		Container c = map.getParent();
		while (!(c instanceof Frame)) c=c.getParent();

		// Create list of columns
		Object o2[] = new Object[tm.indexH.size()];
		for (int i = 0; i < o2.length; i++)
			o2[i] = header.get(((Integer)tm.indexH.get(i)).intValue());

		extrudeColumnIndex = 0;

		// Prompt for column to extrude by
		Object o = JOptionPane.showInputDialog(c, "Choose column to extrude icon elevation by:", "Select Column",
				JOptionPane.QUESTION_MESSAGE, null, o2, o2[extrudeColumnIndex]);
		if (o==null) return;
		for (extrudeColumnIndex = 0; extrudeColumnIndex < o2.length; extrudeColumnIndex++)
			if (o2[extrudeColumnIndex] == o) break;

		float[] values = new float[tm.displayToDataIndex.size()];
		for (int i = 0; i < values.length; i++) {
			try {
				values[i] = Float.parseFloat(tm.getValueAt(i, extrudeColumnIndex).toString());
			} catch (Exception ex) {
				values[i] = Float.NaN;
			}
		}

		if (extrudeTool == null){
			extrudeTool = new ExtrudeTool();
			extrudeTool.addPropertyChangeListener(this);
		}

		extrudeTool.setName((String)o + " - " + desc.name);
		tp.setTitleAt(tp.indexOfComponent(extrudeLabel), "Symbol Elevation - " + (String)o);

		extrudeTool.showDialog((JFrame) c);

		processVisibility();
	}

	public synchronized void setArea(final Rectangle2D rect) {
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				tm.setArea(rect, 1);
				if (cst != null && cst.isShowing()) updateColorScale();
				if (sst != null && sst.isShowing()) updateSymbolScale();

				Iterator iter2 = xypoints.iterator();
				for (Iterator iter = graphs.iterator(); iter.hasNext();) {
					XYGraph xyg = (XYGraph) iter.next();
					DataSetGraph dsg = (DataSetGraph) iter2.next();
					synchronized (xyg.getTreeLock()) {
						xyg.paintComponent(xyg.getGraphics(), false);
					}
				}
				processVisibility();
			}
		});
	}

	public synchronized void processVisibility() {
		if (!plot) {
			for (WWIcon icon : getIconMap().values()) {
				if (icon != null)
					icon.setVisible(false);
			}
			return;
		}

		if (tm == null || tm.displayToDataIndex == null) return;

		boolean[] visible = new boolean[data.size()];
		if (visible.length == 0) return;

		int size = (int) (ICON_SIZE * symbolSize / 100f);

		Map<Integer, DetailedIcon> icons = this.getIconMap();
		Map<Integer, Polyline> polylines = this.getPolylineMap();

		for (int i = 0; i < tm.displayToDataIndex.size(); i++) {

			int index = (Integer)tm.displayToDataIndex.get(i);
			DetailedIcon icon = icons.get(index);
			Polyline pl = polylines.get(index);

			Color c = this.getColor();
			if (cst != null && cst.isShowing()) {
				c = getSymbolColor(i);
				if (c == null) 
					continue;
			}

			int scaledSize = size;
			if (sst != null && sst.isShowing()) {
				float scale = getSymbolScale(i);
				scaledSize = (int) (size * scale);
				if(Float.isNaN(scale)) 
					continue;
			}

			float elevation = 0;
			if (extrudeTool != null && extrudeTool.isShowing()) {
				elevation = getSymbolElevation(i);
				if (Float.isNaN(elevation)) 
					continue;
			}

			if (icon != null) {
				icon.setIconColor(c);
				icon.setSize(new Dimension(scaledSize,scaledSize));
				icon.setIconElevation((int) elevation);
			}
			if (pl != null) {
				pl.setColor(c);
			}

			visible[index] = true;
		}

		for (Entry<Integer, DetailedIcon> icon : this.getIconMap().entrySet())
			icon.getValue().setVisible(visible[icon.getKey()]);

		for (Entry<Integer, Polyline> pl : this.getPolylineMap().entrySet())
			if (!visible[pl.getKey()])
				pl.getValue().setColor(Color.gray);
	}

	private float getSymbolElevation(int i) {
		try {
			float f = Float.parseFloat(tm.getValueAt(i, extrudeColumnIndex).toString());
			return f * extrudeTool.getScale();
		} catch (NumberFormatException ex) {
			return Float.NaN;
		}
	}

	private Color getSymbolColor(int i) {
		try {
			float f = Float.parseFloat(tm.getValueAt(i, colorColumnIndex).toString());
			if (Float.isNaN(f))
				return null;
			return cst.getColor(f);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private float getSymbolScale(int i) {
		try {
			float k = Float.parseFloat(tm.getValueAt(i, scaleColumnIndex).toString());
			return sst.getSizeRatio(k);
		} catch (NumberFormatException ex) {
			return Float.NaN;
		}
	}

	public synchronized void updateLayer() {
		if (sceneGraph == null)
			return;

		boolean flat = false; 
		
		if (layerKey != -1) {
			flat = sceneGraph.isAFlatScene(layerKey);
			sceneGraph.disposeSubLayer(layerKey);
		}

		layerKey = sceneGraph.createSubLayer();

		if (flat)
			sceneGraph.makeFlatLayer(layerKey);

		Map<Integer, DetailedIcon> myIcons = this.getIconMap();
		int size = (int) (ICON_SIZE * symbolSize / 100f);

		if (iconRenderer == null)
			iconRenderer = new DetailedIconRenderer();

		int index = 0;
		for (UnknownData d : this.data) {
			boolean invalidStation = (Float.isNaN(d.y) || d.y > 90 || d.y < -90);
			invalidStation = invalidStation || (Float.isNaN(d.x) || d.x > 360 || d.y < -180);

			if (!invalidStation) {
				DetailedIcon icon = new DetailedIcon(ICON_PATH, Position.fromDegrees(d.y, d.x, 0));
				icon.setIconColor( this.getColor() );
				icon.setSize(new Dimension(size, size));
				icon.setHighlightScale(2);
				myIcons.put(index, icon);
				
				sceneGraph.addItem(
						new WWSceneGraph.SceneItemIcon(icon, iconRenderer),
						layerKey);
			}
			index++;
		}

		for (Polyline poly : getPolylineMap().values())
			renderableLayer.removeRenderable(poly);
		getPolylineMap().clear();

		for (int polyIndex : polylines) {
			UnknownData ud = data.get(polyIndex);
			if (ud.polyline == null) continue;

			List<Position> points = new LinkedList<Position>();

			PathIterator pi = ud.polyline.getPathIterator(new AffineTransform());
			float[] coords = new float[2];
			while (true) {
				pi.currentSegment(coords);
				if (pi.isDone()) break;
				points.add( Position.fromDegrees(coords[1] + ud.polyY0, coords[0] + ud.polyX0, 0) );
				pi.next();
			}

			Polyline pl = new Polyline(points);
			pl.setClosed(false);
			pl.setColor(Color.BLUE);
			pl.setFollowTerrain(true);
			pl.setLineWidth(2);
			getPolylineMap().put(polyIndex, pl);

			renderableLayer.addRenderable(pl);
		}
		processVisibility();
	}

	public void selected(SelectEvent event) {
		if (event.getEventAction().equals(SelectEvent.LEFT_CLICK)) {
			Map<Integer, DetailedIcon> myIcons = this.getIconMap();

			int index = -1;
			for (Entry<Integer, DetailedIcon> entry : myIcons.entrySet()) {
				if (entry.getValue() == event.getTopObject()) {
					index = entry.getKey();
					break;
				}
			}

			if (index == -1) {
				// Check polylines
				Map<Integer, Polyline> myPolylines = this.getPolylineMap();

				for (Entry<Integer, Polyline> entry : myPolylines.entrySet()) {
					if (entry.getValue() == event.getTopObject()) {
						index = entry.getKey();
						break;
					}
				}
			}

			final int i = index;
			if (i != -1) {
				new Thread () {
					public void run() {
						int j = 0;
						for (int index : tm.displayToDataIndex) {
							if (index == i) {
								dataT.getSelectionModel().setSelectionInterval(j, j);
								dataT.ensureIndexIsVisible(j);
								break;
							} else
								j++;
						}
					}
				}.start();
			}
		}
	}

	public void setLayerSceneGraph(WWSceneGraph layer) {
		this.sceneGraph = layer;
	}

	public void setRenderableLayer(RenderableLayer layer) {
		this.renderableLayer = layer;
	}

	public void selectWWLasso(List<Position> area) {
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

		cylindricalPath.moveTo((float)  pos.getLongitude().degrees, 
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

			cylindricalPath.lineTo(
					(float) x, 
					(float) pos.getLatitude().degrees);

			lastX = x;

			pnt = polarProj.getMapXY(pos.getLongitude().degrees, 
					pos.getLatitude().degrees);
			polarPath.lineTo(
					(float) pnt.getX(), 
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

		dataT.getSelectionModel().setValueIsAdjusting(true);
		dataT.getSelectionModel().clearSelection();
		if (station){
			for( int k=0 ; k<tm.displayToDataIndex.size() ; k++) {
				int z = ((Integer)tm.displayToDataIndex.get(k)).intValue();
				UnknownData d = (UnknownData) data.get(z);

				if (f!=null&&k<f.length&&Float.isNaN(f[k])) continue;
				if (f2!=null&&k<f2.length&&Float.isNaN(f2[k])) continue;
				if (dataT.isRowSelected(k)){ continue; }
				//Symbol s = new Symbol(Symbol.CIRCLE, (float) (3./zoom), b, a);

				if (Float.isNaN(d.x) || Float.isNaN(d.y)) continue;

				pnt = lassoProj.getMapXY(d.x, d.y);

				double x = pnt.getX();
				double y = pnt.getY();

				if (!isPolar)
					while (x > 180) x -= 360;

				if (r.contains(x, y) && lassoPath.contains(x, y))
					dataT.getSelectionModel().addSelectionInterval(k, k);
				else if (!isPolar && crossedDateLine)
				{
					if (positive)
						x += 360;
					else
						x -= 360;

					if (r.contains(x, y) && lassoPath.contains(x, y))
						dataT.getSelectionModel().addSelectionInterval(k, k);
				}
			}
		}

		dataT.getSelectionModel().setValueIsAdjusting(false);

		if (dataT.getSelectedRow() != -1)
			dataT.ensureIndexIsVisible(dataT.getSelectedRow());
	}
}

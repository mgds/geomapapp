package haxby.map;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.undo.UndoManager;

import org.geomapapp.credit.Credit;
import org.geomapapp.gis.shape.ESRIShapefile;
import org.geomapapp.grid.Grid2DOverlay;
import org.geomapapp.grid.GridDialog;
import org.geomapapp.util.XML_Menu;

import haxby.db.custom.UnknownDataSet;
import haxby.proj.CylindricalProjection;
import haxby.proj.Mercator;
import haxby.proj.Projection;
import haxby.util.ScaledComponent;

//import com.sun.image.codec.jpeg.JPEGCodec;
//import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 	XMap loads and manipulates the map.
 */
public class XMap extends ScaledComponent implements Zoomable,
				Scrollable, Printable, AdjustmentListener, MouseListener, MouseMotionListener, KeyListener {

	Projection proj;

	// GMA 1.6.4: Variables added to record values when mouse button is pressed (for panning)
	/**
	 * Pressed x value
	 */
	protected int pressedX = 0;

	/**
	 * Pressed y value
	 */
	protected int pressedY = 0;

	/**
	 * Pressed x value on screen
	 */
	protected int screenPressedX = 0;

	/**
	 * Pressed y value on screen
	 */
	protected int screenPressedY = 0;
	// GMA 1.6.4

	/**
	 * Base cursor, Used to override defaultCursor settings
	 */
	protected Cursor baseCursor = Cursor.getDefaultCursor();

	/**
	 * Base map; Optional.
	 */
	protected BufferedImage image = null;

	/**
	 * Overlay objects added to the map.
	 */
	protected Vector<Overlay> overlays;

	/**
	 * A mapping of Overlay to Floats for the alpha values to draw each overlay with
	 */
	protected Map<Overlay, Float> overlayAlphas;

	/**
	 * Not implemented.
	 */
	protected Vector<MapInset> mapInsets;

	protected MapTools tools;
	/**
	 * Scale factor
	 */
	protected double zoom;

	/**
	 * Base units of grid
	 */
	protected String units = "m";
	
	/**
	 * Base data type of grid
	 */
	protected String dataType = "";

	/**
	 * Alternative z value to be displayed
	 */
	protected double alternateZ = Double.NaN;

	/**
	 * Alternative units to be displayed (associated with alternateZ)
	 */
	protected String alternateUnits = "m";

	/**
	 * Alternative z value to be displayed
	 */
	protected float alternate2Z = Float.NaN;

	/**
	 * Alternative units to be displayed (associated with alternateZ)
	 */
	protected String alternate2Units = "m";

	/**
	 * Angle of ratation; Not implemented.
	 */
	protected int rotation;

	/**
	 * Dimension of map when zoom = 0;
	 */
	protected int width, height;

	/**
	 	Used to scroll the map.
	 */
	protected JScrollPane scrollPane = null;

	/**
	 * MapBorder draws Latitude/Longitude annotations on the border.
	 */
	protected MapBorder mapBorder = null;

	/**
	 * Plot the Latitude/Longitude lines.
	 */
	protected boolean graticule = true;

	/**
	 * For CylindricalProjection, wrap = nodes per 360 degrees; otherwise wrap = -1.
	 */
	protected double wrap;
	Object app;
	Grid2DOverlay focus = null;

	/**
	 * Paint insets on map if true, do not paint insets on map if false.
	 */
	protected boolean includeInsets = true;
	protected boolean spaceBarDown = false;
	protected String pastZoom = "0", nextZoom;

	/**
	 * UndoManager to manage zoom actions
	 */
	protected static UndoManager undoManager;
	public static JTextField zoomActionTrack = new JTextField(80);

	public XMap(Object app, Projection proj, BufferedImage image) {
		this.app = app;
		this.proj = proj;
		this.image = image;
		width = image.getWidth();
		height = image.getHeight();
		overlays = new Vector();
		overlayAlphas = new HashMap<Overlay, Float>();
		mapInsets = null;
		zoom = 1d;
		rotation = 0;
		try {
			CylindricalProjection p = (CylindricalProjection) proj;
			wrap = Math.rint(360.*(p.getX(10.)-p.getX(9.)));
		} catch (ClassCastException ex) {
			wrap = -1.;
		}

		// GMA 1.6.4: Add mouse motion listener to perform pan, mouse listener to perform center
		addMouseMotionListener(this);
		addMouseListener(this);
		undoManager = new UndoManager();
		undoManager.setLimit(8);
		zoomActionTrack.getDocument().addUndoableEditListener(undoManager);
	}

	public XMap(Object app, Projection proj, int width, int height) {
		this.app = app;
		this.proj = proj;
		this.width = width;
		this.height = height;
		overlays = new Vector();
		overlayAlphas = new HashMap<Overlay, Float>();
		mapInsets = null;
		zoom = 1d;
		setLayout( null );
	//	addMapInset( new haxby.image.Logo(this) );
		try {
			CylindricalProjection p = (CylindricalProjection) proj;
			wrap = Math.rint(360.*(p.getX(10.)-p.getX(9.)));
		} catch (ClassCastException ex) {
			wrap = -1.;
		}

		// GMA 1.6.4: Add mouse motion listener to perform pan, mouse listener to perform center
		addMouseMotionListener(this);
		addMouseListener(this);
		addKeyListener(this);
		undoManager = new UndoManager();
		undoManager.setLimit(8);
		zoomActionTrack.getDocument().addUndoableEditListener(undoManager);
	}

	/**
	 *	Assigns the scrollPane.
	 */
	public void addNotify() {
		super.addNotify();
		Container c = getParent();
		while(c != null) {
			if( c instanceof JScrollPane ) {
				scrollPane = (JScrollPane) c;

//				***** GMA 1.6.4: Add adjustment listeners to scroll bars to perform pan
				scrollPane.getHorizontalScrollBar().addAdjustmentListener(this);
				scrollPane.getVerticalScrollBar().addAdjustmentListener(this);

				return;
			}
			c = c.getParent();
		}
	}

	/**
	Get wrap value.
	 * @return wrap value.
	 */
	public double getWrap() {
		return wrap;
	}

	public MapTools getMapTools() {
		if( app instanceof MapApp ) {
			tools = ((MapApp)app).tools;
		}
		return tools;
	}

	public BufferedImage getBaseMap(){
		if( app instanceof MapApp ) {
			return ((MapApp)app).getBaseMap();
		}
		return null;
	}

	public Credit getCredit() {
		if( app instanceof MapApp ) {
			return ((MapApp)app).credit;
		}
		return null;
	}

	/**
	 *	Sets the map border.
	 *	@param border map border to set.
	 */
	public void setMapBorder( MapBorder border ) {
		mapBorder = border;
	}

	/**
		Get the map border.
		@return the map border.
	 */
	public MapBorder getMapBorder() {
		return mapBorder;
	}

	/**
	 	Gets the default height/width.
	 	@return the default height/width in a dimension.
	 */
	public Dimension getDefaultSize() {
		return new Dimension( width, height );
	}

	/**
	 	Gets the Preferred Size of map.
	 	@return the perferred size of the map.
	 */
	public Dimension getPreferredSize() {
		Dimension dim = new Dimension( (int) Math.ceil( zoom*(double)width ),
					(int) Math.ceil( zoom*(double)height ));
		if(rotation%2==1) {
			int tmp = dim.width;
			dim.width = dim.height;
			dim.height = tmp;
		}
		if(mapBorder != null) {
			Insets ins = mapBorder.getBorderInsets(this);
			dim.width += ins.left + ins.right;
			dim.height += ins.top + ins.bottom;
		}
		return dim;
	}

	/**
	 	If there is an overlay.
	 	@return If there is an overlay.
	 */
	public boolean hasOverlay( Overlay overlay ) {
		return overlays.contains( overlay );
	}

	/**
	 	Adds Overlay object to wrap.
	 	@param overlay Overlay object to add.
	 */
	public void addOverlay( int index, Overlay overlay ) {
		addOverlay("Overlay", overlay, index, null, true, null);
	}

	public void addOverlay( int index, Overlay overlay, boolean fireChange) {
		addOverlay("Overlay", overlay, index, null, fireChange, null);
	}

	public void addOverlay(Overlay overlay, boolean fireChange) {
		addOverlay("Overlay", overlay, fireChange, null);
	}

	public int getOverlaysSize() {
		return overlays.size();
	}

	public float getOverlayAlpha(Overlay overlay) {
		if (!overlayAlphas.containsKey(overlay))
			return Float.NaN;
		return overlayAlphas.get(overlay);
	}

	public void setOverlayAlpha(Overlay overlay, float alpha) {
		if (overlayAlphas.containsKey(overlay))
			overlayAlphas.put(overlay, alpha);
	}

	public int getOverlayIndex(Overlay overlay) {
		return overlays.indexOf(overlay);
	}

	public void addOverlay( Overlay overlay ) {
		addOverlay("Overlay", overlay, overlays.size(), null, true, null);
	}

	public void addOverlay( String name, String infoURLString, Overlay overlay ) {
		addOverlay(name, overlay, overlays.size(), infoURLString, true, null);
	}

	public void addOverlay( String name, String infoURLString, Overlay overlay, XML_Menu xml_item ) {
		addOverlay(name, overlay, overlays.size(), infoURLString, true, xml_item);
	}

	public void addOverlay( String name, Overlay overlay ) {
		addOverlay(name, overlay, overlays.size(), null, true, null);
	}

	public void addOverlay(String name, Overlay overlay, XML_Menu xml_item){
		addOverlay(name, overlay, overlays.size(), null, true, xml_item);
	}

	public void addOverlay( String name, Overlay overlay, int index ) {
		addOverlay(name, overlay, index, null, true, null);
	}

	public void addOverlay( String name, Overlay overlay, boolean fireChange) {
		addOverlay(name, overlay, overlays.size(), null, fireChange, null);
	}

	public void addOverlay( String name, Overlay overlay, boolean fireChange, XML_Menu xml_item ) {
		addOverlay(name, overlay, overlays.size(), null, fireChange, xml_item);
	}

	public void addOverlay( String name, Overlay overlay, int index, String infoURLString, boolean fireChange) {
		addOverlay(name, overlay, index, infoURLString, true, null);
	}

	public void addOverlay( String name, Overlay overlay, int index, String infoURLString, boolean fireChange, XML_Menu xml_item) {
		if( overlays.contains(overlay) )return;
		if ( index > (overlays.size() - 1) ) {
			overlays.add(overlay);
		} else {
			overlays.add(index,overlay);
		}
		overlayAlphas.put(overlay, 1f);
		if( overlay instanceof Grid2DOverlay ) {
			focus=(Grid2DOverlay)overlay;
		} else if (overlay instanceof ESRIShapefile && ((ESRIShapefile) overlay).getMultiGrid() != null) {
			focus = ((ESRIShapefile) overlay).getMultiGrid().getGrid2DOverlay();
		}
		
		if (fireChange){
			if(xml_item == null){
				// xml null
				this.firePropertyChange(name, infoURLString, overlay);
			}
			if(xml_item != null){
				// xml not null
				if(infoURLString == null){
					//if infoURLString is null, then use the name parameter
					this.firePropertyChange(name, xml_item, overlay);
				} else {
					this.firePropertyChange(infoURLString, xml_item, overlay);
				}
			}
		}
	}

	public void moveOverlayToFront(Overlay overlay) {
		overlays.remove(overlay);
		overlays.add(overlay);
	}

	public void moveOverlayToTop(Overlay overlay) {
		//move layer to top of layerManager
		((MapApp)app).layerManager.moveToTop(overlay);
	}
	
	public Grid2DOverlay getFocus() {
		return focus;
	}

	public void setIncludeInsets( boolean input ) {
		includeInsets = input;
	}

	public void setFocus( Grid2DOverlay newFocus ) {
		focus = newFocus;
	}

	/**
	 * Removes Overlay object from wrap.
	 * @param overlay Overlay object to remove.
	 */
	public int removeOverlay( Overlay overlay ) {
		return removeOverlay(overlay, true);
	}

	public int removeOverlay( Overlay overlay, boolean removeFlag ) {
		int index = overlays.indexOf(overlay);
		overlays.remove( overlay );
		overlayAlphas.remove(overlay);

		boolean doMask = focus.isMasked();
		if (overlay == focus || 
				(overlay instanceof ESRIShapefile && ((ESRIShapefile) overlay).getMultiGrid() != null && 
						((ESRIShapefile) overlay).getMultiGrid().getGrid2DOverlay() == focus)) {
			for( int i=overlays.size()-1 ; i>-1 ; i--) {
				Overlay ol = overlays.get(i);
				if( ol instanceof Grid2DOverlay ) {
					focus=(Grid2DOverlay)ol;
					break;
				} else if (ol instanceof ESRIShapefile && ((ESRIShapefile) ol).getMultiGrid() != null) {
					focus = ((ESRIShapefile) ol).getMultiGrid().getGrid2DOverlay();
					break;
				}
			}
		}
		if (app instanceof MapApp && doMask) {
			((MapApp) app).tools.maskB.doClick();
			((MapApp) app).tools.maskB.doClick();
		}
		this.firePropertyChange("overlays", overlay, removeFlag);
		return index;
	}
	
	public void removeDataSet(UnknownDataSet ds) {
		((MapApp) app).layerManager.removeLayerPanel(ds);
	}

	/**
	 * Not Implemented.
	 */
	public void rotate(int direction) {
		Dimension dim = getPreferredSize();
		Rectangle rect = getVisibleRect();
		if(dim.width>rect.width) rect.width = dim.width;
		if(dim.height>rect.height) rect.height = dim.height;
		if(proj instanceof CylindricalProjection ) return;
		rotation += (direction>0) ? 1:3;
		rotation &= 0x3;
		JScrollBar hsb = scrollPane.getHorizontalScrollBar();
		int x = hsb.getValue();
		JScrollBar vsb = scrollPane.getVerticalScrollBar();
		int y = vsb.getValue();
		if(direction>0) {
			hsb.setValue(dim.height - rect.height - y);
			vsb.setValue(x);
		} else {
			hsb.setValue(y);
			vsb.setValue(dim.width -rect.width - x);
		}
		revalidate();
		repaint();
	}

	/**
	 * Zoom in to point p.
	 * @param point to zoom to.
	 */
	public void zoomIn( Point p ) {
		doZoom( p, 2d );
	}
	
	/**
	 * Zoom in to point p.with speed
	 * @param point to zoom to.
	 * @param double zoom speed.
	 */
	public void zoomSpeed( Point p, Double d ) {
		doZoom( p, d );
	}

	/**
	 	Zoom out from point p.
	 	@param point to zoom from.
	 */
	public void zoomOut( Point p ) {
		doZoom( p, .5d );
	}

	/**
	 * Set if the graticule should be displayed.
	 * @param tf True if graticul should be displayed.
	 */
	public void setGraticule( boolean tf ) {
		if( graticule==tf )return;
		graticule = tf;
		repaint();
	}

	public void zoomToWESN(double[] wesn) {
		if (wesn[0] >= wesn[1] ||
				wesn[2] >= wesn[3]) return;

		if (wesn[1] - wesn[0] >= 300) { // Zoom to World
			Point2D.Double p = (Point2D.Double) 
				proj.getMapXY(180, (wesn[2] + wesn[3]) / 2);

			p.x *= zoom;
			p.y *= zoom;
			Insets insets = getInsets();
			p.x += insets.left;
			p.y += insets.top;
			doZoom(p, 1 / zoom);
			return;
		}

		if (proj instanceof Mercator) {
			wesn[2] = Math.max(wesn[2], -80);
			wesn[3] = Math.min(wesn[3], 80);
		}

		Point2D[] pts = new Point2D[] {
				proj.getMapXY(wesn[0],wesn[3]),
				proj.getMapXY(wesn[1],wesn[3]),
				proj.getMapXY(wesn[0],wesn[2]),
				proj.getMapXY(wesn[1],wesn[2])};

		if (wrap > 0) {
			while (pts[0].getX() > pts[1].getX())
				pts[0].setLocation(pts[0].getX() - wrap, pts[0].getY());

			while (pts[2].getX() > pts[3].getX())
				pts[2].setLocation(pts[2].getX() - wrap, pts[2].getY());
		}

		double minX, minY;
		double maxX, maxY;
		minX = minY = Double.MAX_VALUE;
		maxX = maxY = -Double.MAX_VALUE;
		for (int i = 0; i < pts.length; i++) {
			minX = Math.min(minX, pts[i].getX());
			minY = Math.min(minY, pts[i].getY());
			maxX = Math.max(maxX, pts[i].getX());
			maxY = Math.max(maxY, pts[i].getY());
		}

		while (minX < 0 && wrap > 0) {
			minX += wrap;
			maxX += wrap;
		}

		Rectangle2D.Double bounds = new Rectangle2D.Double();
		bounds.x = minX;
		bounds.y = minY;

		bounds.width = maxX - minX;
		bounds.height = maxY - minY;

		bounds.x -= bounds.width * .1;
		bounds.y -= bounds.height* .1;

		bounds.width += bounds.width * .2;
		bounds.height -= bounds.height * .1;

		zoomToRect(bounds);
	}

	public void zoomToRect( Rectangle2D rect ) {
		Point2D.Double p = new Point2D.Double( rect.getX() + rect.getWidth()/2., rect.getY() + rect.getHeight()/2. );
		p.x *= zoom;
		p.y *= zoom;
		Insets insets = new Insets(0,0,0,0);
		if(mapBorder!=null) insets = mapBorder.getBorderInsets(this);
		p.x += insets.left;
		p.y += insets.top;
		Rectangle r = getVisibleRect();
		r.width -= insets.left + insets.right;
		r.height -= insets.top + insets.bottom;
		double factor = Math.min( r.getWidth()/(zoom*rect.getWidth()),
					r.getHeight()/(zoom*rect.getHeight()) );
		// default min zoom is one.
		if(factor < 1) {
			factor = 1;
		}
		doZoom( p, factor );
	}
	/**
	 	Zoom to a rectangle defined by a mouse
	 	@param rect Rectangled defined by the mouse
	 */
	public void zoomTo( Rectangle rect ) {
		if(rect.width<10 || rect.height<10) return;
		Point p = new Point( rect.x + rect.width/2, rect.y + rect.height/2 );
		Rectangle r = getVisibleRect();
		Insets insets = new Insets(0,0,0,0);
		if(mapBorder!=null) insets = mapBorder.getBorderInsets(this);
		r.width -= insets.left + insets.right;
		r.height -= insets.top + insets.bottom;
		double factor = Math.min( r.getWidth()/rect.getWidth(),
					r.getHeight()/rect.getHeight() );
		doZoom( p, factor );
	}

	/**
	 	Zoom by factor and center on point p.
	 	@param p point to center on.
	 	@param factor Factor to zoom by.
	 */
	public void doZoom( Point2D p, double factor ) {
		AffineTransform ATrans = new AffineTransform();
		ATrans.scale( zoom, zoom );
		Insets insets = getInsets();
		Rectangle rect = getVisibleRect();
		Dimension dim = getParent().getSize();
		if( dim.width>rect.width) rect.width = dim.width;
		if( dim.height>rect.height) rect.height = dim.height;
		rect.width -= insets.left + insets.right;
		rect.height -= insets.top + insets.bottom;
		double nX = (p.getX() - insets.left - (rect.width/(2.*factor)));
		double nY = ( p.getY() - insets.top - rect.height/(2.*factor));
		Point2D newP = null;
		try {
			newP = ATrans.inverseTransform( new Point2D.Double(nX, nY), null );
		} catch (Exception ex ) {
			return;
		}
		ATrans.scale( factor, factor );
		zoom *= factor;
		newP = ATrans.transform( newP, null );
		int newX = (int)newP.getX(); // + insets.left;
		// add the wrap value if newX is -ve.
		if (wrap > 0) {
			while (newX < 0) newX += wrap * zoom;
		}
		int newY = (int)newP.getY(); // + insets.top;	
		invalidate();
		scrollPane.validate();
		JScrollBar sb = scrollPane.getHorizontalScrollBar();
		sb.setValue(newX);
		sb = scrollPane.getVerticalScrollBar();
		sb.setValue(newY);
		revalidate();
		
		//If this is MapApp Auto Focus
		if (app instanceof MapApp) {
			((MapApp) app).autoFocus();
		}
		double[] wesn = getWESN();
		MapApp.sendLogMessage("Zoom&WESN="+wesn[0]+","+wesn[1]+","+wesn[2]+","+wesn[3]+"&zoom_level=" + zoom);
	}

	public void setZoomHistoryPast(XMap mapSource) {
		Rectangle2D r = null;
		r = mapSource.getClipRect2D();
		Point2D.Double p2 = new Point2D.Double(
				r.getX()+.5*r.getWidth(),
				r.getY()+.5*r.getHeight() );
		p2 = (Point2D.Double)mapSource.getProjection().getRefXY(p2);

		double zoom = mapSource.getZoom();
		NumberFormat fmtZoom1 = NumberFormat.getInstance();
		fmtZoom1.setMinimumFractionDigits(1);
		fmtZoom1.format(zoom);
		// set zoom
		if (getZoomHistoryPast().contentEquals("0")) {
			pastZoom = "past, " + p2.getX() + ", " + p2.getY() + ", " + zoom;
			zoomActionTrack.selectAll();
			zoomActionTrack.replaceSelection(pastZoom);
			//System.out.println("jtext " + zoomActionTrack.getText());
		}
	}

	public String getZoomHistoryPast() {
		if(pastZoom.contentEquals("0")) {
			pastZoom = "0";
		}
		return pastZoom;
	}

	public void setZoomHistoryNext(XMap mapSource) {
		Rectangle2D r = null;
		r = mapSource.getClipRect2D();
		Point2D.Double p2 = new Point2D.Double(
				r.getX()+.5*r.getWidth(),
				r.getY()+.5*r.getHeight() );
		p2 = (Point2D.Double)mapSource.getProjection().getRefXY(p2);

		double zoom = mapSource.getZoom();
		NumberFormat fmtZoom1 = NumberFormat.getInstance();
		fmtZoom1.setMinimumFractionDigits(1);
		fmtZoom1.format(zoom);

		nextZoom = "next, " + p2.getX() + ", " + p2.getY() + ", " + zoom;
		zoomActionTrack.selectAll();
		zoomActionTrack.replaceSelection(nextZoom);
		//System.out.println("track: " + zoomActionTrack.getText());
	}

	public String getZoomHistoryNext() {
		return nextZoom;
	}

	/*
	public void updateZoomHistory(String past, String next) throws IOException {
		doZoomHistory(past, next);
	}
	*/

	/*
	protected void doZoomHistory(String past, String next) throws IOException {

		if (app instanceof MapApp) {
			if(((MapApp) app).historyFile.exists()) {
				((MapApp) app).startNewZoomHistory();
			}

			if(((MapApp) app).historyFile.canWrite()) {
				BufferedWriter bw = new BufferedWriter(new FileWriter(((MapApp) app).historyFile.getAbsoluteFile(), false));
				bw.write(past + "\n");
				bw.write(next);
				bw.close();
			}
		}
	}
	*/

	public double[] getScales() {
		return new double[] { zoom, zoom};
	}

	public Insets getInsets() {
		if(mapBorder != null) {
			return mapBorder.getBorderInsets(this);
		}
		return super.getInsets();
	}

	/**
	 	Gets the zoom factor;
	 	@return the zoom factor.
	 */
	public double getZoom() {
		return zoom;
	}

	/**
	 	Rectangle to zoom to.
	 */
	Rectangle zoomRect = null;
	
	/**
	 	Sets the rectangle to zoom to.
	 	@param Rectangle to zoom to.
	 */
	public void setRect(Rectangle rect) {
		synchronized (getTreeLock()) {
			Graphics2D g = (Graphics2D)getGraphics();
			g.setXORMode(Color.white);
			if(zoomRect!=null) g.draw(zoomRect);
			zoomRect = rect;
			if(zoomRect!=null) g.draw(zoomRect);
		}
	}

	public JLabel getInfoLabel() {
		if( app instanceof MapApp ) {
			try {
				return ((MapApp)app).tools.info;
			} catch(Exception ex) {
				return null;
			}
		} else {
			return null;
		}
	}

	public boolean isSelectable() {
		return getMapTools().isSelectable();
	}

	/**
	 * Sets the units displayed above the map
	 * @param newUnits
	 */
	// 1.3.5: Sets the units displayed above the map next to the zoom factor
	public void setUnits( String newUnits )	{
		units = newUnits;
	}
	
	public void setDataType( String newDataType ) {
		dataType = newDataType;
	}

	// GMA 1.6.2: Functions to change alternative units and z value
	public void setAlternateZValue( double inputAlternateZ ) {
		alternateZ = inputAlternateZ;
	}

	public void setAlternateUnits( String inputAlternateUnits ) {
		alternateUnits = inputAlternateUnits;
	}

	public void setAlternate2ZValue( float inputAlternateZ ) {
		alternate2Z = inputAlternateZ;
	}

	public void setAlternate2Units( String inputAlternateUnits ) {
		alternate2Units = inputAlternateUnits;
	}

	/**
	 	Sets the coordinates of mouse to be displayed.
	 	@param p mouse location.
	 */
	public void setXY( Point p ) {
		if( p==null ) {
			if( app instanceof MapApp ) {
				((MapApp)app).tools.info.setText("");
			} else if(app instanceof PolarMapApp ) {
				((PolarMapApp)app).tools.info.setText("");
			}
			return;
		}
		Point2D.Double pt = (Point2D.Double)proj.getRefXY( getScaledPoint(p));
		setLonLat( pt );
	}
	public void setLonLat( double lon, double lat) {
		setLonLat( new Point2D.Double(lon, lat) );
	}
	void setLonLat( Point2D.Double pt ) {
		Point2D.Double pt0 = new Point2D.Double(pt.x, pt.y);
		while( pt.x>180. ) pt.x-=360.;
		String ew = "E";
		String ns = "N";
		if(pt.x<0.) {
			pt.x = -pt.x;
			ew = "W";
		}
		if(pt.y<0.) {
			pt.y = -pt.y;
			ns = "S";
		}
		NumberFormat fmt = NumberFormat.getInstance();
		fmt.setMaximumFractionDigits(3);
		fmt.setMinimumFractionDigits(3);
		NumberFormat fmt1 = NumberFormat.getInstance();
		fmt1.setMaximumFractionDigits(1);
		fmt1.setMinimumFractionDigits(1);

//		***** GMA 1.6.4: Allow as many digits as needed for XMCS two-way travel time
		NumberFormat fmt2 = NumberFormat.getInstance();
		fmt2.setMaximumFractionDigits(3);
		fmt2.setMinimumFractionDigits(3);

//		GMA 1.4.8: Format for minutes and for degrees with minutes listed
		NumberFormat fmtMins = NumberFormat.getInstance();
		fmtMins.setMaximumFractionDigits(0);
		fmtMins.setMinimumFractionDigits(0);
		NumberFormat fmtDegsMins = NumberFormat.getInstance();
		fmtDegsMins.setMaximumFractionDigits(0);
		fmtDegsMins.setMinimumFractionDigits(0);

		if( app instanceof MapApp ) {
			Grid2DOverlay focus = null;
			for( int i=overlays.size()-1 ; i>-1 ; i--) {
				if (overlays.get(i) instanceof ESRIShapefile && ((ESRIShapefile) overlays.get(i)).getMultiGrid() != null) {
					focus = ((ESRIShapefile) overlays.get(i)).getMultiGrid().getGrid2DOverlay();
					break;
				}
				if( overlays.get(i) instanceof Grid2DOverlay) {
					focus = (Grid2DOverlay)overlays.get(i);
					break;
				}
			}

//			GMA 1.4.8: TESTING
//			System.out.println( "Longitude: " + pt.getX() + "\t " + "Latitude: " + pt.getY() + "\t " + "Zoom: " + zoom );
			double minLon = ( pt.getX() % 1 ) * 60;
			double minLat = ( pt.getY() % 1 ) * 60;
//			System.out.println( "Longitude minutes: " + minLon + "\t " + "Latitude minutes: " + minLat );
//			fmt.setMaximumFractionDigits(7);
			fmt.setMinimumIntegerDigits(3);
			fmtDegsMins.setMinimumIntegerDigits(3);
			fmtMins.setMinimumIntegerDigits(2);
			if ( zoom < 4 ) {
				fmt.setMaximumFractionDigits(2);
				fmt.setMinimumFractionDigits(2);
				fmtMins.setMaximumFractionDigits(0);
				fmtMins.setMinimumFractionDigits(0);
			}
			else if ( zoom >= 4 && zoom < 16 ) {
				fmt.setMaximumFractionDigits(2);
				fmt.setMinimumFractionDigits(2);
				fmtMins.setMaximumFractionDigits(0);
				fmtMins.setMinimumFractionDigits(0);
			}
			else if ( zoom >= 16 && zoom < 256 ) {
				fmt.setMaximumFractionDigits(3);
				fmt.setMinimumFractionDigits(3);
				fmtMins.setMaximumFractionDigits(1);
				fmtMins.setMinimumFractionDigits(1);
			}
			else if ( zoom >= 256 && zoom < 2048 ) {
				fmt.setMaximumFractionDigits(4);
				fmt.setMinimumFractionDigits(4);
				fmtMins.setMaximumFractionDigits(2);
				fmtMins.setMinimumFractionDigits(2);
			}
			else if ( zoom >= 2048 && zoom < 4096 ) {
				fmt.setMaximumFractionDigits(4);
				fmt.setMinimumFractionDigits(4);
				fmtMins.setMaximumFractionDigits(3);
				fmtMins.setMinimumFractionDigits(3);
			}
			else if ( zoom >= 4096 && zoom < 32768 ) {
				fmt.setMaximumFractionDigits(5);
				fmt.setMinimumFractionDigits(5);
				fmtMins.setMaximumFractionDigits(3);
				fmtMins.setMinimumFractionDigits(3);
			}
			else if ( zoom >= 32768 ) {
				fmt.setMaximumFractionDigits(6);
				fmt.setMinimumFractionDigits(6);
				fmtMins.setMaximumFractionDigits(4);
				fmtMins.setMinimumFractionDigits(4);
			}
			MapApp mApp = ((MapApp)app);
			GridDialog gridDialog = mApp.getMapTools().getGridDialog();
			float z = Float.NaN; 
			String str = null;
			String dt = null;
			if (focus != null) {
				z = focus.getZ(pt0);
				str = focus.getUnits();
				dt = focus.getDataType();
			
				if (str != null)
					setUnits(str);
				if (dt != null)
					setDataType(dt);
			} else {
				// NSS 04/07/17 - this part may now be deprecated, since I set focus even for ESRIShape files in line 914 above
				if ( mApp.tools.suite != null && gridDialog != null) {
					Overlay topOverlay = mApp.layerManager.getOverlays().get(0);
					if (topOverlay instanceof ESRIShapefile && ((ESRIShapefile) topOverlay).getMultiGrid() != null) {
						Grid2DOverlay grid = ((ESRIShapefile) topOverlay).getMultiGrid().getGrid2DOverlay();
						z = grid.getZ(pt0);
						units = ((ESRIShapefile) topOverlay).getGridUnits();
					}	
				}
			}

//			***** GMA 1.4.8: Add a listing of the latitude and longitude that includes minutes
//			((MapApp)app).tools.info.setText( "("+fmt.format(pt.getX()) 
//				+" "+ew+", "+ fmt.format(pt.getY()) +" "+ns);

			//GMA 1.6.2: If decimal is zero for alternate values take it out
			String alternateZString = fmt2.format( (double)alternateZ );
			String alternate2ZString = fmt1.format( (double)alternate2Z );
			if ( alternateZString.indexOf(".") != -1 ) {
				if ( alternateZString.substring( alternateZString.indexOf(".") + 1, alternateZString.length() ).equals("0") ) {
					alternateZString = alternateZString.substring( 0, alternateZString.indexOf(".") );
				}
			}
			if ( alternate2ZString.indexOf(".") != -1 ) {
				if ( alternate2ZString.substring( alternate2ZString.indexOf(".") + 1, alternate2ZString.length() ).equals("0") ) {
					alternate2ZString = alternate2ZString.substring( 0, alternate2ZString.indexOf(".") );
				}
			}

			// Displays location and zoom on main MapApp
			if (((MapApp)app).tools.info != null) {
				((MapApp)app).tools.info.setText("(" + fmtDegsMins.format( Math.floor(pt.getX()) ) + "\u00B0" + 
					fmtMins.format( minLon ) + "\u0027" + ew + ", " + fmtDegsMins.format( Math.floor(pt.getY()) ) + "\u00B0" +
					fmtMins.format( minLat ) + "\u0027" + ns + ") (" + fmt.format( pt.getX() ) + "\u00B0" + ew + ", " + 
					fmt.format( pt.getY() ) + "\u00B0" + ns + ")"

					//1.3.5: Display appropriate units depending on the grid being displayed
					+( Float.isNaN(z) ? "" : ", "+ fmt1.format( (double)z ) +" " + units )

					// GMA 1.6.2: Display alternate values if there are alternate values
					+( Double.isNaN(alternateZ) ? "" : ", "+ alternateZString +" " + alternateUnits )
					+( Float.isNaN(alternate2Z) ? "" : ", "+ alternate2ZString +" " + alternate2Units )

	//				GMA 1.4.8: Dropped "factor" from "zoom factor"
	//				+"), zoom factor = "+ fmt1.format(zoom) );
					+", zoom = "+ fmt1.format(zoom) );
			}
		} else if( app instanceof PolarMapApp ) {
			((PolarMapApp)app).tools.info.setText( "("+fmt.format(pt.getX()) 
				+" "+ew+", "+ fmt.format(pt.getY()) +" "+ns+")");
		}
	}

	public String getLonLat() {
		if (((MapApp)app).tools.info == null) return null;
		String lonLat = "";
		lonLat = ((MapApp)app).tools.info.getText();
		return lonLat;
	}

	/**
	 	Gets the projection.
	 	@return the projection.
	 */
	public Projection getProjection() {
		return proj;
	}

	/**
	 * Not implemented.
	 */
	public void addMapInset( MapInset inset ) {
		if( mapInsets==null ) mapInsets = new Vector<MapInset>();
		mapInsets.add( inset );
	}

	/**
	 * Not implemented.
	 */
	public void removeMapInset( MapInset inset ) {
		if(mapInsets==null) {
			return;
		}
		mapInsets.remove( inset );
	}
//	public boolean isFocusTraversable() { return true; }

	/**
	 * Not Implemented.
	 */
	public double[] getWESN() {
		double[] wesn = new double[4];
		Rectangle r = getVisibleRect();
		if(mapBorder != null) {
			r = mapBorder.getInteriorRectangle(this, r.x, r.y, r.width, r.height);
		}
		Point2D.Double pt = new Point2D.Double( r.getX(), r.getY() );
		pt = (Point2D.Double)proj.getRefXY( getScaledPoint(pt));
		wesn[0] = pt.x;
		wesn[3] = pt.y;
		pt = new Point2D.Double( r.getX()+r.getWidth(),
					 r.getY()+r.getHeight() );
		pt = (Point2D.Double)proj.getRefXY( getScaledPoint(pt));
		wesn[1] = pt.x;
		wesn[2] = pt.y;
		return wesn;
	}

	/**
		Gets the scaled and offset graphics object.
		<pre>
		<b>Caution</b> - Must always be put in the code block:
			XMap map;
			Shape stuff;
			synchronized(map.getTreeLock()) {
				Graphics2D g = map.getGraphics2D();
				g.draw(stuff);
			}
		This ensures that the recorded graphics are syncronized.
		</pre>
		@return the scaled and offset graphics object.
	 */
	public Graphics2D getGraphics2D() {
		Graphics2D g = (Graphics2D) getGraphics();
		Rectangle r = getVisibleRect();
		if(mapBorder != null) {
			g.clip(mapBorder.getInteriorRectangle(this, r.x, r.y, r.width, r.height));
			Insets ins = mapBorder.getBorderInsets(this);
			g.translate((double)ins.left, (double)ins.top);
		} else {
			g.clip(r);
		}
		g.scale(zoom, zoom);
		return g;
	}

	/**
	 	Gets the map coordinates (Projection object) at the curso location.
	 	@param mousePoint location of mouse.
	 	@return the coordinates of the mouse releative to the map.
	 */
	public Point2D getScaledPoint( Point2D mousePoint ) {
		double x = mousePoint.getX();
		double y = mousePoint.getY();
		if(mapBorder != null) {
			Insets ins = mapBorder.getBorderInsets(this);
			x -= (double) ins.left;
			y -= ins.top;
		}
		x /= zoom;
		y /= zoom;
		return new Point2D.Double(x, y);
	}

	/**
	 * Gets the units of the current map overlay
	 * @return String representation of the units of the current map overlay
	 */
	public String getUnits() {
		return units;
	}

	/**
	 * Gets the data type of the current map overlay
	 * @return String representation of the data type of the current map overlay
	 */
	public String getDataType() {
		return dataType;
	}
	
	/**
	 * Saves current image.
	 */
	public void saveBaseMap() throws IOException {
		JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
		int ok = chooser.showSaveDialog(null);
		if(ok==chooser.CANCEL_OPTION) return;

		Insets ins = new Insets(0,0,0,0);
		Rectangle r = getVisibleRect();
		int w = width;
		int h = height;
		Font font = null;
		if(mapBorder != null) {
			ins = mapBorder.getBorderInsets(this);
			float scale = (float)width / (float)r.width;
			font = mapBorder.getFont();
			Font font1 = font.deriveFont(scale*(float)font.getSize());
			mapBorder.setFont(font1);
			ins = mapBorder.getBorderInsets(this);
			w += ins.left+ins.right;
			h += ins.bottom+ins.top;
		}
		BufferedImage im = new BufferedImage(w, h,
					BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = im.createGraphics();
		if(mapBorder != null) {
			// GMA 1.4.8: TESTING - Has no affect on borders
			mapBorder.paintBorder(this, g2, 0, 0, w, h);
			
			g2.clip( mapBorder.getInteriorRectangle(this, 
						0, 0, w, h) );
			g2.translate(ins.left, ins.top);
			mapBorder.setFont(font);
		}
		for( int i=0 ; i<overlays.size() ; i++) {
			if( overlays.get(i) instanceof MapOverlay ) {
				((MapOverlay)overlays.get(i)).draw(g2);
				break;
			}
		}
		BufferedOutputStream out = new BufferedOutputStream(
					new FileOutputStream( chooser.getSelectedFile()));
		//JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
		//encoder.encode(im);
		ImageIO.write(im, "JPEG", out);

		out.flush();
		out.close();
	}

	/**
	 * Saves current image.as a JPEG.
	 */
	public void saveJPEGImage(File file) throws IOException {
		Rectangle r = getVisibleRect();
		BufferedImage im = new BufferedImage( r.width, r.height, BufferedImage.TYPE_INT_RGB );
		Graphics2D g2d = im.createGraphics();
		g2d.translate(-r.getX(), -r.getY());
		paint(g2d);		
		int s_idx = file.getName().indexOf(".");
		String suffix = s_idx<0
				? "jpg"
				: file.getName().substring(s_idx+1);
		if( !javax.imageio.ImageIO.getImageWritersBySuffix(suffix).hasNext() )suffix = "jpg";
		javax.imageio.ImageIO.write( im, suffix, file);
	//	BufferedOutputStream out = new BufferedOutputStream(
	//				new FileOutputStream( file ));
	//	JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
	//	encoder.encode(im);
	}

//	***** GMA 1.6.6: Add option to save as .tiff image for high detail
	public void savePMGImage(File file) throws IOException {
		Rectangle r = getVisibleRect();
		BufferedImage im = new BufferedImage( r.width, r.height, BufferedImage.TYPE_INT_BGR );
		Graphics2D g2d = im.createGraphics();
		g2d.translate(-r.getX(), -r.getY());
		paint(g2d);
		int s_idx = file.getName().indexOf(".");
		String suffix = s_idx<0
				? "png"
				: file.getName().substring(s_idx+1);
		if( !javax.imageio.ImageIO.getImageWritersBySuffix(suffix).hasNext() ) {
			suffix = "png";
		}
		javax.imageio.ImageIO.write( (RenderedImage)im, suffix, file );
	}

	public void saveJPEGImage() throws IOException {
		JFileChooser chooser = MapApp.getFileChooser();
		int ok = chooser.showSaveDialog(getTopLevelAncestor());
		if(ok==chooser.CANCEL_OPTION) return;
		saveJPEGImage( chooser.getSelectedFile() );
	}

	/**
	 * Loads a JPEG.
	 */
	public byte[] getJPEGImage() throws IOException {
		Rectangle r = getVisibleRect();
		BufferedImage im = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = im.createGraphics();
		g2d.translate(-r.getX(), -r.getY());
		paint(g2d);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		//JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
		//encoder.encode(im);
		ImageIO.write(im, "JPEG", out);
		return out.toByteArray();
	}

	/**
	 * Gets the region currently displayed in Projection coordinates.
	 * @return the region currently displayed in Projection coordinates.
	 */
	public Rectangle2D getClipRect2D() {
		Rectangle r = getVisibleRect();
		Dimension dim = getPreferredSize();
		r.width = Math.min(r.width, dim.width);
		r.height = Math.min(r.height, dim.height);
		AffineTransform at = new AffineTransform();
		if(rotation==1) {
			at.translate( 0., dim.getHeight() );
			at.rotate(-.5*Math.PI);
		} else if( rotation==2 ) {
			at.translate( dim.getWidth(), dim.getHeight() );
			at.rotate( Math.PI );
		} else if( rotation == 3) {
			at.translate( dim.getWidth(), 0. );
			at.rotate( .5*Math.PI );
		}
		if(rotation != 0) {
			Point2D p1 = at.transform(new Point(r.x,r.y), null);
			Point2D p2 = at.transform(new Point(r.x+r.width,r.y+r.width), null);
			r.x = (int) Math.min(p1.getX(), p2.getX());
			r.width = (int) Math.max(p1.getX(), p2.getX()) - r.x;
			r.y = (int) Math.min(p1.getY(), p2.getY());
			r.height = (int) Math.max(p1.getY(), p2.getY()) - r.y;
		}

		if(mapBorder != null) {
			Insets ins = mapBorder.getBorderInsets(this);
			r.width -= ins.left + ins.right;
			r.height -= ins.top + ins.bottom;
		}
		Rectangle2D.Double r2d = new Rectangle2D.Double(
				r.getX()/zoom, r.getY()/zoom,
				r.getWidth()/zoom, r.getHeight()/zoom);
		return r2d;
	}
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}
	public int getScrollableUnitIncrement(Rectangle visibleRect,
				int orientation, int direction) {
		if( orientation == SwingConstants.VERTICAL) return 10;
		int newX = visibleRect.x + (direction>0 ? 10 : -10);
		if( wrap>0. ) {
			int dx = (int) (wrap*zoom);
			int test = getPreferredSize().width - visibleRect.width;
			while( newX<0 ) newX += dx;
			while( newX>test ) newX -= dx;
		}
		return (direction>0) ? (newX-visibleRect.x) : -(newX-visibleRect.x);
	}
	public int getScrollableBlockIncrement(Rectangle visibleRect,
				int orientation, int direction) {
		if( orientation == SwingConstants.VERTICAL) return visibleRect.height/2;
		int dx = visibleRect.width/2;
		int newX = visibleRect.x + (direction>0 ? dx : -dx);
		if( wrap>0. ) {
			dx = (int) (wrap*zoom);
			int test = getPreferredSize().width - visibleRect.width;
			while( newX<0 ) newX += dx;
			while( newX>test ) newX -= dx;
		}
		return (direction>0) ? (newX-visibleRect.x) : -(newX-visibleRect.x);
	}
	public boolean getScrollableTracksViewportWidth() { return false; }
	public boolean getScrollableTracksViewportHeight() { return false; }

	/**
	 	Paints designated graphics.
	 	@param g what to paint.
	 */
	public void paintComponent( Graphics g ) {
		MapApp mApp = (MapApp) app;
		Graphics2D g2 = (Graphics2D)g;
		Rectangle clip = g.getClipBounds();
		Dimension dim = getPreferredSize();
		Rectangle r = getVisibleRect();
		if(r.width>dim.width) r.width = dim.width;
		if(r.height>dim.height) r.height = dim.height;
		AffineTransform at = g2.getTransform();
		Rectangle interior = new Rectangle();
		if(mapBorder != null) {
			mapBorder.paintBorder(this, g, r.x, r.y, r.width, r.height);
			interior = mapBorder.getInteriorRectangle(this,
					r.x, r.y, r.width, r.height);
			g2.clip( interior );
			Insets ins = mapBorder.getBorderInsets(this);
			g2.translate(ins.left, ins.top);
		}
// Mac fix?
		BufferedImage im = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = im.createGraphics();
		g2d.setRenderingHint( RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		// GMA 1.4.8: TESTING
		g2d.setColor( Color.white );

		g2d.fillRect( 0, 0, r.width, r.height);
		g2d.translate( -r.x, -r.y );
		g2d.scale(zoom, zoom);
		int i=0;
		if(image != null && mApp.isBaseMapVisible())
			g2d.drawImage(image, 0, 0, this);

		while( i<overlays.size() && overlays.get(i) instanceof MapOverlay ) {
			Overlay overlay = overlays.get(i);
			if (overlay == mApp.baseMap) {
				i++;
				continue;
			}

			if ( mApp.isLayerVisible(overlay) ) {
				float alpha = getAlpha(overlay);
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));;
				
				if (overlay == mApp.baseMapFocus) {
					mApp.baseMap.draw(g2d);
				}
				overlay.draw(g2d);
			}
			i++;
		}
		g2.setPaintMode();
		if( image != null || i!=0 ) {
			g2.translate( r.x, r.y);
			g2.drawImage( im, 0, 0, this);
			g2.translate( -r.x, -r.y);
		}
		g2.scale(zoom, zoom);
		int i0=i;

		for( i=i0 ; i<overlays.size() ; i++) {
			Overlay overlay = overlays.get(i);
			if (overlay == mApp.baseMap) continue;

			if (mApp.isLayerVisible( overlay)) {
				float alpha = getAlpha(overlay);
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

				if (overlay == mApp.baseMapFocus) {
					mApp.baseMap.draw(g2);
				}
				overlay.draw(g2);
			}
		}
		mApp.baseMap.drawMask(g2);
		
		g2.setPaintMode();
		if( graticule && mapBorder instanceof PolarMapBorder) ((PolarMapBorder)mapBorder).draw(g2);
		if( mapInsets==null || mapInsets.size()==0 || !includeInsets ) {
			return;
		}	
		g2.setTransform(at);
		g2.setClip( getVisibleRect() );
		g2.translate( r.x, r.y );
		interior = getVisibleRect();
		for( i=0 ; i<mapInsets.size() ; i++) {
			((MapInset)mapInsets.get(i)).draw( g2, interior.width, interior.height );
		}
	}

	/**
	 * Prints the current view.
	 * @param g What to print.
	 * @param fmt Page format.
	 * @param pageNo Page Number.
	 * @return if the page exists.
	 */
	public int print( Graphics g, PageFormat fmt, int pageNo ) {
		if( pageNo>0 ) return NO_SUCH_PAGE;
		Graphics2D g2 = (Graphics2D)g;
		Dimension dim = getPreferredSize();
		Rectangle r = getVisibleRect();
	//	r.height += 20;
		if(r.width>dim.width) r.width = dim.width;
		if(r.height>dim.height) r.height = dim.height;
		org.geomapapp.util.DateFmt df = new org.geomapapp.util.DateFmt();
		int secs = (int)(System.currentTimeMillis()/1000L);
		String date = df.format(secs);
		Font font = new Font("SansSerif", Font.PLAIN, 8);
		g.setFont( font );
		Rectangle2D r2d = font.getStringBounds(date, g2.getFontRenderContext());
	//	g2.translate( r.getWidth()-20.-r2d.getWidth(), r.getHeight()+18. );
		g2.setColor( Color.black);

	//	g.setClip( new Rectangle( 0, 0, r.width, r.height) );
		double w = fmt.getImageableWidth();
		double h = fmt.getImageableHeight();
		double x = fmt.getImageableX();
		double y = fmt.getImageableY();
		g2.translate(x, y);
		double scale = Math.min( w / r.getWidth(), h / r.getHeight());
		int xd = (int)(scale*r.getWidth()-10.-r2d.getWidth());
		int yd = (int)(scale*r.getHeight()+18.);
		g2.drawString( date, xd, yd);
		g2.translate( -r.getX()*scale, -r.getY()*scale );
		g2.scale( scale, scale);
//	System.out.println(x +"\t"+ y +"\t"+ w +"\t"+ h  +"\t"+ zoom
//			 +"\t"+ scale +"\t"+ r.getWidth() +"\t"+ r.getHeight());
		if(mapBorder != null) {
//			GMA 1.4.8: TESTING - Commenting this out does not affect border
			mapBorder.paintBorder(this, g, r.x, r.y, r.width, r.height);

			g2.clip( mapBorder.getInteriorRectangle(this, 
						r.x, r.y, r.width, r.height));
			Insets ins = mapBorder.getBorderInsets(this);
			g2.translate(ins.left, ins.top);
		}
		g2.scale(zoom, zoom);
		if(image != null)g2.drawImage(image, 0, 0, this);
		for(int i=0 ; i<overlays.size() ; i++) {
			Overlay overlay = (Overlay)overlays.get(i);
			boolean isMap = overlay instanceof MapOverlay;
			if( !isMap )continue;
			overlay.draw(g2);
		}
		if( graticule && mapBorder instanceof PolarMapBorder) ((PolarMapBorder)mapBorder).draw(g2);
		for(int i=0 ; i<overlays.size() ; i++) {
			Overlay overlay = (Overlay)overlays.get(i);
			boolean isMap = overlay instanceof MapOverlay;
			if( isMap )continue;
			overlay.draw(g2);
		}
		return PAGE_EXISTS;
	}

	public float getAlpha(Overlay overlay) {
		if (overlay == null) return 0;

		// This ties the baseMap and the baseMapFocus together
		if (overlay == ((MapApp) app).baseMap)
			return overlayAlphas.get( 
					((MapApp) app).baseMapFocus);

		Float alpha = overlayAlphas.get(overlay);
		return alpha == null ? 0 : alpha;
	}

	public Object getApp() {
		return app;
	}

	public void setBaseCursor(Cursor cursor) {
		baseCursor = cursor;
	}

	public void setCursor(Cursor cursor) {
		if (cursor.equals(Cursor.getDefaultCursor()))
			super.setCursor( ((MapApp)app).getMapTools().getCurrentCursor() );
		else
		super.setCursor(cursor);
	}

	public void adjustmentValueChanged(AdjustmentEvent e) {
	}

	// Pans the map if user holds space bar down while navigating with pointer.
	public void keyPressed(KeyEvent e) {
		if ( e.getKeyCode() == KeyEvent.VK_SPACE) {
			setSpaceBarPress(true);
		}

		if(getSpaceBarPress()) {
			// Set locations
			screenPressedX = MouseInfo.getPointerInfo().getLocation().x;
			screenPressedY = MouseInfo.getPointerInfo().getLocation().y;
			pressedX = scrollPane.getHorizontalScrollBar().getValue();
			pressedY = scrollPane.getVerticalScrollBar().getValue();

			// Set cursor to closed hand
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			ClassLoader loader = org.geomapapp.util.Icons.class.getClassLoader();
			String path = "org/geomapapp/resources/icons/close_hand.png";
			java.net.URL url = loader.getResource(path);
			try {
				BufferedImage im = ImageIO.read(url);
				Cursor closeHandCursor = toolkit.createCustomCursor( im, new Point(0,0), "close_hand");
				setCursor(closeHandCursor);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void keyReleased(KeyEvent e) {
		if ( e.getKeyCode() == KeyEvent.VK_SPACE) {
			setSpaceBarPress(false);
		}
		// If pan button in toolbar is selected replace with open hand cursor
		if(((MapApp)app).tools.panB.isSelected() ){
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			ClassLoader loader = org.geomapapp.util.Icons.class.getClassLoader();
			String pathB = "org/geomapapp/resources/icons/open_hand.png";
			java.net.URL url = loader.getResource(pathB);
			setSpaceBarPress(false);
			try {
				BufferedImage im = ImageIO.read(url);
				Cursor openHandCursor = toolkit.createCustomCursor( im, new Point(0,0), "close_hand");
				setCursor(openHandCursor);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		else if(!getSpaceBarPress()){
			setCursor(Cursor.getDefaultCursor());
			setSpaceBarPress(false);
		}
	}

	public void keyTyped(KeyEvent e) {
	}

	public void mouseDragged(MouseEvent e) {

		// GMA 1.6.4: Perform pan while "Pan" button is selected by performing a centering on the point that is dragged to.
		// Pan with right click of mouse equivalent of modifier 4
		if ( (((MapApp)app).tools.panB.isSelected() && e.getModifiers()==16) ||
				((e.getModifiers()==4)) ) {
			// Set cursor to closed hand while drag
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			ClassLoader loader = org.geomapapp.util.Icons.class.getClassLoader();
			String path = "org/geomapapp/resources/icons/close_hand.png";
			java.net.URL url = loader.getResource(path);
			try {
				BufferedImage im = ImageIO.read(url);
				Cursor closeHandCursor = toolkit.createCustomCursor( im, new Point(0,0), "close_hand");
				setCursor(closeHandCursor);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			int newX = pressedX - ( MouseInfo.getPointerInfo().getLocation().x - screenPressedX );
			if (wrap > 0) {
				double dx = wrap * zoom;
				while (newX < 0) {
					newX += dx;
				}
				int test = getPreferredSize().width - getVisibleRect().width;
				while (newX > test)
					newX -= dx;
			}
			scrollPane.getHorizontalScrollBar().setValue( newX );
			/*if ( screenPressedX < MouseInfo.getPointerInfo().getLocation().x ) {
				scrollPane.getHorizontalScrollBar().setValue( newX );
			}
			else {
				scrollPane.getHorizontalScrollBar().setValue( newX );
			} */
			if ( screenPressedY < MouseInfo.getPointerInfo().getLocation().y ) {
				scrollPane.getVerticalScrollBar().setValue( pressedY - ( MouseInfo.getPointerInfo().getLocation().y - screenPressedY ) );
			}
			else {
				scrollPane.getVerticalScrollBar().setValue( pressedY + ( screenPressedY - MouseInfo.getPointerInfo().getLocation().y ) );
			}
			revalidate();
			repaint();
		} 
		else {
			setSpaceBarPress(false);
		}
		// Request focus on so that KeyEvents work again
		requestFocusInWindow();
	}

	public void mouseMoved(MouseEvent e) {
		//check the profile button in the main tool bar
		if (((MapApp)app).tools.profileB.isSelected()) {
			setCursor(Cursor.getDefaultCursor());			
		}

		spaceBarDown = getSpaceBarPress();
		// Do actions if space bar is down and the mouse is moving.
		if(spaceBarDown) {
			int newX = pressedX - ( MouseInfo.getPointerInfo().getLocation().x - screenPressedX );
			if (wrap > 0) {
				double dx = wrap * zoom;
				while (newX < 0) {
					newX += dx;
				}
				int test = getPreferredSize().width - getVisibleRect().width;
				while (newX > test)
					newX -= dx;
			}
			scrollPane.getHorizontalScrollBar().setValue( newX );
			if ( screenPressedY < MouseInfo.getPointerInfo().getLocation().y ) {
				scrollPane.getVerticalScrollBar().setValue( pressedY - ( MouseInfo.getPointerInfo().getLocation().y - screenPressedY ) );
			}
			else {
				scrollPane.getVerticalScrollBar().setValue( pressedY + ( screenPressedY - MouseInfo.getPointerInfo().getLocation().y ) );
			}
			revalidate();
			repaint();
			// Request focus on click so that KeyEvents work again
			requestFocusInWindow();
		}
	}
	public void mouseClicked(MouseEvent e) {
		// GMA 1.6.4: Center on spot where user double-clicks (but not if Digitizer or Survey Planner are open)
		if ( e.getClickCount() >= 2 && e.getModifiers()==16 && 
				!((MapApp)app).digitizer.isEnabled() && !((MapApp)app).db[10].isEnabled()) {
			Point p = e.getPoint();
			doZoom( p, 1 );
		}
	}

	public void mouseEntered(MouseEvent e) {
		requestFocusInWindow(); // To activate Key Events
	}

	public void mouseExited(MouseEvent e) {
	}
	public void mousePressed(MouseEvent e) {
//		***** GMA 1.6.4: Record screen position when user presses mouse button
//		System.out.println("Pressed value: " + e.getX());
//		System.out.println("Pressed value: " + e.getY());
//		pressedX = e.getX();
//		pressedY = e.getY();
		pressedX = scrollPane.getHorizontalScrollBar().getValue();
		pressedY = scrollPane.getVerticalScrollBar().getValue();
		screenPressedX = MouseInfo.getPointerInfo().getLocation().x;
		screenPressedY = MouseInfo.getPointerInfo().getLocation().y;

		// Set cursor to closed hand while right button pressed
		if(((MapApp)app).tools.panB.isSelected() ) {
			if (e.getModifiers()==4 || e.getModifiers()==16) {
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				ClassLoader loader = org.geomapapp.util.Icons.class.getClassLoader();
				String path = "org/geomapapp/resources/icons/close_hand.png";
				java.net.URL url = loader.getResource(path);
				try {
					BufferedImage im = ImageIO.read(url);
					Cursor closeHandCursor = toolkit.createCustomCursor( im, new Point(0,0), "close_hand");
					setCursor(closeHandCursor);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} else {
				setSpaceBarPress(false);
				setCursor(Cursor.getDefaultCursor());
			}
		}
		
		//check Profile button in the main tool bar
		if (((MapApp)app).tools.profileB.isSelected()) {
			setCursor(Cursor.getDefaultCursor());
		}
		
		// Request focus on click so that KeyEvents work again
		requestFocusInWindow();
	}
	public void mouseReleased(MouseEvent e) {
		
		// If pan button in toolbar is selected replace with open hand cursor
		if(((MapApp)app).tools.panB.isSelected() ){
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			ClassLoader loader = org.geomapapp.util.Icons.class.getClassLoader();
			String pathB = "org/geomapapp/resources/icons/open_hand.png";
			java.net.URL url = loader.getResource(pathB);
			try {
				BufferedImage im = ImageIO.read(url);
				Cursor openHandCursor = toolkit.createCustomCursor( im, new Point(0,0), "close_hand");
				setCursor(openHandCursor);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} // Otherwise set to the default cursor
		else {
			setCursor(Cursor.getDefaultCursor());
		}
	}

	public void setSpaceBarPress(boolean a){
		spaceBarDown = a;
	}

	public boolean getSpaceBarPress(){
		return spaceBarDown;
	}

	public Double getZoomValue() {
		Double zv = getZoom();
		return zv;
	}

	public Double getZoomValueX() {
		return null;
	}

	public Double getZoomValueY() {
		return null;
	}
	
	public Vector<Overlay> getOverlays() {
		return overlays;
	}
}
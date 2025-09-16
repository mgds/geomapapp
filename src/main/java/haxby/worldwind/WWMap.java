package haxby.worldwind;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.terrain.Tessellator;
import gov.nasa.worldwind.view.orbit.FlyToOrbitViewAnimator;
import gov.nasa.worldwind.view.orbit.OrbitView;
import haxby.map.MapApp;
import haxby.map.Overlay;
import haxby.map.XMap;
import haxby.proj.ConstrainedIdentityProjection;
import haxby.worldwind.globe.MYEBSRectangularTessellator;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import javax.swing.SwingUtilities;

import org.geomapapp.util.XML_Menu;

import com.jogamp.opengl.util.awt.Screenshot;

public class WWMap extends XMap {

	BufferedImage scratch = new BufferedImage(10,10,BufferedImage.TYPE_4BYTE_ABGR);
	WorldWindowGLCanvas wwd;
	Sector visibleSector;
	RenderableLayer layer;

	public WWMap(MapApp app, WorldWindowGLCanvas wwd) {
		super(app, new ConstrainedIdentityProjection(), 1, 1);
		this.wwd = wwd;
		visibleSector = Sector.FULL_SPHERE;

		wwd.getSceneController().addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals(AVKey.VIEW)) {
					checkVisibleSector();
				}
			}
		});
	}

	public Container getParent() {
		return wwd.getParent();
	}

	public Rectangle2D getClipRect2D() {
		if (visibleSector == null)
			return new Rectangle2D.Double(-180,-90,360,180);
		else
			return new Rectangle2D.Double(
					visibleSector.getMinLongitude().degrees,
					visibleSector.getMinLatitude().degrees,
					visibleSector.getDeltaLonDegrees(),
					visibleSector.getDeltaLatDegrees());
	}

	public Graphics2D getGraphics2D() {
		Graphics2D g = scratch.createGraphics();
		g.setClip(getClipRect2D());
		return g;
	}

	public int removeOverlay(Overlay overlay) {
		if (overlay instanceof WWOverlay) {
			wwd.getModel().getLayers().remove(((WWOverlay)overlay).getLayer());
			wwd.removeSelectListener(((WWOverlay)overlay).getSelectListener());
		}
		return super.removeOverlay(overlay);
	}

	public void addOverlay(Overlay overlay) {
		addOverlay("Overlay", overlay, 0, null, true);
	}

	@Override
	public void addOverlay(int index, Overlay overlay) {
		addOverlay("Overlay", overlay, index, null, true);
	}

	@Override
	public void addOverlay(int index, Overlay overlay, boolean fireChange) {
		addOverlay("Overlay", overlay, index, null, fireChange);
	}

	@Override
	public void addOverlay(Overlay overlay, boolean fireChange) {
		addOverlay("Overlay", overlay, 0, null, fireChange);
	}

	@Override
	public void addOverlay(String name, Overlay overlay, boolean fireChange) {
		addOverlay(name, overlay, 0, null, fireChange);
	}

	@Override
	public void addOverlay(String name, Overlay overlay, int index,
			String infoURLString, boolean fireChange) {
		addOverlay( name, overlay, index, infoURLString, fireChange, null);
	}

	@Override
	public void addOverlay( String name, Overlay overlay, boolean fireChange, XML_Menu xml_item ) {
		addOverlay(name, overlay, overlays.size(), null, fireChange, xml_item);
	}

	@Override
	public void addOverlay( String name, Overlay overlay, int index, String infoURLString, boolean fireChange, XML_Menu xml_item) {
		super.addOverlay(name, overlay, index, infoURLString, fireChange, xml_item);
		if (overlay instanceof WWOverlay) {
			WWOverlay wwOverlay = (WWOverlay)overlay;
			Layer layer = wwOverlay.getLayer();

			if (layer instanceof WWLayer)
				((WWLayer) layer).setInfoURL(infoURLString);

			LayerList layers = wwd.getModel().getLayers();
			if (layers.contains(layer)) return;

			layers.add( layer );
			wwd.addSelectListener( wwOverlay.getSelectListener() );
		}
	}

	@Override
	public void addOverlay(String name, Overlay overlay, int index) {
		addOverlay(name, overlay, index, null, true);
	}

	@Override
	public void addOverlay(String name, Overlay overlay) {
		addOverlay(name, overlay, 0, null, true);
	}

	@Override
	public void addOverlay(String name, String infoURLString, Overlay overlay) {
		addOverlay(name, overlay, 0, infoURLString, true);
	}

	public double getZoom() {
		return 1;
	}

	public void repaint() {
		wwd.getModel().firePropertyChange(AVKey.LAYER, null, this);
	}

	public Rectangle getVisibleRect() {
		Rectangle2D rect = getClipRect2D();
		return new Rectangle((int)rect.getX(),(int)rect.getY(), (int) rect.getWidth(), (int) rect.getHeight());
	}
	
	public double getWrap() {
		return 0;
	}

	public double[] getWESN() {
		return new double[] { visibleSector.getMinLongitude().degrees,
				visibleSector.getMaxLongitude().degrees,
				visibleSector.getMinLongitude().degrees,
				visibleSector.getMaxLongitude().degrees };
	}

	public void doZoom(Point2D p, double factor) {
		LatLon dest = new LatLon(Angle.fromDegrees(p.getY()), Angle.fromDegrees(p.getX()));
		double wwzoom = 2785 * Math.pow(factor, -.8311) * 10000;

		final OrbitView view = (OrbitView) wwd.getView();

		FlyToOrbitViewAnimator fto = 
			FlyToOrbitViewAnimator.createFlyToOrbitViewAnimator(
				view,
				view.getCenterPosition(), new Position(dest, 0),
				view.getHeading(), Angle.fromDegrees(0),
				view.getPitch(), Angle.fromDegrees(0),
				view.getZoom(), wwzoom,
				5000, WorldWind.CONSTANT); //was true

		view.addAnimator(fto);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				((MapApp)getApp()).getFrame().toFront();
				view.firePropertyChange(AVKey.VIEW,  null, view);
			}
		});
	}

	public void zoomIn(Point p) {
		doZoom(p, 1);
	}

	public void zoomOut(Point p) {
		doZoom(p, 1);
	}

	public void zoomTo(Rectangle rect) {
		doZoom(new Point2D.Double(rect.getCenterX(),rect.getCenterY()), 1);
	}

	public void zoomToRect(Rectangle2D rect) {
		doZoom(new Point2D.Double(rect.getCenterX(),rect.getCenterY()), 1);
	}

	public void setRect(Rectangle rect) {
	}

	public void saveJPEGImage(File file) throws IOException {
		Rectangle r = wwd.getBounds();
		wwd.getContext().makeCurrent();
		Screenshot.writeToFile(file, r.width, r.height);
		wwd.getContext().release();
	}

	public void savePMGImage(File file) throws IOException {
		Rectangle r = wwd.getBounds();
		wwd.getContext().makeCurrent();
		Screenshot.writeToFile(file, r.width, r.height);
		wwd.getContext().release();
	}

	public int print(Graphics g, PageFormat fmt, int pageNo) {
		if( pageNo>0 ) return NO_SUCH_PAGE;
		Graphics2D g2 = (Graphics2D)g;
		Dimension dim = wwd.getPreferredSize();
		Rectangle r = wwd.getBounds();

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

		wwd.getContext().makeCurrent();
		BufferedImage image = Screenshot.readToBufferedImage(r.width, r.height);
		g2.drawImage(image, 0, 0, this);
		return PAGE_EXISTS;
	}

	public void checkVisibleSector() {
		Sector newVisibleSector;

		Tessellator t = wwd.getModel().getGlobe().getTessellator();

		if (t instanceof MYEBSRectangularTessellator)
			newVisibleSector = ((MYEBSRectangularTessellator) t).getCurrentCoverage();
		else 
			return;

		if (newVisibleSector != null && !newVisibleSector.equals(visibleSector)) {
			visibleSector = newVisibleSector;
			new Thread() {
				public void run() {
					for (Object overlay : overlays) {
						if (overlay instanceof WWOverlay)
							((WWOverlay) overlay).setArea(getClipRect2D());
					}
				}
			}.start();
		}
	}

	public double getWWZoom() {
		return ((OrbitView) wwd.getView()).getZoom();
	}

	public double getGMAZoom() {
		return Math.pow(getWWZoom() / 10000 / 2785, 1 / -.8311);
	}

	public void setBaseCursor(Cursor cursor) {
		super.setBaseCursor(cursor);
		setCursor(cursor);
	}

	public void setCursor(Cursor cursor) {
		super.setCursor(cursor);
		wwd.setCursor(cursor);
	}

	public void zoomToWESN(double[] wesn) {
		double delta_x = wesn[1] - wesn[0];
		double delta_y = wesn[3] - wesn[2];

		double earthRadius = wwd.getModel().getGlobe().getRadius();

		double horizDistance = earthRadius * delta_x;
		double vertDistance = earthRadius * delta_y;

		// Form a triangle consisting of the longest distance on the ground and the ray from the eye to the center point 
		// The ray from the eye to the midpoint on the ground bisects the FOV
		double distance = Math.max(horizDistance, vertDistance) / 64;
		double altitude = distance / Math.tan(wwd.getView().getFieldOfView().radians / 2);

		LatLon latlon = LatLon.fromDegrees(wesn[2] + delta_y / 2, wesn[0] + delta_x / 2);
		Position pos = new Position(latlon, altitude);
		final OrbitView view = (OrbitView) wwd.getView();
		Position oldPos = view.getEyePosition();
		view.setEyePosition(pos);

		Position center = view.getCenterPosition();
		Angle heading = view.getHeading();
		Angle pitch = view.getPitch();
		double zoom = view.getZoom();
		view.setEyePosition(oldPos);

		FlyToOrbitViewAnimator fto = 
			FlyToOrbitViewAnimator.createFlyToOrbitViewAnimator(
				view,
				view.getCenterPosition(), center,
				view.getHeading(), heading,
				view.getPitch(), pitch,
				view.getZoom(), zoom,
				5000, WorldWind.CONSTANT); //was true

		view.addAnimator(fto);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				((MapApp)getApp()).getFrame().toFront();
				view.firePropertyChange(AVKey.VIEW,  null, view);
			}
		});
	}
}

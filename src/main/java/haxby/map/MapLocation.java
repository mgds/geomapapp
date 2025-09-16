package haxby.map;

import haxby.proj.*;
import java.awt.*;
import java.awt.geom.*;

public class MapLocation {
	public double lon, lat, zoom;
	public String name;
	Rectangle2D.Double bounds;
	double wrap;
	boolean selected;
	static Font font = new Font("SansSerif", Font.PLAIN, 10);
	public MapLocation( String name, double lon, double lat, double zoom ) {
		this.name = name;
		this.lon = lon;
		this.lat = lat;
		this.zoom = zoom;
		selected = false;
	}
	public String toString() {
		return name;
	}
	public void draw(Graphics2D g, XMap map) {
		if(zoom<2.)return;
	//	Rectangle rect = map.getVisibleRect();
		Dimension dim = map.getParent().getSize();
		Projection proj = map.getProjection();
		Point2D p = proj.getMapXY(new Point2D.Double(lon, lat));
		bounds = new Rectangle2D.Double(
				p.getX()-.46*dim.getWidth()/zoom,
				p.getY()-.46*dim.getHeight()/zoom,
				.92*dim.getWidth()/zoom,
				.92*dim.getHeight()/zoom );
		wrap = map.getWrap();
		Rectangle2D.Double r = (Rectangle2D.Double)map.getClipRect2D();
		g.setColor( selected ? Color.white : Color.black);
		g.setStroke( new BasicStroke(1f/(float)map.getZoom()));
		float size = 9f/(float)map.getZoom();
		g.setFont( font.deriveFont( size));
		if( wrap>0 ) {
			while( bounds.x>r.x+r.width)bounds.x-=wrap;
			while( bounds.x+bounds.width<r.x)bounds.x+=wrap;
			while( bounds.x<r.x+r.width) {
				g.draw(bounds);
				g.drawString(name, 
					.1f*size+(float)(bounds.x),
					-.2f*size+(float)(bounds.y));
				bounds.x+=wrap;
			}
		} else {
			g.draw(bounds);
			g.drawString(name, 
				.5f*size+(float)(bounds.x+bounds.width),
				.5f*(float)(bounds.y + bounds.height));
		}
	}
	public void unselect(XMap map) {
		if( !selected) return;
		selected=false;
		synchronized(map.getTreeLock() ) {
			draw(map.getGraphics2D(), map);
		}
	}
	boolean select( XMap map, Point2D p) {
		if(zoom<2.)return false;
		if( wrap>0 ) {
			while( bounds.x>p.getX())bounds.x-=wrap;
			while( bounds.x+bounds.width<p.getX())bounds.x+=wrap;
		}
		boolean tf = bounds.contains(p.getX(), p.getY());
		if( tf!=selected ) {
			selected = tf;
			synchronized(map.getTreeLock() ) {
				draw(map.getGraphics2D(), map);
			}
		}
		return tf;
	}
}

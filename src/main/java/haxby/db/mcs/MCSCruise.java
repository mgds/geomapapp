package haxby.db.mcs;

import haxby.map.*;
import haxby.proj.*;
import java.sql.Timestamp;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;

public class MCSCruise implements Overlay {
	MCS mcs;
	XMap map;
	String id;
	Timestamp startTime;
	Vector lines;
	double[] wesn;
	Rectangle2D.Double bounds;
	public MCSCruise(MCS mcs, XMap map, String id, Timestamp start, double[] wesn) {
		this.map = map;
		this.id = id.trim();
		this.wesn = wesn;
		startTime = start;
		lines = new Vector();
		setMap(map);
	}
	public void setMap(XMap map) {
		if( map!=null ) {
			bounds = new Rectangle2D.Double();
			Point2D p = map.getProjection().getMapXY(new Point2D.Double(wesn[0], wesn[3]));
			bounds.x = p.getX()-.5;
			bounds.y = p.getY()-.5;
			p = map.getProjection().getMapXY(new Point2D.Double(wesn[1], wesn[2]));
			bounds.width  = p.getX() - bounds.x+.5;
			bounds.height = p.getY() - bounds.y+.5;
		}
		for(int i=0 ; i<lines.size() ; i++) ((MCSLine)lines.get(i)).setMap(map);
	}
	public String getID() {
		return new String(id);
	}
	public String toString() {
		return getID();
	}
	public void addLine(MCSLine line) {
		lines.add(line);
	}
	public MCSLine[] getLines() {
		MCSLine[] tmp = new MCSLine[lines.size()];
		for(int i=0 ; i<lines.size() ; i++) tmp[i] = (MCSLine)lines.get(i);
		return tmp;
	}
	public void draw(Graphics2D g) {
		if(map==null)return;
		double wrap = map.getWrap();
		AffineTransform at = g.getTransform();
		double offset = 0.;
		Rectangle rect = g.getClipBounds();
		while( bounds.x+offset > rect.getX() ) offset -= wrap;
		while( bounds.x+bounds.width+offset < rect.getX() ) offset += wrap;
		if( bounds.x+offset > rect.getX()+rect.getWidth() ) return;
		g.translate( offset, 0.);
		g.draw(bounds);
		while( bounds.x +offset < rect.getX()+rect.getWidth() ) {
			offset += wrap;
			g.translate( wrap, 0.);
			g.draw(bounds);
		}
		g.setTransform(at);
	}
	public void drawLines(Graphics2D g) {
		if(map==null)return;
	//	if( !g.getClipBounds().intersects(bounds)) return;
		double wrap = map.getWrap();
		AffineTransform at = g.getTransform();
		double offset = 0.;
		Rectangle rect = g.getClipBounds();
		while( bounds.x+offset > rect.getX() ) offset -= wrap;
		while( bounds.x+bounds.width+offset < rect.getX() ) offset += wrap;
		if( bounds.x+offset > rect.getX()+rect.getWidth() ) return;
		g.translate( offset, 0.);
		for(int i=0 ; i<lines.size() ; i++) {
			MCSLine line = (MCSLine)lines.get(i);
			line.draw(g);
		}
		while( bounds.x +offset < rect.getX()+rect.getWidth() ) {
			offset += wrap;
			g.translate( wrap, 0.);
			for(int i=0 ; i<lines.size() ; i++) {
				MCSLine line = (MCSLine)lines.get(i);
				line.draw(g);
			}
		}
		g.setTransform(at);
	}
	public boolean contains( double x, double y, double wrap ) {
		if( wrap<=0. ) return bounds.contains( x, y);
		if( y<bounds.y || y>bounds.y+ bounds.height ) return false;
		double offset = 0;
		while( bounds.x+offset > x ) offset-=wrap;
		while( bounds.x+bounds.width+offset < x ) offset+=wrap;
		if( bounds.x <= x ) return true;
		return false;
	}
	public Rectangle2D getBounds() {
		if(map==null) return new Rectangle();
		return new Rectangle2D.Double( bounds.x, bounds.y, bounds.width, bounds.height );
	}
}

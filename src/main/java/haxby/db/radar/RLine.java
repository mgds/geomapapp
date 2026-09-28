package haxby.db.radar;

import haxby.db.mcs.*;
import haxby.map.*;
import haxby.proj.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.swing.*;

public class RLine 
		implements Overlay {

	RCruise cruise;
	String lineID;

	XMap map;
	CDP[] cdp;
	Point2D[] points;
	Shape currentShape = null;
	Rectangle2D.Double bounds;
	Vector crossings;
	double cdpSpacing;
	double[] cdpRange;
	double[] zRange;

//	***** GMA 1.6.6: Add dot to display for Macs
	
	Shape dot = null;
	Shape prevDot = null;
//	***** GMA 1.6.6
	
	protected RLine() {
		cruise = null;
		cdp = null;
		map = null;
		points = null;
		lineID = "";
		crossings = new Vector();
		cdpRange = null;
		zRange = null;
	}
	public RLine( RCruise cruise, String lineID) {
		this();
		this.lineID = lineID;
		this.cruise = cruise;
	}
	public RLine( XMap map, RCruise cruise, String lineID, CDP[] cdp) {
		this(cruise, lineID);
		this.map = map;
		this.cdp = cdp;
		bounds = new Rectangle2D.Double();
		setMap(map);
	}
	public void setRanges( double[] cdpRange, double[] zRange ) {
		this.cdpRange = cdpRange;
		this.zRange = zRange;
	}
	public double[] getZRange() {
		return zRange;
	}
	public double[] getCDPRange() {
		return cdpRange;
	}
	public double getCDPSpacing() { // *******
		return cdpSpacing;
	}
	public Point2D pointAtCDP( int cdpN ) {
		int i=0;
		if( cdpN<cdp[0].number() ) cdpN=cdp[0].number();
		if( cdpN>cdp[cdp.length-1].number() ) cdpN=cdp[cdp.length-1].number();
		while( i<cdp.length-2 && cdpN>=cdp[i+1].number() ) i++;
		double dx = (double) (cdpN-cdp[i].number()) / (double)(cdp[i+1].number()-cdp[i].number());
		Point2D.Double p = new Point2D.Double(
				points[i].getX() + dx*(points[i+1].getX()-points[i].getX()),
				points[i].getY() + dx*(points[i+1].getY()-points[i].getY()));
		return p;
	}
	public void drawCDP( int cdpN ) {
		if( currentShape==null &&cdpN==-1 ) return;
		GeneralPath shape = new GeneralPath();
		float zoom = (float)map.getZoom();
		if( cdpN==-1 ) {
			shape = null;
		} else {
			Point2D p = pointAtCDP( cdpN );
			shape.moveTo( (float)p.getX()-2f/zoom, (float)p.getY() );
			shape.lineTo( (float)p.getX()-16f/zoom, (float)p.getY() );
			shape.moveTo( (float)p.getX()+2f/zoom, (float)p.getY() );
			shape.lineTo( (float)p.getX()+16f/zoom, (float)p.getY() );
			shape.moveTo( (float)p.getX(), (float)p.getY()-2f/zoom );
			shape.lineTo( (float)p.getX(), (float)p.getY()-16f/zoom);
			shape.moveTo( (float)p.getX(), (float)p.getY()+2f/zoom );
			shape.lineTo( (float)p.getX(), (float)p.getY()+16f/zoom);
		}
		synchronized( map.getTreeLock() ) {
			Graphics2D g = map.getGraphics2D();
			g.setXORMode( Color.white );
			g.setStroke( new BasicStroke( 2f/zoom ));
			if( currentShape!=null ) g.draw(currentShape);
			currentShape = shape;
			if( currentShape!=null ) g.draw(currentShape);
			
//			***** GMA 1.6.6: Display for Macs
			
			String osName = System.getProperty("os.name"); 
			if ( osName.startsWith("Mac OS") ) {
				g = map.getGraphics2D();
				g.setStroke( new BasicStroke( 2f/(float)map.getZoom() ) );
				double size = 7./map.getZoom();
				Point2D p = pointAtCDP( cdpN );
				prevDot = dot;
				dot = (Shape)( new Arc2D.Double( p.getX(), p.getY(),  size/6, size/6, 0., 360., Arc2D.CHORD ) );
				if ( prevDot != null ) {
					g.setColor(Color.red);
					g.draw(prevDot);
				}
				g.setColor(Color.white);
				g.draw(dot);
			}
//			***** GMA 1.6.6
			
		}
	}
	public void drawSeg( int cdp1, int cdp2) {
		synchronized(map.getTreeLock()) {
			Graphics2D g = map.getGraphics2D();
			g.setStroke(new BasicStroke(2f/(float)map.getZoom()));
			
//			***** GMA 1.6.6: On Macs, make the lines thicker so that alternate display method can be used
//			g.setStroke(new BasicStroke(2f/(float)map.getZoom()));
			
			String osName = System.getProperty("os.name"); 
			if ( osName.startsWith("Mac OS") ) {
				g.setStroke(new BasicStroke(5f/(float)map.getZoom()));
			}
			else {
				g.setStroke(new BasicStroke(2f/(float)map.getZoom()));
			}
//			***** GMA 1.6.6
			
			drawSeg( cdp1, cdp2, g);
		}
	}
	public void drawSeg( int cdp1, int cdp2, Graphics2D g) {
		g.setColor(Color.yellow);
		draw(g);
		g.setColor(Color.red);
		GeneralPath path = new GeneralPath();
		Point2D p = pointAtCDP( cdp1 );
		path.moveTo( (float)p.getX(), (float)p.getY() );
		int k=0;
		while( k<cdp.length-1 && cdp[k].number() < cdp1 ) k++;
		while( k<cdp.length-1 && cdp[k].number() < cdp2 ) {
			path.lineTo( (float)points[k].getX(), (float)points[k].getY() );
			k++;
		}
		p = pointAtCDP( cdp2 );
		path.lineTo( (float)p.getX(), (float)p.getY() );
		g.draw(path);
	}
	public void setMap(XMap map) {
		this.map = map;
		if(map==null || cdp==null || cdp.length==0)return;
		Projection proj = map.getProjection();
		points = new Point2D[cdp.length];
		Point2D.Double lastPt = new Point2D.Double();
		double distance = 0.;
		for( int i=0 ; i<cdp.length ; i++) {
			Point2D.Double pt = (Point2D.Double)cdp[i].getXY();
			if( i!=0 ) {
				double dx = lastPt.x-pt.x;
				while( dx>180. ) dx -= 360;
				while( dx<-180. ) dx += 360;
				dx *= Math.cos( Math.toRadians( (pt.y+lastPt.y)/2. ) );
				distance += Math.sqrt( Math.pow( (pt.y-lastPt.y), 2 ) +
					Math.pow( dx, 2 ) );
			}
			while(pt.x<0)pt.x+=360;
			points[i] = proj.getMapXY(pt);
			if( i==0 ) {
				bounds.x = points[i].getX();
				bounds.y = points[i].getY();
			}
			if(points[i].getX() < bounds.x) bounds.x = points[i].getX();
			if(points[i].getX() > bounds.width) bounds.width = points[i].getX();
			if(points[i].getY() < bounds.y) bounds.y = points[i].getY();
			if(points[i].getY() > bounds.height) bounds.height = points[i].getY();
			lastPt = pt;
		}
		distance *= 111200.;
		cdpSpacing = distance / (double) (cdp[cdp.length-1].number() - cdp[0].number());
		bounds.width -= bounds.x;
		bounds.height -= bounds.y;
	}
	public RCruise getCruise() {
		return cruise;
	}
	public String getCruiseID() {
		return cruise.getID();
	}
	public String getID() {
		return new String(lineID);
	}
	public String toString() {
		return new String(lineID);
	}
	public double distanceSq(double x, double y) {
		double dist=10000;
		if(map==null || zRange==null ) return dist;
		for( int i=0 ; i<points.length-1 ; i++) {
		//	if(cdp[i+1].getConnect()) {
				dist = Math.min(dist, Line2D.ptSegDistSq(
					points[i].getX(), points[i].getY(),
					points[i+1].getX(), points[i+1].getY(),
					x, y) );
		//	}
		}
		return dist;
	}
	public void draw(Graphics2D g) {
		if(map==null) return;
		currentShape = null;
		if( !bounds.intersects(g.getClipBounds()) ) return;
		GeneralPath track = new GeneralPath();
		track.moveTo((float)points[0].getX(), (float)points[0].getY()) ;
		for( int i=1 ; i<points.length ; i++ ) {
			track.lineTo((float)points[i].getX(), (float)points[i].getY()) ;
		}
		g.draw(track);
	}
	public Rectangle2D getBounds() {
		if(map==null) return new Rectangle();
		return new Rectangle2D.Double( bounds.x, bounds.y, bounds.width, bounds.height );
	}
	public void addCrossing( double cdp, double cdpX, RLine lineX ) {
		crossings.add( new RCrossing( cdp, cdpX, lineX ) );
	}
	public static double[] cross(RLine lineA, RLine lineB) {
		if(lineA.points==null || lineB.points==null) return null;
		Line2D.Double A, B;
		for(int i=0 ; i<lineA.cdp.length-1 ; i++) {
			A = new Line2D.Double( lineA.points[i], lineA.points[i+1] );
			for(int j=0 ; j<lineB.cdp.length-1 ; j++) {
				B = new Line2D.Double( lineB.points[j], lineB.points[j+1] );
				double[] x = intersection( A, B);
				if(x==null) continue;
				double[] cdpX = new double[2];
				cdpX[0] = lineA.cdp[i].number() + 
					x[0]*(lineA.cdp[i+1].number()-lineA.cdp[i].number());
				cdpX[1] = lineB.cdp[j].number() + 
					x[1]*(lineB.cdp[j+1].number()-lineB.cdp[j].number());
				return cdpX;
			}
		}
		return null;
	}
	public static double[] intersection( Line2D a, Line2D b) {
		if( !a.intersectsLine(b) ) return null;
		double[] x = new double[2];
		double r1 = b.ptLineDist( a.getP1() );
		double r2 = b.ptLineDist( a.getP2() );
		x[0] = r1 / (r1+r2);
		r1 = a.ptLineDist( b.getP1() );
		r2 = a.ptLineDist( b.getP2() );
		x[1] = r1 / (r1+r2);
		return x;
	}
}

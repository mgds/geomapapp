package haxby.db;

import haxby.map.*;
import haxby.proj.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;

public class TrackOverlay implements Overlay {
	protected double[][] lonlat;
	protected float[][] xy;
	protected float[][] anotXY;
	protected boolean[] connect;
	protected Vector anotations;
	protected XMap map;
	protected Color color;
	protected float lineWidth;
	protected TrackOverlay( XMap map ) {
		this.map = map;
		color = Color.black;
		anotations = new Vector();
	}
	public TrackOverlay( double[][] lonlat, XMap map ) {
		this.map = map;
		this.lonlat = lonlat;
		color = Color.black;
		lineWidth = 1f;
		anotations = new Vector();
	}
	protected void project() {
		Projection proj = map.getProjection();
		xy = new float[lonlat.length][2];
		connect = new boolean[lonlat.length];
		for(int i=0 ; i<lonlat.length ; i++) {
			Point2D p = proj.getMapXY( new Point2D.Double(
					lonlat[i][0], lonlat[i][1]) );
			xy[i][0] = (float)p.getX();
			xy[i][1] = (float)p.getY();
			connect[i] = true;
		}
		connect[0] = false;
		anotXY = new float[anotations.size()][2];
		for( int i=0 ; i<anotations.size() ; i++) {
			Anotation a = (Anotation)anotations.get(i);
			Point2D p = proj.getMapXY( new Point2D.Double(
				a.x, a.y ));
			anotXY[i][0] = (float)p.getX();;
			anotXY[i][1] = (float)p.getY();;
		}
	}
	protected void project(float distanceTest) {
		Projection proj = map.getProjection();
		xy = new float[lonlat.length][2];
		connect = new boolean[lonlat.length];
		distanceTest *= distanceTest;
		for(int i=0 ; i<lonlat.length ; i++) {
			Point2D p = proj.getMapXY( new Point2D.Double(
					lonlat[i][0], lonlat[i][1]) );
			xy[i][0] = (float)p.getX();
			xy[i][1] = (float)p.getY();
			if( i==0 ) {
				connect[i] = false;
			} else {
				float dx = xy[i][0]-xy[i-1][0];
				float dy = xy[i][1]-xy[i-1][1];
				connect[i] = ( dx*dx + dy*dy 
						< distanceTest );
			}
		}
		anotXY = new float[anotations.size()][2];
		for( int i=0 ; i<anotations.size() ; i++) {
			Anotation a = (Anotation)anotations.get(i);
			Point2D p = proj.getMapXY( new Point2D.Double(
				a.x, a.y ));
			anotXY[i][0] = (float)p.getX();;
			anotXY[i][1] = (float)p.getY();;
		}
	}
	public void setLineWidth( float w ) {
		lineWidth = w;
	}
	public float getLineWidth() {
		return lineWidth;
	}
	public void setColor( Color c ) {
		color = c;
	}
	public Color getColor() {
		return color;
	}
	public void draw(Graphics2D g) {
		if(xy.length <=1) return;
		GeneralPath path = new GeneralPath();
		Rectangle2D bounds = map.getClipRect2D();
		path.moveTo( xy[0][0], xy[0][1] );
		boolean plot = false;
		for( int i=1 ; i<xy.length ; i++) {
			if( !bounds.contains( (double)xy[i][0], (double)xy[i][1])) {
				plot=false;
				continue;
			}
			if( connect[i] && plot  ) {
				path.lineTo( xy[i][0], xy[i][1] );
			} else {
				path.moveTo( xy[i][0], xy[i][1] );
				plot=true;
			}
		}
		g.setColor(color);
		g.setStroke(new BasicStroke( lineWidth/(float)map.getZoom() ));
		g.draw(path);
	}
}

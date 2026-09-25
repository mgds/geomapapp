package org.geomapapp.gis.shape;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.io.LittleIO;

import haxby.map.XMap;

public class ESRIPoint implements ESRIShape {
		
	public Point2D point;
	public ESRIPoint(double x, double y) {
		point = new Point2D.Double(x,y);
	}
	public int getType() {
		return 1;
	}
	public double getX() { return point.getX(); }
	public double getY() { return point.getY(); }
	public void setLocation( double x, double y) {
		point.setLocation(x, y);
	}
	public void setLocation( Point2D p ) {
		point = p;
	}
	public void toFloat() {
		point = new Point2D.Float( (float)point.getX(),
					(float)point.getY() );
	}
	public boolean canView( Rectangle2D r, double wrap) {
		double y=getY();
		if( y<r.getY() || y>r.getY()+r.getHeight())return false;
		double x = getX();
		if( wrap>0. ) {
			while( x<r.getX() )x+=wrap;
			while( x>r.getX()+r.getWidth() )x-=wrap;
			return x<r.getX();
		}
		return x<r.getX() || x>r.getX()+r.getWidth();
	}
	public void forward( MapProjection proj) {
		point = proj.getMapXY( point );
	}
	public void inverse( MapProjection proj) {
		point = proj.getRefXY( point );
	}
	public double[][] forward( MapProjection proj, double wrap, double[][] bounds) {
		forward(proj);
		double x = getX();
		double y = getY();
		if( bounds[0]==null ) bounds[0]= new double[] {x, x};
		if( bounds[1]==null ) bounds[1] = new double[] {y, y};
		double[] xr = bounds[0];
		double[] yr = bounds[1];
		double xc = (xr[0]+xr[1])/2.;
		double yc = (yr[0]+yr[1])/2.;
		if( wrap>0. ) {
			while( x>xc+wrap/2.) x-=wrap;
			while( x<xc-wrap/2.) x+=wrap;
		}
		if( x>xr[1] )xr[1]=x;
		else if( x<xr[0] )xr[0]=x;
		if( y>yr[1] )yr[1]=y;
		else if( y<yr[0] )yr[0]=y;
		return bounds;
	}
	public double[][] inverse(MapProjection proj, double[][] bounds) {
		inverse(proj);
		double x = getX();
		double y = getY();
		if( bounds[0]==null ) bounds[0]= new double[] {x, x};
		if( bounds[1]==null ) bounds[1] = new double[] {y, y};
		double[] xr = bounds[0];
		double[] yr = bounds[1];
		double xc = (xr[0]+xr[1])/2.;
		double yc = (yr[0]+yr[1])/2.;
		while( x>xc+180.) x-=360.;
		while( x<xc-180.) x+=360.;
		if( x>xr[1] )xr[1]=x;
		else if( x<xr[0] )xr[0]=x;
		if( y>yr[1] )yr[1]=y;
		else if( y<yr[0] )yr[0]=y;
		return bounds;
	}
		
	public void draw(Graphics2D g, Rectangle2D r, double wrap) {
		double y=getY();
		if( y<r.getY() || y>r.getY()+r.getHeight())return;
		double x = getX();
		if( wrap>0. ) {
			AffineTransform at = g.getTransform();
			while( x>r.getX()+wrap )x-=wrap;
			while( x<r.getX() )x+=wrap;
			if( x>r.getX()+r.getWidth() )return;
			g.translate( x-getX(), 0.);
			draw(g);
			while( x+wrap<r.getX()+r.getWidth() ) {
				g.translate(wrap,0.);
				draw(g);
				x+=wrap;
			}
			g.setTransform( at );
			return;
		}
		if( x<r.getX() || x>r.getX()+r.getWidth() )return;;
		draw(g);
	}	
	public void draw(Graphics2D g) {
		AffineTransform at = g.getTransform();
		g.translate( getX(), getY() );
		g.scale( 1./at.getScaleX(), 1./at.getScaleX() );
		g.setStroke( new BasicStroke(2f));
		g.draw( new Arc2D.Double(-4., -4., 8., 8., 
					0., 360., 
					Arc2D.CHORD));
		g.setTransform( at);
	}
	public NearNeighbor select( NearNeighbor n, XMap map ) {
		double test = n.distanceSq( point );
		if( test<n.radiusSq ) {
			n.shape = this;
			n.radiusSq = test;
			n.index = 0;
		}
		return n;
	}
	public int writeShape( OutputStream out ) throws IOException {
		LittleIO.writeDouble( getX(), out );
		LittleIO.writeDouble( getY(), out );
		return 16;
	}
	public String toString() {
		return "ESRIPoint: ("+getX() +", "+getY()+")";
	}
}

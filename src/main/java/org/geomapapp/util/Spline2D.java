package org.geomapapp.util;

import java.awt.geom.*;

import java.util.Vector;

public class Spline2D {
	Vector points;
	GeneralPath path;
	boolean closePath;
	double[][] ax;
	double[][] ay;
	public Spline2D() {
		this( false );
	}
	public Spline2D(boolean closePath) {
		this.closePath = closePath;
		points = new Vector();
	}
	public void setPoints(Vector points) {
		reset();
		if( points==null ) return;
		this.points = points;
	}
	public boolean isPathClosed() {
		return closePath;
	}
	public void setClosePath(boolean tf) {
		if( tf==closePath )return;
		closePath = tf;
		path = null;
	}
	public void addPoint( Point2D p ) {
		path = null;
		points.add(p);
	}
	public void setLastPoint( Point2D p ) {
		path = null;
		points.setElementAt(p, points.size()-1);
	}
	public void removePoint( int index ) {
		try {
			points.remove(index);
			path = null;
		} catch(Exception e) {
		//	System.out.println( e.getClass().getName() +"\t"+ e.getMessage() );
		}
	}
	public Vector getPoints() {
		return points;
	}
	public GeneralPath getPath() {
		if( path==null )computePath();
		return path;
	}
	public void reset() {
		path = null;
		points = new Vector();
		ax = null;
		ay = null;
	}
	void  computePath() {
		points.trimToSize();
		path = new GeneralPath();
		if( points.size()<2 )return;
		double[] x = new double[points.size()];
		double[] y = new double[points.size()];
		double[] t = new double[points.size()];
		for( int i=0 ; i<x.length ; i++) {
			Point2D p = (Point2D)points.get(i);
			x[i] = p.getX();
			y[i] = p.getY();
			if( i==0 ) t[i] = 0.;
			else t[i] = t[i-1]+Math.sqrt( (x[i]-x[i-1])*(x[i]-x[i-1])
						+ (y[i]-y[i-1])*(y[i]-y[i-1]) );
		}
		if( closePath ) {
			for( int i=0 ; i<x.length-1 ; i++) {
				t[i] = t[i+1]-t[i];
			}
			int i = x.length-1;
			t[i] = Math.sqrt( (x[i]-x[0])*(x[i]-x[0])
					+ (y[i]-y[0])*(y[i]-y[0]) );
		}
		int n = points.size();
		try {
			ax = closePath
				? Spline.wrapSpline(x, t, n)
				: Spline.spline(x, t, n);
			ay = closePath
				? Spline.wrapSpline(y, t, n)
				: Spline.spline(y, t, n);
		} catch(Exception e) {
			path.moveTo( (float)x[0], (float)y[0] );
			for( int k=1 ; k<x.length ; k++) path.lineTo( (float)x[k], (float)y[k] );
			return;
		}
		n--;
		path.moveTo( (float)x[0], (float)y[0]);
		for( int i=0 ; i<n ; i++) {
			double dt = closePath ? t[i] : t[i+1]-t[i];
			double dt1 = closePath ? t[i] : i==n ? t[i]-t[i-1] : t[i+1]-t[i];
			path.curveTo(
				(float)(x[i]+dt*ax[i][0]/3.), (float)(y[i]+dt*ay[i][0]/3.),
				(float)(x[i+1]-dt*ax[i+1][0]/3.), (float)(y[i+1]-dt*ay[i+1][0]/3.),
				(float)x[i+1], (float)y[i+1]);
		}
		if( closePath ) {
			path.curveTo(
				(float)(x[n]+t[n]*ax[n][0]/3.), (float)(y[n]+t[n]*ay[n][0]/3.),
				(float)(x[0]-t[n]*ax[0][0]/3.), (float)(y[0]-t[n]*ay[0][0]/3.),
				(float)x[0], (float)y[0]);
			path.closePath();
		}
	}
}
package org.geomapapp.geom;

import java.awt.geom.Point2D;

public class UTMProjection implements MapProjection {
	UTM utm;
	double x0, y0, scaleX, scaleY;
	public boolean equals(Object obj) {
		UTMProjection p= null;
		try {
			p = (UTMProjection) obj;
		} catch (ClassCastException ex) {
			return false;
		}
	//	if(!super.equals(p))return false;
		if( !utm.equals(p.getUTM()) ) return false;
		return (x0==p.x0) &&
			(y0==p.y0) &&
			(scaleX==p.scaleX) &&
			(scaleY==p.scaleY);
	}
	public UTMProjection( double x0, double y0, double scaleX, double scaleY, UTM utm ) {
		this.utm = utm;
		this.x0 = x0;
		this.y0 = y0;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
	}
	public UTMProjection( double x0, double y0, double scaleX, double scaleY,
				int zone, int ellipsoid, int hemisphere) {
	//	super(zone, ellipsoid, hemisphere);
		utm = new UTM(zone, ellipsoid, hemisphere);
		this.x0 = x0;
		this.y0 = y0;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
	}
	public double[] getOriginUTM() {
		return new double[] {x0, y0};
	}
	public double[] getScaleXY() {
		return new double[] {scaleX, scaleY};
	}
	public UTM getUTM() {
		return utm;
	}
	public Point2D getRefXY(Point2D mapXY) {
		return getRefXY( mapXY.getX(), mapXY.getY() );
	}
	public Point2D getRefXY(double mapX, double mapY) {
		return utm.getRefXY( x0+scaleX*mapX, y0-scaleY*mapY );
	}
	public Point2D getMapXY(Point2D refXY) {
		return getMapXY( refXY.getX(), refXY.getY() );
	}
	public Point2D getMapXY(double refX, double refY) {
		Point2D p = utm.getMapXY(refX, refY);
		p.setLocation( (p.getX()-x0)/scaleX, (y0-p.getY())/scaleY);
		return p;
	}
	public boolean isCylindrical() { return false; }
	public boolean isConic() { return false; }
/*
	public static void main(String[] args) {
		UTMProjection proj = new UTMProjection(562473.88127782, 4770325, 30,
				18, CLARKE_1866, NORTH);
		Point2D.Double p = new Point2D.Double(0, 0);
		p = (Point2D.Double) proj.getRefXY(p);
		System.out.println("UL: " + p.x +", "+ p.y);
		p.x = 2116;
		p.y = 0;
		p = (Point2D.Double) proj.getRefXY(p);
		System.out.println("UR: " + p.x +", "+ p.y);
		p.x = 0;
		p.y = 9472;
		p = (Point2D.Double) proj.getRefXY(p);
		System.out.println("LL: " + p.x +", "+ p.y);
		p.x = 2116;
		p.y = 9472;
		p = (Point2D.Double) proj.getRefXY(p);
		System.out.println("LR: " + p.x +", "+ p.y);
	}
*/
}
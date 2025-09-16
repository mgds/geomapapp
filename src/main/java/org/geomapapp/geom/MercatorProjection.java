package org.geomapapp.geom;

import java.awt.geom.Point2D;

public class MercatorProjection implements MapProjection {
	Mercator merc;
	double x0, y0, scaleX, scaleY;
	public boolean equals(Object obj) {
		MercatorProjection p= null;
		try {
			p = (MercatorProjection) obj;
		} catch (ClassCastException ex) {
			return false;
		}
		if( !merc.equals(p.getMercator()) ) return false;
		return (x0==p.x0) &&
			(y0==p.y0) &&
			(scaleX==p.scaleX) &&
			(scaleY==p.scaleY);
	}
	public MercatorProjection( double x0, double y0, double scaleX, double scaleY, Mercator merc ) {
		this.merc = merc;
		this.x0 = x0;
		this.y0 = y0;
		this.scaleX = scaleX;
		this.scaleY = scaleY;
	}
	public Mercator getMercator() {
		return merc;
	}
	public Point2D getRefXY(Point2D mapXY) {
		return getRefXY( mapXY.getX(), mapXY.getY() );
	}
	public Point2D getRefXY(double mapX, double mapY) {
		return merc.getRefXY( x0+scaleX*mapX, y0-scaleY*mapY );
	}
	public Point2D getMapXY(Point2D refXY) {
		return getMapXY( refXY.getX(), refXY.getY() );
	}
	public Point2D getMapXY(double refX, double refY) {
		Point2D p = merc.getMapXY(refX, refY);
		p.setLocation( (p.getX()-x0)/scaleX, (y0-p.getY())/scaleY);
		return p;
	}
	public boolean isCylindrical() { return true; }
	public boolean isConic() { return false; }
}
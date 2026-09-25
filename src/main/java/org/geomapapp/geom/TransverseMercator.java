package org.geomapapp.geom;

import java.awt.geom.Point2D;
import java.io.*;

public class TransverseMercator implements MapProjection {

	private double a, e2, e4, e6, epr2, e1, e1_2, e1_3, e1_4;
	private double m1, m2, m3, m4;

	int ellipsoid;
	double cLon;
	double x0, y0, scale;

	public TransverseMercator( double cLongtude, int ellipsoid) {
		this( cLongtude, ellipsoid, 0., 0., 1. );
	}
	public TransverseMercator( double cLongtude,
				int ellipsoid,
				double x0,
				double y0,
				double scale ) {
		cLon = cLongtude;
		this.ellipsoid = ellipsoid;
		this.x0 = x0;
		this.y0 = y0;
		this.scale = scale;
		init();
	}
	void init() {
		a = major[ellipsoid];
		e2 = 1 - Math.pow(minor[ellipsoid] / a, 2);
		e4 = e2*e2;
		e6 = e2*e4;
		epr2 = e2 / (1-e2);
		e1 = (1 - Math.sqrt( 1-e2 )) / (1 + Math.sqrt( 1-e2 ));
		e1_2 = e1*e1;
		e1_3 = e1*e1_2;
		e1_4 = e1*e1_3;
		m1 = 1 - e2/4 - 3*e4/64 - 5*e6/256;
		m2 = 3*e2/8 + 3*e4/32 + 45*e6/1024;
		m3 = 15*e4/256 + 45*e6/1024;
		m4 = 35*e6/3072;
	}
	public Point2D getMapXY( Point2D lonLat ) {
		return getMapXY( lonLat.getX(), lonLat.getY() );
	}
	public Point2D getMapXY( double lon, double lat) {
		lat = Math.toRadians(lat);
		lon = lon - cLon;
		while( lon > 180 ) lon -= 360;
		while( lon < -180 ) lon += 360;
		lon = Math.toRadians(lon);
		double coslat = Math.cos(lat);
		double sinlat = Math.sin(lat);
		double tanlat = sinlat / coslat;
		double n = a / Math.sqrt(1 - e2 * sinlat * sinlat);
		double t = tanlat * tanlat;
		double t2 = t * t;
		double c = epr2 * coslat * coslat;
		double b = lon * coslat;
		double b2 = b * b;
		double b3 = b * b2;
		double b4 = b * b3;
		double b5 = b * b4;
		double b6 = b * b5;
		double m = a * (m1*lat - m2*Math.sin(2*lat) +
				m3*Math.sin(4*lat) - m4*Math.sin(6*lat));
		double x = n * (b + (1 - t + c) * b3 / 6 +
				(5 - 18 * t + t2 + 72 * c - 58 * epr2) * b5 / 120);
		x = (x-x0)/scale;
		double y = (m + n * tanlat * (b2 / 2 +
				(5 - t + 9 * c + 4 * c * c) * b4 / 24 +
				(61 - 58 * t + t2 + 600 * c - 330 * epr2) *
				b6 / 720 ) );
		y = (y-y0)/scale;
		return new Point2D.Double(x, y);
	}
	public Point2D getRefXY( Point2D xy ) {
		return getRefXY( xy.getX(), xy.getY() );
	}
	public Point2D getRefXY( double x, double y ) {
		x = x0+x*scale;
		y = y0+y*scale;
		double m = y;
		double mu = m / (a*m1);
		double lat = mu + (3 * e1 / 2 - 27 * e1_3 / 32) *
					Math.sin(2 * mu) + (21 * e1_2 / 16 -
					55 * e1_4 / 32) * Math.sin(4 * mu) +
					(151 * e1_3 / 96) * Math.sin(6 * mu);
		double coslat = Math.cos(lat);
		double sinlat = Math.sin(lat);
		double tanlat = sinlat / coslat;
		double c1 = epr2 * coslat * coslat;
		double c1_2 = c1 * c1;
		double t1 = tanlat * tanlat;
		double t1_2 = t1 * t1;
		double n0 = Math.sqrt( 1 - e2 * sinlat * sinlat);
		double n1 = a / n0;
		double r1 = a * (1-e2) / (n0*n0*n0);
		double d = x / n1;
		double d2 = d * d;
		double d3 = d * d2;
		double d4 = d * d3;
		double d5 = d * d4;
		double d6 = d * d5;
		lat -= ( n1*tanlat/r1 ) * ( d2/2 - (5 + 3*t1 + 10*c1 - 4*c1_2 - 9*epr2 ) * d4/24 +
					(61 + 90*t1 + 298*c1 + 45*t1_2 - 252*epr2 - 3*c1_2) * d6/720);
		double lon = ( d - (1 + 2*t1 + c1) * d3/6
				+ (5 - 2*c1 + 28*t1 - 3*c1_2 + 8*epr2 + 24*t1_2) * d5/120 ) /coslat;
		return new Point2D.Double( cLon + Math.toDegrees(lon), Math.toDegrees(lat));
	}
	public boolean isCylindrical() {
		return false;
	}
	public boolean isConic() {
		return false;
	}
	public boolean equals(Object obj) {
		TransverseMercator p;
		try {
			p = (TransverseMercator)obj;
		} catch (ClassCastException ex) {
			return false;
		}
		return (cLon==p.cLon) &&
			(ellipsoid==p.ellipsoid) &&
			(x0==p.x0) &&
			(y0==p.y0) &&
			(scale==p.scale);
	}
}
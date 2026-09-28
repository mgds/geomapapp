package haxby.util;

import java.awt.geom.*;

public class XYZ {
	public double x;
	public double y;
	public double z;
	public XYZ() {
		x = y = z = 0d;
	}
	public XYZ(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	public XYZ plus(XYZ p) {
		return new XYZ(x+p.x, y+p.y, z+p.z);
	}
	public XYZ minus(XYZ p) {
		return new XYZ(x-p.x, y-p.y, z-p.z);
	}
	public XYZ times(double c) {
		return new XYZ(x*c, y*c, z*c);
	}
	public double dot(XYZ p) {
		return x*p.x + y*p.y + z*p.z;
	}
	public XYZ cross(XYZ p) {
		return new XYZ(y*p.z - z*p.y,
				z*p.x - x*p.z, 
				x*p.y - y*p.x);
	}
	public double getNorm() {
		return Math.sqrt(x*x + y*y + z*z);
	}
	public void normalize() {
		double norm = getNorm();
		x /= norm;
		y /= norm;
		z /= norm;
	}
	public Point2D getLonLat() {
		double norm = getNorm();
		double lat = Math.toDegrees(Math.asin(z/norm));
		double lon = Math.toDegrees(Math.atan2(y, x));
		return new Point2D.Double(lon,lat);
	}
	public static XYZ LonLat_to_XYZ(Point2D lonlat) {
		double z = Math.sin(Math.toRadians(lonlat.getY()));
		double c = Math.cos(Math.toRadians(lonlat.getY()));
		double x = c*Math.cos(Math.toRadians(lonlat.getX()));
		double y = c*Math.sin(Math.toRadians(lonlat.getX()));
		return new XYZ(x, y, z);
	}
}

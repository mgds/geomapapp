package org.geomapapp.geom;

public class IdentityProjection extends CylindricalProjection {
	public IdentityProjection() {}
	public double getLatitude(double y) {
		return y;
	}
	public double getY(double latitude) {
		return latitude;
	}
	public double getX(double longitude) {
		return longitude;
	}
	public double getLongitude(double x) {
		return x;
	}
	public boolean equals(Object obj) {
		IdentityProjection p;
		try {
			p = (IdentityProjection)obj;
		} catch (ClassCastException ex) {
			return false;
		}
		return true;
	}
}
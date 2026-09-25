package org.geomapapp.geom;

public class GCPoint {
	public double longitude;
	public double latitude;
	public double elevation;
	public GCPoint( double longitude,
			double latitude,
			double elevation) {
		this.longitude = longitude;
		this.latitude = latitude;
		this.elevation = elevation;
	}
	public XYZ getXYZ() {
		return XYZ.GCPoint_to_XYZ(this);
	}
}
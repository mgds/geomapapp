package org.geomapapp.geom;

public class RectangularProjection extends CylindricalProjection {
	double scaleY;
	double[] wesn;
	private int _width, _height;
	public RectangularProjection(double[] wesn, int width, int height) {
		refLon = wesn[0];
		refLat = wesn[3];
		scale = (double)(width-1) / (wesn[1] - wesn[0]);
		scaleY = (double)(height-1) / (wesn[3] - wesn[2]);
		range = 2;
		_width = width;
		_height = height;
		this.wesn = new double[] {wesn[0], wesn[1], wesn[2], wesn[3]};
	}
	public double[] getWESN() {
		return new double[] {wesn[0], wesn[1], wesn[2], wesn[3]};
	}
	public double getY(double lat) {
		double y = (refLat - lat) * scaleY;
		return y;
	}
	public double getLatitude(double y) {
		double lat = refLat - y / scaleY;
		return lat;
	}
	public int getWidth() { return _width; }
	public int getHeight() { return _height; }
	public boolean isCylindrical() { return true; }
	public boolean isConic() { return false; }
}
package haxby.proj;

public class SRTMProjection extends CylindricalProjection {
	double scaleY;
	public SRTMProjection(double[] wesn, int width, int height) {
		refLon = wesn[0];
		refLat = wesn[2];
		scale = (double)(width-1) / (wesn[1] - wesn[0]);
		scaleY = (double)(height-1) / (wesn[3] - wesn[2]);
	}
	public double getY(double lat) {
		double y = ( lat-refLat ) * scaleY;
		return y;
	}
	public double getLatitude(double y) {
		double lat = refLat + y / scaleY;
		return lat;
	}
	public boolean isCylindrical() { return true; }
	public boolean isConic() { return false; }
}
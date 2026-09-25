package haxby.proj;

import java.awt.geom.Point2D;

/** 
* An interface that defines methods used by projection objects
* Normally used for translating points from (to) geographics coordinates,
* (longitude, latitude) to (from) map coordinates (x, y).
* @author Bill Haxby
*/
public abstract interface Projection 
		extends org.geomapapp.geom.MapProjection {

	/**
	* Clarke, 1866 ellipsoid.
	*/
	public static final int CLARKE_1866 = 1;

	public static final int NORTH = 1;
	public static final int SOUTH = 2;
	public static final int RANGE_180W_to_180E = 0;
	public static final int RANGE_0_to_360 = 1;
	public static final double[][] AE_AP = {
			{ 6370997,	6370997 },
			{ 6378206.4, 	6356583.8 },		// clarke, 1866
			{ 6378137.,	6356752.314245 }		// wgs84
			};

	/**
	    Gets the longitude range used by the projection (0-360 or -180-180)
	    @return RANGE_0_to_360 or RANGE_180W_to_180E
	 */
	public int getLongitudeRange();
	public void setLongitudeRange(int range);

}
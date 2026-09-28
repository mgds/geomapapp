package haxby.proj;

import java.awt.geom.Point2D;

public abstract class CylindricalProjection implements Projection {
	protected double refLon, refLat;
	protected int ellipsoid;
	protected double scale;
	protected int range;
	static double[][] lonRange = 	{ {-180, 180}, {0, 360}};
	protected CylindricalProjection() {
		range = 0;
	}

	/**
		Gets the Map coordinates from a reference point.
		@param refXY the reference point to convert.
		@return map coordinates from the refernce point.
	*/
	public Point2D getMapXY(Point2D refXY) {
		return getMapXY( refXY.getX(), refXY.getY() );
	}

	/**
		Gets the Map coordinates from a reference point.
		@param refX the reference point's X to convert.
		@param refY the reference point's Y to convert.
		@return map coordinates from the refernce point.
	*/
	public Point2D getMapXY(double refX, double refY) {
		return new Point2D.Double(getX(refX), getY(refY));
	}

	/**
	 	Gets the reference coordinates from the map point.
	 	@param mapXY the map point to convert.
	 	@return the reference coordinates from the map point.
	*/
	public Point2D getRefXY(Point2D mapXY) {
		return getRefXY( mapXY.getX(), mapXY.getY() );
	}

	/**
	 	Gets the reference coordinates from the map point.
	 	@param mapY the map point's X to convert.
	 	@param mapY the map point's Y to convert.
	 	@return the reference coordinates from the map point.
	*/
	public Point2D getRefXY(double mapX, double mapY) {
		return new Point2D.Double(getLongitude(mapX), 
				getLatitude(mapY));
	}

	/**
	 	If the map is Cylindrical (True).
	 	@return if the map is Cylindrical.
	*/
	public boolean isCylindrical() {
		return true;
	}

	/**
	 	If the map is Conic (False).
	 	@return if the map is Conic.
	*/
	public boolean isConic() {
		return false;
	}

	/**
	 	Gets the latitude of the sent y.
	 	@param the y value to convert
	 	@return the latitude.
	*/
	public abstract double getLatitude(double y);

	/**
	 	Gets the y value of the sent latitude.
	 	@param the latitude value to convert
	 	@return the y value.
	*/
	public abstract double getY(double latitude);

	/**
	 	Returns the longitude of the sent x.
	 	@param the x value to send.
	 	@return the longitude.
	*/
	public double getLongitude(double x) {
		double longitude = x / scale;
		while(longitude > lonRange[range][1] ) longitude -= 360d;
		while(longitude < lonRange[range][0]) longitude += 360d;
		longitude += refLon;
		if (range == RANGE_180W_to_180E && longitude > 180d) longitude -= 360d;
		return longitude;
	}

	/**
	 	Returns the x value of the sent longitude.
	 	@param the longitude value to send.
	 	@return the x value.
	*/
	public double getX(double longitude) {
		while(longitude-refLon > lonRange[range][1] ) longitude -= 360d;
		while(longitude-refLon < lonRange[range][0]) longitude += 360d;
		if (range == RANGE_180W_to_180E && longitude < 0d) return scale * (longitude - refLon + 360d);
		return scale*(longitude-refLon);
	}

	/**
		Gets the longitude range used by the projection (0-360 or -180-180)
		@return RANGE_0_to_360 or RANGE_180W_to_180E
	 */
	public int getLongitudeRange() {
		return range;
	}
	
	public void setLongitudeRange(int range) {
		this.range = range;
	}
	
	/**
	 	Compares the sent Object with the current Object.
	 	@param obj the Object to compare.
	 	@return true if the objects are equal.
	*/
	public boolean equals(Object obj) {
		CylindricalProjection p=null;
		try {
			p = (CylindricalProjection) obj;
		} catch (ClassCastException ex) {
			return false;
		}
		return (refLon==p.refLon) &&
			(refLon==p.refLon) &&
			(ellipsoid==p.ellipsoid) &&
			(scale==p.scale) &&
			(range==p.range);
	}
}
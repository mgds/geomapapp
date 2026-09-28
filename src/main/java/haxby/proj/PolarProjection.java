package haxby.proj;

import java.awt.geom.Point2D;

public abstract class PolarProjection implements Projection {
	
	transient protected Point2D.Double poleXY;
	protected double poleX, poleY;
	protected double refLon;
	protected int hemisphere;
	protected int ellipsoid;
	protected int range;

	/**
		Gets the reference point from the map coordinates.
		@param mapXY the map coordinates.
		@return the reference point.
	*/	
	public Point2D getRefXY(Point2D mapXY) {
		return getRefXY( mapXY.getX(), mapXY.getY() );
	}

	/**
		Gets the reference point from the map coordinates.
		@param mapX the map coordinates x value.
		@param mapY the map coordinates y value.
		@return the reference point.
	*/
	public Point2D getRefXY(double mapX, double mapY) {
		double x = mapX - poleX;
		double y = mapY - poleY;
		double r = Math.sqrt(x*x + y*y);
		double latitude = getLatitude( r );
		double longitude = refLon;
		if(hemisphere==NORTH) longitude -= Math.toDegrees(Math.atan2(y, x));
		else longitude += Math.toDegrees(Math.atan2(y, x));
		if (range == RANGE_180W_to_180E && longitude > 180d) longitude -= 360d;
		if (range == RANGE_0_to_360 && longitude < 0) longitude += 360d; 
		return new Point2D.Double( longitude, latitude);
	}

	/**
		Gets the map coordinates from the reference point.
		@param refXY the reference point.
		@return the map coordinates.
	*/	
	public Point2D getMapXY(Point2D refXY) {
		return getMapXY( refXY.getX(), refXY.getY() );
	}

	/**
		Gets the map coordinates from the reference points.
		@param refX the reference point's x value.
		@param refY the reference point's y value.
		@return the map coordinate.
	*/
	public Point2D getMapXY(double refX, double refY) {
		double r = getRadius(refY);
		double a = (hemisphere == SOUTH) ?
			Math.toRadians(refX - refLon) :
			Math.toRadians(-refX + refLon);
		Point2D.Double p = new Point2D.Double( poleX + r * Math.cos(a),
						poleY + r*Math.sin(a));
		return p;
	}

	/**
		Gets if the map is Cylindrical.(False)
		@return if the map is Cylindrical.
	*/
	public boolean isCylindrical() {
		return false;
	}

	/**
		Gets if the map is Conic.(True)
		@return if the map is Conic.
	*/
	public boolean isConic() {
		return true;
	}

	/**
	 	Gets the Latitude using a sent radius.
	 	@param radius radius to use.
	 	@return the converted Latitude.
	*/
	public abstract double getLatitude(double radius);

	/**
	 	Gets the radius using a sent Latitude.
	 	@param latitude latitude to use.
	 	@return the converted Radius.
	*/
	public abstract double getRadius(double latitude);

	/**
	 	Gets corresponding number to which hemisphere the map is of.
	 	@return 1 is north, 2 is south.
	*/
	public int getHemisphere() { return hemisphere; }

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
	 	Compares the current Object with the sent Object
	 	@param obj the sent Object.
	 	@return if the objects are equal.
	 */
	public boolean equals(Object obj) {
		PolarProjection p;
		try {
			p = (PolarProjection)obj;
		} catch (ClassCastException ex) {
			return false;
		}
		return (poleX==p.poleX) &&
			(poleY==p.poleY) &&
			(refLon==p.refLon) &&
			(ellipsoid==p.ellipsoid) &&
			(hemisphere==p.hemisphere);
	}
}
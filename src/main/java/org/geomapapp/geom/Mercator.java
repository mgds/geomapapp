package org.geomapapp.geom;

public class Mercator extends CylindricalProjection {
	double e, es, halfE;
	double refY, scaleY;
	static double halfPI = Math.PI/2;

	/**
	 	Creates a Mercator with the sent reference x, reference y, meters per pixel, latitude of true scale, and the range. Sends a default ellipsoid of 0.
	 	@param refLon Longitude to draw 0,0 at.
	 	@param refLat Latitude to draw 0,0 at.
	 	@param mPerPixel meters per pixel.
	 	@param scaleLat The latitude of true scale.
	 	@param range integer used to determine which range is used.
	*/
	public Mercator(double refLon, double refLat,
			double mPerPixel, double scaleLat, int range) {
		this(refLon, refLat, mPerPixel, scaleLat, 0, range);
	}

	/**
	 	Creates a Mercator with the sent reference x, reference y, meters per pixel, latitude of true scale, ellipsoid, and the range.
	 	@param refLon Longitude to draw 0,0 at.
	 	@param refLat Latitude to draw 0,0 at.
	 	@param mPerPixel meters per pixel.
	 	@param scaleLat The latitude of true scale.
	 	@param ellipsoid integer used to determine which projection type.
	 	@param range integer used to determine which range is used.
	*/
	public Mercator(double refLon, double refLat,
			double mPerPixel, double scaleLat,
			int ellipsoid, int range) {
		this.ellipsoid = ellipsoid;
		this.range = range;
		init();
		this.refLon = refLon;
		this.refLat = refLat;
		scaleLat = Math.toRadians(scaleLat);
		double m1 = Math.cos(scaleLat);
		scale = 2. * Math.PI * major[ellipsoid] * m1 / mPerPixel / Math.sqrt( 1 - es * (1-m1*m1) );
		scale /= 360;
		scaleY = Math.toDegrees(scale);
		refY = 0;
		if( refLat!=0 )refY = getY(refLat)/scaleY;
	}

	/**
	 	Creates a Mercator with the sent reference x, reference y, pixels per 360 degrees, ellipsoid, and the range.
	 	@param refLon Longitude to draw 0,0 at.
	 	@param refLat Latitude to draw 0,0 at.
	 	@param pixelsPer360 How often the map repeats itself in pixels.
	 	@param ellipsoid integer used to determine which projection type.
	 	@param range integer used to determine which range is used.
	*/
	public Mercator(double refLon, double refLat,
			int pixelsPer360, 
			int ellipsoid, int range) {
		this( refLon, refLat, (double)pixelsPer360, ellipsoid, range);
	}
	public Mercator(double refLon, double refLat,
			double pixelsPer360, 
			int ellipsoid, int range) {
		this.ellipsoid = ellipsoid;
		this.range = range;
		init();
		this.refLon = refLon;
		this.refLat = refLat;
		scale = pixelsPer360 / 360d;
		scaleY = Math.toDegrees(scale);
		refY = 0;
		if( refLat!=0 ) refY = getY(refLat)/scaleY;
	}

	/**
	 	Gets the y value of the sent latitude.
	 	@param lat latitude to convert.
	 	@return the converted Y value.
	*/
	public double getY(double lat) {
		lat = Math.toRadians(lat);
		double y = Math.tan( .5 * (halfPI-lat) );
		if(ellipsoid != 0) {
			double c = e * Math.sin(lat);
			c = Math.pow( (1-c)/(1+c) , halfE );
			y /= c;
		}
		y = Math.log(y);
		return (y-refY) * scaleY;
	}

	/**
	 	Gets the latitude of the sent y value.
	 	@param y value to convert.
	 	@return the converted latitude.
	*/
	public double getLatitude(double y) {
		y = Math.exp( y/scaleY +refY );
		double lat = halfPI - 2*Math.atan(y);
		if(ellipsoid != 0) {
			double dlat, c;
			for( int i=0 ; i<10 ; i++) {
				c = e*Math.sin(lat);
				dlat = lat;
				lat = halfPI - 2*Math.atan( y * Math.pow( (1-c)/(1+c) , halfE) );
				if(Math.abs(dlat-lat) < 1.e-10) break;
			}
		}
		lat = Math.toDegrees(lat);
		return lat;
	}

	private void init() {
		if(ellipsoid<0 || ellipsoid>=MAX_SPHEROIDS) ellipsoid=0;
		if( ellipsoid==0 ) {
			e = 0;
			es = 0;
			halfE = 0;
			return;
		}
		es = 1 - Math.pow( minor[ellipsoid] / major[ellipsoid], 2);
		e = Math.sqrt(es);
		halfE = e/2;
	}

	/**
	 	Compares the current Object with the sent Object.
	 	@param obj the sent Object.
	 	@return if the objects are equal.
	*/
	public boolean equals(Object obj) {
		Mercator p;
		try {
			p = (Mercator)obj;
		} catch (ClassCastException ex) {
			return false;
		}
		if(!super.equals(p))return false;
		return (refY==p.refY) && (scaleY==p.scaleY);
	}
}
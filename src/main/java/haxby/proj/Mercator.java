package haxby.proj;

import java.awt.geom.Point2D;

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
		scale = 2.*Math.PI*AE_AP[ellipsoid][0] * m1 / Math.sqrt( 1 - es * (1-m1*m1) );
		scale /= mPerPixel;
		scale /= 360.;
		scaleY = Math.toDegrees(scale);
		refY = 0;
		refY = getY(refLat)/scaleY;
	}

	public Mercator( double[] wesn, double scaleLat, int ellipsoid, int width, int height) {
		int range = wesn[0]*wesn[1]<=0. ? 1 : 0;
		Mercator merc = new Mercator(0., 0., 1., scaleLat, ellipsoid, range);
		this.ellipsoid = ellipsoid;
		this.range = range;
		init();
		refLon = merc.getLongitude( wesn[0] );
		refLat = merc.getLatitude( -wesn[3] );
	System.out.println( "reference longitude:\t"+ refLon +"\t reference latitude:\t"+ refLat );
		scaleLat = Math.toRadians(scaleLat);
		double m1 = Math.cos(scaleLat);
		scale = 2.*Math.PI*AE_AP[ellipsoid][0] * m1 / Math.sqrt( 1 - es * (1-m1*m1) );
		double mPerPixel = (wesn[1]-wesn[0])/(width-1.);
		scale /= mPerPixel;
		scale /= 360.;
		scaleY = Math.toDegrees(scale);
		refY = 0;
		refY = getY(refLat)/scaleY;
		Point2D pt = getRefXY(new Point2D.Double(0.,0.));
		System.out.println( pt.getX() +"\t"+pt.getY());
		pt = getRefXY(new Point2D.Double(width-1., height-1.));
		System.out.println( pt.getX() +"\t"+pt.getY());
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
	 	@param latitude to convert.
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
		if(ellipsoid<0 || ellipsoid>2) ellipsoid=0;
		if( ellipsoid==0 ) {
			e = 0;
			es = 0;
			halfE = 0;
			return;
		}
		es = 1 - Math.pow( AE_AP[ellipsoid][1] / AE_AP[ellipsoid][0], 2);
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
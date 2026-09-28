package org.geomapapp.geom;

import java.awt.geom.Point2D;

public class PolarStereo extends PolarProjection {
	double scale;
	private double e, es, e4, halfE;
	static double halfPi = Math.PI/2;

	/**
	 	Creates a Polar Stereographic projection with the sent x and y values, in
			the reference Longitude, the scale, and the hemisphere.
	 	@param poleXY coordinates of the pole.
	 	@param refLon Reference Longitude used to determine the meridian.
	 	@param scale Scale of the map.
	 	@param hemisphere Integer used to identify which Hemisphere the map is of.
	*/
	public PolarStereo(Point2D poleXY, double refLon, double scale, int hemisphere) {
		this(poleXY, refLon, scale, hemisphere, 0);
	}

	public PolarStereo(Point2D poleXY, double refLon, double scale, 
				int hemisphere, int ellipsoid) {
		poleX = poleXY.getX();
		poleY = poleXY.getY();
		this.refLon = refLon + 90;
		if( hemisphere==SOUTH ) this.refLon -= 180;
		this.scale = scale;
		this.hemisphere = hemisphere;
		this.ellipsoid = ellipsoid;
		init();
	}

	public PolarStereo(Point2D poleXY, double refLon, 
				double mPerPixel, double scaleLat,
				int hemisphere, int ellipsoid) {
		poleX = poleXY.getX();
		poleY = poleXY.getY();
		this.refLon = refLon + 90;
		if( hemisphere==SOUTH ) this.refLon -= 180;
		this.hemisphere = hemisphere;
		this.ellipsoid = ellipsoid;
		init();
		scale = major[ellipsoid];
		if( hemisphere==SOUTH ) scaleLat = -scaleLat;
		double c;
		if( scaleLat > 89.99999 ) {
			scale = scale * 2 / e4;
		} else {
			scaleLat = Math.toRadians(scaleLat);
			c = e*Math.sin(scaleLat);
			scale *= Math.cos(scaleLat) / Math.sqrt( 1-c*c );
			c = Math.pow( (1-c)/(1+c) , halfE );
			scale *= c / Math.tan( .5 * (halfPi - scaleLat) );
		}
		scale /= mPerPixel;
	}
	private void init() {
		if( ellipsoid<0 || ellipsoid>=MAX_SPHEROIDS ) ellipsoid=0;
		if( ellipsoid==0 ) {
			e = 0;
			e4 = 1;
			return;
		}
		es = 1 - ( minor[ellipsoid] / major[ellipsoid] ) * 
					( minor[ellipsoid] / major[ellipsoid] );
		e = Math.sqrt(es);
		e4 = Math.sqrt( Math.pow(1+e,1+e) * Math.pow(1-e,1-e) );
		halfE = e/2;
	}

	/**
	 	Gets the Latitude at a certain radius.
	 	@param radius Radius to convert.
	 	@return radius converted to Latitude.
	*/
	public double getLatitude(double radius) {
		double r = radius/scale;
		double lat = halfPi - 2d*Math.atan(r);
		if(ellipsoid != 0) {
			double tol = 1.e-10;
			double dlat, c;
			for( int i=0 ; i<10 ; i++) {
				c = e*Math.sin(lat);
				dlat = lat;
				lat = halfPi - 2*Math.atan( r * Math.pow( (1-c)/(1+c) , halfE) );
				if(Math.abs(dlat-lat) < tol) break;
			}
		}
		lat = Math.toDegrees(lat);
		if(hemisphere == SOUTH) lat = -lat;
		return lat;
	}

	/**
	 	Gets the Radius at a certain latitude.
	 	@param latitude Latitude to convert.
	 	@return latitude converted to the radius.
	*/
	public double getRadius(double latitude) {
		if(hemisphere == SOUTH) latitude = -latitude;
		if( latitude > 89.99999 ) return 0;
		latitude = Math.toRadians(latitude);
		double radius = scale * Math.tan( (halfPi - latitude)*.5d );
		if(ellipsoid != 0) {
			double c = e * Math.sin(latitude);
			c = Math.pow( (1-c)/(1+c) , halfE );
			radius /= c;
		}
		return radius;
	}

	/**
	 	Compares the current object with the sent object.
	 	@param obj Object to compare.
	 	@return if the objects are equal.
	*/
	public boolean equals(Object obj) {
		PolarStereo p;
		try {
			p = (PolarStereo)obj;
		} catch (ClassCastException ex) {
			return false;
		}
		if(!super.equals(p))return false;
		return scale==p.scale;
	}
}
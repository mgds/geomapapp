package org.geomapapp.gis.shape;

import java.awt.geom.Point2D;

public class NearNeighbor {
	public Object shape;
	public final Point2D test;
	public double radiusSq;
	public double index;
	public NearNeighbor( Object shape, 
				Point2D test, 
				double radiusSq,
				double index) {
		this.shape = shape;
		this.test = test;
		this.radiusSq = radiusSq;
		this.index = index;
	}
	public double distanceSq( Point2D p ) {
		return test.distanceSq( p );
	}
}

package org.geomapapp.geom;

import java.awt.geom.Point2D;

/** 
* An interface that defines methods used by projection objects
* Normally used for translating points from (to) geographics coordinates,
* (longitude, latitude) to (from) map coordinates (x, y).
* @author Bill Haxby
*/
public abstract interface MapProjection extends GCTP_Constants {
	public static final int NORTH = 1;
	public static final int SOUTH = 2;
	public static final int GEOGRAPHIC = 0;
	public static final int MERCATOR = 1;
	public static final int POLAR_STEREO = 2;
	public static final int UTM = 3;
	public static final int UNKNOWN = -1;
	public static final String[] name = new String[] {
			"Geographic",
			"Mercator",
			"Polar Stereographic",
			"UTM"
		};
	/**
		transforms map coordinates to reference coordinates.
		@param mapX Map's X coordinate.
		@param mapY Map's Y coordinate.
		@return the reference coordinates of a mapped point
	*/
	public Point2D getRefXY(double mapX, double mapY);

	/**
	 	Gets the reference point of Map coordinates.
	 	@param mapXY the Map point to convert.
		@return the reference coordinates of a mapped point
	*/
	public Point2D getRefXY(Point2D mapXY);

	/**
		Gets the Map coordinates of a reference point.
		@param refX the Longitude to convert.
		@param refY the Latitude to convert.
		@return the map coordinates of a reference point
	*/
	public Point2D getMapXY(double refX, double refY);

	/**
		Gets the Map coordinates of a reference.
		@param refXY the refence point to convert.
		@return the map coordinates of a reference point
	*/
	public Point2D getMapXY(Point2D refXY);

	/**
		Checks if the the projection is cylindrical.
		@return true if the projection is cylindrical, i.e. if 
		<PRE>
			mapX = f(refX) 
		 and 	mapY = f(refY)
		</PRE>	
	*/
	public boolean isCylindrical();

	/**
		Gets if the projection is conic.
		@return true if the projection is conic.
	*/
	public boolean isConic();

	/**
		Compares if the sent Object is equal to the current Object.
		@return true if the two objects are equal.
	*/
	public boolean equals(Object obj);
}
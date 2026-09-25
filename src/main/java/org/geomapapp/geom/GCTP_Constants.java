package org.geomapapp.geom;

public interface GCTP_Constants {
	// Spheroid constants
	/** Sphere of radius 6,370,997 metres spheroid code (Default). */
	public static final int SPHERE = 0;

	/** Clarke 1866 spheroid code. */
	public static final int CLARKE1866 = 1;

	/** WGS 84 spheroid code. */
	public static final int WGS84 = 2;

	/** WGS 72 spheroid code. */
	public static final int WGS72 = 3;

	/** The total number of spheroid codes. */
	public static final int MAX_SPHEROIDS = 4;

	/** The list of spheroid code names. */
	public static final String[] SPHEROID_NAMES = {
	"Sphere of radius 6370997 m",
	"Clarke 1866",
	"WGS 84",
	"WGS 72",
};
/* Semi-Major axis of supported Spheroids */
public final static double[] major = {
		6370997.0,		/* 0: Sphere of Radius 6370997 meters*/
		6378206.4,		/* 1: Clarke 1866 (default) */
		6378137.0,		/* 2: WGS 84 */
		6378135.0,		/* 3: WGS 72 */
	};

/* Semi-Minor axis of supported Spheroids */
public final static double[] minor = {
		6370997.0,		/* 0: Sphere of Radius 6370997 meters*/
		6356583.8,		/* 1: Clarke 1866 (default) */
		6356752.314245,		/* 2: WGS 84 */
		6356750.519915,		/* 3: WGS 72 */
	};
}
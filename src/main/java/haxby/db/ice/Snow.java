package haxby.db.ice;

public class Snow {
	public final static double[][] wCoeff = {		// snow water equivalent
			{ 8.37, -0.0270, -0.3400, -0.0319, -0.0056, -0.0005},	// January
			{ 9.43,  0.0058, -0.1309,  0.0017, -0.0021, -0.0072},
			{10.74,  0.1618,  0.0276,  0.0213,  0.0076, -0.0125},
			{11.67,  0.0841, -0.1328,  0.0081, -0.0003, -0.0301},
			{11.80, -0.0043, -0.4284, -0.0380, -0.0071, -0.0063},
			{12.48,  0.2084, -0.5739, -0.0468, -0.0023, -0.0253},
			{ 4.01,  0.0970, -0.4930, -0.0333, -0.0026, -0.0343},
			{ 1.08,  0.0712, -0.1450, -0.0155,  0.0014,  0.0000},
			{ 3.84,  0.0393, -0.2107, -0.0182, -0.0053, -0.0190},
			{ 6.24,  0.1158, -0.2803, -0.0215,  0.0015, -0.0176},
			{ 7.54,  0.0567, -0.3201,  0.0284, -0.0032, -0.0129},
			{ 8.00, -0.0540, -0.3650, -0.0362, -0.0112, -0.0035}
		};
	public final static double[][] hCoeff = {		// snow thickness, cm
			{28.01,  0.1270, -1.1833, -0.1164, -0.0051,  0.0243},
			{30.28,  0.1056, -0.5908, -0.0263, -0.0049,  0.0044},
			{33.89,  0.5486, -0.1996,  0.0280,  0.0216, -0.0176},
			{36.80,  0.4046, -0.4005,  0.0256,  0.0024, -0.0641},
			{36.93,  0.0214, -1.1795, -0.1076, -0.0244, -0.0142},
			{36.59,  0.7021, -1.4819, -0.1195, -0.0009, -0.0603},
			{11.02,  0.3008, -1.2591, -0.0811, -0.0043, -0.0959},
			{ 4.64,  0.3100, -0.6350, -0.0655,  0.0059, -0.0005},
			{15.81,  0.2119, -1.0292, -0.0868, -0.0177, -0.0723},
			{22.66,  0.3594, -1.3483, -0.1063,  0.0051, -0.0577},
			{25.57,  0.1496, -1.4643, -0.1409, -0.0079, -0.0258},
			{26.67, -0.1876, -1.4229, -0.1413, -0.0316, -0.0029}
		};
	public static double thickness(java.awt.geom.Point2D lonlat, int month) {
		double lon = Math.toRadians( lonlat.getX() );
		double r = 90.-lonlat.getY();
		double x = r*Math.cos(lon);
		double y = r*Math.sin(lon);
		double t = hCoeff[month][0] + x*hCoeff[month][1] + y*hCoeff[month][2]
			+ x*y*hCoeff[month][3] + x*x*hCoeff[month][4] + y*y*hCoeff[month][5];
		return t*.01;
	}
	public static double waterEquivalent(java.awt.geom.Point2D lonlat, int month) {
		double lon = Math.toRadians( lonlat.getX() );
		double r = 90.-lonlat.getY();
		double x = r*Math.cos(lon);
		double y = r*Math.sin(lon);
		double t = wCoeff[month][0] + x*wCoeff[month][1] + y*wCoeff[month][2]
			+ x*y*wCoeff[month][3] + x*x*wCoeff[month][4] + y*y*wCoeff[month][5];
		return t*.01;
	}
	public static double conductivity(java.awt.geom.Point2D lonlat, int month) {
		double k = 2. * Math.pow( (waterEquivalent(lonlat, month)/
				(thickness(lonlat, month)*.9) ), 2);
		return k;
	}
}

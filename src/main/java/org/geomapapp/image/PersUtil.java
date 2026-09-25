package org.geomapapp.image;

import org.geomapapp.geom.*;

public class PersUtil {
	public static double[] zTest(Perspective3D pers,
				double[] zRange, 	// should be scaled by V.E
				int height) {		// and height
		double[] range = new double[] {
				1.+zRange[0]/GCTP_Constants.major[0],
				1.+zRange[1]/GCTP_Constants.major[0]
			};
		double r1 = range[0]*range[0];
		double r2 = range[1]*range[1];
		XYZ vp = pers.minusVP(new XYZ());
		double v2 = vp.dot(vp);
		XYZ pt1 = pers.minusVP(pers.inverse(new XYZ( -.5, 0., .001))).normalize();
		XYZ pt2 = pers.minusVP(pers.inverse(new XYZ( .5, 0., .001))).normalize();
		double a0 = Math.abs(Math.sin(Math.acos(pt1.dot(pt2))));

		double[] test = new double[513];
		for( int y=0 ; y<(int)Math.rint(height*Math.sqrt(2.)) ; y++) {
			XYZ d = pers.minusVP(pers.inverse(new XYZ(0.,y, 1.))).normalize();
			double b = vp.dot(d);
			double c2 = (v2-r2);
			if( b*b-c2<=0. ) break;
			double c1 = (v2-r1);
			double a = (b*b-c1<=0.) ?
				-b+Math.sqrt(b*b-c2) :
				-b+Math.sqrt(b*b-c1);
			XYZ pt = vp.plus( d.times(a) );
		}
		return test;
	}
}

package haxby.proj;

import haxby.util.*;
import java.awt.geom.*;

public class Perspective {
	XYZ vp;
	XYZ[] coord;
	double fov;
	public Perspective( XYZ v, XYZ fp, XYZ z, double fov) {	
		vp = v;
		coord = new XYZ[3];
		coord[0] = fp.minus(vp);
		coord[0].normalize();
		coord[1] = coord[0].cross(z);
		coord[1].normalize();
		coord[2] = coord[1].cross(coord[0]);
		this.fov = Math.tan( Math.toRadians(fov) );
	}
	public Perspective( double vpx, double vpy, double vpz,
			double fpx, double fpy, double fpz,
			double fov) {
		this( new XYZ( vpx, vpy, vpz), new XYZ( fpx, fpy, fpz),
			new XYZ(0.,0.,1), fov);
	}
	public Point2D getMapXY( XYZ p ) {
		XYZ z = p.minus(vp);
		double r = z.dot(coord[0])*fov;
		Point2D.Double map = new Point2D.Double(
				z.dot(coord[1]) / r,
				z.dot(coord[2]) / r);
		return map;
	}
}
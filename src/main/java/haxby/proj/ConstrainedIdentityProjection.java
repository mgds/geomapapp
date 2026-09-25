package haxby.proj;

import java.awt.geom.Point2D;
/**
 * Constrained Identity Projection ensures that the
 * requested refX is within [-180,180]
 *
 * @author Justin Coplan
 *
 */
public class ConstrainedIdentityProjection extends IdentityProjection {
	public Point2D getMapXY(double refX, double refY) {
		while (refX > 180) refX -= 360;
		while (refX < -180) refX += 360;
//		if (refX < 0) refX += 360;
//		if (refY > 90) refY = 90;
//		if (refY < -90) refY = -90;
		return super.getMapXY(refX, refY);
//		return super.getMapXY(0,0); 
	}

	public Point2D getMapXY(Point2D refXY) {
		return getMapXY(refXY.getX(), refXY.getY());
	}
}
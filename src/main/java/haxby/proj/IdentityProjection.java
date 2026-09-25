package haxby.proj;

import java.awt.geom.*;

public class IdentityProjection implements Projection {
	public IdentityProjection() {}
	public Point2D getMapXY(Point2D refXY) {
		return getMapXY( refXY.getX(), refXY.getY() );
	}
	public Point2D getMapXY(double refX, double refY) {
		return new Point2D.Double(refX, refY);
	}
	public Point2D getRefXY(Point2D mapXY) {
		return getRefXY( mapXY.getX(), mapXY.getY() );
	}
	public Point2D getRefXY(double mapX, double mapY) {
		return new Point2D.Double(mapX, mapY);
	}
	public boolean isCylindrical() {
		return true;
	}
	public boolean isConic() {
		return false;
	}
	public int getLongitudeRange() {
		return 0;
	}
	public boolean equals(Object obj) {
		IdentityProjection p;
		try {
			p = (IdentityProjection)obj;
		} catch (ClassCastException ex) {
			return false;
		}
		return true;
	}
	@Override
	public void setLongitudeRange(int range) {
		// TODO Auto-generated method stub
		
	}
	
}
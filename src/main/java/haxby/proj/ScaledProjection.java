package haxby.proj;
import java.awt.geom.Point2D;
public class ScaledProjection implements Projection {
	Projection parent;
	double scale;
	double xOffset;
	double yOffset;
	public ScaledProjection(Projection parent, double scale, double xOffset, double yOffset) {
		this.parent = parent;
		this.scale = scale;
		this.xOffset = xOffset;
		this.yOffset = yOffset;
	}
	public Point2D getRefXY(double mapX, double mapY) {
		mapX = xOffset + mapX*scale;
		mapY = yOffset + mapY*scale;
		return parent.getRefXY(mapX, mapY);
	}
	public Point2D getRefXY(Point2D mapXY) {
		return getRefXY(mapXY.getX(), mapXY.getY());
	}
	public Point2D getMapXY(double refX, double refY) {
		Point2D p0 = parent.getMapXY(refX, refY);
		Point2D.Double p = new Point2D.Double(p0.getX(), p0.getY());
		p.x = (p.x - xOffset) / scale;
		p.y = (p.y - yOffset) / scale;
		return p;
	}
	public Point2D getMapXY(Point2D refXY) {
		return getMapXY( refXY.getX(), refXY.getY());
	}
	public boolean isCylindrical() {
		return parent.isCylindrical();
	}
	public boolean isConic() {
		return parent.isConic();
	}
	public int getLongitudeRange() {
		return parent.getLongitudeRange();
	}
	public boolean equals(Object obj) {
		try {
			ScaledProjection proj = (ScaledProjection)obj;
			return  ( proj.getScale() == scale
				&& proj.getXOffset() == xOffset
				&& proj.getYOffset() == yOffset
				&& proj.equals(parent) );
		} catch (Throwable ex) {
			return false;
		}
	}
	public double getScale() { return scale; }
	public double getXOffset() { return xOffset; }
	public double getYOffset() { return yOffset; }
	public Projection getParent() { return parent; }
	@Override
	public void setLongitudeRange(int range) {
		// TODO Auto-generated method stub
		
	}
}
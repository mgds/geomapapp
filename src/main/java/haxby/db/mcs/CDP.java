package haxby.db.mcs;

import java.awt.geom.Point2D;
public class CDP {
	int cdp;
	long time;
	Point2D xy;
	boolean connect;
	public CDP( int cdp, Point2D xy, long time, boolean connect) {
		this.cdp = cdp;
		this.xy = xy;
		this.time = time;
		this.connect = connect;
	}
	public CDP( int cdp, double lon, double lat, long time, boolean connect) {
		this(cdp, new Point2D.Double(lon, lat), time, connect);
	}
	public int number() {
		return cdp;
	}
	public Point2D getXY() {
		return new Point2D.Double(xy.getX(), xy.getY());
	}
	public double getX() {
		return xy.getX();
	}
	public double getY() {
		return xy.getY();
	}
	public long getTime() {
		return time;
	}
	public boolean getConnect() {
		return connect;
	}
}

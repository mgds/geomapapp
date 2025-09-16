package haxby.nav;

public abstract class ControlPt extends java.awt.geom.Point2D {
	public static class Short extends ControlPt {
		public short x;
		public short y;
		public Short() {
			x = 0;
			y = 0;
		}
		public Short( short x, short y) {
			this.x = x;
			this.y = y;
		}
		public double getX() {
			return (double)x;
		}
		public double getY() {
			return (double)y;
		}
		public void setLocation(double x, double y) {
			this.x = (short)x;
			this.y = (short)y;
		}
	}
	public static class Float extends ControlPt {
		float x;
		float y;
		public Float( float x, float y ) {
			this.x = x;
			this.y = y;
		}
		public double getX() {
			return (double)x;
		}
		public double getY() {
			return (double)y;
		}
		public void setLocation(double x, double y) {
			this.x = (float)x;
			this.y = (float)y;
		}
	}
	public static class Double extends ControlPt {
		double x;
		double y;
		public Double( double x, double y ) {
			this.x = x;
			this.y = y;
		}
		public double getX() {
			return x;
		}
		public double getY() {
			return y;
		}
		public void setLocation(double x, double y) {
			this.x = x;
			this.y = y;
		}
	}
}

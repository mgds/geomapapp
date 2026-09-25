package haxby.nav;

public class TimeControlPt extends ControlPt {
	public int time;
	public ControlPt cpt;
	public TimeControlPt(ControlPt cpt, int seconds) {
		this.cpt = cpt;
		time = seconds;
	}
	public void setTimeInSec( int sec ) {
		time = sec;
	}
	public void setTimeInMillis(long millis) {
		time = (int) (millis/1000);
	}
	public long getTimeInMillis() {
		return 1000L * (long) time;
	}
	public ControlPt getControlPt() {
		return cpt;
	}
	public double getX() {
		return cpt.getX();
	}
	public double getY() {
		return cpt.getY();
	}
	public void setLocation(double x, double y) {
		cpt.setLocation(x, y);
	}
	public String toString() {
		return "TimeControlPt["+time+", "+cpt.toString()+"]";
	}
}

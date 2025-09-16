package haxby.nav;

public class IndexControlPoint extends ControlPoint {
	public int index;
	public IndexControlPoint(double x, double y, int time, int index) {
		super(x, y, time);
		this.index = index;
	}
}

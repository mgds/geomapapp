package haxby.nav;

public class Nearest {
	public TrackLine track;
	public double x;
	public double rtest;
	public int seg;
	public Nearest( TrackLine track, int seg, double x, double rtest ) {
		this.track = track;
		this.seg = seg;
		this.x = x;
		this.rtest = rtest;
	}
}

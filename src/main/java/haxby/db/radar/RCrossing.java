package haxby.db.radar;

public class RCrossing {
	public double cdp1, cdp2;
	public RLine cross;
	public RCrossing( double cdp1, double cdp2, RLine cross) {
		this.cross = cross;
		this.cdp1 = cdp1;
		this.cdp2 = cdp2;
	}
}

package haxby.db.mcs;

public class MCSCrossing {
	public double cdp1, cdp2;
	public MCSLine cross;
	public MCSCrossing( double cdp1, double cdp2, MCSLine cross) {
		this.cross = cross;
		this.cdp1 = cdp1;
		this.cdp2 = cdp2;
	}
}

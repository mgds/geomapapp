package haxby.db.xmcs;

public class XMCrossing {
	public double cdp1, cdp2;
	public XMLine cross;
	public XMCrossing( double cdp1, double cdp2, XMLine cross) {
		this.cross = cross;
		this.cdp1 = cdp1;
		this.cdp2 = cdp2;
	}
}
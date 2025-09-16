package haxby.db.mb;

import haxby.map.*;
import haxby.nav.*;

import java.awt.*;
import java.awt.geom.*;

public class MBTrack {
	XMap map;
	TrackLine nav;
//	MBData data;
	public MBTrack( TrackLine nav) {
		this.nav = nav;
//		data = null;
	}
	public String getName() {
		return nav.getName();
	}
	public long getTime( Nearest n ) {
		return nav.getTime(n);
	}
	public int getStart() {
		return nav.getStart();
	}
	public int getEnd() {
		return nav.getEnd();
	}

//	GMA 1.4.8: Changed "mask" in TrackLine.java from byte to int to make "types" 
//	in MGG work correctly
//	public byte getTypes() {
	public int getTypes() {

		return nav.getTypes();
	}

	public Rectangle2D getBounds() {
		return nav.getBounds();
	}
//	public boolean hasData() { 
//		return (data != null) );
//	}
	public void draw(Graphics2D g2) {
//		if( data==null ) {
			nav.draw(g2);
//		} else {
//			data.draw(g2);
//		}
	}
	public boolean contains( double x, double y ) {
		return nav.contains(x, y);
	}
	public boolean firstNearPoint( double x, double y, Nearest n) {
		return nav.firstNearPoint( x, y, n);
	}

	public TrackLine getNav() {
		return nav;
	}
}

package haxby.db.mgg;

import haxby.map.*;
import haxby.nav.*;

import java.awt.*;
import java.awt.geom.*;

public class MGGTrack {
	XMap map;
	TrackLine nav;
//	MGGData data;
	public MGGTrack( TrackLine nav) {
		this.nav = nav;
//		data = null;
	}
	public String getName() {
		return nav.getName();
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

//	public boolean hasData() { 
//		return (data != null) );
//	}
	public TrackLine getNav() {
		return nav;
	}

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
	public boolean intersects( Rectangle2D rect ) {
		return nav.intersects(rect);
	}
	public boolean firstNearPoint( double x, double y, Nearest n) {
		return nav.firstNearPoint( x, y, n);
	}
}
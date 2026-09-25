package haxby.db.age;

import haxby.nav.*;

import java.util.Vector;

public class Isochron {
	Vector segs, seg;
	TrackLine nav;
	short plate, conjugate;
	short anom;
	public Isochron( short plate1, short plate2, int anomaly ) {
		plate = plate1;
		conjugate = plate2;
		anom = (short)anomaly;
		segs = new Vector();
		seg = null;
		nav = null;
	}
	public void add( float lon, float lat, boolean connect ) {
		if( !connect ) {
			if( seg!=null ) {
				if(seg.size()<=1) segs.remove(seg);
				else seg.trimToSize();
			}
			seg = new Vector();
			segs.add( seg );
		}
		seg.add( new float[] {lon, lat} );
	}
	public Vector getSegs() { return segs; }
	public void finish() {
		if( seg!=null ) {
			if(seg.size()<=1) segs.remove(seg);
			else seg.trimToSize();
			seg=null;
		}
		segs.trimToSize();
	}
	public void setNav( TrackLine nav ) {
		this.nav = nav;
		segs = null;
	}
}

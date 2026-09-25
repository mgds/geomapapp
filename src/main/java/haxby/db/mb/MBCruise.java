package haxby.db.mb;

import java.util.Vector;

public class MBCruise {
	String name;
	int fmt;
	Vector tracks;
	public MBCruise( String name, int fmt ) {
		this.name = name;
		this.fmt = fmt;
		tracks = new Vector();
	}
	public String toString() {
		return name;
	}
	public String getName() {
		return name;
	}
	public int getMBFormat() {
		return fmt;
	}
	public void addTrack( MBTrack track ) {
		tracks.add( track );
	}
	public MBTrack[] getTracks() {
		MBTrack[] t = new MBTrack[tracks.size()];
		for( int i=0 ; i<t.length ; i++) t[i] = (MBTrack)tracks.get(i);
		return t;
	}
}

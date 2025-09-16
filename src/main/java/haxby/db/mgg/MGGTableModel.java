package haxby.db.mgg;

import haxby.util.*;

import java.util.*;

public class MGGTableModel extends XBTableModel {
	MGGTracks mgg;
	Vector tracks;
	Vector indices;
	GregorianCalendar cal = new GregorianCalendar(
			TimeZone.getTimeZone("GMT"));
	public MGGTableModel( MGGTracks mgg ) {
		this.mgg = mgg;
		clearTracks();
	}
	void addTrack( MGGTrack track, int index ) {
		tracks.add(track);
		indices.add(new Integer(index));
	}
	void clearTracks() {
		tracks = new Vector(mgg.tracks.length);
		indices = new Vector(mgg.tracks.length);
	}
	int indexOf( int i ) {
		if(i<0 || i>=indices.size() ) return -1;
		int index = ((Integer)indices.get(i)).intValue();
		return index;
	}
	int indexOf( MGGTrack track ) {
		int index = tracks.indexOf( track );
		return index;
	}
	public int getRowCount() {
		if( tracks.size()==0 ) return 1;
		return tracks.size();
	}
	public Object getValueAt(int row, int col) {
		if( tracks.size()==0 ) {
			if( col<3 ) return new Boolean(false);
			return "no tracks";
		}
		MGGTrack track = (MGGTrack)tracks.get(row);
		if( col==3 ) {
			cal.setTime( new Date( (long)track.getStart()*1000L ) );
			String s = (1+cal.get(cal.MONTH)) +"/"+ cal.get(cal.YEAR);
			return s;
		}
		return new Boolean(
			(track.getTypes() & ((byte)1)<<col) != 0 );
	}
	public String getColumnName( int col ) {
		return cols[col];
	}
	public int getColumnCount() {
		return cols.length;
	}
	public Class getColumnClass( int col ) {
		if( col<3 ) return Boolean.class;
		return String.class;
	}
	public boolean isCellEditable( int row, int col) {
		return false;
	}
	public String getRowName( int row ) {
		if( row<0 || row>=tracks.size() ) return "none";
		return ((MGGTrack)tracks.get(row)).getName();
	}
	static String[] cols = {
//			***** 1.6.2: Change the titles of the radio buttons to more clearly convey the data type 
//			being selected.
			"Depth",
			"Gravity",
			"Magnetic",
//			***** 1.6.2
			"start"};
}

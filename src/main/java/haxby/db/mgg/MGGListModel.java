package haxby.db.mgg;

import javax.swing.AbstractListModel;
import java.util.*;

public class MGGListModel extends AbstractListModel {
	MGG mgg;
	Vector tracks;
	Vector indices;
	GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
	public MGGListModel( MGG mgg ) {
		this.mgg = mgg;
		tracks = new Vector();
		indices = new Vector();
	}
	public void addTrack( MGGTrack track, int index ) {
		tracks.add(track);
		indices.add(new Integer(index));
	}
	public void clearTracks() {
		if( tracks.size()==0 ) return;
		fireIntervalRemoved( this, 1, tracks.size()-1 );
		tracks.clear();
		indices.clear();
	}
	public int indexOf( int i ) {
		if(i<0 || i>=indices.size() ) return -1;
		int index = ((Integer)indices.get(i)).intValue();
		return index;
	}
	public int indexOf( MGGTrack track ) {
		int index = tracks.indexOf( track );
		return index;
	}
	public int getSize() {
		if( tracks.size()==0 ) return 1;
		return tracks.size();
	}
	public Object getElementAt( int row ) {
		if( row<0 || row>=tracks.size() ) return "none";
		return ((MGGTrack)tracks.get(row)).getName();
	}
	public void updateList() {
		fireIntervalAdded( this, 0, tracks.size() );
	}
}
package haxby.db.ship;

import haxby.db.mgg.MGGTrack;

import javax.swing.AbstractListModel;
import java.util.*;

public class ShipListModel extends AbstractListModel{

	Ship ship;
	Vector<ShipTrack> tracks;
	Vector<Integer> indices;
	GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
	
	public ShipListModel(Ship ship) {
		this.ship = ship;
		tracks = new Vector<ShipTrack>();
		indices = new Vector<Integer>();
	}
	
	public void addTrack(ShipTrack track, int index){
		tracks.add(track);
		indices.add(new Integer(index));
	}
	
	public void clearTracks(){
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
	
	public int indexOf(ShipTrack track){
		int index = tracks.indexOf( track );
		return index;
	}
	
	public int getSize() {
		if( tracks.size()==0 ) return 1;
		return tracks.size();
	}
	
	public Object getElementAt( int row ) {
		if( row<0 || row>=tracks.size() ) return "none";
		return ((ShipTrack)tracks.get(row)).getName();
	}
	
	public void updateList() {
		fireIntervalAdded( this, 0, tracks.size() );
	}

}

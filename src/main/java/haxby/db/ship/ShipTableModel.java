package haxby.db.ship;
import haxby.db.ship.*;

import haxby.util.*;

import java.util.*;
import java.util.regex.Pattern;

import javax.swing.table.TableModel;


public class ShipTableModel extends XBTableModel{
	ShipTracks ship;
	Vector<ShipTrack> tracks;
	Vector<Integer> indices;

	private Comparator<Integer> columnSorter=null;

	private int lastSortedCol = -1;
	private boolean ascent = true;

	public ShipTableModel( ShipTracks ship ) {
		
		this.ship = ship;
		clearTracks();
	}
	void addTrack( ShipTrack track, int index ) {
		tracks.add(track);
		indices.add(new Integer(index));
		
	}
	void clearTracks() {
		tracks = new Vector<ShipTrack>(ship.tracks.length);
		indices = new Vector<Integer>(ship.tracks.length);
		
	}
	int indexOf( int i ) {
		if(i<0 || i>=indices.size() ) return -1;
		int index = ((Integer)indices.get(i)).intValue();
		return index;
	}
	int indexOf( ShipTrack track ) {
		int index = tracks.indexOf(track );
		return index;
	}
	public int getRowCount() {
		if( tracks.size()==0 ) return 1;
		return tracks.size();
	}
	public Object getValueAt(int row, int col) {
		if( tracks.size()==0 ) {
			
			return "no tracks";
		}
		ShipTrack track = (ShipTrack)tracks.get(row);
		
		switch(col){
		case 0:
			return track.cruiseID.trim();
			
		case 1:
			return track.ship.trim();
		case 2:
			return track.chief_scientist.trim();
		case 3:
			return track.initiative.trim();
		case 4:
			return track.start.trim();
		case 5:
			return track.end.trim();
		case 6:	
			return track.url.trim();
		case 7:
			return track.multibeam.trim();
		case 8:
			return track.singlebeam.trim();				
		case 9:
			return track.sidescan.trim();			
		case 10:
			return track.photograph.trim();
		case 11:
			return track.mag_grav.trim();				
		case 12:
			return track.ctd.trim();
		case 13:
			return track.adcp.trim();
		case 14:
			return track.samples.trim();
		case 15:
			return track.auv_rov_hov.trim();
		case 16:
			return track.chemistry.trim();
		case 17:
			return track.biology.trim();
		case 18:
			return track.temperature.trim();
		case 19:
			return track.seismic_reflection.trim();
		case 20:
			return track.seismic_refraction.trim();
		case 21:
			return track.seismicity.trim();
		case 22:
			return track.visualization.trim();

	}

		return null;
	}
	public String getColumnName( int col ) {
		return cols[col];
	}
	public int getColumnCount() {
		return cols.length;
	}
	public Class<String> getColumnClass( int col ) {
		
		return String.class;
	}
	public boolean isCellEditable( int row, int col) {
		return false;
	}

	public String getRowName( int row ) {
		if( row<0 || row>=tracks.size() ) return "none";
			return " ";
	}

	public void sortByColumn(int col) {
		if (lastSortedCol == col)
			ascent = !ascent;
		else
			ascent = true;
		lastSortedCol = col;

		Vector<ShipTrack> tempTracks = tracks;
		Vector<Integer> tempIndex = indices;
		HashMap<Integer, ShipTrack>  tempIndexMap = new HashMap<Integer, ShipTrack>();
		for(Integer i: tempIndex){
			//System.out.println("before "+i);
			tempIndexMap.put(i, tempTracks.get(i));
		}

		Collections.sort(tempIndex, getColumnSorter());
		clearTracks();


		for(Integer i:tempIndex){
			//System.out.println("after "+ i);
			addTrack(tempIndexMap.get(i),i);
			tracks.copyInto(ship.tracks);
		}

		updateRowToDisplayIndex();
		fireTableStructureChanged();
	}

	private Comparator<? super Integer> getColumnSorter() {
		if (columnSorter == null) {
			//System.out.println("not null");

			columnSorter = new Comparator<Integer>() {
				public int compare(Integer arg0, Integer arg1) {

					int cmp;
					//System.out.println("yes"+ indices.get(lastSortedCol));
					//System.out.println("tracks.get");

					String obj0 = tracks.get(arg0).get_field_by_col(lastSortedCol);
					String obj1 = tracks.get(arg1).get_field_by_col(lastSortedCol);
					//System.out.println("obj0 "+obj0);
					//System.out.println("obj1 "+obj1);

					cmp = obj0.compareTo(obj1);
					return ascent ? cmp : -cmp;
				}
			};
		}
		return columnSorter;
	}

	private void updateRowToDisplayIndex() {


	}
	static String[] cols = {

//			***** 1.6.2: Change the titles of the radio buttons to more clearly convey the data type 
//			being selected.
			"CruiseID",//0
			"Ship",//1
			"Chief Scientist",//2
			"Initiative",//3
			"Start Date",//4
			"End Date",//5
			"URL",//6
			"Multibeam/Phase",//7
			"SingleBeam",//8
			"SideScan",//9
			"Photograph",//10
			"Mag/Grav",//11
			"CTD",//12
			"ADCP",//13
			"Samples",//14
			"AUV/ROV/HOV",//15
			"Chemistry",//16
			"Biology",//17
			"Temperature",//18
			"Seismic Reflection",//19
			"Seismic Refraction",//20
			"Seismicity",//21
			"Visualization"//22
			};
}
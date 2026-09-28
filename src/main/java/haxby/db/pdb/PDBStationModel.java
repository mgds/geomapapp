package haxby.db.pdb;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import haxby.map.XMap;
import haxby.util.SortableTableModel;
public class PDBStationModel extends SortableTableModel {
	public XMap map;
	public PDB pdb;
	public int[] stations;
	public int[] inArea;
	public int[] current;
	public int[] selected;
	public int[] toPlot;
	public String[] colName;
	public Class[] colClass;
	public double wrap;
	public int materialFlags;
	public int dataFlags;
	public long rockFlags;
	public int altFlags;

	HashMap stationToIndex;

	// Keeps track of sorted Col and direction
	private int lastSortedCol = -1;
	private boolean ascent = true;

	// Column sorter is used in sorting index's of row data
	// Column sorter compares two PDBStation index's based on lastSortedColumn
	private Comparator columnSorter = new Comparator() {
		public int compare(Object arg0, Object arg1) {
			int cmp;

			PDBStation s0 = PDBStation.get(((Integer)arg0).intValue());
			PDBStation s1 = PDBStation.get(((Integer)arg1).intValue());

			String obj0 = getValueAt(s0, lastSortedCol).toString();
			String obj1 = getValueAt(s1, lastSortedCol).toString();

			if (colClass[lastSortedCol] == String.class) {
				cmp = obj0.compareToIgnoreCase(obj1);
			} else {
				// Try to make them numbers
				double d0,d1;
				try {
					d0 = Double.parseDouble(obj0);
					d1 = Double.parseDouble(obj1);
					cmp = d0 - d1 > 0 ? 1 : -1;
				} catch (NumberFormatException ex) {
					cmp = obj0.compareToIgnoreCase(obj1);
				}
			}
			return ascent ? cmp : -cmp;
		}
	};

	
	private Comparator<Integer> rowSorter = new Comparator<Integer>() {
		public int compare(Integer arg0, Integer arg1) {

			PDBStation s0 = PDBStation.get((arg0).intValue());
			PDBStation s1 = PDBStation.get((arg1).intValue());

			int cmp = s0.getID().compareToIgnoreCase(s1.getID());

			return ascent ? cmp : -cmp;
		}
	};
	
	public PDBStationModel(XMap map, PDB pdb, double wrap) {
		super();
		this.map = map;
		this.pdb = pdb;
		this.wrap = wrap;
		stations = new int[PDBStation.size()];
		int k=0;
		for( int i=0 ; i<stations.length ; i++ ) {
			if( PDBStation.get(i)!=null) stations[k++]=i;
		}
		if( k<stations.length ) {
			int[] tmp = new int[k];
			System.arraycopy( stations, 0, tmp, 0, k);
			stations = tmp;
		}
		current = new int[stations.length];
		System.arraycopy( stations, 0, current, 0, k);
		stationToIndex = new HashMap();
		updateStationIndexMap();

		toPlot = new int[0];
		findCloseStations(1./16.);
	//	findCloseStations(1./256.);
		selected = new int[0];
		materialFlags = 0xffff;
		dataFlags = 0xffff;
		rockFlags = 0xfffffffffL;
		altFlags = 0xffff;
		colName = new String[] { 
					"Number of Samples",
					"Material",
					"Data Available",
					"Rock Type",
					"Longitude",
					"Latitude" };
		colClass = new Class[] { 
					Integer.class,
					String.class,
					String.class,
					String.class,
					Double.class,
					Double.class};
	}
	int[] plot = null;
	public int[] getPlot() {
		return plot;
	}
	void findCloseStations(double test) {
		int n = PDBStation.size();
		plot = new int[n];
		for(int i=0 ; i<n  ; i++) {
			if(PDBStation.get(i)!=null) plot[i] = i;
			else plot[i]=-1;
		}
		double x0, y0, x, y;
		double wtest = wrap/2.;
		for(int i=0 ; i<n  ; i++) {
			if( plot[i]!=i ) continue;
			x0 = PDBStation.get(i).getX();
			y0 = PDBStation.get(i).getY();
			for(int j=i+1 ; j<n ; j++) {
				if( plot[j]!=j )continue;
				y = PDBStation.get(j).getY()-y0;
				x = PDBStation.get(j).getX()-x0;
				if(wrap>0) {
					while(x<-wtest)x+=wrap;
					while(x>wtest)x-=wrap;
				}
				double r = x*x+y*y;
				if(r<test) {
					plot[j]=plot[i];
				}
			}
		}
		int num=0;
		for(int i=0 ; i<n  ; i++) if(plot[i]==i) num++;
	}
	public void setMaterialFlags(int flags) {
		if( flags==materialFlags ) return;
		materialFlags = flags;
		pdb.repaintMap();
	}
	public void setDataFlags(int flags) {
		if( flags==dataFlags ) return;
		dataFlags = flags;
		pdb.repaintMap();
	}
	public void setRockFlags(long flags) {
		if( flags==rockFlags ) return;
		rockFlags = flags;
		pdb.repaintMap();
	}
	public void setAlterationFlags(int flags) {
		if( flags==altFlags ) return;
		altFlags = flags;
		pdb.repaintMap();
	}
	public synchronized void setArea( java.awt.geom.Rectangle2D bounds, double zoom ) {
		double test = 1.;
		int ktest = 3;
		while( ktest>= 0 && zoom*zoom > test ) {
			test *= 4.;
			ktest--;
		}
		int[] tmp = new int[stations.length];
		int k=0;
		double x1 = bounds.getX();
		double x2 = x1+bounds.getWidth();
		double y1 = bounds.getY();
		double y2 = y1+bounds.getHeight();
		for(int i=0 ; i<stations.length ; i++) {
			PDBStation station = PDBStation.get(stations[i]);
			if( !station.hasMaterials(materialFlags) )continue;
			if( !station.hasDataType( dataFlags )) continue;
			if( !station.hasRockType( rockFlags )) continue;
			double y = station.getY();
			if( y<y1 || y>y2 )continue;
			double x = station.getX();
			if( wrap>0. ) {
				while( x<x1 )x+=wrap;
			}
			if(x>x2) continue;
			tmp[k++] = stations[i];
		}
		if( k!=stations.length ) {
			current = new int[k];
			System.arraycopy(tmp, 0, current, 0, k);
		} else {
			current = tmp;
		}
		current = pdb.getAnalysisModel().search();
		if(zoom < 5.) {
			int[] tmp1 = new int[k];
			boolean[] ok = new boolean[PDBStation.size()];
			for( int i=0 ; i<ok.length ; i++) ok[i]=false;
			int k1 = 0;
			for(int i=0 ; i<current.length ; i++) {
				int j = current[i];
				if( !ok[plot[j]] ) {
					ok[plot[j]] = true;
					k1++;
				}
			}
			toPlot = new int[k1];
			k1 = 0;
			for( int i=0 ; i<ok.length ; i++) if(ok[i]) toPlot[k1++]=i;
		} else {
			toPlot = current;
		}
		pdb.getCompiledModel().search();
		updateStationIndexMap();
		if (lastSortedCol != -1) {
			ascent = !ascent;
			sortByColumn(lastSortedCol);
		}
		else
			fireTableDataChanged();
	}

	private synchronized void updateStationIndexMap() {
		stationToIndex.clear();
		for (int i = 0; i < current.length; i++) {
			stationToIndex.put(PDBStation.get(current[i]), new Integer(i));
		}
	}

	// Sorts on column col, alternating between ascent and descent
	public synchronized void sortByColumn(int col) {
		if (lastSortedCol == col)
			ascent = !ascent;
		else
			ascent = true;
		lastSortedCol = col;

		Integer[] tmp = new Integer[current.length];
		for (int i = 0; i < current.length; i++)
			tmp[i] = new Integer(current[i]);
		Arrays.sort(tmp, columnSorter);
		for (int i = 0; i < current.length; i++)
			current[i] = tmp[i].intValue();

		updateStationIndexMap();
		fireTableDataChanged();
	}
	
	public synchronized void sortRows() {
		ascent = !ascent;

		Integer[] tmp = new Integer[current.length];
		for (int i = 0; i < current.length; i++)
			tmp[i] = new Integer(current[i]);
		Arrays.sort(tmp, rowSorter);
		for (int i = 0; i < current.length; i++)
			current[i] = tmp[i].intValue();

		updateStationIndexMap();
		fireTableDataChanged();
	}

	private Object getValueAt(PDBStation s, int col) {
		if( col==0 ) {
			return new Integer(s.howManySamples());
		} else if( col==4 ) {
			return new Float( s.getLongitude());
		} else if( col==5 ) {
			return new Float( s.getLatitude());
		} else if( col==1 ) {
			StringBuffer sb = new StringBuffer();
			boolean tf = false;
			for( int i=0 ; i<PDBMaterial.size() ; i++) {
				if( s.hasMaterials( 1<<i ) ) {
					if(tf) sb.append("; ");
					tf = true;
					sb.append(PDBMaterial.materialCode[i][0]);
				}
			}
			return sb.toString();
		} else if( col==2 ) {
			StringBuffer sb = new StringBuffer();
			boolean tf = false;
			for( int i=0 ; i<PDBDataType.getGroupSize() ; i++) {
				if( s.hasDataType( 1<<i ) ) {
					if(tf) sb.append("; ");
					tf = true;
					sb.append(PDBDataType.dataCode[i][0]);//MAJ,TE,REE etc.
				}
			}
			return sb.toString();
		} else if( col==3 ) {
			StringBuffer sb = new StringBuffer();
			boolean tf = false;
			for( int i=0 ; i<PDBRockType.size() ; i++) {
				if( s.hasRockType( 1L<<i ) ) {
					if(tf) sb.append("; ");
					tf = true;
					sb.append(PDBRockType.rockCode[i][0]);
				}
			}
			return sb.toString();
		} else {
			return null;
		}
	}

	// classes implementing TableModel
	public int getRowCount() {
		return current.length;
	}
	public Object getValueAt(int row, int col) {
		PDBStation s = PDBStation.get(current[row]);
		return getValueAt(s, col);
	}
	public String getColumnName( int col ) {
		return colName[col];
	}
	public int getColumnCount() {
		return 6;
	//	return colName.length;
	}
	public boolean isCellEditable( int row, int col) {
		return false;
	}
	public String getRowName( int row ) {
		return PDBStation.get(current[row]).getID();
	}
	/*
	static String[] alterationCode = { "F", "S", "M", "E", "A", "T"};
	static String[][] materialCode = {
				{ "WR", "whole rock"},
				{ "GL", "glass" },
				{ "ROCK", "Rock (unspec.)" },
				{ "MIN", "mineral"},
				{ "INC", "inclusion" },
				{ "CC", "CC" },
				{ "G", "G" },
				{ "GM", "GM" } };
	static String[][] dataCode = {
				{"M", "major"},
				{"T", "trace"},
				{"I", "isotope"},
				{"MIN", "mineral"},
				{"INC", "inclusion" },
				{"N", "noble gas" },
				{"V", "volatile" },
				{"AGE", "age"},
				{"RM", "rock mode" } };
	*/

	public void dispose() {
		stations = null;
		stationToIndex.clear();
		columnSorter = null;
	}
}
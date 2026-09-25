package haxby.db.pdb;

import haxby.util.*;
import java.util.*;
public class PDBAnalysisModel extends SortableTableModel {
	PDB pdb;

	Vector analyses;
	HashMap analysisToIndex; // PDBAnalysis to index in table

	boolean[] codes;
	PDBDataType dt=null;
	int[] cols = null;

	// Keeps track of sorted Col and direction
	private int lastSortedCol = -1;
	private boolean ascent = true;

	// Column sorter is used in sorting index's of row data
	// Column sorter compares two PDBAnlysis based on the columb lastSordedCol
	private Comparator columnSorter = new Comparator() {
		public int compare(Object arg0, Object arg1) {
			int cmp;

			PDBAnalysis a0 = (PDBAnalysis) arg0;
			PDBAnalysis a1 = (PDBAnalysis) arg1;

			Float f0 = getValueAt(a0, lastSortedCol);
			Float f1 = getValueAt(a1, lastSortedCol);

			if (f0 == null && f1 == null)
				cmp = 0;
			else if (f0 == null)
				cmp = -1;
			else if (f1 == null)
				cmp = 1;
			else
				cmp = f0.floatValue() - f1.floatValue() > 0 ? 1 : -1;

			return ascent ? cmp : -cmp;
		}
	};

	private Comparator<PDBAnalysis> rowSorter = new Comparator<PDBAnalysis>() {
		public int compare(PDBAnalysis a0, PDBAnalysis a1) {
			int cmp = a0.getName().compareToIgnoreCase(a1.getName());
			return ascent ? cmp : -cmp;
		}
	};
	
	public PDBAnalysisModel(PDB pdb) {
		super();
		this.pdb = pdb;
		analysisToIndex = new HashMap();
		try { 
			dt = new PDBDataType();
		} catch(Exception ex) {
		}
		codes = new boolean[dt.size()];
	}
	public synchronized int[] search( ) {
		if( PDBSample.sample==null ) {
			if(analyses.size()==0)return new int[] {};
			analyses = new Vector();
			fireTableStructureChanged();
			return new int[] {};
		}
		PDBStationModel model = pdb.getModel();
		int material = model.materialFlags;
		int data = model.dataFlags;
		long rock = model.rockFlags;
		int[] stations = model.current;
		int[] currentStations = new int[stations.length];
		for(int k=0 ; k<dt.size() ; k++) codes[k]=false;
		analyses = new Vector();
		int stn = 0;
		for(int i=0 ; i<stations.length ; i++ ) {
			int stationNum = stations[i];
			PDBStation st = PDBStation.get(stations[i]);
			
			int[] snum = st.getSampleNums();
			for( int j=0 ; j<snum.length ; j++) {
				int jj = snum[j];
				PDBSample samp = PDBSample.sample.get(jj);
				if (samp == null) continue;

				if( !samp.hasRockType(rock) ) continue;
				for( int k=0 ; k<samp.batch.length ; k++) {
					PDBBatch b = samp.batch[k];
					if( !b.hasMaterial(material) ) continue;
					if( !b.hasDataType(data) ) continue;
					for( int m=0 ; m<b.analyses.length ; m++) {
						PDBAnalysis a = b.analyses[m];
						if( !a.hasDataType(data) ) continue;
						if(stn==0 || currentStations[stn-1]!=stationNum) {
							currentStations[stn++] = stationNum;
						}
						analyses.add(a);
						for( int p=0 ; p<a.code.length ; p++) {
							int test = (int)a.code[p];	
							if( (PDBDataType.getGroupFlag(test)&data)==0)continue;
							codes[test] = true;
						}
					}
				}
			}
		}
		int[] tmpStn = new int[stn];
		System.arraycopy( currentStations, 0, tmpStn, 0, stn );
		currentStations = tmpStn;
		int ng = dt.dataCode.length;
		Vector[] gp = new Vector[ng];
		for( int k=0 ; k<ng ; k++) {
			gp[k] = new Vector();
		}
		int nCode = 0;
		for(int i=0 ; i<codes.length ; i++) {
			if( !codes[i] )continue;
			int g = dt.getGroupIndex( i );
			if(g>=0 && g<ng)gp[g].add(new Integer(i));
			nCode++;
		}
		cols = new int[nCode];
		int n=0;
		for(int k=0 ;k<gp.length ; k++) {
			int[] order = new int[gp[k].size()];
			for(int i=0 ; i<gp[k].size() ; i++) {
				order[i] = ((Integer)gp[k].get(i)).intValue();
			}
			for(int i=0 ; i<gp[k].size() ; i++) {
				for(int j=i+1 ; j<gp[k].size() ; j++) {
					if( dt.order[order[i]] > dt.order[order[j]] ) {
						int tmp = order[i] ;
						order[i] = order[j];
						order[j] = tmp;
					}
				}
			}
			for(int i=0 ; i<order.length ; i++) {
				cols[n++] = order[i];
			}
		}

		updateAnalysisIndexMap();

		fireTableStructureChanged();
		if (lastSortedCol != -1) {
			ascent = !ascent;
			sortByColumn(lastSortedCol);
		}
		return currentStations;
	}

	private synchronized void updateAnalysisIndexMap() {
		analysisToIndex.clear();
		for (int i = 0; i < analyses.size(); i++) {
			analysisToIndex.put(analyses.get(i), new Integer(i));
		}
	}

	// Sorts on column col, alternating between ascent and descent
	public synchronized void sortByColumn(int col) {
		if (lastSortedCol == col)
			ascent = !ascent;
		else
			ascent = true;
		lastSortedCol = col;

		Collections.sort(analyses, columnSorter);

		updateAnalysisIndexMap();
		fireTableDataChanged();
	}
	
	public synchronized void sortRows() {
		ascent = !ascent;

		Collections.sort(analyses, rowSorter);

		updateAnalysisIndexMap();
		fireTableDataChanged();
	}
	
	public Float getValueAt(PDBAnalysis a, int col) {
		for( int k=0 ; k<a.code.length ; k++) {
			if( (int)a.code[k]==cols[col] )return new Float(a.val[k]);
		}
		return null;
	}

	// methods implementing TableModel
	public int getRowCount() {
		if(analyses.size()>123138) return 1;
		return analyses.size();
	}
	public Object getValueAt(int row, int col) {
		if(analyses.size()>123138) return "";
		PDBAnalysis a = (PDBAnalysis)analyses.get(row);
		return getValueAt(a, col);
	}
	public String getColumnName( int col ) {
		if(analyses.size()>123138) {
			return "?";
		}
		String units = PDBDataType.getUnits(cols[col]);
		if(units==null || units.equals("null") ) units="";
		else units = " ("+units+")";
		String name = PDBDataType.getName(cols[col]);
		return name + units;
	}
	public int getColumnCount() {
		if(analyses.size()>123138) return 1;
		if(cols==null)return 0;
		return cols.length;
	}
	public boolean isCellEditable( int row, int col) {
		return false;
	}
	public String getRowName( int row ) {
		if(analyses.size()>123138) return "Too many analyses (>123138). Please zoom in.";
		return ((PDBAnalysis)analyses.get(row)).getName();
	}

	public void dispose() {
		analyses.clear();
		analysisToIndex.clear();
	}
}
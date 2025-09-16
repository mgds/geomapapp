package haxby.db.pdb;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import haxby.util.SortableTableModel;
public class PDBSampleModel extends SortableTableModel {
	PDB pdb;
	Vector samples;
	HashMap sampleToIndex; // PDBSample to index in table

	boolean[] codes;
	PDBDataType dt=null;
	int[] cols = null;

	// Keeps track of sorted Col and direction
	private int lastSortedCol = -1;
	private boolean ascent = true;

	// Column sorter is used in sorting index's of row data
	// Column sorter compares two PDBSample based on the column lastSordedCol
	private Comparator columnSorter = new Comparator() {
		public int compare(Object arg0, Object arg1) {
			int cmp;

			PDBSample a0 = (PDBSample) arg0;
			PDBSample a1 = (PDBSample) arg1;

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

	private Comparator<PDBSample> rowSorter = new Comparator<PDBSample>() {
		public int compare(PDBSample a0, PDBSample a1) {
			int cmp = a0.getName().compareToIgnoreCase(a1.getName());
			return ascent ? cmp : -cmp;
		}
	};
	
	public PDBSampleModel(PDB pdb) {
		super();
		this.pdb = pdb;
		try { 
			dt = new PDBDataType();
		} catch(Exception ex) {
		}
		samples = new Vector();
		sampleToIndex = new HashMap();
		codes = new boolean[dt.size()];
	}

	public synchronized void search( ) {
		if( PDBSample.sample==null ) {
			if(samples.size()==0)return;
			samples = new Vector();
			fireTableStructureChanged();
			return;
		}
		PDBStationModel model = pdb.getModel();
		int material = model.materialFlags;
		int data = model.dataFlags;
		long rock = model.rockFlags;
		int[] stations = model.current;
		for(int k=0 ; k<dt.size() ; k++) codes[k]=false;  //dt.size() total number of stations in station model
		samples = new Vector();
		for(int i=0 ; i<stations.length ; i++ ) {
			PDBStation st = PDBStation.get(stations[i]);

			int[] snum = st.getSampleNums();
			for( int j=0 ; j<snum.length ; j++) {
				int jj = snum[j];

				PDBSample samp = PDBSample.sample.get(jj);
				if (samp == null) continue;
				
				if( !samp.hasRockType(rock) ) continue;
				boolean compiled = false;
				for( int k=0 ; k<samp.batch.length ; k++) {
					PDBBatch b = samp.batch[k];
					if( !b.hasMaterial(material) ) continue;
					if( !b.hasDataType(data) ) continue;
					for( int m=0 ; m<b.analyses.length ; m++) {
						PDBAnalysis a = b.analyses[m];
						if( !a.hasDataType(data) ) continue;
						for( int p=0 ; p<a.code.length ; p++) {
							if( !a.compiled[p] ) continue;
							int test = (int)a.code[p];	
							if( (PDBDataType.getGroupFlag(test)&data)==0)continue;
							codes[test] = true;
							compiled = true;
							break;
						}
					}
				}
				if( compiled ) {
					samples.add( samp );
				}
			}
		}
		int ng = dt.dataCode.length;
		Vector[] gp = new Vector[ng];
		for( int k=0 ; k<ng ; k++) {
			gp[k] = new Vector();
		}
		int nCode = 0;
		for(int i=0 ; i<codes.length ; i++) {
			if( !codes[i] )continue;
			int g = dt.getGroupIndex( i );
			if(g>=0 && g<ng) {
				gp[g].add(new Integer(i));
				nCode++;
			}
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
		cols = pdb.getAnalysisModel().cols;

		updateSampleIndexMap();
		fireTableStructureChanged();
		if (lastSortedCol != -1) {
			ascent = !ascent;
			sortByColumn(lastSortedCol);
		}
	}

	private synchronized void updateSampleIndexMap() {
		sampleToIndex.clear();
		for (int i = 0; i < samples.size(); i++) {
			sampleToIndex.put(samples.get(i), new Integer(i));
		}
	}

	// Sorts on column col, alternating between ascent and descent
	public synchronized void sortByColumn(int col) {
		if (lastSortedCol == col)
			ascent = !ascent;
		else
			ascent = true;
		lastSortedCol = col;

		Collections.sort(samples, columnSorter);

		updateSampleIndexMap();
		fireTableDataChanged();
	}
	
	public synchronized void sortRows() {
		ascent = !ascent;

		Collections.sort(samples, rowSorter);

		updateSampleIndexMap();
		fireTableDataChanged();
	}

	//Really get the column name, ignoring sample size
	public String getColumnName( int col , boolean ignoreSampleSize) {
		if (!ignoreSampleSize) return getColumnName(col);

		String units = PDBDataType.getUnits(cols[col]);
		if(units==null || units.equals("null") ) units="";
		else units = " ("+units+")";
		String name = PDBDataType.getName(cols[col]);
		return name + units;
	}

	public int getColumnCount (boolean ignoreSampleSize) {
		if (!ignoreSampleSize) return getColumnCount();

		if(cols==null)return 0;
		return cols.length;
	}

	public Float getValueAt(PDBSample samp,int col) {
		PDBStationModel model = pdb.getModel();
		int material = model.materialFlags;
		int data = model.dataFlags;
		long rock = model.rockFlags;
		try{ 
			for( int k=0 ; k<samp.batch.length ; k++) {
				PDBBatch b = samp.batch[k];
				if( !b.hasMaterial(material) ) continue;
				if( !b.hasDataType(data) ) continue;
				for( int m=0 ; m<b.analyses.length ; m++) {
					PDBAnalysis a = b.analyses[m];
					if( !a.hasDataType(data) ) continue;
					for( int p=0 ; p<a.code.length ; p++) {
						if( !a.compiled[p] ) continue;
						if( a.code[p]<0 )continue;
						try {
							if( (int)a.code[p]==cols[col] )return new Float(a.val[p]);
						} catch(ArrayIndexOutOfBoundsException ex) {
							System.out.println(ex);
						}
					}
				}
			}
		} catch (NullPointerException ne) {
			return null;
		}
		return null;
	}

	// methods implementing TableModel 47730
	public int getRowCount() {
//		if( samples.size()>47555 ) return 1;
		return samples.size();
	}

	public Object getValueAt(int row, int col) {
//System.out.println("r " + row + " c " + col + " s " + samples.size());

//		if( samples.size()>47555 ) return "";
		PDBSample samp = (PDBSample)samples.get(row);
		return getValueAt(samp, col);
	}
	public String getColumnName( int col ) {
//		if( samples.size()>47555 ) {
//			return "?";
//		}
		String units = PDBDataType.getUnits(cols[col]);
		if(units==null || units.equals("null") ) units="";
		else units = " ("+units+")";
		String name = PDBDataType.getName(cols[col]);
		return name + units;
	}
	public int getColumnCount() {
//		if( samples.size()>47555 ) return 1;
		if(cols==null)return 0;
		return cols.length;
	}
	public boolean isCellEditable( int row, int col) {
		return false;
	}
	public String getRowName( int row ) {
//		if( samples.size()>47555 ) return "Too many samples (>47730). Please zoom in.";
		return ((PDBSample)samples.get(row)).getName();
	}

	public void dispose() {
		samples.clear();
		sampleToIndex.clear();
	}
}
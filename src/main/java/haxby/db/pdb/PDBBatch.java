package haxby.db.pdb;

public class PDBBatch {
	short ref;
	byte material;
//	byte mineral;
	PDBSample parent;
	PDBAnalysis[] analyses;
	short dataTypes;
	public PDBBatch( PDBSample sample, 
			int refNum, 
			int materialCode 
		//	int mineralCode 
			) {
		parent = sample;
//		ref = (short)refNum; // not used?
		material = (byte)materialCode;
//		mineral = (byte)mineralCode; // not used?
		analyses = new PDBAnalysis[0];
		dataTypes = 0;
	}
	public String getName(PDBAnalysis a) {
		for( int i=0 ; i<analyses.length ; i++) {
			if(analyses[i]==a) {
				return parent.getName(this)+":"+i;
			}
		}
		return parent.getName(this)+":";
	}
	public int getStationNum() {
		return parent.getStationNum();
	}
	public void addAnalysis( PDBAnalysis a ) {
		PDBAnalysis[] tmp = new PDBAnalysis[analyses.length+1];
		for(int i=0 ; i<analyses.length ; i++) tmp[i] = analyses[i];
		tmp[analyses.length] = a;
		analyses = tmp;
		dataTypes |= a.dataTypes;
	}
	public boolean hasDataType(int types) {
		return ((int)dataTypes & types) != 0;
	}
	public boolean hasMaterial( int type ) {
		return ((int)material & type) != 0;
	}
	public void dispose() {
		parent = null;
		for (int i = 0; i < analyses.length; i++)
		{
			analyses[i].dispose();
			analyses[i] = null;
		}
		analyses = null;
	}
}

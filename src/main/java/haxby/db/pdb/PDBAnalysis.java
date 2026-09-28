package haxby.db.pdb;

public class PDBAnalysis {
	PDBBatch parent;
	short[] code;
	float[] val;
	float[] stdDev;
	boolean[] compiled;
	short dataTypes;
	public PDBAnalysis(PDBBatch batch, 
			short[] code, 
			float[] val, 
//			float[] stdDev,
			boolean[] compiled) {
		parent = batch;
		this.code = code;
		this.val = val;
//		this.stdDev = stdDev; // not used?
		this.compiled = compiled;
		int data = 0;
		for(int i=0 ; i<code.length ; i++) {
			data |= PDBDataType.getGroupFlag((int)code[i]);
		}
		dataTypes = (short)data;
	}
	public String getName() {
		return parent.getName(this);
	}
	public int getStationNum() {
		return parent.getStationNum();
	}
	public boolean hasDataType(int types) {
		return ((int)dataTypes & types) != 0;
	}
	public void dispose() {
		parent = null;
	}
}

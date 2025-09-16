package haxby.db.pdb;

public class PDBChemistry {
	public short code;
	public float val;
	public float stdev;
	public PDBChemistry( short code, float val, float stdev ) {
		this.code = code;
		this.val = val;
		this.stdev = stdev;
	}
}

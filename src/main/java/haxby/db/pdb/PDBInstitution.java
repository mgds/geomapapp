package haxby.db.pdb;

public class PDBInstitution {
	static PDBInstitution[] institutes = null;
	String name;
	public PDBInstitution(String name) {
		this.name = name;
	}
	public String toString() {
		return name;
	}
	public String getName() {
		return name;
	}
	public int getIndex() {
		for(int i=0 ; i<institutes.length ; i++) {
			if(institutes[i]==this) return i;
		}
		return -1;
	}
	static void initInstitutes( int n ) {
		institutes = new PDBInstitution[n];
	}
	static void addInstitution( PDBInstitution institute, int index ) {
		if(institutes == null) initInstitutes( index );
		else if(institutes.length <= index ) {
			PDBInstitution[] tmp = new PDBInstitution[index+1];
			System.arraycopy( institutes, 0, tmp, 0, institutes.length);
			institutes = tmp;
		}
		institutes[index] = institute;
	}
	public static PDBInstitution getInstitution( int index ) {
		return institutes[index];
	}
	public static int size () {
		if( institutes==null )return 0;
		return institutes.length;
	}
}

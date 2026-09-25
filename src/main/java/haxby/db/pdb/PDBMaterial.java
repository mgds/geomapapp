package haxby.db.pdb;

public class PDBMaterial {
	static PDBMaterial[] material = null;
	public final static String[][] materialCode = {
				{ "WR", "Whole Rock"}, //1				db = 8				--> material bit shift = 0
				{ "GL", "Glass" }, //2 					db = 3				--> material bit shift = 1
				{ "ROCK", "Rock (unspec.)" }, //4		db = 7				--> material bit shift = 2
				{ "MIN", "mineral"} //8				db = 6				--> material bit shift = 3
				//{ "INC", "inclusion" }, //16			db = 5				--> material bit shift = 4
				//{ "CC", "CC" }, // 32 				db = 1				--> material bit shift = 5
				//{ "G", "G" }, // 64					db = no longer there--> material bit shift = 6
				//{ "GM", "GM" }  //128					db = 4				--> material bit shift = 7
				}; 
	static boolean initiallized = false;
	String abbrev;
	String name;
	int code;
	public PDBMaterial(String abbrev, String name, int code) {
		this.abbrev = abbrev;
		this.name = name;
		this.code = code;
	}
	static void load() {
		material = new PDBMaterial[materialCode.length];
		for(int i=0 ; i<material.length ; i++ ) {

			material[i] = new PDBMaterial(materialCode[i][0],
					materialCode[i][1], (1<<i));
		}
		initiallized = true;
	}
	public static int size() {
		return material.length;
	}
	public static PDBMaterial get( int index ) {
		return material[index];
	}
	static void unload() {
		material = null;
		initiallized = false;
	}
}
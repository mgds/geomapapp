package haxby.db.pdb;

public class PDBAlteration {
	static PDBAlteration[] alteration = null;
	static String[][] alterationCode = {
				{ "0", "Fresh"},
				{ "1", "Slight" },
				{ "2", "Moderate" },
				{ "3", "Extensive" },
				{ "4", "Even more" },
				{ "5", "Total" } };
	static boolean initiallized = false;
	String abbrev;
	String name;
	int code;
	public PDBAlteration(String abbrev, String name, int code) {
		this.abbrev = abbrev;
		this.name = name;
		this.code = code;
	}
	static void load() {
		alteration = new PDBAlteration[alterationCode.length];
		for(int i=0 ; i<alteration.length ; i++ ) {
			alteration[i] = new PDBAlteration(alterationCode[i][0],
					alterationCode[i][1], (1<<i));
		}
		initiallized = true;
	}
	public static int size() {
		return alteration.length;
	}
	public static PDBAlteration get( int index ) {
		return alteration[index];
	}
}

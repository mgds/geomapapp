package haxby.db.pdb;

public class PDBRockType {
	static PDBRockType[] rock = null;
	static String[][] rockCode = {
				{ "IPF", "igneous:plutonic:felsic", "1" },
				{ "IPM", "igneous:plutonic:mafic", "2" },
				{ "IPU", "igneous:plutonic:ultramafic", "4" },
				{ "IVC", "igneous:volcanic:clastic", "8" },
				{ "IVF", "igneous:volcanic:felsic", "16" },
				{ "IVI", "igneous:volcanic:intermediate", "32" },
				{ "IVM", "igneous:volcanic:mafic", "64" },
				{ "MET", "metamorphic", "128" },
				{ "SDUN", "sedimentary:unknown", "256" },
				{ "VEI", "vein", "512" },
				{ "IVU", "igneous:volcanic:ultramafic", "1024" },
				{ "IPI", "igneous:plutonic:intermediate", "2048" },
				{ "E", "exotic", "4096" },
				{ "X", "xenolith", "8192" },
				{ "Z", "sedimentary", "16384" },
				{ "XPM", "xenolith:plutonic:mafic", "32768" },
				{ "XPI", "xenolith:plutonic:intermediate", "65536" },
				{ "XPU", "xenolith:plutonic:ultramafic", "131072" },
				{ "XVM", "xenolith:volcanic:mafic", "262144" },
				{ "ALT", "altered material", "524288" },
				{ "SDBI", "sedimentary:biogenic", "1048576" },
				{ "SDCR", "sedimentary:carbonate", "2097152" },
				{ "SDCH", "sedimentary:chemical", "4194304" },
				{ "SDCG", "sedimentary:conglomerate&breccia", "8388608" },
				{ "SDEV", "sedimentary:evaporite", "16777216" },
				{ "SDGP", "sedimentary:glacial&paleosol", "33554432" },
				{ "SDIS", "sedimentary:ironstone", "67108864" },
				{ "SDMT", "sedimentary:metalliferous", "134217728" },
				{ "SDCS", "sedimentary:mixed_carb-siliciclastic", "268435456" },
				{ "SDPH", "sedimentary:phosphorite", "536870912" },
				{ "SDSI", "sedimentary:siliceous", "1073741824" },
				{ "SDSB", "sedimentary:siliceous_biogenic", "2147483648" },
				{ "SDSC", "sedimentary:siliciclastic", "4294967296" },
				{ "SDVL", "sedimentary:volcaniclastic", "8589934592" },
				{"ore", "ore", "17179869184"},
				{ "UNKNOWN", "UNKNOWN", "34359738368" }};

	static boolean initiallized = false;
	String abbrev;
	String name;
	String code;

	public PDBRockType(String abbrev, String name, String code) {
		this.abbrev = abbrev;
		this.name = name;
		this.code = code;
	}
	static void load() {
		rock = new PDBRockType[rockCode.length];
		for(int i=0 ; i<rock.length ; i++ ) {
			rock[i] = new PDBRockType(rockCode[i][0], rockCode[i][1], rockCode[i][2]);
		}
		initiallized = true;
	}
	static void unload() {
		rock = null;
		initiallized = false;
	}
	public static int size() {
		return rock.length;
	}
	public static PDBRockType get( int index ) {
		return rock[index];
	}
}
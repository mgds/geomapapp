package org.geomapapp.db.dsdp;

public class FossilDatum {
	public static String[] timeScales = new String[] {
				"Hilgen et al., 1995",
				"Curry, Shackleton et al., 1995",
				"Shackleton and Crowhurst, 1997",
				"Berggren, et al., 1995b, SEPM-54",
				"Shackleton, Crowhurst, Hagelberg et al., 1995",
				"Cande and Kent, 1992",
				"Gradstein, et al., 1995",
				"Lourens et al., 1996",
				"Harland et al., 1990",
				"Cande and Kent, 1995",
				"Gradstein, et al., 1994",
				"Berggren et al., 1995a, GSA",
				"Berggren et al., 1985",
				"Shackleton, Berger & Peltier, 1990",
				"Clement & Robinson, 1987",
				"Tiedemann and Franz, 1997",
				"Bickert et al., 1997",
				"Morgans et al. 1996"
			};
	public static String[] types = new String[] {
				"FO",
				"LO"
			};
	public float age;
	public short timeScale;
	public int code;
	public short type;
	public int groupIndex;

	public FossilDatum( float age, int timeScale, int code, int type, int groupIndex ) {
		this.age = age;
		this.timeScale = (short)timeScale;
		this.code = code;
		this.type = (short)type;
		this.groupIndex = groupIndex;
	}
	public boolean equals( Object o) {
		try {
			FossilDatum f = (FossilDatum)o;
			return f.age==age 
				&& f.timeScale==timeScale
				&& f.code==code
				&& f.type==type;
		} catch(Exception e) {
			return false;
		}
	}
	public static int getTimeScale( String name ) {
		for( int k=0 ; k<timeScales.length ; k++) if( name.equals(timeScales[k]) )return k;
		return -1;
	}
}

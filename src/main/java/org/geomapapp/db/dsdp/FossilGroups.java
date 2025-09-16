package org.geomapapp.db.dsdp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.StringTokenizer;

public class FossilGroups {
	String[] name;
	String[] abbrev;
	FossilGroups() {
	}
	public String getGroupName(int index) {
		return name[index-1];
	}
	public String getGroupAbbreviation(int index) {
		return abbrev[index-1];
	}
	public static FossilGroups getFossilGroups() throws IOException {
		FossilGroups gps = new FossilGroups();
		BufferedReader in = new BufferedReader(
				new FileReader( "/home/bill/projects/DSDP2000/janus/fauna/groups.tsf" ));
	//	BufferedReader in = new BufferedReader(
	//		new InputStreamReader( (URLFactory.url(Taxon.BASE+"groups.tsf")).openStream() ));
		String s = in.readLine();
		gps.name = new String[10];
		gps.abbrev = new String[10];
		for( int k=0 ; k<10 ; k++) {
			s = in.readLine();
			StringTokenizer st = new StringTokenizer(s,"\t");
			st.nextToken();
			gps.name[k] = st.nextToken();
			gps.abbrev[k] = st.nextToken();
		}
		in.close();
		return gps;
	}
}

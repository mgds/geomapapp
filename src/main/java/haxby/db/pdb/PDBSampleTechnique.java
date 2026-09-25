package haxby.db.pdb;

import haxby.map.MapApp;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.Vector;

public class PDBSampleTechnique {
	static byte[][] abbrev=null;
	static byte[][] name=null;
	static boolean loaded = false;

	static String PETDB_PATH = PathUtil.getPath("PORTALS/PETDB_PATH",
			MapApp.BASE_URL+"/data/portals/petdb/");

	public PDBSampleTechnique() throws IOException {
		if(!loaded) load();
	}
	public static void load() throws IOException {
		if(loaded) return;
		// sample_techiques not in use
		URL url = URLFactory.url(PETDB_PATH + "sample_techniques");
		InputStream urlIn = url.openStream();
		BufferedReader in = new BufferedReader(
				new InputStreamReader(urlIn));
		Vector v = new Vector();
		Vector v1 = new Vector();
		String line;
		while( (line=in.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line);
			v.add(st.nextToken());
			v1.add(st.nextToken());
		}
		try {
			in.close();
			urlIn.close();
		} catch(IOException ex) {
		}
		abbrev = new byte[v.size()][];
		name = new byte[v.size()][];
		for(int i=0 ; i<v.size() ; i++) {
			abbrev[i] = ( (String)v.get(i) ).getBytes();
			name[i] = ( (String)v1.get(i) ).getBytes();
		}
		loaded = true;
	}
	public static int size() {
		return name.length;
	}
	public static String getAbbreviation( int index ) {
		return new String( abbrev[index] );
	}
	public static String getName( int index ) {
		return new String( name[index] );
	}
}

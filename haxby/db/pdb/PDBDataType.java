package haxby.db.pdb;

import haxby.map.MapApp;
import haxby.util.NumberFormatUtil;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

public class PDBDataType {
	/* FIXME: Lulin Song leave 'Age' and 'EM' here. It will be removed in the future when pdb_dataC code[i] matches the new list */
	public final static String[][] dataCode = {
				{ "AGE", "Age" },        /*Kerstin recommend to remove this. But pdb_dataC file use this */
				{ "MAJ", "Major" },
				{ "TE", "Trace" },
				{ "REE", "Rare Earth" },
				{ "IR", "Radio-Isotope" },
				{ "IS", "Stable Isotope" },
				{ "NGAS", "Noble Gas" },
				{ "VO", "Volatile" },
				{ "US", "U-Series" },
				{ "EM", "End Member" }, /* Kerstin recommend to remove this. But pdb_dataC file use this */
				{ "RT", "Ratio" },
				{ "MODE", "Rock Mode" },
				{ "MD", "Model Data" },
				{ "SPEC", "Speciation Ratio" },
				{ "GEO", "Geospatial" }
	};
	static byte[] group;
	static byte[][] name;
	static byte[][] units;
	static int[] order;
	static boolean initiallized = false;
	
	private static JDialog dialog = null;

	static String PETDB_PATH = PathUtil.getPath("PORTALS/PETDB_PATH",
			MapApp.BASE_URL+"/data/portals/petdb/");

	public PDBDataType() throws IOException {
		if( !initiallized ) load();
	}
	static void setProgressDialog(JDialog dlg) {
		dialog = dlg;
	}
	static void load() throws IOException {
		if( initiallized ) return;
		Vector<String[]> v = new Vector<String[]>();
		//URL url = URLFactory.url(PETDB_PATH + "June2014/item_codeA_new.txt");
		URL url = URLFactory.url(PETDB_PATH + "petdb_latest/pdb_item_codeA_new.tsv");
		URLConnection urlConn = url.openConnection();
		urlConn.setDoInput(true); 
		urlConn.setUseCaches(false);

		BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

		//first read the header
		String s = in.readLine();
		//then the data
		int count = 0;
		while( (s=in.readLine()) != null) {
			count++;
			if(null != dialog) {
				final int tnuoc = count;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						dialog.setTitle("Reading the " + NumberFormatUtil.cardinalToOrdinal(tnuoc) + " data type");
					}
				});
			}
			String [] results = s.split("\\t");
			v.add( new String[] {results[0], 
					results[1], 
					results[2], 
					results[3]} );
		}
		count = 0;

		try {
			in.close();
		} catch ( IOException ex ) {
		}

		Vector<String> groups = new Vector<String>();
		for(int i=0 ; i<dataCode.length ; i++ ) {
			groups.add( dataCode[i][0] );
		}
		group = new byte[v.size()];
		name = new byte[v.size()][];
		units = new byte[v.size()][];
		order = new int[v.size()];

		for(int i=0 ; i<v.size() ; i++) {
			String[] ss = (String[])v.get(i);
			group[i] = (byte)groups.indexOf( ss[1] );
			name[i] = ss[0].getBytes();
			units[i] = ss[2].getBytes();

			if( units[i].equals("null") )units[i]=null;
			order[i] = Integer.parseInt(ss[3]);
			if(null != dialog) {
				dialog.setTitle("Fully processed " + count + " data types of " + v.size());
			}
		}
		initiallized = true;
	}

	public static int getGroupSize() {
		return dataCode.length;
	}
	public static int size() {
		return name.length;
	}
	public static int getGroupIndex( int index ) {
		return (int) group[index];
	}
	public static int getGroupFlag( int index ) {
		return (1 << getGroupIndex(index));
	}
	public static String getGroupAbbrev( int index ) {
		return dataCode[getGroupIndex(index)][0];
	}
	public static String getGroupName( int index ) {
		return dataCode[getGroupIndex(index)][1];
	}
	public static String getName( int index ) {
		String s = new String(name[index]);
		return s;
	}
	public static String getUnits( int index ) {
		return new String(units[index]);
	}
}

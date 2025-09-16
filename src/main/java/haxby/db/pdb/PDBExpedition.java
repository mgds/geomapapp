package haxby.db.pdb;

import haxby.map.MapApp;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import java.util.SortedMap;
import java.util.TreeMap;

public class PDBExpedition {
	static PDBExpedition[] expeditions = null;
	String name;
	short start, end;
	short institution;
	short[] chiefs;
	static boolean loaded = false;

	static String PETDB_PATH = PathUtil.getPath("PORTALS/PETDB_PATH",
			MapApp.BASE_URL+"/data/portals/petdb/");

	public PDBExpedition( String name, 
				short start, 
				short institution,
				short[] chiefs) { 
		this.name = name;
		this.start = start;
		this.institution = institution;
		this.chiefs = chiefs;
	}
	public String toString() {
		return name;
	}
	public String getName() {
		return name;
	}
	public int getStart() {
		return (int)start;
	}
	public int getInstitutionIndex() {
		return (int)institution;
	}
	public short[] getChiefs() {
		short[] tmp = new short[chiefs.length];
		System.arraycopy( chiefs, 0, tmp, 0, chiefs.length );
		return tmp;
	}
	static void init( int n ) {
		expeditions = new PDBExpedition[n];
	}
	static void add( PDBExpedition expedition, int index ) {
		if(expeditions == null) init( index );
		else if(expeditions.length <= index ) {
			PDBExpedition[] tmp = new PDBExpedition[index+100];
			System.arraycopy( expeditions, 0, tmp, 0, expeditions.length);
			expeditions = tmp;
		}
		expeditions[index] = expedition;
	}
	static int trimToSize() {
		int k;
		for( k=expeditions.length-1 ; k>=0 ; k-- ) {
			if( expeditions[k] != null ) break;
		}
		if(k==expeditions.length-1) return size();
		PDBExpedition[] tmp = new PDBExpedition[k+1];
		if(k>=0) {
			System.arraycopy( expeditions, 0, tmp, 0, k+1 );
		}
		expeditions = tmp;
		return size();
	}
	public static void unload() {
		expeditions = null;
		loaded = false;
	}
	public static void load() throws IOException {
		if(loaded) return;
		
		//URL url = URLFactory.url(PETDB_PATH + "June2014/expeditions_new.txt");
		URL url = URLFactory.url(PETDB_PATH + "petdb_latest/pdb_expeditions_new.tsv");
		URLConnection urlConn = url.openConnection();
		urlConn.setDoInput(true); 
		urlConn.setUseCaches(false);
		

		BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
//		BufferedReader in = new BufferedReader(new FileReader("C:\\Users\\Lulin Song\\workspace\\GMA-PetDB-Portal-Branch\\GeoMapApp\\haxby\\db\\pdb\\expeditions_new.txt"));

		// Read Data File
		int index=0;
		short start, institution;
		String code, s;
		short[] chiefs;

		//read the header first
		s = in.readLine();
		//now read the body
		SortedMap<Integer, PDBExpedition> expeditions_map = new TreeMap<>();
		while ((s = in.readLine())!= null){
			String [] results = s.split("\\t");
			index = Integer.parseInt(results[0]);
			start = Short.parseShort(results[1]);
			institution = Short.parseShort(results[2]);
			code = results[3];
			chiefs = new short[Byte.parseByte(results[4])];
			for(int k=0 ; k<chiefs.length ; k++){
				short cheifName = Short.parseShort(results[4 + k]);
				chiefs[k]= cheifName;
			}
			//add( new PDBExpedition(code, start, institution, chiefs), index);
			expeditions_map.put(index, new PDBExpedition(code, start, institution, chiefs));
		}
		init(expeditions_map.lastKey());
		for(Integer i : expeditions_map.keySet()) {
			add(expeditions_map.get(i), i);
		}
		
		try {
			in.close();
		} catch ( IOException ex ) {
		}
		trimToSize();
		loaded = true;
	}
	public static boolean isLoaded() {
		return loaded;
	}
	public static PDBExpedition get( int index ) {
		if( index<0 || index>=size() )return null;
		return expeditions[index];
	}
	public static int size () {
		if( expeditions==null )return 0;
		return expeditions.length;
	}
}

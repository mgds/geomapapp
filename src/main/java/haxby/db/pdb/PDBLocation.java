package haxby.db.pdb;

import haxby.map.MapApp;
import haxby.proj.Projection;

import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import java.util.SortedMap;
import java.util.TreeMap;

public class PDBLocation {
	static PDBLocation[] locations = null;
	float lon;
	float lat;
	short[] elev;
	transient float x, y;
	static boolean loaded = false;

	static String PETDB_PATH = PathUtil.getPath("PORTALS/PETDB_PATH",
			MapApp.BASE_URL+"/data/portals/petdb/");

	public PDBLocation( float lon, float lat, short[] elev ) {
		this.lon = lon;
		this.lat = lat;
		this.elev = elev; // not used?
		x = y = Float.NaN;
	}
	public void project( Projection proj ) {
		Point2D p = proj.getMapXY( new Point2D.Float(lon, lat) );
		x = (float)p.getX();
		y = (float)p.getY();
	}
	public float getLongitude() {
		return lon;
	}
	public float getLatitude() {
		return lat;
	}
	public short[] getElevationRange() {
		short[] tmp = new short[] {elev[0], elev[1]};
		return tmp;
	}
	public Point2D getMapXY() {
		return new Point2D.Float(x, y);
	}
	public double getX() {
		if(Float.isNaN(x)) return Double.NaN;
		return (double)x;
	}
	public double getY() {
		if(Float.isNaN(y)) return Double.NaN;
		return (double)y;
	}
	static void unload() {
		if (!loaded) return;

		for (int i = 0; i < locations.length; i++)
			locations[i] = null;
		locations = null;
		loaded = false;
	}
	public static void load() throws IOException {
		if( loaded ) return;
		
		//URL url = URLFactory.url(PETDB_PATH + "June2014/locations_new.txt"); 
		URL url = URLFactory.url(PETDB_PATH + "petdb_latest/pdb_locations_new.tsv");
		URLConnection urlConn = url.openConnection();
		urlConn.setDoInput(true); 
		urlConn.setUseCaches(false);

		BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
//		BufferedReader in = new BufferedReader(new FileReader("C:\\Users\\Lulin Song\\workspace\\GMA-PetDB-Portal-Branch\\GeoMapApp\\haxby\\db\\pdb\\locations.txt"));

		String s;
		int index=0;
		float lon, lat;
		short[] elev = new short[2];

		//first read the header
		s = in.readLine();
		
		//now read the data
		SortedMap<Integer, PDBLocation> locations_map = new TreeMap<>();
		while ((s = in.readLine())!= null){
			String [] results = s.split("\\t");
			if (results.length < 3) continue;
			index = Integer.parseInt(results[0]);
			lon = Float.parseFloat(results[1]);
			lat = Float.parseFloat(results[2]);
			// elev not used?
		//	elev[0] = Short.parseShort(results[3]);
		//	elev[1] = Short.parseShort(results[4]);
			/* Lulin Changed to the following. elevation min and max are floats in database. */
			//elev[0] = (short)Float.parseFloat(results[3]);
			//elev[1] = (short)Float.parseFloat(results[4]);
			//add( new PDBLocation( lon, lat, elev ), index);
			locations_map.put(index, new PDBLocation(lon, lat, elev));
		}
		init(locations_map.lastKey());
		for(Integer i : locations_map.keySet()) {
			add(locations_map.get(i), i);
		}
		try {
			in.close();
		} catch ( IOException ex ) {
		}
		loaded = true;
	}

	public static boolean isLoaded() {
		return loaded;
	}
	static void init( int n ) {
		locations = new PDBLocation[n];
	}
	static void add( PDBLocation location, int index ) {
		if(locations == null) {
			init( index );
		} else if(locations.length <= index ) {
			PDBLocation[] tmp = new PDBLocation[index+100];
			System.arraycopy( locations, 0, tmp, 0, locations.length);
			locations = tmp;
		}
		locations[index] = location;
	}
	static int trimToSize() {
		int k;
		for( k=locations.length-1 ; k>=0 ; k-- ) {
			if( locations[k] != null ) break;
		}
		if(k==locations.length-1)return size();
		PDBLocation[] tmp = new PDBLocation[k+1];
		if(k>=0) {
			System.arraycopy( locations, 0, tmp, 0, k+1 );
		}
		locations = tmp;
		return size();
	}
	public static PDBLocation get( int index ) {
		return locations[index];
	}
	public static int size() {
		if( locations==null )return 0;
		return locations.length;
	}
}

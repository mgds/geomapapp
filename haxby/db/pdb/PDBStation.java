package haxby.db.pdb;

import haxby.map.MapApp;
import haxby.util.NumberFormatUtil;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

public class PDBStation {
	public static PDBStation[] stations = null;
	String id;
	boolean suffix;
	int expedition;
	int location;
	long[] samples;
	byte sampleTechnique;
	byte materialFlags;
	String materialCodes;
	short itemsMeasured;
	byte alterationFlags;
	long rockTypes;
	static boolean loaded = false;
	private static JDialog dialog = null;
	public static HashMap<String, PDBStation> idToStation = new HashMap<String, PDBStation>();

	static String PETDB_PATH = PathUtil.getPath("PORTALS/PETDB_PATH",
			MapApp.BASE_URL+"/data/portals/petdb/");

	public PDBStation(String id,
			long[] samples,
			int expedition,
			int location,
			byte sampleTechnique,
			byte materialFlags,
			short itemsMeasured,
			byte alterationFlags,
			long rockTypes ) {
		if(id.startsWith("@")) {
			this.id = id.substring(1,id.length());
			suffix = true;
		} else {
			this.id = id;
			suffix = false;
		}
		this.samples = samples;
//		this.expedition = expedition;  // not used?
		this.location = location;
//		this.sampleTechnique = sampleTechnique; // not used?
		this.materialFlags = materialFlags;
		this.itemsMeasured = itemsMeasured;
//		this.alterationFlags = alterationFlags; // not used?
		this.rockTypes = rockTypes;
	}
	public String getID() {
		if(suffix) {
			if(PDBExpedition.get((int)expedition)==null) return "N/A";
			return PDBExpedition.get((int)expedition).getName()+id;
		} else return id;
	}
	public int getExpeditionIndex() {
		return (int)expedition;
	}
	public PDBExpedition getExpedition() {
		if( expedition<0 || expedition>=PDBExpedition.size() )return null;
		return PDBExpedition.get( (int)expedition );
	}
	public int howManySamples() {
		if(samples==null)return 0;
		return samples.length;
	}
	public int[] getSampleNums() {
		int[] tmp = new int[howManySamples()];
		for( int i=0 ; i<tmp.length ; i++) {
			tmp[i] = (int)samples[i];
		}
		return tmp;
	}
	public int getTechniqueIndex() {
		return (int)sampleTechnique;
	}
	public int getLocationIndex() {
		return (int)location;
	}
	public PDBLocation getLocation() {
		return PDBLocation.get( (int)location);
	}
	public float getLongitude() {
		return PDBLocation.get( (int)location).lon;
	}
	public float getLatitude() {
		return PDBLocation.get( (int)location).lat;
	}
	public double getX() {
		return PDBLocation.get( (int)location).getX();
	}
	public double getY() {
		return PDBLocation.get( (int)location).getY();
	}
	public Point2D getMapXY() {
		return PDBLocation.get( (int)location).getMapXY();
	}
	public boolean hasMaterials( int materials ) {
		return ((int)materialFlags & materials)!=0;
	}
	public int getMaterials() {
		return (int)materialFlags;
	}
	public boolean hasDataType( int type ) {
		return ((int)itemsMeasured & type)!=0;
	}
	public boolean hasRockType( long type ) {
		return (rockTypes & type)!=0L;
	}
//  NOT USED?	
//	public boolean hasAlterations( int alterations ) {
//		return ((int)alterationFlags & alterations)!=0;
//	}
	static void init( int n ) {
		stations = new PDBStation[n];
	}
	static void add( PDBStation station, int index ) {
		if(stations == null) {
			init( index );
		} else if(stations.length <= index ) {
			PDBStation[] tmp = new PDBStation[index+100];
			System.arraycopy( stations, 0, tmp, 0, stations.length);
			stations = tmp;
		}
		stations[index] = station;
		if(null != station) {
			idToStation.put(station.id, station);
		}
	}
	static int trimToSize() {
		int k;
		for( k=stations.length-1 ; k>=0 ; k-- ) {
			if( stations[k] != null ) break;
		}
		if(k==stations.length-1) return size();
		PDBStation[] tmp = new PDBStation[k+1];
		if(k>=0) {
			System.arraycopy( stations, 0, tmp, 0, k+1 );
		}
		stations = tmp;
		return size();
	}
	
	static void setProgressDialog(JDialog dlg) {
		dialog = dlg;
	}
	
	static void unload() {
		if (stations != null) {
			for (int i = 0; i < stations.length; i++)
				stations[i] = null;
			stations = null;
		}
		loaded = false;
	}

	public static void load() throws IOException {
		if(loaded) return;

		URL url = URLFactory.url(PETDB_PATH + "petdb_latest/pdb_stations_new.tsv");
		//URL url = URLFactory.url(PETDB_PATH + "June2014/stations_new.txt");
		URLConnection urlConn = url.openConnection();
		urlConn.setDoInput(true);
		urlConn.setUseCaches(false);
		BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

		// Read Data File
		int index=0;
		String id, s;
		long[] samples;
		int expedition;
		int location;
		byte sampleTechnique;
		byte materialFlags;
		short itemsMeasured;
		byte alterationFlags;
		long rockTypes;
		
		//first read the header
		s = in.readLine();
		
		int count = 0;
		//then read the data
		SortedMap<Integer, PDBStation> stations_map = new TreeMap<>();
		while ((s = in.readLine())!= null){
			String [] results = s.split("\\t");
			if (results.length != 11) continue;
			index = Integer.parseInt(results[0]);
			id = results[1];
			expedition =  results[2].length() > 0 ? Integer.parseInt(results[2]) : 0;
			location = Integer.parseInt(results[3]);
			count++;
			if(null != dialog) {
				final int tnuoc = count;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						dialog.setTitle("Processing the " + NumberFormatUtil.cardinalToOrdinal(tnuoc) + " station");
					}
				});
			}
			if(PDBLocation.locations[location] == null) {
				stations_map.put(index, null);
			}
			else {
				samples = new long[Short.parseShort(results[4])];
				String [] sampleItems = results[5].split(",");
				for( int i=0 ; i<samples.length ; i++) {
					samples[i] = Long.parseLong(sampleItems[i]);
				}
				try {
					sampleTechnique = Byte.parseByte(results[6]);
				} catch(Exception e) {
					sampleTechnique = 0;
				}
				try {
					materialFlags = Byte.parseByte(results[7]);
				} catch(Exception e) {
					continue;
				}
				itemsMeasured = Short.parseShort(results[8]);
				alterationFlags = Byte.parseByte(results[9]);
				rockTypes = Long.parseLong(results[10]);
				stations_map.put(index, new PDBStation(id, samples, expedition, location,
											sampleTechnique, materialFlags, itemsMeasured,
											alterationFlags, rockTypes));
			}
			/*
			add( new PDBStation( id, samples, expedition, location,
							sampleTechnique, materialFlags, itemsMeasured,
							alterationFlags, rockTypes ), index);
			if( PDBLocation.locations[location]==null) {
				stations[index]=null;
			}
			*/
		}
		init(stations_map.size());
		for(Integer i : stations_map.keySet()) {
			add(stations_map.get(i), i);
		}

		try {
			in.close();
		} catch ( IOException ex ) {}
		loaded = true;
	}
	public static boolean isLoaded() {
		return loaded;
	}
	public static PDBStation get( int index ) {
		
		if( index<0 || index>=size() ) {
			System.err.println("PDBStation:get(index)="+index+" size="+size() );
			return null;
		}
		return stations[index];
	}
	public static int size () {
		if( stations==null )return 0;
		return stations.length;
	}
}
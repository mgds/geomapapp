package haxby.db.pdb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import haxby.map.MapApp;
import haxby.util.NumberFormatUtil;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

public class PDBSample {
	public static Map<Integer,PDBSample> sample;
	int parent;
	byte[] id;
	boolean suffix;
	PDBBatch[] batch;
	long rockType;
	int specimenNumber;
	private static JDialog dialog = null;
	public static HashMap<String, PDBSample> idToSample = new HashMap<String, PDBSample>();

	static String PETDB_PATH = PathUtil.getPath("PORTALS/PETDB_PATH",
			MapApp.BASE_URL+"/data/portals/petdb/");

	public PDBSample( int station, String ID, long rockType, int snum) {
		parent = station;
		if( ID==null ) ID="";
		if(ID.startsWith("@")) {
			id = ID.substring(1).getBytes();
			suffix = true;
		} else {
			id = ID.getBytes();
			suffix = false;
		}
		batch = new PDBBatch[0];
		this.rockType = rockType;
		this.specimenNumber = snum;
		idToSample.put(ID, this);
	}
	public int getStationNum() {
		return (int)parent;
	}
	public void addBatch( PDBBatch b ) {
		PDBBatch[] tmp = new PDBBatch[batch.length+1];
		for(int i=0 ; i<batch.length ; i++) {
			tmp[i] = batch[i];
		}
		tmp[batch.length] = b;
		batch = tmp;
	}
	public String getName() {
		if(suffix) return PDBStation.get((int)parent).getID() + new String(id);
		else return new String(id);
	}
	public String getName(PDBBatch b) {
		for(int i=0 ; i<batch.length ; i++) {
			if( batch[i]==b ) {
				return getName() +":"+ i;
			}
		}
		return getName() +":";
	}
	public boolean hasRockType(long type) {
		return ( rockType & type )!= 0L;
	}
	static void unload() {
		if (sample == null) return;
		sample.clear();
		sample = null;
	}
	private void dispose() {
		for (int i = 0; i < batch.length; i++) {
			batch[i].dispose();
			batch[i] = null;
		}
		batch = null;
	}
	
	static void setProgressDialog(JDialog dlg) {
		dialog = dlg;
	}
	
	public static void load() throws IOException {
	//	URL url = URLFactory.url(PETDB_PATH + "June2014/pdb_dataC_new.txt");
		URL url = URLFactory.url(PETDB_PATH + "petdb_latest/pdb_dataC_new.txt");
		URLConnection urlConn = url.openConnection();
		urlConn.setDoInput(true); 
		urlConn.setUseCaches(false);

		BufferedReader txtReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

		sample = new HashMap<Integer, PDBSample>(); 
		int snum = -1;

			String oneLine;
			int maxref=0;
			int maxmaterialshift=0;
			
		int sampleCount = 0;
		while( true ) {
			try{
			oneLine = txtReader.readLine();
			if(oneLine==null) break;
			if(oneLine.startsWith("/*")==true) continue; /* skip first line. The data starts with second line. */
			sampleCount++;
			if(null != dialog) {
				final int sc = sampleCount;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						dialog.setTitle("Loading the " + NumberFormatUtil.cardinalToOrdinal(sc) + " sample…");
					}
				});
			}
			String [] substrings = oneLine.split("\\t");
			int index=0; /* substrings index */		
			snum = Integer.parseInt(substrings[index++]); 

			int station = Integer.parseInt(substrings[index++]);

			String id = substrings[index++];

			long rock = Long.parseLong(substrings[index++]);
//System.out.println(station + " " + id + " " + rock + " snum " + snum);
			PDBSample samp = new PDBSample(station, id, rock, snum);
			sample.put(snum, samp);
			
			int nb = Short.parseShort(substrings[index++]);
			for( int i=0 ; i<nb ; i++) {

				int ref = Short.parseShort(substrings[index++]);
				if (ref > maxref) maxref=ref;
				int materialShift = Integer.parseInt(substrings[index++]);

				if(materialShift > maxmaterialshift ) maxmaterialshift = materialShift; 

				int material = 1<< materialShift;

			//	int mineral = Integer.parseInt(substrings[index++]);  Lulin Song: Removed in the new format

			//	PDBBatch b = new PDBBatch( samp, ref, material, mineral);
				//LS: It seems the mineral read but never used. So removed from PDBBatch to see if it will break anything
				PDBBatch b = new PDBBatch( samp, ref, material);
				
				int na = Integer.parseInt(substrings[index++]);
				for( int j=0 ; j<na ; j++) {

					int at = Integer.parseInt(substrings[index++]); //not used
					int nc = Integer.parseInt(substrings[index++]);

					float[] val = new float[nc];
					float[] stdDev = new float[nc]; // not used
					short[] code = new short[nc];
					boolean[] compiled = new boolean[nc];
					boolean hasSD = false;
					for( int k=0 ; k<nc ; k++) {
						/* The item_codes are stored in an array whose index starting with 0, but this column represents row number in item_codeA_new.txt file. The row number starts with 1 */
						code[k]=(short)((Short.parseShort(substrings[index++])) - 1); 
						val[k] = Float.parseFloat(substrings[index++]);

						String stdDevStr = substrings[index++];
						if(stdDevStr.equalsIgnoreCase("null") == false)
						{
							stdDev[k] = Float.parseFloat(stdDevStr);
							hasSD=true;
						}

						compiled[k]=Boolean.parseBoolean(substrings[index++]);
					}
					if(!hasSD) stdDev=null;
					
					PDBAnalysis a = new PDBAnalysis(b, code, val, compiled);
					b.addAnalysis(a);
				}


				if( PDBStation.stations[station]!=null) samp.addBatch(b);


			}
				if( PDBStation.stations[station]==null) sample.remove(snum);

			} catch (Exception e){
				//Catch exception if any
			//	System.err.println("Sample Reading Error: snum:" + e.getMessage());
				continue;
			}
			
		}
		try {
			txtReader.close();
		} catch(IOException ex) {
		}
	}
}
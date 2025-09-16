package org.geomapapp.db.dsdp;

import haxby.util.URLFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.Vector;

public class Ages {
	static byte[][] names;
	static float[][] minmax;
	boolean[] hasAge;
	public Ages() {
		init();
		hasAge = new boolean[names.length];
		for(int k=0 ; k<names.length ; k++) hasAge[k]=false;
	}
	public void setHasAge(int k) {
		hasAge[k]=true;
	}
	public int[] getAgeIndices() {
		int count=0;
		for(int k=0 ; k<names.length ; k++) if(hasAge[k])count++;
		int[] indices = new int[count];
		count=0;
		for( int k=0 ; k<names.length ; k++) if(hasAge[k]) indices[count++]=k;
		return indices;
	}
	static String getAgeName( int age ) {
		if( names==null )init();
		return new String( names[age] );
	}
	static float[] getAgeRange( int age ) {
		if( names==null )init();
		return new float[] {minmax[age][0], minmax[age][1] };
	}
	static void init() {
		if( names!=null )return;
		Vector ids = new Vector();
		Vector ranges = new Vector();
		try {
			URL url = URLFactory.url(DSDP.ROOT + "ageMapA.tsf.gz");
			BufferedReader in = new BufferedReader(
				new InputStreamReader( 
				new java.util.zip.GZIPInputStream(url.openStream()) ));
			in.readLine();
			String s;
			StringTokenizer st;
			while( (s=in.readLine())!=null ) {
				st = new StringTokenizer(s,"\t");
				ids.add(st.nextToken());
				ranges.add( new float[] {
					Float.parseFloat(st.nextToken()),
					Float.parseFloat(st.nextToken())
					});
			}
			in.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
		names = new byte[ids.size()][];
		minmax = new float[ids.size()][2];
		for( int k=0 ; k<names.length ; k++) {
			names[k] = ids.get(k).toString().getBytes();
			minmax[k] = (float[])ranges.get(k);
		}
	}
}

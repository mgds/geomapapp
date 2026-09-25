package org.geomapapp.db.dsdp;

import java.util.StringTokenizer;

public class FossilEntry {
	public float depth;
	public byte thickness;
	short[] picks;
	public byte ref;
	public FossilEntry( float depth, 
			int thickness, 
			int[] codes, 
			int[] abundances,
			int ref) {
		this.depth = depth;
		this.thickness = (byte)thickness;
		for( int i=0 ; i<codes.length ; i++) {
			picks[i] = (short)( (codes[i]<<3) | (7&abundances[i]) );
		}
		this.ref = (byte)ref;
	}
	public FossilEntry( String line, java.util.Vector refs ) {
		StringTokenizer st = new StringTokenizer(line, "\t", true);
		String s = "";
		int count = 0;
		ref = (byte)(refs.size()-1);
		while( st.hasMoreTokens() ) {
			s = st.nextToken();
			count++;
			if( s.equals("\t"))continue;
			if( count==2 ) depth = Float.parseFloat(s);
			if( count==3 ) {
				int thick = Integer.parseInt(s);
				depth -= thickness*.5f;
				thickness = (byte)thick;
			}
			if( count==4 ) {
				int i = refs.indexOf(s);
				if( i<0 ) {
					ref = (byte)refs.size();
					refs.add(s);
				} else {
					ref = (byte)i;
				}
			}
			if( st.hasMoreTokens() )s=st.nextToken();
		}
		st = new StringTokenizer(s, ";");
		picks = new short[st.countTokens()];
		for( int k=0 ; k<picks.length ; k++) {
			StringTokenizer st1 = new StringTokenizer(st.nextToken(), ",");
			int pick = Integer.parseInt(st1.nextToken())<<3;
			if( !st1.hasMoreTokens() ) pick |= 7;
			else pick |= Integer.parseInt(st1.nextToken());
			picks[k] = (short)pick;
		}
	}
	public boolean hasCode(int code) {
		for( int i=0 ; i<picks.length ; i++) {
			if( code==(0x0000ffff&(int)picks[i]>>3) )return true;
		}
		return false;
	}
	public int abundanceForCode(int code) {
		for( int i=0 ; i<picks.length ; i++) {
			if( code==(0x0000ffff&(int)picks[i]>>3) ) {
				int a = picks[i]&7;
				if( a==7 ) a=-1;
				return a;
			}
		}
		return -2;	// code not in this entry
	}
	public int[] getCodes() {
		int[] codes = new int[picks.length];
		for( int i=0 ; i<picks.length ; i++) {
			codes[i] = 0x0000ffff&(int)(picks[i]>>3); 
		}
		return codes;
	}
	public int[] getAbundances() {
		int[] codes = new int[picks.length];
		for( int i=0 ; i<picks.length ; i++) {
			codes[i] = picks[i]&7;
			if( codes[i]==7 )codes[i]=-1;
		}
		return codes;
	}
}

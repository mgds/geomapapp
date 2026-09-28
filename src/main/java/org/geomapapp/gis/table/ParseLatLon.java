package org.geomapapp.gis.table;

import java.util.StringTokenizer;
import java.io.*;

public class ParseLatLon {
	public static double parse(String s) {
		s = s.trim();
		try {
			double d = Double.parseDouble(s);
			return d;
		} catch(Exception e) {
			try {
			StringTokenizer st;
			s = s.toLowerCase();
			byte[] chars = s.getBytes();
			double sign = 1.;
			String delim = "news";
			String neg = "ws";
			String delim2 = "\'\"`";
			for(int k=0 ; k<chars.length ; k++) {
				if( chars[k]<0 ) {
					chars[k] = (byte)' ';
				} else if( delim.indexOf( chars[k] )>=0 ) {
					if( neg.indexOf( chars[k] )>=0 ) sign=-1.;
					chars[k] = (byte)' ';
				} else if( delim2.indexOf( chars[k] )>=0 ) {
					chars[k] = (byte)' ';
				}
			}
			s = new String(chars);
			st = new StringTokenizer(s);
			double deg = Double.NaN;
			if( st.countTokens()==3 ) {
				deg = Double.parseDouble(st.nextToken()) 
					+ Double.parseDouble(st.nextToken())/60.
					+ Double.parseDouble(st.nextToken())/60./60.;
			} else if( st.countTokens()==2 ) {
				deg = Double.parseDouble(st.nextToken()) 
					+ Double.parseDouble(st.nextToken())/60.;
			} else {
				deg = Double.parseDouble(st.nextToken());
			}
			return deg*sign;
			} catch(Exception ex) {
			}
		}
		return Double.NaN;
	}
	public static void main(String[] args) {
		try {
			BufferedReader in = new BufferedReader(
				new InputStreamReader(System.in));
			System.out.println(parse(in.readLine()));
		} catch (IOException e) {
		}
	}
}

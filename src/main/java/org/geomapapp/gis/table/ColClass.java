package org.geomapapp.gis.table;

import java.awt.Color;
import java.io.*;
import java.util.*;

public class ColClass {
	static Class[] all = new Class[] { 
			Color.class,
			Boolean.class,
			Byte.class,
			Short.class,
			Integer.class,
			Double.class,
			String.class };
	static String[] bools = new String[] {
				"t", "f", "true", "false",
				"0", "1", "y", "n", "yes", "no" };
			//	"y", "n", "yes", "no" };
	static String[] trues = new String[] {
				"t", "true", "1", "y", "yes" };
	static boolean isTrue(String s) {
		if( s==null ) return false;
		for( int k=0 ; k<trues.length ; k++) {
			if( s.equalsIgnoreCase(trues[k]) )return true;
		}
		return false;
	}
	static boolean boolTest(String s) {
		for( int k=0 ; k<bools.length ; k++) {
			if( s.equalsIgnoreCase(bools[k]) )return true;
		}
		return false;
	}
	static boolean colorTest(String s) {
		StringTokenizer st = new StringTokenizer(s, ",");
		if( st.countTokens()!=3 )return false;
		for( int k=0 ; k<3 ; k++) {
			try {
				int i = Integer.parseInt(st.nextToken());
				if( i<0 || i>255 )return false;
			} catch(Exception e) {
				return false;
			}
		}
		return true;
	}
	public static Class[] getColumnClasses( Vector data, int nCol ) {
		Class[] classes = new Class[nCol];
		boolean[] isNull = new boolean[nCol];
		int k;
		Vector[] types = new Vector[nCol];
		for(k=0 ; k<classes.length ; k++) {
			isNull[k] = true;
			types[k] = new Vector(all.length);
			for( int i=0 ; i<all.length ; i++)types[k].add(all[i]);
		}
		for( k=0 ; k<data.size() ; k++) {
			Vector row  = (Vector)data.get(k);
			int n = (int)Math.min( row.size(), classes.length);
			for( int i=0 ; i<n ; i++) {
				if( row.get(i)==null)continue;
				if( types[i].size()==1 )continue;
				String s = row.get(i).toString();
				if( s.trim().length()==0 )continue;
				isNull[i] = false;
				if( !colorTest(s) ) types[i].remove(Color.class);
				if( !boolTest(s) )types[i].remove(Boolean.class);
				try {
					double d = Double.parseDouble(s);
					if( d!=Math.rint(d) ) {
						types[i].remove(Byte.class);
						types[i].remove(Short.class);
						types[i].remove(Integer.class);
					} else {
						if( Math.abs(d)>127. )types[i].remove(Byte.class);
						if( Math.abs(d)>32767. )types[i].remove(Short.class);
						if( Math.abs(d)>(double)0x7fffffff )types[i].remove(Integer.class);
					}
				} catch(Exception e) {
					types[i].remove(Byte.class);
					types[i].remove(Short.class);
					types[i].remove(Integer.class);
					types[i].remove(Double.class);
				}
			}
		}
		for( k=0 ; k<nCol ; k++) {
			if( isNull[k] ) classes[k] = String.class;
			else classes[k] = (Class)types[k].get(0);
		}
		return classes;
	}
/*
	public static void main(String[] args) {
		if( args.length!=1 ) {
			System.out.println("usage: java ColClasses file");
			System.exit(0);
		}
		try {
			File file = new File(args[0]);
			BufferedReader in = new BufferedReader(
					new FileReader(file));
			String s = in.readLine();
			StringTokenizer st = new StringTokenizer(s,"\t",true);
			Vector names = new Vector();
			int k=0;
			while( st.hasMoreTokens() ) {
				s = st.nextToken();
				if( s.equals("\t") ) {
					names.add("");
				} else {
					names.add(s);
					if( st.hasMoreTokens() )st.nextToken();
					k++;
			//		System.out.println( k+"\t"+ s);
				}
			}
			Class[] classes = new Class[names.size()];
			for(k=0 ; k<classes.length ; k++) classes[k] = null;
			while( (s=in.readLine())!=null ) {
				st = new StringTokenizer(s,"\t",true);
				Vector t = new Vector();
				k=0;
				while( st.hasMoreTokens() ) {
					s = st.nextToken();
					k++;
					if( s.equals("\t") ) {
						t.add("");
						continue;
					} else {
						t.add(s);
						if( st.hasMoreTokens())st.nextToken();
						if( classes[k-1]==String.class )continue;
						if( classes[k-1]==Boolean.class ) {
							if( !boolTest(s) ) {
								classes[k-1]=String.class;
							}
							continue;
						}
						try {
							double d = Double.parseDouble(s);
							if( classes[k-1]==Double.class )continue;
							if( d==Math.rint(d) ) classes[k-1]=Integer.class;
							else classes[k-1] = Double.class;
						} catch(Exception e) {
							if( boolTest(s) ) {
								classes[k-1]=Boolean.class;
							} else {
								classes[k-1]=String.class;
							}
						}
					}
				}
			}
			for( int i=0 ; i<classes.length ; i++) {
				System.out.println( i +"\t"+ names.get(i) +"\t"+ classes[i]);
			}
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
*/
}

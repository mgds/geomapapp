package org.geomapapp.gis.table;

import java.io.*;
import java.util.*;

public class Tags {
	public static Vector uniqueTags( Vector rows, int col, int maxTags ) {
		Vector tags = maxTags>0 
			? new Vector(maxTags)
			: new Vector();
		for( int k=0 ; k<rows.size() ; k++) {
			Vector row = (Vector)rows.get(k);
			if( row.size()<=col)continue;
			if( row.get(col)==null )continue;
			String t = row.get(col).toString();
			if( maxTags>0 && tags.size()>=maxTags)break;
			if( !tags.contains(t) )tags.add(t);
		}
		return tags;
	}
	public static void main(String[] args) {
		if( args.length<3 ) {
			System.out.println("usage: java Tags file maxTags col [col col ..]");
			System.exit(0);
		}
		try {
			File file = new File(args[0]);
			BufferedReader in = new BufferedReader(
					new FileReader(file));
			int maxTags = Integer.parseInt(args[1]);
			int[] cols = new int[args.length-2];
			for(int k=0 ; k<cols.length ; k++) cols[k]=Integer.parseInt(args[k+2]);
			Vector[] tags = new Vector[args.length-2];
			for(int i=0 ; i<tags.length ; i++) tags[i]=new Vector(maxTags);
			String s = in.readLine();
			StringTokenizer st = new StringTokenizer(s,"\t",true);
			Vector names = new Vector();
			int k=0;
			while( st.hasMoreTokens() ) {
					s = st.nextToken();
				if( s.equals("\t") ) names.add("");
				else {
					names.add(s);
					if( st.hasMoreTokens() )st.nextToken();
					k++;
			//		System.out.println( k+"\t"+ s);
				}
			}
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
					}
				}
				for( int i=0 ; i<cols.length ; i++) {
					if( tags[i].size()>maxTags )continue;
					if( t.size()<=cols[i] ) continue;
					String test = (String)t.get(cols[i]);
					if( !tags[i].contains(test) ) tags[i].add(test);
				}
			}
			for( int i=0 ; i<cols.length ; i++) {
				System.out.println( cols[i] +"\t"+ names.get(cols[i]) +"\t"+ tags[i].size());
				for( k=0 ; k<tags[i].size() ; k++) {
					System.out.println("\t"+tags[i].get(k));
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}

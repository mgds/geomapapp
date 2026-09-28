package org.geomapapp.gis.table;

import java.io.*;
import java.util.*;

public class SpaceToTab {
	public static void main(String[] args) {
		try {
			BufferedReader in = new BufferedReader(
				new InputStreamReader(System.in));
			String s;
			while( (s=in.readLine())!=null ) {
				if( s.trim().length()==0 )continue;
				StringTokenizer st = new StringTokenizer(s);
				StringBuffer sb= new StringBuffer(st.nextToken());
				while( st.hasMoreTokens() )sb.append("\t"+st.nextToken());
				System.out.println( sb );
			}
		}catch(IOException ex) {
			ex.printStackTrace(System.err);
		}
	}
}

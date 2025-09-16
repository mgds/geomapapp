package org.geomapapp.db.dsdp;

import haxby.util.URLFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Hashtable;
import java.util.StringTokenizer;

public class BRGAcronyms {
	Hashtable lookup;
	public BRGAcronyms() {
		try {
			lookup = new Hashtable();
			URL url = URLFactory.url(DSDP.ROOT +"acronyms.txt");
			BufferedReader in = new BufferedReader(
				new InputStreamReader(url.openStream()));
			String s;
			StringTokenizer st;
			StringBuffer sb;
			while( (s=in.readLine())!=null ) {
				st = new StringTokenizer(s);
				String key = st.nextToken();
				sb = new StringBuffer(st.nextToken());
				while( st.hasMoreTokens() ) sb.append(" "+st.nextToken());
				lookup.put( key, sb.toString() );
			}
			in.close();
		} catch(Exception e) {
			e.printStackTrace( System.err );
		}
	}
	public String getDescription(String key) {
		if(lookup==null|| lookup.size()==0)return "Unknown Acronym";
		Object val = lookup.get(key);
		if( val==null )return "Unknown Acronym";
		return (String)val;
	}
}

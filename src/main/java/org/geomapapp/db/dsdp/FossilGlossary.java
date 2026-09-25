package org.geomapapp.db.dsdp;

import haxby.util.URLFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

public class FossilGlossary {
	static Vector glossary;
	public static String get( int index ) {
		if( glossary==null )init();
		return new String((byte[])glossary.get(index));
	}
	public static void dispose() {
		glossary = null;
	}
	static void init() {
		try {
			glossary = new Vector();
			URL url = URLFactory.url( DSDP.ROOT+"fauna/glossary.gz");
			BufferedReader in = new BufferedReader(
				new InputStreamReader( 
				new GZIPInputStream( url.openStream() )));
			String s;
			while( (s=in.readLine())!=null ) glossary.add(s.getBytes());
			glossary.trimToSize();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}

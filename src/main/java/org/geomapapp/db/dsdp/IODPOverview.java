package org.geomapapp.db.dsdp;

import haxby.util.URLFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Vector;

public class IODPOverview {
	Vector headings;
	Vector rows;
	String dir = "general";
	public IODPOverview() throws IOException {
		URL url = URLFactory.url(Janus.BASE+dir+"/dbtable.cgi");
		BufferedReader in = new BufferedReader(
			new InputStreamReader( url.openStream() ));
		String s = in.readLine();
		while( !(s=in.readLine()).startsWith("<table") );
		headings = parseRow(in);
	//	System.out.println( headings.size() );
		rows = new Vector();
		while(true) {
			try {
				Vector row = parseRow(in);
				if( row==null )break;
				rows.add( row );
	//			System.out.println( rows.size() +"\t"+ row.size() );
			} catch( NullPointerException e) {
				break;
			}
		}
		for( int k=0 ; k<rows.size() ; k++) {
			Vector row = (Vector)rows.get(k);
			String[] entry = (String[])row.get(0);
			String line = "\tpublic static final int NAME = \t// "+ entry[0];
			System.out.println( line );
		}
		System.out.println( "\tpublic static String[] description = new String[] {");
		for( int k=0 ; k<rows.size() ; k++) {
			Vector row = (Vector)rows.get(k);
			String[] entry = (String[])row.get(0);
			String line = "\t\t\""+ entry[0] +"\"";
			if( k!=rows.size()-1 ) line += ",";
			System.out.println( line );
		}
		System.out.println( "\t}");

		System.out.println( "\tstatic String[] cgi = new String[] {");
		for( int k=0 ; k<rows.size() ; k++) {
			Vector row = (Vector)rows.get(k);
			String[] entry = (String[])row.get(0);
			String urlString = entry[1].substring(3);
			int i = urlString.indexOf("shtml");
			String cgi = urlString.substring(0, i)+"cgi";
			if( !urlString.endsWith("shtml") ) cgi += urlString.substring(urlString.lastIndexOf("?"));
			String line = "\t\t\""+ cgi +"?\"";
			if( k!=rows.size()-1 ) line += ",";
			System.out.println( line );
		}
		System.out.println( "\t}");
	}
	public Vector parseRow(BufferedReader in) throws IOException {
		String s;
		while( !(s=in.readLine()).startsWith("<tr") ) {
			if( s.startsWith("</table>") ) return null;
		}
		Vector row = new Vector();
		while( !(s=in.readLine()).startsWith("</tr") ) {
			if( !s.startsWith("<td") )continue;
			row.add(parseTD(s));
		}
		return row;
	}
	public String[] parseTD(String s) {
		String[] entry = new String[2];
		int k=s.indexOf("<td");
		if( k<0 )return null;
		while(true) {
			if( s.startsWith("</td>") ) {
				break;
			} else if( s.startsWith("<a href=") ) {
				entry[1] = s.substring(s.indexOf("\"")+1, s.indexOf(">")-1 );
				s = s.substring(s.indexOf(">")+1);
			} else if(s.startsWith("<")) {
				s = s.substring(s.indexOf(">")+1);
			} else {
				k = s.indexOf("<");
				entry[0] = s.substring(0, k);
				s = s.substring(k);
			}
		}
	//	System.out.println( entry[0] +"\t"+ entry[1] );
		return entry;
	}
	public static void main(String[] args) {
		try {
			new IODPOverview();
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.exit(0);
	}
}

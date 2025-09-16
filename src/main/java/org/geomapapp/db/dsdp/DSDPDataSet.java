package org.geomapapp.db.dsdp;

import haxby.util.URLFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

public class DSDPDataSet {
	DSDP dsdp;
	DataEntry entry;
	Vector data;
	public DSDPDataSet(DSDP dsdp, DataEntry entry) throws IOException {
		this.dsdp = dsdp;
		this.entry = entry;
		getData();
	}
	void getData() throws IOException {
		URL url = URLFactory.url( entry.getURL() );
		InputStream input = url.openStream();
		if( entry.getURL().endsWith(".gz") ) input = new GZIPInputStream(input);
		BufferedReader in = new BufferedReader(
			new InputStreamReader(input));
		String s = in.readLine();
		String[] headings = parseRow(s, "\t");
		String id = "";
		int source = -1;
		Vector data = new Vector();
		while( (s=in.readLine()) != null ) {
			String[] row = parseRow(s, "\t");
			if( row[0].length()>0 ) {
				id = row[0];
				if( data.size()>0 ) {
					setData( data );
					data = new Vector();
				}
			}
			data.add(row);
		}
		in.close();
		if( data.size()>0 ) setData( data );
	}
	void setData( Vector d ) {
		if( data==null) data = new Vector();
		float[] depth = new float[d.size()];
		float[] val = new float[d.size()];
		String[] row = (String[])d.get(0);
		DSDPHole hole = dsdp.holeForID(row[0]);
		for( int k=0 ; k<val.length ; k++) {
			row = (String[])d.get(k);
			depth[k] = Float.parseFloat( row[1] );
			val[k] = Float.parseFloat( row[4] );
		}
		DSDPData dd = new DSDPData( depth, val, hole, this);
		data.add( dd );
		hole.addData(dd);
	}
	public String getName() {
		return entry.toString();
	}
	public String toString() {
		return entry.toString();
	}
	public void dispose() {
		if( data==null )return;
		for( int k=0 ; k<data.size() ; k++) {
			DSDPData dd = (DSDPData)data.get(k);
			dd.hole.removeData( dd );
		}
		data = null;
	}
	public static String[] parseRow( String line, String token ) {
		StringTokenizer st = new StringTokenizer(line,token, true);
		String s;
		Vector fields = new Vector();
		while( st.hasMoreTokens() ) {
			s = st.nextToken();
			if( s.equals(token) ) {
				fields.add("");
				continue;
			}
			s = s.trim();
			if( s.startsWith("\"")&&s.endsWith("\"") ) {
				if( s.length()<=2) s="";
				else s = s.substring(1, s.length()-1);
			} else if( s.startsWith("-99") || s.equals("-") ) {
				s = "";
			}
			fields.add(s);
			if( st.hasMoreTokens() )st.nextToken();
		}
		String[] vals = new String[fields.size()];
		for( int k=0 ; k<vals.length ; k++) vals[k]=(String)fields.get(k);
		return vals;
	}
}

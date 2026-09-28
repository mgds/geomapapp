package org.geomapapp.db.dsdp;

import haxby.util.URLFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

public class DSDPAgeDepth {
	DSDP dsdp;
	public DSDPAgeDepth(DSDP dsdp, String url) throws IOException {
		this.dsdp = dsdp;
		getData(url);
	}
	void getData(String urlString) throws IOException {
		URL url = URLFactory.url(urlString);
		InputStream input = url.openStream();
		if( urlString.endsWith(".gz") ) input = new GZIPInputStream(input);
		BufferedReader in = new BufferedReader(
			new InputStreamReader(input));
		String s = in.readLine();
		String[] headings = DSDPDataSet.parseRow(s, "\t");
		String id = "";
		int source = -1;
		Vector data = new Vector();
		while( (s=in.readLine()) != null ) {
			String[] row = DSDPDataSet.parseRow(s, "\t");
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
		Vector ageDepth = new Vector(d.size());
		Vector sources = new Vector(d.size());
		String[] row = (String[])d.get(0);
		DSDPHole hole = dsdp.holeForID(row[0]);
		for( int k=0 ; k<d.size() ; k++) {
			row = (String[])d.get(k);
			ageDepth.add( new float[] {Float.parseFloat( row[1] ), Float.parseFloat( row[2] )} );
			if ( row.length > 3 ) {
				sources.add(row[3]);
			}
		}
	//	System.out.println( hole.toString() +"\t"+ ageDepth.size());
		hole.addAgeModel(ageDepth);
		hole.addSources(sources);
	}
	public String getName() {
		return "age";
	}
}

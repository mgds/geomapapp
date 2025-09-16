package org.geomapapp.db.dsdp;

import java.util.Vector;
import java.util.StringTokenizer;

import java.io.*;

public class FossilGroup {
	Vector codes;
	String groupName;
	public FossilGroup(String groupName, Vector codes) {
		this.groupName = groupName;
		this.codes = codes;
	}
	public FossilGroup(String groupName, InputStream input, boolean hasHeader) throws IOException {
		this(groupName, new Vector());
		BufferedReader in = new BufferedReader(
			new InputStreamReader(input));
		String s;
		if( hasHeader )in.readLine();
		while( (s=in.readLine())!=null) {
			StringTokenizer st = new StringTokenizer(s,",");
			short[] entry = new short[st.countTokens()];
			for( int i=0 ; i<entry.length ; i++) {
				entry[i] = (short)Integer.parseInt(st.nextToken());
			}
			codes.add(entry);
		}
		codes.trimToSize();
		in.close();
	}
	public void dispose() {
		codes = new Vector();
	}
	public String getGroupName() {
		return groupName;
	}
	public String toString() {
		return groupName;
	}
	public String getFossilName(int code) {
		return getFossilName( (short)code );
	}
	public String getFossilName(short code) {
		if( code<0 || code>=codes.size() )return "";
		short[] indices = (short[])codes.get(code);
		if( indices==null || indices.length==0 )return "";
		StringBuffer sb = new StringBuffer();
		sb.append( FossilGlossary.get( 0x0000ffff & (int)indices[0] ) );
		for( int k=1 ; k<indices.length ; k++) {
			sb.append( " "+FossilGlossary.get( 0x0000ffff & (int)indices[k] ) );
		}
		return sb.toString();
	}
}

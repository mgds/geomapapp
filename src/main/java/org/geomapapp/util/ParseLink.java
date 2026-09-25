package org.geomapapp.util;

import haxby.util.URLFactory;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.Vector;

public class ParseLink {
	public static Vector parse(URL url) throws IOException {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(url.openStream())); 
			return parse(reader, null);
		} finally {
			if (reader!=null) reader.close();
		}
	}
	public static Vector parse(BufferedReader in, Vector properties) throws IOException {
		boolean top = properties==null;
		if( top )properties = new Vector();
		int i=-1;
		while( true ) {
			try {
				i=nextChar(in);
			} catch(EOFException e) {
				if(!top) throw e;
				return properties;
			} 
			if((char)i=='<') addProperty(in, properties, -1);
		}
	}
	static void addProperty( BufferedReader in, Vector properties, int first) throws IOException {
		Vector props = new Vector();
		char c;
		int i = (first==-1) ? nextChar(in) : first;
		c = (char)i;
		if( c=='!' || c=='?' ) {
			parseComment(in);
			return;
		}
		String name = null;
		StringBuffer sb = new StringBuffer();
		sb.append(c);

	// parse name

		boolean space = false;
		while(true) {
		//	i=nextChar(in);
			i = in.read();
			c=(char)i;
			if( c=='/' ) {
				c = (char)nextChar(in);
				if( c!='>' ) {
				//	throw new IOException("error parsing "+name);
					sb.append('/');
					sb.append(c);
					continue;
				}
				name = sb.toString();
				props.trimToSize();
				properties.add( new Object[] {name, props});
				return;
			} else if(c=='>') {
				name=sb.toString();
				break;
			} else {
				if( Character.isWhitespace(c) ) {
					if( space ) continue;
					space = true;
				} else {
					space = false;
				}
				sb.append(c);
			}
		}

		name = sb.toString();
	//	StringTokenizer st = new StringTokenizer(sb.toString());
	//	if( st.countTokens()>0 ) {
	//		name = st.nextToken();
// System.out.println( name );
	//	}
	//  parse properties

		sb = new StringBuffer();
		boolean white = false;

		while( true ) {
			i = in.read();
			if( i==-1 ) throw new EOFException();
			c = (char)i;
			if( c=='<' ) {
				c = (char)nextChar(in);
				if( c=='/' ) {
					String s = sb.toString().trim();
					try {
						parseEndTag(name, in);
					} catch(IOException e) {
						System.out.println( sb.length() +"\t"+ s );
						throw e;
					}
					if( s.length()==0 ) {
						props.trimToSize();
						properties.add( new Object[] {name, props});
						return;
					}
					if( props.size()>0 )throw new IOException( "parse error in "+name);
				//	properties.remove( 0 );
					properties.add( new Object[] {name, s} );
					return;
				} else if( c=='!' || c=='?' ) {
					parseComment(in);
				} else {
					addProperty( in, props, (int)c );
				}
			} else if( Character.isWhitespace(c) ) {
				if( white )continue;
				white = true;
				sb.append(' ');
			} else {
				sb.append( c );
				white = false;
			}
		}
	}
	static void parseEndTag(String name, BufferedReader in) throws IOException {
		StringBuffer sb = new StringBuffer();
		StringTokenizer st = new StringTokenizer(name);
		name = st.nextToken();
		while( true ) {
			char c = (char)nextChar(in);
			if( c=='>' ) {
				String nm = sb.toString();
				if( !nm.equals(name) ) throw new IOException("error parsing "+name +" : "+ nm);
				return;
			} else {
				sb.append(c);
			}
		}
	}
	static String parseComment(BufferedReader in) throws IOException {
		StringBuffer sb = new StringBuffer();
		boolean white = false;
		int count = 0;
		while( true ) {
			int i = in.read();
			if( i==-1 )throw new EOFException();
			char c = (char)i;
			if( Character.isWhitespace(c) ) {
				if( white )continue;
				white = true;
				sb.append(' ');
				continue;
			}
			if( c=='<' )count++;
			if( c=='>' ) {
				if( count==0) {
				//	System.out.println("comment:\t"+sb.toString());
					return sb.toString();
				}
				count--;
			}
			sb.append(c);
			white = false;
		}
	}
	static String getQuoteString(BufferedReader in) throws IOException {
		StringBuffer sb = new StringBuffer();
		int i;
		while( (char)(i=in.read())!='\"' ) sb.append((char)i);
		return sb.toString();
	}
	static int nextChar( BufferedReader in ) throws IOException {
		int i;
		while( (i=in.read())!=-1 ) {
// System.out.println( (char)i);
			if( Character.isWhitespace((char)i) )continue;
			else break;
		}
		if( i==-1 ) throw new EOFException();
		return i;
	}
	public static void printXML( Vector properties, int level, PrintStream out ) throws IOException {
	//	System.out.println( properties.size() );
		if( out==null ) out = System.out;
		for( int k=0 ; k<properties.size() ; k++) {
			Object[] p = (Object[])properties.get(k);
			StringBuffer sb = new StringBuffer();
			StringBuffer sb0 = new StringBuffer();
			for( int i=0 ; i<level ; i++)sb0.append("  ");
		//	sb.append( "<"+p[0].toString().toLowerCase()+">");
			sb.append( sb0+ "<"+p[0].toString()+">");
			if( p[1] instanceof String || p[1] instanceof Integer) {
				out.println( sb +p[1].toString()+"</"+p[0].toString()+">");
			} else {
				out.println( sb);
				printXML( (Vector)p[1], level+1, out);
			//	out.println( "</"+p[0].toString().toLowerCase()+">");
				StringTokenizer st = new StringTokenizer( p[0].toString() );
				out.println( sb0+ "</"+st.nextToken()+">");
			}
		}
	}
	public static void printProperties( Vector properties, int level ) throws IOException {
	//	System.out.println( properties.size() );
		for( int k=0 ; k<properties.size() ; k++) {
			Object[] p = (Object[])properties.get(k);
			StringBuffer sb = new StringBuffer();
			for( int i=0 ; i<level ; i++) sb.append("    ");
			sb.append( p[0].toString());
			sb.append(":");
			if( p[1] instanceof String ) {
				sb.append( "\t"+p[1].toString());
				System.out.println( sb);
			} else {
				System.out.println( sb);
				printProperties( (Vector)p[1], level+1);
			}
		}
	}
	public static void printProperties( Vector properties, int level, StringBuffer buffer ) throws IOException {
	//	System.out.println( properties.size() );
		for( int k=0 ; k<properties.size() ; k++) {
			Object[] p = (Object[])properties.get(k);
			StringBuffer sb = new StringBuffer();
			for( int i=0 ; i<level ; i++) sb.append("    ");
			sb.append( p[0].toString());
			sb.append(":");
			if( p[1] instanceof String ) {
				sb.append( "\t"+p[1].toString());
				buffer.append(sb+"\n");
			//	System.out.println( sb);
			} else {
				buffer.append(sb+"\n");
			//	System.out.println( sb);
				printProperties( (Vector)p[1], level+1, buffer);
			}
		}
	}
	public static Object getProperty( Vector props, String name ) {
		for( int i=0 ; i<props.size() ; i++) {
			Object[] prop = (Object[])props.get(i);
			if( name.equals(prop[0]) )return prop[1];
		}
		return null;
	}
	public static Vector getProperties( Vector props, String name ) {
		Vector p = new Vector();
		for( int i=0 ; i<props.size() ; i++) {
			if( props.get(i)==null )continue;
			Object[] prop = (Object[])props.get(i);
			if( name.equals(prop[0]) )p.add(prop[1]);
		}
		return p;
	}
	public static void main(String[] args) {
		if( args.length!=1 ) {
			System.out.println( "usage: java org.geomapapp.db.util.ParseLink url");
			System.exit(-1);
		}
		try {
			printXML( ParseLink.parse( URLFactory.url(args[0]) ), 0, null);
		//	printProperties( ParseLink.parse( URLFactory.url(args[0]) ), 0);
		} catch(IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}
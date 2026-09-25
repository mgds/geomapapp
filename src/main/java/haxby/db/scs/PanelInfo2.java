package haxby.db.scs;

import java.io.*;
import java.util.*;

public class PanelInfo2 {
	public static void main(String[] args) {
		String dir = (args.length==1) ? args[0]
					: System.getProperty( "user.dir" );
		File[] files = (new File( dir, "panels")).listFiles( new FileFilter() {
				public boolean accept( File file ) {
					return file.getName().endsWith(".info");
				}
			});
		if( files.length==0 ) {
			System.out.println( "no .info files in "+ dir+"/panels" );
			System.exit(0);
		}
		Vector names = new Vector();
		for( int k=0 ; k<files.length ; k++) names.add( files[k].getName() );
		Collections.sort( names );
		try {
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(
					new FileOutputStream( new File(dir, "panels2.info" ))));
			File pDir = new File( dir, "panels" );
			File file = new File( pDir, (String)names.get(0) );
			BufferedReader in = new BufferedReader( 
					new FileReader( file ));
			String s = in.readLine();
			in.close();
			StringTokenizer st = new StringTokenizer(s);
			short width = Short.parseShort( st.nextToken() );
			short height = Short.parseShort( st.nextToken() );
			double xScale = Double.parseDouble( st.nextToken() );
			double yScale = Double.parseDouble( st.nextToken() );
			int time = (int) Double.parseDouble( st.nextToken() );
			double top = Double.parseDouble( st.nextToken() );
			out.writeDouble( xScale );
			out.writeDouble( yScale );
			out.writeInt( names.size() );
			out.writeShort( width );
			out.writeShort( height );
			out.writeInt( time );
			out.writeShort( (short)( top / yScale ) );
			int x = 0;
			out.writeInt( 0 );
			int t = time + 30*width;
			x += width;
			for( int k=1 ; k<names.size() ; k++) {
				file = new File( pDir, (String)names.get(k) );
				in = new BufferedReader(
						new FileReader( file ));
				s = in.readLine();
				in.close();
				st = new StringTokenizer(s);
				width = Short.parseShort( st.nextToken() );
				height = Short.parseShort( st.nextToken() );
				out.writeShort( width );
				out.writeShort( height );
				st.nextToken();
				st.nextToken();
				time = (int) Double.parseDouble( st.nextToken() );
				out.writeInt( time );
				top = Double.parseDouble( st.nextToken() );
				out.writeShort( (short)( top / yScale ) );
				if( time-t>600 ) {
					x += 20;
				} else {
					x += (time-t)/30;
				}
				out.writeInt( x );
				t = time + 30*width;
				x += width;
			}
			System.out.println( "width = " + x );
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
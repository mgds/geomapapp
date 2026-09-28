package haxby.db.scs;

import java.io.*;
import java.util.*;

public class PanelInfo_dt {
	public static void main(String[] args) {
		
		double dt = Double.parseDouble( args[args.length-1] );
	
		String dir = (args.length==2) ? args[0]
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
					new FileOutputStream( new File(dir, "panels.info" ))));
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
			int time = (int) (Double.parseDouble( st.nextToken() ) + dt);
			double top = Double.parseDouble( st.nextToken() );
			out.writeDouble( xScale );
			out.writeDouble( yScale );
			out.writeInt( names.size() );
			out.writeShort( width );
			out.writeShort( height );
			out.writeInt( time );
			out.writeShort( (short)( top / yScale ) );
			for( int k=1 ; k<names.size() ; k++) {
				file = new File( pDir, (String)names.get(k) );
				in = new BufferedReader(
						new FileReader( file ));
				s = in.readLine();
				in.close();
				st = new StringTokenizer(s);
				out.writeShort( Short.parseShort( st.nextToken() ));
				out.writeShort( Short.parseShort( st.nextToken() ));
				st.nextToken();
				st.nextToken();
				out.writeInt( (int) (Double.parseDouble( st.nextToken() ) +dt) );
				out.writeShort( (short) (Double.parseDouble( st.nextToken() )/ yScale ));
			}
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
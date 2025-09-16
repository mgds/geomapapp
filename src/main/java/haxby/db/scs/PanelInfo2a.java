package haxby.db.scs;

import java.io.*;
import java.util.*;

public class PanelInfo2a {
	public static void main(String[] args) {
		String dir = (args.length==1) ? args[0]
					: System.getProperty( "user.dir" );
		File[] dirs = (new File(dir)).listFiles( new FileFilter() {
				public boolean accept( File file ) {
					if( !file.isDirectory() )return false;
					boolean ok = true;
					try {
						int k=Integer.parseInt( file.getName() );
					} catch (NumberFormatException ex) {
						ok = false;
					}
					return ok;
				}
			});
		Vector infoFiles = new Vector();
		for( int k=0 ; k<dirs.length ; k++) {
			System.out.println( dirs[k].getPath() );
			File[] files = (new File( dirs[k], "panels")).listFiles( new FileFilter() {
				public boolean accept( File file ) {
					return file.getName().endsWith(".info");
				}
			});
			if( files == null ) continue;
			System.out.println( files.length +" files" );
			for(int i=0 ; i<files.length ; i++) infoFiles.add(files[i]);
		}
		if( infoFiles.size()==0 ) {
			System.out.println( "no .jpg files in "+ dir+"/*/panels" );
			System.exit(0);
		}
		Collections.sort( infoFiles,
				new Comparator() {
					public int compare( Object o1, Object o2 ) {
						String s1 = ((File)o1).getName();
						String s2 = ((File)o2).getName();
						return s1.compareTo( s2 );
					}
					public boolean equals( Object o ) {
						return ( o==this );
					}
				});
		try {
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(
					new FileOutputStream( new File(dir, "panels2.info" ))));
			File file = (File)infoFiles.get(0);
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
			out.writeInt( infoFiles.size() );
			out.writeShort( width );
			out.writeShort( height );
			out.writeInt( time );
			out.writeShort( (short)( top / yScale ) );
			int x = 0;
			out.writeInt( 0 );
			int t = time + 30*width;
			x += width;
			for( int k=1 ; k<infoFiles.size() ; k++) {
				file = (File)infoFiles.get(k);
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
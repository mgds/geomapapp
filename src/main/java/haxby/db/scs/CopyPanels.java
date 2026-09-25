package haxby.db.scs;

import java.io.*;
import java.util.*;

public class CopyPanels {
	public static void main(String[] args) {
		String dir = (args.length==1) ? args[0]
					: System.getProperty( "user.dir" );
		File[] files = (new File( dir, "panels")).listFiles( new FileFilter() {
				public boolean accept( File file ) {
					return file.getName().endsWith(".jpg");
				}
			});
		if( files.length==0 ) {
			System.out.println( "no .jpg files in "+ dir+"/panels" );
			System.exit(0);
		}
		Vector names = new Vector();
		for( int k=0 ; k<files.length ; k++) names.add( files[k].getName() );
		Collections.sort( names );
		byte[] buf = new byte[32768];
		try {
			File pDir = new File( dir, "panels" );
			File p2Dir = new File( dir, "panels2" );
			if( !p2Dir.exists() ) p2Dir.mkdir();
			for( int k=0 ; k<names.size() ; k++) {
				File file = new File( pDir, (String)names.get(k) );
				BufferedInputStream in = new BufferedInputStream(
					new FileInputStream( file ));
				String name = ( (k<100) ? "0":"")
					+ ( (k<10) ? "0":"")
					+ k
					+ ".jpg";
				file = new File( p2Dir, name );
				BufferedOutputStream out = new BufferedOutputStream(
						new FileOutputStream( file ));
				int len;
				while( (len=in.read(buf)) != -1 ) {
					out.write( buf, 0, len );
				}
				in.close();
				out.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
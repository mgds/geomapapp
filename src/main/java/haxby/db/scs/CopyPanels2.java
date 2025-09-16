package haxby.db.scs;

import java.io.*;
import java.util.*;

public class CopyPanels2 {
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
		Vector jpgFiles = new Vector();
		for( int k=0 ; k<dirs.length ; k++) {
			System.out.println( dirs[k].getPath() );
			File[] files = (new File( dirs[k], "panels")).listFiles( new FileFilter() {
				public boolean accept( File file ) {
					return file.getName().endsWith(".jpg");
				}
			});
			if( files == null ) continue;
			System.out.println( files.length +" files" );
			for(int i=0 ; i<files.length ; i++) jpgFiles.add(files[i]);
		}
		if( jpgFiles.size()==0 ) {
			System.out.println( "no .jpg files in "+ dir+"/*/panels" );
			System.exit(0);
		}
		Collections.sort( jpgFiles,
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
		byte[] buf = new byte[32768];
		try {
			File p2Dir = new File( dir, "panels2" );
			if( !p2Dir.exists() ) p2Dir.mkdir();
			for( int k=0 ; k<jpgFiles.size() ; k++) {
				File file = (File)jpgFiles.get(k);
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
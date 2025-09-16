package org.geomapapp.io;

import java.io.*;
import java.util.*;
import javax.swing.JLabel;

public class FileUtility {
	public static void copyAll(File fromDir, File toDir) throws IOException {
		copyAll( fromDir, toDir, null);
	}
	public static void copyAll(File fromDir, File toDir, JLabel label) throws IOException {
		File[] files = fromDir.listFiles();
		for( int k=0 ; k<files.length ; k++) {
			File to = new File(toDir, files[k].getName());
			if( files[k].isDirectory() ) {
				if( !to.exists() )to.mkdir();
				copyAll( files[k], to, label );
			} else {
				copy( files[k], to, label);
			}
		}
	}
	public static void copy(File from, File to) throws IOException {
		copy( from, to, null);
	}
	public static void copy(File from, File to, JLabel label) throws IOException {
		if( !to.exists() ) {
			File dir = to.getParentFile();
			if(!dir.exists()) dir.mkdirs();
		}
		if( label!=null ) {
			label.setText("copying "+ from.getParentFile().getName()+File.separator+from.getName());
			label.paintImmediately( label.getVisibleRect() );
		}
		byte[] buf = new byte[32768];
		BufferedInputStream in = new BufferedInputStream(
			new FileInputStream(from), 32768);
		BufferedOutputStream out = new BufferedOutputStream(
			new FileOutputStream(to), 32768);
		int len=32768;
		int offset = 0;
		while( true ) {
			len=in.read(buf);
			if( len==-1 ) {
				in.close();
				out.flush();
				out.close();
				return;
			}
			out.write( buf, 0, len);
		}
	}
	public static long DiskUsage( File file ) {
		if( !file.exists() )return 0L;
		long size = file.length();
		if( file.isDirectory() ) {
			File[] files = file.listFiles();
			for( int k=0 ; k<files.length ; k++) size += DiskUsage( files[k] );
		}
		return size;
	}
	public static File[] getFiles(File dir, String suffix) {
		Vector files = new Vector();
		File[] list = dir.listFiles();
		if( list==null ) list = new File[0];
		for( int i=0 ; i<list.length ; i++) {
			String name = list[i].getName();
			if( name.endsWith(suffix) ) {
				files.add(list[i]);
			} else if( list[i].isDirectory() ) {
				File[] tmp = getFiles(list[i], suffix);
				for( int k=0 ; k<tmp.length ; k++) {
					files.add(tmp[k]);
				}
			}
		}
		list = new File[files.size()];
		for(int i=0 ; i<list.length ; i++) {
			list[i] = (File)files.get(i);
		}
		return list;
	}
	public static int getNLevel( File dir, String suffix) {
		return getNLevel( dir, suffix, 0);
	}
	public static int getNLevel( File dir, String suffix, int start) {
		File[] list = dir.listFiles();
		if( list==null ) return -1;
		for( int i=0 ; i<list.length ; i++) {
			String name = list[i].getName();
			if( name.endsWith(suffix) ) {
				return start;
			} else if( list[i].isDirectory() ) {
				int n = getNLevel( list[i], suffix, start+1 );
				if( n>=0 )return n;
			}
		}
		return -1;
	}
	public static void main(String[] args) {
		if( args.length!=2 && args.length!=3) {
			System.out.println("usage: java org.geomapapp.io.FileUtility dir suffix [code]\n\t"
					+"code = \t1 - count files (default)\n\t\t"+
					"2 - list files");
			System.exit(0);
		}
		File[] files = getFiles(new File(args[0]), args[1]);
		System.out.println(files.length +" files");
		if( args.length==2 || !args[2].equals("2") ) {
			System.exit(0);
		}
		for(int k=0 ; k<files.length ; k++) System.out.println(files[k].getPath());
		System.exit(0);
	}
		
}

package org.geomapapp.io;

import java.io.*;
import java.util.zip.*;

import javax.swing.JOptionPane;

public class ZipUtil {
	public static void zipDirectory(File dir, File archive) throws IOException {
		if( !dir.isDirectory() ) throw new IOException("dir must be a directory");
		if( !archive.getName().endsWith(".zip") ) archive = new File( archive.getParentFile(), archive.getName()+".zip");
		if( archive.isDirectory() ) throw new IOException("archive must not be a directory");
		if( archive.exists() ) {
			int ok=JOptionPane.showConfirmDialog(haxby.map.MapApp.anchor, "Archive file exists. Overwrite?",
					"overwrite?", JOptionPane.YES_NO_OPTION);
			if( ok==JOptionPane.NO_OPTION )return;
		}
		ZipOutputStream zip = new ZipOutputStream(
				new FileOutputStream(archive) );
		ZipEntry entry = new ZipEntry(dir.getName()+"/");
		entry.setTime( dir.lastModified() );
		zip.putNextEntry(entry);
		putDirectory(dir, zip, entry);
		zip.closeEntry();
		zip.close();
	}
	static void putDirectory(File dir, ZipOutputStream zip, ZipEntry entry) throws IOException {
		File[] files = dir.listFiles();
		if( files==null )return;
		for( int k=0 ; k<files.length ; k++) {
			if( files[k].isDirectory() ) {
				ZipEntry e = new ZipEntry(entry.getName()+files[k].getName()+"/");
				e.setTime( files[k].lastModified() );
				zip.putNextEntry(e);
				putDirectory( files[k], zip, e );
			} else {
				BufferedInputStream in = new BufferedInputStream(
					new FileInputStream( files[k] ), 32768);
				ZipEntry e = new ZipEntry(entry.getName()+files[k].getName());
				e.setTime( files[k].lastModified() );
				zip.putNextEntry(e);
				byte[] buf = new byte[32768];
				int len=32768;
				int offset = 0;
				while( true ) {
					len=in.read(buf);
					if( len==-1 ) {
						in.close();
						break;
					}
					zip.write( buf, 0, len);
				}
			}
		}
	}
	public static void main(String[] args) {
		if( args.length!=2 ) {
			System.out.println("usage: java org.geomapapp.io.ZipUtil dir archive");
			System.exit(0);
		}
		try {
			ZipUtil.zipDirectory( new File(args[0]), new File(args[1]));
		} catch(IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}

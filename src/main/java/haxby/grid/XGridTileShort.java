package haxby.grid;

import java.io.*;
import java.net.*;
import java.util.zip.*;

import haxby.proj.*;

public class XGridTileShort extends XGridTile {
	
	public XGridTileShort(int x0, int y0, 
			int size, 
			File dir,
			Projection proj) throws IOException {
		super( x0, y0, size, dir, proj);
	}
	public XGridTileShort(int x0, int y0, 
			int size, 
			URL url,
			Projection proj) throws IOException {
		super( x0, y0, size, url, proj);
	}
	public String fileName() {
		return ( (x0>=0) ? "E"+x0 : "W"+(-x0) )
			+ ( (y0>=0) ? "N"+y0 : "S"+(-y0) )
			+ "_" + size + ".igrid.gz";
	}
	public void writeGrid() throws IOException {
		if( url!=null )return;
		File file = new File(dir, fileName());
		if( !file.exists() ) file.createNewFile();
		DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(
				new GZIPOutputStream(
				new FileOutputStream(file))));
		int i=0;
		int n;
		short[] buffer = new short[grid.length];
		for( int k=0 ; k<grid.length ; k++ ) {
			if( Float.isNaN(grid[k])) {
				buffer[k] = XgrdIO.NODATA;
			} else if( grid[k]>32767f || grid[k]<-32767f) {
				buffer[k] = XgrdIO.NODATA;
			} else {
				buffer[k] = (short) Math.rint( grid[k] );
			}
		}
		byte[] buf = XgrdIO.encode( buffer );
		n = buf.length;
		out.writeInt( n );
		out.write( buf, 0, n);
		out.close();
	}
	void readGrid() throws IOException {
		DataInputStream in =null;
		if( url==null ) {
			in = new DataInputStream(
				new GZIPInputStream(
				new BufferedInputStream(
				new FileInputStream(new File(dir, fileName())))));
		} else {
			in = new DataInputStream( 
				new GZIPInputStream( url.openStream() ));
		}
		int i=0;
		int n = in.readInt();
		byte[] buf = new byte[n];
		in.readFully( buf );
		in.close();

		short[] h = XgrdIO.decode( buf, grid.length );
		if( h.length!=size*size ) {
			throw new IOException( "incorrect length: "+ 
					h.length +" ("+ (size*size) +")");
		}
		for( int k=0 ; k<grid.length ; k++ ) {
			if( h[k]==XgrdIO.NODATA ) {
				grid[k] = Float.NaN;
			} else {
				grid[k] = (float)h[k];
			}
		}
	}
}

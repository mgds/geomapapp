package org.geomapapp.gis.shape;

import org.geomapapp.io.LittleIO;

import java.io.*;

public class MainHeader {
	public int length;
	public int version;
	public int type;
	public double[] xBounds;
	public double[] yBounds;
	public double[] zBounds;
	public double[] mBounds;
	protected MainHeader() {
	}
	public MainHeader( int length,
			int version,
			int type,
			double[] xBounds,
			double[] yBounds,
			double[] zBounds,
			double[] mBounds ) {
		this.length = length;
		this.version = version;
		this.type = type;
		this.xBounds = xBounds;
		this.yBounds = yBounds;
		this.zBounds = zBounds;
		this.mBounds = mBounds;
	}
	public static MainHeader getHeader( InputStream input ) throws IOException {
		DataInputStream in = new DataInputStream(input);
		if( in.readInt()!= 9994 ) 
			throw new IOException("not a shape file");
		MainHeader hdr = new MainHeader();
		for( int k=0 ; k<5 ; k++ )in.readInt();
		hdr.length = in.readInt();
		hdr.version = LittleIO.readInt(in);
		hdr.type = LittleIO.readInt(in);
		double minX = LittleIO.readDouble(in);
		double minY = LittleIO.readDouble(in);
		double maxX = LittleIO.readDouble(in);
		double maxY = LittleIO.readDouble(in);
		
		hdr.xBounds = new double[] {
				minX,
				maxX
				};
		hdr.yBounds = new double[] {
				minY,
				maxY
				};
		hdr.zBounds = new double[] {
				LittleIO.readDouble(in),
				LittleIO.readDouble(in)
				};
		hdr.mBounds = new double[] {
				LittleIO.readDouble(in),
				LittleIO.readDouble(in)
				};
		return hdr;
	}
	public void writeHeader(OutputStream output) throws IOException {
		DataOutputStream out = new DataOutputStream(output);
		out.writeInt(9994);
		for( int k=0 ; k<5 ; k++ )out.writeInt(0);
		out.writeInt( length );
		LittleIO.writeInt( version, out );
		LittleIO.writeInt( type, out );
		for( int k=0 ; k<2 ; k++ )LittleIO.writeDouble(xBounds[k], out);
		for( int k=0 ; k<2 ; k++ )LittleIO.writeDouble(yBounds[k], out);
		for( int k=0 ; k<2 ; k++ )LittleIO.writeDouble(zBounds[k], out);
		for( int k=0 ; k<2 ; k++ )LittleIO.writeDouble(mBounds[k], out);
	}
	public static void main(String[] args) {
		javax.swing.JFileChooser c = new javax.swing.JFileChooser(
					System.getProperty("user.dir"));
		int ok = c.showOpenDialog(null);
		if( ok==c.CANCEL_OPTION ) System.exit(0);
		try {
			MainHeader h = getHeader( new FileInputStream(
					c.getSelectedFile() ));
			System.out.println( h.length);
			System.out.println( h.version);
			System.out.println( h.type);
			System.out.println( h.xBounds[0] +"\t"+ h.xBounds[0] +"\t"+ h.xBounds[0] +"\t"+ h.xBounds[0]);
			System.out.println( h.yBounds[0] +"\t"+ h.yBounds[0] +"\t"+ h.yBounds[0] +"\t"+ h.yBounds[0]);
			System.out.println( h.zBounds[0] +"\t"+ h.zBounds[0] +"\t"+ h.zBounds[0] +"\t"+ h.zBounds[0]);
			System.out.println( h.mBounds[0] +"\t"+ h.mBounds[0] +"\t"+ h.mBounds[0] +"\t"+ h.mBounds[0]);
		} catch(IOException ex) {
			System.out.println( ex.getMessage() );
		}
		System.exit(0);
	}
}

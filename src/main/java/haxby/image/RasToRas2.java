package haxby.image;

import java.io.*;
import java.util.zip.*;

public class RasToRas2 {
	double gamma;
	String inFile, outFile;
	public RasToRas2( String file ) {
		inFile = file;
		gamma = 1.;
		outFile = "out.r2.gz";
	}
	public void setGamma( double gamma ) {
		this.gamma = gamma;
	}
	public void setOutputFile( String name ) {
		outFile = name;
	}
	public void perform() throws IOException {
		DataInputStream in = new DataInputStream(
				new BufferedInputStream(
				new FileInputStream( inFile )));
		DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(
				new GZIPOutputStream(
				new FileOutputStream( outFile ))));
		if( in.readInt() != R2.MAGIC ) throw new IOException( "unrecognized fromat" );
		int w = in.readInt();
		int h = in.readInt();
		if( in.readInt() != 8 ) {
			throw new IOException("not a 2-bit sunraster[sic] file");
		}
		int size = in.readInt();
		in.readInt();
		in.readInt();
		int lutLength = in.readInt()/3;
		int[] lut = new int[lutLength];
		for( int i=0 ; i<3 ; i++) {
			for( int k=0 ; k<lutLength ; k++) {
				lut[k] = in.readUnsignedByte();
			}
		}
		out.writeInt(R2.MAGIC);
		out.writeInt( w );
		out.writeInt( h );
		out.writeInt(2);
		out.writeInt( w*h/4 );
		out.writeInt(0);
		out.writeInt(0);
		out.writeInt(0);
		if( gamma != 1. ) {
			for( int k=0 ; k<lutLength ; k++) {
				double z = Math.pow( lut[k]/255., 1./gamma )*255.;
				lut[k] = (int)Math.rint(z);
				if( lut[k]<75 )lut[k]=3;
				else if( lut[k]<150 )lut[k]=2;
				else if( lut[k]<225 )lut[k]=1;
				else lut[k]=0;
			}
		}
		for( int y=0 ; y<h ; y++ ) {
			int yy = y*w;
			for( int x=0 ; x<w ; x+=4) {
				byte b = (byte)( (lut[in.readUnsignedByte()]<<6)
					| (lut[in.readUnsignedByte()]<<4)
					| (lut[in.readUnsignedByte()]<<2)
					| lut[in.readUnsignedByte()]);
				out.writeByte(b);
			}
		}
		out.close();
	}
	public static void main(String[] args) {
		if( args.length!=2 ) {
			System.out.println( "usage java haxby.image.RasToRas2 file(8-bit-sunraster) gamma");
			System.exit(0);
		}
		try {
			RasToRas2 rr = new RasToRas2(args[0]);
			rr.setGamma( Double.parseDouble( args[1] ));
			String file = args[0].substring(0, args[0].indexOf(".ras") ) +".r2.gz";
			rr.setOutputFile( file );
			rr.perform();
		} catch(IOException ex) {
			ex.printStackTrace();
		}
	}
}

package haxby.image;

import java.io.*;
import java.awt.image.*;
//import com.sun.image.codec.jpeg.*;

import javax.imageio.ImageIO;

public class QT {
	int nFrame;
	int frameRate;
	int width, height;
	RandomAccessFile out;
	long sizePointer, offsetPointer, dataPointer, mdatPointer;
	int iFrame;
	int length;
	File file;
	public QT( 	int nFrame, 
				int frameRate, 
				int width, 
				int height, 
				File file) throws IOException {
		this.nFrame = nFrame;
		this.frameRate = frameRate;
		this.file = file;
		this.width = width;
		this.height = height;
		length = 8;
		init();
	}
	public int addImage( BufferedImage image ) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		//JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(bout);
		//encoder.encode(image);
		ImageIO.write(image, "JPEG", bout);
		byte[] code = bout.toByteArray();
		addJPEG( code );
		return code.length;
	}
	public void addJPEG( byte[] code ) throws IOException {
		out.seek( sizePointer );
		out.writeInt( code.length );
		sizePointer += 4;
		out.seek( offsetPointer);
		out.writeInt( (int)dataPointer );
		offsetPointer += 4;
		out.seek( dataPointer );
		out.write( code );
		dataPointer += code.length;
		iFrame++;
		length += code.length;
		if( iFrame==nFrame ) {
			out.seek( mdatPointer );
			out.writeInt( length );
			out.close();
		}
	}
	void init() throws IOException {
		out = new RandomAccessFile(file, "rw");
		int hLen = 590 + nFrame*8;
		out.writeInt( hLen );
		out.writeBytes( "moov" );
//  mvhd atom
		out.writeInt( 108 );
		out.writeBytes( "mvhd" );
		out.writeInt( 0 );
		out.writeInt( 0 );
		out.writeInt( 0 );
		out.writeInt( 60000 );		// time scale - units per sec
		out.writeInt( 60000*nFrame/frameRate );		// duration
		out.writeShort( 1 );
		out.writeShort( 0 );
		out.writeShort( 255 );
		out.writeShort( 0 );
		out.writeInt( 0 );
		out.writeInt( 0 );
//  matrix
		out.writeShort( 1 );
		out.writeShort( 0 );
		for(int k=0 ; k<3 ; k++ ) out.writeInt( 0 );
		out.writeShort( 1 );
		out.writeShort( 0 );
		for(int k=0 ; k<3 ; k++ ) out.writeInt( 0 );
		out.writeShort( 16384 );
		out.writeShort( 0 );
		for(int k=0 ; k<6 ; k++ ) out.writeInt( 0 );
		out.writeInt( 3 );
// end of mvhd atom
// trak atom
		int tLen = hLen-8-108;
		out.writeInt( tLen );
		out.writeBytes( "trak" );
		out.writeInt( 92 );
		out.writeBytes( "tkhd" );
		out.writeInt( 3 );
		out.writeInt( 0 );
		out.writeInt( 0 );
		out.writeInt( 1 );		// ID
		out.writeInt( 0 );
		out.writeInt( 60000*nFrame/frameRate );
		for(int k=0 ; k<4 ; k++ ) out.writeInt( 0 );
		out.writeShort( 1 );
		out.writeShort( 0 );
		for(int k=0 ; k<3 ; k++ ) out.writeInt( 0 );
		out.writeShort( 1 );
		out.writeShort( 0 );
		for(int k=0 ; k<3 ; k++ ) out.writeInt( 0 );
		out.writeShort( 16384 );
		out.writeShort( 0 );
		out.writeShort( width );
		out.writeShort( 0 );
		out.writeShort( height );
		out.writeShort( 0 );
//
// media atom
		out.writeInt( tLen-100 );
		out.writeBytes( "mdia" );
// media header
		out.writeInt( 32 );
		out.writeBytes( "mdhd" );
		out.writeInt( 1 );	
		out.writeInt( 0 );
		out.writeInt( 0 );
		out.writeInt( 60000 );
		out.writeInt( 60000*nFrame/frameRate );
		out.writeInt( 0 );

		out.writeInt( 36 );
		out.writeBytes( "hdlr" );
		out.writeInt( 0 );
		out.writeBytes( "mhlr" );
		out.writeBytes( "vide" );
		out.writeBytes( "    " );
		out.writeInt( 0 );
		out.writeInt( 0 );
		out.writeBytes( "    " );
// minf
		out.writeInt( tLen-176 );
		out.writeBytes( "minf" );
		out.writeInt( 20 );
		out.writeBytes( "vmhd" );
		out.writeInt( 1 );
		out.writeShort( 64 );
		out.writeShort( -32768 );
		out.writeShort( -32768 );
		out.writeShort( -32768 );

		out.writeInt( 36 );
		out.writeBytes( "hdlr" );
		out.writeInt( 0 );
		out.writeBytes( "dhlr" );
		out.writeBytes( "alis" );
		out.writeBytes( "    " );
		out.writeInt( 0 );
		out.writeInt( 0 );
		out.writeBytes( "    " );

		out.writeInt( 36 );
		out.writeBytes( "dinf" );
		out.writeInt( 28 );
		out.writeBytes( "dref" );
		out.writeInt( 0 );
		out.writeInt( 1 );
		out.writeInt( 12 );
		out.writeBytes( "alis" );
		out.writeInt( 1 );
// stbl
		out.writeInt( tLen-276 );
		out.writeBytes( "stbl" );

		out.writeInt( 102 );
		out.writeBytes( "stsd" );
		out.writeInt( 0 );
		out.writeInt( 1 );
		out.writeInt( 86 );
		out.writeBytes( "jpeg" );
		out.writeInt( 0 );
		out.writeInt( 1 );
		out.writeInt( 0 );
		out.writeBytes( "appl" );
		out.writeInt( 1023 );
		out.writeInt( 1023 );
		out.writeShort( width );
		out.writeShort( height );
		out.writeShort( 72 );
		out.writeShort( 0 );
		out.writeShort( 72 );
		out.writeShort( 0 );
		out.writeShort( 0 );
		out.writeInt( 1 );
		out.writeBytes( "jpeg" );
		for( int k=0 ; k<7 ; k++) out.writeBytes( "    " );
		out.writeShort( 24 );
		out.writeShort( -1 );
// stts
		out.writeInt( 24 );
		out.writeBytes( "stts" );
		out.writeInt( 0 );
		out.writeInt( 1 );
		out.writeInt( nFrame );
		out.writeInt( 60000 / frameRate );
// stsc
		out.writeInt( 28 );
		out.writeBytes( "stsc" );
		out.writeInt( 0 );
		out.writeInt( 1 );
		out.writeInt( 1 );
		out.writeInt( 1 );
		out.writeInt( 1 );
// stsz
		out.writeInt( 4*(5+nFrame) );
		out.writeBytes( "stsz" );
		out.writeInt( 0 );
		out.writeInt( 0 );
		out.writeInt( nFrame );
		sizePointer = out.getFilePointer();
		for( int k=0 ; k<nFrame ; k++ ) out.writeInt(0);
// stco
		out.writeInt( 4*(4+nFrame) );
		out.writeBytes( "stco" );
		out.writeInt( 0 );
		out.writeInt( nFrame );
		offsetPointer = out.getFilePointer();
		for( int k=0 ; k<nFrame ; k++ ) out.writeInt(0);
		mdatPointer = out.getFilePointer();
		out.writeInt(0);
		out.writeBytes( "mdat" );
		dataPointer = out.getFilePointer();
		iFrame = 0;
	}
}

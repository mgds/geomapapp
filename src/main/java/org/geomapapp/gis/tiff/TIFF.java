package org.geomapapp.gis.tiff;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import java.io.*;
import java.net.URL;
import org.geomapapp.io.LittleIO;

import org.geomapapp.grid.Grid2D;
import haxby.proj.*;

public class TIFF {
	public final static short BYTE = 1;
	public final static short ASCII = 2;
	public final static short SHORT = 3;
	public final static short LONG = 4;
	public final static short RATIONAL = 5;
	public final static short SBYTE = 6;
	public final static short UNDEFINED = 7;
	public final static short SSHORT = 8;
	public final static short SLONG = 9;
	public final static short SRATIONAL = 10;
	public final static short FLOAT = 11;
	public final static short DOUBLE = 12;
	public final static int[] TYPE_LENGTH = new int[] {
				1, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8};
	boolean little;
	int width;
	int height;
	int[] bitsPerSample;
	File file;
	public TIFF(File file) {
		this.file = file;
	}
	public void read() throws IOException {
		RandomAccessFile in = new RandomAccessFile( file, "r" );
		short endian = in.readShort();
		if( endian!=0x4949 && endian!=0x4d4d )
			throw new IOException("Not a TIFF File");
		little = endian==0x4949;
		short code = little ?
			LittleIO.readShort(in)
			: in.readShort();
		if( code!=42 ) throw new IOException("Not a TIFF File");
		int IFD = little ?
			LittleIO.readInt(in)
			: in.readInt();
		in.seek( (long)IFD );
		int nEntry = little ?
			LittleIO.readShort(in)
			: in.readShort();
		Entry[] entry = new Entry[nEntry];
		for(int k=0 ; k<nEntry ; k++) {
			int tag = 0x0000ffff & (little ?
				LittleIO.readShort(in)
				: in.readShort());
			short type = little ?
				LittleIO.readShort(in)
				: in.readShort();
			int count = little ?
				LittleIO.readInt(in)
				: in.readInt();
System.out.println( tag +"\t"+ type +"\t"+ count);
			if( count*TYPE_LENGTH[type]<=4 ) {
				long address = in.getFilePointer();
				entry[k] = new Entry( tag, type, count, (int)address);
				entry[k].readData(in);
				int len = TYPE_LENGTH[type];
				for( int i=count*len ; i<4 ; i++)in.readByte();
			} else {
				int offset = little ?
					LittleIO.readInt(in)
					: in.readInt();
				entry[k] = new Entry( tag, type, count, offset);
			}
			byte[] data = entry[k].data;
		}
		for(int k=0 ; k<nEntry ; k++) {
			Entry e = entry[k];
			if( e.data==null ) {
				in.seek( (long)e.offset );
				e.readData( in );
			}
			System.out.println( k +"\t"+ e.tag +"\t"+ e.type  +"\t"+ e.count );
			int n = Math.min(6, e.count);
			if( e.type==ASCII ) n=e.count;
			DataInputStream din = new DataInputStream(
				new ByteArrayInputStream(e.data));
			if( e.tag==34735 ) {
				for( int i=0 ; i<e.count ; i+=4 ) {
					System.out.println("\t\t"+ din.readUnsignedShort()
							+"\t"+ din.readUnsignedShort()
							+"\t"+ din.readUnsignedShort()
							+"\t"+ din.readUnsignedShort());
				}
				continue;
			}
			StringBuffer sb = new StringBuffer();
			if(e.type==ASCII)sb.append("\t");
			for( int i=0 ; i<n ; i++) {
				if( e.type==UNDEFINED || e.type==BYTE || e.type==SBYTE) sb.append("\t"+din.readByte());
				else if( e.type==LONG || e.type==SLONG) sb.append("\t"+ din.readInt());
				else if( e.type==SHORT ) sb.append("\t"+ (0x0000ffff & din.readShort()));
				else if( e.type==SSHORT ) sb.append("\t"+ din.readShort());
				else if( e.type==ASCII ) {
					byte b = din.readByte();
					if( b!=0&&(char)b!='|' )sb.append((char)b);
					else if(i!=n-1)sb.append("\n\t");
					if( (char)b=='\n' )sb.append("\t");
				} else if( e.type==FLOAT ) sb.append("\t"+ din.readFloat());
				else if( e.type==DOUBLE ) {
					try {
						sb.append("\t"+ din.readDouble());
					} catch(EOFException ex) {
						sb.append("\tEOF");
					}
				} else if( e.type==RATIONAL || e.type==SRATIONAL ) sb.append("\t("+ din.readInt()
										+","+din.readInt()+")");
			}
			System.out.println( sb );
		}
	}
	public void write() throws IOException {
	}
	class Entry {
		public int tag;
		public short type;
		public int count;
		public byte[] data;
		int offset;
		public Entry( int tag, short type, int count, int offset) {
			this.tag = tag;
			this.type = type;
			this.count = count;
			this.offset = offset;
		}
		public Entry( int tag, short type, int count, byte[] data) {
			this.tag = tag;
			this.type = type;
			this.count = count;
			this.data = data;
		}
	//	public byte[] readData( RandomAccessFile in ) throws IOException {
	//		if( data != null ) return data;
	//		long address = in.getFilePointer();
	//		in.seek( (long)offset );
	//		return readData( in );
	//	}
		public byte[] readData( DataInput in ) throws IOException {
			if( data != null ) return data;
			int len = TYPE_LENGTH[type];
			data = new byte[count*len];
			if( len==1 || !little ) {
				for( int i=0 ; i<count*len ; i++ ) {
				try {
					data[i]=in.readByte();
				} catch(EOFException e) {
					System.out.println( "*** "+ tag +"\t"+(i/len) );
				}
				}
			} else {
				for( int i=0 ; i<count*len ; i+=len ) {
					for( int j=0 ; j<len ; j++) data[i+len-j-1]=in.readByte();
				}
			}
			return data;
		}
	}
	public static void writeTiff( Grid2D grid, File file ) throws IOException {
		boolean isImage = grid instanceof Grid2D.Image;
		Grid2D.Image image = isImage
			? (Grid2D.Image)grid
			: null;
		int bytesPerSample = isImage
			? 3
			: 4;
		Mercator proj = null;
		try {
			proj = (Mercator) grid.getProjection();
		} catch(ClassCastException e) {
			throw new IOException( "Projection must be Mercator" );
		}

		Rectangle bounds = grid.getBounds();
		int bufferLength = bytesPerSample * bounds.width*bounds.height;
		boolean odd = (bufferLength&1) == 1;
		if( odd ) bufferLength++;
		DataOutputStream out = new DataOutputStream(
			new BufferedOutputStream(
			new FileOutputStream( file )));
		out.writeShort(0x4d4d);
		out.writeShort( (short)42);
		out.writeInt( 8+bufferLength );
		for( int y=bounds.y ; y<bounds.y+bounds.height ; y++) {
			for( int x=bounds.x ; x<bounds.x+bounds.width ; x++) {
				if( isImage ) {
					int rgb = image.rgbValue(x, y);
					out.write( (rgb>>16)&255 );
					out.write( (rgb>>8)&255 );
					out.write( rgb&255 );
				} else {
					float val = (float)grid.valueAt(x,y);
					out.writeFloat( val );
				}
			}
		}
		if(odd) out.write(0);
		int rowsPerStrip = 1;
		while( bytesPerSample*bounds.width*rowsPerStrip < 8192 ) rowsPerStrip++;
		int nStrip = bounds.height/rowsPerStrip;
		if( nStrip*rowsPerStrip<bounds.height ) nStrip++;

		double wrap = Math.rint(360./proj.getLongitude(1.));
		int iwrap = (int)Math.rint(wrap);
// System.out.println( bounds +"\t"+ iwrap +"\t"+ proj.getLongitude(1.));
		double mPerNode = proj.major[0]*2.*Math.PI /wrap;
		
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream( bout );

		int nEntry = 19;
		if( isImage ) nEntry--;
		int address = 8+bufferLength + nEntry*12 + 6;

		out.writeShort( nEntry );
	// width (ImageWidth)
		out.writeShort( (short)256 );
		out.writeShort( LONG );
		out.writeInt( 1 );
		out.writeInt( bounds.width );
	// height (ImageLength)
		out.writeShort( (short)257 );
		out.writeShort( LONG );
		out.writeInt( 1 );
		out.writeInt( bounds.height );
	// BitsPerSample 
		out.writeShort( (short)258 );
		out.writeShort( SHORT );
		if( isImage ) {
			out.writeInt( 3 );
			out.writeInt( address );
			dout.writeShort( 8 );
			dout.writeShort( 8 );
			dout.writeShort( 8 );
			address += 6;
		} else {
			out.writeInt( 1 );
			out.writeShort( (short)(bytesPerSample*8) );
			out.writeShort((short)0);
		}
	// Compression 
		out.writeShort( (short)259 );
		out.writeShort( SHORT );
		out.writeInt( 1 );
		out.writeShort( (short)1 );	// no compression
		out.writeShort((short)0);
	// PhotometricInterpretation
		out.writeShort( (short)262 );
		out.writeShort( SHORT );
		int photo = isImage ? 2 : 1;
		out.writeInt(1);
		out.writeShort( photo );
		out.writeShort((short)0);
	// StripOffsets
		out.writeShort( (short)273 );
		out.writeShort( LONG );
		out.writeInt( nStrip );
		out.writeInt( address );
		address += nStrip*4;
		int offset = 8;
		for( int k=0 ; k<nStrip ; k++) {
			dout.writeInt( offset );
			offset += bounds.width*rowsPerStrip*bytesPerSample;
		}
	// Orientation
		out.writeShort( (short)274 );
		out.writeShort( SHORT );
		out.writeInt( 1 );
		out.writeShort( 1 );
		out.writeShort((short)0);
	// SamplesPerPixel
		out.writeShort( (short)277 );
		out.writeShort( SHORT );
		int samples = isImage ? 3 : 1;
		out.writeInt( 1 );
		out.writeShort( samples );
		out.writeShort((short)0);
	// RowsPerStrip
		out.writeShort( (short)278 );
		out.writeShort( LONG );
		out.writeInt( 1 );
		out.writeInt( rowsPerStrip );
	// StripByteCounts
		out.writeShort( (short)279 );
		out.writeShort( LONG );
		out.writeInt( nStrip );
		out.writeInt( address );		
		address += nStrip*4;
		int len = rowsPerStrip*bytesPerSample*bounds.width;
		for( int k=0 ; k<nStrip-1 ; k++) {
			dout.writeInt( len );
		}
		int extra = bytesPerSample*bounds.width * (bounds.height - (nStrip-1)*rowsPerStrip);
		dout.writeInt( extra );

		int resolution = (int)( Math.max( bounds.width, bounds.height)/4. );
	// XResolution
		out.writeShort( (short)282 );
		out.writeShort( RATIONAL );
		out.writeInt( 1 );
		out.writeInt( address );
		dout.writeInt(resolution);
		dout.writeInt(1);
		address += 8;
	// YResolution
		out.writeShort( (short)283 );
		out.writeShort( RATIONAL );
		out.writeInt( 1 );
		out.writeInt( address );
		dout.writeInt(resolution);
		dout.writeInt(1);
		address += 8;
	// PlanarConfiguration (probably not necessary)
		out.writeShort( (short)284 );
		out.writeShort( SHORT );
		out.writeInt( 1 );
		out.writeShort( 1 );
		out.writeShort((short)0);
	// ResolutionUnit
		out.writeShort( (short)296 );
		out.writeShort( SHORT );
		out.writeInt( 1 );
		out.writeShort( 2 );	// Inches
		out.writeShort((short)0);
	// SampleFormat
	if( !isImage ) {
		out.writeShort( (short)339 );
		out.writeShort( SHORT );
		out.writeInt( 1 );
		int fmt = isImage ? 1 : 3;
		out.writeShort( fmt );
		out.writeShort((short)0);
	}
	// GeoTIFF ModelPixelScaleTag
		out.writeShort( 33550 );
		out.writeShort( DOUBLE );
		out.writeInt( 3 );
		out.writeInt(address);
		dout.writeDouble( mPerNode );
		dout.writeDouble( mPerNode );
		dout.writeDouble( 0. );
		address += 3*8;
	// GeoTIFF ModelTiepointTag
		out.writeShort( 33922 );
		out.writeShort( DOUBLE );
		out.writeInt( 6 );
		out.writeInt(address);
		dout.writeDouble( 0. );
		dout.writeDouble( 0. );
		dout.writeDouble( 0. );
		int xx = bounds.x;
		while( xx>iwrap/2 )xx -= iwrap;
		while( xx<-iwrap/2 )xx += iwrap;
		dout.writeDouble( xx*mPerNode );
		dout.writeDouble( -bounds.y*mPerNode );
		dout.writeDouble( 0. );
		address += 6*8;
	// GeoTIFF GeoKeyDirectoryTag
		out.writeShort( 34735 );
		out.writeShort( SHORT );
		out.writeInt( 56 );
		out.writeInt( address );
		address +=2*56;
	// GeoTIFF header
		dout.writeShort( 1 );
		dout.writeShort( 1 );
		dout.writeShort( 2 );
		dout.writeShort( 13 );
	// GTModelTypeGeoKey
		dout.writeShort( 1024 );
		dout.writeShort( 0 );
		dout.writeShort( 1 );
		dout.writeShort( 1 );
	// GTRasterTypeGeoKey
		dout.writeShort( 1025 );
		dout.writeShort( 0 );
		dout.writeShort( 1 );
		int rasType = isImage ? 1 : 2;
		dout.writeShort( rasType );		// 2 (RasterPixelIsPoint) for grids, 1 for images
	// ProjectionGeoKey
		dout.writeShort( 3074 );
		dout.writeShort( 0 );
		dout.writeShort( 1 );
		dout.writeShort( 32767 );	// user defined
	// ProjCoordTransGeoKey
		dout.writeShort( 3075 );
		dout.writeShort( 0 );
		dout.writeShort( 1 );
		dout.writeShort( 7 );
/*
	// GeogGeodeticDatumGeoKey
		dout.writeShort( 2050 );
		dout.writeShort( 0 );
		dout.writeShort( 1 );
		if( grid.toString().equals("Topography") ) {
			dout.writeShort( 6326 );	// Datum_WGS84
		} else if( grid.toString().equals("gravity") ) {
			dout.writeShort( 0 );		// Datum undefined for gravity
		} else if( grid.toString().equals("geoid") ) {
			dout.writeShort( 6030 );	// DatumE_WGS84 (Ellipsoid) for geoid
		} else {
			dout.writeShort( 6326 );	// Datum_WGS84
		}
*/
	// GeographicTypeGeoKey
		dout.writeShort( 2048 );
		dout.writeShort( 0 );
		dout.writeShort( 1 );

//		***** Changed by A.K.M. 06/27/06 *****
//		if( grid.toString().equals("DEM") ) {
//		Changed "DEM" to topography because changed name in 
//		Grid2DOverlay.java
		if( grid.toString().equals(org.geomapapp.grid.GridDialog.DEM) ) {
//		***** Changed by A.K.M. 06/27/06 *****

		//	dout.writeShort( 4326 );	// Datum_WGS84
			dout.writeShort( 4035 );	// GCSE_Sphere
		} else if( grid.toString().equals(org.geomapapp.grid.GridDialog.GRAVITY) ) {
		//	dout.writeShort( 4030 );		// Datum undefined for gravity
			dout.writeShort( 4035 );		// GCSE_Sphere
		} else if( grid.toString().equals(org.geomapapp.grid.GridDialog.GEOID) ) {
		//	dout.writeShort( 4030 );	// DatumE_WGS84 (Ellipsoid) for geoid
			dout.writeShort( 4035 );	// GCSE_Sphere
		} else {
		//	dout.writeShort( 4326 );	// Datum_WGS84
			dout.writeShort( 4035 );	// GCSE_Sphere
		}
	// GeogEllipsoidGeoKey
		dout.writeShort( 2056 );
		dout.writeShort( 0 );
		dout.writeShort( 1 );
		dout.writeShort( 7035 );	// Ellipse_Sphere
	// ProjNatOriginLongGeoKey
		dout.writeShort( 3080 );
		dout.writeShort( 34736 );
		dout.writeShort( 1 );
		dout.writeShort( 0 );
	// ProjNatOriginLatGeoKey
		dout.writeShort( 3081 );
		dout.writeShort( 34736 );
		dout.writeShort( 1 );
		dout.writeShort( 1 );
	// ProjFalseEastingGeoKey
		dout.writeShort( 3082 );
		dout.writeShort( 34736 );
		dout.writeShort( 1 );
		dout.writeShort( 2 );
	// ProjFalseNorthingGeoKey
		dout.writeShort( 3083 );
		dout.writeShort( 34736 );
		dout.writeShort( 1 );
		dout.writeShort( 3 );
	// ProjScaleAtNatOriginGeoKey
		dout.writeShort( 3092 );
		dout.writeShort( 34736 );
		dout.writeShort( 1 );
		dout.writeShort( 4 );
	// ProjectedCSTypeGeoKey
		dout.writeShort( 3072 );
		dout.writeShort( 0 );
		dout.writeShort( 1 );
		dout.writeShort( 32767 );
	// ProjLinearUnitsGeoKey
		dout.writeShort( 3076 );
		dout.writeShort( 0 );
		dout.writeShort( 1 );
		dout.writeShort( 9001 );
/*		
                1       1       2       15
                1024    0       1       1
                1025    0       1       1
                3075    0       1       7
                3074    0       1       32767
                2050    0       1       6267
                2056    0       1       7008
                3080    34736   1       0
                3081    34736   1       1
                3082    34736   1       2
                3083    34736   1       3
                3089    34736   1       4
                3092    34736   1       5
                **3073    34737   29      0
                3072    0       1       32767
                3076    0       1       9001
*/
	// GeoDoubleParamsTag
		out.writeShort(34736);
		out.writeShort( DOUBLE );
		out.writeInt( 5 );
		out.writeInt( address );

		out.writeInt(0);

		dout.writeDouble(0.);
		dout.writeDouble(0.);
		dout.writeDouble(0.);
		dout.writeDouble(0.);
		dout.writeDouble(1.);

		dout.flush();

		address += 8*5;
		byte[] buf = bout.toByteArray();
		int address0 = address - (8+bufferLength + nEntry*12 + 6);
	System.out.println( address0 +"\t"+ buf.length );
		out.write( buf);
		out.close();
	}

	public static void writeTiffUTM( Grid2D grid, File file, boolean trans ) throws IOException {
		boolean isImage = grid instanceof Grid2D.Image;
		Grid2D.Image image = isImage
			? (Grid2D.Image)grid
			: null;
		Rectangle bounds = grid.getBounds();
/*
		boolean trans = false;
		if( isImage ) {
			for( int y=bounds.y ; y<bounds.y+bounds.height ; y++) {
				for( int x=bounds.x ; x<bounds.x+bounds.width ; x++) {
					int rgb = image.rgbValue(x, y);
					if( (rgb&0xff000000) != 0xff000000 ) {
						trans = true;
						break;
					}
				}
				if( trans )break;
			}
		}
*/
		int bytesPerSample = isImage
			? (trans ? 4 : 3)
			: 4;
System.out.println(trans);
		org.geomapapp.geom.UTMProjection proj = null;
		try {
			proj = (org.geomapapp.geom.UTMProjection) grid.getProjection();
		} catch(ClassCastException e) {
			throw new IOException( "Projection must be UTMProjection" );
		}

		int bufferLength = bytesPerSample * bounds.width*bounds.height;
		boolean odd = (bufferLength&1) == 1;
		if( odd ) bufferLength++;
		DataOutputStream out = new DataOutputStream(
			new BufferedOutputStream(
			new FileOutputStream( file )));
		out.writeShort(0x4d4d);
		out.writeShort( (short)42);
		out.writeInt( 8+bufferLength );
		for( int y=bounds.y ; y<bounds.y+bounds.height ; y++) {
			for( int x=bounds.x ; x<bounds.x+bounds.width ; x++) {
				if( isImage ) {
					int rgb = image.rgbValue(x, y);
					out.write( (rgb>>16)&255 );
					out.write( (rgb>>8)&255 );
					out.write( rgb&255 );
					if( trans ) out.write( (rgb>>24)&255 );
				} else {
					float val = (float)grid.valueAt(x,y);
					out.writeFloat( val );
				}
			}
		}
		if(odd) out.write(0);
		int rowsPerStrip = 1;
		while( bytesPerSample*bounds.width*rowsPerStrip < 8192 ) rowsPerStrip++;
		int nStrip = bounds.height/rowsPerStrip;
		if( nStrip*rowsPerStrip<bounds.height ) nStrip++;

		double mPerNode = proj.getScaleXY()[0];
		
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream( bout );

		int nEntry = 19;
		if( isImage ) nEntry--;
		if( trans ) nEntry++;
		int address = 8+bufferLength + nEntry*12 + 6;

		out.writeShort( nEntry );
	// width (ImageWidth)
		out.writeShort( (short)256 );
		out.writeShort( LONG );
		out.writeInt( 1 );
		out.writeInt( bounds.width );
	// height (ImageLength)
		out.writeShort( (short)257 );
		out.writeShort( LONG );
		out.writeInt( 1 );
		out.writeInt( bounds.height );
	// BitsPerSample 
		out.writeShort( (short)258 );
		out.writeShort( SHORT );
		if( isImage ) {
			out.writeInt( trans ? 4 : 3 );
			out.writeInt( address );
			dout.writeShort( 8 );
			dout.writeShort( 8 );
			dout.writeShort( 8 );
			if( trans )dout.writeShort( 8 );
			address += 6;
			if( trans ) address += 2;
		} else {
			out.writeInt( 1 );
			out.writeShort( (short)(bytesPerSample*8) );
			out.writeShort((short)0);
		}
	// Compression 
		out.writeShort( (short)259 );
		out.writeShort( SHORT );
		out.writeInt( 1 );
		out.writeShort( (short)1 );	// no compression
		out.writeShort((short)0);
	// PhotometricInterpretation
		out.writeShort( (short)262 );
		out.writeShort( SHORT );
		int photo = isImage ? 2 : 1;
		out.writeInt(1);
		out.writeShort( photo );
		out.writeShort((short)0);
	// StripOffsets
		out.writeShort( (short)273 );
		out.writeShort( LONG );
		out.writeInt( nStrip );
		out.writeInt( address );
		address += nStrip*4;
		int offset = 8;
		for( int k=0 ; k<nStrip ; k++) {
			dout.writeInt( offset );
			offset += bounds.width*rowsPerStrip*bytesPerSample;
		}
	// Orientation
		out.writeShort( (short)274 );
		out.writeShort( SHORT );
		out.writeInt( 1 );
		out.writeShort( 1 );
		out.writeShort((short)0);
	// SamplesPerPixel
		out.writeShort( (short)277 );
		out.writeShort( SHORT );
		int samples = isImage ? (trans ? 4 : 3) : 1;
		out.writeInt( 1 );
		out.writeShort( samples );
		out.writeShort((short)0);
	// RowsPerStrip
		out.writeShort( (short)278 );
		out.writeShort( LONG );
		out.writeInt( 1 );
		out.writeInt( rowsPerStrip );
	// StripByteCounts
		out.writeShort( (short)279 );
		out.writeShort( LONG );
		out.writeInt( nStrip );
		out.writeInt( address );
		address += nStrip*4;
		int len = rowsPerStrip*bytesPerSample*bounds.width;
		for( int k=0 ; k<nStrip-1 ; k++) {
			dout.writeInt( len );
		}
		int extra = bytesPerSample*bounds.width * (bounds.height - (nStrip-1)*rowsPerStrip);
		dout.writeInt( extra );

		int resolution = (int)( Math.max( bounds.width, bounds.height)/4. );
	// XResolution
		out.writeShort( (short)282 );
		out.writeShort( RATIONAL );
		out.writeInt( 1 );
		out.writeInt( address );
		dout.writeInt(resolution);
		dout.writeInt(1);
		address += 8;
	// YResolution
		out.writeShort( (short)283 );
		out.writeShort( RATIONAL );
		out.writeInt( 1 );
		out.writeInt( address );
		dout.writeInt(resolution);
		dout.writeInt(1);
		address += 8;
	// PlanarConfiguration (probably not necessary)
		out.writeShort( (short)284 );
		out.writeShort( SHORT );
		out.writeInt( 1 );
		out.writeShort( 1 );
		out.writeShort((short)0);
	// ResolutionUnit
		out.writeShort( (short)296 );
		out.writeShort( SHORT );
		out.writeInt( 1 );
		out.writeShort( 2 );	// Inches
		out.writeShort((short)0);
	// Extra Samples
		if( trans ) {
			out.writeShort( (short)338 );
			out.writeShort( SHORT );
			out.writeInt( 1 );
			out.writeShort( (short)2 );
			out.writeShort((short)0);
		}
	// SampleFormat
	if( !isImage ) {
		out.writeShort( (short)339 );
		out.writeShort( SHORT );
		out.writeInt( 1 );
		int fmt = isImage ? 1 : 3;
		out.writeShort( fmt );
		out.writeShort((short)0);
	}
	// GeoTIFF ModelPixelScaleTag
		out.writeShort( 33550 );
		out.writeShort( DOUBLE );
		out.writeInt( 3 );
		out.writeInt(address);
		dout.writeDouble( mPerNode );
		dout.writeDouble( mPerNode );
		dout.writeDouble( 0. );
		address += 3*8;
	// GeoTIFF ModelTiepointTag
		out.writeShort( 33922 );
		out.writeShort( DOUBLE );
		out.writeInt( 6 );
		out.writeInt(address);
		dout.writeDouble( 0. );
		dout.writeDouble( 0. );
		dout.writeDouble( 0. );
		double[] xy0 = proj.getOriginUTM();
		dout.writeDouble( xy0[0] );
		dout.writeDouble( xy0[1] );
		dout.writeDouble( 0. );
		address += 6*8;
	// GeoTIFF GeoKeyDirectoryTag
		out.writeShort( 34735 );
		out.writeShort( SHORT );
		out.writeInt( 20 );
		out.writeInt( address );
		address +=2*4*5;
	//	address +=2*56;
	// GeoTIFF header
		dout.writeShort( 1 );
		dout.writeShort( 1 );
		dout.writeShort( 2 );
		dout.writeShort( 4 );
	// GTModelTypeGeoKey
		dout.writeShort( 1024 );
		dout.writeShort( 0 );
		dout.writeShort( 1 );
		dout.writeShort( 1 );
	// GTRasterTypeGeoKey
		dout.writeShort( 1025 );
		dout.writeShort( 0 );
		dout.writeShort( 1 );
		int rasType = isImage ? 1 : 2;
		dout.writeShort( rasType );		// 2 (RasterPixelIsPoint) for grids, 1 for images
	// ProjectionGeoKey
		dout.writeShort( 3072 );
		dout.writeShort( 0 );
		dout.writeShort( 1 );
		int zone = 32600 + proj.getUTM().getZone()
			+ (proj.getUTM().getHemisphere()==proj.NORTH ? 0 : 100);
		dout.writeShort( zone );	
	// ProjLinearUnitsGeoKey
		dout.writeShort( 3076 );
		dout.writeShort( 0 );
		dout.writeShort( 1 );
		dout.writeShort( 9001 );
	// GeoDoubleParamsTag
		out.writeShort(34736);
		out.writeShort( DOUBLE );
		out.writeInt( 5 );
		out.writeInt( address );

		out.writeInt(0);

		dout.writeDouble(0.);
		dout.writeDouble(0.);
		dout.writeDouble(0.);
		dout.writeDouble(0.);
		dout.writeDouble(1.);

		dout.flush();

		address += 8*5;
		byte[] buf = bout.toByteArray();
		int address0 = address - (8+bufferLength + nEntry*12 + 6);
	System.out.println( address0 +"\t"+ buf.length );
		out.write( buf);
		out.close();
	}

	public static void main(String[] args) {
		JFileChooser c = new JFileChooser(System.getProperty("user.dir"));
		javax.swing.filechooser.FileFilter f = 
			new javax.swing.filechooser.FileFilter() {
				public boolean accept(File f) {
					if( f.isDirectory()) return true;
					String name = f.getName().toLowerCase();
					return name.endsWith(".tif")||name.endsWith(".tiff");
				}
				public String getDescription() {
					return "TIFF files";
				}
			};
		c.addChoosableFileFilter(f);
		int ok = c.showOpenDialog(null);
		if( ok==c.CANCEL_OPTION ) System.exit(0);
		File file = c.getSelectedFile();
		System.out.println( file.getName() );
		TIFF tif = new TIFF( file );
		try {
			tif.read();
		} catch(IOException e ) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}

package haxby.util;

import java.io.*;

public class SwapIO {
	public static float readFloat( InputStream in ) throws IOException {
		int val = 0;
		for( int k=0 ; k<4 ; k++ ) {
			int i= in.read()&0xff;
			val |= i<<(8*k);
		}
		return Float.intBitsToFloat(val);
	}
	public static double readDouble( InputStream in ) throws IOException {
		long val = 0;
		for( int k=0 ; k<8 ; k++ ) {
			long i= (long)(in.read()&0xff);
			val |= i<<(8*k);
		}
		return Double.longBitsToDouble(val);
	}
	public static short readShort( InputStream in ) throws IOException {
		short val = 0;
		for( int k=0 ; k<2 ; k++ ) {
			int i = in.read()&0xff;
			val |= (short)(i<<(8*k));
		}
		return val;
	}
	public static int readInt( InputStream in ) throws IOException {
		int val = 0;
		for( int k=0 ; k<4 ; k++ ) {
			int i = in.read()&0xff;
			val |= i<<(8*k);
		}
		return val;
	}
	public static long readLong( InputStream in ) throws IOException {
		long val = 0;
		for( int k=0 ; k<8 ; k++ ) {
			long i= (long)(in.read()&0xff);
			val |= i<<(8*k);
		}
		return val;
	}
	public static void writeLong( long val, OutputStream out ) throws IOException {
		for( int k=0 ; k<8 ; k++) {
			out.write( (int)val );
			val>>=8;
		}
	}
	public static void writeDouble( double f, OutputStream out ) throws IOException {
		writeLong( Double.doubleToLongBits(f), out);
	}
	public static void writeInt( int val, OutputStream out ) throws IOException {
		for( int k=0 ; k<4 ; k++) {
			out.write( val );
			val>>=8;
		}
	}
	public static void writeFloat( float f, OutputStream out ) throws IOException {
		writeInt(Float.floatToIntBits(f), out);
	}
	public static void writeShort( short val, OutputStream out ) throws IOException {
		for( int k=0 ; k<2 ; k++) {
			out.write( (int)val );
			val>>=8;
		}
	}
}

package org.geomapapp.grid;

import java.io.*;

public class XgrdIO {
	public final static short NODATA = -32768;
	static int[] slopeCode = null;
	static int[] code = null;
	static boolean[] isCode = null;

	public static byte[] encode(short[] ih) throws IOException {
		if( code==null ) codeInit();
		ByteArrayOutputStream bOut = new ByteArrayOutputStream(2048);
		DataOutputStream out = new DataOutputStream(bOut);
	
		int ix, ix1, ix2;
		int nx = ih.length;
		ix1 = 0;
		while(ix1 < nx) {
			while(ix1<nx && ih[ix1] == NODATA) ix1++;
			if(ix1 == nx) break;
			ix2 = ix1;
			while(ix2<nx && ih[ix2] != NODATA) ix2++;
			ix2--;
			encode1(ih,ix1,ix2,out);
			ix1 = ix2+1;
		}
		return bOut.toByteArray();
	}
	static void encode1(short[] ih, int ix1, int ix2, DataOutputStream out) throws IOException {
		int jx1 = ix1+1;
		int jx2 = ix2+1;
		if(jx1 > 32767) {
			out.writeByte((jx1>>16) | 0x80 );
			out.writeShort(jx1 & 0xffff);
		} else {
			out.writeShort(jx1);
		} 
		if(jx2 > 32767) {
			out.writeByte((jx2>>16) | 0x80 );
			out.writeShort(jx2 & 0xffff);
		} else {
			out.writeShort(jx2);
		} 
		out.writeShort((int)ih[ix1]);

		int ix = ix1;
		int jx, dh;
		while(ix<ix2) {
			jx = ix+1;
			while(jx<ix2 && ih[jx] == ih[ix]) jx++;
			int n = jx-ix-1;
			if(n <= 1) {
				ix++;
				dh = (int)ih[ix] - (int)ih[ix-1];
				if(dh >= -100 && dh <100) {
					out.writeByte(dh+100);
				} else if(Math.abs(dh) > 6499) {
					out.writeByte(252);
					out.writeShort(ih[ix]);
				} else {
					out.writeShort(code[dh+6500]);
				}
			} else if(n > 32767) {
				out.writeByte(253);
				out.writeByte(n>>16);
				out.writeShort(n & 0xffff);
				ix += n;
			} else if(n > 256) {
				out.writeByte(251);
				out.writeShort(n);
				ix += n;
			} else {
				out.writeByte(250);
				out.writeByte(n-1);
				ix += n;
			}
		}
	}

	public static short[] decode(byte[] h, int nx) throws IOException {
		if( code==null ) codeInit();
		short[] ih = new short[nx];
		int ix1,ix2,ix,slope,b;
		ix=0;
		DataInputStream in = new DataInputStream(
				new ByteArrayInputStream(h));
		while(ix < nx) {
			try {
				b = in.readUnsignedByte();	
			} catch (EOFException e) {
				for(int i=ix ; i<nx ; i++)ih[i] = NODATA;
				return ih;
			}
			if(b > 127) {
				ix1 = ((b & 0x7f ) << 16) | in.readUnsignedShort();
			} else {
				ix1 = (b << 8) | in.readUnsignedByte();
			}
			b = in.readUnsignedByte();	
			if(b > 127) {
				ix2 = ((b & 0x7f ) << 16) | in.readUnsignedShort();
			} else {
				ix2 = (b << 8) | in.readUnsignedByte();
			}
			ix1--;
			ix2--;
			while(ix<ix1) ih[ix++] = NODATA;
			ih[ix++] = in.readShort();
			while(ix<=ix2) {
				b = in.readUnsignedByte();
				short ih0 = ih[ix-1];
				if(b<200) {
					ih[ix++] = (short) ( (int)ih0 + slopeCode[b] );
				} else if(b < 250) {
					ih[ix++] = (short) ( (int)ih0 + slopeCode[b]
						+ in.readUnsignedByte());
				} else if(b == 250) {
					int n = in.readUnsignedByte()+1;
					for(int i=0 ; i<n ; i++)ih[ix++] = ih0;
				} else if(b == 253) {
					int n = (in.readUnsignedByte()<<16) | in.readUnsignedShort();
					for(int i=0 ; i<n ; i++)ih[ix++] = ih0;
				} else if(b == 251) {
					int n = in.readUnsignedShort();
					for(int i=0 ; i<n ; i++)ih[ix++] = ih0;
				} else if ( b == 252 ) {
					ih[ix++] = in.readShort();
				}
			}
		}
		return ih;
	}
	static void codeInit () {
		if( code!=null ) return;
		slopeCode = new int[256];
		isCode = new boolean[256];
		int i, i1, i2;
		for ( i=0 ; i<200 ; i++ ) {
			slopeCode[i] = i-100;
			isCode[i] = false;
		}
		for ( i=200 ; i<225 ; i++ ) {
			slopeCode[i] = -100 - (225-i)  * 256;
			isCode[i] = true;
		}
		for ( i=225 ; i<250 ; i++ ) {
			slopeCode[i] = 100 + (i-225) * 256;
			isCode[i] = true;
		}
		for ( i=250 ; i<256 ; i++ ) {
			isCode[i] = true;
		}
		code = new int[13001];
		for( i1=0 ; i1<250 ; i1++) {
			if(isCode[i1]) {
				for( i2=0 ; i2<256 ; i2++) {
					i = 6500 + slopeCode[i1] + i2;
					code[i] = i1<<8 | i2;
				}
			}
		}
	}
}

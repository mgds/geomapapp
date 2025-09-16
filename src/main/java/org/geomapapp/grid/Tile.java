package org.geomapapp.grid;

import java.io.*;

class Tile {
	public int res, x, y;
	public byte[] jpeg=null;
	public Tile(int res, int x, int y, InputStream in, int length) throws IOException {
		this.res = res;
		this.x = x;
		this.y = y;
		DataInputStream input = new DataInputStream(
					new BufferedInputStream(in));
		java.util.Vector vec = new java.util.Vector(1, 1);
		byte[] code = new byte[10000];
		int i=0;
		int len;
		length = 0;
		while( (len=input.read(code, i, 10000-i)) != -1) {
			i+=len;
			length += len;
			if( i==10000 ) {
				vec.add(code);
				code = new byte[10000];
				i=0;
			}
		}
		jpeg = new byte[length];
		for(int k=0 ; k<vec.size(); k++) {
			System.arraycopy((byte[])vec.get(k), 0,
					jpeg, k*10000, 10000);
		}
		System.arraycopy(code,0,jpeg,10000*vec.size(),i);
	}
}

package haxby.grid;

import haxby.proj.Projection;
import java.io.*;
import java.net.*;

public class MaskTile implements Serializable {
	byte[] mask;
	Projection proj;
	int x0, y0, size;
	int[] bounds;
	File dir;
	URL url;
	double[] center;
	boolean hasData;
	public MaskTile(int x0, int y0, 
			int size, 
			File dir,
			Projection proj) {
		this.proj = proj;
		this.x0 = x0;
		this.y0 = y0;
		this.size = size;
		mask = new byte[size*size];
		bounds = new int[] { x0*size, y0*size, (x0+1)*size, (y0+1)*size };
		center = new double[] { .5*(double)(bounds[0]+bounds[2]),
					.5*(double)(bounds[1]+bounds[3]) };
		this.dir = dir;
		File file = new File(dir, fileName());
		url = null;
		if( file.exists() ) {
			try {
				readMask();
				hasData = true;
			} catch( IOException ex) {
				hasData = false;
				for( int k=0 ; k<size*size ; k++) mask[k] = (byte)0;
			}
		} else {
			hasData = false;
			for( int k=0 ; k<size*size ; k++) mask[k] = (byte)0;
		}
	}
	public MaskTile(int x0, int y0, 
			int size, 
			URL url,
			Projection proj) {
		this.proj = proj;
		this.x0 = x0;
		this.y0 = y0;
		this.size = size;
		mask = new byte[size*size];
		bounds = new int[] { x0*size, y0*size, (x0+1)*size, (y0+1)*size };
		center = new double[] { .5*(double)(bounds[0]+bounds[2]),
					.5*(double)(bounds[1]+bounds[3]) };
		this.dir = null;
		this.url = url;
		try {
			readMask();
			hasData=true;
		} catch( IOException ex) {
			hasData = false;
			for( int k=0 ; k<size*size ; k++) mask[k] = (byte)0;
		}
	}
	public String fileName() {
		return ( (x0>=0) ? "E"+x0 : "W"+(-x0) )
			+ ( (y0>=0) ? "N"+y0 : "S"+(-y0) )
			+ "_" + size + ".mask";
	}
	public int[] getBounds() {
		return new int[] { x0*size, y0*size, (x0+1)*size, (y0+1)*size };
	}
	public int getMaskSize() {
		return size;
	}
	public byte[] getMask() { return mask; }
	public int indexOf(int x, int y) {
		if( !contains( x, y) )return -1;
		return x-bounds[0] + (y-bounds[1])*size;
	}
	public boolean contains(int x, int y) {
		return (x>=bounds[0] && y>=bounds[1] && x<bounds[2] && y<bounds[3]);
	}
	public double distanceSq(int x, int y) {
		double dx = (double)x - center[0];
		double dy = (double)y - center[1];
		return dx*dx+dy*dy;
	}
	public void setMask(int x, int y, int val) {
		int i = indexOf(x, y);
		if(i==-1)throw new ArrayIndexOutOfBoundsException();
		mask[i] = (byte)val;
	}
	public void setBitMask(int x, int y, int val, int bitmask) {
		int i = indexOf(x, y);
		if(i==-1)throw new ArrayIndexOutOfBoundsException();
		mask[i] = (byte)( ((int)mask[i] & (~bitmask)) | (val&bitmask) );
	}
	public int getZ( int x, int y) {
		int i=indexOf(x, y);
		if(i==-1) return 0;
		return (int)mask[i];
	}
	public void writeMask() throws IOException {
		int n=0;
		int i;
		for( i=0 ; i<mask.length ; i++) {
			if( mask[i]!=0 ) {
				n++;
				break;
			}
		}
		if( n==0 ) {
			return;
		}
		File file = new File(dir, fileName());
		if( !file.exists() ) file.createNewFile();
		DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(
				new FileOutputStream(file)));
		i=0;
		while(i<mask.length) {
			n=1;
			byte test = mask[i];
			i++;
			while( i<mask.length && mask[i]==test ) {
				n++;
				i++;
			}
			n = n|( (int)test <<24 );
			out.writeInt(n);
		}
		out.close();
	}
	void readMask() throws IOException {
		DataInputStream in = null;
		if( url==null ) {
			in = new DataInputStream(
				new BufferedInputStream(
				new FileInputStream(new File(dir, fileName()))));
		} else {
			in = new DataInputStream( url.openStream() );
		}
		int i=0;
		int n;
		while( i<mask.length ) {
			n = in.readInt();
			byte tmp = (byte) (n>>24);
			n &= 0xffffff;
			for( int k=0 ; k<n ; k++,i++) {
				mask[i] = tmp;
			}
		}
	}
}

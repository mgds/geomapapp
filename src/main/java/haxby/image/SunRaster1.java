package haxby.image;

import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.util.*;
import java.util.zip.*;

public class SunRaster1 extends JComponent {
	BufferedImage image;
	int width, height;
	static int MAGIC = 1504078485;
	static int[] BITS = {0x80, 0x40, 0x20, 0x10, 0x8, 0x4, 0x2, 0x1};
	static IndexColorModel lut = defaultLUT();
	static IndexColorModel lut0 = grayLUT();
	DataBufferByte buffer;
	int stride;
	DataBufferByte tmpBuffer=null;

	public SunRaster1(InputStream in, boolean zipped)
				throws IOException {
		DataInputStream din=null;
		if(zipped) {
			System.out.println("compressed");
			din = new DataInputStream(
				new BufferedInputStream(
				new GZIPInputStream(in)));
		} else {
			din = new DataInputStream(
				new BufferedInputStream(in));
		}
		if( din.readInt() != MAGIC) {
			din.close();
			throw new IOException("not a sunraster file");
		}
		int w = din.readInt();
		int h = din.readInt();
		if( din.readInt() != 1) {
			din.close();
			throw new IOException("not a 1-bit sunraster file");
		}
		int size = din.readInt();
		w = size/h;
		for(int i=0 ; i<3 ; i++) din.readInt();
		int w8 = (w+7)/8;
		int ht = h;
		if(h%2==1) ht++;
		byte[] bits = new byte[w8*ht];
		for( int k=0 ; k<h*w8 ; k++ ) {
			bits[k] = (byte) din.readByte();
		}
		din.close();
		
		setImage(bits, w, ht);
	}
	public void setImage( byte[] bitmap, int width, int height ) {
		buffer = new DataBufferByte(bitmap,bitmap.length);
		WritableRaster raster = Raster.createPackedRaster(buffer, 
					width, height, 1, new Point(0,0));
		image = new BufferedImage(lut, raster, false, new Hashtable());
		this.width = width;
		this.height = height;
	}
	public static IndexColorModel grayLUT() {
		byte[] gray = new byte[256];
		for( int i=0 ; i<256 ; i++) gray[i] = (byte)(255-i);
		return new IndexColorModel(8, 256, gray, gray, gray);
	}
	public static IndexColorModel defaultLUT() {
		byte[] gray = new byte[] {-1, 0};
		return new IndexColorModel(1, 2, gray, gray, gray);
	}
	public BufferedImage getImage() { return image; }
	public Dimension getPreferredSize() {
		Dimension size = new Dimension(width, height);
		return size;
	}
	public void paint(Graphics g) {
		g.drawImage( image, 0, 0, this);
	}
}

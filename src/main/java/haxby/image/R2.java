package haxby.image;

import java.io.*;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.util.*;
import java.util.zip.*;

public class R2 implements ScalableImage {
	BufferedImage image;
	int width, height;
	public final static int MAGIC = 1504078485;
	static int[] BITS = {0x80, 0x40, 0x20, 0x10, 0x8, 0x4, 0x2, 0x1};
	static IndexColorModel lut2 = defaultLUT(false);
	static IndexColorModel lut0 = grayLUT(false);
	DataBufferByte buffer;
	int stride;
	DataBufferByte tmpBuffer=null;
	Rectangle tmpRect = null;
	boolean rev = false;
	boolean flip = false;
	int xR, yR, xA, yA;
	boolean hFlip = false;

	public R2(byte[] bitmap, int width, int height) {
		stride = bitmap.length / height;
		xR = yR = xA = yA = 1;
		rev = false;
		flip = false;
		setImage(bitmap, width, height);
	}
	public R2(InputStream in, boolean zipped)
				throws IOException {
		DataInputStream din=null;
		if(zipped) {
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
		if( din.readInt() != 2) {
			din.close();
			throw new IOException("not a 2-bit sunraster[sic] file");
		}
		int size = din.readInt();
		stride = size/h;
		for(int i=0 ; i<3 ; i++) din.readInt();
		byte[] bitmap = new byte[size];
		try {
			din.readFully(bitmap);
		} catch(IOException ex) {
		}
		din.close();
		xR = yR = xA = yA = 1;
		rev = false;
		flip = false;
		setImage(bitmap, w, h);
	}
	public void saveImage(String file) throws IOException {
		FileOutputStream fout = new FileOutputStream(file);
		GZIPOutputStream gzout = new GZIPOutputStream(fout);
		DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(gzout));
		byte[] buf0 = buffer.getData();
		out.writeInt(1504078485);
		out.writeInt(width);
		out.writeInt(height);
		out.writeInt(2);
		out.writeInt(buf0.length);
		out.writeInt(1);
		out.writeInt(0);
		out.writeInt(0);
		out.write(buf0);
		gzout.finish();
		fout.close();
	}
	public void saveRas(String file) throws IOException {
		FileOutputStream fout = new FileOutputStream(file);
		DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(fout));
		int w = 4*stride;
		out.writeInt(1504078485);
		out.writeInt(w);
		out.writeInt(height);
		out.writeInt(8);
		out.writeInt(w*height);
		out.writeInt(1);
		out.writeInt(1);
		out.writeInt(12);
		byte[] gray = new byte[] {-1, -83, 83, 0};
		for(int i=0 ; i<3 ; i++) {
			out.write(gray);
		}
		byte[] buf0 = buffer.getData();
		int length = 4*stride;
		byte[] line = new byte[length];

		System.out.println(line.length +"\t"+ height +"\t"+ line.length);
		for( int y=0 ; y<stride*height ; y+=stride ) {
			int k=0;
			for( int x=y ; x<y+stride ; x++) {
				line[k++] = (byte)((int)(buf0[x]>>6)&3);
				line[k++] = (byte)((int)(buf0[x]>>4)&3);
				line[k++] = (byte)((int)(buf0[x]>>2)&3);
				line[k++] = (byte)((int)(buf0[x])&3);
			}
			out.write(line);
		}
		fout.close();
	}
	public void savePGM(String file) throws IOException {
		FileOutputStream fout = new FileOutputStream(file);
		GZIPOutputStream gzout = new GZIPOutputStream(fout);
		DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(gzout));
		char[] chars = (new String("P5\n" + width +" "+height +"\n255\n")).toCharArray();
		for(int i=0 ; i<chars.length ; i++) out.writeByte((int)chars[i]);
		byte[] buf0 = buffer.getData();
		byte[] line = new byte[4*stride];
		byte[] gray = new byte[] {-1, -83, 83, 0};

		System.out.println(line.length +"\t"+ height +"\t"+ line.length);
		for( int y=0 ; y<stride*height ; y+=stride ) {
			int k=0;
			for( int x=y ; x<y+stride ; x++) {
				line[k++] = gray[ (int)(buf0[x]>>6)&3 ];
				line[k++] = gray[ (int)(buf0[x]>>4)&3 ];
				line[k++] = gray[ (int)(buf0[x]>>2)&3 ];
				line[k++] = gray[ (int)(buf0[x])&3 ];
			}
			out.write(line);
		}
		gzout.finish();
		fout.close();
	}
	public void setImage( byte[] bitmap, int width, int height ) {
		buffer = new DataBufferByte(bitmap,bitmap.length);
		WritableRaster raster = Raster.createPackedRaster(buffer, 
					width, height, 2, new Point(0,0));
		image = new BufferedImage(lut2, raster, false, new Hashtable());
		this.width = width;
		this.height = height;
	}

	public Dimension getSize() {
		Dimension size = new Dimension(width, height);
		return size;
	}
	public static IndexColorModel grayLUT(boolean reversed) {
		byte[] gray = new byte[256];
		if(reversed) {
			for( int i=0 ; i<256 ; i++) gray[i] = (byte)(i);
			for( int i=0 ; i<256 ; i++) {
				double g = (double) (((int)gray[i])&255);
				gray[i] = (byte)(int)( 255.98*Math.pow( (.01+g)/255.01d , .5d));
			}
		} else {
			for( int i=0 ; i<256 ; i++) gray[i] = (byte)(255-i);
		}
		return new IndexColorModel(8, 256, gray, gray, gray);
	}
	public static IndexColorModel defaultLUT(boolean reversed) {
		byte[] gray;
		if( reversed ) {
			gray = new byte[] {0, 83, -83, -1};
			for( int i=0 ; i<4 ; i++) {
				double g = (double) (((int)gray[i])&255);
				gray[i] = (byte)(int)( 255.98*Math.pow( (.01+g)/255.01d , .5d));
			}
		} else {
			gray = new byte[] {-1, -83, 83, 0};
		}
		return new IndexColorModel(2, 4, gray, gray, gray);
	}
	public BufferedImage getImage() { return image; }
	public Rectangle getImageableRect(Rectangle rect, int xAvg, int yAvg, int xRep, int yRep) {
		Rectangle r = rect.intersection(new Rectangle(0,0,width*xRep/xAvg,height*yRep/yAvg));
		if(flip==hFlip && xA==xAvg && yA== yAvg && xRep==xR && yRep==yR 
				&& tmpRect != null && tmpRect.contains(r))return tmpRect;
		if(r.width<=0 || r.height <=0) return r;
		int x = r.x + r.width;
		int dx = x % (xRep*4);
		if( dx != 0 ) x += xRep*4-dx;
		r.x -= r.x % (xRep*4);
		r.width = x - r.x;
		int y = r.y + r.height;
		int dy = y % yRep;
		if( dy != 0 ) y += yRep-dy;
		r.y -= r.y % yRep;
		r.height = y - r.y;
		return r;
	}
	byte[] getBuffer(Rectangle rect, int xAvg, int yAvg, int xRep, int yRep) {
		int y0 = rect.y * yAvg/yRep;
		int x0 = rect.x * xAvg/xRep;
		int nAvg = xAvg*yAvg;
		int w = rect.width;
		int h = rect.height;
		int x, y, xx, yy, kx, ky;
		int z;
		byte b;
		byte[] buf = new byte[w*h];
		byte[] buf0 = buffer.getData();
		int[] n = new int[4*xAvg];
		for( y=0, yy=y0*stride ; y<h*w ; y+=yRep*w, yy+=stride*yAvg ) {
		try {
			for( x=0, xx=yy+x0/4 ; x<w ; x+=xRep*4, xx+=xAvg ) {
				for( int i=0 ; i<4*xAvg ; i++) n[i]=0;
				for( ky=0 ; ky<yAvg*stride ; ky+=stride ) {
					kx=0;
					for( int i=0 ; i<xAvg ; i++) {
						n[kx++] += (int) (buf0[ky+xx+i]>>6) &3;
						n[kx++] += (int) (buf0[ky+xx+i]>>4) &3;
						n[kx++] += (int) (buf0[ky+xx+i]>>2) &3;
						n[kx++] += (int) (buf0[ky+xx+i]) &3;
					}
				}
				if( xAvg!=1 ) {
					kx=0;
					for( int i=0 ; i<4 ; i++ ) {
						n[i] = n[kx++];
						for( int j=1 ; j<xAvg ; j++) {
							n[i] += n[kx++];
						}
					}
				}
				for( int i=0 ; i<4 ; i++ ) {
					b = (byte) (n[i]*255/nAvg/3);
					for( ky=0 ; ky<w*yRep ; ky+=w ) {
						for( kx = x ; kx<x+xRep ; kx++) {
							buf[i*xRep+y+ky+kx] = b;
						}
					}
				}
			}
		} catch(ArrayIndexOutOfBoundsException e) {}
		}
		return buf;
	}
	byte[] getFlipBuffer(Rectangle rect, int xAvg, int yAvg, int xRep, int yRep) {
		int y0 = rect.y * yAvg/yRep;
		int x0 = width - (rect.x+rect.width) * xAvg/xRep;
		int nAvg = xAvg*yAvg;
		int w = rect.width;
		int h = rect.height;
		int x, y, xx, yy, kx, ky;
		int z;
		byte b;
		byte[] buf = new byte[w*h];
		byte[] buf0 = buffer.getData();
		int[] n = new int[4*xAvg];
		for( y=0, yy=y0*stride ; y<h*w ; y+=yRep*w, yy+=stride*yAvg ) {
		try {
			for( x=w-xRep*4, xx=yy+x0/4 ; x>=0 ; x-=xRep*4, xx+=xAvg ) {
				for( int i=0 ; i<4*xAvg ; i++) n[i]=0;
				for( ky=0 ; ky<yAvg*stride ; ky+=stride ) {
					kx=0;
					for( int i=0 ; i<xAvg ; i++) {
						n[kx++] += (int) (buf0[ky+xx+i]>>6) &3;
						n[kx++] += (int) (buf0[ky+xx+i]>>4) &3;
						n[kx++] += (int) (buf0[ky+xx+i]>>2) &3;
						n[kx++] += (int) (buf0[ky+xx+i]) &3;
					}
				}
				if( xAvg!=1 ) {
					kx=0;
					for( int i=0 ; i<4 ; i++ ) {
						n[i] = n[kx++];
						for( int j=1 ; j<xAvg ; j++) {
							n[i] += n[kx++];
						}
					}
				}
				for( int i=0 ; i<4 ; i++ ) {
					b = (byte) (n[i]*255/nAvg/3);
					for( ky=0 ; ky<w*yRep ; ky+=w ) {
						for( kx = x ; kx<x+xRep ; kx++) {
							buf[(3-i)*xRep+y+ky+kx] = b;
						}
					}
				}
			}
		} catch(ArrayIndexOutOfBoundsException e) {}
		}
		return buf;
	}
	public BufferedImage getScaledImage(Rectangle rect, int xAvg, int yAvg, int xRep, int yRep) {
		byte[] buf;
		if(flip==hFlip && xA==xAvg && yA== yAvg && xRep==xR && yRep==yR 
				&& tmpRect != null && tmpRect.contains(rect)) {
			buf = tmpBuffer.getData();
		} else if(flip) {
			buf = getFlipBuffer(rect, xAvg, yAvg, xRep, yRep);
		} else {
			buf = getBuffer(rect, xAvg, yAvg, xRep, yRep);
		}
		DataBufferByte buff = new DataBufferByte(buf,buf.length);
		WritableRaster raster = Raster.createPackedRaster(buff, 
					rect.width, rect.height, 8, new Point(0,0));
		xR = xRep;
		yR = yRep;
		xA = xAvg;
		yA = yAvg;
		hFlip = flip;
		tmpBuffer = buff;
		tmpRect = rect;

		return new BufferedImage(lut0, raster, false, new Hashtable());
	}
	public void setFlip(boolean tf) {
		if(flip==tf)return;
		flip = tf;
	}
	public boolean isFlip() { return flip; }
	public void setRevVid(boolean tf) {
		if(rev==tf)return;
		rev = tf;
		lut0 = grayLUT(rev);
		lut2 = defaultLUT(rev);
		setImage(buffer.getData(), width, height);
	}
	public static void main( String[] args) {
		int xAvg = 2;
		int yAvg = 2;
		if( args.length==2 ) {
			xAvg = Integer.parseInt(args[0]);
			yAvg = Integer.parseInt(args[1]);
		}
		JFileChooser c = new JFileChooser(System.getProperty("user.dir"));
		while( true ) {
			int ok = c.showOpenDialog(null);
			if( ok==c.CANCEL_OPTION ) System.exit(0);
			try {
				File file = c.getSelectedFile();
				String path = file.getParent();
				String name = file.getName();
				name = name.substring(0, name.indexOf(".r2.gz"))+"_"+xAvg+"_"+yAvg+".jpg";
				R2 r2 = new R2( new FileInputStream(file), true);
				BufferedImage im = r2.getImage();
				int w = im.getWidth();
				int h = im.getHeight();
				im = r2.getScaledImage( new Rectangle(0,0,w/xAvg,h/yAvg), xAvg, yAvg, 1, 1);
				javax.imageio.ImageIO.write( im, "jpg", new File(path, name));
			} catch(Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}
}

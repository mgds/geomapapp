package haxby.image;

import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;

import javax.imageio.ImageIO;

public class JPEGimage implements ScalableImage{
	private static final int[] RGB_MASKS = {0xFF0000, 0xFF00, 0xFF};
	private static final ColorModel RGB_OPAQUE =
		new DirectColorModel(32, RGB_MASKS[0], RGB_MASKS[1], RGB_MASKS[2]);
	BufferedImage image;
	int width, height;
	DataBufferByte buffer;
	int stride;
	DataBufferByte tmpBuffer=null;
	Rectangle tmpRect = null;
	boolean rev = false;
	boolean flip = false;
	int xR, yR, xA, yA;
	boolean hFlip = false;
	
	public JPEGimage(URL url, int width, int height){
		try {
			image = ImageIO.read(url);
			this.width = image.getWidth();
			this.height = image.getHeight();
			stride = ((DataBufferByte)image.getRaster().getDataBuffer()).getData().length / height;
			buffer = (DataBufferByte)image.getRaster().getDataBuffer();
			xR = yR = xA = yA = 1;
			rev = false;
			flip = false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public BufferedImage getImage() {
		return image;
	}

	public Rectangle getImageableRect(Rectangle rect, int xAvg, int yAvg,
			int xRep, int yRep) {
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

	public BufferedImage getScaledImage(Rectangle rect, int xAvg, int yAvg,
			int xRep, int yRep) {
		byte[] buf;
		if(flip==hFlip && xA==xAvg && yA== yAvg && xRep==xR && yRep==yR 
				&& tmpRect != null && tmpRect.contains(rect)) {
			buf = tmpBuffer.getData();
		} else if(flip) {
			buf = getFlipBuffer(rect, xAvg, yAvg, xRep, yRep);
		} else  {
			buf = getBuffer(rect, xAvg, yAvg, xRep, yRep);
		}
		DataBufferByte buff = new DataBufferByte(buf,buf.length);
		
		//PixelGrabber pg = new PixelGrabber(image, rect.x, rect.y,  rect.width, rect.height, true);
		/*try {
			pg.grabPixels();
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		//DataBuffer dBuffer = new DataBufferInt((int[]) pg.getPixels(), rect.width * rect.height);
		//WritableRaster raster = Raster.createPackedRaster(buff, 
		//			rect.width, rect.height, 8, new Point(0,0));
		xR = xRep;
		yR = yRep;
		xA = xAvg;
		yA = yAvg;
		hFlip = flip;
		tmpBuffer = buff;
		tmpRect = rect;

		//return new BufferedImage(RGB_OPAQUE, raster, false, new Hashtable());
		
		Image newImage = image.getScaledInstance(rect.width, rect.height, Image.SCALE_SMOOTH);
		BufferedImage bim =
		new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB);
		bim.createGraphics().drawImage(newImage, 0, 0, null);
		return bim;
	}
	

	private byte[] getBuffer(Rectangle rect, int xAvg, int yAvg, int xRep,
			int yRep) {
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

	private byte[] getFlipBuffer(Rectangle rect, int xAvg, int yAvg, int xRep,
			int yRep) {
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

	public boolean isFlip() {
		return flip;
	}

	public void setFlip(boolean tf) {
		if(flip==tf)return;
		flip = tf;
	}

	public void setRevVid(boolean tf) {
		if(rev==tf)return;
		rev = tf;
		
	}

}

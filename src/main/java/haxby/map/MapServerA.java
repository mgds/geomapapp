package haxby.map;

import haxby.proj.Mercator;
import haxby.proj.Projection;
import haxby.util.URLFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;

//import com.sun.image.codec.jpeg.JPEGCodec;
//import com.sun.image.codec.jpeg.JPEGImageDecoder;
//import com.sun.image.codec.jpeg.JPEGImageEncoder;
public class MapServerA {
	static Mercator proj = new Mercator(0., 0., 360., Projection.SPHERE, Mercator.RANGE_0_to_360);
	static int[] highRes = {600,300,200,120,60,0};
	static int[] res = {30,15,10,5};
	static int[] tileWidth = {600,600,600,600};
	static byte[] red = {100, 0};
	static byte[] green = {100, 0};
	static byte[] blue = {100, 0};
	static byte[] alpha = {-1, 0};
	static int[] bits = {0x80, 0x40, 0x20, 0x10, 0x8, 0x4, 0x2, 0x1};
	static IndexColorModel cm = new IndexColorModel(1, 2, red, green, blue, alpha);
	public static double[] fitWESN(double[] wesn, int width, int height) {
		Rectangle rect = new Rectangle(0, 0, width, height);
		double scale = (wesn[1] - wesn[0]) / (double)width;
		double scaleY = (wesn[3] - wesn[2]) / (double)height;
		double x0, y0;
		if(scaleY > scale) {
			scale = scaleY;
			x0 = .5d * (wesn[0] + wesn[1]);
			wesn[0] = x0 - (double)width * .5d * scale;
			wesn[1] = wesn[0] + (double)width * scale;
		} else {
			y0 = .5d * (wesn[2] + wesn[3]);
			wesn[3] = y0 + (double)height * .5d * scale;
			wesn[2] = wesn[3] - (double)height * scale;
		}
		return wesn;
	}
	public static boolean getHighRes(double[] wesn, int width, int height, BufferedImage image) {
		getImage(wesn, width, height, image);
		Graphics2D g = image.createGraphics();
		Rectangle rect = new Rectangle(0, 0, width, height);
		double scale = (wesn[1] - wesn[0]) / (double)width;
		double r = 1/scale;
		if(r < (double)res[0]) return false;
		int ires = 0;
		while( r < (double)highRes[ires] && ires<highRes.length-1) ires++;
		if(ires == highRes.length-1) ires--;
		double gridScale = 600 / (double) highRes[ires];

		int x1 = (int) Math.floor( wesn[0] / gridScale);
		int x2 = (int) Math.ceil( wesn[1] / gridScale);
		int xmax = (int) Math.ceil( 360 / gridScale);

		int y1 = (int) Math.ceil( wesn[3] / gridScale);
		int y2 = (int) Math.floor( wesn[2] / gridScale);
		Rectangle clipRect = new Rectangle(8, 8, 600, 600);

		int xgrid, ygrid;
		String name;
		File file;
		double x0, y0;
		double west;
		double north;
		double offset;
		double pad = 8 / (double) highRes[ires];
		BufferedImage im = null;
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		AffineTransform at = new AffineTransform();
		boolean returnVal = false;
		for( int x=x1 ; x<x2 ; x++ ) {
			xgrid = x;
			offset = -pad;
			while(xgrid < 0) {
				xgrid += xmax;
				offset -= 360;
			}
			while(xgrid >= xmax) {
				xgrid -= xmax;
				offset += 360;
			}
			west = (double) xgrid * gridScale + offset;
			x0 = (west - wesn[0]) / scale;
			for( int y=y1 ; y>y2 ; y--) {

				north = (double)y *gridScale + pad;
				if( y>0 ) {
					name = "g" + highRes[ires] + "_" + xgrid + "_N" + y +".jpg";
				} else {
					ygrid = -y;
					name = "g" + highRes[ires] + "_" + xgrid + "_S" + ygrid +".jpg";
				}
				String url = haxby.map.MapApp.TEMP_BASE_URL + "tiles/g"+highRes[ires]+"/" + name;
				im = null;
				try { 
					BufferedInputStream in = new BufferedInputStream(
						(URLFactory.url(url)).openStream());
					//JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(in);
					//im = decoder.decodeAsBufferedImage();
					im = ImageIO.read(in);
					in.close();
				} catch ( Exception ioe) {
					continue;
				}
				g.setTransform(at);
				g.setClip(rect);
				y0 = (wesn[3] - north) / scale ;
				g.translate(x0, y0);
				double scl = 1 / ((double) highRes[ires] * scale);
				g.scale(scl, scl);
				g.clip(clipRect);
				returnVal = true;
				g.drawRenderedImage(im, at);
			}
		}
		return returnVal;
	}
	public static boolean get3D(double[] wesn, int width, int height, BufferedImage image) {
		getImage(wesn, width, height, image);
		Graphics2D g = image.createGraphics();
		Rectangle rect = new Rectangle(0, 0, width, height);
		double scale = (wesn[1] - wesn[0]) / (double)width;
		double r = 1/scale;
		if(r < (double)res[0]) return false;
		int ires = 0;
		while( r < (double)highRes[ires] && ires<highRes.length-1) ires++;
		if(ires == highRes.length-1) ires--;
		double gridScale = 600 / (double) highRes[ires];
		
		int x1 = (int) Math.floor( wesn[0] / gridScale);
		int x2 = (int) Math.ceil( wesn[1] / gridScale);
		int xmax = (int) Math.ceil( 360 / gridScale);

		int y1 = (int) Math.ceil( wesn[3] / gridScale);
		int y2 = (int) Math.floor( wesn[2] / gridScale);
		Rectangle clipRect = new Rectangle(8, 8, 600, 600);

		int xgrid, ygrid;
		String name;
		File file;
		double x0, y0;
		double west;
		double north;
		double offset;
		double pad = 8 / (double) highRes[ires];
		BufferedImage im = null;
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		AffineTransform at = new AffineTransform();
		boolean returnVal = false;
		for( int x=x1 ; x<x2 ; x++ ) {
			xgrid = x;
			offset = -pad;
			while(xgrid < 0) {
				xgrid += xmax;
				offset -= 360;
			}
			while(xgrid >= xmax) {
				xgrid -= xmax;
				offset += 360;
			}
			west = (double) xgrid * gridScale + offset;
			x0 = (west - wesn[0]) / scale;
			for( int y=y1 ; y>y2 ; y--) {
				
				north = (double)y *gridScale + pad;
				if( y>0 ) {
					name = "d" + highRes[ires] + "_" + xgrid + "_N" + y +".jpg";
				} else {
					ygrid = -y;
					name = "d" + highRes[ires] + "_" + xgrid + "_S" + ygrid +".jpg";
				}
				String url = haxby.map.MapApp.TEMP_BASE_URL + "stereo/g"+highRes[ires]+"/" + name;
				im = null;
				try { 
					BufferedInputStream in = new BufferedInputStream(
						(URLFactory.url(url)).openStream());
					//JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(in);
					//im = decoder.decodeAsBufferedImage();
					im = ImageIO.read(in);
					in.close();
				} catch ( Exception ioe) {
					if( y>0 ) {
						name = "g" + highRes[ires] + "_" + 
							xgrid + "_N" + y +".jpg";
					} else {
						ygrid = -y;
						name = "g" + highRes[ires] + "_" + 
							xgrid + "_S" + ygrid +".jpg";
					}
					url = haxby.map.MapApp.TEMP_BASE_URL + "tiles/g"+
						highRes[ires]+"/" + name;
					try {
						BufferedInputStream in = new BufferedInputStream(
							(URLFactory.url(url)).openStream());
						//JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(in);
						//im = decoder.decodeAsBufferedImage();
						im = ImageIO.read(in);
						in.close();
					} catch ( Exception ioe2) {
						continue;
					}
				}
				g.setTransform(at);
				g.setClip(rect);
				y0 = (wesn[3] - north) / scale ;
				g.translate(x0, y0);
				double scl = 1 / ((double) highRes[ires] * scale);
				g.scale(scl, scl);
				g.clip(clipRect);
				returnVal = true;
				g.drawRenderedImage(im, at);
			}
		}
		return returnVal;
	}
	public static void getImage(double[] wesn, int width, int height, BufferedImage image) {
		Graphics2D g = image.createGraphics();
		g.setColor(Color.lightGray);
		Rectangle rect = new Rectangle(0, 0, width, height);
		g.fill(rect);
		double scale = (wesn[1] - wesn[0]) / (double)width;
		double scaleY = (wesn[3] - wesn[2]) / (double)height;
		double x0, y0;
		if(scaleY > scale) {
			scale = scaleY;
			x0 = .5d * (wesn[0] + wesn[1]);
			wesn[0] = x0 - (double)width * .5d * scale;
			wesn[1] = wesn[0] + (double)width * scale;
		} else {
			y0 = .5d * (wesn[2] + wesn[3]);
			wesn[3] = y0 + (double)height * .5d * scale;
			wesn[2] = wesn[3] - (double)height * scale;
		}
		double r = 1/scale;
		int ires = 0;
		while( r < (double)res[ires] && ires<res.length-1) ires++;

		double gridScale = (double) tileWidth[ires] / (double) res[ires];

		int x1 = (int) Math.floor( wesn[0] / gridScale);
		int x2 = (int) Math.ceil( wesn[1] / gridScale);
		int xmax = (int) Math.ceil( 360 / gridScale);

		int y1 = (int) Math.ceil( wesn[3] / gridScale);
		int y2 = (int) Math.floor( wesn[2] / gridScale);
		Rectangle clipRect = new Rectangle(8, 8, tileWidth[ires], tileWidth[ires]);

		int xgrid, ygrid;
		String name;
		File file;
		double west;
		double north;
		double offset;
		double pad = 8 / (double) res[ires];
		BufferedImage im = null;
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		AffineTransform at = new AffineTransform();
		for( int x=x1 ; x<x2 ; x++ ) {
			xgrid = x;
			offset = -pad;
			while(xgrid < 0) {
				xgrid += xmax;
				offset -= 360;
			}
			while(xgrid >= xmax) {
				xgrid -= xmax;
				offset += 360;
			}
			west = (double) xgrid * gridScale + offset;
			x0 = (west - wesn[0]) / scale;
			for( int y=y1 ; y>y2 ; y--) {

				north = (double)y *gridScale + pad;
				if( y>0 ) {
					name = "n" + res[ires] + "_" + xgrid + "_N" + y +".jpg";
				} else {
					ygrid = -y;
					name = "n" + res[ires] + "_" + xgrid + "_S" + ygrid +".jpg";
				}
				String url = haxby.map.MapApp.TEMP_BASE_URL + "tiles/g"+res[ires]+"/" + name;
				im = null;
				try { 
					BufferedInputStream in = new BufferedInputStream(
						(URLFactory.url(url)).openStream());
					//JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(in);
					//im = decoder.decodeAsBufferedImage();
					im = ImageIO.read(in);
					in.close();
				} catch ( Exception ioe) {
					continue;
				}
				g.setTransform(at);
				g.setClip(rect);
				y0 = (wesn[3] - north) / scale;
				g.translate(x0, y0);
				double scl = 1 / ((double) res[ires] * scale);
				g.scale(scl, scl);
				g.clip(clipRect);
				g.drawRenderedImage(im, at);
			}
		}
	//	return image;
	}
	public static BufferedImage getMask(double[] wesn, int width, int height) {
		int w = 1 + (width+7) / 8;
		int n = w*height;
	//	byte[] msk = new byte[n];
	//	for(int i=0 ; i<n ; i++ ) msk[i] = 0;
	//	DataBufferByte buf = new DataBufferByte(msk, n);
	//	WritableRaster ras = Raster.createPackedRaster(buf,
	//			width, height, 1, new Point(0,0));
	//	BufferedImage image = new BufferedImage(cm, ras, false, new Hashtable());
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Rectangle rect = new Rectangle(0, 0, width, height);

		double scale = (wesn[1] - wesn[0]) / (double)width;
		double r = 1/scale;
		int ires = 0;
		while( r < (double)highRes[ires] && ires<highRes.length-1) ires++;
		int kres = highRes[ires];
		if(ires == highRes.length-1) {
			ires = 0;
			while( r < (double)res[ires] && ires<res.length-1) ires++;
			kres = res[ires];
		}
		double gridScale = 600 / (double) kres;

		int x1 = (int) Math.floor( wesn[0] / gridScale);
		int x2 = (int) Math.ceil( wesn[1] / gridScale);
		int xmax = (int) Math.ceil( 360 / gridScale);

		int y1 = (int) Math.ceil( wesn[3] / gridScale);
		int y2 = (int) Math.floor( wesn[2] / gridScale);
		Rectangle clipRect = new Rectangle(0, 0, 600, 600);

		int xgrid, ygrid;
		String name;
		File file;
		double x0, y0;
		double west;
		double north;
		double offset;
		BufferedImage im = null;
		DataBufferByte buffer;
		WritableRaster raster;
		AffineTransform at = new AffineTransform();
		byte[] mask;
		byte[] mask0 = new byte[600*75];
		for(int i=0 ; i<600*75 ; i++ ) mask0[i] = 0;
		for( int x=x1 ; x<x2 ; x++ ) {
			xgrid = x;
			offset = 0;
			while(xgrid < 0) {
				xgrid += xmax;
				offset -= 360;
			}
			while(xgrid >= xmax) {
				xgrid -= xmax;
				offset += 360;
			}
			west = (double) xgrid * gridScale + offset;
			x0 = (west - wesn[0]) / scale;
			for( int y=y1 ; y>y2 ; y--) {
				north = (double)y *gridScale;
				if( y>0 ) {
					name = "m" + kres + "_" + xgrid + "_N" + y +".gz";
				} else {
					ygrid = -y;
					name = "m" + kres + "_" + xgrid + "_S" + ygrid +".gz";
				}
				String url = haxby.map.MapApp.TEMP_BASE_URL + "tiles/m"+kres+"/" + name;
				im = null;
				mask = new byte[600*75];
				try { 
					DataInputStream in = new DataInputStream(new GZIPInputStream(
						(URLFactory.url(url)).openStream()));
					in.readFully(mask);
					in.close();
				} catch ( Exception ioe) {
					mask = mask0;
				}
				buffer = new DataBufferByte(mask, 600*75);
				raster = Raster.createPackedRaster(buffer,
						600, 600, 1, new Point(0,0));
				im = new BufferedImage(cm, raster, false, new Hashtable());
				g.setTransform(at);
				g.setClip(rect);
				y0 = (wesn[3] - north) / scale ;
				g.translate(x0, y0);
				double scl = 1 / ((double) kres * scale);
				g.scale(scl, scl);
				g.clip(clipRect);
				g.drawRenderedImage(im, at);
			}
		}
	//	mask = new byte[n];
	//	for(int i=0 ; i<n ; i++) mask[i] = 0;
	//	int k = 0;
	//	System.out.println(width +"\t"+ height +"\t"+ w +"\t"+ n);
	//	for(int y=0 ; y<height ; y++) {
	//		for(int x=0 ; x<width ; x++) {
	//			if(image.getRGB(x,y) == 0) {
	//				System.out.println(x +"\t"+ y +"\t"+ k +"\t"+ (k>>3) +"\t"+ (k&8));
	//				mask[k>>3] |= bits[k&7];
	//			}
	//			k++;
	//		}
	//		while((k&7) != 0) k++;
	//	}
	//	buffer = new DataBufferByte(mask, n);
	//	raster = Raster.createPackedRaster(buffer, width, height, 1, new Point(0,0));
	//	im = new BufferedImage(cm, raster, false, new Hashtable());
		return image;
	}
	public static byte[] printImage(BufferedImage image) 
			throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		//JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
		//encoder.encode(image);
		ImageIO.write(image, "JPEG", out);
		return out.toByteArray();
	}
	public static void main(String[] args) {
		System.out.println("tiling");
		if(args.length != 5) {
			System.out.println("usage: java MapServerA west east south north 1(low)or2(high)");
			System.exit(0);
		} else {
			double[] wesn = new double[4];
			try {
				for(int i=0 ; i<4 ; i++) {
					wesn[i] = Double.parseDouble(args[i]);
	System.err.println(args[i]);
					if(i>1) wesn[i] = proj.getY(wesn[i]);
				}
				int high = Integer.parseInt(args[4]);
				wesn = fitWESN(wesn,720, 540);
	System.err.println(wesn[0] +"\t"+ wesn[1] +"\t"+ wesn[2] +"\t"+ wesn[3]);
				BufferedImage image = new BufferedImage(720, 540, BufferedImage.TYPE_INT_RGB);
				if(high == 1) {
					getImage(wesn,720, 540, image);
				} else {
					getHighRes(wesn,720, 540,image);
				}
				byte[] data = printImage(image);
				System.out.println(data.length +" bytes");
	//			System.out.write(data, 0, data.length);
				FileOutputStream out = new FileOutputStream("junk");
				out.write(data, 0, data.length);
				out.close();
			} catch (Exception e) {
	//			System.out.println("error: "+e.getMessage());
	//			e.printStackTrace();
			}
		}
	//	try {
	//		Thread.currentThread().sleep(4000L);
	//	} catch (Exception e) {
	//	}
		System.exit(0);
	}
}
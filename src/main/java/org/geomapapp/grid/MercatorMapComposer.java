package org.geomapapp.grid;

import haxby.util.URLFactory;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.geomapapp.geom.CylindricalProjection;
import org.geomapapp.geom.MapProjection;
import org.geomapapp.geom.Mercator;

/*
 * Not used in GMA as of 6/10/09 JOC
 */
public class MercatorMapComposer {
	static Vector tiles = new Vector(20);
	static Vector masks = new Vector(20);
	static String base = haxby.map.MapApp.TEMP_BASE_URL + "MapApp/merc_320_1024/";
	static String alt = null;
	public static void setBaseURL( String baseURL ) {
		alt = base;
		base = baseURL;
	}
	public static void setAlternateURL( String url ) {
		alt = url;
	}
	public static Grid2D.Image getImage(Rectangle2D rect, double zoom) {
		int mapRes = 512;
		int res = 512;
		while(zoom*res/mapRes > 1.5 && res>1) {
			res /=2;
		}
		int scale = mapRes/res;
		int x = (int)Math.floor(scale*rect.getX());
		int y = (int)Math.floor(scale*(rect.getY()));
		int width = (int)Math.ceil( scale*(rect.getX()+rect.getWidth()) ) - x;
		int height = (int)Math.ceil( scale*(rect.getY()+rect.getHeight()) ) - y;
		Rectangle bounds = new Rectangle( x, y, width, height );
		Mercator merc = new Mercator( 0., 0, 640*scale, MapProjection.SPHERE, CylindricalProjection.RANGE_0_to_360);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		BufferedImage tile;
		if( res < 32 ) {
			int resA = 32;
			int scaleA = mapRes/32;
			int xA = (int)Math.floor(scaleA*rect.getX());
			int yA = (int)Math.floor(scaleA*(rect.getY()-260.));
			int widthA = (int)Math.ceil( scaleA*(rect.getX()+rect.getWidth()) ) - xA;
			int heightA = (int)Math.ceil( scaleA*(rect.getY()-260.+rect.getHeight()) ) - yA;
			BufferedImage imageA = new BufferedImage(widthA, heightA, BufferedImage.TYPE_INT_RGB);
			int tileX0 = xA/320;
			if( xA<0 && tileX0*320!=xA ) tileX0--;
			int tileY0 = yA/320;
			if( yA<0 && tileY0*320!=yA ) tileY0--;
			int tileX, tileY;
			int x0,y0;
			int x1,x2,y1,y2;
			for( tileX = tileX0 ; tileX*320<xA+widthA ; tileX++) {
				x0 = tileX*320;
				x1 = Math.max( x0, xA);
				x2 = Math.min( x0+320, xA+widthA);
				for( tileY = tileY0 ; tileY*320<yA+heightA ; tileY++) {
					y0 = tileY*320;
					y1 = Math.max( y0, yA);
					y2 = Math.min( y0+320, yA+heightA);
					try {
						tile = getTile(resA, tileX, tileY);
						if(tile == null )continue;
					} catch( Exception ex ) {
						continue;
					}
					for( int ix=x1; ix<x2 ; ix++) {
						for( int iy=y1 ; iy<y2 ; iy++) {
							imageA.setRGB(ix-xA, iy-yA, tile.getRGB(ix-x0+8, iy-y0+8));
						}
					}
				}
			}
			Graphics2D g = image.createGraphics();
			double s = res/32.;
			double dx = xA/s - x;
			double dy = yA/s - y;
			AffineTransform at = new AffineTransform();
			at.translate( dx, dy );
			at.scale( 1./s, 1./s );
			g.setRenderingHint( RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawRenderedImage( imageA,  at);
		}
		BufferedImage mapImage = null;
		Rectangle mapRect = new Rectangle();
	//	if( res == overlay.getResolution() ) {
	//		mapRect = overlay.getRect();
	//		if( mapRect.contains(x, y, width, height) ) return false;
	//		mapImage = overlay.getImage();
	//	}
		int tileX0 = x/320;
		if( x<0 && tileX0*320!=x ) tileX0--;
		int tileY0 = y/320;
		if( y<0 && tileY0*320!=y ) tileY0--;
		int tileX, tileY;
		int x0,y0;
		int x1,x2,y1,y2;
		for( tileX = tileX0 ; tileX*320<x+width ; tileX++) {
			x0 = tileX*320;
			x1 = Math.max( x0, x);
			x2 = Math.min( x0+320, x+width);
			for( tileY = tileY0 ; tileY*320<y+height ; tileY++) {
				y0 = tileY*320;
				y1 = Math.max( y0, y);
				y2 = Math.min( y0+320, y+height);
				if(mapImage != null && mapRect.contains(x1,y1,x2-x1,y2-y1)) {
					for( int ix=x1; ix<x2 ; ix++) {
						for( int iy=y1 ; iy<y2 ; iy++) {
							image.setRGB(ix-x, iy-y, 
								mapImage.getRGB(ix-mapRect.x, iy-mapRect.y));
						}
					}
					continue;
				}
				try {
					tile = getTile(res, tileX, tileY);
					if(tile == null )continue;
				} catch( Exception ex ) {
				//	ex.printStackTrace();
					continue;
				}
				for( int ix=x1; ix<x2 ; ix++) {
					for( int iy=y1 ; iy<y2 ; iy++) {
						image.setRGB(ix-x, iy-y, tile.getRGB(ix-x0+8, iy-y0+8));
					}
				}
			}
		}
	//	x += 320*scale;
	//	y += 260*scale;
	//	overlay.setImage(image, x/(double)scale, y/(double)scale, 1./(double)scale);
	//	overlay.setRect(x, y, width, height);
	//	overlay.setResolution(res);
		Grid2D.Image gim = new Grid2D.Image( bounds, merc );
		gim.setBuffer( image);
		return gim;
	}
	public static BufferedImage getTile( int res, int x, int y) 
					throws IOException {
		Tile tile;
		int wrap = 1024/res;
		while(x<0) x+=wrap;
		while(x>=wrap) x-=wrap;
		for( int i=0 ; i<tiles.size() ; i++) {
			tile = (Tile)tiles.get(i);
			if(res==tile.res && x==tile.x && y==tile.y) {
				if(i!=0) {
					tiles.remove(i);
					tiles.add(0,tile);
				}
			//	JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(
			//				new ByteArrayInputStream(tile.jpeg));
			//	return decoder.decodeAsBufferedImage();
				return ImageIO.read( new ByteArrayInputStream(tile.jpeg) );
			}
		}
		int nGrid = 1024/res;
		int nLevel = 0;
		while( nGrid>8 ) {
			nLevel++;
			nGrid /= 8;
		}
		int factor = 8;
		for( int k=1 ; k<nLevel ; k++) factor *=8;
		String name = "i_"+res;
		for( int k=0 ; k<nLevel ; k++) {
			int xG = factor*(int)Math.floor( (double)x / (double)factor);
			int yG = factor*(int)Math.floor( (double)y / (double)factor);
			name += "/"+ getName( xG, yG );
			factor /= 8;
		}
		name += "/"+ getName( x, y ) +".jpg";
	//	URL url = URLFactory.url("http://oceana-ridge.ldeo.columbia.edu/"
		URL url = URLFactory.url(base + name );
		try {
			URLConnection con = url.openConnection();
			InputStream in = con.getInputStream();
			tile = new Tile(res, x, y, in, 0);
		} catch(IOException ex) {
			if( alt==null ) throw ex;
			url = URLFactory.url(alt + name );
			URLConnection con = url.openConnection();
			InputStream in = con.getInputStream();
			tile = new Tile(res, x, y, in, 0);
		}
		if(tiles.size() == 0) {
			tiles.add(tile);
		} else if(tiles.size() == 20) {
			tiles.remove(19);
			tiles.add(0,tile);
		} else {
			tiles.add(0,tile);
		}
	//	JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(
	//					new ByteArrayInputStream(tile.jpeg));
	//	return decoder.decodeAsBufferedImage();
		return ImageIO.read( new ByteArrayInputStream(tile.jpeg) );
	}
/*
	public static boolean getMaskImage(Rectangle2D rect, MapOverlay overlay, int mapRes) {
		double zoom = overlay.getXMap().getZoom();
		int res = mapRes;
		while(zoom*res/mapRes > 2.5 && res>1) {
			res /=2;
		}
		int scale = mapRes/res;
		int x = (int)Math.floor(scale*rect.getX());
		int y = (int)Math.floor(scale*(rect.getY()-260.));
		int width = (int)Math.ceil( scale*(rect.getX()+rect.getWidth()) ) - x;
		int height = (int)Math.ceil( scale*(rect.getY()-260.+rect.getHeight()) ) - y;
		width++;
		height++;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for( int yy=0 ; yy<height ; yy++) {
			for(int xx=0 ; xx<width ; xx++) image.setRGB(xx,yy,0x80000000);
		}
		BufferedImage tile;
		if( res < 32 ) {
			int resA = 32;
			int scaleA = mapRes/32;
			int xA = (int)Math.floor(scaleA*rect.getX());
			int yA = (int)Math.floor(scaleA*(rect.getY()-260.));
			int widthA = (int)Math.ceil( scaleA*(rect.getX()+rect.getWidth()) ) - xA;
			int heightA = (int)Math.ceil( scaleA*(rect.getY()-260.+rect.getHeight()) ) - yA;
			widthA++;
			heightA++;
			BufferedImage imageA = new BufferedImage(widthA, heightA, BufferedImage.TYPE_INT_ARGB);
			int tileX0 = xA/320;
			if( xA<0 && tileX0*320!=xA ) tileX0--;
			int tileY0 = yA/320;
			if( yA<0 && tileY0*320!=yA ) tileY0--;
			int tileX, tileY;
			int x0,y0;
			int x1,x2,y1,y2;
			for( tileX = tileX0 ; tileX*320<xA+widthA ; tileX++) {
				x0 = tileX*320;
				x1 = Math.max( x0, xA);
				x2 = Math.min( x0+320, xA+widthA);
				for( tileY = tileY0 ; tileY*320<yA+heightA ; tileY++) {
					y0 = tileY*320;
					y1 = Math.max( y0, yA);
					y2 = Math.min( y0+320, yA+heightA);
					try {
						tile = getMask(resA, tileX, tileY);
						if(tile == null )continue;
					} catch( Exception ex ) {
						continue;
					}
					for( int ix=x1; ix<x2 ; ix++) {
						for( int iy=y1 ; iy<y2 ; iy++) {
							imageA.setRGB(ix-xA, iy-yA, tile.getRGB(ix-x0, iy-y0));
						}
					}
				}
			}
			Graphics2D g = image.createGraphics();
			double s = res/32.;
			double dx = xA/s - x;
			double dy = yA/s - y;
			AffineTransform at = new AffineTransform();
			at.translate( dx, dy );
			at.scale( 1./s, 1./s );
			g.drawRenderedImage( imageA,  at);
		}
		int tileX0 = x/320;
		if( x<0 && tileX0*320!=x ) tileX0--;
		int tileY0 = y/320;
		if( y<0 && tileY0*320!=y ) tileY0--;
		int tileX, tileY;
		int x0,y0;
		int x1,x2,y1,y2;
		for( tileX = tileX0 ; tileX*320<x+width ; tileX++) {
			x0 = tileX*320;
			x1 = Math.max( x0, x);
			x2 = Math.min( x0+320, x+width);
			for( tileY = tileY0 ; tileY*320<y+height ; tileY++) {
				y0 = tileY*320;
				y1 = Math.max( y0, y);
				y2 = Math.min( y0+320, y+height);
				try {
					tile = getMask(res, tileX, tileY);
					if(tile == null )continue;
				} catch( Exception ex ) {
				//	ex.printStackTrace();
					continue;
				}
				for( int ix=x1; ix<x2 ; ix++) {
					for( int iy=y1 ; iy<y2 ; iy++) {
						image.setRGB(ix-x, iy-y, tile.getRGB(ix-x0, iy-y0));
					}
				}
			}
		}
	//	x += 320*scale;
		y += 260*scale;
		overlay.setMaskImage(image, (x-.5)/(double)scale, (y-.5)/(double)scale, 1./(double)scale);
	//	overlay.setRect(x, y, width, height);
	//	overlay.setResolution(res);
		return true;
	}
	public static BufferedImage getMask( int res, int x, int y) 
					throws IOException {
		int wrap = 1024/res;
		while(x<0) x+=wrap;
		while(x>=wrap) x-=wrap;
		Tile tile;
		for( int i=0 ; i<masks.size() ; i++) {
			tile = (Tile)masks.get(i);
			if(res==tile.res && x==tile.x && y==tile.y) {
				if(i!=0) {
					masks.remove(i);
					masks.add(0,tile);
				}
				return maskImage( tile );
			}
		}
		int nGrid = 1024/res;
		int nLevel = 0;
		while( nGrid>8 ) {
			nLevel++;
			nGrid /= 8;
		}
		int factor = 8;
		for( int k=1 ; k<nLevel ; k++) factor *=8;
		String name = "m_"+res;
		for( int k=0 ; k<nLevel ; k++) {
			int xG = factor*(int)Math.floor( (double)x / (double)factor);
			int yG = factor*(int)Math.floor( (double)y / (double)factor);
			name += "/"+ getName( xG, yG );
			factor /= 8;
		}
		name += "/"+ getName( x, y ) +".mask";
	//	URL url = URLFactory.url("http://oceana-ridge.ldeo.columbia.edu/"
		URL url = URLFactory.url(base + name );
//	System.out.println( url.toString() );
		try {
			URLConnection con = url.openConnection();
			InputStream in = con.getInputStream();
			tile = new Tile(res, x, y, in, 0);
		} catch(IOException ex) {
			if( alt==null ) throw ex;
			url = URLFactory.url(alt + name );
			URLConnection con = url.openConnection();
			InputStream in = con.getInputStream();
			con = url.openConnection();
			in = con.getInputStream();
			tile = new Tile(res, x, y, in, 0);
		}
		if(masks.size() == 0) {
			masks.add(tile);
		} else if(masks.size() == 20) {
			masks.remove(19);
			masks.add(0,tile);
		} else {
			masks.add(0,tile);
		}
		return maskImage( tile );
	}
	static BufferedImage maskImage( Tile tile ) throws IOException {
		DataInputStream in = new DataInputStream(
				new ByteArrayInputStream( tile.jpeg ));
		BufferedImage image = new BufferedImage( 320, 320, BufferedImage.TYPE_INT_ARGB);
		int gray = 0x80000000;
		int i=0;
		int n;
		byte[] mask = new byte[320*320];
		while( i<mask.length ) {
			n = in.readInt();
			byte tmp = (byte) (n>>24);
			n &= 0xffffff;
			for( int k=0 ; k<n ; k++,i++) {
				mask[i] = tmp;
			}
		}
		i=0;
		for(int y=0 ; y<320 ; y++ ) {
			for( int x=0 ; x<320 ; x++, i++) {
				if( mask[i]!=0 ) image.setRGB(x, y, 0);
				else image.setRGB(x, y, gray);
			}
		}
		return image;
	}
*/
	public static String getName(int x0, int y0) {
		return ( (x0>=0) ? "E"+x0 : "W"+(-x0) )
			+ ( (y0>=0) ? "N"+y0 : "S"+(-y0) )
			+ "_320";
	}
}

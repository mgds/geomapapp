package haxby.map;

import haxby.util.URLFactory;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JLabel;

//import com.sun.image.codec.jpeg.JPEGCodec;
//import com.sun.image.codec.jpeg.JPEGImageDecoder;

/*
 * Not used in GMA as of 6/10/09 JOC
 */
public class PolarMapServer {
	static Vector tiles = new Vector(20);
	public static boolean getImage(Rectangle2D rect, MapOverlay overlay) {
		double zoom = overlay.getXMap().getZoom();
		if(zoom < 1.75) {
			if(overlay.getImage()==null) return false;
			overlay.setImage(null, 0., 0., 1);
			return true;
		}
		double res = 2;
		int ires = 24;
		if(zoom > 3.5) {
			res=4;
			ires = 48;
		}
		int x = (int)Math.floor(res*rect.getX());
		int y = (int)Math.floor(res*rect.getY());
		int width = (int)Math.ceil( res*(rect.getX()+rect.getWidth()) ) - x;
		int height = (int)Math.ceil( res*(rect.getY()+rect.getHeight()) ) - y;
		BufferedImage mapImage = null;
		Rectangle mapRect = new Rectangle();
		if( ires == overlay.getResolution() ) {
			mapRect = overlay.getRect();
			if( mapRect.contains(x, y, width, height) ) return false;
			mapImage = overlay.getImage();
		}
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		BufferedImage tile;
		int tileX0 = x/600;
		int tileY0 = y/600;
		int tileX, tileY;
		int x0,y0;
		int x1,x2,y1,y2;
		for( tileX = tileX0 ; tileX*600<x+width ; tileX++) {
			x0 = tileX*600;
			x1 = Math.max( x0, x);
			x2 = Math.min( x0+600, x+width);
			for( tileY = tileY0 ; tileY*600<y+height ; tileY++) {
				y0 = tileY*600;
				y1 = Math.max( y0, y);
				y2 = Math.min( y0+600, y+height);
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
					tile = getTile(ires, tileX, tileY);
					if(tile == null )continue;
				} catch( Exception ex ) {
					continue;
				}
				for( int ix=x1; ix<x2 ; ix++) {
					for( int iy=y1 ; iy<y2 ; iy++) {
						image.setRGB(ix-x, iy-y, tile.getRGB(ix-x0+8, iy-y0+8));
					}
				}
			}
		}
		overlay.setImage(image, x/res, y/res, 1/res);
		overlay.setRect(x, y, width, height);
		overlay.setResolution(ires);
		return true;
	}
	public static boolean getImage(Rectangle2D rect, MapOverlay overlay, JLabel label) {
		double zoom = overlay.getXMap().getZoom();
		if(zoom < 1.75) {
			if(overlay.getImage()==null) return false;
			overlay.setImage(null, 0., 0., 1);
			return true;
		}
		double res = 2;
		int ires = 24;
		if(zoom > 3.5) {
			res=4;
			ires = 48;
		}
		int x = (int)Math.floor(res*rect.getX());
		int y = (int)Math.floor(res*rect.getY());
		int width = (int)Math.ceil( res*(rect.getX()+rect.getWidth()) ) - x;
		int height = (int)Math.ceil( res*(rect.getY()+rect.getHeight()) ) - y;
		BufferedImage mapImage = null;
		Rectangle mapRect = new Rectangle();
		if( ires == overlay.getResolution() ) {
			mapRect = overlay.getRect();
			if( mapRect.contains(x, y, width, height) ) return false;
			mapImage = overlay.getImage();
		}
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		BufferedImage tile;
		int tileX0 = x/600;
		int tileY0 = y/600;
		int tileX, tileY;
		int x0,y0;
		int x1,x2,y1,y2;
		if(label != null) {
			label.setText("Composing tiles");
		}
		for( tileX = tileX0 ; tileX*600<x+width ; tileX++) {
			x0 = tileX*600;
			x1 = Math.max( x0, x);
			x2 = Math.min( x0+600, x+width);
			for( tileY = tileY0 ; tileY*600<y+height ; tileY++) {
				y0 = tileY*600;
				y1 = Math.max( y0, y);
				y2 = Math.min( y0+600, y+height);
				if(mapImage != null && mapRect.contains(x1,y1,x2-x1,y2-y1)) {
					for( int ix=x1; ix<x2 ; ix++) {
						for( int iy=y1 ; iy<y2 ; iy++) {
							image.setRGB(ix-x, iy-y, 
								mapImage.getRGB(ix-mapRect.x, iy-mapRect.y));
						}
					}
					continue;
				}
				if(label!=null)label.setText("Composing tiles:\t"+tileX +"\t"+tileY+"\t"+ires);
				try {
					tile = getTile(ires, tileX, tileY);
					if(tile == null )continue;
				} catch( Exception ex ) {
					continue;
				}
				for( int ix=x1; ix<x2 ; ix++) {
					for( int iy=y1 ; iy<y2 ; iy++) {
						image.setRGB(ix-x, iy-y, tile.getRGB(ix-x0+8, iy-y0+8));
					}
				}
			}
		}
	//	if(label != null) label.setText("done");
		overlay.setImage(image, x/res, y/res, 1/res);
		overlay.setRect(x, y, width, height);
		overlay.setResolution(ires);
	//	if(label != null) label.setText(" ");
		return true;
	}
	public static BufferedImage getTile( int res, int x, int y) 
					throws IOException {
		Tile tile;
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
				BufferedImage im = ImageIO.read(new ByteArrayInputStream(tile.jpeg));
				return im;
			}
		}
		URL url = URLFactory.url(haxby.map.MapApp.TEMP_BASE_URL + "arctic/topo/tiles_light/NP"+res+"_"+x+"_"+y+".jpg");
		URLConnection con = url.openConnection();
		int length = con.getContentLength();
		InputStream in = con.getInputStream();
		tile = new Tile(res, x, y, in, length);
		in.close();
		if(tiles.size() == 0) {
			tiles.add(tile);
		} else if(tiles.size() == 20) {
			tiles.remove(19);
			tiles.add(0,tile);
		} else {
			tiles.add(0,tile);
		}
		//JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(
		//				new ByteArrayInputStream(tile.jpeg));
		//return decoder.decodeAsBufferedImage();
		BufferedImage im = ImageIO.read(new ByteArrayInputStream(tile.jpeg));
		return im;
	}
}

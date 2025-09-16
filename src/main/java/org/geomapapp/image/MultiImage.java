package org.geomapapp.image;

import haxby.map.MapApp;
import haxby.map.MapOverlay;
import haxby.util.URLFactory;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.geomapapp.gis.shape.ESRIShapefile;
import org.geomapapp.grid.TileIO;

public class MultiImage {
	int minRes, maxRes;
	Rectangle2D bounds;
	String baseURL;
	MapOverlay overlay;
	int mapType;
	String imageType;
	
	boolean remote;
	
//	GMA 1.4.8: public so can be read by zoomTo() in GridLayerDialog
//	ESRIShapefile shape;
	public ESRIShapefile shape;
	
	public MultiImage( int minRes, 
			int maxRes, 
			Rectangle2D bounds,
			int mapType,
			String imageType,
			String baseURL,
			ESRIShapefile shape ) {
		this.minRes = minRes;
		this.maxRes = maxRes;
		this.bounds = bounds;
		if( baseURL.startsWith("file://" )) baseURL = baseURL.substring(7);
		else if( baseURL.startsWith("file:")) baseURL = baseURL.substring(5);
		this.baseURL = baseURL;
		this.shape = shape;
		this.mapType = mapType;
		this.imageType = imageType;
		
		remote = baseURL.toLowerCase().startsWith("http");
	}
	public void setMap() {
		overlay = new MapOverlay( shape.getMap() );
	}

	public MapOverlay getMapOverlay() {
		return overlay;
	}
	
	public void getImagePolar (Rectangle2D rect, int mapRes)
	{
		double zoom = overlay.getXMap().getZoom();
		int res = mapRes;
		while(zoom*res/mapRes > 1.5 && res>1) {
			res /=2;
		}
		res = mapRes/res;
		res = Math.min(res , maxRes);
		res = Math.max(res, minRes);
		int scale = res;
		
		Rectangle bnds0 = new Rectangle( (int)Math.floor( bounds.getX()*res ),
				(int)Math.floor( (bounds.getY())*res ),
				(int)Math.ceil( bounds.getWidth()*res ),
				(int)Math.ceil( bounds.getHeight()*res ) );
		
		int x = (int)Math.floor(scale*(rect.getX()-320.));
		int y = (int)Math.floor(scale*(rect.getY()-320.));
		int width = (int)Math.ceil( scale*(rect.getX()-320.+rect.getWidth()) ) - x;
		int height = (int)Math.ceil( scale*(rect.getY()-320.+rect.getHeight()) ) - y;
		
		// Check that MultiImage intersects 
		if (y + height < bnds0.getMinY() || y > bnds0.getMaxY()) 
		{
			return;
		}
		if (x + width < bnds0.getMinX() || x > bnds0.getMaxX()) return;
		
		// Clamp x,y,width,height to drawing area
		if( x<bnds0.x ) {
			width -= bnds0.x - x;
			x = bnds0.x;
		}
		if( x + width > bnds0.getMaxY() ) {
			width = (int) (bnds0.getMaxX() - x);
		}
		
		if( y < bnds0.y ) {
			height -= bnds0.y - y;
			y = bnds0.y;
		}
		if( y + height > bnds0.getMaxY() ) {
			height = (int) (bnds0.getMaxY() - y);
		}
		
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		clearImage(image);
		
		BufferedImage tile;
		BufferedImage mapImage = null;
		Rectangle mapRect = new Rectangle();
		if( res == overlay.getResolution() ) {
			mapRect = overlay.getRect();
			if( mapRect.contains(x, y, width, height) ) return;
			mapImage = overlay.getImage();
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
				if(mapImage != null && mapRect.contains(
						x1 + 320 * scale, // Convert back to map XY
						y1 + 320 * scale , // Convert back to map XY
						x2-x1,
						y2-y1)) {
					for( int ix=x1; ix<x2 ; ix++) {
						for( int iy=y1 ; iy<y2 ; iy++) {
							image.setRGB(ix-x, iy-y, 
								mapImage.getRGB(ix-mapRect.x + 320 * scale, // Convert back to map XY
										iy-mapRect.y + 320 * scale)); // Convert back to map XY
						}
					}
					continue;
				}
				try {
					tile = getTile(res, tileX, tileY);
					if(tile == null )continue;
				} catch( Exception ex ) {
//					ex.printStackTrace(); 
					continue;
				}
				for( int ix=x1; ix<x2 ; ix++) {
					for( int iy=y1 ; iy<y2 ; iy++) {
						image.setRGB(ix-x, iy-y, tile.getRGB(ix-x0+8, iy-y0+8));
					}
				}
			}
		}
		x += 320*scale;
		y += 320*scale;
		overlay.setImage(image, x/(double)scale, y/(double)scale, 1./(double)scale);
		overlay.setRect(x, y, width, height);
		overlay.setResolution(res);
	}
	
	public void getImageMerc (Rectangle2D rect, int mapRes)
	{
		double zoom = overlay.getXMap().getZoom();
		int res = mapRes;
		while(zoom*res/mapRes > 1.5 && res>1) {
			res /=2;
		}
		res = mapRes/res;
		res = Math.min(res , maxRes);
		res = Math.max(res, minRes);
		int scale = res;
		int wrap = 640*res;
		
		Rectangle bnds0 = new Rectangle( (int)Math.floor( bounds.getX()*res ),
				(int)Math.floor( (bounds.getY())*res ),
				(int)Math.ceil( bounds.getWidth()*res ),
				(int)Math.ceil( bounds.getHeight()*res ) );

		int x = (int)Math.floor(scale*(rect.getX()));
		int y = (int)Math.floor(scale*(rect.getY()-260.));
		int width = (int)Math.ceil( scale*(rect.getX()+rect.getWidth()) ) - x;
		int height = (int)Math.ceil( scale*(rect.getY()-260.+rect.getHeight()) ) - y;
		
		// Apply wrap
		while (x > bnds0.getMaxX()) x-= wrap;
		while (x + width < bnds0.getMinX()) x += wrap;
		
		// Check that MultiImage intersects 
		if (y + height < bnds0.getMinY() || y > bnds0.getMaxY()) 
		{
			return;
		}
		if (x + width < bnds0.getMinX() || x > bnds0.getMaxX()) return;
		
		// Clamp x,y,width,height to drawing area
		if( x<bnds0.x ) {
			width -= bnds0.x - x;
			x = bnds0.x;
		}
		if( x + width > bnds0.getMaxY() ) {
			width = (int) (bnds0.getMaxX() - x);
		}
		
		if( y < bnds0.y ) {
			height -= bnds0.y - y;
			y = bnds0.y;
		}
		if( y + height > bnds0.getMaxY() ) {
			height = (int) (bnds0.getMaxY() - y);
		}
		
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		clearImage(image);
		
		BufferedImage tile;
		BufferedImage mapImage = null;
		Rectangle mapRect = new Rectangle();
		if( res == overlay.getResolution() ) {
			mapRect = overlay.getRect();
			if( mapRect.contains(x, y, width, height) ) return;
			mapImage = overlay.getImage();
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
				if(mapImage != null && mapRect.contains(
						x1,
						y1 + 260 * scale, // Convert back to map XY 
						x2-x1,
						y2-y1)) {
					for( int ix=x1; ix<x2 ; ix++) {
						for( int iy=y1 ; iy<y2 ; iy++) {
							image.setRGB(ix-x, iy-y, 
								mapImage.getRGB(ix-mapRect.x, 
										iy-mapRect.y + 260 * scale)); // Convert back to map XY
						}
					}
					continue;
				}
				try {
					tile = getTile(res, tileX, tileY);
					if(tile == null )continue;
				} catch( Exception ex ) {
//					ex.printStackTrace(); 
					continue;
				}
				for( int ix=x1; ix<x2 ; ix++) {
					for( int iy=y1 ; iy<y2 ; iy++) {
						image.setRGB(ix-x, iy-y, tile.getRGB(ix-x0+8, iy-y0+8));
					}
				}
			}
		}
		
		y += 260*scale;
		overlay.setImage(image, x/(double)scale, y/(double)scale, 1./(double)scale);
		overlay.setRect(x, y, width, height);
		overlay.setResolution(res);
	}
	
	private BufferedImage getTile(int res, int tileX, int tileY) throws IOException {
		String path = baseURL + "i_" + res + "/" + 
			TileIO.getName(tileX * 320, tileY * 320, 320) +
			"." + imageType;

		BufferedImage img;
		
		if (remote)
			img = ImageIO.read( URLFactory.url(path) );
		else
			img = ImageIO.read( new File(path) );
		
		return img;
	}
	public static void clearImage(BufferedImage img)
	{
		Graphics2D g = img.createGraphics();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0));
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
	}
	
	public void draw(Graphics2D g) {
		overlay.draw(g);
	}
	
	public void dispose() {
	}
	
	public void focus() {
		if (overlay == null) return;
		
		if (mapType == MapApp.MERCATOR_MAP)
			getImageMerc(shape.getMap().getClipRect2D(), 512);
		else
			getImagePolar(shape.getMap().getClipRect2D(), 512);
	}
}

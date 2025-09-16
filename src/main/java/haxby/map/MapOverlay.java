package haxby.map;

import org.geomapapp.grid.Grid2D;
// import org.geomapapp.geom.*;
import haxby.proj.*;

import java.awt.image.*;
import java.awt.*;
import java.awt.geom.*;


/**
 	Uses XMap to draw the Maps and Resolutions.
 */
public class MapOverlay implements Overlay {
	protected XMap map;

	/**
	 	The map
	 */
	protected BufferedImage image;

	/**
	 	Masked Image.
	 */
	protected BufferedImage maskedImage;

	/**
	 	Image's scale.
	 */
	protected double scale;

	/**
	 	Image's x and y coordinates.
	 */
	protected double x0, y0;

	/**
	 	Masked Image's scale.
	 */
	protected double scaleM;

	/**
	 	Masked Image's x and y coordinates.
	 */
	protected double x0M, y0M;

	/**
	 	If Masked Image shown.
	 */
	protected boolean mask;	
	protected Rectangle rect;
	protected int res;

	/**
	 	Saves XMap and sets variables to null.
	 	@param map the XMap previously created.
	 */
	public MapOverlay( XMap map ) {
		this.map = map;
		image = null;
		maskedImage = null;
		mask = false;
		res = 0;
		rect = new Rectangle();
	}

	/**
	 	Loads the Image at x0, y0, with a scale of scale.
	 	@param im Image to load.
	 	@param x0 x coordinate of image.
	 	@param y0 y coordinate of image.
	 	@param scale scale of image.
	 */
	public void setImage( BufferedImage im, double x0, double y0, double scale) {
		image = im;
	//	maskedImage = null;
		this.x0 = x0;
		this.y0 = y0;
		this.scale = scale;
	}

	/**
	 	Loads the Image.
	 	@param im Image to load.
	 */
	public void setImage( BufferedImage im ) {
		image = im;
	}

	/**
	 	Gets the current image.
	 	@return the Image.
	 */
	public BufferedImage getImage() {
		return image;
	}

	public BufferedImage getMaskedImage() {
		return maskedImage;
	}

	public Grid2D.Image getGeoRefImage() {
		double wrap = map.getWrap();
		if( image==null || wrap<=0. )return null;
		double yeq = ((CylindricalProjection)map.getProjection()).getY(0.);
		double xx0 = x0;
		while(xx0>wrap/2.)xx0-=wrap;
		while(xx0<-wrap/2.)xx0+=wrap;
		Rectangle bounds = new Rectangle(
				(int)(xx0/scale), (int)((y0-yeq)/scale),
				image.getWidth(), image.getHeight());
// System.out.println(bounds +"\t"+ (wrap/scale) +"\t"+ wrap);
		Mercator merc = new Mercator( 0., 0., (int)(wrap/scale), 0, 0);
		Grid2D.Image grid = new Grid2D.Image(bounds, merc);
		if( mask ) {
			BufferedImage im = new BufferedImage( bounds.width, bounds.height, image.TYPE_INT_RGB);
			Graphics2D g = (Graphics2D)im.createGraphics();
			g.drawImage(image,0,0,map);
			double ds = scale/scaleM;
			double x = scale*(ds*x0M-x0);
			double y = scale*(ds*y0M-y0);
			g.translate(x,y);
			g.scale( 1./ds, 1./ds);
			g.drawImage(maskedImage,0,0,map);
			grid.setBuffer(im);
		} else {
			grid.setBuffer(image);
		}
		return grid;
	}
	public double getScale() {
		return scale;
	}
	public double[] getOffsets() {
		return new double[] {x0, y0};
	}
	public XMap getMap() {
		return map;
	}
	public void setRect(Rectangle r) {
		rect.x = r.x;
		rect.y = r.y;
		rect.width = r.width;
		rect.height = r.height;
	}
	public void setRect(int x, int y, int width, int height) {
		rect.x = x;
		rect.y = y;
		rect.width = width;
		rect.height = height;
	}
	public Rectangle getRect() { return rect; }
	public void setResolution( int r ) { res = r; }
	public int getResolution() { return res; }

	/**
	 	Loads the Image at x0, y0, with a scale of scale; Loads the Masked Image.
	 	@param im Image to load.
	 	@param maskedImage Masked image to load.
	 	@param x0 x coordinate of image.
	 	@param y0 y coordinate of image.
	 	@param scale scale of image.
	 */
	public void setImage( BufferedImage im, BufferedImage maskedImage, 
				double x0, double y0, double scale) {
		image = im;
		this.maskedImage = maskedImage;
		this.x0 = x0;
		this.y0 = y0;
		this.scale = scale;
		this.x0M = x0;
		this.y0M = y0;
		this.scaleM = scale;
	}

	/**
	 	Loads the Masked Image at x0, y0, with a scale of scale.
	 	@param maskedImage Masked Image to load.
	 	@param x0 x coordinate of masked image.
	 	@param y0 y coordinate of masked image.
	 	@param scale scale of masked image.
	 */
	public void setMaskImage( BufferedImage im, double x0, double y0, double scale) {
		this.maskedImage = im;
		this.x0M = x0;
		this.y0M = y0;
		this.scaleM = scale;
	}

	/**
	 	Gets the XMap
	 	@return the XMap.
	 */
	public XMap getXMap() {
		return map;
	}

	/**
	 	Set the state of maskedImage.
	 	@param if the image should be Masked.
	 */
	public void maskImage( boolean tf ) {
		mask = tf;
	}
	
	public boolean isMasked() {
		if( maskedImage==null ) return false;
		return mask;
	}

	/**
	 	Draws the Image.
	 	@param g what to draw.
	 */
	public void drawImage(Graphics2D g) {
		if( image==null ) return;
		Rectangle2D.Double rect = (Rectangle2D.Double) map.getClipRect2D();
		if( y0+image.getHeight()*scale < rect.y ) return;
		if( y0 > rect.y + rect.height ) return;
		AffineTransform at = g.getTransform();
		AffineTransform trans = new AffineTransform();
		double wrap = map.getWrap();
		if(wrap > 0.) {
			while( x0 > rect.x ) x0-=wrap;
			while( x0 + image.getWidth()*scale < rect.x ) x0+=wrap;
		}
		if( x0 > rect.x+rect.width ) return;
		trans.translate(x0, y0);
		trans.scale(scale, scale);
		g.drawRenderedImage(image, trans);
		g.setTransform( at );
		if( wrap<=0.)return;
		x0+=wrap;
		while( x0 < rect.x+rect.width ) {
			trans.translate(wrap/scale, 0.);
			g.drawRenderedImage(image, trans);
			g.setTransform( at );
			x0+=wrap;
		}
	}
	
	/**
	 	Draws the Masked Image.
	 	@param g what to draw.
	 */
	public void drawMask(Graphics2D g) {
		if( maskedImage==null ) return;
		if( !mask ) return;
		Rectangle2D.Double rect = (Rectangle2D.Double) map.getClipRect2D();
		if( y0+image.getHeight()*scale < rect.y ) return;
		if( y0 > rect.y + rect.height ) return;
		AffineTransform transM = new AffineTransform();
		double wrap = map.getWrap();
		if(wrap > 0.) {
			while( x0M > rect.x ) x0M-=wrap;
			while( x0M + image.getWidth()*scale < rect.x ) x0M+=wrap;
		}
		if( x0M > rect.x+rect.width ) return;
		transM.translate(x0M, y0M);
		transM.scale(scaleM, scaleM);
		//Don't let the selected transparency affect the mask - set alpha to 1
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));;
		g.drawRenderedImage(maskedImage, transM);
		if (wrap > 0) x0M+=wrap;
	}

	/**
	 	Draws g.
	 	@param What to draw.
	 */
	public void draw(Graphics2D g) {
		if( image==null ) return;
		Rectangle2D.Double rect = (Rectangle2D.Double) map.getClipRect2D();
		if( y0+image.getHeight()*scale < rect.y ) return;
		if( y0 > rect.y + rect.height ) return;
		AffineTransform at = g.getTransform();
		AffineTransform trans = new AffineTransform();
		double wrap = map.getWrap();
		if(wrap > 0.) {
			while( x0 > rect.x ) x0-=wrap;
			while( x0 + image.getWidth()*scale < rect.x ) x0+=wrap;
		}
		if( x0 > rect.x+rect.width ) return;
		trans.translate(x0, y0);
		trans.scale(scale, scale);
		g.drawRenderedImage(image, trans);
		g.setTransform( at );
		if( wrap<=0.)return;
		x0+=wrap;
		while( x0 < rect.x+rect.width ) {
			trans.translate(wrap/scale, 0.);
			g.drawRenderedImage(image, trans);
			g.setTransform( at );
			x0+=wrap;
		}
	}
}

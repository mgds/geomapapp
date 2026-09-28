package org.geomapapp.util;

import org.geomapapp.geom.MapProjection;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.Shape;

public class ImageLayer implements Layer {
	BufferedImage image;
	double x, y;
	double scale;
	public ImageLayer( BufferedImage image, double x, double y , double scale) {
		this.image = image;
		this.x = x;
		this.y = y;
		this.scale = scale;
	}
	public ImageLayer( BufferedImage image ) {
		this( image, 0., 0., 1. );
	}
	public void draw(Graphics2D g, AffineTransform aTrans, Rectangle2D bounds) {
		AffineTransform at0 = g.getTransform();
		g.transform( aTrans );
		g.translate( x, y );
		g.scale( scale, scale );
		g.drawRenderedImage( image, new AffineTransform() );
		g.setTransform( at0 );
	}
	public Rectangle2D getBounds() {
		return new Rectangle2D.Double( x, y, image.getWidth()*scale, image.getHeight()*scale );
	}
	public void setProjection( MapProjection proj ) {
	}
	public boolean select( Shape shape, Rectangle2D shapeBounds ) {
		if( shapeBounds.intersects(getBounds()) )return shape.intersects(getBounds());
		return false;
	}
}

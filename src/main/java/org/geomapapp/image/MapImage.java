package org.geomapapp.image;

import org.geomapapp.util.*;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

public class MapImage implements haxby.map.Overlay {
	ImageComponent imageC;
	BufferedImage image;
	double x0, y0;
	double scale;
	public MapImage(ImageComponent imageC ) {
		this.imageC = imageC;
	}
	public void setImage( BufferedImage image,
				double x0,
				double y0,
				double scale ) {
		this.image = image;
		this.x0 = x0;
		this.y0 = y0;
		this.scale = scale;
	}
	public void draw( Graphics2D g ) {
		if( image==null )return;
		AffineTransform at = g.getTransform();
		g.translate( x0, y0 );
		g.scale( scale, scale );
		g.drawRenderedImage(image, new AffineTransform() );
	}
}

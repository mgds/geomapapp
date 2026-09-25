package org.geomapapp.util;

import org.geomapapp.geom.MapProjection;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public abstract interface Layer {
	public abstract void draw(Graphics2D g, AffineTransform aTrans, Rectangle2D bounds);
	public abstract Rectangle2D getBounds();
	public abstract boolean select( Shape shape, Rectangle2D shapeBounds );
	public abstract void setProjection( MapProjection proj );
}

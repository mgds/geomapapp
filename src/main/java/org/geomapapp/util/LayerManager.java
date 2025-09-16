package org.geomapapp.util;

import org.geomapapp.geom.MapProjection;

import java.util.Vector;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class LayerManager {
	ScalableComp map;
	MapProjection proj;
	Vector layers;
	Vector insets;
	public LayerManager( ScalableComp map ) {
		this.map = map;
		layers = new Vector();
		insets = new Vector();
	}
	public void setProjection( MapProjection proj ) {
		this.proj = proj;
	}
	public void draw(Graphics2D g, AffineTransform aTrans, Rectangle2D bounds) {
		for( int k=0 ; k<layers.size() ; k++) {
			((Layer)layers.get(k)).draw( g, aTrans, bounds);
		}
	}
	public Rectangle2D getBounds() {
		return map.getBounds();
	}
	public boolean select( Shape shape, Rectangle2D bounds ) {
		boolean sel = false;
		for( int k=0 ; k<layers.size() ; k++) {
			sel = sel || ((Layer)layers.get(k)).select( shape, bounds );
		}
		return sel;
	}
}

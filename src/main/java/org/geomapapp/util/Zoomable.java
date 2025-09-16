package org.geomapapp.util;

import java.awt.*;

/**
 	Zoomable allows for the zooming in and out of objects.
 */	
public abstract interface Zoomable {
	/**
	 	While moving this updates the mouse location.
	 	@param p Mouse location.
	 */
	public void setXY(Point p);
	
	/**
	 	While dragging this redefines the rectangle to the mouse coordinates.
	 	@param rect Rectangle from where mouse clicked to where mouse dragged.
	 */
	public void setRect(Rectangle rect);
	
	/**
	 	Zooms to a rectangle made by the mouse.
	 	@param rect Rectangle to zoom to, defined by mouse location.
	 */
	public void zoomTo(Rectangle rect);
	
	/**
	 	Zooms in onmmouse by an increment of 2x.
	 	@param p point to zoom in to.
	 */
	public void zoomIn(Point p);
	
	/**
	 	Zooms in onmmouse by an increment of 1/2x.
	 	@param p point to zoom out to.
	 */
	public void zoomOut(Point p);
}
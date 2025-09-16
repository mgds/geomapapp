package org.geomapapp.map;

import org.geomapapp.grid.*;
import org.geomapapp.util.*;

import haxby.proj.*;
import haxby.map.Overlay;
import haxby.map.MapOverlay;

import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.Vector;

/**
 	GMAMap loads and manipulates the map.
 */
public class GMAMap extends ScalableComponent implements Zoomable {
					
					
	Projection proj;
	
	/**
	 	Overlay objects added to the map.
	 */
	protected Vector overlays;
	
	/**
	 	Not implemented.
	 */
	protected Vector mapInsets;
	
	/**
	 	Scale factor
	 */
	protected double zoom;
	
	/**
	 	Dimension of map when zoom = 1;
	 */
	protected Rectangle bounds;
	
	/**
	 	For CylindricalProjection, wrap = nodes per 360 degrees; otherwise wrap = -1.
	 */
	protected double wrap;
	Grid2DOverlay focus = null;
	
	public GMAMap(Projection proj, Rectangle bounds ) {
		this.proj = proj;
		this.bounds = bounds;
		width = bounds.width;
		height = bounds.height;
		overlays = new Vector();
		zoom = 1d;
		try {
			CylindricalProjection p = (CylindricalProjection) proj;
			wrap = Math.rint(360.*(p.getX(10.)-p.getX(9.)));
		} catch (ClassCastException ex) {
			wrap = -1.;
		}
		setLayout( null );
	}
	
	/**
	 	Get wrap value.
	 	@return wrap value.
	 */
	public double getWrap() {
		return wrap;
	}
	
	/**
	 	Gets the default height/width.
	 	@return the default height/width in a dimension.
	 */
	public Dimension getDefaultSize() {
		return new Dimension( width, height );
	}
	
	/**
	 	If there is an overlay.
	 	@return If there is an overlay.
	 */	 
	public boolean hasOverlay( Overlay overlay ) {
		return overlays.contains( overlay );
	}
	
	/**
	 	Adds Overlay object to wrap.
	 	@param overlay Overlay object to add.
	 */
	public void addOverlay( Overlay overlay ) {
		overlays.add(overlay );
		if( overlay instanceof Grid2DOverlay ) focus=(Grid2DOverlay)overlay;
	}
	public Grid2DOverlay getFocus() {
		return focus;
	}
	/**
	 	Removes Overlay object from wrap.
	 	@param overlay Overlay object to remove.
	 */
	public void removeOverlay( Overlay overlay ) {
		overlays.remove( overlay );
	}
	
	public double[] getScales() {
		AffineTransform at = getTransform();
		return new double[] { at.getScaleX(), at.getScaleY()};
	}
	
	/**
	 	Gets the zoom factor;
	 	@return the zoom factor.
	 */	 
	public double getZoom() {
		AffineTransform at = getTransform();
		return at.getScaleX();
	}
	
	/**
	 	Gets the projection.
	 	@return the projection.
	 */
	public Projection getProjection() {
		return proj;
	}
	
	
	/**
	 	Gets the scaled and offset graphics object.
	 	<pre>
	 	<b>Caution</b> - Must always be put in the code block:
	 		GMAMap map;
	 		Shape stuff;
	 		synchronized(map.getTreeLock())
	 		{
	 			Graphics2D g = map.getGraphics2D();
	 			g.draw(stuff);
	 		}
	 	This ensures that the recorded graphics are syncronized.
	 	</pre>
	 	@return the scaled and offset graphics object.
	 */
	public Graphics2D getGraphics2D() {
		Graphics2D g = (Graphics2D) getGraphics();
		Rectangle r = getVisibleRect();
		g.clip(r);
		g.transform(getTransform());
		return g;
	}
	
	/**
	 	Gets the map coordinates (Projection object) at the curso location.
	 	@param mousePoint location of mouse.
	 	@return the coordinates of the mouse releative to the map.
	 */
	public Point2D getScaledPoint( Point2D mousePoint ) {
		return inverseTransform(mousePoint);
	}
	
	/**
	 	Gets the region currently displayed in Projection coordinates.
	 	@return the region currently displayed in Projection coordinates.
	 */
	public Rectangle2D getClipRect2D() {
		Rectangle r = getVisibleRect();
		Dimension dim = getPreferredSize();
		r.width = Math.min(r.width, dim.width);
		r.height = Math.min(r.height, dim.height);
		AffineTransform at = new AffineTransform();
		double zoom = getZoom();
		Rectangle2D.Double r2d = new Rectangle2D.Double(
				r.getX()/zoom, r.getY()/zoom,
				r.getWidth()/zoom, r.getHeight()/zoom);
		return r2d;
	}
	
	/**
	 	Paints designated graphics.
	 	@param g what to paint.
	 */
	public void paintComponent( Graphics g ) {
		Graphics2D g2 = (Graphics2D)g;
		Dimension dim = getPreferredSize();
		g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.transform( getTransform() );
		for( int i=0 ; i<overlays.size() ; i++) {
			((Overlay)overlays.get(i)).draw(g2);
		}
	}
}

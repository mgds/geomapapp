/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package haxby.worldwind.renderers;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
//import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.IconRenderer;
import gov.nasa.worldwind.render.WWIcon;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.OGLStackHandler;
import gov.nasa.worldwind.util.OGLUtil;
import gov.nasa.worldwind.view.orbit.OrbitView;
import haxby.worldwind.layers.DetailedIconLayer.DetailedIcon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Iterator;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import com.jogamp.opengl.util.texture.TextureCoords;

/**
 * @author tag
 * @version $Id: IconRenderer.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class DetailedIconRenderer extends IconRenderer
{
	protected double heading;
	protected double pitch;
	protected boolean isExtruded = true;
	protected OGLStackHandler oglStackHandler = new OGLStackHandler();

	public DetailedIconRenderer()
	{
	}

	public void render(DrawContext dc, Iterable<WWIcon> icons, double opacity)
	{
		this.drawMany(dc, icons, null, opacity);
	}

	protected class DetailedOrderedIcon extends IconRenderer.OrderedIcon
	{
		double opacity;
		
		public DetailedOrderedIcon(WWIcon icon, Vec4 point, Layer layer, double eyeDistance, double horizonDistance, double opacity)
		{
			super(icon,point,layer,eyeDistance,horizonDistance);
			this.opacity = opacity;
		}

		public Vec4 getPoint() {
			return point;
		}

		public DetailedIcon getIcon() {
			return (DetailedIcon) icon;
		}

		public Layer getLayer() {
			return layer;
		}

		public double getOpacity() {
			return opacity;
		}
	}
	
	@Override
	protected void beginDrawIcons(DrawContext dc) {
	  GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

		this.oglStackHandler.clear();

		int attributeMask =
			GL.GL_DEPTH_BUFFER_BIT // for depth test, depth mask and depth func
				| GL2.GL_TRANSFORM_BIT // for modelview and perspective
				| GL2.GL_VIEWPORT_BIT // for depth range
				| GL2.GL_CURRENT_BIT // for current color
				| GL.GL_COLOR_BUFFER_BIT // for alpha test func and ref, and blend
				| GL2.GL_TEXTURE_BIT // for texture env
				| GL.GL_DEPTH_BUFFER_BIT // for depth func
				| GL2.GL_ENABLE_BIT; // for enable/disable changes
		this.oglStackHandler.pushAttrib(gl, attributeMask);

		// Apply the depth buffer but don't change it.
		if ((!dc.isDeepPickingEnabled())) {
			gl.glEnable(GL.GL_DEPTH_TEST);
		}
		gl.glDepthMask(false);

		// Suppress any fully transparent image pixels
		gl.glEnable(GL2.GL_ALPHA_TEST);
		gl.glAlphaFunc(GL.GL_GREATER, 0.001f);

		// Load a parallel projection with dimensions (viewportWidth, viewportHeight)
		this.oglStackHandler.pushProjectionIdentity(gl);
		gl.glOrtho(0d, dc.getView().getViewport().width, 0d, dc.getView().getViewport().height, -16d, 16d);

		this.oglStackHandler.pushModelview(gl);
		this.oglStackHandler.pushTexture(gl);

		if (dc.isPickingMode())
		{
			this.pickSupport.beginPicking(dc);

			// Set up to replace the non-transparent texture colors with the single pick color.
			gl.glEnable(GL.GL_TEXTURE_2D);
			gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_COMBINE);
			gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_SRC0_RGB, GL2.GL_PREVIOUS);
			gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_RGB, GL2.GL_REPLACE);
		}
		else
		{
			gl.glEnable(GL.GL_TEXTURE_2D);
			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		}

		//Only calculate pitch / heading once **
		heading = this.computeHeading(dc.getView());
		pitch = this.computePitch(dc.getView());
	}

	protected void endDrawIcons(DrawContext dc)
	{
		if (dc.isPickingMode())
			this.pickSupport.endPicking(dc);

        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

        if (dc.isPickingMode())
        {
            gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, OGLUtil.DEFAULT_TEX_ENV_MODE);
            gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_SRC0_RGB, OGLUtil.DEFAULT_SRC0_RGB);
            gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_RGB, OGLUtil.DEFAULT_COMBINE_RGB);
        }

		gl.glBindTexture(GL.GL_TEXTURE_2D, 0);

		this.oglStackHandler.pop(gl);
	}

    protected void drawIconsInBatch(DrawContext dc, OrderedIcon uIcon)
    {
        this.drawIcon(dc, uIcon);

        // Draw as many as we can in a batch to save ogl state switching.
        Object nextItem = dc.peekOrderedRenderables();
        while (nextItem != null && nextItem instanceof OrderedIcon)
        {
            OrderedIcon oi = (OrderedIcon) nextItem;
            if (oi.getRenderer() != this)
                return;

            dc.pollOrderedRenderables(); // take it off the queue
            drawIcon(dc, oi);
            //this.drawIcon(dc, oi);

            nextItem = dc.peekOrderedRenderables();
        }
    }

   @SuppressWarnings({"UnusedDeclaration"})
    public void pick(DrawContext dc, Iterable<? extends WWIcon> icons, java.awt.Point pickPoint, Layer layer)
    {
        drawMany(dc, icons, layer, 1);
    }

    public void render(DrawContext dc, Iterable<? extends WWIcon> icons)
    {
        drawMany(dc, icons, null);
    }

    protected void drawMany(DrawContext dc, Iterable<? extends WWIcon> icons, Layer layer) {
		drawMany(dc, icons, layer, 1);
	}

	protected void drawMany(DrawContext dc, Iterable<? extends WWIcon> icons, Layer layer, double opacity)
	{
		if (dc == null)
		{
			String msg = Logging.getMessage("nullValue.DrawContextIsNull");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}

		if (dc.getVisibleSector() == null)
			return;

		SectorGeometryList geos = dc.getSurfaceGeometry();
		//noinspection RedundantIfStatement
		if (geos == null)
			return;

		if (icons == null)
		{
			String msg = Logging.getMessage("nullValue.IconIterator");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}

		Iterator<? extends WWIcon> iterator = icons.iterator();

		if (!iterator.hasNext())
			return;

		double horizon = dc.getView().getHorizonDistance(); // was computeHorizonDistance();

		while (iterator.hasNext())
		{
			DetailedIcon icon = (DetailedIcon) iterator.next();
			if (!isIconValid(icon, true))
			{
				// Record feedback data for this WWIcon if feedback is enabled.
				if (icon != null)
					this.recordFeedback(dc, icon, null, null);

				continue;
			}

			if (!icon.isVisible())
			{
				// Record feedback data for this WWIcon if feedback is enabled.
				this.recordFeedback(dc, icon, null, null);

				continue;
			}

			// Determine Cartesian position from the surface geometry if the icon is near the surface,
			// otherwise draw it from the globe.
			Position pos = icon.getPosition();
			Vec4 iconPoint = null;
			if (pos.getElevation() < dc.getGlobe().getMaxElevation() && !this.isAlwaysUseAbsoluteElevation()) {
				iconPoint = dc.getSurfaceGeometry().getSurfacePoint(icon.getPosition());
			}
			if (iconPoint == null) {
				 Angle lat = pos.getLatitude();
	             Angle lon = pos.getLongitude();
	          //   double elevation = pos.getElevation();
	           //     if (!this.isAlwaysUseAbsoluteElevation()) {
	           //         elevation += dc.getGlobe().getElevation(lat, lon);
	           //     }
				iconPoint = dc.getGlobe().computePointFromPosition(icon.getPosition());
			}
			
			double eyeDistance = icon.isAlwaysOnTop() ? 0 : dc.getView().getEyePoint().distanceTo3(iconPoint);

			if (this.isHorizonClippingEnabled() && eyeDistance > horizon)
			{
				// Record feedback data for this WWIcon if feedback is enabled.
				this.recordFeedback(dc, icon, iconPoint, null);

				continue; // don't render horizon-clipped icons
			}

			// If enabled, eliminate icons outside the view volume. Primarily used to control icon visibility beyond
			// the view volume's far clipping plane.
			if (this.isViewClippingEnabled() && !dc.getView().getFrustumInModelCoordinates().contains(iconPoint))
			{
				// Record feedback data for this WWIcon if feedback is enabled.
				this.recordFeedback(dc, icon, iconPoint, null);

				continue; // don't render frustum-clipped icons
			}

			// The icons aren't drawn here, but added to the ordered queue to be drawn back-to-front.
			dc.addOrderedRenderable(
					new DetailedOrderedIcon(icon, iconPoint, layer, eyeDistance, horizon, opacity));

			if (icon.isShowToolTip())
				this.addToolTip(dc, icon, iconPoint);
		}
	}

	protected Vec4 drawIcon(DrawContext dc, OrderedIcon uIcon)
	{
		DetailedOrderedIcon dIcon = (DetailedOrderedIcon) uIcon;
		
		if (dIcon.getPoint() == null)
		{
			String msg = Logging.getMessage("nullValue.PointIsNull");
			Logging.logger().severe(msg);

			 // Record feedback data for this WWIcon if feedback is enabled.
			if (dIcon.getIcon() != null) {
				this.recordFeedback(dc, dIcon.getIcon(), null, null);
			}
			return null;
		}

		DetailedIcon icon = dIcon.getIcon();

	LatLon latlon = dIcon.getIcon().getPosition();
	double iconLat = latlon.getLatitude().radians;
	double iconLon = latlon.getLongitude().radians;

		if (dc.getView().getFrustumInModelCoordinates().getNear().distanceTo(dIcon.getPoint()) < 0)
		{
			// Record feedback data for this WWIcon if feedback is enabled.
			this.recordFeedback(dc, icon, dIcon.getPoint(), null);

			return null;
		}

		final Vec4 screenPoint = dc.getView().project(dIcon.getPoint());
		if (screenPoint == null)
		{
			// Record feedback data for this WWIcon if feedback is enabled.
			this.recordFeedback(dc, icon, dIcon.getPoint(), null);

			return null;
		}

	Position centerPosition = dc.getViewportCenterPosition();
	double x,y,c;
	x = y = c = 0;

//		double pedestalScale;
//		double pedestalSpacing;
		//if (this.pedestal != null) {
			if (this.pedestal != null && centerPosition != null) {
				LatLon centerPos = centerPosition;
				double centerLat = centerPos.getLatitude().radians;
				double centerLon = centerPos.getLongitude().radians;

				double dLat = iconLat - centerLat;
				double dLon = iconLon - centerLon;
	
//			pedestalScale = this.pedestal.getScale();
//			pedestalSpacing = pedestal.getSpacingPixels();

	// Calculate great circle distance
	double a = Math.pow( Math.sin(dLat / 2) , 2) +
	Math.cos(centerLat) * 
	Math.cos(iconLat)* 
	Math.pow(Math.sin(dLon / 2) , 2);

	// c is radians from our center point to our icon
	c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

	// Calculate great circle bearing
	// Just calculate the x and y changes because we only need a vector
	// Perpendicular to the bearing not the bearing itself
			y = Math.sin(dLon) * Math.cos(iconLat);
			x = Math.cos(centerLat) * Math.sin(iconLat) -
			Math.sin(centerLat) * Math.cos(iconLat) * Math.cos(dLon);
			}
	//	}
//		else
//		{
//			pedestalScale = 0d;
//			pedestalSpacing = 0d;
//		}

 //System.out.println(icon.getIconElevation());
 		if (icon.getIconElevation() == 0 || !isExtruded) 
		{
			drawIcon(dc, dIcon, screenPoint, false, c, x, y);
			return screenPoint;
		} 
		
		double ve = dc.getVerticalExaggeration();
		Vec4 tmpPoint = null;
		if (icon.getIconElevation() < dc.getGlobe().getMaxElevation()) {
			tmpPoint = dc.getSurfaceGeometry().getSurfacePoint(
					uIcon.getPosition().getLatitude(),
					uIcon.getPosition().getLongitude(),
					ve * icon.getIconElevation());
		}
		if (tmpPoint == null) {
			tmpPoint = dc.getGlobe().computePointFromPosition(
						uIcon.getPosition().getLatitude(),
						uIcon.getPosition().getLongitude(),
						ve * icon.getIconElevation());
		}

		tmpPoint = dc.getView().project(tmpPoint);
		
		if (tmpPoint == null) {
			return screenPoint;
		}

		final Vec4 projectedPoint = tmpPoint;
		
		if (icon.getIconElevation() < 0)
		{
			drawIcon(dc, dIcon, projectedPoint, false, c, x, y);
			drawLine(dc, screenPoint, projectedPoint, dIcon);
			if (icon.isHighlighted()) {
				drawIcon(dc, dIcon, screenPoint, true, c, x, y);
			}
		}
		else
		{
			if (icon.isHighlighted())
			{
				drawIcon(dc, dIcon, screenPoint, true, c, x, y);
			}
			drawLine(dc, screenPoint, projectedPoint, dIcon);
			drawIcon(dc, dIcon, projectedPoint, false, c, x, y);
		}
			
//	        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.


//			this.setDepthFunc(dc, dIcon, screenPoint);
//
//			gl.glMatrixMode(GL2.GL_MODELVIEW);
//			gl.glLoadIdentity();

//			Dimension size = icon.getSize();
//			double width = size != null ? size.getWidth() : icon.getImageTexture().getWidth(dc);
//			double height = size != null ? size.getHeight() : icon.getImageTexture().getHeight(dc);
//			gl.glTranslated(screenPoint.x - width / 2, screenPoint.y + (pedestalScale * height) + pedestalSpacing, 0d);

//		Rectangle rect = new Rectangle((int) (screenPoint.x - width / 2), (int) (screenPoint.y), (int) width,
//		(int) (height + (pedestalScale * height) + pedestalSpacing));

//		if (dc.isPickingMode())
//		{
			//If in picking mode and pick clipping is enabled, check to see if the icon is within the pick volume.
//			if (this.isPickFrustumClippingEnabled() && !dc.getPickFrustums().intersectsAny(rect))
//			{
				// Record feedback data for this WWIcon if feedback is enabled.
//				this.recordFeedback(dc, icon, dIcon.getPoint(), rect);
//				return screenPoint;
//			}
//			else
//			{
//				java.awt.Color color = dc.getUniquePickColor();
//				int colorCode = color.getRGB();
//				this.pickSupport.addPickableObject(colorCode, icon, dIcon.getPosition(), false);
//				gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
//			}
//		}

//		if (icon.getBackgroundTexture() != null)
//			this.applyBackground(dc, icon, screenPoint, width, height, pedestalSpacing, pedestalScale);

//		if (icon.getImageTexture().bind(dc))
//		{
//			TextureCoords texCoords = icon.getImageTexture().getTexCoords();
//			gl.glScaled(width, height, 1d);
//			dc.drawUnitQuad(texCoords);
//		}

//		if (this.pedestal != null && this.pedestal.getImageTexture() != null)
//		{
//			gl.glLoadIdentity();
//			gl.glTranslated(screenPoint.x - (pedestalScale * (width / 2)), screenPoint.y, 0d);
//			gl.glScaled(width * pedestalScale, height * pedestalScale, 1d);

//			if (this.pedestal.getImageTexture().bind(dc))
//			{
//				TextureCoords texCoords = this.pedestal.getImageTexture().getTexCoords();
//				dc.drawUnitQuad(texCoords);
//			}
//		}

		// Record feedback data for this WWIcon if feedback is enabled.
//		this.recordFeedback(dc, icon, dIcon.getPoint(), rect);

		
//		}
		return screenPoint;
	}

	protected void drawLine(DrawContext dc, Vec4 screenPoint, Vec4 projectedPoint, DetailedOrderedIcon uIcon) {
		//GL gl = dc.getGL();
		GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
		
		gl.glLoadIdentity();
		
		double opacity = uIcon.getOpacity();
		
		opacity = Math.min(opacity, .5f);
		
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glColor4d(.2,.2,.2, opacity);
		
		gl.glBegin(GL.GL_LINES);
			gl.glVertex2d(screenPoint.x, screenPoint.y);
			gl.glVertex2d(projectedPoint.x, projectedPoint.y);
		gl.glEnd();
		gl.glEnable(GL.GL_TEXTURE_2D);
		
		gl.glColor4f(1,1,1,1);
	}

	private void drawIcon(DrawContext dc, DetailedOrderedIcon uIcon, Vec4 screenPoint, boolean surface, double c,
			double x, double y) { //  protected Vec4 drawIcon(DrawContext dc, OrderedIcon uIcon)
		DetailedIcon icon = uIcon.getIcon();
		boolean subSurface = icon.getIconElevation() < 0;
		
		//javax.media.opengl.GL gl = dc.getGL();
		GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

		this.setDepthFunc(dc, uIcon, screenPoint);

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

		Dimension size = icon.getSize();
		double width = size != null ? size.getWidth() : icon.getImageTexture().getWidth(dc);
		double height = size != null ? size.getHeight() : icon.getImageTexture().getHeight(dc);
		gl.glTranslated(screenPoint.x, screenPoint.y, 0d);

		Rectangle rect = new Rectangle((int) (screenPoint.x - width / 2), (int) (screenPoint.y - height / 2), (int) width,
				(int) (height));
		
		if (dc.isPickingMode())
		{
			 //If in picking mode and pick clipping is enabled, check to see if the icon is within the pick volume.
			if (this.isPickFrustumClippingEnabled() && !dc.getPickFrustums().intersectsAny(rect))
			{
				// Record feedback data for this WWIcon if feedback is enabled.
				this.recordFeedback(dc, icon, uIcon.getPoint(), rect);

				return;
			}
			else
			{
				java.awt.Color color = dc.getUniquePickColor();
				int colorCode = color.getRGB();
				this.pickSupport.addPickableObject(colorCode, icon, uIcon.getPosition(), false);
				gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
			}
		} else {
			Color color = icon.getIconColor();
			if (icon.isHighlighted())
				color = icon.getHighlightedIconColor();
			if (color != null) {
				byte opacity = (byte) (uIcon.getOpacity()* 255);
				if (subSurface)
					if (!surface)
						opacity = (byte) Math.min(opacity, 200);
				gl.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), opacity);
			}
		}

		gl.glRotated(-pitch, 1d, 0d, 0d);
		gl.glRotated(heading, 0d, 0d, 1d);

		gl.glRotated(Math.toDegrees(c), -x, y, 0);

		if (icon.isHighlighted())
			gl.glScaled(icon.getHighlightScale(), icon.getHighlightScale(), icon.getHighlightScale());

		// If the icon is projected below the surface draw the surface icon at half scale
		if (surface) {
			if (icon.getIconElevation() == 0) {
				gl.glScaled(width, height, 1d);
			} else {
				gl.glScaled(width / 2, height / 2, 1d);
			} 
		} else {
			gl.glScaled(width, height, 1d);
		}
		gl.glTranslatef(-.5f, -.5f, 0);
		
		if (icon.getImageTexture().bind(dc))
		{
			TextureCoords texCoords = icon.getImageTexture().getTexCoords();
			dc.drawUnitQuad(texCoords);
		}

		// Record feedback data for this WWIcon if feedback is enabled.
		this.recordFeedback(dc, icon, uIcon.getPoint(), rect);
	}
	


	// Moved to ww 1.2 needed to define recordFeedback method
    //**************************************************************//
    //********************  Feedback  ******************************//
    //**************************************************************//

    /**
     * Returns true if the IconRenderer should record feedback about how the specified WWIcon has been processed.
     *
     * @param dc   the current DrawContext.
     * @param icon the WWIcon to record feedback information for.
     *
     * @return true to record feedback; false otherwise.
     */
 //   protected boolean isFeedbackEnabled(DrawContext dc, DetailedIcon icon)
 //   {
  //      if (dc.isPickingMode())
  //          return false;
//
  //      Boolean b = (Boolean) icon.getValue(AVKey.FEEDBACK_ENABLED);
 //       return (b != null && b);
 //   }
 
	/**
	 * If feedback is enabled for the specified WWIcon, this method records feedback about how the specified WWIcon has
	 * been processed.
	 *
	 * @param dc         the current DrawContext.
	 * @param icon       the icon which the feedback information refers to.
	 * @param modelPoint the icon's reference point in model coordinates.
	 * @param screenRect the icon's bounding rectangle in screen coordinates.
	 */
	protected void recordFeedback(DrawContext dc, DetailedIcon icon, Vec4 modelPoint,
			Rectangle screenRect) {
		if (!this.isFeedbackEnabled(dc, icon))
			return;

		this.doRecordFeedback(dc, icon, modelPoint, screenRect);
	}
	
    /**
     * Records feedback about how the specified WWIcon has been processed.
     *
     * @param dc         the current DrawContext.
     * @param icon       the icon which the feedback information refers to.
     * @param modelPoint the icon's reference point in model coordinates.
     * @param screenRect the icon's bounding rectangle in screen coordinates.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void doRecordFeedback(DrawContext dc, DetailedIcon icon, Vec4 modelPoint, Rectangle screenRect)
    {
   //     icon.setValue(AVKey.FEEDBACK_REFERENCE_POINT, modelPoint);
  //      icon.setValue(AVKey.FEEDBACK_SCREEN_BOUNDS, screenRect);
    }

	private double computeHeading(View view)
	{
		if (view == null)
			return 0.0;

		if (!(view instanceof OrbitView))
			return 0.0;

		OrbitView orbitView = (OrbitView) view;
		return orbitView.getHeading().getDegrees();
	}

	private double computePitch(View view)
	{
		if (view == null)
			return 0.0;

		if (!(view instanceof OrbitView))
			return 0.0;

		OrbitView orbitView = (OrbitView) view;
		return orbitView.getPitch().getDegrees();
	}

	@Override
	public String toString()
	{
 	   return Logging.getMessage("layers.IconLayer.Name");
	}

	public void setExtruded(boolean isExtruded) {
		this.isExtruded = isExtruded;
	}
	
	public boolean isExtruded()
	{
		return this.isExtruded ;
	}
}

/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package haxby.worldwind.layers;

import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.texture.*;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.view.*;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.geomapapp.image.Palette;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.*;

public class ColorScaleLayer extends AbstractLayer
{
	
	public final static String NORTHWEST = "NorthWest";
	public final static String SOUTHWEST = "SouthWest";
	public final static String NORTHEAST = "NorthEast";
	public final static String SOUTHEAST = "SouthEast";

	/**
 	* On window resize, scales the compass icon to occupy a constant relative size of the viewport.
 	*/
	public final static String RESIZE_STRETCH = "ResizeStretch";
	/**
 	* On window resize, scales the compass icon to occupy a constant relative size of the viewport, but not larger than
 	* the icon's inherent size scaled by the layer's icon scale factor.
 	*/
	public final static String RESIZE_SHRINK_ONLY = "ResizeShrinkOnly";
	/**
 	* Does not modify the compass icon size when the window changes size.
 	*/
	public final static String RESIZE_KEEP_FIXED_SIZE = "ResizeKeepFixedSize";
	
	protected ColorScale scaleSupplier;

	// Draw it as ordered with an eye distance of 0 so that it shows up in front of most other things.
	private OrderedIcon orderedImage = new OrderedIcon();
	private int width = 20;
	private int scale = 1;
	private int height = 200;
	private Vec4 locationCenter;
	private double borderWidth = 20;
	private String position = NORTHWEST;
	private String resizeBehavior = RESIZE_SHRINK_ONLY;
	private int toViewportScale = 1;
	private int scale_divisions = 20;

	private TextRenderer textRenderer = new TextRenderer( Font.decode("Arial-36-BOLD"), true, true);
	
	private class OrderedIcon implements OrderedRenderable
	{
		public double getDistanceFromEye()
		{
			return 0;
		}

		public void pick(DrawContext dc, Point pickPoint)
		{
			// Not implemented
		}

		public void render(DrawContext dc)
		{
			ColorScaleLayer.this.draw(dc);
		}
	}

	public void setScaleSupplier(ColorScale scaleSupplier) {
		this.scaleSupplier = scaleSupplier;
	}
	
	protected void doRender(DrawContext dc)
	{
		dc.addOrderedRenderable(this.orderedImage);
	}

	private void draw(DrawContext dc)
	{
		if (scaleSupplier == null ||
				!scaleSupplier.isColorScaleValid()) return;
		
		//GL gl = dc.getGL();
		GL2 gl = (GL2)dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

		boolean attribsPushed = false;
		boolean modelviewPushed = false;
		boolean projectionPushed = false;

		try
		{
			gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT
				| GL.GL_COLOR_BUFFER_BIT
				| GL2.GL_ENABLE_BIT
				| GL2.GL_TEXTURE_BIT
				| GL2.GL_TRANSFORM_BIT
				| GL2.GL_VIEWPORT_BIT
				| GL2.GL_CURRENT_BIT
				| GL2.GL_LINE_BIT);
			attribsPushed = true;

			gl.glDisable(GL.GL_TEXTURE_2D);		// no textures
			
			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			gl.glDisable(GL.GL_DEPTH_TEST);

			double width = this.getScaledWidth();
			double height = this.getScaledHeight();

			// Load a parallel projection with xy dimensions (viewportWidth, viewportHeight)
			// into the GL projection matrix.
			java.awt.Rectangle viewport = dc.getView().getViewport();
			gl.glMatrixMode(javax.media.opengl.GL2.GL_PROJECTION);
			gl.glPushMatrix();
			projectionPushed = true;
			gl.glLoadIdentity();
			double maxwh = width > height ? width : height;
			gl.glOrtho(0d, viewport.width, 0d, viewport.height, -0.6 * maxwh, 0.6 * maxwh);

			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glPushMatrix();
			modelviewPushed = true;
			gl.glLoadIdentity();

			double scale = this.computeScale(viewport);
			Vec4 locationSW = this.computeLocation(viewport, scale);

			gl.glTranslated(locationSW.x, locationSW.y, locationSW.z);
			gl.glScaled(scale, scale, 1);

			gl.glScaled(width, height, 1d);
			
			Palette p = scaleSupplier.getPalette();
			float [] range = scaleSupplier.getRange();

			float deltaRange = range[1] - range[0];
			
			// Compute anotation locations
			int k = 0;
			double scaleInt = 1;
			int a1 = (int)Math.ceil(range[0]/scaleInt);
			int a2 = (int)Math.floor(range[1]/scaleInt);
			double hMax = 2 * (textRenderer.getBounds(Integer.toString(a2)).getHeight());
			double[] dAnot = {2, 2.5, 2};
			while( hMax*(a2-a1+1) > height * scale ) {
				scaleInt *= dAnot[k];
				k = (k+1)%3;
				a1 = (int)Math.ceil(range[0]/scaleInt);
				a2 = (int)Math.floor(range[1]/scaleInt);
			} 
//			a1 = (int)Math.ceil(range[0]/scaleInt);
//			a2 = (int)Math.floor(range[1]/scaleInt);
			
			// Draw the scale bar
			float[] rgb = new float[3];
			float z;
			Color c;
			
			// Draw the Scale Bar 
			gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_FILL);
			gl.glBegin(GL2.GL_QUAD_STRIP);
				for (int y = 0; y < scale_divisions ; y++) {
					z = y / (scale_divisions - 1f) * deltaRange + range[0];
					
					c = new Color( p.getRGB(z, .215f) );
					rgb = c.getRGBColorComponents(rgb);
					gl.glColor4f(rgb[0], rgb[1], rgb[2], (float) this.getOpacity());
					gl.glVertex2d(0, y / (scale_divisions - 1.0));
					
					c = new Color( p.getRGB(z, .815f) );
					rgb = c.getRGBColorComponents(rgb);
					gl.glColor4f(rgb[0], rgb[1], rgb[2], (float) this.getOpacity());
					gl.glVertex2d(1, y / (scale_divisions - 1.0));
					
				}
			gl.glEnd();
		
			// Draw the scale box
			gl.glLineWidth(2f);
			gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
			gl.glColor4f(0,0,0, (float) this.getOpacity());
			gl.glBegin(GL2.GL_QUADS);
				gl.glVertex2d(0,0);
				gl.glVertex2d(1,0);
				gl.glVertex2d(1,1);
				gl.glVertex2d(0,1);
			gl.glEnd();
			
			
			double ax;
			// Draw the Annotation lines
			gl.glBegin(GL.GL_LINES);
				for( k=a1 ; k<=a2 ; k++ ) {
					ax = (scaleInt*k - range[0]) / deltaRange;
					gl.glVertex2d(0,ax);
					gl.glVertex2d(1,ax);
				}
			gl.glEnd();
			
			
			
			//Draw the annotations
			gl.glLoadIdentity();
			gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_FILL);
			gl.glDisable(GL.GL_CULL_FACE);
			
			Vec4 locationSE = locationSW.add3(new Vec4(scale * width* 1, 0, 0));
			
			
			String anot;
			
			for( k=a1 ; k<=a2 ; k++ ) {
				ax = (scaleInt*k - range[0]) / deltaRange;
				anot = String.format("%.00f",(k*scaleInt * scaleSupplier.getAnnotationFactor()));
				
				drawLabel(anot, locationSE.add3(new Vec4(4, scale * height * ax, 0)));
			}
			
			// Draw the scale name
			
			drawLabel(scaleSupplier.getTitle(), locationSW.add3(new Vec4(0, scale * (height + hMax / 2), 0)));
			
//			drawLabel( String.format("%.0f ", range[0]), locationSW.add3( new Vec4(scale * width * 1.5, 0, 0)));
//			drawLabel( String.format("%.0f ", range[1]), locationSW.add3( new Vec4(scale * width * 1.5, scale * height, 0)));
		}
		finally
		{
			if (projectionPushed)
			{
				gl.glMatrixMode(GL2.GL_PROJECTION);
				gl.glPopMatrix();
			}
			if (modelviewPushed)
			{
				gl.glMatrixMode(GL2.GL_MODELVIEW);
				gl.glPopMatrix();
			}
			if (attribsPushed)
				gl.glPopAttrib();
		}
	}
	
 // Draw the scale label
	private void drawLabel(String text, Vec4 screenPoint)
	{
		if (text == null) return;
		
		Rectangle2D nameBound = this.textRenderer.getBounds(text);
		
		int x = (int) screenPoint.x();
		int y = (int) (screenPoint.y() - nameBound.getHeight() / 2d);

		this.textRenderer.begin3DRendering();

		this.textRenderer.setColor(0,0,0, (float)getOpacity());
		this.textRenderer.draw(text, x + 1, y - 1);
		this.textRenderer.setColor(1,1,1, (float)getOpacity());
		this.textRenderer.draw(text, x, y);

		this.textRenderer.end3DRendering();

	}

	private double computeScale(java.awt.Rectangle viewport)
	{
		if (this.resizeBehavior.equals(RESIZE_SHRINK_ONLY))
		{
			return Math.min(1d, (this.toViewportScale) * viewport.width / this.getScaledWidth());
		}
		else if (this.resizeBehavior.equals(RESIZE_STRETCH))
		{
			return (this.toViewportScale) * viewport.width / this.getScaledWidth();
		}
		else if (this.resizeBehavior.equals(RESIZE_KEEP_FIXED_SIZE))
		{
			return 1d;
		}
		else
		{
			return 1d;
		}
	}

	private double getScaledWidth()
	{
		return this.width * this.scale;
	}

	private double getScaledHeight()
	{
		return this.height * this.scale;
	}

	private Vec4 computeLocation(java.awt.Rectangle viewport, double scale)
	{
		double width = this.getScaledWidth();
		double height = this.getScaledHeight();

		double scaledWidth = scale * width;
		double scaledHeight = scale * height;

		double x;
		double y;

		if (this.locationCenter != null)
		{
			x = viewport.getWidth() - scaledWidth / 2 - this.borderWidth;
			y = viewport.getHeight() - scaledHeight / 2 - this.borderWidth;
		}
		else if (this.position.equals(NORTHEAST))
		{
			x = viewport.getWidth() - scaledWidth - this.borderWidth;
			y = viewport.getHeight() - scaledHeight - this.borderWidth;
		}
		else if (this.position.equals(SOUTHEAST))
		{
			x = viewport.getWidth() - scaledWidth - this.borderWidth;
			y = 0d + this.borderWidth;
		}
		else if (this.position.equals(NORTHWEST))
		{
			x = 0d + this.borderWidth;
			y = viewport.getHeight() - scaledHeight - this.borderWidth;
		}
		else if (this.position.equals(SOUTHWEST))
		{
			x = 0d + this.borderWidth;
			y = 0d + this.borderWidth;
		}
		else // use North East
		{
			x = viewport.getWidth() - scaledWidth / 2 - this.borderWidth;
			y = viewport.getHeight() - scaledHeight / 2 - this.borderWidth;
		}

		return new Vec4(x, y, 0);
	}

	public String toString()
	{
		return "Color Scale";
	}
	
	public static interface ColorScale {
		public float[] getRange();
		public Palette getPalette();
		public String getTitle();
		public boolean isColorScaleValid();
    	public double getAnnotationFactor();
	}
}

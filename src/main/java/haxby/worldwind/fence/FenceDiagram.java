/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package haxby.worldwind.fence;

import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.BufferFactory.DoubleBufferFactory;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.DoubleBuffer;
import java.util.LinkedList;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

public class FenceDiagram implements Renderable
{
	private static final int MAX_DIM = 1024;

	protected List<LatLon> points;
	protected double elevation;
	protected double baseHeight;
	protected double exageration = 1;
	protected float crossLinePercent = Float.NaN;

	protected int antiAliasHint = GL.GL_FASTEST;
	protected Color color = Color.WHITE;

	protected List<SubFence> children = new LinkedList<SubFence>();
	protected int imageHeight, imageWidth;
	protected BufferedImage imageSource;

	protected float yMin = 0;
	protected float yMax = 1;

	protected float opacity = Float.NaN;

	private WorldWindow ww;
	private int maxSubDim;

	private class SubFence {
		private List<LatLon> points;
		private Vec4 referenceCenter;
		private DoubleBuffer crossLine;
		private DoubleBuffer vertices;
		private DoubleBuffer stilts;
		private BufferedImage subImage;

		protected Texture texture;
		protected DoubleBuffer textureCoordinates;
		protected long geomGenTime;

		public SubFence(List<LatLon> points, BufferedImage subImage) {
			this.points = points;
			this.subImage = subImage;
		}

		public void render(DrawContext dc) {
			GL2 gl = dc.getGL().getGL2();
			float opacity = Float.isNaN(FenceDiagram.this.opacity) ? 
					(float) dc.getCurrentLayer().getOpacity() : FenceDiagram.this.opacity;

			if (opacity == 0) return;

			if (this.vertices == null) 
				this.initializeGeometry(dc);

			if (this.crossLine == null && !Float.isNaN(crossLinePercent))
				this.initializeCrossline(dc);

			if (this.stilts == null ||
					this.geomGenTime != dc.getFrameTimeStamp()) {
				initializeStilts(dc);
				this.geomGenTime = dc.getFrameTimeStamp();
			}

			if (this.texture == null)  {
				long sTime = System.currentTimeMillis();
				System.out.print("Making texture ... ");
				this.texture = AWTTextureIO.newTexture(gl.getGLProfile(), subImage, true);
				System.out.println("Done. " + (System.currentTimeMillis() - sTime));
			}

			//GL gl = dc.getGL();
			boolean textureMatrixPushed = false;

			int attrBits = GL2.GL_HINT_BIT | 
				GL2.GL_CURRENT_BIT | 
				GL.GL_COLOR_BUFFER_BIT |
				GL.GL_LINE_WIDTH;

			if (!dc.isPickingMode())
			{
				if (this.texture != null)
					attrBits |= GL2.GL_TEXTURE_BIT | GL2.GL_TRANSFORM_BIT;
			}

			gl.glPushAttrib(attrBits);
			gl.glPushClientAttrib(GL2.GL_CLIENT_VERTEX_ARRAY_BIT);
			dc.getView().pushReferenceCenter(dc, this.referenceCenter);

			try
			{
				if (!dc.isPickingMode())
				{
					gl.glColor4f(1, 1, 1, opacity);

					if (this.texture != null)
					{
						gl.glMatrixMode(GL.GL_TEXTURE);
						gl.glPushMatrix();
						textureMatrixPushed = true;
						gl.glLoadIdentity();

						gl.glEnable(GL.GL_TEXTURE_2D);

						if (this.textureCoordinates == null)
							this.initializeTextureCoordinates();
						gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
						gl.glTexCoordPointer(2, 
								GL2.GL_DOUBLE, 
								0, 
								this.textureCoordinates.rewind());

						gl.glEnable(GL.GL_BLEND);
						if (opacity == 1) {
							gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
						}
						else {
							gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
						}

						this.texture.bind(gl);
					}
				}

				gl.glHint(GL2.GL_POLYGON_SMOOTH_HINT, antiAliasHint);
				gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
				gl.glVertexPointer(3, GL2.GL_DOUBLE, 0, this.vertices.rewind());
				gl.glDrawArrays(GL2.GL_QUAD_STRIP, 0, this.points.size() * 2);

				if (textureMatrixPushed) {
					gl.glMatrixMode(GL2.GL_TEXTURE);
					gl.glPopMatrix();
				}

				gl.glColor3f(0, 0, 0);
				gl.glVertexPointer(3, GL2.GL_DOUBLE, 0, this.stilts.rewind());
				gl.glDrawArrays(GL2.GL_LINES, 0, this.points.size() * 2);

				if (this.crossLine != null && !Float.isNaN(crossLinePercent)) {
					gl.glColor3f(1, 0, 0);
					gl.glLineWidth(4);
					gl.glVertexPointer(3, GL2.GL_DOUBLE, 0, this.crossLine.rewind());
					gl.glDrawArrays(GL.GL_LINE_STRIP, 0, this.points.size() );
				}
			}
			finally
			{
				if (textureMatrixPushed)
				{
					gl.glMatrixMode(GL.GL_TEXTURE);
					gl.glPopMatrix();
				}

				gl.glPopClientAttrib();
				gl.glPopAttrib();
				dc.getView().popReferenceCenter(dc);
			}
		}

		public void dispose() {
			if (vertices != null) {
				vertices.clear();
			}
			vertices = null;
			
			if (points != null)
				points.clear();
			points = null;

			subImage = null;

			if (texture != null) {
				GLContext context = ww.getSceneController().getDrawContext().getGLContext();

				int i = context.makeCurrent();
				if (i == GLContext.CONTEXT_CURRENT) {
					((Disposable) texture).dispose();
					context.release();
				} else
					System.err.println("Could not make GLContext current");
			}
			texture = null;
		}

		private void initializeCrossline(DrawContext dc) {
			DoubleBuffer verts = crossLine;

			if (verts == null)
				verts = Buffers.newDirectDoubleBuffer(points.size() * 3);

			for (LatLon point : points)
			{
				double alt = 
					elevation + baseHeight * exageration * (yMax - yMin) * crossLinePercent;
				
				Vec4 p1 =
					dc.getGlobe().computePointFromPosition(point.getLatitude(), point.getLongitude(), alt);

				verts.put(p1.x - referenceCenter.x);
				verts.put(p1.y - referenceCenter.y);
				verts.put(p1.z - referenceCenter.z);
			}
			
			this.crossLine = verts;
		}
		
		private void initializeStilts(DrawContext dc) {
			DoubleBuffer verts = stilts;
			
			if (verts == null)
				verts = Buffers.newDirectDoubleBuffer(points.size() * 2 * 3);

			for (LatLon point : points)
			{
				Vec4 p1 =
					dc.getGlobe().computePointFromPosition(point.getLatitude(), point.getLongitude(), elevation);
				Vec4 p2 = dc.getSurfaceGeometry().getSurfacePoint(
						new Position(point, 0));

				if (p2 == null)
				{
					double elev = 
						dc.getGlobe().getElevation(
								point.latitude, point.longitude) 
								* dc.getVerticalExaggeration();
					p2 = dc.getGlobe().computePointFromPosition(
							new Position(point, elev));
				}
				
				verts.put(p1.x - referenceCenter.x);
				verts.put(p1.y - referenceCenter.y);
				verts.put(p1.z - referenceCenter.z);
				
				verts.put(p2.x - referenceCenter.x);
				verts.put(p2.y - referenceCenter.y);
				verts.put(p2.z - referenceCenter.z);
			}
			
			this.stilts = verts;
		}
		
		private void initializeGeometry(DrawContext dc)
		{
			DoubleBuffer verts = Buffers.newDirectDoubleBuffer(points.size() * 2 * 3);

			Vec4[][] p = new Vec4[points.size()][2];

			int i = 0;
			for (LatLon point : points)
			{
				p[i][0] = dc.getGlobe().computePointFromPosition(point.getLatitude(), point.getLongitude(), elevation);
				p[i][1] = dc.getGlobe().computePointFromPosition(point.getLatitude(), point.getLongitude(), elevation + baseHeight * exageration * (yMax - yMin));
				i++;
			}

			Vec4 refcenter = p[p.length / 2][0];
			for (i = 0; i < p.length; i++)
			{
				verts.put(p[i][0].x - refcenter.x);
				verts.put(p[i][0].y - refcenter.y);
				verts.put(p[i][0].z - refcenter.z);
				
				verts.put(p[i][1].x - refcenter.x);
				verts.put(p[i][1].y - refcenter.y);
				verts.put(p[i][1].z - refcenter.z);
			}
			
			this.referenceCenter = refcenter;
			this.vertices = verts;
		}
		
		protected void initializeTextureCoordinates()
		{
			TextureCoords tc = this.texture.getImageTexCoords();
			float left = tc.left();
			float right = tc.right();

			float top = tc.top();
			float bottom = tc.bottom();
			
			float tcWIDTH = right - left;
			float tcHEIGHT = bottom - top;
			
			top = top + tcHEIGHT * yMin;
			bottom = top + tcHEIGHT * (yMax - yMin);
			
			this.textureCoordinates = Buffers.newDirectDoubleBuffer(points.size() * 2 * 2);
			
			this.vertices.rewind();
			
			double totalDistance = 0;
			double x0 = vertices.get();
			double y0 = vertices.get();
			vertices.get(); vertices.get(); vertices.get(); vertices.get();
			while (this.vertices.hasRemaining())
			{
				double x = vertices.get();
				double y = vertices.get();
				vertices.get(); vertices.get(); vertices.get(); vertices.get();
				
				double dx = x0 - x;
				double dy = y0 - y;
				totalDistance += Math.pow(dx * dx + dy * dy,
						.5);
				
				x0 = x;
				y0 = y;
			}
			
			this.vertices.rewind();
			double distance = 0;
			x0 = vertices.get();
			y0 = vertices.get();
			vertices.get(); vertices.get(); vertices.get(); vertices.get();
			this.textureCoordinates.put(left).put(bottom); // 0,0
			this.textureCoordinates.put(left).put(top); // 0,1
			while (this.vertices.hasRemaining())
			{
				double x = vertices.get();
				double y = vertices.get();
				vertices.get(); vertices.get(); vertices.get(); vertices.get();
				
				double dx = x0 - x;
				double dy = y0 - y;
				distance += Math.pow(dx * dx + dy * dy,
						.5);
				
				x0 = x;
				y0 = y;
				
				double d = distance / totalDistance;
				
				this.textureCoordinates.put(d * tcWIDTH + left).put(bottom); // Bottom
				this.textureCoordinates.put(d * tcWIDTH + left).put(top); // Top
			}
		}
	}
	
	public FenceDiagram(WorldWindow ww, List<LatLon> points, double elevation, double exageration, BufferedImage imageSource) {
		this(ww, points, elevation, exageration);
		setImageSource(imageSource);
	}

	public FenceDiagram(WorldWindow ww, List<LatLon> points, double elevation, double exageration)
	{
		if (points == null)
		{
			String msg = Logging.getMessage("nullValue.PositionIsNull");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}

		this.ww = ww;
		this.points = points;
		this.elevation = elevation;
		this.exageration = exageration;
	}
	
	private void createSubFences() {
		Globe globe = ww.getModel().getGlobe();
		
		Vec4[] p = new Vec4[points.size()];
		double[] pDistance = new double[points.size()];
		pDistance[0] = 0;
		
		double totalDistance = 0;
		int i = 0;
		for (LatLon point : points) {
			p[i] = globe.computePointFromPosition(point.getLatitude(), point.getLongitude(), elevation);
			if (i > 0) {
				double distance = p[i].distanceTo3(p[i - 1]);
				totalDistance += distance;
				pDistance[i] = totalDistance;
			}
			i++;
		}
		
		double pixelsPerUnit = imageWidth / totalDistance;
		double subFenceUnitDistance = maxSubDim / pixelsPerUnit;
		
		this.baseHeight = imageHeight / pixelsPerUnit;
		System.out.println(baseHeight);
		
		double startDistance = 0;
		double endDistance = subFenceUnitDistance;

		List<LatLon> subPoints = new LinkedList<LatLon>();
		subPoints.add(globe.computePositionFromPoint(p[0]));
		
		System.out.println("Creating SubFences");
		
		i = 1;
		while (i < p.length) {
			// If the next point is too far
			if (pDistance[i] >= endDistance) {
				// Split the line and start a new subfence...
				Vec4 p1 = p[i - 1];
				Vec4 p2 = p[i];
				
				double d1 = pDistance[i - 1];
				double d2 = pDistance[i];

				double percent = (endDistance - d1) / (d2 - d1);
				
				Vec4 diff = p2.add3(p1.getNegative3());
				diff = diff.multiply3(percent);
				
				Vec4 mid = p1.add3(diff);
				LatLon midLatLon = globe.computePositionFromPoint(mid);
				subPoints.add(midLatLon);
				
				BufferedImage subImage = subSampleImage(startDistance, endDistance, totalDistance);
				children.add( new SubFence(subPoints, subImage) );
				
				subPoints = new LinkedList<LatLon>();
				subPoints.add(midLatLon);
				startDistance = endDistance;
				endDistance += subFenceUnitDistance;
			} 
			// Next point is within our width so just add it
			else 
			{
				// Add the current point
				subPoints.add(globe.computePositionFromPoint(p[i++]));
			}
		}
		
		BufferedImage subImage = subSampleImage(startDistance, totalDistance, totalDistance);
		if (subImage != null)
			children.add(new SubFence(subPoints, subImage));
		
		imageSource = null;
	}


	private BufferedImage subSampleImage(double startDistance, double endDistance,
			double totalDistance) {
		int x1 = (int) (startDistance / totalDistance * imageWidth);
		int x2 = (int) (endDistance / totalDistance * imageWidth);
		
		if (x2 == x1) return null;
		
		System.out.println("SubSampling " + (x2-x1) + " by " + maxSubDim + " from " + imageWidth + " by " + imageHeight + " " + (int) (100 * startDistance / totalDistance) + "%");
		BufferedImage subImage = new BufferedImage(x2 - x1, maxSubDim, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = subImage.createGraphics();
		g.drawImage(imageSource, 0, 0, x2 - x1, maxSubDim, x1, 0, x2, imageHeight, null);
//		g.setStroke(new BasicStroke(3));
//		g.setColor(Color.black);
//		g.drawRect(0, 0, maxSubDim, maxSubDim);
		return subImage;
	}

	public void setOpacity(float opacity) {
		this.opacity = opacity;
	}
	
	public float getOpacity() {
		return opacity;
	}
	
	public Color getColor()
	{
		return color;
	}

	public void setColor(Color color)
	{
		if (color == null)
		{
			String msg = Logging.getMessage("nullValue.ColorIsNull");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}

		this.color = color;
	}

	public void setImageSource(BufferedImage imageSource)
	{
		if (imageSource == null)
		{
			for (SubFence fence : children)
				fence.dispose();
			children.clear();
			this.imageSource = null;
			return;
		}
		
		this.imageHeight = imageSource.getHeight();
		this.imageWidth = imageSource.getWidth();
		this.imageSource = imageSource;
		this.maxSubDim = Math.min(MAX_DIM, imageHeight);

		createSubFences();
	}

	public int getAntiAliasHint()
	{
		return antiAliasHint;
	}

	public void setAntiAliasHint(int hint)
	{
		if (!(hint == GL.GL_DONT_CARE || hint == GL.GL_FASTEST || hint == GL.GL_NICEST))
		{
			String msg = Logging.getMessage("generic.InvalidHint");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}

		this.antiAliasHint = hint;
	}

	public double getElevation()
	{
		return elevation;
	}

	public void setElevation(double elevation)
	{
		this.elevation = elevation;
		
		for (SubFence fence : children) {
			fence.vertices = null;
			fence.crossLine = null;
		}
	}

	public void setBaseHeight(double baseHeight) {
		this.baseHeight = baseHeight;
		
		for (SubFence fence : children) {
			fence.vertices = null;
			fence.crossLine = null;
		}
	}
	
	public double getBaseHeight() {
		return baseHeight;
	}
	
	public void setExageration(double exageration) {
		this.exageration = exageration;
		
		for (SubFence fence : children) {
			fence.vertices = null;
			fence.crossLine = null;
		}
	}

	public double getExageration() {
		return exageration;
	};

	public void setYMax(float max) {
		setYRange(new float[] {yMin, max});
	}

	public void setYMin(float min) {
		setYRange(new float[] {min, yMax});
	}

	public void setYRange(float[] yRange) {
		this.yMin = yRange[0];
		this.yMax = yRange[1];
		for (SubFence sub : children)
		{
			sub.vertices = null;
			sub.textureCoordinates = null;
		}
	}

	public void render(DrawContext dc)
	{
		if (dc == null)
		{
			String message = Logging.getMessage("nullValue.DrawContextIsNull");
			Logging.logger().severe(message);
			throw new IllegalStateException(message);
		}

		if (this.children != null)
		{
			for (SubFence fd : children)
				fd.render(dc);
		}
	}

	public void dispose() {
		points.clear();
		imageSource = null;

		GLContext context = ww.getSceneController().getDrawContext().getGLContext();
		int i = context.makeCurrent();
		if (i == GLContext.CONTEXT_CURRENT) {
			for (SubFence sf : children) {
				if (sf.texture != null)  {
					((Disposable) sf.texture).dispose();
					System.out.println("Texture disposed");
				}
				sf.texture = null;

				sf.dispose();
			}
			context.release();
		} else
			System.err.println("Could not make GLContext current");

		children.clear();
	}

	public List<LatLon> getPositions() {
		return points;
	}

	public void setDrawLineAt(float linePercent) {
		this.crossLinePercent = linePercent;

		for (SubFence fence : children) {
			fence.vertices = null;
			fence.crossLine = null;
		}
	}
}

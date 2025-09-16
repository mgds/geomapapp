package haxby.worldwind.renderers;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import haxby.db.fms.Earthquakes.Earthquake;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;

public class FMSRenderer {
	
	private static final int MAX_DRAWN_FMS = 100;
	protected GLUquadric quadric;
	protected PickSupport pickSupport = new PickSupport();
	
	public FMSRenderer()
	{
	}
	
	public void render(DrawContext dc, Iterable<FMS> solutions, boolean extrude) {
		if (solutions == null)
			return;
		
		draw(dc, solutions, extrude);
	}
	
	public void pick(DrawContext dc, List<FMS> solutions, boolean isExtruded,
			Point point, Layer layer) {
		
		if (solutions == null)
			return;
		
		pickSupport.clearPickList();
		draw(dc, solutions, isExtruded);
		pickSupport.resolvePick(dc, point, layer);
		pickSupport.clearPickList();
		
	}

	protected Vec4 computeSurfacePoint(DrawContext dc, Position pos)
	{
		// Compute points that are at the track-specified elevation
		Vec4 point = dc.getSurfaceGeometry().getSurfacePoint(pos);
		if (point != null)
			return point;

		// Point is outside the current sector geometry, so compute it from the globe.
		return dc.getGlobe().computePointFromPosition(pos);
	}
	
	protected Vec4 draw(DrawContext dc, Iterable<FMS> solutions, boolean extrude)
	{
		if (dc.getVisibleSector() == null)
			return null;

		SectorGeometryList geos = dc.getSurfaceGeometry();
		if (geos == null)
			return null;
		
		List<Vec4> points = new ArrayList<Vec4>();
		for (FMS fms : solutions)
		{
			if (!fms.isInitialized)
				fms.initialize(dc);
			
			
			double elevation = extrude ?
					fms.depth * dc.getVerticalExaggeration()
					: 0;
			
			Vec4 pnt = 
				this.computeSurfacePoint(dc, 
						Position.fromDegrees(
								fms.lat, 
								fms.lon, 
								elevation));
			
			points.add(pnt);
		}

		if (points.size() < 1)
			return null;

		int drawCount = 0;
		
		this.begin(dc);
		{
			Vec4 cameraPosition = dc.getView().getEyePoint();
//			this.junctionMaterial.apply(dc.getGL(), GL.GL_FRONT);
			Iterator<FMS> solution = solutions.iterator();

			for (Vec4 point : points)
			{
				FMS fms = solution.next();
				if (!dc.getVisibleSector().contains(LatLon.fromDegrees(fms.lat, fms.lon)))
					continue;
				
				double dist = cameraPosition.distanceTo3(point);
				
//				fms.render(dc, point, 4000);

				fms.render(dc, point, dist/ 75);
				
				if (extrude)
					drawLine(dc, 
							point, 
							this.computeSurfacePoint(dc, 
									Position.fromDegrees(
										fms.lat, 
										fms.lon, 0)));
				
				drawCount++;
				
				if (drawCount > MAX_DRAWN_FMS)
					break;
			}
		}
		this.end(dc);
		
		return null; // TODO: return the last-drawn location
	}
	
	private void drawLine(DrawContext dc, Vec4 point, Vec4 next) {
//		GL gl = dc.getGL();
		GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
		gl.glColor3f(1, 1, 0);
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3d(point.x, point.y, point.z);
		gl.glVertex3d(next.x, next.y, next.z);
		gl.glEnd();
	}

	protected void begin(DrawContext dc)
	{
		GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
		//GL gl = dc.getGL();

		gl.glPushAttrib(
			GL2.GL_TEXTURE_BIT | 
			GL2.GL_ENABLE_BIT | 
			GL2.GL_CURRENT_BIT | 
			GL2.GL_LIGHTING_BIT | 
			GL2.GL_TRANSFORM_BIT |
			GL.GL_DEPTH_FUNC);
		
		
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glDisable(GL2.GL_LIGHTING);
		
		if (dc.isPickingMode())
			pickSupport.beginPicking(dc);
		else
		{
			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		}	

//		gl.glDisable(GL.GL_DEPTH_TEST);
//		gl.glDepthFunc(GL.GL_ALWAYS);
		
//		gl.glDisable(GL.GL_COLOR_MATERIAL);

//		gl.glLightfv(GL.GL_LIGHT1, GL.GL_POSITION, lightPosition, 0);
//		gl.glLightfv(GL.GL_LIGHT1, GL.GL_DIFFUSE, lightDiffuse, 0);
//		gl.glLightfv(GL.GL_LIGHT1, GL.GL_AMBIENT, lightAmbient, 0);
//		gl.glLightfv(GL.GL_LIGHT1, GL.GL_SPECULAR, lightSpecular, 0);
//
//		gl.glDisable(GL.GL_LIGHT0);
//		gl.glEnable(GL.GL_LIGHT1);
//		gl.glEnable(GL.GL_LIGHTING);
//		gl.glEnable(GL.GL_NORMALIZE);


		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		
	}

	protected void end(DrawContext dc)
	{
		if (dc.isPickingMode())
			pickSupport.endPicking(dc);
		
		//GL gl = dc.getGL();
		GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPopMatrix();

//		gl.glDisable(GL.GL_LIGHT1);
//		gl.glEnable(GL.GL_LIGHT0);
//		gl.glDisable(GL.GL_LIGHTING);
//		gl.glDisable(GL.GL_NORMALIZE);
		gl.glPopAttrib();
	}
	
	public class FMS
	{
		protected Earthquake eq;
		protected int glListId;
		protected boolean isInitialized = false;
		protected float strike, strike2,
			dip, dip2,
			rake, rake2,
			lat, lon, depth;
		protected Position position;

		public FMS(float lat, float lon, float depth, float strike, float dip, float rake, float strike2, float dip2, float rake2)
		{
			this.lat = lat;
			this.lon = lon;
			this.depth = depth;
			this.strike = strike;
			this.strike2 = strike2;
			this.dip = dip;
			this.dip2 = dip2;
			this.rake = rake;
			this.rake2 = rake2;
			
			position = Position.fromDegrees(lat, lon, 0);
		}
		
		public FMS(Earthquake eq) {
			this(eq.lat,
					eq.lon,
					eq.depth,
					eq.strike1,
					eq.dip1,
					eq.rake1,
					eq.strike2,
					eq.dip2,
					eq.rake2);
			this.eq = eq;
		}

		protected void initialize(DrawContext dc)
		{
			GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
			this.glListId = gl.glGenLists(4);
			if (quadric == null)
			{
				quadric = dc.getGLU().gluNewQuadric();
				dc.getGLU().gluQuadricDrawStyle(quadric, GLU.GLU_FILL);
				dc.getGLU().gluQuadricNormals(quadric, GLU.GLU_SMOOTH);
				dc.getGLU().gluQuadricOrientation(quadric, GLU.GLU_OUTSIDE);
				dc.getGLU().gluQuadricTexture(quadric, false);
			}

			compileFMS(dc, strike, dip, rake, strike2, dip2, rake2);
//			drawFMS(dc, strike, dip, rake, strike2, dip2, rake2);

			this.isInitialized = true;
		}

		protected void dispose()
		{
			if (this.isInitialized)
			{
				this.isInitialized = false;

				GLContext glc = GLContext.getCurrent();
				if (glc == null)
					return;

				glc.getGL().getGL2().glDeleteLists(this.glListId, 4);

				this.glListId = -1;
			}
		}

		protected void render(DrawContext dc, Vec4 point, double radius)
		{
			dc.getView().pushReferenceCenter(dc, point);
			
			//GL gl = dc.getGL();
			GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

			gl.glScaled(radius, radius, radius);
			gl.glRotated(lon, 0, 1, 0);
			gl.glRotated(lat, -1, 0, 0);

//			drawFMS(dc, strike, dip, rake, strike2, dip2, rake2);
			
			if (dc.isPickingMode())
			{
				Color color = dc.getUniquePickColor();
				pickSupport.addPickableObject(color.getRGB(),
						eq, 
						position, 
						false);
				gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
			} else
				gl.glColor3f(1,1,1);
			
			gl.glCallList(this.glListId);
			gl.glCallList(this.glListId+1);
			
			if (!dc.isPickingMode())
				gl.glColor3f(0,0,0);
			gl.glCallList(this.glListId+2);
			gl.glCallList(this.glListId+3);
			
//			GL gl = dc.getGL();
//			gl.glBegin(GL.GL_LINES);
//			gl.glVertex2f(0, 4);
//			gl.glVertex2d(0, 0);
//			gl.glEnd();
			
			dc.getView().popReferenceCenter(dc);
		}
		
		private void compileFMS(DrawContext dc, float strike, float dip, float rake, float strike2, float dip2, float rake2) 
		{
			GLU glu = dc.getGLU(); 
			//GL gl = dc.getGL();
			GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
			
			double[] a = {0,0,1, 0};
			double[] b = {0,0,-1, 0};
			
			double radius = .95;
			int slices = 36;
			int stacks = 18;
			
			double[][][] planes 
				= new double[][][] { {a,a}, {b,a}, {a,b}, {b,b}};
			
			for (int i = 0; i < 4; i++)
			{
				gl.glNewList(glListId + i, GL2.GL_COMPILE);
				
				gl.glPushMatrix();
				gl.glRotatef(strike, 0, 0, -1);
				gl.glRotatef(dip, 0, -1, 0);
				
				gl.glClipPlane(GL2.GL_CLIP_PLANE0, planes[i][0], 0);
				gl.glPopMatrix();
				
				gl.glPushMatrix();
				gl.glRotatef(strike2, 0, 0, -1);
				gl.glRotatef(dip2, 0, -1, 0);
				
				gl.glClipPlane(GL2.GL_CLIP_PLANE1, planes[i][1], 0);
				gl.glPopMatrix();

				gl.glEnable(GL2.GL_CLIP_PLANE0);
				gl.glEnable(GL2.GL_CLIP_PLANE1);
				
				glu.gluSphere(quadric, radius, slices, stacks);

				gl.glDisable(GL2.GL_CLIP_PLANE0);
				gl.glDisable(GL2.GL_CLIP_PLANE1);
				
				gl.glEndList();
			}
		}
		
		private void drawFMS(DrawContext dc, float strike, float dip, float rake, float strike2, float dip2, float rake2) 
		{
			GLU glu = dc.getGLU(); 
			//GL gl = dc.getGL();
			GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
			
			boolean drawPlanes = true;
			boolean drawLines = false;
			
			double[] a = {0,0,1, 0};
			double[] b = {0,0,-1, 0};
			
			double radius = .95;
			int slices = 36;
			int stacks = 18;
			
			gl.glPushMatrix();
			gl.glRotatef(strike, 0, 0, -1);
			gl.glRotatef(dip, 0, -1, 0);
			
			gl.glColor3f(0,1,0);
			
			if (drawPlanes)
				glu.gluDisk(quadric, 0, 1, 32, 32);
			
			gl.glClipPlane(GL2.GL_CLIP_PLANE0, a, 0);
			gl.glClipPlane(GL2.GL_CLIP_PLANE1, b, 0);
			
			gl.glRotatef(-rake, 0, 0, -1);
			
			if (drawLines)
			{
				gl.glBegin(GL.GL_LINES);
				gl.glVertex2d(0, 0);
				gl.glVertex2d(0, 1.5);
				gl.glEnd();
			}
			gl.glPopMatrix();
			
			gl.glPushMatrix();
			gl.glRotatef(strike2, 0, 0, -1);
			gl.glRotatef(dip2, 0, -1, 0);
			
			gl.glColor3f(1,0,0);
			
			if (drawPlanes)
				glu.gluDisk(quadric, 0, 1, 32, 32);
			
			gl.glClipPlane(GL2.GL_CLIP_PLANE2, a, 0);
			gl.glClipPlane(GL2.GL_CLIP_PLANE3, b, 0);
			
			gl.glRotatef(rake2, 0, 0, 1);
			if (drawLines)
			{
				gl.glBegin(GL.GL_LINES);
				gl.glVertex2d(0, 0);
				gl.glVertex2d(0, 1.5);
				gl.glEnd();
			}
			gl.glPopMatrix();
			
//			gl.glColor3f(0, 0, 0);
//			glu.gluSphere(quadratic, .95f, 32, 32);
			
//			if (true) return;
			
			gl.glEnable(GL2.GL_CLIP_PLANE2);
			{
				gl.glEnable(GL2.GL_CLIP_PLANE0);
				
				gl.glColor3f(1, 1, 1);
				glu.gluSphere(quadric, radius, slices, stacks);
				
				gl.glDisable(GL2.GL_CLIP_PLANE0);
				
				gl.glEnable(GL2.GL_CLIP_PLANE1);
				
//				gl.glColor3f(.4f,.4f,.4f);
				gl.glColor3f(0,0,0);
				
				glu.gluSphere(quadric, radius, slices, stacks);
				
				gl.glDisable(GL2.GL_CLIP_PLANE1);
			}
			gl.glDisable(GL2.GL_CLIP_PLANE2);
			
			gl.glEnable(GL2.GL_CLIP_PLANE3);
			{
				gl.glEnable(GL2.GL_CLIP_PLANE0);
				
				gl.glColor3f(0,0,0);
//				gl.glColor3f(.4f,.4f,.4f);
				glu.gluSphere(quadric, radius, slices, stacks);
				
				gl.glDisable(GL2.GL_CLIP_PLANE0);
				
				gl.glEnable(GL2.GL_CLIP_PLANE1);
				
				gl.glColor3f(1, 1, 1);
				glu.gluSphere(quadric, radius, slices, stacks);
				
				gl.glDisable(GL2.GL_CLIP_PLANE1);
			}
			gl.glDisable(GL2.GL_CLIP_PLANE3);
		}
	}

	public void dispose(List<FMS> toDispose) {
		for (FMS fms : toDispose)
			fms.dispose();
	}
}

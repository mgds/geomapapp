package haxby.worldwind.awt;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.LinkedList;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

public class LassoSelectionHandler extends AbstractLayer
						implements MouseListener, MouseMotionListener {

	private WorldWindowGLCanvas wwd;
	private View view;
	private boolean isLasso = false;
	private Point prevPoint;
	private Point mousePoint;

	private List<Position> lasso = new LinkedList<Position>();
	private List<LassoSelectListener> listeners = new LinkedList<LassoSelectListener>();
	private LassoRenderable lassoRender = new LassoRenderable();

	private boolean isLDown = false;
	private boolean enabled = false;

	public LassoSelectionHandler(WorldWindowGLCanvas wwd) {
		this.wwd = wwd;
		this.view = wwd.getView();

		wwd.getInputHandler().addMouseListener(this);
		wwd.getInputHandler().addMouseMotionListener(this);
		wwd.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {
		
			}
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_L)
					isLDown = false;
			}
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_L)
					isLDown = true;
			}
		});
	}

	public class LassoRenderable implements OrderedRenderable {
		public void pick(DrawContext dc, Point pickPoint) {
		}

		public void render(DrawContext dc) {
			beginDrawLasso(dc);
			
			//GL gl = dc.getGL();
			GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
			gl.glLoadIdentity();

			gl.glColor3f(1, 1, 0);
			gl.glBegin(GL.GL_LINE_STRIP);

			Vec4 startPoint = null;

			for (Position pos : lasso)
			{
				Vec4 screenPoint = dc.getView().project(
						dc.getGlobe().computePointFromPosition(pos));

				if (screenPoint == null)
					continue;
				if (startPoint == null)
					startPoint = screenPoint;

				gl.glVertex3d(screenPoint.x, screenPoint.y, startPoint.z);
			}
			if (startPoint != null) 
				gl.glVertex3d(startPoint.x, startPoint.y, startPoint.z);

			gl.glEnd();
			endDrawLasso(dc);
		}

		public double getDistanceFromEye() {
			return 0;
		}
	}
	
	protected void doRender(DrawContext dc) {
		if (lasso.size() > 1)
			dc.addOrderedRenderable(lassoRender);
	}

	private void beginDrawLasso(DrawContext dc) {
		//GL gl = dc.getGL();
		GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

		int attributeMask =
			GL.GL_DEPTH_BUFFER_BIT // for depth test, depth mask and depth func
				| GL2.GL_TRANSFORM_BIT // for modelview and perspective
				| GL2.GL_VIEWPORT_BIT // for depth range
				| GL2.GL_CURRENT_BIT // for current color
				| GL2.GL_COLOR_BUFFER_BIT // for alpha test func and ref, and blend
				| GL2.GL_DEPTH_BUFFER_BIT // for depth func
				| GL2.GL_ENABLE_BIT; // for enable/disable changes
		gl.glPushAttrib(attributeMask);

		// Apply the depth buffer but don't change it.
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_ALWAYS);
		gl.glDepthMask(false);

		// Load a parallel projection with dimensions (viewportWidth, viewportHeight)
		int[] viewport = new int[4];
		gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glOrtho(0d, viewport[2], 0d, viewport[3], -1d, 1d);

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
	}

	private void endDrawLasso(DrawContext dc) {
		//GL gl = dc.getGL();
		GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPopMatrix();

		gl.glPopAttrib();
	}
	
	private Point constrainPointToComponentBounds(int x, int y, Component c)
	{
		if (c != null)
		{
			if (x < 0)
				x = 0;
			if (y < 0)
				y = 0;

			if (x > c.getWidth())
				x = c.getWidth();
			if (y > c.getHeight())
				y = c.getHeight();
		}

		return new Point(x, y);
	}
	
	private void updateMousePoint(MouseEvent event)
	{
		if (event != null)
		{
			if (this.wwd instanceof Component)
				this.mousePoint = constrainPointToComponentBounds(event.getX(), event.getY(), (Component) this.wwd);
			else
				this.mousePoint = new Point(event.getX(), event.getY());
		}
		else
		{
			this.mousePoint = null;
		}
	}

	private boolean testLassoConditions(MouseEvent e) {
		int onmask = InputEvent.BUTTON1_DOWN_MASK;
		int offmask = InputEvent.CTRL_DOWN_MASK
						| InputEvent.SHIFT_DOWN_MASK;
		return (e.getModifiersEx() & (onmask | offmask)) == onmask
				&& (enabled  || isLDown);
	}

	private void pushMousePoint()
	{
		Position pos = view.computePositionFromScreenPoint(mousePoint.x, mousePoint.y);
		if (pos != null)
			lasso.add(pos);
	}

	private void beginLasso(MouseEvent e)
	{
		isLasso = true;

		updateMousePoint(e);
		pushMousePoint();
	}

	private boolean isLassoing()
	{
		return isLasso;
	}

	private void endLasso(MouseEvent e)
	{
		updateMousePoint(e);
		pushMousePoint();
		
		isLasso = false;

		for (LassoSelectListener listener : listeners)
			listener.selectLasso(lasso);
		
		lasso.clear();
	}

	private void updateLasso(MouseEvent e)
	{
		prevPoint = mousePoint;
		updateMousePoint(e);
		
		if (prevPoint != null &&
				mousePoint != null &&
				prevPoint.distance(mousePoint) < 4)
			mousePoint = prevPoint;
		else
			pushMousePoint();
	}

	public void mouseClicked(MouseEvent e) {
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
	}
	public void mouseMoved(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		if (testLassoConditions(e))
			beginLasso(e);
	}

	public void mouseReleased(MouseEvent e) {
		if (isLassoing())
			endLasso(e);
	}

	public void mouseDragged(MouseEvent e) {
		if (isLassoing()) {
			updateLasso(e);
			e.consume();
		}
	}

	public void addSelectionListener(LassoSelectListener listener) {
		listeners.add(listener);
	}

	public void removeSelectionListener(LassoSelectListener listener) {
		listeners.remove(listener);
	}

	public interface LassoSelectListener {
		public void selectLasso(List<Position> area);
	}

	public void setLassoEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}

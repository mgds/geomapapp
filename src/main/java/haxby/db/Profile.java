package haxby.db;

import haxby.map.*;
import haxby.proj.*;
import haxby.grid.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import java.util.*;

public class Profile extends JComponent
		implements Overlay, MouseListener, MouseMotionListener, ActionListener {
	XMap map;
	XGrid_Z grid;
	JFrame frame;
	Point2D.Double p1, p2;
	Point2D.Double map1, map2;
	float[] z;
	float max, min;
	double xMax;
	Font font;

	static double[] incr = {2, 2.5, 2};

	Line2D.Double mapLine;

	double X0;
	Shape currentShape;

	public Profile(XMap map, XGrid_Z grid) {
		this.map = map;
		this.grid = grid;
		z = null;
		frame = new JFrame("Profile");
		frame.setSize(600,400);
		frame.getContentPane().add(this, "Center");
	//	JPanel panel = new JPanel();
	//	panel.add(info);
	//	frame.getContentPane().add(panel, "North");
		mapLine = new Line2D.Double();
		mapLine.x1 = -100;
		map.addMouseMotionListener(this);
		map.addMouseListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		map.addOverlay(this);
		font = new Font("SansSerif",Font.PLAIN, 10);
		currentShape=null;
	}
	public void setMap( XMap map) {
		z = null;
		this.map.removeMouseMotionListener(this);
		this.map.removeMouseListener(this);
		this.map = map;
		map.addMouseMotionListener(this);
		map.addMouseListener(this);
	}
	public void setGrid( XGrid_Z grid) {
		z = null;
		this.grid = grid;
	}
	public void setProfile(Point2D pt1, Point2D pt2) {
		if(grid==null)return;
		X0 = 0;
		Projection mProj = map.getProjection();
		Projection gProj = grid.getProjection();
		double zoom = map.getZoom();
		p1 = (Point2D.Double) mProj.getRefXY(pt1);
		p2 = (Point2D.Double) mProj.getRefXY(pt2);
		map1 = (Point2D.Double) mProj.getMapXY(p1);
		map2 = (Point2D.Double) mProj.getMapXY(p2);
		xMax = Math.pow(p2.y-p1.y, 2) + Math.pow( (p2.x-p1.x) *
					Math.cos(Math.toRadians( .5*(p1.y+p2.y) )), 2);
		xMax = Math.sqrt(xMax) * 111.2;
		Point2D.Double x1 = (Point2D.Double)mProj.getMapXY(p1);
		Point2D.Double x2 = (Point2D.Double)mProj.getMapXY(p2);
		int n = (int) (x1.distance(x2)*map.getZoom()) + 1;
		if( n<3 ) {
			z = null;
			map.repaint();
			repaint();
			return;
		}
		double dx = (x2.x - x1.x) / (n-1);
		double dy = (x2.y - x1.y) / (n-1);
		z = new float[n];
		min = 1000000f;
		max = -1000000f;
		Point2D.Double p;
		for( int i=0 ; i<n ; i++) {
			p = (Point2D.Double)mProj.getRefXY(x1);
			z[i] = grid.valueAtRef(p.x, p.y);
			x1.x += dx;
			x1.y += dy;
			if(!Float.isNaN(z[i])) {
				if(z[i] > max) max = z[i];
				if(z[i] < min) min = z[i];
			}
		}
		if(max <= min) {
			z = null;
			map.repaint();
			repaint();
			return;
		}
		if(frame.isVisible()) {
			repaint();
		} else {
			Dimension size = frame.getSize();
			frame.pack();
			frame.setSize(size);
			frame.setVisible(true);
		}
		X0 = xMax/2;
		if(frame.getState() == Frame.ICONIFIED)frame.setState(Frame.NORMAL);
		frame.toFront();
		map.repaint();
	}
	public void draw(Graphics2D g2) {
		if(z == null) return;
		currentShape=null;
		Line2D.Double line = new Line2D.Double();
		line.x1 = map1.x;
		line.y1 = map1.y;
		line.x2 = map2.x;
		line.y2 = map2.y;
		g2.setColor(Color.black);
		double zoom = map.getZoom();
		g2.setStroke( new BasicStroke( 2f/(float)zoom ));
		g2.draw(line);
	}
	public void paint(Graphics g) {
		if(z == null) {
			g.drawString("No Profile", 50,50);
			return;
		}
		int n = z.length;
		Dimension size = getSize();
		float xScale = (float)(size.width-80) / (float) (n-1);
		float yScale = (float)(size.height-60) / (max - min);
		boolean plot = false;
		GeneralPath gp = new GeneralPath();
		for( int i=0 ; i<n ; i++) {
			if(Float.isNaN(z[i])) {
				plot = false;
			} else if(plot) {
				gp.lineTo( 70f + xScale * (float)i,
					(float)(size.height-50) - yScale * (z[i] - min));
				plot = true;
			} else {
				gp.moveTo( 70f + xScale * (float)i,
					(float)(size.height-50) - yScale * (z[i] - min));
				plot = true;
			}
		}
		g.setColor(Color.black);
		Graphics2D g2d = (Graphics2D)g;
		g2d.draw(gp);
		g2d.setStroke(new BasicStroke(2f));
		g.drawLine(70, size.height-50, 70, 10);
		g.drawLine(70, size.height-45, size.width-5, size.height-45);
		g2d.setStroke(new BasicStroke(1f));
		double xInc = 1;
		int k = 0;
		while (Math.floor(xMax/xInc) > 5) {
			xInc *= incr[k];
			k = (k+1)%3;
		}
		xInc = Math.rint(xInc);
		double xAnot = 0;
		while(xAnot+X0 < xMax) {
			gp = new GeneralPath();
			float xGraph = 70f + (float)(size.width-80) * (float)((X0+xAnot)/xMax);
			gp.moveTo(xGraph, size.height-45);
			gp.lineTo(xGraph, size.height-50);
			g2d.draw(gp);
			String s = Integer.toString((int)xAnot);
			g.setFont(font);
			FontMetrics fm = getFontMetrics(font);
			int h = fm.getHeight();
			int w = fm.stringWidth(s);
			g.drawString(s, (int)xGraph - w/2, size.height-45 + h);
			xAnot += xInc;
		}
		xAnot = -xInc;
		while(xAnot+X0 > 0) {
			gp = new GeneralPath();
			float xGraph = 70f + (float)(size.width-80) * (float)((X0+xAnot)/xMax);
			gp.moveTo(xGraph, size.height-45);
			gp.lineTo(xGraph, size.height-50);
			g2d.draw(gp);
			String s = Integer.toString((int)xAnot);
			g.setFont(font);
			FontMetrics fm = getFontMetrics(font);
			int h = fm.getHeight();
			int w = fm.stringWidth(s);
			g.drawString(s, (int)xGraph - w/2, size.height-45 + h);
			xAnot -= xInc;
		}
		double yInc = 1;
		k = 0;
		while( Math.floor((double)max / yInc) - Math.ceil((double)min / yInc) > 5) {
			yInc *= incr[k];
			k = (k+1)%3;
		}
		yInc = Math.rint(yInc);
		double yAnot = Math.ceil((double)min/yInc) *yInc;
		while(yAnot < (double)max) {
			gp = new GeneralPath();
			float yGraph = (float)(size.height-50) - yScale * ((float)yAnot - min);
			gp.moveTo(70f, yGraph);
			gp.lineTo(75f, yGraph);
			((Graphics2D)g).draw(gp);
			String s = Integer.toString(-(int)yAnot);
			g.setFont(font);
			FontMetrics fm = getFontMetrics(font);
			int h = fm.getHeight();
			int w = fm.stringWidth(s);
			g.drawString(s, 66-w, (int)yGraph + h/2);
			yAnot += yInc;
		}
		String s = "km";
		g.setFont(font);
		FontMetrics fm = getFontMetrics(font);
		int h = fm.getHeight();
		int w = fm.stringWidth(s);
		g.drawString(s, 70+size.width/2 - w/2, size.height-5);
		s = "Depth, m";
		g.drawString(s, 5, 15);
	}
	void drawShape() {
		if(currentShape != null) {
			synchronized(map.getTreeLock()) {
				Graphics2D g = map.getGraphics2D();
				g.setXORMode( Color.white );
				g.setStroke( new BasicStroke( 1f/(float)map.getZoom() ) );
				g.draw(currentShape);
			}
		}
	}
	void setShape(Shape shape) {
		drawShape();
		currentShape = shape;
		drawShape();
	}
	public void mouseEntered(MouseEvent e) {
		if(e.getSource() == this)requestFocus();
	}
	public void mouseExited( MouseEvent e ) {
		mapLine.x1 = -100;
		setShape(null);
	}
	public void mouseClicked( MouseEvent e ) {
		if( e.isControlDown() || e.getSource() == map) return;
		mapLine.x1 = -100;
		setShape(null);
		if( e.getSource() == this) {
			Dimension size = getSize();
			if(e.getX() > 70 && e.getX() <= size.getWidth()-10) {
				X0 = xMax * (double)(e.getX()-70) / (size.getWidth()-80);
				repaint();
			}
		}
	}
	public void mousePressed( MouseEvent e ) {
		if( e.getSource() == map) {
			if(e.isShiftDown()) {
				Point2D p = map.getScaledPoint( e.getPoint() );
				mapLine.x1 = p.getX();
				mapLine.y1 = p.getY();
			} else {
				mapLine.x1 = -100;
				setShape(null);
				 return;
			}
		}
	}
	public void mouseReleased( MouseEvent e ) {
		if( e.getSource() == map) {
			if(e.isControlDown()) {
				mapLine.x1 = -100;
				setShape(null);
				return;
			} else if(e.isShiftDown()) {
				Point2D p = map.getScaledPoint( e.getPoint() );
				mapLine.x2 = p.getX();
				mapLine.y2 = p.getY();
				setProfile(new Point2D.Double(mapLine.x1, mapLine.y1),
					new Point2D.Double(mapLine.x2, mapLine.y2));
				setShape(null);
			}
		}
		mapLine.x1 = -100;
	}
	public void mouseDragged( MouseEvent e ) {
		if( e.getSource() == map && e.isShiftDown() ) {
			if(e.isControlDown()) {
				return;
			} else if(mapLine.x1 != -100) {
				Point2D p = map.getScaledPoint( e.getPoint() );
				mapLine.x2 = p.getX();
				mapLine.y2 = p.getY();
				setShape(mapLine);
			} else {
				Point2D p = map.getScaledPoint( e.getPoint() );
				mapLine.x1 = p.getX();
				mapLine.y1 = p.getY();
			}
		}
	}
	public void mouseMoved( MouseEvent e ) {
		mapLine.x1 = -100;
		Dimension size = getSize();
		if( e.getSource() == this) synchronized(map.getTreeLock()) {
			Graphics2D g = (Graphics2D) map.getGraphics();
			if(e.getX() < 70 || e.getX() > size.width - 10) {
				setShape(null);
				return;
			}
			double x = map1.x + (map2.x-map1.x) * (double)(e.getX()-70) / (size.getWidth()-80);
			double y = map1.y + (map2.y-map1.y) * (double)(e.getX()-70) / (size.getWidth()-80);
			int ix = (int)Math.rint(x);
			int iy = (int)Math.rint(y);
			setShape(new Rectangle(ix-4, iy-4, 8, 8));
		}
	}
	public void actionPerformed(ActionEvent e) {
	}
}

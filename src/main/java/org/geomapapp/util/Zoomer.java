package org.geomapapp.util;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.event.*;

public class Zoomer implements MouseListener, 
				MouseMotionListener,
				KeyListener {
	Zoomable z;
	Point p0;
	long when;
	static Cursor ZOOM_IN = Cursors.ZOOM_IN();
	static Cursor ZOOM_OUT = Cursors.ZOOM_OUT();

	public Zoomer(Zoomable z) {
		this.z = z;
		p0 = null;
		when = 0L;
	}
	public void setZoomable(Zoomable z) {
		this.z = z;
	}

	public void mouseClicked(MouseEvent e) {
		if(e.isControlDown()) {
		//	if(e.isShiftDown()) z.zoomOut(e.getPoint());
		//	else z.zoomIn(e.getPoint());
		}
		z.setRect(null);
		p0 = null;
	}
	public void mousePressed(MouseEvent e) {
		if(e.isControlDown()) {
			p0 = e.getPoint();
			when = e.getWhen();
		}
	}
	public void mouseReleased(MouseEvent e) {
		if(e.isControlDown()) {
			z.setRect(null);
			if(p0 == null) {
				return;
			}
			Point p = e.getPoint();
			if(e.getWhen() - when < 500L) {
				if(e.isShiftDown()) z.zoomOut(p0);
				else z.zoomIn(p0);
				p0 = null;
			} else {
				int w = (int) Math.abs(p.x-p0.x);
				int h = (int) Math.abs(p.y-p0.y);
				int x = (int) Math.min(p.x,p0.x);
				int y = (int) Math.min(p.y,p0.y);
				z.zoomTo(new Rectangle(x, y, w, h));
			}
		} else {
			z.setRect(null);
		}
		p0 = null;
	}
	public void mouseEntered(MouseEvent e) {
		z.setRect(null);
		z.setXY(e.getPoint());
		Component c;
		try {
			c = (Component)e.getSource();
		} catch (ClassCastException ex) {
			return;
		}
		c.requestFocus();
		if(e.isControlDown()) {
			if(e.isShiftDown()) {
				c.setCursor(ZOOM_OUT);
			} else {
				c.setCursor(ZOOM_IN);
			}
		} else {
			c.setCursor(Cursor.getDefaultCursor());
		}
	}
	public void mouseExited(MouseEvent e) {
		z.setRect(null);
		z.setXY(null);
		Component c;
		try {
			c = (Component)e.getSource();
		} catch (ClassCastException ex) {
			return;
		}
		c.setCursor(Cursor.getDefaultCursor());
	}
	public void mouseDragged(MouseEvent e) {
		z.setXY(e.getPoint());
		if(e.isControlDown()) {
			if(p0 == null) {
				p0 = e.getPoint();
				when = e.getWhen();
			} else {
				Point p = e.getPoint();
				int w = (int) Math.abs(p.x-p0.x);
				int h = (int) Math.abs(p.y-p0.y);
				int x = (int) Math.min(p.x,p0.x);
				int y = (int) Math.min(p.y,p0.y);
				z.setRect(new Rectangle(x, y, w, h));
			}
		} else {
			z.setRect(null);
		}
	}
	public void mouseMoved(MouseEvent e) {
		z.setRect(null);
		z.setXY(e.getPoint());
	}
	public void keyPressed(KeyEvent evt) {
		Component c;
		try {
			c = (Component)evt.getSource();
		} catch (ClassCastException ex) {
			return;
		}
		if(evt.getKeyCode() == KeyEvent.VK_CONTROL) {
			if(evt.isShiftDown()) {
				c.setCursor(ZOOM_OUT);
			} else {
				c.setCursor(ZOOM_IN);
			}
		} else if(evt.getKeyCode() == KeyEvent.VK_SHIFT) {
			if(evt.isControlDown()) {
				c.setCursor(ZOOM_OUT);
			}
		}
	}
	public void keyReleased(KeyEvent evt) {
		Component c;
		try {
			c = (Component)evt.getSource();
		} catch (ClassCastException ex) {
			return;
		}
		if(evt.getKeyCode() == KeyEvent.VK_CONTROL) {
			c.setCursor(Cursor.getDefaultCursor());
		} else if(evt.getKeyCode() == KeyEvent.VK_SHIFT) {
			if(evt.isControlDown()) {
				c.setCursor(ZOOM_IN);
			}
		}
	}
	public void keyTyped(KeyEvent evt) {
	//	char c = evt.getKeyChar();
	//	if( c=='R' ) {
	//		z.rotate(-1);
	//	} else if( c=='r' ) {
	//		z.rotate(1);
	//	}
	}
	static Cursor setZOOM_IN() {
		BufferedImage zoomImage = new BufferedImage(24,24,
			BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = zoomImage.createGraphics();
		g.setStroke(new BasicStroke(4.f));
		g.setColor(Color.white);
		g.draw(new Arc2D.Double(5,2,14,14,0,360,Arc2D.CHORD));
		g.draw(new Line2D.Double(8,9,16,9));
		g.draw(new Line2D.Double(12,5,12,13));
		g.draw(new Line2D.Double(12,16,12,20));
		g.setColor(Color.black);
		g.setStroke(new BasicStroke(2.f));
		g.draw(new Arc2D.Double(5,2,14,14,0,360,Arc2D.CHORD));
	//	g.draw(new Arc2D.Double(4.5,1.5,15,15,0,360,Arc2D.CHORD));
		g.draw(new Line2D.Double(8,9,16,9));
		g.draw(new Line2D.Double(12,5,12,13));
		g.draw(new Line2D.Double(12,16,12,20));
		return Toolkit.getDefaultToolkit().createCustomCursor(zoomImage, 
					new Point(12,9), "ZOOM_IN");
	}
	static Cursor setZOOM_OUT() {
		BufferedImage zoomImage = new BufferedImage(24,24,
			BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = zoomImage.createGraphics();
		g.setStroke(new BasicStroke(4.f));
		g.setColor(Color.white);
	//	g.draw(new Arc2D.Double(4.5,1.5,15,15,0,360,Arc2D.CHORD));
		g.draw(new Arc2D.Double(5,2,14,14,0,360,Arc2D.CHORD));
		g.draw(new Line2D.Double(8,9,16,9));
		g.draw(new Line2D.Double(12,16,12,20));
		g.setColor(Color.black);
		g.setStroke(new BasicStroke(2.f));
		g.draw(new Arc2D.Double(5,2,14,14,0,360,Arc2D.CHORD));
	//	g.draw(new Arc2D.Double(4.5,1.5,15,15,0,360,Arc2D.CHORD));
		g.draw(new Line2D.Double(8,9,16,9));
		g.draw(new Line2D.Double(12,16,12,20));
		return Toolkit.getDefaultToolkit().createCustomCursor(zoomImage, 
					new Point(12,9), "ZOOM_IN");
	}
}
package haxby.map;

import haxby.util.Cursors;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.IOException;
import javax.swing.JToggleButton;

import org.geomapapp.util.Icons;

public class Zoomer implements MouseListener,
							MouseMotionListener,
							KeyListener {
	Zoomable z;
	Point p0;
	long when,
		wheelValue,
		wStart,
		wEnd;
	static Cursor ZOOM_IN = Cursors.ZOOM_IN();
	static Cursor ZOOM_OUT = Cursors.ZOOM_OUT();
	JToggleButton zoomIn,zoomOut;

	public Zoomer(Zoomable z) {
		this.z = z;
		p0 = null;
		when = 0L;
		wheelValue = 0L;
	}
	public void setZoomable(Zoomable z) {
		this.z = z;
	}

	public JToggleButton getZoomIn() {
		if (zoomIn==null)	zoomIn = new JToggleButton(Icons.getIcon(Icons.ZOOM_IN, false));
		return zoomIn;
	}

	public JToggleButton getZoomOut() {
		if (zoomOut==null)	zoomOut = new JToggleButton(Icons.getIcon(Icons.ZOOM_OUT, false));
		return zoomOut;
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
		if(e.isControlDown()||(zoomIn!=null&&zoomIn.isSelected())||(zoomOut!=null&&zoomOut.isSelected())) {
			p0 = e.getPoint();
			when = e.getWhen();
		}
	}
	public void mouseReleased(MouseEvent e) {
		if(e.isControlDown()||(zoomIn!=null&&zoomIn.isSelected())||(zoomOut!=null&&zoomOut.isSelected())) {
			z.setRect(null);
			if(p0 == null) {
				return;
			}
			Point p = e.getPoint();

			// Zoom in or zoom out
			if(e.getWhen() - when < 250) {
				if (e.isShiftDown()||(zoomOut!=null&&zoomOut.isSelected()&&!e.isControlDown())) {
					// ZOOM OUT: cap off at 2^-2
					if( e.getSource() instanceof XMap) {
						// zoom history set first zoom checks it for the first time only
						((XMap) e.getSource()).setZoomHistoryPast((XMap) e.getSource());
						if(z.getZoomValue() > Math.pow(2, 0)) {
							// zoom out
							z.zoomOut(p0);
							// zoom history set next
							((XMap) e.getSource()).setZoomHistoryNext((XMap) e.getSource());
						}
					} else {
						z.zoomOut(p0);
					}
					// not needed anymore
					/*try {
						// update past and next on zoom out
						((XMap) e.getSource()).updateZoomHistory(((XMap) e.getSource()).getZoomHistoryPast(), ((XMap) e.getSource()).getZoomHistoryNext());
					} catch (IOException e1) {}
					*/
				}
				else {
					// ZOOM IN: cap off at 2^20
					if( e.getSource() instanceof XMap) {
						// zoom history set first zoom checks it for the first time only
						((XMap) e.getSource()).setZoomHistoryPast((XMap) e.getSource());
						if(z.getZoomValue() < Math.pow(2, 20)) {
							// zoom in
							z.zoomIn(p0);
							// zoom history set next
							((XMap) e.getSource()).setZoomHistoryNext((XMap) e.getSource());
						}
					} else {
						z.zoomIn(p0);
					}
				}
				p0 = null;
			}
			// Or ZOOM TO
			else {
				int w = (int) Math.abs(p.x-p0.x);
				int h = (int) Math.abs(p.y-p0.y);
				int x = (int) Math.min(p.x,p0.x);
				int y = (int) Math.min(p.y,p0.y);
				// Zoom TO RECT: cap off at 2^20
				if(z.getZoomValue() < Math.pow(2, 20)) {
					// zoom history set past
					((XMap) e.getSource()).setZoomHistoryPast((XMap) e.getSource());
					// zoom to rect
					z.zoomTo(new Rectangle(x, y, w, h));
					// zoom history set next
					((XMap) e.getSource()).setZoomHistoryNext((XMap) e.getSource());
				}
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

//		***** Changed by A.K.M. 06/26/06 *****
//		c.requestFocus();
//		Commented out so that new JFrame grid dialog does not go into the 
//		background every time the mouse cursor enters the map
//		***** Changed by A.K.M. 06/26/06 *****

		if(e.isControlDown()||(zoomIn!=null&&zoomIn.isSelected())||(zoomOut!=null&&zoomOut.isSelected())) {
			if(e.isShiftDown()||(zoomOut!=null&&zoomOut.isSelected())) {
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
	//	z.setXY(e.getPoint());
		if(e.isControlDown()||(zoomIn!=null&&zoomIn.isSelected())||(zoomOut!=null&&zoomOut.isSelected())) {
			z.setXY(e.getPoint());
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
			if ((zoomIn!=null&&!zoomIn.isSelected())&&(zoomOut!=null&&!zoomOut.isSelected()))
				c.setCursor(Cursor.getDefaultCursor());
			else if (zoomIn!=null&&zoomIn.isSelected())
				c.setCursor(ZOOM_IN);
			else if (zoomOut!=null&&zoomOut.isSelected())
				c.setCursor(ZOOM_OUT);
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

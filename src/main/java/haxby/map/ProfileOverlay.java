package haxby.map;

import haxby.proj.*;
import haxby.grid.*;
import haxby.db.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

public class ProfileOverlay extends JComponent
			implements MouseListener,
				MouseMotionListener,
	//			XYPoints,
				Overlay {
	XMap map;
	GridOverlay grid;
	Line2D.Double line;
	Point2D point;
	public ProfileOverlay( XMap map, GridOverlay grid ) {
		this.map = map;
		this.grid = grid;
		init();
	}
	protected void start() {
		map.addMouseListener(this);
		map.addMouseMotionListener(this);
		map.setCursor( Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) );
	}
	protected void stop() {
		map.removeMouseListener(this);
		map.removeMouseMotionListener(this);
		map.setCursor( Cursor.getDefaultCursor());
		init();
	}
	protected void init() {
		line = null;
		point = null;
	}
	public void draw(Graphics2D g) {
		if( line==null )return;
		g.setColor( Color.black );
		g.setStroke( new BasicStroke(1f/(float)map.getZoom()) );
		if( point!=null ) g.setXORMode(Color.white);
		g.draw( line );
		if( point!=null) g.setPaintMode();
	}
	public void mouseEntered( MouseEvent evt ) {
	}
	public void mouseExited( MouseEvent evt ) {
	}
	public void mousePressed( MouseEvent evt ) {
	}
	public void mouseReleased( MouseEvent evt ) {
	}
	public void mouseClicked( MouseEvent evt ) {
		if( point==null ) {
			point = map.getScaledPoint( evt.getPoint() );
			return;
		}
		line = new Line2D.Double( point, map.getScaledPoint(evt.getPoint()));
		point = null;
		map.repaint();
	}
	public void mouseMoved( MouseEvent evt ) {
		if( evt.isControlDown() )return;
		if( map.getCursor()!=Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) ) {
			map.setCursor( Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) );
		}
		if( point==null ) return;
		drawLine();
		Point2D p = map.getScaledPoint( evt.getPoint() );
		line = new Line2D.Double( point, p);
		drawLine();
	}
	public void mouseDragged( MouseEvent evt ) {
	}
	void drawLine() {
		if( line == null )return;
		synchronized( map.getTreeLock() ) {
			Graphics2D g = map.getGraphics2D();
			g.setStroke( new BasicStroke(1f/(float)map.getZoom()) );
			g.setXORMode(Color.white);
			g.draw( line );
		}
	}
}

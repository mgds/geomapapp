package org.geomapapp.util;

import java.awt.event.*;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import javax.swing.JToggleButton;

public class ZoomOut extends javax.swing.event.MouseInputAdapter {
	Point lastP;
	ScalableComp sc;
	JToggleButton toggle;
	transient Point start;
	public ZoomOut(ScalableComp sc) {
		this.sc = sc;
	}
	public JToggleButton getToggle() {
		if( toggle==null ) {
			toggle = new JToggleButton( Icons.getIcon(Icons.ZOOM_OUT, false) );
			toggle.setSelectedIcon( Icons.getIcon(Icons.ZOOM_OUT, true) );
			toggle.addChangeListener( new javax.swing.event.ChangeListener() {
				public void stateChanged( javax.swing.event.ChangeEvent e) {
					setCursor();
				}
			});
		}
		return toggle;
	}
	public void setCursor() {
		if( toggle.isSelected() ) sc.setCursor( Cursors.getCursor( Cursors.ZOOM_OUT ) );
	}
	public void mouseClicked( MouseEvent e ) {
		if( toggle.isSelected() ) sc.doZoom( e.getPoint(), .5 );
	}
	public void mousePressed(MouseEvent e) {
		if( !toggle.isSelected() )return;
		start = e.getPoint();
	}
	public void mouseDragged(MouseEvent e) {
		if( !toggle.isSelected() )return;
		Rectangle shape = new Rectangle( start.x, start.y, 0, 0);
		shape.add(e.getPoint());
	//	if( shape.width<=10 || shape.height<=10 )return;
//System.out.println( shape.width +"\t"+ shape.height );
		sc.setShape( shape);
	}
	public void mouseReleased(MouseEvent e) {
		if( !toggle.isSelected() )return;
		Rectangle shape = new Rectangle( start.x, start.y, 0, 0);
		shape.add(e.getPoint());
		if( shape.width<=10 || shape.height<=10 ) {
			sc.setShape( null );
			return;
		}
		Dimension dim = sc.getSize();
		double factor = dim.width/shape.getWidth();
		double f = dim.height/shape.getHeight();
		if( f<factor )factor = f;
		double x = shape.getX() + .5*shape.getWidth();
		double y = shape.getY() + .5*shape.getHeight();
		sc.doZoom( new Point2D.Double(x,y), factor );
	}
}
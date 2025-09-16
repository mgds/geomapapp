package org.geomapapp.util;

import java.awt.event.*;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import javax.swing.JToggleButton;

public class Zoom extends javax.swing.event.MouseInputAdapter {
	Point lastP;
	ScalableComp sc;
	JToggleButton zoomIn, zoomOut;
	transient Point start;
	public Zoom(ScalableComp sc) {
		this.sc = sc;
	}
	public JToggleButton getZoomInToggle() {
		if( zoomIn==null ) {
			zoomIn = new JToggleButton( Icons.getIcon(Icons.ZOOM_IN, false) );
			zoomIn.setSelectedIcon( Icons.getIcon(Icons.ZOOM_IN, true) );
			zoomIn.addChangeListener( new javax.swing.event.ChangeListener() {
				public void stateChanged( javax.swing.event.ChangeEvent e) {
					setCursor();
				}
			});
		}
		return zoomIn;
	}
	public JToggleButton getZoomOutToggle() {
		if( zoomOut==null ) {
			zoomOut = new JToggleButton( Icons.getIcon(Icons.ZOOM_OUT, false) );
			zoomOut.setSelectedIcon( Icons.getIcon(Icons.ZOOM_OUT, true) );
			zoomOut.addChangeListener( new javax.swing.event.ChangeListener() {
				public void stateChanged( javax.swing.event.ChangeEvent e) {
					setCursor();
				}
			});
		}
		return zoomOut;
	}
	public void setCursor() {
		if( zoomIn.isSelected() ) sc.setCursor( Cursors.getCursor( Cursors.ZOOM_IN ) );
		else if( zoomOut.isSelected() ) sc.setCursor( Cursors.getCursor( Cursors.ZOOM_OUT ) );
	//	else sc.setCursor( Cursor.getDefaultCursor() );
	}
	public void mouseClicked( MouseEvent e ) {
		if( zoomIn.isSelected() ) sc.doZoom( e.getPoint(), 2. );
		else if( zoomOut.isSelected() ) sc.doZoom( e.getPoint(), .5 );
	}
	public void mousePressed(MouseEvent e) {
		if( !zoomIn.isSelected() && !zoomOut.isSelected() )return;
		start = e.getPoint();
	}
	public void mouseDragged(MouseEvent e) {
		if( !zoomIn.isSelected() && !zoomOut.isSelected() )return;
		Rectangle shape = new Rectangle( start.x, start.y, 0, 0);
		shape.add(e.getPoint());
	//	if( shape.width<=10 || shape.height<=10 )return;
//System.out.println( shape.width +"\t"+ shape.height );
		sc.setShape( shape);
	}
	public void mouseReleased(MouseEvent e) {
		if( !zoomIn.isSelected() && !zoomOut.isSelected() )return;
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
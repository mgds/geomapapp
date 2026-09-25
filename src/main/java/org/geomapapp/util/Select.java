package org.geomapapp.util;

import java.awt.event.*;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Cursor;
import java.awt.geom.GeneralPath;
import javax.swing.JToggleButton;

public class Select extends javax.swing.event.MouseInputAdapter {
	Point lastP;
	ScalableComp sc;
	JToggleButton toggle;
	transient GeneralPath shape;
	public Select(ScalableComp sc) {
		this.sc = sc;
	}
	public JToggleButton getToggle() {
		if( toggle==null ) {
			toggle = new JToggleButton( Icons.getIcon(Icons.SELECT, false) );
			toggle.setSelectedIcon( Icons.getIcon(Icons.SELECT, true) );
			toggle.addChangeListener( new javax.swing.event.ChangeListener() {
				public void stateChanged( javax.swing.event.ChangeEvent e) {
					setCursor();
				}
			});
		}
		return toggle;
	}
	public void setCursor() {
		if( toggle.isSelected() ) sc.setCursor( Cursor.getDefaultCursor() );
	}
	public void mouseClicked( MouseEvent e ) {
		if( !toggle.isSelected() ) return;
	}
	public void mousePressed(MouseEvent e) {
		if( !toggle.isSelected() ) return;
		shape = new GeneralPath();
		shape.moveTo( (float)e.getX(), (float)e.getY() );
	}
	public void mouseDragged(MouseEvent e) {
		if( !toggle.isSelected() ) return;
		shape.lineTo( (float)e.getX(), (float)e.getY() );
		sc.setShape( shape);
	}
	public void mouseReleased(MouseEvent e) {
		if( !toggle.isSelected() ) return;
		shape = null;
		sc.setShape( null );
	}
}
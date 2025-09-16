package org.geomapapp.util;

import java.awt.event.*;
import java.awt.Point;
import java.awt.Cursor;
import javax.swing.JToggleButton;

public class Pan extends javax.swing.event.MouseInputAdapter {
	Point lastP;
	ScalableComp sc;
	JToggleButton toggle;
	Cursor cursor;
	public Pan(ScalableComp sc) {
		this.sc = sc;
		cursor = Cursors.getCursor( Cursors.HAND );
	}
	public JToggleButton getToggle() {
		if( toggle==null) {
			toggle = new JToggleButton(Icons.getIcon(Icons.HAND, false));
			toggle.setSelectedIcon( Icons.getIcon(Icons.HAND, true));
			toggle.addChangeListener( new javax.swing.event.ChangeListener() {
				public void stateChanged( javax.swing.event.ChangeEvent e) {
					setCursor();
				}
			});
		}
		return toggle;
	}
	public Cursor getCursor() {
		return cursor;
	}
	void setCursor() {
	//	if( toggle.isSelected() ) sc.setCursor( Cursor.getPredefinedCursor( Cursor.HAND_CURSOR ) );
		if( toggle.isSelected() ) sc.setCursor( Cursors.getCursor( Cursors.HAND ) );
	//	else sc.setCursor( Cursor.getDefaultCursor() );
	}
	public void mouseDragged(MouseEvent e) {
		if( !toggle.isSelected() )return;
		if( lastP==null ) {
			lastP = e.getPoint();
			return;
		}
		sc = (ScalableComp)e.getSource();
		Point p = e.getPoint();
		sc.pan( p.x-lastP.x, p.y-lastP.y );
		lastP = p;
	}
	public void mousePressed( MouseEvent e ) {
		if( !toggle.isSelected() )return;
		lastP = e.getPoint();
		sc.initPan();
	}
	public void mouseReleased( MouseEvent e ) {
		if( !toggle.isSelected() )return;
		lastP = null;
		sc.disposePan();
	}
	public void mouseClicked( MouseEvent e ) {
		if( !toggle.isSelected() )return;
		sc.doZoom( e.getPoint(), 1. );
	}
}
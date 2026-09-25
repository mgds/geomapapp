package org.geomapapp.util;

import java.awt.event.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JLabel;

public class Info extends javax.swing.event.MouseInputAdapter {
	ScalableComp sc;
	JLabel info;
	java.text.NumberFormat fmt;
	public Info(ScalableComp sc) {
		fmt = java.text.NumberFormat.getInstance();
		fmt.setMaximumFractionDigits(1);
		fmt.setGroupingUsed(false);
		this.sc = sc;
	}
	public JLabel getLabel() {
		if( info==null) {
			info = new JLabel("Location");
		}
		return info;
	}
	public void mouseMoved(MouseEvent e) {
		Point2D p = sc.inverse(e.getPoint());
		Rectangle2D r = sc.visibleRect();
		info.setText( fmt.format(r.getX()) +", "+ 
				fmt.format(r.getY()) +", "+ 
				fmt.format(r.getWidth()) +", "+ 
				fmt.format(r.getHeight()) +", "+ 
				fmt.format(p.getX()) +", "+ 
				fmt.format(p.getY()) );
	}
}

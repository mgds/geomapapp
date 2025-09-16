package org.geomapapp.db.dsdp;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.geom.*;

public class CoreDisplay extends JComponent {
	DSDPHole hole;
	double zScale;
	MouseAdapter mouse;
	JTextField text;
	JLabel label;
	int prevAge = -1;
	
	public CoreDisplay(DSDPHole hole, JTextField text) {
		this.text = text;
		this.hole = hole;
		zScale = 2.;
		mouse = new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				e.getComponent().requestFocus();
			}
		};
	}
	
	public CoreDisplay(DSDPHole hole, JLabel label) {
		this.label = label;
		this.hole = hole;
		zScale = 2.;
		mouse = new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				e.getComponent().requestFocus();
			}
		};
	}
	
	public void setHole(DSDPHole hole) {
		this.hole = hole;
	}
	public void setZScale( double zScale ) {
//System.out.println( zScale +"\t"+ this.zScale );
		this.zScale = zScale;
	}
	public double getZScale() {
		return zScale;
	}
	public Dimension getPreferredSize() {
		int h = (int)Math.ceil(hole.totalPen*zScale);
		return new Dimension(44, h);
	}
	public void addNotify() {
		removeMouseListener( mouse );
		addMouseListener( mouse );
		super.addNotify();
	}
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D)graphics;
		DSDPCore[] cores = hole.cores;
		if(cores==null)return;
		Dimension d = getPreferredSize();
		g.setColor(Color.white);
		g.fillRect( 0, 0, d.width, d.height);
		g.translate(20,0);
		d.width -= 20;
		g.setColor( Color.black );
		g.drawRect( 1, 1, d.width-2, d.height-2);
		g.setFont(new Font("SansSerif", Font.PLAIN, 9));
		for( int i=0 ; i<cores.length ; i++) {
			if( cores[i]==null )continue;
			Rectangle2D.Double r = new Rectangle2D.Double(
				2., cores[i].top*zScale, 
				d.width-3., 
				(cores[i].bottom-cores[i].top)*zScale);
			g.setColor( Color.lightGray );
			g.fill(r);
			r.height = (cores[i].recovered)*zScale;
			g.setColor( Color.gray );
			g.fill(r);
			g.setColor( Color.white );
			String s = ""+(i+1);
			int y = (int)(cores[i].bottom*zScale);
			g.drawString(s, 2, y-3 );
			y = (int)(cores[i].top*zScale);
			g.drawLine( 0, y, 24, y);
		}
		g.rotate(Math.PI/2.);
		org.geomapapp.util.Axes.drawAxis( g, false, null, 0., hole.totalPen, d.height, 4);
		prevAge = -1;
	}
	public void drawLineAtAge( int currentAge )	{
		synchronized (getTreeLock()) {
			Graphics2D g = (Graphics2D)getGraphics();
			Rectangle r = getVisibleRect();
			int x1 = r.x;
			int x2 = r.x+r.width;
			g.setXORMode( Color.cyan );
			if ( prevAge != -1)	{
				g.drawLine(x1, prevAge, x2, prevAge);
			}
			g.drawLine(x1, currentAge, x2, currentAge);
			prevAge = currentAge;
		}
	}
}

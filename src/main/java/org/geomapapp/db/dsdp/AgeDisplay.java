package org.geomapapp.db.dsdp;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.geom.*;

public class AgeDisplay extends JComponent {
	DSDPHole hole;
	double zScale;
	MouseInputAdapter mouse;
	AgeInterval age;
	JTextField text;
	JLabel label;
	int prevAge = -1;
	
	public AgeDisplay(DSDPHole hole, JTextField text) {
		this.text = text;
		this.hole = hole;
		zScale = 2.;
		mouse = new MouseInputAdapter() {
			public void mouseEntered(MouseEvent e) {
				e.getComponent().requestFocus();
			}
			public void mouseClicked(MouseEvent e) {
				setAge( e.getY() );
			}
		};
	}
	
	public AgeDisplay(DSDPHole hole, JLabel label) {
		this.label = label;
		this.hole = hole;
		zScale = 2.;
		mouse = new MouseInputAdapter() {
			public void mouseEntered(MouseEvent e) {
				e.getComponent().requestFocus();
			}
			public void mouseClicked(MouseEvent e) {
				setAge( e.getY() );
			}
		};
	}
	
	public void setHole(DSDPHole hole) {
		this.hole = hole;
	}
	public Dimension getPreferredSize() {
		int h = (int)Math.ceil(hole.totalPen*zScale);
		return new Dimension(24, h);
	}
	public void addNotify() {
	//	removeMouseMotionListener( mouse );
	//	addMouseMotionListener( mouse );
		removeMouseListener( mouse );
		addMouseListener( mouse );
		super.addNotify();
	}
	public void setZScale( double zScale ) {
		this.zScale = zScale;
	}
	public double getZScale() {
		return zScale;
	}
	void setAge(int y) {
		float dep = (float)(y/zScale);
		AgeInterval age = hole.ageAtDepth(dep);
		if( age==this.age)return;
		this.age = age;
		if( age==null )return;
		float[] range = age.getAgeRange();
		if ( text != null ) {
			text.setText( age.getAgeName() +": "+ range[0] +"-"+ range[1] );
		}
		else if ( label != null ) {
			label.setText( age.getAgeName() +": "+ range[0] +"-"+ range[1] );
		}
	}
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D)graphics;
		AgeInterval[] ages = hole.ageIntervals;
		if( ages==null )return;
		Dimension d = getPreferredSize();
		g.setColor(Color.white);
		g.fillRect( 0, 0, d.width, d.height);
		g.setColor( Color.black );
		g.drawRect( 1, 1, d.width-2, d.height-2);
		g.setFont(new Font("SansSerif", Font.PLAIN, 12));
		Color[] color = new Color[] { Color.gray, Color.darkGray };
		int col = 0;
		AffineTransform at = g.getTransform();
		Shape clip = g.getClip();
		double top = 0.;
		for( int i=0 ; i<ages.length ; i++) {
			if( ages[i]==null )continue;
			Rectangle2D.Double r = new Rectangle2D.Double(
				2., ages[i].top*zScale, 
				d.width-3., 
				(ages[i].bottom-ages[i].top)*zScale);
			g.setColor( Color.lightGray );
			g.fill(r);
			g.setColor(Color.black);
			g.draw(r);
			r = new Rectangle2D.Double(
				2., top*zScale,
				d.width-3.,
				(ages[i].bottom-top)*zScale);
			r.x += 2;
			r.y += 4;
			r.width-=4;
			r.height-=6;
			g.clip(r);
			g.setColor( Color.white );
			String s = ages[i].getAgeName();
			g.translate( 20., ages[i].bottom*zScale-4.);
			g.rotate( -Math.PI/2.);
			g.drawString(s, 0, 0 );
			g.setColor( Color.blue.darker() );
			g.drawString(s, -1, -1 );
			g.setTransform(at);
			g.setClip(clip);
			top = ages[i].bottom;
		}
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

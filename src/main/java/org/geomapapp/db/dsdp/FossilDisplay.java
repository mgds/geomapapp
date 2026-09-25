package org.geomapapp.db.dsdp;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.geom.*;

public class FossilDisplay extends JComponent {
	DSDPHole hole;
	CoreDisplay coreDisp;
	FossilGroup group;
	FossilAssembly fossils;
	double zScale;
	JTextField text, altText;
	JLabel label;
	MouseInputAdapter mouse;
	int lastFossil = -1;
	int lastY = -1;
	int prevAge = -1;
	
	public FossilDisplay(DSDPHole hole,
			JTextField text,
			CoreDisplay coreDisp,
			FossilGroup group,
			JTextField altText ) {
		this.text = text;
		this.altText = altText;
		this.coreDisp = coreDisp;
		this.hole = hole;
		this.group = group;
		fossils = hole.getFossilAssembly( group.getGroupName() );
		zScale = 2.;
	}
	
	public FossilDisplay(DSDPHole hole,
			JLabel label,
			CoreDisplay coreDisp,
			FossilGroup group,
			JTextField altText ) {
		this.label = label;
		this.altText = altText;
		this.coreDisp = coreDisp;
		this.hole = hole;
		this.group = group;
		fossils = hole.getFossilAssembly( group.getGroupName() );
		zScale = 2.;
	}
	
	public String getGroupName() {
		return group.getGroupName();
	}
	public void setHole( DSDPHole hole ) {
		this.hole = hole;
		fossils = hole.getFossilAssembly( group.getGroupName() );
	}
	public void setGroup(FossilGroup group) {
		this.group = group;
		fossils = hole.getFossilAssembly( group.getGroupName() );
	}
	public Dimension getPreferredSize() {
		int h = (int)Math.ceil(hole.totalPen*zScale);
		if( fossils==null ) return new Dimension( 250, h);
		int w = fossils.getAllCodes().length * 4;
		return new Dimension(w, h);
	}
	public void setZScale( double zScale ) {
		this.zScale = zScale;
	}
	public double getZScale() {
		return zScale;
	}
	public void addNotify() {
		super.addNotify();
		if( mouse==null ) {
			initMouse();
		} else {
			removeMouseListener(mouse);
			removeMouseMotionListener(mouse);
		}
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
	}
	void initMouse() {
		mouse = new MouseInputAdapter() {
			public void mouseExited(MouseEvent e) {
			//	setLoc(-1, -1);
			}
			public void mousePressed(MouseEvent e) {
				mouseDragged(e);
			}
			public void mouseDragged(MouseEvent e) {
				int k = e.getX()/4;
				setLoc(k, e.getY() );
			}
		};
	}
	void setLoc(int k, int y ) {
		requestFocus();
		if( fossils==null )return;
		short[] codes = fossils.getAllCodes();
		if( k>=codes.length )k=-1;
		if( k>=0 ) {
			int code = codes[k];
			String name = group.getFossilName(code);
		//	text.setText("("+code+") "+name);
//			text.setText(name);

			if ( text != null ) {
				text.setText(name);
			}
			else if ( label != null ) {
				label.setText(name);
				label.firePropertyChange("text", 0, 1);
			}
			
			FossilEntry[] fe = fossils.entries; 
			float test = (float)y;
			float z0 = (float)zScale * fe[0].depth;
			int i;
			for( i=0 ; i<fe.length-1 ; i++) {
				float z = (float)zScale * fe[i+1].depth;
				float zTest = (z+z0)*.5f;
				if( zTest > test)break;
				z0 = z;
			}
			y = (int)(zScale * fe[i].depth);
			int j = hole.coreNumberAtDepth(fe[i].depth);
			if( j>=0 ) {
				DSDPCore c = hole.getCores()[j];
				int section = 1+(int)Math.floor( (fe[i].depth-c.top)/1.5);
				StringBuffer sb = new StringBuffer((j+1)+"-");
				if( section>c.nSection) sb.append("CC, ");
				else {
					int iz = (int)Math.rint((fe[i].depth-(c.top+1.5*(section-1)))*100.);
					sb.append( section+"-"+iz+" cm,  ");
				}
				sb.append( fossils.getReference( (int)fe[i].ref ));
				altText.setText(sb.toString());
			} else {
				altText.setText(fossils.getReference( (int)fe[i].ref ));
			}
		} else {
			if ( text != null ) {
				text.setText("");
			}
			else if ( label != null ) {
				label.setText("");
			}
			altText.setText("");
		}
		Rectangle r = getVisibleRect();
		int x1 = r.x;
		int x2 = r.x+r.width;
		r.width = 4;
		synchronized(getTreeLock()) {
			Graphics2D g = (Graphics2D)getGraphics();
			g.setXORMode(Color.red);
			if( lastFossil != -1) {
				r.x = lastFossil*4;
				g.fill(r);
				g.drawLine( x1, lastY, x2, lastY);
			}
			lastFossil = k;
			lastY = y;
			if( lastFossil != -1) {
				r.x = lastFossil*4;
				g.fill(r);
				g.drawLine( x1, lastY, x2, lastY);
			}
		}
	}
	public void paint(Graphics graphics) {
		lastFossil = -1;
		Graphics2D g = (Graphics2D)graphics;
		prevAge = -1;
		if( fossils==null ) return;
		short[] codes = fossils.getAllCodes();
		FossilEntry[] entries = fossils.entries;
		g.setStroke( new BasicStroke(2f));
		g.setColor(Color.white);
		g.fill( getVisibleRect());
		g.setColor( Color.black );
		float[] pt;
		for( int i=0 ; i<codes.length ; i++) {
			Vector az = new Vector();
			for( int k=0 ; k<entries.length ; k++) {
				float a = (float)entries[k].abundanceForCode((int)codes[i]);
				if( a==-2.f )continue;
				a = (a+1.f)/4.f;
				float z = (float)zScale * entries[k].depth;
				az.add( new float[] {a, z, (float)k});
			}
			if( az.size()!=0 ) {
				pt = (float[])az.get(0);
				if( az.size()==1 ) {
					Line2D.Float line = new Line2D.Float(1.5f-pt[0], pt[1], 2.f+pt[0], pt[1]);
					g.draw(line);
				} else {
					GeneralPath p = new GeneralPath();
					p.moveTo( 1.5f-pt[0], pt[1]);
					for( int j=1 ; j<az.size() ; j++) {
						pt = (float[])az.get(j);
						p.lineTo( 1.5f-pt[0], pt[1]);
					}
					for( int j=az.size()-1 ; j>=0 ; j--) {
						pt = (float[])az.get(j);
						p.lineTo( 2.f+pt[0], pt[1]);
					}
					g.setColor(Color.gray);
					p.closePath();
					g.fill(p);
					g.setColor( Color.black );
					for( int j=0 ; j<az.size() ; j++) {
						pt = (float[])az.get(j);
						Line2D.Float line = new Line2D.Float(1.5f-pt[0], pt[1], 
							2.f+pt[0], pt[1]);
						g.draw(line);
					}
				}
			}
			g.translate( 4., 0.);
		}
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

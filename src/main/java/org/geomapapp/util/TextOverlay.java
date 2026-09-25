package org.geomapapp.util;

import java.awt.*;
import java.util.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.beans.*;

public class TextOverlay extends JComponent {
	TextPanel panel;
	JDialog dialog;
	Point2D location;
	Point2D point;
	Container parent;
	MouseInputAdapter mouse;
	Rectangle2D.Double bounds=null;
	public TextOverlay(Font font) {
		setSize(1024*1024, 1024*1024);
		setLocation(0,0);
		location = new Point(0,0);
		init(font);
	}
	public TextOverlay() {
		setSize(1024*1024, 1024*1024);
		setLocation(0,0);
		location = new Point(0,0);
		init(null);
	}
	boolean removing=false;
	public void addNotify() {
		super.addNotify();
		parent = getParent();
		if( mouse==null ) initMouse();
		parent.addMouseListener(mouse);
		parent.addMouseMotionListener(mouse);
		if( !removing && parent instanceof ScalableComponent) {
			Rectangle r = ((ScalableComponent)parent).getVisibleRect();
			Point2D p = new Point2D.Double(r.x+r.width*.4,
							r.y+r.height*.5);
			p = ((ScalableComponent)parent).inverseTransform(p);
			location = p;
		} 
		showDialog();
		panel.focusText();
		panel.selectText();
		removing=false;
	}
	public void removeNotify() {
		removing = true;
		super.removeNotify();
		parent.removeMouseListener(mouse);
		parent.removeMouseMotionListener(mouse);
	}
	void initMouse() {
		mouse = new MouseInputAdapter() {
			public void mouseDragged(MouseEvent evt) {
				movePoint(evt);
			}
			public void mousePressed(MouseEvent evt) {
				moveInit(evt.getPoint());
			}
			public void mouseReleased(MouseEvent evt) {
				resetPoint();
			}
			public void mouseClicked(MouseEvent evt) {
				if( evt.isControlDown() )return;
				select(evt.getPoint());
			}
		};
	}
	void select( Point2D p ) {
		point = null;
		if( bounds==null )return;
		if( parent instanceof ScalableComponent) {
			p = ((ScalableComponent)parent).inverseTransform(p);
		}
		if( panel.getRotation() != 0.) {
			AffineTransform at = new AffineTransform();
			at.rotate(-panel.getRotation(), bounds.getX(), bounds.getY() );
			p = at.transform(p, null);
		}
		if( bounds.contains(p) ) {
			dialog.show();
			panel.focusText();
		}
		else dialog.hide();
	}
	void resetPoint() {
		if(point==null)return;
		point = null;
	}
	void moveInit(Point2D p) {
		if( !dialog.isVisible() )return;
		point = null;
		if( bounds==null )return;
		if( parent instanceof ScalableComponent) {
			p = ((ScalableComponent)parent).inverseTransform(p);
		}
		point = p;
		if( panel.getRotation() != 0.) {
			AffineTransform at = new AffineTransform();
			at.rotate(-panel.getRotation(), bounds.getX(), bounds.getY() );
//	System.out.println( p.getX() +"\t"+ p.getY() );
			p = at.transform(p, null);
//	System.out.println( p.getX() +"\t"+ p.getY()+"\t"+ bounds.contains(p)+"\n" );
		}
		if( !bounds.contains(p) ) point=null;
	}
	void movePoint(MouseEvent evt) {
		if( !dialog.isVisible() )return;
		Point2D p = evt.getPoint();
		if( point==null) return;
		if( parent instanceof ScalableComponent) {
			p = ((ScalableComponent)parent).inverseTransform(p);
		} 
		double factor = evt.isShiftDown() ? .2 : 1.;
		location = new Point2D.Double(
			location.getX()+factor*(p.getX()-point.getX()),
			location.getY()+factor*(p.getY()-point.getY()));
		point = p;
		repaint();
	}
	public void delete() {
		parent.remove(this);
		dialog.dispose();
		parent.repaint();
	}
	void init(Font font) {
		panel = new TextPanel(font, this);
		initDialog();
		panel.addPropertyChangeListener( 
			new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					repaint();
				}
			});
	}
	public Font hideDialog() {
		if(dialog==null) return null;
		dialog.hide();
		return panel.resolveFont();
	}
	public void showDialog() {
		if(dialog==null) {
			if( parent==null ) return;
			initDialog();
		}
		dialog.show();
	}
	void initDialog() {
		if( parent==null ) return;
		dialog = new JDialog((JFrame)getTopLevelAncestor(),
				"Text Dialog",
				false);
		dialog.getContentPane().add(panel);
		dialog.pack();
	//	dialog.setDefaultCloseOperation(dialog.HIDE_ON_CLOSE);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				close();
			}
		});
	}
	void close() {
	}
	public void paint(Graphics g) {
		if( !isVisible() )return;
		Graphics2D g2 = (Graphics2D)g;
		AffineTransform at0 = g2.getTransform();
	//	double dx = 3.;
		double dx = 1.;
		if(parent instanceof ScalableComponent) {
			AffineTransform xf = ((ScalableComponent)parent).getTransform();
			g2.transform(xf);
			dx /= xf.getScaleX();
		}
		Font font = panel.resolveFont();
		int size = font.getSize();
		g2.setFont( font);
		String text = panel.getText();
		FontRenderContext ctxt = g2.getFontRenderContext();
		double ht = (double)font.getLineMetrics("A",ctxt).getHeight();
		double space = font.getStringBounds(" ",ctxt).getWidth();
		GlyphVector glyph;
		Rectangle2D r;
		g2.translate(location.getX(), location.getY());
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setStroke( new BasicStroke((float)(3.*dx),
				BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND) );
		StringTokenizer lines = new StringTokenizer(text,"\n", true);
		bounds = new Rectangle2D.Double(
					location.getX(), 
					location.getY(),
					0., 0.);
		if( panel.getRotation()!=0.) g2.rotate(panel.getRotation());
		while( lines.hasMoreTokens() ) {
			g2.translate(0., ht);
			String line = lines.nextToken();
			bounds.height += ht;
			if( line.equals("\n") )continue;
			if( line.length()==0 ) continue;
			glyph = font.createGlyphVector(ctxt, line.trim());
			StringTokenizer spaces = new StringTokenizer(line," ", true);
			double x0 = 0f;
			while( spaces.hasMoreTokens() ) {
				if( spaces.nextToken().equals(" ") ) x0+=space;
				else break;
			}
			r = glyph.getVisualBounds();
			AffineTransform at = g2.getTransform();
			g2.translate( x0-r.getX(), -(r.getY()+r.getHeight()));
			bounds.width = Math.max(bounds.width, 
					x0-r.getX()+r.getWidth());
			Shape shape = glyph.getOutline();
// /*	shadow
			int shadow = panel.getShadow();
			if( shadow>0 ) {
				g2.setColor( new Color(0,0,0,shadow));
				for( int x=-5 ; x<6 ; x++) {
					for( int y=-5 ; y<6 ; y++) {
						if(x*x+y*y > 30)continue;
						g2.translate((x+5)*dx,(y+5)*dx);
						g2.fill(shape);
						g2.translate(-(x+5)*dx,-(y+5)*dx);
					}
				}
				g2.translate(3*dx,3*dx);
			}
// */
			Color color = panel.getFillColor();
			if( color!=null ) {
				g2.setColor(color);
				g2.fill(shape);
			}
			color = panel.getOutlineColor();
			if( color!=null ) {
				float lw = panel.getLineWidth();
				BasicStroke stroke = new BasicStroke( lw,
					BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_ROUND);

				g2.setStroke( stroke );
				g2.setColor(color);
				g2.draw(shape);
			}
			if( lines.hasMoreTokens() ) lines.nextToken();
			g2.setTransform( at );
		}
		g2.setTransform( at0 );
	}
}
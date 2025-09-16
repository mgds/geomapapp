package org.geomapapp.util;

import org.geomapapp.image.*;

import java.awt.*;
import java.util.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.beans.*;

public class RectangleOverlay extends JComponent {
	Point2D location;
	Point2D point;
	Container parent;
	MouseInputAdapter mouse;
	Rectangle2D.Double shape;
	JDialog dialog;
	boolean editing;
	boolean creating;
	boolean select;
	static int[] cursors;
	public RectangleOverlay() {
		if(cursors==null)initCursors();
		setSize(1024*1024, 1024*1024);
		setLocation(0,0);
		location = new Point(0,0);
		editing = select = creating = false;
	}
	static void initCursors() {
		cursors = new int[16];
		for( int k=0 ; k<16 ; k++) cursors[k] = Cursor.DEFAULT_CURSOR;
		cursors[1] = Cursor.W_RESIZE_CURSOR;
		cursors[2] = Cursor.E_RESIZE_CURSOR;
		cursors[4] = Cursor.N_RESIZE_CURSOR;
		cursors[8] = Cursor.S_RESIZE_CURSOR;
		cursors[5] = Cursor.NW_RESIZE_CURSOR;
		cursors[6] = Cursor.NE_RESIZE_CURSOR;
		cursors[9] = Cursor.SW_RESIZE_CURSOR;
		cursors[10] = Cursor.SE_RESIZE_CURSOR;
	}
	public void addNotify() {
		super.addNotify();
		parent = getParent();
		if( mouse==null ) initMouse();
		parent.addMouseListener(mouse);
		parent.addMouseMotionListener(mouse);
		if( !removing ) {
			showDialog();
			creating = true;
		}
		repaint();
		removing = false;
	}
	boolean removing=false;
	public void removeNotify() {
		removing = true;
		try {
			parent.removeMouseListener(mouse);
			parent.removeMouseListener(mouse);
		} catch(Exception ex) {
		}
		super.removeNotify();
	}
	JTextField scaleF;
	JCheckBox antiAlias;
	void showDialog() {
		if(dialog==null) initDialog();
		dialog.show();
	}
	ColorComponent fill, outline;
	JCheckBox fillCB, outlineCB;
	void initDialog() {
		dialog = new JDialog((JFrame)getTopLevelAncestor(),
				"Shape Dialog",
				false);
		JLabel label = new JLabel("line width");
		scaleF = new JTextField("1");
		ActionListener redraw = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				repaint();
			}
		};
		antiAlias = new JCheckBox("anti-alias");
		antiAlias.addActionListener( redraw );
		scaleF.addActionListener( redraw );
		antiAlias.setSelected(false);
		JPanel panel = new JPanel(new GridLayout(0,2));
		panel.add(label);
		panel.add(scaleF);
		outline = new ColorComponent(Color.black);
		outline.addMouseListener( new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				modColor();
			}
		});
		outlineCB = new JCheckBox("outline", true);
		outlineCB.addActionListener(redraw);
		panel.add(outlineCB);
		panel.add(outline);

		fill = new ColorComponent(Color.white);
		fill.addMouseListener( new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				modFill();
			}
		});
		fillCB = new JCheckBox("fill", false);
		fillCB.addActionListener(redraw);
		panel.add(fillCB);
		panel.add(fill);

	//	panel.add(antiAlias);
		JButton b = new JButton("delete");
		panel.add(b);
		b.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				delete();
			}
		});
		b = new JButton("to front");
		b.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				toFront();
			}
		});
		panel.add(b);
		dialog.getContentPane().add(panel);
		dialog.pack();
	}
	void modColor() {
		if( !outlineCB.isSelected() ) return;
		ColorModPanel p = new ColorModPanel(outline.getColor().getRGB(), true);
		int ok = JOptionPane.showConfirmDialog( dialog, p, 
				"foreground color", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if( ok==JOptionPane.CANCEL_OPTION) return;
		int rgb = p.getRGB();
		outline.setColor( new Color( p.getRGB(), true) );
		repaint();
	}
	void modFill() {
		if( !fillCB.isSelected() ) return;
		ColorModPanel p = new ColorModPanel(fill.getColor().getRGB(), true);
		int ok = JOptionPane.showConfirmDialog( dialog, p, 
				"foreground color", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if( ok==JOptionPane.CANCEL_OPTION) return;
		int rgb = p.getRGB();
		fill.setColor( new Color( p.getRGB(), true) );
		repaint();
	}
	void toFront() {
		try {
			parent.add(this,0);
		}catch(Exception ex) {
		}
	}
	void delete() {
		parent.remove(this);
		parent.removeMouseListener(mouse);
		parent.removeMouseMotionListener(mouse);
		dialog.dispose();
		parent.repaint();
	}
	void initMouse() {
		mouse = new MouseInputAdapter() {
			public void mouseDragged(MouseEvent evt) {
				if( evt.isControlDown() )return;
				movePoint(evt);
			}
			public void mousePressed(MouseEvent evt) {
				if( evt.isControlDown() )return;
				moveInit(evt.getPoint());
			}
			public void mouseReleased(MouseEvent evt) {
				if( evt.isControlDown() )return;
				resetPoint();
			}
			public void mouseMoved(MouseEvent evt) {
				if( evt.isControlDown() )return;
				move(evt.getPoint());
			}
			public void mouseClicked(MouseEvent evt) {
				if( evt.isControlDown() )return;
				select(evt.getPoint());
			}
		};
	}
	int side(Point2D p) {
		p = ((ScalableComponent)parent).inverseTransform(p);
		double zoom = ((ScalableComponent)parent).getTransform().getScaleX();
		int side = 0;
		if( (p.getY()-shape.getY())*zoom>-3. && 
				(p.getY()-shape.getY()-shape.getHeight())*zoom<3. ) {
			if( Math.abs((p.getX()-shape.getX())*zoom)<=3. )side += 1;
			else if( Math.abs((p.getX()-shape.getWidth()-shape.getX())*zoom)<=3. )side += 2;
		}
		if( (p.getX()-shape.getX())*zoom>-3. && 
				(p.getX()-shape.getX()-shape.getWidth())*zoom<3. ) {
			if( Math.abs((p.getY()-shape.getY())*zoom)<=3. )side += 4;
			else if( Math.abs((p.getY()-shape.getY()-shape.getHeight())*zoom)<=3. )side += 8;
		}
		return side;
	}
	void move(Point2D p) {
		if( creating ) {
			if( point==null ) return;
			p = ((ScalableComponent)parent).inverseTransform(p);
			shape = new Rectangle.Double(
				Math.min( p.getX(), point.getX()),
				Math.min( p.getY(), point.getY()),
				Math.abs( p.getX()-point.getX()),
				Math.abs( p.getY()-point.getY()) );
			repaint();
		} else if( select) {
			parent.setCursor( Cursor.getPredefinedCursor(cursors[side(p)]));
		}
	}
	void create(Point2D p) {
		if( !creating ) return;
		p = ((ScalableComponent)parent).inverseTransform(p);
		if( point==null ) {
			point = p;
		} else {
			shape = new Rectangle.Double(
				Math.min( p.getX(), point.getX()),
				Math.min( p.getY(), point.getY()),
				Math.abs( p.getX()-point.getX()),
				Math.abs( p.getY()-point.getY()) );
			location = new Point2D.Double( shape.getX(), shape.getY());
			repaint();
			creating = false;
			select = true;
			point = null;
		}
	}
	void select( Point2D p ) {
		if( creating ) {
			create(p);
			return;
		}
		boolean redraw = select;
		select = false;
		if( shape==null )return;
		if( parent instanceof ScalableComponent) {
			p = ((ScalableComponent)parent).inverseTransform(p);
		}
		if( shape.contains(p) ) select = true;
		if( redraw|| select )repaint();
		if( !select ) dialog.hide();
		else showDialog();
	}
	void resetPoint() {
		if(creating)return;
		point = null;
	}
	int side;
	void moveInit(Point2D p) {
		if(creating)return;
		if( !dialog.isVisible() )select = false;
		point = null;
		if( !select )return;
		if( shape==null )return;
		side = side(p);
		if( parent instanceof ScalableComponent) {
			point = ((ScalableComponent)parent).inverseTransform(p);
		} else {
			point = p;
		}
		if( side!=0 )return;
		if( !shape.contains(point) ) point=null;
	}
	void movePoint(MouseEvent evt) {
		if(creating)return;
		Point2D p = evt.getPoint();
		if( point==null) return;
		if( parent instanceof ScalableComponent) {
			p = ((ScalableComponent)parent).inverseTransform(p);
		} 
		double dx = p.getX()-point.getX();
		double dy = p.getY()-point.getY();
		if( side==0 ) {
			location = new Point2D.Double(
				location.getX()+dx,
				location.getY()+dy);
			shape.x = location.getX();
			shape.y = location.getY();
		}
		if( (side&1)==1 ) {
			location = new Point2D.Double(
				location.getX()+dx,
				location.getY());
			shape.width -= dx;
			shape.x = location.getX();
		} else if((side&2)==2) {
			shape.width += dx;
		}
		if( (side&4)==4) {
			location = new Point2D.Double(
				location.getX(),
				location.getY()+dy);
			shape.height -= dy;
			shape.y +=dy;
		} else if((side&8)==8) {
			shape.height += dy;
		}
			
		point = p;
		repaint();
	}
	public void paint(Graphics g) {
		if( !isVisible() )return;
		if( dialog==null )return;
		if( !dialog.isVisible() )select = false;
		if( shape==null ) return;
		if(dialog==null)return;
		Graphics2D g2 = (Graphics2D)g;
		AffineTransform at0 = g2.getTransform();
		float zoom = 1f;
		if(parent instanceof ScalableComponent) {
			AffineTransform xf = ((ScalableComponent)parent).getTransform();
			g2.transform(xf);
			zoom = (float) xf.getScaleX();
		}
		if( fillCB.isSelected()) {
			g2.setColor( fill.getColor() );
			g2.fill(shape);
		}
		if( outlineCB.isSelected() ) {
			float lw = 1f;
			try {
				lw = Float.parseFloat(scaleF.getText());
			} catch(Exception ex) {
			}
			g2.setStroke(new BasicStroke(lw));
			g2.setColor( outline.getColor() );
			g2.draw(shape);
		}
		if( select ) {
			g2.setStroke( new BasicStroke(1f/zoom));
			Rectangle2D.Float r = new Rectangle2D.Float(
				-3f/zoom, -3f/zoom, 6f/zoom, 6f/zoom);
			AffineTransform xf = g2.getTransform();
			g2.translate( shape.getX(), shape.getY() );
			g2.setColor( Color.white );
			g2.fill( r );
			g2.setColor( Color.black );
			g2.draw( r );
			g2.setTransform( xf );
			g2.translate( shape.getX()+shape.getWidth(), shape.getY() );
			g2.setColor( Color.white );
			g2.fill( r );
			g2.setColor( Color.black );
			g2.draw( r );
			g2.setTransform( xf );
			g2.translate( shape.getX()+shape.getWidth(), shape.getY()+shape.getHeight() );
			g2.setColor( Color.white );
			g2.fill( r );
			g2.setColor( Color.black );
			g2.draw( r );
			g2.setTransform( xf );
			g2.translate( shape.getX(), shape.getY()+shape.getHeight() );
			g2.setColor( Color.white );
			g2.fill( r );
			g2.setColor( Color.black );
			g2.draw( r );
			g2.setTransform( xf );
			g2.setPaintMode();
		}
	//	AffineTransform at = new AffineTransform();
	//	at.translate(location.getX(), location.getY());
	//	g2.setTransform( at );
	//	g2.setStroke(new BasicStroke(1f));
	//	g2.setColor( Color.black);
	//	g2.draw(shape);
		g2.setTransform( at0 );
	}
}
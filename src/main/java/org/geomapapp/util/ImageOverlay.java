package org.geomapapp.util;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;

public class ImageOverlay extends JComponent {
	Point2D location;
	Point2D point;
	Container parent;
	MouseInputAdapter mouse;
	Rectangle2D.Double bounds=null;
	BufferedImage image;
	JDialog dialog;
	JTextField scaleF;
	JCheckBox antiAlias;
	String path;
	public ImageOverlay(BufferedImage image, String path) {
		this(image);
		this.path = path;
	}
	public ImageOverlay(BufferedImage image) {
		this.image = image;
		setSize(1024*1024, 1024*1024);
		setLocation(0,0);
		location = new Point(0,0);
	}
	public String toString() {
		StringBuffer sb = new StringBuffer(path);
		sb.append( "\n"+ location.getX() +"\t"+ location.getY() +"\t"+ scaleF.getText() );
		sb.append( "\n"+antiAlias.isSelected() );
		return sb.toString();
	}
	public void setScale(double scale) {
		scaleF.setText( Double.toString(scale) );
	}
	public void addNotify() {
		super.addNotify();
		parent = getParent();
		if( mouse==null ) initMouse();
		parent.addMouseListener(mouse);
		parent.addMouseMotionListener(mouse);
		try {
			ScalableComponent comp = (ScalableComponent)parent;
			Rectangle r = comp.getVisibleRect();
			Point2D p = new Point2D.Double(r.getX(), r.getY());
			p = ((ScalableComponent)parent).inverseTransform(p);
			if( bounds==null ) {
				location = p;
				bounds = new Rectangle2D.Double(p.getX(),
							p.getY(),
							image.getWidth(),
							image.getHeight());
			}
		} catch(Exception ex) {
		}
		showDialog();
		repaint();
	}
	public void removeNotify() {
		try {
			parent.removeMouseListener(mouse);
			parent.removeMouseListener(mouse);
		} catch(Exception ex) {
		}
		super.removeNotify();
	}
	void showDialog() {
		if(dialog==null) initDialog();
		dialog.show();
	}
	void initDialog() {
		dialog = new JDialog((JFrame)getTopLevelAncestor(),
				"Image Dialog",
				false);
		JLabel label = new JLabel("scale factor");
		scaleF = new JTextField("1");
		antiAlias = new JCheckBox("anti-alias");
		ActionListener redraw = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				repaint();
			}
		};
		antiAlias.addActionListener( redraw );
		scaleF.addActionListener( redraw );
		antiAlias.setSelected(false);
		JPanel panel = new JPanel(new GridLayout(2,0));
		panel.add(label);
		panel.add(scaleF);
		panel.add(antiAlias);
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
	void toFront() {
		try {
			parent.add(this,0);
		}catch(Exception ex) {
		}
	}
	void delete() {
		parent.remove(this);
		image = null;
		parent.removeMouseListener(mouse);
		parent.removeMouseMotionListener(mouse);
		dialog.dispose();
		parent.repaint();
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
	boolean select = false;
	void select( Point2D p ) {
		point = null;
		boolean redraw = select;
		select = false;
		if( bounds==null )return;
		if( parent instanceof ScalableComponent) {
			p = ((ScalableComponent)parent).inverseTransform(p);
		}
		if( bounds.contains(p) ) select = true;
		if( redraw|| select )repaint();
		if( !select ) dialog.hide();
		else showDialog();
	}
	void resetPoint() {
		if(point==null)return;
		point = null;
	}
	void moveInit(Point2D p) {
		point = null;
		if( !select )return;
		if( bounds==null )return;
		if( parent instanceof ScalableComponent) {
			point = ((ScalableComponent)parent).inverseTransform(p);
		} else {
			point = p;
		}
		if( !bounds.contains(point) ) point=null;
	}
	void movePoint(MouseEvent evt) {
		Point2D p = evt.getPoint();
		if( point==null) return;
		if( parent instanceof ScalableComponent) {
			p = ((ScalableComponent)parent).inverseTransform(p);
		} 
		double factor = evt.isShiftDown() ? .2 : 1.;
		location = new Point2D.Double(
			location.getX()+factor*(p.getX()-point.getX()),
			location.getY()+factor*(p.getY()-point.getY()));
		bounds.x = location.getX();
		bounds.y = location.getY();
		point = p;
		repaint();
	}
	public void paint(Graphics g) {
		if( !isVisible() )return;
		if(dialog==null)return;
		Graphics2D g2 = (Graphics2D)g;
		AffineTransform at0 = g2.getTransform();
		float zoom = 1f;
		if(parent instanceof ScalableComponent) {
			AffineTransform xf = ((ScalableComponent)parent).getTransform();
			g2.transform(xf);
			zoom = (float) xf.getScaleX();
		}
		AffineTransform at = new AffineTransform();
		at.translate(location.getX(), location.getY());
		try {
			double scale = Double.parseDouble(scaleF.getText());
			at.scale( scale, scale);
			bounds.width = scale*image.getWidth();
			bounds.height = scale*image.getHeight();
			if( antiAlias.isSelected() ) {
				g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			}
		} catch(Exception ex) {
		}
		g2.drawRenderedImage(image,at);
		if( select ) {
			g2.setStroke(new BasicStroke(1f/zoom));
			g2.setColor( Color.black);
			g2.draw(bounds);
		}
		g2.setTransform( at0 );
	}
}

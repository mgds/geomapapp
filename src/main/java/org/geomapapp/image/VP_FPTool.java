package org.geomapapp.image;

import org.geomapapp.grid.*;
import org.geomapapp.util.*;
import org.geomapapp.geom.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;

public class VP_FPTool extends JComponent {
	ImageComponent imComp;
	Grid2DOverlay overlay;
	Arrow arrow;
	Point2D focus;
	Point2D viewPoint;
	public VP_FPTool(Grid2DOverlay overlay ) {
		this.overlay = overlay;
		init();
	}
	public void paint(Graphics g) {
		update();
		Dimension dim = getSize();
		Dimension size = imComp.getPreferredSize();
		imComp.setLocation( (dim.width-size.width)/2,
				(dim.height-size.height)/2);
		super.paint(g);
	}
	void init() {
		setLayout(null);
		imComp = new ImageComponent(overlay.getImage());
		Dimension size = imComp.getPreferredSize();
		double z = 200./size.width;
		if( size.height>size.width) z = 200./size.height;
		imComp.doZoom(new Point(), z);
		imComp.setBorder( BorderFactory.createLineBorder(Color.black));
		imComp.setLocation(150, 150);
		imComp.setSize( imComp.getPreferredSize());
		arrow = new Arrow(this);
		add(arrow);
		add( imComp );
		javax.swing.event.MouseInputAdapter mouse =
			new javax.swing.event.MouseInputAdapter() {
				public void mousePressed(MouseEvent e) {
					if( e.isControlDown() ) return;
					setFocus(e.getPoint());
				}
				public void mouseDragged(MouseEvent e) {
					if( e.isControlDown() ) return;
					setViewPoint(e.getPoint());
				}
			};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
	}
	Point2D getFocus() {
		Point2D p = imComp.transform(focus);
		Point loc = imComp.getLocation();
		p.setLocation( p.getX()-loc.x, p.getY()-loc.getY() );
		p = imComp.inverseTransform(p);
		Rectangle r = overlay.getGrid().getBounds();
		p.setLocation(  p.getX()+r.x, p.getY()+r.y);
		return overlay.getGrid().getProjection().getRefXY(p);
	}
	Point2D getViewPoint() {
		Point2D p = imComp.transform(viewPoint);
		Point loc = imComp.getLocation();
		p.setLocation( p.getX()-loc.x, p.getY()-loc.getY() );
		p = imComp.inverseTransform(p);
		Rectangle r = overlay.getGrid().getBounds();
		p.setLocation(  p.getX()+r.x, p.getY()+r.y);
		return overlay.getGrid().getProjection().getRefXY(p);
	}
	public void setFocus(Point2D p) {
		viewPoint = null;
		focus = imComp.inverseTransform(p);
		arrow.repaint();
/*
	Point2D p1 = new Point2D.Double(p.getX()-150, p.getY()-150);
	p1 = imComp.inverseTransform(p1);
	System.out.println( focus.getX() +"\t"+
				focus.getY()  +"\t"+
				p1.getX()  +"\t"+
				p1.getY() );
*/
	}
	public void setViewPoint( Point2D p) {
		viewPoint = imComp.inverseTransform(p);
		arrow.repaint();
	}
	public void update() {
		imComp.setImage( overlay.getImage());
	}
	class Arrow extends JComponent {
		VP_FPTool tool;
		public Arrow(VP_FPTool tool) {
			this.tool = tool;
			setLocation(0,0);
			setBounds(0, 0, 1024*1024, 1024*1024);
		}
		public void paintComponent(Graphics g) {
			Point2D focus = tool.focus;
			Point2D viewPoint = tool.viewPoint;
			if( focus==null || viewPoint==null )return;
			AffineTransform at = imComp.getTransform();
			Point2D.Double p1 = (Point2D.Double)at.transform(
					viewPoint, new Point2D.Double());
			Point2D.Double p2 = (Point2D.Double)at.transform(
					focus, new Point2D.Double());
			XYZ x1 = new XYZ(p1.x, p1.y, 0.);
			XYZ x2 = new XYZ(p2.x, p2.y, 0.);
			XYZ dx = x2.minus(x1).normalize();
			XYZ dy = dx.cross(new XYZ(0.,0.,1));
			GeneralPath path = new GeneralPath();
			path.moveTo((float)p2.x, (float)p2.y);
			double x = x2.x - dx.x*20. +dy.x*8.;
			double y = x2.y - dx.y*20. +dy.y*8.;
			path.lineTo( (float)x, (float)y);
			x = x2.x - dx.x*12. +dy.x*2.;
			y = x2.y - dx.y*12. +dy.y*2.;
			path.lineTo( (float)x, (float)y);
			x = x1.x + dy.x*2.;
			y = x1.y + dy.y*2.;
			path.lineTo( (float)x, (float)y);
			x = x1.x - dy.x*2.;
			y = x1.y - dy.y*2.;
			path.lineTo( (float)x, (float)y);
			x = x2.x - dx.x*12. - dy.x*2.;
			y = x2.y - dx.y*12. - dy.y*2.;
			path.lineTo( (float)x, (float)y);
			x = x2.x - dx.x*20. -dy.x*8.;
			y = x2.y - dx.y*20. -dy.y*8.;
			path.lineTo( (float)x, (float)y);
			g.setColor(Color.black);
			path.closePath();
			Graphics2D g2 = (Graphics2D)g;
			g2.fill(path);
			Point2D f = getFocus();
			Point2D vp = getViewPoint();
			java.text.NumberFormat fmt = java.text.NumberFormat.getInstance();
			fmt.setMaximumFractionDigits(3);
			g.drawString( fmt.format(f.getX()) 
					+", "+fmt.format(f.getY())
					+"  -   "+fmt.format(vp.getX())
					+", "+fmt.format(vp.getY()), 10, 20);
		}
	}
}

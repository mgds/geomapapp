package org.geomapapp.image;

import org.geomapapp.geom.*;
import javax.swing.*;
import java.awt.geom.*;
import java.awt.*;

public class Arrow extends JComponent
		implements haxby.map.Overlay {
	public Point p1, p2;
	public Arrow(Point p1, Point p2) {
		this.p1 = p1;
		this.p2 = p2;
	}
	public Dimension getPreferredSize() {
		return new Dimension(500, 400);
	}
	public void paintComponent(Graphics g) {
		draw( (Graphics2D)g );
	}
	public void draw(Graphics2D g) {
		XYZ p1 = new XYZ( this.p1.getX(), this.p1.getY(), 0.);
		XYZ p2 = new XYZ( this.p2.getX(), this.p2.getY(), 0.);
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
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor( Color.white );
		g2.draw(path);
		g2.setColor( Color.black );
		g2.fill(path);
	}
	public static void main(String[] args) {
		Arrow arrow = new Arrow(
			new Point(10,10),
			new Point(400,50));
		JFrame frame = new JFrame();
		frame.getContentPane().add(arrow);
		frame.pack();
		frame.show();
	}
}

package haxby.util;

import java.awt.Insets;
import java.awt.geom.Point2D;

public abstract class ScaledComponent extends javax.swing.JComponent {
	public abstract double[] getScales();
	public Point2D getScaledPoint( Point2D mousePoint ) {
		Insets ins = getInsets();
		Point2D.Double p = new Point2D.Double( mousePoint.getX(), mousePoint.getY() );
		p.x -= ins.left;
		p.y -= ins.top;
		double[] scales = getScales();
		p.x /= scales[0];
		p.y /= scales[1];
		return p;
	}
}

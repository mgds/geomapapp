package haxby.image;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;

public abstract interface ComposerElement {
	public abstract void draw( Graphics2D g );
	public abstract boolean select( Point2D p );
	public abstract void setSelected( boolean tf );
	public abstract boolean isSelected();
	public abstract void setVisible( boolean tf );
	public abstract boolean isVisible();
	public abstract void dragged( Point2D from, Point2D to );
	public abstract Shape getShape();
}

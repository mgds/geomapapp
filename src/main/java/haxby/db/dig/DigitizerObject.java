package haxby.db.dig;

import java.awt.*;
public abstract interface DigitizerObject {
	public abstract void start();
	public abstract boolean finish();
	public abstract void setName( String name );
	public abstract boolean select( double x, double y );
	public abstract void setSelected( boolean tf );
	public abstract boolean isSelected();
	public abstract boolean isActive();
	public abstract javax.swing.Icon getIcon();
	public abstract javax.swing.Icon getDisabledIcon();
	public abstract void setVisible(boolean tf);
	public abstract boolean isVisible();
	public abstract Color getColor();
	public abstract Color getFill();
	public abstract void setColor( Color c );
	public abstract void setFill( Color c );
	public abstract BasicStroke getStroke();
	public abstract void setStroke( BasicStroke s );
	public abstract void redraw();
	public abstract void draw( Graphics2D g );
}

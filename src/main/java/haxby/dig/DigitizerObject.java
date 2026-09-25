package haxby.dig;

import java.awt.*;
import javax.swing.ImageIcon;

public abstract interface DigitizerObject {
	public static ImageIcon ICON = null;
	public static ImageIcon SELECTED_ICON = null;
	public static ImageIcon DISABLED_ICON = null;
	public abstract ImageIcon getIcon();
	public abstract ImageIcon getDisabledIcon();
	public abstract void start();
	public abstract boolean finish();
	public abstract void setName( String name );
	public abstract boolean select( double x, double y, double[] scales );
	public abstract void setSelected( boolean tf );
	public abstract boolean isSelected();
	public abstract boolean isActive();
	public abstract void setVisible(boolean tf);
	public abstract boolean isVisible();
	public abstract Color getColor();
	public abstract Color getFill();
	public abstract void setColor( Color c );
	public abstract void setFill( Color c );
	public abstract BasicStroke getStroke();
	public abstract void setStroke( BasicStroke s );
	public abstract void redraw();
	public abstract void draw( Graphics2D g, double[] scales, Rectangle bounds, Insets ins );
}
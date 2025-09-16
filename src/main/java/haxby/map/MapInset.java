package haxby.map;

// import java.awt.event.*;

public abstract interface MapInset
		//	extends	MouseListener,
		//		MouseMotionListener 
					{
//	public abstract boolean contains( double x, double y );
	public abstract void draw( java.awt.Graphics2D g, int width, int height );
}

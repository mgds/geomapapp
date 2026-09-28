package haxby.map;

import java.awt.geom.Rectangle2D;

public interface FocusOverlay extends Overlay {
	public abstract void focus(Rectangle2D rect);

	public abstract Runnable createFocusTask(Rectangle2D rect); 
}

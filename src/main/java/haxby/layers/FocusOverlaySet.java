package haxby.layers;

import haxby.map.FocusOverlay;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

public class FocusOverlaySet implements FocusOverlay {
	
	protected List<FocusOverlay> overlays = new LinkedList<FocusOverlay>();
	
	public Runnable createFocusTask(final Rectangle2D rect) {
		return new Runnable() {
			public void run() {
				focus(rect);
			}
		};
	}
	
	public void focus(Rectangle2D rect) {
		for (FocusOverlay overlay : overlays)
			overlay.focus(rect);	
	}
	
	public void draw(Graphics2D g) {
		for (FocusOverlay overlay : overlays)
			overlay.draw(g);
	}
	
	public void addOverlay(FocusOverlay overlay){
		this.overlays.add(overlay);
	}
	
	public void removeOverlay(FocusOverlay overlay){
		this.overlays.remove(overlay);
	}
}

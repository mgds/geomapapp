package haxby.util;

import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

import org.geomapapp.grid.Grid2DOverlay;

public class ToggleableMouseInputAdapter extends MouseInputAdapter implements MouseInputListener {
	private boolean mouseListenerIsActive;
	private Grid2DOverlay grid;

	public ToggleableMouseInputAdapter(Grid2DOverlay grid) {
		super();
		this.grid = grid;
		mouseListenerIsActive = true;
	}

	public boolean isMouseListenerIsActive() {
		return mouseListenerIsActive;
	}

	public void setMouseListenerIsActive(boolean mouseListenerIsActive) {
		this.mouseListenerIsActive = mouseListenerIsActive;
	}
	
	
	public Grid2DOverlay getGrid() {
		return grid;
	}

	public void setGrid(Grid2DOverlay grid) {
		this.grid = grid;
	}

}

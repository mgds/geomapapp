package haxby.worldwind;

import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.layers.Layer;

import java.awt.geom.Rectangle2D;

public interface WWOverlay {
	public Layer getLayer();
	public void setArea(Rectangle2D bounds);
	public SelectListener getSelectListener();
}

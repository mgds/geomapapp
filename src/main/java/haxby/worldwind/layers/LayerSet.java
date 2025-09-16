package haxby.worldwind.layers;

import java.awt.Point;
import java.util.LinkedList;
import java.util.List;

import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.DrawContext;

public class LayerSet extends AbstractLayer {

	public List<Layer> layers = new LinkedList<Layer>();
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		
		for (Layer layer : layers)
			layer.setEnabled(enabled);
	}

	@Override
	public void setOpacity(double opacity) {
		super.setOpacity(opacity);
		for (Layer layer : layers)
			layer.setOpacity(opacity);
	}
	
	@Override
	public void setPickEnabled(boolean pickable) {
		super.setPickEnabled(pickable);
		for (Layer layer : layers)
			layer.setPickEnabled(pickable);
	}
	
	@Override
	protected void doRender(DrawContext dc) {
		for (Layer layer : layers)
			layer.render(dc);
	}

	@Override
	protected void doPick(DrawContext dc, Point point) {
		for (Layer layer : layers)
			layer.pick(dc, point);
	}
	
	public boolean add(Layer o) {
		return layers.add(o);
	}

	public boolean remove(Object o) {
		return layers.remove(o);
	}
}

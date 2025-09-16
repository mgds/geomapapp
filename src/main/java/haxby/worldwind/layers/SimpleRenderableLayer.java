package haxby.worldwind.layers;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import haxby.util.WESNSupplier;

public class SimpleRenderableLayer extends RenderableLayer implements InfoSupplier, WESNSupplier {
	
	private String infoURL;
	private double[] wesn;
	
	public SimpleRenderableLayer() {
		super();
	}

	public SimpleRenderableLayer(Layer delegateOwner) {
		super();
	}
	
	public String getInfoURL() {
		return infoURL;
	}
	
	public double[] getWESN() {
		return wesn;
	}
	
	public void setInfoURL(String infoURL) {
		this.infoURL = infoURL;
	}
	
	public void setWESN(double[] wesn) {
		this.wesn = wesn;
	}
}

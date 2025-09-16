package haxby.worldwind.layers;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.SurfaceImage;
import haxby.util.LegendSupplier;
import haxby.util.WESNSupplier;

import java.awt.Point;

public class SurfaceImageLayer extends RenderableLayer implements WESNSupplier, LegendSupplier {
	private SurfaceImage si;
	private String legendURL;
	
	public void setSurfaceImage(SurfaceImage si) {
		removeAllRenderables();
		
		this.si = si;
		addRenderable(si);
		setOpacity(getOpacity());
	}
	
	public double[] getWESN() {
		if (si == null) return null;
		Sector sector = si.getSector();
		double[] wesn = new double[] {
				sector.getMinLongitude().degrees,
				sector.getMaxLongitude().degrees,
				sector.getMinLatitude().degrees,
				sector.getMaxLatitude().degrees
		};
		return wesn;
	}
	
	public void setLegendURL(String legendURL) {
		this.legendURL = legendURL;
	}
	
	public String getLegendURL() {
		return legendURL;
	}
	
	@Override
	public void setOpacity(double d) {
		super.setOpacity(d);
		si.setOpacity(d);
	}
	
	@Override
	public void pick(DrawContext arg0, Point arg1) {
//		super.pick(arg0, arg1);
	}
}

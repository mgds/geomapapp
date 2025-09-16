package haxby.worldwind.layers;


import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import haxby.db.fms.Earthquakes.Earthquake;
import haxby.worldwind.renderers.FMSRenderer;
import haxby.worldwind.renderers.FMSRenderer.FMS;

import java.awt.Point;
import java.util.LinkedList;
import java.util.List;

public class FMSLayer extends AbstractLayer {
	
	private FMSRenderer fmsRenderer = new FMSRenderer();
	
	private List<FMS> solutions = new LinkedList<FMS>();
	private List<FMS> toDispose;
	
	private boolean isExtruded = true;
	
	public void addFMS(float lat,
			float lon,
			float depth,
			float strike1, 
			float dip1,
			float rake1,
			float strike2,
			float dip2,
			float rake2)
	{
		solutions.add( fmsRenderer.new FMS(
				lat,
				lon,
				depth,
				strike1,
				dip1,
				rake1,
				strike2,
				dip2,
				rake2));
	}
	
	public void addFMS(Earthquake eq) {
		solutions.add( fmsRenderer.new FMS(eq));
	}
	
	@Override
	public void dispose() {
		super.dispose();
		
		clearSolutions();
	}
	
	public void clearSolutions()
	{
		toDispose = solutions;
		solutions = new LinkedList<FMS>();
	}
	
	@Override
	protected void doRender(DrawContext dc) {
		if (toDispose != null) {
			fmsRenderer.dispose(toDispose);
			toDispose = null;
		}
		
		fmsRenderer.render(dc, solutions, isExtruded);
	}
	
	@Override
	protected void doPick(DrawContext dc, Point point) {
		fmsRenderer.pick(dc, solutions, isExtruded, point, this);
	}
	
	public void setExtruded(boolean tf)
	{
		this.isExtruded = tf;
	}
	
	public boolean isExtruded() {
		return this.isExtruded;
	}
}

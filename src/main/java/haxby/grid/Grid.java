package haxby.grid;

public abstract interface Grid {
	public float[] getGrid();
	public java.awt.Dimension getSize();
	public haxby.proj.Projection getProjection();
}

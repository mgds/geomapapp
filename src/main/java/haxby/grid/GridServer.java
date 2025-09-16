package haxby.grid;

import haxby.proj.Projection;

public abstract interface GridServer {
	public abstract Projection getProjection();
	public abstract double valueAt( int x, int y );
	public abstract int getGridSize();
}

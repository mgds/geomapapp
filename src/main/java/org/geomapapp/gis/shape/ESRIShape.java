package org.geomapapp.gis.shape;

import java.awt.geom.Rectangle2D;
import java.awt.Graphics2D;
import java.awt.Color;
import java.io.OutputStream;
import java.io.IOException;
import org.geomapapp.geom.MapProjection;

import haxby.map.XMap;

public abstract interface ESRIShape extends haxby.map.Overlay {
	public int getType();
	public NearNeighbor select( NearNeighbor neighbor, XMap map);
	public int writeShape( OutputStream out ) throws java.io.IOException;
	public double[][] inverse( MapProjection proj, double[][] bounds);
	public boolean canView( Rectangle2D r, double wrap);
	public void draw(Graphics2D g, Rectangle2D r, double wrap);
//	public void draw(Graphics2D g, Color stroke, Color fill, Rectangle2D r, double wrap);
//	public void setSelected( boolean tf );
//	public void project(MapProjection proj);
}

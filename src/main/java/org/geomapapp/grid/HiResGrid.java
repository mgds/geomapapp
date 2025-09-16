package org.geomapapp.grid;

import org.geomapapp.map.*;

import haxby.proj.*;
import haxby.map.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.tree.*;
import java.util.*;

public class HiResGrid extends MapPlace {
	public int resolution;
	public Rectangle bounds;
	public double[] range;
	public HiResGrid( MapPlace parent, String name ) {
		super( parent, name);
	}
	public HiResGrid( MapPlace parent, 
			String name, 
			double lon, 
			double lat, 
			int zoom,
			Rectangle bounds,
			double[] range ) {
		this( parent, name );
		leaf = true;
		this.lon = lon;
		this.lat = lat;
		this.zoom = (double)zoom;
		resolution = zoom;
		this.bounds = bounds;
//	System.out.println( name +"\t"+ bounds );
		this.range = range;
		selected = false;
		GridComposer.addHiResArea( this );
	}
}

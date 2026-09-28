package org.geomapapp.image;

import org.geomapapp.geom.XYZ;

public class Palettes {
	public Palette ocean;
	public Palette land;
	public Palette both;
	public XYZ sun;
	public double colorInterval;
	public int button;
	public static Palettes clipboard;
	public Palettes( Palette ocean,
			Palette land,
			Palette both,
			XYZ sun,
			double colorInterval,
			int button ) {
		this.ocean = ocean;
		this.land = land;
		this.both = both;
		this.sun = sun;
		this.colorInterval = colorInterval;
		this.button = button;
	}
}

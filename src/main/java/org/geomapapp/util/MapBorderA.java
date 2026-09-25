package org.geomapapp.util;

import org.geomapapp.geom.*;

import java.awt.*;

public class MapBorderA implements javax.swing.SwingConstants {
	boolean graticule;
	int sides = 0;
	public MapBorderA() {
		graticule = false;
	}
	public void setSides( int sides ) {
		this.sides = sides;
	}
	public void setGraticule( boolean tf ) {
		graticule = tf;
	}
}

package haxby.db.ice;

import haxby.map.*;
import haxby.proj.*;
import java.awt.*;
import java.util.*;

public class IceExpedition {
	IceCore[] cores;
	String name;
	public IceExpedition( String name, IceCore[] cores) {
		this.cores = cores;
		this.name = name;
	}
	public String toString() {
		return name;
	}
	public void drawTrack(Graphics2D g) {
		for( int i=0 ; i<cores.length ; i++) {
			cores[i].drawTrack( g );
		}
	}
	public void draw(Graphics2D g) {
		for( int i=0 ; i<cores.length ; i++) {
			cores[i].draw( g );
		}
	}
	public void plotXY( Graphics2D g,
			java.awt.geom.Rectangle2D rect,
			double xScale, double yScale, Vector xy ) {
		for( int i=0 ; i<cores.length ; i++) {
			cores[i].plotXY( g, rect, xScale, yScale, xy);
		}
	}
}

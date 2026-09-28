package haxby.db;

import haxby.map.*;
import haxby.proj.*;
import java.awt.*;
import java.awt.geom.*;

public class StationOverlay implements Overlay {
	double[][] lonlat;
	float[][] xy;
	XMap map;
	Color color;
	Symbol symbol;
	public StationOverlay( double[][] lonlat, XMap map ) {
		this.map = map;
		this.lonlat = lonlat;
		Projection proj = map.getProjection();
		xy = new float[lonlat.length][2];
		for(int i=0 ; i<lonlat.length ; i++) {
			Point2D p = proj.getMapXY( new Point2D.Double(
					lonlat[i][0], lonlat[i][1]) );
			xy[i][0] = (float)p.getX();
			xy[i][1] = (float)p.getY();
		}
		color = Color.black;
		symbol = new Symbol( Symbol.CIRCLE, 5f, Color.black, Color.white );
	}
	public void setSymbol( Symbol s ) {
		symbol = s;
	}
	public Symbol getSymbol() {
		return symbol;
	}
	public void draw(Graphics2D g) {
		if(xy.length <=1) return;
		GeneralPath path = new GeneralPath();
		path.moveTo( xy[0][0], xy[0][1] );
		for( int i=1 ; i<xy.length ; i++) {
			path.lineTo( xy[i][0], xy[i][1] );
		}
		g.draw(path);
	}
}

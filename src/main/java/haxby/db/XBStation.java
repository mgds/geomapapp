package haxby.db;

import haxby.proj.*;
import haxby.map.*;
import haxby.util.*;

import java.awt.*;
import java.awt.geom.*;

public class XBStation implements Overlay {
	Point2D refXY;
	float x, y;
	String name;
	XMap map;
	float size;
	public XBStation(XMap map, Point2D refXY, String name, float size) {
		this.refXY = refXY;
		this.map = map;
		Point2D mapXY = map.getProjection().getMapXY(refXY);
		x = (float)mapXY.getX();
		y = (float)mapXY.getY();
		this.name = name;
		this.size = size;
	}
	public void draw( Graphics2D g ) {
		GeneralPath path = new GeneralPath();
		float dx = size / (float)map.getZoom();
		path.moveTo( x-dx, y-dx );
		path.lineTo( x+dx, y-dx );
		path.lineTo( x+dx, y+dx );
		path.lineTo( x-dx, y+dx );
		path.closePath();
		g.setColor(Color.black);
		g.setStroke( new BasicStroke(2f/(float)map.getZoom()) );
		g.draw( path );
		g.setColor( Color.white );
		g.fill( path);
		if( name==null) return;
		Font font = new Font( "SansSerif", Font.PLAIN, 9);
		font = font.deriveFont( 9f/(float)map.getZoom());
		g.setFont(font);
		g.translate( x+dx*1.5f, y+dx);
		g.setColor( Color.black);
		g.drawString( name, 0, 0);
		g.translate( -(x+dx*1.5f), -(y+dx));
	}
}

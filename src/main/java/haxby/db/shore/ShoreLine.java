package haxby.db.shore;

import haxby.map.Overlay;
import haxby.map.XMap;
import haxby.proj.Projection;
import haxby.util.URLFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.URL;

public class ShoreLine implements Overlay {
	XMap map;
	GeneralPath path;

	float lineWidth;
	Color color;
	boolean visible;

	public ShoreLine( XMap map ) {
		this.map = map;
		path = null;
		color = Color.blue;
		lineWidth = 3f;
		visible = false;
	}

	public boolean isVisible() {
		return visible;
	}

	public float getWidth() {
		return lineWidth;
	}

	public Color getColor() {
		return color;
	}

	public void setWidth(float width) {
		lineWidth = width;
	}

	public void setColor(Color c) {
		color = c;
	}

	public void load( String urlname ) throws IOException {
		if( urlname==null ) {
			path=null;
			return;
		}
		URL url = URLFactory.url( urlname );
		DataInputStream in = new DataInputStream( url.openStream() );
		int n;
		Projection proj = map.getProjection();
		path = new GeneralPath();
		while( true ) {
			try {
				n = in.readInt();
			} catch(EOFException ex ) {
				break;
			}
			for( int k=0 ; k<n ; k++) {
				Point2D.Double p = new Point2D.Double(
					1.e-06*in.readInt(), 1.e-06*in.readInt() );
				p = (Point2D.Double) proj.getMapXY(p);
				if( k==0 ) path.moveTo( (float)p.x, (float)p.y );
				else path.lineTo( (float)p.x, (float)p.y );
			}
		}
	}
	public void setVisible( boolean tf ) {
		visible = tf;
	}
	public void draw( Graphics2D g ) {
		if( !visible || path==null ) return;
		g.setColor( color );
		g.setStroke( new BasicStroke(
				lineWidth/(float)map.getZoom(),
				BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND) );
		g.draw( path );
	}
}

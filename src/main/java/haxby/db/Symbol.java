package haxby.db;

import java.awt.*;
import java.awt.geom.*;

public class Symbol {
	public static final int CIRCLE = 0;
	public static final int SQUARE = 1;
	public static final int DIAMOND = 2;
	public static final int TRIANGLE = 3;

	int type;
	float size;
	Shape shape;
	Color fill;
	Color outline;
	float scale;
	public Symbol( int type, float size, Color outline, Color fill ) {
		this.type = type;
		this.size = size;
		this.outline = outline;
		this.fill = fill;
		shape = null;
		scale = 1f;
		doShape();
	}
	void doShape() {
		if( type==0 ) doCircle();
		else if( type==1 ) doSquare();
		else if( type==2 ) doDiamond();
		else if( type==3 ) doTriangle();
	}
	void doCircle() {
		float s = size/scale;
		Arc2D.Float arc = new Arc2D.Float( -s/2f, -s/2f, s, s,
				0f, 360f, Arc2D.CHORD);
		shape = arc;
	}
	void doSquare() {
		float s = size/scale;
		Rectangle2D.Float square = new Rectangle2D.Float( -s/2f, -s/2f, s, s );
		shape = square;
	}
	void doDiamond() {
		GeneralPath path = new GeneralPath();
		float s = size / (float)Math.sqrt(2.) / scale;
		path.moveTo( 0, s );
		path.lineTo( s, 0 );
		path.lineTo( 0, -s );
		path.lineTo( -s, 0 );
		path.closePath();
		shape = path;
	}
	void doTriangle() {
		GeneralPath path = new GeneralPath();
		float s = size / (float)Math.sqrt(2.) / scale;
		path.moveTo( 0, -s );
		s = size / 2f / scale;
		path.lineTo( s, s);
		path.lineTo( -s, s);
		path.closePath();
		shape = path;
	}
	public void draw(Graphics2D g) {
		if(fill != null) {
			g.setColor(fill);
			g.fill( shape);
		}
		if( outline != null ) {
			g.setColor(outline);
			g.setStroke( new BasicStroke( 1f ) );
			g.draw( shape );
		}
	}
}

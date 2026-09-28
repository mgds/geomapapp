package org.geomapapp.util;

import java.awt.*;
import java.awt.geom.*;

public class Symbol {
	Color outline;
	Color fill;
	GeneralPath shape;
	float lineWidth;
	public Symbol( Shape shape, Color outline, Color fill, float lineWidth ) {
		this.shape = new GeneralPath(shape);
		this.outline = outline;
		this.fill = fill;
		this.lineWidth = lineWidth;
	}
	public void setOutline(Color color) {
		outline = color;
	}
	public void setFill(Color color) {
		fill = color;
	}
	public void setLineWidth(float w) {
		lineWidth = w;
	}
	public Rectangle getBounds() {
		Rectangle r = shape.getBounds();
		int dx = 2;
		if( lineWidth>=1f ) dx += (int)lineWidth;
		r.x -= dx;
		r.y -= dx;
		r.width += 2*dx;
		r.height += 2*dx;
		return r;
	}
	public void draw(Graphics2D g) {
		if( fill!=null ) {
			g.setColor(fill);
			g.fill(shape);
		}
		if( outline!=null ) {
			g.setColor(outline);
			g.setStroke( new BasicStroke(lineWidth) );
			g.draw(shape);
		}
	}
}
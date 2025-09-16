package haxby.image;

import java.awt.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.awt.event.*;
import javax.swing.*;

public class TextImage implements ComposerElement {
	String text;
	GlyphVector glyph;
	Point location;
	Font font;
	Color outline, fill;
//	Rectangle2D.Double bounds;
	Rectangle bounds;
	boolean selected, visible;
	public TextImage( Point p ) {
		this.location = p;
		text = null;
		glyph = null;
		font = null;
		outline = null;
		bounds = null;
		fill = Color.black;
		selected = false;
		visible = true;
	}
	public void setText(String text, Font font, FontRenderContext context) {
		this.text = text;
		this.font = font;
		glyph = font.createGlyphVector( context, text);
		Rectangle2D bounds = glyph.getVisualBounds();
		this.bounds = new Rectangle( (int)bounds.getX()-3, (int)bounds.getY()-3,
				(int)bounds.getWidth() + 6, (int)bounds.getHeight() + 6 );
	}
	public String getText() {
		return text;
	}
	public void draw( Graphics2D g ) {
		if( text==null ) return;
		g.setStroke( new BasicStroke(1f) );
		g.setColor( Color.white );
		g.translate( location.x, location.y );
		g.fill( bounds );
		g.setColor( Color.black );
		g.draw(bounds);
		g.fill( glyph.getOutline() );
		g.translate( -location.x, -location.y );
	}
	public boolean select( Point2D p ) {
		return bounds.contains( p.getX()-location.getX(),
					p.getY()-location.getY());
	}
	public void setSelected( boolean tf ) {
		selected = tf;
	}
	public boolean isSelected() {
		return selected;
	}
	public void setVisible( boolean tf ) {
		visible = tf;
	}
	public boolean isVisible() {
		return visible;
	}
	public void dragged( Point2D from, Point2D to ) {
		location.x += (int)Math.rint(to.getX()-from.getX());
		location.y += (int)Math.rint(to.getY()-from.getY());
	}
	public Shape getShape() {
		GeneralPath path = new GeneralPath();
		Rectangle rect = new Rectangle( location.x+bounds.x,
					location.y+bounds.y, bounds.width, bounds.height);
		path.append( rect, false);
		path.append( glyph.getOutline(), false );
		return path;
	}
}

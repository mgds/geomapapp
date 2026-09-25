package haxby.image;

import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.awt.event.*;
import javax.swing.*;

public class TextElement implements ComposerElement {
	String text;
	GlyphVector glyph;
	GlyphVector[] glyphs;
	Point location;
	Font font;
	Color outline, fill;
//	Rectangle2D.Double bounds;
	Rectangle bounds;
	double offset;
	boolean selected, visible;
	public TextElement( Point p ) {
		this.location = p;
		text = null;
		glyph = null;
		glyphs = null;
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
		StringTokenizer st = new StringTokenizer( text, "\n");
		if( st.countTokens()<=1 ) {
			glyph = font.createGlyphVector( context, text);
			Rectangle2D bounds = glyph.getVisualBounds();
			this.bounds = new Rectangle( (int)bounds.getX()-3, (int)bounds.getY()-3,
					(int)bounds.getWidth() + 6, (int)bounds.getHeight() + 6 );
			offset = bounds.getHeight();
		} else {
			glyphs = new GlyphVector[st.countTokens()];
			double h=0.;
			double w=0.;
			Rectangle2D[] bounds = new Rectangle2D[st.countTokens()];
			for( int k=0 ; k<glyphs.length ; k++) {
				glyphs[k] = font.createGlyphVector( context, st.nextToken() );
				bounds[k] = glyphs[k].getVisualBounds();
				h = Math.max( h, Math.abs( bounds[k].getHeight() ) );
				w = Math.max( w, Math.abs( bounds[k].getWidth() ) );
			}
			this.bounds = new Rectangle( (int)bounds[0].getX()-3, (int)bounds[0].getY()-3,
					(int)w + 6, (int)(h*1.2*glyphs.length) + 6 );
			offset = h*1.2;
		}
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
		if( glyph != null ) {
			g.fill( glyph.getOutline() );
			g.translate( -location.x, -location.y );
		} else if( glyphs!=null) {
			AffineTransform at = g.getTransform();
			for( int k=0 ; k<glyphs.length ; k++ ) {
				g.fill( glyphs[k].getOutline() );
				g.translate( 0., offset );
			}
			g.setTransform( at );
		}
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
		return path;
	}
}

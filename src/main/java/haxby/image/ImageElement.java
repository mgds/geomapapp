package haxby.image;

import haxby.map.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import javax.swing.*;

public class ImageElement implements ComposerElement {
	static BufferedImage arrow=null;
	static Point arrowOffset;
	Point location;
	Rectangle bounds;
	boolean visible=true;
	BufferedImage image;
	public ImageElement( Point p ) {
		location = p;
		if( arrow==null ) initArrow();
		location.x -= arrowOffset.x;
		location.y -= arrowOffset.y;
		bounds = new Rectangle( location.x, location.y,
				arrow.getWidth(), arrow.getHeight() );
		image = arrow;
	}
	public ImageElement( Point p, BufferedImage image ) {
		this.image = image;
		location = p;
		bounds = new Rectangle( location.x, location.y,
				image.getWidth(), image.getHeight() );
	}
	public void draw( Graphics2D g ) {
		g.drawImage( image, location.x, location.y, new JPanel() );
	}
	public boolean select( Point2D p ) {
		return bounds.contains(p);
	}
	public void setSelected( boolean tf ) {
	}
	public boolean isSelected() {
		return false;
	}
	public void setVisible( boolean tf ) {
		visible = tf;
	}
	public boolean isVisible() {
		return visible;
	}
	public void dragged( Point2D from, Point2D to ) {
		int dx = (int)Math.rint( to.getX()-from.getX() );
		int dy = (int)Math.rint( to.getY()-from.getY() );
		location.x += dx;
		location.y += dy;
		bounds.x += dx;
		bounds.y += dy;
	}
	public Shape getShape() {
		GeneralPath path = new GeneralPath();
		path.append( bounds, false );
		path.append( new Rectangle(bounds.x-1, bounds.y-1, bounds.width+2, bounds.height+2), false );
		path.append( new Rectangle(bounds.x+1, bounds.y+1, bounds.width-2, bounds.height-2), false );
		return path;
	}
	static void initArrow() {
		JPanel panel = new JPanel();
		ImageIcon sel = MapTools.SELECT( true );
		Image im = sel.getImage();
		int w = im.getWidth( panel );
		int h = im.getHeight( panel );
		arrow = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = arrow.createGraphics();
		g.drawImage( im, 0, 0, panel);
		int black = 0xff000000;
		int white = 0xfff0f0f0;
		boolean offset = true;
		arrowOffset = new Point();
		for( int y=0 ; y<h ; y++ ) {
			for( int x=0 ; x<w ; x++) {
				int c = arrow.getRGB(x, y);
				if( c==white || c==black ) {
					if(offset) {
						arrowOffset = new Point( x+1, y+1);
						offset=false;
					}
					continue;
				}
				arrow.setRGB( x, y, 0);
			}
		}
	}
}

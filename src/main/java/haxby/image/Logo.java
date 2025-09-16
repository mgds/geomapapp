package haxby.image;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import java.io.*;
import java.util.zip.*;
import java.net.URL;
import haxby.map.*;

public class Logo extends JComponent implements MapInset {
	BufferedImage image;
	Point location;
	XMap map;
	public Logo(XMap map) {
		try {
			DataInputStream in = new DataInputStream(
				new GZIPInputStream(
				new FileInputStream( "/scratch/ridgembs/bill/antarctic/logos/nsf.img" )));
			BufferedImage im = new BufferedImage( in.readInt(), in.readInt(),
				BufferedImage.TYPE_INT_ARGB);
			for( int y=0 ; y<im.getHeight() ; y++ ) {
				for( int x=0 ; x<im.getWidth() ; x++) {
					im.setRGB( x, y, in.readInt() );
				}
			}
			image = new BufferedImage( im.getWidth()/2, im.getHeight()/2,
					BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			AffineTransform at = new AffineTransform();
			at.scale( .5, .5);
			g.drawRenderedImage( im, at );
		} catch( Exception ex ) {
			ex.printStackTrace( System.err );
			image = null;
		}
		this.map = map;
	}
	public Dimension getPreferredSize() {
		if( image==null ) return new Dimension(10, 10);
		return  new Dimension( image.getWidth(), image.getHeight() );
	}
	public Rectangle getBounds() {
		Point p = getLocation();
		return new Rectangle( p.x, p.y, image.getWidth(), image.getHeight() );
	}
	public Point getLocation() {
		Rectangle r = map.getVisibleRect();
		return new Point( r.x+20, r.y+20 );
	}
	public void paintComponent( Graphics g ) {
		if( image==null ) return;
		g.drawImage( image, 0, 0, this );
	}
	public void draw( Graphics2D g, int w, int h ) {
		if( image==null ) return;
		g.drawImage( image, 20, 20, this );
	}
}

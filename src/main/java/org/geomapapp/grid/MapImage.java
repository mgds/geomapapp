package org.geomapapp.grid;

import haxby.map.XMap;
import org.geomapapp.geom.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.*;

public class MapImage {
	XMap map;
	public MapImage( XMap map ) {
		this.map = map;
	}
	public Grid2D.Image getImage( boolean geo ) {
		MapProjection proj = (MapProjection)map.getProjection();
		if( !proj.isCylindrical() ) return null;
		Rectangle r = map.getVisibleRect();
		Insets insets = map.getInsets();
		int x = r.x + insets.left;
		int y = r.y + insets.top;
		int width = r.width - insets.left - insets.right;
		int height = r.height - insets.top - insets.bottom;
		BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = im.createGraphics();
	//	g2d.translate(-r.getX(), -r.getY());
		g2d.translate(-x, -y);
		map.paint( g2d );
//		try {
//			javax.imageio.ImageIO.write( im, "jpg", new java.io.File("test.jpg") );
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
		Rectangle2D.Double r2 = (Rectangle2D.Double)map.getClipRect2D();
		Point2D.Double pt = (Point2D.Double)proj.getRefXY( new Point2D.Double( r2.x, r2.y ));
		Mercator merc = new Mercator( pt.x, pt.y, map.getWrap()*map.getZoom(), 0, 2);
		Grid2D.Image grid = new Grid2D.Image( new Rectangle(0,0,width,height),
						merc, im );
		if( geo ) return grid.getGeoImage();
		return grid;
	}
}

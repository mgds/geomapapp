package haxby.worldwind.db.scs;

import haxby.db.scs.SCSCruise;
import haxby.nav.ControlPt;
import haxby.worldwind.layers.dynamic_tiler.AbstractTrackTiler;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class SCSTrackTiler extends AbstractTrackTiler {

	protected SCSCruise[] cruises;
	
	public SCSTrackTiler(SCSCruise[] cruises) {
		this.cruises = cruises;
	}

	@Override
	public BufferedImage drawTracksInBounds(int level, Rectangle2D tileBounds) {
		BufferedImage img = new BufferedImage(512,512, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		Composite c = g.getComposite();
		g.setComposite( AlphaComposite.getInstance(AlphaComposite.CLEAR, 0));
		g.fillRect(0, 0, 512, 512);
		g.setComposite(c);
		
		AffineTransform at = g.getTransform();
		
		Stroke s = g.getStroke();
		int width = 1;
		g.setStroke(new BasicStroke( width ) );
		g.setColor( Color.black );
		
//		g.scale(512 / tileBounds.getWidth(), 512 / tileBounds.getHeight());
//		g.translate(-tileBounds.getX(), -tileBounds.getY());
		
		double x0 = tileBounds.getX();
		double y0 = -tileBounds.getY() - tileBounds.getHeight();
		
		double dx = tileBounds.getWidth();
		double dy = tileBounds.getHeight();
		
		for (SCSCruise cruise : cruises)
		{
			if (cruise == null) continue;
			if (!cruise.getNav().getBounds().intersects(tileBounds)) continue;
			
			ControlPt[][] cpts = cruise.getNav().getCpts();
			GeneralPath path = new GeneralPath();
			
			boolean wrap = false;
			boolean wrapDown = false;
			for( int seg=0 ; seg<cpts.length ; seg++ ) {
				float pntX0 = (float) cpts[seg][0].getX();
				
				float x = (float) ((pntX0 - x0) / dx * 512);
				float y = (float) ((-cpts[seg][0].getY() - y0) / dy * 512);
				
				path.moveTo( x, y );
				for(int i=1 ; i<cpts[seg].length ; i++) {
					float pntX1 = (float) cpts[seg][i].getX();
					
					if (Math.abs(pntX1 - pntX0) > 180) 
					{
						if (pntX1 > pntX0)
						{
							if (!wrap)
								wrapDown = true;
							pntX1 -= 360;
						}
						else 
						{
							if (!wrap)
								wrapDown = false;
							pntX1 += 360;
						}
						wrap = true;
					}
					
					x = (float) ((pntX1 - x0) / dx * 512);
					y = (float) ((-cpts[seg][i].getY() - y0) / dy * 512);
					
					path.lineTo( x, y);
					
					pntX0 = pntX1;
				}
			}
			g.draw(path);
			if (wrap)
			{
				if (wrapDown)
					g.translate(360 * 512 / dx, 0);
				else
					g.translate(-360 * 512 / dx, 0);
				g.draw(path);
			}
			g.setTransform(at);
		}
		
		g.setStroke(s);
		return img;
	}
}

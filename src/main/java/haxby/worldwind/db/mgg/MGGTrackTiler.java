package haxby.worldwind.db.mgg;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.TextureTile;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import haxby.db.mgg.MGGTrack;
import haxby.nav.ControlPt;
import haxby.worldwind.layers.dynamic_tiler.AbstractTrackTiler;
import haxby.worldwind.layers.dynamic_tiler.DynamicTiler;

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
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;

import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

public class MGGTrackTiler extends AbstractTrackTiler {

	protected MGGTrack[] tracks;
	protected byte types;
	
	public MGGTrackTiler(MGGTrack[] tracks, byte types)
	{
		this.tracks = tracks;
		this.types = types;
	}
	
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
		
		for (MGGTrack track : tracks)
		{
			if (track == null) continue;
			if ((track.getTypes() & types) == 0) continue;
			if (!track.getNav().getBounds().intersects(tileBounds)) continue;
			
			ControlPt[][] cpts = track.getNav().getCpts();
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

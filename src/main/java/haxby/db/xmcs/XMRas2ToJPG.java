package haxby.db.xmcs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import haxby.map.XMap;
import haxby.proj.ProjectionFactory;
import haxby.util.PathUtil;

//import com.sun.image.codec.jpeg.JPEGCodec;
//import com.sun.image.codec.jpeg.JPEGImageEncoder;

public class XMRas2ToJPG {

	/**
	 * Takes a mcs_control and produces full jpg images for each entry in mcs_control
	 * Corresponding raster images must be in MCS/cruiseID/img/
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Ussage: java haxby.db.xmcs.XMRas2ToJPG cruiseID radar");
			System.err.println("\tShould be run with extra heap space allocated (-Xmx256m)");
			System.exit(0);
		}

		XMap map = new XMap(null,ProjectionFactory.getMercator(640), 1000,600);
		XMCruise.MULTI_CHANNEL_PATH = PathUtil.getPath("PUBLIC_HOME_PATH")+"MCS/";

		String cruiseID = args[0];
		final String isRadar = args.length > 1 ? args[1] : "";
		Boolean loadFromFile = args.length == 1;
		
		XMCruise cruise = new XMCruise(null, map, cruiseID);
		XMLine[] lines;
		try {
			if (loadFromFile) {
				lines = cruise.loadLinesFromFile();
			} else {
				lines = cruise.loadLines();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		XMImage oImage = new XMImage();
		XMImage image = new XMImage() {
			public void saveJPEG(OutputStream out) throws IOException {
				if(image==null) throw new IOException("no image loaded");
				xAvg = yAvg = xRep = yRep = 1;
				Rectangle rect = getVisibleRect();
				Dimension dim = getPreferredSize();
				rect.width = dim.width-rect.x;
				rect.height = dim.height-rect.y;
				BufferedImage im = new BufferedImage( rect.width, rect.height, BufferedImage.TYPE_INT_RGB);
				Graphics2D g2 = im.createGraphics();
				g2.translate(-rect.x, -rect.y);
				if(border != null) {
					Dimension size = getPreferredSize();
					Rectangle bounds = new Rectangle(0, 0, size.width, size.height);
					if(rect.contains(bounds)) {
						rect=bounds;
						g2.clipRect(rect.x, rect.y, rect.width, rect.height);
					}
					Insets ins = border.getBorderInsets(this);
					border.setYTitle(isRadar);
					border.paintBorder(this, g2, rect.x, rect.y, rect.width, rect.height);
					int[] seg = {cdpAt(rect.x), cdpAt(rect.x + rect.width)};
					if( seg[1]>seg[0]) {
					//	GeneralPath path = new GeneralPath();
					//	path.moveTo(0f, 0f);
					//	path.lineTo( 4f, -10f );
					//	path.lineTo( -4f, -10f );
					//	path.closePath();
						g2.setFont( new Font("SansSerif", Font.BOLD, 10));
						FontMetrics fm = g2.getFontMetrics();
						AffineTransform at = g2.getTransform();
						double scale = (double)(rect.width-ins.left-ins.right) / 
							(double)(seg[1]-seg[0]);
						if(isRevVid()) {
						//	g2.setColor( new Color( .75f, .75f, .75f, .75f) );
							g2.setColor( Color.yellow);
						} else {
						//	g2.setColor( new Color( .25f, .25f, .25f, .75f) );
							g2.setColor( Color.blue);
						}
						for( int k=0 ; k<line.crossings.size() ; k++) {
							XMCrossing crs = (XMCrossing)line.crossings.get(k);
							if( crs.cdp1<seg[0] || crs.cdp1>seg[1])continue;
							int x = isFlip() ?
								rect.x+ins.left+(int)Math.rint((-crs.cdp1+seg[1])*scale) :
								rect.x+ins.left+(int)Math.rint((crs.cdp1-seg[0])*scale);
							g2.translate( x, rect.y+ins.top );
							g2.drawLine( 0, 0, 0, -12 );
						//	g2.fill(path);
							g2.drawString( crs.cross.toString(), 
								-fm.stringWidth(crs.cross.toString())/2, -14);
							g2.setTransform(at);
						}
					}
					g2.translate(ins.left, ins.top);
					g2.clipRect(rect.x, rect.y, 
						rect.width-ins.left-ins.right, 
						rect.height-ins.top-ins.bottom);
				}
				if(!image.isFlip() && getZoomX()==1 && getZoomY()==1) {
					g2.drawImage(image.getImage(), 0, 0, this);
				} else {
					Rectangle r = image.getImageableRect(g2.getClipBounds(), xAvg, yAvg, xRep, yRep);
					if(r.width >0 || r.height>0 ) {
						BufferedImage im1 = image.getScaledImage(r, xAvg, yAvg, xRep, yRep);
						g2.drawImage( im1, r.x, r.y, this);
					}
				}
				g2.translate( rect.x, rect.y );
				//JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
				//encoder.encode(im);
				ImageIO.write(im, "JPEG", out);
				out.flush();
				out.close();
			}
		};
		image.otherImage = oImage;

		for (int i = 0; i < lines.length; i++) {
			try {
				if (loadFromFile) {
					image.loadImageFromFile(lines[i]);
				} else {
					image.loadImage(lines[i]);
				}
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(cruiseID+"-"+lines[i].lineID+".jpg")));
				image.saveJPEG(out);
				System.out.println(cruiseID+"-"+lines[i].lineID+".jpg");
			} catch (IOException e) {
				System.err.println("error on XMLine " + lines[i].lineID);
				e.printStackTrace();
			}
		}
	}
}

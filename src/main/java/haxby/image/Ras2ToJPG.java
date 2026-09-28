package haxby.image;

import haxby.db.radar.RCruise;
import haxby.db.radar.RLine;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;

//import com.sun.image.codec.jpeg.JPEGCodec;
//import com.sun.image.codec.jpeg.JPEGImageEncoder;

public class Ras2ToJPG {
	RLine line;
	Ras2ToJPG.RBorder border;
	int xAvg, yAvg, xRep, yRep;
	double[] cdpInterval;
	double[] tRange;
	ScalableImage image;
	int width, height;
	
	public static void main(String[] args) throws Exception{
		if (args.length != 4) {
			System.out.println("Ussage: RRasToJPG cruiseName lineID BoundsFile ImageDir");
			return;
		}
		Ras2ToJPG ras2jpg = new Ras2ToJPG();
		RCruise cruise = new RCruise(null,null,args[0]);
		RLine line = new RLine(cruise,args[1]);
		
		// Load bounds
		File boundsFile = new File(args[2]);
		File imgDir = new File(args[3]);
		System.out.println(boundsFile);
		System.out.println(imgDir);
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(boundsFile)));
		
		String in = reader.readLine();
		while (in != null) {
			String[] split = in.split("\t");
			String cruiseID =  split[0].trim();
			if (!cruiseID.equals(args[0])) {
				in = reader.readLine();
				continue;
			}
			String lineID =  split[1].trim();
			if (!lineID.equals(args[1])) {
				in = reader.readLine();
				continue;
			}
			
			double[] cdpRange = new double[] {
					Double.parseDouble( split[2]),
					Double.parseDouble( split[3]) };
			double[] zRange = new double[] {
					Double.parseDouble( split[4]),
					Double.parseDouble( split[5]) };
			line.setRanges( cdpRange, zRange );
			break;
		}
		
		if (line.getCDPRange() == null) {
			System.out.println("Could not find cruise/line in bounds file");
			return;
		}
		
		ras2jpg.loadImage(imgDir, line);
		
		BufferedOutputStream out = new BufferedOutputStream(
									new FileOutputStream(args[1] + ".jpg"));	
		ras2jpg.saveJPEG(out);
		
		out.close();
	}
	
	public void loadImage(File imgDir, RLine line ) throws IOException {
		if( line.getZRange()==null ) throw new IOException(" no data for "+line.getID());
		this.line = line;
		xRep = yRep = 1;
		xAvg = yAvg = 1;
		cdpInterval = line.getCDPRange();
		tRange = line.getZRange();
		image = null;
		System.gc();
		DataInputStream in = null;
		
		String fs = System.getProperty("file.separator");

		File f = new File(imgDir.toString() 
				+fs+ line.getID().trim() + ".r2.gz" );
		in = new DataInputStream(
			new GZIPInputStream(
			new BufferedInputStream(
			new FileInputStream(f))));
		if( in.readInt() != R2.MAGIC ) throw new IOException("unknown format");
		width = in.readInt();
		height = in.readInt();
		if( in.readInt() != 2 ) throw new IOException("unknown format");
		int size = in.readInt();
		for( int i=0 ; i<3 ; i++) in.readInt();
		byte[] bitmap = new byte[size];
		int pos = 0;
		int n=0;
		try {
			in.readFully(bitmap);
		} catch (IOException ex) {
		}
		image = new R2(bitmap, width, height);
		try {if(in != null) 
			in.close();
		} catch( Exception ex1 ) {
		}
		
		border = new Ras2ToJPG.RBorder(line, width, height);
		border.setTitle();
	}
	
	public Dimension getPreferredSize() {
		if(image==null) return new Dimension( 1000, 200 );
		Dimension size = new Dimension( width*xRep/xAvg, height*yRep/yAvg);
		if(border != null) {
			Insets ins = border.getBorderInsets();
			size.width += ins.left + ins.right;
			size.height += ins.top + ins.bottom;
		}
		return size;
	}
	
	public void saveJPEG(OutputStream out) throws IOException {
		if(image==null) throw new IOException("no image loaded");
		Rectangle rect = new Rectangle();
		Dimension dim = getPreferredSize();
		
		xAvg = 1;
		yAvg = 1;
		dim = getPreferredSize();
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
			Insets ins = border.getBorderInsets();
			border.paintBorder(null, g2, rect.x, rect.y, rect.width, rect.height);
			g2.translate(ins.left, ins.top);
			g2.clipRect(rect.x, rect.y, 
				rect.width-ins.left-ins.right, 
				rect.height-ins.top-ins.bottom);
		}
		g2.drawImage(image.getImage(), 0, 0, null);
		g2.translate( rect.x, rect.y );
		//JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
		//encoder.encode(im);
		ImageIO.write(im, "JPEG", out);
		out.flush();
	}
	
	private static class RBorder {
		String cruise, id;
		boolean paintTitle = true;
		Font titleFont, font;
		int titleH, titleWidth, titleY;
		int anotH, anotY, anotW;
		String title, yTitle;
		Insets insets;
		int width, height;
		double[] cdpInterval, tRange;
		static double[] dAnot = {2, 2.5, 2};
		static AffineTransform rotate90 = new AffineTransform(0, -1, 1, 0, 0, 0);
		public RBorder(RLine line, int width, int height) {
			this.cruise = line.getCruiseID();
			this.id = line.getID();
			this.width = width;
			this.height = height;
			this.cdpInterval = line.getCDPRange();
			this.tRange = line.getZRange();
			font = new Font("SansSerif", Font.PLAIN, 10);
			titleFont = new Font("SansSerif", Font.PLAIN, 12);
			title = cruise +", Line "+ id +", Shot#";
			yTitle = "2-way time, microsec";
			
			BufferedImage img = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
			setTitleSize(img.createGraphics().getFontMetrics(titleFont));
			setAnotSize(img.createGraphics().getFontMetrics(font));
		}
		public Insets getBorderInsets() {
			return new Insets(titleH + anotH, anotW, 0, 0);
		}
		public Insets getBorderInsets(Insets insets) {
			if(insets == null)return getBorderInsets();
			insets.left = anotW;
			insets.top = titleH + anotH;
			insets.right = 0;
			insets.bottom = 0;
			return insets;
		}
		public boolean isBorderOpaque() { return true; }
		public void paintBorder(Component c, Graphics g,
					int x, int y, int w, int h) {
			Graphics2D g2 = (Graphics2D)g;
			AffineTransform at = g2.getTransform();
			g.setColor(Color.white);
			g.fillRect(x,y,w,h);
			g.setColor(Color.black);
			if(paintTitle) {
				g.setFont(titleFont);
				g.drawString(title, x + (w - titleWidth)/2, y+titleY);
			}
			FontMetrics fm = g.getFontMetrics(font);
			x += anotW;
			w -= anotW;
			y += titleH+anotH;
			h -= titleH+anotH;
			g.translate(x, y);
			g.drawRect(-1,-1,w+2,h+2);
			g.drawRect(-1,-1,w+2,h+2);
			double zoom = 1;
			double cdp1 = cdpInterval[0];
			double cdp2 = cdpInterval[1];
			double cdpScale = (cdp2 - cdp1) / (zoom*width);
			double cdpMin;
			double cdpMax;
			
			cdpMin = cdp1 + (x-anotW)*cdpScale;
			cdpMax = cdpMin + w*cdpScale ;
				
			int k = 0;
			double cdpInt = 1;
			int a1 = (int)Math.ceil(cdpMin/cdpInt);
			int a2 = (int)Math.floor(cdpMax/cdpInt);
			int wMax = 2*fm.stringWidth(Integer.toString(a2));
			while( wMax*(a2-a1+1) > w ) {
				cdpInt *= dAnot[k];
				k = (k+1)%3;
				a1 = (int)Math.ceil(cdpMin/cdpInt);
				a2 = (int)Math.floor(cdpMax/cdpInt);
			} 
			a1 = (int)Math.ceil(cdpMin/cdpInt);
			a2 = (int)Math.floor(cdpMax/cdpInt);
			int da = (int)Math.rint(cdpInt);
			int ax, aw;
			String anot;
			g.setFont(font);
			for( k=a1 ; k<=a2 ; k++ ) {
				ax = (int)Math.rint( (cdpInt*(double)k - cdpMin)/cdpScale );
				g.fillRect(ax-1, -5, 2, 5);
				anot = Integer.toString((int)(k*cdpInt));
				aw = fm.stringWidth(anot);
				g.drawString(anot, ax-aw/2, -6);
			}

			g.translate(0, h);
			g2.transform(rotate90);
			g.setFont(titleFont);
			g.drawString(yTitle, (h-fm.stringWidth(yTitle))/2, -anotH);
			g.setFont(font);
			zoom = 1;
			double t1 = tRange[0]/10;
			double t2 = tRange[1]/10;
			double tScale = (t2 - t1) / (zoom*height);
			double tMin = t1 + (y-titleH-anotH)*tScale;
			double tMax = tMin + h*tScale ;
			k = 0;
			double tInt = 1;
			a1 = (int)Math.ceil(tMin/tInt);
			a2 = (int)Math.floor(tMax/tInt);
			wMax = 2*fm.stringWidth(Integer.toString(a2));
			while( wMax*(a2-a1+1) > h ) {
				tInt *= dAnot[k];
				k = (k+1)%3;
				a1 = (int)Math.ceil(tMin/tInt);
				a2 = (int)Math.floor(tMax/tInt);
			} 
			a1 = (int)Math.ceil(tMin/tInt);
			a2 = (int)Math.floor(tMax/tInt);
			da = (int)Math.rint(tInt);
			for( k=a1 ; k<=a2 ; k++ ) {
				ax = h-(int)Math.rint( (tInt*(double)k - tMin)/tScale );
				g.fillRect(ax-1, -5, 2, 5);
				da = (int)(k*tInt);
				anot = Integer.toString(da/100);
				int tmp = da%100;
				if( tmp != 0 ) {
					if( tmp < 10 ) {
						anot += ".0"+tmp;
					} else if( tmp%10 == 0) {
						anot += "."+(tmp/10);
					} else {
						anot += "."+tmp;
					}
				}
				aw = fm.stringWidth(anot);
				g.drawString(anot, ax-aw/2, -6);
			}
			g2.setTransform(at);
		}
		public void setTitle() {
			title = cruise +" Line "+ id +", Shot#";
		}
		void setTitleSize(FontMetrics fm) {
			if( !paintTitle ) {
				titleH = 0;
				return;
			}
			titleWidth = fm.stringWidth(title);
			titleY = fm.getLeading() + fm.getHeight();
			titleH = titleY + fm.getDescent();
		}
		void setAnotSize(FontMetrics fm) {
			anotH = fm.getLeading() + fm.getHeight() + 5;
			anotY = fm.getLeading() + fm.getHeight();
			anotW = fm.getLeading() + fm.getHeight() + anotH;
		}
	}
}

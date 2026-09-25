package org.geomapapp.db.dsdp;

import haxby.map.MapApp;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;

public class CoreColorDisplay extends JComponent {
	DSDPHole hole;
	double zScale;
	MouseAdapter mouse;
	int prevAge = -1;
	Color backgroundC = Color.white;
	
	static String DSDP_COLOR_PATH = PathUtil.getPath("DSDP/DSDP_COLOR_PATH",
			MapApp.BASE_URL+"/data/portals/dsdp/RGB/");

	List<ColorEntry> colorEntries;
	double bottomDepth = 0.0;
	static final int CORE_DESCRIPTIONS_DISPLAY_WIDTH = 20;
	private static final String errMsg = "Error attempting to launch web browser";
	DSDPCore[] cores;
	
	boolean exists = true;
	
	public static class ColorEntry {
		public float startDepth, endDepth;
		public int[] rgb;
		public ColorEntry(float startDepth, float endDepth, int[] rgb) {
			super();
			this.startDepth = startDepth;
			this.endDepth = endDepth;
			this.rgb = rgb;
		}
	}
	
	public CoreColorDisplay(DSDPHole hole) {
			setToolTipText("Click for Core Descriptions");
			zScale = 2.;
			this.hole = hole;
			mouse = new MouseAdapter() {
				public void mouseEntered(MouseEvent e) {
					e.getComponent().requestFocus();
				}
			};
			cores = hole.cores;
			readCoreDescriptionList();
	}
	
	public void readCoreDescriptionList() {
		colorEntries = new LinkedList<ColorEntry>();
		
		try {
			URL url = URLFactory.url(DSDP_COLOR_PATH + hole.toString() + "-RGB.txt");
			BufferedReader in = new BufferedReader( new InputStreamReader( 
					( url ).openStream() ) );
			String s;
			s = in.readLine();
			while ( ( s = in.readLine() ) != null ) {
				String[] sArr = s.split("\t");
				if ( sArr.length == 4 ) {
					String startDepth = sArr[0];
					String endDepth = sArr[1];
					String[] rgbS = sArr[3].split(",");
					
					try {
						if (rgbS.length != 3) continue;
						float startDepthF = Float.parseFloat(startDepth);
						float endDepthF = Float.parseFloat(endDepth);
						int[] rgb = new int[3];
						for (int i = 0; i < 3; i++)
						{
							rgb[i] = Integer.parseInt(rgbS[i]);
							if (rgb[i] > 255 || rgb[i] < 0) continue;
						};
						
						colorEntries.add( new ColorEntry(startDepthF, endDepthF, rgb ));
					} catch (NumberFormatException ex) {
						ex.printStackTrace();
						continue;
					}
				}
			}
			in.close();
			
			if (colorEntries.size() == 0)
			{
				exists = false;
				return;
			}
			
			exists = true;
			
			Collections.sort(colorEntries, new Comparator<ColorEntry>() {
				public int compare(ColorEntry o1, ColorEntry o2) {
					return o2.startDepth - o1.startDepth > 0 ? -1 : 1;
				}
			});
			
			bottomDepth = 
				colorEntries.get(colorEntries.size() - 1).endDepth;
		} catch (MalformedURLException mue) {
			mue.printStackTrace();
		} catch (IOException ioe) {
			exists = false;
		}
	}
	
	public void setBGC( Color bgColor ) {
		backgroundC = bgColor;
	}
	
	public void setHole(DSDPHole hole) {
		this.hole = hole;
		zScale = 2.;
		cores = hole.cores;
		readCoreDescriptionList();
	}
	public void setZScale( double zScale ) {
		this.zScale = zScale;
	}
	public double getZScale() {
		return zScale;
	}
	public Dimension getPreferredSize() {
		int h = (int)Math.ceil( bottomDepth * zScale ) + 10;
		return new Dimension( CORE_DESCRIPTIONS_DISPLAY_WIDTH, h );
	}
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D)graphics;
		if ( exists ) {
			Dimension d = getPreferredSize();
			g.setColor(Color.black);
			g.drawRect( 0, 0, d.width - 1, d.height - 1 );
			g.setColor(backgroundC);
			g.fillRect( 1, 1, d.width - 2, d.height - 2 );
			g.setFont(new Font("SansSerif", Font.PLAIN, 9));

			g.setColor(Color.black);
			for (ColorEntry entry : colorEntries) {
				int sY = (int)( ( entry.startDepth ) * zScale  );
				int eY = (int)( ( entry.endDepth ) * zScale  );
				if (sY == eY) {
					eY++;
				}
				g.setColor(new Color(entry.rgb[0], entry.rgb[1], entry.rgb[2]));
				g.fillRect(1, sY, d.width - 2, eY - sY);
			}
		}
		else {
			g.drawRect(0, 0, 0, 0);
		}
		prevAge = -1;
	}
	public void drawLineAtAge( int currentAge )	{
		if ( exists ) {
			synchronized (getTreeLock()) {
				Graphics2D g = (Graphics2D)getGraphics();
				Rectangle r = getVisibleRect();
				int x1 = r.x;
				int x2 = r.x+r.width;
				g.setXORMode( Color.cyan );
				if ( prevAge != -1)	{
					g.drawLine(x1, prevAge, x2, prevAge);
				}
				g.drawLine(x1, currentAge, x2, currentAge);
				prevAge = currentAge;
			}
		}
	}
	
	public void accessURL( String inputURLString ) {
		BrowseURL.browseURL(inputURLString);
	}
	
	public static void main( String[] args) {
		
	}
}

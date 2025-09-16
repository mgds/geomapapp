package haxby.map;

import haxby.proj.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.*;
/**
 * The actual drawing of the latitude and longitude label 
 * is accomplished in CylindricalMapBorder.java for Mercator 
 * projection.
 * 
 * @author Andrew K. Melkonian
 * @author Samantha Chan
 *
 */

public class MapBorder extends AbstractBorder {
	public static final int TOP = 0;
	public static final int BOTTOM = 1;
	public static final int LEFT = 2;
	public static final int RIGHT = 3;
	protected boolean[] plotSide = {true, false, true, false };
	protected XMap map;
	protected Insets insets;
	protected Font font;
	protected int lineWidth, tickLength;

	protected ArrayList<String> longitudeList;


//	GMA 1.4.8: TESTING	
//	static double[] mInt = {1, 2, 3, 5, 10, 15, 20, 30,
//				60, 120, 300, 600, 900, 1200, 1800, 2700, 3600 };

	/* These values control the spacing of the tick marks. Beginning at zoom 1
	 * With the last item in the list.
	*/
	static double[] mInt = {0.05, 0.1, 0.25, 0.5, 1, 2, 3, 5, 10, 15, 20, 30,
			60, 120, 300, 600, 900, 1200, 1800, 2700, 3600 };

	// labelInt tickmarks for CylindricalMapBorder.java
	static double[] labelInt = {0.0125, 0.025, 0.0625, 0.2, 0.5, 1,
			1, 1, 1, 1, 1, 1, 2, 3, 5, 10, 15, 20, 30, 60, 
			120, 300, 600, 900, 1200, 1800, 2700, 3600 };
		
	protected MapBorder() {
	}
	public MapBorder(XMap map) {
		this.map = map;
		lineWidth = 3;
		tickLength = 3;	
		font = new Font("Arial",Font.PLAIN,12);
		plotSide = new boolean[] {false, true, false, true };
		format();
	}

	public boolean hasOverlap(){
		if(longitudeList==null){
			return false;
		}

		HashSet<String> longitudeSet = new HashSet<String>();

		for(String s:longitudeList){
			longitudeSet.add(s);
		}

		return !(longitudeList.size() == longitudeSet.size());
		
	}

	public void setLineWidth(int w) {
		if(lineWidth == w)return;
		lineWidth = w;
		format();
		map.revalidate();
	}
	public void setTickLength(int length) {
		if(length == tickLength) return;
		tickLength = length;
		format();
		map.revalidate();
	}
	public Font getFont() { return font; }
	public void setFont(Font font) {
		this.font = font;
		format();
		map.revalidate();
	}
	public void setSide (int side, boolean set) {
		if(plotSide[side] == set)return;
		plotSide[side] = set;
		format();
		map.revalidate();
	}
	public boolean isSideSelected(int side) {
		return plotSide[side];
	}
	void format() {
		insets = new Insets(0,0,0,0);
		FontMetrics fm = map.getFontMetrics(font);
		int h = fm.getHeight();
		insets.top = lineWidth + tickLength;
		if(plotSide[0]) insets.top += h;
		insets.bottom = lineWidth + tickLength;
		if(plotSide[1]) insets.bottom += h;
		int w = fm.stringWidth("88\u00B0N") + 4;
		insets.left = lineWidth + tickLength;
		if(plotSide[2]) insets.left += w;
		insets.right = lineWidth + tickLength;
		if(plotSide[3]) insets.right += w;
	}
	public Insets getBorderInsets(Component c) {
		return (Insets)insets.clone();
	}
	public Insets getBorderInsets(Component c, Insets insets) {
		insets = (Insets)this.insets.clone();
		return insets;
	}
	public boolean isBorderOpaque() { return true; }
	public void paintBorder(Component c, Graphics g,
				int x, int y, int w, int h) {

		longitudeList = new ArrayList<String>();
		Graphics2D g2d = (Graphics2D) g;
		g2d.setColor(Color.white);
		Rectangle rect = getInteriorRectangle(c, x, y, w, h);
		g2d.fillRect(x, y, w, rect.y-y);
		g2d.fillRect(x, y, rect.x-x, h);
		g2d.fillRect(x, rect.y+rect.height, w, y+h-rect.y-rect.height);
		g2d.fillRect(rect.x+rect.width, y, x+w - rect.x - rect.width, h);
		g2d.setColor(Color.black);
		Rectangle rect1 = new Rectangle(rect.x-lineWidth-tickLength, rect.y-lineWidth-tickLength,
				rect.width+2*lineWidth+2*tickLength, rect.height+2*lineWidth+2*tickLength);
		g2d.fillRect(rect1.x, rect1.y, rect1.width, lineWidth);
		g2d.fillRect(rect1.x, rect1.y, lineWidth, rect1.height);
		g2d.fillRect(rect1.x, rect1.y+rect1.height-lineWidth, rect1.width, lineWidth);
		g2d.fillRect(rect1.x+rect1.width-lineWidth, rect1.y, lineWidth, rect1.height);
		g2d.setColor(Color.black);

		Projection proj = map.getProjection();
		double zoom = map.getZoom();
		Point2D ll = new Point2D.Double( (double)(rect.x-insets.left)/zoom, 
				(double)(rect.y+rect.height-insets.top)/zoom);
		Point2D ur = new Point2D.Double( (double)(rect.x+rect.width-insets.left)/zoom, 
				(double)(rect.y-insets.top)/zoom);
		Point2D sw = proj.getRefXY(ll);
		Point2D ne = proj.getRefXY(ur);
		if( ne.getX()<sw.getX() ) {
			ne.setLocation( ne.getX()+360d, ne.getY());
		}

		g2d.setFont(font);
		FontMetrics fm = g2d.getFontMetrics();
		Point2D.Double xy = new Point2D.Double();

		int i = mInt.length - 1;
		int x1 = 0;
		int x2 = 0;
		do {
			x1 = (int)Math.ceil(sw.getX() * 60 / mInt[i]);
			x2 = (int)Math.floor(ne.getX() * 60 / mInt[i]);
			i--;
		} while ( (x2-x1 < 2) && i>=0);
		i++;
		xy.y = (double)y;
		int xDeg;

		// Drawing longitude labels. xDeg is decimal minutes
		for( int xx=x1 ; xx<=x2 ; xx++) {
			xDeg = xx;
			while(xDeg > 180*60/ mInt[i]) xDeg -= 360*60/ mInt[i];
			while(xDeg <= -180*60/ mInt[i]) xDeg += 360*60/ mInt[i];
			xy.x = (double)xx * mInt[i] / 60d;
			int xPos = insets.left + (int)(zoom * proj.getMapXY(xy).getX());
			g2d.drawLine(xPos, rect.y, xPos, rect.y-tickLength);
			g2d.drawLine(xPos, rect.y+rect.height, xPos, rect.y+rect.height+tickLength);
			int deg = (int)Math.floor((double)xDeg * mInt[i] / 60d);
			if(xDeg < 0) deg++;
			int min = (int)Math.abs(Math.rint( (double)(deg*60) - (double)xDeg*mInt[i]));
			if(min == 60) {
				if(xDeg<0)deg--;
				else deg++;
				min = 0;
			}
			if(deg<0) deg = -deg;
			String s = deg +"\u00B0";
			if(xDeg < 0 && deg!=0 ) s += "W";
			else if(xDeg == 0) s = "W"+s+"E";
			else s += "E";
			if(min != 0) s += min + "\u00B4";
			int dx = fm.stringWidth(s);
			if(plotSide[0])g.drawString(s, xPos-dx/2, y+fm.getHeight()-fm.getDescent());
			if(plotSide[1])g.drawString(s, xPos-dx/2, y+h-fm.getDescent());
			longitudeList.add(s);
		}
		i = mInt.length - 1;
		// Draw latitude labels
		int y1 = 0;
		int y2 = 0;
		do {
			y1 = (int)Math.ceil(sw.getY() * 60 / mInt[i]);
			y2 = (int)Math.floor(ne.getY() * 60 / mInt[i]);
			i--;
		} while ( (y2-y1 < 2) && i>=0);
		i++;
		xy.x = (double)x;
		for( int xx=y1 ; xx<=y2 ; xx++) {
			xy.y = (double)xx * mInt[i] / 60d;
			int yPos = insets.top + (int)(zoom * proj.getMapXY(xy).getY());
			g2d.drawLine(rect.x, yPos, rect.x-tickLength, yPos);
			g2d.drawLine(rect.x+rect.width, yPos, rect.x+rect.width+tickLength, yPos);
			int deg = (int)Math.floor((double)xx * mInt[i] / 60d);
			if(xx < 0) deg++;
			int min = (int)Math.abs(Math.rint( (double)(deg*60) - (double)xx*mInt[i]));
			if(min == 60) {
				if(xx<0)deg--;
				else deg++;
				min = 0;
			}
			if(deg < 0) deg = -deg;
			String s = deg +"\u00B0";
			if(xx < 0 && deg!=0 ) s += "S";
			else if(xx != 0) s += "N";
			if(min != 0) {
				int dx0 = lineWidth + tickLength +2;
				int dx = dx0 + fm.stringWidth(s);
				if(plotSide[2])g.drawString(s, rect.x-dx, yPos);
				if(plotSide[3])g.drawString(s, rect.x+rect.width + dx0, yPos);
				s = min + "\u00B4";
				dx0 = lineWidth + tickLength +2;
				dx = dx0 + fm.stringWidth(s);
				if(plotSide[2])g.drawString(s, rect.x-dx, 
						yPos+fm.getHeight()-fm.getDescent());
				if(plotSide[3])g.drawString(s, rect.x+rect.width + dx0,
						yPos+fm.getHeight()-fm.getDescent());
			} else {
				int dx0 = lineWidth + tickLength +2;
				int dx = dx0 + fm.stringWidth(s);
				if(plotSide[2]) g.drawString(s, rect.x-dx, 
					yPos+(fm.getHeight()-fm.getDescent())/2);
				if(plotSide[3])g.drawString(s, rect.x+rect.width + dx0,
					yPos+(fm.getHeight()-fm.getDescent())/2);
			}
		}
	}
}

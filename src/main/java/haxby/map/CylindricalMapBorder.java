package haxby.map;

import haxby.proj.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.*;

/**
 * Draws the map border of the map and displays the longitude
 * and latitude. Tick marks on map border display according
 * to various zooms. At certain zooms the seconds will have a
 * various increased decimal point depending on the growth of zoom.
 * 
 * @author Andrew K. Melkonian
 * @author Samantha Chan
 *
 */
public class CylindricalMapBorder extends MapBorder
				implements Overlay {
	double wrap;
	CylindricalProjection proj;
	int x1, x2, xInt;
	int y1, y2, yInt;
	int nAnotScalerX, nAnotScalerY;

	public CylindricalMapBorder(XMap map) {
		super(map);
		proj = (CylindricalProjection)map.getProjection();
		wrap = Math.rint(360.*(proj.getX(10.)-proj.getX(9.)));
		format();
	}
	public void paintBorder(Component c, Graphics g,
				int x, int y, int w, int h) {

		super.longitudeList = new ArrayList<String>();
		double zoom = map.getZoom();
		//System.out.println("Zoom is: " + zoom);

		Graphics2D g2d = (Graphics2D) g;
		g.setColor(Color.white);
		Rectangle rect = getInteriorRectangle(c, x, y, w, h);
		Dimension interior = new Dimension( w-insets.left-insets.right,
					h-insets.top-insets.bottom );
		g.fillRect(x, y, w, rect.y-y);
		g.fillRect(x, y, rect.x-x, h);
		g.fillRect(x, rect.y+rect.height, w, y+h-rect.y-rect.height);
		g.fillRect(rect.x+rect.width, y, x+w - rect.x - rect.width, h);
		g.setColor(Color.black);
		Rectangle rect1 = new Rectangle(rect.x-lineWidth-tickLength, rect.y-lineWidth-tickLength,
				rect.width+2*lineWidth+2*tickLength, rect.height+2*lineWidth+2*tickLength);
		g.fillRect(rect1.x, rect1.y, rect1.width, lineWidth);
		g.fillRect(rect1.x, rect1.y, lineWidth, rect1.height);
		g.fillRect(rect1.x, rect1.y+rect1.height-lineWidth, rect1.width, lineWidth);
		g.fillRect(rect1.x+rect1.width-lineWidth, rect1.y, lineWidth, rect1.height);
		g.setColor(Color.black);

		Point2D ll = new Point2D.Double( (double)(rect.x-insets.left)/zoom, 
				(double)(rect.y+rect.height-insets.top)/zoom);
		Point2D ur = new Point2D.Double( (double)(rect.x+rect.width-insets.left)/zoom, 
				(double)(rect.y-insets.top)/zoom);
		Point2D sw = proj.getRefXY(ll);
		Point2D ne = proj.getRefXY(ur);
		double west = sw.getX();
		double east = west + rect.width * 360./wrap / zoom;
		if(east>=west+360.)east = west+359.999;

		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		Point2D.Double xy = new Point2D.Double();

		int i = labelInt.length - 1;
		x1 = 0;
		x2 = 0;	

		// At various zooms set XY scalers
		if(zoom >= 4096){
			nAnotScalerX = 200;
			nAnotScalerY = 200;
		}else{
			nAnotScalerX = 100;
			nAnotScalerY = 100;
		}
		if((zoom >= 1024) && (zoom < 4096)){
			nAnotScalerX = 200;
			nAnotScalerY = 100;
		}
		int nAnotX = (int)Math.max( 2, interior.width/nAnotScalerX );
		int nAnotY = (int)Math.max( 2, interior.height/nAnotScalerY );

		// Determines the tick mark interval
		do {
			x1 = (int)Math.ceil(west * 60 / labelInt[i]);	// x1 is the left most label value
			x2 = (int)Math.floor(east * 60 / labelInt[i]);	// x2 is the right most label value
			i--;
		} while ( (x2-x1 < nAnotX) && i>=0);
		i++;
		xInt = i;
		xy.y = (double)y;
		int xDeg;
		double xPos;
		g2d.setStroke( new BasicStroke(3f) );
		g.setColor( Color.black );
		//System.out.println(" x1: " + x1 + " x2: " + x2 + " i: " + i + " xInt: " + xInt + " west: " + west + " east: " + east);
		//System.out.println("Drawing longitude labels");
		//System.out.println("i is : " + i + " LabelInt : " + labelInt[i]);

		// For ticks and labels on the x axis
		for( int xx=x1 ; xx<=x2 ; xx++) {
			xy.x = (double)xx * labelInt[i] / 60d; 			// The degree label value
			xPos = insets.left + (zoom * proj.getX(xy.x)); 	// The label location
			xDeg = xx;										// The sequence number of the label (begins with 0)

			//System.out.println("xy.x: " + xy.x + "xPos: " + xPos + "xDeg: " + xDeg);
			while(xDeg > 180*60/ labelInt[i]) xDeg -= 360*60/ labelInt[i];
			while(xDeg <= -180*60/ labelInt[i]) xDeg += 360*60/ labelInt[i];
			int deg = (int)Math.floor((double)xDeg * labelInt[i] / 60d);
			if(xDeg < 0) deg++;
			int min = (int)Math.abs(Math.rint( (double)(deg*60) - (double)xDeg*labelInt[i]));

//			GMA 1.4.8: Calculate seconds based on double precision minutes
			//System.out.println( "zoom: " + zoom + "\t" + "min: " + min);
			double floatMin = (double)(deg*60) - (double)xDeg*labelInt[i];
			//System.out.println( "floatMin: " + floatMin);
			if ( floatMin < 0 ) {
				floatMin *= -1;
			}
			double sec = (double)(60 * (floatMin % 1));

			//System.out.println( "sec: " + sec);
			if(sec != 0) {
				min = (int)(Math.rint(floatMin - (floatMin % 1)));
			}

			if(min == 60) {
				if(xDeg<0)deg--;
				else deg++;
				min = 0;
			}
			if(deg<0) deg = -deg;
			String s = deg +"\u00B0";
			if(xDeg < 0 ) s += "W ";
			else if(xDeg == 0) s = "W"+s+"E";
			else s += "E";
			if(min != 0) s += min +"\u00B4";

//			GMA 1.4.8: Add seconds if sec is not equal to zero
			if((sec != 0) && (zoom < 131072)) s += formatNum(sec) + "\u00B4" + "\u00B4";
			if((sec != 0) && (zoom >= 131072)&& (zoom < 524288)) s += formatNum1(sec) + "\u00B4" + "\u00B4";
			if((sec != 0) && (zoom >= 524288)) s += formatNum2(sec) + "\u00B4" + "\u00B4";

			int dx = fm.stringWidth(s);
			while(xPos<(double)rect.x) {
				xPos += zoom*wrap;
			}
			while(xPos<(double)(rect.x+rect.width)) {
				int ix = (int)Math.rint(xPos);
				g.drawLine(ix, rect.y, ix, rect.y-tickLength);
				g.drawLine(ix, rect.y+rect.height, ix, rect.y+rect.height+tickLength);
				if(plotSide[0])g.drawString(s, ix-dx/2, y+fm.getHeight()-fm.getDescent());
				if(plotSide[1])g.drawString(s, ix-dx/2, y+h-fm.getDescent());
				xPos += zoom*wrap;
				super.longitudeList.add(s);
			}
		}
		if( i>1 ) {
			i -= 2;
			if( i>0 ) i--;
			int ix1 = (int)Math.ceil(west * 60 / labelInt[i]);
			int ix2 = (int)Math.floor(east * 60 / labelInt[i]);
			g2d.setStroke( new BasicStroke(1f) );
			for( int xx=ix1 ; xx<=ix2 ; xx++) {
				xy.x = (double)xx * labelInt[i] / 60d;
				xPos = insets.left + (zoom * proj.getX(xy.x));
				while(xPos<(double)rect.x) {
					xPos += zoom*wrap;
				}
				while(xPos<(double)(rect.x+rect.width)) {
					int ix = (int)Math.rint(xPos);
					g.drawLine(ix, rect.y, ix, rect.y-tickLength);
					g.drawLine(ix, rect.y+rect.height, ix, rect.y+rect.height+tickLength);
					xPos += zoom*wrap;
				}
			}
		}
		//Gets the last value from labelInt array first
		i = labelInt.length - 1; // i equals 20
		y1 = 0;
		y2 = 0;

		//y1 and y2 determine the tick marks spacing 
		do {
			y1 = (int)Math.ceil(sw.getY() * 60 / labelInt[i]);
			y2 = (int)Math.floor(ne.getY() * 60 / labelInt[i]);
			i--;
		} while ( (y2-y1 < nAnotY) && i>=0);

		i++;
		yInt = i; 	//yInt uses the index into labelInt[] 
		xy.x = (double)x;
		g2d.setStroke( new BasicStroke(3f) );

		// Drawing latitude tick marks and latitude labels
		for( int xx=y1 ; xx<=y2 ; xx++) {
			xy.y = (double)xx * labelInt[i] / 60d;
			int yPos = insets.top + (int)(zoom * proj.getMapXY(xy).getY());
			g.setColor( Color.lightGray );
			g.drawLine(rect.x, yPos, rect.x-tickLength, yPos);
			g.drawLine(rect.x+rect.width, yPos, rect.x+rect.width+tickLength, yPos);
			g.setColor( Color.black );
			int deg = (int)Math.floor((double)xx * labelInt[i] / 60d);
			if(xx < 0) deg++;
			int min = (int)Math.abs(Math.rint( (double)(deg*60) - (double)xx*labelInt[i]));

//			GMA 1.4.8: Calculate seconds based on double precision minutes
			//System.out.println( "zoom: " + zoom + "\t" + "min: " + min);
			double floatMin = (double)(deg*60) - (double)xx*labelInt[i];
			//System.out.println( "floatMin: " + floatMin);
			if ( floatMin < 0 ) {
				floatMin *= -1;
			}
			int sec = (int)(60 * (floatMin % 1));
			//System.out.println( "sec: " + sec);
			if(sec != 0) {
				min = (int)(Math.rint(floatMin - (floatMin % 1)));
			}

			if(min == 60) {
				if(xx<0)deg--;
				else deg++;
				min = 0;
			}
			if(deg < 0) deg = -deg;
			// Writing the degree value and the degree symbol into string s
			String s = deg +"\u00B0";
			if(xx < 0 ) s += "S";
			else if(xx != 0) s += "N";
			// Write the minutes value below the degree
			if(min != 0) {
				int dx0 = lineWidth + tickLength +2;
				int dx = dx0 + fm.stringWidth(s);
				if(plotSide[2])g.drawString(s, rect.x-dx, yPos);
				if(plotSide[3])g.drawString(s, rect.x+rect.width + dx0, yPos);

				// Add the minute value and the minute symbol into string s
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

//			GMA 1.4.8: Add seconds if sec is not equal to zero
			if( sec != 0 ) {
				int dx0 = lineWidth + tickLength +2;
				int dx = dx0 + fm.stringWidth(s);

				if(zoom < 131072) s= formatNum(sec) + "\u00B4" + "\u00B4";
				if(zoom >= 131072) s= formatNum1(sec) + "\u00B4" + "\u00B4";
				if(zoom >= 524288) s = formatNum2(sec) + "\u00B4" + "\u00B4";
				dx0 = lineWidth + tickLength +2;
				dx = dx0 + fm.stringWidth(s);
				if ( min != 0 ) {
					if(plotSide[2]) {
						g.drawString(s, rect.x-dx, yPos+2*(fm.getHeight()-fm.getDescent()));
					}
					if(plotSide[3]) {
						g.drawString(s, rect.x+rect.width + dx0, yPos+2*(fm.getHeight()-fm.getDescent()));
					}
				} else {
					if(plotSide[2]) {
						g.drawString(s, rect.x-dx, yPos+fm.getHeight()-fm.getDescent());
					}
					if(plotSide[3]) {
						g.drawString(s, rect.x+rect.width + dx0, yPos+fm.getHeight()-fm.getDescent());
					}
				}
			}
		}
		if(i>1) {
			i -= 2;
			if( i>0 ) i--;
			int iy1 = (int)Math.ceil(sw.getY() * 60 / labelInt[i]);
			int iy2 = (int)Math.floor(ne.getY() * 60 / labelInt[i]);
			g2d.setStroke( new BasicStroke(1f) );
			for( int xx=iy1 ; xx<=iy2 ; xx++) {
				xy.y = (double)xx * labelInt[i] / 60d;
				int yPos = insets.top + (int)(zoom * proj.getMapXY(xy).getY());
				g.drawLine(rect.x, yPos, rect.x-tickLength, yPos);
				g.drawLine(rect.x+rect.width, yPos, rect.x+rect.width+tickLength, yPos);
			}
		}
	}

	// Formats double into preferred decimal place.
	public String formatNum2(double inValue){
		DecimalFormat twoDec = new DecimalFormat("0.00");
		twoDec.setGroupingUsed(false);
		return twoDec.format(inValue);
		}
	public String formatNum1(double inValue){
		DecimalFormat oneDec = new DecimalFormat("0.0");
		oneDec.setGroupingUsed(false);
		return oneDec.format(inValue);
		}
	public String formatNum(double inValue){
		DecimalFormat noDec = new DecimalFormat("0");
		noDec.setGroupingUsed(false);
		return noDec.format(inValue);
		}

	// Draw tick marks
	public void draw(Graphics2D g) {
		double zoom = map.getZoom();
		double[] wesn = map.getWESN();
		double west = wesn[0];
		double east = wesn[1];
		if( east<west ) east += 360.;
		if(east>=west+360.)east = west+359.999;
		int i = xInt;
		int ix1 = (int)Math.ceil(west * 60 / labelInt[i]);
		int ix2 = (int)Math.floor(east * 60 / labelInt[i]);
		g.setStroke( new BasicStroke(1.5f) );

		g.setColor(Color.black);
		g.setStroke( new BasicStroke(1f/(float)zoom) );
		Line2D.Double line = new Line2D.Double();
		line.y1 = proj.getY( wesn[3] );
		line.y2 = proj.getY( wesn[1] );
		for( int xx=ix1 ; xx<=ix2 ; xx++) {
			double x = (double)xx * labelInt[i] / 60d;
			line.x1 = proj.getX(x);
			line.x2 = line.x1;
			g.draw( line );
//	System.out.println(west +"\t"+ east 
//			+"\t"+ ix1 +"\t"+ ix2
//			+"\t"+ x +"\t"+ line.x2
//			+"\t"+ line.y1 +"\t"+ line.y2);
		}
		int iy1 = (int)Math.ceil(wesn[2] * 60 / labelInt[yInt]);
		int iy2 = (int)Math.floor(wesn[3] * 60 / labelInt[yInt]);
		line.x1 = proj.getX( wesn[0] );
		line.x2 = proj.getX( wesn[1] );
		for( int xx=iy1 ; xx<=iy2 ; xx++) {
			double y = (double)(xx * labelInt[yInt]) / 60d;
			line.y1 = proj.getY(y);
			line.y2 = line.y1;
			g.draw( line );
		}
	}
}

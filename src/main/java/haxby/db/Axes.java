package haxby.db;

import java.awt.*;
import java.awt.geom.*;

public class Axes {
	public final static int LEFT = 1;
	public final static int RIGHT = 2;
	public final static int TOP = 4;
	public final static int BOTTOM = 8;

//	***** GMA 1.6.2: Limit the number of digits displayed so they don't clog the graph
	public final static int MAX_NUM_OF_SIGNIFICANT_DIGITS_PLUS_ONE = 11;
//	***** GMA 1.6.2

	int sides;
	XYPoints xy;
	int dataIndex;
	Font font;
	double[] res = { 2., 2.5, 2. };
	public Axes( XYPoints pts, int dataIndex, int sides ) {
		xy = pts;
		this.dataIndex = dataIndex;
		this.sides = sides;
		font = new Font("Serif", Font.PLAIN, 12 );
	}
	public void setSides( int sides ) {
		this.sides = sides;
	}
	public void setFont( Font font) {
		this.font = font;
	}
	public Insets getInsets() {
		int size = font.getSize();
		size = 5*size/2;
		Insets ins = new Insets(0, 0, 0, 0);
		if( (sides&LEFT) != 0 ) ins.left+=size;
		if( (sides&RIGHT) != 0 ) ins.right+=size;
		if( (sides&TOP) != 0 ) ins.top+=size;
		if( (sides&BOTTOM) != 0 ) ins.bottom+=size;
		return ins;
	}
	public void drawAxes( Graphics2D g,
			Rectangle2D bounds,
			Rectangle rect ) {
		RenderingHints hints = g.getRenderingHints();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		AffineTransform at = g.getTransform();
		Insets ins = getInsets();
		g.setFont( font);
		g.setColor( Color.black );
// X Axis
		if( (sides&BOTTOM) != 0 ) {
			g.translate( rect.x+ins.left, rect.y+rect.height-ins.bottom);
			drawAxis( g, false, xy.getXTitle(dataIndex), 
				bounds.getX(), bounds.getX()+bounds.getWidth(), 
				rect.width-ins.left-ins.right, 
				-(rect.height-ins.top-ins.bottom) );
			g.setTransform( at );
		}
		if( (sides&TOP) != 0 ) {
			g.translate( rect.x+ins.left, rect.y+ins.top);
			drawAxis( g, true, xy.getXTitle(dataIndex), 
				bounds.getX(), bounds.getX()+bounds.getWidth(), 
				rect.width-ins.left-ins.right, 0 );
			g.setTransform( at );
		}
// Y Axis
		if( (sides&LEFT ) != 0 ) {
			g.translate( rect.x+ins.left, rect.y+rect.height-ins.bottom);
			g.rotate(Math.toRadians(-90.));
			drawAxis( g, true, xy.getYTitle(dataIndex), 
				bounds.getY()+bounds.getHeight(), bounds.getY(), 
				rect.height-ins.top-ins.bottom, 
				(rect.width-ins.left-ins.right) );
			g.setTransform( at );
		}
		if( (sides&RIGHT ) != 0 ) {
			g.translate( rect.x+rect.width-ins.right, rect.y+rect.height-ins.bottom);
			g.rotate(Math.toRadians(-90.));
			drawAxis( g, false, xy.getYTitle(dataIndex), 
				bounds.getY()+bounds.getHeight(), bounds.getY(), 
				rect.height-ins.top-ins.bottom, 0 );
			g.setTransform( at );
		}
		g.setRenderingHints( hints );
	}
	public void drawAxis( Graphics2D g, boolean above, String name,
				double x1, double x2, int width, int height ) {
		double resolution = 1.;
		double min, max;
		if( x2>x1 ) {
			min = x1;
			max = x2;
		} else {
			min = x2;
			max = x1;
		}
		int nAnot = width/100;
		if(nAnot<1) nAnot=1;
//	System.out.println( min +"\t"+max +"\t"+ width +"\t"+ nAnot);
		while( Math.floor(max/resolution)-Math.ceil(min/resolution) < nAnot) resolution /= 10.;
		while( Math.floor(max/resolution)-Math.ceil(min/resolution) > nAnot) resolution *= 10.;
		int kres = 0;
		while( Math.floor(max/resolution)-Math.ceil(min/resolution) < nAnot) {
			kres = (kres+2) %3;
			resolution /= res[kres];
		}
		double val;
		double scale = ((double)width)/(x2-x1);
		if( height != 0 ) {
			double res2 = resolution;
			kres = (kres+2) %3;
			res2 /= res[kres];
			kres = (kres+2) %3;
			res2 /= res[kres];
			g.setStroke(new BasicStroke(1f));
			g.setColor( new Color( 225, 225, 225 ));
			val = res2 * Math.ceil(min/res2);
			while( val<=max ) {
				int x = (int) (scale*(val-x1));
				g.drawLine( x, 0, x, height );
				val += res2;
			}
		}
		val = resolution * Math.ceil(min/resolution);

		g.setStroke(new BasicStroke(2f));
		g.setColor( Color.black );
		g.drawLine( 0, 0, width, 0);
		g.setStroke(new BasicStroke(1f));
		int size = font.getSize();
		int y = size+3;
		int x;
		if( above ) y = -5;
		FontMetrics fm = g.getFontMetrics();
		while( val<=max ) {

//			***** GMA 1.6.2: Display digits down to actual interval on graph so that it is clear 
//			what the intervals on the graph are

			String s;
			if (!(val == 0 && above) && val % 1 == 0) {	
				// The !(val == 0 && above) is a little bodge to display 0.0 on the top of the y-axis.
				// The .0 part will get cut off, making it look like an integer 0 is displayed.
				
				//display axes as integers
				s = Integer.toString((int) val);
			} else {
				s = Double.toString(val);

				if ( s.length() > MAX_NUM_OF_SIGNIFICANT_DIGITS_PLUS_ONE ) {
					s = s.substring(0, MAX_NUM_OF_SIGNIFICANT_DIGITS_PLUS_ONE);
				}
				if ( s.indexOf(".") != -1 ) {
					int trailingZeros = 0;
					for ( int j = s.length() - 1; j > -1; j-- ) {
						if ( s.substring(j,j+1).equals("0") ) {
							trailingZeros++;
						}
						else {
							j = -1;
						}
					}
					if ( trailingZeros > 0 ) {
						s = s.substring(0, s.length() - trailingZeros);
					}
					if ( s.substring( s.length()-1, s.length() ).equals(".") ) {
						s += "0";
					}
				}
			}
//			***** GMA 1.6.2

			x = (int) (scale*(val-x1));
			if( height != 0 ) {
				g.setColor( Color.gray );
				g.drawLine( x, 0, x, height);
			}
			x -= fm.stringWidth(s)/2;
			g.setColor( Color.black );
			g.drawString(s, x, y);
			val += resolution;
		}
		x = (width-fm.stringWidth(name)) / 2;
		if(above) y=-(size*5/2-size);
		else y=size*2+5;
		g.drawString(name, x, y);
	}
	
	/*
	 * Return the width of the x-axis, adding enough room so that tick labels are not cut off
	 */
	public double getXAxisWidth(Graphics2D g, Rectangle2D bounds, Rectangle rect) {
		Insets ins = getInsets();
		double x1 = bounds.getX();
		double x2 = bounds.getX()+bounds.getWidth();
		int width = rect.width-ins.left-ins.right;

		double resolution = 1.;
		double min, max;
		if( x2>x1 ) {
			min = x1;
			max = x2;
		} else {
			min = x2;
			max = x1;
		}
		int nAnot = width/100;
		if(nAnot<1) nAnot=1;

		while( Math.floor(max/resolution)-Math.ceil(min/resolution) < nAnot) resolution /= 10.;
		while( Math.floor(max/resolution)-Math.ceil(min/resolution) > nAnot) resolution *= 10.;
		int kres = 0;
		while( Math.floor(max/resolution)-Math.ceil(min/resolution) < nAnot) {
			kres = (kres+2) %3;
			resolution /= res[kres];
		}
		double scale = ((double)width)/(x2-x1);
		double val = resolution * Math.ceil(min/resolution);
		FontMetrics fm = g.getFontMetrics();
		while( val<=max ) val += resolution;

//		***** GMA 1.6.2: Display digits down to actual interval on graph so that it is clear 
//		what the intervals on the graph are
		String s = Double.toString(val);
		if ( s.length() > MAX_NUM_OF_SIGNIFICANT_DIGITS_PLUS_ONE ) {
			s = s.substring(0, MAX_NUM_OF_SIGNIFICANT_DIGITS_PLUS_ONE);
		}
		if ( s.indexOf(".") != -1 ) {
			int trailingZeros = 0;
			for ( int j = s.length() - 1; j > -1; j-- ) {
				if ( s.substring(j,j+1).equals("0") ) {
					trailingZeros++;
				}
				else {
					j = -1;
				}
			}
			if ( trailingZeros > 0 ) {
				s = s.substring(0, s.length() - trailingZeros);
			}
			if ( s.substring( s.length()-1, s.length() ).equals(".") ) {
				s += "0";
			}
		}			
		double sw = fm.stringWidth(s);
		double xAxisWidth = (val - resolution + sw/(2. * scale));
		return Math.max(max-min,  xAxisWidth);
	}
}

package haxby.map;

import haxby.proj.Projection;
import haxby.util.XYZ;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class MapScale implements MapInset, 
				MouseListener,
				MouseMotionListener {
	int xOffset, yOffset;
	int length;
	boolean vertical;
	XMap map;
	public MapScale( XMap map ) {
		this.map = map;
		reset();
	}
	public void reset() {
		vertical = false;
		Rectangle r = map.getVisibleRect();
		int h = r.height;
		MapBorder border = map.getMapBorder();
		if( border!=null ) {
			Insets ins = border.getBorderInsets(map);
			h -= ins.top+ins.bottom;
		}
		xOffset = 50;
		yOffset = h - 50;
		length = 200;
	}
	public void draw( Graphics2D g, int w, int h ) {
		int x = xOffset;
		int y = yOffset;
		Line2D.Double lineMinor, lineMajor;
//		GMA 1.4.8: TESTING
		boolean labeled = false;

		if( x+length > w ) x = w - length;
		if( x<0 ) x=0;
		if( y>h-18 ) y=h-18;
		xOffset = x;
		yOffset = y;

		Rectangle r = map.getVisibleRect();
		MapBorder border = map.getMapBorder();
		if( border!=null ) {
			Insets ins = border.getBorderInsets(map);
			r.x -= ins.left;
			r.y -= ins.top;
		}
		Projection proj = map.getProjection();
		Point pt = new Point( r.x+x, r.y+y );
		Point2D p = map.getScaledPoint( pt );
		XYZ xyz1 = XYZ.LonLat_to_XYZ( proj.getRefXY( p ) );
		pt.x += length;
		p = map.getScaledPoint( pt );
		XYZ xyz2 = XYZ.LonLat_to_XYZ( proj.getRefXY( p ) );
		double distance = Math.acos( xyz1.dot( xyz2 ) ) * 6370.997;
		
		double[] dx = new double[] {2., 2.5, 2.};
		double div = .1;
		int indx = 0;
		while( div*length/distance < 4. ) {
			div *= dx[indx];
			indx = (indx+1)%3;
		}
		double div1 = div;
		for(int k=0 ; k<2 ; k++) {
			div1 *= dx[indx];
			indx = (indx+1)%3;
		}
		int idx=5;
		if( indx==2 ) idx=4;
		AffineTransform at0 = g.getTransform();
		g.translate( x, y);
		g.setFont( new Font( "Helvetica", Font.PLAIN, 12 ) );
		FontMetrics fm = g.getFontMetrics();
		RenderingHints hints = g.getRenderingHints();
		g.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
		g.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON );
		g.setColor( Color.black );

	//	g.drawLine( 0, 0, length, 0 );

		double xx = 0.;
		

		double dxx = div*length/distance;

//		GMA 1.4.8: TESTING
		double test = 0;
//		if ( ( distance / div ) < 2 ) {
//			System.out.println( "distance/length: " + ( distance / length ) );
//			dxx = div*length/(4*distance);
//		}
		//System.out.println( "distance: " + distance );
		if ( distance <= 0.05 ) {
			div1 = 0.005;
		}
		else if ( distance <= 0.1 ) {
			div1 = 0.01;
		}
		else if ( distance <= 0.3 ) {
			div1 = 0.02;
		}
		else if ( distance <= 0.5 ) {
			div1 = 0.1;
		}
		g.setStroke( new BasicStroke(0.9f) );	// Set Stroke
		lineMinor = new Line2D.Double(0.,0.,0.,-4.);	// Minor Line
		
//		System.out.println( "div1: " + div1 );
//		System.out.println( "distance/div1: " + ( distance / div1 ) );
//		if ( distance <= 0.5 ) {
//			dxx /= 4;
//		}
		
		// Draw minor tick marks
		while( xx<length ) {
			g.draw( lineMinor );	
	//		System.out.println( "dxx: " + dxx + "\t xx: " + xx + "\t length: " + length );		
			xx += dxx;
			lineMinor.x1 = xx;
			lineMinor.x2 = xx;
		}
		
		xx = 0.;
		dxx = div1*length/distance;
		g.setStroke( new BasicStroke(1.3f) );			// Set Stroke
		lineMajor = new Line2D.Double(0.,2.,0.,-6.);	// Major Line
		double dist = 0.;
		java.text.NumberFormat fmt = java.text.NumberFormat.getInstance();
		
//		GMA 1.4.8: Cut off distance label at two decimal places
		java.text.NumberFormat fmt2 = java.text.NumberFormat.getInstance();
		fmt2.setMaximumFractionDigits(2);
		
		int ix=0;
		do {
//			GMA 1.4.8: TESTING
//			System.out.println( "zoom: " + map.zoom + "\t xx: " + xx + "\t length: " + length + "\t ix: " + ix + "\t idx: " + idx );
			
			if( ix%idx==0 ) {
				String s = fmt.format( dist );
				int xPlot = (int)xx - fm.stringWidth(s)/2; 
				if( xx+idx*dxx >= length ) {
					// System.out.println("s " + s + " xx " + xx + "idx " + idx + " dxx " + dxx);
					// GMA 1.4.8: If label is drawn, set labeled to true
					if ( s.compareTo("0") != 0 ) {
						labeled = true;
					}	
					s += " km";
				}
				
				Color textC = Color.BLACK;
				Color bgColor = new Color(255, 255, 255, 100);
				Rectangle2D rect = fm.getStringBounds(s, g);
				if(s.contentEquals("0")) {					
					 g.setColor(bgColor);
					 g.fillRect(xPlot - 5, -6, length + 26, 25);
					 g.setColor(textC);
					 g.drawString( s, xPlot, 15 );
					 g.setStroke( new BasicStroke(1.9f) );	// Set Stroke
					 g.draw( lineMajor ); // At zero
				} else {
					if(xPlot >= 162) {
						g.setColor(bgColor);
						g.fillRect(length + 18, -6, 25, 25);
						g.setColor(textC);
						g.drawString( s, xPlot, 15 );
					} else {
						g.drawString( s, xPlot, 15 );
					}

						g.setStroke( new BasicStroke(1.9f) );	// Set Stroke
						g.draw( lineMajor ); // At number values
					
					//System.out.println("s " + s + " xplot " + xPlot);
				}
			} else {
				g.setStroke( new BasicStroke(1.3f) );	// Set Stroke
				g.draw( lineMajor );
			}
			ix++;
			xx += dxx;
			lineMajor.x1 = xx;
			lineMajor.x2 = xx;
			dist += div1;
		} while( xx<length );
		
//		GMA 1.4.8: Print distance label if there is none already
//		System.out.println( "dist: " + ( dist - div1 ) );
		if ( !labeled && ( ( dist - div1 ) != 0 ) ) {
			String s = fmt.format(dist - div1) + " km";
			g.drawString( s, ((int)(xx-dxx) - fm.stringWidth(s)/2), 14 );
		}
		
		g.setTransform( at0 );
		g.setRenderingHints( hints );
	}
	boolean selected = false;
	Point lastPoint = null;
	public void mouseEntered( MouseEvent evt ) {
	}
	public void mouseExited( MouseEvent evt ) {
	}
	public void mousePressed( MouseEvent evt ) {
		if( evt.isControlDown() ) {
			selected = false;
			return;
		}
		selected = false;
		Rectangle r = map.getVisibleRect();
		MapBorder border = map.getMapBorder();
		if( border!=null ) {
			Insets ins = border.getBorderInsets(map);
			r.x += ins.left;
			r.y += ins.top;
		}
		r.x += xOffset;
		r.y += yOffset;

//		***** GMA 1.5.4: TESTING
		r.y -= 16;
		r.height = 24;
		r.x -= 10;

		r.width = length;		
		if( r.contains( evt.getPoint() ) ) {
			selected = true;
			lastPoint = evt.getPoint();
		}
	}
	public void mouseReleased( MouseEvent evt ) {
		if( evt.isControlDown() ) {
			selected = false;
			return;
		}
		if( selected ) {
			selected = false;
			map.repaint();
		}
	}
	public void mouseClicked( MouseEvent evt ) {
	}
	public void mouseMoved( MouseEvent evt ) {
	}
	public void mouseDragged( MouseEvent evt ) {
		if( evt.isControlDown() ) {
			selected = false;
			return;
		}
		if( selected ) {
			Point p = evt.getPoint();
			xOffset += p.x - lastPoint.x;
			yOffset += p.y - lastPoint.y;
			if(xOffset<0) xOffset=0;
			if(yOffset<0) yOffset=0;
			lastPoint = p;
		}
	}
}

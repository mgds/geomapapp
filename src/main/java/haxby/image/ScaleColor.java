package haxby.image;

import haxby.grid.*;
import haxby.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class ScaleColor extends JComponent {
	GridImager imager;
	XGrid_Z grid;
	float[] zRange;
	Histogram zHist;
	int side;
	int lastX;
	int minX, maxX, middle;
	public ScaleColor( GridImager imager, Histogram hist ) {
		this.imager = imager;
		setHist( hist );
	}
	public void setHist( Histogram hist ) {
		zHist = hist;
		zRange = imager.getRange();
		enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
		side = 0;
		lastX = -1;
		minX = maxX = middle = 0;
		if( getParent() != null)repaint();
	}
	void drawLine() {
		synchronized (getTreeLock()) {
			Graphics2D g = (Graphics2D)getGraphics();
			Dimension dim = getSize();
			g.setXORMode( Color.white );
			g.drawLine(lastX,0,lastX,dim.height);
		}
	}
	public Dimension getMinimumSize() {
		return new Dimension( 100, 40 );
	}
	int nearbyTest( int x ) {
		if( x- minX < 3 && x-minX > -3 ||
				x- maxX < 3 && x-maxX > -3 ) {
			return (x- minX < 3) ? -1 : 1;
		}
		if(side!=0)setCursor( Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR) );
		return 0;
	}
	public void processMouseEvent( MouseEvent evt ) {
	//	super.processMouseEvent( evt );
		if( side==0 ) {
			if( evt.getID()==evt.MOUSE_ENTERED) {
				requestFocus();
				int x = evt.getX();
				side = nearbyTest(x);
				if(side == 0) return;
				setCursor( Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR) );
			}
			return;
		}
		if( evt.getID()==evt.MOUSE_PRESSED) {
			int x = evt.getX();
			lastX=x;
			drawLine();
		} else if( evt.getID()==evt.MOUSE_EXITED) {
			drawLine();
			side = 0;
			setCursor( Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR) );
			lastX = -1;
		} else if( evt.getID()==evt.MOUSE_RELEASED) {
			float[] oldRange = new float[] { zRange[0], zRange[1] };
			if(side==-1) {
				zRange[0] = zRange[0] + 
					(zRange[1]-zRange[0]) * (float)(lastX-minX)
					/ (float)( maxX-minX );
			} else {
				zRange[1] = zRange[0] + 
					(zRange[1]-zRange[0]) * (float)(lastX-minX)
					/ (float)( maxX-minX );
			}
			imager.setRange( zRange[0], zRange[1] );
			repaint();
		}
	}
	public void processMouseMotionEvent( MouseEvent evt ) {
	//	super.processMouseMotionEvent( evt );
		if( evt.getID()==evt.MOUSE_MOVED ) {
			int x = evt.getX();
			side = nearbyTest(x);
			if(side == 0) return;
			setCursor( Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR) );
		} else if( evt.getID()==evt.MOUSE_DRAGGED ) {
			if( side==0 ) return;
			int x=evt.getX();
			if(side==1&&x-minX<10) return;
			if(side==-1&&maxX-x<10) return;
			drawLine();
			lastX = x;
			drawLine();
		}
	}
	public void paint( Graphics g ) {
		Graphics2D g2 = (Graphics2D)g;
		Dimension dim = getSize();
		g.setColor( Color.white );
		g.fillRect( 0, 0, dim.width, dim.height );
		middle = dim.width / 2;
		minX = middle - dim.width/6;
		maxX = middle + dim.width/6;
		float z0 = 2f*zRange[0] - zRange[1];
		float dz = 3f * ( zRange[1] - zRange[0] ) / (float)dim.width;
		float scale = ((float)dim.height-15) / (float)zHist.getMaxCounts();
		float z;
		GeneralPath path = new GeneralPath();
		path.moveTo( 0f, (float)dim.width);
		XYZ grad = new XYZ(0.,0.,1.);
		for( int i=0 ; i<dim.width ; i++ ) {
			z = z0 + (float)i * dz;
		//	float[] col = imager.getColor( z );
		//	Color rgb = new Color( col[0], col[1], col[2] );
			int y =  dim.height-15 - 
					(int)(scale* (float)zHist.getCounts(z));
			Rectangle  r = new Rectangle( i, y, 1, dim.height-15-y);
		//	g.setColor( rgb );
			g.setColor( new Color(imager.getRGB( z, .6f )) );
			g2.fill( r );
			path.lineTo( (float)i, (float)y);
		}
		g.setColor( Color.black );
		g2.draw( path );
		String val = "" + (int)zRange[0];
		Rectangle2D bounds = g2.getFont().getStringBounds( val, g2.getFontRenderContext() );
		int x = minX - (int) (bounds.getWidth()/2.);
		g2.drawString( val, x, 12 );
		val = "" + (int)zRange[1];
		bounds = g2.getFont().getStringBounds( val, g2.getFontRenderContext() );
		x = maxX - (int) (bounds.getWidth()/2.);
		g2.drawString( val, x, 12 );
		g.drawLine( 0, dim.height-15, dim.width, dim.height-15);
		g.setColor( new Color( 0,0,0,100) );
		g.drawLine( minX, 0, minX, dim.height-15);
		g.drawLine( maxX, 0, maxX, dim.height-15);
	}
}

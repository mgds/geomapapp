package org.geomapapp.image;

import org.geomapapp.util.*;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import java.beans.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ColorHistogram extends JComponent {
	private static final long serialVersionUID = 5691425386877293974L;
	Palette palette;
	boolean flip=false;
	Histogram zHist;
	int side;
	int lastX;
	int minX, maxX, middle;
	int[] tabs;
	float[] zRange;
	
	ArrayList<Float> colorZAL;
	ColorScaleTool cst;

	public ColorHistogram() {
		MouseInputAdapter mouse = new MouseInputAdapter() {
			public void mousePressed(MouseEvent evt) {
				initDrag(offsetPt(evt.getPoint()));
			}
			public void mouseDragged(MouseEvent evt) {
				drag(offsetPt(evt.getPoint()));
			}
			public void mouseReleased(MouseEvent evt) {
				apply();
			}
			public void mouseMoved(MouseEvent evt) {
				testX(offsetPt(evt.getPoint()));
			}
			public void mouseEntered(MouseEvent evt) {
				mouseMoved(evt);
			}
		};
		addMouseListener( mouse);
		addMouseMotionListener( mouse);
	}

	public void setColorScaleTool(ColorScaleTool t){
		cst = t;
	}
	public void setPalette( Palette pal ) {
		palette = pal;
		if( getParent() != null)repaint();
	}
	public Palette getPalette() {
		return palette;
	}
	public void setHist( Histogram hist ) {
		zHist = hist;
		side = 0;
		lastX = -1;
		minX = maxX = middle = 0;
		if( getParent() != null)repaint();
	}
	Point offsetPt(Point pt) {
		Insets ins = getInsets();
		pt.x -= ins.left;
		pt.y -= ins.top;
		return pt;
	}
	Dimension getRealSize() {
		Dimension dim = getSize();
		Insets ins = getInsets();
		dim.width -= ins.left + ins.right;
		dim.height -= ins.top + ins.bottom;
		return dim;
	}
	int nearbyTest( Point pt) {
		int x = pt.x;
		Dimension dim = getRealSize();
		if( pt.y < dim.height-15) {
			if( x- minX < 3 && x-minX > -3 ||
					x- maxX < 3 && x-maxX > -3 ) {
				return (x- minX < 3) ? -1 : 1;
			}
		} else if(tabs!=null) {
			for( int k=0 ; k<tabs.length ; k++) {
				if( x-tabs[k]<2 && x-tabs[k]>-2 ) return 10+k;
			}
		}

		if(side!=0)setCursor( Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR) );
		return 0;
	}
	void testX(Point pt) {
		requestFocus();
		side = nearbyTest(pt);
		if(side == 0) return;
		setCursor( Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR) );
	}
	void drag(Point pt) {
		int x = pt.x;
		if( side==0 ) {
			initDrag(pt);
			return;
		}
		if(side==1&&x-minX<10) return;
		if(side==-1&&maxX-x<10) return;

		if( side>=10 ) {
			int k = side-10;
			if( k!=tabs.length-1 && tabs[k+1]-x<3 )return;
			if( k!=0 && x-tabs[k-1]<3 )return;
		}

		drawLine();
		lastX = x;
		drawLine();
	}
	void initDrag(Point pt) {
		int x = pt.x;
		testX(pt);
		if( side==0 )return;
		drawLine();
		lastX=x;
		drawLine();
	}
	void apply() {
		if( palette==null || zHist==null )return;
		if(side==0) return;
		zRange = palette.getRange();
		float[] oldRange = new float[] { zRange[0], zRange[1] };
		if( side>=10 ) {
			float z = zRange[0] +
				(zRange[1]-zRange[0]) * (float)(lastX-minX)
				/ (float)( maxX-minX );
			palette.setScaledZ( side-10, z);
		} else if(side==-1) {
			zRange[0] = zRange[0] +
				(zRange[1]-zRange[0]) * (float)(lastX-minX)
				/ (float)( maxX-minX );
			palette.setRange( zRange[0], zRange[1] );
		} else {
			zRange[1] = zRange[0] +
				(zRange[1]-zRange[0]) * (float)(lastX-minX)
				/ (float)( maxX-minX );
			palette.setRange( zRange[0], zRange[1] );
		}
		lastX = -1;
		repaint();
		firePropertyChange("RANGE_CHANGED", oldRange, zRange);
	}
	void drawLine() {
		if( lastX<0 || side==0) return;
		synchronized (getTreeLock()) {
			Graphics2D g = (Graphics2D)getGraphics();
			Dimension dim = getSize();
			g.setXORMode( Color.white );
			g.drawLine(lastX,0,lastX,dim.height);
		}
	}
	void flip(){
		flip=!flip;
		palette.flipRGB();
	}

	public Dimension getMinimumSize() {
		return new Dimension( 100, 40 );
	}
	public Dimension getPreferredSize() {
		return new Dimension( 300, 100 );
	}
	
	@Override
	//override repaint to also include legend, if present
	public void repaint() {
		if (cst != null && cst.leg != null) {
			cst.leg.repaint();
			cst.leg.getTopLevelAncestor().repaint();
		}
		super.repaint();
	}
	public void paintComponent( Graphics g ) {
		if( palette==null || zHist==null ) return;
		float[] zRange = palette.getRange();
		Graphics2D g2 = (Graphics2D)g;
		Dimension dim = getSize();
		Insets ins = getInsets();
		g.translate( ins.left, ins.top);
		dim.width -= ins.left + ins.right;
		dim.height -= ins.top + ins.bottom;
		g.setColor( Color.white );
		g.fillRect( 0, 0, dim.width, dim.height );
		middle = dim.width / 2;
		minX = middle - dim.width/4;
		maxX = middle + dim.width/4;
		float z0 = .5f*(zRange[0]+zRange[1]);
		float dz = (zRange[1]-zRange[0] );

		tabs = new int[palette.getControlPoints().length];
		for( int k=0 ; k<tabs.length ; k++) {
			float z = palette.getScaledZ(k);
			tabs[k] = (int)(minX + middle*(z-zRange[0])/dz);
		}

		z0 -= dz;
		dz = 2f * dz / (float)dim.width;
		double[] range = new double[] {z0, z0+dz};
		float scale = ((float)dim.height-20) / (float)zHist.getMaxCounts();
		float z;
		// Draws histogram outline on x
		GeneralPath path = new GeneralPath();
		path.moveTo( 0f, (float)dim.width);
		colorZAL = new ArrayList<Float>();
		for( int i=0 ; i<dim.width ; i++ ) {
			z = z0 + (float)i * dz;
			//System.out.println("z added");
			colorZAL.add(z);
			int y =  dim.height-20 - 
					(int)(scale* (float)zHist.getCounts(z));
			Rectangle  r = new Rectangle( i, y, 1, dim.height-15-y);
			g.setColor( new Color(palette.getRGB( z )) );
			g2.fill( r );
			path.lineTo( (float)i, (float)y);
		}
		g.setColor( Color.black );
		g2.draw( path );
		g2.translate(0, dim.height-15);
		double dx = .5*(double)(zRange[1]-zRange[0]);
		//Draws x-axis scale bar for histogram
		Axes.drawAxis( g2, false, null, -dx+(double)zRange[0],
				dx+(double)zRange[1], dim.width, 4);

		// Draws diamond marker on histogram
		path = new GeneralPath();
		path.moveTo(4f, 0f);
		path.lineTo(0f, 4f);
		path.lineTo(-4f, 0f);
		path.lineTo(0f, -4f);
		path.closePath();
		g.setColor( Color.black );
		for( int k=0 ; k<tabs.length ; k++) {
			g2.translate(tabs[k], 0);
			g2.draw(path);
			g2.translate(-tabs[k], 0);
		}
/*
		String val = "" + (int)zRange[0];
		Rectangle2D bounds = g2.getFont().getStringBounds( val, g2.getFontRenderContext() );
		int x = minX - (int) (bounds.getWidth()/2.);
		g2.drawString( val, x, dim.height-3 );
		val = "" + (int)zRange[1];
		bounds = g2.getFont().getStringBounds( val, g2.getFontRenderContext() );
		x = maxX - (int) (bounds.getWidth()/2.);
		g2.drawString( val, x, dim.height-3 );
		g.drawLine( 0, dim.height-15, dim.width, dim.height-15);
*/
		g2.translate(0, -(dim.height-15));
		g.setColor( new Color( 0,0,0,100) );
		g.drawLine( minX, 0, minX, dim.height-15);
		g.drawLine( maxX, 0, maxX, dim.height-15);
		
		//display palette name
		g.setColor( Color.BLACK );
		g2.drawString(palette.name, 10, 15);
	}

	public ArrayList<Float> returnColorZ(){
		return colorZAL;
	}
	
	public float[] getRange() {
		return palette.getRange();
	}
	
	public void setRange(float[] range) {
		zRange = range;
		palette.setRange( zRange[0], zRange[1] );
		repaint();
	}
	
	public int[] getTabs() {
		return tabs;
	}
	
	public void setTabs(int[] tabs) {
		this.tabs = tabs;
	}
}

package org.geomapapp.util;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.event.MouseInputAdapter;

import org.geomapapp.image.ColorScaleTool;

public class ScaleHistogram extends JComponent {
		boolean flip=false;
		Histogram zHist;
		int side;
		int lastX;
		int minX, maxX, middle;
		float[] range;
		SymbolScaleTool sst;
		ArrayList<Float> scaleZAL;

		public ScaleHistogram() {
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
		public void setSymbolScaleTool(SymbolScaleTool t){
			sst = t;
		}
		public void setRange( double[] range) {
			setRange( new float[] {(float) range[0], (float) range[1]});
		}
		public void setRange( float[] range ) {
			this.range = range;
			if (range[1] == range[0])
				this.range[1]++;
			if( getParent() != null) { repaint(); }
		}
		public void setHist( Histogram hist ) {
			zHist = hist;
			side = 0;
			lastX = -1;
			minX=maxX=middle=0;
			if( getParent() != null)
				repaint();
		}
		public boolean isReady(){
			if (middle==0) {
				Dimension dim = getSize();
				middle = dim.width / 2;
				minX = middle - dim.width/4;
				maxX = middle + dim.width/4;
			}
			return middle>0;
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
			if( range==null || zHist==null )return;
			if(side==0) return;
			float[] oldRange = new float[] { range[0], range[1] };
			if(side==-1) {
				range[0] = range[0] +
					(range[1]-range[0]) * (float)(lastX-minX)
					/ (float)( maxX-minX );
//				palette.setRange( zRange[0], zRange[1] );
			} else {
				range[1] = range[0] +
					(range[1]-range[0]) * (float)(lastX-minX)
					/ (float)( maxX-minX );
				//palette.setRange( zRange[0], zRange[1] );
			}
			if (range[0] == range[1])
				range[1]++;

			lastX = -1;
			repaint();
			firePropertyChange("RANGE_CHANGED", oldRange, range);
		}
		void drawLine() {
			if( lastX<0 || side==0) return;
			synchronized (getTreeLock()) {
				Graphics2D g = (Graphics2D)getGraphics();
				Dimension dim = getSize();
				g.setXORMode( Color.YELLOW );
				g.drawLine(lastX,0,lastX,dim.height);
			}
		}
		void flip(){
			flip=!flip;
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
			if (sst != null && sst.leg != null) {
				sst.leg.repaint();
				sst.leg.getTopLevelAncestor().repaint();
			}
			super.repaint();
		}
		public float getRatio(float z){
			if (Float.isNaN(z)) return Float.NaN;
			else if (z<=range[0]) return flip ? 2 : .35f;
			else if (z>=range[1]) return flip ? .35f : 2;
			Dimension dim = getSize();
			float dz = (range[1]-range[0] );

			z = (z - range[0]) / dz;
			if (flip) z = 1 - z;
			
			return z * 1.65f + .35f;
		}

		public float[] getRange() {
			return range;
		}
		public void paintComponent( Graphics g ) {
			if( range==null || zHist==null ) return;
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
			float z0 = .5f*(range[0]+range[1]);
			float dz = (range[1]-range[0] );

//			tabs = new int[palette.getControlPoints().length];
//			for( int k=0 ; k<tabs.length ; k++) {
//				float z = palette.getScaledZ(k);
//				tabs[k] = (int)(minX + middle*(z-zRange[0])/dz);
//			}

			z0 -= dz;
			dz = 2f * dz / (float)dim.width;
			//double[] range = new double[] {z0, z0+dz};
			float scale = ((float)dim.height-20) / (float)zHist.getMaxCounts();
	//System.out.println("scale " + scale);
			float z;
			GeneralPath path = new GeneralPath();
			path.moveTo( 0f, (float)dim.width);
			g.setColor(Color.GRAY);
			scaleZAL = new ArrayList<Float>();
			for( int i=0 ; i<dim.width ; i++ ) {
				z = z0 + (float)i * dz;
				scaleZAL.add(z);
				int y =  dim.height-20 - 
						(int)(scale* (float)zHist.getCounts(z));
				Rectangle  r = new Rectangle( i, y, 1, dim.height-15-y);
				//g.setColor( new Color(palette.getRGB( z )) );
				g2.fill( r );
				path.lineTo( (float)i, (float)y);
			}
			g.setColor( Color.black );
			g2.draw( path );
			g2.translate(0, dim.height-15);
			double dx = .5*(double)(range[1]-range[0]);
			Axes.drawAxis( g2, false, null, -dx+(double)range[0],
					dx+(double)range[1], dim.width, 4);

//			path = new GeneralPath();
//			path.moveTo(4f, 0f);
//			path.lineTo(0f, 4f);
//			path.lineTo(-4f, 0f);
//			path.lineTo(0f, -4f);
//			path.closePath();
//			g.setColor( Color.black );
//			for( int k=0 ; k<tabs.length ; k++) {
//				g2.translate(tabs[k], 0);
//				g2.draw(path);
//				g2.translate(-tabs[k], 0);
//			}

			g2.translate(0, -(dim.height-15));
			g.setColor( Color.BLUE );
			g.drawLine( minX, 0, minX, dim.height-15);
			g.drawLine( maxX, 0, maxX, dim.height-15);
		}
		
		public ArrayList<Float> getScaleZAL(){
			return scaleZAL;
		}
	}
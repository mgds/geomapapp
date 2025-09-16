package haxby.grid;

import haxby.proj.*;
import java.io.*;
import java.awt.*;
import java.awt.geom.*;

public class Mask implements Serializable {
	Masker gridder;
	Projection proj;
	byte[] grid;
	int x0, y0, width, height;
	Mask() {
	}
	public Mask(int x0, int y0, int width, int height, URLMasker gridder) {
		proj = new ScaledProjection( gridder.getProjection(),
				1., (double)x0, (double)y0);
		this.x0 = x0;
		this.y0 = y0;
		this.width = width;
		this.height = height;
		this.gridder = null;
		grid = new byte[height*width];
		int k=0;
		
		int tileX1 = x0/320;
		if( x0<0 ) tileX1--;
		int tileX2 = (x0+width-1)/320;
		if( x0+width-1<0 ) tileX2--;
		int tileY1 = y0/320;
		if( y0<0 ) tileY1--;
		int tileY2 = (y0+height-1)/320;
		if( y0+height-1<0 ) tileY2--;
		for( int tileX=tileX1 ; tileX<=tileX2 ; tileX++ ) {
			int x1 = tileX*320;
			int x2 = x1+319;
			if( x1<x0 ) x1=x0;
			if( x2>x0+width-1 ) x2=x0+width-1;
			int xOffset = x1-x0;
			for( int tileY=tileY1 ; tileY<=tileY2 ; tileY++ ) {
				int y1 = tileY*320;
				int y2 = y1+319;
				if( y1<y0 ) y1=y0;
				if( y2>y0+height-1 ) y2=y0+height-1;
				int offset = xOffset+ width*(y1-y0);
				for( int y=y1 ; y<=y2 ; y++, offset+=width ) {
					k = offset;
					for( int x=x1 ; x<=x2 ; x++,k++ ) {
						grid[k] = (byte)gridder.valueAt(x, y);
					}
				}
			}
		}
/*
		float h, wt;
		for( int y=y0 ; y<y0+height ; y++) {
			for(int x=x0 ; x<x0+width ; x++, k++) {
				grid[k] = (byte)gridder.valueAt(x, y);
			}
		}
*/
		
	}
	public Mask(int x0, int y0, int width, int height, Masker gridder) {
		proj = new ScaledProjection( gridder.getProjection(),
				1., (double)x0, (double)y0);
		this.x0 = x0;
		this.y0 = y0;
		this.width = width;
		this.height = height;
		this.gridder = gridder;
		grid = new byte[height*width];
		int k=0;
		float h, wt;
		for( int y=y0 ; y<y0+height ; y++) {
			for(int x=x0 ; x<x0+width ; x++, k++) {
				grid[k] = (byte)gridder.valueAt(x, y);
			}
		}
	}
	public Projection getProjection() {
		return proj;
	}
	public byte[] getGrid() {
		return grid;
	}
	public int[] getBounds() {
		return new int[] {x0, y0, x0+width, y0+height};
	}
	public Dimension getSize() {
		return new Dimension(width, height);
	}
	public int valueAtRef( double refX, double refY) {
		Point2D.Double p = (Point2D.Double) proj.getMapXY( new Point2D.Double(refX, refY) );
		return valueAt(p.x, p.y);
	}
	public int valueAt( int ix, int iy) {
		if(ix<0 || ix>width-2 )return -1;
		if(iy<0 || iy>height-2 )return -1;
		int i = ix + width*iy;
		return (int)grid[i];
	}
	public int valueAt( double x, double y) {
		return valueAt( (int)Math.rint(x), (int)Math.rint(y) );
	}
	public Mask decimate() {
		int w = (width-1)/2;
		int h = (height-1)/2;
		Mask gz = new Mask();
		gz.width = w;
		gz.height = h;
		gz.x0 = x0+1;
		gz.y0 = y0+1;
		gz.gridder = gridder;
		gz.proj = new ScaledProjection( gridder.getProjection(),
				2., (double)x0, (double)y0);
		byte[] g = new byte[w*h];
		int k=0;
		for( int y=1 ; y<height-1 ; y+=2) {
			for(int x=1 ; x<width-1 ; x+=2, k++) {
				int i = y*width+x;
				g[k] = grid[i];
				if( grid[i]!=0 ) continue;
				for( int j=-1 ; j<=1 ; j+=2) {
					if( grid[i+j]!=0 ) {
						g[k] = grid[i+j];
						break;
					}
					if( grid[i+j*width]!=0 ) {
						g[k] = grid[i+j*width];
						break;
					}
				}
			}
		}
		gz.grid = g;
		return gz;
	}
}

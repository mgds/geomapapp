/*
 * @(#)Grid2D.java	0.1	03/12/19
 *
 * $Log: Grid2D.java,v $
 * Revision 1.2  2004/08/03 20:08:37  bill
 * added rcs tags
 *
 */
package org.geomapapp.grid;

import org.geomapapp.geom.*;
import java.awt.geom.*;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.Dimension;

/**
 * A container for an 2-dimensional grid of numerical values.
 * @see MapProjection
 * @author Bill Haxby
 */
public abstract class Grid2D {

	public final static String rcsid = "$Id: Grid2D.java,v 1.2 2004/08/03 20:08:37 bill Exp $";	// add ID tag

	/**
	 * The bounds of the grid in the projected coordinate
	 * system.  
	 */
	protected Rectangle bounds;
	/**
	 * The map projection object that translates between the
	 * reference coordinate system and the grid space.
	 */
	protected MapProjection projection;

	/**
	 * initiallizes the dimensions of the <code>Grid2D</code>
	 *	object.
	 * @param bounds the bounding box of the <code>Grid2D</code>
	 * @param projection <code>MapProjection</code> for this grid, 
	 * or <code>null</code> (in which case an <code>IdentityProjection
	 * </code> will be created).
	 */
	protected Grid2D(Rectangle bounds,
			MapProjection projection) {
		if( projection==null ) this.projection = new IdentityProjection();
		else this.projection = projection;
		this.bounds = bounds;
	}
	/**
	 * Gets the rectangle that specifies the location and size
	 * of this <code>Grid2D</code> in the projected coordinate system.
	 * @return the bounds.
	 */
	public Rectangle getBounds() {
		return (Rectangle)bounds.clone();
	}
	public Dimension getSize() {
		return new Dimension(bounds.width, bounds.height);
	}
	public void fillNaNs() {
		byte[] mask = new byte[bounds.width*bounds.height];
		int n=0;
		int k=0;
		for( int y=bounds.y ; y<bounds.y+bounds.height ; y++) {
			for( int x=bounds.x ; x<bounds.x+bounds.width ; x++) {
				boolean ok = !Double.isNaN(valueAt(x,y));
				if( ok ) {
					mask[k++]=(byte)0;
				} else {
					mask[k++]=(byte)4;
					n++;
				}
			}
		}
		if( n==0 )return;
		float[] grid = new float[bounds.width*bounds.height];
		k=0;
		for( int y=bounds.y ; y<bounds.y+bounds.height ; y++) {
			for( int x=bounds.x ; x<bounds.x+bounds.width ; x++) {
				grid[k] = (float)valueAt(x,y);
				if( mask[k]==4 ) {
					int xA, xB, yA, yB;
					xA = xB = yA = yB = 0;
					for( int xx=x-1 ; xx>=bounds.x ; xx--) {
						double z = valueAt(xx,y);
						if( Double.isNaN(z) )continue;
						xA = xx-x;
						break;
					}
					for( int xx=x+1 ; xx<bounds.x+bounds.width ; xx++) {
						double z = valueAt(xx,y);
						if( Double.isNaN(z) )continue;
						xB = xx-x;
						break;
					}
					for( int yy=y-1 ; yy>=bounds.y ; yy--) {
						double z = valueAt(x,yy);
						if( Double.isNaN(z) )continue;
						yA = yy-y;
						break;
					}
					for( int yy=y+1 ; yy<bounds.y+bounds.height ; yy++) {
						double z = valueAt(x,yy);
						if( Double.isNaN(z) )continue;
						yB = yy-y;
						break;
					}
					double est = 0.;
					double wt = 0.;
					if( xA!=0 ) {
						double w = 1./Math.abs(xA);
						wt += w;
						est += w*valueAt(x+xA,y);
					}
					if( xB!=0 ) {
						double w = 1./Math.abs(xB);
						wt += w;
						est += w*valueAt(x+xB,y);
					}
					if( yA!=0 ) {
						double w = 1./Math.abs(yA);
						wt += w;
						est += w*valueAt(x,y+yA);
					}
					if( yB!=0 ) {
						double w = 1./Math.abs(yB);
						wt += w;
						est += w*valueAt(x,y+yB);
					}
					if( wt==0.) wt=1.;
					grid[k] = (float)(est/wt);
		//	System.out.println( (x-bounds.x) +"\t"+ xA +"\t"+xB +"\t"+ (y-bounds.y) +"\t"+ yA +"\t"+ yB +"\t"+grid[k]);
				}
				k++;
			}
		}
		haxby.grid.MinCurvature.solve(grid, mask, bounds.width, bounds.height,
							.25f, 1.5f, .1f);
		k=0;
		for( int y=bounds.y ; y<bounds.y+bounds.height ; y++) {
			for( int x=bounds.x ; x<bounds.x+bounds.width ; x++) {
				if( mask[k]==4 ) {
					setValue( x,y,(double)grid[k]);
				}
				k++;
			}
		}
	}
	public double[] getRange() {
		double[] range = new double[] {0.,0.};
		boolean start=true;
		for( int x=bounds.x ; x<bounds.x+bounds.width ; x++) {
			for( int y=bounds.y ; y<bounds.y+bounds.height ; y++) {
				double z = valueAt(x,y);
				if( java.lang.Double.isNaN(z) )continue;
				if (java.lang.Double.isInfinite(z)) continue;
				if( start) {
					start = false;
					range[0] = range[1] = z;
					continue;
				}
				if( z<range[0] )range[0]=z;
				if( z>range[1] )range[1]=z;
			}
		}
		return range;
	}
	public double[] getWESN() {
		double[] wesn = new double[] {0.,0.,0.,0.};
		if( projection.isCylindrical() ) {
		//	CylindricalProjection p = (CylindricalProjection)projection;
			Point2D ul = projection.getRefXY( bounds.getX(), bounds.getY() );
			Point2D lr = projection.getRefXY( bounds.getX()+bounds.getWidth(), bounds.getY()+bounds.getHeight() );
			wesn[0] = ul.getX();
			wesn[1] = lr.getX();
			if( wesn[1]<wesn[0] ) wesn[1]+=360;
			wesn[2] = lr.getY();
			wesn[3] = ul.getY();
			return wesn;
		}
		boolean start=true;
		Point2D.Double p;
		for( int x=bounds.x ; x<bounds.x+bounds.width ; x++) {
			p = (Point2D.Double)projection.getRefXY( new Point(x,bounds.y) );
			if( start ) {
				start=false;
				wesn[0] = wesn[1] = p.x;
				wesn[2] = wesn[3] = p.y;
				continue;
			}
			if( p.x>wesn[1] ) wesn[1]=p.x;
			else if( p.x<wesn[0] )wesn[0]=p.x;
			if( p.y>wesn[3] ) wesn[3]=p.y;
			else if( p.y<wesn[2] )wesn[2]=p.y;
			p = (Point2D.Double)projection.getRefXY( new Point(x,bounds.y+bounds.height) );
			if( p.x>wesn[1] ) wesn[1]=p.x;
			else if( p.x<wesn[0] )wesn[0]=p.x;
			if( p.y>wesn[3] ) wesn[3]=p.y;
			else if( p.y<wesn[2] )wesn[2]=p.y;
		}
		for( int y=bounds.y ; y<bounds.y+bounds.height ; y++) {
			p = (Point2D.Double)projection.getRefXY( new Point(bounds.x,y) );
			if( p.x>wesn[1] ) wesn[1]=p.x;
			else if( p.x<wesn[0] )wesn[0]=p.x;
			if( p.y>wesn[3] ) wesn[3]=p.y;
			else if( p.y<wesn[2] )wesn[2]=p.y;
			p = (Point2D.Double)projection.getRefXY( new Point(bounds.x+bounds.width,y) );
			if( p.x>wesn[1] ) wesn[1]=p.x;
			else if( p.x<wesn[0] )wesn[0]=p.x;
			if( p.y>wesn[3] ) wesn[3]=p.y;
			else if( p.y<wesn[2] )wesn[2]=p.y;
		}
		try {
			p = (Point2D.Double)projection.getMapXY( new Point(0, 90 ) );
			if( contains(p.getX(), p.getY()) ) wesn[3]=90.;
		} catch(Exception e) {
		}
		try {
			p = (Point2D.Double)projection.getMapXY( new Point(0, -90 ) );
			if( contains(p.getX(), p.getY()) ) wesn[2]=-90.;
		} catch(Exception e) {
		}
		return wesn;
	}
	public MapProjection getProjection() {
		return projection;
	}
	public boolean contains( int x, int y) {
		return !(x<bounds.x 
			|| x>=bounds.x+bounds.width 
			|| y<bounds.y 
			|| y>=bounds.y+bounds.height );
	}
	public boolean contains( double x, double y) {
		return !(x<bounds.x 
			|| x>bounds.x+bounds.width-1 
			|| y<bounds.y 
			|| y>bounds.y+bounds.height-1 );
	}
	public abstract double valueAt( int x, int y);
	
	public double valueAt( double x, double y ) {
		return valueAt(x, y, false);
	}
	
	public double valueAt( double x, double y, boolean isGMRT ) {
		return Interpolate2D.bicubic(this, x, y, isGMRT);
	}
	public int getIndex(int x, int y) {
		return x-bounds.x + bounds.width*(y-bounds.y);
	}
	/**
	 * Sets the value of a grid node.  If the specified node
	 * is not contained in the grid, the method returns 
	 * immediately.
	 * @param x the x-index of the grid node
	 * @param y the y-index of the grid node
	 * @param val A double value to replace the current
	 *      grid value at the specified node.
	 */
	public abstract void setValue( int x, int y, double val );

	public static class Double extends Grid2D {
		protected double[] grid;
		public final static double NaN = java.lang.Double.NaN;
		public Double( Rectangle bounds,
				MapProjection projection ) {
			super( bounds, projection );
			grid=null;
		}
		public static boolean isNaN(double z) {
			return java.lang.Double.isNaN(z);
		}
		public void initGrid() {
			if( grid!=null )return;
			grid = new double[bounds.width*bounds.height];
			for( int k=0 ; k<grid.length ; k++ ) grid[k] = NaN;
		}
		public double valueAt( int x, int y) {
			if( grid==null || !contains(x, y) ) return NaN;
			return grid[getIndex(x, y)];
		}
		public void setValue( int x, int y, double val ) {
			if( !contains(x, y) ) return;
			if( java.lang.Double.isNaN(val) && grid==null )return;
			initGrid();
			grid[getIndex(x, y)] = val;
		}
		public double[] getBuffer() {
			return grid;
		}
		/**
		 * Sets the data buffer that stores the grid values
		 * @param buffer The data buffer. Either a <code>double[]</code>
		 *	of length <code>bounds.width*bounds.height</code>,
		 * 	or <code>null</code>
		 * @throws ArrayIndexOutOfBoundsException if the length
		 * 	of the buffer is <B>not</B> 
		 *	<code>bounds.width*bounds.height</code>
		 * 	or <code>null</code>
		 */
		public void setBuffer(double[] buffer) {
			int size = bounds.width*bounds.height;
			if( buffer!=null && buffer.length<size ) {
				throw new ArrayIndexOutOfBoundsException(
					"buffer too small");
			}
			grid=buffer;
		}
	}
	public static class Integer extends Grid2D {
		public final static int NaN = 0x80000000;
		protected int[] grid;
		public Integer( Rectangle bounds,
				MapProjection projection ) {
			super( bounds, projection );
			grid=null;
		}
		public void initGrid() {
			if(grid!=null)return;
			grid = new int[bounds.width*bounds.height];
			for( int k=0 ; k<grid.length ; k++ ) grid[k] = NaN;
		}
		public double valueAt( int x, int y) {
			return (double)intValue(x, y);
		}
		public int intValue( int x, int y) {
			if( grid==null || !contains(x, y) ) return NaN;
			return grid[getIndex(x, y)];
		}
		public void setValue( int x, int y, double val ) {
			setValue( x, y, (int)val);
		}
		public void setValue( int x, int y, int val ) {
			if( !contains(x, y) ) return;
			if( grid==null && val==NaN )return;
			initGrid();
			grid[getIndex(x, y)] = val;
		}
		public int[] getBuffer() {
			return grid;
		}
		/**
		 * Sets the data buffer that stores the grid values
		 * @param buffer The data buffer. Either a <code>float[]</code>
		 *	of length <code>bounds.width*bounds.height</code>,
		 * 	or <code>null</code>
		 * @throws ArrayIndexOutOfBoundsException if the length
		 * 	of the buffer is <B>not</B> 
		 *	<code>bounds.width*bounds.height</code>
		 * 	or <code>null</code>
		 */
		public void setBuffer(int[] buffer) {
			int size = bounds.width*bounds.height;
			if( buffer!=null && buffer.length<size ) {
				throw new ArrayIndexOutOfBoundsException(
					"buffer too small");
			}
			grid=buffer;
		}
	}
	public static class Float extends Grid2D {
		public final static float NaN = java.lang.Float.NaN;
		protected float[] grid;
		public Float( Rectangle bounds,
				MapProjection projection ) {
			super( bounds, projection );
			grid=null;
		}
		public void initGrid() {
			if(grid!=null)return;
			grid = new float[bounds.width*bounds.height];
			for( int k=0 ; k<grid.length ; k++ ) grid[k] = NaN;
		}
		public double valueAt( int x, int y) {
			return (double)floatValue(x, y);
		}
		public float floatValue( int x, int y) {
			if( grid==null || !contains(x, y) ) return NaN;
			return grid[getIndex(x, y)];
		}
		public void setValue( int x, int y, double val ) {
			setValue( x, y, (float)val);
		}
		public void setValue( int x, int y, float val ) {
			if( !contains(x, y) ) return;
			if( grid==null && java.lang.Float.isNaN(val) )return;
			initGrid();
			grid[getIndex(x, y)] = val;
		}
		public float[] getBuffer() {
			return grid;
		}
		/**
		 * Sets the data buffer that stores the grid values
		 * @param buffer The data buffer. Either a <code>float[]</code>
		 *	of length <code>bounds.width*bounds.height</code>,
		 * 	or <code>null</code>
		 * @throws ArrayIndexOutOfBoundsException if the length
		 * 	of the buffer is <B>not</B> 
		 *	<code>bounds.width*bounds.height</code>
		 * 	or <code>null</code>
		 */
		public void setBuffer(float[] buffer) {
			int size = bounds.width*bounds.height;
			if( buffer!=null && buffer.length<size ) {
				throw new ArrayIndexOutOfBoundsException(
					"buffer too small");
			}
			grid=buffer;
		}
	}
	public static class FloatWT extends Grid2D {
		public final static float NaN = java.lang.Float.NaN;
		protected float[] grid;
		protected float[] weight;
		public FloatWT( Rectangle bounds,
				MapProjection projection ) {
			super( bounds, projection );
			grid=null;
		}
		public void initGrid() {
			if(grid!=null)return;
			grid = new float[bounds.width*bounds.height];
			weight = new float[bounds.width*bounds.height];
			for( int k=0 ; k<grid.length ; k++ ) weight[k] = grid[k] = 0f;
		}
		public double valueAt( int x, int y) {
			return (double)floatValue(x, y);
		}
		public float floatValue( int x, int y) {
			if( grid==null || !contains(x, y) ) return NaN;
			int k = getIndex(x, y);
			if( weight[k]==0 ) return NaN;
			return grid[k]/weight[k];
		}
		public void setValue( int x, int y, double val ) {
			setValue( x, y, (float)val, 1f);
		}
		public void setValue( int x, int y, float val, float wt ) {
			if( !contains(x, y) ) return;
			if( grid==null && java.lang.Float.isNaN(val) )return;
			initGrid();
			int k = getIndex(x, y);
			grid[k] = val*wt;
			weight[k] = wt;
		}
		public void addValue( int x, int y, float val, float wt ) {
			if( !contains(x, y) ) return;
			if( grid==null && java.lang.Float.isNaN(val) )return;
			initGrid();
			int k = getIndex(x, y);
			grid[k] += val*wt;
			weight[k] += wt;
		}
		public float[] getBuffer() {
			return grid;
		}
		public float[] getWeights() {
			return weight;
		}
		/**
		 * Sets the data buffer that stores the grid values
		 * @param buffer The data buffer. Either a <code>float[]</code>
		 *	of length <code>bounds.width*bounds.height</code>,
		 * 	or <code>null</code>
		 * @throws ArrayIndexOutOfBoundsException if the length
		 * 	of the buffer is <B>not</B> 
		 *	<code>bounds.width*bounds.height</code>
		 * 	or <code>null</code>
		 */
		public void setBuffer(float[] buffer, float[] wt) {
			int size = bounds.width*bounds.height;
			if( buffer!=null && buffer.length<size ) {
				throw new ArrayIndexOutOfBoundsException(
					"buffer too small");
			}
			grid=buffer;
			weight = wt;
		}
	}
	public static class Short extends Grid2D {
		public final static short NaN = -32768;
		double scale, offset;
		boolean scaled;
		protected short[] grid;
		public Short( Rectangle bounds,
				MapProjection projection ) {
			super( bounds, projection );
			scale = 1.;
			offset = 0.;
			scaled = false;
			grid=null;
		}
		public void scale(double offset, double scale) {
			this.scale = scale;
			this.offset = offset;
			if( scale!=1. || offset!=0. ) scaled=true;
		}
		public double[] getScales() {
			return new double[] {offset, scale};
		}
		public boolean isScaled() {
			return scaled;
		}
		public void initGrid() {
			if(grid!=null)return;
			grid = new short[bounds.width*bounds.height];
			for( int k=0 ; k<grid.length ; k++ ) grid[k] = NaN;
		}
		public double valueAt( int x, int y) {
			short val = shortValue(x, y);
			if( val==NaN )return Double.NaN;
			if( scaled ) return offset + val/scale;
			return (double)val;
		}
/*		public double valueAt( double x, double y ) {
			return valueAt( (int)Math.floor(x), (int)Math.floor(y));
		} */
		public short shortValue(int x, int y) {
			if( grid==null || !contains(x, y) ) return NaN;
			return grid[getIndex(x, y)];
		}
		public short shortValue( double x, double y ) {
			return shortValue( (int)Math.rint(x), (int)Math.rint(y));
		}
		
		public void setValue( int x, int y, double val, boolean invertOffset ) {
			if ( scaled ) {
				if ( invertOffset ) {
					val = (val - offset)*scale;
				}
				else {
					val = (val + offset)*scale;
				}
			}
			if( java.lang.Double.isNaN(val) ||
				Math.rint(Math.abs(val)) > 32767. ) setValue( x, y, NaN);
			else setValue( x, y, (short)Math.rint(val));
		}
		
		public void setValue( int x, int y, double val ) {
			if ( scaled ) {
				val = (val - offset)*scale;
			}
			if( java.lang.Double.isNaN(val) ||
				Math.rint(Math.abs(val)) > 32767. ) setValue( x, y, NaN);
			else setValue( x, y, (short)Math.rint(val));
		}
		public void setValue( int x, int y, short val ) {
			if( !contains(x, y) ) return;
			if( grid==null && val == NaN)return;
			initGrid();			
			grid[getIndex(x, y)] = val;
		}
		public short[] getBuffer() {
			return grid;
		}
		/**
		 * Sets the data buffer that stores the grid values
		 * @param buffer The data buffer. Either a <code>short[]</code>
		 *	of length <code>bounds.width*bounds.height</code>,
		 * 	or <code>null</code>
		 * @throws ArrayIndexOutOfBoundsException if the length
		 * 	of the buffer is <B>not</B> 
		 *	<code>bounds.width*bounds.height</code>
		 * 	or <code>null</code>
		 */
		public void setBuffer(short[] buffer) {
			int size = bounds.width*bounds.height;
			if( buffer!=null && buffer.length<size ) {
				throw new ArrayIndexOutOfBoundsException(
					"buffer too small");
			}
			grid=buffer;
		}
	}
	public static class Byte extends Grid2D {
		public final static short NaN = 0;
		protected byte[] grid;
		public Byte( Rectangle bounds,
				MapProjection projection ) {
			super( bounds, projection );
			grid=null;
		}
		public void initGrid() {
			if(grid!=null)return;
			grid = new byte[bounds.width*bounds.height];
			for( int k=0 ; k<grid.length ; k++ ) grid[k] = NaN;
		}
		public double valueAt( int x, int y) {
			short val = byteValue(x, y);
			return (double)val;
		}
		public byte byteValue(int x, int y) {
			if( grid==null || !contains(x, y) ) return NaN;
			return grid[getIndex(x, y)];
		}
		public void setValue( int x, int y, double val ) {
			setValue( x, y, (byte)Math.rint(val));
		}
		public void setValue( int x, int y, byte val ) {
			if( !contains(x, y) ) return;
			if( grid==null && val==NaN )return;
			initGrid();
			grid[getIndex(x, y)] = val;
		}
		public byte[] getBuffer() {
			return grid;
		}
		/**
		 * Sets the data buffer that stores the grid values
		 * @param buffer The data buffer. Either a <code>byte[]</code>
		 *	of length <code>bounds.width*bounds.height</code>,
		 * 	or <code>null</code>
		 * @throws ArrayIndexOutOfBoundsException if the length
		 * 	of the buffer is <B>not</B> 
		 *	<code>bounds.width*bounds.height</code>
		 * 	or <code>null</code>
		 */
		public void setBuffer(byte[] buffer) {
			int size = bounds.width*bounds.height;
			if( buffer!=null && buffer.length<size ) {
				throw new ArrayIndexOutOfBoundsException(
					"buffer too small");
			}
			grid=buffer;
		}
	}
	public static class Image extends Grid2D {
		public final static int NaN = 0;
		protected BufferedImage grid;
		public Image( Rectangle bounds,
				MapProjection projection ) {
			super( bounds, projection );
			grid=null;
		}
		public Image( Rectangle bounds,
				MapProjection projection,
				BufferedImage image ) {
			super( bounds, projection );
			grid=image;
		}
		public void initGrid() {
			if(grid!=null)return;
			grid = new BufferedImage(bounds.width,
					bounds.height,
					BufferedImage.TYPE_INT_ARGB);
		}
		public double valueAt( int x, int y) {
			int val = rgbValue(x, y);
			if( val==NaN ) return Double.NaN;
			return (double)val;
		}
		public double valueAt( double x, double y ) {
			return valueAt( (int)Math.rint(x), (int)Math.rint(y));
		}
		public int rgbValue(int x, int y) {
			if( grid==null || !contains(x, y) ) return NaN;
			return grid.getRGB( x-bounds.x, y-bounds.y );
		}
		public void setValue( int x, int y, double val ) {
			setValue( x, y, (int)Math.rint(val));
		}
		public void setValue( int x, int y, int val ) {
			if( !contains(x, y) ) return;
			if( grid==null && val==NaN )return;
			initGrid();
			grid.setRGB(x-bounds.x, y-bounds.y, val);
		}
		public BufferedImage getBuffer() {
			return grid;
		}
		/**
		 * Sets the data buffer that stores the grid values
		 * @param buffer The data buffer. Either a <code>byte[]</code>
		 *	of length <code>bounds.width*bounds.height</code>,
		 * 	or <code>null</code>
		 * @throws ArrayIndexOutOfBoundsException if the length
		 * 	of the buffer is <B>not</B> 
		 *	<code>bounds.width*bounds.height</code>
		 * 	or <code>null</code>
		 */
		public void setBuffer(BufferedImage image) {
			if( image.getWidth()!=bounds.width 
					|| image.getHeight()!=bounds.height) {
				throw new ArrayIndexOutOfBoundsException(
					"buffer too small");
			}
			grid=image;
		}
		public Image getGeoImage() {
			if( projection instanceof RectangularProjection ) return this;
		//	System.out.println( projection.getClass().getName() );
			if( !projection.isCylindrical() )return null;
		//	CylindricalProjection proj = (CylindricalProjection)projection;
			double[] wesn = getWESN();
			double dy = (wesn[3]-wesn[2])/(grid.getHeight()-1.);
			BufferedImage im = new BufferedImage(grid.getWidth(), grid.getHeight(), grid.getType());
			for( int j=0 ; j<grid.getHeight() ; j++) {
				double y = projection.getMapXY(new Point2D.Double(wesn[0],wesn[3] - j*dy)).getY();
				double d = y-Math.floor(y);
				int iy = (int)Math.floor(y) - bounds.y;
				if( iy==grid.getHeight()-1 )iy--;
				for( int x=0 ; x<grid.getWidth() ; x++ ) {
					if( iy<0 || iy>grid.getHeight()-2 ) {
						im.setRGB(x,j,0);
						continue;
					}
					int rgb1 = grid.getRGB(x,iy);
					int rgb2 = grid.getRGB(x,iy+1);
					int c1 = (rgb1>>24)&255;
					int c2 = (rgb2>>24)&255;
					if( c1!=255 || c2!=255 ) {
						im.setRGB(x,j,0);
						continue;
					}
					int rgb = 0xff000000;
					c1 = (rgb1>>16)&255;
					c2 = (rgb2>>16)&255;
					int c = (int)(c1+d*(c2-c1));
					if( c<0 )c=0;
					else if( c>255 )c=255;
					rgb |= c<<16;
					c1 = (rgb1>>8)&255;
					c2 = (rgb2>>8)&255;
					c = (int)(c1+d*(c2-c1));
					if( c<0 )c=0;
					else if( c>255 )c=255;
					rgb |= c<<8;
					c1 = rgb1&255;
					c2 = rgb2&255;
					c = (int)(c1+d*(c2-c1));
					if( c<0 )c=0;
					else if( c>255 )c=255;
					rgb |= c;
					im.setRGB( x, j, rgb);
				}
			}
			Grid2D.Image g = new Grid2D.Image( new Rectangle(0, 0, im.getWidth(), im.getHeight()),
						new RectangularProjection( wesn, im.getWidth(), im.getHeight()),
						im );
			return g;
		}
	}
	public static class Boolean extends Grid2D {
		protected byte[] grid;
		public Boolean( Rectangle bounds,
				MapProjection projection ) {
			super( bounds, projection );
			grid=null;
		}
		public void initGrid() {
			if(grid!=null)return;
			int size = bounds.width*bounds.height;
			size = (size+7)>>3;
			grid = new byte[size];
			for( int k=0 ; k<size ; k++ ) grid[k] = 0;
		}
		public double valueAt( double x, double y ) {
			return valueAt( (int)Math.rint(x),
					(int)Math.rint(y) );
		}
		public double valueAt( int x, int y) {
			boolean val = booleanValue(x, y);
			return val ? 1. : 0.;
		}
		public boolean booleanValue(int x, int y) {
			if( grid==null || !contains(x, y) ) return false;
			int i = getIndex(x,y);
			int k = i>>3;
			i -= k<<3;
			byte test = (byte)(1<<i);
			return (grid[k]&test)==test;
		}
		public void setValue( int x, int y, double val ) {
			setValue( x, y, (val!=0.) );
		}
		public void setValue( int x, int y, boolean tf ) {
			if( !contains(x, y) ) return;
			if( grid==null && !tf )return;
			initGrid();
			int i = getIndex(x,y);
			int k = i>>3;
			i -= k<<3;
			if(tf) {
				grid[k] |= (byte)(1<<i);
			} else {
				grid[k] &= (byte)(~(1<<i));
			}
		}
		public byte[] getBuffer() {
			return grid;
		}
		/**
		 * Sets the data buffer that stores the grid values
		 * @param buffer The data buffer. Either a <code>byte[]</code>
		 *	of length <code>(bounds.width*bounds.height+7)>>3</code>,
		 * 	or <code>null</code>
		 * @throws ArrayIndexOutOfBoundsException if the length
		 * 	of the buffer is <B>not</B> 
		 *	<code>(bounds.width*bounds.height+7)>>3</code>
		 * 	or <code>null</code>
		 */
		public void setBuffer(byte[] buffer) {
			int size = bounds.width*bounds.height;
			size = (size+7)>>3;
			if( buffer!=null && buffer.length<size ) {
				throw new ArrayIndexOutOfBoundsException(
					"buffer too small");
			}
			grid=buffer;
		}
	}
}
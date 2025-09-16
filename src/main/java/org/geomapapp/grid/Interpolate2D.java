package org.geomapapp.grid;

public class Interpolate2D {
	public static double bicubic_wrap(float[] z, 
				int width, 
				int height, 
				double x, 
				double y) {
		double x0 = Math.floor(x);
		if(x0>0 && x0<width-2) return bicubic(z, width, height, x, y);
		x -= x0;
		int ix = ((int)x0-1);
		while(ix<0) ix+=width;
		ix %= width;
		double y0 = Math.floor(y);
		if(y0<1)y0=1;
		if(y0>height-3)y0=height-3;
		double[] z1 = new double[4];
		double[] z0 = new double[4];
		int k = (int)(y0-1)*width;
		for(int i=0 ; i<4 ; i++) {
			z0[0] = z[ix+k];
			for(int k1=0 ; k1<4 ; k1++) {
				ix++;
				z0[k1] = z[ (ix%width) + k];
			}
			z1[i] = cubic(z0, 0, x);
			k += width;
		}
		return cubic(z1, 0, y-y0);
	}
	public static double bicubic(float[] z, 
				int width, 
				int height, 
				double x, 
				double y) {
		return bicubic( z, width, height, x, y, true);
	}
	public static double bicubic(float[] z, 
				int width, 
				int height, 
				double x, 
				double y,
				boolean testBounds) {
		double x0 = Math.floor(x);
		if(testBounds && (x0<0 || x0>width-1) ) return Double.NaN;
		if(x0<1)x0=1;
		if(x0>width-3)x0=width-3;
		double y0 = Math.floor(y);
		if(testBounds && (y0<0 || y0>height-1)) return Double.NaN;
		if(y0<1)y0=1;
		if(y0>height-3)y0=height-3;
		double[] z1 = new double[4];
		int k = (int)x0-1 + (int)(y0-1)*width;
		for(int i=0 ; i<4 ; i++) {
			z1[i] = cubic(z, k, x-x0);
			k += width;
		}
		return cubic(z1, 0, y-y0);
	}
	
	public static double cubic(float[] z, int offset, double x) {
		return cubic(z, offset, x, false);
	}
	
	public static double cubic(float[] z, int offset, double x, boolean isGMRT) {
		double[] z1 = new double[4];
		for(int i=0 ; i<4 ; i++) {
			z1[i] = (double) z[i+offset];
		}
		return cubic(z1, 0, x, isGMRT);
	}
	
	public static double bicubic(Grid2D grid, 
			double x, 
			double y ) {
		return bicubic(grid, x, y, false);
	}
	
	public static double bicubic(Grid2D grid, 
				double x, 
				double y, boolean isGMRT ) {
		if( !grid.contains( x, y ) ) return Double.NaN;
		java.awt.Rectangle bounds = grid.getBounds();
		int width = bounds.width;
		int height = bounds.height;
		x -= bounds.x;
		y -= bounds.y;
		double x0 = Math.floor(x);
		if(x0<1)x0=1;
		if(x0>width-3)x0=width-3;
		double y0 = Math.floor(y);
		if(y0<1)y0=1;
		if(y0>height-3)y0=height-3;
		double[] z = new double[4];
		double[] z1 = new double[4];
		for(int i=0 ; i<4 ; i++) {
			for( int k=0 ; k<4 ; k++ ) {
				z[k] = grid.valueAt( bounds.x + (int)(x0-1+k), 
						(int)(bounds.y + y0-1+i) );
			}
			z1[i] = cubic(z, 0, x-x0, isGMRT);
		}
		return cubic(z1, 0, y-y0, isGMRT);
	}
	
	public static double cubic(double[] z, int offset, double x, boolean isGMRT) {
		if (isGMRT) return cubicGMRT(z, offset, x);
		return cubic(z, offset, x);
	}
	
	public static double cubic(double[] z, int offset, double x) {
		for(int i=offset+1 ; i<offset+3 ; i++) {
			if(Double.isNaN(z[i])) return Double.NaN;
		}
		double x1 = 1-x;
		if( x<=-1. ) {
			return z[offset] + (x+1.)*( -1.5*z[offset] +2.*z[offset+1] -z[offset+2]*.5);
		} else if( x<=0. ) {
			return z[offset+1] + x*.5 * (z[offset+2] - z[offset]
					+ x * (z[offset+2] + z[offset] - 2*z[offset+1]));
		} else if( x>=2. ) {
			return z[offset+3] + (x-2.)*( .5*z[offset+1] -2.*z[offset+2] +1.5*z[offset+3]);
		} else if( x>=1. ) {
			return z[offset+2] + x1*.5 * (z[offset+1] - z[offset+3]
					+ x1 * (z[offset+1] + z[offset+3] - 2*z[offset+2]));
		}
		double z1 = z[offset+1] + x*.5 * (z[offset+2] - z[offset] + x * (z[offset+2] + z[offset] - 2*z[offset+1]));
		double z2 = z[offset+2] + x1*.5 * (z[offset+1] - z[offset+3] + x1 * (z[offset+1] + z[offset+3] - 2*z[offset+2]));
		if( Double.isNaN(z1) && Double.isNaN(z2) ) {
			return z[offset+1] + x * ( z[offset+2]-z[offset+1] );
		} else if( Double.isNaN(z1) )return z2;
		else if( Double.isNaN(z2) )return z1;
		return x1*z1 + x*z2;
/*
		double y = x1 * ( z[offset+1] +
				x*.5 * (z[offset+2] - z[offset]
				+ x * (z[offset+2] + z[offset] - 2*z[offset+1])))
			+ x * (z[offset+2] +
				x1*.5 * (z[offset+1] - z[offset+3]
				+ x1 * (z[offset+1] + z[offset+3] - 2*z[offset+2])));
		return y;
*/
	}
	
	//use this version for the GMRT grid
	public static double cubicGMRT(double[] z, int offset, double x) {
		for(int i=offset ; i<offset+4 ; i++) {
			if(Double.isNaN(z[i])) return Double.NaN;
		}
		double x1 = 1-x;
		if( x<=-1. ) {
			return z[offset] + (x+1.)*( -1.5*z[offset] +2.*z[offset+1] -z[offset+2]*.5);
		} else if( x<=0. ) {
			return z[offset+1] + x*.5 * (z[offset+2] - z[offset]
					+ x * (z[offset+2] + z[offset] - 2*z[offset+1]));
		} else if( x>=2. ) {
			return z[offset+3] + (x-2.)*( .5*z[offset+1] -2.*z[offset+2] +1.5*z[offset+3]);
		} else if( x>=1. ) {
			return z[offset+2] + x1*.5 * (z[offset+1] - z[offset+3]
					+ x1 * (z[offset+1] + z[offset+3] - 2*z[offset+2]));
		}
		double y = x1 * ( z[offset+1] +
				x*.5 * (z[offset+2] - z[offset]
				+ x * (z[offset+2] + z[offset] - 2*z[offset+1])))
			+ x * (z[offset+2] +
				x1*.5 * (z[offset+1] - z[offset+3]
				+ x1 * (z[offset+1] + z[offset+3] - 2*z[offset+2])));
		return y;
	}
	
	public static double[] linearMultiZ(double[][] xz, double x) {
		int i=0;
		for( i=0 ; i<xz.length ; i++ ) {
			if( x<xz[i][0] ) break;
		}
		if( i!=0 )i--;
		if( i==xz.length-1 ) i--;
		double[] z = new double[xz[0].length-1];
		for( int j=0 ; j<z.length ; j++) {
			if( xz[i+1][0]==xz[i][0] ) z[j] = .5*(xz[i][j+1]+xz[i+1][j+1]);
			else z[j] =  xz[i][j+1] + (x-xz[i][0]) * (xz[i+1][j+1]-xz[i][j+1]) / (xz[i+1][0]-xz[i][0]);
		}
		return z;
	}
	public static double linear(double[][] xz, double x) {
		int i=0;
		for( i=0 ; i<xz.length ; i++ ) {
			if( x<xz[i][0] ) break;
		}
		if( i!=0 )i--;
		if( i==xz.length-1 ) i--;
		double z = ( xz[i+1][0]==xz[i][0] ) 
				? .5*(xz[i][1]+xz[i+1][1])
				: xz[i][1] + (x-xz[i][0]) * (xz[i+1][1]-xz[i][1]) / (xz[i+1][0]-xz[i][0]);
		return z;
	}
	static final int[] icoeffX =  { 0, -1,  0,  1, -2, -1,  1,  2, -1,  0,  1,  0};
	static final int[] icoeffY =  {-2, -1, -1, -1,  0,  0,  0,  0,  1,  1,  1,  2};
	static final float[] coeff0 = {-1, -2,  8, -2, -1,  8,  8, -1, -2,  8, -2, -1};
	static final float[] coeff2 = { 0,  0,  1,  0,  0,  1,  1,  0,  0,  1,  0,  0};
	public static int minCurvature(float[] z, byte[] mask, 
				int width, int height, 
				float tension, float relaxation, float test) {
		int[] icoeff = new int[12];
		float[] coeff = new float[12];
		float total=0f;
		for( int i=0 ; i<12 ; i++) {
			icoeff[i] = icoeffX[i] + width*icoeffY[i];
			coeff[i] = (1-tension) * coeff0[i] +
					tension * coeff2[i];
			total += coeff[i];
		}
		for( int i=0 ; i<12 ; i++) coeff[i] *= relaxation/total;
		float r = 1-relaxation;
		float z1;
		int xc, yc, i0;
		int nIterations = 0;
		float maxDiff=0;
		do {
	// first do top and bottom edges
			maxDiff=0;
			for( int y=0 ; y<2 ; y++) {
				for( int x=0 ; x<width ; x++) {
					i0 = x+width*y;
					if(mask[i0] < 3)continue;
					z1 = z[i0]*r;
					for( int i=0 ; i<12 ; i++) {
						yc = y+icoeffY[i];
						xc = x+icoeffX[i];
						if( yc<0 ) yc=-yc;
						if( xc<0 ) xc=-xc;
						if( xc>=width ) xc = 2*width - xc -2;
						z1 += coeff[i] * z[xc+width*yc];
					}
				//	if(z1>-10f)continue;
					maxDiff = Math.max(maxDiff, Math.abs(z1-z[i0]));
					z[i0]=z1;
				}
			}
			for( int y=height-2 ; y<height ; y++) {
				for( int x=0 ; x<width ; x++) {
					i0 = x + width*y;
					if(mask[i0] < 3)continue;
					z1 = z[i0]*r;
					for( int i=0 ; i<12 ; i++) {
						yc = y+icoeffY[i];
						xc = x+icoeffX[i];
						if( yc>=height ) yc = 2*height - yc - 2;
						if( xc<0 ) xc=-xc;
						if( xc>=width ) xc = 2*width - xc - 2;
						z1 += coeff[i] * z[xc+width*yc];
					}
				//	if(z1>-10f)continue;
					maxDiff = Math.max(maxDiff, Math.abs(z1-z[i0]));
					z[i0]=z1;
				}
			}
	// left and right edges
			for( int x=0 ; x<2 ; x++) {
				for( int y=2 ; y<height-2 ; y++) {
					i0 = x+width*y;
					if(mask[i0] < 3)continue;
					z1 = z[i0]*r;
					for( int i=0 ; i<12 ; i++) {
						yc = y+icoeffY[i];
						xc = x+icoeffX[i];
						if( xc<0 ) xc=-xc;
						z1 += coeff[i] * z[xc+width*yc];
					}
				//	if(z1>-10f)continue;
					maxDiff = Math.max(maxDiff, Math.abs(z1-z[i0]));
					z[i0]=z1;
				}
			}
			for( int x=width-2 ; x<width ; x++) {
				for( int y=2 ; y<height-2 ; y++) {
					i0 = x+width*y;
					if(mask[i0] < 3)continue;
					z1 = z[i0]*r;
					for( int i=0 ; i<12 ; i++) {
						yc = y+icoeffY[i];
						xc = x+icoeffX[i];
						if( xc>=width ) xc = 2*width - xc - 2;
						z1 += coeff[i] * z[xc+width*yc];
					}
				//	if(z1>-10f)continue;
					maxDiff = Math.max(maxDiff, Math.abs(z1-z[i0]));
					z[i0]=z1;
				}
			}
	// interior
			int kount = 0;
			for( int y=2*width ; y<width * (height-2) ; y+=width) {
				for( int x=y+2 ; x<y+width-2 ; x++) {
					if(mask[x] < 3)continue;
					kount++;
					z1 = z[x]*r;
					for( int i=0 ; i<12 ; i++) {
						z1 += coeff[i]*z[x+icoeff[i]];
					}
					maxDiff = Math.max(maxDiff, Math.abs(z1-z[x]));
				//	if(z1>-10f)continue;
					z[x]=z1;
				}
			}
			nIterations++;
	//	System.out.println(nIterations+"\t"+maxDiff +"\t"+ kount);
		} while(maxDiff>test && nIterations<250);
		return nIterations;
	}
}

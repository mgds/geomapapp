package org.geomapapp.grid;

public class Interpolate2D {
	static boolean bicubic_fallback = false;
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
		double[] z1 = new double[4];
		for(int i=0 ; i<4 ; i++) {
			z1[i] = (double) z[i+offset];
		}
		return cubic(z1, 0, x);
	}
	public static double bicubic(Grid2D grid, 
				double x, 
				double y ) {
		return bicubic(grid,x,y,true);
	}
	public static double bicubicNanAware(Grid2D grid, 
				double x, 
				double y ) {
		return bicubicNanAware(grid,x,y,true);
	}
	
	public static double bicubic(Grid2D grid, 
				double x, 
				double y,
				boolean andcontains ) {
		if( andcontains && !grid.contains( x, y ) ) return Double.NaN;
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
			z1[i] = cubic(z, 0, x-x0);
		}
		return cubic(z1, 0, y-y0);
	}
	
	public static double bicubicNanAware(Grid2D grid, 
				double x, 
				double y,
				boolean andcontains ) {
		if( andcontains && !grid.contains( x, y ) ) return Double.NaN;
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
			z1[i] = cubicNanAware(z, 0, x-x0);
		}
		return cubicNanAware(z1, 0, y-y0);
	}
    
	public static double bilinear(Grid2D grid, 
				double x, 
				double y ) {
		return bilinear(grid,x,y,true);
	}
    public static double bilinear(Grid2D grid, 
				double x, 
				double y,
				boolean andcontains ) {
		if( andcontains && !grid.contains( x, y ) ) return Double.NaN;
		java.awt.Rectangle bounds = grid.getBounds();
		int width = bounds.width;
		int height = bounds.height;
		x -= bounds.x;
		y -= bounds.y;
        double x0 = Math.floor(x);
		if(x0<1)x0=1;
		if(x0>width-1)x0=width-1;
		double y0 = Math.floor(y);
		if(y0<1)y0=1;
		if(y0>height-1)y0=height-1;
		double[] z = new double[2];
		double[] z1 = new double[2];
		for(int i=0 ; i<2 ; i++) {
			for( int k=0 ; k<2 ; k++ ) {
				z[k] = grid.valueAt( bounds.x + (int)(x0-1+k), 
						(int)(bounds.y + y0-1+i) );
			}
			z1[i] = linear(z, 0, x-x0);
		}
		return linear(z1, 0, y-y0);
    }
    
    public static double linear (double[] z, int offset, double x) {
        for(int i=offset ; i<offset+2 ; i++) {
			if(Double.isNaN(z[i])) return Double.NaN;
		}
        return (1.-x)*z[offset] + (x)*z[offset+1];
    }
	
	public static double bicubic_edge(Grid2D grid, 
				double x, 
				double y ) {
		if( !grid.contains( x, y ) ) return Double.NaN;
		java.awt.Rectangle bounds = grid.getBounds();
		int width = bounds.width;
		int height = bounds.height;
		x -= bounds.x;
		y -= bounds.y;
		double x0 = Math.floor(x);
		//if(x0<1)x0=1;
		//if(x0>width-3)x0=width-3;
		double y0 = Math.floor(y);
		//if(y0<1)y0=1;
		//if(y0>height-3)y0=height-3;
		double[] z = new double[4];
		double[] z1 = new double[4];
		for(int i=0 ; i<4 ; i++) {
			for( int k=0 ; k<4 ; k++ ) {
				double zx0 = Math.max(1,Math.min(width,x0+k))-1;
				double zy0 = Math.max(1,Math.min(height,y0+k))-1;
				z[k] = grid.valueAt( ((int)bounds.x + zx0), 
						(int)(bounds.y + zy0) );
			}
			z1[i] = cubic(z, 0, x-x0);
		}
		return cubic(z1, 0, y-y0);
	}
	public static double cubic(double[] z, int offset, double x) {
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

	public static double cubicNanAware(double[] z, int offset, double x) {
		// Count valid samples
		boolean[] ok = new boolean[4];
		int n = 0;
		for (int i = 0; i < 4; i++) {
			ok[i] = !Double.isNaN(z[offset + i]);
			if (ok[i]) n++;
		}

		// No data
		if (n == 0) return Double.NaN;

		// Only one value → nearest
		if (n == 1) {
			for (int i = 0; i < 4; i++) {
				double dist = Math.abs(x - ((double) i - 1.0));
				if (ok[i] && dist <= 0.5)
					return z[offset + i];
			}
			return Double.NaN;
		}

		// Two adjacent values → linear interpolation
		if (n == 2) {
			for (int i = 0; i < 3; i++) {
				double dist1 = Math.abs(x - ((double) i - 1.0));
				double dist2 = Math.abs(x - ((double) i));
				boolean between = (x >= ((double) i - 1.0)) && (x <=((double) i));
				if (ok[i] && ok[i + 1] && (between || dist1<=0.5 || dist2>=0.5)) {
					double t = x - (i - 1);
					return z[offset + i] * (1 - t)
						+ z[offset + i + 1] * t;
				}
			}
		}

		// Three or four values → cubic using only valid samples
		// Fill missing samples by copying nearest valid neighbor
		double[] zz = new double[4];
		for (int i = 0; i < 4; i++) {
			if (ok[i]) {
				zz[i] = z[offset + i];
			} else {
				// nearest valid neighbor
				for (int d = 1; d < 4; d++) {
					if (i - d >= 0 && ok[i - d]) { zz[i] = z[offset + i - d]; break; }
					if (i + d < 4 && ok[i + d]) { zz[i] = z[offset + i + d]; break; }
				}
			}
		}

		return cubic(zz, offset, x);
	}
}

package haxby.grid;

public class Interpolate {
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
	static double[] c0 = {-1d/3d, -1d/2d, 1, -1d/6d };
	static double[] c1 = {1d/2d, -1, 1d/2d };
	static double[] c2 = {-1d/6d, 1d/2d, -1d/2d, 1d/6d };
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
}

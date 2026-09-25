package org.geomapapp.math;

import org.geomapapp.util.*;

import java.awt.*;

public class BandPass {
	public static final int SPLINE = 0;
	public static final int LINEAR = 1;
	double[] t;
	double[] z;
	double spacing;
	double[] eqZ;
	double[] xRange, yRange;
	int method;
	boolean whiten;
	String xTitle, yTitle;

	public BandPass( double[] t, 
			double[] z, 
			double spacing,
			int interpolateMethod,
			boolean preWhiten ) {
		this.spacing = spacing;
		this.t = t;
		this.z = z;
		whiten = preWhiten;
		method = interpolateMethod;
		interpolate( );
		xTitle = "CDP number";
		yTitle = "two-way time, mSec";
	}
	public double[][] pass(double[] low, double[] high) {
		int n = eqZ.length;
		double[][] f = new double[n][2];
		double N = 1./n;
		if( whiten ) {
			for( int i=0 ; i<n-1 ; i++ ) {
				f[i][0] = eqZ[i+1]-eqZ[i];
				f[i][1] = 0.;
			}
			f[eqZ.length-1][0] = 0.;
			f[eqZ.length-1][1] = 0.;
		} else {
			for( int i=0 ; i<n ; i++ ) {
				f[i][0] = eqZ[i];
				f[i][1] = 0.;
			}
		}
		f = FFT.fft_1d( f );
		double dp1 = .5*Math.PI/(low[1]-low[0]);
		double dp2 = .5*Math.PI*(high[1]-high[0]);
		double k0 = 1./(xRange[1]-xRange[0]);
		for( int i=0 ; i<n ; i++) {
			double k = (i>n/2) ?
				(n-i)*k0 : i*k0;
			if( k<low[0] ) {
				f[i][0] =f[i][1] = 0.;
			} else if( k<low[1] ) {
				double factor = N*Math.pow(Math.sin( (k-low[0])*dp1 ), 2.);
				f[i][0] *= factor;
				f[i][1] *= factor;
			} else if( k<high[0] ) {
				f[i][0] *= N;
				f[i][1] *= N;
			} else if( k<high[1] ) {
				double factor = N*Math.pow(Math.sin( (k-high[1])*dp2 ), 2.);
				f[i][0] *= factor;
				f[i][1] *= factor;
			} else {
				f[i][0] =f[i][1] = 0.;
			}
		}
		f = FFT.ifft_1d( f );
		if( whiten ) {
			f[0][1] = 0.;
			for( int i=1 ; i<n ; i++) {
				f[i][1] = f[i-1][1]+f[i-1][0];
			}
			for( int i=0 ; i<n ; i++) {
				f[i][0] = f[i][1];
			}
		}
		double[] tt = new double[t.length];
		double[] zz = new double[t.length];
		int i=0;
		double eqSpacing = (xRange[1]-xRange[0]) / (n-1);
		yRange = new double[2];
		for( int k=0 ; k<t.length ; k++) {
			tt[k] = t[k];
			double test = xRange[0] + i*eqSpacing;
			while( test+eqSpacing<tt[k]&&i<n-2 ) {
				i++;
				test += eqSpacing;
			}
			zz[k] = f[i][0] + (tt[k]-test)*(f[i+1][0]-f[i][0])/eqSpacing;
			if( k==0 ) {
				yRange[0]=yRange[1]=zz[k];
			} else if(yRange[0]>zz[k]) {
				yRange[1]=zz[k];
			} else if(yRange[1]<zz[k]) {
				yRange[1] = zz[k];
			}
		}
		double dy = yRange[1]-yRange[0];
		yRange[0] -= .05*dy;
		yRange[1] += .05*dy;
		f = new double[][] {tt, zz};
		return f;
	}
	public double[] getRange() {
		return new double[] {xRange[0], xRange[1]};
	}
	public double getSpacing() {
		return (xRange[1]-xRange[0])/(eqZ.length-1);
	}
	void interpolate( ) {
		double[] t;
		double[] z;
		double[][] tz = bin( this.t, this.z, spacing);
		t = tz[0];
		z = tz[1];
		int i1 = (int)Math.rint(t[0]/spacing);
		int i2 = (int)Math.rint(t[t.length-1]/spacing);
		int n = i2-i1+1;
		int n2 = 2;
		while( n2<n ) n2*=2;
		double eqSpacing = spacing * (double)n/(double)n2;
		xRange = new double[2];
		xRange[0] = i1*spacing;
		xRange[1] = xRange[0] + (n2-1)*eqSpacing;
		eqZ = new double[n2];
		if( method==0 ) {
			double[][] c = Spline.spline(z, t, t.length);
			int i=0;
			for( int k=0 ; k<n2 ; k++) {
				double x = xRange[0] + k*eqSpacing;
				while( x>t[i+1] && i<t.length-2 ) i++;
				x -= t[i];
				eqZ[k] = z[i] + x*(c[i][0]+x*(c[i][1]+x*c[i][2]));
			}
		} else {
			int i=0;
			for( int k=0 ; k<n2 ; k++) {
				double x = xRange[0] + k*eqSpacing;
				while( x>t[i+1] && i<t.length-2 ) i++;
				x -= t[i];
				double dt = t[i+1]-t[i];
				eqZ[k] = z[i] + x*(z[i+1]-z[i])/dt;
			}
		}
	}
	public static double[][] bin( double[] tt, double[] zz, double spacing ) {
		double min, max;
		min = max = tt[0];
		for( int i=1 ; i<tt.length ; i++) {
			if( min>tt[i] )min=tt[i];
			else if(max<tt[i]) max=tt[i];
		}
		int i1 = (int)Math.rint(min/spacing);
		int i2 = (int)Math.rint(max/spacing);
		int n = i2-i1+1;
		double[] t = new double[n];
		double[] z = new double[n];
		int[] w = new int[n];
		for( int k=0 ; k<n ; k++) w[k]=0;
		for( int i=0 ; i<tt.length ; i++) {
			int k = (int)Math.rint(tt[i]/spacing)-i1;
			t[k] += tt[i];
			z[k] += zz[i];
			w[k]++;
		}
		int m = 0;
		for( int k=0 ; k<n ; k++) {
			if( w[k]==0 ) {
				m++;
				continue;
			}
			z[k-m] = z[k]/w[k];
			t[k-m] = t[k]/w[k];
		}
		if( m!=0 ) {
			tt = new double[n-m];
			System.arraycopy( z, 0, tt, 0, n-m);
			z = tt;
			tt = new double[n-m];
			System.arraycopy( t, 0, tt, 0, n-m);
			t = tt;
		}
		return new double[][] {t, z};
	}
}

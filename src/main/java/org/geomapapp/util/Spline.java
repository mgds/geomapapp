package org.geomapapp.util;

public class Spline {
	public static double[][] spline(double[] x, double[] t, int n) {
		double[] dt = new double[n-1];
		double[] d = new double[n-1];
		double[] b = new double[n-1];
		double[][] a = new double[n][3];

		for( int i=0 ; i<n-1 ; i++) {
			dt[i] = t[i+1] - t[i];
			d[i] = (x[i+1] - x[i]) / dt[i];
		}
		for( int i=1 ; i<n-1 ; i++ ) {
			a[i][0] = dt[i-1];
			a[i][1] = 2 * (dt[i-1]+dt[i]);
			a[i][2] = dt[i];
			b[i] = 3 * (d[i]-d[i-1]);
		}
		a[0][1] = 1;
		a[0][2] = 0;
		b[0] = 0;

		a[n-2][2] = 0;

		for(int i=1 ; i<n-1 ; i++) {
			a[i][1] = a[i][1]*a[i-1][1] - a[i-1][2]*a[i][0];
			a[i][2] = a[i][2]*a[i-1][1];
			b[i] = b[i]*a[i-1][1] - b[i-1]*a[i][0];
		}
		a[n-2][1] = b[n-2] / a[n-2][1];
		for( int i=n-3 ; i>=0 ; i--) {
			a[i][1] = (b[i] - a[i][2]*a[i+1][1]) / a[i][1];
		}
		for( int i=0 ; i<n-2 ; i++) {
			a[i][2] = (a[i+1][1]-a[i][1]) / (3*dt[i]);
			a[i][0] = d[i] - dt[i]*( a[i][1] + dt[i]*a[i][2] );
		}
		a[n-2][2] = -a[n-2][1] / (3*dt[n-2]);
		a[n-2][0] = d[n-2] - dt[n-2]*( a[n-2][1] + dt[n-2]*a[n-2][2] );

		a[n-1][0] = a[n-2][0] + 2.* dt[n-2]*a[n-2][1];
		a[n-1][1] = a[n-2][1] + 3.* dt[n-2]*a[n-2][2];
		a[n-1][2] = 0.;

		return a;
	}
/*
	public static double[][] spline(double[] x, double[] t, int n) {
		double[] dt = new double[n-1];
		double[] d = new double[n-1];
		double[] b = new double[n-1];
		double[][] a = new double[n-1][3];

		for( int i=0 ; i<n-1 ; i++) {
			dt[i] = t[i+1] - t[i];
			d[i] = (x[i+1] - x[i]) / dt[i];
		}
		for( int i=2 ; i<n-2 ; i++ ) {
			a[i][0] = dt[i-1];
			a[i][1] = 2 * (dt[i-1]+dt[i]);
			a[i][2] = dt[i];
			b[i] = 3 * (d[i]-d[i-1]);
		}
		a[1][1] = (dt[0]+dt[1]) * (2*dt[1]+dt[0]);
		a[1][2] = (dt[0]+dt[1]) * (dt[1]-dt[0]);
		b[1] = 3 * dt[1] * (d[1] - d[0]);

		a[n-2][0] = (dt[n-3]+dt[n-2]) * (dt[n-3]-dt[n-2]);
		a[n-2][1] = (dt[n-3]+dt[n-2]) * (2*dt[n-3]+dt[n-2]);
		b[n-2] = 3 * dt[n-3] * (d[n-2]-d[n-3]);

		for(int i=2 ; i<n-1 ; i++) {
			a[i][1] = a[i][1]*a[i-1][1] - a[i-1][2]*a[i][0];
			a[i][2] = a[i][2]*a[i-1][1];
			b[i] = b[i]*a[i-1][1] - b[i-1]*a[i][0];
		}
		a[n-2][1] = b[n-2] / a[n-2][1];
		for( int i=n-3 ; i>=1 ; i--) {
			a[i][1] = (b[i] - a[i][2]*a[i+1][1]) / a[i][1];
		}
		a[0][1] = -(a[2][1]*dt[0] - a[1][1]*(dt[0]+dt[1]) ) / dt[1];
		for( int i=0 ; i<n-2 ; i++) {
			a[i][2] = (a[i+1][1]-a[i][1]) / (3*dt[i]);
			a[i][0] = d[i] - dt[i]*( a[i][1] + dt[i]*a[i][2] );
		}
		a[n-2][2] = a[n-3][2];
		a[n-2][0] = d[n-2] - dt[n-2]*( a[n-2][1] + dt[n-2]*a[n-2][2] );

		return a;
	}
*/
	public static double[][] spline(double[] x, int n) {
		double[][] a = new double[n-1][3];
		double[] b = new double[n];
		int i, j, k;
		for( i=2 ; i<n-2 ; i++) {
			b[i] = 3d*(x[i+1] - 2*x[i] + x[i-1]);
		}
		b[1] = (x[2] - 2*x[1] + x[0]) / 2;
		a[n-2][1] = (x[n-1] - 2*x[n-2] + x[n-3]) / 2;
		a[1][1] = 0;
		for( i=1 ; i<n-3 ; i++) {
			a[i+1][1] = 1 / (4-a[i][1]);
			b[i+1] = (b[i+1]-b[i]) * a[i+1][1];
		}
		for( i=n-3 ; i>=2 ; i-- ) {
			a[i][1] = b[i] - a[i+1][1]*a[i][1];
		}
		a[1][1] = b[1];
		a[0][1] = -a[2][1] + a[1][1]*2;
	//	a[n-2][1] = -a[n-4][1] + a[n-3][1]*2;
		for( i=0 ; i<n-2 ; i++ ) {
			a[i][2] = (a[i+1][1] - a[i][1]) / 3;
		}
		a[n-2][2] = a[n-3][2];
		for( i=0 ; i<n-1 ; i++ ) {
			a[i][0] = x[i+1]-x[i] - a[i][1] - a[i][2];
		}
		return a;
	}
	public static double[][] wrapSpline(double[] x, int n) {
		double[] b = new double[n];
		double[][] a = new double[n][3];
		double[][] a1 = new double[2][n];
		int i, j, k;
		for( i=0 ; i<n-2 ; i++) {
			b[i] = 3d*(x[i+2] - x[i]);
		}
		b[n-2] = 3d*(x[0]-x[n-2]);
		b[n-1] = 3d*(x[1]-x[n-1]);
		for( i=0 ; i<n ; i++) {
			a1[0][i] = 0d;
			a1[1][i] = 0d;
		}
		a1[0][n-2] = 1d;
		a1[0][n-1] = 4d;
		a1[0][0] = 1d;
		a1[1][n-1] = 1d;
		a1[1][0] = 4d;
		a1[1][1] = 1d;
		for( i=0 ; i<n-2 ; i++) {
			a1[0][i+1] -= 4d*a1[0][i];
			a1[0][i+2] -= a1[0][i];
			b[n-2] -= b[i]*a1[0][i];
			a1[1][i+1] -= 4d*a1[1][i];
			a1[1][i+2] -= a1[1][i];
			b[n-1] -= b[i]*a1[1][i];
		}

		a[n-2][0] = (b[n-2]*a1[1][n-1] - b[n-1]*a1[0][n-1]) /
				(a1[0][n-2]*a1[1][n-1] - a1[1][n-2]*a1[0][n-1]);
		a[n-1][0] = (b[n-1] - a[n-2][0]*a1[1][n-2]) / a1[1][n-1];

		for( i=n-3 ; i>=0 ; i--) {
			a[i][0] = (b[i] - a[i+2][0] - 4d*a[i+1][0]);
		}
		for( i=0 ; i<n ; i++) {
			k = (i+1)%n;
			double dx = x[k] - x[i];
			a[i][1] = 3d*dx - 2d*a[i][0] - a[k][0];
			a[i][2] = dx - a[i][0] - a[i][1];
		}
		return a;
	}
	public static double[][] wrapSpline(double[] x, double[] dt, int n) {
		double[] b = new double[n];
		double[][] a = new double[n][3];
		double[][] a1 = new double[2][n];
		int i, j, k;
		for( i=0 ; i<n ; i++) {
			j = (i+1)%n;
			k = (i+2)%n;
			a[i][2] = dt[i]/dt[j];
			a[i][1] = 2d * (1d + a[i][2]);
			b[i] = 3d*((x[k]-x[j])*a[i][2]/dt[j]
				 + (x[j] - x[i])/dt[i]);
			a1[0][i] = 0d;
			a1[1][i] = 0d;
		}
		a1[0][n-2] = 1d;
		a1[0][n-1] = a[n-2][1];
		a1[0][0] = a[n-2][2];
		a1[1][n-1] = 1d;
		a1[1][0] = a[n-1][1];
		a1[1][1] = a[n-1][2];

		for( i=0 ; i<n-2 ; i++) {
			a1[0][i+1] -= a[i][1]*a1[0][i];
			a1[0][i+2] -= a[i][2] * a1[0][i];
			b[n-2] -= b[i]*a1[0][i];
			a1[1][i+1] -= a[i][1]*a1[1][i];
			a1[1][i+2] -= a[i][2] * a1[1][i];
			b[n-1] -= b[i]*a1[1][i];
		}

		a[n-2][0] = (b[n-2]*a1[1][n-1] - b[n-1]*a1[0][n-1]) /
				(a1[0][n-2]*a1[1][n-1] - a1[1][n-2]*a1[0][n-1]);
		a[n-1][0] = (b[n-1] - a[n-2][0]*a1[1][n-2]) / a1[1][n-1];

		for( i=n-3 ; i>=0 ; i--) {
			a[i][0] = (b[i] - a[i][2]*a[i+2][0] - a[i][1]*a[i+1][0]);
		}
		for( i=0 ; i<n ; i++) {
			k = (i+1)%n;
			double dx = (x[k] - x[i])/dt[i];
			a[i][1] = (3d*dx - 2d*a[i][0] - a[k][0]) / dt[i];
			a[i][2] = ((dx - a[i][0])/dt[i] - a[i][1])/dt[i];
		}
		return a;
	}
}
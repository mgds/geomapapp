package haxby.util;

public class LinearEq {
	public static double[] wlsq( double[][] x, double[] y, double[] wt, int nf, LFunction function ) {
		double[] b = new double[nf];
		double[][] wk = new double[nf+1][nf];

		for( int i=0 ; i<nf ; i++ ) {
			b[i]=0;
			for(int k=0 ; k<=nf ; k++ ) wk[k][i]=0.;
		}

		for( int i=0 ; i<y.length ; i++) {
			wk[nf] = function.eval( x[i], nf );
			for( int k=0 ; k<nf ; k++ ) {
				b[k] += wk[nf][k]*y[i]*wt[i];
				for( int j=k ; j<nf ; j++ ) {
					wk[j][k] += wk[nf][k] * wk[nf][j] * wt[i];
				}
			}
		}
		for( int k=0 ; k<nf-1 ; k++ ) {
			for( int j=k+1 ; j<nf ; j++ ) {
				wk[k][j] = wk[j][k];
			}
		}
		return solve( wk, b );
	}
	public static double[] solve( double[][] a, double[] b ) {
		int n = b.length;
		for(int i=0 ; i<n ; i++ ) {
			if( a[i][i]==0. ) {
				b[i] = 0;
				continue;
			}
			double t = 1./a[i][i];
			for( int k=i+1 ; k<n ; k++ ) {
				a[i][k] *= t;
			}
			b[i] *= t;
			a[i][i] = 1.;
			for( int j=i+1 ; j<n ; j++ ) {
				for( int k=i+1 ; k<n ; k++ ) {
					a[j][k] -= a[i][k]*a[j][i];
				}
				b[j] -= b[i]*a[j][i];
			}
		}
		for( int i=n-2 ; i>=0 ; i--) {
			if( a[i][i]==0. ) {
				b[i] = 0.;
			} else {
				for( int k=i+1 ; k<n ; k++ ) {
					b[i] -= b[k]*a[i][k];
				}
			}
		}
		return b;
	}
}

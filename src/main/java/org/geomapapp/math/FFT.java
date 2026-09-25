package org.geomapapp.math;

public class FFT {
	public static void main(String[] args) {
		double[][] data = new double[8][2];
		data[0][0] = 1.;
		fft_1d(data);
		ifft_1d(data);
		System.out.println( data[0][0] +"");
	}

	public static double[][] fft_1d(  double[][] array ) {
			double u_r,u_i, w_r,w_i, t_r,t_i;
			int ln, nv2, k, l, le, le1, j, ip, i, n;

			n = array.length;
			ln = (int)( Math.log( (double)n )/Math.log(2) + 0.5 );
			nv2 = n / 2;
			j = 1;
			for (i = 1; i < n; i++ ) {
					if (i < j) {
							t_r = array[i - 1][0];
							t_i = array[i - 1][1];
							array[i - 1][0] = array[j - 1][0];
							array[i - 1][1] = array[j - 1][1];
							array[j - 1][0] = t_r;
							array[j - 1][1] = t_i;
					}
					k = nv2;
					while (k < j) {
							j = j - k;
							k = k / 2;
					}
					j = j + k;
			}

			for (l = 1; l <= ln; l++) { /* loops thru stages */
					le = (int)(Math.exp( (double)l * Math.log(2) ) + 0.5 );
					le1 = le / 2;
					u_r = 1.0;
					u_i = 0.0;
					w_r =  Math.cos( Math.PI / (double)le1 );
					w_i = -Math.sin( Math.PI / (double)le1 );
					for (j = 1; j <= le1; j++) { /* loops thru 1/2 twiddle values per stage */
							for (i = j; i <= n; i += le) { /* loops thru points per 1/2 twiddle */
									ip = i + le1;
									t_r = array[ip - 1][0] * u_r - u_i * array[ip - 1][1];
									t_i = array[ip - 1][1] * u_r + u_i * array[ip - 1][0];

									array[ip - 1][0] = array[i - 1][0] - t_r;
									array[ip - 1][1] = array[i - 1][1] - t_i;

									array[i - 1][0] =  array[i - 1][0] + t_r;
									array[i - 1][1] =  array[i - 1][1] + t_i;
							} 
							t_r = u_r * w_r - w_i * u_i;
							u_i = w_r * u_i + w_i * u_r;
							u_r = t_r;
					} 
			}
			return array;
	}

	public static double[][] ifft_1d(  double[][] array ) {
			double  u_r,u_i, w_r,w_i, t_r,t_i;
			int     ln, nv2, k, l, le, le1, j, ip, i, n;

			n = array.length;
			ln = (int)( Math.log( (double)n )/Math.log(2) + 0.5 );
			nv2 = n / 2;
			j = 1;
			for (i = 1; i < n; i++ ) {
					if (i < j) {
							t_r = array[i - 1][0];
							t_i = array[i - 1][1];
							array[i - 1][0] = array[j - 1][0];
							array[i - 1][1] = array[j - 1][1];
							array[j - 1][0] = t_r;
							array[j - 1][1] = t_i;
					}
					k = nv2;
					while (k < j) {
							j = j - k;
							k = k / 2;
					}
					j = j + k;
			}

			for (l = 1; l <= ln; l++) { /* loops thru stages */
					le = (int)(Math.exp( (double)l * Math.log(2) ) + 0.5 );
					le1 = le / 2;
					u_r = 1.0;
					u_i = 0.0;
					w_r =  Math.cos( Math.PI / (double)le1 );
					w_i =  Math.sin( Math.PI / (double)le1 );
					for (j = 1; j <= le1; j++) { /* loops thru 1/2 twiddle values per stage */
							for (i = j; i <= n; i += le) { /* loops thru points per 1/2 twiddle */
									ip = i + le1;
									t_r = array[ip - 1][0] * u_r - u_i * array[ip - 1][1];
									t_i = array[ip - 1][1] * u_r + u_i * array[ip - 1][0];

									array[ip - 1][0] = array[i - 1][0] - t_r;
									array[ip - 1][1] = array[i - 1][1] - t_i;

									array[i - 1][0] =  array[i - 1][0] + t_r;
									array[i - 1][1] =  array[i - 1][1] + t_i;
							} 
							t_r = u_r * w_r - w_i * u_i;
							u_i = w_r * u_i + w_i * u_r;
							u_r = t_r;
					} 
			}
			return array;
	}
}

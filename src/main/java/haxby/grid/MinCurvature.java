package haxby.grid;

public class MinCurvature {
	static final int[] icoeffX =  { 0, -1,  0,  1, -2, -1,  1,  2, -1,  0,  1,  0};
	static final int[] icoeffY =  {-2, -1, -1, -1,  0,  0,  0,  0,  1,  1,  1,  2};
	static final float[] coeff0 = {-1, -2,  8, -2, -1,  8,  8, -1, -2,  8, -2, -1};
	static final float[] coeff2 = { 0,  0,  1,  0,  0,  1,  1,  0,  0,  1,  0,  0};
	public static int solve(float[] z, byte[] mask, 
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
					if( Float.isNaN(z1) )continue;
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
					if( Float.isNaN(z1) )continue;
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
					if( Float.isNaN(z1) )continue;
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
					if( Float.isNaN(z1) )continue;
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
					if( Float.isNaN(z1) )continue;
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

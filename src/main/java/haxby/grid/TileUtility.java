package haxby.grid;

public class TileUtility {
	public static float[] decimate2X(float[] grid, int size) {
		int size2 = size/2;
		float[] z = new float[size2*size2];
		float[] wz = new float[size2*size2];
		double dx, dy, wt, zz;
		double nx, ny, ix, iy;
		double xx, yy;
		int k;
		yy = -.75;
		for( int y=0 ; y<size*size ; y+=size ) {
			yy += .5;
			for( int x=0 ; x<size ; x++ ) {
				xx = -.25 + x*.5d;
				if(Float.isNaN(grid[x+y])) continue;
				dx = 0;
				nx = 0;
				dy = 0;
				ny = 0;
				if(x!=0 && (!Float.isNaN(grid[x+y-1])) ) {
					dx += (double) (grid[x+y]-grid[x+y-1]);
					nx++;
				}
				if(x!=size-1 && (!Float.isNaN(grid[x+y+1])) ) {
					dx += (double) (-grid[x+y]+grid[x+y+1]);
					nx++;
				}
				if(y!=0 && (!Float.isNaN(grid[x+y-size])) ) {
					dy += (double) (grid[x+y]-grid[x+y-size]);
					ny++;
				}
				if(y!=size*(size-1) && (!Float.isNaN(grid[x+y+size])) ) {
					dy += (double) (-grid[x+y]+grid[x+y+size]);
					ny++;
				}
				if(nx==0 || ny==0) continue;

				ix = Math.floor(xx);
				iy = Math.floor(yy);
				if( ix>=0 && iy>=0 ) {
					k = (int)ix + size2*(int)iy;
					wt = 1/Math.sqrt(Math.pow((xx-ix)/nx, 2) + Math.pow((yy-iy)/ny, 2));
					zz = grid[x+y] + 2.*((dx/nx)*(ix-xx) + (dy/ny)*(iy-yy));
					z[k] += (float)(wt*zz);
					wz[k] += (float)wt;
				}
				ix = Math.floor(xx)+1;
				iy = Math.floor(yy);
				if( ix<size2 && iy>=0 ) {
					k = (int)ix + size2*(int)iy;
					wt = 1/Math.sqrt(Math.pow((xx-ix)/nx, 2) + Math.pow((yy-iy)/ny, 2));
					zz = grid[x+y] + 2.*((dx/nx)*(ix-xx) + (dy/ny)*(iy-yy));
					z[k] += (float)(wt*zz);
					wz[k] += (float)wt;
				}
				ix = Math.floor(xx);
				iy = Math.floor(yy)+1;
				if( ix>=0 && iy<size2 ) {
					k = (int)ix + size2*(int)iy;
					wt = 1/Math.sqrt(Math.pow((xx-ix)/nx, 2) + Math.pow((yy-iy)/ny, 2));
					zz = grid[x+y] + 2.*((dx/nx)*(ix-xx) + (dy/ny)*(iy-yy));
					z[k] += (float)(wt*zz);
					wz[k] += (float)wt;
				}
				ix = Math.floor(xx)+1;
				iy = Math.floor(yy)+1;
				if( ix<size2 && iy<size2 ) {
					k = (int)ix + size2*(int)iy;
					wt = 1/Math.sqrt(Math.pow((xx-ix)/nx, 2) + Math.pow((yy-iy)/ny, 2));
					zz = grid[x+y] + 2.*((dx/nx)*(ix-xx) + (dy/ny)*(iy-yy));
					z[k] += (float)(wt*zz);
					wz[k] += (float)wt;
				}
			}
		}
		for( int i=0 ; i<size2*size2 ; i++) {
			if(wz[i]==0) z[i] = Float.NaN;
			else z[i] /= wz[i];
		}
		return z;
	}
	public static float[] decimateSS(float[] grid, int size) {
		int size2 = size/2;
		float[] z = new float[size2*size2];
		float[] wz = new float[size2*size2];
		double dx, dy, wt, zz;
		double nx, ny, ix, iy;
		double xx, yy;
		int k;
		yy = -.75;
		for( int y=0 ; y<size*size ; y+=size ) {
			yy += .5;
			for( int x=0 ; x<size ; x++ ) {
				xx = -.25 + x*.5d;
				if(Float.isNaN(grid[x+y])) continue;
				ix = Math.floor(xx);
				iy = Math.floor(yy);
				if( ix>=0 && iy>=0 ) {
					k = (int)ix + size2*(int)iy;
					z[k] += grid[x+y];
					wz[k] ++;
				}
				ix = Math.floor(xx)+1;
				iy = Math.floor(yy);
				if( ix<size2 && iy>=0 ) {
					k = (int)ix + size2*(int)iy;
					z[k] += grid[x+y];
					wz[k] ++;
				}
				ix = Math.floor(xx);
				iy = Math.floor(yy)+1;
				if( ix>=0 && iy<size2 ) {
					k = (int)ix + size2*(int)iy;
					z[k] += grid[x+y];
					wz[k] ++;
				}
				ix = Math.floor(xx)+1;
				iy = Math.floor(yy)+1;
				if( ix<size2 && iy<size2 ) {
					k = (int)ix + size2*(int)iy;
					z[k] += grid[x+y];
					wz[k] ++;
				}
			}
		}
		for( int i=0 ; i<size2*size2 ; i++) {
			if(wz[i]==0) z[i] = Float.NaN;
			else z[i] /= wz[i];
		}
		return z;
	}
}

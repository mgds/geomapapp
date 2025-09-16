package haxby.grid;

// import java.io.*;
import java.util.Vector;

public class GridMask {
	public static void shaveEdge(float[] z, int width, int height) {
		byte[] mask = initMask(z, width, height);
		for(int i=0 ; i<width*height ; i++) {
			if(mask[i]==2)z[i]=Float.NaN;
		}
	}
	public static byte[] initMask(float[] z, int width, int height) {
		byte[] mask = new byte[width*height];
		for( int i=0 ; i<width*height ; i++) {
			if(Float.isNaN(z[i])) mask[i]=0;
			else mask[i]=1;
		}
		initMask( mask, width, height );
		return mask;
	}
	public static void initMask(byte[] mask, int width, int height) {
		for( int y=width ; y<width*(height-1) ; y+=width) {
			for( int x=y ; x<y+width-1 ; x++) {
				if(mask[x]==1) {
					if(mask[x-1]==0 || mask[x+1]==0 
							|| mask[x-width]==0 
							|| mask[x+width]==0) {
						mask[x]=2;
					}
				}
			}
		}
	}
	public static byte[] maskGrid(float[] z, int width, int height, 
				double radius1, double radius2) {
		byte[] mask = initMask(z, width, height);
		mask0(mask, radius1, width, height);
		mask1(mask, radius2, width, height);
		for( int i=0 ; i<width*height ; i++) {
			if(mask[i]<2)continue;
			if(mask[i]==2)mask[i]=1;
			if(mask[i]<=4)mask[i]=3;
			else mask[i] = 4;
		}
		return mask;
	}
	public static void mask0(byte[] mask, double radius, int width, int height) {
		int ir0 = (int)Math.floor(radius);
		double r0 = radius*radius;
		Vector[] kxy = new Vector[5];
		for(int i=0 ; i<5 ; i++) kxy[i] = new Vector();
		int[] r = new int[5];
		for(int y=-ir0 ; y<=ir0 ; y++) {
			r[0] = y*y;
			r[1] = r[0];
			r[2] = (1+y)*(1+y);
			r[3] = r[2];
			r[4] = r[2];
			for( int x=-ir0 ; x<=ir0 ; x++) {
				int r1 = r[0] + x*x;
				if( (double)r1 <= r0 ) {
					kxy[0].add(new int[] {x, y*width});
					int x2 = (x+1)*(x+1);
					r1 = r[1] + x2;
					if( (double)r1 > r0 ) {
						kxy[1].add(new int[] {x, y*width});
					}
					r1 = r[2] + x2;
					if( (double)r1 > r0 ) {
						kxy[2].add(new int[] {x, y*width});
					}
					r1 = r[3] + x*x;
					if( (double)r1 > r0 ) {
						kxy[3].add(new int[] {x, y*width});
					}
					r1 = r[4] + (x-1)*(x-1);
					if( (double)r1 > r0 ) {
						kxy[4].add(new int[] {x, y*width});
					}
				}
			}
		}
		int wh = width*height;
		int k, ix, iy;
		int[] xy;
		for( int y=width ; y<width*(height-1) ; y+=width) {
			for( int x=1 ; x<width-1 ; x++) {
				if(mask[x+y]==2) {
					if(mask[y+x-1]==2)k=1;
					else if(mask[x+y-width]==2)k=3;
					else if(mask[x+y-width-1]==2)k=2;
					else if(mask[x+y+1-width]==2)k=4;
					else k=0;
					for( int i=0 ; i<kxy[k].size() ; i++) {
						xy = (int[])kxy[k].get(i);
						ix = x+xy[0];
						if(ix<0 || ix>=width)continue;
						iy = y+xy[1];
						if(iy<0 || iy>=wh) continue;
						if(mask[ix+iy]==0) mask[ix+iy]=4;
					}
				}
			}
		}
	}
	public static void mask1(byte[] mask, double radius, int width, int height) {
		int ir0 = (int)Math.floor(radius);
		double r0 = radius*radius;
		Vector[] kxy = new Vector[5];
		for(int i=0 ; i<5 ; i++) kxy[i] = new Vector();
		int[] r = new int[5];
		for(int y=-ir0 ; y<=ir0 ; y++) {
			r[0] = y*y;
			r[1] = r[0];
			r[2] = (1+y)*(1+y);
			r[3] = r[2];
			r[4] = r[2];
			for( int x=-ir0 ; x<=ir0 ; x++) {
				int r1 = r[0] + x*x;
				if( (double)r1 <= r0 ) {
					kxy[0].add(new int[] {x, y*width});
					int x2 = (x+1)*(x+1);
					r1 = r[1] + x2;
					if( (double)r1 > r0 ) {
						kxy[1].add(new int[] {x, y*width});
					}
					r1 = r[2] + x2;
					if( (double)r1 > r0 ) {
						kxy[2].add(new int[] {x, y*width});
					}
					r1 = r[3] + x*x;
					if( (double)r1 > r0 ) {
						kxy[3].add(new int[] {x, y*width});
					}
					r1 = r[4] + (x-1)*(x-1);
					if( (double)r1 > r0 ) {
						kxy[4].add(new int[] {x, y*width});
					}
				}
			}
		}
		int wh = width*height;
		int k, ix, iy;
		int[] xy;
		for( int y=width ; y<width*(height-1) ; y+=width) {
			for( int x=1 ; x<width-1 ; x++) {
				if(mask[x+y]==4) {
					if(mask[x+y-1]==0 || mask[x+y+1]==0 
							|| mask[x+y-width]==0 
							|| mask[x+y+width]==0) {
						mask[x+y]=6;
					}
				}
				if(mask[x+y]!=6) continue;
				if(mask[y+x-1]==6)k=1;
				else if(mask[x+y-width]==6)k=3;
				else if(mask[x+y-width-1]==6)k=2;
				else if(mask[x+y+1-width]==6)k=4;
				else k=0;
				for( int i=0 ; i<kxy[k].size() ; i++) {
					xy = (int[])kxy[k].get(i);
					ix = x+xy[0];
					if(ix<0 || ix>=width)continue;
					iy = y+xy[1];
					if(iy<0 || iy>=wh) continue;
					if(mask[ix+iy]==4) mask[ix+iy]=5;
				}
			}
		}
	}
	public static float[] gridDistance( float[] grid, 
					int w, 
					int h, 
					float radius,
					boolean outward) {
		// Make a parallel grid
		float[] dist = new float[w*h];
		int r = (int)Math.floor( (double)radius );
		
		// Every NaN value in grid is set to 0 in dist
		// every non NaN value in grid is set to 1 in dist
		for(int i=0 ; i<w*h ; i++) {
			if( Float.isNaN(grid[i]) ) {
				if( outward ) dist[i]=1f;
				else dist[i]=0f;
			} else if( outward ) {
				dist[i]=0f;
			} else {
				dist[i]=1f;
			}
		}
		
		// d is a 2 dimensions of r+1
		float[][] d = new float[r+1][r+1];
		for( int x=0 ; x<r+1 ; x++) {
			double rx = (double)(x*x);
			for( int y=0 ; y<r+1 ; y++) {
				double rr = Math.sqrt( rx+y*y )/radius;
				if( rr>1.) rr=1.;
				d[x][y] = (float) rr;
			}
		}
		
		
		int k=0;
		// For each y value
		for( int y=0 ; y<h ; y++) {
			// For each x value
			for( int x=0 ; x<w ; x++, k++) {
				if( !Float.isNaN(grid[k]) ) {
					// If a neighboring cell is a NaN
					if( (x>0 && Float.isNaN(grid[k-1])) ||
							(x<w-1 && Float.isNaN(grid[k+1])) ||
							(y>0 && Float.isNaN(grid[k-w])) ||
							(y<h-1 && Float.isNaN(grid[k+w])) ) {
						// For -radius to radius
						for( int xx=-r ; xx<r ; xx++ ) {
							int xd = (int)Math.abs(xx); // xDistance
							// For -radius to radius
							for( int yy=-r ; yy<r ; yy++ ) {
								// If point lays in grid
								if( xx+x>=0 && xx+x<w && yy+y>=0 && yy+y<h) {
									int yd = (int)Math.abs(yy);
									if( dist[x+xx+w*(y+yy)]>d[xd][yd] ) {
										dist[x+xx+w*(y+yy)] = d[xd][yd];
									}
								}
							}
						}
					}
				}
			}
		}
		return dist;
	}
}

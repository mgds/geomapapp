package org.geomapapp.grid;

import java.util.Vector;
import java.awt.Rectangle;

public class MaskGrid {
	static Vector[] xyFor( double radius ) {
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
					kxy[0].add(new int[] {x, y});
					int x2 = (x+1)*(x+1);
					r1 = r[1] + x2;
					if( (double)r1 > r0 ) {
						kxy[1].add(new int[] {x, y});
					}
					r1 = r[2] + x2;
					if( (double)r1 > r0 ) {
						kxy[2].add(new int[] {x, y});
					}
					r1 = r[3] + x*x;
					if( (double)r1 > r0 ) {
						kxy[3].add(new int[] {x, y});
					}
					r1 = r[4] + (x-1)*(x-1);
					if( (double)r1 > r0 ) {
						kxy[4].add(new int[] {x, y});
					}
				}
			}
		}
		for( int k=0 ; k<5 ; k++) kxy[k].trimToSize();
		return kxy;
	}
	public static Grid2D.Boolean dataMask(Grid2D z, boolean invert) {
		Rectangle b = z.getBounds();
		Grid2D.Boolean mask = new Grid2D.Boolean( b, z.getProjection() );
		for( int y=b.y ; y<b.y+b.height ; y++) {
			for( int x=b.x ; x<b.x+b.width ; x++) {
				mask.setValue( x, y, invert != Double.isNaN( z.valueAt(x,y) ));
			}
		}
		return mask;
	}
	public static Grid2D.Boolean not(Grid2D.Boolean data) {
		Rectangle b = data.getBounds();
		Grid2D.Boolean mask = new Grid2D.Boolean( b, data.getProjection() );
		for( int y=b.y ; y<b.y+b.height ; y++) {
			for( int x=b.x ; x<b.x+b.width ; x++) mask.setValue( x, y, !data.booleanValue(x,y) );
		}
		return mask;
	}
	public static Grid2D.Boolean and(Grid2D.Boolean m1, Grid2D.Boolean m2) {
		Rectangle b = m1.getBounds();
		Grid2D.Boolean mask = new Grid2D.Boolean( b, m1.getProjection() );
		for( int y=b.y ; y<b.y+b.height ; y++) {
			for( int x=b.x ; x<b.x+b.width ; x++) mask.setValue(x,y,m1.booleanValue(x,y)&&m2.booleanValue(x,y));
		}
		return mask;
	}
	public static Grid2D.Boolean or(Grid2D.Boolean m1, Grid2D.Boolean m2) {
		Rectangle b = m1.getBounds();
		Grid2D.Boolean mask = new Grid2D.Boolean( b, m1.getProjection() );
		for( int y=b.y ; y<b.y+b.height ; y++) {
			for( int x=b.x ; x<b.x+b.width ; x++) mask.setValue(x,y,m1.booleanValue(x,y)||m2.booleanValue(x,y));
		}
		return mask;
	}
	public static Grid2D.Boolean edgeMask(Grid2D.Boolean data) {
		Rectangle b = data.getBounds();
		Grid2D.Boolean mask = new Grid2D.Boolean( b, data.getProjection() );
		for( int y=b.y ; y<b.y+b.height ; y++) {
			int yy = y-b.y;
			for( int x=b.x ; x<b.x+b.width ; x++) {
				if( data.booleanValue(x,y) ) {
					int xx = x-b.x;
					mask.setValue( x, y, 
						(xx>0 && !data.booleanValue(x-1,y))
						|| (yy>0 && !data.booleanValue(x,y-1))
						|| (yy<b.height-1 && !data.booleanValue(x,y+1))
						|| (xx<b.width-1 && !data.booleanValue(x+1,y)) );
				}
			}
		}
		return mask;
	}
	public static Grid2D.Boolean distMask(Grid2D.Boolean edge, Grid2D.Boolean data, double radius) {
		Vector[] kxy = xyFor( radius );
		int[] xy;
		int k=0;
		Rectangle b = data.getBounds();
		Grid2D.Boolean mask = new Grid2D.Boolean( b, data.getProjection() );
		for( int y=b.y ; y<b.y+b.height ; y++) {
			for( int x=b.x ; x<b.x+b.width ; x++) {
				if( edge.booleanValue(x,y) ) {
					if( edge.booleanValue(x-1,y) )k=1;
					else if( edge.booleanValue(x,y-1) )k=3;
					else if( edge.booleanValue(x-1,y-1) )k=2;
					else if( edge.booleanValue(x+1,y-1) )k=4;
					else k=0;
					for( int i=0 ; i<kxy[k].size() ; i++) {
						xy = (int[])kxy[k].get(i);
						if( !data.booleanValue(x+xy[0], y+xy[1]) )
							mask.setValue( x+xy[0], y+xy[1], true );
					}
				}
			}
		}
		return mask;
	}
	public static Grid2D.Float distanceFrom ( Grid2D.Boolean data, 
					Grid2D.Boolean edge,
					float radius ) {
		Rectangle b = data.getBounds();
		Grid2D.Float dist = new Grid2D.Float( b, data.getProjection() );
		int r = (int)Math.floor( (double)radius );
		float[][] d = new float[r][r];
		for( int x=0 ; x<r+1 ; x++) {
			double rx = (double)(x*x);
			for( int y=0 ; y<r+1 ; y++) {
				double rr = Math.sqrt( rx+y*y )/radius;
				if( rr>1.) rr=1.;
				d[x][y] = (float) rr;
			}
		}
		int k=0;
		for( int y=b.y ; y<b.y+b.height ; y++) {
			for( int x=b.x ; x<b.x+b.width ; x++) {
				if( data.booleanValue(x,y) ) {
					dist.setValue( x, y, 0f );
				} else {
					dist.setValue( x, y, 1f );
				}
			}
		}
		for( int y=b.y ; y<b.y+b.height ; y++) {
			for( int x=b.x ; x<b.x+b.width ; x++) {
				if( !edge.booleanValue(x,y) ) continue;
				for( int xx=-r ; xx<r ; xx++ ) {
					int xd = (int)Math.abs(xx);
					for( int yy=-r ; yy<r ; yy++ ) {
						if( !data.contains(x+xx,y+yy) )continue;
						if( data.booleanValue(x+xx,y+yy) )continue;
						int yd = (int)Math.abs(yy);
						if( dist.valueAt( x+xx, y+yy )>d[xd][yd] ) {
							dist.setValue( x+xx, y+yy, d[xd][yd] );
						}
					}
				}
			}
		}
		return dist;
	}
}

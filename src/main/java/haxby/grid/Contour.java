package haxby.grid;

import haxby.map.*;
import haxby.proj.*;
import java.util.Vector;
import java.awt.geom.*;
import java.awt.*;

public class Contour implements Overlay {
	XMap map;
	Grid grid;
	Vector contours;
	float[] z;
	int nx, ny;
	Con con;
	boolean show;
	int interval = 0;
	static int[] dx = {0, 1, 1, 0};
	static int[] dy = {0, 0, 1, 1};
	static int[] flag = {2, 4, 8, 16};

	public Contour(XMap map, Grid grid) {
		this.map = map;
		this.grid = grid;
		contours = null;
		show = false;
		if( grid==null ) return;
		z = grid.getGrid();
		Dimension size = grid.getSize();
		nx = size.width;
		ny = size.height;
		interval = 0;
	}
	public void setGrid(Grid g) {
		interval = 0;
		grid = g;
		contours = null;
		z = null;
		show = false;
		if( grid==null ) return;
		z = grid.getGrid();
		Dimension size = grid.getSize();
		nx = size.width;
		ny = size.height;
	}
	float[] getCell( int x, int y ) {
		if( x<0 || x>=nx-1 || y<0 || y>=ny-1) {
			return null;
		}
		x = x+nx*y;
		float[] cell = new float[] {z[x], z[x+1], z[x+nx+1], z[x+nx] };
		for( int k=0 ; k<4 ; k++) if( Float.isNaN( cell[k] ) ) return null;
		return cell;
	}
	public int getInterval() {
		return interval;
	}
	public void contour(int interval) {
		if(grid==null) return;
		this.interval = interval;
		byte[][] flags = new byte[ny][nx];
		for( int y=0 ; y<ny ; y++) {
			for( int x=0 ; x<nx ; x++) flags[y][x] = 0;
		}
		float max = -1000000f;
		float min = -max;
		float g;
		Cell next;
		int kount = 0;
		for( int y=0 ; y<ny ; y++) {
			for( int x=0 ; x<nx ; x++) {
				g = z[x+nx*y];
				if(!Float.isNaN(g)) {
					if(g>max) max = g;
					if(g<min) min = g;
				} else {
					kount++;
					flags[y][x] = 1;
/*
					if( x<nx-1 ) {
						flags[y][x+1] = 1;
						if( y<ny-1 ) {
							flags[y+1][x] = 1;
							flags[y+1][x+1] = 1;
						}
					} else if( y<ny-1 ) {
						flags[y+1][x] = 1;
					}
*/
					if(x>0) {
						flags[y][x-1] = 1;
						if(y>0) {
							flags[y-1][x] = 1;
							flags[y-1][x-1] = 1;
						}
					} else if(y>0) {
						flags[y-1][x] = 1;
					}
				}
			}
		}
		int ic1 = (int)Math.ceil((double)min/(double)interval);
		ic1 *= interval;
		int ic2 = (int)Math.floor((double)max/(double)interval);
		ic2 *= interval;

		int side;
		contours = new Vector();
		for( int ic=ic1 ; ic<=ic2 ; ic+=interval) {
			for( int y=0 ; y<ny-1 ; y++) {
				for( int x=0 ; x<nx-1 ; x++) {
					flags[y][x] &= 1;
				}
			}
			float val = (float)ic;
			for( int y=0 ; y<ny-1 ; y++) {
				int yy = y * nx;
				for( int x=0 ; x<nx-1 ; x++) {
					if( flags[y][x] != 0) continue;
					float[] cell = getCell(x, y);
					if( cell == null) continue;
					if((cell[0] >= val) ^ (cell[1] >= val)) {
						con = new Con(ic);
						lastP=null;
						side = 0;
						next = new Cell(x, y, 0);
						flags[y][x] |= 2;
						while(nextCell(cell, ic, next) != null) {
							cell = getCell(next.x, next.y);
							if( cell == null
									|| (flags[next.y][next.x]&1) == 1) {
								if(con.length() > 1) contours.add(con);
								if( side==2 || getCell(x, y-1 )==null 
									|| (flags[y-1][x]&1)==1 ) break;
								con = new Con(ic);
								side = 2;
								next = new Cell(x, y-1, 2);
							} else if(next.side==0 && next.x==x && next.y==y ) {
								con.close();
								contours.add(con);
								break;
							}
							flags[next.y][next.x] |= flag[next.side];
						}
					}
				}
			}
		}
	}
	Point2D.Double lastP=null;
	public Cell nextCell(float[] g, int z, Cell c) {
		if( g==null ) return null;
		int s1 = c.side;
		int s2 = (s1+1)%4;
		double g1 = (double)g[s1];
		double g2 = (double)g[s2];
		double d = ((double)z - g1) / (g2-g1);
		double x = c.x + dx[s1] + (dx[s2]-dx[s1]) * d;
		double y = c.y + dy[s1] + (dy[s2]-dy[s1]) * d;
		Point2D.Double p = (Point2D.Double) map.getProjection().getMapXY(
				grid.getProjection().getRefXY(
					new Point2D.Double(x,y)));
		if( lastP != null && map.getWrap()>0. ) {
			while( p.x>lastP.x+map.getWrap()*.5 ) p.x -= map.getWrap();
			while( p.x<lastP.x-map.getWrap()*.5 ) p.x += map.getWrap();
		}
		lastP = p;
		con.add(p.x, p.y);
		int center = 0;
		int k;
		for( k=0 ; k<4 ; k++) center += g[k];
		boolean up = (center>=z*4);
		boolean test;
		if( (g[s1]>= z) ^ up ) {
			test = (g[s1]>=z);
			for( k=0 ; k<3 ; k++) {
				s1 = (s1+3)%4;
				if( (g[s1]>=z ) ^ test) {
					c.nextCell(s1);
					return c;
				}
			}
			return null;
		} else {
			test = !(g[s1]>=z);
			for( k=0 ; k<3 ; k++) {
				s1 = s2;
				s2 = (s1+1)%4;
				if( (g[s2]>=z ) ^ test) {
					c.nextCell(s1);
					return c;
				}
			}
			return null;
		}
	}
	public void setVisible( boolean show) {
		this.show = show;
	}
	public boolean isVisible() {
		return show;
	}
	public void draw(Graphics2D g) {
		if(grid==null) return;
		if(contours == null)return;
		if( !show) return;
		double zoom = map.getZoom();
		g.setStroke(new BasicStroke(1.f/(float)zoom));
		int n = contours.size();
		g.setColor(Color.black);
		Rectangle2D bounds = map.getClipRect2D();
		double wrap = map.getWrap();
		for( int i=0 ; i<n ; i++) {
			((Con)contours.get(i)).draw(g, bounds, wrap);
		}
	}
	class Cell {
		public int x, y, side;
		int[] dx = {0, 1, 0, -1};
		int[] dy = {-1, 0, 1, 0};
		public Cell(int x, int y, int s) {
			this.x = x;
			this.y = y;
			side = s;
		}
		public void nextCell(int s) {
			x += dx[s];
			y += dy[s];
			side = (s+2)%4;
		}
	}
	class Con {
		GeneralPath gp;
		int z;
		int n;
		double[] xRange;
		public Con(int z) {
			this.z = z;
			n = 0;
			gp = new GeneralPath();
			xRange = new double[] {0., 0.};
		}
		public int length() { return n; }
		public int getZ() { return z; }
		public void add(double x, double y) {
			if(n == 0) {
				gp.moveTo((float)x, (float)y);
				xRange[0] = xRange[1] = x;
			} else {
				gp.lineTo((float)x, (float)y);
				if(x < xRange[0]) xRange[0] = x;
				if(x > xRange[1]) xRange[1] = x;
			}
			n++;
		}
		public void close() {
			gp.closePath();
		}
		public void draw(Graphics2D g, Rectangle2D bounds, double wrap) {
			if( wrap<=0. ) {
				g.draw(gp);
				return;
			}
			AffineTransform at = g.getTransform();
			double x0 = bounds.getX();
			while( xRange[1]>x0+wrap ) {
				g.translate( -wrap, 0. );
				x0 += wrap;
			}
			while( xRange[1]<x0 ) {
				g.translate( wrap, 0);
				x0 -= wrap;
			}
			double x1 = x0 + bounds.getWidth();
			while( xRange[0]<x1 ) {
				g.draw(gp);
				x1 -= wrap;
				g.translate( wrap, 0. );
			}
			g.setTransform( at );
		}
	}
}

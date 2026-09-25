package org.geomapapp.grid;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Vector;

import haxby.map.XMap;

public class ContourGrid {
	Grid2D grid;
	Grid2DOverlay overlay;
	Vector contours;
	int nx, ny;
	Con con;
	double interval = 0;
	double bolding_interval = 0;
	boolean visible;
	static int[] dx = {0, 1, 1, 0};
	static int[] dy = {0, 0, 1, 1};
	static byte[] flag = {2, 4, 8, 16};
	double scale;
	double x0, y0;
	public ContourGrid(Grid2DOverlay overlay) {
		this.overlay = overlay;
		setGrid(overlay.getGrid());
		interval = -1;
		bolding_interval = -1;
		visible = false;
	}
	public void setGrid(Grid2D grid) {
		if( grid!=this.grid ) {
			contours = new Vector();
			visible=false;
		}
		this.grid = grid;
		if( grid!=null ) {
			scale = overlay.getScale();
			double[] offsets = overlay.getOffsets();
			x0 = offsets[0];
			y0 = offsets[1];
			Rectangle bounds = grid.getBounds();
			x0 -= scale*bounds.x;
			y0 -= scale*bounds.y;
		}
		contours = new Vector();
	}
	public void setVisible(boolean tf) {
		visible = tf;
	}
	public boolean isVisible() {
		return visible;
	}
	public void draw(Graphics2D g) {
		if(!visible)return;
		
		// keep contours visible if plotted, no matter what the opacity
		XMap map = overlay.getMap();
		float alpha = map.getAlpha(overlay);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));
		Stroke oldStroke = g.getStroke();
		float line_width = ((BasicStroke) oldStroke).getLineWidth();
		for( int k=0 ; k<contours.size() ; k++) {
			Con con = (Con)contours.get(k);
			if (bolding_interval > 0 && con.z % bolding_interval == 0) {
				g.setStroke(new BasicStroke(line_width * 2));
			}
			g.draw( con.gp );
			g.setStroke(oldStroke);
		}
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
	}
	double[] getCell( int x, int y ) {
		double[] cell = new double[] {
				grid.valueAt(x, y),
				grid.valueAt(x+1, y),
				grid.valueAt(x+1, y+1),
				grid.valueAt(x, y+1) };
		for( int k=0 ; k<4 ; k++) if( Double.isNaN( cell[k] ) ) return null;
		return cell;
	}
	public double getInterval() {
		return interval;
	}
	public double getBolding() {
		return bolding_interval;
	}
	public void contour(double interval, double bolding_interval, int[] range) {
		if(grid==null) return;
		this.interval = interval;
		this.bolding_interval = bolding_interval;
		Grid2D.Byte flags = new Grid2D.Byte(
				grid.getBounds(), 
				grid.getProjection());
		double max = -1000000f;
		double min = -max;
		double g;
		Cell next = null;
		int kount = 0;
		Rectangle bounds = grid.getBounds();
		for( int y=bounds.y ; y<bounds.y+bounds.height ; y++) {
			for( int x=bounds.x ; x<bounds.x+bounds.width ; x++) {
				g = grid.valueAt(x,y);
				if(!Double.isNaN(g)) {
					if(g>max) max = g;
					if(g<min) min = g;
				} else {
					kount++;
					flags.setValue(x, y, (byte)1);
					flags.setValue(x,y-1,(byte)1);
					flags.setValue(x-1,y-1,(byte)1);
				}
			}
		}
		int ic1 = (int)Math.ceil(min/interval);
		if( range[0]>ic1 )ic1=range[0];
		int ic2 = (int)Math.floor(max/interval);
		if( range[1]<ic2 )ic2=range[1];

		int side;
		contours = new Vector();
		for( int ic=ic1 ; ic<=ic2 ; ic++ ) {
			double val = ic*interval;
			for( int y=bounds.y ; y<bounds.y+bounds.height ; y++) {
				for( int x=bounds.x ; x<bounds.x+bounds.width ; x++) {
					flags.setValue(x,y,(byte)flags.byteValue(x,y)&1);
				}
			}
			for( int y=bounds.y ; y<bounds.y+bounds.height-1 ; y++) {
				for( int x=bounds.x ; x<bounds.x+bounds.width ; x++) {
					if( flags.byteValue(x,y)!=0 )continue;
					double[] cell = getCell(x, y);
					if( cell == null) continue;
					if((cell[0] >= val) ^ (cell[1] >= val)) {
						con = new Con(val);
						lastP=null;
						side = 0;
						next = new Cell(x, y, 0);
						flags.setValue(x,y,(byte)flags.byteValue(x,y)|2);
						while(nextCell(cell, val, next) != null) {	
							cell = getCell(next.x, next.y);
							int test = flags.byteValue(next.x,next.y)&1;
							if( cell == null
									|| test == 1) {
								if(con.length() > 1) contours.add(con);
								test = flags.byteValue(x,y-1)&1;
								if( side==2 || getCell(x, y-1 )==null 
									|| test==1 ) break;
								con = new Con(val);
								side = 2;
								next = new Cell(x, y-1, 2);
							} else if(next.side==0 && next.x==x && next.y==y ) {
								con.close();
								contours.add(con);
								break;
							}
							flags.setValue( next.x, next.y,
								flags.byteValue(next.x, next.y)
								| flag[next.side]);
						}
					}
				}
			}
		}
	}
	Point2D.Double lastP=null;
	public Cell nextCell(double[] g, double z, Cell c) {
		if( g==null ) return null;
		int s1 = c.side;
		int s2 = (s1+1)%4;
		double g1 = (double)g[s1];
		double g2 = (double)g[s2];
		double d = (z - g1) / (g2-g1);
		double x = c.x + dx[s1] + (dx[s2]-dx[s1]) * d;
		double y = c.y + dy[s1] + (dy[s2]-dy[s1]) * d;
		Point2D.Double p = new Point2D.Double(x,y);
		lastP = p;
		con.add(x0 + scale*p.x, y0 + scale*p.y);
		double center = 0;
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
		double z;
		int n;
		double[] xRange;
		public Con(double z) {
			this.z = z;
			n = 0;
			gp = new GeneralPath();
			xRange = new double[] {0., 0.};
		}
		public int length() { return n; }
		public double getZ() { return z; }
		public void add(double x, double y) {
			if(n == 0) {
				gp.moveTo((float)x, (float)y);
				xRange[0] = xRange[1] = x;
			} else {
				gp.lineTo((float)x, (float)y);
				if(x < xRange[0]) xRange[0] = x;
				if(x > xRange[1]) xRange[1] = x;
			}
//	System.out.println( n +"\t"+ z +"\t"+ x  +"\t"+  y);
			n++;
		}
		public void close() {
			gp.closePath();
		}
	}
	public void dispose() {
		 contours.clear();
		 con = null;
		 grid = null;
	}
}

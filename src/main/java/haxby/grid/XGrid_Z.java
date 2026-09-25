package haxby.grid;

import haxby.proj.*;
import java.io.*;
import java.awt.*;
import java.awt.geom.*;

public class XGrid_Z implements Grid, Serializable {
	GridServer gridder;
	Projection proj;
	float[] grid;
	int x0, y0, width, height;
	int wrap=-1;
	XGrid_Z() {
	}
	public XGrid_Z(int x0, int y0, int width, int height, int dec, GridServer gridder) {
		proj = new ScaledProjection( gridder.getProjection(),
				2., (double)x0 + .5, (double)y0 + .5);
		if( gridder.getProjection() instanceof CylindricalProjection ) {
			double lon0 = proj.getRefXY( new Point(0,0) ).getX();
			double lon1 = proj.getRefXY( new Point(1,0) ).getX();
			wrap = (int) Math.rint( 360./(lon1-lon0) );
		} else {
			wrap = -1;
		}
		this.x0 = x0;
		this.y0 = y0;
		this.width = width/2;
		this.height = height/2;
		this.gridder = gridder;
		grid = new float[this.height*this.width];
		int k=0;
		XGrid_ZW xgrid;
		float h, wt;
		for( int y=y0 ; y<y0+this.height*2 ; y+=2) {
			for(int x=x0 ; x<x0+this.width*2 ; x+=2, k++) {
				grid[k]=0f;
				float ww = 0f;
				for(int iy=y ; iy<y+2 ; iy++) {
					for(int ix=x ; ix<x+2 ; ix++) {
						double z = gridder.valueAt(ix, iy);
						if( !Double.isNaN(z) ) {
							grid[k] += (float) z;
							ww ++;
						}
					}
				}
				if( ww==0f ) grid[k] = Float.NaN;
				else grid[k] /= ww;
			}
		}
	}
	public XGrid_Z(int x0, int y0, int width, int height, GridServer gridder) {
		proj = new ScaledProjection( gridder.getProjection(),
				1., (double)x0, (double)y0);
		if( gridder.getProjection() instanceof CylindricalProjection ) {
			double lon0 = proj.getRefXY( new Point(0,0) ).getX();
			double lon1 = proj.getRefXY( new Point(1,0) ).getX();
			wrap = (int) Math.rint( 360./(lon1-lon0) );
		} else {
			wrap = -1;
		}
		this.x0 = x0;
		this.y0 = y0;
		this.width = width;
		this.height = height;
		this.gridder = gridder;
		grid = new float[height*width];
		int k=0;
/*
		for( int y=y0 ; y<y0+height ; y++) {
			for(int x=x0 ; x<x0+width ; x++, k++) {
				double z = gridder.valueAt(x, y);
				if( Double.isNaN(z) ) grid[k] = Float.NaN;
				else grid[k] = (float) z;
			}
		}
*/
		int tileX1 = x0/320;
		if( x0<0 ) tileX1--;
		int tileX2 = (x0+width-1)/320;
		if( x0+width-1<0 ) tileX2--;
		int tileY1 = y0/320;
		if( y0<0 ) tileY1--;
		int tileY2 = (y0+height-1)/320;
		if( y0+height-1<0 ) tileY2--;
		for( int tileX=tileX1 ; tileX<=tileX2 ; tileX++ ) {
			int x1 = tileX*320;
			int x2 = x1+319;
			if( x1<x0 ) x1=x0;
			if( x2>x0+width-1 ) x2=x0+width-1;
			int xOffset = x1-x0;
			for( int tileY=tileY1 ; tileY<=tileY2 ; tileY++ ) {
				int y1 = tileY*320;
				int y2 = y1+319;
				if( y1<y0 ) y1=y0;
				if( y2>y0+height-1 ) y2=y0+height-1;
				int offset = xOffset+ width*(y1-y0);
				for( int y=y1 ; y<=y2 ; y++, offset+=width ) {
					k = offset;
					for( int x=x1 ; x<=x2 ; x++,k++ ) {
						grid[k] = (float)gridder.valueAt(x, y);
					}
				}
			}
		}
	}
	public static void filegrid(int x0, int y0, int width, int height, GridServer gridder,
			String file) throws IOException {
		DataOutputStream out = new DataOutputStream(
			new BufferedOutputStream(
			new FileOutputStream(file)));
		Projection proj = new ScaledProjection( gridder.getProjection(),
				1., (double)x0, (double)y0);
		out.writeInt( width );
		out.writeInt( height );
		out.writeInt( x0 );
		out.writeInt( y0 );
		Point2D p0 = proj.getRefXY(new Point2D.Double(0.,0.));
		Point2D p1 = proj.getRefXY(new Point2D.Double(1.,0.));
		int wrap = (int) Math.rint( 360./(p1.getX()-p0.getX()) );
		out.writeInt( wrap );
		float[] grid = new float[width];
		int k=0;
		float h, wt;
		for( int y=y0 ; y<y0+height ; y++) {
			k=0;
			for(int x=x0 ; x<x0+width ; x++, k++) {
				double z = gridder.valueAt(x, y);
				if( Double.isNaN(z) ) grid[k] = Float.NaN;
				else grid[k] = (float) z;
			}
			int i=0;
			while(i<grid.length) {
				int n=0;
				while( i<grid.length && Float.isNaN(grid[i]) ) {
					n++;
					i++;
				}
				out.writeInt(n);
				if( i>=grid.length ) break;
				n=0;
				while( i+n<grid.length && !Float.isNaN(grid[i+n]) ) n++;
				out.writeInt(n);
				for( int j=0 ; j<n ; j++) {
					out.writeFloat(grid[i]);
					i++;
				}
			}
		}
		out.close();
	}
	public Projection getProjection() {
		return proj;
	}
	public double[] getLonLatBounds() {
		if( gridder.getProjection() instanceof CylindricalProjection ) {
			Point p0 = new Point(0, 0);
			Point2D ul = proj.getRefXY( p0 );
			p0.x = width-1;
			p0.y = height-1;
			Point2D lr = proj.getRefXY( p0 );
			double[] bounds = new double[] {
				ul.getX(), lr.getX(),
				lr.getY(), ul.getY() };
			return bounds;
		} 
		double[] bounds = new double[4];
		Point p0 = new Point(0, 0);
		Point2D p = proj.getRefXY( p0 );
		bounds[0] = bounds[1] = p.getX();
		bounds[2] = bounds[3] = p.getY();
		p0.y = 0;
		for( int x=1 ; x<width ; x++) {
			p0.x = x;
			p = proj.getRefXY( p0 );
			bounds[0] = Math.min( p.getX(), bounds[0] );
			bounds[1] = Math.max( p.getX(), bounds[1] );
			bounds[2] = Math.min( p.getY(), bounds[2] );
			bounds[3] = Math.max( p.getY(), bounds[3] );
		}
		p0.y = height-1;
		for( int x=0 ; x<width ; x++) {
			p0.x = x;
			p = proj.getRefXY( p0 );
			bounds[0] = Math.min( p.getX(), bounds[0] );
			bounds[1] = Math.max( p.getX(), bounds[1] );
			bounds[2] = Math.min( p.getY(), bounds[2] );
			bounds[3] = Math.max( p.getY(), bounds[3] );
		}
		p0.x = 0;
		for( int y=1 ; y<height-1 ; y++) {
			p0.y = y;
			p = proj.getRefXY( p0 );
			bounds[0] = Math.min( p.getX(), bounds[0] );
			bounds[1] = Math.max( p.getX(), bounds[1] );
			bounds[2] = Math.min( p.getY(), bounds[2] );
			bounds[3] = Math.max( p.getY(), bounds[3] );
		}
		p0.x = width-1;
		for( int y=1 ; y<height-1 ; y++) {
			p0.y = y;
			p = proj.getRefXY( p0 );
			bounds[0] = Math.min( p.getX(), bounds[0] );
			bounds[1] = Math.max( p.getX(), bounds[1] );
			bounds[2] = Math.min( p.getY(), bounds[2] );
			bounds[3] = Math.max( p.getY(), bounds[3] );
		}
		if( gridder.getProjection() instanceof PolarProjection ) {
			if( ((PolarProjection)gridder.getProjection()).getHemisphere() == Projection.NORTH ) {
				p = proj.getMapXY( new Point(0, 90 ) );
				if( contains( p.getX(), p.getY()) ) {
					bounds[3] = 90.;
					bounds[0] = 0.;
					bounds[1] = 360.;
				}
			} else {
				p = proj.getMapXY( new Point(0, -90 ) );
				if( contains( p.getX(), p.getY()) ) {
					bounds[2] = -90.;
					bounds[0] = 0.;
					bounds[1] = 360.;
				}
			}
		}
		return bounds;
	}
	public float[] getGrid() {
		return grid;
	}
	public int[] getBounds() {
		return new int[] {x0, y0, x0+width, y0+height};
	}
	public Dimension getSize() {
		return new Dimension(width, height);
	}
	public float valueAtRef( double refX, double refY) {
		Point2D.Double p = (Point2D.Double) proj.getMapXY( new Point2D.Double(refX, refY) );
		if( wrap>0 ) {
			while( p.x<0. ) p.x+=wrap;
			while( p.x>wrap-1 ) p.x-=wrap;
		}
		return valueAt(p.x, p.y);
	}
	public float valueAt( int ix, int iy) {
		if(ix<0 || ix>width-1 )return Float.NaN;
		if(iy<0 || iy>height-1 )return Float.NaN;
		int i = ix + width*iy;
		return grid[i];
	}
	public boolean contains( double x, double y) {
		if(x<0 || x>width-1 || y<0 || y>height-1) return false;
		return true;
	}
	public float valueAt( double x, double y) {
		if( wrap>0 ) {
			while( x<0. ) x+=wrap;
			while( x>wrap-1 ) x-=wrap;
		}
		double z = Interpolate.bicubic( grid, width, height, x, y );
		if( !Double.isNaN(z) ) return (float)z;
		int ix = (int)Math.floor(x);
		if(ix<0 || ix>width-2 )return Float.NaN;
		int iy = (int)Math.floor(y);
		if(iy<0 || iy>height-2 )return Float.NaN;
		int i = ix + width*iy;
		if( Float.isNaN(grid[i]) || Float.isNaN(grid[i+1])
				|| Float.isNaN(grid[i+width]) 
				|| Float.isNaN(grid[i+1+width]) )return Float.NaN;
		float dx = (float) (x -(double)ix);
		float dy = (float) (y -(double)iy);
		float dxy = dx*dy;
		return grid[i]*(1f-dx-dy+dxy) + grid[i+1]*(dx-dxy) +
				grid[i+width]*(dy-dxy) + grid[i+width+1]*dxy;
	}
	public static XGrid_Z padGrid(XGrid_ZW xgrid, int pad, GridServer gridder) {
		int[] wesn = xgrid.getBounds();
		int x0 = wesn[0] - pad;
		int y0 = wesn[1] - pad;
		int width = wesn[2]-wesn[0] + 2*pad;
		int height = wesn[3]-wesn[1] + 2*pad;
		return new XGrid_Z(x0, y0, width, height, gridder);
	}
	public XGrid_Z decimate() {
		int w = (width-1)/2;
		int h = (height-1)/2;
		XGrid_Z gz = new XGrid_Z();
		gz.width = w;
		gz.height = h;
	//	gz.x0 = x0+1;
	//	gz.y0 = y0+1;
		gz.x0 = x0+1;
		gz.y0 = y0+1;
		gz.gridder = gridder;
		gz.proj = new ScaledProjection( proj,
				2., 1., 1.);
		float[] g = new float[w*h];
		int k=0;
		float ht, wt;
		for( int y=1 ; y<height-1 ; y+=2) {
			for(int x=1 ; x<width-1 ; x+=2, k++) {
				int i = y*width+x;
				ht = wt = 0f;
				if( !Float.isNaN(grid[i]) ) {
					ht += grid[i];
					wt += 1f;
				}
				if( !Float.isNaN(grid[i-width]) && !Float.isNaN(grid[i+width])) {
					ht += .3f*grid[i-width] + .3f*grid[i+width];;
					wt += .6f;
				}
				if( !Float.isNaN(grid[i-1]) && !Float.isNaN(grid[i+1]) ) {
					ht += .3f*grid[i-1] + .3f*grid[i+1];
					wt += .6f;
				}
				if( !Float.isNaN(grid[i-width-1]) && !Float.isNaN(grid[i+width+1])) {
					ht += .1f*grid[i-width-1] + .1f*grid[i+width+1];
					wt += .2f;
				}
				if( !Float.isNaN(grid[i+width-1]) && !Float.isNaN(grid[i-width+1]) ) {
					ht += .1f*grid[i+width-1] + .1f*grid[i-width+1];
					wt += .2f;
				}
				if( wt!=0f ) g[k]=ht/wt;
				else g[k] = Float.NaN;
			}
		}
		gz.grid = g;
		return gz;
	}
	public XGrid decimate(int dec) {
		int w = width/dec + 1;
		int h = height/dec + 1;
		float[] z = new float[h*w];
		float[] wz = new float[h*w];
		double d = 1/(double)dec;
		double dx, dy, wt, zz;
		double nx, ny, ix, iy;
		double xx;
		double yy = .5*d-d;
		int k;
		for( int y=0 ; y<width*height ; y+=width ) {
			yy += d;
			xx = .5*d-d;
			for( int x=0 ; x<width ; x++ ) {
				xx += d;
				if(Float.isNaN(grid[x+y])) continue;
				dx = 0;
				nx = 0;
				dy = 0;
				ny = 0;
				if(x!=0 && (!Float.isNaN(grid[x+y-1])) ) {
					dx += (double) (grid[x+y]-grid[x+y-1]);
					nx++;
				}
				if(x!=width-1 && (!Float.isNaN(grid[x+y+1])) ) {
					dx += (double) (-grid[x+y]+grid[x+y+1]);
					nx++;
				}
				if(y!=0 && (!Float.isNaN(grid[x+y-width])) ) {
					dy += (double) (grid[x+y]-grid[x+y-width]);
					ny++;
				}
				if(y!=width*(height-1) && (!Float.isNaN(grid[x+y+width])) ) {
					dy += (double) (-grid[x+y]+grid[x+y+width]);
					ny++;
				}
				if(nx!=0 && ny!=0) try {
					ix = Math.floor(xx);
					iy = Math.floor(yy);
					k = (int)ix + w*(int)iy;
					wt = 1/Math.sqrt(Math.pow((xx-ix)/nx, 2) + Math.pow((yy-iy)/ny, 2));
					zz = grid[x+y] + dec*((dx/nx)*(ix-xx) + (dy/ny)*(iy-yy));
					z[k] += (float)(wt*zz);
					wz[k] += (float)wt;
					ix = Math.floor(xx)+1;
					iy = Math.floor(yy);
					k = (int)ix + w*(int)iy;
					wt = 1/Math.sqrt(Math.pow((xx-ix)/nx, 2) + Math.pow((yy-iy)/ny, 2));
					zz = grid[x+y] + dec*((dx/nx)*(ix-xx) + (dy/ny)*(iy-yy));
					z[k] += (float)(wt*zz);
					wz[k] += (float)wt;
					ix = Math.floor(xx);
					iy = Math.floor(yy)+1;
					k = (int)ix + w*(int)iy;
					wt = 1/Math.sqrt(Math.pow((xx-ix)/nx, 2) + Math.pow((yy-iy)/ny, 2));
					zz = grid[x+y] + dec*((dx/nx)*(ix-xx) + (dy/ny)*(iy-yy));
					z[k] += (float)(wt*zz);
					wz[k] += (float)wt;
					ix = Math.floor(xx)+1;
					iy = Math.floor(yy)+1;
					k = (int)ix + w*(int)iy;
					wt = 1/Math.sqrt(Math.pow((xx-ix)/nx, 2) + Math.pow((yy-iy)/ny, 2));
					zz = grid[x+y] + dec*((dx/nx)*(ix-xx) + (dy/ny)*(iy-yy));
					z[k] += (float)(wt*zz);
					wz[k] += (float)wt;
				} catch(ArrayIndexOutOfBoundsException ex) {
				//	System.out.println(xx +"\t"+ yy +"\t"+ w +"\t"+ h);
				}
			}
		}
		for( int i=0 ; i<w*h ; i++) {
			if(wz[i]==0) z[i] = Float.NaN;
			else z[i] /= wz[i];
		}
		return new XGrid((double)x0-.5*d, (double)y0-.5*d, w, h, d, gridder.getProjection(), z);
	}
	public void save(String fileName) throws IOException {
		DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(
				new FileOutputStream( fileName )));
		out.writeInt( width );
		out.writeInt( height );
		out.writeInt( x0 );
		out.writeInt( y0 );
		Point2D p0 = proj.getRefXY(new Point2D.Double(0.,0.));
		Point2D p1 = proj.getRefXY(new Point2D.Double(1.,0.));
		int wrap = (int) Math.rint( 360./(p1.getX()-p0.getX()) );
		out.writeInt( wrap );
		int i=0;
		int n=0;
		while(i<grid.length) {
			n=0;
			while( i<grid.length && Float.isNaN(grid[i]) ) {
				n++;
				i++;
			}
			out.writeInt(n);
			if( i>=grid.length ) break;
			n=0;
			while( i+n<grid.length && !Float.isNaN(grid[i+n]) ) n++;
			out.writeInt(n);
			for( int k=0 ; k<n ; k++) {
				out.writeFloat(grid[i]);
				i++;
			}
		}
		out.close();
	}
	public static XGrid_Z readGrid(String fileName) throws IOException {
		DataInputStream in = new DataInputStream(
			new BufferedInputStream(
			new FileInputStream( fileName ) ));
		XGrid_Z g = new XGrid_Z();
		g.width = in.readInt();
		g.height = in.readInt();
		g.x0 = in.readInt();
		g.y0 = in.readInt();
		int wrap = in.readInt();
		Mercator merc = ProjectionFactory.getMercator(wrap);
		g.proj = new ScaledProjection( merc, 1., (double)g.x0, (double)g.y0 );
		int i=0;
		int n;
		g.grid = new float[g.width*g.height];
		for( n=0 ; n<g.grid.length ; n++) g.grid[n]=Float.NaN;
		while( i<g.grid.length ) {
			n = in.readInt();
			i+=n;
			if( i<g.grid.length ) {
				n = in.readInt();
				for( int k=0 ; k<n ; k++) {
					g.grid[i] = in.readFloat();
					i++;
				}
			}
		}
		in.close();
		return g;
	}
}

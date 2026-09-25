package org.geomapapp.image;

import org.geomapapp.grid.*;
import org.geomapapp.geom.*;
import org.geomapapp.util.*;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

public class PerspectiveImage extends ImageComponent 
				implements Runnable {
	Grid2D grid;
	BufferedImage map;
	Perspective3D pers;
	Rectangle bounds;
	double ve;
	double[][] xyz;
	XYZ[] corners;
	int[][] xy;
	float[][] color;
	PerspectiveGeometry tool;
	Grid2D.Float zBuf;
	Thread thread;
	Grid2D grd;
	boolean running = false;
	int background = 0xff000000;
	public PerspectiveImage(Grid2D grid, PerspectiveGeometry tool) {
		setLayout(null);
		this.tool = tool;
		this.grid = grid;
		ve = 1.;
		c1 = new double[2];
		c2 = new double[2];
		width = 500;
		height = 300;
	}
	public void setGrid(Grid2D grid) {
		vec=null;
		this.grid = grid;
		width = 400;
		height = 300;
	}
	public Grid2D getGrid() {
		return grid;
	}
	public void setVE(double ve) {
		if( ve!=this.ve )vec=null;
		this.ve = ve;
	}
	public boolean run( Perspective3D pers, Rectangle bounds,
			Grid2D grid, BufferedImage map, double ve ) {
		if(thread!=null && thread.isAlive())return false;
		setVE(ve);
		this.pers = pers;
		this.bounds = bounds;
		grd = grid;
		this.map = map;
		thread = new Thread(this);
		thread.start();
		running = true;
		return true;
	}
	public void run() {
		render4( pers, bounds, grd, map);
		this.map = null;
		repaint();
	}
	public void paintComponent(Graphics g) {
		if(image==null) {
			Dimension d = getSize();
			Rectangle r = new Rectangle( -d.width/2, -d.height/2,
					d.width, d.height);
		//	render4( tool.getPerspective(), r, grid, map);
		}
		super.paintComponent(g);
	}
	XYZ[][] vec;
	void doXYZ() {
		Rectangle bounds = grid.getBounds();
		MapProjection proj = grid.getProjection();
		int width = bounds.width/4;
		int height = bounds.height/4;
		vec = new XYZ[height][width];
		for( int y=0 ; y<height ; y++) {
			int yy = y*4+bounds.y;;
			for(int x=0 ; x<width ; x++) {
				int xx = x*4+bounds.x;
				double z = grid.valueAt(xx,yy);
				if( Double.isNaN(z) ) {
					vec[y][x]=null;
					continue;
				}
				Point2D pt = proj.getRefXY(
					new Point(xx,yy));
				GCPoint p = new GCPoint(
					pt.getX(), pt.getY(), z*ve);
				vec[y][x] = p.getXYZ();
			}
		}
	}
	public void render4( Perspective3D pers, Rectangle bounds,
			Grid2D grid, BufferedImage map ) {
		boolean t = thread!=null && thread.isAlive();
		if( map==null )return;
		this.map = map;
		width = bounds.width;
		height = bounds.height;
		if( width<=0 || height <=0 )return;
		if( grid != this.grid) {
			this.grid = grid;
			vec=null;
		}
		if(vec==null) doXYZ();
		corners = null;
		Rectangle gridBounds = grid.getBounds();
		int w = gridBounds.width/4;
		int h = gridBounds.height/4;
		if( vec.length!=h || vec[0].length!=w) {
			doXYZ();
		}
		if( vec.length<2 || vec[0].length<2 )return;
		this.pers = pers;
		this.bounds = bounds;
		image = new BufferedImage(bounds.width,
					bounds.height,
					BufferedImage.TYPE_INT_RGB);
	//	Graphics2D g = (Graphics2D)image.createGraphics();
	//	g.setColor( new Color( 75, 75, 120);
		zBuf = new Grid2D.Float( 
			new Rectangle(0,0,bounds.width, bounds.height), 
			null);
		xyz = new double[3][3];
	//	xyz = new double[3][2];
		for( int y=0 ; y<vec.length-1 ; y++) {
			for( int x=0 ; x<vec[0].length-1 ; x++) {
				if( t!=running ) {
					running = false;
					return;
				}
				if( x*4+1>=map.getWidth()||y*4+1>=map.getHeight() )continue;
				int n=0;
				XYZ v = pers.forward(vec[y][x]);
				if( v!=null ) {
					xyz[n] = new double[] {
						(v.x-bounds.x), 
						(v.y-bounds.y),
						v.z }; 
					n++;
				}
				v = pers.forward(vec[y][x+1]);
				if( v!=null ) {
					xyz[n] = new double[] {
						(v.x-bounds.x), 
						(v.y-bounds.y),
						v.z }; 
					n++;
				}
				if( n==0 )continue;
				v = pers.forward(vec[y+1][x+1]);
				if( v!=null ) {
					xyz[n] = new double[] {
						(v.x-bounds.x), 
						(v.y-bounds.y),
						v.z }; 
					n++;
				}
				if( n<2 )continue;
				int rgb = map.getRGB(x*4+1, y*4+1);
				if( n==3 ) {
					fill( rgb, zBuf );
					xyz[1] = xyz[2];
					n = 2;
				}
				v = pers.forward(vec[y+1][x]);
				if( v!=null ) {
					xyz[n] = new double[] {
						(v.x-bounds.x), 
						(v.y-bounds.y),
						v.z }; 
					n++;
				}
				if( n==3 ) fill(rgb, zBuf);
			}
		}
		this.map = null;
		running = false;
	}
	public Grid2D.Float render( Perspective3D pers, 
			Rectangle bounds,
			Grid2D grid, 
			BufferedImage map,
			boolean quality ) {
		width = bounds.width;
		height = bounds.height;
		this.map = map;
		this.grid = grid;
		this.pers = pers;
		this.bounds = bounds;
		image = new BufferedImage(bounds.width,
					bounds.height,
					BufferedImage.TYPE_INT_RGB);
		Grid2D.Float zBuf = new Grid2D.Float( 
			new Rectangle(0,0,bounds.width,bounds.height), 
			null);
		Rectangle rect = grid.getBounds();
		MapProjection proj = grid.getProjection();
		xyz = new double[3][3];
		corners = new XYZ[3];
		xy = new int[3][2];
		color = new float[3][3];
		double[][] xTrig = null;
		double[][] yTrig = null;
		if( proj instanceof CylindricalProjection ) {
			CylindricalProjection prj = (CylindricalProjection)proj;
			xTrig = new double[rect.width][2];
			yTrig = new double[rect.height][2];
			for( int y=0 ; y<rect.height ; y++) {
				double lat = prj.getLatitude(
						(double)(y+rect.getY()) );
				lat = Math.toRadians(lat);
				yTrig[y][0] = Math.cos(lat);
				yTrig[y][1] = Math.sin(lat);
			}
			for( int x=0 ; x<rect.width ; x++) {
				double lon= prj.getLongitude(
						(double)(x+rect.getX()));
				lon = Math.toRadians(lon);
				xTrig[x][0] = Math.cos(lon);
				xTrig[x][1] = Math.sin(lon);
			}
		} else if( proj instanceof haxby.proj.CylindricalProjection ) {
			haxby.proj.CylindricalProjection prj = 
				(haxby.proj.CylindricalProjection)proj;
			xTrig = new double[rect.width][2];
			yTrig = new double[rect.height][2];
			for( int y=0 ; y<rect.height ; y++) {
				double lat = prj.getLatitude(
						(double)(y+rect.getY()) );
				lat = Math.toRadians(lat);
				yTrig[y][0] = Math.cos(lat);
				yTrig[y][1] = Math.sin(lat);
			}
			for( int x=0 ; x<rect.width ; x++) {
				double lon= prj.getLongitude(
						(double)(x+rect.getX()));
				lon = Math.toRadians(lon);
				xTrig[x][0] = Math.cos(lon);
				xTrig[x][1] = Math.sin(lon);
			}
		}
		for( int y=0 ; y<rect.height-2 ; y++) {
			int yy = y+rect.y;
			for( int x=0 ; x<rect.width-2 ; x++) {
				int xx = x+rect.x;
				int n=0;
				double z = grid.valueAt(xx,yy);
				XYZ corner = null;
				if( !Double.isNaN(z) ) {
					XYZ v = null;
					if( xTrig==null ) {
						Point2D pt = proj.getRefXY(
							new Point(xx,yy));
						GCPoint p = new GCPoint(
							pt.getX(), pt.getY(), z*ve);
						corner = p.getXYZ();
						v = pers.forward( corner );
					} else {
						corner = getXYZ(xTrig[x], yTrig[y], z*ve);
						v = pers.forward( corner );
					}
					if( v!=null ) {
						corners[n] = (corner);
						xyz[n] = new double[] {
							(v.x-bounds.x), 
							(v.y-bounds.y),
							v.z }; 
						xy[n] = new int[] {x, y};
					//	color[n] = getRGB(x, y, color[n]);
						n++;
					}
				}
				z = grid.valueAt(xx+1,yy);
				if( !Double.isNaN(z) ) {
					XYZ v = null;
					if( xTrig==null ) {
						Point2D pt = proj.getRefXY(
							new Point(xx+1,yy));
						GCPoint p = new GCPoint(
							pt.getX(), pt.getY(), z*ve);
						corner = p.getXYZ();
						v = pers.forward( corner );
					} else {
						corner = getXYZ(xTrig[x+1], yTrig[y], z*ve);
						v = pers.forward( corner );
					}
					if( v!=null ) {
						corners[n] = (corner);
						xyz[n] = new double[] {
							(v.x-bounds.x), 
							(v.y-bounds.y),
							v.z }; 
						xy[n] = new int[] {x+1, y};
					//	color[n] = getRGB(x+1, y, color[n]);
						n++;
					}
				}
				if( n==0 )continue;
				z = grid.valueAt(xx+1,yy+1);
				if( !Double.isNaN(z) ) {
					XYZ v = null;
					if( xTrig==null ) {
						Point2D pt = proj.getRefXY(
							new Point(xx+1,yy+1));
						GCPoint p = new GCPoint(
							pt.getX(), pt.getY(), z*ve);
						corner = p.getXYZ();
						v = pers.forward( corner );
					} else {
						corner = getXYZ(xTrig[x+1], yTrig[y+1], z*ve);
						v = pers.forward( corner );
					}
					if( v!=null ) {
						corners[n] = (corner);
						xyz[n] = new double[] {
							(v.x-bounds.x), 
							(v.y-bounds.y),
							v.z }; 
						xy[n] = new int[] {x+1, y+1};
					//	color[n] = getRGB(x+1, y+1, color[n]);
						n++;
					}
				}
				if( n<2 )continue;
				int rgb = map.getRGB(x, y);
				if( n==3 ) {
					if(quality) {
						fill( zBuf );
					} else {
						fill( rgb, zBuf );
					}
					xyz[1] = xyz[2];
					xy[1] = xy[2];
					corners[1]=corners[2];
					n = 2;
				}
				z = grid.valueAt(xx,yy+1);
				if( !Double.isNaN(z) ) {
					XYZ v = null;
					if( xTrig==null ) {
						Point2D pt = proj.getRefXY(
							new Point(xx,yy+1));
						GCPoint p = new GCPoint(
							pt.getX(), pt.getY(), z*ve);
						corner = p.getXYZ();
						v = pers.forward( corner );
					} else {
						corner = getXYZ(xTrig[x], yTrig[y+1], z*ve);
						v = pers.forward( corner );
					}
					if( v!=null ) {
						corners[n] = (corner);
						xyz[n] = new double[] {
							(v.x-bounds.x), 
							(v.y-bounds.y),
							v.z }; 
						xy[n] = new int[] {x, y+1};
					//	color[n] = getRGB(x, y+1, color[n]);
						n++;
					}
				}
				if( n==3 ) {
					if(quality) {
						fill( zBuf );
					} else {
						fill( rgb, zBuf );
					}
				}
			}
		}
		this.map = null;
		return zBuf;
	}
	boolean testNormal() {
		if( corners==null )return true;
		XYZ n = corners[1].minus(corners[0]).cross(corners[2].minus(corners[1]));
//	System.out.println( n.x +"\t"+ n.y +"\t"+ n.z +"\t"+ 
//			corners[0].x +"\t"+ corners[0].y +"\t"+ corners[0].z 
//			+"\t"+ (n.dot(pers.minusVP(corners[0])))); 
		return (n.dot(pers.minusVP(corners[0]))>0.);
//		return true;
	}
	XYZ getXYZ( double[] xt, double[] yt, double z) {
		double r = 1.+z/GCTP_Constants.major[0];
		return new XYZ( yt[0]*xt[0]*r,
				yt[0]*xt[1]*r,
				yt[1]*r);
	}
	void fill(Grid2D.Float zBuf) {
		boolean inside=false;
		for( int k=0 ; k<3 ; k++) {
			if( xyz[k][0]<0 ||xyz[k][0]>bounds.width)continue;
			if( xyz[k][1]<0 ||xyz[k][1]>bounds.height)continue;
			inside = true;
			break;
		}
		if( !inside ) return;
		if( !testNormal() )return;
		if( !initTriangleXY() ) return;
		int[][] segs = PolygonFill.fill(xyz);
		for( int k=0 ; k<segs.length ; k++) {
			int iy = segs[k][0];
			if( iy<0||iy>=bounds.height )continue;
			int x1 = (int)Math.max(0,segs[k][1]);
			int x2 = (int)Math.min(bounds.width,segs[k][2]);
			for( int ix=x1 ; ix<x2 ; ix++) {
				float dist = (float)getDist(ix, iy);
				float z = zBuf.floatValue(ix, iy);
				if( Float.isNaN(z) || z>dist ) {
					zBuf.setValue(ix, iy, dist);
				//	getColor(ix, iy);
					image.setRGB(ix, iy, getColor(ix, iy));
				}
			}
		}
	}
	void fill(int rgb, Grid2D.Float zBuf) {
		if( !initTriangle() ) return;
		boolean inside=false;
		for( int k=0 ; k<3 ; k++) {
			if( xyz[k][0]<0 ||xyz[k][0]>bounds.width)continue;
			if( xyz[k][1]<0 ||xyz[k][1]>bounds.height)continue;
			inside = true;
			break;
		}
		if( !inside ) return;
		if( !testNormal() )return;
		int[][] segs = PolygonFill.fill(xyz);
		for( int k=0 ; k<segs.length ; k++) {
			int iy = segs[k][0];
			if( iy<0||iy>=bounds.height )continue;
			int x1 = (int)Math.max(0,segs[k][1]);
			int x2 = (int)Math.min(bounds.width,segs[k][2]);
			for( int ix=x1 ; ix<x2 ; ix++) {
				float dist = (float)getDist(ix, iy);
				float z = zBuf.floatValue(ix, iy);
				if( Float.isNaN(z) || z>dist ) {
					zBuf.setValue(ix, iy, dist);
					image.setRGB(ix, iy, rgb);
				}
			}
		}
	}
	double a1, a2;
	double[] c1, c2;
	boolean initTriangle() {
		if( xyz==null || xyz.length<2 )return false;
		double dx1 = xyz[1][0]-xyz[0][0];
		double dy1 = xyz[1][1]-xyz[0][1];
		double dz1 = xyz[1][2]-xyz[0][2];
		double dx2 = xyz[2][0]-xyz[0][0];
		double dy2 = xyz[2][1]-xyz[0][1];
		double dz2 = xyz[2][2]-xyz[0][2];
		double den = dx1*dy2 - dx2*dy1;
		if( den==0f ) return false;
		a1 = (dz1*dy2 - dz2*dy1) / den;
		a2 = -(dz1*dx2 - dz2*dx1) / den;
		return true;
	}
	boolean initTriangleXY() {
		double dx1 = xyz[1][0]-xyz[0][0];
		double dy1 = xyz[1][1]-xyz[0][1];
		double dz1 = xyz[1][2]-xyz[0][2];
		double dx2 = xyz[2][0]-xyz[0][0];
		double dy2 = xyz[2][1]-xyz[0][1];
		double dz2 = xyz[2][2]-xyz[0][2];
		double den = dx1*dy2 - dx2*dy1;
		if( den==0f ) return false;
		a1 = (dz1*dy2 - dz2*dy1) / den;
		a2 = -(dz1*dx2 - dz2*dx1) / den;
		for( int k=0 ; k<2 ; k++) {
			dz1 = xy[1][k]-xy[0][k];
			dz2 = xy[2][k]-xy[0][k];
			c1[k] = (dz1*dy2 - dz2*dy1) / den;
			c2[k] = -(dz1*dx2 - dz2*dx1) / den;
		}
		return true;
	}
	float[] getRGB(int x, int y, float[] rgb) {
		try {
			return (new Color(map.getRGB(x,y))).getRGBColorComponents(rgb);
		} catch(Exception ex) {
		}
		return null;
	}
	int getColor(int ix, int iy) {
		double x = xy[0][0] -.5 + c1[0]*(ix-xyz[0][0])
				+ c2[0]*(iy-xyz[0][1]);
		double y = xy[0][1] -.5 + c1[1]*(ix-xyz[0][0])
				+ c2[1]*(iy-xyz[0][1]);
		ix = (int)Math.floor(x);
		if( ix<0 || ix>map.getWidth()-2) return background;
		iy = (int)Math.floor(y);
		if( iy<0 || iy>map.getHeight()-2) return background;
		int[][][] c = new int[2][2][3];
		x -= ix;
		y -= iy;
		double dxy = x*y;
		for( int kx=0 ; kx<2 ; kx++) {
			for( int ky=0 ; ky<2 ; ky++ ) {
				int rgb = map.getRGB(ix+kx,iy+ky);
				c[kx][ky][0] = (rgb>>16)&255;
				c[kx][ky][1] = (rgb>>8)&255;
				c[kx][ky][2] = (rgb)&255;
			}
		}
		int[] rgb = new int[3];
		for( int k=0 ; k<3 ; k++) {
			rgb[k] = (int)Math.rint(c[0][0][k]*(1.-x-y+dxy)
				+ c[1][0][k]*(x-dxy)
				+ c[0][1][k]*(y-dxy)
				+ c[1][1][k]*dxy);
		}
		return 0xff000000 | (rgb[0]<<16) | (rgb[1]<<8) | rgb[2];
	}
	double getDist(int x, int y) {
		return xyz[0][2] 
			+ a1*((double)x-xyz[0][0]) 
			+ a2*((double)y-xyz[0][1]);
	}
	public static void main(String[] args) {
		PerspectiveImage pi = new PerspectiveImage(null, null);
		pi.color = new float[][] {
			{0f, 0f, 0f},
			{0f, 1f, 0f},
			{1f, 0f, 1f} };
		pi.xyz = new double[][] {
			{0., 0., 10.},
			{5.1, 0., 10.},
			{0., 5.1, 10.} };
		pi.bounds = new Rectangle(0,0,10, 10);
		pi.zBuf = new Grid2D.Float( 
			pi.bounds,
			null);
		pi.fill(pi.zBuf);
	}
	public void dispose() {
		super.dispose();
		
		grid = null;
		grd = null;
		tool = null;
		pers = null;
	}
}

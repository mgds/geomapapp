package haxby.grid;

import haxby.proj.*;
import haxby.map.*;
import java.io.*;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.StringTokenizer;
import java.util.Vector;

public class GridderZW implements Serializable,
				GridServer  {
	Projection proj;
	GridderZW gridder;
	transient XGrid_ZW[] grid;
	int gridSize;
	int nLevel;
	File dir;
	File cruiseDir;
	String path;
	transient Vector tiles;
	transient XGrid_ZW lastGrid = null;
	transient boolean readonly=false;
	int wrapX;
	boolean wrap;
	int[] readOrder;
	public GridderZW(int gridSize, 
			int numGrids, 
			int nLevel,
			Projection proj, 
			String dirName,
			GridderZW gridder) throws IOException {
		this( gridSize, numGrids, nLevel, proj, dirName);
		this.gridder = gridder;
	}
	public GridderZW(int gridSize, 
			int numGrids, 
			int nLevel,
			Projection proj, 
			String dirName) throws IOException {
		gridder = null;
		this.gridSize = gridSize;
		this.proj = proj;
		this.nLevel = nLevel;
		this.dir = new File(dirName + "/zw");
		path = dir.getPath();
		if(!dir.exists()) {
			if(!dir.mkdirs()) throw new IOException(
					"\n** could not create "+ dirName);
//	System.out.println(dir.getPath() +" created");
		} else if( !dir.isDirectory() ) {
			throw new IOException("\n** open error:\t"+
				dirName +" not a directory");
		}
		cruiseDir = new File(dir, "cruises");
		if( !cruiseDir.exists()) cruiseDir.mkdirs();
		grid = new XGrid_ZW[numGrids];
		readOrder = new int[numGrids];
		for( int i=0 ; i<numGrids ; i++) {
			readOrder[i] = -1;
			grid[i] = null;
		}
		tiles = new Vector();
		wrap = false;
	}
	public void setWrap(int wrapX) {
		this.wrapX = wrapX;
		this.wrap = (wrapX>0);
	}
	public void setReadonly(boolean tf) {
		readonly = tf;
	}
	public String getDirectory() {
		return path;
	}
	public void addPoint(int x, int y, double z, double wt) throws IOException {
		if(wrap) {
			while(x<0) x+=wrapX;
			while(x>=wrapX) x-=wrapX;
		}
		getGrid(x, y).addPoint(x, y, z, wt);
	}
	public Projection getProjection() {
		return proj;
	}
	public int getGridSize() {
		return gridSize;
	}
	public int getNumGrids() {
		return grid.length;
	}
	public void setNumGrids(int nGrid) {
		if( nGrid<=grid.length )return;
		XGrid_ZW[] tmp = new XGrid_ZW[nGrid];
		readOrder = new int[nGrid];
		System.arraycopy(grid, 0, tmp, 0, grid.length);
		for(int i=grid.length ; i<nGrid ; i++ ) {
			tmp[i]=null;
			readOrder[i]=-1;
		}
		grid = tmp;
	}
	public File getCruiseDir() {
		return cruiseDir;
	}
	public XGrid_ZW getGrid(String name) throws IOException {
		StringTokenizer st = new StringTokenizer(name, "WESN_.", true);
		int x0 = (st.nextToken().equals("E")) ? 1 : -1;
		x0 *= Integer.parseInt(st.nextToken());
		int y0 = (st.nextToken().equals("N")) ? 1 : -1;
		y0 *= Integer.parseInt(st.nextToken());
		st.nextToken();
		return getGrid(x0*gridSize, y0*gridSize);
	}
	public Point2D[] getGridOutline( String name ) {
		StringTokenizer st = new StringTokenizer(name, "WESN_.", true);
		int x0 = (st.nextToken().equals("E")) ? 1 : -1;
		x0 *= gridSize * Integer.parseInt(st.nextToken());
		int y0 = (st.nextToken().equals("N")) ? 1 : -1;
		y0 *= gridSize * Integer.parseInt(st.nextToken());
		Point2D[] outline = new Point2D[4];
		Point p = new Point(x0, y0);
		outline[0] = proj.getRefXY(p);
		p = new Point(x0+gridSize, y0);
		outline[1] = proj.getRefXY(p);
		p = new Point(x0+gridSize, y0+gridSize);
		outline[2] = proj.getRefXY(p);
		p = new Point(x0, y0+gridSize);
		outline[3] = proj.getRefXY(p);
		return outline;
	}
	public Point2D[] getGridOutline( String name, Projection prj ) {
		StringTokenizer st = new StringTokenizer(name, "WESN_.", true);
		int x0 = (st.nextToken().equals("E")) ? 1 : -1;
		x0 *= gridSize * Integer.parseInt(st.nextToken());
		int y0 = (st.nextToken().equals("N")) ? 1 : -1;
		y0 *= gridSize * Integer.parseInt(st.nextToken());
		Point2D[] outline = new Point2D[4];
		Point p = new Point(x0, y0);
		outline[0] = prj.getMapXY(proj.getRefXY(p));
		p = new Point(x0+gridSize, y0);
		outline[1] = prj.getMapXY(proj.getRefXY(p));
		p = new Point(x0+gridSize, y0+gridSize);
		outline[2] = prj.getMapXY(proj.getRefXY(p));
		p = new Point(x0, y0+gridSize);
		outline[3] = prj.getMapXY(proj.getRefXY(p));
		return outline;
	}
	public String getName(int x0, int y0) {
		return ( (x0>=0) ? "E"+x0 : "W"+(-x0) )
			+ ( (y0>=0) ? "N"+y0 : "S"+(-y0) )
			+ "_" + gridSize;
	}
	public File getFile(String name) {
		StringTokenizer st = new StringTokenizer(name, "WESN_.", true);
		int x0 = (st.nextToken().equals("E")) ? 1 : -1;
		x0 *= Integer.parseInt(st.nextToken());
		int y0 = (st.nextToken().equals("N")) ? 1 : -1;
		y0 *= Integer.parseInt(st.nextToken());
		return getFile(x0, y0);
	}
	public File getFile(int x, int y) {
		int factor = 8;
		for( int k=1 ; k<nLevel ; k++) factor *=8;
		File fileDir = new File(path);
		for( int k=0 ; k<nLevel ; k++) {
			int xG = factor*(int)Math.floor( (double)x / (double)factor);
			int yG = factor*(int)Math.floor( (double)y / (double)factor);
			fileDir = new File(fileDir,getName(xG, yG));
			factor /= 8;
		}
		return new File(fileDir, getName(x, y) + ".xgrid");
	}
	public XGrid_ZW getGrid(int x, int y) throws IOException {
		if(wrap) {
			while(x<0) x+=wrapX;
			while(x>=wrapX) x-=wrapX;
		}
		if(lastGrid != null && lastGrid.contains(x, y)) return lastGrid;
		int xx = (int)Math.floor( (double)x/(double)gridSize );
		int yy = (int)Math.floor( (double)y/(double)gridSize );
		for( int i=0 ; i<grid.length ; i++ ) {
			if( grid[i]==null ) break;
			if( grid[i]!=null && grid[i].contains(x, y) ) {
				lastGrid = grid[i];
				return grid[i];
			}
		}
		int tileIndex;
		for( tileIndex =tiles.size()-1 ; tileIndex >=0  ; tileIndex--) {
			Point p = (Point)tiles.get(tileIndex);
			if( p.x==xx && p.y==yy )break;
		}
		if( tileIndex  == -1 ) tiles.add(new Point(xx,yy));

		int factor = 8;
		for( int k=1 ; k<nLevel ; k++) factor *=8;
		File fileDir = new File(path);
		for( int k=0 ; k<nLevel ; k++) {
			int xG = factor*(int)Math.floor( (double)xx / (double)factor);
			int yG = factor*(int)Math.floor( (double)yy / (double)factor);
			fileDir = new File(fileDir,getName(xG, yG));
			factor /= 8;
		}
		if(!fileDir.exists()) fileDir.mkdirs();
//	System.out.println( fileDir.getPath() );
		for( int i=0 ; i<grid.length ; i++ ) {
			readOrder[i]++;
			if(grid[i]==null) {
				grid[i] = new XGrid_ZW(xx, yy, gridSize, fileDir, proj);
				if( gridder!=null ) merge(grid[i], x, y);
				lastGrid = grid[i];
				return grid[i];
			}
		}
		double dist = 0;
		int igrid = -1;
		if( lastGrid==null) {
			igrid = 0;
		} else {
			for( int i=0 ; i<grid.length ; i++ ) {
				if( readOrder[i]==grid.length ) {
					readOrder[i] = 0;
					igrid = i;
					break;
				}
			}
			if( igrid==-1 ) {
				igrid = 0;
				for( int i=0 ; i<grid.length ; i++ ) {
					grid[i] = null;
					readOrder[i] = -1;
				}
			}
		}
		if(!readonly) {
			grid[igrid].writeGrid();
		}
		grid[igrid] = null;
		System.gc();
		grid[igrid] = new XGrid_ZW(xx, yy, gridSize, fileDir, proj);
		if( gridder!=null ) merge(grid[igrid], x, y);
		lastGrid = grid[igrid];
		return lastGrid;
	}
	void merge( XGrid_ZW grid, int x, int y) {
		if( gridder==null ) return;
		int xx = (int)Math.floor( (double)x/(double)gridSize );
		int yy = (int)Math.floor( (double)y/(double)gridSize );
		if( !gridder.getFile(xx, yy).exists() )return;
		XGrid_ZW g = null;
		try {
			g = gridder.getGrid( x, y );
		} catch(IOException ex) {
			return;
		}
		float[][] z = grid.getGrid();
		float[][] z1 = g.getGrid();
		for(int i=0 ; i<z.length ; i++) {
			if( z1[i][1]!=0f ) {
				z[i][0] += z1[i][0];
				z[i][1] += z1[i][1];
			}
		}
	}
	public void finish() throws IOException {
		for( int i=0 ; i<grid.length ; i++ ) {
			if(grid[i]==null)break;
			if(!readonly) {
				grid[i].writeGrid();
			}
		}
	}
	public XGrid getXGrid( double[] wesn, int decFctr, 
				GridImager imager,
				MapOverlay map ) {
		Point2D ul = proj.getMapXY( new Point2D.Double( wesn[0], wesn[3] ) );
		Point2D lr = proj.getMapXY( new Point2D.Double( wesn[1], wesn[2] ) );
		double w = Math.rint( ul.getX() );
		double e = Math.rint( lr.getX() );
		double n = Math.rint( ul.getY() );
		double s = Math.rint( lr.getY() );
		Projection prj = map.getXMap().getProjection();
		Point2D p0 = prj.getMapXY( proj.getRefXY( new Point2D.Double( w, n )));
		Point2D p1 = prj.getMapXY( proj.getRefXY( new Point2D.Double( e, n )));
		double scale = (p1.getX()-p0.getX()) / (e-w);
		int x0 = (int)Math.rint(w);
		int y0 = (int)Math.rint(n);
		int width = (int)Math.rint(e-w);
		int height = (int)Math.rint(s-n);
		XGrid_Z gz = new XGrid_Z(x0, y0, width, height, this);
		grid = new XGrid_ZW[0];
		XGrid xg = gz.decimate( decFctr );
		int numGrids = grid.length;
		gz = null;
		setNumGrids( numGrids);
		return xg;
	}
	public double valueAt( double x, double y ) {
		int ix = (int)Math.floor(x);
		int iy = (int)Math.floor(y);
		double v = valueAt(ix,iy);
		if( Double.isNaN(v) ) return v;
		double dx = x-(double)ix;
		double dy = y-(double)iy;
		double dxy = dx*dy;
		double z = v*(1.-dx-dy+dx*dy);
		v = valueAt(ix+1,iy);
		if( Double.isNaN(v) ) return v;
		z += v*(dx - dxy);
		v = valueAt(ix,iy+1);
		if( Double.isNaN(v) ) return v;
		z += v*(dy - dxy);
		v = valueAt(ix+1,iy+1);
		if( Double.isNaN(v) ) return v;
		z += v*dxy;
		return z;
	}
	public double valueAt( int x, int y ) {
		if(wrap) {
			while(x<0) x+=wrapX;
			while(x>=wrapX) x-=wrapX;
		}
		XGrid_ZW g = null;
		try {
			g = getGrid(x, y);
		} catch(IOException ex) {
			return Double.NaN;
		}
		int i = g.indexOf( x, y);
		double w = g.getGrid()[i][1];
		if(w==0.) return Double.NaN;
		else return g.getGrid()[i][0] / w;
	}
}

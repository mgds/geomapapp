package haxby.grid;

import haxby.proj.Projection;
import java.io.*;
import java.awt.Point;
import java.awt.geom.*;
import java.util.StringTokenizer;
import java.util.Vector;

public class TilerZ implements GridServer, Serializable {
	Projection proj;
	TilerZ tiler;
	transient XGridTile[] grid;
	int gridSize;
	int scale;
	int nLevel;
	File dir;
	String path;
	transient Vector tiles;
	transient XGridTile lastGrid = null;
	transient boolean readonly=false;
	int wrapX;
	boolean wrap;
	public TilerZ(int gridSize, 
			int scale,
			int numGrids, 
			int nLevel,
			Projection proj, 
			String dirName,
			TilerZ tiler) throws IOException {
		this( gridSize, scale, numGrids, nLevel, proj, dirName);
		this.tiler = tiler;
	}
	public TilerZ(int gridSize, 
			int scale,
			int numGrids, 
			int nLevel,
			Projection proj, 
			String dirName) throws IOException {
		tiler = null;
		this.gridSize = gridSize;
		this.scale = scale;
		this.proj = proj;
		this.nLevel = nLevel;
		this.dir = new File(dirName + "/z_" + scale);
		path = dir.getPath();
		if(!dir.exists()) {
			if(!dir.mkdirs()) throw new IOException(
					"\n** could not create "+ dirName);
		} else if( !dir.isDirectory() ) {
			throw new IOException("\n** open error:\t"+
				dirName +" not a directory");
		}
		grid = new XGridTile[numGrids];
		for( int i=0 ; i<numGrids ; i++) grid[i] = null;
		tiles = null;
		wrap = false;
	}
	public int getScale() {
		return scale;
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
	public void addPoint(int x, int y, double z) throws IOException {
		if(wrap) {
			while(x<0) x+=wrapX;
			while(x>=wrapX) x-=wrapX;
		}
		getGrid(x, y).addPoint(x, y, (float)z);
	}
	public double valueAt( int x, int y ) {
		if(wrap) {
			while(x<0) x+=wrapX;
			while(x>=wrapX) x-=wrapX;
		}
		try {
			return getGrid(x, y).getZ(x, y);
		} catch (IOException ex) {
			return Double.NaN;
		}
	}
	public XGrid_Z composeGrid( double[] wesn ) {
		Point2D.Double p1 = (Point2D.Double) proj.getMapXY(
				new Point2D.Double(wesn[0], wesn[3]) );
		Point2D.Double p2 = (Point2D.Double) proj.getMapXY(
				new Point2D.Double(wesn[1], wesn[2]) );
		if(wrap) while( p2.x<p1.x ) p2.x += wrapX;
		p1.x = Math.floor(p1.x);
		p1.y = Math.floor(p1.y);
		p2.x = Math.ceil(p2.x);
		p2.y = Math.ceil(p2.y);
		int x1 = (int)p1.x;
		int y1 = (int)p1.y;
		int x2 = (int)p2.x;
		int y2 = (int)p2.y;
		int width = x2-x1+1;
		int height = y2-y1+1;
		return new XGrid_Z( x1, y1, width, height, (GridServer)this);
	}
	public Projection getProjection() {
		return proj;
	}
	public int getGridSize() {
		return gridSize;
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
	public void setNumGrids(int nGrid) {
		if( nGrid==grid.length )return;
		XGridTile[] tmp = new XGridTile[nGrid];
		if( nGrid>grid.length ) {
			System.arraycopy(grid, 0, tmp, 0, grid.length);
			for(int i=grid.length ; i<nGrid ; i++ )tmp[i]=null;
		} else {
			System.arraycopy(grid, 0, tmp, 0, nGrid);
		}
		grid = tmp;
	}
	public XGridTile getGrid(String name) throws IOException {
		StringTokenizer st = new StringTokenizer(name, "WESN_.", true);
		int x0 = (st.nextToken().equals("E")) ? 1 : -1;
		x0 *= Integer.parseInt(st.nextToken());
		int y0 = (st.nextToken().equals("N")) ? 1 : -1;
		y0 *= Integer.parseInt(st.nextToken());
		st.nextToken();
		return getGrid(x0*gridSize, y0*gridSize);
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
		return new File(fileDir, getName(x, y) + ".zgrid");
	}
	public XGridTile getGrid(int x, int y) throws IOException {
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
		int factor = 8;
		for( int k=1 ; k<nLevel ; k++) factor *=8;
		File fileDir = new File(path);
		for( int k=0 ; k<nLevel ; k++) {
			int xG = factor*(int)Math.floor( (double)xx / (double)factor);
			int yG = factor*(int)Math.floor( (double)yy / (double)factor);
			fileDir = new File(fileDir,getName(xG, yG));
			factor /= 8;
		}
//	System.out.println( getFile(xx, yy).getPath() );
		if(!fileDir.exists()) fileDir.mkdirs();
		for( int i=0 ; i<grid.length ; i++ ) {
			if(grid[i]==null) {
				if( getFile(xx, yy).exists() || tiler==null ) {
				//	grid[i] = new XGridTile(xx, yy, gridSize, fileDir, proj);
					grid[i] = getGridTile(xx, yy, fileDir);
				} else {
					grid[i] = tiler.getGrid(x, y);
				}
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
				if( grid[i]==lastGrid ) {
					igrid = (i+1) % grid.length;
					break;
				}
			}
		}
		if(!readonly) {
			grid[igrid].writeGrid();
		}
		grid[igrid] = null;
		System.gc();
		if( getFile(xx, yy).exists() || tiler==null ) {
		//	grid[igrid] = new XGridTile(xx, yy, gridSize, fileDir, proj);
			grid[igrid] = getGridTile(xx, yy, fileDir);
		} else {
			grid[igrid] = tiler.getGrid(x, y);
		}
		lastGrid = grid[igrid];
		return lastGrid;
	}
	XGridTile getGridTile( int x, int y, File dir ) throws IOException {
		return new XGridTile(x, y, gridSize, dir, proj);
	}
		public void finish() throws IOException {
			if(readonly) return;
			for( int i=0 ; i<grid.length ; i++ ) {
				if(grid[i]==null)break;
				grid[i].writeGrid();
			}
		}
}
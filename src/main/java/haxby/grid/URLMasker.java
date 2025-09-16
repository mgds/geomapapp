package haxby.grid;

import haxby.proj.Projection;
import haxby.util.URLFactory;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.Vector;

public class URLMasker implements Serializable {
	URLMasker masker;
	Projection proj;
	transient MaskTile[] grid;
	int gridSize;
	int scale;
	int nLevel;
	String baseURL;
//	File dir;
//	String path;
	transient Vector tiles;
	transient MaskTile lastGrid = null;
	transient boolean readonly=false;
	int wrapX;
	boolean wrap;
	public URLMasker(int gridSize, 
			int scale,
			int numGrids, 
			int nLevel,
			Projection proj, 
			String URLName,
			URLMasker masker) {
		this( gridSize, scale, numGrids, nLevel, proj, URLName);
		this.masker = masker;
	}
	public URLMasker(int gridSize, 
			int scale,
			int numGrids, 
			int nLevel,
			Projection proj, 
			String URLName) {
		masker = null;
		this.gridSize = gridSize;
		this.scale = scale;
		this.proj = proj;
		this.nLevel = nLevel;
		baseURL =URLName + "/m_" + scale;
		grid = new MaskTile[numGrids];
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
	public int valueAt( int x, int y ) {
		if(wrap) {
			while(x<0) x+=wrapX;
			while(x>=wrapX) x-=wrapX;
		}
		try {
			return getGrid(x, y).getZ(x, y);
		} catch (IOException ex) {
			return -1;
		}
	}
	public Projection getProjection() {
		return proj;
	}
	public int getGridSize() {
		return gridSize;
	}
	public void setNumGrids(int nGrid) {
		if( nGrid==grid.length )return;
		MaskTile[] tmp = new MaskTile[nGrid];
		if( nGrid>grid.length ) {
			System.arraycopy(grid, 0, tmp, 0, grid.length);
			for(int i=grid.length ; i<nGrid ; i++ )tmp[i]=null;
		} else {
			System.arraycopy(grid, 0, tmp, 0, nGrid);
		}
		grid = tmp;
	}
	public MaskTile getGrid(String name) throws IOException {
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
	public URL getURL( String name ) throws IOException {
		StringTokenizer st = new StringTokenizer(name, "WESN_.", true);
		int x0 = (st.nextToken().equals("E")) ? 1 : -1;
		x0 *= Integer.parseInt(st.nextToken());
		int y0 = (st.nextToken().equals("N")) ? 1 : -1;
		y0 *= Integer.parseInt(st.nextToken());
		return getURL( x0*gridSize, y0*gridSize);
	}
	public URL getURL(int x, int y) throws IOException {
		int factor = 8;
		for( int k=1 ; k<nLevel ; k++) factor *=8;
		String url = baseURL;
		for( int k=0 ; k<nLevel ; k++) {
			int xG = factor*(int)Math.floor( (double)x / (double)factor);
			int yG = factor*(int)Math.floor( (double)y / (double)factor);
			url += "/"+getName(xG, yG);
			factor /= 8;
		}
		url += "/" + getName(x, y) + ".mask";
		return URLFactory.url( url );
	}
	public MaskTile getGrid(int x, int y) throws IOException {
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
		URL url = getURL(xx, yy);
		for( int i=0 ; i<grid.length ; i++ ) {
			if(grid[i]==null) {
			//	try {
					grid[i] = new MaskTile(xx, yy, gridSize, url, proj);
			//	} catch(IOException ex) {
			//	}
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
		grid[igrid] = null;
		System.gc();
		grid[igrid] = new MaskTile(xx, yy, gridSize, url, proj);
		lastGrid = grid[igrid];
		return lastGrid;
	}
}

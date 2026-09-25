package haxby.grid;

import haxby.proj.Projection;
import java.io.*;
import java.util.StringTokenizer;

public class Gridder implements Serializable {
	Projection proj;
	transient XGrid_ZW[] grid;
	int gridSize;
	File dir;
	transient XGrid_ZW lastGrid = null;
	transient boolean readonly=false;
	public Gridder(int gridSize, 
			int numGrids, 
			Projection proj, 
			String dirName) throws IOException {
		this.proj = proj;
		this.dir = new File(dirName);
		if(!dir.exists()) {
			if(!dir.mkdirs()) throw new IOException(
					"\n** could not create "+ dirName);
	System.out.println(dir.getPath() +" created");
		} else if( !dir.isDirectory() ) {
			throw new IOException("\n** open error:\t"+
				dirName +" not a directory");
		}
		this.gridSize = gridSize;
		grid = new XGrid_ZW[numGrids];
		for( int i=0 ; i<numGrids ; i++) grid[i] = null;
	}
	public void setReadonly(boolean tf) {
		readonly = tf;
	}
	public String getDirectory() {
		return dir.getName();
	}
	public void addPoint(int x, int y, double z, double wt) throws IOException {
		getGrid(x, y).addPoint(x, y, z, wt);
	}
	public Projection getProjection() {
		return proj;
	}
	public int getGridSize() {
		return gridSize;
	}
	public void setNumGrids(int nGrid) {
		if( nGrid==grid.length )return;
		XGrid_ZW[] tmp = new XGrid_ZW[nGrid];
		if( nGrid>grid.length ) {
			System.arraycopy(grid, 0, tmp, 0, grid.length);
			for(int i=grid.length ; i<nGrid ; i++ )tmp[i]=null;
		} else {
			System.arraycopy(grid, 0, tmp, 0, nGrid);
		}
		grid = tmp;
	}
	public XGrid_ZW getGrid(String name) throws IOException {
		File file = new File(dir,name);
		if( !file.exists() ) {
			throw new IOException(file.getAbsolutePath() +" does not exist");
		}
		StringTokenizer st = new StringTokenizer(name, "WESN_.", true);
		int x0 = (st.nextToken().equals("E")) ? 1 : -1;
		x0 *= Integer.parseInt(st.nextToken());
		int y0 = (st.nextToken().equals("N")) ? 1 : -1;
		y0 *= Integer.parseInt(st.nextToken());
		st.nextToken();
		return getGrid(x0*gridSize, y0*gridSize);
	}
	public XGrid_ZW getGrid(int x, int y) throws IOException {
		if(lastGrid != null && lastGrid.contains(x, y)) return lastGrid;
		for( int i=0 ; i<grid.length ; i++ ) {
			if( grid[i]==null ) break;
			if( grid[i]!=null && grid[i].contains(x, y) ) {
				lastGrid = grid[i];
				return grid[i];
			}
		}
		int xx = (int)Math.floor( (double)x/(double)gridSize );
		int yy = (int)Math.floor( (double)y/(double)gridSize );
		for( int i=0 ; i<grid.length ; i++ ) {
			if(grid[i]==null) {
				grid[i] = new XGrid_ZW(xx, yy, gridSize, dir, proj);
				lastGrid = grid[i];
				return grid[i];
			}
		}
		double dist = 0;
		int igrid = -1;
		for( int i=0 ; i<grid.length ; i++ ) {
			double d = grid[i].distanceSq(x, y);
//	System.out.println(x/gridSize +"\t"+ y/gridSize +"\t"+ grid[i].fileName() +"\t"+ Math.sqrt(d));
			if(d>dist) {
				dist = d;
				igrid = i;
			}
		}
		if(!readonly) grid[igrid].writeGrid();
		grid[igrid] = new XGrid_ZW(xx, yy, gridSize, dir, proj);
		lastGrid = grid[igrid];
		return lastGrid;
	}
	public void finish() throws IOException {
		if(!readonly) return;
		for( int i=0 ; i<grid.length ; i++ ) {
			if(grid[i]==null)break;
			grid[i].writeGrid();
		}
	}
}

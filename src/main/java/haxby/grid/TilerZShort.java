package haxby.grid;

import haxby.proj.Projection;
import java.io.*;

public class TilerZShort extends TilerZ {
	public TilerZShort(int gridSize, 
			int scale,
			int numGrids, 
			int nLevel,
			Projection proj, 
			String dirName,
			TilerZ tiler) throws IOException {
		super( gridSize, scale, numGrids, nLevel, proj, dirName, tiler);
	}
	public TilerZShort(int gridSize, 
			int scale,
			int numGrids, 
			int nLevel,
			Projection proj, 
			String dirName) throws IOException {
		super( gridSize, scale, numGrids, nLevel, proj, dirName);
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
		return new File(fileDir, getName(x, y) + ".igrid.gz");
	}
	XGridTile getGridTile( int x, int y, File dir ) throws IOException {
		return new XGridTileShort(x, y, gridSize, dir, proj);
	}
		public void finish() throws IOException {
			if(readonly) return;
			for( int i=0 ; i<grid.length ; i++ ) {
				if(grid[i]==null)break;
				grid[i].writeGrid();
			}
		}
}
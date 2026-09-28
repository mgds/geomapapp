package haxby.grid;

import haxby.proj.Projection;
import java.io.*;
import java.net.*;

public class XGridTile implements Serializable {
	float[] grid;
	Projection proj;
	int x0, y0, size;
	int[] bounds;
	File dir;
	URL url;
	boolean hasData;
	public XGridTile(int x0, int y0, 
			int size, 
			File dir,
			Projection proj) throws IOException {
		this.proj = proj;
		this.x0 = x0;
		this.y0 = y0;
		this.size = size;
		grid = new float[size*size];
		for( int k=0 ; k<size*size ; k++) grid[k] = Float.NaN;
		bounds = new int[] { x0*size, y0*size, (x0+1)*size, (y0+1)*size };
		this.dir = dir;
		File file = new File(dir, fileName());
		url = null;
		if(file.exists()) {
			try {
				readGrid();
				hasData = true;
			} catch( IOException ex) {
				hasData = false;
				for( int k=0 ; k<size*size ; k++) grid[k] = Float.NaN;
			}
		} else {
			hasData = false;
			for( int k=0 ; k<size*size ; k++) grid[k] = Float.NaN;
		}
	}
	public XGridTile(int x0, int y0, 
			int size, 
			URL url,
			Projection proj) throws IOException {
		this.proj = proj;
		this.x0 = x0;
		this.y0 = y0;
		this.size = size;
		grid = new float[size*size];
		for( int k=0 ; k<size*size ; k++) grid[k] = Float.NaN;
		bounds = new int[] { x0*size, y0*size, (x0+1)*size, (y0+1)*size };
		this.url = url;
		dir = null;
		try {
			readGrid();
			hasData = true;
		} catch( IOException ex) {
			for( int k=0 ; k<size*size ; k++) grid[k] = Float.NaN;
			hasData = false;
		}
	}
	public static XGridTile tileZW( GridderZW gridder, 
				TilerZ tiler, 
				String name) throws IOException {
		Projection proj = gridder.getProjection();
		XGrid_ZW gzw = gridder.getGrid(name);
		XGridTile tile = tiler.getGrid(name);
		float[][] g = gzw.getGrid();
		float[] grid = new float[g.length];
		for( int k=0 ; k<g.length ; k++) {
			if(g[k][1]==0.) {
				grid[k] = Float.NaN;
			} else {
				grid[k] = g[k][0]/g[k][1];
			}
		}
		tile.grid = grid;
		return tile;
	}
	public String fileName() {
		return ( (x0>=0) ? "E"+x0 : "W"+(-x0) )
			+ ( (y0>=0) ? "N"+y0 : "S"+(-y0) )
			+ "_" + size + ".zgrid";
	}
	public int[] getBounds() {
		return new int[] { x0*size, y0*size, (x0+1)*size, (y0+1)*size };
	}
	public int getSize() {
		return size;
	}
	public float[] getGrid() { return grid; }
	public int indexOf(int x, int y) {
		if( !contains( x, y) )return -1;
		return x-bounds[0] + (y-bounds[1])*size;
	}
	public boolean contains(int x, int y) {
		return (x>=bounds[0] && y>=bounds[1] && x<bounds[2] && y<bounds[3]);
	}
	public void addPoint(int x, int y, float val) {
		int i = indexOf(x, y);
		if(i==-1) return;
		if( grid==null ) {
			grid = new float[size*size];
			for(int k=0 ; k<grid.length ; k++) grid[k]=Float.NaN;
		}
		grid[i] = val;
	}
	public double getZ( int x, int y) {
		if( grid==null ) return Double.NaN;
		int i = indexOf(x, y);
		if(i==-1 || Float.isNaN(grid[i]) ) return Double.NaN;
		return grid[i];
	}
	public void writeGrid() throws IOException {
		if( url!=null )return;
		File file = new File(dir, fileName());
		if( !file.exists() ) file.createNewFile();
		DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(
				new FileOutputStream(file)));
		int i=0;
		int n;
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
	void readGrid() throws IOException {
		DataInputStream in =null;
		if( url==null ) {
			in = new DataInputStream(
				new BufferedInputStream(
				new FileInputStream(new File(dir, fileName()))));
		} else {
			in = new DataInputStream( url.openStream() );
		}
		int i=0;
		int n;
		while( i<grid.length ) {
			n = in.readInt();
			i+=n;
			if( i<grid.length ) {
				n = in.readInt();
				for( int k=0 ; k<n ; k++) {
					grid[i] = in.readFloat();
					i++;
				}
			}
		}
	}
}

package haxby.grid;

import haxby.proj.Projection;
import java.io.*;
import java.awt.geom.Point2D;
import java.awt.Point;

public class XGrid_ZW implements Serializable {
	float[][] grid;
	Projection proj;
	int x0, y0, size;
	int[] bounds;
	File dir;
	double[] center;
	public XGrid_ZW(int x0, int y0, 
			int size, 
			File dir,
			Projection proj) throws IOException {
		this.proj = proj;
		this.x0 = x0;
		this.y0 = y0;
		this.size = size;
		grid = new float[size*size][2];
		bounds = new int[] { x0*size, y0*size, (x0+1)*size, (y0+1)*size };
		center = new double[] { .5*(double)(bounds[0]+bounds[2]),
					.5*(double)(bounds[1]+bounds[3]) };
		this.dir = dir;
		File file = new File(dir, fileName());
		if(file.exists()) readGrid();
	//	else System.out.println("init:\t"+file.getPath());
	}
	public Point2D[] getOutline() {
		Point2D[] outline = new Point2D[4];
		Point map = new Point( bounds[0], bounds[1]);
		outline[0] = proj.getRefXY(map);
		map = new Point( bounds[0], bounds[3]);
		outline[1] = proj.getRefXY(map);
		map = new Point( bounds[2], bounds[3]);
		outline[2] = proj.getRefXY(map);
		map = new Point( bounds[2], bounds[1]);
		outline[3] = proj.getRefXY(map);
		return outline;
	}
	public Point2D[] getOutline(Projection p) {
		Point2D[] outline = new Point2D[4];
		Point2D latlon;
		Point map = new Point( bounds[0], bounds[1]);
		latlon = proj.getRefXY(map);
		outline[0] = p.getMapXY(latlon);
		map = new Point( bounds[0], bounds[3]);
		latlon = proj.getRefXY(map);
		outline[1] = proj.getMapXY(latlon);
		map = new Point( bounds[2], bounds[3]);
		latlon = proj.getRefXY(map);
		outline[2] = proj.getMapXY(latlon);
		map = new Point( bounds[2], bounds[1]);
		latlon = proj.getRefXY(map);
		outline[3] = proj.getMapXY(latlon);
		return outline;
	}
	public String fileName() {
		return ( (x0>=0) ? "E"+x0 : "W"+(-x0) )
			+ ( (y0>=0) ? "N"+y0 : "S"+(-y0) )
			+ "_" + size + ".xgrid";
	}
	public int[] getBounds() {
		return new int[] { x0*size, y0*size, (x0+1)*size, (y0+1)*size };
	}
	public int getSize() {
		return size;
	}
	public float[][] getGrid() { return grid; }
	public int indexOf(int x, int y) {
		if( !contains( x, y) )return -1;
		return x-bounds[0] + (y-bounds[1])*size;
	}
	public boolean contains(int x, int y) {
		return (x>=bounds[0] && y>=bounds[1] && x<bounds[2] && y<bounds[3]);
	}
	public double distanceSq(int x, int y) {
		double dx = (double)x - center[0];
		double dy = (double)y - center[1];
		return dx*dx+dy*dy;
	}
	public void addPoint(int x, int y, double val, double wt) {
		int i = indexOf(x, y);
		if(i==-1)throw new ArrayIndexOutOfBoundsException();
		grid[i][0] += (float)(wt*val);
		grid[i][1] += (float)wt;
	}
	public void writeGrid() throws IOException {
		File file = new File(dir, fileName());
		if( !file.exists() ) file.createNewFile();
		DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(
				new FileOutputStream(file)));
		int i=0;
		int n;
		while(i<grid.length) {
			n=0;
			while( i<grid.length && grid[i][1]==0 ) {
				n++;
				i++;
			}
			out.writeInt(n);
			if( i>=grid.length ) break;
			n=0;
			while( i+n<grid.length && grid[i+n][1]!=0 ) n++;
			out.writeInt(n);
			for( int k=0 ; k<n ; k++) {
				out.writeFloat(grid[i][0]);
				out.writeFloat(grid[i][1]);
				i++;
			}
		}
		out.close();
	//	System.out.println("wrote:\t"+file.getPath());
	}
	void readGrid() throws IOException {
		File file = new File(dir, fileName());
	//	System.out.println( file.getPath() );
		DataInputStream in = new DataInputStream(
				new BufferedInputStream(
				new FileInputStream( file)));
		int i=0;
		int n;
		while( i<grid.length ) {
			n = in.readInt();
			i+=n;
			if( i<grid.length ) {
				n = in.readInt();
				for( int k=0 ; k<n ; k++) {
					grid[i][0] = in.readFloat();
					grid[i][1] = in.readFloat();
					i++;
				}
			}
		}
	//	System.out.println("read:\t"+file.getPath());
	}
}

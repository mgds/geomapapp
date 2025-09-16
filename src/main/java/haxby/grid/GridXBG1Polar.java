package haxby.grid;

import haxby.proj.PolarStereo;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.grid.Grid2D;
import org.geomapapp.grid.TileIO;
import org.geomapapp.grid.TiledGrid;
import org.geomapapp.grid.Grid2D.Short;

public class GridXBG1Polar {
	
	public static void main(String[] args) {
		if( args.length != 3  && args.length != 4) {
			System.out.println("usage: java GridXBG1Polar dir xbgDir weight [pole]");
			System.out.println("\t where pole is optional and 0 for SP and 1 for NP");
			System.out.println("\t pole defaults to 0");
			System.exit(0);
		}
		System.out.println(args[0]);
		boolean southPole = true;
		if (args.length == 4)
			southPole = Integer.parseInt(args[3]) == 0;	
		
		String pole;
		PolarStereo proj, proj4;
		if (southPole)
		{
			proj = new PolarStereo( new java.awt.Point(0, 0),
					180., 50., -71.,
					PolarStereo.SOUTH,
					PolarStereo.WGS84);
			proj4 = new PolarStereo( new java.awt.Point(0, 0),
					180., 200., -71.,
					PolarStereo.SOUTH,
					PolarStereo.WGS84);
			pole = "SP";
		} else
		{
			proj = new PolarStereo( new java.awt.Point(0, 0),
					0., 50., 71.,
					PolarStereo.NORTH,
					PolarStereo.WGS84);
			proj4 = new PolarStereo( new java.awt.Point(0, 0),
					0., 200., 71.,
					PolarStereo.NORTH,
					PolarStereo.WGS84);
			pole = "NP";
		}
		
		String dir = args[0];
		if (dir.equals(".")) dir = System.getProperty("user.dir");
		int nLevel = 0;
		int nGrid = 1024;
		while( nGrid>8 ) {
			nLevel++;
			nGrid /= 8;
		}
		int nLevel4 = 0;
		int nGrid4 = 256;
		while( nGrid4>8 ) {
			nLevel4++;
			nGrid4 /= 8;
		}
		try {
			double wt = Double.parseDouble( args[2] );
			
			TiledGrid tiledGrid = openXBG( new File(args[1]), southPole);
			Grid2D.Short grid2DShort = 
				(Short) tiledGrid.composeGrid( tiledGrid.getBounds() );
			
			short[] gridZShort = grid2DShort.getBuffer();
			float[] gridZ = new float[gridZShort.length];
			for(int i=0 ; i<gridZ.length ; i++) {
				if( gridZShort[i]==0f ) 
					gridZ[i] = Float.NaN;
				else
					gridZ[i] = gridZShort[i];
			}
			
			MapProjection gp = tiledGrid.getProjection();
			Rectangle dim = tiledGrid.getBounds();
	System.out.println( "Width: " + dim.width  +"\t"+ " Height: " + dim.height +"\t"+ "Length: " + gridZ.length );
			float[] mask = GridMask.gridDistance( gridZ, dim.width, dim.height, 10f, false);
			int x1, y1, x2, y2;
			
			double wesn[] = grid2DShort.getWESN();

			x1 = y1 = Integer.MAX_VALUE;
			x2 = y2 = -Integer.MAX_VALUE;
			
			int[][] array = new int[][] {{0,0},
					{0,1},
					{1,0},
					{1,1}};
			
			for (int[] a : array)
			{
				Point2D testP = proj.getMapXY(wesn[a[0]], wesn[a[1] + 2]);
				
				x1 = Math.min(x1, (int)Math.floor(testP.getX()));
				x2 = Math.max(x2, (int)Math.floor(testP.getX()));
				y1 = Math.min(y1, (int)Math.ceil(testP.getY()));
				y2 = Math.max(y2, (int)Math.ceil(testP.getY()));
			}
			
			Point p = new Point();
			Point2D.Double map;
	System.out.println( x1 +"\t"+ x2 +"\t"+ y1 +"\t"+ y2 +"\t"+ (1 + (x2/320) - (x1/320)) +" by " + (1 + (y2/320) - (y1/320)) + " grids" );
			GridderZW gridder = new GridderZW( 320, 3, //  + (x2/320) - (x1/320),
					nLevel4, 
					proj4, 
					dir+"/" + 
					pole + 
					"_320_200");

			int yy1 = 320*(int)Math.floor(y1/320.);
			int yy2 = 320*(int)Math.ceil(y2/320.);
			int xx1 = 320*(int)Math.floor(x1/320.);
			int xx2 = 320*(int)Math.ceil(x2/320.);
	System.out.println( xx1 +"\t"+ xx2 +"\t"+ yy1 +"\t"+ yy2);
			for(int yy=yy1 ; yy<=yy2 ; yy+=320){
				int ty1 = Math.max(y1,yy);
				int ty2 = Math.min(y2,yy+320);
				System.out.println( (1 + (yy/320) - (y1/320)) + " of " + (1 + (y2/320) - (y1/320)));
				for(int xx=xx1 ; xx<=xx2 ; xx+=320) {
					int tx1 = Math.max(x1,xx);
					int tx2 = Math.min(x2,xx+320);
//	System.out.println( tx1 +"\t"+ tx2 +"\t"+ ty1 +"\t"+ ty2);
					for( int y=ty1 ; y<ty2 ; y++) {
						for( int x=tx1 ; x<tx2 ; x++) {
							p.y = y;
							p.x = x;
							map = (Point2D.Double)gp.getMapXY(proj4.getRefXY(p));
	//	if( x==tx1)System.out.println( x +"\t"+ y +"\t"+ proj4.getRefXY(p).getX() +"\t"+ proj4.getRefXY(p).getY() +"\t"+ map.getX() +"\t"+ map.getY());
							double z = tiledGrid.valueAt(map.x, map.y);
	//									grid.sample(map.x, map.y);
							if(Double.isNaN(z)) continue;
							double weight = Interpolate.bicubic( mask, 
								dim.width, dim.height,
								map.x - dim.x, map.y - dim.y);
							if( weight<=0. )continue;
							if( weight>1. )weight=1.;
							gridder.addPoint(x, y, z, wt*weight);
						}
					}
				}
			}
			System.out.println("Gridder4 Finish");
			gridder.finish();
			
			x1 = y1 = Integer.MAX_VALUE;
			x2 = y2 = -Integer.MAX_VALUE;
			
			array = new int[][] {{0,0},
					{0,1},
					{1,0},
					{1,1}};
			
			for (int[] a : array)
			{
				Point2D testP = proj.getMapXY(wesn[a[0]], wesn[a[1] + 2]);
				
				x1 = Math.min(x1, (int)Math.floor(testP.getX()));
				x2 = Math.max(x2, (int)Math.floor(testP.getX()));
				y1 = Math.min(y1, (int)Math.ceil(testP.getY()));
				y2 = Math.max(y2, (int)Math.ceil(testP.getY()));
			}
			
			p = new Point();
			System.out.println( x1 +"\t"+ x2 +"\t"+ y1 +"\t"+ y2 +"\t"+ (1 + (x2/320) - (x1/320)) +" by " + (1 + (y2/320) - (y1/320)) + " grids" );
			gridder = new GridderZW( 320, 3, // + (x2/320) - (x1/320),
					nLevel, proj, dir+"/"+ pole + "_320_50");
			yy1 = 320*(int)Math.floor(y1/320.);
			yy2 = 320*(int)Math.floor(y2/320.);
			xx1 = 320*(int)Math.floor(x1/320.);
			xx2 = 320*(int)Math.floor(x2/320.);
			for(int yy=yy1 ; yy<=yy2 ; yy+=320){
				int ty1 = Math.max(y1,yy);
				int ty2 = Math.min(y2,yy+320);
				System.out.println( (1 + (yy/320) - (y1/320)) + " of " + (1 + (y2/320) - (y1/320)));
				for(int xx=xx1 ; xx<=xx2 ; xx+=320) {
					int tx1 = Math.max(x1,xx);
					int tx2 = Math.min(x2,xx+320);
					for( int y=ty1 ; y<ty2 ; y++) {
						for( int x=tx1 ; x<tx2 ; x++) {
							p.y = y;
							p.x = x;
							map = (Point2D.Double)gp.getMapXY(proj.getRefXY(p));
							double z = grid2DShort.valueAt(map.x, map.y); 
	//									grid.sample(map.x, map.y);
							if(Double.isNaN(z)) continue;
							double weight = Interpolate.bicubic( mask, 
										dim.width, dim.height,
										map.x, map.y);
							if( weight<=0. )continue;
							if( weight>1. )weight=1.;
							gridder.addPoint(x, y, z, wt*weight);
						}
					}
				}
			}
			System.out.println("gridder finish");
			gridder.finish();
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(0);
		}
	}
	
	public static TiledGrid openXBG(File dir, boolean southPole) 
	{
		int res = Integer.parseInt(dir.getName().substring(2));
		
		MapProjection gridProj;
		
		if (southPole)
		{
			gridProj = new PolarStereo( new java.awt.Point(0, 0),
					180., 25600 / res, -71.,
					PolarStereo.SOUTH,
					PolarStereo.WGS84);
		} else
		{
			gridProj = new PolarStereo( new java.awt.Point(0, 0),
					0., 25600 / res, 71.,
					PolarStereo.NORTH,
					PolarStereo.WGS84);
		}
		
		File[] files = org.geomapapp.io.FileUtility.getFiles(dir,"igrid.gz");
		File directory = files[0].getParentFile();
		int nLevel = 0;
		while( !directory.equals(dir) ) {
			nLevel++;
			directory = directory.getParentFile();
		}
		TileIO.Short tileIO = new TileIO.Short(gridProj,
			dir.getAbsolutePath(),
			320, nLevel);
		int minX, maxX;
		int minY, maxY;
		minX = minY = Integer.MAX_VALUE;
		maxX = maxY = -Integer.MAX_VALUE;
		boolean start = true;
		Grid2D.Short tile;
		for( int k=0 ; k<files.length ; k++) {
			int[] xy = TileIO.getIndices(files[k].getName());
			int x0 = xy[0]*320;
			int y0 = xy[1]*320;
			try {
				tile = (Grid2D.Short)tileIO.readGridTile(x0,y0);
			} catch(Exception ex) {
				ex.printStackTrace();
				continue;
			}
			for(int x=x0 ; x<x0+320 ; x++) {
				for(int y=y0 ; y<y0+320 ; y++) {
					double z=tile.valueAt(x,y);
					if(Double.isNaN(z))continue;
					if( start ) {
						minX = maxX = x;
						minY = maxY = y;
						start = false;
						continue;
					}
					if( x>maxX )maxX=x;
					else if( x<minX )minX=x;
					if( y>maxY )maxY=y;
					else if( y<minY )minY=y;
				}
			}
		}
		Rectangle bounds = new Rectangle(
			minX, minY,
			(maxX-minX+1),
			(maxY-minY+1));
	//	System.out.println( minX +"\t"+ maxX 
	//			+"\t"+ (maxX-minX+1) 
	//			+"\t"+ (maxY-minY+1));
		Point2D p0 = gridProj.getRefXY(
			new Point( (minX+maxX)/2, (minY+maxY)/2 ));
		Point2D p1 = gridProj.getRefXY(
			new Point( minX, minY ));
		Point2D p2 = gridProj.getRefXY(
			new Point( maxX, maxY ));
System.out.println( p1.getX() +"\t"+ p2.getX() +"\t"+ p2.getY() +"\t"+ p1.getY());
		int mnX = (int)Math.floor(minX/320.);
		int mxX = (int)Math.ceil(maxX/320.);
		int mnY = (int)Math.floor(minY/320.);
		int mxY = (int)Math.ceil(maxY/320.);
		TiledGrid grd = new TiledGrid( gridProj, bounds, tileIO, 320,
				(mxX-mnX+1)*(mxY-mnY+1), null);
		double[] range = grd.getRange();
		System.out.println( p0.getX() +"\t"+ p0.getY() 
				+"\t"+ res
				+"\t"+ minX 
				+"\t"+ minY 
				+"\t"+ (maxX-minX+1) 
				+"\t"+ (maxY-minY+1) 
				+"\t"+ range[0]
				+"\t"+ range[1] );
		
		return grd;
	}
	
	public static double[] getMapWESN(MapProjection gridProj,
			MapProjection mapProj,
			double grid_x0,
			double grid_y0,
			double width,
			double height)
	{
		double x0, x1, y0, y1;
		x0 = y0 = Double.MAX_VALUE;
		x1 = y1 = - Double.MAX_VALUE;
		
		Point2D p;
		Point2D.Double pt = new Point2D.Double();
		for( int x=0 ; x<width ; x++) {
			pt.x = x + grid_x0;
			pt.y = grid_y0;
			p = mapProj.getMapXY(gridProj.getRefXY(pt));
			
			x0 = Math.min(x0, p.getX());
			x1 = Math.max(x1, p.getX());
			y0 = Math.min(y0, p.getY());
			y1 = Math.max(y1, p.getY());
			
			pt.y = height + grid_y0 - 1;
			p = mapProj.getMapXY(gridProj.getRefXY(pt));
			
			x0 = Math.min(x0, p.getX());
			x1 = Math.max(x1, p.getX());
			y0 = Math.min(y0, p.getY());
			y1 = Math.max(y1, p.getY());
		}
		for( int y=0 ; y<height ; y++) {
			pt.x = grid_x0;
			pt.y = y + grid_y0;
			p = mapProj.getMapXY(gridProj.getRefXY(pt));
			
			x0 = Math.min(x0, p.getX());
			x1 = Math.max(x1, p.getX());
			y0 = Math.min(y0, p.getY());
			y1 = Math.max(y1, p.getY());
			
			pt.x = width + grid_x0 - 1;
			p = mapProj.getMapXY(gridProj.getRefXY(pt));
			
			x0 = Math.min(x0, p.getX());
			x1 = Math.max(x1, p.getX());
			y0 = Math.min(y0, p.getY());
			y1 = Math.max(y1, p.getY());
		}
		
		return new double[] {x0, x1, y0, y1};
	}
}

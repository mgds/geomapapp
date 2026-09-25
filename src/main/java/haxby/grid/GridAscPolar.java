package haxby.grid;

import haxby.proj.PolarStereo;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.grid.ASC_PolarGrid;
import org.geomapapp.grid.Grid2D;
import org.geomapapp.grid.Grid2D.Float;

public class GridAscPolar {
	public static void main(String[] args) {
		if( args.length != 4  && args.length != 5) {
			System.out.println("usage: java GridAscPolar dir asc_file scaleLat weight [pole]");
			System.out.println("\t where pole is optional and 0 for SP and 1 for NP");
			System.out.println("\t pole defaults to 0");
			System.exit(0);
		}
		System.out.println(args[0]);

		boolean southPole = true;
		if (args.length == 5)
			southPole = Integer.parseInt(args[4]) == 0;

		String pole;
		PolarStereo proj, proj4;
		if (southPole) {
			proj = new PolarStereo( new java.awt.Point(0, 0),
					180., 50., -71.,
					PolarStereo.SOUTH,
					PolarStereo.WGS84);
			proj4 = new PolarStereo( new java.awt.Point(0, 0),
					180., 200., -71.,
					PolarStereo.SOUTH,
					PolarStereo.WGS84);
			pole = "SP";
		} else {
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
		int nLevel = 0;
		int nGrid = 1024;
		while( nGrid>8 ) {
			nLevel++;
			nGrid /= 8;
		}
		int nLevel4 = 0;
		int nGrid4 = 256; // Check this 
		while( nGrid4>8 ) {
			nLevel4++;
			nGrid4 /= 8;
		}

		// nLevel = 3, nGrid = 2
		// nLevel4 = 2, nGrid4 = 4 
		// nGrid + nGrid4 is not used again

		try {
			double wt = Double.parseDouble(args[3]);

			// Loads the ASCII grid, with header as first 6 lines
			ASC_PolarGrid grid = new ASC_PolarGrid(new File(args[1]));
			grid.readHeader();
			grid.setProjection(
					new PolarStereo(new Point(0,0),
						southPole ? 180. : 0.,
						grid.cell_size,
						Double.parseDouble( args[2] ),
						southPole ? PolarStereo.SOUTH : PolarStereo.NORTH,
						PolarStereo.WGS84)
			);
			Grid2D.Float grid2D = (Float) grid.getGrid();

			float[] gridZ = grid2D.getBuffer();
			for(int i=0 ; i<gridZ.length ; i++) {
				if( gridZ[i]==0f ) gridZ[i] = Float.NaN;
			}
			MapProjection gridProj = grid2D.getProjection();
			Rectangle bounds = grid2D.getBounds();
	System.out.println( "Width: " + bounds.width  +"\t"+ " Height: " + bounds.height +"\t"+ "Length: " + gridZ.length );

			// Mask functions use coordinates from 0 to width and 0 to height.  If the imported grid does
			//  not have it's origin at the upper corner then the x and y values must be shifted when dealing with
			//	the mask.  Shifted by the Grid x and y coordinates (dim.x, dim.y)
		float[] mask = GridMask.gridDistance( gridZ, bounds.width, bounds.height, 10f, false);
			int x1, y1, x2, y2;

			double[] mapWESN = getMapWESN(gridProj, proj4, bounds.x, bounds.y, bounds.width, bounds.height);

			x1 = (int) Math.floor(mapWESN[0]);
			x2 = (int) Math.ceil(mapWESN[1]);
			y1 = (int) Math.floor(mapWESN[2]);
			y2 = (int) Math.ceil(mapWESN[3]);

			Point p = new Point();
			Point2D.Double map;
	System.out.println( x1 +"\t"+ x2 +"\t"+ y1 +"\t"+ y2 +"\t"+ (1 + (x2/320) - (x1/320)) +" by " + (1 + (y2/320) - (y1/320)) + " grids" );
			GridderZW gridder = new GridderZW( 320, 3, //  + (x2/320) - (x1/320),
					nLevel4, proj4, dir+"/" + pole + "_320_200");

			int yy1 = 320*(int)Math.floor(y1/320.);
			int yy2 = 320*(int)Math.floor(y2/320.);
			int xx1 = 320*(int)Math.floor(x1/320.);
			int xx2 = 320*(int)Math.floor(x2/320.);
	System.out.println( xx1 +"\t"+ xx2 +"\t"+ yy1 +"\t"+ yy2);
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
							map = (Point2D.Double)gridProj.getMapXY(proj4.getRefXY(p));
//	if( x==tx1)System.out.println( x +"\t"+ y +"\t"+ proj4.getRefXY(p).getX() +"\t"+ proj4.getRefXY(p).getY() +"\t"+ map.getX() +"\t"+ map.getY());
							double z = grid2D.valueAt(map.x, map.y);
							if(Double.isNaN(z)) continue;

							// Subtract the grid origin from the grid coordinate to move
							//	the origin to 0,0 when dealing with the mask
							double weight = Interpolate.bicubic( mask, 
								bounds.width, bounds.height,
								map.x - bounds.x, map.y - bounds.y);
							if( weight<=0. )continue;
							if( weight>1. )weight=1.;
							int tx = x ;
							gridder.addPoint(tx, y, z, wt*weight);
						}
					}
				}
			}
			System.out.println("gridder4 finished");
			gridder.finish();

			mapWESN = getMapWESN(gridProj, proj, bounds.x, bounds.y, bounds.width, bounds.height);

			x1 = (int) Math.floor(mapWESN[0]);
			x2 = (int) Math.ceil(mapWESN[1]);
			y1 = (int) Math.floor(mapWESN[2]);
			y2 = (int) Math.ceil(mapWESN[3]);

			p = new Point();
	System.out.println( x1 +"\t"+ x2 +"\t"+ y1 +"\t"+ y2 +"\t"+ (1 + (x2/320) - (x1/320)) +" by " + (1 + (y2/320) - (y1/320)) + " grids" );
			gridder = new GridderZW( 320, 3, // + (x2/320) - (x1/320),
					nLevel, proj, dir+"/" + pole + "_320_50");

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
							map = (Point2D.Double)gridProj.getMapXY(proj.getRefXY(p));
							double z = grid2D.valueAt(map.x, map.y);
							if(Double.isNaN(z)) continue;

							// Subtract the grid origin from the grid coordinate to move
							//	the origin to 0,0 when dealing with the mask
							double weight = Interpolate.bicubic( mask, 
										bounds.width, bounds.height,
										map.x - bounds.x, map.y - bounds.y);
							if( weight<=0. )continue;
							if( weight>1. )weight=1.;
							int tx = x ;
							gridder.addPoint(tx, y, z, wt*weight);
						}
					}
				}
			}
			gridder.finish();
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(0);
		}
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

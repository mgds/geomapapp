package haxby.grid;

import haxby.proj.PolarStereo;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import org.geomapapp.geom.CylindricalProjection;
import org.geomapapp.geom.MapProjection;
import org.geomapapp.geom.RectangularProjection;
import org.geomapapp.grid.Grd;
import org.geomapapp.grid.GrdProperties;
import org.geomapapp.grid.Grid2D;

public class GridGrdPolar {
	public static void main(String[] args) {
		System.out.println(args[0]);
		if( args.length != 3 && args.length != 4) {
			System.out.println("usage: java GridGrd1SP dir filenames_file [pole]");
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
			BufferedReader in = new BufferedReader(
				new FileReader( args[1] ));
			String s;
			while( (s=in.readLine()) != null) {
				String[] sArr = s.split("\t");
				
				File file = new File(sArr[0]);
				double wt = Double.parseDouble(sArr[2]);
				
				System.out.println(file.getAbsolutePath());
				
//				XGrid grid = mercator ?
//					XGrid.getGrd1M( file, sign,
//						Integer.parseInt(st.nextToken()),
//						Double.parseDouble(st.nextToken()))
//					:
//						
//					XGrid.getGrd1(file, sign);

				GrdProperties gridP = new GrdProperties(sArr[0]);
				Grid2D.Float grid = Grd.readGrd( sArr[0], null, gridP, false, null);
				
				float[] gridZ = grid.getBuffer();
				for(int i=0 ; i<gridZ.length ; i++) {
					if( gridZ[i]==0f ) gridZ[i] = Float.NaN;
				}
				
				MapProjection gridProj = grid.getProjection();
				Rectangle bounds = grid.getBounds();

				System.out.println( "Width: " + bounds.width  +"\t"+ " Height: " + bounds.height +"\t"+ "Length: " + gridZ.length );
		
				Point2D ul = gridProj.getRefXY( new Point(bounds.x, bounds.y) );
				Point2D lr = gridProj.getRefXY( 
						new Point(bounds.x + bounds.width-1, bounds.y + bounds.height-1) );
				
				double wrap;
				wrap = 360.*(bounds.width-1.)/(lr.getX()-ul.getX());
				if (!gridProj.isCylindrical())
					wrap = -1;
		
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
								Point2D p2 = proj4.getRefXY(p);
								map = (Point2D.Double)gridProj.getMapXY(p2);
								if( wrap>0. ) {
									while(map.x>=bounds.x+bounds.width)map.x-=wrap;
									while(map.x<bounds.x)map.x+=wrap;
								}
								
//	if( x==tx1)System.out.println( x +"\t"+ y +"\t"+ proj4.getRefXY(p).getX() +"\t"+ proj4.getRefXY(p).getY() +"\t"+ map.getX() +"\t"+ map.getY());
								double z = grid.valueAt(map.x, map.y);
								if(Double.isNaN(z)) continue;
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
				System.out.println("gridder4 finished");
		
				mapWESN = getMapWESN(gridProj, proj4, bounds.x, bounds.y, bounds.width, bounds.height);
				
				x1 = (int) Math.floor(mapWESN[0]);
				x2 = (int) Math.ceil(mapWESN[1]);
				y1 = (int) Math.floor(mapWESN[2]);
				y2 = (int) Math.ceil(mapWESN[3]);
				
				p = new Point();
		System.out.println( x1 +"\t"+ x2 +"\t"+ y1 +"\t"+ y2 +"\t"+ (1 + (x2/320) - (x1/320)) +" by " + (1 + (y2/320) - (y1/320)) + " grids" );
				gridder = new GridderZW( 320, 3, // + (x2/320) - (x1/320),
						nLevel, proj, dir+"/" + pole + "_320_50");
//				width = grid.getWidth();
//				height = grid.getHeight();
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
								Point2D p2 = proj.getRefXY(p);
								p2.setLocation(p2.getX() - 360, p2.getY());
								map = (Point2D.Double)gridProj.getMapXY(p2);
								if( wrap>0. ) {
									while(map.x>=bounds.x+bounds.width)map.x-=wrap;
									while(map.x<bounds.x)map.x+=wrap;
								}
								
								double z = grid.valueAt(map.x, map.y);
								if(Double.isNaN(z)) continue;
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
				System.out.println("gridder finished");
			}
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

package haxby.grid;

import haxby.proj.PolarStereo;
import haxby.proj.Projection;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import org.geomapapp.geom.MapProjection;

/**
 *	Can only bring in grd1 files in a Mercator or Rectangular projection and
 *	creates ZW grids (x grids) at the 1 and 4 resolutions in the folders
 *	 SP_320_50 AND SP_320_200
 *
 *	Grd1 files are made from netcdf files with grdreformat
 *
 *	For example 
 *		grdreformat A.grd A.grd1=1 
 *
 *	This works with gmt version 3 netcdf and not sure about gmt version 4.
 *
 *	If have difficulty importing a global grid try reporojecting the grid from 0-360
 *	to -180/180  with grdedit
 *
 *	for example:
 *		grdedit sedthick_world_gma.grd -R-180/180/-78/81 -S -V
 */
public class GridGrd1SP {
	public static void main(String[] args) {
		System.out.println(args[0]);
		if( args.length != 2 ) {
			System.out.println("usage: java GridGrd1SP dir filenames_file");
			System.exit(0);
		}
		PolarStereo proj = new PolarStereo( new java.awt.Point(0, 0),
				180., 50., -71.,
				PolarStereo.SOUTH,
				PolarStereo.WGS84);
		PolarStereo proj4 = new PolarStereo( new java.awt.Point(0, 0),
				180., 200., -71., //
				PolarStereo.SOUTH,
				PolarStereo.WGS84);
		
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
				StringTokenizer st = new StringTokenizer(s);
				
				File file = new File(sArr[0]);
				boolean sign = (Integer.parseInt(sArr[1])==-1);
				double wt = Double.parseDouble(sArr[2]);
				boolean mercator = false;
				if ( sArr.length > 3 ) {
					mercator = sArr[3].equalsIgnoreCase("M");
				}
				
				System.out.println(file.getAbsolutePath());
				
				XGrid grid = mercator ?
					XGrid.getGrd1M( file, sign,
						Integer.parseInt(st.nextToken()),
						Double.parseDouble(st.nextToken()))
					:
					XGrid.getGrd1(file, sign);

				float[] gridZ = grid.getGrid();
				for(int i=0 ; i<gridZ.length ; i++) {
					if( gridZ[i]==0f ) gridZ[i] = Float.NaN;
				}
				Projection gp = grid.getProjection();
				Dimension dim = grid.getSize();
		System.out.println( "Width: " + dim.width  +"\t"+ " Height: " + dim.height +"\t"+ "Length: " + gridZ.length );
				float[] mask = GridMask.gridDistance( gridZ, dim.width, dim.height, 10f, false);
				int x1, y1, x2, y2;

				if( gp.isCylindrical() ) {
					Point2D ul = gp.getRefXY(new Point2D.Double(0., 0.));
					Point2D lr = gp.getRefXY(new Point2D.Double(dim.getWidth()-1., 
								dim.getHeight()-1.));
					ul = proj4.getMapXY(ul);
					x1 = (int)Math.ceil( ul.getX() );
					y1 = (int)Math.ceil( ul.getY() );
					ul = proj4.getMapXY(lr);
					x2 = (int)Math.ceil( ul.getX() );
					if( x2<x1 ) x2 += 320*256;
					y2 = (int)Math.ceil( ul.getY() );
				} else {
					Point2D.Double pt = new Point2D.Double(0., 0.);
					Point2D p = gp.getRefXY(pt);
		System.out.println( "origin = \t"+ p.getX() +"\t"+ p.getY());
					p = proj4.getMapXY(p);
					double xmin, xmax, ymin, ymax;
					xmin = xmax = p.getX();
					ymin = ymax = p.getY();
					for( int x=0 ; x<dim.width ; x++) {
						pt.x = x;
						pt.y = 0.;
						p = proj4.getMapXY(gp.getRefXY(pt));

						if( p.getX()<xmin )xmin = p.getX();
						else if( p.getX()>xmax )xmax = p.getX();
						if( p.getY()<ymin )ymin = p.getY();
						else if( p.getY()>ymax )ymax = p.getY();
						pt.y = dim.getHeight()-1.;
						p = proj4.getMapXY(gp.getRefXY(pt));

						if( p.getX()<xmin )xmin = p.getX();
						else if( p.getX()>xmax )xmax = p.getX();
						if( p.getY()<ymin )ymin = p.getY();
						else if( p.getY()>ymax )ymax = p.getY();
					}
					for( int y=0 ; y<dim.height ; y++) {
						pt.x = 0.;
						pt.y = y;
						p = proj4.getMapXY(gp.getRefXY(pt));

						if( p.getX()<xmin )xmin = p.getX();
						else if( p.getX()>xmax )xmax = p.getX();
						if( p.getY()<ymin )ymin = p.getY();
						else if( p.getY()>ymax )ymax = p.getY();
						pt.x = dim.getWidth()-1.;
						p = proj4.getMapXY(gp.getRefXY(pt));

						if( p.getX()<xmin )xmin = p.getX();
						else if( p.getX()>xmax )xmax = p.getX();
						if( p.getY()<ymin )ymin = p.getY();
						else if( p.getY()>ymax )ymax = p.getY();
					}
					x1 = (int)Math.ceil( xmin );
					y1 = (int)Math.ceil( ymin );
					x2 = (int)Math.ceil( xmax );
					if( x2<x1 ) x2 += 320*256;
					y2 = (int)Math.ceil( ymax );
				}
				
				
				Point p = new Point();
				Point2D.Double map;
		System.out.println( x1 +"\t"+ x2 +"\t"+ y1 +"\t"+ y2 +"\t"+ (1 + (x2/320) - (x1/320)) +" by " + (1 + (y2/320) - (y1/320)) + " grids" );
				GridderZW gridder = new GridderZW( 320, 3, //  + (x2/320) - (x1/320),
						nLevel4, proj4, dir+"/SP_320_200");
				
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
								map = (Point2D.Double)gp.getMapXY(p2);
//	if( x==tx1)System.out.println( x +"\t"+ y +"\t"+ proj4.getRefXY(p).getX() +"\t"+ proj4.getRefXY(p).getY() +"\t"+ map.getX() +"\t"+ map.getY());
								double z = grid.sample(map.x, map.y);
								if(Double.isNaN(z)) continue;
								double weight = Interpolate.bicubic( mask, 
									dim.width, dim.height,
									map.x, map.y);
								if( weight<=0. )continue;
								if( weight>1. )weight=1.;
								int tx = x ;
								gridder.addPoint(tx, y, z, wt*weight);
							}
						}
					}
				}
				gridder.finish();
				if( gp.isCylindrical() ) {
					Point2D ul = gp.getRefXY(new Point2D.Double(0., 0.));
					Point2D lr = gp.getRefXY(new Point2D.Double(dim.getWidth()-1., 
								dim.getHeight()-1.));
					ul = proj.getMapXY(ul);
					x1 = (int)Math.ceil( ul.getX() );
					y1 = (int)Math.ceil( ul.getY() );
					ul = proj.getMapXY(lr);
					x2 = (int)Math.ceil( ul.getX() );
					if( x2<x1 ) x2 += 320*256;
					y2 = (int)Math.ceil( ul.getY() );
				} else {
					Point2D.Double pt = new Point2D.Double(0., 0.);
					Point2D pp = proj.getMapXY(gp.getRefXY(pt));
					double xmin, xmax, ymin, ymax;
					xmin = xmax = pp.getX();
					ymin = ymax = pp.getY();
					for( int x=0 ; x<dim.width ; x++) {
						pt.x = x;
						pt.y = 0.;
						pp = proj.getMapXY(gp.getRefXY(pt));

						if( pp.getX()<xmin )xmin = pp.getX();
						else if( pp.getX()>xmax )xmax = pp.getX();
						if( pp.getY()<ymin )ymin = pp.getY();
						else if( pp.getY()>ymax )ymax = pp.getY();
						pt.y = dim.getHeight()-1.;
						pp = proj.getMapXY(gp.getRefXY(pt));

						if( pp.getX()<xmin )xmin = pp.getX();
						else if( pp.getX()>xmax )xmax = pp.getX();
						if( pp.getY()<ymin )ymin = pp.getY();
						else if( pp.getY()>ymax )ymax = pp.getY();
					}
					for( int y=0 ; y<dim.height ; y++) {
						pt.x = 0.;
						pt.y = y;
						pp = proj.getMapXY(gp.getRefXY(pt));

						if( pp.getX()<xmin )xmin = pp.getX();
						else if( pp.getX()>xmax )xmax = pp.getX();
						if( pp.getY()<ymin )ymin = pp.getY();
						else if( pp.getY()>ymax )ymax = pp.getY();
						pt.x = dim.getWidth()-1.;
						pp = proj.getMapXY(gp.getRefXY(pt));

						if( pp.getX()<xmin )xmin = pp.getX();
						else if( pp.getX()>xmax )xmax = pp.getX();
						if( pp.getY()<ymin )ymin = pp.getY();
						else if( pp.getY()>ymax )ymax = pp.getY();
					}
					x1 = (int)Math.ceil( xmin );
					y1 = (int)Math.ceil( ymin );
					x2 = (int)Math.ceil( xmax );
					if( x2<x1 ) x2 += 320*256;
					y2 = (int)Math.ceil( ymax );
				}
				
				p = new Point();
		System.out.println( x1 +"\t"+ x2 +"\t"+ y1 +"\t"+ y2 +"\t"+ (1 + (x2/320) - (x1/320)) +" by " + (1 + (y2/320) - (y1/320)) + " grids" );
				gridder = new GridderZW( 320, 3, // + (x2/320) - (x1/320),
						nLevel, proj, dir+"/SP_320_50");
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
								map = (Point2D.Double)gp.getMapXY(proj.getRefXY(p));
								double z = grid.sample(map.x, map.y);
								if(Double.isNaN(z)) continue;
								double weight = Interpolate.bicubic( mask, 
											dim.width, dim.height,
											map.x, map.y);
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
	
	
}

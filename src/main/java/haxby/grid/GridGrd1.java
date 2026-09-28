package haxby.grid;

import haxby.proj.*;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

public class GridGrd1 {
	public static void main(String[] args) {
		if( args.length != 2 ) {
			System.out.println("usage: java GridGrd1 dir filenames_file");
			System.exit(0);
		}
		Mercator proj = ProjectionFactory.getMercator(1024*320);
		Mercator proj4 = ProjectionFactory.getMercator(256*320);
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
				StringTokenizer st = new StringTokenizer(s);
				File file = new File( st.nextToken() );
				boolean sign = (Integer.parseInt(st.nextToken())==-1);
				double wt = Double.parseDouble( st.nextToken() );
				boolean mercator = st.hasMoreTokens()
					&& st.nextToken().equalsIgnoreCase("M");
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
		System.out.println( dim.width  +"\t"+ dim.height +"\t"+ gridZ.length );
				float[] mask = GridMask.gridDistance( gridZ, dim.width, dim.height, 10f, false);
				int x1, y1, x2, y2;
				int test = 320*256;
				double xtest, ytest;
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
						xtest = p.getX();
						ytest = p.getY();
						if(xtest<xmin-test/2.) xtest += test;
						if(xtest>xmax+test/2.) xtest -= test;
						p.setLocation( xtest, ytest);
						if( p.getX()<xmin )xmin = p.getX();
						else if( p.getX()>xmax )xmax = p.getX();
						if( p.getY()<ymin )ymin = p.getY();
						else if( p.getY()>ymax )ymax = p.getY();
						pt.y = dim.getHeight()-1.;
						p = proj4.getMapXY(gp.getRefXY(pt));
						xtest = p.getX();
						ytest = p.getY();
						if(xtest<xmin-test/2.) xtest += test;
						if(xtest>xmax+test/2.) xtest -= test;
						p.setLocation( xtest, ytest);
						if( p.getX()<xmin )xmin = p.getX();
						else if( p.getX()>xmax )xmax = p.getX();
						if( p.getY()<ymin )ymin = p.getY();
						else if( p.getY()>ymax )ymax = p.getY();
					}
					for( int y=0 ; y<dim.height ; y++) {
						pt.x = 0.;
						pt.y = y;
						p = proj4.getMapXY(gp.getRefXY(pt));
						xtest = p.getX();
						ytest = p.getY();
						if(xtest<xmin-test/2.) xtest += test;
						if(xtest>xmax+test/2.) xtest -= test;
						p.setLocation( xtest, ytest);
						if( p.getX()<xmin )xmin = p.getX();
						else if( p.getX()>xmax )xmax = p.getX();
						if( p.getY()<ymin )ymin = p.getY();
						else if( p.getY()>ymax )ymax = p.getY();
						pt.x = dim.getWidth()-1.;
						p = proj4.getMapXY(gp.getRefXY(pt));
						xtest = p.getX();
						ytest = p.getY();
						if(xtest<xmin-test/2.) xtest += test;
						if(xtest>xmax+test/2.) xtest -= test;
						p.setLocation( xtest, ytest);
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
		System.out.println( x1 +"\t"+ x2 +"\t"+ y1 +"\t"+ y2 +"\t"+ (1 + (x2/320) - (x1/320)) +" grids" );
				GridderZW gridder = new GridderZW( 320, 1, //  + (x2/320) - (x1/320),
						nLevel4, proj4, dir+"/merc_320_256");
				int width = grid.getWidth();
				int height = grid.getHeight();
				int yy1 = 320*(int)Math.floor(y1/320.);
				int yy2 = 320*(int)Math.floor(y2/320.);
				int xx1 = 320*(int)Math.floor(x1/320.);
				int xx2 = 320*(int)Math.floor(x2/320.);
		System.out.println( xx1 +"\t"+ xx2 +"\t"+ yy1 +"\t"+ yy2);
				for(int yy=yy1 ; yy<=yy2 ; yy+=320){
					int ty1 = Math.max(y1,yy);
					int ty2 = Math.min(y2,yy+320);
					for(int xx=xx1 ; xx<=xx2 ; xx+=320) {
						int tx1 = Math.max(x1,xx);
						int tx2 = Math.min(x2,xx+320);
		System.out.println( tx1 +"\t"+ tx2 +"\t"+ ty1 +"\t"+ ty2);
						for( int y=ty1 ; y<ty2 ; y++) {
							for( int x=tx1 ; x<tx2 ; x++) {
								p.y = y;
								p.x = x;
								map = (Point2D.Double)gp.getMapXY(proj4.getRefXY(p));
//	if( x==tx1)System.out.println( x +"\t"+ y +"\t"+ proj4.getRefXY(p).getX() +"\t"+ proj4.getRefXY(p).getY() +"\t"+ map.getX() +"\t"+ map.getY());
								double z = grid.sample(map.x, map.y);
								if(Double.isNaN(z)) continue;
								double weight = Interpolate.bicubic( mask, 
									dim.width, dim.height,
									map.x, map.y);
								if( weight<=0. )continue;
								if( weight>1. )weight=1.;
								int tx = x ;
								if( tx>=test )tx-=test;
								gridder.addPoint(tx, y, z, wt*weight);
							}
						}
					}
				}
				gridder.finish();
				test = 1024*320;
				if( gp.isCylindrical() ) {
					Point2D ul = gp.getRefXY(new Point2D.Double(0., 0.));
					Point2D lr = gp.getRefXY(new Point2D.Double(dim.getWidth()-1., 
								dim.getHeight()-1.));
					ul = proj.getMapXY(ul);
					x1 = (int)Math.ceil( ul.getX() );
					y1 = (int)Math.ceil( ul.getY() );
					ul = proj.getMapXY(lr);
					x2 = (int)Math.ceil( ul.getX() );
					
//					***** GMA 1.6.0: TESTING
					
//					if( x2<x1 ) x2 += 320*256;
					
					if( x2<x1 ) x2 += 320*1024;
					
//					***** GMA 1.6.0
					
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
						xtest = pp.getX();
						ytest = pp.getY();
						if(xtest<xmin-test/2.) xtest += test;
						if(xtest>xmax+test/2.) xtest -= test;
						pp.setLocation( xtest, ytest);
						if( pp.getX()<xmin )xmin = pp.getX();
						else if( pp.getX()>xmax )xmax = pp.getX();
						if( pp.getY()<ymin )ymin = pp.getY();
						else if( pp.getY()>ymax )ymax = pp.getY();
						pt.y = dim.getHeight()-1.;
						pp = proj.getMapXY(gp.getRefXY(pt));
						xtest = pp.getX();
						ytest = pp.getY();
						if(xtest<xmin-test/2.) xtest += test;
						if(xtest>xmax+test/2.) xtest -= test;
						pp.setLocation( xtest, ytest);
						if( pp.getX()<xmin )xmin = pp.getX();
						else if( pp.getX()>xmax )xmax = pp.getX();
						if( pp.getY()<ymin )ymin = pp.getY();
						else if( pp.getY()>ymax )ymax = pp.getY();
					}
					for( int y=0 ; y<dim.height ; y++) {
						pt.x = 0.;
						pt.y = y;
						pp = proj.getMapXY(gp.getRefXY(pt));
						xtest = pp.getX();
						ytest = pp.getY();
						if(xtest<xmin-test/2.) xtest += test;
						if(xtest>xmax+test/2.) xtest -= test;
						pp.setLocation( xtest, ytest);
						if( pp.getX()<xmin )xmin = pp.getX();
						else if( pp.getX()>xmax )xmax = pp.getX();
						if( pp.getY()<ymin )ymin = pp.getY();
						else if( pp.getY()>ymax )ymax = pp.getY();
						pt.x = dim.getWidth()-1.;
						pp = proj.getMapXY(gp.getRefXY(pt));
						xtest = pp.getX();
						ytest = pp.getY();
						if(xtest<xmin-test/2.) xtest += test;
						if(xtest>xmax+test/2.) xtest -= test;
						pp.setLocation( xtest, ytest);
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
		System.out.println( x1 +"\t"+ x2 +"\t"+ y1 +"\t"+ y2 +"\t"+ (1 + (x2/320) - (x1/320)) +" grids" );
				gridder = new GridderZW( 320, 1, // + (x2/320) - (x1/320),
						nLevel, proj, dir+"/merc_320_1024");
				width = grid.getWidth();
				height = grid.getHeight();
				yy1 = 320*(int)Math.floor(y1/320.);
				yy2 = 320*(int)Math.floor(y2/320.);
				xx1 = 320*(int)Math.floor(x1/320.);
				xx2 = 320*(int)Math.floor(x2/320.);
				for(int yy=yy1 ; yy<=yy2 ; yy+=320){
					int ty1 = Math.max(y1,yy);
					int ty2 = Math.min(y2,yy+320);
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
								if( tx>=test )tx-=test;
								gridder.addPoint(tx, y, z, wt*weight);
							}
						}
					}
				}
				gridder.finish();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(0);
		}
	}
}

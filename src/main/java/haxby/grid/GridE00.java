package haxby.grid;

import haxby.proj.*;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

public class GridE00 {
	public static void main(String[] args) {
		if( args.length != 2 ) {
			System.out.println("usage: java GridE00 dir filenames_file");
			System.exit(0);
		}
		Mercator proj0 = ProjectionFactory.getMercator(16*1024*320);
		Mercator proj1 = ProjectionFactory.getMercator(1024*320);
		Mercator proj4 = ProjectionFactory.getMercator(256*320);
		String dir = args[0];
		try {
			BufferedReader in = new BufferedReader(
				new FileReader( args[1] ));
			String s;
			while( (s=in.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(s);
				File file = new File( st.nextToken() );
				boolean changeSign =  Integer.parseInt(st.nextToken())<0;
				double wt = Double.parseDouble( st.nextToken() );
				int res = Integer.parseInt(st.nextToken());
				Mercator proj = ProjectionFactory.getMercator( 320*res );
				int nLevel = 0;
				int nGrid = res;
				while( nGrid>8 ) {
					nLevel++;
					nGrid /= 8;
				}
				XGrid e00 = XGrid.getE00( file, changeSign );
				Projection gp = e00.getProjection();
				Dimension dim = e00.getSize();
				double xmin, xmax, ymin, ymax;
				Point p = new Point(0,0);
				Point2D.Double map = (Point2D.Double)proj.getMapXY(
								gp.getRefXY( p ));
				xmin = xmax = map.x;
				ymin = ymax = map.y;
				for( int x=0 ; x<dim.width ; x++ ) {
					p.x = x;
					p.y = 0;
					map = (Point2D.Double)proj.getMapXY(
								gp.getRefXY( p ));
					if( map.x>xmax ) xmax = map.x;
					else if( map.x<xmin ) xmin = map.x;
					if( map.y>ymax ) ymax = map.y;
					else if( map.y<ymin ) ymin = map.y;
					p.y = dim.height-1;
					map = (Point2D.Double)proj.getMapXY(
								gp.getRefXY( p ));
					if( map.x>xmax ) xmax = map.x;
					else if( map.x<xmin ) xmin = map.x;
					if( map.y>ymax ) ymax = map.y;
					else if( map.y<ymin ) ymin = map.y;
				}
				for( int y=0 ; y<dim.height ; y++ ) {
					p.y = y;
					p.x = 0;
					map = (Point2D.Double)proj.getMapXY(
								gp.getRefXY( p ));
					if( map.x>xmax ) xmax = map.x;
					else if( map.x<xmin ) xmin = map.x;
					if( map.y>ymax ) ymax = map.y;
					else if( map.y<ymin ) ymin = map.y;
					p.x = dim.width-1;
					map = (Point2D.Double)proj.getMapXY(
								gp.getRefXY( p ));
					if( map.x>xmax ) xmax = map.x;
					else if( map.x<xmin ) xmin = map.x;
					if( map.y>ymax ) ymax = map.y;
					else if( map.y<ymin ) ymin = map.y;
				}
				int x1 = (int)Math.floor( xmin );
				int y1 = (int)Math.floor( ymin );
				int x2 = (int)Math.ceil( xmax );
				int y2 = (int)Math.ceil( ymax );
				int igx1 = x1/320;
				int igx2 = x2/320;
				int igy1 = (int)Math.floor(y1/320.);
				int igy2 = (int)Math.floor(y2/320.);
			System.out.println( x1 +"\t"+ x2 +"\t"+ y1 +"\t"+ y2);
				GridderZW gridder = new GridderZW( 320, 3,
						nLevel, proj, dir+"/merc_320_" + res);
				gridder.setWrap( res*320 );
				float[] gridZ = e00.getGrid();
				float[] mask = GridMask.gridDistance( gridZ, 
							dim.width, dim.height, 
							10f, false);
				for( int igy=igy1 ; igy<=igy2 ; igy++ ) {
					int yy1 = igy*320;
					int yy2 = yy1+319;
					if(yy1<y1) yy1=y1;
					if( yy2>y2 ) yy2=y2;
					for( int igx=igx1 ; igx<=igx2 ; igx++ ) {
						int xx1 = igx*320;
						int xx2 = xx1+319;
						if(xx1<x1) xx1=x1;
						if( xx2>x2 ) xx2=x2;
						p.x = xx1;
						p.y = yy1;
						map = (Point2D.Double)proj.getRefXY(p);
			System.out.println( xx1 +"\t"+ xx2 +"\t"+ yy1 +"\t"+ yy2 +"\t"+ map.getX() +"\t"+ map.getY());
						for(int y=yy1 ; y<=yy2 ; y++){
							for(int x=xx1 ; x<=xx2 ; x++) {
								p.y = y;
								p.x = x;
								map = (Point2D.Double)gp.getMapXY(proj.getRefXY(p));
								double z = e00.sample(map.x, map.y);
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
				gridder.finish();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(0);
		}
	}
}

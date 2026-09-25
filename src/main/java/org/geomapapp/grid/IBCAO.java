package org.geomapapp.grid;

import org.geomapapp.geom.*;
import org.geomapapp.image.*;
import org.geomapapp.util.*;

import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

public class IBCAO {
	PolarStereo proj;
	Grid2D.Short grid;
	Grid2D.Boolean landMask;
	public IBCAO() throws IOException {
	//	proj = new PolarStereo( new Point(1161, 1161),
		proj = new PolarStereo( new Point(0, 0),
				0., 2500., 75., PolarStereo.NORTH,
				PolarStereo.WGS84);
		
//		Rectangle( left, top, width, height ), confused why top is negative
//		Perhaps the IBCAO first row is the bottom line of the map
		grid = new Grid2D.Short( new Rectangle(-1161, -1161, 2323, 2323),
					proj);
		landMask = new Grid2D.Boolean( new Rectangle(-1161, -1161, 2323, 2323),
					proj);
		DataInputStream in = new DataInputStream(
			new BufferedInputStream(
					
//			***** GMA 1.6.0: TESTING
//			new FileInputStream("/scratch/ridgembs/bill/arctic/topo/IBCAO.short")));
			new FileInputStream("C:/Documents and Settings/akm/My Documents/arctic/IBCAO/IBCAO.short")));
//			***** GMA 1.6.0		
		
		for( int y=-1161 ; y<1162 ; y++ ) {
			for( int x=-1161 ; x<1162 ; x++ ) {
				short i = in.readShort();
				grid.setValue( x, y, i );
				landMask.setValue( x, y, i>=0 );
			}
		}
	}
	public Grid2D.Short getGrid() {
		return grid;
	}
	public Grid2D.Boolean getMask() {
		return landMask;
	}
	public static void main(String[] args) {
		try {
			IBCAO ibcao = new IBCAO();
			Grid2D.Short grid = ibcao.getGrid();
			MapProjection proj = grid.getProjection();
			Point2D p = proj.getRefXY( new Point(-1161,-1161) );
			System.out.println( p.getX() +"\t"+ p.getY() );
			p = proj.getRefXY( new Point(1161,0) );
			System.out.println( p.getX() +"\t"+ p.getY() );
			p = proj.getRefXY( new Point(320*4,0) );
			System.out.println( p.getX() +"\t"+ p.getY() );
			p = proj.getRefXY( new Point(320*5,0) );
			System.out.println( p.getX() +"\t"+ p.getY() );
			p = proj.getRefXY( new Point(320*6,0) );
			System.out.println( p.getX() +"\t"+ p.getY() );
			p = proj.getRefXY( new Point(320*7,0) );
			System.out.println( p.getX() +"\t"+ p.getY() );
			p = proj.getRefXY( new Point(320*8,0) );
			System.out.println( p.getX() +"\t"+ p.getY() );
			double y1 = p.getY();
		//	Palette land = new Palette( 2 );
		//	Palette ocean = new Palette( 1 );
		//	Grid2D.Boolean landMask = ibcao.getMask();
		//	GridRenderer gr = new GridRenderer( new Palette(0),
		//			4., 2500., new XYZ(-1., 1., 1.) );
		//	gr.setLandPalette(land);
		//	gr.setOceanPalette(ocean);
		//	BufferedImage image = gr.gridImage( grid, landMask );
		//	javax.imageio.ImageIO.write( image, "jpg", new File( "test.jpg" ));
			TileIO.Short tiler = new TileIO.Short(
					
//					***** GMA 1.6.0: TESTING
//					proj, "/scratch/ridgembs/bill/arctic/topo/NP_320/z_8", 320, 1);
					proj, "C:/Documents and Settings/akm/My Documents/arctic/topo/NP_320/z_8", 320, 1);
//					***** GMA 1.6.0
			
			for( int iy=-4 ; iy<4 ; iy++) {
				for( int ix=-4 ; ix<4 ; ix++) {
					int y0 = 320*iy;
					int x0 = 320*ix;
					Grid2D.Short g = (Grid2D.Short)tiler.createGridTile(x0, y0);
				int count=0;
					for( int y=y0 ; y<y0+320 ; y++) {
						for( int x=x0 ; x<x0+320 ; x++) {
							short i = grid.shortValue(x,y);
							if( i==g.NaN )continue;
							g.setValue(x, y, i);
							count++;
						}
					}
				System.out.println( tiler.getDirPath(x0,y0)+"/"+tiler.getName(x0,y0)+"\t"+ count);
					tiler.writeGridTile(g);
				}
			}
			Mercator merc = new Mercator( 0., 0., 320*1024/32, 0, Mercator.RANGE_0_to_360);
			y1 = merc.getY(y1-.1);
			double y2 = merc.getY(65.);
			Rectangle bounds = new Rectangle( 0, (int)Math.floor(y2), 10240, (int)Math.ceil(y1-y2));
			
			int nLevel = 0;
			int nGrid = 1024/32;
			while( nGrid>8 ) {
				nLevel++;
				nGrid /= 8;
			}
			TileIO.Short tilerM = new TileIO.Short(
					proj, 
					haxby.map.MapApp.TEMP_BASE_URL + "MapApp/merc_320_1024/multibeam/z_32",
					320, nLevel);
			
//			***** GMA 1.6.0: TESTING
			tilerM.setReadonly(true);
//			***** GMA 1.6.0
			
			TiledGrid tg = new TiledGrid( merc, bounds, tilerM, 320, 8, null );
			
//			***** GMA 1.6.0: TESTING
			tg.setReadonly(true);
//			***** GMA 1.6.0
			
			tg.setWrap( 10240);
			Grid2D.Short g = null;
			for( int iy=-8 ; iy<8 ; iy++) {
				for( int ix=-8 ; ix<8 ; ix++) {
					int y0 = 320*iy;
					int x0 = 320*ix;
					try {
						g = (Grid2D.Short)tiler.readGridTile(x0, y0);
					} catch(IOException e) {
						g = (Grid2D.Short)tiler.createGridTile(x0, y0);
					}
				int count=0;
					for( int y=y0 ; y<y0+320 ; y++) {
						for( int x=x0 ; x<x0+320 ; x++) {
							short i = grid.shortValue(x,y);
							if( i!=g.NaN )continue;
							p = merc.getMapXY(proj.getRefXY( new Point(x,y) ));
							double z = tg.valueAt(p.getX(), p.getY());
							if( Double.isNaN(z) ) continue;
							g.setValue(x, y, (short)Math.rint(z));
							count++;
						}
					}
				System.out.println( tiler.getDirPath(x0,y0)+"/"+tiler.getName(x0,y0)+"\t"+ count);
					tiler.writeGridTile(g);
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}

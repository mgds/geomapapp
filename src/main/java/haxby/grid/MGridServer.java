package haxby.grid;

import haxby.map.GridOverlay;
import haxby.proj.Projection;
import haxby.proj.ProjectionFactory;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

public class MGridServer {
	static String base = haxby.map.MapApp.TEMP_BASE_URL + "MapApp/";
	public static void setBaseURL( String baseURL ) {
		base = baseURL;
	}
	public static String getBaseURL() {
		return base;
	}
	/*
	 *  Not Currently used in GMA 6/10/09
	 */
	public static boolean getGrid(Rectangle2D rect, GridOverlay overlay, int mapRes) {
		double zoom = overlay.getXMap().getZoom();
		int res = mapRes;
		while(zoom*res/mapRes > 1.5 && res>1) {
			res /=2;
		}
		int scale = mapRes/res;
		int x = (int)Math.floor(scale*rect.getX());
		int y = (int)Math.floor(scale*(rect.getY()-260.));
		int width = (int)Math.ceil( scale*(rect.getX()+rect.getWidth()) ) - x;
		int height = (int)Math.ceil( scale*(rect.getY()-260.+rect.getHeight()) ) - y;
		Projection proj = ProjectionFactory.getMercator( 1024*320/res );
		int nLevel = 0;
		int nGrid = 1024/res;
		while( nGrid>8 ) {
			nLevel++;
			nGrid /= 8;
		}
		URLTilerZ tiler = null;
		int ng = 2 + width/320;
	//	System.out.println( x +"\t"+ y +"\t"+ width +"\t"+ height +"\t"+ scale +"\t"+ ng);
		try { 
			tiler = new URLTilerZ( 320, res, ng, nLevel, proj, base+"merc_320_1024" );
			tiler.setWrap( 1024*320/res);
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}
		XGrid_Z grid = new XGrid_Z( x, y, width, height, tiler );
		proj = grid.getProjection();
		if( res < 4 ) {
			int resA = 4;
			int scaleA = mapRes/4;
			int xA = (int)Math.floor(scaleA*rect.getX()-320.);
			int yA = (int)Math.floor(scaleA*(rect.getY()-260.));
			int widthA = (int)Math.ceil( scaleA*(rect.getX()+rect.getWidth()) ) - xA;
			int heightA = (int)Math.ceil( scaleA*(rect.getY()-260.+rect.getHeight()) ) - yA;
			Projection projA = ProjectionFactory.getMercator( 1024*320/4 );
			nLevel = 0;
			nGrid = 1024/4;
			while( nGrid>8 ) {
				nLevel++;
				nGrid /= 8;
			}
			tiler = null;
			ng = 2 + widthA/320;
	//	System.out.println("\t"+ xA +"\t"+ yA +"\t"+ widthA +"\t"+ heightA +"\t"+ scaleA +"\t"+ ng);
			try { 
				tiler = new URLTilerZ( 320, 4, ng, nLevel, projA, base+"merc_320_1024" );
			} catch (IOException ex) {
				ex.printStackTrace();
				return false;
			}
			XGrid_Z gridA = new XGrid_Z( xA-2, yA-2, widthA+4, heightA+4, tiler );
			tiler = null;
			float[] z = grid.getGrid();
			int k=0;
			Point p = new Point();
			for( int iy=0 ; iy<height ; iy++ ) {
				p.y = iy;
				for( int ix=0 ; ix<width ; ix++, k++) {
					if( Float.isNaN(z[k]) ) {
						p.x = ix;
						Point2D pt = gridA.getProjection().getMapXY(
								proj.getRefXY(p) );
						z[k] = gridA.valueAt( pt.getX(), pt.getY() );
					}
				}
			}
		}
		if( res < 32 ) {
			int resA = 32;
			int scaleA = mapRes/32;
			int xA = (int)Math.floor(scaleA*rect.getX());
			int yA = (int)Math.floor(scaleA*(rect.getY()-260.));
			int widthA = (int)Math.ceil( scaleA*(rect.getX()+rect.getWidth()) ) - xA;
			int heightA = (int)Math.ceil( scaleA*(rect.getY()-260.+rect.getHeight()) ) - yA;
			Projection projA = ProjectionFactory.getMercator( 1024*320/32 );
			nLevel = 0;
			nGrid = 1024/32;
			while( nGrid>8 ) {
				nLevel++;
				nGrid /= 8;
			}
			tiler = null;
			ng = 2 + widthA/320;
			try { 
				tiler = new URLTilerZ( 320, 32, ng, nLevel, projA, base+"merc_320_1024" );
			} catch (IOException ex) {
				ex.printStackTrace();
				return false;
			}
			XGrid_Z gridA = new XGrid_Z( xA-2, yA-2, widthA+4, heightA+4, tiler );
			tiler = null;
			float[] z = grid.getGrid();
			int k=0;
			Point p = new Point();
			for( int iy=0 ; iy<height ; iy++ ) {
				p.y = iy;
				for( int ix=0 ; ix<width ; ix++, k++) {
					if( Float.isNaN(z[k]) ) {
						p.x = ix;
						Point2D pt = gridA.getProjection().getMapXY(
								proj.getRefXY(p) );
						z[k] = gridA.valueAt( pt.getX(), pt.getY() );
					}
				}
			}
		}
		overlay.setGrid(grid);
		return true;
	}
}

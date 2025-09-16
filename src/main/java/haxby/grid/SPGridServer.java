package haxby.grid;

import haxby.map.GridOverlay;
import haxby.proj.PolarStereo;
import haxby.proj.Projection;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

public class SPGridServer {
	static String base = haxby.map.MapApp.TEMP_BASE_URL + "antarctic/SP_320_50";
	public static void setBaseURL( String baseURL ) {
		base = baseURL;
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
		int x = (int)Math.floor(scale*(rect.getX()-320.));
		int y = (int)Math.floor(scale*(rect.getY()-320.));
		int width = (int)Math.ceil( scale*(rect.getX()-320.+rect.getWidth()) ) - x;
		int height = (int)Math.ceil( scale*(rect.getY()-320.+rect.getHeight()) ) - y;
		Projection proj = new PolarStereo( new Point(0, 0),
				180., res*50., -71.,
				PolarStereo.SOUTH, PolarStereo.WGS84);
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
			tiler = new URLTilerZ( 320, res, ng, nLevel, proj, base );
		} catch (IOException ex) {
			return false;
		}
		XGrid_Z grid = new XGrid_Z( x, y, width, height, tiler );
		proj = grid.getProjection();
		if( res < 4 ) {
			int resA = 4;
			int scaleA = mapRes/4;
			int xA = (int)Math.floor(scaleA*(rect.getX()-320.));
			int yA = (int)Math.floor(scaleA*(rect.getY()-320.));
			int widthA = (int)Math.ceil( scaleA*(rect.getX()-320.+rect.getWidth()) ) - xA;
			int heightA = (int)Math.ceil( scaleA*(rect.getY()-320.+rect.getHeight()) ) - yA;
			PolarStereo projA = new PolarStereo( new Point(0, 0),
				180., 4.*50., -71.,
				PolarStereo.SOUTH, PolarStereo.WGS84);
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
				tiler = new URLTilerZ( 320, 4, ng, nLevel, projA, base );
			} catch (IOException ex) {
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
		if( res < 64 ) {
			int resA = 64;
			int scaleA = mapRes/64;
			int xA = (int)Math.floor(scaleA*(rect.getX()-320.));
			int yA = (int)Math.floor(scaleA*(rect.getY()-320.));
			int widthA = (int)Math.ceil( scaleA*(rect.getX()-320.+rect.getWidth()) ) - xA;
			int heightA = (int)Math.ceil( scaleA*(rect.getY()-320.+rect.getHeight()) ) - yA;
			PolarStereo projA = new PolarStereo( new Point(0, 0),
				180., 64.*50., -71.,
				PolarStereo.SOUTH, PolarStereo.WGS84);
			nLevel = 0;
			nGrid = 1024/64;
			while( nGrid>8 ) {
				nLevel++;
				nGrid /= 8;
			}
			tiler = null;
			ng = 2 + widthA/320;
			try { 
				tiler = new URLTilerZ( 320, 64, ng, nLevel, projA, base );
			} catch (IOException ex) {
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

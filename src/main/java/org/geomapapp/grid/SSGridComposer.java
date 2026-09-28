package org.geomapapp.grid;

import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.proj.Projection;
import haxby.util.PathUtil;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.geomapapp.geom.Mercator;

public class SSGridComposer {
	static String base1 = PathUtil.getPath("GLOBAL_GRIDS/GRAVITY", 
			MapApp.BASE_URL+"/data/global_tiles/merc/gravity_675/");
	static String base2 = PathUtil.getPath("GLOBAL_GRIDS/GEOID", 
			MapApp.BASE_URL+"/data/global_tiles/merc/geoid_675/");
	static String base3 = PathUtil.getPath("GLOBAL_GRIDS/TOPO_9", 
			MapApp.BASE_URL+"/data/global_tiles/merc/topo_9_675/");
	static String base4 = PathUtil.getPath("GLOBAL_GRIDS/GRAVITY_18",
			MapApp.BASE_URL+"/data/global_tiles/merc/gravity_18_675/");
	static String base5 = PathUtil.getPath("GLOBAL_GRIDS/OCEAN_AGES", 
			MapApp.BASE_URL+"/data/global_tiles/merc/ocean_ages_675/");
	static String base6 = PathUtil.getPath("GLOBAL_GRIDS/SPREADING_ASYMMETRY", 
			MapApp.BASE_URL+"/data/global_tiles/merc/spreading_asymmetry_675/");
	static String base7 = PathUtil.getPath("GLOBAL_GRIDS/SPREADING_RATE", 
			MapApp.BASE_URL+"/data/global_tiles/merc/spreading_rate_675/");

	static String base = base1;
	public static final int GRAVITY = 1;
	public static final int GEOID = 2;
	public static final int TOPO_9 = 3;
	public static final int GRAVITY_18 = 4;
	public static final int AGE = 5;
	public static final int SPREADING_ASYMMETRY = 6;
	public static final int SPREADING_RATE = 7;

//	static String base = "/scratch/antarctic/bill/srtm/3arcsec/africa/merc_320_1024/";
	public static void setBaseURL( String baseURL ) {
		base = baseURL;
		if(!base.endsWith("/")) base += "/";
		base1 = base + "gravity/";
		base2 = base + "geoid/";
		base3 = base + "topo/";
		base4 = base + "gravity_18/";
		base5 = base + "ocean_ages_675/";
		base6 = base + "spreading_asymmetry_675/";
		base7 = base + "spreading_rate_675/";
	}
		public static boolean getGrid(Rectangle2D rect,
								Grid2DOverlay overlay, int which,
								int mapRes) {
				double zoom = overlay.getXMap().getZoom();
				return getGrid( rect, overlay, which, mapRes, zoom, true);
		}
		public static boolean getGrid(Rectangle2D rect,
								Grid2DOverlay overlay, int which,
								int mapRes, double zoom) {
				return getGrid( rect, overlay, which, mapRes, zoom, false);
		}
		public static boolean getGrid(Rectangle2D rect,
								Grid2DOverlay overlay, int which,
								int mapRes, double zoom, boolean reset) {

		int tileHeightScale = 1;
		Mercator proj = null;
		int maxRes = 1;

		if( which==1 ) {
			base = base1;
			maxRes = 32;
		}
		if( which==2 ) {
			base = base2;
			maxRes = 16;
		}
		if (which==3 ) {
			base = base3;
			maxRes = 32;
		}
		if (which==4 ) {
			base = base4;
			maxRes = 32;
		}
		if (which==5 ) {
			base = base5;
			maxRes = 16;
		}
		if (which==6 ) {
			base = base6;
			maxRes = 16;
		}
		if (which==7 ) {
			base = base7;
			maxRes = 16;
		}

		XMap map = overlay.getXMap();
		int size = 675;
		int res = mapRes;
		while(zoom*res/mapRes > 1.5 && res>512/maxRes) {
			res /=2;
		}
		int scale = mapRes/res;

		switch (which) {
			case GEOID:
			case GRAVITY:
				tileHeightScale = 400;
				proj = new Mercator(.008333333, 72.0033, 675*scale,
						0, 0);
				break;
			case GRAVITY_18:
			case TOPO_9:
			case AGE:
			case SPREADING_RATE:
			case SPREADING_ASYMMETRY:
				tileHeightScale = 540;
				proj = new Mercator(.008333333, 80.738, 675*scale,
						0, 0);
				break;
			default:
				break;
		}

		Projection mapProj = map.getProjection();
		Point2D pt1 = mapProj.getRefXY(
				new Point2D.Double(
					rect.getX(), 
					rect.getY()));
		Point2D pt2 = mapProj.getRefXY(
				new Point2D.Double(
					rect.getX(),
					rect.getY() + 1.));

		pt1 = proj.getMapXY(pt1);
		pt2 = proj.getMapXY(pt2);
		double scl = pt2.getY()-pt1.getY();
		int x = (int)Math.floor(pt1.getX());
		int y = (int)Math.floor(pt1.getY());
		pt1 = proj.getMapXY( mapProj.getRefXY( new Point(x,y) ));
		int width = (int)Math.ceil( scl*rect.getWidth()+1.);
		int height = (int)Math.ceil( scl*rect.getHeight()+1.);
		Rectangle r0 = new Rectangle( 0, 0, 675*scale,
					tileHeightScale*scale);

		Rectangle bounds = new Rectangle(x, y, width, height);
		int nLevel = 2;
		Grid2D.Short grid = new Grid2D.Short( bounds, proj);
		TileIO tileIO = new TileIO.Short( proj,
				base + "z_" + scale,
				size, nLevel);
	//	System.out.println( base + "z_" + scale);
		TiledGrid tiler = new TiledGrid( proj, 
						r0,
						tileIO,
						size,
						1,
						null);
		// System.out.println( r0 );
		// System.out.println( bounds );
		tiler.setWrap( 675*scale );
		// System.out.println( (675*scale)+"\n");
		grid = (Grid2D.Short)tiler.composeGrid(grid);

		switch (which) {
		case GRAVITY:
		case GRAVITY_18:
			grid.scale(0., 10.);
			break;

		case GEOID:
		case AGE:
		case SPREADING_ASYMMETRY:
		case SPREADING_RATE:
			grid.scale(0, 100);
		default:
			break;
		}

		overlay.setImage( 
				new BufferedImage(width, height, 
					BufferedImage.TYPE_INT_RGB),
				pt1.getX(), pt1.getY(), scl);
		overlay.setImage(null);
		overlay.setGrid(grid, null, false, true, reset);
		return true;
	}

	public static Grid2DOverlay getGridWW(Rectangle2D rect, int level, int which) {
		double MERC_MAX_LAT = 81;
		double MERC_MIN_LAT = -79;

		int tileHeightScale = 1;
		Mercator proj = null;
		int maxRes = 1;

		if( which==1 ) {
			base = base1;
			maxRes = 32;
		}
		if( which==2 ) {
			base = base2;
			maxRes = 16;
		}
		if (which==3 ) {
			base = base3;
			maxRes = 32;
		}
		if (which==4 ) {
			base = base4;
			maxRes = 32;
		}
		if (which==5 ) {
			base = base5;
			maxRes = 16;
		}
		if (which==6 ) {
			base = base6;
			maxRes = 16;
		}
		if (which==7 ) {
			base = base7;
			maxRes = 16;
		}

		int size = 675;
		int mapRes = 512;
		int res = mapRes;

		double zoom = 8 * Math.pow(2, level);

		while(zoom*res/mapRes > 1.5 && res>512/maxRes) {
			res /=2;
		}
		int scale = mapRes/res;
		int pixelsPer360 = 675*scale;

		switch (which) {
			case GRAVITY:
			case GEOID:
				tileHeightScale = 400;
				proj = new Mercator(.008333333, 72.0033, 675*scale,
						0, 0);
				break;
			case GRAVITY_18:
			case TOPO_9:
			case AGE:
			case SPREADING_ASYMMETRY:
			case SPREADING_RATE:
				tileHeightScale = 540;
				proj = new Mercator(.008333333, 80.738, 675*scale,
						0, 0);
				break;
			default:
				break;
		}

		double minX = rect.getX();
		double minY = rect.getY();
		double maxX = rect.getMaxX();
		double maxY = rect.getMaxY();

		maxY = Math.min(maxY, MERC_MAX_LAT);
		minY = Math.min(minY, MERC_MAX_LAT);
		maxY = Math.max(maxY, MERC_MIN_LAT);
		minY = Math.max(minY, MERC_MIN_LAT);

		if (maxY == minY)
			return null;

		Point2D minXY = proj.getMapXY(minX, minY);
		Point2D maxXY = proj.getMapXY(maxX, maxY);

		minX = minXY.getX();
		minY = minXY.getY();
		maxX = maxXY.getX();
		maxY = maxXY.getY();

		if (Double.isInfinite(minY) || Double.isInfinite(maxY)) 
			return null;

		if (minX > maxX) 
			maxX += pixelsPer360; 

		if (minY > maxY) {
			double y = minY;
			minY = maxY;
			maxY = y;
		}

		int x = (int)Math.floor(minX);
		int y = (int)Math.floor(minY);
		int width = (int)Math.ceil(maxX) - x;
		int height = (int)Math.ceil(maxY) - y;

		Rectangle r0 = new Rectangle( 0, 0, 675*scale,
				tileHeightScale*scale);
		Rectangle bounds = new Rectangle(x, y, width, height);
		int nLevel = 2;
		Grid2D.Short grid = new Grid2D.Short( bounds, proj);
		TileIO tileIO = new TileIO.Short( proj,
				base + "z_" + scale,
				size, nLevel);
		TiledGrid tiler = new TiledGrid( proj, 
						r0,
						tileIO,
						size,
						1,
						null);
		// System.out.println( r0 );
		tiler.setWrap( 675*scale );
		// System.out.println( (675*scale)+"\n");
		grid = (Grid2D.Short)tiler.composeGrid(grid);
		if( which==GRAVITY || which == GRAVITY_18) grid.scale(0., 10.);
		else if( which==GEOID ) grid.scale(0., 100.);

		Grid2DOverlay overlay = new Grid2DOverlay(null) {
			public void setGrid(Grid2D grid, Grid2D.Boolean landMask, boolean hasLand,
					boolean hasOcean, boolean reset) {
				this.landMask = landMask;
				land = hasLand;
				ocean = hasOcean;
				if( this.grid==null && grid==null)return;
				this.grid = grid;
				if( grid==null ) return;
			}
		};
		overlay.setResolution(scale);
		overlay.setGrid(grid, null, false, true, true);
		return overlay;
	}
}

package org.geomapapp.grid;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;

import haxby.map.GetGrid;
import haxby.map.MapApp;
import haxby.map.MapOverlay;
import haxby.map.XMap;
import haxby.proj.PolarStereo;
import haxby.proj.Projection;
import haxby.proj.ProjectionFactory;
import haxby.util.PathUtil;
import haxby.util.VersionUtil;

public class GridComposer extends GetGrid {
//	static String base = "/local/data/home/bill/db/merc_320_1024/";

	public static String base = PathUtil.getPath("GMRT_LATEST/MERCATOR_GRID_TILE_PATH");
	public static String mbPath = PathUtil.getPath("GMRT_LATEST/MERCATOR_GRID_TILE_PATH");
	public static String vo_base = PathUtil.getPath("GMRT_LATEST/VO_GRID_TILE_PATH");
	public static String spBase = PathUtil.getPath("GMRT_LATEST/SP_GRID_TILE_PATH");
	public static String npBase = PathUtil.getPath("GMRT_LATEST/NP_GRID_TILE_PATH");
	static {
		if(MapApp.AT_SEA) {
			base = base.replaceFirst("current", VersionUtil.getVersion("GMRT"));
			mbPath = mbPath.replaceFirst("current", VersionUtil.getVersion("GMRT"));
			vo_base = vo_base.replaceFirst("current", VersionUtil.getVersion("GMRT"));
			spBase = spBase.replaceFirst("current", VersionUtil.getVersion("GMRT"));
			npBase = npBase.replaceFirst("current", VersionUtil.getVersion("GMRT"));
		}
	}
	
	static int oceanGridMaxResLevel = 512;
	static HiResGrid[] hiRes;


    /**
     * The resolution levels at which GDEM data exists.
     */
	static int[] gdemResLevels = {
		8192, 4096, 2048
	};

	static int[][] mbResLevels = { 
		{1024, 512, 64}, // merc (MB, SS)
		{512, 64, 16}, // SP (MB, SS, BedMap)
		{512, 64, 32, 16} // NP (MB, SS, IBCAO, BedMap)
	};

	public static void setBaseURL( String baseURL ) {
		base = baseURL;
		if(!base.endsWith("/")) base += "/";
	}

	public static void addHiResArea( HiResGrid grid ) {
		if( hiRes==null ) {
			hiRes = new HiResGrid[] {grid};
		} else {
			HiResGrid[] grids = new HiResGrid[hiRes.length+1];
			System.arraycopy(hiRes, 0, grids, 0, hiRes.length);
			grids[hiRes.length] = grid;
			hiRes = grids;
		}
	}
	public static boolean getHiRes(Rectangle2D rect, 
				Grid2DOverlay overlay, 
				int mapRes) {
		double zoom = overlay.getXMap().getZoom();
		if( zoom<mapRes*1.5 )return false;
		Rectangle2D.Double r = new Rectangle2D.Double(
				rect.getX(), rect.getY()-260.,
				rect.getWidth(), rect.getHeight());
		if( hiRes==null ) return false;
		double[] range = new double[2];
		range[0] = range[1] = 0.;
		int maxRes = 0;
		boolean start = true;
		for( int k=0 ; k<hiRes.length ; k++) {
			Rectangle bounds = hiRes[k].bounds;
			double s = (double)hiRes[k].resolution;
			double test1 = bounds.x-s*(r.x+r.width);
			double test2 = s*r.x - (bounds.x+bounds.width);
			double test3 = bounds.y - s*(r.y+r.height);
			double test4 = s*r.y - (bounds.y+bounds.height);
		//	System.out.println( bounds.x+"\t"+(s*r.x) +"\t"+ bounds.y+"\t"+(s*r.y));
		//	System.out.println( (bounds.x+bounds.width) +"\t"+(s*r.x) +"\t"+ bounds.y+"\t"+(s*r.y));
			if( bounds.x>s*(r.x+r.width)
				|| bounds.y>s*(r.y+r.height)
				|| bounds.x+bounds.width < s*r.x
				|| bounds.y+bounds.height < s*r.y ) continue;
	//	System.out.println( test1 +"\t"+ test2 +"\t"+ test3 +"\t"+ test4);
			if( start ) {
				range[0] = hiRes[k].range[0];
				range[1] = hiRes[k].range[1];
				maxRes = hiRes[k].resolution;
				start = false;
			} else {
				if( hiRes[k].range[0]<range[0] ) {
					range[0]=hiRes[k].range[0];
				} else if( hiRes[k].range[1]>range[1] ) {
					range[1]=hiRes[k].range[1];
				}
				if( hiRes[k].resolution>maxRes )maxRes = hiRes[k].resolution;
			}
		}
	//	System.out.println( maxRes +"\t"+ start );
		if( start ) return false;
		int res = mapRes;
	//	if( zoom>mapRes*1.5 ) {
		while(zoom/mapRes > 1.5 && res<maxRes ) {
			res *=2;
			zoom /= 2;
		}
		int scale = res;

		int x = (int)Math.floor(scale*rect.getX());
		int y = (int)Math.floor(scale*(rect.getY()-260.));
		int width = (int)Math.ceil( 
			scale*(rect.getX()+rect.getWidth()) ) - x;
		int height = (int)Math.ceil( 
			scale*(rect.getY()-260.+rect.getHeight()) ) - y;
		Rectangle r0 = new Rectangle( 0, -260*scale, 640*scale,
					260*2*scale);
		Projection proj = ProjectionFactory.getMercator( 2*res*320 );
		Rectangle bounds = new Rectangle(x, y, width, height);
		int nLevel = 0;
		int nGrid = 2*res;
		while( nGrid>8 ) {
			nLevel++;
			nGrid /= 8;
		}
		Grid2D.Short grid = new Grid2D.Short( bounds, proj);
		TileIO tileIO = new TileIO.Short( proj,
				base + "hiRes/z_" + res,
				320, nLevel);
//	System.out.println( nLevel +"\t"+ base + "hiRes/z_" +res);
		TiledGrid tiler = new TiledGrid( proj, 
						r0,
						tileIO,
						320,
						1,
						null);
		tiler.setWrap( 2*res*320 );
		if( range[1]-range[0]<2000. ) {
			double offset = (range[0]+range[1]) * .5;
			double scl = 10.;
			if( range[1]-range[0]<1000. )scl=20.;
			if( range[1]-range[0]<400. )scl=50.;
			if( range[1]-range[0]<200. )scl=100.;
			grid.scale( offset, scl);
		}
		grid = (Grid2D.Short)tiler.composeGrid(grid);

		Grid2D.Boolean land = new Grid2D.Boolean(bounds, proj);
		boolean hasOcean = false;
		boolean hasLand = false;
		for( x=bounds.x ; x<bounds.x+bounds.width ; x++) {
			for( y=bounds.y ; y<bounds.y+bounds.height ; y++) {
				double z = grid.valueAt(x, y, true);
				if( Double.isNaN(z) )continue;
				if( z>=0 ) {
					hasLand = true;
					land.setValue(x, y, true);
				} else {
					hasOcean = true;
				}
			}
		}
//	System.out.println( hasLand +"\t"+ hasOcean);
		if( !hasLand && !hasOcean ) return false;
		overlay.setGrid(grid, land, hasLand, hasOcean, true, true);
		return true;
	}

	public static boolean getGrid(Rectangle2D rect,
								Grid2DOverlay overlay,
								int mapRes) {
		double zoom = overlay.getXMap().getZoom();
		return getGrid( rect, overlay, mapRes, zoom, true);
	}
	public static boolean getGrid(Rectangle2D rect,
							Grid2DOverlay overlay,
							int mapRes, double zoom) {
		return getGrid( rect, overlay, mapRes, zoom, false);
	}

	public static boolean getGrid(Rectangle2D rect,
			Grid2DOverlay overlay,
			int mapRes, double zoom, boolean reset) {
		return getGrid( rect, overlay, mapRes, zoom, reset, base, mbPath);
	}
	
	public static boolean getGrid(Rectangle2D rect,
			Grid2DOverlay overlay,
			int mapRes, double zoom, boolean reset, String baseUrl, String mbPath) {
		

			if( zoom>mbResLevels[0][0] * 1.5 ) {
				if( getHiRes(rect, overlay, mapRes) )return true;
			}

			// Sets resolution to be the smallest 2^n greater than zoom.
			int res = 1;
			while(zoom > res) {
				res *= 2;
			}

			int x = (int)Math.floor(res*rect.getX());
			int y = (int)Math.floor(res*(rect.getY()-260.));
			int width = (int)Math.ceil(res*(rect.getX()+rect.getWidth()) ) - x;
			int height = (int)Math.ceil(res*(rect.getY()-260.+rect.getHeight()) ) - y;
			Projection proj = ProjectionFactory.getMercator( 320*2*res );
			Rectangle bounds = new Rectangle(x, y, width, height);

			Grid2D.Float grid = GetGrid.getGrid( bounds, res, false, baseUrl, mbPath, true);
			Grid2D.Boolean land = new Grid2D.Boolean(bounds, proj);
			boolean hasOcean = false;
			boolean hasLand = false;
			int oceanPixels = 0; //need at least 2 ocean pixels to calculate color palette range
			
			for( x=bounds.x ; x<bounds.x+bounds.width ; x++) {
				for( y=bounds.y ; y<bounds.y+bounds.height ; y++) {
					boolean tf = grid.floatValue(x,y)>=0;
					land.setValue( x, y, tf );
					if( tf ) {
						hasLand=true;
					} else {
						oceanPixels++;
					}
				}
			}
			hasOcean = oceanPixels > 1;
			overlay.setGrid(grid, land, hasLand, hasOcean, reset, true);
			return true;
		}
/*public static boolean getGrid(Rectangle2D rect,
							Grid2DOverlay overlay,
							int mapRes, double zoom, boolean reset) {
		if( zoom>mbResLevels[0][0] * 1.5 ) {
			if( getHiRes(rect, overlay, mapRes) )return true;
		}

		boolean isFull = false;
		boolean gdemTF = true;
		boolean srtmTF = true;
		boolean gridR = true;		// This displays NOAA coastal grids combined with selected contributed grids. In the dir path grids/z_ 
		boolean mbR =  true;
		boolean ocean = false;

		// Sets resolution to be the smallest 2^n greater than zoom.
		int res = 1;
		while(zoom > res) {
			res *= 2;
		}

		// Creates a rectangle r0 from the map window size.
		int scale = res;
		int x = (int)Math.floor(scale*rect.getX());
		int y = (int)Math.floor(scale*(rect.getY()-260.));
		int width = (int)Math.ceil( 
			scale*(rect.getX()+rect.getWidth()) ) - x;
		int height = (int)Math.ceil( 
			scale*(rect.getY()-260.+rect.getHeight()) ) - y;
		Rectangle r0 = new Rectangle( 0, -260*scale, 640*scale,
					260*2*scale);
		Projection proj = ProjectionFactory.getMercator( 320*2*res );
		Rectangle bounds = new Rectangle(x, y, width, height);

		// nLevel = 0 for res = 1-4; 1 for 8-32; 2 for 64-256; 3 for 512-2048; 4 for 4096-16384.
		int iRes = res;
		int nLevel = 0;
		while (iRes >= 8) {
			iRes /= 8;
			nLevel++;
		}
*/
		/* Step 1: Composes tiles for land area at resolutions 1-2048. ASTER = max res of 2048 and NED = max res of 8192.
		* This step handles areas with ASTER data.
		* For the directory called ocean contains the Smith & Sandwell version that contains the srtm land and ocean.
		 * */
/*		Grid2D.Short grid = new Grid2D.Short( bounds, proj); //shell for grid
		TileIO tileIO = new TileIO.Short( proj,
				base + "gdem/z_" + res,
				320, nLevel);
		TiledGrid tiler = new TiledGrid( proj, 
						r0,
						tileIO,
						320,
						1,
						null);
		tiler.setWrap( 320*2*res );

		if (gdemTF) {
			if (res <= gdemResLevels[0]) {
				System.out.println("Compose GDEM: " + res);
				grid = (Grid2D.Short)tiler.composeGrid(grid);
				// Tests the tile for NaN's. This would occur if we have high-res NED beyond resolution of 2048.
				// If this occurs, isFull is set to false and we require special treatment.
				if (grid.getBuffer() != null) {
					isFull = true;
					for (int i = 0; i < grid.getBuffer().length; i++)
						if (grid.getBuffer()[i] == Grid2D.Short.NaN)
							isFull = false;
				}
			}

			// Step 2: Special treatment for resolutions beyond 2048 with NaN's.
			// gdemResLevels = [8192, 2048]
			for (int res0 : gdemResLevels) { //gdemResLevels
		//	System.out.println("getting gdem tiles for res0 and res " + res0 + " " + res);
				// If high-res tile is full, no special treatment is needed.
				if (isFull) break;
				// If the high-res tile is not full, decreases resolution until the ASTER data to fill it in.
				if (res <= res0) continue;

				System.out.println("Compose GDEM: " + res0);

				// Factor adjusts x,y value to correctly sample lower resolution grids.
				double factor = res0*1.0/res;
				scale = res0;
				int x0 = (int)Math.floor(scale*rect.getX())-2;
				int y0 = (int)Math.floor(scale*(rect.getY()-260.))-2;
				int width0 = 4 + (int)Math.ceil(scale*(rect.getX()+rect.getWidth()) ) - x0;
				int height0 = 4 + (int)Math.ceil( 
						scale*(rect.getY()-260.+rect.getHeight()) ) - y0;
				r0 = new Rectangle( 0, -260*scale, 640*scale,
						260*2*scale);
				Projection proj0 = ProjectionFactory.getMercator( 
						2*320*res0);
				Rectangle bounds0 = new Rectangle(x0, y0, width0, height0);
				iRes = res0;
				nLevel = 0;
				while (iRes >= 8) {
					iRes /= 8;
					nLevel++;
				}
				// g0 is the temporary grid for the land x,y,z values and gets tiles out of the gdem directory.
				Grid2D.Short g0 = new Grid2D.Short( bounds0, proj0);
				tileIO = new TileIO.Short( proj0,
						base + "gdem/z_" + res0,
						320, nLevel);
				tiler = new TiledGrid( proj0, 
								r0,
								tileIO,
								320,
								1,
								null);
				tiler.setWrap( 320*2*res0);

				g0 = (Grid2D.Short)tiler.composeGrid(g0);

				// Assumes the composed g0 grid has no NaN's then tests.
				isFull = true;
				for( x=bounds.x ; x<bounds.x+bounds.width ; x++) {
					//System.out.println("val x: " + x);
					for( y=bounds.y ; y<bounds.y+bounds.height ; y++) {
						if( grid.shortValue(x,y) != Grid2D.Short.NaN )continue;
						double z = g0.valueAt(factor*x, factor*y);

						// If NaN's are found, isFull is set to false and the for loop will iterate again at the next lower resolution.
						if (Double.isNaN(z)) isFull = false;
						// If not a NaN, set the land elevation value.
						else { 
							grid.setValue(x, y, z);
						}
					}
				}
			}
		}

		// land is a Boolean grid. Its elements will be true for land and false for ocean.
		Grid2D.Boolean land = new Grid2D.Boolean(bounds, proj);
		boolean hasOcean = true;
		boolean hasLand = false;
*/	
		/* If there is SRTM data, check the ocean_/z_ directory to see if there is any ocean data
		 * in the land tile
		 */
//		if(srtmTF) { 
		/* Step 3: Gets tiles for the ocean from the ocean directory. Determines if grid node is in Ocean (100) or Land(0). This is used by the GMA
		 * color palette. Starts by assuming the map window shows both land and ocean.
		 */
//		Grid2D.Short ogrid = null;
		/* Looks in the created grid for the land elevations then starts with hasLand as true and hasOcean as false.
		 * Grid is null if the map has no land. Grid is not null if map is all or partially land.
		 */
/*		if( grid.getBuffer()!=null ) { // testing the grid composed from GDEM tiles.
			hasLand = true;
			hasOcean = false;
*/
			/* The ocean tiles have resolutions 1,2,4,...,512. Thus res0 cannot be higher than 512.
			* Background ocean is never higher than 512 in resolution.
			* */
/*			int res0 = Math.min(res, oceanGridMaxResLevel);
			//System.out.println("res0 = " + res0 + "  ; res = " + res);
			double factor = res0*1.0/res;
			scale = res0;
			int x0 = (int)Math.floor(scale*rect.getX())-2;
			int y0 = (int)Math.floor(scale*(rect.getY()-260.))-2;
			int width0 = 4 + (int)Math.ceil(
					scale*(rect.getX()+rect.getWidth()) ) - x0;
			int height0 = 4 + (int)Math.ceil( 
					scale*(rect.getY()-260.+rect.getHeight()) ) - y0;
			r0 = new Rectangle( 0, -260*scale, 640*scale,
					260*2*scale);
			Projection proj0 = ProjectionFactory.getMercator( 
					2*320*res0);
			Rectangle bounds0 = new Rectangle(x0, y0, width0, height0);
			iRes = res0;
			nLevel = 0;
			while (iRes >= 8) {
				iRes /= 8;
				nLevel++;
			}
*/
			/* Creates an ogrid for the ocean. Containing the Smith & Sandwell
			 * elevations for both oceans and land.
			 */
/*			ogrid = new Grid2D.Short( bounds0, proj0);
			tileIO = new TileIO.Short( proj0,
				base + "ocean/z_" + res0,
				320, nLevel);
			tiler = new TiledGrid( proj0, 
						r0,
						tileIO,
						320,
						1,
						null);
			tiler.setWrap( 320*2*res0);
			System.out.println("Compose SRTM Grid: " + res);
			ogrid = (Grid2D.Short)tiler.composeGrid(ogrid);
			for( x=bounds.x ; x<bounds.x+bounds.width ; x++) {
				for( y=bounds.y ; y<bounds.y+bounds.height ; y++) {
					// Finding x,y values that are NaN in temporary lower resolution grid
					boolean tf = (short)ogrid.valueAt(factor * x, factor * y) == 0 &&
								 !Double.isNaN(ogrid.valueAt(factor * x, factor * y)); // set to true if value is 0 or -3XXX
				// System.out.println("ogrid value at x,y= " + ogrid.valueAt(factor * x, factor * y));
				// System.out.println("tf = " + tf);
				// System.out.println("uncasted ogrid double value at x,y= " + ogrid.valueAt(factor * x, factor * y));
				// System.out.println("casted ogrid short value at x,y= " + (short)ogrid.valueAt(factor * x, factor * y));
				// System.out.println(x + "\t" + y + "\t" + ogrid.shortValue(x,y));

					land.setValue(x, y, tf);	// Placing true/false in every node of the land grid previously created as a empty shell.

					if( !tf ) { //false
						// Sets NaN's in our grid previously fill with land only GDEM resolution data that appears in map window.
						grid.setValue(x, y, Double.NaN); //nan transparent
						hasOcean = true;
						// System.out.println("tf = " + tf);
					
						if (ocean) {
							grid.setValue(x, y, -ogrid.valueAt(factor * x,factor * y)); // negating the z value
					System.out.println("Negated zvalue:  " + -ogrid.valueAt(factor * x,factor * y));
						}
					}
				}
			}
		}
		}
//		System.out.println("hasOcean = " + hasOcean);
//		hasOcean = true;

		if ( hasOcean ) {
			// Load from grid...
			tileIO = new TileIO.Short( proj,
					base + "grids/z_" + res,
					320, nLevel);
			tiler = new TiledGrid( proj, 
					r0,
					tileIO,
					320,
					1,
					null);
			tiler.setWrap( 320*2*res );

			if (gridR) {
				//System.out.println("Compose Grid from grids/z_" + res);
				grid = (Grid2D.Short)tiler.composeGrid(grid);
			}

			// Load from Multibeam...
			tileIO = new TileIO.Short( proj,
				base + "multibeam/z_" + res,
				320, nLevel);
			tiler = new TiledGrid( proj, 
					r0,
					tileIO,
					320,
					1,
					null);
			tiler.setWrap( 320*2*res );
			if (mbR) {
				System.out.println("Compose MB " + res);
				grid = (Grid2D.Short)tiler.composeGrid(grid);
			}
			for (int res0 : mbResLevels[0]) {
				if (res <= res0) continue;

				double factor = res0*1.0/res;
				scale = res0;
				int x0 = (int)Math.floor(scale*rect.getX())-2;
				int y0 = (int)Math.floor(scale*(rect.getY()-260.))-2;
				int width0 = 4 + (int)Math.ceil(
						scale*(rect.getX()+rect.getWidth()) ) - x0;
				int height0 = 4 + (int)Math.ceil( 
						scale*(rect.getY()-260.+rect.getHeight()) ) - y0;
				r0 = new Rectangle( 0, -260*scale, 640*scale,
						260*2*scale);
				Projection proj0 = ProjectionFactory.getMercator( 
						2*320*res0);
				Rectangle bounds0 = new Rectangle(x0, y0, width0, height0);
				iRes = res0;
				nLevel = 0;
				while (iRes >= 8) {
					iRes /= 8;
					nLevel++;
				}

				if (gridR) {
					// Load grid...
					Grid2D.Short g0 = new Grid2D.Short( bounds0, proj0);
					TileIO t0 = new TileIO.Short( proj0,
							base + "grids/z_" + res0,
							320, nLevel);
					tiler = new TiledGrid( proj0, 
							r0,
							t0,
							320,
							1,
							null);
					tiler.setWrap( 320*2*res0);
				//System.out.println("Compose Grid from grids/z_" + res0);
					g0 = (Grid2D.Short)tiler.composeGrid(g0);

					for( x=bounds.x ; x<bounds.x+bounds.width ; x++) {
						for( y=bounds.y ; y<bounds.y+bounds.height ; y++) {
							if( grid.shortValue(x,y) != grid.NaN )continue;
							double z = g0.valueAt(factor*x, factor*y);
							//if ((!Double.isNaN(z)) && (z > -100.00)) {
							//	grid.setValue(x, y, z);
							//}
							if (!Double.isNaN(z)) grid.setValue(x, y, z);
						}
					}
				}

				if (mbR) {
					Grid2D.Short g0 = new Grid2D.Short( bounds0, proj0);
					TileIO.Short t0 = new TileIO.Short( proj0,
							base + "multibeam/z_" + res0,
							320, nLevel);
					tiler = new TiledGrid( proj0, 
							r0,
							t0,
							320,
							1,
							null);
					tiler.setWrap( 320*2*res0);
					System.out.println("Compose MB: " + res0);
					g0 = (Grid2D.Short)tiler.composeGrid(g0);

					for( x=bounds.x ; x<bounds.x+bounds.width ; x++) {
						for( y=bounds.y ; y<bounds.y+bounds.height ; y++) {
							if( grid.shortValue(x,y) != grid.NaN )continue;
							double z = g0.valueAt(factor*x, factor*y);
							//if((!Double.isNaN(z)) && (z < 100.00)) {
							//System.out.println("Compose less then 100m");
							//	grid.setValue(x, y, z);
							//}
							if (!Double.isNaN(z)) grid.setValue(x, y, z);
						}
					}
				}
			}
		}

		for( x=bounds.x ; x<bounds.x+bounds.width ; x++) {
			for( y=bounds.y ; y<bounds.y+bounds.height ; y++) {
				boolean tf = grid.shortValue(x,y)>=0;
				land.setValue( x, y, grid.shortValue(x,y)>=0 );
				if( tf ) hasLand=true;
			}
		}

		overlay.setGrid(grid, land, hasLand, hasOcean, reset);
		return true;
	}
	*/
	public static boolean getMask(Rectangle2D rect, 
				MapOverlay overlay) {
		double zoom = overlay.getXMap().getZoom();
		int res = 1;
		while(zoom > res) {
			res *=2;
		}
		res = Math.min(res, 512);

		int scale = res;
		int x = (int)Math.floor(scale*rect.getX());
		int y = (int)Math.floor(scale*(rect.getY()-260.));
		int width = (int)Math.ceil( scale*(rect.getX()+rect.getWidth()) ) - x;
		int height = (int)Math.ceil( scale*(rect.getY()-260.+rect.getHeight()) ) - y;

		Rectangle r0 = new Rectangle( 0, -260*scale, 640*scale,
				260*2*scale);

		if (width <= 0 || height <=0) return false;

		Projection proj = ProjectionFactory.getMercator( 320*2*res );
		Rectangle bounds = new Rectangle(x, y, width, height);
		if (bounds.width <= 0 || bounds.height <= 0)
			return false;

		int iRes = res;
		int nLevel = 0;
		while (iRes >= 8) {
			iRes /= 8;
			nLevel++;
		}

		Grid2D.Boolean grid = new Grid2D.Boolean( bounds, proj);
		TileIO.Boolean tileIO = new TileIO.Boolean( proj,
				base + "mask/m_" + res,
				320, nLevel);
		TiledMask tiler = new TiledMask( proj, 
						r0,
						tileIO,
						320,
						1,
						(TiledMask)null);
		tiler.setWrap( 320*2*res );
		System.out.println("Compose Mask: " + res);
		grid = (Grid2D.Boolean)tiler.composeGrid(grid);
		BufferedImage image = new BufferedImage( bounds.width,
				bounds.height, BufferedImage.TYPE_INT_ARGB);
		for( y=0 ; y<bounds.height ; y++) {
			for( x=0 ; x<bounds.width ; x++) {
				image.setRGB( x, y, 
					grid.booleanValue(x+bounds.x, y+bounds.y) ?
						0 : 0x80000000);
			}
		}
		Point2D p0 = new Point2D.Double(bounds.getX(), bounds.getY());
		XMap map = overlay.getXMap();
		p0 = map.getProjection().getMapXY( grid.getProjection().getRefXY(p0));
		Point2D p1 = new Point2D.Double(bounds.getX()+1., bounds.getY());
		p1 = map.getProjection().getMapXY( grid.getProjection().getRefXY(p1));
		double gridScale = p1.getX()<p0.getX() ?
			p1.getX()+map.getWrap()-p0.getX() :
			p1.getX() - p0.getX();
		overlay.setMaskImage(image, 
				p0.getX(),
				p0.getY(),
				gridScale);
	//	overlay.setGrid(grid, land, hasLand, hasOcean);
		return true;
	}

		public static boolean getGridSP(Rectangle2D rect,
								Grid2DOverlay overlay,
								int mapRes) {
			double zoom = overlay.getXMap().getZoom();
			return getGridSP( rect, overlay, mapRes, zoom, true);
		}

		public static boolean getGridSP(Rectangle2D rect,
				Grid2DOverlay overlay,
				int mapRes, double zoom, boolean reset) {
			return getGridSP(rect, overlay, mapRes, zoom, reset, spBase);
		}
		
		public static boolean getGridSP(Rectangle2D rect,
								Grid2DOverlay overlay,
								int mapRes, double zoom, boolean reset, String baseUrl) {
			int res = 1;
			while (res < zoom)
			res *= 2;

		int scale = res;
		int x = (int)Math.floor(scale*(rect.getX()-320.));
		int y = (int)Math.floor(scale*(rect.getY()-320.));
		int width = (int)Math.ceil( scale*(rect.getX()-320.+rect.getWidth()) ) - x;
		int height = (int)Math.ceil( scale*(rect.getY()-320.+rect.getHeight()) ) - y;
		Projection proj = new PolarStereo( new Point(0, 0),
				180., 25600. / res, -71.,
				PolarStereo.SOUTH, PolarStereo.WGS84);

		Rectangle r0 = new Rectangle( -320*scale, -320*scale, 640*scale,
					320*2*scale);
		Rectangle bounds = new Rectangle(x, y, width, height);

		int iRes = res;
		int nLevel = 0;
		while (iRes >= 8) {
			iRes /= 8;
			nLevel++;
		}

		Grid2D.Short grid = new Grid2D.Short( bounds, proj);
		TileIO.Short tileIO = new TileIO.Short( proj,
				baseUrl + "multibeam/z_" + res,
				320, nLevel);
		TiledGrid tiler = new TiledGrid( proj, 
					r0,
					tileIO,
					320,
					1,
					null);

		// Get res grid
		grid = (Grid2D.Short)tiler.composeGrid(grid);

		boolean isFull = false;
		for (int res0 : mbResLevels[1]) {
			if (isFull) break;
			if (res <= res0) continue;

			double factor = res0*1.0/res;
			scale = res0;

			int x0 = (int)Math.floor(scale*(rect.getX()-320.)) - 2;
			int y0 = (int)Math.floor(scale*(rect.getY()-320.)) - 2;
			int width0 = (int)Math.ceil( scale*(rect.getX()-320.+rect.getWidth()) ) - x0 + 4;
			int height0 = (int)Math.ceil( scale*(rect.getY()-320.+rect.getHeight()) ) - y0 + 4;
			r0 = new Rectangle( -320*scale, -320*scale, 640*scale,
					320*2*scale);
			Projection proj0 = new PolarStereo( new Point(0, 0),
					180., 25600. / res0, -71.,
				PolarStereo.SOUTH, PolarStereo.WGS84);
			Rectangle bounds0 = new Rectangle(x0, y0, width0, height0);
			iRes = res0;
			nLevel = 0;
			while (iRes >= 8) {
				iRes /= 8;
				nLevel++;
			}
			Grid2D.Short g0 = new Grid2D.Short( bounds0, proj0);
			TileIO t0 = new TileIO.Short( proj0,
					baseUrl + "multibeam/z_" + res0,
					320, nLevel);
			tiler = new TiledGrid( proj0, 
					r0,
					t0,
					320,
					1,
					null);
			g0 = (Grid2D.Short)tiler.composeGrid(g0);

			isFull = true;
			for( x=bounds.x ; x<bounds.x+bounds.width ; x++) {
				for( y=bounds.y ; y<bounds.y+bounds.height ; y++) {
					if( grid.shortValue(x,y) != grid.NaN )continue;
					double z = g0.valueAt(factor*x, factor*y, true);
					if (Double.isNaN(z))
						isFull = false;
					else 
						grid.setValue(x, y, z);
				}
			}
		}

		Grid2D.Boolean land = new Grid2D.Boolean(bounds, proj);
		boolean hasLand = false;
		boolean hasOcean = false;
		for( x=bounds.x ; x<bounds.x+bounds.width ; x++) {
			for( y=bounds.y ; y<bounds.y+bounds.height ; y++) {
				boolean tf = grid.shortValue(x,y)>=0;
				land.setValue( x, y, grid.shortValue(x,y)>=0 );
				if( tf ) {
					hasLand=true;
				} else {
					hasOcean = true;
				}
			}
		}
		
		overlay.setGrid(grid, land, hasLand, hasOcean, reset, true);
		return true;
	}

		public static boolean getGridNP(Rectangle2D rect,
			Grid2DOverlay overlay,
			int mapRes) {
			double zoom = overlay.getXMap().getZoom();
			return getGridNP( rect, overlay, mapRes, zoom, true);
		}

		public static boolean getGridNP(Rectangle2D rect,
				Grid2DOverlay overlay,
				int mapRes, double zoom, boolean reset) {
			return getGridNP( rect, overlay, mapRes, zoom, reset, npBase);
		}
		
		public static boolean getGridNP(Rectangle2D rect,
							Grid2DOverlay overlay,
							int mapRes, double zoom, boolean reset, String baseUrl) {
			int res = 1;
			while (res < zoom)
			res *= 2;

			int scale = res;
			int x = (int)Math.floor(scale*(rect.getX()-320.));
			int y = (int)Math.floor(scale*(rect.getY()-320.));
			int width = (int)Math.ceil( scale*(rect.getX()-320.+rect.getWidth() + 4) ) - x;
			int height = (int)Math.ceil( scale*(rect.getY()-320.+rect.getHeight() + 4) ) - y;
			Projection proj = new PolarStereo( new Point(0, 0),
				0., 25600./res, 71.,
				PolarStereo.NORTH, PolarStereo.WGS84);

			Rectangle r0 = new Rectangle( -320*scale, -320*scale, 640*scale,
					320*2*scale);
			Rectangle bounds = new Rectangle(x, y, width, height);

			int iRes = res;
			int nLevel = 0;
			while (iRes >= 8) {
				iRes /= 8;
				nLevel++;
			}

			Grid2D.Short grid = new Grid2D.Short( bounds, proj);
			TileIO.Short tileIO = new TileIO.Short( proj,
					baseUrl + "multibeam/z_" + res,
					320, nLevel);
			TiledGrid tiler = new TiledGrid( proj, 
					r0,
					tileIO,
					320,
					1,
					null);
			grid = (Grid2D.Short)tiler.composeGrid(grid);

			boolean isFull = false;

			for (int res0 : mbResLevels[2]) {
				if (isFull) break;
				if (res <= res0) continue;

				double factor = res0 * 1.0 / res;
				scale = res0;

				int x0 = (int)Math.floor(scale*(rect.getX()-320.)) - 2;
				int y0 = (int)Math.floor(scale*(rect.getY()-320.)) - 2;
				int width0 = (int)Math.ceil( scale*(rect.getX()-320.+rect.getWidth()) ) - x0 + 4;
				int height0 = (int)Math.ceil( scale*(rect.getY()-320.+rect.getHeight()) ) - y0 + 4;
				r0 = new Rectangle( -320*scale, -320*scale, 640*scale, 320*2*scale);
				Projection proj0 = new PolarStereo( new Point(0, 0),
						0., 25600. / res0, 71.,
						PolarStereo.NORTH, PolarStereo.WGS84);
				Rectangle bounds0 = new Rectangle(x0, y0, width0, height0);
				nLevel = 0;

				iRes = res0;
				nLevel = 0;
				while (iRes >= 8) {
					iRes /= 8;
					nLevel++;
				}

				Grid2D.Short g0 = new Grid2D.Short( bounds0, proj0);
				TileIO t4 = new TileIO.Short( proj0,
						baseUrl + "multibeam/z_" + res0,
						320, nLevel);
				tiler = new TiledGrid( proj0, 
						r0,
						t4,
						320,
						1,
						null);
				g0 = (Grid2D.Short)tiler.composeGrid(g0);

				isFull = true;
				for( x=bounds.x ; x<bounds.x+bounds.width ; x++) {
					for( y=bounds.y ; y<bounds.y+bounds.height ; y++) {
						if( grid.shortValue(x,y) != grid.NaN )continue;
						double z = g0.valueAt(factor*x, factor*y, true);
						if (Double.isNaN(z))
							isFull = false;
						else 
							grid.setValue(x, y, z);
					}
				}
			}

			Grid2D.Boolean land = new Grid2D.Boolean(bounds, proj);
			boolean hasLand = false;
			boolean hasOcean = false;
			for( x=bounds.x ; x<bounds.x+bounds.width ; x++) {
				for( y=bounds.y ; y<bounds.y+bounds.height ; y++) {
					boolean tf = grid.shortValue(x,y)>=0;
					land.setValue( x, y, grid.shortValue(x,y)>=0 );
					if( tf ) hasLand=true;
					else hasOcean = true;
				}
			}
			overlay.setGrid(grid, land, hasLand, hasOcean, reset, true);
			return true;
		}

	/* Geting grids for Virtual Ocean. These grids are in the old tile 
	 * indexing system. And gets the grids from GMRT version 1.0 
	 */
	public static Grid2DOverlay getGridWW(Rectangle2D rect, int level) {
		System.out.println("y grid");
		double MERC_MAX_LAT = 81;
		double MERC_MIN_LAT = -79;

		double zoom = 8 * Math.pow(2, level);
		int mapRes = 512;
		int res = mapRes;

		while(zoom*res/mapRes > 1.5 && res>1) {
			res /=2;
		}

		int pixelsPer360 = 1024*320/res;

		Projection proj = //new IdentityProjection(); 
			ProjectionFactory.getMercator(pixelsPer360);

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

		int scale = mapRes/res;

		int x = (int)Math.floor(minX);
		int y = (int)Math.floor(minY);
		int width = (int)Math.ceil(maxX) - x;
		int height = (int)Math.ceil(maxY) - y;

		Rectangle r0 = new Rectangle( 0, -260*scale, 640*scale,
					260*2*scale);

		Rectangle bounds = new Rectangle(x, y, width, height);

		int nLevel = 0;
		int nGrid = 1024/res;
		while( nGrid>8 ) {
			nLevel++;
			nGrid /= 8;
		}
		Grid2D.Short grid = new Grid2D.Short( bounds, proj);
		TileIO tileIO = new TileIO.Short( proj,
				vo_base + "srtm/z_" + res,
				320, nLevel);
		TiledGrid tiler = new TiledGrid( proj, 
						r0,
						tileIO,
						320,
						1,
						null);
		tiler.setWrap( 1024*320/res );
		grid = (Grid2D.Short)tiler.composeGrid(grid);

		Grid2D.Boolean land = new Grid2D.Boolean(bounds, proj);
		boolean hasOcean = true;
		boolean hasLand = false;
// System.out.println( (grid.getBuffer()!=null) +"\t"+ tileIO.getDirPath(r0.x,r0.y)+ tileIO.getName(r0.x,r0.y));
		if( grid.getBuffer()!=null ) {
			hasLand = true;
			hasOcean = false;
			Grid2D.Short ogrid = new Grid2D.Short( bounds, proj);
			tileIO = new TileIO.Short( proj,
				vo_base + "ocean/z_" + res,
				320, nLevel);
			tiler = new TiledGrid( proj, 
						r0,
						tileIO,
						320,
						1,
						null);
			tiler.setWrap( 1024*320/res );
			ogrid = (Grid2D.Short)tiler.composeGrid(ogrid);
			for( x=bounds.x ; x<bounds.x+bounds.width ; x++) {
				for( y=bounds.y ; y<bounds.y+bounds.height ; y++) {
					boolean tf = ogrid.shortValue(x,y)==0;
// System.out.println( x+"\t"+y+"\t"+ogrid.shortValue(x,y));
					land.setValue(x, y, tf);
					if( !tf )hasOcean = true;
				}
			}
		}
		if( hasOcean ) {
			tileIO = new TileIO.Short( proj,
				vo_base + "multibeam/z_" + res,
				320, nLevel);
			tiler = new TiledGrid( proj, 
					r0,
					tileIO,
					320,
					1,
					null);
			tiler.setWrap( 1024*320/res );
			grid = (Grid2D.Short)tiler.composeGrid(grid);
			
			// Puts the resolution 4 grids under the (res) grids
			if( res<4 ) {
				double factor = .25*res;
				scale = mapRes/4;
				double scale2 = 1d * scale / (mapRes / res);

				int x4 = (int)Math.floor(scale2 * minX)-2;
				int y4 = (int)Math.floor(scale2 * minY)-2;
				int width4 = 4 + (int)Math.ceil(scale2 * maxX) - x4;
				int height4 = 4 + (int)Math.ceil(scale2 * maxY) - y4;
				r0 = new Rectangle( 0, -260*scale, 640*scale,
					260*2*scale);

				int pixelsPer360_4 = 1024*320/4; 

				Projection proj4 = ProjectionFactory.getMercator( pixelsPer360_4 );
				Rectangle bounds4 = new Rectangle(x4, y4, width4, height4);
				nLevel = 0;
				nGrid = 1024/4;
				while( nGrid>8 ) {
					nLevel++;
					nGrid /= 8;
				}
				Grid2D.Short g4 = new Grid2D.Short( bounds4, proj4);
				TileIO t4 = new TileIO.Short( proj4,
						vo_base + "multibeam/z_4",
						320, nLevel);
				tiler = new TiledGrid( proj4, 
					r0,
					t4,
					320,
					1,
					null);
				tiler.setWrap( 1024*320/4 );
				g4 = (Grid2D.Short)tiler.composeGrid(g4);
				for( x=bounds.x ; x<bounds.x+bounds.width ; x++) {
					for( y=bounds.y ; y<bounds.y+bounds.height ; y++) {
						if( grid.shortValue(x,y) != grid.NaN )continue;
						if( hasLand && land.booleanValue(x,y) ) continue;
						grid.setValue(x, y, 
							g4.valueAt(factor*x, factor*y, true));
					}
				}
			}
			// Puts the resolution 32 grids under the higher resolution grid
			if( res<32 ) {
				double factor = res/32.;
				scale = mapRes/32;
				double scale2 = 1d * scale / (mapRes / res);

				int x4 = (int)Math.floor(scale2 * minX)-2;
				int y4 = (int)Math.floor(scale2 * minY)-2;
				int width4 = 4 + (int)Math.ceil(scale2 * maxX) - x4;
				int height4 = 4 + (int)Math.ceil(scale2 * maxY) - y4;

				r0 = new Rectangle( 0, -260*scale, 640*scale,
					260*2*scale);

				int pixelsPer360_32 = 1024*320/32; 

				Projection proj4 = ProjectionFactory.getMercator( pixelsPer360_32 );
				Rectangle bounds4 = new Rectangle(x4, y4, width4, height4);

				nLevel = 0;
				nGrid = 1024/32;
				while( nGrid>8 ) {
					nLevel++;
					nGrid /= 8;
				}
				Grid2D.Short g4 = new Grid2D.Short( bounds4, proj4);
				TileIO t4 = new TileIO.Short( proj4,
						vo_base + "multibeam/z_32",
						320, nLevel);
				tiler = new TiledGrid( proj4, 
					r0,
					t4,
					320,
					1,
					null);
				tiler.setWrap( 1024*320/32 );
				g4 = (Grid2D.Short)tiler.composeGrid(g4);
				for( x=bounds.x ; x<bounds.x+bounds.width ; x++) {
					for( y=bounds.y ; y<bounds.y+bounds.height ; y++) {
						if( grid.shortValue(x,y) != grid.NaN ) {
							continue;
						}
						if( hasLand && land.booleanValue(x,y) ) continue;
						grid.setValue(x, y,
							g4.valueAt(factor*x, factor*y, true));
					}
				}
			}
		}
		if( !hasLand ) {
			for( x=bounds.x ; x<bounds.x+bounds.width ; x++) {
				for( y=bounds.y ; y<bounds.y+bounds.height ; y++) {
					boolean tf = grid.shortValue(x,y)>=0;
					land.setValue( x, y, grid.shortValue(x,y)>=0 );
					if( tf ) hasLand=true;
				}
			}
			
		}
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
		overlay.setGrid(grid, land, hasLand, hasOcean, true, true);
		return overlay;
	}
}
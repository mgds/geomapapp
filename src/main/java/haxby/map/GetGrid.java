package haxby.map;

import haxby.proj.Mercator;
import haxby.proj.Projection;
import haxby.proj.ProjectionFactory;
import haxby.util.PathUtil;
import haxby.util.VersionUtil;
import haxby.grid.NetCDFGrid;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.io.FileOutputStream;

import org.geomapapp.grid.Grid2D;
import org.geomapapp.grid.TileIO;
import org.geomapapp.grid.TiledGrid;
import org.geomapapp.grid.TiledMask;

public class GetGrid {
        
    /**
     * The resolution levels at which GDEM data exists.
     */
    final static int[] GDEM_RES_LEVELS = {8192, 2048};

    /**
     * The resolution levels at which the Mulitbeam/Smith and
     * Sandwell composite data exists.
     */
    final static int[] MB_RES_LEVELS = {1024, 512, 64}; // merc (MB, SS)

    /**
     * The number of pixels per 360 degrees for our unscaled Mercator projection.
     */
    final static int PIXELS_PER_360 = 640;
    final static int PIXELS_PER_180 = PIXELS_PER_360 / 2;

    /**
     * The size of the northern or southern hemisphere in our
     * unscaled Mercator projection. Cuts off before the pole.
     */
    final static int PIXELS_PER_NS_HEMISPHERE = 260;

    /**
     * The number of extra pixels to add to a scaled bounding box
     * for a resolution different than the final resolution of the
     * composed grid. This padding ensures that a scaled point
     * from the production resolution to the "different"
     * resolution will not fall outside the bounds of the composed
     * grid.
     */
    final static int DEFAULT_PADDING = 4;

    /**
     * The base URL from which to pull tile data.
     */
    // Uses Path Util to get path from xml file.
	public static String base = PathUtil.getPath("GMRT_LATEST/MERCATOR_GRID_TILE_PATH");
	public static String mb_base = PathUtil.getPath("GMRT_LATEST/MERCATOR_GRID_TILE_PATH");
	static {
		if(MapApp.AT_SEA) {
			base = base.replaceFirst("current", VersionUtil.getVersion("GMRT"));
			mb_base = mb_base.replaceFirst("current", VersionUtil.getVersion("GMRT"));
		}
	}
	
    /**
     * relative paths to data directories
     */
    //Contributed grid content
    static String cGridsDir = "grids";
    //High resolution land data
    static String landDir = "gdem";
    //GMRT curated multibeam data
    static String swathDir = "swath";
    //GEBCO basemap
    static String basemapDir = "gebco_basemap";
    
    /**
     * GetGrid now uses a static grid object that is manipulated by functions
     */
    static Grid2D.Float finalGrid;
	
	
    /**
     * Scale up the bounding box to the desired resolution.
     *
     * @param unscaledBounds the bounding box at the original resolution
     * @param resFrom the original resolution
     * @param resTo the scaling factor for the desired resolution
     * (must be a power of 2)
     * @param padding the number of pixels to add to each
     * dimension of the grid to ensure that scaled points from
     * other bounding box scales fall within this scaled bounding box
     *
     * @return a scaled bounding box for the requested resolution
     * that is rounded outward to the nearest grid node
     */
    public static Rectangle getScaledBounds(Rectangle2D unscaledBounds,
					    int resFrom, 
					    int resTo,
					    int padding) {
		Rectangle2D rect = unscaledBounds;
		double scalingFactor = (double) resTo / (double) resFrom;
	
		int x = (int) Math.floor(scalingFactor * rect.getX()) - padding;
		int y = (int) Math.floor(scalingFactor * rect.getY()) - padding;
		int width = 2 * padding + (int) Math.ceil( 
							  scalingFactor * (rect.getX() + rect.getWidth()) 
							   ) - x;
		int height = 2 * padding + (int) Math.ceil( 
							   scalingFactor * (rect.getY() + rect.getHeight())
							    ) - y;
	
		return new Rectangle(x, y, width, height);
    }
                                        
    /**
     * Return the subdirectory depth of the tileset at the given
     * resolution. Necessary for instatiating a TileIO object.
     *
     * @param res the resolution in question
     *
     * @return the number of directory 
     */
    public static int getNLevel(int res) {
		int nLevel = 0;
	
		while (res >= 8) {
		    res /= 8;
		    nLevel++;
		}
	
		return nLevel;
    }
  
    
    /**
     * Fills a from a Grid2D.Short with data from the requested
     * resolution and tileset. Does not overwrite existing values,
     * replacing only NaNs.
     * 
     * @param tilesPrefix the path from GetGrid.base to the tiles,
     * including the "z_", "zw_", or other prefix (e.g. "gdem/z_")
     * @param res the scaling factor the the new resolution (must
     * be a power of 2)
     * @param grid the grid to be populated
     */
    public static void fillShortGrid(String tilesPrefix,
		     int gridRes,
		     int fillRes, 
		     String basePath) {
    	fillShortGrid(tilesPrefix, gridRes, fillRes, basePath, false);
    }
    
    public static void fillShortGrid(String tilesPrefix,
				     int gridRes,
				     int fillRes, 
				     String basePath, boolean isGMRT) {
		// DEBUG
	//	System.out.println("Filling from " + baseUrl + tilesPrefix + fillRes);
	
		// setup tile IO
		Rectangle gridBounds = finalGrid.getBounds();
		Rectangle fillBounds = getScaledBounds(gridBounds, gridRes, fillRes, DEFAULT_PADDING);
		double scaleFactor = ((double) fillRes) * 1.0 / ((double) gridRes);
		Projection proj = ProjectionFactory.getMercator(PIXELS_PER_360 * fillRes);
		Rectangle fillBaseRect = new Rectangle(0, 
			       (-PIXELS_PER_NS_HEMISPHERE * fillRes)-DEFAULT_PADDING,
			       PIXELS_PER_360 * fillRes,
			       (PIXELS_PER_NS_HEMISPHERE * 2 * fillRes)+DEFAULT_PADDING);
		TileIO fillIO = new TileIO.Short(proj,
						 basePath + tilesPrefix + "/z_" + fillRes,
						 PIXELS_PER_180,
						 getNLevel(fillRes));
		TiledGrid fillTiler = new TiledGrid(proj, 
						    fillBaseRect,
						    fillIO,
						    PIXELS_PER_180,
						    1,
						    null);
		fillTiler.setWrap(PIXELS_PER_360 * fillRes);
	
		// read in grid contents to buffer
		Grid2D.Short fillGrid = new Grid2D.Short(fillBounds, proj);
		fillGrid = (Grid2D.Short)fillTiler.composeGrid(fillGrid);
	
		// write contents to grid; if we're filling from the
		// same resolution as the final product, no need to
		// scale
	
		// DEBUG
//		int gridNaNs = 0;
//		int fillVals = 0;
	
		if (gridRes == fillRes) {
		    for (int x = gridBounds.x; x < gridBounds.x + gridBounds.width; x++) {
				for (int y = gridBounds.y; y < gridBounds.y + gridBounds.height; y++) {
				    double new_z = fillGrid.valueAt(x, y);
				    double old_z = finalGrid.valueAt(x, y);
				    if (Double.isNaN(old_z)) {
						finalGrid.setValue(x, y, new_z);
						// DEBUG
//						if (!Double.isNaN(new_z))
//						    fillVals++;
//						gridNaNs++;
				    }
				}
		    }
		} else {
		    for (int x = gridBounds.x; x < gridBounds.x + gridBounds.width; x++) {
				for (int y = gridBounds.y; y < gridBounds.y + gridBounds.height; y++) {
				    // induces bicubic interpolation
				    double new_z = fillGrid.valueAt(scaleFactor * x, scaleFactor * y, isGMRT);
				    double old_z = finalGrid.valueAt(x, y);
				    if (Double.isNaN(old_z)) {
						finalGrid.setValue(x, y, new_z);
						// DEBUG
//						if (!Double.isNaN(new_z))
//						    fillVals++;
//						gridNaNs++;
				    }
				}
		    }
		}
	
		// DEBUG
		//long total = gridBounds.width * gridBounds.height;
		//System.out.printf("%d of %d nodes were NaNs\n", gridNaNs, total);
		//System.out.printf("%d of %d nodes were filled with something other than a NaN\n", fillVals, total);
	}
    
    /**
     * Fills from a Grid2D.Float with data from the requested
     * resolution and tileset. Does not overwrite existing values,
     * replacing only NaNs.
     * 
     * @param tilesPrefix the path from GetGrid.base to the tiles,
     * including the "z_", "zw_", or other prefix (e.g. "gdem/z_")
     * @param gridRes the resolution of the input tiles (must
     * be a power of 2)
     * @param fillRes the resolution of the output tile (must
     * be a power of 2)
     * @param basePath the basepath to use
     */
    public static void fillFloatGrid(String tilesPrefix,
		     int gridRes,
		     int fillRes, 
            String basePath) {
    	fillFloatGrid (tilesPrefix, gridRes, fillRes, basePath, false);
    }
    
    public static void fillFloatGrid(String tilesPrefix,
				     int gridRes,
				     int fillRes, 
                     String basePath, boolean isGMRT) {

		// DEBUG
		//System.out.println("Filling from " + base + tilesPrefix + fillRes);
	
		// setup tile IO
		Rectangle gridBounds = finalGrid.getBounds();
		Rectangle fillBounds = getScaledBounds(gridBounds, gridRes, fillRes, DEFAULT_PADDING);
		double scaleFactor = ((double) fillRes) * 1.0 / ((double) gridRes);
		Projection fillProj = ProjectionFactory.getMercator(PIXELS_PER_360 * fillRes);
		Rectangle fillBaseRect = new Rectangle(0, 
			       (-PIXELS_PER_NS_HEMISPHERE * fillRes)-DEFAULT_PADDING,
			       PIXELS_PER_360 * fillRes,
			       (PIXELS_PER_NS_HEMISPHERE * 2 * fillRes)+DEFAULT_PADDING);
		TileIO fillIO = new TileIO.Float(fillProj,
						 basePath + tilesPrefix + "/z_" + fillRes,
						 PIXELS_PER_180,
						 getNLevel(fillRes));
		TiledGrid fillTiler = new TiledGrid(fillProj,
						    fillBaseRect,
						    fillIO,
						    PIXELS_PER_180,
						    1,
						    null);
		fillTiler.setWrap(PIXELS_PER_360 * fillRes);
	
		// read in grid contents to buffer
		Grid2D.Float fillGrid = new Grid2D.Float(fillBounds, fillProj);
		fillGrid = (Grid2D.Float)fillTiler.composeGrid(fillGrid);
	
		// write contents to grid; if we're filling from the
		// same resolution as the final product, no need to
		// scale
			
		// DEBUG
		//int gridNaNs = 0;
		//int fillVals = 0;
	
		if (gridRes == fillRes) {
		    for (int x = gridBounds.x; x < gridBounds.x + gridBounds.width; x++) {
				for (int y = gridBounds.y; y < gridBounds.y + gridBounds.height; y++) {
				    double new_z = fillGrid.valueAt(x, y);
				    double old_z = finalGrid.valueAt(x, y);
				    if (Double.isNaN(old_z)) {
						finalGrid.setValue(x, y, new_z);
						// DEBUG
						//if (!Double.isNaN(new_z))
						 //   fillVals++;
						//gridNaNs++;
				    }
				}
		    }
		} else {
		    for (int x = gridBounds.x; x < gridBounds.x + gridBounds.width; x++) {
				for (int y = gridBounds.y; y < gridBounds.y + gridBounds.height; y++) {
				    // induces bicubic interpolation
				    double new_z = fillGrid.valueAt(scaleFactor * x, scaleFactor * y, isGMRT);
				    double old_z = finalGrid.valueAt(x, y);
				    if (Double.isNaN(old_z)) {
						finalGrid.setValue(x, y, new_z);
						// DEBUG
	//					if (!Double.isNaN(new_z))
	//					    fillVals++;
	//					gridNaNs++;
				    }
				}
		    }
		}
	
		// DEBUG
		//long total = gridBounds.width * gridBounds.height;
		//System.out.printf("%d of %d nodes were NaNs\n", gridNaNs, total);
		//System.out.printf("%d of %d nodes were filled with something other than a NaN\n", fillVals, total);
    }

    /**
     * Apply the hi-res mask to the grid, converting all non-hi-res data points to NaNs.
     * 
     * @param tilesPrefix the name of the directory where the mask
     * tiles are located, plus the prefix for a masked tile
     * (e.g. for /path/to/merc320/mask/m_* tilesPrefix is
     * "mask/m_"
     * @param gridRes the resolution of the grid to mask
     * @param grid the grid to mask
     */
    public static void applyMask(String tilesPrefix,
				 int gridRes,
				 Grid2D grid, String baseUrl) {
	// setup tile IO
	Rectangle gridBounds = grid.getBounds();
	int maskRes = gridRes;
	Projection maskProj = ProjectionFactory.getMercator(PIXELS_PER_360 * maskRes);
	Rectangle maskBaseRect = new Rectangle(0, 
					       -PIXELS_PER_NS_HEMISPHERE * maskRes,
					       PIXELS_PER_360 * maskRes,
					       PIXELS_PER_NS_HEMISPHERE * 2 * maskRes);
	TileIO.Boolean maskIO = new TileIO.Boolean(maskProj,
					 baseUrl + tilesPrefix + "/m_" + maskRes,
					 PIXELS_PER_180,
					 getNLevel(maskRes));
	TiledMask maskTiler = new TiledMask(maskProj,
					    maskBaseRect,
					    maskIO,
					    PIXELS_PER_180,
					    1,
					    null);
	maskTiler.setWrap(PIXELS_PER_360 * maskRes);
	// read in grid contents to buffer
	Rectangle maskBounds = gridBounds;
	Grid2D.Boolean maskGrid = new Grid2D.Boolean(maskBounds, maskProj);
	maskGrid = (Grid2D.Boolean)maskTiler.composeGrid(maskGrid);

	// DEBUG
	int t = 0;
	int f = 0;

	for (int x = gridBounds.x; x < gridBounds.x + gridBounds.width; x++) {
	    for (int y = gridBounds.y; y < gridBounds.y + gridBounds.height; y++) {
		/*if (maskGrid.booleanValue(x,y)) {
		    grid.setValue(x, y, Float.NaN);
		    t++;
		} else {
		    f++;
		}*/
			float d = (float) grid.valueAt(x, y);
			if( !Float.isNaN(d) && d < 0 && maskGrid.valueAt(x, y) == 0)
				grid.setValue(x, y, Float.NaN);
	    }
	}
	//System.out.printf("True: %d\nFalse %d\n", t, f);
    }

    /**
     * Set the base URL for grabbing tile content.
     *
     * @param baseURL the new base URL
     */
    public static void setBaseURL(String baseURL) {
    	base = baseURL;
    }

    /**
     * Compose a new grid of a given region and resolution. The
     * grid may be optionally masked to include only high
     * resolution content.
     *
     * @param projectedBounds a double-valued bounding box
     * represent merc-320 projected coordinates
     * @param boundsRes the resolution of the bounding box
     * @param res the resolution of composed grid
     * @param masked whether to mask the composed grid
     */
    public static Grid2D.Float getGrid(Rectangle2D.Double unscaledBounds, int boundsRes, int res, boolean masked, String basePath, String mbPath) {
		Rectangle projectedBounds = getScaledBounds(unscaledBounds, boundsRes, res, 0); // no padding
		return getGrid(projectedBounds, res, masked, basePath, mbPath);
    }

    
    
    /**
     * Compose a new grid of a given region and resolution. The
     * grid may be optionally masked to include only high
     * resolution content. 
     *
     * @param projectedBounds a bounding box projected to merc-320
     * at the requested resolution of the composed grid
     * @param res the resolution of composed grid
     * @param masked whether to mask the composed grid
     */
    public static Grid2D.Float getGrid(Rectangle projectedBounds, int res, boolean masked, String basePath, String mbPath) {
    	return getGrid(projectedBounds, res, masked, basePath, mbPath, false);
    }
    
    public static Grid2D.Float getGrid(Rectangle projectedBounds, int res, boolean masked, String basePath, String mbPath, boolean isGMRT) {
		// A record of the current state of the live tiles
		// String[] tilePrefixes_Short = {"gdem/z_", "ocean/z", "grids/z_"};
		// String[] tilePrefixes_Float = {"z_"};
		// String[] tilePrefixes_Boolean = {"mask/m_"}; 
	
		// create our grid2D float container
		finalGrid = new Grid2D.Float(
	        projectedBounds,
	        ProjectionFactory.getMercator(PIXELS_PER_360 * res)
		);
	
		// The grid composer follows a "fill-in" logic,
		// whereby only NaN's in the grid buffer can be
		// overwritten by subsequent data. Therefore, we write
		// the highest resolution data first and "fill-in"
		// around it.
	
		// contributed grids at res, down through all relevant grid content
		
	    int cGridsFinalRes = (res>512) ? 512 : res;
		for (int res0 = res; res0 >= cGridsFinalRes; res0 /= 2) {
	        fillFloatGrid( cGridsDir , res , res0, basePath, isGMRT );
	    }
	    
		// gdem @ res and at 8192/2048 if required
		if (res >= 128) {
			fillShortGrid( landDir , res , res, basePath, isGMRT );
			if (res > 8192) fillShortGrid( landDir , res, 8192, basePath, isGMRT );
			if (res > 2048) fillShortGrid( landDir , res , 2048, basePath, isGMRT );
		}
	
		// multibeam from scale down to 64
	    if (res >= 512) {
	        for (int res0 = res; res0 >= 512; res0 /= 2) {
	            fillFloatGrid( "" , res , res0 ,  mbPath, isGMRT );
	            if (!basePath.equals(mbPath)) fillFloatGrid( "" , res , res0, basePath, isGMRT );
		    }
	    } else {
	        //Bypasses normal workflow if res is 128 or less
	        //The normal mbPath has GEBCO burned in from 64 to 1
	        //The swathDir is only multibeam data.
	        //Using the swathDir eliminates interpolation overshoots
	        //    introduced by burning in GEBCO
	        String fillDir = ((res >= 256)?"":swathDir);
			fillFloatGrid( fillDir , res , res , mbPath, isGMRT );
	        if (!basePath.equals(mbPath)) fillFloatGrid( fillDir , res , res , basePath, isGMRT);
		}
	
		
		// Masking not necessary if grid built in this order
		if (masked) {
		    /*applyMask("mask",
			      res,
			      (Grid2D) finalGrid);*/
		} else {
		// fill in remaining NaNs with GEBCO
	        int basemapRes = Math.min(res,128);
			fillFloatGrid(basemapDir, res, basemapRes, basePath, isGMRT);
		}
	                
		return finalGrid;
    }  
    
    
     
    private static void printUsage(String[] args, String message) {
	System.err.println("--- error  -- ");
	System.err.println("to compose a grid with bounds \n\twest=20, east=40, south=-20, north=0");
	System.err.println("\tand resolution 4 (must be power of 2 between 1 [~100 m/node] and 512");
	System.err.println("command should be form:");
	System.err.println("\tjava xb.map.GetGrid 20 40 -20 0 4 <masked> <file_prefix>" );
	if (args != null && args.length > 0) {
	    System.err.println(args.length + " arguments");
	    for (int k = 0; k < args.length; k++) {
		System.err.println(k +"\t"+ args[k]);
	    }
	} else {
	    System.err.println("no arguments");
	}
	if (message!=null) System.err.println(message );
	System.exit(0);
    }

    /**
     * Construct a grid at a particular resolution that encompasses a WESN bounding box.
     *
     * USAGE:
     * java xb.map.GetGrid <w> <e> <s> <n> <res> <masked> <output file name>
     * <masked> should be 1 for a masked grid, 0 for an unmasked
     * grid
     * <res> must be a power of 2
     */ 
    public static void main(String[] args ) {

	// parse command line arguments
	if (args.length != 7) {
	    printUsage(args, "wrong arg length: " + args.length);
	}

	double[] wesn = new double[] { Double.parseDouble(args[0]),
				       Double.parseDouble(args[1]),
				       Double.parseDouble(args[2]),
				       Double.parseDouble(args[3]) };
	int res = Integer.parseInt(args[4]) ;
	boolean masked = Integer.parseInt(args[5]) == 1;
                
	// make sure res is a power of 2
	int kres=0;
	for (kres = 1; kres < 600; kres *= 2) {
	    if (res == kres) {
		break;
	    }
	}
	if (res != kres) {
	    printUsage(args, "res = " + res + ", kres = " + kres + "\t" + res);
	}

	// make sure all of the wesn were actually parsed corretly
	for (int k = 0; k < 4; k++) {
	    if (Double.isNaN(wesn[k])) {
		printUsage(args, null);
	    }
	}

	// wrap wesn
	while (wesn[0] > wesn[1]) {
	    wesn[0] -= 360.;
	}
	while (wesn[1] > wesn[0] + 360.) {
	    wesn[0]-=360.;
	}
	while (wesn[0] < 0.) {
	    wesn[1] += 360.;
	    wesn[0] += 360.;
	}
	while (wesn[0]>=360.) {
	    wesn[1] -= 360.;
	    wesn[0] -= 360.;
	}

	// create unscaled, projected bounding box
	Mercator merc = ProjectionFactory.getMercator(PIXELS_PER_360);
	double ymin = merc.getY(wesn[3]);
	double ymax = merc.getY(wesn[2]);
	Rectangle2D.Double area = new Rectangle2D.Double(
							 wesn[0] * PIXELS_PER_360 / 360., 
							 ymin, 
							 (wesn[1] - wesn[0]) * PIXELS_PER_360 / 360., 
							 ymax - ymin     );
	Grid2D.Float grid = null;

	// try to create the grid
	try {
	    grid = getGrid(area, 1, res, masked, base, mb_base);
	} catch (Throwable ex) {
	    System.err.println("An error occured while composing grd file");
	    System.err.println(ex.getMessage());

	    if (ex instanceof OutOfMemoryError) {
		System.err.println("out of memory");
	    }

	    ex.printStackTrace(System.err);
	    System.exit(1);
	}
                
	// try to create the output file for writing
	String name =  args[6] + ".grd";
	FileOutputStream out = null;
	try {
	    out = new FileOutputStream(name);
	} catch (Exception ex) {
	    System.err.println("Could not create output file");
	    System.err.println("ex.getMessage()");
	    ex.printStackTrace(System.err);
	    System.exit(1);
	}

	try {
	  //  NetCDFGrid.createStandardGrd(grid, null, (OutputStream) out);
	} catch (Throwable ex) {
	    System.err.println("An error occured while composing grd file");
	    System.err.println(ex.getMessage());

	    if (ex instanceof OutOfMemoryError) {
		System.err.println("out of memory");
	    }

	    ex.printStackTrace(System.err);
	    System.exit(1);
	}
    }
}

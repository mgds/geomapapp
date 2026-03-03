package xb.map;

import haxby.proj.Mercator;
import haxby.proj.Projection;
import haxby.proj.ProjectionFactory;

import haxby.grid.NetCDFGrid;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import java.io.OutputStream;
import java.io.FileOutputStream;

import org.geomapapp.grid.Grid2D;
import org.geomapapp.grid.TileIO;
import org.geomapapp.grid.TiledGrid;
// import org.geomapapp.grid.TiledMask;

import tools.GmrtUtil;

public class GetGrid {

  /**
   * The resolution levels at which GDEM data exists. (No longer used?)
   */
  final static int[] GDEM_RES_LEVELS = { 8192, 2048 };

  /**
   * The resolution levels at which the Mulitbeam/Smith and Sandwell composite
   * data exists. (No longer used?)
   */
  final static int[] MB_RES_LEVELS = { 1024, 512, 64 }; // merc (MB, SS)

  /**
   * The number of pixels per 360 degrees for our unscaled Mercator projection.
   */
  final static int PIXELS_PER_360 = 640;
  final static int PIXELS_PER_180 = PIXELS_PER_360 / 2;

  /**
   * The number of extra pixels to add to a scaled bounding box for a resolution
   * different than the final resolution of the composed grid. This padding
   * ensures that a scaled point from the production resolution to the "different"
   * resolution will not fall outside the bounds of the composed grid.
   */
  final static int DEFAULT_PADDING = 4;

  // The basepath/URL from which to pull tile data.
  static String base = "/tiles/current/merc_320/";
  // The basepath/URL for the multibeam tile directory
  static String mbPath = "/tiles/current/merc_320/";

  // Old base URLs for testing, but provides example of setting
  // basepaths via absolute URL instead of absolute path
  // static String base = "http://dev2.geomapapp.org/merc/";
  // static String base = "http://gmrt.marine-geo.org/tiles/2.0/merc_320/";

  /**
   * Toggled by SetBaseURL and SetMBURL functions. If true, init() function does
   * not try to load urls from configuration file
   */
  static boolean customPath = false;

  /*
   * relative paths to data directories
   */
  // Contributed grid content
  static final String cGridsDir = "grids";
  // High resolution land data
  static final String landDir = "gdem";
  // GMRT curated multibeam data
  static final String swathDir = "swath";
  // GEBCO basemap
  static final String basemapDir = "gebco_basemap";

  /**
   * GetGrid now uses a static grid object that is manipulated by functions
   */

  /**
   * Sets basepaths based on configuration file
   */
  public static void init() {
    if (!customPath) {
      GetGrid.base = GmrtUtil.getProperty("getgrid_tile_dir");
      GetGrid.mbPath = GmrtUtil.getProperty("getgrid_tile_dir");
    }
  }

  /**
   * Scale up the bounding box to the desired resolution.
   *
   * @param unscaledBounds the bounding box at the original resolution
   * @param resFrom        the original resolution
   * @param resTo          the scaling factor for the desired resolution (must be
   *                       a power of 2)
   * @param padding        the number of pixels to add to each dimension of the
   *                       grid to ensure that scaled points from other bounding
   *                       box scales fall within this scaled bounding box
   *
   * @return a scaled bounding box for the requested resolution that is rounded
   *         outward to the nearest grid node
   */
  public static Rectangle getScaledBounds(Rectangle2D unscaledBounds, int resFrom, int resTo, int padding) {
    Rectangle2D rect = unscaledBounds;
    double scalingFactor = (double) resTo / (double) resFrom;

    int x = (int) Math.floor(scalingFactor * rect.getX()) - padding;
    int y = (int) Math.floor(scalingFactor * rect.getY()) - padding;
    int width = padding + (int) Math.ceil(scalingFactor * (rect.getX() + rect.getWidth())) - x;
    int height = padding + (int) Math.ceil(scalingFactor * (rect.getY() + rect.getHeight())) - y;
    return new Rectangle(x, y, width, height);
  }

  /**
   * Fills a from a Grid2D.Short with data from the requested resolution and
   * tileset. Does not overwrite existing values, replacing only NaNs. Uses
   * default basepath.
   *
   * @param tilesPrefix the path from GetGrid.base to the tiles, including the
   *                    "z_", "zw_", or other prefix (e.g. "gdem/z_")
   * @param gridRes     the resolution of the input tiles (must be a power of 2)
   * @param fillRes     the resolution of the output tile (must be a power of 2)
   */
  public static void fillShortGrid(String tilesPrefix, int gridRes, int fillRes, Grid2D.Float finalGrid) {
    fillShortGrid(tilesPrefix, gridRes, fillRes, GetGrid.base, finalGrid);
  }

  /**
   * Fills a from a Grid2D.Short with data from the requested resolution and
   * tileset. Does not overwrite existing values, replacing only NaNs.
   *
   * @param tilesPrefix the path from GetGrid.base to the tiles, including the
   *                    "z_", "zw_", or other prefix (e.g. "gdem/z_")
   * @param gridRes     the resolution of the input tiles (must be a power of 2)
   * @param fillRes     the resolution of the output tile (must be a power of 2)
   * @param basePath    The absolute basepath to use
   */
  public static void fillShortGrid(String tilesPrefix, int gridRes, int fillRes, String basePath,
      Grid2D.Float finalGrid) {
    // DEBUG
    // System.out.println("Filling from " + base + tilesPrefix + fillRes);

    // setup tile IO
    Rectangle gridBounds = finalGrid.getBounds();
    Rectangle fillBounds = getScaledBounds(gridBounds, gridRes, fillRes, DEFAULT_PADDING);
    double scaleFactor = ((double) fillRes) * 1.0 / ((double) gridRes);
    Projection proj = ProjectionFactory.getMercator(PIXELS_PER_360 * fillRes);
    Rectangle fillBaseRect = new Rectangle(0, (-PIXELS_PER_360 * 3 * fillRes) - DEFAULT_PADDING,
        PIXELS_PER_360 * fillRes, (PIXELS_PER_360 * 6 * fillRes) + DEFAULT_PADDING);
    TileIO fillIO = new TileIO.Short(proj, basePath + tilesPrefix + "/z_" + fillRes, PIXELS_PER_180,
        GmrtUtil.nLevel(fillRes));
    TiledGrid fillTiler = new TiledGrid(proj, fillBaseRect, fillIO, PIXELS_PER_180, 1, null);
    fillTiler.setWrap(PIXELS_PER_360 * fillRes);

    // read in grid contents to buffer
    Grid2D.Short fillGrid = new Grid2D.Short(fillBounds, proj);

    fillGrid = (Grid2D.Short) fillTiler.composeGrid(fillGrid);

    // write contents to grid; if we're filling from the
    // same resolution as the final product, no need to
    // scale

    // DEBUG
    // int gridNaNs = 0;
    // int fillVals = 0;

    if (gridRes == fillRes) {
      for (int x = gridBounds.x; x < gridBounds.x + gridBounds.width; x++) {
        for (int y = gridBounds.y; y < gridBounds.y + gridBounds.height; y++) {
          double new_z = fillGrid.valueAt(x, y);
          double old_z = finalGrid.valueAt(x, y);
          if (Double.isNaN(old_z)) {
            finalGrid.setValue(x, y, new_z);
            // DEBUG
            // if (!Double.isNaN(new_z))
            // fillVals++;
            // gridNaNs++;
          }
        }
      }
    } else {
      for (int x = gridBounds.x; x < gridBounds.x + gridBounds.width; x++) {
        for (int y = gridBounds.y; y < gridBounds.y + gridBounds.height; y++) {
          // induces bicubic interpolation
          double new_z = fillGrid.valueAtNanAware(scaleFactor * x, scaleFactor * y);
          double old_z = finalGrid.valueAt(x, y);
          if (Double.isNaN(old_z)) {
            finalGrid.setValue(x, y, new_z);
            // DEBUG
            // if (!Double.isNaN(new_z))
            // fillVals++;
            // gridNaNs++;
          }
        }
      }
    }
    // DEBUG
    // long total = gridBounds.width * gridBounds.height;
    // System.out.printf("%d of %d nodes were NaNs\n", gridNaNs, total);
    // System.out.printf("%d of %d nodes were filled with something other than a
    // NaN\n", fillVals, total);
  }

  /**
   * Gets a single point from a grid layer at a specific res with default basepath
   *
   * @param tilesPrefix the path from GetGrid.base to the tiles, including the
   *                    "z_", "zw_", or other prefix (e.g. "gdem/z_")
   * @param gridRes     the resolution of the input tiles (must be a power of 2)
   * @param lat         The latitude to find the value for.
   * @param lon         The longitude to find the value for.
   */
  public static double getShortGridVal(String tilesPrefix, int gridRes, double lat, double lon) {
    return getShortGridVal(tilesPrefix, gridRes, lat, lon, GetGrid.base);
  }

  /**
   * Gets a single point from a grid layer at a specific res
   *
   * @param tilesPrefix the path from GetGrid.base to the tiles, including the
   *                    "z_", "zw_", or other prefix (e.g. "gdem/z_")
   * @param gridRes     the resolution of the input tiles (must be a power of 2)
   * @param lat         The latitude to find the value for.
   * @param lon         The longitude to find the value for.
   * @param basePath    The absolute basepath to use
   */
  public static double getShortGridVal(String tilesPrefix, int gridRes, double lat, double lon, String basePath) {
    Mercator proj = ProjectionFactory.getMercator(PIXELS_PER_360 * gridRes);
    Rectangle fillBaseRect = new Rectangle(0, (-PIXELS_PER_360 * 3 * gridRes) - DEFAULT_PADDING,
        PIXELS_PER_360 * gridRes, (PIXELS_PER_360 * 6 * gridRes) + DEFAULT_PADDING);
    TileIO fillIO = new TileIO.Short(proj, basePath + tilesPrefix + "/z_" + gridRes, PIXELS_PER_180,
        GmrtUtil.nLevel(gridRes));
    TiledGrid fillTiler = new TiledGrid(proj, fillBaseRect, fillIO, PIXELS_PER_180, 1, null);
    fillTiler.setWrap(PIXELS_PER_360 * gridRes);
    double x = lon * ((double) gridRes) * ((double) PIXELS_PER_360) / 360.;
    if (x < 0.)
      x += (double) (PIXELS_PER_360 * gridRes);
    double y = proj.getY(lat);
    double val = fillTiler.valueAtNanAware(x, y);
    // System.out.printf("%d ",PIXELS_PER_360 * gridRes);
    // System.out.printf("%s %.6f %.6f %.6f %.6f
    // %.6f%n",tilesPrefix,lon,lat,x,y,val);
    return val;
  }

  /**
   * Gets a single point from a grid layer at a specific res. Uses default
   * basepath
   *
   * @param tilesPrefix the path from GetGrid.base to the tiles, including the
   *                    "z_", "zw_", or other prefix (e.g. "gdem/z_")
   * @param gridRes     the resolution of the input tiles (must be a power of 2)
   * @param lat         The latitude to find the value for.
   * @param lon         The longitude to find the value for.
   */
  public static double getFloatGridVal(String tilesPrefix, int gridRes, double lat, double lon) {
    return getFloatGridVal(tilesPrefix, gridRes, lat, lon, base);
  }

  /**
   * Gets a single point from a grid layer at a specific res
   *
   * @param tilesPrefix the path from GetGrid.base to the tiles, including the
   *                    "z_", "zw_", or other prefix (e.g. "gdem/z_")
   * @param gridRes     the resolution of the input tiles (must be a power of 2)
   * @param lat         The latitude to find the value for.
   * @param lon         The longitude to find the value for.
   * @param basePath    The basePath to use
   */
  public static double getFloatGridVal(String tilesPrefix, int gridRes, double lat, double lon, String basePath) {
    Mercator proj = ProjectionFactory.getMercator(PIXELS_PER_360 * gridRes);
    Rectangle fillBaseRect = new Rectangle(0, (-PIXELS_PER_360 * 3 * gridRes) - DEFAULT_PADDING,
        PIXELS_PER_360 * gridRes, (PIXELS_PER_360 * 6 * gridRes) + DEFAULT_PADDING);
    TileIO fillIO = new TileIO.Float(proj, basePath + tilesPrefix + "/z_" + gridRes, PIXELS_PER_180,
        GmrtUtil.nLevel(gridRes));
    TiledGrid fillTiler = new TiledGrid(proj, fillBaseRect, fillIO, PIXELS_PER_180, 1, null);
    fillTiler.setWrap(PIXELS_PER_360 * gridRes);
    double x = lon * ((double) gridRes) * ((double) PIXELS_PER_360) / 360.;
    if (x < 0.)
      x += (double) (PIXELS_PER_360 * gridRes);
    double y = proj.getY(lat);
    double val = fillTiler.valueAtNanAware(x, y);
    // System.out.printf("%d ",PIXELS_PER_360 * gridRes);
    // System.out.printf("%s %.6f %.6f %.6f %.6f
    // %.6f%n",tilesPrefix,lon,lat,x,y,val);
    return val;
  }

  /**
   * Fills from a Grid2D.Float with data from the requested resolution and
   * tileset. Does not overwrite existing values, replacing only NaNs. Uses
   * default basepath
   *
   * @param tilesPrefix the path from GetGrid.base to the tiles, including the
   *                    "z_", "zw_", or other prefix (e.g. "gdem/z_")
   * @param gridRes     the resolution of the input tiles (must be a power of 2)
   * @param fillRes     the resolution of the output tile (must be a power of 2)
   */
  public static void fillFloatGrid(String tilesPrefix, int gridRes, int fillRes, Grid2D.Float finalGrid) {
    fillFloatGrid(tilesPrefix, gridRes, fillRes, base, finalGrid);
  }

  /**
   * Fills from a Grid2D.Float with data from the requested resolution and
   * tileset. Does not overwrite existing values, replacing only NaNs.
   *
   * @param tilesPrefix the path from GetGrid.base to the tiles, including the
   *                    "z_", "zw_", or other prefix (e.g. "gdem/z_")
   * @param gridRes     the resolution of the input tiles (must be a power of 2)
   * @param fillRes     the resolution of the output tile (must be a power of 2)
   * @param basePath    the basepath to use
   */
  public static void fillFloatGrid(String tilesPrefix, int gridRes, int fillRes, String basePath,
      Grid2D.Float finalGrid) {
    // DEBUG
    // System.out.println("Filling from " + base + tilesPrefix + fillRes);
    // setup tile IO
    // System.out.printf("%d %d%n",gridRes,fillRes);
    Rectangle gridBounds = finalGrid.getBounds();
    Rectangle fillBounds = getScaledBounds(gridBounds, gridRes, fillRes, DEFAULT_PADDING);
    double scaleFactor = ((double) fillRes) * 1.0 / ((double) gridRes);
    Projection fillProj = ProjectionFactory.getMercator(PIXELS_PER_360 * fillRes);
    Rectangle fillBaseRect = new Rectangle(0, (-PIXELS_PER_360 * 3 * fillRes) - DEFAULT_PADDING,
        PIXELS_PER_360 * fillRes, (PIXELS_PER_360 * 6 * fillRes) + DEFAULT_PADDING);
    TileIO fillIO = new TileIO.Float(fillProj, basePath + tilesPrefix + "/z_" + fillRes, PIXELS_PER_180,
        GmrtUtil.nLevel(fillRes));
    TiledGrid fillTiler = new TiledGrid(fillProj, fillBaseRect, fillIO, PIXELS_PER_180, 1, null);
    fillTiler.setWrap(PIXELS_PER_360 * fillRes);

    // read in grid contents to buffer
    Grid2D.Float fillGrid = new Grid2D.Float(fillBounds, fillProj);
    fillGrid = (Grid2D.Float) fillTiler.composeGrid(fillGrid);

    // write contents to grid; if we're filling from the
    // same resolution as the final product, no need to
    // scale
    // DEBUG
    // int gridNaNs = 0;
    // int fillVals = 0;

    if (gridRes == fillRes) {
      // System.out.printf("%d %d%n",gridBounds.x,gridBounds.y);
      for (int x = gridBounds.x; x < gridBounds.x + gridBounds.width; x++) {
        for (int y = gridBounds.y; y < gridBounds.y + gridBounds.height; y++) {
          double new_z = fillGrid.valueAt(x, y);
          double old_z = finalGrid.valueAt(x, y);
          if (Double.isNaN(old_z)) {
            finalGrid.setValue(x, y, new_z);
            // DEBUG
            // if (!Double.isNaN(new_z))
            // fillVals++;
            // gridNaNs++;
          }
        }
      }
    } else {
      for (int x = gridBounds.x; x < gridBounds.x + gridBounds.width; x++) {
        for (int y = gridBounds.y; y < gridBounds.y + gridBounds.height; y++) {
          // induces bicubic interpolation
          double new_z = fillGrid.valueAtNanAware(scaleFactor * x, scaleFactor * y);
          double old_z = finalGrid.valueAt(x, y);

          if (Double.isNaN(old_z)) {
            finalGrid.setValue(x, y, new_z);
            // DEBUG
            // if (!Double.isNaN(new_z))
            // fillVals++;
            // gridNaNs++;
          }
        }
      }
    }

    // DEBUG
    // long total = gridBounds.width * gridBounds.height;
    // System.out.printf("%d of %d nodes were NaNs\n", gridNaNs, total);
    // System.out.printf("%d of %d nodes were filled with something other than a
    // NaN\n", fillVals, total);
  }

  /**
   * Set the base URL for grabbing tile content. Also sets multibeam basepath
   *
   * @param baseURL the new base URL
   */
  public static void setBaseURL(String baseURL) {
    base = baseURL;
    mbPath = baseURL;
    customPath = true;
  }

  /**
   * Set the base URL for grabbing tile content for multibeam. Multibeam requires
   * a different basepath when adding a cruise to GMRT, But for other uses is the
   * same as base variable.
   *
   * @param baseURL the new base URL
   */
  public static void setMBURL(String mbURL) {
    mbPath = mbURL;
    customPath = true;
  }

  /**
   * Compose a new grid of a given region and resolution. The grid may be
   * optionally masked to include only high resolution content.
   *
   * @param projectedBounds a double-valued bounding box represent merc-320
   *                        projected coordinates
   * @param boundsRes       the resolution of the bounding box
   * @param res             the resolution of composed grid
   * @param masked          whether to mask the composed grid
   */
  public static Grid2D.Float getGrid(Rectangle2D.Double unscaledBounds, int boundsRes, int res, boolean masked) {
    Rectangle projectedBounds = getScaledBounds(unscaledBounds, boundsRes, res, 0); // no padding
    return getGrid(projectedBounds, res, masked);
  }

  /**
   * Compose a new grid of a given region and resolution. The grid may be
   * optionally masked to include only high resolution content.
   *
   * @param projectedBounds a bounding box projected to merc-320 at the requested
   *                        resolution of the composed grid
   * @param res             the resolution of composed grid
   * @param masked          whether to mask the composed grid
   */
  public static Grid2D.Float getGrid(Rectangle projectedBounds, int res, boolean masked) {
    init();
    return getGrid(projectedBounds, res, masked, base, mbPath, ProjectionFactory.getMercator(PIXELS_PER_360 * res));
  }

  /**
   * Compose a new grid of a given region and resolution. The grid may be
   * optionally masked to include only high resolution content.
   *
   * @param projectedBounds a bounding box projected to merc-320 at the requested
   *                        resolution of the composed grid
   * @param res             the resolution of composed grid
   * @param masked          whether to mask the composed grid
   * @param base            base path for content
   * @param mbPath          base path for additional multibeam content (usually not used)
   * @param merc            the Mercator projection object
   */
  public static Grid2D.Float getGrid(Rectangle projectedBounds, int res, boolean masked, String base, String mbPath,
      Projection merc) {
    // create our grid2D float container
    Grid2D.Float finalGrid = new Grid2D.Float(projectedBounds, merc);

    // The grid composer follows a "fill-in" logic, whereby only NaN's in the grid buffer can be
    // overwritten by subsequent data. Therefore, we write the highest resolution data first and 
    // "fill-in" around it.

    // All cGrids layers are rendered to at least this resolution
    int cGridsNativeRes = 512;
    //GEBCO is rendered at this resolution
    int GebcoNativeRes = 128;
    //ASTER was rendered to this resolution
    int AsterRes_NASA = 2048;
    //NED was rendered to this resolution
    int NedRes_USGS = 8192;
    //All multibeam data are rendered to at least this resolution
    int mbNativeRes = 512;

    // contributed grids at res, down through all relevant grid content

    int cGridsFinalRes = Math.min(cGridsNativeRes,res);
    for (int res0 = res; res0 >= cGridsFinalRes; res0 /= 2) {
      fillFloatGrid(GetGrid.cGridsDir, res, res0, base, finalGrid);
    }

    // gdem @ res if > GEBCO resolution and at ASTER/NED res if greater than ASTER/NED data
    if (res > GebcoNativeRes)
      fillShortGrid(GetGrid.landDir, res, res, base, finalGrid);
    if (res > NedRes_USGS)
      fillShortGrid(GetGrid.landDir, res, NedRes_USGS, base, finalGrid);
    if (res > AsterRes_NASA)
      fillShortGrid(GetGrid.landDir, res, AsterRes_NASA, base, finalGrid);
      
    // multibeam from res down to 512 or at res if less than 512
    int mbFinalRes = Math.min(mbNativeRes, res);
    for (int res0 = res; res0 >= mbFinalRes; res0 /= 2) {
      fillFloatGrid("", res, res0, mbPath, finalGrid);
      if (!base.equals(mbPath))
        fillFloatGrid("", res, res0, base, finalGrid);
    }

    // fill in remaining NaNs with GEBCO
    if (!masked) {
      int basemapRes = Math.min(res, GebcoNativeRes);
      fillFloatGrid(GetGrid.basemapDir, res, basemapRes, base, finalGrid);
    }

    return finalGrid;
  }

  /**
   * Compose a new grid of a given region and resolution from a single directory.
   * The grid may be optionally masked to include only high resolution content.
   * Change the static base variable to change the directory
   *
   * @param unscaledBounds a bounding box rectangle at any resolution
   * @param boundsRes      the resolution of the bounding box
   * @param res            the resolution of the composed grid
   * @param masked         whether to mask the composed grid (unused)
   */
  public static Grid2D.Float getGridLayer(Rectangle2D.Double unscaledBounds, int boundsRes, int res, boolean masked) {
    Rectangle projectedBounds = getScaledBounds(unscaledBounds, boundsRes, res, 0); // no padding
    return getGridLayer(projectedBounds, res, masked);
  }

  /**
   * Compose a new grid of a given region and resolution from a single directory.
   * Change the static base variable to change the directory
   *
   * @param projectedBounds a bounding box projected to merc-320 at the requested
   *                        resolution of the composed grid
   * @param res             the resolution of composed grid
   * @param masked          whether to mask the composed grid (unused)
   */
  public static Grid2D.Float getGridLayer(Rectangle projectedBounds, int res, boolean masked) {
    init();
    // A record of the current state of the live tiles
    // String[] tilePrefixes_Short = {"gdem/z_", "ocean/z", "grids/z_"};
    // String[] tilePrefixes_Float = {"z_"};
    // String[] tilePrefixes_Boolean = {"mask/m_"};
    // System.out.printf("Res: %d%n",res);
    // create our grid2D float container
    Grid2D.Float finalGrid = new Grid2D.Float(projectedBounds, ProjectionFactory.getMercator(PIXELS_PER_360 * res));
    // fill in remaining NaNs
    fillFloatGrid("", res, Math.min(res, 128), finalGrid);

    return finalGrid;
  }

  /**
   * Get a single point based on latitude/longitude
   *
   * @param lat The requested latitude.
   * @param lon The requested longitude.
   */
  public static double getGridValue(double lat, double lon) {
    init();
    // The grid composer follows a "fill-in" logic,
    // whereby only NaN's in the grid buffer can be
    // overwritten by subsequent data. Therefore, we write
    // the highest resolution data first and "fill-in"
    // around it.

    // Since this is for a single point, we can return as soon as we find data
    int res = 2048;
    double gridVal = Double.NaN;

    // contributed grids at res, down to and including 512

    for (int res0 = res; res0 >= 512; res0 /= 2) {
      gridVal = getFloatGridVal(cGridsDir, res0, lat, lon);
      if (!Double.isNaN(gridVal))
        return gridVal;
    }
    // gdem @ res
    gridVal = getShortGridVal(landDir, res, lat, lon);
    if (!Double.isNaN(gridVal))
      return gridVal;

    // multibeam from scale down to 512 (res is hard coded to 2048 in this code)
    for (int res0 = res; res0 >= 512; res0 /= 2) {
      gridVal = getFloatGridVal("", res0, lat, lon);
      if (!Double.isNaN(gridVal))
        return gridVal;
    }

    return getFloatGridVal(basemapDir, 128, lat, lon);
  }

  private static void printUsage(String[] args, String message) {
    System.err.println("--- error  -- ");
    System.err.println("to compose a grid with bounds \n\twest=20, east=40, south=-20, north=0");
    System.err.println("\tand resolution 4 (must be power of 2 between 1 [~100 m/node] and 512");
    System.err.println("command should be form:");
    System.err.println("\tjava xb.map.GetGrid 20 40 -20 0 4 <masked> <file_prefix>");
    if (args != null && args.length > 0) {
      System.err.println(args.length + " arguments");
      for (int k = 0; k < args.length; k++) {
        System.err.println(k + "\t" + args[k]);
      }
    } else {
      System.err.println("no arguments");
    }
    if (message != null)
      System.err.println(message);
    System.exit(0);
  }

  /**
   * Construct a grid at a particular resolution that encompasses a WESN bounding
   * box.
   *
   * USAGE: java xb.map.GetGrid <w> <e> <s> <n> <res> <masked> <output file name>
   * <masked> should be 1 for a masked grid, 0 for an unmasked grid <res> must be
   * a power of 2
   */
  public static void main(String[] args) {

    // parse command line arguments
    if (args.length != 7) {
      printUsage(args, "wrong arg length: " + args.length);
    }

    double[] wesn = new double[] { Double.parseDouble(args[0]), Double.parseDouble(args[1]),
        Double.parseDouble(args[2]), Double.parseDouble(args[3]) };
    int res = Integer.parseInt(args[4]);
    boolean masked = Integer.parseInt(args[5]) == 1;

    // make sure res is a power of 2
    int kres = 0;
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
      wesn[0] += 360.;
    }
    while (wesn[0] < 0.) {
      wesn[1] += 360.;
      wesn[0] += 360.;
    }
    while (wesn[0] >= 360.) {
      wesn[1] -= 360.;
      wesn[0] -= 360.;
    }

    // create unscaled, projected bounding box
    Mercator merc = ProjectionFactory.getMercator(PIXELS_PER_360);
    double ymin = merc.getY(wesn[3]);
    double ymax = merc.getY(wesn[2]);
    Rectangle2D.Double area = new Rectangle2D.Double(wesn[0] * PIXELS_PER_360 / 360., ymin,
        (wesn[1] - wesn[0]) * PIXELS_PER_360 / 360., ymax - ymin);
    Grid2D.Float grid = null;

    // try to create the grid
    try {
      grid = getGrid(area, 1, res, masked);
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
    String name = args[6] + ".grd";
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
      NetCDFGrid.createStandardGrd(grid, null, (OutputStream) out);
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

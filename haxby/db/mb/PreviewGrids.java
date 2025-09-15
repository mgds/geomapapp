package haxby.db.mb;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;

import javax.swing.JToggleButton;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.grid.Grid2D;
import org.geomapapp.grid.Grid2DOverlay;
import org.geomapapp.grid.GridComposer;
import org.geomapapp.grid.GridDialog;
import org.geomapapp.grid.TileIO;
import org.geomapapp.grid.TiledMask;

import haxby.map.FocusOverlay;
import haxby.map.MMapServer;
import haxby.map.MapApp;
import haxby.map.MapOverlay;
import haxby.map.PoleMapServer;
import haxby.map.XMap;
import haxby.proj.PolarStereo;
import haxby.proj.Projection;
import haxby.proj.ProjectionFactory;
import haxby.util.PathUtil;
import haxby.util.VersionUtil;

public class PreviewGrids
{
  
  private static int MAP_PROJ;
  
  public static void main(String[] inputArgs)
  {

//    if ((inputArgs.length != 0) && (inputArgs.length != 1))
//    {
//      System.err.println("Usage: PreviewMerge [mergePath] [maxRes]");
//      System.exit(-1);
//    }

	HashMap<String, String> paths = new HashMap<>();
    if (inputArgs.length == 1) {
    	paths.put("merc_path", inputArgs[0] + "/merc_320/");
    	paths.put("sp_path", inputArgs[0] + "/SP_320/");
    	paths.put("np_path", inputArgs[0] + "/NP_320/");
    	paths.put("merc_mbPath", inputArgs[0] + "/merc_320/");
    } else if (inputArgs.length == 2) {
    	paths.put("merc_path", inputArgs[0] + "/merc_320/");
    	paths.put("sp_path", inputArgs[0] + "/SP_320/");
    	paths.put("np_path", inputArgs[0] + "/NP_320/");
    	paths.put("merc_mbPath", inputArgs[1] + "/merc_320/");
    }
    else {
    	paths.put("merc_path", PathUtil.getPath("DEV_GMRT2/MERCATOR_GRID_TILE_PATH"));
    	paths.put("sp_path", PathUtil.getPath("DEV_GMRT2/SP_GRID_TILE_PATH"));
    	paths.put("np_path", PathUtil.getPath("DEV_GMRT2/NP_GRID_TILE_PATH"));
    	paths.put("merc_mbPath", PathUtil.getPath("DEV_GMRT2/MERCATOR_GRID_TILE_PATH"));
    }
    if(MapApp.AT_SEA) { 
	    for(String key : paths.keySet()) {
	    	if(new File(paths.get(key)).exists()) {}
	    	else {
	    		paths.put(key, paths.get(key).replaceFirst("current", VersionUtil.getVersion("GMRT")));
	    	}
	    }
    }
    
    int maxRes = 4096;
    MapApp.setup();
    MapApp mapApp = MapApp.createMapApp(new String[0]);
    XMap map = mapApp.getMap();
    
    MAP_PROJ = mapApp.getMapType();
    
    // Production tiles:
    String prodPath;
	if (MAP_PROJ == MapApp.SOUTH_POLAR_MAP)
		prodPath = PathUtil.getPath("GMRT_LATEST/SP_TILE_PATH");
	else if (MAP_PROJ == MapApp.NORTH_POLAR_MAP)
		prodPath = PathUtil.getPath("GMRT_LATEST/NP_TILE_PATH");
	else
		prodPath = PathUtil.getPath("GMRT_LATEST/MERCATOR_TILE_PATH");
	if(MapApp.AT_SEA) {
		prodPath = prodPath.replace("current", VersionUtil.getVersion("GMRT"));
	}
    ImageViewer imageViewerProd = new ImageViewer(map, prodPath, maxRes);
    mapApp.getMapTools().maskB.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent evt)
      {
    	  imageViewerProd.maskImage(((JToggleButton)evt.getSource()).isSelected());
      }
    });
    
    // Development tiles
    String devPath;
    if (MAP_PROJ == MapApp.SOUTH_POLAR_MAP)
    	devPath= paths.get("sp_path");
    else if (MAP_PROJ == MapApp.NORTH_POLAR_MAP)
    	devPath = paths.get("np_path");
    else
    	devPath = paths.get("merc_path");
    ImageViewer imageViewerDev = new ImageViewer(map, devPath, maxRes);
    mapApp.getMapTools().maskB.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent evt)
      {
    	  imageViewerDev.maskImage(((JToggleButton)evt.getSource()).isSelected());
      }
    });
       
    // Development Contributed Grid tiles
    String devCPath = devPath + "/grids/";
    ImageViewer imageViewerCDev = new ImageViewer(map, devCPath, maxRes);
    mapApp.getMapTools().maskB.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent evt)
      {
    	  imageViewerCDev.maskImage(((JToggleButton)evt.getSource()).isSelected());
      }
    });
    
    mapApp.addFocusOverlay(imageViewerProd, "Production GMRT Images");
    mapApp.addFocusOverlay(imageViewerDev, "Development GMRT Images");
    mapApp.addFocusOverlay(imageViewerCDev, "Dev cGrid Images");
   
    String gridName = GridDialog.DEV;
    GridViewer gridViewer = new GridViewer(map, gridName, paths, maxRes);
    gridViewer.setShown(false);
    GridDialog.GRID_LOADERS.put(gridName, gridViewer);
    GridDialog.GRID_UNITS.put(gridName, "m");
    GridDialog gridDialog = mapApp.getMapTools().getGridDialog();
    gridDialog.addGrid(gridViewer);
    gridDialog.setSelectedGrid(gridViewer);
    gridDialog.gridCmds.put("mergeCmd", gridName);
    gridDialog.showDialog();
    gridViewer.getRenderer().oceanB.setSelected(true);
    gridDialog.startGridLoad();
    
    int dx;
    int dy;
    if (mapApp.getMapType() == 0)
    {
      dx = 0;
      dy = 260;
    }
    else
    {
      dx = dy = 320;
    }
    gridViewer.dx = dx;
    gridViewer.dy = dy;
    
  }
  
  public static class GridViewer
    extends Grid2DOverlay
    implements GridDialog.GridLoader
  {
	private HashMap<String, String> paths;
    public int dy;
    public int dx;
    private boolean shown;
    
    public GridViewer(XMap map, String name, HashMap<String, String> paths, int maxRes)
    {
      super(map, name);
      this.paths = paths;
      shown = true;
    }
    
    public void setShown(boolean b) {
    	shown = b;
    }
    
	public void loadGrid(Grid2DOverlay grid) {
      double zoom = map.getZoom();
      int res = 1;
      while (res < zoom) {
        res *= 2;
      }
      if (res < 64) {
      //  return;
      }

	  if (MAP_PROJ == MapApp.SOUTH_POLAR_MAP)
		  GridComposer.getGridSP(map.getClipRect2D(), grid, 512, zoom, true, paths.get("sp_path"));
	  else if (MAP_PROJ == MapApp.NORTH_POLAR_MAP)
		  GridComposer.getGridNP(map.getClipRect2D(), grid, 512, zoom, true, paths.get("np_path"));
	  else
		  GridComposer.getGrid(grid.getMap().getClipRect2D(), grid, 512, zoom, true, paths.get("merc_path"), paths.get("merc_mbPath"));
	}
	
	@Override
	public boolean shouldShow() {
		return shown;
	}
  }
  
  public static class ImageViewer
    extends MapOverlay
    implements FocusOverlay
  {
    private String base;
    private int maxRes;
    public int dx;
    public int dy;
    
    public ImageViewer(XMap map, String path, int maxRes)
    {
      super(map);
      this.maxRes = maxRes;
      this.base = path;
      if (((MapApp)map.getApp()).getMapType() == 0)
      {
        dx = 0;
        dy = 260;
      }
      else
      {
        dx = dy = 320;
      }
    }
    
    public Runnable createFocusTask(final Rectangle2D rect)
    {
      return new Runnable()
      {
        public void run()
        {
          PreviewGrids.ImageViewer.this.focus(rect);
          PreviewGrids.ImageViewer.this.getMap().repaint();
        }
      };
    }
    
    public void focus(Rectangle2D rect)
    {
      getImage(rect, getMap().getZoom());
      getMask(rect, getMap().getZoom());
    }
    
    
    private boolean getImage(Rectangle2D rect, double zoom) {
    	if (MAP_PROJ == MapApp.SOUTH_POLAR_MAP)
    		return PoleMapServer.getImage(rect, this, PoleMapServer.SOUTH_POLE, this.base, false);
    	else if (MAP_PROJ == MapApp.NORTH_POLAR_MAP)
    		return PoleMapServer.getImage(rect, this, PoleMapServer.NORTH_POLE, this.base, false);
    	else
    		return MMapServer.getImage(rect,  this, zoom, this.base, false);   	
    }


    public void getMask(Rectangle2D rect, double zoom)
    {
      int res = 1;
      while (zoom > res) {
        res *= 2;
      }
      int scale = res;
      int x = (int)Math.floor(scale * (rect.getX() - this.dx));
      int y = (int)Math.floor(scale * (rect.getY() - this.dy));
      int width = (int)Math.ceil(scale * (rect.getX() - this.dx + rect.getWidth())) - x;
      int height = (int)Math.ceil(scale * (rect.getY() - this.dy + rect.getHeight())) - y;
      Rectangle r0 = new Rectangle(-this.dx * scale, -this.dy * scale, 640 * scale, this.dy * 2 * scale);
      if ((width <= 0) || (height <= 0)) {
        return;
      }
      Projection proj = null;
      if (MAP_PROJ == 0) {
        proj = ProjectionFactory.getMercator(640 * res);
      } else if (MAP_PROJ == 1) {
        proj = new PolarStereo(new Point(0, 0), 180.0D, 25600.0D / res, -71.0D, 2, 2);
      } else {
        proj = new PolarStereo(new Point(0, 0), 0.0D, 25600.0D / res, 71.0D, 1, 2);
      }
      Rectangle bounds = new Rectangle(x, y, width, height);
      if ((bounds.width <= 0) || (bounds.height <= 0)) {
        return;
      }
      int iRes = res;
      int nLevel;
      for (nLevel = 0; iRes >= 8; nLevel++) {
        iRes /= 8;
      }
      Grid2D.Boolean grid = new Grid2D.Boolean(bounds, (MapProjection)proj);
      TileIO.Boolean tileIO = new TileIO.Boolean((MapProjection)proj, this.base + "mask/m_" + res, 320, nLevel);
      TiledMask tiler = new TiledMask((MapProjection)proj, r0, tileIO, 320, 1, (TiledMask)null);
      if (MAP_PROJ == 0) {
        tiler.setWrap(640 * res);
      }
      grid = (Grid2D.Boolean)tiler.composeGrid(grid);
      BufferedImage image = new BufferedImage(bounds.width, bounds.height, 2);
      for (y = 0; y < bounds.height; y++) {
        for (x = 0; x < bounds.width; x++) {
          image.setRGB(x, y, grid.booleanValue(x + bounds.x, y + bounds.y) ? 0 : Integer.MIN_VALUE);
        }
      }
      Point2D p0 = new Point2D.Double(bounds.getX(), bounds.getY());
      XMap map = getXMap();
      p0 = map.getProjection().getMapXY(grid.getProjection().getRefXY(p0));
      Point2D p1 = new Point2D.Double(bounds.getX() + 1.0D, bounds.getY());
      p1 = map.getProjection().getMapXY(grid.getProjection().getRefXY(p1));
      double gridScale = p1.getX() < p0.getX() ? p1.getX() + map.getWrap() - p0.getX() : p1.getX() - p0.getX();
      setMaskImage(image, p0.getX(), p0.getY(), gridScale);
    }
  }
}

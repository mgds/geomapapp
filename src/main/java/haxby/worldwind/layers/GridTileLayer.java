package haxby.worldwind.layers;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.cache.BasicMemoryCache;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.TextureTile;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.OGLUtil;
import gov.nasa.worldwind.util.TileKey;
import haxby.worldwind.image.ImageResampler;
import haxby.worldwind.layers.ColorScaleLayer.ColorScale;
import haxby.worldwind.layers.SunCompassLayer.SunAngle;
import haxby.worldwind.layers.dynamic_tiler.DynamicImageTileLayer;
import haxby.worldwind.layers.dynamic_tiler.DynamicTiler;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.swing.JFrame;

import org.geomapapp.grid.Grid2DOverlay;
import org.geomapapp.image.GridRenderer;
import org.geomapapp.image.Palette;
import org.geomapapp.image.SimpleRenderingTools;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.awt.AWTTextureData;

public class GridTileLayer extends DynamicImageTileLayer implements PropertyChangeListener,
																	SunAngle,
																	ColorScale,
																	QueryableGridLayer {
	public static final int TILE_SIZE = 512;
	
	private GridTiler tiler;

	private String annotationUnits = "";
	private double annotationFactor = 1;
	
	public GridTileLayer(GridRetriever retriever, ImageResampler ImageResampler) {
		super(makeLevels(retriever.getNumLevels()), new GridTiler(retriever, ImageResampler));
		tiler = (GridTiler) getTiler();
		tiler.renderTools.addChangeListener(this);
		
		if (!WorldWind.getMemoryCacheSet().containsCache(TextureTile.class.getName()))
		{
			long size = Configuration.getLongValue(AVKey.TEXTURE_IMAGE_CACHE_SIZE, 3000000L);
			MemoryCache cache = new BasicMemoryCache((long) (0.85 * size), size);
			cache.setName("Texture Tiles");
			WorldWind.getMemoryCacheSet().addCache(TextureTile.class.getName(), cache);
		}
		
		this.setUseTransparentTextures(true);
		this.setDrawTileBoundaries(true);
	}
	
	protected void doRender(DrawContext dc) {
		super.doRender(dc);

		TextureTile tile = this.getCenterTile();
		if (tile != null)
			tiler.setCurrentTile(tile);
	}
	
	public void dispose() {
		this.tiler.dispose();
		super.dispose();
	}
	
	public void propertyChange(PropertyChangeEvent event) {
		this.tiler.propertyChange(event);
		this.firePropertyChange(AVKey.LAYER, null, this);
	}
	
	public Double getSunAngle() {
		if (tiler.renderTools.getSunTool().isSunOn())
			return tiler.renderTools.getSunTool().getDeclination();
		else
			return null;
	}
	
	public boolean isSunValid() 
	{
		return tiler.renderTools.getSunTool().isSunOn();
	}

	public void setAnnotationFactor(double annotationFactor) {
		this.annotationFactor = annotationFactor;
	}
	
	public void setAnnotationUnits(String annotationUnits) {
		this.annotationUnits = annotationUnits;
	}
	
	public double getAnnotationFactor() {
		return annotationFactor;
	}
	
	public Palette getPalette() {
		return tiler.gridRenderer.getPalette();
	}
	
	public float[] getRange() {
		return tiler.gridRenderer.getPalette().getRange();
	}
	
	public String getTitle() {
		return annotationUnits;
	}
	
	public boolean isColorScaleValid() {
		return tiler.centerGrid != null;
	}
	
	public String getName() {
		if (tiler == null || tiler.retriever == null)
			return "Grid Tile Layer";
		return tiler.retriever.getName();
	}
	
	private static LevelSet makeLevels(int numLevels) {
		AVList params = new AVListImpl();
		
		params.setValue(AVKey.TILE_WIDTH, TILE_SIZE);
		params.setValue(AVKey.TILE_HEIGHT, TILE_SIZE);
		params.setValue(AVKey.DATA_CACHE_NAME, "null");
		params.setValue(AVKey.SERVICE, "null");
		params.setValue(AVKey.DATASET_NAME, "grid");
		params.setValue(AVKey.FORMAT_SUFFIX, "null");
		params.setValue(AVKey.NUM_LEVELS, numLevels);
		params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
		params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(36), Angle.fromDegrees(36)));
		params.setValue(AVKey.SECTOR, Sector.FULL_SPHERE);
		
 	   return new LevelSet(params);
	}
	
	public double getValueAt(double lat, double lon) {
		int currentRes = -1;
		double currentValue = Double.NaN;
		
		tiler.rwl.readLock().lock();
		for (Grid2DOverlay overlay : tiler.grids.values())
		{
			int res = overlay.getResolution();
			if (currentRes > res)
				continue;

			Point2D point = overlay.getGrid().getProjection().getMapXY(lon, lat);
			double value = overlay.getGrid().valueAt(point.getX(), point.getY());
			if (Double.isNaN(value))
				continue;
			
			currentValue = value;
			currentRes = res;
		}
		tiler.rwl.readLock().unlock();
		
		return currentValue * annotationFactor;
	}
	
	public String getValueUnits() {
		return annotationUnits;
	}
	
	public static interface GridRetriever {
		public Grid2DOverlay retriveGrid(Rectangle2D bounds, int level);
		public float getVEFactor();
		public int getNumLevels();
		public String getName();
	}
	
	
	private static class GridTiler implements DynamicTiler { 
		
		private static Set<TileKey> invalid = new HashSet<TileKey>();

		private ImageResampler resampler;
		
		private Map<TileKey, Grid2DOverlay> grids = new HashMap<TileKey, Grid2DOverlay>();
		private Set<TileKey> valid = new HashSet<TileKey>();
		private Set<TileKey> inMemory = new HashSet<TileKey>();
		
		private Grid2DOverlay centerGrid = null;
		
		private SimpleRenderingTools renderTools = new SimpleRenderingTools();
		private GridRenderer gridRenderer = new GridRenderer();
		{
			gridRenderer.setBackground(0);
		}
		
		private GridRetriever retriever;
		
		private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
		private boolean isDisposed = false;
		
		private JFrame frame = new JFrame();
		{
			frame.getContentPane().add(renderTools);
			frame.pack();
			
			renderTools.bothB.setEnabled(true);
			renderTools.applyToRenderer(gridRenderer);
		}
		
		public GridTiler(GridRetriever retriever, ImageResampler resampler) {
			this.retriever = retriever;
			this.resampler = resampler;
			gridRenderer.setVEFactor(retriever.getVEFactor());
			frame.setTitle(retriever.getName());
		}
		
		public void dispose() {
			rwl.writeLock().lock();
			isDisposed = true;
			
			frame.dispose();
			renderTools.setGrid(null);
			if (centerGrid != null)
				centerGrid.dispose();
			centerGrid = null;
			
			for (Grid2DOverlay grid : grids.values()) {
				grid.dispose();
			}
			
			grids.clear();
			
			for (TileKey tileKey : inMemory) {
				TextureTile tile = (TextureTile) 
					WorldWind.getMemoryCache(TextureTile.class.getName())
						.getObject(tileKey);
				
				if (tile != null) {
					WorldWind.getMemoryCache(TextureTile.class.getName())
						.remove(tileKey);
				}
			}
			
			invalid.addAll(valid);
			
			inMemory.clear();
			valid.clear();
			
			rwl.writeLock().unlock();
		}
		
		public void propertyChange(PropertyChangeEvent evt) {
			rwl.writeLock().lock();
			
			renderTools.applyToRenderer(gridRenderer);
			invalid.addAll(valid);
			valid.clear();
			
			rwl.writeLock().unlock();
		}
		
		public synchronized void setCurrentTile(TextureTile tile) {
			
			Grid2DOverlay grid;
			
			try
			{
				rwl.readLock().lock();
				grid = grids.get(tile.getTileKey());
				if (grid == null) {
					TextureTile fallback = tile.getFallbackTile();
					if (fallback == null)
						return;
					
					grid = grids.get(fallback.getTileKey());
					if (grid == null)
						return;
				}
			} finally 
			{
				rwl.readLock().unlock();
			}
			
			if (centerGrid == grid) 
				return;
			
			renderTools.setGrid(grid);
			
			if (centerGrid == null) {
				rwl.writeLock().lock();
				
				renderTools.normalize();
				renderTools.applyToRenderer(gridRenderer);
				invalid.addAll(valid);
				valid.clear();
				
				rwl.writeLock().unlock();
			}
			centerGrid = grid;
			
			if (!frame.isVisible())
				frame.setVisible(true);
		}
		
		public void disposeInvalids(DrawContext dc) {
			if (invalid.size() == 0) return;
			
			rwl.writeLock().lock();
			
			for (TileKey tile : invalid) {
				Texture t = (Texture) dc.getTextureCache().get(tile);
				
				if (t != null)
				{
					t.destroy(dc.getGL().getGL2());
					dc.getTextureCache().remove(tile);
				}
//				
//				TextureTile tt = 
//					(TextureTile) 
//					WorldWind.getMemoryCache(TextureTile.class.getName()).getObject(tile);
			}
			invalid.clear();
			
			rwl.writeLock().unlock();
		}
		
		public boolean holdsTexture(DrawContext dc, TextureTile tile) {
			if (invalid.contains(tile.getTileKey()))
				return false;
			
			if (!valid.contains(tile.getTileKey()))
				return false;
			
			TextureTile tt = requestTextureTile(tile);
			
			if (tt == null)
				return false;
			
			if (tt.getTextureData() == null &&
					!tt.isTextureInMemory(dc.getTextureCache()))
				return false;

			return true;
		}
		
		public TextureTile requestTextureTile(TextureTile tile) {
			 return (TextureTile) 
				WorldWind.getMemoryCache(TextureTile.class.getName()).getObject(tile.getTileKey());
		}

		public TextureTile retriveTextureTile(DrawContext dc, TextureTile tile) {
			// Create the tile bounds from the tile sector
			Sector s = tile.getSector();
			Rectangle2D tileBounds = new Rectangle2D.Double(s.getMinLongitude().degrees, s.getMinLatitude().degrees,
					s.getDeltaLonDegrees(), s.getDeltaLatDegrees());

			rwl.readLock().lock();

			// Try to get a grid from our cache
			Grid2DOverlay grid = grids.get(tile.getTileKey());

			// No grid in cache
			if (grid == null) {
				// Use our GridRetriver to retrive a grid
				grid = retriever.retriveGrid(tileBounds, tile.getLevelNumber());

				// Grid load failed
				if (grid == null) {
					System.err.println("Grid load failed\t" + tile);
					rwl.readLock().unlock();
					return null;
				}

				rwl.readLock().unlock();
				rwl.writeLock().lock();

				// Add the loaded grid to our cache
				grids.put(tile.getTileKey(), grid);
				
				rwl.readLock().lock();
				rwl.writeLock().unlock();
			}
			
			//  Check that we havent disposed while waiting
			if ( isDisposed ) {
				grids.clear();
				rwl.readLock().unlock();
				return null;
			}

			// Render our grid
			BufferedImage img = renderGrid(grid, tileBounds);

			// Add tile to Set of valid tiles
			valid.add(tile.getTileKey());
			
			// Draw our Tile Border and Tile Level on the Image
			/*
			Graphics2D g = img.createGraphics();
			g.setColor(Color.red);
			g.scale(2, 2);
			g.drawString(""+tile.getLevelNumber(), 10, 10);
			
			g.scale(.5, .5);
			g.drawRect(0, 0, TILE_SIZE-1, TILE_SIZE-1);
			*/

			// Try to get this tile from memory
			TextureTile tileB = (TextureTile) 
				WorldWind.getMemoryCache(TextureTile.class.getName())
					.getObject(tile.getTileKey());

			if (tileB != null)
				// A tile has been found, use that
				tile = tileB;
			else {
				// No tile could be found, use this one
				WorldWind.getMemoryCache(TextureTile.class.getName())
					.add(tile.getTileKey(), tile);
				inMemory.add(tile.getTileKey());
			}

			rwl.readLock().unlock();
			// Create a new TextureData from the rendered image of the grid
			//TextureData td = new TextureData(GL.GL_RGBA, GL.GL_RGBA, false, img);
			 //TextureData td = OGLUtil.newTextureData(GL.GL_RGBA, GL.GL_RGBA, false, img);
			GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
			TextureData td = new AWTTextureData(gl.getGLProfile(), GL.GL_RGBA, GL.GL_RGBA, false, img);

			// If our tile holds an old texture dispose it
//			if (tile.getTextureData() != null)
//				tile.setTextureData(null);

			// Set the new tile texture
			tile.setTextureData(td);

			return tile;
		}
		
		private BufferedImage renderGrid(Grid2DOverlay grid, Rectangle2D tileBounds) {
			BufferedImage sample;
			if (renderTools.bothB.isSelected())
				sample = gridRenderer.gridImage(grid.getGrid()).image;
			else
				sample = gridRenderer.gridImage(grid.getGrid(), grid.getLandMask()).image;

			Rectangle rect = grid.getGrid().getBounds();
			double sampleMaxLat = grid.getGrid().getProjection().getRefXY(rect.getMinX(), rect.getMinY()).getY();
			double sampleMinLat = grid.getGrid().getProjection().getRefXY(rect.getMinX(), rect.getMaxY()).getY();

			sample = resampler.resampleImage(sample, sampleMinLat, sampleMaxLat, tileBounds.getY(), tileBounds.getMaxY(), TILE_SIZE);
			BufferedImage img = resizeImage(sample, tileBounds.getY(), tileBounds.getMaxY(), tileBounds.getY(), tileBounds.getMaxY());

			return img;
		}
		
		/**
		 *	Takes an Buffered Image and resizes it to TILE_SIZE x TILE_SIZE 
		 */
		private static BufferedImage resizeImage(BufferedImage sample, double sampleMinLat, double sampleMaxLat, double tileMinLat, double tileMaxLat) {
			BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, sample.getType());
			double tileScale = TILE_SIZE / (tileMinLat - tileMaxLat);

			// s for source; d for destination; all measurements in pixels
			int sx1 = 0;
			int sy1 = 0;
			int sx2 = sample.getWidth();
			int sy2 = sample.getHeight();

			int dx1 = 0;
			int dy1 = (int) ((sampleMaxLat - tileMaxLat) * tileScale);
			int dx2 = TILE_SIZE;
			int dy2 = (int) ((sampleMinLat - tileMaxLat) * tileScale);

			img.createGraphics().drawImage(sample, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);

			return img;
		}
	}


	@Override
	protected void requestTexture(DrawContext dc, TextureTile tile) {
		        Vec4 centroid = tile.getCentroidPoint(dc.getGlobe());
		        if (this.getReferencePoint() != null)
		            tile.setPriority(centroid.distanceTo3(this.getReferencePoint()));

		        RequestTask task = new RequestTask(dc, tile, this);
		        this.getRequestQ().add(task);
		
	}

	@Override
	protected void forceTextureLoad(DrawContext dc, TextureTile tile) {
	    	TextureTile tileB = tiler.retriveTextureTile(dc, tile);
	    	
	    	if (tileB == null) {
	    		this.levels.markResourceAbsent(tile);
	    		return;
	    	}
	}
}

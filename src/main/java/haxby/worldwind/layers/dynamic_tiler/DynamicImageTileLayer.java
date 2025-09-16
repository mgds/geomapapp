/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package haxby.worldwind.layers.dynamic_tiler;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Box;
import gov.nasa.worldwind.geom.Cylinder;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.layers.TextureTile;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.PerformanceStatistic;
import gov.nasa.worldwind.util.Tile;
//import haxby.worldwind.layers.ScalingTiledImageLayer.ScalingTextureTile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import com.jogamp.opengl.util.awt.TextRenderer;

/**
 * @author tag
 * @version $Id: TiledImageLayer.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public abstract class DynamicImageTileLayer extends AbstractLayer
{
	protected final DynamicTiler tiler;
	
	// Infrastructure
	protected static final LevelComparer levelComparer = new LevelComparer();
	protected final LevelSet levels;
	protected ArrayList<TextureTile> topLevels;
	protected boolean forceLevelZeroLoads = false;
	protected boolean levelZeroLoaded = false;
	protected boolean retainLevelZeroTiles = false;
	protected String tileCountName;
	protected double splitScale = 0.9; // TODO: Make configurable

	// Diagnostic flags
	protected boolean showImageTileOutlines = false;
	protected boolean drawTileBoundaries = false;
	protected boolean useTransparentTextures = false;
	protected boolean drawTileIDs = false;
	protected boolean drawBoundingVolumes = false;
	protected TextRenderer textRenderer = null;

	// Stuff computed each frame
	protected ArrayList<TextureTile> currentTiles = new ArrayList<TextureTile>();
	protected TextureTile centerTile;
	protected TextureTile currentResourceTile;
	protected Vec4 referencePoint;
	protected LatLon centerPoint;
	protected boolean atMaxResolution = false;
	protected PriorityBlockingQueue<Runnable> requestQ = new PriorityBlockingQueue<Runnable>(200);

	public DynamicImageTileLayer(LevelSet levelSet, DynamicTiler tiler)
	{
		if (levelSet == null)
		{
			String message = Logging.getMessage("nullValue.LevelSetIsNull");
			Logging.logger().severe(message);
			throw new IllegalArgumentException(message);
		}

		if (tiler == null)
		{
			String message = "nullValue.DynamicTilerIsNull";
			Logging.logger().log(java.util.logging.Level.FINE, message);
			throw new IllegalArgumentException(message);
		}

		this.levels = new LevelSet(levelSet); // the caller's levelSet may change internally, so we copy it.
		this.tiler = tiler;

		this.createTopLevelTiles();

		this.setPickEnabled(false); // textures are assumed to be terrain unless specifically indicated otherwise.
		this.tileCountName = this.getName() + " Tiles";
	}

	public DynamicTiler getTiler() {
		return tiler;
	}

	@Override
	public void setName(String name)
	{
		super.setName(name);
		this.tileCountName = this.getName() + " Tiles";
	}

	public boolean isUseTransparentTextures()
	{
		return this.useTransparentTextures;
	}

	public void setUseTransparentTextures(boolean useTransparentTextures)
	{
		this.useTransparentTextures = useTransparentTextures;
	}

	public boolean isForceLevelZeroLoads()
	{
		return this.forceLevelZeroLoads;
	}

	public void setForceLevelZeroLoads(boolean forceLevelZeroLoads)
	{
		this.forceLevelZeroLoads = forceLevelZeroLoads;
	}

	public boolean isRetainLevelZeroTiles()
	{
		return retainLevelZeroTiles;
	}

	public void setRetainLevelZeroTiles(boolean retainLevelZeroTiles)
	{
		this.retainLevelZeroTiles = retainLevelZeroTiles;
	}

	public boolean isDrawTileIDs()
	{
		return drawTileIDs;
	}

	public void setDrawTileIDs(boolean drawTileIDs)
	{
		this.drawTileIDs = drawTileIDs;
	}

	public boolean isDrawTileBoundaries()
	{
		return drawTileBoundaries;
	}

	public void setDrawTileBoundaries(boolean drawTileBoundaries)
	{
		this.drawTileBoundaries = drawTileBoundaries;
	}

	public boolean isShowImageTileOutlines()
	{
		return showImageTileOutlines;
	}

	public void setShowImageTileOutlines(boolean showImageTileOutlines)
	{
		this.showImageTileOutlines = showImageTileOutlines;
	}

	public boolean isDrawBoundingVolumes()
	{
		return drawBoundingVolumes;
	}

	public void setDrawBoundingVolumes(boolean drawBoundingVolumes)
	{
		this.drawBoundingVolumes = drawBoundingVolumes;
	}

	protected LevelSet getLevels()
	{
		return levels;
	}

	protected void setSplitScale(double splitScale)
	{
		this.splitScale = splitScale;
	}

	protected PriorityBlockingQueue<Runnable> getRequestQ()
	{
		return requestQ;
	}

	public boolean isMultiResolution()
	{
		return this.getLevels() != null && this.getLevels().getNumLevels() > 1;
	}

	public boolean isAtMaxResolution()
	{
		return this.atMaxResolution;
	}

	public TextureTile getCenterTile() {
		return centerTile;
	}

	protected void createTopLevelTiles()
	{
		Sector sector = this.levels.getSector();

		Level level = levels.getFirstLevel();
		Angle dLat = level.getTileDelta().getLatitude();
		Angle dLon = level.getTileDelta().getLongitude();
		Angle latOrigin = this.levels.getTileOrigin().getLatitude();
		Angle lonOrigin = this.levels.getTileOrigin().getLongitude();

        // Determine the row and column offset from the common World Wind global tiling origin.
        int firstRow = Tile.computeRow(dLat, sector.getMinLatitude(), latOrigin);
        int firstCol = Tile.computeColumn(dLon, sector.getMinLongitude(), lonOrigin);
        int lastRow = Tile.computeRow(dLat, sector.getMaxLatitude(), latOrigin);
        int lastCol = Tile.computeColumn(dLon, sector.getMaxLongitude(), lonOrigin);

        int nLatTiles = lastRow - firstRow + 1;
        int nLonTiles = lastCol - firstCol + 1;

        this.topLevels = new ArrayList<TextureTile>(nLatTiles * nLonTiles);

        Angle p1 = Tile.computeRowLatitude(firstRow, dLat, latOrigin);
        for (int row = firstRow; row <= lastRow; row++)
        {
            Angle p2;
            p2 = p1.add(dLat);

            Angle t1 = Tile.computeColumnLongitude(firstCol, dLon, lonOrigin);
            for (int col = firstCol; col <= lastCol; col++)
            {
                Angle t2;
                t2 = t1.add(dLon);

                this.topLevels.add(new TextureTile(new Sector(p1, p2, t1, t2), level, row, col));
                t1 = t2;
            }
            p1 = p2;
        }
    }

    protected void loadAllTopLevelTextures(DrawContext dc)
    {
        for (TextureTile tile : this.topLevels)
        {
            if (!tiler.holdsTexture(dc, tile))
                this.forceTextureLoad(dc, tile);
        }

        this.levelZeroLoaded = true;
	}

	// ============== Tile Assembly ======================= //
	// ============== Tile Assembly ======================= //

    protected void assembleTiles(DrawContext dc)
    {
        this.currentTiles.clear();

        for (TextureTile tile : this.topLevels)
        {
            if (this.isTileVisible(dc, tile))
            {
                this.currentResourceTile = null;
                this.addTileOrDescendants(dc, tile);
            }
        }
    }

    protected void addTileOrDescendants(DrawContext dc, TextureTile tile)
    {
        if (this.meetsRenderCriteria(dc, tile))
        {
            this.addTile(dc, tile);
            return;
        }

        // The incoming tile does not meet the rendering criteria, so it must be subdivided and those
        // subdivisions tested against the criteria.

        // All tiles that meet the selection criteria are drawn, but some of those tiles will not have
        // textures associated with them either because their texture isn't loaded yet or because they
        // are finer grain than the layer has textures for. In these cases the tiles use the texture of
        // the closest ancestor that has a texture loaded. This ancestor is called the currentResourceTile.
        // A texture transform is applied during rendering to align the sector's texture coordinates with the
        // appropriate region of the ancestor's texture.

        TextureTile ancestorResource = null;

        try {
        	if (tiler.holdsTexture(dc, tile) || tile.getLevelNumber() == 0) {
        		ancestorResource = this.currentResourceTile;
        		this.currentResourceTile = tile;
        	}
            else if (!tile.getLevel().isEmpty())
            {
//                this.addTile(dc, tile);
//                return;
                
             // Issue a request for the parent before descending to the children.
                if (tile.getLevelNumber() < this.levels.getNumLevels())
                {
                    // Request only tiles with data associated at this level
                    if (!this.levels.isResourceAbsent(tile))
                        this.requestTexture(dc, tile);
                }
        	}
        	
        	if (this.levels.isFinalLevel(tile.getLevelNumber()) && !isTextureInMemory(dc, tile))
        		this.requestTexture(dc, tile);
        	
        	TextureTile[] subTiles = tile.createSubTiles(this.levels.getLevel(tile.getLevelNumber() + 1));
        	for (TextureTile child : subTiles)
        	{
        		if (this.isTileVisible(dc, child))
        			this.addTileOrDescendants(dc, child);
        	}
        } finally {
        	if (ancestorResource != null)
        		this.currentResourceTile = ancestorResource;
        }
    }

    protected void addTile(DrawContext dc, TextureTile tile)
    {
        tile.setFallbackTile(null);

        if (tiler.holdsTexture(dc, tile))
        {
            this.addTileToCurrent(tiler.requestTextureTile(tile));
            return;
        }

        // Level 0 loads may be forced
        if (tile.getLevelNumber() == 0 && this.forceLevelZeroLoads && !isTextureInMemory(dc, tile))
        {
            this.forceTextureLoad(dc, tile);
            if (tiler.holdsTexture(dc, tile))
            {
                this.addTileToCurrent(tiler.requestTextureTile(tile));
                return;
            }
        }

        // Tile's texture isn't available, so request it
        if (tile.getLevelNumber() < this.levels.getNumLevels())
        {
            // Request only tiles with data associated at this level
            if (!this.levels.isResourceAbsent(tile))
                this.requestTexture(dc, tile);
        }

        // Set up to use the currentResource tile's texture
        if (this.currentResourceTile != null) {
        	if (this.currentResourceTile.getLevelNumber() == 0 && this.forceLevelZeroLoads
                    && !tiler.holdsTexture(dc, this.currentResourceTile))
        		this.forceTextureLoad(dc, this.currentResourceTile);
        	
        	// If fallback tile has its texture
        	if (tiler.holdsTexture(dc, this.currentResourceTile)) {
        		tile.setFallbackTile(tiler.requestTextureTile(this.currentResourceTile));
        		this.addTileToCurrent(tile);
        		return;
        	}
        }
        
    	// The Fallback Tile's texture isn't available; Check the children
    	addDescendants(dc, tile);
    }

    protected void addDescendants(DrawContext dc, TextureTile tile) {
    	if (tile.getLevelNumber() < this.levels.getNumLevels() - 1) {
    		TextureTile[] subTiles = tile.createSubTiles(this.levels.getLevel(tile.getLevelNumber() + 1));
        	for (TextureTile child : subTiles)
        	{
        		if (tiler.holdsTexture(dc, child))
        			this.addTileToCurrent(child);
        		else 
        			this.addDescendants(dc, child);
        	}
    	}
    }

    protected void addTileToCurrent(TextureTile tile)
    {
		if (tile.getSector().contains(centerPoint))
			centerTile = tile;
        this.currentTiles.add(tile);
    }

    protected boolean isTileVisible(DrawContext dc, TextureTile tile)
    {
//        if (!(tile.getExtent(dc).intersects(dc.getView().getFrustumInModelCoordinates())
//            && (dc.getVisibleSector() == null || dc.getVisibleSector().intersects(tile.getSector()))))
//            return false;
//
//        Position eyePos = dc.getView().getEyePosition();
//        LatLon centroid = tile.getSector().getCentroid();
//        Angle d = LatLon.sphericalDistance(eyePos.getLatLon(), centroid);
//        if ((!tile.getLevelName().equals("0")) && d.compareTo(tile.getSector().getDeltaLat().multiply(2.5)) == 1)
//            return false;
//
//        return true;
//
        return tile.getExtent(dc).intersects(dc.getView().getFrustumInModelCoordinates()) &&
            (dc.getVisibleSector() == null || dc.getVisibleSector().intersects(tile.getSector()));
    }
//
//    protected boolean meetsRenderCriteria2(DrawContext dc, TextureTile tile)
//    {
//        if (this.levels.isFinalLevel(tile.getLevelNumber()))
//            return true;
//
//        Sector sector = tile.getSector();
//        Vec4[] corners = sector.computeCornerPoints(dc.getGlobe());
//        Vec4 centerPoint = sector.computeCenterPoint(dc.getGlobe());
//
//        View view = dc.getView();
//        double d1 = view.getEyePoint().distanceTo3(corners[0]);
//        double d2 = view.getEyePoint().distanceTo3(corners[1]);
//        double d3 = view.getEyePoint().distanceTo3(corners[2]);
//        double d4 = view.getEyePoint().distanceTo3(corners[3]);
//        double d5 = view.getEyePoint().distanceTo3(centerPoint);
//
//        double minDistance = d1;
//        if (d2 < minDistance)
//            minDistance = d2;
//        if (d3 < minDistance)
//            minDistance = d3;
//        if (d4 < minDistance)
//            minDistance = d4;
//        if (d5 < minDistance)
//            minDistance = d5;
//
//        double r = 0;
//        if (minDistance == d1)
//            r = corners[0].getLength3();
//        if (minDistance == d2)
//            r = corners[1].getLength3();
//        if (minDistance == d3)
//            r = corners[2].getLength3();
//        if (minDistance == d4)
//            r = corners[3].getLength3();
//        if (minDistance == d5)
//            r = centerPoint.getLength3();
//
//        double texelSize = tile.getLevel().getTexelSize(r);
//        double pixelSize = dc.getView().computePixelSizeAtDistance(minDistance);
//
//        return 2 * pixelSize >= texelSize;
//    }

    protected boolean meetsRenderCriteria(DrawContext dc, TextureTile tile)
    {
        return this.levels.isFinalLevel(tile.getLevelNumber()) || 
        	!needToSplit(dc, tile.getSector());
    }

    protected boolean needToSplit(DrawContext dc, Sector sector)
    {
        Vec4[] corners = sector.computeCornerPoints(dc.getGlobe(), dc.getVerticalExaggeration());
        Vec4 centerPoint = sector.computeCenterPoint(dc.getGlobe(), dc.getVerticalExaggeration());

        View view = dc.getView();
        double d1 = view.getEyePoint().distanceTo3(corners[0]);
        double d2 = view.getEyePoint().distanceTo3(corners[1]);
        double d3 = view.getEyePoint().distanceTo3(corners[2]);
        double d4 = view.getEyePoint().distanceTo3(corners[3]);
        double d5 = view.getEyePoint().distanceTo3(centerPoint);

        double minDistance = d1;
        if (d2 < minDistance)
            minDistance = d2;
        if (d3 < minDistance)
            minDistance = d3;
        if (d4 < minDistance)
            minDistance = d4;
        if (d5 < minDistance)
            minDistance = d5;

        double cellSize = (Math.PI * sector.getDeltaLatRadians() * dc.getGlobe().getRadius()) / 20; // TODO

        return !(Math.log10(cellSize) <= (Math.log10(minDistance) - this.splitScale));
    }

    protected boolean atMaxLevel(DrawContext dc)
    {
        Position vpc = dc.getViewportCenterPosition();
        if (dc.getView() == null || this.getLevels() == null || vpc == null)
            return false;

        if (!this.getLevels().getSector().contains(vpc.getLatitude(), vpc.getLongitude()))
            return true;

        Level nextToLast = this.getLevels().getNextToLastLevel();
        if (nextToLast == null)
            return true;

        Sector centerSector = nextToLast.computeSectorForPosition(vpc.getLatitude(), vpc.getLongitude(),
            this.levels.getTileOrigin());
        return this.needToSplit(dc, centerSector);
    }

    // ============== Rendering ======================= //
    // ============== Rendering ======================= //
    // ============== Rendering ======================= //

    public void render(DrawContext dc) {
    	this.atMaxResolution = this.atMaxLevel(dc);
        super.render(dc);
    }

    @Override
    protected void doRender(DrawContext dc)
    {
        if (this.forceLevelZeroLoads && !this.levelZeroLoaded)
            this.loadAllTopLevelTextures(dc);
        if (dc.getSurfaceGeometry() == null || dc.getSurfaceGeometry().size() < 1)
            return; // TODO: throw an illegal state exception?

        dc.getGeographicSurfaceTileRenderer().setShowImageTileOutlines(this.showImageTileOutlines);

        tiler.disposeInvalids(dc);

        draw(dc);
    }

    protected void draw(DrawContext dc)
    {
        this.referencePoint = this.computeReferencePoint(dc);

        Position pos = dc.getView().getEyePosition();
        this.centerPoint = new LatLon(pos.getLatitude(), pos.getLongitude());
        
        this.assembleTiles(dc); // Determine the tiles to draw.

        if (this.currentTiles.size() >= 1)
        {
            TextureTile[] sortedTiles = new TextureTile[this.currentTiles.size()];
            sortedTiles = this.currentTiles.toArray(sortedTiles);
            Arrays.sort(sortedTiles, levelComparer);

            //GL gl = dc.getGL();
            GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

            if (this.isUseTransparentTextures() || this.getOpacity() < 1)
            {
                gl.glPushAttrib(GL.GL_COLOR_BUFFER_BIT | GL2.GL_POLYGON_BIT | GL2.GL_CURRENT_BIT);
                gl.glColor4d(1d, 1d, 1d, this.getOpacity());
                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            }
            else
            {
                gl.glPushAttrib(GL.GL_COLOR_BUFFER_BIT | GL2.GL_POLYGON_BIT);
            }

            gl.glPolygonMode(GL.GL_FRONT, GL2.GL_FILL);
            gl.glEnable(GL.GL_CULL_FACE);
            gl.glCullFace(GL.GL_BACK);

            dc.setPerFrameStatistic(PerformanceStatistic.IMAGE_TILE_COUNT, this.tileCountName,
                this.currentTiles.size());
            dc.getGeographicSurfaceTileRenderer().renderTiles(dc, this.currentTiles);

            gl.glPopAttrib();

            if (this.drawTileIDs)
                this.drawTileIDs(dc, this.currentTiles);

            if (this.drawBoundingVolumes)
                this.drawBoundingVolumes(dc, this.currentTiles);

            this.currentTiles.clear();
        }

        this.sendRequests();
        this.requestQ.clear();
    }

    protected void sendRequests()
    {
        Runnable task = this.requestQ.poll();
        while (task != null)
        {
            if (!WorldWind.getTaskService().isFull())
            {
                WorldWind.getTaskService().addTask(task);
            }
            task = this.requestQ.poll();
        }
    }

    public boolean isLayerInView(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (dc.getView() == null)
        {
            String message = Logging.getMessage("layers.AbstractLayer.NoViewSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        return !(dc.getVisibleSector() != null && !this.levels.getSector().intersects(dc.getVisibleSector()));
    }

    protected Vec4 computeReferencePoint(DrawContext dc)
    {
        if (dc.getViewportCenterPosition() != null)
            return dc.getGlobe().computePointFromPosition(dc.getViewportCenterPosition());

        java.awt.geom.Rectangle2D viewport = dc.getView().getViewport();
        int x = (int) viewport.getWidth() / 2;
        for (int y = (int) (0.5 * viewport.getHeight()); y >= 0; y--)
        {
            Position pos = dc.getView().computePositionFromScreenPoint(x, y);
            if (pos == null)
                continue;

            return dc.getGlobe().computePointFromPosition(pos.getLatitude(), pos.getLongitude(), 0d);
        }

        return null;
    }

    protected Vec4 getReferencePoint()
    {
        return this.referencePoint;
    }

    protected static class LevelComparer implements Comparator<TextureTile>
    {
        public int compare(TextureTile ta, TextureTile tb)
        {
            int la = ta.getFallbackTile() == null ? ta.getLevelNumber() : ta.getFallbackTile().getLevelNumber();
            int lb = tb.getFallbackTile() == null ? tb.getLevelNumber() : tb.getFallbackTile().getLevelNumber();

            return la < lb ? -1 : la == lb ? 0 : 1;
        }
    }

    protected void drawTileIDs(DrawContext dc, ArrayList<TextureTile> tiles)
    {
        java.awt.Rectangle viewport = dc.getView().getViewport();
        if (this.textRenderer == null) {
            this.textRenderer = new TextRenderer(java.awt.Font.decode("Arial-Plain-13"), true, true);
            this.textRenderer.setUseVertexArrays(false);
        }

        dc.getGL().glDisable(GL.GL_DEPTH_TEST);
        dc.getGL().glDisable(GL.GL_BLEND);
        dc.getGL().glDisable(GL.GL_TEXTURE_2D);

        this.textRenderer.setColor(java.awt.Color.YELLOW);
        this.textRenderer.beginRendering(viewport.width, viewport.height);
        for (TextureTile tile : tiles)
        {
            String tileLabel = tile.getLabel();

            if (tile.getFallbackTile() != null)
                tileLabel += "/" + tile.getFallbackTile().getLabel();

            LatLon ll = tile.getSector().getCentroid();
            Vec4 pt = dc.getGlobe().computePointFromPosition(ll.getLatitude(), ll.getLongitude(),
                dc.getGlobe().getElevation(ll.getLatitude(), ll.getLongitude()));
            pt = dc.getView().project(pt);
            this.textRenderer.draw(tileLabel, (int) pt.x, (int) pt.y);
        }
        this.textRenderer.endRendering();
    }

    protected void drawBoundingVolumes(DrawContext dc, ArrayList<TextureTile> tiles)
    {
    	GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
        float[] previousColor = new float[4];
        gl.glGetFloatv(GL2.GL_CURRENT_COLOR, previousColor, 0);
        gl.glColor3d(0, 1, 0);

        for (TextureTile tile : tiles)
        {
            ((Cylinder) tile.getExtent(dc)).render(dc);
        }

        Box c = Sector.computeBoundingBox(dc.getGlobe(), dc.getVerticalExaggeration(), this.levels.getSector());
        gl.glColor3d(1, 1, 0);
        c.render(dc);

        gl.glColor4fv(previousColor, 0);
    }
    
    protected void forceTextureLoad(DrawContext dc, TextureTile tile)
    {
    	TextureTile tileB = tiler.retriveTextureTile(dc, tile);
    	
    	if (tileB == null) {
    		this.levels.markResourceAbsent(tile);
    		return;
    	}
    }

    protected void requestTexture(DrawContext dc, TextureTile tile)
    {
        Vec4 centroid = tile.getCentroidPoint(dc.getGlobe());
        if (this.getReferencePoint() != null)
            tile.setPriority(centroid.distanceTo3(this.getReferencePoint()));

        RequestTask task = new RequestTask(dc, tile, this);
        this.getRequestQ().add(task);
    }
    
    protected boolean isTextureInMemory(DrawContext dc, TextureTile tile)
    {
        return tiler.holdsTexture(dc, tile);
    }
    
    protected static class RequestTask implements Runnable, Comparable<RequestTask>
    {
        protected final DynamicImageTileLayer layer;
        protected final TextureTile tile;
        protected final DrawContext dc;

        public RequestTask(DrawContext dc, TextureTile tile, DynamicImageTileLayer layer)
        {
            this.layer = layer;
            this.tile = tile;
            this.dc = dc;
        }

        public void run()
        {
            // check to ensure load is still needed
//            if (this.layer.isTextureInMemory(this.tile))
//                return;

           this.layer.forceTextureLoad(dc, tile);
           this.layer.firePropertyChange(AVKey.LAYER, null, layer);
        }

        /**
         * @param that the task to compare
         * @return -1 if <code>this</code> less than <code>that</code>, 1 if greater than, 0 if equal
         * @throws IllegalArgumentException if <code>that</code> is null
         */
        public int compareTo(RequestTask that)
        {
            if (that == null)
            {
                String msg = Logging.getMessage("nullValue.RequestTaskIsNull");
                Logging.logger().log(java.util.logging.Level.FINE, msg);
                throw new IllegalArgumentException(msg);
            }
            return this.tile.getPriority() == that.tile.getPriority() ? 0 :
                this.tile.getPriority() < that.tile.getPriority() ? -1 : 1;
        }

        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            final RequestTask that = (RequestTask) o;

            // Don't include layer in comparison so that requests are shared among layers
            return !(tile != null ? !tile.equals(that.tile) : that.tile != null);
        }

        public int hashCode()
        {
            return (tile != null ? tile.hashCode() : 0);
        }

        public String toString()
        {
            return this.tile.toString();
        }
    }
//
//    protected TextureTile getContainingTile(TextureTile tile, Angle latitude, Angle longitude, int levelNumber)
//    {
//        if (!tile.getSector().contains(latitude, longitude))
//            return null;
//
//        if (tile.getLevelNumber() == levelNumber || this.levels.isFinalLevel(tile.getLevelNumber()))
//            return tile;
//
//        TextureTile containingTile;
//        TextureTile[] subTiles = tile.createSubTiles(this.levels.getLevel(tile.getLevelNumber() + 1));
//        for (TextureTile child : subTiles)
//        {
//            containingTile = this.getContainingTile(child, latitude, longitude, levelNumber);
//            if (containingTile != null)
//                return containingTile;
//        }
//
//        return null;
//    }
}
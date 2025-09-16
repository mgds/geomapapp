package haxby.map;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;

import haxby.util.SilentProcessingTask;

/**
 * Specialized runnable for getImage requests during MapApp.mapFocus(). Multiplexes
 * between Mercator and N/S polar projections. 
 * @author bargbb
 */

public class GetImageRequest extends SilentProcessingTask {
	
	/**
	 * GetImageRequest uses data from the image displayed onscreen as a cache when
	 * the new focus area includes the previous view. However, using this image is
	 * only safe when the previous GetImageRequest finished completely. mapImageSafe
	 * keeps track of this state.
	 */
	public static volatile boolean mapImageSafe = false;
	
	private int proj;
	
	/*
	 * The bounding box of the new view, given as decimal coordinates on a 
	 * terminal window 640 pixels wide
	 */
	private Rectangle2D rect;
	/*
	 * The overlay to whose image we are updating.
	 */
	private MapOverlay overlay;
	/*
	 * The zoom level of the current view.
	 */
	private double zoom;

	private int x;
	private int y;
	private int width;
	private int height;
	private int res;
	private int scale;
	private int rectToTileXShift;
	private int rectToTileYShift;

	private BufferedImage workingImage;

	/**
	 * Bounding box of previous view.
	 */
	private Rectangle mapRect;
	/**
	 * Resolution of previous view.
	 */
	private int mapRes;
	
	/**
	 * Create a new GetImageRequest.
	 * @param rect map area on which to focus
	 * @param overlay the current XMap
	 */
	public GetImageRequest(Rectangle2D rect, MapOverlay overlay, String taskID,
						   int proj) {
		// terminate all previous requests and wait for fully termination before 
		// proceeding
		super(true, true, taskID);

		this.rect = rect;
		this.overlay = overlay;
		this.zoom = overlay.getXMap().getZoom();

		this.res = 1;
		while(zoom > res) { // res is greatest power of 2 less than zoom
			res *=2;
		}
		this.scale = res;

		// coordinate transformations between tiling scheme and XMap
		this.proj = proj;

		switch (proj) {
		case MapApp.MERCATOR_MAP:
			this.rectToTileXShift = 0;
			this.rectToTileYShift = 260;
			break;
		case MapApp.NORTH_POLAR_MAP:
		case MapApp.SOUTH_POLAR_MAP:
			this.rectToTileXShift = 320;
			this.rectToTileYShift = 320;
			break;
		default:
			this.rectToTileXShift = this.rectToTileYShift = 0;
		}
		
		this.x = (int)Math.floor(scale*(rect.getX() - (double)rectToTileXShift));
		this.y = (int)Math.floor(scale*(rect.getY() - (double)rectToTileYShift));
		this.width = (int)Math.ceil(scale * 
						(rect.getX() - (double)rectToTileXShift	+ rect.getWidth())
									) - x;
		this.height = (int)Math.ceil(scale *
						(rect.getY() - (double)rectToTileYShift	+ rect.getHeight())
									) - y;
		
		this.workingImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = this.workingImage.createGraphics();
		graphics.setColor(Color.LIGHT_GRAY);
		graphics.fillRect(0,0,width,height);
		this.mapRect = new Rectangle(overlay.getRect());
		this.mapRes = overlay.getResolution();
	}
	
	/**
	 * Performs any cleanup necessary for an early termination. Called by 
	 * SilentProcessingTask.cancel(). In this case, we just want to invalidate the
	 * mapImage stored in the XMap overlay since it has been modified since the last
	 * completed GetImageRequest.
	 */
	protected void earlyCleanup() {
		mapImageSafe = false;
	}

	/**
	 * Execute modified version of getImage with periodic checks for cancellation after
	 * painting individual tiles.
	 */
	public void run() {
		if (isCancelled) return;
		if (width <= 0 || height <= 0) return;

		// --------------------------------------------------------
		// First add in any relevant content from the previous view
		// --------------------------------------------------------
		
		// Sometimes, we've done a small enough pan that the previous overlay
		// image still captures the current view. Avoid updating in this case.
		if (res != mapRes || !mapRect.contains(x, y, width, height)) {
			if (isCancelled) return;
			updateWorkingImageFromImage(overlay.getImage(), mapRes);
		}
			
		// --------------------------------------------------------
		// Now fill in content at any lower resolutions
		// --------------------------------------------------------		
		int baseRes;
		
		switch (proj) {
		case MapApp.MERCATOR_MAP:
			baseRes = 128;
			break;
		case MapApp.NORTH_POLAR_MAP:
			baseRes = PoleMapServer.baseRes[PoleMapServer.NORTH_POLE];
			break;
		case MapApp.SOUTH_POLAR_MAP:
			baseRes = PoleMapServer.baseRes[PoleMapServer.SOUTH_POLE];
			break;
		default:
			baseRes = 64;
		}
				
		// Update workingImage for all resolutions in inclusive range [baseRes, 512]
		if (res > baseRes) {
			LinkedList<Integer> subRes = new LinkedList<Integer>();
			subRes.add(baseRes);
			for (int ires = 512; ires < res; ires *= 2) {
				subRes.add(ires);
			}
			for (Integer ires : subRes) {
				if (isCancelled) return;
				updateWorkingImageFromResolution(ires, true);
			}
		}
		
		// --------------------------------------------------------
		// Now fill in content at any lower resolutions
		// --------------------------------------------------------	
		BufferedImage tile;

		int tileX0 = x/320;
		if (x<0 && tileX0*320!=x) tileX0--;
		int tileY0 = y/320;
		if (y<0 && tileY0*320!=y) tileY0--;
		int tileX, tileY;
		int x0,y0;
		int x1,x2,y1,y2;

		for (tileX = tileX0; tileX*320<x+width; tileX++) {
			x0 = tileX * 320;
			x1 = Math.max(x0, x);
			x2 = Math.min(x0 + 320, x + width);
			for (tileY = tileY0; tileY * 320 < y + height; tileY++) {
				if (isCancelled) return;

				y0 = tileY * 320;
				y1 = Math.max(y0, y);
				y2 = Math.min(y0+320, y + height);
			
				int yShift = proj == MapApp.MERCATOR_MAP ? 260 : 0;
				if (mapRect.contains(x1,
									 y1 + yShift * scale, // Convert back to map XY
									 x2-x1,
									 y2-y1)
					&& mapImageSafe) {
					// this conditional indicates that this tile is covered by
					// the image from the previous view, which we already copied
					// into our image buffer (at the beginning of the workflow)
					// therefore, we just ignore this tile and move on
					continue;
				}
				
				try {
					switch (proj) {
					case MapApp.MERCATOR_MAP:
						tile = MMapServer.getTile(res, tileX, tileY);
						break;
					case MapApp.NORTH_POLAR_MAP:
						tile = PoleMapServer.getTile(res, tileX, tileY, 
													 PoleMapServer.NORTH_POLE);
						break;
					case MapApp.SOUTH_POLAR_MAP:
						tile = PoleMapServer.getTile(res, tileX, tileY, 
													 PoleMapServer.SOUTH_POLE);
						break;
					default:
						tile = null;
					}
					if(tile == null )continue;
				} catch (Exception ex) {
					System.out.println(ex);
					continue;
				}
			
				// fill pixels into working image from tile
				for (int ix=x1; ix<x2; ix++) {
					for (int iy=y1; iy<y2; iy++) {
						int tX = ix-x0;
						int tY = iy-y0;
						if (tX < 0 || tY < 0) continue;
						if (tX >= tile.getWidth() || tY >= tile.getWidth()) continue;
						workingImage.setRGB(ix-x, 
											iy-y, 
											tile.getRGB(ix-x0 + 8, iy-y0 + 8));
					}
				}
				
				// at this point we have `image' updated with the data from a new tile, so we
				// repaint
				int x_XMap = x + rectToTileXShift * scale;
				int y_XMap = y + rectToTileYShift * scale;
				
				if (isCancelled) return;
				overlay.setImage(workingImage, 
								 x_XMap / (double)scale,
								 y_XMap / (double)scale,
								 1. / (double)scale);
				overlay.setRect(x_XMap, y_XMap, width, height);
				overlay.setResolution(res);
				
				if (isCancelled) return;
				overlay.map.repaint();
				// done repainting
			}
		}
		
		int x_XMap = x + rectToTileXShift * scale;
		int y_XMap = y + rectToTileYShift * scale;
				
		if (isCancelled) return;
		overlay.setImage(workingImage, 
						 x_XMap / (double)scale,
						 y_XMap / (double)scale,
						 1. / (double)scale);
		overlay.setRect(x, y, width, height);
		overlay.setResolution(res);
		
		if (isCancelled) return;
		overlay.map.repaint();

		// image and bounds as represented in overlay are now accurate
		finish();
		mapImageSafe = true;
	}
	
	/**
	 * Update the workingImage for this request with new content (either pulled
	 * from a previous view or from the map server). Takes care of coordinate
	 * transformations between XMap and MapServer for each projection.
	 * @param imageRes
	 */
	private void updateWorkingImageFromImage(BufferedImage image, int imageRes) {
		
	    int mapServerToXMapYShift;
		
		switch (proj) {
		case MapApp.MERCATOR_MAP:
			mapServerToXMapYShift = 260 * res;
			break;
		case MapApp.NORTH_POLAR_MAP:
		case MapApp.SOUTH_POLAR_MAP:
		default:
			mapServerToXMapYShift = 0;
		}
		
		if (res == imageRes) {
			Graphics2D g = workingImage.createGraphics();
			g.drawImage(overlay.getImage(), 
						((int)mapRect.getX()) - x,
						((int)mapRect.getY() - mapServerToXMapYShift) - y,
						null);
		} else {
			// otherwise scale the image from the previous view and write it
			int mapScale = mapRes;
			int mapX = (int)Math.floor(mapScale*rect.getX());
			int mapY = (int)Math.floor(mapScale*rect.getY());
			double s = (double)mapRes / res;
			double dx = mapX/s - x;
			double dy = mapY/s - y;

			AffineTransform at = new AffineTransform();
			at.translate(dx, dy);
			at.scale(1./s, 1./s);

			Graphics2D g = workingImage.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
							   RenderingHints.VALUE_INTERPOLATION_BILINEAR );
			g.drawRenderedImage(overlay.getImage(), at);
		}
	}
	
	/**
	 * Upsample a lower resolution to fill in a portion of this requests working image.
	 * @param ires the res we are sampling from
	 * @param repaint whether or not to repaint the display after this update
	 */
	private void updateWorkingImageFromResolution(int ires, boolean repaint) {
		BufferedImage tile;
		
		int resA = ires;
		int scaleA = ires;
		int xA = (int)Math.floor(scaleA*(rect.getX() - (double)rectToTileXShift));
		int yA = (int)Math.floor(scaleA*(rect.getY() - (double)rectToTileYShift));
		int widthA = (int)Math.ceil(scaleA * 
						(rect.getX() - (double)rectToTileXShift	+ rect.getWidth())
									) - xA;
		int heightA = (int)Math.ceil(scaleA *
						(rect.getY() - (double)rectToTileYShift	+ rect.getHeight())
									) - yA;

		BufferedImage imageA = new BufferedImage(widthA, heightA, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = imageA.createGraphics();
		g2.setComposite(AlphaComposite.Clear);
		g2.fillRect(0, 0, width, height);

		int tileX0 = xA/320;
		if (xA<0 && tileX0*320!=xA) tileX0--;
		int tileY0 = yA/320;
		if (yA<0 && tileY0*320!=yA) tileY0--;
		int tileX, tileY;
		int x0,y0;
		int x1,x2,y1,y2;
		
		// For each tile at resA that intersects the new view (given by rect)
		for (tileX = tileX0; tileX*320<xA+widthA; tileX++) {
			x0 = tileX*320;
			x1 = Math.max(x0, xA);
			x2 = Math.min(x0+320, xA+widthA);
			for(tileY = tileY0; tileY*320<yA+heightA; tileY++) {
				if (isCancelled) return;

				y0 = tileY*320;
				y1 = Math.max( y0, yA);
				y2 = Math.min( y0+320, yA+heightA);
				try {
					switch (proj) {
					case MapApp.MERCATOR_MAP:
						tile = MMapServer.getTile(resA, tileX, tileY);
						break;
					case MapApp.NORTH_POLAR_MAP:
						tile = PoleMapServer.getTile(resA, tileX, tileY, 
													 PoleMapServer.NORTH_POLE);
						break;
					case MapApp.SOUTH_POLAR_MAP:
						tile = PoleMapServer.getTile(resA, tileX, tileY, 
													 PoleMapServer.SOUTH_POLE);
						break;
					default:
						tile = null;
					}
					if (tile == null) continue;
				} catch(Exception ex) {
					continue;
				}
				for( int ix=x1; ix<x2 ; ix++) {
					for( int iy=y1 ; iy<y2 ; iy++) {
						imageA.setRGB(ix-xA, iy-yA, 
								(0xff << 24) | tile.getRGB(ix-x0 + 8, iy-y0 + 8));
					}
				}
			
				// by this point, imageA has been update with data from the tile, and
				// the tile definitely does exist
				// so, we try to write the new tile directly onto the image as it exists
				Graphics2D g = workingImage.createGraphics();
				double s = (double)ires / res;
				double dx = xA/s - x;
				double dy = yA/s - y;
				AffineTransform at = new AffineTransform();
				at.translate(dx, dy);
				at.scale(1./s, 1./s);
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
								   RenderingHints.VALUE_INTERPOLATION_BILINEAR );
				g.drawRenderedImage(imageA,  at);

				int x_XMap = x + rectToTileXShift * scale;
				int y_XMap = y + rectToTileYShift * scale;

				if (isCancelled) return;
				if (repaint) {
					overlay.setImage(workingImage, 
									 x_XMap/(double)scale, 
									 y_XMap/(double)scale, 
									 1./(double)scale);
					overlay.setRect(x_XMap, y_XMap, width, height);
					overlay.setResolution(res);
					if (isCancelled) return;
					overlay.map.repaint();
				}
				// done writing and repainting
			}
		}
	}
	
}
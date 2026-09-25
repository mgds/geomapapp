package haxby.map;

import haxby.proj.Projection;
import haxby.proj.ProjectionFactory;
import haxby.util.PathUtil;
import haxby.util.ProcessingDialog;
import haxby.util.SilentProcessingTask;
import haxby.util.URLFactory;
import haxby.util.VersionUtil;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.BindException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import org.json.JSONException;

public class MMapServer extends MapOverlay implements FocusOverlay {

	public MMapServer(XMap map) {
		super(map);
	}

	public Runnable createFocusTask(final Rectangle2D rect) {
		return new Runnable() {
			public void run() {
				focus(rect);
				map.repaint();
			}
		};
	}

	public void focus(Rectangle2D rect) {
		MMapServer.getImage(rect, MMapServer.this);
	}

	public static boolean DRAW_TILE_LABELS = false;
	private static int CACHE_SIZE = 20;
	static Vector tiles = new Vector(CACHE_SIZE);
	static Vector masks = new Vector(CACHE_SIZE);
	static byte[][] coverage=null;

	public static String base = PathUtil.getPath("GMRT_LATEST/MERCATOR_TILE_PATH");
	static {
		if(MapApp.AT_SEA) {
			base = base.replaceFirst("current", VersionUtil.getVersion("GMRT"));
		}
	}
	//public static String[] splitBase = base.split("/");
	public static String GMRT_VERSION_FILE = "gmrt_version/current";

	/*
	 * Read in GMRT version from gmrt_version/current file
	 */
	public static String getVersionGMRT() {
		String versionNum = "Unknown";
		/*try {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					new URL(URLFactory.url(MapApp.getBaseURL()), GMRT_VERSION_FILE).openStream()));
			versionNum = in.readLine();
			in.close();
			//replace $VERSION in path names with current GMRT version number
		
			PathUtil.replacePlaceHolder("$GMRT_VERSION", versionNum);
			if (base.contains("$GMRT_VERSION")) {
				base = PathUtil.getPath("GMRT_LATEST/MERCATOR_TILE_PATH");
				if(MapApp.AT_SEA) {
					base = base.replaceFirst("current", MapApp.which_os.gmrt_current);
				}
			}
		} catch (Exception e) {
			System.err.println("Not able to find GMRT Version");
		}
		*/
		try {
			versionNum = VersionUtil.getVersion("GMRT");
		}
		catch(JSONException e) {
			e.printStackTrace();
		}
		return versionNum;
	}

	public static void loadTileIndicies() {
		try {
			String urlStr = base + "merc_indices.zip";
			URL url = new URL(urlStr);

			ZipInputStream zis = new ZipInputStream(
					new BufferedInputStream(url.openStream()));
			ZipEntry ze = null;
			while ((ze = zis.getNextEntry()) != null) {
				int res = Integer.parseInt(ze.getName());

				Set<Integer> resIndicies = new HashSet<Integer>();
				ObjectInputStream ois = new ObjectInputStream(zis);
				while (true) {
					try {
						int x = ois.readInt();
						int y = ois.readInt();

						x += res;
						y += res;

						int key = (x << 16) | y;
						resIndicies.add(key);
					} catch (EOFException ex) {
						break;
					}
				}
				zis.closeEntry();
			}
			zis.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	static String alt = null;
	public static void setBaseURL( String baseURL ) {
		alt = base;
		base = baseURL;
	}
	public static void setAlternateURL( String url ) {
		alt = url;
	}

	/**
	 * Create a duplicate of a BufferedImage.
	 * @param bi
	 * @return the copied image.
	 */
	private static BufferedImage deepCopy(BufferedImage bi) {
		 ColorModel cm = bi.getColorModel();
		 boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		 WritableRaster raster = bi.copyData(null);
		 return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}
	
	private static boolean getImage(Rectangle2D rect, MapOverlay overlay, double zoom) {
		return getImage(rect, overlay, zoom, base, true);
	}
	
	public static boolean getImage(Rectangle2D rect, MapOverlay overlay, double zoom, String path, boolean bufferTiles) {
		int res = 1;
		while(zoom > res) {
			res *=2;
		}
		int scale = res;
		int x = (int)Math.floor(scale*rect.getX());
		int y = (int)Math.floor(scale*(rect.getY()-260.));
		int width = (int)Math.ceil( scale*(rect.getX()+rect.getWidth()) ) - x;
		int height = (int)Math.ceil( scale*(rect.getY()-260.+rect.getHeight()) ) - y;

		if (width <= 0 || height <=0) return false;

		BufferedImage mapImage = null;
		Rectangle mapRect = new Rectangle();
		if( res == overlay.getResolution() ) {
			// we need to make deep copies of both the previous rectangle and image because
			// we will be modifying overlay throughout this workflow
			mapRect = new Rectangle(overlay.getRect());
			if( mapRect.contains(x, y, width, height) ) return false;
			mapImage = deepCopy(overlay.getImage());
		}

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(Color.gray);
		graphics.fillRect(0,0,width,height);

		BufferedImage tile;
		if( res > 64 ) {
			BufferedImage subImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

			List<Integer> subRes = new LinkedList<Integer>();
			subRes.add(64);
			for (int ires = 512; ires < res; ires *= 2)
				subRes.add(ires);
			for (Integer ires : subRes) {
				int resA = ires;
				int scaleA = ires;
				int xA = (int)Math.floor(scaleA*rect.getX());
				int yA = (int)Math.floor(scaleA*(rect.getY()-260.));
				int widthA = (int)Math.ceil( scaleA*(rect.getX()+rect.getWidth()) ) - xA;
				int heightA = (int)Math.ceil( scaleA*(rect.getY()-260.+rect.getHeight()) ) - yA;
				BufferedImage imageA = new BufferedImage(widthA, heightA, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2 = imageA.createGraphics();
				g2.setComposite(AlphaComposite.Clear);
				g2.fillRect(0, 0, width, height);

				int tileX0 = xA/320;
				if( xA<0 && tileX0*320!=xA ) tileX0--;
				int tileY0 = yA/320;
				if( yA<0 && tileY0*320!=yA ) tileY0--;
				int tileX, tileY;
				int x0,y0;
				int x1,x2,y1,y2;
				for( tileX = tileX0 ; tileX*320<xA+widthA ; tileX++) {
					x0 = tileX*320;
					x1 = Math.max( x0, xA);
					x2 = Math.min( x0+320, xA+widthA);
					for( tileY = tileY0 ; tileY*320<yA+heightA ; tileY++) {
						y0 = tileY*320;
						y1 = Math.max( y0, yA);
						y2 = Math.min( y0+320, yA+heightA);
						try {
							tile = getTile(resA, tileX, tileY, path, bufferTiles);
							if(tile == null )continue;
						} catch( Exception ex ) {
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
						Graphics2D g = image.createGraphics();
						double s = (double)ires / res;
						double dx = xA/s - x;
						double dy = yA/s - y;
						AffineTransform at = new AffineTransform();
						at.translate( dx, dy );
						at.scale( 1./s, 1./s );
						g.setRenderingHint( RenderingHints.KEY_INTERPOLATION,
											RenderingHints.VALUE_INTERPOLATION_BILINEAR);
						g.drawRenderedImage( imageA,  at);
						
						int y_XMap = y + 260*scale;
						overlay.setImage(image, x/(double)scale, y_XMap/(double)scale, 1./(double)scale);
						overlay.setRect(x, y_XMap, width, height);
						overlay.setResolution(res);
						
						overlay.map.repaint();
						// done writing and repainting
					}
				}
/*				Graphics2D g = subImage.createGraphics();
				double s = (double)ires / res;
				double dx = xA/s - x;
				double dy = yA/s - y;
				AffineTransform at = new AffineTransform();
				at.translate( dx, dy );
				at.scale( 1./s, 1./s );
				g.setRenderingHint( RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.drawRenderedImage( imageA,  at);*/
			}
/*			Graphics2D g2 = image.createGraphics();
			g2.drawImage(subImage, 0, 0, null);*/
		}

		int tileX0 = x/320;
		if( x<0 && tileX0*320!=x ) tileX0--;
		int tileY0 = y/320;
		if( y<0 && tileY0*320!=y ) tileY0--;
		int tileX, tileY;
		int x0,y0;
		int x1,x2,y1,y2;

		for( tileX = tileX0 ; tileX*320<x+width ; tileX++) {
			x0 = tileX*320;
			x1 = Math.max( x0, x);
			x2 = Math.min( x0+320, x+width);
			for( tileY = tileY0 ; tileY*320<y+height ; tileY++) {
				y0 = tileY*320;
				y1 = Math.max( y0, y);
				y2 = Math.min( y0+320, y+height);
				if(mapImage != null 
						&& mapRect.contains(x1,
								y1 + 260 * scale, // Convert back to map XY
								x2-x1,
								y2-y1)) {
					for( int ix=x1; ix<x2 ; ix++) {
						for( int iy=y1 ; iy<y2 ; iy++) {
							image.setRGB(ix-x, iy-y, 
								mapImage.getRGB(ix-mapRect.x,
										iy-mapRect.y + 260 * scale)); // Convert back to map XY
						}
					}
					// at this point we have `image' updated with the data from mapImage, so
					// we want to repaint
					int y_XMap = y + 260 * scale;
					overlay.setImage(image, x/(double)scale, y_XMap/(double)scale, 1./(double)scale);
					overlay.setRect(x, y_XMap, width, height);
					overlay.setResolution(res);

					overlay.map.repaint();
					// done repainting
					continue;
				}
				try {
					tile = getTile(res, tileX, tileY, path, bufferTiles);
					if(tile == null )continue;
				} catch( Exception ex ) {
				//	ex.printStackTrace();
					continue;
				}
				for( int ix=x1; ix<x2 ; ix++) {
					for( int iy=y1 ; iy<y2 ; iy++) {
						int tX = ix-x0;
						int tY = iy-y0;
						if (tX <0 || tY <0) continue;
						if (tX>= tile.getWidth() || tY >= tile.getWidth()) continue;
						image.setRGB(ix-x, iy-y, tile.getRGB(ix-x0 + 8, iy-y0 + 8));
					}
				}
				// at this point we have `image' updated with the data from a new tile, so we
				// repaint
				int y_XMap = y + 260 * scale;
				overlay.setImage(image, x/(double)scale, y_XMap/(double)scale, 1./(double)scale);
				overlay.setRect(x, y_XMap, width, height);
				overlay.setResolution(res);

				overlay.map.repaint();
				// done repainting
			}
		}
		y += 260*scale;
		overlay.setImage(image, x/(double)scale, y/(double)scale, 1./(double)scale);
		overlay.setRect(x, y, width, height);
		overlay.setResolution(res);
		return true;
	}

	public static boolean getImage(Rectangle2D rect, MapOverlay overlay) {
		return getImage(rect, overlay, overlay.getXMap().getZoom());
	}

	public static boolean getBaseImage(Rectangle2D rect, MapOverlay overlay) {
		return getImage(rect, overlay, 1);
	}

	
	public static BufferedImage getTile( int res, int x, int y) 
			throws IOException {
		return getTile(res, x, y, base, true);
	}
	
	public static BufferedImage getTile( int res, int x, int y, String path, boolean bufferTiles) 
					throws IOException {
		int MAX_TRIES = 1;
		Tile tile;
		int wrap = res * 2;
		URL url = null;

		while(x<0) x+=wrap;
		while(x>=wrap) x-=wrap;

		// Check that the tile exists...
		if (bufferTiles) {
			if (res > 64) {
				int xx = x;
				int yy = y;
	
				xx += res;
				yy += res;
				int key = (xx << 16) | yy;
	//			if (!set.contains(key)) //TODO what was this `set'?
	//				throw new IOException("No such tile");
			}
	
			for( int i=0 ; i<tiles.size() && !DRAW_TILE_LABELS; i++) {
				tile = (Tile)tiles.get(i);
				if(res==tile.res && x==tile.x && y==tile.y) {
					if(i!=0) {
						synchronized (tiles) {
							tiles.remove(tile);
							tiles.add(0,tile);
						}
					}
					return ImageIO.read(new ByteArrayInputStream(tile.jpeg));
				}
			}
		}
		
		int nGrid = res;
		int nLevel = 0;
		while( nGrid>=8 ) {
			nLevel++;
			nGrid /= 8;
		}
		int factor = 8;
		for( int k=1 ; k<nLevel ; k++) factor *=8;
		String name = "i_"+res;
		for( int k=0 ; k<nLevel ; k++) {
			int xG = factor*(int)Math.floor( (double)x / (double)factor);
			int yG = factor*(int)Math.floor( (double)y / (double)factor);
			name += "/"+ getName( xG, yG );
			factor /= 8;
		}
		name += "/"+ getName( x, y ) +".jpg";
		url = URLFactory.url(path + name );
		//System.out.println(res + " " + x + " " + y + ": gmrt tile: " + url);
		int tries = MAX_TRIES;
		while (true) {
			try {
				//System.out.println("url path: " + url);
				URLConnection con = url.openConnection();
				InputStream in = con.getInputStream();
				tile = new Tile(res, x, y, in, 0);
				break;
			}
			catch (BindException ex) {
				ex.printStackTrace();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
			catch (IOException ex) {
				ex.printStackTrace();
				tries--;
				if (tries <= 0) {
					if (DRAW_TILE_LABELS) {
						BufferedImage i2 = new BufferedImage(336,336, BufferedImage.TYPE_INT_RGB);
						return drawTileName(i2, name);
					} else
						throw ex;
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		}

		if (bufferTiles) {
			synchronized (tiles) {
				if(tiles.size() == 0) {
					tiles.add(tile);
				} else if(tiles.size() == CACHE_SIZE) {
					tiles.remove(CACHE_SIZE - 1);
					tiles.add(0,tile);
				} else {
					tiles.add(0,tile);
				}
			}
		}
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(tile.jpeg));

		if (DRAW_TILE_LABELS)
			return drawTileName(image, name);
		else
			return image;
	}

	private static BufferedImage drawTileName(BufferedImage image, String name) {
		BufferedImage i2 = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = i2.createGraphics();
		g2.drawImage(image, null, 0, 0);
		g2.setColor(Color.red);
		g2.setStroke(new BasicStroke(4));
		g2.drawRect(8, 8, image.getWidth() - 8, image.getHeight() - 8);

		g2.setColor(Color.RED);
		g2.setFont(g2.getFont().deriveFont(Font.BOLD).deriveFont(13f));
		FontMetrics fm = g2.getFontMetrics();
		String[] parts = name.split("/");
		for (int i = 0; i < parts.length; i++) {
			String s = i == parts.length - 1 ? parts[i] : parts[i] + "/";
			int x = 40 + i * 5;
			int y = 40 + i * (fm.getHeight() + 5);
			Rectangle2D rect = fm.getStringBounds(s, g2);
			g2.setColor(Color.white);
			g2.fillRect(x, y - fm.getAscent(), (int)rect.getWidth(), fm.getHeight());
			g2.setColor(Color.red);
			g2.drawString(s, x, y);
		}
		return i2;
	}

	public static String getJPEGPath( int x, int y, int res ) {
		int nGrid = 1024/res;
		int nLevel = 0;
		while( nGrid>8 ) {
			nLevel++;
			nGrid /= 8;
		}
		int factor = 8;
		for( int k=1 ; k<nLevel ; k++) factor *=8;
		String name = "i_"+res;
		for( int k=0 ; k<nLevel ; k++) {
			int xG = factor*(int)Math.floor( (double)x / (double)factor);
			int yG = factor*(int)Math.floor( (double)y / (double)factor);
			name += "/"+ getName( xG, yG );
			factor /= 8;
		}
		name += "/"+ getName( x, y ) +".jpg";
		return name;
	}

	public static String getName(int x0, int y0) {
		return ( (x0>=0) ? "E"+x0 : "W"+(-x0) )
			+ ( (y0>=0) ? "N"+y0 : "S"+(-y0) )
			+ "_320";
	}

	public static void setCacheSize(int newCacheSize) {
		if (newCacheSize < CACHE_SIZE) {
			for (int i = newCacheSize; i < CACHE_SIZE; i++) {
				if (i < tiles.size())
					tiles.removeElementAt(i);
				if (i < masks.size())
					masks.removeElementAt(i);
			}
		}
		CACHE_SIZE = newCacheSize;
	}

	public static void main(String[] args) {
		if (args.length != 7) {
			System.err.println("Usage: MMapServer west east south north width height output_file");
			System.err.println("\tWhere west / east are [0,360]");
			System.exit(-1);
		}

		float[] wesn = new float[4];
		int image_width = 0, image_height = 0;

		try {
			wesn[0] = Float.parseFloat(args[0]);
			wesn[1] = Float.parseFloat(args[1]);
			wesn[2] = Float.parseFloat(args[2]);
			wesn[3] = Float.parseFloat(args[3]);
		} catch (NumberFormatException ex) {
			System.err.println("Invalid WESN Number");
			System.err.println(ex.getMessage());
			System.exit(-1);
		}
		try {
			image_width = Integer.parseInt(args[4]);
			image_height = Integer.parseInt(args[5]);
		} catch (NumberFormatException ex) {
			System.err.println("Invalid width height");
			System.exit(-1);
		}

		if (wesn[0] < 0 || wesn[1] < 0) {
			wesn[0] += 360;
			wesn[1] += 360;
//			System.err.println("Invalid east west");
//			System.exit(-1);
		}

		wesn[2] = Math.max(wesn[2], -79);
		wesn[3] = Math.min(wesn[3], 81);

		if (wesn[1] <= wesn[0]) {
			System.err.println("east cannot be less than west");
			System.exit(-1);
		}

		if (wesn[3] <= wesn[2]) {
			System.err.println("north cannot be less than south");
			System.exit(-1);
		}

		if (image_width <= 0 || image_height <= 0) {
			System.err.println("invalid image size");
			System.exit(-1);
		}

		float dLon = wesn[1] - wesn[0];
		float dLat = wesn[3] - wesn[2];
		float ppD = Math.max(image_width / dLon, image_height / dLat);

		int mapRes = 512;
		int res = mapRes;
		while(640.0 / 360 * (mapRes / res) < ppD && res>1) {
			res /=2;
		}

		System.out.println("Making map at " + res + " resolution");

		int scale = mapRes/res;
		int pixelsPer360 = 1024*320/res;

		Projection proj = ProjectionFactory.getMercator(pixelsPer360);

		double minX = wesn[0];
		double minY = wesn[2];
		double maxX = wesn[1];
		double maxY = wesn[3];

		Point2D minXY = proj.getMapXY(minX, minY);
		Point2D maxXY = proj.getMapXY(maxX, maxY);

		minX = minXY.getX();
		minY = maxXY.getY();
		maxX = maxXY.getX();
		maxY = minXY.getY();

		if (minX == maxX) minX = 0;

		int x = (int)Math.floor(minX);
		int y = (int)Math.floor(minY);
		int width = (int)Math.ceil(maxX) - x;
		int height = (int)Math.ceil(maxY) - y;

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		BufferedImage tile;

		if( res < 32 ) {
			int resA = 32;
			int scaleA = mapRes/32;

			double scale2 = 1d * scaleA / (scale);

			int xA = (int)Math.floor(scale2 * minX);
			int yA = (int)Math.floor(scale2 * minY);
			int widthA = (int)Math.ceil( scale2 * ( maxX )) - xA;
			int heightA = (int)Math.ceil( scale2 * ( maxY )) - yA;

			BufferedImage imageA = new BufferedImage(widthA, heightA, BufferedImage.TYPE_INT_RGB);

			int tileX0 = xA/320;
			if( xA<0 && tileX0*320!=xA ) tileX0--;
			int tileY0 = yA/320;
			if( yA<0 && tileY0*320!=yA ) tileY0--;
			int tileX, tileY;
			int x0,y0;
			int x1,x2,y1,y2;

			for( tileX = tileX0 ; tileX*320<xA+widthA ; tileX++) {
				x0 = tileX*320;
				x1 = Math.max( x0, xA);
				x2 = Math.min( x0+320, xA+widthA);
				for( tileY = tileY0 ; tileY*320<yA+heightA ; tileY++) {
					y0 = tileY*320;
					y1 = Math.max( y0, yA);
					y2 = Math.min( y0+320, yA+heightA);
					try {
						tile = getTile(resA, tileX, tileY);
						if(tile == null )continue;
					} catch( Exception ex ) {
						if (!(ex instanceof FileNotFoundException)) {
							ex.printStackTrace();
						}
						continue;
					}
					for( int ix=x1; ix<x2 ; ix++) {
						for( int iy=y1 ; iy<y2 ; iy++) {
							imageA.setRGB(ix-xA, iy-yA, 
									tile.getRGB(ix-x0+8, iy-y0+8));
						}
					}
				}
			}

			Graphics2D g = image.createGraphics();
			double s = res/32.;
			double dx = xA/s - x;
			double dy = yA/s - y;
			AffineTransform at = new AffineTransform();
			at.translate( dx, dy );
			at.scale( 1./s, 1./s );
			g.setRenderingHint( RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawRenderedImage( imageA,  at);
		}

		int tileX0 = x/320;
		if( x<0 && tileX0*320!=x ) tileX0--;
		int tileY0 = y/320;
		if( y<0 && tileY0*320!=y ) tileY0--;
		int tileX, tileY;
		int x0,y0;
		int x1,x2,y1,y2;

		boolean drawFlag = false;
		for( tileX = tileX0 ; tileX*320<x+width ; tileX++) {
			x0 = tileX*320;
			x1 = Math.max( x0, x);
			x2 = Math.min( x0+320, x+width);
			for( tileY = tileY0 ; tileY*320<y+height ; tileY++) {
				y0 = tileY*320;
				y1 = Math.max( y0, y);
				y2 = Math.min( y0+320, y+height);

				try {
					tile = getTile(res, tileX, tileY);
					if(tile == null )continue;
				} catch( Exception ex ) {
					if (!(ex instanceof FileNotFoundException)) {
						ex.printStackTrace();
					}
					continue;
				}

				drawFlag = true;
				for( int ix=x1; ix<x2 ; ix++) {
					for( int iy=y1 ; iy<y2 ; iy++) {
						image.setRGB(ix-x, iy-y, tile.getRGB(ix-x0+8, iy-y0+8));
					}
				}
			}
		}

		BufferedImage out_image = new BufferedImage(image_width, image_height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = out_image.createGraphics();
		g2d.drawImage(image, 0, 0, image_width, image_height, 0, 0, width, height, null);

		try {
			ImageIO.write(image, "png", new File(args[6]));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

/*
	void initCoverage() {
		coverage = new byte[6][];
		try {
			URL url = URLFactory.url("http://oceana-ridgea.ldeo.columbia.edu/"
				+"SP/coverage");
			DataInputStream in = new DataInputStream(
				new BufferedInputStream( url.openStream() ));
			for(int k=0 ; k<6 ; k++) {
				int n = in.readInt();
				int i=0;
				while( i<n ) {
					int nn = getInt( in );
*/
}

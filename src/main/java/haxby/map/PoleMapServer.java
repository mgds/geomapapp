package haxby.map;

import haxby.proj.PolarStereo;
import haxby.proj.Projection;
import haxby.util.PathUtil;
import haxby.util.SilentProcessingTask;
import haxby.util.URLFactory;
import haxby.util.VersionUtil;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.geomapapp.grid.Grid2D;
import org.geomapapp.grid.TileIO;
import org.geomapapp.grid.TiledMask;

public class PoleMapServer {
	public static boolean DRAW_TILE_LABELS = false;
	public static final int SOUTH_POLE = 0;
	public static final int NORTH_POLE = 1;

	static Vector tiles = new Vector(20);
	static Vector masks = new Vector(20);
	static byte[][] coverage=null;
	static String alt[] = new String[2];
	static ClassLoader loader = null;
	static boolean loaded = true;

	public static String base[] = {
		PathUtil.getPath("GMRT_LATEST/SP_TILE_PATH"),
		PathUtil.getPath("GMRT_LATEST/NP_TILE_PATH"),
	};
	static {
		if(MapApp.AT_SEA) { 
			for(int i = 0; i < base.length; i++) {
				base[i] = base[i].replaceFirst("current", VersionUtil.getVersion("GMRT"));
			}
		}
	}

	static int baseRes[] = {
		64,
		64
	};

	public static void setBaseURL( String baseURL , int whichPole) {
		alt = base;
		base[whichPole] = baseURL;
	}
	public static void setAlternateURL( String altURL , int whichPole ) {
		alt[whichPole] = altURL;
	}
	
	public static boolean getImage(Rectangle2D rect, MapOverlay overlay, int whichPole) {
		return getImage(rect, overlay, whichPole, base[whichPole], true);
	}
	public static boolean getImage(Rectangle2D rect, MapOverlay overlay, int whichPole, String path, boolean bufferTiles) {
		double zoom = overlay.getXMap().getZoom();
		int res = 1;
		while(zoom > res) {
			res *=2;
		}
		int scale = res;
		int x = (int)Math.floor(scale*(rect.getX()-320.));
		int y = (int)Math.floor(scale*(rect.getY()-320.));
		int width = (int)Math.ceil( scale*(rect.getX()-320.+rect.getWidth()) ) - x;
		int height = (int)Math.ceil( scale*(rect.getY()-320.+rect.getHeight()) ) - y;

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		BufferedImage tile;

		if( res > baseRes[whichPole] ) {
			BufferedImage subImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

			List<Integer> subRes = new LinkedList<Integer>();
			subRes.add(baseRes[whichPole]);
			for (int ires = 512; ires < res; ires *= 2) 
				subRes.add(ires);
			for (Integer ires : subRes) {
				int resA = ires;
				int scaleA = ires;
				int xA = (int)Math.floor(scaleA*(rect.getX()-320.));
				int yA = (int)Math.floor(scaleA*(rect.getY()-320.));
				int widthA = (int)Math.ceil( scaleA*(rect.getX()-320.+rect.getWidth()) ) - xA;
				int heightA = (int)Math.ceil( scaleA*(rect.getY()-320.+rect.getHeight()) ) - yA;
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
										(0xff << 24) | tile.getRGB(ix-x0+8, iy-y0+8));
							}
						}
					}
				}
				Graphics2D g = subImage.createGraphics();
				double s = ires / (double) res;
				double dx = xA/s - x;
				double dy = yA/s - y;
				AffineTransform at = new AffineTransform();
				at.translate( dx, dy );
				at.scale( 1./s, 1./s );
				g.setRenderingHint( RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.drawRenderedImage( imageA,  at);
			}

			Graphics2D g2 = image.createGraphics();
			g2.drawImage(subImage, 0, 0, null);
		}

		BufferedImage mapImage = null;
		Rectangle mapRect = new Rectangle();
		if( res == overlay.getResolution() ) {
			mapRect = overlay.getRect();
			if( mapRect.contains(x, y, width, height) ) return false;
			mapImage = overlay.getImage();
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
				if(mapImage != null && mapRect.contains(
						x1 + 320 * scale, // Convert back to map XY
						y1 + 320 * scale, // Convert back to map XY
						x2-x1,
						y2-y1)) {
					for( int ix=x1; ix<x2 ; ix++) {
						for( int iy=y1 ; iy<y2 ; iy++) {
							image.setRGB(ix-x, iy-y, 
								mapImage.getRGB(
										ix-mapRect.x + 320 * scale, // Convert back to map XY 
										iy-mapRect.y + 320 * scale)); // Convert back to map XY 
						}
					}
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
						image.setRGB(ix-x, iy-y, tile.getRGB(ix-x0+8, iy-y0+8));
					}
				}
			}
		}
		x += 320*scale;
		y += 320*scale;
		overlay.setImage(image, x/(double)scale, y/(double)scale, 1./(double)scale);
		overlay.setRect(x, y, width, height);
		overlay.setResolution(res);
		return true;
	}

	
	public static BufferedImage getTile( int res, int x, int y, int whichPole) 
			throws IOException {
		return getTile(res, x, y, base[whichPole], true);
		
	}
	public static BufferedImage getTile( int res, int x, int y, String path, boolean bufferTiles) 
					throws IOException {
		Tile tile;
		URL url = null;

		if (bufferTiles) {
			for( int i=0 ; i<tiles.size() && !DRAW_TILE_LABELS; i++) {
				tile = (Tile)tiles.get(i);
				if((res==tile.res) && (x==tile.x) && (y==tile.y) && res >=8) { //cached up to i4
					if(i!=0) {
						tiles.remove(i);
						tiles.add(0,tile);
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
		
		//System.out.println(url);
		try {
			URLConnection con = url.openConnection();
			InputStream in = con.getInputStream();
			tile = new Tile(res, x, y, in, 0);
		} catch(IOException ex) {
			if( alt==null ) {
				if (DRAW_TILE_LABELS)
					return drawTileName(
							new BufferedImage(336,336, BufferedImage.TYPE_INT_RGB),
							name);
				else
					throw ex;
			}
			try {
				url = URLFactory.url(alt + name );
				URLConnection con = url.openConnection();
				InputStream in = con.getInputStream();
				tile = new Tile(res, x, y, in, 0);
			} catch (IOException ex2) {
				if (DRAW_TILE_LABELS)
					return drawTileName(
							new BufferedImage(336,336, BufferedImage.TYPE_INT_RGB),
							name);
				else 
					throw ex2;
			}
		}
		
		if (bufferTiles) {
			if(tiles.size() == 0) {
				tiles.add(tile);
			} else if(tiles.size() == 20) {
				tiles.remove(19);
				tiles.add(0,tile);
			} else {
				tiles.add(0,tile);
			}
		}
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(tile.jpeg));

		return DRAW_TILE_LABELS ? drawTileName(image, name) : image;
	}
	public static boolean getMaskImage(Rectangle2D rect, MapOverlay overlay, int whichPole) {
		XMap map = overlay.getMap();
		double zoom = map.getZoom();
		int res = 1;
		while(zoom > res) {
			res *=2;
		}
		int scale = res;
		int x = (int)Math.floor(scale*(rect.getX() - 320));
		int y = (int)Math.floor(scale*(rect.getY() - 320));
		int width = (int)Math.ceil( scale*(rect.getX()-320+rect.getWidth()) ) - x;
		int height = (int)Math.ceil( scale*(rect.getY()-320+rect.getHeight()) ) - y;

		Rectangle r0 = new Rectangle( -320*scale, -320*scale, 640*scale,
				320*2*scale);

		if (width <= 0 || height <=0) return false;

		Projection proj = null;
		if (whichPole == 0)
			proj = new PolarStereo( new Point(0, 0),
					180., 25600. / res, -71.,
					PolarStereo.SOUTH, PolarStereo.WGS84);
		else
			proj = new PolarStereo( new Point(0, 0),
					0.,  25600. / res, 71.,
					PolarStereo.NORTH, PolarStereo.WGS84);
		
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
				base[whichPole] + "mask/m_" + res,
				320, nLevel);
		TiledMask tiler = new TiledMask( proj, 
						r0,
						tileIO,
						320,
						1,
						(TiledMask)null);

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
		p0 = map.getProjection().getMapXY( grid.getProjection().getRefXY(p0));
		Point2D p1 = new Point2D.Double(bounds.getX()+1., bounds.getY());
		p1 = map.getProjection().getMapXY( grid.getProjection().getRefXY(p1));
		double gridScale = p1.getX()<p0.getX() ?
			p1.getX()+map.getWrap()-p0.getX() :
			p1.getX() - p0.getX();

		x += 320*scale;
		y += 320*scale;
		overlay.setMaskImage(image, 
				p0.getX(),
				p0.getY(),
				gridScale);
		return true;
	}
	public static String getName(int x0, int y0) {
		return ( (x0>=0) ? "E"+x0 : "W"+(-x0) )
			+ ( (y0>=0) ? "N"+y0 : "S"+(-y0) )
			+ "_320";
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

	// get the class loader
	static void init() {
		if( loader!=null ) return;
		try {
			loader = org.geomapapp.util.Icons.class.getClassLoader();
			loaded = true;
		} catch(Exception ex) {
			loaded = false;
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
package haxby.worldwind.tilers;

import haxby.proj.Mercator;
import haxby.proj.Projection;
import haxby.proj.ProjectionFactory;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;

import org.geomapapp.io.FileUtility;

public class MaskToWorldWindTiler {
	
	private static float maxLat = 81;
	private static float minLat = -79.174f;
	
	private int tileCount = 0;
	private int tileCountDone = 0;
	private int emptyCount = 0;
	private int prevEmptyPercent = -1;
	private int prevPercent = -1;
	
	public static int TILE_SIZE = 512;
	
	private File outputDirectory;
	private int levelZeroDelta;
	private MapImageProcessor imageProcessor;
	private boolean isScalingTileLongitude = true;
	private boolean discardEmptyTiles = true;
	
	private static final String BASE = "/home/geomapapp/apache/htdocs/MapApp/merc_320_1024/mask/";
	
	public MaskToWorldWindTiler(File outputDirectory, int levelZeroDelta, MapImageProcessor imageProcessor) {
		this.imageProcessor = imageProcessor;
		this.outputDirectory = outputDirectory;
		this.levelZeroDelta = levelZeroDelta;
	}
	
	public MaskToWorldWindTiler(File outputDirectory, int levelZeroDelta) {
		this(outputDirectory, levelZeroDelta, new MapImageProcessor() {
			public BufferedImage processImage(BufferedImage image,
					double[] resultWESN, double[] imageWESN) {
				return image;
			}
		});
	}
	
	public void setDiscardEmptyTiles(boolean discardEmptyTiles) {
		this.discardEmptyTiles = discardEmptyTiles;
	}
	
	public void setIsScalingTileLongitude(boolean tf) {
		this.isScalingTileLongitude = tf;
	}
	
	public void setImageProcessor(MapImageProcessor imageProcessor) {
		this.imageProcessor = imageProcessor;
	}
	
	public void tilesForLevels(int numLevels) throws IOException {
		for (int i = 0; i < numLevels; i++)
			createTilesAtLevel(i);
	}
	
	public void createTilesAtLevel(final int level) throws IOException{
		if (!outputDirectory.exists())
			if (!outputDirectory.mkdir())
				throw new IOException("Could not make directory " + outputDirectory);
		
		double zoom = 8 * Math.pow(2, level);
		
		int mapRes = 512;
		int res = mapRes;
		while(zoom*res/mapRes > 1.5 && res>1) {
			res /=2;
		}
		
		tileCount= 0;
		tileCountDone = 0;
		
		File levelDirectory = new File(outputDirectory, level + "");
		if (!levelDirectory.exists())
			if (!levelDirectory.mkdir())
				throw new IOException("Could not make directory " + levelDirectory);
		
		ExecutorService executor  = Executors.newFixedThreadPool(4);
		
		double delta = levelZeroDelta / Math.pow(2, level);
		
		int tileY0 = getTileNumY(minLat, level, levelZeroDelta);
		int tileY1 = getTileNumY(maxLat + delta, level, levelZeroDelta);
		
		HashSet<Integer>[] flaggedTiles = new HashSet[tileY1 - tileY0];
		
		for (int i = 0; i < flaggedTiles.length; i++)
			flaggedTiles[i] = new HashSet<Integer>();
		
		Mercator proj = ProjectionFactory.getMercator( 320*1024/res );

		File[] files = FileUtility.getFiles(new File(BASE + "m_" + res), ".bgrid.gz");
		
		for (File file : files) 
		{
			String name = file.getName();
			
			StringTokenizer st = new StringTokenizer(name, "WESN_.", true);
			int x0 = (st.nextToken().equals("E")) ? 1 : -1;
			x0 *= Integer.parseInt(st.nextToken());
			int y0 = (st.nextToken().equals("N")) ? 1 : -1;
			y0 *= Integer.parseInt(st.nextToken());
			
			x0 *= 320;
			y0 *= 320;
			
			Point2D p0 = proj.getRefXY(x0, y0);
			Point2D p1 = proj.getRefXY(x0 + 320, y0 + 320);
			
			tileY0 = getTileNumY(p0.getY(), level, levelZeroDelta);
			tileY1 = getTileNumY(p1.getY() + delta, level, levelZeroDelta);
			if (tileY0 == tileY1) tileY1++;
			
			for (int tileY = tileY0; tileY < tileY1; tileY++) {
				File tileYDirectory = new File(levelDirectory, tileY+"");
				if (!tileYDirectory.exists())
					if (!tileYDirectory.mkdir())
						throw new IOException("Could not make directory " + tileYDirectory);
				
				final File directory = tileYDirectory;
				final int y = tileY;
				
				double lat0 = getLatFromTileY(tileY, level, levelZeroDelta);
				double lat1 = lat0 + levelZeroDelta / Math.pow(2, level);
				
				int lonMult = getLonMultiplier(lat0, lat1);
				double delta_x = levelZeroDelta / Math.pow(2, level) * lonMult;
				
				int tileX0 = getTileNumX(lat0, lat1, p0.getX(), level, levelZeroDelta);
				int tileX1 = getTileNumX(lat0, lat1, p1.getX() + delta_x, level, levelZeroDelta);
				if (tileX0 == tileX1) tileX1++;
				
				int maxX = (int) (10 * Math.pow(2, level) / lonMult);
				
				for (int tileX = tileX0; tileX < tileX1; tileX++) {
					final int x = tileX >= maxX ? tileX - maxX : tileX;
					
					if (!flaggedTiles[y].add(x))
						continue;
					
					Runnable task = new Runnable() {
						public void run() {
							try {
								createTile(x, y, level, directory);
								synchronized (MaskToWorldWindTiler.this) {
									int x = (int)  ((++tileCountDone)  * 100d / tileCount);
									if (x > prevPercent) {
										prevPercent = x;
										System.out.print("\tPercent done: " + x);
									}
									int y = (int) (emptyCount * 100d / tileCount);
									if (y > prevEmptyPercent) {
										prevEmptyPercent = y;
										System.out.println("\t Empty Percent: " + y);
									}
								}
								
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					};
					tileCount++;
					executor.submit(task);
				}
			}
		}
		
		executor.shutdown();
	}
	
	private void createTile(int tileX, int tileY, int level, File parent) throws IOException{
		double delta = levelZeroDelta / Math.pow(2, level);
		
//		double lon1 = lon0 + delta;
		double lat0 = getLatFromTileY(tileY, level, levelZeroDelta);
		double lat1 = lat0 + delta;
		
		int multiplier = getLonMultiplier(lat0, lat1);
		double lon0 = getLonFromTile(lat0, lat1, tileX, level, levelZeroDelta);
		double lon1 = lon0 + delta * multiplier;
		
		double[] tileWESN = new double[] {lon0,lon1,lat0,lat1};
		
		lat0 = Math.max(lat0, minLat);
		lat1 = Math.min(lat1, maxLat);
		
		if (lat0 == lat1) return;
		
		double[] imageWESN = new double[] {lon0,lon1,lat0,lat1};
		
		Rectangle2D.Double bounds = new Rectangle2D.Double(lon0, 
												lat0, 
												lon1 - lon0, 
												lat1 - lat0);
		
		BufferedImage tileImage = getImageWW(bounds, level, !discardEmptyTiles);
		
		if (tileImage == null) {
			if (!discardEmptyTiles) 
				System.err.print("\nCould not create tile " + tileX + "," + tileY + "," + level + "\n\t" + bounds);
			else
				emptyCount++;
//				System.out.print("\nEmpty Tile at: \t" + tileX + "," + tileY + "," + level);
			return;
		}
		
		tileImage = imageProcessor.processImage(tileImage, tileWESN, imageWESN);
		
		BufferedImage scaledTileImage;
		
		if (tileImage.getHeight() != TILE_SIZE || tileImage.getWidth() != TILE_SIZE) {
			scaledTileImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
			
			Graphics2D g = scaledTileImage.createGraphics();
			g.setComposite( AlphaComposite.Src );
			g.drawImage(tileImage, 0, 0, TILE_SIZE, TILE_SIZE, null);
		} else
			scaledTileImage = tileImage;
		
		StringBuffer sb = new StringBuffer();
		sb.append(tileY).append("_").append(tileX).append(".png");
		
		ImageIO.write(scaledTileImage, "png", new File(parent, sb.toString()));
	}
	
	private BufferedImage getImageWW(Rectangle2D.Double rect, int level, boolean returnRedundant) {
		double zoom = 8 * Math.pow(2, level);
		
		int mapRes = 512;
		int res = mapRes;
		while(zoom*res/mapRes > 1.5 && res>1) {
			res /=2;
		}
		
		int pixelsPer360 = 1024*320/res;
		
		Projection proj =  
			ProjectionFactory.getMercator(pixelsPer360);
		
		double minX = rect.getX();
		double minY = rect.getY();
		double maxX = rect.getMaxX();
		double maxY = rect.getMaxY();
		
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
		
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int xx = 0; xx < width; xx++)
			for (int yy = 0; yy < height; yy++)
				image.setRGB(xx, yy,  0x80000000);
		BufferedImage tile;

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
					ex.printStackTrace();
//					if (!(ex instanceof FileNotFoundException)) {
//						System.err.println(rect + " " + tileX + " " + tileY + " " + res);
//						ex.printStackTrace();
//					}
					continue;
				}
				
				drawFlag = true;
				for( int ix=x1; ix<x2 ; ix++) {
					for( int iy=y1 ; iy<y2 ; iy++) {
						image.setRGB(ix-x, iy-y, tile.getRGB(ix-x0, iy-y0));
					}
				}
			}
		}
		if (!drawFlag && !returnRedundant) return null;
		
		return image;
	}

	private BufferedImage getTile(int res, int x, int y) throws IOException {
		int wrap = 1024/res;
		while(x<0) x+=wrap;
		while(x>=wrap) x-=wrap;
		
		
		int nGrid = 1024/res;
		int nLevel = 0;
		while( nGrid>8 ) {
			nLevel++;
			nGrid /= 8;
		}
		int factor = 8;
		for( int k=1 ; k<nLevel ; k++) factor *=8;
		String name = "m_"+res;
		for( int k=0 ; k<nLevel ; k++) {
			int xG = factor*(int)Math.floor( (double)x / (double)factor);
			int yG = factor*(int)Math.floor( (double)y / (double)factor);
			name += "/"+ getName( xG, yG );
			factor /= 8;
		}
		name += "/"+ getName( x, y ) +".bgrid.gz";

		File f = new File( BASE + name );
		if (!f.exists()) return null;
		
		DataInputStream in = new DataInputStream(
				new BufferedInputStream(
				new GZIPInputStream(
				new FileInputStream( f ))));
		
		int size = 320*320;
		size = (size+7)>>3;
		byte[] buf = new byte[size];
		in.readFully( buf );
		in.close();

		BufferedImage img = new BufferedImage(320, 320, BufferedImage.TYPE_INT_ARGB);
		for (int xx = 0; xx < 320; xx++)
		{
			for (int yy = 0; yy < 320; yy++)
			{
				int i = xx + yy * 320;
				int k = i>>3;
				i -= k<<3;
				byte test = (byte)(1<<i);
				boolean tf = (buf[k]&test)==test;
				
				img.setRGB(xx, yy, tf ?
						0x00FFFFFF : 0x80000000);
			}
		}
		
		return img;
	}

	private String getName(int x0, int y0) {
		return ( (x0>=0) ? "E"+x0 : "W"+(-x0) )
			+ ( (y0>=0) ? "N"+y0 : "S"+(-y0) )
			+ "_320";
	}

	private int getLonMultiplier(double lat0, double lat1) {
		if (!isScalingTileLongitude ) return 1;
		
		int multiplier = 1;
		double maxLat = Math.min(Math.abs(lat0), Math.abs(lat1));
		double radius = Math.cos(Math.toRadians(maxLat));
		while (multiplier * 2 * radius < 1)
			multiplier *= 2;
		return multiplier;
	}
	
	public double getLonFromTile(double lat0, double lat1, int tileX, int level, int levelZeroDelta) {
		double delta = levelZeroDelta / Math.pow(2, level) * getLonMultiplier(lat0, lat1);
		return tileX * delta - 180;
	}
	
	public static double getLatFromTileY(int tileY, int level, int levelZeroDelta) {
		double delta = levelZeroDelta / Math.pow(2, level);
		return tileY * delta - 90;
	}
	
	public int getTileNumX(double lat0, double lat1, double lon, int level, int levelZeroDelta) {
		double delta = levelZeroDelta / Math.pow(2, level) * getLonMultiplier(lat0, lat1);
		return (int) Math.floor((lon + 180) / delta);
	}
	
	public static int getTileNumY(double lat, int level, int levelZeroDelta) {
		double delta = levelZeroDelta / Math.pow(2, level);
		return (int) Math.floor((lat + 90) / delta); 
	}
	
	public static class MercatorResizeAndResample implements MapImageProcessor {
		public BufferedImage processImage(BufferedImage image,
				double[] resultWESN, double[] imageWESN) {
			BufferedImage resampled = resampleImage(image, imageWESN[2], imageWESN[3], resultWESN[2], resultWESN[3]);
			return resizeImage(resampled, resultWESN[2], resultWESN[3], resultWESN[2], resultWESN[3]); 
		}
		
		private static BufferedImage resizeImage(BufferedImage sample, double sampleMinLat, double sampleMaxLat, double tileMinLat, double tileMaxLat) {
			BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
			for (int x = 0; x < TILE_SIZE; x++)
				for (int y = 0; y < TILE_SIZE; y++)
					img.setRGB(x, y, 0x80000000);
			
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
			
			Graphics2D g = img.createGraphics();
			g.setComposite( AlphaComposite.Src );
			g.drawImage(sample, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
			
			return img;
		}
		
		private static BufferedImage resampleImage(BufferedImage sample, double sampleMinLat, double sampleMaxLat, double tileMinLat, double tileMaxLat) {
 			BufferedImage img = new BufferedImage(sample.getWidth(), TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
 			for (int x = 0; x < TILE_SIZE; x++)
				for (int y = 0; y < TILE_SIZE; y++)
					img.setRGB(x, y, 0x80000000);
 			
			double tileScale_DegreesPerPixel = (tileMinLat - tileMaxLat) / TILE_SIZE;

			// Y = 0 is equator
			double sampleMinY = Math.log( Math.tan(Math.toRadians(sampleMinLat)) + 
									1 / Math.cos(Math.toRadians(sampleMinLat)) );
			double sampleMaxY = Math.log( Math.tan(Math.toRadians(sampleMaxLat)) + 
									1 / Math.cos(Math.toRadians(sampleMaxLat)) );
			
			double sampleYDelta = sampleMinY - sampleMaxY;

			int rgb[] = new int[img.getWidth()];
			for (int tileRow = 0; tileRow < TILE_SIZE; tileRow++) {
				double lat = tileScale_DegreesPerPixel * tileRow + tileMaxLat;
				
				if (lat > 81) continue;
				if (lat < -79) continue;
				
				lat = Math.toRadians(lat);
				
				double sampleY = (Math.log(Math.tan(lat) + 1 / Math.cos(lat)));
				
				double sampleRatio = (sampleY - sampleMaxY) / sampleYDelta;
//				System.out.println(Math.toDegrees(lat) + "\t" + sampleY + "\t" + sampleRatio);
				
				if (sampleRatio > 1 || sampleRatio < 0) {
					System.out.println("Outside sample range");
					continue;
				}
				
				int sampleRow = (int) Math.floor(sampleRatio * sample.getHeight());
				rgb = sample.getRGB(0, sampleRow, sample.getWidth(), 1, rgb, 0, 1);
				img.setRGB(0, tileRow, img.getWidth(), 1, rgb, 0, 1);
			}
			
			return img; 
		} 
	}
	
	
	public static void main(String[] args) throws IOException{
		if (args.length < 1) {
			System.err.println("Ussage: GridToWorldWindTiler outputDirectory [TileLevel]");
			System.exit(-1);
		}
		
		MaskToWorldWindTiler tiler = new MaskToWorldWindTiler(new File(args[0]), 36);

		tiler.setImageProcessor(new MercatorResizeAndResample());
//		tiler.setDiscardEmptyTiles(false);
		
		int level;
		if (args.length == 2)
			level = Integer.parseInt(args[1]);
		else
			level = 0;
		
		
		tiler.createTilesAtLevel(level);
	}
	
}

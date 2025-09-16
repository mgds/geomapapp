package haxby.worldwind.tilers;

import haxby.grid.GridImager;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.grid.Grd;
import org.geomapapp.grid.Grid2D;
import org.geomapapp.grid.GridUtilities;
import org.geomapapp.image.GridRenderer;
import org.geomapapp.image.Palette;

public class GridToWorldWindTiler {

	public static int TILE_SIZE = 512;
	private GridRenderer renderer;
	private Grid2D grid;
	private File outputDirectory;
	private int levelZeroDelta;
	private MapImageProcessor imageProcessor;
	private boolean isScalingTileLongitude = true;
	private String outputFormat = "jpg";
	
	public GridToWorldWindTiler(Grid2D grid, File outputDirectory, int levelZeroDelta, MapImageProcessor imageProcessor, GridRenderer renderer) {
		this.imageProcessor = imageProcessor;
		this.grid = grid;
		this.outputDirectory = outputDirectory;
		this.levelZeroDelta = levelZeroDelta;
		this.renderer = renderer;
	}

	public GridToWorldWindTiler(Grid2D grid, File outputDirectory, int levelZeroDelta) {
		this(grid, outputDirectory, levelZeroDelta, new MapImageProcessor() {

			public BufferedImage processImage(BufferedImage image,
					double[] resultWESN, double[] imageWESN) {
				return image;
			}
		}, new GridRenderer());
	}

	public GridToWorldWindTiler(Grid2D grid, File outputDirectory, int levelZeroDelta, GridRenderer renderer) {
		this(grid, outputDirectory, levelZeroDelta, new MapImageProcessor() {
			public BufferedImage processImage(BufferedImage image,
					double[] resultWESN, double[] imageWESN) {
				return image;
			}
		}, renderer);
	}
	
	public GridToWorldWindTiler(Grid2D grid, File outputDirectory, int levelZeroDelta, MapImageProcessor imageProcessor) {
		this(grid, outputDirectory, levelZeroDelta, imageProcessor, new GridRenderer());
	}
	
	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}

	public String getOutputFormat() {
		return outputFormat;
	}

	public void setIsScalingTileLongitude(boolean tf) {
		this.isScalingTileLongitude = tf;
	}

	public GridRenderer getRenderer() {
		return renderer;
	}

	public void setRenderer(GridRenderer renderer) {
		this.renderer = renderer;
	}

	public void setImageProcessor(MapImageProcessor imageProcessor) {
		this.imageProcessor = imageProcessor;
	}

	public void gridToTilesForLevels(int numLevels) throws IOException {
		for (int i = 0; i < numLevels; i++)
			gridToTilesAtLevel(i);
	}

	public void gridToTilesAtLevel(final int level) throws IOException{
		if (!outputDirectory.exists())
			if (!outputDirectory.mkdir())
				throw new IOException("Could not make directory " + outputDirectory);

		File levelDirectory = new File(outputDirectory, level + "");
		if (!levelDirectory.exists())
			if (!levelDirectory.mkdir())
				throw new IOException("Could not make directory " + levelDirectory);

		ExecutorService executor  = Executors.newFixedThreadPool(4);

		double[] wesn = grid.getWESN();

		int tileY0 = getTileNumY(wesn[2], level, levelZeroDelta);
		int tileY1 = getTileNumY(wesn[3], level, levelZeroDelta);

		tileY0 = Math.max(0, tileY0);
		tileY1 = Math.min(getTileNumY(90, level, levelZeroDelta)-1, tileY1);

		for (int tileY = tileY0; tileY <= tileY1; tileY++) {
			File tileYDirectory = new File(levelDirectory, tileY+"");
			if (!tileYDirectory.exists())
				if (!tileYDirectory.mkdir())
					throw new IOException("Could not make directory " + tileYDirectory);

			final File directory = tileYDirectory;
			final int y = tileY;

			double lat0 = getLatFromTileY(tileY, level, levelZeroDelta);
			double lat1 = lat0 + levelZeroDelta / Math.pow(2, level);

			int tileX0 = getTileNumX(lat0, lat1, wesn[0], level, levelZeroDelta);
			int tileX1 = getTileNumX(lat0, lat1, wesn[1], level, levelZeroDelta);

			for (int tileX = tileX0; tileX < tileX1; tileX++) {
				final int x = tileX;

				Runnable task = new Runnable() {
					public void run() {
						try {
							gridToTile(x, y, level, directory);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				};
				executor.submit(task);
			}
		}

		executor.shutdown();
		try {
			executor.awaitTermination(60 * 60 * 24, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void gridToTile(int tileX, int tileY, int level, File parent) throws IOException{
		double delta = levelZeroDelta / Math.pow(2, level);

//		double lon1 = lon0 + delta;
		double lat0 = getLatFromTileY(tileY, level, levelZeroDelta);
		double lat1 = lat0 + delta;

		int multiplier = getLonMultiplier(lat0, lat1);
		double lon0 = getLonFromTile(lat0, lat1, tileX, level, levelZeroDelta);
		double lon1 = lon0 + delta * multiplier;

		double[] tileWESN = new double[] {lon0, lon1, lat0, lat1};

		MapProjection proj = grid.getProjection();
		Point2D ul = proj.getMapXY(lon0, lat1);
		Point2D lr = proj.getMapXY(lon1, lat0);

		Rectangle bounds = new Rectangle();
		bounds.x = (int)Math.floor(ul.getX());
		bounds.y = (int)Math.floor(ul.getY());
		bounds.width = (int) Math.ceil(lr.getX()) - bounds.x;
		bounds.height = (int) Math.ceil(lr.getY()) - bounds.y;

		Grid2D subGrid = GridUtilities.getSubGrid(bounds, grid);
		BufferedImage tileImage = renderer.gridImage(subGrid).image;

//		System.out.println(bounds + "\t" + grid.getBounds());

		tileImage = imageProcessor.processImage(tileImage, tileWESN, tileWESN);

		BufferedImage scaledTileImage = new BufferedImage(TILE_SIZE, TILE_SIZE, tileImage.getType());
		Graphics2D g = scaledTileImage.createGraphics();
		g.setComposite(AlphaComposite.Src);
		g.drawImage(tileImage, 0, 0, TILE_SIZE, TILE_SIZE, null);

		StringBuffer sb = new StringBuffer();
		sb.append(tileY).append("_").append(tileX).append(".").append(outputFormat);

		ImageIO.write(scaledTileImage, outputFormat, new File(parent, sb.toString()));
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

	public double getLatFromTileY(int tileY, int level, int levelZeroDelta) {
		double delta = levelZeroDelta / Math.pow(2, level);
		return tileY * delta - 90;
	}

	public int getTileNumX(double lat0, double lat1, double lon, int level, int levelZeroDelta) {
//		while (lon > 180)
//			lon -= 360;
		double delta = levelZeroDelta / Math.pow(2, level) * getLonMultiplier(lat0, lat1);
		return (int) Math.floor((lon + 180) / delta);
	}

	public static int getTileNumY(double lat, int level, int levelZeroDelta) {
		double delta = levelZeroDelta / Math.pow(2, level);
		return (int) Math.floor((lat + 90) / delta); 
	}

	public static void main(String[] args) throws IOException{
		if (args.length < 2) {
			System.err.println("Ussage: GridToWorldWindTiler gridFile outputDirectory [TileLevel]");
			System.exit(-1);
		}

		Grid2D grid = Grd.readGrd(args[0]);

		GridToWorldWindTiler tiler = new GridToWorldWindTiler(grid, new File(args[1]), 36);

		GridRenderer renderer = tiler.getRenderer();
		Palette p = new Palette(GridImager.defaultRED, GridImager.defaultGREEN, GridImager.defaultBLUE, GridImager.defaultHT);
		p.setGamma(1.5);
//		p.setModel(6);
//		p.setRange(0,200);
		renderer.setPalette(p);

		int level;
		if (args.length == 3)
			level = Integer.parseInt(args[2]);
		else
			level = 0;

		double zoom = 8 * Math.pow(2, level);
		int mapRes = 512; 
		int res = mapRes;

		while(zoom*res/mapRes > 1.5 && res>1) {
			res /=2;
		}

		double ve = 2.5;
		int ires = res;
		while( ires>32 ) {
			ve *= 1.5;
			ires /=2;
		}
//		p.setVE(ve);
		renderer.setVE(ve);
		renderer.setVEFactor(.001);
//		renderer.setVE(12.65625);
		renderer.setUnitsPerNode( res*100. );
		renderer.setBackground( 0xff808080 );

		tiler.gridToTilesAtLevel(level);
	}
}
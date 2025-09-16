package haxby.worldwind.tilers;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;


/**
 * Creates tiles that the haxby.worldwind.layers.GeoMapAppSurfaceLayer class expects
 * 	These are 512 x 512 tiles and go 6 levels deep.  The level 0 lat_lon_delta is 36
 * 	The tiles are created from the WMS server at baseURL
 *
 *	To create tiles for a specified level use
 *		haxby.worldwind.WMSToWorldWindTiler result_directory tile_level
 *
 * 	So to create entire to create the entire tileset the commands
 * 	
 * 		haxby.worldwind.WMSToWorldWindTiler tiles 0
 * 		haxby.worldwind.WMSToWorldWindTiler tiles 1
 * 		haxby.worldwind.WMSToWorldWindTiler tiles 2
 *		haxby.worldwind.WMSToWorldWindTiler tiles 3
 *		haxby.worldwind.WMSToWorldWindTiler tiles 4
 *		haxby.worldwind.WMSToWorldWindTiler tiles 5
 *
 * 	Should be used.
 * 
 * The contents of the tiles/ directory must then be moved to the tileset location that 
 * 	GeoMapAppSurfaceLayer expects. 
 * 
 * The expected location is MapApp.TEMP_BASE_URL + MapApp/ortho_512/ as of 8/1/07
 */
public class WMSToWorldWindTiler {
	
	private static final double LEVEL_ZERO_DELTA = 36; // degrees
	private static final int MAX_TRIES = 5;
	//private static String baseURL = "http://www.marine-geo.org/services/wms?service=WMS&version=1.1.1&request=GetMap&layers=dem_model&SRS=EPSG:4326&format=image/jpeg&width=512&height=512";
	
	private static int tileCount= 0;
	private static int tileCountDone = 0;
	
	
	public static void main(String[] args) throws Exception {
		System.setProperty("java.awt.headless", "true"); 

		if (args.length < 3) {
			System.err.println("\tUsage: haxby.worldwind.WMSToWorldWindTiler wms_request result_directory tile_level");
			System.exit(-1);
		}
		
		BufferedReader in = new BufferedReader(new FileReader(new File(args[0])));
		final String baseURL = in.readLine();
		String[] wesnStr = in.readLine().split(",");
		in.close();
		double[] wesn= new double[4];
		for (int i = 0; i < 4; i++) {
			wesn[i] = Double.parseDouble(wesnStr[i]);
			System.out.println(wesn[i]);
		}
		
		File rootDirectory = new File(args[1]);
		if (!rootDirectory.exists() && !rootDirectory.mkdir()) {
			System.err.println("Could not make root directory " + args[0]);
			System.exit(-1);
		}
		
		int level = Integer.parseInt(args[2]);
		final double delta = LEVEL_ZERO_DELTA / Math.pow(2, level);

		File levelDirectory = new File(rootDirectory, level + "/");
		if (!levelDirectory.exists() && !levelDirectory.mkdir()) {
			System.err.println("Could not make directory " + levelDirectory);
			System.exit(-1);
		}
		
		final boolean isScalingTileLongitude = false;
		
		ExecutorService executor = Executors.newFixedThreadPool(8);
		
		
		for (double y = -90; y < 90; y += delta) {
			if (y > wesn[3]) continue;
			if (y + delta < wesn[2]) continue;
			
			int yCount = (int) Math.round((y + 90) / delta);
			
			final File dir = new File(levelDirectory, + yCount + "/");
			if (!dir.exists() && !dir.mkdir()) {
				System.err.println("Could not make directory " + dir);
				System.exit(-1);
			}
			
			final double lonDelta = delta * getLonMultiplier(y, y + delta, isScalingTileLongitude);
			
			for (double x = -180; x < 180; x += lonDelta) {
				if (x > wesn[1]) continue;
				if (x + delta < wesn[0]) continue;
				
				final double lat = y;
				final double lon = x;
				
				int xCount = (int) Math.round((lon + 180) / lonDelta);

				final File destFile = new File(dir, yCount + "_" + xCount + ".jpg");
				
				//If the file already exists...
				if (destFile.exists())
					continue;
				
				//If the parent file does not exist...
				if (level > 0) {
					int xx = xCount / 2;
					int yy = yCount / 2;
					File parentFile = new File(rootDirectory, (level-1) + "/");
					parentFile = new File(parentFile, yy + "/");
					parentFile = new File(parentFile, yy + "_" + xx + ".jpg");
					if (!parentFile.exists())
						continue;
				}
				
				Runnable task = new Runnable() {
					public void run() {
						
						try {
							BufferedImage img = 
								getImage(baseURL, lat, lon, delta, lonDelta);
							
							// Check that the image isn't mostly black....
							int c = 0;
							for (int xx = 0; xx < 512; xx++)
								for (int yy = 0; yy < 512; yy++) {
									int rgb = img.getRGB(xx, yy);
									if ((rgb & 0xFF) < 10 &&
											((rgb >> 4) & 0xFF) < 10 &&
											((rgb >> 8) & 0xFF) < 10)
										c++;
								}
							
							++tileCountDone;
							
							if (c / (512.0 * 512) > .95) {
								return;
							}
							
							ImageIO.write(img, "jpg", destFile);
							
							System.out.println("Percent done: " + tileCountDone  * 100d / tileCount);
						} catch (IOException ex) {
							System.err.print("Tile Creation Failed at " + lat + ", " + lon );
							ex.printStackTrace();
							System.exit(-1);
						}
					}
				};
				
				tileCount++;
				executor.submit(task);
			}
		}
		
		executor.shutdown();
		executor.awaitTermination(60 * 60 * 60 * 24, TimeUnit.SECONDS);
	}
	
	public static BufferedImage getImage(String baseURL, double minLat, double minLon, double deltaLat, double deltaLon) throws IOException {
		URL url = new URL(baseURL + "&bbox=" + minLon + ","
											+ minLat + "," 
											+ (minLon + deltaLon) + ","
											+ (minLat + deltaLat));
		
		int i = 0;
		while (true) {
			int tries = MAX_TRIES;
			while (true) {
				try {
					return ImageIO.read(url);
				} catch (IOException ex) {
					tries--;
					if (tries == 0) {
						ex.printStackTrace();
						if (i == MAX_TRIES) {
							System.err.println("Could not retrive tile " + minLat + " " + minLon + " " + deltaLat + " from url \n\t" + url);
							throw ex;
						}
						break;
					}
				}
			}
			
			i++;
			// Wait a long time before we try again...
			try {
				System.err.println("Sleeping for half an hour...");
				Thread.sleep(1000 * 60 * 30);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			}
		}
	}
	
	private static int getLonMultiplier(double lat0, double lat1, boolean isScalingTileLongitude) {
		if (!isScalingTileLongitude ) return 1;
		
		int multiplier = 1;
		double maxLat = Math.min(Math.abs(lat0), Math.abs(lat1));
		double radius = Math.cos(Math.toRadians(maxLat));
		while (multiplier * 2 * radius < 1)
			multiplier *= 2;
		return multiplier;
	}
}

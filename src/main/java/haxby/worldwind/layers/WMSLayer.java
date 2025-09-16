package haxby.worldwind.layers;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileUrlBuilder;
import haxby.util.URLFactory;

import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;

public class WMSLayer {

	private static double clamp(double v, double min, double max)
	{
		return v < min ? min : v > max ? max : v;
	}

	public static Layer buildWMSLayer(String name,
			final String url,
			double wesn[],
			String srs,
			int numLevels) 
	{
		if (!"EPSG:4326".equals(srs))
			return null;

		AVList params = new AVListImpl();

		params.setValue(AVKey.TILE_WIDTH, 512);
		params.setValue(AVKey.TILE_HEIGHT, 512);
		params.setValue(AVKey.DATA_CACHE_NAME, "wms/"+name);
		params.setValue(AVKey.SERVICE, "null");
		params.setValue(AVKey.DATASET_NAME, "wms");
		int i = url.indexOf("&format=");
		int j = url.indexOf("&", i+8);
		final String suffix = url.substring(i + 8, j).replace("image/", "");
		params.setValue(AVKey.FORMAT_SUFFIX, "." + suffix);
		params.setValue(AVKey.NUM_LEVELS, 12);
		params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
		params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(36d), Angle.fromDegrees(36d)));

		params.setValue(AVKey.SECTOR, Sector.fromDegrees(
				clamp(wesn[2], -90d, 90d),
				clamp(wesn[3], -90d, 90d),
				clamp(wesn[0], -180d, 180d),
				clamp(wesn[1], -180d, 180d)));


		params.setValue(AVKey.TILE_URL_BUILDER, new TileUrlBuilder() {
			public URL getURL(Tile tile, String imageFormat) throws MalformedURLException {
				Sector s = tile.getSector();
				int width = tile.getLevel().getTileWidth();
				int height = tile.getLevel().getTileHeight();
				double dLat = s.getDeltaLatDegrees();
				double dLon = s.getDeltaLonDegrees();

				double ratioScale = (double) width / height *
				dLat / dLon;

				if (ratioScale < 1)
					width = (int) Math.ceil(width / ratioScale);
				else
					height = (int) Math.ceil(height * ratioScale);

				StringBuffer sb = new StringBuffer(url);
				sb.append("&SRS=EPSG:4326");
				sb.append("&width=");
				sb.append(width);
				sb.append("&height=");
				sb.append(height);

				sb.append("&bbox=");
				sb.append(s.getMinLongitude().getDegrees());
				sb.append(",");
				sb.append(s.getMinLatitude().getDegrees());
				sb.append(",");
				sb.append(s.getMaxLongitude().getDegrees());
				sb.append(",");
				sb.append(s.getMaxLatitude().getDegrees());

//				System.out.println(sb);

				URL url = URLFactory.url(sb.toString().replace(" ", "%20"));
				return url;
			}
		});

		BasicScalingTiledImageLayer layer = new BasicScalingTiledImageLayer(new LevelSet(params)) {
			//@Override
			protected boolean isTileValid(BufferedImage image) {
				//JPEG compression will cause white to be not quite white
				String lowercaseFormat = suffix.toLowerCase();
				
				int blackC = 0;
				int whiteC = 0;
				
				int threshold = lowercaseFormat.contains("jpg")
						|| lowercaseFormat.contains("jpeg") ? 56 : 6;
				for (int x = 0; x < image.getWidth(); x++)
				{
					for (int y = 0; y < image.getHeight(); y++)
					{
						int rgb = image.getRGB(x, y);
						if (isBlack(rgb, threshold))
							blackC++;
						if (isWhite(rgb, 256 - threshold))
							whiteC++;
					}
				}
				System.out.println(blackC * 1. / (image.getWidth() * image.getHeight()));
				
				return blackC * 1. / (image.getWidth() * image.getHeight()) < .97 &&
						whiteC * 1. / (image.getWidth() * image.getHeight()) < .97;
			}
		};
		layer.setUseTransparentTextures(true);
		layer.setName(name);

		layer.setValue(AVKey.URL_CONNECT_TIMEOUT, 30000);
		layer.setValue(AVKey.URL_READ_TIMEOUT, 30000);
		layer.setValue(AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT, 60000);

		return layer;
	}
	
	protected static boolean isBlack(int rgb, int threshold)
	{
		int r = (rgb >> 16) & 0xff;
		int g = (rgb >> 8) & 0xff;
		int b = (rgb >> 0) & 0xff;
		return r + b + g < threshold * 3;
	}

	protected static boolean isWhite(int rgb, int threshold)
	{
		int r = (rgb >> 16) & 0xff;
		int g = (rgb >> 8) & 0xff;
		int b = (rgb >> 0) & 0xff;
		return r + b + g > threshold * 3;
	}
	
	public static Layer buildWMSLayer(haxby.wms.Layer wmslayer, final String url) 
	{
		AVList params = new AVListImpl();

		if (wmslayer == null) {
			wmslayer = new haxby.wms.Layer();
			wmslayer.setName("tmp/"+System.currentTimeMillis());
			wmslayer.setWesn( new double[] {-180,180,-90,90});
		}

		params.setValue(AVKey.TILE_WIDTH, 512);
		params.setValue(AVKey.TILE_HEIGHT, 512);
		params.setValue(AVKey.DATA_CACHE_NAME, "wms/"+wmslayer.getName());
		params.setValue(AVKey.SERVICE, "null");
		params.setValue(AVKey.DATASET_NAME, "wms");
		int i = url.indexOf("&format=");
		int j = url.indexOf("&", i+8);
		params.setValue(AVKey.FORMAT_SUFFIX, "." + url.substring(i + 8, j));
		params.setValue(AVKey.NUM_LEVELS, 12);
		params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
		params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(36d), Angle.fromDegrees(36d)));


		double wesn[] = wmslayer.getWesn();

		params.setValue(AVKey.SECTOR, Sector.fromDegrees(
				clamp(wesn[2], -90d, 90d),
				clamp(wesn[3], -90d, 90d),
				clamp(wesn[0], -180d, 180d),
				clamp(wesn[1], -180d, 180d)));


		params.setValue(AVKey.TILE_URL_BUILDER, new TileUrlBuilder() {
			public URL getURL(Tile tile, String imageFormat) throws MalformedURLException {
				Sector s = tile.getSector();
				int width = tile.getLevel().getTileWidth();
				int height = tile.getLevel().getTileHeight();
				double dLat = s.getDeltaLatDegrees();
				double dLon = s.getDeltaLonDegrees();

				double ratioScale = (double) width / height *
				dLat / dLon;

				if (ratioScale < 1)
					width = (int) Math.ceil(width / ratioScale);
				else
					height = (int) Math.ceil(height * ratioScale);

				StringBuffer sb = new StringBuffer(url);
				sb.append("&SRS=EPSG:4326");
				sb.append("&width=");
				sb.append(width);
				sb.append("&height=");
				sb.append(height);

				sb.append("&bbox=");
				sb.append(s.getMinLongitude().getDegrees());
				sb.append(",");
				sb.append(s.getMinLatitude().getDegrees());
				sb.append(",");
				sb.append(s.getMaxLongitude().getDegrees());
				sb.append(",");
				sb.append(s.getMaxLatitude().getDegrees());

				sb.append("&transparent=TRUE");
				sb.append("&bgcolor=0x000000");

//				System.out.println(width + "\t" + height);

				URL url = URLFactory.url(sb.toString().replace(" ", "%20"));
				return url;
			}
		});

		BasicScalingTiledImageLayer layer = new BasicScalingTiledImageLayer(new LevelSet(params));
		layer.setUseTransparentTextures(true);
		layer.setName(wmslayer.getName());

		layer.setValue(AVKey.URL_CONNECT_TIMEOUT, 30000);
		layer.setValue(AVKey.URL_READ_TIMEOUT, 30000);
		layer.setValue(AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT, 60000);

		return layer;
	}
}

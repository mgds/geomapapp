package haxby.layers.tile512;

import haxby.map.FocusOverlay;
import haxby.map.MapOverlay;
import haxby.map.XMap;
import haxby.proj.Projection;
import haxby.util.ConnectionWrapper;
import haxby.util.LegendSupplier;
import haxby.util.URLFactory;
import haxby.util.WESNSupplier;
import haxby.util.WarningSupplier;
import haxby.util.ProcessingDialog.StartStopTask;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

public class Tile512Overlay extends MapOverlay
		implements FocusOverlay, LegendSupplier, WESNSupplier, WarningSupplier {

	private String legendURL;
	private String warningURL;
	private LayerSetDetails lsd;
	
	private int overlayRes;
	
	public double[] getWESN() {
		return lsd.wesn;
	}
	
	public String getLegendURL() {
		return legendURL;
	}
	
	public void setLegendURL(String legendURL) {
		this.legendURL = legendURL;
	}
	
	public String getWarningURL() {
		return warningURL;
	}
	
	public void setWarningURL(String warningURL) {
		this.warningURL = warningURL;
	}
	
	public Tile512Overlay(LayerSetDetails lsd, XMap map) {
		super(map);
		
		this.lsd = lsd;
		
		double ppdImage = (1 << (lsd.numLevels - 1)) * lsd.tileSize / lsd.levelZeroTileDelta;
		
		int zoom = 1;
		double ppd = 640 / 360.;
		
		while (ppd < ppdImage) {
			zoom *= 2;
			ppd *= 2;
		}
		
		overlayRes = zoom;
	}
	
	public void focus(Rectangle2D rect) {
		getMercImage(rect, 
				Tile512Overlay.this, 
				overlayRes);
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				map.repaint();
			}
		});
	}
	
	public Runnable createFocusTask(final Rectangle2D rect) {
		final ConnectionWrapper wrapper = new ConnectionWrapper();
		
		return new StartStopTask() {
			public void run() {
				getMercImage(rect, 
						Tile512Overlay.this, 
						overlayRes, 
						wrapper);
				
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						map.repaint();
					}
				});
			}
		
			public void stop() {
				synchronized (wrapper)
				{
					if (wrapper.connection != null)
						wrapper.connection.abort();
				}
			}
		};
	}
	
	public String toString() {
		return lsd.name == null ? super.toString() : lsd.name ;
	}
	
	public static boolean getMercImage(Rectangle2D rect, 
			Tile512Overlay overlay,
			int mapRes) {
		return getMercImage(rect, overlay, mapRes, null);
	}
	
	public static boolean getMercImage(Rectangle2D rect, 
			Tile512Overlay overlay,
			int mapRes,
			ConnectionWrapper wrapper) {
		double zoom = overlay.getMap().getZoom();
		int res = mapRes;
		while (zoom * res / mapRes > 1.5 && res > 1) {
			res /= 2;
		}
		int scale = mapRes / res;
		double wrap = overlay.getMap().getWrap();

		if (rect.getWidth() > wrap && wrap != -1)
			rect = new Rectangle2D.Double(0, rect.getY(), wrap, rect
					.getHeight());

		int x = (int) Math.floor(scale * rect.getX());
		int y = (int) Math.floor(scale * (rect.getY() - 260.));
		int width = (int) Math.ceil(scale * (rect.getX() + rect.getWidth()))
				- x;
		int height = (int) Math.ceil(scale
				* (rect.getY() - 260. + rect.getHeight()))
				- y;
		int ortho_width = width;
		int ortho_height = height;

		Projection proj = overlay.getXMap().getProjection();
		Point2D minLonLat = proj.getRefXY(rect.getX(), rect.getMinY());
		Point2D maxLonLat = proj.getRefXY(rect.getMaxX(), rect.getMaxY());
		float minX = (float) minLonLat.getX();
		float minY = (float) minLonLat.getY();
		float maxX = (float) maxLonLat.getX();
		float maxY = (float) maxLonLat.getY();

		if (minY > maxY) {
			float swap = maxY;
			maxY = minY;
			minY = swap;
		}

		while (minX < -180)
			minX += 360;
		while (minX > 180)
			minX -= 360;
		while (maxX < -180)
			maxX += 360;
		while (maxX > 180)
			maxX -= 360;

		double dLat, dLon;

		dLat = maxY - minY;
		if (maxX < minX)
			dLon = 180 - minX + (maxX - -180);
		else if (maxX == minX)
			dLon = 360;
		else
			dLon = maxX - minX;

		double pixelsPerDegreeLon = ortho_width * 1.0 / dLon;
		double pixelsPerDegreeLat = ortho_height* 1.0 / dLat;
		
		BufferedImage orthoImage = new BufferedImage(ortho_width, ortho_height,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = orthoImage.createGraphics();
		
		int tileLevel = 0;
		int tileWidth = overlay.lsd.tileSize;
		double degreesPerTile = overlay.lsd.levelZeroTileDelta;
		while (tileWidth / degreesPerTile < pixelsPerDegreeLon && 
				tileLevel + 1 < overlay.lsd.numLevels)
		{
			tileLevel++;
			degreesPerTile /= 2;
		}
		
		if (maxX < minX) {
			drawTile512(overlay, 
					minX, minY, 180, maxY, 
					pixelsPerDegreeLon, pixelsPerDegreeLat, 
					tileLevel, degreesPerTile, 
					g, wrapper);
			
			g.translate((180 - minX) * pixelsPerDegreeLon, 0);
			minX = -180;
		} else if (minX == maxX) {
			drawTile512(overlay, 
					minX, minY, 180, maxY, 
					pixelsPerDegreeLon, pixelsPerDegreeLat, 
					tileLevel, degreesPerTile, 
					g, wrapper);

			g.translate(180 * pixelsPerDegreeLon, 0);
			minX -= 180;
		}		
		drawTile512(overlay, 
				minX, minY, maxX, maxY, 
				pixelsPerDegreeLon, pixelsPerDegreeLat, 
				tileLevel, degreesPerTile, 
				g, wrapper);
		
		
		BufferedImage image = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_ARGB);
		g = image.createGraphics();

		float deltaLat = maxY - minY;
		float minYR = (float) Math.toRadians(minY);
		float maxYR = (float) Math.toRadians(maxY);

		float mapMinY = (float) Math.log(Math.tan(minYR) + 1 / Math.cos(minYR));
		float mapMaxY = (float) Math.log(Math.tan(maxYR) + 1 / Math.cos(maxYR));
		float mapScale = height / (mapMaxY - mapMinY);
		int windowMinY = (int) Math.ceil(mapMinY * mapScale);
		int windowMaxY = (int) Math.floor(mapMaxY * mapScale);

		for (int i = windowMaxY; i >= windowMinY && windowMaxY - i < height; i--) {
			double lat = (Math.atan(sinh(i / mapScale)));
			lat = Math.toDegrees(lat);
			lat = ((maxY - lat) / deltaLat) * ortho_height;

			g.drawImage(orthoImage, 0, windowMaxY - i, width, windowMaxY - i
					+ 1, 0, (int) lat, ortho_width, (int) lat + 1, null);

		}

		y += 260 * scale;
		overlay.setImage(image, x / (double) scale, y / (double) scale,
				1. / scale);
		overlay.setRect(x, y, width, height);
		overlay.setResolution(res);
		
		return true;
	}

	private static void drawTile512(Tile512Overlay overlay, 
			float minX, float minY, float maxX, float maxY, 
			double pixelsPerDegreeLon, double pixelsPerDegreeLat, 
			int tileLevel, double degreesPerTile,
			Graphics2D g,
			ConnectionWrapper wrapper) 
	{
		double[] wesn = overlay.lsd.wesn;
		
		if (minX > maxX) return;
		if (minY > maxY) return;
		
		int x0 = (int) ((minX + 180) / degreesPerTile);
		int x1 = (int) ((maxX + 180) / degreesPerTile);
		int y0 = (int) ((minY + 90) / degreesPerTile);
		int y1 = (int) ((maxY + 90) / degreesPerTile);
		
		HttpClient client = new HttpClient();
		HostConfiguration hostConfiguration = client.getHostConfiguration();
		
		for (int tileX = x0; tileX <= x1; tileX++) 
		{
			double tileLon0 = tileX * degreesPerTile - 180;
			double tileLon1 = tileLon0 + degreesPerTile;
			if (tileLon0 > wesn[1]) continue;
			if (tileLon1 < wesn[0]) continue;
			
			
			for (int tileY = y0; tileY <= y1; tileY++)
			{
				double tileLat0 = tileY * degreesPerTile - 90; 
				double tileLat1 = tileLat0 + degreesPerTile; 
				
				if (tileLat0 > wesn[3]) continue;
				if (tileLat1 < wesn[2]) continue;
				
				String path = overlay.lsd.imagePath
					+ tileLevel + "/" 
					+ tileY + "/" 
					+ tileY + "_" + tileX + "." +  overlay.lsd.imageExtension;
				
				BufferedImage img = null;
				
				URL url;
				URI uri;
				List<Proxy> list;
				try {
					url = URLFactory.url(path);
					uri = url.toURI();
				} catch (MalformedURLException e) {
					e.printStackTrace();
					continue;
				} catch (URISyntaxException e) {
					e.printStackTrace();
					continue;
				}
				
				if (uri.getScheme().equalsIgnoreCase("file"))
					try {
						img = ImageIO.read(url);
					} catch (IOException e) {
						e.printStackTrace();
					}
				else 
					img = readHTTPImage(uri, hostConfiguration, client, wrapper);

				if (img != null) {
					int dx1 = (int) ((tileLon0 - minX) * pixelsPerDegreeLon);
					int dx2 = (int) ((tileLon1 - minX) * pixelsPerDegreeLon);
					int dy2 = (int) ((maxY - tileLat0) * pixelsPerDegreeLat);
					int dy1 = (int) ((maxY - tileLat1) * pixelsPerDegreeLat);
					
					g.drawImage(img, dx1, dy1, dx2, dy2, 0, 0, overlay.lsd.tileSize, overlay.lsd.tileSize, null);
				}
			}
		}
	}
	
	private static BufferedImage readHTTPImage(URI uri,
			HostConfiguration hostConfiguration, HttpClient client,
			ConnectionWrapper wrapper) {
		List<Proxy> list = ProxySelector.getDefault().select(uri);
		for (Proxy p : list)
		{
			InetSocketAddress addr = (InetSocketAddress) p.address();
			
			if (addr == null)
				hostConfiguration.setProxyHost(null);
			else
				hostConfiguration.setProxy(addr.getHostName(), addr.getPort());
			
			try {
				HttpMethod method = new GetMethod(uri.toString());
				synchronized (wrapper) {
					wrapper.connection = method;
				}
				
				int sc = client.executeMethod(hostConfiguration, method);
				
				if (sc != HttpStatus.SC_OK) {
					continue;
				}
				
				// Check Content Type
				Header h = method.getResponseHeader("Content-Type");
				if (h == null || !h.getValue().contains("image")) 
					continue;
				
				return ImageIO.read( method.getResponseBodyAsStream() );
			} catch (IOException ex) {
				continue;
			}
		}
		return null;
	}

	private static double sinh(double x) {
		return (Math.exp(x) - Math.exp(-x)) / 2;
	}
}
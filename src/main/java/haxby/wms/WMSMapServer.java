package haxby.wms;

import haxby.map.MapApp;
import haxby.map.MapOverlay;
import haxby.proj.Projection;
import haxby.util.ConnectionWrapper;
import haxby.util.URLFactory;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

public class WMSMapServer {

	public static boolean getSPImage(Rectangle2D rect, 
			MapOverlay overlay,
			int mapRes, 
			String baseURL,
			String srs) {
		return getSPImage(rect, overlay, mapRes, baseURL, srs, null);
	}

	public static boolean getSPImage(Rectangle2D rect, 
			MapOverlay overlay,
			int mapRes, 
			String baseURL,
			String srs,
			ConnectionWrapper wrapper) {
		if (!"EPSG:3031".equals(srs))
			return false;

		double zoom = overlay.getXMap().getZoom();
		int res = mapRes;
		while(zoom*res/mapRes > 1.5 && res>1) {
			res /=2;
		}
		int scale = mapRes/res;
		int x = (int)Math.floor(scale*(rect.getX()-320.));
		int y = (int)Math.floor(scale*(rect.getY()-320.));
		int width = (int)Math.ceil( scale*(rect.getX()-320.+rect.getWidth()) ) - x;
		int height = (int)Math.ceil( scale*(rect.getY()-320.+rect.getHeight()) ) - y;

		Rectangle2D.Double bounds = new Rectangle2D.Double(
				rect.getX() - 320, 
				rect.getY() - 320,
				rect.getWidth(),
				rect.getHeight());

		bounds.x *= 25600;
		bounds.y *= 25600;
		bounds.width *= 25600;
		bounds.height *= 25600;

		StringBuffer requestURL = new StringBuffer(baseURL);

		requestURL.append("&BBOX=");
		requestURL.append(bounds.getMinX());
		requestURL.append(",");
		requestURL.append(-bounds.getMaxY());
		requestURL.append(",");
		requestURL.append(bounds.getMaxX());
		requestURL.append(",");
		requestURL.append(-bounds.getMinY());

		requestURL.append("&WIDTH=");
		requestURL.append(width);
		requestURL.append("&HEIGHT=");
		requestURL.append(height);

		// WMS url
		System.out.println("wms: " + requestURL.toString());
		MapApp.sendLogMessage("Imported_WMS&URL="+requestURL.toString());

		BufferedImage image = getWMSTile(requestURL.toString(), width, height, wrapper);

		x += 320*scale;
		y += 320*scale;
		overlay.setImage(image, x/(double)scale, y/(double)scale, 1./(double)scale);
		overlay.setRect(x, y, width, height);
		overlay.setResolution(res);
		return true;
	}

	public static boolean getMercImage(Rectangle2D rect, MapOverlay overlay,
			int mapRes, String baseURL, String srs) {
		return getMercImage(rect, overlay, mapRes, baseURL, srs, null);
	}

	public static boolean getMercImage(Rectangle2D rect, MapOverlay overlay,
			int mapRes, String baseURL, String srs, ConnectionWrapper wrapper) {
//System.out.println("srs " + srs);
		if (!srs.equals("EPSG:4326"))
			return false;

		long sTime2 = System.currentTimeMillis();

		double zoom = overlay.getMap().getZoom();
		int res = mapRes;
		while (zoom * res / mapRes > 1.5 && res > 1) {
			res /= 2;
		}
		float scale = mapRes / res;
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
		int ortho_height = height * 2;

		Projection proj = overlay.getXMap().getProjection();
		Point2D minLonLat = proj.getRefXY(rect.getX(), rect.getMinY());
		Point2D maxLonLat = proj.getRefXY(rect.getMaxX(), rect.getMaxY());
		float minX = (float) minLonLat.getX();
		float minY = (float) minLonLat.getY();
		float maxX = (float) maxLonLat.getX();
		float maxY = (float) maxLonLat.getY();
//
//		XYZ r1 = XYZ.LonLat_to_XYZ(new Point2D.Double(minX,minY));
//		XYZ r2 = XYZ.LonLat_to_XYZ(new Point2D.Double(maxX,minY));
//		double angle = Math.acos( r1.dot(r2) );
//		double targetRes = angle / width;
//		System.out.println(targetRes*Projection.major[0] + "\t" + res);

		if (minY > maxY) {
			float swap = maxY;
			maxY = minY;
			minY = swap;
		}

		int xOffset = 0;
		while (minX < -180)
			minX += 360;
		while (minX > 180)
			minX -= 360;
		while (maxX < -180)
			maxX += 360;
		while (maxX > 180)
			maxX -= 360;

		if (wrap > 0) {
			if (rect.getWidth() > wrap) {
				maxX = minX;
			}
		}

		double dLat, dLon;

		dLat = maxY - minY;
		if (maxX < minX)
			dLon = 180 - minX + (maxX - -180);
		else if (maxX == minX)
			dLon = 360;
		else
			dLon = maxX - minX;

		double ratioScale = (double) ortho_width / ortho_height * (dLat)
				/ (dLon);

		if (ratioScale > 1)
			ortho_width = (int) Math.ceil(ortho_width / ratioScale);
		else
			ortho_height = (int) Math.ceil(ortho_height * ratioScale);

		BufferedImage orthoImage = new BufferedImage(ortho_width, ortho_height,
				BufferedImage.TYPE_INT_ARGB);
		String bbox;
		String widthStr;
		String heightStr;

		if (maxX < minX) {
			bbox = "bbox=" + minX + "," + minY + "," + "180," + maxY + "&";

			int minMapX = (int) Math.floor(proj.getMapXY(
					new Point2D.Float(minX, 0)).getX()
					* scale);
			int dateLineMapX = (int) (Math.ceil(proj.getMapXY(180, 0).getX())
					* scale);
			if (minMapX > dateLineMapX)
				dateLineMapX *= 3;
			xOffset = dateLineMapX - minMapX;
			if (ratioScale > 1)
				xOffset = (int) Math.ceil(xOffset / ratioScale);

			widthStr = "width=" + xOffset + "&";
			heightStr = "height=" + ortho_height + "&";

			String url = baseURL + bbox + widthStr + heightStr;
//			Special consideration given to USGS WMS, must have specific order
			System.out.println(url);
			MapApp.sendLogMessage("Imported_WMS&URL="+url);
			BufferedImage tile = getWMSTile(url, ortho_width - xOffset,
					ortho_height, wrapper);
			orthoImage.createGraphics().drawImage(tile, 0, 0, null);

			minX = -180;
		} else if (minX == maxX) {
			bbox = "bbox=" + minX + "," + minY + "," + "180," + maxY + "&";

			int minMapX = (int) Math.floor(proj.getMapXY(
					new Point2D.Float(minX, 0)).getX()
					* scale);
			int dateLineMapX = (int) (Math.ceil(proj.getMapXY(180, 0).getX())
					* scale);
			if (minMapX > dateLineMapX)
				dateLineMapX *= 3;
			xOffset = dateLineMapX - minMapX;
			if (ratioScale > 1)
				xOffset = (int) Math.ceil(xOffset / ratioScale);

			widthStr = "width=" + xOffset + "&";
			heightStr = "height=" + ortho_height + "&";

			String url = baseURL + bbox + widthStr + heightStr;
			//System.out.println(url);

			BufferedImage tile = getWMSTile(url, ortho_width - xOffset,
					ortho_height, wrapper);
			orthoImage.createGraphics().drawImage(tile, 0, 0, null);

			minX -= 180;
		}

		bbox = "bbox=" + minX + "," + minY + "," + maxX + "," + maxY + "&";

		widthStr = "width=" + (ortho_width - xOffset) + "&";
		heightStr = "height=" + ortho_height + "&";

		String url = baseURL + bbox + widthStr + heightStr;
		// WMS url
		System.out.println("wms: " + url);
		MapApp.sendLogMessage("Imported_WMS&URL="+url);

		BufferedImage tile = getWMSTile(url, ortho_width - xOffset,
				ortho_height, wrapper);
		orthoImage.createGraphics().drawImage(tile, xOffset, 0, null);

		long sTime = System.currentTimeMillis();
		// Ortho to Mercator
		BufferedImage image = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();

		float deltaLat = maxY - minY;
		float minYR = (float) Math.toRadians(minY);
		float maxYR = (float) Math.toRadians(maxY);

		float mapMinY = (float) Math.log(Math.tan(minYR) + 1 / Math.cos(minYR));
		float mapMaxY = (float) Math.log(Math.tan(maxYR) + 1 / Math.cos(maxYR));
		float mapScale = height / (mapMaxY - mapMinY);
		int windowMinY = (int) Math.ceil(mapMinY * mapScale);
		int windowMaxY = (int) Math.floor(mapMaxY * mapScale);

		// int rgb[] = new int[ortho_width];
		for (int i = windowMaxY; i >= windowMinY && windowMaxY - i < height; i--) {
			double lat = (Math.atan(sinh(i / mapScale)));
			lat = Math.toDegrees(lat);
			lat = ((maxY - lat) / deltaLat) * ortho_height;

			g.drawImage(orthoImage, 0, windowMaxY - i, width, windowMaxY - i
					+ 1, 0, (int) lat, ortho_width, (int) lat + 1, null);

			// rgb = orthoImage.getRGB(0, (int) (lat), ortho_width, 1, rgb, 0,
			// 1);
			// image.setRGB(0, windowMaxY - i, ortho_width, 1, rgb, 0, 1);
		}
		// Display fetch time
		System.out.println("Fetch Time: "
				+ (System.currentTimeMillis() - sTime2) / 1000f);
		// System.out.println("Project Time: " + (System.currentTimeMillis() -
		// sTime) / 1000f);

		y += 260 * scale;
		overlay.setImage(image, x / (double) scale, y / (double) scale,
				1. / (double) scale);
		overlay.setRect(x, y, width, height);
		overlay.setResolution(res);
		return true;
	}

	private static BufferedImage getWMSTile(String urlString, int width,
			int height) {
		return getWMSTile(urlString, width, height, null);
	}

	private static BufferedImage getWMSTile(String urlString, int width,
			int height, ConnectionWrapper wrapper) {
		HttpClient client = new HttpClient();
		HostConfiguration hostConfig = client.getHostConfiguration();

		try {
			URL url = URLFactory.url(urlString);
			URI uri = url.toURI();
			List<Proxy> list = ProxySelector.getDefault().select(uri);

			HttpMethod method = new GetMethod(uri.toString());
			synchronized (wrapper) {
				wrapper.connection = method;
			}

			Iterator<Proxy> iter = list.iterator();
			while (iter.hasNext()) {
				Proxy p = iter.next();
				InetSocketAddress addr = (InetSocketAddress) p.address();

				if (addr == null)
					hostConfig.setProxyHost(null);
				else
					hostConfig.setProxy(addr.getHostName(), addr.getPort());

				int sc;
				try {
					sc = client.executeMethod(hostConfig, method);
				} catch (IOException ex) {
					if (!iter.hasNext())
						throw ex;
					continue;
				}

				if (sc != HttpStatus.SC_OK) {
					System.err.println("Status Code: " + sc);
					for (Header h : method.getResponseHeaders())
						System.err.println(h);

					return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				}

				// Check Content Type
				Header h = method.getResponseHeader("Content-Type");
				if (h == null || !h.getValue().contains("image"))
					return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

				return ImageIO.read( method.getResponseBodyAsStream() );
			}

		} catch (IOException ex) {
			return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		} catch (URISyntaxException e) {
			return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		} finally {
			synchronized (wrapper) {
				if (wrapper.connection != null)
					wrapper.connection.releaseConnection();

				wrapper.connection = null;
			}
		}
		return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	}

	private static double sinh(double x) {
		return (Math.exp(x) - Math.exp(-x)) / 2;
	}
}

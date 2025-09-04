package haxby.layers.image;

import haxby.map.XMap;
import haxby.proj.Projection;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class GeographicImageOverlay extends ImageOverlay {

	protected GeoRefImage geoImage;

	public GeographicImageOverlay(XMap map, GeoRefImage source) {
		super(map);
		this.geoImage = source;
	}

	public GeographicImageOverlay(XMap map, 
			BufferedImage img,
			double[] wesn) {
		this(map,  new GeoRefImage(img, wesn) );
	}

	public double[] getWESN() {
		return geoImage.wesn;
	}

	protected void retrieveImage(Rectangle2D rect) {
		int mapRes = geoImage.maxViewRes;

		double zoom = map.getZoom();
		int res = mapRes;
		while (zoom * res / mapRes > 1.5 && res > 1) {
			res /= 2;
		}
		int scale = mapRes / res;
		double wrap = map.getWrap();

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

		Projection proj = map.getProjection();
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

		if (ortho_width == 0 || ortho_height == 0) return;

		BufferedImage orthoImage = new BufferedImage(ortho_width, ortho_height,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = orthoImage.createGraphics();

		if (maxX < minX) {
			drawTile(minX, minY, 180, maxY, 
					pixelsPerDegreeLon, pixelsPerDegreeLat, g);

			g.translate((180 - minX) * pixelsPerDegreeLon, 0);
			minX = -180;
		} else if (minX == maxX) {
			drawTile(minX, minY, 180, maxY, 
					pixelsPerDegreeLon, pixelsPerDegreeLat, g);

			g.translate(180 * pixelsPerDegreeLon, 0);
			minX -= 180;
		}
		drawTile(minX, minY, maxX, maxY, 
				pixelsPerDegreeLon, pixelsPerDegreeLat, g);

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
		setImage(image, x / (double) scale, y / (double) scale,
				1. / scale);
		setRect(x, y, width, height);
	}

	private void drawTile(
			float minX, float minY, float maxX, float maxY, 
			double pixelsPerDegreeLon, double pixelsPerDegreeLat, 
			Graphics2D g) 
	{
		double[] wesn = geoImage.wesn;

		if (minX > maxX) return;
		if (minY > maxY) return;

		int dx1 = (int) ((wesn[0] - minX) * pixelsPerDegreeLon);
		int dx2 = (int) ((wesn[1] - minX) * pixelsPerDegreeLon);
		int dy2 = (int) ((maxY - wesn[2]) * pixelsPerDegreeLat);
		int dy1 = (int) ((maxY - wesn[3]) * pixelsPerDegreeLat);

		if (Math.abs(dx1 - dx2) < 5) return;
		if (Math.abs(dy1 - dy2) < 5) return;

		try {
			BufferedImage source = geoImage.getImage();

			g.drawImage(source, dx1, dy1, dx2, dy2, 
					0, 0, source.getWidth(), source.getHeight(), null);
		} catch (OutOfMemoryError oome) {
			oome.printStackTrace();
			JLabel memE = new JLabel("<html>GeoMapApp needs more memory to display this image<br>Continue without image or restart from terminal with more memory<br>java -Xmx512m -jar GeoMapApp.jar</html>");
			JOptionPane.showMessageDialog(haxby.map.MapApp.anchor, memE, "Out Of Memory Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private static double sinh(double x) {
		return (Math.exp(x) - Math.exp(-x)) / 2;
	}
}

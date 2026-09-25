package haxby.layers.image;

import haxby.map.FocusOverlay;
import haxby.map.MapOverlay;
import haxby.map.XMap;
import haxby.proj.Projection;
import haxby.util.WESNSupplier;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ImageOverlaySet extends MapOverlay 
				implements FocusOverlay, WESNSupplier {

	protected List<GeoRefImage> images = new LinkedList<GeoRefImage>();
	protected String name;
	
	public ImageOverlaySet(XMap map) {
		super(map);
	}
	
	public void addGeoImage(GeoRefImage image) {
		images.add(image);
	}
	
	public void removeGeoImage(GeoRefImage image) {
		images.remove(image);
	}
	
	public double[] getWESN() {
		double[] wesn = new double[] {180, -180, 90, -90};
		if (images.size() == 0) return null;
		
		for (GeoRefImage geoRef : images)
		{
			wesn[0] = Math.min(wesn[0], geoRef.wesn[0]);
			wesn[1] = Math.max(wesn[1], geoRef.wesn[1]);
			wesn[2] = Math.min(wesn[2], geoRef.wesn[2]);
			wesn[3] = Math.max(wesn[3], geoRef.wesn[3]);
		}
		return wesn;
	}
	
	public Runnable createFocusTask(final Rectangle2D rect) {
		return new Runnable() {
			public void run() {
				getMercImage(rect);
				
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						map.repaint();
					}
				});
			}
		};
	}
	
	public void focus(Rectangle2D rect) {
		getMercImage(rect);
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				map.repaint();
			}
		});
	}

	protected void getMercImage(Rectangle2D rect) {
		int maxRes = -1;
		int mapRes = Integer.MAX_VALUE;
		for (GeoRefImage geoImg : images)
			maxRes = Math.max(maxRes, geoImg.maxRes);
		if (maxRes != -1)
			mapRes = maxRes;
		
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
					pixelsPerDegreeLon, pixelsPerDegreeLat,  
					scale, g);
			
			g.translate((180 - minX) * pixelsPerDegreeLon, 0);
			minX = -180;
		} else if (minX == maxX) {
			drawTile(minX, minY, 180, maxY, 
					pixelsPerDegreeLon, pixelsPerDegreeLat,  
					scale, g);

			g.translate(180 * pixelsPerDegreeLon, 0);
			minX -= 180;
		}		
		drawTile(minX, minY, maxX, maxY, 
				pixelsPerDegreeLon, pixelsPerDegreeLat,  
				scale, g);
		
		
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
			double scale,
			Graphics2D g) 
	{
		if (minX > maxX) return;
		if (minY > maxY) return;
		
		for (GeoRefImage geoImage : images) {
			double[] wesn = geoImage.wesn;
			
			if (wesn[0] > maxX) continue;
			if (wesn[1] < minX) continue;
			if (wesn[2] > maxY) continue;
			if (wesn[3] < minY) continue;
			
			if (geoImage.minViewRes > scale) continue;
			if (geoImage.maxViewRes < scale) continue;
			
			int dx1 = (int) ((wesn[0] - minX) * pixelsPerDegreeLon);
			int dx2 = (int) ((wesn[1] - minX) * pixelsPerDegreeLon);
			int dy2 = (int) ((maxY - wesn[2]) * pixelsPerDegreeLat);
			int dy1 = (int) ((maxY - wesn[3]) * pixelsPerDegreeLat);
			
			if (Math.abs(dx1 - dx2) < 4) return;
			if (Math.abs(dy1 - dy2) < 4) return;

			try {
				BufferedImage source = geoImage.getImage();
						
				g.drawImage(source, dx1, dy1, dx2, dy2, 
						0, 0, source.getWidth(), source.getHeight(), null);
			} catch (OutOfMemoryError oome) {
				oome.printStackTrace();
				JLabel memE = new JLabel("<html>GeoMapApp needs more memory to display this image<br>Continue without image or restart from terminal with more memory<br>java -Xmx512m -jar GeoMapApp.jar</html>");
				JOptionPane.showMessageDialog(haxby.map.MapApp.anchor, memE, "Out Of Memory Error",
						JOptionPane.ERROR_MESSAGE);
				break;
			}
		}
	}
	
	private static double sinh(double x) {
		return (Math.exp(x) - Math.exp(-x)) / 2;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return name;
	}
}

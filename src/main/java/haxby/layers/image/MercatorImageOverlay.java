package haxby.layers.image;

import haxby.map.XMap;
import haxby.proj.Projection;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class MercatorImageOverlay extends ImageOverlay {

	protected GeoRefImage geoImage;

	public MercatorImageOverlay(XMap map, GeoRefImage source) {
		super(map);
		this.geoImage = source;
	}

	public MercatorImageOverlay(XMap map, 
			BufferedImage img,
			double[] wesn) {
		this(map,  new GeoRefImage(img, wesn) );
	}

	public double[] getWESN() {
		return geoImage.wesn;
	}

	protected void retrieveImage(Rectangle2D rect) {
		double[] wesn = geoImage.wesn;

		int mapRes = geoImage.maxViewRes;

		double zoom = map.getZoom();
		int res = mapRes;
		while (zoom * res / mapRes > 1.5 && res > 1) {
			res /= 2;
		}
		int scale = mapRes / res;
		double wrap = map.getWrap();

		boolean fullWrap = false;
		if (rect.getWidth() > wrap && wrap != -1) {
			rect = new Rectangle2D.Double(0, rect.getY(), wrap, rect
					.getHeight());
			fullWrap = true;
		}

		int x = (int) Math.floor(scale * rect.getX());
		int y = (int) Math.floor(scale * (rect.getY() - 260.));
		int width = (int) Math.ceil(scale * (rect.getX() + rect.getWidth()))
				- x;
		int height = (int) Math.ceil(scale
				* (rect.getY() - 260. + rect.getHeight()))
				- y;

		System.out.println(x + "\t" + y + "\t" + width + "\t" + height);

		Projection proj = map.getProjection();
		Point2D minLonLat = proj.getRefXY(rect.getX(), rect.getMinY());
		Point2D maxLonLat = proj.getRefXY(rect.getMaxX(), rect.getMaxY());

		Point2D minMapXY = proj.getMapXY(wesn[0], wesn[3]);
		Point2D maxMapXY = proj.getMapXY(wesn[1], wesn[2]);

		System.out.println(wesn[0] + "\t" + wesn[1]);
		System.out.println(minMapXY.getX() * scale + "\t" + maxMapXY.getX() * scale);
		System.out.println(x + "\t" + wrap * scale);

		int sX = (int) (minMapXY.getX() * scale - x);
		int sY = (int) ((minMapXY.getY() - 260) * scale - y);

		int eX = (int) (maxMapXY.getX() * scale - x);
		int eY = (int) ((maxMapXY.getY() - 260) * scale - y);

		while (sX > eX) {
			eX += wrap * scale;
		}
		
		System.out.println(sX + "\t" + sY);
		System.out.println(eX + "\t" + eY);
		
		if (width < 0 || height < 0)
			return;

		BufferedImage mercImage = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = mercImage.createGraphics();
		BufferedImage source = geoImage.getImage();
		g.drawImage(geoImage.getImage(), sX, sY, eX, eY, 0, 0, source.getWidth(), source.getHeight(), null);
		if (fullWrap && eX > width) 
		{
			sX = (int) (sX - wrap * scale);
			eX = (int) (eX - wrap * scale);
			g.drawImage(geoImage.getImage(), sX, sY, eX, eY, 0, 0, source.getWidth(), source.getHeight(), null);
		}

		while (x + width > wrap * scale)
			x -= wrap * scale;

		y += 260 * scale;
		setImage(mercImage, x / (double) scale, y / (double) scale,
				1. / scale);
		setRect(x, y, width, height);
	}
}

package org.geomapapp.grid;

import haxby.proj.Projection;
import haxby.proj.ProjectionFactory;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.geomapapp.geom.MapProjection;

public class NASAGridComposer {

	private static final String NASA_ELEV_WMS = 
		PathUtil.getPath("GLOBAL_GRIDS/NASA_ELEV_WMS", "http://www.nasa.network.com/elev?");
	private static final int BG_COLOR = -9999;
	private static final float maxRes = 0.125f;
	
	public static boolean getNASAGrid(Rectangle2D rect,
			Grid2DOverlay overlay, int mapRes, double zoom) {

		double wrap = overlay.getXMap().getWrap();
		float res = mapRes;
		while(zoom*res/mapRes > 1.5 && res > maxRes) {
			res /=2;
		}

		int scale = (int) (mapRes/res);
		int x = (int)Math.floor(scale*rect.getX());
		int y = (int)Math.floor(scale*(rect.getY()-260.));
		int width = (int)Math.ceil( 
				scale*(rect.getX()+rect.getWidth()) ) - x;
		int height = (int)Math.ceil( 
				scale*(rect.getY()-260.+rect.getHeight()) ) - y;

		Projection gridProj = ProjectionFactory.getMercator( (int) (1024*320/res) );
		Rectangle bounds = new Rectangle(x, y, width, height);
		
		Grid2D.Double grid = new Grid2D.Double( bounds, gridProj);
		grid.initGrid();
		Grid2D.Boolean landMask = new Grid2D.Boolean( bounds, gridProj);
		landMask.initGrid();

		Projection proj = overlay.getXMap().getProjection();
		Point2D p = proj.getRefXY(rect.getX(), rect.getY());
		double minX, maxX, minY, maxY;
		int xOffset = 0;
		minX = p.getX();
		minY = p.getY();
		p = proj.getRefXY(rect.getMaxX(), rect.getMaxY());
		maxX = p.getX();
		maxY = p.getY();

		if (minY > maxY) {
			double swap = maxY;
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
		
		if (wrap > 0) {
			if (rect.getWidth() > wrap) {
				maxX = minX;
			}
		}
		
//		XYZ r1 = XYZ.LonLat_to_XYZ(new Point2D.Double(minX, minY));
//		XYZ r2 = XYZ.LonLat_to_XYZ(new Point2D.Double(maxX, minY));
//		double angle = Math.acos( r1.dot(r2) );
//		double targetRes = angle / width;
//		System.out.println(targetRes *Projection.major[0]+ "m" + "\t" + res);

		StringBuffer sb = new StringBuffer(NASA_ELEV_WMS);

		if (!sb.toString().endsWith("?"))
			sb.append("?");
		
		if (!sb.toString().toLowerCase().contains("service=wms"))
			sb.append("service=WMS");
		sb.append("&request=GetMap");

		sb.append("&version=");
		sb.append("1.3.0");

		//    sb.append("&srs=EPSG:4326");
		sb.append("&srs=EPSG:3785");

		sb.append("&layers=");
		sb.append("mergedAsterElevations");

		sb.append("&styles=");

		sb.append("&format=");
		sb.append("application/bil16");

		sb.append("&bgColor=");
		sb.append(BG_COLOR);

		boolean hasLand = false;
		boolean hasOcean = false;
		
		if (maxX < minX) {
			StringBuffer sb2 = new StringBuffer(sb);

			double minMapX = Math.floor(proj.getMapXY(
					new Point2D.Double(minX, 0)).getX()
					* scale);
			double dateLineMapX = (Math.ceil(proj.getMapXY(180, 0).getX())
					* scale);
			
			if (minMapX > dateLineMapX)
				dateLineMapX *= 3;
			
			xOffset = (int) (dateLineMapX - minMapX);

			boolean[] flags = readWMSElev(sb2, grid, landMask, 
					xOffset, height,
					minX, 180, minY, maxY);
			hasLand = flags[0];
			hasOcean = flags[1];
			
			minX = -180;
		} else if (minX == maxX) {
			StringBuffer sb2 = new StringBuffer(sb);
			
			double minMapX =  Math.floor(proj.getMapXY(
					new Point2D.Double(minX, 0)).getX()
					* scale);
			double dateLineMapX = (Math.ceil(proj.getMapXY(180, 0).getX())
					* scale);
			if (minMapX > dateLineMapX)
				dateLineMapX *= 3;
			xOffset = (int) (dateLineMapX - minMapX);
			
			boolean[] flags = readWMSElev(sb2, grid, landMask, 
					xOffset, height,
					minX, 180, minY, maxY);
			hasLand = flags[0];
			hasOcean = flags[1];
			
			minX -= 180;
		}
		
		
		boolean[] flags = readWMSElev(sb, grid, landMask, 
				width - xOffset, height,
				minX, maxX, minY, maxY);
		hasLand = hasLand || flags[0];
		hasOcean = hasOcean || flags[1];
		
		if (hasLand || hasOcean) {
			overlay.setGrid(grid, landMask, hasLand, hasOcean, true);
			return true;
		}
		return false;
		

// 		Using NASA's Elevevation Model was forgone for using the WMS server...
		
//		// Calculate our targetResolution in radians
//		XYZ r1 = XYZ.LonLat_to_XYZ(new Point2D.Double(wesn[0], wesn[2]));
//		XYZ r2 = XYZ.LonLat_to_XYZ(new Point2D.Double(wesn[1], wesn[2]));
//		double angle = Math.acos( r1.dot(r2) );
//		double targetRes = angle / width;
//
//		System.out.println(targetRes);
//		System.out.println(width + "\t" + height);
//
//		Sector query = Sector.fromDegrees(wesn[2], wesn[3], wesn[0], wesn[1]);
//		List<LatLon> latlons = new ArrayList<LatLon>();
//
//		for (int yy = 0; yy < bounds.height; yy++)
//			for (int xx = 0; xx < bounds.width; xx++)
//			{
//				Point2D.Double refXY = (Point2D.Double) gridProj.getRefXY(xx + bounds.x, yy + bounds.y);
//				if (refXY.x > 180) refXY.x -= 360;
//				latlons.add(LatLon.fromDegrees(refXY.getY(), refXY.getX()));
//			}
//
//		double d = em.getElevations(query,latlons, targetRes, grid.getBuffer());
//		System.out.println("Best res:\t" + em.getBestResolution(query) * Projection.major[0]);
//		System.out.println("Requested:\t" + targetRes * Projection.major[0]);
//		System.out.println("Got back:\t" + d * Projection.major[0]);
//
//		if (d == Double.MAX_VALUE) return false;
//
//		boolean hasLand = false;
//		boolean hasOcean = false;
//		Grid2D.Boolean landMask = new Grid2D.Boolean( bounds, gridProj);
//		landMask.initGrid();
//		for (int yy = 0; yy < bounds.height; yy++)
//			for (int xx = 0; xx < bounds.width; xx++)
//			{
//				double v = grid.valueAt(bounds.x + xx, bounds.y + yy);
//				if (v == grid.NaN || Double.isNaN(v)) continue;
//				if (v >= 0) {
//					hasLand = true;
//					landMask.setValue(xx + bounds.x, yy + bounds.y, true);
//				} else 
//					hasOcean = true;
//			}
//
//		overlay.setGrid(grid, landMask, hasLand, hasOcean, true);
//
//		return true;
	}
	
	private static boolean[] readWMSElev(StringBuffer sb, Grid2D.Double grid, Grid2D.Boolean landMask,
			int width, int height,
			double minX,double maxX,
			double minY,double maxY) throws IndexOutOfBoundsException {
		sb.append("&width=");
		sb.append(width);
		sb.append("&height=");
		sb.append(height);
		
		sb.append("&bbox=");
		sb.append(minX).append(",");
		sb.append(minY).append(",");
		sb.append(maxX).append(",");
		sb.append(maxY);
		
		ByteBuffer buffer;
		try {
			URL url = URLFactory.url(sb.toString());
			URLConnection con = url.openConnection();
			InputStream is = con.getInputStream();
			ReadableByteChannel channel = Channels.newChannel(is);
			buffer = ByteBuffer.allocate(con.getContentLength());
			buffer.order(ByteOrder.LITTLE_ENDIAN);

			int numBytesRead = 0;
			while (numBytesRead >= 0 && numBytesRead < buffer.limit())
			{
				int count = channel.read(buffer);
				if (count > 0)
					numBytesRead += count;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return new boolean[] {false, false};
		} catch (IOException e) {
			e.printStackTrace();
			return new boolean[] {false, false};
		}

		if (buffer != null)
			buffer.flip();

		ShortBuffer shortBuffer = buffer.asShortBuffer();

		boolean hasLand = false;
		boolean hasOcean = false;
		
		MapProjection gridProj = grid.getProjection();
		Rectangle bounds = grid.bounds;
		double[] gridBuffer = grid.getBuffer();
		int i = -1;
		for (int yy = 0; yy < bounds.height; yy++)
			for (int xx = 0; xx < bounds.width; xx++)
			{
				i++;
				Point2D refXY = gridProj.getRefXY(xx + bounds.x, yy + bounds.y);
				double lat = refXY.getY();
				double lon = refXY.getX();
				while (lon > maxX) lon -= 360;
				if (lon == -180)
					System.currentTimeMillis();

				if (lon > maxX || lon < minX) continue;
				if (lat > maxY || lat < minY) continue;

				short s = (short) lookupElevation(shortBuffer, 
						lat, lon,
						minX, maxX, minY, maxY,
						width, height,
						BG_COLOR);

				if (s == BG_COLOR) continue;
				gridBuffer[i] = s;
				if (s >= 0) {
					hasLand = true;
					landMask.setValue(xx + bounds.x, yy + bounds.y, true);
				} else 
					hasOcean = true;
			}
		
		return new boolean[] {hasLand, hasOcean};
	}

	private static double lookupElevation(ShortBuffer shortBuffer, 
			double lat, double lon,
			double minX, double maxX, double minY, double maxY,
			int width, int height,
			int bgColor) {
		final int tileHeight = height;
		final int tileWidth = width;

		final double sectorDeltaLat = maxY - minY;
		final double sectorDeltaLon = maxX - minX;
		final double dLat = maxY - lat;
		final double dLon = lon - minX;
		final double sLat = dLat / sectorDeltaLat;
		final double sLon = dLon / sectorDeltaLon;

		int j = (int) ((tileHeight - 1) * sLat);
		int i = (int) ((tileWidth - 1) * sLon);
		int k = j * tileWidth + i;

		if (k >= tileHeight * tileWidth) return bgColor;
		if (k < 0) return bgColor;

		double eLeft = shortBuffer.get(k);

		double eRight = i < (tileWidth - 1) ? shortBuffer.get(k + 1) : eLeft;

		if (bgColor == eLeft)
			eLeft = eRight;
		if (bgColor == eRight)
			eRight = eLeft;

		double dw = sectorDeltaLon / (tileWidth - 1);
		double dh = sectorDeltaLat / (tileHeight - 1);
		double ssLon = (dLon - i * dw) / dw;
		double ssLat = (dLat - j * dh) / dh;

		double eTop = eLeft + ssLon * (eRight - eLeft);

		if (j < tileHeight - 1 && i < tileWidth - 1)
		{
			eLeft = shortBuffer.get(k + tileWidth);
			eRight = shortBuffer.get(k + tileWidth + 1);

			if (bgColor == eLeft)
				eLeft = eRight;
			if (bgColor == eRight)
				eRight = eLeft;
		}

		double eBot = eLeft + ssLon * (eRight - eLeft);
		
		if (eTop == bgColor)
			eTop = eBot;
		if (eBot == bgColor)
			eBot = eTop;
		
		return eTop + ssLat * (eBot - eTop);
	}
}

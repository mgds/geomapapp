package org.geomapapp.util;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.imageio.ImageIO;

public class Cursors {
	private static HashMap cursorCache = new HashMap();

	public static final int ZOOM_IN = 0;
	public static final int ZOOM_OUT = 1;
	public static final int HAND = 2;
	public static final int CROSS_HAIR = 3;
	public static final int LASSO = 4;
	static String[] names = new String[] {
			"zoom_in.png",
			"zoom_out.png",
			"hand.png",
			"cross_hair.png",
			"lasso.png"
		};
	static int[][] hotSpot  = new int[][] {
			{9, 9},
			{9, 9},
			{7, 2},
			{10, 10},
			{5,	18}
		};
	public static Cursor getCursor( int which ) {
		Cursor c = (Cursor) cursorCache.get(new Integer(which));
		if (c != null) return c;

		try {
			ClassLoader loader = org.geomapapp.util.Icons.class.getClassLoader();
			String path = "org/geomapapp/resources/icons/" +names[which];
			java.net.URL url = loader.getResource(path);
			BufferedImage im = ImageIO.read(url);
			String name = names[which].substring(0, names[which].lastIndexOf("."));
			System.out.println(im.getWidth() + "\t" + im.getHeight());
			
			c = Toolkit.getDefaultToolkit().createCustomCursor(im, new Point(hotSpot[which][0],hotSpot[which][1]), name);
			cursorCache.put(new Integer(which), c);
			return c;
		} catch(Exception ex) {
			return Cursor.getDefaultCursor();
		}
	}
	public static Cursor ZOOM_IN() {
		int[][] map = {
			{0,0,0,0,1,1,1,1,0,0,0,0,0,0,0,0},
			{0,0,1,1,1,2,2,1,1,1,0,0,0,0,0,0},
			{0,1,1,2,2,2,2,2,2,1,1,0,0,0,0,0},
			{0,1,2,2,2,1,1,2,2,2,1,0,0,0,0,0},
			{1,1,2,2,2,1,1,2,2,2,1,1,0,0,0,0},
			{1,2,2,1,1,1,1,1,1,2,2,1,0,0,0,0},
			{1,2,2,1,1,1,1,1,1,2,2,1,0,0,0,0},
			{1,1,2,2,2,1,1,2,2,2,1,1,0,0,0,0},
			{0,1,2,2,2,1,1,2,2,2,1,0,0,0,0,0},
			{0,1,1,2,2,2,2,2,2,1,1,0,0,0,0,0},
			{0,0,1,1,1,2,2,1,1,1,1,0,0,0,0,0},
			{0,0,0,0,1,1,1,1,0,0,1,1,1,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,1,1,1,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0},
		};
		return doIcon(map, "ZOOM_IN");
	}
	public static Cursor ZOOM_OUT() {
		int[][] map = {
			{0,0,0,0,1,1,1,1,0,0,0,0,0,0,0,0},
			{0,0,1,1,1,2,2,1,1,1,0,0,0,0,0,0},
			{0,1,1,2,2,2,2,2,2,1,1,0,0,0,0,0},
			{0,1,2,2,2,2,2,2,2,2,1,0,0,0,0,0},
			{1,1,2,2,2,2,2,2,2,2,1,1,0,0,0,0},
			{1,2,2,1,1,1,1,1,1,2,2,1,0,0,0,0},
			{1,2,2,1,1,1,1,1,1,2,2,1,0,0,0,0},
			{1,1,2,2,2,2,2,2,2,2,1,1,0,0,0,0},
			{0,1,2,2,2,2,2,2,2,2,1,0,0,0,0,0},
			{0,1,1,2,2,2,2,2,2,1,1,0,0,0,0,0},
			{0,0,1,1,1,2,2,1,1,1,1,0,0,0,0,0},
			{0,0,0,0,1,1,1,1,0,0,1,1,1,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,1,1,1,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0},
		};
		return doIcon(map, "ZOOM_OUT");
	}
	static Cursor doIcon(int[][] map, String name) {
		int black = Color.black.getRGB();
		int white = Color.white.getRGB();
		Dimension dim = Toolkit.getDefaultToolkit().getBestCursorSize(16, 16);
		BufferedImage im;
		if(dim.width==0) {
			im = new BufferedImage(16, 16, 
				BufferedImage.TYPE_INT_ARGB);
		} else {
			im = new BufferedImage(dim.width, dim.height,
				BufferedImage.TYPE_INT_ARGB);
		}
		for(int y=0 ; y<16 ; y++) {
			for( int x=0 ; x<16 ; x++) {
				if(map[y][x] == 1) im.setRGB(x, y, black);
				else if(map[y][x] == 2) im.setRGB(x, y, white);
			}
		}
		return Toolkit.getDefaultToolkit().createCustomCursor(im, new Point(6,6), name);
	}
}

package haxby.util;

import java.awt.image.BufferedImage;
import java.awt.*;

public class Cursors {
	static int black = Color.black.getRGB();
	static int white = Color.white.getRGB();
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

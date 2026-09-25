package haxby.db;

import java.awt.image.*;
import java.awt.*;
import javax.swing.*;

public class Icons {
	public static ImageIcon SELECTED = selectedIcon();
	public static ImageIcon UNSELECTED = unSelectedIcon();
	static ImageIcon selectedIcon() {
		byte[][] map = {
			{0,0,0,0,0,0,0,0,0,0,0,0},
			{0,1,1,1,1,1,1,1,1,1,1,0},
			{0,1,0,0,0,0,0,0,0,0,1,0},
			{0,1,0,1,1,1,1,1,1,0,1,0},
			{0,1,0,1,1,1,1,1,1,0,1,0},
			{0,1,0,1,1,1,1,1,1,0,1,0},
			{0,1,0,1,1,1,1,1,1,0,1,0},
			{0,1,0,1,1,1,1,1,1,0,1,0},
			{0,1,0,1,1,1,1,1,1,0,1,0},
			{0,1,0,0,0,0,0,0,0,0,1,0},
			{0,1,1,1,1,1,1,1,1,1,1,0},
			{0,0,0,0,0,0,0,0,0,0,0,0} };
		BufferedImage im = new BufferedImage(12,12,BufferedImage.TYPE_INT_ARGB);
		for( int y=0 ; y<12 ; y++) {
			for( int x=0 ; x<12 ; x++) {
				if(map[y][x]==1) im.setRGB(x, y, 0xff000000);
				else im.setRGB(x, y, 0);
			}
		}
		return new ImageIcon(im);
	}
	public static ImageIcon unSelectedIcon() {
		byte[][] map = {
			{0,0,0,0,0,0,0,0,0,0,0,0},
			{0,1,1,1,1,1,1,1,1,1,1,0},
			{0,1,0,0,0,0,0,0,0,0,1,0},
			{0,1,0,0,0,0,0,0,0,0,1,0},
			{0,1,0,0,0,0,0,0,0,0,1,0},
			{0,1,0,0,0,0,0,0,0,0,1,0},
			{0,1,0,0,0,0,0,0,0,0,1,0},
			{0,1,0,0,0,0,0,0,0,0,1,0},
			{0,1,0,0,0,0,0,0,0,0,1,0},
			{0,1,0,0,0,0,0,0,0,0,1,0},
			{0,1,1,1,1,1,1,1,1,1,1,0},
			{0,0,0,0,0,0,0,0,0,0,0,0} };
		BufferedImage im = new BufferedImage(12,12,BufferedImage.TYPE_INT_ARGB);
		for( int y=0 ; y<12 ; y++) {
			for( int x=0 ; x<12 ; x++) {
				if(map[y][x]==1) im.setRGB(x, y, 0xff000000);
				else im.setRGB(x, y, 0);
			}
		}
		return new ImageIcon(im);
	}
}

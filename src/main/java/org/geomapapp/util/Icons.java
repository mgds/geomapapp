package org.geomapapp.util;

import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.GrayFilter;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class Icons {
	public static int SELECT = 0;
	public static int ZOOM_IN = 1;
	public static int ZOOM_OUT = 2;
	public static int FORWARD = 3;
	public static int BACK = 4;
	public static int GRID = 5;
	public static int HORIZONTAL = 6;
	public static int VERTICAL = 7;
	public static int MOVE = 8;
	public static int ROTATE = 9;
	public static int SCALE = 10;
	public static int SAVE = 11;
	public static int WIDER = 12;
	public static int STRETCH = 12;
	public static int NARROWER = 13;
	public static int SHRINK = 13;
	public static int UP = 14;
	public static int DOWN = 15;
	public static int NORMALIZE = 16;
	public static int CONTOUR = 17;
	public static int OCEAN = 18;
	public static int LAND = 19;
	public static int CONTINUOUS = 20;
	public static int DISCRETE = 21;
	public static int UNNORMALIZE = 22;
	public static int OCEAN_LAND = 23;
	public static int SPIN = 24;
	public static int INCLINE = 25;
	public static int INFO = 26;
	public static int MASK = 27;
	public static int PROFILE = 28;
	public static int RECTANGLE = 29;
	public static int DIGITIZE = 30;
	public static int POLYGON = 31;
	public static int POINTS = 32;
	public static int HOME = 33;
	public static int FOLDER = 34;
	public static int PARENT = 35;
	public static int TEXT = 36;
	public static int AMP = 37;
	public static int SLANT = 38;
	public static int FOCUS = 39;
	public static int FOSSIL_RANGE = 40;
	public static int RANGE = 41;
	public static int CLOSE = 42;
	public static int LIGHTBULB = 43;
	public static int OPEN = 44;
	public static int HELP = 45;
	public static int HAND = 46;
	public static int CLIP_RECT = 47;
	public static int LASSO = 48;
	public static int MAP_INSET = 49;
	public static int OPEN_HAND = 50;
	public static int PALETTE2 = 51;
	public static int THREED = 52;
	public static int LAYERS = 53;
	public static int MEASUREMENT_ICON = 54;
	public static int CONTRIBUTED_GRIDS_ICON = 55;
	public static int STRATIGRAPHIC_RANGES_ICON = 56;
	public static int LEGEND = 57;
	public static int CAPTURE = 58;
	public static int WARNING = 59;
	public static int GMA_LOGO_64 = 60;
	public static int S_E = 61;
	public static int E_S = 62;
	public static int NASA_GRID = 63;
	public static int LOCK = 64;
	public static int UNLOCK = 65;
	public static int PAN_ICON = 66;
	public static int DIGITIZE_ICON = 67;
	public static int FOCUS_ICON = 68;
	public static int SAVE_ICON = 69;
	public static int SELECT_ICON = 70;
	public static int ZOOM_IN_ICON = 71;
	public static int ZOOM_OUT_ICON = 72;
	public static int LAYERS_ICON = 73;
	public static int PROFILE_ICON = 74;
	public static int GRID_ICON = 75;
	public static int POLYGON_ICON = 76;
	public static int MASK_ICON = 77;
	public static int GRATICULE_ICON = 78;
	public static int ZOOM_UNDO_ICON = 79;
	public static int PALETTE_ICON = 80;
	
	static String[] names = new String[] {
				"select.png",
				"zoom_in.png",
				"zoom_out.png",
				"forward.png",
				"back.png",
				"grid.png",
				"horizontal.png",
				"vertical.png",
				"move.png",
				"rotate.png",
				"scale.png",		// 10
				"save.png",
				"wider.png",
				"narrower.png",
				"up.png",
				"down.png",
				"normalize.png",
				"contour.png",
				"ocean.png",
				"land.png",
				"continuous.png",	// 20
				"discrete.png",
				"unnormalize.png",
				"ocean_land.png",
				"spin.png",
				"incline.png",
				"info_s.gif",
				"mask.png",
				"profile.png",
				"rectangle.png",
				"digitize.png",		// 30
				"polygon.png",
				"points.png",
				"home.png",
				"folder.png",
				"parentFolder.png",
				"text.png",
				"amp.png",
				"slant.png",
				"focus.png",
				"fossil_range.png",	// 40
				"range.png",
				"close.png",
				"lightbulb.png",
				"open.png",
				"help.png",
				"hand.png",
				"clipRect.png",
				"lasso.png",
				"mapInset.png",
				"open_hand.png",	// 50
				"palette2.png",
				"threed.png",
				"layers.png",
				"measurement_icon.png",
				"contributed_grids.png",
				"stratigraphic_ranges_icon.png",
				"legend.png",
				"capture_24x24.gif",
				"GMA_Alert_24x24.png",
				"GMA_Logo_64X64.png",	// 60
				"s_e.png",
				"e_s.png",
				"nasa_grid.png",
				"lock.png",
				"unlock.png",
				"pan_mainToolbar.png",
				"digitize_mainToolbar.png",
				"focus_mainToolbar.png",
				"save_mainToolbar.png",
				"select_mainToolbar.png",	// 70
				"zoom_in_mainToolbar.png",
				"zoom_out_mainToolbar.png",
				"layers_mainToolbar.png",
				"profile_mainToolbar.png",
				"grid_mainToolbar.png",
				"polygon_mainToolbar.png",
				"mask_mainToolbar.png",
				"graticule_mainToolbar.png",
				"zoom_undo_mainToolbar.png",
				"color-palette-icon-40494.png"  // 80
			};
	static XBIcon[][] icons = new XBIcon[names.length][2];
	static XBIcon[][] disIcons = new XBIcon[names.length][2];
	static int[] rgb = { 0, 0xff000000, 0xffffffff, 0xffff0000 };
	static ClassLoader loader = null;
	static XBIcon[] defaultIcon = new XBIcon[2];
	public static XBIcon getDefaultIcon(boolean selected) {
		int i = selected ? 1 : 0;
		if( defaultIcon[i] != null) return defaultIcon[i];
		int i1 = (i+1)%2;
		if( defaultIcon[i1] != null) {
			defaultIcon[i] = new XBIcon( defaultIcon[i1].getImage(), selected);
			return defaultIcon[i];
		}
		BufferedImage im = new BufferedImage(22, 22, 
				BufferedImage.TYPE_INT_ARGB);
		int black = 0xff000000;
		for( int y=0 ; y<22 ; y++) {
			for( int x=0 ; x<22 ; x++) {
				im.setRGB(x,y,0);
			}
		}
		for( int y=6 ; y<16 ; y++ ) {
			im.setRGB( 6, y, black );
			im.setRGB( 15, y, black );
			im.setRGB( y, 6, black );
			im.setRGB( y, 15, black );
		}
		defaultIcon[i] = new XBIcon(im, selected);
		return defaultIcon[i];
	}
	public static XBIcon getIcon(int which, boolean selected) {
		if( which<0 || which>=icons.length ) return getDefaultIcon(selected);
		int i = selected ? 1 : 0;
		if( icons[which][i]!=null )return icons[which][i];
		int i1 = (i+1)%2;
		if( icons[which][i1]!=null ) {
			icons[which][i] = new XBIcon(icons[which][i1].getImage(), selected);
			return icons[which][i];
		}

		try {
			if( loader==null ) {
				loader = org.geomapapp.util.Icons.class.getClassLoader();
			}
			String path = "org/geomapapp/resources/icons/" +names[which];
			java.net.URL url = loader.getResource(path);
			BufferedImage im = ImageIO.read(url);
			icons[which][i] = new XBIcon(im, selected);
		} catch(Exception ex) {
			System.err.println("Error reading " + "org/geomapapp/resources/icons/" + names[which]);
			ex.printStackTrace();
			return getDefaultIcon(selected);
		}
		return icons[which][i];
	}
	public static XBIcon getDisabledIcon(int which, boolean selected) {
		if( which<0 || which>=disIcons.length ) return getDefaultIcon(selected);
		int i = selected ? 1 : 0;
		if( disIcons[which][i]!=null )return disIcons[which][i];
		int i1 = (i+1)%2;
		if( disIcons[which][i1]!=null ) {
			disIcons[which][i] = new XBIcon(disIcons[which][i1].getImage(), selected);
			return disIcons[which][i];
		}

		try {
			if( loader==null ) {
				loader = org.geomapapp.util.Icons.class.getClassLoader();
			}
			String path = "org/geomapapp/resources/icons/" +names[which];
			java.net.URL url = loader.getResource(path);
			BufferedImage im = ImageIO.read(url);
			Image im1 = GrayFilter.createDisabledImage(im);
			Graphics g = im.createGraphics();
			g.drawImage(im1,0,0, new JPanel() );
			disIcons[which][i] = new XBIcon(im, selected);
		} catch(Exception ex) {
			return getDefaultIcon(selected);
		}
		return disIcons[which][i];
	}
	public static void main(String[] args) {
		JFrame frame = new JFrame("Icons");
	//	Box box = Box.createHorizontalBox();
		JToggleButton tb;
		JPanel box = new JPanel( new GridLayout(0, 10) );
		for(int k=-1 ; k<icons.length ; k++) {
			tb = new JToggleButton();
			tb.setIcon( getIcon(k, false) );
			tb.setSelectedIcon( getIcon(k, true) );
			tb.setBorder(null);
			if( k>=0 ) {
				String name = names[k].substring( 0, names[k].lastIndexOf(".") ).toUpperCase();
				if( name.length()>0 ) tb.setToolTipText(name);
			}
			box.add(tb);
		}
		frame.getContentPane().add(box,"North");
		frame.pack();
		frame.show();
		frame.setDefaultCloseOperation( frame.EXIT_ON_CLOSE);
	}
}

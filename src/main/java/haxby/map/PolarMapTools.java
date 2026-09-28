package haxby.map;

import haxby.proj.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.swing.*;

public class PolarMapTools implements ActionListener {
	//			KeyListener {
	PolarMapApp app;
	XMap map;
	MapOverlay overlay;
	JToggleButton[] tb;
	JToggleButton maskB;
	JButton focus;
	JLabel info;
	Box box;
	public PolarMapTools(PolarMapApp app, XMap map, MapOverlay overlay) {
		this.app = app;
		this.map = map;
		this.overlay = overlay;
		init();
	}
	public Box getTools() {
		return box;
	}
	void init() {
		tb = new JToggleButton[2];
		box = Box.createHorizontalBox();
		ButtonGroup bg = new ButtonGroup();
		tb[0] = new JToggleButton(SELECT(false));
		tb[0].setSelectedIcon(SELECT(true));
		tb[0].setBorder( border );
		bg.add(tb[0]);
		tb[0].setSelected(true);
		box.add(tb[0]);
		tb[1] = new JToggleButton(ZOOM_IN(false));
		tb[1].setSelectedIcon(ZOOM_IN(true));
		tb[1].setBorder( border );
		bg.add(tb[1]);
		box.add(tb[1]);
		box.add( Box.createHorizontalStrut(5) );
		focus = new JButton(FOCUS(false));
		focus.setToolTipText("Focus Map - \"F\"");
		focus.setPressedIcon( FOCUS(true) );
		focus.setBorder( border );
	//	bg.add(focus);
		box.add(focus);
		// Mask Button
		maskB = new JToggleButton(MASK(false));
		maskB.setSelected(false);
		maskB.setSelectedIcon(MASK(true));
		maskB.setBorder( border );
		maskB.setToolTipText("Highlight High-Resolution GMRT Data");
		box.add(maskB);

		// Action Listeners
		maskB.addActionListener(this);
		focus.addActionListener( this );
		for( int i=0 ; i<2 ; i++) {
			tb[i].addActionListener( this);
		}
		box.add( Box.createHorizontalStrut(5));
		info = new JLabel("");
		info.setForeground( Color.black);
		info.setFont( new Font("MonoSpaced",Font.PLAIN, 12) );
		box.add(info);
	}
	public void actionPerformed(ActionEvent evt) {
		if( evt.getSource()==maskB ) {
		//	app.setMask( maskB.isSelected() );
		} else if( evt.getSource()==tb[1] ) {
			JOptionPane.showMessageDialog( tb[1].getTopLevelAncestor(),
				"In Map Window:\n     control+click to zoom in\n"
				+"     control+shift+click to zoom out\n"
				+"     control+drag centers selected area in map window",
				"How to Zoom",
				JOptionPane.INFORMATION_MESSAGE);
			tb[0].doClick();
		} else if (evt.getSource()==focus ) {
			app.mapFocus();
		}
	}
	public void mapFocus() {
		double wrap = map.getWrap();
		double zoom = map.getZoom();
		Mercator merc = new Mercator( 0., 0., 360, Projection.SPHERE,
				Mercator.RANGE_0_to_360);
		if( zoom < 2000./wrap ) {
			BufferedImage im = null;
						overlay.setImage( im, 0., 0., 1.);
						return;
				}
				Rectangle2D.Double rect = (Rectangle2D.Double)map.getClipRect2D();
				while( rect.x>wrap ) rect.x-=wrap;
				while( rect.x<0.  ) rect.x+=wrap;
				double[] wesn = new double[4];
		Mercator proj = (Mercator) map.getProjection();
				wesn[0] = rect.x * 360./wrap;
				wesn[1] = wesn[0] + rect.width * 360./wrap;
				wesn[2] = -merc.getY( proj.getLatitude( rect.y + rect.height ) );
				wesn[3] = -merc.getY( proj.getLatitude( rect.y ) );
				int w = (int) Math.rint( zoom*rect.width );
				int h = (int) Math.rint( zoom*rect.height );
				BufferedImage image = new BufferedImage( w, h,
										BufferedImage.TYPE_INT_RGB);
				MapServerA.getHighRes( wesn, w, h, image);
				overlay.setImage( image, rect.x, rect.y, 1./zoom );
				map.repaint();
	}
	static ImageIcon FOCUS(boolean pressed) {
		BufferedImage im;
		im = new BufferedImage(20, 20,
				BufferedImage.TYPE_INT_RGB);
		int color = white;
		if( pressed ) color = 0xffc0c0c0;
		for(int y=0 ; y<20 ; y++) {
			for( int x=0 ; x<20 ; x++) im.setRGB(x, y, color);
		}
		Graphics2D g = im.createGraphics();
		g.setColor(Color.black);
		g.setFont( new Font("Serif", Font.ITALIC, 16));
		g.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.drawString("fx", 1, 15);
		return new ImageIcon( im);
	}
	static ImageIcon MASK(boolean selected) {
		int[][] map = {
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,1,1,1,1,0,0,0,0,1,1,1,1,0,0},
			{0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
			{1,1,1,1,0,0,1,1,1,1,0,0,1,1,1,1},
			{1,1,1,0,0,0,0,1,1,0,0,0,0,1,1,1},
			{1,1,1,1,0,0,1,1,1,1,0,0,1,1,1,1},
			{1,1,1,1,1,1,1,0,0,1,1,1,1,1,1,1},
			{0,1,1,1,1,1,1,0,0,1,1,1,1,1,1,0},
			{0,0,1,1,1,1,0,0,0,0,1,1,1,1,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
		};
		return doIcon(map, selected);
	}
	static ImageIcon SELECT(boolean selected) {
		int[][] map = {
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,1,1,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,1,1,1,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,1,1,1,1,0,0,0,0,0,0,0,0},
			{0,0,0,0,1,1,1,1,1,0,0,0,0,0,0,0},
			{0,0,0,0,1,1,1,1,1,1,0,0,0,0,0,0},
			{0,0,0,0,1,1,1,1,1,1,1,0,0,0,0,0},
			{0,0,0,0,1,1,1,1,1,1,1,1,0,0,0,0},
			{0,0,0,0,1,1,1,1,1,0,0,0,0,0,0,0},
			{0,0,0,0,1,1,0,1,1,0,0,0,0,0,0,0},
			{0,0,0,0,1,0,0,0,1,1,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,1,1,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,1,1,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
		};
		return doIcon(map, selected);
	}
	static ImageIcon ZOOM_IN(boolean selected) {
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
		return doIcon(map, selected);
	}
	static ImageIcon ZOOM_OUT(boolean selected) {
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
		return doIcon(map, selected);
	}
	static int black = Color.black.getRGB();
	static int white = (new Color(240,240,240)).getRGB();
	static ImageIcon doIcon(int[][] map, boolean selected) {
		BufferedImage im;
		im = new BufferedImage(map[0].length+4, map.length+4,
				BufferedImage.TYPE_INT_RGB);
		int color = white;
		if(selected) color = 0xffc0c0c0;
		for(int y=0 ; y<map[0].length+4 ; y++) {
			for( int x=0 ; x<map.length+4 ; x++) im.setRGB(x, y, color);
		}
		for(int y=2 ; y<map[0].length+2 ; y++) {
			for( int x=2 ; x<map.length+2 ; x++) {
				if(map[y-2][x-2] == 1) im.setRGB(x, y, black);
				else im.setRGB(x, y, color);
			}
		}
		return new ImageIcon(im);
	}
	static javax.swing.border.Border border = BorderFactory.createLineBorder(Color.black);
	static javax.swing.border.Border borderSel = BorderFactory.createLoweredBevelBorder();
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		PolarMapTools tools = new PolarMapTools(null, null, null);
		frame.getContentPane().add(tools.getTools(), "Center");
		frame.pack();
		frame.show();
		Box box = Box.createHorizontalBox();
		ButtonGroup bg = new ButtonGroup();
		JToggleButton tb = new JToggleButton(SELECT(false));
		tb.setSelectedIcon(SELECT(true));
		tb.setBorder( border );
		bg.add(tb);
		tb.setSelected(true);
		box.add(tb);
		tb = new JToggleButton(ZOOM_IN(false));
		tb.setSelectedIcon(ZOOM_IN(true));
		tb.setBorder( border );
		bg.add(tb);
		box.add(tb);
		tb = new JToggleButton(ZOOM_OUT(false));
		tb.setSelectedIcon(ZOOM_OUT(true));
		tb.setBorder( border );
		bg.add(tb);
		box.add(tb);
		JButton b = new JButton(FOCUS(false));
		b.setToolTipText("focus map");
		b.setPressedIcon( FOCUS(true) );
		b.setBorder( border );
		bg.add(b);
		box.add(b);
		frame.getContentPane().add(box);
	}
}

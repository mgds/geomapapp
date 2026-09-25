package haxby.db.mcs;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.event.*;

public class Buttons implements ItemListener {
	public Buttons() {
	}
	public void itemStateChanged(ItemEvent e) {
		try {
			AbstractButton b = (AbstractButton)e.getItem();
			if(e.getStateChange()==e.SELECTED) b.setBorder(borderSel);
			else b.setBorder(border);
		} catch(ClassCastException ex) {
			System.out.println(ex.getMessage());
		}
	}
	static javax.swing.border.Border border = BorderFactory.createRaisedBevelBorder();
	static javax.swing.border.Border borderSel = BorderFactory.createLoweredBevelBorder();
	static int black = Color.black.getRGB();
	static int white = (new Color(240,240,240)).getRGB();
	static int gray = (new Color(160,160,160)).getRGB();
	static ImageIcon NEGATIVE() {
		BufferedImage im;
		im = new BufferedImage(20, 20,
			BufferedImage.TYPE_INT_RGB);
		Graphics2D g = im.createGraphics();
		g.setColor(Color.black);
		g.fillRect(0,0,20,20);
		g.setColor(Color.white);
		g.setFont(new Font("Serif",Font.BOLD, 16));
		g.drawString("A", 4, 15);
		return new ImageIcon(im);
	}
	public static ImageIcon POSITIVE() {
		return POSITIVE(false);
	}
	public static ImageIcon POSITIVE(boolean selected) {
		int[][] map = {
			{1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,1,1,1,1},
			{0,0,0,0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
			{1,1,1,1,1,1,1,0,1,1,1,1,1,0,0,0,0,0,0,0},
			{1,1,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,1,1},
			{0,0,1,1,1,1,1,1,1,1,0,0,1,1,1,1,1,1,1,1},
			{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0},
			{1,1,1,1,1,1,1,1,1,1,1,1,0,0,0,0,0,0,0,0},
			{1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,1,0,0,0,0,0,1,1,0,0,0,0},
			{0,0,0,0,0,1,1,1,1,1,1,0,1,1,1,1,1,0,0,0},
		};
		BufferedImage im;
		im = new BufferedImage(20, 20,
			BufferedImage.TYPE_INT_RGB);
		Graphics2D g = im.createGraphics();
		if( selected ) {
			g.setColor(new Color(192,192,192));
		} else {
			g.setColor(new Color(240,240,240));
		}
		g.fillRect(0,0,22,22);
		g.setColor(Color.black);
		g.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(new Font("Serif",Font.BOLD, 16));
		g.drawString("A", 4, 15);
		return new ImageIcon(im);
	}
	static ImageIcon REVERSE() {
		int[][] map = {
			{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
			{1,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,1},
			{1,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,1},
			{1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1},
			{1,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,1},
			{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,0},
			{0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}
		};
		return doIcon(map, false);
	}
	static ImageIcon NORMAL() {
		int[][] map = {
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0},
			{0,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,1},
			{1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,1},
			{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
		};
		return doIcon(map, false);
	}
	static ImageIcon WIDER(boolean selected) {
		int[][] map = {
			{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,1,0,0,0,0,0,0,0,0,1,0,0,0,1},
			{1,0,1,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1},
			{1,1,1,1,1,1,1,1,0,0,1,1,1,1,1,1,1,1},
			{1,0,1,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1},
			{1,0,0,0,1,0,0,0,0,0,0,0,0,1,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		};
		return doIcon(map, selected);
	}
	static ImageIcon NARROWER(boolean selected) {
		int[][] map = {
			{0,0,0,0,0,0,1,0,0,0,0,1,0,0,0,0,0,0},
			{0,0,0,0,0,0,1,0,0,0,0,1,0,0,0,0,0,0},
			{0,0,0,0,0,0,1,0,0,0,0,1,0,0,0,0,0,0},
			{0,0,0,0,0,0,1,0,0,0,0,1,0,0,0,0,0,0},
			{0,0,0,0,0,0,1,0,0,0,0,1,0,0,0,0,0,0},
			{0,0,0,0,0,0,1,0,0,0,0,1,0,0,0,0,0,0},
			{0,0,1,0,0,0,1,0,0,0,0,1,0,0,0,1,0,0},
			{0,0,0,1,1,0,1,0,0,0,0,1,0,1,1,0,0,0},
			{1,1,1,1,1,1,1,0,0,0,0,1,1,1,1,1,1,1},
			{0,0,0,1,1,0,1,0,0,0,0,1,0,1,1,0,0,0},
			{0,0,1,0,0,0,1,0,0,0,0,1,0,0,0,1,0,0},
			{0,0,0,0,0,0,1,0,0,0,0,1,0,0,0,0,0,0},
			{0,0,0,0,0,0,1,0,0,0,0,1,0,0,0,0,0,0},
			{0,0,0,0,0,0,1,0,0,0,0,1,0,0,0,0,0,0},
			{0,0,0,0,0,0,1,0,0,0,0,1,0,0,0,0,0,0},
			{0,0,0,0,0,0,1,0,0,0,0,1,0,0,0,0,0,0},
			{0,0,0,0,0,0,1,0,0,0,0,1,0,0,0,0,0,0},
			{0,0,0,0,0,0,1,0,0,0,0,1,0,0,0,0,0,0}
		};
		return doIcon(map, selected);
	}
	public static ImageIcon SAVE(boolean selected) {
		int[][] map = {
			{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
			{1,1,0,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1},
			{1,0,1,1,0,0,0,0,0,0,0,0,0,0,1,0,1,1},
			{1,1,0,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1},
			{1,0,1,1,0,0,0,0,0,0,0,0,0,0,1,0,1,1},
			{1,1,0,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1},
			{1,0,1,1,0,0,0,0,0,0,0,0,0,0,1,0,1,1},
			{1,1,0,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1},
			{1,0,1,1,0,0,0,0,0,0,0,0,0,0,1,0,1,1},
			{1,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1},
			{1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,1},
			{1,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1},
			{1,0,1,1,1,1,1,1,1,1,1,1,0,0,1,0,1,1},
			{1,1,0,1,1,1,1,1,1,1,1,1,0,0,1,1,0,1},
			{1,0,1,1,1,1,1,1,1,1,1,1,0,0,1,0,1,1},
			{1,1,0,1,1,1,1,1,1,1,1,1,0,0,1,1,0,1},
			{1,0,1,1,1,1,1,1,1,1,1,1,0,0,1,0,1,1},
			{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
		};
		return doIcon(map, selected);
	}
	static ImageIcon doIcon(int[][] map, boolean selected) {
		int white = selected ? gray : Buttons.white;
		BufferedImage im;
		im = new BufferedImage(map[0].length+2, map.length+2, 
			BufferedImage.TYPE_INT_RGB);
		for(int y=0 ; y<map[0].length+2 ; y++) {
			for( int x=0 ; x<map.length+2 ; x++) im.setRGB(x, y, white);
		}
		for(int y=1 ; y<map[0].length+1 ; y++) {
			for( int x=1 ; x<map.length+1 ; x++) {
				if(map[y-1][x-1] == 1) im.setRGB(x, y, black);
				else im.setRGB(x, y, white);
			}
		}
		return new ImageIcon(im);
	}
	static ImageIcon doIcon(int[][] map) {
		BufferedImage im;
		im = new BufferedImage(map[0].length+4, map.length+4, 
			BufferedImage.TYPE_INT_RGB);
		for(int y=0 ; y<map[0].length+4 ; y++) {
			for( int x=0 ; x<map.length+4 ; x++) im.setRGB(x, y, white);
		}
		for(int y=2 ; y<map[0].length+2 ; y++) {
			for( int x=2 ; x<map.length+2 ; x++) {
				if(map[y-2][x-2] == 1) im.setRGB(x, y, black);
				else im.setRGB(x, y, white);
			}
		}
		return new ImageIcon(im);
	}
	public static void main(String[] args) {
		Buttons buttons = new Buttons();
		JFrame frame = new JFrame();
		Box box = Box.createHorizontalBox();
		JToggleButton tb = new JToggleButton(POSITIVE());
		tb.setSelectedIcon(NEGATIVE());
		tb.setBorder(border);
		tb.addItemListener(buttons);
		box.add(tb);
		tb = new JToggleButton(REVERSE());
		tb.setBorder(border);
		tb.addItemListener(buttons);
		box.add(tb);
		JButton b = new JButton(NARROWER(false));
		b.setBorder(border);
		b.addItemListener(buttons);
		box.add(b);
		b = new JButton(WIDER(false));
		b.setBorder(border);
		b.addItemListener(buttons);
		box.add(b);
		frame.getContentPane().add(box);
		frame.pack();
		frame.show();
	}
}

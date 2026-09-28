package org.geomapapp.util;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.imageio.*;
import javax.swing.event.MouseInputAdapter;
import java.beans.*;

public class XBDraw {
	ImageComponent image;
	JPanel tools;
	TextOverlay txt;
	JFileChooser chooser;

	public XBDraw( ImageComponent image ) {
		this.image = image;
		JFrame frame = new JFrame("XBDraw");
		initTools();
		frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new JScrollPane(image));
		frame.getContentPane().add(tools, "North");
		Zoomer z = new Zoomer(image);
		image.addMouseListener(z);
		image.addMouseMotionListener(z);
		image.addKeyListener(z);
		frame.pack();
		Dimension dim = frame.getPreferredSize();
		if( dim.width>1200 )dim.width=1200;
		if( dim.height>900 )dim.height=900;
		frame.setSize(dim);
		frame.show();
	}
	void initTools() {
		tools = new JPanel(new GridLayout(1, 0));
		JButton b = new JButton("T");
		tools.add(b);
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				text();
			}
		});
		b = new JButton("I");
		tools.add(b);
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				image();
			}
		});
		b = new JButton("R");
		tools.add(b);
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				box();
			}
		});
		b = new JButton("Save");
		tools.add(b);
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				save();
			}
		});
		b = new JButton("open");
		tools.add(b);
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				open();
			}
		});
	}
	void open() {
		try {
			image.open();
			image.removeAll();
		} catch(Exception ex) {
			ex.printStackTrace(System.err);
			System.exit(-1);
		}
	}
	void save() {
		if(chooser==null)chooser = image.chooser;
		if(chooser==null)chooser = new JFileChooser(System.getProperty("user.dir"));
		int ok = chooser.showSaveDialog(null);
		if( ok==chooser.CANCEL_OPTION )return;
		File file = chooser.getSelectedFile();
		String name = file.getName();
		int s_idx = file.getName().indexOf(".");
		String suffix = s_idx<0
				? "jpg"
				: file.getName().substring(s_idx+1);
		if( !ImageIO.getImageWritersBySuffix(suffix).hasNext() )suffix = "jpg";
		image.resetTransform();
		Dimension dim = image.getPreferredSize();
		image.setSize(dim);
		BufferedImage im = new BufferedImage(dim.width, dim.height, 
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = im.createGraphics();
		image.paint(g);
		try {
			ImageIO.write( im, suffix, file);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	void box() {
		RectangleOverlay ovl = new RectangleOverlay();
		image.add(ovl,0);
	}
	void text() {
		Font font =(txt==null) ? null : txt.hideDialog();
		txt = new TextOverlay(font);
		image.add(txt,0);
		txt.showDialog();
	}
	void image() {
		if(chooser==null)chooser = image.chooser;
		if(chooser==null)chooser = new JFileChooser(System.getProperty("user.dir"));
		int ok = chooser.showOpenDialog(null);
		if( ok==chooser.CANCEL_OPTION )return;
		try {
			java.awt.image.BufferedImage im = ImageIO.read(chooser.getSelectedFile());
			ImageOverlay ovl = new ImageOverlay(im, chooser.getSelectedFile().getCanonicalPath());
			image.add(ovl,0);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	public static void main(String[] args) {
		ImageComponent im = new ImageComponent();
		if( args.length==2 ) {
			im = new ImageComponent( Integer.parseInt(args[0]), Integer.parseInt(args[1]));
		//	im.setColor(Color.white);
		} else {
			try {
				im.open();
			} catch(Exception ex) {
				ex.printStackTrace(System.err);
				System.exit(-1);
			}
		}
		new XBDraw(im);
	}
}
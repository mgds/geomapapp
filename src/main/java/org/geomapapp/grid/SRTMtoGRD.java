package org.geomapapp.grid;

import org.geomapapp.geom.RectangularProjection;
import org.geomapapp.io.ShowStackTrace;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;

public class SRTMtoGRD {
//	static String root = "/local/data/home/bill/db/srtm/1x1/z_1";
	static String root = "file:///scratch/antarctic/bill/srtm/3arcsec/tmp/1x1_1200_360/z_1/";
	JTextField west, east, south, north;
	JLabel width, height, size;
	JTextField name;
	JFrame frame;
	JFileChooser chooser;
	public SRTMtoGRD() {
		init();
	}
	void init() {
		frame = new JFrame("SRTM 3 arcSec to GRD");
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		Box box = Box.createVerticalBox();
		west = new JTextField("0.", 8);
		east = new JTextField("1.", 8);
		south = new JTextField("0.", 8);
		north = new JTextField("1.", 8);
		width = new JLabel("1201");
		height = new JLabel("1201");
		size = new JLabel(1+(1201*1201*4/1024/1024)+" MB");
		name = new JTextField("Untitled");
		width.setBackground(Color.white);
		height.setBackground(Color.white);
		size.setBackground(Color.white);
		width.setOpaque(true);
		height.setOpaque(true);
		size.setOpaque(true);
		JPanel p1 = new JPanel( new GridLayout(3,3) );
		p1.setBorder( BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder( Color.black ),
				"geographic bounds"));
		p1.add( new JLabel(""));
		p1.add( north );
		p1.add( new JLabel(""));
		p1.add( west );
		p1.add( new JLabel(""));
		p1.add( east );
		p1.add( new JLabel(""));
		p1.add( south );
		p1.add( new JLabel(""));
		JPanel p2 = new JPanel( new GridLayout(4,2,1,1) );
		p2.setBorder( BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder( Color.black ),
				"grd dimensions"));
		p2.add( new JLabel("width") );
		p2.add( width );
		p2.add( new JLabel("height") );
		p2.add( height );
		p2.add( new JLabel("size") );
		p2.add( size );
		JPanel p0 = new JPanel( new GridLayout(1,1) );
		p0.add( name );
		p0.setBorder( BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder( Color.black ),
				"name"));
		box.add(p0);
		box.add(p1);
		box.add(p2);
		JPanel p3 = new JPanel();
		JButton grid = new JButton("grid");
		p3.add( grid );
		box.add(p3);
		grid.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				grid();
			}
		});
		frame.getContentPane().add( box);
		frame.pack();
		frame.show();
		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				update();
			}
		};
		CaretListener cl = new CaretListener() {
			public void caretUpdate(CaretEvent e) {
				update();
			}
		};
		west.addCaretListener(cl);
		east.addCaretListener(cl);
		south.addCaretListener(cl);
		north.addCaretListener(cl);
		west.addActionListener(al);
		east.addActionListener(al);
		south.addActionListener(al);
		north.addActionListener(al);
	}
	void update() {
		try {
			double e = Double.parseDouble(east.getText() );
			double w = Double.parseDouble(west.getText() );
			int wd = (int)Math.rint(1.+1200*(e-w));
			if( wd<0 )wd=0;
			width.setText(wd+"");
		} catch(Exception e) {
		}
		try {
			double n = Double.parseDouble(north.getText() );
			double s = Double.parseDouble(south.getText() );
			int h = (int)Math.rint(1.+1200*(n-s));
			if( h<0 )h=0;
			height.setText(h+"");
		} catch(Exception e) {
		}
		int wd = Integer.parseInt(width.getText());
		int ht = Integer.parseInt(height.getText());
		size.setText(1+(wd*ht*4/1024/1024)+" MB");
	}
	void grid() {
		double e,w,n,s;
		e=w=s=n=-9999.;
		try {
			e = Double.parseDouble(east.getText());
			w = Double.parseDouble(west.getText() );
			n = Double.parseDouble(north.getText() );
			s = Double.parseDouble(south.getText() );
			if( e<=w )throw new NumberFormatException("east must be >= west");
			if( n<=s )throw new NumberFormatException("north must be >= soutn");
		} catch(NumberFormatException ex) {
			String m = ex.getMessage();
			if( !m.startsWith("east") || !m.startsWith("north") ) m = "Parse error "+m;
			JOptionPane.showMessageDialog( frame, m);
			return;
		}
		int x1 = (int)Math.floor(w*1200.);
		int x2 = (int)Math.ceil(e*1200.);
		int y1 = (int)Math.floor((-n)*1200.);
		int y2 = (int)Math.ceil((-s)*1200.);
		int width = x2-x1+1;
		int height = y2-y1+1;
		RectangularProjection proj = new RectangularProjection(new double[] {0.,1.,-1.,0.},
								1201, 1201);
		try {
			TileIO.Short io = new TileIO.Short(proj, root, 1200, 2);
			int g1 = (int)Math.floor(x1/1200);
			int g2 = (int)Math.floor(x2/1200);
			
			Rectangle bounds = new Rectangle(x1, y1, width, height);
//System.out.println(bounds);
			TiledGrid tg = new TiledGrid(proj, bounds, io, 1200, g2-g1+1, null);
			Grid2D.Short g = new Grid2D.Short( bounds, proj);
			g = (Grid2D.Short)tg.composeGrid(g);
			Grd.writeGrd( g, name.getText()+".grd");
		} catch(Throwable ex) {
			ShowStackTrace.showTrace(ex, frame);
		}
	}
	public static void main(String[] args) {
		new SRTMtoGRD();
	}
}

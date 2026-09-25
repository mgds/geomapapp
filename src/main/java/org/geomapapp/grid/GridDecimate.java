package org.geomapapp.grid;

import org.geomapapp.io.*;
import org.geomapapp.geom.*;

import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GridDecimate {
	JFileChooser chooser;
	JFrame frame;
	JTextArea output;
	File dir;
	public GridDecimate() {
		init();
	}
	void init() {
		dir = new File(System.getProperty("user.dir"));
		frame = new JFrame("GridDecimate "+dir.getName());
		frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
		chooser = new JFileChooser(dir);
		output = new JTextArea(30, 80);
		frame.getContentPane().add(new JScrollPane(output));
		JPanel panel = new JPanel();

		JButton button = new JButton("open");
		button.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				open();
			}
		});
		panel.add(button);

		button = new JButton("process");
		button.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				process();
			}
		});
		panel.add(button);
		frame.getContentPane().add(panel, "North");
		frame.pack();
		frame.show();
	}
	void open() {
		chooser.setFileSelectionMode(chooser.DIRECTORIES_ONLY);
		int ok = chooser.showOpenDialog(frame);
		if(ok==chooser.CANCEL_OPTION)return;
		dir = chooser.getSelectedFile();
		String name = dir.getName();
		frame.setTitle("GridDecimate "+name);
		boolean accept = name.startsWith("z_");
		int res = 1;
		if( accept ) {
			try {
				res = Integer.parseInt( name.substring(2) );
			} catch(Exception ex) {
				accept = false;
			}
		}
		if( accept ) {
			accept = (res%2)==0;
		}
		if( !accept ) {
			output.setText("The selected directory must have the form z_####");
			output.append("\n  where #### is an even number");
			return;
		}
		files = FileUtility.getFiles( dir, ".igrid.gz");
		output.setText( files.length +" files");
	}
	File[] files;
	void process() {
		if( files==null ) {
			output.setText("First select a directory");
			return;
		}
		String name = dir.getName();
		int res = Integer.parseInt( name.substring(2) );
		Mercator proj = new Mercator(0., 0., res*320, 0, 0);
		Vector decIndices = new Vector();
		TileIO.Short tileIO = new TileIO.Short( proj,
					dir.getAbsolutePath(),
					320, 0);
		TiledGrid fine = new TiledGrid(proj,
			new Rectangle(0, -260*res, 640*res, 260*2*res),
			tileIO, 320, 16, null);
	System.out.println( (320*res) +"");
		fine.setWrap( 320*2*res);
		int newRes = res/2;
		Mercator newProj = new Mercator(0., 0., newRes*320, 0, 0);
		File newDir = new File(dir.getParentFile(), "z_"+newRes);
		TileIO.Short newIO = new TileIO.Short( newProj,
				newDir.getAbsolutePath(),
				320, 0);

		for( int i=0 ; i<files.length ; i++) {
			int[] indices = tileIO.getIndices(files[i].getName());
			indices[0] = (int) Math.floor( indices[0]*.5 );
			indices[1] = (int) Math.floor( indices[1]*.5 );
			boolean add = true;
			for( int k=0 ; k<decIndices.size() ; k++) {
				int[] tmp = (int[])decIndices.get(k);
				if( tmp[0]==indices[0] && tmp[1]==indices[1] ) {
					add = false;
					break;
				}
			}
			if( add ) decIndices.add(indices);
		}
		output.append( "\n"+decIndices.size() +" new tiles");
		output.repaint();
		for( int k=0 ; k<decIndices.size() ; k++) {
			int[] indices = (int[])decIndices.get(k);
			int x1 = indices[0]*320;
			int y1 = indices[1]*320;
			output.append("\n\t"+ indices[0] +"\t"+ indices[1]);
			output.repaint();
			Grid2D.Short coarse = (Grid2D.Short)newIO.createGridTile(x1, y1);
			double min, max;
			min = max = 0.;
			boolean start = true;
			for( int x=x1 ; x<x1+320 ; x++) {
				for( int y=y1 ; y<y1+320 ; y++) {
					int xx = 2*x;
					int yy = 2*y;
					if( !fine.contains(xx, yy) ) {
						System.out.println(xx +"\t"+ yy +" not in grid");
						System.exit(0);
					}
					double z = fine.valueAt(xx,yy);
					if(Double.isNaN(z))continue;
					if( start ) {
						min = max = z;
						start = false;
						continue;
					}
					if( z<min )min=z;
					else if(z>max)max=z;
				}
			}
			output.append( "\n\t\t"+min +"\t"+ max);
			output.repaint();
			if( max-min<1000. ) {
				double offset = .5*(max+min);
				coarse.scale(offset, 10.);
			}
			double z, z1, val, wt;
			int npt=0;
			for( int x=x1 ; x<x1+320 ; x++) {
				for( int y=y1 ; y<y1+320 ; y++) {
					int xx = 2*x;
					int yy = 2*y;
					val = wt = 0.;
					z = fine.valueAt(xx,yy);
					if(!Double.isNaN(z)) {
						val += z;
						wt += 1.;
					}
					z = fine.valueAt(xx+1,yy);
					z1 = fine.valueAt(xx-1,yy);
					if( !Double.isNaN(z) && !Double.isNaN(z1) ) {
						val += .3*(z+z1);
						wt += .6;
					}
					z = fine.valueAt(xx,yy+1);
					z1 = fine.valueAt(xx,yy-1);
					if( !Double.isNaN(z) && !Double.isNaN(z1) ) {
						val += .3*(z+z1);
						wt += .6;
					}
					z = fine.valueAt(xx+1,yy+1);
					z1 = fine.valueAt(xx-1,yy-1);
					if( !Double.isNaN(z) && !Double.isNaN(z1) ) {
						val += .1*(z+z1);
						wt += .2;
					}
					z = fine.valueAt(xx-1,yy+1);
					z1 = fine.valueAt(xx+1,yy-1);
					if( !Double.isNaN(z) && !Double.isNaN(z1) ) {
						val += .1*(z+z1);
						wt += .2;
					}
					if( wt!=0. ) {
						coarse.setValue(x, y, val/wt);
						npt++;
					}
				}
			}
			try {
				newIO.writeGridTile(coarse);
				output.append("\n\t\t"+npt +" points");
				output.repaint();
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	public static void main(String[] args) {
		new GridDecimate();
	}
}

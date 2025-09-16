package org.geomapapp.util;

import haxby.map.*;
import haxby.proj.*;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.print.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.imageio.*;

public class XYPrint {
	XYGraph graph;
	JToggleButton saveJPEG;
	JToggleButton savePNG;
	JToggleButton saveToFile;
	JToggleButton saveToClipboard;
	JToggleButton print;
	JToggleButton autoX, autoY;
	JTextField xScaleT, yScaleT;
	JComboBox cb;
	public XYPrint( XYGraph graph ) {
		this.graph = graph;
	}
	JMenu getSaveMenu() {
		initSave();
		JMenu fileMenu = new JMenu("Save");
		JMenuItem mi = new JMenuItem("Copy to clipboard");
		mi.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveToClipboard.doClick();
				save();
			}
		});
		fileMenu.add(mi);
		mi = new JMenuItem("Save JPEG image");
		mi.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveJPEG.doClick();
				save();
			}
		});
		fileMenu.add(mi);
		mi = new JMenuItem("Save PNG image");
		mi.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				savePNG.doClick();
				save();
			}
		});
		fileMenu.add(mi);
		mi = new JMenuItem("Save ASCII table");
		mi.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveToFile.doClick();
				save();
			}
		});
		fileMenu.add(mi);
		mi = new JMenuItem("Print");
		mi.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				print.doClick();
				save();
			}
		});
		fileMenu.add(mi);
		return fileMenu;
	}
	void save() {
		Container dialog = graph.getTopLevelAncestor();
		if( dialog==null )return;
		int ok= JOptionPane.CANCEL_OPTION;
		if( print.isSelected() ) {
			PrinterJob job = PrinterJob.getPrinterJob();
			if( fmt==null) {
				PageFormat f = job.defaultPage();
				PageFormat newF = job.pageDialog(f);
				if( newF==f )return;
				fmt = newF;
			} else {
				PageFormat f = job.pageDialog(fmt);
				if( f==fmt ) return;
				fmt = f;
			}
			job.setPrintable(graph, fmt);
			try {
				if(job.printDialog()) job.print();
			} catch (PrinterException pe) {
			}
			return;
		} else if( saveToFile.isSelected() ) {
/*
			JFileChooser chooser = MapApp.getFileChooser();
			ok = chooser.showSaveDialog(dialog);
			if( ok==chooser.CANCEL_OPTION ) return;
			if( chooser.getSelectedFile().exists() ) {
				ok = askOverWrite();
				if( ok==JOptionPane.CANCEL_OPTION ) return;
			}
			try {
				PrintStream out = new PrintStream(
					new FileOutputStream( chooser.getSelectedFile() ));
				float[] data;
				out.println( "Longitude\tLatitude\tElevation\tDistance");
				for( int k=0 ; k<xyz.size() ; k++) {
					data = (float[])xyz.get(k);
					out.println( data[2] +"\t"+
							data[3] +"\t"+
							data[1] +"\t"+
							data[0]);
				}
				out.close();
			} catch(IOException e) {
				JOptionPane.showMessageDialog(dialog,
						" Save failed: "+e.getMessage(),
						" Save failed",
						 JOptionPane.ERROR_MESSAGE);
			}
		} else if( saveToClipboard.isSelected() ) {
			float[] data;
			JTextArea text = new JTextArea();
			text.append("Longitude\tLatitude\tElevation\tDistance\n");
			for( int k=0 ; k<xyz.size() ; k++) {
				data = (float[])xyz.get(k);
				text.append( data[2] +"\t"+
					data[3] +"\t"+
					data[1] +"\t"+
					data[0] +"\n");
			}
			text.selectAll();
			text.copy();
*/
		} else if( saveJPEG.isSelected() ) {
			JFileChooser chooser = MapApp.getFileChooser();
			ok = chooser.showSaveDialog(dialog);
			if( ok==chooser.CANCEL_OPTION ) return;
			if( chooser.getSelectedFile().exists() ) {
				ok = askOverWrite();
				if( ok==JOptionPane.CANCEL_OPTION ) return;
			}
			BufferedImage image = graph.getImage();
			try {
				ImageIO.write(image,
					"jpg",
					chooser.getSelectedFile());
			} catch(IOException e) {
				JOptionPane.showMessageDialog(dialog,
						" Save failed: "+e.getMessage(),
						" Save failed",
						 JOptionPane.ERROR_MESSAGE);
			}
		} else if( savePNG.isSelected() ) {
			JFileChooser chooser = MapApp.getFileChooser();
			ok = chooser.showSaveDialog(dialog);
			if( ok==chooser.CANCEL_OPTION ) return;
			if( chooser.getSelectedFile().exists() ) {
				ok = askOverWrite();
				if( ok==JOptionPane.CANCEL_OPTION ) return;
			}
			BufferedImage image = graph.getImage();
			try {
				ImageIO.write(image,
					"png",
					chooser.getSelectedFile());
			} catch(IOException e) {
				JOptionPane.showMessageDialog(dialog,
						" Save failed: "+e.getMessage(),
						" Save failed",
						 JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	int askOverWrite() {
		Container dialog = graph.getTopLevelAncestor();
		JFileChooser chooser = MapApp.getFileChooser();
		int ok = JOptionPane.NO_OPTION;
		while( true ) {
			ok = JOptionPane.showConfirmDialog(dialog,
				"File exists. Overwrite?",
				"Overwrite?",
				JOptionPane.YES_NO_CANCEL_OPTION);
			if( ok!=JOptionPane.NO_OPTION) return ok;
			ok = chooser.showSaveDialog(dialog);
			if( ok==chooser.CANCEL_OPTION ) return JOptionPane.CANCEL_OPTION;
			if( !chooser.getSelectedFile().exists() ) return JOptionPane.YES_OPTION;
		}
	}
	PageFormat fmt;
	JPanel savePanel;
	void initSave() {
		savePanel = new JPanel(new GridLayout(0,1));
		savePanel.setBorder( BorderFactory.createTitledBorder("Save Options"));
		ButtonGroup gp = new ButtonGroup();
		saveToFile = new JToggleButton("Save ASCII table");
		savePanel.add( saveToFile );
		gp.add( saveToFile );
		saveToClipboard = new JToggleButton("Copy to clipboard");
		savePanel.add( saveToClipboard );
		gp.add( saveToClipboard );
		saveJPEG = new JToggleButton("Save JPEG image");
		savePanel.add( saveJPEG );
		gp.add( saveJPEG );
		savePNG = new JToggleButton("Save PNG image");
		savePanel.add( savePNG );
		gp.add( savePNG );
		print = new JToggleButton("Print");
		savePanel.add( print );
		gp.add( print );
	}
}
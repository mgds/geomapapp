package org.geomapapp.gis.table;

import javax.swing.*;
import java.io.*;
import haxby.util.XBTable;

import java.awt.event.*;

import java.net.URL;

public class HTMLTable {
	public static void main(String[] args) {
		if( args.length!=1) {
			System.out.println("usage: java org.geomapapp.gis.HTMLTable url");
			System.exit(0);
		}
		new HTMLTable(args[0]);
	}
	public HTMLTable(String url) {
		try {
			JEditorPane pane = new JEditorPane(url);
			JFrame frame = new JFrame();
			frame.getContentPane().add(new JScrollPane(pane));
			frame.pack();
			frame.show();
			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					open();
				}
			});
		} catch( Exception e) {
			e.printStackTrace();
		}
	}
	void open() {
		try {
			TableDB t = new TableDB();
			XBTable table = t.createTable();
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			table.setScrollableTracksViewportWidth(false);
			JFrame f = new JFrame( );
			f.getContentPane().add(new JScrollPane(table));
			f.pack();
			f.show();
			f.setDefaultCloseOperation(f.EXIT_ON_CLOSE);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}

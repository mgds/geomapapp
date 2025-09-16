package org.geomapapp.gis.table;

import haxby.util.XBTable;

import org.geomapapp.util.XYGraph;

import java.util.*;
import java.awt.datatransfer.*;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.net.URL;
import javax.swing.*;
import javax.swing.event.*;

public class TableViewer {
	JFileChooser c;
	JFrame frame;
	JScrollPane sp;
	XBTable table;
	TableDB tdb;
	JCheckBox editCB;
	TableViewer parent;
	Vector children;
	GraphDialog graphDialog;
	public TableViewer(TableViewer parent) {
		this.parent = parent;
		try {
			open();
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
	public TableViewer() {
		try {
			open();
		} catch(Throwable e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	void open() throws IOException {
		if(c==null)c = new JFileChooser(System.getProperty("user.dir"));
		int ok = c.showOpenDialog(null);
		if( ok==c.CANCEL_OPTION ) {
			if( frame==null && parent==null &&children==null )System.exit(0);
			return;
		}
		tdb = new TableDB(c.getSelectedFile());
		graphDialog = new GraphDialog(tdb);
		table = tdb.createTable();
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setScrollableTracksViewportWidth(false);
		if( frame==null ) {
			frame = new JFrame( c.getSelectedFile().getName() );
			sp = new JScrollPane(table);
			frame.getContentPane().add(sp);
			JPanel panel = new JPanel(new FlowLayout());
			JButton b = new JButton("open");
			panel.add(b);
			b.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						open();
					} catch(Exception ex) {
						ex.printStackTrace(System.err);
					}
				}
			});
			b = new JButton("new");
			panel.add(b);
			b.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					addChild();
				}
			});
			editCB = new JCheckBox("edit");
			panel.add(editCB);
			editCB.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					edit();
				}
			});
			editCB.setSelected(false);

			b = new JButton("graph");
			panel.add(b);
			b.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					graph();
				}
			});
			
			frame.getContentPane().add(panel, "North");
			frame.getContentPane().add(tdb.getInfoLabel(), "South");
			frame.pack();
			frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
		} else {
			frame.setTitle(c.getSelectedFile().getName() );
			sp.setViewportView( table );
			sp.revalidate();
		}
		frame.show();
	}
	void graph() {
		int ok = graphDialog.showDialog();
		if( ok==JOptionPane.CANCEL_OPTION)return;
		XYGraph g = new XYGraph(
			new GMAGraph( tdb, 
				graphDialog.getXColumn(), 
				graphDialog.getYColumn(), 
				graphDialog.connect() ),
			0);
		JFrame frame = new JFrame();
		frame.getContentPane().add(g);
		frame.pack();
		frame.show();
		frame.setDefaultCloseOperation(frame.DISPOSE_ON_CLOSE);
	}
	public void addChild() {
		TableViewer child = new TableViewer(this);
		if( children==null )children=new Vector();
		children.add( child );
	}
	void edit() {
		tdb.setEditable( editCB.isSelected() );
	}
	public static void main(String[] args) {
		new TableViewer();
	}
}

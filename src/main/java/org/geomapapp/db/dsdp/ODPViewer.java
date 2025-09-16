package org.geomapapp.db.dsdp;

import org.geomapapp.gis.table.*;
import haxby.util.XBTable;
import org.geomapapp.util.XYGraph;
import java.util.*;
import java.awt.datatransfer.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.event.*;

public class ODPViewer {
	JFileChooser c;
	JFrame frame;
	JScrollPane sp;
	XBTable table;
	TableDB tdb;
	JCheckBox editCB;
	ODPViewer parent;
	Vector children;
	GraphDialog graphDialog;
	public ODPViewer(ODPViewer parent) {
		this.parent = parent;
		try {
			open();
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
	public ODPViewer() {
		try {
			open();
		} catch(Throwable e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	void open() throws IOException {
		String url = Janus.showOpenDialog(frame);
		if( url==null ) {
			if( frame==null)System.exit(0);
			return;
		}
	//	URL u = URLFactory.url(url);
	//	URLConnection con = u.openConnection();
	//	con.setDoOutput(true);
	//	PrintStream out = new PrintStream(con.getOutputStream());
	//	out.print("JanusWeb_header_footer=false");
	//	out.close();

		Vector[] data = Janus.parseDataTable(url);
		Vector headings = new Vector();
		for( int k=0 ; k<data[0].size() ; k++) {
			headings.add( ((String[])data[0].get(k))[0]);
		}
		Vector rows = new Vector();
		for( int i=0 ; i<data[1].size() ; i++) {
			Vector row = (Vector)data[1].get(i);
			Vector r = new Vector();
			for( int k=0 ; k<headings.size() ; k++) {
				if( k>=row.size() )r.add("");
				else r.add( ((String[])row.get(k))[0]);
			}
			rows.add(r);
		}
		
		tdb = new TableDB(headings, rows, new StringBuffer());
System.out.println( headings.size() +" columns, "+ rows.size() +" rows");
		graphDialog = new GraphDialog(tdb);
		table = tdb.createTable();
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setScrollableTracksViewportWidth(false);
		if( frame==null ) {
			frame = new JFrame( url );
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
			frame.setTitle(url );
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
		ODPViewer child = new ODPViewer(this);
		if( children==null )children=new Vector();
		children.add( child );
	}
	void edit() {
		tdb.setEditable( editCB.isSelected() );
	}
	public static void main(String[] args) {
		new ODPViewer();
	}
}

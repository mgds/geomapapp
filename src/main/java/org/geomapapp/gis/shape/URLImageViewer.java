package org.geomapapp.gis.shape;

import haxby.util.URLFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.geomapapp.util.Icons;
import org.geomapapp.util.ImageComponent;
import org.geomapapp.util.ParseLink;
import org.geomapapp.util.Zoomer;

public class URLImageViewer {
	ImageComponent image;
	ESRIShapefile shape;
	String[] template;
	JLabel info;
	JDialog dialog;
	
//	GMA 1.4.8: Make dialog a frame so "Layers" window can be minimized
//	Dialog parent;
	JFrame parent;
	
	JScrollPane scroll;
	int row=-1;
	int index=0;
	static final int IMAGE_URL_COLUMN_NUM = 6;
	JComboBox combo;
	ListSelectionListener listSelL;
	Zoomer zoom;
	
//	GMA 1.4.8: Constructor not applicable for a JFrame "Layers" window
/*
	public URLImageViewer(Vector props, ESRIShapefile shape, JLabel info, Dialog parent) {
		this.parent = parent;		
		if( props==null ) {
			info.setText("null properties vector");
			return;
		}
		Vector urls = ParseLink.getProperties(props, "url");
		if( urls.size()==0 ) {
			String msg = (String)ParseLink.getProperty(props, "message");
			if(msg==null)return;
			info.setText(msg);
			return;
		}
		template = new String[urls.size()];
		for( int i=0 ; i<urls.size() ; i++) {
			StringTokenizer st = new StringTokenizer( (String)urls.get(i), "$");
			StringBuffer sb = new StringBuffer();
			while( st.hasMoreTokens() )sb.append(st.nextToken());
			template[i] = sb.toString();
		}
		image = new ImageComponent(400,300);
		this.shape = shape;
		if( info == null )info = new JLabel();
		this.info = info;
		info.setText( shape.toString() );
		init();
	}
*/
	
//	GMA 1.4.8: Added constructor that can accept JFrame
	public URLImageViewer(Vector props, ESRIShapefile shape, JLabel info, JFrame parent) {
		this.parent = parent;
		if( props==null ) {
			info.setText("null properties vector");
			return;
		}
		Vector urls = ParseLink.getProperties(props, "url");
		if( urls.size()==0 ) {
			String msg = (String)ParseLink.getProperty(props, "message");
			if(msg==null)return;
			info.setText(msg);
			return;
		}
		template = new String[urls.size()];
		for( int i=0 ; i<urls.size() ; i++) {
			StringTokenizer st = new StringTokenizer( (String)urls.get(i), "$");
			StringBuffer sb = new StringBuffer();
			while( st.hasMoreTokens() )sb.append(st.nextToken());
			template[i] = sb.toString();
		}
		image = new ImageComponent(400,300);
		this.shape = shape;
		if( info == null )info = new JLabel();
		this.info = info;
		info.setText( shape.toString() );
		init();
	}
	
	void init() {
		dialog = new JDialog(parent, shape.toString());
		index = 0;
		zoom = new Zoomer(image);
		image.addMouseListener(zoom);
	//	image.addMouseMotionListener(zoom);
		image.addKeyListener(zoom);
		dialog.getContentPane().add( new JScrollPane(image) );

		JPanel panel = new JPanel();

		combo = new JComboBox();
		if ( template[0].toLowerCase().indexOf("jason") != -1 ) {
			for( int k=0 ; k<4 ; k++) {
				combo.addItem("camera "+(k+1));
			}
		}
		else {
			for( int k=0 ; k<template.length ; k++) {
				combo.addItem("camera "+(k+1));
			}
		}
		combo.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				open();
			}
		});
		panel.add(combo);

		open();

		JButton back = new JButton(Icons.getIcon(Icons.BACK, false));
		back.setPressedIcon( Icons.getIcon(Icons.BACK, true ));
		back.setBorder( BorderFactory.createEmptyBorder(2,2,2,2));
		back.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				back();
			}
		});
		panel.add(back);

		JButton forward = new JButton(Icons.getIcon(Icons.FORWARD, false));
		forward.setPressedIcon( Icons.getIcon(Icons.FORWARD, true ));
		forward.setBorder( BorderFactory.createEmptyBorder(2,2,2,2));
		forward.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				forward();
			}
		});
		panel.add(forward);

		dialog.getContentPane().add(panel, "North");

		dialog.setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );

		dialog.addWindowListener( new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				shape.getTable().getSelectionModel().removeListSelectionListener(listSelL);
				image.removeMouseListener(zoom);
				image.removeMouseMotionListener(zoom);
				image.removeKeyListener(zoom);
			}
		});

		dialog.pack();
		dialog.setVisible(true);
		listSelL = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				open();
			}
		};
		shape.getTable().getSelectionModel().addListSelectionListener(listSelL);
	}
	void back() {
		int row = shape.getTable().getSelectedRow();
		if( row==0 )return;
		if (row > shape.getTable().getModel().getRowCount()) return;
		row--;
		shape.getTable().setRowSelectionInterval(row, row);
		shape.getTable().ensureIndexIsVisible( row );
	}
	void forward() {
		int row = shape.getTable().getSelectedRow();
		if( row>=shape.getTable().getRowCount()-1 )return;
		row++;
		shape.getTable().setRowSelectionInterval(row, row);
		shape.getTable().ensureIndexIsVisible( row );
	}
	void open() {
		int row = shape.getTable().getSelectedRow();
		if( row==-1 )return;
		int i = combo.getSelectedIndex();
		if( row==this.row && i==this.index ) return;
		this.row=row;
		this.index=i;
		StringBuffer sb = new StringBuffer();
		StringTokenizer st = null;
		if ( template.length - 1 < i ) {
			st = new StringTokenizer( template[0], "{}",true);
		} else {
			st = new StringTokenizer( template[i], "{}",true);
		}
		String [] sImageURLs = ((String)shape.getDBFFile().getValueAt(row, IMAGE_URL_COLUMN_NUM)).split(",");
		while( st.hasMoreTokens() ) {
			String s = st.nextToken();
			if( s.equals("{") ) {
				int col = Integer.parseInt(st.nextToken())-1;
				sb.append( shape.getDBFFile().getValueAt(row, col) );
				st.nextToken();
			} else {
				sb.append( s );
			}
		}
		
		if ( template[0].toLowerCase().indexOf("alvin") != -1 ) {
			try {
				image.setImage( ImageIO.read( URLFactory.url(sb.toString())));
			} catch(IOException e) {
				info.setText( "failed to load image"+ (row+1) );
			}
		}
		else {
			String tempImageURL = "";
			String infoTxt = info.getText();
	//		System.out.println(sb.toString());
			try {
				tempImageURL = template[0].substring(0, template[0].indexOf("{1}"));
	//			System.out.println(tempImageURL);
				String [] imageNameElements = sb.toString().split("\\.");
	//			System.out.println(sImageURLs[i].substring(1) + imageNameElements[imageNameElements.length-2] + "." + imageNameElements[imageNameElements.length-1]);
				
				if ( imageNameElements[imageNameElements.length-2].lastIndexOf("/") != -1 ) {
					tempImageURL += (sImageURLs[i].substring(1) + imageNameElements[imageNameElements.length-2].substring(imageNameElements[imageNameElements.length-2].lastIndexOf("/") + 1) + "." + imageNameElements[imageNameElements.length-1]);
				} else {
					tempImageURL += (sImageURLs[i].substring(1) + imageNameElements[imageNameElements.length-2] + "." + imageNameElements[imageNameElements.length-1]);
				}
				image.setImage( ImageIO.read( URLFactory.url(tempImageURL)));
				info.setText(infoTxt);
			} catch(IOException e) {
				info.setText( "failed to load image"+ (row+1) );
			}
		}
	}
}

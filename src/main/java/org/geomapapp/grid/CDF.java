package org.geomapapp.grid;

import ucar.nc2.*;
import ucar.ma2.*;
import java.util.*;
import java.util.List;
import java.io.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class CDF {
	NetcdfFile nc;
	JFileChooser chooser;
	JFrame frame;
	JTextArea text;
	JComboBox variableCB;
	JComboBox globalCB;
	JComboBox dimCB;
	public CDF() {
		init();
	}
	void init() {
		chooser = new JFileChooser(System.getProperty("user.dir"));
		JButton open = new JButton("Open");
		open.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				open();
			}
		});
		dimCB = new JComboBox();
		dimCB.addItem("Dimensions");
		dimCB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				showDim();
			}
		});
		globalCB = new JComboBox();
		globalCB.addItem("Global");
		globalCB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				showGlobal();
			}
		});
		variableCB = new JComboBox();
		variableCB.addItem("Variables");
		variableCB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				showVar();
			}
		});
		JPanel panel = new JPanel(new FlowLayout());
		panel.add(open);
		panel.add(globalCB);
		panel.add(dimCB);
		panel.add(variableCB);

		text = new JTextArea(10, 50);

		frame = new JFrame("netCDF");
		frame.getContentPane().add( panel, "North");
		frame.getContentPane().add( new JScrollPane(text) );
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation( frame.EXIT_ON_CLOSE);
	}
	void showDim() {
		try {
			text.setText( nc.findDimension( dimCB.getSelectedItem().toString()).toString() );
		} catch(Exception e) {
		}
	}
	void showVar() {
		try {
			text.setText( nc.findVariable( variableCB.getSelectedItem().toString()).toString() );
		} catch(Exception e) {
		}
	}
	void showGlobal() {
		try {
			text.setText( nc.findGlobalAttribute( globalCB.getSelectedItem().toString()).toString() );
		} catch(Exception e) {
		}
	}
	void open() {
		int ok = chooser.showOpenDialog(frame);
		if( ok==chooser.CANCEL_OPTION )return;
		File file = chooser.getSelectedFile();
		String fileName = file.getPath();
		try {
			
//			***** GMA 1.6.4: TESTING
//			nc = new NetcdfFile(fileName);
		
			nc = NetcdfFile.open(fileName);
//			***** GMA 1.6.4

			frame.setTitle( file.getName() +"  "+ nc.getId() );
			
//			***** GMA 1.6.4: TESTING
//			Iterator vi = nc.getGlobalAttributeIterator();
			
			List globalList = nc.getGlobalAttributes();
			Iterator vi = globalList.iterator();
//			***** GMA 1.6.4
			
			globalCB.removeAllItems();
			while(vi.hasNext()) {
				Attribute v = (Attribute) vi.next();
				globalCB.addItem(v.getName());
			}
			
//			***** GMA 1.6.4: TESTING
//			vi = nc.getVariableIterator();
			
			List variableList = nc.getVariables();
			vi = variableList.iterator();
//			***** GMA 1.6.4
			
			variableCB.removeAllItems();
			while(vi.hasNext()) {
				Variable v = (Variable) vi.next();
				variableCB.addItem(v.getName());
			}
			
//			***** GMA 1.6.4: TESTING
//			vi = nc.getDimensionIterator();
			
			List dimensionList = nc.getDimensions();
			vi = dimensionList.iterator();
//			***** GMA 1.6.4			

			dimCB.removeAllItems();
			while(vi.hasNext()) {
				ucar.nc2.Dimension v = (ucar.nc2.Dimension) vi.next();
				dimCB.addItem(v.getName());
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		new CDF();
	}
}

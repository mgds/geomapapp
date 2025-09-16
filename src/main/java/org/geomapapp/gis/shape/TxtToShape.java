package org.geomapapp.gis.shape;

import java.util.*;
import org.geomapapp.gis.table.TableDB;
import javax.swing.JFileChooser;
import java.io.*;
import java.awt.Color;

public class TxtToShape {
	public TxtToShape() {
		JFileChooser choose = new JFileChooser(System.getProperty("user.dir"));
		int ok=choose.showOpenDialog(null);
		if( ok==JFileChooser.CANCEL_OPTION )System.exit(0);
		try {
			File file = choose.getSelectedFile();
			TableDB table = new TableDB(file);
			int nc = table.getColumnCount()-2;
			Vector headings = new Vector(nc);
			Vector classes = new Vector(nc);
			int latCol = table.latCol;
			int lonCol = table.lonCol;
			for( int k=0 ; k<nc+2 ; k++) {
				if( k==latCol || k==lonCol)continue;
				headings.add( table.getColumnName(k) );
				Class cl = table.getColumnClass(k);
				if( Number.class.isAssignableFrom(cl) ) cl = Number.class;
				else if( cl != Boolean.class ) cl = String.class;
				classes.add(cl);
			}
			String name = file.getName();
			name = name.substring(0,name.lastIndexOf("."));
			ESRIShapefile shapes = new ESRIShapefile( name, 1, headings, classes);
			for( int i=0 ; i<table.getRowCount() ; i++) {
				Vector row = new Vector(nc);
				for( int k=0 ; k<nc+2 ; k++) {
					if( k==latCol || k==lonCol)continue;
					if( table.getColumnClass(k)==Color.class) {
						Color c = (Color)table.getValueAt(i,k);
						row.add( c.getRed()+","+c.getGreen()+","+c.getBlue() );
					} else {
						row.add( table.getValueAt(i,k) );
					}
				}
				double lat = Double.parseDouble( table.getValueAt(i,latCol).toString() );
				double lon = Double.parseDouble( table.getValueAt(i,lonCol).toString() );
				shapes.addShape( new ESRIPoint(lon,lat), row);
			}
			shapes.forward( new org.geomapapp.geom.IdentityProjection(), -1.);
			shapes.writeShapes(new File(file.getParentFile(), name));
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new TxtToShape();
	}
}

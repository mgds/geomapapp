package org.geomapapp.gis.shape;

import haxby.util.URLFactory;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.geomapapp.gis.table.TableDB;

public class TabToShape {
	String path;
	public TabToShape(String path) throws IOException {
		this.path = path;
	}
	public ESRIShapefile getShapefile() throws IOException {
		boolean url = path.startsWith("http:") || path.startsWith("file:");
		TableDB db = url ? new TableDB(path) : new TableDB(new File(path));
		int latC = db.latCol;
		int lonC = db.lonCol;
		Vector classes = new Vector(db.getColumnCount()-2);
		Vector headings = new Vector(db.getColumnCount()-2);
		String name = url ? (URLFactory.url(path)).getFile() : (new File(path)).getName();
		name = name.substring( 0, name.lastIndexOf(".") );
		for( int k=0 ; k<db.getColumnCount() ; k++) {
			if( k==latC || k==lonC )continue;
			if( Number.class.isAssignableFrom( db.getColumnClass(k) ) )classes.add(Number.class);
			else if( db.getColumnClass(k)==Boolean.class  )classes.add(Boolean.class);
			else classes.add(String.class);
			headings.add( db.getColumnName(k) );
		}
		ESRIShapefile shp = new ESRIShapefile( name, 1, headings, classes);
		for( int i=0 ; i<db.getRowCount() ; i++) {
			Vector record = new Vector(headings.size());
			for( int k=0 ; k<db.getColumnCount() ; k++) {
				if( k==latC || k==lonC )continue;
				record.add( db.getValueAt(i,k) );
			}
			try {
				ESRIPoint p = new ESRIPoint(
					Double.parseDouble( db.getValueAt(i,lonC).toString() ), 
					Double.parseDouble( db.getValueAt(i,latC).toString() ));
				shp.addShape( p, record );
			} catch(Exception e) {
			}
		}
		return shp;
	}
	public static void main(String[] args) {
		try {
			javax.swing.JFileChooser c = new javax.swing.JFileChooser(System.getProperty("user.dir"));
			while( true ) {
				int ok = c.showOpenDialog(null);
				if( ok==javax.swing.JFileChooser.CANCEL_OPTION )System.exit(0);
				File f = c.getSelectedFile();
				TabToShape ts = new TabToShape( f.getPath() );
				ESRIShapefile shp = ts.getShapefile();
				String name = shp.getName();
				File file = new File( f.getPath(), name );
				c.setSelectedFile(file);
				ok = c.showSaveDialog(null);
				if( ok==javax.swing.JFileChooser.CANCEL_OPTION )System.exit(0);
				shp.writeShapes( c.getSelectedFile() );
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}

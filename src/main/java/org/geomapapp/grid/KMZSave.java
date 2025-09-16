package org.geomapapp.grid;

import org.geomapapp.geom.*;

import haxby.map.MapTools;

import javax.imageio.ImageIO;
import java.io.*;

import javax.swing.JFileChooser;
import java.util.Vector;
import org.geomapapp.util.ParseLink;
import java.util.zip.*;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

public class KMZSave {
	Grid2DOverlay grid;
	public KMZSave(Grid2DOverlay grid) {
		this.grid = grid;
	}
	public void save() throws IOException {
		Grid2D.Image g = null;
		try {
			g = grid.getGeoRefImage().getGeoImage();
		} catch(Exception e) {
		}
		if( g==null )return;
		save( grid.getMap().getTopLevelAncestor(), g);
	}
	public void save( java.awt.Component comp, Grid2D.Image g ) throws IOException {
		JFileChooser chooser = haxby.map.MapApp.getFileChooser();
		chooser.setSelectedFile(new File("untitled" + MapTools.saveCount++ + ".kmz"));
		chooser.setFileFilter( new FileFilter() {
			public String getDescription() {
				return "Google Earth KMZ";
			}
			public boolean accept(File f) {
				return  f.isDirectory() ||
					f.getName().toLowerCase().endsWith(".kmz");
			}
		});
		
		int ok = JOptionPane.NO_OPTION;
		String name = "";
		File kmz = null;
		while( true ) {
			ok = chooser.showSaveDialog( comp );
			if( ok==JFileChooser.CANCEL_OPTION )return;
			File file = chooser.getSelectedFile();
			name = chooser.getSelectedFile().getName();
			if( name.toLowerCase().endsWith(".kmz") ) 
				name = name.substring(0, name.toLowerCase().indexOf(".kmz"));
			File path = file.getParentFile();
			kmz = new File( path, name+".kmz" );
			if( kmz.exists() ) {
				ok = JOptionPane.showConfirmDialog(comp, "File exists.  Overwrite?");
				if( ok==JOptionPane.CANCEL_OPTION )return;
				if( ok==JOptionPane.OK_OPTION )break;
			} else {
				break;
			}
		}
		ZipOutputStream zip = new ZipOutputStream(
			new FileOutputStream(kmz) );
		ZipEntry entry = new ZipEntry(name+".kml");
		zip.putNextEntry(entry);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(bytes);
		out.println( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
		out.println( "<kml xmlns=\"http://earth.google.com/kml/2.0\">" );
		Vector p0 = new Vector();
		Vector p1 = new Vector();
		p0.add( new Object[] { "GroundOverlay", p1 });
		p1.add( new String[] { "description", "Image from GeoMapApp" });
		p1.add( new String[] { "name", name });
		p1.add( new String[] { "visibility", "1" });
		p1.add( new String[] { "open", "1" });

		double[] wesn = ((RectangularProjection)g.getProjection()).getWESN();
		while (wesn[0] > 180)
			wesn[0] -= 360;
		while (wesn[1] > 180)
			wesn[1] -= 360;
		
		// fix bug on crossing if east is less then west e+360
		if (wesn[1] < wesn[0]) {
			wesn[1] += 360;
		}
		
		java.awt.Rectangle bounds = g.getBounds();
		double dx = (wesn[3]-wesn[2])/(bounds.height-1.)*111111.;
		double dist = dx*Math.sqrt( Math.pow(bounds.getHeight(),2) + Math.pow(bounds.getWidth(),2) );;
		Vector look = new Vector();
		p1.add( new Object[] { "LookAt", look });
		double lon = (wesn[0]+wesn[1])/2.;
		
		if (lon > 180.0)
			lon -= 360;
		
		look.add( new String[] { "longitude", Double.toString(lon) });
		
		
		
		double lat = (wesn[2]+wesn[3])/2.;
		look.add( new String[] { "latitude", Double.toString(lat) });
		look.add( new String[] { "range", Double.toString(dist) });
		look.add( new String[] { "tilt", "10" });
		look.add( new String[] { "heading", "0" });
		Vector icon = new Vector();
		p1.add( new Object[] { "Icon", icon });
		icon.add( new String[] {"href", name+".png"});
		Vector bnds = new Vector();
		p1.add( new Object[] { "LatLonBox", bnds});
		bnds.add( new String[] {"west", Double.toString(wesn[0]) });
		bnds.add( new String[] {"east", Double.toString(wesn[1]) });
		bnds.add( new String[] {"south", Double.toString(wesn[2]) });
		bnds.add( new String[] {"north", Double.toString(wesn[3]) });
		ParseLink.printXML( p0, 0, out );
		out.println("</kml>");
		out.flush();
		byte[] buf = bytes.toByteArray();
		int len = buf.length;
		zip.write( buf, 0, len);
		entry = new ZipEntry(name+".png");
		zip.putNextEntry(entry);
		out.close();
		
		bytes = new ByteArrayOutputStream();
		javax.imageio.ImageIO.write( g.getBuffer(), "PNG", bytes );
		buf = bytes.toByteArray();
		len = buf.length;
		zip.write( buf, 0, len);
		zip.closeEntry();
		zip.close();
	}
}

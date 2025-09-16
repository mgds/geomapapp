package org.geomapapp.image;

import org.geomapapp.geom.*;
import org.geomapapp.util.*;
import org.geomapapp.gis.shape.*;
import org.geomapapp.grid.TileIO;

import haxby.map.MapApp;
import haxby.proj.PolarStereo;

import java.io.*;

import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

public class XBItoShape {
	public XBItoShape() {
	}
	
//	1.4.4: Read in file name so new files are named correctly.
	public File open(File dir, String fileName, String imageType, int mapType) throws IOException {
		
		Vector headings = new Vector();
		headings.add("name");
		Vector classes = new Vector();
		classes.add( String.class );
	//	ESRIShapefile shapes = new ESRIShapefile( dirs[0].getParentFile().getName(), 5, headings, classes);

		File[] resDir = dir.listFiles( new FileFilter() {
			public boolean accept(File f) {
				if( !f.isDirectory() )return false;
				return f.getName().startsWith("i_");
			}
		});
		if( resDir==null || resDir.length==0 )return null;
		int minRes = Integer.parseInt( resDir[0].getName().substring(2) );
		int mDir = 0;
		int maxRes = minRes;
		for( int k=1 ; k<resDir.length ; k++) {
			int res = Integer.parseInt( resDir[k].getName().substring(2) );
			if( res<minRes ) {
				minRes=res;
			} else if( res>maxRes ) {
				maxRes=res;
				mDir = k;
			}
		}
		int res = maxRes;
		MapProjection proj;
		
		double mPerPixel = 25600;
		int i = 1;
		while (i < maxRes)
		{
			i *= 2;
			mPerPixel /= 2;
		}
		switch (mapType)
		{
		default:
		case MapApp.MERCATOR_MAP:
			proj = new Mercator(0., 0., res*640, 0, 0);
			break;
		case MapApp.SOUTH_POLAR_MAP:
			proj = new PolarStereo(new Point2D.Float(0,0), 180., mPerPixel, -71., PolarStereo.SOUTH, PolarStereo.WGS84);
			break;
		case MapApp.NORTH_POLAR_MAP:
			proj = new PolarStereo(new Point2D.Float(0,0), 0., mPerPixel, 71., PolarStereo.NORTH, PolarStereo.WGS84);
			break;
		}
		File[] files = org.geomapapp.io.FileUtility.getFiles(resDir[mDir],imageType);
		File directory = files[0].getParentFile();
		int nLevel = 0;
		while( !directory.equals(resDir[mDir]) ) {
			nLevel++;
			directory = directory.getParentFile();
		}
		
		int minX, maxX;
		int minY, maxY;
		minX = minY = Integer.MAX_VALUE;
		maxX = maxY = -Integer.MAX_VALUE;
		
		for( int k=0 ; k<files.length ; k++) {
			int[] xy = TileIO.getIndices(files[k].getName());
			int x0 = xy[0]*320;
			int y0 = xy[1]*320;
			
			minX = Math.min(minX, x0);
			maxX = Math.max(maxX, x0 + 328);
			minY = Math.min(minY, y0);
			maxY = Math.max(maxY, y0 + 328);
		}
		Rectangle bounds = new Rectangle(
			minX, minY,
			(maxX-minX+1),
			(maxY-minY+1));

		Point2D p0 = proj.getRefXY(
			new Point( (minX+maxX)/2, (minY+maxY)/2 ));
		Point2D p1 = proj.getRefXY(
			new Point( minX, minY ));
		Point2D p2 = proj.getRefXY(
			new Point( maxX, maxY ));
		
		int mnX = (int)Math.floor(minX/320.);
		int mxX = (int)Math.floor(maxX/320.);
		int mnY = (int)Math.floor(minY/320.);
		int mxY = (int)Math.floor(maxY/320.);
		
		double width = p2.getX()-p1.getX();
		double height = p1.getY()-p2.getY();
		double w = width*.025;
		double h = height*.025;
		ESRIPolygon box = new ESRIPolygon( p1.getX()-.025*width,
						p2.getX()+.025*width,
						p2.getY()-.025*height,
						p1.getY()+.025*height,
						1, 5 );
		box.addPoint( 0, p1.getX()-w, p1.getY()+h);
		box.addPoint( 1, p1.getX()-w, p2.getY()-h);
		box.addPoint( 2, p2.getX()+w, p2.getY()-h);
		box.addPoint( 3, p2.getX()+w, p1.getY()+h);
		box.addPoint( 4, p1.getX()-w, p1.getY()+h);
		
		Vector record = new Vector();

		record.add( fileName );
		
		ESRIShapefile shape = new ESRIShapefile( fileName, 5, headings, classes);
		shape.addShape( box, record );
		File shp = new File(dir, fileName+".shp" );
		shape.writeShapes( new File(dir, fileName ) );

		File linkFile = new File( dir, fileName+".link");
		
		String root = "file://" + dir.getCanonicalPath().replace('\\', '/');
		if( !root.endsWith("/") ) root += "/";
		Vector data = new Vector(1);
		Vector props = new Vector();
		double scl = 1./res;
		data.add( new Object[] { "data", props });
		
//		props.add( new Object[] { "name", dir.getName() });
		props.add( new Object[] { "name", fileName });
		
		props.add( new Object[] { "type", "tiled_images" });
		props.add( new Object[] { "url", root });
		props.add( new Object[] { "x_min", ""+((minX-1)*scl) });
		props.add( new Object[] { "x_max", ""+((maxX+1)*scl) });
		props.add( new Object[] { "y_min", ""+((minY-1)*scl) });
		props.add( new Object[] { "y_max", ""+((maxY+1)*scl) });
		props.add( new Object[] { "res_min", ""+minRes });
		props.add( new Object[] { "res_max", ""+maxRes });
		props.add( new Object[] { "map_type", ""+mapType });
		props.add( new Object[] { "image_type", ""+imageType});
		ParseLink.printXML( data, 0, new PrintStream( new FileOutputStream(linkFile) ));
	//	dir = dirs[0].getParentFile();
	//	shapes.writeShapes( new File(dir, dir.getName() ) );
		return shp;
	}
}

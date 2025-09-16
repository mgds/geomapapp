package org.geomapapp.grid;

import org.geomapapp.geom.*;
import org.geomapapp.util.*;
import org.geomapapp.gis.shape.*;

import haxby.map.MapApp;
import haxby.proj.PolarStereo;

import java.io.*;

import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

public class XBGtoShape {
	public XBGtoShape() {
	}
	
//	1.4.4: Read in file name so new files are named correctly.
	public File open(File dir, String fileName, int mapType) {
		Vector headings = new Vector();
		headings.add("name");
		headings.add("node dx");
		headings.add("minZ");
		headings.add("maxZ");
		Vector classes = new Vector();
		classes.add( String.class );
		classes.add( Number.class );
		classes.add( Number.class );
		classes.add( Number.class );
	//	ESRIShapefile shapes = new ESRIShapefile( dirs[0].getParentFile().getName(), 5, headings, classes);
		File[] resDir = dir.listFiles( new FileFilter() {
			public boolean accept(File f) {
				if( !f.isDirectory() )return false;
				return f.getName().startsWith("z_");
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
		File[] files = org.geomapapp.io.FileUtility.getFiles(resDir[mDir],"igrid.gz");
		if (files.length == 0) return null;
		
		File directory = files[0].getParentFile();
		int nLevel = 0;
		while( !directory.equals(resDir[mDir]) ) {
			nLevel++;
			directory = directory.getParentFile();
		}
		TileIO.Short tileIO = new TileIO.Short(proj,
			resDir[mDir].getAbsolutePath(),
			320, nLevel);
		int minX, maxX;
		int minY, maxY;
		minX = maxX = 0;
		minY = maxY = 0;
		double[] range = new double[] { 0.,0. };
		boolean start = true;
		Grid2D.Short tile;
		for( int k=0 ; k<files.length ; k++) {
			int[] xy = tileIO.getIndices(files[k].getName());
			int x0 = xy[0]*320;
			int y0 = xy[1]*320;
			try {
				tile = (Grid2D.Short)tileIO.readGridTile(x0,y0);
			} catch(Exception ex) {
				continue;
			}
			for(int x=x0 ; x<x0+320 ; x++) {
				for(int y=y0 ; y<y0+320 ; y++) {
					double z=tile.valueAt(x,y);
					if(Double.isNaN(z))continue;
					if( start ) {
						minX = maxX = x;
						minY = maxY = y;
						range[0] = range[1] = z;
						start = false;
						continue;
					}
					if( x>maxX )maxX=x;
					else if( x<minX )minX=x;
					if( y>maxY )maxY=y;
					else if( y<minY )minY=y;
					if( z>range[1] )range[1]=z;
					else if( z<range[0] )range[0]=z;
				}
			}
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
		
		double scale = Math.cos( Math.toRadians( p0.getY()) );
		scale *= proj.major[0]*2.*Math.PI/640./maxRes;
		scale = .1*Math.rint(scale*10.);
		
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

//		1.4.4: In "Import Grid" from the "Layers" button, name of 
//		imported grid is now imported correctly.
//		record.add( dir.getName() );
		record.add( fileName );
		
		record.add( new Float((float)scale) );
		record.add( new Float((float)range[0]) );
		record.add( new Float((float)range[1]) );
	//	record.add( new Integer(minRes) );
	//	record.add( new Integer(maxRes) );

	//	shapes.addShape( box, record );

/*
		ESRIShapefile shape = new ESRIShapefile( dir.getName(), 5, headings, classes);
		shape.addShape( box, record );
		File shp = new File(dir, dir.getName()+".shp" );
		shape.writeShapes( new File(dir, dir.getName() ) );

		File linkFile = new File( dir, dir.getName()+".link");
*/
		
		ESRIShapefile shape = new ESRIShapefile( fileName, 5, headings, classes);
		shape.addShape( box, record );
		File shp = new File(dir, fileName+".shp" );
		try {
			shape.writeShapes( new File(dir, fileName ) );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		File linkFile = new File( dir, fileName+".link");
		
		String root;
		try {
			root = "file://" + dir.getCanonicalPath();
			if( !root.endsWith("/") ) root += "/";
	
		Vector data = new Vector(1);
		Vector props = new Vector();
		double scl = 1./res;
		data.add( new Object[] { "data", props });
		
//		props.add( new Object[] { "name", dir.getName() });
		props.add( new Object[] { "name", fileName });
		
		props.add( new Object[] { "type", "tiled_grid" });
		props.add( new Object[] { "url", root });
		props.add( new Object[] { "x_min", ""+((minX-1)*scl) });
		props.add( new Object[] { "x_max", ""+((maxX+1)*scl) });
		props.add( new Object[] { "y_min", ""+((minY-1)*scl) });
		props.add( new Object[] { "y_max", ""+((maxY+1)*scl) });
		props.add( new Object[] { "z_min", ""+range[0] });
		props.add( new Object[] { "z_max", ""+range[1] });
		props.add( new Object[] { "res_min", ""+minRes });
		props.add( new Object[] { "res_max", ""+maxRes });
		ParseLink.printXML( data, 0, new PrintStream( new FileOutputStream(linkFile) ));
	//	dir = dirs[0].getParentFile();
	//	shapes.writeShapes( new File(dir, dir.getName() ) );
		return shp;
		} catch (IOException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
		}
		return shp;
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length != 2 && args.length != 3)
		{
			System.err.println("Ussage: XBGtoShape dir name [map_type]" +
					"\n\t mercator = 0" +
					"\n\t south_pole = 1" +
					"\n\t north_pole = 2");
			System.exit(0);
		}
		
		new XBGtoShape().open(
				new File(args[0]),
				args[1], 
				args.length == 3 ?
						Integer.parseInt(args[2]) :
						0);
	}
}

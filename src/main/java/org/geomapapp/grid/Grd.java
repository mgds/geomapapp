package org.geomapapp.grid;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JEditorPane;
import javax.swing.JOptionPane;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.geom.Mercator;
import org.geomapapp.geom.RectangularProjection;
import org.geomapapp.geom.UTM;
import org.geomapapp.geom.UTMProjection;

import haxby.util.GeneralUtils;
import haxby.util.PathUtil;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

public class Grd {
	Grid2D.Float grid;
	String source;
	Grd() {
	}
	final static String X_MERC = "Spherical Mercator projected Longitude, -Jm1, length from West Edge.";
	final static String Y_MERC = "Spherical Mercator projected Latitude, -Jm1, length from South Edge.";
	final static String X_GEO = "Degrees Longitude";
	final static String Y_GEO = "Degrees Latitude";
	final static int MEGABYTE = (1024*1024);

	public static void writeGrd( Grid2D grid, String fileName ) throws IOException, Exception {
/*
		Rectangle bounds = grid.getBounds();
		MapProjection proj = grid.getProjection();
		boolean merc = proj instanceof Mercator;
		if( !merc ) merc = proj instanceof haxby.proj.Mercator;
		boolean rect = proj instanceof RectangularProjection;
		if( !merc && !rect ) throw new IOException("Unknown Projection");

		double[] range = grid.getRange();
		Point2D ul = proj.getRefXY(new java.awt.Point(bounds.x,bounds.y));
		Point2D lr = proj.getRefXY(new java.awt.Point(bounds.x+bounds.width-1,
						bounds.y+bounds.height-1));
		double[] wesn = new double[] {
			ul.getX(), lr.getX(), lr.getY(), ul.getY() };
		if( wesn[1]<wesn[0] )wesn[1]+=360.;
*/
		double[] range = grid.getRange();
		Rectangle bounds = grid.getBounds();
		int nx = bounds.width;
		int ny = bounds.height;
		MapProjection proj = grid.getProjection();
		boolean merc = proj instanceof Mercator;
		if ( !merc ) {
			merc = proj instanceof haxby.proj.Mercator;
		}
		double[] wesn = new double[4];
		Point2D.Double p2d = new Point2D.Double( bounds.getX(), bounds.getY() );
		Point2D pt = proj.getRefXY(p2d);
		wesn[0] = pt.getX();
		wesn[3] = pt.getY();
		double north = pt.getY();
		p2d.x = bounds.getX() + bounds.width - 1.;
		p2d.y = bounds.getY() + bounds.height - 1.;
		pt = proj.getRefXY(p2d);
		wesn[1] = pt.getX();
		if( wesn[1] < wesn[0] ) {
			wesn[1] += 360.;
		}
		if( wesn[0] > 180. ) {
			wesn[0] -= 360.;
			wesn[1] -= 360.;
		}
		wesn[2] = pt.getY();

//		System.out.println("wesn[0]: " + wesn[0] + "\t wesn[1]: " + wesn[1] + "\t wesn[2]: " + wesn[2] + "\t wesn[3]: " + wesn[3]);

		double south = pt.getY();
		double dy = ( north - south ) / ( bounds.height - 1. );

		int k=0;
		float minZ = 10000f;
		float maxZ = -10000f;
		float[] newZ = new float[bounds.height];
		double[] yy = new double[bounds.height];
		int[] i0 = new int[bounds.height];
		for( int y = 0; y < bounds.height; y++ ) {
			p2d.y = north - y * dy;
			yy[y] = proj.getMapXY(p2d).getY() - bounds.y;
			double y0 = Math.floor(yy[y]);
			if ( y0 < 1. ) {
				y0 = 1.;
			}
			if( y0 > bounds.height - 3 ) {
				y0 = bounds.height - 3.;
			}
			i0[y] = (int)Math.rint(y0) - 1;
			yy[y] -= y0;
		}
		k=0;
		float[] zVal1 = new float[ bounds.width * bounds.height ];
		for( int x = 0; x < bounds.width; x++ ) {
			for( int y = 0; y < bounds.height; y++ ) {
				newZ[y] = (float)grid.valueAt( x + bounds.x, y + bounds.y );
			}
			zVal1[x] = (float)grid.valueAt( x + bounds.x, bounds.y );
			zVal1[ x + bounds.width * ( bounds.height-1 ) ] = (float)grid.valueAt( x + bounds.x, bounds.y + bounds.height - 1 );
			for( int y = 1; y < bounds.height - 1; y++, k += bounds.width ) {
				k = x + bounds.width * y;
				zVal1[k] = (float)Interpolate2D.cubic( newZ, i0[y], yy[y] );
				if( !Float.isNaN( zVal1[k] ) ) {
					if ( zVal1[k] > maxZ ) {
						maxZ = zVal1[k];
					}
					if ( zVal1[k] < minZ ) {
						minZ = zVal1[k];
					}
				}
			}
		}
//		***** GMA 1.6.4: TESTING
//		NetcdfFileWriteable nc = new NetcdfFileWriteable();
//		nc.setName(fileName);

		Point2D ul = proj.getRefXY( new Point( bounds.x, bounds.y ) );
		Point2D lr = proj.getRefXY( new Point( bounds.x + bounds.width - 1, bounds.y + bounds.height-1 ) );
		NetcdfFileWriteable nc = NetcdfFileWriteable.createNew(fileName, false);
//		if( merc ) {
//			Mercator m = new Mercator( 0., 0., 360, 0, 0 );
//			ul = m.getMapXY(ul);
//			lr  = m.getMapXY(lr);
//			wesn[2] = -lr.getY();
//			wesn[3] = -ul.getY();
//		}
		Group dimensionGroup = new Group( nc, nc.getRootGroup(), "Dimensions" );
		Group attributeGroup = new Group( nc, nc.getRootGroup(), "Attributes" );
		Group variableGroup = new Group( nc, nc.getRootGroup(), "Variables" );
		Dimension xDim = nc.addDimension( "x", bounds.width );
		Dimension yDim = nc.addDimension( "y", bounds.height );
		Variable x = nc.addVariable( "x", DataType.INT, new Dimension[] { xDim } );
		Variable y = nc.addVariable( "y", DataType.INT, new Dimension[] { yDim } );
		Dimension[] zdim  = new Dimension[1];
		zdim[0] = nc.addDimension( "zdim", bounds.width*bounds.height );
		Variable z = nc.addVariable( "z_float", DataType.FLOAT, zdim );
		Attribute conventions = new Attribute( "Conventions", "COARDS" );
		Attribute title = new Attribute( "title", "Geographic Grid" );
		Attribute history = new Attribute( "history", "Created by GeoMapApp" );
		Attribute source = new Attribute( "source", "Spherical Mercator Projected with -Jm1 " + "-R" + wesn[0] + "/" + wesn[1] + "/" + wesn[2] + "/" + wesn[3] );
		Attribute node_offset = new Attribute( "node_offset", new Integer(0) );
		nc.addGlobalAttribute(conventions);
		nc.addGlobalAttribute(title);
		nc.addGlobalAttribute(history);
		nc.addGlobalAttribute(source);
		nc.addGlobalAttribute(node_offset);
		nc.addVariableAttribute( "x", "long_name", "Longitude" );
		nc.addVariableAttribute( "x", "units", "degrees_east" );
		nc.addVariableAttribute( "x", "actual_range", Array.factory( new double[] {wesn[0], wesn[1] } ) );
		nc.addVariableAttribute( "y", "long_name", "Latitude" );
		nc.addVariableAttribute( "y", "units", "degrees_north" );
		nc.addVariableAttribute( "y", "actual_range", Array.factory( new double[] {wesn[2], wesn[3] } ) );
		nc.addVariableAttribute( "z_float", "long_name", "z" );
		nc.addVariableAttribute( "z_float", "_FillValue", Double.NaN );
		nc.addVariableAttribute( "z_float", "actual_range", Array.factory( new double[] { (double)minZ, (double)maxZ } ) );
		nc.addVariable(dimensionGroup, x);
		nc.addVariable(dimensionGroup, y);
		nc.addVariable(variableGroup, z);
//		nc.addGroup( nc.getRootGroup(), dimensionGroup );
//		nc.addGroup( nc.getRootGroup(), attributeGroup );
//		nc.addGroup( nc.getRootGroup(), variableGroup );
//		dimensionGroup.addVariable(x);
//		dimensionGroup.addVariable(y);
//		variableGroup.addVariable(z);

		float[] zVal = new float[ bounds.width * bounds.height ];
		k=0;
		for( int lat = bounds.y; lat < bounds.y + bounds.height; lat++ ) {
			for( int lon = bounds.x; lon < bounds.x + bounds.width; lon++ ) {
				zVal[k++] = (float)grid.valueAt( lon, lat );
			}
		}

		float [] zTmp = new float[zVal1.length];
		int l = 0;
		k = zVal1.length - nx;
		while ( k > -1 ) {
			for ( int m = k; m < ( k + nx ); m++ ) {
				zTmp[l] = zVal1[m];
				l++;
			}
			k = k - nx;
		}
		nc.create();
		try {
			nc.write("z", Array.factory( zTmp ));
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		nc.close();
//		System.out.println("wesn[0]: " + wesn[0] + "\t wesn[1]: " + wesn[1] + "\t wesn[2]: " + wesn[2] + "\t wesn[3]: " + wesn[3]);
//		System.out.println("xDim: " + xDim + " yDim: " + yDim);
//		System.out.println("minZ: " + minZ + " maxZ: " + maxZ);
//		***** GMA 1.6.4

/*
		if( merc )nc.addGlobalAttribute( "source", "Spherical Mercator Projected with -Jm1 "
			+"-R"+wesn[0]+"/"+wesn[1]+"/"+wesn[2]+"/"+wesn[3]);
		if( merc ) {
			Mercator m = new Mercator( 0.,0.,360, 0, 0);
			ul = m.getMapXY( ul );
			lr  = m.getMapXY( lr );
			wesn[2] = -lr.getY();
			wesn[3] = -ul.getY();
		}
		Dimension[] two = new Dimension[1];
		two[0] = nc.addDimension( "two", 2 );
		Dimension[] four = new Dimension[1];
		four[0] = nc.addDimension( "four", 4 );
		Dimension[] zdim  = new Dimension[1];
		zdim[0] = nc.addDimension( "zdim", bounds.width*bounds.height );

		nc.addVariable( "x_range", double.class, two);
		nc.addVariable( "y_range", double.class, two);
		nc.addVariable( "z_range", double.class, two);
		nc.addVariable( "spacing", double.class, two);
		nc.addVariable( "dimension", int.class, two);
		nc.addVariable( "z", float.class, zdim );

		nc.addVariableAttribute("x_range", "units", 
				merc ? X_MERC : X_GEO);
		nc.addVariableAttribute("y_range", "units", 
				merc ? Y_MERC : Y_GEO);
		nc.addVariableAttribute("z_range", "units", 
				"Elevation in meters");

		nc.addVariableAttribute("z", "scale_factor", new Double(1));
		nc.addVariableAttribute("z", "add_offset", new Double(0));
		nc.addVariableAttribute("z", "node_offset", new Integer(0));
		nc.addVariableAttribute("z", "fill_value", new Float( Float.NaN ) );

		nc.create();

		nc.write( "x_range", ArrayAbstract.factory( new double[] {wesn[0], wesn[1] } ));
		nc.write( "y_range", ArrayAbstract.factory( new double[] {wesn[2], wesn[3] } ));
		nc.write( "z_range", ArrayAbstract.factory( range ));
		nc.write( "spacing", ArrayAbstract.factory( new double[] {
					(wesn[1] - wesn[0]) / (bounds.width-1),
					(wesn[3] - wesn[2]) / (bounds.height-1) } ));
		nc.write( "dimension", ArrayAbstract.factory( 
				new int[] { bounds.width, bounds.height } ));
		float[] z = new float[bounds.width*bounds.height];
		int k=0;
		for(int y=bounds.y ; y<bounds.y+bounds.height ; y++) {
			for( int x=bounds.x ; x<bounds.x+bounds.width ; x++) {
				z[k++] = (float)grid.valueAt(x,y);
			}
		}
		nc.write( "z", ArrayAbstract.factory( z ));
		nc.close();
*/
	}
	public static Grid2D.Float readGrd(String fileName ) throws IOException {
		return readGrd( fileName, null );
	}
	
	public static Grid2D.Float readGrd( String fileName, MapProjection proj, GrdProperties grdP, boolean flipGrid, ImportGrid ig) throws IOException {
		return readGrd(fileName, proj, grdP, flipGrid, Double.NaN, ig);
	}

	public static Grid2D.Float readGrd( String fileName, MapProjection proj, GrdProperties grdP, boolean flipGrid, Double noData, ImportGrid ig) throws IOException {
		double[] x_range = grdP.x_range;
		double[] y_range = grdP.y_range;
		double[] z_range = grdP.z_range;
		double[] spacing = grdP.spacing;
		int[] dimension = grdP.dimension;
		float[] z = null;
		double scaleFactor = grdP.scaleFactor;
		double add_offset = grdP.add_offset;
		int node_offset = grdP.node_offset;
		
		NetcdfFile nc = null;
		String fileType;
		MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
		
		try {
			nc = NetcdfFile.open(fileName);
			String fileTypeDesc = nc.getFileTypeDescription();
			fileType = nc.getFileTypeId();
		} catch(java.lang.IllegalArgumentException ex) {
			IOException e = new IOException("Not a netcdf file");
			e.fillInStackTrace();
			throw e;
		}

		boolean coardsCompliant = false;
		List coardsList = nc.getGlobalAttributes();
		Iterator coardsIterator = coardsList.iterator();
		while ( coardsIterator.hasNext() ) {
			Attribute coardsAttribute = (Attribute)coardsIterator.next();
			if ( coardsAttribute.isString() ) {
				if ( coardsAttribute.getStringValue().toLowerCase().indexOf("coards") != -1 ) {
					System.out.println("COARDS compliant");
					coardsCompliant = true;
				}
			}
		}

		List variableList = nc.getVariables();
		Iterator variableListIterator = variableList.iterator();
		while ( variableListIterator.hasNext() ) {
			Variable variable = (Variable)variableListIterator.next();

			if ( variable.getName().equals("z") ) {
				List variableAttributeList = variable.getAttributes();
				Iterator variableAttributeListIterator = variableAttributeList.iterator();


				try {
					if(null != ig) {
						ig.appendNewText("\nReading the grid from " + grdP.file + ". ");
						ig.waiting = true;
						ig.displayWaitingDots();
					}
					String zArrayType = variable.read().getElementType().toString();
	//				System.out.println("Element type for z array: " + zArrayType);
					if ( zArrayType.equals("float")) {
	//					System.out.println("is float");
						z = (float[])variable.read().copyTo1DJavaArray();
						for( int k = 0; k < z.length; k++ ) {
							if (z[k] == noData) {
								z[k] = Float.NaN;
							}
						}
						if( add_offset!=0. || scaleFactor!=1. ) {
							for( int k = 0; k < z.length; k++ ) {
								if( Float.isNaN(z[k]) ) {
									continue;
								}
								double tmp = z[k];
								z[k] = (float)tmp;
							}
						}
					}
					else if (zArrayType.equals("double") ) {
						double[] z_double = (double[]) variable.read().copyTo1DJavaArray();
						z = new float[z_double.length];

						for( int k = 0; k < z_double.length; k++ ) {
							z[k] = (float) z_double[k];
							if (z[k] == noData) {
								z[k] = Float.NaN;
							}
						}
						if( add_offset!=0. || scaleFactor!=1. ) {
							for( int k = 0; k < z_double.length; k++ ) {
								
								if( Float.isNaN(z[k]) ) {
									continue;
								}
								double tmp = z_double[k];
								z[k] = (float)tmp;
							}
						}
					}
					else if ( zArrayType.equals("short") ) {
						short[] tmpShort = (short[])variable.read().copyTo1DJavaArray();
						z = new float[tmpShort.length];
						for (int i = 0; i < tmpShort.length; i++) {
							if(z.length < 100 || (i+1) % (z.length/100) == 0) {
								ig.showPercent(i * 100 / z.length);
							}
							z[i] = tmpShort[i];
							if (z[i] == noData) 
								z[i] = Float.NaN;
						}

						if( add_offset!=0. || scaleFactor!=1. ) {
							for( int k = 0; k < z.length; k++ ) {
								if( Float.isNaN(z[k]) ) {
									continue;
								}
								double tmp = z[k];
								z[k] = (float)tmp;
							}
						}
					}
					if(null != ig) {
						ig.waiting = false;
						ig.appendNewText("\nFinished reading " + grdP.file + ".");
					}
				} catch (OutOfMemoryError e) {

		            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
		            long maxMemory = heapUsage.getMax() / MEGABYTE;
		            long usedMemory = heapUsage.getUsed() / MEGABYTE;
		            System.out.println("Memory Use :" + usedMemory + "M/" + maxMemory + "M");
		            
		            String downloadUrl = PathUtil.getPath("PUBLIC_HOME_PATH") + "UnixInstall.html";
					String msg = "Unable to open " + fileName + ". <br>Out of Memory.<br><br>"
							+ "The grid is large. More memory needs to be allocated to GeoMapApp to import the grid.<br><br>"
							+ "Follow these steps:<br><br>"
							+ "Download the GeoMapApp jar file from here: <a href='"+downloadUrl+"'>"+downloadUrl+"</a><br><br>"
							+ "Run that GeoMapApp jar file from the command line with additional memory allocated. Here is an example command:<br><br>"
							+ "<i>java -jar -Xmx100000m GeoMapApp.jar</i><br><br>"
							+ "Import the grid.";
							
					//create an EditorPane to handle the html and hyperlink
				    JEditorPane ep = GeneralUtils.makeEditorPane(msg);
					JOptionPane.showMessageDialog(haxby.map.MapApp.anchor,ep , "Out of Memory Error", JOptionPane.ERROR_MESSAGE);
					if(null != ig) ig.waiting = false;
					return null;
				}
			}
		}

//		In the COARDS compliant GMT version 4 format, the row order is flipped as compared to GMT-3, thus,
//		this loop flips the row order of the z array

//		NSS 2020-08-12: found an example where the row order was not flipped if the fileType is HDF5.
//		Don't know if this is a general rule for HDF5 (NetCDF-4) files, but if we come across it again,
//      may need to add the following check on fileType:
//		if ( coardsCompliant && fileType != "HDF5") {
		
		// allow user to manually flip the grid 
		if (flipGrid) coardsCompliant = ! coardsCompliant;
		
		if (coardsCompliant) {
			int nx = dimension[0];
			float [] zTmp = new float[z.length];
			int l = 0;
			int k = z.length - nx;
			while ( k > -1 ) {
				for ( int m = k; m < ( k + nx ); m++ ) {
					zTmp[l] = z[m];
					l++;
				}
				k = k - nx;
			}
			z = zTmp;
		}

		if( node_offset==1 ) {
			x_range[0] += .5*spacing[0];
			x_range[1] -= .5*spacing[0];
			y_range[0] += .5*spacing[1];
			y_range[1] -= .5*spacing[1];
		}
		if( proj==null ) {
			proj = new RectangularProjection(
				new double[] { x_range[0], x_range[1], y_range[0], y_range[1] },
				dimension[0], dimension[1]);
		}
		int index = fileName.lastIndexOf(".");
		File infoFile = new File( fileName+".info");
	//	System.out.println( infoFile.getName() );
		if( infoFile.exists() ) {
			try {
				BufferedReader in = new BufferedReader(
					new FileReader( infoFile ));
				String s;
				while( (s=in.readLine())!=null ) {
					StringTokenizer st = new StringTokenizer(s);
					if( st.nextToken().equals("UTM_Zone_Number") ) {
						int zone = Integer.parseInt(st.nextToken());
						int NS = UTM.NORTH;
						if( zone<0 ) {
							zone = -zone;
							NS = UTM.SOUTH;
						}
						double dx = (x_range[1]-x_range[0])/(dimension[0]-1.);
						double dy = (y_range[1]-y_range[0])/(dimension[1]-1.);
						proj = new UTMProjection( x_range[0], y_range[1],
							dx, dy, zone, 2, NS );
						break;
					}
				}
				in.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		Grid2D.Float grid = new Grid2D.Float( new java.awt.Rectangle(0, 0, dimension[0], dimension[1]), proj);
		grid.setBuffer( z );
		return grid;
	}
//	***** GMA 1.6.4

	public static Grid2D.Float readGrd(String fileName, MapProjection proj) throws IOException {
		Grd grd = new Grd();
		NetcdfFile nc = null;
		try {
			nc = NetcdfFile.open(fileName);
		} catch(java.lang.IllegalArgumentException ex) {
			IOException e = new IOException("Not a netcdf file");
			e.fillInStackTrace();
			throw e;
		}

		boolean coardsCompliant = false;
		List coardsList = nc.getGlobalAttributes();
		Iterator coardsIterator = coardsList.iterator();
		if ( coardsIterator.hasNext() ) {
			Attribute coardsAttribute = (Attribute)coardsIterator.next();
			if ( coardsAttribute.isString() ) {
				if ( coardsAttribute.getStringValue().toLowerCase().indexOf("coards") != -1 ) {
					System.out.println("COARDS compliant");
					coardsCompliant = true;
				}
			}
		}

		if ( coardsCompliant ) {
			return null;
		}

//		***** GMA 1.6.4: TESTING
//		Iterator vi = nc.getGlobalAttributeIterator();
		List globalAttributeList = nc.getGlobalAttributes();
		Iterator vi = globalAttributeList.iterator();
//		***** GMA 1.6.4

		while(vi.hasNext()) {
			Attribute v = (Attribute) vi.next();
			if( v.getName().equals("source") ) grd.source = v.getStringValue();
		//	System.out.println( v.getName() +"\t"+ v.getStringValue());
		}

//		***** GMA 1.6.4: TESTING
//		vi = nc.getVariableIterator();
		List variableList = nc.getVariables();
		vi = variableList.iterator();
//		***** GMA 1.6.4

		double[] x_range = null;
		double[] y_range = null;
		double[] z_range = null;
		double[] spacing = null;
		int[] dimension = null;
		float[] z = null;
		double scaleFactor=1.;
		double add_offset=0.;
		int node_offset=0;
		while(vi.hasNext()) {
			Variable v = (Variable) vi.next();
			int[] dims = v.getShape();
						StringBuffer sb = new StringBuffer(v.getName() +":\t(");
						for( int k=0 ; k<dims.length ; k++) {
								sb.append( dims[k] );
								if( k<dims.length-1 ) sb.append(", ");
								else sb.append(")");
						}
			sb.append( "\t"+ v.getDataType().toString());
			// System.out.println( sb );
	if( v.getDataType().toString().equalsIgnoreCase("float") ) {
	//	if( dims.length==2 ) {
	//		float[][] d0 = (float[][])v.read().copyToNDJavaArray();
	//		System.out.println( d0.length +"\t"+ d0[0].length );
	//	}
	//	float[] data = (float[])v.read().copyTo1DJavaArray();
	//	int n = 5;
	//	if( data.length<5 )n = data.length;
	//	for( int k=0 ; k<n ; k++) System.out.println( data[k] );
	}

//			***** GMA 1.6.4: TESTING
//			Iterator vi2  = v.getAttributeIterator();

			List variableAttributeList = v.getAttributes();
			Iterator vi2 = variableAttributeList.iterator();
//			***** GMA 1.6.4

			while(vi2.hasNext()) {
				Attribute a = (Attribute) vi2.next();
				if( a.isString() ) {
				//	System.out.println( "\t"+ a.getName() +"\t"+ a.getStringValue());
				} else {
				//	System.out.println( "\t"+ a.getName() +"\t"+ 
				//	a.getValueType().getName() +"\t"+a.getNumericValue());
				}
			}
			if( v.getName().equals( "x_range" )) {
				x_range = (double[])v.read().copyTo1DJavaArray();
			} else if( v.getName().equals( "y_range" )) {
				y_range = (double[])v.read().copyTo1DJavaArray();
			} else if( v.getName().equals( "z_range" )) {
				z_range = (double[])v.read().copyTo1DJavaArray();
			} else if( v.getName().equals( "spacing" )) {
				spacing = (double[])v.read().copyTo1DJavaArray();
			} else if( v.getName().equals( "dimension" )) {
				dimension = (int[])v.read().copyTo1DJavaArray();
			} else if( v.getName().equals( "z" )) {

//				***** GMA 1.6.4: TESTING
//				Iterator it = v.getAttributeIterator();

				List variableAttributeList2 = v.getAttributes();
				Iterator it = variableAttributeList2.iterator();
//				***** GMA 1.6.4

				while(it.hasNext()) {
					Attribute att = (Attribute) it.next();
					if( att.getName().equals("scale_factor") ) {
						scaleFactor = att.getNumericValue().doubleValue();
					} else if( att.getName().equals("add_offset") ) {
						add_offset = att.getNumericValue().doubleValue();
					} else if( att.getName().equals("node_offset") ) {
						node_offset = att.getNumericValue().intValue();
					}
				}
				z = (float[])v.read().copyTo1DJavaArray();
				if( add_offset!=0. || scaleFactor!=1. ) {
					for( int k=0 ; k<z.length ; k++) {
						if( Float.isNaN(z[k]) )continue;
//						double tmp = add_offset+z[k]*scaleFactor;
						double tmp = z[k];
						z[k] = (float)tmp;
					}
				}
			}
		}
		nc.close();
		if( node_offset==1 ) {
			x_range[0] += .5*spacing[0];
			x_range[1] -= .5*spacing[0];
			y_range[0] += .5*spacing[1];
			y_range[1] -= .5*spacing[1];
		}
		if( proj==null ) {
			proj = new RectangularProjection(
				new double[] { x_range[0], x_range[1], y_range[0], y_range[1] },
				dimension[0], dimension[1]);
		}
		int index = fileName.lastIndexOf(".");
		File infoFile = new File( fileName+".info");
	//	System.out.println( infoFile.getName() );
		if( infoFile.exists() ) {
			try {
				BufferedReader in = new BufferedReader(
					new FileReader( infoFile ));
				String s;
				while( (s=in.readLine())!=null ) {
					StringTokenizer st = new StringTokenizer(s);
					if( st.nextToken().equals("UTM_Zone_Number") ) {
						int zone = Integer.parseInt(st.nextToken());
						int NS = UTM.NORTH;
						if( zone<0 ) {
							zone = -zone;
							NS = UTM.SOUTH;
						}
						double dx = (x_range[1]-x_range[0])/(dimension[0]-1.);
						double dy = (y_range[1]-y_range[0])/(dimension[1]-1.);
						proj = new UTMProjection( x_range[0], y_range[1],
							dx, dy, zone, 2, NS );
						break;
					}
				}
				in.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		Grid2D.Float grid = new Grid2D.Float( new java.awt.Rectangle(0, 0, dimension[0], dimension[1]),
					proj);
		grid.setBuffer( z );
		return grid;
	}
	public static void main(String[] args) {
		if(args.length != 1) {
			System.out.println("usage: java Grd filename");
			System.exit(0);
		}
		try {
			Grid2D.Float grd = Grd.readGrd( args[0] );
			java.awt.Rectangle r = grd.getBounds();
			System.out.println( r.x +"\t"+ r.y +"\t"+ r.width +"\t"+ r.height);
			double[] range = grd.getRange();
			System.out.println( range[0] +"\t"+ range[1]);
			MapProjection proj = grd.getProjection();
			Point2D ul = proj.getRefXY(new java.awt.Point(0,0));
			Point2D lr = proj.getRefXY(new java.awt.Point(r.width-1,r.height-1));
			System.out.println( ul.getX() +"\t"+ lr.getX() +"\t"+ lr.getY() +"\t"+ ul.getY());
		} catch(IOException ex) {
			ex.printStackTrace();
		}
	}
}
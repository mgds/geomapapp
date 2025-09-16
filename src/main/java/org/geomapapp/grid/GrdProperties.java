package org.geomapapp.grid;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.geom.RectangularProjection;

import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class GrdProperties {
	public String file;
	public Vector<Object[]> props;
	public double[] x_range;
	public double[] y_range;
	public double[] z_range;
	public double[] spacing;
	public int[] dimension;
	public double scaleFactor=1.;
	public double add_offset=0.;
	public int node_offset=0;

//	***** GMA 1.6.4: TESTING
	public boolean coardsCompliant = false;
	public double fillValue = Double.NaN;
	public List<?> coardsList;
//	***** GMA 1.6.4

	public GrdProperties(String fileName) throws IOException {
		this(fileName, false);
	}
	
	public GrdProperties(String fileName, Boolean flipGrid) throws IOException {
		file = fileName;
		NetcdfFile nc = null;
		try {
			nc = NetcdfFile.open(fileName);
		} catch(java.lang.IllegalArgumentException ex) {
			IOException e = new IOException("Not a netcdf file");
			e.fillInStackTrace();
			throw e;
		}
		props = new Vector<Object[]>();
		Vector<String[]> global = new Vector<String[]>();

		// Check to see if the file is COARDS complaint
		coardsList = nc.getGlobalAttributes();
		Iterator<?> coardsIterator = coardsList.iterator();
		if ( coardsIterator.hasNext() ) {
			Attribute coardsAttribute = (Attribute)coardsIterator.next();
			if ( coardsAttribute.isString() ) {
				//System.out.println(coardsAttribute.toString());
				if ( coardsAttribute.getStringValue().toLowerCase().indexOf("coards") != -1 ) {
					System.out.println("COARDS compliant. " + coardsAttribute.toString());
					coardsCompliant = true;
				}
			}
		}
		
		if (flipGrid) coardsCompliant = !coardsCompliant;
		
		if ( coardsCompliant ) {
			process(nc);
			nc.close();
			return;
		}
//		***** GMA 1.6.4

//		***** GMA 1.6.4: TESTING
//		Iterator vi = nc.getGlobalAttributeIterator();
		List<?> globalList = nc.getGlobalAttributes();
		Iterator<?> vi = globalList.iterator();
//		***** GMA 1.6.4

		while(vi.hasNext()) {
			Attribute v = (Attribute) vi.next();

//			***** GMA 1.6.4: TESTING
//			System.out.println("Next global attribute: " + v.getStringValue());
//			***** GMA 1.6.4

			if (v.getStringValue() != null) {
				String value = getPrintable( v.getStringValue() );
				global.add( new String[] {v.getName(), value } );
			}
		//	byte[] b = v.getStringValue().getBytes();
		//	StringBuffer sb = new StringBuffer();
		//	for( int k=0 ; k<b.length ; k++) {
		//		if( b[k]>31&&b[k]<0x7f ) sb.append((char)b[k]);
		//		if( b[k]==9 || b[k]==10 ) sb.append((char)b[k]);
		//	}
		//	global.add( new String[] {v.getName(), sb.toString() } );
		//	if( v.getName().equals("source") ) {
		//		javax.swing.JOptionPane.showMessageDialog( MapApp.anchor, new javax.swing.JTextArea(v.getStringValue()));
		//		byte[] b = v.getStringValue().getBytes();
		//		for( int k=0 ; k<b.length ; k++) {
		//			if( b[k]>31&&b[k]<0x7e )System.out.println(b[k]+"\t"+(char)b[k]);
		//			else System.out.println(b[k]);
		//		}
		//		System.out.println( v.getStringValue().length() );
		//	}
		}
		props.add( new Object[] {"global", global} );
		Vector<Object[]> variables = new Vector<Object[]>();
		props.add( new Object[] {"variables", variables});

//		***** GMA 1.6.4: TESTING
//		vi = nc.getVariableIterator();
		List<?> variableList = nc.getVariables();
		vi = variableList.iterator();
//		***** GMA 1.6.4

		while(vi.hasNext()) {
			Vector<Object[]> var = new Vector<Object[]>();
			Variable v = (Variable) vi.next();
//			System.out.println("Variable description: " + v.getDescription());

			variables.add( new Object[] {v.getFullName(), var} );
			int[] dims = v.getShape();

//			***** GMA 1.6.4: TESTING
//			Iterator vi2  = v.getAttributeIterator();
			List<?> attributeList = v.getAttributes();
			Iterator<?> vi2 = attributeList.iterator();
//			***** GMA 1.6.4

			while(vi2.hasNext()) {
				Attribute a = (Attribute) vi2.next();
				if( a.isString() ) {
//					System.out.println("a.getName(): " + a.getName());
//					System.out.println("getPrintable(a.getStringValue()): " + getPrintable(a.getStringValue()));
					var.add( new String[] {a.getName(), getPrintable(a.getStringValue())});
				}
			}

			if( v.getFullName().equals( "x_range" )) {
				x_range = (double[])v.read().copyTo1DJavaArray();
				var.add( new Object[] {"x_min", x_range[0]+""});
				var.add( new Object[] {"x_max", x_range[1]+""});
			} else if( v.getFullName().equals( "y_range" )) {
				y_range = (double[])v.read().copyTo1DJavaArray();
				var.add( new Object[] {"y_min", y_range[0]+""});
				var.add( new Object[] {"y_max", y_range[1]+""});
			} else if( v.getFullName().equals( "z_range" )) {
				z_range = (double[])v.read().copyTo1DJavaArray();
				var.add( new Object[] {"z_min", z_range[0]+""});
				var.add( new Object[] {"z_max", z_range[1]+""});
			} else if( v.getFullName().equals( "spacing" )) {
				spacing = (double[])v.read().copyTo1DJavaArray();
				var.add( new Object[] {"x_incr", spacing[0]+""});
				var.add( new Object[] {"y_incr", spacing[1]+""});
			} else if( v.getFullName().equals( "dimension" )) {
				dimension = (int[])v.read().copyTo1DJavaArray();
				var.add( new Object[] {"width", dimension[0]+""});
				var.add( new Object[] {"height", dimension[1]+""});
			} else if( v.getFullName().equals( "z" )) {

//				***** GMA 1.6.4: TESTING
//				Iterator it = v.getAttributeIterator();
				List<?> attributeList2 = v.getAttributes();
				Iterator<?> it = attributeList2.iterator();
//				***** GMA 1.6.4

				while(it.hasNext()) {
					Attribute att = (Attribute) it.next();
					if( att.getName().equals("scale_factor") ) {
						scaleFactor = att.getNumericValue().doubleValue();
						var.add( new Object[] {"scale_factor", scaleFactor+""} );
					} else if( att.getName().equals("add_offset") ) {
						add_offset = att.getNumericValue().doubleValue();
						var.add( new Object[] {"add_offset", add_offset+""} );
					} else if( att.getName().equals("node_offset") ) {
						node_offset = att.getNumericValue().intValue();
						var.add( new Object[] {"node_offset", node_offset+""} );
					}
				}
			}
		}
		
		// if no x,y or z-range, try the coardsCompliant process just in case there was an error in the header
		if (x_range == null || y_range == null || z_range == null ) {
			try {
				process(nc);
			} catch(Exception ex) {}
		}
		
//		System.out.println("Dimension: " + dimension[0] + " " + dimension[1]);
//		System.out.println("Spacing: " + spacing[0] + " " + spacing[1]);
//		System.out.println("x_range[0]: " + x_range[0] + " x_range[1]: " + x_range[1] + " y_range[0]: " + y_range[0] + " y_range[1]: " + y_range[1] + " z_range[0]: " + z_range[0] + " z_range[1]: " + z_range[1]);
		nc.close();
	}

//	***** GMA 1.6.4: TESTING

	public void process( NetcdfFile ncfile ) {

		x_range = new double[2];
		y_range = new double[2];
		z_range = new double[2];
		spacing = new double[2];

		List<?> dimensionList = ncfile.getDimensions();
		Iterator<?> dimensionListIterator = dimensionList.iterator();
		Vector<String> tempDimensions = new Vector<String>();
		int i = 0;
		while ( dimensionListIterator.hasNext() ) {
			String s = ((Dimension)dimensionListIterator.next()).toString();
			if (s.contains("grid_mapping")) continue;
			tempDimensions.add( s.substring( s.indexOf("= ") + 2, s.indexOf(";") ) );
			i++;
		}
		i = 0;
		dimension = new int[tempDimensions.size()];
		while ( i < tempDimensions.size() ) {
			dimension[i] = Integer.parseInt( (String)(tempDimensions.get(i)) );
			i++;
		}

		List<?> variableList = ncfile.getVariables();
		Iterator<?> variableListIterator = variableList.iterator();
		while ( variableListIterator.hasNext() ) {
			Variable v = (Variable)variableListIterator.next();
			List<?> variableAttributeList = v.getAttributes();
			Iterator<?> variableAttributeListIterator = variableAttributeList.iterator();
			while ( variableAttributeListIterator.hasNext() ) {
				Attribute variableAttribute = (Attribute)variableAttributeListIterator.next();
				if ( variableAttribute.getName().toLowerCase().indexOf("fillvalue") != -1 ) {
					fillValue = variableAttribute.getNumericValue().doubleValue();
				}
				else if ( variableAttribute.getName().indexOf("actual_range") != -1 ) {
					Array variableAttributeArray = variableAttribute.getValues();
					IndexIterator ii = variableAttributeArray.getIndexIterator();
					int z_range_index = 0;
					int y_range_index = 0;
					int x_range_index = 0;
					while ( ii.hasNext() ) {
						double variableAttributeArrayValue = ii.getDoubleNext();
						if ( v.getFullName().equals("z") || v.getFullName().equals("Variables/float z(y,x)") ) {
							z_range[z_range_index] = variableAttributeArrayValue;
							z_range_index++;
						}
						else if ( v.getFullName().equals("y") || v.getFullName().equals("Dimensions/y") || v.getFullName().contains("lat")) {
//							System.out.println(variableAttributeArrayValue);
							y_range[y_range_index] = variableAttributeArrayValue;
							y_range_index++;
						}
						else if ( v.getFullName().equals("x") || v.getFullName().equals("Dimensions/x") || v.getFullName().contains("lon")) {
							x_range[x_range_index] = variableAttributeArrayValue;
							x_range_index++;
						}
					}
				}
			}
		}

		List<?> globalAttributeList = ncfile.getGlobalAttributes();
		Iterator<?> globalAttributeListIterator = globalAttributeList.iterator();
		// Checks if node_offset is present before reading. This is new in GMT 4 grids.
		Attribute globalAttribute = ncfile.findGlobalAttribute("node_offset");
		if(globalAttribute != null) {
			node_offset = globalAttribute.getNumericValue().intValue();
			System.out.println("node_offset: " + node_offset);
		}
//		globalAttribute = ncfile.findGlobalAttribute("scale_factor");
//		if ( globalAttribute != null ) {
//			System.out.println(globalAttribute.getName());
//		}
//		System.out.println(node_offset);
//		while ( globalAttributeListIterator.hasNext() ) {
//			globalAttribute = (Attribute)globalAttributeListIterator.next();
//			if ( globalAttribute.getName() != null ) {
//				System.out.println(globalAttribute.getName());
//			}
//		}
		spacing[0] = Math.abs( ( x_range[1] - x_range[0] )  / ( dimension[0] - 1 ) );
		spacing[1] = Math.abs( ( y_range[1] - y_range[0] )  / ( dimension[1] - 1 ) );
//		System.out.println("Dimension: " + dimension[0] + " " + dimension[1]);
//		System.out.println("Spacing: " + spacing[0] + " " + spacing[1]);
//		System.out.println("x_range[0]: " + x_range[0] + " x_range[1]: " + x_range[1] + " y_range[0]: " + y_range[0] + " y_range[1]: " + y_range[1] + " z_range[0]: " + z_range[0] + " z_range[1]: " + z_range[1]);		
//		The COARDS convention has the z-values written in the opposite row order from GMT-3 To
	}
//	***** GMA 1.6.4

	public static String[] getHeader( String fileName) throws IOException {
		//String file1 = fileName;
		NetcdfFile ncFile = null;
		String[] attributeNetCDF;
		try {
			ncFile = NetcdfFile.open(fileName);
		} catch(java.lang.IllegalArgumentException ex) {
			IOException e = new IOException("Not a netcdf file");
			e.fillInStackTrace();
			throw e;
		}
		List<?> globalAttList = ncFile.getGlobalAttributes();
		attributeNetCDF = new String[globalAttList.size()];

		for(int i=0; i < globalAttList.size(); i++ ) {
			attributeNetCDF[i] = globalAttList.get(i).toString();
		}
		return attributeNetCDF;
	}

	public MapProjection getProjection() {
		double[] x_range = new double[] {this.x_range[0], this.x_range[1]};
		double[] y_range = new double[] {this.y_range[0], this.y_range[1]};
		if( node_offset==1 ) {
			x_range[0] += .5*spacing[0];
			x_range[1] -= .5*spacing[0];
			y_range[0] += .5*spacing[1];
			y_range[1] -= .5*spacing[1];
		}
		return new RectangularProjection(
				new double[] { x_range[0], x_range[1], y_range[0], y_range[1] },
				dimension[0], dimension[1]);
	}

	public static String getPrintable( String s ) {
		byte[] b = s.getBytes();
		StringBuffer sb = new StringBuffer();
		for( int k=0 ; k<b.length ; k++) {
			if( b[k]>31&&b[k]<0x7f ) sb.append((char)b[k]);
			else if( b[k]==9 || b[k]==10 ) sb.append((char)b[k]);
		}
		return sb.toString();
	}
}
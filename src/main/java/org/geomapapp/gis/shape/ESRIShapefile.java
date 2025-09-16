package org.geomapapp.gis.shape;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.tree.TreeNode;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.grid.Grid2DOverlay;
import org.geomapapp.grid.MultiGrid;
import org.geomapapp.image.MultiImage;
import org.geomapapp.io.LittleIO;
import org.geomapapp.util.ParseLink;
import org.geomapapp.util.ScalableComponent;

import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.util.URLFactory;
import haxby.util.WESNSupplier;
import haxby.util.XBTable;

public class ESRIShapefile extends java.awt.geom.Rectangle2D.Double 
				implements haxby.map.Overlay, TreeNode, WESNSupplier {
			//	implements ESRIShape {
	public final static int NULL = 0;
	public final static int POINT = 1;
	public final static int POINT_M = 21;
	public final static int POINT_Z = 11;
	public final static int MULTIPOINT = 8;
	public final static int MULTIPOINT_M = 28;
	public final static int MULTIPOINT_Z = 18;
	public final static int POLYLINE = 3;
	public final static int POLYLINE_M = 23;
	public final static int POLYLINE_Z = 13;
	public final static int POLYGON = 5;
	double nodata = -2.e38;
	Color defaultOutline;
	Color defaultFill;
	Color defaultSelCol;
	LineWidth defaultLW;
	String path;
	String filename;
	MainHeader header;
	Vector shapes;
	
//	GMA 1.4.8: Include suite so that it can be accessed by classes that use this class
	public ShapeSuite suite;
	
	ESRIShapefile parent;
	Vector children;
	DBFFile dbfFile;
	Vector properties;
	boolean visible = true;
	Vector selected;
	XMap map;
	XBTable table;
	int selectedIndex = -1;
	boolean disposed=false;
	String linkType;
	String infoURL;
	
	MultiGrid multiGrid;
	MultiImage multiImage;
	
	static String units = "m";
	static String dataType= "Elevation";
	
	private String gridUnits = "";
	private String gridDataType = "";
	
	float[] wesn = null;
	
	public ESRIShapefile(String name, int type, Vector names, Vector classes) {
		filename = name;
		dbfFile = new DBFFile(names, classes);
		header = new MainHeader();
		header.type = type;
		shapes = new Vector();
		children = new Vector();
		initColors();
	}
	public ESRIShapefile( ZipInputStream zip ) throws IOException {
		filename = null;
		boolean hasDBF = false;
		boolean hasSHP = false;
		ZipEntry entry = zip.getNextEntry();
		while( zip.available()!=0 ) {
			String name = entry.getName();
		System.out.println( name );
			if( name.endsWith(".dbf") || name.endsWith(".shp") ) {
				filename = name.substring( 0, name.lastIndexOf(".") );
			} else if( !name.endsWith(".link") ) {
			//	zip.closeEntry();
				entry = zip.getNextEntry();
				continue;
			}
			int size = (int)entry.getSize();
			byte[] buffer = new byte[size];
			int off = 0;
			int len=size;
			while( (len = zip.read(buffer, off, size-off)) < size-off )off+=len;
			ByteArrayInputStream in = new ByteArrayInputStream(buffer);
			if( name.endsWith(".dbf") ) {
				hasDBF = true;
				dbfFile = new DBFFile( in );
			} else if(name.endsWith(".shp")) {
				shapes = new Vector();
				hasSHP = true;
				readShapes(in);
			} else if(name.endsWith(".link")) {
				readProperties(in);
			}
			entry = zip.getNextEntry();
		//	zip.closeEntry();
		}
		zip.close();
		selected = new Vector();
		initColors();
		if( hasDBF && hasSHP )return;
		else throw new IOException("insufficient information");
	}

	public ESRIShapefile( String path, String fileprefix ) throws IOException {
		super();
		this.path = path;
		this.filename = fileprefix;
		if( !exists() )  {
			throw new IOException((new File(path,fileprefix)).getPath() + " does not exist");
		}
		filename = fileprefix;
		shapes = new Vector();
		readShapes();
		readUnits();
	//	dbfFile = new DBFFile( path, fileprefix, properties);
		dbfFile = new DBFFile( path, fileprefix);
		selected = new Vector();
		initColors();
	}
	public void dispose() {
		disposed = true;
		shapes = new Vector(0);
		selected = new Vector(0);
		children = new Vector(0);
		properties = null;
		dbfFile.records = new Vector();
		if( multiGrid!=null) multiGrid.dispose();
		if (multiImage != null) multiImage.dispose();
		
		if (map != null)
			map.removeOverlay(this);
	}
	public void setParent( ESRIShapefile parent ) {
		this.parent = parent;
	}
	public void addShape( ESRIShape shape, Vector record) {
		shapes.add( shape );
		dbfFile.addRecord( record );
		getTable();
		dbfFile.fireTableStructureChanged();
	}
	public void clear() {
		shapes = new Vector();
		dbfFile.records = new Vector();
	}
	public Object clone() {
		ESRIShapefile shp = new ESRIShapefile( getName(), header.type, dbfFile.names, dbfFile.classes);
		for( int k=0 ; k<shapes.size() ; k++) {
			shp.addShape( (ESRIShape)shapes.get(k), (Vector)dbfFile.records.get(k) );
		}
		return shp;
	}
	public ESRIShapefile subset( int[] indices ) {
		ESRIShapefile shp = new ESRIShapefile( getName()+".1", header.type, dbfFile.names, dbfFile.classes);
		for( int k=0 ; k<indices.length ; k++) {
			shp.addShape( (ESRIShape)shapes.get(indices[k]), (Vector)dbfFile.records.get(indices[k]) );
		}
		return shp;
	}
	public void removeSelectedObject() {
		int index = getTable().getSelectedRow();
		if( index<0 )return;
		shapes.remove( index);
		dbfFile.getRecords().remove(index);
		dbfFile.fireTableStructureChanged();
		map.repaint();
	}
	public int size() {
		return shapes.size();
	}
	public boolean exists() {
		if( path.startsWith( "http" )) return true;
		if( path.startsWith( "file://" )) return true;
		if( !(new File(path,filename+".shp")).exists() ) return false;
		if( !(new File(path,filename+".dbf")).exists() ) return false;
	//	if( !(new File(path,filename+".shx")).exists() ) return false;
		return true;
	}
	public Vector getShapes() {
	//	if( shapes.size()==0 ) {
	//		try {
	//			shapes = readShapes();
	//		} catch(IOException ex) {
	//		}
	//	}
		return shapes;
	}
	public int getSelectedIndex( NearNeighbor n ) {
		int k=selectedIndex;
		for( int i=0 ; i<shapes.size() ; i++) {
			k=(k+1)%shapes.size();
			ESRIShape shape = (ESRIShape)shapes.get(k);
			if( shape.getType()==0 )continue;
			if( shape.select(n, map).shape==shape ) {
				selectedIndex = k;
				return k;
			}
		}
		selectedIndex = -1;
		return -1;
	}
	public File getShapeFile() {
		return new File( path, filename );
	}
	public void writeShapes(File file) throws IOException {
		if( map!=null ) {
			writeShapes(file, map.getProjection());
		} else {
			writeShapes(file, new org.geomapapp.geom.IdentityProjection());
		}
	}
	public void writeShapes(File file, MapProjection proj) throws IOException {
		String path = file.getParent();
		String name = file.getName();
		if( name.endsWith(".shp") ) name=name.substring(0, name.indexOf(".shp"));
		dbfFile.write(path, name);
		file = new File( path, name+".shp");
		RandomAccessFile shp = new RandomAccessFile(file, "rw");
		shp.writeInt(9994);
		for( int k=0 ; k<6 ; k++ ) shp.writeInt(0);
		LittleIO.writeInt( 1000, shp );
		LittleIO.writeInt( getType(), shp );
		shp.seek(100L);
		double[][] bounds = new double[4][];
		int[][] offlen = new int[shapes.size()][2];
		for( int k=0 ; k<shapes.size() ; k++) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			offlen[k][0] = ((int)shp.getFilePointer())/2;
			ESRIShape shape = (ESRIShape)shapes.get(k);
			if( shape.getType()==0 ) {
				shp.writeInt(k+1);
				shp.writeInt( 2 );
				shp.writeInt(0);
				continue;
			}
			if( proj!=null ) {
				if( shape instanceof ESRIMultiPoint) {
					ESRIMultiPoint mp = (ESRIMultiPoint)shape;
					bounds = shape.inverse(proj, bounds);
				} else {
					ESRIPoint p = (ESRIPoint)shape;
					bounds = shape.inverse(proj, bounds);
				}
			}
			int recLen = shape.writeShape(out);
			byte[] data = out.toByteArray();
//	System.out.println( k +"\t"+ recLen +"\t"+ data.length);
			shp.writeInt(k+1);
			shp.writeInt( data.length/2 + 2 );
			LittleIO.writeInt( shape.getType(), shp );
			shp.write( data );
			offlen[k][1] = data.length/2 + 2;
		}
		int length = ((int)shp.getFilePointer())/2;
		shp.seek( 24L);
		shp.writeInt( length );
		shp.seek( 36L );
		double[] range = new double[] { 0., 0.,};
		double[] xRange = bounds[0];
		double[] yRange = bounds[1];
		x = xRange[0];
		width = xRange[1]-x;
		y = yRange[0];
		height = yRange[1]-y;
		LittleIO.writeDouble( xRange[0], shp);
		LittleIO.writeDouble( yRange[0], shp);
		LittleIO.writeDouble( xRange[1], shp);
		LittleIO.writeDouble( yRange[1], shp);
		for( int k=2 ; k<4 ; k++) {
			double[] r = bounds[k];
			if( r==null ) r=range;
			LittleIO.writeDouble( r[0], shp);
			LittleIO.writeDouble( r[1], shp);
		}
		shp.close();

		file = new File( path, name+".shx");
		RandomAccessFile shx = new RandomAccessFile(file, "rw");
		shx.writeInt(9994);
		for( int k=0 ; k<5 ; k++ ) shx.writeInt(0);
		shx.writeInt( 50+4*offlen.length );
		LittleIO.writeInt( 1000, shx );
		LittleIO.writeInt( getType(), shx );
		LittleIO.writeDouble( xRange[0], shx);
		LittleIO.writeDouble( yRange[0], shx);
		LittleIO.writeDouble( xRange[1], shx);
		LittleIO.writeDouble( yRange[1], shx);

		for( int k=2 ; k<4 ; k++) {
			double[] r = bounds[k];
			if( r==null ) r=range;
			LittleIO.writeDouble( r[0], shx);
			LittleIO.writeDouble( r[1], shx);
		}
		for( int k=0 ; k<offlen.length ; k++) {
			shx.writeInt(offlen[k][0]);
			shx.writeInt(offlen[k][1]);
		}
		shx.close();
		if( map!=null)forward(proj, map.getWrap());
	}
	public NearNeighbor select( NearNeighbor n ) {
		for( int k=0 ; k<shapes.size() ; k++) {
			ESRIShape shape = (ESRIShape)shapes.get(k);
			if( shape.getType()==0 )continue;
			if( shape.select(n, map).shape==shape ) return n;
		}
		return n;
	}
	public DBFFile getDBFFile() {
		return dbfFile;
	}
	public XBTable getTable() {
		if( table==null ) {
			table = new XBTable(dbfFile);
		}
		return table;
	}
	public String getName() {
		return filename;
	}
	public void setName( String name) {
		filename = name;
	}
	public int getType() {
		if( header==null ) return 0;
		return header.type;
	}
	public void setVisible( boolean tf ) {
		if( visible==tf )return;
		visible = tf;
		map.repaint();
		((MapApp)map.getApp()).layerManager.setLayerVisible(this,visible);
	}
	public boolean isVisible() {
		return visible;
	}
	
//	***** GMA 1.6.6: Add functions to get, set and read units
	
	public static String getUnits() {
		return units;
	}
	
	//non static version that returns units for this grid
	public String getGridUnits() {
		return gridUnits;
	}
	
	public void setUnits( String inputUnits ) {
		units = inputUnits;
		gridUnits = inputUnits;
	}
	
	public static void nullUnits() {
		units = null;
	}
	
	
	public void readUnits(String path, String filename) throws FileNotFoundException {
		if( !exists() ) {
			throw new FileNotFoundException();
		}
		boolean url = path.startsWith( "http" ) || path.startsWith( "file://" );
		if( url&& !path.endsWith("/") ) {
			path += "/";
		}
		try {
			InputStream in = url ? (haxby.util.URLFactory.url(path+filename)).openStream() : new FileInputStream(new File(path, filename));
			BufferedReader reader = null;
			try {
				reader = new BufferedReader( new InputStreamReader(in) );
				
				String p =reader.readLine();
				if(p !=null){
					setDataType(p.trim());
					gridDataType = p.trim();
				}
				
				String u = reader.readLine();
				setUnits(u.trim());	
				reader.close();
			} catch(IOException e) {
				try {
					setUnits("m");
					setDataType("Elevation");
					in.close();
					reader.close();
				} catch(Exception ex) {
				}
			}
			in.close();
		} catch(IOException e) {
			setUnits("m");
			setDataType("Elevation");
		}
		System.gc();
	}
	public void readUnits() throws FileNotFoundException, MalformedURLException, IOException {
		if( !exists() ) {
			throw new FileNotFoundException();
		}
		boolean url = path.startsWith( "http" ) || path.startsWith( "file://" );
		if( url&& !path.endsWith("/") ) {
			path += "/";
		}
		URL unitsUrl = null;
		File unitsFile = null;
		//sometimes the units file is filename.units.txt, and sometimes
		//it is just units.txt.  Look for the first. If not found, try the
		//latter option.
		if (url) {
			unitsUrl = URLFactory.url(path+filename+".units.txt");
			if (unitsUrl.getProtocol().equals("http")) {
				HttpURLConnection huc = (HttpURLConnection)  unitsUrl.openConnection();
				huc.setRequestMethod("HEAD");
				if (huc.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
					unitsUrl = URLFactory.url(path+"units.txt");
				}
			} else if (unitsUrl.getProtocol().equals("file")) {
				unitsUrl = URLFactory.url(path+"units.txt");
			}		
		} else {
			unitsFile = new File(path, filename+".units.txt");
			if (!unitsFile.exists()) {
				unitsFile = new File(path, "units.txt");
			}
		}
					
		try {
			InputStream in = url ? (unitsUrl).openStream() : new FileInputStream(unitsFile);
			BufferedReader reader = null;
			try {
				reader = new BufferedReader( new InputStreamReader(in) );
				
				String p =reader.readLine();
				if(p !=null){
					setDataType(p.trim());
				}
				
				String u = reader.readLine();
				setUnits(u.trim());
				reader.close();
			} catch(IOException e) {
				try {
					in.close();
					reader.close();
				} catch(Exception ex) {
				}
			}
			in.close();
		} catch(IOException e) {
			setUnits("m");
			setDataType("Elevation");
		}
	}
//	***** GMA 1.6.6
	
	public static String getDataType() {
		return dataType;
	}
	
	//non-static version that returns datatype for this grid
	public String getGridDataType() {
		return gridDataType;
	}
	
	public void setDataType( String inputDataType ) {
		dataType = inputDataType ;
		gridDataType = inputDataType;
	}
	
	void initColors() {
		defaultLW = new LineWidth(1f);
		int t = getType()%10;
		if( t==3 ) defaultFill=null;
		else defaultFill = Color.lightGray;
		defaultOutline = Color.black;
		defaultSelCol = Color.white;
	}
	public void setDefaultFill(Color c) {
		if( getType()%10 == 3 )return;
	}
	public Color getDefaultFill() {
		return defaultFill;
	}
	public void setDefaultOutline(Color c) {
		defaultOutline = c;
	}
	public Color getDefaultOutline() {
		return defaultOutline;
	}
	public void setDefaultSelectionColor(Color c) {
		defaultSelCol = c;
	}
	public Color getDefaultSelectionColor() {
		return defaultSelCol;
	}
	public void setDefaultLW(LineWidth lw) {
		defaultLW = lw;
	//	map.repaint();
	}
	public LineWidth getDefaultLW() {
		return defaultLW;
	}
	public void sort( int col, boolean inverse ) {
		int[] order = dbfFile.sort(col, inverse);
		Vector shps = new Vector( order.length );
		for( int k=0 ; k<order.length ; k++) shps.add(shapes.get(order[k]));
		shapes = shps;
	}
	public Vector getProperties() {
		return properties;
	}
	public void openGridDialog() {
		if( multiGrid==null )return;
		multiGrid.showDialog();
	}
	public MultiGrid getMultiGrid() {
		return multiGrid;
	}
	
	public MultiImage getMultiImage() {
		return multiImage;
	}
	
	public void readProperties( InputStream in ) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(
					new InputStreamReader(in));
			properties = ParseLink.parse( reader, null);
			Vector props = (Vector)ParseLink.getProperty( properties, "data");
			linkType = (String)ParseLink.getProperty( props, "type");
			if( linkType.equals("tiled_grid") ) {
				double xmin = java.lang.Double.parseDouble( (String)ParseLink.getProperty( props, "x_min" ));
				double xmax = java.lang.Double.parseDouble( (String)ParseLink.getProperty( props, "x_max" ));
				double ymin = java.lang.Double.parseDouble( (String)ParseLink.getProperty( props, "y_min" ));
				double ymax = java.lang.Double.parseDouble( (String)ParseLink.getProperty( props, "y_max" ));
				double zmin = java.lang.Double.parseDouble( (String)ParseLink.getProperty( props, "z_min" ));
				double zmax = java.lang.Double.parseDouble( (String)ParseLink.getProperty( props, "z_max" ));
				int minRes = Integer.parseInt( (String)ParseLink.getProperty( props, "res_min" ));
				int maxRes = Integer.parseInt( (String)ParseLink.getProperty( props, "res_max" ));
				String background = (String)ParseLink.getProperty( props, "background" );
				
				int backgroundI;
				if (background != null)
					backgroundI = (int) Long.parseLong(background, 16);
				else 
					backgroundI = 0;
				
				multiGrid = new MultiGrid( minRes, maxRes,
						new Rectangle2D.Double( xmin,ymin,xmax-xmin,ymax-ymin ),
						new double[] {zmin, zmax},
						path.endsWith("/") ? path : path + "/",
//						(String)ParseLink.getProperty(props, "url"),
						this,
						backgroundI);
			} else if (linkType.equals("tiled_images"))
			{
				double xmin = java.lang.Double.parseDouble( (String)ParseLink.getProperty( props, "x_min" ));
				double xmax = java.lang.Double.parseDouble( (String)ParseLink.getProperty( props, "x_max" ));
				double ymin = java.lang.Double.parseDouble( (String)ParseLink.getProperty( props, "y_min" ));
				double ymax = java.lang.Double.parseDouble( (String)ParseLink.getProperty( props, "y_max" ));
				int minRes = Integer.parseInt( (String)ParseLink.getProperty( props, "res_min" ));
				int maxRes = Integer.parseInt( (String)ParseLink.getProperty( props, "res_max" ));
				int mapType = Integer.parseInt( (String)ParseLink.getProperty( props, "map_type" ));
				multiImage = new MultiImage( minRes, maxRes,
						new Rectangle2D.Double( xmin,ymin,xmax-xmin,ymax-ymin ),
						mapType,
						(String)ParseLink.getProperty(props, "image_type"),
						(String)ParseLink.getProperty(props, "url"),
						this );
			}
			reader.close();
		} catch(IOException e) {
			e.printStackTrace();
			properties = null;
			try {
				reader.close();
			} catch(Exception ex) {
			}
		}
	}
	public Vector readShapes() throws IOException {
		if( !exists() ) throw new FileNotFoundException();
		boolean url = path.startsWith( "http" ) || path.startsWith( "file://" );
		if( url&& !path.endsWith("/") ) path += "/";
		try {
			InputStream in = url ?
				(haxby.util.URLFactory.url(path+filename+".link")).openStream()
				: new FileInputStream(new File(path, filename+".link"));
			readProperties(in);
		} catch(IOException e) {
			properties = null;
		}
		InputStream in = url ?
			(haxby.util.URLFactory.url(path+filename+".shp")).openStream()
			: new FileInputStream(new File(path, filename+".shp"));
		readShapes( in );
		return shapes;
	}
	public void readShapes(InputStream in) throws IOException {
		DataInputStream shp = new DataInputStream(
				new BufferedInputStream(in));
		header = MainHeader.getHeader(shp);
		int t = header.type%10;
	//	if( header.type-t == 10 || header.type==31 )
		if( header.type==31 )
			throw new IOException("unsupported shape type: "+header.type);
		x = header.xBounds[0];
		width = header.xBounds[1]-x;
		y = header.yBounds[0];
		height = header.xBounds[1]-y;
		int offset = 100;
		while( offset<2*header.length-1 ) {
			int n = shp.readInt();
			int len = shp.readInt();
			offset+=8+len*2;
			len *=2;
			int type = LittleIO.readInt(shp);
			if( type!=0 && type!=header.type ) throw new IOException("wrong type: "+type);
			if( type==0 ) {
				shapes.add( new ESRINull() );
			} else if( type==1 ) {
				shapes.add(new ESRIPoint(
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp)));
			} else if( type==21 ) {
				shapes.add(new ESRIPointM(
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp)));
			} else if( type==8 ) {
				ESRIMultiPoint obj = new ESRIMultiPoint(
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readInt(shp));
				for( int i=0 ; i<obj.length() ; i++) {
					obj.addPoint( i,
						LittleIO.readDouble(shp),
						LittleIO.readDouble(shp));
				}
				shapes.add( obj );
			} else if( type==28 ) {
				ESRIMultiPointM obj = new ESRIMultiPointM(
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readInt(shp));
				for( int i=0 ; i<obj.length() ; i++) {
					obj.addPoint( i,
						LittleIO.readDouble(shp),
						LittleIO.readDouble(shp));
				}
				obj.setMRange(
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp));
				for( int i=0 ; i<obj.length() ; i++) {
					obj.m[i] = LittleIO.readDouble(shp);
				}
				shapes.add( obj );
			} else if( type==23 ) {
				ESRIPolyLineM obj = new ESRIPolyLineM(
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readInt(shp),
					LittleIO.readInt(shp));
				for( int i=0 ; i<obj.nParts() ; i++) {
					obj.parts[i] = LittleIO.readInt(shp);
				}
				for( int i=0 ; i<obj.length() ; i++) {
					obj.addPoint( i,
						LittleIO.readDouble(shp),
						LittleIO.readDouble(shp));
				}
				obj.setMRange(
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp));
				for( int i=0 ; i<obj.length() ; i++) {
					obj.m[i] = LittleIO.readDouble(shp);
				}
				shapes.add( obj );
			} else if( type==13 ) {
				ESRIPolyLineZ obj = new ESRIPolyLineZ(
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readInt(shp),
					LittleIO.readInt(shp));
				for( int i=0 ; i<obj.nParts() ; i++) {
					obj.parts[i] = LittleIO.readInt(shp);
				}
				for( int i=0 ; i<obj.length() ; i++) {
					obj.addPoint( i,
						LittleIO.readDouble(shp),
						LittleIO.readDouble(shp));
				}
				obj.setZRange(
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp));
				for( int i=0 ; i<obj.length() ; i++) {
					obj.z[i] = LittleIO.readDouble(shp);
				}
				obj.setMRange(
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp));
				for( int i=0 ; i<obj.length() ; i++) {
					obj.m[i] = LittleIO.readDouble(shp);
				}
				shapes.add( obj );
			} else if( type==3 ) {
				ESRIPolyLine obj = new ESRIPolyLine(
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readInt(shp),
					LittleIO.readInt(shp));
				for( int i=0 ; i<obj.nParts() ; i++) {
					obj.parts[i] = LittleIO.readInt(shp);
				}
				for( int i=0 ; i<obj.length() ; i++) {
					obj.addPoint( i,
						LittleIO.readDouble(shp),
						LittleIO.readDouble(shp));
				}
				shapes.add( obj );
			} else if( type==5 ) {
				ESRIPolygon obj = new ESRIPolygon(
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readInt(shp),
					LittleIO.readInt(shp));
				for( int i=0 ; i<obj.nParts() ; i++) {
					obj.parts[i] = LittleIO.readInt(shp);
				}
				for( int i=0 ; i<obj.length() ; i++) {
					obj.addPoint( i,
						LittleIO.readDouble(shp),
						LittleIO.readDouble(shp));
				}
				shapes.add( obj );
			} else if( type==15 ) {
				ESRIPolygonZ obj = new ESRIPolygonZ(
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp),
					LittleIO.readInt(shp),
					LittleIO.readInt(shp));
				shapes.add( obj );
				int ln = 44+obj.nParts()*4+16*obj.length()+16*(obj.length()+2);
				for( int i=0 ; i<obj.nParts() ; i++) {
					obj.parts[i] = LittleIO.readInt(shp);
				}
				for( int i=0 ; i<obj.length() ; i++) {
					obj.addPoint( i,
						LittleIO.readDouble(shp),
						LittleIO.readDouble(shp));
				}
				obj.setZRange(
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp));
				for( int i=0 ; i<obj.length() ; i++) {
					obj.z[i] = LittleIO.readDouble(shp);
				}
				if( len!=0 && ln>len ) continue;
				obj.setMRange(
					LittleIO.readDouble(shp),
					LittleIO.readDouble(shp));
				for( int i=0 ; i<obj.length() ; i++) {
					obj.m[i] = LittleIO.readDouble(shp);
				}
			}
		}
	}
	public void setMap(XMap map) {
		if(map==null)return;
		if(this.map!=null)return;
		this.map = map;
		forward(map.getProjection(), map.getWrap());
		if( multiGrid!=null ) {
			multiGrid.setMap();
		} else if ( multiImage!= null) {
			multiImage.setMap();
		} else {
			// TODO : Polygons and Lines go here.
		}
	}
	public XMap getMap() {
		return map;
	}
	public void forward(MapProjection proj, double wrap) {
		if( shapes==null || shapes.size()==0 )return;
		double[][] bounds = new double[4][];
		for( int k=0 ; k<shapes.size() ; k++) {
			ESRIShape shape = (ESRIShape)shapes.get(k);
			if( shape.getType()==0 )continue;
			try {
				bounds = ((ESRIPoint)shape).forward(proj, wrap, bounds);
			} catch(Exception e) {
				bounds = ((ESRIMultiPoint)shape).forward(proj, wrap, bounds);
			}
		}
		if( bounds[0]==null )return;
		x = bounds[0][0];
		y = bounds[1][0];
		width = bounds[0][1]-x;
		height = bounds[1][1]-y;
	//	System.out.println( x +"\t"+ y +"\t"+ width +"\t"+ height);
	}
	public void drawSelection(java.awt.Graphics2D g, ScalableComponent comp, double wrap) {
		if( disposed || !visible ) return;
		AffineTransform aTrans = comp.getTransform();
		int[] rows = getTable().getSelectedRows();
		Arrays.sort(rows);
		int i=0;
		g.setStroke(new BasicStroke(defaultLW.getLineWidth()/(float)aTrans.getScaleX()));
		Rectangle2D rect = comp.getUnscaledVisibleRect();
		g.setColor(defaultSelCol);
		for( int k=0 ; k<rows.length ; k++) {
			ESRIShape shape = (ESRIShape)shapes.get(rows[k]);
			if( shape.getType()==0 )continue;
			shape.draw(g, rect, wrap);
		}
	}
	public void draw(java.awt.Graphics2D g, ScalableComponent comp, double wrap) {
		if( disposed || !visible ) return;
		AffineTransform aTrans = comp.getTransform();
		
//	System.out.println( aTrans.getScaleX()
//			 +"\t"+ aTrans.getTranslateX() 
//			 +"\t"+ aTrans.getTranslateY() );
		int[] rows = getTable().getSelectedRows();
		Arrays.sort(rows);
		int i=0;
		g.setStroke(new BasicStroke(defaultLW.getLineWidth()/(float)aTrans.getScaleX()));
		g.setColor(defaultOutline);
		Rectangle2D rect = comp.getUnscaledVisibleRect();
		for( int k=0 ; k<shapes.size() ; k++) {
			if( i<rows.length && k==rows[i]) {
				i++;
				continue;
			}
			ESRIShape shape = (ESRIShape)shapes.get(k);
			if( shape.getType()==0 )continue;
			shape.draw(g, rect, wrap);
		}
		g.setColor(defaultSelCol);
		for( int k=0 ; k<rows.length ; k++) {
			ESRIShape shape = (ESRIShape)shapes.get(rows[k]);
			if( shape.getType()==0 )continue;
			shape.draw(g, rect, wrap);
		}
	}
	public void draw(java.awt.Graphics2D g) {
		if( disposed || !visible ) return;
		if( multiGrid!=null ) {
			multiGrid.draw(g);
			return;
		}
		if (multiImage != null) {
			multiImage.draw(g);
			return;
		}
		int[] rows = getTable().getSelectedRows();
		Arrays.sort(rows);
		int i=0;
		g.setStroke(new BasicStroke(defaultLW.getLineWidth()/(float)map.getZoom()));
		g.setColor(defaultOutline);
		Rectangle2D rect = map.getClipRect2D();
		double wrap = map.getWrap();
		for( int k=0 ; k<shapes.size() ; k++) {
			if( i<rows.length && k==rows[i]) {
				i++;
				continue;
			}
			ESRIShape shape = (ESRIShape)shapes.get(k);
			if( shape.getType()==0 )continue;
			shape.draw(g, rect, wrap);
		}
		g.setColor(defaultSelCol);
		for( int k=0 ; k<rows.length ; k++) {
			ESRIShape shape = (ESRIShape)shapes.get(rows[k]);
			if( shape.getType()==0 )continue;
			shape.draw(g, rect, wrap);
		}
	}
	public String toString() {
		return filename;
	}
//
// methods implementing TreeNode
//
	public TreeNode getChildAt(int childIndex) {
		return (TreeNode)children.get(childIndex);
	}
	public int getChildCount() {
		return children.size();
	}
	public TreeNode getParent() {
		return parent;
	}
	public int getIndex(TreeNode node) {
		return children.indexOf( node );
	}
	public boolean getAllowsChildren() {
		return true;
	}
	public boolean isLeaf() {
		return getChildCount()==0;
	}
	public Enumeration children() {
		return children.elements();
	}
	public static void main(String[] args) {
		javax.swing.JFileChooser c = new javax.swing.JFileChooser(
					System.getProperty("user.dir"));
		int ok = c.showOpenDialog(null);
		if( ok==c.CANCEL_OPTION ) System.exit(0);
		try {
			File file = c.getSelectedFile();
			String path = file.getParent();
			String name = file.getName();
			name = name.substring( 0, name.lastIndexOf(".") );
			ESRIShapefile shape = new ESRIShapefile( path, name);
			Vector shapes = shape.getShapes();
			if( shape.getType()!=1 && shape.getType()!=3 ) System.exit(0);
			DBFFile dbf = new DBFFile( path, name);
			XBTable table = new XBTable(dbf);
			JScrollPane sp = new JScrollPane(table);
			JFrame frame = new JFrame(name);
			frame.getContentPane().add(sp);
			frame.pack();
			frame.setVisible(true);
			frame.setDefaultCloseOperation( frame.EXIT_ON_CLOSE );
			for(int k=0 ; k<shapes.size() ; k++) {
				System.out.println( shapes.get(k) );
			}
		} catch(IOException ex) {
			System.out.println( ex.getMessage() );
		}
	//	System.exit(0);
	}
	public void setInfoURL(String infoURL) {
		this.infoURL = infoURL;
	}
	
	public String getInfoURL() {
		return infoURL;
	}
	
	public double[] getWESN() {
		if (wesn != null) return new double[] {wesn[0], wesn[1], wesn[2], wesn[3]};
		return new double[] {header.xBounds[0], header.xBounds[1], 
				header.yBounds[0], header.yBounds[1]};
	}
	public void setWESN(String wesn) {
		if (wesn == null) return;
		String[] wesnS = wesn.split(",");
		if (wesnS.length < 4) return;
		float[] wesnF = new float[4];
		try {
			for (int i = 0; i < wesnS.length; i++)
				wesnF[i] = java.lang.Float.parseFloat(wesnS[i]); 
		} catch (NumberFormatException ex) {return;}
		this.wesn = wesnF;
	}
	
	/*
	 * Override the default Rectangle equals that will return 
	 * true if the x, y, height and width are the same.
	 */
	@Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        // If obj is a Grid2DOverlay, then compare with this.multiGrid.grid
        if (obj instanceof Grid2DOverlay && this.multiGrid != null && obj == this.multiGrid.getGrid2DOverlay()) {
        	return true;
        }
        return false;
	}
        
}

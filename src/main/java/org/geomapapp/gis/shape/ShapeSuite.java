package org.geomapapp.gis.shape;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;

import org.geomapapp.grid.Grid2DOverlay;
import org.geomapapp.grid.ImportGrid;
import org.geomapapp.image.Palette;
import org.geomapapp.image.RenderingTools;
import org.geomapapp.io.ShowStackTrace;
import org.geomapapp.util.ParseLink;
import org.geomapapp.util.XML_Menu;

import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.proj.Mercator;
import haxby.proj.PolarStereo;
import haxby.proj.Projection;
import haxby.util.GeneralUtils;
import haxby.util.UIDTracker;
import haxby.util.XBTable;

public class ShapeSuite extends AbstractTableModel {
	public Vector<ESRIShapefile> shapeFiles;
	public JFileChooser chooser;
	public FileFilter shapeF;
	public Component parent;
	public XMap map;
	public JDialog openDialog;
	public JRadioButton fileB;
	public JRadioButton urlB;
	public JRadioButton listB;
	public JRadioButton gridB;
	public LayerModel layers;
	public JTree layerTree;

	// GMA 1.4.8: To be used and accessed through this class by other classes.
	public ViewShapes viewShapes;
	
	public ShapeSuite() {
		shapeFiles = new Vector<ESRIShapefile>();
	}

	public Vector<ESRIShapefile> getShapes() {
		return shapeFiles;
	}

	public boolean addShapeFile( ESRIShapefile shape ) {
		for (ESRIShapefile shp2 : shapeFiles)
			if (shp2.path != null && shp2.filename != null)
				if (shp2.path.equals(shape.path) && shp2.filename.equals(shape.filename))
					return false;

		if((map!=null) && (shape.getMap()==null)) {
			shape.setMap(map);
		}
		shapeFiles.add( shape );

//		GMA 1.4.8: Must set ShapeSuite for new ESRIShapefile
		shape.suite = this;

		if( map!=null) {
			map.addOverlay(shape.filename, shape);
			if (shape.getMultiImage() != null)
				shape.getMultiImage().focus();
			map.repaint();
		}
		fireTableStructureChanged();
		return true;
	}

	public void removeShapeFile( ESRIShapefile shape ) {
		shapeFiles.remove( shape );
		shape.dispose();
		if (map != null) {
			map.repaint();
		}
		fireTableStructureChanged();
	}

	public boolean addShapeFile( File file ) throws IOException {
		if( file.getName().endsWith(".zip") ) {
			ESRIShapefile shape = new ESRIShapefile( new java.util.zip.ZipInputStream( new FileInputStream(file) ));
			if( map!=null ) shape.setMap(map);
			shapeFiles.add( shape );
			map.addOverlay(shape.filename, shape);
			fireTableStructureChanged();
			return true;
		}
		String path = file.getParent();
		String name  = file.getName();
		name = name.substring( 0, name.lastIndexOf(".") );
		ESRIShapefile shape = new ESRIShapefile( path, name);

		if (containsShape(shape))
			return false;

//		GMA 1.4.8: Must set ShapeSuite for new ESRIShapefile
		shape.suite = this;
		shapeFiles.add( shape );

		if( map!=null ) {
			shape.setMap(map); // shape.forward( map.getProjection(), map.getWrap() );
			map.addOverlay(shape.filename, shape);
			if (shape.getMultiImage() != null)
				shape.getMultiImage().focus();
			map.repaint();
		}
		fireTableStructureChanged();
		return true;
	}

	public boolean addShapeFile( String url ) throws IOException {
		int i = url.lastIndexOf("/")+1;
		String path = url.substring(0,i);
		String name  = url.substring(i);
		name = name.substring( 0, name.lastIndexOf(".") );
		ESRIShapefile shape = new ESRIShapefile( path, name );

		if (containsShape(shape))
			return false;

//		GMA 1.4.8: Must set ShapeSuite for new ESRIShapefile
		shape.suite = this;
		shapeFiles.add( shape );

		if( map!=null ) {
			shape.setMap(map); // shape.forward( map.getProjection(), map.getWrap() );
			map.addOverlay(name,shape);
			if (shape.getMultiImage() != null)
				shape.getMultiImage().focus();
			map.repaint();
		}
		fireTableStructureChanged();
		MapApp.sendLogMessage("Shape_File_Imported_URL&name="+name);
		return true;
	}

	public boolean addShapeFile( String url, XML_Menu inputXML_Menu ) throws IOException {
		int i = url.lastIndexOf("/")+1;
		String path = url.substring(0,i);
		String name  = url.substring(i);
		name = name.substring( 0, name.lastIndexOf(".") );
		ESRIShapefile shape = new ESRIShapefile( path, name );
		name = inputXML_Menu.name;
		shape.setName(name);
		shape.setInfoURL(inputXML_Menu.infoURLString);
		shape.setWESN(inputXML_Menu.wesn);

		String urlPath = inputXML_Menu.layer_url.toString();
		if (urlPath != null && urlPath.endsWith(".shp"))
		// Track hits to viewed shape
			UIDTracker.sendTrackStat( urlPath.replace(".shp", ".uid"));
		if (containsShape(shape)){
			return false;
		}

//		GMA 1.4.8: Must set ShapeSuite for new ESRIShapefile
		shape.suite = this;
		shapeFiles.add( shape );
		if( map!=null ) {
			shape.setMap(map); // shape.forward( map.getProjection(), map.getWrap() );

			// Add the shape file to the map
			// Remove it before hand to make sure it hasn't been added prematurely / already exists
			map.removeOverlay(shape);
			map.addOverlay(name, inputXML_Menu.infoURLString, shape, inputXML_Menu);
			
			
			if (shape.getMultiImage() != null) {
				shape.getMultiImage().focus();
			} else if (shape.getMultiGrid()!=null) {
				// TODO: nothing to do for now
			} else {
				// TODO: reserve for the lines and polygons
			}
			map.repaint();
		}
		
		//load any saved grid parameters from the session XML file
		if (shape != null && shape.getMultiGrid() != null && shape.getMultiGrid().getGrid2DOverlay() != null) {
			Grid2DOverlay overlay = shape.getMultiGrid().getGrid2DOverlay();
			overlay.loadSessionParameters(inputXML_Menu);
		}

		fireTableStructureChanged();
		return true;
	}

	public boolean containsShape(ESRIShapefile shape) {
		if (shapeFiles.contains(shape)) {
			return true;
		}

		for (ESRIShapefile shp2 : shapeFiles)
			if (shp2.path != null && shp2.filename != null)
				if (shp2.path.equals(shape.path) && shp2.filename.equals(shape.filename))
					return true;
		return false;
	}

	public void setParent(Component parent, ViewShapes inputViewShapes) {
		this.parent = parent;
		viewShapes = inputViewShapes;
	}

//	GMA 1.4.8: Change visibility to public so that MapApp can invoke function
//	String getURLString() {
	public String getURLString() {
		JTextField txt = new JTextField(60);
		JPanel panel = new JPanel(new GridLayout(2,0));
		panel.add(new JLabel("Enter URL (example: \"https://www.ldeo.columbia.edu/~akm/shape_files/WF2004_T741.shp\")"));
		panel.add(txt);
		int ok = JOptionPane.showConfirmDialog((JFrame)map.getTopLevelAncestor(),
					panel,
					"enter URL",
					JOptionPane.OK_CANCEL_OPTION);
		if( ok==JOptionPane.CANCEL_OPTION) return null;
		return txt.getText();
	}

//	GMA 1.4.8: Made importGrid() public so it can be called in MapApp
//	void importGrid() {
	public void importGrid() {
		Projection proj = map.getProjection();
		int mapType;
		if (proj instanceof Mercator)
			mapType = MapApp.MERCATOR_MAP;
		else if (proj instanceof PolarStereo)
			if (((PolarStereo) proj).getHemisphere() == PolarStereo.NORTH)
				mapType = MapApp.NORTH_POLAR_MAP;
			else 
				mapType = MapApp.SOUTH_POLAR_MAP;
		else 
			mapType = MapApp.MERCATOR_MAP; // just a fail safe

		new ImportGrid(new JFrame("ImportGrid"), this, mapType);
	}

	public void openData(ESRIShapefile shape) {
		try {
			Vector props = shape.getProperties();
			XBTable dbfTable = shape.getTable();
			props = (Vector)((Object[])props.get(0))[1];
			String type = ParseLink.getProperty(props, "type").toString();
			if( type.equals("image") ) {
				if ( viewShapes == null || viewShapes.dialog == null ) {
					((MapApp)map.getApp()).getMapTools().shapeTB.doClick();
				}
				if ( viewShapes != null && viewShapes.dialog != null ) {
					URLImageViewer viewer = new URLImageViewer(props, shape, new JLabel(ParseLink.getProperty(props, "name").toString()), viewShapes.dialog);
				}
				return;
			} else if( type.equals("tiled_grid") ) {
				shape.openGridDialog();
				return;
			} else if ( type.equals("tiled_images") ) {
				shape.getMultiImage().focus();
				return;
			}
			if( !type.equals("shape") )return;
			int row = dbfTable.getSelectedRow();
			if( row==-1 )return;
			String link = insertTokens(ParseLink.getProperty(props, "url").toString(),row,shape);
			try {
				addShapeFile(link);
			} catch(IOException e) {
			}
		} catch(Exception e) {
			ShowStackTrace.showTrace( e, map.getTopLevelAncestor() );
		}
	}
	String insertTokens(String template, int row, ESRIShapefile shape) {
		String tokens = "${}";
		StringTokenizer st = new StringTokenizer( template, tokens, true );
		StringBuffer sb = new StringBuffer();
		while( st.hasMoreTokens() ) {
			String s = st.nextToken();
			if( s.equals("$") ) {
				s = st.nextToken();
				if( s.equals("{") ) {
					s = st.nextToken();
					String s1 = st.nextToken();
					if( s1.equals("}") ) {
						int col = Integer.parseInt(s)-1;
						sb.append( shape.getDBFFile().getValueAt(row, col) );
					}
				} else {
					sb.append("$"+s);
				}
				continue;
			}
			sb.append(s);
		}
		return sb.toString();
	}

//	GMA 1.4.8: Have addShapeFile() return boolean to indicate whether file has been loaded
//	public void addShapeFile( ) throws IOException {
	public boolean addShapeFile() throws IOException {
		getChooser();
		boolean multi = chooser.isMultiSelectionEnabled();
		chooser.setMultiSelectionEnabled(true);
		chooser.addChoosableFileFilter(shapeF);
		int ok = chooser.showOpenDialog(parent);
		if( ok==chooser.CANCEL_OPTION ) {
			chooser.setMultiSelectionEnabled(multi);
			chooser.removeChoosableFileFilter(shapeF);

//			GMA 1.4.8: Modified version must return boolean
			return false;
		}
		File[] sel = chooser.getSelectedFiles();
		for( int k=0 ; k<sel.length ; k++) {
			addShapeFile(sel[k]);
			MapApp.sendLogMessage("Shape_File_Imported&name="+sel[k].getName());
		}
		chooser.setMultiSelectionEnabled(multi);
		chooser.removeChoosableFileFilter(shapeF);

//		GMA 1.4.8: Modified version must return boolean
		return true;
	}

	public JFileChooser getChooser() {
		if( chooser==null ) {
			chooser = haxby.map.MapApp.getFileChooser();
			shapeF = new javax.swing.filechooser.FileFilter() {
				public boolean accept(File f) {
					if( f.isDirectory() ) return true;
					return f.getName().toLowerCase().endsWith(".shp");
				}
				public String getDescription() {
					return "shape files (.shp)";
				}
			};
		}
		return chooser;
	}


	public void setMap(XMap map) {
		if( map==null|| this.map!=null )return;
		this.map = map;
		for( int k=0 ; k<shapeFiles.size() ; k++) {
			ESRIShapefile s = shapeFiles.get(k);
			if(s.getMap()==null) s.setMap(map);
		}
	}
// TableModel methods
	public int getRowCount() {
		return shapeFiles.size();
	}
	public int getColumnCount() {
		return 5;
	}

//	If shapefile is present, returns row of shapefile, otherwise returns -1
	public int getRowForShapefile(ESRIShapefile shape) {
		int row = 0;
		for ( ; row < shapeFiles.size(); row++ ) {
			if ( shapeFiles.get(row).equals(shape) ) {
				return row;
			}
		}
		return -1;
	}

//	Returns class that contains this class in GeoMapApp (ViewShapes)
	public ViewShapes getViewShapes() {
		return viewShapes;
	}

	public boolean isRowVisible( String rowName ) {
		boolean rowIsVisible = false;
		if ( rowName != null ) {
			for ( int i = 0; i < shapeFiles.size(); i++ ) {
				ESRIShapefile shape = (ESRIShapefile)shapeFiles.get(i);
				if ( shape.getName().indexOf(rowName) != -1 ) {
					if ( shape.isVisible() ) {
						rowIsVisible = true;
					}
				}
			}
		}
		return rowIsVisible;
	}

	public Object getValueAt(int row, int column) {
		ESRIShapefile shape = shapeFiles.get(row);
		if( column==0 ) return shape.getName();
		else if( column==1 ) return new Integer(shape.getType());
		else if( column==2 ) return new Boolean(shape.isVisible());
		else if( column==3 ) return shape.getDefaultOutline();
		else if( column==4 ) return shape.getDefaultLW();
		else return null;
	}

	public Class getColumnClass(int column) {
		if( column==0 ) return String.class;
		else if( column==1 ) return Integer.class;
		else if( column==2 ) return Boolean.class;
		else if( column==3 ) return Color.class;
		else if( column==4 ) return LineWidth.class;
		else return String.class;
	}

	public String getColumnName(int column) {
		if( column==0 ) return "name";
		else if( column==1 ) return "type";
		else if( column==2 ) return "visible";
		else if( column==3 ) return "color";
		else if( column==4 ) return "Line";
		else return "unknown";
	}
	public boolean isCellEditable(int row, int col) {
		return col!=1 && getColumnClass(col)!=Color.class;
	}

	public void setValueAt(Object val, int row, int col) {
		ESRIShapefile shape = shapeFiles.get(row);
		try {
		//	System.out.println(val.toString() +"\t"+ val.getClass().getName());
		//	if( col==2 )shape.setVisible( ((Boolean)val).booleanValue());
			if( col==2 )shape.setVisible( ((Boolean)val).booleanValue());
			if( col==3 ) {
				Color c = (Color)val;
				shape.setDefaultOutline( c );
			}
			if( col==4 ) shape.setDefaultLW( (LineWidth)val );
			if( col==0) shape.setName( val.toString() );
		} catch(Exception e) {
		}
	}

	public static void main(String[] args) {
		ShapeSuite suite = new ShapeSuite();
		try {
			suite.addShapeFile();
		} catch(IOException e) {
			e.printStackTrace(System.err);
			System.exit(0);
		}
		MapApp app = new MapApp(MapApp.MERCATOR_MAP);
		suite.setMap( app.getMap() );
		suite.setParent( app.getMap().getTopLevelAncestor(), null );
		JTable table = new JTable(suite);
		JFrame frame = new JFrame();
		frame.getContentPane().add(new JScrollPane(table));
		frame.pack();
		frame.show();
	}
}
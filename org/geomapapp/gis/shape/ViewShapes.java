package org.geomapapp.gis.shape;

import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.XBTable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.geomapapp.image.ColorModPanel;
import org.geomapapp.io.ShowStackTrace;
import org.geomapapp.util.Icons;
import org.geomapapp.util.ParseLink;

public class ViewShapes {
	ShapeSuite suite;
	XMap map;
	XBTable table;
	
//	GMA 1.4.8: Make "Layers" window a frame so it can be minimized
//	JDialog dialog;
	JFrame dialog;
	
	JSplitPane split;
	Vector shapes;
	Vector properties;
	XBTable dbfTable;
	ESRIShapefile shape;
	DBFFile dbf;
	ListSelectionListener tableL;
	ListSelectionListener dbfL;
	MouseAdapter headerL;
	MouseAdapter mapL;
	MouseAdapter colorL;
	ColorModPanel colorModPanel;
	JComboBox lineWidths;
	JToggleButton shapeToggle;
	JButton openObject;
	JLabel objectName;
	String infoURL;
	JButton info;
	JButton help;
	public ViewShapes(ShapeSuite suite, XMap map) {
		this.suite = suite;
		this.map = map;
		initToggle();
	//	init();
	}
	public ViewShapes() {
		suite = new ShapeSuite();
		MapApp app = new MapApp(MapApp.MERCATOR_MAP);
		map = app.getMap();
		suite.setMap( map );
		initToggle();
	}
	public void addShapeFile( ESRIShapefile shape ) {
		suite.addShapeFile( shape );
	}
	public void setVisible(boolean tf) {
		dialog.setVisible( tf);
	}
	public JToggleButton getToggle() {
		return shapeToggle;
	}
	void initToggle() {
		shapeToggle = new JToggleButton(Icons.getIcon(Icons.POLYGON, false));
		shapeToggle.setSelectedIcon(Icons.getIcon(Icons.POLYGON, true));
		shapeToggle.setBorder( null );
		shapeToggle.setToolTipText("Shapefile Manager");
		shapeToggle.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if( shapeToggle.isSelected() ) {
				//	if( suite.getShapes().size()==0 ) {
					if( table==null ) {
						init();
					//	shapeToggle.setSelected(false);
						return;
					}
				}
				dialog.setVisible( shapeToggle.isSelected() );
			}
		});
	}
	void init() {
		shapes = suite.getShapes();
	//	if( shapes==null || shapes.size()==0)System.exit(0);
		initListeners();
		map.removeMouseListener( mapL );
		map.addMouseListener( mapL );
	//	suite.setMap( map );
		JFrame top = (JFrame)map.getTopLevelAncestor();
		suite.setParent( top, this );
		table = new XBTable(suite);
		table.getTableHeader().setReorderingAllowed(false);
		table.addMouseListener(colorL);
		table.getSelectionModel().addListSelectionListener( tableL );

//		GMA 1.4.8: Make "Layers" window a frame so it can be minimized
//		dialog = new JDialog(top, "Layers");
		dialog = new JFrame("Shapefile Manager");
		if(!dialog.isVisible()) dialog.setLocation(((MapApp)map.getApp()).getFrame().getLocation());

		split = new JSplitPane();
		JScrollPane sp = new JScrollPane(table);
		split.setLeftComponent(sp);
		shape = shapes.size()==0
				? null
				: (ESRIShapefile)shapes.get(0);
		if(shape==null) {
			split.setRightComponent( new JLabel("no shape files loaded") );
		} else {
			dbfTable = shape.getTable();
			dbfTable.getTableHeader().setReorderingAllowed(false);
			sp = new JScrollPane(dbfTable);
			split.setRightComponent(sp);
			map.addOverlay(shape);
			dbfTable.getColumnHeader().addMouseListener(headerL);
			dbfTable.getSelectionModel().addListSelectionListener( dbfL );
			map.repaint();
		}

		dialog.getContentPane().add(split);

		JMenuBar bar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		bar.add(fileMenu);
		fileMenu.setMnemonic(KeyEvent.VK_F);
		JMenuItem item = fileMenu.add("Import shape URL");
		item.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JTextField field = new JTextField(60);
				JPanel p = new JPanel(new GridLayout(0,1));
				p.add( new JLabel("Enter shp URL"));
				p.add(field);
				int ok = JOptionPane.showConfirmDialog(
					table, p, "URL", JOptionPane.OK_CANCEL_OPTION);
				if( ok == JOptionPane.CANCEL_OPTION)return;
				try {
					suite.addShapeFile(field.getText());
				} catch(Exception ex) {
					ex.printStackTrace(System.err);
				}
			}
		});
		fileMenu.addSeparator();
		item = fileMenu.add("Save Current ShapeFile");
		item.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if( table.getSelectedRow()<0 ) return;
				shape = (ESRIShapefile)shapes.get(table.getSelectedRow());
				JFileChooser c = suite.getChooser();
				int ok = JOptionPane.NO_OPTION;
				c.setSelectedFile( new File(c.getCurrentDirectory(), shape.getName()));
				File file = null;
				while( ok==JOptionPane.NO_OPTION ) {
					ok = c.showSaveDialog( table );
					if( ok==c.CANCEL_OPTION )return;
					file = c.getSelectedFile();
					if( !file.getName().endsWith(".shp") ) {
						file = new File( c.getCurrentDirectory(), file.getName()+".shp" );
					}
					if( file.exists() ) {
						ok = JOptionPane.showConfirmDialog(table,
							"File exists. OverWrite?");
						if( ok==JOptionPane.CANCEL_OPTION )return;
					} else {
						ok = JOptionPane.NO_OPTION-1;
					}
				}
				try {
					shape.writeShapes(file);
				} catch(Exception ex) {
					ex.printStackTrace(System.err);
				}
			}
		});
		JPanel north = new JPanel( new BorderLayout());
	//	north.add( bar, "West");

//		GMA 1.4.8: Changed question mark button to "Help" button to better indicate its functionality to the user
//		help = new JButton( Icons.getIcon(Icons.HELP, false) );
//		help.setPressedIcon( Icons.getIcon(Icons.HELP, true) );
//		help.setBorder( BorderFactory.createEmptyBorder(1,1,1,1));
		help = new JButton( "Help" );
		
		help.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String url =
					PathUtil.getPath("HTML/LAYER_INTERFACE", 
							MapApp.BASE_URL+"/gma_html/UserGuide.htm#How_to_import_shapefiles");
				BrowseURL.browseURL(url);
			}
		});

		info = new JButton( Icons.getIcon(Icons.INFO, false) );
		info.setPressedIcon( Icons.getIcon(Icons.INFO, true) );
		info.setDisabledIcon( Icons.getDisabledIcon( Icons.INFO, false ));
		info.setBorder( BorderFactory.createEmptyBorder(1,1,1,1));
		info.setEnabled(false);
		info.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showInfo();
			}
		});

		JButton zoom = new JButton( Icons.getIcon(Icons.ZOOM_IN, false) );
		zoom.setPressedIcon( Icons.getIcon(Icons.ZOOM_IN, true) );
		zoom.setBorder( BorderFactory.createEmptyBorder(1,1,1,1));
		zoom.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				zoomTo();
			}
		});

		JButton close = new JButton( Icons.getIcon(Icons.CLOSE, false) );
		close.setPressedIcon( Icons.getIcon(Icons.CLOSE, true) );
		close.setBorder( BorderFactory.createEmptyBorder(1,1,1,1));
		close.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				closeData();
			}
		});

		openObject = new JButton( Icons.getIcon(Icons.LIGHTBULB, false) );
		openObject.setToolTipText("Open Object");
		openObject.setPressedIcon( Icons.getIcon(Icons.LIGHTBULB, true) );
		openObject.setBorder( BorderFactory.createEmptyBorder(1,1,1,1));
		openObject.setDisabledIcon( Icons.getDisabledIcon( Icons.LIGHTBULB, false ));
		openObject.setEnabled(false);

		openObject.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openData();
			}
		});

		objectName = new JLabel("");

		JPanel tools = new JPanel();
		tools.add( info );
		tools.add( zoom );
		tools.add( openObject );
		tools.add( objectName );
		north.add( tools, "Center");

		tools = new JPanel();
	//	tools.add( info );
		tools.add( help );
		tools.add( close );
		north.add( tools, "West");
		dialog.getContentPane().add(north,"North");
		dialog.pack();
		Dimension dim = dialog.getPreferredSize();
		dialog.setSize( new Dimension(dim.width*2/3, 200) );
		split.setDividerLocation(dim.width/3);
		dialog.show();

		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				shapeToggle.setSelected(false);
			}
		});

		lineWidths = new JComboBox();
		for( int i=1 ; i<10 ; i++) {
			LineWidth lw = new LineWidth((float)i);
			lineWidths.addItem(lw);
		}
		LineWidth renderer = new LineWidth(1f);
		lineWidths.setRenderer( renderer );
	//	JOptionPane.showMessageDialog( MapApp.anchor, lineWidths);
		table.setDefaultEditor( LineWidth.class, 
			new javax.swing.DefaultCellEditor(lineWidths));
		table.setDefaultRenderer( LineWidth.class, renderer);
		
		table.getSelectionModel().setSelectionMode(
				table.getSelectionModel().SINGLE_SELECTION);
	}
	public void closeData() {
		if( table.getSelectedRow()<0 ) return;
		int row = table.getSelectedRow();
		shape = (ESRIShapefile)shapes.get(row);
		map.removeOverlay(shape);
		if ( !suite.shapeFiles.contains(shape) ) {
			return;
		}
		dbfTable = shape.getTable();
		dbfTable.getSelectionModel().removeListSelectionListener( dbfL );
		suite.removeShapeFile( shape );
		if( table.getRowCount()==0 )return;
		if( row>=table.getRowCount() )row--;
		table.setRowSelectionInterval(row, row);
	}
	
	public void closeData(ESRIShapefile shapeToClose) {
		int row = suite.getRowForShapefile(shapeToClose);
		if ( row < 0 ) {
			return;
		}
		map.removeOverlay(shapeToClose);
		dbfTable = shapeToClose.getTable();
		dbfTable.getSelectionModel().removeListSelectionListener( dbfL );
		suite.removeShapeFile(shapeToClose);
		if( table.getRowCount()==0 )return;
		if( row>=table.getRowCount() )row--;
		table.setRowSelectionInterval(row, row);
	}
	
	public void showInfo() {
		if( infoURL==null )return;
		int row = dbfTable.getSelectedRow();
		if( row==-1 )return;
		String link = insertTokens(infoURL, row);
		BrowseURL.browseURL(link);
	}
	
	public JFrame getFrame() {
		return dialog;
	}
	
	public int getSelectedRow() {
		return table.getSelectedRow();
	}
	
	public String getSelectedShape() {
		return shape.getName();
	}
	
	void zoomTo() {
		int row = dbfTable.getSelectedRow();
		try {
			Rectangle2D.Double rect = null;
			if( row==-1 ) {
				rect = (Rectangle2D.Double)this.shape;
				double w = shape.getWidth();
				double h = shape.getHeight();
				rect = new Rectangle2D.Double( shape.getX()-.05*w,
							shape.getY()-.05*h,
							w*1.1, h*1.1 );
			} else {
				Rectangle2D shape = (Rectangle2D)this.shape.getShapes().get(row);
				double w = shape.getWidth();
				double h = shape.getHeight();
				rect = new Rectangle2D.Double( shape.getX()-.25*w,
							shape.getY()-.25*h,
							w*1.5, h*1.5 );
			}
			
//			GMA 1.4.8: TESTING - trying to pull out lat/lon for future display in "Layers" window
			Point2D.Double p = new Point2D.Double( shape.getX(), shape.getY() );
			Point2D.Double pt = (Point2D.Double)map.getProjection().getRefXY( p );
			System.out.println( "Unadjusted lat/lon: " + pt.getX() + "\t" + pt.getY() );
			
			map.zoomToRect( rect );
			map.repaint();
		} catch(Exception e) {
		}
	}
	public void openData() {
		try {
			Vector props = shape.getProperties();
			props = (Vector)((Object[])props.get(0))[1];
			String type = ParseLink.getProperty(props, "type").toString();
			if( type.equals("image") ) {
				URLImageViewer viewer = new URLImageViewer(props, shape, objectName, dialog);
				return;
			} else if( type.equals("tiled_grid") ) {
				shape.openGridDialog();
				shape.setVisible(true);
				return;
			} else if ( type.equals("tiled_images") ) {
				shape.getMultiImage().focus();
				shape.setVisible(true);
				return;
			}
			if( !type.equals("shape") )return;
			int row = dbfTable.getSelectedRow();
			if( row==-1 )return;
			String link = insertTokens(ParseLink.getProperty(props, "url").toString(), row);
			try {
				boolean tf = suite.addShapeFile(link);
				if (!tf) 
					return;
				row=suite.getShapes().size()-1;
				table.setRowSelectionInterval( row, row );
			} catch(IOException e) {
				objectName.setText( "No Link Present");
			}
		} catch(Exception e) {
			ShowStackTrace.showTrace( e, map.getTopLevelAncestor() );
		}
	}
	String insertTokens(String template, int row) {
		String tokens = "${}";
		StringTokenizer st = new StringTokenizer( template, tokens, true );
		StringBuffer sb = new StringBuffer();
		boolean tag=false;
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
	void update(XBTable t) {
		if( t==table ) {
			if(dbfTable!=null)dbfTable.getSelectionModel().removeListSelectionListener( dbfL );
			if( table.getSelectedRow()<0 ) {
				shape=null;
			//	openObject.setSelected( false );
				openObject.setEnabled( false );
				info.setEnabled( false );
				objectName.setText("");
				return;
			}
			shape = (ESRIShapefile)shapes.get(table.getSelectedRow());
			map.moveOverlayToFront(shape);
			dbfTable = shape.getTable();
			dbfTable.getSelectionModel().removeListSelectionListener( dbfL );
			dbfTable.getSelectionModel().addListSelectionListener( dbfL );
			properties = shape.getProperties();
			int row = dbfTable.getSelectedRow();
			if( row<0 ) {
				if( table.getRowCount()>0 )dbfTable.setRowSelectionInterval(0, 0);
			}
			row = dbfTable.getSelectedRow();
			if( row<0 ) {
				openObject.setEnabled(false);
				info.setEnabled( false );
			} else {
				openObject.setEnabled( properties!=null );
			}
			if( properties==null ) {
				objectName.setText("");
				info.setEnabled( false );
			} else {
				Object[] p = (Object[])properties.get(0);
				Vector props = (Vector)p[1];
				objectName.setText( ParseLink.getProperty(props, "name").toString() );
				infoURL = (String)ParseLink.getProperty(props, "info");
				info.setEnabled( infoURL!=null );
			}

			JScrollPane sp = new JScrollPane(dbfTable);
			int loc = split.getDividerLocation();
			split.setRightComponent(sp);
			dbfTable.getColumnHeader().addMouseListener(headerL);
			split.setDividerLocation(loc);
			map.repaint();
			dbfTable.repaint();
		} else if( t==dbfTable ) {
			int row = dbfTable.getSelectedRow();
			if( row<0 ) {
				openObject.setEnabled(false);
				info.setEnabled( false );
			} else {
				openObject.setEnabled( properties!=null );
				info.setEnabled( infoURL!=null );
			}
			synchronized (map.getTreeLock()) {
				shape.draw(map.getGraphics2D());
				map.repaint();
			}
		}
	}
	void select(MouseEvent e) {
		if( shape==null )return;
		double radiusSq = Math.pow( 5./map.getZoom(), 2);
		java.awt.geom.Point2D p = map.getScaledPoint(e.getPoint());
		NearNeighbor n = new NearNeighbor( null, p, radiusSq, 0.);
		int i = shape.getSelectedIndex(n);
		if( i<0 ) {
			dbfTable.getSelectionModel().clearSelection();
			return;
		}
		dbfTable.getSelectionModel().setSelectionInterval( i, i);
		dbfTable.ensureIndexIsVisible( i );
		map.repaint();
	}
	void sort( MouseEvent e ) {
		int col = dbfTable.columnAtPoint( e.getPoint());
		shape.sort( col, e.isShiftDown() );
	}
	void initListeners() {
		colorModPanel = new ColorModPanel();
		colorModPanel.setToolTipText("Click to choose color.");
		colorL = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if( e.getClickCount()!=1 )return;
				int col = table.columnAtPoint(e.getPoint());
				if( table.getModel().getColumnClass(col)!=Color.class)return;
				int row = table.rowAtPoint(e.getPoint());
				Color color = (Color)table.getModel().getValueAt(row, col);
				int rgb = color.getRGB();
				colorModPanel.setDefaultRGB(rgb);
				int newRGB = colorModPanel.showDialog(table.getTopLevelAncestor());
				if( newRGB == rgb)return;
				table.getModel().setValueAt(
					new Color(newRGB),
					row, col);
				map.repaint();
			}
		};
		tableL = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				update(table);
			}
		};
		dbfL = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				update(dbfTable);
			}
		};
		headerL = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				sort( e );
			}
		};
		mapL = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if( e.isControlDown() ) return;
				if( map.isSelectable() ) select(e);
			}
		};
	}
	public static void main(String[] args) {
		new ViewShapes();
	}
	public void selectIndex(int i) {
		if (table != null)
			table.getSelectionModel().setSelectionInterval(i, i);
	}
}

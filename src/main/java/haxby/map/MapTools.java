package haxby.map;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.undo.CannotUndoException;

import org.geomapapp.gis.shape.ShapeSuite;
import org.geomapapp.gis.shape.ViewShapes;
import org.geomapapp.grid.Grid2D;
import org.geomapapp.grid.Grid2DOverlay;
import org.geomapapp.grid.GridDialog;
import org.geomapapp.grid.KMZSave;
import org.geomapapp.grid.MapImage;
import org.geomapapp.util.Icons;

import haxby.grid.ContributedGridsOverlay;
import haxby.proj.Mercator;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;

public class MapTools implements ActionListener {
	protected MapApp app = null;
	protected XMap map;
	public JToggleButton[] tb;
	public JToggleButton selectB;
	public JToggleButton contributedGrids;
	public JToggleButton maskB, graticule;
	public JToggleButton profileB;
	public JToggleButton digitizeB;
	public JToggleButton shapeTB;
	public JButton focus, grid, save, zoomPrevious, layerManagerB, contribute;
	JPanel panelTop = new JPanel(new BorderLayout());

//	***** GMA 1.6.4: Add pan toggle button and grid to save
	public JToggleButton panB;
	boolean noButton = true;
	public Grid2DOverlay gridToSave = null;
//	***** GMA 1.6.4

	protected JLabel info;
	protected Box box, box2;
	protected GridDialog gridDialog;
	org.geomapapp.util.GMAProfile profile;

//	GMA 1.4.8: Making ViewShapes and ShapeSuite viewable so they can be accessed by MapApp
	public ShapeSuite suite;
	public ViewShapes shapes;

	boolean gridLoaded = false;
	boolean includeInsets = true;
	public static int saveCount = 1;
	protected String contributeDataUrl = PathUtil.getPath("CONTRIBUTE_PATH");

	public MapTools(MapApp app, XMap map) {
		this.app = app;
		this.map = map;
		init();
	}
	public MapApp getApp() {
		return app;
	}
	public JPanel getTools() {
		return panelTop;
	}
	public boolean getIncludeInsets() {
		return includeInsets;
	}
	public GridDialog getGridDialog() {
		return gridDialog;
	}
	public Grid2DOverlay[] getGrids() {
		return gridDialog.getGrids();
	}
	public boolean isSelectable() {
		for( int k=1 ; k<tb.length ; k++) {
			if(tb[k].isSelected())return false;
		}
		return true;
	}
	protected void init() {
		tb = new JToggleButton[4];
		box = Box.createHorizontalBox();
		box.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); // padding top bottom
		box2 = Box.createHorizontalBox();

		ButtonGroup bg = new ButtonGroup();
		JToggleButton no = new JToggleButton();
		bg.add(no);
		JToggleButton tb4 = new JToggleButton();

		// Select Icon
		tb[0] = new JToggleButton(Icons.getIcon(Icons.SELECT_ICON, false));
		tb[0].setSelectedIcon(Icons.getIcon(Icons.SELECT_ICON,true));
		tb[0].setBorder( BorderFactory.createEmptyBorder());
		tb[0].setToolTipText("Select");
		bg.add(tb[0]);
		tb[0].setSelected(true);
		box.add(tb[0]);
		selectB = tb[0];
		box.add(Box.createHorizontalStrut(1)); // space
//		***** GMA 1.6.4: Add pan toggle button to toolbar

		// Pan Icon
		panB = new JToggleButton(Icons.getIcon(Icons.PAN_ICON, false));
		panB.setSelectedIcon(Icons.getIcon(Icons.PAN_ICON,true));
		panB.setBorder( BorderFactory.createEmptyBorder() );
		panB.setToolTipText("Pan the Map");
		bg.add(panB);
		box.add(panB);
		panB.addActionListener(this);
		new ToggleToggler( panB,  no );
		box.add(Box.createHorizontalStrut(1)); // space

		//	Save Icon
		save = new JButton(Icons.getIcon(Icons.SAVE_ICON,false));
		save.setPressedIcon(Icons.getIcon(Icons.SAVE_ICON,true));
		save.setBorder( BorderFactory.createEmptyBorder() );
		save.setEnabled( true );
		save.addActionListener(this);
		save.setToolTipText("Save As...");
		box.add(save);
		box.add(Box.createHorizontalStrut(1)); // space

		// Zoom in Icon
		tb[1] = app.getZoomer().getZoomIn();
		tb[1].setIcon(Icons.getIcon(Icons.ZOOM_IN_ICON ,false));
		tb[1].setSelectedIcon(Icons.getIcon(Icons.ZOOM_IN_ICON ,true));
		tb[1].setBorder(BorderFactory.createEmptyBorder());
		// GMA 1.6.4: Add tool-tip-text to main zoom buttons to indicate their function and the corresponding key command
		tb[1].setToolTipText("Zoom In (Ctrl-Click)");

		bg.add(tb[1]);
		box.add(tb[1]);
		box.add(Box.createHorizontalStrut(1)); // space

		// Zoom out Icon
		tb[2] = app.getZoomer().getZoomOut();
		tb[2].setIcon(Icons.getIcon(Icons.ZOOM_OUT_ICON ,false));
		tb[2].setSelectedIcon(Icons.getIcon(Icons.ZOOM_OUT_ICON,true));
		tb[2].setBorder( BorderFactory.createEmptyBorder() );
//		***** GMA 1.6.4: Add tool-tip-text to main zoom buttons to indicate their function and the corresponding key command
		tb[2].setToolTipText("Zoom Out (Ctrl-Shift-Click)");
		bg.add(tb[2]);
		box.add(tb[2]);
		box.add(Box.createHorizontalStrut(1)); // space

		// Undo Zoom
		zoomPrevious = new JButton( Icons.getIcon(Icons.ZOOM_UNDO_ICON, false));
		zoomPrevious.setPressedIcon(Icons.getIcon(Icons.ZOOM_UNDO_ICON, true));
		zoomPrevious.setBorder(BorderFactory.createEmptyBorder());
		zoomPrevious.setEnabled(true);
		zoomPrevious.addActionListener(this);
		zoomPrevious.setToolTipText("Undo Last Zoom");
		box.add(zoomPrevious);
		box.add(Box.createHorizontalStrut(1)); // space

		// Distance Profile Tool
		profile = new org.geomapapp.util.GMAProfile(map);
		profileB = new JToggleButton(Icons.getIcon(Icons.PROFILE_ICON,false));
		profileB.setSelectedIcon(Icons.getIcon(Icons.PROFILE_ICON,true));
		profileB.setBorder( BorderFactory.createEmptyBorder() );
		profileB.setToolTipText("Distance/Profile Tool");
		profileB.addActionListener(this);
		tb[3] = profileB;
		bg.add(tb[3]);
		box.add(tb[3]);
		box.add(Box.createHorizontalStrut(1)); // space

		// Lat, Lon, Depth Icon
		digitizeB = new JToggleButton(Icons.getIcon(Icons.DIGITIZE_ICON,false));
		digitizeB.setSelectedIcon(Icons.getIcon(Icons.DIGITIZE_ICON,true));
		digitizeB.setBorder(BorderFactory.createEmptyBorder());
		digitizeB.setEnabled(true);
		digitizeB.addActionListener(this);
		digitizeB.setToolTipText("Digitize Latitude, Longitude and Depth");
		box.add(digitizeB);
		box.add(Box.createHorizontalStrut(1)); // space

	//	org.geomapapp.gis.shape.Digitizer dig = new org.geomapapp.gis.shape.Digitizer(map);
	//	tb[4] = dig.getToggle();

//		GMA 1.4.8: Making ViewShapes and ShapeSuite viewable so they can be accessed by MapApp
//		org.geomapapp.gis.shape.ShapeSuite suite = new org.geomapapp.gis.shape.ShapeSuite();
		suite = new ShapeSuite();
		suite.setMap(map);

//		GMA 1.4.8: Making ViewShapes and ShapeSuite viewable so they can be accessed by MapApp
//		org.geomapapp.gis.shape.ViewShapes shapes = new org.geomapapp.gis.shape.ViewShapes(suite, map);
		shapes = new ViewShapes(suite, map);

		// Shapefile Manager Icon
		shapeTB = shapes.getToggle();
		shapeTB.setIcon(Icons.getIcon(Icons.POLYGON_ICON ,false));
		shapeTB.setSelectedIcon(Icons.getIcon(Icons.POLYGON_ICON ,true));
		shapeTB.setBorder( BorderFactory.createEmptyBorder() );
		box.add(shapeTB);
		box.add(Box.createHorizontalStrut(1)); // space
	//	bg.add(tb[4]);
	//	map.addOverlay( dig );

		//box.add( Box.createHorizontalStrut(5) );
		if( map.getMapBorder() instanceof PolarMapBorder ) {
			graticule = new JToggleButton(Icons.getIcon(Icons.GRATICULE_ICON,false));
			graticule.setSelected( true );
			graticule.setToolTipText("Toggle Graticule");
			graticule.setSelectedIcon(Icons.getIcon(Icons.GRATICULE_ICON,true));
			//graticule.setSelectedIcon( haxby.image.Icons.getIcon(haxby.image.Icons.GRATICULE,true) );
			graticule.setBorder( BorderFactory.createEmptyBorder());
			graticule.addActionListener( this);
			box.add(graticule);
			box.add(Box.createHorizontalStrut(1)); // space
		}

		focus = new JButton(Icons.getIcon(Icons.FOCUS_ICON,false));
		focus.setToolTipText("Focus Map - \"F\"");
		focus.setPressedIcon( Icons.getIcon(Icons.FOCUS_ICON,true) );
		focus.setBorder( BorderFactory.createEmptyBorder());
	//	bg.add(focus);
		box.add(focus);
		box.add(Box.createHorizontalStrut(1)); // space

		maskB = new JToggleButton(Icons.getIcon(Icons.MASK_ICON,false));
		maskB.setSelected(false);
		maskB.setSelectedIcon(Icons.getIcon(Icons.MASK_ICON,true));
		maskB.setBorder( BorderFactory.createEmptyBorder());
		box.add(maskB);
		maskB.addActionListener(this);
		maskB.setToolTipText("Highlight High-Resolution GMRT Data");
		box.add(Box.createHorizontalStrut(1)); // space
/*
		contributedGrids = new JToggleButton( Icons.getIcon(Icons.CONTRIBUTED_GRIDS_ICON, false));
		contributedGrids.setSelected(false);
		contributedGrids .setSelectedIcon(Icons.getIcon(Icons.CONTRIBUTED_GRIDS_ICON, true));
		contributedGrids.setBorder( BorderFactory.createEmptyBorder(1,1,1,1));
		contributedGrids.addActionListener(this);
		contributedGrids.setToolTipText("<html>Show Contributed Grids<br>\tClick to select<br>\tRight Click to load</html>");
		box.add(contributedGrids);
*/
		gridDialog = new GridDialog((JFrame)map.getTopLevelAncestor());
//		gridDialog.addGrid( (Grid2DOverlay)overlay );
		tb4 = gridDialog.getToggle();
		tb4.setIcon(Icons.getIcon(Icons.GRID_ICON ,false));
		tb4.setSelectedIcon(Icons.getIcon(Icons.GRID_ICON ,true));
		box.add(tb4);
		box.add(Box.createHorizontalStrut(1));

		switch (app.getMapType()) {
		case MapApp.MERCATOR_MAP:
			gridDialog.initMercatorGrids(map);
			break;
		case MapApp.SOUTH_POLAR_MAP:
			gridDialog.initSPGrids(map);
			break;
		case MapApp.NORTH_POLAR_MAP:
			gridDialog.initNPGrids(map);
			break;
		default:
			break;
		}

		gridDialog.getToggle().addActionListener(this);

		layerManagerB = new JButton( Icons.getIcon(Icons.LAYERS_ICON, false));
		layerManagerB.setPressedIcon(Icons.getIcon(Icons.LAYERS_ICON, true));
		layerManagerB.setBorder(BorderFactory.createEmptyBorder());
		layerManagerB.setEnabled(true);
		layerManagerB.addActionListener(this);
		layerManagerB.setToolTipText("Layer Manager");
		box.add(layerManagerB);
		box.add(Box.createHorizontalStrut(1)); // space

	//	grid = new JButton(Icons.getIcon(Icons.GRID,false));
	//	grid.setPressedIcon(Icons.getIcon(Icons.GRID,true));
	//	grid.setBorder( null );
	//	box.add(grid);
	//	grid.addActionListener(this);
	//	grid.setToolTipText("Load Grid");
	//	contour = new JButton(Icons.getIcon(Icons.CONTOUR,false));
	//	contour.setPressedIcon(Icons.getIcon(Icons.CONTOUR,true));
	//	contour.setBorder( null );
	//	contour.setEnabled( false );
	//	box.add(contour);
//		***** Changed by A.K.M. 06/29/06 *****
/*
		save = new JButton(Icons.getIcon(Icons.SAVE,false));
		save.setPressedIcon(Icons.getIcon(Icons.SAVE,true));
		save.setBorder( null );
		save.setEnabled( true );
		save.addActionListener(this);
		save.setToolTipText("Save grid");
		box.add(save);
*/
//		Moved save icon left in the tool bar
//		***** Changed by A.K.M. 06/29/06 *****

	//	contour.addActionListener(this);
	//	contour.setToolTipText("Contour Grid");
	//	profile = new JToggleButton();

		focus.addActionListener( this );
		for( int i=0 ; i<tb.length ; i++) { 
			tb[i].addActionListener( this); 
			new ToggleToggler(tb[i],no); 
		}

		box.add( Box.createHorizontalStrut(1)); // space
		// Lat Lon
		info = new JLabel("");
		info.setForeground( Color.black);
		info.setFont( new Font("Arial",Font.PLAIN, 12) );
		info.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		box.add(info);
		box.add(Box.createHorizontalStrut(1)); // space

		// Add Contribute Data
		contribute = getContributeButton();
		contribute.addActionListener(this);

		// Don't show if it is at sea.
		if(!MapApp.AT_SEA) {
			box2.add(contribute);
			box2.add(Box.createHorizontalStrut(3)); // space
		}

		panelTop.add(box, "West");
		panelTop.add(box2, "East");
	}

	public Cursor getCurrentCursor() {
		if (!map.baseCursor.equals(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)))
			return map.baseCursor;

		if (tb[0].isSelected() || tb[3].isSelected()) {
			return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
		}

		if (panB.isSelected()) {
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			ClassLoader loader = org.geomapapp.util.Icons.class.getClassLoader();
			String path = "org/geomapapp/resources/icons/open_hand.png";
			java.net.URL url = loader.getResource(path);
			try {
				BufferedImage im = ImageIO.read(url);
//				Image image = toolkit.getImage("img.gif");
				Cursor brokenCursor = toolkit.createCustomCursor( im, new Point(0,0), "open_hand" );
				return brokenCursor;
			} catch (IOException e1) {
				e1.printStackTrace();
				return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
			}
		}
		if (tb[1].isSelected()) {
			return Zoomer.ZOOM_IN;
		}
		if (tb[2].isSelected()) {
			return Zoomer.ZOOM_OUT;
		}
		return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
	}

	public void setInfoText(String text) {
		info.setText(text);
	}

	public void promptRemoveInsets() {
		boolean showInsetConfirm = false;
		if (map.getApp() instanceof MapApp) {
			MapApp mapp = (MapApp)map.getApp();
			if ( map.mapInsets != null ) {
				for ( int k = 0; k < map.mapInsets.size(); k++ ) {
					if ( map.mapInsets.get(k) instanceof MapColorScale || map.mapInsets.get(k) instanceof MapScale ) {
						showInsetConfirm = true;
					}
				}
				if (!showInsetConfirm && mapp.li != null) {
					if ( mapp.li.getRect() != null ) {
						showInsetConfirm = true;
					}
				}
				if (showInsetConfirm) {
					int mapInsetConfirm = JOptionPane.showConfirmDialog( map.getTopLevelAncestor(), "Remove Insets?");
					if ( mapInsetConfirm == JOptionPane.YES_OPTION ) {
						includeInsets = false;
						map.setIncludeInsets(includeInsets);
					}
					else {
						includeInsets = true;
						map.setIncludeInsets(includeInsets);
					}
					return;
				}
			}
		}
		includeInsets = true;
		map.setIncludeInsets(includeInsets);
	}

	public String getInfoText() {
		String infoText = null;
		infoText = info.getText();
		return infoText;
	}

	public void createEmptyBorders() {
		for (int i = 0; i < tb.length; i++)	{
			tb[i].setBorder( BorderFactory.createEmptyBorder(1,1,1,1) );
		}
		maskB.setBorder( BorderFactory.createEmptyBorder(1,1,1,1) );

		if ( graticule != null ) {
			graticule.setBorder( BorderFactory.createEmptyBorder(1,1,1,1) );
		}
		shapeTB.setBorder( BorderFactory.createEmptyBorder(1,1,1,1) );
		gridDialog.gridTB.setBorder( BorderFactory.createEmptyBorder(1,1,1,1) );
		digitizeB.setBorder( BorderFactory.createEmptyBorder(1,1,1,1) );
		focus.setBorder( BorderFactory.createEmptyBorder(1,1,1,1) );
//		contour.setBorder( BorderFactory.createEmptyBorder(1,1,1,1) );
		save.setBorder( BorderFactory.createEmptyBorder(1,1,1,1) );
	}

	public JButton getContributeButton() {
		JButton contributeB = new JButton();
		contributeB.setLayout(new BorderLayout());
		JLabel label1 = new JLabel("Contribute");
		JLabel label2 = new JLabel("Data");
		label1.setFont(new Font("Arial", Font.BOLD, 11));
		contributeB.add(BorderLayout.CENTER,label1);
		label2.setFont(new Font("Arial", Font.BOLD, 11));
		label2.setHorizontalAlignment(SwingConstants.CENTER);
		contributeB.add(BorderLayout.SOUTH,label2);
		contributeB.setSize(new Dimension(115, 30));
		contributeB.setMargin(new Insets(0, 0, 0, 0));
		contributeB.setBackground(new Color( 224, 224, 224)); ;
		contributeB.setFont(new Font("Arial", Font.BOLD, 11));
		contributeB.setHorizontalAlignment(SwingConstants.LEFT);
		
		return contributeB;
	}

	public void checkMapInsert() {
	}

	protected void save() {
		JPanel panel = new JPanel(new GridLayout(0,1));

		ButtonGroup saveType = new ButtonGroup();
		JRadioButton saveGrid1 = new JRadioButton("Grid: .XYZ (ascii format)");
		JRadioButton saveGrid2 = new JRadioButton("Grid: .GMT - .GRD (masked format)");
		final JRadioButton saveGrid3 = new JRadioButton("Grid: NetCDF-4 (nf) .NC");
		JRadioButton saveGrid4 = new JRadioButton("Grid: GeoTIFF");
		JRadioButton saveGrid5 = new JRadioButton("Grid: ESRI");
		JRadioButton saveGrid6 = new JRadioButton("Grid: NetCDF-3 (cf) .NC");
		JRadioButton saveGRD = new JRadioButton("Grid: Higher Resolution Options");

		JRadioButton saveTiffImage = new JRadioButton("Image: GeoTIFF (no overlays)");
		JRadioButton saveKMZ = new JRadioButton("Image: .KMZ (Google Earth)");
		JRadioButton saveMap = new JRadioButton("Image: .JPEG (smallest filesize)", true);
		// GMA 1.6.6: Add option to save as .png image for high detail
		JRadioButton saveMapPNG = new JRadioButton("Image: .PNG (best resolution)");
		JRadioButton savePS = new JRadioButton("Image: PostScript", false);

		JRadioButton saveColorBar = new JRadioButton("Save Color Bar: .PNG");
		
		boolean mercator = map.getProjection() instanceof Mercator;
		// if mercator projection give selected options
		if( mercator ) {
			panel.add(saveGrid1);
		//	panel.add(saveGrid2);
			panel.add(saveGrid3);
			panel.add(saveGrid6);
			panel.add(saveGrid4);
			panel.add(saveGrid5);
			panel.add(saveGRD);
			panel.add(saveTiffImage);
			panel.add(saveKMZ);
			panel.add(savePS);
			panel.add(saveMap);
			panel.add(saveMapPNG);
			panel.add(saveColorBar);

			saveType.add(saveGrid1);
			//saveType.add(saveGrid2);
			saveType.add(saveGrid3);
			saveGrid3.setEnabled(false);
			saveType.add(saveGrid6);
			saveType.add(saveGrid4);
			saveType.add(saveGrid5);
			saveType.add(saveGRD);
			saveType.add(saveTiffImage);
			saveType.add(saveKMZ);
			saveType.add(savePS);
			savePS.setEnabled(false);
			saveType.add(saveMap);
			saveMap.setSelected( true );
			saveType.add(saveMapPNG);
			saveType.add(saveGRD);
			saveType.add(saveColorBar);

			if(map.getMapTools().app.AT_SEA) {
				saveGRD.setEnabled(false);
			}
			//System.out.println(map.getFocus().toString());
			boolean enableSaveGRD = map.getFocus().toString().equals("GMRT Grid Version " + MapApp.versionGMRT);

			if(enableSaveGRD==false){
				saveGRD.setEnabled(false);
			} else {
				saveGRD.setEnabled(true);
			}

		} else {
			// all other projections give 4 options
			panel.add(saveGrid1);
			panel.add(saveMap);
			panel.add(saveMapPNG);
			panel.add(saveColorBar);

			saveType.add(saveGrid1);
			saveType.add(saveMap);
			saveMap.setSelected( true );
			saveType.add(saveMapPNG);
			saveType.add(saveColorBar);
		}
	//	saveGrid.addActionListener(this);
	//	saveMap.addActionListener(this);

		gridLoaded = false;
		Grid2DOverlay ovl = map.getFocus();
		if( ovl.getGrid() != null ) {
			gridLoaded = true;
		} else if ( gridToSave != null ) {		// GMA 1.6.4: TESTING
			gridLoaded = true;
		}
		saveGrid1.setEnabled(gridLoaded);
		saveGrid2.setEnabled(gridLoaded);
		//saveGrid3.setEnabled(gridLoaded);
		saveGrid4.setEnabled(gridLoaded);
		saveGrid5.setEnabled(gridLoaded);
		saveGrid6.setEnabled(gridLoaded);

		int c = JOptionPane.showConfirmDialog( map.getTopLevelAncestor(), panel, "Save Map Window As:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		// Close if no or cancel
		if (c == JOptionPane.CANCEL_OPTION || c == JOptionPane.NO_OPTION ) {
			return;
		}

		saveGrid1.setEnabled(false);
		saveGrid2.setEnabled(false);
		saveGrid3.setEnabled(false);
		saveGrid4.setEnabled(false);
		saveGrid5.setEnabled(false);
		saveGrid6.setEnabled(false);
		savePS.setEnabled(false);
		saveMap.setEnabled(false);
		saveMapPNG.setEnabled(false); // GMA 1.6.6: Add option to save as .png image for high detail
		saveKMZ.setEnabled(false);
		saveTiffImage.setEnabled(false);

//		***** GMA 1.6.4: TESTING
//		boolean g2d = (overlay instanceof Grid2DOverlay);

		final MapOverlay overlay;
		if ( map.getFocus().getGrid() == null && gridToSave != null ) {
			overlay = gridToSave;
		} else
			overlay = map.getFocus();

		final boolean g2d = (overlay instanceof Grid2DOverlay);
//		***** GMA 1.6.4

		Dimension dim = null;
		if( g2d ) {
			try {
				dim = ((Grid2DOverlay) overlay).getGrid().getSize();
			} catch( Exception ex) {
			}
		} else {
			dim = ((GridOverlay) overlay).grid.getSize();
		}

		if( savePS.isSelected() ) {
			if( g2d ) {
				((Grid2DOverlay) overlay).savePS();
			}
		} else if (saveGrid1.isSelected()) {
//			File file = new File("*.xyz");
			File file = new File("untitled" + saveCount++ + ".xyz");

			JLabel label = new JLabel("Save " + dim.width
								+ " by " + dim.height
								+ " grid in XYZ ascii format?");
			panel.add( label );
			label.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(Color.black),
					BorderFactory.createEmptyBorder(10,10,10,10) ));

			int s = JOptionPane.showConfirmDialog( map.getTopLevelAncestor(), panel, "Save grid", JOptionPane.OK_CANCEL_OPTION);

			if(s == JOptionPane.CANCEL_OPTION)
				return;

			JFileChooser chooser = MapApp.getFileChooser();
			chooser.setSelectedFile(file);
			chooser.setFileFilter( new FileFilter() {
				public String getDescription() {
					return "XYZ Ascii Grid (.xyz)";
				}
				public boolean accept(File f) {
					return  f.isDirectory() ||
						f.getName().toLowerCase().endsWith(".xyz");
				}
			});

			int confirm = JOptionPane.NO_OPTION;

			while (confirm == JOptionPane.NO_OPTION) {
				int ok = chooser.showSaveDialog(map.getTopLevelAncestor());

				if (ok == chooser.CANCEL_OPTION)
					return;

				file = chooser.getSelectedFile();
				if ( !file.getName().toLowerCase().endsWith(".xyz") ) {
					file = new File(file.getPath() + ".xyz");
				}
				if (file.exists()) {
					confirm = JOptionPane.showConfirmDialog(map.getTopLevelAncestor(), "File exists, Overwrite?");
					if (confirm == JOptionPane.CANCEL_OPTION) return;
				}
				else
					break;
			}

			final File saveTo = file;
			app.addProcessingTask("Saving Grid...", new Runnable() {
				public void run() {
					try {
						if( g2d ) {
							((Grid2DOverlay) overlay).saveGrid(saveTo);
						} else {
							((GridOverlay) overlay).saveGrid(saveTo);
						}
					} catch(Exception ex) {
						JOptionPane.showMessageDialog(map.getTopLevelAncestor(),
								"an error occurred during this operation:\t"
								+ " "+ ex.getMessage());
					}
				}
			});
		} else if (saveGrid5.isSelected()) { // ESRI grid
			if( g2d ) {
				((Grid2DOverlay) overlay).saveGridToESRI();
			}
		} else if( saveGrid4.isSelected() ) {
			JOptionPane.showMessageDialog( map.getTopLevelAncestor(), "This tiff file cannot be opened with an image viewer.  It is a grid that has been bundled in geotiff format for specific GIS applications. To save as a viewable GeoTIFF image, please use the \"Save image as GeoTIFF\" option.");

			File file = new File("untitled" + saveCount++ + ".tif");

			JFileChooser chooser = MapApp.getFileChooser();
			chooser.setSelectedFile(file);
			chooser.setFileFilter( new FileFilter() {
				public String getDescription() {
					return "GeoTIFF Grid (.tif)";
				}
				public boolean accept(File f) {
					return  f.isDirectory() ||
						f.getName().toLowerCase().endsWith(".tif") ||
						f.getName().toLowerCase().endsWith(".tiff");
				}
			});

			int confirm = JOptionPane.NO_OPTION;

				while (confirm == JOptionPane.NO_OPTION) {
					int ok = chooser.showSaveDialog(map.getTopLevelAncestor());

					if (ok == chooser.CANCEL_OPTION)
					return;

				file = chooser.getSelectedFile();
				if ( !file.getName().toLowerCase().endsWith(".tif") &&
						!file.getName().toLowerCase().endsWith(".tiff")) {
					file = new File(file.getPath() + ".tif");
				}

				if (file.exists()) {
					confirm = JOptionPane.showConfirmDialog(map.getTopLevelAncestor(), "File exists, Overwrite?");
					if (confirm == JOptionPane.CANCEL_OPTION) return;
				}
				else
					break;
			}

			final File saveTo = file;
			app.addProcessingTask("Saving Grid...", new Runnable() {
				public void run() {
					try {
						Grid2D g = ((Grid2DOverlay) overlay).getGrid();
						org.geomapapp.gis.tiff.TIFF.writeTiff(g, saveTo);
					} catch(Exception ex) {
							JOptionPane.showMessageDialog(map.getTopLevelAncestor(),
									"an error occurred during this operation:\t"
									+ " "+ ex.getMessage());
					}
				}
			});
		} else if( saveKMZ.isSelected() ) {
			map.setIncludeInsets(false);

			MapImage mi = new MapImage(map);
			try {
				Grid2D.Image im = mi.getImage(true);
				KMZSave kmz = new KMZSave(null);
				kmz.save( map.getTopLevelAncestor(), im );
			} catch(Exception ex) {
					JOptionPane.showMessageDialog(map.getTopLevelAncestor(),
							"an error occurred during this operation:\t"
							+ " "+ ex.getMessage());
			}
			map.setIncludeInsets(true);
		}
		else if( saveTiffImage.isSelected() ) {
			JOptionPane.showMessageDialog( map.getTopLevelAncestor(), "This tiff image can be opened with any image viewer. \nGeoreferencing information will be included using a sphere mercator projection with zero at the Greenwich Meridian and the equator.  \nTo view with Latitude/Longitude units you may need to change the display units from \"Meters\" to \"Decimal Degrees\" or \"Degrees Minutes Seconds\" in the Layer Properties of your GIS software package.");

			File file = new File("untitled" + saveCount++ + ".tif");

			if ( overlay.getGeoRefImage() == null || map.getZoom() < 2.0 ) {
				JOptionPane.showMessageDialog( map.getTopLevelAncestor(), "Impossible to make a GeoTIFF at this low resolution with hemispheres repeated.  Zoom into desired area and try again.");
				return;
			}

			JFileChooser chooser = MapApp.getFileChooser();
			chooser.setSelectedFile(file);
			chooser.setFileFilter( new FileFilter() {
				public String getDescription() {
					return "GeoTIFF Image (.tif)";
				}
				public boolean accept(File f) {
					return  f.isDirectory() ||
						f.getName().toLowerCase().endsWith("tif") ||
						f.getName().toLowerCase().endsWith("tiff");
				}
			});
			int confirm = JOptionPane.NO_OPTION;

			while (confirm == JOptionPane.NO_OPTION) {
				int ok = chooser.showSaveDialog(map.getTopLevelAncestor());

				if (ok == chooser.CANCEL_OPTION)
					return;

				file = chooser.getSelectedFile();
				if ( !file.getName().endsWith(".tif") && !file.getName().endsWith(".tiff") ) {
					file = new File(file.getPath() + ".tif");
				}

				if (file.exists()) {
					confirm = JOptionPane.showConfirmDialog(map.getTopLevelAncestor(), "File exists, Overwrite?");
					if (confirm == JOptionPane.CANCEL_OPTION) return;
				}
				else
					break;
			}

			final File saveTo = file;
			app.addProcessingTask("Saving Image...", new Runnable() {
				public void run() {
					try {
						Grid2D.Image g = overlay.getGeoRefImage();
						org.geomapapp.gis.tiff.TIFF.writeTiff(g, saveTo);
					} catch(Exception ex) {
						ex.printStackTrace();
							JOptionPane.showMessageDialog(map.getTopLevelAncestor(),
									"an error occurred during this operation:\t"
									+ " "+ ex.getMessage());
					}
				}
			});
		} else if (saveGrid2.isSelected() || saveGrid3.isSelected() ) {
			File file = new File("untitled" + saveCount++ + ".grd");

			JLabel label = new JLabel("Save " + dim.width
								+ " by " + dim.height
								+ " grid in GMT-4 - GRD format?");
			panel.add( label );
			label.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(Color.black),
					BorderFactory.createEmptyBorder(10,10,10,10) ));

			int s = JOptionPane.showConfirmDialog( map.getTopLevelAncestor(), panel, "Save grid", JOptionPane.OK_CANCEL_OPTION);

			if(s == JOptionPane.CANCEL_OPTION)
				return;

			JFileChooser chooser = MapApp.getFileChooser();
			chooser.setSelectedFile(file);
			chooser.setFileFilter( new FileFilter() {
				public String getDescription() {
					return "GMT-4 Grid (.grd)";
				}
				public boolean accept(File f) {
					return  f.isDirectory() ||
						f.getName().toLowerCase().endsWith(".grd");
				}
			});

			int confirm = JOptionPane.NO_OPTION;

			while (confirm == JOptionPane.NO_OPTION) {
				int ok = chooser.showSaveDialog(map.getTopLevelAncestor());

				if (ok == chooser.CANCEL_OPTION)
					return;

				file = chooser.getSelectedFile();
				if ( !file.getName().toLowerCase().endsWith(".grd") ) {
					file = new File(file.getPath() + ".grd");
				}

				if (file.exists()) {
					confirm = JOptionPane.showConfirmDialog(map.getTopLevelAncestor(), "File exists, Overwrite?");
					if (confirm == JOptionPane.CANCEL_OPTION) return;
				}
				else
					break;
			}
			final File saveTo = file;
			app.addProcessingTask("Saving Grid...", new Runnable() {
				public void run() {
					try {
						if( saveGrid3.isSelected() ) {
							((Grid2DOverlay) overlay).saveGrd(saveTo);
						} else {
							((Grid2DOverlay) overlay).saveMaskedGrd(saveTo);
						}
					} catch(Exception ex) {
						JOptionPane.showMessageDialog(map.getTopLevelAncestor(),
								"an error occurred during this operation:\t"
								+ " "+ ex.getMessage());
						//ex.printStackTrace();
					}
				}
			});
		} else if ( saveGrid6.isSelected() ) {
			File file = new File("untitled" + saveCount++ + ".nc");

			JLabel label = new JLabel( "Save " + dim.width + " by " + dim.height + " grid in NetCDF-3 - NC format?" );
			panel.add( label );
			label.setBorder(BorderFactory.createCompoundBorder( BorderFactory.createLineBorder(Color.black), BorderFactory.createEmptyBorder( 10,10,10,10 ) ) );
			int s = JOptionPane.showConfirmDialog( map.getTopLevelAncestor(), panel, "Save grid", JOptionPane.OK_CANCEL_OPTION);
			if ( s == JOptionPane.CANCEL_OPTION ) {
				return;
			}
			JFileChooser chooser = MapApp.getFileChooser();
			chooser.setSelectedFile(file);
			chooser.setFileFilter( new FileFilter() {
				public String getDescription() {
					return "NetCDF-3 Grid (.nc)";
				}
				public boolean accept(File f) {
					return  f.isDirectory() ||
						f.getName().toLowerCase().endsWith(".nc");
				}
			});

			int confirm = JOptionPane.NO_OPTION;
			while (confirm == JOptionPane.NO_OPTION) {
				int ok = chooser.showSaveDialog(map.getTopLevelAncestor());
				if (ok == chooser.CANCEL_OPTION) {
					return;
				}
				file = chooser.getSelectedFile();
				if ( !file.getName().toLowerCase().endsWith(".nc") ) {
					file = new File(file.getPath() + ".nc");
				}
				if (file.exists()) {
					confirm = JOptionPane.showConfirmDialog(map.getTopLevelAncestor(), "File exists, Overwrite?");
					if (confirm == JOptionPane.CANCEL_OPTION) return;
				}
				else {
					break;
				}
			}

			final File saveTo = file;
			app.addProcessingTask("Saving Grid...", new Runnable() {
				public void run() {
					try {
						((Grid2DOverlay) overlay).saveGrdGMT3(saveTo);
					} catch(Exception ex) {
							JOptionPane.showMessageDialog( map.getTopLevelAncestor(), "an error occurred during this operation:\t" + " " + ex.getMessage() );
					}
				}
			});
		}else if (saveGRD.isSelected()){ 
			double west = map.getWESN()[0];
			double east = map.getWESN()[1];
			double south = map.getWESN()[2];
			double north = map.getWESN()[3];

			if(map.mapBorder.hasOverlap()){
				west = east+.0001;
			}

			if(east <= west){
				west = west - 360;
			}

//			String url = "https://gmrt.marine-geo.org/cgi-bin/getmap_page?west="+west+"&east="+east+"&south="+south+"&north="+ north;
			String url = "https://www.gmrt.org/services/cmg_results.php?west="+west+"&east="+east+"&south="+south+"&north="+ north;

			try{
				BrowseURL.browseURL(url);
			}catch(Exception ex) {
				ex.printStackTrace();
			}

		}else if(saveMap.isSelected()) {
			promptRemoveInsets();

			File file = new File("untitled" + saveCount++ + ".jpg");

			JFileChooser chooser = MapApp.getFileChooser();
			chooser.setSelectedFile(file);
			chooser.setFileFilter( new FileFilter() {
				public String getDescription() {
					return "JPEG Image (.jpg)";
				}
				public boolean accept(File f) {
					return  f.isDirectory() ||
						f.getName().toLowerCase().endsWith(".jpg") ||
						f.getName().toLowerCase().endsWith(".jpeg");
				}
			});

			int confirm = JOptionPane.NO_OPTION;

			while (confirm == JOptionPane.NO_OPTION) {
				int ok = chooser.showSaveDialog(map.getTopLevelAncestor());

				if (ok == chooser.CANCEL_OPTION)
					return;

				file = chooser.getSelectedFile();
				if ( !file.getName().toLowerCase().endsWith(".jpg") 
						&& !file.getName().toLowerCase().endsWith(".jpeg") ) {
					file = new File(file.getPath() + ".jpg");
				}

				if (file.exists()) {
					confirm = JOptionPane.showConfirmDialog(map.getTopLevelAncestor(), "File exists, Overwrite?");
					if (confirm == JOptionPane.CANCEL_OPTION) return;
				}
				else
					break;
			}

			final File saveTo = file;
			app.addProcessingTask("Saving Image...", new Runnable() {
				public void run() {
					try {
						map.saveJPEGImage(saveTo);
					}
					catch (Exception ex) { }
					finally {
						map.setIncludeInsets(true);
					}
				}
			});
		}

//		***** GMA 1.6.6: Add option to save as .png image for high detail
		else if(saveMapPNG.isSelected()) {
			promptRemoveInsets();

			File file = new File("untitled" + saveCount++ + ".png");

			JFileChooser chooser = MapApp.getFileChooser();
			chooser.setSelectedFile(file);
			chooser.setFileFilter( new FileFilter() {
				public String getDescription() {
					return "Portable Network Graphics (.png)";
				}
				public boolean accept(File f) {
					return  f.isDirectory() ||
						f.getName().toLowerCase().endsWith(".png");
				}
			});

			int confirm = JOptionPane.NO_OPTION;

			while (confirm == JOptionPane.NO_OPTION) {
				int ok = chooser.showSaveDialog(map.getTopLevelAncestor());

				if (ok == chooser.CANCEL_OPTION)
					return;

				file = chooser.getSelectedFile();
				if ( !file.getName().toLowerCase().endsWith(".png")) {
					file = new File(file.getPath() + ".png");
				}

				if (file.exists()) {
					confirm = JOptionPane.showConfirmDialog(map.getTopLevelAncestor(), "File exists, Overwrite?");
					if (confirm == JOptionPane.CANCEL_OPTION) return;
				}
				else
					break;
			}

			final File saveTo = file;
			app.addProcessingTask("Saving Image...", new Runnable() {
				public void run() {
					try {
						map.savePMGImage(saveTo);
					}
					catch (Exception ex) { }
					finally {
						map.setIncludeInsets(true);
					}
				}
			});
		} 
		else if(saveColorBar.isSelected()) {

			app.initializeColorScale();
			try {
				app.getColorScale().saveColorScale();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
	}

	protected void saveImage() {
		JPanel panel = new JPanel(new GridLayout(0,1));

		ButtonGroup saveType = new ButtonGroup();
		JRadioButton saveKMZ = new JRadioButton("Save image kmz (Google Earth)");
		JRadioButton saveMap = new JRadioButton("Save .jpeg image (smallest filesize)", true);
		JRadioButton saveMapPNG = new JRadioButton("Save .png image (best resolution)");

		boolean mercator = map.getProjection() instanceof Mercator;
		if( mercator ) {
			panel.add(saveKMZ);
		}
		panel.add(saveMap);
		panel.add(saveMapPNG);

	//	saveGrid.addActionListener(this);
	//	saveMap.addActionListener(this);

		if( mercator ) saveType.add(saveKMZ);
		saveType.add(saveMap);
		saveType.add(saveMapPNG);
		saveMap.setSelected( true );

		gridLoaded = false;
		Grid2DOverlay ovl = map.getFocus();
		if( ovl.getGrid()!=null ) gridLoaded = true;

		int c = JOptionPane.showConfirmDialog( map.getTopLevelAncestor(), panel, "Save?", JOptionPane.OK_CANCEL_OPTION);
		if (c == JOptionPane.CANCEL_OPTION)
			return;

		saveMap.setEnabled(false);
		saveMapPNG.setEnabled(false);
		saveKMZ.setEnabled(false);

		MapOverlay overlay = map.getFocus();
		boolean g2d = (overlay instanceof Grid2DOverlay);
		Dimension dim = null;
		if( g2d ) {
			try {
				dim = ((Grid2DOverlay) overlay).getGrid().getSize();
			} catch( Exception ex) {
			}
		} else {
			dim = ((GridOverlay) overlay).grid.getSize();
		}
		if( saveKMZ.isSelected() ) {
			map.setIncludeInsets(false);
			MapImage mi = new MapImage(map);
			try {
				Grid2D.Image im = mi.getImage(true);
				KMZSave kmz = new KMZSave(null);
				kmz.save( map.getTopLevelAncestor(), im );
			} catch(Exception ex) {
					JOptionPane.showMessageDialog(map.getTopLevelAncestor(),
							"an error occurred during this operation:\t"
							+ " "+ ex.getMessage());
			}
			map.setIncludeInsets(true);
		}
		else if(saveMap.isSelected()) {
			promptRemoveInsets();
			File file = new File("untitled" + saveCount++ + ".jpg");

			JFileChooser chooser = MapApp.getFileChooser();
			chooser.setSelectedFile(file);
			chooser.setFileFilter( new FileFilter() {
				public String getDescription() {
					return "JPEG Image (.jpg)";
				}
				public boolean accept(File f) {
					return  f.isDirectory() ||
						f.getName().toLowerCase().endsWith(".jpg") ||
						f.getName().toLowerCase().endsWith(".jpeg");
				}
			});

			int confirm = JOptionPane.NO_OPTION;

			while (confirm == JOptionPane.NO_OPTION) {
				int ok = chooser.showSaveDialog(map.getTopLevelAncestor());

				if (ok == chooser.CANCEL_OPTION)
					return;

				file = chooser.getSelectedFile();
				if ( !file.getName().toLowerCase().endsWith(".jpg") 
						&& !file.getName().toLowerCase().endsWith(".jpeg") ) {
					file = new File(file.getPath() + ".jpg");
				}

				if (file.exists()) {
					confirm = JOptionPane.showConfirmDialog(map.getTopLevelAncestor(), "File exists, Overwrite?");
					if (confirm == JOptionPane.CANCEL_OPTION) return;
				}
				else
					break;
			}

			final File saveTo = file;
			app.addProcessingTask("Saving Image...", new Runnable() {
				public void run() {
					try {
						map.saveJPEGImage(saveTo);
					}
					catch (Exception ex) { }
					finally {
						map.setIncludeInsets(true);
					}
				}
			});
		}
		else if(saveMapPNG.isSelected()) {
			promptRemoveInsets();
			File file = new File("untitled" + saveCount++ + ".png");

			JFileChooser chooser = MapApp.getFileChooser();
			chooser.setSelectedFile(file);
			chooser.setFileFilter( new FileFilter() {
				public String getDescription() {
					return "Bitmap Image (.png)";
				}
				public boolean accept(File f) {
					return  f.isDirectory() ||
						f.getName().toLowerCase().endsWith(".png");
				}
			});

			int confirm = JOptionPane.NO_OPTION;

			while (confirm == JOptionPane.NO_OPTION) {
				int ok = chooser.showSaveDialog(map.getTopLevelAncestor());

				if (ok == chooser.CANCEL_OPTION)
					return;

				file = chooser.getSelectedFile();
				if ( !file.getName().toLowerCase().endsWith(".png")) {
					file = new File(file.getPath() + ".png");
				}

				if (file.exists()) {
					confirm = JOptionPane.showConfirmDialog(map.getTopLevelAncestor(), "File exists, Overwrite?");
					if (confirm == JOptionPane.CANCEL_OPTION) return;
				}
				else
					break;
			}

			final File saveTo = file;
			app.addProcessingTask("Saving Image...", new Runnable() {
				public void run() {
					try {
						map.savePMGImage(saveTo);
					}
					catch (Exception ex) { }
					finally {
						map.setIncludeInsets(true);
					}
				}
			});
		}
	}

	private void disableProfileTools() {
		// turn off profile tool
		profile.setEnabled(false);
	}

	public void actionPerformed(ActionEvent evt) {
		if( evt.getSource()==maskB ) {
			app.setMask( maskB.isSelected() );
		}
		else if ( evt.getSource() == contributedGrids) {
			if ( app.contributedGridsOverlay == null ) {
				app.addProcessingTask("Contributed Grids", new Runnable() {
					public void run() {
						app.contributedGridsOverlay = new ContributedGridsOverlay(app, map, app.whichMap);
						app.toggleContributedGrids();
					}
				});
			}
			else
				app.toggleContributedGrids();
		}
		else if( evt.getSource()==tb[0] || evt.getSource()==tb[1] || evt.getSource()== tb[2]) {
			disableProfileTools();
		}

		else if (evt.getSource()==focus ) {
			if(app!=null) {
				app.mapFocus();

				if (map.getZoom() < 4) {
					app.addProcessingTask("Composing Basemap", new Runnable() {
						public void run() {
							MMapServer.getBaseImage( new Rectangle(0,0,640,498), app.baseMap) ;
							map.repaint();
						}
					});
				}
			}
		}

//		***** GMA 1.6.4: Change cursor to hand when "Pan" button is selected
		else if( evt.getSource() == panB ) {
			// Make sure our profile tool gets deselected.

			if ( !panB.isSelected() ) {
				tb[0].setSelected(true);
			} else {
				disableProfileTools();
			}
		}

//		***** Changed by A.K.M. 1.3.5 *****
//		Load the grid when the toggle button is selected
//		and no grid is already loaded and dispose the grid
//		when the toggle button is deselected
		else if ( evt.getSource()==profileB ) {		
			if ( profileB.isSelected() && !gridDialog.isDialogVisible() ) {
				gridDialog.gridCB.setSelectedIndex(0);
				gridDialog.gridTB.doClick();
//				gridDialog.loadGrid();
				//app.numGridLoaders += 1;
				//System.out.println("Number of grid loaders: " + app.numGridLoaders);
				gridLoaded = true;
			}
			else if ( !profileB.isSelected() ) {
				//app.numGridLoaders -= 1;
				//System.out.println("Number of grid loaders: " + app.numGridLoaders);
				if (!gridDialog.isDialogVisible() && !digitizeB.isSelected() && gridDialog.isLoaded() ) {
					gridDialog.disposeGrid();
					gridLoaded = false;
				}
			}
			profile.setEnabled(profileB.isSelected());
		}
//		***** Changed by A.K.M. 1.3.5 *****

		else if( evt.getSource()==graticule ) {
			map.setGraticule( graticule.isSelected() );
		} 
		else if( evt.getSource()==save ) {
			save();
		} else if (evt.getSource() == contribute) {
			try{
				BrowseURL.browseURL(contributeDataUrl);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}

//		1.3.5: Deselect the toggle button
		else if ( evt.getSource()==gridDialog.getToggle() ) {
			if (!gridDialog.getToggle().isSelected()) {
				if (profileB.isSelected()) {
					profileB.doClick();
				}
				if (digitizeB.isSelected()) {
					digitizeB.doClick();
				}
			}
			else {
				gridLoaded = true;
			}
		} else if ( evt.getSource()==layerManagerB ) {
				((MapApp)app).setLayerManagerVisible(true);
		} else if ( evt.getSource()==zoomPrevious ) {
			// try from undo manager
			try {
				if (XMap.undoManager.canUndo()) {
					XMap.undoManager.undo();
					if(XMap.zoomActionTrack.getText().isEmpty()) {
						XMap.undoManager.undo();
					}
//
					String[] zoomManagerCapture = XMap.zoomActionTrack.getText().split(", ");
					if(zoomManagerCapture.length == 4) {
						//split should have 4 values.
						Double xPoint = Double.parseDouble(zoomManagerCapture[1]);
						Double yPoint = Double.parseDouble(zoomManagerCapture[2]);
						Double zoomLoc = Double.parseDouble(zoomManagerCapture[3]);
						map.validate();
						double zoomMap = map.getZoom();

						// translate lon,lat to xy on map
						Point2D.Double point = (Point2D.Double)(map.getProjection().getMapXY( 
								new Point2D.Double( xPoint, yPoint )));
						point.x *= zoomMap;
						point.y *= zoomMap;
						Insets insets = map.getInsets();
						point.x += insets.left;
						point.y += insets.top;
						double zoomFactor = zoomLoc/zoomMap;
						map.doZoom( point, zoomFactor );	// zoom to past/previous zoom
					}
				}
			} catch (CannotUndoException cuex) {
				// catch but do nothing.
				System.out.println("cannot undo zoom");
			}
			//insert undo action
			/*
			if(app.historyFile.exists()) {

			} else {
				app.startNewZoomHistory();
			}
			*/
		}
	}
/*
	public void mapFocus() {
		double wrap = map.getWrap();
		double zoom = map.getZoom();
		Mercator merc = new Mercator( 0., 0., 360, Projection.SPHERE,
				Mercator.RANGE_0_to_360);
	//	if( zoom < 2000./wrap ) {
	//		BufferedImage im = null;
	//		overlay.setImage( im, 0., 0., 1.);
	//		return;
	//	}
		Rectangle2D.Double rect = (Rectangle2D.Double)map.getClipRect2D();
		while( rect.x>wrap ) rect.x-=wrap;
		while( rect.x<0.  ) rect.x+=wrap;
		double[] wesn = new double[4];
		Mercator proj = (Mercator) map.getProjection();
		wesn[0] = rect.x * 360./wrap;
		wesn[1] = wesn[0] + rect.width * 360./wrap;
		wesn[2] = -merc.getY( proj.getLatitude( rect.y + rect.height ) );
		wesn[3] = -merc.getY( proj.getLatitude( rect.y ) );
		int w = (int) Math.rint( zoom*rect.width );
		int h = (int) Math.rint( zoom*rect.height );
		BufferedImage image = new BufferedImage( w, h,
									BufferedImage.TYPE_INT_RGB);
		MapServerA.getHighRes( wesn, w, h, image);
		overlay = map.getFocus();
		overlay.setImage( image, rect.x, rect.y, 1./zoom );
		map.repaint();
	}
*/
	static ImageIcon FOCUS(boolean pressed) {
		BufferedImage im;
		im = new BufferedImage(20, 20,
				BufferedImage.TYPE_INT_RGB);
		int color = white;
		if( pressed ) color = 0xffc0c0c0;
		for(int y=0 ; y<20 ; y++) {
			for( int x=0 ; x<20 ; x++) im.setRGB(x, y, color);
		}
		Graphics2D g = im.createGraphics();
		g.setColor(Color.black);
		g.setFont( new Font("Serif", Font.ITALIC, 16));
		g.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.drawString("fx", 1, 15);
		return new ImageIcon( im);
	}
	static ImageIcon MASK(boolean selected) {
		int[][] map = {
/*
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,1,1,1,1,0,0,0,0,1,1,1,1,0,0},
			{0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
			{1,1,1,1,0,0,1,1,1,1,0,0,1,1,1,1},
			{1,1,1,0,0,0,0,1,1,0,0,0,0,1,1,1},
			{1,1,1,1,0,0,1,1,1,1,0,0,1,1,1,1},
			{1,1,1,1,1,1,1,0,0,1,1,1,1,1,1,1},
			{0,1,1,1,1,1,1,0,0,1,1,1,1,1,1,0},
			{0,0,1,1,1,1,0,0,0,0,1,1,1,1,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
*/
			{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
			{1,1,0,0,0,0,0,0,0,0,0,0,0,0,1,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,1,1,0,0,0,0,1,1,0,0,0,1},
			{1,0,0,1,1,1,1,0,0,1,1,1,1,0,0,1},
			{1,0,0,1,1,1,1,0,0,1,1,1,1,0,0,1},
			{1,0,0,1,1,1,0,0,0,0,1,1,1,0,0,1},
			{1,0,0,0,0,0,0,0,1,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,1,0,0,0,0,0,0,0,1},
			{1,1,0,0,0,0,1,1,0,0,0,0,0,0,1,1},
			{1,1,0,0,0,1,1,0,0,0,0,0,0,0,1,1},
			{1,1,0,0,0,1,1,0,0,0,0,0,0,0,1,1},
			{1,1,0,0,0,0,1,1,0,0,0,0,0,0,1,1},
			{1,1,1,0,0,0,0,0,0,0,0,0,1,1,1,1},
			{1,1,1,1,0,0,0,0,0,0,0,1,1,1,1,1},
			{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
		};
		return doIcon(map, selected);
	}
	static ImageIcon GRID(boolean selected) {
		int[][] map = {
			{0,1,1,1,1,0,1,1,1,1,0,1,1,1,1,0},
			{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1},
			{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1},
			{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1},
			{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1},
			{0,1,1,1,1,0,1,1,1,1,0,1,1,1,1,0},
			{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1},
			{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1},
			{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1},
			{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1},
			{0,1,1,1,1,0,1,1,1,1,0,1,1,1,1,0},
			{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1},
			{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1},
			{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1},
			{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1},
			{0,1,1,1,1,0,1,1,1,1,0,1,1,1,1,0}
		};
		return doIcon(map, selected);
	}
	static ImageIcon CONTOUR(boolean selected) {
		int[][] map = {
			{0,0,0,0,1,0,0,0,0,0,1,1,0,0,0,0},
			{0,0,0,1,0,0,0,0,1,1,0,0,1,0,0,0},
			{0,0,1,0,0,0,1,1,0,0,0,0,0,1,0,0},
			{0,1,0,0,0,1,0,0,0,0,0,0,0,1,0,0},
			{1,0,0,0,1,0,0,0,0,1,0,0,0,1,0,0},
			{0,0,0,0,1,0,0,0,1,0,1,0,0,1,0,0},
			{0,0,0,1,0,0,0,1,0,0,0,1,0,0,1,0},
			{0,0,1,0,0,0,1,0,0,0,0,1,0,0,1,0},
			{0,0,1,0,0,1,0,0,0,0,0,1,0,0,1,0},
			{0,0,1,0,0,1,0,0,0,0,0,1,0,0,0,1},
			{0,0,1,0,0,0,1,1,0,0,1,0,0,0,0,1},
			{0,0,0,1,0,0,0,0,1,1,0,0,0,0,0,1},
			{0,0,0,0,1,1,0,0,0,0,0,0,0,0,1,0},
			{0,0,0,0,0,0,1,1,1,0,0,0,0,0,1,0},
			{0,0,0,0,0,0,0,0,0,1,1,0,0,1,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,0}
		};
		return doIcon(map, selected);
	}
	public static ImageIcon SELECT(boolean selected) {
		int[][] map = {
			{0,0,0,2,2,0,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,2,1,2,0,0,0,0,0,0,0,0,0,0},
			{0,0,0,2,1,1,2,0,0,0,0,0,0,0,0,0},
			{0,0,0,2,1,1,1,2,0,0,0,0,0,0,0,0},
			{0,0,0,2,1,1,1,1,2,0,0,0,0,0,0,0},
			{0,0,0,2,1,1,1,1,1,2,0,0,0,0,0,0},
			{0,0,0,2,1,1,1,1,1,1,2,0,0,0,0,0},
			{0,0,0,2,1,1,1,1,1,1,1,2,0,0,0,0},
			{0,0,0,2,1,1,1,1,1,1,1,1,2,0,0,0},
			{0,0,0,2,1,1,1,1,1,2,2,2,2,0,0,0},
			{0,0,0,2,1,1,0,1,1,2,0,0,0,0,0,0},
			{0,0,0,2,1,2,2,2,1,1,2,0,0,0,0,0},
			{0,0,0,2,2,0,0,2,1,1,2,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,2,1,2,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,2,1,1,2,0,0,0,0},
			{0,0,0,0,0,0,0,0,0,2,2,0,0,0,0,0},
		};
		return doIcon(map, selected);
	}
	static ImageIcon GRATICULE(boolean selected) {
		int[][] map = {
			{0,0,0,0,0,1,0,0,1,0,0,0,0,0,0,0},
			{0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,1},
			{0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,1},
			{0,0,0,0,0,1,0,0,1,0,0,0,0,0,1,0},
			{0,0,0,0,1,0,0,0,0,1,0,0,0,0,1,0},
			{0,0,0,1,0,0,0,0,0,0,1,1,0,1,0,0},
			{0,0,1,0,0,0,0,0,0,0,0,0,1,1,0,0},
			{0,1,0,0,0,0,0,0,0,0,0,0,1,0,1,1},
			{1,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0},
			{0,1,0,0,0,0,0,0,0,0,0,1,0,0,0,0},
			{0,0,1,0,0,0,0,0,0,0,0,1,0,0,0,0},
			{0,0,0,1,0,0,0,0,0,0,1,0,0,0,0,0},
			{0,0,0,0,1,1,0,0,0,0,1,0,0,0,0,0},
			{0,0,0,0,0,0,1,1,0,1,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,1,1,0,0,0,0,0,0},
			{0,0,0,0,0,0,0,0,1,0,1,1,0,0,0,0}
		};
		return doIcon(map, selected);
	}
	static ImageIcon ZOOM_IN(boolean selected) {
		int[][] map = {
			{0,0,0,0,1,1,1,1,0,0,0,0,0,0,0,0},
			{0,0,1,1,1,2,2,1,1,1,0,0,0,0,0,0},
			{0,1,1,2,2,2,2,2,2,1,1,0,0,0,0,0},
			{0,1,2,2,2,1,1,2,2,2,1,0,0,0,0,0},
			{1,1,2,2,2,1,1,2,2,2,1,1,0,0,0,0},
			{1,2,2,1,1,1,1,1,1,2,2,1,0,0,0,0},
			{1,2,2,1,1,1,1,1,1,2,2,1,0,0,0,0},
			{1,1,2,2,2,1,1,2,2,2,1,1,0,0,0,0},
			{0,1,2,2,2,1,1,2,2,2,1,0,0,0,0,0},
			{0,1,1,2,2,2,2,2,2,1,1,0,0,0,0,0},
			{0,0,1,1,1,2,2,1,1,1,1,0,0,0,0,0},
			{0,0,0,0,1,1,1,1,0,0,1,1,1,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,1,1,1,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0},
		};
		return doIcon(map, selected);
	}
	static ImageIcon ZOOM_OUT(boolean selected) {
		int[][] map = {
			{0,0,0,0,1,1,1,1,0,0,0,0,0,0,0,0},
			{0,0,1,1,1,2,2,1,1,1,0,0,0,0,0,0},
			{0,1,1,2,2,2,2,2,2,1,1,0,0,0,0,0},
			{0,1,2,2,2,2,2,2,2,2,1,0,0,0,0,0},
			{1,1,2,2,2,2,2,2,2,2,1,1,0,0,0,0},
			{1,2,2,1,1,1,1,1,1,2,2,1,0,0,0,0},
			{1,2,2,1,1,1,1,1,1,2,2,1,0,0,0,0},
			{1,1,2,2,2,2,2,2,2,2,1,1,0,0,0,0},
			{0,1,2,2,2,2,2,2,2,2,1,0,0,0,0,0},
			{0,1,1,2,2,2,2,2,2,1,1,0,0,0,0,0},
			{0,0,1,1,1,2,2,1,1,1,1,0,0,0,0,0},
			{0,0,0,0,1,1,1,1,0,0,1,1,1,0,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,1,1,1,0,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,0},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1},
			{0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0},
		};
		return doIcon(map, selected);
	}
	static int black = Color.black.getRGB();
	static int white = (new Color(240,240,240)).getRGB();
	static ImageIcon doIcon(int[][] map, boolean selected) {
		BufferedImage im;
		im = new BufferedImage(map[0].length+4, map.length+4,
				BufferedImage.TYPE_INT_RGB);
		int color = white;
		if(selected) color = 0xffc0c0c0;
		for(int y=0 ; y<map[0].length+4 ; y++) {
			for( int x=0 ; x<map.length+4 ; x++) im.setRGB(x, y, color);
		}
		for(int y=2 ; y<map[0].length+2 ; y++) {
			for( int x=2 ; x<map.length+2 ; x++) {
				if(map[y-2][x-2] == 1) im.setRGB(x, y, black);
				else if(map[y-2][x-2] == 2) im.setRGB(x, y, white);
				else im.setRGB(x, y, color);
			}
		}
		return new ImageIcon(im);
	}
	static javax.swing.border.Border border = BorderFactory.createLineBorder(Color.black);
	static javax.swing.border.Border borderSel = BorderFactory.createLoweredBevelBorder();
/*
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		MapTools tools = new MapTools(null, null, null);
		frame.getContentPane().add(tools.getTools(), "Center");
		frame.pack();
		frame.show();
		Box box = Box.createHorizontalBox();
		ButtonGroup bg = new ButtonGroup();
		JToggleButton tb = new JToggleButton(SELECT(false));
		tb.setSelectedIcon(SELECT(true));
		tb.setBorder( border );
		bg.add(tb);
		tb.setSelected(true);
		box.add(tb);
		tb = new JToggleButton(ZOOM_IN(false));
		tb.setSelectedIcon(ZOOM_IN(true));
		tb.setBorder( border );
		bg.add(tb);
		box.add(tb);
		tb = new JToggleButton(ZOOM_OUT(false));
		tb.setSelectedIcon(ZOOM_OUT(true));
		tb.setBorder( border );
		bg.add(tb);
		box.add(tb);
		JButton b = new JButton(FOCUS(false));
		b.setToolTipText("focus map");
		b.setPressedIcon( FOCUS(true) );
		b.setBorder( border );
		bg.add(b);
		box.add(b);
		frame.getContentPane().add(box);
	}
*/

	private class ToggleToggler implements ActionListener, ChangeListener {
		boolean wasSelected;
		JToggleButton b,no;
		ButtonGroup bg;
		public ToggleToggler(JToggleButton b,JToggleButton no){
			this.b=b;
			this.no=no;
			b.addActionListener(this);
			b.addChangeListener(this);
			wasSelected=b.isSelected();
		}

		public void stateChanged(ChangeEvent e) {
			if (!b.isSelected()) wasSelected=false;
		}

		public void actionPerformed(ActionEvent e) {
			if (wasSelected) no.doClick();
				wasSelected=b.isSelected();
			}
	}


	public class TIFFFilter extends javax.swing.filechooser.FileFilter {
		//Accept all directories and all tiff files.
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return true;
			}
			String extension = f.getPath().substring( f.getPath().lastIndexOf(".") );
			if (extension != null) {
				if (extension.equals("tiff") || extension.equals("tif") ) {
					return true;
				} else {
					return false;
				}
			}
			return false;
		}
		//The description of this filter
		public String getDescription() {
			return "Just TIFF Images";
		}
	}
}
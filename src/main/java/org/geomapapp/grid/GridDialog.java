package org.geomapapp.grid;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Rectangle2D;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.geomapapp.gis.shape.ESRIShapefile;
import org.geomapapp.grid.Grid2D.Double;
import org.geomapapp.image.RenderingTools;
import org.geomapapp.util.Icons;
import org.geomapapp.util.XML_Menu;

import haxby.db.mb.PreviewCruise.CruiseGridViewer;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.proj.PolarStereo;
import haxby.util.BrowseURL;
import haxby.util.GeneralUtils;
import haxby.util.LayerManager.LayerPanel;

/**
 * Automatically reloads the grid when panning and zooming and is used for Global 
 * Topography, Gravity and GEOID.
 * @since 1.3.0
 * @version 2.5.0
 */

public class GridDialog implements ItemListener, WindowListener {

//	1.3.5: Attempting to implement loading of new grid
//	public final static String AGE = "Seafloor Bedrock Age (Muller 2008)";
	public final static String AGE = "Seafloor Crustal Age (Muller 2008 v3)";

//	1.7.8 Adding spreading rate and spreading asymmetry grids
//	public final static String SPREADING_RATE = "Spreading Rates (Muller 2008)";
//	public final static String SPREADING_ASYMMETRY= "Spreading Asymmetry (Muller 2008)";
	public final static String SPREADING_RATE = "Seafloor Spreading Rates (Muller 2008 v3)";
	public final static String SPREADING_ASYMMETRY= "Seafloor Spreading Rate Asymmetry (Muller 2008 v3)";

	public final static String DEM = "GMRT Grid Version " + MapApp.versionGMRT;
	public final static String GRAVITY = "Gravity (Sandwell and Smith 97)";
	public final static String GRAVITY_18 = "Gravity (Sandwell and Smith v18.1)";
	public final static String GEOID = "Geoid (Sandwell and Smith 97)";
	public final static String TOPO_9 = "Topography (Smith and Sandwell v9.1)";
	public final static String NASA_ELEV_MODEL = "NASA Elevation Model (USA 10m, World 30m, Ocean 900m)";
	public final static String DEV = "Development Tiles Grid";

	public final static String AGE_UNITS = "mY";
	public final static String SPREADING_RATE_UNITS = "mm/a";
	public final static String SPREADING_ASYMMETRY_UNITS = "%";
	public final static String GRAVITY_UNITS = "mgal";
	public final static String GEOID_UNITS = "m";
	public final static String DEM_UNITS = "m";
	public final static String NASA_ELEV_MODEL_UNITS = "m";

	public Map<String, String> gridCmds;
	public static Map<String, String> GRID_CMDS = new HashMap<String, String>();
	public static Map<String, String> GRID_CMDS_M = new HashMap<String, String>();
	public static Map<String, String> GRID_CMDS_NP = new HashMap<String, String>();
	public static Map<String, String> GRID_CMDS_SP = new HashMap<String, String>();
	public static Map<String, GridLoader> GRID_LOADERS = new HashMap<String, GridLoader>(); 
	public static Map<String, String> GRID_UNITS = new HashMap<String, String>();
	public static Map<String, String> GRID_URL = new HashMap<String, String>();
	public static Map<String, String> GRID_SHORT_TO_LONG_NAMES = new HashMap<String, String>();
	static 
	{
		GRID_SHORT_TO_LONG_NAMES.put(DEM, "Global Multi-Resolution Topography (GMRT)");
		GRID_SHORT_TO_LONG_NAMES.put(GEOID, "Geoid Heights (Sandwell and Smith v9.2)");
		GRID_SHORT_TO_LONG_NAMES.put(GRAVITY, "Gravity Anomalies (Sandwell and Smith v97)");
	    GRID_SHORT_TO_LONG_NAMES.put(GRAVITY_18, "Gravity Anomalies (Sandwell and Smith v18.1)");
	    GRID_SHORT_TO_LONG_NAMES.put(NASA_ELEV_MODEL, "NASA-ASTER-USGS Elevation Model (USA 10m, World 30m Ocean 900m)");
	    GRID_SHORT_TO_LONG_NAMES.put(AGE, "Seafloor Crustal Age (Muller 2008 v3)");
	    GRID_SHORT_TO_LONG_NAMES.put(SPREADING_RATE, "Seafloor Spreading Rates (Muller 2008 v3)");
	    GRID_SHORT_TO_LONG_NAMES.put(SPREADING_ASYMMETRY, "Seafloor Spreading Rate Asymmetry (Muller 2008 v3)");
	    GRID_SHORT_TO_LONG_NAMES.put(TOPO_9, "Topography and Bathymetry (Smith and Sandwell v9.1b)");

		GRID_CMDS_NP.put("GMRTGridNCmd", DEM);
		GRID_CMDS_SP.put("GMRTGridSCmd", DEM);
		GRID_CMDS_M.put("GMRTGridGCmd", DEM);
		GRID_CMDS_M.put("GMRTGridMCmd", DEM);
		GRID_CMDS_M.put("GravitySS97Cmd", GRAVITY);
		GRID_CMDS_M.put("GravitySSv18NCmd", GRAVITY_18);
		GRID_CMDS_M.put("GravitySSv18SCmd", GRAVITY_18);
		GRID_CMDS_M.put("GravitySSv18MCmd", GRAVITY_18);
		GRID_CMDS_M.put("NASA_DEM_CMD", NASA_ELEV_MODEL);
		GRID_CMDS_M.put("GeoidSS97Cmd", GEOID);
		GRID_CMDS_M.put("TopoSSv9MCmd", TOPO_9);
		GRID_CMDS_M.put("AgeMuller2008Cmd", AGE);
		GRID_CMDS_M.put("SpreadingRateAsymmetryMuller2008Cmd", SPREADING_ASYMMETRY);
		GRID_CMDS_M.put("SpreadingRateMuller2008Cmd", SPREADING_RATE);

		GRID_CMDS.putAll(GRID_CMDS_M);
		GRID_CMDS.putAll(GRID_CMDS_SP);
		GRID_CMDS.putAll(GRID_CMDS_NP);
		
		GRID_UNITS.put(AGE, AGE_UNITS);
		GRID_UNITS.put(SPREADING_RATE, SPREADING_RATE_UNITS);
		GRID_UNITS.put(SPREADING_ASYMMETRY, SPREADING_ASYMMETRY_UNITS);
		GRID_UNITS.put(GRAVITY, GRAVITY_UNITS);
		GRID_UNITS.put(GRAVITY_18, GRAVITY_UNITS);
		GRID_UNITS.put(GEOID, GEOID_UNITS);
		GRID_UNITS.put(DEM, DEM_UNITS);
		GRID_UNITS.put(TOPO_9, DEM_UNITS);
		GRID_UNITS.put(NASA_ELEV_MODEL, NASA_ELEV_MODEL_UNITS);

		GRID_URL.put(AGE, "https://www.ngdc.noaa.gov/mgg/ocean_age/ocean_age_2008.html");
		GRID_URL.put(SPREADING_RATE, "https://www.ngdc.noaa.gov/mgg/ocean_age/ocean_age_2008.html");
		GRID_URL.put(SPREADING_ASYMMETRY, "https://www.ngdc.noaa.gov/mgg/ocean_age/ocean_age_2008.html");
		GRID_URL.put(GRAVITY, "https://topex.ucsd.edu/marine_grav/mar_grav.html");
		GRID_URL.put(GRAVITY_18, "https://topex.ucsd.edu/marine_grav/mar_grav.html");
		GRID_URL.put(GEOID, "http://www.ngdc.noaa.gov/mgg/bathymetry/predicted/explore.HTML");
		GRID_URL.put(TOPO_9, "https://topex.ucsd.edu/WWW_html/mar_topo.html");
		GRID_URL.put(NASA_ELEV_MODEL, "https://asterweb.jpl.nasa.gov/gdem.asp");

		GRID_LOADERS.put(DEM, new GridLoader() {
			public void loadGrid(Grid2DOverlay grid) {
				GridComposer.getGrid(grid.getMap().getClipRect2D(), grid, 512);
			}
		});
		GRID_LOADERS.put(GRAVITY, new GridLoader() {
			public void loadGrid(Grid2DOverlay grid) {
				SSGridComposer.getGrid(grid.getXMap().getClipRect2D(), grid, 1, 512);
			}
		});
		GRID_LOADERS.put(GRAVITY_18, new GridLoader() {
			public void loadGrid(Grid2DOverlay grid) {
				SSGridComposer.getGrid(grid.getXMap().getClipRect2D(), grid, 4, 512);
			}
		});
		GRID_LOADERS.put(GEOID, new GridLoader() {
			public void loadGrid(Grid2DOverlay grid) {
				SSGridComposer.getGrid(grid.getXMap().getClipRect2D(), grid, 2, 512);
			}
		});
		GRID_LOADERS.put(TOPO_9, new GridLoader() {
			public void loadGrid(Grid2DOverlay grid) {
				SSGridComposer.getGrid(grid.getXMap().getClipRect2D(), grid, 3, 512);
			}
		});
		GRID_LOADERS.put(AGE, new GridLoader() {
			public void loadGrid(Grid2DOverlay grid) {
				SSGridComposer.getGrid(grid.getXMap().getClipRect2D(), grid, 5, 512);
			}
		});
		GRID_LOADERS.put(SPREADING_ASYMMETRY, new GridLoader() {
			public void loadGrid(Grid2DOverlay grid) {
				SSGridComposer.getGrid(grid.getXMap().getClipRect2D(), grid, 6, 512);
			}
		});
		GRID_LOADERS.put(SPREADING_RATE, new GridLoader() {
			public void loadGrid(Grid2DOverlay grid) {
				SSGridComposer.getGrid(grid.getXMap().getClipRect2D(), grid, 7, 512);
			}
		});
		GRID_LOADERS.put(NASA_ELEV_MODEL, new GridLoader() {
			public void loadGrid(Grid2DOverlay grid) {
				XMap map = grid.getXMap();
				NASAGridComposer.getNASAGrid(map.getClipRect2D(), grid, 512, map.getZoom());
			}
		});
	}

//	1.3.5: "DEM" changed to topography
//	public final static String DEM = "DEM";

	Vector<Grid2DOverlay> grids;
	public JComboBox<Grid2DOverlay> gridCB;
	JRadioButton color, pers;
	JFrame owner;

//	***** Changed by A.K.M. 06/26/06 *****
//	JDialog dialog;
//	Initialize as JFrame instead of JDialog to add minimization capability
	JFrame dialog;
//	***** Changed by A.K.M. 06/26/06 *****

	public JToggleButton gridTB;
	public boolean loaded = false;
	public JToggleButton toggleListLockUnlock;
	public Hashtable<String, Grid2DOverlay> gridCBElements;
	Hashtable<Grid2DOverlay, MultiGrid> mGrids;
	MultiGrid mGrid;
	private Grid2DOverlay disposedGrid = null;
	private boolean loading = false;
	
	public GridDialog( JFrame owner ) {
		this.owner = owner;
		grids = new Vector<Grid2DOverlay>();
		gridCB = new JComboBox<Grid2DOverlay>();
		gridTB = new JToggleButton(Icons.getIcon(Icons.GRID, false));
		gridTB.setBorder( BorderFactory.createEmptyBorder() );
		gridTB.setSelectedIcon( Icons.getIcon(Icons.GRID, true));
		gridTB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if( gridTB.isSelected() ) {
					showDialog();				
					Grid2DOverlay grid = gridCBElements.get(GRID_SHORT_TO_LONG_NAMES.get(GridDialog.DEM));
					grid.getMap().addOverlay(grid.name,grid);
					for (int i = 0 ; i < gridCB.getItemCount(); i++) {
						if ((Grid2DOverlay)gridCB.getItemAt(i) == grid) {
							gridCB.setSelectedIndex(i);
						}
					}
					grid.getRenderer().oceanB.setSelected(true);
					startGridLoad();
				}
				else {
					Grid2DOverlay grid = gridCBElements.get(GRID_SHORT_TO_LONG_NAMES.get(GridDialog.DEM));
					dispose(grid.name);
				}
			}
		});
		gridTB.setToolTipText("Load/Unload GMRT Grid");
		mGrids = new Hashtable<Grid2DOverlay, MultiGrid>();
		gridCBElements = new Hashtable<String, Grid2DOverlay>();
	}

	public JToggleButton getToggle() {
		return gridTB;
	}

	//Global grids
	public void showDialog() {
		if ( dialog == null ) {
			initDialog(null);
		}
		if (!dialog.isVisible()) {
			Point point = owner.getLocation();
			point.y = owner.getHeight() + point.y + 10;
			Rectangle screenBounds = owner.getGraphicsConfiguration().getDevice().getDefaultConfiguration().getBounds();
			if(point.y + dialog.getHeight() > screenBounds.y + screenBounds.height) {
				point.y = screenBounds.y + screenBounds.height - dialog.getHeight();
			}
			dialog.setLocation(point);
		}
		dialog.setVisible(true);
	}
	
	//Contributed Grids
	public void showDialog( String gridName, MultiGrid mGrid ) {
			
		mGrids.put( mGrid.grid, mGrid );

		//Initialize gridCBElements and add grid to gridCBElements
		gridCBElements.put( mGrid.grid.name, mGrid.grid );
		
		initDialog(gridName);

		if (!dialog.isVisible()) {
			Point point = owner.getLocation();
			point.y += owner.getHeight() + 10;
			Rectangle screenBounds = owner.getGraphicsConfiguration().getDevice().getDefaultConfiguration().getBounds();
			if(point.y + dialog.getHeight() > screenBounds.y + screenBounds.height) {
				point.y = screenBounds.y + screenBounds.height - dialog.getHeight();
			}
			dialog.setLocation(point); //sets frame location
		}
		dialog.setVisible(true);
	}
	
	
	void initDialog(String gridName) {
//		Grid loads if no grid is already loaded, otherwise the dialog
//		window just pops up without the grid being reloaded
		if( dialog!=null ) {

//			***** GMA 1.4.8: Show loading bar
//			loadGrid();
			startGridLoad();
//			***** GMA 1.4.8
			return;
		}

		ChangeListener cl = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				switchPanel();
			}
		};
		if( owner==null && gridCB.getItemCount()!=0 ) {
			Grid2DOverlay grid = (Grid2DOverlay)gridCB.getItemAt(0);
			owner = (JFrame)grid.getMap().getTopLevelAncestor();
		}

		dialog = new JFrame("Loaded Grids");

		dialog.addWindowListener(this);
		dialog.addComponentListener(new ComponentListener() {

			volatile Timer resizeTimer;

			public void componentMoved(ComponentEvent evt) {
			}

			@Override
			public void componentHidden(ComponentEvent arg0) {
			}

			@Override
			public void componentResized(ComponentEvent e) {
				if(null == resizeTimer || !resizeTimer.isRunning()) {
					resizeTimer = new Timer(100, new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent event) {
							dialog.pack();
							//dialog.repaint();
							resizeTimer.stop();
						}
					});
				}
				resizeTimer.restart();
			}

			@Override
			public void componentShown(ComponentEvent e) {
			}
		});
		dialog.setLocationRelativeTo(owner);

		Box panel = Box.createHorizontalBox();

		JButton info = new JButton( Icons.getIcon(Icons.INFO, false) );
		info.setSelectedIcon( Icons.getIcon(Icons.INFO, true) );
		info.setBorder( BorderFactory.createEmptyBorder(1,1,1,1));
		panel.add(info);
		info.addActionListener( new ActionListener() {
		public void actionPerformed(ActionEvent e) {
				showInfo();
			}
		});
		panel.add( Box.createHorizontalStrut(10) );
		
		gridCB.setToolTipText( "Select a Data Type");

		panel.add( gridCB );
		gridCB.addItemListener( this );

		if ( gridName != null ) {
			gridCB.setSelectedItem( gridCBElements.get(gridName) );
		}

		Border b = BorderFactory.createCompoundBorder(
				BorderFactory.createCompoundBorder(
					BorderFactory.createEmptyBorder(1,1,1,1),
					BorderFactory.createEtchedBorder()),
				BorderFactory.createEmptyBorder(1,1,1,1));

		panel.add( Box.createHorizontalStrut(10) );

		// Add lock to stop zoom if toggling between list of grids to display
		toggleListLockUnlock = new JToggleButton(Icons.getIcon(Icons.LOCK, false));
		toggleListLockUnlock.setBorder( BorderFactory.createEmptyBorder(1,1,1,1));
		toggleListLockUnlock.setToolTipText("Click to unlock displayed map area");
		toggleListLockUnlock.setSelectedIcon(Icons.getIcon(Icons.UNLOCK, false)); // Startup default position is locked
		panel.add( toggleListLockUnlock );

		toggleListLockUnlock.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(toggleListLockUnlock.isSelected()) {
					toggleListLockUnlock.setToolTipText("Click to lock displayed map area");
				} else {
					toggleListLockUnlock.setToolTipText("Click to unlock displayed map area");
				}
			}
		});
		
		JButton save = new JButton( Icons.getIcon(Icons.SAVE, false) );
		save.setPressedIcon( Icons.getIcon(Icons.SAVE, true) );
		save.setBorder( BorderFactory.createEmptyBorder(1,1,1,1));
		panel.add( save );
		save.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				save();
			}
		});

//		Changed text to reload because grid is now loaded when dialog is 
//		opened
		JButton gridB = new JButton("Reload");
		gridB.setBorder( b );
		panel.add( gridB );
		gridB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				startGridLoad();
			}
		});

		JButton fillB = new JButton("Fill In");
		fillB.setBorder( b );
		fillB.setToolTipText("fill data gaps");
		fillB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fill();
			}
		});

		JButton disposeB = new JButton("Close");
		disposeB.setBorder( b );
		disposeB.setToolTipText("Close Window and Dispose Grid");
		panel.add( disposeB );
		disposeB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				closeGrid();
			}
		});

		panel.add( Box.createHorizontalStrut(10) );
		ButtonGroup bg = new ButtonGroup();

		color = new JRadioButton("2D ");
		color.setToolTipText( "Color Palette Tool");
		pers = new JRadioButton("3D ");
		pers.setToolTipText("3D Perspective Tool");
		panel.add( color );
		panel.add( pers );

		bg.add( color );
		bg.add( pers );
		color.setSelected(true);
		color.addChangeListener( cl );
		JPanel toolPanel = new JPanel(new BorderLayout());
		toolPanel.add(panel,"West");
		// Add top tool panel
		dialog.getContentPane().add( toolPanel,"North");
		dialog.pack();
		dialog.addWindowListener( new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				if (gridTB.isSelected()) {
					gridTB.doClick();
				}
			}
		});
		
		// only zoom a contributed grid if user has not zoomed past default of 1
		Grid2DOverlay grid = (Grid2DOverlay)gridCB.getSelectedItem();
		XMap map = grid.getMap();

		switchPanel();

//		***** Automatically adjust Color bar when different grid is selected
		if ( map != null && map.getApp() instanceof MapApp && loaded ) {
			((MapApp)map.getApp()).initializeColorScale();
		}
	}
	void showInfo() {
		Grid2DOverlay grid = (Grid2DOverlay)gridCB.getSelectedItem();
		String url;

		if (isContributedGrid(grid.name)) {
			XMap map = grid.getMap();
			ESRIShapefile overlay = (ESRIShapefile) ((MapApp)map.getApp()).layerManager.getOverlay(grid.name);
			
			if (overlay != null) {
				url = overlay.getInfoURL();
				if (url != null)
					BrowseURL.browseURL(url);
			}
		} else {
			if( grid.toString().equals(DEM)) {
				try {
					grid.getMap().getCredit().showInfo();
				}catch(Exception e) {
				}
			} else {
				url = GRID_URL.get(grid.toString());
				if (url != null)
					BrowseURL.browseURL(url);
			}
		}
	}

//	1.3.5: disposeGrid() now public to allow other classes
//	to call it
	public void disposeGrid() {
		if( gridCB.getItemCount()==0 )return;
		Grid2DOverlay grid = (Grid2DOverlay)gridCB.getSelectedItem();
		haxby.map.XMap map = grid.getMap();

		map.removeOverlay(grid);
		grid.dispose();
		
		for ( Enumeration<String> e = gridCBElements.keys(); e.hasMoreElements(); ) {
			String currentKey = (String)e.nextElement();
			if (!isContributedGrid(currentKey)) continue;
			//set the layer visibility to false in the layer manager
			((MapApp)map.getApp()).layerManager.setLayerVisible(((MultiGrid)mGrids.get(currentKey)).shape,false);
			//Removes the Layer Panel
			((MapApp)map.getApp()).layerManager.removeLayerPanel(((MultiGrid)mGrids.get(currentKey)).shape);
			dispose(gridCBElements.get(currentKey));
		}
		
		loaded = false;
		if (map.getApp() instanceof MapApp)
			((MapApp)map.getApp()).autoFocus();
		map.repaint();
		if ( dialog != null ) {
			switchPanel();
		}
		System.gc();

//		***** GMA 1.6.6: Automatically adjust Color bar when Grid Options window is disposed

		if ( map != null && map.getApp() instanceof MapApp) {
			((MapApp)map.getApp()).initializeColorScale();
		}
//		***** GMA 1.6.6
	}
	
	/*
	 * To close down one of the pre-loaded global grids
	 */
	private void closeGrid() {
		if (gridCB.getSelectedItem() == null) return;
		Grid2DOverlay grid = (Grid2DOverlay) gridCB.getSelectedItem();
		if( grid==null )return;
		List<LayerPanel> layerPanels = ((MapApp)grid.getMap().getApp()).layerManager.getLayerPanels();
		for (LayerPanel lp : layerPanels) {
			if (lp.layerName.equals(grid.name) || lp.layerName.equals(GRID_SHORT_TO_LONG_NAMES.get(grid.name)) 
					|| lp.layerName.equals(getShortName(grid.name))) {
				((MapApp)grid.getMap().getApp()).layerManager.doRemove(lp);
				return;
			}
		}
	}
	
	/*
	 * Based on the old disposeChosenGrid from GridLayerDialog for contributed grids
	 */
	public void dispose(String gridToRemoveName) {
		if ( gridToRemoveName != null ) {
			XMap map = ((Grid2DOverlay)gridCB.getSelectedItem()).getMap();
			
			Grid2DOverlay gridToRemove = (Grid2DOverlay)gridCBElements.get(gridToRemoveName);
			if (gridToRemove == null) {
				gridToRemove = (Grid2DOverlay)gridCBElements.get(GRID_SHORT_TO_LONG_NAMES.get(gridToRemoveName));
			}
			if ( mGrids.containsKey(gridToRemove) ) {
				mGrids.remove(gridToRemove);
			}
			//contributed grids only
			if ( isContributedGrid(gridToRemoveName) ) {
				
				//Set units to null
				org.geomapapp.gis.shape.ESRIShapefile.nullUnits();
				
				// Remove reference to saving grid
				((MapApp)map.getApp()).setToolsGrid(null);
				if (gridToRemove != null) {
					gridToRemove.dispose();
					disposedGrid = gridToRemove;
					gridCBElements.remove(gridToRemoveName);
					gridCB.removeItem(gridToRemove);
				}
			} else {
				((MapApp)map.getApp()).setToolsGrid(null);
				if (gridToRemove != null) {
					gridToRemove.dispose();
					gridToRemove.getMap().removeOverlay(gridToRemove);
				}
			}
			
			//If closing GMRT grid, deselect the toggle button
			if ((gridToRemoveName.equals(DEM) ||gridToRemoveName.equals(GRID_SHORT_TO_LONG_NAMES.get(DEM)))) {
				gridTB.setSelected(false);
			}
			
			currentGrid = gridCB.getSelectedIndex();
			//Check the layer manager for the next grid.
			//If no more, then close down the dialog.
			List<LayerPanel> layerPanels = ((MapApp)(map.getApp())).layerManager.getLayerPanels();
			for (LayerPanel lp : layerPanels) {
				if (lp.layer instanceof Grid2DOverlay) {
					Grid2DOverlay gotoGrid = ((Grid2DOverlay) lp.layer);
					if (gotoGrid != gridToRemove && !gotoGrid.name.equals(MapApp.baseFocusName)) {
						gridCB.setSelectedItem(gotoGrid);
						switchGrid();
						return;
					}
				} else if (lp.layer instanceof ESRIShapefile && ((ESRIShapefile)lp.layer).getMultiGrid() != null ) {
					Grid2DOverlay gotoGrid = ((ESRIShapefile)lp.layer).getMultiGrid().grid;
					if (gotoGrid != gridToRemove) {
						gridCB.setSelectedItem(gotoGrid);
						switchGrid();
						return;
					}
				}

			}
			toggleListLockUnlock = null;
			dialog.dispose();
			loaded = false;
			if ( map != null && map.getApp() instanceof MapApp) {
				((MapApp)map.getApp()).initializeColorScale();
			}
			map.repaint();
		}
		
	}
	
	
	/*
	 * This version works better if grids have the same name
	 */
	public void dispose(Grid2DOverlay gridToRemove) {
		if ( gridToRemove != null ) {
			String gridToRemoveName = gridToRemove.name;
					
			XMap map = ((Grid2DOverlay)gridCB.getSelectedItem()).getMap();

			//contributed grids only
			if ( isContributedGrid(gridToRemove.name) ) {
				
				//Set units to null
				org.geomapapp.gis.shape.ESRIShapefile.nullUnits();
				
				// Remove reference to saving grid
				((MapApp)map.getApp()).setToolsGrid(null);
				if (gridToRemove != null) {
					gridToRemove.dispose();
					disposedGrid = gridToRemove;
//					gridCBElements.remove(gridToRemoveName);
					gridCB.removeItem(gridToRemove);
				}
			} else {
				((MapApp)map.getApp()).setToolsGrid(null);
				if (gridToRemove != null) {
					gridToRemove.dispose();
					gridToRemove.getMap().removeOverlay(gridToRemove);
				}
			}
			
			//If closing GMRT grid, deselect the toggle button
			if ((gridToRemoveName.equals(DEM) ||gridToRemoveName.equals(GRID_SHORT_TO_LONG_NAMES.get(DEM)))) {
				gridTB.setSelected(false);
			}
			
			currentGrid = gridCB.getSelectedIndex();
			//Check the layer manager for the next grid.
			//If no more, then close down the dialog.
			List<LayerPanel> layerPanels = ((MapApp)(map.getApp())).layerManager.getLayerPanels();
			for (LayerPanel lp : layerPanels) {
				if (lp.layer instanceof Grid2DOverlay) {
					Grid2DOverlay gotoGrid = ((Grid2DOverlay) lp.layer);
					if (gotoGrid != gridToRemove && !gotoGrid.name.equals(MapApp.baseFocusName)) {
						gridCB.setSelectedItem(gotoGrid);
						switchGrid();
						return;
					}
				} else if (lp.layer instanceof ESRIShapefile && ((ESRIShapefile)lp.layer).getMultiGrid() != null ) {
					Grid2DOverlay gotoGrid = ((ESRIShapefile)lp.layer).getMultiGrid().grid;
					if (gotoGrid != gridToRemove) {
						gridCB.setSelectedItem(gotoGrid);
						switchGrid();
						return;
					}
				}

			}
			toggleListLockUnlock = null;
			dialog.dispose();
			loaded = false;
			if ( map != null && map.getApp() instanceof MapApp) {
				((MapApp)map.getApp()).initializeColorScale();
			}
			map.repaint();
		}
		
	}
	
	void save() {
		haxby.map.XMap map = ((Grid2DOverlay)gridCB.getSelectedItem()).getMap();
		((MapApp)map.getApp()).save();
	}
	void fill() {
		if( gridCB.getItemCount()==0 )return;
		Grid2DOverlay grid = (Grid2DOverlay)gridCB.getSelectedItem();
		grid.fillNaNs();
	}

	public void startGridLoad() {
		Grid2DOverlay grid = (Grid2DOverlay)gridCB.getSelectedItem();
		MapApp app = (MapApp) grid.getMap().getApp();

		app.addProcessingTask(grid.name, 
			new Runnable() {
				public void run() {
					Grid2DOverlay grid = (Grid2DOverlay)gridCB.getSelectedItem();

					if ( dialog != null ) {
						dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					}

					try {
						loadGrid();
						//When the grid is loaded hide the load dialog
						if ( grid.lut.contourB.isSelected() )	{
							grid.contour.contour( grid.interval, grid.bolding_interval, grid.cb );
							grid.contour.setVisible( true );
							grid.getMap().repaint();
						}
					}
					finally {
						if ( dialog != null ) {
							dialog.setCursor(Cursor.getDefaultCursor());
						}
						
						// if currently using survey planner, update the elevations.
						if (app.getCurrentDB() != null && app.getCurrentDB().getClass() == haxby.db.surveyplanner.SurveyPlanner.class) {
							((haxby.db.surveyplanner.SurveyPlanner)app.getCurrentDB()).spSel.updateSurveyPlanner();
						}
					}
				}
			});
	}

	public void loadGrid() {
		if( gridCB.getItemCount()==0 )return;
		Grid2DOverlay grid = (Grid2DOverlay)gridCB.getSelectedItem();
		XMap map = grid.getMap();
		
		if (isContributedGrid(grid.name) && map.getZoom() <= 1 && 
				(toggleListLockUnlock == null || (toggleListLockUnlock != null && toggleListLockUnlock.isSelected()))) {
			// Zoom to location of grid on map so that layer loads quickly
			zoomTo();
		}
		
		boolean merc =  map.getProjection() instanceof haxby.proj.Mercator;
		
		if (!isContributedGrid(grid.name)) {
			//global grids
			
			//add grid to Layer Manager here only if global grid.
			//contributed grids get added in ShapeSuite.addShapeFile
			if ( !map.hasOverlay(grid) ) {
				map.addOverlay(grid.name,grid);
			}
			
			if( !merc && !grid.toString().equals(GridDialog.DEV)  && !(grid instanceof CruiseGridViewer)) {
				int which = ((MapApp) map.getApp()).getMapType();
				if (which == MapApp.SOUTH_POLAR_MAP)
					GridComposer.getGridSP(map.getClipRect2D(), grid, 512);
				else if (which == MapApp.NORTH_POLAR_MAP)
					GridComposer.getGridNP(map.getClipRect2D(), grid, 512);

			} else {
				GRID_LOADERS.get(grid.toString()).loadGrid(grid);
			}
		} else {
			//contributed grids
			int res=2;
			while( res < map.getZoom()*1.4 ) res *= 2;

			Grid2D grd;
			MultiGrid mg = ((MultiGrid)mGrids.get( grid ));
			if (merc)
				grd = mg.getGrid( res, map.getClipRect2D());
			else {
				boolean southPole =
					((PolarStereo) map.getProjection()).getHemisphere() == PolarStereo.SOUTH;
				grd = mg.getGridPolar(res, map.getClipRect2D(), southPole);
			}

			if ( grd != null ) {
				Rectangle r = grd.getBounds();
				Grid2D.Boolean land = new Grid2D.Boolean( r, grd.getProjection() );
				boolean hasLand = false;
				boolean hasOcean = false;
				for( int y=r.y ; y<r.y+r.height ; y++) {
					for( int x=r.x ; x<r.x+r.width ; x++) {
						double val = grd.valueAt(x,y);
						if( Double.isNaN(val) )continue;
						if( val>=0. ) {
							hasLand=true;
							land.setValue( x, y, true);
						} else hasOcean=true;
					}
				}
				grid.setGrid( grd, land, hasLand, hasOcean);
			}
		}
		
		if ( dialog != null ) {
			switchPanel();
		}
		loaded = true;
		disposedGrid = null;
		
//		Automatically adjust Color bar when different grid is selected
		if ( map != null && map.getApp() instanceof MapApp && loaded) {
			((MapApp)map.getApp()).initializeColorScale();
		}

	}

	public void switchGrid() {
		if( gridCB.getItemCount()==0 )return;
		//get the currently selected grid
		Grid2DOverlay grid = (Grid2DOverlay)gridCB.getSelectedItem();
		switchGrid(grid, null);
	}
	
	public void switchGrid(Grid2DOverlay grid, XML_Menu menu) {
		loading = true;
		switchPanel();
		loading = false;
		haxby.map.XMap map = grid.getMap();

		if (grid.toString().equals(GridDialog.DEM) ||
				grid.toString().equals(GridDialog.DEV) ||
				grid.toString().equals(GridDialog.NASA_ELEV_MODEL))
			 grid.getRenderer().oceanB.setSelected(true);
		
		if (grid.toString().equals(GridDialog.DEM)) gridTB.setSelected(true);

		String units;
		if (!isContributedGrid(grid.name)) {
			//add grid to Layer Manager here only if global grid.
			//contributed grids get added in ShapeSuite.addShapeFile
			if ( !map.hasOverlay(grid) ) {
				map.addOverlay(grid.name,grid,menu);
			} else {
				((MapApp)map.getApp()).layerManager.moveToTop(grid);
			}
			units = GRID_UNITS.get(grid.toString());
			if (units != null)
				map.setUnits(units);
			map.setDataType(grid.getDataType());
		} 
		
		// If locked don't zoom when switching grids.
		if(toggleListLockUnlock != null && toggleListLockUnlock.isSelected()) {
			zoomTo();
		}
		map.repaint();
	}
	
	int currentGrid = -1;
	JComponent currentPanel;
	JComponent defaultPanel = new JComponent() {
		private static final long serialVersionUID = 1L;
		public Dimension getPreferredSize() {
			return new Dimension(400, 50);
		}
		public void paint(Graphics g) {
			g.drawString("Grid falls outside displayed map area", 10, 40);
		}
	};
	JComponent loadingPanel = new JComponent() {
		private static final long serialVersionUID = 1L;
		public Dimension getPreferredSize() {
			return new Dimension(400, 50);
		}
		public void paint(Graphics g) {
			g.drawString("Loading grid", 10, 40);
		}
	};
	
	void switchPanel() {
		if( gridCB.getItemCount()==0 )return;
		Grid2DOverlay grid = (Grid2DOverlay)gridCB.getSelectedItem();	
		
		// Set Map Tools Save grid
		((MapApp) grid.getMap().getApp()).setToolsGrid(grid);
	
		if (currentGrid != -1 && grid != gridCB.getItemAt(currentGrid)) {
			dialog.getContentPane().remove( currentPanel );
		}
		try {
			RenderingTools tools = grid.getRenderer();
			tools.setParentFrame(dialog);
			JComponent tool;
			if (grid.getGrid() == null || tools == null || color == null) {
				if (loading) {
					tool = loadingPanel; // display "loading" message
				} else {
					tool = defaultPanel; // display "out of area" message
				}
			} else {
				tool = tools.getTool( color.isSelected() ? "color" : "3D");
			}

			// Add main window view
			if (currentPanel != tool) {
				if( currentPanel!=null )
					dialog.getContentPane().remove( currentPanel );
				dialog.getContentPane().add(tool, "Center");
			}
			
			currentPanel = tool;
			currentGrid = gridCB.getSelectedIndex();
			
		} catch(Exception e) {
			currentPanel = defaultPanel;
			dialog.getContentPane().add( currentPanel, "Center");
			e.printStackTrace();
		}
		
		Dimension dim = dialog.getSize();
		dialog.pack();
		if( currentPanel!=grid.getRenderer()) dialog.setSize(dim);
		Point dloc = dialog.getLocation();
		Rectangle screenBounds = dialog.getGraphicsConfiguration().getDevice().getDefaultConfiguration().getBounds();
		if(dloc.y + dialog.getHeight() > screenBounds.y + screenBounds.height) {
			dloc.y = screenBounds.y + screenBounds.height - dialog.getHeight();
		}
		dialog.setLocation(dloc);
		dialog.toFront();
		dialog.repaint();
	}
	public void addGrid( Grid2DOverlay grid ) {
		gridCB.addItem( grid );
		if (GRID_SHORT_TO_LONG_NAMES.get(grid.name) == null) {
			gridCBElements.put(grid.name, grid);
		} else {
			gridCBElements.put(GRID_SHORT_TO_LONG_NAMES.get(grid.name), grid);
		}
	}
	
	//addGrid for contributed grids
	public void addGrid( Grid2DOverlay grid, MultiGrid mGrid ) {
		if ( !mGrids.containsKey(mGrid.grid) ) {
			mGrids.put( mGrid.grid, mGrid );
		}
		if ( !gridCBElements.containsKey(mGrid.grid) ) {
			gridCBElements.put( mGrid.grid.name, mGrid.grid );
			System.out.println("Adding to combo box");
		}
		if ( gridCB.getItemCount() == 0 ) {
			gridCB.addItem(mGrid.grid);
		}
		else {
			boolean doNotAdd = false;
			for ( int i = 0; i < gridCB.getItemCount(); i++ ) {
				if ( gridCB.getItemAt(i).equals(mGrid.grid) ) {
					doNotAdd = true;
				}
			}
			if ( !doNotAdd ) {
				//add new grid to top of combo box
				gridCB.insertItemAt(mGrid.grid, 0);
			}
		}
		gridCB.setSelectedItem( mGrid.grid);		
		loadGrid();
	}
	
	public void setSelectedGrid(Grid2DOverlay grid) {
		gridCB.setSelectedItem(grid);
	}
	
	public Grid2DOverlay[] getGrids() {
		Grid2DOverlay[] grids = new Grid2DOverlay[gridCB.getItemCount()];
		for( int k=0 ; k<gridCB.getItemCount() ; k++) {
			grids[k] = (Grid2DOverlay)gridCB.getItemAt(k);
		}
		return grids;
	}
	public boolean isLoaded() {
		return loaded;
	}

// Indicates whether the dialog is visible
	public boolean isDialogVisible() {
		if ( dialog == null ) {
			return false;
		}
		else {
			return dialog.isVisible();
		}
	}

//	***** Changed by A.K.M. 06/28/06 *****
//	If a new data type is selected in the combo box its 
//	corresponding grid will be loaded if it has not already 
//	been loaded
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == gridCB && e.getStateChange() == ItemEvent.SELECTED ) {
			if (disposedGrid == null){
				
				Grid2DOverlay grid = (Grid2DOverlay)gridCB.getSelectedItem();
				//contributed grids
				if (isContributedGrid(grid.name)) {
					ESRIShapefile shape = ((MultiGrid)mGrids.get(grid)).shape;
					String p = ((MultiGrid)mGrids.get(grid)).baseURL;
					try {
						shape.readUnits(p, "units.txt");
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					}
					grid.getMap().removeOverlay( shape );
					grid.getMap().addOverlay(shape.getName(), shape.getInfoURL(), shape);
				}
				switchGrid();
				startGridLoad();
			}
		}
	}
//	***** Changed by A.K.M. 06/28/06 *****

	public void windowActivated(WindowEvent we) {
	}
	public void windowClosed(WindowEvent we) {
	}
	public void windowClosing(WindowEvent we) {
		if ( we.getSource().equals(dialog) ) {
			if( gridCB.getItemCount() == 0 ) {
				return;
			}
			//close all open grids
			Grid2DOverlay grid = (Grid2DOverlay)gridCB.getItemAt(0);
			List<LayerPanel> layerpanels = ((MapApp)grid.getMap().getApp()).layerManager.getLayerPanels();
			LinkedList<LayerPanel> copyOfLayerPanels = new LinkedList<LayerPanel>();
			copyOfLayerPanels.addAll(layerpanels);
			for (LayerPanel lp : copyOfLayerPanels) {
				if ((lp.layer instanceof Grid2DOverlay || lp.layer instanceof ESRIShapefile) && !lp.layerName.matches(MapApp.baseFocusName)) 
					((MapApp)grid.getMap().getApp()).layerManager.doRemove(lp);				
			}
			disposeGrid();
		}
	}
	public void windowDeactivated(WindowEvent we) {
	}
	public void windowDeiconified(WindowEvent we) {
	}
	public void windowIconified(WindowEvent we) {
	}
	public void windowOpened(WindowEvent we) {
	}

	
	public String getUnits(String string) {
		if (isContributedGrid(string)) {
			//this part not currently in use (NSS 11/02/16)
			String units = org.geomapapp.gis.shape.ESRIShapefile.getUnits();
			if (units != null) return units;
			return("m");
		} else {
			return GRID_UNITS.get(string);
		}
	}

	public static interface GridLoader {
		public void loadGrid(Grid2DOverlay grid);
	}

	public void initMercatorGrids(XMap map) {
		gridCmds = GRID_CMDS_M;
		Grid2DOverlay g;
		// Alphabetized items added to JComboBox.
		g = new Grid2DOverlay(map, DEM);			//0
		addGrid( g );
		g = new Grid2DOverlay(map, GEOID);
		addGrid( g );
		g = new Grid2DOverlay(map, GRAVITY);
		addGrid( g );
		g = new Grid2DOverlay(map, GRAVITY_18);
		addGrid( g );
		g = new Grid2DOverlay(map, NASA_ELEV_MODEL);
		addGrid( g );
		g = new Grid2DOverlay(map, AGE);			//5
		g.setBackground( 0x00000000 );
		addGrid( g );
		g = new Grid2DOverlay(map, SPREADING_ASYMMETRY);
		g.setBackground( 0x00000000 );
		addGrid( g );
		g = new Grid2DOverlay(map, SPREADING_RATE);
		g.setBackground( 0x00000000 );
		addGrid( g );
		g = new Grid2DOverlay(map, TOPO_9);
		addGrid( g );
	}

	public void initSPGrids(XMap map) {
		gridCmds = GRID_CMDS_SP;
		Grid2DOverlay g = new Grid2DOverlay(map, DEM);
		addGrid( g );
	}

	public void initNPGrids(XMap map) {
		gridCmds = GRID_CMDS_NP;
		Grid2DOverlay g = new Grid2DOverlay(map, DEM);
		addGrid( g );
	}

	public boolean isGridCommand(String name, String menu_name) {
		
		//The Muller Global grids have their command as "shape_cmd"
		//in the menu xml file, so need to check specifically for them
		if (name.equals("shape_cmd") && gridCmds.containsValue(menu_name)) {
			return true;
		}
		return gridCmds.containsKey(name);
	}

	public void gridCommand(String cmd, XML_Menu menu) {
		String name = gridCmds.get(cmd);
		if (name == null) return;

		for (int i = 0; i < gridCB.getItemCount(); i++) {
			Object obj = gridCB.getItemAt(i);
			if (obj.toString().equals(name)) {
				Grid2DOverlay grid = gridCB.getItemAt(i);
				showDialog();
				switchGrid(grid, menu);
				gridCB.setSelectedIndex(i);
				if (!isLoaded())
					startGridLoad();
			}
		}
	}
	
	private boolean isContributedGrid(String gridName) {
		return !(gridCmds.containsValue(gridName) ||
				 gridCmds.containsValue(getShortName(gridName)));
	}
	
	/*
	 * Based on GridLayerDialog.zoomTo() for contributed grids
	 */
	void zoomTo() {
		Grid2DOverlay grid = (Grid2DOverlay)gridCB.getSelectedItem();
		if( grid==null ) {
			System.out.println("null");
			return;
		}
		MultiGrid tempMGrid = ((MultiGrid)mGrids.get( grid ));
		XMap m = grid.getMap();
		try {
			Rectangle2D.Double rect = null;
			Rectangle2D shape = (Rectangle2D)tempMGrid.shape.getShapes().get( 0 );
			double w = shape.getWidth();
			double h = shape.getHeight();
			rect = new Rectangle2D.Double( shape.getX()-.25*w, shape.getY()-.25*h, w*1.5, h*1.5 );
			// Tracks zoom before, does zoom, tracks zoom after
			m.setZoomHistoryPast(m);
			m.zoomToRect( rect );
			m.setZoomHistoryNext(m);
			m.repaint();
		} catch(Exception e) {}
	}
	
	/*
	 * Based on GridLayerDialog.refershGrid for contributed grids
	 */
	public void refreshGrid(ESRIShapefile shape) {
		if( gridCB.getItemCount()==0 )return;
		for ( int i = 0; i < gridCB.getItemCount(); i++ ) {
			final Grid2DOverlay grid = (Grid2DOverlay)gridCB.getItemAt(i);
			if (grid != null && isContributedGrid(grid.name) && ((MultiGrid)mGrids.get(grid)).shape != null) {
				if ( shape.equals(((MultiGrid)mGrids.get(grid)).shape) ) {
					final XMap map = grid.getMap();
					MapApp app = ((MapApp) map.getApp());

					app.addProcessingTask(grid.toString(), new Runnable() {
						public void run() {
							boolean merc =  map.getProjection() instanceof haxby.proj.Mercator;
							int res=2;
							while( res < map.getZoom()*1.4 ) res *= 2;

							Grid2D grd;
							if (merc)
								grd = ((MultiGrid)mGrids.get( grid )).getGrid( res, map.getClipRect2D());
							else {
								boolean southPole =
									((PolarStereo) map.getProjection()).getHemisphere() == PolarStereo.SOUTH;
								grd = ((MultiGrid)mGrids.get( grid )).getGridPolar(res, map.getClipRect2D(), southPole);
							}

							if ( grd != null ) {
								Rectangle r = grd.getBounds();
								Grid2D.Boolean land = new Grid2D.Boolean( r, grd.getProjection() );
								boolean hasLand = false;
								boolean hasOcean = false;

								for( int y=r.y ; y<r.y+r.height ; y++) {
									for( int x=r.x ; x<r.x+r.width ; x++) {
										double val = grd.valueAt(x,y);
										if( Double.isNaN(val) )continue;
						
										if( val>=0. ) {
											hasLand=true;
											land.setValue( x, y, true);
										} else hasOcean=true;
									}
								}
								grid.setGrid( grd, land, hasLand, hasOcean);
								if (grid.lut.contourB.isSelected()) {
									grid.contour.contour(grid.interval, grid.bolding_interval, grid.cb);
									grid.contour.setVisible(true);
								}
								switchPanel();
								loaded = true;
							}
							map.repaint();
						}
					});
				}
				return;
			}
		}
	}

	/*
	 * Based on GridLayerDialog.refreshGrids() for contributed grids
	 */
	public void refreshGrids() {
		if( gridCB.getItemCount()==0 )return;
		for ( int i = 0; i < gridCB.getItemCount(); i++ ) {
			final Grid2DOverlay grid = (Grid2DOverlay)gridCB.getItemAt(i);
			final XMap map = grid.getMap();
			MapApp app = ((MapApp) map.getApp());

			if (grid == null ||!app.layerManager.getLayerVisibleDefaultFalse(grid)) continue;

			app.addProcessingTask(grid.toString(), new Runnable() {
				public void run() {	
					boolean merc =  map.getProjection() instanceof haxby.proj.Mercator;
					if (!isContributedGrid(grid.name)) {
						//global grids
						if( !merc && !grid.toString().equals(GridDialog.DEV) && !(grid instanceof CruiseGridViewer)) {
							int which = ((MapApp) map.getApp()).getMapType();
							if (which == MapApp.SOUTH_POLAR_MAP)
								GridComposer.getGridSP(map.getClipRect2D(), grid, 512);
							else if (which == MapApp.NORTH_POLAR_MAP)
								GridComposer.getGridNP(map.getClipRect2D(), grid, 512);

						} else {
							GRID_LOADERS.get(grid.toString()).loadGrid(grid);
							switchPanel();
						}
						if (grid.lut.contourB.isSelected()) {
							grid.contour.contour(grid.interval, grid.bolding_interval, grid.cb);
							grid.contour.setVisible(true);
						}
						map.repaint();
					} else {
						//contributed grids
						int res=2;
						while( res < map.getZoom()*1.4 ) res *= 2;
	
						Grid2D grd;
						if (merc)
							grd = ((MultiGrid)mGrids.get( grid )).getGrid( res, map.getClipRect2D());
						else {
							boolean southPole =
								((PolarStereo) map.getProjection()).getHemisphere() == PolarStereo.SOUTH;
							grd = ((MultiGrid)mGrids.get( grid )).getGridPolar(res, map.getClipRect2D(), southPole);
						}
	
						if ( grd != null ) {
							Rectangle r = grd.getBounds();
							Grid2D.Boolean land = new Grid2D.Boolean( r, grd.getProjection() );
							boolean hasLand = false;
							boolean hasOcean = false;
							for( int y=r.y ; y<r.y+r.height ; y++) {
								for( int x=r.x ; x<r.x+r.width ; x++) {
									double val = grd.valueAt(x,y);
									if( Double.isNaN(val) )continue;
									if( val>=0. ) {
										hasLand=true;
										land.setValue( x, y, true);
									} else hasOcean=true;
								}
							}
							grid.setGrid( grd, land, hasLand, hasOcean);
							if (grid.lut.contourB.isSelected()) {
								grid.contour.contour(grid.interval, grid.bolding_interval, grid.cb);
								grid.contour.setVisible(true);
							}
							switchPanel();
							loaded = true;
							map.repaint();
							System.gc();
						}
					}
				}
			});
		}
	}
	
	/*
	 * Based on GridLayerDialog.getGrid() for contributed grids
	 */
	public Grid2DOverlay getGrid() {
		if ( gridCB.getSelectedItem() != null ) {
			return (Grid2DOverlay)gridCB.getSelectedItem();
		}
		else {
			return null;
		}
	}
	
	/*
	 * Get the GRID_CMD for a given grid name
	 */
	public String getCmd(String gridName) {
		return GeneralUtils.getKeyByValue(gridCmds, gridName);
	}
	
	/*
	 * Get the global grid's short for a given long name
	 */
	public static String getShortName(String longName) {
		return GeneralUtils.getKeyByValue(GRID_SHORT_TO_LONG_NAMES, longName);
	}
}
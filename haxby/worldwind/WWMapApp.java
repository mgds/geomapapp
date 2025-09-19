package haxby.worldwind;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.ScalebarLayer;
import gov.nasa.worldwind.layers.Earth.NASAWFSPlaceNameLayer; // was EarthNASAPlaceNameLayer
import gov.nasa.worldwind.layers.placename.PlaceNameLayer;
import gov.nasa.worldwind.render.SurfaceImage;
import haxby.db.Database;
import haxby.map.MapApp;
import haxby.map.StartUp;
import haxby.map.Zoomer;
import haxby.util.BrowseURL;
import haxby.util.LegendSupplier;
import haxby.util.SearchTree;
import haxby.util.URLFactory;
import haxby.util.WESNSupplier;
import haxby.worldwind.LayerManager.ILayer;
import haxby.worldwind.LayerManager.WWILayer;
import haxby.worldwind.awt.LassoSelectionHandler;
import haxby.worldwind.db.custom.WWCustomDB;
import haxby.worldwind.db.eq.WWEQ;
import haxby.worldwind.db.fms.WWFocalMechanismSolutionDB;
import haxby.worldwind.db.mb.WWMBTracks;
import haxby.worldwind.db.mgg.WWMGG;
import haxby.worldwind.db.pdb.WWPDB;
import haxby.worldwind.db.scs.WWSCS;
import haxby.worldwind.db.xmcs.WWXMCS;
import haxby.worldwind.fence.FenceDiagram;
import haxby.worldwind.fence.ImportFenceDiagram;
import haxby.worldwind.image.ImageResampler;
import haxby.worldwind.image.WWImportImageLayer;
import haxby.worldwind.layers.ColorScaleLayer;
import haxby.worldwind.layers.GeoMapAppMaskLayer;
import haxby.worldwind.layers.GridTileLayer;
import haxby.worldwind.layers.InfoSupplier;
import haxby.worldwind.layers.LayerComposer;
import haxby.worldwind.layers.MagneticAnomaliesLayer;
import haxby.worldwind.layers.OceanAgesSurfaceLayer;
import haxby.worldwind.layers.SedimentThicknessSurfaceLayer;
import haxby.worldwind.layers.SimpleRenderableLayer;
import haxby.worldwind.layers.SpreadingAsymmetrySurfaceLayer;
import haxby.worldwind.layers.SpreadingRateSurfaceLayer;
import haxby.worldwind.layers.SunCompassLayer;
import haxby.worldwind.layers.SurfaceImageLayer;
import haxby.worldwind.layers.WMSLayer;
import haxby.worldwind.layers.ColorScaleLayer.ColorScale;
import haxby.worldwind.layers.SunCompassLayer.SunAngle;
import haxby.worldwind.util.WWSearchTree;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;

import org.geomapapp.db.dsdp.DSDPDemo;
import org.geomapapp.grid.Grid2DOverlay;
import org.geomapapp.util.XML_Menu;
import org.xml.sax.SAXException;

public class WWMapApp extends MapApp implements PropertyChangeListener {
	public static final String VO_BASE_URL = "http://www.virtualocean.org/";
	private static final String WORLD_WIND_CACHE_NAME = "Earth";
	public final static String VERSION = "2.6.1"; // 03.13.17
	private static final String VO_NAME = "Virtual Ocean " + WWMapApp.VERSION;

	static {
		SUPPORTED_MAPS.add(0, Integer.valueOf(MapApp.WORLDWIND));
	}

	protected Model wwModel;
	protected WorldWindowGLCanvas wwCanvas;

	protected SunCompassLayer compassLayer;
	protected ColorScaleLayer colorScaleLayer;
	protected ScalebarLayer scalebarLayer;
	protected PlaceNameLayer placeNamesLayer; 

	protected JCheckBoxMenuItem scalebarMI;
	protected JCheckBoxMenuItem placeNamesMI;
	protected JCheckBoxMenuItem colorScaleMI;
	protected JCheckBoxMenuItem compassMI;

	protected JCheckBoxMenuItem layerManagerMI;
	protected JFrame layerManagerDialog;
	protected LayerManager layerManager;

	protected LassoSelectionHandler lassoSelectionHandler;

	public WWMapApp() {
		super();
	}

	public WWMapApp(int mapType) {
		super(mapType);
	}

	protected void WWInit() {
		JWindow startup = new JWindow();
		StartUp start = new StartUp(WORLDWIND);
		startup.getContentPane().add(start, "Center");
		startup.pack();
		startup.setLocationRelativeTo(MapApp.anchor);
		start.setText("Composing Globe");
		startup.setVisible(true);

		Configuration.setValue(AVKey.TEXTURE_IMAGE_CACHE_SIZE, 15 * 1000 * 1000);
		Configuration.setValue(AVKey.TEXTURE_CACHE_SIZE, 160 * 1000 * 1000);

		Configuration.setValue(AVKey.TASK_POOL_SIZE, 4);
		Configuration.setValue(AVKey.TASK_QUEUE_SIZE, 1);

//		Configuration.setValue(AVKey.GLOBE_CLASS_NAME, EarthFlat.class.getName());
//		Configuration.setValue(AVKey.VIEW_CLASS_NAME, FlatOrbitView.class.getName());

		wwCanvas = new WorldWindowGLCanvas();

		wwCanvas.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (!wwCanvas.hasFocus())
					wwCanvas.requestFocus();
			}
		});
		wwModel = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
		wwModel.setLayers(new LayerList( new Layer[] {} ));

		wwCanvas.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {
			}
			public void keyReleased(KeyEvent e) {
				if (e.isControlDown())
					if (e.getKeyCode() == KeyEvent.VK_L)
						setLayerManagerVisible(true);
			}
			public void keyPressed(KeyEvent e) {
			}
		});

		initLayerManager();

		compassLayer = new SunCompassLayer();
		layerManager.addIgnoredLayer(compassLayer);
		wwModel.getLayers().add(compassLayer);

		colorScaleLayer = new ColorScaleLayer();
		layerManager.addIgnoredLayer(colorScaleLayer);
		wwModel.getLayers().add(colorScaleLayer);

		scalebarLayer = new ScalebarLayer();
		layerManager.addIgnoredLayer(scalebarLayer);
		wwModel.getLayers().add(scalebarLayer);

		placeNamesLayer = new NASAWFSPlaceNameLayer();

		lassoSelectionHandler = new LassoSelectionHandler(wwCanvas);
		layerManager.addIgnoredLayer(lassoSelectionHandler);
		wwModel.getLayers().add(lassoSelectionHandler);

//		((FlatGlobe)wwModel.getGlobe()).setProjection(FlatGlobe.PROJECTION_MERCATOR);
//		Tessellator tessellator = new MYEBSRectangularTessellator();
//		wwModel.getGlobe().setTessellator(tessellator);

		wwCanvas.setModel(wwModel);

		map = new WWMap(this, wwCanvas);

		start.setText("Initializing GUI");
		CURRENT_PROJECTION = "g";
		initWWGUI();

		wwModel.addPropertyChangeListener(this);

		loadGeoMapAppMap();
		setLayerManagerVisible(false);
		
		start.setText("Initializing Zoomer");
		layerManager.setZoomer(new LayerManager.XMapZoomer(map));

		startup.dispose();
		start = null;

		frame.pack();
		frame.setSize( 1000, 750 );
		frame.setVisible(true);
	}

	@Override
	protected void checkVersion() {
		/**
		 * Don't update versions for WW
		 */
	}

	public void setLayerManagerVisible(boolean tf) {
		if (getMapType() == MapApp.WORLDWIND) {
			if (!layerManagerDialog.isVisible() && tf) {
				int x = this.getFrame().getLocation().x;
				x += this.getFrame().getWidth();
				x += 10;
				layerManagerDialog.setLocation(x, 
						layerManagerDialog.getLocation().y);
			}

			layerManagerMI.setSelected(tf);
			layerManagerDialog.setVisible(tf);
			if (tf)
				layerManagerDialog.requestFocus();
		}
		else {
			super.setLayerManagerVisible(tf);
		}
	}

	protected void toggleLayerManager() {
		layerManagerDialog.setVisible(layerManagerMI.isSelected());
	}

	protected void updateSunAngleProvider() {
		LayerList ll = wwModel.getLayers();
		for (int i = ll.size() - 1; i >= 0; i--) {
			Layer l = ll.get(i);
			if (l.isEnabled() && (l.getOpacity() > .4) &&
					true)
				for (Class<?> c : l.getClass().getInterfaces())
					if (c.equals( SunAngle.class ) && ((SunAngle) l).isSunValid()) {
						compassLayer.setAngleSupplier((SunAngle) l);
						return;
					}
		}
		compassLayer.setAngleSupplier(null);
	}

	protected void updateColorScaleProvider() {
		LayerList ll = wwModel.getLayers();
		for (int i = ll.size() - 1; i >= 0; i--) {
			Layer l = ll.get(i);
			if (l.isEnabled() && (l.getOpacity() > .2)) {
				for (Class<?> c : l.getClass().getInterfaces())
					if (c.equals( ColorScale.class ) && ((ColorScale) l).isColorScaleValid()) {
						colorScaleLayer.setScaleSupplier((ColorScale) l);
						return;
					}
			}
		}
		colorScaleLayer.setScaleSupplier(null);
	}

	protected void updateCreditButtonState() {
		LayerList ll = wwModel.getLayers();
		for (int i = ll.size() - 1; i >= 0; i--) {
			Layer l = ll.get(i);
			if (l.isEnabled() && (l.getOpacity() > .2)) {
				for (Class<?> c : l.getClass().getInterfaces())
					if (c.equals( InfoSupplier.class )) {
						((WWMapTools)tools).creditB.setEnabled(true);
						return;
					}
				((WWMapTools)tools).creditB.setEnabled(false);
				return;
			}
		}
		((WWMapTools)tools).creditB.setEnabled(false);
	}

	protected void showDSDP() {
		if (whichMap == WORLDWIND) {
			map.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			map.getTopLevelAncestor().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			if( dsdp==null ) dsdp=new DSDPDemo(this, new haxby.worldwind.db.dsdp.WWDSDP());
			dsdp.show();

			map.getTopLevelAncestor().setCursor(Cursor.getDefaultCursor());
			map.setCursor(Cursor.getDefaultCursor());
		} else
			super.showDSDP();
	}

	public void actionPerformed(ActionEvent evt) throws OutOfMemoryError {
		String name = evt.getActionCommand();

		System.out.println(name);

		// Only process events here if we are a WorldWind map
		if (whichMap != WORLDWIND) {
			super.actionPerformed(evt);
		}
		else if (name.equals("gmrt_cmd")) {
			loadGeoMapAppMap();
		}
		else if (name.equals("landsat_cmd")) {
			loadICubeLandSatMap();
		}
		else if (name.equals("msve_cmd")) {
			loadMSVirtualEarthAerialLayer();
		}
		else if (name.equals("msve_r_cmd")) {
			loadMSVirtualEarthRoadsLayer();
		}
		else if (name.equals("msve_h_cmd")) {
			loadMSVirtualEarthHybridLayer();
		}
		else if (name.equals("open_street_map_cmd")) {
			loadOpenStreetMapLayer();
		}
		else if (name.equals("ClearCacheCmd")) {
			clearDataCache();
		}
		else if ( name.equals("GMRTGridCmd" ) ) {
			loadTopoGrid();
		} 
		else if (name.equals("TopoSSv9MCmd")) {
			loadTopo9Grid();
		}
		else if (name.equals("GravitySSv18MCmd")) {
			loadGravity_18Grid();
		}
		else if (name.equals("GeoidSS97Cmd")) {
			loadGeoidGrid();
		}
		else if (name.equals("SeaFloorAsyGridCmd")) {
			loadSpreadingAsymmetryGrid();
		}
		else if (name.equals("SeaFloorAgeGridCmd")) {
			loadAgeGrid();
		}
		else if (name.equals("SeaFloorSpreadRateGridCmd")) {
			loadSpreadingRateGrid();
		}
		else if (name.equals("color_scale_cmd")) {
			boolean tf = ((JCheckBoxMenuItem)evt.getSource()).isSelected();
			toggleColorScaleLayer(tf);
		}
		else if (name.equals("distance_scale_cmd")) {
			boolean tf = ((JCheckBoxMenuItem)evt.getSource()).isSelected();
			toggleScaleBarLayer(tf);
		}
		else if (name.equals("place_names_cmd")) {
			boolean tf = ((JCheckBoxMenuItem)evt.getSource()).isSelected();
			togglePlaceNamesLayer(tf);
		}
		else if (name.equals("compass_cmd")) {
			boolean tf = ((JCheckBoxMenuItem)evt.getSource()).isSelected();
			toggleSunCompassLayer(tf);
		}
		else if ( name.equals("open_search_tree") ) {
			JMenuItem source = (JMenuItem) evt.getSource();
			XML_Menu sourceMenu = XML_Menu.getXML_Menu(source);
			SearchTree st = new WWSearchTree(sourceMenu);
			st.setMapApp(this);
		}
		else if ( name.equals("import_fence_cmd")) {
			importFenceImage();
		}
		else if ( name.equals("fence_cmd")) {
			XML_Menu tile_menu = XML_Menu.getXML_Menu((JMenuItem) evt.getSource());

			loadFenceDiagram(tile_menu);
		}
		else {
			super.actionPerformed(evt);
		}
	}

	protected void loadFenceDiagram(XML_Menu menu) {
		BufferedImage fdImage = null;
		List<LatLon> fdNav = null;
		try {
			URL imageURL = URLFactory.url(menu.layer_url);
			URL navURL = URLFactory.url(menu.layer_url2);
			fdImage = ImageIO.read(new BufferedInputStream(
						imageURL.openStream()));
			 fdNav = ImportFenceDiagram.loadNav(navURL.openStream());
		} catch (MalformedURLException ex) {
			ex.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (fdImage == null || fdNav == null) return;

		float baseElev = 0;
		if (menu.base_elev != null) {
			try {
				baseElev = Float.parseFloat(menu.base_elev);
			} catch (NumberFormatException ex) {}
		}

		FenceDiagram fd = new FenceDiagram(wwCanvas, 
				fdNav, 
				baseElev * 1000,
				1, 
				fdImage);

		if (menu.height != null) {
			try {
				float height = Float.parseFloat(menu.height);
				fd.setBaseHeight(height);
			} catch (NumberFormatException ex) {}
		}

		SimpleRenderableLayer rl = new SimpleRenderableLayer();
		rl.setName(menu.name);
		rl.addRenderable(fd);
		rl.setInfoURL(menu.infoURLString);

		makeLayerVisible(rl);
	}

	protected void importFenceImage() {
		ImportFenceDiagram.importFenceDiagram(this.getFrame(), wwCanvas, this);
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals(AVKey.LAYER) ||
				evt.getPropertyName().equals(AVKey.LAYERS)) {
			updateSunAngleProvider();
			updateColorScaleProvider();
			updateCreditButtonState();

			compassMI.setSelected( compassLayer.isEnabled() && 
					wwModel.getLayers().contains(compassLayer));

			colorScaleMI.setSelected( colorScaleLayer.isEnabled() && 
					wwModel.getLayers().contains(colorScaleLayer));

			scalebarMI.setSelected( scalebarLayer.isEnabled() && 
					wwModel.getLayers().contains(scalebarLayer));

			placeNamesMI.setSelected( placeNamesLayer.isEnabled() && 
					wwModel.getLayers().contains(placeNamesLayer));

			/// Check to make sure the topmost visible DB is enabled
			LayerList layers = wwModel.getLayers();
			ListIterator<Layer> li = layers.listIterator(layers.size());
			while (li.hasPrevious()) {
				Layer layer = li.previous();
				if (layer.isEnabled() &&
						layer instanceof WWLayer) {
					final Database topDB = ((WWLayer) layer).getDB();
					if (currentDB == topDB) return;
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							if (currentDB != null) {
								dialog.remove( currentDB.getSelectionDialog() );
								getDataDisplayDialog().remove( currentDB.getDataDisplay() );
							}
							setCurrentDB(topDB);
							addDBToDisplay(topDB);
						}
					});
					return;
				}
			}
			setCurrentDB(null);
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (currentDB != null) {
						dialog.remove( currentDB.getSelectionDialog() );
						getDataDisplayDialog().remove( currentDB.getDataDisplay() );
					}
					hPane.setRightComponent( null );
					vPane.setBottomComponent( null );
					setCurrentDB(null);
				}
			});
		}
	}

	protected void clearDataCache() {
		JPanel p2 = new JPanel(new GridLayout(0,1));

		JCheckBox earth = new JCheckBox("World Wind Earth Data");
		JCheckBox geomapapp = new JCheckBox("GeoMapApp Data");

		p2.add(new JLabel("Chose which data caches to clear: "));
		p2.add(earth);
		p2.add(geomapapp);

		JPanel p = new JPanel(new BorderLayout());
		p.add(p2);

		int c = 
			JOptionPane.showConfirmDialog(this.frame, p, "Clear Data Cache", JOptionPane.OK_CANCEL_OPTION);

		if (c == JOptionPane.CANCEL_OPTION)
			return;

		if (earth.isSelected()) {
			new Thread() {
				public void run() {
					FileStore cache = WorldWind.getDataFileStore();
					URL url = cache.findFile(WORLD_WIND_CACHE_NAME, false);

					while (url != null) {
						try {
							File root = new File(url.toURI());

							if (!root.exists())
								break;

							List<File> files = new LinkedList<File>();
							createFileList(root, files);

							for (File file : files)
								file.delete();
						} catch (URISyntaxException e) {
							break;
						}
					}
				}
			}.start();
		}

		if (geomapapp.isSelected()) {
			new Thread() {
				public void run() {
					FileStore cache = WorldWind.getDataFileStore();
					URL url = cache.findFile("GeoMapApp", false);

					while (url != null) {
						try {
							File root = new File(url.toURI());

							if (!root.exists())
								break;

							List<File> files = new LinkedList<File>();
							createFileList(root, files);

							for (File file : files)
								file.delete();
						} catch (URISyntaxException e) {
							break;
						}
					}
				}
			}
		.start();
		}
	}

	private void createFileList(File file, List<File> list) {
		if (file.isDirectory()) {
			for (File child : file.listFiles()) 
				createFileList(child, list);
		}
		list.add(file);
	}

	@Override
	public void setMask(boolean tf) {
		if (whichMap != WORLDWIND) {
			super.setMask(tf);
			return;
		}

		Layer newLayer = LayerComposer.getGeoMapAppMaskLayer();

		if (tf) {
			layerManager.addIgnoredLayer(newLayer);
			makeLayerVisible(newLayer);
		} else {
			disposeLayer(GeoMapAppMaskLayer.class);
		}
	}

	protected void load512TileSet(XML_Menu tile_menu) throws IOException {
		if (whichMap != WORLDWIND) {
			super.load512TileSet(tile_menu);
			return;
		}
		makeLayerVisible(LayerComposer.get512Layer(tile_menu));
	}

	protected void addExternalImage(XML_Menu image_menu) {
		if (whichMap != WORLDWIND) {
			super.addExternalImage(image_menu);
			return;
		}

		double wesn[] = new double[4];
		String[] s = image_menu.wesn.split(",");
		for (int i = 0; i < 4; i++)
			wesn[i] = Double.parseDouble(s[i]);

		try {
			BufferedImage image = ImageIO.read(
					URLFactory.url(image_menu.layer_url));
			if (image_menu.mapproj.equals("m")) { // Reproject
				image = ImageResampler
					.MERCATOR_TO_GEOGRAPHIC
					.resampleImage(image, wesn[2], wesn[3], wesn[2], wesn[3], image.getHeight());
			}
			else if (image_menu.mapproj.equals("g"))
				; //
			else {
				System.err.println("Unsupported projection " + image_menu.mapproj);
				return;
			}

			SurfaceImageLayer layer = new SurfaceImageLayer();
			layer.setName( image_menu.name );
			layer.setLegendURL( image_menu.legend );

			Sector sector = Sector.fromDegrees(wesn[2],wesn[3], wesn[0],wesn[1]);
			SurfaceImage si = new SurfaceImage(image, sector);
			layer.setSurfaceImage(si);

			makeLayerVisible(layer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void importImage() {
		if (whichMap != WORLDWIND) {
			super.importImage();
			return;
		}
		new WWImportImageLayer().importImage(this);
	}

	protected void loadMSVirtualEarthAerialLayer() {
		makeLayerVisible(LayerComposer.getVirtualEarthAerialLayer());
	}

	protected void loadMSVirtualEarthRoadsLayer() {
		makeLayerVisible(LayerComposer.getVirtualEarthRoadsLayer());
	}

	protected void loadMSVirtualEarthHybridLayer() {
		makeLayerVisible(LayerComposer.getVirtualEarthHybridLayer());
	}

	protected void loadOpenStreetMapLayer() {
		makeLayerVisible(LayerComposer.getOpenStreetMapLayer());
	}
	protected void loadBlueMarbleMap() {
		makeLayerVisible(LayerComposer.getBlueMarbleMapLayer());
	}

	protected void loadICubeLandSatMap() {
		makeLayerVisible(LayerComposer.getICubeLandSatMapLayer());
	}

	protected void loadGeoMapAppMap() {
		makeLayerVisible(LayerComposer.getTopoMapLayer());
	}

	protected void disposeGridLayer() {
		disposeLayer(GridTileLayer.class);
	}

	protected void loadGeoidGrid() {
		disposeGridLayer();
		makeLayerVisible(LayerComposer.getGeoidGridLayer());
	}

	protected void loadTopo9Grid() {
		disposeGridLayer();
		makeLayerVisible(LayerComposer.getTopo9GridLayer());
	}

	protected void loadAgeGrid() {
		disposeGridLayer();
		makeLayerVisible(LayerComposer.getAgeGridLayer());
	}

	protected void loadSpreadingRateGrid() {
		disposeGridLayer();
		makeLayerVisible(LayerComposer.getSpreadingRateGridLayer());
	}

	protected void loadSpreadingAsymmetryGrid() {
		disposeGridLayer();
		makeLayerVisible(LayerComposer.getSpreadingAsymmetryGridLayer());
	}

	protected void loadGravityGrid() {
		disposeGridLayer();
		makeLayerVisible(LayerComposer.getGravityGridLayer());
	}

	protected void loadGravity_18Grid() {
		disposeGridLayer();
		makeLayerVisible(LayerComposer.getGravity_18GridLayer());
	}

	protected void loadTopoGrid() {
		disposeGridLayer();
		makeLayerVisible(LayerComposer.getTopoGridLayer());
	}

	private void toggleScaleBarLayer(boolean tf) {
		if (tf)
			makeLayerVisible(scalebarLayer);
		else
			wwModel.getLayers().remove(scalebarLayer);
	}

	private void togglePlaceNamesLayer(boolean tf) {
		if (tf)
			makeLayerVisible(placeNamesLayer);
		else
			wwModel.getLayers().remove(placeNamesLayer);
	}

	private void toggleSunCompassLayer(boolean tf) {
		if (tf)
			makeLayerVisible(compassLayer);
		else 
			wwModel.getLayers().remove(compassLayer);
	}

	private void toggleColorScaleLayer(boolean tf) {
		if (tf)
			makeLayerVisible(colorScaleLayer);
		else
			wwModel.getLayers().remove(colorScaleLayer);
	}
	
	private void disposeLayer(Class<?> c) {
		LayerList ll = wwModel.getLayers();

		for (Layer layer : ll) {
			if (c.isInstance(layer)) {
				ll.remove(layer);
				layer.dispose();
				return;
			}
		}
	}

	private void disposeSedimentThicknessLayer() {
		disposeLayer(SedimentThicknessSurfaceLayer.class);
	}

	private void disposeMagneticAnomaliesLayer() {
		disposeLayer(MagneticAnomaliesLayer.class);
	}

	private void disposeOceanAgesLayer() {
		disposeLayer(OceanAgesSurfaceLayer.class);
	}

	private void disposeSpreadingRateLayer() {
		disposeLayer(SpreadingRateSurfaceLayer.class);
	}

	private void disposeSpreadingAsymLayer() {
		disposeLayer(SpreadingAsymmetrySurfaceLayer.class);
	}

	private boolean isLayerVisible(Class<?> c) {
		Layer layer = null;

		for (Layer l : wwModel.getLayers())
			if (c.isInstance(l)) {
				layer = l;
				break;
			}

		if (layer == null)
			return false;

		if (!layer.isEnabled())
			return false;

		if (layer.getOpacity() < .3)
			return false;

		return true;
	}

	public synchronized void hideLayer(Layer layer) {
		wwModel.getLayers().remove(layer);
	}

	public synchronized void makeLayerVisible(Layer newLayer, boolean ignoreLayer) {
		if (newLayer == null) return;

		if (ignoreLayer)
			layerManager.addIgnoredLayer(newLayer);

		LayerList ll = wwModel.getLayers();

		newLayer.setEnabled(true);
		if (newLayer.getOpacity() < .3)
			newLayer.setOpacity(1);

		ll.remove(newLayer);
		ll.add(ll.size(), newLayer);

		if (!ignoreLayer)
			setLayerManagerVisible(true);
	}

	public synchronized void makeLayerVisible(Layer newLayer) {
		if (newLayer == null) return;

		LayerList ll = wwModel.getLayers();

		newLayer.setEnabled(true);
		if (newLayer.getOpacity() < .3)
			newLayer.setOpacity(1);

		ll.remove(newLayer);
		ll.add(ll.size(), newLayer);
		setLayerManagerVisible(true);
	}

	protected void initWWGUI() {
		// Make all our PopupMenus heavyweight!
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);

// create "focus" overlay with NULL image
		focus = new Grid2DOverlay( map );
		map.addOverlay(focus);

		// initialize zoomer
		zoomer = new Zoomer(map);
		frame = createBaseFrame(VO_NAME);

//		progress = new org.geomapapp.util.ProgressDialog(frame);
		tools = new WWMapTools(this, map, wwCanvas);

		JPanel mainPanel = new JPanel(new BorderLayout() );
		mainPanel.add(tools.getTools(), "North");
		frame.getContentPane().add(mainPanel, "Center");
//		frame.getContentPane().add(tools.getTools(), "North");

		frame.addWindowListener( new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				System.exit(0);
			}
		});

		initWWDB();

		locs = new WWMapPlaces( (WWMap) map, tools);

		hPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT );
		hPane.setLeftComponent( wwCanvas );
		hPane.setOneTouchExpandable(true);
		hPane.setContinuousLayout(true);

		JPanel panel = new JPanel( new GridLayout(0,1) );
		dbLabel = new JLabel("");
		dbLabel.setForeground(Color.black);
		panel.add(dbLabel);
		closeDB = new JButton("Close");
		closeDB.addActionListener(this);
		panel.add(closeDB);
		detach_attachB = new JButton("Detach");
		detach_attachB.addActionListener(this);
		panel.add(detach_attachB);

		dialog = new JPanel(new BorderLayout());
		dialog.add( panel, "North");
		dialog.setPreferredSize(new Dimension(120, 450));
		dialog.setMinimumSize(new Dimension(120,8));
		
		// For vPane Panel to scroll
		dialogScroll = new JScrollPane(dialog);
		dialogScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		dialogScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		vPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
		vPane.setTopComponent( hPane );
		vPane.setOneTouchExpandable(true);
		mainPanel.add(vPane, "Center");

		String mainMenuURL = MapApp.NEW_BASE_URL + "gma_menus/main_menu.xml";
		try {
			XML_Menu.setMapApp(this);
			menuBar = XML_Menu.createMainMenuBar(XML_Menu.parse(mainMenuURL));
			frame.setJMenuBar(menuBar);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		}

//		JMenuItem mi = XML_Menu.commandToMenuItemHash.get("cmt_cmd");
//		if (mi != null)
//			mi.setEnabled(false);

		colorScaleCB = (JCheckBoxMenuItem) 
			XML_Menu.commandToMenuItemHash.get("color_scale_cmd");

		scalebarMI = ((JCheckBoxMenuItem)XML_Menu.commandToMenuItemHash.get("distance_scale_cmd")); 
		colorScaleMI = ((JCheckBoxMenuItem)XML_Menu.commandToMenuItemHash.get("color_scale_cmd"));
		placeNamesMI = ((JCheckBoxMenuItem)XML_Menu.commandToMenuItemHash.get("place_names_cmd"));
		compassMI = ((JCheckBoxMenuItem)XML_Menu.commandToMenuItemHash.get("compass_cmd"));
		layerManagerMI = ((JCheckBoxMenuItem)XML_Menu.commandToMenuItemHash.get("layer_manager_cmd"));

		colorScaleMI.setSelected(true);
		scalebarMI.setSelected(true);
		compassMI.setSelected(true);

//		frame.pack();
//		frame.setSize( 1000, 800 );
//		frame.setVisible(true);

		// Shows select home directory
		getGMARoot();
	}

	protected void initWWDB() {
		custom = new haxby.worldwind.db.custom.WWCustomDB(map);
		((WWCustomDB) custom).setLassoSelectionHandler(lassoSelectionHandler);
		lassoSelectionHandler.addSelectionListener( 
				(WWCustomDB) custom );

		portal_commands = haxby.util.PortalCommands.getPortalCommands();
		int ndb = 7;		//Number of databases: 6 in WorldWind Mode
		db = new Database[ndb];

		WWPDB pdb = new haxby.worldwind.db.pdb.WWPDB(map);
		pdb.setLassoSelectionHandler(lassoSelectionHandler);
		lassoSelectionHandler.addSelectionListener(pdb);

		WWFocalMechanismSolutionDB fmsDB = new haxby.worldwind.db.fms.WWFocalMechanismSolutionDB(map);
		fmsDB.setLassoSelectionHandler(lassoSelectionHandler);
		lassoSelectionHandler.addSelectionListener(fmsDB);
//		layerManager.addIgnoredLayer( fmsDB.getLayer() );

		db[0] = new WWMGG(wwCanvas, map, 2900);
		db[1] = new WWMBTracks(wwCanvas, map, 4000);
		db[2] = new WWSCS(wwCanvas, map);
		db[3] = new WWXMCS(wwCanvas, map);
		db[4] = pdb;
		db[5] = new WWEQ(map);
		db[6] = fmsDB;
	}

	protected void initLayerManager() {
		if (whichMap != MapApp.WORLDWIND) {
			super.initLayerManager();
			return;
		}

		JFrame d = new JFrame();
		d.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				((JCheckBoxMenuItem)(XML_Menu.commandToMenuItemHash.get("layer_manager_cmd"))).setSelected(false);
			}
		});
		LayerManager lm = new LayerManager(new LayerManager.XMapZoomer(map));
		lm.setLayerList( toLayerList(wwModel.getLayers()) );

		JScrollPane sp = new JScrollPane(lm);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		d.setTitle("Layers");
		d.setContentPane(sp);
		d.setSize(new Dimension(lm.getPreferredSize().width+20,lm.getPreferredSize().height+55));
		d.setMaximumSize(new Dimension(400,300));

		d.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		d.setLocationRelativeTo(this.getFrame());

		d.setState(Frame.NORMAL);
		d.setAlwaysOnTop(true);

		lm.listeners.add(new LayerManager.LayerListListener() {
			public void up(ILayer l) {
				LayerList ll = wwModel.getLayers();
				List<ILayer> list = toLayerList(ll);
				int i = list.indexOf(l);
				if (i >= list.size() - 1) return;

				list.set(i, list.set(i + 1, list.get(i)));

				LayerList newLayerList = new LayerList();
				for (ILayer layer : list) {
					newLayerList.add((Layer) layer.getLayer());
				}

				wwModel.setLayers(newLayerList);
			}
			public void remove(ILayer l) {
				LayerList ll = wwModel.getLayers();
				List<ILayer> list = toLayerList(ll);
				Layer layer = ll.remove(list.indexOf(l));
				if (layer instanceof WWLayer)
					((WWLayer)layer).close();
			}
			public void down(ILayer l) {
				LayerList ll = wwModel.getLayers();
				List<ILayer> list = toLayerList(ll);
				int i = list.indexOf(l);
				if (i <= 0) return;

				list.set(i, list.set(i - 1, list.get(i)));

				LayerList newLayerList = new LayerList();
				for (ILayer layer : list) {
					newLayerList.add((Layer) layer.getLayer());
				}
				wwModel.setLayers(newLayerList);
			}
		});

		wwModel.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals(AVKey.LAYERS)) {
					layerManager.setLayerList( toLayerList(wwModel.getLayers()) );

					Dimension size = new Dimension(
							layerManager.getMinimumSize().width+20,
							layerManager.getMinimumSize().height+30);
					Dimension maxSize = layerManagerDialog.getMaximumSize();

					size.height = Math.min(size.height, maxSize.height);
					size.width = Math.min(size.width, maxSize.width);

					layerManagerDialog.setMinimumSize(size);
					layerManagerDialog.setSize(size);
				} else if (evt.getPropertyName().equals(AVKey.LAYER)) {
					layerManager.layerStateChanged();
				}
			}
		});

		d.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				layerManagerMI.setState(false);
			}
		});

		this.layerManager = lm;
		this.layerManagerDialog = d;
	}

	@Override
	public void addWMSLayer(String name, String url, String infoURLString,
			double[] wesn, String srs, int mapRes, XML_Menu xml_item) {
		if (whichMap == WORLDWIND){
			if (url == null) return;

			double dPerNode = 360.0 / 640 / mapRes;
			double dPerNode2 = 36.0 / 512;
			int nLevel = 0;
			while (dPerNode2 > dPerNode) {
				nLevel++;
				dPerNode2 /= 2;
			}

			makeLayerVisible( 
				WMSLayer.buildWMSLayer(name, url, wesn, srs, nLevel));
		}
		else
			super.addWMSLayer(name, url, infoURLString, wesn, srs, mapRes, xml_item);
	}

	@Override
	public void addWMSLayer(String name, String url, String infoURLString,
			double[] wesn, String srs, int mapRes) {
		addWMSLayer(name, url, infoURLString, wesn, srs, mapRes, null);
	}

	@Override
	public void addWMSLayer(String url, String wesnString, String srs) {
		String[] results = wesnString.split(",");
		double[] wesn = new double[4];
		for ( int i = 0; i < results.length; i++ ) {
			wesn[i] = Double.parseDouble(results[i]);
		}
		addWMSLayer(url, wesn, srs);
	}

	@Override
	public void addWMSLayer(haxby.wms.Layer layer, String url) {
		if (whichMap == WORLDWIND) {
			if (url == null) return;
			makeLayerVisible( WMSLayer.buildWMSLayer(layer, url));
		}
		else
			super.addWMSLayer(layer, url);
	}

	@Override
	public void addWMSLayer(String name, String url, double[] wesn, String srs) {
		addWMSLayer(name, url, null, wesn, srs, Integer.MAX_VALUE);
	}

	@Override
	public void addWMSLayer(String url, double[] wesn, String srs) {
		this.addWMSLayer(System.currentTimeMillis()+"", url, wesn, srs);
	}

	private List<ILayer> toLayerList(LayerList ll) {
		List<ILayer> layers = new ArrayList<ILayer>();

		for (Layer layer : ll) {
			WWILayer ilayer = new LayerManager.WWILayer(layer);

			if (layer instanceof InfoSupplier) {
				String infoURL = ((InfoSupplier) layer).getInfoURL();
				ilayer.setInfoURL(infoURL);
			}
			if (layer instanceof LegendSupplier) {
				String legendURL = ((LegendSupplier) layer).getLegendURL();
				ilayer.setLegendURL(legendURL);
			}
			if (layer instanceof WESNSupplier) {
				ilayer.setWESN(((WESNSupplier)layer).getWESN());
			}
			layers.add(ilayer);
		}
		return layers;
	}

	public void showCredit() {
		LayerList ll = wwModel.getLayers();
		for (int i = ll.size() - 1; i >= 0; i--) {
			Layer l = ll.get(i);
			if (l.isEnabled() && (l.getOpacity() > .2)) {
				for (Class<?> c : l.getClass().getInterfaces())
					if (c.equals( InfoSupplier.class )) {
						String url = ((InfoSupplier) l).getInfoURL();
						BrowseURL.browseURL(url);
					}
				return;
			}
		}
	}

	protected void closeCurrentDB() {
		closeDB(currentDB);
	}

	@Override
	public void mapFocus() {
		if (whichMap != WORLDWIND) {
			super.mapFocus();
		}
	}

	public WorldWindow getCanvas() {
		return wwCanvas;
	}

	public static void main(String[] args) {
		// 1.6.8
		// Set the name of the MenuBar title for macs
//		if (Configuration.isMacOS()) {
//			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Virtual Ocean");
//			new haxby.os.mac.MacOSAboutHandler("VO", WWMapApp.VERSION); 
//		}

		com.Ostermiller.util.Browser.init();

		if (args.length==1) {
			try {
				int mapType = Integer.parseInt(args[0]);
				new WWMapApp(mapType);
				return;
			} catch (NumberFormatException ex) {
			}
		}
		new WWMapApp();
	}

	static
	{
		supported_commands.add("add_image_cmd");
		supported_commands.add("ClearCacheCmd"); 
		supported_commands.add("color_scale_cmd");
		supported_commands.add("compass_cmd");
		supported_commands.add("distance_scale_cmd");
		supported_commands.add("fence_cmd");
		supported_commands.add("GeoidSS97Cmd");
		supported_commands.add("gmrt_cmd");
		supported_commands.add("GMRTGridCmd");
		supported_commands.add("GravitySSv16MCmd");				//10
		supported_commands.add("import_image_cmd");
		supported_commands.add("import_fence_cmd");
		supported_commands.add("landsat_cmd");
		supported_commands.add("msve_cmd");
		supported_commands.add("msve_r_cmd");
		supported_commands.add("msve_h_cmd");
		supported_commands.add("open_street_map_cmd");
		supported_commands.add("place_names_cmd");
		supported_commands.add("SeaFloorAsyGridCmd");
		supported_commands.add("SeaFloorAgeGridCmd");
		supported_commands.add("SeaFloorSpreadRateGridCmd");
		supported_commands.add("TopoSSv9MCmd");
	}
}

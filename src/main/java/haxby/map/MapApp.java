package haxby.map;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.Authenticator;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.xml.parsers.ParserConfigurationException;

import org.geomapapp.credit.Credit;
import org.geomapapp.db.dsdp.DSDPDemo;
import org.geomapapp.geom.MapProjection;
import org.geomapapp.gis.shape.ESRIShapefile;
import org.geomapapp.grid.Grid2DOverlay;
import org.geomapapp.grid.GridComposer;
import org.geomapapp.grid.GridDialog;
import org.geomapapp.image.ColorScaleTool;
import org.geomapapp.image.Palette;
import org.geomapapp.io.GMADownload;
import org.geomapapp.map.MapPlaces;
import org.geomapapp.util.NetUtil;
import org.geomapapp.util.ProgressDialog;
import org.geomapapp.util.SymbolScaleTool;
import org.geomapapp.util.XML_Menu;
import org.json.JSONException;
import org.xml.sax.SAXException;

import haxby.db.Database;
import haxby.db.age.Isochrons;
import haxby.db.custom.CustomDB;
import haxby.db.custom.DBInputDialog;
import haxby.db.custom.UnknownDataSet;
import haxby.db.dig.Digitizer;
import haxby.db.eq.EQ;
import haxby.db.fms.FocalMechanismSolutionDB;
import haxby.db.mb.MBTracks;
import haxby.db.mb.PreviewCruise;
import haxby.db.mgg.MGG;
import haxby.db.pdb.PDB;
import haxby.db.pmel.PMEL;
import haxby.db.radar.Radar;
import haxby.db.scs.SCS;
import haxby.db.ship.Ship;
import haxby.db.shore.ShoreLine;
import haxby.db.shore.ShoreOptionPanel;
import haxby.db.surveyplanner.SurveyPlanner;
import haxby.db.velocityvectors.VelocityVectors;
import haxby.db.xmcs.XMCS;
import haxby.grid.ContributedGridsOverlay;
import haxby.layers.image.GeographicImageOverlay;
import haxby.layers.image.ImageOverlay;
import haxby.layers.image.ImportImageLayer;
import haxby.layers.image.MercatorImageOverlay;
import haxby.layers.tile512.LayerSetDetails;
import haxby.layers.tile512.Tile512Overlay;
import haxby.proj.Mercator;
import haxby.proj.PolarStereo;
import haxby.proj.Projection;
import haxby.proj.ProjectionFactory;
import haxby.util.BrowseURL;
import haxby.util.GeneralUtils;
import haxby.util.LayerManager;
import haxby.util.LayerManager.LayerPanel;
import haxby.util.PathUtil;
import haxby.util.ProcessingDialog;
import haxby.util.ProcessingDialog.StartStopTask;
import haxby.util.ProcessingDialog.StartStopThread;
import haxby.util.SearchTree;
import haxby.util.SilentProcessingDialog;
import haxby.util.SilentProcessingTask;
import haxby.util.UIDTracker;
import haxby.util.URLFactory;
import haxby.util.WESNPanel;
import haxby.wfs.WFSViewServer;
import haxby.wms.Layer;
import haxby.wms.WMSViewServer;
import haxby.wms.WMS_ESPG_3031_Overlay;
import haxby.wms.WMS_ESPG_4326_Overlay;
import haxby.wms.XML_Layer;

import org.geomapapp.util.OSAdjustment;
import haxby.util.VersionUtil;

public class MapApp implements ActionListener,
							   KeyListener {

	public static final int MERCATOR_MAP = 0;
	public static final int SOUTH_POLAR_MAP = 1;
	public static final int NORTH_POLAR_MAP = 2;
	public static final int WORLDWIND = 3;
	public static final int DEFAULT_LONGITUDE_RANGE = Projection.RANGE_180W_to_180E;
	public static final List<Integer> SUPPORTED_MAPS = new LinkedList<Integer>();
	static {
		SUPPORTED_MAPS.add(new Integer(MERCATOR_MAP));
		SUPPORTED_MAPS.add(new Integer(SOUTH_POLAR_MAP));
		SUPPORTED_MAPS.add(new Integer(NORTH_POLAR_MAP));
	}

	public final static String VERSION = "3.7.6.13"; //March 20th, 2026
	public final static String GEOMAPAPP_NAME = "GeoMapApp " + VERSION;
	private static boolean DEV_MODE = false; 
	static boolean isNewVersion = false;
	
	public static final String PRODUCTION_URL = "https://app.geomapapp.org/";
	public static String DEFAULT_URL = "https://app.geomapapp.org/";
	public static final String DEV_URL = "https://app-dev.geomapapp.org/"; 
	private static String DEV_PASSWORD_PATH = "gma_passwords/dev_server_password";
	public static String BASE_URL;
	public static String NEW_BASE_URL;
	public static String TEMP_BASE_URL = "https://app.geomapapp.org/"; // stay for old references, mostly all changed or not in use.

//	Name for base map image overlay
	public static String baseMapName = "GMRT Basemap";

//	Name for base grid overlay (base topography grid is part of this overlay)
	public static String versionGMRT;
	public static String baseFocusName;

	public static boolean AT_SEA = false;
// (bargbb) is 1000 in live version, lowered to 250 for added responsiveness
	public final static int AUTO_FOCUS_WAIT = 250;
	public static String CURRENT_PROJECTION = "m";
	public boolean switchingProjection = false;

	public static boolean ReadMenusCache;
	protected static ProgressDialog progress;
	protected int whichMap;
	protected boolean fetchCacheMenus = false;
	protected int numGridLoaders = 0;
	protected XMap map = null;
	protected Zoomer zoomer;
	protected MapTools tools;
	// bill test
	protected static JFrame frame = null;
	protected JFrame option = null;

	protected Grid2DOverlay baseMapFocus;
	protected Grid2DOverlay focus;
	protected List<FocusOverlay> focusOverlays =
		Collections.synchronizedList( new ArrayList<FocusOverlay>() );
	protected MapOverlay baseMap;

	protected Mercator merc;
	protected JSplitPane hPane;
	protected JSplitPane vPane;
	protected JScrollPane dialogScroll;
	protected JPanel dialog;
	protected JPanel panel;
	protected JLabel dbLabel;
	protected JButton closeDB;
	protected JButton detach_attachB;
	protected JRadioButton range180Btn, range360Btn;

	protected CustomDB custom = null;
	protected Digitizer digitizer = null;
	protected Database[] db=null;
	protected ShoreLine shoreLine = null;
	protected ShoreOptionPanel opShorePanel = null;
	protected Database currentDB = null;
	// Dialogs
	protected ProcessingDialog processingDialog = null;
	protected SilentProcessingDialog silentProcessingDialog = null;

	// Menus
	public static JMenuBar menuOnOff =null;
	protected JMenuBar menuBar;
	protected JMenuBar menuBarMulti;

	protected JCheckBoxMenuItem colorScaleCB;
	protected MapColorScale colorScale = null;
	protected LocationInset li = null;
	protected MapScale mapScale = null;
	protected ContributedGridsOverlay contributedGridsOverlay;
	protected WFSViewServer wfsWindow;
	protected WMSViewServer wmsWindow;
	protected String directory;
	protected boolean[] tmpSides = new boolean[4];
	protected JComboBox<String> font;
	protected JComboBox<String> menuFont;

	protected Vector servers;
	protected JComboBox serverList;
	protected JPanel serverPanel;
	protected String serverURLString;
	protected File parentRoot = getGMARoot();
	protected File serverDir = new File( parentRoot, "servers");
	protected File serverFile = new File( serverDir, "default_server.dat" );
	protected File historyDir = new File( parentRoot, "history");
	//public File historyFile = new File( historyDir, "zoom.txt"); //not used
	protected File historyVersionFile = new File( historyDir, "version");
	protected File menusCacheDir = new File( parentRoot, "menus_cache");
	protected File menuFontFile = new File(menusCacheDir, "menu_font.txt");
	protected File menusCacheFile = new File( menusCacheDir, "menu_updated.txt");
	protected File menusCacheDir2 = new File(menusCacheDir, "menus");
	protected File menusCacheFileFirst = new File( menusCacheDir2, "main_menu.xml");
	protected File menusCacheFileLast = new File( menusCacheDir2, "help_menu.xml");
	protected File preferencesDir = new File( parentRoot, "preferences");
	protected File logGridImportsFile = new File(preferencesDir, "log_grid_imports.txt");
	protected File portalCacheDir = new File(menusCacheDir, "portals");
	protected File portalSelectFile = new File(menusCacheDir, "default_portals.txt");
	protected File portalSelectFileOld = new File(menusCacheDir, "default_portals.dat");
	protected File showMailingListFile = new File(parentRoot, "hideMailingListDialog");
	protected JPanel inputDevPasswordPanel;
	protected JTextField inputDevPasswordText;
	protected JLabel inputDevPasswordLabel;
	protected File proxyDir = new File( parentRoot, "proxy_servers");
	protected File proxyFile = new File( proxyDir, "proxy_list.dat" );
	protected List<String> proxies;
	protected JComboBox proxyList = null;
	protected String currentDefaultProxy = null;
	protected int selectedServer = 0;
	protected int tmpSelectedServer = 0;
	protected boolean proxyOptionsPresent = false;
	protected boolean proxySelected = false;
	protected File layerSessionDir = new File( parentRoot , "layers");
	protected boolean loadSession = true;
	protected static String sessionImport = "";
	
	public boolean logGridImports = logGridImportsFile.exists();
	public File gridImportsLogDir = getGridImportsLogDir();
	private static File DEFAULT_GRID_IMPORTS_LOGS_DIR = new File(System.getProperty("user.home") + "/Desktop/");
	
	protected JTextField fontSize;
	protected JTextField menuFontSize;
	protected JCheckBox[] side;
	protected JCheckBox showTileNames;
	protected JCheckBox gridsCB;
	protected JTextField gridsDirTF;
	protected JButton gridsDirBtn;
	protected JFileChooser gridsChooser;
	protected static JCheckBox mbPortalCache = new JCheckBox("Multibeam Swath Bathymetry");
	static {
		mbPortalCache.setSelected(true);
	}
	protected JCheckBox pPortalCache;
	protected JButton clearMCache,
						clearPCache;
	protected Font tmpFont,
					defaultFont;
	protected Font curMenuFont, defaultMenuFont;
	private String menuFontDelimiter = ", ";
	protected boolean[] dfltSides = new boolean[4];
	protected boolean scroll = true;
	protected boolean attached = true;
	protected JCheckBox doScroll;
	protected String wmsCRS = "CRS=CRS:84";
	protected String wmsCRS3031 = "CRS=EPSG:3031";
	protected String wmsSRS4326 = "SRS=EPSG:4326";
	protected String wmsSRS3031 = "SRS=EPSG:3031";
	// more comments
	protected volatile DSDPDemo dsdp;
	protected static MapPlaces locs;
	public Credit credit;
	long focusTime = -1;
	int processingTasks;
	protected JFrame dataDisplayDialog;

	// Task Locks
	protected Object autoFocusLock = new Object(); // Lock Object for the autoFocus method
	protected Object processingTaskLock = new Object();
	protected Object silentProcessingTaskLock = new Object();
	
	public static OSAdjustment.OS which_os = OSAdjustment.getOS();

	public static JFileChooser chooser = null;
	public static ArrayList<String> portal_commands;
	
	public static Component anchor = null;
	
	private JCheckBox loadAltTiles;
	//private JTextField altTilesUrlTextField;

	public StartUp start,
					startNP,
					startSP;

	public LayerManager layerManager;
	public JFrame layerManagerDialog;
	
	private static MapApp theApp;
	
	public static boolean shouldShowMailingListPopup = true;
	
	// Menu Listener action bring to front
	public MenuListener listener = new MenuListener() {
		public void menuCanceled(MenuEvent e) {
		}

		public void menuDeselected(MenuEvent e) {
			JMenu wm = (JMenu) e.getSource();

			// Cleans Windows if not visible
			if(wm.getText().matches("Windows") || wm.getText().matches("Window")) {
			wm.removeAll();
			System.gc();
			}

			/* When user first imports menu item the background is set to another color to catch user attention.
			 * After user interaction will set the background color back.
			 */
			if(wm.getText().matches("My Layer Sessions")) {
				JMenu selectSessionMenu = (JMenu) wm;
				try {
					JMenuItem selectSessionMenuChild = (JMenuItem) selectSessionMenu.getMenuComponent(0);
					Color currentColor = selectSessionMenu.getBackground();
					Color defaultColor = selectSessionMenuChild.getBackground();
					if(currentColor.getRed() + currentColor.getGreen() + currentColor.getBlue() == 586) {
						selectSessionMenu.setBackground(defaultColor);
						selectSessionMenu.revalidate();	
				}
				} catch (Exception ex) {
					//don't do anything if menu component and cannot be cast as a JMenuItem 
					//(eg if it is a separator bar)
				}
			}
		}

		public void menuSelected(MenuEvent e) {
			final JMenu selectedMenu = (JMenu) e.getSource();
			JMenuItem winMi = null;
			final Frame[] frames = Frame.getFrames();
			Frame[] checkSearchableAlreadyOpen = null;

			// On BaseMaps Action
			if(selectedMenu.getText().matches("Basemaps")) {
				JMenu selectMain = (JMenu) selectedMenu;
				JMenuItem select2 = (JMenuItem) selectMain.getMenuComponent(0);
				checkSearchableAlreadyOpen = frames;
				Boolean click = true;
			}

			// Gets all open frames in GMA and sets the dynamic menu items in windows section
			if(selectedMenu.getText().matches("Windows") || selectedMenu.getText().matches("Window")) {
				selectedMenu.removeAll(); // Removes preloaded empty xml item once
				for (int i = 0; i < frames.length; i++) {
					final String title = frames[i].getTitle();
					boolean isVisible = frames[i].isVisible();
					boolean isFocused = frames[i].isFocused();

					if(title.length() != 0 && isVisible) {
						winMi = new JMenuItem(title);
						// For the menu item selected bring that frame to front
						winMi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								JMenuItem selectedMI = (JMenuItem) ae.getSource();
								for (int j = 0; j < frames.length; j++) {
									if (frames[j].getTitle().matches(Pattern.quote(selectedMI.getText()))) {
										frames[j].toFront();
									}
								}
							}
						});
						// Add these items in the Windows menu
						selectedMenu.add(winMi);
					}
				}
			}
		}
	};

	public MapApp( String dir ) {
		this( dir, null );
	}
	public MapApp( String dir, String baseURL ) {
		try {
			getServerList();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(MapApp.anchor, "Error reading remote server list", "Non-Critical Error", JOptionPane.ERROR_MESSAGE);
		}
		try {
			getProxies();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		checkConnection();

		BASE_URL = PathUtil.getPath("ROOT_PATH");
		NEW_BASE_URL = PathUtil.getPath("ROOT_PATH"); // need to clean if same as base
		serverURLString = PathUtil.getPath("SERVER_LIST",BASE_URL+"/gma_servers/server_list.dat");
		
		VersionUtil.init(BASE_URL + "versions.json");
		checkVersion();

		if( baseURL != null ) {
			BASE_URL = baseURL;
			if( !BASE_URL.endsWith("/") ) BASE_URL += "/";
		}
		DEV_MODE = BASE_URL.equals(DEV_URL);

		//BASE_URL = PathUtil.getPath("ROOT_PATH");
		NEW_BASE_URL = PathUtil.getPath("ROOT_PATH"); // need to clean if same as base
		//serverURLString = PathUtil.getPath("SERVER_LIST",BASE_URL+"/gma_servers/server_list.dat");

		
		versionGMRT = MMapServer.getVersionGMRT();		
		baseFocusName = "GMRT Image Version " + versionGMRT;
		whichMap = MapApp.MERCATOR_MAP;
		directory = dir;
		File file = new File(dir);
		if( dir.equals(".") ) file = new File( System.getProperty("user.dir") );

		BaseMapSelect sel = new BaseMapSelect();
		whichMap = sel.getBaseMap();
		if( whichMap==-1 )System.exit(0);

		/* Not needed
		switch (whichMap) {
		case MERCATOR_MAP:
			MMapServer.setBaseURL("file:" + file.getPath() +"/merc_320_1024/" );
			if( baseURL != null ) {
				GridComposer.setBaseURL( BASE_URL +"/MapApp/");
				MMapServer.setAlternateURL( BASE_URL +"/MapApp/merc_320_1024/" ); // data/mgds/gmrt/tiles_1.0/merc_320_1024cd 
			}
			MInit();
			break;
		case SOUTH_POLAR_MAP:
			PoleMapServer.setBaseURL("file:" + file.getPath()+"/SP_320_50/" , PoleMapServer.SOUTH_POLE);
			if( baseURL != null ) {
				SPGridServer.setBaseURL( BASE_URL +"/antarctic/SP_320_50");
				PoleMapServer.setAlternateURL( BASE_URL +"/antarctic/SP_320_50/", PoleMapServer.SOUTH_POLE); // data/mgds/gmrt/tiles_1.0/antarctic
			}
			SPInit();
			break;
		case NORTH_POLAR_MAP:
			PoleMapServer.setBaseURL("file:" + file.getPath()+"/NP_320_50/", PoleMapServer.NORTH_POLE);
			if( baseURL != null ) {
//				SPGridServer.setBaseURL( BASE_URL +"/arctic/NP_320_50");
				PoleMapServer.setAlternateURL( BASE_URL +"/arctic/NP_320_50/", PoleMapServer.NORTH_POLE);  // data/mgds/gmrt/tiles_1.0/arctic
			}
			NPInit();
			break;
		case WORLDWIND:
			if( baseURL != null ) {
				GridComposer.setBaseURL( BASE_URL +"/MapApp/");
			}
			WWInit();
			break;
		default:
			break;
		} */
		processBorder();
	}

	// Start
	public MapApp( ) {
		this(-1);
	}

	public MapApp( int which ) {
		isNewVersion = !getHistoryVersion().equals(VERSION);
		if(null == System.getProperty("geomapapp.paths_location") || System.getProperty("geomapapp.paths_location").startsWith("http")) {
			try {
				getServerList();
			} catch (IOException e) {
				//JOptionPane.showMessageDialog(MapApp.anchor, "Error reading remote server list", "Non-Critical Error", JOptionPane.ERROR_MESSAGE);
				BASE_URL = DEFAULT_URL;
			}
		}
		DEV_MODE = BASE_URL.replace("http://", "https://").equals(DEV_URL);
		
		try {
			getProxies();
			if(!AT_SEA) fetchCacheMenus = getMenusCache(); // add menu cache dir
			defaultMenuFont = XML_Menu.getMenuFont();
			curMenuFont = readMenuFont(); //get menu font
			if(null == curMenuFont) {
				curMenuFont = XML_Menu.getMenuFont();
			}
			setMenuFont(curMenuFont);
			startNewZoomHistory();			//start history dir
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		checkConnection();

		//BASE_URL = PathUtil.getPath("ROOT_PATH");
		NEW_BASE_URL = PathUtil.getPath("ROOT_PATH");		
		/*try {
			getServerList();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(MapApp.anchor, "Error reading remote server list", "Non-Critical Error", JOptionPane.ERROR_MESSAGE);
		}
		DEV_MODE = BASE_URL.equals(DEV_URL);*/

		VersionUtil.init(BASE_URL + "versions.json");
		checkVersion();
		shouldShowMailingListPopup = shouldShowMailingListPopup && !showMailingListFile.exists();

//		if(shouldShowMailingListPopup) {
//			showMailingListPopup();
//		}
//		else {
//			if(!showMailingListFile.exists()) {
//				try {
//					showMailingListFile.createNewFile();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		}

		// User chooses
		if (which == -1) {
			BaseMapSelect sel = new BaseMapSelect();
			whichMap = MapApp.MERCATOR_MAP;
			whichMap = sel.getBaseMap();
		} else {
			whichMap = which;
		}

		if( whichMap==-1 ) {
			System.exit(0);
		}
		
		versionGMRT = MMapServer.getVersionGMRT();		
		baseFocusName = "GMRT Image Version " + versionGMRT;

		if(whichMap==MapApp.MERCATOR_MAP) {
			MInit();
		} else if (whichMap == MapApp.SOUTH_POLAR_MAP) {
			SPInit();
		} else if (whichMap == MapApp.NORTH_POLAR_MAP) {
			NPInit();
		} else if (whichMap == MapApp.WORLDWIND) {
			WWInit();
		}
		processBorder();
	}
	
	private Font readMenuFont() {
		boolean readFont = false;
		Font f = null;
		if(menuFontFile.exists()) {
			try {
				Scanner s = new Scanner(menuFontFile);
				String fontStr = s.nextLine();
				while(fontStr.trim().length() == 0 && !fontStr.contains(menuFontDelimiter) && s.hasNextLine()) {
					fontStr = s.nextLine();
				}
				String[] fontInfo = fontStr.split(menuFontDelimiter);
				f = new Font(fontInfo[0], Font.PLAIN, Integer.parseInt(fontInfo[1]));
				readFont = true;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		if(!readFont) {
			try {
				f = XML_Menu.getMenuFont();
				menuFontFile.createNewFile();
				PrintStream ps = new PrintStream(menuFontFile);
				ps.println(f.getName() + menuFontDelimiter + f.getSize());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return f;
	}

	public static boolean isDevMode() {
		return DEV_MODE;
	}
	private void checkConnection() {
		if (AT_SEA) {
			return;
		}
		if (NetUtil.ping(BASE_URL)){
			return;
		}
		final ProxySelector defSel = ProxySelector.getDefault();

		ProxySelector.setDefault( new ProxySelector() {
			public List<Proxy> select(URI uri) {
				if (uri == null) {
					throw new IllegalArgumentException("URI can't be null.");
				}
				String protocol = uri.getScheme();

				if (protocol.equalsIgnoreCase("file")) {
					ArrayList<Proxy> l = new ArrayList<Proxy>();
					l.add(Proxy.NO_PROXY);
					return l;
				}
				synchronized (proxies) {
					if (proxies.size() > 0 &&
							("http".equalsIgnoreCase(protocol) ||
									"https".equalsIgnoreCase(protocol))) {
						ArrayList<Proxy> l = new ArrayList<Proxy>();

						for (String proxy : proxies) {
							String[] s = proxy.split("\\s");
							l.add(new Proxy(Proxy.Type.HTTP,
									new InetSocketAddress(s[0],Integer.parseInt(s[1]))));
						}
						return l;
					}
				}
				if (defSel != null) {
					return defSel.select(uri);
				} else {
					ArrayList<Proxy> l = new ArrayList<Proxy>();
					l.add(Proxy.NO_PROXY);
					return l;
				}
			}

			@Override
			public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
				if (!(sa instanceof InetSocketAddress)) return;
				// Move failed proxy to back of our list
				InetSocketAddress isa = (InetSocketAddress) sa;
				synchronized (proxies) {
					for (int i = 0; i < proxies.size(); i++) {
						String string = proxies.get(i);
						String[] s = string.split("\\s");
						InetSocketAddress inetSocketAddress = new InetSocketAddress(s[0], Integer.parseInt(s[1]));
						if (isa.equals(inetSocketAddress)) {
							proxies.add(proxies.remove(i));
							return;
						}
					}
				}
			}
		});

		if (proxies.size() > 0) {
			if (NetUtil.ping(BASE_URL))
				return;
		}

		// Show Connection Failed Dialog
		final JDialog d = new JDialog((JFrame) null, "Could Not Connect!", true);
		d.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		d.setSize(220, 180);
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		JLabel message1 = new JLabel("<html><center><b>Could Not Reach "
				+ DEFAULT_URL + "</b><br><br>GeoMapApp <b>requires</b> an internet connection.</center>"
				+ "<ol><li>Quit GeoMapApp</li>"
				+ "<li>Check your computer's network connection.</li>"
				+ "<li>Restart GeoMapApp</li></ol><br></html>");
		p.add(message1, BorderLayout.NORTH);

		/* TODO: this might confuse users we want them to have
		 * internet connections for GMA to work properly!
		 * Exclude b1 and b2 for now. Find out if Proxy config even works?
		 */
		JButton b1 = new JButton("Continue With No Internet");
		b1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				d.dispose();
			}
		});
		//p.add(b1);

		JButton b2 = new JButton("Configure Proxy Settings");
		b2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				configureProxy(d);

				if (NetUtil.ping(DEFAULT_URL)){
					d.dispose();
					writeProxies();
				}
			}
		});
		//p.add(b2, BorderLayout.CENTER);

		JButton b3 = new JButton("Quit");
		b3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(-1);
			}
		});
		p.add(b3, BorderLayout.EAST);

		d.getContentPane().add(p);
		d.pack();
		d.setLocationRelativeTo(MapApp.anchor);
		d.setVisible(true);
	}

	protected void writeProxies() {
		try {
			PrintStream out = new PrintStream(proxyFile);
			for (String s : proxies)
				out.println(s);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	protected void configureProxy(JDialog owner) {
		final JDialog d = new JDialog(owner, "Proxy Settings", true);
		JPanel p = new JPanel();
		p.add(new JLabel("HTTP Proxy: "));

		final JTextField host = new JTextField(10);
		p.add(host);

		p.add(new JLabel("Port: "));

		final JTextField port = new JTextField(5);
		p.add(port);

		JPanel p2 = new JPanel(new BorderLayout());
		p2.setBorder(BorderFactory.createTitledBorder("Manual proxy configuration: "));
		p2.add(p);

		JPanel p3 = new JPanel(new BorderLayout());
		p = new JPanel();
		final JLabel usernameL = new JLabel("Username: ");
		usernameL.setEnabled(false);
		p.add(usernameL);
		final JTextField username = new JTextField(10);
		username.setEnabled(false);
		p.add(username);
		final JLabel passwordL = new JLabel("Password: ");
		passwordL.setEnabled(false);
		p.add(passwordL);
		final JTextField password = new JTextField(5);
		password.setEnabled(false);
		p.add(password);
		p3.add(p);

		final JCheckBox cb = new JCheckBox("Proxy Authentication Required",false);
		cb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean tf = cb.isSelected();
				username.setEnabled(tf);
				usernameL.setEnabled(tf);
				password.setEnabled(tf);
				passwordL.setEnabled(tf);
			}
		});
		p3.add(cb, BorderLayout.NORTH);
		p2.add(p3, BorderLayout.SOUTH);
		d.getContentPane().add(p2);

		p = new JPanel();
		JButton b = new JButton("OK");
		b.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Integer.parseInt(port.getText().trim());
				} catch (NumberFormatException ex) {
					return;
				}

				String s = host.getText().trim() + "\t" + port.getText().trim();
				if (!proxies.contains(s))
					proxies.add(s);

				if (cb.isSelected()) {
					Authenticator.setDefault(new Authenticator() {
						protected PasswordAuthentication pAuth =
							new PasswordAuthentication(username.getText(), password.getText().toCharArray());
						protected PasswordAuthentication getPasswordAuthentication() {
							return pAuth;
						}
					});
				}
				d.dispose();
			}
		});
		p.add(b);

		b = new JButton("Cancel");
		b.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				d.dispose();
			}
		});
		p.add(b);

		d.getContentPane().add(p, BorderLayout.SOUTH);
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		d.setLocationRelativeTo(MapApp.anchor);
		d.pack();
		d.setVisible(true);
	}
	
	private void processBorder() {
		if (map.getMapBorder() == null) return;

		defaultFont = map.getMapBorder().getFont();
		for (int i = 0; i< 4; i++) {
			dfltSides[i] = map.getMapBorder().isSideSelected(i);
		}
	}

	public XMap getMap() {
		return map;
	}
	public static JMenuBar getMenuBar() {
		menuOnOff = frame.getJMenuBar();
		return menuOnOff;
	}
	public JFrame getFrame() {
		return frame;
	}
	public MapProjection getProjection() {
		if(null != map) {
			return map.getProjection();
		}
		return null;
	}
	protected void checkVersion() {
		if (AT_SEA) {
			return;
		}
		URL url=null;
		/*try {
			String versionURL = PathUtil.getPath("VERSION_PATH",
					BASE_URL+"/gma_version/").replace("//","/")
					.replaceFirst(":/",  "://") + "version";
			url = URLFactory.url(versionURL);

			BufferedReader in = new BufferedReader(new InputStreamReader( url.openStream() ));
			String version = in.readLine();*/
			String version = "Unknown";
			try {
				version = VersionUtil.getVersion("GeoMapApp");
			}
			catch(JSONException e) {
				e.printStackTrace();
			}
			try {
				String alertPath = PathUtil.getPath("HTML/HTML_PATH",
						BASE_URL+"/gma_html/") + "GMA_Alert.html";
				String queryStr = "os=" + which_os.name() + "&gma_version=" + VERSION;
				url = URLFactory.url(alertPath + "?" + queryStr);
				JEditorPane jep = new JEditorPane(url);
				JPanel panel = new JPanel( new BorderLayout() );
				JScrollPane sp = new JScrollPane(jep);
				sp.setPreferredSize( new Dimension(600,400) );
				sp.setSize( new Dimension(600,400) );
				panel.add( sp );
				JOptionPane.showMessageDialog( MapApp.anchor, panel, "GeoMapApp Alert", JOptionPane.INFORMATION_MESSAGE);
			//	System.out.println( jep.getText() );
			}
			catch(Exception e) {
			}
			if( compareVersions(VERSION, version) < 0) {
				GMADownload.download( VERSION, version);
			}
		/*} catch (IOException ex ) {
			JOptionPane.showMessageDialog(frame,
					"The server: " + url.getHost() + "\n is not available. Please be patient.",
					getBaseURL(), JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
			// System.exit(0);
		}*/
	}
	
	public void showMailingListPopup() {
		JDialog dialog = new JDialog((Frame)null, "Join GeoMapApp Mailing List", true);
		ActionListener listener = new ActionListener() {
			boolean isShowingAgain = true;
			private void createNewFileIfNotExists() {
				try {
					if(!showMailingListFile.exists()) {
						showMailingListFile.createNewFile();
					}
				}
				catch(IOException e) {
					e.printStackTrace();
				}
			}
			public void actionPerformed(ActionEvent event) {
				String cmd = event.getActionCommand();
				switch(cmd) {
				case "mailingList":
					String url = PathUtil.getPath("ANNOUNCE_PATH");;
					BrowseURL.browseURL( url );
					if(!isShowingAgain) {
						createNewFileIfNotExists();
					}
					dialog.setVisible(false);
					break;
				case "nojoin":
					dialog.setVisible(false);
					if(!isShowingAgain) {
						createNewFileIfNotExists();
					}
					break;
				case "noshow":
					if(event.getSource() instanceof JCheckBox) {
						isShowingAgain = !((JCheckBox)event.getSource()).isSelected();
					}
					break;
				}
			}
		};
		JPanel panel = new JPanel();
		dialog.add(panel);
		JButton joinMailingListBtn = new JButton("Join Mailing List");
		joinMailingListBtn.setActionCommand("mailingList");
		joinMailingListBtn.addActionListener(listener);
		panel.add(joinMailingListBtn);
		JButton dontJoinBtn = new JButton("Don't Join Mailing List Right Now");
		dontJoinBtn.setActionCommand("nojoin");
		dontJoinBtn.addActionListener(listener);
		panel.add(dontJoinBtn);
		JCheckBox dontShowAgain = new JCheckBox("Don't Show This Window Again", false);
		dontShowAgain.setActionCommand("noshow");
		dontShowAgain.addActionListener(listener);
		panel.add(dontShowAgain);
		dialog.pack();
		dialog.setLocationRelativeTo(anchor);
		anchor = dialog;
		dialog.setVisible(true);
	}
	
	public static boolean isJar() {
		String str = String.valueOf(MapApp.class.getResource("MapApp.class"));
		System.out.println(str);
		return str.startsWith("jar:");
	}

	/*
	 * read in the sha256 hash for the Dev Server password from the server
	 */
	private String getDevPasswordHash() {
		URL url=null;
		String passwordURL = BASE_URL + DEV_PASSWORD_PATH; 
		try {
			url = URLFactory.url(passwordURL);
			BufferedReader in = new BufferedReader(new InputStreamReader( url.openStream() ));
			return in.readLine();
		} catch (IOException ex ) {
			JOptionPane.showMessageDialog(frame,
					"The server: " + url.getHost() + "\n is not available. Please be patient.",
					getBaseURL(), JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}
		return null;
	}
	
	public String toString() {
		return GEOMAPAPP_NAME;
	}

	public int getMapType() {
		return whichMap;
	}

	protected void SPInit() {
		sendLogMessage("Launching_In_SP");
		JWindow startup = new JWindow();
		startSP = new StartUp(SOUTH_POLAR_MAP);
		Container c = startup.getContentPane();
		c.add(startSP, "Center");
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		startup.pack();
		Dimension win = startup.getSize();
		startup.setLocation( (screen.width-win.width)/2, (screen.height-win.height)/2 );
		startup.setVisible(true);

		PolarStereo proj = new PolarStereo( new Point(320, 320),
				180., 25600., -71., PolarStereo.SOUTH, PolarStereo.WGS84, DEFAULT_LONGITUDE_RANGE);
		int width = 640;
		int height = 640;
		map = new XMap( this, proj, width, height);
		PolarMapBorder border = new PolarMapBorder(map);
		map.setMapBorder(border);

		frame = createBaseFrame(GEOMAPAPP_NAME);
		initLayerManager();

		startSP.setText("Composing South Polar Basemap Image");
		baseMap = new MapOverlay( map );
		if( !PoleMapServer.getImage( new Rectangle(0,0,640,640), baseMap, PoleMapServer.SOUTH_POLE) ) {
			System.out.println("unable to create base map");
			System.exit(0);
		}

		PoleMapServer.getMaskImage( new Rectangle(0,0,640,640), baseMap, PoleMapServer.SOUTH_POLE);
		map.addOverlay(baseMapName,baseMap,false);

		CURRENT_PROJECTION = "s";
		startSP.setText("Initializing GUI");
		initGUI();
		startup.dispose();
		startSP = null;
	}

	protected void SPInit2() {
		sendLogMessage("Switching_To_SP");
		mapScale = null;
		JWindow startup = new JWindow();
		startSP = new StartUp(SOUTH_POLAR_MAP);
		Container c = startup.getContentPane();
		//c.add(startSP, "Center");
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		startup.pack();
		Dimension win = startup.getSize();
		startup.setLocation( (screen.width-win.width)/2, (screen.height-win.height)/2 );
		startup.setVisible(true);

		// See if existing projection is open
		try {
			if(frame.getTitle() !=null){
				switchingProjection = true;
				// dispose of all open frames
				for (Frame thisFrame : Frame.getFrames()) {
					thisFrame.dispose();
				}	
				whichMap=MapApp.SOUTH_POLAR_MAP;
				System.gc();
			}
		} catch(NullPointerException npx) {
			System.out.println("null");
		}

		//get the longitude range for the current projection and carry it forward to the new one 
		int lonRange;
		try {
			lonRange = map.getProjection().getLongitudeRange();
		} catch(Exception e) {
			lonRange = DEFAULT_LONGITUDE_RANGE;
		}
		
		PolarStereo proj = new PolarStereo( new Point(320, 320),
				180., 25600., -71., PolarStereo.SOUTH, PolarStereo.WGS84, lonRange);
		int width = 640;
		int height = 640;
		map = new XMap( this, proj, width, height);
		PolarMapBorder border = new PolarMapBorder(map);
		map.setMapBorder(border);

		frame = createBaseFrame(GEOMAPAPP_NAME);
		initLayerManager();

		startSP.setText("Composing South Polar Basemap Image");
		baseMap = new MapOverlay( map );
		if( !PoleMapServer.getImage( new Rectangle(0,0,640,640), baseMap, PoleMapServer.SOUTH_POLE) ) {
			System.out.println("unable to create base map");
			System.exit(0);
		}

		PoleMapServer.getMaskImage( new Rectangle(0,0,640,640), baseMap, PoleMapServer.SOUTH_POLE);
		map.addOverlay(baseMapName,baseMap,false);

		CURRENT_PROJECTION = "s";
		// startSP.setText("Initializing GUI");
		//unload any loaded databases
		for (Database database : db) {
			database.unloadDB();
		}	
		closeDSDP();
		initGUI();
		startup.dispose();
		startSP = null;
	}

	public void detach_attachDoClick(){
		detach_attachB.doClick();
	}

	protected void NPInit2() {
		sendLogMessage("Switching_To_NP");
		mapScale = null;
		JWindow startup2 = new JWindow();
		startNP = new StartUp(NORTH_POLAR_MAP);
		Container c = startup2.getContentPane();
		//c.add(startNP, "Center");
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		startup2.pack();
		Dimension win = startup2.getSize();
		startup2.setLocation( (screen.width-win.width)/2, (screen.height-win.height)/2 );
		// Compose Basemap Image
		startNP.setText("Composing North Polar Basemap Image");
		startup2.setVisible(true);

		// See if existing projection is open
		try{
			if(frame.getTitle() !=null) {
				switchingProjection = true;
				// dispose of all open frames
				for (Frame thisFrame : Frame.getFrames()) {
					thisFrame.dispose();
				}
				whichMap=MapApp.NORTH_POLAR_MAP;
				System.gc();
			}
		} catch(NullPointerException npx) {
			System.out.println("null");
		}

		//get the longitude range for the current projection and carry it forward to the new one 
		int lonRange;
		try {
			lonRange = map.getProjection().getLongitudeRange();
		} catch(Exception e) {
			lonRange = DEFAULT_LONGITUDE_RANGE;
		}
		
		PolarStereo proj = new PolarStereo( new Point(320, 320),
				0., 25600., 71., PolarStereo.NORTH, PolarStereo.WGS84, lonRange );
		int width = 640;
		int height = 640;
		map = new XMap( this, proj, width, height);
		PolarMapBorder border = new PolarMapBorder(map);
		map.setMapBorder(border);
		frame = createBaseFrame(GEOMAPAPP_NAME);
		initLayerManager();

		baseMap = new MapOverlay( map );
		if( !PoleMapServer.getImage( new Rectangle(0,0,640,640), baseMap, PoleMapServer.NORTH_POLE) ) {
			System.out.println("unable to create base map");
			System.exit(0);
		}
		PoleMapServer.getMaskImage( new Rectangle(0,0,640,640), baseMap, PoleMapServer.NORTH_POLE);
		map.addOverlay(baseMapName,baseMap,false);
		CURRENT_PROJECTION = "n";
		//unload any loaded databases
		for (Database database : db) {
			database.unloadDB();
		}
		closeDSDP();
		initGUI();
		//startNP.setText("Initializing GUI");
		startup2.dispose();
		startNP = null;
	}

	protected void NPInit() {
		sendLogMessage("Launching_In_NP");
		JWindow startup = new JWindow();
		startNP = new StartUp(NORTH_POLAR_MAP);
		Container c = startup.getContentPane();
		c.add(startNP, "Center");
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		startup.pack();
		Dimension win = startup.getSize();
		startup.setLocation( (screen.width-win.width)/2, (screen.height-win.height)/2 );
		startup.setVisible(true);

		PolarStereo proj = new PolarStereo( new Point(320, 320),
				0., 25600., 71., PolarStereo.NORTH, PolarStereo.WGS84, DEFAULT_LONGITUDE_RANGE);
		int width = 640;
		int height = 640;
		map = new XMap( this, proj, width, height);
		PolarMapBorder border = new PolarMapBorder(map);
		map.setMapBorder(border);

		frame = createBaseFrame(GEOMAPAPP_NAME);
		initLayerManager();

		startNP.setText("Composing North Polar Basemap Image");
		baseMap = new MapOverlay( map );
		if( !PoleMapServer.getImage( new Rectangle(0,0,640,640), baseMap, PoleMapServer.NORTH_POLE) ) {
			System.out.println("unable to create base map");
			System.exit(0);
		}
		PoleMapServer.getMaskImage( new Rectangle(0,0,640,640), baseMap, PoleMapServer.NORTH_POLE);
		map.addOverlay(baseMapName,baseMap,false);
		CURRENT_PROJECTION = "n";
		startNP.setText("Initializing GUI");
		initGUI();
		startup.dispose();
		startNP = null;
	}

	protected void MInit() {
		sendLogMessage("Launching_In_Mercator");
		JWindow startup = new JWindow();
		start = new StartUp();
		startup.getContentPane().add(start, "Center");
	//	startup.getContentPane().add(start.label, "North");
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		startup.pack();
		Dimension win = startup.getSize();
		//startup.setLocation( (screen.width-win.width)/2, (screen.height-win.height)/2 );
		startup.setLocationRelativeTo(anchor);
		startup.show();
		Mercator proj = ProjectionFactory.getMercator( 640 );
		double lat = proj.getLatitude( -260. );
		proj = new Mercator( 0., lat, 640, Projection.SPHERE, DEFAULT_LONGITUDE_RANGE);
		int width = 1280;
		int height = 498;
		map = new XMap( this, proj, width, height);
		CylindricalMapBorder border = new CylindricalMapBorder(map);
		map.setMapBorder(border);
		frame = createBaseFrame(GEOMAPAPP_NAME);
		initLayerManager();
		start.setText("Composing Mercator Basemap Image");

		baseMap = new MapOverlay( map );
//		 GMA 1.6.0 Changed MMServer to the getBaseMap method
		if( !MMapServer.getBaseImage( new Rectangle(0,0,640,498), baseMap) ) {
			System.out.println("unable to create base map");
			System.exit(0);
		}
	//	MMapServer.getMaskImage( new Rectangle(0,0,640,498), baseMap, 512);
		GridComposer.getMask( new Rectangle(0,0,640,498), baseMap );
		map.addOverlay(baseMapName,baseMap,false);

		CURRENT_PROJECTION = "m";
		start.setText("Initializing GUI");
		initGUI();
		startup.dispose();
		start = null;
	}

	protected void MInit2() {
		sendLogMessage("Switching_To_Mercator");
		mapScale = null;
		JWindow startup = new JWindow();
		start = new StartUp();
		//startup.getContentPane().add(start, "Center");
	//	startup.getContentPane().add(start.label, "North");
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		startup.pack();
		Dimension win = startup.getSize();
		startup.setLocation( (screen.width-win.width)/2, (screen.height-win.height)/2 );
		startup.show();

		// See if existing projection is open
		try {
			if(frame.getTitle() !=null){
				switchingProjection = true;
				// dispose of all open frames
				for (Frame thisFrame : Frame.getFrames()) {
					thisFrame.dispose();
				}		
				whichMap=MapApp.MERCATOR_MAP;
				System.gc();
			}
		} catch(NullPointerException npx) {
			System.out.println("null");
		}

		//get the longitude range for the current projection and carry it forward to the new one 
		int lonRange;
		try {
			lonRange = map.getProjection().getLongitudeRange();
		} catch(Exception e) {
			lonRange = DEFAULT_LONGITUDE_RANGE;
		}
		
		Mercator proj = ProjectionFactory.getMercator( 640 );
		double lat = proj.getLatitude( -260. );
		proj = new Mercator( 0., lat, 640, Projection.SPHERE, lonRange);
		int width = 1280;
		int height = 498;
		map = new XMap( this, proj, width, height);
		CylindricalMapBorder border = new CylindricalMapBorder(map);
		map.setMapBorder(border);
		frame = createBaseFrame(GEOMAPAPP_NAME);
		initLayerManager();
		start.setText("Composing Mercator Basemap Image");

		baseMap = new MapOverlay( map );
//		 GMA 1.6.0 Changed MMServer to the getBaseMap method
		if( !MMapServer.getBaseImage( new Rectangle(0,0,640,498), baseMap) ) {
			System.out.println("unable to create base map");
			System.exit(0);
		}
	//	MMapServer.getMaskImage( new Rectangle(0,0,640,498), baseMap, 512);
		GridComposer.getMask( new Rectangle(0,0,640,498), baseMap );
		map.addOverlay(baseMapName,baseMap,false);

		CURRENT_PROJECTION = "m";
		//unload any loaded databases
		for (Database database : db) {
			database.unloadDB();
		}
		closeDSDP();
		start.setText("Initializing GUI");
		initGUI();
		startup.dispose();
		start = null;
	}

	protected JFrame createBaseFrame(String name) {
		JFrame frame = new JFrame(name);
		frame.addWindowFocusListener(new WindowFocusListener() {
			public void windowLostFocus(WindowEvent e) {
				for (Component c : menuBar.getComponents()) {
					JMenu m = (JMenu) c;
					if (m.getPopupMenu().isVisible()) {
						m.menuSelectionChanged(false);
						m.getPopupMenu().setVisible(false);
					}
				}
			}
		public void windowGainedFocus(WindowEvent e) {}
		});
		frame.setLocationRelativeTo(anchor);
		return frame;
	}

	protected void WWInit() {
		JOptionPane.showMessageDialog(MapApp.anchor, "Unsupported Map Selected", "Error", JOptionPane.ERROR_MESSAGE);
		System.exit(-1);
	}

	protected void initGUI() {
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		// System.out.println(whichMap);
		// create "focus" overlay with NULL image
		focus = new Grid2DOverlay( map, baseFocusName );
		baseMapFocus = focus;
		map.addOverlay(baseFocusName, focus);

		if( whichMap==MapApp.SOUTH_POLAR_MAP ) {
			shoreLine = new ShoreLine( map );
			try {
				String shoreLinePath = PathUtil.getPath("SP_SHORE_PATH",
						MapApp.BASE_URL+"/data/gmrt_tiles/sp/") +"SP.shore";
				shoreLine.load( shoreLinePath );
				map.addOverlay( "South Polar Shorelines", shoreLine, false );
			} catch( IOException ex ) {
				shoreLine=null;
				ex.printStackTrace();
			}
		}

		// Initialize zoomer
		if( whichMap==MapApp.MERCATOR_MAP ) {
			start.setText("Initializing Zoomer");
		} else if (whichMap==MapApp.SOUTH_POLAR_MAP ) {
			startSP.setText("Initializing Zoomer");
		} else if (whichMap==MapApp.NORTH_POLAR_MAP) {
			startNP.setText("Initializing Zoomer");
		}
		zoomer = new Zoomer(map);
		map.addMouseListener(zoomer);
		map.addMouseMotionListener(zoomer);
//		map.addMouseWheelListener(zoomer);
		map.addKeyListener(zoomer);
		map.addKeyListener(this);
		map.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); // no border
		JScrollPane sp = new JScrollPane(map);

		// Listens for scrolling to request an AutoFocus
		AdjustmentListener al = new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				autoFocus();
			}
		};
		sp.getHorizontalScrollBar().addAdjustmentListener(al);
		sp.getVerticalScrollBar().addAdjustmentListener(al);

		tools = new MapTools(this, map);
		tools.digitizeB.addActionListener(this);
		JPanel mainPanel = new JPanel(new BorderLayout() );
		mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
		mainPanel.add(tools.getTools(), "North");	// Add top Tool Bar
		frame.getContentPane().add(mainPanel, "Center");
		frame.addWindowListener( new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				sendLogMessage("Exiting_GMA");
				System.exit(0);
			}
		});

		progress = new org.geomapapp.util.ProgressDialog(frame);

		// Initialize Database
		if( whichMap==MapApp.MERCATOR_MAP ) {
			start.setText("Initializing Database");
		} else if (whichMap==MapApp.SOUTH_POLAR_MAP ) {
			startSP.setText("Initializing Database");
		} else if (whichMap==MapApp.NORTH_POLAR_MAP) {
			startNP.setText("Initializing Database");
		}
		initDB();

		locs = new MapPlaces(map, tools);

		hPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT ); // Right split pane (Data Tables)
		hPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0)); // no border
		hPane.setLeftComponent( sp );
		hPane.setOneTouchExpandable(true);

		//Add Title Label, Close Button, Attach/Detach Button.
		panel = new JPanel( new GridLayout(0,1) );
		dbLabel = new JLabel("");
		dbLabel.setForeground(Color.black);
		dbLabel.setHorizontalAlignment(JLabel.CENTER);
		dbLabel.setBorder(BorderFactory.createEtchedBorder());
		panel.add(dbLabel);

		closeDB = new JButton("Close");
		closeDB.addActionListener(this);
		panel.add(closeDB);

		detach_attachB = new JButton("Detach");
		detach_attachB.addActionListener(this);
		panel.add(detach_attachB);

		dialog = new JPanel(new BorderLayout());
		dialog.add( panel, "North");
		dialog.setPreferredSize(new Dimension(125, 1000));
		dialog.setMinimumSize(new Dimension(125,8));
		// For vPane Panel to scroll
		dialogScroll = new JScrollPane(dialog);
		dialogScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		dialogScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		vPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT ); // Lower split pane (Data Tables)
		vPane.setTopComponent( hPane );
		vPane.setOneTouchExpandable(true);
		vPane.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY)); // just bottom border
		mainPanel.add(vPane, "Center");

		tools.createEmptyBorders();
		//Before we begin check to make sure xmls are there
		File layerSessionF = new File(layerSessionDir, "SessionsMenu.xml");
		File mySessionF = new File(layerSessionDir, "MySessions.xml");
		if (!layerSessionF.exists()) {
			LayerManager.checkLayerFileStructure();
		}
		// Create main menu bar
		String mainMenuURL = System.getProperty("geomapapp.menus_url");
		File mainMenuFile=null;
		if (mainMenuURL == null) {
			// Check to see if we want dev server
			if(BASE_URL.matches(DEV_URL)){
				mainMenuURL = DEV_URL + "gma_menus/main_menu.xml";
				frame.setTitle("**DEVELOPMENT MODE** "+ GEOMAPAPP_NAME); // Change the title in frame to indicate your in Dev mode.
			} else {
				// If not get regular menus from cache or from server?
				frame.setTitle(GEOMAPAPP_NAME);
			}
		}

		try {
			XML_Menu.setMapApp(this);
			// Add Initializing Menu Items
			if( whichMap==MapApp.MERCATOR_MAP ) {
				start.setText("Initializing Menu Items");
			} else if (whichMap==MapApp.SOUTH_POLAR_MAP ) {
				startSP.setText("Initializing Menu Items");
			} else if (whichMap==MapApp.NORTH_POLAR_MAP) {
				startNP.setText("Initializing Menu Items");
			}
			List<XML_Menu> menuLayers = null;

			// Check the cache to determine where to fetch xml menus
			if(fetchCacheMenus) {
				mainMenuFile = new File( menusCacheDir2, "main_menu.xml");
				//mainMenuURL = PathUtil.getPath("MENU_PATH",MapApp.BASE_URL+"/gma_menus/main_menu.xml"); // 3.5.2 and older
				mainMenuURL = PathUtil.getPath("NEW_MENU_PATH_2015",MapApp.BASE_URL+"/gma_menus/main_menu_new_2015.xml");

				if(XML_Menu.validate(mainMenuFile) == false) {
					menuLayers = XML_Menu.parse(mainMenuURL);
				} else if (XML_Menu.validate(mainMenuFile) == true) {
					// Check for File as first item.
					menuLayers = XML_Menu.parse(mainMenuFile);
					try{
						if(menuLayers.get(0).name.contentEquals("File")) {
							//menuLayers = XML_Menu.parse(mainMenuFile);
						} else {
							// First layer doesn't have file. Possible corruption. Set to get from server.
							ReadMenusCache = false;
							menuLayers = XML_Menu.parse(mainMenuURL);
						}
					} catch (IndexOutOfBoundsException ioobex) {
						// First layer doesn't have file. Possible corruption. Set to get from server.
						ReadMenusCache = false;
						menuLayers = XML_Menu.parse(mainMenuURL);
					}
				}
				//System.out.println("true");
			} else {
				//mainMenuURL = PathUtil.getPath("MENU_PATH",MapApp.BASE_URL+"/gma_menus/main_menu.xml"); // 3.5.2 and older
				mainMenuURL = PathUtil.getPath("NEW_MENU_PATH_2015",MapApp.BASE_URL+"/gma_menus/main_menu_new_2015.xml");
				menuLayers = XML_Menu.parse(mainMenuURL);
				//System.out.println("false");
			}
			// Menu Bar is created
			menuBar = XML_Menu.createMainMenuBar(menuLayers);
			//menuBar = XML_Menu.createMainMenuBar(XML_Menu.parse(mainMenuURL));
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		}

		if (!mySessionF.exists()) {
			frame.setJMenuBar(menuBar);
		} else {
			loadSessionCustomMainMenu();
		}

		JMenuItem mi = XML_Menu.commandToMenuItemHash.get("map_inset_cmd");
		if ( mi != null ) {
			((JCheckBoxMenuItem)mi).setSelected(true);
		}
		mi = XML_Menu.commandToMenuItemHash.get("bathymetry_credits_cmd");
		if ( mi != null ) {
			((JCheckBoxMenuItem)mi).setSelected(true);
		}
		//mi = XML_Menu.commandToMenuItemHash.get("cmt_cmd");
		//if (mi != null) 
		//	mi.setEnabled(false);

		colorScaleCB = (JCheckBoxMenuItem)
			XML_Menu.commandToMenuItemHash.get("color_scale_cmd");
		locs.showLoc = (JCheckBoxMenuItem) XML_Menu.getMenutItem("show_places_cmd");

		// Initializing Elevation
		String initializingElevation = "Initializing Elevation Data Sources";
		if( whichMap==MapApp.MERCATOR_MAP ) {
			start.setText(initializingElevation);
		} else if (whichMap==MapApp.SOUTH_POLAR_MAP ) {
			startSP.setText(initializingElevation);
		} else if (whichMap==MapApp.NORTH_POLAR_MAP) {
			startNP.setText(initializingElevation);
		}

		try {
			credit = new Credit(map, getMapType());
			map.addOverlay( "Elevation Data Sources", credit, false );
			frame.getContentPane().add( credit.getPanel(), "South");
		} catch(Exception e) {
			e.printStackTrace();
		}

		if (whichMap == MERCATOR_MAP) {
			//first remove any existing location inset
			li = null;
			//then add a new one
			addMapInset();
		}
		frame.pack();
		frame.setSize( 1000, 710 );
		frame.setLocationRelativeTo(anchor);
		anchor = frame;
		frame.setVisible(true);
		getGMARoot();
	}

	protected void showDSDP() {
		addProcessingTask("Seafloor Drilling, Coring and Logging (DSDP-ODP-IODP)",
				new Runnable() {
			public void run() {
				if( dsdp==null ) dsdp=new DSDPDemo(MapApp.this);
				dsdp.show();
				dsdp.toFront();
			}
		});
	}

	public void closeDSDP(){
		if (dsdp == null) return;
		dsdp.close();
		dsdp = null;
		// Check if menu item is still selected, deselect it on close.
		deselectDSDP();
	}

	public void deselectDSDP() {
		if ( XML_Menu.commandToMenuItemHash != null &&
				XML_Menu.commandToMenuItemHash.containsKey("seafloor_driling_cmd") &&
				((JCheckBoxMenuItem)XML_Menu.commandToMenuItemHash.get("seafloor_driling_cmd")).isSelected()) {
			((JCheckBoxMenuItem)XML_Menu.commandToMenuItemHash.get("seafloor_driling_cmd")).setSelected(false);
		}
	}

	// Loads GMA regular XML menu
	public void loadSystemMainMenu() {
		//Retrieving XML main menus
		String mainMenuURL = System.getProperty("geomapapp.menus_url");
		if (mainMenuURL == null) {
		//	mainMenuURL = PathUtil.getPath("MENU_PATH",MapApp.BASE_URL+"/gma_menus/main_menu.xml"); // 3.5.2 and older
			mainMenuURL = PathUtil.getPath("NEW_MENU_PATH_2015",MapApp.BASE_URL+"/gma_menus/main_menu_new_2015.xml");
		}
		try {
			XML_Menu.setMapApp(this);
			menuBar = XML_Menu.createMainMenuBar(XML_Menu.parse(mainMenuURL));
			frame.setJMenuBar(menuBar);
			System.out.println("Main menu loaded");
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		}
		//Delete MySession file
		File mySessionFile = new File(layerSessionDir + File.separator + "MySessions.xml");
		if(mySessionFile.exists() && !mySessionFile.delete()){
			System.out.println("Could not delete file");
			return;
		}
	}

	// Loads custom GMA regular XML menu
	public void loadSessionCustomMainMenu() {
		//Retrieving XML main menus
		String mainMenuURL = System.getProperty("geomapapp.menus_url");
		// Check to make sure xml files are in layers dir.
		File customMenuURL = new File(layerSessionDir, "SessionsMenu.xml");
		File sessionsFile = new File(layerSessionDir, "MySessions.xml");

		if (mainMenuURL == null) {
			//mainMenuURL = PathUtil.getPath("MENU_PATH",MapApp.BASE_URL+"/gma_menus/main_menu.xml"); // 3.5.2 and older
			mainMenuURL = PathUtil.getPath("NEW_MENU_PATH_2015",MapApp.BASE_URL+"/gma_menus/main_menu_new_2015.xml");
		}
		if(!customMenuURL.exists()) {
			//Check and create.
				LayerManager.checkLayerFileStructure();
			}

		try {
			XML_Menu.setMapApp(this);
			menuBar = XML_Menu.createMainMenuBars(XML_Menu.parse(mainMenuURL),XML_Menu.parse(customMenuURL));
			frame.setJMenuBar(menuBar);

			//Delete the tmp Session file
			File tmp = new File(layerSessionDir + File.separator + "MySessions.xml.tmp");
			if (tmp.exists() && !tmp.delete()) {
				System.out.println("Could not delete file");
				return;
			}
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		}
	}

	// Reloads GMA regular XML menu
	public void reloadSessionMainMenu() throws ConnectException{
		XML_Menu.setMapApp(this);
		frame.getJMenuBar().removeAll();	//Clear All

		//Retrieving XML main menus. Reload.
		String mainMenuURL = System.getProperty("geomapapp.menus_url");
		// Check to make sure xml file are in layers dir
		File customMenuURL = new File(layerSessionDir, "SessionsMenu.xml");
		File sessionsFile = new File(layerSessionDir, "MySessions.xml");

		if (mainMenuURL == null) {
		//	mainMenuURL = PathUtil.getPath("MENU_PATH",MapApp.BASE_URL+"/gma_menus/main_menu.xml"); // 3.5.2 and older
			mainMenuURL = PathUtil.getPath("NEW_MENU_PATH_2015",MapApp.BASE_URL+"/gma_menus/main_menu_new_2015.xml");
		}

		if( !customMenuURL.exists()) {
			//Check and create.
				LayerManager.checkLayerFileStructure();
			}

		try {
			XML_Menu.setMapApp(this);
			menuBar = XML_Menu.createMainMenuBars(XML_Menu.parse(mainMenuURL),XML_Menu.parse(customMenuURL));
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
	}

	public static File getGMARoot() {
		return org.geomapapp.io.GMARoot.getRoot();
	}
	public static org.geomapapp.util.ProgressDialog getProgressDialog() {
		return progress;
	}
	
	public MapColorScale getColorScale() {
		return colorScale;
	}

	void initDB() {
		custom = new CustomDB(map);
		portal_commands = haxby.util.PortalCommands.getPortalCommands();
		digitizer = new Digitizer(map);

		switch (whichMap) {
		case MapApp.MERCATOR_MAP:
			int ndb = 12;

			db = new Database[ndb];
			db[0] = (Database) new MGG(map, 2900);
			if( directory != null ) {
				db[1] = (Database) new MBTracks(map, 4000,
					"file:"+(new File(directory)).getPath()+"/mb_control") ;
			} else {
				db[1] = (Database) new MBTracks(map, 4000);
			}
			db[2] = (Database) new PDB(map);
			db[3] = (Database) new EQ(map);
			db[4] = (Database) new FocalMechanismSolutionDB(map);
			db[5] = (Database) new PMEL(map);
			db[6] = (Database) new XMCS(map);
			db[7] = (Database) new SCS( map );
			db[8] = (Database) new Isochrons( map );
			db[9] = (Database) new Ship(map , 4000);
			db[10] = (Database) new SurveyPlanner(map);
			db[11] = (Database) new VelocityVectors(map);

			break;

		case MapApp.SOUTH_POLAR_MAP:
			db = new Database[10];
			db[0] = (Database) new MGG(map, 2900);
			if( directory != null ) {
				db[1] = new MBTracks(map, 4000,
					"file:"+(new File(directory)).getPath()+"/mb_control") ;
			} else {
				db[1] = (Database) new MBTracks(map, 4000);
			}
			db[2] = (Database) new EQ(map);
			db[3] = new FocalMechanismSolutionDB(map);
			db[4] = (Database) new Radar( map );
			db[5] = (Database) new SCS( map );
			db[6] = (Database) new XMCS( map );
			db[7] = (Database) new Isochrons( map );
			db[8] = new PDB(map);
			db[9] = (Database) new Ship(map , 4000);
			break;

		case MapApp.NORTH_POLAR_MAP:
			db = new Database[9];
			db[0] = (Database) new MGG(map, 2900);
			if( directory != null ) {
				db[1] = new MBTracks(map, 4000,
					"file:"+(new File(directory)).getPath()+"/mb_control") ;
			} else {
				db[1] = (Database) new MBTracks(map, 4000);
			}
			db[2] = (Database) new EQ(map);
			db[3] = (Database) new FocalMechanismSolutionDB(map);
			db[4] = (Database) new haxby.db.scs.SCS( map );
			db[5] = (Database) new haxby.db.xmcs.XMCS( map );
			db[6] = (Database) new haxby.db.age.Isochrons( map );
			db[7] = new PDB(map);
			db[8] = (Database) new haxby.db.ship.Ship(map , 4000);
			break;

		default:
			break;
		}
	}
	public void keyPressed( KeyEvent evt ) {
	}
	public void keyTyped( KeyEvent evt ) {
	}
	public void keyReleased( KeyEvent evt ) {
		if( evt.getKeyCode() == KeyEvent.VK_F ) {
			tools.focus.doClick();
		} else if( evt.getKeyCode() == KeyEvent.VK_M ) {
			tools.maskB.doClick();
		}
	}
	public static Point getAnchorLocation() {
		if(null == anchor) return new Point(0,0);
		return anchor.getLocation();
	}
	public static int getAnchorX() {
		if(null == anchor) return 0;
		return anchor.getX();
	}
	public static int getAnchorY() {
		if(null == anchor) return 0;
		return anchor.getY();
	}
	public void setMask( boolean tf ) {
		if(tf) {
			Thread focusMask = null;		
			switch (whichMap) {
				case MapApp.MERCATOR_MAP:
					focusMask = new Thread("Mask Thread " + System.currentTimeMillis()) {
						public void run() {
							if (!GridComposer.getMask(map.getClipRect2D(), baseMap)) {
								System.out.println("mask failed");
							}
							map.repaint();
						}
					};
				break;
				case MapApp.NORTH_POLAR_MAP:
					focusMask = new Thread("Mask Thread " + System.currentTimeMillis()) {
						public void run() {
							if(!PoleMapServer.getMaskImage(map.getClipRect2D(),
									baseMap, 
														   PoleMapServer.NORTH_POLE)) {
								System.out.println("mask failed");
							}
							map.repaint();
						}
					};
				break;
				case MapApp.SOUTH_POLAR_MAP:
					focusMask = new Thread("Mask Thread " + System.currentTimeMillis()) {
						public void run() {
							if(!PoleMapServer.getMaskImage(map.getClipRect2D(),
									baseMap, 
														   PoleMapServer.SOUTH_POLE)) {
								System.out.println("mask failed");
							}
							map.repaint();
						}
					};
				break;
			}
			addProcessingTask("Mask", focusMask);
		}
		
		baseMap.maskImage(tf);
		// if we're turning the mask on for the first time, we have to load it
		map.repaint();
	}

	// GMA 1.6.2: Set the size of the main GeoMapApp window
	public void setFrameSize( int width, int height ) {
		frame.setSize( width, height );
		vPane.resetToPreferredSizes();
	}
	
	private int whichCursor = Cursor.DEFAULT_CURSOR;
	private boolean isWaiting = false;
	
	public synchronized void startWaiting() {
		new Thread(new Runnable() {
			public void run() {
				if(!isWaiting) {
					whichCursor = anchor.getCursor().getType();
					frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					map.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					isWaiting = true;
				}
			}
		}).start();
	}
	
	public synchronized void stopWaiting() {
		new Thread(new Runnable() {
			public void run() {
				frame.setCursor(Cursor.getPredefinedCursor(whichCursor));
				map.setCursor(Cursor.getPredefinedCursor(whichCursor));
				isWaiting = false;
			}
		}).start();
	}

	public void mapFocus() {
		focusTime = -1;
		focus = map.getFocus();

		if ( tools.gridDialog != null ) {
			if ( tools.gridDialog.gridCB.getItemCount() != 0 && tools.gridDialog.isLoaded() ) {
				tools.gridDialog.refreshGrids();
			}
		}

		//NSS 01/31/16 commented out to prevent grid loads getting caught in loop.
		//Need to keep an eye out in case this causes problems elsewhere.
//		if (tools.gridDialog.isLoaded()) {
//			Grid2DOverlay gridOverlay = (Grid2DOverlay)tools.gridDialog.gridCB.getSelectedItem();
//			// Check that the grid is visible
//			if (isLayerVisible(gridOverlay)) {
//				tools.gridDialog.startGridLoad();
//			}
//		}

		// Basemap Focus:
		if ((isBaseMapVisible() )) {
			// Image requests use SilentProcessingTask, since their progress is self-evident
			// as the tiles are progressively painted
			if (map.getZoom() > 1.5) {
				SilentProcessingTask focusImage = 
						new GetImageRequest(map.getClipRect2D(),
								baseMapFocus, 
								"Base Map Image Request",
								whichMap);
				addSilentProcessingTask(focusImage);
			}

			// Mask requests use ProcessingDialog, since they don't progressively repaint screen
			if (baseMap.isMasked()) {
				Thread focusMask = null;
				switch (whichMap) {
					case MapApp.MERCATOR_MAP:
						focusMask = new Thread("Mask Focus Thread " + System.currentTimeMillis()) {
							public void run() {
								if (!GridComposer.getMask(map.getClipRect2D(), baseMap)) {
									System.out.println("mask failed");
								}
								map.repaint();
							}
						};
					break;
					case MapApp.NORTH_POLAR_MAP:
						focusMask = new Thread("Mask Focus Thread " + System.currentTimeMillis()) {
							public void run() {
								if(!PoleMapServer.getMaskImage(map.getClipRect2D(),
										baseMap, 
															   PoleMapServer.NORTH_POLE)) {
									System.out.println("mask failed");
								}
								map.repaint();
							}
						};
					break;
					case MapApp.SOUTH_POLAR_MAP:
						focusMask = new Thread("Mask Focus Thread " + System.currentTimeMillis()) {
							public void run() {
								if(!PoleMapServer.getMaskImage(map.getClipRect2D(),
										baseMap, 
															   PoleMapServer.SOUTH_POLE)) {
									System.out.println("mask failed");
								}
								map.repaint();
							}
						};
					break;
				}
				addProcessingTask("Base Map Focus Mask", focusMask);
			}
		} else {
			map.repaint();
		}

		// Focus Shapes:
		for (Iterator iter = tools.suite.getShapes().iterator(); iter.hasNext();) {
			final ESRIShapefile shape = (ESRIShapefile) iter.next();
			if (isLayerVisible(shape) && shape.getMultiImage() != null)
				addProcessingTask(shape.getName(), new Runnable() {
						public void run() {
							shape.getMultiImage().focus();
							map.repaint();
						}
					});
		}

		// Focus our FocusOverlays
		List<FocusOverlay> cFOS = new ArrayList<FocusOverlay>(focusOverlays.size());
		synchronized (focusOverlays) {
			for (FocusOverlay overlay : focusOverlays)
				cFOS.add(overlay);
		}
		for (FocusOverlay overlay : cFOS) {
			if (isLayerVisible(overlay)) {
				addProcessingTask(overlay.toString(),
						overlay.createFocusTask(map.getClipRect2D()));
			}
		}
	}

	public void save() {
		tools.save();
	}

	public void saveImage() {
		tools.saveImage();
	}

//	***** GMA 1.5.2: Add function to invoke WFS from AccessAllData.java
	public void invokeWFS() {
		if ( wfsWindow != null ) {
			if ( wfsWindow.checkFrame() ) {
				wfsWindow.showFrame();
			}
			else {
				try {
					wfsWindow.remoteWFS();
				} catch (Exception ignore) {
					ignore.printStackTrace();
				}
			}
		}
		else {
			wfsWindow = new WFSViewServer(this);
			try {
				wfsWindow.remoteWFS();
			} catch (Exception ignore) {
				ignore.printStackTrace();
			}
		}
	}

//	***** GMA 1.6.0
	public void invokeWMS() {
		if ( wmsWindow != null ) {
			if ( wmsWindow.checkFrame() ) {
				wmsWindow.showFrame();
			}
			else {
				try {
					wmsWindow.remoteWMS();
				} catch (Exception ignore) {
					ignore.printStackTrace();
				}
			}
		}
		else {
			wmsWindow = new WMSViewServer(this);
			try {
				wmsWindow.remoteWMS();
			} catch (Exception ignore) {
				ignore.printStackTrace();
			}
		}
	}
	//Updates color bar scale
	public void initializeColorScale() {
		if (colorScale != null || !colorScaleCB.isSelected()) {
			map.removeMapInset( colorScale );
			map.removeMouseListener( colorScale );
			map.removeMouseMotionListener( colorScale );
		map.repaint();
		map.updateUI();
		}
		colorScale=new MapColorScale(map);

		if( colorScaleCB.isSelected() ) {
			map.addMapInset( colorScale );
			map.addMouseListener( colorScale );
			map.addMouseMotionListener( colorScale );
		}
	}

	public void removeColorScale() { //	***** GMA 1.6.6
		if( colorScale != null ) {
			map.removeMapInset(colorScale);
			map.removeMouseListener(colorScale);
			map.removeMouseMotionListener(colorScale);
			colorScaleCB.setSelected(false);
		}
	}

	public void addMapInset() {
		if ( li == null ) {
			li = new LocationInset(map);
		}
		map.addMapInset( li );
		map.addMouseListener( li );
		map.addMouseMotionListener( li );
		map.repaint();
	}

	public void removeMapInset() {
		if ( li == null ) {
			li = new LocationInset(map);
		}
		map.removeMapInset( li );
		map.removeMouseListener( li );
		map.removeMouseMotionListener( li );
		map.repaint();
	}

	protected void importImage() {
		new ImportImageLayer().importImage(this);
	}
	
	public void addCruisePreview(String url, String altTilesPath) {
		//figure out the name of the cruise and the info file path
		String[] pathParts = url.split("/");
		String folderName = pathParts[pathParts.length-1];
		String infoFile = url + "/" + folderName + ".gmrt.xml";
		try {
			//fetch and read the info file (XML)
			URL cruiseInfoUrl = new URL(infoFile);
			URLConnection rootConn = cruiseInfoUrl.openConnection();
			BufferedReader infoReader = new BufferedReader(new InputStreamReader(rootConn.getInputStream()));

			//Should be a one- or two-line file, but can't be too careful
			String infoXml = "";
			String line = null;
			while(null != (line = infoReader.readLine())) {
				infoXml += " " + line.trim();
				infoXml = infoXml.trim();
			}
			infoReader.close();
			int cruiseInputIndex = infoXml.indexOf("<CRUISE_INPUT");
			
			//get the best resolution available
			String resolutionAttr = "resolution=\"";
			int resStartIndex = infoXml.indexOf(resolutionAttr, cruiseInputIndex) + resolutionAttr.length();
			int resEndIndex = infoXml.indexOf("\"", resStartIndex);
			String bestRes = infoXml.substring(resStartIndex, resEndIndex);
			
			String weightAttr = "weight=\"";
			int weightStartIndex = infoXml.indexOf(weightAttr, cruiseInputIndex) + weightAttr.length();
			int weightEndIndex = infoXml.indexOf("\"", weightStartIndex);
			String weightFctr = infoXml.substring(weightStartIndex, weightEndIndex);
			
			int shouldContinue = JOptionPane.showConfirmDialog(vPane, "Best resolution found: " + bestRes + "\nWeight: " + weightFctr + "\nContinue loading from " + url.replace("file://", "") + "?", "", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
			if(shouldContinue == JOptionPane.CANCEL_OPTION || shouldContinue == JOptionPane.CLOSED_OPTION || shouldContinue == JOptionPane.NO_OPTION) return;
			cancelOps();
			//pass off the rest of the work to previously existing PreviewCruise
			if(null == altTilesPath) {
				PreviewCruise.showCruise(this, new String[] {url, bestRes});
			}
			else {
				PreviewCruise.showCruise(this, new String[] {url, bestRes, altTilesPath});
			}
		}
		catch (MalformedURLException murle) {
			murle.printStackTrace();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
	}
	
	public void previewCruiseFromMenu(boolean fromLocalFile) {
		String url = null, altTilesUrl = null;
		if(fromLocalFile) {
			JFileChooser chooser = new JFileChooser();
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int option = chooser.showOpenDialog(null);
			if(JFileChooser.APPROVE_OPTION == option) {
				String path = chooser.getSelectedFile().getAbsolutePath();
				if(which_os == OSAdjustment.OS.WINDOWS) {
					path = "/" + Character.toLowerCase(path.charAt(0)) + path.substring(1).replaceAll("\\\\", "/");
				}
				url = "file://" + path;
			}
		}
		else {
			//TODO may later want to add fully custom URLs for cruises
			String cruiseRootDir = "http://dev2.geomapapp.org/cruises/";
			JPanel chooseDirPanel = new JPanel(new GridLayout(0,1));
			JLabel chooseDirLabel = new JLabel("Choose a directory to search in:");
			chooseDirPanel.add(chooseDirLabel);
			JRadioButton todoButton = new JRadioButton("todo", true),
					doneButton = new JRadioButton("done"),
					submittedButton = new JRadioButton("submitted");
			ButtonGroup chooseDirButtons = new ButtonGroup();
			chooseDirButtons.add(todoButton);
			chooseDirButtons.add(doneButton);
			chooseDirButtons.add(submittedButton);
			chooseDirPanel.add(todoButton);
			chooseDirPanel.add(doneButton);
			chooseDirPanel.add(submittedButton);
			/*
			JTextField altTilesUrlTextField = new JTextField("http://dev2.geomapapp.org/");
			JCheckBox loadAltTiles = new JCheckBox("Load alternate base map tiles? If yes, specify URL:");
			chooseDirPanel.add(loadAltTiles);
			chooseDirPanel.add(altTilesUrlTextField);
			*/
			int optionChosen = JOptionPane.showConfirmDialog(vPane, chooseDirPanel, "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if(optionChosen != JOptionPane.OK_OPTION) return;
			/*if(loadAltTiles.isSelected()) {
				altTilesUrl = altTilesUrlTextField.getText();
			}*/
			Enumeration<AbstractButton> buttons = chooseDirButtons.getElements();
			String optStr = null;
			while(buttons.hasMoreElements()) {
				AbstractButton b = buttons.nextElement();
				if(b.isSelected()) {
					optStr = b.getText();
				}
			}
			String cruiseDir = cruiseRootDir + optStr;
			String latestUrl = null;
			try {
				URL cruiseDirUrl = new URL(cruiseDir);
				URLConnection conn = cruiseDirUrl.openConnection();
				latestUrl = cruiseDir;
				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				Stream<String> htmlLines = reader.lines();
				String[] cruises = htmlLines.filter(x -> x.contains("alt=\"[DIR]\""))
						.map(new Function<String, String>() {
							@Override
							public String apply(String line) {
								String linkTag = "a href=\"";
								int startIndex = line.indexOf(linkTag) + linkTag.length();
								int endIndex = line.indexOf("/", startIndex);
								return line.substring(startIndex, endIndex);
							}
						})
						.collect(Collectors.toList()).toArray(new String[] {});
				String cruiseNameStr = (String)JOptionPane.showInputDialog(vPane, "Preview which cruise?", "Choose a cruise (" + optStr + ")", JOptionPane.PLAIN_MESSAGE, null, cruises, cruises[0]);
				if(null != cruiseNameStr) {
					url = cruiseDir + "/" + cruiseNameStr;
				}
			}
			catch(MalformedURLException murle) {
				System.out.println("Malformed URL: " + cruiseDir);
				murle.printStackTrace();
			} catch (IOException e) {
				System.out.println("Could not access the URL: " + latestUrl);
				JOptionPane.showMessageDialog(vPane, "The URL " + latestUrl + " could not be reached.\nThe server might be down.", "", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
		}
		if(null != url) {
			ButtonGroup showDevTiles = new ButtonGroup();
			JRadioButton useDevTilesBtn = new JRadioButton("Development"),
					useProdTilesBtn = new JRadioButton("Production");
			showDevTiles.add(useDevTilesBtn);
			showDevTiles.add(useProdTilesBtn);
			JPanel devTilesDecision = new JPanel(new GridLayout(0,1));
			devTilesDecision.add(new JLabel("Choose the base map tiles:"));
			devTilesDecision.add(useProdTilesBtn);
			devTilesDecision.add(useDevTilesBtn);
			useProdTilesBtn.setSelected(true);
			int shouldContinue = JOptionPane.showConfirmDialog(vPane, devTilesDecision, "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if(JOptionPane.OK_OPTION == shouldContinue) {
				if(useDevTilesBtn.isSelected()) {
					altTilesUrl = "http://dev2.geomapapp.org/";
				}
				System.out.println(url);
				if(null != altTilesUrl) {
					System.out.println(altTilesUrl);
				}
				addCruisePreview(url, altTilesUrl);
			}
		}
	}

	public synchronized void actionPerformed(ActionEvent evt) throws OutOfMemoryError {
		String name = evt.getActionCommand();
		System.out.println(name);
		sendLogMessage(name);

		if (name.equals("ImportWFSCmd")) {
			invokeWFS();
		} else if (name.equals("ImportWMSCmd")) {
			invokeWMS();
		} else if (name.equals("import_image_cmd")) {
			importImage();
		}

		else if (name.equals("BrowseShapefileCmd")) {
			try {
				if (tools.suite.addShapeFile()) {
					if ( !tools.shapeTB.isSelected() ) {
						tools.shapeTB.doClick();
					}
					else {
						tools.shapes.setVisible(true);
					}
				}
			} catch (IOException e) {
				org.geomapapp.io.ShowStackTrace.showTrace(e);
			}
		}

		else if (name.equals("URLShapefileCmd")) {
			String url = tools.suite.getURLString();
			if( url==null )return;
			try {
				tools.suite.addShapeFile( url );
			} catch (IOException e) {
				org.geomapapp.io.ShowStackTrace.showTrace(e);
			}
			if (!tools.shapeTB.isSelected()) {
				tools.shapeTB.doClick();
			}
			else {
				tools.shapes.setVisible(true);
			}
		}

		else if (name.equals("FormatRequirementsShapefileCmd")) {
			BrowseURL.browseURL(
					PathUtil.getPath("HTML/IMPORTING_DATA_HELP", 
							MapApp.BASE_URL+"/gma_html/Importing_Data.html"));
		}

		else if (name.equals("BrowseGridCmd")) {
			if (!tools.shapeTB.isSelected()) {
				tools.shapeTB.doClick();
			} else {
				tools.shapes.setVisible(true);
			}
			tools.suite.importGrid();
		}
		else if (name.equals("FormatRequirementsGridCmd")) {
			BrowseURL.browseURL(
					PathUtil.getPath("HTML/IMPORTING_DATA_HELP", 
							MapApp.BASE_URL+"/gma_html/Importing_Data.html"));
		}
		else if (name.equals("FormatRequirementsSessionCmd")) {
			BrowseURL.browseURL(
					PathUtil.getPath("HTML/IMPORTING_SESSIONS_HELP", 
							MapApp.BASE_URL+"/gma_html/help/Sessions/index.html"));
		}
		else if (name.equals("ClipboardTablesCmd")) {
			importDataTableNew( DBInputDialog.IMPORT_CLIPBOARD, null );
		}
		else if (name.equals("TabTablesCmd")) {
			importDataTableNew( DBInputDialog.IMPORT_TAB_TEXT_FILE, null );
		}
		else if (name.equals("CommaTablesCmd")) {
			importDataTableNew( DBInputDialog.IMPORT_COMMA_TEXT_FILE, null );
		}
		else if (name.equals("PipeTablesCmd")) {
			importDataTableNew( DBInputDialog.IMPORT_PIPE_TEXT_FILE, null );
		}
		else if (name.equals("ExcelTablesCmd")) {
			importDataTableNew( DBInputDialog.IMPORT_EXCEL_FILE, null );
		}
		else if (name.equals("ASCIIURLTablesCmd")) {
			importDataTableNew( DBInputDialog.IMPORT_ASCII_URL, null );
		}
		else if (name.equals("ExcelURLTablesCmd")) {
			importDataTableNew( DBInputDialog.IMPORT_EXCEL_URL, null );
		}
		else if (name.equals("FormatRequirementsTablesCmd")) {
			BrowseURL.browseURL(
					PathUtil.getPath("HTML/IMPORTING_DATA_HELP", 
							MapApp.BASE_URL+"/gma_html/Importing_Data.html"));
		}
		else if (name.equals("PreferencesCmd")) {
			this.showOps();
		}
		else if (name.equals("OK")) {
			this.acceptOps();
		}
		else if (name.equals("Preview")) {
			this.previewOps();
		}
		else if (name.equals("Cancel")) {
			this.cancelOps();
		}
		else if (name.equals("Defaults")) {
			this.defaultOps();
		}
		else if (name.equals("Reset")) {
			this.resetOps();
		}
		// Try to clear menu cache check for errors
		else if (name.equals("Clear Menus Cache")) {
			try {
				clearCache(menusCacheDir2);
				System.out.println("Menus cache is cleared.");
			} catch (IOException e) {
				if(!menusCacheDir.exists()){
					System.out.println("Menus cache is already clear");
				} else {
					e.printStackTrace();
				}
			}
		}
		// Try to clear menu cache check for errors
		else if (name.equals("Clear Portals Cache")) {
			try {
				clearCache(portalCacheDir);
				System.out.println("Portals cache is cleared.");
			} catch (IOException e) {
				if(!menusCacheDir.exists()){
					System.out.println("Portals cache is already clear");
				} else {
					e.printStackTrace();
				}
			}
		}
		else if (name.equals("SaveMapWindowCmd")) {
			tools.save();
		}

		else if (name.equals("PrintMapWindowCmd")) {
			PrinterJob job = PrinterJob.getPrinterJob();
			PageFormat defaultFmt = job.defaultPage();
			PageFormat fmt = job.pageDialog(defaultFmt);
			if (fmt == defaultFmt) return;
			job.setPrintable(map, fmt);
			try {
				if( job.printDialog() ) {
					job.print();
				}
			} catch (PrinterException e) {
				e.printStackTrace();
			}
			return;
		}

		else if (name.equals("ExitCmd")) {
			sendLogMessage("Exiting_GMA");
			System.exit(0);
			return;
		}

		else if (name.equals("open_search_tree")) {
			switchingProjection = false;
			JMenuItem source = (JMenuItem) evt.getSource();
			XML_Menu sourceMenu = XML_Menu.getXML_Menu(source);
			SearchTree st = new SearchTree(sourceMenu);
			st.setMapApp(this);
		}

		else if (tools.gridDialog != null &&
					evt.getSource() instanceof JMenuItem &&
					(JMenuItem) evt.getSource() != null &&
					XML_Menu.getXML_Menu((JMenuItem) evt.getSource()) != null &&
					tools.gridDialog.isGridCommand(name, XML_Menu.getXML_Menu((JMenuItem) evt.getSource()).name)) {
		
			final JMenuItem mi = (JMenuItem) evt.getSource();
			final XML_Menu menu = XML_Menu.getXML_Menu(mi);
			
			//if name is shape_cmd, but the grid is one of the global grids in GridDialog
			//e.g. one of the Muller grids, then need to update the command name
			if (name.equals("shape_cmd")) {
				name = tools.gridDialog.getCmd(menu.name);
			}
			tools.gridDialog.gridCommand(name, menu);
			// Sort the layers in the Layer Manager
			if (menu.index != null) layerManager.sortLayers();
			
			//load any saved grid parameters from the session XML file
			for (Overlay overlay : map.getOverlays()) {
				if (overlay instanceof Grid2DOverlay) {
					Grid2DOverlay grid = (Grid2DOverlay) overlay;
					if (grid.getName().equals(GridDialog.GRID_CMDS.get(name))) {
						grid.loadSessionParameters(menu);
					}
				}
			}
		}

		else if (name.equals("shape_cmd")) {
			final JMenuItem mi = (JMenuItem) evt.getSource();
			final XML_Menu menu = XML_Menu.getXML_Menu(mi);

			addProcessingTask(mi.getText(), new Runnable() {
				public void run() {
					addShapeFile(mi.getName(), menu);
					if ( !tools.shapeTB.isSelected() ) {
						tools.shapeTB.doClick();
					}
					else {
						tools.shapes.setVisible(true);
					}
					// Sort the layers in the Layer Manager
					if (menu.index != null) layerManager.sortLayers();
				}
			});
		}

		else if (name.equals("table_cmd")) {
			if(custom.loadDB()) {
				//mi: The menu item for the chosen table
				final JMenuItem mi = (JMenuItem) evt.getSource();
				//menu: all the info of the tags in the selected table
				final XML_Menu menu = XML_Menu.getXML_Menu(mi);
				final String tableLayerName = "Data Table: " + mi.getText();

				disableCurrentDB();
				currentDB = custom;
				currentDB.setEnabled(true);
				// Make sure task is added before displaying datatable
				if(true){
				addProcessingTask(tableLayerName, new Runnable() {
					public void run() {
						// Loads
						String url = mi.getName();
						//check to see if the URL is being redirected
						//eg to https version of the page
						url = URLFactory.checkForRedirect(url);
						if (!URLFactory.checkWorkingURL(url)) {
							JOptionPane.showMessageDialog(MapApp.anchor, "Error loading layer:\n"+tableLayerName, "Error", JOptionPane.ERROR_MESSAGE);
							layerManager.missingLayer(menu.index);
							return;
						}
						int i = url.lastIndexOf(".");
						String uidPath = url.substring(0, i) + ".uid";
						//System.out.println(uidPath);
						UIDTracker.sendTrackStat(uidPath);

						custom.loadURL( tableLayerName,
								url,
								Integer.parseInt(menu.type),
								menu.infoURLString, menu);
						
						//get the imported dataset
						List<UnknownDataSet> contenders = custom.dataSets.stream().filter(ds -> ds.toString().equals(tableLayerName)).collect(Collectors.toList());
						if (contenders.size() == 0) return;
						UnknownDataSet dataset = contenders.get(0);
						
						//restore any symbol configurations
						if (menu.symbol_allcolor != null) {
							Color color = new Color(Integer.parseInt(menu.symbol_allcolor));
							dataset.setColor(color);
						}
						if (menu.symbol_shape != null) dataset.setSymbolShape(menu.symbol_shape);
						if (menu.symbol_size != null) dataset.symbolSize = Integer.parseInt(menu.symbol_size);
						
						//set up symbol scale tool if included in xml
						if (menu.sst != null && menu.sst.equals("true") 
								&& menu.sst_col_ind != null && menu.sst_num_col_ind != null) {
							
							//create a new scaling tool using the map, but no data
							SymbolScaleTool sst = new SymbolScaleTool(new float[1], map);
							sst.addPropertyChangeListener(dataset);

							//set the range from the values in the xml file
							try{
								float[] range = {Float.parseFloat(menu.sst_range_start), Float.parseFloat(menu.sst_range_end)};
								sst.setRange(range);
							} catch (Exception ex) {
								//no useable range values, so leave as default
							}							

							//has the range been flipped?
							if (menu.sst_flip != null && menu.sst_flip.equals("true")) {
								sst.flip();
							}
							
							//which column are we scaling by?
							int scaleColumnIndex = Integer.parseInt(menu.sst_col_ind);
							
							//look up the scaling column name and use to set titles
							String colName = dataset.header.get(scaleColumnIndex);
							sst.setName(colName + " - " + dataset.desc.name);
							
							//attach the scaling tool to the dataset
							dataset.sst = sst;
							dataset.setScaleColumnIndex(scaleColumnIndex);
							dataset.setScaleNumericalColumnIndex(Integer.parseInt(menu.sst_num_col_ind));
							//update the symbol sizes based on the column index set above
							dataset.updateSymbolScale();
													
							//repaint map and display scaling tool dialog
							Container c = map.getParent();
							while (!(c instanceof Frame)) c=c.getParent();
							dataset.sst.showDialog((JFrame)null);
							//need to give main window the focus again
							((JFrame)c).toFront();
							((JFrame)c).requestFocus();
						}
						
						//set up color scale tool if included in xml
						if (menu.cst != null && menu.cst.equals("true") 
								&& menu.cst_col_ind != null && menu.cst_num_col_ind != null) {
							
							//create a new color scaling tool with no data
							ColorScaleTool cst = new ColorScaleTool(new float[1]);
							cst.addPropertyChangeListener(dataset);


							//which column are we scaling by?
							int colorColumnIndex = Integer.parseInt(menu.cst_col_ind);
							
							//look up the scaling column name and use to set titles
							String colName = dataset.header.get(colorColumnIndex);
							cst.setName(colName + " - " + dataset.desc.name);
	
							//attach the scaling tool to the dataset
							dataset.cst = cst;
							
							//set up the color palette
							float[] r = GeneralUtils.string2FloatArray(menu.cst_pal_r);
							float[] g = GeneralUtils.string2FloatArray(menu.cst_pal_g);
							float[] b = GeneralUtils.string2FloatArray(menu.cst_pal_b);
							float[] ht = GeneralUtils.string2FloatArray(menu.cst_pal_ht);
							Palette pal = new Palette(r, g, b, ht);
							//determine if color histogram is discrete or continuous
							String interval = menu.cst_discrete;
							pal.setDiscrete(Float.parseFloat(interval));
							if (interval.equals("-1.0")){
								//continuous
								cst.setDiscrete(false);
							} else {
								//discrete
								cst.setColorInterval(interval);
								cst.setDiscrete(true);
							}
							cst.setPalette(pal);
							
							//set diamond tabs on histogram
							int[] tabs = GeneralUtils.string2IntArray(menu.cst_tabs);
							cst.getScaler().setTabs(tabs);
														
							dataset.setColorColumnIndex(colorColumnIndex);
							dataset.setColorNumericalColumnIndex(Integer.parseInt(menu.cst_num_col_ind));
							//update the symbol sizes based on the column index set above
							dataset.updateColorScale();
							
							//repaint map and display color scaling tool dialog
							Container c = map.getParent();
							while (!(c instanceof Frame)) c=c.getParent();
							dataset.cst.showDialog((JFrame)null);
							//need to give main window the focus again
							((JFrame)c).toFront();
							((JFrame)c).requestFocus();
							
							//set the range from the values in the xml file (needs to be done after the color tool has been displayed)
							try{
								float[] range = {Float.parseFloat(menu.cst_range_start), Float.parseFloat(menu.cst_range_end)};
								cst.getScaler().setRange(range);
							} catch (Exception ex) {
								//no useable range values, so leave as default
							}
						}
						// sort layers in Layer Manager
						if (menu.index != null) layerManager.sortLayers();
					
					}
					
				});
				}
				addCurrentDBToDisplay();
			}
			//return;
		}

		else if (name.equals("wfs_cmd")) {
			JMenuItem source = (JMenuItem) evt.getSource();
			XML_Menu wfs_menu = XML_Menu.getXML_Menu(source);
			getWFSLayer(wfs_menu);
			// Sort the layers in the Layer Manager
			if (wfs_menu.index != null) layerManager.sortLayers();
		}

		else if (name.equals("tile_512_cmd")) {
			JMenuItem title_source = (JMenuItem) evt.getSource();
			XML_Menu tile_menu = XML_Menu.getXML_Menu(title_source);
			try {
				load512TileSet(tile_menu);
				// Sort the layers in the Layer Manager
				if (tile_menu.index != null) layerManager.sortLayers();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		else if (name.equals("add_image_cmd")) {
			XML_Menu image_menu = XML_Menu.getXML_Menu((JMenuItem) evt.getSource());
			addExternalImage(image_menu);
			// Sort the layers in the Layer Manager
			if (image_menu.index != null) layerManager.sortLayers();
		}

		else if (name.equals("wms_cmd")) {
			JMenuItem source = (JMenuItem) evt.getSource();
			XML_Menu wms_XML_Menu = XML_Menu.getXML_Menu(source);
			try {
				getWMSLayer(wms_XML_Menu);
				// Sort the layers in the Layer Manager
				if (wms_XML_Menu.index != null) layerManager.sortLayers();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		else if (name.equals("wms_usgs_quads_cmd")) {
			if ( map.getZoom() < 16 ) {
				JOptionPane.showMessageDialog(frame, "Must zoom in to at least 16 to view USGS Quad Maps due to server issues");
			}
			else {
				JMenuItem source = (JMenuItem) evt.getSource();
				XML_Menu wms_XML_Menu = XML_Menu.getXML_Menu(source);
				String url = "http://terraservice.net/ogcmap.ashx?VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG:4326&LAYERS=DRG&STYLES=blank&TRANSPARENT=FALSE&FORMAT=image/jpeg&";
				if ("EPSG:4326".equals(wms_XML_Menu.srs)) {
					FocusOverlay overlay = new WMS_ESPG_4326_Overlay(url,
																	map,
																	wms_XML_Menu.name);
					addFocusOverlay(overlay,
							wms_XML_Menu.name,
							wms_XML_Menu.infoURLString);
				}

				if (map.getZoom() <= 1.5) {
					map.repaint();
				} else {
					mapFocus();
				}
				// Sort the layers in the Layer Manager
				if (wms_XML_Menu.index != null) layerManager.sortLayers();
			}
		}

		else if (name.equals("open_browser_cmd")) {
			BrowseURL.browseURL(((JMenuItem)evt.getSource()).getName());
		}
		//Portal Menu Commands
		else if (portal_commands.contains(name)) {
			// First check for caches.
			try {
				getPortalCacheSelect();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// Now proceed to portals.
			for( int i=0 ; i<db.length ; i++) {
				if( db[i].getCommand().equals(name) ) {
					if ( evt.getSource() instanceof JCheckBoxMenuItem &&
							((JCheckBoxMenuItem)evt.getSource()).isSelected() ) {
						final Database database = db[i];
						final JMenuItem mi = (JMenuItem) evt.getSource();
						System.gc();
						database.setup();
						Runnable loadDB = new Runnable() {
							public void run() {
								if(database.loadDB()) {
									disableCurrentDB();

									currentDB = database;
									currentDB.setEnabled(true);

									addCurrentDBToDisplay();

									while( map.hasOverlay( database ) ) {
										map.removeOverlay(database);
									}
									XML_Menu m = XML_Menu.getXML_Menu(mi);

									if ( m != null ) {
										map.addOverlay( database.getDBName(), m.infoURLString, database, m );
									}
									else {
										map.addOverlay( database.getDBName(), database );
									}
									sendLogMessage("Opening Portal$name="+database.getDBName());
								}
								else if(!database.isLoadCancelled()){
									JOptionPane.showMessageDialog(vPane, "Error loading " + currentDB.getDBName(), "", JOptionPane.ERROR_MESSAGE);
								}
							}
						};
						if(database.isLoadCancelled()) {
							mi.setSelected(false);
						}
						else {
							addProcessingTask(mi.getText(), loadDB);
						}
					}
					else {
						currentDB = db[i];
						if(currentDB instanceof XMCS) {
							((XMCS)currentDB).linesLoaded = false;
						}
						closeCurrentDB();
						Vector<Overlay> overlays = map.overlays;
						for ( int j = 0; j < map.overlays.size(); j++ ) {
							if ( overlays.get(j) instanceof haxby.db.Database ) {
								setCurrentDB(((haxby.db.Database)overlays.get(j)));
								enableCurrentDB();
								addDBToDisplay(((haxby.db.Database)overlays.get(j)));
								break;
							}
						}
						System.gc();
						map.repaint();
					}
					return;
				}
			}
			if (name.equals("seafloor_driling_cmd")) {
				if ( evt.getSource() instanceof JCheckBoxMenuItem ) {
					if( ((JCheckBoxMenuItem)evt.getSource()).isSelected() ) {
						showDSDP();
					} else {
						closeDSDP();
					}
				}
			}
		}

		else if (name.equals("color_scale_cmd")) {
			if(colorScale==null ) {
				colorScale = new MapColorScale(map);
			}

			if ( evt.getSource() instanceof JCheckBoxMenuItem ) {
				if( ((JCheckBoxMenuItem)evt.getSource()).isSelected() ) {
					initializeColorScale();
				} else {
					removeColorScale();
				}
				map.repaint();
			}
		}

		else if (name.equals("distance_scale_cmd")) {
			if( mapScale==null ) mapScale=new MapScale(map);
			if ( evt.getSource() instanceof JCheckBoxMenuItem ) {
				if( ((JCheckBoxMenuItem)evt.getSource()).isSelected() ) {
					map.addMapInset( mapScale );
					map.addMouseListener( mapScale );
					map.addMouseMotionListener( mapScale );
				} else {
					map.removeMapInset( mapScale );
					map.removeMouseListener( mapScale );
					map.removeMouseMotionListener( mapScale );
				}
				map.repaint();
			}
		}

		else if (name.equals("map_inset_cmd")) {
			if( li==null ) li=new LocationInset(map);
			if ( evt.getSource() instanceof JCheckBoxMenuItem ) {
				if( ((JCheckBoxMenuItem)evt.getSource()).isSelected() ) {
					map.addMapInset( li );
					map.addMouseListener( li );
					map.addMouseMotionListener( li );
				} else {
					map.removeMapInset( li );
					map.removeMouseListener( li );
					map.removeMouseMotionListener( li );
				}
				map.repaint();
			}
		}

		else if (name.equals("layer_manager_cmd")) {
			setLayerManagerVisible(((JCheckBoxMenuItem)evt.getSource()).isSelected());
		}

		else if (name.equals("bathymetry_credits_cmd")) {
			credit.setEnabled(((JCheckBoxMenuItem)evt.getSource()).isSelected());
			credit.getPanel().updateUI();
		}

		else if (name.equals("add_bookmark_cmd")) {
			locs.addPlace();
		}

		else if (name.equals("map_place_cmd")) {
			JMenuItem source = (JMenuItem) evt.getSource();
			XML_Menu temp = XML_Menu.getXML_Menu(source);
			Point2D.Double p = (Point2D.Double)(map.getProjection().getMapXY(new Point2D.Double( Double.parseDouble(temp.map_place_lon), Double.parseDouble(temp.map_place_lat) )));
			double z = map.getZoom();
			p.x *= z;
			p.y *= z;
			Insets insets = map.getInsets();
			p.x += insets.left;
			p.y += insets.top;
			double factor = Double.parseDouble(temp.map_place_zoom)/z;
			
			// Tacks zoom before, does zoom, tracks zoom after
			map.setZoomHistoryPast(map);
			map.doZoom( p, factor );
			map.setZoomHistoryNext(map);

			if (tools != null) {
				tools.getApp().autoFocus();
			}
		}
		else if (name.equals("show_places_cmd")){
			if(((JCheckBoxMenuItem)evt.getSource()).isSelected()){
					MapPlaces.showLoc.setSelected(true);
					MapApp.showPlaces();
				}else{
					MapPlaces.showLoc.setSelected(false);
					MapApp.showPlaces();
			}
		}
		else if (name.equals("browse_bookmarks_cmd")) {
			locs.browseBookmarks();
		}
		else if (name.equals("import_layer_session_cmd")) {
			LayerManager.importB.doClick();
			if (!LayerManager.doImport) return;
			try {
				reloadSessionMainMenu();
			} catch (ConnectException e) {
				e.printStackTrace();
			}
			// revalidate again for quicker update 
			frame.getJMenuBar().revalidate();

			try {
				JMenuBar reloadedBar = MapApp.getMenuBar();
				int mCount = reloadedBar.getMenuCount();
				final JMenu lastMenu = reloadedBar.getMenu(mCount-1);
	
				if(lastMenu.getText().contentEquals("My Layer Sessions")) {
						// Handle the  opaque setting some OS have different settings.
					if(lastMenu.isOpaque() != true) {
						lastMenu.setOpaque(true);
					}
					lastMenu.setBackground(new Color(255,192,139));
	
					// Load All Layers and Zoom if available
					JMenuItem subLast = null;
					Component[] childFirst = lastMenu.getMenuComponents();
					//find the menu item with the same name as the session we are importing
					for (Component child : childFirst) {
						try {
							if (((JMenuItem) child).getText().equals(sessionImport)) {
								subLast = (JMenuItem) child;
								break;
							}
						} catch (Exception ex) {
							System.out.println(ex.getStackTrace());
						};
					}
					
					//JMenuItem subLast = (JMenuItem) childFirst[0];
					JMenu sessionMenuFirst =	(JMenu) subLast;
					Component[] subChild = sessionMenuFirst.getMenuComponents();
					int sCount = subChild.length;
					// Load all layers then zoom
					final JMenuItem subChildItemLast = (JMenuItem) subChild[sCount-1];
					final JMenuItem subChildItemFirst = (JMenuItem) subChild[0];
	
					final Runnable doLoadAllLayers = new Runnable() {
						 public void run() {
							 if(subChildItemLast.getText().contentEquals("Load All Layers")) {
										subChildItemLast.doClick();
								}
						}
					};
	
					final Runnable doZoomSession = new Runnable() {
						public void run() {
							if(loadSession && subChildItemFirst.getText().contentEquals("Zoom To Saved Session")) {
								subChildItemFirst.doClick();
							}
						}
					};
	
					final Runnable revalidate = new Runnable() {
						public void run() {
							lastMenu.revalidate();
						}
					};
		
					Thread appThread = new Thread() {
						public void run() {
							try {								
								SwingUtilities.invokeAndWait(revalidate);  		// invoke 1
								SwingUtilities.invokeAndWait(doLoadAllLayers);	// invoke 2
								Thread.sleep(1000);								// sleep 1 sec
								SwingUtilities.invokeLater(doZoomSession);		// invoke later when awt appThread has returned
								// need to reload grid after zoom to get the correct color histogram
								if (tools != null && tools.gridDialog != null && tools.gridDialog.isDialogVisible()) tools.gridDialog.startGridLoad();
							}
							catch (Exception e) {
								e.printStackTrace();
							}
						}
					};
	
					// sync start loading.
					synchronized (map.getTreeLock()) {
						appThread.start();
						
					}
				}
			} catch (ClassCastException ex) {
				return;
			}			
		} else if (name.equals("save_layer_session_cmd")) {
			LayerManager.captureB.doClick();
		} else if (name.equals("reload_layer_session_cmd")) {
			try {
				reloadSessionMainMenu();
			} catch (ConnectException e) {
				e.printStackTrace();
				loadSystemMainMenu();
			}
		} else if (name.equals("close_layer_session_cmd")) {
			//Loads Main GMA Menu Only
			loadSystemMainMenu();
		} else if (name.equals("zoom_to_wesn_cmd")) {
			zoomToWESN();
		} else if (name.equals("zoom_to_session_area_cmd")) {
			// Looks for specified 3 tags in xml for zoom level and point
			System.out.println("Zoom to region.");
			JMenuItem menuItem = (JMenuItem) evt.getSource();
			try{
				XML_Menu thisXMLItem = XML_Menu.getXML_Menu(menuItem);
				Double xPoint = Double.parseDouble(thisXMLItem.lonX);
				Double yPoint = Double.parseDouble(thisXMLItem.latY);
				Double zoomLoc = Double.parseDouble(thisXMLItem.zoom);
	
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
				// zoom history set first zoom checks it for the first time only, does zoom, tracks zoom after
				map.setZoomHistoryPast(map);
				map.doZoom( point, zoomFactor );
				map.setZoomHistoryNext(map);
			} catch (NullPointerException np){
				System.out.println("No zoom area specified.");
			}
		} else if (name.equals("load_all_session_layers_cmd")) {
			JMenuItem menuItemLoad = (JMenuItem) evt.getSource();
			XML_Menu thisXMLItemLoad = XML_Menu.getXML_Menu(menuItemLoad);
			layerManager.resetMissingLayers();

			loadSession = true;
			//if this layer isn't applicable to this projection, may need to switch projection
			String proj = thisXMLItemLoad.proj.toLowerCase();
			if (!proj.contains(MapApp.CURRENT_PROJECTION.toLowerCase())) {
				if (proj.contains("m")) {
					int confirm = JOptionPane.showConfirmDialog(MapApp.anchor, 
							"This session will switch to Mercator Projection", 
							"Switch Projection", JOptionPane.OK_CANCEL_OPTION);
					if (confirm == JOptionPane.CANCEL_OPTION) {
						loadSession = false;
						return;
					}
					MInit2();
				} else if (proj.contains("s")) {
					int confirm = JOptionPane.showConfirmDialog(MapApp.anchor, 
							"This session will switch to South Polar Projection", 
							"Switch Projection", JOptionPane.OK_CANCEL_OPTION);
					if (confirm == JOptionPane.CANCEL_OPTION) {
						loadSession = false;
						return;
					}
					SPInit2();	
				} else if (proj.contains("n")) {
					int confirm = JOptionPane.showConfirmDialog(MapApp.anchor, 
							"This session will switch to North Polar Projection", 
							"Switch Projection", JOptionPane.OK_CANCEL_OPTION);
					if (confirm == JOptionPane.CANCEL_OPTION) {
						loadSession = false;
						return;
					}
					NPInit2();	
				}
			}
				
			for(int m=thisXMLItemLoad.parent.child_layers.size()-1; m >= 0; m--) {
				if(thisXMLItemLoad.parent.child_layers.get(m).name.contains("Zoom To Saved Session") ||
					thisXMLItemLoad.parent.child_layers.get(m).name.contains("Load All Layers")	) {
					// Do nothing
				} else {
					XML_Menu sessionLayer = thisXMLItemLoad.parent.child_layers.get(m);
					if (sessionLayer.name.matches(baseFocusName)) {
						// attach index number to basemap
						List<LayerPanel> layerPanels = layerManager.getLayerPanels();
						for (LayerPanel lp : layerPanels) {
							if (lp.layerName.matches(baseFocusName) && sessionLayer.index != null){
								lp.setItem(sessionLayer);
							}
						}
						
						
					}
					XML_Menu.getMenuItem(sessionLayer).doClick();
				}
			}
			//reselect the map insert menu option if needed
			JMenuItem mi = XML_Menu.commandToMenuItemHash.get("map_inset_cmd");
			if ( mi != null ) {
				((JCheckBoxMenuItem)mi).setSelected(true);
			}

			mi = XML_Menu.commandToMenuItemHash.get("bathymetry_credits_cmd");
			if ( mi != null ) {
				((JCheckBoxMenuItem)mi).setSelected(true);
			}

			colorScaleCB = (JCheckBoxMenuItem)
				XML_Menu.commandToMenuItemHash.get("color_scale_cmd");
			locs.showLoc = (JCheckBoxMenuItem) XML_Menu.getMenutItem("show_places_cmd");
			
		} else if (name.equals("switch_north_cmd")) {
			System.out.println("Starting North Projection");
			NPInit2();
		} else if (name.equals("switch_south_cmd")) {
			System.out.println("Starting South Projection");
			SPInit2();
		} else if (name.equals("switch_merc_cmd")) {
			System.out.println("Starting Mercator Projection");
			MInit2();
		}
		else if(name.equals("loadTilesLocalCmd")) {
			previewCruiseFromMenu(true);
		}
		else if(name.equals("loadTilesRemoteCmd")) {
			previewCruiseFromMenu(false);
		}

		// Close DB button action
		if(evt.getSource() == closeDB) {
			if(currentDB instanceof XMCS) {
				((XMCS)currentDB).linesLoaded = false;
			}
			closeCurrentDB();
			return;
		} else if (evt.getSource() == detach_attachB) {
			toggleDisplayAttachment();
		}
		else if (evt.getSource() == serverList) {
			if ( ((String)serverList.getSelectedItem()).equals(DEV_URL) && !BASE_URL.equals(DEV_URL) ) {
				addDevPasswordField();
			}
			else {
				removeDevPasswordField();
			}
		}

		else if (evt.getSource()==tools.digitizeB) {
			if (digitizer != null && tools.digitizeB.isSelected()) {
				addProcessingTask(digitizer.getDBName(), new Runnable() {
					public void run() {
						if(digitizer.loadDB()) {
							disableCurrentDB();

							currentDB = digitizer;
							currentDB.setEnabled(true);

							addCurrentDBToDisplay();
						}
						int sI = tools.gridDialog.gridCB.getSelectedIndex();
					
						//set grid dialog to GMRT Grid if no other grid is already loaded
						if (!tools.gridDialog.isDialogVisible()) {
							Grid2DOverlay GMRTgrid = tools.gridDialog.gridCBElements.get(GridDialog.GRID_SHORT_TO_LONG_NAMES.get(GridDialog.DEM));
							for (int i = 0 ; i < tools.gridDialog.gridCB.getItemCount(); i++) {
								if ((Grid2DOverlay)tools.gridDialog.gridCB.getItemAt(i) == GMRTgrid) {
									tools.gridDialog.gridCB.setSelectedIndex(i);
								}
							}
							layerManager.moveToTop(GMRTgrid);
						
							if (tools.gridDialog.getToggle().isSelected())
								if (sI != 0)
									tools.gridDialog.loadGrid();
								else ;
							else {
								tools.gridDialog.getToggle().doClick();
								digitizer.setLoadedGMRTForDig(true);
							}
						}						
						while( map.hasOverlay( digitizer ) ) {
							map.removeOverlay(digitizer);
						}					
						map.addOverlay("Digitizer", digitizer );
						tools.digitizeB.setSelected(true);
					}
				});
				return;
			}
			else {
				closeDB.doClick();
			}
		}

		//stopWaiting();
		if (db == null) {
			return;
		}
	}

	public void loadDatabase(Database database, String infoURLString) {
		if(database.loadDB()) {
			disableCurrentDB();

			currentDB = database;
			currentDB.setEnabled(true);

			addCurrentDBToDisplay();

			while( map.hasOverlay( database ) ) {
				map.removeOverlay(database);
			}
			map.addOverlay( database.getDBName(), infoURLString, database );
		}
	}

	private void zoomToWESN() {
		final JDialog d = new JDialog(frame, "Zoom To...", true);

		final WESNPanel wesnP = new WESNPanel();
		TitledBorder border = BorderFactory.createTitledBorder("Location (Negatives for Western & Southern Hemisphere)");
		wesnP.setBorder(border);

		JPanel p2 = new JPanel();
		JButton accept = new JButton("Zoom");
		accept.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double wesn[] =  wesnP.getWESN();
				if (wesn == null) {
					Toolkit.getDefaultToolkit().beep();
					return;
				}
				if (wesn[0] > wesn[1] || wesn[2] > wesn[3]) {
					if (wesn[0] > wesn[1]) {
						wesnP.west.setText("! W > E !");
						wesnP.east.setText("! E < W !");
					}
					if (wesn[2] > wesn[3]) {
						wesnP.north.setText("! N < S !");
						wesnP.south.setText("! S > N !");
					}
					Toolkit.getDefaultToolkit().beep();
					return;
				}
				d.setVisible(false);
			}
		});
		p2.add(accept);

		final JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				wesnP.setWESN(null);
				d.setVisible(false);
			}
		});
		p2.add(cancel);

		JPanel c = new JPanel(new BorderLayout());
		c.add(wesnP);
		c.add(p2, BorderLayout.SOUTH);

		d.addWindowListener( new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				wesnP.setWESN(null);
			}
		});
		d.getContentPane().add(c);
		d.setLocationRelativeTo(frame);
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		d.pack();
		d.setVisible(true);

		double[] wesn = wesnP.getWESN();
		if (wesn == null) {
			return;
		}
		// zoom history set first zoom checks it for the first time only, does zoom, tracks zoom after
		map.setZoomHistoryPast(map);
		map.zoomToWESN(wesn);
		map.setZoomHistoryNext(map);
	}
	//	***** GMA 1.6.4: Set grid in MapTools so that it can be saved
	public void setToolsGrid(Grid2DOverlay inputGrid) {
		tools.gridToSave = inputGrid;
	}

	private void showOps() {
		map.removeMouseListener(zoomer);

		// GMA 1.6.2: Add title "Preferences" to Preferences window
		option = new JFrame("Preferences");
		option.setLocation(frame.getLocation());
		// Do nothing on close. Must select cancel
		option.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		JTabbedPane prefer = new JTabbedPane();
		JPanel mapbord = new JPanel(new BorderLayout());

		// Font
		JPanel fonts = new JPanel(new FlowLayout());
		tmpFont = map.getMapBorder().getFont();

		String[] fontList = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		font = new JComboBox<>(fontList);
		font.setName("Font");
		font.setSelectedItem(map.getMapBorder().getFont().getName());
		fonts.add(font);

		JLabel fontSizeL = new JLabel("Font size:");
		fonts.add(fontSizeL);

		fontSize = new JTextField();
		fontSize.setText("" + map.getMapBorder().getFont().getSize());
		fonts.add(fontSize);

		//Scrollbar
		scroll = (map.scrollPane.getHorizontalScrollBarPolicy() == JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		doScroll = new JCheckBox("Enable Scrollbars", scroll);
		fonts.add(doScroll);

		mapbord.add(fonts, BorderLayout.NORTH);

		//MapBorder sides
		JPanel sides = new JPanel(new FlowLayout());
		JLabel explSides = new JLabel("Map Annotations: ");
		sides.add(explSides);

		for (int i = 0; i< tmpSides.length; i++)
			tmpSides[i] = map.getMapBorder().isSideSelected(i);

		side = new JCheckBox[4];
		side[0] = new JCheckBox("Top", tmpSides[0]);
		side[1] = new JCheckBox("Bottom", tmpSides[1]);
		side[2] = new JCheckBox("Left", tmpSides[2]);
		side[3] = new JCheckBox("Right", tmpSides[3]);

		for (int i = 0; i< side.length; i++)
			sides.add(side[i]);

		mapbord.add(sides, BorderLayout.CENTER);
		
		//Buttons
		JPanel buttons = new JPanel(new FlowLayout());

		JButton ok = new JButton("OK");
		ok.setToolTipText("Accept current settings");
		ok.addActionListener(this);
		buttons.add(ok);

		JButton prev = new JButton("Preview");
		prev.setToolTipText("Preview current settings");
		prev.addActionListener(this);
		buttons.add(prev);

		JButton reset = new JButton("Reset");
		reset.setToolTipText("Reset settings");
		reset.addActionListener(this);
		buttons.add(reset);

		JButton defaults = new JButton("Defaults");
		defaults.setToolTipText("Return to default settings");
		defaults.addActionListener(this);
		buttons.add(defaults);

		JButton cancel = new JButton("Cancel");
		cancel.setToolTipText("Cancel");
		cancel.addActionListener(this);
		buttons.add(cancel);

		option.getContentPane().add(buttons, BorderLayout.SOUTH);

		// Tab Add Map Boarder
		prefer.add("Map Border", mapbord);
		
		//Menu font
		JPanel menuFontTab = new JPanel(new BorderLayout());
		JPanel fontSelection = new JPanel(new FlowLayout());
		
		curMenuFont = UIManager.getFont("Menu.font");
		menuFont = new JComboBox<>(fontList);
		menuFont.setName("Menu Font");
		menuFont.setSelectedItem(curMenuFont.getName());
		fontSelection.add(menuFont);
		
		JLabel lblMenuFontSize = new JLabel("Size: ");
		menuFontSize = new JTextField();
		menuFontSize.setText(String.valueOf(curMenuFont.getSize()));
		fontSelection.add(lblMenuFontSize);
		fontSelection.add(menuFontSize);
		menuFontTab.add(fontSelection);
		prefer.add("Menu Font", menuFontTab);
		

		// Tab Coordinates
		JPanel coordsTab = new JPanel(new BorderLayout());
		
		// Set whether you want longitude ranges to go from -180 to +180, or 0 to 360.
		// This is when dragging/placing datapoints on the map
		JPanel ranges = new JPanel(new FlowLayout());
		JLabel rangesL = new JLabel("Longitude Range: ");
		ranges.add(rangesL);
		range180Btn = new JRadioButton("-180 to +180");
		range360Btn = new JRadioButton("0 to 360");
		ButtonGroup rangeGroup = new ButtonGroup();
	
		int lonRange = map.getProjection().getLongitudeRange();
		if (lonRange == Projection.RANGE_180W_to_180E) { range180Btn.setSelected(true); }
		if (lonRange == Projection.RANGE_0_to_360) { range360Btn.setSelected(true); }
		rangeGroup.add(range180Btn);
		rangeGroup.add(range360Btn);
		ranges.add(range180Btn);
		ranges.add(range360Btn);
		coordsTab.add(ranges, BorderLayout.NORTH);
		
		prefer.add("Coordinates", coordsTab);
		
		// Tab Grid Import Options
		JPanel gridsP = new JPanel(new BorderLayout());
		gridsCB = new JCheckBox("Generate log file when importing grids", logGridImports);
		gridsCB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean newEnabled = ((JCheckBox)e.getSource()).isSelected();
				gridsDirTF.setEnabled(newEnabled);
				gridsDirBtn.setEnabled(newEnabled);
			}
		});
		JPanel gridsDirP = new JPanel();
		gridsDirTF = new JTextField(gridImportsLogDir.toString());
		gridsDirTF.setEnabled(logGridImports);
		gridsDirBtn = new JButton("Change Logs Directory");
		gridsDirBtn.setEnabled(logGridImports);
		gridsDirBtn.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				gridsChooser = new JFileChooser();
				gridsChooser.setCurrentDirectory(gridImportsLogDir);
				gridsChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				gridsChooser.setAcceptAllFileFilterUsed(false);
				if (gridsChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					gridsDirTF.setText(gridsChooser.getSelectedFile().toString());
				}
			}
		});
	
		gridsP.add(gridsCB, BorderLayout.NORTH);
		gridsDirP.add(gridsDirBtn);
		gridsDirP.add(gridsDirTF);
		gridsP.add(gridsDirP, BorderLayout.CENTER);
		prefer.add("Grid Imports", gridsP);
				
		// Tab Cache Options
		JPanel cacheOptions = new JPanel(new BorderLayout());
		JPanel panel1 = new JPanel(new FlowLayout());
		clearMCache = new JButton("Clear Menus Cache");
		clearMCache.addActionListener(this);
		panel1.add(clearMCache);

		clearPCache = new JButton("Clear Portals Cache");
		clearPCache.addActionListener(this);
		panel1.add(clearPCache);
		
		// Portal Cache Options
		JPanel portalOptions = new JPanel();
		portalOptions.setLayout(new javax.swing.BoxLayout(portalOptions, javax.swing.BoxLayout.Y_AXIS));

		portalOptions.add(mbPortalCache);
		//portalOptions.add(pPortalCache);
		JPanel portalLabels = new JPanel();
		portalLabels.setLayout(new javax.swing.BoxLayout(portalLabels, javax.swing.BoxLayout.Y_AXIS));
		JLabel portalLabel1 = new JLabel("Enable Portal Cache for: ");
		portalLabels.add(portalLabel1);

		cacheOptions.add(portalLabels, BorderLayout.WEST);
		cacheOptions.add(portalOptions, BorderLayout.CENTER);
		cacheOptions.add(panel1, BorderLayout.NORTH);
		prefer.addTab( "Cache Options", cacheOptions);

		option.getContentPane().add(prefer);
		option.pack();
		option.setVisible(true);

// Tab Add Menu Options
//JPanel menuOptions = new JPanel(new BorderLayout());
//prefer.add("Menu Options", menuOptions);

//		***** GMA 1.6.2: Add "Server Options" tab to the "Preferences" window
		// ** GMA 3.7.5: Remove "Server Options" from the "Preferences" window
		/*
		Border emptyBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		Border lineBorder = BorderFactory.createLineBorder( Color.black );
		Border compBorder = BorderFactory.createCompoundBorder( lineBorder, emptyBorder );
		JPanel serverOptions = new JPanel( new BorderLayout( 5, 5 ) );
		serverPanel = new JPanel( new BorderLayout( 5, 5) );
		serverPanel.setBorder(compBorder);
		JLabel serverLabel = new JLabel("Select Server: ");
		if ( serverList != null ) {
			serverList.setSelectedIndex(selectedServer);
			serverList.addActionListener(this);
			serverPanel.add( serverLabel, BorderLayout.WEST );
			serverPanel.add( serverList, BorderLayout.CENTER );
		}
		serverOptions.add( serverPanel, BorderLayout.NORTH );
		
		inputDevPasswordPanel = new JPanel();
		inputDevPasswordLabel = new JLabel("Password:");
		inputDevPasswordText = new JTextField(10);
		inputDevPasswordPanel.add(inputDevPasswordLabel);
		inputDevPasswordPanel.add(inputDevPasswordText);
		inputDevPasswordPanel.setVisible(false);
		serverPanel.add(inputDevPasswordPanel, BorderLayout.SOUTH );
		*/

		// Tab Server Options
		//prefer.addTab( "Server Options", serverOptions);
//		***** GMA 1.6.2

		//ShoreLine
		if (whichMap == MapApp.SOUTH_POLAR_MAP) {
			opShorePanel = new ShoreOptionPanel(shoreLine);
			prefer.add("Shoreline", opShorePanel);
		}


		// Development Options
		JPanel devOptions = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		JButton loadLocalTiles = new JButton("Load cruise tiles from your computer"),
				loadRemoteTiles = new JButton("Load cruise tiles from the server");
		loadLocalTiles.setActionCommand("loadTilesLocalCmd");
		loadRemoteTiles.setActionCommand("loadTilesRemoteCmd");
		if(MapApp.AT_SEA) loadRemoteTiles.setEnabled(false);
		loadLocalTiles.addActionListener(this);
		loadRemoteTiles.addActionListener(this);
		c.gridwidth = 2;
		c.gridy = 0;
		c.gridx = 0;
		c.weightx = 0.5;
		devOptions.add(loadRemoteTiles, c);
		c.gridx = 2;
		devOptions.add(loadLocalTiles, c);
		c.gridwidth = 4;
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0.0;
		showTileNames = new JCheckBox("Show Tile Names", MMapServer.DRAW_TILE_LABELS);
		showTileNames.setHorizontalAlignment(SwingConstants.CENTER);
		devOptions.add(showTileNames, c);
		

		// Tab Server Options
		prefer.addTab( "GMRT Tiles", devOptions);

		option.getContentPane().add(prefer);
		option.setMinimumSize(new Dimension(prefer.getPreferredSize().width + 100, option.getPreferredSize().height));
		option.pack();
		option.setVisible(true);
	}

	private void defaultOps() {
		map.getMapBorder().setFont(defaultFont);

		for (int i = 0; i < dfltSides.length; i++) {
			map.getMapBorder().setSide(i, dfltSides[i]);
			side[i].setSelected(dfltSides[i]);
		}

		if ( defaultFont == null ) {
			defaultFont = new Font("Arial",Font.PLAIN,10);
		}
		defaultMenuFont = XML_Menu.getMenuFont();
		
		font.setSelectedItem(defaultFont.getName());
		fontSize.setText("" + defaultFont.getSize());
		
		menuFont.setSelectedItem(defaultMenuFont.getName());
		menuFontSize.setText(String.valueOf(defaultMenuFont.getSize()));

		map.scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		map.scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		doScroll.setSelected(true);

		if (whichMap == MapApp.SOUTH_POLAR_MAP)
			opShorePanel.defaults();

		if(null != serverList) serverList.setSelectedItem( DEFAULT_URL );

		showTileNames.setSelected(false);
		MMapServer.DRAW_TILE_LABELS = false;
		PoleMapServer.DRAW_TILE_LABELS = false;
		
		int lonRange = DEFAULT_LONGITUDE_RANGE;
		if (lonRange == Projection.RANGE_180W_to_180E) { range180Btn.setSelected(true); }
		if (lonRange == Projection.RANGE_0_to_360) { range360Btn.setSelected(true); }
		
		gridsCB.setSelected(false);
		logGridImports = false;
		gridsDirTF.setText(DEFAULT_GRID_IMPORTS_LOGS_DIR.toString());
		gridsDirTF.setEnabled(logGridImports);
		gridsDirBtn.setEnabled(logGridImports);
	}

	private void resetOps() {
		for (int i = 0; i<side.length; i++)
				side[i].setSelected(tmpSides[i]);

		font.setSelectedItem(tmpFont.getName());
		fontSize.setText("" + tmpFont.getSize());
		
		menuFont.setSelectedItem(curMenuFont.getName());
		menuFontSize.setText(String.valueOf(curMenuFont.getSize()));
		setMenuFont(curMenuFont);

		map.getMapBorder().setFont(tmpFont);

		for (int i = 0; i < tmpSides.length; i++)
			map.getMapBorder().setSide(i, side[i].isSelected());

		if (scroll) {
			map.scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			map.scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		} else {
			map.scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			map.scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		}

		doScroll.setSelected(scroll);

		if (whichMap == MapApp.SOUTH_POLAR_MAP)
			opShorePanel.cancel();

		int lonRange = map.getProjection().getLongitudeRange();
		if (lonRange == Projection.RANGE_180W_to_180E) { range180Btn.setSelected(true); }
		if (lonRange == Projection.RANGE_0_to_360) { range360Btn.setSelected(true); }
		
		if(null != serverList) serverList.setSelectedIndex(selectedServer);
		showTileNames.setSelected(MMapServer.DRAW_TILE_LABELS);
		
		gridsCB.setSelected(logGridImports);
		gridsDirTF.setText(gridImportsLogDir.toString());
		gridsDirTF.setEnabled(logGridImports);
		gridsDirBtn.setEnabled(logGridImports);
	}

	private void cancelOps() {
		this.resetOps();
		option.setVisible(false);
		option.dispose();
		map.addMouseListener(zoomer);
	}

	private void previewOps() {
		Font theFont = new Font( (String) font.getSelectedItem(), Font.PLAIN, Integer.parseInt(fontSize.getText()));
		map.getMapBorder().setFont(theFont);
		
		Font theMenuFont = new Font((String)menuFont.getSelectedItem(), Font.PLAIN, Integer.parseInt(menuFontSize.getText()));
		setMenuFont(theMenuFont);

		for (int i = 0; i < tmpSides.length; i++)
			map.getMapBorder().setSide(i,side[i].isSelected());

		if (doScroll.isSelected()) {
			map.scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			map.scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		} else {
			map.scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			map.scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		}

		if (whichMap == MapApp.SOUTH_POLAR_MAP) {
			opShorePanel.preview();
		}
		PoleMapServer.DRAW_TILE_LABELS = MMapServer.DRAW_TILE_LABELS = showTileNames.isSelected();
		focus.image = null;
		
		if (range180Btn.isSelected()) {
			map.getProjection().setLongitudeRange(Projection.RANGE_180W_to_180E);
			map.repaint();
		}
		if (range360Btn.isSelected()) {
			map.getProjection().setLongitudeRange(Projection.RANGE_0_to_360);
			map.repaint();
		}
		
		mapFocus();
	}

	private void acceptOps() {
		this.previewOps();
		curMenuFont = new Font((String)menuFont.getSelectedItem(), Font.PLAIN, Integer.parseInt(menuFontSize.getText()));
		String fontString = curMenuFont.getName() + menuFontDelimiter + curMenuFont.getSize();
		if(null != menuFontFile) {
			try {
				if(!menuFontFile.exists()) {
					menuFontFile.createNewFile();
				}
				PrintStream ps = new PrintStream(menuFontFile);
				ps.println(fontString);
				ps.close();
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
//		***** GMA 1.6.2: Load server and proxy options and make currently selected options the default options so they are loaded when GeoMapApp restarts
		if ( serverList != null && selectedServer != serverList.getSelectedIndex()) {
			if ( ((String)serverList.getSelectedItem()).equals(DEV_URL) && inputDevPasswordText != null ) {
				// convert the input password to an sha256 hash and compare with the value on the server
				String inputPasswordHash = GeneralUtils.stringToSHA256(inputDevPasswordText.getText());
				if ( inputPasswordHash.equals(getDevPasswordHash()) ) {
					BASE_URL = (String)serverList.getSelectedItem();
					TEMP_BASE_URL = (String)serverList.getSelectedItem();
					JOptionPane.showMessageDialog(option, "Password accepted.  Please restart GeoMapApp.", "Success", JOptionPane.OK_OPTION);
				}
				else {
					BASE_URL = PathUtil.getPath("ROOT_PATH");
					TEMP_BASE_URL = PathUtil.getPath("ROOT_PATH");
					JOptionPane.showMessageDialog(option, "Incorrect password.", "Incorrect Password", JOptionPane.ERROR_MESSAGE);
					return;
				}
			} 
			else {
				BASE_URL = (String)serverList.getSelectedItem();
				TEMP_BASE_URL = (String)serverList.getSelectedItem();
				JOptionPane.showMessageDialog(option, "Please restart GeoMapApp to switch out of DEV mode.", "Success", JOptionPane.OK_OPTION);
			}
			selectedServer = serverList.getSelectedIndex();
		}

		try {
			if ( serverDir.exists() && serverFile.exists() ) {
				serverFile.delete();
				serverFile.createNewFile();
			}
			else {
				if ( !serverDir.exists() ) {
					serverDir.mkdir();
				}
				serverFile.createNewFile();
			}
			BufferedWriter out = new BufferedWriter( new FileWriter(serverFile, true) );
			out.write( BASE_URL + "\r\n" );
			out.flush();
			out.close();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(MapApp.anchor, "Error writing to default_server.dat", "File I/O Error", JOptionPane.ERROR_MESSAGE);
		}

		try {
			// action on cache portal dat
			if(getMbPortalCache() == true) {
				if(!portalSelectFile.exists()) {
					menusCacheDir.mkdirs();
					portalSelectFile.createNewFile();
				}
			} else {
				portalSelectFile.delete();
			}
		} catch (IOException e) {
			e.printStackTrace();
			portalSelectFile.delete();
			System.out.println("cache not allowed on this system");
		}

		PoleMapServer.DRAW_TILE_LABELS = MMapServer.DRAW_TILE_LABELS = showTileNames.isSelected();
		focus.image = null;

		logGridImports = gridsCB.isSelected();
		gridImportsLogDir = new File(gridsDirTF.getText());
		try {
			// save Log Grid Imports settings
			if(logGridImports) {
				if(!logGridImportsFile.exists()) {
					preferencesDir.mkdirs();
					logGridImportsFile.createNewFile();
				}
				BufferedWriter out = new BufferedWriter( new FileWriter(logGridImportsFile, false) );
				out.write(gridImportsLogDir.getPath());
				out.close();
			} else {
				logGridImportsFile.delete();
			}
		} catch (IOException e) {
			e.printStackTrace();
			logGridImportsFile.delete();
			System.out.println("Not able to create logGridImportsFile");
		}
		
		option.hide();
		option.dispose();
		map.addMouseListener(zoomer);

//		***** GMA 1.6.2: Prompt user to restart GeoMapApp for server and proxy settings to take full effect
		//JOptionPane.showMessageDialog(MapApp.anchor, "Please restart GeoMapApp for server settings to take effect", "Restart GeoMapApp", JOptionPane.PLAIN_MESSAGE);
//		***** GMA 1.6.2

		mapFocus();
	}

	protected void initLayerManager() {

		JFrame d = new JFrame();
		d.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				((JCheckBoxMenuItem)(XML_Menu.commandToMenuItemHash.get("layer_manager_cmd"))).setSelected(false);
			}
		});
		LayerManager lm;
		
		//use existing layer manager if it already exists
		if (layerManager != null) {
			lm = layerManager;
		} else {
			lm = new LayerManager();
		}
		
		lm.setLayerList( toLayerList(map.overlays) );
		lm.setMap(map);

		lm.setDialog(d);
		JScrollPane sp = new JScrollPane(lm);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		d.setTitle("Layer Manager");
		d.setContentPane(sp);
//		d.getContentPane().add(sp);
		d.pack();
		d.setSize(new Dimension(lm.getPreferredSize().width+20,lm.getPreferredSize().height+55));
		d.setMaximumSize(new Dimension(400,300));

		d.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		d.setLocationRelativeTo(frame);
		d.setState(Frame.NORMAL);
		d.setAlwaysOnTop(true);
		this.layerManager = lm;
		this.layerManagerDialog = d;	
	}

	private List<Overlay> toLayerList(Vector<Overlay> overlayVector) {
		List<Overlay> layers = new ArrayList<Overlay>();

		for (Overlay layer : overlayVector) {
			layers.add(layer);
		}
		return layers;
	}

	public void setLayerManagerVisible(boolean tf){
		int xFrame = frame.getLocation().x;
		int widthFrame = frame.getWidth();
		int yFrame = frame.getLocation().y;
		int x = xFrame + widthFrame;

		if (!layerManagerDialog.isVisible() && tf) {
			layerManagerDialog.setLocation(x-238, yFrame +60);
			layerManagerDialog.toFront();
		}
		if (layerManagerDialog.isVisible() && tf) {
			layerManagerDialog.setState(Frame.NORMAL);
		}

		layerManagerDialog.setVisible(tf);
		if (tf){
			layerManagerDialog.requestFocus();
			layerManagerDialog.setLocation(x-238, yFrame +60);
		}

		JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem)
			XML_Menu.commandToMenuItemHash.get("layer_manager_cmd");
		if (cbmi != null) cbmi.setSelected(tf);
	}

	public static void findLaunchFile() {
		// Check for our .gma_launch file
		String gmaLocation = MapApp.class.getProtectionDomain().getCodeSource().getLocation().toString();
		String base = gmaLocation.substring(0, gmaLocation.lastIndexOf("/")+1);

		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new URL(base + ".gma_launch").openStream()));
			String str = null;
			while ( (str = in.readLine()) != null) {
				if (str.startsWith("at_sea")) MapApp.AT_SEA = true;
				if (str.startsWith("local_files")) {
					// set the location of the paths file
					if (System.getProperty("geomapapp.paths_location") == null || System.getProperty("geomapapp.paths_location") == "") {
						System.setProperty("geomapapp.paths_location", "htdocs/gma_paths/GMA_paths.xml");
					}
					BASE_URL = PathUtil.getPath("ROOT_PATH");
					String GMRTRootURL = PathUtil.getPath("GMRT2_ROOT_PATH"); 
					URLFactory.addSubEntry(BASE_URL, "htdocs/");
					URLFactory.addSubEntry(BASE_URL.replace("https://", "http://"), "htdocs/");
					URLFactory.addSubEntry(GMRTRootURL, "htdocs/gmrt/");
					URLFactory.addSubEntry(GMRTRootURL.replace("https://", "http://"), "htdocs/gmrt/");
					URLFactory.addSubEntry(BASE_URL.replace("http://", "").replace("https://", ""), base + "htdocs/");
				}
			}
			in.close();
			return;
		} catch (IOException e) {
//			e.printStackTrace();
		}

		String cwd = System.getProperty("user.dir");
		File gma_launch = new File(new File(cwd), ".gma_launch");

		if (!gma_launch.exists()) return;

		try {
			BufferedReader in = new BufferedReader(
					new FileReader( gma_launch
							));
			String str = null;
			while ( (str = in.readLine()) != null) {
				if (str.startsWith("at_sea")) MapApp.AT_SEA = true;
				if (str.startsWith("local_files")) {
					base = gma_launch.getParentFile().toURI().toURL().toString();
					// set the location of the paths file
					if (System.getProperty("geomapapp.paths_location") == null || System.getProperty("geomapapp.paths_location") == "") {
						System.setProperty("geomapapp.paths_location", "htdocs/gma_paths/GMA_paths.xml");
					}
					BASE_URL = PathUtil.getPath("ROOT_PATH");
					String GMRTRootURL = PathUtil.getPath("GMRT2_ROOT_PATH"); 
					URLFactory.addSubEntry(GMRTRootURL, "htdocs/gmrt/");
					URLFactory.addSubEntry(GMRTRootURL.replace("https://", "http://"), "htdocs/gmrt/");
					URLFactory.addSubEntry(BASE_URL, "htdocs/");
					URLFactory.addSubEntry(BASE_URL.replace("https://", "http://"), "htdocs/");
					URLFactory.addSubEntry(BASE_URL.replace("http://", "").replace("https://", ""), base + "htdocs/");
				}
			}
			in.close();
			return;
		} catch (IOException e) {
//			e.printStackTrace();
		}
	}
	
	public void setMenuFont(final Font newFont) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				UIManager.put("Menu.font", newFont);
				UIManager.put("MenuItem.font", newFont);
				UIManager.put("CheckBoxMenuItem.font", newFont);
				if(null != menuBar) {
					menuBar.setFont(newFont);
					menuBar.updateUI();
					menuBar.update(menuBar.getGraphics());
					menuBar.invalidate();
					menuBar.repaint();
					//menuBar.setVisible(false);
					//recursively change the menu fonts
					Runnable r = new Runnable() {
						private void changeFont(JComponent jc) {
							if(null != jc) {
								jc.setFont(newFont);
								if(jc instanceof JMenu) {
									JMenu jm = (JMenu)jc;
									int numSubMenus = jm.getItemCount();
									for(int i = 0; i < numSubMenus; i++) {
										changeFont(jm.getItem(i));
									}
								}
							}
						}
						public void run() {
							int c = menuBar.getMenuCount();
							for(int i = 0; i < c; i++) {
								changeFont(menuBar.getMenu(i));
							}
						}
					};
					r.run();
				}
				//menuBar.setVisible(true);
				System.out.println("Changed menu font to " + newFont);
			}
		});
	}
	
	public static MapApp getApp() {
		return theApp;
	}

	public static MapApp createMapApp(String[] args) {		
		findLaunchFile();
		
		if (BASE_URL == null) BASE_URL = PathUtil.getPath("ROOT_PATH");
		
		String atSea = System.getProperty("geomapapp.at_sea");
		if ("true".equalsIgnoreCase(atSea))
			MapApp.AT_SEA = true;

		String baseURL = System.getProperty("geomapapp.base_url");
		if (baseURL != null) {
			URLFactory.subMap.put(MapApp.BASE_URL, baseURL);
		}

		com.Ostermiller.util.Browser.init();
		
		UIManager.put("Menu.font", XML_Menu.getMenuFont());
		UIManager.put("MenuItem.font", XML_Menu.getMenuFont());
		UIManager.put("CheckBoxMenuItem.font", XML_Menu.getMenuFont());

		
		MapApp app = null;
		if( args.length==0) {
			app = new MapApp();
		} else if( args.length==1) {
			app = new MapApp(args[0]);
		} else if( args.length==2) {
			app =new MapApp(args[0], args[1]);
		}
		
		if(null == app) {
			versionGMRT = MMapServer.getVersionGMRT();
			baseFocusName = "GMRT Image Version " + versionGMRT;
		}
//		else if(isNewVersion) {
//			if(app.serverFile.exists()) {
//				try {
//					BufferedReader sfReader = new BufferedReader(new FileReader(app.serverFile));
//					String server = sfReader.readLine();
//					if(!server.equals(DEV_URL)) {
//						PrintStream ps = new PrintStream(new FileOutputStream(app.serverFile, false));
//						ps.println(DEFAULT_URL);
//					}
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		}
		return app;
	}

	public static void setup() {
		fixVendorNameIssue(com.sun.media.imageioimpl.common.PackageUtil.class, "Sun", "1.1", "JAI");
		//fixes issue with column sorting
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
	}

	public static void main( String[] args) {
		setup();
		theApp = createMapApp(args);
	}

	public static String getBaseURL() {
		return BASE_URL;
	}
	public static void setBaseURL( String url ) {
		BASE_URL = url;
	}
	public static JFileChooser getFileChooser() {
		if( chooser==null ) chooser = new JFileChooser(System.getProperty("user.home"));
		chooser.setDialogTitle("Open");
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.resetChoosableFileFilters();
		chooser.setAcceptAllFileFilterUsed(true);
		return chooser;
	}

	public Zoomer getZoomer(){
		return zoomer;
	}
	public BufferedImage getBaseMap(){
		return baseMap.getImage();
	}

//	***** GMA 1.6.4: Make "tools" accessible by other classes
	public MapTools getMapTools() {
		return tools;
	}

//	***** GMA 1.4.8: Consolidate code for importing a data table into one function
	public void importDataTable( String optionSelected, String title ) {
		importDataTable(optionSelected, title, null);
	}
	public void importDataTable(String optionSelected, String title, XML_Menu xmlMenu_item) {
		if(custom.loadDB()) {
			disableCurrentDB();

			currentDB = custom;
			currentDB.setEnabled(true);

			addCurrentDBToDisplay();

			while( map.hasOverlay( custom ) ) {
				map.removeOverlay(custom);
			}
			map.addOverlay(title == null ? "Imported Data" : title, custom, xmlMenu_item);
			custom.currentLoadOption = optionSelected;
			custom.titleOfDataset = title;
			custom.load();
		}
		return;
	}

	public void importDataTableNew( String optionSelected, String title ) {
		importDataTableNew(optionSelected, title, null);
	}
	public void importDataTableNew(String optionSelected, String title, XML_Menu xmlMenu_item) {
		if(custom.loadDB()) {
			disableCurrentDB();

			currentDB = custom;
			currentDB.setEnabled(true);

			custom.currentLoadOption = optionSelected;
			custom.titleOfDataset = title;
			custom.load(this);
		}
		return;
	}

	// GMA 1.5.2: Return names of items in Database menu
	public String[] getDBNames() {
		String[] dbNames = new String[db.length];
		int i = 0;
		for (Database d : db) {
			dbNames[i++] = d.getDBName();
		}
		return dbNames;
	}

	protected void load512TileSet(XML_Menu tile_menu) throws IOException {
		LayerSetDetails lsd = LayerSetDetails.levelsFromXML_Menu(tile_menu);
		if (lsd == null) return;

		Tile512Overlay overlay = new Tile512Overlay(lsd, map);
		overlay.setLegendURL(tile_menu.legend);
		overlay.setWarningURL(tile_menu.warning);
		addFocusOverlay(overlay, tile_menu.name, tile_menu.infoURLString, tile_menu);

		// For all tile_512_cmd zoom to wesn when loaded. If wesn is null default to world.
		double wesnXML[] = new double[4];
		String[] s;
		try {
			s = tile_menu.wesn.split(",");
		} catch(NullPointerException ne) {
			String world_wesn = "-180,180,-90,90";
			s = world_wesn.split(",");
		}
		for (int i = 0; i < 4; i++) {
			wesnXML[i] = Double.parseDouble(s[i]);
		}
		// zoom history set first zoom checks it for the first time only, does zoom, tracks zoom after
		map.setZoomHistoryPast(map);
		map.zoomToWESN(wesnXML);
		map.setZoomHistoryNext(map);

		if (map.getZoom() <= 1.5) {
			map.repaint();
		} else {
			mapFocus();
		}
	}

	protected void addExternalImage(XML_Menu image_menu) {
		double wesn[] = new double[4];
		String[] s = image_menu.wesn.split(",");
		for (int i = 0; i < 4; i++)
			wesn[i] = Float.parseFloat(s[i]);

		try {
			BufferedImage image = ImageIO.read(
					URLFactory.url(image_menu.layer_url));
			ImageOverlay overlay;
			if (image_menu.mapproj.equals("m"))
				overlay = new MercatorImageOverlay(map, image, wesn);
			else if (image_menu.mapproj.equals("g"))
				overlay = new GeographicImageOverlay(map, image, wesn);
			else {
				System.err.println("Unsupported projection " + image_menu.mapproj);
				return;
			}
			overlay.setLegendURL(image_menu.legend);

			addFocusOverlay(overlay, image_menu.name, image_menu.infoURLString);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void getWFSLayer(XML_Menu wfs_menu) {
		String wfsURL = wfs_menu.layer_url;
		String wfsTitle = wfs_menu.name;
		String wfsLayer = wfs_menu.wfs_layer_feature;
		String wfsBbox;
		if (!URLFactory.checkWorkingURL(wfsURL)) {
			JOptionPane.showMessageDialog(MapApp.anchor, "Error loading layer:\n"+wfsTitle, "Error", JOptionPane.ERROR_MESSAGE);
			layerManager.missingLayer(wfs_menu.index);
			return;
		}
		if(wfs_menu.wfs_bbox !=null){
			wfsBbox = wfs_menu.wfs_bbox;
		}else{
			wfsBbox ="-180,-90,180,90";
		}

		WFSViewServer wfsWindow = new WFSViewServer(this);
		try {
			wfsWindow.remoteWFS();
		} catch (Exception ignore) {
			ignore.printStackTrace();
		}
		try {
			URL inputCapabilitiesURL = URLFactory.url(wfsURL);
			wfsWindow.dispose();
			wfsWindow.setCapabilitiesURL(inputCapabilitiesURL);
			wfsWindow.readCapabilities(inputCapabilitiesURL);
			wfsWindow.setCurrentWFSTitle(wfsTitle);
			wfsWindow.setCurrentLayerName(wfsLayer);
			wfsWindow.setCurrentWFSBbox(wfsBbox);
			wfsWindow.loadLayer();
			//wfsWindow.loadWFSLayer(wfs_menu);
		} catch (MalformedURLException e) {
			System.out.println("Could not load WFS layer from URL - layer: " + wfsURL + " - " + wfsLayer);
			e.printStackTrace();
		}
	}

	public void getWMSLayer(XML_Menu wms_XML_Menu) throws IOException {
		
		if (!URLFactory.checkWorkingURL(wms_XML_Menu.layer_url)) {
			JOptionPane.showMessageDialog(MapApp.anchor, "Error loading layer:\n"+wms_XML_Menu.name, "Error", JOptionPane.ERROR_MESSAGE);
			layerManager.missingLayer(wms_XML_Menu.index);
			return;
		}
		double[] inputWESN = new double[4];
		if (wms_XML_Menu.wesn != null) {
			String[] s = wms_XML_Menu.wesn.split(",");
			for (int i = 0; i < 4; i++)
				inputWESN[i] = Double.parseDouble(s[i]);
			if (whichMap == MapApp.SOUTH_POLAR_MAP) {
				wms_XML_Menu.srs = "EPSG:3031";
			}
		}

		int mapRes = Integer.MAX_VALUE;
		if (wms_XML_Menu.mapres != null)
			try {
				mapRes = Integer.parseInt(wms_XML_Menu.mapres);
			} catch (NumberFormatException ex) {}

		addProcessingTask(wms_XML_Menu.name,
				XML_Layer.accessWMSTask(this,
						wms_XML_Menu.srs,
						wms_XML_Menu.layer_url,
						wms_XML_Menu.layers,
						wms_XML_Menu.name,
						wms_XML_Menu.infoURLString,
						inputWESN,
						mapRes,
						wms_XML_Menu));
	}

//	***** GMA 1.5.2: Add function to access "Database" menu menu items
	public void accessDatabaseMenuItems(final String accessItem) {
		for( int i=0 ; i<db.length ; i++) {
			if( accessItem.equals( db[i].getDBName()) ) {

				final Database database = db[i];
				addProcessingTask(database.getDBName(), new Runnable() {
					public void run() {
						Runtime rt = Runtime.getRuntime();
						long free = rt.freeMemory()/1024/1024;
						long total = rt.totalMemory()/1024/1024;
						System.out.println("before:\t" + free +" MB Free,\t" + (total-free) +" MB used");
						if(database.loadDB()) {
							disableCurrentDB();

							currentDB = database;
							currentDB.setEnabled(true);

							addCurrentDBToDisplay();

							while( map.hasOverlay( database ) ) {
								map.removeOverlay(database);
							}
							map.addOverlay(database.getDBName(), database );
						}
						free = rt.freeMemory()/1024/1024;
						total = rt.totalMemory()/1024/1024;
						System.out.println("after:\t" + free +" MB Free,\t" + (total-free) +" MB used");
						map.repaint();
					}
				});
			}
		}
		if ( accessItem == "Drilling-DSDP/ODP/BRG" ) {
			showDSDP();
		}
	}

	// GMA 1.5.2: Get tools.suite.layers for the layer tree to give to data portal
	public void addShapeFile(String layerURL) {
		try {
			tools.suite.addShapeFile(layerURL);
			if ( !tools.shapeTB.isSelected() ) {
				tools.shapeTB.doClick();
			}
			else {
				tools.shapes.setVisible(true);
			}
			initializeColorScale();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addShapeFile(String layerURL, XML_Menu inputXML_Menu) {
		try {
			if (!URLFactory.checkWorkingURL(layerURL)) {
				JOptionPane.showMessageDialog(MapApp.anchor, "Error loading layer:\n"+inputXML_Menu, "Error", JOptionPane.ERROR_MESSAGE);
				layerManager.missingLayer(inputXML_Menu.index);
				return;
			}
			tools.suite.addShapeFile(layerURL, inputXML_Menu);			
			if ("true".equals(inputXML_Menu.multipleshapes)) {

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if ( !tools.shapeTB.isSelected() ) {
							tools.shapeTB.doClick();
						} else {
							tools.shapes.setVisible(true);
						}
						tools.shapes.selectIndex(tools.suite.getRowCount() - 1);
					}
				});
			}
			initializeColorScale();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

//	***** GMA 1.5.2: Added this function to access global datasets
	public void getGlobalDataset(String inputName, String inputURL, int inputType) {
		if(custom.loadDB()) {
			disableCurrentDB();

			currentDB = custom;
			currentDB.setEnabled(true);
			addCurrentDBToDisplay();

			map.addOverlay(inputName, custom );
			custom.loadURL(inputName, inputURL, inputType);
		}
		return;
	}

	/**
	* Adds another request for an autofocus.
	* MapApp will wait AUTO_FOCUS_WAIT ms until focusing
	* MapApp will only autofocus if the autoFocus Check box is selected
	* If autoFocus() is called while waiting the timer will be set back to 0
	*  and wait another AUTO_FOCUS_WAIT seconds
	*/
	public void autoFocus() {
		synchronized (autoFocusLock) {
			if (focusTime == -1) {
				focusTime = System.currentTimeMillis() + AUTO_FOCUS_WAIT;
				Thread t =new Thread("Focus Thread" + focusTime) {
					public void run() {
						while (System.currentTimeMillis() < focusTime)
							try {
								sleep(200);
							} catch (InterruptedException e) {
								e.printStackTrace();
						}
						if (focusTime!=-1) {
							mapFocus();
						}
					}
				};
				t.setDaemon(true);
				t.start();
			} else{
				focusTime = System.currentTimeMillis() + AUTO_FOCUS_WAIT;
			}
		}
	}

	public void addFocusOverlay(FocusOverlay overlay) {
		addFocusOverlay(overlay, "Overlay", null);
	}

	public void addFocusOverlay(FocusOverlay overlay, String overlayName) {
		addFocusOverlay(overlay, overlayName, null);
	}

	public void addFocusOverlay(FocusOverlay overlay, String overlayName, String infoURLString) {
		addFocusOverlay(overlay, overlayName, infoURLString, null);
	}

	public void addFocusOverlay(FocusOverlay overlay, String overlayName, String infoURLString, XML_Menu menu_item) {
		synchronized (focusOverlays) {
			if (focusOverlays.add(overlay)) {
				map.addOverlay(overlayName,infoURLString,overlay, menu_item);
				autoFocus();
			}
		}
	}

	public void removeFocusOverlay(FocusOverlay overlay) {
		removeFocusOverlay(overlay, true);
	}

	public void removeFocusOverlay(FocusOverlay overlay, boolean removeFromMap) {
		synchronized (focusOverlays) {
			focusOverlays.remove(overlay);
			if (removeFromMap)
				map.removeOverlay(overlay);
		}
	}

	public void addWMSLayer(String url, String wesnString, String srs) {
		String[] results = wesnString.split(",");
		double[] wesn = new double[4];
		for ( int i = 0; i < results.length; i++ ) {
			wesn[i] = Double.parseDouble(results[i]);
		}
		this.addWMSLayer(url, wesn, srs);
	}

	public void addWMSLayer(String url, double[] wesn, String srs) {
		this.addWMSLayer(System.currentTimeMillis()+"",url, wesn, srs);
	}

	public void addWMSLayer(String name, String url, float[] wesn, String srs) {
//		if (url == null) {
//			int i = 0;
//			while (i < focusOverlays.size())
//				if (focusOverlays.get(i) instanceof WMS_ESPG_4326_Overlay
//						||
//						focusOverlays.get(i) instanceof WMS_ESPG_3031_Overlay) {
//					map.overlays.remove(focusOverlays.get(i));
//					focusOverlays.remove(i);
//				}
//				else
//					i++;
//			mapFocus();
//			return;
//		}
		if ("EPSG:4326".equals(srs)) {
			if(url.contains("1.3.0")) {
				addFocusOverlay(new WMS_ESPG_4326_Overlay(
						url + wmsCRS +"&",
						map,
						name),name);
			} else {
				addFocusOverlay(new WMS_ESPG_4326_Overlay(
					url + wmsSRS4326 +"&",
					map,
					name),name);
			}
		} else if ("EPSG:3031".equals(srs)) {
			if(url.contains("1.3.0")) {
				addFocusOverlay(new WMS_ESPG_3031_Overlay(
					url + wmsCRS3031 + "&",
					map,
					name),name);
			} else {
				addFocusOverlay(new WMS_ESPG_3031_Overlay(
						url + wmsSRS3031 + "&",
						map,
						name),name);
			}
		}
		if (map.getZoom() <= 1.5) {
			map.repaint();
		} else {
			mapFocus();
		}
	}
	public void addWMSLayer(String name, String url, float wesn[], String srs, String infoUrl) {
		if ("EPSG:4326".equals(srs)) {
			if(url.contains("1.3.0")) {
				addFocusOverlay(new WMS_ESPG_4326_Overlay(
					url + wmsCRS + "&",
					map,
					name),name, infoUrl);
			} else {
				addFocusOverlay(new WMS_ESPG_4326_Overlay(
						url + wmsSRS4326 + "&",
						map,
						name),name, infoUrl);
			}
		} else if ("EPSG:3031".equals(srs)) {
			if(url.contains("1.3.0")) {
				addFocusOverlay(new WMS_ESPG_3031_Overlay(
					url + wmsCRS3031 + "&",
					map,
					name),name, infoUrl);
			} else {
				addFocusOverlay(new WMS_ESPG_3031_Overlay(
						url + wmsSRS3031 + "&",
						map,
						name),name, infoUrl);
			}
		}
		if (map.getZoom() <= 1.5) {
			map.repaint();
		} else {
			mapFocus();
		}
	}

	public void addWMSLayer(String name, String url, double wesn[], String srs) {
		addWMSLayer(name, url, wesn, srs, "");
	}

	public void addWMSLayer(String name, String url, double wesn[], String srs, String infoURL) {
		if ("EPSG:4326".equals(srs)) {
			if(infoURL.length() == 0) {
				if(url.contains("1.3.0")) {
					addFocusOverlay(new WMS_ESPG_4326_Overlay(
							url + wmsCRS + "&",
							map,
							name,wesn),name);
				} else { 
					addFocusOverlay(new WMS_ESPG_4326_Overlay(
							url + wmsSRS4326 + "&",
							map,
							name,wesn),name);
				}
			} else {
				if(url.contains("1.3.0")) {
					addFocusOverlay(new WMS_ESPG_4326_Overlay(
						url + wmsCRS + "&",
						map,
						name,wesn),name, infoURL);
				} else {
					addFocusOverlay(new WMS_ESPG_4326_Overlay(
							url + wmsSRS4326 + "&",
							map,
							name,wesn),name, infoURL);
				}
			}
		} else if ("EPSG:3031".equals(srs)) {
			if(infoURL.length() == 0) {
				if(url.contains("1.3.0")) {
					addFocusOverlay(new WMS_ESPG_3031_Overlay(
							url + wmsCRS3031 + "&",
							map,
							name,wesn),name);
				} else {
					addFocusOverlay(new WMS_ESPG_3031_Overlay(
							url + wmsSRS3031 + "&",
							map,
							name,wesn),name);
				}
			} else {
				if(url.contains("1.3.0")) {
					addFocusOverlay(new WMS_ESPG_3031_Overlay(
						url + wmsCRS3031 + "&",
						map,
						name,wesn),name, infoURL);
				} else {
					addFocusOverlay(new WMS_ESPG_3031_Overlay(
							url + wmsSRS3031 + "&",
							map,
							name,wesn),name, infoURL);
				}
			}
		}
		if (map.getZoom() <= 1.5) {
			map.repaint();
		} else {
			mapFocus();
		}
	}

	public void addWMSLayer(String name, String url, String infoURLString,
			double wesn[], String srs, int mapRes){
		addWMSLayer(name, url, infoURLString, wesn, srs, mapRes, null);
	}

	public void addWMSLayer(String name, String url, String infoURLString,
			double wesn[], String srs, int mapRes, XML_Menu xml_item) {
		if ("EPSG:4326".equals(srs)) {
			if(url.contains("1.3.0")) {
				addFocusOverlay(
					new WMS_ESPG_4326_Overlay(url + wmsCRS + "&",map, name, wesn, mapRes),
					name, infoURLString, xml_item);
			} else {
				addFocusOverlay(
					new WMS_ESPG_4326_Overlay(url + wmsSRS4326 + "&",map, name, wesn, mapRes),
					name, infoURLString, xml_item);
			}
		}
		else if ("EPSG:3031".equals(srs)) {
			if(url.contains("1.3.0")) {
				addFocusOverlay(
					new WMS_ESPG_3031_Overlay(url + wmsCRS3031 + "&", map, name, wesn, mapRes),
					name, infoURLString, xml_item);
			} else {
				addFocusOverlay(
				new WMS_ESPG_3031_Overlay(url + wmsSRS3031 + "&", map, name, wesn, mapRes),
				name, infoURLString, xml_item);
			}
		}
		//resort the layers when loading a session file
		if (xml_item.index != null) layerManager.sortLayers();
		
		if (map.getZoom() <= 1.5) {
			map.repaint();
		}
		else {
			mapFocus();
		}
	}

	public void addWMSLayer(Layer layer, String url){
		// Get Layer name
		String layerNameWMS = "[WMS: " + haxby.wms.WMSViewServer.serverList.getSelectedItem().toString()+
							"] " + layer.getTitle();
		if (layer == null) return;

		// Get info URL
		String infoURL = null;
		if(layer.getDataURLs() != null) {
			infoURL = layer.getDataURLs()[0];
		} else if (layer.getMetadataURLs() != null) {
			infoURL = layer.getMetadataURLs()[0];
		}

		if (getMapType() == MapApp.MERCATOR_MAP && layer.supportsSRS("EPSG:4326")) {
			if(layer.getName()==null) {
				addWMSLayer(url, layer.getWesn(), "EPSG:4326");
			} else if (infoURL != null || layer.getLatLonBoundingBox()) {
				// Show info or zoomto button or both.
				if(layer.getLatLonBoundingBox()) {
					double []wesn =new double[4];
					wesn[0] = layer.getWesn()[0];
					wesn[1] = layer.getWesn()[1];
					wesn[2] = layer.getWesn()[2];
					wesn[3] = layer.getWesn()[3];
					if (infoURL !=null) {
						// With info and zoomto icons
						addWMSLayer(layerNameWMS, url, wesn, "EPSG:4326",infoURL);
					} else {
						// With zoomto icon
						addWMSLayer(layerNameWMS, url, wesn, "EPSG:4326");
					}
			} else {
					// With info icon
					addWMSLayer(layerNameWMS, url, layer.getWesn(), "EPSG:4326", infoURL);
				}
			} else {
				// With no extra icons
				addWMSLayer(layerNameWMS, url, layer.getWesn(), "EPSG:4326");
			}
		} else if (getMapType() == MapApp.SOUTH_POLAR_MAP && layer.supportsSRS("EPSG:3031")) {
			if(layer.getName()==null) {
				addWMSLayer(url, layer.getWesn(), "EPSG:3031");
			} else if (infoURL != null || layer.getLatLonBoundingBox()) {
				if(layer.getLatLonBoundingBox()) {
					double []wesn =new double[4];
					wesn[0] = layer.getWesn()[0];
					wesn[1] = layer.getWesn()[1];
					wesn[2] = layer.getWesn()[2];
					wesn[3] = layer.getWesn()[3];
					if (infoURL != null){
						// With info and zoomto icons
						addWMSLayer(layerNameWMS, url, wesn, "EPSG:3031", infoURL);
					} else {
						// With zoomto icon
						addWMSLayer(layerNameWMS, url, wesn, "EPSG:3031");
					}
				} else {
					// With info icon
					addWMSLayer(layerNameWMS, url, layer.getWesn(), "EPSG:3031", infoURL);
				}
			} else {
				// With no extra icons
				addWMSLayer(layerNameWMS, url, layer.getWesn(), "EPSG:3031");
			}
		}
	}

	public void addDevPasswordField() {
		inputDevPasswordPanel.setVisible(true);
		serverPanel.repaint();
		option.pack();
		option.show();
	}

	public void removeDevPasswordField() {
		if ( inputDevPasswordPanel != null ) {
			inputDevPasswordText.setText("");
			inputDevPasswordPanel.setVisible(false);
			serverPanel.repaint();
			option.pack();
			option.show();
		}
	}

	// For preferences
	public void getServerList() throws IOException {
		servers = new Vector();
		if ( serverDir.exists() && serverFile.exists() ) {
			if(isNewVersion) {
				BufferedReader serverIn = new BufferedReader( new FileReader(serverFile) );
				//should only have one line in default_server.dat
				String server = serverIn.readLine();
				serverIn.close();
				server = server.replace("http://", "https://");
				servers.add(server);
				if(!server.equals(DEV_URL)) {
					server = DEFAULT_URL;
					BufferedWriter out = new BufferedWriter(new FileWriter(serverFile,false));
					out.write(DEFAULT_URL + "\r\n");
					out.flush();
					out.close();
				}
				else {
					DEFAULT_URL = server;
				}
				BASE_URL = server;
				TEMP_BASE_URL = server;
			}
			else {
				BufferedReader serverIn = new BufferedReader( new FileReader(serverFile) );
				String s = null;
				while ( ( s = serverIn.readLine() ) != null ) {
					s = s.replace("http://", "https://");
					servers.add(s);
					DEFAULT_URL = s;
					BASE_URL = s;
					TEMP_BASE_URL = s;
				}
				serverIn.close();
			}
		}
		else {
			if ( !serverDir.exists() ) {
				serverDir.mkdirs();
			}
			serverFile.createNewFile();
			BufferedWriter out = new BufferedWriter( new FileWriter(serverFile, true) );
			out.write( DEFAULT_URL + "\r\n" );
			out.flush();
			out.close();
		}
		serverURLString = PathUtil.getPath("SERVER_LIST",
				BASE_URL+"/gma_servers/server_list.dat");
		URL serverURL = URLFactory.url(serverURLString);
		BufferedReader serverURLIn = new BufferedReader( new InputStreamReader( serverURL.openStream()));
		String s = null;
		while (( s = serverURLIn.readLine() ) != null ) {
			if ( s.indexOf("http") == 0 && !servers.contains(s)) {
				servers.add(s);
			}
		}
		if ( servers.isEmpty() ) {
			servers.add(DEFAULT_URL);
			servers.add(DEV_URL);
		}
		serverList = new JComboBox(servers);
		
	}

	public void getProxies() throws IOException {
		proxies = new LinkedList<String>();
		if ( proxyDir.exists() && proxyFile.exists() ) {
			BufferedReader proxiesIn = new BufferedReader( new FileReader(proxyFile) );
			String s = null;
			while ( ( s = proxiesIn.readLine() ) != null ) {
				if ( !proxies.contains(s) ) {
					proxies.add( s );
				}
			}
			proxiesIn.close();
		} else {
			if ( !proxyDir.exists() ) {
				proxyDir.mkdirs();
			}
			proxyFile.createNewFile();
		}
	}

	public void updatePortal(String portalUpdatedTime) throws IOException {
		// delete text file
		if (portalSelectFile.exists()) {
			portalSelectFile.delete();
		}

		// delete old .dat file
		if (portalSelectFileOld.exists()) {
			portalSelectFileOld.delete();
		}

		// make and update new file
		menusCacheDir.mkdirs();
		portalSelectFile.createNewFile();

		// Put in new values just mb for now from server
		BufferedWriter outMB = new BufferedWriter( new FileWriter(portalSelectFile, false) );
		outMB.write(portalUpdatedTime);
		outMB.close();
		mbPortalCache.setSelected(true);
	}

	// Get default_portals.txt file to determine which portals to cache.
	protected void getPortalCacheSelect() throws IOException {
		URL urlPortals;
		String portalUpdatedTime;
		String portalCacheURL = PathUtil.getPath("PORTALS_CACHE_TEXT_PATH",
				MapApp.BASE_URL+"/data/portals/default_portals.txt");
		// Get the portals cache file from the server.
		try {
			urlPortals = URLFactory.url(portalCacheURL);
			BufferedReader inPortals = new BufferedReader(new InputStreamReader(urlPortals.openStream()));
			portalUpdatedTime = inPortals.readLine();

				//System.out.println("pt " + portalUpdatedTime);
			// If local cache directory exists and portal cache file exists
			if ( menusCacheDir.exists() && portalSelectFile.exists() ) {
				// read local file
				BufferedReader cacheSelectIn = new BufferedReader( new FileReader(portalSelectFile) );
				String s = null;
				s = cacheSelectIn.readLine();
				if(s == null) {
					// If file seems empty Don't cache
					mbPortalCache.setSelected(false);
					updatePortal(portalUpdatedTime);
				} else if (s != null) {
					String[]str = s.split("\t"); // local
					String[]serverS = portalUpdatedTime.split("\t");

					if(str[0] == null) { // nothing in cache file
						// If file seems empty Don't cache
						mbPortalCache.setSelected(false);
						updatePortal(portalUpdatedTime);
					} else if(str[0].matches("multibeam_bathymetry_cmd")) { // Its mb portal?
						//System.out.println("test ser " + serverS[1] + " test loc " + str[1]);
						if(str.length >1 && str[1] !=null) { // note: older versions read the line not tabs check this
							if(serverS[1].matches(str[1])) {
								// if local time match sever file time true for cache
								mbPortalCache.setSelected(true);
							} else { // different times
								// if local time doesnt match server time false and replace the entire portal file
								mbPortalCache.setSelected(false);
								//portalSelectFile.delete();			// delete file
								updatePortal(portalUpdatedTime);	// make and update new file
								System.out.println("updating cache");
							}
						} else { // handle old way
							mbPortalCache.setSelected(false);
							portalSelectFile.delete();			// delete file
							updatePortal(portalUpdatedTime);	// make and update new file
							System.out.println("older cache");
						}
					}
					// For reading other lines for future portals
					while (( s = cacheSelectIn.readLine() ) != null ) {
						String[]strOther = s.split("\t");
						//if certain commands then set the checkbox preference.
						//System.out.println("end " + strOther[0] + " " + strOther[1]);
					}
				}
				cacheSelectIn.close();
			}
			else if (menusCacheDir.exists() && !portalSelectFile.exists()) {
				// portal file has been deleted - set to false
				mbPortalCache.setSelected(false);
			}
		// FIRST TIME for portals cache set default to be true.
			else {
				if ( !menusCacheDir.exists() ) { // make sure this is in place
					menusCacheDir.mkdirs();
				}
				if( !portalSelectFile.exists() ) { // make sure this is in place
					//System.out.println("creating new cache");
					// Start as default cache MB portal get new time.
					updatePortal(portalUpdatedTime);
				}
			}
		} catch (MalformedURLException e) {
			// Error, set to false
			mbPortalCache.setSelected(false);
		} catch (IOException e) {
			// Error, set to false
			mbPortalCache.setSelected(false);
		}
	}

	// start new zoom history
	protected void startNewZoomHistory() {
		if(!historyDir.exists()) {
			historyDir.mkdirs();
		}
		/*
		if(historyFile.exists()) {
			// delete old zoom.txt file
			historyFile.delete();
		}
		try {
			historyFile.createNewFile();
		} catch (IOException e) {
			//System.out.println(e);
		}
		*/
	}

	/*protected void updateZoomHistory(String past, String next) throws IOException {
		if(!historyFile.exists()) {
			startNewZoomHistory();
		}

		if(historyFile.canWrite()) {
			BufferedWriter bw = new BufferedWriter(new FileWriter(historyFile.getAbsoluteFile(), false));
			bw.write(past + "\n");
			bw.write(next);
			bw.close();
		}
	}*/

	// get the current version number stored in .GMA/history/version
	public String getHistoryVersion() {
		String version = "";
		if (historyVersionFile.exists()) {
			//read history/version file
			try {
				BufferedReader inLocal = new BufferedReader(new FileReader(historyVersionFile));
				version = inLocal.readLine();
				inLocal.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} 
		return version;
	}
	
	
	// Get menu updated file, create one if local doesn't exist.
	public boolean getMenusCache() {
		
		//First check GMA version number - if we have a new version, delete the MenusCache 
		//so we can make sure we have the most up-to-date version.
		//Also delete the layerSessionDir directory since old session formats might
		//not be compatible with the latest release
		//String historyVersion = getHistoryVersion();
		if (isNewVersion) {
			//isNewVersion = true;
			//delete MenusCache
			if (menusCacheDir.exists()) {
				GeneralUtils.deleteFolder(menusCacheDir);
			}
			//delete layerSessionDir
			/*if (layerSessionDir.exists()) {
				GeneralUtils.deleteFolder(layerSessionDir);
			}*/
			
			// add history directory if none.
			if(!historyDir.exists()) {
				historyDir.mkdir();
			}
			
			//update the history/version file
			BufferedWriter out;
			try {
				out = new BufferedWriter(new FileWriter(historyVersionFile));
				out.write(VERSION);
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// Get server time
		String menusCacheURL = PathUtil.getPath("MENU_CACHE_PATH",
				MapApp.BASE_URL+"/gma_menus/menu_updated.txt");
		URL urlMenus;
		String menuUpdatedTime;
		try {
			urlMenus = URLFactory.url(menusCacheURL);
			BufferedReader in = new BufferedReader(new InputStreamReader( urlMenus.openStream() ));
			menuUpdatedTime = in.readLine();

			if (menusCacheDir.exists() && menusCacheDir2.exists() && menusCacheFile.exists()) {
				BufferedReader inLocal = new BufferedReader(new FileReader(menusCacheFile));
				String menuLocalTime = inLocal.readLine();
				inLocal.close();
			//System.out.println("localtime: " + menuLocalTime + " remotetime " + menuUpdatedTime);

				// Also add portals directory if none.
				if(!portalCacheDir.exists()) {
					portalCacheDir.mkdir();
				}

				// only return false and get menus from server when localtime equals remote time.
				if(menuLocalTime.matches(menuUpdatedTime) && menusCacheFileFirst.exists() && menusCacheFileLast.exists()) {
					ReadMenusCache = true;
					//System.out.println("true");
				} else if (!menuLocalTime.matches(menuUpdatedTime)) {
					//System.out.println("false");
					// menu_update.txt don't match, get a fresh one.
					BufferedWriter out = new BufferedWriter(new FileWriter(menusCacheFile));
					out.write(menuUpdatedTime);
					out.close();
					ReadMenusCache = false;
				}
				else {
				// If there is no main_menu.xml then the cache might be corrupt.
					ReadMenusCache = false;
				}
			} else {
				// No directory and files yet, make them.
				if(!menusCacheDir.exists()) {
					menusCacheDir.mkdirs();
				} else {
					// If the user system does give permissions to create files get from server.
					ReadMenusCache = false;
				}
				if(!menusCacheDir2.exists()) {
						menusCacheDir2.mkdir();
				} else {
					// If the use system do not allow permission to make.
					ReadMenusCache = false;
				}

				if(!portalCacheDir.exists()) {
					portalCacheDir.mkdir();
				}
				if( !portalSelectFile.exists() ) { // make sure this is in place
					//System.out.println("creating new cache");
					// Start as default cache MB portal get new time.
					URL urlPortals = URLFactory.url(PathUtil.getPath("PORTALS_CACHE_TEXT_PATH"));
					BufferedReader inPortals = new BufferedReader(new InputStreamReader(urlPortals.openStream()));
					String portalUpdatedTime = inPortals.readLine();
					updatePortal(portalUpdatedTime);
				}
			// Check on creation of file
				boolean isCreated = false;
				isCreated = menusCacheFile.createNewFile();
				//System.out.println(isCreated);
				if (isCreated == true) {
					menusCacheFile.createNewFile();
					BufferedWriter out = new BufferedWriter(new FileWriter(menusCacheFile));
					out.write(menuUpdatedTime);
					out.close();
				}
				ReadMenusCache = false;
			}
		} catch (MalformedURLException e) {
			// Error, set to false
			ReadMenusCache = false;
		} catch (IOException e) {
			// Error, set to false
			ReadMenusCache = false;
		}
		return ReadMenusCache;
	}

	public static boolean getMbPortalCache() {
		return mbPortalCache.isSelected();
	}

	public static void setMbPortalCache(boolean b) {
		if(getMbPortalCache() == b) {
			System.out.println ("equal");
		} else if (getMbPortalCache() != b) {
			System.out.println("not equal");
		}
	}

	// Clear cache recursively from given dir.
	protected static void clearCache(File file) throws IOException {
		if(file.isDirectory()) {
			//directory is empty, then delete it
			if(file.list().length==0) {
				file.delete();
			} else {
				//List all the directory contents
				String files[] = file.list();
				for (String temp : files) {
					//Construct the file structure
					File fileDelete = new File(file, temp);

					//Recursive delete
					clearCache(fileDelete);
				}
				//Check the directory again, if empty then delete it
				if(file.list().length==0) {
					file.delete();
					//System.out.println("Removed Cache directory: " + file.getAbsolutePath());
				}
			}
		} else {
			//if file, then delete it
			file.delete();
			//System.out.println("Removed Cache File : " + file.getAbsolutePath());
		}
	}

	public void addProcessingTask(String taskName, Runnable task) {
		Thread t;
		if (task instanceof StartStopTask) {
			t = new StartStopThread( (StartStopTask) task, taskName );
		} else {
			t = new Thread(task, taskName);
		}
		addProcessingTask(taskName, t);
	}

	public void addProcessingTask(String taskName, Thread task) {
		synchronized (processingTaskLock) {
			if (processingDialog == null) processingDialog = new ProcessingDialog(frame, map);
			processingDialog.addTask(taskName, task);
		}
	}

	public void addSilentProcessingTask(SilentProcessingTask task) {
		synchronized (silentProcessingTaskLock) {
			if (silentProcessingDialog == null) silentProcessingDialog = new SilentProcessingDialog();
			silentProcessingDialog.addTask(task);
		}
	}

	public void setCurrentDB( haxby.db.Database inputDB ) {
		currentDB = inputDB;
	}

	public void enableCurrentDB() {
		currentDB.setEnabled(true);
	}

//	Returns the current database that is being displayed and is active
	public haxby.db.Database getCurrentDB() {
		return currentDB;
	}

	public void disableCurrentDB() {
		if( currentDB != null && currentDB.isEnabled()) {
			currentDB.setEnabled(false);
			if(null != currentDB && null != currentDB.getSelectionDialog()) {
				dialog.remove( currentDB.getSelectionDialog() );
			}
		}
	}

//	Displays sidebar of chosen database
	public void addDBToDisplay( haxby.db.Database chosenDatabase ) {
		dbLabel.setText( chosenDatabase.getDBName() );
		dialog.add( chosenDatabase.getSelectionDialog(), "Center");
		hPane.setRightComponent( dialogScroll );
		if( chosenDatabase.getSelectionDialog() != null ) {
			int w = chosenDatabase.getSelectionDialog().getPreferredSize().width;
			hPane.setDividerLocation( hPane.getSize().width -w
				-hPane.getDividerSize() );
		}
		if (chosenDatabase.getDataDisplay() != null) {
			int h = chosenDatabase.getDataDisplay().getPreferredSize().height;
			detach_attachB.setEnabled( h > 50 );

			if (attached || h < 50) {
				attachDisplay();
			}
			else {
				detachDisplay();
			}
		} else {
			detach_attachB.setEnabled(false);
		}
	}

	// Displays Side bar of current DB
	public void addCurrentDBToDisplay() {
		final Database cDB = currentDB;
	//	if (cDB == null) return;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				dbLabel.setText( cDB.getDBName() );
				cDB.getSelectionDialog().setPreferredSize(cDB.getSelectionDialog().getMaximumSize());
				dialog.add( cDB.getSelectionDialog(), "Center");
				Dimension minLayoutSize = dialog.getLayout().minimumLayoutSize(dialog);
				Dimension dialogSize = new Dimension(dialog.getPreferredSize().width, minLayoutSize.height);
				dialog.setPreferredSize(dialogSize);
				dialog.setMaximumSize(dialogSize);
				cDB.getSelectionDialog().setPreferredSize(cDB.getSelectionDialog().getMinimumSize());
				hPane.setRightComponent( dialogScroll );

				if( cDB.getSelectionDialog() != null ) {
					int w = cDB.getSelectionDialog().getPreferredSize().width;
					hPane.setDividerLocation( hPane.getSize().width - w
							-hPane.getDividerSize() );
				}

				if( cDB.getDataDisplay() != null ) {
					int h = cDB.getDataDisplay().getPreferredSize().height;
					detach_attachB.setEnabled( h > 50 );

					if (attached || h < 50) {
						//Attaches Tables spreadsheet under the main map.
						attachDisplay();
					}
					else {
						detachDisplay();
					}
				} else {
					detach_attachB.setEnabled(false);
				}
			}
		});
	}

	public void closeDB( haxby.db.Database databaseToClose ) {
		if ( XML_Menu.commandToMenuItemHash != null &&
				XML_Menu.commandToMenuItemHash.containsKey(databaseToClose.getCommand()) &&
				((JCheckBoxMenuItem)XML_Menu.commandToMenuItemHash.get(databaseToClose.getCommand())).isSelected()) {
			((JCheckBoxMenuItem)XML_Menu.commandToMenuItemHash.get(databaseToClose.getCommand())).setSelected(false);
		}
		Runtime rt = Runtime.getRuntime();
		long free = rt.freeMemory()/1024/1024;
		long total = rt.totalMemory()/1024/1024;
		//System.out.println("before:\t" + free +" MB Free,\t" + (total-free) +" MB used");
		if(databaseToClose == null ) return;
		map.removeOverlay( databaseToClose );
		databaseToClose.setEnabled(false);
		dialog.remove( databaseToClose.getSelectionDialog() );
		getDataDisplayDialog().remove( databaseToClose.getDataDisplay() );
		getDataDisplayDialog().setVisible(false);
		databaseToClose.disposeDB();

		if ( databaseToClose == digitizer ) {
			if (!tools.gridDialog.isDialogVisible()) {
				tools.gridDialog.disposeGrid();
				if (tools.profileB.isSelected()) {
					tools.profileB.doClick();
				}
			}
			if (digitizer.getLoadedGMRTForDig() && tools.gridDialog.getToggle().isSelected()) {
				digitizer.setLoadedGMRTForDig(false);
				tools.gridDialog.getToggle().doClick();
			}
			tools.digitizeB.setSelected(false);
		}
//
		hPane.setRightComponent( null );
		vPane.setBottomComponent( null );
		System.gc();
		databaseToClose = null;
		free = rt.freeMemory()/1024/1024;
		total = rt.totalMemory()/1024/1024;
		System.out.println("after:\t" + free +" MB Free,\t" + (total-free) +" MB used");
	}

	protected void closeCurrentDB() {
		closeDB(currentDB);
		List<Overlay> overlays = layerManager.getOverlays();
		for ( int i = 0; i < overlays.size(); i++ ) {
			if ( overlays.get(i) instanceof haxby.db.Database ) {
				setCurrentDB(((haxby.db.Database)overlays.get(i)));
				enableCurrentDB();
				addDBToDisplay(((haxby.db.Database)overlays.get(i)));
				break;
			}
		}
	}
	
	//
	public static String getAppropriateUrl(String origUrl) {
		String insecureProdUrl = PRODUCTION_URL.replaceFirst("https://", "http://");
		String secureProdUrl = PRODUCTION_URL.replaceFirst("http://", "https://");
		return origUrl.replaceFirst(insecureProdUrl, BASE_URL.replaceFirst("https://", "http://")).replaceFirst(secureProdUrl, (null == BASE_URL)?(PRODUCTION_URL):(BASE_URL));
	}

	protected void toggleDisplayAttachment() {
		if(currentDB == null ) return;

		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				if (attached) {
					detachDisplay();
				} else {
					attachDisplay();
				}
			}
		});
	}
	protected synchronized void attachDisplay() {
		if (currentDB == null) return;

		JFrame displayDialog = getDataDisplayDialog();

		vPane.setBottomComponent( currentDB.getDataDisplay() );
		displayDialog.setVisible(false);
		displayDialog.getContentPane().remove(currentDB.getDataDisplay());

		if( currentDB.getDataDisplay() != null ) {
			int h = currentDB.getDataDisplay().getPreferredSize().height;
			if(h>200) h=200;
			if(currentDB.getDBName().equals("PetDB: Petrological Database Bedrock Chemistry")) {
				h = 145;
			}
			vPane.setDividerLocation( vPane.getSize().height - h
					- vPane.getDividerSize() );
		}
		detach_attachB.setText("Detach Profile/Table");
		attached = true;
	}

	protected synchronized void detachDisplay() {
		if (currentDB == null) return;
		if (currentDB.getDataDisplay() == null) return;
		if (currentDB.getDataDisplay().getPreferredSize().height < 50) return;

		JFrame displayDialog = getDataDisplayDialog();
		displayDialog.setTitle( currentDB.getDBName() );

		displayDialog.getContentPane().removeAll();
		displayDialog.getContentPane().add(currentDB.getDataDisplay());

		int w = vPane.getSize().width;
		int h = currentDB.getDataDisplay().getPreferredSize().height;
		if(h>200) h=200;

		int x = frame.getLocationOnScreen().x;
		int y = frame.getLocationOnScreen().y + frame.getHeight();

		displayDialog.setSize(w, h);
		displayDialog.setLocation(x, y);

		vPane.setBottomComponent( null );
		displayDialog.setState( Frame.NORMAL );
		displayDialog.setVisible(true);
		detach_attachB.setText("Attach Profile/Table");
		attached = false;
	}

	protected JFrame getDataDisplayDialog() {
		if (dataDisplayDialog == null) {
			dataDisplayDialog = new JFrame();
			dataDisplayDialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			dataDisplayDialog.addWindowListener( new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					detach_attachB.doClick();
				}
			});
		}
		return dataDisplayDialog;
	}

	public void toggleContributedGrids() {
		boolean tf = map.hasOverlay( contributedGridsOverlay );
		if (tf) {
			map.removeOverlay( contributedGridsOverlay);
			map.removeMouseListener(contributedGridsOverlay);
		}
		else {
			map.addOverlay("Contributed Grid Locations",contributedGridsOverlay);
			map.addMouseListener(contributedGridsOverlay);
		}
		tools.contributedGrids.setSelected( !tf );
		map.repaint();
	}

	public void toggleContributedGrids(boolean inputtf) {
		boolean tf = inputtf;
		if (tf) {
			if (map.hasOverlay(contributedGridsOverlay)) {
				map.removeOverlay( contributedGridsOverlay);
			}
			map.removeMouseListener(contributedGridsOverlay);
		}
		else {
			if (!map.hasOverlay(contributedGridsOverlay)) {
				map.addOverlay("Contributed Grid Locations",contributedGridsOverlay);
			}
			map.addMouseListener(contributedGridsOverlay);
		}
		tools.contributedGrids.setSelected( !tf );
		map.repaint();
	}

	public boolean isBaseMapVisible() {
		return layerManager == null ||
				layerManager.baseMapVisible;
	}

	public boolean isLayerVisible(Overlay layer) {
		if ((layer == baseMap || layer == baseMapFocus) && !isBaseMapVisible())
			return false;
		return layerManager == null ||
				layerManager.getLayerVisible(layer);
	}

	public static void showPlaces() {
		locs.showPlaces();
	}

	/*
	 * The layer name of the session file we are importing
	 */
	public static void setSessionImport(File xmlFile) {

		try {
			sessionImport = XML_Menu.getRootName(xmlFile);
			return;
		} catch (Exception e) {
			sessionImport = xmlFile.getName().replace(".xml", "");
		}
		//sessionImport = xmlFile.getName().replace(".xml", "");
	}
	
	/*
	 * Compare software version number with version on server
	 */
	private int compareVersions(String vSoftware, String vServer) {
		// This will only trigger the Download New Version alert if the
		// server version is newer than the software version,
		// but since we may need to backtrack if a release goes bad
		// best not to use this in release version.
		if (DEV_MODE) {
			try {
			String[] software = vSoftware.split("\\.");
			String[] server = vServer.split("\\.");
			for (int i=0; i<software.length && i < server.length; i++) {
				int sw = Integer.parseInt(software[i]);
				int sv = Integer.parseInt(server[i]);
				if (Integer.compare(sw,sv) != 0) return Integer.compare(sw,sv) ;
			}
			return Integer.compare(software.length, server.length);
			}
			catch(NumberFormatException nfe) {
				return -1;
			}
		} else {
			//use this line for release version instead.
			if (!vSoftware.equals(vServer)) return -1;
		}
		return 0;
	}
	
	/*
	 * Read in the Grid Imports Log Dir from the saved file, if available
	 */
	private File getGridImportsLogDir() {
		if (logGridImports) {
			if ( preferencesDir.exists() && logGridImportsFile.exists() ) {
				// read local file
				BufferedReader in;
				try {
					in = new BufferedReader( new FileReader(logGridImportsFile) );

					String s = null;
					s = in.readLine();
					in.close();
					if(s != null) {
						return new File(s);
					}
					
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return DEFAULT_GRID_IMPORTS_LOGS_DIR;
	}
	
	public ProcessingDialog getProcessingDialog() {
		return this.processingDialog;
	}

	public static void sendLogMessage(String message) {
		if (AT_SEA) return;
		message = message.replace(" ", "_");
		String logURL = PathUtil.getPath("LOG_PATH") + "?log=" + message 
				+ "&proj=" + CURRENT_PROJECTION + "&gma_version=" + VERSION;
		try {
			HttpURLConnection con = (HttpURLConnection) new URL( logURL ).openConnection();
			con.getResponseCode();
		} catch (Exception e) {
			System.out.println("message NOT logged: " + message);
			e.printStackTrace();
		}
	}
	
	public static void fixVendorNameIssue(Class<?> clazz, String vendor, String version, String specTitle) {
		try {
			Field vendorField = clazz.getDeclaredField("vendor");
			vendorField.setAccessible(true);
			vendorField.set(null, vendor);
			
			Field versionField = clazz.getDeclaredField("version");
			versionField.setAccessible(true);
			versionField.set(null, version);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static int getGraphicsDevice(Component c) {
		GraphicsDevice[] screenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		GraphicsDevice compDevice = c.getGraphicsConfiguration().getDevice();
		for(int i = 0; i < screenDevices.length; i++) {
			if(compDevice.equals(screenDevices[i])) return i;
		}
		return -1;
	}
	
	public static HashSet<String> supported_commands = new HashSet<String>();
	static {
		supported_commands.add("add_bookmark_cmd");
		supported_commands.add("add_image_cmd");
		supported_commands.add("AgeMuller2008Cmd");
		supported_commands.add("ASCIIURLTablesCmd");
		supported_commands.add("bathymetry_credits_cmd");
		supported_commands.add("browse_bookmarks_cmd");
		supported_commands.add("BrowseGridCmd");
		supported_commands.add("BrowseShapefileCmd");
		supported_commands.add("ClipboardTablesCmd");
		supported_commands.add("color_scale_cmd");					//10
		supported_commands.add("CommaTablesCmd");
		supported_commands.add("PipeTablesCmd");
		supported_commands.add("distance_scale_cmd");
		supported_commands.add("ExcelTablesCmd");
		supported_commands.add("ExcelURLTablesCmd");
		supported_commands.add("ExitCmd");
		supported_commands.add("FormatRequirementsGridCmd");
		supported_commands.add("FormatRequirementsShapefileCmd");
		supported_commands.add("FormatRequirementsTablesCmd");
		supported_commands.add("FormatRequirementsSessionCmd");
		supported_commands.add("GeoidSS97Cmd");
		supported_commands.add("GMRTGridNCmd");
		supported_commands.add("GMRTGridSCmd");						//20
		supported_commands.add("GMRTGridGCmd");
		supported_commands.add("GMRTGridMCmd");
		supported_commands.add("GravitySS97Cmd");
		supported_commands.add("GravitySSv18NCmd");
		supported_commands.add("GravitySSv18SCmd");
		supported_commands.add("GravitySSv18MCmd");
		supported_commands.add("import_image_cmd");
		supported_commands.add("ImportWFSCmd");
		supported_commands.add("ImportWMSCmd");
		supported_commands.add("layer_manager_cmd");				//30
		supported_commands.add("load_all_session_layers_cmd");
		supported_commands.add("map_inset_cmd");
		supported_commands.add("map_place_cmd");
		supported_commands.add("open_browser_cmd");
		supported_commands.add("open_search_tree");
		supported_commands.add("PreferencesCmd");
		supported_commands.add("PrintMapWindowCmd");
		supported_commands.add("SaveMapWindowCmd");
		supported_commands.add("SpreadingRateAsymmetryMuller2008Cmd");
		supported_commands.add("SpreadingRateMuller2008Cmd");		//40
		supported_commands.add("shape_cmd");
		supported_commands.add("show_places_cmd");
		supported_commands.add("switch_merc_cmd");
		supported_commands.add("switch_north_cmd");
		supported_commands.add("switch_south_cmd");
		supported_commands.add("table_cmd");
		supported_commands.add("TabTablesCmd");
		supported_commands.add("tile_512_cmd");
		supported_commands.add("TopoSSv9MCmd");
		supported_commands.add("URLShapefileCmd");					//50
		supported_commands.add("wfs_cmd");
		supported_commands.add("wms_cmd");
		supported_commands.add("wms_usgs_quads_cmd");
		supported_commands.add("zoom_to_wesn_cmd");
		supported_commands.add("zoom_to_session_area_cmd");
		supported_commands.add("NASA_DEM_CMD");
		supported_commands.add("close_layer_session_cmd");
		supported_commands.add("import_layer_session_cmd");
		supported_commands.add("reload_layer_session_cmd");
		supported_commands.add("save_layer_session_cmd");			//60
	}
}

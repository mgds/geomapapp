package org.geomapapp.map;

import haxby.map.MapApp;
import haxby.map.MapTools;
import haxby.map.Overlay;
import haxby.map.XMap;
import haxby.proj.ConstrainedIdentityProjection;
import haxby.proj.Mercator;
import haxby.util.FilesUtil;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.MouseInputAdapter;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.geomapapp.grid.Grid2DOverlay;
import org.geomapapp.grid.GridDialog;
import org.geomapapp.grid.HiResGrid;
import org.geomapapp.io.GMARoot;

/**
 * Provides the function to capture the bounds of the current map
 * window and opens a dialog box that requests saving in the map
 * places menu or in a file. The file is placed in the users GMA
 * folder with the suffix .loc.
 * 
 * @author Bill Haxby
 * @author Justin Coplan
 * @author Samantha Chan
 * @version 2.2.1
 * @since 1.2_08
 */

public class MapPlaces implements Overlay {
	protected static Hashtable locs;
	protected static XMap map;
	protected MapTools tools;
	protected static File locFile;
	public static JCheckBoxMenuItem showLoc;
	protected static JCheckBox showBookmarkedPlaces = new JCheckBox("Show Bookmarked Places");
	protected JCheckBox list;
	protected static JCheckBox save;
	protected static JList locList;
	protected MouseInputAdapter mouse;
	protected static MapPlace root;	
	protected static MapPlacesViewer mapPlacesViewer;

	/**
	 * Class constructor specifying the map and tool object to create.
	 * 
	 * @param map
	 * @param tools
	 */
	public MapPlaces(XMap map, MapTools tools ) {
		this.map = map;
		this.tools = tools;
		locs = new Hashtable();
		root = new MapPlace(null, "Bookmarked Places");
		mouse = new MouseInputAdapter() {
			public void mouseMoved(MouseEvent evt) {
				if( evt.isControlDown() )return;;
				select( evt.getPoint());
			}
			public void mouseClicked(MouseEvent evt) {
				if( evt.isControlDown() )return;;
				zoomTo( evt.getPoint() );
			}
		};
		initPlaces();
	}

	/**
	 * When Check Box Menu Item 'Show Places' is selected the overlay
	 * of boxes will be repainted to display to the selected map. 
	 */
	public void showPlaces() {
		map.removeMouseListener(mouse);
		map.removeMouseMotionListener(mouse);
		if( showLoc.isSelected() ) {
			map.addMouseListener(mouse);
			map.addMouseMotionListener(mouse);
			//synch the selected checkbox in another location
			showBookmarkedPlaces.setSelected(true);
			if(map.hasOverlay(this))return;
			map.addOverlay("Bookmarked Places",this,false);
			map.repaint();
		} else {
			showBookmarkedPlaces.setSelected(false);
			if(!map.hasOverlay(this))return;
			map.removeOverlay(this);
			map.repaint();
		}
	}
	/**
	 * The preset values of checkboxes are set.
	 * Retrieves preset places and loads them. 
	 */
	protected void initPlaces() {
		checkFileStructure();
		list = new JCheckBox("Add for This Session Only", true);
		save = new JCheckBox("Save for Future Sessions", false);
		addPresetPlaces();
		loadMyPlaces();
	}
	
	@SuppressWarnings("unchecked")
	public void addPlace( MapPlace loc) {
		locs.put( loc.name, loc );
		MapPlacesViewer.updateViewers();
	}

	public void removePlace( MapPlace loc) {
		locs.remove( loc.name);
		MapPlacesViewer.updateViewers();
	}
	/**
	 * Displays a new panel popup to prompt user on input for
	 * new place name. To add or save a location. The save
	 * option will place the info into MyPlaces.loc file. 
	 */
	public void addPlace() {
		double zoom = map.getZoom();
		Rectangle2D r = map.getClipRect2D();
		Point2D.Double p = new Point2D.Double(
				r.getX()+.5*r.getWidth(),
				r.getY()+.5*r.getHeight() );
		p = (Point2D.Double)map.getProjection().getRefXY(p);
		JPanel panel = new JPanel(new GridLayout(0,1,2,2));
		JLabel label = new JLabel("Bookmark Name: ");
		JTextField nameF = new JTextField("A Place");
		panel.add(label);
		panel.add(nameF);
		panel.add(list);
		panel.add(save);
		int ok = JOptionPane.showConfirmDialog( 
				map.getTopLevelAncestor(),
				panel,
				"Add Bookmark for Current Map View",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if(ok==JOptionPane.CANCEL_OPTION) return;
		if(ok==JOptionPane.CLOSED_OPTION) return;
		
		MapPlace loc = createPlace( 
				root,
				nameF.getText(), 
				p.getX(),
				p.getY(),
				zoom);
		// Will execute list or save options only if Okay button is selected
		if( list.isSelected())  {
			addPlace(loc);
		}
		// Saves to file
		if( save.isSelected()) {
			File gmaRoot = org.geomapapp.io.GMARoot.getRoot();
			// Before we begin 
				// Check to make sure places folder is there
				File file = new File(gmaRoot, "places");
				//Check to make sure all.loc is there
				File allFile = new File(file, "all.loc");
				// Check to make sure My Saved Places.loc is there
				File locFile = new File( file, "My Saved Places.loc");
				
				if( locFile.exists()&& allFile.exists() && file.exists() ) {
					String addLine = createLocStr(loc);
					FilesUtil.addLineinFile("My Saved Places.loc",addLine);

					reloadMyPlaces();
					MapPlacesViewer.updateViewers();
				}else{
					String addLine = createLocStr(loc);
					FilesUtil.addLineinFile("My Saved Places.loc",addLine);

					loadMyPlaces();
					reloadMyPlaces();
					MapPlacesViewer.updateViewers();
				}

		}
	}

	protected String createLocStr(MapPlace loc) {
		return (loc.name
				+"\t"+ loc.lon
				+"\t"+ loc.lat
				+"\t"+ loc.zoom);
	}

	protected MapPlace createPlace(MapPlace root, String text, double x,
			double y, double zoom) {
		return new MapPlace(root, text, x, y, zoom);
	}

	/**
	 * Checks the file structure needed to save and load bookmarks.
	 * @return 
	 */	
	public static void checkFileStructure(){
		File gmaRoot = org.geomapapp.io.GMARoot.getRoot();
		// Before we begin 
		if( gmaRoot!=null ) {
			// Check to make sure places folder is there
			File file = new File(gmaRoot, "places");
			if( !file.exists() ) file.mkdirs();
			// Check to make sure My Saved Places.loc is there
			File locFile = new File( file, "My Saved Places.loc");
			if( !locFile.exists() ) {
				try {
					PrintStream out = new PrintStream(
						new FileOutputStream(locFile));
					out.close();
				} catch(IOException ex) {
				}
			}
			//Check to make sure all.loc is there
			File allFile = new File(file, "all.loc");
			if( !allFile.exists() ) {
				try {
					PrintStream out = new PrintStream(
						new FileOutputStream(allFile));
					out.append( "My Saved Places.loc");
					out.flush();
					out.close();
				} catch(IOException ex) {
				}
			}
			if(allFile.exists()){
				try {
					PrintStream out = new PrintStream(
						new FileOutputStream(allFile));
					out.append( "My Saved Places.loc");
					out.flush();
					out.close();
				} catch(IOException ex) {
				}
			}
		}
	}

	/**
	 * Deletes a previously saved place from My Saved Places
	 * directory.
	 * @param loc location name user input when saving
	 * @see FilesUtil
	 */
	public void deleteBookmarks(MapPlace loc) {
		locs.remove( loc.name);
		System.out.println("Deleting " + loc);
		double zoom = loc.zoom;
		Point2D.Double p = (Point2D.Double)(map.getProjection().getMapXY( 
				new Point2D.Double( loc.lon, loc.lat )));
		p = (Point2D.Double)map.getProjection().getRefXY(p);

		//Call FilesUtil for removal
		FilesUtil.removeLineinFile("My Saved Places.loc", 
				loc + "\t" + p.getX() + "\t" + p.getY() + "\t" + zoom);
	}

	public String saveBookmarks(MapPlace loc) {
		System.out.println("Saving " + loc);
		double zoom = loc.zoom;
		Point2D.Double p = (Point2D.Double)(map.getProjection().getMapXY( 
				new Point2D.Double( loc.lon, loc.lat )));
		p = (Point2D.Double)map.getProjection().getRefXY(p);

		//Return string to write to file
		String sbm =loc + "\t" + p.getX() + "\t" + p.getY() + "\t" + zoom;
		return sbm;
		}
	
	public String importBookmarks(String importFile) {
		System.out.println("Importing " + importFile);

		File gmaRoot = org.geomapapp.io.GMARoot.getRoot();
			File file = new File(gmaRoot, "places");
			File allFile = new File(file, "all.loc");
			File locFile = new File( file, "My Saved Places.loc");

		if( locFile.exists()&& allFile.exists() && file.exists() ) {
			//Call FilesUtil for removal
			FilesUtil.insertLineinFile("My Saved Places.loc",importFile);
			reloadMyPlaces();
			MapPlacesViewer.updateViewers();

		}else{
			checkFileStructure();
			//Call FilesUtil for removal
			FilesUtil.insertLineinFile("My Saved Places.loc",importFile);

			loadMyPlaces();
			reloadMyPlaces();
			MapPlacesViewer.updateViewers();
		}
		return importFile;
		}

	/**
	 * Adds the preset places from set server path.
	 * @see PathUtil
	 */
	protected void addPresetPlaces() {
		try {
			String pathURL =
				PathUtil.getPath("PLACES_PATH", "gma_places/");

			URL url = URLFactory.url( pathURL );
			loadPlaces(url, "all.loc", root, true);

			if ( map.getProjection() instanceof Mercator ) {
				loadPlaces(url, "grids.loc", root, true);
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	/**
	 * Load places from users home directory
	 * @see GMARoot
	 */
	protected void loadMyPlaces() {
		File gmaRoot = org.geomapapp.io.GMARoot.getRoot();
		if( gmaRoot==null )return;
		File file = new File(gmaRoot, "places");
		if( !file.exists() || !file.isDirectory() )return;
		try {
			loadPlaces( file.toURI().toURL(), "all.loc", root);
			} catch(Exception ex) {
		}
	}
	
	protected void reloadMyPlaces() {
		// Find My Saved Places
		MapPlace node = null;
		for (MapPlace child : root.children)
			if (child.name.equals("My Saved Places"))
			{	
				node = child;
				break;
			}

		if (node == null) return;
		root.children.remove(node);
		node = new MapPlace(root, "My Saved Places");
		
		File gmaRoot = org.geomapapp.io.GMARoot.getRoot();
		if( gmaRoot==null )return;
		File file = new File(gmaRoot, "places");
		if( !file.exists() || !file.isDirectory() )return;
		try {
			loadPlaces( file.toURI().toURL(), "My Saved Places.loc", node);
			} catch(Exception ex) {
		}
	}

	protected void loadPlaces(URL url,
								String file,
								MapPlace parent)
										throws IOException {
		loadPlaces(url, file, parent, true);
	}
	/**
	 * Load places from server path
	 * 
	 * @param url name of url path origin
	 * @param file name of file to load
	 * @param parent name of parent if exists or root
	 * @param addToMenu boolean value yes/no
	 * @throws IOException If an input or output exception occurred
	 */
	protected void loadPlaces(URL url,
								String file, 
								MapPlace parent,
								boolean addToMenu)
										throws IOException {
		URL url1 = URLFactory.url(url, file);
		BufferedReader in = new BufferedReader(
			new InputStreamReader(url1.openStream()));
		StringTokenizer st;
		String line;
		MapPlace place;
		while( (line=in.readLine()) != null) {
			st = new StringTokenizer(line, "\t");
			if( st.countTokens()==1 ) {
				int n = line.indexOf(".loc");
				String name = n>0 ?
					line.substring(0,n) : line;

				if ( !(map.getProjection() instanceof Mercator || map.getProjection() instanceof ConstrainedIdentityProjection)) {
					if ( name.toLowerCase().indexOf("world") == -1 && name.toLowerCase().indexOf("my ") == -1 ) {
						in.close();
						return;
					}
				}

				if( file.startsWith("grids") || parent instanceof HiResGrid) {
					place = new HiResGrid(parent, name);
				} else {
					place = new MapPlace(parent, name);
				}

				loadPlaces(url, line, place, addToMenu);
				continue;
			}
			if( parent instanceof HiResGrid) {
				place = new HiResGrid(
					parent,
					st.nextToken(),
					Double.parseDouble(st.nextToken()),
					Double.parseDouble(st.nextToken()),
					Integer.parseInt(st.nextToken()),
					new Rectangle( 
						Integer.parseInt(st.nextToken()),
						Integer.parseInt(st.nextToken()),
						Integer.parseInt(st.nextToken()),
						Integer.parseInt(st.nextToken())),
					new double[] {
						Double.parseDouble(st.nextToken()),
						Double.parseDouble(st.nextToken())
					}
				);
			} else {
				place = readPlace(parent, st);
			}
			locs.put( place.name, place);
		}
	}

	protected MapPlace readPlace(MapPlace parent, StringTokenizer st) {
		return new MapPlace(
				parent,
				st.nextToken(),
				Double.parseDouble(st.nextToken()),
				Double.parseDouble(st.nextToken()),
				Double.parseDouble(st.nextToken())
			);
	}

	void selectAll() {
		if(locList!=null) {
			locList.setSelectionInterval(
				0, locList.getModel().getSize()-1);
		}
	}
	void selectNone() {
		if(locList!=null) locList.clearSelection();
	}
	void editPlaces() {
	}
	void goTo(String cmd) {
		MapPlace loc = (MapPlace)locs.get(cmd);
		goTo(loc);
	}
	/**
	 * Scales to fit the requested place location on user request.
	 * @param loc location
	 */
	protected void goTo(MapPlace loc) {
		if (!loc.isLeaf()) return;

		Point2D.Double p = (Point2D.Double)(map.getProjection().getMapXY( 
				new Point2D.Double( loc.lon, loc.lat )));
		double z = map.getZoom();
		p.x *= z;
		p.y *= z;
		Insets insets = map.getInsets();
		p.x += insets.left;
		p.y += insets.top;
		double factor = loc.zoom/z;
		if( loc instanceof org.geomapapp.grid.HiResGrid) {
		//	System.out.println("high res");
			Rectangle rect = map.getVisibleRect();
			rect.width -= insets.left+insets.right;
			rect.height -= insets.top+insets.bottom;
			Rectangle r = ((org.geomapapp.grid.HiResGrid)loc).bounds;
			double s = Math.max( r.getWidth()/rect.width,
					r.getHeight()/rect.height);
			factor /= s;
			map.doZoom( p, factor );
			if (tools != null && tools.getGridDialog() != null) {
				GridDialog gd = tools.getGridDialog();
				Grid2DOverlay[] overlays = gd.getGrids();
				if( !gd.getToggle().isSelected() ) gd.getToggle().doClick();
				gd.gridCB.setSelectedItem( overlays[0]);
				gd.loadGrid();
			}
		//	tools.grid.doClick();
		} else  {
			map.doZoom( p, factor );
//			tools.focus.doClick();
			if (tools != null)
				tools.getApp().autoFocus();
		}
	}
	
	/**
	 * Draws bounding box of bookmarked locations
	 */
	public void draw(Graphics2D g) {
		if( !showLoc.isSelected() )return;
		double zoom = map.getZoom();
		g.setStroke(new BasicStroke(1f/(float)zoom));
		Rectangle rect = map.getVisibleRect();
		Dimension r1 = map.getParent().getSize();
		Enumeration e = locs.elements();
		while( e.hasMoreElements()) {
			MapPlace loc = (MapPlace)e.nextElement();
			loc.draw(g, map);
		}
	}
	public MapPlace select( Point pt ) {
		Point2D p = map.getScaledPoint(pt);
		Enumeration e = locs.elements();
		MapPlace select = null;
		while( e.hasMoreElements()) {
			MapPlace loc = (MapPlace)e.nextElement();
			if( loc.zoom<2.)continue;
			if( loc.select(map, p) ) {
				if( select==null ) {
					select = loc;
				} else {
					if( loc.zoom>select.zoom ) {
						select.unselect(map);
						select=loc;
					}
				}
			}
		}
		return select;
	}

	public void zoomTo( Point pt ) {
		Point2D p = map.getScaledPoint(pt);
		MapPlace select = select( pt );
		if( select==null )return;
		goTo(select);
	}
	/**
	 * Calls viewer for browsing bookmarks
	 */

	public void browseBookmarks() {
		if (mapPlacesViewer == null)
			mapPlacesViewer = new MapPlacesViewer(this, root, true);
			mapPlacesViewer.bookmarkFrame.setVisible(false);
			mapPlacesViewer.bookmarkFrame.setVisible(true);

	}

	public static void main(String[] args) throws Exception {
		MapPlaces m = new MapPlaces(null, null);
		m.browseBookmarks();
	}

	public static class MapPlacesViewer {
		public static List<MapPlacesViewer> mapViewers
			= new LinkedList<MapPlacesViewer>();

		public static void addViewer(MapPlacesViewer viewer) {
			mapViewers.add(viewer);
		}

		public static void updateViewers() {
			SwingUtilities.invokeLater( new Runnable() {
				public void run() {
					for (MapPlacesViewer viewer : mapViewers) {
						viewer.rootNode = createTreeNodes(viewer.rootPlace);
						viewer.treeModel.setRoot( viewer.rootNode );
					}
				}
			});
		}

		protected DefaultTreeModel treeModel;
		protected JTree bookmarkTree;
		protected JFrame bookmarkFrame;
		protected JTextField searchBar;

		protected int initialXPos;
		protected int initialYPos;
		protected int currentXPos = -1;
		protected int currentYPos = -1;
		protected DefaultMutableTreeNode tearOffNode;
		protected DefaultMutableTreeNode rootNode;

		static final String okButtonCmd = "ok_button_cmd";
		static final String searchBarCmd = "search_bar_cmd";
		static final String searchLabelText = "Search: ";
		//static final String organizeLabelText = "Organize Your Bookmarks: ";
		static final String searchBarText = "Enter bookmark search term";
		static final String frameTitle = "Bookmarks";

		protected MapPlaces places;
		protected MapPlace rootPlace;
		protected boolean topFrame;

		public MapPlacesViewer(MapPlaces places, MapPlace root, boolean topFrame) {
			this(places, root, topFrame, 0, 0);
		}

		public MapPlacesViewer(MapPlaces places,
				MapPlace root,
				boolean topFrame,
				int xPos, int yPos) {
			this.places = places;
			this.topFrame = topFrame;

			this.rootPlace = root;
			this.rootNode = createTreeNodes(root);

			treeModel= new DefaultTreeModel(rootNode);
			bookmarkTree = new JTree(treeModel);

			this.initialXPos = xPos;
			this.initialYPos = yPos;

			initBookmarkFrame();
			MapPlacesViewer.mapViewers.add(this);
		}

		private static DefaultMutableTreeNode createTreeNodes(MapPlace root) {
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(root);

			for (MapPlace child : root.children) 
				node.add( createTreeNodes(child) );
			return node;
		}

		/**
		 * Creates Browse Bookmark section. Displays the tree set of places.
		 * Lets users search or view saved bookmark location. Search is
		 * highlighted before and after each entry for added convenience. 
		 */
		private void initBookmarkFrame() {
			bookmarkFrame = new JFrame(rootNode.toString());
			bookmarkFrame.addWindowListener(new WindowAdapter() {
				public void windowClosed(WindowEvent e) {
					MapPlacesViewer.mapViewers.remove(MapPlacesViewer.this);
				}
			});
			bookmarkFrame.setDefaultCloseOperation(
					topFrame ? JFrame.HIDE_ON_CLOSE : JFrame.DISPOSE_ON_CLOSE);

			JLabel searchLabel = new JLabel(searchLabelText);
			//JLabel organizeLabel = new JLabel(organizeLabelText);
			JPanel contentPane = new JPanel( new BorderLayout( 10, 10 ));
			Border emptyBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
			contentPane.setBorder( emptyBorder );
			JPanel searchPane = new JPanel( new BorderLayout( 5, 10 ) );
			JPanel buttonPane = new JPanel( new BorderLayout( 5, 5 ) );
			JPanel buttonPane2 = new JPanel( new BorderLayout( 3, 3 ) );
			JPanel buttonPane3 = new JPanel( new BorderLayout( 3, 3 ) );
			JButton okButton = new JButton("Go To");
			okButton.setToolTipText("Go to any selected bookmark to view.");
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) 
						bookmarkTree.getLastSelectedPathComponent();

					if (!(node.getUserObject() instanceof MapPlace)) return;
					MapPlace place = (MapPlace) node.getUserObject();
					places.goTo(place);
				}
			});

			JButton deleteButton = new JButton("Delete Bookmark(s)");
			deleteButton.setToolTipText("Delete selected bookmark(s) from 'My Saved Places'");
			deleteButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					TreePath[] currentSelection = bookmarkTree.getSelectionPaths();
				//System.out.println("Selected "+ currentSelection);
					if (currentSelection != null) {
						DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) 
						bookmarkTree.getLastSelectedPathComponent();
						if (!(currentNode.getUserObject() instanceof MapPlace)) return;

						MapPlace place = (MapPlace) currentNode.getUserObject();
						MutableTreeNode parent = (MutableTreeNode)(currentNode.getParent());

						DefaultMutableTreeNode nodeParent = (DefaultMutableTreeNode)
															currentNode.getParent();
					if(nodeParent==null)return;
						String p = "My Saved Places", np = nodeParent.toString();
						if(np.equals(p)){
							try{
								int i=0, j;
								j = currentSelection.length;
							 for (i=0; i<j; i++ ){
								DefaultMutableTreeNode currentNodes = (DefaultMutableTreeNode) 
								currentSelection[i].getLastPathComponent();
								MapPlace placeNodes = (MapPlace) currentNodes.getUserObject();
								places.deleteBookmarks(placeNodes);
								treeModel.removeNodeFromParent(currentNodes);
							 }
							}finally{}
						}
					}
					if (currentSelection == null) {
						return;
					}
				}
			});

			/* Save button checks user selection is only in MyPlaces branch
		 	* Prompts user to save file in .loc format
		 	* Calls savePlace method to gather location info and saves to file 
		 	*/
			JButton saveAsButton = new JButton("Export Bookmark(s)");
			saveAsButton.setToolTipText("Export bookmark(s)from 'My Saved Places'.");
			saveAsButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					TreePath[] currentSelection = bookmarkTree.getSelectionPaths();
					//Looks at user's current selection
					if (currentSelection != null) {
						DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) 
						bookmarkTree.getLastSelectedPathComponent();  
						System.out.println("current node " + currentNode);
						if (!(currentNode.getUserObject() instanceof MapPlace)) return;
						MapPlace place = (MapPlace) currentNode.getUserObject();
						MutableTreeNode parent = (MutableTreeNode)(currentNode.getParent());

						//Looks at current selection's parent.
						DefaultMutableTreeNode nodeParent = (DefaultMutableTreeNode) 
						currentNode.getParent();
						if(nodeParent==null)return;

						String p = "My Saved Places", 
								np = nodeParent.toString(), 
								fileName = System.getProperty("user.name")+ 
										"'s_Places_" + FilesUtil.fileTimeEST().replace( ':', '-' ),
								bookMarks = System.getProperty("user.name")+ 
										"'s_Places_" + FilesUtil.fileTimeEST();

						//Exports only if selected node is in a certain directory 
						if(np.equals(p)){
							JFileChooser bookmarkExport = new JFileChooser
							(System.getProperty("user.home") + "/Desktop");
							bookmarkExport.setSelectedFile( new File(
									bookmarkExport.getCurrentDirectory(),
									fileName + ".loc"));
							bookmarkExport.setControlButtonsAreShown(true);
							int bmx = bookmarkExport.showSaveDialog(map.getTopLevelAncestor());
							if( bmx==JFileChooser.CANCEL_OPTION )return;
							locFile = bookmarkExport.getSelectedFile();
							//System.out.println("locfile" + locFile);
						
							// Same file name prompts user to proceed with replace dialog
							if(locFile.exists()){
							int overwriteReturnValue = JOptionPane.showConfirmDialog(MapApp.anchor,   
									locFile.getName().toString()
									+ " already exists. "   
									+ "Do you want to replace it?",   
								"File Already Exists",   
								 JOptionPane.YES_NO_OPTION,   
								JOptionPane.WARNING_MESSAGE);
							if( (overwriteReturnValue==JFileChooser.CANCEL_OPTION) ||
									(overwriteReturnValue==JFileChooser.ERROR_OPTION) )return;
							}

							//Save if current filename doesn't exist
							save.setText("save to "+locFile.getName());
								try {
									int i=0, j = currentSelection.length;
									PrintStream out = new PrintStream(
										new FileOutputStream(locFile, false));
								 for (i=0; i<j; i++ ){
									DefaultMutableTreeNode currentNodes = (DefaultMutableTreeNode) 
									currentSelection[i].getLastPathComponent();
									MapPlace placeNodes = (MapPlace) currentNodes.getUserObject();
								out.println(bookMarks + ": "+ places.saveBookmarks(placeNodes));
						//System.out.print("Selected  " + currentNodes);
								 }
								 out.close();
							} catch(IOException ex) {}}
					}	
					if (currentSelection == null) {
						return;
					}
				}	
			
			});

			JButton importButton = new JButton("Import Bookmark");
			importButton.setToolTipText("Imports a bookmark file in .loc format from your desktop");
			importButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					// Before we begin check if files are there

					JFileChooser bookmarkImport = new JFileChooser
					(System.getProperty("user.home") + "/Desktop");//haxby.map.MapApp.getFileChooser();
					bookmarkImport.setDialogTitle("Import Bookmarks");
					bookmarkImport.setAcceptAllFileFilterUsed(true);
					// Set filter to display only .loc files
					bookmarkImport.setFileFilter(new FileFilter() {
						public boolean accept(File file) {
							String fileName = file.getName().toLowerCase();
							if (fileName.endsWith(".loc")) {
								return true;
								}
							return false;
							}

						public String getDescription() {
							return "Bookmark file (*.loc)";
						}
					});
					bookmarkImport.setSelectedFile( new File(
							bookmarkImport.getCurrentDirectory(),
							".loc"));
					bookmarkImport.setFileSelectionMode(JFileChooser.FILES_ONLY);
					int result = bookmarkImport.showOpenDialog(map.getTopLevelAncestor());
					
					if(result==JFileChooser.CANCEL_OPTION){
						return;
					}else if(result==JFileChooser.APPROVE_OPTION){
						
						File f = bookmarkImport.getSelectedFile();
						String fs = f.toString();
						places.importBookmarks(fs);
					}
				}
			});

			searchBar = new JTextField(20);
			searchBar.setText(searchBarText);
			searchBar.selectAll();
			searchBar.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					bookmarkTree.getSelectionModel().clearSelection();
					closeAllPaths(bookmarkTree);
					search(searchBar.getText(), rootNode);
					searchBar.selectAll();
				}
				
			});

			// Bookmarked Places checkbox in Browse menu synchs with Main menu checkbox

			showBookmarkedPlaces.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					if (showLoc == null) return;
					
					if(showBookmarkedPlaces.isSelected())
					{
						showLoc.setSelected(true);
						MapApp.showPlaces();
					}else{
						showLoc.setSelected(false);
						MapApp.showPlaces();
					}
				}
			});

			//showBookmarkedPlaces.addActionListener((ActionListener) this);
			
			bookmarkTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
			bookmarkTree.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2)
					{
						DefaultMutableTreeNode node = (DefaultMutableTreeNode) 
						bookmarkTree.getLastSelectedPathComponent();
						if (!(node.getUserObject() instanceof MapPlace)) return;
						
						MapPlace place = (MapPlace) node.getUserObject();
						places.goTo(place);
					}
				}

				public void mousePressed(MouseEvent e) {
					int xOff = ((JTree)e.getSource()).getLocationOnScreen().x;
					int yOff = ((JTree)e.getSource()).getLocationOnScreen().y;
					initialXPos = e.getX()+xOff;
					initialYPos = e.getY()+yOff;
				}
				/**
				 * Dragging from a parent or child node and releasing 
				 * produces a teared off view of the tree.
				 */
				public void mouseReleased(MouseEvent e) {
					int xOff = ((JTree)e.getSource()).getLocationOnScreen().x;
					int yOff = ((JTree)e.getSource()).getLocationOnScreen().y;
					currentXPos = e.getX()+xOff;
					currentYPos = e.getY()+yOff;
					if ( e.getSource().equals(bookmarkTree) && 
							( Math.abs(currentXPos - initialXPos) + Math.abs(currentYPos - initialYPos) ) > 50 ) {
						if (tearOffNode != null) {
							tearOffSearchTree();
							tearOffNode = null;
						}
					}
				}
			});
			bookmarkTree.addMouseMotionListener(new MouseMotionAdapter() {
				public void mouseDragged(MouseEvent e) {
//					if ( e.getClickCount() == 0 & e.getSource().equals(tree) ) {
					if ( e.getSource().equals(bookmarkTree) ) {
						tearOffNode = (DefaultMutableTreeNode)bookmarkTree.getLastSelectedPathComponent();
					}
				}
			});
			JScrollPane treeView = new JScrollPane(bookmarkTree);
			contentPane.add(buttonPane, "South");
				buttonPane.add(buttonPane3, "South");
					buttonPane3.add(okButton, "East");
					buttonPane3.add(showBookmarkedPlaces,"West");
			contentPane.add(searchPane, "North");
				searchPane.add(searchLabel, "West");
				searchPane.add(searchBar, "Center");
				searchPane.add(buttonPane2,"North");
					//buttonPane2.add(organizeLabel,"North");
					buttonPane2.add(importButton, "West");
					buttonPane2.add(saveAsButton, "Center");
					buttonPane2.add(deleteButton, "East");
			contentPane.add(treeView, "Center");
			contentPane.setOpaque(true);

			bookmarkFrame.setContentPane(contentPane);
			bookmarkFrame.pack();
			if ( initialXPos != -1 && initialYPos != -1 ) {
				bookmarkFrame.setLocation(initialXPos, initialYPos);
			}
			bookmarkFrame.setVisible(true);
		}

		public void search( String query, DefaultMutableTreeNode root) {
			for (Enumeration e = root.children(); e.hasMoreElements() ;) {
				 DefaultMutableTreeNode child;
				 child = (DefaultMutableTreeNode)e.nextElement();
				 String tempChildString = child.toString().toLowerCase();
				 String tempQuery = query.toLowerCase();
				 if ( tempChildString.indexOf(tempQuery) != -1 ) {
					 bookmarkTree.expandPath( new TreePath( ((DefaultMutableTreeNode) child.getParent()).getPath() ) );
					 bookmarkTree.getSelectionModel().addSelectionPath(new TreePath( child.getPath() ) );
				 }
				 search(query, child);
			 }
		}

		public void closeAllPaths(JTree tree) {
			TreePath pathToRoot = new TreePath(tree.getModel().getRoot());
			closePaths(tree, pathToRoot);
			if (!tree.isRootVisible())
				tree.expandPath(pathToRoot);
		}

		public void closePaths(JTree tree, TreePath path) {
			Object node = path.getLastPathComponent();
			TreeModel model = tree.getModel();
			if (model.isLeaf(node))
				return;
			int num = model.getChildCount(node);
			for (int i = 0; i < num; i++)
				closePaths(tree, path.pathByAddingChild(model.getChild(node, i)));
			tree.collapsePath(path);
		}

		protected void tearOffSearchTree() {
			new MapPlacesViewer(places, 
					(MapPlace) tearOffNode.getUserObject(),
					false, 
					currentXPos, 
					currentYPos);
		}
	}
}
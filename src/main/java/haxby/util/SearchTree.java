package haxby.util;

import haxby.map.MapApp;
import haxby.map.Overlay;
import haxby.map.XMap;
import haxby.proj.Projection;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.ParserConfigurationException;

import org.geomapapp.util.XML_Menu;
import org.xml.sax.SAXException;

public class SearchTree implements 	ActionListener,
									MouseListener,
									MouseMotionListener {

	SearchTreeOverlay overlay;
	JCheckBox showPlaces;
	JFrame searchTreeFrame;
	protected JTree tree;
	JTextField searchBar;
	protected MapApp mapApp;
	String searchURL = null;
	XML_Menu searchMenu = null;
	DefaultMutableTreeNode rootNode = null;
	DefaultMutableTreeNode tearOffNode = null;
	static final String showPlacesCmd = "show_search_tree_places";
	static final String okButtonCmd = "ok_button_cmd";
	static final String searchBarCmd = "search_bar_cmd";
	static final String searchLabelText = "Search: ";
	static final String searchBarText = "Enter keyword for requested data type";
	int initialXPos = -1;
	int initialYPos = -1;
	int currentXPos = -1;
	int currentYPos = -1;

	public static SearchTreeOverlay currentOverlay;
	public static JCheckBox currentShowPlacesCB;

	public void removeCurrentOverlay(XMap map) {
		if (currentOverlay != null ) {
			if (map.hasOverlay(currentOverlay)) {
				map.removeOverlay(currentOverlay);
				map.removeMouseListener(currentOverlay);
				map.repaint();
			}
			currentOverlay = null;
		}

		if (currentShowPlacesCB != null) {
			currentShowPlacesCB.setSelected(false);
			currentShowPlacesCB = null;
		}
	}

	public void makeOverlayCurrent(SearchTreeOverlay overlay, JCheckBox placesCB, XMap map) {
		removeCurrentOverlay(map);

		map.addOverlay(overlay, false);
		map.addMouseListener(overlay);
		placesCB.setSelected(true);

		map.repaint();

		currentOverlay = overlay;
		currentShowPlacesCB = placesCB;
	}

	public SearchTree(String searchURL) {
		this.searchURL = searchURL;
		initGUI();
	}

	public SearchTree(XML_Menu searchMenu) {
		this.searchMenu = searchMenu;
		initGUI();
	}

	public SearchTree(DefaultMutableTreeNode rootNode) {
		this.rootNode = rootNode;
		initGUI();
	}

	public SearchTree(DefaultMutableTreeNode rootNode, int xpos, int ypos) {
		this.rootNode = rootNode;
		this.initialXPos = xpos;
		this.initialYPos = ypos;
		initGUI();
	}

	private void initGUI() {
		searchTreeFrame = new JFrame();
		searchTreeFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		JLabel searchLabel = new JLabel(searchLabelText);
		JPanel contentPane = new JPanel( new BorderLayout( 10, 10 ) );
		Border emptyBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		contentPane.setBorder( emptyBorder );
		JPanel searchPane = new JPanel( new BorderLayout( 5, 5 ) );
		JButton okButton = new JButton("OK");
		okButton.setActionCommand(okButtonCmd);
		okButton.addActionListener(this);
		searchBar = new JTextField(20);
		searchBar.setText(searchBarText);
		searchBar.selectAll();
		searchBar.setActionCommand(searchBarCmd);
		searchBar.addActionListener(this);
		if ( searchURL != null ) {
			loadURL(searchURL);
		} else if ( rootNode != null ) {
			tree = new JTree(rootNode);
			searchTreeFrame.setTitle(rootNode.toString());
		} else if ( searchMenu != null) {
			if (searchMenu.search_url != null) {
				loadURL(searchMenu.search_url);
				searchTreeFrame.setTitle(searchMenu.name);
			} else {
				tree = XML_Menu.createTree(searchMenu.parent.child_layers);
				searchTreeFrame.setTitle(searchMenu.parent.name);
			}
		}
		showPlaces = new JCheckBox("Show On Map");
		showPlaces.setActionCommand(showPlacesCmd);
		showPlaces.setSelected(false);
		showPlaces.addActionListener(this);

		rootNode = (DefaultMutableTreeNode) tree.getModel().getRoot();

		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		tree.addMouseListener(this);
		tree.addMouseMotionListener(this);
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				if (overlay == null) return;
				if (!mapApp.getMap().hasOverlay(overlay)) return;
				mapApp.getMap().repaint();
			}
		});
		JScrollPane treeView = new JScrollPane(tree);
		contentPane.add(treeView, "Center");
		contentPane.add(okButton, "South");
		searchPane.add(showPlaces, "South");
		searchPane.add(searchLabel, "West");
		searchPane.add(searchBar, "Center");
		contentPane.add(searchPane, "North");
		contentPane.setOpaque(true);
		searchTreeFrame.setContentPane(contentPane);
		searchTreeFrame.pack();
		if ( initialXPos != -1 && initialYPos != -1 ) {
			searchTreeFrame.setLocation(initialXPos, initialYPos);
		}

		searchTreeFrame.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				// Remove on close.
				if (showPlaces.isSelected()) {
					removeCurrentOverlay(mapApp.getMap());
				}

				// if the window closing event is due to switching
				// the projection, then finish here, otherwise go 
				// ahead and select the menu item of the closed 
				// search tree
				if (mapApp.switchingProjection) return;
				
				JMenuBar enableMenus = MapApp.getMenuBar();
				for (int i = 0; i < enableMenus.getMenuCount(); i++) {
					JMenu menuB = enableMenus.getMenu(i);
					final JMenu baseMenu = enableMenus.getMenu(i);
					if(enableMenus.getMenu(i).getText().contentEquals("Basemaps") &&
							searchTreeFrame.getTitle().contentEquals("Basemaps")) {
						baseMenu.setEnabled(true);
						menuB.doClick(1);
					}
					if(enableMenus.getMenu(i).getText().contentEquals("DataLayers") &&
							searchTreeFrame.getTitle().contentEquals("DataLayers")) {
						baseMenu.setEnabled(true);
						menuB.doClick(1);
						
					}
					if(enableMenus.getMenu(i).getText().contentEquals("Portals") &&
							searchTreeFrame.getTitle().contentEquals("Portals")) {
						baseMenu.setEnabled(true);
						menuB.doClick(1);
						
					} 
					if(enableMenus.getMenu(i).getText().contentEquals("Datasets") &&
							searchTreeFrame.getTitle().contentEquals("Datasets")) {
						baseMenu.setEnabled(true);
						menuB.doClick(1);
						
					}
					if(enableMenus.getMenu(i).getText().contentEquals("FocusSites") &&
							searchTreeFrame.getTitle().contentEquals("FocusSites")) {
						baseMenu.setEnabled(true);
						menuB.doClick(1);
						
					} 
					if(enableMenus.getMenu(i).getText().contentEquals("Overlays") &&
							searchTreeFrame.getTitle().contentEquals("Overlays")) {
						baseMenu.setEnabled(true);
						menuB.doClick(1);
						
					}

				}
			}

			public void windowOpen(WindowEvent e) {
				if(searchTreeFrame.getTitle().contentEquals("Basemaps")) {
					searchTreeFrame.toFront();
				}
			}
		});
		searchTreeFrame.setVisible(true);
		searchTreeFrame.toFront();
	}

	private void loadURL(String searchURL) {
		try {
			tree = XML_Menu.createTree(XML_Menu.parse(searchURL));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
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

	public void setMapApp(MapApp mapApp) {
		this.mapApp = mapApp;
		searchTreeFrame.setLocation(mapApp.getFrame().getLocation());
	}

	public SearchTree tearOffSearchTree(DefaultMutableTreeNode rootNode, int xpos, int ypos) {
		SearchTree tearOffTree = createSearchTree(rootNode, xpos, ypos);
		if ( this.mapApp != null ) {
			tearOffTree.setMapApp(this.mapApp);
		}
		return tearOffTree;
	}

	protected SearchTree createSearchTree(DefaultMutableTreeNode rootNode,
			int xpos, int ypos) {
		return new SearchTree(rootNode, xpos, ypos);
	}

	public void search( String query, DefaultMutableTreeNode root) {
		for (Enumeration e = root.children(); e.hasMoreElements() ;) {
			 DefaultMutableTreeNode child;
			 child = (DefaultMutableTreeNode)e.nextElement();
			 String tempChildString = child.toString().toLowerCase();
			 String tempQuery = query.toLowerCase();
			 if ( tempChildString.indexOf(tempQuery) != -1 ) {
				Enumeration<?> pathFrom = child.pathFromAncestorEnumeration(rootNode);
				List<Object> pathL = new LinkedList<Object>();
				while (pathFrom.hasMoreElements()) 
					pathL.add(pathFrom.nextElement());
				TreePath treePath = new TreePath(pathL.toArray());

				tree.getSelectionModel().addSelectionPath(treePath);
				tree.expandPath( treePath );
				tree.scrollPathToVisible(treePath);
			 }
			 search(query, child);
		 }
	}

	public static void main(String args[]) {
		SearchTree searchTree = new SearchTree(MapApp.BASE_URL+"/gma_menus/basemaps_menu.xml");
	}

	private void ok() {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
		if (node == null) {
			return;
		}
		else if ( mapApp != null && ((XML_Menu)node.getUserObject()).command != null ) {
			XML_Menu menu = (XML_Menu) node.getUserObject();
			JMenuItem mi = XML_Menu.getMenuItem(menu);

			if (mi != null)
				mi.doClick();
			else {
				XML_Menu.createMenuItem(menu).doClick();
			}
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		
		if ( command.equals(okButtonCmd) ) {
			ok();
		}
		
		else if ( command.equals(searchBarCmd) ) {
			tree.getSelectionModel().clearSelection();
			closeAllPaths(tree);
			search(searchBar.getText(), (DefaultMutableTreeNode)tree.getModel().getRoot());
		}

		else if ( command.equals(showPlacesCmd) ) {
			setShowPlaces(showPlaces.isSelected());
		}
	}

	public void setShowPlaces(boolean tf) {
		XMap map = mapApp.getMap();

		if (tf) {
			if (overlay == null) overlay = createSearchTreeOverlay();
			
			makeOverlayCurrent(overlay, showPlaces, map);
			
		} else {
			removeCurrentOverlay(map);
		}
		map.repaint();
	}

	protected SearchTreeOverlay createSearchTreeOverlay() {
		return new SearchTreeOverlay();
	}

	public void mouseDragged(MouseEvent e) {
//		if ( e.getClickCount() == 0 & e.getSource().equals(tree) ) {
		if ( e.getSource().equals(tree) ) {
			tearOffNode = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
		}
	}

	public void mouseMoved(MouseEvent e) {
	}

	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2)
			ok();
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		int xOff = ((JTree)e.getSource()).getLocationOnScreen().x;
		int yOff = ((JTree)e.getSource()).getLocationOnScreen().y;
		initialXPos = e.getX()+xOff;
		initialYPos = e.getY()+yOff;
	}

	public void mouseReleased(MouseEvent e) {
		int xOff = ((JTree)e.getSource()).getLocationOnScreen().x;
		int yOff = ((JTree)e.getSource()).getLocationOnScreen().y;
		this.currentXPos = e.getX()+xOff;
		this.currentYPos = e.getY()+yOff;
		if ( e.getSource().equals(tree) && ( Math.abs(currentXPos - initialXPos) + Math.abs(currentYPos - initialYPos) ) > 50 ) {
			if (tearOffNode != null) {
				tearOffSearchTree(tearOffNode, currentXPos, currentYPos);
				tearOffNode = null;
			}
		}
	}
	
	public class SearchTreeOverlay extends MouseAdapter implements Overlay {
		protected List<SearchTreeItem> orderedList 
			= new ArrayList<SearchTreeItem>();
		
		protected Font font = new Font("SansSerif", Font.PLAIN, 10);
		protected int dx, dy;
		
		public SearchTreeOverlay() {
			DefaultMutableTreeNode rootNode = 
				(DefaultMutableTreeNode) tree.getModel().getRoot();
			Enumeration<?> nodes = rootNode.depthFirstEnumeration();
			while (nodes.hasMoreElements()) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
				if (!node.isLeaf()) continue;
				
				XML_Menu obj = (XML_Menu) node.getUserObject();
				if (obj == null) continue;
				if (obj.wesn == null) continue;
				String[] wesnS = obj.wesn.split(",");
				if (wesnS.length != 4) continue;
				
				float[] wesn = new float[4];
				try {
				for (int i = 0; i < wesn.length; i++)
				{
					wesn[i] = Float.parseFloat(wesnS[i]);
				}
				} catch (NumberFormatException ex) { continue; }
				
				XMap map = mapApp.getMap();
				Projection proj = map.getProjection();
				double wrap = map.getWrap();
				
				Point2D[] pts = new Point2D[] {
						proj.getMapXY(wesn[0],wesn[3]),
						proj.getMapXY(wesn[1],wesn[3]),
						proj.getMapXY(wesn[0],wesn[2]),
						proj.getMapXY(wesn[1],wesn[2])};

				if (wrap > 0) {
					while (pts[0].getX() > pts[1].getX())
						pts[0].setLocation(pts[0].getX() - wrap, pts[0].getY());
					
					while (pts[2].getX() > pts[3].getX())
						pts[2].setLocation(pts[2].getX() - wrap, pts[2].getY());
				}

				double minX, minY;
				double maxX, maxY;
				minX = minY = Double.MAX_VALUE;
				maxX = maxY = -Double.MAX_VALUE;
				for (int i = 0; i < pts.length; i++) {
					minX = Math.min(minX, pts[i].getX());
					minY = Math.min(minY, pts[i].getY());
					maxX = Math.max(maxX, pts[i].getX());
					maxY = Math.max(maxY, pts[i].getY());
				}

				while (minX < 0 && wrap > 0) {
					minX += wrap;
					maxX += wrap;
				}

				Rectangle2D.Double bounds = new Rectangle2D.Double();

				bounds.x = minX;
				bounds.y = minY;

				bounds.width = maxX - minX;
				bounds.height = maxY - minY;

				if (Double.isNaN(bounds.width) ||
						Double.isNaN(bounds.height) ||
						Double.isNaN(bounds.x) ||
						Double.isNaN(bounds.y) ||
						Double.isInfinite(bounds.width) ||
						Double.isInfinite(bounds.height) ||
						Double.isInfinite(bounds.x) ||
						Double.isInfinite(bounds.y)) {
					continue;
				}

				if (bounds.width * bounds.height == 0) {
					continue;
				}

				if (wrap > 0) {
					// The plus 20 is to catch less than boundary cases.
					// We don't want to show Global Grids
					if (bounds.width + 20 > wrap) continue;
				}

				SearchTreeItem sti = new SearchTreeItem(bounds, node);
				orderedList.add(sti);
			}

			Collections.sort(orderedList, new Comparator<SearchTreeItem>() {
				public int compare(SearchTreeItem o1, SearchTreeItem o2) {
					double a1 = o1.bounds.width * o1.bounds.height;
					double a2 = o2.bounds.width * o2.bounds.height;
					if (a1 > a2)
						return 1;
					else if (a1 < a2)
						return -1;
					else 
						return 0;
				}
			});
		}

		public void draw(Graphics2D g) {
			List<SearchTreeItem> selection = new LinkedList<SearchTreeItem>();

			for (SearchTreeItem sti : orderedList) {
				if (isNodeSelected(sti.node)) {
					selection.add(sti);
					continue;
				}
				draw(g, sti, false);
			}
			
			for (SearchTreeItem sti : selection) {
				draw(g, sti, true);
			}
		}

		private boolean isNodeSelected(DefaultMutableTreeNode node) {
			Enumeration path = node.pathFromAncestorEnumeration(
					(TreeNode) tree.getModel().getRoot());
			List<Object> pathL = new LinkedList<Object>();
			while (path.hasMoreElements())
				pathL.add(path.nextElement());

			return tree.getSelectionModel().isPathSelected( 
					new TreePath( pathL.toArray() ));
		}

		public void draw(Graphics2D g, SearchTreeItem sti, boolean selected) {
			XML_Menu obj = (XML_Menu) sti.node.getUserObject();
			if (obj == null) return;

			XMap map = mapApp.getMap();
			double wrap = map.getWrap();
			double zoom = map.getZoom();

			Rectangle2D.Double bounds = sti.bounds;

			Rectangle2D.Double r = (Rectangle2D.Double)map.getClipRect2D();
			g.setStroke( new BasicStroke(1f/(float) zoom));
			float size = 9f/(float) zoom;
			Font dFont = font.deriveFont(size );
			g.setFont( dFont );

			if( wrap>0 ) {
				while( bounds.x>r.x)
					bounds.x-=wrap;
				while( bounds.x+bounds.width<r.x)
					bounds.x+=wrap;
				while( bounds.x<r.x+r.width) {
					g.setColor( selected ? Color.white : Color.black );
					g.draw(bounds);

					if (selected) {
						float x = .1f*size+(float)(bounds.x);
						float y = -.2f*size+(float)(bounds.y);

						g.drawString(obj.name, x, y);

						TextLayout tl = new TextLayout(obj.name, dFont, g.getFontRenderContext());

						g.setColor(Color.white);

						double border = 2 / zoom;

						Rectangle2D stringBounds = tl.getBounds();
						stringBounds.setRect(
								x + stringBounds.getX() - border, 
								y + stringBounds.getY() - border, 
								stringBounds.getWidth() + 2 * border,
								stringBounds.getHeight() + 2 * border);
						g.fill(stringBounds);

						g.setColor(Color.black);

						g.draw(stringBounds);
						g.drawString(obj.name, x, y);
					}
					bounds.x+=wrap;
				}
			} else {
				g.setColor( selected ? Color.white : Color.black );
				g.draw(bounds);
				
				if (selected) {
					float x = .1f*size+(float)(bounds.x);
					float y = -.2f*size+(float)(bounds.y);

//					g.drawString(obj.name, x, y);

					TextLayout tl = new TextLayout(obj.name, dFont, g.getFontRenderContext());

					g.setColor(Color.white);

					double border = 2 / zoom;

					Rectangle2D stringBounds = tl.getBounds();
					stringBounds.setRect(
							x + stringBounds.getX() - border, 
							y + stringBounds.getY() - border, 
							stringBounds.getWidth() + 2 * border,
							stringBounds.getHeight() + 2 * border);
					g.fill(stringBounds);

					g.setColor(Color.black);
					g.draw(stringBounds);
					g.drawString(obj.name, x, y);
				}
			}
		}

		public void mouseClicked(MouseEvent e) {
			if (e.isConsumed() || e.isControlDown())
				return;

			if (e.getButton() == MouseEvent.BUTTON3 || (System.getProperty("os.name").toLowerCase().contains("mac") && e.isControlDown()) ) {
				ok();
				return;
			}

			XMap map = mapApp.getMap();
			Point2D point = map.getScaledPoint( e.getPoint() );
			double wrap = map.getWrap();
			if (wrap > 0)
				while (point.getX() > wrap)
					point.setLocation(point.getX() - wrap, point.getY());

			int startI = -1;

			if (tree.getSelectionPath() != null) {
				DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) 
					tree.getSelectionPath().getLastPathComponent();

				if (selectedNode.isLeaf()) {
					int j = 0;
					for (SearchTreeItem sti : orderedList) {
						if (sti.node == selectedNode) {
							if (select(sti.bounds, point))
								startI = j;
							break;
						} 
						j++;
					}
				}
			}
			startI++;

			DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) tree.getModel().getRoot();
			
			for (int i = 0; i < orderedList.size(); i++) {
				SearchTreeItem sti = orderedList.get((startI + i) % orderedList.size());
				
				if (select(sti.bounds, point)) {
					Enumeration<?> pathFrom = sti.node.pathFromAncestorEnumeration(rootNode);
					List<Object> pathL = new LinkedList<Object>();
					while (pathFrom.hasMoreElements()) 
						pathL.add(pathFrom.nextElement());
					TreePath treePath = new TreePath(pathL.toArray());

					tree.clearSelection();
					tree.getSelectionModel().setSelectionPath(treePath);
					tree.scrollPathToVisible(treePath);
					map.repaint();

					e.consume();
					return;
				}
			}
			
			if (startI != 0) return;

			tree.getSelectionModel().clearSelection();
			map.repaint();
		}

		private boolean select(Rectangle2D.Double bounds, Point2D point) {
			if (bounds == null) return false;

			XMap map = mapApp.getMap();
			double wrap = map.getWrap();

			if (wrap > 0) {
				// The plus 20 is to catch less than boundary cases.
				// We don't want to show Global Grids
				if (bounds.width + 20 > wrap) return false;
			} else if (mapApp.getMapType() != MapApp.MERCATOR_MAP) {
				if (bounds.width >= 640 &&
						bounds.height >= 640)
					return false;
			}

			Rectangle2D.Double r = (Rectangle2D.Double) map.getClipRect2D();
			if (wrap > 0)
				while (r.x > wrap)
					r.x -= wrap;

			if (bounds.contains(point)) {
				if (bounds.x < r.x &&
						bounds.y < r.y &&
						bounds.getMaxX() > r.getMaxX() &&
						bounds.getMaxY() > r.getMaxY())
					return false;
				return true;
			}
			while (bounds.x + bounds.width > wrap && wrap > 0) {
				
				bounds.x -= wrap;
				if (bounds.contains(point)) {
					if (bounds.x < r.x &&
							bounds.y < r.y &&
							bounds.getMaxX() > r.getMaxX() &&
							bounds.getMaxY() > r.getMaxY())
						return false;
					return true;
				}
			}
			return false;
		}
	}

	public static class SearchTreeItem {
		public Rectangle2D.Double bounds;
		public DefaultMutableTreeNode node;
		public SearchTreeItem(Rectangle2D.Double bounds , DefaultMutableTreeNode node) {
			this.bounds = bounds;
			this.node = node;
		}
	}
}

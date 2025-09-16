package haxby.db.all;

import haxby.db.custom.XMLJTreeDialog.XMLTreeNode;
import haxby.map.MapApp;
import haxby.util.URLFactory;
import haxby.wfs.WFSViewServer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
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
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.geomapapp.util.ParseLink;

public class AccessAllData implements ActionListener, TreeSelectionListener {

	haxby.map.MapApp mapApp;
	JFrame frame;
	JMenu treeMenu = new JMenu("Select Data From Menu");
	JPanel contentPane;
	JPanel searchPane = null;
	JPanel firstPane;
	JPanel secondPane = null;
	JPanel thirdPane = null;
	JButton okButton = null;
	JLabel searchLabel = new JLabel("\nSearch: ");
	JTextField searchBar = null;
	boolean loaded = false;
	int n_null = 0;
	private JTree tree;
	Vector openTreePaths;
	Vector rootVector;
	public DefaultMutableTreeNode top;
	public Hashtable layerNameToURL;
	public Hashtable globalNameToURL;
	public Hashtable globalNameToType;
	public Hashtable focusNameToURL;
	public Hashtable focusNameToWFSLayer;
	public Hashtable focusNameToType;
	String defaultSearchBarText = "Enter keyword for requested data type";
	private String layersXMLName;

	private boolean parseLayers = true;

	public AccessAllData( haxby.map.MapApp owner , boolean parseLayers) {
		this.parseLayers = parseLayers;

		mapApp = owner;

		switch (mapApp.getMapType()) {
		default:
		case MapApp.MERCATOR_MAP: layersXMLName = "layers.xml"; break;
		case MapApp.SOUTH_POLAR_MAP: layersXMLName = "layers_SP.xml"; break;
		case MapApp.NORTH_POLAR_MAP: layersXMLName = "layers_NP.xml"; break;
		}

		openTreePaths = new Vector();
		layerNameToURL = new Hashtable();
		globalNameToURL = new Hashtable();
		globalNameToType = new Hashtable();
		focusNameToURL = new Hashtable();
		focusNameToType = new Hashtable();
		focusNameToWFSLayer = new Hashtable();
		rootVector = new Vector();
		frame = new JFrame("Available Data");
		contentPane = new JPanel( new BorderLayout( 10, 10 ) );
		Border emptyBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		contentPane.setBorder( emptyBorder );
		searchPane = new JPanel( new BorderLayout( 5, 5 ) );
		top = new DefaultMutableTreeNode();
		okButton = new JButton("OK");
		okButton.addActionListener(this);
		searchBar = new JTextField(20);
		searchBar.setText(defaultSearchBarText);
		searchBar.selectAll();
		searchBar.addActionListener(this);
		createNodes(top);
		tree = new JTree(top);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		tree.addTreeSelectionListener(this);
		JScrollPane treeView = new JScrollPane(tree);
		contentPane.add(treeView, "Center");
		contentPane.add(okButton, "South");
		searchPane.add(searchLabel, "West");
		searchPane.add(searchBar, "Center");
		contentPane.add(searchPane, "North");
		contentPane.setOpaque(true);
		frame.setContentPane(contentPane);
		frame.pack();
		traverseTree(top, treeMenu);
	}

	public AccessAllData( haxby.map.MapApp owner ) {
		this(owner, true);
	}

	public void openDataWindow() {
		contentPane = new JPanel( new BorderLayout( 10, 10 ) );
		Border emptyBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		contentPane.setBorder( emptyBorder );
		searchPane = new JPanel( new BorderLayout( 5, 5 ) );
		top = new DefaultMutableTreeNode();
		okButton = new JButton("OK");
		okButton.addActionListener(this);
		searchBar = new JTextField(20);
		searchBar.setText(defaultSearchBarText);
		searchBar.selectAll();
		searchBar.addActionListener(this);
		createNodes(top);
		tree = new JTree(top);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		tree.addTreeSelectionListener(this);
		JScrollPane treeView = new JScrollPane(tree);
		contentPane.add(treeView, "Center");
		contentPane.add(okButton, "South");
		searchPane.add(searchLabel, "West");
		searchPane.add(searchBar, "Center");
		contentPane.add(searchPane, "North");
		contentPane.setOpaque(true);
		frame.setContentPane(contentPane);
		frame.pack();
		frame.setVisible(true);
	}

	private void createNodes(DefaultMutableTreeNode top) {
		DefaultMutableTreeNode category = null;
		DefaultMutableTreeNode book = null;
		category = new DefaultMutableTreeNode("Custom Data Viewers");
		top.add(category);
		String[] dbNames = mapApp.getDBNames();

		for ( int i = 0; i < dbNames.length; i++ ) {
			book = new DefaultMutableTreeNode(dbNames[i]);
			category.add(book);
		}

		category = new DefaultMutableTreeNode("General Data Viewers");
		addGlobalDBContents(category);
//		category = new DefaultMutableTreeNode("Layers");

		if (parseLayers) {
			try {
	//			Vector layers = ParseLink.parse(URLFactory.url(org.geomapapp.io.GMARoot.ROOT_URL +"/Layers/layers.xml"));
				Vector layers;
				// not in use, excluded from build.
				if ( mapApp.getMapType() == haxby.map.MapApp.SOUTH_POLAR_MAP ) {
					layers = ParseLink.parse(URLFactory.url(haxby.map.MapApp.TEMP_BASE_URL +"GMA/Layers/layers_SP.xml"));
				} else if ( mapApp.getMapType() == haxby.map.MapApp.NORTH_POLAR_MAP ) {
					layers = ParseLink.parse(URLFactory.url(haxby.map.MapApp.TEMP_BASE_URL +"GMA/Layers/layers_NP.xml"));
				} else {
					layers = ParseLink.parse(URLFactory.url(haxby.map.MapApp.TEMP_BASE_URL +"GMA/Layers/layers.xml"));
				}
	//			Vector layers = ParseLink.parse(URLFactory.url(org.geomapapp.io.GMARoot.ROOT_URL +"/Layers/ + layersXMLName));
	//			Vector layers = ParseLink.parse(URLFactory.url(haxby.map.MapApp.TEMP_BASE_URL +"GMA/Layers/" + layersXMLName));
	//			LayerEntry rootEntry = new LayerEntry(null, new Vector(), "layers", "", null);
				DefaultMutableTreeNode rootEntry = new DefaultMutableTreeNode("Layers");
				layers = ParseLink.getProperties( layers, "layer");
				for( int i=0 ; i<layers.size() ; i++) {
					addLayers( (Vector)layers.get(i), rootEntry);
				}
				category.add(rootEntry);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		top.add(category);

		if ( mapApp.getMapType() == haxby.map.MapApp.MERCATOR_MAP ) {
				addFocusSites(top);
		}

		for (Enumeration e = top.children(); e.hasMoreElements() ;) {
			 DefaultMutableTreeNode child = (DefaultMutableTreeNode)e.nextElement();
			 rootVector.add(child);
		}
	}

	public void addFocusSites(DefaultMutableTreeNode parent) {
		Vector data = null;
		String xmlurl = haxby.map.MapApp.TEMP_BASE_URL +"GMA/FocusSites/focussites.xml";
		try {
			data = ParseLink.parse(URLFactory.url(xmlurl));
		} catch (IOException e) {
		}
		if (data!=null) {
			makeFocusTree(parent, data);
		}
	}

	public void addLayers(Vector layers, DefaultMutableTreeNode parent) {
		String name = (String)ParseLink.getProperty( layers, "name");
		if( name==null ) name = "untitled_"+(++n_null);
//		System.out.println(name);
		String description = (String)ParseLink.getProperty( layers, "description");
		if( description==null )description="";
		String url = (String)ParseLink.getProperty( layers, "url");
		Vector children = (url==null)
				? new Vector()
				: null;
//		LayerEntry entry = new LayerEntry( parent, children, name, description, url);
		DefaultMutableTreeNode entry = new DefaultMutableTreeNode(name);
		if ( name != null && url != null ) {
			layerNameToURL.put(name, url);
		}
		parent.add( entry );
		Vector props = ParseLink.getProperties( layers, "layer");
		for( int k=0 ; k<props.size() ; k++) {
			Vector prop = (Vector)props.get(k);
			addLayers( prop, entry);
		}
	}

	public void addGlobalDBContents(DefaultMutableTreeNode parent) {
		Vector data = null;
		String xmlurl = haxby.map.MapApp.TEMP_BASE_URL +"database/globalDB.xml";
		try {
			data = ParseLink.parse(URLFactory.url(xmlurl));
		} catch (IOException e) {
		}
		DefaultMutableTreeNode globalRoot = new DefaultMutableTreeNode("Tables");
		if (data!=null) {
			makeTree(globalRoot, data);
		}
		parent.add(globalRoot);
	}

	public void traverseTree(DefaultMutableTreeNode root, JMenu rootMenu) {
		for (Enumeration e = root.children(); e.hasMoreElements(); ) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode)e.nextElement();
			if ( child.isLeaf() ) {
				JMenuItem childMI = new JMenuItem(child.toString());
				rootMenu.add(childMI);
				childMI.addActionListener(this);
			} else {
				JMenu nextRootMenu = new JMenu(child.toString());
				traverseTree(child, nextRootMenu);
				rootMenu.add(nextRootMenu);
			}
		}
	}

	private void makeTree(DefaultMutableTreeNode root, Vector childernData) {
		for (Iterator iter = childernData.iterator(); iter.hasNext();) {
			DefaultMutableTreeNode child;
			XMLTreeNode childName = new XMLTreeNode(null);
			Object[] obj = (Object[]) iter.next();
			if (obj[1] instanceof Vector) {
//				System.out.println(ParseLink.getProperty((Vector)obj[1], "url"));
//				System.out.println(ParseLink.getProperty((Vector)obj[1], "type"));
				if ( ParseLink.getProperty((Vector)obj[1], "url") != null ) {
					globalNameToURL.put( ParseLink.getProperty((Vector)obj[1], "name"), ParseLink.getProperty((Vector)obj[1], "url") );
				}
				if ( ParseLink.getProperty((Vector)obj[1], "type") != null ) {
					globalNameToType.put( ParseLink.getProperty((Vector)obj[1], "name"), ParseLink.getProperty((Vector)obj[1], "type") );
			 	}
				child = new DefaultMutableTreeNode(ParseLink.getProperty((Vector)obj[1], "name"));
				makeTree(child, (Vector)obj[1]);
				root.add(child);
				} else {
			}
		}
	}

	private void makeFocusTree(DefaultMutableTreeNode root, Vector childrenData) {
		for (Iterator iter = childrenData.iterator(); iter.hasNext();) {
			DefaultMutableTreeNode child;
			XMLTreeNode childName = new XMLTreeNode(null);
			Object[] obj = (Object[]) iter.next();
			if (obj[1] instanceof Vector) {
				Object name = ParseLink.getProperty((Vector)obj[1], "name");

			if (name == null)
				continue;

			if (!parseLayers && name.toString().equals("Layers"))
				continue;

				if ( ParseLink.getProperty((Vector)obj[1], "url") != null ) {
					focusNameToURL.put( name, ParseLink.getProperty((Vector)obj[1], "url") );
				}
				if ( ParseLink.getProperty((Vector)obj[1], "type") != null ) {
				focusNameToType.put( name, ParseLink.getProperty((Vector)obj[1], "type") );
				}
				if ( ParseLink.getProperty((Vector)obj[1], "url2") != null ) {
					focusNameToWFSLayer.put( name, ParseLink.getProperty((Vector)obj[1], "url2") );
				}
				child = new DefaultMutableTreeNode(name);
				makeFocusTree(child, (Vector)obj[1]);
				root.add(child);
			} else {
			}
		}
	}

	public void search( String query, DefaultMutableTreeNode root) {
		for (Enumeration e = root.children(); e.hasMoreElements() ;) {
			 DefaultMutableTreeNode child;
			 child = (DefaultMutableTreeNode)e.nextElement();
			 String tempChildString = child.toString().toLowerCase();
			 String tempQuery = query.toLowerCase();
			 if ( tempChildString.indexOf(tempQuery) != -1 ) {
//				 System.out.println(child.toString());
				 tree.expandPath( new TreePath( ((DefaultMutableTreeNode) child.getParent()).getPath() ) );
				 tree.getSelectionModel().addSelectionPath(new TreePath( ( (DefaultMutableTreeNode) child).getPath() ) );
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

	public JMenu getTreeMenu() {
		return treeMenu;
	}

	public void actionPerformed(ActionEvent arg0) {
		if ( arg0.getSource() == okButton ) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
			if (node == null) {
				return;
			}
			Object nodeInfo = node.getUserObject();
			if (node.getParent().toString().equals("Custom Data Viewers")) {
				mapApp.accessDatabaseMenuItems(node.toString());
			}
			else if ( focusNameToURL.get(node.toString()) != null ) {
				if ( focusNameToWFSLayer.containsKey(node.toString()) ) {
					WFSViewServer wfsWindow = new WFSViewServer(mapApp);
					try {
						wfsWindow.remoteWFS();
					} catch (Exception ignore) {
						ignore.printStackTrace();
					}
					try {
						URL inputCapabilitiesURL = URLFactory.url((String)focusNameToURL.get(node.toString()));
						wfsWindow.dispose();
						wfsWindow.setCapabilitiesURL(inputCapabilitiesURL);
						wfsWindow.readCapabilities(inputCapabilitiesURL);
						wfsWindow.setCurrentLayerName((String)focusNameToWFSLayer.get(node.toString()));
						wfsWindow.getLayer();
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if ( focusNameToType.get(node.toString()) == null ) {
					mapApp.addShapeFile((String)focusNameToURL.get(node.toString()));
				}
				else {
					mapApp.getGlobalDataset( node.toString(), (String)focusNameToURL.get(node.toString()), Integer.parseInt( (String)focusNameToType.get(node.toString()) ) );
				}
			}
			else if ( layerNameToURL.get(node.toString()) != null ) {
				mapApp.addShapeFile((String)layerNameToURL.get(node.toString()));
			}
			else if ( globalNameToURL.get(node.toString()) != null && globalNameToType.get(node.toString()) != null ) {
				mapApp.getGlobalDataset( node.toString(), (String)globalNameToURL.get(node.toString()), Integer.parseInt( (String)globalNameToType.get(node.toString()) ) );
			}
		}
		else if ( arg0.getSource() == searchBar ) {
			tree.getSelectionModel().clearSelection();
			closeAllPaths(tree);
			search(searchBar.getText(), top);
		}
		else if ( arg0.getSource() instanceof JMenuItem ) {
			JMenuItem tempMI = (JMenuItem)arg0.getSource();
			if ( focusNameToURL.get(tempMI.getText()) != null ) {
				if ( focusNameToWFSLayer.containsKey(tempMI.getText()) ) {
					WFSViewServer wfsWindow = new WFSViewServer(mapApp);
					try {
						wfsWindow.remoteWFS();
					} catch (Exception ignore) {
						ignore.printStackTrace();
					}
					try {
						URL inputCapabilitiesURL = URLFactory.url((String)focusNameToURL.get(tempMI.getText()));
						wfsWindow.dispose();
						wfsWindow.setCapabilitiesURL(inputCapabilitiesURL);
						wfsWindow.readCapabilities(inputCapabilitiesURL);
						wfsWindow.setCurrentLayerName((String)focusNameToWFSLayer.get(tempMI.getText()));
						wfsWindow.getLayer();
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if ( focusNameToType.get(tempMI.getText()) == null ) {
					mapApp.addShapeFile((String)focusNameToURL.get(tempMI.getText()));
				} else {
					mapApp.getGlobalDataset( tempMI.getText(), (String)focusNameToURL.get(tempMI.getText()), Integer.parseInt( (String)focusNameToType.get(tempMI.getText()) ) );
				}
			} else if ( layerNameToURL.get(tempMI.getText()) != null ) {
				mapApp.addShapeFile((String)layerNameToURL.get(tempMI.getText()));
			} else if ( globalNameToURL.get(tempMI.getText()) != null && globalNameToType.get(tempMI.getText()) != null ) {
				mapApp.getGlobalDataset( tempMI.getText(), (String)globalNameToURL.get(tempMI.getText()), Integer.parseInt( (String)globalNameToType.get(tempMI.getText()) ) );
			} else {
				mapApp.accessDatabaseMenuItems(tempMI.getText());
			}
		}
	}

	public void valueChanged(TreeSelectionEvent arg0) {
		if ( arg0.getSource() == tree ) {
			if ( arg0.isAddedPath() ) {
//				System.out.println("Added selection path");
				openTreePaths.add(arg0.getPath());
			} else {
//				System.out.println("Removed selection path");
			}
		}
	}
}

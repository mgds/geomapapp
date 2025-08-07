package org.geomapapp.util;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.geomapapp.gis.shape.ESRIShapefile;
import org.geomapapp.grid.Grid2DOverlay;
import org.geomapapp.grid.GridDialog;
import org.geomapapp.image.Palette;
import org.geomapapp.io.GMARoot;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import haxby.db.custom.CustomDB;
import haxby.db.custom.UnknownDataSet;
import haxby.map.MapApp;
import haxby.map.Overlay;
import haxby.map.StartUp;
import haxby.util.FilesUtil;
import haxby.util.GeneralUtils;
import haxby.util.LayerManager.LayerPanel;
import haxby.util.PathUtil;
import haxby.util.URLFactory;
import haxby.wms.XML_Layer.RequestLayer;
/**
 * @author Samantha Chan
 * @author Justin Coplan
 * @author Andrew Melkonian
 *
 */
public class XML_Menu {

	public static String url = MapApp.NEW_BASE_URL + "gma_menus/main_menu.xml";
	public static String current_os = System.getProperty("os.name");
	static haxby.map.MapApp mapApp;
	static StartUp start;
	static JMenu winMenu;
	public String name,
				index,
				command,
				layer_url,
				layer_url2,
				infoURLString,
				search_url,
				proj,
				mapproj,
				os,
				checkbox,			//10
				type,
				wesn,
				srs,
				symbol_shape,
				symbol_allcolor,
				symbol_size,
				sst,
				sst_col_ind,
				sst_num_col_ind,
				sst_range_start,
				sst_range_end,
				sst_flip,
				cst,
				cst_col_ind,
				cst_num_col_ind,
				cst_range_start,
				cst_range_end,
				cst_pal_r,
				cst_pal_g,
				cst_pal_b,
				cst_pal_ht,
				cst_discrete,
				cst_tabs,
				grid,
				grid_which_pal,
				grid_lpal_name,
				grid_lpal_r,
				grid_lpal_g,
				grid_lpal_b,
				grid_lpal_ht,
				grid_lpal_discrete,
				grid_lpal_range,
				grid_opal_name,
				grid_opal_r,
				grid_opal_g,
				grid_opal_b,
				grid_opal_ht,
				grid_opal_range,
				grid_opal_discrete,
				grid_dpal_name,
				grid_dpal_r,
				grid_dpal_g,
				grid_dpal_b,
				grid_dpal_ht,
				grid_dpal_discrete,
				grid_dpal_range,
				grid_tabs,
				grid_ve,
				grid_illum,
				grid_az,
				grid_alt,
				grid_contours,
				grid_cont_int,
				grid_cont_bolding,
				grid_cont_min,
				grid_cont_max,
				separator_bar,
				shortcut,
				multipleshapes,
				map_place_lon,
				map_place_lat,
				map_place_zoom,
				isRemote,			//20
				legend,
				warning,
				mapres,
				version,
				transparent,
				format,
				style,
				base_elev,
				height,
				opacity_value,		//30
				display_layer,
				color,
				session,
				layer_file_import,
				wfs_layer_feature,
				wfs_bbox,
				zoom,
				lonX,
				latY;

	public RequestLayer[] layers;
	public List<XML_Menu> child_layers = new LinkedList<XML_Menu>();
	public XML_Menu parent;
	public static Vector<String[]> possibleGrids = null;
	public static Hashtable<String, XML_Menu> menuHash = new Hashtable<String, XML_Menu>();
	public static Hashtable<String, JMenuItem> commandToMenuItemHash = new Hashtable<String, JMenuItem>();
	public static Hashtable<JMenuItem, XML_Menu> menuItemToMenu = new Hashtable<JMenuItem, XML_Menu>();
	public static Map<XML_Menu, JMenuItem> menuToMenuItem = new HashMap<XML_Menu, JMenuItem>();
	public static Hashtable<String, String> attributeToVariableHash = new Hashtable<String, String>();
	public static XML_Menu template = new XML_Menu();

	private static void clearMenus() {
		menuItemToMenu.clear();
		
		//If we are switching projections due to a session load, then we want to keep the
		//Layer Sessions menu items so we can load them up after the switch.
		Map<XML_Menu, JMenuItem> menuToMenuItemTmp = new HashMap<XML_Menu, JMenuItem>();
		Set<XML_Menu> keys = menuToMenuItem.keySet();
		Iterator<XML_Menu> itr = keys.iterator();
		while (itr.hasNext()) {
			XML_Menu m = itr.next();
			JMenuItem mi = menuToMenuItem.get(m);
			try {
				if (m.parent.parent.name.equals("My Layer Sessions")) {
					menuItemToMenu.put(mi, m);
					menuToMenuItemTmp.put(m, mi);
				}
			} catch(Exception e){}	
		}
		menuToMenuItem.clear();
		menuToMenuItem.putAll(menuToMenuItemTmp);
	}
	
	public static void initAttributeToVariableHash() {
		attributeToVariableHash.put("checkbox", template.checkbox);
		attributeToVariableHash.put("lonX", template.lonX);
		attributeToVariableHash.put("latY", template.latY);
		attributeToVariableHash.put("color", template.color);
		attributeToVariableHash.put("command", template.command);
		attributeToVariableHash.put("display", template.display_layer);
		attributeToVariableHash.put("name", template.name);
		attributeToVariableHash.put("index", template.index);
		attributeToVariableHash.put("format", template.format);
		attributeToVariableHash.put("href", template.infoURLString);
		attributeToVariableHash.put("import_file", template.layer_file_import);		//10
		attributeToVariableHash.put("legend", template.legend);
		attributeToVariableHash.put("mapproj", template.mapproj);
		attributeToVariableHash.put("multipleshapes", template.multipleshapes);
		attributeToVariableHash.put("mapres", template.mapres);
		attributeToVariableHash.put("opacity", template.opacity_value);
		attributeToVariableHash.put("os", template.os);
		attributeToVariableHash.put("proj", template.proj);
		attributeToVariableHash.put("search_url", template.search_url);
		attributeToVariableHash.put("separator_bar", template.separator_bar);
		attributeToVariableHash.put("session", template.session);					//20
		attributeToVariableHash.put("shortcut", template.shortcut);
		attributeToVariableHash.put("symbol_shape", template.symbol_shape);
		attributeToVariableHash.put("symbol_allcolor", template.symbol_allcolor);
		attributeToVariableHash.put("symbol_size", template.symbol_size);
		attributeToVariableHash.put("sst", template.sst);
		attributeToVariableHash.put("sst", template.sst);
		attributeToVariableHash.put("sst_col_ind", template.sst_col_ind);
		attributeToVariableHash.put("sst_num_col_ind", template.sst_num_col_ind);
		attributeToVariableHash.put("sst_range_start", template.sst_range_start);
		attributeToVariableHash.put("sst_range_end", template.sst_range_end);
		attributeToVariableHash.put("sst_flip", template.sst_flip);
		attributeToVariableHash.put("cst", template.cst);
		attributeToVariableHash.put("cst_col_ind", template.cst_col_ind);
		attributeToVariableHash.put("cst_num_col_ind", template.cst_num_col_ind);
		attributeToVariableHash.put("cst_range_start", template.cst_range_start);
		attributeToVariableHash.put("cst_range_end", template.cst_range_end);
		attributeToVariableHash.put("cst_pal_r", template.cst_pal_r);
		attributeToVariableHash.put("cst_pal_g", template.cst_pal_g);
		attributeToVariableHash.put("cst_pal_b", template.cst_pal_b);
		attributeToVariableHash.put("cst_pal_ht", template.cst_pal_ht);
		attributeToVariableHash.put("cst_discrete", template.cst_discrete);
		attributeToVariableHash.put("cst_tabs", template.cst_tabs);
		attributeToVariableHash.put("grid", template.grid);
		attributeToVariableHash.put("grid_which_pal", template.grid_which_pal);
		attributeToVariableHash.put("grid_dpal_name", template.grid_dpal_name);
		attributeToVariableHash.put("grid_dpal_r", template.grid_dpal_r);
		attributeToVariableHash.put("grid_dpal_g", template.grid_dpal_g);
		attributeToVariableHash.put("grid_dpal_b", template.grid_dpal_b);
		attributeToVariableHash.put("grid_dpal_ht", template.grid_dpal_ht);
		attributeToVariableHash.put("grid_dpal_discrete", template.grid_dpal_discrete);
		attributeToVariableHash.put("grid_dpal_range", template.grid_dpal_range);
		attributeToVariableHash.put("grid_lpal_name", template.grid_lpal_name);
		attributeToVariableHash.put("grid_lpal_r", template.grid_lpal_r);
		attributeToVariableHash.put("grid_lpal_g", template.grid_lpal_g);
		attributeToVariableHash.put("grid_lpal_b", template.grid_lpal_b);
		attributeToVariableHash.put("grid_lpal_ht", template.grid_lpal_ht);
		attributeToVariableHash.put("grid_lpal_discrete", template.grid_lpal_discrete);
		attributeToVariableHash.put("grid_lpal_range", template.grid_lpal_range);
		attributeToVariableHash.put("grid_opal_name", template.grid_opal_name);
		attributeToVariableHash.put("grid_opal_r", template.grid_opal_r);
		attributeToVariableHash.put("grid_opal_g", template.grid_opal_g);
		attributeToVariableHash.put("grid_opal_b", template.grid_opal_b);
		attributeToVariableHash.put("grid_opal_ht", template.grid_opal_ht);
		attributeToVariableHash.put("grid_opal_discrete", template.grid_opal_discrete);
		attributeToVariableHash.put("grid_opal_range", template.grid_opal_range);
		attributeToVariableHash.put("grid_tabs", template.grid_tabs);
		attributeToVariableHash.put("grid_ve", template.grid_ve);
		attributeToVariableHash.put("grid_illum", template.grid_illum);
		attributeToVariableHash.put("grid_alt", template.grid_alt);
		attributeToVariableHash.put("grid_az", template.grid_az);
		attributeToVariableHash.put("grid_contours", template.grid_contours);
		attributeToVariableHash.put("grid_cont_int", template.grid_cont_int);
		attributeToVariableHash.put("grid_cont_bolding", template.grid_cont_bolding);
		attributeToVariableHash.put("grid_cont_min", template.grid_cont_min);
		attributeToVariableHash.put("grid_cont_max", template.grid_cont_max);
		attributeToVariableHash.put("style", template.style);
		attributeToVariableHash.put("transparent", template.transparent);
		attributeToVariableHash.put("type", template.type);
		attributeToVariableHash.put("url", template.layer_url);
		attributeToVariableHash.put("url2", template.layer_url2);
		attributeToVariableHash.put("version", template.version);
		attributeToVariableHash.put("warning", template.warning);
		attributeToVariableHash.put("wesn", template.wesn);							//30
		attributeToVariableHash.put("wfs_feature", template.wfs_layer_feature);
		attributeToVariableHash.put("wfs_bbox", template.wfs_bbox);
		attributeToVariableHash.put("zoom", template.zoom);
	}

	public static List<XML_Menu> parse(String url) throws MalformedURLException,
	IOException, ParserConfigurationException, SAXException {
		return parse(URLFactory.url(url));
	}

	public static List<XML_Menu> parse(URL url) throws IOException,
	ParserConfigurationException, SAXException {
		return parse(new BufferedInputStream(url.openStream()));
	}

	public static List<XML_Menu> parse(File f) throws ParserConfigurationException,
	SAXException, IOException {
		return parse(new BufferedInputStream(new FileInputStream(f)));
	}

	public static boolean validate(File mainMenuFile) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setIgnoringComments(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse(mainMenuFile);
		} catch (SAXException e) {
			return false;
		} catch (ParserConfigurationException pe) {
			return false;
		} catch (IOException io) {
			return false;
		}
		return true;
	}

	protected static File parentRoot = MapApp.getGMARoot();
	protected static File menusCacheDir = new File( parentRoot, "menus_cache");
	protected static File menusCacheDir2 = new File(menusCacheDir, "menus");
	protected static File menusCacheFileFirst = new File( menusCacheDir2, "main_menu.xml");

	public static List<XML_Menu> parse(InputStream in)
	throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringComments(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document dom = db.parse(in);
		Node root = dom.getFirstChild();

		// Cache main xml file
		if(MapApp.ReadMenusCache == false ) {
			Transformer transformer;
			try {
				transformer = TransformerFactory.newInstance().newTransformer();
				Result outputRoot = new StreamResult(menusCacheFileFirst);
				Source input = new DOMSource(dom);
				transformer.transform(input, outputRoot);
			} catch (TransformerConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TransformerFactoryConfigurationError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TransformerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		List<XML_Menu> menus = parseMenu(root);
		MapApp.ReadMenusCache = true;
		return menus;
	}

	public static List<XML_Menu> parseMenu(Node root) {
		boolean switchMe = false;
		NodeList nodes = root.getChildNodes();
		List<XML_Menu> menus = new LinkedList<XML_Menu>();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			XML_Menu sub_layer = new XML_Menu();
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			if (node.getNodeName().equals("layer")) {
				NamedNodeMap attributes = node.getAttributes();
				Node attribute = null;

				if ((attribute = attributes.getNamedItem("name")) != null ) {
					sub_layer.name = attribute.getNodeValue();
				//	System.out.println(sub_layer.name);
				}
				if ((attribute = attributes.getNamedItem("index")) != null ) {
					sub_layer.index = attribute.getNodeValue();
				}
				if ((attribute = attributes.getNamedItem("command")) != null ) {
					sub_layer.command = attribute.getNodeValue();
					if ( sub_layer.command.equals("wms_cmd") ) {
						List<Node> inputLayers = getElements(node, "wms_layer");
						Iterator<Node> iter = inputLayers.iterator();
						sub_layer.layers = new RequestLayer[inputLayers.size()];
						for (int j = 0; j < inputLayers.size(); j++) {
							sub_layer.layers[j] = parseLayer(iter.next());
						}
					}
					else if ( sub_layer.command.equals("map_place_cmd")) {
						List<Node> inputLayers = getElements(node, "map_place");
						Iterator<Node> iter = inputLayers.iterator();
						for (int j = 0; j < inputLayers.size(); j++) {
							Node tempNode = iter.next();
							sub_layer.map_place_lon = tempNode.getAttributes().getNamedItem("lon").getNodeValue();
							sub_layer.map_place_lat = tempNode.getAttributes().getNamedItem("lat").getNodeValue();
							sub_layer.map_place_zoom = tempNode.getAttributes().getNamedItem("zoom").getNodeValue();
						}
					}
				}
				if ( (attribute = attributes.getNamedItem("type")) != null ) {
					sub_layer.type = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("checkbox")) != null ) {
					sub_layer.checkbox = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("opacity")) != null ) {
					sub_layer.opacity_value = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("color")) != null ) {
					sub_layer.color = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("lonX")) != null ) {
					sub_layer.lonX = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("latY")) != null ) {
					sub_layer.latY = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("session")) != null ) {
					sub_layer.session = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("display")) != null ) {
					sub_layer.display_layer = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("srs")) != null ) {
					sub_layer.srs = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("symbol_shape")) != null ) {
					sub_layer.symbol_shape = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("symbol_allcolor")) != null ) {
					sub_layer.symbol_allcolor = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("symbol_size")) != null ) {
					sub_layer.symbol_size = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("sst")) != null ) {
					sub_layer.sst = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("sst_col_ind")) != null ) {
					sub_layer.sst_col_ind = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("sst_num_col_ind")) != null ) {
					sub_layer.sst_num_col_ind = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("sst_range_start")) != null ) {
					sub_layer.sst_range_start = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("sst_range_end")) != null ) {
					sub_layer.sst_range_end = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("sst_flip")) != null ) {
					sub_layer.sst_flip = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("cst")) != null ) {
					sub_layer.cst = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("cst_col_ind")) != null ) {
					sub_layer.cst_col_ind = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("cst_num_col_ind")) != null ) {
					sub_layer.cst_num_col_ind = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("cst_range_start")) != null ) {
					sub_layer.cst_range_start = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("cst_range_end")) != null ) {
					sub_layer.cst_range_end = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("cst_pal_r")) != null ) {
					sub_layer.cst_pal_r = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("cst_pal_g")) != null ) {
					sub_layer.cst_pal_g = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("cst_pal_b")) != null ) {
					sub_layer.cst_pal_b = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("cst_pal_ht")) != null ) {
					sub_layer.cst_pal_ht = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("cst_discrete")) != null ) {
					sub_layer.cst_discrete = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("cst_tabs")) != null ) {
					sub_layer.cst_tabs = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid")) != null ) {
					sub_layer.grid = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_which_pal")) != null ) {
					sub_layer.grid_which_pal = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_dpal_name")) != null ) {
					sub_layer.grid_dpal_name = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_dpal_r")) != null ) {
					sub_layer.grid_dpal_r = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_dpal_g")) != null ) {
					sub_layer.grid_dpal_g = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_dpal_b")) != null ) {
					sub_layer.grid_dpal_b = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_dpal_ht")) != null ) {
					sub_layer.grid_dpal_ht = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_dpal_discrete")) != null ) {
					sub_layer.grid_dpal_discrete = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_dpal_range")) != null ) {
					sub_layer.grid_dpal_range = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_lpal_name")) != null ) {
					sub_layer.grid_lpal_name = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_lpal_r")) != null ) {
					sub_layer.grid_lpal_r = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_lpal_g")) != null ) {
					sub_layer.grid_lpal_g = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_lpal_b")) != null ) {
					sub_layer.grid_lpal_b = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_lpal_ht")) != null ) {
					sub_layer.grid_lpal_ht = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_lpal_discrete")) != null ) {
					sub_layer.grid_lpal_discrete = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_lpal_range")) != null ) {
					sub_layer.grid_lpal_range = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_opal_name")) != null ) {
					sub_layer.grid_opal_name = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_opal_r")) != null ) {
					sub_layer.grid_opal_r = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_opal_g")) != null ) {
					sub_layer.grid_opal_g = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_opal_b")) != null ) {
					sub_layer.grid_opal_b = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_opal_ht")) != null ) {
					sub_layer.grid_opal_ht = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_opal_discrete")) != null ) {
					sub_layer.grid_opal_discrete = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_opal_range")) != null ) {
					sub_layer.grid_opal_range = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_tabs")) != null ) {
					sub_layer.grid_tabs = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_ve")) != null ) {
					sub_layer.grid_ve = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_illum")) != null ) {
					sub_layer.grid_illum = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_alt")) != null ) {
					sub_layer.grid_alt = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_az")) != null ) {
					sub_layer.grid_az = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_contours")) != null ) {
					sub_layer.grid_contours = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_cont_int")) != null ) {
					sub_layer.grid_cont_int = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_cont_bolding")) != null ) {
					sub_layer.grid_cont_bolding = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_cont_min")) != null ) {
					sub_layer.grid_cont_min = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("grid_cont_max")) != null ) {
					sub_layer.grid_cont_max = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("wesn")) != null ) {
					sub_layer.wesn = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("wfs_feature")) != null ) {
					sub_layer.wfs_layer_feature = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("wfs_bbox")) != null ) {
					sub_layer.wfs_bbox = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("import_file")) != null ) {
					sub_layer.layer_file_import = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("href")) != null ) {
					sub_layer.infoURLString = MapApp.getAppropriateUrl(attribute.getNodeValue());

					if (!sub_layer.infoURLString.startsWith("http")) {
						sub_layer.infoURLString =
							PathUtil.getPath("ROOT_PATH") + sub_layer.infoURLString;
					}
				}
				if ( (attribute = attributes.getNamedItem("search_url")) != null ) {
					sub_layer.search_url = MapApp.getAppropriateUrl(attribute.getNodeValue());

					if (!sub_layer.search_url.startsWith("http")){
						sub_layer.search_url =
							PathUtil.getPath("ROOT_PATH") + sub_layer.search_url;
					}
				}
				if ( (attribute = attributes.getNamedItem("url2")) != null ) {
					sub_layer.layer_url2 = MapApp.getAppropriateUrl(attribute.getNodeValue());

					if (!sub_layer.layer_url2.startsWith("http")) {
						sub_layer.layer_url2 =
							PathUtil.getPath("ROOT_PATH") + sub_layer.layer_url2;
					}
				}
				if ( (attribute = attributes.getNamedItem("url")) != null ) {
					sub_layer.layer_url = MapApp.getAppropriateUrl(attribute.getNodeValue());
					if (!sub_layer.layer_url.startsWith("http") && !sub_layer.layer_url.contains("import")) {
						sub_layer.layer_url = PathUtil.getPath("ROOT_PATH") + sub_layer.layer_url;
					}

					if (sub_layer.layer_url.startsWith("import_file")){
							File file = new File(org.geomapapp.io.GMARoot.getRoot() + File.separator + "layers" + File.separator +
									sub_layer.layer_file_import);

							if (file.exists()){
								try {
									DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
									dbf.setIgnoringComments(true);
									DocumentBuilder db = null;
									db = dbf.newDocumentBuilder();
									Document doc = null;
									doc = db.parse(file);
									doc.getDocumentElement().normalize();

									Node sub_root = doc.getFirstChild();
									sub_layer.child_layers = parseMenu(sub_root);
									for (XML_Menu m : sub_layer.child_layers)
										m.parent = sub_layer;
									} catch (ParserConfigurationException e) {
										e.printStackTrace();
									} catch (DOMException e) {
										e.printStackTrace();
									} catch (SAXException e) {
										System.out.println("SAXException" + sub_layer.layer_url);
										e.printStackTrace();
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							}

						if ( sub_layer.layer_url.endsWith(".xml") ) {
							DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
							dbf.setIgnoringComments(true);
							DocumentBuilder db = null;

							try {
								db = dbf.newDocumentBuilder();
								Document dom = null;
								// We want to rewrite on the fly all url menus to grab from dev location.
								if(MapApp.BASE_URL.matches(MapApp.DEV_URL)){
									sub_layer.layer_url= sub_layer.layer_url.replaceAll("http://app\\.", "http://app-dev.")
											.replaceAll("https://app\\.", "https://app-dev.");
								}

								String[] parseURL = sub_layer.layer_url.split("/");
								if(MapApp.ReadMenusCache == true) {
									File mF = new File(GMARoot.getRoot() + File.separator + "menus_cache" + File.separator + "menus" + File.separator + parseURL[parseURL.length - 1]);
									// Catch if dom is corrupt
									try {
										dom = db.parse(new BufferedInputStream(new FileInputStream(mF)));
									} catch (SAXException se) {
										// If corrupt call clear cache to flush out the cache.
										switchMe = true;
									} catch(IOException ioe) {
										switchMe = true;
									}
								}
								if(MapApp.ReadMenusCache == false || switchMe == true) {
									dom = db.parse(URLFactory.url(sub_layer.layer_url).openStream());
									Transformer transformer;
									try {
										if(!MapApp.AT_SEA) {
											transformer = TransformerFactory.newInstance().newTransformer();
											Result outputRoot = new StreamResult(new File(GMARoot.getRoot() + File.separator + "menus_cache" + File.separator + "menus" + File.separator + parseURL[parseURL.length - 1]));
											Source input = new DOMSource(dom);
											transformer.transform(input, outputRoot);
										}
									} catch (TransformerConfigurationException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (TransformerFactoryConfigurationError e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (TransformerException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								Node sub_root = dom.getFirstChild();
								sub_layer.child_layers = parseMenu(sub_root);

							for (XML_Menu m : sub_layer.child_layers)
								m.parent = sub_layer;
							} catch (ParserConfigurationException e) {
								e.printStackTrace();
							} catch (DOMException e) {
								e.printStackTrace();
							} catch (SAXException e) {
								System.out.println("SAXException" + sub_layer.layer_url);
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				else {
					sub_layer.child_layers = parseMenu(node);
					for (XML_Menu m : sub_layer.child_layers)
						m.parent = sub_layer;
				}
				if ( (attribute = attributes.getNamedItem("proj")) != null ) {
					sub_layer.proj = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("mapproj")) != null ) {
					sub_layer.mapproj = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("os")) != null ) {
					sub_layer.os = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("separator_bar")) != null ) {
					sub_layer.separator_bar = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("shortcut")) != null ) {
					sub_layer.shortcut = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("multipleshapes")) != null ) {
					sub_layer.multipleshapes = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("remote")) != null ) {
					sub_layer.isRemote = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("legend")) != null ) {
					sub_layer.legend = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("warning")) != null ) {
					sub_layer.warning = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("zoom")) != null ) {
					sub_layer.zoom = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("mapres")) != null ) {
					sub_layer.mapres = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("base_elev")) != null ) {
					sub_layer.base_elev = attribute.getNodeValue();
				}
				if ( (attribute = attributes.getNamedItem("height")) != null ) {
					sub_layer.height = attribute.getNodeValue();
				}

				menuHash.put(sub_layer.name + sub_layer.layer_url, sub_layer);
				if ( sub_layer.command != null ) {
					if (sub_layer.command.equals("shape_cmd") ) {
						if ( possibleGrids == null ) {
							possibleGrids = new Vector<String[]>();
						}
						String[] temp = {sub_layer.name,sub_layer.layer_url};
						if ( mapApp.getMapType() == MapApp.MERCATOR_MAP && sub_layer.proj.indexOf("m") != -1 ) {
							possibleGrids.add(temp);
						}
						else if ( mapApp.getMapType() == MapApp.NORTH_POLAR_MAP && sub_layer.proj.indexOf("n") != -1 ) {
							possibleGrids.add(temp);
						}
						else if ( mapApp.getMapType() == MapApp.SOUTH_POLAR_MAP && sub_layer.proj.indexOf("s") != -1 ) {
							possibleGrids.add(temp);
						}
						else if ( mapApp.getMapType() == MapApp.WORLDWIND && sub_layer.proj.indexOf("g") != -1 ) {
							possibleGrids.add(temp);
						}
					}
					if (sub_layer.command.equals("wms_cmd")) {
						sub_layer.isRemote = "true";
					}
					if (sub_layer.command.equals("wms_usgs_quads_cmd")) {
						sub_layer.isRemote = "true";
					}
					if (sub_layer.command.equals("NASA_DEM_CMD")) {
						sub_layer.isRemote = "true";
					}
				}
				menus.add(sub_layer);
			}
		}
		return menus;
	}

	private static List<Node> getElements(Node node, String elementName) {
		List<Node> list = new LinkedList<Node>();
		NodeList nodeList = node.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++)
			if (nodeList.item(i).getNodeName().equals(elementName)
					&& nodeList.item(i).getNodeType() == Node.ELEMENT_NODE)
				list.add(nodeList.item(i));
		return list;
	}

	private static RequestLayer parseLayer(Node root) {
		RequestLayer layer = new RequestLayer();

		Node name = root.getAttributes().getNamedItem("name");
		layer.name = name != null ? name.getNodeValue() : null;

		Node style = root.getAttributes().getNamedItem("style");
		layer.style = style != null ? style.getNodeValue() : null;
		return layer;
	}

	public static void setStart(StartUp inputStart) {
		start = inputStart;
	}

	// Calls one XML list to load menus to main menu bar.
	public static JMenuBar createMainMenuBar(List<XML_Menu> menuLayers) {
		//clear old menu hash tables (if switching projection)
		clearMenus();
		
		JMenuBar mainMenuBar = new JMenuBar();
		for (XML_Menu sub_menu : menuLayers) {
			if ( mapApp.start != null ) {
				mapApp.start.setText("Initializing " + sub_menu.name + " Menu...");
			}
			if ( sub_menu.proj.toLowerCase().indexOf(MapApp.CURRENT_PROJECTION.toLowerCase()) != -1 ) {
				JMenu subMenu = createMenu(sub_menu);
				if (subMenu.getItemCount() != 0)
					mainMenuBar.add(subMenu);
					subMenu.addMenuListener(mapApp.listener);
			}
		}
		if ( mapApp != null && mapApp.start != null ) {
			mapApp.start.setText("Initializing GUI");
		}
		return mainMenuBar;
	}

	/* Calls Two XML list to load menus to main menu bar. One can be from server
	 * while one in on user desktop or another server.
	 */
	public static JMenuBar createMainMenuBars(List<XML_Menu> menuLayers, List<XML_Menu> menuLayersLocal) {
		JMenuBar mainMenuBar = new JMenuBar();
		for (XML_Menu sub_menu : menuLayers) {
			if ( mapApp.start != null ) {
				mapApp.start.setText("Initializing " + sub_menu.name + " Menu...");
			}
			if ( sub_menu.proj.toLowerCase().indexOf(MapApp.CURRENT_PROJECTION.toLowerCase()) != -1 ) {
				JMenu subMenu = createMenu(sub_menu);
				if (subMenu.getItemCount() != 0)
					mainMenuBar.add(subMenu);
					subMenu.addMenuListener(mapApp.listener);
			}
		}
		// Add My Sessions Menu
		for (XML_Menu sub_menu2 : menuLayersLocal) {
			if ( mapApp.start != null ) {
				mapApp.start.setText("Initializing " + sub_menu2.name + " Menu...");
			}
			if ( sub_menu2.proj.toLowerCase().indexOf(MapApp.CURRENT_PROJECTION.toLowerCase()) != -1 ) {
				JMenu subMenu2 = createMenu(sub_menu2);
				if (subMenu2.getItemCount() != 0)
					mainMenuBar.add(subMenu2);
					subMenu2.addMenuListener(mapApp.listener);
			}
		}

		if ( mapApp != null && mapApp.start != null ) {
			mapApp.start.setText("Initializing GUI");
		}
		return mainMenuBar;
	}

	// Creates Main Menus from xml
	public static JMenu createMenu(XML_Menu menuLayer) {
		JMenu menu = new JMenu(menuLayer.name);
		menuToMenuItem.put(menuLayer, menu);

		boolean isEnabled = false;

		for (XML_Menu sub_menu : menuLayer.child_layers) {
			if (sub_menu.proj != null) {
				//Don't do for imported sessions as we are now adding all sessions to the menu, and will
				//switch projection when loading.
				if ( !(menuLayer.parent != null && menuLayer.parent.name.equals("My Layer Sessions")) &&
						!menuLayer.name.equals("My Layer Sessions") &&
						!sub_menu.proj.toLowerCase().contains(MapApp.CURRENT_PROJECTION.toLowerCase())) {
					if ( sub_menu.separator_bar != null )
						menu.addSeparator();
					continue;
				}
			}
			if (sub_menu.os != null)
				if (!current_os.toLowerCase().contains(sub_menu.os.toLowerCase()))
					continue;

			if ( sub_menu.child_layers.size() > 0 ) {
				JMenu subMenu = createMenu(sub_menu);
				if (subMenu.getItemCount() != 0)
					menu.add(subMenu);

				isEnabled = isEnabled || subMenu.isEnabled();
			}
			else {
				JMenuItem mi = createMenuItem(sub_menu);

				isEnabled = isEnabled || mi.isEnabled();

				if ( sub_menu.separator_bar != null ) {
					if ( sub_menu.separator_bar.equals("above")) {
						menu.addSeparator();
						menu.add(mi);
					}
					else {
						menu.add(mi);
						menu.addSeparator();
					}
				}
				else {
					menu.add(mi);
				}
			}
		}

		if ("true".equalsIgnoreCase(menuLayer.isRemote) && MapApp.AT_SEA)
			menu.setEnabled(false);
		else
			menu.setEnabled(isEnabled);

		return menu;
	}

	// Creates the Menu Items belonging in each Menu from xml
	public static JMenuItem createMenuItem(XML_Menu menuItemLayer) {
		JMenuItem mi;
		if ( menuItemLayer.checkbox != null && menuItemLayer.checkbox.equals("true")) {
			mi = new JCheckBoxMenuItem(menuItemLayer.name, false);
		} else if( menuItemLayer.color != null && (menuItemLayer.name.contentEquals("") || menuItemLayer.name.contentEquals("Zoom To Saved Session"))) {

			String itemColor = menuItemLayer.color.toString();
			//System.out.println("Item Hex Color" + itemColor);
			Color c = Color.decode(itemColor);
			
			//Color textColor = Color.decode("#333333");
			mi = new JMenuItem(menuItemLayer.name );
			mi.setBackground(c);
			
			//mi.setEnabled(false);

		} else if(menuItemLayer.name != null && menuItemLayer.name.contains("Click For Searchable Tear-Off Menus")) {
			mi = new JMenuItem(menuItemLayer.name);
			
			// Color the background of certain items
			Color colorBackground1 = new Color(255,192,139);		// light orange
			if(menuItemLayer.command.contains("open_search_tree")) {
				mi.setBackground(colorBackground1);
			}
		} else {			
			mi = new JMenuItem(menuItemLayer.name);
		}

		if ( menuItemLayer.layer_url != null ) {
			mi.setName(menuItemLayer.layer_url);
		}

		mi.setActionCommand(menuItemLayer.command);
		mi.addActionListener(mapApp);

		if ( menuItemLayer.command != null ) {
			commandToMenuItemHash.put(menuItemLayer.command, mi);

			if (!MapApp.supported_commands.contains(menuItemLayer.command) &&
					!MapApp.portal_commands.contains(menuItemLayer.command))
				mi.setEnabled(false);
		}

		if ( menuItemLayer.shortcut != null ) {
			String keystroke;
			if (System.getProperty("os.name").toLowerCase().contains("mac"))
				keystroke = "meta ";
			else
				keystroke = "control released ";

			keystroke += menuItemLayer.shortcut.toUpperCase();

			mi.setAccelerator(KeyStroke.getKeyStroke( keystroke ));
		}

		if ("true".equalsIgnoreCase(menuItemLayer.isRemote) && MapApp.AT_SEA)
			mi.setEnabled(false);
		menuItemToMenu.put(mi, menuItemLayer);
		menuToMenuItem.put(menuItemLayer, mi);

		return mi;
	}

	public static JTree createTree(List<XML_Menu> menuLayers) {
		DefaultMutableTreeNode top = new DefaultMutableTreeNode();
		for (XML_Menu sub_menu : menuLayers) {
			if ( sub_menu.proj.toLowerCase().indexOf(MapApp.CURRENT_PROJECTION.toLowerCase()) != -1 &&
					!"open_search_tree".equals(sub_menu.command)) {
				top.add(createBranch(sub_menu));
			}
		}
		JTree tree = new JTree(top);
		return tree;
	}

	public static DefaultMutableTreeNode createBranch(XML_Menu menuLayer) {
		DefaultMutableTreeNode branch = new DefaultMutableTreeNode(menuLayer);
		for (XML_Menu sub_menu : menuLayer.child_layers) {
			if ( sub_menu.proj == null || sub_menu.proj.toLowerCase().indexOf(MapApp.CURRENT_PROJECTION.toLowerCase()) != -1 && ( sub_menu.os == null || current_os.toLowerCase().contains(sub_menu.os.toLowerCase()) ) ) {
				if ( sub_menu.child_layers.size() > 0 ) {
					branch.add(createBranch(sub_menu));
				}
				else {
					DefaultMutableTreeNode leafNode = new DefaultMutableTreeNode(sub_menu);
					branch.add(leafNode);
				}
			}
		}
		return branch;
	}

	public static void closeAllPaths(JTree tree) {
		TreePath pathToRoot = new TreePath(tree.getModel().getRoot());
		closePaths(tree, pathToRoot);
		if (!tree.isRootVisible())
			tree.expandPath(pathToRoot);
	}

	public static void closePaths(JTree tree, TreePath path) {
		Object node = path.getLastPathComponent();
		TreeModel model = tree.getModel();
		if (model.isLeaf(node))
			return;
		int num = model.getChildCount(node);
		for (int i = 0; i < num; i++)
			closePaths(tree, path.pathByAddingChild(model.getChild(node, i)));
		tree.collapsePath(path);
	}

	public static void setMapApp(haxby.map.MapApp inputMapApp) {
		mapApp = inputMapApp;
	}

	public String toString() {
		return name;
	}

	public static JFrame getTreeWindow(String urlString) {
		JFrame frame = new JFrame("");
		try {
			List<XML_Menu> menuList = parse(urlString);
			JTree tree = createTree(menuList);
			JScrollPane treeView = new JScrollPane(tree);
			frame.getContentPane().add(treeView,"Center");
			JTextField tf = new JTextField(20);
			frame.getContentPane().add(tf,"South");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.pack();
			frame.setVisible(true);
			tree.expandPath(new TreePath( ((DefaultMutableTreeNode)(tree.getModel().getRoot())).getChildAt(0).getChildAt(0) ) );
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		return frame;
	}

	/* Save each current session layer into a temp file for further processing.
	 * After processing saves to the xmlFile name and placed on desktop.
	 */
	public static void saveSessionLayer(XML_Menu item, File xmlFile) throws IOException {
		//Check the file first
		// boolean containsItemCheck = FilesUtil.containsItemCheck(item.name);
		// System.out.println("Does it contain same items already? " + containsItemCheck);
		//Write the information if it exists
		
		//Does the session have defined symbol sizes or colors?
		String symbol_shape = "";
		String symbol_allcolor = "";
		String symbol_size = "";
		boolean sst = false;
		String sst_col_ind = "";
		String sst_num_col_ind = "";
		String sst_range_start = "";
		String sst_range_end = "";
		boolean sst_flip = false;
		boolean cst = false;
		String cst_col_ind = "";
		String cst_num_col_ind = "";
		String cst_range_start = "";
		String cst_range_end = "";
		String cst_pal_r = "";
		String cst_pal_g = "";
		String cst_pal_b = "";
		String cst_pal_ht = "";
		String cst_discrete = "";
		String cst_tabs = "";

		boolean isGrid = false;
		String grid_which_pal = "";
		String grid_dpal_name = "";
		String grid_dpal_r = "";
		String grid_dpal_g = "";
		String grid_dpal_b = "";
		String grid_dpal_ht = "";
		String grid_dpal_discrete = "";
		String grid_dpal_range = "";
		String grid_opal_name = "";
		String grid_opal_r = "";
		String grid_opal_g = "";
		String grid_opal_b = "";
		String grid_opal_ht = "";
		String grid_opal_discrete = "";
		String grid_opal_range = "";
		String grid_lpal_name = "";
		String grid_lpal_r = "";
		String grid_lpal_g = "";
		String grid_lpal_b = "";
		String grid_lpal_ht = "";
		String grid_lpal_discrete = "";
		String grid_lpal_range = "";
		
		String grid_tabs = "";
		String grid_ve = "";
		boolean grid_illum = true;
		String grid_az = "";
		String grid_alt = "";
		boolean grid_contours = false;
		String grid_cont_int = "";
		String grid_cont_bolding = "";
		String grid_cont_min = "";
		String grid_cont_max = "";
		int lpIndex = 0;
		
		Vector<Overlay> overlays = mapApp.getMap().getOverlays();
		List<LayerPanel> layerpanels = mapApp.layerManager.getLayerPanels();
		ArrayList<LayerPanel> saveableLayers = mapApp.layerManager.saveableLayers;
		for (Overlay overlay : overlays) {	
			LayerPanel layerPanel = mapApp.layerManager.getLayerPanel(overlay);
			if (layerPanel != null && (layerPanel.layerName.equals(item.name) || layerPanel.layerName.equals(GridDialog.getShortName(item.name)))) 
				lpIndex = saveableLayers.indexOf(layerPanel);
			if (overlay instanceof ESRIShapefile || overlay instanceof Grid2DOverlay ) {				
				Grid2DOverlay grid;
				if (overlay instanceof ESRIShapefile)
					if (((ESRIShapefile) overlay).getMultiGrid() != null) {
						grid = ((ESRIShapefile) overlay).getMultiGrid().getGrid2DOverlay();
					} else continue;
				else {
					grid = (Grid2DOverlay) overlay;
					// if gmrt basemap, just continue
					if (grid.getName().matches(MapApp.baseFocusName)) continue;
				}
				//if this isn't our grid, then skip try next overlay
				if (!grid.getName().equals(item.name) && !(GridDialog.GRID_CMDS.containsKey(item.command) && GridDialog.GRID_CMDS.get(item.command).equals(grid.getName()))) {
					continue;
				}
				isGrid = true;
				
				grid_which_pal = grid.lut.whichPalette();
				grid_ve = Double.toString(grid.lut.getVE());
				if (grid.lut.getScaler() != null && grid.lut.getSunTool() != null ) {
					grid_tabs = GeneralUtils.array2String(grid.lut.getScaler().getTabs());
					grid_illum = grid.lut.getSunIllum();
					grid_az = Double.toString(grid.lut.getSunTool().getDeclination());
					grid_alt = Double.toString(grid.lut.getSunTool().getInclination());
					grid_contours = grid.lut.isContourSelected();
					grid_cont_int = Double.toString(grid.getInterval());
					grid_cont_bolding = Double.toString(grid.getBolding());
					grid_cont_min = Integer.toString(grid.cb[0]);
					grid_cont_max = Integer.toString(grid.cb[1]);
				}
			
				//get the RGB and HT values for the default, land and ocean palettes
				Palette dPal = grid.lut.getDefaultPalette();
				if (dPal != null) {
					grid_dpal_name = dPal.getName();
					grid_dpal_r = GeneralUtils.array2String(dPal.getRed());
					grid_dpal_g = GeneralUtils.array2String(dPal.getGreen());
					grid_dpal_b = GeneralUtils.array2String(dPal.getBlue());
					grid_dpal_ht = GeneralUtils.array2String(dPal.getHt());
					grid_dpal_discrete = Float.toString(dPal.getDiscrete());
					grid_dpal_range = GeneralUtils.array2String(dPal.getRange());
				}
				Palette lPal = grid.lut.getLandPalette();
				if (lPal != null) {
					grid_lpal_name = lPal.getName();
					grid_lpal_r = GeneralUtils.array2String(lPal.getRed());
					grid_lpal_g = GeneralUtils.array2String(lPal.getGreen());
					grid_lpal_b = GeneralUtils.array2String(lPal.getBlue());
					grid_lpal_ht = GeneralUtils.array2String(lPal.getHt());
					grid_lpal_discrete = Float.toString(lPal.getDiscrete());
					grid_lpal_range = GeneralUtils.array2String(lPal.getRange());
				}
				Palette oPal = grid.lut.getOceanPalette();
				if (oPal != null) {
					grid_opal_name = oPal.getName();
					grid_opal_r = GeneralUtils.array2String(oPal.getRed());
					grid_opal_g = GeneralUtils.array2String(oPal.getGreen());
					grid_opal_b = GeneralUtils.array2String(oPal.getBlue());
					grid_opal_ht = GeneralUtils.array2String(oPal.getHt());
					grid_opal_discrete = Float.toString(oPal.getDiscrete());
					grid_opal_range = GeneralUtils.array2String(oPal.getRange());
				}

			}
			if (overlay instanceof CustomDB || overlay instanceof UnknownDataSet) {
				Vector<UnknownDataSet> datasets = (overlay instanceof CustomDB)?(((CustomDB) overlay).dataSets):(new Vector<>(Arrays.asList(new UnknownDataSet[] {(UnknownDataSet)overlay})));
				for (UnknownDataSet ds : datasets) {
					if (ds.xml_menu != null && ds.xml_menu.name == item.name) {
						for (LayerPanel lp : layerpanels) {
							if (lp.layerName.equals(" Data Table: "+item.name)) {
								lpIndex = saveableLayers.indexOf(lp);
								break;
							}
						}
						if (ds.sst != null) {
							sst = true;
							sst_col_ind = Integer.toString(ds.getScaleColumnIndex());
							sst_num_col_ind = Integer.toString(ds.getScaleNumericalColumnIndex());
							sst_range_start = Float.toString(ds.sst.getRange()[0]);
							sst_range_end = Float.toString(ds.sst.getRange()[1]);
							sst_flip = ds.sst.scaler.flip;
						}
						if (ds.cst != null) {
							cst = true;
							cst_col_ind = Integer.toString(ds.getColorColumnIndex());
							cst_num_col_ind = Integer.toString(ds.getColorNumericalColumnIndex());
							if (ds.cst.getScaler() != null && ds.cst.getScaler().getRange() != null ) {
								cst_range_start = Float.toString(ds.cst.getScaler().getRange()[0]);
								cst_range_end = Float.toString(ds.cst.getScaler().getRange()[1]);
								cst_tabs = GeneralUtils.array2String(ds.cst.getScaler().getTabs());
							}
							Palette pal = ds.cst.getCurrentPalette();
							if (pal != null) {
								cst_pal_r = GeneralUtils.array2String(pal.getRed());
								cst_pal_g = GeneralUtils.array2String(pal.getGreen());
								cst_pal_b = GeneralUtils.array2String(pal.getBlue());
								cst_pal_ht = GeneralUtils.array2String(pal.getHt());
								cst_discrete = Float.toString(pal.getDiscrete());
							}
						}
						
						symbol_shape = ds.shapeString;
						symbol_allcolor = Integer.toString(ds.getColor().getRGB());
						symbol_size = Integer.toString(ds.symbolSize);
					}
				}
			}
		}
		
		 if ((item.name!= null)) {
			FilesUtil.writeLayerToFile('\r' + "\t" + "<layer" + '\r', xmlFile);
			String nameLayer = ('\t' + "\t" + "name=" + '"' + item.name + '"' + "\r");
			FilesUtil.writeLayerToFile(nameLayer, xmlFile);
		
			String indexLayer = ('\t' + "\t" + "index=" + '"' + lpIndex + '"' + "\r");
			FilesUtil.writeLayerToFile(indexLayer, xmlFile);
			
			if (symbol_shape != null) {
				String shapeLayer = ('\t' + "\t" + "symbol_shape=" + '"' + symbol_shape + '"' + "\r");
				FilesUtil.writeLayerToFile(shapeLayer, xmlFile);
			}
			if (symbol_allcolor != null) {
				String colorLayer = ('\t' + "\t" + "symbol_allcolor=" + '"' + symbol_allcolor + '"' + "\r");
				FilesUtil.writeLayerToFile(colorLayer, xmlFile);
			}
			if (symbol_size != null) {
				String sizeLayer = ('\t' + "\t" + "symbol_size=" + '"' + symbol_size + '"' + "\r");
				FilesUtil.writeLayerToFile(sizeLayer, xmlFile);
			}
			if (sst) {
				String sstLayer = ('\t' + "\t" + "sst=" + '"' + sst + '"' + "\r");
				FilesUtil.writeLayerToFile(sstLayer, xmlFile);
				String sstLayer2 = ('\t' + "\t" + "sst_col_ind=" + '"' + sst_col_ind + '"' + "\r");
				FilesUtil.writeLayerToFile(sstLayer2, xmlFile);
				String sstLayer3 = ('\t' + "\t" + "sst_num_col_ind=" + '"' + sst_num_col_ind + '"' + "\r");
				FilesUtil.writeLayerToFile(sstLayer3, xmlFile);
				String sstLayer4 = ('\t' + "\t" + "sst_range_start=" + '"' + sst_range_start + '"' + "\r");
				FilesUtil.writeLayerToFile(sstLayer4, xmlFile);
				String sstLayer5 = ('\t' + "\t" + "sst_range_end=" + '"' + sst_range_end + '"' + "\r");
				FilesUtil.writeLayerToFile(sstLayer5, xmlFile);
				String sstLayer6 = ('\t' + "\t" + "sst_flip=" + '"' + sst_flip + '"' + "\r");
				FilesUtil.writeLayerToFile(sstLayer6, xmlFile);
			}
			if (cst) {
				String cstLayer = ('\t' + "\t" + "cst=" + '"' + cst + '"' + "\r");
				FilesUtil.writeLayerToFile(cstLayer, xmlFile);
				cstLayer = ('\t' + "\t" + "cst_col_ind=" + '"' + cst_col_ind + '"' + "\r");
				FilesUtil.writeLayerToFile(cstLayer, xmlFile);
				cstLayer = ('\t' + "\t" + "cst_num_col_ind=" + '"' + cst_num_col_ind + '"' + "\r");
				FilesUtil.writeLayerToFile(cstLayer, xmlFile);
				cstLayer = ('\t' + "\t" + "cst_range_start=" + '"' + cst_range_start + '"' + "\r");
				FilesUtil.writeLayerToFile(cstLayer, xmlFile);
				cstLayer = ('\t' + "\t" + "cst_range_end=" + '"' + cst_range_end + '"' + "\r");
				FilesUtil.writeLayerToFile(cstLayer, xmlFile);
				cstLayer = ('\t' + "\t" + "cst_pal_r=" + '"' + cst_pal_r + '"' + "\r");
				FilesUtil.writeLayerToFile(cstLayer, xmlFile);
				cstLayer = ('\t' + "\t" + "cst_pal_g=" + '"' + cst_pal_g + '"' + "\r");
				FilesUtil.writeLayerToFile(cstLayer, xmlFile);
				cstLayer = ('\t' + "\t" + "cst_pal_b=" + '"' + cst_pal_b + '"' + "\r");
				FilesUtil.writeLayerToFile(cstLayer, xmlFile);
				cstLayer = ('\t' + "\t" + "cst_pal_ht=" + '"' + cst_pal_ht + '"' + "\r");
				FilesUtil.writeLayerToFile(cstLayer, xmlFile);
				cstLayer = ('\t' + "\t" + "cst_discrete=" + '"' + cst_discrete + '"' + "\r");
				FilesUtil.writeLayerToFile(cstLayer, xmlFile);
				cstLayer = ('\t' + "\t" + "cst_tabs=" + '"' + cst_tabs + '"' + "\r");
				FilesUtil.writeLayerToFile(cstLayer, xmlFile);
			}
			if (isGrid) {
				String gridLayer = ('\t' + "\t" + "grid=" + '"' + isGrid + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_which_pal=" + '"' + grid_which_pal + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_dpal_name=" + '"' + grid_dpal_name + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_dpal_r=" + '"' + grid_dpal_r + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_dpal_g=" + '"' + grid_dpal_g + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_dpal_b=" + '"' + grid_dpal_b + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_dpal_ht=" + '"' + grid_dpal_ht + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_dpal_discrete=" + '"' + grid_dpal_discrete + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_dpal_range=" + '"' + grid_dpal_range + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_lpal_name=" + '"' + grid_lpal_name + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_lpal_r=" + '"' + grid_lpal_r + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_lpal_g=" + '"' + grid_lpal_g + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_lpal_b=" + '"' + grid_lpal_b + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_lpal_ht=" + '"' + grid_lpal_ht + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_lpal_discrete=" + '"' + grid_lpal_discrete + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_lpal_range=" + '"' + grid_lpal_range + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_opal_name=" + '"' + grid_opal_name + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_opal_r=" + '"' + grid_opal_r + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_opal_g=" + '"' + grid_opal_g + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_opal_b=" + '"' + grid_opal_b + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_opal_ht=" + '"' + grid_opal_ht + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_opal_discrete=" + '"' + grid_opal_discrete + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_opal_range=" + '"' + grid_opal_range + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_tabs=" + '"' + grid_tabs + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_ve=" + '"' + grid_ve + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_illum=" + '"' + grid_illum + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_alt=" + '"' + grid_alt + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_az=" + '"' + grid_az + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_contours=" + '"' + grid_contours + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_cont_int=" + '"' + grid_cont_int + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_cont_bolding=" + '"' + grid_cont_bolding + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_cont_min=" + '"' + grid_cont_min + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
				gridLayer = ('\t' + "\t" + "grid_cont_max=" + '"' + grid_cont_max + '"' + "\r");
				FilesUtil.writeLayerToFile(gridLayer, xmlFile);
			}
			if (item.layer_url!= null) {
				// catch special char in url info string
				item.layer_url = item.layer_url.replace("&", "&amp;");
				String urlLayer = ('\t' + "\t" + "url=" + '"' + item.layer_url + '"' + "\r");
				FilesUtil.writeLayerToFile(urlLayer, xmlFile);
			}
			if (item.layer_url2!= null) {
				// catch special char in url info string
				item.layer_url2 = item.layer_url2.replace("&", "&amp;");
				String urlLayer2 = ('\t' + "\t" + "url2=" + '"' + item.layer_url2 + '"' + "\r");
				FilesUtil.writeLayerToFile(urlLayer2, xmlFile);
			}
			if (item.legend!= null) {
				// catch special char in url info string
				item.legend = item.legend.replace("&", "&amp;");
				String legendLayer = ('\t' + "\t" + "legend=" + '"' + item.legend + '"' + "\r");
				FilesUtil.writeLayerToFile(legendLayer, xmlFile);
			}
			if (item.infoURLString!= null) {
				// catch special char in url info string
				item.infoURLString = item.infoURLString.replace("&", "&amp;");
				String hrefLayer = ('\t' + "\t" + "href=" + '"' + item.infoURLString + '"'+ "\r");
				FilesUtil.writeLayerToFile(hrefLayer, xmlFile);
			}
			if (item.srs!= null) {
				String srsLayer = ('\t' + "\t" + "srs=" + '"' + item.srs + '"' + "\r");
				FilesUtil.writeLayerToFile(srsLayer, xmlFile);
			}
			if (item.type!= null) {
				String typeLayer = ('\t' + "\t" + "type=" + '"' + item.type + '"' + "\r");
				FilesUtil.writeLayerToFile(typeLayer, xmlFile);
			}
			if (item.opacity_value != null) {
				String opacityLayer =('\t' + "\t" + "opacity=" + '"' + item.opacity_value + '"' + "\r");
				FilesUtil.writeLayerToFile(opacityLayer, xmlFile);
			}
			if (item.warning!= null) {
				String warningLayer = ('\t' + "\t" + "warning=" + '"' + item.warning + '"' + "\r");
				FilesUtil.writeLayerToFile(warningLayer, xmlFile);
			}
			if (item.display_layer != null) {
				String displayLayer =('\t' + "\t" + "display=" + '"' + item.display_layer + '"' + "\r");
				FilesUtil.writeLayerToFile(displayLayer, xmlFile);
			}
			if (item.wesn!= null) {
				String wesnLayer = ('\t' + "\t" + "wesn=" + '"' + item.wesn + '"' + "\r");
				FilesUtil.writeLayerToFile(wesnLayer, xmlFile);
			}
			if ((item.proj)!= null) {
				String projLayer = ('\t' + "\t" + "proj=" + '"' + item.proj + '"' + "\r");
				FilesUtil.writeLayerToFile(projLayer, xmlFile);
			}
			if ((item.checkbox)!= null) {
				String checkBoxLayer = ('\t' + "\t" + "checkbox=" + '"' + item.checkbox+ '"' + "\r");
				FilesUtil.writeLayerToFile(checkBoxLayer, xmlFile);
			}
			if ((item.multipleshapes)!= null) {
				String multiShapeLayer = ('\t' + "\t" + "multipleshapes=" + '"' + item.multipleshapes+ '"' + "\r");
				FilesUtil.writeLayerToFile(multiShapeLayer, xmlFile);
			}
			if ((item.mapproj) != null ) {
				String mapProjLayer = ('\t' + "\t" + "mapproj=" + '"' + item.mapproj + '"' + "\r");
				FilesUtil.writeLayerToFile(mapProjLayer, xmlFile);
			}
			if ((item.color)!= null) {
				String colorLayer = ('\t' + "\t" + "color=" + '"' + item.color + '"' + "\r");
				FilesUtil.writeLayerToFile(colorLayer, xmlFile);
			}
			if ((item.wfs_layer_feature)!= null) {
				String wfsFeatureLayer = ('\t' + "\t" + "wfs_feature=" + '"' + item.wfs_layer_feature + '"' + "\r");
				FilesUtil.writeLayerToFile(wfsFeatureLayer, xmlFile);
			}
			if ((item.wfs_bbox)!= null) {
				String wfsBboxLayer = ('\t' + "\t" + "wfs_bbox=" + '"' + item.wfs_bbox + '"' + "\r");
				FilesUtil.writeLayerToFile(wfsBboxLayer, xmlFile);
			}
			//Should be the last item so we can close the beginning layer tag
			if(item.command!= null) {
				String commandLayer = ('\t' + "\t" + "command=" + '"' + item.command + '"' + ">" + "\r");
				FilesUtil.writeLayerToFile(commandLayer, xmlFile);

					//If the command is wms will write the wms_layer tabs
					if (( item.command.equals("wms_cmd"))&& (item.layers.length !=0)) {
						FilesUtil.writeLayerToFile('\t' + "<wms_layer" + "\r", xmlFile);
						String wmsNameLayer = ('\t' + "\t"  + "name=" + '"'
											+ item.layers[0].name + '"' + "\r");
						FilesUtil.writeLayerToFile(wmsNameLayer, xmlFile);
						if(item.style !=null){
							String wmsStyleLayer = ('\t' + "\t"  + "style=" + '"'
									+ item.layers[0].style + '"' + "/>" + "\r");
							FilesUtil.writeLayerToFile(wmsStyleLayer, xmlFile);
						}else if(item.style == null){
							String wmsStyleLayer = ('\t' + "\t" + "style=" + '"'+'"' + "/>" + "\r");
							FilesUtil.writeLayerToFile(wmsStyleLayer, xmlFile);
						}
					}
				}

		//Close Layer tab
		FilesUtil.writeLayerToFile('\t' + "</layer>" + "\r", xmlFile);
		}
	} // end saveSessionLayer()

	public static JMenuItem getMenutItem(String commandKey) {
		if (commandToMenuItemHash == null) return null;
		return commandToMenuItemHash.get(commandKey);
	}

	public static JMenuItem getMenuItem(XML_Menu menu) {
		if (menuToMenuItem == null) return null;
		return menuToMenuItem.get(menu);
	}

	public static XML_Menu getXML_Menu(JMenuItem mi) {
		if (menuItemToMenu == null) return null;
		return menuItemToMenu.get(mi);
	}
	
	/* 
	 * read in the root layer name - originally created to read in session import files
	 */
	public static String getRootName(File f) throws ParserConfigurationException, SAXException, IOException {
		InputStream in = new BufferedInputStream(new FileInputStream(f));
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringComments(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document dom = db.parse(in);
		Node root = dom.getFirstChild();
		return root.getAttributes().getNamedItem("name").getTextContent();
	}
	
	/*
	 * Return the XML_Menu with the given name
	 */
	public static XML_Menu getXML_Menu(String name) {
		if (menuItemToMenu == null) return null;

		Set<XML_Menu> menus = menuToMenuItem.keySet();
		//There can be multiple menu items with the same name, if global grids are in the My Layer Sessions menu.
		//We (probably) want the one that isn't in that menu.
		for (XML_Menu menu : menus) {
			if (menu.name != null && (menu.name.equals(name) || (menu.name.equals(GridDialog.GRID_SHORT_TO_LONG_NAMES.get(name))))) {
					//check that grandparent is not "My Layer Sessions"
					if (menu.parent == null || !menu.parent.parent.name.equals("My Layer Sessions")) {
						return menu;
					}
			}
		}
		return null;
	}
}
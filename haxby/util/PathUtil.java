package haxby.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import haxby.map.MapApp;
/**
 * Retrieves specific urls and/or paths from a .xml file at a 
 * remote location defined by pathURL.
 * 
 * @author Justin Coplan
 * @version 2.3.1
 * @since 2.0.0
 */
public class PathUtil {
	private static String pathURL;
	static 
	{
		pathURL = System.getProperty("geomapapp.paths_location");
		if (pathURL == null)
			try {
				pathURL = URLFactory.url(MapApp.DEFAULT_URL) + "gma_paths/GMA_paths.xml";
			} catch (MalformedURLException e) {
				System.err.println("Can't find GMA_paths.xml file");
			}
	}

	private static Map<String, String> keyToURL;
	private static Map<String, String> keyToURL2;

	public static String getPath(String key) {
		if (keyToURL == null) loadPaths();
		String url = keyToURL.get(key.toLowerCase());
		// if running in dev mode, replace any production URLs with the dev version
		if (url != null && MapApp.BASE_URL != null && MapApp.BASE_URL.equals(MapApp.DEV_URL)) {
			url = url.replace(MapApp.PRODUCTION_URL, MapApp.DEV_URL);
		}
		if (url != null) return url;
		System.err.println("KEY: " + key + " not found");
		return null;
	}

	public static String getPath(String key, String defaultURL) {
		if (keyToURL == null) loadPaths();
		String url = keyToURL.get(key.toLowerCase());
		// if running in dev mode, replace any production URLs with the dev version
		if (url != null && MapApp.BASE_URL != null && MapApp.BASE_URL.equals(MapApp.DEV_URL)) {
			url = url.replace(MapApp.PRODUCTION_URL, MapApp.DEV_URL);
		}
		if (url != null) return url;
		System.err.println("KEY: " + key + " not found\n Using: " + defaultURL);

		if (!defaultURL.contains("http")) {
			//defaultURL could also be a key
			url = keyToURL.get(defaultURL.toLowerCase());
			if (url != null) return url;
			defaultURL = getPath("ROOT_PATH", MapApp.BASE_URL) + defaultURL;
		}
		keyToURL.put(key, defaultURL);
		return defaultURL;
	}

	public static void setPathURL(String url) {
		pathURL = url;
	}

	private static void loadPaths() {
		keyToURL = new HashMap<String, String>();
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setIgnoringComments(true);

			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse( URLFactory.url(pathURL).openStream() );
			
			Node root = getElement(dom, "GMA_paths");
			for (Node layer : getElements(root, "layer"))
				addLayer(layer, null);

		} catch (IOException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(MapApp.anchor, "Could not connect to the GeoMapApp server. It's likely that you have internet issues or the server is down.", "", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		String rootPath = getPath("ROOT_PATH", MapApp.BASE_URL);

		for (String key : keyToURL.keySet()) {
			String url = keyToURL.get(key);
			if (url.contains("http"))
				continue;

			keyToURL.put(key, rootPath + url);
		}
	}

	// Loads another GMA paths file from another location
	public static void loadNewPaths(String path) {
		keyToURL2 = new HashMap<String, String>();

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setIgnoringComments(true);

			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse( URLFactory.url(path).openStream() );

			Node root = getElement(dom, "GMA_paths");
			for (Node layer : getElements(root, "layer"))
				addLayer(layer, null);

		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		String rootPath = getPath("ROOT_PATH", MapApp.BASE_URL);

		for (String key : keyToURL2.keySet()) {
			String url = keyToURL2.get(key);
			System.out.println("np url"+ url);
			if (url.contains("http://") || url.contains("https://"))
				continue;

			keyToURL2.put(key, rootPath + url);
		}
	}

	private static void addLayer(Node layer, String path) {
		String name = layer.getAttributes().getNamedItem("name").getNodeValue();
		if (path != null)
			name = path + name;

		Node urlNode =layer.getAttributes().getNamedItem("url");
		if (urlNode != null) {
			String url = urlNode.getNodeValue();
			keyToURL.put(name.toLowerCase(), url);
		}

		name = name + "/";
		for (Node childLayer : getElements(layer, "layer"))
			addLayer(childLayer, name);
	}

	private static List<Node> getElements(Node node, String elementName) {
		List<Node> list = new LinkedList<Node>();
		NodeList nodeList = node.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (nodeList.item(i).getNodeName().equals(elementName)
					&& nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
				list.add(nodeList.item(i));
			}	
		}
		return list;
	}

	private static Node getElement(Node node, String elementName) {
		NodeList nodeList = node.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (nodeList.item(i).getNodeName().equals(elementName)
					&& nodeList.item(i).getNodeType() == Node.ELEMENT_NODE)
				return nodeList.item(i);
		}
		return null;
	}

	/*
	 * replace any placeholders in path names with variables, eg version numbers
	 */
	public static void replacePlaceHolder (String placeHolder, String var) {
		for (Map.Entry<String, String> entry : keyToURL.entrySet()) {
			entry.setValue(entry.getValue().replace(placeHolder, var));
		}
	}
}

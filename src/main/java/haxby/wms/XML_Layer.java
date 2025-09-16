package haxby.wms;

import haxby.map.MapApp;
import haxby.util.ConnectionWrapper;
import haxby.util.URLFactory;
import haxby.util.ProcessingDialog.StartStopTask;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.geomapapp.util.XML_Menu;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XML_Layer {

	public String name;
	public List<XML_Layer> folders = new LinkedList<XML_Layer>();
	public List<Request> maps = new LinkedList<Request>(); 

	public static class Request {
		public String name;
		public String url;
		public String srs;
		public int mapRes = Integer.MAX_VALUE;
		public double[] wesn;
		public RequestLayer[] layers;
	}

	public static class RequestLayer {
		public String name;
		public String style;
	}

	public static XML_Layer parse(String url) throws MalformedURLException,
			IOException, ParserConfigurationException, SAXException {
		return parse(URLFactory.url(url));
	}

	public static XML_Layer parse(URL url) throws IOException,
			ParserConfigurationException, SAXException {
		return parse(new BufferedInputStream(url.openStream()));
	}

	public static XML_Layer parse(File f) throws ParserConfigurationException,
			SAXException, IOException {
		return parse(new BufferedInputStream(new FileInputStream(f)));
	}

	public static XML_Layer parse(InputStream in)
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringComments(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document dom = db.parse(in);

		Node root = getElement(dom, "Layers");
		XML_Layer layer = parseFolder(root);
		layer.name = "Basemaps";
		return layer;
	}

	private static XML_Layer parseFolder(Node root) {
		if (root == null)
			return null;
		XML_Layer layer = new XML_Layer();

		NodeList nodes = root.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE)
				continue;

			if (node.getNodeName().equals("Folder"))
				layer.folders.add(parseFolder(node));
			else if (node.getNodeName().equals("Map"))
				layer.maps.add(parseMap(node));
		}

		Node name = root.getAttributes().getNamedItem("name");
		layer.name = name != null ? name.getNodeValue() : null;

		return layer;
	}

	private static Request parseMap(Node root) {
		Request request = new Request();

		Node name = root.getAttributes().getNamedItem("name");
		request.name = name != null ? name.getNodeValue() : null;

		Node url = root.getAttributes().getNamedItem("url");
		request.url = url != null ? url.getNodeValue() : null;

		Node srs = root.getAttributes().getNamedItem("srs");
		request.srs = srs != null ? srs.getNodeValue() : null;

		Node wesn = root.getAttributes().getNamedItem("wesn");
		if (wesn != null) {
			String[] s = wesn.getNodeValue().split(",");

			request.wesn = new double[4];
			for (int i = 0; i < 4; i++)
				request.wesn[i] = Double.parseDouble(s[i]);
		}

		List<Node> layers = getElements(root, "wms_layer");
		Iterator<Node> iter = layers.iterator();
		request.layers = new RequestLayer[layers.size()];
		for (int i = 0; i < layers.size(); i++)
			request.layers[i] = parseLayer(iter.next());

		return request;
	}

	private static RequestLayer parseLayer(Node root) {
		RequestLayer layer = new RequestLayer();

		Node name = root.getAttributes().getNamedItem("name");
		layer.name = name != null ? name.getNodeValue() : null;

		Node style = root.getAttributes().getNamedItem("style");
		layer.style = style != null ? style.getNodeValue() : null;

		return layer;
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

	private static Node getElement(Node node, String elementName) {
		NodeList nodeList = node.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (nodeList.item(i).getNodeName().equals(elementName)
					&& nodeList.item(i).getNodeType() == Node.ELEMENT_NODE)
				return nodeList.item(i);
		}
		return null;
	}

	public static JMenu buildMapsMenu(final MapApp mapApp, String url) {
		XML_Layer layers = null;
		try {
			layers = parse(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		if (layers == null)
			return new JMenu("Basemaps");

		JMenu root = createMenu(layers, mapApp);

		JMenuItem baseMap = new JMenuItem("GeoMapApp");
		baseMap.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mapApp.addWMSLayer(null, (double[])null, null);
			}
		});
		root.insert(baseMap, 0);
		return root;
	}

	private static JMenu createMenu(XML_Layer root, final MapApp mapApp) {
		JMenu rootMenu = new JMenu(root.name);

		for (XML_Layer child : root.folders)
			rootMenu.add(createMenu(child, mapApp));

		for (Request request : root.maps)
			rootMenu.add(createMenuItem(request, mapApp));

		return rootMenu;
	}

	public static void accessWMS(final MapApp mapApp,
							final String requestSRS,
							final String requestURL,
							final RequestLayer[] requestLayers,
							final String requestName,
							final String infoURLString,
							final double[] requestWESN, 
							final int mapRes) {
		accessWMS(mapApp, requestSRS, requestURL, requestLayers, requestName,
				infoURLString, requestWESN, mapRes, null);
	}

	public static void accessWMS(final MapApp mapApp,
							final String requestSRS,
							final String requestURL,
							final RequestLayer[] requestLayers,
							final String requestName,
							final String infoURLString,
							final double[] requestWESN,
							final int mapRes,
							ConnectionWrapper wrapper) {
		accessWMS(mapApp, requestSRS, requestURL, requestLayers, requestName,
				infoURLString, requestWESN, mapRes, null, null);
	}

	public static void accessWMS(final MapApp mapApp,
							final String requestSRS,
							final String requestURL,
							final RequestLayer[] requestLayers,
							final String requestName,
							final String infoURLString,
							final double[] requestWESN, 
							final int mapRes,
							final XML_Menu xml_item,
							ConnectionWrapper wrapper) {
		if (requestSRS.equals("EPSG:4326") ||
				requestSRS.equals("EPSG:3031")) {
			Capabilities cap = null;
			try {
				cap = CapabilitiesParser.parseCapabilities(requestURL, wrapper);
			} catch (SAXException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (ParserConfigurationException e1) {
				e1.printStackTrace();
			} catch (FactoryConfigurationError e1) {
				e1.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			} 
			if (cap == null)
				return;

			String[] layers = new String[requestLayers.length];
			String[] styles = new String[layers.length];

			String legendURLs[][] = new String[layers.length][];
			boolean hasLegend = false;
			for (int i = 0; i < layers.length; i++) {
				layers[i] = requestLayers[i].name;
				styles[i] = requestLayers[i].style;
				if (styles[i] == null)
					styles[i] = "";
				Style s = cap.getStyle(layers[i], styles[i]);
				if (s == null) {
					s = searchChildLayersForLegend(cap.getLayer(), requestLayers[i].name);
				}
				if (s == null) continue;
				legendURLs[i] = s.getLegendURLs();
				hasLegend = hasLegend || legendURLs[i] != null;
			}
			// if there us a legend, add the URL to the xml_item, where it can be picked up later
			if (hasLegend) xml_item.legend = legendURLs[0][0];
			String url = cap.getLayerURL(layers, styles, requestSRS);

			mapApp.addWMSLayer(requestName, url, infoURLString, requestWESN,
					requestSRS, mapRes, xml_item);

			// Make a WMS Legend Dialog
			if (hasLegend) {
				new WMSLegendDialog(mapApp.getFrame(), legendURLs, requestName);
			}
		}
	}

	/*
	 * recursive method to search child layers for WMS legend
	 */
	private static Style searchChildLayersForLegend(Layer layer, String name) {
		Style s = null;
		Layer[] childLayers = layer.getChildren();
		for ( int m = 0; m < childLayers.length; m++ ) {
			s = searchChildLayersForLegend(childLayers[m], name);
			if (s != null) return s; 
			if (childLayers[m].getName() != null && childLayers[m].getName().equals(name)) {
				for ( int j = 0; j < childLayers[m].getStyles().length; j++ ) {
					for ( int k = 0; k < childLayers[m].getStyles()[j].getLegendURLs().length; k++ ) {
						if ( childLayers[m].getStyles()[j].getLegendURLs()[k] != null ) {
							s = childLayers[m].getStyles()[j];
							return s;
						}
					}
				}
			} 
		}

		return s;
	}
	private static JMenuItem createMenuItem(final Request request,
			final MapApp mapApp) {
		JMenuItem mi = new JMenuItem(request.name);

		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mapApp.addProcessingTask("WMS: " + request.name, new Runnable() {
					public void run() {
						XML_Layer.accessWMS(mapApp, request.srs,
								request.url, request.layers,
								request.name, request.url,
								request.wesn, request.mapRes);
					}
				});
			}
		});
		return mi;
	}

	public static StartStopTask accessWMSTask(final MapApp mapApp, 
			final String srs,
			final String layer_url, 
			final RequestLayer[] layers, 
			final String name,
			final String infoURLString, 
			final double[] inputWESN,
			final int mapRes,
			final XML_Menu xml_item) {
		final ConnectionWrapper wrapper = new ConnectionWrapper();
		//System.out.println("xml_item" + xml_item.name);
		return new StartStopTask() {
			public void run() {
				XML_Layer.accessWMS(mapApp, 
					srs, 
					layer_url, 
					layers, 
					name, 
					infoURLString, 
					inputWESN,
					mapRes,
					xml_item,
					wrapper);
			}

			public void stop() {
				synchronized (wrapper) {
					if (wrapper.connection != null)
						wrapper.connection.abort();
				}
			}
		};
	}
}
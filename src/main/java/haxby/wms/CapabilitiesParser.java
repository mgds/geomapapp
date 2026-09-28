package haxby.wms;

import haxby.util.ConnectionWrapper;
import haxby.util.URLFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CapabilitiesParser {
	/**
	 * Parses capabilities from the XML returned from URL
	 * @param url the url for the GetCapabilities page of the Web Mapping Service
	 * @throws FactoryConfigurationError
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 * @throws URISyntaxException
	 */
	public static Capabilities parseCapabilities(URL url) throws SAXException, IOException, ParserConfigurationException, FactoryConfigurationError, URISyntaxException {
		return parseCapabilities(url, null);
	}

	public static Capabilities parseCapabilities(URL url,
			ConnectionWrapper wrapper) throws SAXException, IOException,
			ParserConfigurationException, FactoryConfigurationError,
			URISyntaxException {
		Iterator iter;
		Node node;

		Capabilities capabilities = new Capabilities();
		capabilities.setRequestURL(url);

		// Parse the XML
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringComments(true);
		DocumentBuilder db = dbf.newDocumentBuilder();

		Document dom = null;
		HttpClient client = new HttpClient();
		HostConfiguration hostConfig = client.getHostConfiguration();
		URI uri = url.toURI();
		List<Proxy> list = ProxySelector.getDefault().select(uri);
		Iterator<Proxy> proxyIter = list.iterator();
		while(proxyIter.hasNext()) {
			Proxy p = proxyIter.next();
			InetSocketAddress addr = (InetSocketAddress) p.address();

			if (addr == null)
				hostConfig.setProxyHost(null);
			else
				hostConfig.setProxy(addr.getHostName(), addr.getPort());

			HttpMethod method = new GetMethod(uri.toString());
			if (wrapper != null)
				synchronized (wrapper) {
					wrapper.connection = method;
				}

			try {
				int sc = client.executeMethod(hostConfig, method);

				if (sc != HttpStatus.SC_OK)
					throw new IOException("Status Code: " + sc);
				
				dom = db.parse(method.getResponseBodyAsStream());
				method.releaseConnection();
				break;
			} catch (IOException ex) {
				if (!proxyIter.hasNext())
					throw ex;
				continue;
			}
		}
		// Determines if the tag relates to version 1.1.0 or 1.3.0
		Node root;
		if(getElement(dom, "WMS_Capabilities") != null) {
			root = getElement(dom, "WMS_Capabilities");
		} else {
			root = getElement(dom, "WMT_MS_Capabilities");
		}
		// set version
		Node version = root.getAttributes().getNamedItem("version");
		String versionNum = version.getNodeValue();
		capabilities.setVersion(versionNum);
		//System.out.println("num: " + versionNum);

		Node serviceNode = getElement(root, "Service");
		Node capabilityNode = getElement(root, "Capability");

		// Read the Service Title && Abstract
		node = getElement(serviceNode, "Title");
		capabilities.setServiceTitle( getText(node) ); 
		node = getElement(serviceNode, "Abstract");
		if (node != null) 
			capabilities.setDescription( getText(node) );

		// Read the supported formats
		Node requestNode = getElement(capabilityNode, "Request");
		Node getMapNode = getElement(requestNode, "GetMap");

		// Check for alternative request URL
		Node dcpTypeNode = getElement(getMapNode, "DCPType");
		if (dcpTypeNode != null) {
			Node httpNode = getElement(dcpTypeNode, "HTTP");
			Node getNode = getElement(httpNode, "Get");
			boolean get = true;
			if (getNode == null) {
				getNode = getElement(httpNode, "Post");
				get = false;
			}
			Node onlineResource = getElement(getNode, "OnlineResource");
			Node href = onlineResource.getAttributes().getNamedItem("xlink:href");
			String hrefStr = href.getNodeValue();
			if (!hrefStr.contains("?"))
				hrefStr = hrefStr + "?";
			else if (!hrefStr.endsWith("&"))
				hrefStr = hrefStr + "&";
			capabilities.setRequestURL(URLFactory.url(hrefStr));
			capabilities.setIsGet(get);
		}

		List formatNodes = getElements(getMapNode, "Format");
		String[] supportedFormats = new String[formatNodes.size()];
		capabilities.setSupportedFormats(supportedFormats);

		iter = formatNodes.iterator();
		for (int i = 0; i < supportedFormats.length; i++)
			supportedFormats[i] = getText((Node) iter.next());

		//Read layers
		Node layerNode = getElement(capabilityNode, "Layer");

		Style[] styles = null;
		List styleNodes = getElements(layerNode, "Style");
		if (styleNodes.size() != 0) {
			styles = new Style[styleNodes.size()];
			for (int i = 0; i < styles.length; i++)
				styles[i] = parseStyle((Node) iter.next());
		}

		if (layerNode != null) {
			Layer layer = parseLayer(layerNode);
			capabilities.setLayer(layer);
		}
		return capabilities;
	}

	public static Capabilities parseCapabilities(String url) throws SAXException, IOException, ParserConfigurationException, FactoryConfigurationError, URISyntaxException {
		return parseCapabilities(url, null);
	}

	public static Capabilities parseCapabilities(String url, ConnectionWrapper wrapper) throws SAXException, IOException, ParserConfigurationException, FactoryConfigurationError, URISyntaxException {
		if (url.indexOf("?") != -1)
			url = url.substring(0, url.indexOf("?") + 1);
		else
			url += "?";
		url += "REQUEST=GetCapabilities&";
		url += "VERSION=1.1.1&";
		url += "SERVICE=WMS&";
		return parseCapabilities(URLFactory.url(url), wrapper);
	}

	private static Layer parseLayer(Node layerNode) {
		Layer layer = new Layer();

		// Read Name && Title && Abstract;
		Node name = getElement(layerNode, "Name");
		if (name != null)
			layer.setName( getText(name) );
		Node title = getElement(layerNode, "Title");
		layer.setTitle( getText(title) );
		Node description = getElement(layerNode, "Abstract");
		if (description != null)
			layer.setDescription( getText(description) );

		// Check for 1.3.0
		Node exGeoBBNode = getElement(layerNode, "EX_GeographicBoundingBox");

		if (exGeoBBNode != null) {
			Node wbl = getElement(exGeoBBNode,"westBoundLongitude"); // order in xml wesn
			Node ebl = getElement(exGeoBBNode,"eastBoundLongitude");
			Node sbl = getElement(exGeoBBNode,"southBoundLatitude");
			Node nbl = getElement(exGeoBBNode,"northBoundLatitude");

			double wesn[] = new double[4];			// order for crs wsen
			wesn[0] = Double.parseDouble(getText(wbl));	// minx
			wesn[1] = Double.parseDouble(getText(sbl));	// maxx
			wesn[2] = Double.parseDouble(getText(ebl));	// miny
			wesn[3] = Double.parseDouble(getText(nbl));	// maxy
			layer.setWesn(wesn);
			layer.setLatLonBoundingBox(true);
		} 

		// Read the LatLonBoundingBox 1.1.1
		Node latLon = getElement(layerNode, "LatLonBoundingBox");
		if (latLon != null) {
			NamedNodeMap attr = latLon.getAttributes();
			double wesn[] = new double[4];
			wesn[0] = Double.parseDouble(attr.getNamedItem("minx").getNodeValue());
			wesn[1] = Double.parseDouble(attr.getNamedItem("maxx").getNodeValue());
			wesn[2] = Double.parseDouble(attr.getNamedItem("miny").getNodeValue());
			wesn[3] = Double.parseDouble(attr.getNamedItem("maxy").getNodeValue());
			layer.setWesn(wesn);
			layer.setLatLonBoundingBox(true);
		}

		// Read the SRS version 1.1
		List srsTypes = new LinkedList();
		List srsNodes = getElements(layerNode, "SRS");

		if (srsNodes.size() != 0) {
			for (Iterator iter = srsNodes.iterator(); iter.hasNext();) {
				String srsText = getText((Node) iter.next());
				if (srsText != null) {
					StringTokenizer st = new StringTokenizer(srsText);	
					while (st.hasMoreTokens()) {
						srsTypes.add(st.nextToken());
					}
				}
			}

			String[] srs = new String[srsTypes.size()];
			Iterator iter = srsTypes.iterator();
			for (int i = 0; i < srs.length; i++) {
				srs[i] = (String) iter.next();
			}
			layer.setSrs(srs);
		}

		// Read the CRS version 1.3.0
		List crsTypes = new LinkedList();
		List crsNodes = getElements(layerNode, "CRS");

		if (crsNodes.size() != 0) {
			for (Iterator iter2 = crsNodes.iterator(); iter2.hasNext();) {
				StringTokenizer st = new StringTokenizer( 
						getText((Node) iter2.next()));

				while (st.hasMoreTokens()) {
					crsTypes.add(st.nextToken());
				}
			}

			String[] crs = new String[crsTypes.size()];
			Iterator iter2 = crsTypes.iterator();
			for (int i = 0; i < crs.length; i++) {
				crs[i] = (String) iter2.next();
				//System.out.println("c " + crs[i]);
			}
			layer.setSrs(crs);
		}
/*else {
			System.out.println("empty");
			if (crsNodes.size() != 0) {
				for (Iterator iter2 = srsNodes.iterator(); iter2.hasNext();) {
					StringTokenizer st = new StringTokenizer( 
							getText((Node) iter2.next()));

					while (st.hasMoreTokens()) {
						crsTypes.add(st.nextToken());
					}
				}

				String[] crs = new String[crsTypes.size()];
				Iterator iter2 = crsTypes.iterator();
				for (int i = 0; i < crs.length; i++) {
					crs[i] = (String) iter2.next();
				}
				layer.setSrs(crs);
			} 
		}
*/
		// Read the DataURL
		List dataURL = getElements(layerNode, "DataURL");
		if (dataURL.size() > 0) {
			String urls[] = new String[dataURL.size()];
			int i = 0;
			for (Object node : dataURL) {
				Node resource = getElement( (Node) node, "OnlineResource");
				Node href = resource.getAttributes().getNamedItem("xlink:href");
				urls[i++] = href.getNodeValue();
			}
			layer.setDataURLs(urls);
		}

		// Read the MetadataURL
		List metadataURL = getElements(layerNode, "MetadataURL");
		if (metadataURL.size() > 0) {
			String metaUrls[] = new String[metadataURL.size()];
			int i = 0;
			for (Object node : metadataURL) {
				Node resource = getElement( (Node) node, "OnlineResource");
				Node href = resource.getAttributes().getNamedItem("xlink:href");
				metaUrls[i++] = href.getNodeValue();
			}
			layer.setMetadataURLs(metaUrls);
		}

		// Read the Styles
		List styleNodes = getElements(layerNode, "Style");
		if (styleNodes.size() != 0) {
			Style[] styles = new Style[styleNodes.size()];
			Iterator iter = styleNodes.iterator();
			for (int i = 0; i < styles.length; i++)
				styles[i] = parseStyle((Node) iter.next());

			layer.setStyles(styles);
		}

		// Read the Layers
		List layerNodes = getElements(layerNode, "Layer");
		if (layerNodes.size() != 0) {
			Layer[] layers = new Layer[layerNodes.size()];
			Iterator iter = layerNodes.iterator();
			for (int i = 0; i < layers.length; i++) 
				layers[i] = parseLayer((Node) iter.next());

			layer.setChildren(layers);
		}
		return layer;
	}

	private static Style parseStyle(Node styleNode) {
		Style style = new Style();

//		 Read Name && Title && Abstract && LegendURL && DataURL;
		Node name = getElement(styleNode, "Name");
		style.setName( getText(name) );

		Node title = getElement(styleNode, "Title");
		style.setTitle( getText(title) );

		Node description = getElement(styleNode, "Abstract");
		if (description != null)
			style.setDescription( getText(description) );

		List legendURLs = getElements(styleNode, "LegendURL");
		if (legendURLs.size() > 0) {
			String urls[] = new String[legendURLs.size()];

			int i = 0;
			for (Object node : legendURLs) {
				Node format = getElement((Node) node, "Format");
				if (getText(format).contains("image")) {
					Node resource = getElement((Node) node, "OnlineResource");
					Node href = resource.getAttributes().getNamedItem("xlink:href");

					urls[i++] = href.getNodeValue();
				}
				else {
					i++;
				}
			}
			style.setLegendURL(urls);
		}

		Node styleURL = getElement(styleNode, "StyleURL");
		if (styleURL != null) {
			Node resource = getElement(styleURL, "OnlineResource");
			Node href = resource.getAttributes().getNamedItem("xlink:href");

			style.setStyleURL( href.getNodeValue() );
		}
		return style;
	}

	private static List getElements(Node node, String elementName) {
		List list = new LinkedList();
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

	private static String getText(Node node) {
		NodeList nodeList = node.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (nodeList.item(i).getNodeName().equals("#text") ||
					nodeList.item(i).getNodeName().equals("#cdata-section")) {
				return nodeList.item(i).getNodeValue();
			}
		}
		return null;
	}

	public static void main(String[] args) throws Exception {
		Capabilities cap = 
			parseCapabilities("https://www.gmrt.org/services/mapserver/wms_merc?request=GetCapabilities&service=WMS&version=1.0.0");
		System.out.println(cap);
		System.out.println(cap.getLayer().supportsSRS("EPSG:4326"));
	}
}

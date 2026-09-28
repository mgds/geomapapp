package haxby.layers.tile512;

import haxby.util.URLFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.geomapapp.util.XML_Menu;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class LayerSetDetails {
	public String name;
	public double[] wesn;
	public float opacity;
	public float levelZeroTileDelta;
	public int numLevels;
	public int tileSize;
	public String imageExtension;
	public String imagePath;

	public static LayerSetDetails levelsFromXML_Menu(XML_Menu menu) {
		String wwmlPath = menu.layer_url2;
		if (wwmlPath == null) return null;

		// Parse the WWML
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringComments(true);
		DocumentBuilder db = null;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return null;
		}

		Document dom = null;
		try {
			dom = db.parse(URLFactory.url(wwmlPath).openStream());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (dom == null) return null;

		Node root = getElement(dom, "LayerSet");
		Node quadTileSet = getElement(root, "QuadTileSet");

		LayerSetDetails lsd = new LayerSetDetails();

		// WWLayer Name
		lsd.name = root.getAttributes().getNamedItem("Name").getNodeValue();

		Node bbox = getElement(quadTileSet, "BoundingBox");
		String north = getText(
						getElement(getElement(bbox, "North"), "Value"));
		String south = getText(
						getElement(getElement(bbox, "South"), "Value"));
		String east = getText(
						getElement(getElement(bbox, "East"), "Value"));
		String west = getText(
						getElement(getElement(bbox, "West"), "Value"));

		// WWLayer BoundingBox
		lsd.wesn = new double[4];
		lsd.wesn[0] = Double.parseDouble(west);
		lsd.wesn[1] = Double.parseDouble(east);
		lsd.wesn[2] = Double.parseDouble(south);
		lsd.wesn[3] = Double.parseDouble(north);

		lsd.opacity = Float.parseFloat(getText(getElement(quadTileSet, "Opacity"))) /
							255f;

		Node imageAccessor = getElement(quadTileSet, "ImageAccessor");

		lsd.levelZeroTileDelta = Float.parseFloat(
				getText( getElement(imageAccessor, "LevelZeroTileSizeDegrees")) );
		lsd.numLevels = Integer.parseInt(
				getText( getElement(imageAccessor, "NumberLevels")));
		lsd.tileSize = Integer.parseInt(
				getText( getElement(imageAccessor, "TextureSizePixels")));
		lsd.imageExtension = getText( getElement(imageAccessor, "ImageFileExtension"));
		lsd.imagePath = getText( getElement(imageAccessor, "PermanentDirectory") );

		lsd.imagePath = wwmlPath.substring(0, wwmlPath.lastIndexOf("/") + 1);

		return lsd;
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
}
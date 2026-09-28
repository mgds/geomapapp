package org.geomapapp.db.dsdp;

import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ParseBRG {
	String BASE = PathUtil.getPath("BRG_LOGFILE_QUERY", 
			"http://brg.ldeo.columbia.edu/services/LogFile/");
	BRGEntry root;
	JLabel label;
	public ParseBRG(String leg, String hole) throws IOException, ParserConfigurationException, SAXException {
		Vector<BRGEntry> children = new Vector<BRGEntry>();
		root = new BRGEntry(null, children, leg+"-"+hole, null);
		
		String urlString = BASE + "?hole=" + hole;
		try
		{
			int legI = Integer.parseInt(leg);
			if (legI >= 300)
				urlString = BASE + "?hole=U" + hole;
		} catch (NumberFormatException ex) {}
		
		URL url = URLFactory.url(urlString);
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringComments(true);
		
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document dom = db.parse( new BufferedInputStream(url.openStream()) );
		
		Node logFilesRoot = getElement(dom, "brg:LogFiles");
		List<Node> logFiles = getElements(logFilesRoot, "LogFile");
		for (Node logFile : logFiles)
		{
			Node fileNameNode = getElement(logFile, "Filename");
			Node fileURLNode = getElement(logFile, "FileUrl");
			String fileName = getText(fileNameNode);
			String fileURL  = getText(fileURLNode);
			if (fileURL.toLowerCase().endsWith(".dat") ||
					fileURL.toLowerCase().endsWith(".gif"))
				children.add(
					new BRGEntry(root, null, fileName, fileURL));
		}
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
	
	private static String getText(Node node) {
		NodeList nodeList = node.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (nodeList.item(i).getNodeName().equals("#text") ||
					nodeList.item(i).getNodeName().equals("#cdata-section"))
			{
				return nodeList.item(i).getNodeValue();
			}
		}
		return null;
	}
	
	public BRGEntry getRoot() {
		return root;
	}
	public static void main(String[] args) {
		try {
			ParseBRG pBRG = new ParseBRG( "", "642E");
			
			JFrame jf = new JFrame();
			jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			JTree jt = new JTree(pBRG.getRoot());
			jf.getContentPane().add(new JScrollPane(jt));
			jf.pack();
			jf.setVisible(true);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}

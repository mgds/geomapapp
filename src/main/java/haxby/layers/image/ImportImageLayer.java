package haxby.layers.image;

import haxby.layers.image.ImageProvider.FileImageProvider;
import haxby.layers.image.ImageProvider.URLImageProvider;
import haxby.layers.image.ImageProvider.ZipImageProvider;
import haxby.map.FocusOverlay;
import haxby.map.MapApp;
import haxby.util.WESNPanel;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ImportImageLayer {

	public static List<FileFilter> supportedImageSources = new LinkedList<FileFilter>();
	static {
		supportedImageSources.add(
			new FileFilter() {
			public String getDescription() {
				return "Google Earth (KML / KMZ)";
			}
		
			public boolean accept(File f) {
				if (f.isDirectory()) return true;
				
				if (f.getName().toLowerCase().endsWith(".kml"))
					return true;
				if (f.getName().toLowerCase().endsWith(".kmz"))
					return true;
				
				return false;
			}
		});
		supportedImageSources.add( 
			new FileFilter() {
			public String getDescription() {
				return "Supported Image Files";
			}
			
			public boolean accept(File f) {
				if (f.isDirectory()) return true;
				
				String name = f.getName();
				String suffix = name.substring(name.lastIndexOf(".")+1);
				return ImageIO.getImageReadersBySuffix(suffix).hasNext();
			}
		});
	}
	private static FileFilter allSources = 
		new FileFilter() {
			public String getDescription() {
				return "Supported Image Overlay Sources";
			}
			
			public boolean accept(File f) {
				if (f.isDirectory()) 
					return true;
				for (FileFilter ff : supportedImageSources)
					if (ff.accept(f)) return true;
				
				return false;
			}
		};
	
	public void importImage(MapApp mapApp) {
		JFileChooser chooser = MapApp.getFileChooser();
		for (FileFilter ff : supportedImageSources)
			chooser.setFileFilter(ff);
		chooser.setFileFilter(allSources);
		
		int c = chooser.showOpenDialog(mapApp.getFrame());
		if (c == JFileChooser.CANCEL_OPTION) return;
		
		File file = chooser.getSelectedFile();
		if (file == null) return;
		
		if (file.getName().toLowerCase().endsWith(".kml"))
			importKMZ(mapApp, file, false);
		else if (file.getName().toLowerCase().endsWith(".kmz"))
			importKMZ(mapApp, file, true);
		else
			importImage(mapApp, file);
	}
	
	private void importKMZ(MapApp mapApp, File file, boolean kmz) {
		try {
			InputStream in;
			
			if (kmz)
			{
				ZipInputStream zis = new ZipInputStream(
						new BufferedInputStream(
								new FileInputStream(file)));
				
				ZipEntry ze = null;
				while ((ze = zis.getNextEntry()) != null) {
					if (ze.getName().endsWith(".kml"))
						break;
				}
				
				if (ze == null) return;
				
				in = zis;
			} else
				in = new BufferedInputStream( new FileInputStream( file ));
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setIgnoringComments(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse( in );

			final Map<GeoRefImage, Integer> drawOrders 
				= new HashMap<GeoRefImage, Integer>();
			
			SortedSet<GeoRefImage> layers = 
				new TreeSet<GeoRefImage>(new Comparator<GeoRefImage>() {
					public int compare(GeoRefImage o1, GeoRefImage o2) {
						Integer i1 = drawOrders.get(o1);
						Integer i2 = drawOrders.get(o2);
						int c = i1.compareTo(i2);
						return c == 0 ? 1 : c;
					}
				});
			
			List<Node> groundOverlays = findElements(doc, "GroundOverlay");
			for (Node groundOverlay : groundOverlays) {
				Node name = findElement(groundOverlay, "name");
				Node text = findElement(name, "#text");
				String imageName = null;
				if (text != null) imageName = text.getNodeValue();
				else 
				{
					Node parent = groundOverlay.getParentNode();
					while (parent != null)
					{
						name = findElement(parent, "name");
						text = findElement(name, "#text");
						if (text != null) {
							imageName = text.getNodeValue();
							break;
						}
					}
					if (imageName == null)
						imageName = file.getName();
				}
				
				// Parse drawOrder
				int drawOrder = 0;
				Node drawOrderNode = findElement(groundOverlay, "drawOrder");
				text = findElement(drawOrderNode, "#text");
				if (text != null)
					drawOrder = Integer.parseInt(text.getNodeValue());
				
				// Parse Icon Element
				Node icon = findElement(groundOverlay, "Icon");
				Node href = findElement(icon, "href");
				text = findElement(href, "#text");
				if (text == null) continue;
				String link = text.getNodeValue();
				
				double[] wesn = new double[4];
				
				// Parse LatLonBox Element
				Node latLonBox = findElement(groundOverlay, "LatLonBox");
				Node north = findElement(latLonBox, "north");
				text = findElement(north, "#text");
				if (text == null) continue;
				wesn[3] = Double.parseDouble(text.getNodeValue());
				
				Node south = findElement(latLonBox, "south");
				text = findElement(south, "#text");
				if (text == null) continue;
				wesn[2] = Double.parseDouble(text.getNodeValue());
				
				Node east = findElement(latLonBox, "east");
				text = findElement(east, "#text");
				if (text == null) continue;
				wesn[1] = Double.parseDouble(text.getNodeValue());
				
				Node west = findElement(latLonBox, "west");
				text = findElement(west, "#text");
				if (text == null) continue;
				wesn[0] = Double.parseDouble(text.getNodeValue());

				// Parse Lod if present
				int minPixels = -1;
				int maxPixels = -1;
				Node lod = findElement(groundOverlay, "Lod");
				Node minLodPixels = findElement(lod, "minLodPixels");
				text = findElement(minLodPixels, "#text");
				if (text != null)  
					minPixels = Integer.parseInt(text.getNodeValue());
				
				Node maxLodPixels = findElement(lod, "maxLodPixels");
				text = findElement(maxLodPixels, "#text");
				if (text != null) 
					maxPixels = Integer.parseInt(text.getNodeValue());
				
				int minViewRes = 1;
				int maxViewRes = Integer.MAX_VALUE;
				if (minLodPixels != null || maxLodPixels != null)
				{
					double dLat = wesn[3] - wesn[2];
					double dLon = wesn[1] - wesn[0];
					
					if (minLodPixels != null && minPixels != -1) {
						double ppdLat = minPixels / dLat;
						double ppdLon = minPixels / dLon;
						
						double ppdImage = Math.max(ppdLat, ppdLon);
						
						int zoom = 1;
						double ppd = 640 / 360.;
						
						while (ppd < ppdImage) {
							zoom *= 2;
							ppd *= 2;
						}
						
						minViewRes = zoom;
					}
					
					if (maxLodPixels != null && maxPixels != -1) {
						double ppdLat = maxPixels / dLat;
						double ppdLon = maxPixels / dLon;
						
						double ppdImage = Math.max(ppdLat, ppdLon);
						
						int zoom = 1;
						double ppd = 640 / 360.;
						
						while (ppd < ppdImage) {
							zoom *= 2;
							ppd *= 2;
						}
						
						maxViewRes = zoom;
					}
				}
				
				// Figure out our image type
				ImageProvider image = null;
				
				if (link.startsWith("http"))
					image = new URLImageProvider(link);
				else {
					if (kmz)
					{
						ZipInputStream zis = new ZipInputStream(
								new BufferedInputStream(
										new FileInputStream(file)));
						
						ZipEntry ze = null;
						while ((ze = zis.getNextEntry()) != null) {
							if (ze.getName().equals(link))
								break;
						}
						
						zis.close();
						
						if (ze != null)
							image = new ZipImageProvider(link, file);
					}
					
					if (image == null)
					{
						File imageF = new File(link);
						if (!imageF.exists()) 
							imageF = new File(file.getParent(), link);
						if (!imageF.exists())
							continue;
						
						image = new FileImageProvider(imageF);
					}
				}
				
				GeoRefImage geoImage = new GeoRefImage(image, wesn, minViewRes, maxViewRes);
				drawOrders.put(geoImage, drawOrder);
				layers.add(geoImage);
			}
			
			ImageOverlaySet layer = new ImageOverlaySet(mapApp.getMap());
			layer.setName(file.getName());
			
			for (GeoRefImage geoImage : layers) {
				layer.addGeoImage(geoImage);
			}
			
			mapApp.addFocusOverlay(layer, file.getName());
			
		}
		catch (ParserConfigurationException ex) {} 
		catch (SAXException e) { } 
		catch (IOException e) { e.printStackTrace(); }
		catch (NumberFormatException e) { }
	}

	private static List<Node> findElements(Node root, String elementName) {
		LinkedList<Node> matches = new LinkedList<Node>();
		if (root == null || elementName == null) return matches;

		NodeList childNodes = root.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeName().equals( elementName ))
				matches.add(node);
			else 
			{
				List<Node> elements = findElements(node, elementName);
				matches.addAll(elements);
			}
		}
		
		return matches;
	}
	
	private static Node findElement(Node root, String elementName)
	{
		if (root == null || elementName == null) return null;
		
		NodeList childNodes = root.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeName().equals( elementName ))
				return node;
			node = findElement(node, elementName);
			if (node != null)
				return node;
		}
		
		return null;
	}

	protected void importImage(MapApp mapApp, File file) {
		ImageWESNProj wesn = showWESNDialog(mapApp.getFrame());
		if (wesn == null) return;
		
		try {
			BufferedImage image = ImageIO.read(file);
			FocusOverlay overlay;
			if (wesn.merc)
				overlay = new MercatorImageOverlay(mapApp.getMap(), image, wesn.wesn);
			else
				overlay = new GeographicImageOverlay(mapApp.getMap(), image, wesn.wesn);
			
			mapApp.addFocusOverlay(overlay, file.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static ImageWESNProj showWESNDialog(JFrame owner) 
	{
		final JDialog d = new JDialog(owner, "Image Location", true);
		
		final WESNPanel wesnP = new WESNPanel();
		TitledBorder border = BorderFactory.createTitledBorder("Image Location (Negatives for Western and Southern Hemisphere)");
		wesnP.setBorder(border);
		
		JPanel p2 = new JPanel();
		JButton accept = new JButton("Accept");
		accept.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double wesn[] =  wesnP.getWESN();
				if (wesn == null) {
					Toolkit.getDefaultToolkit().beep();
					return;
				}
				if (wesn[0] > wesn[1] || 
						wesn[2] > wesn[3])
				{
					if (wesn[0] > wesn[1])
					{
						
						wesnP.west.setText("! W > E !");
						wesnP.east.setText("! E < W !");
					}
					if (wesn[2] > wesn[3])
					{
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
		
		JPanel p3 = new JPanel();
		p3.setBorder( BorderFactory.createTitledBorder("Source Image Projection")) ;
		ButtonGroup bg = new ButtonGroup();
		JRadioButton rb = new JRadioButton("Geographic", true);
		bg.add(rb);
		p3.add(rb);
		JRadioButton merc = new JRadioButton("Mercator");
		bg.add(merc);
		p3.add(merc);
		
		JPanel c = new JPanel(new BorderLayout());
		c.add(p3, BorderLayout.NORTH);
		c.add(wesnP);
		c.add(p2, BorderLayout.SOUTH);
		
		d.addWindowListener( new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				wesnP.setWESN(null);
			}
		});
		d.getContentPane().add(c);
		d.setLocationRelativeTo(owner);
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		d.pack();
		d.setVisible(true);
		
		double wesn[] = wesnP.getWESN();
		if (wesn == null) return null;
		
		return new ImageWESNProj(wesn, merc.isSelected());
	}
	
	protected static class ImageWESNProj {
		public double[] wesn;
		public boolean merc;
		public ImageWESNProj(double[] wesn, boolean merc) {
			this.merc = merc;
			this.wesn = wesn;
		}
	}
}

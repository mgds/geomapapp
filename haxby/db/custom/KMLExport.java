package haxby.db.custom;

import haxby.map.MapApp;
import haxby.util.PathUtil;
import haxby.util.URLFactory;
import haxby.util.XBTable;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class KMLExport {
	public static final String ICON_PATH = 
		PathUtil.getPath("ICONS/GE_ICON_PATH", MapApp.BASE_URL+"/gma_icons/icon.gif");
	public static final String ICON_NAME =
		ICON_PATH.substring(ICON_PATH.lastIndexOf("/") + 1);

	private static final int N_ANOT = 3;
	private static final int LON_PARTITION_N = 20;
	private static final int LAT_PARTITION_N = 20;
	private static final String LABEL_MIN_PIXELS = "1500";

	public static void exportToKML(UnknownDataSet ds, DataSelector select) {
		KMLExportConfigDialog fc = null;
		HeaderConfig hc = null;

		// Set up the headers and fields
		do {
			if (hc == null)
				hc = new HeaderConfig((JFrame)ds.map.getTopLevelAncestor(),ds);
			else
				hc.setVisible(true);
			if (hc.visibleV == null) {
				hc.dispose();
				return;
			}

			int imgI = guessImage(ds, hc.visibleV, select);
			fc = new KMLExportConfigDialog((JFrame) ds.map.getTopLevelAncestor(), 
					ds.desc.name.replace(":", "").replace(",", ""), hc.visibleV, imgI);
		} while (!fc.export);
		hc.dispose();

		// Get the save file
		JFileChooser jfc = new JFileChooser(System.getProperty("user.home"));
		
		String defaultName = ds.desc.name.replace(":", "").replace(",", "") + ".kmz";
		defaultName = defaultName.replace("Data Table ", "").replace(" ", "_");
		File f=new File(defaultName);
		jfc.setSelectedFile(f);

		do {
			int c = jfc.showSaveDialog(null);
			if (c==JFileChooser.CANCEL_OPTION||c==JFileChooser.ERROR_OPTION) return;
			f = jfc.getSelectedFile();
			if (f.exists()) {
				c=JOptionPane.showConfirmDialog(MapApp.anchor, "File Already Exists\nConfirm Overwrite");
				if (c==JOptionPane.OK_OPTION) break;
				// if (c==JOptionPane.CANCEL_OPTION) return;
			}
		} while (f.exists());

		int n = 1 + fc.getScales().size();
		n *= select.getRowCount();

		JDialog dialog = new JDialog((JFrame) ds.map.getTopLevelAncestor());
		dialog.setTitle("Exporting to KMZ");
		dialog.setModal(true);
		JProgressBar pb = new JProgressBar(0,n);
		JPanel p = new JPanel();
		p.add(new JLabel("Saving " + f.getName()));
		p.add(pb);

		dialog.getContentPane().setLayout(new FlowLayout());
		dialog.getContentPane().add(p);
		dialog.pack();
		dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		dialog.setLocation(dialog.getOwner().getX() + dialog.getOwner().getWidth() / 2 - dialog.getWidth()/2, 
				dialog.getOwner().getY() + dialog.getOwner().getHeight() / 2 - dialog.getHeight()/2);

		new Task(new Object[] {ds,select,f,fc,hc,pb,dialog}) {
			public void run() {
				exportToKML((UnknownDataSet)args[0],
						(DataSelector)args[1],
						(File)args[2],
						(KMLExportConfigDialog)args[3],
						(HeaderConfig)args[4],
						(JProgressBar)args[5]);
				((JDialog) args[6]).dispose();
			}
		}.start();
		dialog.setVisible(true);
	}

	private static int guessImage(UnknownDataSet ds, Vector<String> visibleV, DataSelector select) {
		for (int i = 0; i < visibleV.size(); i++) {
			int hIndex = 0;
			for (;hIndex < ds.header.size(); hIndex++) 
				if (ds.header.get(hIndex) == visibleV.get(i))
					break;
			if (hIndex == ds.header.size())
				continue;

			String value = "";
			for (int j = 0; j < select.getRowCount(); j++) {
				value = select.getValueAt(j, hIndex).toString();
				if (value.length()==0) continue;
				else break; 
			}
			if (HyperlinkTableRenderer.validURL(value))
				return i;
		}
		return -1;
	}

	private static void exportToKML(UnknownDataSet ds, DataSelector select, File f, KMLExportConfigDialog fc, HeaderConfig hc, JProgressBar pb) {
		//Get placemark count
		int n = 0;

		ZipOutputStream zos = null;
		try {
			zos = new ZipOutputStream( new BufferedOutputStream (new FileOutputStream(f)));
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(MapApp.anchor, e.getMessage(), "Output Config Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		}

		try {
			Document doc2 = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element kml = doc2.createElement("kml");
			kml.setAttribute("xmlns", "http://earth.google.com/kml/2.1");
			doc2.appendChild(kml);

			Element topFolder = doc2.createElement("Document");
			kml.appendChild(topFolder);

			Element leaf = doc2.createElement("name");
			leaf.appendChild(doc2.createTextNode(processString(ds.desc.name)));
			topFolder.appendChild(leaf);

			List<Element> styles = createStyleDefinitions(doc2);
			for (Element e : styles)
				topFolder.appendChild( e );

			Element dataFolder = doc2.createElement("Folder");
			topFolder.appendChild(dataFolder);

			leaf = doc2.createElement("styleUrl");
			leaf.appendChild(doc2.createTextNode("#radioFolderStyle"));
			dataFolder.appendChild(leaf);

			leaf = doc2.createElement("name");
			leaf.appendChild(doc2.createTextNode("Data"));
			dataFolder.appendChild(leaf);

			// Create our network links
			for (int scaleIndex = -1; scaleIndex < fc.getScales().size(); scaleIndex++) {
				KMLExportConfigDialog.ScalePanel scalePanel = null;
				if (scaleIndex != -1) {
					scalePanel = (KMLExportConfigDialog.ScalePanel) fc.getScales().get(scaleIndex);
					if (!scalePanel.colorCB.isSelected() && !scalePanel.sizeCB.isSelected())
						continue;
				}

				Element networkLink = doc2.createElement("NetworkLink");
				dataFolder.appendChild(networkLink);
				Element link = doc2.createElement("Link");
				networkLink.appendChild(link);
				leaf = doc2.createElement("href");
				if (scaleIndex == -1)
					leaf.appendChild(doc2.createTextNode("data.kml"));
				else 
					leaf.appendChild(doc2.createTextNode(scaleIndex + ".kml"));
				link.appendChild(leaf);

				leaf = doc2.createElement("visibility");
				if (scaleIndex == -1)
					leaf.appendChild(doc2.createTextNode("1"));
				else
					leaf.appendChild(doc2.createTextNode("0"));
				networkLink.appendChild(leaf);

				leaf = doc2.createElement("name");
				if (scaleIndex == -1)
					leaf.appendChild(doc2.createTextNode("Unscaled Data"));
				else
					leaf.appendChild(doc2.createTextNode(processString(scalePanel.getScaleName())));
				networkLink.appendChild(leaf);

				if (scaleIndex == -1 || !scalePanel.colorCB.isSelected()) {
					leaf = doc2.createElement("styleUrl");
					leaf.appendChild(doc2.createTextNode("#hiddenChildrenFolder"));
					networkLink.appendChild(leaf);
				}
			}

			// Save our top file first
			ZipEntry ze = new ZipEntry("doc.kml");
			zos.putNextEntry(ze);

			Transformer trans = TransformerFactory.newInstance().newTransformer();
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.transform(new DOMSource(doc2), new StreamResult(zos));

			// For each scaled item
			boolean scale = false;
			scaleIndex: for (int scaleIndex = -1; scaleIndex < fc.getScales().size(); scaleIndex++) {
				System.gc();

				KMLExportConfigDialog.ScalePanel scalePanel = null;
				if (scaleIndex != -1) {
					scalePanel = (KMLExportConfigDialog.ScalePanel) fc.getScales().get(scaleIndex);
					if (!scalePanel.colorCB.isSelected() && !scalePanel.sizeCB.isSelected())
						continue;
				}

				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
				kml = doc.createElement("kml");
				kml.setAttribute("xmlns", "http://earth.google.com/kml/2.1");
				doc.appendChild(kml);

				topFolder = doc.createElement("Document");
				kml.appendChild(topFolder);
				styles = createStyleDefinitions(doc);
				for (Element element : styles)
					topFolder.appendChild(element);

				Element scaleFolder = topFolder;
				Element screenOverlay = null;
				if (scaleIndex == -1) {
					// set to check hide children folder behavior
					leaf = doc.createElement("styleUrl");
					leaf.appendChild(doc.createTextNode("#hiddenChildrenFolder"));
					scaleFolder.appendChild(leaf);

					leaf = doc.createElement("name");
					leaf.appendChild(doc.createTextNode("Unscaled Data"));
					scaleFolder.appendChild(leaf);
				}
				else {
					scale = true;
					Element midFolder = topFolder; //doc.createElement("Folder");
//					topFolder.appendChild(midFolder);

					leaf = doc.createElement("name");
					leaf.appendChild(doc.createTextNode(processString(scalePanel.getScaleName())));
					midFolder.appendChild(leaf);

					// Build the Scale Overlay
					screenOverlay = doc.createElement("ScreenOverlay");

					leaf = doc.createElement("name");
					leaf.appendChild(doc.createTextNode("Scale Overlay"));
					screenOverlay.appendChild(leaf);

					leaf = doc.createElement("overlayXY");
					leaf.setAttribute("x", "0");
					leaf.setAttribute("y", "1");
					leaf.setAttribute("xunits", "fraction");
					leaf.setAttribute("yunits", "fraction");
					screenOverlay.appendChild(leaf);

					leaf = doc.createElement("screenXY");
					leaf.setAttribute("x", "0");
					leaf.setAttribute("y", "1");
					leaf.setAttribute("xunits", "fraction");
					leaf.setAttribute("yunits", "fraction");
					screenOverlay.appendChild(leaf);

					leaf = doc.createElement("size");
					leaf.setAttribute("x", "-1");
					leaf.setAttribute("y", "-1");
					leaf.setAttribute("xunits", "pixels");
					leaf.setAttribute("yunits", "pixels");
					screenOverlay.appendChild(leaf);

					Element icon = doc.createElement("Icon");
					screenOverlay.appendChild(icon);

					leaf = doc.createElement("href");
					leaf.appendChild(doc.createTextNode(scaleIndex+".jpg"));
					icon.appendChild(leaf);

					scaleFolder = doc.createElement("Folder");
					midFolder.appendChild(scaleFolder); 

					// set to check hide children folder behavior
					leaf = doc.createElement("styleUrl");
					leaf.appendChild(doc.createTextNode("#hiddenChildrenFolder"));
					scaleFolder.appendChild(leaf);

					leaf = doc.createElement("name");
					leaf.appendChild(doc.createTextNode("Data"));
					scaleFolder.appendChild(leaf);
				}

				double colorMin = Double.MAX_VALUE;
				double colorMax = -Double.MAX_VALUE;
				double sizeMin = Double.MAX_VALUE;
				double sizeMax = -Double.MAX_VALUE;
				int colorHeaderIndex = -1;
				int sizeHeaderIndex = -1;
				if (scale) {
					if (scalePanel.colorCB.isSelected()) {
						colorHeaderIndex = ds.header.indexOf(scalePanel.color.getSelectedItem());
						for (int i = 0; i < select.getRowCount(); i++) {
							try {
								double x = Double.parseDouble(select.getValueAt(i, colorHeaderIndex).toString());
								if (Double.isNaN(x)) continue;
								if (x < colorMin) colorMin = x;
								if (x > colorMax) colorMax = x;
							} catch (NumberFormatException ex){
								continue;
							}
						}

						if (colorMin == Double.MAX_VALUE && colorMax == -Double.MAX_VALUE) {
							ze = new ZipEntry(scaleIndex == -1 ? "data.kml" : scaleIndex + ".kml");
							zos.putNextEntry(ze);

							trans = TransformerFactory.newInstance().newTransformer();
							trans.setOutputProperty(OutputKeys.INDENT, "yes");
							trans.transform(new DOMSource(doc), new StreamResult(zos));
							continue scaleIndex;
						}

						if (colorMin != colorMax) {
							// Add the Scale Overlay
							if (scalePanel.colorCB.isSelected()) topFolder.appendChild(screenOverlay);

							ze = new ZipEntry(scaleIndex + ".jpg");
							try {
								zos.putNextEntry(ze);
	//							System.out.println(colorMin + "\t" + colorMax + "\t" + scalePanel.color.getSelectedItem().toString());
								BufferedImage img = 
									drawScale(colorMin, colorMax, 
											scalePanel.color.getSelectedItem().toString(), 
											fc.units[scalePanel.color.getSelectedIndex()].getSelectedItem().toString());

								ImageIO.write(img, "jpg", zos);
							} catch (IOException e) {
								JOptionPane.showMessageDialog(MapApp.anchor, e.getMessage(), "Color Scale Output Error", JOptionPane.ERROR_MESSAGE);
								e.printStackTrace();
							}
						}
					}
					if (scalePanel.sizeCB.isSelected()) {
						sizeHeaderIndex = ds.header.indexOf(scalePanel.size.getSelectedItem());
						for (int i = 0; i < select.getRowCount(); i++) {
							try {
								double x = Double.parseDouble(select.getValueAt(i, sizeHeaderIndex).toString());
								if (Double.isNaN(x)) continue;
								if (x < sizeMin) sizeMin = x;
								if (x > sizeMax) sizeMax = x;
							} catch (NumberFormatException ex){
								continue;
							}
						}

						if (sizeMin == Double.MAX_VALUE && sizeMax == -Double.MAX_VALUE) {
							ze = new ZipEntry(scaleIndex == -1 ? "data.kml" : scaleIndex + ".kml");
							zos.putNextEntry(ze);

							trans = TransformerFactory.newInstance().newTransformer();
							trans.setOutputProperty(OutputKeys.INDENT, "yes");
							trans.transform(new DOMSource(doc), new StreamResult(zos));
							continue scaleIndex;
						}
					}
					// Draw our color scale
				}

				Element[][][] regions = new Element[360 / LON_PARTITION_N][180 / LAT_PARTITION_N][2];
				for (int i = 0; i < select.getRowCount(); i++) {
					n++;
					pb.setValue(n);
					pb.repaint();

					Element place = doc.createElement("Placemark");

					Element style = doc.createElement("Style");
					place.appendChild(style);

					// BalloonStyle
					Element bStyle = doc.createElement("BalloonStyle");
					style.appendChild(bStyle);
					{
						leaf = doc.createElement("text");
						leaf.appendChild(doc.createCDATASection(getBallonStyle()));
						bStyle.appendChild(leaf);
					}

					// Icon Style
					Element iStyle = doc.createElement("IconStyle");
					style.appendChild(iStyle);

					if (colorHeaderIndex != -1) {
						leaf = doc.createElement("color");
						iStyle.appendChild(leaf);
						String color = getColor(select.getValueAt(i,colorHeaderIndex), colorMin, colorMax);
						if (color == null) continue;
						leaf.appendChild(doc.createTextNode(color));
					}
					if (sizeHeaderIndex != -1) {
						leaf = doc.createElement("scale");
						iStyle.appendChild(leaf);
						String size = getScale(select.getValueAt(i,sizeHeaderIndex), sizeMin, sizeMax);
						if (size == null) continue;
						leaf.appendChild(doc.createTextNode(size));
					}

//					 Placemark name
					leaf = doc.createElement("name");
					place.appendChild(leaf);
					if (!scale) {
						int nameIndex = fc.getNameIndex();
						if (nameIndex!=-1)
							for (int k = 0; k < ds.header.size(); k++)
								if (ds.header.get(k) == hc.visibleV.get(nameIndex)) {
									leaf.appendChild(doc.createTextNode(processString(select.getValueAt(i, k).toString())));
									break;
								}
					} else {
						int nameIndex = ds.header.indexOf(scalePanel.name.getSelectedItem());

						if (nameIndex == colorHeaderIndex || nameIndex == sizeHeaderIndex) {
							double min = nameIndex == colorHeaderIndex ? colorMin : sizeMin;
							double max = nameIndex == colorHeaderIndex ? colorMax : sizeMax;
							double d = Double.parseDouble(select.getValueAt(i, nameIndex).toString());
							String out;
							// TODO sig figs
							int resolution = (int) getResolution(min, max);
							int changeDigit = (int) log10(resolution);
							NumberFormat nf = new DecimalFormat();
							if (changeDigit < 1) { 
								if (changeDigit < -3) {
									nf = new DecimalFormat("#.#E0");
								} else {
									nf.setMaximumFractionDigits(-changeDigit + 2);
									nf.setMinimumFractionDigits(1);
								}
								out = nf.format(d);
							} else if (changeDigit == 1) {
								nf.setMaximumFractionDigits(1);
								out = nf.format(d);
							} else if (changeDigit > 6) {
								nf = new DecimalFormat("#.#E0");
								out = nf.format(d);
							} else {
								int value = (int) d;
								if (resolution > 1000) {
									value /= (resolution / 1000);
									value *= (resolution / 1000);
									out = value + "";
								} else {
									out = nf.format(value);
								}
							}
							leaf.appendChild(doc.createTextNode(out));
						} else
							leaf.appendChild(doc.createTextNode(processString(select.getValueAt(i, nameIndex).toString())));
					}

					Element icon = doc.createElement("Icon");
					iStyle.appendChild(icon);

					leaf = doc.createElement("href");
					leaf.appendChild(doc.createTextNode(ICON_NAME));
					icon.appendChild(leaf);

					leaf = doc.createElement("hotSpot");
					leaf.setAttribute("x", "0");
					leaf.setAttribute("y", "0");
					iStyle.appendChild(leaf);

					leaf = doc.createElement("description");
					place.appendChild(leaf);
					StringBuffer sb = new StringBuffer();
					sb.append("<table>");

					headers: for (int j = 0; j < hc.visibleV.size(); j++) {
						for (int k = 0; k < ds.header.size(); k++) {
							if (ds.header.get(k) != hc.visibleV.get(j)) 
								continue;

							String str = select.getValueAt(i, k).toString();
							String fieldData = processString(str);

							if (fieldData.length() == 0)  
								continue headers; // Dont add Blank Fields

							// Column 1
							sb.append("<tr>");
							sb.append("<td>");
								sb.append("<b>");
								sb.append("<font size='+1'>");
								sb.append(processString(fc.names[j].getText()));
								sb.append("</font>");
								sb.append(":</b>");
							sb.append("</td>");

							// Column 2
							sb.append("<td>");
							if (fc.image[j].isSelected()) { // If this is selected as an image
								sb.append("<a href='");
								sb.append(str);
								sb.append("'>");
								sb.append("<img src='");
								sb.append(str);
								sb.append("' width='150'/>");
								sb.append("</a>");
							} else {
								sb.append(fieldData);
								sb.append(" "+processString(fc.units[j].getSelectedItem().toString()));
							}

							sb.append("</td>");
							sb.append("</tr>");
							continue headers;
						}
					}
					sb.append("</table>");

					CDATASection cdata = doc.createCDATASection(sb.toString());
					leaf.appendChild(cdata);

					Element point = doc.createElement("Point");
					place.appendChild(point);

					leaf = doc.createElement("coordinates");
					point.appendChild(leaf);
					float lat, lon;
					try {
						lat = Float.parseFloat(select.getValueAt(i, ds.latIndex).toString());
						lon = Float.parseFloat(select.getValueAt(i, ds.lonIndex).toString());
						if (lon > 180) lon -= 360;
						leaf.appendChild(doc.createTextNode(lon+","+lat+",0"));

					} catch (NumberFormatException ex) {
						// Can't parse the lat lon, drop it
						continue; 
					}

					// Add our balloon style url
//					leaf = doc.createElement("styleUrl");
//					leaf.appendChild( doc.createTextNode("#balloonStyle") );
//					place.appendChild(leaf);

					// Clone our placemark and apply label styles
					Element[] regionFolder = getRegion(regions, lat, lon, scaleFolder, doc);
					Element visiblePlace = (Element) place.cloneNode(true);
					regionFolder[0].appendChild(place);
					regionFolder[1].appendChild(visiblePlace);

					leaf = doc.createElement("styleUrl");
					leaf.appendChild(doc.createTextNode("#hiddenStyleMap"));
					place.appendChild(leaf);

					leaf = doc.createElement("styleUrl");
					leaf.appendChild(doc.createTextNode("#normalStyleMap"));
					visiblePlace.appendChild(leaf);
				} // End rowCount for

				// Save this data to kml now
				ze = new ZipEntry(scaleIndex == -1 ? "data.kml" : scaleIndex + ".kml");
				zos.putNextEntry(ze);

				trans = TransformerFactory.newInstance().newTransformer();
				trans.setOutputProperty(OutputKeys.INDENT, "yes");
				trans.transform(new DOMSource(doc), new StreamResult(zos));
			} // End scaleIndex for

			// Load up and write out the icon from ICON_PATH to ICON_NAME
			ze = new ZipEntry(ICON_NAME);
			zos.putNextEntry(ze);

			BufferedImage iconIMG = ImageIO.read(URLFactory.url(ICON_PATH));
			String iconFormat = ICON_NAME.substring( ICON_NAME.lastIndexOf(".") + 1);
			ImageIO.write(iconIMG, iconFormat, zos);

			zos.close();
		} catch (ParserConfigurationException e) {
			JOptionPane.showMessageDialog(MapApp.anchor, e.getMessage(), "KMZ Parser Configure Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			JOptionPane.showMessageDialog(MapApp.anchor, e.getMessage(), "KMZ Transform Configure Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			JOptionPane.showMessageDialog(MapApp.anchor, e.getMessage(), "KMZ Transform Configure Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (TransformerException e) {
			JOptionPane.showMessageDialog(MapApp.anchor, e.getMessage(), "KMZ Transform Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(MapApp.anchor, e.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		JOptionPane.showMessageDialog(MapApp.anchor, "Export Successful");
	}

	private static String getBallonStyle() {

		return "<b><font size='+1'>$[name]</font></b>" +
				"<br/><br/>" +
				"$[description]" +
				"<br/><br/><hr/>" +
				"<align='right'>" +
				"<a href='https://www.marine-geo.org/'>" +
					"<img src='https://www.marine-geo.org/images/MGDS.gif' width='144' height='57'/>" +
				"</a>" +
				"<br><br>" +
				"Funded by the <a href='https://www.nsf.gov/'> National Science Foundation</a>.</font></tr>";

	}

	private static Element[] getRegion(Element[][][] regions, float lat, float lon, Element parent, Document doc) {
		int x = (int) Math.floor((lon + 180) / LON_PARTITION_N); 
		int y = (int) Math.floor((lat + 90) / LAT_PARTITION_N);

		if (x >= regions.length) x = 0;
		if (y >= regions[0].length) y = regions[0].length - 1;
		if (x < 0) x = 0;
		if (y < 0) y = 0;

		if (regions[x][y][0] == null) { // create the region
			regions[x][y][0] = doc.createElement("Folder");
			regions[x][y][1] = doc.createElement("Folder");
			parent.appendChild(regions[x][y][0]);
			parent.appendChild(regions[x][y][1]);

			Element region = doc.createElement("Region");
			Element latLonBox = doc.createElement("LatLonAltBox");
			region.appendChild(latLonBox);

			int boundX = -180 + LON_PARTITION_N * x;
			int boundY = -90 + LAT_PARTITION_N * y;

			Element leaf = doc.createElement("north");
			leaf.appendChild(doc.createTextNode((boundY + LAT_PARTITION_N) + ""));
			latLonBox.appendChild(leaf);
			leaf = doc.createElement("south");
			leaf.appendChild(doc.createTextNode(boundY + ""));
			latLonBox.appendChild(leaf);
			leaf = doc.createElement("east");
			leaf.appendChild(doc.createTextNode((boundX + LON_PARTITION_N) + ""));
			latLonBox.appendChild(leaf);
			leaf = doc.createElement("west");
			leaf.appendChild(doc.createTextNode(boundX + ""));
			latLonBox.appendChild(leaf);

			Element visibleRegion = (Element) region.cloneNode(true);
			regions[x][y][0].appendChild(region);
			regions[x][y][1].appendChild(visibleRegion);

			Element lod = doc.createElement("Lod");
			region.appendChild(lod);
			leaf = doc.createElement("maxLodPixels");
			leaf.appendChild(doc.createTextNode(LABEL_MIN_PIXELS));
			lod.appendChild(leaf);

			lod = doc.createElement("Lod");
			visibleRegion.appendChild(lod);
			leaf = doc.createElement("minLodPixels");
			leaf.appendChild(doc.createTextNode(LABEL_MIN_PIXELS));
			lod.appendChild(leaf);
		}
		return regions[x][y];
	}

	private static List<Element> createStyleDefinitions(Document doc) {
		List<Element> styles = new LinkedList<Element>();

		// Create our plain LabelStyle
		Element style = doc.createElement("Style");
		style.setAttribute("id", "plainLabel");
		Element lStyle = doc.createElement("LabelStyle");
		style.appendChild(lStyle);

		Element leaf = doc.createElement("scale");
		leaf.appendChild(doc.createTextNode("1"));
		lStyle.appendChild(leaf);

		styles.add(style);

//		 Create our highlight LabelStyle
		style = doc.createElement("Style");
		style.setAttribute("id", "highlightLabel");
		lStyle = doc.createElement("LabelStyle");
		style.appendChild(lStyle);

		leaf = doc.createElement("scale");
		lStyle.appendChild(leaf);
		leaf.appendChild(doc.createTextNode("1.5"));

		styles.add(style);

//		 Create our hidden LabelStyle
		style = doc.createElement("Style");
		style.setAttribute("id", "hiddenLabel");
		lStyle = doc.createElement("LabelStyle");
		style.appendChild(lStyle);

		leaf = doc.createElement("scale");
		leaf.appendChild(doc.createTextNode("0"));
		lStyle.appendChild(leaf);

		styles.add(style);

//		 Create our normal stlye map
		style = doc.createElement("StyleMap");
		style.setAttribute("id", "normalStyleMap");

		Element pair = doc.createElement("Pair");
		style.appendChild(pair);
		leaf = doc.createElement("key");
		pair.appendChild(leaf);
		leaf.appendChild(doc.createTextNode("normal"));
		leaf = doc.createElement("styleUrl");
		pair.appendChild(leaf);
		leaf.appendChild(doc.createTextNode("#plainLabel"));

		pair = doc.createElement("Pair");
		style.appendChild(pair);
		leaf = doc.createElement("key");
		pair.appendChild(leaf);
		leaf.appendChild(doc.createTextNode("highlight"));
		leaf = doc.createElement("styleUrl");
		pair.appendChild(leaf);
		leaf.appendChild(doc.createTextNode("#highlightLabel"));

		styles.add(style);

//		 Create our hidden stlye map
		style = doc.createElement("StyleMap");
		style.setAttribute("id", "hiddenStyleMap");

		pair = doc.createElement("Pair");
		style.appendChild(pair);
		leaf = doc.createElement("key");
		pair.appendChild(leaf);
		leaf.appendChild(doc.createTextNode("normal"));
		leaf = doc.createElement("styleUrl");
		pair.appendChild(leaf);
		leaf.appendChild(doc.createTextNode("#hiddenLabel"));

		pair = doc.createElement("Pair");
		style.appendChild(pair);
		leaf = doc.createElement("key");
		pair.appendChild(leaf);
		leaf.appendChild(doc.createTextNode("highlight"));
		leaf = doc.createElement("styleUrl");
		pair.appendChild(leaf);
		leaf.appendChild(doc.createTextNode("#plainLabel"));

		styles.add(style);

		// Create our radioFolderStyle
		style = doc.createElement("Style");
		style.setAttribute("id", "radioFolderStyle");
		Element listStyle = doc.createElement("ListStyle");
		style.appendChild(listStyle);
		leaf = doc.createElement("listItemType");
		leaf.appendChild(doc.createTextNode("radioFolder"));
		listStyle.appendChild(leaf);

		styles.add(style);

		// Create our hiddenChildrenFolder
		style = doc.createElement("Style");
		style.setAttribute("id", "hiddenChildrenFolder");
		listStyle = doc.createElement("ListStyle");
		style.appendChild(listStyle);
		leaf = doc.createElement("listItemType");
		leaf.appendChild(doc.createTextNode("checkHideChildren"));
		listStyle.appendChild(leaf);

		styles.add(style);

		// Create our ballonstyle
		style = doc.createElement("Style");
		style.setAttribute("id", "balloonStyle");
		Element bStyle = doc.createElement("BalloonStyle");
		style.appendChild(bStyle);
		{
			leaf = doc.createElement("text");
			leaf.appendChild(
					doc.createCDATASection("<b><font color='#CC0000' size='+3'>$[name]</font></b><br/><br/>$[description]<br/><br/>"));
			bStyle.appendChild(leaf);
		}

		styles.add(style);

		return styles;
	}

	/**
	 * Remove all noncharacter data
	 * @param str
	 * @return
	 */
	private static String processString(String str) {
		String nStr = str;
		int i = 0;
		while (i < nStr.length()) {
			if (Character.isIdentifierIgnorable(nStr.charAt(i)))
				nStr = nStr.substring(0, i) + nStr.substring(i+1);
			else
				i++;
		}
		return nStr;
	}

	private static String getColor(Object valueAt, double min, double max) {
		try {
			double value = Double.parseDouble(valueAt.toString());
			if (Double.isNaN(value)) return null;
			if (min == max)	return "ffffffff"; //min == max, return white

			value = (value - min) / (max - min);
			double r = value * 256;
			double b = (1 - value) * 256;
			String red = Integer.toHexString((int)r);
			String blue = Integer.toHexString((int)b);
			if (blue.length() == 1) blue = "0" + blue;
			else if (blue.length() == 3) blue = "ff";
			if (red.length()==1) red = "0" + red;
			else if (red.length() == 3) red = "ff";
			return "ff" + blue + "00" + red;
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static String getScale(Object valueAt, double min, double max) {
		try {
			double value = Double.parseDouble(valueAt.toString());
			if (Double.isNaN(value)) return null;
			if (min == max) return "1";
			value = (value - min) / (max - min);
			float s = (float) value * 2 + .5f;
			return s + "";
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static double getResolution(double min, double max) {
		double resolution = 1.;

		while( Math.floor(max/resolution)-Math.ceil(min/resolution) < N_ANOT) resolution /= 10.;
		while( Math.floor(max/resolution)-Math.ceil(min/resolution) > N_ANOT) resolution *= 10.;
		
		return resolution;
	}

	private static BufferedImage drawScale(double min, double max, String name, String units) {
		int xOff = 30;
		int yOff = 5;
		int w = 350;
		int h = 40;
		BufferedImage img = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
		FontMetrics fm = img.createGraphics().getFontMetrics();
		int imgW = w + 3 * xOff + Math.max(fm.stringWidth(name), fm.stringWidth(units));

		img = new BufferedImage(imgW,h + yOff * 2,BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();

		g.setColor(Color.WHITE);
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
		g.setColor(Color.BLACK);
		g.drawRect(0, 0, img.getWidth() - 1,img.getHeight() - 1);

		GradientPaint gp = new GradientPaint(xOff,0,Color.BLUE,xOff+w,0,Color.RED);
		g.setPaint(gp);
		g.fillRect(xOff, yOff, w, h / 2);
		g.setColor(Color.BLACK);
		g.drawRect(xOff, yOff, w, h / 2);

		g.drawString(name, w + xOff * 2, h / 3);
		g.drawString(units, w + xOff * 2,2 *  h / 3);

		double[] res = { 2., 2.5, 2. };
		double resolution = getResolution(min, max);
		double res2 = resolution;

		int kres = 0;
		while( Math.floor(max/resolution)-Math.ceil(min/resolution) < N_ANOT) {
			kres = (kres+2) %3;
			resolution /= res[kres];
		}
		double val;
		double scale = w/(max-min);
		g.setStroke(new BasicStroke(2f));
		g.setColor( Color.BLACK);
		val = resolution * Math.ceil(min/resolution);

		int changeDigit = (int) log10(res2);
		NumberFormat nf = new DecimalFormat();
		if (changeDigit < 1) { 
			if (changeDigit < -3) {
				nf = new DecimalFormat("#.#E0");
			} else {
				nf.setMaximumFractionDigits(-changeDigit + 2);
				nf.setMinimumFractionDigits(1);
			}
		} else if (changeDigit == 1) {
			nf.setMaximumFractionDigits(1);
		} else if (changeDigit > 6)
			nf = new DecimalFormat("#.#E0");

		while( val<=max ) {
			int x = 1 + xOff +(int) (scale*(val-min));
			g.drawLine( x, yOff, x, yOff + 2 * h / 3 );
			String s = nf.format(val);
			x -= fm.stringWidth(s) / 2;
			g.drawString(s, x, yOff + h);

			val += resolution;
		}
		return img;
	}

	private static double log10(double x) {
		return Math.log(x) / Math.log( 10.0 );
	}

	public static interface DataSelector {
		public int getRowCount();
		public Object getValueAt(int row, int col);
	}

	private static class HeaderConfig extends JDialog implements ActionListener, WindowListener {
		UnknownDataSet ds;
		JList hidden,visible;
		public Vector<String> hiddenV, visibleV;
		
		public HeaderConfig(JFrame owner, UnknownDataSet ds){
			super(owner,"Exporting to KML: Fields to Export", true);
			this.ds=ds;
			initVectors();
			initGUI();
		}

		public void initVectors(){
			hiddenV = new Vector<String>();
			visibleV = new Vector<String>();
			Vector<String> v = (Vector<String>) ds.header.clone();
			for (int i = 0; i < ds.tm.indexH.size(); i++){
				String columnName = ds.header.get(ds.tm.indexH.get(i));
				if (columnName != XBTable.PLOT_COLUMN_NAME) {
					visibleV.add(columnName);
				}				
				v.remove(columnName);
			}
			v.trimToSize();
			for (int i =0; i < v.size(); i++)
				hiddenV.add(v.get(i));
		}

		public void initGUI(){
			this.addWindowListener(this);
			JPanel p = new JPanel(new BorderLayout());
			p.add(new JLabel("Exclude"),BorderLayout.NORTH);
			hidden = new JList(hiddenV);
			hidden.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			hidden.setLayoutOrientation(JList.VERTICAL);
			JScrollPane sp = new JScrollPane(hidden);
			//sp.setPreferredSize(hidden.getPreferredSize());
			p.add(sp);
			JPanel p2 = new JPanel();
			p2.add(p);

			p = new JPanel(new GridLayout(4,1));
			JButton b = new JButton("<<");
			b.setActionCommand("<<");
			b.addActionListener(this);
			p.add(b);
			b = new JButton("<");
			b.setActionCommand("<");
			b.addActionListener(this);
			p.add(b);
			b = new JButton(">");
			b.setActionCommand(">");
			b.addActionListener(this);
			p.add(b);
			b = new JButton(">>");
			b.setActionCommand(">>");
			b.addActionListener(this);
			p.add(b);
			p2.add(p);

			p = new JPanel(new BorderLayout());
			p.add(new JLabel("Export"), BorderLayout.NORTH);
			visible = new JList(visibleV);
			visible.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			visible.setLayoutOrientation(JList.VERTICAL);
			JScrollPane sp2 = new JScrollPane(visible);
			p.add(sp2);
			p2.add(p);

			Dimension d;
			if (hidden.getPreferredSize().width>visible.getPreferredSize().width) d = sp.getPreferredSize();
			else d = sp2.getPreferredSize();
			sp.setPreferredSize(d);
			sp2.setPreferredSize(d);
			
			p = new JPanel(new BorderLayout());
			p.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			p.add(p2);

			p2 = new JPanel(new GridLayout(2,1));
			b = new JButton(Character.toString((char)8743));
			b.setActionCommand("up");
			b.addActionListener(this);
			p2.add(b);
			b = new JButton(Character.toString((char)8744));
			b.setActionCommand("down");
			b.addActionListener(this);
			p2.add(b);
			p.add(p2, BorderLayout.EAST);

			p2 = new JPanel();
			b = new JButton("Cancel");
			b.setActionCommand("cancel");
			b.addActionListener(this);
			p2.add(b);
			b = new JButton("Next");
			b.setActionCommand("next");
			b.addActionListener(this);
			p2.add(b);
			p.add(p2,BorderLayout.SOUTH);

			setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			getContentPane().add(p);
			pack();

			setLocationRelativeTo(MapApp.anchor);
			setVisible(true);
		}

		public void actionPerformed(ActionEvent e) {
			String ac = e.getActionCommand();
			if (ac.equals("next")) ok();
			else if (ac.equals("cancel")) cancel();
			else if (ac.equals("<<")) allLeft();
			else if (ac.equals("<")) left();
			else if (ac.equals(">")) right();
			else if (ac.equals(">>")) allRight();
			else if (ac.equals("up")) up();
			else if (ac.equals("down")) down();
		}

		public void ok(){
			if (visibleV.size()==0) return;

			setVisible(false);
		}

		public void cancel(){
			dispose();
			visibleV = null;
		}

		public void allLeft(){
			visibleV=new Vector<String>();
			hiddenV=new Vector<String>(ds.header.size());
			for (int i = 0; i < ds.header.size(); i++)
				hiddenV.add(i, ds.header.get(i));

			hidden.setListData(hiddenV);
			visible.setListData(visibleV);
		}

		public void left(){
			Object o[] = visible.getSelectedValuesList().toArray();
			for (int i = 0; i < o.length; i++) {
				visibleV.remove(o[i]);
				hiddenV.add((String) o[i]);
			}

			hidden.setListData(hiddenV);
			visible.setListData(visibleV);
		}

		public void right(){
			Object o[] = hidden.getSelectedValuesList().toArray();
			for (int i = 0; i < o.length; i++) {
				visibleV.add((String) o[i]);
				hiddenV.remove(o[i]);
			}

			hidden.setListData(hiddenV);
			visible.setListData(visibleV);
		}

		public void up() {
			int[] s = visible.getSelectedIndices();
			if (s.length == 0) return; //nothing
			boolean[] moved = new boolean[s.length];
			
			for (int i = 0; i < s.length; i++) {
				if (s[i] == 0) 
					continue;
				if (i > 0 && s[i-1] == s[i] - 1 && !moved[i-1])
					continue;
				moved[i] = true;
				visibleV.add(s[i]-1, visibleV.remove(s[i]));
			}
			visible.setListData(visibleV);

			visible.setSelectedIndices(new int[] {});
			for (int i = 0; i < s.length; i++) {
				if (moved[i])
					visible.addSelectionInterval(s[i]-1, s[i]-1);
				else 
					visible.addSelectionInterval(s[i], s[i]);
			}
		}

		public void down() {
			int[] s = visible.getSelectedIndices();
			if (s.length == 0) return; //nothing
			boolean[] moved = new boolean[s.length];
			
			for (int i = s.length - 1; i >= 0 ; i--) {
				if (s[i] == visibleV.size()-1) 
					continue;
				if (i < s.length - 1 && s[i+1] == s[i] + 1 && !moved[i+1])
					continue;
				moved[i] = true;
				visibleV.add(s[i]+1, visibleV.remove(s[i]));
			}
			visible.setListData(visibleV);

			visible.setSelectedIndices(new int[] {});
			for (int i = 0; i < s.length; i++) {
				if (moved[i])
					visible.addSelectionInterval(s[i]+1, s[i]+1);
				else 
					visible.addSelectionInterval(s[i], s[i]);
			}
		}

		public void allRight(){
			visibleV=new Vector<String>(ds.header.size());
			hiddenV=new Vector<String>();
			for (int i = 0; i < ds.header.size(); i++)
				visibleV.add(i, ds.header.get(i));

			hidden.setListData(hiddenV);
			visible.setListData(visibleV);
		}

		public void windowActivated(WindowEvent e) {}
		public void windowClosed(WindowEvent e) {}
		public void windowClosing(WindowEvent e) {
			cancel();
		}
		public void windowDeactivated(WindowEvent e) {}
		public void windowDeiconified(WindowEvent e) {}
		public void windowIconified(WindowEvent e) {}
		public void windowOpened(WindowEvent e) {}
	}

	private static class Task extends Thread {
		protected Object[] args;
		public Task(Object[] args) {
			this.args = args;
		}
	}
	
	public static void main(String [] args) throws Exception {
	}
}

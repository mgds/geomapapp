package haxby.wms;

import haxby.map.MapApp;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.geomapapp.io.GMARoot;
import org.xml.sax.SAXException;

public class WMSViewServer implements ActionListener {
	haxby.map.MapApp mapApp;
	JFrame frame;
	JPanel contentPane,
			firstPane,
			secondPane,
			p,
			p2,
			p3;
	JPanel thirdPane = null;
	LayerExplorer layerExplorer = null;
	JLabel exampleWMSURL,
			connectionLabel,
			connectedLabel;
	JTextField txt;
	JButton connect,
			disposeButton,
			deleteLayer,
			loadLayer,
			bookmarkLayer;
	JComboBox layersList = null;
	Vector serverNames;// = new Vector();
	Vector layerNames;// = new Vector();
	Hashtable layerNamesMap,
				layerLayerMap,
				newServerNames;// = new Hashtable();
	Capabilities capabilities;
	URL capabilitiesURL = null;
	URL layerURL = null;
	URL remoteWMSURL = null;
	String currentLayerName = null;
	WMSThread connectWMSThread = null;
	Thread firstWMSThread =null;
	public static JComboBox serverList = null;
	private static File wmsDir = new File(GMARoot.getRoot() + File.separator + "wms_layers");
	private static File wmsFile = new File(wmsDir + File.separator + "wms_layers_list.dat");

	public WMSViewServer(haxby.map.MapApp owner) {
		mapApp = owner;
		newServerNames = new Hashtable();

		try {
			remoteWMSURL = URLFactory.url(
					PathUtil.getPath("WMS_SERVER_LIST", 
							MapApp.BASE_URL+"/gma_wms/wms_servers.dat"));
			try {
				BufferedReader in = new BufferedReader( new InputStreamReader( remoteWMSURL.openStream() ) );
				String s = in.readLine();
				String [] results = s.split("\t");

				if( s==null || s.equals("null") ) {
					System.out.println( "WMS file empty" );
				} else {
					newServerNames.put(results[0], results[1]);
					serverNames = new Vector();
					serverNames.add(s);

					while ( ( s = in.readLine() ) != null ) {
						results = s.split("\t");
						newServerNames.put(results[0], results[1]);
						serverNames.add(s);
					}
				}
				in.close();
			} catch (IOException e) {}
		} catch ( MalformedURLException ex ) {}
	}

	public void remoteWMS() throws Exception {
		if (serverNames == null) serverNames = new Vector();
		else serverNames.clear();
		if (layerNames == null) layerNames = new Vector();
		else layerNames.clear();
		if (layerLayerMap == null) layerLayerMap = new Hashtable();
		else layerLayerMap.clear();
		if (layerNamesMap == null) layerNamesMap = new Hashtable();
		else layerNamesMap.clear();

		if ( wmsDir.exists() && wmsFile.exists() ) {
			try {
				ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(wmsFile)));

				layerNames = (Vector) ois.readObject();
				layerNamesMap = (Hashtable) ois.readObject();
				layerLayerMap = (Hashtable) ois.readObject();
			} catch (InvalidClassException ex) {
				wmsFile.delete();
			}
			catch (IOException ex) {
				wmsFile.delete();
			}
		}
		else {
			if ( !wmsDir.exists() ) {
				wmsDir.mkdir();
			}
			wmsFile.createNewFile();
		}

		frame = new JFrame( "Connect to WMS" );
		frame.setLocationRelativeTo(mapApp.getFrame());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		contentPane = new JPanel( new BorderLayout( 10, 10 ) );
		firstPane = new JPanel( new BorderLayout( 7, 7 ) );
		secondPane = new JPanel( new BorderLayout( 5, 5 ) );
//		exampleWFSURLPane = new JPanel( new BorderLayout() );
//		connectPane = new JPanel( new BorderLayout() );
		exampleWMSURL = new JLabel( "Example: \"https://neo.gsfc.nasa.gov/wms/wms?request=GetCapabilities\"" );
		txt = new JTextField( 50 );
		connect = new JButton( "Connect" );
		disposeButton = new JButton( "Dispose" );
		disposeButton.setEnabled(false);

		Border emptyBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		Border lineBorder = BorderFactory.createLineBorder( Color.black );
		Border titledBorder = BorderFactory.createTitledBorder( lineBorder, "Enter a WMS GetCapabilities URL" );
		Border compBorder = BorderFactory.createCompoundBorder( titledBorder, emptyBorder );
		contentPane.setBorder( emptyBorder );
		firstPane.setBorder( compBorder );
		connect.addActionListener(this);
		disposeButton.addActionListener(this);
		txt.addActionListener(this);
		firstPane.add( exampleWMSURL, BorderLayout.NORTH );
		firstPane.add( txt, BorderLayout.CENTER );
		firstPane.add( connect, BorderLayout.WEST );
		firstPane.add( disposeButton, BorderLayout.EAST );
		firstPane.add(secondPane, BorderLayout.SOUTH);

		Vector tempServerNames = new Vector();
		Object element = "--- Select a Web Map Service Server ---";
		tempServerNames.add(0, element);
		for (Enumeration e = newServerNames.keys(); e.hasMoreElements() ;) {
			tempServerNames.add(e.nextElement());
		}

		Collections.sort(tempServerNames);

		p = new JPanel(new BorderLayout());
		serverList = new JComboBox(tempServerNames);
		serverList.setSelectedIndex(0);
		serverList.addActionListener(this);
		p.add( serverList, BorderLayout.CENTER);
		p.add( new JLabel("Servers: "), BorderLayout.WEST);
		secondPane.add( p, BorderLayout.NORTH);

		p2 = new JPanel(new BorderLayout());
		layersList = new JComboBox(layerNames);
		layersList.setSelectedIndex(-1);
		layersList.addActionListener(this);
		p2.add( layersList, BorderLayout.CENTER);
		p2.add( new JLabel("Layers:   "), BorderLayout.WEST);
		deleteLayer = new JButton("Remove Bookmark");
		deleteLayer.addActionListener(this);
//		deleteLayer.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));
		p2.add(deleteLayer, BorderLayout.EAST);
		secondPane.add( p2, BorderLayout.CENTER);

		//Connection message
		p3 = new JPanel(new BorderLayout(5,0));
		connectionLabel = new JLabel();
		p3.add(connectionLabel, BorderLayout.WEST);

		secondPane.add( p3, BorderLayout.SOUTH);

		contentPane.add( firstPane, BorderLayout.NORTH );
		contentPane.setOpaque(true);
		contentPane.setMaximumSize(new Dimension(450,600));

		frame.setContentPane(contentPane);
		frame.pack();
		frame.setVisible(true);
	}

	public boolean checkFrame() {
		boolean frameOn = false;
		if ( frame != null ) {
			frameOn = true;
		}
		return frameOn;
	}

	public void showFrame() {
		if ( frame != null ) {
			frame.setVisible(true);
		}
	}

	public void setCapabilitiesURL( URL inputCapabilitiesURL ) {
		capabilitiesURL = inputCapabilitiesURL;
	}

	public URL getCapabilitiesURL() {
		return capabilitiesURL;
	}

	public void setCurrentLayerName( String inputCurrentLayerName ) {
		currentLayerName = inputCurrentLayerName;
	}

	public void dispose() {
		if ( layerExplorer != null ) {
			contentPane.remove(layerExplorer);
		}
		if ( thirdPane != null ) {
			contentPane.remove(thirdPane);
			p3.removeAll();
		}
		frame.pack();
		capabilities = null;
		layerExplorer = null;
		thirdPane = null;
		capabilitiesURL = null;
		currentLayerName = null;

		mapApp.addWMSLayer(null, (double[])null, null);
		System.gc();
	}

	public void reset() {
		txt.setText(null);
		disposeButton.setEnabled(false);
		serverList.setSelectedIndex(0);
		connect.setEnabled(true);
		connectionLabel = new JLabel();
		p3.removeAll();
		frame.pack();
		System.gc();
	}

	public void bookmarkLayer() {
		// Get our layer
		Layer layer = layerExplorer.getSelectedLayer();
		if (layer == null) return;
		else if (!layer.isRequestable()) return;

		String layerTitle = layerExplorer.getSelectedLayerTitle();
		String layerString = layerTitle + "\t" + 
							layerExplorer.getSelectedLayerURL() + "\t" +
							layerExplorer.getCapabilitiesURL();

//		layerNames.add(layerTitle);
		((DefaultComboBoxModel) layersList.getModel()).addElement(layerTitle);
		layerNamesMap.put(layerTitle, layerString);
		layerLayerMap.put(layerTitle, layer);

		try {
			ObjectOutputStream oos = new ObjectOutputStream(
										new BufferedOutputStream(
											new FileOutputStream(wmsFile, false)));
			oos.writeObject(layerNames);
			oos.writeObject(layerNamesMap);
			oos.writeObject(layerLayerMap);
			oos.flush();
			oos.close();

			JOptionPane.showMessageDialog(frame, "Layer Bookmarked under name: " + layerTitle);

		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	public void readCapabilities( URL inputCapabilitiesURL ) {
		try {
			capabilities = CapabilitiesParser.parseCapabilities(inputCapabilitiesURL);

			String[] imageFormats = capabilities.getSupportedFormats();
			boolean supportedReader = false;
			for (int i = 0; i < imageFormats.length; i++) {
				Iterator it = ImageIO.getImageReadersByMIMEType(imageFormats[i]);
				if (it.hasNext())
					supportedReader = true;
			}
			if (!supportedReader) {
				JOptionPane.showMessageDialog(MapApp.anchor, "No Supported Image Formats Available",
										"Web Map Service does not supply any supported image formats", JOptionPane.ERROR_MESSAGE);
				capabilities = null;
				return;
			}

			layerExplorer = new LayerExplorer(capabilities);
			layerExplorer.addTreeSelectionListener(
					new TreeSelectionListener() {
						public void valueChanged(TreeSelectionEvent e) {
							TreePath path = e.getNewLeadSelectionPath();
							boolean tf = false;
							if (path == null) tf = false;
							else {
								Object obj = path.getLastPathComponent();
								if (obj instanceof Style)
									tf = Capabilities.isRequetable((Style)obj);
								else if (obj instanceof Layer)
									tf = Capabilities.isRequetable((Layer)obj);
								else 
									tf = false;
							}
							loadLayer.setEnabled(tf);
							bookmarkLayer.setEnabled(tf);
						}
					});
			// If it isn't the same connection don't display last results
			if(inputCapabilitiesURL.toString().compareTo(getCapabilitiesURL().toString()) == 0) {
				contentPane.add(layerExplorer);

				thirdPane = new JPanel(new FlowLayout());
				loadLayer = new JButton("Load Layer");
				loadLayer.setEnabled(false);
				loadLayer.addActionListener(this);
				thirdPane.add(loadLayer);

				bookmarkLayer = new JButton("Bookmark Layer");
				bookmarkLayer.addActionListener(this);
				bookmarkLayer.setEnabled(false);
				thirdPane.add(bookmarkLayer);
				contentPane.add(thirdPane, BorderLayout.SOUTH);

				//Display connect status
				p3.removeAll();
				connectionLabel = new JLabel("<html><font color=#2DAF73>Connected!");
				p3.add(connectionLabel, BorderLayout.WEST);
				frame.pack();
			}
		} catch (SAXException e) {
			JOptionPane.showMessageDialog(frame, "URL Yielded an invalid GetCapabilities File", "Invalid URL", JOptionPane.ERROR_MESSAGE);
			capabilitiesURL = null;
			p3.removeAll();
			frame.pack();
			e.printStackTrace();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(frame, "Could not Connect to URL" + "\n" +
								e.getMessage(), "Invalid URL", JOptionPane.ERROR_MESSAGE);
			capabilitiesURL = null;
			p3.removeAll();
			frame.pack();
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			JOptionPane.showMessageDialog(frame, "Could not configure XML Parsing Correctly" +
								"\n" + e.getMessage(), "Invalid Parsing", JOptionPane.ERROR_MESSAGE);
			capabilitiesURL = null;
			p3.removeAll();
			frame.pack();
			e.printStackTrace();
		} catch (FactoryConfigurationError e) {
			JOptionPane.showMessageDialog(frame, "Could not configure XML Parsing Correctly" + "\n" +
								e.getMessage(), "Invalid Parsing", JOptionPane.ERROR_MESSAGE);
			capabilitiesURL = null;
			p3.removeAll();
			frame.pack();
			e.printStackTrace();
		} catch (URISyntaxException e) {
			JOptionPane.showMessageDialog(frame, "Invalid URL" + "\n" + e.getMessage(), "Invalid URL", JOptionPane.ERROR_MESSAGE);
			p3.removeAll();
			frame.pack();
			e.printStackTrace();
		}
	}

	public void loadLayer(String requestURL, Layer layer) {
		// send to handle which projection for srs, crs
		mapApp.addWMSLayer(layer, requestURL);
	}

	public void loadLayer(String requestURL, Style style) {
		String legendURLs[][] = new String[1][];
		legendURLs[0] = style.getLegendURLs();

		if (legendURLs[0] != null) {
			new WMSLegendDialog(mapApp.getFrame(), legendURLs, style.getParent().getName());
		}
		loadLayer(requestURL, style.getParent());
	}

	private void loadBookmark() {
		if (layersList.getSelectedIndex() == -1) return;
		if (layerNamesMap.get(layersList.getSelectedItem()) == null) return;

		String[] split = ((String)layerNamesMap.get(layersList.getSelectedItem())).split("\\t");
		String requestURL = split[1];
		String capURL = split[2];

		txt.setText(capURL);
		connect.doClick();

		Layer layer = (Layer) layerLayerMap.get(layersList.getSelectedItem());
		loadLayer(requestURL, layer);
	}

	private void deleteLayer() {
		if (layersList.getSelectedIndex() == -1) return;
		if (layerNamesMap.get(layersList.getSelectedItem()) == null) return;

		String layerTitle = layersList.getSelectedItem().toString();

		((DefaultComboBoxModel) layersList.getModel()).removeElement(layerTitle);
		layerNamesMap.remove(layerTitle);
		layerLayerMap.remove(layerTitle);

		try {
			ObjectOutputStream oos = new ObjectOutputStream(
										new BufferedOutputStream(
											new FileOutputStream(wmsFile, false)));
			oos.writeObject(layerNames);
			oos.writeObject(layerNamesMap);
			oos.writeObject(layerLayerMap);
			oos.flush();
			oos.close();

			layersList.setSelectedIndex(-1);
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	private void exits() {
		frame.dispose();
	}

	public void actionPerformed(ActionEvent e) {
		// Action on connect
		if ( e.getSource() == connect || e.getSource() == txt ) {
			connect.setEnabled(false);
			disposeButton.setEnabled(true);

			//Read capabilities and return
			if (capabilitiesURL !=  null && capabilitiesURL.toString().equals(txt.getText()))
				return;
			dispose();
			//Try to connect with URL check if it is invalid
			try {
				String tmp = txt.getText().trim();
				tmp = tmp.replace("?&", "?");
				if (tmp.indexOf("?") != -1) {
					String params = tmp.substring(tmp.indexOf("?")+1, tmp.length());
					String[] keyValues = params.split("&");

					tmp = tmp.substring(0, tmp.indexOf("?") + 1);
			//System.out.println(keyValues.length + " l " + keyValues[0].toString() + " " + keyValues[1].toString());
					for (String keyValue : keyValues) {
						String[] split = keyValue.split("=");
						if (split[0].equalsIgnoreCase("REQUEST")) {
							split[1] = "GetCapabilities";
						}
						if (split[0].equalsIgnoreCase("VERSION")) {
							if(split[1].equalsIgnoreCase("1.3.0")) {
								split[1] = "1.3.0";
							} else {
								split[1] = "1.1.1";
							}
						}
						if (split[0].equalsIgnoreCase("SERVICE")) {
							split[1] = "WMS";
						}
						tmp += split[0] + "=" + split[1] + "&";
					}
				}
				else {
					tmp += "?";
					tmp += "REQUEST=GetCapabilities&";
					tmp += "VERSION=1.1.1&";
					tmp += "SERVICE=WMS&";
				}
				capabilitiesURL = URLFactory.url(tmp);
			} catch (MalformedURLException e1) {
				reset();
				JOptionPane.showMessageDialog(MapApp.anchor, "Invalid URL", "MalformedURLException", JOptionPane.ERROR_MESSAGE);
				return;
			}
			// Send I/O stream connection.
			connectWMSThread = new WMSThread();
			connectWMSThread.start();
		}
		else if ( e.getSource() == serverList ) {
			if(serverList.getSelectedItem().toString() !="--- Select a Web Map Service Server ---"){
				txt.setText(newServerNames.get(serverList.getSelectedItem().toString()).toString());
			}else{
				txt.setText("");
			}
		}
		else if ( e.getSource() == layersList) {
			loadBookmark();
		}
		else if (e.getSource() == loadLayer) {
			Style s = layerExplorer.getSelectedStyle();
			String selectedLayerURL = layerExplorer.getSelectedLayerURL();
			if (s == null) {
				Layer l = layerExplorer.getSelectedLayer();
				if (l.getStyles().length == 1) {
					loadLayer(selectedLayerURL, l.getStyles()[0]);
				} else {
					loadLayer(selectedLayerURL, layerExplorer.getSelectedLayer());
				}
			}
			else {
				loadLayer(selectedLayerURL, s);
			}
		}
		else if (e.getSource() == bookmarkLayer) {
			bookmarkLayer();
		}
		else if (e.getSource() == deleteLayer) {
			deleteLayer();
		}
		else if ( e.getSource() == disposeButton ) {
			reset();
			dispose();
			exits();
			mapApp.invokeWMS();
		}
	}

	class WMSThread extends Thread {
		volatile boolean stop = false;
		public void run() {
			//Display connect status
			p3.remove(connectionLabel);
			connectionLabel = new JLabel("<html><font color=#2554C7>Connecting ..." +
					"<br> Please be patient:<br> Loading times may vary depending on " +
					"server traffic and connection speed." +
					"<br>You will be notified of a successful/unsuccessful connection.</font></html>");
			p3.add(connectionLabel, BorderLayout.WEST);
			frame.pack();

			URLConnection check = null;
			// Check I/O stream is okay before getting anything.
			try {
				check = capabilitiesURL.openConnection();
				check.getInputStream();
				if(check == null) {
					// Do nothing
					return ;
				} else if (check != null) {
					Thread.sleep(20);
					if(!stop) {
						//Stream is okay go read capabilities
						readCapabilities(capabilitiesURL);
						System.out.println("Successful Connection");
					return;
					} else {
						// Do nothing
						stop = false;
						return;
					}
				}
			} catch (IOException e) {
				// Stream failed inform user
				System.out.println(e.getMessage());
				if(connectionLabel.getText().contains("Connecting")) {
					p3.remove(connectionLabel);
					connectionLabel = new JLabel("<html><font color=#CC6633>Disconnected. The Operation Timed Out, Unsuccessful." +
							"<br>Please dispose and try another selection.</font></html>");
					p3.add(connectionLabel, BorderLayout.WEST);
					frame.pack();
				}
				return;
			} catch (InterruptedException ie) {
				ie.printStackTrace();
				return;
			}
		}
	}
}
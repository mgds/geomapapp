package haxby.wfs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.geomapapp.io.GMARoot;

import haxby.map.MapApp;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

/**
 * This WFS interface works to connect,read and retrieve data from a variety of hosted
 * Web Feature Services. There are refined filter options for results on a region and
 * a certain feature type. There are also further filters for a specific feature type. 
 * 
 * @author Andrew K. Melkonian
 * @author Samantha Chan
 */
public class WFSViewServer implements ActionListener {
	haxby.map.MapApp mapApp;
	JFrame frame;
	JPanel contentPane,
			firstPane,
			filterPane,
			loadButtonPane,
			secondTopRowPane;
	JPanel secondPane = null;
	JPanel thirdPane = null;
	JPanel stdscalePane = null;
	JPanel bboxPane = null;
	JPanel bboxContPane = null;
	JPanel exampleWFSURLPane = null;
	JPanel connectPane = null;
	JPanel connectButtonPane = null;
	JPanel textPane = null;
	JPanel disposePane = null;
	JPanel cboxPane = null;
	JPanel bboxComboPane = null;
	JPanel featureComboPane = null;
	JLabel exampleWFSURL;
	JLabel stdscaleLabel = new JLabel("Enter STDSCALE: ");
	JLabel bboxLabel = new JLabel("Enter bounding-box parameters: ");
	JLabel noticeLabel = new JLabel("<html><font color=#2554C7>\n" +
		"Loading times may vary depending on WFS server traffic and connection speed.<br>" +
		"Large data sets may require more memory allocation.<br></html>");
	JLabel gridImageLabel = null;
	JTextField txt,
				stdscaleTxt,
				northTxt,
				southTxt,
				eastTxt,
				westTxt;
	JButton connect,
			disposeButton,
			loadLayer,
			loadCurrentViewB;
	JComboBox layerList;
	JComboBox serverList = null;
	JComboBox bboxList = null;
	JComboBox filterList = null;
	JComboBox filterValueList = null;
	Vector layerNames,
			layerhasFilter,
			filterNames,
			filterValues;
	Vector serverNames = null;
	Vector bboxNames = null;
	Vector filterNamesDefault = null;
	Vector filterValuesDefault = null;
	String[][] anArray =null;
	URL capabilitiesURL = null;
	URL layerURL = null;
	URL remoteWFSURL = null;
	URL bboxURL = null;
	String currentWFSName = null;
	String currentLayerName = null;
	String currentWFSTitle = null;
	String currentWFSBbox = null;
	String currentFilterName = null;
	String currentFilterValue = null;
	String STDSCALE = "&STDSCALE=";
	String BBOX = "&bbox=";
	String currentViewBbox = "&bbox=";
	String currentViewCoordinate=null;
	String worldBbox = "World, -180 180 -90 90";
//	JLabel featureSelectLabel = new JLabel("Select Feature");
//	JLabel bboxSelectLabel = new JLabel("Select Bounding Box");
	String featureSelectLabel = "Select Feature";
	String bboxSelectLabel = "Select Bounding Box";
	String filterTypeSelectLabel = "Select Filter Type";
	String filterValueSelectLabel = "Select Filter Value";
	Hashtable newServerNames = null;
	Hashtable serverBBoxes = null;
	Hashtable bboxes = null;
	Hashtable hideBBoxList = null;
	File wfsDir = new File( GMARoot.getRoot() + File.separator + "wfs_servers");
	File wfsFile = new File( wfsDir + File.separator + "wfs_server_list.dat");

	public WFSViewServer( haxby.map.MapApp owner ) {
		mapApp = owner;

		newServerNames = new Hashtable();
		serverBBoxes = new Hashtable();
		bboxes = new Hashtable();
		hideBBoxList = new Hashtable();
		bboxNames = new Vector();
		filterNamesDefault = new Vector();
		filterNamesDefault.add("No filters available");

		filterValuesDefault = new Vector();
		filterValuesDefault.add("No filters available");

		try {
			String gridImageURL = PathUtil.getPath("WFS/GRID_IMAGE", MapApp.BASE_URL+"/gma_wfs/wfsgridsimagemod3.jpg");
			gridImageLabel = new JLabel( "Grids", new ImageIcon(URLFactory.url(gridImageURL) ), JLabel.CENTER );
		} catch ( MalformedURLException ex ) {
		}

		try {
			String serversURL = PathUtil.getPath("WFS/SERVER_LIST", MapApp.BASE_URL+"/gma_wfs/wfs_servers_new.dat");
			remoteWFSURL = URLFactory.url(serversURL);
			try {
				BufferedReader in = new BufferedReader( new InputStreamReader( remoteWFSURL.openStream() ) );
				String s = in.readLine();
				// If the file is null stop
				if( s==null || s.equals("null") ) {
					System.out.println( "WFS file empty" );
				}else {
					// Splits on spacing
					String [] results = s.split("\\s");
					serverBBoxes.put(results[1], results[2]);
					newServerNames.put(results[0], results[1]);
					s = results[1];
					if(results.length > 3){
						hideBBoxList.put(results[0], results[3]);
					}else if(results.length <=3){
						hideBBoxList.put(results[0], "0");
					}
					serverNames = new Vector();
					serverNames.add(s);
					while ( ( s = in.readLine() ) != null ) {
						results = s.split("\\s");
						//Fourth split is the controller for enabling or disabling the bbox list
						if(results.length > 3){
							hideBBoxList.put(results[0], results[3]);
						}else if(results.length <=3){
							hideBBoxList.put(results[0], "0");
						}
						serverBBoxes.put(results[1], results[2]);
						s = results[1];

						newServerNames.put(results[0], results[1]);
						serverNames.add(s);
					}
				}
				in.close();
			} catch (IOException e) {}
		} catch ( MalformedURLException ex ) {
		}
	}

	public void remoteWFS() throws Exception {
		if ( serverNames == null ) {
			serverNames = new Vector();
		}
		if ( wfsDir.exists() && wfsFile.exists() ) {
			BufferedReader in = new BufferedReader( new FileReader(wfsFile) );
			String s = in.readLine();
			if( s==null || s.equals("null") ) {
				System.out.println( "WFS file empty" );
			}
			else {
				if ( !newServerNames.containsKey(s) ) {
					newServerNames.put(s, s);
				}
				if ( !serverNames.contains(s) ) {
					serverNames.add(s);
				}
				while ( ( s = in.readLine() ) != null ) {
					if ( !newServerNames.containsKey(s) ) {
						newServerNames.put(s, s);
					}
					if ( !serverNames.contains(s) ) {
						serverNames.add(s);
					}
				}
			}
			in.close();
		}
		else {
			if ( !wfsDir.exists() ) {
				wfsDir.mkdir();
			}
			wfsFile.createNewFile();
		}

//		BufferedReader in = new BufferedReader( new InputStreamReader( capabilitiesURL.openStream() ) );

		frame = new JFrame( "Connect to WFS" );
		frame.setLocationRelativeTo(mapApp.getFrame());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		contentPane = new JPanel( new BorderLayout( 10, 10 ) );
		firstPane = new JPanel( new BorderLayout( 5, 5 ) );
//		firstPane = new JPanel( new GridLayout(2, 1));
//		exampleWFSURLPane = new JPanel( new BorderLayout() );
//		connectPane = new JPanel( new BorderLayout() );

//		***** GMA 1.6.2: Changed example URL in "Connect to WFS"
		exampleWFSURL = new JLabel( "Example: \"https://www.marine-geo.org/services/mapserver/eventwfs?request=GetCapabilities&service=WFS&version=1.0.0\"" );
//		***** GMA 1.6.2

		txt = new JTextField( 10 );
		connect = new JButton( "Connect" );
		disposeButton = new JButton( "Dispose" );
		disposeButton.setEnabled(false);

//		connectButtonPane = new JPanel();
//		textPane = new JPanel();
//		disposePane = new JPanel();
//		cboxPane = new JPanel();

		Border emptyBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		Border lineBorder = BorderFactory.createLineBorder( Color.black );
		Border titledBorder = BorderFactory.createTitledBorder( lineBorder, "Enter a WFS GetCapabilities URL" );
		Border compBorder = BorderFactory.createCompoundBorder( titledBorder, emptyBorder );
		contentPane.setBorder( emptyBorder );
		firstPane.setBorder( compBorder );

		connect.addActionListener(this);
		disposeButton.addActionListener(this);
		txt.addActionListener(this);

		firstPane.add( exampleWFSURL, BorderLayout.NORTH );
		firstPane.add( txt, BorderLayout.CENTER );
		firstPane.add( connect, BorderLayout.WEST );
		firstPane.add( disposeButton, BorderLayout.EAST );
//		exampleWFSURLPane.add( exampleWFSURL, BorderLayout.CENTER );
//		connectButtonPane.add(connect);
//		textPane.add(txt );
//		disposePane.add(disposeButton);

		Vector tempServerNames = new Vector();
		Object element = "--- Select a Web Feature Service ---";
		tempServerNames.add(0, element);
		for (Enumeration e = newServerNames.keys(); e.hasMoreElements() ;) {
			tempServerNames.add((String)e.nextElement());
		}

		Collections.sort(tempServerNames);

		if ( !serverNames.isEmpty() ) {
//			serverList = new JComboBox(serverNames);
			serverList = new JComboBox(tempServerNames);
			txt.setText((String)newServerNames.get(serverList.getSelectedItem().toString()));
			serverList.addActionListener(this);
			firstPane.add( serverList, BorderLayout.SOUTH );
//			cboxPane.add(serverList);
		}

//		connectPane.add(textPane, BorderLayout.EAST);
//		connectPane.add(connectButtonPane, BorderLayout.NORTH);
//		connectPane.add(disposePane, BorderLayout.WEST);
//		connectPane.add(cboxPane, BorderLayout.SOUTH);

//		firstPane.add(exampleWFSURLPane);
//		firstPane.add(connectPane);

		contentPane.add( firstPane, BorderLayout.NORTH );
		contentPane.setOpaque(true);
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

	public void chooseLayer() {
		if ( capabilitiesURL != null ) {
			if ( !serverNames.contains( capabilitiesURL.toString() ) ) {
				if ( serverNames.isEmpty() && serverList == null ) {
					serverList = new JComboBox(serverNames);
					txt.setText((String)newServerNames.get(serverList.getSelectedItem().toString()));
					serverList.addActionListener(this);
					firstPane.add( serverList, BorderLayout.CENTER );
				}

				newServerNames.put(capabilitiesURL.toString(), capabilitiesURL.toString());

				serverNames.add( capabilitiesURL.toString() );
				serverList.setSelectedIndex( serverList.getItemCount() - 1 );
				serverList.repaint();
				firstPane.repaint();
				try {
					BufferedWriter out = new BufferedWriter( new FileWriter(wfsFile, true) );
					out.write( capabilitiesURL.toString() + "\r\n" );
					out.flush();
					out.close();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(MapApp.anchor, "Error writing to wfs_server_list.dat", "File I/O Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}

//		secondPane = new JPanel( new GridLayout(1, 3, 30, 10) );
//		secondPane = new JPanel( new BorderLayout(20, 10) );
//		secondPane = new JPanel( new GridLayout(5, 0, 20, 10) );
		secondTopRowPane = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 10));
		secondPane = new JPanel( new BorderLayout(10, 10) );
		bboxPane = new JPanel ( new GridLayout(3, 3) );
		bboxContPane = new JPanel ( new GridLayout( 1, 2 ) );
		loadButtonPane = new JPanel ( new GridLayout( 2, 1, 0, 5 ) );
		stdscalePane = new JPanel( new BorderLayout() );
		layerList = new JComboBox(layerNames);
		layerList.addActionListener(this);

		Border emptyBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		Border lineBorder = BorderFactory.createLineBorder( Color.black );
		Border titledBorder = BorderFactory.createTitledBorder( lineBorder, featureSelectLabel );
		Border compBorder = BorderFactory.createCompoundBorder( titledBorder, emptyBorder );

		featureComboPane = new JPanel();
		featureComboPane.setBorder(compBorder);
		featureComboPane.add(layerList);

		stdscaleTxt = new JTextField(10);
		stdscaleTxt.addActionListener(this);
		stdscalePane.add(stdscaleLabel, BorderLayout.WEST);
		stdscalePane.add(stdscaleTxt, BorderLayout.CENTER);
		northTxt = new JTextField(5);
		northTxt.addActionListener(this);
		southTxt = new JTextField(5);
		southTxt.addActionListener(this);
		eastTxt = new JTextField(5);
		eastTxt.addActionListener(this);
		westTxt = new JTextField(5);
		westTxt.addActionListener(this);
		bboxPane.add(new JPanel());
		bboxPane.add(northTxt);
		bboxPane.add(new JPanel());
		bboxPane.add(westTxt);
		bboxPane.add(new JPanel());
		bboxPane.add(eastTxt);
		bboxPane.add(new JPanel());
		bboxPane.add(southTxt);
		bboxPane.add(new JPanel());

		bboxes = new Hashtable();
		bboxNames = new Vector();
		try {
			String bboxPath = PathUtil.getPath("WFS/BBOX", MapApp.BASE_URL+"/gma_wfs/bboxes.dat");
			bboxURL = URLFactory.url(bboxPath);
			try {
				BufferedReader inBoxes = new BufferedReader( new InputStreamReader( bboxURL.openStream() ) );
				String sBoxes = null;
				bboxNames.add("Current view");

				if ( serverBBoxes.containsKey(capabilitiesURL.toString()) ) {
					if ( ((String)serverBBoxes.get(capabilitiesURL.toString())).equals("1") ) {
						String [] results = worldBbox.split("\\,");
						bboxes.put(results[0], results[1]);
						sBoxes = results[0];
						bboxNames.add(sBoxes);
					}
				}
				while ( ( sBoxes = inBoxes.readLine() ) != null ) {
					String [] results = sBoxes.split("\\,");
					bboxes.put(results[0], results[1]);
					sBoxes = results[0];
					bboxNames.add(sBoxes);
				}
				inBoxes.close();
			} catch (IOException e) {
			}
		} catch ( MalformedURLException ex ) {
		}
		bboxList = new JComboBox(bboxNames);
		bboxList.addActionListener(this);

		try {
				if ( ((String)hideBBoxList.get(serverList.getSelectedItem().toString())).equals("1")) {
						bboxList.setEnabled(false);
				} else if(((String)hideBBoxList.get(serverList.getSelectedItem().toString())).equals("0")){
					bboxList.setEnabled(true);
				}
		} catch (NullPointerException ne) {
			bboxList.setEnabled(false);
		}

		emptyBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		lineBorder = BorderFactory.createLineBorder( Color.black );
		titledBorder = BorderFactory.createTitledBorder( lineBorder, bboxSelectLabel );
		compBorder = BorderFactory.createCompoundBorder( titledBorder, emptyBorder );

		bboxComboPane = new JPanel();
		bboxComboPane.setBorder( compBorder );
		bboxComboPane.add(bboxList);

//		layerList.setPreferredSize( new Dimension( 60, 40 ) );

		filterList = new JComboBox(filterNamesDefault);
		filterList.setEnabled(false);
		filterList.addActionListener(this);

		filterValueList = new JComboBox(filterValuesDefault);
		filterValueList.addActionListener(this);

		filterPane = new JPanel(new BorderLayout(20,5));
		Border titledFilterBorder = BorderFactory.createTitledBorder( lineBorder, filterTypeSelectLabel );
		Border fBorder = BorderFactory.createCompoundBorder( titledFilterBorder, emptyBorder );
		filterPane.setBorder(fBorder);
		filterPane.add(filterList,BorderLayout.NORTH);

		//Add Load Buttons
		loadLayer = new JButton("Load All Data");
		loadLayer.addActionListener(this);

		loadCurrentViewB = new JButton("Load Data for Current Map View");
		loadCurrentViewB.addActionListener(this);
		//Can't calculate bounding boxes for polar projections, so disable
		//this button for North and South Polar projections.
		if (MapApp.CURRENT_PROJECTION == "s" || MapApp.CURRENT_PROJECTION == "n") {
			loadCurrentViewB.setEnabled(false);
			bboxList.setEnabled(false);
		}

		emptyBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		lineBorder = BorderFactory.createLineBorder( Color.black );
		titledBorder = BorderFactory.createTitledBorder( lineBorder, "Select Feature from " + currentWFSTitle);
		compBorder = BorderFactory.createCompoundBorder( titledBorder, emptyBorder );

		secondPane.setBorder( compBorder );
		secondTopRowPane.add(featureComboPane);
		secondTopRowPane.add(filterPane);
		bboxContPane.add(bboxLabel);
		bboxContPane.add(bboxPane);
		secondTopRowPane.add(bboxComboPane);
		secondTopRowPane.add(loadButtonPane);
		loadButtonPane.add(loadCurrentViewB, BorderLayout.NORTH);
		loadButtonPane.add(loadLayer, BorderLayout.SOUTH);
		secondPane.add(secondTopRowPane,BorderLayout.NORTH);
		secondPane.add(noticeLabel,BorderLayout.SOUTH);
//		secondPane.add(gridImageLabel);

		contentPane.add(secondPane, BorderLayout.CENTER);

		frame.pack();
		frame.setVisible(true);
	}

	public void setCapabilitiesURL( URL inputCapabilitiesURL ) {
		capabilitiesURL = inputCapabilitiesURL;
	}

	public void setCurrentLayerName( String inputCurrentLayerName ) {
		currentLayerName = inputCurrentLayerName;
	}

	public void setCurrentWFSTitle( String inputCurrentWFSTitle) {
		currentWFSTitle = inputCurrentWFSTitle;
	}

	public void setCurrentWFSBbox( String inputCurrentWFSBbox) {
		currentWFSBbox = inputCurrentWFSBbox;
	}
	public StringBuffer getLayer() {
		if ( thirdPane != null) {
			contentPane.remove(thirdPane);
		}
		thirdPane = null;
		layerURL = null;

		String s = "";
		String temp = "";
		String labels = "";
		BufferedReader in;
		int numberOfLines = 0;
		int counter = 0;
		boolean isFeatureMember = false;
		boolean isPoint = false;
		boolean isGeometry = false;
		boolean isBoundedBy = false;
		boolean dataLabelsNotSet = true;
		boolean hasFeatureMembers = false;
		boolean dataToAppend = false;
		currentFilterName = filterList.getSelectedItem().toString();
		currentFilterValue = filterValueList.getSelectedItem().toString();
		StringBuffer sb = new StringBuffer();
//		labels = labels + "Lon\tLat";

		thirdPane = new JPanel( new BorderLayout() );

		Border emptyBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		Border lineBorder = BorderFactory.createLineBorder( Color.black );
		Border titledBorder = BorderFactory.createTitledBorder( lineBorder, "Layer Information for " + currentLayerName );
		Border compBorder = BorderFactory.createCompoundBorder( titledBorder, emptyBorder );

		thirdPane.setBorder( compBorder );

		String bboxInput = null;
		String bboxFilterInput = null;
		//Is null for bbox when selecting current view
		if ( westTxt.getText().equals("") || northTxt.getText().equals("") || eastTxt.getText().equals("") || southTxt.getText().equals("") ) {	
		}
		else if ( checkBoundingBoxInput() ) {
			//Reads bbox format as W,S,E,N
			bboxInput = BBOX + westTxt.getText() + "," + southTxt.getText() + "," + eastTxt.getText() + "," + northTxt.getText();

			//Reads as coordinate format ie. <coordinate>w,s e,n</coordinate>
			bboxFilterInput = westTxt.getText().toString() + "," +southTxt.getText().toString() + "%20" + eastTxt.getText().toString() + "," + northTxt.getText().toString();
			//System.out.println("W: " +westTxt.getText() +" E: " +eastTxt.getText()+ " S: "+ southTxt.getText()+ " N: "+northTxt.getText());
		}

		String filterNameInput = null;
		if ((currentFilterName!="No filters available")){
			filterNameInput = currentFilterName.replaceAll(" ", "%20");
		}

		String filterValueInput = null;
		if ((currentFilterValue !="No filters available")){	
			filterValueInput = currentFilterValue.replaceAll(" ", "%20");
		}

		String stdscaleInput = null;
		if ( stdscaleTxt.getText() != null && !stdscaleTxt.getText().equals("") ) {
			stdscaleInput = STDSCALE + stdscaleTxt.getText();
		}

		if ( capabilitiesURL != null ) {
			int index1 = capabilitiesURL.toString().indexOf( "GetCapabilities" );
			if ( index1 == -1 ) {
				index1 = capabilitiesURL.toString().indexOf( "getcapabilities" );
			}
			int index2 = capabilitiesURL.toString().indexOf( "GetCapabilities" ) + 15;
//			if ( index2 == -1 ) {
//				index2 = capabilitiesURL.toString().indexOf( "getcapabilities" ) + 15;
//			}
			index2 = index1 + 15;

			try {
					String layerURLString = capabilitiesURL.toString().substring(0, index1) + "GetFeature" + capabilitiesURL.toString().substring(index2) + "&typename=" + currentLayerName;
				if ( stdscaleInput != null ) {
					layerURLString += stdscaleInput;
				}
				if ((!currentViewBbox.equals("&bbox=")) && (currentWFSBbox == null)) {
					layerURLString += currentViewBbox;
				}else if(currentWFSBbox !=null){
					String current = "&bbox="+ currentWFSBbox;
					layerURLString += current;
					}else if ( bboxInput != null ) {
					layerURLString += bboxInput;
				}
				if((filterNameInput!=null)&&(filterValueInput !=null)&&(bboxInput != null)){
					layerURLString += "&filter=<Filter><AND><BBOX><PropertyName>MS_GEOMETRY</PropertyName>" +
							"<Box><coordinates>" + bboxFilterInput + "</coordinates></Box></BBOX>" +
							"<PropertyIsEqualTo><PropertyName>" + filterNameInput +"</PropertyName><Literal>" +
							filterValueInput + "</Literal>" + "</PropertyIsEqualTo></AND></Filter>";
				}else if((filterNameInput!=null)&&(filterValueInput !=null)&&(bboxInput == null)){
					String box = currentViewCoordinate.replaceAll("&bbox=", "");
					box = box.replaceAll(" ", "%20");
					layerURLString += "&filter=<Filter><AND><BBOX><PropertyName>MS_GEOMETRY</PropertyName>" +
					"<Box><coordinates>" + box + "</coordinates></Box></BBOX>" +
					"<PropertyIsEqualTo><PropertyName>" + filterNameInput +"</PropertyName><Literal>" +
					filterValueInput + "</Literal>" + "</PropertyIsEqualTo></AND></Filter>";
				}

				// The request url
				System.out.println("request url: " + layerURLString);
				layerURL = URLFactory.url(layerURLString);

				try {
					in = new BufferedReader( new InputStreamReader( layerURL.openStream() ) );
					Vector testerHap = new Vector();

					if ( capabilitiesURL.toString().indexOf(".ldeo") != -1 || capabilitiesURL.toString().indexOf("marine-geo") != -1 || capabilitiesURL.toString().indexOf("gmrt") != -1 || capabilitiesURL.toString().indexOf("ciesin") != -1 ) {
						while ( ( s = in.readLine() ) != null )	{
							s = s.trim();
							//If result set is null prompt user.
							if(s.contentEquals("<gml:null>missing</gml:null>")){
								JOptionPane.showMessageDialog(frame, "The Selected WFS \n" +
														"\n From: " + currentWFSTitle + '\n' +
														"With Feature: " + layerList.getSelectedItem().toString() + '\n' + "With Filter Type: " + filterNameInput + '\n' +
														"With Filter Value: " + filterValueInput +'\n' +"With Region: " + bboxList.getSelectedItem().toString() + '\n' + 
														"\n Has yielded an EMPTY result. Please try again.");
								break;
							}
							if ( isFeatureMember ) {
								if ( isPoint ) {
									if ( s.indexOf( "<gml:coordinate" ) != -1 ) {
										if ( dataLabelsNotSet ) {
											if ( labels.equals("") ) {
												labels = labels + "Lon\tLat";
											}
											else {
												labels = labels + "\tLon\tLat";
											}
										}
										if ( s.length() < 18 ) {
											s = in.readLine();
											s = s.replace( ',', '\t' );
											if ( !dataToAppend ) {
												sb.append(searchAndReplaceAndpersanInURL(s));
												dataToAppend = true;
											}
											else {
												sb.append("\t" + searchAndReplaceAndpersanInURL(s));
											}
											s = in.readLine();
										} else {
											s = s.substring( s.indexOf( "<gml:coordinate" ) + 17, s.indexOf( "</gml:coordinates>" ) );
											s = s.replace( ',', '\t' );
											if ( !dataToAppend ) {
												sb.append(searchAndReplaceAndpersanInURL(s));
												dataToAppend = true;
											}
											else {
												sb.append("\t" + searchAndReplaceAndpersanInURL(s));
											}
										}
									}
								}
								if ( s.indexOf( "boundedBy" ) != -1 ) {
									if ( s.indexOf( "</" ) == -1 ) {
										isBoundedBy = true;
									}
									else {
										isBoundedBy = false;
									}
								}
								else if ( s.indexOf( "Geometry>" ) != -1 || s.indexOf( "geom>" ) != -1 || s.indexOf( "GEOM>" ) != -1 ) {
									if ( s.indexOf( "</" ) == -1 ) {
										isGeometry = true;
									}
									else {
										isGeometry = false;
									}
								}
								else if ( s.indexOf( "gml:Point" ) != -1 ) {
									if ( s.indexOf( "</" ) == -1 ) {
										isPoint = true;
									}
									else {
										isPoint = false;
									}
								}
								if ( !isPoint && !isGeometry && !isBoundedBy && s.indexOf( currentLayerName ) == -1 && s.startsWith("</") == false ) {
									if ( dataLabelsNotSet ) {
										if ( s.indexOf(":") != -1 && s.indexOf(">") != -1 ) {
											if ( labels.equals("") ) {
												labels = labels + s.substring( s.indexOf(":") + 1, s.indexOf(">") );
											}
											else {
												labels = labels + "\t" + s.substring( s.indexOf(":") + 1, s.indexOf(">") );
											}
										}
									}
									if ( s.indexOf("</") != -1 ) {
										if ( !dataToAppend ) {
											sb.append( searchAndReplaceAndpersanInURL( s.substring( s.indexOf(">") + 1, s.indexOf("</") ) ) );
											dataToAppend = true;
										} else {
											if ( s.indexOf(">") < s.indexOf("</") ) {
												sb.append( "\t" + searchAndReplaceAndpersanInURL( s.substring( s.indexOf(">") + 1, s.indexOf("</") ) ) );
											}
											else {
												sb.append( "\t" + searchAndReplaceAndpersanInURL( s.substring( 0, s.indexOf("</") ) ) );
											}
										}
									}
									else {
										if ( !dataToAppend ) {
											sb.append( searchAndReplaceAndpersanInURL( s.substring( s.indexOf(">") + 1 ) ) );
											dataToAppend = true;
										}
										else {
											sb.append( searchAndReplaceAndpersanInURL( "\t" + s.substring( s.indexOf(">") + 1 ) ) );
										}
									}
								}
							}
							if ( s.indexOf( "featureMember" ) != -1 ) {
								if ( s.indexOf( "</" ) == -1 ) {
									isFeatureMember = true;
									hasFeatureMembers = true;
								}
								else {
									isFeatureMember = false;
									if ( dataLabelsNotSet ) {
										dataLabelsNotSet = false;
										labels = labels + "\n";
										sb.insert( 0, labels );
									}
									sb.append("\n");
									dataToAppend = false;
								}
							}
						}
						in.close();
					}
					else {
					while ( ( s = in.readLine() ) != null ) {		// everything else
//						System.out.println(counter);
//						testerHap.add(s);
						temp += s;
/*						temp = s;
						if ( numberOfLines > 1 ) {
							s = s.trim();
							if ( isFeatureMember ) {
								if ( isPoint ) {
									if ( s.indexOf( "<gml:coordinate" ) != -1 ) {
										if ( dataLabelsNotSet ) {
											if ( labels.equals("") ) {
												labels = labels + "Lon\tLat";
											}
											else {
												labels = labels + "\tLon\tLat";
											}
										}
										if ( s.length() < 18 ) {
											s = in.readLine();
											s = s.replace( ',', '\t' );
											if ( !dataToAppend ) {
												sb.append(s);
												dataToAppend = true;
											}
											else {
												sb.append("\t" + s);
											}
											s = in.readLine();
											
										} else {
											s = s.substring( s.indexOf( "<gml:coordinate" ) + 17, s.indexOf( "</gml:coordinates>" ) );
											s = s.replace( ',', '\t' );
											if ( !dataToAppend ) {
												sb.append(s);
												dataToAppend = true;
											}
											else {
												sb.append("\t" + s);
											}
										}	
									}
								}
								if ( s.indexOf( "boundedBy" ) != -1 ) {
									if ( s.indexOf( "</" ) == -1 ) {
										isBoundedBy = true;
									}
									else {
										isBoundedBy = false;
									}
								}
								else if ( s.indexOf( "Geometry>" ) != -1 || s.indexOf( "geom>" ) != -1 || s.indexOf( "GEOM>" ) != -1 ) {
									if ( s.indexOf( "</" ) == -1 ) {
										isGeometry = true;
									}
									else {
										isGeometry = false;
									}
								}
								else if ( s.indexOf( "Point" ) != -1 ) {
									if ( s.indexOf( "</" ) == -1 ) {
										isPoint = true;
									}
									else {
										isPoint = false;
									}
								}
								if ( !isPoint && !isGeometry && !isBoundedBy && s.indexOf( currentLayerName ) == -1 && s.startsWith("</") == false ) {
									if ( dataLabelsNotSet ) {
										if ( s.indexOf(":") != -1 && s.indexOf(">") != -1 ) {
											if ( labels.equals("") ) {
												labels = labels + s.substring( s.indexOf(":") + 1, s.indexOf(">") );
											}
											else {
												labels = labels + "\t" + s.substring( s.indexOf(":") + 1, s.indexOf(">") );
											}
											
										}
									}
									if ( s.indexOf("</") != -1 ) {
										if ( !dataToAppend ) {
											sb.append( s.substring( s.indexOf(">") + 1, s.indexOf("</") ) );
											dataToAppend = true;
										} else {
											sb.append( "\t" + s.substring( s.indexOf(">") + 1, s.indexOf("</") ) );
										}
									}
									else {
										if ( !dataToAppend ) {
											sb.append( s.substring( s.indexOf(">") + 1 ) );
											dataToAppend = true;
										}
										else {
											sb.append( "\t" + s.substring( s.indexOf(">") + 1 ) );
										}
									}
								}
							}
							if ( s.indexOf( "featureMember" ) != -1 ) {
								if ( s.indexOf( "</" ) == -1 ) {
									isFeatureMember = true;
									hasFeatureMembers = true;
								}
								else {
									isFeatureMember = false;
									if ( dataLabelsNotSet ) {
										dataLabelsNotSet = false;
										labels = labels + "\n";
										sb.insert( 0, labels );
									}
									sb.append("\n");
									dataToAppend = false;
								}
							}
						}*/
					}
					// Finished close stream.
					in.close();
//					while ( !testerHap.isEmpty() ) {
//						s = (String)testerHap.firstElement();
//						temp += s;
//						testerHap.remove(s);
//					}
//					if ( numberOfLines == 1 ) {

						if ( temp != null ) {
							String[] result = temp.split("<");
							temp = null;
							System.gc();
							for (int i=2; i<result.length; i++) {
								if ( result[i] != null ) {
									result[i] = "<" + result[i];
									result[i] = result[i].trim();
									if ( isFeatureMember ) {
										if ( isPoint ) {
											if ( result[i].indexOf( "<gml:coordinate" ) != -1 ) {
												if ( dataLabelsNotSet ) {
													if ( labels.equals("") ) {
														labels = labels + "Lon\tLat";
													}
													else {
														labels = labels + "\tLon\tLat";
													}
												}
												if ( result[i].length() < 18 ) {
//													result[i] = in.readLine();
													if ( (i+1) < result.length ) {
														result[i] = result[i+1];
													}
													result[i] = result[i].replace( ',', '\t' );
													if ( !dataToAppend ) {
														sb.append( searchAndReplaceAndpersanInURL(result[i]) );
														dataToAppend = true;
													}
													else {
														sb.append("\t" + searchAndReplaceAndpersanInURL(result[i]));
													}
//													result[i] = in.readLine();
													if ( (i+1) < result.length ) {
														result[i] = result[i+1];
													}
												} else {
//													result[i] = result[i].substring( result[i].indexOf( "<gml:coordinate" ) + 17, result[i].indexOf( "</gml:coordinates>" ) );
//													result[i] = result[i].substring( result[i].indexOf( "<gml:coordinate" ) + 17 );
													result[i] = result[i].substring( result[i].indexOf( ">" ) + 1 );

													result[i] = result[i].replace( ',', '\t' );
													if ( !dataToAppend ) {
														sb.append(searchAndReplaceAndpersanInURL(result[i]));
														dataToAppend = true;
													}
													else {
														sb.append("\t" + searchAndReplaceAndpersanInURL(result[i]));
													}
												}
											}
										}
										if ( result[i].indexOf( "boundedBy" ) != -1 ) {
											if ( result[i].indexOf( "</" ) == -1 ) {
												isBoundedBy = true;
											}
											else {
												isBoundedBy = false;
											}
										}
										else if ( result[i].indexOf( "Geometry>" ) != -1 || result[i].indexOf( "geom>" ) != -1 || result[i].indexOf( "GEOM>" ) != -1 ) {
											if ( result[i].indexOf( "</" ) == -1 ) {
												isGeometry = true;
											}
											else {
												isGeometry = false;
											}
										}
										else if ( result[i].indexOf( "gml:Point" ) != -1 ) {
											if ( result[i].indexOf( "</" ) == -1 ) {
												isPoint = true;
											}
											else {
												isPoint = false;
											}
										}
										if ( !isPoint && !isGeometry && !isBoundedBy && result[i].indexOf( currentLayerName ) == -1 && result[i].startsWith("</") == false ) {
											if ( dataLabelsNotSet ) {
												if ( result[i].indexOf(":") != -1 && result[i].indexOf(">") != -1 ) {
													if ( labels.equals("") ) {
														labels = labels + result[i].substring( result[i].indexOf(":") + 1, result[i].indexOf(">") );
													}
													else {
														labels = labels + "\t" + result[i].substring( result[i].indexOf(":") + 1, result[i].indexOf(">") );
													}
												}
											}
											if ( result[i].indexOf("</") != -1 ) {
												if ( !dataToAppend ) {
//													***** GMA 1.5.8: TESTING
//													System.out.println(result[i].substring( result[i].indexOf(">") + 1, result[i].indexOf("</") ) );
//													sb.append( result[i].substring( result[i].indexOf(">") + 1, result[i].indexOf("</") ) );
													sb.append( searchAndReplaceAndpersanInURL( result[i].substring( result[i].indexOf(">") + 1, result[i].indexOf("</") ) ) );
//													***** GMA 1.5.8
													dataToAppend = true;
												} else {
//													***** GMA 1.5.8: TESTING
//													System.out.println( "\t" + result[i].substring( result[i].indexOf(">") + 1, result[i].indexOf("</") ) );
//													sb.append( "\t" + result[i].substring( result[i].indexOf(">") + 1, result[i].indexOf("</") ) );
													sb.append( searchAndReplaceAndpersanInURL( "\t" + result[i].substring( result[i].indexOf(">") + 1, result[i].indexOf("</") ) ) );
//													***** GMA 1.5.8
												}
											}
											else {
												if ( !dataToAppend ) {
//													***** GMA 1.5.8: TESTING
//													System.out.println( result[i].substring( result[i].indexOf(">") + 1 ) );
//													sb.append( result[i].substring( result[i].indexOf(">") + 1 ) );
													sb.append( searchAndReplaceAndpersanInURL( result[i].substring( result[i].indexOf(">") + 1 ) ) );
//													***** GMA 1.5.8
													dataToAppend = true;
												}
												else {
//													***** GMA 1.5.8: TESTING
//													System.out.println( "\t" + result[i].substring( result[i].indexOf(">") + 1 ) );
//													sb.append( "\t" + result[i].substring( result[i].indexOf(">") + 1 ) );
													sb.append( searchAndReplaceAndpersanInURL( "\t" + result[i].substring( result[i].indexOf(">") + 1 ) ) );
//													***** GMA 1.5.8
												}
											}
										}
									}
									if ( result[i].indexOf( "featureMember" ) != -1 ) {
										if ( result[i].indexOf( "</" ) == -1 ) {
											isFeatureMember = true;
											hasFeatureMembers = true;
										}
										else {
											isFeatureMember = false;
											if ( dataLabelsNotSet ) {
												dataLabelsNotSet = false;
												labels = labels + "\n";
												sb.insert( 0, labels );
											}
											sb.append("\n");
											numberOfLines++;
											// Popup box for user.
											if(numberOfLines >= 100000 && loadCurrentViewB.isEnabled()) {
												JOptionPane.showMessageDialog(frame, "Feature: " + layerList.getSelectedItem().toString() + '\n' +
												"Region: " + bboxList.getSelectedItem().toString() + '\n' +
												"<html><br>The selected WFS has over <b>100,000</b> results.</html> \n" +
												"Please zoom in closer and try re-loading it. \n", "Too Many Results", JOptionPane.WARNING_MESSAGE);
												sb.delete(0, sb.length());
											}
											dataToAppend = false;
										}
									}
								}
							}
						}
					}  // end else
				} catch (IOException e) {
					JOptionPane.showMessageDialog(MapApp.anchor, "Error reading data from WFS", "IOException", JOptionPane.ERROR_MESSAGE);
				}
			} catch ( MalformedURLException mue ) {
				JOptionPane.showMessageDialog(MapApp.anchor, "Failed to retrieve GML", "MalformedURLException", JOptionPane.ERROR_MESSAGE);
			}
		}
		contentPane.add(thirdPane, BorderLayout.SOUTH);
		frame.pack();
		frame.setVisible(true);
		MapApp.sendLogMessage("Imported_WFS&URL="+layerURL);
		return sb;
	}

	public void loadLayer() {
		loadLayer.doClick();
	}

	public void requestCurrentView() {
		if ( layerList.getSelectedItem().toString() == null )
			return;

		if ( mapApp.getMap().getZoom() <= 1.5 ) {
			//System.out.println("whwhaa");
			bboxList.setSelectedItem("World");
			String boundingParameters = (String)bboxes.get(bboxList.getSelectedItem().toString());
			boundingParameters = boundingParameters.trim();
			String [] WESN = boundingParameters.split("\\s");
			westTxt.setText(WESN[0]);
			eastTxt.setText(WESN[1]);
			southTxt.setText(WESN[2]);
			northTxt.setText(WESN[3]);
			if ( layerList.getSelectedItem().toString() != null ) {
				currentLayerName = layerList.getSelectedItem().toString();
				StringBuffer temp1 = getLayer();
				StringBuffer[] dataSB = { temp1 };
				sendToDataTables(dataSB);
			}
			return;
		}
		else {
			mapApp.getMap().getVisibleRect();
			Point p = new Point( (int)mapApp.getMap().getVisibleRect().getMinX(), (int)mapApp.getMap().getVisibleRect().getMinY() );
			Point2D.Double p2d1 = (Point2D.Double)mapApp.getMap().getProjection().getRefXY( mapApp.getMap().getScaledPoint(p));
			p = new Point( (int)mapApp.getMap().getVisibleRect().getMaxX(), (int)mapApp.getMap().getVisibleRect().getMaxY() );
			Point2D.Double p2d2 = (Point2D.Double)mapApp.getMap().getProjection().getRefXY( mapApp.getMap().getScaledPoint(p));
			while ( p2d1.x > 180. ) {
				p2d1.x-=360.;
			}
			while ( p2d2.x > 180. ) {
				p2d2.x-=360.;
			}
			p = new Point( (int)mapApp.getMap().getVisibleRect().getWidth()/10, 0 );
			Point2D.Double p2d3 = (Point2D.Double)mapApp.getMap().getProjection().getRefXY( mapApp.getMap().getScaledPoint(p));
			if ( p2d3.x*10 >= 360.0 ) {
				currentViewBbox += "-180.0";
				currentViewBbox += "," + mapApp.getMap().getWESN()[2];
				currentViewBbox += ",180.0";
				currentViewBbox += "," + mapApp.getMap().getWESN()[3];
				StringBuffer temp1 = getLayer();
				StringBuffer[] dataSB = { temp1 };
				sendToDataTables(dataSB);
			}
			else if ( p2d1.x + (p2d3.x*10) > 180 ) {
				currentViewBbox += p2d1.x;
				currentViewBbox += "," + mapApp.getMap().getWESN()[2];
				currentViewBbox += ",180.0";
				currentViewBbox += "," + mapApp.getMap().getWESN()[3];
				if ( layerList.getSelectedItem().toString() != null ) {
					currentLayerName = layerList.getSelectedItem().toString();
				}
				StringBuffer temp1 = getLayer();
				currentViewBbox = "&bbox=";
				currentViewBbox += "-180.0";
				currentViewBbox += "," + mapApp.getMap().getWESN()[2];
				currentViewBbox += "," + p2d2.x;
				currentViewBbox += "," + mapApp.getMap().getWESN()[3];
				currentViewCoordinate = "-180.0" + "," + 
							mapApp.getMap().getWESN()[2] + " "+ p2d2.x +
							"," + mapApp.getMap().getWESN()[3];
				//System.out.println("in temp 2" + currentViewCoordinate);
				StringBuffer temp2 = getLayer();
				StringBuffer[] dataSB = { temp1, temp2 };
				sendToDataTables(dataSB);
			}
			else {
				currentViewBbox += p2d1.x;
				currentViewBbox += "," + mapApp.getMap().getWESN()[2];
				currentViewBbox += "," + p2d2.x;
				currentViewBbox += "," + mapApp.getMap().getWESN()[3];
				currentViewCoordinate = p2d1.x + "," + mapApp.getMap().getWESN()[2]+
							" " + p2d2.x + "," + mapApp.getMap().getWESN()[3];
				//System.out.println("in temp 1" + currentViewCoordinate);
				if ( layerList.getSelectedItem().toString() != null ) {
					currentLayerName = layerList.getSelectedItem().toString();
				}
				StringBuffer temp1 = getLayer();
				StringBuffer[] dataSB = { temp1 };
				sendToDataTables(dataSB);
			}
			currentViewBbox = "&bbox=";
		}
	}

	public void requestLayer(){
			if ( ((String)bboxList.getSelectedItem()).equals("Current view") && (currentWFSBbox == null) && bboxList.isEnabled()) {
				requestCurrentView();
				return;
			}
			if( currentWFSBbox == null && bboxList.isEnabled()) {
				String boundingParameters = (String)bboxes.get(bboxList.getSelectedItem().toString());
				boundingParameters = boundingParameters.trim();
				String [] WESN = boundingParameters.split("\\s");
				westTxt.setText(WESN[0]);
				southTxt.setText(WESN[2]);
				eastTxt.setText(WESN[1]);
				northTxt.setText(WESN[3]);
		}
			if ( layerList.getSelectedItem().toString() != null ) {
				currentLayerName = layerList.getSelectedItem().toString();
				StringBuffer temp1 = getLayer();
				StringBuffer[] dataSB = { temp1 };
				sendToDataTables(dataSB);
			}
		}

	public void sendToDataTables( StringBuffer[] inputSBArr ) {
		if ( inputSBArr != null && inputSBArr.length > 0 ) {
			Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
			for ( int i = 1; i < inputSBArr.length; i++ ) {
				inputSBArr[i] = inputSBArr[i].delete(0,inputSBArr[i].indexOf("\n")+1);
				inputSBArr[0].append(inputSBArr[i]);
			}
			String inputStr = inputSBArr[0].toString();
			if (inputStr.length() == 0) return;
			StringSelection ss = new StringSelection(inputStr);
			c.setContents(ss, ss);
			mapApp.importDataTable( "Import from Clipboard (paste)...", currentLayerName + " - " + currentWFSTitle );
		}
		System.gc();
	}

	public void reset() {
		txt.setText(null);
		disposeButton.setEnabled(false);
		serverList.setSelectedIndex(0);
		connect.setEnabled(true);
		frame.pack();
		System.gc();
	}

	public void dispose() {
		if ( secondPane != null ) {
			contentPane.remove(secondPane);
		}
		if ( thirdPane != null ) {
			contentPane.remove(thirdPane);
		}
		frame.pack();
		secondPane = null;
		thirdPane = null;
		layerList = null;
		layerNames = null;
		layerhasFilter=null;
		filterNames = null;
		filterValues = null;
		capabilitiesURL = null;
		currentLayerName = null;
		currentWFSName = null;
		currentWFSTitle = null;
		currentWFSBbox = null;
		System.gc();
	}

	public void readCapabilities( URL inputCapabilitiesURL ) {
		layerNames = new Vector();
		layerhasFilter = new Vector();
		filterNames = new Vector();
		filterNames.add("None");
		filterValues = new Vector();
		filterValues.add("None");
		anArray = new String[50][50];
		BufferedReader in;
		String s = null;
		boolean isWFS = false;
		boolean isFeatureType = false;
		boolean isKeywordType = false;
		boolean isService = false;
		boolean hasFeatureType = false;
		int nameNum = -1;
		int valueNum =-1;
		try {
			in = new BufferedReader( new InputStreamReader( inputCapabilitiesURL.openStream() ) );
			String temp = "";
			int numberOfLines = 0;
			while ( ( s = in.readLine() ) != null )	{
				temp = s;
				numberOfLines++;
				if ( numberOfLines > 1 ) {
					if ( s.indexOf("WFS_Capabilities") != -1 ) {
						isWFS = true;
					}
					else if ( s.indexOf("<Service>") != -1 ) {
						isService = true;
					}
					else if ( s.indexOf("</Service>") != -1 ) {
						isService = false;
					}
					else if ( s.indexOf("<FeatureType>") != -1 || s.indexOf("<FeatureType ") != -1 ) {
						isFeatureType = true;
					}
					else if ( s.indexOf("</FeatureType>") != -1 ) {
						isFeatureType = false;
					}
					if ( isFeatureType ) {
						if ( s.indexOf("<Name>") != -1 ) {
							hasFeatureType = true;
							int index1 = s.indexOf( "<Name>" ) + 6;
							int index2 = s.indexOf( "</Name>" );
							layerNames.add(s.substring(index1, index2));

								s = in.readLine();
								while(!s.contentEquals("<Keywords>")){
									if(!s.contentEquals("</FeatureType>")){
									s = in.readLine();
									}
									if(s.contains("</FeatureType>")){
										layerhasFilter.add(isKeywordType);
										break;
									}
									if(s.contains("<Keywords>")){
										if ( s.indexOf("<Keywords>") != -1){
											isKeywordType = true;
										}
										if(!s.contains("&lt;filter&gt;") && (s.contentEquals("</Keywords>"))){
											break;
										}else if(!s.contains("</Keywords>")){
											s = in.readLine();
										}
										while(!s.contains("&lt;filter&gt;")){
											s = in.readLine();
										}
										while(s.indexOf("&lt;filter&gt") != -1){
										//Extract Each Filter Name
										if (s.contains("&lt;filter&gt;")) {
											s = in.readLine();
											while ( s.indexOf("&lt;name&gt") != -1 ) {
												nameNum++;
											s = s.replaceAll("&lt;", "");
											s = s.replaceAll("&gt;", "");
											hasFeatureType = true;
											int index3 = 0;
											int index4 = 0;
											index3 = s.indexOf( "name," )+5;
											index4 = s.indexOf( "/name" );
												filterNames.add(s.substring(index3, index4));
											}
												//Looking for value
												while(!s.contains("&lt;value&gt;")){
													s = in.readLine();
												}
												while(s.contains("&lt;value&gt;")){
													valueNum++;
													s = s.replaceAll("&lt;", "");
													s = s.replaceAll("&gt;", "");
													int a = s.indexOf("value")+5;
													int b = s.indexOf("/value");
													filterValues.add(s.substring(a, b));
													
													String nameString = s.substring(a, b).toString();
													anArray[nameNum][valueNum] = nameString;
													 
													//System.out.println("filter value is " + nameString + "anArray[nameNum][valueNum]" + anArray[nameNum][valueNum]);
													s = in.readLine();
												}
												while(s.contains("&lt;/values&gt;")){
													s = in.readLine();
													valueNum = -1;
												}
												if(s.contains("&lt;/filter&gt;")){
													s = in.readLine();
												}
											}
										}
									}
								}
							}
						}
					else if ( isService ) {
						//System.out.println("s.indexOf(NAME)" + s.indexOf("<Name>"));
						if ( s.indexOf("<Name>") != -1 ) {
							int index1 = s.indexOf( "<Name>" ) + 6;
							int index2 = s.indexOf( "</Name>" );
							currentWFSName = s.substring(index1, index2);
							//System.out.println("index1: " + index1 + "index2: " + index2);
						}

						if ( s.indexOf("<Title>") != -1 ) {
							int index1 = s.indexOf( "<Title>" ) + 7;
							int index2 = s.indexOf( "</Title>" );
							currentWFSTitle = s.substring(index1, index2);
						}
					}
				}
				if ( numberOfLines == 1 ) {
					if ( temp != null ) {
						String[] result = temp.split("<");
						for (int i=2; i<result.length; i++) {
							if ( result[i] != null ) {
								result[i] = "<" + result[i];
								result[i] = result[i].trim();
								if ( result[i].indexOf("WFS_Capabilities") != -1 ) {
									isWFS = true;
								}
								else if ( result[i].indexOf("<Service>") != -1 ) {
									isService = true;
								}
								else if ( result[i].indexOf("</Service>") != -1 ) {
									isService = false;
								}
								else if ( result[i].indexOf("<FeatureType>") != -1 || s.indexOf("<FeatureType ") != -1 ) {
									isFeatureType = true;
								}
								else if ( result[i].indexOf("</FeatureType>") != -1 ) {
									isFeatureType = false;
								}
								if ( isFeatureType ) {
									if ( result[i].indexOf("<Name>") != -1 ) {
										hasFeatureType = true;
										int index1 = result[i].indexOf( "<Name>" ) + 6;
										int index2 = result[i].indexOf( "</Name>" );
										if ( index2 != -1 ) {
											layerNames.add(result[i].substring(index1, index2));
										}
										else {
											layerNames.add(result[i].substring(index1));
										}
									}
								}
								else if ( isService ) {
									if ( result[i].indexOf("<Name>") != -1 ) {
										int index1 = result[i].indexOf( "<Name>" ) + 6;
										int index2 = result[i].indexOf( "</Name>" );
										if ( index2 != -1 ) {
											currentWFSName = result[i].substring(index1, index2);
										}
										else {
											currentWFSName = result[i].substring(index1);
										}
									}
									if ( result[i].indexOf("<Title>") != -1 ) {
										int index1 = result[i].indexOf( "<Title>" ) + 7;
										int index2 = result[i].indexOf( "</Title>" );
										if ( index2 != -1 ) {
											currentWFSTitle = result[i].substring(index1, index2);
										}
										else {
											currentWFSTitle = result[i].substring(index1);
										}
									}
								}
							}
						}
					}
				}
			}

			in.close();
			if ( !isWFS ) {
				JOptionPane.showMessageDialog(MapApp.anchor, "WFS service not found", "Invalid WFS URL", JOptionPane.ERROR_MESSAGE);
				reset();
				dispose();
				exits();
				mapApp.invokeWFS();
				return;
			}
			else if ( hasFeatureType ) {
				chooseLayer();
			}
		} catch (IOException e1) {
			JOptionPane.showMessageDialog(MapApp.anchor, "Error reading URL. WFS service not found.", "IOException", JOptionPane.ERROR_MESSAGE);
			reset();
			dispose();
			exits();
			mapApp.invokeWFS();
		}
	}

	public boolean checkBoundingBoxInput() {
		boolean good = true;
		String northBound = northTxt.getText();
		String southBound = southTxt.getText();
		String eastBound = eastTxt.getText();
		String westBound = westTxt.getText();
		try {
			double northCoord = Double.parseDouble(northBound);
			double southCoord = Double.parseDouble(southBound);
			double eastCoord = Double.parseDouble(eastBound);
			double westCoord = Double.parseDouble(westBound);

//			***** GMA 1.6.2: Changed boundary condition checks so all focus site bounding boxes work
//			if ( northCoord < -90. || northCoord > 90. || southCoord < -90. || southCoord > 90. || eastCoord < -180. || eastCoord > 180. || westCoord < -180. || westCoord > 180. || northCoord < southCoord || eastCoord < westCoord ) {
			if ( northCoord < -90. || northCoord > 90. || southCoord < -90. || southCoord > 90. || eastCoord < -180. || westCoord < -180. || westCoord > 180. || northCoord < southCoord || eastCoord < westCoord ) {
//			***** GMA 1.6.2
				good = false;
			}
		} catch (NumberFormatException nfe) {
			JOptionPane.showMessageDialog(MapApp.anchor, "Malformed Bounding Box Number", "Number Format Exception", JOptionPane.ERROR_MESSAGE);
			good = false;
		}
		return good;
	}

	public String searchAndReplaceAndpersanInURL(String input) {
		String output = null;
		if ( input != null && (input.indexOf( "http://" ) != -1  || input.indexOf("https://") != -1)) {
			output = input.replaceAll( "&amp;", "&" );
			output = output.replaceAll( "&lt;", "<");
			output = output.replaceAll( "&gt;", ">");
			output = output.replaceAll( "&apos;", "'");
			output = output.replaceAll( "&quot;", "\"");
			return output;
		}
		else {
			return input;
		}
	}

	public void processSize(int nameNum, int valueNum) {
		// Take out none reference
		nameNum = nameNum -1;
		valueNum = valueNum -1;
	}

	private void exits() {
		frame.dispose();
	}

	public void actionPerformed(ActionEvent e) {
		if ( e.getSource() == connect || e.getSource() == txt ) {
			connect.setEnabled(false);
			disposeButton.setEnabled(true);

			dispose();
			try {
				capabilitiesURL = URLFactory.url(txt.getText());
			} catch (MalformedURLException e1) {
				JOptionPane.showMessageDialog(MapApp.anchor, "Invalid URL", "MalformedURLException", JOptionPane.ERROR_MESSAGE);
			} catch (NullPointerException e2) {
				JOptionPane.showMessageDialog(MapApp.anchor, "Empty URL", "No URL Given", JOptionPane.ERROR_MESSAGE);
			}
			readCapabilities(capabilitiesURL);
		}
		else if ( e.getSource() == serverList ) {
//			txt.setText(serverList.getSelectedItem().toString());
			txt.setText((String)newServerNames.get(serverList.getSelectedItem().toString()));
		}
		//Actions on Feature List
		else if (e.getSource() == layerList) {
			//When the feature has a filter listed on filterList

		try {
				//layer has filter types is true show filters
				if(layerhasFilter.get(layerList.getSelectedIndex()).equals(true)){
					filterList = new JComboBox(filterNames);
					filterList.addActionListener(this);
					//filterValueList = new JComboBox(filterValues);
					filterValueList.setVisible(false);
					filterPane.removeAll();
					filterPane.add(filterList,BorderLayout.NORTH);
					frame.pack();
					filterPane.updateUI();
					secondPane.updateUI();
				}
				//layer has filter types is false gray out the filter list
				else if(layerhasFilter.get(layerList.getSelectedIndex()).equals(false)){
					filterList = new JComboBox(filterNamesDefault);
					filterList.setEnabled(false);
					filterPane.removeAll();
					filterPane.add(filterList,BorderLayout.NORTH);
					filterPane.updateUI();
					secondPane.updateUI();
					frame.pack();
				}
		} catch (ArrayIndexOutOfBoundsException ae) {
			//layer has filter types is false gray out the filter list
			filterList = new JComboBox(filterNamesDefault);
			filterList.setEnabled(false);
			filterPane.removeAll();
			filterPane.add(filterList,BorderLayout.NORTH);
			filterPane.updateUI();
			secondPane.updateUI();
			frame.pack();
		}
//			if ( layerList.getSelectedItem().toString() != null ) {
//				currentLayerName = layerList.getSelectedItem().toString();
//				getLayer();
//			}
		}
		else if (e.getSource() == filterList) {
			filterPane.remove(filterValueList);
			Vector<String> pickerValues = new Vector<String>();
			// If the users still chooses None do nothing
			if(filterList.getSelectedItem().toString().contains("None")){
				filterValueList.setVisible(false);
				filterPane.updateUI();
				secondPane.updateUI();
				frame.pack();
			}
			//System.out.println(filterList.getSelectedItem().toString()+ filterList.getSelectedIndex());
			else{
				int pickerType = filterList.getSelectedIndex()-1;

				for(int i=0; i < anArray.length; i++){
					if(anArray[pickerType][i]!=null){
					pickerValues.add(anArray[pickerType][i]);
					}else{
						break;
					}
				}
				filterValueList = new JComboBox(pickerValues);
				Border emptyBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
				Border lineBorder = BorderFactory.createLineBorder( Color.black );
				Border vBorder = BorderFactory.createTitledBorder( lineBorder, filterValueSelectLabel );
				Border valueFilterBorder = BorderFactory.createCompoundBorder( vBorder, emptyBorder );

				filterValueList.setBorder(valueFilterBorder);
				filterPane.add(filterValueList,BorderLayout.SOUTH);
				filterPane.updateUI();
				filterValueList.updateUI();
				frame.pack();
			}

		}else if(e.getSource() == filterValueList){
		}
		else if ( e.getSource() == bboxList ) {
//			String boundingParameters = (String)bboxes.get(bboxList.getSelectedItem().toString());
//			boundingParameters = boundingParameters.trim();
//			String [] WESN = boundingParameters.split("\\s");
//			westTxt.setText(WESN[0]);
//			southTxt.setText(WESN[2]);
//			eastTxt.setText(WESN[1]);
//			northTxt.setText(WESN[3]);
//			if ( layerList.getSelectedItem().toString() != null ) {
//				currentLayerName = layerList.getSelectedItem().toString();
//				getLayer();
//			}
		}
		else if ( e.getSource() == loadLayer ) {		// Load Entire Feature
			if ( bboxList.getSelectedItem() != null ){
				currentFilterName = filterList.getSelectedItem().toString();
				currentFilterValue = filterValueList.getSelectedItem().toString();

				if ((currentFilterName=="None")) {
					currentFilterName = null;
				}

				if ((currentFilterValue =="None")) {
					currentFilterValue=null;
				}
				if((currentFilterName!=null && currentFilterValue==null)||
						(currentFilterName==null && filterValueList.isVisible() && currentFilterValue !=null)){
					JOptionPane.showMessageDialog(frame, "A Filter Type and Value must be selected to show filtered results."+
													'\n' +"To show all results without filtering select None for type and value.");
				} else {
					mapApp.addProcessingTask("Loading WFS...",
						new Runnable() {
							public void run() {
								requestLayer();
							}
						});
				}
			}
		}
		else if ( e.getSource().equals(loadCurrentViewB) ) {		// Load for Current View
			mapApp.addProcessingTask("Loading WFS...",
					new Runnable() {
						public void run() {
							requestCurrentView();
						}
					});
		}
		else if ( e.getSource() == disposeButton ) {
			reset();
			dispose();
			exits();
			mapApp.invokeWFS();
		}
	}
}

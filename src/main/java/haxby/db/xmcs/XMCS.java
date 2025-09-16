package haxby.db.xmcs;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;

import haxby.db.Database;
import haxby.dig.Digitizer;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

/***
 * XMCS loads a list of XMCruises available from MCS/expedition_list
 *
 * MCS/expedition_list should be in the format of
 * 	cruiseID	mapType	West	East	South	North
 * where mapType is a map selection
 * 	1 == MERCATOR
 * 	2 == SOUTH_POLAR
 * 	3 == MERCATOR && SOUTH_POLAR
 *  4 == NORTH POLAR
 *  5 == MERCATOR && NORTH_POLAR
 *  7 == MERCATOR && SOUTH POLAR && NORTH POLAR
 * and West East South North defines the cruise bounding box
 *
 * All directories relative to MapApp.BASE_URL
 * 	as of 6/07 /home/geomapapp/apache/htdocs/
 *
 * To enter a new cruise into the database follow these steps
 * 	(1) Obtain:
 * 		-	Nav Files for each individual line
 * 		-	Segy Files for the cruise
 * 	(2) Make:
 * 		-	The directory MCS/cruiseID/
 * 		-	The subdirectories 
 * 				MCS/cruiseID/nav/
 * 				MCS/cruiseID/img/
 * 				MCS/cruiseID/segy/
 * 	(3) Move:
 * 		-	The Segy files into the segy directoy
 * 		-	The Nav files into the nav directory
 *
 * 	(4)	Rename:
 * 		-	The nav files to the format cruiseID-lineID.nav
 * 		-	The segy files to the format cruiseID-lineID.segy
 * 	(5)	Process:
 * 		-	Turn each segy file into a sun raster image with the command
 * 				java haxby.db.xmcs.XMChirp segyFile
 * 			where segyFile is the segy you are processing
 * 			!!SAVE THE OUTPUT FROM THIS COMMAND!!
 * 			Save output either manually or with redirection to a file
 *
 * 		-	The Delay Minimum and Total Durration from this proccess are
 * 				needed for the bounds file (Step 6)
 * 	(6)	Process:
 * 		- 	Create a bounds file in this format:
 * 				cruiseID	lineID	cdpMin	cdpMax	delayMin	timeDuration
 * 			with a line entry for each line
 * 				and each field is tab seperated
 * 			Obtain the cdpMin and cdpMax from the nav files
 * 			Obtain the delayMin and timeDuration from the XMChirp output for
 * 				the coresponding segy file
 * 		-	Move the "bounds" file to the nav/ directory
 *
 * 	(7)	Convert:
 * 		-	GZip each ras file into a r2.gz with the command
 * 				java haxby.image.RasToRas2 rasFile .4
 * 	(8)	Move:
 * 		-	The resulting r2.gz files into the img/ directory
 * 	(9)	Create:
 * 		-	The mcs_control file with the command
 * 				java haxby.db.xmcs.XMControl cruiseID navDirectory
 * 			!!SAVE THE OUTPUT FROM THIS COMMAND!! 
 *			The WESN bounding box outputed is needed for step 13
 *
 * 			Where navDirectory is the directory with all the nav files
 * 				and cruiseID is the cruiseID
 * 			And the nav files are in the format
 * 				cruiseID	lineID	shotPoint	Lat	Lon
 * 			and the fields are tab seperated 
 * 	(10) Move:
 * 		-	The resulting mcs_control file into the nav/ directory
 * 	(11) Create:
 * 		**NOTE** Before this command is run ALL PRECEDING STEPS must have been
 * 					completed succesfully
 * 		-	The full size jpg images with the command
 * 				java -Xmx256m haxby.db.xmcs.XMRas2ToJPG cruiseID
 * 			where cruiseID is the cruise id
 * 	(12) Move:
 * 		-	The resulting jpg files into the img/ directory
 * 	(13) Add:
 * 		-	An entry to the MCS/expedition_list in the format
 * 				cruiseID	mapType	West	East	South	North
 * 			where mapType is a map selection
 * 				1 == MERCATOR
 * 				2 == SOUTH_POLAR
 * 				3 == MERCATOR && SOUTH_POLAR
 *  			5 == MERCATOR && NORTH_POLAR
 *  			7 == MERCATOR && SOUTH POLAR && NORTH POLAR
 * 			and West East South North defines the cruise bounding box
 * 			obtained from the XMControl command (step 9)
 */
public class XMCS implements ActionListener, 
							MouseListener, 
							MouseMotionListener,
							Database {
	public static final int MERCATOR_MAP = 1;
	public static final int SOUTH_POLAR_MAP = 2;
	public static final int NORTH_POLAR_MAP = 4;
	public static JRadioButton[] mcsDataSelect;
	protected static XMCruise[] cruises;
	protected static boolean initiallized = false;
	protected XMap map = null;
	protected JPanel panel = null;
	protected JLabel label1;
	protected JComboBox cruiseList = null;
	protected JComboBox lineList = null;
	protected XMCruise currentCruise = null;
	protected static XMLine currentLine = null;
	protected boolean mouseE = false;
	protected boolean enabled = false;
	
	protected JDialog dialogProgress;
	protected JProgressBar pb;
	protected JPanel progressPanel;
	protected JLabel progressLabel;
	private boolean clickEvent = false;
	public boolean linesLoaded = false;
	
	protected static final Color SELECTED_CRUISE_COLOR = new Color(0, 0xFF, 0);

	JMenu[] menus;
	XMImage image = null;
	XMImage imageAlt = null;
	String mapSelect = "selectLine";
	JSplitPane imagePane;
	JRadioButton orientH, orientV;
	JButton load;
	// LDEO Multi Channel Seismic
	static String MULTI_CHANNEL_PATH = PathUtil.getPath("PORTALS/MULTI_CHANNEL_PATH",
			MapApp.BASE_URL+"/data/portals/mcs/");
	static String MULTI_CHANNEL_EXP_LIST = PathUtil.getPath("PORTALS/MULTI_CHANNEL_EXP_LIST",
			MapApp.BASE_URL+"/data/portals/mcs/expedition_list");
	//USGS Multi Channel Seismic
	static String USGS_MULTI_CHANNEL_PATH = PathUtil.getPath("PORTALS/USGS_MULTI_CHANNEL_PATH",
								"https://cmgds.marine.usgs.gov/gma/USGS_MCS/");
	static String USGS_MULTI_CHANNEL_EXP_LIST = PathUtil.getPath("PORTALS/USGS_MULTI_CHANNEL_EXP_LIST",
								"https://cmgds.marine.usgs.gov/gma/USGS_MCS/expedition_list_USGS_MCS");
	//USGS Single Channel Seismic
	static String USGS_SINGLE_CHANNEL_PATH = PathUtil.getPath("PORTALS/USGS_SINGLE_CHANNEL_PATH",
								"https://cmgds.marine.usgs.gov/gma/USGS_SCS/");
	static String USGS_SINGLE_CHANNEL_EXP_LIST = PathUtil.getPath("PORTALS/USGS_SINGLE_CHANNEL_EXP_LIST",
								"https://cmgds.marine.usgs.gov/gma/USGS_SCS/expedition_list_USGS_SCS");

	//TODO
	static String ANTARCTIC_SDLS_PATH = PathUtil.getPath("PORTALS/ANTARCTIC_SDLS",
			MapApp.BASE_URL+"/data/portals/mcs/sdls/");

	static String ANTARCTIC_SDLS_EXP_LIST = PathUtil.getPath("PORTALS/ANTARCTIC_SDLS_EXP_LIST",
			MapApp.BASE_URL+"/data/portals/mcs/sdls/expedition_list");

	Digitizer dig1, dig2;

	public XMCS( XMap map ) {
		this.map = map;
		image = createXMImage(dig1);
		imageAlt = createXMImage(dig2);
		image.setOtherImage( imageAlt );
		imageAlt.setOtherImage( image );

		// GMA 1.6.2: Changed the default split of the view area to make the view windows more visible
		imagePane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT,
				image.panel, imageAlt.panel );

		imagePane.setOneTouchExpandable( true );
		imagePane.setDividerLocation(imagePane.getMaximumDividerLocation() + 150);
	}

	protected XMImage createXMImage(Digitizer dig) {
		return new XMImage(dig);
	}

	public void setEnabled( boolean tf ) {
		if( tf && enabled) return;
		enabled = tf;
		if(enabled) {
			map.addMouseListener( this);
		} else {
			map.removeMouseListener( this);
		}
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void disposeDB() {
		image.disposeImage();
		imageAlt.disposeImage();
		imagePane.setLeftComponent( image.panel );
		imagePane.setRightComponent( imageAlt.panel );
		image.line = null;
		imageAlt.line = null;
		currentCruise = null;
		currentLine = null;
		panel = null;
		cruises = null;
		unloadDB();
	}
	public boolean loadDB() {
//		MapApp app = (MapApp)map.getApp();
//		app.setFrameSize( 1000, 1000 );

		if( initiallized ) return true;
		try {
			initRadar(map, this);
		} catch(IOException ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	public boolean isLoaded() {
		return initiallized;
	}
	public void unloadDB() {
		initiallized = false;
	}
	public String getDBName() {
		return "Digital Seismic Reflection Profiles (MCS & SCS)";
	}
	public String getCommand() {
		return "mcs_cmd";
	}
	public String getDescription() {
		return getDBName();
	}
	public JComponent getDataDisplay() {
		return imagePane;
	}
	public JComponent getSelectionDialog() {
		if( !initiallized )return null;
		if(panel!=null) {
			return panel;
		}

		JPanel panel1 = new JPanel(new GridLayout(0,1));

		mcsDataSelect = new JRadioButton[4];
		mcsDataSelect[0] = new JRadioButton("LDEO & UTIG MCS");
		mcsDataSelect[0].setToolTipText("Academic Seismic Data");
		mcsDataSelect[1] = new JRadioButton("USGS MCS");
		mcsDataSelect[1].setToolTipText("USGS Multi Channel Seismic Data");
		mcsDataSelect[2] = new JRadioButton("USGS SCS");
		mcsDataSelect[2].setToolTipText("Single Channel Seismic Data");
		mcsDataSelect[3] = new JRadioButton("Antarctic - SDLS");
		mcsDataSelect[3].setToolTipText("Antarctic - SDLS");
		ButtonGroup bGroupMCS = new ButtonGroup();

		// Check that the expedition list are there with 200 status or will gray out.
		try {
			URL exp1 = URLFactory.url(USGS_MULTI_CHANNEL_EXP_LIST);
			HttpURLConnection con1 = (HttpURLConnection) exp1.openConnection();
			con1.setRequestMethod("HEAD");
			URL exp2 = URLFactory.url(USGS_SINGLE_CHANNEL_EXP_LIST);
			HttpURLConnection con2 = (HttpURLConnection) exp2.openConnection();
			//TODO
			//URL exp3 = URLFactory.url(ANTARCTIC_SDLS_EXP_LIST);
			//HttpURLConnection con3 = (HttpURLConnection) exp3.openConnection();
			con2.setRequestMethod("HEAD");
			//con3.setRequestMethod("HEAD");
				if(con1.getResponseCode() != HttpURLConnection.HTTP_OK){
					mcsDataSelect[1].setEnabled(false);
				}
				if(con2.getResponseCode() != HttpURLConnection.HTTP_OK){
					mcsDataSelect[2].setEnabled(false);
				}
		/*		if(con3.getResponseCode() != HttpURLConnection.HTTP_OK){
					mcsDataSelect[3].setEnabled(false);
				}*/
		} catch (Exception e) {
		}
		
		// check if each list contains cruises for the current projection
		mcsDataSelect[0].setEnabled(checkListForProjection(MULTI_CHANNEL_EXP_LIST));
		mcsDataSelect[1].setEnabled(checkListForProjection(USGS_MULTI_CHANNEL_EXP_LIST));
		mcsDataSelect[2].setEnabled(checkListForProjection(USGS_SINGLE_CHANNEL_EXP_LIST));
		mcsDataSelect[3].setEnabled(checkListForProjection(ANTARCTIC_SDLS_EXP_LIST));
		
		// If GMA is at sea all gray out
		if(MapApp.AT_SEA == true) {
			mcsDataSelect[1].setEnabled(false);
			mcsDataSelect[2].setEnabled(false);
			mcsDataSelect[3].setEnabled(false);
		}

		for (int i = 0; i < mcsDataSelect.length; i++) {
			 bGroupMCS.add(mcsDataSelect[i]);
			mcsDataSelect[i].addActionListener(this);
			panel1.add(mcsDataSelect[i]);
		}
		mcsDataSelect[0].setSelected(true);

		label1 = new JLabel("Cruise");
		cruiseList = new JComboBox();
		cruiseList.addItem("- Select -");
		for(int i=0 ; i<cruises.length ; i++) {
			cruiseList.addItem(cruises[i]);
		}

		JButton btn;
		Box box = Box.createVerticalBox();

		panel1.add( label1);
		panel1.add( cruiseList );

		lineList = new JComboBox();
		lineList.addItem("- Select Line -");
		lineList.setVisible(false);
		panel1.add(lineList);

		btn = new JButton("Load View 1");
		btn.setActionCommand("view-1");
		btn.addActionListener(this);
		panel1.add(btn);
		btn = new JButton("Load View 2");
		btn.setActionCommand("view-2");
		btn.addActionListener(this);
		panel1.add(btn);
		ButtonGroup gp1 = new ButtonGroup();

		// GMA 1.6.2: Changed the default split of the view area to make the view windows more visible
		orientH = new JRadioButton( "Horizontal", true );
		orientH.addActionListener(this);
		gp1.add(orientH);
		panel1.add( orientH );

		// GMA 1.6.2: Changed the default split of the view area to make the view windows more visible
		orientV = new JRadioButton( "Vertical", false );
		orientV.addActionListener(this);
		gp1.add(orientV);
		panel1.add( orientV );

		box.add(panel1);

		cruiseList.addActionListener(this);
		lineList.addActionListener(this);
		lineList.setRenderer( new XMLineRenderer() );

		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(box);
		panel.setPreferredSize(new Dimension(170, panel.getY())); //Set Size
		return panel;
	}

	/*
	 * check the cruise list to see if it contains cruises for the current 
	 * map projection
	 */
	private boolean checkListForProjection(String listPath) {
		URL url;
		try {
			url = URLFactory.url(listPath);
			BufferedReader in = new BufferedReader( new InputStreamReader(url.openStream()));
	
			String inStr;
			while ((inStr = in.readLine()) != null) {
				String[] split = inStr.split("\t");
				if (split.length != 6) continue; // improper entry
	
				int mapType = MapApp.MERCATOR_MAP;
				if (map.getApp() instanceof MapApp)
					mapType =((MapApp) map.getApp()).getMapType();
	
				try {
					int cruiseType = Integer.parseInt(split[1]);
					switch (mapType) {
						case MapApp.MERCATOR_MAP:
							if ((cruiseType & MERCATOR_MAP) != 0) return true;
							break;
						case MapApp.SOUTH_POLAR_MAP:
							if ((cruiseType & SOUTH_POLAR_MAP) != 0) return true;
							break;
						case MapApp.NORTH_POLAR_MAP:
							if ((cruiseType & NORTH_POLAR_MAP) != 0) return true;
							break;
					}
	
				} catch (NumberFormatException ex) {
					return false;
				}
			}
		} catch (Exception e) {
			return false;
		}	
		return false;
	}
	
	public void draw(Graphics2D g) {
		if(map==null || cruises.length==0)return;
		int k, k0;
		if(currentCruise==null) {
			k=0;
			k0 = cruises.length-1;
		} else {
			k = cruiseList.getSelectedIndex();
			k0 = k-1;
			k = k%cruises.length;
		}
		g.setStroke(new BasicStroke(2f/(float)map.getZoom()));
		g.setColor(Color.black);
		String path = MULTI_CHANNEL_PATH;
		if (mcsDataSelect[1].isSelected()) {
			path = USGS_MULTI_CHANNEL_PATH;
		} else if (mcsDataSelect[2].isSelected()) {
			path = USGS_SINGLE_CHANNEL_PATH;
		} else if(mcsDataSelect[3].isSelected()){
			path = ANTARCTIC_SDLS_PATH;
		}
		dialogProgress = new JDialog((Frame)null, "Loading Cruises");
		progressPanel = new JPanel(new BorderLayout());
		progressPanel.setBorder(BorderFactory.createEmptyBorder(5, 1, 5, 1));
		dialogProgress.setLocationRelativeTo(map);
		pb = new JProgressBar(1,cruises.length);
		progressLabel = new JLabel("Loading lines for " + cruises.length + " cruises");
		progressPanel.add(progressLabel, BorderLayout.NORTH);
		progressPanel.add(pb);
		dialogProgress.setContentPane(progressPanel);
		dialogProgress.setPreferredSize(new Dimension(380,60));
		dialogProgress.pack();
		dialogProgress.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialogProgress.setAlwaysOnTop(true);
	//	while(k!=k0) {
		for(k=0 ; k<cruises.length ; k++) {
			pb.setValue(k+1);
			pb.repaint();
			dialogProgress.setTitle("Loading Cruise #" + (k+1) + " of " + cruises.length);
			dialogProgress.pack();
			if(!linesLoaded && !cruises[k].isLoaded()) {
				dialogProgress.setVisible(true);
				//System.out.println("Loading lines for cruise #" + (k+1) + " of " + cruises.length);
				try {
					cruises[k].loadLines(path);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		// Draw the selected cruise to white
			if (currentCruise == cruises[k]) {
				g.setColor(SELECTED_CRUISE_COLOR);
			} else {
				g.setColor(Color.black);
			}
			//cruises[k].draw(g);
			cruises[k].drawLines(g);
		}
		linesLoaded = true;
		dialogProgress.setVisible(false);

		if(currentCruise!=null) {
			g.setColor(SELECTED_CRUISE_COLOR);
			currentCruise.drawLines(g);
			g.setColor(Color.black);
			if(currentLine!=null) {
				if( currentLine.getCruise() != currentCruise ) {
					currentLine=null;
				} else if( currentLine==image.line ){
//					***** GMA 1.6.6: On Macs, make the lines thicker so that alternate display method can be used

					String osName = System.getProperty("os.name");
					if ( osName.startsWith("Mac OS") ) {
						g.setStroke(new BasicStroke(5f/(float)map.getZoom()));
					}
//					***** GMA 1.6.6
					g.setColor(Color.yellow);
					image.line.draw(g);
				} else if( currentLine==imageAlt.line) {

//					***** GMA 1.6.6: On Macs, make the lines thicker so that alternate display method can be used
					String osName = System.getProperty("os.name");
					if ( osName.startsWith("Mac OS") ) {
						g.setStroke(new BasicStroke(5f/(float)map.getZoom()));
					}
//					***** GMA 1.6.6

					g.setColor(Color.yellow);
					imageAlt.line.draw(g);
				} else {
					g.setColor(Color.white);
					currentLine.draw(g);
				}
			}
		}
		if(image != null && image.line != null) {
			int[] cdp = image.getVisibleSeg();
			image.line.drawSeg( cdp[0], cdp[1], g);
		}
		if(imageAlt != null && imageAlt.line != null) {
			int[] cdp = imageAlt.getVisibleSeg();
			imageAlt.line.drawSeg( cdp[0], cdp[1], g);
		}
	}
	protected void setSelectedCruise(XMCruise cruise) {
		setSelectedCruise(cruise, null);
	}
	protected void setSelectedCruise(XMCruise cruise, String path) {
		if( map==null || !map.hasOverlay(this) )return;
		if(cruise == currentCruise)return;
		currentLine = null;
		lineList.removeAllItems();
		lineList.addItem("- Select Line -");
		/*if (currentCruise != null)
			currentCruise.clearLines();*/
		currentCruise = cruise;
		map.repaint();

		if(cruise==null)return;
		if(path==null) {
			if(mcsDataSelect[0].isSelected())
			path = MULTI_CHANNEL_PATH;
		} else if (mcsDataSelect[1].isSelected()) {
			path = USGS_MULTI_CHANNEL_PATH;
		} else if (mcsDataSelect[2].isSelected()) {
			path = USGS_SINGLE_CHANNEL_PATH;
		}else if(mcsDataSelect[3].isSelected()){
			path = ANTARCTIC_SDLS_PATH;
			//TODO
		}

		// Load lines each time the cruise is selected
		XMLine[] lines = currentCruise.getLines();
		if (lines.length == 0) 
			try {
				lines = currentCruise.loadLines(path);
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(map, ex.getMessage(), "Could Not Load Cruise Lines", JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		for( int i=0 ; i<lines.length ; i++) {
			lineList.addItem(lines[i]);
		}
		//zoomToCruise();
	}
	protected void zoomToCruise() {
			// zoom history set first zoom checks it for the first time only, Zoom to Cruise Box, tracks zoom after
			map.setZoomHistoryPast(map);
			map.zoomToRect(currentCruise.getBounds());
			map.setZoomHistoryNext(map);
	}

	protected void setSelectedLine(XMLine line) {
		if( map==null || !map.hasOverlay(this) )return;
		if(line == currentLine) return;
		XMCruise cruise=null;
		try {
			cruise = (XMCruise) cruiseList.getSelectedItem();
		} catch ( ClassCastException e ) {
			if(null == currentCruise) return;
			cruise = currentCruise;
		}
		synchronized (map.getTreeLock()) {
			Graphics2D g = map.getGraphics2D();
			currentLine = line;
			draw(g);
		}
	}

	public void actionPerformed( ActionEvent e ) {
		String cmd = e.getActionCommand();
		// Toggle of radio buttons will obtain different expedition lists
		if( e.getSource() instanceof JRadioButton ) {
			selectDataSource(e.getSource());
			linesLoaded = false;
			map.repaint();
			
		}

		if(e.getSource() == load) {
			JFileChooser chooser = new JFileChooser();
			chooser.showOpenDialog(null);

			startLoadImage(image, currentLine);
			return;
			
		}

		if( e.getSource() == cruiseList ) {
			try {
				if(mcsDataSelect[1].isSelected()) {
					setSelectedCruise((XMCruise) cruiseList.getSelectedItem(), USGS_MULTI_CHANNEL_PATH );
					lineList.setVisible(true);
				} else if(mcsDataSelect[2].isSelected()) {
					setSelectedCruise((XMCruise) cruiseList.getSelectedItem(), USGS_SINGLE_CHANNEL_PATH );
					lineList.setVisible(true);
				}else if(mcsDataSelect[3].isSelected()){ 
					//TODO
					setSelectedCruise((XMCruise) cruiseList.getSelectedItem(), ANTARCTIC_SDLS_PATH);
					lineList.setVisible(true);
				}else {
					setSelectedCruise((XMCruise) cruiseList.getSelectedItem());
					lineList.setVisible(true);
				}
				if(!clickEvent && null != currentCruise) {
					zoomToCruise();
				}
			} catch ( ClassCastException ex ) {
				setSelectedCruise( null );
			} finally {
				mouseE = false;
			}
		} else if( e.getSource() == lineList ) {
			try {
				setSelectedLine((XMLine) lineList.getSelectedItem());
			} catch ( ClassCastException ex ) {
				setSelectedLine( null );
			}
		} else if( cmd.equals("view-1") ) {
			image.dig.reset();
			startLoadImage(image, currentLine);

		} else if( cmd.equals("view-2") ) {
			imageAlt.dig.reset();
			startLoadImage(imageAlt, currentLine);
		} else if( e.getSource() == orientV || e.getSource() == orientH ) {
			Dimension dim = imagePane.getSize();
			if( orientV.isSelected() ) {
				imagePane.setOrientation(JSplitPane.VERTICAL_SPLIT);
				imagePane.setDividerLocation( dim.height/2 );
			} else {
				imagePane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
				imagePane.setDividerLocation( dim.width/2 );
			}
		} else if( cmd.startsWith("select") ) {
			mapSelect = "selectLine";
		} else if( cmd.equals("close") ) {
		//	close();
		} else if( cmd.equals("quit")) {
		}
	}
	protected void selectDataSource(Object source) {
		if(source==mcsDataSelect[0]) {
			try {
				initRadar( map, this, MULTI_CHANNEL_EXP_LIST);
				cruiseList.removeAllItems();
				cruiseList.addItem("- Select -");
				for(int i=0 ; i<cruises.length ; i++) {
					cruiseList.addItem(cruises[i]);
				}
				cruiseList.updateUI();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			lineList.setVisible(false);
		} else if(source==mcsDataSelect[1]) {
			try {
				initRadar( map, this,USGS_MULTI_CHANNEL_EXP_LIST);
				cruiseList.removeAllItems();
				cruiseList.addItem("- Select -");
				for(int i=0 ; i<cruises.length ; i++) {
					cruiseList.addItem(cruises[i]);
				}
				cruiseList.updateUI();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			lineList.setVisible(false);
		} else if(source==mcsDataSelect[2]) {
			try {
				initRadar( map, this,USGS_SINGLE_CHANNEL_EXP_LIST);
				cruiseList.removeAllItems();
				cruiseList.addItem("- Select -");
				for(int i=0 ; i<cruises.length ; i++) {
					cruiseList.addItem(cruises[i]);
				}
				cruiseList.updateUI();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			lineList.setVisible(false);
		} else if(source==mcsDataSelect[3]) {
			try{
				initRadar(map, this, ANTARCTIC_SDLS_EXP_LIST);//TODO
				cruiseList.removeAllItems();
				cruiseList.addItem("- Select -");
				for(int i=0;i<cruises.length; i++){
					cruiseList.addItem(cruises[i]);
				}
				cruiseList.updateUI();
			} catch (IOException e1){
				e1.printStackTrace();
			}
			lineList.setVisible(false);
		}
	}

	private void startLoadImage(final XMImage imageDes,final XMLine line) {
		if( line==null || imageDes.saving ) {
			return;
		}

		Runnable r = new Runnable() {
			public void run() {
				loadImage(imageDes, line);
			}
		};
		if (map.getApp() instanceof MapApp)
			((MapApp)map.getApp()).
				addProcessingTask("Loading MCS Image: " + line.lineID, r);
		else
			new Thread(r).start();
	}

	private void loadImage(XMImage imageDest, XMLine line) {
		try {
			imageDest.loadImage(line);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(map.getTopLevelAncestor(), "Unable to load image. "
					+ ex.getMessage());
			imageDest.line = null;
		}
		synchronized( map.getTreeLock() ) {
			Graphics2D g = map.getGraphics2D();
			draw( g );
		}
	}

	public void mouseClicked(MouseEvent e) {
		clickEvent = true;
		if(e.isControlDown())return;
		if(e.getSource() != map) return;
		if(e.getButton()==e.BUTTON3){
			currentCruise=null;
		}

		double scale = map.getZoom();
		double wrap = map.getWrap();
		Point2D.Double p = (Point2D.Double) map.getScaledPoint( e.getPoint() );
		if(wrap > 0.) {
			p.x = p.x % wrap;
		}
		
		//find and select cruise closest to p
		if(cruises.length > 0) {
			int cruiseIndex = 0, lineIndex = 0;
			while(cruiseIndex < cruises.length && cruises[cruiseIndex].lines.isEmpty()) {
				cruiseIndex++;
			}
			if(cruiseIndex < cruises.length) { 
				double shortestDistSq = cruises[cruiseIndex].lines.get(0).distanceSq(p.x, p.y);
				for(int i = 1; i < cruises[cruiseIndex].lines.size(); i++) {
					double distSq = cruises[cruiseIndex].lines.get(i).distanceSq(p.x, p.y);
					if(distSq < shortestDistSq) {
						shortestDistSq = distSq;
						lineIndex = i;
					}
				}
				for(int i = cruiseIndex+1; i < cruises.length; i++) {
					for(int j = 0; j < cruises[i].lines.size(); j++) {
						double distSq = cruises[i].lines.get(j).distanceSq(p.x, p.y);
						if(distSq < shortestDistSq) {
							shortestDistSq = distSq;
							cruiseIndex = i;
							lineIndex = j;
						}
					}
				}
				if(shortestDistSq <= 15/scale) { //TODO this threshold needs trial and error to be determined
					//setSelectedCruise(cruises[cruiseIndex]);
					cruiseList.setSelectedItem(cruises[cruiseIndex]);
					//setSelectedLine(cruises[cruiseIndex].lines.get(lineIndex));
					lineList.setSelectedItem(currentCruise.lines.get(lineIndex));
				}
				else if(null != currentLine) {
					currentLine = null;
					lineList.setSelectedItem("- Select Line -");
				}
				map.repaint();
			}
			else {
				cruiseList.setSelectedItem("- Select -");
				lineList.setSelectedItem("- Select Line -");
			}
		}
		else {
			cruiseList.setSelectedItem("- Select -");
			lineList.setSelectedItem("- Select Line -");
		}

		/*if (currentCruise != null && currentCruise.contains(p.x, p.y, wrap)) {
			// Select Line
			XMLine[] lines = currentCruise.getLines();
			if(lines==null || lines.length==0) return;
			int k = -1;
			if( currentLine!=null) for( k=0 ; k<lines.length ; k++) {
				if( lines[k]==currentLine ) break;
			}
			k=(k+1)%lines.length;
			int k0 = k;
			double minDist = 4./scale/scale;
			k0 = -1;

			while( wrap>0. && p.x>wrap ) p.x -= wrap;
			for( int kk=0 ; kk<lines.length ; kk++) {
				double dist1 = lines[k].distanceSq(p.x, p.y);
				if(dist1 < minDist) {
					minDist = dist1;
					k0 = k;
					break;
				}
				k = (k+1)%lines.length;
			}
			if(k0!=-1) lineList.setSelectedItem(lines[k0]);
			else lineList.setSelectedIndex(0);
		} else {
			// Select Area
			int k = cruiseList.getSelectedIndex();
			k=k%cruises.length;
			int k0 = k;
			if(k==-1){
				System.out.println("zero");
				cruiseList.setSelectedIndex(0);
				return;
			}

			ArrayList<XMCruise> containingCruises = new ArrayList<XMCruise>();
			while(true) {

				if(cruises[k].contains(p.x, p.y, wrap) ) {
					mouseE = true;
					containingCruises.add(cruises[k]);
					//cruiseList.setSelectedItem(cruises[k]);
					//return;
				}
				k = (k+1)%cruises.length;
				if(k==k0)break;
			}
			XMCruise retCruise = null;
			double minDist = Double.MAX_VALUE;

			if(containingCruises.size()==0){
				cruiseList.setSelectedIndex(0);
				return;
			}

			for(XMCruise c:containingCruises){
				double dist = Math.sqrt(Math.pow(c.getBounds().getCenterX()-p.x, 2)+Math.pow(c.getBounds().getCenterY()-p.y, 2));
				if(dist<minDist)
				{
					minDist = dist;
					retCruise = c;
				}
			}
			cruiseList.setSelectedItem(retCruise);
			//cruiseList.setSelectedIndex(0);
		}*/
		clickEvent = false;
	}
	public void mousePressed(MouseEvent e) {
	}
	public void mouseReleased(MouseEvent e) {
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
	}
	public void mouseMoved(MouseEvent e) {
	}
	public void mouseDragged(MouseEvent e) {
	}

	/**
	 * Loads the list of XMCruises found at
	 * MCS/expedition_list or external lists
	 */
	protected static void initRadar(XMap map, XMCS mcs) throws IOException {
		initRadar(map, mcs, null);
	}
	protected static void initRadar(XMap map, XMCS mcs,String listPath) throws IOException {
		ArrayList<Thread> live= new ArrayList<Thread>();
		Thread tr = null;
		String path = null;
		path = MULTI_CHANNEL_PATH;

		if(listPath==null || mcsDataSelect[0].isSelected()){
			listPath = MULTI_CHANNEL_EXP_LIST;
			path = MULTI_CHANNEL_PATH;
		} else if ( mcsDataSelect[1].isSelected()) {
			listPath = USGS_MULTI_CHANNEL_EXP_LIST;
			path = USGS_MULTI_CHANNEL_PATH;
		} else if ( mcsDataSelect[2].isSelected()) {
			listPath = USGS_SINGLE_CHANNEL_EXP_LIST;
			path = USGS_SINGLE_CHANNEL_PATH;
		} else if( mcsDataSelect[3].isSelected()) {
			listPath = ANTARCTIC_SDLS_EXP_LIST;
			path = ANTARCTIC_SDLS_PATH;
		}
		URL url = URLFactory.url( listPath);
		BufferedReader in = new BufferedReader( new InputStreamReader(url.openStream()));

		Vector<XMCruise> tmp = new Vector<XMCruise>(); // XMCruise
		String inStr;
		while ((inStr = in.readLine()) != null) {
			String[] split = inStr.split("\t");
			if (split.length != 6) continue; // improper entry

			int mapType = MapApp.MERCATOR_MAP;
			if (map.getApp() instanceof MapApp)
				mapType =((MapApp) map.getApp()).getMapType();

			try {
				int cruiseType = Integer.parseInt(split[1]);
				switch (mapType) {
				case MapApp.MERCATOR_MAP:
					if ((cruiseType & XMCS.MERCATOR_MAP) == 0) continue;
					break;
				case MapApp.SOUTH_POLAR_MAP:
					if ((cruiseType & XMCS.SOUTH_POLAR_MAP) == 0) continue;
					break;
				case MapApp.NORTH_POLAR_MAP:
					if ((cruiseType & XMCS.NORTH_POLAR_MAP) == 0) continue;
					break;
				default:
					break;
				}

				Point2D.Double wn = new Point2D.Double(Double.parseDouble(split[2]),
										Double.parseDouble(split[5]));
				Point2D.Double es = new Point2D.Double(Double.parseDouble(split[3]),
										Double.parseDouble(split[4]));

				XMCruise cruise = new XMCruise(mcs, map, split[0]);
				cruise.setBounds(wn, es);
				tmp.add(cruise);
			} catch (NumberFormatException ex) {
				continue;
			}
		}

		cruises = new XMCruise[tmp.size()];
		for( int i=0 ; i<cruises.length ; i++){
			cruises[i] = (XMCruise) tmp.get(i);
			
			boolean dateLineCheck = cruises[i].cruiseIDL;
			boolean check2 = cruises[i].bounds.contains(map.getWrap()/2,cruises[i].getBounds().getY()) || cruises[i].bounds.contains(0,cruises[i].getBounds().getY());
			boolean polarProblems = (((MapApp) map.getApp()).getMapType()==MapApp.SOUTH_POLAR_MAP) || (((MapApp) map.getApp()).getMapType()==MapApp.NORTH_POLAR_MAP); 
			if(dateLineCheck || check2){
				try {
					cruises[i].loadLines(path);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(polarProblems){
				
				final int j = i;
				final String thPath = path;
				final XMap thrMap=map;
				live.add( new Thread(){ 
					public void run(){
						try{
							cruises[j].loadLines(thPath);
							thrMap.repaint();
						}catch(Exception e){
							e.printStackTrace();
						}
					}
				});
				
				live.get(live.size()-1).start();
			}
		}



		initiallized = true;
		/*while(true){
			int i=0;
			for(Thread t:live){
				if(t.isAlive()){
					i++;
				}
			}
			if(i==0){
				map.repaint();
				break;
			}
		}*/

	}
}

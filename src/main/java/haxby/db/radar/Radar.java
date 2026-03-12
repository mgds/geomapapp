package haxby.db.radar;

import haxby.db.Database;
import haxby.db.mcs.CDP;
import haxby.db.xmcs.XMCS;
import haxby.db.xmcs.XMCruise;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.UTFDataFormatException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;


/**
 * The base class for marine Radar (Multi-Channel Seismic) data.
 * Holds a list of all Radar cruises Lamont-Doherty Geological 
 * Observatory's seismic database.
 * 
 */
public class Radar implements ActionListener, 
			MouseListener, 
			MouseMotionListener,
			Database {
	protected static RCruise[] cruises;
	protected static boolean initiallized = false;
	private  Map<String, String> uidMap;
	private String selectedPingFile;
	private static String uidURLString = PathUtil.getPath("PORTALS/RADAR_LOOKUP",
			MapApp.BASE_URL+"/data/portals/sp_radar/radar_lookup/");
	private static String RADAR_PATH = PathUtil.getPath("PORTALS/RADAR_PATH",
			MapApp.BASE_URL+"/data/portals/sp_radar/");
	private static String RADAR_EXP_LIST = PathUtil.getPath("PORTALS/RADAR_EXP_LIST",
			MapApp.BASE_URL+"/data/portals/sp_radar/radar_lookup/expedition_list.txt");
	private static String RADAR_BOUNDS = PathUtil.getPath("PORTALS/RADAR_BOUNDS",
			MapApp.BASE_URL+"/data/portals/sp_radar/bounds");
	private static String RADAR_CONTROL = PathUtil.getPath("PORTALS/RADAR_CONTROL",
			MapApp.BASE_URL+"/data/portals/sp_radar/radar_control");
	private static String MULTI_CHANNEL_PATH =  PathUtil.getPath("PORTALS/MULTI_CHANNEL_PATH",
			MapApp.BASE_URL+"/data/portals/mcs/");

	private  String selectedDataUID;
	XMap map = null;
	JPanel panel = null;
	JComboBox cruiseList = null;
	JComboBox lineList = null;
	JMenu[] menus;
	RCruise currentCruise = null;
	static RLine currentLine = null;
	RImage image = null;
	RImage imageAlt = null;
	String mapSelect = "selectLine";
	boolean mouseE = false;
	boolean enabled = false;
	JSplitPane imagePane;
	JRadioButton orientH, orientV;
	private JButton infoBtn;
	public Radar( XMap map ) {
		this.map = map;
		image = new RImage();
		imageAlt = new RImage();
		image.setOtherImage( imageAlt );
		imageAlt.setOtherImage( image );
		imagePane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT,
				image.panel, imageAlt.panel );
		
		imagePane.setDividerLocation(400);		
		imagePane.setOneTouchExpandable( true );
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
	//	image = new RImage();
	//	imageAlt = new RImage();
		image.disposeImage();
		imageAlt.disposeImage();
		imagePane.setLeftComponent( image.panel );
		imagePane.setRightComponent( imageAlt.panel );
		uidMap.clear();
	}
	public boolean loadDB() {
		if( initiallized ) return true;
		uidMap = new HashMap<String, String>();
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
		return "Radar Profiles";
	}

	public String getCommand() {
		return "radar_cmd";
	}

	public String getDescription() {
		return "Antarctic Airborn Database - UNDER CONSTRUCTION";
	}
	public JComponent getDataDisplay() {
		return imagePane;
	}
	public JComponent getSelectionDialog() {
		if( !initiallized )return null;
		if(panel!=null) {
			return panel;
		}
		Font font = new Font("SansSerif", Font.PLAIN, 12);
		javax.swing.border.Border border = BorderFactory.createCompoundBorder(
				BorderFactory.createEtchedBorder(),
				BorderFactory.createEmptyBorder(1,3,1,3));
		cruiseList = new JComboBox();
		cruiseList.addItem("Survey");
		for(int i=0 ; i<cruises.length ; i++) cruiseList.addItem(cruises[i]);
		JLabel label;
		JButton btn;
		Box box = Box.createVerticalBox();

		JPanel panel1 = new JPanel(new GridLayout(0,1));

		panel1.add( cruiseList );

		lineList = new JComboBox();
		lineList.addItem("Line");
		panel1.add(lineList);

		btn = new JButton("view-1");
		btn.addActionListener(this);
		panel1.add(btn);
		btn = new JButton("view-2");
		btn.addActionListener(this);
		panel1.add(btn);
		infoBtn = new JButton("Survey Info");
		infoBtn.setEnabled(false);
		infoBtn.addActionListener(this);
		panel1.add(infoBtn);
//		btn = new JButton("Download Cruise Data");
//		btn.addActionListener(this);
//		panel1.add(btn);
		ButtonGroup gp1 = new ButtonGroup();
		orientV = new JRadioButton( "Vertical", false );
		orientV.addActionListener(this);
		gp1.add(orientV);
		panel1.add( orientV );
		orientH = new JRadioButton( "Horizontal", true );
		orientH.addActionListener(this);	
		
		
		gp1.add(orientH);
		panel1.add( orientH );
		box.add(panel1);

		javax.swing.border.Border border1 = BorderFactory.createEmptyBorder(2,3,1,3);

		JPanel panel2 = new JPanel(new GridLayout(3,1));

		ButtonGroup grp = new ButtonGroup();
		label = new JLabel("map select");
		label.setFont(font);
		label.setForeground(Color.black);
		label.setBorder(border1);
		label.setOpaque(true);
		panel2.add(label);
		JRadioButton button = new JRadioButton("area");
		button.setFont(font);
		button.setSelected(true);
		button.setBorder(border1);
		button.setActionCommand("selectArea");
		button.addActionListener(this);
		grp.add(button);
		panel2.add(button);
		button = new JRadioButton("line");
		button.setFont(font);
		button.setBorder(border1);
		button.setActionCommand("selectLine");
		button.addActionListener(this);
		grp.add(button);
		panel2.add(button);

		panel2.setBorder(BorderFactory.createLineBorder(Color.black));
	//	box.add(panel2);

		cruiseList.addActionListener(this);
		lineList.addActionListener(this);
		lineList.setRenderer( new RLineRenderer() );

		panel = new JPanel();
		panel.add(box);		
		return panel;
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
	//	while(k!=k0) {
		for(k=0 ; k<cruises.length ; k++) {
			cruises[k].draw(g);
		}
		if(currentCruise!=null) {
		//	g.setColor(Color.white);
		//	currentCruise.draw(g);
			g.setColor(Color.black);
			currentCruise.drawLines(g);
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
		if( image.line != null) {
			int[] cdp = image.getVisibleSeg();
			image.line.drawSeg( cdp[0], cdp[1], g);
		}
		if( imageAlt.line != null) {
			int[] cdp = imageAlt.getVisibleSeg();
			imageAlt.line.drawSeg( cdp[0], cdp[1], g);
		}
	}
	void setSelectedCruise(RCruise cruise) {
		if( map==null || !map.hasOverlay(this) )return;
		if(cruise == currentCruise)return;
		currentLine = null;
		lineList.removeAllItems();
		lineList.addItem("Line");
		currentCruise = cruise;
		map.repaint();
		if(cruise==null)return;
		RLine[] lines = currentCruise.getLines();
		for( int i=0 ; i<lines.length ; i++) lineList.addItem(lines[i]);
	}
	void setSelectedLine(RLine line) {
		if( map==null || !map.hasOverlay(this) )return;
		if(line == currentLine) return;
		RCruise cruise=null;
		try {
			cruise = (RCruise) cruiseList.getSelectedItem();
		} catch ( ClassCastException e ) {
			return;
		}
		synchronized (map.getTreeLock()) {
			Graphics2D g = map.getGraphics2D();
			currentLine = line;			
			draw(g);
		}
	}
	public void actionPerformed( ActionEvent e ) {
		String cmd = e.getActionCommand();
		if( e.getSource() == cruiseList ) {
			infoBtn.setEnabled(cruiseList.getSelectedItem() instanceof RCruise);
			try {
				String tmpCruise = (currentCruise==null) ? "null" : currentCruise.getID();
				setSelectedCruise((RCruise) cruiseList.getSelectedItem());
			} catch ( ClassCastException ex ) {
				setSelectedCruise( null );
			} finally {
				mouseE = false;
			}
		} else if( e.getSource() == lineList ) {
			try {
				setSelectedLine((RLine) lineList.getSelectedItem());
			} catch ( ClassCastException ex ) {
				setSelectedLine( null );
			}
		}else if(cmd.equals("Download Cruise Data")){
			//Is this part ever called?
			if(currentLine==null)return;

			selectedPingFile = currentLine.getID();
			URL url = null;
			BufferedReader in = null;
			try{
			url = URLFactory.url(RADAR_EXP_LIST);
			in = new BufferedReader( new InputStreamReader(url.openStream()) );

			String name_lookup;

			while ((name_lookup = in.readLine()) != null ) {
				String[] split = name_lookup.split("\\s");
				if(split[0].equalsIgnoreCase(currentLine.getCruiseID())){
					selectedPingFile = split[1];
					break;
				}
			}
			}catch(Exception ex){
				ex.printStackTrace();
			}
			ArrayList<String> missing = new ArrayList<String>();
			String request = uidURLString + selectedPingFile + ".data_lookup";
			try {
				ArrayList<String> types = new ArrayList<String>();
				types.add("jpg");
				types.add("segy");
				types.add("nav");
				types.add("nc");
				types.add("mat");
				
				for(String type_string:types){
				url = URLFactory.url(request);
				in = new BufferedReader( new InputStreamReader(url.openStream()) );
				selectedDataUID="";
				String s;
				while ( (s = in.readLine()) != null ) {
					String[] split = s.split("\\s");
					if((split[0].equalsIgnoreCase(currentLine.getID()) || split[0].equalsIgnoreCase(currentLine.getCruiseID()+"-"+currentLine.getID())) && split[1].equalsIgnoreCase(type_string)){
						if(split.length>=3)
							selectedDataUID = split[2];
						break;
					}
				}
				if(selectedDataUID.isEmpty())
				{
					missing.add(type_string);
				}
				}	
			} catch (IOException ec) {
				ec.printStackTrace();
			}

			JPanel savePrompt = new JPanel(new GridLayout(0, 1));
			JCheckBox imageFullCB = new JCheckBox("Save Viewport");
			JCheckBox imageCB = new JCheckBox("Save jpg");
			JCheckBox segyCB = new JCheckBox("Save segy");
			JCheckBox navCB = new JCheckBox("Save nav");
			JCheckBox matCB = new JCheckBox("Save mat");
			JCheckBox ncCB = new JCheckBox("save nc");
			ButtonGroup boxGroup = new ButtonGroup();

			if(missing.contains("jpg"))
				imageCB.setEnabled(false);
			if(missing.contains("segy"))
				segyCB.setEnabled(false);
			if(missing.contains("nav"))
				navCB.setEnabled(false);
			if(missing.contains("mat"))
				matCB.setEnabled(false);
			if(missing.contains("nc"))
				ncCB.setEnabled(false);

			boxGroup.add(imageFullCB);
			boxGroup.add(imageCB);
			boxGroup.add(segyCB);
			boxGroup.add(navCB);
			boxGroup.add(matCB);
			boxGroup.add(ncCB);

			savePrompt.add(imageFullCB);
			savePrompt.add(imageCB);
			savePrompt.add(segyCB);
			savePrompt.add(navCB);
			savePrompt.add(matCB);
			savePrompt.add(ncCB);

			JOptionPane.showConfirmDialog(MapApp.anchor, savePrompt, "Save what?", JOptionPane.OK_CANCEL_OPTION);

			String type = null;

			if(imageCB.isSelected())
				type = "jpg";
			if(segyCB.isSelected())
				type = "segy";
			if(navCB.isSelected())
				type = "nav";
			if(matCB.isSelected())
				type = "mat";
			if(ncCB.isSelected())
				type = "nc";

			try {
				url = URLFactory.url(request);
				in = new BufferedReader( new InputStreamReader(url.openStream()) );
				selectedDataUID="";
				String s;
				while ( (s = in.readLine()) != null ) {
					System.out.println(s);
					String[] split = s.split("\\s");
					if((split[0].equalsIgnoreCase(currentLine.getID()) || split[0].equalsIgnoreCase(currentLine.getCruiseID()+"-"+currentLine.getID())) && split[1].equalsIgnoreCase(type)){
						if(split.length>=3)
							selectedDataUID = split[2];
						break;
					}
				}
			} catch (IOException ec) {
				ec.printStackTrace();
			}
			if(selectedDataUID.isEmpty())
				return;

			String str = "https://www.marine-geo.org/tools/search/Files.php?client=GMA&data_set_uid="+ selectedDataUID;
			BrowseURL.browseURL(str);

		}else if(cmd.equals("Survey Info")) {
			if(currentCruise == null)
				return;
			String name=null;
			try{
				URL url = URLFactory.url(RADAR_EXP_LIST);
				BufferedReader in = new BufferedReader( new InputStreamReader(url.openStream()) );

				String name_lookup;

				while ((name_lookup = in.readLine()) != null ) {
					String[] split = name_lookup.split("\\s");
					if(split[0].equalsIgnoreCase(currentCruise.getID())){
						name = split[1];
						break;
					}
				}
				}catch(Exception ex){
					ex.printStackTrace();
				}

				BrowseURL.browseURL("https://www.marine-geo.org/tools/search/entry.php?id="+name);

		}else if( cmd.equals("view-1") ) {
			if( currentLine==null )return;
			try {
				if (!image.saving)
					image.loadImage(currentLine);
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(map.getTopLevelAncestor(), "unable to load image\n"
					+ex.getMessage());
				image.line = null;
			//	ex.printStackTrace();
			}
			synchronized( map.getTreeLock() ) {
				Graphics2D g = map.getGraphics2D();
				draw( g );
			}
		} else if( cmd.equals("view-2") ) {
			if( currentLine==null )return;
			try {
				if (!imageAlt.saving)
					imageAlt.loadImage(currentLine);
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(map.getTopLevelAncestor(), "unable to load image\n"
					+ex.getMessage());
				imageAlt.line = null;
			//	ex.printStackTrace();
			}
			synchronized( map.getTreeLock() ) {
				Graphics2D g = map.getGraphics2D();
				draw( g );
			}
		} else if( e.getSource() == orientV || e.getSource() == orientH ) {
			Dimension dim = imagePane.getSize();
			if( orientV.isSelected() ) {
				imagePane.setOrientation(imagePane.VERTICAL_SPLIT);
				imagePane.setDividerLocation( dim.height/2 );
			} else {
				imagePane.setOrientation(imagePane.HORIZONTAL_SPLIT);
				imagePane.setDividerLocation( dim.width/2 );
			}
		} else if( cmd.startsWith("select") ) {
			mapSelect = "selectLine";
		} else if( cmd.equals("close") ) {
		//	close();
		} else if( cmd.equals("quit")) {
		}
	}
	public void mouseClicked(MouseEvent e) {
		if(e.isControlDown())return;
		if(e.getSource() != map) return;
		double scale = map.getZoom();
		Insets insets = map.getMapBorder().getBorderInsets(map);
		Point2D.Double p = (Point2D.Double) map.getScaledPoint( e.getPoint() );
		if(mapSelect.trim().equals("selectArea")) {
			int k = cruiseList.getSelectedIndex();
			k=k%cruises.length;
			int k0 = k;
			double wrap = map.getWrap();
			while(true)  {	
				if(cruises[k].contains(p.x, p.y, wrap) ) {
					mouseE = true;
					cruiseList.setSelectedItem(cruises[k]);
					return;
				}
				k = (k+1)%cruises.length;
				if(k==k0)break;
			} 
			cruiseList.setSelectedIndex(0);
		} else if(mapSelect.equals("selectLine")) {
			if(currentCruise==null)return;
			RLine[] lines = currentCruise.getLines();
			if(lines==null || lines.length==0) return;
			int k = -1;
			if( currentLine!=null) for( k=0 ; k<lines.length ; k++) {
				if( lines[k]==currentLine ) break;
			}
			k=(k+1)%lines.length;
			int k0 = k;
			double minDist = 1/scale;
			k0 = -1;
			for( int kk=0 ; kk<lines.length ; kk++) {
				double dist = lines[k].distanceSq(p.x, p.y);
				if(dist < minDist) {
					minDist = dist;
					k0 = k;
					break;
				}
				k = (k+1)%lines.length;
			} 
			if(k0!=-1) lineList.setSelectedItem(lines[k0]);
			else lineList.setSelectedIndex(0);
		} else if(mapSelect.equals("selectPt")) {
		//	if(image==null || image.getLine()==null ) return;
		//	RLine line = image.getLine();
		}
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
	 * Initiallizes all the <code>RCruise</code> objects in the ldeo database.
	 */
	static String[] users = new String[] {
				"mstuding",
				"robinb",
				"bill" };
	protected static void initRadar(XMap map, Radar mcs) throws IOException {
/*
		try {
			String name = System.getProperty("user.name");
			boolean ok = false;
			for(int k=0 ; k<users.length ; k++) {
				if( name.equals(users[k]) ) {
					ok=true;
					break;
				}
			}
			if( !ok ) throw new IOException("You do not have permission to access this database");
		} catch(Exception ex) {
			throw new IOException("You do not have permission to access this database");
		}
*/
		//DEP change the URLs to read from the sp_radar portal, added AGAP data
		//it reads some file like antarctic/radar/radar_control
		String s;
		URL url = URLFactory.url(RADAR_CONTROL); 
		//url2 - The AGAP files read like XMCS files DEP 9.20.2011
		/*"http://new.geomapapp.org/data/portals/mcs/AGAP/nav/mcs_control"*/
		URL url2 = URLFactory.url(RADAR_PATH + "expedition_list");
		
		//String cruiseID = "";
		//String lineID = "";
		//RCruise expedition = null;
		Vector tmp = new Vector();
		//loads stuff into tmp

		for(int i=0;i<2;i++)
		{
		String cruiseID = "";
		String lineID = "";
		RCruise expedition = null;
		if(i==0)
		{
		DataInputStream in = new DataInputStream( url.openStream() );
		while( true ) {
			try {				
				s = in.readUTF();
				System.out.println(s);
			} catch (EOFException ex) {
				break;
			}
			StringTokenizer st = new StringTokenizer(s);
			String id = st.nextToken();
			String cruise = st.nextToken();
			if( !cruise.equals( cruiseID ) ) {
				expedition = new RCruise( mcs, map, cruise );
				tmp.add( expedition );
//	System.out.println( cruise );
				cruiseID = cruise;
			}
			int nseg = in.readInt();
			in.readInt();
			in.readInt();
			in.readInt();
			int npt = in.readInt();
			CDP[] cdp = new CDP[2];
			int[] entry = new int[] {
				in.readInt(),
				in.readInt(),
				in.readInt() };
			cdp[0] = new CDP( entry[2],
				(double)(entry[0]*1.e-6),
				(double)(entry[1]*1.e-6),
				(long)entry[2], false);
			entry = new int[] {
				in.readInt(),
				in.readInt(),
				in.readInt() };
			cdp[1] = new CDP( entry[2],
				(double)(entry[0]*1.e-6),
				(double)(entry[1]*1.e-6),
				(long)entry[2], true);
			RLine line = new RLine( map, expedition, id, cdp );
			//System.out.println( "\t"+ id );
			expedition.addLine( line );
		}//end of the while true
		
		in.close();		
		}
		if(i==1)
		{
			
			BufferedReader in = new BufferedReader( new InputStreamReader(url2.openStream()));

			
			String inStr;
			while ((inStr = in.readLine()) != null) {
				String[] split = inStr.split("\t");
				if (split.length != 6) 
					{
					System.out.println("improper");// improper entry
					continue;
					}

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
					default:
						break;
					}

					Point2D.Double wn = new Point2D.Double(Double.parseDouble(split[2]),
											Double.parseDouble(split[5]));
					Point2D.Double es = new Point2D.Double(Double.parseDouble(split[3]),
											Double.parseDouble(split[4]));
					
					expedition = new RCruise(mcs, map, split[0]);
					expedition.addLine(expedition.loadLines(RADAR_PATH)[0]);
					
					tmp.add(expedition);
				} catch (NumberFormatException ex) {
					continue;
				}
			}
		}
		}
		cruises = new RCruise[tmp.size()];
		for( int i=0 ; i<cruises.length ; i++) {
			cruises[i] = (RCruise) tmp.get(i);
			cruises[i].setBounds();
			RLine[] lines = cruises[i].getLines();
			for( int k=0 ; k<lines.length-1 ; k++) {
				for( int j=k+1 ; j<lines.length ; j++) {
					double[] crs = RLine.cross(lines[k], lines[j]);
					if(crs==null) continue;
					lines[k].addCrossing( crs[0], crs[1], lines[j] );
					lines[j].addCrossing( crs[1], crs[0], lines[k] );
				}
			}
		}
		URL url3 = URLFactory.url(RADAR_BOUNDS);
		URL url4 = URLFactory.url(RADAR_PATH + "AGAP/nav/bounds");
		ArrayList<URL> boundURLs = new ArrayList<URL>();
		boundURLs.add(url3);
		boundURLs.add(url4);
		for(URL currentURL: boundURLs)
		{
		DataInputStream in = new DataInputStream( currentURL.openStream() );
		BufferedReader reader = new BufferedReader(
				new InputStreamReader( in ));
		while( (s=reader.readLine()) != null ) {
			StringTokenizer st = new StringTokenizer(s);
			String id = st.nextToken();
			int index = -1;
			for( int i=0 ; i<cruises.length ; i++) {
				if( id.equals( cruises[i].getID() )) {
					index = i;
					break;
				}
			}
			if( index==-1 ) continue;
			RLine[] lines = cruises[index].getLines();
			index = -1;
			id = st.nextToken();
			for( int i=0 ; i<lines.length ; i++) {
				if( id.equals( lines[i].getID() )) {
					index = i;
					break;
				}
			}
			if( index==-1 ) continue;
			double[] cdpRange = new double[] {
					Double.parseDouble( st.nextToken()),
					Double.parseDouble( st.nextToken()) };
			double[] zRange = new double[] {
					Double.parseDouble( st.nextToken()),
					Double.parseDouble( st.nextToken()) };
			lines[index].setRanges( cdpRange, zRange );
		}
		}		
		initiallized = true;
	}
}

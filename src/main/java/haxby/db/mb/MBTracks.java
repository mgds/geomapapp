package haxby.db.mb;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;

import haxby.db.Database;
import haxby.db.mgg.MGG;
import haxby.map.MapApp;
import haxby.map.Overlay;
import haxby.map.XMap;
import haxby.nav.ControlPt;
import haxby.nav.Nearest;
import haxby.nav.TimeControlPt;
import haxby.nav.TrackLine;
import haxby.proj.Projection;
import haxby.util.BrowseURL;
import haxby.util.FilesUtil;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

public class MBTracks implements Database, Overlay, MouseListener {
	protected XMap map;
//	MBTrack[] tracks;
	protected Vector cruises;
	protected int selectedCruise;
	protected int selectedTrack;
	protected int size;
	protected MBSelection mbSel;
	protected boolean plot;
	protected boolean enabled;
	protected boolean loaded;
	protected JTextArea display;
	protected Point p;

//	***** GMA 1.6.0: Add button to bring up selected ping file in datalink
	protected JButton retrievePingFile;
	protected JPanel trackInfoAndButton;
	protected String selectedPingFile;
	protected String cruiseSelected;
//	***** GMA 1.6.0

//	***** GMA 1.6.4: Set up variables to read in UIDs and set them for selected file
	protected Set<String> loadedCruises;
	protected Map<String, String> uidMap;
	protected String uidURLString = PathUtil.getPath("PORTALS/MB_LOOKUP",
			MapApp.BASE_URL+"/data/portals/mb/mb_lookup/");
	protected String MARINE_GEO_PATH_URLS = PathUtil.getPath("MARINE_GEO_PATH_URLS",
			MapApp.BASE_URL+"/gma_paths/MARINE_GEO_paths.xml");
	protected String selectedDataUID;
	protected String selectedDataSetUID;
//	***** GMA 1.6.4
	// Cache paths
	protected File gmaRoot = MapApp.getGMARoot();
	protected File topCacheDir = new File(gmaRoot, "menus_cache");
	protected File portalCacheDir = new File(topCacheDir, "portals");
	protected File portalCacheDir2 = new File(portalCacheDir, "multibeam_bathymetry_cmd");
	protected File portalCacheFile = new File( portalCacheDir2, "mb_control_merc");
	protected File portalCacheFileN = new File( portalCacheDir2, "mb_control_NP");
	protected File portalCacheFileS = new File( portalCacheDir2, "mb_control_SP");
	protected  String control;
	static Color cruiseColor = new Color( 160, 60, 60 );
	protected String mbStatString = "http://www.marine-geo.org/tools/search/GMAPortalStats.php?cmd=multibeam_bathymetry_cmd";

	protected JDialog dialogProgress;
	protected JProgressBar pb;
	protected JPanel progressPanel;
	protected JLabel progressLabel;
	
	private String name = "Multibeam Bathymetry Swaths";

	public MBTracks( XMap map, int size ) {
		this.map = map;
		control = null;
	//	tracks = new MBTrack[size];
		cruises = new Vector();
		this.size = 0;
		plot = false;
		selectedCruise = -1;
		selectedTrack = -1;
		mbSel = new MBSelection(this);
		enabled = false;
		loaded = false;
		display = new JTextArea("none selected");
		display.setForeground(Color.black);

//		***** GMA 1.6.0: Add button to bring up selected ping file in datalink
		retrievePingFile = new JButton("Download selected ping file");
		retrievePingFile.addMouseListener(this);
		trackInfoAndButton = new JPanel(new BorderLayout());
		trackInfoAndButton.add(display, BorderLayout.NORTH);
		trackInfoAndButton.add(retrievePingFile, BorderLayout.EAST);
//		***** GMA 1.6.0
	}
	public MBTracks( XMap map, int size, String control) {
		this( map, size );
		this.control = control;
	}
	public boolean isLoaded() {
		return loaded;
	}
	public void unloadDB() {
		loaded = false;
	}
	public void disposeDB() {
	//	tracks = null;
		mbSel.cruises.setSelectedItem(null);
		uidMap.clear();
		loadedCruises.clear();
		cruises.clear();
		mbSel.cruisesListModel.clear();
		loaded = false;
	}
	
	public boolean loadDB() {
		if( loaded ) return true;
		int mapType = ((MapApp) map.getApp()).getMapType();
		sendStat(mapType); // Send stat

		dialogProgress = new JDialog((Frame)null, "Loading Files");
		progressPanel = new JPanel(new BorderLayout());
		progressPanel.setBorder(BorderFactory.createEmptyBorder(5, 1, 5, 1));
		dialogProgress.setLocationRelativeTo(map);
		pb = new JProgressBar(0,100);
		progressLabel = new JLabel("Processing Files");
		progressPanel.add(progressLabel, BorderLayout.NORTH);
		progressPanel.add(pb);
		dialogProgress.getContentPane().add(progressPanel);
		dialogProgress.setPreferredSize(new Dimension(180,60));
		dialogProgress.pack();
		dialogProgress.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialogProgress.setVisible(true);
		dialogProgress.setAlwaysOnTop(true);

		Map<String, String> hashSha1Map = new HashMap<String, String>();
		
		//Float versionGMRT = Float.parseFloat(MapApp.versionGMRT);	
		try {
			DataInputStream in = null;
			File inf = null;
			if( control == null) {
				String mggControl = null;
				// Read in the sha1 hash values from the mb_control_sha1 file.
				// Should be in the format PROJECTION:SHA1_HASH_CODE
				String hashSha1File = PathUtil.getPath("GMRT_LATEST/MB_CONTROL_SHA1");
				try {
					BufferedReader inFile = new BufferedReader(new InputStreamReader(new URL(hashSha1File).openStream()));
					String str = null;
					while ( (str = inFile.readLine()) != null) {
						String[] splitStr = str.split("  ");
						hashSha1Map.put(splitStr[1], splitStr[0]);
					}
					inFile.close();
				} catch (IOException e) {
					System.out.println("Error reading in sha1 hash code file " + hashSha1File);
				}

				// Determine which map assign correct control file and cache file
				String remoteHash;
				switch (mapType) {
				case MapApp.MERCATOR_MAP:
				default:
					mggControl = PathUtil.getPath("GMRT_LATEST/MB_CONTROL_MERC");
					inf = new File(portalCacheFile.getAbsolutePath());
					break;
				case MapApp.SOUTH_POLAR_MAP:
					mggControl = PathUtil.getPath("GMRT_LATEST/MB_CONTROL_SP");
					inf = new File(portalCacheFileS.getAbsolutePath());
					break;
				case MapApp.NORTH_POLAR_MAP:
					mggControl = PathUtil.getPath("GMRT_LATEST/MB_CONTROL_NP");
					inf = new File(portalCacheFileN.getAbsolutePath());
					break;
				}
				
				String controlFile = mggControl.substring(mggControl.lastIndexOf("/") + 1);
				remoteHash = hashSha1Map.get(controlFile);
				URL url = URLFactory.url(mggControl);

				// No cache get from server
				if(MapApp.getMbPortalCache() == false || MapApp.AT_SEA) {
					System.out.println("load from server");
					in = new DataInputStream(new BufferedInputStream(url.openStream()));
				} else if (MapApp.getMbPortalCache() == true) {
					// read in from local cache file
					try {
						System.out.println("load from cache");
						String localHash = null;
						localHash = FilesUtil.createSha1(inf);

						if (!localHash.matches(remoteHash)) {
							System.out.println("updating cached file");
							makeCache(inf, mggControl);
						}	
					}
					catch (Exception e) {
						System.out.println("no cache file found, creating new one");
						makeCache(inf, mggControl);
					}
					
					try {
						in = new DataInputStream(new BufferedInputStream(new FileInputStream(portalCacheFile)));
					} catch(IOException io) {
						System.out.println("no cache file found, read from server");
						in = new DataInputStream(new BufferedInputStream(url.openStream()));
					}
				}
			} else {
				System.out.println("using control url");
				URL url = URLFactory.url( control );
				in = new DataInputStream(new BufferedInputStream(url.openStream()));
				
				switch (mapType) {
					case MapApp.MERCATOR_MAP:
					default:
						inf = new File(portalCacheFile.getAbsolutePath());
						break;
					case MapApp.SOUTH_POLAR_MAP:
						inf = new File(portalCacheFileS.getAbsolutePath());
						break;
					case MapApp.NORTH_POLAR_MAP:
						inf = new File(portalCacheFileN.getAbsolutePath());
						break;
				}
			}

			Point2D.Double pt = new Point2D.Double();
			double wrap = map.getWrap();
			double wraptest = wrap/2.;
			double xtest = 0d;
			Projection proj = map.getProjection();
			String name = "";
			String leg = "";
			MBCruise cruise = null;
			double wesn[] = new double[] {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};

			// Update progress labels
			int lengthFile = (int)inf.length();
			pb.setMaximum(lengthFile);
			progressLabel.setText("Loading Tracks");
			pb.setIndeterminate(false);
			dialogProgress.pack();
			while( true ) {
				try {
					name = in.readUTF();
					StringTokenizer st = new StringTokenizer(name);
					String mbFile = st.nextToken();
					leg = st.nextToken();
					name = mbFile;

					if (cruise != null && !cruise.getName().equals(leg)) {
						wesn = new double[] {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
					}
				} catch (EOFException ex) {
					// End of file break out of while
					pb.setValue(pb.getValue() + (lengthFile - pb.getValue()));
					pb.repaint();
					dialogProgress.dispose();
					break;
				}
				int nseg = in.readInt();
				ControlPt[][] cpt = new ControlPt[nseg][];
				int start = in.readInt();
				int end = in.readInt();
				int fmt = in.readInt();
				int time;
				Rectangle2D.Double bounds = new Rectangle2D.Double();

				for( int i=0 ; i<nseg ; i++) {
					cpt[i] = new ControlPt[in.readInt()];
					for( int j=0 ; j<cpt[i].length ; j++) {
						pt.x = 1.e-6 * in.readInt();
						pt.y = 1.e-6 * in.readInt();
						wesn[0] = Math.min(wesn[0], pt.x);
						wesn[1] = Math.max(wesn[1], pt.x);
						wesn[2] = Math.min(wesn[2], pt.y);
						wesn[3] = Math.max(wesn[3], pt.y);
						if( control!=null) {
							time = in.readInt();
						} else {
							time = 0;
						}
						Point2D.Double p = (Point2D.Double)proj.getMapXY(pt);
						if(j==0&&i==0) {
							bounds.x = p.x;
							bounds.y = p.y;
							bounds.width = 0.;
							bounds.height = 0.;
							xtest = p.x;
						} else {
							if( wrap>0 ) {
								while(p.x>xtest+wraptest) p.x-=wrap;
								while(p.x<xtest-wraptest) p.x+=wrap;
							}
							if(p.x<bounds.x) {
								bounds.width += bounds.x-p.x;
								bounds.x = p.x;
								xtest = bounds.x + .5*bounds.width;
							} else if( p.x>bounds.x+bounds.width ) {
								bounds.width = p.x-bounds.x;
								xtest = bounds.x + .5*bounds.width;
							}
							if(p.y<bounds.y) {
								bounds.height += bounds.y-p.y;
								bounds.y = p.y;
							} else if( p.y> bounds.y+bounds.height ) {
								bounds.height = p.y-bounds.y;
							}
						}
						if( control != null ) {
							cpt[i][j] = new TimeControlPt( 
								new ControlPt.Float( (float)p.x, (float)p.y ),
								time);
						} else {
							cpt[i][j] = new ControlPt.Float( (float)p.x, (float)p.y );
						}
					}
				}
				if( name.startsWith("par92015") ) {
					continue;
				}
				byte mask = -1;
				bounds.x -= .5;
				bounds.y -= .5;
				bounds.width += 1.;
				bounds.height += 1.;
				MBTrack track = new MBTrack( new TrackLine( name, bounds,
								cpt , start, end, mask, (int)wrap));
				if( cruise==null || !leg.equals(cruise.getName() )) {
					cruise = new MBCruise( leg, fmt );
					cruises.add( cruise);
					mbSel.cruisesListModel.addElement(cruise);
				}
				cruise.addTrack(track);
				// Update progress bar.
				pb.setValue(pb.getValue() + (2*track.getName().length() + 50));
				pb.repaint();
			}
		} catch ( IOException ex ) {
			loaded = false;
			ex.printStackTrace();
		}
		Collections.sort( cruises, new Comparator() {
				public int compare(Object o1, Object o2) {
					String s1 = o1.toString();
					String s2 = o2.toString();
					return s1.compareTo(s2);
				}
				public boolean equals( Object o ) {
					return o==this;
				}
			});
		uidMap = new HashMap<String, String>();
		loadedCruises = new HashSet<String>();

		loaded = true;
//	trim();
		return loaded;
	}
	
	public void add(MBTrack track) {
	//	if(size==tracks.length) {
	//		MBTrack[] tmp = new MBTrack[size+10];
	//		System.arraycopy(tracks, 0, tmp, 0, size);
	//		tracks = tmp;
	//	}
	//	tracks[size++] = track;
	}
	void trim() {
	//	if( size< tracks.length ) {
	//		MBTrack[] tmp = new MBTrack[size];
	//		System.arraycopy(tracks, 0, tmp, 0, size);
	//		tracks = tmp;
	//	}
	}

	public void sendStat(int mapType) {
		// Send usage stat
		switch (mapType) {
		case MapApp.MERCATOR_MAP:
			default:
			mbStatString = mbStatString + "&sub_name=mercator";
			break;
		case MapApp.SOUTH_POLAR_MAP:
			mbStatString = mbStatString + "&sub_name=south";
			break;
		case MapApp.NORTH_POLAR_MAP:
			mbStatString = mbStatString + "&sub_name=north";
		}
		mbStatString = mbStatString.replaceAll("\\s", "%20");
		//System.out.println(mbStatString);
		URL logURL;
		try {
			logURL = URLFactory.url(mbStatString);
			InputStream inStat = logURL.openStream();
			inStat.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getDBName() {
		return name;
	}
	
	public void setDBName(String newName) {
		name = newName;
	}

	public String getCommand() {
		return "multibeam_bathymetry_cmd";
	}

	public String getDescription() {
		return "Multibeam navigation for Ridge MBS";
	}
	public JComponent getSelectionDialog() {
		return mbSel.getDialog();
	}
	public JComponent getDataDisplay() {

//		***** GMA 1.6.0: TESTING
//		return display;
		return trackInfoAndButton;
//		***** GMA 1.6.0
	}

	public void makeCache(File inf, String mggControl) {
		try {
			URL urlCopy = URLFactory.url(mggControl);
			InputStream oriFile = urlCopy.openStream();
			
			portalCacheDir2.mkdirs();
			
			Files.copy(oriFile, inf.toPath(), REPLACE_EXISTING);
			oriFile.close();
			
		} catch (IOException e){
			System.out.println("cache not allowed");
		}
	}
	
	
	public void setEnabled( boolean tf ) {
		if( tf && enabled ) return;
		if( tf ) {
			map.addMouseListener(this);
		} else {
			map.removeMouseListener( this );
		}
		enabled = tf;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void draw(Graphics2D g) {
		if(!plot)return;
		Stroke stroke = g.getStroke();
		g.setStroke( new BasicStroke( 1f/(float)map.getZoom() ));
		if(enabled) {
			g.setColor(Color.black);
		} else {
			g.setColor(new Color(120, 90, 60));
		}
		for( int k=0 ; k<cruises.size() ; k++) {
			Vector t = ((MBCruise)cruises.get(k)).tracks;
			for( int i=0 ; i<t.size() ; i++) {
				((MBTrack)t.get(i)).draw(g);
			}
		}
		if(enabled) {
			drawSelectedCruise(g, Color.white);
			drawSelectedTrack(g, cruiseColor);
		}
		g.setStroke(stroke);
	}
	public void setPlot(boolean plot) {
		if(this.plot == plot) {
			return;
		}
		this.plot = plot;
		map.repaint();
	}
	protected void setSelectedCruise( int c ) {
		if( selectedCruise==c ) return;
		synchronized (map.getTreeLock()) {
			Graphics2D g = map.getGraphics2D();
			drawSelectedCruise( g, Color.black );
			selectedCruise = c;
			drawSelectedCruise( g, Color.white);
		}
	}
	void drawSelectedCruise( Graphics2D g, Color color ) {
		int size = cruises.size();
		if(selectedCruise<0 || selectedCruise>=size) return;
		MBCruise cruise = (MBCruise)cruises.get(selectedCruise);
		g.setStroke( new BasicStroke( 1f/(float)map.getZoom() ));
		g.setColor(color);
		Vector tracks = cruise.tracks;
		for( int i=0 ; i<tracks.size(); i++) {
			((MBTrack)tracks.get(i)).draw(g);
		}
	}
	void drawSelectedTrack( Graphics2D g, Color color ) {
		int size = cruises.size();
		if(selectedCruise<0 || selectedCruise>=size) return;
		MBCruise cruise = (MBCruise)cruises.get(selectedCruise);
		Vector tracks = cruise.tracks;
		if( selectedTrack<0 || selectedTrack>=tracks.size()) return;
		g.setStroke( new BasicStroke( 1f/(float)map.getZoom() ));
		g.setColor(color);
		((MBTrack)tracks.get(selectedTrack)).draw(g);
	}

	public void mousePressed(MouseEvent evt) {
	}
	public void mouseReleased( MouseEvent evt) {
	}
	public void mouseClicked( MouseEvent evt) {
		p = evt.getPoint();
		if ( evt.getSource().equals(retrievePingFile) ) {
			PathUtil.loadNewPaths(MARINE_GEO_PATH_URLS);
			String DOWNLOAD_PING_FILE_PATH = PathUtil.getPath("DOWNLOAD_PING_FILE_PATH", 
									"https://www.marine-geo.org/tools/search/GMADownload.php");
			String str = DOWNLOAD_PING_FILE_PATH + "?client=GMA&data_uid=" + selectedDataUID;

			if (selectedDataUID == null) return;

			BrowseURL.browseURL(str);
		}
//		***** GMA 1.6.0

		else {
			if( !plot) return;
			if(evt.isControlDown())return;
			if(evt.isShiftDown())return;
			double zoom = map.getZoom();
			Nearest nearest = new Nearest(null, 0, 0, Math.pow(2./zoom, 2) );
			Insets insets = map.getMapBorder().getBorderInsets(map);
			double x = (evt.getX()-insets.left)/zoom;
			double y = (evt.getY()-insets.top)/zoom;
			boolean back = false;
		//	boolean back = evt.isShiftDown();
			int size = cruises.size();
			int i0 = selectedCruise;
			while( i0<0 ) i0+=size;
			if( back ) i0+=size;
			for( int k=0 ; k<size ; k++) {
				int i = back ?
					(i0 - (1+k))%size :
					(i0 + 1+k )%size;
				MBCruise cruise = (MBCruise)cruises.get(i);
				Vector files = cruise.tracks;
				int j0 = selectedTrack;
				while( j0<0 ) j0 += files.size();
				if( back ) i0 -= files.size();
				for( int kk=0 ; kk< files.size() ; kk++) {
					int j = back ?
						(j0 - (1+kk))%files.size() :
						(j0 + 1+kk )%files.size();
					MBTrack track = (MBTrack) files.get(j);
					if( !track.contains(x, y) ) continue;
					if( track.firstNearPoint(x, y, nearest) ) {
						mbSel.cruises.setSelectedIndex( i );
						synchronized (map.getTreeLock()) {
							Graphics2D g = map.getGraphics2D();
						//	if(i==selectedCruise && j==selectedTrack)return;
							if( i!=selectedCruise) drawSelectedCruise(g, Color.black);
							selectedCruise = i;
							selectedTrack = j;
							drawSelectedCruise(g, Color.white);
							drawSelectedTrack(g, cruiseColor);
							updateDisplay(cruise, track, nearest);
							return;
						}
					}
				}
			}
		display.setText( "none selected" );
			mbSel.cruises.setSelectedItem( null );
			if(selectedCruise==-1) {
				return;
			}
			synchronized (map.getTreeLock()) {
				Graphics2D g = map.getGraphics2D();
				drawSelectedCruise(g, Color.black);
			}
			selectedCruise = -1;
			selectedTrack = -1;
		}
	}
	public Object getSelectionObject(double x, double y, double distanceSq) {
		return null;
	}
	public void selectObject( Object selectedObject ) {
	}
	public void mouseEntered( MouseEvent evt) {
	}
	public void mouseExited( MouseEvent evt) {
	}

	public void updateDisplay(MBCruise cruise, MBTrack track, Nearest nearest) {
		String text = "cruise: "+cruise.getName()+ ", file: "+ track.getName();

		selectedPingFile = track.getName();
		cruiseSelected = cruise.getName();

		if (!loadedCruises.contains(cruise.getName())) {
			// Try to load the lookup file
			String request = uidURLString + cruise.getName() + ".data_lookup";

			try {
				URL url = URLFactory.url(request);
				BufferedReader in = new BufferedReader( new InputStreamReader(url.openStream()) );

				String s;
				while ( (s = in.readLine()) != null ) {
					//System.out.println(s);
					String[] split = s.split("\\s");
					uidMap.put(split[0], split[1] + "," + split[2]);
				}
			} catch (IOException e) {
				//e.printStackTrace();
			}
			loadedCruises.add(cruise.getName());
		}

		String uids = uidMap.get(selectedPingFile);
		if ( uids == null && selectedPingFile.indexOf(".gz") != -1 ) {
			uids = uidMap.get( selectedPingFile.substring( 0, selectedPingFile.indexOf(".gz") ) );
		}

		if ( uids != null ) {
			String[] result = uids.split(",");
			selectedDataSetUID = result[0];
			selectedDataUID = result[1];
		}
		else {
			selectedDataSetUID = null;
			selectedDataUID = null;
		}
//			***** GMA 1.6.4

		//	+",  format= " + cruise.getMBFormat() ;
		long t = track.getTime(nearest);
		if( t!=-1 ) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeZone( TimeZone.getTimeZone("GMT") );
			cal.setTime( new Date( t ) );
			StringBuffer date = new StringBuffer();
			date.append( cal.get(cal.YEAR) +"-" );
			int day = cal.get(cal.DAY_OF_YEAR);
			if( day<10 ) date.append( "0" );
			if( day<100 ) date.append( "0" );
			date.append( day +"-" );
			day = cal.get( cal.HOUR_OF_DAY );
			if( day<10 ) date.append( "0" );
			date.append( day  +":");
			day = cal.get( cal.MINUTE );
			if( day<10 ) date.append( "0" );
			date.append( day );
			text += ", date: " + date.toString();
		//	text += ", time = "+(t/1000);
		}
		display.setText( text);
	}
}
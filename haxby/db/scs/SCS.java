package haxby.db.scs;

import haxby.db.Database;
import haxby.dig.Digitizer;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.nav.Nearest;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.ProcessingDialog;
import haxby.util.URLFactory;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.GeneralPath;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class SCS implements Database,
			MouseListener,
			ListSelectionListener,
//			MouseMotionListener,
			ActionListener {

	static String SINGLE_CHANNEL_PATH = PathUtil.getPath("PORTALS/SINGLE_CHANNEL_PATH",
			MapApp.BASE_URL+"/data/portals/scs/");

//	static String baseURL = "file:/scratch/ridgembs/bill/SCS/";
	static String baseURL = SINGLE_CHANNEL_PATH;
	protected SCSCruise[] cruises;
	protected static boolean initiallized = false;
	protected XMap map;
//	SCSImage image1, image2;
	protected JComboBox cruiseCB;

//	1.3.5: Scroll pane on right side of window
	protected JScrollPane cruiseSP;
//	1.3.5: List contained by cruiseSP
	protected JList cruiseList;
//	1.3.5: Wrapper for elements in cruiseList
	protected DefaultListModel cruiseListModel;
//	1.3.5: Display area in cruiseSP to display items in cruiseList
	protected JTextField cruiseName;
	protected boolean enabled;

//	1.3.5: Indicates whether cruiseList has been 
//	fully populated and is ready to receive commands
	protected boolean cruiseListPopulated = false;
	protected JSplitPane imagePane;
	protected JRadioButton orientH, orientV;

//	1.3.5: Button clicked to view profile of selected track
	protected JButton view;
	protected int selCruise = -1;
	protected int selPanel = -1;
	protected GeneralPath selPath = null;
	protected GeneralPath imagePath = null;
	protected JLabel label;
	protected SCSImage2 image;
	protected JPanel dataPanel;
	protected JPanel selectionDialog;
	protected Digitizer dig;
	protected JScrollPane imageSP;

	protected JDialog dialogProgress;
	protected JProgressBar pb;
	protected JPanel progressPanel;
	protected JLabel progressLabel;

	public SCS( XMap map ) {
		this.map = map;
		cruiseCB = null;
		enabled = false;
		label = new JLabel( "Lamont single-channel seismics data rescue project" );
		label.setOpaque( true );
		label.setBackground( Color.white );
		image = createSCSImage();
		dig = new Digitizer( image );
		dataPanel = new JPanel( new BorderLayout() );
		JPanel digPanel = new JPanel( new BorderLayout() );
		Box box = Box.createHorizontalBox();
		JButton save = new JButton( haxby.image.Icons.getIcon(haxby.image.Icons.SAVE, false) );
		save.setPressedIcon( haxby.image.Icons.getIcon(haxby.image.Icons.SAVE, true) );
		save.setBorder( null );
		save.setActionCommand( "save" );
		save.setToolTipText( "save digitized products");
		save.addActionListener( this );
		box.add( save );

		JToggleButton depB = new JToggleButton( haxby.image.Icons.getIcon(haxby.image.Icons.DEPTH, 
					false) );
		depB.setSelectedIcon( haxby.image.Icons.getIcon(haxby.image.Icons.DEPTH, true) );
		depB.setBorder( null );
		depB.setActionCommand( "depth" );
		depB.addActionListener( this );
		depB.setSelected( true );
		depB.setToolTipText( "toggle seafloor reflector" );
		box.add( depB );

		box.add( Box.createHorizontalStrut( 4 ) );
		box.add( dig.getPanel() );

//		1.3.5: Add zoom-icons from SCSImage2 to tool-bar
//		on the bottom-left of the window
		box.add( image.getPanel() );

		digPanel.add( box, "North" );
		digPanel.add( new JScrollPane( dig.getList()), "Center" );
		dataPanel.add( digPanel, "West");
		image.setDigitizer( dig );
		imageSP = new JScrollPane( image );
		dataPanel.add( imageSP, "Center" );

		JPanel infoPanel = new JPanel( new BorderLayout() );
		ClassLoader cl = getClass().getClassLoader();
		try {
			URL url = cl.getResource("org/geomapapp/resources/logos/noaa_h.gif");
			ImageIcon icon = new ImageIcon( url );
			JLabel logo = new JLabel(icon);
			logo.setBorder( BorderFactory.createEtchedBorder() );
			logo.setToolTipText("https://www.ngdc.noaa.gov/  (click to open URL)");
			logo.addMouseListener( new MouseAdapter() {
					public void mouseClicked( MouseEvent evt ) {
						BrowseURL.browseURL("https://www.ngdc.noaa.gov");
					}
				});
			JPanel logoPanel = new JPanel( new BorderLayout() );
			logoPanel.add( logo, "East" );
			url = cl.getResource("org/geomapapp/resources/logos/ldeo_h.gif");
			icon = new ImageIcon( url );
			logo = new JLabel(icon);
			logo.setBorder( BorderFactory.createEtchedBorder() );
		//	logo.setToolTipText("http://www.ldeo.columbia.edu/  (click to copy to clipboard)");
			logo.setToolTipText("https://lamont.columbia.edu/  (click to open URL)");
			logo.addMouseListener( new MouseAdapter() {
					public void mouseClicked( MouseEvent evt ) {
						BrowseURL.browseURL("https://lamont.columbia.edu");
					}
				});
			logoPanel.add( logo, "West" );
			infoPanel.add( logoPanel, "West" );
		} catch ( Exception ex ) {
			ex.printStackTrace( System.err );
		}
		label.setBorder( BorderFactory.createEtchedBorder() );
		infoPanel.add( label, "Center" );
		dataPanel.add( infoPanel, "South" );
		selectionDialog = new JPanel( new FlowLayout(FlowLayout.CENTER,1,1));
		selectionDialog.setPreferredSize(new Dimension (200, 250));

//		***** Changed by A.K.M. 1.3.5 *****
//		Initialize cruiseSP, cruiseList and cruiseListModel
		cruiseListModel = new DefaultListModel();
		cruiseList = new JList(cruiseListModel);
		cruiseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		cruiseList.setSelectedIndex(0);

//		Change this to adjust the default size of cruiseSP
		cruiseList.setVisibleRowCount(10);
		cruiseSP = new JScrollPane(cruiseList);
		cruiseSP.setColumnHeaderView( new JLabel("All SCS Legs"));
		cruiseSP.setToolTipText("double-click a leg to view profile");
		cruiseList.setToolTipText("double-click a leg to view profile");
		cruiseList.addMouseListener(new MouseListener() {

			public void mouseClicked(MouseEvent e) {
//				If a double-click occurs in the list view the 
//				profile of the selected track
				if (e.getClickCount() == 2)	{
					view.doClick();
				}
				return;
			}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
		});

//		Added separate load button
		JButton load = new JButton( 
				"<html><body><center>"
				+"Load Previously<br>"
				+"Digitized Products"
				+"</center></body></html>");
		load.setToolTipText("Click to find a file. Leg needs to be selected.");
//		***** Changed by A.K.M. 1.3.5 *****

		view = new JButton( "View Profile" );
		view.setToolTipText("Click to view profile.");

		JPanel tempPane =  new JPanel(new BorderLayout(0,2));
		tempPane.add(cruiseSP,"North");
		tempPane.add(load,"Center");
		tempPane.add(view,"South");

		selectionDialog.add(tempPane);

		view.addActionListener( this );
		load.setActionCommand("Load Previously Digitized Products");
		load.addActionListener( this );
	}
	protected SCSImage2 createSCSImage() {
		return new SCSImage2(this);
	}
	public void setEnabled( boolean tf ) {
		if( tf == enabled) return;
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
		map.removeMouseListener( this);
		initiallized = false;

//		***** Changed by A.K.M. 1.3.5 *****
//		Make sure everything is set and disposed of properly
//		when disposeDB() is called

		selPath = null;
		selCruise = -1;

		if (image.timeDep != null)
			image.timeDep.clear();
		if (image.panels != null)
			image.panels.clear();
		image.cruise = null;
		image.paths = null;

		cruiseListModel.clear();
		cruises = null;
		cruiseListPopulated = false;
		
		dig.reset();
		try {
			finalize();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.gc();
//		***** Changed by A.K.M. 1.3.5 *****
	}
	public boolean loadDB() {
		if( initiallized ) return true;
		// create progress panel
		dialogProgress = new JDialog((Frame)null, "Loading Files");
		progressPanel = new JPanel(new BorderLayout());
		progressPanel.setBorder(BorderFactory.createEmptyBorder(5, 1, 5, 1));
		dialogProgress.setLocationRelativeTo(map);
		pb = new JProgressBar(0,100);
		progressLabel = new JLabel("Processing Files");
		progressPanel.add(progressLabel, BorderLayout.NORTH);
		progressPanel.add(pb);
		dialogProgress.getContentPane().add(progressPanel);
		dialogProgress.setPreferredSize(new Dimension(190,60));
		dialogProgress.pack();
		dialogProgress.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialogProgress.setVisible(true);
		dialogProgress.setAlwaysOnTop(true);
		try {
//			1.3.5: Every time the cruises are loaded cruiseList
//			needs to be re-populated
			cruiseListPopulated = false;

			URL url = URLFactory.url( baseURL + "cruises" );
			int lengthFile = 0;
			BufferedReader in = new BufferedReader(new InputStreamReader( url.openStream() ));
			String s;
			Vector<String> tmp_cruises = new Vector<String>();
			while( (s=in.readLine()) != null ) {
				tmp_cruises.add( s );
				lengthFile++;
			}
			in.close();
			pb.setMaximum(lengthFile);
			progressLabel.setText("Loading files");
			pb.setIndeterminate(false);
			dialogProgress.pack();
			cruises = new SCSCruise[tmp_cruises.size()];
			for( int k=0 ; k<tmp_cruises.size() ; k++) {
				String path = baseURL + tmp_cruises.get(k) + "/";
				try {
					cruises[k] = new SCSCruise( map, path );

					/* Change C#### cruises to RC#### for correct path.
					 * Should be fixed on data paths.
					 */
					if(cruises[k].name.startsWith("C")) {
						cruises[k].name = cruises[k].name.replace("C", "RC");
					}
//					1.3.5: Populate cruiseList via cruiseListModel
					cruiseListModel.addElement(cruises[k]);
				} catch (IOException ex) {
					ex.printStackTrace();
					cruises[k] = null;
					continue;
				}
				pb.setValue(pb.getValue() + 1);
				int roundValue = (int) (pb.getValue()/(lengthFile * 0.01));
				progressLabel.setText("Loading files " + roundValue + " % complete. ");
			}
				dialogProgress.pack();
//				1.3.5: Start listening to list selections after list 
//				has been populated (otherwise listens during list population
//				TODO: Possible memory leak from invoking every time loadDB is 
//				called
			cruiseList.addListSelectionListener(this);
			pb.repaint();
			dialogProgress.dispose();
//			1.3.5: cruiseList is now populated
			cruiseListPopulated = true;

		} catch(IOException ex) {
			initiallized = false;
			return false;
		}
		initiallized = true;
		return initiallized;
	}
	public boolean isLoaded() {
		return initiallized;
	}
	public void unloadDB() {
		initiallized = false;
	}
	public String getDBName() {
		return "Analog Seismic Reflection Profiles";
	}

	public String getCommand() {
		return "scs_cmd";
	}

	public String getDescription() {
		return "Lamont Single Channel Seismic Reflection Database";
	}
	public JComponent getDataDisplay() {
		return dataPanel;
	}
	public JComponent getSelectionDialog() {
		return selectionDialog;
	}
	public void draw(Graphics2D g) {
		if( map==null || cruises.length==0)return;
		g.setStroke(new BasicStroke(1f/(float)map.getZoom()));
		g.setColor(Color.darkGray);
		for( int k=0 ; k<cruises.length ; k++) {
			if( cruises[k] != null ) cruises[k].draw(g);
		}

		if( selCruise!=-1 && cruises[selCruise] != image.cruise ) {
			int k = selCruise;
			g.setColor( Color.white );
			cruises[k].draw(g);
			if( selPath!=null ) {
				g.setColor( Color.white );
				g.draw(selPath);
			}
		}

		if( image.cruise!=null ) {
//			***** GMA 1.6.6: On Macs, make the lines thicker so that alternate display method can be used
//			g.setStroke(new BasicStroke(3f/(float)map.getZoom()));

			String osName = System.getProperty("os.name"); 
			if ( osName.startsWith("Mac OS") ) {
				g.setStroke(new BasicStroke(5f/(float)map.getZoom()));
			}
			else {
				g.setStroke(new BasicStroke(3f/(float)map.getZoom()));
			}
//			***** GMA 1.6.6
			g.setColor( Color.darkGray );
			image.cruise.draw(g);

//			***** GMA 1.6.6: On Macs, make the lines thicker so that alternate display method can be used
//			g.setStroke(new BasicStroke(1f/(float)map.getZoom()));

			if ( osName.startsWith("Mac OS") ) {
				g.setStroke(new BasicStroke(5f/(float)map.getZoom()));
			} else {
				g.setStroke(new BasicStroke(1f/(float)map.getZoom()));
			}
//			***** GMA 1.6.6

			g.setColor( Color.red );
			if( image.paths!=null ) {
				g.draw( image.paths );

//				***** GMA 1.6.6: Account for wrap
				double wrap = image.cruise.map.getWrap();
				if( wrap>0. ) {
					if ( image.cruise.map.getFocus() == null ) {
						g.translate( wrap, 0. );
					}
					g.draw( image.paths );
				}
//				***** GMA 1.6.6
			}
		}
	}
	public void mousePressed(MouseEvent evt) {}

	public void mouseReleased( MouseEvent evt) {}

	public void mouseClicked( MouseEvent evt) {
		if(evt.isControlDown())return;
		double zoom = map.getZoom();
		Nearest nearest = new Nearest(null, 0, 0, Math.pow(2./zoom, 2) );
		Insets insets = map.getMapBorder().getBorderInsets(map);
		double x = (evt.getX()-insets.left)/zoom;
		double y = (evt.getY()-insets.top)/zoom;
		if( image.cruise!=null && image.cruise.firstNearPoint(x, y, nearest) ) {
			image.scrollTo( (int)(image.cruise.getTime(nearest)/1000L) );
			return;
		}
		int c = selCruise;

		int kk0 = c;
		for( int kk=0 ; kk<=cruises.length ; kk++) {
			int ic = (kk+kk0+1)%cruises.length;
			while( ic>=cruises.length ) ic -= cruises.length;
			try {
				if( !cruises[ic].contains(x, y ) ) continue;
			} catch( NullPointerException ex) {
				System.out.println("null pointer ic = "+ic);
				continue;
			}
			if( !cruises[ic].firstNearPoint(x, y, nearest) ) continue;
			String text = cruises[ic].name +" "+ 
					SCSCruise.dateString(cruises[ic].getTime(nearest));
			selPanel = cruises[ic].getPanel( cruises[ic].getTime(nearest) );
			selPath = cruises[ic].getPanelPath( selPanel );

			label.setText( text );
			synchronized (map.getTreeLock()) {
				Graphics2D g = map.getGraphics2D();
				g.setStroke( new BasicStroke( 1f/(float)zoom ));
				if( c>=0 ) {
					g.setColor( Color.darkGray );
					cruises[c].draw(g);
				}
				selCruise = ic;
				g.setColor( Color.white );
				cruises[ic].draw(g);

//				1.3.5: Jump to same track in cruiseList and scroll to that part of the list
				cruiseList.setSelectedIndex(ic);
				cruiseList.ensureIndexIsVisible((ic));

				if( selPath!=null ) {
					g.setColor( Color.white );
					g.draw( selPath );
				}

//					1.3.5: If the track is double-clicked view the profile
				if (evt.getClickCount() == 2)	{
					view.doClick();
				}
				return;
			}
		}
		if( c>=0 ) {
			synchronized (map.getTreeLock()) {
				Graphics2D g = map.getGraphics2D();
				g.setStroke( new BasicStroke( 1f/(float)zoom ));
				g.setColor( Color.darkGray );
				cruises[c].draw(g);
			}
		}
		selCruise = -1;
	}
	public void mouseEntered( MouseEvent evt) {
	}
	public void mouseExited( MouseEvent evt) {
	}
	public void actionPerformed( ActionEvent evt ) {
		if( evt.getActionCommand().equals("View Profile") ) {
			if( selCruise<0 ) {
				if( image.cruise==null)return;
				imagePath = null;
				label.setText( "Lamont single-channel seismics data rescue project" );
				image.cruise = null;
				image.panels = new Vector();
				image.invalidate();
				image.repaint();
				map.repaint();
				return;
			}

			// Add progress bar while retrieving list of data
			ProcessingDialog ld = new ProcessingDialog(MapApp.getApp().getFrame(), MapApp.getApp().getMap());
			ld.addTask("Retrieving Data", new Thread( new Runnable() {
			public void run() {
				int c = selCruise;
				try {
//					1.3.5: Do not run through load code in setCruise
					image.setCruise( cruises[c], false );
				} catch (IOException ex ) {
					ex.printStackTrace();
				}
			}
			}));

			map.repaint();
			Dimension d = map.getTopLevelAncestor().getSize();
		}

//		***** Changed by A.K.M. 1.3.5 *****
//		Same as "view profile" but run through load code in setCruise
		else if( evt.getActionCommand().equals("Load Previously Digitized Products" )) {
			if( selCruise<0 ) {
				if( image.cruise==null) {
					JOptionPane.showMessageDialog(MapApp.anchor, "Select A Line");
					return;
				}
				imagePath = null;
			//	image.image = null;
				label.setText( "Lamont single-channel seismics data rescue project" );
				image.cruise = null;
				image.panels = new Vector();
				image.invalidate();
				image.repaint();
				map.repaint();
				return;
			}
			int c = selCruise;
			try {
				image.setCruise( cruises[c], true );
			} catch (IOException ex ) {
				ex.printStackTrace();
			}
			map.repaint();
		}
//		***** Changed by A.K.M. 1.3.5 *****
		else if( evt.getActionCommand().equals("depth" ) ) {
			image.setPlotDepth( ((JToggleButton)evt.getSource()).isSelected() );
		} else if( evt.getActionCommand().equals("save" ) ) {
			image.save();
		}
	}
	public void setInfoText( String text ) {
		label.setText( text );
	}

//	***** Changed by A.K.M. 1.3.5 *****
//	Highlight selected track on map when selected in cruiseList
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == false) {

			if (cruiseList.getSelectedIndex() == -1) {
			//No selection, disable fire button.
				return;
			} else {
				//Selection, enable the fire button.
				//if (cruiseList.getSelectedIndex() - 1 != -1) {
				//System.out.println("Item selected: " + cruiseListModel.get(cruiseList.getSelectedIndex() - 1));
				//}
				//System.out.println(cruiseList.getSelectedIndex());
				if (cruiseListPopulated) {
					int c = selCruise;
					double zoom = map.getZoom();
					Nearest nearest = new Nearest(null, 0, 0, Math.pow(2./zoom, 2) );
					int ip = 0;
					int ic = cruiseList.getSelectedIndex();
					String text = cruises[ic].name +" "+ 
					SCSCruise.dateString(cruises[ic].getTime(nearest));
					selPanel = cruises[ic].getPanel( cruises[ic].getTime(nearest) );
					selPath = cruises[ic].getPanelPath( selPanel );
					label.setText( text );

					synchronized (map.getTreeLock()) {
						Graphics2D g = map.getGraphics2D();
						g.setStroke( new BasicStroke( 1f/(float)zoom ));
						if( c>=0 ) {
							g.setColor( Color.darkGray );
							cruises[c].draw(g);
						}
						selCruise = ic;
						g.setColor( Color.white );
						cruises[ic].draw(g);
						if( selPath!=null ) {
							g.setColor( Color.white );
							g.draw( selPath );
						}
						return;
					}
				}
			}
		}
	}
}
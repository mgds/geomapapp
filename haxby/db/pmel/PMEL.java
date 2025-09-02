package haxby.db.pmel;

import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.db.*;
import haxby.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

/**
 * The Locations and Timing of Seafloor Earthquakes and Volcanic Eruptions shows earthquake
 * events in specific regions. The data source is comes from PMEL/NOAA.
 * 
 * @author Samantha Chan
 * @author William F. Haxby
 * @version 3.1.0
 * @since 1.1.6
 */
public class PMEL implements Database, ActionListener {
	static Vector<PMELEvent> earthquakes;

	XMap map;
	Vector<PMELEvent> current;
	JPanel dialog;
	JLabel kountLabel;
	JTextField minYear;
	JTextField maxYear;
	JRadioButton[] datasets;
	JComboBox activity;
	JList activityList;
	DefaultListModel activityListModel;
	JScrollPane activitySP;
	JCheckBox visible;
	JButton loadB, loadB2;
	JLabel eventLabel;
	static boolean loaded = false;
	static boolean loadedEvent = false;
	boolean enabled = false;
	int dataset, dataset2;
	EventHistogram hist, hist2;
	ScaleTime scaleTime = null;
	ArrayList<String> specificEvents;
	String[] patternE = {"LuckyStrike, 2001",
			"Easter Microplate 22S, 1996",
			"Gorda Ridge Seismic Event 2001" //2
};

	public PMEL(XMap map) {
		this.map = map;
		earthquakes=new Vector<PMELEvent>();
		current =new Vector<PMELEvent>();
		initDialog();
		kountLabel = new JLabel("0 events, 0 shown");
		kountLabel.setForeground( Color.black);
		kountLabel.setFont( new Font( "SansSerif", Font.PLAIN, 12));
		dataset = -1;
		dataset2 = -1;
	}
	public String getDBName() {
		return "Seafloor Earthquakes and Volcanic Eruptions";
	}

	public String getCommand() {
		return "seve_cmd";
	}

	public String getDescription() {
		return "Deep Ocean Seismicity from Hydroacoustic Monitoring";
	}
	public boolean loadDB() {
		if(loaded) return true;
		if( dataset==-1 ) {
			JPanel panel = new JPanel( new GridLayout(0, 1) );
			JLabel label = new JLabel( "Select a data set to display:" );
			panel.add( label );
			JRadioButton sosus = new JRadioButton("Juan de Fuca \"SOSUS\"", true);
			JRadioButton epr = new JRadioButton( "East Pacific Rise \"EPR\"", false);
			JRadioButton mar = new JRadioButton( "Mid-Atlantic Ridge \"MAR\"", false);

			ButtonGroup gp = new ButtonGroup();
			gp.add( sosus );
			gp.add( epr );
			gp.add( mar );

			panel.add( sosus );
			panel.add( epr );
			panel.add( mar );

			int ok = JOptionPane.showConfirmDialog( map.getTopLevelAncestor(),
						panel, "Select", JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.PLAIN_MESSAGE);
			if(ok!=JOptionPane.OK_OPTION) return false;
			int dataIndex = 0;
			if( sosus.isSelected()) dataIndex=0;
			else if( epr.isSelected()) dataIndex=1;
			else if( mar.isSelected()) dataIndex=2;

			datasets[dataIndex].setSelected(true);
			return load(dataIndex);
		}
		return true;
	}

	// Loads radio button selections
	boolean load(int dataIndex) {
		loadedEvent = false;
		dataset2 = -1;
		if(loaded && dataset==dataIndex ) return true;
		earthquakes = new Vector<PMELEvent>();
		dataset = dataIndex;

		try {
			String pmelPath = PathUtil.getPath("PORTALS/EARTHQUAKE_PMEL_PATH",
					MapApp.BASE_URL+"/data/portals/eq_seafloor/pmel/")
						+ dataset +".txt";
			URL url = URLFactory.url(pmelPath);
			URLConnection urlConn = url.openConnection();
			urlConn.setDoInput(true); 
			urlConn.setUseCaches(false);

			BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

			int minTime = 2000000000;
			int maxTime = 0;
			Calendar calP = Calendar.getInstance( TimeZone.getTimeZone("GMT") );
			calP.set( Calendar.MILLISECOND, 0 );

			// Read Data File
			String s;
			while ((s = in.readLine())!= null){
				if (s.startsWith("*/")){
					while (true) try{
						s = in.readLine();
						// Convert time to millisecond
						calP.set( Calendar.YEAR, Integer.parseInt(s.substring(0,4)) );			//YYYY
						calP.set( Calendar.DAY_OF_YEAR, Integer.parseInt(s.substring(4,7)) );	//DDD
						calP.set( Calendar.HOUR_OF_DAY, Integer.parseInt(s.substring(7,9)) );	//HH
						calP.set( Calendar.MINUTE, Integer.parseInt(s.substring(9,11)) );		//MM
						calP.set( Calendar.SECOND, Integer.parseInt(s.substring(11,13)) );		//SS

						// Set values
						int time = (int)(calP.getTime().getTime()/1000);
						if( time>maxTime ) maxTime = time;
						if( time<minTime ) minTime = time;
						float lon = Float.valueOf(s.substring(42, 50));
						float lat = Float.valueOf(s.substring(34, 41));
						float elon = Float.valueOf(s.substring(58, 64));
						float elat = Float.valueOf(s.substring(51, 57));
						float mag = Float.valueOf(s.substring(72, 79));
						//System.out.println("time: " + time + " lon: " + lon + " lat:" + lat + " elon: " + elon + " elat: " + elat+ " mag: " + mag);

						if( elon>.1f || elat>.1f )continue;
						Point2D p = map.getProjection().getMapXY( new Point2D.Float(lon, lat));
						earthquakes.add( new PMELEvent( time, (float)p.getX(), (float)p.getY(), mag ));
						} catch (NullPointerException ex) {
						earthquakes.trimToSize();
						loaded = true;

						current = new Vector<PMELEvent>( earthquakes.size() );
						for( int k=0 ; k<earthquakes.size() ; k++) {
							current.add( earthquakes.get(k) );
						}

						double dt = 3600.*24.*7.;
						hist = new EventHistogram( this, dt );
						if( scaleTime==null ) scaleTime=new ScaleTime(this, hist);
						else scaleTime.setHist(hist);
						break;
					}
				} // end of if statement
			}
			in.close(); // close BR
		} catch(IOException ex) {
			loaded = false;
		}
		return loaded;
	}

	// Loads drop down list selections file format is different.
	boolean loadEvent(int dataIndex2) {
		loaded = false;
		dataset = -1;
		if(loadedEvent && dataset2==dataIndex2 ) return true;
		earthquakes = new Vector<PMELEvent>();
		dataset2 = dataIndex2;
		try {
			String pmelPath = PathUtil.getPath("PORTALS/EARTHQUAKE_PMEL_PATH",
					MapApp.BASE_URL+"/data/portals/eq_seafloor/pmel/")
						+ "event/" + dataset2 +".txt";
			URL url = URLFactory.url(pmelPath);
			URLConnection urlConn = url.openConnection();
			urlConn.setDoInput(true); 
			urlConn.setUseCaches(false);

			BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

			int minTime = 2000000000;
			int maxTime = 0;
			Calendar calP = Calendar.getInstance( TimeZone.getTimeZone("GMT") );
			calP.set( Calendar.MILLISECOND, 0 );

			// Read Data File
			String s;
			while ((s = in.readLine())!= null){
				if (s.startsWith("*/")){
					while (true) try{
						s = in.readLine();
						// Convert time to millisecond
						calP.set( Calendar.YEAR, Integer.parseInt(s.substring(0,4)) );			//YYYY
						calP.set( Calendar.DAY_OF_YEAR, Integer.parseInt(s.substring(4,7)) );	//DDD
						calP.set( Calendar.HOUR_OF_DAY, Integer.parseInt(s.substring(7,9)) );	//HH
						calP.set( Calendar.MINUTE, Integer.parseInt(s.substring(9,11)) );		//MM
						calP.set( Calendar.SECOND, Integer.parseInt(s.substring(11,13)) );		//SS

						// Set values
						int time = (int)(calP.getTime().getTime()/1000);
						if( time>maxTime ) maxTime = time;
						if( time<minTime ) minTime = time;
						float lon = Float.valueOf(s.substring(40, 50));
						float lat = Float.valueOf(s.substring(31, 40));
						float elon = Float.valueOf(s.substring(59, 67));
						float elat = Float.valueOf(s.substring(51, 59));
						float mag = Float.valueOf(s.substring(75, 82));
						//System.out.println("time: " + time + " lon: " + lon + " lat:" + lat + " elon: " + elon + " elat: " + elat+ " mag: " + mag);
						// If error is bigger then 0.1 throw out
						if( elon>.1f || elat>.1f ) continue;
						Point2D p = map.getProjection().getMapXY( new Point2D.Float(lon, lat));
						earthquakes.add( new PMELEvent( time, (float)p.getX(), (float)p.getY(), mag ));

					} catch (NullPointerException ex) {
						earthquakes.trimToSize();
						loadedEvent = true;
						current = new Vector<PMELEvent>( earthquakes.size() );

						for( int k=0 ; k<earthquakes.size() ; k++) {
							current.add( earthquakes.get(k) );
						}

						double dt = 3600.*24.*7.;
						hist = new EventHistogram( this, dt );
						if( scaleTime==null ) scaleTime=new ScaleTime(this, hist);
						else scaleTime.setHist(hist);
						break;
					}
				} // end of if statement
			}
			in.close(); // close BR
		} catch(IOException ex) {
			loadedEvent = false;
		}
		return loadedEvent;
	}

	public boolean isLoaded() {
		return loaded;
	}

	public void unloadDB() {
		loaded = false;
	}
	
	public void disposeDB() {
		earthquakes = new Vector<PMELEvent>();
		dataset = -1;
		dataset2 = -1;
		loaded = false;
		loadedEvent = false;
	}
	public void setEnabled( boolean tf ) {
		return;
	}
	public boolean isEnabled() {
		return loaded;
	}
	void initDialog() {
		dialog = new JPanel( new BorderLayout() );
		JPanel center1 = new JPanel( new BorderLayout() );
		JPanel areas = new JPanel( new GridLayout(0, 1) );
		datasets = new JRadioButton[3];
		datasets[0] = new JRadioButton("SOSUS");
		datasets[0].setToolTipText("Juan de Fuca");
		datasets[1] = new JRadioButton("EPR");
		datasets[1].setToolTipText( "East Pacific Rise");
		datasets[2] = new JRadioButton("MAR");
		datasets[2].setToolTipText("Mid-Atlantic Ridge");
		ButtonGroup gp = new ButtonGroup();
		gp.add( datasets[0] );
		gp.add( datasets[1] );
		gp.add( datasets[2] );

		datasets[dataset].setSelected(true);
		areas.add( datasets[0] );
		areas.add( datasets[1] );
		areas.add( datasets[2] );

		loadB = new JButton("Load");
		areas.add( loadB );
		loadB.addActionListener( this );
		areas.setBorder( BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.black), "Areas") );
		center1.add( areas, "North" );

		JPanel activityPanel = new JPanel( new GridLayout(0, 1) );
		activityPanel.setBorder( BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.black), "Events") );
		// Items here still matches order of files name

		// Take items and sort
		specificEvents = new ArrayList<String>();
		for( int i=0; i<patternE.length; i++) {
			specificEvents.add(patternE[i]); 
		}
		Collections.sort(specificEvents);

		eventLabel = new JLabel("<html>Choose a specific<br>earthquake activity<br>event to load:</html>");
		activity = new JComboBox();
		loadB2 = new JButton("Load");

		// Add sorted items to view
		for( int h=0; h<specificEvents.size(); h++) {
			activity.addItem(specificEvents.get(h));
		}

		activityPanel.add(eventLabel);
		activityPanel.add(activity);
		activityPanel.add(loadB2);
		loadB2.addActionListener( this );
		center1.add( activityPanel, "Center" );
		dialog.add( center1, "Center" );

		visible = new JCheckBox("Plot", true);
		visible.addActionListener( this);
		dialog.add( visible, "South");

		JButton pmelB = new JButton(
			"<html><body><center>"
			+"Open NOAA/PMEL<br>"
			+"web page about<br>"
			+"these data"
			+"</center></body></html>");
		dialog.add( pmelB, "North");
		pmelB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				BrowseURL.browseURL(PathUtil.getPath("PORTALS/PMEL_PATH", "https://www.pmel.noaa.gov/acoustics/"));
			}
		});

	//	JButton replot = new JButton("replot");
	//	dialog.add( replot );
	//	replot.addActionListener( this);
	}
	public JComponent getSelectionDialog() {
		return dialog;
	}
	public JComponent getDataDisplay() {
		//return kountLabel;
		return scaleTime.getPanel();
	}
	public void draw( Graphics2D g ) {
		if( map==null || !visible.isSelected() ) return;
		Rectangle2D rect = map.getClipRect2D();
		float yMin = (float)rect.getY();
		float yMax = (float)(rect.getY() + rect.getHeight());
		float xMin = (float)rect.getX();
		float xMax = (float)(rect.getX() + rect.getWidth());
		float wrap = (float)map.getWrap();
		AffineTransform at = g.getTransform();
		//AffineTransform at1 = new AffineTransform();
		double zoom = map.getZoom();
		g.setStroke( new BasicStroke( 1f/(float)zoom) );
		Color blue = new Color( 0, 0, 128 );
		g.setColor( blue );
		zoom = Math.pow( zoom, .9 );
		Rectangle2D.Double arc = new Rectangle2D.Double( -1./zoom, -1./zoom, 2./zoom, 2./zoom);
	//	Arc2D.Double arc = new Arc2D.Double( -2./zoom, -2./zoom, 4./zoom, 4./zoom,
	//			0., 360., Arc2D.CHORD );
		//GeneralPath shape = new GeneralPath(arc);
		int kount=0;
		g.setColor(Color.red);
		current.removeAllElements();
		double[] range = hist.getCurrentRange();
		for( int k=0 ; k<earthquakes.size() ; k++) {
			PMELEvent eq = (PMELEvent)earthquakes.get(k);
			if( eq.time<range[0] )continue;
			if( eq.time>range[1] ) break;
			if( eq.y<yMin || eq.y>yMax ) continue;
			if( wrap>0f ) {
				while( eq.x>xMin+wrap ) eq.x -= wrap;
				while( eq.x<xMin ) eq.x += wrap;
				if( eq.x<xMax ) {
					current.add( eq );
					kount++;
					if(kount>5000)continue;
				}
				while( eq.x<xMax ) {
					g.translate( (double)eq.x, (double)eq.y );
				//	double scale = (eq.mag*.1-3.);
				//	at1.setToScale( scale, scale );
				//	Shape s = shape.createTransformedShape(at1);
				//	if( eq.dep >2500 ) g.setColor(Color.red);
				//	else if(eq.dep>500) g.setColor(Color.yellow);
				//	else g.setColor(Color.green);
					g.setColor(Color.red);
					g.fill(arc);
					g.setColor(Color.DARK_GRAY);
					g.draw(arc);
					eq.x += wrap;
					g.setTransform(at);
				}
			} else {
				if( eq.x>xMin && eq.x<xMax ) {
					current.add( eq );
					kount++;
					if(kount>5000)continue;
					g.translate( (double)eq.x, (double)eq.y );
				//	double scale = (eq.mag*.1-3.);
				//	at1.setToScale( scale, scale );
				//	Shape s = shape.createTransformedShape(at1);
				//	if( eq.dep >2500 ) g.setColor(Color.red);
				//	else if(eq.dep>500) g.setColor(Color.yellow);
				//	else g.setColor(Color.green);
					g.setColor(Color.red);
					g.fill(arc);
					g.setColor(Color.DARK_GRAY);
					g.draw(arc);
					eq.x += wrap;
					g.setTransform(at);
				}
			}
		}
		hist.reBin();
		scaleTime.repaint();
		int shown = kount;
		if( kount>5000 )shown = 5000;
		kountLabel.setText( kount +" events, "+shown+" shown");
	}
	public void actionPerformed(ActionEvent evt) {
		if( evt.getSource()==visible ) {
			map.repaint();
			return;
		}
		if( evt.getSource()==loadB) {
			// Add progress bar while retrieving list of data
			ProcessingDialog ld = new ProcessingDialog(MapApp.getApp().getFrame(), MapApp.getApp().getMap());
			ld.addTask("Retrieving Data", new Thread( new Runnable() {
			public void run() {
				for( int k=0 ; k<3 ; k++) {
					if( datasets[k].isSelected() ) {
						load(k);
						visible.setSelected(true);
						map.repaint();
						break;
					}
				}
			}
			}));
			return;
		}
		//From the sorted list of specific events, find the index from the unordered list to match the filename and load.
		if( evt.getSource()==loadB2) {
			// Add progress bar while retrieving list of data
			ProcessingDialog ld = new ProcessingDialog(MapApp.getApp().getFrame(), MapApp.getApp().getMap());
			ld.addTask("Retrieving Data", new Thread( new Runnable() {
			public void run() {
				for( int l=0 ; l<activity.getItemCount(); l++) {
					if( activity.getSelectedIndex() == l ) {
						for(int m=0; m<patternE.length; m++) {
							if(activity.getSelectedItem().equals(patternE[m])) {
								loadEvent(m);
								visible.setSelected(true);
								map.repaint();
								break;
							}
						}
						break;
					}
				}
			}
			}));
			return;
		}

		if( !visible.isSelected() )return;
	//	select();
	//	map.repaint();
	}
	
	public String getDatasetName() {
		for (JRadioButton d : datasets) {
			if (d.isSelected()) return d.getText();
		}
		return "unknown";
	}
	
	public String getActivity() {
		if (loadedEvent) {
			return activity.getSelectedItem().toString();
		}
		return null;
	}
/*
	void select() {
		try {
			float minD = 10f*Float.parseFloat( minDep.getText() );
			float maxD = 10f*Float.parseFloat( maxDep.getText() );
			float minM = 10f*Float.parseFloat( minMag.getText() );
			float maxM = 10f*Float.parseFloat( maxMag.getText() );
			int minY = Integer.parseInt( minYear.getText() );
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
			cal.set( minY, 0, 1);
			int minTime = (int)( cal.getTime().getTime()/1000L);
			int maxY = Integer.parseInt( maxYear.getText() );
			cal.set( maxY, 0, 1);
			int maxTime = (int)( cal.getTime().getTime()/1000L);
			current = new Vector(earthquakes.size());
			for( int k=0 ; k<earthquakes.size() ; k++) {
				EarthQuake eq = (EarthQuake)earthquakes.get(k);
				if( eq.time<minTime || eq.time>maxTime )continue;
				if( (float)eq.dep<minD || (float)eq.dep>maxD )continue;
				if( (float)eq.mag<minM || (float)eq.mag>maxM )continue;
				current.add( eq );
			}
			current.trimToSize();
		} catch(NumberFormatException ex) {
			JOptionPane.showMessageDialog( map.getTopLevelAncestor(), "number format exception");
		}
	}
*/
}
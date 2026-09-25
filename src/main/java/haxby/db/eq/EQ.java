package haxby.db.eq;

import haxby.db.Database;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.SceneGraph;
import haxby.util.URLFactory;
import haxby.util.SceneGraph.SceneGraphEntry;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

public class EQ implements Database, ActionListener {
	protected static Vector<EarthQuake> earthquakes;

	protected SceneGraph scene;
	protected XMap map;
	protected Vector<EarthQuake> current;
	protected Vector currentIndexInEQ;
	protected JPanel dialog;
	protected JLabel kountLabel;
	protected JTextField minMag;
	protected JTextField maxMag;
	protected JTextField minDep;
	protected JTextField maxDep;
	protected JTextField minYear;
	protected JTextField maxYear;
	protected JCheckBox visible;
	protected JPanel colorKeyPanel;
	protected JPanel panel1;
	protected JLabel colorKey1, colorKey2, colorKey3;

	protected float minD = 0;
	protected float maxD = 8000;
	protected int minTime = 50;
	protected int maxTime = 90;
	protected float minM = 50;
	protected float maxM = 90;
	{
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.set( 1964, 0, 1);
		minTime = (int)( cal.getTime().getTime()/1000L);
		cal.set( 1965, 0, 1);
		maxTime = (int)( cal.getTime().getTime()/1000L);
	}

	protected static boolean loaded=false;
	protected boolean enabled = false;

	static String EARTHQUAKE_EPICENTER_DATA = PathUtil.getPath("PORTALS/EARTHQUAKE_EPICENTER_DATA",
			MapApp.BASE_URL+"/data/portals/eq_epicent/seismicity.db");

	public EQ(XMap map) {
		this.map = map;
		earthquakes=new Vector<EarthQuake>();
		initDialog();
		kountLabel = new JLabel("0 events, 0 shown");
		kountLabel.setForeground( Color.black);
		kountLabel.setFont( new Font( "SansSerif", Font.PLAIN, 12));
	}
	public String getDBName() {
		return "Earthquake Locations and Epicenters";
	}

	public String getCommand() {
		return "ics_cmd";
	}

	public String getDescription() {
		return "Global Seismicity, 1964-1995 ISC??";
	}
	public boolean loadDB() {
		if(loaded) return true;
		earthquakes=new Vector<EarthQuake>();
		scene = new SceneGraph(map, 8);
		try {
			URL url = URLFactory.url(EARTHQUAKE_EPICENTER_DATA);
			DataInputStream in = new DataInputStream( url.openStream() );

			int i = 0;
			while( true ) try {
				int time = in.readInt();
				float lon = in.readFloat();
				float lat = in.readFloat();
				short dep = in.readShort();
				short mag = in.readShort();
				Point2D p = map.getProjection().getMapXY( new Point2D.Float(lon, lat));
				EarthQuake eq = new EarthQuake( time,
						(float)p.getX(), (float)p.getY(), dep, mag);
				earthquakes.add( eq );

				scene.addEntry( new EarthQuakeEntry(eq, i) );
				i++;
			} catch (EOFException ex) {
				earthquakes.trimToSize();
				select();
				loaded=true;
				break;
			}
		} catch(IOException ex) {
			loaded=false;
		}
		return loaded;
	}
	public boolean isLoaded() {
		return loaded;
	}
	public void unloadDB() {
		loaded = false;
	}
	public void disposeDB() {
		earthquakes.clear();
		current.clear();
		currentIndexInEQ.clear();
		scene = null;
		loaded = false;
	}
	public void setEnabled( boolean tf ) {
		return;
	}
	public boolean isEnabled() {
		return loaded;
	}
	void initDialog() {
		JPanel dialog = new JPanel( new GridLayout(0, 1) );
		JLabel label = new JLabel( "Depth range (km)");
		label.setForeground( Color.black );
		dialog.add( label);
		minDep = new JTextField("0");
		dialog.add( minDep );
		minDep.addActionListener( this);
		maxDep = new JTextField("800");
		dialog.add( maxDep );
		maxDep.addActionListener( this);

		label = new JLabel( "Magnitude range");
		label.setForeground( Color.black );
		dialog.add( label);
		minMag = new JTextField("5.0");
		dialog.add( minMag );
		minMag.addActionListener( this);
		maxMag = new JTextField("9.0");
		dialog.add( maxMag );
		maxMag.addActionListener( this);

		label = new JLabel( "Years (inclusive)");
		label.setForeground( Color.black );
		dialog.add( label);
		minYear = new JTextField("1964");
		minYear.setToolTipText("Minimum year is 1964");
		dialog.add( minYear );
		minYear.addActionListener( this);
		maxYear = new JTextField("1995");
		maxYear.setToolTipText("Maximum year is 1995");
		dialog.add( maxYear );
		maxYear.addActionListener( this);

		visible = new JCheckBox("plot", true);
		dialog.add( visible );
		visible.addActionListener( this);

		JButton replot = new JButton("replot");
		dialog.add( replot );
		replot.addActionListener( this);

		panel1 = new JPanel( new GridLayout(2, 1));
		colorKeyPanel = new JPanel( new GridLayout(0, 1));
		Border emptyBorder = BorderFactory.createEmptyBorder(1, 4, 1, 4);
		Border lineBorder = BorderFactory.createLineBorder( Color.black );
		Border titledBorder = BorderFactory.createTitledBorder( lineBorder, "Legend: Earthquake Foci" );
		Border compBorder = BorderFactory.createCompoundBorder( titledBorder, emptyBorder );
		colorKeyPanel.setBorder( compBorder );
		colorKey1 = new JLabel("<html>" + "<font color=green size=2>Shallower than 50km depth</font>" + "</html>");
		colorKey2 = new JLabel("<html>" + "<font color=#FFD700 size=2>50km to 250km depth</font>" + "</html>");
		colorKey3 = new JLabel("<html>" + "<font color=red size=2>Deeper than 250km depth" + "</html>");
		colorKeyPanel.add(colorKey1);
		colorKeyPanel.add(colorKey2);
		colorKeyPanel.add(colorKey3);
		panel1.add(colorKeyPanel);

		JButton iscB = new JButton(
				"<html><body><center>"
				+"More Info<br>"
				+"on global seismicity"
				+"</center></body></html>");
		iscB.setSize(200, 40);
		iscB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				BrowseURL.browseURL("http://www.isc.ac.uk/");
			}
		});
		panel1.add(iscB);

		//panel1.add(dialog);
		this.dialog = new JPanel( new GridLayout(2, 1));
		this.dialog.add(panel1);
		this.dialog.add( dialog);
		this.dialog.setPreferredSize(new Dimension(190, this.dialog.getHeight())); //Set Size
	}
	public JComponent getSelectionDialog() {
		return dialog;
	}
	public JComponent getDataDisplay() {
		return kountLabel;
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
		AffineTransform at1 = new AffineTransform();
		double mapZoom = map.getZoom();

		g.setStroke( new BasicStroke( 1f/(float)mapZoom) );
		Color blue = new Color( 0, 0, 128 );
		g.setColor( blue );

		double zoom = Math.pow( mapZoom, .75 );
		Arc2D.Double arc = new Arc2D.Double( -1./zoom, -1./zoom, 2./zoom, 2./zoom,
				0., 360., Arc2D.CHORD );
		GeneralPath shape = new GeneralPath(arc);

		int kount=0;
		int shown; 
		for( int k=0 ; k<current.size() ; k++) {
			EarthQuake eq = (EarthQuake)current.get(k);
			if( eq.y<yMin || eq.y>yMax ) continue;
			if( wrap>0f ) {
				while( eq.x>xMin+wrap ) eq.x -= wrap;
				while( eq.x<xMin ) eq.x += wrap;
				if( eq.x<xMax ) {
					kount++;
				}
			} else {
				if( eq.x>xMin && eq.x<xMax ) {
					kount++;
				}
			}
		}

		if (kount < 5000) // Draw all icons as a flat scene
		{
			for( int k=0 ; k<current.size() ; k++) {
				EarthQuake eq = (EarthQuake)current.get(k);
				if( eq.y<yMin || eq.y>yMax ) continue;
				if( wrap>0f ) {
					while( eq.x>xMin+wrap ) eq.x -= wrap;
					while( eq.x<xMin ) eq.x += wrap;
					while( eq.x<xMax ) {
						g.translate( (double)eq.x, (double)eq.y );
						double scale = (eq.mag*.1-3.);
						at1.setToScale( scale, scale );
						Shape s = shape.createTransformedShape(at1);
						if( eq.dep >2500 ) g.setColor(Color.red);
						else if(eq.dep>500) g.setColor(Color.yellow);
						else g.setColor(Color.green);
						g.fill(s);
						g.setColor( blue );
						g.draw( s );
						eq.x += wrap;
						g.setTransform( at);
					}
				} else {
					if( eq.x>xMin && eq.x<xMax ) {
						g.translate( (double)eq.x, (double)eq.y );
						double scale = (eq.mag*.1-3.);
						at1.setToScale( scale, scale );
						Shape s = shape.createTransformedShape(at1);
						if( eq.dep >2500 ) g.setColor(Color.red);
						else if(eq.dep>500) g.setColor(Color.yellow);
						else g.setColor(Color.green);
						g.fill(s);
						g.setColor( blue );
						g.draw( s );
						g.setTransform( at);
					}
				}
			}
			shown = kount;
		}
		else
		{
			List toDraw = scene.getEntriesToDraw(rect, mapZoom);

			for (Iterator iterator = toDraw.iterator(); iterator.hasNext();) {
				SceneGraphEntry entry = (SceneGraphEntry) iterator.next();
				EarthQuake eq = (EarthQuake) earthquakes.get( entry.getID() );

				if( wrap>0f ) {
					while( eq.x>xMin+wrap ) eq.x -= wrap;
					while( eq.x<xMin ) eq.x += wrap;
					while( eq.x<xMax ) {
						g.translate( (double)eq.x, (double)eq.y );
						double scale = (eq.mag*.1-3.);
						at1.setToScale( scale, scale );
						Shape s = shape.createTransformedShape(at1);
						if( eq.dep >2500 ) g.setColor(Color.red);
						else if(eq.dep>500) g.setColor(Color.yellow);
						else g.setColor(Color.green);
						g.fill(s);
						g.setColor( blue );
						g.draw( s );
						eq.x += wrap;
						g.setTransform( at);
					}
				} else {
					if( eq.x>xMin && eq.x<xMax ) {
						g.translate( (double)eq.x, (double)eq.y );
						double scale = (eq.mag*.1-3.);
						at1.setToScale( scale, scale );
						Shape s = shape.createTransformedShape(at1);
						if( eq.dep >2500 ) g.setColor(Color.red);
						else if(eq.dep>500) g.setColor(Color.yellow);
						else g.setColor(Color.green);
						g.fill(s);
						g.setColor( blue );
						g.draw( s );
						g.setTransform( at);
					}
				}
			}
			shown = toDraw.size();
		}
		kountLabel.setText( kount +" events, "+shown+" shown");
	}
	public void actionPerformed(ActionEvent evt) {
		if( evt.getSource()==visible ) {
			map.repaint();
			return;
		}
		if( !visible.isSelected() )return;
		select();
		map.repaint();
	}
	protected void select() {
		try {
			minD = 10f*Float.parseFloat( minDep.getText() );
			maxD = 10f*Float.parseFloat( maxDep.getText() );
			minM = 10f*Float.parseFloat( minMag.getText() );
			maxM = 10f*Float.parseFloat( maxMag.getText() );
			int minY = Integer.parseInt( minYear.getText() );
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
			cal.set( minY, 0, 1);
			minTime = (int)( cal.getTime().getTime()/1000L);
			int maxY = Integer.parseInt( maxYear.getText() );
			cal.set( maxY, 0, 1);
			maxTime = (int)( cal.getTime().getTime()/1000L);
			current = new Vector(earthquakes.size());
			currentIndexInEQ = new Vector(earthquakes.size());
			for( int k=0 ; k<earthquakes.size() ; k++) {
				EarthQuake eq = (EarthQuake)earthquakes.get(k);
				if( eq.time<minTime || eq.time>maxTime )continue;
				if( (float)eq.dep<minD || (float)eq.dep>maxD )continue;
				if( (float)eq.mag<minM || (float)eq.mag>maxM )continue;
				current.add( eq );
				currentIndexInEQ.add(new Integer(k));
			}
			current.trimToSize();
			currentIndexInEQ.trimToSize();
		} catch(NumberFormatException ex) {
			JOptionPane.showMessageDialog( map.getTopLevelAncestor(), "number format exception");
		}
	}

	private class EarthQuakeEntry implements SceneGraphEntry {
		EarthQuake eq;
		int index;
		public EarthQuakeEntry(EarthQuake eq, int index) {
			this.eq = eq;
			this.index = index;
		}

		public int getID() {
			return index;
		}

		public double getX() {
			return eq.x;
		}

		public double getY() {
			return eq.y;
		}

		public boolean isVisible() {
			if( eq.time<minTime || eq.time>maxTime ) return false;
			if( eq.dep<minD || eq.dep>maxD )return false;
			if( eq.mag<minM || eq.mag>maxM )return false;
			return true;
		}
	}
}

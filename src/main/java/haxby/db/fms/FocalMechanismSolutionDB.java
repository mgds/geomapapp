package haxby.db.fms;

import haxby.db.Database;
import haxby.db.fms.Earthquakes.Earthquake;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.SceneGraph;
import haxby.util.URLFactory;
import haxby.util.SceneGraph.SceneGraphEntry;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.geomapapp.credit.Flag;

public class FocalMechanismSolutionDB implements Database {

	protected int mapType;
	protected XMap map;

	protected SceneGraph scene;
	protected boolean isEnabled;
	protected List all;
	protected JLabel kountLabel;
	protected JTextArea dataTextArea;
	protected JPanel dataDisplay;
	protected  JPanel panelDisplay;
	protected JPanel panelRight = null;
	protected int selection;
	protected String infoURL = "https://www.globalcmt.org/";
	protected JButton info;

	protected MouseAdapter processClick = new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
			processClick(e);
		}
	};

	public FocalMechanismSolutionDB(XMap map) {
		this.map = map;
		mapType = ((MapApp) map.getApp()).getMapType();
	}

	private void createDataDisplay() {
		dataDisplay = new JPanel(new BorderLayout());
		dataDisplay.setBorder( BorderFactory.createEmptyBorder(1, 1, 1, 1));

		kountLabel = new JLabel("<html>0 events<br> 0 shown</html>");
		kountLabel.setForeground( Color.black);
		kountLabel.setFont( new Font( "SansSerif", Font.PLAIN, 12));
		kountLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
		dataDisplay.add(kountLabel, BorderLayout.WEST);

		dataTextArea = new JTextArea(" ");
		dataTextArea.setEditable(false);
		dataTextArea.setRows(2);
		dataTextArea.setBorder( BorderFactory.createEmptyBorder(1, 4, 1, 1));
		dataTextArea.setFont(dataTextArea.getFont().deriveFont(10f));
		dataDisplay.add(dataTextArea);
	}

	private void createInfoDisplay() {
		JPanel panelA = new JPanel(new FlowLayout(FlowLayout.CENTER,1,10));
		// Get start and end dates from remote file.
		URL url = null;
		String startDate = null;
		String endDate= null;
		try {
			String dateRangeURL = PathUtil.getPath("PORTALS/EARTHQUAKE_FOCAL_MECHANISMS_PATH",
					MapApp.BASE_URL+"/data/portals/eq_fms_cmt/beachball_plot/")  + "data_date_range.txt";
			url = URLFactory.url(dateRangeURL);
			BufferedReader in = new BufferedReader(new InputStreamReader( url.openStream() ));

			//if(in.ready()) {
				// Get Dataset Information
				startDate = in.readLine();
				endDate = in.readLine();
			//}
			in.close();
		} catch(Exception e) {
			System.out.println("check dates #1");
			startDate = "January 1976";
			endDate = "April 2015";
		}

		if(startDate == null || endDate == null) {
			System.err.println("check dates #2");
			startDate = "January 1976";
			endDate = "April 2015";
		}

		String dateText = "<html><br>Focal Mechanism Solutions<br>from:<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>" + startDate + "</b><br>to:<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>" + endDate + "</b><br><br></html>";
		JLabel textDate = new JLabel(dateText);
		textDate.setFont( new Font( "SansSerif", Font.PLAIN, 11));
		textDate.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Data Set Information"),
							BorderFactory.createEmptyBorder(2,4,2,4)));

		panelA.add(textDate);
		// Instructions text
		String instructText = "<html><br>Zoom in to view beach balls<br><br>Click a beach ball to display<br>CMT solution<br><br></html>";
		JLabel textInfo = new JLabel(instructText);
		textInfo.setFont( new Font( "SansSerif", Font.PLAIN, 11));
		panelA.add(textInfo);
		// Info button
		info = new JButton("Get More Info");
		try {
			info.setIcon( Flag.getInfoIcon());
		} catch(Exception e) {
		}
		panelA.add(info);

		info.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				BrowseURL.browseURL(infoURL);
			}
		});
		panelRight = new JPanel();
		panelRight.setLayout(new BoxLayout( panelRight, BoxLayout.Y_AXIS ) );
		panelRight.setPreferredSize(new Dimension(200, 200));  // set size
		panelRight.add(panelA);
	}

	public void processClick(MouseEvent evt) {
		if (!isEnabled) return;
		if (map==null) return;
		if (evt.isControlDown()) return;
		if (evt.isConsumed()||!map.isSelectable()) return;

		Rectangle2D rect = map.getClipRect2D();
		float yMin = (float)rect.getY();
		float yMax = (float)(rect.getY() + rect.getHeight());
		float xMin = (float)rect.getX();
		float xMax = (float)(rect.getX() + rect.getWidth());
		float wrap = (float)map.getWrap();
		double mapZoom = map.getZoom();
		Insets insets = map.getMapBorder().getBorderInsets(map);
		double x = (evt.getX()-insets.left) / mapZoom;
		double y = (evt.getY()-insets.top) / mapZoom;

		double zoom = mapZoom * 3;
		EarthquakeItem nearest = null;
		double dist;
		double distN = Double.MAX_VALUE; 

		if (mapZoom >= 32)
			distN = 1 / zoom * EarthquakeItem.size / 2;
		else
			distN = 4 / zoom;

		for (int i = selection + 1; i < all.size(); i++	) {
			EarthquakeItem eq = (EarthquakeItem) all.get(i);

			if( Double.isNaN(eq.x) || Double.isNaN(eq.y) ) continue;

			if ( eq.y < yMin || eq.y > yMax) continue;

			double eqX = eq.x;
			if (wrap > 0) {
				while( eqX > xMin+wrap ) eqX -= wrap;
				while( eqX < xMin ) eqX += wrap;
				if( eqX > xMax ) 
					continue;
			}

			if (wrap > 0f) {
				double x0 = 0;
				dist = Math.sqrt(Math.pow(x-eq.x, 2)+Math.pow(y-eq.y, 2));
				while (x0 < wrap) {
					x0+=wrap;
					dist = Math.min(Math.sqrt(Math.pow(x-x0-eq.x, 2)+Math.pow(y-eq.y, 2)),dist); 
				}
			} else {
				dist = Math.sqrt(Math.pow(x-eq.x, 2)+Math.pow(y-eq.y, 2));
			}

			if (dist>distN)continue;

			distN=dist;
			nearest=eq;
			selection = i;
			break;
		}

		if (nearest==null) {
			for (int i = 0;i <= selection;i++) {
				EarthquakeItem eq = (EarthquakeItem) all.get(i);

				if( Double.isNaN(eq.x) || Double.isNaN(eq.y) ) continue;

				if ( eq.y < yMin || eq.y > yMax) continue;

				double eqX = eq.x;
				if (wrap > 0) {
					while( eqX > xMin+wrap ) eqX -= wrap;
					while( eqX < xMin ) eqX += wrap;
					if( eqX > xMax ) 
						continue;
				}

				if (wrap > 0f) {
					double x0 = 0;
					dist = Math.sqrt(Math.pow(x-eq.x, 2)+Math.pow(y-eq.y, 2));
					while (x0 < wrap) {
						x0+=wrap;
						dist = Math.min(Math.sqrt(Math.pow(x-x0-eq.x, 2)+Math.pow(y-eq.y, 2)),dist); 
					}
				} else {
					dist = Math.sqrt(Math.pow(x-eq.x, 2)+Math.pow(y-eq.y, 2));
				}

				if (dist>distN)continue;

				distN=dist;
				nearest=eq;
				selection = i;
				break;
			}
		}

		if (nearest == null)
			selection = -1;

		selectionChanged();
	}

	private void selectionChanged() {
		if (selection == -1)
			dataTextArea.setText("");
		else {
			Earthquake eq = ((EarthquakeItem) all.get(selection)).eq;
			StringBuffer sb = new StringBuffer();
			// Ids will have different lengths after 2004. Account for this in spacing so it looks okay in GUI.
			String year [] = eq.date.split("/");
			Integer yearEQ = Integer.valueOf(year[0]);
			String header= "ID\t        Lat\tLon\tDate\tTime\tDepth(km)\tMb\t Ms\tMw\tNP 1 Strike\tNP 1 Dip\tNP 1 Rake\tNP 2 Strike\tNP 2 Dip\tNP 2 Rake\n";
			if(yearEQ <= 2004) {
				header = "ID\t Lat\tLon\tDate\tTime\tDepth(km)\tMb\t Ms\tMw\tNP 1 Strike\tNP 1 Dip\tNP 1 Rake\tNP 2 Strike\tNP 2 Dip\tNP 2 Rake\n";
			}
			sb.append(header);
			sb.append(eq.identifier);
			sb.append(eq.lat);
			sb.append("\t");
			sb.append(eq.lon);
			sb.append("\t");
			sb.append(eq.date);
			sb.append("\t");
			sb.append(eq.time);
			sb.append("\t");
			sb.append(eq.depth / -1000.);
			sb.append("\t");
			sb.append(eq.magnitude_body);
			sb.append("\t");
			sb.append(eq.magnitude_surface);
			sb.append("\t");
			sb.append(eq.mw);
			sb.append("\t");
			sb.append(eq.strike1);
			sb.append("\t");
			sb.append(eq.dip1);
			sb.append("\t");
			sb.append(eq.rake1);
			sb.append("\t");
			sb.append(eq.strike2);
			sb.append("\t");
			sb.append(eq.dip2);
			sb.append("\t");
			sb.append(eq.rake2);

			dataTextArea.setText(sb.toString());
			dataTextArea.select(header.length(), sb.length());
			dataTextArea.requestFocusInWindow();
		}
		map.repaint();
	}

	public void disposeDB() {
		Earthquakes.dispose();
		selection = -1;
		all.clear();
		all = null;
		scene = null;
		dataDisplay = null;
		info = null;
		panelRight = null;
		map.removeMouseListener(processClick);
	}

	public void draw(Graphics2D g) {
		if( map==null) return;

		Rectangle2D rect = map.getClipRect2D();
		float yMin = (float)rect.getY();
		float yMax = (float)(rect.getY() + rect.getHeight());
		float xMin = (float)rect.getX();
		float xMax = (float)(rect.getX() + rect.getWidth());
		float wrap = (float)map.getWrap();

		double mapZoom = map.getZoom();

		g.setStroke( new BasicStroke( 1f/(float)mapZoom) );
		AffineTransform at = g.getTransform();

		double zoom = mapZoom * 3;// Math.pow( mapZoom, 1.25 );
		Arc2D.Double arc = new Arc2D.Double( -8./zoom, -8./zoom, 16./zoom, 16./zoom,
				0., 360., Arc2D.CHORD );

		GeneralPath shape = new GeneralPath(arc);

		int kount=0;
		int shown;
		for( int k=0 ; k<all.size() ; k++) {
			EarthquakeItem eq = (EarthquakeItem)all.get(k);
			double x = eq.x;
			double y = eq.y;

			if( y < yMin || y > yMax ) continue;
			if( wrap>0f ) {
				while( x > xMin+wrap ) x -= wrap;
				while( x < xMin ) x += wrap;
				if( x < xMax )
					kount++;
			}
			else
				if( x > xMin && x < xMax )
					kount++;
		}

		// Draw flat scene
		if (kount < 5000) {
			for( int k=0 ; k<all.size() ; k++) {
				EarthquakeItem eq = (EarthquakeItem)all.get(k);
				if (k == selection) continue;

				double x = eq.x;
				double y = eq.y;

				if( y < yMin || y > yMax ) continue;
				if( wrap>0f ) {
					while( x > xMin+wrap ) x -= wrap;
					while( x < xMin ) x += wrap;
					while( x < xMax ) {
						g.translate( x, y );

						if (mapZoom >= 32)
							drawFMS(g, zoom, eq);
						else
							drawEQ(g, shape);

						x += wrap;
						g.setTransform( at);
					}
				} else {
					if( x > xMin && x < xMax ) {
						g.translate( x, y );

						if (mapZoom >= 32)
							drawFMS(g, zoom, eq);
						else
							drawEQ(g, shape);

						g.setTransform( at);
					}
				}
			}
			shown = kount;
		}
		// Use scene graph
		else {
			List toDraw = scene.getEntriesToDraw(rect, zoom);

			for (Iterator iterator = toDraw.iterator(); iterator.hasNext();) {
				SceneGraphEntry entry = (SceneGraphEntry) iterator.next();
				if (entry.getID() == selection) continue;

				EarthquakeItem eq = (EarthquakeItem) all.get( entry.getID() );

				double x = eq.x;
				double y = eq.y;

				if( y < yMin || y > yMax ) continue;
				if( wrap>0f ) {
					while( x > xMin+wrap ) x -= wrap;
					while( x < xMin ) x += wrap;
					while( x < xMax ) {
						g.translate( x, y );

						if (mapZoom >= 32)
							drawFMS(g, zoom, eq);
						else
							drawEQ(g, shape);

						x += wrap;
						g.setTransform( at);
					}
				} else {
					if( x > xMin && x < xMax ) {
						g.translate( x, y );
						
						if (mapZoom >= 32)
							drawFMS(g, zoom, eq);
						else
							drawEQ(g, shape);
						
						g.setTransform( at);
					}
				}
			}
			shown = toDraw.size();
		}
		// Draw selection
		if (selection != -1) {
			EarthquakeItem eq = (EarthquakeItem) all.get(selection);

			double x = eq.x;
			double y = eq.y;

			if(! (y < yMin || y > yMax )) {
				if( wrap>0f ) {
					while( x > xMin+wrap ) x -= wrap;
					while( x < xMin ) x += wrap;
					if( x < xMax ) {
						kount++;
					}
					while( x < xMax ) {
						g.translate( x, y );

						if (mapZoom >= 32) {
							drawFMS(g, zoom, eq);

							g.setColor(Color.RED);
							Stroke s = g.getStroke();
							g.setStroke(new BasicStroke(4));
							g.drawArc(0, 0, eq.size, eq.size, 0, 360);
							g.setStroke(s);
						}
						else {
							drawEQ(g, shape);
							g.setColor(Color.RED);
							g.draw(arc);
						}
						x += wrap;
						g.setTransform( at);
					}
				} else {
					if( x > xMin && x < xMax ) {
					//
						kount++;
						g.translate( x, y );
						if (mapZoom >= 32) {
							drawFMS(g, zoom, eq);

							g.setColor(Color.RED);
							Stroke s = g.getStroke();
							g.setStroke(new BasicStroke(4));
							g.drawArc(0, 0, eq.size, eq.size, 0, 360);
							g.setStroke(s);
						}
						else {
							drawEQ(g, shape);
							g.setColor(Color.RED);
							g.draw(arc);
						}
						g.setTransform( at);
					}
				}
			}
		}
		if(kountLabel!=null)
			kountLabel.setText("<html>" + shown +" events<br> "+ kount +" shown</html>");
	}

	private void drawFMS(Graphics2D g, double zoom, EarthquakeItem eq) {
		g.scale(1. / zoom, 1. / zoom);
		BufferedImage img = eq.getFMSImage();

		switch (mapType) {
		case MapApp.SOUTH_POLAR_MAP:
			g.rotate( Math.toRadians( eq.eq.lon )) ;
			break;
		case MapApp.NORTH_POLAR_MAP:
			g.rotate( -Math.toRadians( eq.eq.lon )) ;
			break;
		default:
			break;
		}
		g.translate(-img.getWidth() / 2, -img.getHeight() / 2);
		g.drawImage(img, null, null);
	}

	private void drawEQ(Graphics2D g, GeneralPath shape) {
		g.setColor(Color.green);
		g.fill(shape);
		g.setColor( Color.blue );
		g.draw( shape );
	}

	public JComponent getDataDisplay() {
		if (dataDisplay == null) createDataDisplay();
		return dataDisplay;
	}

	public String getDBName() {
		return "EQ Focal Mechanism Solutions";
	}

	public String getCommand() {
		return "cmt_cmd";
	}

	public String getDescription() {
		return "EQ FMS";
	}

	public JComponent getSelectionDialog() {
		if (panelRight == null) {
			createInfoDisplay();
		}
		return panelRight;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public boolean isLoaded() {
		return all != null && !all.isEmpty();
	}
	
	public void unloadDB() {
		all = null;
	}
	
	public boolean loadDB() {
		if (isLoaded()) return true;

		if (!Earthquakes.load()) return false;

		scene = new SceneGraph(map, 10000);

		List eqs = Earthquakes.getEQs(); 

		all = new ArrayList(eqs.size());

		int i = 0;

		Iterator iter = eqs.iterator();
		while (iter.hasNext()) {
			Earthquake eq = (Earthquake) iter.next();
			EarthquakeItem eqItem = new EarthquakeItem(eq);
			all.add(eqItem);

			scene.addEntry(new EarthquakeEntry(eqItem, i));
			i++;
		}
		map.addMouseListener(processClick);

		return true;
	}

	public void setEnabled(boolean tf) {
		this.isEnabled = tf;
		map.removeMouseListener(processClick);
		if (tf)
			map.addMouseListener(processClick);
	}

	private class EarthquakeEntry implements SceneGraphEntry {
		private EarthquakeItem eq;
		private int index;

		public EarthquakeEntry(EarthquakeItem eq, int index) {
			this.index = index;
			this.eq = eq;
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
			return true;
		}
	}

	public class EarthquakeItem {
		public static final int size = 60;
		public Earthquake eq;
		public BufferedImage fms;
		public double x, y;

		public EarthquakeItem(Earthquake eq) {
			this.eq = eq;

			Point2D p = map.getProjection().getMapXY( eq.lon , eq.lat );
			x = p.getX();
			y = p.getY();
		}

		public BufferedImage getFMSImage() {
//			if (fms == null) drawFMSImage();
//			return fms;
			return drawFMSImage();
		}

		private BufferedImage drawFMSImage() {
			BufferedImage fms = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

			Graphics2D g = fms.createGraphics();
			Composite c = g.getComposite();

			g.setComposite(AlphaComposite.getInstance(
							AlphaComposite.CLEAR, 0));
			g.fillRect(0, 0, size, size);
			g.setComposite(c);

			g.translate(size / 2, size / 2);
			FMSDraw.drawFMS(g, size / 2,
					eq.strike1,
					eq.dip1, 
					eq.rake1, 
					eq.strike2, 
					eq.dip2, 
					eq.rake2);
			return fms;
		}
	}
}
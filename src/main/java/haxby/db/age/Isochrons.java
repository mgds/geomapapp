package haxby.db.age;

import haxby.db.Database;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.nav.ControlPt;
import haxby.nav.Nearest;
import haxby.nav.TrackLine;
import haxby.proj.Projection;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.geomapapp.util.XML_Menu;

public class Isochrons implements Database,
				MouseListener,
				ActionListener {
	XMap map;
	Vector isochrons;
	JPanel selectDialog;
	JPanel display;
	JCheckBox plotCB;
	JLabel infoLabel;
	boolean enabled;
	boolean loaded;
	Vector timeScale;
	Color blue;

	String ISOCRONS_PATH = PathUtil.getPath("PORTALS/ISOCRONS_PATH",
			MapApp.BASE_URL+"/data/portals/isochrons/");

	public Isochrons( XMap map ) {
		this.map = map;
		isochrons = new Vector();
		timeScale = new Vector();
		enabled = false;
		loaded = false;
		selectDialog = null;
		display = null;
		plotCB = new JCheckBox("plot isochrons");
		plotCB.setSelected(true);
		infoLabel = new JLabel(getDescription());
		blue = new Color( 0, 0, 120 );
	}
	public String getDBName() {
		return "Seafloor Magnetic Anomalies";
	}

	public String getCommand() {
		return "magnetic_anomaly_cmd";
	}

	public String getDescription() {
		return "Magnetic anomaly isochrons from Muller, 1997";
	}
	public boolean loadDB() {
		if( loaded ) return loaded;
		try {
			URL url = URLFactory.url(ISOCRONS_PATH + "timescale");
			DataInputStream in = new DataInputStream( url.openStream() );
			timeScale = new Vector();
			while( true ) {
				try {
					int anomaly = in.readInt();
					int age = (int)Math.rint( 1000f * in.readFloat() );
					timeScale.add( new int[] { anomaly, age } );
				} catch (EOFException ex) {
					break;
				}
			}
			in.close();
			url = URLFactory.url(ISOCRONS_PATH + "isochrons");
			in = new DataInputStream( url.openStream() );
			int k=0;
			Point2D.Float pt = new Point2D.Float();
			double wrap = map.getWrap();
			double wraptest = wrap/2.;
			Projection proj = map.getProjection();
			String name = "";
			Dimension mapDim = map.getDefaultSize();
			int anom;
			while( true ) {
				try {
					anom = (int)in.readShort();
				} catch(EOFException ex) {
					break;
				}
				short plate = in.readShort();
				short conjugate = in.readShort();
				int size = (int)in.readShort();
				Isochron chron = new Isochron( plate, conjugate, anom);
				for( k=0 ; k<size ; k++) {
					int n = (int)in.readShort();
					if( n==0 ) continue;
					chron.add( in.readFloat(), in.readFloat(), false);
					for( int i=1 ; i<n ; i++ ) {
						chron.add( in.readFloat(), in.readFloat(), true);
					}
				}
				chron.finish();
				Rectangle2D.Double bounds = new Rectangle2D.Double();
				double xtest = 0d;
				Vector segs = chron.getSegs();
				int nseg = segs.size();
				ControlPt.Float[][] cpt = new ControlPt.Float[nseg][];
				for( int i=0 ; i<nseg ; i++) {
					Vector seg = (Vector)segs.get(i);
					cpt[i] = new ControlPt.Float[seg.size()];
					for( int j=0 ; j<cpt[i].length ; j++) {
						float[] xy = (float[])seg.get(j);
						pt.x = xy[0];
						pt.y = xy[1];
						Point2D.Double p = (Point2D.Double)proj.getMapXY(pt);
						if(j==0&&i==0) {
							bounds.x = p.x;
							bounds.y = p.y;
							bounds.width = 0.;
							bounds.height = 0.;
							xtest = p.x;
						} else {
							if( wrap>0.) {
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
						cpt[i][j] = new ControlPt.Float((float)p.x, (float)p.y );
					}
				}
				if( !bounds.intersects(0., 0.,
						mapDim.getWidth(),
						mapDim.getHeight()) ) continue;
				if( Math.max(bounds.getHeight(), bounds.getWidth()) > 1.e5 ) continue;
				bounds.x -= 1.;
				bounds.y -= 1.;
				bounds.width += 2.;
				bounds.height += 2.;
				name = (anom>=100) ?
					"M"+(anom-100) :
					Integer.toString(anom);
				chron.setNav( new TrackLine(name, bounds, cpt, 0, 0, (byte)0, (int)wrap) );
				isochrons.add( chron );
			}
		} catch ( IOException ex ) {
			loaded = false;
			ex.printStackTrace();
			return loaded;
		}
		loaded = true;
		isochrons.trimToSize();
		return true;
	}
	public boolean isLoaded() {
		return loaded;
	}
	public void unloadDB() {
		loaded = false;
	}
	public void disposeDB() {
		setEnabled( false );
		map.removeMouseListener( this );
		isochrons = new Vector();
		loaded = false;
		System.gc();
	}
	public void setEnabled( boolean tf ) {
		if( tf && enabled ) return;
		map.removeMouseListener( this );
		map.addMouseListener(this);
		enabled = tf;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public JComponent getSelectionDialog() {
		if( selectDialog==null ) {
			selectDialog = new JPanel( new FlowLayout(FlowLayout.CENTER,1,1));
			selectDialog.setPreferredSize(new Dimension (200, 250));
			plotCB.addActionListener( this );
			JButton button = new JButton(
				"<html><body><center>"
				+"Open web page<br>"
				+"about isochrons"
				+"</center></body></html>");
			selectDialog.add(button);
			button.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					BrowseURL.browseURL("https://www.earthbyte.org/Resources/Agegrid/1997/digit_isochrons.html");
				}
			});
			selectDialog.add( plotCB);
		}
		return selectDialog;
	}
	public JComponent getDataDisplay() {
		if( display==null ) {
			display = new JPanel( new BorderLayout() );
			display.add( infoLabel, "South" );
		}
		return display;
	}
	public void draw( Graphics2D g ) {
		if( !plotCB.isSelected() ) return;
		if( !loaded ) return;
		g.setColor( blue );
		g.setStroke( new BasicStroke( 1f/(float)map.getZoom()) );
		for( int k=0 ; k<isochrons.size() ; k++) {
			Isochron chron = (Isochron)isochrons.get(k);
			chron.nav.draw( g );
		}
		drawSelectedIndex( g, true );
	}
	public void actionPerformed( ActionEvent evt ) {
		if( evt.getSource() == plotCB ) {
			if( plotCB.isSelected() ) {
				synchronized ( map.getTreeLock() ) {
					draw( map.getGraphics2D() );
				}
			} else {
				map.repaint();
			}
		}
	}
	public void mouseEntered( MouseEvent evt ) {
	}
	public void mouseExited( MouseEvent evt ) {
	}
	public void mousePressed( MouseEvent evt ) {
	}
	public void mouseReleased( MouseEvent evt ) {
	}
	public void mouseClicked( MouseEvent evt ) {
		if(evt.isControlDown())return;
		if( !plotCB.isSelected() ) return;
		double zoom = map.getZoom();
		Nearest nearest = new Nearest(null, 0, 0, 2./zoom );
		Insets insets = map.getMapBorder().getBorderInsets(map);
		double x = (evt.getX()-insets.left)/zoom;
		double y = (evt.getY()-insets.top)/zoom;
		synchronized (map.getTreeLock()) {
			drawSelectedIndex(map.getGraphics2D(), false);
		}
		selectedIndex = -1;
		for( int i=0 ; i<isochrons.size() ; i++ ) {
			Isochron chron = (Isochron)isochrons.get(i);
			TrackLine nav = chron.nav;
			if( !nav.contains( x, y ) ) continue;
			if( nav.firstNearPoint(x, y, nearest) ) {
				selectedIndex = i;
				synchronized (map.getTreeLock()) {
					drawSelectedIndex(map.getGraphics2D(), true);
				}
				return;
			}
		}
		return;
	}
	int selectedIndex = -1;
	void drawSelectedIndex( Graphics2D g, boolean tf ) {
		if( selectedIndex==-1 ) {
			infoLabel.setText( getDescription() );
			return;
		} else if( tf ) {
			g.setColor( Color.white );
			g.setStroke( new BasicStroke( 1f/(float)map.getZoom()) );
			Isochron chron = (Isochron)isochrons.get(selectedIndex);
			int anom = (int) chron.anom;
			String name = (chron.anom>100) ? "M"+(anom-100) : Integer.toString(anom);
			double age=0.;
			for( int k=0 ; k<timeScale.size() ; k++ ) {
				int[] a = (int[])timeScale.get(k);
				if( a[0]==anom ) {
					age = .001 * a[1];
					break;
				}
			}
			NumberFormat fmt = NumberFormat.getInstance();
			infoLabel.setText( "Anomaly "+ name +", Age (millions of years) = " + fmt.format(age) );
			chron.nav.draw( g );
		} else {
			g.setColor( blue );
			g.setStroke( new BasicStroke( 1f/(float)map.getZoom()) );
			Isochron chron = (Isochron)isochrons.get(selectedIndex);
			infoLabel.setText( getDescription() );
			chron.nav.draw( g );
		}
	}
}
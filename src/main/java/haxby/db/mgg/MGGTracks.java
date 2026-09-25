package haxby.db.mgg;

import haxby.db.*;
import haxby.map.*;
import haxby.proj.*;
import haxby.nav.*;
import haxby.util.*;

import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;

public class MGGTracks implements Database, 
			ListSelectionListener,
			MouseListener  {
	XMap map;
	MGGTrack[] tracks;
	MGGTableModel model;
	XBTable table;
	JScrollPane tableSP;
	int selectedIndex;
	int size;
	byte types;
	MGGSelection mggSel;
	boolean enabled;
	boolean loaded;

	static String MGD77_PATH = PathUtil.getPath("PORTALS/MGD77_PATH",
			MapApp.BASE_URL+"/data/portals/mgd77/");

	public MGGTracks( XMap map, int size ) {
		this.map = map;
		tracks = new MGGTrack[size];
		this.size = 0;
		types = (byte)0x7;
		selectedIndex = -1;
		mggSel = new MGGSelection(this);
		enabled = false;
		loaded = false;
		model = new MGGTableModel(this);
		table = new XBTable( model );
		tableSP = new JScrollPane(table);
		table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(this);
	}
	public boolean isLoaded() {
		return loaded;
	}
	public void unloadDB() {
		loaded = false;
	}
	public void disposeDB() {
		tracks = null;
		loaded = false;
	}
	public boolean loadDB() {
		System.out.println("y");
		if( loaded ) return true;
		try {
			int nPer360 = 20480;
			//String dir = "/scratch/ridgembs/bill/kn166/java/src/mapapp/MGG"; //old
			// Not in use
			//URL url = URLFactory.url(haxby.map.MapApp.TEMP_BASE_URL + "data/mgg/mgg_control_20480");
			URL url = URLFactory.url(MGD77_PATH + "mb_control327680"); // not in use, point it to correct area anyway.
			DataInputStream in = new DataInputStream(
					new BufferedInputStream(url.openStream()));
			int k=0;
			Point2D.Double pt = new Point2D.Double();
			double wrap = map.getWrap();
			double wraptest = wrap/2.;
			double xtest = 0d;
			Mercator proj = (Mercator) map.getProjection();
			String name = "";
			Dimension dim = map.getDefaultSize();
			Rectangle mapBounds = new Rectangle(0, 0, dim.width, dim.height );
			while( true ) {
				try {
					name = in.readUTF();
				} catch (EOFException ex) {
					break;
				}
				int nseg = in.readInt();
				ControlPt.Float[][] cpt = new ControlPt.Float[nseg][];
				byte types = 0;
				if( in.readInt() != 0 ) types |= (byte)0x4;
				if( in.readInt() != 0 ) types |= (byte)0x2;
				if( in.readInt() != 0 ) types |= (byte)0x1;
				int start = in.readInt();
				int end = in.readInt();
				Rectangle2D.Double bounds = new Rectangle2D.Double();
				for( int i=0 ; i<nseg ; i++) {
					cpt[i] = new ControlPt.Float[in.readInt()];
					for( int j=0 ; j<cpt[i].length ; j++) {
						pt.x = 1.e-6*(double)in.readInt();
						pt.y = 1.e-6*(double)in.readInt();
						Point2D.Double p = (Point2D.Double)proj.getMapXY(pt);
						if(j==0&&i==0) {
							bounds.x = p.x;
							bounds.y = p.y;
							bounds.width = 0.;
							bounds.height = 0.;
							xtest = p.x;
						} else {
							if( wrap>0. ) {
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
						cpt[i][j] = new ControlPt.Float(
								(float)p.x, (float)p.y );
					}
				}
				if( !mapBounds.intersects( bounds.x, 
							bounds.y, 
							bounds.width, 
							bounds.height) ) continue;
				add( new MGGTrack( 
						new TrackLine( name, bounds, cpt , start, end, types, (int)wrap)));
			}
		} catch ( IOException ex ) {
			loaded = false;
			ex.printStackTrace();
		}
		loaded = true;
		trim();
		model.clearTracks();
		for( int i=0 ; i<tracks.length ; i++) {
			model.addTrack(tracks[i], i);
		}
		return loaded;
	}
	public void add(MGGTrack track) {
		if(size==tracks.length) {
			MGGTrack[] tmp = new MGGTrack[size+10];
			System.arraycopy(tracks, 0, tmp, 0, size);
			tracks = tmp;
		}
		tracks[size++] = track;
	}
	void trim() {
		if( size< tracks.length ) {
			MGGTrack[] tmp = new MGGTrack[size];
			System.arraycopy(tracks, 0, tmp, 0, size);
			tracks = tmp;
		}
	}
	public String getDBName() {
		return "underway MG&G";
	}

	public String getCommand() {
		return "underway MG&G";
	}

	public String getDescription() {
		return "Depth, free-air gravity anomalies and magnetic anomalies\n"
			+"measured by research ships";
	}
	public JComponent getSelectionDialog() {
		return mggSel.getDialog();
	}
	public JComponent getDataDisplay() {
		return tableSP;
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
		model.clearTracks();
		if( types==0 ) return;
		Stroke stroke = g.getStroke();
		g.setStroke( new BasicStroke( 1f/(float)map.getZoom() ));
		Rectangle2D rect = map.getClipRect2D();
		if(enabled) {
			g.setColor(Color.black);
		} else {
			g.setColor(Color.gray);
		}
		for( int i=0 ; i<size ; i++) {
			if( (types & tracks[i].getTypes()) ==0)continue;
			if( !tracks[i].intersects(rect)) continue;
			tracks[i].draw(g);
			model.addTrack(tracks[i], i);
		}
		if(selectedIndex>=0 && selectedIndex<size) {
			if(enabled) {
				g.setColor(Color.white);
			} else {
				g.setColor(Color.lightGray);
			}
			tracks[selectedIndex].draw(g);
		}
		model.fireTableStructureChanged();
		g.setStroke(stroke);
	}
	public void setTypes(boolean topo, boolean grav, boolean mag) {
		types = 0;
		if(topo) types |= (byte)0x4;
		if(grav) types |= (byte)0x2;
		if(mag)  types |= (byte)0x1;
		selectedIndex = -1;
		map.repaint();
	}
	void drawSelectedTrack( Color color ) {
		if(selectedIndex<0 || selectedIndex>=size) return;
		synchronized (map.getTreeLock()) {
			Graphics2D g = map.getGraphics2D();
			g.setStroke( new BasicStroke( 1f/(float)map.getZoom() ));
			g.setColor(color);
			tracks[selectedIndex].draw(g);
		}
	}
	public void mousePressed(MouseEvent evt) {
	}
	public void mouseReleased( MouseEvent evt) {
	}
	public void mouseClicked( MouseEvent evt) {
		if(evt.isControlDown())return;
		double zoom = map.getZoom();
		Nearest nearest = new Nearest(null, 0, 0, 2./zoom );
		Insets insets = map.getMapBorder().getBorderInsets(map);
		double x = (evt.getX()-insets.left)/zoom;
		double y = (evt.getY()-insets.top)/zoom;
		boolean back = evt.isShiftDown();
		int i0 = selectedIndex;
		while( i0<0 ) i0+=size;
		if( back ) i0+=size;
		for( int k=0 ; k<size ; k++) {
			int i = back ?
				(i0 - (1+k))%size :
				(i0 + 1+k )%size;
			if( (types & tracks[i].getTypes()) ==0)continue;
			if( !tracks[i].contains(x, y) ) continue;
			if( tracks[i].firstNearPoint(x, y, nearest) ) {
				if(i==selectedIndex)return;
				int index = model.indexOf(tracks[i]);
				if( index<0 ) {
					drawSelectedTrack(Color.black);
					selectedIndex = -1;
					return;
				}
				table.setRowSelectionInterval(index, index);
				table.ensureIndexIsVisible( index);
				return;
			}
		}
		if(selectedIndex==-1)return;
		drawSelectedTrack(Color.black);
		selectedIndex = -1;
	}
	public void valueChanged(ListSelectionEvent e) {
	//	if(e.getSource()==table) {
			int i = table.getSelectedRow();
			if(i!=-1) {
				i = model.indexOf( i );
			}
			if(i==selectedIndex)return;
			drawSelectedTrack(Color.black);
			selectedIndex = i;
			drawSelectedTrack(Color.white);
			return;
	//	}
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
}
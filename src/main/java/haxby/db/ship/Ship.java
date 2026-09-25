
package haxby.db.ship;

import haxby.db.*;
import haxby.db.mgg.MGGTrack;
import haxby.map.*;
import haxby.proj.*;
import haxby.nav.*;
import haxby.util.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.JTableHeader;

public class Ship implements Database, 
			ListSelectionListener,
			MouseListener {
	protected XMap map;
	protected ShipTrack[] tracks;
	protected ShipListModel model;
	protected ShipDataDisplay display;
	protected JList list;
	protected int selectedIndex;
	protected int dataIndex;
	protected int size;
	protected byte types;
	protected ShipSelector shipSel;
	protected boolean enabled;
	protected boolean loaded;
	protected ShipTracks shipTracks;
	protected ShipTableModel shipTableModel;
	
	protected int in_view;
	
	static String SHIP_PATH = PathUtil.getPath("PORTALS/SHIP_PATH",
			MapApp.BASE_URL+"/data/portals/ship/");
	static String SHIP_PATH_CONTROL = PathUtil.getPath("PORTALS/SHIP_PATH_CONTROL",
			MapApp.BASE_URL+"/data/portals/ship/control/");

	
	
	public Ship( XMap map, int size ) {
		this.map = map;
		tracks = new ShipTrack[size];
		this.size = 0;
		types = (byte)0x7;
		selectedIndex = -1;
		dataIndex = -1;
		
		enabled = false;
		loaded = false;
		model = new ShipListModel(this);
		createDataDisplay();
		list = display.cruiseL;
		
		
		
		shipSel = new ShipSelector(this);
		shipTracks = new ShipTracks(map,size);
		shipTableModel = new ShipTableModel(shipTracks);
		shipTracks.model = shipTableModel;
		
		shipTracks.table = new XBTable(shipTableModel);
		
		shipTracks.initTable();
		shipTracks.dataT.getSelectionModel().addListSelectionListener(this);
		shipTracks.dataT.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		shipTracks.dataT.setRowSelectionAllowed(true);
		shipTracks.dataT.setColumnSelectionAllowed(false);
		//shipTracks.dataT.setAutoCreateRowSorter(true);//TODO fix this
		shipTracks.dataT.getTableHeader().addMouseListener( new MouseListener(){
			public void mouseClicked(MouseEvent e) {
				// TODO Auto-generated method stub
				list.clearSelection();
				shipTracks.dataT.clearSelection();
				int c = shipTracks.dataT.getTableHeader().columnAtPoint(e.getPoint());
				
				
				shipTableModel.sortByColumn(c);
				//tracks = shipTracks.tracks;
			}

			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub
			}

			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub
			}

			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub
			}

			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub
			}

			});
		
		shipTracks.dataT.addMouseListener(new MouseListener(){

			public void mouseClicked(MouseEvent e) {
				Point pt = e.getPoint();
				int ccol = shipTracks.dataT.columnAtPoint(pt);
				if(ccol==6) { 
					int crow = shipTracks.dataT.rowAtPoint(pt);

					if(crow<0)
						return;

					String url = (String)shipTracks.dataT.getValueAt(crow, ccol);

					try{
						BrowseURL.browseURL(url);
					}catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			}

			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
		});
	}

	protected void createDataDisplay() {
		display = new ShipDataDisplay( this, map );
	}

	public boolean isLoaded() {
		return loaded;
	}

	public void unloadDB() {
		loaded = false;
	}
	
	public void disposeDB() {
		tracks = new ShipTrack[size];
		size = 0;
		dataIndex = -1;
		loaded = false;

//		1.4.4: Clear tracks to free up space (control files can be quite large)
		model.clearTracks();
		
	}

	public boolean loadDB() {
		if( loaded ) return true;

		shipSel.loadDB();
		loaded =true;
		return loaded;
	}

	protected boolean isValidBounds(Rectangle2D bounds) {
		Dimension mapDim = map.getDefaultSize();
		return bounds.intersects(0., 0., mapDim.getWidth(), mapDim.getHeight());
	}

	public void add(ShipTrack track) {
		if(size==tracks.length) {
			ShipTrack[] tmp = new ShipTrack[size+10];
			System.arraycopy(tracks, 0, tmp, 0, size);
			tracks = tmp;
		}
		tracks[size++] = track;
	}

	void trim() {
		if( size< tracks.length ) {
			ShipTrack[] tmp = new ShipTrack[size];
			System.arraycopy(tracks, 0, tmp, 0, size);
			tracks = tmp;
		}
	}

	public String getDBName() {
		return "Search Expedition Data";
	}

	public String getCommand() {
		return "ship_cmd";
	}

	public String getDescription() {
		return "Ship data";
	}

	public JComponent getSelectionDialog() {
		return shipSel.getDialog();		
	}

	public JComponent getDataDisplay() {
		return shipTracks.tableSP;
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
		shipTracks.draw(g);
		if( !loaded ) return;
		if(display.data != null)display.data.currentPoint = null;
		Stroke stroke = g.getStroke();
		if( types==0 ) {
			g.setStroke( new BasicStroke( 1f/(float)map.getZoom() ));
			if(enabled && display.data != null) display.data.draw(g);
			g.setStroke(stroke);
			return;
		}
		g.setStroke( new BasicStroke( 1f/(float)map.getZoom() ));
		Rectangle2D rect = map.getClipRect2D();

		int temp = selectedIndex;

		model.clearTracks();
		shipTableModel.clearTracks();
		selectedIndex = temp;

		if(enabled) {
			g.setColor(Color.black);
		} else {
			g.setColor(Color.darkGray);
		}
		/*int*/ in_view = 0;
		for( int i=0 ; i<size ; i++) {
			if(tracks[i]!=null){
			shipTracks.dataT.clearSelection();
			
			if( !(tracks[i].intersects(rect))) continue;
			if( i==dataIndex && display.data != null) display.data.draw(g);
			else tracks[i].draw(g);
			rect = map.getClipRect2D();
			
			model.addTrack(tracks[i], i);
			shipTableModel.addTrack(tracks[i], in_view);
			in_view++;	
		}
		}
		model.updateList();
		shipTableModel.fireTableStructureChanged();
		if(enabled && display.data != null) display.data.draw(g);
		if((selectedIndex>=0) && (selectedIndex<size)) {
			if(enabled) {
				g.setColor(Color.white);
			} else {
				g.setColor(Color.lightGray);
			}
			if( selectedIndex==dataIndex ) display.data.draw(g);
			else tracks[selectedIndex].draw(g);
		}


		selectedIndex = temp;

		in_view--;

		g.setStroke(stroke);
		if( enabled && selectedIndex!=-1 && in_view!=-1) {
			int index = model.indexOf(tracks[selectedIndex]);
			synchronized( map.getTreeLock() ) {
				shipTracks.dataT.setRowSelectionInterval(in_view, in_view);
				shipTracks.dataT.ensureIndexIsVisible(in_view);
				list.setSelectedIndex(index);
				list.ensureIndexIsVisible(index);
			}
			//list.setSelectedValue( tracks[selectedIndex], true );
		}
	}


	void drawSelectedTrack( Color color ) {
		shipTracks.drawSelectedTrack(color);
		if(selectedIndex<0 || selectedIndex>=size) return;
		synchronized (map.getTreeLock()) {
			Graphics2D g = map.getGraphics2D();
			g.setStroke( new BasicStroke( 1f/(float)map.getZoom() ));
			g.setColor(color);
			if( selectedIndex==dataIndex ) display.data.draw(g);
			else tracks[selectedIndex].draw(g);
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
			if(tracks[i]!=null){	
			//if( (types & tracks[i].getTypes()) ==0)continue;
			if( !tracks[i].contains(x, y) ) continue;
			if( tracks[i].firstNearPoint(x, y, nearest) ) {
				
			//	if(i==selectedIndex)return;
				int index = shipTableModel.indexOf(tracks[i]);
				if( index<0 ) break;
			//		drawSelectedTrack(Color.black);
			//		selectedIndex = -1;
			//		return;
			//	}
			//	list.setSelectedValue(tracks[i], true);
			//	selectedIndex = i;
				shipTracks.dataT.setRowSelectionInterval(index, index);
				shipTracks.dataT.scrollRectToVisible(shipTracks.dataT.getCellRect(index,0, true));
				list.setSelectedIndex(index);
				list.ensureIndexIsVisible(index);

				return;}
			}
		}
		if(selectedIndex==-1)return;
		drawSelectedTrack(Color.black);
		list.clearSelection();
		selectedIndex = -1;
	}

	public void valueChanged(ListSelectionEvent e) {
		int i = shipTracks.dataT.getSelectedRow();

		if(i!=-1) {
			i = model.indexOf( i );
		}
		drawSelectedTrack(Color.black);
		selectedIndex = i;
		drawSelectedTrack(Color.white);
		return;
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

	public void newTrackSet(String loadedControlFiles) {
	}
}
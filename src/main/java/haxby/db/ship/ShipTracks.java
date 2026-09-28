package haxby.db.ship;

import haxby.db.*;
import haxby.db.custom.HyperlinkTableRenderer;


import haxby.map.*;
import haxby.proj.*;
import haxby.nav.*;
import haxby.util.*;

import java.net.*;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;


public class ShipTracks implements Database, ListSelectionListener, MouseListener
{
	XMap map;
	ShipTrack[] tracks;
	ShipTableModel model;
	XBTable table;
	JScrollPane tableSP;
	int selectedIndex;
	int size;
	byte types;
	ShipSelection shipSel;
	boolean enabled;
	boolean loaded;
	XBTableExtension dataT;

	static String SHIP_PATH = PathUtil.getPath("PORTALS/SHIP_PATH",
			MapApp.BASE_URL+"/data/portals/ship/");

	static String SHIP_PATH_CONTROL = PathUtil.getPath("PORTALS/SHIP_PATH_CONTROL",
			MapApp.BASE_URL+"/data/portals/ship/control/");
	
	public ShipTracks(XMap map, int size)
	{
		this.map = map;
		tracks = new ShipTrack[size];
		this.size = 0;
		types = (byte)0x7;
		selectedIndex = -1;
		shipSel = new ShipSelection(this);
		enabled = false;
		loaded = false;
		model = new ShipTableModel(this);
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

	static final class XBTableExtension extends XBTable {
		TableCellRenderer renderer = new HyperlinkTableRenderer();

		private XBTableExtension(TableModel model) {
			super(model);
		}		

		public javax.swing.table.TableCellRenderer getCellRenderer(int row,
				int column) {
			
			return renderer;
		}
	}
	
	public void initTable(){
		dataT =  new XBTableExtension(model);
		dataT.setAutoscrolls(true);		
		dataT.setScrollableTracksViewportWidth(false);	
		dataT.setFillsViewportHeight(true);
		tableSP = new JScrollPane(dataT);		
	}
	
	
	public boolean loadDB()
	{
		return true;
	}
		
	void trim() {
		if( size< tracks.length ) {
			ShipTrack[] tmp = new ShipTrack[size];
			System.arraycopy(tracks, 0, tmp, 0, size);
			tracks = tmp;
		}
	}
	
	public void add(ShipTrack track) {
		/*
		if(size==tracks.length) {
			ShipTrack[] tmp = new ShipTrack[size+10];
			System.arraycopy(tracks, 0, tmp, 0, size);
			tracks = tmp;
		}
		tracks[size++] = track;*/
	}
	
	public JComponent getSelectionDialog() {
		return shipSel.getDialog();
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
			//if( (types & tracks[i].getTypes()) ==0)continue;
			//if( !tracks[i].intersects(rect)) continue;
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
				(i0 + (1+k ))%size;
			//if( (types & tracks[i].getTypes()) ==0)continue;
			if( !tracks[i].contains(x, y) ) continue;
			if( tracks[i].firstNearPoint(x, y, nearest) ) {
				
			//	if(i==selectedIndex)return;
				int index = model.indexOf(tracks[i]);
				if( index<0 ) break;
			//		drawSelectedTrack(Color.black);
			//		selectedIndex = -1;
			//		return;
			//	}
			//	list.setSelectedValue(tracks[i], true);
			//	selectedIndex = i;
				table.setRowSelectionInterval(index, index);
				//table.setSelectedIndex(index);
				//list.ensureIndexIsVisible(index);		
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

	public String getCommand() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getDBName() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

}

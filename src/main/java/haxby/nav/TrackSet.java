package haxby.nav;

import haxby.map.*;
import java.awt.*;
import java.awt.event.*;

public class TrackSet implements Overlay, MouseListener {
	XMap map;
	TrackLine[] tracks;
	int selectedIndex;
	int size;
	byte types;
	public TrackSet( XMap map, int size ) {
		this.map = map;
		tracks = new TrackLine[size];
		this.size = 0;
		types = (byte)0x7;
		selectedIndex = -1;
	}
	public void add(TrackLine track) {
		if(size==tracks.length) {
			TrackLine[] tmp = new TrackLine[size+10];
			System.arraycopy(tracks, 0, tmp, 0, size);
			tracks = tmp;
		}
		tracks[size++] = track;
	}
	public String getName() {
		return "underway MG&G";
	}
	public String getDescription() {
		return "";
	}
	public void draw(Graphics2D g) {
		Stroke stroke = g.getStroke();
		g.setStroke( new BasicStroke( 1f/(float)map.getZoom() ));
		g.setColor(Color.black);
		for( int i=0 ; i<size ; i++) {
			if( (types & tracks[i].getTypes()) ==0)continue;
			tracks[i].draw(g);
		}
		if(selectedIndex>=0 && selectedIndex<size) {
			g.setColor(Color.white);
			tracks[selectedIndex].draw(g);
		}
		g.setStroke(stroke);
	}
	public void setTypes(boolean topo, boolean grav, boolean mag) {
		types = 0;
		if(topo) types |= (byte)0x4;
		if(grav) types |= (byte)0x2;
		if(mag)  types |= (byte)0x1;
		selectedIndex = -1;
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
				drawSelectedTrack(Color.black);
				selectedIndex = i;
				drawSelectedTrack(Color.white);
				return;
			}
		}
		if(selectedIndex==-1)return;
		drawSelectedTrack(Color.black);
		selectedIndex = -1;
	}
	public void mouseEntered( MouseEvent evt) {
	}
	public void mouseExited( MouseEvent evt) {
	}
}

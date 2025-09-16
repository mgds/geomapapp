package org.geomapapp.map;

import haxby.proj.*;
import haxby.map.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.tree.*;
import java.util.*;

public class MapPlace implements TreeNode {
	public double lon, lat, zoom;
	public String name;
	protected Rectangle2D.Double bounds;
	protected double wrap;
	protected boolean selected;
	protected MapPlace parent;
	protected Vector<MapPlace> children = new Vector<MapPlace>();
	protected boolean leaf;
	protected static Font font = new Font("SansSerif", Font.PLAIN, 10);
	public MapPlace( MapPlace parent, String name ) {
		this.parent = parent;
		this.name = name;
		leaf = false;
		
		if (parent != null) {
			parent.children.add(this);
		}
	}
	public MapPlace( MapPlace parent, String name, double lon, double lat, double zoom ) {
		this( parent, name );
		leaf = true;
		this.lon = lon;
		this.lat = lat;
		this.zoom = zoom;
		selected = false;
	}
	public String toString() {
		return name;
	}
	public void draw(Graphics2D g, XMap map) {
		if( !leaf)return;
		if(zoom<2.)return;
		Dimension dim = map.getParent().getSize();
		Projection proj = map.getProjection();
		Point2D p = proj.getMapXY(new Point2D.Double(lon, lat));
		bounds = new Rectangle2D.Double(
				p.getX()-.46*dim.getWidth()/zoom,
				p.getY()-.46*dim.getHeight()/zoom,
				.92*dim.getWidth()/zoom,
				.92*dim.getHeight()/zoom );
		wrap = map.getWrap();
		Rectangle2D.Double r = (Rectangle2D.Double)map.getClipRect2D();
		g.setColor( selected ? Color.white : Color.black);
		g.setStroke( new BasicStroke(1f/(float)map.getZoom()));
		float size = 9f/(float)map.getZoom();
		g.setFont( font.deriveFont( size));
		if( wrap>0 ) {
			while( bounds.x>r.x+r.width)bounds.x-=wrap;
			while( bounds.x+bounds.width<r.x)bounds.x+=wrap;
			while( bounds.x<r.x+r.width) {
				g.draw(bounds);
				g.drawString(name, 
					.1f*size+(float)(bounds.x),
					-.2f*size+(float)(bounds.y));
				bounds.x+=wrap;
			}
		} else {
			g.draw(bounds);
			g.drawString(name, 
				.5f*size+(float)(bounds.x+bounds.width),
				.5f*(float)(bounds.y + bounds.height));
		}
	}
	public void unselect(XMap map) {
		if(!leaf)return;
		if( !selected) return;
		selected=false;
		synchronized(map.getTreeLock() ) {
			draw(map.getGraphics2D(), map);
		}
	}
	boolean select( XMap map, Point2D p) {
		if(!leaf)return false;
		if(zoom<2.)return false;
		if( wrap>0 ) {
			while( bounds.x>p.getX())bounds.x-=wrap;
			while( bounds.x+bounds.width<p.getX())bounds.x+=wrap;
		}
		boolean tf = bounds.contains(p.getX(), p.getY());
		if( tf!=selected ) {
			selected = tf;
			synchronized(map.getTreeLock() ) {
				draw(map.getGraphics2D(), map);
			}
		}
		return tf;
	}
	public Enumeration children() {
		if( children==null ) createChildren();
		return children.elements();
	}
	public boolean getAllowsChildren() {
		return !leaf;
	}
	public TreeNode getChildAt( int index ) {
		if( children==null ) createChildren();
		return (TreeNode)children.get(index);
	}
	public int getChildCount() {
		if( children==null ) createChildren();
		return children.size();
	}
	public int getIndex( TreeNode node ) {
		if( children==null ) createChildren();
		return children.indexOf(node);
	}
	public TreeNode getParent() {
		return parent;
	}
	public boolean isLeaf() {
		return leaf;
	}
	void createChildren() {
		children = new Vector();
	}
}

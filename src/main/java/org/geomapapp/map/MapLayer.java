package org.geomapapp.map;

import org.geomapapp.geom.MapProjection;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import javax.swing.tree.TreeNode;
import javax.swing.JTree;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.io.IOException;

public abstract class MapLayer implements TreeNode {
	GMAMap map;
	protected String name;
	protected String description;
	protected Hashtable properties;
	protected MapLayer parent;
	protected Vector children;
	boolean visible = true;
	protected MapLayer(GMAMap map) {
	}
	protected MapLayer() {
		name = "unknown";
		description = "";
	}
	public String getName() {
		return name;
	}
	public void setName(String n) {
		name = n;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String txt) {
		description = txt;
	}
	public void setVisible( boolean tf ) {
		if( visible==tf )return;
		visible = tf;
		if( map!=null )map.repaint();
	}
	public void setActive(boolean tf) {
	}
	public void showDialog() {
	}
	public void hideDialog() {
	}
	public boolean setProperty(Object key, Object value) {
		if(properties==null) properties=new Hashtable();
		if( properties.put(key, value).equals(value) )return false;
		return true;
	}
	public abstract Rectangle2D getGeoBounds();
	public abstract Rectangle2D getMapBounds(MapProjection mProj);
	public abstract void load(String url) throws IOException;
	public abstract void draw(Graphics2D g, MapProjection mProj, AffineTransform aTrans);
	public Enumeration children() {
		if( children==null ) createChildren();
		return children.elements();
	}
	public boolean getAllowsChildren() {
		return children!=null;
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
		return children.size()>0;
	}
	protected void addChild( MapLayer layer ) {
		createChildren();
		children.add(layer);
	}
	protected void createChildren() {
		if( children==null )children = new Vector();
	}
	public String toString() {
		return name;
	}
}

package org.geomapapp.gis.shape;

import javax.swing.tree.*;
import java.util.*;

public class LayerEntry implements TreeNode {
	Vector children;
	LayerEntry parent;
	String name;
	String description;
	String url;
	boolean visible=true;
	public LayerEntry( LayerEntry parent, 
			Vector children, 
			String name, 
			String description, 
			String url ) {
		this.parent = parent;
		this.children = children;
		this.name = name;
		this.url = url;
	}
	public String toString() {
		return name;
	}
	public String getDescription() {
		return description;
	}
	public String getURL() {
		return url;
	}
	public void setParent(LayerEntry parent) {
		this.parent = parent;
	}
	public TreeNode getChildAt(int childIndex) {
		return (LayerEntry)children.get(childIndex);
	}
	public int getChildCount() {
		return children.size();
	}
	public TreeNode getParent() {
		return parent;
	}
	public int getIndex(TreeNode node) {
		return children.indexOf(node);
	}
	public boolean getAllowsChildren() {
		return children!=null;
	}
	public boolean isLeaf() {
		return children==null;
	}
	public Enumeration children() {
		return children.elements();
	}
	public Vector getChildren() {
		return children;
	}
	public boolean isVisible() {
		return visible;
	}
	public void setVisible( boolean tf ) {
		visible=tf;
	}
}

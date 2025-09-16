package org.geomapapp.db.dsdp;

import javax.swing.tree.*;
import java.util.*;

public class BRGEntry implements TreeNode {
	Vector children;
	BRGEntry parent;
	String name;
	String url;
	public BRGEntry( BRGEntry parent, Vector children, String name, String url ) {
		this.parent = parent;
		this.children = children;
		this.name = name;
		this.url = url;
	}
	public String toString() {
		return name;
	}
	public String getURL() {
		if( parent==null && (children==null||children.size()==0) )return null;
		return url;
	}
	public void setParent(BRGEntry parent) {
		this.parent = parent;
	}
	public TreeNode getChildAt(int childIndex) {
		return (BRGEntry)children.get(childIndex);
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
}

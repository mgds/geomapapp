package org.geomapapp.io;

import java.io.File;
import javax.swing.tree.*;
import javax.swing.JFileChooser;
import java.util.*;

public class FileNode implements TreeNode {
	File file;
	FileNode parent;
	Vector children;
	Filter filter;
	public FileNode(File file, FileNode parent) {
		this.file = file;
		this.parent = parent;
		children = null;
		if( parent==null )filter = new Filter(JFileChooser.FILES_AND_DIRECTORIES, false);
		else filter = parent.filter;
	}
	public File getFile() {
		return file;
	}
	public Enumeration children() {
		if( children==null ) createChildren();
		return children.elements();
	}
	public boolean getAllowsChildren() {
		return file.isDirectory();
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
		return !getAllowsChildren();
	}
	public void setSelectionMode(int mode) {
		filter.setSelectionMode(mode);
	}
	public void setShowHidden(boolean tf) {
		filter.setShowHidden(tf);
	}
	class Filter implements java.io.FileFilter {
		int mode;
		boolean showHidden;
		public Filter( int mode, boolean showHidden ) {
			this.mode = mode;
			this.showHidden = showHidden;
		}
		public void setSelectionMode(int mode) {
			this.mode = mode;
		}
		public void setShowHidden(boolean tf) {
			showHidden = tf;
		}
		public boolean accept(File file) {
			if( mode==javax.swing.JFileChooser.DIRECTORIES_ONLY && !file.isDirectory() )return false;
			if( !showHidden && file.getName().startsWith(".") )return false;
			return true;
		}
	}
	void createChildren() {
		children = new Vector();
		if( !getAllowsChildren() ) return;
		File[] files = file.listFiles( filter);
		if( files==null )return;
		Arrays.sort( files, new Comparator() {
			public int compare(Object o1, Object o2) {
				File f1 = (File)o1;
				File f2 = (File)o2;
				if( f1.isDirectory()) {
					if( !f2.isDirectory() ) return -1;
				} else if( f2.isDirectory() ) {
					return 1;
				}
				String s1 = f1.getName().toLowerCase();
				String s2 = f2.getName().toLowerCase();
				return s1.compareTo(s2);
			}
			public boolean equals(Object o) {
				return o==this;
			}
		});
		for( int k=0 ; k<files.length ; k++) {
			children.add( new FileNode( files[k], this) );
		}
	}
	public String toString() {
		return file.getName();
	}
	public static void main(String[] args) {
		FileNode root=null;
		if( args.length==0 ) {
			String dir = System.getProperty("user.dir");
			root = new FileNode( new File(dir), null);
		} else {
			root = new FileNode( new File(args[0]), null);
		}
		javax.swing.JFrame frame = new javax.swing.JFrame(root.toString());
		javax.swing.JTree tree = new javax.swing.JTree(root);
		tree.setDragEnabled(true);
		frame.getContentPane().add(new javax.swing.JScrollPane(tree));
		frame.pack();
		frame.show();
		frame.setDefaultCloseOperation(
			frame.EXIT_ON_CLOSE);
	}
}

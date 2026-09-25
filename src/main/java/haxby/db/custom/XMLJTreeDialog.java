package haxby.db.custom;

import haxby.util.URLFactory;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.geomapapp.io.GMARoot;
import org.geomapapp.util.ParseLink;

//import com.sun.org.apache.xerces.internal.impl.dtd.XMLNotationDecl;

public class XMLJTreeDialog extends JDialog implements ActionListener,
		MouseListener, KeyListener {
	private boolean localModified = false;
	private JTree localTree;
	private JTabbedPane tabPane;
	private TreePath selection;
	private JPopupMenu editMenu;
	private int copy_paste = 0;

	public XMLJTreeDialog(JDialog owner) {
		super(owner, true);

		File rt = GMARoot.getRoot();
		if (rt != null) {
			XMLTreeNode localRoot = new XMLTreeNode(null);
			localRoot.setTag("Folder");

			String fs = System.getProperty("file.separator");
			File localDB = new File(rt.getPath() + fs + "localDB.xml");
			if (localDB.exists()) {
				try {
					URL url = localDB.toURI().toURL();
					Vector data = ParseLink.parse(url);
					makeTree(localRoot, data);
				} catch (IOException ex) {
				}
			}

			localTree = new JTree(localRoot);
//			localTree.setRootVisible(false);
			localTree.getSelectionModel().setSelectionMode(
					TreeSelectionModel.SINGLE_TREE_SELECTION);
			localTree.addMouseListener(this);
			localTree.addKeyListener(this);
		}

		tabPane = new JTabbedPane();
		JScrollPane sp = new JScrollPane(localTree);
		tabPane.add(sp,"Local");

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(tabPane);

		editMenu = new JPopupMenu();
		JMenuItem mi = new JMenuItem("New");
		mi.setActionCommand("new");
		mi.addActionListener(this);
		editMenu.add(mi);

		mi = new JMenuItem("Edit");
		mi.setActionCommand("edit");
		mi.addActionListener(this);
		editMenu.add(mi);

		mi = new JMenuItem("Copy");
		mi.setActionCommand("copy");
		mi.addActionListener(this);
		editMenu.add(mi);

		mi = new JMenuItem("Cut");
		mi.setActionCommand("cut");
		mi.addActionListener(this);
		editMenu.add(mi);

		mi = new JMenuItem("Paste");
		mi.setActionCommand("paste");
		mi.addActionListener(this);
		editMenu.add(mi);

		mi = new JMenuItem("Delete");
		mi.setActionCommand("delete");
		mi.addActionListener(this);
		editMenu.add(mi);

		JPanel p = new JPanel();
		JButton b = new JButton("OK");
		b.setActionCommand("ok");
		b.addActionListener(this);
		p.add(b);

		b = new JButton("Cancel");
		b.setActionCommand("cancel");
		b.addActionListener(this);
		p.add(b);

		getContentPane().add(p,BorderLayout.SOUTH);
		setTitle("Browsing Databases");
		setSize(400, 400);
		setLocation(owner.getX() + 80, owner.getY() + 20);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setVisible(true);
	}

	public XMLTreeNode getSelection() {
		return (XMLTreeNode) selection.getLastPathComponent();
	}

	private void ok() {
		JTree tree;
		selection = localTree.getSelectionPath();

		//Do we need to write a new XML file?
		if (localModified) {
			File rt = GMARoot.getRoot();
			if (rt != null) {
				XMLTreeNode root = (XMLTreeNode) localTree.getModel().getRoot();
				Vector v = makeXMLVector(root);

				String fs = System.getProperty("file.separator");
				File localDB = new File(rt.getPath() + fs + "localDB.xml");
				localDB.delete();
				PrintStream ps=null;
				try {
					ps = new PrintStream(new FileOutputStream(localDB));
					ParseLink.printXML(v, 0, ps);
				} catch (FileNotFoundException ext) {
				} catch (IOException ext) {
				} finally {
					if (ps!=null) ps.close();
				}
			}
		}
		dispose();
	}

	private void cancel() {
		selection = null;
		dispose();
	}

	public void keyPressed(KeyEvent e) {

	}

	public void keyReleased(KeyEvent e) {
		if (e.isControlDown()) {
			if (e.getKeyCode()==KeyEvent.VK_C) copy();
			else if (e.getKeyCode()==KeyEvent.VK_X) cut();
			else if (e.getKeyCode()==KeyEvent.VK_V) paste();
			else if (e.getKeyCode()==KeyEvent.VK_D) delete();
			else if (e.getKeyCode()==KeyEvent.VK_E) edit();
			else if (e.getKeyCode()==KeyEvent.VK_N) newEntry();
		}
	}

	public void keyTyped(KeyEvent e) {
		if (e.isControlDown()) {
			if (e.getKeyCode()==KeyEvent.VK_C) copy();
			else if (e.getKeyCode()==KeyEvent.VK_X) cut();
			else if (e.getKeyCode()==KeyEvent.VK_V) paste();
			else if (e.getKeyCode()==KeyEvent.VK_D) delete();
			else if (e.getKeyCode()==KeyEvent.VK_E) edit();
			else if (e.getKeyCode()==KeyEvent.VK_N) newEntry();
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("ok")) ok();
		else if (e.getActionCommand().equals("cancel")) cancel();
		else if (e.getActionCommand().equals("new")) newEntry();
		else if (e.getActionCommand().equals("edit")) edit();
		else if (e.getActionCommand().equals("copy")) copy();
		else if (e.getActionCommand().equals("cut")) cut();
		else if (e.getActionCommand().equals("paste")) paste();
		else if (e.getActionCommand().equals("delete")) delete();
	}

	private void newEntry() {
		TreePath toMake;
		if (tabPane.getSelectedIndex()==0)
			toMake = localTree.getSelectionPath();
		else {
			JOptionPane.showMessageDialog(this, "Cannot edit global listing");
			return;
		}
		XMLTreeNode parent = (XMLTreeNode) toMake.getLastPathComponent();

		String[] strs = {"Folder","Entry"};
		int c = JOptionPane.showOptionDialog(this, "Entry type",
				"Chose entry type", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, strs, strs[0]);
		if (c == JOptionPane.CLOSED_OPTION) return;

		if (c==0) {
			String name = JOptionPane.showInputDialog(this, "Folder name:","Folder");
			if (name == null) return;
			Vector v = new Vector();
			v.add(new Object[] {"name",name});
			XMLTreeNode node = new XMLTreeNode(v);
			node.setUserObject(name);
			node.setTag("Folder");
			((DefaultTreeModel) localTree.getModel()).insertNodeInto(node, parent, parent.getChildCount());
			localModified = true;
		} else {
			String name = JOptionPane.showInputDialog(this, "Entry name:","Entry");
			if (name == null) return;
			String url = JOptionPane.showInputDialog(this, "URL:","url");
			if (url == null) return;
			String[] types = {"ASCII URL","EXCEL URL"};
			c = JOptionPane.showOptionDialog(this, "Entry type",
					"Chose entry type", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, types, types[0]);
			if (c==JOptionPane.CLOSED_OPTION) return;

			Vector v = new Vector();
			v.add(new Object[] {"name",name});
			v.add(new Object[] {"url",url});
			v.add(new Object[] {"type",c==0?new Integer(UnknownDataSet.ASCII_URL):new Integer(UnknownDataSet.EXCEL_URL)});
			XMLTreeNode node = new XMLTreeNode(v);
			node.setUserObject(name);
			node.setTag("Folder");
			((DefaultTreeModel) localTree.getModel()).insertNodeInto(node, parent, parent.getChildCount());
			localModified = true;
		}
	}
	
	private void edit() {
		TreePath toEdit;
		if (tabPane.getSelectedIndex()==0)
			toEdit = localTree.getSelectionPath();
		else {
			JOptionPane.showMessageDialog(this, "Cannot edit global listing");
			return;
		}
		if (toEdit==null) return;

		int index = 0;
		XMLTreeNode node = (XMLTreeNode)toEdit.getLastPathComponent();
		Vector properties = node.getProperties();
		if (properties.size()==0) return;
		if (properties.size()>1) {
			Object[] obj = new Object[properties.size()];
			for (int i = 0; i < obj.length; i++) {
				obj[i] = ((Object[]) properties.get(i))[0];
			}
			index = JOptionPane.showOptionDialog(this,
					"Chose a property to edit", "Property edit",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
					null, obj, obj[0]);

			if (index == JOptionPane.CLOSED_OPTION)
				return;
		}
		Object[] obj = (Object[]) properties.get(index);
		String s = JOptionPane.showInputDialog(this, obj[0], obj[1]);
		if (s!=null) {
			localModified = true;
			obj[1] = s;
			if (obj[0].equals("name")) {
				node.setUserObject(obj[1]);
				localTree.revalidate();
			}
		}
	}

	private void copy() {
		selection = localTree.getSelectionPath();

		if (selection==null) return;
		copy_paste = 1;
	}

	private void cut() {
		if (tabPane.getSelectedIndex()==0)
			selection = localTree.getSelectionPath();
		else {
			JOptionPane.showMessageDialog(this, "Cannot edit global listing");
			copy();
		}

		if (selection==null) return;
		copy_paste = 2;
	}

	private void paste() {
		if (selection==null) return;

		TreePath pasteTo;
		if (tabPane.getSelectedIndex()==0)
			pasteTo = localTree.getSelectionPath();
		else {
			JOptionPane.showMessageDialog(this, "Cannot edit global listing");
			return;
		}
		if (pasteTo==null) return;

		XMLTreeNode copy = (XMLTreeNode)selection.getLastPathComponent();
		XMLTreeNode node = (XMLTreeNode)pasteTo.getLastPathComponent();

		if (!node.getTag().equals("Folder"))
			return;

		localModified = true;

		XMLTreeNode paste = (XMLTreeNode) copy.clone();
		((DefaultTreeModel) localTree.getModel()).insertNodeInto(paste, node,
				node.getChildCount());

		if (copy_paste==2) {
			((DefaultTreeModel) localTree.getModel()).removeNodeFromParent(copy);
		}
		selection = pasteTo.pathByAddingChild(paste);
		copy_paste = 1;
		localTree.revalidate();
	}

	private void delete() {
		TreePath delete;
		if (tabPane.getSelectedIndex()==0)
			delete = localTree.getSelectionPath();
		else {
			JOptionPane.showMessageDialog(this, "Cannot edit global listing");
			return;
		}

		((DefaultTreeModel) localTree.getModel()).removeNodeFromParent((XMLTreeNode)delete.getLastPathComponent());
		localModified = true;
		localTree.revalidate();
	}

	public void mouseClicked(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
			try {
				Robot robot = new java.awt.Robot();
				robot.mousePress(InputEvent.BUTTON1_MASK);
				robot.mouseRelease(InputEvent.BUTTON1_MASK);
			} catch (AWTException ex) {
				System.out.println(ex);
			}
		}
	}

	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
			try {
				Robot robot = new java.awt.Robot();
				robot.mousePress(InputEvent.BUTTON1_MASK);
				robot.mouseRelease(InputEvent.BUTTON1_MASK);
			} catch (AWTException ex) {
				System.out.println(ex);
			}
		}
		maybeShowPopup(e);
	}

	public void mouseReleased(MouseEvent e) {
		maybeShowPopup(e);
	}

	private void maybeShowPopup(MouseEvent e) {
		if (e.isPopupTrigger()) {
			editMenu.show(e.getComponent(),
							e.getX(), e.getY());
		}
	}

	public static Vector showXMLJTree(String xmlurl, JDialog owner) {
		final Vector toRet = new Vector();
		Vector data = null;
		try {
			data = ParseLink.parse(URLFactory.url(xmlurl));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		XMLTreeNode root = new XMLTreeNode(null);
		makeTree(root, data);

		final JDialog dlg = new JDialog(owner,true);
		final JTree tree = new JTree(root);
		tree.setRootVisible(false);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		JScrollPane sp = new JScrollPane(tree);
		
		JPanel p = new JPanel();
		JButton b = new JButton("OK");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				XMLTreeNode node = (XMLTreeNode) tree.getSelectionPath().getLastPathComponent();
				if (node.isLeaf()) {
					if (ParseLink.getProperty(node.getProperties(), "url") != null) {
						toRet.addAll(node.getProperties());
						dlg.dispose();
					}
				}
			}
		});
		p.add(b);
		b = new JButton("Cancel");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				toRet.clear();
				dlg.dispose();
			}
		});
		p.add(b);

		dlg.setTitle("Browse General Data Viewer -> Tables");
		dlg.getContentPane().setLayout(new BorderLayout());
		dlg.getContentPane().add(p,BorderLayout.SOUTH);
		dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dlg.add(sp);
		dlg.setSize(400, 400);
		dlg.setLocation(owner.getX()+20,owner.getY()+20);
		dlg.setVisible(true);

		return toRet;
	}

	private static void makeTree(XMLTreeNode root, Vector childernData) {
		for (Iterator iter = childernData.iterator(); iter.hasNext();) {
			XMLTreeNode child = new XMLTreeNode(null);
			Object[] obj = (Object[]) iter.next();
			if (obj[1] instanceof Vector) {
				makeTree(child, (Vector)obj[1]);
				child.setUserObject(ParseLink.getProperty(child.getProperties(), "name"));
				child.setTag(obj[0].toString());
				root.add(child);
			} else {
				root.getProperties().add(obj);
			}
		}
	}

	private static Vector makeXMLVector(XMLTreeNode root) {
		Vector childern = new Vector();
		Vector properties = root.getProperties();
		childern.addAll(properties);
		for (Enumeration iter = root.children(); iter.hasMoreElements();) {
			XMLTreeNode child = (XMLTreeNode) iter.nextElement(); 
			Object[] obj = {child.getTag(), makeXMLVector(child)};
			childern.add(obj);
		}
		return childern;
	}

	public static class XMLTreeNode extends DefaultMutableTreeNode {
		Vector properties;
		String tag;

		public XMLTreeNode(Vector properties) {
			if (properties==null) properties = new Vector();
			this.properties = properties;
		}
		public Vector getProperties() {
			return properties;
		}
		public void setTag(String tag) {
			this.tag = tag;
		}
		public String getTag() {
			return tag;
		}
		public Object clone() {
			XMLTreeNode clone = new XMLTreeNode(properties);
			clone.setTag(tag);
			clone.setUserObject(getUserObject());
			for (Enumeration child = children(); child.hasMoreElements();) {
				clone.add((XMLTreeNode) ((XMLTreeNode) child.nextElement())
						.clone());
			}
			return clone;
		}
	}

}

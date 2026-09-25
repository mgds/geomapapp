package org.geomapapp.io;

import org.geomapapp.util.*;
import java.io.*;
import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class DirectoryChooser {
	MutableFileNode root;
	MutableFileNode selected;
	JComboBox ancestors;
	Vector lineage;
	DefaultTreeModel model;
	JTree tree;
	JTextField nameF;
	JDialog dialog;
	int op = 1;
	String title = "Select a Directory";
	
	boolean isGMARoot = false;
	
	public DirectoryChooser(File root) throws IOException {
		if( !root.isDirectory() ) throw new IOException("root must be a directory");
		this.root = new MutableFileNode( root, null );
		this.root.setSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		model = new DefaultTreeModel(this.root, true);
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public int showDialog( Component comp ) {
		return showDialog( comp, title, "West" );
	}
	
	public void setGMARoot( boolean input ) {
		isGMARoot = input;
	}
	
	public int showDialog( Component comp, String title, String location ) {
		javax.swing.border.Border b = BorderFactory.createEmptyBorder(1,1,1,1);
		dialog = new JDialog( (Frame)null, title, true);
		lineage = new Vector();
		ancestors = new JComboBox();
		getLineage();
		JPanel panel = new JPanel(new FlowLayout());
		panel.add( ancestors );
		ancestors.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				goTo();
			}
		});
		JButton home = new JButton(Icons.getIcon(Icons.HOME, false));
		home.setPressedIcon(Icons.getIcon(Icons.HOME, true));
		home.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				goHome();
			}
		});
		panel.add( home );
		home.setBorder(b);
		home.setToolTipText( "Home" );

		JButton up = new JButton(Icons.getIcon(Icons.PARENT, false));
		up.setPressedIcon(Icons.getIcon(Icons.PARENT, true));
		up.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				goUp();
			}
		});
		panel.add( up );
		up.setBorder(b);
		up.setToolTipText( "Up one directory" );

		JButton mkdir = new JButton(Icons.getIcon(Icons.FOLDER, false));
		mkdir.setPressedIcon(Icons.getIcon(Icons.FOLDER, true));
		mkdir.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createDir();
			}
		});
		panel.add( mkdir );
		mkdir.setBorder(b);
		mkdir.setToolTipText( "Create new folder" );

		JPanel p1 = new JPanel(new BorderLayout());
		p1.add( panel, "North");
		tree = new JTree( model);
		TreeSelectionModel sModel = tree.getSelectionModel();
		sModel.setSelectionMode(sModel.SINGLE_TREE_SELECTION);
		sModel.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
			public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
				setValue();
			}
		});
		p1.add( new JScrollPane(tree), "Center");
		p1.add( panel, "North");
		nameF = new JTextField("", 10);
		p1.add( nameF, "South");

		dialog.getContentPane().add( p1, "Center");

		op = 1;
		JPanel p2 = new JPanel( new FlowLayout());
		JButton okB = new JButton("OK");
		p2.add(okB);
		okB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				okOp();
			}
		});
		
		JButton cancelB = new JButton("Cancel");
		if ( isGMARoot ) {
			cancelB.setText("Cancel - WARNING: Home directory required for data import");
		}

		p2.add(cancelB);
		cancelB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancelOp();
			}
		});
		dialog.getContentPane().add( p2, "South");

		if( comp!=null) dialog.getContentPane().add( comp, location);

		dialog.pack();
		Dimension win = dialog.getSize();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		dialog.setLocation( (screen.width-win.width)/2, (screen.height-win.height)/2 );
		dialog.pack();
		dialog.setVisible(true);
		return op;
	}
	void okOp() {
		op = 0;
		TreePath path = tree.getSelectionPath();
		if( path==null ) {
			return;
		}
		selected = (MutableFileNode)path.getLastPathComponent();
		dialog.dispose();
	}
	void cancelOp() {
		op = 1;
		selected = null;
		dialog.dispose();
	}
	void setValue() {
		TreePath path = tree.getSelectionPath();
		if( path==null ) {
			nameF.setText("");
			return;
		}
		MutableFileNode node = (MutableFileNode)path.getLastPathComponent();
		nameF.setText( node.getFile().getName() );
	}
	void createDir() {
		TreePath path = tree.getSelectionPath();
		if( path==null ) {
			JOptionPane.showMessageDialog(tree.getTopLevelAncestor(), 
				"First select a parent directory");
			return;
		}
		JTextField f = new JTextField("NewFolder");
		JPanel panel = new JPanel(new GridLayout(0,1));
		panel.add(new JLabel("Create New Folder"));
		panel.add(f);
		int ok = JOptionPane.showConfirmDialog( tree.getTopLevelAncestor(),
				panel, "Create New Folder",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE );
		if( ok==JOptionPane.CANCEL_OPTION )return;
		System.out.println( path.getLastPathComponent().getClass().getName());
		MutableFileNode node = (MutableFileNode)path.getLastPathComponent();
		File dir = node.getFile();
		File file = new File(dir, f.getText());
		file.mkdir();
		MutableFileNode newNode = new MutableFileNode(file, node);
		model.insertNodeInto( newNode, node, 0);
	}
	void goHome() {
		File dir = new File(System.getProperty("user.home"));
		if( dir.equals(root.getFile()) )return;
		root = new MutableFileNode( dir, null );
		root.setSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		getLineage();
		model.setRoot( root );
		setValue();
	}
	void goUp() {
		File dir = root.getFile().getParentFile();
		if( dir==null )return;
		root = new MutableFileNode( dir, null );
		root.setSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		getLineage();
		model.setRoot( root );
		setValue();
	}
	void goTo() {
		int i = ancestors.getSelectedIndex();
		if( i<1 )return;
		File dir = (File)lineage.get(i);
		if( dir.equals(root.getFile()) )return;
		root = new MutableFileNode( dir, null );
		root.setSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		getLineage();
		model.setRoot( root );
		setValue();
	}
	void getLineage() {
		File dir = root.getFile();
		lineage.clear();
		ancestors.removeAllItems();
		while( dir!=null ) {
			if( dir.getParentFile()==null )break;
			lineage.add(dir);
			ancestors.addItem(dir.getName());
			dir=dir.getParentFile();
		}
		lineage.add(dir);
		ancestors.addItem(dir.getPath());
		File[] roots = File.listRoots();
		for( int k=0 ; k<roots.length ; k++) {
			if( roots[k].equals(dir) )continue;
			lineage.add(roots[k]);
			ancestors.addItem( roots[k].getPath() );
		}
	}
	public File getSelectedFile() {
		if(selected==null) return null;
		return selected.getFile();
	}
	public static void main( String[] args ) {
		File root = new File(System.getProperty("user.dir"));
		try {
			DirectoryChooser c = new DirectoryChooser(root);
			ClassLoader cl = Class.forName(
				"org.geomapapp.io.GMARoot").getClassLoader();
			java.net.URL url = cl.getResource(
				"org/geomapapp/resources/html/GMAFolder.html");
			JEditorPane label = new JEditorPane(url);
			label.setBackground( new java.awt.Color(224, 224, 224));
			label.setBorder(BorderFactory.createEtchedBorder());
			int ok = c.showDialog(label);
			System.out.println(ok);
			if( c.selected!=null) System.out.println(c.selected.getFile().getPath());
		} catch(Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}

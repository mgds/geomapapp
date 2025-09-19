package haxby.util;

import java.io.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;

public class DirectoryChooser implements FileFilter,
					ActionListener,
					KeyListener,
					MouseListener {
	JList list;
	JButton up;
	File dir;
	File[] dirs;
	JTextField currentDir;
	boolean waiting;
	int selectedIndex;
	public DirectoryChooser( File dir ) {
		this.dir = dir;
		waiting = true;
		selectedIndex = -1;
	}
	public File chooseDirectory() {
		waiting = true;
		JPanel panel = new JPanel(new BorderLayout());
		currentDir = new JTextField( dir.getPath(), dir.getPath().length() );
		currentDir.addActionListener( this );
		panel.add( currentDir, "North" );
		list = new JList( getDirs() );
		list.addKeyListener( this);
		list.addMouseListener( this );
		list.setSelectionMode( ListSelectionModel.SINGLE_SELECTION);
		panel.add( new JScrollPane(list), "Center");
	//	JPanel buttons = new JPanel(new GridLayout(0, 1));
		Box buttons = Box.createVerticalBox();
		up = new JButton("parent");
		buttons.add(up);
		up.addActionListener( this );
		JButton ok = new JButton("OK");
		buttons.add(ok);
		ok.addActionListener( this );
		JButton cancel = new JButton("Cancel");
		buttons.add(cancel);
		cancel.addActionListener( this );
		panel.add( buttons, "East");
		selectedIndex = -1;
		JFrame frame = new JFrame("select a directory");
		frame.getContentPane().add(panel);
		frame.pack();
		frame.setVisible(true);
		while( waiting ) {
			try {
				Thread.currentThread().sleep(100L);
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		frame.dispose();
		if( selectedIndex==-1 ) return null;
		return dirs[selectedIndex];
	}
	public String[] getDirs() {
		dirs = dir.listFiles( this );
		Vector paths = new Vector(dirs.length);
		for( int i=0 ; i<dirs.length ; i++) paths.add( dirs[i].getPath() );
		java.util.Collections.sort( paths);
		String[] files = new String[dirs.length];
		for( int i=0 ; i<dirs.length ; i++) {
			dirs[i] = new File((String)paths.get(i));
			files[i] = dirs[i].getName();
		}
		return files;
	}
	public boolean accept(File file) { 
		return file.isDirectory();
	}
	public void actionPerformed(ActionEvent evt) {
		String cmd = evt.getActionCommand();
		if( cmd.equals("parent") ) {
			dir = dir.getParentFile();
			list.setListData( getDirs() );
			currentDir.setText( dir.getPath() );
		} else if(cmd.equals("OK")) {
			int i = list.getSelectedIndex();
			if( i==-1 ) {
				JOptionPane.showMessageDialog( haxby.map.MapApp.anchor, "no directory selected" );
				return;
			}
			selectedIndex = i;
			waiting = false;
		} else if(cmd.equals("Cancel")) {
			selectedIndex = -1;
			waiting = false;
		}
		if( evt.getSource() == currentDir ) {
			File file = new File(currentDir.getText());
			if( !file.exists() || !file.isDirectory() ) return;
			dir = file;
			list.setListData( getDirs() );
			currentDir.setText( dir.getPath() );
		}
	}
	public void mousePressed(MouseEvent evt) {
	}
	public void mouseReleased(MouseEvent evt) {
	}
	public void mouseEntered(MouseEvent evt) {
	}
	public void mouseExited(MouseEvent evt) {
	}
	public void mouseClicked(MouseEvent evt) {
		if( evt.getClickCount()==2) {
			if( list.getSelectedIndex()==-1 )return;
			dir = dirs[list.getSelectedIndex()];
			list.setListData( getDirs() );
			currentDir.setText( dir.getPath() );
		}
	}
	public void keyPressed( KeyEvent evt ) {
	}
	public void keyReleased( KeyEvent evt ) {
		if( evt.getKeyCode()==evt.VK_ENTER ) {
			if( list.getSelectedIndex()==-1 )return;
			dir = dirs[list.getSelectedIndex()];
			list.setListData( getDirs() );
			currentDir.setText( dir.getPath() );
		}
	}
	public void keyTyped( KeyEvent evt ) {
	}
	public static void main(String[] args) {
		System.out.println( (new File(".")).getAbsolutePath() );
		DirectoryChooser dc = new DirectoryChooser( new File(System.getProperty("user.home")) );
		File file = dc.chooseDirectory();
		if( file == null ) {
			System.out.println( "no file selected" );
		} else {
			System.out.println( file.getPath() +" selected");
		}
		System.exit(0);
	}
}

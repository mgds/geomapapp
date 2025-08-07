package org.geomapapp.io;

import haxby.map.MapApp;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;

public class GMADownload {
	public static String root_path = PathUtil.getPath("ROOT_PATH", MapApp.BASE_URL);
	public static String public_home_path = PathUtil.getPath("PUBLIC_HOME_PATH");

	public static void download(String oldVersion, String newVersion) {

//		GMA 1.5.2: main panel too crowded, create some gap between the components in it
		JPanel main = new JPanel(new BorderLayout( 10, 10 ));

		JLabel label = new JLabel(
			"<html><body><center><bold><h1>"
			+"A New Version of <I>GeoMapApp</I> " + newVersion + " <br>"
			+"is Available for Download</h1>"
			+"<br>If you have trouble installing <i>GeoMapApp "
			+newVersion+"</i>, go to:<br>" + public_home_path + "</center>"
			+"<br>* The current version you are running is <b>"+oldVersion
			+"</b><hr></body></html>");
		main.add(label, "North");

		// try to find the executable file
		File userDir = new File(System.getProperty("user.dir"));

		boolean found = false;
		File[] files = null;
		if( userDir.exists() ) {
			files = gmaFiles(userDir);
			if( files!=null && files.length>0) found=true;
		}
		if( !found ) {
			String pathSep = System.getProperty("path.separator");
			String classPath = System.getProperty("java.class.path");
			StringTokenizer st = new StringTokenizer(classPath,pathSep);
		//	System.out.println( classPath );
		//	System.out.println( userDir.getPath() );
			while( !found && st.hasMoreTokens() ) {
				File classDir = new File(st.nextToken());
				if( !classDir.exists() )continue;
				files = gmaFiles(userDir);
				if( files!=null && files.length>0) found=true;
			}
		}

		JPanel which = new JPanel(new GridLayout(0,1));
		which.setBorder(BorderFactory.createTitledBorder(
				"Select a Recommended Format"));
		ButtonGroup gp = new ButtonGroup();
		JCheckBox exe = new JCheckBox(".exe - for Windows");
		gp.add(exe);
		which.add(exe);
		JCheckBox app = new JCheckBox(".app - for Macs (Downloads as .dmg, double-click to open)");
		gp.add(app);
		which.add(app);
		JCheckBox jar = new JCheckBox(".jar - Any Platform");
		gp.add(jar);
		which.add(jar);

		// Get OS name
		String operatingSystem = System.getProperty("os.name").toLowerCase();

		if(operatingSystem.contains("win")) {
			exe.setSelected(true);
			app.setSelected(false);
			jar.setSelected(false);
		} else if(operatingSystem.contains("mac")) {
			app.setSelected(true);
			exe.setEnabled(false);
			jar.setSelected(false);
		} else if (operatingSystem.contains("nix") ||
				operatingSystem.contains("nux") ||
				operatingSystem.contains("aix")){
			jar.setSelected(true);
			app.setSelected(false);
			exe.setEnabled(false);
		} else {
			jar.setSelected(true);
			app.setSelected(false);
			exe.setEnabled(false);
		}

/*		if( found && files.length==1 ) {
			if( files[0].getName().endsWith(".exe")) { 
				exe.setSelected(true);
			} else if( files[0].getName().endsWith(".dmg")) {
				app.setSelected(true);
			} else {
				jar.setSelected( true );
			}
		}
*/
		JPanel options = new JPanel(new GridLayout(0,1));
		options.setBorder(BorderFactory.createTitledBorder(
				"Select an Option"));
		gp = new ButtonGroup();
		String downloadNowText = "<html><b>Download Now</b></html>";
		JCheckBox download = new JCheckBox(downloadNowText);
		gp.add(download);
		options.add(download);
		download.setSelected(true);
		String downloadLaterText = "<html><b>Download Later</b></html>";
		JCheckBox later = new JCheckBox(downloadLaterText);
		gp.add(later);
		options.add(later);

		JButton updates = new JButton("Read What's New");
		updates.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				viewUpdates();
			}
		});
		JPanel panel1 = new JPanel(new BorderLayout());
		panel1.add(updates, BorderLayout.WEST);
		panel1.add(options, BorderLayout.CENTER);
		panel1.add(which,BorderLayout.SOUTH);

//		GMA 1.5.2: Shift this panel up to incorporate mailing-lists panel
//		main.add(panel1, "South");
		main.add(panel1, "Center");

//		***** GMA 1.5.2: Add panel to incorporate message that requests the user join the mailing lists
		JButton joinAnnounceMailingList = new JButton ( "Join Mailing List" );
		joinAnnounceMailingList.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				joinAnnounce();
			}
		});
		JPanel panel2 = new JPanel(new BorderLayout( 5, 5 ));
		Border emptyBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		Border lineBorder = BorderFactory.createLineBorder( Color.LIGHT_GRAY );
		Border titledBorder = BorderFactory.createTitledBorder( lineBorder, "Help improve GeoMapApp" );
		Border compBorder = BorderFactory.createCompoundBorder( titledBorder, emptyBorder );
		panel2.setBorder( compBorder );	
		JLabel joinText = new JLabel("Join the mailing list and provide feedback.");
		panel2.add( joinText, BorderLayout.WEST );
		panel2.add( joinAnnounceMailingList, BorderLayout.CENTER );
		main.add(panel2, "South");
//		***** GMA 1.5.2

		int pane=  JOptionPane.showConfirmDialog(
				null,
				main,
				"New Version Available",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if(later.isSelected()) return;

		// Switch from pointing to downloading file accroding to system and to point to webpage for download for that OS.
		String name = "MapApp/";
		//String name = "GeoMapApp.jar";
		if ( jar.isSelected() ) {
			//name = "GeoMapApp.jar";
			name += "GeoMapApp.jar";
		}
		else if ( exe.isSelected() ) {
			//name = "GeoMapApp.exe";
			name += "GeoMapApp.exe";
		}
		else if ( app.isSelected() ) {
			//name = "GeoMapApp.dmg";
			name += "GeoMapApp-" + newVersion + "-";
			String whichArch = System.getProperty("os.arch");
			if(whichArch.equals("aarch64")) {
				name += "Silicon";
			}
			else {
				name += "Intel";
			}
			name += ".dmg";
		}

		String urlName = PathUtil.getPath("ROOT_PATH") + name;
		if(pane == JOptionPane.OK_OPTION) {
		try {
			BrowseURL.browseURL(urlName);
		} finally {
				System.exit(0);
		}
		} else if (pane == JOptionPane.CLOSED_OPTION) {
			System.exit(0);
		} else if (pane == JOptionPane.CANCEL_OPTION) {
			System.exit(0);
		}
		/*
		File saveDir = found ? files[0].getParentFile() 
				: new File(System.getProperty("user.home"));
		JFileChooser chooser = new JFileChooser(saveDir);
		try {
			chooser.setSelectedFile( new File(saveDir, name));
		} catch (NullPointerException e) { }
		
		int ok = chooser.showSaveDialog(null);
		if( ok==chooser.CANCEL_OPTION ) {
			ok = JOptionPane.showConfirmDialog(
				null,
				"Continue GeoMapApp startup?",
				"Continue GeoMapApp?",
				JOptionPane.YES_NO_OPTION);
			if( ok==JOptionPane.YES_OPTION) return;
			System.exit(0);
		}
		File f = chooser.getSelectedFile();

		try {
			URL url = URLFactory.url( urlName );
			BufferedInputStream in = new BufferedInputStream(
					url.openStream());
			BufferedOutputStream out = new BufferedOutputStream(
					new FileOutputStream( f ));
			//byte[] data = new byte[32768];
			byte[] data = new byte[512];
			int length;

			//while( (length=in.read(data, 0, 32768))!=-1) {
			while( (length=in.read(data, 0, 512))!=-1) {
				out.write(data, 0, length);
			}
			out.flush();
			JOptionPane.showMessageDialog(null, "Update successful.\nPlease restart GeoMapApp");
		} catch (IOException ex){
			ex.printStackTrace();
			File f2 = new File(f.getPath().substring(0, f.getPath().lastIndexOf('.'))+"Old"+
						f.getPath().substring(f.getPath().lastIndexOf('.')));
			try {
				FileUtility.copy(f, f2);
			} catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, 
						"Cannot write to file.\nDownload from " + public_home_path,
						"Error", JOptionPane.ERROR_MESSAGE);
				ok = JOptionPane.showConfirmDialog(
						null,
						"Continue GeoMapApp startup?",
						"Continue GeoMapApp?",
						JOptionPane.YES_NO_OPTION);
					if( ok==JOptionPane.YES_OPTION) return;
					BrowseURL.browseURL(public_home_path);
					System.exit(0);
			}
			JOptionPane.showMessageDialog(null, "GeoMapApp has been prepared to download.\nPlease repeat selections.");
			Runtime r = Runtime.getRuntime();
			if (System.getProperty("os.name").toLowerCase().indexOf("windows")>-1) {
				try { 
					if (f.getPath().indexOf(".jar")>-1)
						r.exec("java -jar "+f2.getPath()); 
					else
						r.exec(f2.getPath()); 
				}
				catch (IOException e) { }
			} else {
				try { r.exec("java -jar "+f2.getPath()); }
				catch (IOException e) { }
			}
		} finally {
			System.exit(0);
		}
		*/
	}

	static File[] gmaFiles(File userDir) {
		File[] files = userDir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return file.getName().equals("GeoMapApp.jar")
					|| file.getName().equals("GeoMapApp.exe")
					|| file.getName().equals("GeoMapApp.dmg")
					|| file.getName().equals("GeoMapApp.app");
			}
		});
		return files;
	}

	static JDialog dialog;
	static void closeDialog() {
		if( dialog!=null ) {
			dialog.dispose();
		}
	}

	static void viewUpdates() {
		String url = PathUtil.getPath("PUBLIC_HOME_PATH") + "eNewsletters/index.html";
				//"WhatsNew.html";

		BrowseURL.browseURL( url );
	}

//	***** GMA 1.5.2: Add functions to display subscribe page when button is clicked
	static void joinDiscuss() {
		String url =  PathUtil.getPath("DISCUSS_PATH");
		BrowseURL.browseURL( url );
	}

	static void joinAnnounce() {
		String url = PathUtil.getPath("ANNOUNCE_PATH");;
		BrowseURL.browseURL( url );
	}

}
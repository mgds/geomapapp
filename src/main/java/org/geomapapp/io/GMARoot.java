package org.geomapapp.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

//import java.io.*;

public class GMARoot {
	//public final static String ROOT_URL = haxby.map.MapApp.TEMP_BASE_URL + "GMA/";
	static String root;
	public static File getRoot() {
		if( root!=null ) {
			if( root.equals("null") ) return null;
			return new File(root);
		}
		try {
			File prefs = new File(
				System.getProperty("user.home"), ".geomapapp-home");
			while( !prefs.exists() ) {
				createPrefs(prefs);
			}
			BufferedReader in = new BufferedReader(
				new FileReader(prefs));
			String s = in.readLine();
			if( s==null || s.equals("null") ) {
				s = createPrefs(prefs);
			}
			root = s;
			moveToCorrectSpot();
			return new File(root);
		} catch(SecurityException se) {
			se.printStackTrace();
			return null;
		} catch(IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	public static void moveToCorrectSpot() {
		File newHome = new File(System.getProperty("user.home"), ".GMA/");
		File oldHome = new File(root);
		try {
			if(!newHome.getCanonicalPath().equals(oldHome.getCanonicalPath())) {
				String noticeFileName = "IMPORTANT_NOTICE.txt";
				File notice = new File(oldHome, noticeFileName);
				String noticeMsg = "This folder is now obsolete and can be deleted. All its contents (except this file) have been copied to " + newHome.getCanonicalPath() + ", where GeoMapApp will continue to look in the future.";
				if(notice.exists()) {
					notice.delete();
					noticeMsg += "\n\nGeoMapApp will change the default home folder to " + newHome.getCanonicalPath() + " every time it starts. Changing it back to this one is a waste of time.";
				}
				System.out.println("Root directory is currently " + root);
				org.apache.commons.io.FileUtils.copyDirectory(oldHome, newHome);
				root = newHome.getCanonicalPath();
				File prefs = new File(System.getProperty("user.home"), ".geomapapp-home");
				PrintStream out = new PrintStream(new FileOutputStream(prefs, false));
				out.println(root);
				out.close();
				notice.createNewFile();
				PrintStream noticeOut = new PrintStream(new FileOutputStream(notice, false));
				noticeOut.println(noticeMsg);
				noticeOut.close();
				System.out.println("Moved GMA home from " + oldHome.getCanonicalPath() + " to " + newHome.getCanonicalPath());
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public static String createPrefs(File prefs) {
		File f = new File(System.getProperty("user.home"), ".GMA/");
		f.mkdir();
		PrintStream out;
		try {
			out = new PrintStream(new FileOutputStream(prefs, false));
			String s = f.getCanonicalPath();
			out.println(s);
			out.close();
			root = s;
		} catch(SecurityException se) {
			root = null;
			se.printStackTrace();
		} catch (FileNotFoundException e) {
			root = null;
			e.printStackTrace();
		} catch (IOException e) {
			root = null;
			e.printStackTrace();
		}
		return root;
	}

//	public static String createPrefs(File prefs) {
//		try {
//			ClassLoader cl = Class.forName(
//					"org.geomapapp.io.GMARoot"
//				).getClassLoader();
//			java.net.URL url = cl.getResource(
//						"org/geomapapp/resources/html/GMAFolder.html");
////	System.out.println( url.toString() );
//		//	BufferedReader in = new BufferedReader(
//		//		new InputStreamReader( 
//		//			cl.getResource(
//		//				"org/geomapapp/resources/html/GMAFolder.html"
//		//			).openStream() ));
//		//	String html = in.readLine();
//			String s;
//		//	while( (s=in.readLine())!=null ) {
//		//		html += " "+s;
//		//		System.out.println(s);
//		//	}
//			JEditorPane label = new JEditorPane(url);
//			label.setBackground( new java.awt.Color(224, 224, 224));
//			label.setBorder(BorderFactory.createEtchedBorder());
//		//	JButton label = new JButton(html);
//			DirectoryChooser dc = new DirectoryChooser(new File(System.getProperty("user.home")));
//			dc.setGMARoot(true);
//			JFileChooser chooser;
//		//	JFileChooser chooser = new JFileChooser(
//		//		System.getProperty("user.home"));
//		//	chooser.setAccessory(label);
//		//	chooser.setFileSelectionMode(chooser.DIRECTORIES_ONLY);
//		//	int ok = chooser.showDialog(null, "OK");
//			int ok = dc.showDialog(label);
//			PrintStream out = new PrintStream(
//				new FileOutputStream(prefs));
//			if( ok==JFileChooser.CANCEL_OPTION ) s="null";
//			else s = dc.getSelectedFile().getCanonicalPath();
//			out.println(s);
//			out.close();
//			root = s;
//		} catch(Exception ex) {
//			root = "null";
//			ex.printStackTrace();
//		}
//		return root;
//	}
}

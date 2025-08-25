package org.geomapapp.io;

import haxby.map.MapApp;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
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
		
		JDialog dialog = new JDialog();
		
		ActionListener listener = new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				//System.out.println(event);
				String cmd = event.getActionCommand();
				switch(cmd) {
				case "update":
					downloadNewVersion(newVersion);
					break;
				case "whatsNew":
					viewUpdates(newVersion);
					break;
				case "mailingList":
					joinAnnounce();
					break;
				case "ignore":
					dialog.setVisible(false);
					break;
				case "quit":
					System.exit(0);
				default:
					System.out.println("Unknown action command: " + cmd);
					System.exit(1);
				}
			}
		};
		
		JPanel main = new JPanel(new GridBagLayout());
		main.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
		GridBagConstraints gbc = new GridBagConstraints();
		Dimension bigBtnSize = new Dimension(300, 50),
				smallBtnSize = new Dimension(170, 30);
		Insets leftSide = new Insets(0, 0, 0, 30),
				rightSide = new Insets(0, 30, 0, 0);
		gbc.anchor = GridBagConstraints.PAGE_START;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridheight = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 7;
		gbc.weightx = 0.1;
		gbc.weighty = 0.1;
		JLabel label = new JLabel(
				"<html><body><center><bold><h2>"
				+ "An update for GeoMapApp, version " 
				+ newVersion + ", has been released.</h2><br>");
		main.add(label, gbc);
		
		gbc.fill = GridBagConstraints.NONE;
		
		gbc.gridwidth = 3;
		gbc.gridx = 3;
		gbc.gridy = 2;
		gbc.insets = new Insets(0, 0, 20, 0);
		JButton updateBtn = new JButton("<html><h3>Update GeoMapApp</h3></html>");
		updateBtn.setActionCommand("update");
		updateBtn.addActionListener(listener);
		main.add(updateBtn, gbc);
		
		gbc.gridx = 2;
		gbc.gridwidth = 2;
		gbc.gridy = 4;
		gbc.anchor = GridBagConstraints.LINE_END;
		gbc.weightx = 0.1;
		JButton whatsNewBtn = new JButton("What's New");
		whatsNewBtn.setActionCommand("whatsNew");
		whatsNewBtn.addActionListener(listener);
		whatsNewBtn.setPreferredSize(smallBtnSize);
		gbc.insets = leftSide;
		main.add(whatsNewBtn, gbc);
		
		gbc.gridx = 4;
		gbc.anchor = GridBagConstraints.LINE_START;
		JButton joinMailingListBtn = new JButton("Join Mailing List");
		joinMailingListBtn.setActionCommand("mailingList");
		joinMailingListBtn.addActionListener(listener);
		joinMailingListBtn.setPreferredSize(smallBtnSize);
		gbc.insets = rightSide;
		main.add(joinMailingListBtn, gbc);
		
		gbc.gridy = 6;
		gbc.gridx = 2;
		gbc.anchor = GridBagConstraints.LINE_END;
		JButton ignoreBtn = new JButton("Run Without Updating");
		ignoreBtn.setActionCommand("ignore");
		ignoreBtn.addActionListener(listener);
		ignoreBtn.setPreferredSize(smallBtnSize);
		gbc.insets = leftSide;
		main.add(ignoreBtn, gbc);
		
		gbc.gridx = 4;
		gbc.anchor = GridBagConstraints.LINE_START;
		JButton quitBtn = new JButton("Quit GeoMapApp");
		quitBtn.setActionCommand("quit");
		quitBtn.addActionListener(listener);
		quitBtn.setPreferredSize(smallBtnSize);
		gbc.insets = rightSide;
		main.add(quitBtn, gbc);
		
		Dimension optimalSize = getUnion(quitBtn.getMinimumSize(), ignoreBtn.getMinimumSize(),
				joinMailingListBtn.getMinimumSize(), whatsNewBtn.getMinimumSize());
		whatsNewBtn.setPreferredSize(optimalSize);
		joinMailingListBtn.setPreferredSize(optimalSize);
		ignoreBtn.setPreferredSize(optimalSize);
		quitBtn.setPreferredSize(optimalSize);

		dialog.setTitle("New Version Available");
		dialog.setModal(true);
		dialog.setContentPane(main);
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
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
	
	// GMA 3.7.6: add function to automatically determine which OS/arch you are on and download the right version
	static void downloadNewVersion(String newVersion) {
		String os = System.getProperty("os.name").toLowerCase();
		String path = "MapApp/";
		if(os.contains("win")) {
			path += "GeoMapApp.exe";
		}
		else if(os.contains("mac")) {
			String name = "GeoMapApp-" + newVersion + "-";
			String whichArch = System.getProperty("os.arch");
			if(whichArch.equals("aarch64")) {
				name += "Silicon";
			}
			else {
				name += "Intel";
			}
			name += ".dmg";
			path += name;
		}
		else {
			path += "GeoMapApp.jar";
		}
		String url = PathUtil.getPath("ROOT_PATH") + path;
		try {
			BrowseURL.browseURL(url);
		}
		finally {
			System.exit(0);
		}
	}
	
	//Also add a way to view a specific version's updates
	static void viewUpdates(String newVersion) {
		String url = PathUtil.getPath("PUBLIC_HOME_PATH") + "eNewsletters/v" + newVersion.replace('.', '_') + ".html";
		BrowseURL.browseURL( url );
	}
	
	static Dimension getUnion(Dimension... dimensions) {
		int width = 0, height = 0;
		for(Dimension d : dimensions) {
			if(width < d.width) width = d.width;
			if(height < d.height) height = d.height; 
		}
		return new Dimension(width, height);
	}

}
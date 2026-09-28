package org.geomapapp.credit;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.json.JSONException;

import haxby.map.MapApp;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.URLFactory;
import haxby.util.VersionUtil;
/**
 * Creation of buttons which will perform a browseURL call to outside sources.
 * Retrieves the destination url from a remote xml file with PathUtil.
 * @version 2.3.1
 * @since 1.1
 */
public class OtherSources {
	JPanel panel;
	//Path on server to locate source xml file
	String CREDIT_PATH_URLS = PathUtil.getPath("CREDIT_PATH_URLS",
			MapApp.BASE_URL+"/gma_credit/html/GMA_Credit_paths.xml")
			.replaceAll(MapApp.PRODUCTION_URL, MapApp.BASE_URL);
	String MARINE_URL = "https://www.gebco.net";
	String LAND_URL = "https://asterweb.jpl.nasa.gov/gdem.asp";
	String ANTARCTIC_URL = "https://www.bas.ac.uk/project/bedmap/";
	String ARCTIC_URL = "https://www.ngdc.noaa.gov/mgg/bathymetry/arctic/arctic.html";
	String SRTM_URL = "https://www2.jpl.nasa.gov/srtm/";
	String NED_URL = "https://www.usgs.gov/publications/national-elevation-dataset";
	
	public OtherSources() {
		panel = new JPanel(new BorderLayout());

		JPanel base = new JPanel(new GridLayout(0,2));
		JLabel marineB = new JLabel("Marine");
		
		
		URL url = null;
		String btn_txt = "GEBCO";
	
	
		/*try {
			String btnTxtURL = PathUtil.getPath("CREDIT_PATH") + "btn_txt/gebco";
			url = URLFactory.url(btnTxtURL);
			BufferedReader in = new BufferedReader(new InputStreamReader( url.openStream() ));
			btn_txt = in.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
		try {
			btn_txt = VersionUtil.getVersion("GEBCO");
		}
		catch(JSONException e) {
			e.printStackTrace();
		}


		
		JButton b = new JButton(btn_txt);
		base.add(marineB);
		base.add(b);
		b.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				PathUtil.loadNewPaths(CREDIT_PATH_URLS);
				String url = PathUtil.getPath("INFO_PATH_MARINE", MARINE_URL);
				BrowseURL.browseURL(url);
			}
		});

		JLabel landB = new JLabel("Land");
		b = new JButton("ASTER");
		base.add(landB);
		base.add(b);
		b.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				PathUtil.loadNewPaths(CREDIT_PATH_URLS);
				String url = PathUtil.getPath("INFO_PATH_LAND_ASTER", LAND_URL);
				BrowseURL.browseURL(url);
			}
		});

		JLabel antB = new JLabel("Antarctic");
		b = new JButton("BEDMAP");
		base.add(antB);
		base.add(b);
		b.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				PathUtil.loadNewPaths(CREDIT_PATH_URLS);
				String url = PathUtil.getPath("INFO_PATH_ANTARCTIC_BEDMAP", ANTARCTIC_URL);
				BrowseURL.browseURL(url);
			}
		});

		JLabel arcticB = new JLabel("Arctic");
		b = new JButton("IBCAO");
		base.add(arcticB);
		base.add(b);
		b.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				PathUtil.loadNewPaths(CREDIT_PATH_URLS);
				String url = PathUtil.getPath("INFO_PATH_ARCTIC_IBCAO", ARCTIC_URL);
				BrowseURL.browseURL(url);
			}
		});
		base.setBorder( BorderFactory.createTitledBorder("GMRT Base Map Components"));
		panel.add(base,"North");

		JLabel usB = new JLabel("US");
		b = new JButton("NED");
		b.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				PathUtil.loadNewPaths(CREDIT_PATH_URLS);
				String url = PathUtil.getPath("INFO_PATH_NED", NED_URL);
				BrowseURL.browseURL(url);
			}
		});
		
		JPanel gmrtBase = new JPanel(new GridLayout(5,1));
		JButton gmrtB = new JButton("Visit the GMRT Website");
		gmrtBase.add(gmrtB);
		gmrtB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				String url = PathUtil.getPath("GMRT2_ROOT_PATH");
				BrowseURL.browseURL(url);
			}
		});
		panel.add(gmrtBase);
		
		
		base = new JPanel(new GridLayout(0,2));
		base.add(usB);
		base.add(b);
		base.setBorder( BorderFactory.createTitledBorder("High Resolution Land"));
		panel.add(base,"South");

	}
	public JPanel getPanel() {
		return panel;
	}
}

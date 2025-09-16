package haxby.util;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JOptionPane;

import org.json.JSONObject;
import org.json.JSONTokener;

import haxby.map.MapApp;

/**
 * 2/2/2024 This code reads version information relevant to GMA from a single JSON file.
 * @author Alex Strong
 *
 */

public class VersionUtil {
	private static JSONObject versionMap;
	private static URL versionURL;
	
	private VersionUtil() {}
	
	public static void init() {
		versionMap = new JSONObject();
		versionURL = null;
	}
	
	public static void init(String versionURLIn) {
		try {
			versionURL = URLFactory.url(versionURLIn);
			JSONTokener tokener = new JSONTokener(versionURL.openStream());
			versionMap = new JSONObject(tokener);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.exit(1);
		} catch(ConnectException e) {
			if(versionURLIn.contains("dev")) {
				JOptionPane.showMessageDialog(MapApp.anchor, "<html><body><center>Could not connect to the dev server.<br>You must be on the LDEO VPN or physically on the LDEO campus to use development mode.</center></body></html>", "Cannot Access Dev Server", JOptionPane.ERROR_MESSAGE);
			}
			else {
				JOptionPane.showMessageDialog(MapApp.anchor, "<html><body><center>Could not connect to the GMA server.<br>Access may be blocked, or something may be wrong with your internet connection.<br>If this persists, contact us.</center></body></html>", "Cannot Access GMA Server", JOptionPane.ERROR_MESSAGE);
			}
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static Object get(String key) {
		return versionMap.get(key);
	}
	
	public static String getVersion(String key) {
		if("GeoMapApp".equals(key)) {
			return versionMap.getJSONObject(key).getString("version").split("\\s+")[0];
		}
		return versionMap.getJSONObject(key).getString("version");
	}
	
	public static String getReleaseDate(String key) {
		return versionMap.getJSONObject(key).getString("release_date");
	}
}

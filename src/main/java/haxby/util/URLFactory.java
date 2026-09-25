package haxby.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import haxby.map.MapApp;

public class URLFactory {
	public static Map<String, String> subMap = new HashMap<String, String>(); 
	
	public static URL url(String url) throws MalformedURLException {
		url = sub(url);
		if (!url.contains("http") && !url.contains("file:"))
			return Paths.get(url).toUri().toURL();
	
		//check if app.geomapapp.org or www.geomapapp.org URLs are being redirected 
		//from http to https.
		//Can't run this for all URLs as I have found examples where following the redirect
		// does not work, eg, for the Multibeam portal URL 
		//http://www.marine-geo.org/tools/search/GMAPortalStats.php?cmd=multibeam_bathymetry_cmd&sub_name=mercator
		if (url.contains("geomapapp.org")) {			
			return new URL(checkForRedirect(url));
		}
		else {
			return new URL(url);
		}
	}

	public static URL url(URL url, String spec) throws MalformedURLException {
		return new URL(url, spec);
	}

	private static String sub(String url) {
		for (Entry<String, String> entry : subMap.entrySet()) {
			if (url.startsWith(entry.getKey()))
				url = url.replaceFirst(entry.getKey(), entry.getValue());
		}
		return url;
	}
	
	public static void addSubEntry(String key, String value) {
		subMap.put(key, value);
	}
	
	/*
	 * Check if the url has been redirected, and change the url if so.
	 */
	public static String checkForRedirect(String url) {
		if (MapApp.AT_SEA) return url;
		if (url != null && MapApp.BASE_URL != null && MapApp.BASE_URL.matches(MapApp.DEV_URL)) {
			url = url.replace(MapApp.PRODUCTION_URL, MapApp.DEV_URL);
		}
		
//		if (url.contains("geomapapp.org")) {	
//			//for https testing
//			url = url.replace("app.geomapapp", "app-upgrade.geomapapp");
//			url = url.replace("www.geomapapp", "www-upgrade.geomapapp");
//		}
		
		try {
			HttpURLConnection con = (HttpURLConnection) new URL( url ).openConnection();
			if (con.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM || con.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
				url = con.getHeaderField("Location");
			}
		} catch(IOException e) {
//			e.printStackTrace();
		}
		return url;
	}
	
	/*
	 * check whether a URL returns a 200 response code
	 */
	public static boolean checkWorkingURL(String url) {
		if (MapApp.AT_SEA) return true;
		url = checkForRedirect(url);
		try {
			HttpURLConnection con = (HttpURLConnection) new URL( url ).openConnection();
			if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
				return true;
			}
		} catch(IOException e) {
			//e.printStackTrace();
			return false;
		}
		return false;
	}

	public static boolean checkWorkingURL(URL url) {
		return checkWorkingURL(url.toString());
	}
}

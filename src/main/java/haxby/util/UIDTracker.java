package haxby.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import haxby.map.MapApp;

public class UIDTracker {
	
	public static String MARINE_GEO_PATH_URLS = PathUtil.getPath("MARINE_GEO_PATH_URLS",
			MapApp.BASE_URL+"/gma_paths/MARINE_GEO_paths.xml");
	
	public static void sendTrackStat(String uidPath) {
		if (uidPath == null) return;
		
		String dataSetUid = null;
		String dataUid = null;
		URL trackURL;
		// Read the uid file and gather data set or data uid number
		try {
			InputStream in = (haxby.util.URLFactory.url(uidPath)).openStream();
			BufferedReader reader = new BufferedReader( new InputStreamReader(in) );
			String lineIn = reader.readLine();

			while(lineIn!= null){
			if(lineIn.startsWith("<data_set_uid>")) {
				dataSetUid = lineIn.replace("<data_set_uid>", "");
				dataSetUid = dataSetUid.replace("</data_set_uid>", "");
			} else if(lineIn.startsWith("<data_uid>")) {
				dataUid = lineIn.replace("<data_uid>", "");
				dataUid = dataUid.replace("</data_uid>", "");
			}
			 lineIn = reader.readLine();
			}
			//System.out.println("dataSetUid " + dataSetUid + " dataUid " + dataUid);
			reader.close();
			System.gc();

			// Send the required parameters out of application to search tracker tool
			PathUtil.loadNewPaths(MARINE_GEO_PATH_URLS);
			String DOWNLOAD_PING_FILE_PATH = PathUtil.getPath("DOWNLOAD_PING_FILE_PATH",
			"https://www.marine-geo.org/tools/search/GMADownload.php");
			String logString =null;

			// Presidence is data_uid, data_set_uid or default to no value.
			if(dataUid !=null){
				logString = (DOWNLOAD_PING_FILE_PATH + "?data_uid=" + dataUid +
							"&client=GMA&GMAView=1");
			}
			else if (dataSetUid !=null) {
				logString = (DOWNLOAD_PING_FILE_PATH + "?data_set_uid=" + dataSetUid +
				"&client=GMA&GMAView=1");
			}
			
			System.out.println("Log URL: " + logString);
			
			if (logString == null) return;
			trackURL = URLFactory.url(logString);
			InputStream sendStat = trackURL.openStream();
			sendStat.close();

		} catch (MalformedURLException e) {
//			e.printStackTrace();
		} catch (IOException e) {
//			e.printStackTrace();
		}
	}
}

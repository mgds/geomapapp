package haxby.db.fms;

import haxby.map.MapApp;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

public class Earthquakes {
	//The menus should be updated to reflect this
	static String EARTHQUAKE_FOCAL_MECHANISMS_DATA = PathUtil.getPath("PORTALS/EARTHQUAKE_FOCAL_MECHANISMS_DATA_V3",
			MapApp.BASE_URL+"/data/portals/eq_fms_cmt/beachball_plot/parsed_CMT_final_solutions.txt");
	private static ArrayList<Earthquake> all;

	public static boolean isLoaded() {
		return all != null;
	}

	public static boolean load() {
		if (isLoaded()) return true;

		all = new ArrayList<Earthquake>();

		try {
			URL url = null;
			try {
				// <sam>This should be changed EARTHQUAKE_FOCAL_MECHANISMS_DATA, use urlfactory compatible with at sea</sam>
				url = URLFactory.url(EARTHQUAKE_FOCAL_MECHANISMS_DATA);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));;
			reader.readLine();

			String str = null;
			while(true){
			try {
				str = reader.readLine();
				String[]s = str.split("\t");

				Earthquake eq = new Earthquake();
				eq.identifier = s[0];
				eq.date = s[1];
				eq.time = s[2];
				eq.lat = Float.parseFloat(s[3]);
				eq.lon = Float.parseFloat(s[4]);
				eq.depth = Float.parseFloat(s[5]) * -1000;
				eq.magnitude_body = Float.parseFloat(s[6]);
				eq.magnitude_surface = Float.parseFloat(s[7]);
				eq.mw = Float.parseFloat(s[8]);
				eq.strike1 = Float.parseFloat(s[9]);
				eq.dip1 = Float.parseFloat(s[10]);
				eq.rake1 = Float.parseFloat(s[11]);
				eq.strike2 = Float.parseFloat(s[12]);
				eq.dip2 = Float.parseFloat(s[13]);
				eq.rake2 = Float.parseFloat(s[14]);
				all.add(eq);
			}catch(Exception noMoreLines){break;}
			}
		}
		catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	public static class Earthquake {
		public String identifier,date, time;
		public float lat, 
						lon,
						depth,
						magnitude_body,
						magnitude_surface,
						mw,
						strike1,
						dip1,
						rake1,
						strike2,
						dip2,
						rake2;
	}

	public static List getEQs() {
		return all;
	}

	public static void dispose() {
		if (all != null)
			all.clear();
		all = null;
	}
}
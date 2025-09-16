package haxby.worldwind.tilers;

import haxby.worldwind.tilers.GeoMapAppToWorldWindTiler;

/**
 * Simply changes the references to the location of the Tiles to a local file: instead of 
 * going through and http connetion.
 */
public class LocalGeoMapAppTiler {
	public static void main (String [] args) throws Exception {
		haxby.map.MapApp.TEMP_BASE_URL = "file:////home/geomapapp/apache/htdocs/";
		GeoMapAppToWorldWindTiler.main(args);
	}
}

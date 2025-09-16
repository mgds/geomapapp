package haxby.worldwind;

import gov.nasa.worldwind.Configuration;

public class WWWrapper {
	public static void main( String[] args) {
		if (Configuration.isMacOS()) {
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "GeoMapApp");
		}
		com.Ostermiller.util.Browser.init();

		WWMapApp.main(args);
	}
}

package haxby.worldwind;

import org.geomapapp.map.MapPlace;

public class WWMapPlace extends MapPlace {
	
	public double zoom2;
	public double heading;
	public double pitch;
	public double ve;

	public WWMapPlace( MapPlace parent, String name, double lon, double lat, double zoom,
			double pitch, double heading, double zoom2, double ve) {
		super( parent, name );
		leaf = true;
		this.lon = lon;
		this.lat = lat;
		this.zoom = zoom;
		selected = false;
		this.pitch = pitch;
		this.heading = heading;
		this.zoom2 = zoom2;
		this.ve = ve;
	}
}

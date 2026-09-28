package haxby.map;

public class MapAppTest extends MapApp {
	public MapAppTest() {				
		super();
		haxby.dig.Digitizer dig = new haxby.dig.Digitizer( map );
		tools.getTools().add( dig.getPanel(), 0 );
		map.addOverlay( dig );
	}
	public static void main( String[] args) {
		new MapAppTest();
	}
}

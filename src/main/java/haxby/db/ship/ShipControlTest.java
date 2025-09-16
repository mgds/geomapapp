package haxby.db.ship;

public class ShipControlTest {
	public static void main(String[] args) {
		ShipControl scTest = new ShipControl();
		scTest.parseXMLFile(args[0]);		
		scTest.parseData();
		scTest.generateControl();
		
	}

}

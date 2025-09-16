package haxby.worldwind.layers;

public interface QueryableGridLayer {
	public double getValueAt(double lat, double lon);
	public String getValueUnits();
}

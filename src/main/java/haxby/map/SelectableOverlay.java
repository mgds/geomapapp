package haxby.map;

public abstract interface SelectableOverlay extends Overlay {
	public abstract Object getSelectionObject(double x, double y, double distanceSq);
	public abstract void selectObject( Object selectedObject );
}

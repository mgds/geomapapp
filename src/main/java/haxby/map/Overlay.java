package haxby.map;

/**
 	Overlay is called by map objects in paintComponent methods.
*/
public abstract interface Overlay {
	
	/**
		Draws.
		@param What to draw.
	*/
	public void draw(java.awt.Graphics2D g);
	public default boolean shouldShow() {
		return true;
	} 
}

package haxby.db.pmel;

/**
 * @author William F. Haxby
 * @since 1.1.6
 */
public class PMELEvent {
	public int time;
	public float	x,
					y,
					mag;

	public PMELEvent( int time, float x, float y, float mag) {
		this.time = time;
		this.x = x;
		this.y = y;
		this.mag = mag;
	}
}

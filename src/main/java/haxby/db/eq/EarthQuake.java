package haxby.db.eq;

public class EarthQuake {
	public float x;
	public float y;
	public int time;
	public short mag;
	public short dep;
	public EarthQuake( int time, float x, float y, short dep, short mag) {
		this.time = time;
		this.x = x;
		this.y = y;
		this.dep = dep;
		this.mag = mag;
	}
}

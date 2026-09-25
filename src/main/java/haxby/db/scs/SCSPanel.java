package haxby.db.scs;

public class SCSPanel implements Comparable {
	java.awt.image.BufferedImage image;
	int[] bounds;		// {x1, x2, y1, y2}
	String rasterfile;
	double[] tt;		// travel times at (y1, y2)
	double[] time;		// seconds since 1/1/70 at (x1, x2)
	double scale;
	public SCSPanel( String rasfile, int[] bounds ) {
		rasterfile = rasfile;
		this.bounds = bounds;
		tt = new double[2];
		time = new double[2];
		scale = 1.;
	}
	public int compareTo(Object o) {
		SCSPanel p = (SCSPanel)o;
		if( p.time[0]>time[0] ) return -1;
		else if( p.time[0]<time[0] ) return 1;
		else return 0;
	}
}
package org.geomapapp.geom;

import java.util.*;

public class PolygonFill {
	public static int[][] fill( double[][] xy ) {
		if( xy.length<2 ) return new int[0][0];
		int min=0;
		int max=0;
		for(int k=0 ; k<xy.length ; k++) {
			if( xy[k][1]==Math.rint(xy[k][1]) ) xy[k][1]+= 1.e-10;
			if( xy[k][1]<xy[min][1] )min=k;
			else if( xy[k][1]>xy[max][1] )max=k;
		}
		int y1 = (int)Math.ceil(xy[min][1]);
		int y2 = (int)Math.ceil(xy[max][1]);
		Comparator comp = new Comparator() {
			public int compare( Object o1, Object o2) {
				return ((Double)o1).compareTo((Double)o2);
			}
			public boolean equals(Object o) {
				return this==o;
			}
		};
		Vector endpts = new Vector();
		for( int y=y1 ; y<y2 ; y++) {
			Vector pts = new Vector();
			for( int k=0 ; k<xy.length ; k++ ) {
				int k1 = (k+1)%xy.length;
				if( (xy[k][1]-y)*(xy[k1][1]-y) >0.) continue;
				double d = xy[k][0] + (xy[k1][0]-xy[k][0])
					* (y-xy[k][1]) / (xy[k1][1]-xy[k][1]);
				pts.add(new Double(d));
			}
			Collections.sort(pts, comp);
			if( pts.size()%2 != 0) {
				int[][] points = new int[0][];
				return points;
			}
			for( int k=0 ; k<pts.size() ; k+=2) {
				int[] seg = new int[3];
				seg[0] = y;
				double d = ((Double)pts.get(k)).doubleValue();
				seg[1] = (int)Math.ceil( d );
				d = ((Double)pts.get(k+1)).doubleValue();
				seg[2] = (int)Math.ceil( d );
				endpts.add(seg);
			}
		}
		int[][] pts = new int[endpts.size()][];
		for( int k=0 ; k<endpts.size() ; k++) pts[k] = (int[])endpts.get(k);
		return pts;
	}
}
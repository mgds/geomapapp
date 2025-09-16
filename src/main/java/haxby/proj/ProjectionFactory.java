package haxby.proj;

import haxby.map.MapApp;

public class ProjectionFactory {
	static Mercator[] merc = null;
	static int[] nPer360 = null;
	public static Mercator getMercator(int pixels_per_360) {
		if(merc!=null) {
			for(int i=0 ; i<merc.length ; i++) {
				if(nPer360[i]==pixels_per_360) return merc[i];
			}
		}
		Mercator m = new Mercator(0d, 0d, 
				pixels_per_360, 0, MapApp.DEFAULT_LONGITUDE_RANGE);
		if(merc==null) {
			merc = new Mercator[1];
			nPer360 = new int[1];
			merc[0] = m;
			nPer360[0] = pixels_per_360;
		} else {
			int n = merc.length;
			Mercator[] tmp = new Mercator[n+1];
			System.arraycopy(merc, 0, tmp, 1, n);
			tmp[0] = m;
			merc = tmp;
			int[] itmp = new int[n+1];
			System.arraycopy(nPer360, 0, itmp, 1, n);
			itmp[0] = pixels_per_360;
			nPer360 = itmp;
		}
		return m;
	}
}
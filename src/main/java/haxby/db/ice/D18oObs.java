package haxby.db.ice;

import haxby.util.URLFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
public class D18oObs {
	static float[][] o18;
	static boolean loaded = false;
	public D18oObs() {
	}
	public static boolean load() {
		if( loaded ) return true;
		try {
			URL url = URLFactory.url(haxby.map.MapApp.TEMP_BASE_URL + "arctic/ice/o18wobs.bin");
			DataInputStream in = new DataInputStream(url.openStream());
			o18 = new float[46][72];
			for( int y=0 ; y<46 ; y++ ) {
				for(int x=0 ; x<72 ; x++ ) {
					o18[y][x] = in.readFloat();
				}
			}
			loaded = true;
		} catch (IOException ex) {
			o18=null;
			loaded = false;
		}
		return loaded;
	}
	public static float getValue( double longitude, double latitude ) {
		if( !loaded ) {
			if( !load() ) return Float.NaN;
		}
		double lon = longitude;
		double lat = latitude;
		lon += 177.5;
		lon /= 5.;
		lat += 90.;
		lat /= 4.;
		while(lon<0) lon += 72.;
		while(lon>=72.) lon -= 72.;
		int x = (int)Math.floor(lon);
		int x1 = x+1;
		if(x1==72) x1=0;
		int y = (int)Math.floor(lat);
		if(y==45)y=44;
		float dx = (float)(lon-x);
		float dy = (float)(lat-y);
		float dxy = dx*dy;
		float answer = Float.NaN;
	try {
		answer = o18[y][x] * (1f-dx-dy+dxy)
			+ o18[y][x1] * (dx-dxy)
			+ o18[y+1][x] * (dy-dxy)
			+ o18[y+1][x1] * dxy;
	} catch( ArrayIndexOutOfBoundsException ex) {
		System.out.println( longitude +"\t"+ latitude +"\t"+ x +"\t"+ y);
	}
		if(Float.isNaN( answer ) ) answer = nearestValue( longitude, latitude );
		return answer;
	}
	public static float nearestValue( double lon, double lat ) {
		if( !loaded ) {
			if( !load() ) return Float.NaN;
		}
		lon += 177.5;
		lon /= 5.;
		lat += 90.;
		lat /= 4.;
		int x = (int)Math.rint(lon);
		while(x<0) x += 72;
		while(x>=72) x -= 72;
		int y = (int)Math.rint(lat);
		return o18[y][x];
	}
}

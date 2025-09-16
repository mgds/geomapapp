package haxby.db.ice;

import haxby.util.URLFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;

public class HeatFlux {
	static float[][][] q;
	static boolean loaded = false;
	public HeatFlux() throws IOException {
		if( !loaded )load();
	}
	public static float getFlux( double lon, double lat, int year ) {
		if( year<1984 || year>1994 ) return Float.NaN;
		if( !loaded ) try {
			load();
		} catch (IOException ex) {
			return Float.NaN;
		}
		year -= 1984;
		double r = .25*637.*Math.sin( Math.toRadians( 45. - lat*.5 ) );
		lon = Math.toRadians(lon-35.);
		double x = 62.5 + r*Math.cos( lon );
		double y = 54.5 + r*Math.sin( lon );
		int i=(int)Math.floor(x);
		int j=(int)Math.floor(y);
		float[][] f = q[year];
		try {
			if( Float.isNaN( f[i][j] ) ) return Float.NaN;
			if( Float.isNaN( f[i+1][j] ) ) return Float.NaN;
			if( Float.isNaN( f[i][j+1] ) ) return Float.NaN;
			if( Float.isNaN( f[i+1][j+1] ) ) return Float.NaN;
			float dx = (float)(x-i);
			float dy = (float)(y-j);
			return f[i][j] * (1f-dx-dy+dx*dy)
				+ f[i+1][j] * (dx-dx*dy)
				+ f[i][j+1] * (dy-dx*dy)
				+ f[i+1][j+1] * dx*dy;
		} catch (ArrayIndexOutOfBoundsException ex ) {
			return Float.NaN;
		}
	}
	static void dispose() {
		q = null;
		loaded = false;
	}
	static void load() throws IOException {
		q = new float[11][130][102];
		URL url = URLFactory.url(haxby.map.MapApp.TEMP_BASE_URL + "arctic/ice/heatflux");
		DataInputStream in = new DataInputStream(url.openStream());
		for( int year=0 ; year<11 ; year++ ) {
				for(int y=0 ; y<102 ; y++) {
			for(int x=0 ; x<130 ; x++) {
					q[year][x][y] = in.readFloat();
				}
			}
		}
		in.close();
		loaded = true;
	}
}

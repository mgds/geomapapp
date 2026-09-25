package haxby.db.ice;

import haxby.proj.PolarStereo;
import haxby.proj.Projection;
import haxby.util.URLFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;

public class IBCAO {
	static PolarStereo proj = null;
	public IBCAO() {
	}
	public static Projection getProjection() {
		if(proj==null) {
			proj = new PolarStereo( new java.awt.Point(1161, 1161),
					0., 2500., 75., PolarStereo.NORTH,
					PolarStereo.WGS84);
		}
		return proj;
	}
	public static boolean[] getMask() {
		boolean[] mask = new boolean[2323*2323];
		try {
			URL url = URLFactory.url(haxby.map.MapApp.TEMP_BASE_URL + "arctic/ice/IBCAO.mask");
			DataInputStream in = new DataInputStream(url.openStream());
			int i=0;
			boolean land = false;
			while( i<2323*2323-1 ) {
				int n = in.readInt();
				for(int k=0 ; k<n ; k++) mask[i++]=land;
				land = !land;
			}
		} catch(IOException ex) {
			ex.printStackTrace();
			for( int i=0 ; i<mask.length ; i++) mask[i]=false;
		}
		return mask;
	}
	public static boolean[] getMask600() {
		boolean[] mask = new boolean[600*600];
		try {
			URL url = URLFactory.url(haxby.map.MapApp.TEMP_BASE_URL + "arctic/ice/landmask.600");
			DataInputStream in = new DataInputStream(url.openStream());
			int i=0;
			boolean land = false;
			while( i<600*600-1 ) {
				int n = in.readInt();
				for(int k=0 ; k<n ; k++) mask[i++]=land;
				land = !land;
			}
		} catch(IOException ex) {
			ex.printStackTrace();
			for( int i=0 ; i<mask.length ; i++) mask[i]=false;
		}
		return mask;
	}
	public static void main(String[] args) {
		boolean[] mask = IBCAO.getMask();
	}
}

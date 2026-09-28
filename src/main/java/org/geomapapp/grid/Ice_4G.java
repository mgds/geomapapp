package org.geomapapp.grid;

import org.geomapapp.geom.*;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.net.URL;

public class Ice_4G {
	public static final String BASE = haxby.map.MapApp.TEMP_BASE_URL + "arctic/topo/";
	Grid2D.Short diff;
	Grid2D.Boolean ice;
	int kyBP;
	public Ice_4G() throws IOException {
		kyBP = -1;
		setKYBP( 0 );
	}
	public double getDiff( double x, double y) {
		if( kyBP==0 ) return 0.;
		return diff.valueAt(x,y);
	}
	public boolean isIce(double x, double y) {
		return ice.booleanValue((int)Math.rint(x),(int)Math.rint(y));
	}
	public void setKYBP( int kyBP ) throws IOException {
		if( this.kyBP==kyBP )return;
		PolarStereo proj = new PolarStereo( new java.awt.Point(80, 80),
			0., 40000., 75., PolarStereo.NORTH,
			PolarStereo.WGS84);
		this.kyBP = kyBP;
		if( kyBP==0 )diff = null;
		else {
			TileIO.Short tiler = new TileIO.Short(
					proj, BASE+"paleo/kyBP_"+kyBP, 160, 0);
			diff = (Grid2D.Short)tiler.readGridTile(0, 0);
		}
		TileIO.Boolean tiler = new TileIO.Boolean(
				proj, BASE+"paleo/kyBP_"+kyBP, 160, 0);
		ice = (Grid2D.Boolean)tiler.readGridTile(0, 0);
	}
}

package org.geomapapp.grid;

import haxby.util.URLFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

public class Ice4G {
	public static final String BASE = haxby.map.MapApp.TEMP_BASE_URL + "arctic/topo/";
	short[] diff;
	boolean[] ice;
	int kyBP;
	public Ice4G() throws IOException {
		kyBP = -1;
		setKYBP( 0 );
	}
	public double getDiff( double lon, double lat) {
		if( kyBP==0 )return 0.;
		double x = lon+179.5;
		while( x<0 ) x+=360;
		while( x>=360 )x-=360;
		double y = 89.5-lat;
		int x1 = (int)Math.floor(x);
		int x2 = (x1+1)%360;
		int y1 = (int)Math.floor(y);
		if( y1<0 )y1++;
		int y2 = y1+1;
		if( y2>179 ) {
			y2=179;
			y1=178;
		}
//System.out.println( lon +"\t"+ lat +"\t"+ x1 +"\t"+ y1);
		x = x-x1;
		y = y-y1;
		return diff[x1+y1*360]*(1.-x-y+x*y) 
				+ diff[x2+y1*360]*(x-x*y)
				+ diff[x1+y2*360]*(y-x*y)
				+ diff[x2+y2*360]*x*y;
	}
	public boolean isIce(double lon, double lat) {
		int x = (int)Math.floor( lon+180 );
		while( x<0 ) x+=360;
		while( x>=360 )x-=360;
		int y = (int)Math.floor( 90. - lat );
// System.out.println( lon +"\t"+ lat +"\t"+ x +"\t"+ y +"\t"+ ice[x+y*360]);
		return ice[x+y*360];
	}
	public void setKYBP( int kyBP ) throws IOException {
		if( this.kyBP==kyBP )return;
		this.kyBP = kyBP;
		if( kyBP==0 )diff = null;
		else {
			URL url = URLFactory.url(BASE +"paleo/diff."+kyBP+".gz");
			DataInputStream in = new DataInputStream(
				new GZIPInputStream( url.openStream() ));
			int k=0;
			diff = new short[180*360];
			for( int y=0 ; y<180 ; y++ ) {
				for( int x=0 ; x<360 ; x++) diff[k++]=in.readShort();
			}
			in.close();
		}
		URL url = URLFactory.url(BASE +"paleo/is_ice."+kyBP+".gz");
		DataInputStream in = new DataInputStream(
			new GZIPInputStream( url.openStream() ));
		int k=0;
		ice = new boolean[180*360];
		int kount=0;
		for( int y=0 ; y<180 ; y++ ) {
			for( int x=0 ; x<360 ; x++) {
				short i = in.readShort();
				ice[k++] = i!=0;
				if( i!=0 ) kount++;
			}
		}
	//	System.out.println( kount);
		in.close();
	}
}

package haxby.db.ice;

import haxby.map.*;
import haxby.grid.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class D18OGrid implements Overlay {
	MapOverlay overlay;
	XMap map;
	IceDB db;
	boolean plot;
	boolean[] land = null;
	static float[] z;
	static boolean zInit=false;
	public D18OGrid(XMap map, IceDB db) {
		this.map = map;
		this.db = db;
		plot = false;
		overlay = new MapOverlay(map);
	}
	public void setPlot(boolean tf) {
		plot = tf;
	//	map.repaint();
	}
	public void draw(Graphics2D g) {
	//	if( !plot )return;
		overlay.draw(g);
	}
	public void grid() {
		z = new float[30*30];
		float[] w = new float[30*30];
		grid( z, w, 1900, 2004, true);
	}
	public void grid( float[] z, float[] w, int yr1, int yr2, boolean complete ) {
		if(land==null)land = IBCAO.getMask600();
		float[][] obs = db.obs.xyd;
		for( int i=0 ; i<obs.length ; i++) {
			if( obs[i][5]<(float)yr1 )continue;
			if( obs[i][5]>(float)yr2 )continue;
			float x = obs[i][0]/20f;
			float y = obs[i][1]/20f;
			float val = obs[i][3];
			int ix = (int)Math.floor(x);
			int iy = (int)Math.floor(y);
			int ix1 = ix-6;
			if(ix1<0) ix1=0;
			int ix2 = ix+7;
			if( ix2>29 )ix2=29;
			int iy1 = iy-6;
			if(iy1<0) iy1=0;
			int iy2 = iy+7;
			if( iy2>29 )iy2=29;
			for( iy=iy1 ; iy<=iy2 ; iy++ ) {
				double rr = Math.pow(iy-(double)y,2);
				for( ix=ix1 ; ix<=ix2 ; ix++ ) {
					double r2 = rr+ Math.pow(ix-(double)x,2);
					if(r2>50.)continue;
					float wt = (float)Math.exp(-r2/2);
					int kk = ix + 30*iy;
					z[kk] += wt*val;
					w[kk] += wt;
				}
			}
		}
		if( !complete ) return;
		for( int i=0 ; i<z.length ; i++) {
			if( w[i]<.001f ) z[i]=Float.NaN;
			else z[i] /= w[i];
		}
		zInit = true;
		int gray = Color.gray.getRGB();
		BufferedImage image = new BufferedImage(600, 600, BufferedImage.TYPE_INT_ARGB);
		for( int y=0 ; y<600 ; y++) {
			double yy = y/20.;
			for( int x=0 ; x<600 ; x++) {
				double xx = x/20.;
				double val = Interpolate.bicubic( z, 30, 30, xx, yy);
				if( land[x+600*y] ) {
					image.setRGB(x,y,0);
				} else if(Double.isNaN(val)) {
					image.setRGB(x,y, gray);
				} else {
				//	image.setRGB(x,y, IceCore.getColor( (float)val+1f ).getRGB() );
					image.setRGB(x,y, IceCore.getColor( (float)val+2f ).getRGB() );
				}
			}
		}
		overlay.setImage( image, 0., 0., 1.);
	}
	public static float valueAt( double x, double y ) {
		if( !zInit ) return Float.NaN;
		double xx = x/20.;
		double yy = y/20.;
		double val = Interpolate.bicubic( z, 30, 30, xx, yy);
		if( Double.isNaN(val) ) return Float.NaN;
		return (float)val;
	}
}
//	public void setImage( BufferedImage im, double x0, double y0, double scale) {

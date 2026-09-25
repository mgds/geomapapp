package haxby.db.ice;

import java.util.*;
import java.awt.geom.*;

public class IceGrowth {
	static double L=3.e08;
	static double K_ICE = 2.;
	public static double[] zHistory( float[] lon, float[] lat, float[] T,
			double h0, double Q, double kSnow, 
			int year, int month, int day, double meltRate ) {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.set( year, month-1, day );
		double[] hIce = new double[ lon.length ];
		hIce[0] = h0;
		boolean computeheat = (Q<0.);
		for(int k=1 ; k<lon.length ; k++) {
			Point2D.Float p = new Point2D.Float(lon[k],lat[k]);
			cal.add( cal.DATE, -1 );
			month = cal.get( cal.MONTH );
//if(cal.get(cal.DATE)==1) {
//	System.out.println( lon[k-1] +"\t"+ lat[k-1] +"\t"+ T[k] +"\t"+ hIce[k-1] +"\t"+ h0 +"\t"+
//		Snow.thickness( p, month ) +"\t"+ Snow.waterEquivalent( p, month ) +"\t"+ Snow.conductivity( p, month ));
//}
			if( T[k]>-1.8f ) {
				h0 += meltRate;
				hIce[k] = hIce[k-1];
				continue;
			}
			double hSnow = Snow.thickness( p, month );
		//	double kSnow = .33;
			if( hSnow<= 0. ) {
				hSnow = 0.;
		//	} else {
		//		kSnow = Snow.conductivity( p, month );
		//		if(kSnow<.1) kSnow = .1;
		//		else if(kSnow>1.) kSnow = 1.;
			}
			if( computeheat ) {
				year = cal.get( cal.YEAR );
				float q = HeatFlux.getFlux( (double)lon[k], (double)lat[k], year);
				if( Float.isNaN(q) ) Q=2.;
				else Q = (double)q;
			}
			double dh = ((-1.8-(double)T[k]) * K_ICE*kSnow/(K_ICE*hSnow+kSnow*h0) - Q) / L;
			if( dh<0 ) {
				hIce[k] = hIce[k-1];
			} else {
				dh *= -86400.;
				h0 += dh;
				hIce[k] = hIce[k-1]+dh;
			}
			if(hIce[k]<0) {
				double[] tmp = new double[k];
				System.arraycopy(hIce,0,tmp,0,k-1);
				return tmp;
			}
		}
		return hIce;
	}
	public static float[][] zHistory2( float[] lon, float[] lat, float[] T,
			double h0, double Q, double kSnow, 
			int year, int month, int day, double meltRate ) {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.set( year, month-1, day );
		float[][] hIce = new float[ lon.length ][2];
		hIce[0][0] = 0f;
		hIce[0][1] = (float)h0;
		boolean computeheat = (Q<0.);
		for(int k=1 ; k<lon.length ; k++) {
			Point2D.Float p = new Point2D.Float(lon[k],lat[k]);
			cal.add( cal.DATE, -1 );
			month = cal.get( cal.MONTH );
			float hSnow = 0f;
			if( T[k]>-1.8f ) {
				hIce[k][0] = hIce[k-1][0]-(float)meltRate;
			} else {
				hSnow = (float)Snow.thickness( p, month );
				if( hSnow<= 0f ) hSnow = 0f;
				hIce[k][0] = hIce[k-1][0];
			}
			if( computeheat ) {
				year = cal.get( cal.YEAR );
				float q = HeatFlux.getFlux( (double)lon[k], (double)lat[k], year);
				if( Float.isNaN(q) ) Q=2.;
				else Q = (double)q;
			}
			h0 = hIce[k-1][1] - hIce[k-1][0];
			double dh = ((-1.8-(double)T[k]) * K_ICE*kSnow/(K_ICE*hSnow+kSnow*h0) - Q) / L;
		//	if( T[k]>-1.8f ) dh = -Q/L;
			dh *= -86400.;
		//	if( dh>0 ) dh=0;
			hIce[k][1] = hIce[k-1][1] + (float)dh;
			if(hIce[k][1]<=0f) {
				float[][] tmp = new float[k][2];
				System.arraycopy(hIce,0,tmp,0,k-1);
				return tmp;
			}
		}
		return hIce;
	}
}

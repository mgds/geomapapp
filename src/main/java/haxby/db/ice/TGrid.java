package haxby.db.ice;

import java.io.*;
import java.awt.geom.Point2D;

public class TGrid {
	static double x0 = 25;
	static double y0 = 21;
	static double scale = 89.860177841254;
	static short[][][] t = null;
	static int year = -1;
	public static float temp( int yr, int day, Point2D point) {
		if(yr<79 || yr>98) return 999f; //Float.NaN;
		Point2D p = getMapXY(point);
		int x = (int)Math.floor(p.getX());
		if(x<0 || x>=46) return 998f; //Float.NaN;
		int y = (int)Math.floor(p.getY());
		if(y<0 || y>=40) return 997f; //Float.NaN;
		if(year!=yr) try {
			readGrids(yr);
		} catch (IOException ex) {
			return 996f; //Float.NaN;
		}
		if( t==null ) return 995f; //Float.NaN;
		double dx = p.getX()-x;
		double dy = p.getY()-y;
		double dxy = dx*dy;
		int d = day-1;
		float T = .01f * (float) ( (double)t[d][y][x]*(1d-dx-dy+dxy)
				+ (double)t[d][y][x+1]*(dx-dxy)
				+ (double)t[d][y+1][x]*(dy-dxy)
				+ (double)t[d][y+1][x+1]*(dxy) );
		return T;
	}
	static void readGrids(int yr) throws IOException {
		year = yr;
		int nDay = (year%4==0) ? 366 : 365;
		t = new short[nDay][40][46];
		FileInputStream fin = null;
		fin = new FileInputStream(
			"/scratch/ridgembs/bill/arctic/temperature/temp/temp." +(year) );
		DataInputStream in = new DataInputStream(
				new BufferedInputStream(fin));
		for(int d=0 ; d<nDay ; d++ ) {
			for( int y=0 ; y<40 ; y++ ) {
				for( int x=0 ; x<46 ; x++ ) {
					t[d][y][x] = in.readShort();
				}
			}
		}
		in.close();
		fin.close();
	}
	public static Point2D getMapXY( Point2D refXY ) {
		double r = scale * Math.sqrt( 1d - Math.sin(Math.toRadians(refXY.getY())) );
		Point2D.Double p = new Point2D.Double();
		p.x = x0 + r * Math.cos( Math.toRadians( refXY.getX() ) );
		p.y = y0 - r * Math.sin( Math.toRadians( refXY.getX() ) );
		return p;
	}
	public static Point2D getRefXY( Point2D mapXY ) {
		double r = Math.sqrt( Math.pow(mapXY.getX(),2) + Math.pow(mapXY.getY(),2) );
		Point2D.Double p = new Point2D.Double();
		p.x = Math.toDegrees( Math.atan2( -mapXY.getY(), mapXY.getX() ) );
		p.y = Math.toDegrees( Math.asin( 1 - Math.pow(r/scale, 2) ) );
		return p;
	}
}

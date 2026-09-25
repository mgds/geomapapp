package haxby.nav;

import haxby.proj.*;
import haxby.util.XYZ;
import java.awt.geom.Point2D;
import java.util.Vector;

public class PingNav {
	String cruise;
	String id;
	int nPing;
	double[] x;
	double[] y;
	int[] time;
	short[] heading;
	short[] course;
	boolean[] edit;
	Vector control;
	double[] xp;
	double[] yp;
	public PingNav(String cruise, String id, int nPing) {
		this.cruise = cruise;
		this.id = id;
		this.nPing = nPing;
		x = new double[nPing];
		y = new double[nPing];
		time = new int[nPing];
		heading = new short[nPing];
		course = new short[nPing];
		edit = new boolean[nPing];
		for( int i=0 ; i<nPing ; i++) edit[i] = true;
		control = null;
	}
	public void addPoint(int p, int time, double lon, double lat,
				float heading, float course) {
		this.time[p] = time;
		x[p] = lon;
		y[p] = lat;
		this.course[p] = (short)course;
		this.heading[p] = (short)heading;
		if(x[p]==0 && y[p]==0) {
			edit[p]=true;
			return;
		}
		if(x[p]>=360d || x[p]<=-360) {
			edit[p]=true;
			return;
		}
		if(y[p]>=90d || y[p]<=-90) {
			edit[p]=true;
			return;
		}
		if( heading>360 || heading<-360) {
			edit[p]=true;
			return;
		}
		if( course>360 || course<-360) {
			edit[p]=true;
			return;
		}
		edit[p] = false;
	}
/**
*  returns true if (1) there are 0 or 1 non-edited points in this
*  nav object; (2) time difference between succesive points is <= 0;
*  or (3) if the speed defined by succesive points is greater than
*  maxSpeed meters/second.
*/
	public boolean speedTest(double maxSpeed) {
		int i1 = 0;
		while( i1<nPing && edit[i1] )i1++;
		if(i1>=nPing-1) return true;
		int i2 = i1+1;
		while( i2<nPing && edit[i2] )i2++;
		if(i2==nPing)return true;
		XYZ p1 = XYZ.LonLat_to_XYZ(new Point2D.Double(x[i1], y[i1]));
		XYZ p2;
		do {
			if(time[i2]<=time[i1]) return true;
			p2 = XYZ.LonLat_to_XYZ(new Point2D.Double(x[i2], y[i2]));
			if(speed(p1, time[i1], p2, time[i2]) > maxSpeed) return true;
			p1.x = p2.x;
			p1.y = p2.y;
			p1.z = p2.z;
			i1 = i2;
			i2++;
			while( i2<nPing && edit[i2] )i2++;
		} while(i2<nPing);
		return false;
	}
	public static double speed( XYZ p1, int t1, XYZ p2, int t2) {
		double dist = Math.toDegrees(Math.acos( p1.dot(p2) ))*111195;
		return dist / (double)(t2-t1);
	}
	public void setNavBounds(double[] wesn) {
		for( int i=0 ; i<nPing ; i++) {
			if(edit[i]) continue;
			if( y[i]>wesn[3] || y[i]<wesn[2] ) {
				edit[i] = true;
				continue;
			}
			while(x[i]>wesn[1]) x[i] -= 360d;
			while(x[i]<wesn[0]) x[i] += 360d;
			if( x[i]>wesn[1] ) edit[i]=true;
		}
	}
	public void computeControlPoints(Projection proj) {
		xp = new double[nPing];
		yp = new double[nPing];
		int i;
		for( i=0 ; i<nPing ; i++) {
			if(edit[i]) continue;
			Point2D.Double p = new Point2D.Double(x[i], y[i]);
			p = (Point2D.Double)proj.getMapXY(p);
			xp[i] = p.x;
			yp[i] = p.y;
		}
		int i1 = 0;
		while( i1<nPing && edit[i1] ) i1++;
		int i2 = nPing-1;
		while( i2>i1 && edit[i2]) i2--;
		control = new Vector();
		if(i1==nPing) return;
		control.add(new IndexControlPoint(x[i1], y[i1], time[i1], i1));
		if(i2<=i1)return;
		int imax = i2;
		control.add(new IndexControlPoint(x[i2], y[i2], time[i2], i2));
		int k = 1;
		do {
			i2 = ((IndexControlPoint)control.get(k)).index;
//	System.out.println(i1 + "\t"+ i2);
			while( (i=segment(i1,i2)) != -1) {
				control.add(k, new IndexControlPoint(x[i], y[i], time[i], i));
				i2 = i;
			}
			k++;
			i1 = i2;
		} while( i1<imax );
	}
	int segment( int i1, int i2) {
		double dt = 1d/(double)(time[i2]-time[i1]);
		double dxdt = (xp[i2]-xp[i1]) * dt;
		double dydt = (yp[i2]-yp[i1]) * dt;
		int k=-1;
		double max = 1;
		double dx, dy, d;
		for( int i=i1+1 ; i<i2 ; i++) {
			if(edit[i]) continue;
			dt = (double)(time[i]-time[i1]);
			dx = xp[i] - (xp[i1] + dt*dxdt);
			dy = yp[i] - (yp[i1] + dt*dydt);
			d = dx*dx + dy*dy;
			if(d>max) {
				max = d;
				k = i;
			}
		}
//	System.out.println(i1 +"\t"+ i2 +"\t"+ max);
		return k;
	}
	public int getNumPings() {
		return nPing;
	}
	public int getNumEdits() {
		int count = 0;
		for(int i=0 ; i<nPing ; i++ ) {
			if(edit[i]) count++;
		}
		return count;
	}
	public Vector getControlPoints() {
		return control;
	}
}

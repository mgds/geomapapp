package haxby.nav;

import haxby.proj.*;
import haxby.util.XYZ;
import java.awt.geom.Point2D;
import java.util.Vector;

public class Nav {
	String cruise;
	Vector nav;
	Vector control;
	double[] xp, yp;
	int[] time;
	Projection proj;
	public Nav(String cruise) {
		this.cruise = cruise;
		nav = new Vector();
		control = null;
	}
	public void addPoint(int time, double lon, double lat) {
		nav.add( new ControlPoint( lon, lat, time ));
	}
	public int getSize() {
		return nav.size();
	}
	public void computeTimeControlPoints(Projection proj) {
		int npt = nav.size();
		xp = new double[npt];
		yp = new double[npt];
		time = new int[npt];
		int i;
		ControlPoint cpt;
		for( i=0 ; i<npt ; i++) {
			cpt = (ControlPoint)nav.get(i);
			Point2D.Double p = new Point2D.Double(cpt.x, cpt.y);
			p = (Point2D.Double)proj.getMapXY(p);
			xp[i] = p.x;
			yp[i] = p.y;
			time[i] = cpt.time;
		}
		int i1 = 0;
		int i2 = npt-1;
		control = new Vector();
		if(i1==npt) return;
		cpt = (ControlPoint)nav.get(i1);
		control.add(new IndexControlPoint(cpt.x, cpt.y, time[i1], i1));
		if(i2<=i1)return;
		int imax = i2;
		cpt = (ControlPoint)nav.get(i2);
		control.add(new IndexControlPoint(cpt.x, cpt.y, time[i2], i2));
		int k = 1;
		do {
			i2 = ((IndexControlPoint)control.get(k)).index;
			while( (i=timeSegment(i1,i2)) != -1) {
				cpt = (ControlPoint)nav.get(i);
				control.add(k, new IndexControlPoint(cpt.x, cpt.y, time[i], i));
				i2 = i;
			}
			k++;
			i1 = i2;
		} while( i1<imax );
	}
	int timeSegment( int i1, int i2) {
		double dt = 1d/(double)(time[i2]-time[i1]);
		double dxdt = (xp[i2]-xp[i1]) * dt;
		double dydt = (yp[i2]-yp[i1]) * dt;
		int k=-1;
		double max = 1;
		double dx, dy, d;
		for( int i=i1+1 ; i<i2 ; i++) {
			dt = (double)(time[i]-time[i1]);
			dx = xp[i] - (xp[i1] + dt*dxdt);
			dy = yp[i] - (yp[i1] + dt*dydt);
			d = dx*dx + dy*dy;
			if(d>max) {
				max = d;
				k = i;
			}
		}
		return k;
	}
	public void computeControlPoints(Projection proj, double xWrap, double dxtest) {
		computeControlPoints(proj, xWrap, dxtest, new javax.swing.JToggleButton(""));
	}
	public void computeControlPoints(Projection proj, double xWrap, double dxtest, javax.swing.JToggleButton abort) {
		this.proj = proj;
		dxtest = dxtest*dxtest;
		int npt = nav.size();
		double wrapTest = xWrap/2.;
		xp = new double[npt];
		yp = new double[npt];
		int i;
		ControlPoint cpt;
		for( i=0 ; i<npt ; i++) {
			cpt = (ControlPoint)nav.get(i);
			Point2D.Double p = new Point2D.Double(cpt.x, cpt.y);
			p = (Point2D.Double)proj.getMapXY(p);
			xp[i] = p.x;
			if( i!=0 && xWrap>0. ) {
				while(xp[i]-xp[i-1] > wrapTest ) xp[i]-=xWrap;
				while(xp[i]-xp[i-1] <= -wrapTest ) xp[i]+=xWrap;
			}
			yp[i] = p.y;
		}

		Vector segs = new Vector();
		int[] seg = new int[] {0, 0};
		double dx, dy;
		i=0;
		while( i < npt-1 ) {
			dx = xp[i+1] - xp[i];
			dy = yp[i+1] - yp[i];
			seg[1] = i;
			if(dx*dx+dy*dy>dxtest) {
				if(seg[0] < seg[1]) segs.add(seg);
				seg = new int[] {i+1, i+1};
			}
			i++;
		}
		seg[1] = npt-1;
		if(seg[0] < seg[1]) segs.add(seg);
		control = new Vector(segs.size());
		for( int iseg = 0; iseg<segs.size() ; iseg++ ) {
			seg = (int[]) segs.get(iseg);
			int i1 = seg[0];
			int i2 = seg[1];
			Vector segControl = new Vector();
			if(i1==npt) return;
			cpt = (ControlPoint)nav.get(i1);
			segControl.add(new IndexControlPoint(cpt.x, cpt.y, cpt.time, i1));
			if(i2<=i1)return;
			int imax = i2;
			cpt = (ControlPoint)nav.get(i2);
			segControl.add(new IndexControlPoint(cpt.x, cpt.y, cpt.time, i2));
			int k = 1;
			do {
				if( abort.isSelected() ) {
					abort.setSelected(false);
					control = new Vector();
					return;
				}
				i2 = ((IndexControlPoint)segControl.get(k)).index;
				while( (i=segment(i1,i2)) != -1) {
					cpt = (ControlPoint)nav.get(i);
					segControl.add(k, new IndexControlPoint(cpt.x, cpt.y, cpt.time, i));
					i2 = i;
				}
				k++;
				i1 = i2;
			} while( i1<imax );
			segControl.trimToSize();
			control.add(segControl);
		}
	}
	int segment( int i1, int i2) {
		int t1 = ((ControlPoint)nav.get(i1)).time;
		int t2 = ((ControlPoint)nav.get(i2)).time;
		if( t1==t2) return oldSegment( i1, i2 );
		double[] v = new double[] { xp[i2]-xp[i1], yp[i2]-yp[i1] };
		if( v[0]==0. && v[1]==0. ) v[0] = .01;
		double dt0=1.;
		if(t1!=t2) dt0 = 1./ (double)(t2-t1);
		int k=-1;
		double lat = .5 * ( ((ControlPoint)nav.get(i1)).y
				+ ((ControlPoint)nav.get(i2)).y );
		double max = (proj instanceof Mercator) 
			? 1./Math.cos(Math.toRadians(lat))
			: 1.;
		double dx, dy, d;
		for( int i=i1+1 ; i<i2 ; i++) {
			int t = ((ControlPoint)nav.get(i)).time;
			double dt = (t-t1)*dt0;
			dx = xp[i1] + dt*v[0] - xp[i];
			dy = yp[i1] + dt*v[1] - yp[i];
			d=dx*dx + dy*dy;
			if( d>max ) {
				max = d;
				k = i;
			}
		}
		return k;
	}
	int oldSegment( int i1, int i2) {
		double[] v = new double[] { xp[i2]-xp[i1], yp[i2]-yp[i1] };
		if( v[0]==0. && v[1]==0. ) v[0] = .01;
		double norm = Math.sqrt( v[0]*v[0] + v[1]*v[1] );
		v[0] /= norm;
		v[1] /= norm;
		int k=-1;
		double lat = .5 * ( ((ControlPoint)nav.get(i1)).y
				+ ((ControlPoint)nav.get(i2)).y );
		double max = 1./Math.cos(Math.toRadians(lat));
		double dx, dy, d;
		for( int i=i1+1 ; i<i2 ; i++) {
			dx = (xp[i] - xp[i1]) * v[0] + (yp[i] - yp[i1]) * v[1];
			dy = -(xp[i] - xp[i1]) * v[1] + (yp[i] - yp[i1]) * v[0];
			if( dx<0 ) {
				d=dx*dx + dy*dy;
			} else if( dx>norm ) {
				dx-=norm;
				d = dx*dx + dy*dy;
			} else {
				d = dy*dy;
			}
			if( d>max ) {
				max = d;
				k = i;
			}
		}
		return k;
	}
	public Vector getControlPoints() {
		return control;
	}
}

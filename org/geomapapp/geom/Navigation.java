package org.geomapapp.geom;

import org.geomapapp.gis.shape.*;

import java.io.*;
import java.util.Vector;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.StringTokenizer;
import java.util.Vector;
import java.awt.geom.Point2D;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.GridLayout;

public class Navigation {
	Vector points;
	double[][] pts;
	int[][] control;
	String name;
	MapProjection proj;
	boolean projected;
	public static void main(String[] args) {
		JFileChooser c = new JFileChooser(System.getProperty("user.dir"));
		c.setMultiSelectionEnabled(true);
		int ok = c.showOpenDialog( null );
		if( ok==JFileChooser.CANCEL_OPTION )System.exit(0);
		Calendar cal = Calendar.getInstance( TimeZone.getTimeZone("GMT") );
		String shapeName = "";
		try {
			File[] files = c.getSelectedFiles();
			if( files.length>1 ) {
				JPanel panel = new JPanel(new GridLayout(2,0) );
				panel.add( new JLabel("enter name of shape file") );
				JTextField text = new JTextField();
				panel.add( text );
				JOptionPane.showMessageDialog( haxby.map.MapApp.anchor, panel, "enter title", JOptionPane.PLAIN_MESSAGE);
				shapeName = text.getText();
			} else {
				shapeName = files[0].getName();
				shapeName = shapeName.substring(0, shapeName.lastIndexOf("."));
			}
			Vector names = new Vector(1);
			names.add("cruise_id");
			Vector classes = new Vector(1);
			classes.add(String.class);
			ESRIShapefile shapes = new ESRIShapefile( shapeName, 23, names, classes);
			for( int i=0 ; i<files.length ; i++) {
				File file = files[i];
				String name = file.getName();
				name = name.substring(0, name.lastIndexOf("."));
				Navigation nav = new Navigation(name);
				BufferedReader in = new BufferedReader(new FileReader( file ));
				String s;
				StringTokenizer st, st1;;
				while( (s=in.readLine())!=null ) {
					st = new StringTokenizer(s);
					st1 = new StringTokenizer(st.nextToken(), "-");
					cal.set( Calendar.YEAR, Integer.parseInt(st1.nextToken()) );
					cal.set( Calendar.MONTH, Integer.parseInt(st1.nextToken())-1 );
					cal.set( Calendar.DATE, Integer.parseInt(st1.nextToken()) );
					st1 = new StringTokenizer(st.nextToken(), ":");
					cal.set( Calendar.HOUR_OF_DAY, Integer.parseInt(st1.nextToken()) );
					cal.set( Calendar.MINUTE, Integer.parseInt(st1.nextToken()) );
					cal.set( Calendar.SECOND, Integer.parseInt(st1.nextToken()) );
					double t = cal.getTimeInMillis()/1000.;
					double x = Double.parseDouble( st.nextToken() );
					double y = Double.parseDouble( st.nextToken() );
					nav.addPoint( x, y, t );
				}
				in.close();
				nav.toArray();
				int nPt = nav.pts.length;
				double[][][] control = nav.computeControl( new Mercator(0., 0., 400000., 0, 0), 
							400000., 300., 10., 2. ); 
				int count = 0;
				for( int k=0 ; k<control.length ; k++) count += control[k].length;
				System.out.println( name +":\n\t"+ nPt +" input points");
				System.out.println( "\t"+ count +" output points in "+ control.length +" segments");
				ESRIShape shape = nav.getShape();
			
				names = new Vector(1);
				names.add(name);
				shapes.addShape(shape, names);
			}
			shapes.writeShapes( new File( files[0].getParentFile(), shapeName ));
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.exit(0);
	}

	public Navigation() {
		this("nav");
	}
	public Navigation(String name) {
		this.name = name;
		projected=false;
		points = new Vector();
	}
	public Navigation(String name, double[][] pts) {
		this.name = name;
		projected=false;
		this.pts = pts;
	}
	public String getName() {
		return name;
	}
	public void addPoint( double x, double y ) {
		pts = null;
		if( points==null )points = new Vector();
		points.add( new double[] {x,y} );
	}
	public void addPoint( double x, double y, double t ) {
		pts = null;
		if( points==null )points = new Vector();
		points.add( new double[] {x,y,t} );
	}
	public void toArray() {
		if( pts!=null )return;
		pts = new double[points.size()][];
		for( int k=0 ; k<pts.length ; k++) pts[k]=(double[])points.get(k);
		points = null;
	}
	public int size() {
		if( pts!=null )return pts.length;
		if( points!=null ) return points.size();
		return 0;
	}
	public void forward( MapProjection proj ) {
		toArray();
		if( projected )return;
		this.proj = proj;
		projected=true;
		if( proj instanceof IdentityProjection )return;
		for( int k=0 ; k<pts.length ; k++) {
			Point2D.Double p = (Point2D.Double)proj.getMapXY( new Point2D.Double(pts[k][0],pts[k][1])  );
			pts[k][0] = p.x;
			pts[k][1] = p.y;
		}
	}
	public void inverse( MapProjection proj ) {
		if( proj != this.proj ) {
			if( !proj.equals(this.proj) )System.out.println("Warning: inverse and forward projection differ");
		}
		toArray();
		if( !projected )return;
		projected=false;
		if( proj instanceof IdentityProjection )return;
		for( int k=0 ; k<pts.length ; k++) {
			Point2D.Double p = (Point2D.Double)proj.getRefXY( new Point2D.Double(pts[k][0],pts[k][1])  );
			pts[k][0] = p.x;
			pts[k][1] = p.y;
		}
	}
	void wrapX( double xWrap ) {
		toArray();
		if( xWrap<=0. )return;
		double w2 = .5*xWrap;
		double x0 = pts[0][0];
		double xmin, xmax;
		xmin = xmax = x0;
		for( int k=1 ; k<pts.length ; k++) {
			double x = pts[k][0];
			while( x>x0+w2 ) x-=xWrap;
			while( x<x0-w2 ) x+=xWrap;
			if( x>xmax ) {
				xmax=x;
				x0 = (xmin+xmax)*.5;
			} else if( x<xmin ) {
				xmin=x;
				x0 = (xmin+xmax)*.5;
			}
			pts[k][0] = x;
		}
	}
	public double[][][] computeControl( MapProjection proj, double xWrap, double maxDX, double maxDR ) {
		return computeControl(proj, xWrap, -1., maxDX, maxDR );
	}
	public double[][][] computeControl( MapProjection proj, double xWrap, double maxDT, double maxDX, double maxDR ) {
		toArray();
		forward(proj);
		int npt = pts.length;
		wrapX( xWrap );
		int i=0;
		Vector segs = new Vector();
		int[] seg = new int[] {0, 0};
		double test = maxDX*maxDX;
		double[] p1 = pts[0];
		while( i<pts.length-1 ) {
			double[] p2= pts[i+1];
			seg[1] = i;
			if( maxDX>0. && Math.pow(p2[0]-p1[0], 2) + Math.pow(p2[1]-p1[1], 2) >test ) {
				if(seg[0] < seg[1]) segs.add(seg);
				seg = new int[] {i+1, i+1};
			} else if( maxDT>0. && p1.length>2 && p2[2]-p1[2]>maxDT ) {
				if(seg[0] < seg[1]) segs.add(seg);
				seg = new int[] {i+1, i+1};
			}
			p1 = p2;
			i++;
		}
		seg[1] = npt-1;
		if(seg[0] < seg[1]) segs.add(seg);
		segs.trimToSize();
		control = new int[segs.size()][];
		test = maxDR*maxDR;
		for( int iseg = 0; iseg<segs.size() ; iseg++ ) {
			seg = (int[]) segs.get(iseg);
			int i1 = seg[0];
			int i2 = seg[1];
			Vector segControl = new Vector();
			segControl.add( Integer.valueOf(i1) );
			segControl.add( Integer.valueOf(i2) );
			int imax = i2;
			int k=1;
			while(i1<imax) {
				i2 = ((Integer)segControl.get(k)).intValue();
				while( (i=segment(i1,i2, test)) != -1 ) {
					segControl.add(k, Integer.valueOf(i) );
					i2=i;
				}
				k++;
				i1 = i2;
			}
			control[iseg] = new int[segControl.size()];
			for( k=0 ; k<segControl.size() ; k++) control[iseg][k]=((Integer)segControl.get(k)).intValue();
		}
		inverse(proj);
		return getControl();
	}
	int segment( int i1, int i2, double test) {
		double max = test;
		if( pts[i1].length==2 )return segment2(i1,i2,max);
		double t1 = pts[i1][2];
		double t2 = pts[i2][2];
		if( t1==t2 )return segment2(i1,i2,max);
		double[] v = new double[] { pts[i2][0]-pts[i1][0], pts[i2][1]-pts[i1][1] };
		if( v[0]==0. && v[1]==0. ) v[0] = test*.01;
		double dt0=1./(t2-t1);
		int k=-1;
		double d=0.;
		for( int i=i1+1 ; i<i2 ; i++) {
			double t = pts[i][2];
			double dt = (t-t1)*dt0;
			double dx = pts[i1][0] + dt*v[0] - pts[i][0];
			double dy = pts[i1][1] + dt*v[1] - pts[i][1];
			d=dx*dx + dy*dy;
			if( d>max ) {
				max = d;
				k=i;
			}
		}
		return k;
	}
	int segment2( int i1, int i2, double test) {
		double max = test;
		double[] v = new double[] { pts[i2][0]-pts[i1][0], pts[i2][1]-pts[i1][1] };
		if( v[0]==0. && v[1]==0. ) v[0] = test*.01;
		double norm = Math.sqrt( v[0]*v[0] + v[1]*v[1] );
		v[0] /= norm;
		v[1] /= norm;
		int k=-1;
		double d=0.;
		for( int i=i1+1 ; i<i2 ; i++) {
			double dx = (pts[i][0] - pts[i1][0]) * v[0] + (pts[i][1] - pts[i1][1]) * v[1];
			double dy = -(pts[i][0] - pts[i1][0]) * v[1] + (pts[i][1] - pts[i1][1]) * v[0];
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
				k=i;
			}
		}
		return k;
	}
	public double[][][] getControl() {
		double[][][] c = new double[control.length][][];
		for( int k=0 ; k<control.length ; k++) {
			c[k] = new double[control[k].length][2];
			for( int i=0 ; i<c[k].length ; i++ ) c[k][i] = pts[control[k][i]];
		}
		return c;
	}
	public ESRIShape getShape() {
		double[][][] data = getControl();
		int npt = 0;
		for( int k=0 ; k<data.length ; k++) {
			npt += data[k].length;
		}
		if( data[0][0].length==2 ) {
			int type = 3;						// POLYLINE
			ESRIPolyLine shape = new ESRIPolyLine(0., 0., 0., 0., 
						data.length, npt);
			npt=0;
			for( int k=0 ; k<data.length ; k++) {
				shape.setPartIndex( k, npt );
				for( int i=0 ; i<data[k].length ; i++) {
					shape.addPoint( npt++, data[k][i][0], data[k][i][1] );
				}
			}
			return shape;
		} else {
			int type = 23;						// POLYLINE_M
			ESRIPolyLineM shape = new ESRIPolyLineM(0., 0., 0., 0., 
						data.length, npt);
			npt=0;
			for( int k=0 ; k<data.length ; k++) {
				shape.setPartIndex( k, npt );
				for( int i=0 ; i<data[k].length ; i++) {
					shape.addMeasure(npt, data[k][i][2]);
					shape.addPoint( npt++, data[k][i][0], data[k][i][1] );
				}
			}
			return shape;
		}
	}
	public double[][] getNav() {
		return pts;
	}
}
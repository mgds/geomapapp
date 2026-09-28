package org.geomapapp.geom;

public class test {
	public static void main( String[] args ) {
		org.geomapapp.geom.Perspective3D pers = new org.geomapapp.geom.Perspective3D(
				new GCPoint(0., -10., 1.),
				new GCPoint(0., 0., 1.),
				20., 100. );
		for( int k=0 ; k<10 ; k++) {
			GCPoint p = new GCPoint( 1.+k,0.,1.);
			XYZ p1 = p.getXYZ();
			XYZ prj = pers.forward(p);
			System.out.println( prj.x +"\t"+ prj.y +"\t"+ prj.z);
			prj = pers.inverse(prj);
			System.out.println( p1.x +"\t"+ p1.y +"\t"+ p1.z);
			System.out.println( prj.x +"\t"+ prj.y +"\t"+ prj.z);
			p = prj.getGCPoint();
			System.out.println( p.longitude +"\t"+ p.latitude +"\t"+ p.elevation);
		}
	}
}
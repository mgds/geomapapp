package org.geomapapp.gis.shape;

import java.awt.geom.Rectangle2D;

import java.io.*;
import org.geomapapp.io.LittleIO;

public class ESRIMultiPointM extends ESRIMultiPoint
			implements ESRIM {
	public double[] m;
	public double[] mRange;
	public ESRIMultiPointM( double xmin,
				double ymin,
				double xmax,
				double ymax,
				int npt) {
		super( xmin, ymin, xmax, ymax, npt);
		m = new double[npt];
	}
	public void setMRange( double mmin,
				double mmax ) {
		mRange = new double[] {mmin, mmax};
	}
	public double[] getMRange() {
		if( mRange==null) {
			mRange = new double[] {m[0], m[0]};
			for( int i=1 ; i<pts.length ; i++) {
				if( m[i]>mRange[1] ) mRange[1]=m[i];
				else if( m[i]<mRange[0] ) mRange[0]=m[i];
			}
		}
		return mRange;
	}
	public double[] getMeasures() {
		return m;
	}
	public void addMeasure(int i, double m) {
		this.m[i] = m;
	}
	public double[][] inverse(org.geomapapp.geom.MapProjection proj, 
					double[][] bounds) {
		bounds = super.inverse( proj, bounds);
		double[] m = getMRange();
		if( bounds[3]==null ) {
			bounds[3] = m;
			return bounds;
		}
		if( m[1]>bounds[3][1] )bounds[3][1]=m[1];
		if( m[0]<bounds[3][0] )bounds[3][0]=m[0];
		return bounds;
	}
	public int writeShape( OutputStream out ) throws IOException {
		int length = 16+super.writeShape(out);
		if( m==null || m.length==0 ) return length;
		LittleIO.writeDouble( getMRange()[0], out );
		LittleIO.writeDouble( getMRange()[1], out );
		for( int k=0 ; k<pts.length ; k++) LittleIO.writeDouble( m[k], out);
		length += 8*pts.length;
		return length;
	}
	public int getType() {
		return 28;
	}
}
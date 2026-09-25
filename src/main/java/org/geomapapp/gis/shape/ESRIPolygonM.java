package org.geomapapp.gis.shape;

import java.awt.geom.*;
import java.awt.*;

import java.io.*;
import org.geomapapp.io.LittleIO;

public class ESRIPolygonM extends ESRIPolygon 
			implements ESRIM {
	public double[] m;
	public double[] mRange;
	public ESRIPolygonM( double xmin,
				double ymin,
				double xmax,
				double ymax,
				int nParts,
				int npt) {
		super( xmin, ymin, xmax, ymax, nParts, npt);
		m = new double[npt];
	}
	public int getType() {
		return 25;
	}
	public void addMeasure( int i, double m ) {
		this.m[i] = m;
	}
	public double[] getMeasures() {
		return m;
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
	public void setMRange(double minm, double maxm) {
		mRange = new double[] {minm, maxm};
	}
	public double[][] inverse(org.geomapapp.geom.MapProjection proj, 
					double[][] bounds) {
		bounds = super.inverse( proj, bounds);
		double[] m = getMRange();
		if( bounds[2]==null ) {
			bounds[2] = m;
			return bounds;
		}
		if( m[1]>bounds[2][1] )bounds[2][1]=m[1];
		if( m[0]<bounds[2][0] )bounds[2][0]=m[0];
		return bounds;
	}
	public ESRIPoint[] getPart( int part ) {
		int k1 = parts[part];
		int k2 = part==parts.length-1
			? pts.length
			: parts[part+1];
		ESRIPointM[] seg = new ESRIPointM[k2-k1];
		for( int k=k1 ; k<k2 ; k++) 
			seg[k-k1] = new ESRIPointM(pts[k].getX(),
						pts[k].getY(),
						m[k]);
		return seg;
	}
	public double measureAt( double index ) {
		if( index<0 || index>m.length-1 )return java.lang.Double.NaN;
		int i = (int)Math.floor(index);
		double dm = m[i]-Math.floor(index);
		if( i==m.length-1 )return m[m.length-1];
		return m[i] + dm*(m[i+1]-m[i]);
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
}

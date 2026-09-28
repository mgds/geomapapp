package org.geomapapp.gis.shape;

import java.awt.geom.Rectangle2D;

import java.io.*;
import org.geomapapp.io.LittleIO;

public class ESRIMultiPointZ extends ESRIMultiPoint {
	public double[] z;
	public double[] zRange;
	public double[] m;
	public double[] mRange;
	public ESRIMultiPointZ( double xmin,
				double ymin,
				double xmax,
				double ymax,
				int npt) {
		super( xmin, ymin, xmax, ymax, npt);
		z = new double[npt];
		m = new double[npt];
	}
	public void setZRange( double zmin,
				double zmax ) {
		zRange = new double[] {zmin, zmax};
	}
	public double[] getZRange() {
		if( zRange==null) {
			zRange = new double[] {z[0], z[0]};
			for( int i=1 ; i<pts.length ; i++) {
				if( z[i]>zRange[1] ) zRange[1]=z[i];
				else if( z[i]<zRange[0] ) zRange[0]=z[i];
			}
		}
		return zRange;
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
	public double[] getZ() {
		return z;
	}
	public void addZ(int i, double z) {
		this.z[i] = z;
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
		double[] z = getZRange();
		if( bounds[2]==null ) {
			bounds[2] = z;
		} else {
			if( z[1]>bounds[2][1] )bounds[2][1]=z[1];
			if( z[0]<bounds[2][0] )bounds[2][0]=z[0];
		}
		double[] m = getMRange();
		if( bounds[3]==null ) {
			bounds[3] = m;
		} else {
			if( m[1]>bounds[3][1] )bounds[3][1]=m[1];
			if( m[0]<bounds[3][0] )bounds[3][0]=m[0];
		}
		return bounds;
	}
	public int writeShape( OutputStream out ) throws IOException {
		int length = 16+super.writeShape(out);
		LittleIO.writeDouble( getZRange()[0], out );
		LittleIO.writeDouble( getZRange()[1], out );
		for( int k=0 ; k<pts.length ; k++) LittleIO.writeDouble( z[k], out);
		length += 8*pts.length;
		LittleIO.writeDouble( getMRange()[0], out );
		LittleIO.writeDouble( getMRange()[1], out );
		for( int k=0 ; k<pts.length ; k++) LittleIO.writeDouble( m[k], out);
		length += 8*pts.length;
		return length;
	}
	public int getType() {
		return 18;
	}
}
package org.geomapapp.gis.shape;

import java.awt.geom.*;
import java.awt.*;

import java.io.*;
import org.geomapapp.io.LittleIO;

public class ESRIPolygonZ extends ESRIPolygon {
	double[] z;
	double[] zRange;
	double[] m;
	double[] mRange;
	public ESRIPolygonZ( double xmin,
				double ymin,
				double xmax,
				double ymax,
				int nParts,
				int npt) {
		super( xmin, ymin, xmax, ymax, nParts, npt);
		z = new double[npt];
		m = new double[npt];
	}
	public double[] getZ() {
		return z;
	}
	public void addZ(int i, double z) {
		this.z[i] = z;
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
	public ESRIPoint[] getPart( int part ) {
		int k1 = parts[part];
		int k2 = part==parts.length-1
			? pts.length
			: parts[part+1];
		ESRIPointZ[] seg = new ESRIPointZ[k2-k1];
		for( int k=k1 ; k<k2 ; k++) 
			seg[k-k1] = new ESRIPointZ(pts[k].getX(),
						pts[k].getY(),
						z[k],
						m[k]);
		return seg;
	}
	public double measureAt( double index ) {
		if( index<0 || index>m.length-1 )return java.lang.Double.NaN;
		int i = (int)Math.floor(index);
		double dm = index-Math.floor(index);
		if( i==m.length-1 )return m[m.length-1];
		return m[i] + dm*(m[i+1]-m[i]);
	}
	public double[] xyAt( double measure ) {
		double[] range = getMRange();
		if( measure<range[0] || measure>range[1] )return null;
		int i=0;
		for( i=1 ; i<m.length ; i++) {
			if( m[i]>=measure ) {
				double dm = (measure-m[i-1])/ (m[i]-m[i-1]);
				double xa = pts[i-1].getX();
				double xb = pts[i].getX();
				double x = xa + dm*(xb-xa);
				double ya = pts[i-1].getY();
				double yb = pts[i].getY();
				double y = ya + dm*(yb-ya);
				return new double[] {x, y};
			}
		}
		return null;
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
		return 15;
	}
}
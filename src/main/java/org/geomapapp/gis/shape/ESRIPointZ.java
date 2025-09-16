package org.geomapapp.gis.shape;

import java.awt.geom.Point2D;

import java.io.*;
import org.geomapapp.io.LittleIO;

public class ESRIPointZ extends ESRIPoint {
	public double m;
	public double z;
	public ESRIPointZ(double x, double y, double z, double m) {
		super( x, y);
		this.m = m;
		this.z = z;
	}
	public int getType() {
		return 11;
	}
	public void addZ( int i, double z ) {
		if(i==0) this.z=z;
	}
	public double[] getZ() {
		return new double[] {z};
	}
	public double[] getZRange() {
		return new double[] {z, z};
	}
	public void setZRange(double minz, double maxz) {
		return;
	}
	public void addMeasure( int i, double m ) {
		if(i==0) this.m=m;
	}
	public double[] getMeasures() {
		return new double[] {m};
	}
	public double[] getMRange() {
		return new double[] {m, m};
	}
	public void setMRange(double minm, double maxm) {
		return;
	}
	public int writeShape( OutputStream out ) throws IOException {
		super.writeShape(out);
		LittleIO.writeDouble( z, out );
		LittleIO.writeDouble( m, out );
		return 32;
	}
}

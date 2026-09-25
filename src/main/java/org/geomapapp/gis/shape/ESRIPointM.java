package org.geomapapp.gis.shape;

import java.awt.geom.Point2D;

import java.io.*;
import org.geomapapp.io.LittleIO;

public class ESRIPointM extends ESRIPoint 
		implements ESRIM {
	public double m;
	public ESRIPointM(double x, double y, double m) {
		super( x, y);
		this.m = m;
	}
	public int getType() {
		return 21;
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
		LittleIO.writeDouble( m, out );
		return 24;
	}
}

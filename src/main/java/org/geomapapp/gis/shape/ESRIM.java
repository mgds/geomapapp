package org.geomapapp.gis.shape;

public abstract interface ESRIM {
	public void addMeasure( int i, double m );
	public double[] getMeasures();
	public double[] getMRange();
	public void setMRange(double minm, double maxm);
}

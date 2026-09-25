package org.geomapapp.util;

public abstract interface ScalableXYPoints extends XYPoints {
	public void setXRange(int dataIndex, double[] range);
	public void setYRange(int dataIndex, double[] range);
	public void resetRanges(int dataIndex);
}
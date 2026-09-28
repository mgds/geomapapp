package org.geomapapp.util;

import java.awt.geom.Rectangle2D;
import java.awt.Graphics2D;

public abstract interface XYPoints {
	public String getXTitle(int dataIndex);
	public String getYTitle(int dataIndex);
	public double[] getXRange(int dataIndex);
	public double[] getYRange(int dataIndex);
	public double getPreferredXScale(int dataIndex);
	public double getPreferredYScale(int dataIndex);
	public void plotXY( Graphics2D g, 
				Rectangle2D bounds,
				double xScale, double yScale,
				int dataIndex);
}
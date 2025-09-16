package org.geomapapp.db.dsdp;

import org.geomapapp.util.*;

import java.awt.*;
import java.awt.geom.*;

public class DSDPData implements ScalableXYPoints {
	float[] depth;
	float[] data;
	float[] age;
	DSDPHole hole;
	DSDPDataSet dataSet;
	double[] xRange;
	public DSDPData(float[] depth, float[] data, DSDPHole hole, DSDPDataSet dataSet) {
		this.depth = depth;
		this.data = data;
		this.hole = hole;
		this.dataSet = dataSet;
		setRange();
		age = new float[depth.length];
		for( int k=0 ; k<age.length ; k++) {
			age[k] = AgeDepthModel.ageAtDepth(hole.getAgeModel(), depth[k]);
		}
	}
	public int getDataCount() {
		return 1;
	}
	public String getXTitle(int dataIndex) {
		return dataSet.getName();
	}
	public String getYTitle(int dataIndex) {
		return "DEPTH";
	}
	void setRange() {
		xRange = new double[] {data[0], data[0]};
		for( int i=1 ; i<data.length ; i++) {
			if( data[i]>xRange[1] )xRange[1]=data[i];
			else if( data[i]<xRange[0] )xRange[0]=data[i];
		}
	}
	public double[] getXRange(int dataIndex) {
		return xRange;
	}
	public double[] getYRange(int dataIndex) {
		return new double[] {-hole.totalPen, 0.};
	}
	public void setXRange(int dataIndex, double[] range) {
		xRange = range;
	}
	public void setYRange(int dataIndex, double[] range) {
	}
	public void resetRanges(int dataIndex) {
		setRange();
	}
	public double getPreferredXScale(int k) {
		double dr = xRange[1]-xRange[0];
		return 400./dr;
	}
	public double getPreferredYScale(int dataIndex) {
		return 2.;
	}
	public void plotXY( Graphics2D g, 
				Rectangle2D bounds,
				double xScale, double yScale,
				int k) {
		boolean start = true;
		Rectangle2D.Double rect = new Rectangle2D.Double(0., 0., 6., 6. );
		g.setStroke( new BasicStroke( 3f ));
		for( int i=0 ; i<depth.length ; i++) {
			rect.x = (data[i]-bounds.getX())*xScale - 3.;
			rect.y = (-depth[i]-bounds.getY())*yScale - 3.;
			g.setColor( Color.white );
			g.draw(rect);
			g.setColor( Color.black );
			g.fill(rect);
		}
	}
}

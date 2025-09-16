package org.geomapapp.gis.table;

import org.geomapapp.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.*;

public class GMAGraph implements XYPoints {
	TableDB db;
	int xCol, yCol;
	double[] xRange, yRange;
	double xScale, yScale;
	boolean connect;
	public GMAGraph(TableDB db, int xCol, int yCol, boolean connect) throws ClassCastException {
		boolean start=true;
		xRange = new double[2];
		yRange = new double[2];
		for( int k=0 ; k<db.getRowCount() ; k++) {
			if( db.getValueAt(k, xCol)==null || db.getValueAt(k, yCol)==null) continue;
			double x = ((Number)db.getValueAt(k, xCol)).doubleValue();
			double y = ((Number)db.getValueAt(k, yCol)).doubleValue();
			if( start ) {
				xRange[0] = xRange[1] = x;
				yRange[0] = yRange[1] = y;
				start = false;
			} else {
				if( x>xRange[1]) xRange[1]=x;
				else if( x<xRange[0]) xRange[0]=x;
				if( y>yRange[1]) yRange[1]=y;
				else if( y<yRange[0]) yRange[0]=y;
			}
		}
		double dx = xRange[1] - xRange[0];
		if( dx==0. ) throw new ClassCastException();
		double dy = yRange[1] - yRange[0];
		if( dy==0. ) throw new ClassCastException();
		xRange[0] -= dx*.02;
		xRange[1] += dx*.02;
		yRange[0] -= dy*.02;
		yRange[1] += dy*.02;
		xScale = 500./dx;
		yScale = 500./dy;
		this.xCol = xCol;
		this.yCol = yCol;
		this.db = db;
		this.connect = connect;
	}
	public String getXTitle(int dataIndex) {
		return db.getColumnName(xCol);
	}
	public String getYTitle(int dataIndex) {
		return db.getColumnName(yCol);
	}
	public double[] getXRange(int dataIndex) {
		return xRange;
	}
	public double[] getYRange(int dataIndex) {
		return yRange;
	}
	public double getPreferredXScale(int dataIndex) {
		return xScale;
	}
	public double getPreferredYScale(int dataIndex) {
		return yScale;
	}
	public void plotXY( Graphics2D g, 
				Rectangle2D bounds,
				double xScale, double yScale,
				int dataIndex) {
		float x0 = (float)bounds.getX();
		float y0 = (float)bounds.getY();
		float xs = (float)xScale;
		float ys = (float)yScale;
		if( connect ) {
			GeneralPath path = new GeneralPath();
			boolean start = true;
			for( int k=0 ; k<db.getRowCount() ; k++) {
				if( db.getValueAt(k, xCol)==null || db.getValueAt(k, yCol)==null) continue;
				float x = xs*(((Number)db.getValueAt(k, xCol)).floatValue()-x0);
				float y = ys*(((Number)db.getValueAt(k, yCol)).floatValue()-y0);
				if( start ) {
					path.moveTo(x,y);
					start = false;
				} else {
					path.lineTo(x,y);
				}
			}
			g.draw(path);
		} else {
			Arc2D.Float symbol = new Arc2D.Float( -2f, -2f, 4f, 4f, 0f, 360f, Arc2D.CHORD);
			for( int k=0 ; k<db.getRowCount() ; k++) {
				if( db.getValueAt(k, xCol)==null || db.getValueAt(k, yCol)==null) continue;
				double x = xs*(((Number)db.getValueAt(k, xCol)).floatValue()-x0);
				double y = ys*(((Number)db.getValueAt(k, yCol)).floatValue()-y0);
				g.translate(x,y);
				g.setColor(Color.white);
				g.fill( symbol );
				g.setColor(Color.black);
				g.draw( symbol );
				g.translate(-x,-y);
			}
		}
	}
}

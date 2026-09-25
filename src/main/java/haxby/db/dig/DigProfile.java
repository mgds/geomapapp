package haxby.db.dig;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import haxby.db.XYPoints;
import haxby.db.dig.LineSegmentsObject.Point4D;
import haxby.map.XMap;

public class DigProfile implements XYPoints {
	LineSegmentsObject line;
	XMap map;
	double[] xRange;
	double[] yRange;
	double xIncrement;
	public DigProfile( XMap map ) {
		this.map = map;
		line = null;
		xRange = new double[2];
		yRange = new double[2];
	}
	public void setLine( LineSegmentsObject line ) {
		this.line = line;
		ArrayList<Point4D> profile = line.profile;
		if (profile == null) return;
		Point4D p4d = profile.get(0);
		xRange[0] = xRange[1] = (double) p4d.getD();
		yRange[0] = yRange[1] = (double) p4d.getZ();
		for( int k=1 ; k<profile.size() ; k++ ) {
			p4d = profile.get(k);
			if( p4d.getD()>xRange[1] ) xRange[1] = p4d.getD();
			else if( p4d.getD()<xRange[0] ) xRange[0] = p4d.getD();
			if( p4d.getZ()>yRange[1] || Double.isNaN(yRange[1])) yRange[1] = p4d.getZ();
			if( p4d.getZ()<yRange[0] || Double.isNaN(yRange[0])) yRange[0] = p4d.getZ();
			if( k==1 ) xIncrement = (double)p4d.getD();
		}
		double dr = (yRange[1]-yRange[0])/20.;
		yRange[0] -= dr;
		yRange[1] += dr;
	}
	public String getXTitle(int dataIndex) {
		return "Distance, km";
	}
	public String getYTitle(int dataIndex) {
		return line.grid.getDataType() + ", " + line.grid.getUnits();
	}
	public double[] getXRange(int dataIndex) {
		if( line==null || line.profile==null || line.profile.size()<2 ) return new double[]{0., 10.};
		return new double[] {xRange[0], xRange[1]};
	}
	public double[] getYRange(int dataIndex) {
		if( line==null || line.profile==null || line.profile.size()<2 ) return new double[]{0., 10.4};
		return new double[] {yRange[0], yRange[1]};
	}
	public double getPreferredXScale(int dataIndex) {
		if( line==null || line.profile==null || line.profile.size()<2 ) return 1.;
		return 2./xIncrement;
	}
	public double getPreferredYScale(int dataIndex) {
		if( line==null || line.profile==null || line.profile.size()<2 ) return 1.;
		return 400./(float)(yRange[1]-yRange[0]);
	}
	public void plotXY( Graphics2D g, 
				Rectangle2D bounds,
				double xScale, double yScale,
				int dataIndex) {
		if( line==null || line.profile==null || line.profile.size()<=1 ) return;
		g.setColor(Color.black);
		float x0 = (float)bounds.getX();
		float y0 = (float)bounds.getY();
		float x1 = x0;
		float x2 = x1+(float)bounds.getWidth();
		if(x1>x2) {
			x1 = x2;
			x2 = x0;
		}
	//	setXInterval(x1, x2);
		ArrayList<Point4D> profile = line.profile;
		int i=0;

		while( i<profile.size() && profile.get(i).getD() < x1 ) {
			i++;
		}
		if( i!=0 && !Double.isNaN(profile.get(i-1).getZ())) i--;
		while( i<profile.size() && 
				profile.get(i).getD() < x2 && 
				Double.isNaN(profile.get(i).getZ() ) ) i++;
		if( i==profile.size() || profile.get(i).getD() > x2 ) {
			return;
		}
	//	currentRange[0] = i;
		GeneralPath path = new GeneralPath();
		float sy = (float)yScale;
		float sx = (float)xScale;
		Point4D p4d = profile.get(i);
		path.moveTo(( p4d.getD()-x0)*sx, (p4d.getZ()-y0)*sy);
		boolean connect = false;
		while( i<profile.size() && p4d.getD() <= x2 ) {
			if( !connect ) {
				path.moveTo( (p4d.getD()-x0)*sx, (p4d.getZ()-y0)*sy);
				connect = true;
			} else {
				path.lineTo( (p4d.getD()-x0)*sx, (p4d.getZ()-y0)*sy);
			}
			i++;
			if( i<profile.size() )p4d = profile.get(i);
		}
	//	currentRange[1] = i--;
		g.draw( path );
	//	currentPoint = null;
	}
}

package org.geomapapp.gis.shape;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.io.LittleIO;

import haxby.map.XMap;

public class ESRIMultiPoint extends Rectangle2D.Double 
			implements ESRIShape {
	public ESRIPoint[] pts;
	public ESRIMultiPoint( double xmin,
				double ymin,
				double xmax,
				double ymax,
				int npt) {
		super( xmin, ymin, xmax-xmin, ymax-ymin);
		pts = new ESRIPoint[npt];
	}
	public void addPoint( int index, double x, double y) {
		pts[index] = new ESRIPoint(x, y);
	}
	public ESRIPoint[] getPoints() {
		return pts;
	}
	public int length() {
		return pts.length;
	}
	public int getType() {
		return 8;
	}
	public NearNeighbor select( NearNeighbor n, XMap map ) {
		for( int k=0 ; k<pts.length ; k++) {
			if( pts[k].select(n, map).shape == pts[k]) {
				n.shape = this;
				n.index = (double)k;
			}
		}
		return n;
	}
	public boolean canView( Rectangle2D r, double wrap) {
		if( y+height<r.getY() || y>r.getY()+r.getHeight())return false;
		double x = this.x;
		if( wrap>0. ) {
			while( x+width<r.getX() )x+=wrap;
			while( x>r.getX()+r.getWidth() )x-=wrap;
			return x+width<r.getX();
		}
		return x+width<r.getX() || x>r.getX()+r.getWidth();
	}
	public void forward(MapProjection proj, double wrap) {
		if(pts.length==0)return;
		for( int k=0 ; k<pts.length ; k++) pts[k].forward(proj);
		if( wrap>0. ) {
			double lastX = pts[0].getX();
			double wrap2 = .5*wrap;
			for( int k=1 ; k<pts.length ; k++) {
				double x = pts[k].getX();
				double offset=0.;
				while( x+offset>lastX+wrap2 )offset-=wrap;
				while( x+offset<lastX-wrap2 )offset+=wrap;
				if(offset!=0.) pts[k].setLocation(x+offset, pts[k].getY());
			}
		}
		x = pts[0].getX();
		double[] xRange = new double[] {x, x};
		y = pts[0].getY();
		double[] yRange = new double[] {y, y};
		for( int k=1 ; k<pts.length ; k++)  {
			double xx = pts[k].getX();
			double yy = pts[k].getY();
			if( xx<xRange[0] ) xRange[0]=xx;
			else if( xx>xRange[1] ) xRange[1]=xx;
			if( yy<yRange[0] ) yRange[0]=yy;
			else if( yy>yRange[1] ) yRange[1]=yy;
		}
		x = xRange[0];
		y = yRange[0];
		width = xRange[1]-xRange[0];
		height = yRange[1]-yRange[0];
	}
	public double[][] forward(MapProjection proj, double wrap, double[][] bounds) {
		if(pts.length==0)return bounds;
		for( int k=0 ; k<pts.length ; k++) pts[k].forward(proj);
		if( wrap>0. ) {
			double lastX = pts[0].getX();
			if( bounds[0]!=null ) {
				lastX= (bounds[0][0]+bounds[0][1])/2.;
			} else {
				bounds[0] = new double[] {lastX, lastX};
				bounds[1] = new double[] {pts[0].getY(), pts[0].getY()};
			}
			double wrap2 = .5*wrap;
			for( int k=0 ; k<pts.length ; k++) {
				double x = pts[k].getX();
				double offset=0.;
				while( x+offset>lastX+wrap2 )offset-=wrap;
				while( x+offset<lastX-wrap2 )offset+=wrap;
				if(offset!=0.) pts[k].setLocation(x+offset, pts[k].getY());
			}
		} else {
			double lastX = pts[0].getX();
			if( bounds[0]!=null ) {
				lastX= (bounds[0][0]+bounds[0][1])/2.;
			} else {
				bounds[0] = new double[] {lastX, lastX};
				bounds[1] = new double[] {pts[0].getY(), pts[0].getY()};
			}
		}
		x = pts[0].getX();
		double[] xRange = new double[] {x, x};
		y = pts[0].getY();
		double[] yRange = new double[] {y, y};
		for( int k=1 ; k<pts.length ; k++)  {
			double xx = pts[k].getX();
			double yy = pts[k].getY();
			if( xx<xRange[0] ) xRange[0]=xx;
			else if( xx>xRange[1] ) xRange[1]=xx;
			if( yy<yRange[0] ) yRange[0]=yy;
			else if( yy>yRange[1] ) yRange[1]=yy;
		}
		x = xRange[0];
		y = yRange[0];
		width = xRange[1]-xRange[0];
		height = yRange[1]-yRange[0];
		if( xRange[0]<bounds[0][0] )bounds[0][0]=xRange[0];
		if( xRange[1]>bounds[0][1] )bounds[0][1]=xRange[1];
		if( yRange[0]<bounds[1][0] )bounds[1][0]=yRange[0];
		if( yRange[1]>bounds[1][1] )bounds[1][1]=yRange[1];
		return bounds;
	}
	public void inverse(MapProjection proj) {
		for( int k=0 ; k<pts.length ; k++) pts[k].inverse(proj);
	}
	public double[][] inverse(MapProjection proj, double[][] bounds) {
		if( pts.length==0 )return bounds;
		bounds = pts[0].inverse( proj, bounds);
		double lastX = pts[0].getX();
		double lastY = pts[0].getY();
		double[] xr = new double[] {lastX, lastX};
		double[] yr = new double[] {lastY, lastY};
		double x, y;
		for( int k=1 ; k<pts.length ; k++) {
			pts[k].inverse(proj);
			x = pts[k].getX();
			while( x>lastX+180.) x-=360.;
			while( x<lastX-180.) x+=360.;
			y = pts[k].getY();
			if( x>xr[1] )xr[1]=x;
			else if( x<xr[0] )xr[0]=x;
			if( y>yr[1] )yr[1]=y;
			else if( y<yr[0] )yr[0]=y;
		}
		this.x = xr[0];
		this.y = yr[0];
		this.width = xr[1]-xr[0];
		this.height = yr[1]-yr[0];
		if( xr[0]<bounds[0][0] )bounds[0][0]=xr[0];
		if( xr[1]>bounds[0][1] )bounds[0][1]=xr[1];
		if( yr[0]<bounds[1][0] )bounds[1][0]=yr[0];
		if( yr[1]>bounds[1][1] )bounds[1][1]=yr[1];
		return bounds;
	}
	public void draw(java.awt.Graphics2D g, Rectangle2D r, double wrap) {
		// if( y+height<r.getY() || y>r.getY()+r.getHeight())return;
		double x = this.x;
		if( wrap>0. ) {
			AffineTransform at = g.getTransform();
			while( x>r.getX())x-=wrap;
			while( x+width<r.getX() )x+=wrap;
			if( x>r.getX()+r.getWidth() )return;
			g.translate( x-this.x, 0.);
			draw(g);
			while( x+wrap<r.getX()+r.getWidth() ) {
				g.translate(wrap,0.);
				draw(g);
				x+=wrap;
			}
			g.setTransform( at );
			return;
		}
	//	if( x+width<r.getX() || x>r.getX()+r.getWidth() )return;
		draw(g);
	}
	public void draw(java.awt.Graphics2D g) {
		for( int k=0 ; k<pts.length ; k++) pts[k].draw(g);
	}
	public int writeShape( OutputStream out ) throws IOException {
		LittleIO.writeDouble( x, out);
		LittleIO.writeDouble( y, out);
		LittleIO.writeDouble( x+width, out);
		LittleIO.writeDouble( y+height, out);
		LittleIO.writeInt( pts.length, out );
		for( int k=0 ; k<pts.length ; k++) pts[k].writeShape(out);
		return 40+16*pts.length;
	}
	public String toString() {
		StringBuffer sb = new StringBuffer("ESRIMultiPoint, "+pts.length+" points, ("+x+", "+y+", "+width+", "+height+")");
		for( int k=0 ; k<pts.length ; k++) sb.append("\n\t"+ pts[k].toString() );
		return sb.toString();
	}
}
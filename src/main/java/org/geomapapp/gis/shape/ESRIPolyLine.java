package org.geomapapp.gis.shape;

import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;

import org.geomapapp.io.LittleIO;

import haxby.map.XMap;
import haxby.util.GeneralUtils;

public class ESRIPolyLine extends ESRIMultiPoint {
	public int[] parts;
	public ESRIPolyLine( double xmin,
				double ymin,
				double xmax,
				double ymax,
				int nParts,
				int npt) {
		super( xmin, ymin, xmax, ymax, npt);
		parts = new int[nParts];
	}
	public int nParts() {
		return parts.length;
	}
	public int getType() {
		return 3;
	}
	public void setPartIndex( int part, int index) {
		parts[part] = index;
	}
	public NearNeighbor select( NearNeighbor n, XMap map ) {
		Line2D.Double line = new Line2D.Double();
		Point2D.Double p1 = new Point2D.Double();
		Point2D.Double p2 = new Point2D.Double();
		for( int i=0 ; i<nParts() ; i++) {
			ESRIPoint[] part = getPart(i);
			for( int k=0 ; k<part.length-1 ; k++) {
				p1.x = part[k].getX();
				p1.y = part[k].getY();
				p2.x = part[k+1].getX();
				p2.y = part[k+1].getY();
				//take in to account the wrap
				GeneralUtils.wrapPoints(map, p1, p2);
				line.setLine(p1, p2);
				GeneralUtils.wrapPoint(map, (Point2D.Double) n.test);
				//if a point occurs multiple times on the map, subtract the wrap 
				//value until we have the first occurrence
				GeneralUtils.unwrapPoint(map, (Point2D.Double) n.test);
				double[] rx = ptSegDistSq(line, n.test);
				if( rx[0]<n.radiusSq ) {
					n.shape = this;
					n.radiusSq = rx[0];
					n.index = parts[i]+k+rx[1];
					if( rx[0]==0. )return n;
				}
			}
		}
		return n;
	}
	public boolean intersects(Rectangle2D r) {
		return canView( r, -1.);
	}
	public boolean canView( Rectangle2D r, double wrap ) {
		if( !r.intersects(this) )return false;
		if( r.contains(this) )return true;
		Line2D.Double line = new Line2D.Double();
		for( int i=0 ; i<nParts() ; i++) {
			ESRIPoint[] part = getPart(i);
			for( int k=0 ; k<part.length-1 ; k++) {
				line.x1 = part[k].getX();
				line.y1 = part[k].getY();
				line.x2 = part[k+1].getX();
				line.y2 = part[k+1].getY();
				if( line.intersects(r) )return true;
			}
		}
		return false;
	}
	public ESRIPoint[] getPart( int part ) {
		int k1 = parts[part];
		int k2 = part==parts.length-1
			? pts.length
			: parts[part+1];
		ESRIPoint[] seg = new ESRIPoint[k2-k1];
		for( int k=k1 ; k<k2 ; k++) seg[k-k1] = pts[k];
		return seg;
	}
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("PolyLine (3), "+ parts.length + " parts\t(" + x +"\t"+ y  +"\t"+ width +"\t"+ height+")" );
		for( int p=0 ; p<parts.length ; p++) {
			ESRIPoint[] pts = getPart(p);
			sb.append("\npart "+p+", "+pts.length +" points");
			for( int k=0 ; k<pts.length ; k++) {
				sb.append("\n\t"+ pts[k].getX() +"\t"+ pts[k].getY());
			}
		}
		return sb.toString();
	}
	public void draw(Graphics2D g) {
	//	g.setStroke( new BasicStroke( 2f/(float)g.getTransform().getScaleX() ) );
		GeneralPath p = new GeneralPath();
		for( int k=0 ; k<parts.length ; k++) {
			ESRIPoint[] pts = getPart(k);
			if( pts==null || pts.length<2 )continue;
			p.moveTo( (float)pts[0].getX(), (float)pts[0].getY() );
			for( int i=1 ; i<pts.length ; i++) {
				
//				GMA 1.4.8: Imported shape files should no longer draw lines the wrong way across the map to connect two 
//				points that are close together.
//				p.lineTo( (float)pts[i].getX(), (float)pts[i].getY() );
				if ( Math.abs(pts[i-1].getX() - pts[i].getX() ) > 320 ) {
					if ( pts[i].getX() < pts[i-1].getX() ) {
						p.lineTo( (float)(pts[i].getX()+640), (float)pts[i].getY() );
						p.moveTo( (float)pts[i].getX(), (float)pts[i].getY() );
					}
					else {
						p.lineTo( (float)(pts[i].getX()-640), (float)pts[i].getY() );
						p.moveTo( (float)pts[i].getX(), (float)pts[i].getY() );
					}
				}
				else {
					p.moveTo( (float)pts[i-1].getX(), (float)pts[i-1].getY() );
					p.lineTo( (float)pts[i].getX(), (float)pts[i].getY() );
				}
				
			}
		}
		g.draw(p);
	}
	public static double[] ptSegDistSq( Line2D line, Point2D pt) {
		double dx, dy, dx0, dy0, r, r0, test, xx;
		double x1 = line.getX1();
		double y1 = line.getY1();
		double x2 = line.getX2();
		double y2 = line.getY2();
		double x = pt.getX();
		double y = pt.getY();
		dx0 = x2-x1;
		dy0 = y2-y1;
		dx = x-x1;
		dy = y-y1;
		boolean port = dx0*dy-dx*dy0>=0.;
		r0 = dx0*dx0 + dy0*dy0;
		test = dx*dx0 + dy*dy0;
		if(test<=0) {
			r = dx*dx + dy*dy;
			xx = 0.;
		} else if( test>=r0 ) {
			dx -= dx0;
			dy -= dy0;
			r = dx*dx + dy*dy;
			xx = 1.;
		} else {
			r = -dx*dy0 + dy*dx0;
			r *= r/r0;
			xx = test/r0;
		}
		return new double[] { r, xx };
	}
	public int writeShape( OutputStream out ) throws IOException {
		LittleIO.writeDouble( x, out);
		LittleIO.writeDouble( y, out);
		LittleIO.writeDouble( x+width, out);
		LittleIO.writeDouble( y+height, out);
		LittleIO.writeInt( parts.length, out );
		LittleIO.writeInt( pts.length, out );
		for( int k=0 ; k<parts.length ; k++) LittleIO.writeInt( parts[k], out);
		for( int k=0 ; k<pts.length ; k++) pts[k].writeShape(out);
		return 40+16*pts.length + 4*parts.length;
	}
		
/*
	public static void main(String[] args) {
		Line2D.Double l = new Line2D.Double(1000., 0., 1000.1, .1);
		Point2D.Double p = new Point2D.Double( 1000., .1);
		double[] rx = ptSegDistSq( l, p);
		System.out.println( rx[0] +"\t"+ rx[1]);
	}
*/
}

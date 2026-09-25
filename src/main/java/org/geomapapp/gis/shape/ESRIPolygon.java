package org.geomapapp.gis.shape;

import java.awt.geom.*;
import java.awt.*;

import java.io.*;
public class ESRIPolygon extends ESRIPolyLine {
	GeneralPath path;
	public ESRIPolygon( double xmin,
				double ymin,
				double xmax,
				double ymax,
				int nParts,
				int npt) {
		super( xmin, ymin, xmax, ymax, nParts, npt);
	}
	public int getType() {
		return 5;
	}
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Polygon (5), "+ parts.length + " parts\t(" + x +"\t"+ y  +"\t"+ width +"\t"+ height+")" );
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
		initPath();
	//	g.setStroke( new BasicStroke( 2f/(float)g.getTransform().getScaleX() ) );
		g.draw(path);
	}
	public NearNeighbor select( NearNeighbor n ) {
		initPath();
		if( path.contains(n.test) ) {
			n.shape = this;
			n.radiusSq = 0.;
			n.index = 0;
		}
		return n;
	}
	void initPath() {
		if( path==null ) {
			GeneralPath p = new GeneralPath();
			for( int k=0 ; k<parts.length ; k++) {
				ESRIPoint[] pts = getPart(k);
				if( pts==null || pts.length<2 )continue;
				GeneralPath p1 = new GeneralPath();
				p1.moveTo( (float)pts[0].getX(), (float)pts[0].getY() );
				for( int i=1 ; i<pts.length ; i++) {
				
//					GMA 1.4.8: Adjusted the line-drawing for shape files so that a line does not go across the map, 
//					that is go the wrong way instead of the right way
//					p1.lineTo( (float)pts[i].getX(), (float)pts[i].getY() );
					if ( Math.abs(pts[i-1].getX() - pts[i].getX() ) > 320 ) {
						if ( pts[i].getX() < pts[i-1].getX() ) {
							p1.lineTo( (float)(pts[i].getX()+640), (float)pts[i].getY() );
							p1.moveTo( (float)pts[i].getX(), (float)pts[i].getY() );
						}
						else {
							p1.lineTo( (float)(pts[i].getX()-640), (float)pts[i].getY() );
							p1.moveTo( (float)pts[i].getX(), (float)pts[i].getY() );
						}
					}
					else {
						p1.moveTo( (float)pts[i-1].getX(), (float)pts[i-1].getY() );
						p1.lineTo( (float)pts[i].getX(), (float)pts[i].getY() );
					}
					
				}
				p1.closePath();				
				p.append( p1, false );
			}
			p.setWindingRule(p.WIND_EVEN_ODD);
			path = p;
		}
	}
	public int writeShape( OutputStream out ) throws IOException {
		return super.writeShape(out);
	}
}

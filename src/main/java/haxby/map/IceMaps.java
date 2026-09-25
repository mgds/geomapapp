package haxby.map;

import haxby.db.ice.*;
import haxby.proj.*;

import java.awt.image.*;
import java.awt.geom.*;
import java.awt.*;
import java.io.*;

public class IceMaps {
	public static void main(String[] args) {
		try {
			boolean[] landMask = new boolean[600*600];
		//	BufferedImage mask = new BufferedImage(600,600,BufferedImage.TYPE_INT_RGB);
		//	Graphics2D g = mask.createGraphics();
			PolarStereo proj = new PolarStereo(new Point2D.Double(300, 300),
					-90., 1, PolarProjection.NORTH);
			double scale = 12;
			double radius = proj.getRadius(89);
			scale = scale/radius;
			proj = new PolarStereo(new Point2D.Double(300, 300),
			-90., scale,PolarProjection.NORTH);
			PolarStereo ps = (PolarStereo)IBCAO.getProjection();
			boolean[] land = IBCAO.getMask();
			for( int y=0 ; y<600 ; y++) {
				for(int x=0 ; x<600 ; x++) {
					Point2D p = proj.getRefXY( new Point(x, y) );
					Point2D p1 = ps.getMapXY( p);
					int i = (int) (Math.rint(p1.getX()) + 2323*Math.rint(p1.getY()));
//	System.out.println( p.getX() +"\t"+ p.getY() +"\t"+ p1.getX() +"\t"+ p1.getY() +"\t"+ i +"\t"+ land.length);
					landMask[x+600*y] = land[i];
				}
			}
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(
					new FileOutputStream( "landmask.600" )));
			int i=0;
			while( i<600*600 ) {
				int n=0;
				while( i<600*600 && !landMask[i]) {
					n++;
					i++;
				}
				out.writeInt(n);
				if( i==600*600) break;
				n=0;
				while( i<600*600 && landMask[i]) {
					n++;
					i++;
				}
				out.writeInt(n);
				if( i==600*600) break;
			}
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}

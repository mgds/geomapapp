package org.geomapapp.grid;

import org.geomapapp.geom.*;

import java.awt.Rectangle;
import java.awt.geom.*;
import java.io.*;

import ucar.nc2.*;
import ucar.ma2.*;

public class NetCDFGrid2D {
	public static void createStandardGrd( 
			Grid2D grid, 
			String name ) throws IOException {
		createStandardGrd( grid, new File(name) );
	}
	public static void createStandardGrd( 
			Grid2D grid, 
			File file ) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream( file ));
		createStandardGrd( grid, out );
	}
	public static void createStandardGrd( 
			Grid2D grid, 
			OutputStream output ) throws IOException {
		createStandardGrd( grid, null, output );
	}
	public static void createStandardGrd( 
			Grid2D grid, 
			Grid2D.Boolean mask, 
			String name ) throws IOException {
		createStandardGrd( grid, mask, new File(name) );
	}
	public static void createStandardGrd( 
			Grid2D grid, 
			Grid2D.Boolean mask, 
			File file ) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream( file ));
		createStandardGrd( grid, mask, out );
	}
	public static void createStandardGrd( 
			Grid2D grid, 
			Grid2D.Boolean mask, 
			OutputStream output ) throws IOException {
		Rectangle bounds = grid.getBounds();
		int nx = bounds.width;
		int ny = bounds.height;
		MapProjection proj = grid.getProjection();
	//	float[] z = grid.getGrid();
	//	byte[] msk = (mask!=null) ? mask.getGrid() : new byte[0];
		double[] wesn = new double[4];
		Point2D.Double p2d = new Point2D.Double( 
					bounds.getX(), 
					bounds.getY() );
		Point2D pt = proj.getRefXY( p2d );
		wesn[0] = pt.getX();
		wesn[3] = pt.getY();
		double north = pt.getY();
		p2d.x = bounds.getX() + bounds.width-1.;
		p2d.y = bounds.getY() + bounds.height-1.;
		pt = proj.getRefXY( p2d );
		wesn[1] = pt.getX();
		if( wesn[1]<wesn[0] ) wesn[1] += 360.;
		if( wesn[0]>180. ) {
			wesn[0] -=360.;
			wesn[1] -=360.;
		}
		wesn[2] = pt.getY();
		double south = pt.getY();
		double dy = (north-south) / (bounds.height-1.);

		int k=0;
		float minZ = 10000f;
		float maxZ = -10000f;
		float[] newZ = new float[ bounds.height ];
		double[] yy = new double[ bounds.height ];
		int[] i0 = new int[ bounds.height ];
		for( int y=0 ; y<bounds.height ; y++) {
			p2d.y = north-y*dy;
			yy[y] = proj.getMapXY( p2d ).getY()-bounds.y;
			double y0 = Math.floor( yy[y] );
		//	yy[y] -= y0;
			if( y0<1. ) y0=1.;
			if( y0>bounds.height-3 ) y0=bounds.height-3.;
			i0[y] = (int)Math.rint(y0)-1;
			yy[y] -= y0;
		}
		k=0;
		float[] z = new float[bounds.width*bounds.height];
		for( int x=0 ; x<bounds.width ; x++ ) {
			for( int y=0 ; y<bounds.height ; y++ ) {
				if( mask!=null && 
					mask.booleanValue(x+bounds.x, y+bounds.y)) newZ[y]=Float.NaN;
				else newZ[y] = (float)grid.valueAt(x+bounds.x, y+bounds.y);
			}
			z[x] = (float)grid.valueAt(x+bounds.x, bounds.y);
			z[x+bounds.width*(bounds.height-1)] = 
				(float)grid.valueAt(x+bounds.x, bounds.y+bounds.height-1);
			for( int y=1 ; y<bounds.height-1 ; y++, k+=bounds.width ) {
				k = x+bounds.width*y;
				z[k] = (float)Interpolate2D.cubic( newZ, i0[y], yy[y] );
				if( !Float.isNaN( z[k] )) {
					if( z[k]>maxZ ) maxZ=z[k];
					if( z[k]<minZ ) minZ=z[k];
				}
			}
		}
		ClassLoader loader = NetCDFGrid2D.class.getClassLoader();
		BufferedInputStream in = new BufferedInputStream( 
			loader.getResourceAsStream("org/geomapapp/resources/grid/netCDF_header" ));
		byte[] header = new byte[728];
		int len=728;
		int off=0;
		int nread;
		while( off<728 ) {
			nread = in.read( header, off, len );
			off += nread;
			len -= nread;
		}
		in.close();

		DataOutputStream out = new DataOutputStream( output );

		out.write( header, 0, 40 );
		out.writeInt( nx*ny );
		out.write( header, 44, 656-44);
		for( k=0 ; k<4 ; k++) out.writeDouble( wesn[k] );
		out.writeDouble( minZ );
		out.writeDouble( maxZ );
		double dx = (wesn[1] - wesn[0]) / (nx-1);
		out.writeDouble( dx );
		dx = (wesn[3] - wesn[2]) / (ny-1);
		out.writeDouble( dx );
		out.writeInt( nx );
		out.writeInt( ny );
		for( k=0 ; k<nx*ny ; k++ ) out.writeFloat( z[k] );
		out.close();
	}
}

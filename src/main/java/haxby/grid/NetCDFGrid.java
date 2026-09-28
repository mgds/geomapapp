package haxby.grid;

import haxby.proj.Projection;

import java.awt.geom.Point2D;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class NetCDFGrid {
	public static void createStandardGrd( XGrid_Z grid, String name ) throws IOException {
		createStandardGrd( grid, new File(name) );
	}
	public static void createStandardGrd( XGrid_Z grid, File file ) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream( file ));
		createStandardGrd( grid, out );
	}
	public static void createStandardGrd( XGrid_Z grid, OutputStream output ) throws IOException {
		createStandardGrd( grid, null, output );
	}
	public static void createStandardGrd( XGrid_Z grid, Mask mask, String name ) throws IOException {
		createStandardGrd( grid, mask, new File(name) );
	}
	public static void createStandardGrd( XGrid_Z grid, Mask mask, File file ) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream( file ));
		createStandardGrd( grid, mask, out );
	}
	
	public static void createStandardGrd( XGrid_Z grid, Mask mask, OutputStream output ) throws IOException {
		createStandardGrd(grid.getGrid(), grid.getSize(), grid.getProjection(), mask, output);
	}
	
	public static void createStandardGrd( float[] z, java.awt.Dimension dim, Projection proj, Mask mask, OutputStream output ) throws IOException {
		int nx = dim.width;
		int ny = dim.height;
		byte[] msk = (mask!=null) ? mask.getGrid() : new byte[0];
		double[] wesn = new double[4];
		Point2D.Double p2d = new Point2D.Double( 0., 0. );
		Point2D pt = proj.getRefXY( p2d );
		wesn[0] = pt.getX();
		wesn[3] = pt.getY();
		double north = pt.getY();
		p2d.x = dim.width-1.;
		p2d.y = dim.height-1.;
		pt = proj.getRefXY( p2d );
		wesn[1] = pt.getX();
		if( wesn[1]<wesn[0] ) wesn[1] += 360.;
		if( wesn[0]>180. ) {
			wesn[0] -=360.;
			wesn[1] -=360.;
		}
		wesn[2] = pt.getY();
		double south = pt.getY();
		double dy = (north-south) / (dim.height-1.);

		int k=0;
		float minZ = 10000f;
		float maxZ = -10000f;
		float[] newZ = new float[ dim.height ];
		double[] yy = new double[ dim.height ];
		int[] i0 = new int[ dim.height ];
		for( int y=0 ; y<dim.height ; y++) {
			p2d.y = north-y*dy;
			yy[y] = proj.getMapXY( p2d ).getY();
			double y0 = Math.floor( yy[y] );
			yy[y] -= y0;
			if( y0<1. ) y0=1.;
			if( y0>dim.height-3 ) y0=dim.height-3.;
			i0[y] = (int)Math.rint(y0)-1;
		}
		int offset = 0;
		for( int x=0 ; x<dim.width ; x++, offset++ ) {
			k = offset;
			for( int y=0 ; y<dim.height ; y++, k+=dim.width ) {
				if( mask!=null && msk[k]==0 ) newZ[y]=Float.NaN;
				else newZ[y] = z[k];
			}
			k = offset+dim.width;
			for( int y=1 ; y<dim.height-1 ; y++, k+=dim.width ) {
				z[k] = (float)Interpolate.cubic( newZ, i0[y], yy[y] );
				if( !Float.isNaN( z[k] )) {
					if( z[k]>maxZ ) maxZ=z[k];
					if( z[k]<minZ ) minZ=z[k];
				}
			}
		}
		ClassLoader loader = NetCDFGrid.class.getClassLoader();
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
	
//	public static void createGrd( XGrid_Z grid, String name ) throws IOException {
//		java.awt.Dimension dim = grid.getSize();
//		int nx = dim.width;
//		int ny = dim.height;
//		Projection proj = grid.getProjection();
//		float[] z = grid.getGrid();
//		double[] wesn = new double[4];
//		Point2D.Double p2d = new Point2D.Double( 0., 0. );
//		Point2D pt = proj.getRefXY( p2d );
//		wesn[0] = pt.getX();
//		wesn[3] = pt.getY();
//		double north = pt.getY();
//		p2d.x = dim.width-1.;
//		p2d.y = dim.height-1.;
//		pt = proj.getRefXY( p2d );
//		wesn[1] = pt.getX();
//		if( wesn[1]<wesn[0] ) wesn[1] += 360.;
//		if( wesn[0]>180. ) {
//			wesn[0] -=360.;
//			wesn[1] -=360.;
//		}
//		wesn[2] = pt.getY();
//		double south = pt.getY();
//		double dy = (north-south) / (dim.height-1.);
//
//		int k=0;
//		float minZ = 10000f;
//		float maxZ = -10000f;
//		float[] newZ = new float[ dim.height ];
//		double[] yy = new double[ dim.height ];
//		int[] i0 = new int[ dim.height ];
//		for( int y=0 ; y<dim.height ; y++) {
//			p2d.y = north-y*dy;
//			yy[y] = proj.getMapXY( p2d ).getY();
//			double y0 = Math.floor( yy[y] );
//			yy[y] -= y0;
//			if( y0<1. ) y0=1.;
//			if( y0>dim.height-3 ) y0=dim.height-3.;
//			i0[y] = (int)Math.rint(y0)-1;
//		}
//		int offset = 0;
//		for( int x=0 ; x<dim.width ; x++, offset++ ) {
//			k = offset;
//			for( int y=0 ; y<dim.height ; y++, k+=dim.width ) newZ[y] = z[k];
//			k = offset+dim.width;
//			for( int y=1 ; y<dim.height-1 ; y++, k+=dim.width ) {
//				z[k] = (float)Interpolate.cubic( newZ, i0[y], yy[y] );
//				if( !Float.isNaN( z[k] )) {
//					if( z[k]>maxZ ) maxZ=z[k];
//					if( z[k]<minZ ) minZ=z[k];
//				}
//			}
//		}
//		NetcdfFileWriteable nc = new NetcdfFileWriteable();
//		nc.setName(name);
//		nc.addGlobalAttribute("title","Bathymetry Grid");
//		nc.addGlobalAttribute("source", "\n\tProjection: Cylindrical Equidistant\n\t"
//				+ "this grid created by the program MapApp 1.0b");
//		Dimension[] dims = new Dimension[1];
//		dims[0] = nc.addDimension("side", 2);
//		nc.addVariable("x_range", double.class, dims);
//		nc.addVariable("y_range", double.class, dims);
//		nc.addVariable("z_range", double.class, dims);
//		nc.addVariable("spacing", double.class, dims);
//		nc.addVariable("dimension", int.class, dims);
//		nc.addVariableAttribute("x_range", "units", "Longitude");
//		nc.addVariableAttribute("y_range", "units", "Latitude");
//		nc.addVariableAttribute("z_range", "units", "Eleveation (m)");
//
//		Dimension[] xydims  = new Dimension[1];
//		xydims[0] = nc.addDimension("xysize", nx*ny);
//		nc.addVariable("z", float.class, xydims);
//		nc.addVariableAttribute("z", "scale_factor", new Double(1));
//		nc.addVariableAttribute("z", "add_offset", new Double(0));
//		nc.addVariableAttribute("z", "node_offset", new Integer(0));
//		nc.addVariableAttribute("z", "fill_value", new Float( Float.NaN ) );
//
//		nc.create();
//		try {
//			nc.write( "x_range", Array.factory( new double[] {wesn[0], wesn[1] } ));
//			nc.write( "y_range", Array.factory( new double[] {wesn[2], wesn[3] } ));
//			nc.write( "z_range", Array.factory( new double[] {(double)minZ, (double)maxZ } ));
//			nc.write( "spacing", Array.factory( new double[] {
//						(wesn[1] - wesn[0]) / (nx-1),
//						(wesn[3] - wesn[2]) / (ny-1) } ));
//			nc.write( "dimension", Array.factory( new int[] { nx, ny } ));
//		} catch (InvalidRangeException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		try {
//			nc.write( "z", Array.factory( z ));
//		} catch( Throwable ex ) {
//			throw new IOException("error writing nc array");
//		}
//		nc.close();
//	}
}

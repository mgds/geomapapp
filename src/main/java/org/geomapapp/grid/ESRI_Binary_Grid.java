package org.geomapapp.grid;

import org.geomapapp.geom.*;
import org.geomapapp.util.ParseLink;

import java.awt.geom.Point2D;
import java.awt.GridLayout;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import javax.swing.*;

public class ESRI_Binary_Grid {
	Grid2D.Float grd;
	MapProjection proj;
	String filename;
	int width,
		height,
		zone;
	float[] grid;
	double x0,
		   y0,
		   dx,
		   nodata;
	public double zMin = Double.MAX_VALUE,
				  zMax = - Double.MAX_VALUE;
	boolean xLLCenter,
			yLLCenter;
	boolean msbfirst = true;
	boolean headerRead = false;

	public ESRI_Binary_Grid(File header) {
		this.filename = header.getPath();
	}

	public void readHeader() throws IOException {
		if (headerRead) return;

		Vector props = new Vector();
		BufferedReader in = new BufferedReader(
			new FileReader(filename));
		String s = in.readLine();
		StringTokenizer st = new StringTokenizer(s);
		while( st.countTokens()==2 ) {
			props.add( new Object[] { st.nextToken().toLowerCase(), st.nextToken().toLowerCase() });
			s = in.readLine();
			if (s == null)
				break;
			st = new StringTokenizer(s);
		}

		String num = (String)ParseLink.getProperty( props, "ncols");
		width = num==null ? -1 : Integer.parseInt( num );

		num = (String)ParseLink.getProperty( props, "nrows");
		height = num==null ? -1 : Integer.parseInt( num );

		num = (String)ParseLink.getProperty( props, "yllcorner");
		if (num == null) {
			num = (String)ParseLink.getProperty( props, "yllcenter");
			yLLCenter = true;
		}
		y0 = num==null ? Double.NaN : Double.parseDouble( num );

		num = (String)ParseLink.getProperty( props, "xllcorner");
		if (num == null) {
			num = (String)ParseLink.getProperty( props, "xllcenter");
			xLLCenter = true;
		}
		x0 = num==null ? Double.NaN : Double.parseDouble( num );

		num = (String)ParseLink.getProperty( props, "cellsize");
		dx = num==null ? Double.NaN : Double.parseDouble( num );

		num = (String)ParseLink.getProperty( props, "nodata_value");
		nodata = num == null ? Double.NaN : Double.parseDouble(num);

		num = (String)ParseLink.getProperty( props, "utm_zone");
		zone = num==null ? 99 : Integer.parseInt( num );
		
		num = (String)ParseLink.getProperty( props, "byteorder");
		msbfirst = num.equals("msbfirst");

		if (xLLCenter)
			x0 += dx / 2;
		if (yLLCenter)
			y0 += dx / 2;

		if( zone == 99 ) {
			double[] wesn = new double[] {
					x0, x0+(width-1)*dx, y0, y0+(height-1)*dx
					};
			proj = new RectangularProjection( wesn, width, height);
		} else {
			int hemi = 1;
			if( zone<0 ) {
				zone=-zone;
				hemi=2;
			}
			proj = new UTMProjection( x0, y0+(height-1)*dx, dx, dx, zone, 2, hemi);
		}
		in.close();
		headerRead = true;
	}

	public Grid2D getGrid() throws IOException {
		if( grd!=null )return grd;

		readHeader();
		String dataFileName = filename.replaceFirst(".[hH][dD][rR]$", ".flt");
		File dataFile = new File(dataFileName);
		DataInputStream dis = new DataInputStream(
				new BufferedInputStream(new FileInputStream(dataFile)));
		grid = new float[width*height];
		int i=0;
		float val;
		byte[] b = new byte[4];

		while( i<width*height) {
			if (msbfirst) {
				val = dis.readFloat();
				if (val < 1) val = Float.NaN;
			}else {
				try{
					dis.readFully(b);
				} catch(EOFException e) {
					break;
				}

				int bits = (
						((int)(b[3] & 255) << 24) +
						((b[2] & 255) << 16) +
						((b[1] & 255) <<  8) +
						((b[0] & 255) <<  0));
				val = Float.intBitsToFloat(bits);
			}

			if( nodata == val) {
				grid[i++]=Float.NaN;
				break;
			} else {
				zMin = Math.min(zMin, val);
				zMax = Math.max(zMax, val);
				grid[i++]=(float)val;
			}
			
		}
		
		grd = new Grid2D.Float( new java.awt.Rectangle(0,0,width,height), proj);
		grd.setBuffer(grid);
		return grd;
	}

	public void computeMaxMin() throws IOException {
		zMin = Double.MAX_VALUE;
		zMax = -Double.MAX_VALUE;
		String dataFileName2 = filename.replaceFirst(".[hH][dD][rR]$", ".flt");
		File dataFile2 = new File(dataFileName2);
		DataInputStream dis = new DataInputStream(
				new BufferedInputStream(new FileInputStream(dataFile2)));
		grid = new float[width*height];
		int i=0;
		float val;
		byte[] b = new byte[4];

		while( i<width*height) {
			if (msbfirst) { 
				val = dis.readFloat();
				if (val < 1) val = Float.NaN;
			}
			else {
				try{
					dis.readFully(b);
				} catch(EOFException e) {
					break;
				}

				int bits = (
						((b[3] & 255) << 24) +
						((b[2] & 255) << 16) +
						((b[1] & 255) <<  8) +
						((b[0] & 255) <<  0));
				val = Float.intBitsToFloat(bits);
			}

			if( nodata == val) {
				grid[i++]=Float.NaN;
				break;
			} else {
				zMin = Math.min(zMin, val);
				zMax = Math.max(zMax, val);
				grid[i++]=(float)val;
			}
		}
	}
}

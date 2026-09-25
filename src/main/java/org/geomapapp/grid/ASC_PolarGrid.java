package org.geomapapp.grid;

import org.geomapapp.geom.*;
import org.geomapapp.util.ParseLink;

import haxby.proj.PolarStereo;

import java.awt.geom.Point2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class ASC_PolarGrid {
	
	boolean xLLCenter, yLLCenter;
	
	public float[] grid;
	public Grid2D.Float grd;
	public int width, height;
	public double cell_size;
	public double x0, y0;
	public double zMin = Double.MAX_VALUE, zMax = -Double.MAX_VALUE;
	public MapProjection gridProj;

	private double nanValue;
	String filename;

	private boolean headerRead = false;
	
	public ASC_PolarGrid(File file) {
		this.filename = file.getPath();
	}
	
	public void readHeader() throws IOException {
		if( grd!=null || headerRead) return;
		
		Vector props = new Vector();
		BufferedReader in = new BufferedReader(
			new FileReader(filename));
		String s = in.readLine();
		StringTokenizer st = new StringTokenizer(s);
		while( st.countTokens()==2 ) {
			System.out.println( s );
			props.add( new Object[] { st.nextToken().toLowerCase(), st.nextToken() });
			s = in.readLine();
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
		cell_size = num==null ? Double.NaN : Double.parseDouble( num );

		num = (String)ParseLink.getProperty( props, "nodata_value");
		nanValue = num == null ? Double.NaN : Double.parseDouble(num);

		if (xLLCenter)
			x0 += cell_size / 2;
		if (yLLCenter)
			y0 += cell_size / 2;
		
		y0 = -(cell_size * (height-1) +y0);

		in.close();
		
		headerRead = true;
	}
	
	public void setProjection(MapProjection proj)
	{
		this.gridProj = proj;
	}
	
	public Grid2D getGrid() throws IOException {
		if( grd!=null )return grd;
		
		readHeader();
		
		BufferedReader in = new BufferedReader(
				new FileReader(filename));
		String s;
		StringTokenizer st;
		do {
			s = in.readLine();
			st = new StringTokenizer(s);
		} while (st.countTokens() == 2);

		grid = new float[width*height];
		int i=0;
		while( true ) {
			st = new StringTokenizer(s);
			while( st.hasMoreTokens() && i<width*height) {
				s = st.nextToken();
				double val = Double.parseDouble( s );
				if( nanValue == val) { 
					grid[i++]=Float.NaN;
				} else {
					zMin = Math.min(zMin, val);
					zMax = Math.max(zMax, val);
					grid[i++]=(float)val;
				}
			}
			if( (s=in.readLine())==null )
			{
				while (i < width * height)
					grid[i++]=Float.NaN;
				break;
			}
		}
		
		in.close();
		
		grd = new Grid2D.Float(
				new java.awt.Rectangle(
						(int) (x0 / cell_size),
						(int) (y0 / cell_size),
						width,
						height), gridProj);
		grd.setBuffer(grid);
		return grd;
	}
}

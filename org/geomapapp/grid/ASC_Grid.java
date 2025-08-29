package org.geomapapp.grid;

import org.geomapapp.geom.*;
import org.geomapapp.util.ParseLink;

import java.awt.geom.Point2D;
import java.awt.GridLayout;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class ASC_Grid {

	boolean xLLCenter, yLLCenter;
	float[] grid;
	Grid2D.Float grd;
	int width, height;
	double dx;
	double x0, y0;
	public double zMin = Double.MAX_VALUE, zMax = -Double.MAX_VALUE;
	MapProjection proj;
	int zone;
	double nodata;
	String filename;
	private boolean headerRead = false;
	
	public ASC_Grid(File file) {
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
			props.add( new Object[] { st.nextToken().toLowerCase(), st.nextToken() });
			s = in.readLine();
			st = new StringTokenizer(s);
		}

		String num = (String)ParseLink.getProperty( props, "ncols");
		width = num==null ? -1 : Integer.parseInt( num.replace(',', '.') );

		num = (String)ParseLink.getProperty( props, "nrows");
		height = num==null ? -1 : Integer.parseInt( num.replace(',', '.') );

		num = (String)ParseLink.getProperty( props, "yllcorner");
		if (num == null) {
			num = (String)ParseLink.getProperty( props, "yllcenter");
			yLLCenter = true;
		}
		y0 = num==null ? Double.NaN : Double.parseDouble( num.replace(',', '.') );

		num = (String)ParseLink.getProperty( props, "xllcorner");
		if (num == null) {
			num = (String)ParseLink.getProperty( props, "xllcenter");
			xLLCenter = true;
		}
		x0 = num==null ? Double.NaN : Double.parseDouble( num.replace(',', '.') );

		num = (String)ParseLink.getProperty( props, "cellsize");
		dx = num==null ? Double.NaN : Double.parseDouble( num.replace(',', '.') );

		num = (String)ParseLink.getProperty( props, "nodata_value");
		nodata = num == null ? Double.NaN : Double.parseDouble(num.replace(',', '.'));

		num = (String)ParseLink.getProperty( props, "utm_zone");
		zone = num==null ? 99 : Integer.parseInt( num.replace(',', '.') );

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

	public Grid2D getGrid(ImportGrid ig) throws IOException {
		if( grd!=null )return grd;

		readHeader();
		readGrid(ig);

		grd = new Grid2D.Float( new java.awt.Rectangle(0,0,width,height), proj);
		grd.setBuffer(grid);
		return grd;
	}

	int showConfirm() {
		JPanel panel = new JPanel(new GridLayout(0,2));
		ButtonGroup gp = new ButtonGroup();
		JRadioButton geog = new JRadioButton("Geographic");
		panel.add( geog );
		panel.add( new JLabel(" "));
		JRadioButton utm = new JRadioButton("UTM Zone");
		panel.add( utm );
		gp.add( geog );
		gp.add( utm );
		JTextField utmZone = new JTextField("");
		if( zone==99 ) {
			geog.setSelected(true);
			panel.add( utmZone );
		} else {
			return JOptionPane.OK_OPTION;
		}
		while( true ) {
			int ok = JOptionPane.showConfirmDialog( null, panel, "Specify Projection", JOptionPane.OK_CANCEL_OPTION );
			if( ok==JOptionPane.CANCEL_OPTION )return ok;
			if( geog.isSelected() ) {
				double[] wesn = new double[] {
						x0, x0+(width-1)*dx, y0, y0+(height-1)*dx
						};
				proj = new RectangularProjection( wesn, width, height);
				return ok;
			} else {
				try {
					zone = Integer.parseInt(utmZone.getText());
				} catch(NumberFormatException e) {
					continue;
				}
				int hemi = 1;
				if( zone<0 ) {
					zone=-zone;
					hemi=2;
				}
				proj = new UTMProjection( x0, y0+(height-1)*dx, dx, dx, zone, 2, hemi);
				return ok;
			}
		}
	}
	
	public void readGrid(ImportGrid ig) throws IOException {
		zMin = Double.MAX_VALUE;
		zMax = -Double.MAX_VALUE;
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
		ig.appendNewText("\nReading " + filename + "… ");
		while( true ) {
			st = new StringTokenizer(s);
			while( st.hasMoreTokens() && i<width*height) {
				s = st.nextToken();
				double val = Double.parseDouble( s.replace(',', '.') );
				if( nodata == val) { 
					grid[i++]=Float.NaN;
				} else {
					zMin = Math.min(zMin, val);
					zMax = Math.max(zMax, val);
					grid[i++]=(float)val;
				}
				if(ig != null && (i % (grid.length/100) == 0 || i+1 >= grid.length)) {
					ig.showPercent(i*100/grid.length);
				}
			}
			if( (s=in.readLine())==null ) {
				while (i < width * height) {
					grid[i++]=Float.NaN;
					if(ig != null && (i % (grid.length/100) == 0 || i+1 >= grid.length)) {
						ig.showPercent(100*i/grid.length);
					}
				}
				break;
			}
		}
		ig.showPercent(100);
		in.close();
	}
	
	public static void main(String[] args) {
		if( args.length!=1 ) {
			System.err.println("usage: java xb.grid.ASC_Grid filename");
			System.exit(-1);
		}
		ASC_Grid asc = new ASC_Grid(new File(args[0]));
		try {
			Grid2D grd = asc.getGrid(null);
			double[] wesn = grd.getWESN();
			System.out.println( wesn[0] +"\t"+ wesn[1] +"\t"+ wesn[2] +"\t"+ wesn[3] );
		} catch(Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}

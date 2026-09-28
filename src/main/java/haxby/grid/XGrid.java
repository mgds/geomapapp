package haxby.grid;

import haxby.proj.*;
import java.io.*;
import java.awt.*;
import java.util.*;
import java.awt.geom.Point2D;

public class XGrid implements Grid, Serializable {
	Projection proj;
	float[] grid;
	double x0, y0, scale;
	int width, height;
	public XGrid(double x0, double y0, 
			int width, int height, 
			double scale, 
			Projection proj, 
			float[] z) {
		this.x0 = x0;
		this.y0 = y0;
		this.width = width;
		this.height = height;
		this.scale = scale;
		grid = z;
		this.proj = proj;
	}
	public float[] getGrid() {
		return grid;
	}
	public double[] getLocation() {
		return new double[] {x0, y0};
	}
	public double getScale() {
		return scale;
	}
	public Dimension getSize() {
		return new Dimension(width, height);
	}
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public double sampleRef(double refX, double refY) {
		Point2D p = proj.getMapXY(refX, refY);
		return sample(p.getX(), p.getY());
	}
	public double sample(double x, double y) {
		double xx = (x-x0) * scale;
		double yy = (y-y0) * scale;
		double zz = Interpolate.bicubic(grid, width, height, xx, yy);
		return zz;
	}
	public double sampleLinear(double x, double y) {
		double xx = (x-x0) * scale;
		double yy = (y-y0) * scale;
		int ix = (int)Math.floor(xx);
		if(ix<0 || ix>=width-1) return Double.NaN;
		int iy = (int)Math.floor(yy);
		if(iy<0 || iy>=height-1) return Double.NaN;
		double dx = xx-ix;
		double dy = yy-iy;
		int i = width*iy + ix;
		try {
			return (1.-dx-dy+dx*dy) * grid[i] + 
				(dx-dx*dy)*grid[i+1] +
				(dy-dx*dy) * grid[i+width]
				+ dx*dy * grid[i+width+1];
		} catch (ArrayIndexOutOfBoundsException ex) {
			return Double.NaN;
		}
	}
	public Projection getProjection() {
		return proj;
	}
	public static XGrid getE00( File file, boolean changeSign ) throws IOException {
		BufferedReader in = new BufferedReader(
			new InputStreamReader(
			new FileInputStream(file)));
		String s = in.readLine();
		s = in.readLine();
		if( !s.startsWith("GRD") ) {
			throw new IOException( "unrecognized format ");
		}
		s = in.readLine();
		StringTokenizer st = new StringTokenizer(s);
		int width = Integer.parseInt( st.nextToken() );
		int height = Integer.parseInt( st.nextToken() );
		
		s = in.readLine();
		double dx = Double.parseDouble( s.substring(0,21).trim() );
		double dy = Double.parseDouble( s.substring(21).trim() );
		s = in.readLine();
		double xmin = Double.parseDouble( s.substring(0,21).trim() );
		double ymin = Double.parseDouble( s.substring(21).trim() );
		s = in.readLine();
		double xmax = Double.parseDouble( s.substring(0,21).trim() );
		double ymax = Double.parseDouble( s.substring(21).trim() );
		System.out.println( file.getName() +"\t"+ width +"\t"+ height);
		System.out.println( dx +"\t"+ ((xmax-xmin)/width) );
		System.out.println( dy +"\t"+ ((ymax-ymin)/height) );
		float[] grid = new float[width*height];
		int k=0;
		for( k=0 ; k<width*height ; k++ ) grid[k]=Float.NaN;
		k=0;
		while( !(s=in.readLine()).trim().equals("EOG") ) {
			int kk=0;
			while( k<width*height && kk<70 ) {
				String ss = s.substring(kk, kk+14 );
				if( !ss.equals( "-0.3402823E+39" ) ) grid[k]=Float.parseFloat(ss);
				k++;
				if( k%width==0 ) break;
				kk += 14;
			}
		}
		while( !(s=in.readLine()).startsWith("PRJ") );
		st = new StringTokenizer( in.readLine() );
		if( !st.nextToken().equals("Projection") ) {
			throw new IOException( "unrecognized projection");
		}
		s = st.nextToken();
		if( !s.equals("UTM") && !s.equals("POLAR") ) {
			throw new IOException( "unrecognized projection");
		}
		if( s.equals("UTM") ) {
			s=in.readLine();
			st = new StringTokenizer( in.readLine() );
			if( !st.nextToken().equals("Zone") ) {
				throw new IOException( "unrecognized projection");
			}
			int zone = Integer.parseInt( st.nextToken() );
			UTMProjection utm = new UTMProjection( xmin+dx/2., ymin+dy/2., dx, dy,
							zone, 2, 1);
			XGrid grd = new XGrid( 0, 0, width, height, 1, utm, grid);
			return grd;
		} else {
			PolarStereo proj = new PolarStereo( new Point2D.Double(-xmin/dx, ymax/dx),
					4., dx, 79.,
					Projection.NORTH,
					Projection.WGS84 );
		//	PolarStereo proj = new PolarStereo( new Point2D.Double(0., 0.),
		//			4., 1, 79.,
		//			Projection.NORTH,
		//			Projection.WGS84 );
//	Point2D p = proj.getRefXY( new Point2D.Double(0.,0.) );
//	System.out.println( p.getX() +"\t"+ p.getY() );
//	p = proj.getRefXY( new Point2D.Double(width-1., 0.) );
//	System.out.println( p.getX() +"\t"+ p.getY() );
//	p = proj.getRefXY( new Point2D.Double(0., height-1.) );
//	System.out.println( p.getX() +"\t"+ p.getY() );
//	p = proj.getRefXY( new Point2D.Double(width-1., height-1.) );
//	System.out.println( p.getX() +"\t"+ p.getY() );
			XGrid grd = new XGrid( 0, 0, width, height, 1, proj, grid);
			return grd;
		}
	}
	public static XGrid getGrd1( File file, boolean changeSign ) throws IOException {
		if( file.getName().toLowerCase().endsWith(".e00") ) return getE00(file, changeSign);
		DataInputStream in = new DataInputStream(
			new BufferedInputStream(
			new FileInputStream(file)));
		int width = in.readInt();
		int height = in.readInt();
		in.readInt();
		in.readInt();
		double[] wesn = new double[] {
				in.readDouble(),
				in.readDouble(),
				in.readDouble(),
				in.readDouble() };
		in.readDouble();
		in.readDouble();
		in.readDouble();
		in.readDouble();
		float scale = (float)in.readDouble();
		float offset = (float)in.readDouble();
		for( int i=0 ; i<800 ; i++) in.readByte();
		float[] z = new float[width*height];
	//	System.out.println( width +"\t"+ height +"\t"+ scale +"\t"+ offset);
		for( int i=0 ; i<width*height ; i++) {
			z[i] = in.readFloat();
			if(Float.isNaN(z[i])) continue;
			z[i] = offset + scale*z[i];
			if(changeSign) z[i]=-z[i];
		}
		in.close();
		RectangularProjection proj = new RectangularProjection(wesn, width, height);
		XGrid grid = new XGrid( 0, 0, width, height, 1, proj, z);
		return grid;
	}
	public static XGrid getGrd1M( File file, 
			boolean changeSign,
			int ellipsoid,
			double scaleLat ) throws IOException {
		DataInputStream in = new DataInputStream(
			new BufferedInputStream(
			new FileInputStream(file)));
		int width = in.readInt();
		int height = in.readInt();
		in.readInt();
		in.readInt();
		double[] wesn = new double[] {
				in.readDouble(),
				in.readDouble(),
				in.readDouble(),
				in.readDouble() };
		in.readDouble();
		in.readDouble();
		in.readDouble();
		in.readDouble();
		float scale = (float)in.readDouble();
		float offset = (float)in.readDouble();
		for( int i=0 ; i<800 ; i++) in.readByte();
		float[] z = new float[width*height];
	//	System.out.println( width +"\t"+ height +"\t"+ scale +"\t"+ offset);
		for( int i=0 ; i<width*height ; i++) {
			z[i] = in.readFloat();
			if(Float.isNaN(z[i])) continue;
			z[i] = offset + scale*z[i];
			if(changeSign) z[i]=-z[i];
		}
		in.close();
		Mercator proj = new Mercator(wesn, scaleLat, ellipsoid, width, height);
		XGrid grid = new XGrid( 0, 0, width, height, 1, proj, z);
		return grid;
	}
	public static XGrid getXgrd( File file ) throws IOException {
		Xgrd xgrd = new Xgrd(file);
		double[] wesn = xgrd.getWESN();
		int width = xgrd.getWidth();
		int height = xgrd.getHeight();
		float[] z = new float[width*height];
		for(int y=0 ; y<height ; y++) {
			short[] row = xgrd.readRow( height-y+1);
			for( int x=0 ; x<width ; x++) {
				if(row[x] == Xgrd.NODATA) z[y*width+x] = Float.NaN;
				else z[y*width+x] = (float)row[x];
			}
		}
		xgrd.close();
		RectangularProjection proj = new RectangularProjection(wesn, width, height);
		XGrid grid = new XGrid( 0, 0, width, height, 1, proj, z);
		return grid;
	}
	public static XGrid getXgrd( File file, double[] wesn ) throws IOException {
		Xgrd xgrd = new Xgrd(file);
		XGrid grid = getXgrd( xgrd, wesn );
		xgrd.close();
		return grid;
	}
	public static XGrid getXgrd( Xgrd xgrd, double[] wesn ) throws IOException {
		double[] wesn0 = xgrd.getWESN();
		int width = xgrd.getWidth();
		int height = xgrd.getHeight();
		double dx = (wesn0[1]-wesn0[0]) / (double)(width-1);
		double dy = (wesn0[3]-wesn0[2]) / (double)(height-1);
		boolean wrap = ( (int)Math.rint(360/dx) == width );
		while(wesn[1] < wesn0[0]) {
			wesn[0] += 360;
			wesn[1] += 360;
		}
		while(wesn[0] > wesn0[1]) {
			wesn[0] -= 360;
			wesn[1] -= 360;
		}
		int x1 = (int)Math.floor( (wesn[0]-wesn0[0]) / dx);
		int x2 = (int)Math.ceil( (wesn[1]-wesn0[0]) / dx);
		int y1 = (int)Math.floor( (wesn[2]-wesn0[2]) / dy);
		int y2 = (int)Math.ceil( (wesn[3]-wesn0[2]) / dy);
		wesn[0] = wesn0[0] + dx*x1;
		wesn[1] = wesn0[0] + dx*x2;
		wesn[2] = wesn0[2] + dy*y1;
		wesn[3] = wesn0[2] + dy*y2;
		int w = x2-x1+1;
		int h = y2-y1+1;
		float[] z = new float[w*h];
		for( int i=0 ; i<w*h ; i++) z[i] = Float.NaN;
		int xx, offset;
		for(int y=y2 ; y>=y1 ; y--) {
			short[] row = xgrd.readRow(y);
			offset = w*(y2-y)-x1;
			for( int x=x1 ; x<=x2 ; x++) {
				xx = x;
				if(wrap) {
					while(xx<0) xx += width;
					while(xx>=width) xx -= width;
				} else if(xx<0 || xx>=width ) {
					continue;
				}
		if(x+offset<0 || x+offset>=w*h || xx<0 || xx>=row.length ) {
			System.out.println("y1 = " + y1 + "\ty2 = " + y2);
			System.out.println("x1 = " + x1 + "\tx2 = " + x2);
			System.out.println("w = " + w + "\th = " + h);
			System.out.println("y = " + y + "\tx = " + x);
			System.out.println("offset = " + offset);
			System.out.println("xx = " + xx);
			break;
		}
			if(row[xx] != Xgrd.NODATA) z[x+offset] = (float)row[xx];
			}
		}
		RectangularProjection proj = new RectangularProjection(wesn, w, h);
		XGrid grid = new XGrid( 0, 0, w, h, 1, proj, z);
		return grid;
	}
	public XGrid SampleXGrid(Projection newproj, double x0, double y0,
				double scale, int width, int height) {
		float[] z = new float[width*height];
		ScaledProjection proj1 = new ScaledProjection( newproj, scale, x0, y0);
		int k = 0;
		for( int y=0 ; y<height ; y++) {
			double yy = y0 + scale*y;
			for( int x=0 ; x<width ; x++, k++) {
			//	Point2D p = proj1.getRefXY(new Point2D.Double(x0+scale*x, yy));
				Point2D p = proj1.getRefXY(new Point(x, y));
				Point2D p1 = proj.getMapXY(p);
				z[k] = (float)sample(p1.getX(), p1.getY());
			}
		}
		return new XGrid(0., 0., width,height, 1., proj1, z);
	}
}

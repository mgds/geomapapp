package haxby.db.scs;

import haxby.map.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class Resample1 {
	String fileName;
	JScrollPane scrollPane;
	byte[][] image;
	JTextField cruise, year, month, day, hour;
	JTextField rotate;
	JCheckBox rotateCB, secCB, dateCB, tileCB;
	JPanel dialog, rotateD, secD, dateD, tileD;
	JPanel buttons;
	JCheckBox breakCB, pickCB;
	double zoom;
	double rotation;
	Vector gaps;
	Vector dates;
	Vector secs;
	Vector tiles;
	JPanel datePanel;
	int w, h;
	public Resample1( String fileName ) {
		this.fileName = fileName;
		w = h = 0;
		try {
			BufferedInputStream in = new BufferedInputStream(
				new FileInputStream( fileName ));
			image = getImage(in);
			w = image[0].length;
			h = image.length;
			System.out.println( w +"\t"+ h );
			String name = fileName.substring( 0, fileName.length()-4) + ".dates";
			BufferedReader bin = new BufferedReader(
				new FileReader( name));
			SDate date;
			StringTokenizer st = new StringTokenizer( bin.readLine() );
			int n = Integer.parseInt( st.nextToken() );
			dates = new Vector( n );
			for(int i=0 ; i<n ; i++) {
				st = new StringTokenizer( bin.readLine() );
				int y = Integer.parseInt( st.nextToken() );
				int m = Integer.parseInt( st.nextToken() );
				int d = Integer.parseInt( st.nextToken() );
				int hr = Integer.parseInt( st.nextToken() );
				double x1 = Double.parseDouble( st.nextToken() );
				double y1 = Double.parseDouble( st.nextToken() );
				double x2 = Double.parseDouble( st.nextToken() );
				double y2 = Double.parseDouble( st.nextToken() );
				if( y1>y2 ) {
					date = new SDate(x1, y1, x2, y2);
				}  else {
					date = new SDate(x2, y2, x1, y1);
				}
				date.year = y;
				date.month = m;
				date.day = d;
				date.hour = hr;
				dates.add( date);
			}
			st = new StringTokenizer( bin.readLine() );
			n = Integer.parseInt( st.nextToken() );
			gaps = new Vector( n+2 );
			gaps.add( new SDate( 0., (double)h, 0., 0.) );
			for(int i=0 ; i<n ; i++) {
				st = new StringTokenizer( bin.readLine() );
				double x1 = Double.parseDouble( st.nextToken() );
				double y1 = Double.parseDouble( st.nextToken() );
				double x2 = Double.parseDouble( st.nextToken() );
				double y2 = Double.parseDouble( st.nextToken() );
				if( y1>y2 ) {
					date = new SDate(x1, y1, x2, y2);
				}  else {
					date = new SDate(x2, y2, x1, y1);
				}
				gaps.add( date);
			}
			gaps.add( new SDate( (double)w, (double)h, (double)w, 0.) );
			Collections.sort( gaps);
			Collections.sort( dates );
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(0);
		}
		for( int k=0 ; k<gaps.size()-1 ; k++) {
			Line2D.Double gap1 = ((SDate)gaps.get(k)).line;
			Line2D.Double gap2 = ((SDate)gaps.get(k+1)).line;
			double x1 = intercept( gap1 );
			double x2 = intercept( gap2 );
			Vector between = new Vector();
			double angle = 0.;
			double nAngle = 0.;
			Vector inside = new Vector();
			for( int i=0 ; i<dates.size() ; i++) {
				Line2D.Double line = ((SDate)dates.get(i)).line;
				double x = intercept( line );
				if( x<x1 || x>x2 )continue;
				inside.add( dates.get(i) );
				double a = (line.x1==line.x2) ?
					0. : Math.atan( (line.x1-line.x2)/(line.y1-line.y2) );
				System.out.println( k +"\t"+ i +"\t"+ a );
				angle += a;
				nAngle++;
			}
			angle /=nAngle;
			System.out.println( k +"\t"+ angle );
			AffineTransform at = new AffineTransform();
			at.rotate( angle );
			Point2D.Double p1 = new Point2D.Double();
			Point2D.Double p2 = new Point2D.Double();
			Point2D.Double p = new Point2D.Double( (double)w, 0.);
			p1 = (Point2D.Double)at.transform(p,p1);
			System.out.println( p1.x +"\t"+ p1.y);

			p = new Point2D.Double( gap1.x1, gap1.y1 );
			p1 = (Point2D.Double)at.transform(p,p1);
			p = new Point2D.Double( gap1.x2, gap1.y2 );
			p2 = (Point2D.Double)at.transform(p,p2);
			Line2D.Double g1 = new Line2D.Double( p1, p2 );
			x1 = intercept( g1 );
			p = new Point2D.Double( gap2.x1, gap2.y1 );
			p1 = (Point2D.Double)at.transform(p,p1);
			p = new Point2D.Double( gap2.x2, gap2.y2 );
			p2 = (Point2D.Double)at.transform(p,p2);
			g1 = new Line2D.Double( p1, p2 );
			x2 = intercept( g1 );
			int ix1 = (int) x1;
			int ix2 = (int) x2;
			if( (ix2-ix1)%2==0 ) ix2++;
			byte[][] im = new byte[h][ix2-ix1+1];
			AffineTransform at1 = new AffineTransform();
			at1.rotate( -angle );
			Point pt = new Point();
			for( int y=0 ; y<h ; y++ ) {
				pt.y = y;
				for( int x=ix1 ; x<=ix2 ; x++) {
					pt.x = x;
					p = (Point2D.Double)at1.transform( pt, p);
					im[y][x-ix1] = sample( p );
				}
			}
			try {
				String name = fileName.substring( 0, fileName.length()-4) + "_"+k+".ras";
				writeRaster( name, im);
				name = fileName.substring( 0, fileName.length()-4) + "_"+k+".dates";
				PrintStream out = new PrintStream( new FileOutputStream( name ));
				out.println( inside.size() +" dates" );
				for( int i=0 ; i<inside.size() ; i++) {
					SDate date = (SDate)inside.get(i);
					Line2D.Double line = date.line;
					p = new Point2D.Double( line.x1, line.y1 );
					p1 = (Point2D.Double)at.transform(p,p1);
					p = new Point2D.Double( line.x2, line.y2 );
					p2 = (Point2D.Double)at.transform(p,p2);
					line = new Line2D.Double( p1, p2 );
					double x = intercept( line );
					if( x<x1 || x>x2 )continue;
					out.println( date.year
						+"\t"+ date.month
						+"\t"+ date.day
						+"\t"+ date.hour
						+"\t"+ p1.x
						+"\t"+ p1.y
						+"\t"+ p2.x
						+"\t"+ p2.y);
				}
				out.println( "0 gaps" );
				out.close();
			} catch(IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	byte sample(Point2D.Double p) {
		int x = (int)Math.floor(p.x);
		if(x<0||x>w-2) return (byte)0xff;
		int y = (int)Math.floor(p.y);
		if(y<0||y>h-2) return (byte)0xff;
		int[][] im = new int[2][2];
		for( int dx=0 ; dx<2 ; dx++ ) {
			for(int dy=0 ; dy<2 ; dy++ ) {
				try {
					im[dx][dy] = 0x000000ff & ((int)image[y+dy][x+dx]);
				} catch( ArrayIndexOutOfBoundsException ex) {
					System.out.println( w +"\t"+ h +"\t"+ (x+dx) +"\t"+ (y+dy) );
					return (byte)0xff;
				}
			}
		}
		double dx = p.x-x;
		double dy = p.y-y;
		double z = im[0][0] * (1.-dx-dy+dx*dy)
			+ im[1][0] * (dx - dx*dy)
			+ im[0][1] * (dy - dx*dy)
			+ im[1][1] * dx*dy;
		int k = (int) Math.rint(z);
		return (byte)k;
	}
	public static double intercept( Line2D.Double line ) {
		if( line.x1==line.x2 ) return line.x1;
		if( line.y1==line.y2 ) return Double.NaN;
		double slope = (line.x2-line.x1) / (line.y2-line.y1);
		return line.x1 - line.y1*slope;
	}
	public byte[][] getImage(InputStream input) throws IOException {
		DataInputStream in = new DataInputStream(input);
		if(in.readInt() != 1504078485) throw new IOException("not a sunraster file");
		int w= in.readInt();
		int h = in.readInt();
		byte[][] im = new byte[h][w];
		in.readInt();
		in.readInt();
		in.readInt();
		in.readInt();
		int length = in.readInt();
		for( int k=0 ; k<length ; k++) in.readByte();
		for(int y=0 ; y<h ; y++) {
			for(int x=0 ; x<w ; x++) {
				im[y][x] = in.readByte();
			}
		}
		return im;
	}
	public void writeRaster( String name, byte[][] raster ) throws IOException {
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(
					new FileOutputStream(name)));
		int w = raster[0].length;
		int h = raster.length;
		out.writeInt(1504078485);
		out.writeInt(w);
		out.writeInt(h);
		out.writeInt(8);
		out.writeInt(w*h);
		out.writeInt(1);
		out.writeInt(1);
		out.writeInt(768);
		byte[] gray = new byte[256];
		for(int i=0 ; i<gray.length ; i++) gray[i]=(byte)i;
		out.write(gray);
		out.write(gray);
		out.write(gray);
		for( int y=0 ; y<h ; y++) {
			for( int x=0 ; x<w ; x++) {
				out.writeByte( raster[y][x] );
			}
		}
		out.close();
	}
	class SDate implements Comparable {
		Line2D.Double line;
		int year, month, day, hour;
		public SDate( double x1, double y1, double x2, double y2 ) {
			line = new Line2D.Double(x1, y1, x2, y2);
		}
		public void set( int y, int m, int d, int h) {
			year = y;
			month = m;
			day = d;
			hour = h;
		}
		public int compareTo( Object obj ) {
			try {
				Line2D.Double l = ((SDate)obj).line;
				double dx = Resample1.intercept(line) - Resample1.intercept(l);
				if(dx>0.) return 1;
				else if( dx<0. ) return -1;
				else return 0;
			} catch(ClassCastException ex) {
				return -1;
			}
		}
	}
	public static void main(String[] args) {
		if( args.length != 1) {
			System.out.println( "usage: java Resample1 dir");
			System.exit(0);
		}
		new Resample1( args[0] );
	}
}
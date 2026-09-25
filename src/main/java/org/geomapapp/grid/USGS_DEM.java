package org.geomapapp.grid;

import haxby.util.URLFactory;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import org.geomapapp.geom.MapProjection;

public class USGS_DEM {
	Grid2D.Short grid;
	MapProjection proj;
	short[] data;
	double[] wesn;
	double dx, dy;
	double scale;
	int width;
	int height;
	double zmin, zmax;
	URL url;
	boolean zipped;
	int level;
	int pattern;
	int pCode;
	int zone;
	int xyUnits;
	int zUnits;
	int datum;
	
	public USGS_DEM( File file ) throws IOException {
		this( file.toURI().toURL());
	}
	public USGS_DEM( String url ) throws IOException {
		this( URLFactory.url(url) );
	}
	public USGS_DEM( URL url ) throws IOException {
		this.url = url;
		zipped = url.toString().endsWith(".gz");
	}
	public void readHeader() throws IOException {
		DataInputStream in = zipped
			? new DataInputStream(
				new GZIPInputStream( url.openStream() ))
			: new DataInputStream( url.openStream() );
		readHeader(in);
		in.close();
	}
	public void readHeader(DataInputStream in) throws IOException {
		byte[] buffer = new byte[1024];
		in.readFully( buffer);
		level = Integer.parseInt( (new String(buffer, 144, 6)).trim() );
		pattern = Integer.parseInt( (new String(buffer, 150, 6)).trim() );
		pCode = Integer.parseInt( (new String(buffer, 156, 6)).trim() );
		if( pCode==2 ) {
			in.close();
			throw new IOException("State Planes not yet supported");
		}
		zone = Integer.parseInt( (new String(buffer, 162, 6)).trim() );
		xyUnits = Integer.parseInt( (new String(buffer, 528, 6)).trim() );
		zUnits = Integer.parseInt( (new String(buffer, 534, 6)).trim() );
		wesn = new double[4];
		String[] s = new String[] {
			(new String(buffer, 546, 24)).trim().toLowerCase(),
			(new String(buffer, 642, 24)).trim().toLowerCase(),
			(new String(buffer, 570, 24)).trim().toLowerCase(),
			(new String(buffer, 618, 24)).trim().toLowerCase()
		};
		for( int k=0 ; k<4 ; k++) {
			if( s[k].indexOf("d")>0 ) {
				StringTokenizer st = new StringTokenizer(s[k], "d");
				s[k] = st.nextToken() +"e"+ st.nextToken();
			}
			wesn[k] = Double.parseDouble( s[k] );
		}
		dx = Double.parseDouble( (new String(buffer, 816, 12)).trim() );
		dy = Double.parseDouble( (new String(buffer, 828, 12)).trim() );
		scale = Double.parseDouble( (new String(buffer, 840, 12)).trim() );
		height = Integer.parseInt( (new String(buffer, 852, 6)).trim() );
		width = Integer.parseInt( (new String(buffer, 858, 6)).trim() );
		datum = Integer.parseInt( (new String(buffer, 890, 2)).trim() );
		if( height==1 ) height = 1+(int)Math.rint( (wesn[3]-wesn[2])/dy );
		if( datum==2 ) datum=3;
		else if( datum!=1 )datum=2;
		if( zone==0 ) {
		}
System.out.println( zone +"\t"+ datum +"\t"+ width +"\t"+ height +"\t"+ dx +"\t"+ dy +"\t"+ scale);
System.out.println( wesn[0] +"\t"+ wesn[1] +"\t"+ wesn[1] +"\t"+ wesn[1]);
	}
	public void readData() throws IOException {
		DataInputStream in = zipped
			? new DataInputStream(
				new GZIPInputStream( url.openStream() ))
			: new DataInputStream( url.openStream() );
		readHeader(in);
		byte[] buffer = new byte[1024];
		boolean start = true;
		zmin = zmax = 0.;
		data = new short[width*height];
		for( int i=0 ; i<width ; i++) {
			in.readFully( buffer);
			int y1 = Integer.parseInt( (new String(buffer, 1, 6)).trim() )-1;
			int x1 = Integer.parseInt( (new String(buffer, 6, 6)).trim() );
			int y2 = y1+Integer.parseInt( (new String(buffer, 1, 6)).trim() );
			String off = (new String(buffer, 72, 24)).trim().toLowerCase();
			if( off.indexOf("d")>0 ) {
				StringTokenizer st = new StringTokenizer(off, "d");
				off = st.nextToken() +"e"+ st.nextToken();
			}
			double offset = Double.parseDouble(off);
			int idx = 144;
			int y = height-1;
			while( y>=0 ) {
				int idx1 = idx+6;
				if( idx1>=1024 ) {
					in.readFully( buffer);
					idx=0;
				}
				short s =-32767;
				try {
					s = Short.parseShort( (new String( buffer, idx, 6)).trim() );
				} catch(NumberFormatException ex) {
					System.out.println( i+"\t"+y+"\t"+(new String( buffer, idx, 6)));
				}
				if( s==-32767 ) {
					s = Grid2D.Short.NaN;
				} else if( start ) {
					zmin = zmax = scale*s;
					start = false;
				} else {
				//	System.out.println( i+"\t"+y+"\t"+s);
					if(zmin>scale*s) zmin = scale*s;
					else if(zmax<scale*s) zmax = scale*s;
				}
				data[ i+width*y ] = s;
				y--;
				idx += 6;
			}
		}
		in.close();
	}
	public static void main(String[] args) {
		if( args.length!=1 ) {
			System.out.println( "usage: java org.geomapapp.grid.USGS_DEM file");
			System.exit(0);
		}
		try {
			USGS_DEM dem = new USGS_DEM(new File(args[0]));
			dem.readData();
			System.out.println( dem.width +"\t"+ dem.height );
			System.out.println( dem.zmin +"\t"+ dem.zmax );
			System.out.println( dem.dx +"\t"+ dem.scale );
			System.out.println( dem.wesn[0] +"\t"+ dem.wesn[2] );
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}

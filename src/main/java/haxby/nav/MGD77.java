package haxby.nav;

import haxby.proj.*;
import java.io.*;
import java.util.*;

public class MGD77 {
	public String name;
	public int[][] xy;
	public Nav nav;
	public MGD77( String file, String name ) throws IOException {
		this.name = name;
		String s;
		Vector points;
		Calendar cal = Calendar.getInstance( TimeZone.getTimeZone( "GMT" ));
		BufferedReader in = new BufferedReader(
				new FileReader( file ));
		nav = new Nav( "01" );
		double lon, lat;
		while( (s=in.readLine()) != null ) {
			try {
//				StringTokenizer st = new StringTokenizer( s,"+" );
//				if(st.countTokens()>=2) s=st.nextToken() + st.nextToken();
				int val = Integer.parseInt( s.substring(13, 16) );
				if( val>90 ) val += 1900;
				else val += 2000;
				cal.set( cal.YEAR, val );
				val = Integer.parseInt( s.substring( 16, 18 ).trim());
				cal.set( cal.MONTH, val-1 );
				val = Integer.parseInt( s.substring( 18, 20 ).trim());
				cal.set( cal.DAY_OF_MONTH, val );
				val = Integer.parseInt( s.substring( 20, 22 ).trim());
				cal.set( cal.HOUR_OF_DAY, val );
				val = Integer.parseInt( s.substring( 22, 24 ).trim());
				cal.set( cal.MINUTE, val );
				val = Integer.parseInt( s.substring( 24, 26 ).trim());
				cal.set( cal.SECOND, val );
				lon = 1.e-05*(double)Integer.parseInt( s.substring( 35, 44 ).trim());
				lat = 1.e-05*(double)Integer.parseInt( s.substring( 27, 35 ).trim());

//				System.out.println("Longitude: " + lon);
//				System.out.println("Latitude: " + lat);

				if( lon==0. && lat==0. ) continue;
				if( Math.abs(lat)>88. ) continue;
			} catch( Exception ex) {
				continue;
			}
			if ( lon < 0 )	{
				lon += 360.;
			}

			nav.addPoint( (int) (cal.getTime().getTime()/1000L), lon, lat);

//			System.out.println("Latitude: " + lon);
		//	int[] entry = new int[] { (int) (cal.getTime().getTime()/1000L),
		//			Integer.parseInt( s.substring( 35, 44 ).trim()),
		//			Integer.parseInt( s.substring( 27, 35 ).trim())
		//	};
		//	System.out.println( cal.get(cal.YEAR) +"\t"+ cal.get(cal.DAY_OF_YEAR) +"\t"+
		//			cal.get(cal.HOUR_OF_DAY) +"\t"+ entry[1] +"\t"+ entry[2]);
		}
		in.close();
	}
	public static void main( String[] args ) {
		String[] argb = new String[3];
		argb[0] = "C://Documents and Settings/akm/My Documents/EW9309.a77";
		argb[1] = "EW9309";
		argb[2] = "77";
		/*if( args.length != 3 ) {
			System.out.println( "usage: java haxby.nav.MGD77 file name format" );
			System.exit(0);
		}*/
		MGD77 mgd = null;
		String name = argb[1];
		String id = "01";
		String cruise = name;
		try {
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(
					new FileOutputStream("C://Documents and Settings/akm/My Documents/EW9309/Control Points/" + argb[1] +".mbcontrol")));
			mgd = new MGD77( argb[0], argb[1] );
			int nPer360 = 327680;
			Projection proj = ProjectionFactory.getMercator(nPer360);
			int fmt = Integer.parseInt( argb[2] );
			int nraw = mgd.nav.getSize();
			System.out.println( mgd.nav.getSize() + " points" );
			mgd.nav.computeControlPoints( proj, (double)nPer360, 20. );
			Vector cpts = mgd.nav.getControlPoints();
			if(cpts.size() == 0) throw new IOException( "no controlPoints" );;
			int start = ((ControlPoint)((Vector)cpts.get(0)).get(0)).time;
			Vector seg = (Vector)cpts.get(cpts.size()-1);
			int end = ((ControlPoint)seg.get(seg.size()-1)).time;
			out.writeUTF( id+"\t"+name );
			out.writeInt(cpts.size());
			out.writeInt(start);
			out.writeInt(end);
			out.writeInt(fmt);
			int ncpt = 0;
			double[] wesn = new double[] {0., 0., 0., 0.};
			double lon0 = 0.;
			for( int iseg=0 ; iseg<cpts.size() ; iseg++ ) {
				seg = (Vector) cpts.get(iseg);
				out.writeInt( seg.size() );
				ncpt = 0;
				for( int i=0 ; i<seg.size() ; i++) {
					ControlPoint cpt = (ControlPoint)seg.get(i);
					out.writeInt((int)Math.rint(1.e06*cpt.x));
					out.writeInt((int)Math.rint(1.e06*cpt.y));
					out.writeInt(cpt.time);
					if( ncpt==0 ) {
						wesn[0] = wesn[1] = cpt.x;
						lon0 = cpt.x;
						wesn[2] = wesn[3] = cpt.y;
					} else {
						while( cpt.x>lon0+180.) cpt.x -= 360.;
						while( cpt.x<lon0-180.) cpt.x += 360.;
						if( cpt.x<wesn[0] ) wesn[0]=cpt.x;
						else if( cpt.x>wesn[1] ) wesn[1]=cpt.x;
						lon0 = (wesn[0]+wesn[1])/2.;
						if( cpt.y<wesn[2] ) wesn[2]=cpt.y;
						else if( cpt.y>wesn[3] ) wesn[3]=cpt.y;
					}
					ncpt++;
				}
				System.out.println(cruise +"\t"+ id +"\t"+ fmt +"\t"+ nraw +"\t"+ ncpt
					+"\n\t"+ wesn[0] +"\t"+ wesn[1]+"\t"+ wesn[2]+"\t"+ wesn[3]);
			}
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(0);
		}
	}
}
/*
3NBP9909     09912200000000-4360780 172714971999999999999991999999999999999999999999999999999999999999999999999999999999
012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
*/

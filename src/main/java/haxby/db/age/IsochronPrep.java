package haxby.db.age;

import java.io.*;
import java.util.*;

public class IsochronPrep {
	public static void main( String[] args ) {
		int kount=0;
		try {
			float[] ages = new float[200];
			for( int k=0 ; k<200 ; k++ ) ages[k]=-1f;
			BufferedReader in = new BufferedReader(
				new FileReader( "/scratch/ridgembs/bill/global/plates/isochrons.dat" ));
			DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(
				new FileOutputStream( "/scratch/ridgembs/bill/Age/isochrons" )));
			String s = in.readLine();
			s = in.readLine();
			StringTokenizer st = new StringTokenizer(s);
			while( true ) {
				kount += 2;
				short plate = Short.parseShort( st.nextToken() );
				float age = Float.parseFloat( st.nextToken() );
				st.nextToken();
				boolean m = st.nextToken().equals("IM");
				int anom = Integer.parseInt( st.nextToken() );
				if( m ) anom+=100;

				if( ages[anom]==-1f ) {
					ages[anom] = age;
				} else {
					if( age != ages[anom] ) {
						System.out.println( "age disparity for anomaly "
							+ anom +":\t"+ age +" vs "+ ages[anom]
							+"\t"+ kount);
					}
				}
				short conjugate = Short.parseShort( st.nextToken() );
				Isochron isochron = new Isochron( plate, conjugate,
							anom );
				while( (s=in.readLine()) != null ) {
					kount++;
					st = new StringTokenizer(s);
					double lat = Double.parseDouble( st.nextToken() );
					if( lat>90. ) {
						if( (s=in.readLine())==null ) break;
						st = new StringTokenizer( in.readLine() );
						break;
					}
					double lon = Double.parseDouble( st.nextToken() );
					boolean connect = st.nextToken().equals("2");
					isochron.add( (float)lon, (float)lat, connect );
				}
				if( s==null ) break;
				out.writeShort( (short)anom );
				out.writeShort(plate);
				out.writeShort(conjugate);
				Vector segs = isochron.getSegs();
				out.writeShort( (short)segs.size() );
//	System.out.println( anom +"\t"+ segs.size() );
				for(int k=0 ; k<segs.size() ; k++) {
					Vector seg = (Vector)segs.get(k);
//	System.out.println( "\t"+ seg.size() +"\t"+ kount );
	if( segs.size()>1 ) System.out.println( segs.size() +"\t"+ seg.size() );
					out.writeShort( (short)seg.size() );
					for( int j=0 ; j<seg.size() ; j++) {
						float[] lonlat = (float[])seg.get(j);
						out.writeFloat(lonlat[0]);
						out.writeFloat(lonlat[1]);
					}
				}
			}
			out.close();
			out = new DataOutputStream(
				new BufferedOutputStream(
				new FileOutputStream( "/scratch/ridgembs/bill/Age/timescale" )));
			for(int k=0 ; k<200 ; k++) {
				if( ages[k]>=0 ) {
					out.writeInt(k);
					out.writeFloat(ages[k]);
					System.out.println( k +"\t"+ ages[k] );
				}
			}
			out.close();
		} catch( Exception ex ) {
			ex.printStackTrace();
		}
		System.out.println( kount );
	}
}
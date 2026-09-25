package haxby.db.radar;

import haxby.nav.*;
import haxby.proj.*;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.sql.*;

public class RControl {
	public static void main(String[] args) {
		Projection proj = new PolarStereo( new Point(0, 0),
				180., 50., -71.,
				PolarStereo.SOUTH, PolarStereo.WGS84);
		try {
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(
					new FileOutputStream("radar_control")));
			BufferedReader fileIn = new BufferedReader(
					new FileReader("/scratch/shadow/users/mstuding/vos-statics/SEGY/lvs_shots_nav.dat"));
			String s;
			String cruise = "Vostok";
			String lineID = "";
			Nav nav = null;
			int nraw = 0;
			int time = 0;
			double lon = 0.;
			double lat = 0.;
			int time1 = 0;
			double lon1 = 0.;
			double lat1 = 0.;
			while( (s=fileIn.readLine()) != null ) {
				StringTokenizer st = new StringTokenizer(s);
				String id = st.nextToken();
				if( !id.equals(lineID) ) {
					if( nav!=null && nraw>1) {
						System.out.println( lineID +"\t"+ nraw +"\t"+
							lon1 +"\t"+ lat1 +"\t"+ lon +"\t"+ lat);
					//	writeNav ( nav, cruise, lineID, proj,out );
						out.writeUTF( lineID+"\t"+cruise );
						out.writeInt( 1 );
						out.writeInt(0);
						out.writeInt( time );
						out.writeInt(0);
						out.writeInt( 2 );
						out.writeInt((int)Math.rint(1.e06*lon1));
						out.writeInt((int)Math.rint(1.e06*lat1));
						out.writeInt( time1 );
						out.writeInt((int)Math.rint(1.e06*lon));
						out.writeInt((int)Math.rint(1.e06*lat));
						out.writeInt( time );
					}
					lineID = id;
					nav = new Nav( id );
					nraw = 1;
					time1 = Integer.parseInt( st.nextToken() );
					lon1 = Double.parseDouble( st.nextToken() );
					lat1 = Double.parseDouble( st.nextToken() );
					nav.addPoint(time1, lon1, lat1);
					continue;
				}
				time = Integer.parseInt( st.nextToken() );
				lon = Double.parseDouble( st.nextToken() );
				lat = Double.parseDouble( st.nextToken() );
				nav.addPoint(time, lon, lat);
				nraw++;
			}
			out.writeUTF( lineID+"\t"+cruise );
			out.writeInt(1);
			out.writeInt( 0 );
			out.writeInt( time );
			out.writeInt(0);
			out.writeInt( 2 );
			out.writeInt((int)Math.rint(1.e06*lon1));
			out.writeInt((int)Math.rint(1.e06*lat1));
			out.writeInt( time1 );
			out.writeInt((int)Math.rint(1.e06*lon));
			out.writeInt((int)Math.rint(1.e06*lat));
			out.writeInt( time );
		//	if( nav!=null && nraw>1) writeNav ( nav, cruise, lineID, proj,out);
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	static void writeNav( Nav nav, String cruise, String id, Projection proj, 
				DataOutputStream out )  throws IOException {
		nav.computeControlPoints( proj, -1., 20. );
		Vector cpts = nav.getControlPoints();
		if(cpts.size() <= 1) return;
		Vector seg = (Vector)cpts.get(cpts.size()-1);
		out.writeUTF( id+"\t"+cruise );
		out.writeInt(cpts.size());
		int ncpt = 0;
		double[] wesn = new double[] {0., 0., 0., 0.};
		double lon0 = 0.;
		for( int iseg=0 ; iseg<cpts.size() ; iseg++ ) {
			seg = (Vector) cpts.get(iseg);
			out.writeInt( seg.size() );
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
		}
		System.out.println(cruise +"\t"+ id +"\t"+ ncpt
			+"\n\t"+ wesn[0] +"\t"+ wesn[1]+"\t"+ wesn[2]+"\t"+ wesn[3]);
	}
}

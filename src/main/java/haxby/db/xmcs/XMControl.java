package haxby.db.xmcs;

import haxby.nav.*;
import haxby.proj.*;

import java.io.*;
import java.util.*;

public class XMControl {

	/**
	 * java haxby.db.mcs.XMControl cruiseID navDir
	 * Expects nav files to be in the format 
		LineID	shotPoint	Lat Lon
		Any information can follow the Lon
	 * Creates an mcs_control for cruiseID based on *nav in navDir
	 * Outputs WESN bounds to StandardIO
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("ussage java haxby.db.mcs.XMControl cruiseID navDir");
			System.exit(0);
		}

		Projection proj = ProjectionFactory.getMercator( 1024*512 );
		try {
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(
					new FileOutputStream("mcs_control")));
			File dir = new File(args[1]);
			File[] files = dir.listFiles( new FileFilter() {
					public boolean accept( File file ) {
						return file.getName().endsWith(".nav");
					}
				});
			String s;
			String cruise = args[0];
			String lineID = "";
			Nav nav = null;
			int time = 0;
			double lon = 0.;
			double lat = 0.;
			int time1 = 0;
			double lon0 = Double.MAX_VALUE;
			double lat0 = Double.MAX_VALUE;
			double lon1 = -Double.MAX_VALUE;
			double lat1 = -Double.MAX_VALUE;
			for( int k=0 ; k<files.length ; k++ ) {
				BufferedReader in = new BufferedReader(
						new FileReader( files[k] ));
				int nraw = 0;
				nav = null;
				while( (s=in.readLine()) != null ) {
					StringTokenizer st  = new StringTokenizer( s );
					lineID = st.nextToken();
					if (nav == null)
						nav = new Nav(lineID);
					int shot = Integer.parseInt( st.nextToken() );
					lat = Double.parseDouble( st.nextToken() );
					lon = Double.parseDouble( st.nextToken() );
					nav.addPoint( shot, lon, lat );
					nraw++;
				}
	System.out.println( cruise +"\t"+ lineID +"\t"+ nraw);
				if( nraw>1 ) {
					double[] wesn = writeNav( nav, cruise, lineID, proj, out );
					if (wesn==null) continue;
					lon0 = Math.min(wesn[0], lon0);
					lon1 = Math.max(wesn[1], lon1);
					lat0 = Math.min(wesn[2], lat0);
					lat1 = Math.max(wesn[3], lat1);
				}
			}
			out.close();

			System.out.println("West\tEast\tSouth\tNorth");
			System.out.println(lon0+"\t"+lon1+"\t"+lat0+"\t"+lat1);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	static double[] writeNav( Nav nav, String cruise, String id, Projection proj, 
				DataOutputStream out )  throws IOException {
	//	nav.computeControlPoints( proj, 1024*512, 20. );
		nav.computeControlPoints( proj, -1, 20. );
		Vector cpts = nav.getControlPoints();
//	System.out.println( cpts.size() +" points");
		if(cpts.size() == 0) return null;

		// Condenses control points to a single line
		int npt = 0;
		for( int iseg=0 ; iseg<cpts.size() ; iseg++ ) 
			npt += ((Vector) cpts.get(iseg)).size();

		Vector seg = (Vector)cpts.get(cpts.size()-1);
		out.writeUTF( id+"\t"+cruise );
		out.writeInt( seg.size() );
		out.writeInt(npt);

		int ncpt = 0;
		double[] wesn = new double[] {0., 0., 0., 0.};
		double lon0 = 0.;
		for( int iseg=0 ; iseg<cpts.size() ; iseg++ ) {
			seg = (Vector) cpts.get(iseg);
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
//		System.out.println(cruise +"\t"+ id +"\t"+ ncpt
//				+"\n\t"+ wesn[0] +"\t"+ wesn[1]+"\t"+ wesn[2]+"\t"+ wesn[3]);
		return wesn;
	}
}
package haxby.db.scs;

import haxby.nav.*;
import haxby.proj.*;

import java.io.*;
import java.util.*;

public class GMTControl {
	public static void main(String[] args) {
		if( args.length != 2 && args.length != 1 ) {
			System.out.println( "usage: java XMBControl path [nPer360]");
			System.exit(0);
		}
		int nPer360 = 80000;
		if( args.length == 2 ) {
			nPer360 = Integer.parseInt(args[1]);
		}
		String dir = (new File( args[0] )).getAbsolutePath();
		if( args[0].equals(".") ) {
			dir = System.getProperty("user.home");
		}
		File[] gmtFiles = (new File(dir)).listFiles( new FileFilter() {
				public boolean accept(File name) {
					return name.getName().endsWith(".gmt");
				}
			});
		if( gmtFiles.length != 1 ) {
			System.out.println( "should be one and only one .gmt file in directory" );
			System.exit(0);
		}

		String name = gmtFiles[0].getName();
		name = name.substring( 0, name.indexOf(".gmt") );
		System.out.println( "cruise "+name);

		Projection proj = ProjectionFactory.getMercator(nPer360);
		try {
			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(
					new FileOutputStream(new File(dir,"control.nav"))));
			BufferedReader in = new BufferedReader(
						new FileReader(gmtFiles[0]));
			String s;
			int nraw = 0;
			Nav nav = new Nav( name );
			while( (s=in.readLine()) != null ) {
				StringTokenizer st = new StringTokenizer(s);
				int time = (int)Double.parseDouble( st.nextToken() );
				double lon = Double.parseDouble( st.nextToken() );
				double lat = Double.parseDouble( st.nextToken() );
				if( lon==0. && lat==0. ) continue;
				nav.addPoint(time, lon, lat);
				nraw++;
			}
			in.close();
			if(nraw<=1) {
				System.out.println( "no control points" );
				System.exit(0);
			}
			nav.computeControlPoints( proj, (double)nPer360, 20. );
			Vector cpts = nav.getControlPoints();
			if(cpts.size() == 0) {
				System.out.println( "no control points" );
				System.exit(0);
			}
			Vector seg = (Vector)cpts.get(cpts.size()-1);
			out.writeUTF( name );
			out.writeInt(cpts.size());
			int ncpt = 0;
			double lon0 = 0.;
			for( int iseg=0 ; iseg<cpts.size() ; iseg++ ) {
				seg = (Vector) cpts.get(iseg);
				out.writeInt( seg.size() );
				for( int i=0 ; i<seg.size() ; i++) {
					ControlPoint cpt = (ControlPoint)seg.get(i);
					out.writeInt((int)Math.rint(1.e06*cpt.x));
					out.writeInt((int)Math.rint(1.e06*cpt.y));
					out.writeInt(cpt.time);
					ncpt++;
				}
			}
			System.out.println(name +"\t"+ nraw +"\t"+ ncpt);
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
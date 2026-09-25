package org.geomapapp.db.alvin;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import org.geomapapp.geom.IdentityProjection;
import org.geomapapp.geom.Navigation;
import org.geomapapp.geom.UTM;
import org.geomapapp.gis.shape.ESRIShape;
import org.geomapapp.gis.shape.ESRIShapefile;

import haxby.util.PathUtil;

public class AlvinNav {
	Navigation nav;
	String date="";
	String name;
	
//	***** GMA 1.5.2: Read depth, heading and altitude into these variables from the nav file, hashtable to do 
//	lookup of platform names
	String depth;
	String heading;
	String altitude;
	Vector infoCruiseIDs;
	Hashtable fileCruiseIDToInfoCruiseID;
	Hashtable diveToPlatform;
	Hashtable diveToNavType;
//	***** GMA 1.5.2
	
	public AlvinNav() {
		diveToPlatform = new Hashtable();
		diveToNavType = new Hashtable();
		infoCruiseIDs = new Vector();
		fileCruiseIDToInfoCruiseID = new Hashtable();
	}
	public AlvinNav( String filePath ) throws IOException {
		diveToPlatform = new Hashtable();
		diveToNavType = new Hashtable();
		open( filePath );
	}
	public void process( String rootPath,String inputSiteID ) throws IOException {
		File root = new File(rootPath);
		// String root = new File(inputSiteID).getPath();
		// File root = new File(rootPath);
		File[] legs = root.listFiles( new FileFilter() {
			public boolean accept(File f) {
				if( !f.isDirectory() )return false;
				File dir = new File(f, "dives");
				return dir.exists();
			}
		});
		FileFilter ff = new FileFilter() {
			public boolean accept(File f) {
				if( !f.isDirectory() )return false;
				File dir = new File(f, "nav");
				return dir.exists();
			}
		};
		FileFilter ff1 = new FileFilter() {
			public boolean accept(File f) {
				return ( f.getName().indexOf("1sec_allnav") + f.getName().indexOf("1Hz") )>0;
			}
		};
		
		String platform = null;
		String navtype = null;
		File info = new File( System.getProperty("user.dir") + File.separator + 
				"Photos"  + File.separator + inputSiteID + ".info" );
		//File info = new File( rootPath + File.separator + inputSiteID + ".info" );
		if ( info.exists() ) {
			BufferedReader in = new BufferedReader( new FileReader(info) );
			String s = null;
			while ( ( s = in.readLine() ) != null ) {
				String[] result = s.split("\\s");
				fileCruiseIDToInfoCruiseID.put( result[0].toUpperCase(), result[0]);
				String diveID = result[1];
				platform = result[4];
				navtype = result[5];
				diveToPlatform.put(diveID, platform);
				diveToNavType.put(diveID, navtype);
			}
		}
		
//		Will take different platform names eventually		
		makeLinkFile(rootPath,inputSiteID,platform);
		
		int nLeg=0;
		Vector names = new Vector();
		Vector classes = new Vector();
		names.add("LegID");
		classes.add(String.class);
		names.add("Platform");
		classes.add(String.class);
		names.add("Dive");
		classes.add(String.class);
		names.add("NavType");
		classes.add(String.class);
		names.add("Date");
		classes.add(String.class);
		ESRIShapefile shapes = new ESRIShapefile( platform + " Dives", 23, names, classes );
		IdentityProjection proj = new IdentityProjection();

		Vector entry;
		for( int k=0 ; k<legs.length ; k++) {
			String legName = (String)fileCruiseIDToInfoCruiseID.get(legs[k].getName().toUpperCase());
	System.out.println("AlvinNav Leg name : " +legName);
			if ( legName == null || legs.equals("") ) {
				legName = legs[k].getName();
			}
			File dir = new File(legs[k], "dives");
			File[] dives = dir.listFiles( ff );
			int nDive=0;
			for( int i=0 ; i<dives.length ; i++) {
				name = legName+", "+dives[i].getName();
				dir = new File(dives[i], "nav");
				File[] file = dir.listFiles(ff1);
				if( file==null || file.length==0 )continue;
				if( file.length>1 ) {
					System.out.println( "Warning: More than 1 nav file present in "+ dir.getPath());
					System.out.println( "\tprocessing first entry: "+ file[0].getName());
				}
				open( file[0].getPath() );
				ESRIShape shape = nav.getShape();
				entry = new Vector();
				if ( ((String)diveToPlatform.get(dives[i].getName())).toLowerCase().indexOf("jason") != -1 ) {
					entry.add( legName.toLowerCase() );
				}
				else {
					entry.add( legName );
				}
				
				entry.add( ((String)diveToPlatform.get(dives[i].getName())).toLowerCase() );
				entry.add( dives[i].getName() );
				entry.add( (String)diveToNavType.get(dives[i].getName()) );
				entry.add( date );
				shapes.addShape( shape, entry );
			}
		}
		shapes.forward( proj, 360.);
		
//		GMA 1.5.2: Set the name of the .shp file to the name of the site
//		shapes.writeShapes(new File(root, "DiveNav"), proj);
		String siteID = null;
		siteID = inputSiteID.substring( inputSiteID.indexOf( File.separator ) + 1 );
		if ( siteID != null ) {
			
//			TODO: Find out the precision of the coordinates being written to the generated shapefiles in the Southern Hemisphere
			//shapes.writeShapes(new File(root, siteID), proj);
			shapes.writeShapes(root, proj);
		}
		else {
			//shapes.writeShapes(new File(root, "DiveNav"), proj);
			shapes.writeShapes(new File(root, "DiveNav"), proj);
		}
		
	}
				
	public void open( String filePath ) throws IOException {
		BufferedReader in = filePath.endsWith(".gz") 
		? new BufferedReader(
			new InputStreamReader(
			new GZIPInputStream(
			new FileInputStream( filePath ))))
		: new BufferedReader(
			new FileReader( filePath ));
		read(in);
	}
	void read( BufferedReader in ) throws IOException {
		String s = in.readLine();
		nav = new Navigation();
		Calendar cal = Calendar.getInstance( TimeZone.getTimeZone("GMT") );
		int zone = -1;
		int north = 1;
		while( (s=in.readLine())!=null ) {
			
//			GMA 1.5.2: Read in nav files that are both whitespace delimited and commma delimited
//			StringTokenizer st = new StringTokenizer(s);
			StringTokenizer st = new StringTokenizer(s, " \t,");
			
			StringTokenizer st1 = new StringTokenizer( st.nextToken().trim(), "-/");
			StringTokenizer st2 = new StringTokenizer( st.nextToken().trim(), ":");
			cal.set( cal.YEAR, Integer.parseInt(st1.nextToken()) );
			
			cal.set( cal.MONTH, Integer.parseInt(st1.nextToken()) );
			cal.set( cal.DATE, Integer.parseInt(st1.nextToken()) );
			cal.set( cal.HOUR_OF_DAY, Integer.parseInt(st2.nextToken()) );
			cal.set( cal.MINUTE, Integer.parseInt(st2.nextToken()) );
			
//			GMA 1.5.2: Read in seconds as double
//			cal.set( cal.SECOND, Integer.parseInt(st2.nextToken()) );
//			cal.set( cal.MILLISECOND, 0 );
			String seconds = st2.nextToken();
			if ( seconds.indexOf(".") != -1 ) {
				cal.set( cal.SECOND, Integer.parseInt( seconds.substring( 0, seconds.indexOf(".") - 1 ) ) );
				cal.set( cal.MILLISECOND, Integer.parseInt( seconds.substring( seconds.indexOf(".") + 1 ) ) );
			}
			else {
				cal.set( cal.SECOND, Integer.parseInt(seconds) );
				cal.set( cal.MILLISECOND, 0 );
			}
			
			double t = cal.getTimeInMillis()*.001;
			
			st.nextToken();
			st.nextToken();
			try {
				double y = Double.parseDouble( st.nextToken().trim() );
				double x = Double.parseDouble( st.nextToken().trim() );

//				***** GMA 1.5.2: Get depth, heading and altitude from nav file
//				for( int k=0 ; k<6 ; k++) st.nextToken();
//				double z = Double.parseDouble( st.nextToken().trim() );
				st.nextToken();
				st.nextToken();
				depth = st.nextToken();
				st.nextToken();
				heading = st.nextToken();
				st.nextToken();
				altitude = st.nextToken();
				double z = Double.parseDouble( altitude.trim() );
//				***** GMA 1.5.2
				
				if( z==0. )continue;
				if( Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) )continue;
				nav.addPoint( x, y, t );
				if( zone==-1 ) {
					zone = 1 + (int)Math.rint( (x+177.)/6 );
					if( y<0. )north=2;
					UTM utm = new UTM( zone, 2, north );
					Point2D p = utm.getMapXY( x, y);
					System.out.println( name +"\t"+ x +"\t"+ y +"\t"+ p.getX() +"\t"+ p.getY());
					date = cal.get(cal.YEAR) +"-"+ (cal.get(cal.MONTH)+1) +"-"+ cal.get(cal.DATE);
				}
			} catch( NumberFormatException e) {
			}
		}
		UTM utm = new UTM( zone, 2, north );
		double[][][] pts = nav.computeControl(utm, -1., 10., 1. );
		nav.inverse(utm);
		int npt = 0;
		for( int k=0 ; k<pts.length ; k++) {
			npt += pts[k].length;
	//		for( int i=0 ; i<pts[k].length; i++) System.out.println(k +"\t"+ pts[k][i][0] +"\t"+ pts[k][i][1]);
		}
	//	System.out.println( "UTM Zone "+ zone );
	//	System.out.println( nav.getNav().length +" points");
	//	System.out.println( pts.length +" segments, "+ npt +" control points");
	//	PrintStream out = new PrintStream(
	//			new FileOutputStream("control.xyt"));
	//	out.println( "seg\tlon\tlat\ttime");
	//	for( int k=0 ; k<pts.length ; k++) {
	//		for( int i=0 ; i<pts[k].length; i++) out.println(k +"\t"+ pts[k][i][0] +"\t"+ pts[k][i][1] +"\t"+ pts[k][i][2]);
	//	} 
	//	out.close();

	//	out = new PrintStream(
	//			new FileOutputStream("nav.xyt"));
	//	out.println( "lon\tlat\ttime");
	//	double[][] pt = nav.getNav();
	//	for( int k=0 ; k<pt.length ; k++) {
	//		int t = (int)Math.rint(pt[k][2]);
	//		if( t%60 != 0)continue;
	//		out.println(pt[k][0] +"\t"+ pt[k][1] +"\t"+ pt[k][2]);
	//	} 
	//	out.close();
	}
	
	public void makeLinkFile(String rootPath, String inputSiteID, String platformID) throws FileNotFoundException {
		PrintStream out = new PrintStream(new FileOutputStream(rootPath + File.separator + inputSiteID +  ".link"));
		out.println("<data>");
		out.println("<name>" + inputSiteID + "-Photos</name>");
		out.println("<type>shape</type>");
		
//		out.println("<info>http://4dgeo.whoi.edu/om-bin/eic2html.pl?f=/webdata/OM/${2}/MDF/OM.${2}.${1}.txt&t=/webdata/OM/${2}/HTMLTemplate/${2}.v1.TEMPLATE</info>");
//		out.println("<info>http://4dgeo.whoi.edu/om-bin/eic2html.pl?f=/webdata/OM/${2}/MDF/OM.${2}.${1}.txt&t=/webdata/OM/${2}/HTMLTemplate/" + platformID.toLowerCase() +".v1.TEMPLATE</info>");
		out.println("<info>http://4dgeo.whoi.edu/om-bin/view_cruise?vehicle=${2}&cruise=${1}</info>");
		
		out.println("<url>" +PathUtil.getPath("PUBLIC_HOME_PATH")+ "GMA/Layers/Photos/NDSF/" + inputSiteID + "/${1}/${2}-${3}/${2}-${3}.shp</url>");
		out.println("<description>");
		out.println("Navigation for ${2} Dive ${3}, Leg ${1}");
		out.println("</description>");
		out.println("</data>");
		out.flush();
		out.close();
	}
	
	public static void main(String[] args) {
		try {
			AlvinNav an = new AlvinNav();
			an.process(args[0], args[0]);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}

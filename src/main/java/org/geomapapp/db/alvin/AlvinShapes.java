package org.geomapapp.db.alvin;

import org.geomapapp.geom.IdentityProjection;
import org.geomapapp.geom.Navigation;
import org.geomapapp.geom.UTM;
import org.geomapapp.gis.shape.*;

import java.awt.geom.Point2D;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class AlvinShapes {
	ESRIShapefile alvin;
	
//	***** GMA 1.5.2: Read in variables from the nav file
	Navigation nav;
	String date="";
	String name;
	String depth;
	String heading;
	String altitude;
	String CSVTemp;
	String DVLTemp;
	public Hashtable timeToDepth;
	public Hashtable timeToHeading;
	public Hashtable timeToAltitude;
	public Hashtable timeToCSVTemp;
	public Hashtable timeToDVLTemp;
	public Hashtable diveToPlatform;
//	***** GMA 1.5.2
	
	public AlvinShapes() {
		timeToDepth = new Hashtable();
		timeToHeading = new Hashtable();
		timeToAltitude = new Hashtable();
		timeToCSVTemp = new Hashtable();
		timeToDVLTemp = new Hashtable();
		diveToPlatform = new Hashtable();
	}
	
	public AlvinShapes( String rootPath ) throws IOException {
		timeToDepth = new Hashtable();
		timeToHeading = new Hashtable();
		timeToAltitude = new Hashtable();
		timeToCSVTemp = new Hashtable();
		timeToDVLTemp = new Hashtable();
		diveToPlatform = new Hashtable();
		File info = new File( rootPath + ".info" );
		if ( info.exists() ) {
			BufferedReader in = new BufferedReader( new FileReader(info) );
			String s = null;
			while ( ( s = in.readLine() ) != null ) {
				String[] result = s.split("\\s");
				String diveID = result[1];
				String platform = result[4];
				diveToPlatform.put(diveID, platform);
			}
		}
	}
	
//	***** GMA 1.5.2: Get these functions so that AlvinShapes can read variables in from the nav files
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
		String dateKey = null;
		String s = in.readLine();
		Calendar cal = Calendar.getInstance( TimeZone.getTimeZone("GMT") );
		String year_string = "";
		String month_string = "";
		String day_string = "";
		String hour_string = "";
		String minute_string = "";
		String second_string = "";
		String millisecond_string = "";
		while( (s=in.readLine())!=null ) {
			StringTokenizer st = new StringTokenizer(s, " \t,");
			StringTokenizer st1 = new StringTokenizer( st.nextToken().trim(), "-/");
			StringTokenizer st2 = new StringTokenizer( st.nextToken().trim(), ":");
//			cal.set( cal.YEAR, Integer.parseInt(st1.nextToken()) );
//			cal.set( cal.MONTH, Integer.parseInt(st1.nextToken()) );
//			cal.set( cal.DATE, Integer.parseInt(st1.nextToken()) );
//			cal.set( cal.HOUR_OF_DAY, Integer.parseInt(st2.nextToken()) );
//			cal.set( cal.MINUTE, Integer.parseInt(st2.nextToken()) );
			year_string = st1.nextToken();
			month_string = st1.nextToken();
			day_string = st1.nextToken();
			hour_string = st2.nextToken();
			minute_string = st2.nextToken();
			String seconds = st2.nextToken();
			if ( seconds.indexOf(".") != -1 ) {
//				cal.set( cal.SECOND, Integer.parseInt( seconds.substring( 0, seconds.indexOf(".") ) ) );
//				cal.set( cal.MILLISECOND, Integer.parseInt( seconds.substring( seconds.indexOf(".") + 1 ) ) );
				second_string = seconds.substring( 0, seconds.indexOf(".") );
				millisecond_string = seconds.substring( seconds.indexOf(".") + 1 );
			}
			else {
//				cal.set( cal.SECOND, Integer.parseInt(seconds) );
//				cal.set( cal.MILLISECOND, 0 );
				second_string = seconds;
				millisecond_string = "00";
			}
//			dateKey = Integer.toString(cal.get(cal.YEAR)) + Integer.toString(cal.get(cal.MONTH)) + Integer.toString(cal.get(cal.DATE)) + Integer.toString(cal.get(cal.HOUR_OF_DAY)) + Integer.toString(cal.get(cal.MINUTE)) + Integer.toString(cal.get(cal.SECOND));
			dateKey = year_string + month_string + day_string + "_" + hour_string + minute_string + second_string;
			double t = cal.getTimeInMillis()*.001;
			st.nextToken();
			st.nextToken();
			try {
				double y = Double.parseDouble( st.nextToken().trim() );
				double x = Double.parseDouble( st.nextToken().trim() );

//				***** GMA 1.5.2: Get depth, heading and altitude from nav file
				st.nextToken();
				st.nextToken();
				depth = st.nextToken();
				timeToDepth.put(dateKey, depth);
				st.nextToken();
				heading = st.nextToken();
				timeToHeading.put(dateKey, heading);
				st.nextToken();
				altitude = st.nextToken();
				timeToAltitude.put(dateKey, altitude);
				CSVTemp = st.nextToken();
				timeToCSVTemp.put(dateKey, CSVTemp);
				DVLTemp = st.nextToken();
				timeToDVLTemp.put(dateKey, DVLTemp); 
//				***** GMA 1.5.2
				
			} catch( NumberFormatException e) {
			}
		}
		in.close();
	}
//	***** GMA 1.5.2
	
	public static void main(String[] args) {
		String root = "C://Documents and Settings/akm/workspace2/GeoMapApp/EPR_9_30N/";
		AlvinShapes tempAlvinShapes = new AlvinShapes();
		try {
			
//			***** GMA 1.5.2: Use site name as file and folder id instead of generic "DiveNav"
//			ESRIShapefile alvin = new ESRIShapefile(root, "DiveNav");
			ESRIShapefile alvin = new ESRIShapefile(root, args[0]);
//			***** GMA 1.5.2
			
			Vector shapes = alvin.getShapes();
			DBFFile dbf = alvin.getDBFFile();
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
			cal.setTimeInMillis(0L);
			org.geomapapp.geom.IdentityProjection proj = new org.geomapapp.geom.IdentityProjection();
			for( int k=0 ; k<shapes.size() ; k++) {
				ESRIPolyLineM shape = (ESRIPolyLineM)shapes.get(k);
				String cruise = dbf.getValueAt(k,0).toString();
				String dive = (String)tempAlvinShapes.diveToPlatform.get(dbf.getValueAt(k,2).toString()) + "-D"+ dbf.getValueAt(k,2).toString();
				File dir = new File( root, cruise);
				
//				***** GMA 1.5.2: Read contents of nav file so that they can be added to the
//				info for the shape file
				File navDir = new File(dir + File.separator + "dives" + File.separator + dbf.getValueAt(k,2).toString() + File.separator + "nav" );
				File[] navFile = navDir.listFiles();
				for ( int m = 0; m < navFile.length; m++ ) {
					tempAlvinShapes.open(navFile[m].toString());
				}
//				***** GMA 1.5.2
				
				dir = new File(dir, dive);
				if( !dir.exists() )dir.mkdirs();
				File images = new File( dir, "images");
				if( !images.exists() ) {
					File xml = new File(dir,dive+".link");
					PrintStream out = new PrintStream(
						new FileOutputStream(xml));
					out.println("<info>");
					out.println(" <message>No photos for "+dive+"</message>" );
					out.println("</info>");
					out.close();
					continue;
				}
				BufferedReader in = new BufferedReader(
					new FileReader( images ));
				String url1 = in.readLine();
				String url2 = in.readLine();
				
//				***** GMA 1.5.2: Expand names to include depth, heading and altitude
//				Vector names = new Vector(1);
				Vector names = new Vector(6);
//				***** GMA 1.5.2
				
				names.add("time");
				
//				***** GMA 1.5.2: Add depth, heading and altitude to displayed attributes
				names.add("depth");
				names.add("heading");
				names.add("altitude");
				names.add("ambient T");
				names.add("DVL temp");
//				***** GMA 1.5.2
				
//				***** GMA 1.5.2: Expand names to include depth, heading and altitude
//				Vector classes = new Vector(1);
//				classes.add(String.class);
				Vector classes = new Vector(6);
				classes.add(String.class);
				classes.add(String.class);
				classes.add(String.class);
				classes.add(String.class);
				classes.add(String.class);
				classes.add(String.class);
//				***** GMA 1.5.2
				
				
				ESRIShapefile shp = new ESRIShapefile( dive, 1, names, classes);
				String s;
				while( (s=in.readLine())!=null) {
					String date = s.substring( 0, s.indexOf(".") );
					StringTokenizer st = new StringTokenizer(s, "_.");
					s = st.nextToken();
					cal.set( Calendar.YEAR, Integer.parseInt( s.substring(0,4)) );
					cal.set( Calendar.MONTH, Integer.parseInt( s.substring(4,6))-1 );
					cal.set( Calendar.DATE, Integer.parseInt( s.substring(6,8)) );
					s = st.nextToken();
					cal.set( Calendar.HOUR_OF_DAY, Integer.parseInt( s.substring(0,2)) );
					cal.set( Calendar.MINUTE, Integer.parseInt( s.substring(2,4)) );
					cal.set( Calendar.SECOND, Integer.parseInt( s.substring(4,6)) );
					double time = .001*cal.getTimeInMillis();
					double[] xy = shape.xyAt(time);
					if( xy==null )continue;
					ESRIPoint p = new ESRIPoint(xy[0], xy[1]);
					
//					***** GMA 1.5.2: Expand record to include depth, heading and altitude
//					Vector record = new Vector(1);
//					record.add(date);
					Vector record = new Vector(4);
					record.add(date);
					if ( tempAlvinShapes.timeToDepth.containsKey( Double.toString(time) ) ) {
						record.add( tempAlvinShapes.timeToDepth.get( Double.toString(time) ).toString() );
					}
					else {
						record.add("NaN");
					}
					if ( tempAlvinShapes.timeToHeading.containsKey( Double.toString(time) ) ) {
						record.add( tempAlvinShapes.timeToHeading.get( Double.toString(time) ).toString() );
					}
					else {
						record.add("NaN");
					}
					if ( tempAlvinShapes.timeToAltitude.containsKey( Double.toString(time) ) ) {
						record.add( tempAlvinShapes.timeToAltitude.get( Double.toString(time) ).toString() );
					}
					else {
						record.add("NaN");
					}
					if ( tempAlvinShapes.timeToCSVTemp.containsKey( Double.toString(time) ) ) {
						record.add( tempAlvinShapes.timeToCSVTemp.get( Double.toString(time) ).toString() );
					}
					else {
						record.add("NaN");
					}
					if ( tempAlvinShapes.timeToDVLTemp.containsKey( Double.toString(time) ) ) {
						record.add( tempAlvinShapes.timeToDVLTemp.get( Double.toString(time) ).toString() );
					}
					else {
						record.add("NaN");
					}
//					***** GMA 1.5.2					
					
					
					shp.addShape( p, record );
				}
				in.close();
				if( shp.size()==0 )continue;
				shp.forward( proj, -1.);
				shp.writeShapes( new File(dir,dive) );
				File xml = new File(dir,dive+".link");
				PrintStream out = new PrintStream(
					new FileOutputStream(xml));
				out.println("<data>");
				out.println(" <name>view</name>" );
				out.println(" <type>image</type>");
				out.println(" <url>"+url1+"${1}.jpg</url>");
				out.println(" <url>"+url2+"${1}.jpg</url>");
				out.println("</data>");
				out.close();
				tempAlvinShapes.timeToAltitude = new Hashtable();
				tempAlvinShapes.timeToDepth = new Hashtable();
				tempAlvinShapes.timeToHeading = new Hashtable();
				tempAlvinShapes.timeToCSVTemp = new Hashtable();
				tempAlvinShapes.timeToDVLTemp = new Hashtable();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}

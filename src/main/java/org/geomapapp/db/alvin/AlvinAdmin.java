package org.geomapapp.db.alvin;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import java.util.Calendar;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.geomapapp.db.alvin.AlvinNav;
import org.geomapapp.db.alvin.AlvinShapes;
import org.geomapapp.db.alvin.FrameURL;
import org.geomapapp.gis.shape.DBFFile;
import org.geomapapp.gis.shape.ESRIPoint;
import org.geomapapp.gis.shape.ESRIPolyLineM;
import org.geomapapp.gis.shape.ESRIShapefile;

public class AlvinAdmin {
	/* FrameDemo.java requires no other files. */

		private static void openAlvinAdmin() {
			//Create and set up the window.
			JFrame alvinDivePhotoFrame = new JFrame( "Alvin Dive Photos" );
			alvinDivePhotoFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			JPanel alvinDivePhotoContentPane = new JPanel( new BorderLayout( 10, 10 ) );
			JPanel nPane = new JPanel( new BorderLayout( 10, 10 ));
			JPanel sPane = new JPanel( new BorderLayout( 10, 10 ));
			JButton frameURLButton = new JButton("Run FrameURL");
			JButton alvinNavButton = new JButton("Run AlvinNav");
			JButton alvinShapesButton = new JButton("Run AlvinShapes");
			JButton alvinUploadButton = new JButton("Upload Files");
			final JTextField alvinPhotoTextField = new JTextField("Enter site-id", 20);

			alvinDivePhotoContentPane.add(nPane,"North");
			nPane.add(frameURLButton, "West");
			nPane.add(alvinNavButton, "Center");
			nPane.add(alvinShapesButton, "East");
			alvinDivePhotoContentPane.add(sPane,"South");
			sPane.add(alvinPhotoTextField,"North");
			//sPane.add(alvinUploadButton, "East");

			//Display the window.
			Border emptyBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
			alvinDivePhotoContentPane.setBorder( emptyBorder );
			alvinDivePhotoContentPane.setOpaque(true);
			alvinDivePhotoFrame.setContentPane(alvinDivePhotoContentPane);
			alvinDivePhotoFrame.pack();
			alvinDivePhotoFrame.setVisible(true);
			//Action performed on Run FrameURL
			frameURLButton.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String root = System.getProperty("user.dir") + File.separator + 
								"Photos" +File.separator + alvinPhotoTextField.getText();
					new FrameURL(root, false);
				}
			});

			//Action performed on Run AlvinNav
			alvinNavButton.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						String inputSiteID = alvinPhotoTextField.getText();
						String rootPath = System.getProperty("user.dir") + File.separator + 
		 									"Photos" + File.separator + inputSiteID;
						AlvinNav an = new AlvinNav();
						an.process(rootPath,inputSiteID);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			});

			//Action performed on Run AlvinShapes
			alvinShapesButton.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String root = System.getProperty("user.dir") + File.separator + 
									"Photos" + File.separator + alvinPhotoTextField.getText();
					AlvinShapes tempAlvinShapes;
					try {
						tempAlvinShapes = new AlvinShapes(alvinPhotoTextField.getText());
						ESRIShapefile alvin = new ESRIShapefile(root, alvinPhotoTextField.getText());
						Vector shapes = alvin.getShapes();
						DBFFile dbf = alvin.getDBFFile();
						Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
						cal.setTimeInMillis(0L);
						org.geomapapp.geom.IdentityProjection proj = new org.geomapapp.geom.IdentityProjection();
						for( int k=0 ; k<shapes.size() ; k++) {
							ESRIPolyLineM shape = (ESRIPolyLineM)shapes.get(k);
							String cruise = dbf.getValueAt(k,0).toString();
							String platform = dbf.getValueAt(k,1).toString();
							String launch = dbf.getValueAt(k,2).toString();
							//String platform = (String)tempAlvinShapes.diveToPlatform.get(dbf.getValueAt(k,2).toString());
						System.out.println("Platform: " + platform);	
							String dive = platform + "-"+ dbf.getValueAt(k,2).toString();
							System.out.println("Dive: " + dive);
							File dir = new File( root, cruise);
							File navDir = new File(dir + File.separator + "dives" + File.separator + launch + File.separator + "nav" );
							File[] navFile = navDir.listFiles();
							for ( int m = 0; m < navFile.length; m++ ) {
								tempAlvinShapes.open(navFile[m].toString());
							}
							dir = new File(dir, dive);
							if( !dir.exists() )dir.mkdirs();
							File images = new File( dir, "images");
							if ( platform.toLowerCase().indexOf("jason") != -1 ) {
								images = new File( root + File.separator + cruise + File.separator + "images");
							}
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
							Vector vectorOfURLs = new Vector();
							String temp = null;
							while ( (temp=in.readLine()).matches("^http(s)?://")) {
								vectorOfURLs.add(temp);
							}
							Vector names = new Vector(6);
							names.add("time");
							names.add("depth (m)");
							names.add("hdg (\u00B0)");
							names.add("alt (m)");
							names.add("temp1 (\u00B0C)");
							names.add("temp2 (\u00B0C)");
							names.add("image URLs");
							Vector classes = new Vector(7);
							classes.add(String.class);
							classes.add(String.class);
							classes.add(String.class);
							classes.add(String.class);
							classes.add(String.class);
							classes.add(String.class);
							classes.add(String.class);
							ESRIShapefile shp = new ESRIShapefile( dive, 1, names, classes);
							String s;

	/*
							if ( platform.toLowerCase().indexOf("jason") != -1 && temp != null) {
								String dateKey = null;
								String date = temp.substring( 0, temp.indexOf(".") );
								StringTokenizer st = new StringTokenizer(temp, "_.");
								temp = st.nextToken();
								cal.set( Calendar.YEAR, Integer.parseInt( temp.substring(0,4)) );
								cal.set( Calendar.DATE, Integer.parseInt( temp.substring(6,8)) );
								temp = st.nextToken();
								cal.set( Calendar.HOUR_OF_DAY, Integer.parseInt( temp.substring(0,2)) );
								cal.set( Calendar.MINUTE, Integer.parseInt( temp.substring(2,4)) );
								cal.set( Calendar.SECOND, Integer.parseInt( temp.substring(4,6)) );
								double time = .001*cal.getTimeInMillis();
								double[] xy = shape.xyAt(time);
								System.out.println(time);
								if( xy==null )continue;
								ESRIPoint p = new ESRIPoint(xy[0], xy[1]);
								Vector record = new Vector(4);
								record.add(date);
								dateKey = Integer.toString(cal.get(cal.YEAR)) + Integer.toString(cal.get(cal.MONTH)) + Integer.toString(cal.get(cal.DATE)) + Integer.toString(cal.get(cal.HOUR_OF_DAY)) + Integer.toString(cal.get(cal.MINUTE)) + Integer.toString(cal.get(cal.SECOND));
								if ( tempAlvinShapes.timeToDepth.containsKey( dateKey ) ) {
									record.add( tempAlvinShapes.timeToDepth.get( dateKey ).toString() );
								}
								else {
									record.add("NaN");
								}
								if ( tempAlvinShapes.timeToHeading.containsKey( dateKey ) ) {
									record.add( tempAlvinShapes.timeToHeading.get( dateKey ).toString() );
								}
								else {
									record.add("NaN");
								}
								if ( tempAlvinShapes.timeToAltitude.containsKey( dateKey ) ) {
									record.add( tempAlvinShapes.timeToAltitude.get( dateKey ).toString() );
								}
								else {
									record.add("NaN");
								}
								if ( tempAlvinShapes.timeToCSVTemp.containsKey( dateKey ) ) {
									record.add( tempAlvinShapes.timeToCSVTemp.get( dateKey ).toString() );
								}
								else {
									record.add("NaN");
								}
								if ( tempAlvinShapes.timeToDVLTemp.containsKey( dateKey ) ) {
									record.add( tempAlvinShapes.timeToDVLTemp.get( dateKey ).toString() );
								}
								else {
									record.add("NaN");
								}
								shp.addShape( p, record );
							}
	*/

							while( (s=in.readLine())!=null) {
								String sImageURLs = null;
								if ( s.indexOf(",") != -1 ) {
									sImageURLs = s.substring(s.indexOf(",") + 1);
									s = s.substring(0, s.indexOf(",") - 1);
								}
								String dateKey = null;
								String date = s.substring( 0, s.indexOf(".") );
								StringTokenizer st = new StringTokenizer(s, "_.");
								s = st.nextToken();
								cal.set( Calendar.YEAR, Integer.parseInt( s.substring(0,4)) );
								cal.set( Calendar.MONTH, Integer.parseInt( s.substring(4,6)) );
								cal.set( Calendar.DATE, Integer.parseInt( s.substring(6,8)) );
								s = st.nextToken();
								cal.set( Calendar.HOUR_OF_DAY, Integer.parseInt( s.substring(0,2)) );
								cal.set( Calendar.MINUTE, Integer.parseInt( s.substring(2,4)) );
								cal.set( Calendar.SECOND, Integer.parseInt( s.substring(4,6)) );
								double time = .001*cal.getTimeInMillis();
								double[] xy = shape.xyAt(time);
								if( xy==null )continue;
								ESRIPoint p = new ESRIPoint(xy[0], xy[1]);
								boolean addRecord = false;
								Vector record = new Vector(4);
								record.add(date);
								dateKey = date;
								if ( tempAlvinShapes.timeToDepth.containsKey( dateKey ) ) {
									record.add( tempAlvinShapes.timeToDepth.get( dateKey ).toString() );
									addRecord = true;
								}
								else {
									record.add("NaN");
								}
								if ( tempAlvinShapes.timeToHeading.containsKey( dateKey ) ) {
									record.add( tempAlvinShapes.timeToHeading.get( dateKey ).toString() );
									addRecord = true;
								}
								else {
									record.add("NaN");
								}
								if ( tempAlvinShapes.timeToAltitude.containsKey( dateKey ) ) {
									record.add( tempAlvinShapes.timeToAltitude.get( dateKey ).toString() );
									addRecord = true;
								}
								else {
									record.add("NaN");
								}
								if ( tempAlvinShapes.timeToCSVTemp.containsKey( dateKey ) ) {
									record.add( tempAlvinShapes.timeToCSVTemp.get( dateKey ).toString() );
									addRecord = true;
								}
								else {
									record.add("NaN");
								}
								if ( tempAlvinShapes.timeToDVLTemp.containsKey( dateKey ) ) {
									record.add( tempAlvinShapes.timeToDVLTemp.get( dateKey ).toString() );
									addRecord = true;
								}
								else {
									record.add("NaN");
								}
								if ( sImageURLs != null ) {
									record.add(sImageURLs);
								}
								else {
									record.add("N/A");
								}
								if ( addRecord ) {
									shp.addShape( p, record );
								}
							}
							in.close();
							if( shp.size()==0 ) continue;
							shp.forward( proj, -1.);
							shp.writeShapes(dir);
							File xml = new File(dir,dive+".link");
							PrintStream out = new PrintStream(
								new FileOutputStream(xml));
							out.println("<data>");
							out.println(" <name>view " + platform + " bottom photos</name>" );
							out.println(" <type>image</type>");
							for ( int j = 0; j < vectorOfURLs.size(); j++ ) {
								out.println(" <url>"+((String)vectorOfURLs.get(j))+"${1}.jpg</url>");
							}
							out.println(" <info>https://www.marine-geo.org/tools/search/entry.php?id=" + cruise.toUpperCase() + "</info>");
							out.println("</data>");
							out.close();
							tempAlvinShapes.timeToAltitude = new Hashtable();
							tempAlvinShapes.timeToDepth = new Hashtable();
							tempAlvinShapes.timeToHeading = new Hashtable();
							tempAlvinShapes.timeToCSVTemp = new Hashtable();
							tempAlvinShapes.timeToDVLTemp = new Hashtable();
						}
					} catch(Exception e1) {
						e1.printStackTrace();
					}
				}
			});

			//Action performed on Upload
			alvinUploadButton.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					//try {
						//String inputSiteID = alvinPhotoTextField.getText();
						//String rootPath = System.getProperty("user.dir") + File.separator + 
		 				//					"Photos" + File.separator + inputSiteID;
						//AlvinNav an = new AlvinNav();
						//an.process(rootPath,inputSiteID);
					//} catch (IOException e1) {
					//	e1.printStackTrace();
			//		}
				}
			});
		}
		public static void main(String[] args) {
		//Schedule a job for the event-dispatching thread:
		//creating and showing this application's GUI.
		//javax.swing.SwingUtilities.invokeLater(new Runnable() {
			//	public void run() {
			openAlvinAdmin();
			// }
		// });
		}
	}
package org.geomapapp.db.alvin;

import haxby.util.URLFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class FrameURL {
	String IMAGE_PREFIX = "https://4dgeo.whoi.edu/DAQ/";
	String navFileURL = File.separator +"data" + File.separator + "mgds" +
						File.separator +"field" + File.separator +"Alvin" + File.separator;
	//String navFileURL = "http://www.marine-geo.org/scratch/Alvin/";
	Hashtable imagePaths = new Hashtable();
	Hashtable imageNames = new Hashtable();
	Hashtable imageURLs = new Hashtable();
	Hashtable jasonPrintStreams = new Hashtable();
	Hashtable imageFileHeaders = new Hashtable();
	Hashtable imageFileImageLists = new Hashtable();
	Hashtable jasonNavPoints = new Hashtable();

/**
 * 
 * @param id
 * @param exitFrameURL
 */
	public FrameURL( String id, boolean exitFrameURL ) {
		try {
			File dir = new File(id);
			File info = new File( id + ".info" );
		System.out.println("Info: " +info);
			if ( info.exists() ) {
				BufferedReader in = new BufferedReader( new FileReader(info) );
				String s = null;
				while ( ( s = in.readLine() ) != null ) {
					
					imageURLs = new Hashtable();
					imageNames = new Hashtable();
					
					String[] result = s.split("\\s");
					String cruiseID = result[0];
					String siteID = result[2];
					String navFile = result[3];
					String platform = result[4];				
					String navtype = result[5];
					String diveID = platform + "-" + result[1];
					String lowerCaseDiveID = platform.toLowerCase() + "-" + result[1];
					String diveIDForURL = platform + "-D" + result[1];
					String navLocationString = id + File.separator + cruiseID + File.separator + "dives" + File.separator + result[1] + File.separator + "nav" + File.separator + navFile + ".gz";
					System.out.println( "cruiseID: " + cruiseID + "\t diveID: " + diveID + "\t siteID: " + siteID + "\t navFile: " + navFile + "\t platform: " + platform + "\t navType: " + navtype);
					URL url;
					BufferedReader inTemp;
					if( !dir.exists() ) {
						dir.mkdirs();
					}
					String sTemp;
					Vector dives;
					System.out.println("Dive Id: " +diveID);
					
					if ( platform.indexOf("Alvin") != -1 ) {
						String urlString = IMAGE_PREFIX+cruiseID+"/"+diveIDForURL+"/Src1/Images0001/proof.page1.html";
						url = URLFactory.url(urlString);
						System.out.println(urlString);
						try {
							inTemp = new BufferedReader(
									new InputStreamReader( url.openStream() ));
						} catch(IOException e) {
							System.out.println("***\tCould not open proofs page");
							continue;
						}
						File file = new File(dir, cruiseID + File.separator + lowerCaseDiveID);
				System.out.println("File: "+file);

						if( !file.exists() ) {
							file.mkdirs();
						}

				String navLocationURL =  navFileURL + result[1] + File.separator + "nav" +
										File.separator + navFile + ".gz";
			System.out.println("NavFile: " + navLocationURL);
				String navFileName = navFile + ".gz";

						File file2 = new File(dir, cruiseID + File.separator + "dives" + 
										File.separator + result[1]+ File.separator + "nav");
						if(!file2.exists()){
							file2.mkdirs();
						}
						
						InputStream inNav = new FileInputStream(navLocationURL);
						OutputStream outNav = new FileOutputStream(file2+ "/" + navFileName);
						byte[] buf = new byte[1024];
						int len;
						while ((len = inNav.read(buf)) > 0){
							outNav.write(buf, 0, len);
						}

						inNav.close();
						outNav.close();
						System.out.println("Copied file to " + file2);

						file = new File(file, "images");
						PrintStream out = new PrintStream(new FileOutputStream( file ));
						String url1 = IMAGE_PREFIX+cruiseID+"/"+diveIDForURL+"/Src1/Images0001/";
						String url2 = IMAGE_PREFIX+cruiseID+"/"+diveIDForURL+"/Src2/Images0001/";
						out.println( url1 + "SubSea1." );
						out.println( url2 + "SubSea2.");
						Vector pages = new Vector();
						while( (sTemp=inTemp.readLine())!=null ) {
							int from = sTemp.indexOf("proof.page");
							if( from<0 )continue;
							from = sTemp.indexOf("proof.page", from+1);
							while( from>=0 ) {
								pages.add( sTemp.substring(from, sTemp.indexOf("\"",from)) );
								sTemp = sTemp.substring( from+1 );
								from = sTemp.indexOf("proof.page");
							}
							break;
						}
						Vector images = getImageNames(inTemp, out, null);
						inTemp.close();
						for( int i=1 ; i<pages.size() ; i++) {
							url = URLFactory.url(url1 + pages.get(i).toString());
							inTemp = new BufferedReader(new InputStreamReader( url.openStream() ));
							images = getImageNames(inTemp, out, images);
							inTemp.close();
						}
						out.close();
						System.out.println( "\t"+ pages.size() +" pages, "+images.size()+" images");
					}
					else if ( platform.indexOf("Jason") != -1 ) {
						String imageFilePath = null;
						String sJason = null;
						String diveDirString = id + File.separator + cruiseID + File.separator + lowerCaseDiveID;

/*						String dateKey = null;
						Calendar cal = Calendar.getInstance( TimeZone.getTimeZone("GMT") );
						String sJasonNav = null;
						BufferedReader inJasonNav = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream( navLocationString ))));
						inJasonNav.readLine();
						while ( ( sJasonNav = inJasonNav.readLine() ) != null ) {
							StringTokenizer st = new StringTokenizer(sJasonNav, " \t,");

							StringTokenizer st1 = new StringTokenizer( st.nextToken().trim(), "-/");
							StringTokenizer st2 = new StringTokenizer( st.nextToken().trim(), ":");
							cal.set( cal.YEAR, Integer.parseInt(st1.nextToken()) );
							cal.set( cal.MONTH, Integer.parseInt(st1.nextToken()) );
							cal.set( cal.DATE, Integer.parseInt(st1.nextToken()) );
							cal.set( cal.HOUR_OF_DAY, Integer.parseInt(st2.nextToken()) );
							cal.set( cal.MINUTE, Integer.parseInt(st2.nextToken()) );
							
//							GMA 1.5.2: Read in seconds as double
//							cal.set( cal.SECOND, Integer.parseInt(st2.nextToken()) );
//							cal.set( cal.MILLISECOND, 0 );
							String seconds = st2.nextToken();
							if ( seconds.indexOf(".") != -1 ) {
								cal.set( cal.SECOND, Integer.parseInt( seconds.substring( 0, seconds.indexOf(".") ) ) );
								cal.set( cal.MILLISECOND, Integer.parseInt( seconds.substring( seconds.indexOf(".") + 1 ) ) );
							}
							else {
								cal.set( cal.SECOND, Integer.parseInt(seconds) );
								cal.set( cal.MILLISECOND, 0 );
							}
							dateKey = Integer.toString(cal.get(cal.YEAR)) + Integer.toString(cal.get(cal.MONTH)) + Integer.toString(cal.get(cal.DATE)) + Integer.toString(cal.get(cal.HOUR_OF_DAY)) + Integer.toString(cal.get(cal.MINUTE)) + Integer.toString(cal.get(cal.SECOND));
							jasonNavPoints.put(dateKey, dateKey);
							double t = cal.getTimeInMillis()*.001;
						}
						inJasonNav.close();
*/
						String imagesDirString = id + File.separator + cruiseID + File.separator + diveID;
						PrintStream out = null;
						File imageFile;
						File jasonPhotos = new File( id + File.separator + cruiseID + File.separator + "jasonphotolist.info" );
						File diveDir = new File( diveDirString );
						if ( !diveDir.exists() ) {
							diveDir.mkdir();
						}
						File imagesDir = new File( imagesDirString );
						if ( !imagesDir.exists() ) {
							imagesDir.mkdir();
						}

						BufferedReader inJason = new BufferedReader( new FileReader(jasonPhotos) );

						while ( ( sJason = inJason.readLine() ) != null ) {
							String[] imageDirSplit = sJason.split("\\.");
							if ( imageDirSplit.length > 2 ) {
								imageDirSplit[0] = imageDirSplit[0].substring(imageDirSplit[0].indexOf("/DAQ"));
								imageDirSplit[0] = "https://4dgeo.whoi.edu" + imageDirSplit[0] + ".";
								String[] imageDirSplitTwo = imageDirSplit[0].split("Jason");
								if ( !imageURLs.containsKey(imageDirSplitTwo[0]) ) {
									imageURLs.put(imageDirSplitTwo[0] + "Jason/", imageDirSplitTwo[0] + "Jason/");
								}
								if ( !imageDirSplit[1].equals("") ) {
									String tempElement = "";
									tempElement = (String)imageNames.get(imageDirSplit[1]);
									if ( imageNames.containsKey(imageDirSplit[1]) && tempElement.indexOf(imageDirSplitTwo[1]) == -1 ) {
										imageNames.put(imageDirSplit[1], tempElement + "," + imageDirSplitTwo[1]);
									} else if ( imageNames.containsKey(imageDirSplit[1]) ) {
							//			imageNames.put(imageDirSplit[1], imageDirSplit[1] + "." + imageDirSplit[2] + "," + imageDirSplitTwo[1]);
									} else {
										imageNames.put(imageDirSplit[1], imageDirSplit[1] + "." + imageDirSplit[2] + "," + imageDirSplitTwo[1]);
									}
								}
							}
						}

						String newImagesDirString = id + File.separator + cruiseID;
//						File newImagesDir = new File(newImagesDirString);
						PrintStream newOut = new PrintStream(new FileOutputStream(newImagesDirString + File.separator + "images"));

						ArrayList sortImages = new ArrayList();
						for (Enumeration e = imageURLs.keys(); e.hasMoreElements(); ) {
							sortImages.add((String)e.nextElement());
						}
						Collections.sort(sortImages);
						for ( int i = 0; i < sortImages.size(); i++ ) {
//							System.out.println((String)sortImages.get(i));
							newOut.println((String)sortImages.get(i));
						}
						sortImages = new ArrayList();
//						for (Enumeration e = imageNames.keys(); e.hasMoreElements(); ) {
						for (Enumeration e = imageNames.elements(); e.hasMoreElements(); ) {
							sortImages.add((String)e.nextElement());
						}
						Collections.sort(sortImages);
						for ( int i = 0; i < sortImages.size(); i++ ) {
//							System.out.println((String)sortImages.get(i));
							newOut.println((String)sortImages.get(i));
						}
						newOut.flush();
						newOut.close();
						inJason.close();

/*						while ( ( sJason = inJason.readLine() ) != null ) {
							String[] imageDirSplit = sJason.split("\\.");
							if ( imageDirSplit.length > 2 ) {								
								if ( imageDirSplit[0].toLowerCase().indexOf("image") != -1) {
									imageFilePath = imageDirSplit[0].substring(imageDirSplit[0].toLowerCase().indexOf("image"));
									imageFilePath = imagesDirString + File.separator + imageFilePath.substring(0, imageFilePath.indexOf("/"));
									if ( !imageDirs.containsKey(imageFilePath) ) {
										System.out.println(imageFilePath);
										imageDirs.put(imageFilePath, imageFilePath);
									}
								}

								if ( !imagePaths.containsKey(imageDirSplit[0]) ) {
									imagePaths.put(IMAGE_PREFIX + imageDirSplit[0].substring(imageDirSplit[0].indexOf("DAQ") + 4) + ".", imageFilePath);
								}

								if ( !imageNames.containsKey(imageDirSplit[1] + imageDirSplit[2]) ) {
									imageNames.put(imageDirSplit[1] + "." + imageDirSplit[2], imageFilePath);
								}
							}
						}

						String newImagesDirString = id + File.separator + cruiseID;
						File newImagesDir = new File(newImagesDirString);
						if ( !newImagesDir.exists() ) {
							newImagesDir.mkdir();
						}
						PrintStream newOut = new PrintStream(new FileOutputStream(newImagesDirString + File.separator + "images"));

						for (Enumeration e1 = imageDirs.elements(); e1.hasMoreElements(); ) {
							ArrayList sortImagePaths = new ArrayList();
							ArrayList sortImageNames = new ArrayList();
							imageFilePath = (String)e1.nextElement();
							imageFile = new File(imageFilePath.toLowerCase());
							out = new PrintStream(new FileOutputStream(imageFile));
							for ( Enumeration e2 = imagePaths.keys(); e2.hasMoreElements(); ) {
								String currentKey = (String)e2.nextElement();
								if ( ( (String)imagePaths.get( currentKey ) ).equals(imageFilePath) ) {
									sortImagePaths.add(currentKey);
								}
							}
							Collections.sort(sortImagePaths);
							for ( int i = 0; i < sortImagePaths.size(); i++ ) {
								out.println((String)sortImagePaths.get(i));
								newOut.println((String)sortImagePaths.get(i));
							}
							for ( Enumeration e2 = imageNames.keys(); e2.hasMoreElements(); ) {
								String currentKey = (String)e2.nextElement();
								if ( ( (String)imageNames.get( currentKey ) ).equals(imageFilePath) ) {
									sortImageNames.add(currentKey);
								}
							}
							Collections.sort(sortImageNames);
							for ( int i = 0; i < sortImageNames.size(); i++ ) {
								out.println((String)sortImageNames.get(i));
								newOut.println((String)sortImageNames.get(i));
							}
							out.flush();
							out.close();
						}
						newOut.flush();
						newOut.close();
						inJason.close();
*/
					}
				}
				in.close();
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		if ( exitFrameURL ) {
			System.exit(0);
		}
	}

	Vector getImageNames( BufferedReader in, PrintStream out, Vector images) throws IOException {
		if( images==null ) images = new Vector();
		String s;
		while( (s=in.readLine())!=null ) {
			int from=0;
			while( (from=s.indexOf("\"SubSea1"))>=0 ) {
				from++;
				String name = s.substring(from, s.indexOf( "\"", from));
				name = name.substring(name.indexOf(".")+1);
				out.println( name );
				images.add( name );
				s = s.substring( from+1 );
			}
		}
		return images;
	}

	public static void main(String[] args) {
		if( args.length!=1 ) {
			System.out.println("usage: java org.geomapapp.db.alvin.FrameURL siteID");
			System.exit(0);
		}
		new FrameURL( args[0], true );
	}
}

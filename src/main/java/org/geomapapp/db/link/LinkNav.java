package org.geomapapp.db.link;

import haxby.util.URLFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.geom.Mercator;
import org.geomapapp.geom.Navigation;
import org.geomapapp.gis.shape.ESRIPolyLineM;

public class LinkNav {
	double[][] xyt;
	Vector tmpXYT;
	ESRIPolyLineM shape;
	String cruiseID;
	public LinkNav(String cruiseID) {
		this.cruiseID = cruiseID;
		tmpXYT = new Vector();
	}
	public String getID() {
		return cruiseID;
	}
	public void addPoint(double[] xyt) {
		if( tmpXYT==null ) tmpXYT = new Vector();
		tmpXYT.add(xyt);
	}
	public void finishInput() {
		xyt = new double[tmpXYT.size()][];
		for( int k=0 ; k<xyt.length ; k++) xyt[k] = (double[])tmpXYT.get(k);
		tmpXYT = null;
	}
	public ESRIPolyLineM getShape() {
		return getShape( new Mercator(0., 0., 40000, 0, 0),
				40000., -1., 10., 1.);
	}
	public ESRIPolyLineM getShape( MapProjection proj, double xWrap, double maxDT, double maxDX, double maxDR ) {
		if( shape!=null )return shape;
		Navigation nav = new Navigation(cruiseID, xyt);
		nav.computeControl( proj, xWrap, maxDT, maxDX, maxDR );
		return (ESRIPolyLineM)nav.getShape();
	}
	public static LinkNav getNav(String id) throws IOException {
		String url = "http://www.marine-geo.org/tools/search/ado_merged.php?id="+id+"&type=nav";
		return getNav(id, url);
	}
	public static LinkNav getNav(String id, String url) throws IOException {
		LinkNav nav = new LinkNav(id);
		BufferedReader in = new BufferedReader(
			new InputStreamReader( (URLFactory.url(url)).openStream() ));
		String s;
		StringTokenizer st;
		while( (s=in.readLine())!=null ) {
			st = new StringTokenizer(s);
			if( st.countTokens()!=4)continue;
			double t = parseLinkDateTime(st.nextToken(), st.nextToken());
			nav.addPoint( new double[] { 
					Double.parseDouble(st.nextToken()),
					Double.parseDouble(st.nextToken()),
					t
				});
		}
		in.close();
		nav.finishInput();
		return nav;
	}
	public static double parseLinkDateTime(String date, String time) {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		StringTokenizer st = new StringTokenizer( date, "-" );
		cal.set( Calendar.YEAR, Integer.parseInt(st.nextToken()) );
		cal.set( Calendar.MONTH, Integer.parseInt(st.nextToken())-1 );
		cal.set( Calendar.DATE, Integer.parseInt(st.nextToken()) );
		st = new StringTokenizer( time, ":" );
		cal.set( Calendar.HOUR_OF_DAY, Integer.parseInt(st.nextToken()) );
		cal.set( Calendar.MINUTE, Integer.parseInt(st.nextToken()) );
		cal.set( Calendar.SECOND, Integer.parseInt(st.nextToken()) );
		cal.set( Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis()*.001;
	}
}

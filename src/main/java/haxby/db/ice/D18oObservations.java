package haxby.db.ice;

import haxby.map.Overlay;
import haxby.map.XMap;
import haxby.proj.Projection;
import haxby.util.URLFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;

public class D18oObservations implements Overlay {
	IceDB db;
	XMap map;
	static float[][] xyd = new float[0][6];
	Color[] color;
	static boolean loaded = false;
	boolean display;
	public D18oObservations(IceDB db, XMap map) {
		this.db = db;
		this.map = map;
		display = true;
		if(!loaded)load();
	}
	public void setDraw( boolean tf ) {
		display = tf;
	}
	public void plotXY( Graphics2D g,
				Rectangle2D bounds,
				double xScale, double yScale ) {
		g.setColor( Color.blue );
		float x0 = (float)bounds.getX();
		float y0 = (float)bounds.getY();
		float sy = (float)yScale;
		float sx = (float)xScale;
		Arc2D.Float arc = new Arc2D.Float(0f, 0f, 5f, 5f, 0f, 360f,
				Arc2D.CHORD);
		for(int k=0 ; k<xyd.length ; k++) {
			float x = (xyd[k][4]-x0)*sx;
			float y = (xyd[k][3]-y0)*sy;
			arc.x = x-2.5f;
			arc.y = y-2.5f;
			g.draw(arc);
		}
	}
		long[] timeInterval() {
				long[] interval = new long[2];
				Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
				interval[0] = 0;
				interval[1] = cal.getTimeInMillis();
				cal.setTimeInMillis(0L);
				try {
						if( db.before.isSelected() ) {
								StringTokenizer st = new StringTokenizer(db.startF.getText(), "/");
								int month = st.countTokens()==2 ? Integer.parseInt(st.nextToken().trim())-1 : 0;
								cal.set( cal.MONTH, month);
								cal.set( cal.YEAR, Integer.parseInt(st.nextToken().trim()));
								cal.set( cal.DATE, 1);
								interval[0] = cal.getTimeInMillis();
						}
						if( db.after.isSelected() ) {
								StringTokenizer st = new StringTokenizer(db.endF.getText(), "/");
								int month = st.countTokens()==2 ? Integer.parseInt(st.nextToken().trim())-1 : 0;
								cal.set( cal.MONTH, month);
								cal.set( cal.YEAR, Integer.parseInt(st.nextToken().trim()));
								cal.set( cal.DATE, 1);
								interval[1] = cal.getTimeInMillis();
						}
				} catch(Exception e) {
				}
				return interval;
		}
	public void draw(Graphics2D g) {
	//	if(!display) return;
	int yr1 = 1900;
	int yr2 = 2005;
	try {
		long[] interval = timeInterval();
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		if( db.before.isSelected() ) {
				cal.setTimeInMillis(interval[0]);
				yr1 = cal.get(cal.YEAR);
		}
		if( db.after.isSelected() ) {
			cal.setTimeInMillis(interval[1]);
			yr2 = cal.get(cal.YEAR);
		}
	} catch(Exception e) {
	}
	//	System.out.println( yr1 +"\t"+ yr2);
		float size = 2.0f + 6f/(float)map.getZoom();
		Arc2D.Float dot = new Arc2D.Float(-size/2f, -size/2f, size, size, 0f, 360f, Arc2D.CHORD);
		float size1 = 2.0f + 7f/(float)map.getZoom();
		GeneralPath triangle = new GeneralPath();
		triangle.moveTo(0f, size1/2f );
		triangle.lineTo(size1/2f, -size1/2f );
		triangle.lineTo(-size1/2f, -size1/2f );
		triangle.closePath();
	//	Rectangle2D.Float square = new Rectangle2D.Float(-size1/2f, -size1/2f, size1, size1);
		AffineTransform at = g.getTransform();
		g.setStroke( new BasicStroke( .5f/(float)map.getZoom() ) );
		RenderingHints hints = g.getRenderingHints();
		g.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		Shape shape = dot;
		for(int k=0 ; k<xyd.length ; k++) {
		//	Shape shape = triangle;
		//	if( xyd[k][5]<=1991f ) shape = dot;
			if( xyd[k][5]>yr2 )continue;
			if( xyd[k][5]<yr1 )continue;
			g.translate( (double)xyd[k][0], (double)xyd[k][1] );
		//	g.setColor( Color.black );
		//	g.draw(shape);
			g.setColor( color[k] );
			g.fill(shape);
			g.setColor( Color.black );
			g.draw(shape);
			g.setTransform( at );
		}
		g.setFont( (new Font("Serif", Font.BOLD, 1)).deriveFont( size1*1.5f));
		Rectangle2D rect = map.getClipRect2D();
		double s = (double) size1;
		double x = rect.getX() + s;
		double y = rect.getY() + 2.*s;
	//	g.translate( x, y );
	//	g.setColor( Color.white );
	//	g.fill( dot );
	//	g.setColor( Color.black );
	//	g.draw( dot );
	//	g.translate( s, s/2.);
	//	String year = db.startF.getText();
	//	String dateString = db.before.isSelected()
	//		? "before 1/1/"+year
	//		: "after 1/1/"+year;
	//	g.drawString( dateString, 0, 0);
	//	g.drawString( "before 1/1/1991", 0, 0);
	//	g.setTransform(at);
	//	g.translate( x, y+s*2.0 );
	//	g.setColor( Color.white );
	//	g.fill( triangle );
	//	g.setColor( Color.black );
	//	g.draw( triangle );
	//	g.translate( s, s/2.);
	//	g.drawString( "after 1/1/1991", 0, 0);
		g.setTransform(at);
		g.setRenderingHints( hints);
	}
	void load() {
		try {
			Vector obs = new Vector();
			URL url = URLFactory.url(haxby.map.MapApp.TEMP_BASE_URL + "arctic/ice/d18o_50.obs");
			BufferedReader in = new BufferedReader(
					new InputStreamReader( url.openStream() ));
			String s;
			StringTokenizer st;
			Point2D.Double p = new Point2D.Double();
			Point2D xy;
			Projection proj = map.getProjection();
			while( (s=in.readLine()) != null ) {
				float[] pt = new float[6];
				st = new StringTokenizer(s);
				p.x = Double.parseDouble( st.nextToken() );	// Longitude
				p.y = Double.parseDouble( st.nextToken() );	// Latitude
				pt[4] = D18oObs.getValue( p.x, p.y );
				xy = proj.getMapXY( p );
				pt[0] = (float)xy.getX();
				pt[1] = (float)xy.getY();
				pt[2] = Float.parseFloat( st.nextToken() );	// depth
				if( pt[2]>25f )continue;
				pt[3] = Float.parseFloat( st.nextToken() );	// d18o
				pt[5] = Float.parseFloat( st.nextToken() );	// year
				obs.add(pt);
			}
			in.close();
			xyd = new float[ obs.size() ][6];
			color = new Color[obs.size()];
			for( int i=0 ; i<obs.size() ; i++) {
				xyd[i] = (float[]) obs.get(i);
				color[i] = IceCore.getColor( xyd[i][3]+2.0f );
			}
			loaded = true;
		} catch(IOException ex) {
			ex.printStackTrace();
			loaded=false;
		}
	}
}

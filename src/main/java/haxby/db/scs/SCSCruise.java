package haxby.db.scs;

import haxby.map.XMap;
import haxby.nav.ControlPt;
import haxby.nav.Nearest;
import haxby.nav.TimeControlPt;
import haxby.nav.TrackLine;
import haxby.proj.Projection;
import haxby.util.URLFactory;

import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class SCSCruise {
	XMap map = null;
	String name;
	short[][] panelSize;
	int[] start;
	short[] top;
	int[] xPosition;
	TrackLine nav;
	double[] bounds=null;
	double xScale, yScale;
	String urlPath;
	static Calendar cal=null;
	GeneralPath path = null;
	int width = 300;
	protected SCSCruise() {
	}
	public SCSCruise( XMap map, String urlPath ) throws IOException {
		this.map = map;
		if( !urlPath.endsWith("/") ) urlPath += "/";
		this.urlPath = urlPath;
		if( cal==null ) cal = Calendar.getInstance( TimeZone.getTimeZone( "GMT" ));

		URL url = URLFactory.url( urlPath + "control.nav" );
		DataInputStream in = new DataInputStream( url.openStream() );
		name = in.readUTF();
		int nseg = in.readInt();
		if( nseg==0 ) throw new IOException( "no control points in cruise" );
		Projection proj = map.getProjection();
		TimeControlPt[][] cpts = new TimeControlPt[nseg][];
		Point2D.Double p0, p;
		p0 = null;
		double wrap = map.getWrap();
		bounds = new double[4];
		int t1 = 0;
		int end = 0;
		for( int i=0 ; i<nseg ; i++) {
			cpts[i] = new TimeControlPt[in.readInt()];
			for( int j=0 ; j<cpts[i].length ; j++) {
				p = new Point2D.Double( 1.e-6* (double)in.readInt(),
							1.e-6* (double)in.readInt() );
				p = (Point2D.Double)proj.getMapXY( p );
				end = in.readInt();
				if( p0==null ) {
					p0=p;
					bounds = new double[] { p0.x, p0.x, p0.y, p0.y };
					t1 = end;
				} else if( wrap>0 ) {
					while( p.x>p0.x+wrap*.5 ) p.x -= wrap;
					while( p.x<p0.x-wrap*.5 ) p.x += wrap;
					p0 = p;
				}
				if( p.x>bounds[1] ) bounds[1] = p.x;
				else if( p.x<bounds[0] ) bounds[0] = p.x;
				if( p.y>bounds[3] ) bounds[3] = p.y;
				else if( p.y<bounds[2] ) bounds[2] = p.y;
				cpts[i][j] = new TimeControlPt(
						new ControlPt.Float( (float)p.x, (float)p.y ),
						end );
			}
		}
		in.close();
		Rectangle2D.Double rect = new Rectangle2D.Double( bounds[0], bounds[2], 
							bounds[1]-bounds[0],
							bounds[3]-bounds[2]);
		nav = new TrackLine( name, rect, cpts, t1, end, (byte)0, (int)wrap);

		url = URLFactory.url( urlPath + "panels2.info" );
		in = new DataInputStream( url.openStream() );
		xScale = in.readDouble();
		yScale = in.readDouble();
		int nPanel = in.readInt();
		panelSize = new short[nPanel][2];
		start = new int[nPanel];
		top = new short[nPanel];
		xPosition = new int[nPanel];
		for( int k=0 ; k<nPanel ; k++ ) {
			panelSize[k][0] = in.readShort();
			panelSize[k][1] = in.readShort();
			start[k] = in.readInt();
			top[k] = in.readShort();
			xPosition[k] = in.readInt();
		}
		width = xPosition[nPanel-1] + panelSize[nPanel-1][0];
		in.close();
	}

	public TrackLine getNav() {
		return nav;
	}

	public boolean contains( double x, double y ) {
		return nav.contains( x, y );
	}
	public long getTime( Nearest nearest) {
		return nav.getTime( nearest );
	}
	public int getPanel( long time ) {
		int t = (int)(time/1000L);
		int i=0;
		while( i<start.length-1 && t>start[i]+panelSize[i][0]*30 ) i++;
	//	System.out.println( t +"\t"+ start[0] +"\t"+ start[i] +"\t"+ i +"\t"+ start.length );
		if( t<start[i] || t>start[i]+panelSize[i][0]*30 ) return -1;
		return i;
	}
	public double[] xyAtTime( double t ) {
		Point2D p = nav.positionAtTime( (int)t );
		if( p==null ) return new double[] {Double.NaN, Double.NaN};
		p = map.getProjection().getRefXY(p);
		return new double[] { p.getX(), p.getY() };
	}
	public GeneralPath getPanelPath( int panel ) {
		if( panel<0 || panel>=start.length ) return null;
		int i = panel;
		return nav.getPath( start[i], start[i]+panelSize[i][0]*30 );
	}
	public boolean firstNearPoint( double x, double y, Nearest n) {
		return nav.firstNearPoint( x, y, n);
	}
	public void draw( Graphics2D g ) {
		nav.draw(g);
	}

//	1.3.5: Overwrite toString() to return name so that 
//	when objects are displayed in list readable name is 
//	shown
	public String toString() {
		return name;
	}

	public static String dateString( long secs ) {
		if( cal==null ) cal = Calendar.getInstance( TimeZone.getTimeZone( "GMT" ));
				cal.setTime( new Date( secs ) );
				StringBuffer date =  new StringBuffer();
				date.append( cal.get(cal.YEAR)+"_" );
				int m = cal.get(cal.MONTH)+1;
				if( m<10 ) date.append("0");
				date.append( m+"_");
				m = cal.get( cal.DATE );
				if( m<10 ) date.append("0");
				date.append( m+"_");
				m = cal.get( cal.HOUR_OF_DAY );
				if( m<10 ) date.append("0");
				date.append( m+"_");
				m = cal.get( cal.MINUTE );
				if( m<10 ) date.append("0");
				date.append( m+"");
				return date.toString();
		}

	public String getName() {
		return name;
	}
}
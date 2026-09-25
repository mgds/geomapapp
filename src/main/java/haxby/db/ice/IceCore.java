package haxby.db.ice;

import haxby.db.*;
import haxby.map.*;
import haxby.proj.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;

public class IceCore implements Comparable, XYPoints {
	XMap map;
	String name;
	float[] d1, d2, d18o, diff;
	float[] depth;
	float thickness;
	int[] index18o;
	float[] lat;
	float[] lon;
	float[] T;
	float[] trajX, trajY;
	float[] obs;
	int trackEnd=0;
	double[] hIce;
	int[] date;
	boolean highlight;
	float[] range;
	IceDB db;
	static float[][] color = {
			{0f,0f,1f},
			{0f,1f,1f},
			{0f, 1f, 0f},
			{1f,1f,0f},
			{1f,0f,0f},
			{1f, 0f, 1f}
			};
	public IceCore( IceDB db, XMap map, String name, int[] date, float[][] data, float[][] track) {
		this.db = db;
		this.map=map;
		this.name = name;
		this.date = date;
		highlight = false;
		d1 = new float[data.length];
		d2 = new float[data.length];
		depth = new float[data.length];
		d18o = new float[data.length];
		diff = new float[data.length];
		range = new float[] {100f, -100f};
		for( int i=0 ; i<data.length ; i++) {
			d1[i] = data[i][0];
			d2[i] = data[i][1];
			depth[i] = .5f * (d1[i] + d2[i]);
			d18o[i] = data[i][2];
			diff[i] = data[i][3];
		}
		thickness = d2[data.length-1];
		boolean[] edit = new boolean[data.length];
		for( int i=0 ; i<data.length ; i++) {
			edit[i]=true;
			if( i>0 && Math.abs( d18o[i]-d18o[i-1] )<2.) edit[i]=false;
			if( i<data.length-1 && Math.abs( d18o[i]-d18o[i+1] )<2.) edit[i]=false;
		}
		int m=0;
		for( int i=0 ; i<data.length ; i++) {
			if( edit[i] ) {
				m++;
			} else if(m>0) {
				d1[i-m] = d1[i];
				d2[i-m] = d2[i];
				depth[i-m] = depth[i];
				d18o[i-m] = d18o[i];
				diff[i-m] = diff[i];
			}
		}
		if( m!=0 ) {
			float[] tmp = new float[data.length-m];
			System.arraycopy( d1, 0, tmp, 0, tmp.length );
			d1 = tmp;
			tmp = new float[data.length-m];
			System.arraycopy( d2, 0, tmp, 0, tmp.length );
			d2 = tmp;
			tmp = new float[data.length-m];
			System.arraycopy( depth, 0, tmp, 0, tmp.length );
			depth = tmp;
			tmp = new float[data.length-m];
			System.arraycopy( d18o, 0, tmp, 0, tmp.length );
			d18o = tmp;
			tmp = new float[data.length-m];
			System.arraycopy( diff, 0, tmp, 0, tmp.length );
			diff = tmp;
		}
		for( int i=0 ; i<d2.length ; i++) {
			if( depth[i]<.1*thickness ) continue;
			if( d18o[i]>range[1] )range[1] = d18o[i];
			if( d18o[i]<range[0] )range[0] = d18o[i];
		}
		lat = new float[track.length];
		lon = new float[track.length];
		T = new float[track.length];
		trajX = new float[track.length];
		trajY = new float[track.length];
		obs = new float[track.length];
		Projection proj = map.getProjection();
		trackEnd = 0;
		for(int i=0 ; i<track.length ; i++) {
			lat[i] = track[i][0];
			lon[i] = track[i][1];
			if( i!=0 && (lat[i]!=lat[i-1] || lon[i]!=lon[i-1])) trackEnd=i;
			T[i] = track[i][2];
			Point2D p = proj.getMapXY(new Point2D.Float(lon[i], lat[i]));
			trajX[i] = (float)p.getX();
			trajY[i] = (float)p.getY();
		//	obs[i] = D18oObs.getValue( (double)lon[i],
		//				(double)lat[i]);
			obs[i] = D18OGrid.valueAt( p.getX(), p.getY() );
			if( Float.isNaN( obs[i] ) ) continue;
			if( obs[i]>range[1] )range[1] = obs[i];
			if( obs[i]<range[0] )range[0] = obs[i];
		}
		hIce = new double[1];
		computeGrowth();
	}
	public String toString() {
		return name;
	}
	public int compareTo( Object o) {
		return name.compareTo( o.toString() );
	}
	public boolean select( float x, float y, float dx ) {
		for( int i=0 ; i<depth.length ; i++) {
			float xx = trajX[index18o[i]] - x;
			if( xx<-dx || xx>dx )continue;
			xx = trajY[index18o[i]] - y;
			if( xx<-dx || xx>dx )continue;
			return true;
		}
		return false;
	}
	public void drawTrack( Graphics2D g ) {
		float zoom = (float)map.getZoom();
		g.setStroke( new BasicStroke( 1f/zoom ));
		if( highlight ) g.setColor( Color.black );
		else g.setColor( Color.gray );
		boolean connect = false;
		GeneralPath path = new GeneralPath();
/*
				int yr1 = 1900;
				int yr2 = 2005;
				try {
						if( db.before.isSelected() ) {
								yr2 = Integer.parseInt( db.startF.getText() )-1;
						} else {
								yr1 = Integer.parseInt( db.startF.getText() );
						}
				} catch(Exception e) {
				}
*/

		long[] interval = timeInterval();
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.set( date[2], date[0]-1, date[1] );
		for(int i=1 ; i<hIce.length ; i+=10 ) {
		//	int year = cal.get(cal.YEAR);
			cal.add(cal.DATE, -10);
			long time = cal.getTimeInMillis();
			if( time<interval[0] || time>interval[1] )continue;
		//	if( year<yr1 )continue;
		//	if( year>=yr2 )continue;
		//	if( hIce[i]==hIce[i-1] ) {
		//		connect=false;
		//		continue;
		//	}
			if( connect ) {
				path.lineTo( trajX[i], trajY[i] );
			} else {
				path.moveTo( trajX[i], trajY[i] );
				connect = true;
			}
		}
		int i = hIce.length-1;
	//	path.lineTo( trajX[i], trajY[i] );
		g.draw( path);
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
	public void draw( Graphics2D g ) {
		float zoom = (float)map.getZoom();
		g.setStroke( new BasicStroke( 1f/zoom ));
/*
				int yr1 = 1900;
				int yr2 = 2005;
				try {
						if( db.before.isSelected() ) {
								yr2 = Integer.parseInt( db.startF.getText() )-1;
						} else {
								yr1 = Integer.parseInt( db.startF.getText() );
						}
				} catch(Exception e) {
				}
*/

		long[] interval = timeInterval();
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		for( int i=0 ; i<depth.length ; i++) {
			cal.set( date[2], date[0]-1, date[1] );
			cal.add(cal.DATE, -index18o[i]);
			long time = cal.getTimeInMillis();
			if( time<interval[0] || time>interval[1] )continue;
		//	int year = cal.get(cal.YEAR);
		//	cal.set( date[2], date[0]-1, date[1] );
		//	if( year<yr1 )continue;
		//	if( year>=yr2 )continue;
			if( depth[i]/thickness <= .1f )continue;
			if( index18o[i]>trackEnd )continue;
			Rectangle2D.Float rect = new Rectangle2D.Float(trajX[index18o[i]]-2.0f-1.5f/zoom,
						trajY[index18o[i]]-2.0f-1.5f/zoom, 4f+3f/zoom, 4f+3f/zoom);
			if(highlight) {
				g.setColor( Color.black );
			} else {
				g.setColor( Color.gray );
			}
				g.fill( rect );
				rect.x += 1.f/zoom;
				rect.y += 1.f/zoom;
				rect.width -= 2.f/zoom;
				rect.height -= 2.f/zoom;
			//	g.setColor( getColor( d18o[i] ));
				g.setColor( getColor( .5f+d18o[i] ));
				g.fill( rect );
		}
	}
	public void plotXY( Graphics2D g,
				Rectangle2D bounds,
				double xScale, double yScale,
				java.util.Vector xy) {
		if( highlight ) g.setColor( Color.red );
		else g.setColor( Color.black );
		float x0 = (float)bounds.getX();
		float y0 = (float)bounds.getY();
		float sy = (float)yScale;
		float sx = (float)xScale;
		Rectangle2D.Float rect = new Rectangle2D.Float(0f, 0f, 4f, 4f);
		for( int i=0 ; i<depth.length ; i++) {
			if( depth[i]/thickness <= .1f )continue;
			if( index18o[i]>trackEnd )continue;
			if( Float.isNaN( obs[index18o[i]]) )continue;
			float x = (obs[index18o[i]]-x0)*sx;
			float y = (d18o[i]-y0)*sy;
			xy.add( new float[] {obs[index18o[i]], d18o[i]} );
			rect.x = x-2.f;
			rect.y = y-2.f;
			if(highlight) g.fill(rect);
			else g.draw( rect );
		}
	}
	public String getXTitle(int dataIndex) {
		return "d18O";
	}
	public String getYTitle(int dataIndex) {
		return "depth in core";
	}
	public double[] getXRange(int dataIndex) {
		return new double[] { (double)range[0]-.5, (double)range[1]+.5};
	}
	public double[] getYRange(int dataIndex) {
	//	return new double[] { -(double)d2[d2.length-1]-.1, +.1};
		return new double[] { (double)thickness+.1, -.1};
	//	return new double[] { .1, (double)d2[d2.length-1]+.1};
	}
	public double getPreferredXScale(int dataIndex) {
		return 40.;
	}
	public double getPreferredYScale(int dataIndex) {
		return 200.;
	}
	public void plotXY( Graphics2D g, 
				Rectangle2D bounds,
				double xScale, double yScale,
				int dataIndex) {
		g.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		GeneralPath path = new GeneralPath();
		boolean connect = false;
		float x0 = (float)bounds.getX();
		float y0 = (float)bounds.getY();
		float ys = (float)yScale;
		float xs = (float)xScale;
		g.setStroke(new BasicStroke(4f));
		Line2D.Float line = new Line2D.Float(0f, 0f, xs*(float)bounds.getWidth(), 0f);
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.set( date[2], date[0]-1, date[1] );
		for(int i=1 ; i<hIce.length ; i++ ) {
			cal.add( cal.DATE, -1 );
			if( i>trackEnd )continue;
			float y = ((float)hIce[i]-y0) * ys;
			if( cal.get(cal.MONTH) == cal.SEPTEMBER && cal.get(cal.DATE)==1) {
				line.y1 = y;
				line.y2 = y;
				g.setColor( new Color(255,100,255,120) );
				g.draw( line );
				g.setColor( Color.black);
				g.drawString( "9/1/"+cal.get(cal.YEAR), 4, (int)y);
			}
			if( Float.isNaN(obs[i]) || hIce[i]==hIce[i-1] ) {
				connect=false;
				continue;
			}
			float x = (obs[i]-x0) * xs;
			if( connect ) {
				path.lineTo( x, y);
			} else {
				path.moveTo( x, y);
			}
			connect = true;
		}
		g.setColor( Color.black);
		g.setStroke(new BasicStroke(2f));
		g.draw( path);
		path = new GeneralPath();
		connect = false;
		for(int i=0 ; i<depth.length ; i++) {
			if( depth[i]<.1*thickness ) {
				connect = false;
				continue;
			}
			float x = (d18o[i]-x0) * xs;
			float y = (depth[i]-y0) * ys;
			if( connect ) {
				path.lineTo( x, y);
			} else {
				path.moveTo( x, y);
			}
			connect = true;
		}
		g.setStroke(new BasicStroke(1f));
		g.draw( path);
		Rectangle2D.Float rect = new Rectangle2D.Float(0f, 0f, 7f, 7f);
		for(int i=0 ; i<depth.length ; i++) {
			if( !name.equals("227") && depth[i]<.1*thickness ) continue;
			rect.x = -3.5f+(d18o[i]-x0) * xs;
			rect.y = -3.5f+(depth[i]-y0) * ys;
			g.setColor( getColor(.5f+d18o[i]) );
			g.fill( rect);
			g.setColor( Color.black );
			if( depth[i]<.1*thickness ) g.setColor( Color.gray );
			g.draw( rect);
		}
	}
	public static Color getColor( float o18 ) {
		if( o18<-3f ) return new Color(color[0][0], color[0][1], color[0][2]);
		float col = (o18+3f);
		int c = (int)Math.floor((double)col);
		if( c>=5 ) return new Color(color[5][0], color[5][1], color[5][2]);
		float dc = col - (float)c;
		return new Color( color[c][0]*(1f-dc) + color[c+1][0]*dc,
				color[c][1]*(1f-dc) + color[c+1][1]*dc,
				color[c][2]*(1f-dc) + color[c+1][2]*dc );
	}
	public void setHighlight( boolean tf ) {
		highlight = tf;
	}
	public void computeGrowth() {
		computeGrowth( -1., .25, .005 );
	}
	public void computeGrowth( double heatFlow, double kSnow, double meltRate ) {
		double h0 = (double)thickness;
	//	hIce = IceGrowth.zHistory(lon, lat, T, 
	//			h0, heatFlow, kSnow,
	//			date[2], date[0], date[1], meltRate);
		float[][] zHist = IceGrowth.zHistory2(lon, lat, T,
				h0, heatFlow, kSnow,
				date[2], date[0], date[1], meltRate);
		hIce = new double[zHist.length];
		hIce[0] = (double) zHist[0][1];
		for( int i=1 ; i<zHist.length-1 ; i++) {
			if( zHist[i][1] > zHist[i-1][1]) zHist[i][1]=zHist[i-1][1];
			hIce[i] = (double) zHist[i][1];
		}
		int k = 0;
		index18o = new int[depth.length];
		for( int i=depth.length-1 ; i>=0 ; i-- ) {
			while( k<hIce.length-1 && hIce[k]>depth[i]) k++;
			index18o[i] = k;
		}
	//	d18oTrack = new float[hIce.length];
	//	for( int i=1 ; i<hIce.length ; i++ ) {
	//		if( hIce[i-1]== hIce[i] ) {
	//			d18oTrack[i] = Float.NaN;
	//			continue;
	//		}
	//		while( d1[k] > hIce[i] && k>0 )k--;
	//		if( hIce[i]>=d1[k] && hIce[i]<=d2[k] ) {
	//			d18oTrack[i] = d18o[k];
	//		} else if( k<d18o.length-1 ) {
	//			if( hIce[i]>d2[k] && hIce[i]<d1[k+1] ) {
	//				d18oTrack[i] = d18o[k] + ((float)hIce[i]-d2[k]) * (d18o[k+1]-d18o[k]) / (d1[k+1]-d2[k]);
	//			} else {
	//				d18oTrack[i] = d18o[k];
	//			}
	//		} else {
	//			d18oTrack[i] = d18o[k];
	//		}
	//	}
	}
}

package haxby.map;

import haxby.proj.*;
import java.util.Vector;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

public class PolarMapBorder extends MapBorder implements Overlay {
	int lat1, lat2, latInt;
	int lon1, lon2, lonInt;
	double latMax, latMax1, latMin;
	double lonMin;
	double lonMax;
	static double[] mInt = {1, 2, 3, 5, 10, 15, 20, 30,
				60, 120, 180, 300, 600, 900, 1800 };

	public PolarMapBorder(XMap map) {
		super(map);
		lineWidth = 1;
		plotSide = new boolean[] {true, true, true, true };
		lon1 = lon2 = lat1 = lat2 = 0;
		lonInt = latInt = 1;
		latMax = latMax1 = latMin = 0;
		format();
	}
	public void format() {
		insets = new Insets(0,0,0,0);
		FontMetrics fm = map.getFontMetrics(font);
		int h = fm.getHeight();
		insets.top = lineWidth;
		if(plotSide[0]) insets.top += h;
		insets.bottom = lineWidth;
		if(plotSide[1]) insets.bottom += h;
		int w = fm.stringWidth("188\u00B0W") + 4;
		insets.left = lineWidth;
		if(plotSide[2]) insets.left += w;
		insets.right = lineWidth;
		if(plotSide[3]) insets.right += w;
	}
	public void paintBorder(Component c, Graphics graphics,
				int x, int y, int w, int h) {
		Graphics2D g = (Graphics2D) graphics;
		Insets insets = getBorderInsets(map);
		g.setColor(Color.white);
		g.fillRect(x, y, w, h);
		g.setColor(Color.black);
		g.drawRect(x, y, w, h);
		PolarProjection proj = (PolarProjection) map.getProjection();
		boolean south = (proj.getHemisphere()==proj.SOUTH);
		Rectangle2D.Double rect = (Rectangle2D.Double) map.getClipRect2D();
		g.setColor(Color.black);
		Rectangle irect = getInteriorRectangle(c, x, y, w, h);
		g.fillRect(irect.x-1, irect.y-1, irect.width+2, irect.height+2);
		g.setColor(Color.lightGray);
		g.fillRect(irect.x, irect.y, irect.width, irect.height);
		g.setColor(Color.black);
		g.translate( insets.left, insets.top );
		Point2D.Double[][] point = new Point2D.Double[2][2];
		Point2D.Double p = new Point2D.Double();
		p.x = rect.x;
		latMin = 1000;
		latMax = -1000;
		lonMin = 1000;
		lonMax = -1000;
		for( int ix=0 ; ix<2 ; ix++ ) {
			p.y = rect.y;
			for( int iy=0 ; iy<2 ; iy++ ) {
				point[ix][iy] = (Point2D.Double)proj.getRefXY(p);
				if(south) {
					point[ix][iy].x = -point[ix][iy].x;
					point[ix][iy].y = -point[ix][iy].y;
				}
				if( point[ix][iy].y > latMax ) latMax = point[ix][iy].y;
				if( point[ix][iy].y < latMin ) latMin = point[ix][iy].y;
				p.y += rect.height;
			}
			p.x += rect.width;
		}
		Point2D.Double pole = (Point2D.Double)proj.getMapXY(new Point(0,south?-90:90));
		if(pole.x < rect.x ) {
			if( pole.y < rect.y ) {
				lonMin = point[0][1].x;
				lonMax = point[1][0].x;
			} else if( pole.y <= rect.y+rect.height ) {
				lonMin = point[0][1].x;
				lonMax = point[0][0].x;
				latMax = proj.getLatitude( rect.x-pole.x );
				if(south) latMax = -latMax;
			} else {
				lonMin = point[1][1].x;
				lonMax = point[0][0].x;
			}
		} else if( pole.x <= rect.x+rect.width ) {
			if( pole.y < rect.y ) {
				lonMin = point[0][0].x;
				lonMax = point[1][0].x;
				latMax = proj.getLatitude( rect.y-pole.y );
				if(south) latMax = -latMax;
			} else if( pole.y <= rect.y+rect.height ) {
				lonMin = -180;
				lonMax = 180;
				latMax = 89.999;
			} else {
				lonMin = point[1][1].x;
				lonMax = point[0][1].x;
				latMax = proj.getLatitude( pole.y - rect.y - rect.height );
				if(south) latMax = -latMax;
			}
		} else if( pole.y < rect.y ) {
			lonMin = point[0][0].x;
			lonMax = point[1][1].x;
		} else if( pole.y <= rect.y+rect.height ) {
			lonMin = point[1][0].x;
			lonMax = point[1][1].x;
			latMax = proj.getLatitude( pole.x - rect.x - rect.width );
			if(south) latMax = -latMax;
		} else {
			lonMin = point[1][0].x;
			lonMax = point[0][1].x;
		}
		while(lonMax < lonMin) lonMax+=360;

		latInt = mInt.length - 1;
		lat1 = 0;
		lat2 = 0;
		do {
			lat1 = (int)Math.ceil(latMin * 60 / mInt[latInt]);
			lat2 = (int)Math.floor(latMax * 60 / mInt[latInt]);
			latInt--;
		} while ( ( (lat2-lat1 < 2) ) && latInt>=0);
		latInt++;

		lonInt = mInt.length - 1;
		lon1 = 0;
		lon2 = 0;
		do {
			lon1 = (int)Math.ceil(lonMin * 60 / mInt[lonInt]);
			lon2 = (int)Math.floor(lonMax * 60 / mInt[lonInt]);
			lonInt--;
		} while ( ( (lon2-lon1 < 2) ) && lonInt>=0);
		lonInt++;

		double r, r2, test, anotX, anotY;
		int deg, min;
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		String anot = ""; 
		String anotate = "";
		Vector anotBounds = new Vector();
		double zoom = map.getZoom();
		boolean ok;
		Rectangle bounds;
		for( int lat=lat1 ; lat<=lat2 ; lat++ ) {
			if(lat == 90*60) continue;
			deg = (int)Math.floor((double)lat * mInt[latInt] / 60d);
			if(lat < 0) deg++;
			min = (int)Math.abs(Math.rint( (double)(deg*60) - (double)lat*mInt[latInt]));
			if(min == 60) {
				if(lat<0)deg--;
				else deg++;
				min = 0;
			}
			if(deg < 0) deg = -deg;
			if( lat<0 ) anot = south ? "N" : "S";
			else if(lat>0) anot = south ? "S" : "N";
			else anot = "";
			anot += deg +"\u00B0";
			r = proj.getRadius( ((double)(south?-lat:lat) * mInt[latInt]) / 60);
			r2 = r*r;
			if(plotSide[0]) {
				test = r2 - Math.pow( pole.y-rect.y, 2 );
				if(test>0) {
					test = Math.sqrt(test);
					anotX = pole.x-test;
					if( anotX>rect.x && anotX<rect.x+rect.width ) {
						if(min == 0)anotate = anot;
						else anotate = anot + min + "\u00B4";
						bounds = fm.getStringBounds(anotate, g).getBounds();
						anotX *= zoom;
						anotX -= bounds.getWidth()/2;
						anotY = rect.y * zoom - 2;
						bounds.x += (int)anotX;
						bounds.y += (int)anotY;
						ok = true;
						for(int i=0 ; i<anotBounds.size() ; i++) {
							if(bounds.intersects( (Rectangle)
									anotBounds.get(i))) {
								ok = false;
								break;
							}
						}
						if(ok) {
							g.drawString(anotate, (int)anotX, (int)anotY);
							anotBounds.add(bounds);
							continue;
						}
					}
					anotX = pole.x+test;
					if( anotX>rect.x && anotX<rect.x+rect.width ) {
						if(min == 0)anotate = anot;
						else anotate = anot + min + "\u00B4";
						bounds = fm.getStringBounds(anotate, g).getBounds();
						anotX *= zoom;
						anotX -= bounds.getWidth()/2;
						anotY = rect.y * zoom - 2;
						bounds.x += (int)anotX;
						bounds.y += (int)anotY;
						ok = true;
						for(int i=0 ; i<anotBounds.size() ; i++) {
							if(bounds.intersects( (Rectangle)
									anotBounds.get(i))) {
								ok = false;
								break;
							}
						}
						if(ok) {
							g.drawString(anotate, (int)anotX, (int)anotY);
							anotBounds.add(bounds);
							continue;
						}
					}
				}
			}
			if(plotSide[3]) {
				test = r2 - Math.pow( pole.x-rect.x-rect.width, 2 );
				if(test>0) {
					test = Math.sqrt(test);
					anotY = pole.y-test;
					if( anotY>rect.y && anotY<rect.y+rect.height ) {
						anotY *= zoom;
						bounds = fm.getStringBounds(anot, g).getBounds();
						if( min==0 ) {
							anotY += (fm.getHeight() - fm.getDescent())/2;
						} else {
							bounds.height += fm.getHeight() - fm.getDescent();
							anotate = min + "\u00B4";
						}
						anotX = (rect.x+rect.width) * zoom + 2;
						bounds.x += (int)anotX;
						bounds.y += (int)anotY;
						ok = true;
						for(int i=0 ; i<anotBounds.size() ; i++) {
							if(bounds.intersects( (Rectangle)
									anotBounds.get(i))) {
								ok = false;
								break;
							}
						}
						if(ok) {
							g.drawString(anot, (int)anotX, (int)anotY);
							if(min != 0) {
								anotY += fm.getHeight() - fm.getDescent();
								g.drawString(anotate, (int)anotX, (int)anotY);
							}
							anotBounds.add(bounds);
							continue;
						}
					}
					anotY = pole.y+test;
					if( anotY>rect.y && anotY<rect.y+rect.height ) {
						anotY *= zoom;
						bounds = fm.getStringBounds(anot, g).getBounds();
						if( min==0 ) {
							anotY += (fm.getHeight() - fm.getDescent())/2;
						} else {
							bounds.height += fm.getHeight() - fm.getDescent();
							anotate = min + "\u00B4";
						}
						anotX = (rect.x+rect.width) * zoom + 2;
						bounds.x += (int)anotX;
						bounds.y += (int)anotY;
						ok = true;
						for(int i=0 ; i<anotBounds.size() ; i++) {
							if(bounds.intersects( (Rectangle)
									anotBounds.get(i))) {
								ok = false;
								break;
							}
						}
						if(ok) {
							g.drawString(anot, (int)anotX, (int)anotY);
							if(min != 0) {
								anotY += fm.getHeight() - fm.getDescent();
								g.drawString(anotate, (int)anotX, (int)anotY);
							}
							anotBounds.add(bounds);
							continue;
						}
					}
				}
			}
			if(plotSide[1]) {
				test = r2 - Math.pow( pole.y-rect.y-rect.height, 2 );
				if(test>0) {
					test = Math.sqrt(test);
					anotX = pole.x-test;
					if( anotX>rect.x && anotX<rect.x+rect.width ) {
						if(min == 0)anotate = anot;
						else anotate = anot + min + "\u00B4";
						bounds = fm.getStringBounds(anotate, g).getBounds();
						anotX *= zoom;
						anotX -= bounds.getWidth()/2;
						anotY = (rect.y+rect.height) * zoom +fm.getHeight()
										- fm.getDescent();
						bounds.x += (int)anotX;
						bounds.y += (int)anotY;
						ok = true;
						for(int i=0 ; i<anotBounds.size() ; i++) {
							if(bounds.intersects( (Rectangle)
									anotBounds.get(i))) {
								ok = false;
								break;
							}
						}
						if(ok) {
							g.drawString(anotate, (int)anotX, (int)anotY);
							anotBounds.add(bounds);
							continue;
						}
					}
					anotX = pole.x+test;
					if( anotX>rect.x && anotX<rect.x+rect.width ) {
						if(min == 0)anotate = anot;
						else anotate = anot + min + "\u00B4";
						bounds = fm.getStringBounds(anotate, g).getBounds();
						anotX *= zoom;
						anotX -= bounds.getWidth()/2;
						anotY = (rect.y+rect.height) * zoom + fm.getHeight()
										- fm.getDescent();
						bounds.x += (int)anotX;
						bounds.y += (int)anotY;
						ok = true;
						for(int i=0 ; i<anotBounds.size() ; i++) {
							if(bounds.intersects( (Rectangle)
									anotBounds.get(i))) {
								ok = false;
								break;
							}
						}
						if(ok) {
							g.drawString(anotate, (int)anotX, (int)anotY);
							anotBounds.add(bounds);
							continue;
						}
					}
				}
			}
			if(plotSide[2]) {
				test = r2 - Math.pow( pole.x-rect.x, 2 );
				if(test>0) {
					test = Math.sqrt(test);
					anotY = pole.y-test;
					if( anotY>rect.y && anotY<rect.y+rect.height ) {
						anotY *= zoom;
						bounds = fm.getStringBounds(anot, g).getBounds();
						if( min==0 ) {
							anotY += (fm.getHeight() - fm.getDescent())/2;
						} else {
							bounds.height += fm.getHeight() - fm.getDescent();
							anotate = min + "\u00B4";
						}
						anotX = rect.x * zoom - bounds.getWidth() - 2;
						bounds.x += (int)anotX;
						bounds.y += (int)anotY;
						ok = true;
						for(int i=0 ; i<anotBounds.size() ; i++) {
							if(bounds.intersects( (Rectangle)
									anotBounds.get(i))) {
								ok = false;
								break;
							}
						}
						if(ok) {
							g.drawString(anot, (int)anotX, (int)anotY);
							if(min != 0) {
								anotX = rect.x * zoom - fm.stringWidth(anotate) - 2;
								anotY += fm.getHeight() - fm.getDescent();
								g.drawString(anotate, (int)anotX, (int)anotY);
							}
							anotBounds.add(bounds);
							continue;
						}
					}
					anotY = pole.y+test;
					if( anotY>rect.y && anotY<rect.y+rect.height ) {
						anotY *= zoom;
						bounds = fm.getStringBounds(anot, g).getBounds();
						if( min==0 ) {
							anotY += (fm.getHeight() - fm.getDescent())/2;
						} else {
							bounds.height += fm.getHeight() - fm.getDescent();
							anotate = min + "\u00B4";
						}
						anotX = rect.x * zoom - bounds.getWidth() - 2;
						bounds.x += (int)anotX;
						bounds.y += (int)anotY;
						ok = true;
						for(int i=0 ; i<anotBounds.size() ; i++) {
							if(bounds.intersects( (Rectangle)
									anotBounds.get(i))) {
								ok = false;
								break;
							}
						}
						if(ok) {
							g.drawString(anot, (int)anotX, (int)anotY);
							if(min != 0) {
								anotX = rect.x * zoom - fm.stringWidth(anotate) - 2;
								anotY += fm.getHeight() - fm.getDescent();
								g.drawString(anotate, (int)anotX, (int)anotY);
							}
							anotBounds.add(bounds);
							continue;
						}
					}
				}
			}
		}
		if( 90-latMin > 4*(90-latMax) ) {
			latMax1 = lat2 *mInt[latInt]/60;
		} else {
			latMax1 = latMax;
		}
		double longitude, latitude;
		Point2D.Double p1,p2;
		double xx, yy;
		for( int lon=lon1 ; lon<=lon2 ; lon++ ) {
			longitude = (double)lon * mInt[lonInt] / 60d;
			if(south) longitude = -longitude;
			while(longitude < -180) longitude+=360;
			while(longitude > 180) longitude-=360;
			deg = (int)Math.floor(Math.abs(longitude));
		//	if(longitude < 0) deg++;
			min = (int)Math.abs(Math.rint( ((double)deg - Math.abs(longitude)) * 60));
			if(min == 60) {
			//	if(lon<0)deg--;
			//	else deg++;
				deg++;
				min = 0;
			}
			if(deg < 0) deg = -deg;
			if( longitude<0 ) anot = "W";
			else if(longitude>0) anot = "E";
			else anot = "";
			anot += deg +"\u00B0";
			if( Math.rint(longitude/90) * 90 == longitude) {
				latitude = south?-latMax:latMax;
			} else {
				latitude = south?-latMax1:latMax1;
			}
			p1 = (Point2D.Double)proj.getMapXY(new Point2D.Double(longitude, latitude));
			latitude = south?-latMin:latMin;
			p2 = (Point2D.Double)proj.getMapXY(new Point2D.Double(longitude, latitude));
			if(plotSide[2]) {
				anotX = rect.x;
				if( (p1.x-anotX)*(p2.x-anotX) <= 0 && p2.x != p1.x) {
					anotY = p1.y + (p2.y-p1.y)*(anotX-p1.x) / (p2.x-p1.x);
					if( anotY>rect.y && anotY < rect.y+rect.height ) {
						anotY *= zoom;
						bounds = fm.getStringBounds(anot, g).getBounds();
						if( min==0 ) {
							anotY += (fm.getHeight() - fm.getDescent())/2;
						} else {
							bounds.height += fm.getHeight() - fm.getDescent();
							anotate = min + "\u00B4";
						}
						anotX = rect.x * zoom - bounds.getWidth() - 2;
						bounds.x += (int)anotX;
						bounds.y += (int)anotY;
						ok = true;
						for(int i=0 ; i<anotBounds.size() ; i++) {
							if(bounds.intersects( (Rectangle)
									anotBounds.get(i))) {
								ok = false;
								break;
							}
						}
						if(ok) {
							g.drawString(anot, (int)anotX, (int)anotY);
							if(min != 0) {
								anotX = rect.x * zoom - fm.stringWidth(anotate) - 2;
								anotY += fm.getHeight() - fm.getDescent();
								g.drawString(anotate, (int)anotX, (int)anotY);
							}
							anotBounds.add(bounds);
							continue;
						}
					}
				}
			}
			if(plotSide[1]) {
				anotY = rect.y + rect.height;
				if((p1.y-anotY)*(p2.y-anotY) <= 0 && p1.y!=p2.y) {
					anotX = p1.x + (p2.x-p1.x)*(anotY-p1.y) / (p2.y-p1.y);
					if( anotX>rect.x && anotX<rect.x+rect.width ) {
						if(min == 0)anotate = anot;
						else anotate = anot + min + "\u00B4";
						bounds = fm.getStringBounds(anotate, g).getBounds();
						anotX *= zoom;
						anotX -= bounds.getWidth()/2;
						anotY = anotY * zoom +fm.getHeight()
										- fm.getDescent();
						bounds.x += (int)anotX;
						bounds.y += (int)anotY;
						ok = true;
						for(int i=0 ; i<anotBounds.size() ; i++) {
							if(bounds.intersects( (Rectangle)
									anotBounds.get(i))) {
								ok = false;
								break;
							}
						}
						if(ok) {
							g.drawString(anotate, (int)anotX, (int)anotY);
							anotBounds.add(bounds);
							continue;
						}
					}
				}
			}
			if(plotSide[3]) {
				anotX = rect.x+rect.width;
				if( (p1.x-anotX)*(p2.x-anotX) <= 0 && p2.x != p1.x) {
					anotY = p1.y + (p2.y-p1.y)*(anotX-p1.x) / (p2.x-p1.x);
					if( anotY>rect.y && anotY < rect.y+rect.height ) {
						anotY *= zoom;
						bounds = fm.getStringBounds(anot, g).getBounds();
						if( min==0 ) {
							anotY += (fm.getHeight() - fm.getDescent())/2;
						} else {
							bounds.height += fm.getHeight() - fm.getDescent();
							anotate = min + "\u00B4";
						}
						anotX = (rect.x+rect.width) * zoom + 2;
						bounds.x += (int)anotX;
						bounds.y += (int)anotY;
						ok = true;
						for(int i=0 ; i<anotBounds.size() ; i++) {
							if(bounds.intersects( (Rectangle)
									anotBounds.get(i))) {
								ok = false;
								break;
							}
						}
						if(ok) {
							g.drawString(anot, (int)anotX, (int)anotY);
							if(min != 0) {
								anotY += fm.getHeight() - fm.getDescent();
								g.drawString(anotate, (int)anotX, (int)anotY);
							}
							anotBounds.add(bounds);
							continue;
						}
					}
				}
			}
			if(plotSide[0]) {
				anotY = rect.y;
				if((p1.y-anotY)*(p2.y-anotY) <= 0 && p1.y!=p2.y) {
					anotX = p1.x + (p2.x-p1.x)*(anotY-p1.y) / (p2.y-p1.y);
					if( anotX>rect.x && anotX<rect.x+rect.width ) {
						if(min == 0)anotate = anot;
						else anotate = anot + min + "\u00B4";
						bounds = fm.getStringBounds(anotate, g).getBounds();
						anotX *= zoom;
						anotX -= bounds.getWidth()/2;
						anotY = rect.y * zoom - 2;
						bounds.x += (int)anotX;
						bounds.y += (int)anotY;
						ok = true;
						for(int i=0 ; i<anotBounds.size() ; i++) {
							if(bounds.intersects( (Rectangle)
									anotBounds.get(i))) {
								ok = false;
								break;
							}
						}
						if(ok) {
							g.drawString(anotate, (int)anotX, (int)anotY);
							anotBounds.add(bounds);
							continue;
						}
					}
				}
			}
		}
		g.translate( -insets.left, -insets.top );
	}
	boolean plotGrid = true;
	public void setPlotGrid(boolean tf) {
		plotGrid = tf;
	}
	GeneralPath getParallel( double lat ) {
		double dx = (lonMax-lonMin)/180.;
		PolarProjection proj = (PolarProjection) map.getProjection();
		GeneralPath path = new GeneralPath();
		Point2D.Double p0 = new Point2D.Double( lonMin, lat) ;
		Point2D p = proj.getMapXY( p0 );
		path.moveTo( (float)p.getX(), (float)p.getY());
		int i1 = (int) Math.ceil( lonMin/dx );
		int i2 = (int) Math.floor( lonMax/dx );
		for( int i=i1 ; i<i2 ; i++) {
			p0.x = i*dx;
			p = proj.getMapXY( p0 );
			path.lineTo( (float)p.getX(), (float)p.getY());
		}
		if( lonMax-lonMin == 360. ) path.closePath();
		else {
			p0.x = lonMax;
			p = proj.getMapXY( p0 );
			path.lineTo( (float)p.getX(), (float)p.getY());
		}
		return path;
	}
	public void draw(Graphics2D g) {
		PolarProjection proj = (PolarProjection) map.getProjection();
		boolean south = (proj.getHemisphere()==proj.SOUTH);
		Point2D.Double pole = (Point2D.Double)proj.getMapXY(new Point(0,south?-90:90));
		double x, y;
		g.setColor(Color.black);
		float zoom = (float)map.getZoom();
		Stroke stroke = g.getStroke();
		BasicStroke stroke1 = new BasicStroke(1f/zoom,
				BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_ROUND,
				1f/zoom,
				new float[] {2f/zoom, 4f/zoom},
				0f);
		g.setStroke(stroke1);
		Arc2D.Double arc;
		Line2D.Double line;
		Point2D p1, p2;
		if( south ) {
			double tmp = lonMin;
			lonMin = -lonMax;
			lonMax = -tmp;
		}
		for( int lat=lat1 ; lat<=lat2 ; lat++) {
			if(lat == 90*60/mInt[latInt] ) continue;
			x = (double)(south?-lat:lat) * mInt[latInt]/60;
			g.draw( getParallel(x) );
		//	x = proj.getRadius( x );
		//	if( lonMax-lonMin==360. ) {
		//		arc = new Arc2D.Double( pole.x-x, pole.y-x, 2*x,2*x, lonMin, lonMax, Arc2D.CHORD);
		//	} else {
		//		arc = new Arc2D.Double( pole.x-x, pole.y-x, 2*x,2*x, lonMin, lonMax, Arc2D.OPEN);
		//	}
		//	g.draw(arc);
		}
		for( int lon=lon1 ; lon<=lon2 ; lon++) {
			x = (double)(south?-lon:lon) * mInt[lonInt]/60;
			if( Math.rint(x/90) * 90 == x) {
				y = south?-latMax:latMax;
			} else {
				y = south?-latMax1:latMax1;
			}
		//	y = south?-latMax:latMax;
			p1 = proj.getMapXY( new Point2D.Double(x, y));
			y = south?-latMin:latMin;
			p2 = proj.getMapXY( new Point2D.Double(x, y));
			line = new Line2D.Double(p1,p2);
			g.draw(line);
		}
		g.setStroke(stroke);
	}
}

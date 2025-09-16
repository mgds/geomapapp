package haxby.db.surveyplanner;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import haxby.map.Overlay;
import haxby.map.XMap;
import haxby.proj.Projection;
import haxby.util.GeneralUtils;

public class SurveyLine implements Overlay {
	private double startLat, startLon, endLat, endLon, startElevation, endElevation, cumulativeDistance, duration;
	private int lineNum;
	private static int counter = 0;
	private XMap map;
	private static double totalDistance = 0;
	private static double speed = 0;
	private static boolean isStraightLine = false;
	private boolean selected;
	String selectedPoint = "none";
	
	public SurveyLine(XMap map) {
		this(map, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
	}
	
	public SurveyLine(XMap map, double startLat, double startLon, double endLat, double endLon) {
		this(map, startLat, startLon, Double.NaN, endLat, endLon, Double.NaN);
	}
	
	public SurveyLine (XMap map, double startLat, double startLon, double startElevation, 
			double endLat, double endLon, double endElevation) {
		this.map = map;
		this.startLat = startLat;
		this.startLon = startLon;
		this.startElevation = startElevation;
		this.endLat = endLat;
		this.endLon = endLon;
		this.endElevation = endElevation;
		this.lineNum = ++counter;
		this.cumulativeDistance = calculateCumulativeDistance();
		this.duration = calculateDuration();
		this.selected = false;
	}
	
	public void setElevations (double startElevation, double endElevation) {
		setStartElevation(startElevation);
		setEndElevation(endElevation);
	}

	public double getGapFromElevations(float swathAngle, float overlap) {
		double meanElevation = (startElevation + endElevation) /-2000d;
		//calculate line spacing based on swath angle and elevation
		double gap = (double)Math.round((2d * meanElevation * Math.tan(Math.toRadians(swathAngle / 2d))) * 1000d) / 1000d;
		//take in to account overlap
		gap = gap * (100 - overlap)/100d;
		//round to 3dp
		gap = (double)Math.round(gap * 1000d)/1000d;
		return gap;
	}

	public double calculateCumulativeDistance() {
		Point2D[] pts;
		
		if (isStraightLine) {
			Line2D line = getLine();
			Projection proj = map.getProjection();
			Point2D p1 = line.getP1();
			Point2D p2 = line.getP2();

			double dist = map.getZoom()*Math.sqrt(
					Math.pow( p1.getX()-p2.getX(),2 ) +
					Math.pow( p1.getY()-p2.getY(),2 )); 
			int npt = (int)Math.ceil(dist);
			if (npt < 100) npt = 100;
						
			double dx = (p2.getX()-p1.getX())/(npt-1.);
			double dy = (p2.getY()-p1.getY())/(npt-1.);
			Point2D thisP;
			pts = new Point2D[npt];
			//generate the interpolated points, convert to lat/lon and add to the pts array
			for(int k=0 ; k<npt ; k++) {
				thisP = new Point2D.Double(
					p1.getX() + k*dx,
					p1.getY() + k*dy);
				pts[k] = proj.getRefXY(thisP);
			}
		} else {
			Point2D lineStart = getStartPoint();
			Point2D lineEnd = getEndPoint();
			pts = new Point2D[] {lineStart, lineEnd};
		}
		
		totalDistance += GeneralUtils.cumulativeDistance(pts);	
		return totalDistance;
	}

	public double calculateDuration() {
		if (speed == 0) return 0;
		duration = cumulativeDistance / (speed * GeneralUtils.KNOTS_2_KPH);
		return (double)Math.round(duration * 100d) / 100d;
	}
	
	public int getCumulativeDistance() {
		return (int) cumulativeDistance;
	}
	
	public double getDuration() {
		return duration;
	}
	
	public double getStartLat() {
		return startLat;
	}
	
	public double getStartLon() {
		return startLon;
	}
	
	public String getStartElevation() {
		return Double.toString(startElevation);
	}
	
	public double getEndLat() {
		return endLat;
	}
	
	public double getEndLon() {
		return endLon;
	}
	
	public String getEndElevation() {
		return Double.toString(endElevation);
	}
	
	public int getLineNum() {
		return lineNum;
	}
	
	public static double getSpeed() {
		return speed;
	}
	
	public void setStartLat(double startLat) {
		this.startLat = startLat;
	}
	
	public void setStartLon(double startLon) {
		this.startLon = startLon;
	}
	
	public void setStartElevation(double startElevation) {
		this.startElevation = startElevation;
	}
	
	public void setEndLat(double endLat) {
		this.endLat = endLat;
	}
	
	public void setEndLon(double endLon) {
		this.endLon = endLon;
	}
	
	public void setEndElevation (double endElevation) {
		this.endElevation = endElevation;
	}

	public void setLineNum (int lineNum) {
		this.lineNum = lineNum;
	}
	
	public void setCumulativeDistance (double distance) {
		this.cumulativeDistance = distance;
	}
	
	public void setDuration(double duration) {
		this.duration = duration;
	}
	
	public static void setSpeed(double s) {
		speed = s;
	}
	
	public static void setIsStraightLine(boolean tf) {
		isStraightLine = tf;
	}

	public static boolean getIsStraightLine() {
		return isStraightLine;
	}
	
	public static void resetCounter() {
		counter = 0;
	}
	
	public static void resetTotalDistance() {
		totalDistance = 0;
	}
	
	public static void resetAll() {
		resetCounter();
		resetTotalDistance();
		speed = 0d;
	}
	
	public Point2D.Double getStartPoint() {
		return new Point2D.Double(startLon, startLat);
	}
	
	public Point2D getEndPoint() {
		return new Point2D.Double(endLon, endLat);
	}
	
	public Line2D getLine() {
		float wrap = (float)map.getWrap();
		
		//get the limits of the displayed map
		Rectangle2D rect = map.getClipRect2D();
		double xmin = rect.getMinX();
		double xmax = rect.getMaxX();
		
		Projection proj = map.getProjection();
		Point2D.Double pt = new Point2D.Double();
		pt.x = getStartLon();
		pt.y = getStartLat();
		Point2D.Double p_start = (Point2D.Double) proj.getMapXY(pt);

		if( wrap>0f ) {
			while( p_start.x + wrap <= xmax ){p_start.x+=wrap;}
			while( p_start.x - wrap >= xmin ){p_start.x-=wrap;}
		}
		
		pt.x = getEndLon();
		pt.y = getEndLat();
		Point2D.Double p_end = (Point2D.Double) proj.getMapXY(pt);

		if( wrap>0f ) {
			while( p_end.x + wrap <= xmax ){p_end.x+=wrap;}
			while( p_end.x - wrap >= xmin ){p_end.x-=wrap;}
		}
				
		//draw the shortest line - either p_start.x to p_end.x or the x+wrap values.
		if ( ((p_start.x - p_end.x) * (p_start.x - p_end.x)) > 
			((p_start.x - (p_end.x + wrap)) * (p_start.x - (p_end.x + wrap))) )  {p_end.x += wrap;}
		if ( ((p_start.x - p_end.x) * (p_start.x - p_end.x)) > 
			(((p_start.x + wrap) - p_end.x) * ((p_start.x + wrap) - p_end.x)) )  {p_start.x += wrap;}
		
        return new Line2D.Double(p_start, p_end);
	}
	
	@Override
	public void draw(Graphics2D g) {

		float wrap = (float)map.getWrap();
		Projection proj = map.getProjection();
		
        Line2D line = getLine();
        Point2D.Double p_start = (Point2D.Double) line.getP1();
        Point2D.Double p_end = (Point2D.Double) line.getP2();
		double sqStartX = p_start.x;
        double sqEndX = p_end.x;
        
		//get the limits of the displayed map
		Rectangle2D rect = map.getClipRect2D();
		double xmin = rect.getMinX();
		double xmax = rect.getMaxX();
        
		double zoom = map.getZoom();
		g.setStroke( new BasicStroke( 2f/(float)zoom ));

    	double arr_size = 6./zoom;
    	double sq_size = 6./zoom;
        double dx = p_end.x - p_start.x, dy = p_end.y - p_start.y;
        double angle = Math.atan2(dy, dx);

        double len = Math.sqrt(dx*dx + dy*dy);

        AffineTransform at = g.getTransform(); 
        GeneralPath path = null;
        Point2D[] pts = null;
        if (!isStraightLine) {
        	//for great circle, get the General Path
        	pts = SurveyPlannerSelector.getPath(line, map);
			path =  SurveyPlannerSelector.getGeneralPath(pts, map);
        }
        double min, max;
		if (path != null) {
			Rectangle pathBounds = path.getBounds();
			min = pathBounds.getMinX();
			max = pathBounds.getMaxX();
		} else {
			min = Math.min(p_start.x, p_end.x);
			max = Math.max(p_start.x, p_end.x);
		}
        
        //draw the line
		double offset = 0.;
		while( min + offset > xmin ) offset -= wrap;
		while( max + offset < xmin ) offset += wrap;
		double initialOffset = offset;
		g.translate( offset, 0.);
     
        Rectangle2D startSq = new Rectangle2D.Double(sqStartX-sq_size/2d - offset, p_start.getY()-sq_size/2d, sq_size, sq_size);
        Rectangle2D endSq = new Rectangle2D.Double(sqEndX-sq_size/2d - offset, p_end.getY()-sq_size/2d,  sq_size, sq_size);
		
		while( min + offset < xmax ) {
			if (selected) g.setColor(Color.WHITE);
			else {
				if (lineNum == 1) g.setColor( Color.RED ); 
				else g.setColor( Color.black );
			}
			if (isStraightLine) {
				g.draw(line);
			}
			else {
				if (max + offset < xmin) {
					g.translate(wrap, 0.);
					g.draw(path);
					g.translate(-wrap, 0.);
				}
				else if (min + offset > xmax) {
					g.translate(-wrap, 0.);
					g.draw(path);
					g.translate(+wrap, 0.);
				} else {
					g.draw(path);
				}
			}

			//draw squares at the end points 
	        g.fill(startSq);
	        g.fill(endSq);
	        
	        //draw the arrow
	        Path2D arrow = new Path2D.Double();
	        double[] xpts = {0, 0-arr_size, 0-arr_size, 0};
	        double[] ypts = {0, 0-arr_size, 0+arr_size, 0};	
	        double deltax = p_start.getX() + len/3d * Math.cos(angle);//p_start.getX() + len/3d;
	        double deltay = p_start.getY() + len/3d * Math.sin(angle);//p_start.getY();
	        
	        if (!isStraightLine && pts != null) {
	        	//find the 1/3 point along the great circle
	        	int third = (int) pts.length/3;
				Point2D pt3 = proj.getMapXY(pts[third]);
	        	deltax = pt3.getX();
	        	deltay = pt3.getY();
	    		if( wrap>0f ) {
	    			while( deltax + wrap <= xmax ){deltax+=wrap;}
	    			while( deltax - wrap >= xmin ){deltax-=wrap;}
	    		}
	    		deltax -= initialOffset;
	        	//get rotation angle for arrow
	        	Point2D.Double p_before = (Point2D.Double) proj.getMapXY(pts[third-1]);
	        	Point2D.Double p_after = (Point2D.Double) proj.getMapXY(pts[third+1]);
	            dx = p_after.x - p_before.x;
	            dy = p_after.y - p_before.y;
	            angle = Math.atan2(dy, dx);
				if (max + offset < xmin) {
					g.translate(wrap, 0.);
				}
				else if (min + offset > xmax) {
					g.translate(-wrap, 0.);
				}
	        } 

	        g.translate(deltax, deltay);
	        g.rotate(angle);
	        arrow.moveTo(xpts[0], ypts[0]);
	        arrow.lineTo(xpts[1], ypts[1]);
	        arrow.lineTo(xpts[2], ypts[2]);
	        arrow.closePath();
			if (lineNum == 1) g.setColor( Color.ORANGE ); 
			else g.setColor( Color.ORANGE );
	        g.fill(arrow);

			offset += wrap;
			g.setTransform(at);
			g.translate( offset, 0.);
		}
		g.setTransform(at);
	}
	
	boolean select(double x, double y) {
		Point2D.Double p0 = (Point2D.Double) map.getProjection().getMapXY(new Point2D.Double(startLon, startLat));
		Point2D.Double p1 = (Point2D.Double) map.getProjection().getMapXY(new Point2D.Double(endLon, endLat));
		Point2D points[] = {p0, p1};
		Rectangle2D.Double bounds = (Rectangle2D.Double)computeBounds(points);
		double r = 2.5/map.getZoom();
		bounds.x -= r;
		bounds.y -= r;
		bounds.width += 2.*r;
		bounds.height += 2.*r;
		double wrap = map.getWrap();
		if( wrap>0. ) {
			while( bounds.x>x ) x += wrap;
			while( bounds.x+bounds.width<x ) x -= wrap;
		}
		if( !bounds.contains( x, y ) )return false;
		double x1, y1, x2, y2, dx0, dy0, dx, dy, r0, r1, test;;

		x1 = p0.getX();
		y1 = p0.getY();
		double rMin = 100000.;
		r *= r;
		for( int i=1 ; i<points.length ; i++) {
			Point2D p = points[i];
			x2 = p.getX();
			y2 = p.getY();
			if(x1==x2 && y1==y2) continue;
			dx0 = x2-x1;
			dy0 = y2-y1;
			dx = x-x1;
			dy = y-y1;
			r0 = dx0*dx0 + dy0*dy0;
			test = dx*dx0 + dy*dy0;
			x1 = x2;
			y1 = y2;
			if(test<0) {
				r1 = dx*dx + dy*dy;
			} else if( test>r0 ) {
				dx -= dx0;
				dy -= dy0;
				r1 = dx*dx + dy*dy;
			} else {
				r1 = -dx*dy0 + dy*dx0;
				r1 *= r1/r0;
			}
			if( r1<rMin ) rMin = r1;
			if( r1>r ) continue;
			return true;
		}
		return false;
	}
	
	Rectangle2D computeBounds(Point2D[] points) {
		Rectangle2D.Double bounds = new Rectangle2D.Double();
		if( points.length==0 ) return bounds;
		bounds.x = points[0].getX();
		bounds.y = points[0].getY();
		for( int i=1 ; i<points.length ; i++ ) {
			Point2D p = points[i];
			if( p.getX()<bounds.x ) {
				bounds.width += bounds.x-p.getX();
				bounds.x = p.getX();
			} else if( p.getX()>bounds.x+bounds.width ) {
				bounds.width = p.getX()-bounds.x;
			}
			if( p.getY()<bounds.y ) {
				bounds.height += bounds.y-p.getY();
				bounds.y = p.getY();
			} else if( p.getY()>bounds.y+bounds.height ) {
				bounds.height = p.getY()-bounds.y;
			}
		}
		return bounds;
	}
	
	boolean isSelected() {
		return selected;
	}
	
	void setSelected( boolean tf ) {
		selected = tf;
	}
	
	boolean selectPointInLine(Point2D.Double p) {
		//return whether the clicked point is the start or end point of the survey line 
		selectedPoint = "none";
		if (Double.isNaN(startLon) || Double.isNaN(startLat)) return false;
		return(selectStartPoint(p) || selectEndPoint(p));
	}
	
	boolean selectStartPoint(Point2D.Double p) {
		Point2D.Double p_start = (Point2D.Double) map.getProjection().getMapXY(new Point2D.Double(startLon, startLat));
		if (selectPoint(p, p_start)) {
			selectedPoint = "start";
			return true;
		}
		return false;
	}
	
	boolean selectEndPoint(Point2D.Double p) {
		Point2D.Double p_end = (Point2D.Double) map.getProjection().getMapXY(new Point2D.Double(endLon, endLat));
		if (selectPoint(p, p_end)) {
			selectedPoint = "end";
			return true;
		}
		return false;
	}
	
	boolean selectPoint(Point2D.Double p_m, Point2D.Double p_sl) {
		//check in the mouse point is close to the start or end point of the line
		double zoom = map.getZoom();
		double r = 5/zoom;
		double wrap = map.getWrap();
		if(p_m.getY() < p_sl.getY() - r || p_m.getY() > p_sl.getY() + r ) return false;
		
		if( wrap>0. ) {
			while(p_m.getX() < p_sl.getX() - wrap/2.){
				p_m.x +=wrap;
			}
			while(p_m.getX() > p_sl.getX() + wrap/2.){
				p_m.x-=wrap;
			}
		}
		if(p_m.getX() < p_sl.getX() - r || p_m.getX() > p_sl.getX() + r) return false;
		return true;
	}
	
	void updateSelectedPoint(Point2D.Double p) {
		//update the start or end lat/lon based on input point
		Point2D lonlat = map.getProjection().getRefXY(p);
		switch (selectedPoint) {
			case "start": {
				startLon = lonlat.getX();
				startLat = lonlat.getY();
				break;
			}
			case "end": {
				endLon = lonlat.getX();
				endLat = lonlat.getY();
				break;
			}
		}
		draw(map.getGraphics2D());
	}
}

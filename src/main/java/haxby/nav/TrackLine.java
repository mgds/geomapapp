package haxby.nav;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class TrackLine{
	String name;
	Rectangle2D bounds;
	ControlPt[][] cpts;
	int start, end;
	GeneralPath returnPath;

//	GMA 1.4.8: Changed "mask" from byte to int to make "types" in MGG work correctly
//	byte mask;
	int mask;
	int wrap;
	int offset = 0;
	public TrackLine(String name, 
			Rectangle2D bounds, 
			ControlPt[][] cpts, 
			int start,
			int end,
			byte dataMask,
			int wrap) {
		this.name = name;
		this.bounds = bounds;
		this.cpts = cpts;
		this.start = start;
		this.end = end;
		mask = dataMask;
		this.wrap = wrap;
	}
	public int getWrap() {
		return wrap;
	}
	public String toString() {
		return name;
	}
	public String getName() {
		return name;
	}
	public int getStart() {
		return start;
	}
	public int getEnd() {
		return end;
	}

//	GMA 1.4.8: Changed "mask" from byte to int to make "types" in MGG work correctly	
//	public byte getTypes() {
	public int getTypes() {
		return mask;
	}

	// returns true if any point in the track line is within the area, or if the line intersects the area
	public boolean intersects( Rectangle2D area ) {
		
		if (name.contains("header only"))
			return true;
		
		float xMin = (float)area.getX();
		float xMax = (float)(area.getX() + area.getWidth());

		float offset = (float)this.offset;

		for( int seg=0 ; seg<cpts.length ; seg++ ) {
			for(int i=0 ; i<cpts[seg].length ; i++) {
				float x = offset+(float)cpts[seg][i].getX();
				float y = (float)cpts[seg][i].getY();
				
				float prevX = (i > 0) ? offset+(float)cpts[seg][i-1].getX() : Float.NaN;
				float prevY = (i > 0) ? (float)cpts[seg][i-1].getY() : Float.NaN;;
				float nextX = (i < cpts[seg].length - 1) ? offset+(float)cpts[seg][i+1].getX() : Float.NaN;
				float nextY = (i < cpts[seg].length - 1) ? (float)cpts[seg][i+1].getY() : Float.NaN;
				
				//some fiddling around to make sure all points are in the correct wrap segment
				if (wrap>0f){

					if (x < xMin && x + wrap < xMax) {
						x += wrap;
					} else if (x > xMax && x - wrap > xMin) {
						x -= wrap;
					}
				
					if( !Float.isNaN(prevX)) {
						while( x-prevX < wrap/2f ){x+=wrap;}
						while( x-prevX > wrap/2f ){x-=wrap;}
					} 
							
					if ( !Float.isNaN(nextX)) {
						if (nextX < xMin && nextX + wrap < xMax) {
							nextX += wrap;
						} else if (nextX > xMax && nextX - wrap > xMin) {
							nextX -= wrap;
						}
					}
				}

				//only draw tracks if a cpt is in the visible map, or if the track 
				//intersects the map
				if (area.contains(x, y) ||
				   (!Float.isNaN(prevX) && area.intersectsLine(prevX, prevY, x, y)) ||
				   (!Float.isNaN(nextX) && area.intersectsLine(x, y, nextX, nextY))) {
					return(true);
				}
			}
		}
				
		return false;				
	}
	
	//previous version that uses intersection of bounds, rather than the track
	public boolean intersectsOld( Rectangle2D area ) {		
		if(wrap>0) {
			if(area.getHeight()>=0) {
				if(area.getY()+area.getHeight()<bounds.getY()) return false;
				if(bounds.getY()+bounds.getHeight()<area.getY()) return false;
			} else {
				if(area.getY()<bounds.getY()) return false;
				if(bounds.getY()+bounds.getHeight()<area.getY()+area.getHeight()) return false;
			}
			while( bounds.getX()+(double)offset > area.getX() ) offset -= wrap;
			while( bounds.getX()+bounds.getWidth()+(double)offset < 
					area.getX() ) offset += wrap;
			if( bounds.getX()+(double)offset > area.getX()+area.getWidth() ) return false;
			return true;
			
		} else {
			return bounds.intersects(area.getX(), area.getY(), 
					area.getWidth(), area.getHeight());
		}
	}
	public boolean contains( double x, double y ) {
		if(wrap>0) {
			if( y<bounds.getY() || 
				y>bounds.getY()+bounds.getHeight() )return false;
			while(x<bounds.getX()) x+=wrap;
			while(x>bounds.getX()+bounds.getWidth()) x-=wrap;
			return (x>bounds.getX());
		} else {
			return bounds.contains(x, y);
		}
	}
	public Rectangle2D getBounds() {
		return new Rectangle2D.Double( bounds.getX(),
			bounds.getY(), bounds.getWidth(), bounds.getHeight());
	}

	public ControlPt[][] getCpts() {
		return cpts;
	}

	public void draw(Graphics2D g) {
		Rectangle area = g.getClipBounds();
		if( !intersects(area) ) return;
		GeneralPath path = new GeneralPath();
		float offset = (float)this.offset;
		for( int seg=0 ; seg<cpts.length ; seg++ ) {
			path.moveTo( offset+(float)cpts[seg][0].getX(), (float)cpts[seg][0].getY() );
			for(int i=1 ; i<cpts[seg].length ; i++) {
				path.lineTo( offset+(float)cpts[seg][i].getX(), (float)cpts[seg][i].getY() );
			}
		}
		g.draw(path);
		if(wrap>0) {
			AffineTransform xform = g.getTransform();
			offset += (float)wrap;
			while( bounds.getX()+(double)offset < area.getX()+area.getWidth() ) {
				g.translate( (double)wrap, 0.d );
				g.draw(path);
				offset += (float)wrap;
			}
			g.setTransform( xform );
		}
		returnPath = path;
	}
	
	public GeneralPath getGeneralPath(){
		return returnPath;
	}
	public long getTime( Nearest nearest) {
		int i = (int)nearest.x;
		try {
			long t1 = ((TimeControlPt)cpts[nearest.seg][i]).getTimeInMillis();
			double dx = nearest.x-i;
			if( i==cpts[nearest.seg].length-1 || dx==0.) return t1;
			long t2 = ((TimeControlPt)cpts[nearest.seg][i+1]).getTimeInMillis();
			return t1 +(long)(dx*(t2-t1));
		} catch( ClassCastException ex) {
			return -1;
		}
	}
	public GeneralPath getPath( int t1, int t2 ) {
		float x, y;
		double dt;
		GeneralPath path = new GeneralPath();
		if( t2<start || t1>end ) return path;
		TimeControlPt p1, p2;
		try {
			for( int seg=0 ; seg<cpts.length ; seg++ ) {
				boolean connect=false;
				for( int i=0 ; i<cpts[seg].length-1 ; i++) {
					p2 = (TimeControlPt)cpts[seg][i+1];
					if( p2.time<=t1 ) continue;
					p1 = (TimeControlPt)cpts[seg][i];
					if( p1.time>t2 ) return path;
					if( p1.time<=t1 ) {
						dt = (double) (t1-p1.time) / (double)(p2.time-p1.time);
						x = (float)( p1.getX()+dt*(p2.getX()-p1.getX()) );
						y = (float)( p1.getY()+dt*(p2.getY()-p1.getY()) );
						path.moveTo( x, y);
						connect = true;
						if( p2.time>t2 ) {
							dt = (double) (t2-p1.time) / (double)(p2.time-p1.time);
							x = (float)( p1.getX()+dt*(p2.getX()-p1.getX()) );
							y = (float)( p1.getY()+dt*(p2.getY()-p1.getY()) );
							path.lineTo( x, y);
							break;
						}
					} else if(!connect) {
						path.moveTo( (float)p1.getX(), (float)p1.getY() );
						connect=true;
					}
					if( p2.time>t2 ) {
						if( !connect ) break;
						dt = (double) (t2-p1.time) / (double)(p2.time-p1.time);
						x = (float)( p1.getX()+dt*(p2.getX()-p1.getX()) );
						y = (float)( p1.getY()+dt*(p2.getY()-p1.getY()) );
						path.lineTo( x, y);
						break;
					} else if( connect ) {
						path.lineTo( (float)p2.getX(), (float)p2.getY() );
					} else {
						path.moveTo( (float)p2.getX(), (float)p2.getY() );
						connect=true;
					}
				}
			}
		} catch(ClassCastException ex) {
		}
		return path;
	}
	public Point2D positionAtTime( int t ) {
		float x, y;
		double dt;
		Point2D point = null;
		if( t<start || t>end ) return null;
		TimeControlPt p1, p2;
		try {
			for( int seg=0 ; seg<cpts.length ; seg++ ) {
				if( cpts[seg].length<2 ) continue;
				p1 = (TimeControlPt)cpts[seg][0];
				if( p1.time>t) break;
				p1 = (TimeControlPt)cpts[seg][cpts[seg].length-1];
				if( p1.time<=t ) continue;
				for( int i=0 ; i<cpts[seg].length-1 ; i++) {
					p2 = (TimeControlPt)cpts[seg][i+1];
					if( p2.time<=t ) continue;
					p1 = (TimeControlPt)cpts[seg][i];
					if( p1.time>t ) return point;
					if( p1.time<=t ) {
						dt = (double) (t-p1.time) / (double)(p2.time-p1.time);
						return new Point2D.Double( 
							p1.getX()+dt*(p2.getX()-p1.getX()) ,
							p1.getY()+dt*(p2.getY()-p1.getY())  );
					}
				}
			}
		} catch(ClassCastException ex) {
		}
		return point;
	}
	public boolean firstNearPoint(double x, double y, Nearest nearest) {
		double dx, dy, dx0, dy0, r, r0, test, xx;
		double x1, y1, x2, y2;
		if(wrap>0) {
			if( y<bounds.getY() || 
				y>bounds.getY()+bounds.getHeight() )return false;
			while(x<bounds.getX()) x+=wrap;
			while(x>bounds.getX()+bounds.getWidth()) x-=wrap;
			if(x<bounds.getX())return false;
		} else {
			if(!bounds.contains(x, y)) return false;
		}
		for( int seg=0 ; seg<cpts.length ; seg++ ) {
			x1 = cpts[seg][0].getX();
			y1 = cpts[seg][0].getY();
			for( int i=0 ; i<cpts[seg].length-1 ; i++ ) {
				x2 = cpts[seg][i+1].getX();
				y2 = cpts[seg][i+1].getY();
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
					r = dx*dx + dy*dy;
					if( r>nearest.rtest ) continue;
					xx = (double)i;
				} else if( test>r0 ) {
					dx -= dx0;
					dy -= dy0;
					r = dx*dx + dy*dy;
					if( r>nearest.rtest ) continue;
					xx = (double)(i+1);
				} else {
					r = -dx*dy0 + dy*dx0;
					r *= r/r0;
					if( r>nearest.rtest ) continue;
					xx = i + test/r0;
				}
				nearest.rtest = r;
				nearest.x = xx;
				nearest.seg = seg;
				nearest.track = this;
				return true;
			}
		}
		return false;
	}
	public void nearestPoint(double x, double y, Nearest nearest) {
		if( nearest.rtest==0 ) return;
		double dx, dy, dx0, dy0, r, r0, test, xx;
		double x1, y1, x2, y2;
		for( int seg=0 ; seg<cpts.length ; seg++ ) {
			x1 = cpts[seg][0].getX();
			y1 = cpts[seg][0].getY();
			for( int i=0 ; i<cpts[seg].length-1 ; i++ ) {
				x2 = (double)cpts[seg][i+1].getX();
				y2 = (double)cpts[seg][i+1].getY();
				dx0 = x2-x1;
				dy0 = y2-y1;
				dx = x-x1;
				dy = y-y1;
				r0 = dx0*dx0 + dy0*dy0;
				test = dx*dx0 + dy*dy0;
				x1 = x2;
				y1 = y2;
				if(test<0) {
					r = dx*dx + dy*dy;
					if( r>nearest.rtest ) continue;
					xx = (double)i;
				} else if( test>r0 ) {
					dx -= dx0;
					dy -= dy0;
					r = dx*dx + dy*dy;
					if( r>nearest.rtest ) continue;
					xx = (double)(i+1);
				} else {
					r = -dx*dy0 + dy*dx0;
					r *= r/r0;
					if( r>nearest.rtest ) continue;
					xx = test/r0;
				}
				nearest.rtest = r;
				nearest.x = xx;
				nearest.seg = seg;
				nearest.track = this;
			}
		}
	}
}

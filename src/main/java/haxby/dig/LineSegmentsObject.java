package haxby.dig;

import haxby.map.*;
import haxby.proj.*;
import haxby.util.*;
import haxby.image.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;

public class LineSegmentsObject
				implements DigitizerObject,
					MouseListener,
					MouseMotionListener,
					KeyListener
					{
	ScaledComponent map;
	Digitizer dig;
	Vector points;
	boolean visible;
	double wrap;
	String name;
	float lineWidth;
	Color color;
	Color fill;
	Color lastColor, lastFill;
	BasicStroke stroke;
	boolean selected;
	boolean showPoints;
	long when;
	int currentPoint;
	double currentOffset = 0.;
	GeneralPath editShape;
	boolean active;
	Vector profile = null;
	static ImageIcon ICON = Icons.getIcon(Icons.SEGMENTS, false);
	static ImageIcon SELECTED_ICON = Icons.getIcon(Icons.SEGMENTS, true);
	static ImageIcon DISABLED_ICON = new ImageIcon(
			GrayFilter.createDisabledImage( ICON.getImage() ));
	static Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
	public LineSegmentsObject( ScaledComponent map, Digitizer dig ) {
		this.map = map;
		if( map instanceof XMap ) {
			wrap = ((XMap)map).getWrap();
		} else {
			wrap = -1;
		}
		this.dig = dig;
		points = new Vector();
		visible = true;
		name = null;
		selected = false;
		lineWidth = 1f;
		color = Color.black;
		when = 0L;
		currentPoint = -1;
		editShape = null;
		active = false;
		setColor( dig.options.color );
		setFill( dig.options.fill );
		setStroke( dig.options.stroke );
		lastColor = color;
		lastFill = Color.white;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String toString() {
		if( name==null) {
			return "Line Segs";
		}
		return name;
	}
	public Color getColor() {
		return color;
	}
	public Color getFill() {
		return fill;
	}
	public void setColor( Color c ) {
		if( c==null ) {
			if( color==null ) {
				color = lastColor;
			} else {
				lastColor = color;
				color = c;
			}
			return;
		}
		color = c;
	}
	public void setFill( Color c ) {
		if( c==null ) {
			if( fill==null ) {
				fill = lastFill;
			} else {
				lastFill = fill;
				fill = c;
			}
			return;
		}
		fill = c;
	}
	public BasicStroke getStroke() {
		return stroke;
	}
	public void setStroke( BasicStroke s ) {
		stroke = s;
	}
	public void start() {
		setSelected( false );
		map.addMouseListener( this );
		map.addMouseMotionListener( this );
		map.addKeyListener( this );
		map.setCursor( cursor );
		active = true;
	}
	public boolean finish() {
		active = false;
		drawEdit();
		map.removeMouseListener( this );
		map.removeMouseMotionListener( this );
		map.removeKeyListener( this );
		map.setCursor( Cursor.getDefaultCursor() );
		if( points.size()<=1 ) return false;
		return true;
	}
	public void setVisible( boolean tf ) {
		visible = tf;
	}
	public boolean isVisible() {
		return visible;
	}
	public boolean select( double x, double y, double[] scales ) {
		if( points.size()<=1 )return false;
		Rectangle2D.Double bounds = (Rectangle2D.Double)computeBounds(scales);
		double r = 2.5;
		bounds.x -= r;
		bounds.y -= r;
		bounds.width += 2.*r;
		bounds.height += 2.*r;
		if( wrap>0. ) {
			while( bounds.x>x ) x += wrap;
			while( bounds.x+bounds.width<x ) x -= wrap;
		}
		if( !bounds.contains( x, y ) )return false;
		double x1, y1, x2, y2, dx0, dy0, dx, dy, r0, r1, test;;
		double[] xyz = (double[])points.get(0);
		x1 = xyz[0]*scales[0];
		y1 = xyz[1]*scales[1];
		double rMin = 100000.;
		r *= r;
		for( int i=1 ; i<points.size() ; i++) {
			xyz = (double[])points.get(i);
			x2 = xyz[0]*scales[0];
			y2 = xyz[1]*scales[1];
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
	public Rectangle2D computeBounds(double[] scales) {
		Rectangle2D.Double bounds = new Rectangle2D.Double();
		if( points.size()==0 ) return bounds;
		double[] xyz = (double[])points.get(0);
		bounds.x = xyz[0]*scales[0];
		bounds.y = xyz[1]*scales[1];
		double x, y;
		for( int i=1 ; i<points.size() ; i++ ) {
			xyz = (double[])points.get(i);
			x = xyz[0]*scales[0];
			y = xyz[1]*scales[1];
			if( x<bounds.x ) {
				bounds.width += bounds.x-x;
				bounds.x = x;
			} else if( x>bounds.x+bounds.width ) {
				bounds.width = x-bounds.x;
			}
			if( y<bounds.y ) {
				bounds.height += bounds.y-y;
				bounds.y = y;
			} else if( y>bounds.y+bounds.height ) {
				bounds.height = y-bounds.y;
			}
		}
		return bounds;
	}
	public void setSelected( boolean tf ) {
		if( selected == tf ) return;
		if( !selected ) {
			map.removeMouseListener(this);
			map.removeMouseMotionListener(this);
			map.addMouseListener(this);
			map.addMouseMotionListener(this);
		} else {
			map.removeMouseListener(this);
			map.removeMouseMotionListener(this);
		}
		selected = tf;
	}
	public boolean isSelected() {
		return selected;
	}
	
	public void setShowPoints (boolean tf) {
		showPoints = tf;
	}
	
	public void draw( Graphics2D g, double[] scales, Rectangle bounds, Insets ins ) {
		
		editShape=null;
		if (!visible || points.size() == 0) return;
		g.setStroke( stroke );
		GeneralPath path = new GeneralPath();
		double[] xyz = (double[])points.get(0);
		double min = xyz[0];
		double max = xyz[1];
		path.moveTo( (float)min, (float)max );
		GeneralPath path1 = new GeneralPath();
		double dx = 2.0;
		double dy = 2.0;
		if (showPoints) {
			dx = dy = 2.5;
		}
		dx /= scales[0];
		dy /= scales[1];
		
		double x, y;
		x = xyz[0];
		y = xyz[1];
				
		if( selected || showPoints) path1.append( new Rectangle2D.Double( x-dx, y-dy, 2*dx, 2*dy), false);
		for(int i=1 ; i<points.size() ; i++) {
			xyz = (double[])points.get(i);
			x = xyz[0];
			y = xyz[1];
			if( x>max ) max=x;
			else if( x<min ) min=x;
			path.lineTo( (float)x, (float)y );
			if( selected || showPoints) path1.append( new Rectangle2D.Double( x-dx, y-dx, 2*dx, 2*dy), false);
		}
		path.transform(getInverseTransform(scales, ins));
		path1.transform(getInverseTransform(scales, ins));
		double wrap = this.wrap * scales[0];
		wrap = 0;
		if( wrap>0. ) {
			AffineTransform at = g.getTransform();
			double offset = 0.;
			while( min+offset>bounds.getX() ) offset -= wrap;
			while( max+offset< bounds.getX() ) offset += wrap;
			g.translate( offset, 0.);
			while( min+offset < bounds.getX()+bounds.getWidth() ) {
				if( fill != null ) {
					g.setColor(fill);
					g.fill( path );
				}
				if( color!=null ) {
					g.setColor(color);
					g.draw( path );
				}
				offset += wrap;
				g.translate( wrap, 0.);
			}
			g.setTransform( at);
		} else {
			if( fill != null ) {
				g.setColor(fill);
				g.fill( path );
			}
			if( color!=null ) {
				g.setColor(color);
				g.draw( path );
			}
		}
		
		if(selected ) {
			g.setColor(Color.MAGENTA);
			g.setStroke( new BasicStroke( 3f ));
			if( wrap>0. ) {
				AffineTransform at = g.getTransform();
				double offset = 0.;
				while( min+offset>bounds.getX() ) offset -= wrap;
				while( max+offset< bounds.getX() ) offset += wrap;
				g.translate( offset, 0.);
				while( min+offset < bounds.getX()+bounds.getWidth() ) {
					g.draw( path );
					//g.draw( path1 );
					offset += wrap;
					g.translate( wrap, 0.);
				}
				g.setTransform( at);
			} else {
				g.draw( path );
				//g.draw( path1 );
			}
		}
		
		if (selected || showPoints) {
			g.setColor(Color.red);
			g.setStroke( new BasicStroke( 3f ));
			if( wrap>0. ) {
				AffineTransform at = g.getTransform();
				double offset = 0.;
				while( min+offset>bounds.getX() ) offset -= wrap;
				while( max+offset< bounds.getX() ) offset += wrap;
				g.translate( offset, 0.);
				while( min+offset < bounds.getX()+bounds.getWidth() ) {
					g.draw( path1 );
					offset += wrap;
					g.translate( wrap, 0.);
				}
				g.setTransform( at);
			} else {
				g.draw( path1 );
			}
		}
		g.setPaintMode();
	}
	public void mouseEntered( MouseEvent evt ) {
		editShape = null;
		mouseMoved(evt);
	}
	public void mouseExited( MouseEvent evt ) {
		drawEdit();
		editShape = null;
	}
	public void mousePressed( MouseEvent evt ) {
		if( evt.isControlDown() || !selected ) return;
		double[] scales = map.getScales();
		double r = 4;
		Point p = evt.getPoint();
		Insets insets = map.getInsets();
		p.x -= insets.left;
		p.y -= insets.top;
		double x, y;
		double wrap = this.wrap * scales[0];
		for( int i=0 ; i<points.size() ; i++) {
			double[] xyz = (double[])points.get(i);
			x = xyz[0]*scales[0];
			y = xyz[1]*scales[1];
			if( p.y<y-r || p.y>y+r )continue;
			currentOffset = 0.;
			if( wrap>0. ) {
				while( p.x<x-wrap/2. ) {
					currentOffset -= wrap;
					p.x+=wrap;
				}
				while( p.x>x+wrap/2. ) {
					currentOffset += wrap;
					p.x-=wrap;
				}
			}
			if( p.x<x-r || p.x>x+r )continue;
			when = evt.getWhen();
			currentPoint = i;
			mouseDragged( evt );
			return;
		}
		currentPoint = -1;
	}
	public void mouseReleased( MouseEvent evt ) {
		if( currentPoint==-1 || editShape==null ) {
			currentPoint = -1;
			return;
		}
		drawEdit();
		editShape = null;
		if( evt.isControlDown() || evt.getWhen()-when<10L ) {
			currentPoint = -1;
			return;
		}
		Point2D.Double p = (Point2D.Double)map.getScaledPoint( evt.getPoint() );
		double[] xyz = (double[])points.get( currentPoint );
		xyz[0] = p.x - currentOffset;
		xyz[1] = p.y;
		xyz[2] = getZ(p);
		map.repaint();
	}
	public void mouseClicked( MouseEvent evt ) {
		if(!active || evt.isControlDown())return;
		if (SwingUtilities.isRightMouseButton(evt)) return;
		drawEdit();
		Point2D.Double p = (Point2D.Double)map.getScaledPoint( evt.getPoint() );
		if( points.size()>0 ) {
			double[] xyz1 = (double[])points.get(points.size()-1);
		//	if( p.x==xyz1[0] && p.y==xyz1[1] ) return;
			if( wrap>0. ) {
				while (p.x>xyz1[0]+wrap/2.) p.x -= wrap;
				while (p.x<xyz1[0]-wrap/2.) p.x += wrap;
			}
		}
		double z = getZ( p );
		double[] xyz = new double[] { p.x, p.y, z};
		points.add( xyz );
		redraw();
	}
	public double getZ(Point2D p) {
		return Double.NaN;
	}
	public boolean isActive() {
		return active;
	}
	public void redraw() {
		
		synchronized( map.getTreeLock() ) {
			Graphics2D g = (Graphics2D)map.getGraphics();
		
			double[] scales = map.getScales();
			Rectangle r = map.getVisibleRect();
			Insets ins = map.getInsets();
			r.width -= ins.left + ins.right;
			r.height -= ins.top + ins.bottom;
			draw( g, scales, r, ins );
		}
	}
	public void mouseMoved( MouseEvent evt ) {
		if(selected || evt.isControlDown())return;
		if( map.getCursor() != cursor ) map.setCursor( cursor );
		if( points.size()==0 )return;
		drawEdit();
		double[] xyz = (double[])points.get(points.size()-1);
		Point2D.Double pt = new Point2D.Double(xyz[0], xyz[1]);
		editShape = new GeneralPath();
		editShape.append( new Line2D.Double( pt,
				map.getScaledPoint( evt.getPoint() )), false );
		editShape.transform(getInverseTransform());
		drawEdit();
	}
	public void mouseDragged( MouseEvent evt ) {
		if( currentPoint < 0)return;
		drawEdit();
		if( evt.isControlDown() ) {
			currentPoint=-1;
			return;
		}
		editShape = new GeneralPath();
		Point2D.Double p0 = (Point2D.Double)map.getScaledPoint( evt.getPoint() );
		if( currentPoint!=0 ) {
			double[] xyz = (double[])points.get(currentPoint-1);
			editShape.append(new Line2D.Double( p0,  
					new Point2D.Double(xyz[0]+currentOffset, xyz[1])), false);
		}
		if( currentPoint!=points.size()-1 ){
			double[] xyz = (double[])points.get(currentPoint+1);
			editShape.append(new Line2D.Double( p0,  
					new Point2D.Double(xyz[0]+currentOffset, xyz[1])), false);
		}
		double[] scales = map.getScales();
		double dx = 2.5/scales[0];
		double dy = 2.5/scales[1];
		editShape.append( new Rectangle2D.Double( p0.x-dx, p0.y-dy, 2.*dx, 2.*dy ), false);
		editShape.transform( getInverseTransform() );
		drawEdit();
	}
	
	AffineTransform getInverseTransform() {
		return getInverseTransform(map.getScales(), map.getInsets());
	}
	
	AffineTransform getInverseTransform(double[] scales, Insets i) {
		AffineTransform at = new AffineTransform();	
		at.translate((double)i.left, (double)i.top );
		at.scale( scales[0], scales[1] );
		return at;
	}
	
	
	public void drawEdit() {
		if( editShape==null ) return;
		synchronized( map.getTreeLock() ) {
			Graphics2D g = (Graphics2D)map.getGraphics();
			g.setStroke( new BasicStroke( 1f ));
			g.setXORMode( Color.white);
			Rectangle2D bounds = editShape.getBounds2D();
			double min = bounds.getX();
			double max = min + bounds.getWidth();

			Rectangle rect = map.getVisibleRect();
			Insets ins = map.getInsets();
			rect.width -= ins.left + ins.right;
			rect.height -= ins.top + ins.bottom;
			
			double wrap = this.wrap * map.getScales()[0];
			if( wrap>0. ) {
				double offset = 0.;
				while( min+offset>rect.getX() ) offset -= wrap;
				while( max+offset< rect.getX() ) offset += wrap;
				g.translate( offset, 0.);
				while( min+offset < rect.getX()+bounds.getWidth() ) {
					g.draw( editShape );
					offset += wrap;
					g.translate( wrap, 0.);
				}
			} else {
				g.draw( editShape );
			}
		}
	}
	public void keyPressed( KeyEvent evt ) {
	}
	public void keyReleased( KeyEvent evt ) {
		if( evt.isControlDown() ) return;
		if( evt.getKeyCode() == evt.VK_ENTER ) dig.selectB.doClick();
		if( evt.getKeyCode() == evt.VK_BACK_SPACE) {
			if( points.size()>0 ) {
				points.remove( points.size()-1 );
				map.repaint();
			}
		}
	}
	public void keyTyped( KeyEvent evt ) {
	}
	public Vector getPoints() {
		return points;
	}
	public void setPoints( Vector points ) {
		this.points = points;
	}
	public ImageIcon getIcon() {
		return ICON;
	}
	public ImageIcon getDisabledIcon() {
		return DISABLED_ICON;
	}
}
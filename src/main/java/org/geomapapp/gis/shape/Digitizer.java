package org.geomapapp.gis.shape;

import org.geomapapp.util.Icons;
import org.geomapapp.util.Spline2D;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.util.Vector;

import haxby.map.*;

public class Digitizer implements Overlay {
	protected MouseInputAdapter mouse;
	protected KeyAdapter key;
	protected ESRIShapefile shapes;
	protected XMap map;
	protected Vector points;
	protected String label;
	Spline2D spline;
	Point2D point;
	Shape line;
	JToggleButton toggle;
	ViewShapes view;
	public Digitizer(XMap map) {
		spline = new Spline2D();
	//	spline.setClosePath(true);
		this.map = map;
		Vector names = new Vector(1);
		names.add("ID");
		Vector classes = new Vector(1);
		classes.add(String.class);
		shapes = new ESRIShapefile("shapes", 3, names, classes);
		shapes.setMap(map);
		points = new Vector();
		initMouse();
		toggle = new JToggleButton(Icons.getIcon(Icons.DIGITIZE, false));
		toggle.setSelectedIcon(Icons.getIcon(Icons.DIGITIZE, true));
		toggle.setBorder(null);
		toggle.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setEnabled(toggle.isSelected());
			}
		});
	}
	public JToggleButton getToggle() {
		return toggle;
	}
	public void setEnabled(boolean tf) {
		if( view!=null)view.setVisible(tf);
		map.removeMouseListener(mouse);
		map.removeMouseMotionListener(mouse);
		map.removeKeyListener( key );
		if( !tf ) {
			if( points.size()>1 ) {
				drawLine();
				points = new Vector();
			}
			return;
		}
		map.addMouseListener(mouse);
		map.addMouseMotionListener(mouse);
		map.addKeyListener(key);
	}
	void removePoint() {
	//	drawLine();
		if( points.size()==0 )return;
		points.remove(points.size()-1);
		map.repaint();
	//	synchronized (map.getTreeLock()) {
	//		draw(map.getGraphics2D());
	//	}
	}
	void addPoint(MouseEvent e) {
	//	drawLine();
		point = map.getScaledPoint(e.getPoint());
		points.add( point );
		map.repaint();
	//	synchronized (map.getTreeLock()) {
	//		draw(map.getGraphics2D());
	//	}
	}
	void drawLine() {
		if( points.size()==0 || point==null )return;
	//	Point2D p = (Point2D)points.get(points.size()-1);
	//	line = new Line2D.Double( point, p);
		points.add(point);
		spline.setPoints(points);
		line = spline.getPath();
		double zoom = map.getZoom();
		Rectangle2D.Double r = new Rectangle2D.Double( -4./zoom, -4./zoom, 8./zoom, 8./zoom);
		synchronized (map.getTreeLock()) {
			Graphics2D g = map.getGraphics2D();
			AffineTransform at = g.getTransform();
			g.setStroke( new BasicStroke(1f/(float)zoom));
			for( int k=0 ; k<points.size()-1 ; k++) {
				Point2D p = (Point2D)points.get(k);
				g.translate( p.getX(), p.getY());
				g.setXORMode(Color.cyan);
				g.fill(r);
				g.setXORMode(Color.red);
				g.draw(r);
				g.setTransform(at);
			}
			g.setStroke( new BasicStroke(2f/(float)zoom));
			g.setXORMode(Color.white);
			g.draw(line);
		//	g.draw( line.getBounds2D() );
		}
		points.remove( points.size()-1);
	}
	void move(MouseEvent e) {
		if( points.size()==0 )return;
		drawLine();
		point = map.getScaledPoint(e.getPoint());
		drawLine();
	}
	void finish() {
		drawLine();
		point=null;
		toggle.setSelected(false);
		if( points.size()<=1 ) {
			points = new Vector();
			return;
		}
		Point2D p = (Point2D)points.get(0);
		double xmin=p.getX();
		double xmax=xmin;
		double ymin=p.getY();
		double ymax=ymin;
		double lastX = xmin;
		double wrap = map.getWrap();
		ESRIPolyLine shape = new ESRIPolyLine( xmin,
				ymin, xmax, ymax, 1, points.size());
		shape.addPoint( 0, xmin, ymin);
		for( int i=1 ; i<points.size() ; i++) {
			p = (Point2D)points.get(i);
			double x = p.getX();
			double y = p.getY();
			if( wrap>0. ) {
				while(x>lastX+wrap/2.)x-=wrap;
				while(x<lastX-wrap/2.)x+=wrap;
			}
			if( x<xmin )xmin=x;
			else if(x>xmax)xmax=x;
			if( y<ymin )ymin=y;
			else if(y>ymax)ymax=y;
			shape.addPoint( i, x, y);
		}
		shape.x = xmin;
		shape.y = ymin;
		shape.width = xmax-xmin;
		shape.height = ymax-ymin;
		Vector record = new Vector();
		record.add("shape");
		shapes.addShape( shape, record);
		if( shapes.size()==1 ) {
			ShapeSuite suite = new ShapeSuite();
			suite.addShapeFile(shapes);
			view = new ViewShapes( suite, map);
		}
		points = new Vector();
		map.repaint();
	}
	void initMouse() {
		mouse = new MouseInputAdapter() {
			public void mouseMoved(MouseEvent e) {
				if( e.isControlDown() || !toggle.isSelected() ) return;
				if( points.size()==0 )return;
				move(e);
			}
			public void mouseClicked(MouseEvent e) {
				if( e.isControlDown() || !toggle.isSelected() ) return;
				if(e.getClickCount()==2) {
					finish();
					return;
				}
				addPoint(e);
			}
		};
		key = new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				int code = e.getKeyCode();
				if( code==e.VK_DELETE )delete();
				else if( code==e.VK_BACK_SPACE ) removePoint();
				else if( code==e.VK_ENTER ) finish();
			}
		};
	}
	void delete() {
		if( points.size()>0 ) {
			drawLine();
			if( points.size()>1 ) {
				points = new Vector();
				map.repaint();
			} else {
				points = new Vector();
			}
			point = null;
		} else {
			shapes.removeSelectedObject();
		}
	}
	public void draw(Graphics2D g) {
		point = null;
		if( points==null || points.size()<2)return;
/*
		GeneralPath path = new GeneralPath();
		g.setColor( Color.red );
		g.setStroke( new BasicStroke(1f/(float)map.getZoom()));
		for( int k=0 ; k<points.size() ; k++) {
			Point2D p = (Point2D)points.get(k);
			if( k==0 ) path.moveTo((float)p.getX(),(float)p.getY());
			else path.lineTo((float)p.getX(),(float)p.getY());
		}
*/
		spline.setPoints(points);
		GeneralPath path = spline.getPath();
		g.setStroke( new BasicStroke(1f/(float)map.getZoom() ));
		g.draw( path);
	}
/*
	public static void main(String[] args) {
		if(  args.length==0 ) {
			MapApp app = new MapApp(0);
			new Digitizer(app.getMap());
		} else {
			xb.bill.PC pc = new xb.bill.PC();
			new Digitizer( pc.getApp().getMap() );
		}
		}
*/
}
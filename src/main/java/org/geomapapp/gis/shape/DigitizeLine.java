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

public class DigitizeLine implements Overlay {
	protected XMap map;
	protected Vector points;
	Spline2D spline;
	Point2D point;
	GeneralPath line;
	boolean closePath;
	boolean finished;
	public DigitizeLine(XMap map, boolean curve, boolean closePath) {
		finished = false;
		this.closePath = closePath;
		if( curve ) {
			spline = new Spline2D();
			spline.setClosePath(closePath);
		}
		this.map = map;
		points = new Vector();
	}
	void removePoint() {
		if( points.size()==0 )return;
		points.remove(points.size()-1);
		map.repaint();
	}
	void addPoint(MouseEvent e) {
		point = map.getScaledPoint(e.getPoint());
		points.add( point );
		map.repaint();
	}
	public void finish() {
		setShape();
		finished = true;
	}
	public void closePath(boolean tf) {
		if( closePath==tf )return;
		if( spline!=null ) spline.setClosePath(tf);
		boolean done = finished;
		finished = false;
		setShape();
		finished = done;
		closePath = tf;
		map.repaint();
	}
	public void setCurved( boolean tf) {
		boolean curved = spline!=null;
		if( curved==tf )return;
		if( tf ) {
			spline = new Spline2D();
		} else {
			spline = null;
		}
		boolean done = finished;
		finished = false;
		setShape();
		finished = done;
		map.repaint();
	}
	public boolean isPathClosed() {
		return closePath;
	}
	public boolean isCurved() {
		return spline!=null;
	}
	void setShape() {
		if( spline!=null) {
			spline.setPoints(points);
			line = spline.getPath();
			spline.setPoints(null);
		} else {
			line = new GeneralPath();
			for( int k=0 ; k<points.size()-1 ; k++) {
				Point2D p = (Point2D)points.get(k);
				if(k==0) line.moveTo((float)p.getX(), (float)p.getY());
				else line.lineTo((float)p.getX(), (float)p.getY());
			}
			if( closePath ) line.closePath();
		}
	}
	void drawLine() {
		if( points.size()==0 || point==null )return;
		points.add(point);
		setShape();
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
		}
		points.remove( points.size()-1);
	}
	void move(MouseEvent e) {
		if( points.size()==0 )return;
		drawLine();
		point = map.getScaledPoint(e.getPoint());
		drawLine();
	}
	void delete() {
		points = new Vector();
		map.repaint();
	}
	public void draw(Graphics2D g) {
		point = null;
		if( points==null || points.size()<2)return;
		setShape();
		g.setStroke( new BasicStroke(1f/(float)map.getZoom() ));
		g.draw( line);
		AffineTransform at = g.getTransform();
		double zoom = map.getZoom();
		g.setStroke( new BasicStroke(1f/(float)zoom));
		Rectangle2D.Double r = new Rectangle2D.Double( -4./zoom, -4./zoom, 8./zoom, 8./zoom);
		for( int k=0 ; k<points.size() ; k++) {
			Point2D p = (Point2D)points.get(k);
			g.translate( p.getX(), p.getY());
			g.setColor(Color.white);
			g.draw(r);
			g.setTransform(at);
		}
	}
}

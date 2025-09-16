package org.geomapapp.util;

import javax.swing.JComponent;
import java.awt.geom.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Vector;

public class ScalableComp extends JComponent {
	protected Rectangle bounds;
	protected int xWrap = -1;
	protected Rectangle unScaledBounds;
	private AffineTransform aTrans;
	protected Vector layers;
	protected double scale;
	protected double xOffset, yOffset;
	transient BufferedImage pan;
	transient int panX, panY;
	transient boolean panning=false;
	transient Shape shape;
	public ScalableComp() {
		aTrans = new AffineTransform();
		bounds = new Rectangle( 0, 0, 800, 600 );
		unScaledBounds = (Rectangle)bounds.clone();
		layers = new Vector();
		xOffset = yOffset = 0.;
		scale = 1.;
	}
	public ScalableComp( Rectangle bounds ) {
		this();
		this.bounds = bounds;
		unScaledBounds = (Rectangle)bounds.clone();
	}
	public ScalableComp( int width, int height ) {
		this( new Rectangle(0, 0, width, height ) );
	}
	public void setXWrap(int wrap) {
		xWrap = wrap;
	}
	public int getXWrap() {
		return xWrap;
	}
	public Rectangle getBounds() {
		return (Rectangle)bounds.clone();
	}
	public BufferedImage getImage() {
		return pan;
	}
	public void addLayer( Layer layer ) {
		layers.add(layer);
	}
	public Dimension getPreferredSize() {
		return new Dimension( 800, 600 );
	}
	public AffineTransform getTransform() {
		return (AffineTransform)aTrans.clone();
	}
	public Point2D inverse( Point2D mousePoint ) {
		try {
			Point2D.Double p = new Point2D.Double();
			p = (Point2D.Double)aTrans.inverseTransform( mousePoint, p);
			return p;
		} catch (Exception ex) {
			return mousePoint;
		}
	}
	public Point2D forward( Point2D pt ) {
		Point2D.Double p = new Point2D.Double();
		p = (Point2D.Double)aTrans.transform( pt, p );
		return p;
	}
	public Rectangle2D visibleRect() {
		Dimension dim = getSize();
		Point p = new Point( 0, 0 );
		Point2D p0 = inverse(p);
		p.x = dim.width;
		p.y = dim.height;
		Point2D p1 = inverse( p );
		Rectangle2D.Double r = new Rectangle2D.Double();
		r.x = Math.min( p0.getX(), p1.getX() );
		r.y = Math.min( p0.getY(), p1.getY() );
		r.width = Math.abs( p0.getX() - p1.getX() );
		r.height = Math.abs( p0.getY() - p1.getY() );
		if( r.width>bounds.width )r.width=bounds.width;
		if( r.height>bounds.height )r.height=bounds.height;
		return r;
	}
	public void doZoom( Point2D p, double factor ) {
		shape = null;
		Dimension dim = getSize();
		Point2D p0 = inverse(new Point2D.Double(p.getX()-scale*bounds.x-dim.width*.5/factor, 
						p.getY()-scale*bounds.y-dim.height*.5/factor));
		scale *= factor;
		xOffset = -p0.getX()*scale;
		yOffset = -p0.getY()*scale;
		checkBounds();
		repaint();
	}
	void checkBounds() {
		aTrans = new AffineTransform();
		aTrans.translate( xOffset, yOffset );
		aTrans.translate( -bounds.x*scale, -bounds.y*scale );
		aTrans.scale( scale, scale );
		Rectangle2D r = visibleRect();
		double xShift = 0.;
		if( r.getX()<bounds.x ) {
			xShift += r.getX()-bounds.getX();
		} else if( r.getX()+r.getWidth() >bounds.x+bounds.width ) {
			xShift += r.getX()+r.getWidth() - bounds.x-bounds.width;
			if( r.getX()+xShift<bounds.x ) xShift += r.getX()-xShift-bounds.getX();
		}
		xOffset += xShift*scale;
		double yShift = 0.;
		if( r.getY()<bounds.y ) {
			yShift += r.getY()-bounds.getY();
		} else if( r.getY()+r.getHeight() >bounds.y+bounds.height ) {
			yShift += r.getY()+r.getHeight() - bounds.y-bounds.height;
			if( r.getY()+yShift<bounds.y ) yShift += r.getY()-yShift-bounds.getY();
		}
		yOffset += yShift*scale;
	}
	public void initPan() {
		panning = true;
		panX = panY = 0;
	}
	public void disposePan() {
		panning = false;
		xOffset += panX;
		yOffset += panY;
		repaint();
	}
	public void pan( int offsetX, int offsetY ) {
		panX += offsetX;
		panY += offsetY;
		repaint();
	}
	public void setShape(Shape shape) {
		this.shape = shape;
		repaint();
	}
	public void paint(Graphics g0) {
		if( pan!=null ) {
			if( panning ) {
				g0.drawImage( pan, panX, panY, this );
				return;
			} else if( shape!=null ) {
				Graphics2D g = (Graphics2D)g0;
				g.drawImage( pan, 0, 0, this);
				BasicStroke s = new BasicStroke(1f);
				g.setColor( Color.red );
				g.draw(shape);
				return;
			}
		}
		Dimension dim = getSize();
		pan = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D)pan.createGraphics();
		g.setColor( Color.lightGray );
		g.fillRect( 0, 0, dim.width, dim.height);
		checkBounds();
		aTrans = new AffineTransform();
		aTrans.translate( xOffset, yOffset );
		aTrans.translate( -bounds.x*scale, -bounds.y*scale );
		aTrans.scale( scale, scale );
		AffineTransform at = getTransform();
		Rectangle2D r = visibleRect();
		for( int i=0 ; i<layers.size() ; i++) {
			((Layer)layers.get(i)).draw( g, at, r );
		}
		g = (Graphics2D)g0;
		g.drawImage( pan, 0, 0, this);
	}
}
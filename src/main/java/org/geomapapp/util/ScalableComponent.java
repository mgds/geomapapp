package org.geomapapp.util;

import java.util.Vector;
import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

public abstract class ScalableComponent extends JComponent
			implements Zoomable, 
				Scrollable,
				SwingConstants {
	protected JScrollPane scrollPane = null;
	AffineTransform aTrans;
	protected int width, height;
	protected Rectangle unscaledBounds;
	Rectangle zoomRect = null;
	protected boolean tracksWidth, tracksHeight;
	protected Symbol shape;
	protected Point2D location;
	int rotation = 0;
	Vector points;
	double zScale = 1.0;

	protected ScalableComponent() {
		aTrans = new AffineTransform();
	}
	public BufferedImage getImage() {
		Rectangle r = getVisibleRect();
		BufferedImage im = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = im.createGraphics();
		g2d.translate(-r.getX(), -r.getY());
		paint(g2d);
		return im;
	}
	public void enableDigitizer(boolean tf) {
	}
	public void saveImage(File file) throws IOException {
		int s_idx = file.getName().indexOf(".");
		String suffix = s_idx<0
			? "jpg"
			: file.getName().substring(s_idx+1);
		if( !javax.imageio.ImageIO.getImageWritersBySuffix(suffix).hasNext() )suffix = "jpg";
		javax.imageio.ImageIO.write( getImage(), suffix, file);
	}
	public void setLocation(Point2D p) {
		clearLocation();
		location = p;
		drawLocation();
	}
	void clearLocation() {
		if( location==null )return;
		if( shape==null ) initShape();
		Point2D p = getTransform().transform( location, new Point2D.Double() );
		int x = (int)Math.rint(p.getX());
		int y = (int)Math.rint(p.getY());
		Rectangle r = shape.getBounds();
		r.x += x;
		r.y += y;
		paintImmediately( r );
	}
	void drawLocation() {
		if( location==null )return;
		if( shape==null ) initShape();
		Point2D p = getTransform().transform( location, new Point2D.Double() );
		int x = (int)Math.rint(p.getX());
		int y = (int)Math.rint(p.getY());
		synchronized(getTreeLock()) {
			Graphics2D g = (Graphics2D)getGraphics();
			g.translate( x, y );
			shape.draw(g);
		}
	}
	void initShape() {
		shape = new Symbol(
			new Arc2D.Double( -6., -6., 12., 12., 0., 360., Arc2D.CHORD),
			Color.magenta, null, 2f);
	}
	public void setShapeColor(Color color) {
		if( shape==null )initShape();
		shape.setOutline(color);
	}
	protected void setUnscaledBounds(Rectangle unscaledBounds) {
		this.unscaledBounds = unscaledBounds;
		width = unscaledBounds.width;
		height = unscaledBounds.height;
	}
	
	public double getZScale() {
		return zScale;
	}
	
	public Rectangle getUnscaledBounds() {
		return (Rectangle)unscaledBounds.clone();
	}
	public Rectangle2D getUnscaledVisibleRect() {
		if( !isVisible() ) return new Rectangle(0,0,0,0);
		Rectangle r = getVisibleRect();
		Point2D p1 = inverseTransform( new Point(r.x, r.y) );
		Point2D p2 = inverseTransform( new Point(r.x+r.width, r.y+r.height) );
		Rectangle2D.Double r0 = new Rectangle2D.Double(
			Math.min( p1.getX(), p2.getX() ),
			Math.min( p1.getY(), p2.getY() ),
			Math.abs( p1.getX() - p2.getX() ),
			Math.abs( p1.getY() - p2.getY() ) );
		return r0;
	}
	public Dimension getPreferredSize() {
		Point p = new Point( width, height);
		Point2D p0 = aTrans.transform( p, null );
		double[] matrix = new double[6];
		aTrans.getMatrix( matrix );
		double w = Math.abs( p0.getX() );
		double h = Math.abs( p0.getY() );
		p.x = p.y = 0;
		p0 = aTrans.transform( p, null );
		if( Math.abs( p0.getX() ) > w ) w = Math.abs( p0.getX() );
		if( Math.abs( p0.getY() ) > h ) h = Math.abs( p0.getY() );
		Insets insets = getInsets();
		w += insets.left+insets.right;
		h += insets.top+insets.bottom;
		return new Dimension( (int)w, (int)h );
	}
	public AffineTransform getTransform() {
		return (AffineTransform)aTrans.clone();
	}
	public AffineTransform getInverseTransform() {
		try {
			return aTrans.createInverse();
		} catch (Exception ex) {
			return new AffineTransform();
		}
	}
	public Point2D inverseTransform( Point2D mousePoint ) {
		try {
			return aTrans.inverseTransform( 
					mousePoint, 
					new Point2D.Double() );
		} catch (Exception ex) {
		//	ex.printStackTrace();
			return mousePoint;
		}
	}
	public Point2D transform( Point2D mapPoint ) {
		return aTrans.transform( mapPoint, new Point2D.Double() );
	}
	public void resetTransform() {
		aTrans = new AffineTransform();
		invalidate();
		if( scrollPane==null ) {
			repaint();
			return;
		}
		scrollPane.validate();
		JScrollBar sb = scrollPane.getHorizontalScrollBar();
		sb.setValue(0);
		sb = scrollPane.getVerticalScrollBar();
		sb.setValue(0);
		repaint();
	}
	public void removeNotify() {
		super.removeNotify();
		scrollPane = null;
	}
	public void addNotify() {
		super.addNotify();
		Container c = getParent();
		while(c != null) {
			if( c instanceof JScrollPane ) {
				scrollPane = (JScrollPane) c;
				return;
			}
			c = c.getParent();
		}
		scrollPane = null;
	}
// methods implementing Scrollable
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}
	public int getScrollableUnitIncrement(Rectangle visibleRect,
					int orientation,
					int direction) {
		return 10;
	}
	public int getScrollableBlockIncrement(Rectangle visibleRect,
					int orientation,
					int direction) {
		Insets ins = getInsets();
		if( orientation==SwingConstants.HORIZONTAL ) {
			return (visibleRect.width-ins.left-ins.right) / 2;
		} else {
			return (visibleRect.height-ins.top-ins.bottom) / 2;
		}
	}
	public boolean getScrollableTracksViewportWidth() {
		return tracksWidth;
	}
	public boolean getScrollableTracksViewportHeight() {
		return tracksHeight;
	}
	public void setScrollableTracksViewportWidth( boolean tf ) {
		tracksWidth = tf;
	}
	public void setScrollableTracksViewportHeight( boolean tf ) {
		tracksHeight = tf;
	}

	public void setRect(Rectangle rect) {
		synchronized (getTreeLock()) {
			Graphics2D g = (Graphics2D)getGraphics();
			g.setXORMode(Color.white);
			if(zoomRect!=null) g.draw(zoomRect);
			zoomRect = rect;
			if(zoomRect!=null) g.draw(zoomRect);
		}
	}
	public void setXY(Point p) {
	}
	public void zoomIn( Point p ) {
		doZoom( p, 2. );
		zScale *= 2.0;
	}
	public void zoomOut( Point p ) {
		doZoom( p, .5 );
		zScale /= 2.0;
	}
	public void zoomTo( Rectangle rect ) {
		if(rect.width<10 || rect.height<10) return;
		Point p = new Point( rect.x + rect.width/2, rect.y + rect.height/2 );
		Rectangle r = getVisibleRect();
		Insets insets = getInsets();
		if( insets!=null ) {
			r.width -= insets.left + insets.right;
			r.height -= insets.top + insets.bottom;
		}
		double factor = Math.min( r.getWidth()/rect.getWidth(),
					r.getHeight()/rect.getHeight() );
		doZoom( p, factor );
	}
	public void doZoom( Point p, double factor ) {
		Insets insets = getInsets();
		Rectangle rect = getVisibleRect();
		Dimension size = getPreferredSize();
		if( rect.width>size.width ) {
			rect.x = 0;
			rect.width=size.width;
		}
		if( rect.height>size.height ) {
			rect.y = 0;
			rect.height=size.height;
		}
		if( p.x>=size.width ) p.x = size.width-1;
		if( p.y>=size.height ) p.y = size.height-1;
		rect.width -= insets.left + insets.right;
		rect.height -= insets.top + insets.bottom;
		int newX = p.x - insets.left - (int)(rect.width/(2.*factor));
		int newY = p.y - insets.top - (int)(rect.height/(2.*factor));
		Point2D newP = null;
		try {
			newP = inverseTransform( new Point(newX, newY) );
		} catch (Exception ex ) {
			return;
		}
		AffineTransform at = new AffineTransform();
		at.scale( factor, factor );
		aTrans.preConcatenate( at );
		newP = transform( newP );
		newX = (int)newP.getX();
		newY = (int)newP.getY();
		invalidate();
		if( scrollPane==null ) {
			repaint();
			return;
		}
		scrollPane.validate();
		JScrollBar sb = scrollPane.getHorizontalScrollBar();
		sb.setValue(newX);
		sb = scrollPane.getVerticalScrollBar();
		sb.setValue(newY);
		repaint();
	}
	public void stretch() {
		scaleX( 2. );
	}
	public void shrink() {
		scaleX( .5 );
	}
	public void scaleX( double factor ) {
		Rectangle rect = getVisibleRect();
		Dimension size = getPreferredSize();
		if( rect.width>size.width ) {
			rect.x = 0;
			rect.width=size.width;
		}
		if( rect.height>size.height ) {
			rect.y = 0;
			rect.height=size.height;
		}
		Insets insets = getInsets();
		Point2D p0 = new Point( rect.x+ rect.width/2, rect.y+rect.height/2 );
		p0 = inverseTransform( p0 );
		AffineTransform stretch = new AffineTransform(new double[] { factor, 0., 0., 1., 0., 0. } );
		aTrans.preConcatenate( stretch );

		p0 = aTrans.transform( p0, null );
		int x = (int)(p0.getX()-rect.width/2);
		int y = (int)(p0.getY()-rect.height/2);
		invalidate();
		if( scrollPane==null ) {
			repaint();
			return;
		}
		scrollPane.validate();
		JScrollBar sb = scrollPane.getHorizontalScrollBar();
		sb.setValue(x);
		sb = scrollPane.getVerticalScrollBar();
		sb.setValue(y);
	//	revalidate();
		repaint();
	}
	public void flip(int orientation) {
		Rectangle rect = getVisibleRect();
		Dimension size = getPreferredSize();
		if( rect.width>size.width ) {
			rect.x = 0;
			rect.width=size.width;
		}
		if( rect.height>size.height ) {
			rect.y = 0;
			rect.height=size.height;
		}
		Insets insets = getInsets();
		rect.x -= insets.left;
		rect.y -= insets.top;
		rect.width -= insets.left + insets.right;
		rect.height -= insets.top+insets.bottom;
		Point2D p0 = new Point( rect.x+ rect.width/2, rect.y+rect.height/2 );
		size.width -= insets.left + insets.right;
		size.height -= insets.top+insets.bottom;
		p0 = inverseTransform( p0 );
		AffineTransform flip = (orientation==HORIZONTAL) ?
				new AffineTransform(new double[] { -1., 0., 0., 1., size.getWidth(), 0.}) :
				new AffineTransform(new double[] { 1., 0., 0., -1., 0., size.getHeight()});
		aTrans.preConcatenate( flip );

		insets = getInsets();
		p0 = aTrans.transform( p0, null );
		int x = (int)(p0.getX()-rect.width/2)+insets.left;
		int y = (int)(p0.getY()-rect.height/2)+insets.top;
		invalidate();
		if( scrollPane==null ) {
			repaint();
			return;
		}
		scrollPane.validate();
		JScrollBar sb = scrollPane.getHorizontalScrollBar();
		sb.setValue(x);
		sb = scrollPane.getVerticalScrollBar();
		sb.setValue(y);
	//	revalidate();
		repaint();
	}
	public void rotate( int n_x_90 ) {
		while( n_x_90<0 ) n_x_90 += 4;
		n_x_90 %= 4;
		rotation = (rotation+n_x_90)%4;
		Rectangle rect = getVisibleRect();
		Dimension size = getPreferredSize();
		if( rect.width>size.width ) {
			rect.x = 0;
			rect.width=size.width;
		}
		if( rect.height>size.height ) {
			rect.y = 0;
			rect.height=size.height;
		}
		Insets insets = getInsets();
		rect.width -= insets.left + insets.right;
		rect.height -= insets.top+insets.bottom;
		Point2D p0 = new Point( rect.x+ rect.width/2, rect.y+rect.height/2 );
		p0 = inverseTransform( p0 );
		AffineTransform rotate = new AffineTransform(
				new double[] { 0., 1., -1., 0., 0., 0.});
		for(int k=0 ; k<n_x_90 ; k++ ) {
			size = getPreferredSize();
			insets = getInsets();
			size.height -= insets.top+insets.bottom;
			AffineTransform at = new AffineTransform();
			at.concatenate( rotate );
			at.translate( 0, -size.height );
			aTrans.preConcatenate( at );
			int tmp = rect.height;
			rect.height = rect.width;
			rect.width = tmp;
		}
			int tmp = rect.height;
			rect.height = rect.width;
			rect.width = tmp;
		p0 = aTrans.transform( p0, null );
		insets = getInsets();
		int x = (int)(p0.getX()-rect.width/2); // -insets.left;
		int y = (int)(p0.getY()-rect.height/2); // -insets.top;
		invalidate();
		if( scrollPane==null ) {
			repaint();
			return;
		}
		scrollPane.validate();
		JScrollBar sb = scrollPane.getHorizontalScrollBar();
		sb.setValue(x);
		sb = scrollPane.getVerticalScrollBar();
		sb.setValue(y);
		repaint();
	}
	public double getRotation() {
		return Math.toRadians(90.*rotation);
	}
}
package haxby.map;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

public abstract class ScalableComponent extends JComponent
			implements Zoomable, SwingConstants {
	protected JScrollPane scrollPane = null;
	AffineTransform ATrans;
	protected int width, height;
	protected ScalableComponent() {
		ATrans = new AffineTransform();
	}
	public Dimension getPreferredSize() {
		Point p = new Point( width, height);
		Point2D p0 = ATrans.transform( p, null );
		double[] matrix = new double[6];
		ATrans.getMatrix( matrix );
		double w = Math.abs( p0.getX() );
		double h = Math.abs( p0.getY() );
		p.x = p.y = 0;
		p0 = ATrans.transform( p, null );
		if( Math.abs( p0.getX() ) > w ) w = Math.abs( p0.getX() );
		if( Math.abs( p0.getY() ) > h ) h = Math.abs( p0.getY() );
		Insets insets = getInsets();
		w += insets.left+insets.right;
		h += insets.top+insets.bottom;
		return new Dimension( (int)w, (int)h );
	}
	public AffineTransform getTransform() {
		return (AffineTransform)ATrans.clone();
	}
	public AffineTransform getInverseTransform() {
		try {
			return getTransform().createInverse();
		} catch (Exception ex) {
			return new AffineTransform();
		}
	}
	public Point2D inverseTransform( Point2D mousePoint ) {
		try {
			return getTransform().inverseTransform( mousePoint, null );
		} catch (Exception ex) {
			return mousePoint;
		}
	}
	public Point2D transform( Point2D mapPoint ) {
		return getTransform().transform( mapPoint, null );
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
	Rectangle zoomRect = null;
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
	}
	public void zoomOut( Point p ) {
		doZoom( p, .5 );
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
	public void resetTransform() {
		ATrans = new AffineTransform();
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
		if( p.y>=size.height ) p.x = size.height-1;
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
		ATrans.preConcatenate( at );
		newP = transform( newP );
		newX = (int)newP.getX();
		newY = (int)newP.getY();
		invalidate();
		scrollPane.validate();
		JScrollBar sb = scrollPane.getHorizontalScrollBar();
		sb.setValue(newX);
		sb = scrollPane.getVerticalScrollBar();
		sb.setValue(newY);
		repaint();
	//	revalidate();
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
		ATrans.preConcatenate( stretch );

		p0 = ATrans.transform( p0, null );
		int x = (int)(p0.getX()-rect.width/2);
		int y = (int)(p0.getY()-rect.height/2);
		invalidate();
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
		ATrans.preConcatenate( flip );

		insets = getInsets();
		p0 = ATrans.transform( p0, null );
		int x = (int)(p0.getX()-rect.width/2)+insets.left;
		int y = (int)(p0.getY()-rect.height/2)+insets.top;
		invalidate();
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
	//	rect.x -= insets.left;
	//	rect.y -= insets.top;
		rect.width -= insets.left + insets.right;
		rect.height -= insets.top+insets.bottom;
		Point2D p0 = new Point( rect.x+ rect.width/2, rect.y+rect.height/2 );
		p0 = inverseTransform( p0 );
		AffineTransform rotate = new AffineTransform(new double[] { 0., 1., -1., 0., 0., 0.});
		for(int k=0 ; k<n_x_90 ; k++ ) {
			size = getPreferredSize();
			insets = getInsets();
			size.height -= insets.top+insets.bottom;
			AffineTransform at = new AffineTransform();
			at.concatenate( rotate );
			at.translate( 0, -size.height );
			ATrans.preConcatenate( at );
			int tmp = rect.height;
			rect.height = rect.width;
			rect.width = tmp;
		}
			int tmp = rect.height;
			rect.height = rect.width;
			rect.width = tmp;
		p0 = ATrans.transform( p0, null );
		insets = getInsets();
		int x = (int)(p0.getX()-rect.width/2); // -insets.left;
		int y = (int)(p0.getY()-rect.height/2); // -insets.top;
		invalidate();
		scrollPane.validate();
		JScrollBar sb = scrollPane.getHorizontalScrollBar();
		sb.setValue(x);
		sb = scrollPane.getVerticalScrollBar();
		sb.setValue(y);
	//	revalidate();
		repaint();
	}
}

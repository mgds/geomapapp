package haxby.db.mcs;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;

public class MCSScale extends JComponent
		implements MouseListener,
			MouseMotionListener {
	MCSImage2 image;
	Rectangle bounds;
	Point start;
	public MCSScale() {
		bounds = new Rectangle( 50, 50, 100, 100);
		image = null;
		start = null;
	}
	public MCSScale(MCSImage2 image) {
		this();
		this.image = image;
		if(image==null) return;
		image.addMouseListener(this);
		image.addMouseMotionListener(this);
	}
	public void setEnabled( MCSImage2 image) {
		if(image == this.image)return;
		if( this.image!=null) {
			this.image.removeMouseMotionListener(this);
			this.image.removeMouseListener(this);
			if( image==null ) this.image.repaint();
		} 
		this.image = image;
		if(image!=null) {
			image.addMouseListener(this);
			image.addMouseMotionListener(this);
			image.repaint();
		}
	}
	public Dimension getPreferredSize() {
		return new Dimension( bounds.width, bounds.height);
	}
	public boolean contains( Point p) {
		if( image==null) return false;
		Rectangle r = image.getVisibleRect();
		Insets ins = image.getBorderInsets();
		return bounds.contains( p.x-r.x-ins.left, p.y-r.y-ins.top );
	}
	public void paint(Graphics g) {
		if(image==null)return;
		Graphics2D g2 = (Graphics2D)g;
		AffineTransform at = g2.getTransform();
		g2.translate( bounds.x, bounds.y );
		double xs = 1./image.getXScale();
		double ys = 1./image.getYScale();
		double xSize = 1.;
		double[] factor = new double[] {2., 2.5, 2.};
		int kx=0;
		while( xSize*factor[kx]*xs < bounds.width-2.) {
			xSize*=factor[kx];
			kx = (kx+1)%3;
		}
		int ky=0;
		double ySize = 1.;
		while( ySize*factor[ky]*ys < bounds.height-12.) {
			ySize*=factor[ky];
			ky = (ky+1)%3;
		}
		GeneralPath path = new GeneralPath();
		if( ySize<=xSize ) {
			path.moveTo( 4f, (float)(bounds.height-13-(int)(ySize*ys)) );
			path.lineTo( 4f, (float)(bounds.height-13) );
			path.lineTo( 4f+(float)(ySize*xs), (float)(bounds.height-13) );
			path.closePath();
		} else {
			path.moveTo( 4f, (float)(bounds.height-13) );
			path.lineTo( 4f+(float)(xSize*xs), (float)(bounds.height-13) );
			path.lineTo( 4f, (float)(bounds.height-13-(int)(xSize*ys)) );
			path.closePath();
		}
		Color black, white, gray;
		if( image.isRevVid() ) {
			black = Color.white;
			white = Color.black;
			gray = new Color( 1.f, 1.f, 1.f, .5f);
		} else {
			black = Color.black;
			white = Color.white;
			gray = new Color( 0.f, 0.f, 0.f, .25f);
		}
		g.setColor( gray );
		g2.fill(path);
		g.setColor(white);
		g.fillRect(0, bounds.height-14, 2+(int)(xSize*xs), 4);
		g.fillRect(0, bounds.height-14-(int)(ySize*ys), 4, (int)(ySize*ys));
		g.setColor(black);
		g.fillRect(1, bounds.height-13, (int)(xSize*xs), 2);
		g.fillRect(1, bounds.height-13-(int)(ySize*ys), 2, (int)(ySize*ys));
		String anot = (int)xSize +" m";
		g.setFont( new Font("SansSerif", Font.PLAIN, 12 ) ) ;
		FontMetrics fm = g.getFontMetrics();
		int x = 1 + (int)(xSize*xs/2.) - fm.stringWidth(anot) / 2;
		int y = bounds.height-6-(int)(ySize*ys);
		g.setColor(white);
		g.drawString( anot, x-1, bounds.height);
		g.drawString( anot, x+1, bounds.height);
		g.drawString( anot, x-1, bounds.height-2);
		g.drawString( anot, x+1, bounds.height-2);
		g.setColor(black);
		g.drawString( anot, x, bounds.height-1);
		anot = (int)ySize +" m";
		g.setColor(white);
		g.drawString( anot, 5, y-1 );
		g.drawString( anot, 7, y-1 );
		g.drawString( anot, 5, y+1 );
		g.drawString( anot, 7, y+1 );
		g.setColor(black);
		g.drawString( anot, 6, y );
		g2.setTransform( at);
	}
	public void mouseClicked( MouseEvent evt) {
	}
	public void mousePressed( MouseEvent evt) {
		if( evt.isControlDown() || evt.isPopupTrigger() 
				|| !contains( evt.getPoint()) ) {
			start = null;
			return;
		}
		image.setCursor( Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR) );
		start = evt.getPoint();
	}
	public void mouseReleased( MouseEvent evt) {
		if( evt.isControlDown() || start == null) {
			start = null;
			return;
		}
		image.setCursor( Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR) );
		Point end = evt.getPoint();
		Rectangle r = image.getVisibleRect();
		Insets ins = image.getBorderInsets();
		r.width -= ins.left+ins.right+bounds.width;
		r.height -= ins.top+ins.bottom+bounds.height;
		bounds.x += end.x-start.x;
		if(bounds.x>r.width)bounds.x=r.width;
		if(bounds.x <0 ) bounds.x=0;
		bounds.y += end.y-start.y;
		if(bounds.y>r.height)bounds.y=r.height;
		if(bounds.y <0 ) bounds.y=0;
		image.repaint();
	}
	public void mouseEntered( MouseEvent evt) {
	}
	public void mouseExited( MouseEvent evt) {
		start = null;
	}
	public void mouseMoved( MouseEvent evt) {
	}
	public void mouseDragged( MouseEvent evt) {
		if( evt.isControlDown() ) {
			start = null;
			return;
		}
	}
}

package haxby.db.scs;

import haxby.map.Zoomable;
import haxby.map.Zoomer;
import haxby.util.URLFactory;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.imageio.ImageIO;

//import com.sun.image.codec.jpeg.JPEGCodec;
//import com.sun.image.codec.jpeg.JPEGImageDecoder;

public class SCSImage extends JComponent
		implements Zoomable,
			//	ActionListener, 
			//	MouseListener, 
				MouseMotionListener {

//	Vector panels;
	SCSCruise cruise;
	int panel;
//	SCSBorder border;
//	SCSImage xImage;
//	Scroller scroller;
	JScrollPane scrollPane = null;
	BufferedImage image;
	double zoomX, zoomY;
	double zoom;
	public SCSImage() {
	//	border = null;
	//	panels = null;
	//	xImage = null;
		image = null;
		zoom = zoomX = zoomY = .25;
		Zoomer zoomer = new Zoomer(this);
		addMouseListener(zoomer);
		addMouseMotionListener(zoomer);
		addKeyListener(zoomer);
	}
	public void setPanel( String urlName, SCSCruise cruise, int panel ) throws IOException {
		URL url = URLFactory.url( urlName );
		//JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder( url.openStream() );
		image = ImageIO.read(url.openStream());
		//image = decoder.decodeAsBufferedImage();
		if( scrollPane!=null ) {
			invalidate();
			scrollPane.revalidate();
		}
		this.cruise = cruise;
		this.panel = panel;
		repaint();
	}
	public void addNotify() {
		super.addNotify();
		Container c = getParent();
		while( c!=null ) {
			if( c instanceof JScrollPane ) {
				scrollPane = (JScrollPane)c;
				return;
			}
			c = c.getParent();
		}
	}
	public Dimension getPreferredSize() {
		if( image==null ) return new Dimension(300, 200);
		Dimension dim = new Dimension(0,0);
		zoomX = zoomY = zoom;
		dim.width = (int) Math.ceil( image.getWidth()*zoomX );
		dim.height = (int) Math.ceil( image.getHeight()*zoomY );
		return dim;
	}
	public void setXY(Point p) {
	}
	public void setRect(Rectangle rect) {
	}
	public void zoomTo(Rectangle rect) {
	}
	public void zoomIn( Point p ) {
		doZoom( p, 2d );
	}
	public void zoomOut( Point p ) {
	//	Dimension dim = getPreferredSize();
	//	if( getVisibleRect().contains( new Rectangle(0,0,dim.width, dim.height ))) return;
		doZoom( p, .5d );
	}
	public void doZoom( Point p, double factor ) {
		Rectangle rect = getVisibleRect();
		double x = p.getX() / zoom;
				double y = p.getY() / zoom;
				double w = rect.getWidth();
				double h = rect.getHeight();
				zoom *= factor;
				int newX = (int) (x*zoom - w*.5d);
				int newY = (int) (y*zoom - h*.5d);
				invalidate();
				scrollPane.validate();
				JScrollBar sb = scrollPane.getHorizontalScrollBar();
				sb.setValue(newX);
				sb = scrollPane.getVerticalScrollBar();
				sb.setValue(newY);
				repaint();
		}
	public void paint(Graphics g) {
		if( image==null ) return;
				Graphics2D g2 = (Graphics2D)g;
				g2.scale(zoom, zoom);
				g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION,
										RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.drawImage( image, 0, 0, this);
		}
	public void mouseDragged( MouseEvent evt ) {
		mouseMoved( evt );
	}
	public void mouseMoved( MouseEvent evt ) {
		if( image==null ) return;
		double x = evt.getX() / zoomX;
	}
	public Double getZoomValue() {
		return null;
	}
	public void zoomSpeed(Point p, Double d) {
	}
}
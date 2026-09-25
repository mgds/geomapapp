package haxby.db;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import haxby.map.Zoomable;

public class XYGraph extends JComponent
			implements Zoomable,
				Printable,
				Scrollable {

	private static final long serialVersionUID = 1L;
	XYPoints xy;
	int dataIndex;
	Axes axes = null;
	double width;
	double height;
	double[] xRange, yRange;
	double xScale, yScale;
	double zoom;
	JScrollPane scPane = null;
	boolean tracksWidth, tracksHeight;
	public XYGraph( XYPoints pts, int dataIndex ) {
		setPoints( pts, dataIndex );
		zoom = 1.;
		tracksWidth = tracksHeight = false;
	}
	public void setPoints( XYPoints pts, int dataIndex ) {
		xy = pts;
		this.dataIndex = dataIndex;
		xRange = xy.getXRange( dataIndex );
		yRange = xy.getYRange( dataIndex );
		xScale = xy.getPreferredXScale( dataIndex );
		yScale = xy.getPreferredYScale( dataIndex );
		width = (xRange[1] - xRange[0]) *xScale;
		if(width<0.) {
			width = -width;
			xScale = -xScale;
		}
		height = (yRange[0] - yRange[1]) *yScale;
		if(height<0.) {
			height = -height;
			yScale = -yScale;
		}
		int sides = Axes.LEFT | Axes.BOTTOM |Axes.RIGHT | Axes.TOP;
		if( axes != null ) sides = axes.sides;
		axes = new Axes( xy, dataIndex,  sides);
		setBackground( Color.white );
	}
	public void setFont(Font font) {
		axes.setFont(font);
	}
	public void setAxesSides( int sides ) {
		axes.setSides( sides);
		if( isVisible() ) repaint();
	}
	public void setZoom(double z) {
		zoom = z;
	}
	public double getZoom() {
		return zoom;
	}
// methods implementing Zoomable
	public void zoomIn( Point p ) {
		doZoom( p, 2. );
	}
	public void zoomOut( Point p ) {
		doZoom( p, .5 );
	}
	public void zoomTo( Rectangle r ) {
	}
	public void setRect( Rectangle r ) {
	}
	public void setXY( Point p ) {
	}
	void doZoom( Point p, double factor ) {
		if( scPane == null || ( tracksWidth && tracksHeight ))return;
		Insets ins = axes.getInsets();
		Rectangle rect = getVisibleRect();
		double x = (double)(p.x-ins.left) / zoom;
		double y = (double)(p.y-ins.top) / zoom;
		double w = (double) (rect.width - ins.left - ins.right);
		double h = (double) (rect.height - ins.top - ins.bottom);
		zoom *= factor;
		int newX = (int) (x*zoom - w*.5d);
		int newY = (int) (y*zoom - h*.5d);
		invalidate();
		scPane.validate();
		JScrollBar sb;
		if(!tracksWidth) {
			sb = scPane.getHorizontalScrollBar();
			sb.setValue(newX);
		}
		if(!tracksHeight) {
			sb = scPane.getVerticalScrollBar();
			sb.setValue(newY);
		}
		revalidate();
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
		Insets ins = axes.getInsets();
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
	public void addNotify() {
		super.addNotify();
		Container c = getParent();
		while( !( c instanceof JScrollPane) ) {
			c = c.getParent();
			if( c==null ) {
				scPane=null;
				return;
			}
		}
		scPane = (JScrollPane)c;
	}
	public double getXAt( Point2D p ) {
		Insets insets = axes.getInsets();
		Rectangle r = getVisibleRect();
		double w = (double)(r.width - insets.left - insets.right);
		if( tracksWidth  || scPane==null) {
			return xRange[0] + (xRange[1]-xRange[0])*(p.getX()-(double)insets.left)/w;
		} else {
			return xRange[0] + (p.getX()-(double)insets.left) / (xScale*zoom);
		}
	}
	public double getYAt( Point2D p ) {
		Insets insets = axes.getInsets();
		Rectangle r = getVisibleRect();
		double h = (double)(r.height - insets.top - insets.bottom);
		if( tracksHeight  || scPane==null) {
			return yRange[1] - (yRange[1]-yRange[0])*(p.getY()-(double)insets.top)/h;
		} else {
			return yRange[1] - (p.getY()-(double)insets.top) / (yScale*zoom);
		}
	}
	public void removeNotify() {
		super.removeNotify();
		scPane = null;
	}
	public Dimension getPreferredSize() {
		Insets ins = axes.getInsets();
		int w = ins.left + ins.right + (int)Math.ceil(width);
		int h = ins.top + ins.bottom + (int)Math.ceil(height);
		if( !tracksWidth ) {
			w = ins.left + ins.right + (int)Math.ceil( zoom*width);
		}
		if( !tracksHeight ) {
			h = ins.top + ins.bottom + (int)Math.ceil( zoom*height);
		}
		return new Dimension( w, h );
	}

	public void paintComponent( Graphics graphics ) {
		paintComponent(graphics,true);
	}
	
	public void paintComponent( Graphics graphics , boolean paintAxes) {
		paintComponent(graphics, paintAxes, true);
	}
		
	public void paintComponent( Graphics graphics , boolean paintAxes, boolean visibleOnly) {
		Graphics2D g = (Graphics2D) graphics;
		if(!printing) g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
						RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		//set rectangle based on whether we are plotting full plot, or visible area only
		Rectangle r;
		if (visibleOnly) {
			r = getVisibleRect();
		} else {
			r =  new Rectangle(0, 0, getWidth(), getHeight());
		}
		g.setColor(Color.white);
		if (paintAxes) g.fill( r );
		Rectangle2D.Double bounds = new Rectangle2D.Double();
		Insets ins = axes.getInsets();
		double w = (double)(r.width - ins.left - ins.right);
		double h = (double)(r.height - ins.top - ins.bottom);
		double sX, sY;
		if( tracksWidth  || scPane==null) {
			bounds.x = xRange[0];
			bounds.width = xRange[1]-xRange[0];
			// May need to extend width to make sure tick labels are fully displayed
			bounds.width = axes.getXAxisWidth(g, bounds, r);
			sX = w / bounds.width;
		} else {
			bounds.x = xRange[0] + (double)(r.x) / (xScale*zoom);
			bounds.width = w / (xScale*zoom);
			sX = xScale*zoom;
		}
		if( tracksHeight || scPane==null ) {
			bounds.y = yRange[1];
			bounds.height = yRange[0]-yRange[1];
			sY = h / bounds.height;
		} else {
			bounds.y = yRange[1] + (double)(r.y) / (yScale*zoom);
			bounds.height = (double)(r.height - ins.top - ins.bottom) / (yScale*zoom);
			sY = yScale*zoom;
		}
	//	System.out.println( "bounds:\t"+bounds.x 
	//			+"\t"+ bounds.y +"\t"+ bounds.width 
	//			+"\t"+ bounds.height +"\nscales\t" + sX +"\t"+ sY);
		if(printing) {
			double scale = (r.getWidth()-ins.left-ins.right)
				/(printRect.getWidth()-ins.top-ins.bottom);
			sX /= scale;
			sY /= scale;
			r = printRect;
		}
		if (paintAxes) axes.drawAxes( g, bounds, r );
		g.clipRect( r.x+ins.left, r.y+ins.top, 
			r.width-ins.left-ins.right, 
			r.height-ins.top-ins.bottom );
		g.translate( r.x+ins.left, r.y+ins.top );
		xy.plotXY( g, bounds, sX, sY, dataIndex );
	}
	boolean printing = false;
	Rectangle printRect;
	public int print(Graphics g, PageFormat fmt, int pageNo) {
		printing = true;
		if( pageNo>1 ) {
			printing = false;
			return NO_SUCH_PAGE;
		}
		Rectangle r = getVisibleRect();
		double w = fmt.getImageableWidth();
		double h = fmt.getImageableHeight();
		double x = fmt.getImageableX();
		double y = fmt.getImageableY();
		Insets ins = axes.getInsets();
		double ww = w - ins.left - ins.right;
		double hh = h - ins.top - ins.bottom;
		double rw = (double)(r.width - ins.left - ins.right);
		double rh = (double)(r.height - ins.top - ins.bottom);
		double scale = Math.min( rh/rw, rw/rh);
		double newH = ww * scale;
		double newW = hh * scale;
		if( !tracksWidth || !tracksHeight ) {
			int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
			scale = 72./dpi;
		}
		newW = rw*scale;
		newH = rh*scale;
		x -= (newW-ww)/2;
		y -= (newH-hh)/2;
		w = newW + ins.left + ins.right;
		h = newH +ins.top + ins.bottom;
		printRect = new Rectangle( (int)x,
					(int)y,
					(int)w,
					(int)h );
		paintComponent(g);
		printing = false;
		return PAGE_EXISTS;
	}
	/**
	 * Get the visible portion of image
	 */
	public BufferedImage getImage() {
		Rectangle r = getVisibleRect();
		BufferedImage image = new BufferedImage(r.width, r.height,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.translate( -r.x, -r.y);
		paintComponent( g );
		return image;
	}
	
	/**
	 * Get the full image, not just the visible portion
	 */
	public BufferedImage getFullImage() {
		Rectangle r = new Rectangle (0, 0, getWidth(), getHeight());
		BufferedImage image = new BufferedImage(r.width, r.height,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.translate( -r.x, -r.y);
		paintComponent( g, true, false );
		return image;
	}
	
	public XYPoints getXYPoints(){
		return xy;
	}

	public void dispose() {
		xy = null;

		Container c = getParent();
		while (!(c instanceof JDialog) &&
				!(c instanceof JFrame) && 
				c != null) c = c.getParent();
		if (c != null)
			if (c instanceof JDialog) 
			((JDialog)c).dispose();
			else if (c instanceof JFrame) 
				((JFrame)c).dispose();
	}
	public Double getZoomValue() {
		return null;
	}
	public void zoomSpeed(Point p, Double d) {
	}
	
	/*
	 * return the double precision width of the graph
	 */
	public double getGraphWidth() {
		return width;
	}
	public void setXRange ( double[] xRange ) {
		this.xRange = xRange;
	}
	public void setYRange ( double[] yRange ) {
		this.yRange = yRange;
	}
}

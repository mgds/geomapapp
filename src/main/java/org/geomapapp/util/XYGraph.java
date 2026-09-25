package org.geomapapp.util;

// import haxby.map.*;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.event.MouseInputAdapter;

public class XYGraph extends JComponent
			implements Zoomable,
				Printable,
				Scrollable	{
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
	MouseInputAdapter mouse;
	JToggleButton digitize;

	public XYGraph( XYPoints pts, int dataIndex ) {
		setPoints( pts, dataIndex );
		zoom = 1.;
		tracksWidth = tracksHeight = false;
		initMouse();
	}
	public int getDataIndex() {
		return dataIndex;
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
		if( scPane!=null ) {
			invalidate();
			scPane.validate();
		}
	}
	public void setFont(Font font) {
		axes.setFont(font);
	}

	public double getYScale() {
		return yScale;
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
	
	public void center( Point p ) {
		doZoom( p, 1. );
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
	public Graphics2D getGraphics2D() {
		Graphics2D g = (Graphics2D)getGraphics();
		Rectangle r = getVisibleRect();
		Insets ins = axes.getInsets();
	//	g.clipRect( r.x+ins.left, r.y+ins.top, 
	//		r.width-ins.left-ins.right, 
	//		r.height-ins.top-ins.bottom );
		g.translate( r.x+ins.left, r.y+ins.top );
		return g;
	}
	public double[] getPlotInfo() {
		Rectangle r = getVisibleRect();
		Rectangle2D.Double bounds = new Rectangle2D.Double();
		Insets ins = axes.getInsets();
		double w = (double)(r.width - ins.left - ins.right);
		double h = (double)(r.height - ins.top - ins.bottom);
		double sX, sY;
		if( tracksWidth  || scPane==null) {
			bounds.x = xRange[0];
			bounds.width = xRange[1]-xRange[0];
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
		return new double[] {sX, sY, bounds.x, bounds.y};
	}
	public void paintComponent( Graphics graphics ) {
		line = null;
		Dimension dim = getPreferredSize();
		Graphics2D g = (Graphics2D) graphics;
		if(!printing) g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
						RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		Rectangle r = getVisibleRect();
		if( !imaging ) {
			g.setColor(Color.white);
			g.fill( r );
		}
		Rectangle2D.Double bounds = new Rectangle2D.Double();
		Insets ins = axes.getInsets();
		double w = (double)(r.width - ins.left - ins.right);
		double h = (double)(r.height - ins.top - ins.bottom);
		double sX, sY;
		if( tracksWidth  || scPane==null) {
			bounds.x = xRange[0];
			bounds.width = xRange[1]-xRange[0];
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
		if(printing) {
			double scale = (r.getWidth()-ins.left-ins.right)
				/(printRect.getWidth()-ins.top-ins.bottom);
			sX /= scale;
			sY /= scale;
			r = printRect;
		}
		axes.drawAxes( g, bounds, r );
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
		Graphics2D g2 = (Graphics2D)g;
		Dimension dim = getPreferredSize();
		Rectangle r = getVisibleRect();
	//	if(r.width>dim.width) r.width = dim.width;
	//	if(r.height>dim.height) r.height = dim.height;
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
	public BufferedImage getImage() {
		return getImage(BufferedImage.TYPE_INT_RGB);
	}
	boolean imaging=false;
	public BufferedImage getImage(int type) {
		Rectangle r = getVisibleRect();
		BufferedImage image = new BufferedImage(r.width, r.height, type);
		Graphics2D g = image.createGraphics();
		g.translate( -r.x, -r.y);
	//	imaging = true;
		paintComponent( g );
	//	imaging=false;
		return image;
	}
	void initMouse() {
		mouse = new MouseInputAdapter() {
		//	public void mouseExited(MouseEvent e) {
		//		setCursor(0);
		//	}
			public void mouseMoved(MouseEvent e) {
				if( canDigitize() )return;
				checkEdge(e);
			}
			public void mouseDragged(MouseEvent e) {
				if( canDigitize() )return;
				dragEdge(e);
			}
			public void mouseReleased(MouseEvent e) {
				if( canDigitize() )return;
				resize(e);
			}
			public void mouseClicked(MouseEvent e) {
				if( canDigitize() )return;
				if( e.isControlDown() )return;
				
//				1.4.2: Do not zoom back out when the graph is clicked
//				reset();
				
			}
			public void mousePressed(MouseEvent e) {
				if( canDigitize() )return;
				initResize(e);
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
		digitize = new JToggleButton(Icons.getIcon(Icons.DIGITIZE, false));
		digitize.setSelectedIcon(Icons.getIcon(Icons.DIGITIZE, true));
		digitize.setToolTipText("edit age model");
		digitize.setBorder( null);
	}
	public JToggleButton getDigitizeButton() {
		return digitize;
	}
	public boolean canDigitize() {
		return digitize.isSelected();
	}
	int[] getWESN() {
		Insets i = axes.getInsets();
		Rectangle r = getVisibleRect();
		int[] wesn = new int[] {
				r.x+i.left,
				r.x+r.width-i.right,
				r.y+i.top,
				r.y+r.height-i.bottom };
		return wesn;
	}
	void checkEdge(MouseEvent e) {
		if( e.isControlDown() )return;
		if( !(xy instanceof ScalableXYPoints) )return;
		int[] wesn = getWESN();
		int cursor = 0;
		int x = e.getX();
		int y = e.getY();
		if( tracksWidth || scPane==null) {
			if( (int)Math.abs(x-wesn[0])<2 ) cursor=10;
			else if( (int)Math.abs(x-wesn[1])<2 ) cursor=11;
		}
		if( (tracksHeight || scPane==null) && cursor==0 ) {
			if( (int)Math.abs(y-wesn[2])<2 ) cursor=8;
			else if( (int)Math.abs(y-wesn[3])<2 ) cursor=9;
		}
		setCursor(cursor);
	}
	int cursor=0;
	Line2D line;

	void setCursor(int cursor) {
		if( cursor==this.cursor )return;
		setCursor( Cursor.getPredefinedCursor(cursor));
		this.cursor = cursor;
	}
	void dragEdge(MouseEvent e) {
		if( e.isControlDown() )return;
		if( !(xy instanceof ScalableXYPoints) )return;
		if( cursor==0 )return;
		drawLine();
		int[] wesn = getWESN();
		Rectangle r = getVisibleRect();
		Point p = e.getPoint();
		if( cursor>=10 ) {
			if( cursor==10 ) {
				if(p.x>wesn[1]-2) p.x=wesn[1]-2;
			} else {
				if(p.x<wesn[0]+2) p.x=wesn[0]+2;
			}
			line = new Line2D.Double(p.x, r.y, p.x, r.y+r.height);
		} else {
			line = new Line2D.Double(r.x, p.y, r.x+r.width, p.y);
		}
		drawLine();
	}
	void drawLine() {
		if( line==null )return;
		synchronized (getTreeLock()) {
			Graphics2D g = (Graphics2D)getGraphics();
			g.setXORMode( Color.white );
			g.draw(line);
		}
	}
	void reset() {
		if( !(xy instanceof ScalableXYPoints) )return;
		ScalableXYPoints pts = (ScalableXYPoints)xy;
		pts.resetRanges(dataIndex );
		setPoints( pts, dataIndex );
		repaint();
	}
	void resize(MouseEvent e) {
		if( e.isControlDown() )return;
		if( !(xy instanceof ScalableXYPoints) )return;
		if( cursor==0 )return;
		ScalableXYPoints pts = (ScalableXYPoints)xy;
		drawLine();
		line = null;
		Point pt = e.getPoint();
		int[] wesn = getWESN();
		if( cursor>=10 ) {
			double[] range = xy.getXRange(dataIndex);
			double[] r = new double[] {range[0], range[1]};
			if( cursor==10 ) {
				if(pt.x>wesn[1]-2) pt.x=wesn[1]-2;
				r[0] = getXAt(pt);
			} else {
				if(pt.x<wesn[0]+2) pt.x=wesn[0]+2;
				r[1] = getXAt(pt);
			}
		//	System.out.println( r[0]+"\t"+ r[1]);
			pts.setXRange(dataIndex, r);
		} else {
			double[] range = xy.getYRange(dataIndex);
			double[] r = new double[] {range[0], range[1]};
			if( cursor==8 ) {
				r[1] = getYAt(pt);
			} else {
				r[0] = getYAt(pt);
			}
			pts.setYRange(dataIndex, r);
		}
		setCursor(0);
		setPoints( pts, dataIndex );
		repaint();
	}
	void initResize(MouseEvent e) {
		if( e.isControlDown() )return;
		if( !(xy instanceof ScalableXYPoints) )return;
	}
}
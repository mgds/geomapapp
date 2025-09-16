package haxby.db.radar;

import haxby.db.mcs.*;
import haxby.db.xmcs.XMLine;


import haxby.dig.AnnotationObject;
import haxby.dig.Digitizer;
import haxby.dig.LineSegmentsObject;
import haxby.dig.LineType;
import haxby.map.*;
import haxby.image.*;
import haxby.util.*;

import java.text.NumberFormat;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

//import com.sun.image.codec.jpeg.*;

public class RImage extends haxby.util.ScaledComponent
		implements ActionListener, MouseListener, MouseMotionListener,  Zoomable {
	RLine line;
	RImage otherImage;
//	RSave save;
	RBorder border;
//	RScale scale;
	Scroller scroller;
	ScalableImage image;
	int width, height;
	JPanel panel = null;
	double[] cdpInterval;
	double[] tRange;
	int xRep, yRep, xAvg, yAvg;
	boolean rev=false;
	boolean flip=false;
	boolean saving=false;
	Zoomer zoomer;
	JToggleButton zoomInTB;
	JToggleButton zoomOutTB;
	JToggleButton digitizeTB;
	private String selectedPingFile;
	private  String uidURLString = PathUtil.getPath("PORTALS/RADAR_LOOKUP",
			MapApp.BASE_URL+"/data/portals/sp_radar/radar_lookup/");
	private String RADAR_PATH = PathUtil.getPath("PORTALS/RADAR_PATH",
			MapApp.BASE_URL+"/data/portals/sp_radar/");
	private String RADAR_EXP_LIST = PathUtil.getPath("PORTALS/RADAR_EXP_LIST",
			MapApp.BASE_URL+"/data/portals/sp_radar/radar_lookup/expedition_list.txt");
	private  String selectedDataUID;

	Digitizer dig;

	public RImage() {
		border = null;
		image = null;
		cdpInterval = null;
		tRange = null;
		scroller = null;
		width = 100;
		height = 50;
		xRep = yRep = xAvg = yAvg = 1;
		panel = new JPanel(new BorderLayout());
		dig = new Digitizer(this);
		JScrollPane toolbarScrollPane = new JScrollPane(getToolBar());
		//toolbarScrollPane.setPreferredSize(new Dimension(50,50));
		panel.add(toolbarScrollPane, "North");
		JScrollPane sp = new JScrollPane(this);
		scroller = new Scroller(sp, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		panel.add(sp,"Center");
		zoomer = new Zoomer(this);
		addMouseListener(zoomer);
		addMouseListener(this);
		addMouseMotionListener(zoomer);
		addMouseMotionListener(this);
		addKeyListener(zoomer);
		line = null;
		otherImage = null;		
	}
	void disposeImage() {
		image = null;
	}
	public void setOtherImage( RImage other ) {
		otherImage = other;
	}
	public void loadImage( RLine line ) throws IOException {
		if( line.getZRange()==null ) throw new IOException(" no data for "+line.getID());
		this.line = line;
		border = new RBorder(this);
		xRep = yRep = 1;
		xAvg = yAvg = 8;
		image = null;
		System.gc();
		width = 100;
		height = 50;
		invalidate();
		DataInputStream in = null;
		border.setTitle();
		cdpInterval = line.getCDPRange();
		tRange = line.getZRange();
		/*MapApp.getBaseURL() + "antarctic/radar/"*/
		URL url = URLFactory.url(RADAR_PATH + line.getCruiseID().trim() 
				+ "/img/" + line.getID().trim() + ".r2.gz" );
		InputStream urlIn = url.openStream();
		in = new DataInputStream(
			new GZIPInputStream(
			new BufferedInputStream(urlIn)));
		if( in.readInt() != R2.MAGIC ) throw new IOException("unknown format");
		width = in.readInt();
		height = in.readInt();
		if( in.readInt() != 2 ) throw new IOException("unknown format");
		int size = in.readInt();
		for( int i=0 ; i<3 ; i++) in.readInt();
		byte[] bitmap = new byte[size];
		int pos = 0;
		int n=0;
		try {
			in.readFully(bitmap);
		} catch (IOException ex) {
		}
		if(line.getCruiseID().trim().equalsIgnoreCase("AGAP")) {
			/*MapApp.getBaseURL() + "antarctic/radar/"*/
			url = URLFactory.url(RADAR_PATH	+ line.getCruiseID().trim() 
					+"/img/"+ line.getID().trim() + ".jpg" );
			//BufferedImage im = ImageIO.read(url);
			//ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			
			
				
			//width = im.getWidth();
			//height = im.getHeight();
			Runtime.getRuntime().gc();
			//image = new R2(((DataBufferByte)im.getRaster().getDataBuffer()).getData(),width,height);
			image = new JPEGimage(url, width, height);
			
		}else{
		image = new R2(bitmap, width, height);}
		
		image.setRevVid( rev );
		image.setFlip(flip);
		try {if(in != null) 
			in.close();
		} catch( Exception ex1 ) {
		}
		if(scroller != null) {
			invalidate();
			synchronized(getTreeLock()) {
				scroller.validate();
			}
			scroller.scrollTo(new Point(0, 0));
			panel.repaint();
		}
		otherImage.repaint();
	}
	public String getCruiseID() {
		return line.getCruise().getID();
	}
	public String getID() {
		return line.getID();
	}
	public String toString() {
		return line.getID();
	}
	public RLine getLine() {
		return line;
	}
	public double getXScale() {
		double scale = line.getCDPSpacing() * (cdpInterval[1]-cdpInterval[0]) /
				(double) (width * xRep/xAvg);
		return scale;
	}
	public double getYScale() {
		return (tRange[1]-tRange[0]) / (double)(height*yRep/yAvg);
	}
	public void setImageScales( double cdp1, double cdp2, double topMillis, double bottomMillis) {
		cdpInterval = new double[] { cdp1, cdp2 };
		tRange = new double[] { topMillis, bottomMillis };
		border = new RBorder(this);
	}
	public Dimension getPreferredSize() {
		if(image==null) return new Dimension( 1000, 200 );
		Dimension size = new Dimension( width*xRep/xAvg, height*yRep/yAvg);
		if(border != null) {
			Insets ins = border.getBorderInsets(this);
			size.width += ins.left + ins.right;
			size.height += ins.top + ins.bottom;
		}
		return size;
	}
	public Insets getBorderInsets() {
		if( border==null ) return new Insets(0,0,0,0);
		return border.getBorderInsets(this);
	}
	public Dimension getMinimumSize() {
		return new Dimension( 500, 100);
	}
	public int[] getVisibleSeg() {
		if( !isVisible() || line==null || image==null ) return new int[] {0, 0};
		Rectangle rect = getVisibleRect();
		Insets ins = border.getBorderInsets(this);
		rect.width -= ins.left + ins.right;
		rect.x += ins.left;
		int[] seg = new int[2];
		if( flip ) {
			seg[0] = cdpAt(rect.x+rect.width);
			seg[1] = cdpAt(rect.x);
		} else {
			seg[0] = cdpAt(rect.x);
			seg[1] = cdpAt(rect.x+rect.width);
		}
		return seg;
	}
	public void drawVisibleSeg() {
		if( !isVisible() || line==null || image==null ) return;
		Rectangle rect = getVisibleRect();
		int[] seg = getVisibleSeg();
		line.drawSeg( seg[0], seg[1] );
	}
	public void paint(Graphics g) {
		if(image==null) {
			g.drawString( "no image loaded", 10, 50 );
			return;
		}
		line.drawCDP( -1 );
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		drawVisibleSeg();
		Rectangle rect = getVisibleRect();
		Dimension dim = getPreferredSize();
		if( rect.width > dim.width ) rect.width = dim.width;
		if( rect.height > dim.height ) rect.height = dim.height;
		if(image==null) return;
		Insets ins = (border==null) ? (new Insets(0,0,0,0)) : border.getBorderInsets(this);
		int[] seg = getVisibleSeg();
		double scale = (double)(rect.width) / (double)(seg[1]-seg[0]);
		AffineTransform crsAT = null;
		if(border != null) {
			Dimension size = getPreferredSize();
			Rectangle bounds = new Rectangle(0, 0, size.width, size.height);
			if(rect.contains(bounds)) {
				rect=bounds;
				g.clipRect(rect.x, rect.y, rect.width, rect.height);
			}
		//	ins = border.getBorderInsets(this);
			border.paintBorder(this, g, rect.x, rect.y, rect.width, rect.height);
		//	seg = getVisibleSeg();
			g.setFont( new Font("SansSerif", Font.BOLD, 10));
			FontMetrics fm = g.getFontMetrics();
			if( seg[1]>seg[0]) {
				AffineTransform at = g2.getTransform();
				scale = (double)(rect.width-ins.left-ins.right) / 
					(double)(seg[1]-seg[0]);
				if(isRevVid()) {
					g2.setColor( Color.yellow);
				} else {
					g2.setColor( Color.blue);
				}
				for( int k=0 ; k<line.crossings.size() ; k++) {
					RCrossing crs = (RCrossing)line.crossings.get(k);
					if( crs.cdp1<seg[0] || crs.cdp1>seg[1])continue;
					int x = isFlip() ?
						rect.x+ins.left+(int)Math.rint((-crs.cdp1+seg[1])*scale) :
						rect.x+ins.left+(int)Math.rint((crs.cdp1-seg[0])*scale);
					g2.translate( x, rect.y+ins.top );
					if( crs.cross == otherImage.getLine() ) {
						crsAT = g2.getTransform();
						g2.drawLine( 0, rect.height, 0, -12 );
					} else {
						g2.drawLine( 0, 0, 0, -12 );
					}
				//	g2.fill(path);
					g2.drawString( crs.cross.toString(), 
						-fm.stringWidth(crs.cross.toString())/2, -14);
					g2.setTransform(at);
				}
			}

			g.setColor(Color.lightGray);
			g.drawLine( rect.x+2, rect.y+2, rect.x+ins.left-2, rect.y+ins.top-2 );
			if(isRevVid()) g2.setColor(Color.white);
			else g2.setColor(Color.black);
			g.setFont( new Font("SansSerif", Font.PLAIN, 10));
			fm = g2.getFontMetrics();
			int w = fm.stringWidth( xRep+"" );
			int x = rect.x + 14;
			int y = rect.y + 10;
			g.drawString( xRep+"", x, y);
			g.drawLine( w+x-3, y+4, w+x+5, y-4);
			g.drawString( xAvg+"", x+2+w, y+9);
			w = fm.stringWidth( yRep+"");
			x = rect.x +1;
			y = rect.y + ins.top - 16;
			g.drawString( yRep+"", x, y);
			g.drawLine( w+x-3, y+4, w+x+5, y-4);
			g.drawString( yAvg+"", x+2+w, y+9);
			g.translate(ins.left, ins.top);
			g.clipRect(rect.x, rect.y, 
				rect.width-ins.left-ins.right, 
				rect.height-ins.top-ins.bottom);
		}
		if(!image.isFlip() && getZoomX()==1 && getZoomY()==1) {
			g2.drawImage(image.getImage(), 0, 0, this);
		} else {
			rect = image.getImageableRect(g2.getClipBounds(), xAvg, yAvg, xRep, yRep);
			if(rect.width >0 && rect.height>0 ) {
				BufferedImage im = image.getScaledImage(rect, xAvg, yAvg, xRep, yRep);
				g2.drawImage( im, rect.x, rect.y, this);
			}
		}
		rect = getVisibleRect();
		if( crsAT != null ) {
			AffineTransform trans = g2.getTransform();
			g2.setTransform( crsAT );
			if(isRevVid()) {
				g2.setColor( Color.yellow);
			} else {
				g2.setColor( Color.blue);
			}
			g2.drawLine( 0, rect.height, 0, 0 );
			g2.setTransform( trans );
		}
		lastTime = Double.NaN;
		otherImage.drawTime( lastTime );
		
		if ( dig != null ) {
			dig.draw( g2 );
		}
	}
	public void saveJPEG(OutputStream out) throws IOException {
		if(image==null) throw new IOException("no image loaded");
		Rectangle rect = getVisibleRect();
		Dimension dim = getPreferredSize();
		if( rect.x+rect.width> dim.width) rect.width = dim.width-rect.x;
		if( rect.y+rect.height> dim.height) rect.height = dim.height-rect.y;
		
		BufferedImage im = new BufferedImage( rect.width, rect.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = im.createGraphics();
		g2.translate(-rect.x, -rect.y);
		if(border != null) {
			Dimension size = getPreferredSize();
			Rectangle bounds = new Rectangle(0, 0, size.width, size.height);
			if(rect.contains(bounds)) {
				rect=bounds;
				g2.clipRect(rect.x, rect.y, rect.width, rect.height);
			}
			Insets ins = border.getBorderInsets(this);
			border.paintBorder(this, g2, rect.x, rect.y, rect.width, rect.height);
			int[] seg = getVisibleSeg();
			if( seg[1]>seg[0]) {
			//	GeneralPath path = new GeneralPath();
			//	path.moveTo(0f, 0f);
			//	path.lineTo( 4f, -10f );
			//	path.lineTo( -4f, -10f );
			//	path.closePath();
				g2.setFont( new Font("SansSerif", Font.BOLD, 10));
				FontMetrics fm = g2.getFontMetrics();
				AffineTransform at = g2.getTransform();
				double scale = (double)(rect.width-ins.left-ins.right) / 
					(double)(seg[1]-seg[0]);
				if(isRevVid()) {
				//	g2.setColor( new Color( .75f, .75f, .75f, .75f) );
					g2.setColor( Color.yellow);
				} else {
				//	g2.setColor( new Color( .25f, .25f, .25f, .75f) );
					g2.setColor( Color.blue);
				}
				for( int k=0 ; k<line.crossings.size() ; k++) {
					RCrossing crs = (RCrossing)line.crossings.get(k);
					if( crs.cdp1<seg[0] || crs.cdp1>seg[1])continue;
					int x = isFlip() ?
						rect.x+ins.left+(int)Math.rint((-crs.cdp1+seg[1])*scale) :
						rect.x+ins.left+(int)Math.rint((crs.cdp1-seg[0])*scale);
					g2.translate( x, rect.y+ins.top );
					g2.drawLine( 0, 0, 0, -12 );
				//	g2.fill(path);
					g2.drawString( crs.cross.toString(), 
						-fm.stringWidth(crs.cross.toString())/2, -14);
					g2.setTransform(at);
				}
			}
			g2.translate(ins.left, ins.top);
			g2.clipRect(rect.x, rect.y, 
				rect.width-ins.left-ins.right, 
				rect.height-ins.top-ins.bottom);
		}
		if(!image.isFlip() && getZoomX()==1 && getZoomY()==1) {
			g2.drawImage(image.getImage(), 0, 0, this);
		} else {
			Rectangle r = image.getImageableRect(g2.getClipBounds(), xAvg, yAvg, xRep, yRep);
			if(r.width >0 || r.height>0 ) {
				BufferedImage im1 = image.getScaledImage(r, xAvg, yAvg, xRep, yRep);
				g2.drawImage( im1, r.x, r.y, this);
			}
		}
		g2.translate( rect.x, rect.y );
		//JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
		//encoder.encode(im);
		ImageIO.write(im, "JPEG", out);
		out.flush();
	}
	
	public void saveDigitizedProducts() {
		
		JFileChooser chooser = MapApp.getFileChooser();
		chooser.setSelectedFile(new File( this.line.getCruiseID()+"_" +this.line.lineID+ "_Horizon1"+ ".txt"));
		int ok = chooser.showSaveDialog( getTopLevelAncestor() );
		
		if( ok == JFileChooser.CANCEL_OPTION )return;
		
		File file = chooser.getSelectedFile();
		
		try {
			PrintStream out = new PrintStream( new FileOutputStream(file) );
			out.println(line.getCruiseID()+" "+line.lineID);//added line data to the text file DEP 9.13.2011
			out.println("Longitude\tLatitude\tTwo-Way Travel Time (secs)\tCMP#");
			Vector objects = dig.getObjects();
			Vector points;
			double[] xy;
			for( int k=0 ; k<objects.size() ; k++ ) {
				LineSegmentsObject obj = (LineSegmentsObject)objects.get(k);
				points = (Vector)obj.getPoints();
				if( obj instanceof AnnotationObject ) {
					out.println(">\t" + obj.toString() +"\t"+ points.size() 
							+"\t"+  ((AnnotationObject)obj).getAnnotation() );
				} else {
					out.println(">\t" + obj.toString() +"\t"+ points.size() );
				}
				if( points.size()==0 ) continue;
				for( int i=0 ; i<points.size() ; i++ ) {
					xy = (double[])points.get(i);
					Point tempP = new Point( (int)xy[0], (int)xy[1] );
					Point2D resultP = reverseProcessPoint(tempP);
					Point2D p = line.pointAtCDP(cdpAt((int)resultP.getX()));
					Point2D latLonP = line.map.getProjection().getRefXY(p);
					double tempX = latLonP.getX();
					while ( tempX > 180. ) {
						tempX -= 360.;
					}
					NumberFormat latLonFormat = NumberFormat.getInstance();
					latLonFormat.setMaximumFractionDigits(6);
					latLonFormat.setMinimumFractionDigits(6);
					NumberFormat timeFormat = NumberFormat.getInstance();
					timeFormat.setMaximumFractionDigits(6);
					timeFormat.setMinimumFractionDigits(6);
					latLonP = new Point2D.Double( tempX, latLonP.getY() );
					out.println(latLonFormat.format(latLonP.getX()) + "\t" + latLonFormat.format(latLonP.getY()) + "\t" + ((timeAt((int)(resultP.getY())))/1000000000.0) + "\t" + cdpAt((int)resultP.getX()));
				}
			}
			out.close();
		} catch(IOException ex) {
			ex.printStackTrace();
		}
	}

	public Point2D reverseProcessPoint( Point inputPoint ) {
		Insets ins = getInsets();
		Point2D.Double p = new Point2D.Double( inputPoint.getX(), inputPoint.getY() );
		double[] scales = getScales();
		p.x *= scales[0];
		p.y *= scales[1];
		p.x += ins.left;
		p.y += ins.top;
		return p;
	}
	
	/**
	 * Returns the point from the object generated by reverseProcessPoint - Donald Pomeroy
	 * @param processedPoint
	 * @return
	 */
	public Point undoReverseProcessPoint(Point2D processedPoint)
	{
		Insets ins = getInsets();
		Point p = new Point((int)processedPoint.getX(),(int)processedPoint.getY());
		double[] scales = getScales();
		p.x -= ins.left;
		p.y -= ins.top;
		p.x /= scales[0];
		p.y /= scales[1];
		return p;
	}

	public double getZoomX() {
		return (double)xRep / (double)xAvg;
	}
	public double getZoomY() {
		return (double)yRep / (double)yAvg;
	}

	public Insets getInsets() {			
		return border.getBorderInsets(this);
	}

	public void setXY(Point p) {
	}
	public void setRect(Rectangle rect) {
	}
	public void newRectangle(Rectangle rect) {
	}
	public void zoomTo(Rectangle rect) {
	}
	public double timeAt( int y ) {
		if( tRange == null) return Double.NaN;
		Insets ins = border.getBorderInsets(this);
		double zoomY = getZoomY();
		zoomY /= (tRange[1]-tRange[0]) / (double)height;
		double c = (y-ins.top)/zoomY;
		double min = Math.min( tRange[0], tRange[1]);
		double max = Math.max( tRange[0], tRange[1]);
		if( c<min ) c=min;
		if( c>max ) c=max;
		return c;
	}
	
	/**
	 * Gets the co-ordinate from the time - Donald Pomeroy
	 * @param t
	 * @return
	 */
	public int undoTimeAt(double t)
	{
		if( tRange == null) return  -1;
		Insets ins = border.getBorderInsets(this);
		double zoomY = getZoomY();
		zoomY /= (tRange[1]-tRange[0]) / (double)height;
		double c = t - tRange[0];
		int y = (int)((c * zoomY) + ins.top);
		return y;
	}
	
	public int cdpAt( int x ) {
		if( cdpInterval == null) return -1;
		Insets ins = border.getBorderInsets(this);
		double zoomX = getZoomX();
		zoomX /= (cdpInterval[1]-cdpInterval[0]+1)/(double)width;
		double c = (x-ins.left)/zoomX;
		if( flip ) {
			c = cdpInterval[1] - c;
		} else {
			c = cdpInterval[0] +  c;
		}
		if(c<cdpInterval[0]) c=cdpInterval[0];
		if(c>cdpInterval[1]) c=cdpInterval[1];
		return (int)Math.rint(c);
	}
	
	/**
	 * Returns the co-ordinate that the CDP was generated from - Donald Pomeroy
	 * @param c
	 * @return
	 */
	public double undoCdpAt( double c)
	{
		if( cdpInterval == null) return -1;
		Insets ins = border.getBorderInsets(this);
		double zoomX = getZoomX();
		zoomX /= (cdpInterval[1]-cdpInterval[0]+1)/(double)width;
		double x;
		c = Math.rint(c);
		if( flip ) {
			c = (-1)*(c - cdpInterval[1]);
		} else {
			c =  c - cdpInterval[0];
		}
		x = ((c*zoomX) + ins.left);
		return x;
	}
	
	public void zoomIn(Point p) {
		if( image==null )return;
		Insets ins = border.getBorderInsets(this);
		Rectangle rect = getVisibleRect();
		double zoomX = getZoomX();
		double zoomY = getZoomY();
		double x = (double) (p.x - ins.left) / zoomX;
		double y = (double) (p.y - ins.top) / zoomY;
		double w = (double) rect.width - ins.left - ins.right;
		double h = (double) rect.height - ins.top - ins.bottom;
		if(xAvg==1) xRep*=2;
		else xAvg /=2;
		if(yAvg==1) yRep*=2;
		else yAvg /=2;
		zoomX = getZoomX();
		zoomY = getZoomY();
		invalidate();
		int newX = (int) (x*zoomX - rect.getWidth()*.5d);
		int newY = (int) (y*zoomY - rect.getHeight()*.5d);
		synchronized(this) {
			scroller.validate();
		}
		scroller.scrollTo(new Point(newX, newY));
		repaint();
	}
	public void zoomOut(Point p) {
		if( image==null )return;
		double zoomX = getZoomX();
		double zoomY = getZoomY();
		Insets ins = border.getBorderInsets(this);
		Rectangle rect = getVisibleRect();
		Rectangle r1 = getBounds();
		double x = (double) (p.x - ins.left) / zoomX;
		double y = (double) (p.y - ins.top) / zoomY;
		double w = (double) rect.width - ins.left - ins.right;
		double h = (double) rect.height - ins.top - ins.bottom;
		if(xRep==1) xAvg*=2;
		else xRep /=2;
		if(yRep==1) yAvg*=2;
		else yRep /=2;
		zoomX = getZoomX();
		zoomY = getZoomY();
		invalidate();
		int newX = (int) (x*zoomX - w*.5d);
		int newY = (int) (y*zoomY - h*.5d);
		synchronized(this) {
			scroller.validate();
		}
		scroller.scrollTo(new Point(newX, newY));
		repaint();
	}
	public void setScroller(Scroller scroller) {
		this.scroller = scroller;
	}
	void popup( Point p ) {
	//	popM.show( this, p.x, p.y );
	}
	public void mouseClicked(MouseEvent e) {
		if( e.isPopupTrigger() ) popup(e.getPoint() );
	}
	public void mousePressed(MouseEvent e) {
		if( e.isPopupTrigger() ) popup(e.getPoint() );
	}
	public void mouseReleased(MouseEvent e) {
	}
	public void mouseEntered(MouseEvent e) {
		if( image==null || line==null )return;
		line.map.repaint();
	}
	public void mouseExited(MouseEvent e) {
		if( line==null) return;
		line.drawCDP( -1 );
		drawTime( Double.NaN );
		otherImage.drawTime( Double.NaN );
	}
	public void mouseMoved(MouseEvent e) {
		if( image==null || line==null )return;
		line.drawCDP( cdpAt( e.getX() ) );
		double t = timeAt( e.getY());
		drawTime( t );
		otherImage.drawTime( t );
	}
	double lastTime = Double.NaN;
	void drawTime( double t ) {
		if( tRange==null ) return;
		synchronized (getTreeLock()) {
			Graphics2D g = (Graphics2D)getGraphics();
			Insets ins = (border==null) ? (new Insets(0,0,0,0)) : border.getBorderInsets(this);
			Rectangle rect = getVisibleRect();
			Dimension dim = getPreferredSize();
			g.setXORMode( Color.white );
			if( !Double.isNaN(lastTime) ) {
				int y = ins.top + 
					(int)Math.rint( (dim.height-ins.top-ins.bottom) *
					(lastTime-tRange[0])/(tRange[1]-tRange[0]) );
				g.drawLine( rect.x+ins.left, y, rect.x+rect.width-ins.right, y);
			}
			lastTime = t;
			if( !Double.isNaN(lastTime) ) {
				int y = ins.top + 
					(int)Math.rint( (dim.height-ins.top-ins.bottom) *
					(lastTime-tRange[0])/(tRange[1]-tRange[0]) );
				g.drawLine( rect.x+ins.left, y, rect.x+rect.width-ins.right, y);
			}
		}
	}
	public void mouseDragged(MouseEvent e) {
	}
	public void actionPerformed(ActionEvent e) {
		
		
		String cmd = e.getActionCommand();
		
		if ( cmd.equals("Zoom In") || cmd.equals("Zoom Out") ) {
			if(zoomInTB.isSelected() || zoomOutTB.isSelected()) {
				for ( int i = 0; i < dig.getButtons().size(); i++ ) {
					((JToggleButton)dig.getButtons().get(i)).setSelected(false);					
				}
				dig.getSelectB().setSelected(true);
				dig.getSelectB().doClick();
			}
			
			if ( !zoomInTB.isSelected() && !zoomOutTB.isSelected() ) {
				dig.getSelectB().setSelected(true);
				dig.getSelectB().doClick();
			}
//			else {
//				for ( int i = 0; i < dig.getButtons().size(); i++ ) {
//					((JToggleButton)dig.getButtons().get(i)).setSelected(false);
//				}
//				dig.getSelectB().setSelected(false);
//			}
		}
		else if(cmd.equals("select")) {
			
		}
		try{
		if( Integer.parseInt(cmd)==0 || Integer.parseInt(cmd)==1)
		{
			
			if(zoomInTB.isSelected()) {
				zoomInTB.doClick();							
			}
			if(zoomOutTB.isSelected()) {
				zoomOutTB.doClick();								
			}
		}
		}catch(NumberFormatException nfe){}
		if(image==null) {
			if(cmd.equals("RevVid")) {
				rev = !rev;
			} else if( cmd.equals("flip")) {
				flip = !flip;
			}
			return;
		}
		if(cmd.equals("RevVid")) {
			if(zoomInTB.isSelected()) {					
				zoomInTB.doClick();				
			}
			if(zoomOutTB.isSelected()) {					
				zoomOutTB.doClick();								
			}
			
			for ( int i = 0; i < dig.getButtons().size(); i++ ) {
				((JToggleButton)dig.getButtons().get(i)).setSelected(false);					
			}
			dig.getSelectB().setSelected(true);
			dig.getSelectB().doClick();
			revVid();
		} else if( cmd.equals("wider")) {
			if(zoomInTB.isSelected()) {					
				zoomInTB.doClick();				
			}
			if(zoomOutTB.isSelected()) {					
				zoomOutTB.doClick();								
			}
			
			for ( int i = 0; i < dig.getButtons().size(); i++ ) {
				((JToggleButton)dig.getButtons().get(i)).setSelected(false);					
			}
			dig.getSelectB().setSelected(true);
			dig.getSelectB().doClick();
			wider();
		} else if( cmd.equals("narrower")) {
			if(zoomInTB.isSelected()) {					
				zoomInTB.doClick();				
			}
			if(zoomOutTB.isSelected()) {					
				zoomOutTB.doClick();								
			}
			
			for ( int i = 0; i < dig.getButtons().size(); i++ ) {
				((JToggleButton)dig.getButtons().get(i)).setSelected(false);					
			}
			dig.getSelectB().setSelected(true);
			dig.getSelectB().doClick();
			narrower();
		} else if( cmd.equals("flip")) {
			if(zoomInTB.isSelected()) {					
				zoomInTB.doClick();				
			}
			if(zoomOutTB.isSelected()) {					
				zoomOutTB.doClick();								
			}
			
			for ( int i = 0; i < dig.getButtons().size(); i++ ) {
				((JToggleButton)dig.getButtons().get(i)).setSelected(false);					
			}
			dig.getSelectB().setSelected(true);
			dig.getSelectB().doClick();
			image.setFlip(!image.isFlip());
			flip = image.isFlip();
			double zoomX = getZoomX();
			double zoomY = getZoomY();
			Insets ins = border.getBorderInsets(this);
			Rectangle rect = getVisibleRect();
			Rectangle r1 = getBounds();
			if(rect.contains(r1)) rect=r1;
			Point p = new Point();
			p.x = (rect.width- ins.left - ins.right)/2+rect.x;
			p.y = (rect.height- ins.top - ins.bottom)/2+rect.y;
			double x = width - p.getX() / zoomX;
			double y = p.getY() / zoomY;
			double w = (double) rect.width - ins.left - ins.right;
			double h = (double) rect.height - ins.top - ins.bottom;
			invalidate();
			int newX = (int) (x*zoomX - w*.5d);
			int newY = (int) (y*zoomY - h*.5d);
			synchronized(this) {
				scroller.validate();
			}
			scroller.scrollTo(new Point(newX, newY));
			repaint();
		} else if( cmd.equals("save")) {
			new Thread(){
				public void run() {
					try
					{
						saving = true;
						save();
						saving = false;
					}
					catch (IOException ex) { }
				}
			}.start();
/*
		} else if( cmd.equals("show scales")) {
			if( scaleMI.isSelected() ) {
				mcsScale.setEnabled( this );
			} else {
				mcsScale.setEnabled( null );
			}
*/
		} else if( cmd.equals("show crossing lines")) {
			repaint();
		}
		else if ( cmd.equals("Save digitized products") ) {
			saveDigitizedProducts();
		}
		
	}

	void save()	throws IOException {
		if(line == null) {
			JOptionPane.showMessageDialog(getTopLevelAncestor(), "Image not loaded.");
			return;
		}
		RLine currentLine = line;
		if(currentLine==null)return;
		
		
		
		selectedPingFile = currentLine.getID();
		URL url = null;
		BufferedReader in = null;
		try{
		url = URLFactory.url(RADAR_EXP_LIST);
		in = new BufferedReader( new InputStreamReader(url.openStream()) );
		
		String name_lookup;
		
		while ((name_lookup = in.readLine()) != null ) {			
			String[] split = name_lookup.split("\\s");
			if(split[0].equalsIgnoreCase(currentLine.getCruiseID())){
				selectedPingFile = split[1];
				break;
			}
		}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		ArrayList<String> missing = new ArrayList<String>();
		String request = uidURLString + selectedPingFile + ".data_lookup";
		try {
			ArrayList<String> types = new ArrayList<String>();
			types.add("jpg");
			types.add("segy");
			types.add("nav");
			types.add("nc");
			types.add("mat");
			
			for(String type_string:types){
			url = URLFactory.url(request);
			in = new BufferedReader( new InputStreamReader(url.openStream()) );
			selectedDataUID="";
			String s;
			while ( (s = in.readLine()) != null ) {								
				String[] split = s.split("\\s");
				if((split[0].equalsIgnoreCase(currentLine.getID()) || split[0].equalsIgnoreCase(currentLine.getCruiseID()+"-"+currentLine.getID())) && split[1].equalsIgnoreCase(type_string)){
					if(split.length>=3)
						selectedDataUID = split[2];
					break;
				}
			}
			if(selectedDataUID.isEmpty())
			{
				missing.add(type_string);
			}
			}	
		} catch (IOException ec) {
			ec.printStackTrace();
		}
		
		JPanel savePrompt = new JPanel(new GridLayout(0, 1));
		
		JCheckBox imageFullCB = new JCheckBox("Save Viewport");
		JCheckBox imageCB = new JCheckBox("Save jpg");
		JCheckBox segyCB = new JCheckBox("Save segy");
		JCheckBox navCB = new JCheckBox("Save nav");
		JCheckBox matCB = new JCheckBox("Save mat");
		JCheckBox ncCB = new JCheckBox("save nc");
		ButtonGroup boxGroup = new ButtonGroup();
		
		if(missing.contains("jpg"))
			imageCB.setEnabled(false);
		if(missing.contains("segy"))
			segyCB.setEnabled(false);
		if(missing.contains("nav"))
			navCB.setEnabled(false);
		if(missing.contains("mat"))
			matCB.setEnabled(false);
		if(missing.contains("nc"))
			ncCB.setEnabled(false);
		
		boxGroup.add(imageFullCB);
		boxGroup.add(imageCB);
		boxGroup.add(segyCB);
		boxGroup.add(navCB);
		boxGroup.add(matCB);
		boxGroup.add(ncCB);
		
		
		savePrompt.add(imageFullCB);
		savePrompt.add(imageCB);			
		savePrompt.add(segyCB);
		savePrompt.add(navCB);
		savePrompt.add(matCB);
		savePrompt.add(ncCB);		
		
		JOptionPane.showConfirmDialog(MapApp.anchor, savePrompt, "Save what?", JOptionPane.OK_CANCEL_OPTION);
		
		String type = null;
		
		if(imageCB.isSelected())
			type = "jpg";
		if(segyCB.isSelected())
			type = "segy";
		if(navCB.isSelected())
			type = "nav";
		if(matCB.isSelected())
			type = "mat";
		if(ncCB.isSelected())
			type = "nc";
		
		
		if (imageFullCB.isSelected()) { // Save Full image
			File file = new File(line.getCruiseID()+"-"+getID() + ".jpg");
			while (true) {
				JFileChooser filechooser = MapApp.getFileChooser();
				filechooser.setSelectedFile(file);
				int c = filechooser.showSaveDialog(getTopLevelAncestor());
				if (c == JFileChooser.CANCEL_OPTION)
					return;
				file = filechooser.getSelectedFile();
				if(!file.exists())
					break;
				int o = JOptionPane.showConfirmDialog(getTopLevelAncestor(), "File exists, Overwrite?");
				if(o == JOptionPane.CANCEL_OPTION)
					return;
				else if(o == JOptionPane.OK_OPTION)
					break;
			}
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));		
			try {
				saveFullJPEG(out);
				out.close();
				JOptionPane.showMessageDialog(MapApp.anchor, "Save Successful");
			}
			catch(IOException ex) { 
				JOptionPane.showMessageDialog(MapApp.anchor, ex.getMessage(), "Error Writing Jpeg", JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}

			setCursor(Cursor.getDefaultCursor());
			return;
		} 

		
		try {
			url = URLFactory.url(request);
			in = new BufferedReader( new InputStreamReader(url.openStream()) );
			selectedDataUID="";
			String s;
			while ( (s = in.readLine()) != null ) {					
				
				String[] split = s.split("\\s");
				if((split[0].equalsIgnoreCase(currentLine.getID()) || split[0].equalsIgnoreCase(currentLine.getCruiseID()+"-"+currentLine.getID())) && split[1].equalsIgnoreCase(type)){
					if(split.length>=3)
						selectedDataUID = split[2];
					break;
				}
			}
		} catch (IOException ec) {
			ec.printStackTrace();
		}
		if(selectedDataUID.isEmpty())
			return;
		
		String str = "https://www.marine-geo.org/tools/search/Files.php?client=GMA&data_set_uid="+ selectedDataUID;
		BrowseURL.browseURL(str);
		
				
	}

	private void saveFullJPEG(OutputStream out) throws IOException {
		if(image==null) throw new IOException("no image loaded");
		//DEP 9.22.2011 move URL to the sp radar portal
		/*MapApp.getBaseURL() + "antarctic/radar/"*/
		URL url = URLFactory.url(RADAR_PATH + line.getCruiseID().trim() + "/img/"+ line.getID().trim() + ".jpg" );

		URLConnection urlCon = url.openConnection();
		BufferedInputStream in = new BufferedInputStream(urlCon.getInputStream());
		int length = urlCon.getContentLength();

		// Create a JProgressBar + JDialog
		JDialog d = new JDialog((Frame)null, "Saving JPEG");
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		d.setLocationRelativeTo(MapApp.anchor);
		JProgressBar pb = new JProgressBar(0,length);
		p.add(new JLabel("Saving " + (length / 1000) + "kb jpeg file"), BorderLayout.NORTH);
		p.add(pb);
		d.getContentPane().add(p);

		d.pack();
		d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		d.setVisible(true);

		byte[] b = new byte[16384];
		int read = in.read(b);
		while (read != -1) {
			out.write(b, 0, read);
			pb.setValue(pb.getValue() + read);
			pb.repaint();
			read = in.read(b);
		}
		out.flush();
		in.close();
		d.dispose();
	}
	private void saveSEGY(OutputStream out) throws IOException {
		if(image==null) throw new IOException("no image loaded");
		//DEP get the dataLink file in the sp_radar portal that points to SEGY directory
		/*MapApp.getBaseURL() + "antarctic/radar/"*/
		URL url = URLFactory.url( RADAR_PATH + line.getCruiseID() + "/dataLink");
		BufferedReader reader = new BufferedReader(new InputStreamReader (url.openStream()));		
		String data = reader.readLine();
		reader.close();

		url = URLFactory.url ( data +"segy/"+ getID() + ".segy");
		URLConnection urlCon = url.openConnection();
		BufferedInputStream in = new BufferedInputStream(urlCon.getInputStream());
		int length = urlCon.getContentLength();

		// Create a JProgressBar + JDialog
		JDialog d = new JDialog((Frame)null, "Saving SEGY");
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		d.setLocationRelativeTo(MapApp.anchor);
		JProgressBar pb = new JProgressBar(0,length);
		p.add(new JLabel("Saving " + (length / 1000000) + "mb segy file"), BorderLayout.NORTH);
		p.add(pb);
		d.getContentPane().add(p);

		d.pack();
		d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		d.setVisible(true);

		byte[] b = new byte[16384];
		int read = in.read(b);
		while (read != -1) {
			out.write(b, 0, read);
			pb.setValue(pb.getValue() + read);
			pb.repaint();
			read = in.read(b);
		}
		out.flush();
		in.close();
		d.dispose();
	}
	
	private void saveNAV(OutputStream out) throws IOException {
		if(image==null) throw new IOException("no image loaded");
		//DEP get the dataLink file in the sp_radar portal that points to directory with the NAV files
		/*MapApp.getBaseURL() + "antarctic/radar/"*/
		URL url = URLFactory.url( RADAR_PATH + line.getCruiseID() + "/dataLink");
		BufferedReader reader = new BufferedReader(new InputStreamReader (url.openStream()));		
		String data = reader.readLine();
		reader.close();

		url = URLFactory.url ( data + getID() + ".nav");
		URLConnection urlCon = url.openConnection();
		BufferedInputStream in = new BufferedInputStream(urlCon.getInputStream());
		int length = urlCon.getContentLength();

		// Create a JProgressBar + JDialog
		JDialog d = new JDialog((Frame)null, "Saving NAV");
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		d.setLocationRelativeTo(MapApp.anchor);
		JProgressBar pb = new JProgressBar(0,length);
		p.add(new JLabel("Saving " + (length / 1000) + "kb nav file"), BorderLayout.NORTH);
		p.add(pb);
		d.getContentPane().add(p);

		d.pack();
		d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		d.setVisible(true);

		byte[] b = new byte[16384];
		int read = in.read(b);
		while (read != -1) {
			out.write(b, 0, read);
			pb.setValue(pb.getValue() + read);
			pb.repaint();
			read = in.read(b);
		}
		out.flush();
		in.close();
		d.dispose();
	}
	
	private void saveMAT(OutputStream out, int count) throws IOException {
		if(image==null) throw new IOException("no image loaded");
		
		URL url = URLFactory.url ( "https://www.marine-geo.org/data/field/AGAP_GAMBIT/radar/mat/"+getID()+"-"+count + "_1D_SAR.mat.gz");
		URLConnection urlCon = url.openConnection();
		BufferedInputStream in = new BufferedInputStream(urlCon.getInputStream());
		int length = urlCon.getContentLength();

		// Create a JProgressBar + JDialog
		JDialog d = new JDialog((Frame)null, "Saving MAT");
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		d.setLocationRelativeTo(MapApp.anchor);
		JProgressBar pb = new JProgressBar(0,length);
		p.add(new JLabel("Saving " + (length / 1000) + "kb mat file"), BorderLayout.NORTH);
		p.add(pb);
		d.getContentPane().add(p);

		d.pack();
		d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		d.setVisible(true);

		byte[] b = new byte[1024];
		int read = in.read(b);
		while (read != -1) {
			out.write(b, 0, read);
			pb.setValue(pb.getValue() + read);
			pb.repaint();
			read = in.read(b);
		}
		
		out.flush();			
		in.close();
		d.dispose();
	}

	
	private void saveNC(OutputStream out, int count) throws IOException {
		if(image==null) throw new IOException("no image loaded");
		
		URL url = URLFactory.url ( "http://www.marine-geo.org/data/field/AGAP_GAMBIT/radar/netcdf/"+getID()+"-"+count + "_1D_SAR.nc.gz");
		URLConnection urlCon = url.openConnection();
		BufferedInputStream in = new BufferedInputStream(urlCon.getInputStream());
		int length = urlCon.getContentLength();

		// Create a JProgressBar + JDialog
		JDialog d = new JDialog((Frame)null, "Saving NC");
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		d.setLocationRelativeTo(MapApp.anchor);
		JProgressBar pb = new JProgressBar(0,length);
		p.add(new JLabel("Saving " + (length / 1000) + "kb nc file"), BorderLayout.NORTH);
		p.add(pb);
		d.getContentPane().add(p);

		d.pack();
		d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		d.setVisible(true);

		byte[] b = new byte[16384];
		int read = in.read(b);
		while (read != -1) {
			out.write(b, 0, read);
			pb.setValue(pb.getValue() + read);
			pb.repaint();
			read = in.read(b);
		}
		out.flush();
		in.close();
		d.dispose();
	}
	public void narrower() {
		double zoomX = getZoomX();
		double zoomY = getZoomY();
		Insets ins = border.getBorderInsets(this);
		Rectangle rect = getVisibleRect();
		Rectangle r1 = getBounds();
		if(rect.contains(r1)) rect=r1;
		Point p = new Point();
		p.x = (rect.width- ins.left - ins.right)/2+rect.x;
		p.y = (rect.height- ins.top - ins.bottom)/2+rect.y;
		double x = p.getX() / zoomX;
		double y = p.getY() / zoomY;
		double w = (double) rect.width - ins.left - ins.right;
		double h = (double) rect.height - ins.top - ins.bottom;
		if(xRep==1) xAvg*=2;
		else xRep /=2;
		zoomX = getZoomX();
		zoomY = getZoomY();
		invalidate();
		int newX = (int) (x*zoomX - w*.5d);
		int newY = (int) (y*zoomY - h*.5d);
		invalidate();
		scroller.validate();
		scroller.scrollTo(new Point(newX, newY));
		repaint();
	}
	public void wider() {
		double zoomX = getZoomX();
		double zoomY = getZoomY();
		Insets ins = border.getBorderInsets(this);
		Rectangle rect = getVisibleRect();
		Rectangle r1 = getBounds();
		if(rect.contains(r1)) rect=r1;
		Point p = new Point();
		p.x = (rect.width- ins.left - ins.right)/2+rect.x;
		p.y = (rect.height- ins.top - ins.bottom)/2+rect.y;
		double x = p.getX() / zoomX;
		double y = p.getY() / zoomY;
		double w = (double) rect.width - ins.left - ins.right;
		double h = (double) rect.height - ins.top - ins.bottom;
		if(xAvg==1) xRep*=2;
		else xAvg /=2;
		zoomX = getZoomX();
		zoomY = getZoomY();
		invalidate();
		int newX = (int) (x*zoomX - w*.5d);
		int newY = (int) (y*zoomY - h*.5d);
		invalidate();
		scroller.validate();
		r1 = getBounds();
		scroller.scrollTo(new Point(newX, newY));
		repaint();
	}
	public void revVid() {
		rev = !rev;
		image.setRevVid(rev);
		repaint();
	}
	JToolBar toolBar=null;
//	javax.swing.border.Border pressed = BorderFactory.createLoweredBevelBorder();
	private void initToolBar() {
		//DEP 9.22.2011 group the zoom in and zoom out buttons to be mutually exclusive as in XMImage
		ButtonGroup zoomGroup = new ButtonGroup();
		JToggleButton no = new JToggleButton();
		zoomGroup.add(no);
		
		
		haxby.util.SimpleBorder border = new haxby.util.SimpleBorder();
		toolBar = new JToolBar(JToolBar.HORIZONTAL);
		toolBar.setFloatable( false );
		
		JButton b = new JButton(Icons.getIcon(Icons.WIDER,false));
		b.setPressedIcon( Icons.getIcon(Icons.WIDER,true) );
		b.setBorder(null);
		b.addActionListener(this);
		toolBar.add(b);
		b.setActionCommand("wider");
		b = new JButton(Icons.getIcon(Icons.NARROWER,false));
		b.setPressedIcon( Icons.getIcon(Icons.NARROWER,true) );
		b.setBorder(null);
		b.addActionListener(this);
		toolBar.add(b);
		b.setActionCommand("narrower");

		JToggleButton tb = new JToggleButton(Icons.getIcon(Icons.POSITIVE_BOX,false));
		tb.setSelectedIcon(Icons.getIcon(Icons.NEGATIVE_BOX,true));
		tb.setToolTipText("Negative/Positive Image");
		tb.setBorder(null);
		tb.addActionListener(this);
		toolBar.add(tb);
		tb.setActionCommand("RevVid");

		tb = new JToggleButton(Icons.getIcon(Icons.FORWARD,false));
		tb.setSelectedIcon(Icons.getIcon(Icons.REVERSE,true));
		tb.setBorder(null);
		tb.addActionListener(this);
		toolBar.add(tb);
		tb.setActionCommand("flip");

		b = new JButton(Icons.getIcon(Icons.SAVE,false));
		b.setPressedIcon( Icons.getIcon(Icons.SAVE,true) );
		b.setBorder(null);
		b.addActionListener(this);
		toolBar.add(b);
		b.setToolTipText("Save Image or Data");
		b.setActionCommand("save");

		for ( int i = 0; i < dig.getButtons().size(); i++ ) {
			toolBar.add((JToggleButton)dig.getButtons().get(i));
			((JToggleButton)dig.getButtons().get(i)).addActionListener(this);
		}
		dig.getSelectB().addActionListener(this);	
		dig.getSelectB().setSelected(true);
		
		

		JButton saveDig = new JButton(Icons.getIcon(Icons.SAVE,false));
		saveDig.setPressedIcon(Icons.getIcon(Icons.SAVE,true));
		saveDig.setBorder(null);
		saveDig.addActionListener(this);
		toolBar.add(saveDig);
		saveDig.setToolTipText("Save digitized products");
		saveDig.setActionCommand("Save digitized products");
		
		
		zoomer = new Zoomer(this);
		addMouseListener(zoomer);
		
		addMouseMotionListener(zoomer);
		
		addKeyListener(zoomer);
		digitizeTB = new JToggleButton();
		zoomInTB = zoomer.getZoomIn();
		zoomInTB.setPressedIcon( Icons.getIcon(Icons.ZOOM_IN,false) );
		zoomInTB.setSelectedIcon( Icons.getIcon(Icons.ZOOM_IN,true) );
		zoomInTB.setBorder(null);
		zoomInTB.setActionCommand("Zoom In");
		zoomInTB.addActionListener(this);
		toolBar.add(zoomInTB);
		zoomInTB.setToolTipText("Zoom In (Ctrl-Click)");
		zoomGroup.add(zoomInTB);
		new ToggleToggler( zoomInTB, no );
		zoomOutTB = zoomer.getZoomOut();
		zoomOutTB.setPressedIcon( Icons.getIcon(Icons.ZOOM_OUT,false) );
		zoomOutTB.setSelectedIcon( Icons.getIcon(Icons.ZOOM_OUT,true) );
		zoomOutTB.setActionCommand("Zoom Out");
		zoomOutTB.addActionListener(this);
		toolBar.add(zoomOutTB);
		zoomOutTB.setToolTipText("Zoom Out (Ctrl-Shift-Click)");
		zoomGroup.add(zoomOutTB);
		new ToggleToggler( zoomOutTB, no );
		//DEP added a close image button to the radar portal
		JButton closeButton = new JButton(org.geomapapp.util.Icons.getIcon(org.geomapapp.util.Icons.CLOSE, false));
		closeButton.setToolTipText("Close Image");
		closeButton.setBorder(null);
		closeButton.setSelectedIcon(org.geomapapp.util.Icons.getIcon(org.geomapapp.util.Icons.CLOSE, true));
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(line != null)
					closeLine();
			}
		});
		
		JButton load = new JButton( org.geomapapp.util.Icons.getIcon(org.geomapapp.util.Icons.OPEN,false) );
		load.setToolTipText("Load previously digitized products");
		load.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				try {
					setLine(Radar.currentLine, true);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		toolBar.add(closeButton);
		toolBar.add(load);
	}
	
	//DEP add a close line function for the close image button
	
	protected void closeLine() {
		XMap map = line.map;
		//line = null;
		border = null;
		image = null;
		System.gc();
		panel.repaint();
		otherImage.repaint();
		map.repaint();
		
		dig.getSelectB().doClick(0);
		
		dig.reset();	
	}
	
public void setLine(RLine cruiseLine, boolean load) throws IOException {
		
		dig.reset();
		
		this.line = cruiseLine;
		int ok;
		
		if(cruiseLine == null)
		{
			JOptionPane.showMessageDialog(MapApp.anchor, "Select a line");
			return;
		}		
		if ( load ){
			JFileChooser chooser = MapApp.getFileChooser();
			chooser.setMultiSelectionEnabled( true );
			ok = chooser.showOpenDialog( getTopLevelAncestor() );
			if( ok==chooser.APPROVE_OPTION ) {
				File[] files = chooser.getSelectedFiles();
				BufferedReader in=null;
				Vector types = dig.getOptionsDialog().getLineTypes();
				Vector objects = dig.getObjects();
				for(int k=0 ; k<files.length ; k++) {
					try {
						in = new BufferedReader(
							new FileReader( files[k] ));
						String s = in.readLine();
						StringTokenizer st = new StringTokenizer(s);
						String name = s;
						System.out.println("name line 233:" + name);
						if( !name.equalsIgnoreCase( cruiseLine.getCruise()+" "+cruiseLine.getID() ) ) {
							JOptionPane.showMessageDialog(MapApp.anchor, "Line in file does not match selected line: "+name);
							in.close();
							continue;
						}
						loadImage(cruiseLine);
						in.readLine();
						while( (s = in.readLine()) != null ) {							
							st = new StringTokenizer(s, "\t");
							String p = st.nextToken();
							
							name = st.nextToken();
							int n = Integer.parseInt( st.nextToken() );
														LineSegmentsObject line;
							if( st.hasMoreTokens() ) {
								AnnotationObject obj = new AnnotationObject(
										cruiseLine.map, dig);
								obj.setAnnotation( st.nextToken() );
							
								line = (LineSegmentsObject) obj;
							} else {
								line = new LineSegmentsObject(
										cruiseLine.map, dig);
							}
							line.setName( name );
							LineType type = null;
							int i=0;
							for( i=0 ; i<types.size() ; i++) {
								type = (LineType)types.get(i);
								if( type.name.equals(name) )break;
							}
							if( i==types.size() ) {
								type = (LineType)types.get(0);
								line.setName( type.name );
							}
							line.setColor( type.color );
							line.setStroke( type.stroke );
							Vector points = new Vector();
							for( i=0 ; i<n ; i++) {
								st = new StringTokenizer( in.readLine());
								st.nextToken();
								st.nextToken();								
								double x =  Double.parseDouble(st.nextToken());
							
								double resultPy = undoTimeAt(1000000000.0*x);
								//System.out.println("X :"+x);								
								double y = Double.parseDouble(st.nextToken());
								double resultPx = undoCdpAt(y);
								Point addPoint = undoReverseProcessPoint(new Point2D.Double(resultPx,resultPy));
								//System.out.println("y :"+ y);
								points.add( new double[] {addPoint.x, addPoint.y, 0.} );
							}
							line.setPoints( points );
							objects.add( line );
							
						}
						in.close();
					} catch (Exception ex ) {
						ex.printStackTrace();
						try {
							in.close();
						} catch (Exception e ) {
						}
						continue;
					}
				}
			}
			chooser.setMultiSelectionEnabled( false );
		}
		System.gc();						
		repaint();		
	}

	public boolean isFlip() { 
		if(image==null) return false;
		return image.isFlip(); 
	}
	public JToolBar getToolBar() {
		if(toolBar==null) initToolBar();
		return toolBar;
	}
	public boolean isRevVid() { return rev; }
	@Override
	public double[] getScales() {
		return new double[] { getZoomX(), getZoomY() };
	}
	public Double getZoomValue() {
		return null;
	}
	public void zoomSpeed(Point p, Double d) {
	}
	
	private class ToggleToggler implements ActionListener, ChangeListener {
		boolean wasSelected;
		JToggleButton b,no;
		ButtonGroup bg;

		public ToggleToggler(JToggleButton b,JToggleButton no) {
			this.b=b;
			this.no=no;
			b.addActionListener(this);
			b.addChangeListener(this);
			wasSelected=b.isSelected();
		}

		public void stateChanged(ChangeEvent e) {
			if (!b.isSelected()) wasSelected=false;
		}
		public void actionPerformed(ActionEvent e) {
			if (wasSelected) no.doClick();
			wasSelected=b.isSelected();
		}
	}
	
}
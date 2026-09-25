package haxby.db.xmcs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import haxby.dig.AnnotationObject;
import haxby.dig.Digitizer;
import haxby.dig.DigitizerObject;
import haxby.dig.LineSegmentsObject;
import haxby.dig.LineType;
import haxby.image.Icons;
import haxby.image.R2;
import haxby.image.ScalableImage;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.map.Zoomable;
import haxby.map.Zoomer;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.Scroller;
import haxby.util.URLFactory;

//import com.sun.image.codec.jpeg.JPEGCodec;
//import com.sun.image.codec.jpeg.JPEGImageDecoder;
//import com.sun.image.codec.jpeg.JPEGImageEncoder;

public class XMImage extends haxby.util.ScaledComponent
		implements ActionListener, MouseListener, MouseMotionListener, Zoomable, ItemListener {

	protected XMLine line;
	XMImage otherImage;
//	XMSave save;
	XMBorder border;
//	XMScale scale;
	Scroller scroller;
	protected ScalableImage image;
	int width, height;
	JPanel panel = null;
	double[] cdpInterval;
	double[] tRange;
	int xRep, yRep, xAvg, yAvg;
	boolean rev=false;
	boolean flip=false;
	boolean saving=false;
	Zoomer zoomer;
	Point scrollPoint = new Point (0,0);
	// LDEO MCS
	static String MULTI_CHANNEL_PATH = PathUtil.getPath("PORTALS/MULTI_CHANNEL_PATH",
			MapApp.BASE_URL+"/data/portals/mcs/");
	//USGS MCS
	static String USGS_MULTI_CHANNEL_PATH = PathUtil.getPath("PORTALS/USGS_MULTI_CHANNEL_PATH",
								"https://cmgds.marine.usgs.gov/gma/USGS_MCS/");
	//USGS SCS
	static String USGS_SINGLE_CHANNEL_PATH = PathUtil.getPath("PORTALS/USGS_SINGLE_CHANNEL_PATH",
								"https://cmgds.marine.usgs.gov/gma/USGS_SCS/");
	//USGS Industry
	static String USGS_INDUSTRY_PATH = PathUtil.getPath("PORTALS/USGS_INDUSTRY_PATH",
								"https://cmgds.marine.usgs.gov/gma/USGS_INDUSTRY/");

	static String ANTARCTIC_SDLS_PATH = PathUtil.getPath("PORTALS/ANTARCTIC_SDLS",
			MapApp.BASE_URL+"/data/portals/mcs/sdls/");

//	***** GMA 1.6.2: Give checkboxes in "Save what?" window class-wide scope so they can affect 
//	each others selection state.
	JCheckBox imageCB;
	JCheckBox imageFullCB;
	JCheckBox segyCB;
	JCheckBox navCB;
	private static final String errMsg = "Error attempting to launch web browser";
	private boolean rightClick = false;
//	***** GMA 1.6.2

//	***** GMA 1.6.2: Add variables to record and display data for a user-selected point along a
//	MCS cruise line.
	JPopupMenu pm;
	double currentTime = 0.0;
	int currentCDP = 0;
	String tempInfo = null;
//	***** GMA 1.6.2

//	***** GMA 1.6.4: Add cursor button to MCS toolbar, must now know state of zoom in and zoom out buttons, so those must be class variables
	JToggleButton digitizeTB = null;
	JToggleButton cursorTB = null;
	JToggleButton zoomInTB = null;
	JToggleButton zoomOutTB = null;
	JToggleButton flipTB = null;
//	***** GMA 1.6.4

//	***** GMA 1.6.4: Add y offset to popup so it doesn't create lines
	static final int POPUP_Y_OFFSET = -10;
	Digitizer dig;
	
	private String lastSavedDir = null;

	public XMImage( Digitizer inputDig ) {
		dig = inputDig;
		dig = new Digitizer(this);
		border = null;
		image = null;
		cdpInterval = null;
		tRange = null;
		scroller = null;
		width = 100;
		height = 50;
		xRep = yRep = xAvg = yAvg = 1;
		panel = new JPanel(new BorderLayout());
		zoomer = new Zoomer(this);
		addMouseListener(zoomer);
		addMouseListener(this);
		addMouseMotionListener(zoomer);
		addMouseMotionListener(this);
		addKeyListener(zoomer);
		panel.add(getToolBar(), "North");
		JScrollPane sp = new JScrollPane(this);
		scroller = new Scroller(sp, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		panel.add(sp,"Center");
		line = null;
		otherImage = null;

//		***** GMA 1.6.2: Add a pop-up menu that appears when a right-click occurs to allow the user 
//		to record the data for a particular point along the selected MCS cruise line.
		
		setToolTipText("Right-click for menu");
		
		pm = new JPopupMenu();
		JMenuItem mi = new JMenuItem("Copy Digitized Values at this Mouse Location to Clipboard");
		mi.setActionCommand("copy");
		mi.addActionListener(this);
		pm.add(mi);
		JMenuItem mi2 = new JMenuItem("Delete Last Horizon");
		mi2.setActionCommand("deleteLastHorizon");
		mi2.addActionListener(this);
		pm.add(mi2);
		JMenuItem mi3 = new JMenuItem("Delete Last Pick");
		mi3.setActionCommand("deleteLastPick");
		mi3.addActionListener(this);
		pm.add(mi3);
		JMenuItem mi4 = new JMenuItem("Name Selected Horizon");
		mi4.setActionCommand("nameHorizon");
		mi4.addActionListener(this);
		pm.add(mi4);
	}
	
	public XMImage() {
		// cut down constructor which doesn't use UI, for use with XMRas2ToJPG, etc.
		border = null;
		image = null;
		cdpInterval = null;
		tRange = null;
		scroller = null;
		width = 100;
		height = 50;
		xRep = yRep = xAvg = yAvg = 1;
		scroller = null;
		line = null;
		otherImage = null;
	}
	
	void disposeImage() {
		image = null;
	}

	public void setDigitizer( Digitizer inputDig ) {
		dig = inputDig;
	}

	public void setOtherImage( XMImage other ) {
		otherImage = other;
	}
	
	public void loadImageFromFile( XMLine line ) throws IOException {
		if( line.getZRange()==null ) throw new IOException(" no data for "+line.getID());
		this.line = line;
		border = new XMBorder(this);
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
	
		File f = new File("img/" + line.getCruiseID().trim() +"-" + line.getID().trim() + ".r2.gz" );
		in = new DataInputStream(
			new GZIPInputStream(
			new BufferedInputStream(
			new FileInputStream(f))));
		
		loadImageFromDataStream(in);
	}
	
	public void loadImage( XMLine line ) throws IOException {
		if( line.getZRange()==null ) throw new IOException(" no data for "+line.getID());
		this.line = line;
		border = new XMBorder(this);
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
		InputStream urlIn;
		if (haxby.db.xmcs.XMCS.mcsDataSelect[1].isSelected()) {
			URL url = URLFactory.url( USGS_MULTI_CHANNEL_PATH + line.getCruiseID().trim() + "/img/" +
				line.getCruiseID().trim() +"-"+ line.getID().trim() + ".r2.gz" );
			urlIn = url.openStream();
		} else if (haxby.db.xmcs.XMCS.mcsDataSelect[2].isSelected()) {
			URL url = URLFactory.url( USGS_SINGLE_CHANNEL_PATH + line.getCruiseID().trim() + "/img/" +
					line.getCruiseID().trim() +"-"+ line.getID().trim() + ".r2.gz" );
			urlIn = url.openStream();
		}else if (haxby.db.xmcs.XMCS.mcsDataSelect[3].isSelected()){
			URL url = URLFactory.url(ANTARCTIC_SDLS_PATH + line.getCruiseID().trim() + "/img/" +
					line.getCruiseID().trim() +"-"+ line.getID().trim() + ".r2.gz");
			urlIn = url.openStream();
		}
		else {
			URL url = URLFactory.url( MULTI_CHANNEL_PATH + line.getCruiseID().trim() + "/img/" +
					line.getCruiseID().trim() +"-"+ line.getID().trim() + ".r2.gz" );
			urlIn = url.openStream();
		}
		in = new DataInputStream(
				new GZIPInputStream(
				new BufferedInputStream(urlIn)));
		loadImageFromDataStream(in);
	}

	private void loadImageFromDataStream(DataInputStream in) throws IOException {
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
		image = new R2(bitmap, width, height);
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
		return line.getCruise().getID() + "_" + line.getID();
	}
	public XMLine getLine() {
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
		border = new XMBorder(this);
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
		if (line != null) line.drawCDP( -1 );
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
					XMCrossing crs = (XMCrossing)line.crossings.get(k);
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
//		if(!image.isFlip() && getZoomX()==1 && getZoomY()==1) {
//			g2.drawImage(image.getImage(), 0, 0, this);
//		} else {
//			rect = image.getImageableRect(g2.getClipBounds(), xAvg, yAvg, xRep, yRep);
//			if(rect.width >0 && rect.height>0 ) {
//				BufferedImage im = image.getScaledImage(rect, xAvg, yAvg, xRep, yRep);
//				g2.drawImage( im, rect.x, rect.y, this);
//			}
//		}

		rect = image.getImageableRect(g2.getClipBounds(), xAvg, yAvg, xRep, yRep);
		if(rect.width >0 && rect.height>0 ) {
			BufferedImage im = image.getScaledImage(rect, xAvg, yAvg, xRep, yRep);
			g2.drawImage( im, rect.x, rect.y, this);
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
					XMCrossing crs = (XMCrossing)line.crossings.get(k);
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
	public double[] getScales() {
		return new double[] { getZoomX(), getZoomY() };
	}
	public Insets getInsets() {
		return border.getBorderInsets(this);
	}
	public double getZoomX() {
		return (double)xRep / (double)xAvg;
	}
	public double getZoomY() {
		return (double)yRep / (double)yAvg;
	}
	public void setXY(Point p) {
	}
	public void setRect(Rectangle rect) {
	}
	public void newRectangle(Rectangle rect) {
	}
	public void zoomTo(Rectangle rect) {
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
	public Point undoReverseProcessPoint(Point2D processedPoint) {
		Insets ins = getInsets();
		Point p = new Point((int)processedPoint.getX(),(int)processedPoint.getY());
		double[] scales = getScales();
		p.x -= ins.left;
		p.y -= ins.top;
		p.x /= scales[0];
		p.y /= scales[1];
		return p;
	}

	public double timeAt( int y ) {
		if( tRange == null) return Double.NaN;
		Insets ins = border.getBorderInsets(this);
		double zoomY = getZoomY();
		zoomY /= (tRange[1]-tRange[0]) / (double)height;
		double c = (y - ins.top)/zoomY;
		double min = Math.min( tRange[0], tRange[1]) - tRange[0];
		double max = Math.max( tRange[0], tRange[1]) - tRange[0];
		if( c<min ) c=min;
		if( c>max ) c=max;
		return c + tRange[0];
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
		if( image==null || rightClick)return;
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
		scrollPoint = new Point(newX, newY);
		scroller.scrollTo(scrollPoint);
		repaint();
	}
	public void zoomOut(Point p) {
		if( image==null || rightClick )return;
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
		scrollPoint = new Point(newX, newY);
		scroller.scrollTo(scrollPoint);
		repaint();
	}
	public void setScroller(Scroller scroller) {
		this.scroller = scroller;
	}

	public void mouseClicked(MouseEvent e) {
	}
	
	public void mousePressed(MouseEvent e) {
		if( image==null || line==null ) {
			return;
		}
		rightClick = SwingUtilities.isRightMouseButton(e);

		tempInfo = line.map.getLonLat();
		int closedParenthesesNumber = 0;
		int secondCPIndex = 0;
		int i = 0;
		while ( i < tempInfo.length() && closedParenthesesNumber < 2 ) {
			if ( tempInfo.substring( i, i + 1 ).equals(")") ) {
				closedParenthesesNumber++;
			}
			if ( closedParenthesesNumber == 2 ) {
				secondCPIndex = i;
			}
			i++;
		}
		tempInfo = tempInfo.substring( 0, secondCPIndex + 1 );
		currentTime = timeAt(e.getY());
		currentCDP = cdpAt(e.getX());

		tryPopUp(e);
	}
	public void mouseReleased(MouseEvent e) {
		tryPopUp(e);
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

//		***** GMA 1.6.2: Give the XMLine object the current time and CMP# to display
		line.currentCMP = cdpAt( e.getX() );
		line.currentTime = timeAt( e.getY() )/1000.0;
//		***** GMA 1.6.2

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

//		***** GMA 1.6.4: If neither zoom in or zoom out button is selected, make sure cursor button is selected
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
		if(Integer.parseInt(cmd)==0) {
			if(zoomInTB.isSelected()) {
				zoomInTB.doClick();
				dig.getDigB().doClick();
			}
			if(zoomOutTB.isSelected()) {
				zoomOutTB.doClick();
				dig.getDigB().doClick();
			}
		}
		else if(Integer.parseInt(cmd)==1) {
			if(zoomInTB.isSelected()) {
				zoomInTB.doClick();
				dig.getAnnotB().doClick();
			}
			if(zoomOutTB.isSelected()) {
				zoomOutTB.doClick();
				dig.getAnnotB().doClick();
			}
		}

		}catch(NumberFormatException nfe){}
//		***** GMA 1.6.4

		if(image==null) {
			if(cmd.equals("RevVid")) {
				rev = !rev;
				if(zoomInTB.isSelected()) {
					zoomInTB.doClick();
				}
				if(zoomOutTB.isSelected()) {
					zoomOutTB.doClick();
				}
			} else if( cmd.equals("flip")) {
				flip = !flip;
				if(zoomInTB.isSelected()) {
					zoomInTB.doClick();
				}
				if(zoomOutTB.isSelected()) {
					zoomOutTB.doClick();
				}
			}
			return;
		}
		if(cmd.equals("RevVid")) {
			revVid();
		}

//		***** GMA 1.6.2: Copy the MCS data for the current point to the clipboard if the user 
//		selects the "Copy to Clipboard" option (option appears as pop-up menu on right-click).
		else if ( cmd.equals("copy") ) {
			copy();
		}
//		***** GMA 1.6.2

		else if( cmd.equals("wider")) {
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
			
			// so we can flip horizons, store the digitized values for the picks and then undigitize them after the flip
			try {
				Vector<LineSegmentsObject> horizons = dig.getObjects();
				for (LineSegmentsObject horizon : horizons) {
					Vector<double[]> points = (Vector<double[]>)horizon.getPoints();
					for (double[] xyz : points) {
						Point tempP = new Point( (int)xyz[0], (int)xyz[1] );
						Point2D resultP = reverseProcessPoint(tempP);
						xyz[1] = cdpAt((int) resultP.getX());
						xyz[0] = timeAt((int)(resultP.getY()));
					}
					
				}
			} catch(Exception ex) {
			}

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
			
			// undo the digitized values to get new xy values for the horizons
			try {
				Vector<LineSegmentsObject> horizons = dig.getObjects();
				for (LineSegmentsObject horizon : horizons) {
					for (double[] xyz : (Vector<double[]>)horizon.getPoints()) {
						double resultPy = undoTimeAt(xyz[0]);
						double resultPx = undoCdpAt(xyz[1]);
						Point newPoint = undoReverseProcessPoint(new Point2D.Double(resultPx,resultPy));
						xyz[0] = newPoint.getX();
						xyz[1] = newPoint.getY();
					}
				}	
			} catch(Exception ex) {
			}
			
			invalidate();
			int newX = (int) (x*zoomX - w*.5d);
			int newY = (int) (y*zoomY - h*.5d);
			synchronized(this) {
				scroller.validate();
			}
			scrollPoint = new Point(newX, newY);
			scroller.scrollTo(scrollPoint);
			repaint();
		} else if( cmd.equals("save")) {
			new Thread(){
				public void run() {
					try {
						saving = true;
						save();
						saving = false;
					}
					catch (IOException ex) { }
					catch (Throwable t) {
						t.printStackTrace();
					}
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
		else if (cmd.equals("deleteLastHorizon")) {
			if (dig.getObjects().size() == 0) return;
			String msg = "Are you sure you wish to delete the last horizon?";
			int n = JOptionPane.showConfirmDialog(this, msg, "Confirm Horizon Deletion", JOptionPane.YES_NO_OPTION);
			if (n == JOptionPane.NO_OPTION) return;
			dig.deleteLastObject();
		}
		else if (cmd.equals("deleteLastPick")) {
			if (dig.getObjects().size() == 0) return;
			try {
				for( int i=0 ; i<dig.getObjects().size(); i++ ) {
					try {
						((DigitizerObject) dig.getObjects().get(i)).setSelected(false);
					} catch( Exception ex) {}
				}
				LineSegmentsObject obj = (LineSegmentsObject) dig.getObjects().lastElement();
				obj.setSelected(true);
				if (obj.getPoints().size() > 0) {
					obj.getPoints().remove(obj.getPoints().lastElement());
					if (obj.getPoints().size() == 0) {
						dig.deleteLastObject(false);
					}
					panel.repaint();
				}
			}
			catch(Exception ex) {return;};
		}
		else if (cmd.equals("nameHorizon")) {
			try {
				Vector<LineSegmentsObject> horizons = dig.getObjects();
				LineSegmentsObject selHorizon = null;
				int count = 0;
				for (LineSegmentsObject h : horizons) {
					if (h.isSelected()) {
						selHorizon = h;
						count ++;
					}
				}
				if (selHorizon != null && count == 1 ) {
					String s = (String)JOptionPane.showInputDialog(this,"Allocate a name to the selected horizon for the Save file", "Name Horizon", JOptionPane.PLAIN_MESSAGE);
					if ((s != null) && (s.length() > 0)) {
						selHorizon.setName(s);
					}
				} else {
					JOptionPane.showMessageDialog(this, "Please select the horizon you wish to name by clicking \n"
							+ "the Select pointer/cursor button and then clicking anywhere on the line.", "Name Horizon", JOptionPane.INFORMATION_MESSAGE);
				}
			}
			catch(Exception ex) {return;}
		}
	}

	public void saveDigitizedProducts() {
		JFileChooser chooser = MapApp.getFileChooser();
		Vector objects = dig.getObjects();
		String horiz_text;
		if (objects.size() > 1) {
			horiz_text = "_Horizons.txt";
		} else {
			horiz_text = "_Horizon1.txt";
		}
		chooser.setSelectedFile(new File( this.line.getCruiseID() + "_" +this.line.lineID + horiz_text));
		int ok = chooser.showSaveDialog( getTopLevelAncestor() );

		if( ok == JFileChooser.CANCEL_OPTION )return;
		File file = chooser.getSelectedFile();
		try {
			PrintStream out = new PrintStream( new FileOutputStream(file) );
			out.println(line.getCruiseID()+" "+line.lineID);//added line data to the text file DEP 9.13.2011
			out.println("Longitude\tLatitude\tTwo-Way Travel Time (secs)\tCMP#");

			Vector points;
			double[] xy;
			for( int k=0 ; k<objects.size() ; k++ ) {
				LineSegmentsObject obj = (LineSegmentsObject)objects.get(k);
				points = (Vector)obj.getPoints();
				if( obj instanceof AnnotationObject ) {
					out.println(">\t" + obj.toString() +"\t"+ points.size() 
							+"\t"+ scrollPoint.getX() + "\t" + scrollPoint.getY() + "\t" + xAvg + "\t" + xRep + "\t" + yAvg + "\t" + yRep + "\t" + flip
							+"\t"+  ((AnnotationObject)obj).getAnnotation() );
				} else {
					out.println(">\t" + obj.toString() +"\t"+ points.size() 
					+"\t"+ scrollPoint.getX() + "\t" + scrollPoint.getY() + "\t" + xAvg + "\t" + xRep + "\t" + yAvg + "\t" + yRep+ "\t" + flip);
				}
				if( points.size()==0 ) continue;
				for( int i=0 ; i<points.size() ; i++ ) {
					NumberFormat latLonFormat = NumberFormat.getInstance();
					latLonFormat.setMaximumFractionDigits(6);
					latLonFormat.setMinimumFractionDigits(6);
					NumberFormat timeFormat = NumberFormat.getInstance();
					timeFormat.setMaximumFractionDigits(6);
					timeFormat.setMinimumFractionDigits(6);
					
					xy = (double[])points.get(i);
					
					Point tempP = new Point( (int)xy[0], (int)xy[1] );
					Point2D resultP = reverseProcessPoint(tempP);
					
					Point2D p = line.pointAtCDP(cdpAt((int)resultP.getX()));
					Point2D latLonP = line.map.getProjection().getRefXY(p);
					double tempX = latLonP.getX();
					while ( tempX > 180. ) {
						tempX -= 360.;
					}


					latLonP = new Point2D.Double( tempX, latLonP.getY() );
					Point2D test = new Point2D.Double(undoCdpAt(cdpAt((int)resultP.getX())), undoTimeAt(timeAt((int)(resultP.getY()))));
				
					//System.out.println(""+undoReverseProcessPoint(test).x);
					//System.out.println(""+undoReverseProcessPoint(test).y);
					//System.out.println(""+line.cdp[cdpAt((int)resultP.getX())].getY());
					out.println(latLonFormat.format(latLonP.getX()) + "\t" + latLonFormat.format(latLonP.getY()) + "\t" + timeFormat.format((timeAt((int)(resultP.getY())))/1000.0) + "\t" + cdpAt((int)resultP.getX()));
				}
			}
			out.close();
			MapApp.sendLogMessage("Saving_or_Downloading&portal=Digital Seismic Reflection Profiles (MCS & SCS)&what=digitized_product&cruise="+this.line.getCruiseID()+"&line=" +(String) this.line.lineID );
		} catch(IOException ex) {
			ex.printStackTrace();
		}
	}

	void save()	throws IOException {
		if(line == null) {
			JOptionPane.showMessageDialog(getTopLevelAncestor(), "Image not loaded.");
			return;
		}

//		***** GMA 1.6.2: Re-organize the "Save what?" window so that "Download segy from UTIG"
//		is a separate option that cannot be grouped with the other save options.
		JPanel savePanel = new JPanel( new BorderLayout() );
		savePanel.setBorder(BorderFactory.createEmptyBorder( 0, 5, 0, 5));
		JPanel saveSegyPrompt = new JPanel( new BorderLayout() );

		JPanel savePrompt = new JPanel(new GridLayout(0, 1));
		imageCB = new JCheckBox("Save viewport jpg");
		imageFullCB = new JCheckBox("Save full jpg");
		segyCB = new JCheckBox("Download segy from UTIG");
		navCB = new JCheckBox("Save nav");
		if(haxby.db.xmcs.XMCS.mcsDataSelect[0].isSelected()){
			segyCB.setText("Download SEGY from the Website");
			try{
				//I assume every segy and nav are in the database under the same uid or else they are unavailable.
				URL url = URLFactory.url(MULTI_CHANNEL_PATH + "mcs_lookup/"+line.getCruiseID()+".data_lookup");
				BufferedReader in = new BufferedReader( new InputStreamReader(url.openStream()) );
				segyCB.setEnabled(true);
				navCB.setEnabled(true);
			}
			catch(FileNotFoundException e){
				segyCB.setEnabled(false);
				navCB.setEnabled(false);
			}
		}
		else if (haxby.db.xmcs.XMCS.mcsDataSelect[1].isSelected()) {
			segyCB.setText("Download MCS segy from USGS");
		} else if (haxby.db.xmcs.XMCS.mcsDataSelect[2].isSelected()) {
			segyCB.setText("Download SCS segy from USGS");
		}else if(haxby.db.xmcs.XMCS.mcsDataSelect[3].isSelected()) {
			segyCB.setText("Download sgy from SDLS");
			segyCB.setText("Download SEGY from the Website");
			try{
				//I assume every segy and nav are in the database under the same uid or else they are unavailable.
				URL url = URLFactory.url(MULTI_CHANNEL_PATH + "sdls/"+line.getCruiseID()+"/img/" + line.getCruiseID()+"-"+line.getID() + ".jpg");
				BufferedReader in = new BufferedReader( new InputStreamReader(url.openStream()) );
				segyCB.setEnabled(true);
				navCB.setEnabled(true);
			}
			catch(FileNotFoundException e){
				imageFullCB.setEnabled(false);
			}
			
			try{
				//I assume every segy and nav are in the database under the same uid or else they are unavailable.
				URL url = URLFactory.url(MULTI_CHANNEL_PATH + "sdls/"+line.getCruiseID()+"/nav/" + line.getCruiseID()+"-"+line.getID() + ".nav");
				BufferedReader in = new BufferedReader( new InputStreamReader(url.openStream()) );
				
				navCB.setEnabled(true);
			}
			catch(FileNotFoundException e){
				navCB.setEnabled(false);
			}
		}

		imageCB.setName("imageCB");
		imageFullCB.setName("imageFullCB");
		segyCB.setName("segyCB");
		navCB.setName("navCB");

		imageCB.addItemListener(this);
		imageFullCB.addItemListener(this);
		segyCB.addItemListener(this);
		navCB.addItemListener(this);

		savePrompt.add(imageCB);
		savePrompt.add(imageFullCB);
		savePrompt.add(navCB);
		savePanel.add(savePrompt, BorderLayout.NORTH);
		saveSegyPrompt.add( new JSeparator( JSeparator.HORIZONTAL ), BorderLayout.NORTH );
		saveSegyPrompt.add(segyCB, BorderLayout.WEST );
		savePanel.add(saveSegyPrompt, BorderLayout.SOUTH);

		ButtonGroup boxGroup = new ButtonGroup();
		boxGroup.add(imageFullCB);
		boxGroup.add(imageCB);
		boxGroup.add(segyCB);
		boxGroup.add(navCB);

//		***** GMA 1.6.2

		int s = JOptionPane.showConfirmDialog(getTopLevelAncestor(), savePanel, "Save Options", JOptionPane.OK_CANCEL_OPTION);
		if(s == 2) {
			return;
		}

//		***** GMA 1.6.2: Remove segyCB from the group of save options, segyCB is no longer 
//		selectable with the other save options.
/*
		boolean multiple = (imageCB.isSelected() ? 1 : 0) +
							(imageFullCB.isSelected() ? 1 : 0) +
							(segyCB.isSelected() ? 1 : 0) +
							(navCB.isSelected() ? 1 : 0) > 1;
*/

		boolean multiple = (imageCB.isSelected() ? 1 : 0) +
		(imageFullCB.isSelected() ? 1 : 0) +
		(navCB.isSelected() ? 1 : 0) > 1;
//		***** GMA 1.6.2

		if (multiple) { // Save as a zip
			File file = new File(this + ".zip");
			while (true) {
				
				FileDialog fd = new FileDialog((Frame)null, "Save", FileDialog.SAVE);
				if(null == lastSavedDir) {
					fd.setDirectory(System.getProperty("user.home"));
				}
				else {
					fd.setDirectory(lastSavedDir);
				}
				fd.setFile(file.getName());
				fd.setVisible(true);
				
				String dir = fd.getDirectory();
				String fileName = fd.getFile();
				fd.dispose();
				
				if (dir != null && fileName != null) {
					lastSavedDir = dir;
					if (!fileName.endsWith(".zip"))
					    fileName += ".zip";
					file = new File(dir,fileName);
					System.out.println(file.getAbsolutePath());
					break;
				} else return;
			}
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

	ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(
					new FileOutputStream(file)));
			try {
				if (imageCB.isSelected()) {
					ZipEntry ze = new ZipEntry(this + ".jpg");
					zos.putNextEntry(ze);
					saveJPEG(zos);
					MapApp.sendLogMessage("Saving_or_Downloading&portal=Digital Seismic Reflection Profiles (MCS & SCS)&what=image_viewport&cruise="+this.line.getCruiseID()+"&line=" +(String) this.line.lineID );
				}
				if (imageFullCB.isSelected()) {
					ZipEntry ze = new ZipEntry(this + "Full.jpg");
					zos.putNextEntry(ze);
					saveFullJPEG(zos);
					MapApp.sendLogMessage("Saving_or_Downloading&portal=Digital Seismic Reflection Profiles (MCS & SCS)&what=image_full&cruise="+this.line.getCruiseID()+"&line=" +(String) this.line.lineID );
				}

//				***** GMA 1.6.2: Remove segyCB from the group of save options that can be 
//				selected together.
/*
				if (segyCB.isSelected()) {
				ZipEntry ze = new ZipEntry(getID() + ".segy");
					zos.putNextEntry(ze);
					saveSEGY(zos);
				}
*/
//				***** GMA 1.6.2

				if (navCB.isSelected()) {
					ZipEntry ze = new ZipEntry(this + ".nav");
					zos.putNextEntry(ze);
					saveNAV(zos);
					MapApp.sendLogMessage("Saving_or_Downloading&portal=Digital Seismic Reflection Profiles (MCS & SCS)&what=nav&cruise="+this.line.getCruiseID()+"&line=" +(String) this.line.lineID );
				}
				zos.close();
				JOptionPane.showMessageDialog(MapApp.anchor, "Save Successful");
			}
			catch(IOException ex) {
				JOptionPane.showMessageDialog(MapApp.anchor, ex.getMessage(), "Error Writing Zip", JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
			setCursor(Cursor.getDefaultCursor());
		} else if (imageCB.isSelected()) { // Save image

			File file = new File(this + "_viewport.jpg");
			while (true) {
				//replaced JFileChoosers with FileDialog as JFileChooser was freezing in some instances
				FileDialog fd = new FileDialog((Frame)null, "Save", FileDialog.SAVE);
				if(null == lastSavedDir) {
					fd.setDirectory(System.getProperty("user.home"));
				}
				else {
					fd.setDirectory(lastSavedDir);
				}
				fd.setFile(file.getName());
				fd.setVisible(true);
				
				String dir = fd.getDirectory();
				String fileName = fd.getFile();
				fd.dispose();
				
				if (dir != null && fileName != null) {
					lastSavedDir = dir;
					if (!fileName.endsWith(".jpg"))
					    fileName += ".jpg";
					file = new File(dir,fileName);
					System.out.println(file.getAbsolutePath());
					break;
				} else return;

			}
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			try {
				saveJPEG(out);
				out.close();
				JOptionPane.showMessageDialog(MapApp.anchor, "Save Successful");
				MapApp.sendLogMessage("Saving_or_Downloading&portal=Digital Seismic Reflection Profiles (MCS & SCS)&what=image_viewport&cruise="+this.line.getCruiseID()+"&line=" +(String) this.line.lineID );
			}
			catch(IOException ex) {
				JOptionPane.showMessageDialog(MapApp.anchor, ex.getMessage(), "Error Writing Jpeg", JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}

			setCursor(Cursor.getDefaultCursor());
		} else if (imageFullCB.isSelected()) { // Save Full image
			File file = new File(this + ".jpg");
			while (true) {
				
				FileDialog fd = new FileDialog((Frame)null, "Save", FileDialog.SAVE);
				if(null == lastSavedDir) {
					fd.setDirectory(System.getProperty("user.home"));
				}
				else {
					fd.setDirectory(lastSavedDir);
				}
				fd.setFile(file.getName());
				fd.setVisible(true);
				
				String dir = fd.getDirectory();
				String fileName = fd.getFile();
				fd.dispose();
				
				if (dir != null && fileName != null) {
					lastSavedDir = dir;
					if (!fileName.endsWith(".jpg"))
					    fileName += ".jpg";
					file = new File(dir,fileName);
					System.out.println(file.getAbsolutePath());
					break;
				} else return;
			}
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			try {
				saveFullJPEG(out);
				out.close();
				JOptionPane.showMessageDialog(MapApp.anchor, "Save Successful");
				MapApp.sendLogMessage("Saving_or_Downloading&portal=Digital Seismic Reflection Profiles (MCS & SCS)&what=image_full&cruise="+this.line.getCruiseID()+"&line=" +(String) this.line.lineID );
			}
			catch(IOException ex) { 
				JOptionPane.showMessageDialog(MapApp.anchor, ex.getMessage(), "Error Writing Jpeg", JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
			setCursor(Cursor.getDefaultCursor());
		} else if (navCB.isSelected()) { // Save Full image
			File file = new File(this + ".nav");
			while (true) {
				
				FileDialog fd = new FileDialog((Frame)null, "Save", FileDialog.SAVE);
				if(null == lastSavedDir) {
					fd.setDirectory(System.getProperty("user.home"));
				}
				else {
					fd.setDirectory(lastSavedDir);
				}
				fd.setFile(file.getName());
				fd.setVisible(true);
				
				String dir = fd.getDirectory();
				String fileName = fd.getFile();
				fd.dispose();
				
				if (dir != null && fileName != null) {
					lastSavedDir = dir;
					if (!fileName.endsWith(".nav"))
					    fileName += ".nav";
					file = new File(dir,fileName);
					System.out.println(file.getAbsolutePath());
					break;
				} else return;
			}
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			try {
				saveNAV(out);
				out.close();
				JOptionPane.showMessageDialog(MapApp.anchor, "Save Successful");
				MapApp.sendLogMessage("Saving_or_Downloading&portal=Digital Seismic Reflection Profiles (MCS & SCS)&what=nav&cruise="+this.line.getCruiseID()+"&line=" +(String) this.line.lineID );
			}
			catch(IOException ex) { 
				JOptionPane.showMessageDialog(MapApp.anchor, ex.getMessage(), "Error Writing NAV", JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
			setCursor(Cursor.getDefaultCursor());
		} else if (segyCB.isSelected()) {

//			***** GMA 1.6.2: The segyCB save option now opens a browser window to the UTIG site 
//			for the selected cruise
/*
			File file = new File(getID() + ".segy");
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
//				if(o == JOptionPane.CANCEL_OPTION) return;
				if(o == JOptionPane.OK_OPTION)
					break;
			}
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));		
			try {
				saveSEGY(out);
				out.close();
				JOptionPane.showMessageDialog(MapApp.anchor, "Save Successful");
			}
			catch(IOException ex) { 
				JOptionPane.showMessageDialog(MapApp.anchor, ex.getMessage(), "Error Writing Segy", JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
			setCursor(Cursor.getDefaultCursor());
*/
			// Relates to download SEGY depends which files are in view.
			String urlString;
			if(haxby.db.xmcs.XMCS.mcsDataSelect[0].isSelected()){
				String selectedDataUID="";
				try {
					URL url = URLFactory.url(MULTI_CHANNEL_PATH + "mcs_lookup/"+line.getCruiseID()+".data_lookup");
					BufferedReader in = new BufferedReader( new InputStreamReader(url.openStream()) );
					String st;
					while ( (st = in.readLine()) != null ) {

						String[] split = st.split("\\s");
						// Try and find the UID for he exact line in the lookup table.
						// If we can't find it, then since we are actually just resolving at the dataset level, all the 
						// UIDs should be the same anyway
						if(split.length>=3)
							selectedDataUID = split[2];
						if((split[0].equalsIgnoreCase(line.getID()) || split[0].equalsIgnoreCase(line.getCruiseID()+"-"+line.getID())) && split[1].equalsIgnoreCase("segy")){
							break;
						}
					}
				} catch (IOException ec) {
					ec.printStackTrace();
				}
				if(selectedDataUID.isEmpty())
					return;

				String str = "http://www.marine-geo.org/tools/search/Files.php?client=GMA&data_set_uid="+ selectedDataUID;
				BrowseURL.browseURL(str);

			}
			else if (haxby.db.xmcs.XMCS.mcsDataSelect[1].isSelected()){
				urlString = "https://cmgds.marine.usgs.gov/cruise.php?cruise=" + getCruiseID().toLowerCase();
				BrowseURL.browseURL(urlString);
			} else if (haxby.db.xmcs.XMCS.mcsDataSelect[2].isSelected()) {
				urlString = "https://cmgds.marine.usgs.gov/cruise.php?cruise=" + getCruiseID().toLowerCase();
				BrowseURL.browseURL(urlString);
			}else if (haxby.db.xmcs.XMCS.mcsDataSelect[3].isSelected()){
				urlString = "https://sdls.ogs.trieste.it/CORE/SelFile1.php?FileType=Seismics&Field=LineName&val="+getCruiseID().toLowerCase();
				BrowseURL.browseURL(urlString);
			}else {
				urlString = "https://www.ig.utexas.edu/sdc/cruise.php?cruiseIn=" + getCruiseID().toLowerCase();
				BrowseURL.browseURL(urlString);
			}
//			***** GMA 1.6.2

		} else if (navCB.isSelected()) {
			String selectedDataUID="";
			try {
				URL url = URLFactory.url(MULTI_CHANNEL_PATH + "mcs_lookup/"+line.getCruiseID()+".data_lookup");
				BufferedReader in = new BufferedReader( new InputStreamReader(url.openStream()) );

				String st;
				while ( (st = in.readLine()) != null ) {
					String[] split = st.split("\\s");
					if((split[0].equalsIgnoreCase(line.getID()) || split[0].equalsIgnoreCase(line.getCruiseID()+"-"+line.getID()))){
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

			String str = "http://www.marine-geo.org/tools/search/Files.php?client=GMA&data_set_uid="+ selectedDataUID;
			BrowseURL.browseURL(str);
			/*
			File file = new File(getID() + ".nav");
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
//				if(o == JOptionPane.CANCEL_OPTION) return;
				if(o == JOptionPane.OK_OPTION)
					break;
			}
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			try {
				saveNAV(out);
				out.close();
				JOptionPane.showMessageDialog(MapApp.anchor, "Save Successful");
			}
		catch(IOException ex) { 
				JOptionPane.showMessageDialog(MapApp.anchor, ex.getMessage(), "Error Writing Nav", JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
			setCursor(Cursor.getDefaultCursor());
		*/}
	}

	private void saveNAV(OutputStream out) throws IOException {
		if(image==null) throw new IOException("no image loaded");

		URL url;
		URLConnection urlCon;
		BufferedInputStream in;
		if (haxby.db.xmcs.XMCS.mcsDataSelect[0].isSelected()) {
			url = URLFactory.url( MULTI_CHANNEL_PATH + line.getCruiseID().trim() + "/nav/" +
					line.getCruiseID().trim() +"-"+ line.getID().trim() + ".nav" );
			urlCon = url.openConnection();
			in = new BufferedInputStream(urlCon.getInputStream());
		} else if (haxby.db.xmcs.XMCS.mcsDataSelect[1].isSelected()) {
			url = URLFactory.url( USGS_MULTI_CHANNEL_PATH + line.getCruiseID().trim() + "/nav/" +
					line.getCruiseID().trim() +"-"+ line.getID().trim() + ".nav" );
			urlCon = url.openConnection();
			in = new BufferedInputStream(urlCon.getInputStream());
		} else if (haxby.db.xmcs.XMCS.mcsDataSelect[2].isSelected()) {
			url = URLFactory.url( USGS_SINGLE_CHANNEL_PATH + line.getCruiseID().trim() + "/nav/" +
					line.getCruiseID().trim() +"-"+ line.getID().trim() + ".nav" );
			urlCon = url.openConnection();
			in = new BufferedInputStream(urlCon.getInputStream());
		} else if (haxby.db.xmcs.XMCS.mcsDataSelect[3].isSelected()) {
			url = URLFactory.url( ANTARCTIC_SDLS_PATH + line.getCruiseID().trim() + "/nav/" +
					line.getCruiseID().trim() +"-"+ line.getID().trim() + ".nav" );
			urlCon = url.openConnection();
			in = new BufferedInputStream(urlCon.getInputStream());
		} else {
			url = URLFactory.url( MULTI_CHANNEL_PATH+ line.getCruiseID().trim() + "/nav/" +
					line.getCruiseID().trim() +"-"+ line.getID().trim() + ".nav" );
			urlCon = url.openConnection();
			in = new BufferedInputStream(urlCon.getInputStream());
		}
		int length = urlCon.getContentLength();
	System.out.println("nav: " + urlCon.getURL());
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
	private void saveSEGY(OutputStream out) throws IOException {
		if(image==null) throw new IOException("no image loaded");

		String mcsPath = PathUtil.getPath("PORTALS/MULTI_CHANNEL_PATH",
				MapApp.BASE_URL+"/data/portals/mcs/");

		URL url = URLFactory.url( mcsPath + line.getCruiseID().trim() + "/segy/" +
				line.getCruiseID().trim() +"-"+ 
				line.getID().trim() + ".segy" );
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
	private void saveFullJPEG(OutputStream out) throws IOException {
		if(image==null) throw new IOException("no image loaded");
		
		URL url;
		URLConnection urlCon;
		BufferedInputStream in;

		if (haxby.db.xmcs.XMCS.mcsDataSelect[0].isSelected()) {
			url = URLFactory.url( MULTI_CHANNEL_PATH + line.getCruiseID().trim() + "/img/" +
					line.getCruiseID().trim() +"-"+ line.getID().trim() + ".jpg" );
			urlCon = url.openConnection();
			in = new BufferedInputStream(urlCon.getInputStream());
		} else if (haxby.db.xmcs.XMCS.mcsDataSelect[1].isSelected()) {
			url = URLFactory.url( USGS_MULTI_CHANNEL_PATH + line.getCruiseID().trim() + "/img/" +
					line.getCruiseID().trim() +"-"+ line.getID().trim() + ".jpg" );
			urlCon = url.openConnection();
			in = new BufferedInputStream(urlCon.getInputStream());
		} else if (haxby.db.xmcs.XMCS.mcsDataSelect[2].isSelected()) {
			url = URLFactory.url( USGS_SINGLE_CHANNEL_PATH + line.getCruiseID().trim() + "/img/" +
					line.getCruiseID().trim() +"-"+ line.getID().trim() + ".jpg" );
			urlCon = url.openConnection();
			in = new BufferedInputStream(urlCon.getInputStream());
		} else if (haxby.db.xmcs.XMCS.mcsDataSelect[3].isSelected()) {
			url = URLFactory.url( ANTARCTIC_SDLS_PATH + line.getCruiseID().trim() + "/img/" +
					line.getCruiseID().trim() +"-"+ line.getID().trim() + ".jpg" );
			urlCon = url.openConnection();
			in = new BufferedInputStream(urlCon.getInputStream());
		} else {
			url = URLFactory.url( MULTI_CHANNEL_PATH + line.getCruiseID().trim() + "/img/" +
					line.getCruiseID().trim() +"-"+ line.getID().trim() + ".jpg" );
			urlCon = url.openConnection();
			in = new BufferedInputStream(urlCon.getInputStream());
		}

		int length = urlCon.getContentLength();
	System.out.println("jpg: " + urlCon.getURL());
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
		scrollPoint = new Point(newX, newY);
		scroller.scrollTo(scrollPoint);
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
		scrollPoint = new Point(newX, newY);
		scroller.scrollTo(scrollPoint);
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
		haxby.util.SimpleBorder border = new haxby.util.SimpleBorder();
		toolBar = new JToolBar(JToolBar.HORIZONTAL);
//		toolBar.setFloatable( false );
		toolBar.setFloatable(true);
		JButton b = new JButton(Icons.getIcon(Icons.WIDER,false));
		b.setPressedIcon( Icons.getIcon(Icons.WIDER,true) );
		b.setBorder(null);
		b.addActionListener(this);
		toolBar.add(b);
		b.setActionCommand("wider");

//		***** GMA 1.6.2: Add tooltiptext to the "wider" button
		b.setToolTipText("Expand");

		b = new JButton(Icons.getIcon(Icons.NARROWER,false));
		b.setPressedIcon( Icons.getIcon(Icons.NARROWER,true) );
		b.setBorder(null);
		b.addActionListener(this);
		toolBar.add(b);
		b.setActionCommand("narrower");

//		***** GMA 1.6.2: Add tooltiptext to the "narrower" button
		b.setToolTipText("Compress");

//		***** GMA 1.6.2: Change the icon for the "RevVid" button
		JToggleButton tb = new JToggleButton(Icons.getIcon(Icons.POSITIVE_BOX,false));
		tb.setSelectedIcon(Icons.getIcon(Icons.NEGATIVE_BOX,true));

		tb.setBorder(null);
		tb.addActionListener(this);
		toolBar.add(tb);
		tb.setActionCommand("RevVid");

//		***** GMA 1.6.2: Add tooltiptext to the "RevVid" button
		tb.setToolTipText("Negative/Positive Image");
//		***** GMA 1.6.2

		flipTB = new JToggleButton(Icons.getIcon(Icons.FORWARD,false));
		flipTB.setSelectedIcon(Icons.getIcon(Icons.REVERSE,true));
		flipTB.setBorder(null);
		flipTB.addActionListener(this);
		toolBar.add(flipTB);
		flipTB.setActionCommand("flip");
		flipTB.setToolTipText("Flip");

		b = new JButton(Icons.getIcon(Icons.SAVE,false));
		b.setPressedIcon( Icons.getIcon(Icons.SAVE,true) );
		b.setBorder(null);
		b.addActionListener(this);
		toolBar.add(b);
		b.setToolTipText("Save Image or Data");
		b.setActionCommand("save");

//		***** GMA 1.6.2: Add zoom buttons to the MCS view area

		ButtonGroup zoomGroup = new ButtonGroup();
		JToggleButton no = new JToggleButton();
		zoomGroup.add(no);

//		***** GMA 1.6.4: Add cursor button and digitize button to MCS toolbar

//		cursorTB = new JToggleButton( Icons.getIcon( Icons.SELECT, false ) );
//		cursorTB.setPressedIcon( Icons.getIcon( Icons.SELECT, true ) );
//		cursorTB.setSelectedIcon( Icons.getIcon( Icons.SELECT, true ) );
//		cursorTB.setBorder(null);
//		cursorTB.addActionListener(this);
//		toolBar.add(cursorTB);
//		cursorTB.setToolTipText("Default Cursor");
//		zoomGroup.add(cursorTB);
//		cursorTB.setSelected(true);
//		digitizeTB = new JToggleButton( Icons.getIcon( Icons.DIGITIZE, false ) );
//		digitizeTB.setPressedIcon( Icons.getIcon( Icons.DIGITIZE, true ) );
//		digitizeTB.setSelectedIcon( Icons.getIcon( Icons.DIGITIZE, true ) );
//		digitizeTB.setBorder(null);
//		digitizeTB.setActionCommand("Digitize Point");
//		digitizeTB.addActionListener(this);
//		toolBar.add(digitizeTB);
		digitizeTB = new JToggleButton(); // Placeholder to prevent NPE

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

//		digitizeTB.setToolTipText("Digitize Point (Shift-Click/Right-Click)");
//		zoomGroup.add(digitizeTB);
//		new ToggleToggler( digitizeTB, no );
//		***** GMA 1.6.4

		zoomInTB = zoomer.getZoomIn();
		zoomInTB.setPressedIcon( Icons.getIcon(Icons.ZOOM_IN,true) );
		zoomInTB.setSelectedIcon( Icons.getIcon(Icons.ZOOM_IN,true) );
		zoomInTB.setBorder(null);
		zoomInTB.setActionCommand("Zoom In");
		zoomInTB.addActionListener(this);
		toolBar.add(zoomInTB);
		zoomInTB.setToolTipText("Zoom In (Ctrl-Click)");
		zoomGroup.add(zoomInTB);
		new ToggleToggler( zoomInTB, no );
		zoomOutTB = zoomer.getZoomOut();
		zoomOutTB.setPressedIcon( Icons.getIcon(Icons.ZOOM_OUT,true) );
		zoomOutTB.setSelectedIcon( Icons.getIcon(Icons.ZOOM_OUT,true) );
		zoomOutTB.setBorder(null);
		zoomOutTB.setActionCommand("Zoom Out");
		zoomOutTB.addActionListener(this);
		toolBar.add(zoomOutTB);
		zoomOutTB.setToolTipText("Zoom Out (Ctrl-Shift-Click)");
		zoomGroup.add(zoomOutTB);
		new ToggleToggler( zoomOutTB, no );

		b = new JButton(org.geomapapp.util.Icons.getIcon(org.geomapapp.util.Icons.CLOSE, false));
		b.setToolTipText("Close Image");
		b.setBorder(null);
		b.setSelectedIcon(org.geomapapp.util.Icons.getIcon(org.geomapapp.util.Icons.CLOSE, true));
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(line != null)
					closeLine();
			}
		});
		toolBar.add(b);

		//DEP 9.9.11: Added a button for loading from an external xy file
		JButton load;
		load = new JButton( org.geomapapp.util.Icons.getIcon(org.geomapapp.util.Icons.OPEN,false) );
		load.setToolTipText("Load Previously Digitized Products");
		load.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) {
				try {
					setLine(line, true);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});

		toolBar.add(load);
//		***** GMA 1.6.2
	}
	protected void closeLine() {

		XMap map = line.map;
		line = null;
		border = null;
		image = null;
		System.gc();
		panel.repaint();
		otherImage.repaint();
		map.repaint();

		dig.getSelectB().doClick(0);

		dig.reset();
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

//	***** GMA 1.6.2: Ensures only one toggle button in a group can be selected at one time
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
//	***** GMA 1.6.2

//	***** GMA 1.6.2: If segyCB is selected the other save options cannot be selected and vice versa.
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		if ( source == imageCB && imageCB.isSelected() ) {
			segyCB.setSelected(false);
		}
		else if ( source == imageFullCB && imageFullCB.isSelected() ) {
			segyCB.setSelected(false);
		}
		else if ( source == segyCB && segyCB.isSelected() ) {
			imageCB.setSelected(false);
			imageFullCB.setSelected(false);
			navCB.setSelected(false);
		}
		else if ( source == navCB && navCB.isSelected() ) {
			segyCB.setSelected(false);
		}
	}
//	***** GMA 1.6.2

//	***** GMA 1.6.2: Functions to display the pop-up menu and copy the MCS data for 
//	the current point to the clipboard.
	public void tryPopUp(MouseEvent evt){
		String osName = System.getProperty("os.name");
		if ( !evt.isControlDown()) {
			if ( osName.startsWith("Mac OS") && evt.isShiftDown() ) {
				pm.show(evt.getComponent(), evt.getX(), evt.getY() );
			}
			else if ( evt.isPopupTrigger() ) {
				pm.show(evt.getComponent(), evt.getX(), evt.getY());
			}
			else if ( digitizeTB.isSelected() ) {
				pm.show(evt.getComponent(), evt.getX(), evt.getY());
			}
			repaint();
		}
	}

	public void copy() {
		StringBuffer sb = new StringBuffer();
		sb.append(tempInfo);
		Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
		String tempString = sb.toString();
		tempString = tempString.replaceAll("zoom.+","");
		tempString = tempString.replaceAll("[\\(\\)=,\\w&&[^WESN\\d]]+","");
		String [] result = tempString.split("\\s+");
		tempString = "";
		for ( int i =2; i < result.length; i++ ) {
			if ( result[i].indexOf("\u00B0") != -1 && result[i].indexOf("\u00B4") == -1 ) {
				result[i] = result[i].replaceAll("\\u00B0","");
			}
			if ( i == 2 ) {
				if ( result[i].indexOf("W") != -1 ) {
					result[i] = "-" + result[i];
				}
				result[i] = result[i].replaceAll("[WE]","");
			}
			else if ( i == 3 ) {
				if ( result[i].indexOf("S") != -1 ) {
					result[i] = "-" + result[i];
				}
				result[i] = result[i].replaceAll("[NS]","");
			}
			result[i] = Float.toString(Float.parseFloat(result[i]));
			tempString += result[i] + "\t";
		}
		tempString = tempString.trim();
		tempString = line.getCruiseID().trim() + "\t" + line.getID().trim() + "\t" + currentTime/1000.0 + "\t" + currentCDP + "\t" + tempString;
		StringSelection ss = new StringSelection(tempString + "\n");
		c.setContents(ss, ss);
	}
//	***** GMA 1.6.2

	public static BufferedImage getImage(XMLine line) throws IOException {
		if( line.getZRange()==null ) throw new IOException(" no data for "+line.getID());
		ScalableImage image = null;
		System.gc();

		int width = 100;
		int height = 50;
		URL url;

		DataInputStream in = null;
		// Based on the selection the images will have different paths
		if (haxby.db.xmcs.XMCS.mcsDataSelect[1].isSelected()) {
			url = URLFactory.url( USGS_MULTI_CHANNEL_PATH+ line.getCruiseID().trim() + "/img/" +
					line.getCruiseID().trim() +"-"+ line.getID().trim() + ".r2.gz" );
		} else if (haxby.db.xmcs.XMCS.mcsDataSelect[2].isSelected()) {
			url = URLFactory.url( USGS_SINGLE_CHANNEL_PATH + line.getCruiseID().trim() + "/img/" +
					line.getCruiseID().trim() +"-"+ line.getID().trim() + ".r2.gz" );
		} else{
			url = URLFactory.url(MULTI_CHANNEL_PATH + line.getCruiseID().trim() + "/img/" +
					line.getCruiseID().trim() +"-"+ line.getID().trim() + ".r2.gz" );
		}

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

		try {
			in.readFully(bitmap);
		} catch (IOException ex) {
		}
		image = new R2(bitmap, width, height);
		try {if(in != null) 
			in.close();
		} catch( Exception ex1 ) {
		}
		return image.getImage();
	}

	public Double getZoomValueX() {
		Double zvx = getZoomX();
		return zvx;
	}
	public Double getZoomValueY() {
		Double zvy = getZoomY();
		return zvy;
	}
	public Double getZoomValue() {
		return null;
	}
	public void zoomSpeed(Point p, Double d) {
	}

	/**
	 * loads the previously saved digitized reflector from a file, replicates the functionality of the method in SCSImage2 - Donald Pomeroy
	 * @param cruiseLine
	 * @param load
	 * @throws IOException
	 */
	public void setLine(XMLine cruiseLine, boolean load) throws IOException {
		int ok;
		if(cruiseLine == null) {
			JOptionPane.showMessageDialog(MapApp.anchor, "Select a line");
			return;
		}
		if ( load ){
			JFileChooser chooser = MapApp.getFileChooser();
			chooser.setMultiSelectionEnabled( true );
			ok = chooser.showOpenDialog( getTopLevelAncestor() );
			if( ok==chooser.APPROVE_OPTION ) {
				dig.reset();
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
							
							try {
								//get zoom levels and scroll point from file and set the view to those values
								int scrollX = (int) Double.parseDouble(st.nextToken());
								int scrollY = (int) Double.parseDouble(st.nextToken());
								
								xAvg = Integer.parseInt(st.nextToken());
								xRep = Integer.parseInt(st.nextToken());
								yAvg = Integer.parseInt(st.nextToken());
								yRep = Integer.parseInt(st.nextToken());

								boolean loadFlip = Boolean.parseBoolean(st.nextToken());
								if (loadFlip != flip) flipTB.doClick();
								scrollPoint = new Point(scrollX, scrollY);
								if(scroller != null) {
									invalidate();
									synchronized(getTreeLock()) {
										scroller.validate();
									}
									scroller.scrollTo(scrollPoint);
									panel.repaint();
								}


							} catch(Exception e) {
								System.out.println(e.getMessage());
							}
							
							if( st.hasMoreTokens() ) {
								AnnotationObject obj = new AnnotationObject(
										this, dig);
								obj.setAnnotation( st.nextToken() );
								line = (LineSegmentsObject) obj;
							} else {
								line = new LineSegmentsObject(
										this, dig);
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
								//line.setName( type.name );
							}
							line.setColor( type.color );
							line.setStroke( type.stroke );
							Vector points = new Vector();
							for( i=0 ; i<n ; i++) {
								st = new StringTokenizer( in.readLine());
								st.nextToken();
								st.nextToken();
								double x =  Double.parseDouble(st.nextToken());

								double resultPy = undoTimeAt(1000*x);
								//System.out.println("X :"+x);
								double y = Double.parseDouble(st.nextToken());
								double resultPx = undoCdpAt(y);
								Point addPoint = undoReverseProcessPoint(new Point2D.Double(resultPx,resultPy));
								//System.out.println("y :"+ y);
								points.add( new double[] {addPoint.x, addPoint.y, 0.} );
							}
							line.setPoints( points );
							line.setColor(Color.RED);
							line.setShowPoints(true);
							objects.add( line );
							dig.getModel().objectAdded();
						}
						in.close();
						dig.setCurrentObject((DigitizerObject) objects.lastElement());
						dig.setMouseListeners();
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
}
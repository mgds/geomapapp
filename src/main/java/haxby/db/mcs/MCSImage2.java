package haxby.db.mcs;

import haxby.map.*;
import haxby.image.*;
import haxby.util.*;

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

import java.sql.*;
//import com.sun.image.codec.jpeg.*;

public class MCSImage2 extends JComponent implements ActionListener, MouseListener, MouseMotionListener, KeyListener, Zoomable {
	MCSLine line;
	MCSSave mcsSave;
	MCSBorder border;
	MCSScale mcsScale;
	MCSImage2 crossLine;
	JCheckBoxMenuItem scaleMI;
	JCheckBoxMenuItem crossMI;
	JMenuItem saveMI;
	JPopupMenu popM;
	Scroller scroller;
	ScalableImage image;
	int width, height;
//	JFrame imageFrame;
	JPanel panel = null;

	double[] cdpInterval;
	double[] tRange;
	int xRep, yRep, xAvg, yAvg;
	boolean rev=false;
	boolean flip=false;
	Zoomer zoomer;

	public MCSImage2() {
		border = null;
		image = null;
		crossLine = null;
		cdpInterval = null;
		tRange = null;
		scroller = null;
		width = 100;
		height = 50;
		xRep = yRep = xAvg = yAvg = 1;
		panel = new JPanel(new BorderLayout());
		panel.add(getToolBar(), "West");
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
		mcsSave = new MCSSave(this);
		mcsScale = new MCSScale( this);
		popM = new JPopupMenu();
		scaleMI = new JCheckBoxMenuItem("show scales", true);
		popM.add( scaleMI );
		crossMI = new JCheckBoxMenuItem("show crossing lines", true);
		popM.add( crossMI );
		saveMI = new JMenuItem( "save" );
		popM.add( saveMI );
		crossMI.addActionListener(this);
		saveMI.addActionListener(this);
		scaleMI.addActionListener(this);
	}
	public void loadImage( MCSLine line ) throws IOException {
		this.line = line;
		border = new MCSBorder(this);
	//	if( scroller==null ) {
	//		JScrollPane sp = new JScrollPane(this);
	//		scroller = new Scroller(sp, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
	//		panel.add(sp,"Center");
	//		Zoomer zoomer = new Zoomer(this);
	//		addMouseListener(zoomer);
	//		addKeyListener(zoomer);
	//	}
	//	if(image==null) {
			xRep = yRep = 1;
			xAvg = yAvg = 8;
	//	}
		image = null;
		System.gc();
		width = 100;
		height = 50;
		invalidate();
		DataInputStream in = null;
		this.line = line;
		border.setTitle();
		URL url = URLFactory.url(haxby.map.MapApp.TEMP_BASE_URL + "cgi-bin"+
			"/MCS/get_image?" + line.getCruiseID().trim() 
			+"+"+ line.getID().trim());
		BufferedReader bin = new BufferedReader(
				new InputStreamReader( url.openStream() ));
		String s = bin.readLine();
		bin.close();
		StringTokenizer st = new StringTokenizer(s);
		cdpInterval = new double[2];
		tRange = new double[2];
		cdpInterval[0] = Double.parseDouble(st.nextToken());
		cdpInterval[1] = Double.parseDouble(st.nextToken());
		double alt = cdpInterval[0] + (cdpInterval[1]-cdpInterval[0])*2;
		double cdp2 = (double)line.cdp[ line.cdp.length-1 ].number();
		if( Math.abs(cdp2-cdpInterval[1]) > Math.abs(cdp2-alt) ) cdpInterval[1]=alt;
		tRange[0] = Double.parseDouble(st.nextToken());
		tRange[1] = Double.parseDouble(st.nextToken());
		String imageURL = st.nextToken();
		url = URLFactory.url(imageURL);
		InputStream urlIn = url.openStream();
		if(imageURL.endsWith(".gz")) {
			in = new DataInputStream(
				new GZIPInputStream(
				new BufferedInputStream(urlIn)));
		} else {
			in = new DataInputStream(
				new BufferedInputStream(urlIn));
		}
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
		try {
			if(in != null) in.close();
		} catch( Exception ex1 ) {
		}
	//	rev = false;
		if(scroller != null) {
			invalidate();
			synchronized(getTreeLock()) {
				scroller.validate();
			}
			scroller.scrollTo(new Point(0, 0));
			panel.repaint();
		}
	//	synchronized( line.map.getTreeLock() ) {
	//		Graphics2D g = line.map.getGraphics2D();
	//		line.getCruise().mcs.draw(g);
	//	}
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
	public MCSLine getLine() {
		return line;
	}
	public double getXScale() {
		double scale = line.getCDPSpacing() * (cdpInterval[1]-cdpInterval[0]) /
				(double) (width * xRep/xAvg);
	//	System.out.println( line.getCDPSpacing() + "\t"+ scale);
		return scale;
	}
	public double getYScale() {
		return (tRange[1]-tRange[0]) / (double)(height*yRep/yAvg);
	}
	public void setImageScales( double cdp1, double cdp2, double topMillis, double bottomMillis) {
		cdpInterval = new double[] { cdp1, cdp2 };
		tRange = new double[] { topMillis, bottomMillis };
		border = new MCSBorder(this);
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
		if(border != null) {
			Dimension size = getPreferredSize();
			Rectangle bounds = new Rectangle(0, 0, size.width, size.height);
			if(rect.contains(bounds)) {
				rect=bounds;
				g.clipRect(rect.x, rect.y, rect.width, rect.height);
			}
			Insets ins = border.getBorderInsets(this);
			border.paintBorder(this, g, rect.x, rect.y, rect.width, rect.height);
			int[] seg = getVisibleSeg();
			if( crossMI.isSelected() && seg[1]>seg[0]) {
			//	GeneralPath path = new GeneralPath();
			//	path.moveTo(0f, 0f);
			//	path.lineTo( 4f, -10f );
			//	path.lineTo( -4f, -10f );
			//	path.closePath();
				g.setFont( new Font("SansSerif", Font.BOLD, 10));
				FontMetrics fm = g.getFontMetrics();
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
					MCSCrossing crs = (MCSCrossing)line.crossings.get(k);
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
			}
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
		g.translate( rect.x, rect.y );
		mcsScale.paint( g );
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
			if( crossMI.isSelected() && seg[1]>seg[0]) {
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
					MCSCrossing crs = (MCSCrossing)line.crossings.get(k);
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
		mcsScale.paint( g2 );
		//JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
		//encoder.encode(im);
		ImageIO.write(im, "JPEG", out);
		out.flush();
		out.close();
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
		popM.show( this, p.x, p.y );
	}
	public void mouseClicked(MouseEvent e) {
		String osName = System.getProperty("os.name");
		if ( osName.startsWith("Mac OS") ) {
			if ( e.isShiftDown() && !e.isControlDown() ) {
				popup( e.getPoint() );
			}
		}
		else if ( e.isPopupTrigger() ) {
			popup(e.getPoint() );
		}
	}
	public void mousePressed(MouseEvent e) {
		String osName = System.getProperty("os.name");
		if ( osName.startsWith("Mac OS") ) {
			if ( e.isShiftDown() && !e.isControlDown() ) {
				popup( e.getPoint() );
			}
		}
		else if ( e.isPopupTrigger() ) {
			popup(e.getPoint() );
		}
	}
	public void mouseReleased(MouseEvent e) {
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
		if( line==null) return;
		line.drawCDP( -1 );
	}
	public void mouseMoved(MouseEvent e) {
		if( image==null || line==null )return;
		line.drawCDP( cdpAt( e.getX() ) );
	}
	public void mouseDragged(MouseEvent e) {
	}
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if(image==null) {
			if(cmd.equals("RevVid")) {
				rev = !rev;
			} else if( cmd.equals("flip")) {
				flip = !flip;
			}
			return;
		}
		if(cmd.equals("RevVid")) {
			revVid();
		} else if( cmd.equals("wider")) {
			wider();
		} else if( cmd.equals("narrower")) {
			narrower();
		} else if( cmd.equals("flip")) {
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
			save();
		} else if( cmd.equals("show scales")) {
			if( scaleMI.isSelected() ) {
				mcsScale.setEnabled( this );
			} else {
				mcsScale.setEnabled( null );
			}
		} else if( cmd.equals("show crossing lines")) {
			repaint();
		}
	}
	void save() {
		if(line==null) return;
		mcsSave.save();
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
	//	javax.swing.border.Border border = BorderFactory.createRaisedBevelBorder();
	//	javax.swing.border.Border border = BorderFactory.createLineBorder(Color.black);
		haxby.util.SimpleBorder border = new haxby.util.SimpleBorder();
		toolBar = new JToolBar(JToolBar.VERTICAL);
		toolBar.setFloatable( false );
		JButton b = new JButton(Buttons.WIDER(false));
		b.setPressedIcon( Buttons.WIDER(true) );
		b.setBorder(border);
		b.addActionListener(this);
		toolBar.add(b);
		b.setActionCommand("wider");
		b = new JButton(Buttons.NARROWER(false));
		b.setPressedIcon( Buttons.NARROWER(true) );
		b.setBorder(border);
		b.addActionListener(this);
		toolBar.add(b);
		b.setActionCommand("narrower");

		JToggleButton tb = new JToggleButton(Buttons.POSITIVE());
		tb.setSelectedIcon(Buttons.NEGATIVE());
		tb.setBorder(border);
		tb.addActionListener(this);
		toolBar.add(tb);
		tb.setActionCommand("RevVid");

		tb = new JToggleButton(Buttons.NORMAL());
		tb.setSelectedIcon(Buttons.REVERSE());
		tb.setBorder(border);
		tb.addActionListener(this);
		toolBar.add(tb);
		tb.setActionCommand("flip");

		b = new JButton(Buttons.SAVE(false));
		b.setPressedIcon( Buttons.SAVE(true) );
		b.setBorder(border);
		b.addActionListener(this);
		toolBar.add(b);
		b.setToolTipText("Save/Download");
		b.setActionCommand("save");
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
	
	public void keyPressed(KeyEvent arg0) {
	}
	public void keyReleased(KeyEvent e) {
	}
	public void keyTyped(KeyEvent e) {
	}
	public Double getZoomValue() {
		return null;
	}
	public void zoomSpeed(Point p, Double d) {
	}
}

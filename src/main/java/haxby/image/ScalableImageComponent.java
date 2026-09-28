package haxby.image;

import haxby.map.*;
import haxby.proj.*;
import haxby.util.*;

import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;

public class ScalableImageComponent 
		extends JComponent
		implements ActionListener,
				Zoomable,
				Overlay {
	XMap map = null;
	GeneralPath nav = null;
	ScalableImage image;
	Scroller scroller;
	int width, height;
	int xRep, yRep, xAvg, yAvg;
	boolean rev=false;
	boolean flip=false;
	Zoomer zoomer;
	JFileChooser chooser;
	JFrame frame;
	float[] dep=null;
	JPanel panel;

	public ScalableImageComponent() {
		chooser = new JFileChooser( System.getProperty("user.dir") );
		chooser.setSelectedFile( new File("*.r2.gz") );
		int ok = chooser.showOpenDialog(null);
		if( ok==chooser.CANCEL_OPTION ) System.exit(0);
		File file = chooser.getSelectedFile();
		try {
			loadImage( file );
		} catch( IOException ex ) {
			ex.printStackTrace();
			System.exit(0);
		}
		panel = new JPanel( new BorderLayout() );
		JScrollPane sp = new JScrollPane(this);
		scroller = new Scroller(sp, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		frame = new JFrame( file.getName() );
		frame.setDefaultCloseOperation( frame.EXIT_ON_CLOSE );
		panel.add( sp, "Center" );
		panel.add( getToolBar(), "West");
		frame.getContentPane().add( panel, "Center" );

		zoomer = new Zoomer(this);
		addMouseListener(zoomer);
		addMouseMotionListener(zoomer);
		addKeyListener(zoomer);

		JMenuBar bar = new JMenuBar();
		JMenu fileM = new JMenu("File");
		bar.add(fileM);
		JMenuItem item = new JMenuItem("open");
		item.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_O, 0 ) );
		fileM.add(item);
		item.addActionListener( this );
		frame.setJMenuBar( bar );

		frame.pack();
		frame.show();
	}
	void loadImage(File file) throws IOException {
		BufferedInputStream in = new BufferedInputStream(
				new FileInputStream( file ));
		image = new R2( in, file.getName().endsWith(".gz") );
		if( frame!=null )frame.setTitle( file.getName() );
		File dir = file.getParentFile();
		String name = file.getName();
		int k = name.indexOf(".r2.gz");
		dep=null;
		nav=null;
		if( k>0 ) {
			name = name.substring(0, k) + ".nav";
			File f = new File( dir, name);
			if( f.exists() ) {
				DataInputStream input = new DataInputStream(
					new BufferedInputStream(
					new FileInputStream( f )));
				int n = input.readInt();
				int top = input.readInt();
				int bottom = input.readInt();
				float scale = 800f / (float)(bottom-top);
				dep = new float[n];
				if( map==null ) {
					for(int i=0 ; i<n ; i++) {
						input.readInt();
						input.readInt();
						input.readInt();
						dep[i] = -(float)input.readInt() / 100f;
						dep[i] -= (float)top;
						dep[i] *= scale;
					}
				} else {
					Projection proj = map.getProjection();
					Point2D.Double point = new Point2D.Double();
					nav = new GeneralPath();
					for(int i=0 ; i<n ; i++) {
						input.readInt();
						point.x = (double)input.readInt();
						point.y = (double)input.readInt();
						point = (Point2D.Double)proj.getMapXY( point );
						if( i==0 ) nav.moveTo( (float)point.x, (float)point.y );
						else nav.lineTo( (float)point.x, (float)point.y );
						dep[i] = -(float)input.readInt() / 100f;
						dep[i] -= (float)top;
						dep[i] *= scale;
					}
				}
				input.close();
			}
		}

		xRep = yRep = xAvg = yAvg = 1;
		BufferedImage im = image.getImage();
		width = im.getWidth();
		height = im.getHeight();
		image.setRevVid( rev );
		image.setFlip(flip);
		if(scroller != null) {
			invalidate();
			synchronized(getTreeLock()) {
				scroller.validate();
			}
			scroller.scrollTo(new Point(0, 0));
		}
		repaint();
	}
	public Dimension getPreferredSize() {
		if(image==null) return new Dimension( 1000, 200 );
		Dimension size = new Dimension( width*xRep/xAvg, height*yRep/yAvg);
		return size;
	}
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		Rectangle rect = getVisibleRect();
		Dimension dim = getPreferredSize();
		if( rect.width > dim.width ) rect.width = dim.width;
		if( rect.height > dim.height ) rect.height = dim.height;
		
		if(!image.isFlip() && getZoomX()==1 && getZoomY()==1) {
			g2.drawImage(image.getImage(), 0, 0, this);
		} else {
			rect = image.getImageableRect(g2.getClipBounds(), xAvg, yAvg, xRep, yRep);
			if(rect.width >0 && rect.height>0 ) {
				BufferedImage im = image.getScaledImage(rect, xAvg, yAvg, xRep, yRep);
				g2.drawImage( im, rect.x, rect.y, this);
			}
		}
		if( dep!=null ) {
			g2.setColor( Color.red );
			g2.setStroke( new BasicStroke( 1f ));
			GeneralPath path = new GeneralPath();
			float sx = (float)getZoomX();
			float sy = (float)getZoomY();
			boolean start=true;
			path.moveTo( 0f, dep[0]*sy );
			for( int i=1 ; i<dep.length ; i++ ) {
				path.lineTo( sx*(float)i, dep[i]*sy );
			}
			g2.draw( path );
		}
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
	public void zoomIn(Point p) {
		if( image==null )return;
		Rectangle rect = getVisibleRect();
		double zoomX = getZoomX();
		double zoomY = getZoomY();
		double x = p.x/zoomX;
		double y = p.y/zoomY;
		double w = (double) rect.width;
		double h = (double) rect.height;
		if(xAvg==1) xRep*=2;
		else xAvg /=2;
		if(yAvg==1) yRep*=2;
		else yAvg /=2;
		zoomX = getZoomX();
		zoomY = getZoomY();
		int newX = (int) (x*zoomX - rect.getWidth()*.5d);
		int newY = (int) (y*zoomY - rect.getHeight()*.5d);
		invalidate();
		synchronized(this) {
			scroller.validate();
		}
		scroller.scrollTo(new Point(newX, newY));
		repaint();
	}
	public void zoomOut(Point p) {
		if( image==null )return;
		Rectangle rect = getVisibleRect();
		double zoomX = getZoomX();
		double zoomY = getZoomY();
		double x = p.x/zoomX;
		double y = p.y/zoomY;
		double w = (double) rect.width;
		double h = (double) rect.height;
		if(xRep==1) xAvg*=2;
		else xRep /=2;
		if(yRep==1) yAvg*=2;
		else yRep /=2;
		zoomX = getZoomX();
		zoomY = getZoomY();
		int newX = (int) (x*zoomX - rect.getWidth()*.5d);
		int newY = (int) (y*zoomY - rect.getHeight()*.5d);
		invalidate();
		synchronized(this) {
			scroller.validate();
		}
		scroller.scrollTo(new Point(newX, newY));
		repaint();
	}
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if(cmd.equals("RevVid")) {
			revVid();
		} else if( cmd.equals("wider")) {
			wider();
		} else if( cmd.equals("narrower")) {
			narrower();
		} else if( cmd.equals("open") ) {
			int ok = chooser.showOpenDialog(frame);
			if( ok==chooser.CANCEL_OPTION ) return;
			File file = chooser.getSelectedFile();
			try {
				loadImage( file );
			} catch( IOException ex ) {
				ex.printStackTrace();
				System.exit(0);
			}
			invalidate();
			synchronized(this) {
				scroller.validate();
			}
			repaint();
		} else if( cmd.equals("flip")) {
			image.setFlip(!image.isFlip());
			flip = image.isFlip();
			double zoomX = getZoomX();
			double zoomY = getZoomY();
			Rectangle rect = getVisibleRect();
			Rectangle r1 = getBounds();
			if(rect.contains(r1)) rect=r1;
			Point p = new Point();
			p.x = rect.width/2+rect.x;
			p.y = rect.height/2+rect.y;
			double x = width - p.getX() / zoomX;
			double y = p.getY() / zoomY;
			double w = (double) rect.width;
			double h = (double) rect.height;
			invalidate();
			int newX = (int) (x*zoomX - w*.5d);
			int newY = (int) (y*zoomY - h*.5d);
			synchronized(this) {
				scroller.validate();
			}
			scroller.scrollTo(new Point(newX, newY));
			repaint();
		}
	}
	public void narrower() {
		double zoomX = getZoomX();
		double zoomY = getZoomY();
		Rectangle rect = getVisibleRect();
		Rectangle r1 = getBounds();
		if(rect.contains(r1)) rect=r1;
		Point p = new Point();
		p.x = rect.width/2+rect.x;
		p.y = rect.height/2+rect.y;
		double x = p.getX() / zoomX;
		double y = p.getY() / zoomY;
		double w = (double) rect.width ;
		double h = (double) rect.height ;
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
		Rectangle rect = getVisibleRect();
		Rectangle r1 = getBounds();
		if(rect.contains(r1)) rect=r1;
		Point p = new Point();
		p.x = rect.width/2+rect.x;
		p.y = rect.height/2+rect.y;
		double x = p.getX() / zoomX;
		double y = p.getY() / zoomY;
		double w = (double) rect.width;
		double h = (double) rect.height;
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
	private void initToolBar() {
		haxby.util.SimpleBorder border = new haxby.util.SimpleBorder();
		toolBar = new JToolBar(JToolBar.VERTICAL);
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

		JToggleButton tb = new JToggleButton(Icons.getIcon(Icons.POSITIVE,false));
		tb.setSelectedIcon(Icons.getIcon(Icons.NEGATIVE,true));
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
		b.setToolTipText("Save Image");
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
	public void draw( Graphics2D g ) {
		if( map==null || nav==null ) return;
		g.setStroke( new BasicStroke( 1f/(float)map.getZoom() ));
		g.setColor( Color.red );
		g.draw( nav );
	}
	public static void main( String[] args ) {
		new ScalableImageComponent();
	}
	public Double getZoomValue() {
		return null;
	}
	public void zoomSpeed(Point p, Double d) {
	}
}

package haxby.db.scs;

import haxby.map.*;
import haxby.db.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.*;
//import com.sun.image.codec.jpeg.*;

public class SCSRegister extends JComponent 
			implements Zoomable,
				ActionListener {
	File file;
	File panelsDir;
	JScrollPane scrollPane;
	BufferedImage image;
	double zoom;
	Vector gaps;
	Vector dates;
	Vector crop;
	JFrame frame;
	JFileChooser chooser;
	SCSLineDigitizer dig;
	SCSTools tools;
	JMenuItem resampleMI;
	boolean resampled = false;
	boolean saved = false;
	Vector panels;
	Comparator compare;

	ViewTZ view;
	XYGraph graph;
	ByteLookupTable lookup;

	public SCSRegister() {
		compare = new Comparator() {
			public int compare(Object o1, Object o2) {
				double[] xx1 = (double[])o1;
				double[] xx2 = (double[])o2;
				if( xx1[0]>xx2[0] ) return 1;
				else if( xx1[0]<xx2[0] ) return -1;
				else return 0;
			}
			public boolean equals(Object obj) {
				return this==obj;
			}
		};
		frame = null;
		dig = new SCSLineDigitizer(this);
		tools = new SCSTools( this, dig);
		panels = null;
		view = new ViewTZ();
		graph = new XYGraph( view, 0 );
		chooser = new JFileChooser( System.getProperty("user.dir") );
		try {
			open();
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(0);
		}
/*
		chooser.setSelectedFile(new File("*.ras"));
		int ok = chooser.showOpenDialog(null);
		if( ok==chooser.CANCEL_OPTION ) System.exit(0);
		file = chooser.getSelectedFile();
		try {
			BufferedInputStream in = new BufferedInputStream(
				new FileInputStream( file ));
			image = decodeAsBufferedImage(in);
*/
		init();
	}
	void init() {
		JMenuBar bar = new JMenuBar();
		JMenu fileM = new JMenu("File");
		bar.add(fileM);
		JMenuItem item = new JMenuItem("Open");
		item.setAccelerator( KeyStroke .getKeyStroke( KeyEvent.VK_O, 0 ));
		fileM.add( item );
		item.addActionListener( this );
		item = new JMenuItem("Exit");
		fileM.add( item );
		item.addActionListener( this );

		JMenu menu = new JMenu("Process");
		resampleMI = new JMenuItem("Resample");
		resampleMI.setAccelerator( KeyStroke .getKeyStroke( KeyEvent.VK_R, 0 ));
		resampleMI.setEnabled( resampled );
		menu.add( resampleMI );
		resampleMI.addActionListener( this );
		bar.add( menu );
		zoom = .5;
		frame = new JFrame(file.getName());
		frame.setDefaultCloseOperation( frame.EXIT_ON_CLOSE);
		scrollPane = new JScrollPane( this );

	//	frame.getContentPane().add( scrollPane, "Center");

		frame.setJMenuBar( bar );

		Zoomer zoomer = new Zoomer(this);
		addMouseListener(zoomer);
		addMouseMotionListener(zoomer);
		addKeyListener(zoomer);
		frame.getContentPane().add( tools.getPanel(), "North");
		if( resampled ) {
			tools.setMode(1);
			resampleMI.setEnabled( false );
		} else {
			tools.setMode(0);
			resampleMI.setEnabled( true );
		}
		tools.buttons[1].doClick();

		Zoomer gz = new Zoomer( graph );
		graph.addMouseMotionListener( gz );
		graph.addMouseListener( gz );
		graph.addKeyListener( gz );
		graph.setScrollableTracksViewportWidth( false );
		graph.setScrollableTracksViewportHeight( false );
		view.setGraph( graph );
		graph.addMouseMotionListener( view );
		JScrollPane sp = new JScrollPane( graph );

		JPanel panel = new JPanel( new BorderLayout() );
		panel.add( sp, "Center" );
		panel.add( view.getLabel(), "North" );

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel, scrollPane );
		split.setOneTouchExpandable( true );

		frame.pack();
		frame.setSize( 1200, 900 );
		split.setDividerLocation( 500 );
		frame.show();
		
		frame.getContentPane().add( split, "Center" );
		
		lookup = null;
		tools.showColorDialog();
	}
	public void setLookup( ByteLookupTable lookup ) {
		this.lookup = lookup;
		repaint();
	}
	public Dimension getPreferredSize() {
		return new Dimension((int)(image.getWidth()*zoom), 
				(int)(image.getHeight()*zoom) );
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
		Dimension dim = getPreferredSize();
		if( getVisibleRect().contains( new Rectangle(0,0,dim.width, dim.height ))) return;
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
		revalidate();
	}
	public Graphics2D getGraphics2D() {
		Graphics2D g2 = (Graphics2D)getGraphics();
		g2.scale(zoom, zoom);
		return g2;
	}
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.scale(zoom, zoom);
		g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		if( lookup != null ) {
			g2.drawImage( image, new LookupOp( lookup, null ), 0, 0 );
		} else {
			g.drawImage( image, 0, 0, this);
		}
		dig.draw( g2 );
	}
	public BufferedImage decodeAsBufferedImage(InputStream input) throws IOException {
		DataInputStream in = new DataInputStream(input);
		if(in.readInt() != 1504078485) throw new IOException("not a sunraster file");
		int w= in.readInt();
		int h = in.readInt();
		BufferedImage im = new BufferedImage( w, h, BufferedImage.TYPE_INT_RGB);
		in.readInt();
		in.readInt();
		in.readInt();
		in.readInt();
		int length = in.readInt();
		for( int k=0 ; k<length ; k++) in.readByte();
		for(int y=0 ; y<h ; y++) {
			for(int x=0 ; x<w ; x++) {
				int i = in.readUnsignedByte();
				im.setRGB(x, y, 0xff000000 | (i<<16) | (i<<8) | i);
			}
		}
		return im;
	}
	void open() throws IOException {
		int ok = chooser.showOpenDialog( frame );
		if( ok==chooser.CANCEL_OPTION ) {
			if( frame==null) System.exit(0);
			else return;
		}
		file = chooser.getSelectedFile();
		File parent = file.getParentFile().getParentFile();
		if( ! view.getCruise().equals(parent.getName()) )  {
			FileFilter filter = new FileFilter() {
				public boolean accept(File file) {
					return file.getName().endsWith(".tz");
				}
			};
			File[] files = parent.listFiles( filter );
			if( files!=null && files.length==1 ) {
				view.setTZ( files[0] );
				view.setCruise( parent.getName() );
			} else {
				view.reset();
				view.setCruise( "" );
			}
			graph.setPoints( view, 0);
		}

		panelsDir = new File( file.getParent(), "panels");
		if( !panelsDir.exists() ) panelsDir.mkdir();
		panels = null;
		System.gc();
		String path = file.getPath();
		BufferedInputStream in = new BufferedInputStream(
			new FileInputStream( file ));
		if( path.endsWith("jpg") ) {
			//JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder( in );
			image = ImageIO.read(in);
			//image = decoder.decodeAsBufferedImage();
		} else {
			image = decodeAsBufferedImage(in);
		}
		File ref;
		resampled = false;
		dig.reset();
		if( path.endsWith("jpg") ) {
			String s;
			BufferedReader in1;
			ok = JOptionPane.YES_OPTION;
			if( ok==JOptionPane.YES_OPTION ) {
				resampled = true;
				String name = path.substring(0, path.length()-3) + "gaps";
				ref = new File(name);
				if( ref.exists() ) {
					dig.gaps = new Vector();
					in1 = new BufferedReader(
						new FileReader( ref ));
					while( (s=in1.readLine()) != null ) {
						StringTokenizer st = new StringTokenizer(s);
						double[] xx = new double[] {
							Double.parseDouble(st.nextToken()),
							Double.parseDouble(st.nextToken()) };
						dig.gaps.add( xx );
					}
				}
				name = path.substring(0, path.length()-3) + "ts";
				ref = new File(name);
				if( ref.exists() ) {
					dig.timeStamps = new Vector();
					in1 = new BufferedReader(
						new FileReader( ref ));
					while( (s=in1.readLine()) != null ) {
						StringTokenizer st = new StringTokenizer(s);
						double[] xx = new double[] {
							Double.parseDouble(st.nextToken()),
							Double.parseDouble(st.nextToken()),
							st.hasMoreTokens() ?
								Double.parseDouble(st.nextToken())
								: 0. };
						dig.timeStamps.add( xx );
					}
				}
				name = path.substring(0, path.length()-3) + "tt";
				ref = new File(name);
				if( ref.exists() ) {
					dig.twoWayTT = new Vector();
					in1 = new BufferedReader(
						new FileReader( ref ));
					while( (s=in1.readLine()) != null ) {
						StringTokenizer st = new StringTokenizer(s);
						double[] xx = new double[] {
							Double.parseDouble(st.nextToken()),
							Double.parseDouble(st.nextToken()) };
						dig.twoWayTT.add( xx );
					}
				}
				name = path.substring(0, path.length()-3) + "dt";
				ref = new File(name);
				dig.ttShift = new Vector();
				if( ref.exists() ) {
					in1 = new BufferedReader(
						new FileReader( ref ));
					while( (s=in1.readLine()) != null ) {
						StringTokenizer st = new StringTokenizer(s);
						double[] xx = new double[] {
							Double.parseDouble(st.nextToken()),
							Double.parseDouble(st.nextToken()),
							st.hasMoreTokens() ?
								Double.parseDouble(st.nextToken())
								: 0. };
						dig.ttShift.add( xx );
					}
				}
			} else {
				dig.reset();
			}
		} else {
			dig.reset();
		}
		if( frame==null ) return;
		invalidate();
		scrollPane.revalidate();
		frame.setTitle( file.getName() );
		if( resampled ) {
			tools.setMode(1);
			tools.buttons[0].doClick();
			resampleMI.setEnabled( false );
		} else {
			tools.setMode(0);
			tools.buttons[1].doClick();
			resampleMI.setEnabled( true );
		}
		repaint();
	}
	public void actionPerformed( ActionEvent evt ) {
		String cmd = evt.getActionCommand();
		if( cmd.equals("Open") ) {
			while( true ) {
				try {
					open();
					break;
				} catch(IOException ex) {
					JOptionPane.showMessageDialog( frame,
						"An error occurred:\t  "+ex.getMessage());
					continue;
				}
			}
		} else if( cmd.equals("Exit") ) {
			System.exit(0);
		} else if( cmd.equals("Resample") ) {
			setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
			if( dig.segments==null || dig.segments.size()==0 ) return;
			resample( (Vector)dig.segments.get( dig.segments.size()-1 ));
			setCursor( Cursor.getDefaultCursor() );
		} else if( cmd.equals("save") ) {
			if( resampled ) savePanels();
		}
	}
	void showErrorMessage(String s ) {
		JOptionPane.showMessageDialog( getTopLevelAncestor(),
				s, "Error", JOptionPane.ERROR_MESSAGE);
	}
	void savePanels() {
		String name;
		PrintStream out;
		double[] xx;
		Vector gaps = new Vector();
		if( dig.gaps!=null ) {
			for(int k=0 ; k<dig.gaps.size() ; k++) {
				gaps.add( dig.gaps.get(k) );
			}
		}
		if( dig.ttShift!=null && dig.ttShift.size() > 0 ) {
			for( int k=0 ; k<dig.ttShift.size() ; k++ ) {
				xx = (double[])dig.ttShift.get(k);
				gaps.add( new double[] {xx[0], xx[0]} );
			}
		}
		Collections.sort( gaps, compare );
		System.out.println( (gaps.size()-1) +" panels");
		panels = new Vector();
		try {
			if( dig.gaps != null && dig.gaps.size()>0 ) {
				Collections.sort( dig.gaps, compare );
				name = file.getPath().substring(0, file.getPath().length()-3) +"gaps";
				out = new PrintStream(
					new FileOutputStream( name ));
				for( int k=0 ; k<dig.gaps.size() ; k++) {
					xx = (double[])dig.gaps.get(k);
					out.println(xx[0] +"\t"+ xx[1] );
				}
				out.close();
			}
			if( dig.timeStamps!=null && dig.timeStamps.size()>0 ) {
				Collections.sort( dig.timeStamps, compare );
				name = file.getPath().substring(0, file.getPath().length()-3) +"ts";
				out = new PrintStream(
					new FileOutputStream( name ));
				for( int k=0 ; k<dig.timeStamps.size() ; k++) {
					xx = (double[])dig.timeStamps.get(k);
					out.println(xx[0] +"\t"+ xx[1] +(xx.length==3 ? "\t"+ xx[2] : "")  );
				}
				out.close();
			}
			if( dig.twoWayTT!=null && dig.twoWayTT.size()>0 ) {
				Collections.sort( dig.twoWayTT, compare );
				name = file.getPath().substring(0, file.getPath().length()-3) +"tt";
				out = new PrintStream(
					new FileOutputStream( name ));
				for( int k=0 ; k<dig.twoWayTT.size() ; k++) {
					xx = (double[])dig.twoWayTT.get(k);
					out.println(xx[0] +"\t"+ xx[1] );
				}
				out.close();
			}
			if( dig.ttShift!=null && dig.ttShift.size()>0 ) {
				Collections.sort( dig.ttShift, compare );
				name = file.getPath().substring(0, file.getPath().length()-3) +"dt";
				out = new PrintStream(
					new FileOutputStream( name ));
				for( int k=0 ; k<dig.ttShift.size() ; k++) {
					xx = (double[])dig.ttShift.get(k);
					out.println(xx[0] +"\t"+ xx[1] + (xx.length==3 ? "\t"+ xx[2] : "") );
				}
				out.close();
			}
		} catch (IOException ex ) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog( getTopLevelAncestor(),
				"\"save\" failed. "+ ex.getMessage() );
		}
		if( dig.twoWayTT==null || dig.twoWayTT.size()!=4 ) {
			showErrorMessage("need 4 travel time points:\n"
					+" 2 to crop (negative travel times)"
					+" and 2 to define scale/offset (tt>=0)");
			panels = null;
			return;
		}
		double[][] yy = new double[2][2];
		double[] yyt;
		int[] yBounds = new int[2];
		int kBounds=0;
		int kk=0;
		for( int k=0 ; k<4 ; k++ ) {
			yyt = (double[])dig.twoWayTT.get(k);
			if( yyt[1]<0. ) yBounds[kBounds++] = (int) yyt[0];
			else yy[kk++] = yyt;
		}
		int top = yBounds[0];
		int bottom = yBounds[1];
		if( top>bottom ) {
			int tmp = top;
			top = bottom;
			bottom = tmp;
		}
		double[] yy1, yy2;
		if( yy[0][0]>yy[1][0] ) {
			yy1 = yy[1];
			yy2 = yy[0];
		} else {
			yy1 = yy[0];
			yy2 = yy[1];
		}
		if( yy2[0]<yy1[0] ) {
			showErrorMessage("travel times must increase dowmward");
			panels = null;
			return;
		}
		double t1 = yy1[1] + (top-yy1[0]) * (yy2[1]-yy1[1]) / (yy2[0]-yy1[0]);
		double t2 = yy1[1] + (bottom-yy1[0]) * (yy2[1]-yy1[1]) / (yy2[0]-yy1[0]);
		System.out.println( top +"\t"+ t1 +"\t"+ bottom +"\t"+ t2 +"\t"+ ((bottom-top)/(t2-t1)));
		double sy = (t2-t1)/(bottom-top);
		if( gaps == null || gaps.size()<2 ) {
			showErrorMessage("there must be at least 2 gaps\n"
					+"(gaps define the panel boundaries");
			panels = null;
			return;
		}
		if( dig.timeStamps == null || dig.timeStamps.size()<2 ) {
			showErrorMessage("there must be at least 1 time stamp in each panel,\n"
					+"and at least one panel must have 2");
			panels = null;
			return;
		}
		double shift = 0.;
		double scale = 1.;
		int ktt = 0;
		double dt = 0.;
		double dx = 0.;
		for( int k=0 ; k<gaps.size()-1 ; k++) {
			int[] bounds = { (int)((double[])gaps.get(k))[1],
					(int)((double[])gaps.get(k+1))[0],
					top, bottom };
			if( bounds[1]<=bounds[0] ) continue;
			if( dig.ttShift!=null && ktt<dig.ttShift.size()) {
				xx = (double[])dig.ttShift.get(ktt);
				if( xx[0]<=bounds[0] ) {
					shift +=xx[1];
					scale /= xx[2];
					ktt++;
				}
			}
			SCSPanel p = new SCSPanel( file.getPath(), bounds );
			p.scale = scale;
			p.tt[0] = scale*(t1+shift);
			p.tt[1] = scale*(t2+shift);
			double[][] times = new double[][]  { {0.,0.}, {0.,0.} };
			kk = 0;
			for( int i=0 ; i<dig.timeStamps.size() ; i++ ) {
				xx = (double[])dig.timeStamps.get(i);
				double[] tt = new double[] { xx[0], xx[1] + xx[2]*3600. };
				if( xx[0]<bounds[0] ) continue;
				if( xx[0]>bounds[1] ) break;
				if( kk==2 ) {
					times[1]=tt;
				} else {
					times[kk++] = tt;
				}
			}
			if( kk==2 ) {
				dt += times[1][1] - times[0][1];
				dx += times[1][0] - times[0][0];
			} else if( kk==0 ) {
				showErrorMessage( "need at least one time stamp per panel" );
				panels = null;
				return;
			}
			panels.add(p);
		}
		if( dt==0 ) {
			showErrorMessage( "need 2 time stamps for at least one panel" );
			panels = null;
			return;
		}
		double dtdx = dt/dx;
		for( int k=0 ; k<gaps.size()-1 ; k++) {
			SCSPanel p = (SCSPanel)panels.get(k);
			kk = 0;
			double[][] times = new double[][]  { {0.,0.}, {0.,0.} };
			for( int i=0 ; i<dig.timeStamps.size() ; i++ ) {
				xx = (double[])dig.timeStamps.get(i);
				if( xx[0]<p.bounds[0] ) continue;
				if( xx[0]>p.bounds[1] ) break;
				double[] tt = new double[] { xx[0], xx[1] + xx[2]*3600. };
				if( kk==2 ) {
					times[1]=tt;
				} else {
					times[kk++] = tt;
				}
			}
			if( kk==2 ) {
				p.time[0] = times[0][1] + (p.bounds[0]-times[0][0])
						* (times[1][1] - times[0][1]) / (times[1][0]-times[0][0]);
				p.time[1] = times[0][1] + (p.bounds[1]-times[0][0])
						* (times[1][1] - times[0][1]) / (times[1][0]-times[0][0]);
			} else {
				p.time[0] = times[0][1] + (p.bounds[0]-times[0][0]) * dtdx;
				p.time[1] = times[0][1] + (p.bounds[1]-times[0][0]) * dtdx;
			}
			t1 = 60.* Math.floor( p.time[0]/60. );
			t2 = 60. * Math.ceil( p.time[1]/60. );
			int width = 1+(int)Math.rint((t2-t1)/30.);
			double dtt = .0075;
			double tt1 = Math.floor(p.tt[0]/dtt) * dtt;
			double tt2 = Math.ceil(p.tt[1]/dtt) * dtt;
			int height = 1+(int)Math.rint( (tt2-tt1)/dtt);
	System.out.println( tt1 +"\t"+ tt2 +"\t"+ t1 +"\t"+ t2 +"\t"+ width );
			p.image = new BufferedImage( width, height, BufferedImage.TYPE_INT_RGB );
			double sx = (p.time[1]-p.time[0])/(p.bounds[1]-p.bounds[0]);
			for( int y=0 ; y<height ; y++) {
				double tmp = p.tt[0] + y*dtt;
				double py = p.bounds[2] + (tmp-p.tt[0])/sy/p.scale;
				int iy = (int)Math.floor(py);
				if( iy<0 || iy>image.getHeight()-2 ) continue;
				double dy = py-iy;
				for( int x=0 ; x<width ; x++ ) {
					tmp = t1 + 30.*x;
					double px = p.bounds[0] + (tmp-p.time[0])/sx;
					int ix = (int)Math.floor(px);
					dx = px-ix;
					if( ix<0 || ix>image.getWidth()-2 ) continue;
//	System.out.println( x +"\t"+ y +"\t"+ ix +"\t"+ iy );
					double c = (1.-dx-dy+dx*dy) * (image.getRGB(ix,iy)&255)
							+ (dx-dx*dy) * (image.getRGB(ix+1,iy)&255)
							+ (dy-dx*dy) * (image.getRGB(ix,iy+1)&255)
							+ (dx*dy) * (image.getRGB(ix+1,iy+1)&255);
					int ic = (int)c;
					ic |= 0xff000000 | (ic<<16) | (ic<<8);
					p.image.setRGB( x, y, ic );
				}
			}
			try {
				name = dateString( t1 ) +".jpg" ;
				BufferedOutputStream bout = new BufferedOutputStream(
						new FileOutputStream( new File(panelsDir, name )));
				//JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(bout);
				//encoder.encode( p.image );
				ImageIO.write(p.image, "JPEG", bout);
				name = dateString( t1 ) +".info";
				out = new PrintStream( new FileOutputStream( new File(panelsDir, name )));
				out.println( width +"\t"+ height +"\t30.\t.0075\t"+ t1 +"\t"+ tt1 );
				out.close();
				System.out.println( "panel "+k +":\t" + 
					((p.time[1]-p.time[0])/(p.bounds[1]-p.bounds[0])) +"\n\t"+
					p.bounds[0] +"\t"+ 
					dateString(p.time[0]) +"\n\t"+ 
					p.bounds[1] +"\t"+
					dateString(p.time[1]));
			} catch(IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	String dateString( double secs ) {
		Calendar cal = dig.cal;
		cal.setTime( new Date( 1000L*(long)secs ) );
		StringBuffer date =  new StringBuffer();
		date.append( cal.get(cal.YEAR)+"_" );
		int m = cal.get(cal.MONTH)+1;
		if( m<10 ) date.append("0");
		date.append( m+"_");
		m = cal.get( cal.DATE );
		if( m<10 ) date.append("0");
		date.append( m+"_");
		m = cal.get( cal.HOUR_OF_DAY );
		if( m<10 ) date.append("0");
		date.append( m+"_");
		m = cal.get( cal.MINUTE );
		if( m<10 ) date.append("0");
		date.append( m+"");
		return date.toString();
	}
	void resample(Vector points) {
		if( points.size()==0 ) return;
	//	Vector points = (Vector)dig.segments.get( dig.segments.size()-1 );
		int n = points.size();
		if( n<=1 )return;
		double[][] xy = new double[n][2];
		Point2D.Double p;
		for( int k=0 ; k<n ; k++ ) {
			p = (Point2D.Double)points.get(k);
			xy[k][0] = p.x;
			xy[k][1] = p.y;
		}
		int w = image.getWidth();
		int h = image.getHeight();
		double[] y0 = new double[w];
		int k=0;
		double yAvg = 0.;
		for( int i=0 ; i<w ; i++ ) {
			while( xy[k+1][0]<i && k<n-2 ) k++;
			y0[i] = xy[k][1] + (xy[k+1][1]-xy[k][1]) * 
					(i-xy[k][0]) / (xy[k+1][0]-xy[k][0]);
			yAvg += y0[i];
		}
		yAvg /= w;
		double xAvg = w*.5;
		double sxy, sx2;
		sxy = sx2 = 0;
		for( int i=0 ; i<w ; i++ ) {
			sxy += (i-xAvg)*(y0[i]-yAvg);
			sx2 += (i-xAvg)*(i-xAvg);
		}
		double slope = Math.atan2( sxy, sx2 );
		double c = Math.cos( slope );
		double s = Math.sin( slope );
		double xx, yy, dx, dy;
		int ix, iy;
		for( k=0 ; k<n ; k++) {
			xx = xAvg + (xy[k][0]-xAvg)*c + (xy[k][1]-yAvg)*s;
			yy = yAvg - (xy[k][0]-xAvg)*s + (xy[k][1]-yAvg)*c;
			xy[k][0] = xx;
			xy[k][1] = yy;
		}
		k=0;
		for( int i=0 ; i<w ; i++ ) {
			while( xy[k+1][0]<i && k<n-2 ) k++;
			y0[i] = xy[k][1] + (xy[k+1][1]-xy[k][1]) * 
					(i-xy[k][0]) / (xy[k+1][0]-xy[k][0]);
		}
		k=0;
		byte[] buf = new byte[w*h];
		for( int x=0 ; x<w ; x++ ) {
			for( int y=0 ; y<h ; y++, k++ ) {
				xx = xAvg + (x-xAvg)*c - (y-yAvg)*s;
				if( xx<0. || x>=w-2. )continue;
				yy = (x-xAvg)*s + (y-yAvg)*c + y0[x] ;
				if( yy<0. || y>=h-2. )continue;
				ix = (int)Math.floor(xx);
				iy = (int)Math.floor(yy);
				if( ix<0 || ix>=w-1 || iy<0 || iy>=h-2) continue;
				dx = xx-ix;
				dy = yy-iy;
				double color = (1.-dx-dy+dx*dy) *(image.getRGB(ix,iy)&255)
					+ (dx-dx*dy) * (image.getRGB(ix+1,iy)&255)
					+ (dy-dx*dy) * (image.getRGB(ix,iy+1)&255)
					+ (dx*dy) * (image.getRGB(ix+1,iy+1)&255);
				buf[k] = (byte)((int)Math.rint(color));
			}
		}
		resampled = true;
		saveRefY(points);
		int rgb;
		k=0;
		for( int x=0 ; x<w ; x++ ) {
			for( int y=0 ; y<h ; y++, k++ ) {
				rgb = 255&(int)buf[k];
				rgb = (255<<24) | (rgb<<16) | (rgb<<8) | rgb;
				image.setRGB( x, y, rgb );
			}
		}
		try {
			BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(file.getPath().substring(0, file.getPath().length()-3) +"jpg"));
			//JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
			//encoder.encode( image );
			ImageIO.write(image, "JPEG", out);
		} catch (IOException ex ) {
			ex.printStackTrace();
		}
			
		if( frame!=null ) {
			dig.reset();
			tools.setMode( 1 );
			resampleMI.setEnabled( false );
			repaint();
		}
	}
	void saveRefY(Vector points) {
		try {
			String name = file.getPath().substring(0, file.getPath().length()-3) +"ref";
			PrintStream out = new PrintStream( 
					new FileOutputStream( name ));
		//	Vector points = (Vector)dig.segments.get( dig.segments.size()-1 );
			int n = points.size();
			if( n<=1 )return;
			double[][] xy = new double[n][2];
			Point2D.Double p;
			for( int k=0 ; k<n ; k++ ) {
				p = (Point2D.Double)points.get(k);
				out.println( p.x +"\t"+ p.y );
			}
			out.close();
		} catch (IOException ex ) {
			JOptionPane.showMessageDialog( getTopLevelAncestor(),
					"save failed: "+ex.getMessage() );
		}
	}
	public static void main(String[] args) {
		new SCSRegister();
	}
	public Double getZoomValue() {
		return null;
	}
	public void zoomSpeed(Point p, Double d) {
	}
}
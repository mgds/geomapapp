package haxby.db.scs;

import haxby.map.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class SCSBreak extends JComponent 
			implements ActionListener,
			Zoomable,
			KeyListener,
			MouseListener,
			MouseMotionListener {
	String fileName;
	JScrollPane scrollPane;
	BufferedImage image;
	JTextField cruise, year, month, day, hour, seconds;
	JTextField rotate;
	JCheckBox rotateCB, secCB, dateCB, tileCB;
	JPanel dialog, rotateD, secD, dateD, tileD;
	JPanel buttons;
	JCheckBox breakCB, pickCB, selectCB;
	double zoom;
	double rotation;
	Vector gaps;
	Vector dates;
	Vector secs;
	JPanel datePanel;
	JPanel datePanel1;
	JPanel secPanel;
	JPanel secPanel1;
	JButton setB, setTime;
	JFrame frame;
	public SCSBreak( String fileName ) {
		this.fileName = fileName;
		 try {
			BufferedInputStream in = new BufferedInputStream(
				new FileInputStream( fileName ));
			image = decodeAsBufferedImage(in);
			String name = fileName.substring( 0, fileName.length()-4) + ".dates";
			if( !(new File(name)).exists() ) {
				gaps = new Vector();
				dates = new Vector();
			} else {
				BufferedReader bin = new BufferedReader(
					new FileReader( name));
				SDate date;
				StringTokenizer st = new StringTokenizer( bin.readLine() );
				int n = Integer.parseInt( st.nextToken() );
				dates = new Vector( n );
				for(int i=0 ; i<n ; i++) {
					st = new StringTokenizer( bin.readLine() );
					int y = Integer.parseInt( st.nextToken() );
					int m = Integer.parseInt( st.nextToken() );
					int d = Integer.parseInt( st.nextToken() );
					int hr = Integer.parseInt( st.nextToken() );
					double x1 = Double.parseDouble( st.nextToken() );
					double y1 = Double.parseDouble( st.nextToken() );
					double x2 = Double.parseDouble( st.nextToken() );
					double y2 = Double.parseDouble( st.nextToken() );
					if( y1>y2 ) {
						date = new SDate(x1, y1, x2, y2);
					}  else {
						date = new SDate(x2, y2, x1, y1);
					}
					date.year = y;
					date.month = m;
					date.day = d;
					date.hour = hr;
					dates.add( date);
				}
				st = new StringTokenizer( bin.readLine() );
				n = Integer.parseInt( st.nextToken() );
				gaps = new Vector( n );
				for(int i=0 ; i<n ; i++) {
					st = new StringTokenizer( bin.readLine() );
					double x1 = Double.parseDouble( st.nextToken() );
					double y1 = Double.parseDouble( st.nextToken() );
					double x2 = Double.parseDouble( st.nextToken() );
					double y2 = Double.parseDouble( st.nextToken() );
					if( y1>y2 ) {
						date = new SDate(x1, y1, x2, y2);
					}  else {
						date = new SDate(x2, y2, x1, y1);
					}
					gaps.add( date);
				}
				Collections.sort( gaps);
				Collections.sort( dates );
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(0);
		}
		secs = new Vector();
		init();
	}
	void init() {
		zoom = .5;
		rotation=0.;
		dialog = new JPanel( new BorderLayout() );

		dateD = new JPanel( new BorderLayout() );
		JPanel panel = new JPanel( new GridLayout(0,1) );
		ButtonGroup gp = new ButtonGroup();
		selectCB = new JCheckBox("select", true);
		breakCB = new JCheckBox("Time gap");
		pickCB = new JCheckBox("hour pick");
		secCB = new JCheckBox("2-way time");
		panel.add(selectCB);
		panel.add(breakCB);
		panel.add(pickCB);
		panel.add(secCB);
		gp.add(selectCB);
		gp.add(breakCB);
		gp.add(pickCB);
		gp.add(secCB);
		JButton saveB = new JButton( "Save" );
		dateD.add( saveB, "South");
		selectCB.addActionListener( this );
		breakCB.addActionListener( this );
		pickCB.addActionListener( this );
		secCB.addActionListener( this );
		saveB.addActionListener( this );
		panel.setBorder( BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder( Color.black ),
				"pick") );
		dateD.add(panel,"North");
		dialog.add( dateD, "North" );
		datePanel = new JPanel(new GridLayout(0,1));
		JLabel label = new JLabel( "year" );
	//	label.setHorizontalAlignment( label.RIGHT );
		datePanel.add( label );
		year = new JTextField("1965");
		datePanel.add( year );
		label = new JLabel( "month" );
	//	label.setHorizontalAlignment( label.RIGHT );
		datePanel.add( label );
		month = new JTextField("7");
		datePanel.add( month );
		label = new JLabel( "day" );
	//	label.setHorizontalAlignment( label.RIGHT );
		datePanel.add( label );
		day = new JTextField("7");
		datePanel.add( day );
		label = new JLabel( "hour" );
	//	label.setHorizontalAlignment( label.RIGHT );
		datePanel.add( label );
		hour = new JTextField("7");
		datePanel.add( hour );
	//	dialog.add( datePanel, "South");
		datePanel1 = new JPanel( new BorderLayout() );
		setB = new JButton( "Set" );
		setB.addActionListener( this);
		datePanel1.add( setB );

		secPanel = new JPanel(new GridLayout(0,1));
		label = new JLabel( "2-way time" );
		secPanel.add( label );
		seconds = new JTextField("0");
		secPanel.add( seconds );
		secPanel1 = new JPanel( new BorderLayout() );
		setTime = new JButton( " Apply ");
		setTime.addActionListener( this );
		secPanel1.add( setTime );

		frame = new JFrame(fileName);
		frame.setDefaultCloseOperation( frame.EXIT_ON_CLOSE);
		scrollPane = new JScrollPane( this );
		frame.getContentPane().add( scrollPane, "Center");
		frame.getContentPane().add( dialog, "West");
		frame.pack();
		frame.show();
		addKeyListener( this );
		addMouseListener(this);
		addMouseMotionListener(this);
		Zoomer zoomer = new Zoomer(this);
		addMouseListener(zoomer);
		addMouseMotionListener(zoomer);
		addKeyListener(zoomer);
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
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.scale(zoom, zoom);
		g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage( image, 0, 0, this);
		g2.setStroke( new BasicStroke(1f/(float)zoom) );
		g.setColor( Color.blue );
		for(int i=0 ; i<dates.size() ; i++) {
			SDate date = (SDate)dates.get(i);
			g2.draw( date.line );
			if( date == selected ) {
				double size = 4./zoom;
				Rectangle2D.Double r = new Rectangle2D.Double(
					date.line.x1-size, date.line.y1-size,
					2.*size, 2.*size);
				g2.draw( r );
				r.x = date.line.x2-size;
				r.y = date.line.y2-size;
				g2.draw( r );
			}
		}
		g.setColor( Color.red.darker() );
		for(int i=0 ; i<gaps.size() ; i++) {
			SDate date = (SDate)gaps.get(i);
			g2.draw( date.line );
			if( date == selected ) {
				double size = 4./zoom;
				Rectangle2D.Double r = new Rectangle2D.Double(
					date.line.x1-size, date.line.y1-size,
					2.*size, 2.*size);
				g2.draw( r );
				r.x = date.line.x2-size;
				r.y = date.line.y2-size;
				g2.draw( r );
			}
		}
		g.setColor( Color.green.darker() );
		double size = 20.;
		if( zoom<1. ) size /= zoom;
		float lw = 1f;
		if( zoom<1. ) lw /= (float)zoom;
		BasicStroke s1 = new BasicStroke( lw );
		BasicStroke s3 = new BasicStroke( 3f*lw );
		for(int i=0 ; i<secs.size() ; i++) {
			STime time = (STime) secs.get(i);
			GeneralPath path = new GeneralPath();
			Line2D.Double line = new Line2D.Double(
						time.p.x-size, time.p.y,
						time.p.x+size, time.p.y);
			path.moveTo( (float)(time.p.x-size/4.), (float)time.p.y );
			path.lineTo( (float)time.p.x, (float)(time.p.y-size/4.) );
			path.lineTo( (float)(time.p.x+size/4.), (float)time.p.y );
			path.lineTo( (float)time.p.x, (float)(time.p.y+size/4.) );
			path.closePath();
			g2.setStroke( s3 );
			g2.setColor( Color.white );
			g2.draw( line );
			g2.draw( path );
			g2.setStroke( s1 );
			g.setColor( Color.green.darker() );
			g2.draw( line );
			g2.fill( path );
		}
		shape = null;
	}
	void addSecPanel() {
		removeDatePanel();
		if( selectedT==null ) return;
		STime d = selectedT;
		seconds.setText( Integer.toString( d.sec ) );
		secPanel1.add(secPanel, "North");
		secPanel1.add(setB, "South");
		dialog.add( secPanel1, "South" );
	//	frame.pack();
		frame.repaint();
	}
	void removeSecPanel() {
		dialog.remove( secPanel1 );
	//	frame.pack();
		frame.repaint();
	}
	void addDatePanel() {
		removeSecPanel();
		if( selected==null ) return;
		SDate d = selected;
		year.setText( Integer.toString( d.year ) );
		month.setText( Integer.toString( d.month ) );
		day.setText( Integer.toString( d.day ) );
		hour.setText( Integer.toString( d.hour ) );
		datePanel1.add(datePanel, "North");
		datePanel1.add(setB, "South");
		dialog.add( datePanel1, "South" );
	//	frame.pack();
		frame.repaint();
	}
	void removeDatePanel() {
		dialog.remove( datePanel1 );
	//	frame.pack();
		frame.repaint();
	}
	public void actionPerformed(ActionEvent evt ) {
		if( evt.getActionCommand().equals("Set") ) {
			if( selected!=null ) {
				try {
					selected.year = Integer.parseInt( year.getText() );
					selected.month = Integer.parseInt( month.getText() );
					selected.day = Integer.parseInt( day.getText() );
					selected.hour = Integer.parseInt( hour.getText() );
				} catch( Exception ex ) {
					JOptionPane.showMessageDialog( frame, "error parsing:\n"
							+ ex.getMessage() );
				}
				return;
			} else if( selectedT!=null ) {
				if( selectedT==null ) return;
				try {
					selectedT.sec = Integer.parseInt( seconds.getText() );
				} catch( Exception ex ) {
					JOptionPane.showMessageDialog( frame, "error parsing:\n"
							+ ex.getMessage() );
				}
				return;
			}
		} else if( !evt.getActionCommand().equals("Save") ) {
			selected = null;
			selectedT = null;
			removeDatePanel();
			removeSecPanel();
			return;
		}
		try {
			String name = fileName.substring( 0, fileName.length()-4) + ".dates";
			PrintStream out = new PrintStream(
				new FileOutputStream( name ));
			out.println( dates.size() +" dates" );
			for( int i=0 ; i<dates.size() ; i++) {
				SDate date = (SDate)dates.get(i);
				out.println( date.year 
					+"\t"+ date.month 
					+"\t"+ date.day
					+"\t"+ date.hour
					+"\t"+ date.line.x1
					+"\t"+ date.line.y1
					+"\t"+ date.line.x2
					+"\t"+ date.line.y2);
			}
			out.println( gaps.size() +" gaps" );
			for( int i=0 ; i<gaps.size() ; i++) {
				SDate date = (SDate)gaps.get(i);
				out.println( date.line.x1
					+"\t"+ date.line.y1
					+"\t"+ date.line.x2
					+"\t"+ date.line.y2);
			}
			out.close();
		} catch(IOException ex ) {
			ex.printStackTrace();
		}
	}
	public void keyPressed(KeyEvent evt) {
	}
	public void keyReleased(KeyEvent evt) {
		if( evt.getKeyCode()==evt.VK_DELETE ) {
			if( selected!=null ) {
				gaps.remove( selected );
				dates.remove( selected );
				selected = null;
			} else if( selectedT!=null ) {
				secs.remove( selectedT );
				selectedT=null;
			}
		}
	}
	public void keyTyped(KeyEvent evt) {
	}
	public void mouseEntered(MouseEvent evt) {
	}
	public void mouseExited(MouseEvent evt) {
	}
	public void mousePressed(MouseEvent evt) {
		if( evt.isControlDown() ) return;
		if( selected == null )return;
		Point2D.Double p = new Point2D.Double(evt.getX()/zoom, evt.getY()/zoom);
		double size = 4./zoom;
		Line2D.Double line = selected.line;
		if( p.x>line.x1-size && p.x<line.x1+size
				&& p.y>line.y1-size && p.y<line.y1+size ) {
			p1 = new Point2D.Double( line.x1, line.y1 );
			editing = 1;
		} else if( p.x>line.x2-size && p.x<line.x2+size
				&& p.y>line.y2-size && p.y<line.y2+size ) {
			p1 = new Point2D.Double( line.x2, line.y2 );
			editing = 2;
	//	} else if( selected.line.ptSegDist(p) < size ) {
	//		editing = 0;
		} else {
			editing = -1;
		}
		when = System.currentTimeMillis();
	}
	public void mouseDragged(MouseEvent evt) {
		if( selected != null && editing>0 ) {
			Point2D.Double p = new Point2D.Double(evt.getX()/zoom, evt.getY()/zoom);
			Line2D.Double line = selected.line;
			if( editing==1 ) {
				line.x1 = p.x;
				line.y1 = p.y;
			} else {
				line.x2 = p.x;
				line.y2 = p.y;
			}
			GeneralPath path = new GeneralPath();
			path.append( line, false );
			double size = 4./zoom;
			Rectangle2D.Double r = new Rectangle2D.Double(
				line.x1-size, line.y1-size,
				2.*size, 2.*size);
			path.append( r, false );
			r = new Rectangle2D.Double(
				line.x2-size, line.y2-size,
				2.*size, 2.*size);
			path.append( r, false );
			drawShape();
			shape = path;
			drawShape();
		}
	}
	public void mouseReleased(MouseEvent evt) {
		if( editing>=0 ) {
			editing = -1;
			if( System.currentTimeMillis()-when <500L ) {
				shape = null;
			} else {
				repaint();
			}
		}
	}
	Point2D.Double p1 = null;
	SDate selected=null;
	STime selectedT=null;
	int editing = -1;
	long when = 0L;
	public void mouseClicked(MouseEvent evt) {
		if( evt.isControlDown() ) return;
		if( selectCB.isSelected() ) {
			Point2D.Double p = new Point2D.Double(evt.getX()/zoom, evt.getY()/zoom);
			selected=null;
			selectedT=null;
			for( int i=0 ; i<dates.size() ; i++ ) {
				SDate date = (SDate)dates.get(i);
				if( date.line.ptSegDist( p ) < 2./zoom ) {
					selected = date;
					addDatePanel();
					repaint();
					return;
				}
			}
			removeDatePanel();
			for( int i=0 ; i<gaps.size() ; i++ ) {
				SDate date = (SDate)gaps.get(i);
				if( date.line.ptSegDist( p ) < 2./zoom ) {
					selected = date;
					repaint();
					return;
				}
			}
			for( int i=0 ; i<secs.size() ; i++ ) {
				STime time = (STime)secs.get(i);
				if( p.y< time.p.y-4./zoom || p.y> time.p.y+4./zoom)continue;
				if( p.x< time.p.x-10./zoom || p.y> time.p.x+10./zoom)continue;
				selectedT=time;
				addSecPanel();
				repaint();
			}
			removeSecPanel();
			repaint();
			return;
		}
		if( secCB.isSelected() ) {
			Point2D.Double p=new Point2D.Double(evt.getX()/zoom, evt.getY()/zoom);
			STime time = new STime( p );
			secs.add( time );
			repaint();
			while( true ) {
				int ok = JOptionPane.showConfirmDialog( getTopLevelAncestor(),
					secPanel, "2-way time pick", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE);
				if( ok!=JOptionPane.OK_OPTION ) {
					secs.remove( time );
					repaint();
					return;
				}
				try {
					time.sec = Integer.parseInt( seconds.getText() );
					return ;
				} catch (Exception ex) {
				}
			}
		}
		if( evt.getClickCount()>1 ) {
			p1=null;
		} else if(p1==null) {
			p1=new Point2D.Double(evt.getX()/zoom, evt.getY()/zoom);
			shape = new Line2D.Double( p1, p1 );
			drawShape();
		} else {
			Point2D.Double p2 = new Point2D.Double(evt.getX()/zoom, evt.getY()/zoom);
			if( p2.y==p1.y) {
				p1 = null;
				return;
			}
			if( pickCB.isSelected() ) {
				Point2D.Double p = new Point2D.Double(evt.getX()/zoom, evt.getY()/zoom);
				SDate date = new SDate( p1.x, p1.y, p.x, p.y );
				dates.add( date );
				repaint();
				while( true ) {
					int ok = JOptionPane.showConfirmDialog( getTopLevelAncestor(),
						datePanel, "date pick", JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.PLAIN_MESSAGE);
					if( ok!=JOptionPane.OK_OPTION ) {
						dates.remove( date );
						repaint();
						break;
					}
					try {
						date.year = Integer.parseInt( year.getText() );
						date.month = Integer.parseInt( month.getText() );
						date.day = Integer.parseInt( day.getText() );
						date.hour = Integer.parseInt( hour.getText() );
						p1 = null;
						return ;
					} catch (Exception ex) {
					}
				}
			} else {
				Point2D.Double p = new Point2D.Double(evt.getX()/zoom, evt.getY()/zoom);
				SDate date = new SDate( p1.x, p1.y, p.x, p.y );
				gaps.add( date );
			}
			p1 = null;
			repaint();
		}
	}
	Shape shape = null;
	public void mouseMoved(MouseEvent evt) {
		if( selectCB.isSelected() ) return;
		Point2D.Double p = new Point2D.Double(evt.getX()/zoom, evt.getY()/zoom);
		if( secCB.isSelected() ) {
			drawShape();
			Line2D.Double line = new Line2D.Double(0., p.y, 
					(double)image.getWidth(), p.y );
			shape = line;
			drawShape();
			return;
		}
		if(p1==null) return;
		drawShape();
		shape = new Line2D.Double( p1, p );
		drawShape();
	}
	void drawShape() {
		if( shape==null )return;
		synchronized( getTreeLock() ) {
			Graphics2D g = (Graphics2D)getGraphics();
			g.scale( zoom, zoom );
			g.setStroke( new BasicStroke( 1f/(float)zoom ) );
			g.setXORMode( Color.white );
			g.draw(shape);
		}
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
	class SDate implements Comparable {
		Line2D.Double line;
		int year, month, day, hour;
		public SDate( double x1, double y1, double x2, double y2 ) {
			line = new Line2D.Double(x1, y1, x2, y2);
		}
		public void set( int y, int m, int d, int h) {
			year = y;
			month = m;
			day = d;
			hour = h;
		}
		public int compareTo( Object obj ) {
			try {
				Line2D.Double l = ((SDate)obj).line;
				double dx = Resample1.intercept(line) - Resample1.intercept(l);
				if(dx>0.) return 1;
				else if( dx<0. ) return -1;
				else return 0;
			} catch(ClassCastException ex) {
				return -1;
			}
		}
	}
	class STime implements Comparable {
		Point2D.Double p;
		int sec;
		public STime( Point2D.Double pt ) {
			p = pt;
		}
		public int compareTo( Object obj ) {
			try {
				STime time = (STime) obj;
				double dx = p.x - time.p.x;
				if( dx>0. ) return 1;
				else if( dx<0. ) return -1;
				else return 0;
			} catch(ClassCastException ex) {
				return -1;
			}
		}
	}
	public static void main(String[] args) {
		if( args.length != 1) {
			System.out.println( "usage: java SCSBreak dir");
			System.exit(0);
		}
		new SCSBreak( args[0] );
	}
	public Double getZoomValue() {
		return null;
	}
	public void zoomSpeed(Point p, Double d) {
	}
}
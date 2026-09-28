package haxby.db.scs;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

public class SCSLineDigitizer extends AbstractListModel
					implements KeyListener,
					MouseListener,
					MouseMotionListener {
	SCSRegister register;
	Vector shapes;
	Vector segments;
	Vector modes;
	boolean digitizing = true;
	boolean editing = false;
	Shape shape = null;
	Point2D.Double point1;
	Vector points;
	GeneralPath path;
	int mode;
	JPanel timePanel;
	JPanel twoWayPanel;
	JPanel offsetPanel;
	JTextField tzF, yearF, monthF, dayF, hourF, minF, twoWayF, offsetF, scaleF;
	Vector gaps, timeStamps, twoWayTT, ttShift;
	Calendar cal;

	public SCSLineDigitizer( SCSRegister register ) {
		this.register = register;
		shapes = new Vector();
		segments = new Vector();
		modes = new Vector();
		register.addKeyListener( this );
		timePanel = new JPanel(new GridLayout(0,2) );
		JLabel label = new JLabel("time zone", SwingConstants.RIGHT);
		timePanel.add( label );
		tzF = new JTextField("0");
		timePanel.add( tzF );
		label = new JLabel("year", SwingConstants.RIGHT);
		timePanel.add( label );
		yearF = new JTextField("1966");
		timePanel.add( yearF );
		label = new JLabel("month", SwingConstants.RIGHT);
		timePanel.add( label );
		monthF = new JTextField("7");
		timePanel.add( monthF );
		label = new JLabel("day", SwingConstants.RIGHT);
		timePanel.add( label );
		dayF = new JTextField("1");
		timePanel.add( dayF );
		label = new JLabel("hour(0-23)", SwingConstants.RIGHT);
		timePanel.add( label );
		hourF = new JTextField("0");
		timePanel.add( hourF );
		label = new JLabel("minute", SwingConstants.RIGHT);
		timePanel.add( label );
		minF = new JTextField("0");
		timePanel.add( minF );

		twoWayPanel = new JPanel(new GridLayout(0,2) );
		label = new JLabel("2-way time(secs)", SwingConstants.RIGHT);
		twoWayPanel.add( label );
		twoWayF = new JTextField("0");
		twoWayPanel.add( twoWayF );

		offsetPanel = new JPanel(new GridLayout(0,2) );
		label = new JLabel("2-way scale, left-to-right", SwingConstants.RIGHT);
		offsetPanel.add( label );
		scaleF = new JTextField("1");
		offsetPanel.add( scaleF );
		label = new JLabel("2-way shift, left-to-right", SwingConstants.RIGHT);
		offsetPanel.add( label );
		offsetF = new JTextField("2");
		offsetPanel.add( offsetF );
		gaps = new Vector();
		timeStamps = new Vector();
		twoWayTT = new Vector();
		ttShift = new Vector();

		cal = Calendar.getInstance( TimeZone.getTimeZone("GMT") );
	}
	public void digitize(int mode) {
		this.mode = mode;
		drawShape();
		shape = null;
		points = null;
		if( digitizing ) return;
		register.removeMouseListener( this );
		register.removeMouseMotionListener( this );
		register.addMouseListener( this );
		register.addMouseMotionListener( this );
		digitizing = true;
		editing = false;
	}
	public void edit() {
		drawShape();
		register.removeMouseListener( this );
		register.removeMouseMotionListener( this );
		register.addMouseListener( this );
		register.addMouseMotionListener( this );
		digitizing = false;
		editing = true;
		points = null;
		shape = null;
	}
	public void reset() {
		register.removeMouseListener( this );
		register.removeMouseMotionListener( this );
		digitizing = false;
		points = null;
		editing = false;
		shape = null;
		shapes = new Vector();
		segments = new Vector();
		modes = new Vector();
		gaps = new Vector();
		timeStamps = new Vector();
		twoWayTT = new Vector();
		ttShift = new Vector();
	}
	void redraw() {
		synchronized( register.getTreeLock() ) {
			Graphics2D g = register.getGraphics2D();
			draw( g );
		}
	}
	static Color[] colors = new Color[] {
			Color.red,
			Color.red,
			new Color(200, 0, 200 ),
			Color.green.darker(),
			Color.red,
			Color.blue };
	void draw( Graphics2D g ) {
		double zoom = register.zoom;
		g.setStroke( new BasicStroke( 1f/(float)zoom ) );
		g.setColor( Color.red );
		for( int k=0 ; k<shapes.size() ; k++) {
			int m = ((Integer)modes.get(k)).intValue();
			if( m!=1) continue;
			g.draw( (Shape)shapes.get(k) );
		}
		g.setColor( colors[2] );
		for( int k=0 ; k<gaps.size() ; k++ ) {
			double[] xx = (double[]) gaps.get(k);
			GeneralPath path1 = new GeneralPath();
			float x1 = (float)xx[0];
			float x2 = (float)xx[1];
			float y = (float) register.image.getHeight() -2f;
			path1.moveTo( x1, 1f );
			path1.lineTo( x2, 1f );
			path1.lineTo( x2, y );
			path1.lineTo( x1, y );
			path1.lineTo( x1, 1f );
			path1.lineTo( x2, y );
			path1.moveTo( x1, y );
			path1.lineTo( x2, 1f );
			g.draw( path1 );
		}
		Rectangle r = register.getVisibleRect();
		double left = r.getX() / register.zoom;
		double bottom = (r.getY()+r.getHeight()) / register.zoom;
		g.setColor( colors[3] );
		Font font = new Font("SansSerif", Font.BOLD, 14 );
		font = font.deriveFont( 18f/(float)register.zoom );
		g.setFont( font );
		for( int k=0 ; k<timeStamps.size() ; k++) {
			double[] xx = (double[]) timeStamps.get(k);
			Line2D.Double line = new Line2D.Double( xx[0], 0., 
					xx[0], (double)register.image.getHeight() );
			g.draw( line );
			AffineTransform at = g.getTransform();
			g.translate( xx[0]-2., bottom-100./zoom );
			g.rotate( -Math.PI/2. );
			String date = register.dateString(xx[1]);
			if( xx.length==3 ) {
				date += " ("+ (int)xx[2] +")";
			}
			g.drawString( date, 0, 0 );
			g.setTransform( at);
		}
		g.setColor( colors[4] );
		for( int k=0 ; k<twoWayTT.size() ; k++) {
			double[] xx = (double[]) twoWayTT.get(k);
			if( xx[1]<0 ) g.setColor( Color.cyan.darker() );
			else g.setColor( colors[4] );
			Line2D.Double line = new Line2D.Double( 0., xx[0],
					(double)register.image.getWidth(), xx[0] );
			g.draw( line );
			if( xx[1]>=0. ) {
				String anot = ((int)Math.rint(xx[1])) + " s";
				g.drawString( anot, (int)left+25, (int)(xx[0]-4.) );
			}
		}
		g.setColor( colors[5] );
		for( int k=0 ; k<ttShift.size() ; k++) {
			double[] xx = (double[]) ttShift.get(k);
			Line2D.Double line = new Line2D.Double( xx[0], 0., 
					xx[0], (double)register.image.getHeight() );
			g.draw( line );
		}
		if( path!=null ) g.draw( path );
		shape = null;
	}
	void select(double x, double y) {
		for( int k=0 ; k<timeStamps.size() ; k++) {
			double[] xx = (double[]) timeStamps.get(k);
			if( Math.abs(xx[0]-x) < 2./register.zoom ) {
				Line2D.Double line = new Line2D.Double( xx[0], 0.,
					xx[0], (double)register.image.getHeight() );
				shape = line;
				drawShape();
				boolean done=false;
				do {
					cal.setTime( new Date( 1000L*(long)xx[1] ));
					if( xx.length==3 ) {
						tzF.setText( Integer.toString( (int)xx[2] ));
					} else {
						tzF.setText( "0" );
					}
					yearF.setText( ""+cal.get(cal.YEAR) );
					monthF.setText( ""+(cal.get(cal.MONTH)+1) );
					dayF.setText( ""+cal.get(cal.DATE) );
					hourF.setText( ""+cal.get(cal.HOUR_OF_DAY) );
					minF.setText( ""+cal.get(cal.MINUTE) );
					int ok = JOptionPane.showConfirmDialog( register.getTopLevelAncestor(),
							timePanel, "enter time stamp",
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.PLAIN_MESSAGE);
					if( ok==JOptionPane.CANCEL_OPTION ) {
						timeStamps.remove(k);
						register.repaint();
						points = null;
						return;
					}
					try {
						int year = Integer.parseInt(yearF.getText().trim());
						int month = Integer.parseInt(monthF.getText().trim())-1;
						int day = Integer.parseInt(dayF.getText().trim());
						int hour = Integer.parseInt(hourF.getText().trim());
						int minute = Integer.parseInt(minF.getText().trim());
						cal.clear();
						cal.set( year, month, day, hour, minute);
						done = true;
						xx[1] = .001*(double)cal.getTime().getTime();
						if( xx.length==3 ) {
							xx[2] = Double.parseDouble( tzF.getText() );
						}
					} catch( NumberFormatException ex ) {
						done = false;
					}
				} while( !done );
				register.repaint();
				return;
			}
		}
		for( int k=0 ; k<ttShift.size() ; k++) {
			double[] xx = (double[]) ttShift.get(k);
			if( Math.abs(xx[0]-x) < 2./register.zoom ) {
				Line2D.Double line = new Line2D.Double( xx[0], 0.,
					xx[0], (double)register.image.getHeight() );
				shape = line;
				drawShape();
				boolean done=false;
				offsetF.setText( Integer.toString( (int)xx[1] ));
				if( xx.length==3 ) {
					scaleF.setText( Double.toString(xx[2]) );
				} else {
					scaleF.setText( "1." );
				}
				do {
					int ok = JOptionPane.showConfirmDialog( register.getTopLevelAncestor(),
							offsetPanel, "modify travel-time shift",
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.PLAIN_MESSAGE);
					if( ok==JOptionPane.CANCEL_OPTION ) {
						ttShift.remove(k);
						register.repaint();
						points = null;
						return;
					}
					try {
						int shift = Integer.parseInt(offsetF.getText());
						done = true;
						xx[1] = (double)shift;
						if( xx.length==3 ) xx[2] = Double.parseDouble( scaleF.getText() );
					} catch( NumberFormatException ex ) {
						done = false;
					}
				} while( !done );
				register.repaint();
				return;
			}
		}
		for( int k=0 ; k<twoWayTT.size() ; k++) {
			double[] xx = (double[]) twoWayTT.get(k);
			if( Math.abs(xx[0]-y) < 2./register.zoom ) {
				Line2D.Double line = new Line2D.Double( 0., xx[0],
					(double)register.image.getWidth(), xx[0]);
				shape = line;
				drawShape();
				boolean done=false;
				do {
					twoWayF.setText( ""+((int)xx[1]) );
					int ok = JOptionPane.showConfirmDialog( register.getTopLevelAncestor(),
							twoWayPanel, "enter time stamp",
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.PLAIN_MESSAGE);
					if( ok==JOptionPane.CANCEL_OPTION ) {
						twoWayTT.remove(k);
						register.repaint();
						return;
					}
					try {
						int tt = Integer.parseInt( twoWayF.getText() );
						done = true;
						xx[1] = (double)tt;
					} catch( NumberFormatException ex ) {
						done = false;
					}
				} while( !done );
				register.repaint();
				return;
			}
		}
		for( int k=0 ; k<gaps.size() ; k++ ) {
			double[] xx = (double[]) gaps.get(k);
			if( x>xx[0] && x<xx[1] ) {
				int ok = JOptionPane.showConfirmDialog( register.getTopLevelAncestor(),
						"delete time gap?", "delete time gap?",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.PLAIN_MESSAGE);
				if( ok==JOptionPane.YES_OPTION ) {
					gaps.remove( k );
					register.repaint();
					return;
				}
				return;
			}
		}
	}
	void drawShape() {
		if( shape==null )return;
		double zoom = register.zoom;
		synchronized( register.getTreeLock() ) {
			Graphics2D g = (Graphics2D)register.getGraphics();
			g.scale( zoom, zoom );
			g.setStroke( new BasicStroke( 1f/(float)zoom ) );
			g.setXORMode( Color.white );
			g.draw(shape);
		}
	}
	public void mousePressed(MouseEvent evt) {
	}
	public void mouseReleased(MouseEvent evt) {
	}
	public void mouseEntered(MouseEvent evt) {
		mouseMoved( evt );
	}
	public void mouseExited(MouseEvent evt) {
		drawShape();
		shape = null;
	}
	public void mouseClicked( MouseEvent evt ) {
		if( evt.isControlDown() ) return;
		if( editing ) {
			double zoom = register.zoom;
			select(evt.getX()/zoom, evt.getY()/zoom);
			return;
		}
		if( !digitizing ) return;
		double zoom = register.zoom;
		Point2D.Double p = new Point2D.Double(evt.getX()/zoom, evt.getY()/zoom);
		if( points==null ) {
			points = new Vector();
			points.add( p );
			if( mode==1 ) {
				path = new GeneralPath();
				path.moveTo( (float)p.x, (float)p.y );
			} else {
				drawShape();
				if( mode==3 ) {
					Line2D.Double line = new Line2D.Double( p.x, 0., p.x,
						(double)register.image.getHeight());
					shapes.add( line );
					segments.add( points );
					modes.add( new Integer(3) );
					int index = modes.size()-1;
					boolean done=false;
					do {
						int ok = JOptionPane.showConfirmDialog( register.getTopLevelAncestor(),
							timePanel, "enter time stamp",
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.PLAIN_MESSAGE);
						if( ok==JOptionPane.CANCEL_OPTION ) {
						segments.remove( index );
						shapes.remove( index );
						modes.remove( index );
						register.repaint();
						points = null;
						return;
						}
						try {
						int year = Integer.parseInt(yearF.getText().trim());
						int month = Integer.parseInt(monthF.getText().trim())-1;
						int day = Integer.parseInt(dayF.getText().trim());
						int hour = Integer.parseInt(hourF.getText().trim());
						int minute = Integer.parseInt(minF.getText().trim());
						cal.clear();
						cal.set( year, month, day, hour, minute);
						done = true;
						double ts = .001*(double)cal.getTime().getTime();
						double tz = Double.parseDouble( tzF.getText() );
						timeStamps.add( new double[] {p.x, ts, tz} );
						} catch( NumberFormatException ex ) {
						done = false;
						}
					} while( !done );
					register.repaint();
					points = null;
				} else if( mode==5 ) {
					Line2D.Double line = new Line2D.Double( p.x, 0., p.x,
						(double)register.image.getHeight());
					shapes.add( line );
					segments.add( points );
					modes.add( new Integer(5) );
					int index = modes.size()-1;
					boolean done=false;
					do {
						int ok = JOptionPane.showConfirmDialog( register.getTopLevelAncestor(),
							offsetPanel, "enter time shift",
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.PLAIN_MESSAGE);
						if( ok==JOptionPane.CANCEL_OPTION ) {
						segments.remove( index );
						shapes.remove( index );
						modes.remove( index );

						register.repaint();
						points = null;
						return;
						}
						try {
						int shift = Integer.parseInt( offsetF.getText() );
						double scale = Double.parseDouble( scaleF.getText() );
						ttShift.add( new double[] { p.x, (double)shift, scale } );
						done = true;
						} catch( NumberFormatException ex ) {
						done = false;
						}
					} while( !done );
					register.repaint();
					points = null;
				} else if( mode==4 ) {
					Line2D.Double line = new Line2D.Double( 0., p.y,
						(double)register.image.getWidth(), p.y);
					shapes.add( line );
					segments.add( points );
					modes.add( new Integer(4) );
					int index = modes.size()-1;
					boolean done=false;
					do {
						int ok = JOptionPane.showConfirmDialog( register.getTopLevelAncestor(),
							twoWayPanel, "enter time stamp",
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.PLAIN_MESSAGE);
						if( ok==JOptionPane.CANCEL_OPTION ) {
						segments.remove( index );
						shapes.remove( index );
						modes.remove( index );
						register.repaint();
						points = null;
						return;
						}
						try {
						int tt = Integer.parseInt( twoWayF.getText() );
						done = true;
						twoWayTT.add( new double[] { p.y, (double)tt } );
						} catch( NumberFormatException ex ) {
						done = false;
						}
					} while( !done );
					register.repaint();
					points = null;
				}
			}
			shape = null;
			return;
		}
		drawShape();
		points.add( p );
		if( mode==1 ) {
			path.lineTo( (float)p.x, (float)p.y );
			redraw();
		}
		if( mode==2 ) {
			path = null;
			GeneralPath path1 = new GeneralPath();
			Point2D.Double p1 = (Point2D.Double)points.get( 0 );
			float x1 = (float)Math.min( p.x, p1.x );
			float x2 = (float)Math.max( p.x, p1.x );
			float y = (float) register.image.getHeight() -2f;
			path1.moveTo( x1, 1f );
			path1.lineTo( x2, 1f );
			path1.lineTo( x2, y );
			path1.lineTo( x1, y );
			path1.lineTo( x1, 1f );
			path1.lineTo( x2, y );
			path1.moveTo( x1, y );
			path1.lineTo( x2, 1f );
			gaps.add( new double[] {(double)x1, (double)x2} );
			shapes.add(path1);
			segments.add( points );
			modes.add( new Integer(mode) );
			points=null;
			register.repaint();
		}
		shape = null;
	}
	public void mouseMoved( MouseEvent evt ) {
	//	if( evt.isControlDown() ) return;
		if( !digitizing ) return;
		if( points==null || points.size()==0 ) {
			if( mode==1 )return;
			if( mode==2 || mode==3 || mode==5 ) {
				double x = evt.getX()/register.zoom;
				drawShape();
				shape = new Line2D.Double( x, 0, x, (double)register.image.getHeight() );
				drawShape();
				return;
			} if( mode==4 ) {
				double y = evt.getY()/register.zoom;
				drawShape();
				shape = new Line2D.Double( 0, y, (double)register.image.getWidth(), y );
				drawShape();
				return;
			}
		}
		double zoom = register.zoom;
		drawShape();
		Point2D.Double p1 = (Point2D.Double)points.get( points.size()-1 );
		Point2D.Double p = new Point2D.Double(evt.getX()/zoom, evt.getY()/zoom);
		if( mode==1 ) {
			shape = new Line2D.Double( p1, p );
		} else if( mode==2 ) {
			GeneralPath path1 = new GeneralPath();
			float x1 = (float)Math.min( p.x, p1.x );
			float x2 = (float)Math.max( p.x, p1.x );
			float y = (float) register.image.getHeight() -2f;
			path1.moveTo( x1, 1f );
			path1.lineTo( x2, 1f );
			path1.lineTo( x2, y );
			path1.lineTo( x1, y );
			path1.lineTo( x1, 1f );
			path1.lineTo( x2, y );
			path1.moveTo( x1, y );
			path1.lineTo( x2, 1f );
			shape = path1;
		}
		drawShape();
	}
	public void mouseDragged(MouseEvent evt) {
	}
	public void keyTyped( KeyEvent evt ) {
	}
	public void keyPressed( KeyEvent evt ) {
	}
	public void keyReleased( KeyEvent evt ) {
		if( !digitizing )return;
		if( mode==1 && evt.getKeyCode()== evt.VK_ENTER ) {
			shapes.add( path );
			segments.add( points );
			modes.add( new Integer(mode) );
			path = null;
			points = null;
			register.repaint();
		} else if( evt.getKeyCode()== evt.VK_DELETE) {
			path = null;
			points = null;
			register.repaint();
		} else if( evt.getKeyCode()== evt.VK_V ) {
			register.tools.buttons[0].doClick();
		}
	}
	public int getSize() {
		int n = 0;
		for( int i=0 ; i<modes.size() ; i++ ) {
			if( ((Integer)modes.get(i)).intValue() == mode ) n++;
		}
		return n;
	}
	public Object getElementAt( int index ) {
		int k=0;
		for( int i=0 ; i<modes.size() ; i++ ) {
			if( ((Integer)modes.get(i)).intValue() == mode ) {
				if( k==index ) return modes.get(i);
			} else {
				k++;
			}
		}
		return null;
	}
}
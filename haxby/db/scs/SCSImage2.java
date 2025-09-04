package haxby.db.scs;

import haxby.dig.AnnotationObject;
import haxby.dig.Digitizer;
import haxby.dig.DigitizerObject;
import haxby.dig.LineSegmentsObject;
import haxby.dig.LineType;
import haxby.image.Icons;
import haxby.map.MapApp;
import haxby.map.Zoomable;
import haxby.map.Zoomer;
import haxby.util.URLFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public class SCSImage2 extends haxby.util.ScaledComponent
		implements Zoomable,
				ActionListener, 
				MouseListener, 
				MouseMotionListener {

	Digitizer dig = null;
	Vector panels;
	SCS scs;
	SCSCruise cruise;
	int panel;
	int[] range;
//	SCSBorder border;
//	SCSImage xImage;
//	Scroller scroller;
	JScrollPane scrollPane = null;
	double zoomX, zoomY;
	double zoom;
	static final double SOUND_VELOCITY_WATER = 1500.0;
	static final double SOUND_VELOCITY_SEDIMENT = 1800.0;
	static final double B_COEFF = 350.0;
	int width, height;
	public GeneralPath paths;
	Vector timeDep = null;
	boolean plotDepth = true;
	JCheckBox autoload;
	String digDir;

//	1.3.5: Move zoomer up a level
	Zoomer zoomer;
//	1.3.5: Add zoom-buttons
	Box tools;
	Vector buttons;
	ButtonGroup buttonGroup;
	JToggleButton zoomInTB;
	JToggleButton zoomOutTB;

//	***** GMA 1.6.2: Add variables to record and display data for a user-selected point along a 
//	SCS leg.	
	JPopupMenu pm;
	String tempInfo = null;
//	***** GMA 1.6.2

//	***** GMA 1.6.6: Add dot to display for Macs

	Shape dot = null;
	Shape prevDot = null;
//	***** GMA 1.6.6

	public SCSImage2(SCS scs) {
//		***** GMA 1.6.2: Tool tip text
		setToolTipText("Right-click for menu");
//		***** GMA 1.6.2
		
		this.scs = scs;
		panels = new Vector();
		zoom = zoomX = zoomY = .25;

		addMouseListener(this);
		addMouseMotionListener(this);

//		***** GMA 1.6.2: Add a pop-up menu that appears when a right-click occurs to allow the user 
//		to record the data for a particular point along the selected SCS leg.
		pm = new JPopupMenu();
		JMenuItem mi = new JMenuItem("Copy Information at this Mouse Location to Clipboard");
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
//		***** GMA 1.6.2
		zoomer = new Zoomer(this);

//		***** Changed by A.K.M. 1.3.5 *****
//		Implement zoom buttons
		tools = Box.createHorizontalBox();
		buttonGroup = new ButtonGroup();
		buttons = new Vector();

		JToggleButton no = new JToggleButton();
		buttonGroup.add(no);

		zoomInTB = zoomer.getZoomIn();
		zoomInTB.setActionCommand("Zoom In");
		zoomInTB.addActionListener(this);
		zoomInTB.setSelectedIcon( Icons.getIcon( Icons.ZOOM_IN, true ) );
		//buttons.add( zoomInTB );
		buttonGroup.add( zoomInTB );
		tools.add( zoomInTB );
		zoomInTB.setBorder( null );
		zoomInTB.setToolTipText("Ctrl-click");

		zoomOutTB = zoomer.getZoomOut();
		zoomOutTB.setActionCommand("Zoom Out");
		zoomOutTB.addActionListener(this);
		zoomOutTB.setSelectedIcon( Icons.getIcon( Icons.ZOOM_OUT, true ) );
		//buttons.add( zoomOutTB );
		buttonGroup.add( zoomOutTB );
		tools.add( zoomOutTB );
		zoomOutTB.setBorder( null );
		zoomOutTB.setToolTipText("Shift-Ctrl-click");

//		Add toggler so that zoom buttons can be deselected
		new ToggleToggler( zoomInTB, no );
		new ToggleToggler( zoomOutTB, no );
//		***** Changed by A.K.M. 1.3.5 *****

		addMouseListener(zoomer);
		addMouseMotionListener(zoomer);
		addKeyListener(zoomer);

		JButton b = new JButton(Icons.getIcon(Icons.SAVE,false));
		b.setPressedIcon( Icons.getIcon(Icons.SAVE,true) );
		b.setBorder(null);
		b.addActionListener(this);
		tools.add(b);
		b.setToolTipText("Save Image");
		b.setActionCommand("save");

		autoload = new JCheckBox("auto-load");
		autoload.setSelected( false );
		digDir = null;
		cruise = null;

		range = new int[] {-1, -1};
		height = 1334;
		width = 300;
	}
	public void setDigitizer( Digitizer dig ) {
		this.dig = dig;
		for ( int i = 0; i < dig.getButtons().size(); i++ ) {
			tools.add((JToggleButton)dig.getButtons().get(i));
			((JToggleButton)dig.getButtons().get(i)).addActionListener(this);
		}
		dig.getSelectB().addActionListener(this);
		dig.getSelectB().setSelected(true);
	}
	
//	1.3.5: Returns box "tools" with zoom buttons
	public Box getPanel() {
		return tools;
	}
	
//	1.3.5: setCruise now does not bring up load dialog, instead 
//	is called with boolean load to indicate whether the load code
//	should be run or not
	public void setCruise( SCSCruise cruise, boolean load ) throws IOException {
		//if( cruise==this.cruise ) return;
		dig.reset();
		this.cruise = cruise;
		int ok;
		/*int ok = JOptionPane.showConfirmDialog( getTopLevelAncestor(),
				"Load previously digitized Products?",
				"load?",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.PLAIN_MESSAGE );
		if( ok==JOptionPane.YES_OPTION ) {*/
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
						String name = st.nextToken();
						if( !name.equals( cruise.name ) ) {
							JOptionPane.showMessageDialog(MapApp.anchor, "Line in file does not match selected line: "+name);
							in.close();
							continue;
						}
						in.readLine();
						while( (s = in.readLine()) != null ) {
							st = new StringTokenizer(s, "\t");
							String p = st.nextToken();
							name = st.nextToken();
							int n = Integer.parseInt( st.nextToken() );
							LineSegmentsObject line;
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
								String t = st.nextToken();
								double x = xAtTime( Double.parseDouble(t));
								String nextTokenS = st.nextToken();
								nextTokenS = st.nextToken();
								double y = Double.parseDouble(st.nextToken())/.0075;
								if (!t.equals("0")) 
									points.add( new double[] {x, y, 0.} );
							}
							line.setPoints( points );
							line.setColor(Color.GREEN);
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

		range[1] = -1;
		width = cruise.width;
		zoom = zoomX = zoomY = .25;
		panels = new Vector();
		System.gc();
		
		/* Change RC#### cruises back to C#### for correct path.
		 * Should be fixed on data paths.
		 */
		String cruiseName;
		if(cruise.name.startsWith("RC")){
			cruiseName = cruise.name.replace("RC", "C");
		} else {
			cruiseName = cruise.name;
		}
		String dir = 
			SCS.baseURL + cruiseName.toUpperCase() + "/panels2/";
		
		byte[] buf = new byte[32767];
		for( int k=0 ; k<cruise.start.length ; k++) {
			String file = dir + ((k<100)?"0":"")
					+ ((k<10)?"0":"")
					+ k + ".jpg";
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			BufferedInputStream in = new BufferedInputStream( (URLFactory.url(file)).openStream() );
			int len;
			while( (len=in.read(buf)) != -1 ) {
				out.write( buf, 0, len );
			}
			panels.add( new SCSPanel2( out.toByteArray() ) );
			in.close();
			out.close();
		}

		timeDep = new Vector();
		try {
			/* Change RC#### cruises back to C#### for correct path.
			 * Should be fixed on data paths.
			 */
			if(cruise.name.startsWith("RC")){
				cruiseName = cruise.name.replace("RC", "C");
			} else {
				cruiseName = cruise.name;
			}
			String url = SCS.baseURL + cruiseName.toUpperCase() + "/"+ cruiseName.toUpperCase() +".tza";
			DataInputStream in = new DataInputStream( (URLFactory.url(url)).openStream() );
			while( true ) {
				try {
					int time = in.readInt();
					short dep = in.readShort();
					short age = in.readShort();
					timeDep.add(new TimeDep( time, dep, age) );
				}catch (EOFException ex) {
					break;
				}
			}
			timeDep.trimToSize();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		invalidate();
		scrollPane.validate();
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
		if( panels.size()==0 ) return new Dimension(300, 200);
		Dimension dim = new Dimension(0,0);
		zoomX = zoomY = zoom;
		dim.width = (int) Math.ceil( width*zoomX );
		dim.height = (int) Math.ceil( height*zoomY );
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
		zoomX = zoomY = zoom;
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
	int lastI=0;
	public double timeAtX( double x ) {
		int i=lastI;
		if( i>= cruise.xPosition.length ||
			cruise.xPosition[i] > x ) i=0;
		while( i<cruise.xPosition.length 
				&& cruise.xPosition[i]+cruise.panelSize[i][0] < x ) {
			i++;
		}
		lastI=i;
		if( i==cruise.xPosition.length ) return Double.NaN;
		if( cruise.xPosition[i] > x ) return Double.NaN;
		double time = cruise.start[i] +
				(x-cruise.xPosition[i]) * 30.;
		return time;
	}
	public double xAtTime( double time ) {
		int i=lastI;
		if( i>= cruise.xPosition.length ||
			cruise.start[i] > time ) i=0;
		while( i<cruise.xPosition.length-1
				&& cruise.start[i+1] < time) i++;
		lastI=i;
		return cruise.xPosition[i] + (time - cruise.start[i] )/30.;
	}
	public void save() {
		JFileChooser chooser = MapApp.getFileChooser();
		chooser.setSelectedFile(new File( this.cruise.name + ".txt"));
		int ok = chooser.showSaveDialog( getTopLevelAncestor() );
		if( ok == chooser.CANCEL_OPTION )return;
		File file = chooser.getSelectedFile();
		try {
			PrintStream out = new PrintStream(
					new FileOutputStream( file ));
		out.println( cruise.name +"\t"+ cruise.nav.getStart() +"\t"+ cruise.nav.getEnd() );
			out.println( "cruise_time(unix_secs)\t"
					+ "longitude\t"
					+ "latitude\t"
					+ "digitized_point_twtt(s)\t"
					+ "distance_from_start(km)\t"
					+ "PDR_seafloor_depth(m)\t"
					+ "seafloor_age(Ma)\t"
					+ "digitized_point_depth(m)\t"
					+ "thickness(m)\t"
					+ "backtrack_depth(m)");
			Vector objects = dig.getObjects();
			Vector points;
			double[] xy, lonlat;
			NumberFormat latLonFormat = NumberFormat.getInstance();
			latLonFormat.setMaximumFractionDigits(6);
			latLonFormat.setMinimumFractionDigits(6);
			for( int k=0 ; k<objects.size() ; k++ ) {
				LineSegmentsObject obj = (LineSegmentsObject)objects.get(k);
				points = (Vector)obj.getPoints();
				int badPoints = 0;
				for( int i=0 ; i<points.size() ; i++ ) {
					double x = ((double[])points.get(i))[0];					
					long t = (long)timeAtX( x );
					lonlat = cruise.xyAtTime( t );
					if (Double.isNaN(lonlat[0]) || Double.isNaN(lonlat[1])){
						badPoints++;
					}
				}
				
				int goodPoints = points.size() - badPoints; 
				if( obj instanceof AnnotationObject ) {
					out.println(">\t" + obj.toString() +"\t"+ goodPoints 
							+"\t"+  ((AnnotationObject)obj).getAnnotation() );
				} else {
					out.println(">\t" + obj.toString() +"\t"+ goodPoints );
				}
				if( points.size()==0 ) continue;
			
				NumberFormat fmt = NumberFormat.getInstance();
				for( int i=0 ; i<points.size() ; i++ ) {
					xy = (double[])points.get(i);
					double x = xy[0];
					double y = xy[1];
					
					long t = (long)timeAtX( x );
					lonlat = cruise.xyAtTime( t );
					if (Double.isNaN(lonlat[0]) || Double.isNaN(lonlat[1])) continue;
					double[] newAgeDepth = new double[2];
					newAgeDepth = ageDepthAtTime((int)t);
					double age = newAgeDepth[0];
					double sfDepthms = newAgeDepth[1];
					double sfDepthM = sfDepthms * (SOUND_VELOCITY_WATER / 2000.0);
					double sfDepthSec = sfDepthms / 1000;
					double cursorDepthM = 0.75 * 7.5 * y;
					double cursorDepthSec = 0.0075 * y;
					double sedThicknessM = 0;
					double sedThicknessSec = 0;
					double backTrackedDepthM = 0;

					if (cursorDepthSec > sfDepthSec)	{
						sedThicknessSec = cursorDepthSec - sfDepthSec;
						sedThicknessM = sedThicknessSec * (SOUND_VELOCITY_SEDIMENT / 2.0);
						backTrackedDepthM = cursorDepthM - B_COEFF * Math.sqrt(age) - (sedThicknessM / 2.0);
					}
					
					String xPos = Double.toString(x*0.1852);
					if ( xPos.indexOf(".") != -1 ) {
						xPos = xPos.substring(0,xPos.indexOf("."));
					}
					String line = t +"\t"+ 
								  latLonFormat.format(lonlat[0]) +"\t"+ 
								  latLonFormat.format(lonlat[1]) +"\t"+
								  fmt.format(cursorDepthSec) + "\t" +
								  xPos +"\t";
					if (sfDepthms >=0 ) {
						line += fmt.format((int)sfDepthM).replace(",", "");
					}
					line += "\t";
					
					if (age >= 0) {
						line += fmt.format(age);
					}
					line += "\t" + 
							fmt.format((int)cursorDepthM).replace(",", "") +"\t";
					
					if ( sfDepthms >= 0 && sedThicknessM > 1 )	{
						line += fmt.format((int)sedThicknessM).replace(",", "");
					}
					line += "\t";
					
					if (age >= 0 && sedThicknessM > 10) {
						line += fmt.format((int)backTrackedDepthM).replace(",", "");
					}
					out.println(line);																		
				}
			}
			out.close();
			MapApp.sendLogMessage("Saving_or_Downloading&portal="+scs.getDBName()+"&what=digitized_product&cruise="+this.cruise.name);
		} catch(IOException ex) {
			ex.printStackTrace();
		}
	}

	public void paint(Graphics g) {
		if( panels.size()==0 ) return;
		Rectangle rect = getVisibleRect();
		Graphics2D g2 = (Graphics2D)g;
		g2.scale(zoom, zoom);
		g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.setBackground(Color.white);
		int x1 = (int)Math.rint( rect.x/zoomX );
		int x2 = (int)Math.rint( (rect.x+rect.width) /zoomX );
		int i1 = 0;
		while( i1<cruise.xPosition.length 
				&& cruise.xPosition[i1]+cruise.panelSize[i1][0] < x1 ) {
			((SCSPanel2)panels.get(i1)).image = null;
			i1++;
		}
		if( i1==cruise.xPosition.length ) return;
		int i2 = i1;
		paths = new GeneralPath();
		int t1, t2;
		int tdindex = 0;
		GeneralPath tdPath = new GeneralPath();

		while( i2<cruise.xPosition.length
				&& cruise.xPosition[i2]<x2 ) {
			SCSPanel2 panel = (SCSPanel2)panels.get(i2);
			try {
				panel.decode();
			} catch(IOException ex) {
				ex.printStackTrace();
			} catch( OutOfMemoryError ex ) {
				for( int k=0 ; k<cruise.xPosition.length ; k++ ) {
					((SCSPanel2)panels.get(k)).image=null;
				}
				try {
					panel.decode();
				} catch(IOException e) {
				}
			}
			g.drawImage( panel.image, cruise.xPosition[i2], cruise.top[i2], this );
			while( tdindex+1<timeDep.size() ) {
				if( ((TimeDep)timeDep.get(tdindex+1)).time<cruise.start[i2] ) tdindex++;
				else break;
			}
//	System.out.println( i2 +"\t"+ tdindex +"\t"+ timeDep.size() );
			if( plotDepth && tdindex<timeDep.size() ) {
				TimeDep td = (TimeDep)timeDep.get(tdindex);
				float x = (float) (cruise.xPosition[i2] + 
						(td.time-cruise.start[i2])/30.);
				float y = (float) (td.dep/7.5);
				tdPath.moveTo( x, y);
				tdindex++;
				while( tdindex<timeDep.size() ) {
					td = (TimeDep)timeDep.get(tdindex);
					if( (td.time-cruise.start[i2])/30 > cruise.panelSize[i2][0])break;
					x = (float) (cruise.xPosition[i2] +
						(td.time-cruise.start[i2])/30.);
					y = (float) (td.dep/7.5);
					tdPath.lineTo( x, y);
					tdindex++;
				}
			}
			t1 = cruise.start[i2];
			if( cruise.xPosition[i2]<x1 ) {
				t1 += (int)(30*(x1-cruise.xPosition[i2]));
			}
			if( cruise.xPosition[i2]+cruise.panelSize[i2][0]>x2 ) {
				t2 = cruise.start[i2] + (int)(30*(x2-cruise.xPosition[i2]));
			} else {
				t2 = cruise.start[i2] + 30*cruise.panelSize[i2][0];
			}
			paths.append( cruise.nav.getPath(t1, t2 ), false);
			i2++;
		}
		g2.setStroke( new BasicStroke(1f/(float)zoom) );
		g2.setColor( Color.red );
		if( plotDepth )g2.draw( tdPath );
		for( int i=i2 ; i<cruise.xPosition.length ; i++) {
			((SCSPanel2)panels.get(i)).image = null;
		}
		double pixelsPerSec = zoomY / .0075;
		int sAnot = 1 + (int) (25./pixelsPerSec);
		Line2D.Double line = new Line2D.Double( x1, 0., x1+5/zoom, 0.);
		Line2D.Double line1 = new Line2D.Double( x2, 0., x2-5/zoom, 0.);
		g2.setStroke( new BasicStroke( 1f/(float)zoom ) );
		g2.setColor( Color.blue );
		for( int i=0 ; i<=10 ; i++) {
			line.y1 = i / .0075;
			line.y2 = line.y1;
			line1.y1 = line1.y2 = line.y2;
			g2.draw(line);
			g2.draw(line1);
		}
		g2.setFont( (new Font("SansSerif", Font.PLAIN, 1)).deriveFont( 10f/(float)zoom ) );
		int xAnot = (int) (x1+8/zoom);
		for( int i=0 ; i<=10 ; i+=sAnot) {
			String tt = i + "";
			int y = (int)(i/.0075 + 4./zoom);
			g2.drawString(tt, xAnot, y);
		}
		
		if( dig!=null) {
			g2.scale(1f/zoom, 1f/zoom);
			dig.draw( g2 );
		}
		System.gc();

		synchronized( cruise.map.getTreeLock() ) {
			Graphics2D gg = cruise.map.getGraphics2D();
			AffineTransform xform = gg.getTransform();

//			***** GMA 1.6.6: On Macs, make the lines thicker so that alternate display method can be used
//			gg.setStroke( new BasicStroke(3f/(float)cruise.map.getZoom() ));
			
			String osName = System.getProperty("os.name");
			if ( osName.startsWith("Mac OS") ) {
				gg.setStroke( new BasicStroke(5f/(float)cruise.map.getZoom() ));
			}
			else {
				gg.setStroke( new BasicStroke(3f/(float)cruise.map.getZoom() ));
			}
//			***** GMA 1.6.6

			gg.setColor( Color.darkGray );
			cruise.draw(gg);
			
//			***** GMA 1.6.6: On Macs, make the lines thicker so that alternate display method can be used
//			gg.setStroke( new BasicStroke(1f/(float)cruise.map.getZoom() ));

			if ( osName.startsWith("Mac OS") ) {
				gg.setStroke( new BasicStroke(5f/(float)cruise.map.getZoom() ));
			} else {
				gg.setStroke( new BasicStroke(1f/(float)cruise.map.getZoom() ));
			}
//			***** GMA 1.6.6

		//	gg.setTransform( xform );
			gg.setColor( Color.red );
			gg.draw( paths );
			double wrap = cruise.map.getWrap();
			if( wrap>0. ) {
			//	gg.translate( wrap*cruise.map.getZoom(), 0. );
				gg.translate( wrap, 0. );
				gg.draw( paths );
			}
		}
	}

	public void paintWhole(Graphics g) {
		if( panels.size()==0 ) return;
		Rectangle rect = getBounds();
		Graphics2D g2 = (Graphics2D)g;
		g2.scale(zoom, zoom);
		g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION,
								RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		int x1 = (int)Math.rint( rect.x/zoomX );
		int x2 = (int)Math.rint( (rect.x+rect.width) /zoomX );
		int i1 = 0;
		while( i1<cruise.xPosition.length 
				&& cruise.xPosition[i1]+cruise.panelSize[i1][0] < x1 ) {
			((SCSPanel2)panels.get(i1)).image = null;
			i1++;
		}
		if( i1==cruise.xPosition.length ) return;
		int i2 = i1;
		paths = new GeneralPath();
		int t1, t2;
		int tdindex = 0;
		GeneralPath tdPath = new GeneralPath();

		while( i2<cruise.xPosition.length
				&& cruise.xPosition[i2]<x2 ) {
			SCSPanel2 panel = (SCSPanel2)panels.get(i2);
			try {
				panel.decode();
			} catch(IOException ex) {
				ex.printStackTrace();
			} catch( OutOfMemoryError ex ) {
				for( int k=0 ; k<cruise.xPosition.length ; k++ ) {
					((SCSPanel2)panels.get(k)).image=null;
				}
				try {
					panel.decode();
				} catch(IOException e) {
				}
			}
			g.drawImage( panel.image, cruise.xPosition[i2], cruise.top[i2], this );
			while( tdindex+1<timeDep.size() ) {
				if( ((TimeDep)timeDep.get(tdindex+1)).time<cruise.start[i2] ) tdindex++;
				else break;
			}
//	System.out.println( i2 +"\t"+ tdindex +"\t"+ timeDep.size() );
			if( plotDepth && tdindex<timeDep.size() ) {
				TimeDep td = (TimeDep)timeDep.get(tdindex);
				float x = (float) (cruise.xPosition[i2] + 
						(td.time-cruise.start[i2])/30.);
				float y = (float) (td.dep/7.5);
				tdPath.moveTo( x, y);
				tdindex++;
				while( tdindex<timeDep.size() ) {
					td = (TimeDep)timeDep.get(tdindex);
					if( (td.time-cruise.start[i2])/30 > cruise.panelSize[i2][0])break;
					x = (float) (cruise.xPosition[i2] +
						(td.time-cruise.start[i2])/30.);
					y = (float) (td.dep/7.5);
					tdPath.lineTo( x, y);
					tdindex++;
				}
			}
			t1 = cruise.start[i2];
			if( cruise.xPosition[i2]<x1 ) {
				t1 += (int)(30*(x1-cruise.xPosition[i2]));
			}
			if( cruise.xPosition[i2]+cruise.panelSize[i2][0]>x2 ) {
				t2 = cruise.start[i2] + (int)(30*(x2-cruise.xPosition[i2]));
			} else {
				t2 = cruise.start[i2] + 30*cruise.panelSize[i2][0];
			}
			paths.append( cruise.nav.getPath(t1, t2 ), false);
			i2++;
		}
		g2.setStroke( new BasicStroke(1f/(float)zoom) );
		g2.setColor( Color.red );
		if( plotDepth )g2.draw( tdPath );
		for( int i=i2 ; i<cruise.xPosition.length ; i++) {
			((SCSPanel2)panels.get(i)).image = null;
		}
		double pixelsPerSec = zoomY / .0075;
		int sAnot = 1 + (int) (25./pixelsPerSec);
		Line2D.Double line = new Line2D.Double( x1, 0., x1+5/zoom, 0.);
		Line2D.Double line1 = new Line2D.Double( x2, 0., x2-5/zoom, 0.);
		g2.setStroke( new BasicStroke( 1f/(float)zoom ) );
		g2.setColor( Color.blue );
		for( int i=0 ; i<=10 ; i++) {
			line.y1 = i / .0075;
			line.y2 = line.y1;
			line1.y1 = line1.y2 = line.y2;
			g2.draw(line);
			g2.draw(line1);
		}
		g2.setFont( (new Font("SansSerif", Font.PLAIN, 1)).deriveFont( 10f/(float)zoom ) );
		int xAnot = (int) (x1+8/zoom);
		for( int i=0 ; i<=10 ; i+=sAnot) {
			String tt = i + "";
			int y = (int)(i/.0075 + 4./zoom);
			g2.drawString(tt, xAnot, y);
		}
		if( dig!=null) dig.draw( g2 );
		System.gc();

		synchronized( cruise.map.getTreeLock() ) {
			Graphics2D gg = cruise.map.getGraphics2D();
			AffineTransform xform = gg.getTransform();
			
//			***** GMA 1.6.6: On Macs, make the lines thicker so that alternate display method can be used
//			gg.setStroke( new BasicStroke(3f/(float)cruise.map.getZoom() ));
			String osName = System.getProperty("os.name");
			if ( osName.startsWith("Mac OS") ) {
				gg.setStroke( new BasicStroke(5f/(float)cruise.map.getZoom() ));
			}
			else {
				gg.setStroke( new BasicStroke(3f/(float)cruise.map.getZoom() ));
			}
//			***** GMA 1.6.6

			gg.setColor( Color.darkGray );
			cruise.draw(gg);
			
//			***** GMA 1.6.6: On Macs, make the lines thicker so that alternate display method can be used
//			gg.setStroke( new BasicStroke(1f/(float)cruise.map.getZoom() ));
		
			if ( osName.startsWith("Mac OS") ) {
				gg.setStroke( new BasicStroke(5f/(float)cruise.map.getZoom() ));
			}
			else {
				gg.setStroke( new BasicStroke(1f/(float)cruise.map.getZoom() ));
			}
//			***** GMA 1.6.6
		//	gg.setTransform( xform );
			gg.setColor( Color.red );
			gg.draw( paths );
			double wrap = cruise.map.getWrap();
			if( wrap>0. ) {
			//	gg.translate( wrap*cruise.map.getZoom(), 0. );
				gg.translate( wrap, 0. );
				gg.draw( paths );
			}
		}
	}

	public double[] getScales() {
		return new double[] { zoomX, zoomY };
	}
	public void mousePressed( MouseEvent evt ) {

//		***** GMA 1.6.2: Record the SCS data at the point where the user right-clicks, 
//		and then bring up a pop-up menu giving the user the option to copy this information 
//		to the clipboard.	
		tempInfo = scs.label.getText();
		tryPopUp(evt);
//		***** GMA 1.6.2
	}

	public void mouseReleased( MouseEvent evt ) {
//		***** GMA 1.6.2: Maintains the presence of the pop-up even when the user releases 
//		the mouse button when the user releases the right mouse button.
		tryPopUp(evt);
//		***** GMA 1.6.2
	}
	public void mouseClicked( MouseEvent evt ) {
	}
	public void mouseEntered( MouseEvent evt ) {
		this.getTopLevelAncestor().repaint();
		mouseMoved( evt );
		if( cruise==null ) return;
		cruise.map.repaint();
	}
	public void mouseExited( MouseEvent evt ) {
		drawShape();
		scs.setInfoText( "Lamont single-channel seismics data rescue project" );
		shape=null;
	}
	public void mouseDragged( MouseEvent evt ) {
		mouseMoved( evt );
	}
	int lastIndex = 0;

	double[] ageDepthAtTime(int t) {
		if( lastIndex>=timeDep.size() )lastIndex = 0;
		TimeDep td1 = (TimeDep)timeDep.get(lastIndex);
		TimeDep td2 = (TimeDep)timeDep.get(lastIndex);
		int time=td1.time;
		if( time<t ) {
			while( lastIndex<timeDep.size()-1 ) {
				td1 = td2;
				lastIndex++;
				td2 = (TimeDep)timeDep.get(lastIndex);
				time = td2.time;
				if(time>t) break;
			}
		} else {
		//	while( lastIndex<timeDep.size()-1 ) {
			while( lastIndex>0 ) {
				td2 = td1;
				lastIndex--;
				td1 = (TimeDep)timeDep.get(lastIndex);
				time = td1.time;
				if(time<t) break;
			}
		}
		double[] ageDepth = new double[2];
		ageDepth[0] = Double.NaN;
		ageDepth[1] = Double.NaN;
		
		if( t<td1.time || t>td2.time )	{
			return ageDepth;
		}

		double depth = (double)td1.dep + (double)(td2.dep-td1.dep)
			* (t-td1.time)/(td2.time-td1.time);

		if( td1.age==-32768 || td2.age==-32768 ) {
			ageDepth[1] = depth;
			return ageDepth;
		}

		double age = (double)td1.age + (double)(td2.age-td1.age)
				* (t-td1.time)/(td2.time-td1.time);
		ageDepth[0] = .01*age;
		ageDepth[1] = depth;
		return ageDepth;
	}

	public Integer getTimeAtPoint( Point p ) {
		if( cruise == null ) return null;

		double x = p.getX() / zoomX;
		double y = p.getY() / zoomY;
		int i=0;
		while( i<cruise.xPosition.length 
				&& cruise.xPosition[i]+cruise.panelSize[i][0] < x ) {
		//	((SCSPanel2)panels.get(i)).image = null;
			i++;
		}
		if( i==cruise.xPosition.length || cruise.xPosition[i]>x ) 
			return null;
		return cruise.start[i] + (int)(30*(x-cruise.xPosition[i]));
	}

	public void mouseMoved( MouseEvent evt ) {
		if( cruise==null ) return;
		double x = evt.getX() / zoomX;
		double y = evt.getY() / zoomY;
		int i=0;
		while( i<cruise.xPosition.length 
				&& cruise.xPosition[i]+cruise.panelSize[i][0] < x ) {
		//	((SCSPanel2)panels.get(i)).image = null;
			i++;
		}
		if( i==cruise.xPosition.length || cruise.xPosition[i]>x ) return;
		SCSPanel2 panel = (SCSPanel2)panels.get(i);
		int t = cruise.start[i] + (int)(30*(x-cruise.xPosition[i]));
		double[] newAgeDepth = new double[2];
		newAgeDepth = ageDepthAtTime(t);
		double age = newAgeDepth[0];
		double sfDepthms = newAgeDepth[1];
		double sfDepthM = sfDepthms * (SOUND_VELOCITY_WATER / 2000.0);
		double sfDepthSec = sfDepthms / 1000;
		double cursorDepthM = 0.75 * 7.5 * y;
		double cursorDepthSec = 0.0075 * y;
		double sedThicknessM = 0;
		double sedThicknessSec = 0;
		double backTrackedDepthM = 0;

		if (cursorDepthSec > sfDepthSec)	{
			sedThicknessSec = cursorDepthSec - sfDepthSec;
			sedThicknessM = sedThicknessSec * (SOUND_VELOCITY_SEDIMENT / 2.0);
			backTrackedDepthM = cursorDepthM - B_COEFF * Math.sqrt(age) - (sedThicknessM / 2.0);
		}

		Point2D p = cruise.nav.positionAtTime( t );
		drawShape();
		if( p==null )return;
		double size = 7./cruise.map.getZoom();
		shape = (Shape) (new Arc2D.Double( p.getX()-size, p.getY()-size, 
						2.*size, 2.*size, 
						0., 360., Arc2D.CHORD ));

//		***** GMA 1.6.6: Add dot to display for Macs

		prevDot = dot;
		dot = (Shape)( new Arc2D.Double( p.getX(), p.getY(),  size/6, size/6, 0., 360., Arc2D.CHORD ) );
//		***** GMA 1.6.6

		StringBuffer label = new StringBuffer();
		NumberFormat fmt = NumberFormat.getInstance();

//		***** GMA 1.6.2: Add current distance along SCS leg to the data label
		label.append( cruise.name);	
		String xPos = Double.toString(x*0.1852);
		if ( xPos.indexOf(".") != -1 ) {
			xPos = xPos.substring(0,xPos.indexOf("."));
		}
		label.append(" " + xPos + "km (");
//		***** GMA 1.6.2

		p = cruise.map.getProjection().getRefXY( p );
		double longitude = p.getX();
		double latitude = p.getY();
		if	(longitude > 180.0)	{
			longitude -= 360.0;
		}

//		***** GMA 1.6.2: Add "WESN" labels to the longitude and latitude
		if ( longitude < 0.00 ) {
			label.append( fmt.format(longitude) + "\u00B0W" );
		}
		else {
			label.append( fmt.format(longitude) + "\u00B0E" );
		}

		if ( latitude < 0.00 ) {
			label.append( ",  " + fmt.format(latitude) + "\u00B0S" );
		}
		else {
			label.append( ",  " + fmt.format(latitude) + "\u00B0N" );
		}
//		***** GMA 1.6.2

		if ( sfDepthms >= 0 ) {
			label.append(",  " + fmt.format((int)sfDepthM) + "m");
		}
		if ( age >= 0 )	{
			label.append(",  " + fmt.format(age) + "Ma");
		}
		label.append("),  " );
		label.append( "z = "+ fmt.format((int)cursorDepthM) +"m,  tt = "+  fmt.format(cursorDepthSec) + "sec"); 
		if ( sfDepthms >= 0 && sedThicknessM > 1 )	{
			label.append(",  thickness = " + fmt.format((int)sedThicknessM) + "m");
		}
		if ( age >= 0 && sedThicknessM > 10 )	{
//			***** GMA 1.6.2: Display "backtrackz" instead of "btz" to better convey information to the user
			label.append(",  backtrackz = " + fmt.format((int)backTrackedDepthM) + "m");
//			***** GMA 1.6.2

		}
		scs.setInfoText( label.toString() );
		drawShape();
	}

	Shape shape;
	void drawShape() {
		if( shape==null ) return;
		synchronized( cruise.map.getTreeLock() ) {
			Graphics2D gg = cruise.map.getGraphics2D();

//			***** GMA 1.6.6: Display dot for Macs

			String osName = System.getProperty("os.name");
			if ( osName.startsWith("Mac OS") ) {
				if ( prevDot != null ) {
					gg.setColor(Color.red);
					gg.setStroke( new BasicStroke( 2f/(float)cruise.map.getZoom() ) );
					gg.draw(prevDot);
				}
				gg.setColor(Color.white);
				gg.setStroke( new BasicStroke( 2f/(float)cruise.map.getZoom() ) );
				gg.draw(dot);
			}
//			***** GMA 1.6.6

			gg = cruise.map.getGraphics2D();
			gg.setStroke( new BasicStroke(1f/(float)cruise.map.getZoom() ));
			gg.setXORMode( Color.white );
			gg.draw( shape );
			double wrap = cruise.map.getWrap();
			if( wrap>0. ) {
				gg.translate( wrap, 0.);
				gg.draw( shape );

//				***** GMA 1.6.6: Display dot for Macs
				if ( osName.startsWith("Mac OS") ) {
					gg = cruise.map.getGraphics2D();
					gg.translate( wrap, 0.);
					if ( prevDot != null ) {
						gg.setColor(Color.red);
						gg.setStroke( new BasicStroke( 2f/(float)cruise.map.getZoom() ) );
						gg.draw(prevDot);
					}
					gg.setColor(Color.white);
					gg.setStroke( new BasicStroke( 2f/(float)cruise.map.getZoom() ) );
					gg.draw(dot);
				}
//				***** GMA 1.6.6
			}
		}
	}
	public void scrollTo (int t) {
		if(cruise==null) return;
		int i=0;
		int x=0;
		while( i<cruise.start.length && cruise.start[i]<t ) i++;
		if( i==cruise.start.length ) {
			x = width;
		} else if( t<cruise.start[i] ) {
			x = cruise.xPosition[i];
		} else if( t>cruise.start[i]+30*cruise.panelSize[i][0] ) {
			x = cruise.xPosition[i] + cruise.panelSize[i][0];
		} else {
			x = cruise.xPosition[i] + (t-cruise.start[i])/30;
		}
		Rectangle r = getVisibleRect();
		x = (int) (x*zoomX) - r.width/2;
		JScrollBar sb = scrollPane.getHorizontalScrollBar();
		sb.setValue(x);
		repaint();
	}
	void setPlotDepth( boolean tf ) {
		plotDepth = tf;
		repaint();
	}
	class TimeDep {
		int time;
		short dep, age;
		public TimeDep( int time, short dep, short age) {
			this.time = time;
			this.dep = dep;
			this.age = age;
		}
	}

//	1.3.5: Toggler allows zoom buttons to be deselected
	private class ToggleToggler implements ActionListener, ChangeListener { 
		boolean wasSelected;
		JToggleButton b,no;
		ButtonGroup bg;

		public ToggleToggler(JToggleButton b,JToggleButton no){
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

	void saveImage(boolean saveWholeImage) throws IOException {
		if ( this.getGraphics() == null) {
			JOptionPane.showMessageDialog(getTopLevelAncestor(), "Image not loaded.");
			return;
		}

		File file = new File(cruise.name + ".jpg");
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

		try {
			saveJPG(file, saveWholeImage);
			JOptionPane.showMessageDialog(MapApp.anchor, "Save Successful");
			if (saveWholeImage) {
				MapApp.sendLogMessage("Saving_or_Downloading&portal="+scs.getDBName()+"&what=image_full&cruise="+this.cruise.name);
			} else {
				MapApp.sendLogMessage("Saving_or_Downloading&portal="+scs.getDBName()+"&what=image_viewport&cruise="+this.cruise.name);
			}
		}
		catch(IOException ex) { 
			JOptionPane.showMessageDialog(MapApp.anchor, ex.getMessage(), "Error Writing Jpeg", JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}

		setCursor(Cursor.getDefaultCursor());
	}
	
	public void saveJPG(File outputFile, boolean saveWholeImage) throws IOException {
		Rectangle rect;
		if ( saveWholeImage ) {
			rect = this.getBounds();
		}
		else {
			rect = scrollPane.getViewport().getVisibleRect().getBounds();
		}
		BufferedImage im = new BufferedImage( rect.width, rect.height, BufferedImage.TYPE_INT_RGB);
		if ( saveWholeImage ) {
			im.getGraphics().setColor(Color.white);
			im.getGraphics().fillRect(rect.x, rect.y, rect.width, rect.height);
			this.paintWhole(im.getGraphics());
		}
		else {
			im.getGraphics().setColor(Color.white);
			im.getGraphics().fillRect(rect.x, rect.y, rect.width, rect.height);
			scrollPane.getViewport().paint(im.getGraphics());
		}
		int s_idx = outputFile.getName().indexOf(".");
		String suffix = s_idx<0
				? "jpg"
				: outputFile.getName().substring(s_idx+1);
		if( !javax.imageio.ImageIO.getImageWritersBySuffix(suffix).hasNext() ) {
			suffix = "jpg";
		}
		javax.imageio.ImageIO.write( (RenderedImage)im, suffix, outputFile );
	}
	
//	***** GMA 1.6.2: Functions to display the pop-up menu and copy the SCS data for 
//	the current point to the clipboard.
	public void tryPopUp(MouseEvent evt){
		String osName = System.getProperty("os.name");
		if ( !evt.isControlDown()  && !zoomInTB.isSelected() && !zoomOutTB.isSelected() ) {
			if ( osName.startsWith("Mac OS") && evt.isShiftDown() ) {
				pm.show(evt.getComponent(), evt.getX(), evt.getY() );
			}
			else if ( evt.isPopupTrigger() ) {
				pm.show(evt.getComponent(), evt.getX(), evt.getY());
			}
		}
	}

	public void copy() {
		StringBuffer sb = new StringBuffer();
		sb.append(tempInfo);
		Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
		String tempString = sb.toString();
		
		tempString = tempString.substring(0,tempString.indexOf(" ")) + tempString.substring(tempString.indexOf(" ")).replaceAll("[\\w\\(\\),=&&[^\\d]]+","");
		tempString = tempString.replaceAll("\\s+","\t");
		tempString = tempString.replaceAll("\\u00B0","");

/*
		String[] tempStringArray = tempString.split("\t");
		tempString = "";
		boolean valueIsLongitude = true;
		for ( int i = 0; i < tempStringArray.length; i++ ) {
			if ( tempStringArray[i].indexOf("\u00B0") != -1 ) {
				if (  valueIsLongitude ) {
					if ( Double.parseDouble( tempStringArray[i].substring( 0, tempStringArray[i].indexOf("\u00B0") ) ) < 0.00 ) {
						tempStringArray[i] += "W";
					}
					else {
						tempStringArray[i] += "E";
					}
					valueIsLongitude = false;
				}
				else {
					if ( Double.parseDouble( tempStringArray[i].substring( 0, tempStringArray[i].indexOf("\u00B0") ) ) < 0.00 ) {
						tempStringArray[i] += "S";
					}
					else {
						tempStringArray[i] += "N";
					}
				}
			}
			tempString += ( tempStringArray[i] + "\t" );
		}
*/

		tempString = tempString.trim();
		StringSelection ss = new StringSelection(tempString + "\n");
		c.setContents(ss, ss);
	}

	public void actionPerformed(ActionEvent evt) {
		if (evt.getActionCommand().equals("copy")) copy();
		else if( evt.getActionCommand().equals("save")) {
			final JDialog wholeImageD = new JDialog(((MapApp)scs.map.getApp()).getFrame(), "Save Image Options");
			ButtonGroup bg = new ButtonGroup();
			final JRadioButton saveWhole = new JRadioButton("Save Whole Image");
			bg.add(saveWhole);
			wholeImageD.add(saveWhole, "North");
			JRadioButton saveView = new JRadioButton("Save Image in View");
			bg.add(saveView);
			saveWhole.setSelected(true);
			wholeImageD.add(saveView, "West");
			JPanel okCancelP = new JPanel(new FlowLayout());
			JButton b;
			b = new JButton("OK");
			b.setActionCommand("OK - SCS Save Image Options");
			b.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					wholeImageD.dispose();
					new Thread(){
						public void run() {
							try {
								if ( saveWhole.isSelected() ) {
									saveImage(true);
								}
								else {
									saveImage(false);
								}
							}
							catch (IOException ex) { }
						}
					}.start();
				}
			});
			okCancelP.add(b);
			b = new JButton("Cancel");
			b.setActionCommand("Cancel - SCS Save Image Options");
			b.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					wholeImageD.dispose();
				}
			});
			okCancelP.add(b);
			wholeImageD.add(okCancelP, "South");
			wholeImageD.pack();
			wholeImageD.setLocation(MapApp.getAnchorX()+400, MapApp.getAnchorY()+400);
			wholeImageD.setVisible(true);
		}
		else if ( evt.getActionCommand().equals("Zoom In") || evt.getActionCommand().equals("Zoom Out") ) {
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
		else if(evt.getActionCommand().equals("select")) {

		}
		else if (evt.getActionCommand().equals("deleteLastHorizon")) {
			if (dig.getObjects().size() == 0) return;
			String msg = "Are you sure you wish to delete the last horizon?";
			int n = JOptionPane.showConfirmDialog(this, msg, "Confirm Horizon Deletion", JOptionPane.YES_NO_OPTION);
			if (n == JOptionPane.NO_OPTION) return;
			dig.deleteLastObject();
		}
		else if (evt.getActionCommand().equals("deleteLastPick")) {			
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
					repaint();
				}
			}
			catch(Exception ex) {return;};
		}
		else if (evt.getActionCommand().equals("nameHorizon")) {
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
			catch (Exception ex) {return;}
		}
		else if(Integer.parseInt(evt.getActionCommand())==0)
		{
			if(zoomInTB.isSelected()) {
				zoomInTB.doClick();
				dig.getDigB().doClick();
			}
			if(zoomOutTB.isSelected()) {
				zoomOutTB.doClick();
				dig.getDigB().doClick();
			}
		}
		else if(Integer.parseInt(evt.getActionCommand())==1) {
			if(zoomInTB.isSelected()) {
				zoomInTB.doClick();
				dig.getAnnotB().doClick();
			}
			if(zoomOutTB.isSelected()) {
				zoomOutTB.doClick();
				dig.getAnnotB().doClick();
			}
		}
	}
//	***** GMA 1.6.2	
	public SCSCruise getCruise() {
		return cruise;
	}

	public GeneralPath getPaths() {
		return paths;
	}
	public Double getZoomValue() {
		return null;
	}
	public void zoomSpeed(Point p, Double d) {
	}
}

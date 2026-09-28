package haxby.db.mgg;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import haxby.db.XYGraph;
import haxby.db.XYPoints;
import haxby.map.MapApp;
import haxby.map.Overlay;
import haxby.map.XMap;
import haxby.nav.ControlPoint;
import haxby.nav.Nav;
import haxby.proj.Mercator;
import haxby.proj.Projection;
import haxby.util.GeneralUtils;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

public class MGGData implements Overlay,
				XYPoints,
				MouseListener,
				MouseMotionListener,
				ActionListener {
	public String id;
	public XMap map;
	public double[] lon, lat;
	public float[] x;
	public boolean[] connect;
	public float[][] data;
	public double[][] yRange;
	public double[] yScale;
	public double[] xRange;
	public double xScale;
	public int[][] cptIndex;
	public float[][] cptX, cptY;
	public Rectangle2D.Double mapBounds;
	public double offset;
	public int[] currentRange = new int[] {0, 0};
	public Point2D.Double currentPoint = null;
	public GeneralPath currentSeg = null;
	public static Color ON_COLOR = Color.red;
	public static Color OFF_COLOR = Color.yellow;

	static String MGD77_PATH = PathUtil.getPath("PORTALS/MGD77_PATH",
			MapApp.BASE_URL+"/data/portals/mgd77/");
	static String MGD77_DATA_LDEO = PathUtil.getPath("PORTALS/MGD77_DATA_LDEO",
			MapApp.BASE_URL+"/data/portals/mgd77/ldeo-mgd77/data/");
	static String MGD77_DATA_NGDC = PathUtil.getPath("PORTALS/MGD77_DATA_NGDC",
			MapApp.BASE_URL+"/data/portals/mgd77/ngdc-mgd77/data/");
	static String MGD77_DATA_ADGRAV = PathUtil.getPath("PORTALS/MGD77_DATA_ADGRAV",
			MapApp.BASE_URL+"/data/portals/mgd77/adgrav-mgd77/data/");
	static String MGD77_DATA_SIO = PathUtil.getPath("PORTALS/MGD77_DATA_SIO",
			MapApp.BASE_URL+"/data/portals/mgd77/sioexplorer-mgd77/data/");

//	***** GMA 1.6.2: Change the titles of the radio buttons to more clearly convey the data type 
//	being selected.
	static String[] title = {"Depth, m", "Gravity, mGal", "Magnetics, nT"};
//	***** GMA 1.6.2

//	***** GMA 1.6.2: Add variables to record and display data for a user-selected point along a 
//	MGG track.
	static String[] units = {"m", "mGal", "nT" };
	String tempInfo = null;
	double currentDistance = 0.0;
	int currentDataIndex = 0;
	JPopupMenu pm;
//	***** GMA 1.6.2	

//	1.4.4: MGD_77 file format positions and constants
	static final int MGD77_LAT_START_POS = 27;
	static final int MGD77_LAT_END_POS = 34;
	static final int MGD77_LON_START_POS = 35;
	static final int MGD77_LON_END_POS = 43;
	static final double MGD77_LAT_SCALE = 0.00001;
	static final double MGD77_LON_SCALE = 0.00001;
	static final int MGD77_BATHY_START_POS = 51;
	static final int MGD77_BATHY_END_POS = 56;
	static final int MGD77_MAGNETICS_START_POS = 72;
	static final int MGD77_MAGNETICS_END_POS = 77;
	static final int MGD77_GRAVITY_START_POS = 103;
	static final int MGD77_GRAVITY_END_POS = 107;
	static final int MGD77_BATHY_SCALE = 10;
	static final int MGD77_MAGNETICS_SCALE = 10;
	static final int MGD77_GRAVITY_SCALE = 10;
	
	static final int MGD77T_DATE_FIELD = 2;
	static final int MGD77T_TIME_FIELD = 3;
	static final int MGD77T_LAT_FIELD = 4;
	static final int MGD77T_LON_FIELD = 5;
	static final int MGD77T_BATHY_FIELD = 9;
	static final int MGD77T_MAGNETICS_FIELD = 15;
	static final int MGD77T_GRAVITY_FIELD = 22;

	public MGGData( XMap map,
			String leg, 
			double[] lon, 
			double[] lat, 
			float[] topo,
			float[] grav,
			float[] mag) {
		this.map = map;
		id = leg;
		this.lon = lon;
		this.lat = lat;
		int n = lon.length;
		mapBounds = new Rectangle2D.Double();

//		***** GMA 1.6.2: Add a pop-up menu that appears when a right-click occurs to allow the user 
//		to record the data for a particular point along the selected MGG track.
		pm = new JPopupMenu();
		JMenuItem mi = new JMenuItem("Copy Information to Clipboard");
		mi.setActionCommand("copy");
		mi.addActionListener(this);
		pm.add(mi);
//		***** GMA 1.6.2

		// compute control points
		if( map!=null ) {
			Nav nav = new Nav( leg );
			for( int i=0 ; i<n ; i++) nav.addPoint( i, lon[i], lat[i]);
			Mercator merc = new Mercator( 0., 0., 320*1024, 
					Projection.SPHERE, Mercator.RANGE_0_to_360);
			nav.computeControlPoints(merc, 1024*320, 250.);
			Vector cpt = nav.getControlPoints();
			cptIndex = new int[cpt.size()][];
			cptX = new float[cpt.size()][];
			cptY = new float[cpt.size()][];
			double minX = 0.;
			double minY = 0.;
			double maxX = 0.;
			double maxY = 0.;
			double meanX=0f;
			double npt=0f;
			Projection proj = map.getProjection();
			double wrap = map.getWrap();
			for( int k=0 ; k<cpt.size() ; k++) {
				Vector seg = (Vector)cpt.get(k);
				if(seg.size()<=1) {
					cptX[k] = new float[0];
					cptY[k] = new float[0];
					cptIndex[k] = new int[0];
					continue;
				}
				cptX[k] = new float[seg.size()];
				cptY[k] = new float[seg.size()];
				cptIndex[k] = new int[seg.size()];
				Point2D.Double pt;
				ControlPoint p;
				for( int i=0 ; i<seg.size() ; i++ ) {
					p = (ControlPoint)seg.get(i);
					pt = (Point2D.Double)proj.getMapXY( new Point2D.Double( p.x, p.y ) );
					if( wrap>0d && npt!=0 ) {
						while( pt.x - meanX/npt<wrap*.5 ) pt.x+=wrap;
						while( pt.x- meanX/npt>wrap*.5 ) pt.x-=wrap;
					}
					if(npt == 0) {
						minX = maxX = pt.x;
						minY = maxY = pt.y;
					}
					meanX += pt.getX();
					npt++;
					if( pt.x>maxX ) maxX=pt.x;
					else if ( pt.x<minX ) minX=pt.x;
					if( pt.y>maxY ) maxY=pt.y;
					else if ( pt.y<minY ) minY=pt.y;
					cptX[k][i] = (float)pt.x;
					cptY[k][i] = (float)pt.y;
					cptIndex[k][i] = p.time;
				}
			}
			mapBounds = new Rectangle.Double( minX-.5, minY-.5, maxX-minX+1., maxY-minY+1. );
		}
		data = new float[3][];
		data[0] = topo;
		data[1] = grav;
		data[2] = mag;
		yRange = new double[3][];
		yScale = new double[3];
		for( int k=0 ; k<3 ; k++) {
			if( data[k]==null || data[k].length==0 ) {
				data[k] = null;
				yRange[k] = new double[] {0., 200.};
				yScale[k] = 1.;
				continue;
			}
			int i=0;
			while( i<n && Float.isNaN(data[k][i]) ) i++;
			if( i==n ) {
				data[k]=null;
				yRange[k] = new double[] {0., 200.};
				yScale[k] = 1.;
				continue;
			}
			float min = data[k][i++];
			float max = min;
			while( i<n ) {
				if( !Float.isNaN(data[k][i]) ) {
					if(data[k][i]<min) min=data[k][i];
					else if(data[k][i]>max) max=data[k][i];
				}
				i++;
			}
			if(max==min) {
				max++;
				min--;
			}
			yRange[k] = new double[] { (double)min, (double)max };
			yScale[k] = 200. / (max-min );
		}
		x = new float[n];
		connect = new boolean[n];
		x[0] = 0f;
		for( int i=1 ; i<n ; i++) {
			double dx = Math.pow(lat[i]-lat[i-1],2);
			double c = Math.cos( Math.toRadians((lat[i]+lat[i-1])/2.) );
			while( lon[i]-lon[i-1] > 180. ) lon[i] -= 360.;
			while( lon[i]-lon[i-1] < -180. ) lon[i] += 360.;
			dx += Math.pow((lon[i]-lon[i-1])*c,2);
			dx = Math.sqrt(dx) * 111.2;
			if(dx>25f) {
				x[i] = x[i-1] + 25;
				connect[i]=false;
			} else {
				x[i] = x[i-1]+(float)dx;
				connect[i] = true;
			}
		}
		xRange = new double[] { 0., (double)x[n-1] };
		xScale = .2;
	}
	public boolean[] dataTypes() {
		return new boolean[] {
			data[0] != null,
			data[1] != null,
			data[2] != null };
	}
	public double[] getXRange( int index ) {
		return new double[] {xRange[0], xRange[1]};
	}
	public double[] getYRange( int index ) {
		return new double[] {yRange[index][0], yRange[index][1]};
	}
	public double getPreferredXScale( int index ) {
		return xScale;
	}
	public double getPreferredYScale( int index ) {
		return yScale[index];
	}
	public String getXTitle( int index ) {
		return "distance, km";
	}
	public String getYTitle( int index ) {
		return title[index];
	}

//	***** GMA 1.6.2: These functions allow the currently selected data index (Depth, Gravity
//	or Magnetic) to be communicated to and gotten from a MGGData object.
	public int getCurrentDataIndex() {
		return currentDataIndex;
	}

	public void setCurrentDataIndex( int index ) {
		currentDataIndex = index;
	}
//	***** GMA 1.6.2	

	public void setXInterval( float x1, float x2) {
		if(map==null || !map.isVisible() ) return;
		synchronized ( map.getTreeLock() ) {
		drawCurrentPoint();
		currentPoint = null;
		Graphics2D g = map.getGraphics2D();
		drawCurrentSeg(g, false);
		float xa, xb, dx;
		boolean in = false;
		boolean out = false;
		currentSeg = new GeneralPath();
		for( int seg = 0 ; seg<cptX.length ; seg++ ) {
			if(cptX[seg].length==0) continue;
			xa = x[cptIndex[seg][0]];
			if( xa>=x1 && xa<=x2 ) {
				in = true;
				currentSeg.moveTo( cptX[seg][0],cptY[seg][0]);
			} else if( in ) {
				break;
			}
			for( int i=1 ; i<cptX[seg].length ; i++ ) {
				//ignore the final point as this will plot at (0,0)
				if (cptIndex[seg][i] == x.length - 1)  continue;
				xb = x[cptIndex[seg][i]];
			
				if( in && xb<=x2 ) {
					currentSeg.lineTo( cptX[seg][i],cptY[seg][i]);
				} else if( in ) {
					dx = (x2-xa) / (xb-xa);
					currentSeg.lineTo( cptX[seg][i-1]*(1f-dx) + cptX[seg][i]*dx,
						cptY[seg][i-1]*(1f-dx) + cptY[seg][i]*dx);
					out = true;
					break;
				} else if( xb>= x1 ) {
					dx = (x1-xa) / (xb-xa);
					currentSeg.moveTo( cptX[seg][i-1]*(1f-dx) + cptX[seg][i]*dx,
						cptY[seg][i-1]*(1f-dx) + cptY[seg][i]*dx);
					in = true;
					if( xb>x2 ) {
						dx = (x2-xa) / (xb-xa);
						currentSeg.lineTo( cptX[seg][i-1]*(1f-dx) + cptX[seg][i]*dx,
							cptY[seg][i-1]*(1f-dx) + cptY[seg][i]*dx);
						out = true;
						break;
					} else {
						currentSeg.lineTo( cptX[seg][i],cptY[seg][i]);
					}
				}
				xa = xb;
			}
			if(out) break;
		}
		drawCurrentSeg(g, true );
		}
	}

//	1.3.5: drawCurrentPoint() draws the circle on the map that indicates which part of the track
//	you are looking at; circle is now thicker
	protected void drawCurrentPoint() {
		if( map==null || currentPoint==null || !map.isVisible() ) return;
		synchronized (map.getTreeLock() ) {
			Graphics2D g = map.getGraphics2D();
			Graphics2D g2 = map.getGraphics2D();
			float zoom = (float)map.getZoom();

			g.setStroke( new BasicStroke( 5f/ zoom ) );

			Rectangle2D rect = map.getClipRect2D();
			double wrap = map.getWrap();
			if( wrap>0. ) while( currentPoint.x-wrap > rect.getX() ) currentPoint.x-=wrap;
			double size = 10./map.getZoom();

			Arc2D.Double arc = new Arc2D.Double( 0., currentPoint.y, 
							size/6, size/6, 0., 360., Arc2D.CHORD);
			Arc2D.Double arc2 = new Arc2D.Double( 0., currentPoint.y-.5*size, 
					size, size, 0., 360., Arc2D.CHORD);
			if( wrap>0. ) {
				while( currentPoint.x < rect.getX()+rect.getWidth() ) {
					g.setColor(Color.red);
					g.setStroke( new BasicStroke( 5f/ zoom ) );
					g.draw(currentSeg);
					g.setColor(Color.white);
					g.setStroke( new BasicStroke( 2.f/ zoom ) );
					arc.x = currentPoint.x;
					g.draw(arc);
					arc2.x = currentPoint.x-.5*size;
					g2.setXORMode( Color.white );
					g2.setStroke( new BasicStroke( 4f/ zoom ) );
					g2.draw(arc2);
					currentPoint.x += wrap;
				}
			} else {
				g.setColor(Color.red);
				g.setStroke( new BasicStroke( 5f/ zoom ) );
				g.draw(currentSeg);
				g.setColor(Color.white);
				g.setStroke( new BasicStroke( 2.f/ zoom ) );
				arc.x = currentPoint.x;
				g.draw(arc);
				arc2.x = currentPoint.x-.5*size;
				g2.setXORMode( Color.white );
				g2.setStroke( new BasicStroke( 4f/ zoom ) );
				g2.draw(arc2);
			}
		}
	}
	protected void drawCurrentSeg(Graphics2D g, boolean on) {
		if( currentSeg==null ) return;
		Color color = g.getColor();
		Stroke stroke = g.getStroke();
		AffineTransform at = g.getTransform();
		g.setColor( on ? ON_COLOR : OFF_COLOR );

//		***** GMA 1.5.2: TESTING
//		g.setStroke( new BasicStroke( 1f/ (float)map.getZoom()) );
		g.setStroke( new BasicStroke( 5f/ (float)map.getZoom()) );
//		***** GMA 1.5.2

		double wrap = map.getWrap();
		if( wrap>0. ) {
			g.translate(-wrap, 0.);
			g.draw( currentSeg );
			g.translate(wrap, 0.);
			g.draw( currentSeg );
			g.translate(wrap, 0.);
			g.draw( currentSeg );
		} else {
			g.draw( currentSeg );
		}
		g.setColor(color);
		g.setStroke(stroke);
		g.setTransform( at);
	}
	public void mouseEntered(MouseEvent evt) {
		mouseMoved(evt);
	}
	public void mouseExited(MouseEvent evt) {
		drawCurrentPoint();
		currentPoint = null;
	}
	public void mouseClicked(MouseEvent evt) {
	}
	public void mousePressed(MouseEvent evt) {

//		***** GMA 1.6.2: Record the MGG data at the point where the user right-clicks,
//		and then bring up a pop-up menu giving the user the option to copy this information
//		to the clipboard.
		tempInfo = map.getLonLat();
		XYGraph graph = (XYGraph)evt.getSource();
		currentDistance = graph.getXAt( evt.getPoint() );
		System.out.println(currentDistance);
		tryPopUp(evt);
//		***** GMA 1.6.2
	}
	public void mouseReleased(MouseEvent evt) {
//		***** GMA 1.6.2: Maintains the presence of the pop-up even when the user releases 
//		the mouse button when the user releases the right mouse button.
		tryPopUp(evt);
//		***** GMA 1.6.2
	}
	public void mouseMoved(MouseEvent evt) {
		if(map==null)return;
		if( !(evt.getSource() instanceof XYGraph) ) {
			return;
		}
		XYGraph graph = (XYGraph)evt.getSource();
		float x0 = (float)graph.getXAt( evt.getPoint() );
		for( int i=currentRange[0]+1 ; i<currentRange[1] ; i++) {
			if( x[i]>x0 ) {
				if( i!=0 && x0-x[i-1]<x[i]-x0) i--;
				Point2D p = map.getProjection().getMapXY(
						new Point2D.Double( lon[i], lat[i] ));
				drawCurrentPoint();
				currentPoint = (Point2D.Double)p;

//				***** GMA 1.6.2: Display lat lon information in addition to the data for the current point
//				in the MGG graph area.				
				if ( data[getCurrentDataIndex()] != null ) {
					map.setAlternateZValue(data[getCurrentDataIndex()][i]);
					map.setAlternateUnits(units[getCurrentDataIndex()]);
				}
				else {
					map.setAlternateZValue(Float.NaN);
					map.setAlternateUnits(units[0]);
				}
				map.setLonLat( lon[i], lat[i] );
				map.setAlternateZValue(Float.NaN);
				map.setAlternateUnits(units[0]);
//				***** GMA 1.6.2

				drawCurrentPoint();
				return;
			}
		}
	}
	public void mouseDragged(MouseEvent evt) {
		mouseMoved(evt);
	}
	public void plotXY( Graphics2D g,
				Rectangle2D bounds,
				double xScale, double yScale,
				int dataIndex) {
/*
		Rectangle2D.Double bnds = new Rectangle2D.Double();
		bnds.x = bounds.getX();
		bnds.width = bounds.getWidth();
		double h = g.getClipBounds().getHeight();
		for( int i=1 ; i<3 ; i++) {
			int c = 255-45*i;
			g.setColor( new Color(c,c,c));
			int index = (i+dataIndex)%3;
			bnds.y = yRange[index][1];
			bnds.height = yRange[index][0]-yRange[index][1];
			plot( g, bnds, xScale, h/bnds.height, index );
		}
*/
		g.setColor(Color.black);
		plot( g, bounds, xScale, yScale, dataIndex );
	}
	void plot( Graphics2D g,
				Rectangle2D bounds,
				double xScale, double yScale,
				int dataIndex) {
		if( data[dataIndex]==null) return;

		float x0 = (float)bounds.getX();
		float y0 = (float)bounds.getY();
		float x1 = x0;
		float x2 = x1+(float)bounds.getWidth();
		if(x1>x2) {
			x1 = x2;
			x2 = x0;
		}
		setXInterval(x1, x2);
		int i=0;
		while( i<x.length && x[i]<x1 ) {
			i++;
		}
		if( i!=0 && !Float.isNaN(data[dataIndex][i-1] )) i--;
		while( i<x.length && x[i]<x2 && Float.isNaN(data[dataIndex][i]) ) i++;
		if( i==x.length || x[i]>x2 ) {
			return;
		}
		currentRange[0] = i;
		GeneralPath path = new GeneralPath();
		float sy = (float)yScale;
		float sx = (float)xScale;
		path.moveTo( (x[i]-x0)*sx, (data[dataIndex][i]-y0)*sy);
		float lastX = x[i];
		while( i<x.length && x[i]<x2 ) {
			if( Float.isNaN(data[dataIndex][i])) {
				i++;
				continue;
			}
			//only plot the section of the track that is currently displayed on the map
			Point2D p = map.getProjection().getMapXY(lon[i], lat[i]);
			if (!inDisplayedMap(p)) {
				i++;
				continue;
			}
			if( !connect[i] || x[i]-lastX>25 ) {
				path.moveTo( (x[i]-x0)*sx, (data[dataIndex][i]-y0)*sy);
			} else {
				path.lineTo( (x[i]-x0)*sx, (data[dataIndex][i]-y0)*sy);
			}
			lastX = x[i];
			i++;
		}
		currentRange[1] = i--;
		g.draw( path );
		currentPoint = null;
	}
	
	/*
	 * Determine if a point is located on the displayed map
	 */
	public boolean inDisplayedMap(Point2D p) {
		double x = p.getX();
		double y = p.getY();
		//get the displayed area
		Rectangle2D rect = map.getClipRect2D();
		float yMin = (float)rect.getY();
		float yMax = (float)(rect.getY() + rect.getHeight());
		float xMin = (float)rect.getX();
		float xMax = (float)(rect.getX() + rect.getWidth());
		float wrap = (float)map.getWrap();

		if (wrap>0f){
			while (x>xMin+wrap) x-=wrap;
			while (x<xMax-wrap) x+=wrap;
		}
		
		if (x >= xMin && x <= xMax && y >= yMin && y <= yMax) return true;

		return false;
	}
	public boolean contains( double x, double y ) {
		if( map==null ) return false;
		if( y<mapBounds.y || y>mapBounds.y+mapBounds.height ) return false;
		if( map.getWrap()>0. ) {
			double wrap = map.getWrap();
			while( x<mapBounds.x ) x+=wrap;
			while( x>mapBounds.x+mapBounds.width ) x-= wrap;
			return (x>=mapBounds.x);
		} else {
			return ( x>=mapBounds.x && x<=mapBounds.width );
		}
	}
	public boolean intersects( Rectangle2D area ) {
		if( map==null ) return false;
		double wrap = map.getWrap();
		if(wrap>0) {
			if(area.getY()+area.getHeight()<mapBounds.getY()) return false;
			if(mapBounds.getY()+mapBounds.getHeight()<area.getY()) return false;
			while( mapBounds.getX()+(double)offset > area.getX() ) offset -= wrap;
			while( mapBounds.getX()+mapBounds.getWidth()+(double)offset <
				area.getX() ) offset += wrap;
			if( mapBounds.getX()+(double)offset > area.getX()+area.getWidth() ) return false;
			return true;
		} else {
			return mapBounds.intersects(area.getX(), area.getY(),
					area.getWidth(), area.getHeight());
		}
	}
	public void draw( Graphics2D g ) {
		Rectangle area = g.getClipBounds();
		currentPoint = null;
		if( !intersects(area) ) return;
		Color color = g.getColor();
		g.setColor( OFF_COLOR );
		GeneralPath path = new GeneralPath();
		float offset = (float)this.offset;
		for( int seg=0 ; seg<cptIndex.length ; seg++ ) {
			path.moveTo( offset+cptX[seg][0], cptY[seg][0] );
			for( int i=0 ; i<cptIndex[seg].length ; i++ ) {
				path.lineTo( offset+cptX[seg][i], cptY[seg][i] );
			}
		}
		g.draw(path);
		double wrap = map.getWrap();
		if(wrap>0) {
			AffineTransform xform = g.getTransform();
			offset += (float)wrap;
			while( mapBounds.getX()+(double)offset < area.getX()+area.getWidth() ) {
				g.translate( (double)wrap, 0.d );
				g.draw(path);
				offset += (float)wrap;
			}
			g.setTransform( xform );
		}
		g.setColor( ON_COLOR );
		drawCurrentSeg(g, true);
		g.setColor( color );
	}

	public static MGGData load(XMap map, String leg, String inputLoadedControlFile ) throws IOException {
//		1.4.4: Find the MGD-77 data file for the currently loaded leg
		URL url = URLFactory.url(MGD77_PATH.toString());
		String MGGurl = url.toString();
		URL dataDirURL;
		URL dataFileURL;
		BufferedReader in;
		BufferedReader inDataDir;
		BufferedReader inDataFile = new BufferedReader(new InputStreamReader( url.openStream()));
		boolean legFound = false;
		boolean isM77T = false;
		try {
			// If url isn't from the server then use the text control file.
			if(!MGGurl.contains(MapApp.BASE_URL)){
				url = URLFactory.url( MGD77_PATH + "control_files_list.txt");
				in = new BufferedReader( new InputStreamReader( url.openStream() ) );
			}else{
				in = new BufferedReader(new InputStreamReader( url.openStream() ) );
			}
			String s = "";
			String sDataDir = "";
			String sDataFile = "";
		
			legFound = false;
			if ( leg != null )	{
				
				// For imported files, load from data file stored on user's hard drive
				// Try this first
				if (!legFound) {
					try {
						MGGData data = loadFromDataFile(map, leg);
						if (data != null) return data;
					} catch (IOException e) {
					}
				}
				
				dataFileURL = null;
				if ( inputLoadedControlFile.compareTo( "LDEO" ) == 0 ) {
					dataFileURL = URLFactory.url(MGD77_DATA_LDEO + leg + ".a77");
				}
				else if ( inputLoadedControlFile.compareTo( "NGDC" ) == 0 ) {
					dataFileURL = URLFactory.url(MGD77_DATA_NGDC + leg + ".a77");
				}
				else if ( inputLoadedControlFile.compareTo( "USAP" ) == 0 ) { //ADGRAV
					dataFileURL = URLFactory.url(MGD77_DATA_ADGRAV + leg + ".a77");
				}
				else if ( inputLoadedControlFile.compareTo( "SIOExplorer" ) == 0 ) {
					dataFileURL = URLFactory.url(MGD77_DATA_SIO + leg + ".a77");
				}
				if ( dataFileURL != null) {
					
					//look for a77 file, if not found, see if there is a m77t file
					if (!URLFactory.checkWorkingURL(dataFileURL)) {
						String m77tFile = dataFileURL.toString().replaceAll(".a77", ".m77t");
						dataFileURL = URLFactory.url(m77tFile);
						isM77T = URLFactory.checkWorkingURL(dataFileURL);
					}
					
					try {
						inDataFile = new BufferedReader( new InputStreamReader( dataFileURL.openStream() ) );
						legFound = true;
					}
					catch ( IOException ioe ) {
						dataFileURL = null;
						inDataFile = null;
						legFound = false;
					}
				}
				if ( !legFound ) {					
					
					while ( ( s = in.readLine() ) != null )	{
						if ( s.indexOf( "[DIR]" ) != -1 && s.indexOf( "Parent Directory" ) == -1 ) {
							int start = s.indexOf( "a href=\"") + 8;
							s= s.substring(start);
							int end = s.indexOf("\">");
							s = s.substring(0, end);
							String dirName = s;
							dataDirURL = URLFactory.url(MGGurl + dirName + "data/");
							inDataDir = new BufferedReader( new InputStreamReader( dataDirURL.openStream() ) );
							while ( ( sDataDir = inDataDir.readLine() ) != null && legFound == false )	{
								if ( sDataDir.indexOf( leg ) != -1 ){
									if(sDataDir.contains("href")){
										sDataFile = sDataDir.substring( sDataDir.indexOf( "a href=\"" ) + 8, sDataDir.indexOf( "\">", sDataDir.indexOf( "a href=\"" ) ) );
									}else{
										sDataFile = sDataDir;
									}
									String tempDataFileName = sDataFile.substring(0, sDataFile.indexOf("."));
									if ( leg.equals(tempDataFileName) ) {
										dataFileURL = URLFactory.url(MGGurl + dirName + "data/" + sDataFile);
										inDataFile = new BufferedReader(
												new InputStreamReader( dataFileURL.openStream() ) );
										legFound = true;
									}
								}
							} //End of while
						}
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		
//		1.4.4: Parse the MGD-77 file and load it into data structure
		if (isM77T) {
			return loadFromM77TFile(map, leg, inDataFile);
		}
		return loadFromA77File(map, leg, inDataFile);
	}
	
	static MGGData loadFromA77File(XMap map, String leg, BufferedReader inDataFile) throws IOException {
		String s;
		ArrayList<Double> lon = new ArrayList<Double>();
		ArrayList<Double> lat = new ArrayList<Double>();
		ArrayList<Float> topo = new ArrayList<Float>();
		ArrayList<Float> grav = new ArrayList<Float>();
		ArrayList<Float> mag = new ArrayList<Float>();
		
		int nt=0;
		int ng=0;
		int nm=0;
		String temp = "";
		boolean dataPresent = false;
		while ( ( s = inDataFile.readLine() ) != null ) { // reading in the .a77 files
			try {
				temp = s.substring( MGD77_LON_START_POS, MGD77_LON_END_POS + 1);
				for ( int i = 0; i < temp.length(); i++ )	{
					if ( !( temp.substring( i, i + 1 ).equals("9") ) && !( temp.substring( i, i + 1 ).equals("+") ) )	{
						lon.add(Double.parseDouble(temp) * MGD77_LON_SCALE);
						dataPresent = true;
						break;
					}
				}
				temp = "";

				temp = s.substring( MGD77_LAT_START_POS, MGD77_LAT_END_POS + 1);
				for ( int i = 0; i < temp.length(); i++ )	{
					if ( !( temp.substring( i, i + 1 ).equals("9") ) && !( temp.substring( i, i + 1 ).equals("+") ) )	{
						lat.add(Double.parseDouble(temp) * MGD77_LAT_SCALE);
						dataPresent = true;
						break;
					}
				}
				temp = "";
			} catch (NumberFormatException ex) {
				continue;
			}

			dataPresent = false;
			float this_topo = Float.NaN;
			try {
				temp = s.substring( MGD77_BATHY_START_POS, MGD77_BATHY_END_POS + 1);
				for ( int i = 0; i < temp.length(); i++ )	{
					if ( !( temp.substring( i, i + 1 ).equals("9") ) && !( temp.substring( i, i + 1 ).equals("+") ) )	{
						this_topo = -1 * Float.parseFloat(temp) / MGD77_BATHY_SCALE;
						dataPresent = true;
						break;
					}
				}
				temp = "";
				if( !Float.isNaN( this_topo ) && dataPresent ) nt++;
				else {
					this_topo = Float.NaN;
				}
			} catch (NumberFormatException ex) {
				this_topo = Float.NaN;
			}
			topo.add(this_topo);
			dataPresent = false;

			float this_grav = Float.NaN;
			try {
				temp = s.substring( MGD77_GRAVITY_START_POS, MGD77_GRAVITY_END_POS + 1);
				for ( int i = 0; i < temp.length(); i++ )	{
					if ( !( temp.substring( i, i + 1 ).equals("9") ) && !( temp.substring( i, i + 1 ).equals("+") ) )	{
						this_grav = Float.parseFloat(temp) / MGD77_GRAVITY_SCALE;
						dataPresent = true;
						break;
					}
				}
				temp = "";
				if( !Float.isNaN( this_grav ) && dataPresent ) ng++;
				else	{
					this_grav = Float.NaN;
				}
			} catch (NumberFormatException ex) {
				this_grav = Float.NaN;
			}
			grav.add(this_grav);
			dataPresent = false;

			float this_mag = Float.NaN;
			try {
				temp = s.substring( MGD77_MAGNETICS_START_POS, MGD77_MAGNETICS_END_POS + 1);
				for ( int i = 0; i < temp.length(); i++ )	{
					if ( !( temp.substring( i, i + 1 ).equals("9") ) && !( temp.substring( i, i + 1 ).equals("+") ) )	{
						this_mag = Float.parseFloat(temp) / MGD77_MAGNETICS_SCALE;
						dataPresent = true;
						break;
					}
				}
				temp = "";
				if( !Float.isNaN( this_mag ) && dataPresent ) nm++;
				else {
					this_mag = Float.NaN;
				}
			} catch (NumberFormatException ex) {
				this_mag = Float.NaN;
			}
			mag.add(this_mag);
			dataPresent = false;
		}

		if( nt==0 && ng==0 && nm==0 ) throw new IOException("no data in leg "+leg);
		if(nt==0) topo=null;
		if(ng==0) grav=null;
		if(nm==0) mag=null;
		System.out.println(nt +"\t"+ ng +"\t"+ nm);
		MGGData data = new MGGData( map, leg, GeneralUtils.arrayList2doubles(lon), GeneralUtils.arrayList2doubles(lat),
				GeneralUtils.arrayList2floats(topo), GeneralUtils.arrayList2floats(grav), GeneralUtils.arrayList2floats(mag));
		return data;
	}
	
	static MGGData loadFromM77TFile(XMap map, String leg, BufferedReader inDataFile) throws IOException {
		String s;
		ArrayList<Double> lon = new ArrayList<Double>();
		ArrayList<Double> lat = new ArrayList<Double>();
		ArrayList<Float> topo = new ArrayList<Float>();
		ArrayList<Float> grav = new ArrayList<Float>();
		ArrayList<Float> mag = new ArrayList<Float>();

		int nt=0;
		int ng=0;
		int nm=0;
		String temp = "";
		boolean dataPresent = false;
		while ( ( s = inDataFile.readLine() ) != null ) { // reading in the .a77 files
			
			// split the line up into its tab-delimited elements
			String[] elements = s.split("\t");

			// check for header lines by seeing if lat field can be parsed as double
			try {
				Double.parseDouble(elements[MGGData.MGD77T_LAT_FIELD]);
			} catch (Exception e) {
				continue;
			}

			// get the lat and lon fields
			try {
				temp = elements[MGD77T_LON_FIELD];
				Double this_lon = Double.parseDouble(temp);
				dataPresent = true;
				if (this_lon < 0) {
					this_lon += 360.;
				}
				lon.add(this_lon);
				temp = "";

				temp = elements[MGD77T_LAT_FIELD];
				lat.add(Double.parseDouble(temp));
				temp = "";
			} catch (Exception ex) {
				continue;
			}

			// get any bathymetry data. If none, for this row, add a NaN.
			dataPresent = false;
			float this_topo = Float.NaN;
			try {
				temp = elements[MGD77T_BATHY_FIELD];
				this_topo = -1 * Float.parseFloat(temp);
				dataPresent = true;

				temp = "";
				if (!Float.isNaN(this_topo) && dataPresent)
					nt++;
				else {
					this_topo = Float.NaN;
				}
			} catch (Exception ex) {
				this_topo = Float.NaN;
			}
			topo.add(this_topo);
			
			// get any magnetic data. If none, for this row, add a NaN.
			dataPresent = false;
			float this_mag = Float.NaN;
			try {
				temp = elements[MGD77T_MAGNETICS_FIELD];
				this_mag = Float.parseFloat(temp);
				dataPresent = true;

				temp = "";
				if (!Float.isNaN(this_mag) && dataPresent)
					nm++;
				else {
					this_mag = Float.NaN;
				}
			} catch (Exception ex) {
				this_mag = Float.NaN;
			}
			mag.add(this_mag);

			// get any gravity data. If none, for this row, add a NaN.
			dataPresent = false;
			float this_grav = Float.NaN;
			try {
				temp = elements[MGD77T_GRAVITY_FIELD];
				this_grav = Float.parseFloat(temp);
				dataPresent = true;

				temp = "";
				if (!Float.isNaN(this_grav) && dataPresent)
					ng++;
				else {
					this_grav = Float.NaN;
				}
			} catch (Exception ex) {
				this_grav = Float.NaN;
			}
			grav.add(this_grav);
			

		}
		if( nt==0 && ng==0 && nm==0 ) throw new IOException("no data in leg "+leg);
		if(nt==0) topo=null;
		if(ng==0) grav=null;
		if(nm==0) mag=null;
		System.out.println(nt +"\t"+ ng +"\t"+ nm);
		
		//create the MGDData structure (need to convert the arrayLists to arrays first)
		MGGData data = new MGGData( map, leg, GeneralUtils.arrayList2doubles(lon), GeneralUtils.arrayList2doubles(lat),
				GeneralUtils.arrayList2floats(topo), GeneralUtils.arrayList2floats(grav), GeneralUtils.arrayList2floats(mag));
		return data;
	}
	
//	***** GMA 1.6.2: Functions to display the pop-up menu and copy the MGG data for 
//	the current point to the clipboard.
	public void tryPopUp(MouseEvent evt){
		String osName = System.getProperty("os.name");
		if ( !evt.isControlDown() ) {
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
		tempString = tempString.replaceAll("zoom.+","");
		tempString = tempString.replaceAll("[\\(\\)=,\\w&&[^WESN\\d]]+","");
		String [] result = tempString.split("\\s+");
		tempString = "";
		for ( int i =0; i < result.length; i++ ) {
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
			tempString += result[i] + "\t";
		}
		tempString = tempString.trim();
		tempString = id + "\t" + Double.toString(currentDistance) + "\t" + tempString;
		StringSelection ss = new StringSelection(tempString + "\n");
		c.setContents(ss, ss);
	}
	public void actionPerformed(ActionEvent evt) {
		if (evt.getActionCommand().equals("copy")) copy();
	}
//	***** GMA 1.6.2
	
	/*
	 * for imported data sets, load from the data file stored on the user's hard drive
	 */
	static MGGData loadFromDataFile(XMap map, String leg) throws IOException { 
		MGGData data = null;
		BufferedReader dataIn;
		File dataFile;
		DataInputStream controlIn;
		String name, s;
		ArrayList<Double> lon = new ArrayList<Double>();
		ArrayList<Double> lat = new ArrayList<Double>();
		ArrayList<Float> topo = new ArrayList<Float>();
		ArrayList<Float> grav = new ArrayList<Float>();
		ArrayList<Float> mag = new ArrayList<Float>();
		// First search the control files to find which one contains our data set.
		// The control file include the data set name.
		// The data file has the same timestamp in the file name, so once we have found
		// the control file, we can work out the data file.
		if ( MGG.MGG_control_dir.exists() ) {
			File[] MGG_control_files = MGG.MGG_control_dir.listFiles();
			for ( int m = 0; m < MGG_control_files.length; m++ ) {
				if ( MGG_control_files[m].getName().indexOf( "mgg_control" ) != -1 ) {
					controlIn = new DataInputStream( new BufferedInputStream( new FileInputStream( MGG_control_files[m] ) ) );
					name = "";
					try {
						name = controlIn.readUTF();
					} catch (EOFException ex) {
						break;
					}
					controlIn.close();
					
					if (name.equals(leg)) {
						//we have found the right control file, now read the data file
						dataFile = new File (MGG.MGG_data_dir, "mgg_data_" + leg);
						try {
							dataIn = new BufferedReader( new InputStreamReader(new FileInputStream( dataFile )));
						} catch (Exception ex) {
							continue;
						}
						while ( ( s = dataIn.readLine() ) != null ) {
						
							StringTokenizer st = new StringTokenizer(s);
							String date = st.nextToken(); //don't need this
							lon.add(Double.parseDouble(st.nextToken()));
							lat.add(Double.parseDouble(st.nextToken()));
							topo.add(Float.parseFloat(st.nextToken()));
							grav.add(Float.parseFloat(st.nextToken()));
							mag.add(Float.parseFloat(st.nextToken()));

						}
						dataIn.close();
						//create the MGDData structure (need to convert the arrayLists to arrays first)
						data = new MGGData( map, leg, GeneralUtils.arrayList2doubles(lon), GeneralUtils.arrayList2doubles(lat),
								GeneralUtils.arrayList2floats(topo), GeneralUtils.arrayList2floats(grav), GeneralUtils.arrayList2floats(mag));
					}
				}
			}
		}


		return data;
	}
	
	/*
	 * Find the x and y ranges for the portion of the track displayed on the map
	 */
	public double[][] getRangesOnMap(int k) {
		double[] newYRange = {};
		float minXOnMap = 1000000f;
		float maxXOnMap = -1000000f;
		float minYOnMap = 1000000f;
		float maxYOnMap = -1000000f;
		int n = lon.length;
		boolean yDataAvailable = true;

		//first find out if there is y-data available for this k-index
		float[] y =  data[k];
		if (y == null || y.length == 0) yDataAvailable = false;
		
		if (yDataAvailable) {
			int i = 0;
			while (i < n && Float.isNaN(data[k][i]))
				i++;
			if (i == n) yDataAvailable = false;
		}
		
		if (yDataAvailable) {
			for (int i=0; i<x.length; i++){
				Point2D p = map.getProjection().getMapXY(lon[i], lat[i]);
				if (inDisplayedMap(p)) {
					if (x[i] < minXOnMap) minXOnMap = x[i];
					if (x[i] > maxXOnMap) maxXOnMap = x[i];
					if (y[i] < minYOnMap) minYOnMap = y[i];
					if (y[i] > maxYOnMap) maxYOnMap = y[i];
				}
			}
			newYRange = new double[] {minYOnMap,  maxYOnMap};
		} else {
			for (int i=0; i<x.length; i++){
				Point2D p = map.getProjection().getMapXY(lon[i], lat[i]);
				if (inDisplayedMap(p)) {
					if (x[i] < minXOnMap) minXOnMap = x[i];
					if (x[i] > maxXOnMap) maxXOnMap = x[i];
				}
			}
			newYRange = new double[] { 0., 200. };
		}
		double[] newXRange = {minXOnMap, maxXOnMap};
		double[][] newRanges = {newXRange, newYRange};
		return newRanges;
	}
	
	/*
	 * Find the x and y ranges for the full track
	 */
	public double[][] getFullRanges(int k) {

		int n = lon.length;
		double[] newXRange = { 0., (double) x[n - 1] };
		double[] newYRange = {};

		if (data[k] == null || data[k].length == 0) {
			newYRange = new double[] { 0., 200. };
			double[][] newRanges = { newXRange, newYRange };
			return newRanges;
		}
		int i = 0;
		while (i < n && Float.isNaN(data[k][i]))
			i++;
		if (i == n) {
			newYRange = new double[] { 0., 200. };
			double[][] newRanges = { newXRange, newYRange };
			return newRanges;
		}
		float min = data[k][i++];
		float max = min;
		while (i < n) {
			if (!Float.isNaN(data[k][i])) {
				if (data[k][i] < min)
					min = data[k][i];
				else if (data[k][i] > max)
					max = data[k][i];
			}
			i++;
		}
		if (max == min) {
			max++;
			min--;
		}
		newYRange = new double[] { (double) min, (double) max };

		double[][] newRanges = { newXRange, newYRange };
		return newRanges;
	}

}
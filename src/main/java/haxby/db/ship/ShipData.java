package haxby.db.ship;
import haxby.db.XYGraph;
import haxby.db.XYPoints;
import haxby.map.MapApp;
import haxby.map.Overlay;
import haxby.map.XMap;
import haxby.nav.ControlPoint;
import haxby.nav.Nav;
import haxby.proj.Mercator;
import haxby.proj.Projection;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Vector;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class ShipData implements Overlay,  MouseListener, MouseMotionListener, ActionListener	
{
	public String id;
	public XMap map;
	public double[] lon, lat;
	public float[] x;
	public boolean[] connect;
	static String[] units = {"m", "mGal", "nT" };
	
	public double[] xRange;
	public double xScale;
	public int[][] cptIndex;
	public float[][] cptX, cptY;
	public Rectangle2D.Double mapBounds;
	public double offset;
	public int[] currentRange = new int[] {0, 0};
	public Point2D.Double currentPoint = null;
	public GeneralPath currentSeg = null;
	public Color onColor = Color.red;
	public Color offColor = Color.yellow;
	
	//Path info will need to be added 
	//TODO
	static String SHIP_PATH = PathUtil.getPath("PORTALS/SHIP_PATH",
			MapApp.BASE_URL+"/data/portals/ship/");
	
	String tempInfo = null;
	double currentDistance = 0.0;
	int currentDataIndex = 0;
	JPopupMenu pm;
	
	public ShipData(XMap map, String leg, double[] lon, double[] lat)
	{
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
		g.setColor( offColor);
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
		g.setColor( onColor );
		drawCurrentSeg(g, true);
		g.setColor( color );
	}
	
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
		g.setColor( on ? onColor : offColor );

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

//				
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
	

}

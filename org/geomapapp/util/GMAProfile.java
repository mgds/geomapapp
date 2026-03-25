package org.geomapapp.util;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.MouseInputAdapter;

import org.geomapapp.geom.XYZ;
import org.geomapapp.gis.shape.ESRIShapefile;
import org.geomapapp.grid.Grid2DOverlay;

import haxby.db.mb.PreviewCruise.CruiseGridViewer;
import haxby.db.mb.PreviewGrids.GridViewer;
import haxby.map.MapApp;
import haxby.map.Overlay;
import haxby.map.XMap;
import haxby.proj.Projection;
import haxby.util.DisplayUtil;
import haxby.util.GeneralUtils;
import haxby.util.LayerManager.LayerPanel;
import haxby.util.ToggleableMouseInputAdapter;
import haxby.util.URLFactory;


public class GMAProfile implements Overlay, XYPoints {
	XMap map;
	Vector<float[]> xyz;
	Vector[] xyzs;
	XYGraph graph;
	Grid2DOverlay[] grids;
	ArrayList<XYGraph> graphs;
	MouseInputAdapter mouse;
	static boolean enabled;
	JFrame dialog;
	boolean reversed = false;
	double spacing;
	Vector savedProfiles;
	long start = 0L;
	Point p0;
	Line2D currentLine,
			line;
	GeneralPath currentPath;
	GeneralPath path;
	Point2D[] currentPts;
//JSlider verticalExgSlider;
	JTextField setStartLatT,
				setStartLonT;
	JTextField setEndLonT;
	JTextField setEndLatT;
	Point2D lastP1,
			lastP2;
	JComboBox startLatDropdown,
				startLonDropdown,
				endLatDropdown,
				endLonDropdown,
				degreesOrMinutes;
	JRadioButton standardUnits,
					metricUnits;
	JToggleButton multiPlotBtn = new JToggleButton();
	JLabel xScaleLabel;
	JLabel[] yScaleLabels;
	JTextField[] verticalExgTs;
	Point2D previousPoint = null; // ***** GMA 1.5.2: TESTING
	String ProfileToolURL = MapApp.BASE_URL+"/gma_html/help/profile_tool_help.html";
	JPanel graphPanel;
	String gridName = "";
	Grid2DOverlay grid = null;
	ArrayList<Grid2DOverlay> gridsToPlot = new ArrayList<Grid2DOverlay>();
	
	public GMAProfile( final XMap map ) {
		this.map = map;
	}
	
	void drawLine() {
		if( currentPath==null )return;
		double wrap = map.getWrap();
		synchronized(map.getTreeLock()) {
			Graphics2D g = map.getGraphics2D();
			g.setStroke( new BasicStroke(2f/(float)map.getZoom()) );
			//g.setColor( Color.red );
			g.setXORMode(Color.white);
			g.draw( currentPath );
			if( wrap>0.) {
				g.translate(wrap,0.);
				g.draw( currentPath );
			}
		}
	}
	void begin(MouseEvent e) {
		start = System.currentTimeMillis();
		p0 = e.getPoint();
	}
	void drag(MouseEvent e) {
		if (p0==null) { begin(e); return; }
		drawLine();
		Point2D p = map.getScaledPoint(e.getPoint());
		currentLine = new Line2D.Double(map.getScaledPoint(p0), p);
		Point2D[] pts = getPath(currentLine, 5);
		currentPath =  getGeneralPath(pts);
		drawLine();
		java.text.NumberFormat fmt = java.text.NumberFormat.getInstance();
		fmt.setMaximumFractionDigits(2);

		map.setXY(e.getPoint());
		map.getMapTools().setInfoText( map.getMapTools().getInfoText() + ", distance = " +
				fmt.format(distance(pts)) +" km");
	}

	double distance(Point2D pt1, Point2D pt2) {
		return distance( new Point2D[] { pt1, pt2});
	}
	double distance(Point2D[] pts) {
		if( pts==null || pts.length == 0) return 0.;
		//System.out.println(pts[0].);
		//TODO figure out how pts works - here is lon,lat to XYZ

		XYZ r1 = XYZ.LonLat_to_XYZ(pts[0]);
		XYZ r2 = XYZ.LonLat_to_XYZ(pts[pts.length-1]);

		//map.getProjection().

		double angle = Math.acos( r1.dot(r2) );
		return Projection.major[0]*angle/1000.;
	}
	GeneralPath getGeneralPath(Point2D[] pts) {
		Projection proj = map.getProjection();
		GeneralPath path = new GeneralPath();
		float[] lastP = null;
		float wrap = (float)map.getWrap()/2f;

		boolean tf = profileTypes != null && straightLine.isSelected();


		for( int k=0 ; k<pts.length ; k++) {

			Point2D p = proj.getMapXY( pts[k] );
			float x = (float)p.getX();
			float y = (float)p.getY();
			//System.out.println("wrap " + wrap);
			//System.out.println("" + (lastP!=null));

			if( lastP!=null && wrap>0f ) {
				while( x-lastP[0] <-wrap ){x+=wrap*2f;}
				while( x-lastP[0] > wrap ){x-=wrap*2f;}
			}
			lastP = new float[] {x, y};
			if( k==0 ) path.moveTo( x, y );
			else path.lineTo( x, y );
		}
		return path;
	}
	Point2D[] getPath(Line2D l, int dec) {

/*
		if( cb !=null &&
			cb.getSelectedItem().toString().startsWith("S") )return getStraightPath(l, dec);
*/
		if( profileTypes != null && straightLine.isSelected() )return getStraightPath(l, dec);
		Point2D p1 = l.getP1();
		Point2D p2 = l.getP2();
		double dist = map.getZoom()*Math.sqrt(
			Math.pow( p1.getX()-p2.getX(),2 ) +
			Math.pow( p1.getY()-p2.getY(),2 )); 
		int npt = (int)Math.ceil( dist/dec);
		Projection proj = map.getProjection();
		Point2D q1 = proj.getRefXY(p1);
		Point2D q2 = proj.getRefXY(p2);
		XYZ r1 = XYZ.LonLat_to_XYZ(q1);
		XYZ r2 = XYZ.LonLat_to_XYZ(q2);
		double angle = Math.acos( r1.dot(r2) )/(npt-1.);
		r2 = r1.cross(r2).cross(r1).normalize();
		Point2D[] path = new Point2D[npt];
		for( int k=0 ; k<npt ; k++) {
			double s = Math.sin(k*angle);
			double c = Math.cos(k*angle);
			XYZ r = r1.times(c).plus( r2.times(s) );
			path[k] = r.getLonLat();
		}
		//TODO set current pts to path
		currentPts = path;

		return path;
	}

	Point2D[] getStraightPath(Line2D l, int dec) {
		Point2D p1 = l.getP1();
		Point2D p2 = l.getP2();

		lastP1 = p1;
		lastP2 = p2;

		Projection proj = map.getProjection();
		double dist = map.getZoom()*Math.sqrt(
			Math.pow( p1.getX()-p2.getX(),2 ) +
			Math.pow( p1.getY()-p2.getY(),2 )); 
		int npt = (int)Math.ceil( dist/dec);
		if( npt<2 )npt=2;

		Point2D[] path = new Point2D[npt];
		double dx = (p2.getX()-p1.getX())/(npt-1.);
		double dy = (p2.getY()-p1.getY())/(npt-1.);


		for( int k=0 ; k<npt ; k++) {
			path[k] = proj.getRefXY(new Point2D.Double(
				p1.getX() + k*dx,
				p1.getY() + k*dy));
		}
		//TODO set current pts to path
		currentPts = path;
		return path;
	}
	void finish(MouseEvent e) {
		if (p0==null) {
			return;
		}
		drawLine();
		Point p1 = e.getPoint();
		if( p0.x==p1.x && p0.y==p1.y ) {
			if( line!=null ) {
				line=null;
				if(dialog!=null)dialog.setVisible(false);
				map.repaint();
			}
			p0=null;
			return;
		}
		if( System.currentTimeMillis()-start<250L) {
			start = 0L;
			currentLine = null;
			if(dialog!=null)dialog.setVisible(false);
			p0=null;
			return;
		}
		map.moveOverlayToFront(this); // Keep us on top!

		start = 0L;
		Point2D pA = map.getScaledPoint( p0 );

		//TODO
		//System.out.println("pA X " + pA.getX() + " pA Y " + pA.getY());
		Point2D pB = map.getScaledPoint( e.getPoint());
		line = new Line2D.Double(pA, pB);
		doProfile((Line2D.Double)line);
		if (dialog != null) {
			setStartLonT.setText(getLon(currentPts[0],startLonDropdown));
			setStartLatT.setText(getLat(currentPts[0],startLatDropdown));
	
			setEndLonT.setText(getLon(currentPts[currentPts.length-1],endLonDropdown));
			setEndLatT.setText(getLat(currentPts[currentPts.length-1],endLatDropdown));
	
			p0=null;
		}
	}
	void lineChanged() {
		doProfile( (Line2D.Double)line);
	}
	void doProfile(Line2D.Double line) {
		Point2D[] pts = getPath(line, 5);
		path =  getGeneralPath(pts);
		currentLine = null;
		currentPath = null;
		ArrayList<Grid2DOverlay> prevGrids = new ArrayList<Grid2DOverlay>(gridsToPlot);
		gridsToPlot.clear();
		map.repaint();
		
		//get list of grids that have had their Plot Profile box 
		//checked in the Layer Manager.  Only take the first 4.
		MapApp app = (MapApp)map.getApp();
		List<LayerPanel> layerPanels = app.layerManager.getLayerPanels();
		for (LayerPanel layerPanel : layerPanels) {
			if (layerPanel.doPlotProfile && layerPanel.isVisible()) {
				Grid2DOverlay thisGrid = null;
				if (layerPanel.layer instanceof Grid2DOverlay) {
					thisGrid = (Grid2DOverlay) layerPanel.layer;
				} else if (layerPanel.layer instanceof ESRIShapefile) {
					ESRIShapefile esf = (ESRIShapefile)(layerPanel.layer);
					if (esf.getMultiGrid() != null) thisGrid = esf.getMultiGrid().getGrid2DOverlay();
				}
				if (thisGrid != null) {
					if (gridsToPlot.size() < 4 && !gridsToPlot.contains(thisGrid)) gridsToPlot.add(thisGrid);
				}
			}
		}
		
		//if no grid to plot, set dialog to null and return
		if (gridsToPlot.size() == 0) {
			if (dialog != null) dialog.dispose();
			return;
		}
		
		//redraw dialog if the list of grids to plot has changed
		if (!gridsToPlot.equals(prevGrids)) {
			if (dialog != null) dialog.dispose();
			graphs = null;
		}

		if (gridsToPlot.size() > 0) grid = gridsToPlot.get(0);

		if (multiPlotBtn.isSelected()) {
			if (graphs == null) {
				graphs = new ArrayList<XYGraph>();
				GMAProfile[] newPoints = new GMAProfile[gridsToPlot.size()]; 
				
				for( int k=0 ; k<gridsToPlot.size() ; k++) {
					newPoints[k] = getNewPoints(gridsToPlot.get(k));
				}
				graph = new XYMultiGraph(newPoints, 0);
				graph.setAxesSides( Axes.LEFT | Axes.BOTTOM );
				graph.addMouseMotionListener(new MouseMotionAdapter() {
					public void mouseMoved(MouseEvent e) {
						setLoc( e );
					}
				});
				graph.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent e) {
						if( e.isControlDown() )return;
						recenter( e );
					}
					public void mouseExited(MouseEvent e) {
						drawArc(null);
						arc = null;
						arc2 = null;
					}
				});
				Zoomer z = new Zoomer(graph);
				graph.addMouseListener(z);
				graph.addKeyListener(z);
				graphs.add((XYMultiGraph) graph);
				
				if (dialog == null || !dialog.isVisible()) {
					dialog = new JFrame("Profile");
					dialog.setDefaultCloseOperation( dialog.HIDE_ON_CLOSE);
					dialog.addWindowListener(new WindowAdapter() {
						public void windowClosing(WindowEvent e) {
							multiPlotBtn.setSelected(true);
							graphs = null;
							path = null;
							map.repaint();
						}
					});
					// make sure multiplot is set to true
					multiPlotBtn.doClick();
					multiPlotBtn.doClick();
				}
				
				
			} else {
				GMAProfile[] points = (GMAProfile[]) ((XYMultiGraph)graph).xyArray;
				for (int i=0; i<gridsToPlot.size(); i++) {
					if (autoYs[i].isSelected()) {
						points[i] = getNewPoints(gridsToPlot.get(i));
					}
				}
				((XYMultiGraph)graph).setPoints(points, 0);
			}
		} else {		
			if(graphs==null ) {
				graphs = new ArrayList<XYGraph>();
				
				for( int k=0 ; k<gridsToPlot.size() ; k++) {
					GMAProfile newPoints = getNewPoints(gridsToPlot.get(k));
					if (newPoints == null) continue;
					graph = new XYGraph(newPoints, 0);
	
					graph.setAxesSides( Axes.LEFT | Axes.BOTTOM );
					graph.addMouseMotionListener(new MouseMotionAdapter() {
						public void mouseMoved(MouseEvent e) {
							setLoc( e );
						}
					});
					graph.addMouseListener(new MouseAdapter() {
						public void mouseClicked(MouseEvent e) {
							if( e.isControlDown() )return;
							recenter( e );
						}
						public void mouseExited(MouseEvent e) {
							drawArc(null);
							arc = null;
							arc2 = null;
						}
					});
					Zoomer z = new Zoomer(graph);
					graph.addMouseListener(z);
					graph.addKeyListener(z);
					graphs.add(graph);
				}
				
				//if not all gridsToPlot are fully loaded, reset to prevGrids
				if (graphs.size() != gridsToPlot.size()) {
					gridsToPlot = prevGrids;
				}
				if (dialog == null || !dialog.isVisible()) {
					dialog = new JFrame("Profile");
					dialog.setDefaultCloseOperation( dialog.HIDE_ON_CLOSE);
					dialog.addWindowListener(new WindowAdapter() {
						public void windowClosing(WindowEvent e) {
							multiPlotBtn.setSelected(false);
							graphs = null;
							path = null;
							map.repaint();
						}
					});

					graphPanel = new JPanel();
					graphPanel.setLayout(new GridLayout(graphs.size(),1));
					for (XYGraph plotGraph : graphs) {
						if (plotGraph != null) graphPanel.add(plotGraph);
					}
					
					dialog.getContentPane().add( graphPanel, "Center" );
					dialog.getContentPane().add( getTools(), "North" );
					dialog.pack();
		//			dialog.setSize(650, 870);
					dialog.setSize(650, 170 + graphPanel.getHeight());
				}
	
			} else {
				for (int i=0; i<graphs.size(); i++) {
					if (autoYs[i].isSelected()) {
						GMAProfile newPoints = getNewPoints(gridsToPlot.get(i));
						graphs.get(i).setPoints(newPoints, 0);
					}
				}
			}
		}
		if(!dialog.isVisible()) {
			DisplayUtil.setRelativeLocation(dialog, 0, 150, MapApp.anchor);
			dialog.setVisible(true);
		}
		dialog.toFront();

		int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
		DecimalFormat fmt = new DecimalFormat("#.#");
		xScaleT.setText(""+ fmt.format(dpi/graph.xScale));
		double xExg = 1000*(dpi/graph.xScale);
		double yExg, vExg;

		for (int i=0; i<graphs.size(); i++) {
			graphs.get(i).repaint();
			yScaleTs[i].setText(""+ (fmt.format((-1)*dpi/graphs.get(i).yScale)));
			yExg = (-1)*(dpi/graphs.get(i).yScale);
			vExg = xExg/yExg;
			verticalExgTs[i].setText(fmt.format(vExg));
		}
	}

	/*
	 * Create new profile points to be plotted in a graph
	 */
	private GMAProfile getNewPoints(Grid2DOverlay gridToPlot) {
		GMAProfile newPoints = new GMAProfile(map);
		
		Vector<float[]> newXyz = new Vector<float[]>(); 
		if( gridToPlot.getGrid()==null ) {
			return null;
		}
		Point2D[] pts = getPath(line, 1);
		double dx = distance(pts);
		newPoints.xScale = 800./dx;

		boolean tf = profileTypes != null && straightLine.isSelected();

		if(tf) {
			Point2D p1 = line.getP1();
			Point2D p2 = line.getP2();
			dx = Math.sqrt(
				Math.pow( p1.getX()-p2.getX(),2 ) +
				Math.pow( p1.getY()-p2.getY(),2 )); 
		}
		dx /= pts.length-1.;
		newPoints.spacing = dx*2.5;
		boolean beginX = true;
		boolean beginY = true;
		float x = 0f;
		float distance = 0;
		for( int k=0 ; k<pts.length ; k++) {
			if(k!=0){
				distance+=distance(pts[k],pts[k-1]);
			}
			double z = gridToPlot.valueAt(pts[k]);
			if( tf && k!= 0) x += (float)distance(pts[k-1], pts[k]);

			//if( Double.isNaN(z) )continue;
			if( !tf ) {
				newXyz.add( new float[] {
					distance,
					(float)z,
					(float)pts[k].getX(),
					(float)pts[k].getY()});
				if( beginX ) {
					beginX = false;
					newPoints.xRange[0] = dx*k;
				}
				if ( beginY ) {
					if( !Double.isNaN(z) ) {
						beginY = false;
						newPoints.yRange[0] = newPoints.yRange[1] = z;
					}
				}
				newPoints.xRange[1] = dx*k;
				if( !Double.isNaN(z) ) {
					if (z>newPoints.yRange[1]) newPoints.yRange[1] = z;
					if (z<newPoints.yRange[0]) newPoints.yRange[0] = z;
				}
			} else {
				newXyz.add( new float[] {
					(float)(dx*k),
					(float)z,
					(float)pts[k].getX(),
					(float)pts[k].getY(),
					x });
				if( beginX ) {
					beginX = false;
					newPoints.xRange[0] = x;
				}
				if (beginY) {
					if( !Double.isNaN(z) ) {
						beginY = false;
						newPoints.yRange[0] = newPoints.yRange[1] = z;
					}
				}
				newPoints.xRange[1] = x;
				if( !Double.isNaN(z) ) {
					if(z>newPoints.yRange[1])newPoints.yRange[1] = z;
					if(z<newPoints.yRange[0])newPoints.yRange[0] = z;
				}
			}
		}
	
		dx = .05*(newPoints.xRange[1]-newPoints.xRange[0]);
		newPoints.xRange[0] -= dx;
		newPoints.xRange[1] += dx;
		dx = .05*(newPoints.yRange[1]-newPoints.yRange[0]);
		newPoints.yRange[0] -= dx;
		newPoints.yRange[1] += dx;
		if (newPoints.yRange[1] == newPoints.yRange[0]) {
			if (newPoints.yRange[0] == 0) {
				newPoints.yRange[0] = -1;
				newPoints.yRange[1] = 0.5;
			}
			else {
				newPoints.yRange[0] = 0;
				newPoints.yRange[1] *= 1.5;
			}
			 
		}
		newPoints.yScale = 250./(newPoints.yRange[1]-newPoints.yRange[0]);
		// make some room for Grid name label at the bottom
		double graphHeight = 250.;
		if (graphPanel != null && graphPanel.getHeight() > 0) {
			graphHeight = graphPanel.getHeight() / gridsToPlot.size();
		}
		double ys = graphHeight / (newPoints.yRange[1]-newPoints.yRange[0]);
		double textHeight = 30 / ys;
		newPoints.yRange[0] -= textHeight;
		
		newPoints.xScale = 800./(newPoints.xRange[1]-newPoints.xRange[0]);
		newPoints.xyz = newXyz;
		newPoints.gridName = gridToPlot.getName();
		newPoints.grid = gridToPlot;
		newPoints.profileTypes = profileTypes;
		newPoints.straightLine = straightLine;

		return newPoints;
	}
	
	
	void recenter(MouseEvent e) {

		boolean tf = profileTypes != null && straightLine.isSelected();
		int index = tf ? 4 : 0;

		double x = graphs.get(0).getXAt( e.getPoint() );

		for (XYGraph thisGraph : graphs) {
			thisGraph.xRange[0] -= x;
			thisGraph.xRange[1] -= x;
			
			if (this.multiPlotBtn.isSelected()) {
				for (XYPoints thisXy : ((XYMultiGraph)thisGraph).xyArray) {
					Vector<float[]> thisXyz = ((GMAProfile)thisXy).xyz;
					
					for( int k=0 ; k<thisXyz.size() ; k++) {
						float[] xy = (float[])thisXyz.get(k);
						xy[index] -= (float)x;
					}
				}
				
			} else {
				Vector<float[]> thisXyz = ((GMAProfile)thisGraph.xy).xyz;
				
				for( int k=0 ; k<thisXyz.size() ; k++) {
					float[] xy = (float[])thisXyz.get(k);
					xy[index] -= (float)x;
				}
			}
			thisGraph.repaint();
		}
	}
	
	Arc2D.Double arc=null;
	Arc2D.Double arc2=null;
	void drawArc( Color inputColor ) {
		if( arc==null )return;
		double wrap = map.getWrap();
		double zoom = map.getZoom();
		synchronized(map.getTreeLock()) {
			Graphics2D g = map.getGraphics2D();
			Graphics2D g2 = map.getGraphics2D();

//			***** GMA 1.5.2: TESTING
			g2.setStroke( new BasicStroke(2f/(float)map.getZoom()) );
			g2.setXORMode(Color.white);
			g.setStroke( new BasicStroke(2.f/(float)map.getZoom()) );
			if ( inputColor == null ) {
				g.setColor( Color.white );
			} else {
				g.setColor( inputColor );
			}
//			***** GMA 1.5.2

			g.draw( arc );
			g2.draw(arc2);
			if( wrap>0.) {
				g.translate(wrap,0.);
				g2.translate(wrap,0.);
				g.draw( arc );
				g2.draw(arc2);
				g.translate(-2.*wrap,0.);
				g2.translate(-2.*wrap,0.);
//				g.draw( arc );
			}

/*			g.draw( arc );
			if( wrap>0.) {
				g.translate(wrap,0.);
				g.draw( arc );
				g.translate(-2.*wrap,0.);
				g.draw( arc );
			}*/
		}
	}
	void setLoc(MouseEvent e) {
		drawArc(null);
		arc = null;
		arc2 = null;
		xyz  = ((GMAProfile)graphs.get(0).xy).xyz; 
		if( xyz==null || xyz.size()<2 )return;
		float x = (float)graph.getXAt( e.getPoint() );
		float[] xy = (float[])xyz.get(0);
		float[] last = xy;
		
		boolean tf = profileTypes != null && straightLine.isSelected();
		int index = tf ? 4 : 0;
		
		if( xy[index]>x )return;
		for( int k=0 ; k<xyz.size() ; k++) {
			xy = (float[])xyz.get(k);
			if( xy[index]>x ) {
				float dx = (x-last[index])/(xy[index]-last[index]);
				double lon = (double)(last[2]+(xy[2]-last[2])*dx);
				double lat = (double)(last[3]+(xy[3]-last[3])*dx);
				map.setLonLat( lon, lat);
				//System.out.println("lat "+ lat + " lon "+lon);

				//TODO converts a longitude latitude double pair into a string, reverse this op
				if ( lonLat != null ) {
					String tempLonLat = map.getLonLat();
					tempLonLat = "   " + tempLonLat;
					//tempLonLat = tempLonLat.replaceAll(", ", ",  ");
					lonLat.setText(tempLonLat);
				}

				Point2D p = map.getProjection().getMapXY(
						new Point2D.Double(lon,lat));
				double z = map.getZoom();

//				***** GMA 1.5.2: TESTING
//				arc = new Arc2D.Double(p.getX()-6/z, p.getY()-6/z,
//						12./z,12./z, 0., 360., Arc2D.CHORD);
				arc = new Arc2D.Double(p.getX(), p.getY(),
						2./z,2./z, 0., 360., Arc2D.CHORD);
				arc2 = new Arc2D.Double(p.getX()-6/z, p.getY()-6/z,
						12./z,12./z, 0., 360., Arc2D.CHORD);
//				***** GMA 1.5.2
				drawArc( Color.RED );
				return;
			}
			last = xy;;
		}
	}
	public void draw(Graphics2D g) {
		arc = null;;
		arc2 = null;;
		if( path==null)return;
		AffineTransform at = g.getTransform();
		if( !enabled || line==null) return;
		g.setStroke( new BasicStroke(4.f/(float)map.getZoom()) );
		g.setColor(Color.white);
		g.draw( path );
		double wrap = map.getWrap();
		if( wrap>0.) {
			g.translate(wrap, 0.);
			g.draw( path );
		}
		g.setTransform(at);
	}

	public void setEnabled(boolean tf) {
		if( tf==enabled )return;
		enabled = tf;
		
		//toggle the visibility of the Plot Profile checkboxes for each layerPanel in the Layer Manager
		MapApp app = (MapApp)map.getApp();
		app.layerManager.displayPlotProfileCheckBoxes(enabled);
		
		if( !enabled ) {
			if(mouse==null)return;
			//remove the mouse listeners for this Profile dialog
			map.removeMouseListener(mouse);
			map.removeMouseMotionListener(mouse);
			
			//enable the mouse listeners for any remaining Profile dialog
			MouseListener[] mouseListeners = map.getMouseListeners();
			for (MouseListener ml : mouseListeners) {
				if (ml.getClass().equals(mouse.getClass())) {
					ToggleableMouseInputAdapter tml = (ToggleableMouseInputAdapter)ml;
					tml.setMouseListenerIsActive(!ml.equals(mouse));
				}
			}
			
			if( dialog!=null ) dialog.dispose();
			if( path!=null ) {
				path = null;
				map.repaint();
			}
		} else {
			initMouse();
			
			//Enable the mouse listeners for the new Profile dialog, and disable for any pre-existing ones
			MouseListener[] mouseListeners = map.getMouseListeners();
			for (MouseListener ml : mouseListeners) {
				if (ml.getClass().equals(mouse.getClass())) {
					ToggleableMouseInputAdapter tml = (ToggleableMouseInputAdapter)ml;
					tml.setMouseListenerIsActive(ml.equals(mouse));
				}
			}
			
			//map.removeMouseListener(mouse);
			//map.removeMouseMotionListener(mouse);
			map.addMouseListener(mouse);
			map.addMouseMotionListener(mouse);
		}
	}
	
	void initMouse() {
		map.addOverlay(this, false);
		Grid2DOverlay grid = map.getFocus();
		mouse = new ToggleableMouseInputAdapter(grid) {
			
			public void mouseClicked(MouseEvent evt) {
				if (isMouseListenerIsActive()) {
					if( dialog!=null )dialog.setVisible(false);
					if( path==null)return;
					path = null;
					map.repaint();
				}
			}
			public void mousePressed(MouseEvent evt) {
				if( evt.isControlDown())return;
				//begin(evt);
			}
			public void mouseReleased(MouseEvent evt) {
				if( evt.isControlDown())return;
				if (isMouseListenerIsActive()) {
					finish(evt);
				}
			}
			public void mouseDragged(MouseEvent evt) {
				if( evt.isControlDown())return;
				if (isMouseListenerIsActive()) {
					drag( evt);
				}
			}
		};
	}
	public String getXTitle(int dataIndex) {
		if (dataIndex == 0) 
			return "Distance, km";
		else return "";
	}
	public String getYTitle(int dataIndex) {
		return grid.getDataType() + ", " + grid.getUnits();
	}
	double[] xRange = new double[] {0.,1000.};
	double[] yRange = new double[] {0.,100.};
	double xScale = 1.;
	double yScale = 1.;
	public double[] getXRange(int dataIndex) {
		return xRange;
	}
	public double[] getYRange(int dataIndex) {
		return yRange;
	}
	public double getPreferredXScale(int dataIndex) {
		return xScale;
	}
	public double getPreferredYScale(int dataIndex) {
		return yScale;
	}
	
	public void plotXY( Graphics2D g,
				Rectangle2D bounds,
				double xScale, double yScale,
				int dataIndex) {
		GeneralPath path = new GeneralPath();
		float x0 = (float)bounds.getX();
		float x1 = x0+(float)bounds.getWidth();
		float y0 = (float)bounds.getY();
		float xs = (float)xScale;
		float ys = (float)yScale;
		float[] data;
		int k1, k2;
		k1 = 0;
		k2 = -1;
		Color[] colors = {Color.BLACK, Color.RED, Color.BLUE, Color.GREEN};
		
		//If the profile doesn't cross the active grid, display a message in the plot
		if( getNumValidZ() == 0) {
			g.setColor( Color.red );
			Font font = new Font("TimesRoman", Font.BOLD, 20); 
			GeneralUtils.drawCenteredString(g, "Profile line does not intersect grid", bounds, font, xScale, yScale, true);
			//add the grid name in the lower left corner
			g.setColor( Color.black );
			font = new Font("TimesRoman", Font.BOLD, 15); 
			GeneralUtils.drawLowerLeftString(g, gridName, bounds, font, xScale, yScale, true, dataIndex);
			return;
		}

/*
		boolean tf =  cb !=null &&
			cb.getSelectedItem().toString().startsWith("S");
*/
		boolean tf = profileTypes != null && straightLine.isSelected();
		int i = tf ? 4 : 0;
		for( int k=0 ; k<xyz.size() ; k++) {
			data = (float[])xyz.get(k);
			if( data[i]<x0 )k1=k;
			if( data[i]<=x1 ) {
				k2 = k+1;
			}
		}
		if( k2>=xyz.size() )k2=xyz.size()-1;
		if( k2==-1 ) return;
		
		data = (float[])xyz.get(k1);
		path.moveTo( xs*(data[i]-x0), ys*(data[1]-y0) );
		float x = data[0];
		boolean gap = false;
		for( int k=k1+1 ; k<=k2 ; k++) {
			data = (float[])xyz.get(k);
	
			if( Float.isNaN(data[1])) {
				//leave a gap when the y-value is NaN
				gap=true;
				continue;
			}
			
			if( data[0]-x>spacing) path.moveTo( xs*(data[i]-x0), ys*(data[1]-y0) );
			
			if (gap) {
				path.moveTo( xs*(data[i]-x0), ys*(data[1]-y0) );
				gap = false;
			}
			else { 
				path.lineTo( xs*(data[i]-x0), ys*(data[1]-y0) );
			}
			x = data[0];
		}
		g.setColor( Color.white );
		g.setStroke( new BasicStroke(4f)); // Background white shadow to graph values
		g.draw(path);
		g.setColor( colors[dataIndex] );
		g.setStroke( new BasicStroke(2f)); 
		g.draw(path);
		
		//add the grid name in the lower left corner
		Font font = new Font("TimesRoman", Font.BOLD, 15); 
		GeneralUtils.drawLowerLeftString(g, gridName, bounds, font, xScale, yScale, true, dataIndex);
	}

	/*
	 * Return whether the multiplot button should be displayed
	 * For now, this is only available when looking at the development
	 * and GMRT released grid using PreviewGrids of PreviewCruise
	 */
	public Boolean isMultiPlot() {
		int count = 0;
		for (Grid2DOverlay thisGrid : gridsToPlot) {
			if (thisGrid instanceof CruiseGridViewer || thisGrid instanceof GridViewer) count ++;
		}
		return (count >= 1);
	}
	
	
	/*
	 * Return the number of non-NaN z-values
	 */
	private int getNumValidZ() {
		int count = 0;
		for (float[] thisXyz : xyz) {
			if (!Float.isNaN(thisXyz[1])) count++; 
		}
		return count;
	}

	void rescale() {
		int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
		graphPanel.removeAll();
		for (int i=0; i<graphs.size(); i++) {
			XYGraph thisGraph = graphs.get(i);
			if( autoX.isSelected() ) {
				thisGraph.setScrollableTracksViewportWidth(true);
				xScaleT.setEditable(false);
				System.out.println("graph y scale "+dpi/thisGraph.yScale);
				System.out.println("graph x scale "+dpi/thisGraph.xScale);
			} else {
				thisGraph.setScrollableTracksViewportWidth(false);
				try {
					Double.parseDouble(xScaleT.getText());
				} catch(Exception ex) {
					xScaleT.setText( Double.toString(xScale) );
				}
				thisGraph.xScale = dpi / Double.parseDouble(xScaleT.getText());
				xScaleT.setEditable(true);
			}
			thisGraph.setZoom(1.);
			if( autoYs[i].isSelected() ) {
				thisGraph.setScrollableTracksViewportHeight(true);
				yScaleTs[i].setEditable(false);
				System.out.println("graph y scale "+dpi/thisGraph.yScale);
				System.out.println("graph x scale "+dpi/thisGraph.xScale);
			} else {
				thisGraph.setScrollableTracksViewportHeight(false);
				try {
					Double.parseDouble(yScaleTs[i].getText());
				} catch(Exception ex) {
					yScaleTs[i].setText( Double.toString(yScale) );
				}
				thisGraph.yScale =-1 * dpi / Double.parseDouble(yScaleTs[i].getText());
				yScaleTs[i].setEditable(true);
			}

			if( !autoX.isSelected() || !autoYs[i].isSelected()) {
				JScrollPane sp = new JScrollPane(thisGraph);
				graphPanel.add(sp);
			} else {
				graphPanel.add(thisGraph);
			}
			
			thisGraph.revalidate();
			DecimalFormat fmt = new DecimalFormat("#.#");
			xScaleT.setText(""+fmt.format(dpi/thisGraph.xScale));
			yScaleTs[i].setText(""+ (fmt.format((-1)*dpi/thisGraph.yScale)));
	
			double xExg = 1000*(dpi/thisGraph.xScale);
			double yExg = (-1)*(dpi/thisGraph.yScale);
			double vExg = xExg/yExg;
	
			verticalExgTs[i].setText(fmt.format(vExg));
			dialog.setVisible(true);
			thisGraph.repaint();
		}
	}
	
	JToggleButton saveJPEG;
	JToggleButton savePNG;
	JToggleButton saveToFile;
	JToggleButton saveToClipboard;
	JToggleButton print;
	JMenu getSaveMenu() {
		initSave();
		JMenu fileMenu = new JMenu("Save");
		fileMenu.setToolTipText("saves image or xyz's of profile");
		JMenuItem mi = new JMenuItem("Copy to clipboard");
		mi.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveToClipboard.doClick();
				save();
			}
		});
		fileMenu.add(mi);
		mi = new JMenuItem("Save JPEG image");
		mi.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveJPEG.doClick();
				save();
			}
		});
		fileMenu.add(mi);
		mi = new JMenuItem("Save PNG image");
		mi.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				savePNG.doClick();
				save();
			}
		});
		fileMenu.add(mi);
		mi = new JMenuItem("Save ASCII table");
		mi.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveToFile.doClick();
				save();
			}
		});
		fileMenu.add(mi);
		mi = new JMenuItem("Print");
		mi.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				print.doClick();
				save();
			}
		});
		fileMenu.add(mi);
		return fileMenu;
	}

	// Help Menu
	JMenu getHelpMenu() {
		JMenu helpMenu = new JMenu("Help");
		helpMenu.setToolTipText("help");
		JMenuItem mi = new JMenuItem("How To...");
		mi.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String helpText = "";
				String s = "";
				URL helpURL = null;
				try {
					helpURL = URLFactory.url(ProfileToolURL);
					BufferedReader in = new BufferedReader( new InputStreamReader( helpURL.openStream() ) );

					while((s=in.readLine())!=null){
						helpText = helpText.concat(s);
					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				JOptionPane.showMessageDialog(MapApp.anchor, helpText, "Help", JOptionPane.PLAIN_MESSAGE);
			}
		});
		helpMenu.add(mi);
		return helpMenu;
	}

	JCheckBox autoX;
	JTextField xScaleT;
	JTextField[] yScaleTs;
	JCheckBox[] autoYs;
	JComboBox cb;

//	1.3.5: Have radio buttons to select between line types
//	instead of a combo box
	ButtonGroup profileTypes;
	ButtonGroup unitTypes;
	JRadioButton greatCircle;
	JRadioButton straightLine;
	JLabel lonLat;

	JPanel getTools() {
		int nGraphs = graphs.size();
		yScaleTs = new JTextField[nGraphs];
		yScaleLabels = new JLabel[nGraphs];
		autoYs = new JCheckBox[nGraphs];
		verticalExgTs = new JTextField[nGraphs];
		
		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent  e) {
				rescale();
				if(autoX.isSelected()) {
					for (JCheckBox autoY : autoYs) {
						if(autoY.isSelected()) {
							doProfile((java.awt.geom.Line2D.Double) line);	
							break;
						}
					}
				}
			}
		};
		//action listener for Vertical Exageration textfield
		ActionListener veActionListener = new ActionListener(){
			public void actionPerformed(ActionEvent e){
				int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
				double xExg = (dpi/graph.xScale);
				double newY = (1.0/Double.parseDouble(((JTextField)e.getSource()).getText())) * xExg * (1000.0);
				autoX.setSelected(false);
				xScaleT.setEditable(true);
				
				//find the Y-scale line for VE textfield selected
				Container yLine =  ((Container)e.getSource()).getParent() ;
				for (int i=0; i<graphs.size(); i++) {
					if ( ((Container)autoYs[i]).getParent() == yLine ) autoYs[i].setSelected(false);
					if ( ((Container)yScaleTs[i]).getParent() == yLine ) {
						yScaleTs[i].setEditable(true);
						yScaleTs[i].setText(""+newY);
					}
				}
				rescale();
			}
		};
		
		JPanel tools = new JPanel(new BorderLayout());
		JPanel panel = new JPanel(new FlowLayout( FlowLayout.LEFT, 0, 0));
		//panel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		JPanel panel1 = new JPanel(new BorderLayout());
		JMenuBar bar = new JMenuBar();

		bar.add(getSaveMenu());
		JSeparator sep1 = new JSeparator(JSeparator.VERTICAL);
		bar.add(sep1, "separator"); // Add menu separator
		bar.add(getHelpMenu());
		bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Color.GRAY));
		//bar.set
		panel.add(bar); // Add menu

//		***** Changed by A.K.M. 1.3.5 *****
		//panel1.add(cb, "West");
//		Add radio buttons instead of combo box to the 
//		profile window
		standardUnits = new JRadioButton("Imperial Units");
		standardUnits.setToolTipText("Select Imperial Units");
		metricUnits = new JRadioButton("Metric Units");
		metricUnits.setToolTipText("Select Metric Units");
		metricUnits.setVisible(false);
		greatCircle = new JRadioButton("Great Circle");
		greatCircle.setToolTipText("Always draws shortest path.");
		straightLine = new JRadioButton("Straight Line");
		straightLine.setToolTipText("Always draws between points.");
		lonLat = new JLabel("");
		lonLat.setFont(new Font("Serif", Font.PLAIN, 13));
		profileTypes = new ButtonGroup();
		profileTypes.add(greatCircle);
		profileTypes.add(straightLine);
		unitTypes = new ButtonGroup();
		unitTypes.add(standardUnits);
		unitTypes.add(metricUnits);

		final DecimalFormat frmt = new DecimalFormat("#.#");

		standardUnits.addActionListener( new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if(xScaleLabel.getText().contains("in."))
					return;	

				//graph.axes.res[0] = graph.axes.res[0]/2.54;
				//graph.axes.res[2] = graph.axes.res[2]/2.54;

				graph.invalidate();
				graph.repaint();

				xScaleLabel.setText("km/in.");
				double xScaleVal = Double.parseDouble(xScaleT.getText());
				xScaleVal = (2.54)*xScaleVal;
				xScaleT.setText(frmt.format(xScaleVal));
				for (int i=0; i<graphs.size(); i++) {
					yScaleLabels[i].setText(graphs.get(i).axes.xy.getYTitle(0).split(",")[1].trim()+"/in.");
					double yScaleVal = Double.parseDouble(yScaleTs[i].getText());
					yScaleVal = (2.54)*yScaleVal;
					yScaleTs[i].setText(frmt.format(yScaleVal));
				}
			}
		});

		//TODO dec format

		metricUnits.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if(xScaleLabel.getText().contains("cm."))
					return;
				/*
				graph.axes.res[0] = graph.axes.res[0]*2.54;
				graph.axes.res[2] = graph.axes.res[2]*2.54;

				graph.invalidate();
				graph.repaint();
				*/
				xScaleLabel.setText("km/in.");
				double xScaleVal = Double.parseDouble(xScaleT.getText());
				//xScaleVal = xScaleVal/(2.54);
				xScaleT.setText(frmt.format(xScaleVal));

				for (int i=0; i<graphs.size(); i++) {
					yScaleLabels[i].setText(graphs.get(i).axes.xy.getYTitle(0).split(",")[1].trim()+"/in.");
					double yScaleVal = Double.parseDouble(yScaleTs[i].getText());
					//yScaleVal = yScaleVal/(2.54);
					yScaleTs[i].setText(frmt.format(yScaleVal));
				}
			};
		});

		standardUnits.setEnabled(false);
		standardUnits.setVisible(false);
		metricUnits.setSelected(true);
//		metricUnits.doClick();
		greatCircle.setSelected(true);
		panel1.add(greatCircle, "West");
		panel1.add(straightLine, "Center");

		JPanel unitPanel = new JPanel(new BorderLayout());
		unitPanel.add(standardUnits,"West");
		unitPanel.add(metricUnits,"Center");
//		***** Changed by A.K.M. 1.3.5 *****
		panel.add(lonLat); // units
	//	panel.add(panel1); // Add circle or line toggle
		panel.add(unitPanel);


//		***** Changed by A.K.M. 1.3.5 *****
//		Add line-changing functionality to radio buttons
		greatCircle.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				lineChanged();
			}
		});
		straightLine.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				lineChanged();
			}
		});
//		***** Changed by A.K.M. 1.3.5 *****
		String[] minutesDegrees = {"Decimal Degrees", "Degrees & Decimal Minutes"};
		degreesOrMinutes = new JComboBox(minutesDegrees);
		degreesOrMinutes.setSize(28, 16);

		degreesOrMinutes.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if(degreesOrMinutes.getSelectedIndex()==0) {
					checkFormat(true,setStartLatT,startLatDropdown);
					checkFormat(true,setStartLonT,startLonDropdown);
					checkFormat(true,setEndLatT,endLatDropdown);
					checkFormat(true,setEndLonT,endLonDropdown);
					// wesn dropdown
					startLatDropdown.setEnabled(false); // dont enable
					startLonDropdown.setEnabled(false);
					endLatDropdown.setEnabled(false);
					endLonDropdown.setEnabled(false);

					startLatDropdown.setVisible(false); // dont show
					startLonDropdown.setVisible(false);
					endLatDropdown.setVisible(false);
					endLonDropdown.setVisible(false);
				}
				else {
					checkFormat(false,setStartLatT,startLatDropdown);
					checkFormat(false,setStartLonT,startLonDropdown);
					checkFormat(false,setEndLatT,endLatDropdown);
					checkFormat(false,setEndLonT,endLonDropdown);
					// wesn dropdown
					startLatDropdown.setEnabled(true); // enable
					startLonDropdown.setEnabled(true);
					endLatDropdown.setEnabled(true);
					endLonDropdown.setEnabled(true);

					startLatDropdown.setVisible(true); // show
					startLonDropdown.setVisible(true);
					endLatDropdown.setVisible(true);
					endLonDropdown.setVisible(true);
				}
			}
		});
		//panel.add(degreesOrMinutes); // decimal degrees
		tools.add(panel,BorderLayout.NORTH); // first row

		//AutoFit X-Scale Items
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		JPanel xPanel = new JPanel(new FlowLayout( FlowLayout.LEFT, 10, 2));
		JLabel xL = new JLabel("X-Scale:");
		xPanel.add(xL);

		// xscale entry
		xScaleT = new JTextField("100",4);
		xPanel.add( xScaleT );
		xScaleT.addActionListener(al);
		xScaleT.setEditable( false );
		xScaleLabel = new JLabel("km/in.");
		xPanel.add( xScaleLabel );

		// xscale autofit
		autoX = new JCheckBox("Auto-fit");
		autoX.setSelected( true );
		xPanel.add( autoX );
		autoX.addActionListener(al);

		panel.add(xPanel);
		
		// Y-Scale Items
		JPanel yScalePanel = new JPanel(new FlowLayout( FlowLayout.LEFT, 10, 2));
		String yScaleTxt = "Y-Scale:";
		if (graphs.size() > 1) yScaleTxt = "Y-Scales:";
		JLabel yL = new JLabel(yScaleTxt);
		yScalePanel.add(yL);
		panel.add(yScalePanel);

		JPanel thisYPanel;
		JLabel thisGraphL;
		JTextField thisYScaleT;
		JLabel thisYScaleLabel;
		JCheckBox thisAutoY;
		JTextField thisVerticalExgT;

		int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
		double xExg = 1000*(dpi/graph.xScale);
		double yExg;
		double vExg;
		DecimalFormat fmt = new DecimalFormat("#.#");
		JLabel veText;
		
		
		// for each plotted graph, add a line containing the y-scale components
		for (int i=0; i<nGraphs; i++) {
			XYGraph thisGraph = graphs.get(i);
			if (thisGraph == null) continue;
			thisYPanel = new JPanel(new FlowLayout( FlowLayout.LEFT, 5, 0));
			thisGraphL = new JLabel(((GMAProfile)thisGraph.xy).gridName);
			thisGraphL.setPreferredSize(new Dimension(250,16));
			thisYPanel.add(thisGraphL);

			// yscale entry
			thisYScaleT = new JTextField("100",4);
			thisYPanel.add( thisYScaleT );
			thisYScaleT.addActionListener(al);
			thisYScaleT.setEditable( false );
			yScaleTs[i] = thisYScaleT;
			
			thisYScaleLabel = new JLabel(graph.axes.xy.getYTitle(0).split(",")[1].trim()+"/in.");
			thisYScaleLabel.setPreferredSize(new Dimension(60,16));
			thisYPanel.add( thisYScaleLabel );
			yScaleLabels[i] = thisYScaleLabel;
			
			// yscale autofit
			thisAutoY = new JCheckBox("Auto-fit");
			thisAutoY.setSelected( true );
			thisYPanel.add( thisAutoY );	
			thisAutoY.addActionListener(al);
			autoYs[i] = thisAutoY;
			
			//Vertical exaggeration
			yExg = (-1)*(dpi/thisGraph.yScale);
			vExg = xExg/yExg;
			
			veText = new JLabel("V.E.:");
			veText.setToolTipText("Vertical Exaggeration");
			
			thisVerticalExgT = new JTextField(fmt.format(vExg), 5);
			thisVerticalExgT.addActionListener(veActionListener);
			thisVerticalExgT.setToolTipText("Vertical Exaggeration");
		//	thisVerticalExgT.setAlignmentX(JComponent.LEFT_ALIGNMENT);
			thisVerticalExgT.setHorizontalAlignment(JTextField.LEFT);
			thisYPanel.add(veText);
			thisYPanel.add(thisVerticalExgT);
			verticalExgTs[i] = thisVerticalExgT;

			panel.add(thisYPanel);

		}
		if (isMultiPlot()) {
			JPanel multiPlotPanel = new JPanel(new FlowLayout( FlowLayout.LEFT, 10, 2));
			multiPlotBtn.setText("Plot on one graph");
			multiPlotBtn.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					int nGraphs = 1;
					graphs = null;
					doProfile( (Line2D.Double)line);
					int panelHeight = 305;
					
					if (multiPlotBtn.isSelected()) {
						multiPlotBtn.setText("Plot on separate graphs");
						panelHeight += 100;
					} else {
						multiPlotBtn.setText("Plot on one graph");
						nGraphs = graphs.size();
						panelHeight *= nGraphs;
					}

					dialog.remove(graphPanel);
					graphPanel = new JPanel();
					graphPanel.setLayout(new GridLayout(nGraphs, 1));
					for (XYGraph plotGraph : graphs) {
						if (plotGraph != null) graphPanel.add(plotGraph);
					}
					
					dialog.getContentPane().add( graphPanel, "Center" );
					dialog.pack();
					dialog.setSize(650, 170 + panelHeight);
				}
			});
		
			multiPlotPanel.add(multiPlotBtn);
			panel.add(multiPlotPanel);
		}



		//WESN dropdowns
		String[] ns = {"N","S"};
		String[] ew = {"E","W"};

		startLatDropdown = new JComboBox(ns);
		startLonDropdown = new JComboBox(ew);
		endLatDropdown = new JComboBox(ns);
		endLonDropdown = new JComboBox(ew);

		startLatDropdown.setEnabled(false); // dont enable
		startLonDropdown.setEnabled(false);
		endLatDropdown.setEnabled(false);
		endLonDropdown.setEnabled(false);

		startLatDropdown.setVisible(false); // dont show
		startLonDropdown.setVisible(false);
		endLatDropdown.setVisible(false);
		endLonDropdown.setVisible(false);

		JPanel startEndPanel = new JPanel(new GridBagLayout());
		final JButton setStartL = new JButton("Enter Start");
		// Lat Text Field
		setStartLatT = new JTextField("lat",6);
		setStartLatT.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				setStartL.doClick();
			}
		});
		setStartLonT = new JTextField("lon",6);
		setStartLonT.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				setStartL.doClick();
			}
		});
		setStartL.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				if(degreesOrMinutes.getSelectedIndex()==0){
					if(MapApp.getApp().getMap().getProjection().getLongitudeRange() == Projection.RANGE_180W_to_180E && (Math.abs(Double.parseDouble(setStartLonT.getText()))> 180)){
						JOptionPane.showMessageDialog(MapApp.anchor,"Longitude must be between -180 and 180", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					else if(MapApp.getApp().getMap().getProjection().getLongitudeRange() == Projection.RANGE_0_to_360 && (Double.parseDouble(setStartLonT.getText())>360 || Double.parseDouble(setStartLonT.getText())<0)) {
						JOptionPane.showMessageDialog(MapApp.anchor,"Longitude must be between 0 and 360", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}

					if((Math.abs(Double.parseDouble(setStartLatT.getText()))> 90)){
						JOptionPane.showMessageDialog(MapApp.anchor,"Latitude must be between -90 and 90", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					try
					{
						currentPts[0]=new Point2D.Double(Double.parseDouble(setStartLonT.getText()), Double.parseDouble(setStartLatT.getText()));
					}catch(NumberFormatException nfe){
						JOptionPane.showMessageDialog(MapApp.anchor,"Use Decimal Degrees", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				else{
					String[] lonTextTokens = setStartLonT.getText().split(" ");
					String[] latTextTokens = setStartLatT.getText().split(" ");
					if((lonTextTokens.length!=2) || (latTextTokens.length!=2)){
						JOptionPane.showMessageDialog(MapApp.anchor,"Use degrees decimal minutes", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}

					if((Double.parseDouble(lonTextTokens[0])>180) || (Double.parseDouble(lonTextTokens[0])<0)){
						JOptionPane.showMessageDialog(MapApp.anchor,"Longitude must be between 0 and 180", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}

					if((Double.parseDouble(latTextTokens[0])>90) || (Double.parseDouble(latTextTokens[0])<0)){
						JOptionPane.showMessageDialog(MapApp.anchor,"Longitude must be between 0 and 90", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}

					if((Double.parseDouble(lonTextTokens[1])>=60) || (Double.parseDouble(lonTextTokens[1])<0) || (Double.parseDouble(latTextTokens[1])>=60) || (Double.parseDouble(latTextTokens[1])<0)){
						JOptionPane.showMessageDialog(MapApp.anchor,"Minutes must be between 0 and 60", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}

					currentPts[0]=new Point2D.Double(lonToNumber(setStartLonT.getText(),startLonDropdown.getSelectedItem().toString()), latToNumber(setStartLatT.getText(),startLatDropdown.getSelectedItem().toString()));
				}

				Point2D pt1 =map.getProjection().getMapXY(currentPts[0]);
				Point2D pt2 = map.getProjection().getMapXY(currentPts[currentPts.length-1]);

				if((lastP1!=null)&&(lastP1.getX()>map.getWrap()) && (pt1.getX()<map.getWrap())){
					//System.out.println("fired profile fix 1");
					pt1.setLocation(pt1.getX()+map.getWrap(), pt1.getY());
				}
				boolean checkFirst = false;
				if(lastP1!=null){
					checkFirst = (lastP1!=null)&&((lastP1.getX()<=0) && (pt1.getX()>=0))||((lastP1.getX()>=0) && (pt1.getX()<=0));
				}
				if(checkFirst){
					//System.out.println("fired profile fix 1 -alt");
					pt1.setLocation(pt1.getX()+map.getWrap(), pt1.getY());
				}

				if((lastP2!=null)&&(lastP2.getX()>map.getWrap()) && (pt2.getX()<map.getWrap())) {
					//System.out.println("fired profile fix 2");
					pt2.setLocation(pt2.getX()+map.getWrap(), pt2.getY());
				}

				boolean checkLast = false;
				if(lastP2!=null){
					checkLast = (lastP2!=null)&&((lastP2.getX()<=0) && (pt2.getX()>=0))||((lastP2.getX()>=0) && (pt2.getX()<=0));
				}
				if(checkLast){
					//System.out.println("fired profile fix 2 -alt");
					pt2.setLocation(pt2.getX()+map.getWrap(), pt2.getY());
				}

				Point2D ptStart = pt1;
				Point2D ptEnd = pt2;

				line = new Line2D.Double(ptStart,ptEnd);
				doProfile((Line2D.Double)line);
			}
		});

		final JButton setEndL = new JButton("Enter End");

		setEndLonT = new JTextField("lon",6);
		setEndLonT.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
					setEndL.doClick();
			}
		});
		setEndLatT = new JTextField("lat",6);
		setEndLatT.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
					setEndL.doClick();
			}
		});
		setEndL.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				if(degreesOrMinutes.getSelectedIndex()==0){
					if((Math.abs(Double.parseDouble(setEndLonT.getText()))> 180)){
						JOptionPane.showMessageDialog(MapApp.anchor,"Longitude must be between -180 and 180", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}

					if((Math.abs(Double.parseDouble(setEndLatT.getText()))> 90)){
						JOptionPane.showMessageDialog(MapApp.anchor,"Latitude must be between -90 and 90", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					//TODO check the dropdowns
					try{
						currentPts[currentPts.length-1]=new Point2D.Double(Double.parseDouble(setEndLonT.getText()), Double.parseDouble(setEndLatT.getText()));
					}catch(NumberFormatException nfe){
						JOptionPane.showMessageDialog(MapApp.anchor,"Use Decimal Degrees", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				else{

					String[] lonTextTokens = setEndLonT.getText().split(" ");
					String[] latTextTokens = setEndLatT.getText().split(" ");
					if((lonTextTokens.length!=2) || (latTextTokens.length!=2)){
						JOptionPane.showMessageDialog(MapApp.anchor,"Use degrees decimal minutes", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}

					if((Double.parseDouble(lonTextTokens[0])>180) || (Double.parseDouble(lonTextTokens[0])<0)){
						JOptionPane.showMessageDialog(MapApp.anchor,"Longitude must be between 0 and 180", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}

					if((Double.parseDouble(latTextTokens[0])>90) || (Double.parseDouble(latTextTokens[0])<0)){
						JOptionPane.showMessageDialog(MapApp.anchor,"Longitude must be between 0 and 90", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}

					if((Double.parseDouble(lonTextTokens[1])>=60) || (Double.parseDouble(lonTextTokens[1])<0) || (Double.parseDouble(latTextTokens[1])>=60) || (Double.parseDouble(latTextTokens[1])<0)){
						JOptionPane.showMessageDialog(MapApp.anchor,"Minutes must be between 0 and 60", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}

					currentPts[currentPts.length-1]=new Point2D.Double(lonToNumber(setEndLonT.getText(),endLonDropdown.getSelectedItem().toString()), latToNumber(setEndLatT.getText(),endLatDropdown.getSelectedItem().toString()));
				}

				//TODO convert lon-lat to xy 
				Point2D pt1 =map.getProjection().getMapXY(currentPts[0]);
				Point2D pt2 = map.getProjection().getMapXY(currentPts[currentPts.length-1]);

				if((lastP1!=null)&&(lastP1.getX()>map.getWrap()) && (pt1.getX()<map.getWrap())){
				//	System.out.println("fired profile fix 3");
					pt1.setLocation(pt1.getX()+map.getWrap(), pt1.getY());
				}
				boolean checkFirst = false;
				if(lastP1!=null){
					checkFirst = (lastP1!=null)&&((lastP1.getX()<=0) && (pt1.getX()>=0))||((lastP1.getX()>=0) && (pt1.getX()<=0));
				}
				if(checkFirst){
				//	System.out.println("fired profile fix 3 -alt");
					pt1.setLocation(pt1.getX()+map.getWrap(), pt1.getY());
				}


				if((lastP2!=null)&&(lastP2.getX()>map.getWrap()) && (pt2.getX()<map.getWrap())){
				//	System.out.println("fired profile fix 4");
					pt2.setLocation(pt2.getX()+map.getWrap(), pt2.getY());
				}

				boolean checkLast = false;
				if(lastP2!=null){
					checkLast = (lastP2!=null)&&((lastP2.getX()<=0) && (pt2.getX()>=0))||((lastP2.getX()>=0) && (pt2.getX()<=0));
				}
				if(checkLast){
				//	System.out.println("fired profile fix 4 -alt");
					pt2.setLocation(pt2.getX()+map.getWrap(), pt2.getY());
				}

				Point2D ptStart = pt1;
				Point2D ptEnd = pt2;

				line = new Line2D.Double(ptStart,ptEnd);
				doProfile((Line2D.Double)line);
			}

		});

		// WEST Panel 
		GridBagConstraints con1 = new GridBagConstraints();
		con1.insets = new Insets(0,5,0,0);
		// First row
		con1.anchor = GridBagConstraints.WEST;
		con1.fill = GridBagConstraints.HORIZONTAL;
		con1.gridx = 1;
		con1.gridy = 0;
		con1.weightx = 0.5;
		con1.weighty = 1.0;
		JLabel startText = new JLabel(" Start");
		startEndPanel.add(startText,con1);

		con1.anchor = GridBagConstraints.WEST;
		con1.fill = GridBagConstraints.HORIZONTAL;
		con1.gridx = 3;
		con1.gridy = 0;
		con1.weightx = 0.5;
		con1.weighty = 1.0;
		JLabel endText = new JLabel(" End");
		startEndPanel.add(endText,con1);

		con1.anchor = GridBagConstraints.WEST;
		con1.fill = GridBagConstraints.FIRST_LINE_START;
		con1.gridx = 5;
		con1.gridy = 0;
		con1.weightx = 0;
		con1.weighty = 0;
		startEndPanel.add(panel1,con1); // circle and line toggle

		// 2nd row
		con1.anchor = GridBagConstraints.WEST;
		con1.fill = GridBagConstraints.HORIZONTAL;
		con1.gridx = 0;
		con1.gridy = 1;
		con1.weightx = 0.2;
		con1.weighty = 1.0;
//		startEndPanel.add(setStartL,con1);
		JLabel latText = new JLabel("Latitude:");
		startEndPanel.add(latText,con1);

		con1.anchor = GridBagConstraints.WEST;
		con1.fill = GridBagConstraints.HORIZONTAL;
		con1.gridx = 1;
		con1.gridy = 1;
		con1.weightx = 0.2;
		con1.weighty = 1.0;
		startEndPanel.add(setStartLatT,con1); // Start Lat

		con1.anchor = GridBagConstraints.WEST;
		con1.fill = GridBagConstraints.HORIZONTAL;
		con1.gridx = 2;
		con1.gridy = 1;
		con1.weightx = 0;
		con1.weighty = 0;
		startEndPanel.add(startLatDropdown,con1); // Start Lat drop down

		con1.anchor = GridBagConstraints.WEST;
		con1.fill = GridBagConstraints.HORIZONTAL;
		con1.gridx = 3;
		con1.gridy = 1;
		con1.weightx = 0.2;
		con1.weighty = 1.0;
		startEndPanel.add(setEndLatT,con1); // End Lat

		con1.anchor = GridBagConstraints.WEST;
		con1.fill = GridBagConstraints.HORIZONTAL;
		con1.gridx = 4;
		con1.gridy = 1;
		con1.weightx = 0;
		con1.weighty = 0;
		startEndPanel.add(endLatDropdown,con1); // End Lat Drop Down

		con1.anchor = GridBagConstraints.WEST;
		con1.fill = GridBagConstraints.HORIZONTAL;
		con1.gridx = 5;
		con1.gridy = 1;
		con1.weighty = 0.2;
		con1.weightx = 1.0;

		// 3nd row
		con1.anchor = GridBagConstraints.WEST;
		con1.fill = GridBagConstraints.HORIZONTAL;
		con1.gridx = 0;
		con1.gridy = 2;
		con1.weightx = 0.2;
		con1.weighty = 1.0;
		JLabel lonText = new JLabel("Longitude:");
		startEndPanel.add(lonText,con1);

		con1.anchor = GridBagConstraints.WEST;
		con1.fill = GridBagConstraints.HORIZONTAL;
		con1.gridx = 1;
		con1.gridy = 2;
		con1.weightx = 0.2;
		con1.weighty = 1.0;
		startEndPanel.add(setStartLonT,con1); // Start Lon 

		con1.anchor = GridBagConstraints.WEST;
		con1.fill = GridBagConstraints.HORIZONTAL;
		con1.gridx = 2;
		con1.gridy = 2;
		con1.weightx = 0;
		con1.weighty = 0;
		startEndPanel.add(startLonDropdown,con1); // Start Lon Dropdown

		con1.anchor = GridBagConstraints.WEST;
		con1.fill = GridBagConstraints.HORIZONTAL;
		con1.gridx = 3;
		con1.gridy = 2;
		con1.weightx = 0.2;
		con1.weighty = 1.0;
		startEndPanel.add(setEndLonT,con1); // End Lon

		con1.anchor = GridBagConstraints.WEST;
		con1.fill = GridBagConstraints.HORIZONTAL;
		con1.gridx = 4;
		con1.gridy = 2;
		con1.weightx = 0;
		con1.weighty = 0;
		startEndPanel.add(endLonDropdown,con1); // End Lon Drop Down

		con1.anchor = GridBagConstraints.WEST;
		con1.fill = GridBagConstraints.HORIZONTAL;
		con1.gridx = 5;
		con1.gridy = 2;
		con1.weighty = 0.2;
		con1.weightx = 1.0;
		startEndPanel.add(degreesOrMinutes,con1); // decimal degrees

		tools.add(startEndPanel, BorderLayout.WEST); // add to WEST Panel 

		setStartLonT.setText(getLon(currentPts[0],startLonDropdown));
		setStartLatT.setText(getLat(currentPts[0],startLatDropdown));

		setEndLonT.setText(getLon(currentPts[currentPts.length-1],endLonDropdown));
		setEndLatT.setText(getLat(currentPts[currentPts.length-1],endLatDropdown));

		//JButton save = new JButton( "Save" );
		tools.add(panel,BorderLayout.SOUTH); // Add autofit panel
		tools.setSize(650, 210);
		metricUnits.doClick();
		return tools;
	} // end of getTools

	void save() {
		if( dialog==null )return;
		int ok= JOptionPane.CANCEL_OPTION;
		if( print.isSelected() ) {
			PrinterJob pj = PrinterJob.getPrinterJob();
			pj.setJobName(" Print Profile ");

			pj.setPrintable (new Printable() {    
				public int print(Graphics pg, PageFormat pf, int pageNum){
					if (pageNum > 0){
						return Printable.NO_SUCH_PAGE;
					}	
					Graphics2D g2 = (Graphics2D) pg;
					g2.translate(pf.getImageableX(), pf.getImageableY());
			        //scale so that printout shows whole graph 
					g2.scale(0.8, 0.8);
					//print the graph panel
					graphPanel.paint(g2);
					return Printable.PAGE_EXISTS;
				}
			});
			if (pj.printDialog() == false)
				return;

			try {
				pj.print();
			} catch (PrinterException ex) {
				// handle exception
			}
			
		} else if( saveToFile.isSelected() ) {
			// determine number of decimal places based on the zoom level
			double zoom = map.getZoom();
			NumberFormat fmt = GeneralUtils.getZoomNumberFormat(zoom);
			JFileChooser chooser = MapApp.getFileChooser();
			ok = chooser.showSaveDialog(dialog);
			if( ok==JFileChooser.CANCEL_OPTION ) return;
			if( chooser.getSelectedFile().exists() ) {
				ok = askOverWrite();
				if( ok==JOptionPane.CANCEL_OPTION ) return;
			}
			try {
				PrintStream out;
				if ( chooser.getSelectedFile().getPath().endsWith(".txt") ) {
					out = new PrintStream( new FileOutputStream( chooser.getSelectedFile() ));
				}
				else {
					out = new PrintStream( new FileOutputStream( new File(chooser.getSelectedFile().getPath() + ".txt") ));
				}

				float[] data;
				ArrayList<String> yDataTypes = new ArrayList<String>();
				ArrayList<String> yUnits = new ArrayList<String>();
				ArrayList<ArrayList<Float>> zData = new ArrayList<ArrayList<Float>>();
				out.print("\t\t");
				
				//reverse the order of the graphs arrayList to display columns in right order
				Collections.reverse(graphs);
				
				for (XYGraph thisGraph : graphs) {
					GMAProfile xy = (GMAProfile)thisGraph.xy;
					//grid names
					out.print("\t"+xy.gridName);
					String[] yTitle = thisGraph.xy.getYTitle(0).split(",");
					yDataTypes.add(yTitle[0]);
					yUnits.add(yTitle[1].replace(" ",""));
					//z-data
					ArrayList<Float> thisZData = new ArrayList<Float>();
					for( int k=0 ; k<xy.xyz.size() ; k++) {
						thisZData.add(xy.xyz.get(k)[1]);
					}
					zData.add(thisZData);
				}
				out.print("\n");
				
				//column headers
				out.print("Longitude\tLatitude\tDistance (km)");
				for (int i=0; i<yDataTypes.size(); i++) {
					out.print("\t" + yDataTypes.get(i) + " (" + yUnits.get(i) + ")");
				}
				out.print("\n");

				GMAProfile xy = (GMAProfile)graphs.get(0).xy;
				for( int k=0 ; k<xy.xyz.size() ; k++) {
					data = (float[])xy.xyz.get(k);
					float tempLon = data[2];

					if(MapApp.getApp().getMap().getProjection().getLongitudeRange() == Projection.RANGE_180W_to_180E && tempLon>=180){
						tempLon=(float)((-1.0)*(360.0 - tempLon));
					}
					else if(MapApp.getApp().getMap().getProjection().getLongitudeRange() == Projection.RANGE_0_to_360 && tempLon < 0) {
						tempLon += 360.;
					}

					boolean tf = profileTypes != null && straightLine.isSelected();
					if(tf) {
							out.print( fmt.format(tempLon) +"\t"+
							fmt.format(data[3]) +"\t"+
							data[4] +"\t"+
							data[1]);
					}else {
						out.print( fmt.format(tempLon) +"\t"+
								fmt.format(data[3]) +"\t"+
								data[0] +"\t"+
								data[1]);
					}
					
					// add extra columns for extra plotted graphs
					for (int i=1; i<zData.size(); i++) {
						out.print("\t" + (zData.get(i)).get(k));
					}
					out.print("\n");
				}
				out.close();
				//revert the order of graphs before continuing
				Collections.reverse(graphs);			
			} catch(IOException e) {
				JOptionPane.showMessageDialog(dialog,
						" Save failed: "+e.getMessage(),
						" Save failed",
						 JOptionPane.ERROR_MESSAGE);
			}
		} else if( saveToClipboard.isSelected() ) {
			// determine number of decimal places based on the zoom level
			double zoom = map.getZoom();
			NumberFormat fmt = GeneralUtils.getZoomNumberFormat(zoom);
			float[] data;
			JTextArea text = new JTextArea();
			ArrayList<String> yDataTypes = new ArrayList<String>();
			ArrayList<String> yUnits = new ArrayList<String>();
			ArrayList<ArrayList<Float>> zData = new ArrayList<ArrayList<Float>>();
			text.append("\t\t");
			//reverse the order of the graphs arrayList to display columns in right order
			Collections.reverse(graphs);
			
			for (XYGraph thisGraph : graphs) {
				GMAProfile xy = (GMAProfile)thisGraph.xy;
				//grid names
				text.append("\t"+xy.gridName);
				String[] yTitle = thisGraph.xy.getYTitle(0).split(",");
				yDataTypes.add(yTitle[0]);
				yUnits.add(yTitle[1].replace(" ",""));
				//z-data
				ArrayList<Float> thisZData = new ArrayList<Float>();
				for( int k=0 ; k<xy.xyz.size() ; k++) {
					thisZData.add(xy.xyz.get(k)[1]);
				}
				zData.add(thisZData);
			}
			text.append("\n");
			
			//column headers
			text.append("Longitude\tLatitude\tDistance (km)");
			for (int i=0; i<yDataTypes.size(); i++) {
				text.append("\t" + yDataTypes.get(i) + " (" + yUnits.get(i) + ")");
			}
			text.append("\n");

			GMAProfile xy = (GMAProfile)graphs.get(0).xy;
			for( int k=0 ; k<xy.xyz.size() ; k++) {
				data = (float[])xy.xyz.get(k);
				float tempLon = data[2];

				if(MapApp.getApp().getMap().getProjection().getLongitudeRange() == Projection.RANGE_180W_to_180E && tempLon>=180) {
					tempLon=(float)((-1.0)*(360.0 - tempLon));
				}
				else if(MapApp.getApp().getMap().getProjection().getLongitudeRange() == Projection.RANGE_0_to_360 && tempLon < 0) {
					tempLon += 360.;
				}

				boolean tf = profileTypes != null && straightLine.isSelected();
				if(tf) {
					text.append( fmt.format(tempLon) +"\t"+
						fmt.format(data[3]) +"\t"+
						data[4] +"\t"+
						data[1]);
				}else {
					text.append( fmt.format(tempLon) +"\t"+
							fmt.format(data[3]) +"\t"+
							data[0] +"\t"+
							data[1]);
				}
				// add extra columns for extra plotted graphs
				for (int i=1; i<zData.size(); i++) {
					text.append("\t" + (zData.get(i)).get(k));
				}
				text.append("\n");
			}
			text.selectAll();
			text.copy();
			//revert the order of graphs before continuing
			Collections.reverse(graphs);
		} else if( saveJPEG.isSelected() ) {
			JFileChooser chooser = MapApp.getFileChooser();
			ok = chooser.showSaveDialog(dialog);
			if( ok==JFileChooser.CANCEL_OPTION ) return;
			if( chooser.getSelectedFile().exists() ) {
				ok = askOverWrite();
				if( ok==JOptionPane.CANCEL_OPTION ) return;
			}
			
			//get the image from the graphPanel
			BufferedImage image = new BufferedImage(graphPanel.getWidth(), graphPanel.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics g = image.getGraphics();
			graphPanel.paint(g);

			try {
				String name = chooser.getSelectedFile().getName();
				int sIndex = name.lastIndexOf(".");
				String suffix = sIndex<0
					? "jpg"
					: name.substring( sIndex+1 );
				if( !ImageIO.getImageWritersBySuffix(suffix).hasNext())suffix = "jpg";

//				***** GMA 1.6.0: Add .jpg extension if necessary
//				ImageIO.write(image, suffix, chooser.getSelectedFile());
				if ( chooser.getSelectedFile().getPath().endsWith(".jpg") ) {
					ImageIO.write(image, suffix, chooser.getSelectedFile());
				}
				else {
					ImageIO.write(image, suffix, new File(chooser.getSelectedFile().getPath() + ".jpg") );
				}
//				***** GMA 1.6.0
			} catch(IOException e) {
				JOptionPane.showMessageDialog(dialog,
						" Save failed: "+e.getMessage(),
						" Save failed",
						 JOptionPane.ERROR_MESSAGE);
			}
		} else if( savePNG.isSelected() ) {
			JFileChooser chooser = MapApp.getFileChooser();
			ok = chooser.showSaveDialog(dialog);
			if( ok==JFileChooser.CANCEL_OPTION ) return;
			if( chooser.getSelectedFile().exists() ) {
				ok = askOverWrite();
				if( ok==JOptionPane.CANCEL_OPTION ) return;
			}
			
			//get the image from the graphPanel
			BufferedImage image = new BufferedImage(graphPanel.getWidth(), graphPanel.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics g = image.getGraphics();
			graphPanel.paint(g);
			
			try {

//				***** GMA 1.6.0: Add .png extension if necessary
//				ImageIO.write(image, "png", chooser.getSelectedFile());
				if ( chooser.getSelectedFile().getPath().endsWith(".png") ) {
					ImageIO.write(image, "png", chooser.getSelectedFile());
				}
				else {
					ImageIO.write(image, "png", new File(chooser.getSelectedFile().getPath() + ".png") );
				}
//				***** GMA 1.6.0

			} catch(IOException e) {
				JOptionPane.showMessageDialog(dialog,
						" Save failed: "+e.getMessage(),
						" Save failed",
						 JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	int askOverWrite() {
		JFileChooser chooser = MapApp.getFileChooser();
		int ok = JOptionPane.NO_OPTION;
		while( true ) {
			ok = JOptionPane.showConfirmDialog(dialog,
				"File exists. Overwrite?",
				"Overwrite?",
				JOptionPane.YES_NO_CANCEL_OPTION);
			if( ok!=JOptionPane.NO_OPTION) return ok;
			ok = chooser.showSaveDialog(dialog);
			if( ok==JFileChooser.CANCEL_OPTION ) return JOptionPane.CANCEL_OPTION;
			if( !chooser.getSelectedFile().exists() ) return JOptionPane.YES_OPTION;
		}
	}
	PageFormat fmt;
	JPanel savePanel;
	void initSave() {
		savePanel = new JPanel(new GridLayout(0,1));
		savePanel.setBorder( BorderFactory.createTitledBorder("Save Options"));
		ButtonGroup gp = new ButtonGroup();
		saveToFile = new JToggleButton("Save ASCII table");
		savePanel.add( saveToFile );
		gp.add( saveToFile );
		saveToClipboard = new JToggleButton("Copy to clipboard");
		savePanel.add( saveToClipboard );
		gp.add( saveToClipboard );
		saveJPEG = new JToggleButton("Save JPEG image");
		savePanel.add( saveJPEG );
		gp.add( saveJPEG );
		savePNG = new JToggleButton("Save PNG image");
		savePanel.add( savePNG );
		gp.add( savePNG );
		print = new JToggleButton("Print");
		savePanel.add( print );
		gp.add( print );
	}

	String getLon(Point2D pt, JComboBox box){
		// determine number of decimal places based on the zoom level
		double zoom = map.getZoom();
		NumberFormat fmt = GeneralUtils.getZoomNumberFormat(zoom);
		NumberFormat fmtMins = GeneralUtils.getZoomNumberFormatMins(zoom);
		
		String degStartX = (""+pt.getX()).split("\\.")[0];
		String startRest = (""+pt.getX()).split("\\.")[1];

		if(degreesOrMinutes.getSelectedIndex()==0){
			if(MapApp.getApp().getMap().getProjection().getLongitudeRange() == Projection.RANGE_180W_to_180E && pt.getX()>180){
				return fmt.format((-1)*(360.0 - pt.getX()));
			}
			else if(MapApp.getApp().getMap().getProjection().getLongitudeRange() == Projection.RANGE_0_to_360 && pt.getX() < 0) {
				return fmt.format(pt.getX()+360.);
			}
			return fmt.format(pt.getX());
		}

		Double minStartX; 
		if(startRest.length()>2){
			String convertStartMinX = startRest.substring(0, 2)+"."+startRest.substring(2,startRest.length());
			minStartX = Double.parseDouble(convertStartMinX)*(60.0/100.0);	
		}
		else
			minStartX = Double.parseDouble(startRest)*(60.0/100.0);

		if(degStartX.substring(0, 1).equals("-")){
			box.setSelectedIndex(1);
			if(MapApp.getApp().getMap().getProjection().getLongitudeRange() == Projection.RANGE_180W_to_180E)
				degStartX = degStartX.substring(1,degStartX.length());
		}
		else if(MapApp.getApp().getMap().getProjection().getLongitudeRange() == Projection.RANGE_180W_to_180E && Double.parseDouble(degStartX) > 180){
			degStartX = ""+(360 - Double.parseDouble(degStartX));
			box.setSelectedIndex(1);
		}
		else{
			box.setSelectedIndex(0);
		}
		return (degStartX + "\u00B0" + fmtMins.format(minStartX))+"\u2032";
	}


	String getLat(Point2D pt, JComboBox box){	
		// determine number of decimal places based on the zoom level
		double zoom = map.getZoom();
		NumberFormat fmt = GeneralUtils.getZoomNumberFormat(zoom);
		NumberFormat fmtMins = GeneralUtils.getZoomNumberFormatMins(zoom);

		if(degreesOrMinutes.getSelectedIndex()==0)
			return fmt.format(pt.getY());

		String degStartX = (""+pt.getY()).split("\\.")[0];
		String startRest = (""+pt.getY()).split("\\.")[1];
		Double minStartX; 

		if(startRest.length()>2){
			String convertStartMinX = startRest.substring(0, 2)+"."+startRest.substring(2,startRest.length());
			minStartX = Double.parseDouble(convertStartMinX)*(60.0/100.0);
		}
		else
			minStartX = Double.parseDouble(startRest)*(60.0/100.0);

		if(degStartX.substring(0, 1).equals("-")){
			box.setSelectedIndex(1);
			degStartX = degStartX.substring(1,degStartX.length());
		}
		else {
			box.setSelectedIndex(0);
		}
		return (degStartX + "\u00B0" + fmtMins.format(minStartX))+"\u2032";
	}

	Double latToNumber(String lat, String ew){
		String[] latTokens = lat.replace("\u2032","").split("\u00B0");
		String num = latTokens[0];
		Double degToDec = (100.0/60.0)*Double.parseDouble(latTokens[1]);
		String decStr = "."+((""+degToDec).split("\\.")[0]) + ((""+degToDec).split("\\.")[1]);
		Double number = Double.parseDouble(num.concat(decStr));
		if(ew.equalsIgnoreCase("S"))
			number = (-1.0)*number;
		return number;
	}

	Double lonToNumber(String lon, String ew){
		String[] latTokens = lon.replace("\u2032","").split("\u00B0");
		String num = latTokens[0];
		num = ""+((int)(Double.parseDouble(num)));
		Double degToDec = (100.0/60.0)*Double.parseDouble(latTokens[1]);
		String decStr = "."+((""+degToDec).split("\\.")[0]) + ((""+degToDec).split("\\.")[1]);
		Double number = Double.parseDouble(num.concat(decStr));
		if(ew.equalsIgnoreCase("W"))
			number = (-1.0)*number;

		if(MapApp.getApp().getMap().getProjection().getLongitudeRange() == Projection.RANGE_180W_to_180E && number > 180){
			number = (-1.0)*(360.0-number);
		}

		else if(MapApp.getApp().getMap().getProjection().getLongitudeRange() == Projection.RANGE_0_to_360 && number < 0) {
			number += 360.;
		}

		return number;
	}

	public void checkFormat(boolean deg, JTextField textToSet, JComboBox box ){
		// determine number of decimal places based on the zoom level
		double zoom = map.getZoom();
		NumberFormat fmt = GeneralUtils.getZoomNumberFormat(zoom);
		NumberFormat fmtMins = GeneralUtils.getZoomNumberFormatMins(zoom);
		if(deg){
			if(!box.isEnabled())
				return;

			String set="";
			if(box.getSelectedIndex()==1){
				set = "-";
			}

			String[] boxText = textToSet.getText().replace("\u2032","").split("\u00B0");
			double decimalValue = Double.parseDouble(boxText[1])/60.0;
			String dec = (""+decimalValue);
			set  = set.concat(boxText[0]+dec.substring(1,dec.length()));
			textToSet.setText(fmt.format(Double.parseDouble(set)));

		}else{
			if(box.isEnabled())
				return;
			String firstPart;

			String boxText[] = textToSet.getText().split("\\.");

			if(boxText[0].substring(0, 1).equals("-")){
				box.setSelectedIndex(1);
				firstPart=boxText[0].substring(1,boxText[0].length());
			}else{
				box.setSelectedIndex(0);
				firstPart = boxText[0];
			}
			String newText;

			if(boxText.length==1){
				newText = firstPart+"\u00B0"+fmtMins.format((Double.parseDouble("."+0)*60.0))+"\u2032";
//			System.out.println(newText + " true");
			}else{
				newText = firstPart+"\u00B0"+fmtMins.format((Double.parseDouble("."+boxText[1])*60.0))+"\u2032";
//				System.out.println(newText + " false");
			}
			textToSet.setText(newText);
		}
	}
	
	//store the status of the profile button in a
	//static parameter so that it is easy to access
	static public boolean getProfileStatus() {
		return enabled;
	}
}



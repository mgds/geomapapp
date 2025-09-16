package haxby.db.dig;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.event.ListSelectionEvent;

import org.geomapapp.geom.XYZ;
import org.geomapapp.grid.Grid2DOverlay;

import haxby.db.custom.DBDescription;
import haxby.db.custom.DBTableModel;
import haxby.db.custom.UnknownData;
import haxby.db.custom.UnknownDataSet;
import haxby.map.XMap;
import haxby.proj.Projection;
import haxby.util.GeneralUtils;


public class LineSegmentsObject extends DBTableModel
				implements DigitizerObject,
					MouseListener,
					MouseMotionListener,
					KeyListener
					{

	private static final long serialVersionUID = 2694953748880636811L;
	public static final String LAT_COL = "Latitude";
	public static final String LON_COL = "Longitude";
	public static final String Z_COL = "Elevation (m)";
	public static String Z_GRIDNAME = Z_COL;
	public static final String DISTANCE_COL = "Distance between points (km)";
	public static final String CUMULATIVE_COL = "Cumulative distance (km)";
	public static final String DURATION_COL = "Duration (hrs)";
	static final String COLUMN_NAMES =  LON_COL+","+LAT_COL+","+Z_COL+","+CUMULATIVE_COL+","+DISTANCE_COL+","+DURATION_COL;
	XMap map;
	Digitizer dig;
	public Vector points;
	ArrayList<Point2D> currentPath;
	ArrayList<Point2D> currentSegPath;
	boolean visible;
	double wrap;
	String name;
	float lineWidth;
	Color color;
	Color fill;
	Color lastColor, lastFill;
	BasicStroke stroke;
	boolean drawing = false;
	boolean selected;
	long when;
	int currentPoint;
	double currentOffset = 0.;
	boolean editShape;
	boolean active;
	ArrayList<Point4D> profile = null;
	int table;
	Grid2DOverlay grid;
	static ImageIcon icon = Digitizer.SEGMENTS(false);
	static ImageIcon invisibleIcon = Digitizer.SEGMENTS(true);
	static Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
	JPopupMenu rightClickMenu;

	public LineSegmentsObject(XMap map, Digitizer dig) {
		this(new UnknownDataSet(new DBDescription("Digitizer",0,""), COLUMN_NAMES, ",", map), map, dig);
	}
	
	public LineSegmentsObject(UnknownDataSet d, XMap map, Digitizer dig) {
		super(d, false);
		
		this.map = map;
		wrap = map.getWrap();
		this.dig = dig;
		points = new Vector();
		visible = true;
		name = null;
		selected = false;
		drawing = false;
		lineWidth = 1f;
		color = Color.black;
		when = 0L;
		currentPoint = -1;
		editShape = false;
		active = false;
		setColor( dig.options.color );
		setFill( dig.options.fill );
		setStroke( dig.options.stroke );
		lastColor = color;
		lastFill = Color.white;
		table = 0;
		rightClickMenu = new JPopupMenu("Popup");
		JMenuItem deleteItem = new JMenuItem("Delete Point");
		deleteItem.setToolTipText("Click to delete");
		deleteItem.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	          dig.deletePtsBtn.doClick();
	        }
	      });
		rightClickMenu.add(deleteItem);
	}
	public void setName(String name) {
		this.name = name;
	}
	public String toString() {
		if( name==null) {
			return "Digitized Segment";
		}
		return name;
	}
	public Color getColor() {
		return color;
	}
	public Color getFill() {
		return fill;
	}
	public void setColor( Color c ) {
		if( c==null ) {
			if( color==null ) {
				color = lastColor;
			} else {
				lastColor = color;
				color = c;
			}
			return;
		}
		color = c;
	}
	public void setFill( Color c ) {
		if( c==null ) {
			if( fill==null ) {
				fill = lastFill;
			} else {
				lastFill = fill;
				fill = c;
			}
			return;
		}
		fill = c;
	}
	public BasicStroke getStroke() {
		return stroke;
	}
	public void setStroke( BasicStroke s ) {
		stroke = s;
	}
	public void start() {
		map.addMouseListener( this );
		map.addMouseMotionListener( this );
		map.addKeyListener( this );
		map.setCursor( cursor );
		active = true;
		dig.table.setModel( this );
	}
	public boolean finish() {
		active = false;
		drawSeg();
		map.removeMouseListener( this );
		map.removeMouseMotionListener( this );
		map.removeKeyListener( this );
		map.setCursor( Cursor.getDefaultCursor() );
		if( points.size()<=1 ) return false;
		return true;
	}
	public void setVisible( boolean tf ) {
		visible = tf;
	}
	public boolean isVisible() {
		return visible;
	}
	public boolean select( double x, double y ) {
		if( points.size()<=1 )return false;
		Rectangle2D.Double bounds = (Rectangle2D.Double)computeBounds();
		double r = 2.5/map.getZoom();
		bounds.x -= r;
		bounds.y -= r;
		bounds.width += 2.*r;
		bounds.height += 2.*r;
		if( wrap>0. ) {
			while( bounds.x>x ) x += wrap;
			while( bounds.x+bounds.width<x ) x -= wrap;
		}
		if( !bounds.contains( x, y ) )return false;
		double x1, y1, x2, y2, dx0, dy0, dx, dy, r0, r1, test;;
		double[] xyz = (double[])points.get(0);
		x1 = xyz[0];
		y1 = xyz[1];
		double rMin = 100000.;
		r *= r;
		for( int i=1 ; i<points.size() ; i++) {
			xyz = (double[])points.get(i);
			x2 = xyz[0];
			y2 = xyz[1];
			if(x1==x2 && y1==y2) continue;
			dx0 = x2-x1;
			dy0 = y2-y1;
			dx = x-x1;
			dy = y-y1;
			r0 = dx0*dx0 + dy0*dy0;
			test = dx*dx0 + dy*dy0;
			x1 = x2;
			y1 = y2;
			if(test<0) {
				r1 = dx*dx + dy*dy;
			} else if( test>r0 ) {
				dx -= dx0;
				dy -= dy0;
				r1 = dx*dx + dy*dy;
			} else {
				r1 = -dx*dy0 + dy*dx0;
				r1 *= r1/r0;
			}
			if( r1<rMin ) rMin = r1;
			if( r1>r ) continue;
			return true;
		}
		return false;
	}
	public Rectangle2D computeBounds() {
		Rectangle2D.Double bounds = new Rectangle2D.Double();
		if( points.size()==0 ) return bounds;
		double[] xyz = (double[])points.get(0);
		bounds.x = xyz[0];
		bounds.y = xyz[1];
		for( int i=1 ; i<points.size() ; i++ ) {
			xyz = (double[])points.get(i);
			if( xyz[0]<bounds.x ) {
				bounds.width += bounds.x-xyz[0];
				bounds.x = xyz[0];
			} else if( xyz[0]>bounds.x+bounds.width ) {
				bounds.width = xyz[0]-bounds.x;
			}
			if( xyz[1]<bounds.y ) {
				bounds.height += bounds.y-xyz[1];
				bounds.y = xyz[1];
			} else if( xyz[1]>bounds.y+bounds.height ) {
				bounds.height = xyz[1]-bounds.y;
			}
		}
		return bounds;
	}
	public void setSelected( boolean tf ) {
		if( selected == tf ) return;
		if( !selected ) {
			map.addMouseListener(this);
			map.addMouseMotionListener(this);
			dig.table.setModel( this );
		} else {
			map.removeMouseListener(this);
			map.removeMouseMotionListener(this);
		}
		selected = tf;
	}
	public boolean isSelected() {
		return selected;
	}
	public Icon getIcon() {
		return icon;
	}
	public Icon getDisabledIcon() {
		return invisibleIcon;
	}
	public void draw( java.awt.Graphics2D g ) {
		drawing = false;
		if( !visible || points.size()<=0 ) return;
		lineWidth = stroke.getLineWidth();
		g.setStroke( new BasicStroke( 2f/(float)map.getZoom() ));
		GeneralPath path = getGeneralPath(currentPath);

		double[] xyz = (double[])points.get(0);
		double min, max;
		if (path != null) {
			Rectangle pathBounds = path.getBounds();
			min = pathBounds.getMinX();
			max = pathBounds.getMaxX();
		} else {
			min = xyz[0];
			max = xyz[1];
		}
		GeneralPath path1 = new GeneralPath();
		double dx = 2./map.getZoom();
		if( selected || active) 
			path1.append( new Rectangle2D.Double( xyz[0]-dx, xyz[1]-dx, 2*dx, 2*dx), false);
		for(int i=1 ; i<points.size() ; i++) {
			xyz = (double[])points.get(i);
			if( xyz[0]>max ) max=xyz[0];
			else if( xyz[0]<min ) min=xyz[0];
			if( selected || active ) 
				path1.append( new Rectangle2D.Double( xyz[0]-dx, xyz[1]-dx, 2*dx, 2*dx), false);
		}

		// Draw a star at the coords in a selected table row
		ArrayList<Shape> stars = new ArrayList<Shape>();
		int[] rows = dig.table.getSelectedRows();
		for (int row: rows) {
			if (row != -1) {
				float lon = Float.parseFloat((String) dig.table.getValueAt(row,0));
				float lat = Float.parseFloat((String) dig.table.getValueAt(row,1));
				Point2D pt = map.getProjection().getMapXY( new Point2D.Double(lon, lat) );
				double x_sel = pt.getX();
				double y_sel = pt.getY();
		
				if( x_sel>max ) max=x_sel;
				else if( x_sel<min ) min=x_sel;
				
				double width = 10/map.getZoom();
				Rectangle2D rect = map.getClipRect2D();
				double wrap = map.getWrap();
				if (wrap > 0f) {
					while(x_sel>rect.getX() ) x_sel -= wrap;
					while(x_sel< rect.getX() ) x_sel += wrap;
				}
				
				Shape star = UnknownDataSet.createStar(5, new Point2D.Double(x_sel,y_sel), width/2.0, width/5.0);
				stars.add(star);
			}
		}
		// draw the paths
		if( wrap>0. ) {
			AffineTransform at = g.getTransform();
			Rectangle2D rect = map.getClipRect2D();
			double offset = 0.;
			while( min+offset>rect.getX() ) offset -= wrap;
			while( max+offset< rect.getX() ) offset += wrap;
			g.translate( offset, 0.);
			while( min+offset < rect.getX()+rect.getWidth() ) {
				if( fill != null ) {
					g.setColor(fill);
					if (path != null) g.fill( path );
				}
				if( color!=null ) {
					g.setColor(color);
					g.draw(path1);
					if (path != null )g.draw(path);
				}
				offset += wrap;
				g.translate( wrap, 0.);
			}
			g.setTransform( at);
		} else {
			if( fill != null ) {
				g.setColor(fill);
				if (path != null) g.fill(path);
			}
			if( color!=null ) {
				g.setColor(color);
				g.draw(path1);
				if (path != null )g.draw(path);
			}
		}
		if( !selected ) return;
		// draw the selected path with point markers and a star for a selected table row coords
		g.setXORMode( Color.white );
		g.setStroke( new BasicStroke( 2f/(float)map.getZoom() ));
		if( wrap>0. ) {
			AffineTransform at = g.getTransform();
			Rectangle2D rect = map.getClipRect2D();
			double offset = 0.;
			while( min+offset>rect.getX() ) offset -= wrap;
			while( max+offset< rect.getX() ) offset += wrap;
			g.translate( offset, 0.);
			while( min+offset < rect.getX()+rect.getWidth() ) {
				g.draw( path1 );
				if (path != null )g.draw(path);
				if (stars.size() > 0) {
					Color col = g.getColor();
					g.setPaintMode();
					for (Shape star : stars) {
						g.setColor(Color.RED);
						g.fill(star);
						g.setColor(Color.RED);
						g.draw(star);
					}
					g.setColor(col);
					g.setXORMode( Color.white );
				}
				offset += wrap;
				g.translate( wrap, 0.);
			}
			g.setTransform( at);
		} else {
			g.draw( path1 );
			if (path != null )g.draw(path);
			if (stars.size() > 0) {
				Color col = g.getColor();
				g.setPaintMode();
				for (Shape star : stars) {
					g.setColor(Color.RED);
					g.fill(star);
					g.setColor(Color.BLUE);
					g.draw(star);
				}
				g.setColor(col);
				g.setXORMode( Color.white );
			}
		}
		g.setPaintMode();
	}
	public void mouseEntered( MouseEvent evt ) {
		mouseMoved(evt);
	}
	public void mouseExited( MouseEvent evt ) {
		drawSeg();
		drawing = false;
	}
	public void mousePressed( MouseEvent evt ) {
		//finish drawing segment on double click
		if (evt.getClickCount() == 2 && !evt.isConsumed() && dig.startStopBtn.isSelected()) {
			evt.consume();
			dig.finish();
			return;
		}
		
        if (dig.startStopBtn.isSelected()){
			dig.startStopBtn.setForeground(Color.RED);
        	dig.startStopBtn.setText("Stop digitizing");
        } 
		if( evt.isControlDown() || !selected ) return;
		double zoom = map.getZoom();
		double r = 5/zoom;
		Point2D.Double p = (Point2D.Double)map.getScaledPoint( evt.getPoint() );
		double wrap = map.getWrap();
		for( int i=0 ; i<points.size() ; i++) {
			double[] xyz = (double[])points.get(i);
			if( p.y<xyz[1]-r || p.y>xyz[1]+r )continue;
			currentOffset = 0.;
			if( wrap>0. ) {
				while( p.x<xyz[0]-wrap/2. ) {
					currentOffset -= wrap;
					p.x+=wrap;
				}
				while( p.x>xyz[0]+wrap/2. ) {
					currentOffset += wrap;
					p.x-=wrap;
				}
			}
			if( p.x<xyz[0]-r || p.x>xyz[0]+r )continue;
			when = evt.getWhen();
			currentPoint = i;
			//highlight in table
			try {
				dig.list.setSelectedIndex(currentPoint);
				dig.list.getListSelectionListeners()[0].valueChanged(new ListSelectionEvent(evt.getSource(), 0, dig.list.getModel().getSize()-1, false));
		        dig.deletePtsBtn.setEnabled(true);	
				if (SwingUtilities.isRightMouseButton(evt)) {
					rightClickMenu.show(evt.getComponent(), evt.getX(), evt.getY());
				}
				dig.table.setRowSelectionInterval(currentPoint, currentPoint);
				dig.map.repaint();
				dig.redraw();
				return;
				} 
			catch(Exception e) {
				System.out.println(e);
				continue;
			}
			
		}
		currentPoint = -1;
	}
	public void mouseReleased( MouseEvent evt ) {
        
		if( !editShape ) {
			currentPoint = -1;
			return;
		}
		//drawEdit();
		editShape = false;
		if( evt.isControlDown() || evt.getWhen()-when<500L ) {
			currentPoint = -1;
			return;
		}
		Point2D.Double p = (Point2D.Double)map.getScaledPoint( evt.getPoint() );
		double[] xyz = (double[])points.get( currentPoint );
		xyz[0] = p.x - currentOffset;
		xyz[1] = p.y;
		xyz[2] = getZ(p);
		fireTableRowsUpdated(currentPoint, currentPoint);
		dig.makeProfile();
		map.repaint();
		
	}
	public void mouseClicked( MouseEvent evt ) {
		dig.insertBtn.setEnabled(dig.table.getSelectedRows().length == 1);
		if(!active || evt.isControlDown())return;
		drawSeg();
		Point2D.Double p = (Point2D.Double)map.getScaledPoint( evt.getPoint() );
		if( points.size()>0 ) {
			double[] xyz1 = (double[])points.get(points.size()-1);
			if( p.x==xyz1[0] && p.y==xyz1[1] ) return;
		}
		double z = getZ( p );
		double[] xyz = new double[] { p.x, p.y, z};
		points.add( xyz );
		Vector<Object> newData = new Vector<Object>(Arrays.asList(p.x, p.y));
		getDataSet().addData(new UnknownData(newData));
		displayToDataIndex.add(getDataSet().data.size()-1);
		fireTableStructureChanged();
		// update the interpolated points and profile every time a new point is added
		dig.makeProfile();
		redraw();
	}
	
	//update z-values for points
	protected void updatePoints() {
		for (Object point : points) {
			double[] xyz = (double[])point;
			Point2D.Double p = new Point2D.Double(xyz[0], xyz[1]);
			xyz[2] = getZ(p);
		}
	}
	
	void insertPoints(int beforeIndex, LineSegmentsObject other) {
		other.updatePoints();
		for(int i = other.points.size()-1; i >= 0; i--) {
			points.add(beforeIndex, other.points.get(i));
		}
	}
	
	public double getZ(Point2D p) {
		if( map.getFocus()==null )return Double.NaN;
		return map.getFocus().getZ(map.getProjection().getRefXY(p));
	}
	
	private GeneralPath getGeneralPath(ArrayList<Point2D> pointsList) {
		if (pointsList == null) return null;
		Projection proj = map.getProjection();
		GeneralPath path = new GeneralPath();
		float[] lastP = null;
		float wrap = (float)map.getWrap()/2f;

		for( int k=0 ; k<pointsList.size() ; k++) {
			Point2D p = proj.getMapXY( pointsList.get(k) );
			float x = (float)p.getX();
			float y = (float)p.getY();

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
	
	protected void getProfile() {
		if( points.size()<1 ) return;
		if (points.size() == 1) {
			//need this if deleting and down to one point
			currentPath = null;
			return;
		}
		grid = map.getFocus();
		
		profile = new ArrayList<Point4D>();
		ArrayList<Point2D> path = new ArrayList<Point2D>();
		ArrayList<Point2D> segment = new ArrayList<Point2D>();
		double distance = 0.;
		float z;
		double[] xyz;
		Point2D.Double p1, p2;
		//generate the interpolated points, convert to lat/lon and add to the path
		for (int i=1; i<points.size(); i++) {
			xyz = (double[])points.get(i-1);
			// start point in lat/lon
			p1 = new Point2D.Double( xyz[0], xyz[1] );
			// end of segment in lat/lon
			xyz = (double[])points.get(i);
			p2 = new Point2D.Double( xyz[0], xyz[1] );
			segment = getPath(p1,p2);
			if (i==1)
				path.addAll(segment);
			else
				path.addAll(segment.subList(1, segment.size()));
		}
		//add points, with cumulative distance and z-values to profile
		for (int j=0; j<path.size(); j++) {
			Point2D thisPt = path.get(j);
			if (j != 0) {
				// add to the distance summation
				Point2D prevPt = path.get(j-1);
				Point2D[] thesepts =  {thisPt, prevPt};
				distance += GeneralUtils.distance(thesepts);
			}
			// get the z value at the point
			if (grid == null) {
				z = Float.NaN;
			} else {
				z = grid.getZ(thisPt);
			}
			profile.add(new Point4D(thisPt, distance, z));
		}
		currentPath = path;
	}

	public boolean isActive() {
		return active;
	}
	public void redraw() {
		synchronized( map.getTreeLock() ) {
			draw( map.getGraphics2D() );
		}
	}
	public void mouseMoved( MouseEvent evt ) {
		if(selected || evt.isControlDown())return;
		if( map.getCursor() != cursor ) map.setCursor( cursor );
		if( points.size()==0 )return;
		drawSeg();
		double[] xyz = (double[])points.get(points.size()-1);
		Point2D.Double pt = new Point2D.Double(xyz[0], xyz[1]);
		currentSegPath = getPath(pt, map.getScaledPoint( evt.getPoint() ));
		drawing = true;
		drawSeg();
	}
	
	// get either a straight line or a great circle list of points
	public ArrayList<Point2D> getPath(Point2D p1, Point2D p2) {
		ArrayList<Point2D> path = new ArrayList<Point2D>();
		double dist = map.getZoom()*Math.sqrt(
				Math.pow( p1.getX()-p2.getX(),2 ) +
				Math.pow( p1.getY()-p2.getY(),2 )); 
		int npt = (int)Math.ceil(dist);
		if (npt < 100) npt = 100;
		Projection proj = map.getProjection();
		
		if( dig.isStraightLine() ) {
			double dx = (p2.getX()-p1.getX())/(npt-1.);
			double dy = (p2.getY()-p1.getY())/(npt-1.);
			Point2D thisP;
			//generate the interpolated points, convert to lat/lon and add to the path
			for(int k=0 ; k<npt ; k++) {
				thisP = new Point2D.Double(
					p1.getX() + k*dx,
					p1.getY() + k*dy);
				path.add(proj.getRefXY(thisP));
			}
		} else {	//great circle
			Point2D q1 = proj.getRefXY(p1);
			Point2D q2 = proj.getRefXY(p2);
			XYZ r1 = XYZ.LonLat_to_XYZ(q1);
			XYZ r2 = XYZ.LonLat_to_XYZ(q2);
			double angle;
			//check if we are going the long way round and adjust the increment angle accordingly
			if (wrap > 0f && Math.abs((p2.getX() - p1.getX())) > wrap/2.) {
				//long way
				angle = -(Math.PI * 2 - Math.acos( r1.dot(r2) ))/(npt-1.);
			} else {
				angle = Math.acos( r1.dot(r2) )/(npt-1.);
			}

			r2 = r1.cross(r2).cross(r1).normalize();
			double s, c;
			XYZ r;
			for(int k=0 ; k<npt ; k++) {
				s = Math.sin(k*angle);
				c = Math.cos(k*angle);
				r = r1.times(c).plus( r2.times(s) );
				path.add(r.getLonLat());
			}
		}
		return path;
	}
	
	
	public void mouseDragged( MouseEvent evt ) {
		dig.insertBtn.setEnabled(dig.table.getSelectedRows().length == 1);
		if( currentPoint < 0)return;
		if( evt.isControlDown() ) {
			currentPoint=-1;
			return;
		}
		editShape = true;
		Point2D.Double p0 = (Point2D.Double)map.getScaledPoint( evt.getPoint() );
		// show dragged lat/lon on table
		points.set(currentPoint,  new double[]{p0.x, p0.y, getZ(p0)});
		fireTableDataChanged();
		updateAll();
	}
	public void drawSeg() {
		if(!drawing || currentSegPath == null || currentSegPath.size() < 2) return;
		synchronized( map.getTreeLock() ) {
			GeneralPath path = getGeneralPath(currentSegPath);
			Graphics2D g = map.getGraphics2D();
			g.setStroke( new BasicStroke( 2f/(float)map.getZoom() ));
			g.setXORMode( Color.white);
			double min = path.getBounds().getMinX();
			double max = path.getBounds().getMaxX();
			
			if( wrap>0. ) {
				Rectangle2D rect = map.getClipRect2D();
				double offset = 0.;
				while( min+offset>rect.getX() ) offset -= wrap;
				while( max+offset< rect.getX() ) offset += wrap;
				g.translate( offset, 0.);
				while( min+offset < rect.getX()+rect.getWidth() ) {
					g.draw( path );
					offset += wrap;
					g.translate( wrap, 0.);
				}
			} else {
				g.draw( path );
			}
		}
	}

	public void keyPressed( KeyEvent evt ) {
	}
	public void keyReleased( KeyEvent evt ) {
		if( evt.getKeyCode() == KeyEvent.VK_ENTER ) dig.finish();
		if( evt.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
			if( points.size()>0 ) {
				points.remove( points.size()-1 );
				fireTableStructureChanged();
				map.repaint();
			}
		}
	}
	public void keyTyped( KeyEvent evt ) {
	}
	public void setTable( int which ) {
		if( which == 1&& (profile==null || profile.size()==0) )return;
		table = which;
		fireTableStructureChanged();
	}
	public String getColumnName(int col) {
		if (grid == null) grid = map.getFocus();
		if (grid != null && grid.getUnits() != null && grid.getUnits() != "") {
			Z_GRIDNAME = grid.getDataType() + " (" + grid.getUnits() + ")";
			if (super.getColumnName(col).equals(Z_COL)) return Z_GRIDNAME;
		}
		return super.getColumnName(col);
	}	
	public int getRowCount() {
		if ( points == null && profile == null) return 0;
		if( table == 0 ) return points.size();
		return profile.size();
	}
	public int getColumnCount() {
		//enable duration column for Survey Planner
		if (dig.isSurveyPlanner()) {
			if (table == 1) return COLUMN_NAMES.split(",").length;
			// if importing, may have extra columns
			return getDataSet().header.size(); 
		}
		return 5;
	}
	
	public int getColumnIndex(String colName) {
		for (int i=0; i< getColumnCount(); i++) {
			if (getColumnName(i).equals(colName) || 
					(colName.equals(Z_COL) && getColumnName(i).equals(Z_GRIDNAME))) {
				return i;
			}
		}
		
		return 999;
	}
	
	public void reorderColumn(int newCol, String colName) {
		int oldCol = getDataSet().header.indexOf(colName);
		indexH.removeElement(oldCol);
		indexH.add(newCol, oldCol);
	}
	
	public Object getValueAt(int row, int column) {
		return getValueAt(row, column, table, false);
	}
	
	public Object getValueAt(int row, int column, int whichTable, boolean save) {
		
		//make sure columns are all left-aligned
		DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
		leftRenderer.setHorizontalAlignment(JLabel.LEFT);
		for (int col=0; col < dig.table.getColumnModel().getColumnCount(); col++) {
			dig.table.getColumnModel().getColumn(col).setCellRenderer(leftRenderer);
		}
		
		// determine number of decimal places based on the zoom level
		double zoom = map.getZoom();
		NumberFormat fmt = GeneralUtils.getZoomNumberFormat(zoom);
		int dp = fmt.getMaximumFractionDigits();
		if (save) {
			fmt.setMaximumFractionDigits(dp+1);
			fmt.setMinimumFractionDigits(dp+1);
		}
		NumberFormat fmt1 = NumberFormat.getInstance();
		fmt1.setMaximumFractionDigits(1);
		fmt1.setMinimumFractionDigits(1);
		
		NumberFormat fmt2 = NumberFormat.getInstance();
		fmt2.setMaximumFractionDigits(dp-2);
		fmt2.setMinimumFractionDigits(dp-2);
		
		if (column == getColumnIndex(DURATION_COL)) {
			double cumDist = Double.parseDouble((getValueAt(row, getColumnIndex(CUMULATIVE_COL), whichTable, save).toString().replace(",", "")));
			return dig.calculateDuration((int)Math.round(cumDist));
		}
		
		if( whichTable==1 ) {
			Point4D p4d = profile.get(row);
			if( column==getColumnIndex(Z_GRIDNAME) ) {
				if (Float.isNaN((float)p4d.getZ())) return "-";
				return fmt1.format(p4d.getZ());
			}
			if (column == getColumnIndex(CUMULATIVE_COL)) return fmt2.format(p4d.getD());
			Point2D.Double pt = (Point2D.Double) p4d.getPoint();
			if (column == getColumnIndex(LON_COL)) return fmt.format(pt.getX());
			if (column == getColumnIndex(LAT_COL)) return fmt.format(pt.getY());
			if (column == getColumnIndex(DISTANCE_COL)) {
				if (row == 0) return 0;
				Point2D.Double prev_pt = (Point2D.Double) profile.get(row-1).getPoint();
				Point2D.Double [] pts = {prev_pt, pt};
				return fmt.format(GeneralUtils.distance(pts));
				//return (int)GeneralUtils.distance(pts);
			}
			return "-";

		}
		if( row>=points.size() ) return null;
		double[] p = (double[])points.get(row);

		Point2D pt = map.getProjection().getRefXY( new Point2D.Double(p[0], p[1]) );
		if (column == getColumnIndex(LON_COL))  return fmt.format(pt.getX());
		else if (column == getColumnIndex(LAT_COL)) return fmt.format(pt.getY());
		else if(column == getColumnIndex(Z_GRIDNAME)) {
			if (Float.isNaN((float)p[2])) return "-";
			return fmt1.format(p[2]);
		}
		else if (column == getColumnIndex(DISTANCE_COL)) {
			double[] prev_p = (double[])points.get(row);
			if (row > 0) {
				prev_p = (double[])points.get(row-1);
			}
			Point2D prev_pt = map.getProjection().getRefXY( new Point2D.Double(prev_p[0], prev_p[1]) );
			Point2D[] pts = {prev_pt, pt};
			
			if (dig.isStraightLine()) {
				double d0 = 0, d1 = 0;
				if (profile != null) {
					for (Point4D profRow : profile) {
						if (profRow.getPoint().equals(prev_pt)) {
							d0 = profRow.getD();
						}
						if (profRow.getPoint().equals(pt)) {
							d1 = profRow.getD();
							break;
						}
					}
				}
				return (fmt2.format(d1-d0));
			}
						
			boolean longWay = false;
			if (wrap > 0f && Math.abs((p[0] - prev_p[0])) > wrap/2.) {
				longWay = true;
			}
			return fmt2.format(GeneralUtils.distance(pts, longWay));
		}
		else if (column == getColumnIndex(CUMULATIVE_COL)) {
			if (row == 0) return 0;
			double d = 0.;
			if (dig.isStraightLine()) {
				for (Point4D profRow : profile) {
					if (profRow.getPoint().equals(pt)) {
						d = profRow.getD();
						return (fmt2.format(d));
					}
				}
			}
			
			boolean longWay = false;
			for (int i=1; i<=row; i++) {
				double[] this_p = (double[])points.get(i);
				double[] prev_p = (double[])points.get(i-1);
				if (wrap > 0f && Math.abs((this_p[0] - prev_p[0])) > wrap/2.) {
					longWay = true;
				} else {
					longWay = false;
				}
				
				Point2D this_pt = map.getProjection().getRefXY( new Point2D.Double(this_p[0], this_p[1]) );
				Point2D prev_pt = map.getProjection().getRefXY( new Point2D.Double(prev_p[0], prev_p[1]) );
				Point2D[] pts = {prev_pt, this_pt};
				d += GeneralUtils.distance(pts, longWay);
			}
			return fmt2.format(d);
		}
		return super.getValueAt(row, column);
	}

	// reorder the table rows
	public void reorder(int fromIndex, int toIndex, int numRows) {
		// remove the points to be moved and store them in a list
		ArrayList<Object> movingPts = new ArrayList<Object>();
		// also reorder the displayToDataIndex (used when importing extra columns in SurveyPlanner) 
		Vector<Integer> movingRows = new Vector<Integer>();
		for (int i = 0; i < numRows; i++) {
			// when a point is removed, the indices get re-assigned,  
			// so we are always removing points[fromIndex]
			movingPts.add(points.remove(fromIndex));
			movingRows.add(displayToDataIndex.remove(fromIndex));
		}
		
		// add the moved points back at their new position
		if (toIndex > points.size()) {
			points.addAll(movingPts);
			displayToDataIndex.addAll(movingRows);
		} else {			
			points.addAll(toIndex, movingPts);
			displayToDataIndex.addAll(toIndex, movingRows);
		}
		// update profile, graph and tables
		fireTableDataChanged();
		updateAll();
	}
	
	// update profile, graph and tables
	public void updateAll() {
		getProfile();
		dig.profile.setLine( this );
		dig.graph.setPoints(dig.profile, 0);
		dig.table.revalidate();
		dig.tableSP.revalidate();
		dig.graph.revalidate();
		dig.graph.repaint();
		map.repaint();
		dig.table.clearSelection();
	}
	
	@Override
	public boolean isCellEditable(int row, int column) {
		if (dig.tabs[0].isSelected()) {
			if (column >= 2) return false;
			return true;
		}
		return false;
	}

	@Override
	public void setValueAt(Object value, int row, int col) {

		if(row>=points.size()) return;
		Double val;
		//check value is a double
		try {
			val = Double.parseDouble(value.toString());
		} catch(Exception e) {
			return;
		}
		
		double[] p = (double[])points.get(row);
		Point2D pt = map.getProjection().getRefXY( new Point2D.Double(p[0], p[1]) );
		Point2D mapPt = map.getProjection().getMapXY(pt);
		// need to convert lat/lon displayed in table to map coords
		// that are stored in points vector
		if( col==0 ) {
			mapPt = map.getProjection().getMapXY(val, pt.getY());
			GeneralUtils.wrapPoint(map, (Point2D.Double) mapPt);
		} else {
			// make sure latitude is in range
			if (val < -90 || val > 90) return;
			// getMapXY can;t handle 90 deg.
			if (val == 90) val = 89.9999;
			mapPt = map.getProjection().getMapXY(pt.getX(), val);
		}
		p = new double[]{mapPt.getX(), mapPt.getY(), getZ(mapPt)};
		points.set(row, p);
		// update profile, graph and tables
		updateAll();
	}
	
	/**
	 * Home made class that contains 2D point co-ords,
	 * a distance measurement, and the z value for the point.
	 * @author Neville Shane
	 *
	 */
	protected class Point4D extends Point2D{
		private Point2D p;
		private double d;
		private double z;
		
		public Point4D(Point2D p, double d, double z) {
			this.p = p;
			this.d = d;
			this.z =z;
		}
		
		@Override
		public double getX() {
			return p.getX();
		}

		@Override
		public double getY() {
			return p.getY();
		}
		
		public Point2D getPoint() {
			return p;
		}

		public double getZ() {
			return z;
		}
		
		public double getD() {
			return d;
		}
		
		@Override
		public void setLocation(double x, double y) {
			p.setLocation(x, y);
		}
		
	}
}

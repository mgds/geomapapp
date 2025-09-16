package haxby.db.pdb;

import haxby.db.XYGraph;
import haxby.db.XYPoints2;
import haxby.util.XBTable;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.geomapapp.util.Cursors;

public class PDBDataSetGraph implements 	XYPoints2,
										MouseMotionListener, 
										MouseListener,
										TableModelListener,
										ListSelectionListener{

	PDB pdb;
	final XYPoints2 xypoints;
	XYGraph parent;

	int[] oldSelection;
	int[] indicesToPaint = null;

	long focusTime = -1;

	int side = 0;
	double xScale,yScale;
	double parentXScale, parentYScale;
	double[] xRange, yRange;
	float minX,maxX,minY,maxY;
	String xTitle,yTitle;
	float[] x,y;
	float x0,y0;
	int x1,x2,y1,y2;
	float lastV=Float.NaN;
	int xIndex,yIndex;
	XBTable table;
	TableModel tm;
	AffineTransform dat;
	public JToggleButton autoResize=null;
	public JToggleButton lassoButton=null;
	JScrollPane sp;
	Polygon poly;
	Cursor lastCursor;

	public PDBDataSetGraph(PDB pdb, String xTitle, String yTitle, int xIndex, int yIndex,
						XBTable table){
		this.pdb = pdb;
		xypoints = this;
		this.xTitle=xTitle;
		this.yTitle=yTitle;
		this.xIndex=xIndex;
		this.yIndex=yIndex;
		this.table = table;
		tm = table.getModel();
		updateRange();
	}

	public void setParent(XYGraph xyg) {
		parent = xyg;
	}

	public double getPreferredXScale(int dataIndex) {
		return xScale;
	}
	public double getPreferredYScale(int dataIndex) {
		return yScale;
	}
	public double[] getXRange(int dataIndex) {
		return xRange;
	}
	public String getXTitle(int dataIndex) {
		return xTitle;
	}
	public double[] getYRange(int dataIndex) {
		return yRange;
	}
	public String getYTitle(int dataIndex) {
		return yTitle;
	}

	public void updateRange() {
		x = new float[tm.getRowCount()];
		y = new float[tm.getRowCount()];
		if ((autoResize!=null&&autoResize.isSelected())||autoResize==null) {
			maxX=Float.NaN;
			minX=Float.NaN;
			minY=Float.NaN;
			maxY=Float.NaN;
		}
		for (int i = 0; i < x.length; i++) {
			try {
				x[i] = Float.parseFloat(tm.getValueAt(i, xIndex).toString());
				y[i] = Float.parseFloat(tm.getValueAt(i, yIndex).toString());
				if (Float.isNaN(x[i])||Float.isNaN(y[i])||(autoResize!=null&&!autoResize.isSelected()))continue;
				if (Float.isNaN(minX)) minX = x[i];
				else if (x[i]<minX) minX=x[i];
				if (Float.isNaN(maxX)) maxX = x[i];
				else if (x[i]>maxX) maxX=x[i];
				if (Float.isNaN(minY)) minY = y[i];
				else if (y[i]<minY) minY=y[i];
				if (Float.isNaN(maxY)) maxY = y[i];
				else if (y[i]>maxY) maxY=y[i];
			} catch (Exception ex) {
				x[i] = Float.NaN;
				y[i] = Float.NaN;
			}
		}

		if ((xRange!=null&&yRange!=null)&&(autoResize==null||!autoResize.isSelected())) return;

		if (Float.isNaN(minX)) { minX = 0; maxX = 0; }
		if (Float.isNaN(minY)) { minY = 0; maxY = 0; }

		if (minY==maxY) {
			minY--;
			maxY++;
		}
		if (minX==maxX) {
			minX--;
			maxX++;
		}

		yRange= new double[] {minY-.05 * (maxY-minY), maxY+.05 * (maxY-minY)};
		xRange = new double[] {minX-.05 * (maxX-minX),maxX+.05 * (maxX-minX)};
		
		if (sp!=null){ 
			yScale = sp.getVisibleRect().height/(yRange[1]-yRange[0]);
			xScale = sp.getVisibleRect().width/(xRange[1]-xRange[0]);
		}
		else {
			yScale = 400./(maxY-minY);
			xScale = 400./(maxX-minX);
		}
	}

	public void plotXY(Graphics2D g, Rectangle2D bounds, double xScale,
			double yScale, int dataIndex) {
//		updateRange();
		parentXScale = xScale;
		parentYScale = yScale;

		dat=g.getTransform();
		g.setColor(Color.BLACK);
		x0 = (float)bounds.getX();
		y0 = (float)bounds.getY();

		if (indicesToPaint != null) {
			paintIndices(g, indicesToPaint, xScale, yScale);
			indicesToPaint = null;
			return;
		}

		float x1 = x0;
		float x2 = x1+(float)bounds.getWidth();
		if(x1>x2) {
			x1 = x2;
			x2 = x0;
		}

		double scale = 2;
		Arc2D.Double arc = new Arc2D.Double(scale * -1.5,
				scale * -1.5,
				scale * 3,
				scale * 3,
				0., 360., Arc2D.CHORD );

		boolean colorScaling = pdb.cst != null 
			&& pdb.cst.isShowing() 
			&& pdb.colorGrid.length == x.length;

		List selected = new LinkedList();
		for (int i = 0; i < x.length; i++) {
			if (Float.isNaN(x[i])||Float.isNaN(y[i])) continue;
			Color a=Color.GRAY,
					b=Color.BLACK;

			if (table.getSelectionModel().isSelectedIndex(i)) {
				selected.add(new Integer(i));
				continue;
			}

			if (colorScaling)
				if (Float.isNaN(pdb.colorGrid[i])) continue;
				else a = pdb.cst.getColor(pdb.colorGrid[i]);

			if (table.getSelectionModel().isSelectedIndex(i))
				a = Color.RED;

			AffineTransform at = g.getTransform();
			g.translate((x[i]-x0)*(float)xScale, (y[i]-y0)*(float)yScale);
			g.setColor(a);
			g.fill(arc);
			g.setColor(b);
			g.draw(arc);
			g.setTransform(at);
		}
		for (Iterator iter = selected.iterator(); iter.hasNext();) {
			int i = ((Integer) iter.next()).intValue();

			Color a= Color.BLACK,
					b=Color.RED;

			if (colorScaling)
				if (Float.isNaN(pdb.colorGrid[i])) continue;
				else a = pdb.cst.getColor(pdb.colorGrid[i]);

			AffineTransform at = g.getTransform();
			g.translate((x[i]-x0)*(float)xScale, (y[i]-y0)*(float)yScale);
			g.setColor(a);
			g.fill(arc);
			g.setColor(b);
			g.draw(arc);
			g.setTransform(at);
		}

		this.x1=(int)((minX-.03*(maxX-minX)-x0)*xScale);
		this.x2=(int)((maxX+.03*(maxX-minX)-x0)*xScale);
		this.y1=(int)((minY-.03*(maxY-minY)-y0)*yScale);
		this.y2=(int)((maxY+.03*(maxY-minY)-y0)*yScale);
		g.setColor(Color.BLUE);
		g.drawLine(this.x1,	y1,	this.x1,	y2);
		g.drawLine(this.x2, y1,	this.x2, 	y2);
		g.drawLine(this.x1, y1,	this.x2, 	y1);
		g.drawLine(this.x1, y2,	this.x2, 	y2);

		oldSelection = table.getSelectedRows();
	}

	public void mouseDragged(MouseEvent e) {
		if (side==0) { //If we are not dragging a side
			if (poly!=null && lassoButton.isSelected()){ // If we are drawing a lasso
				if (Math.abs(poly.xpoints[poly.npoints-1]-e.getX())<=1
						&&Math.abs(poly.ypoints[poly.npoints-1]-e.getY())<=1)  return;
				poly.addPoint(e.getX(), e.getY());
				drawLasso((XYGraph)e.getSource());
			} else 
				mouseMoved(e);
		}
		else drag(e.getPoint(),(XYGraph)e.getSource());
	}
	public void mouseMoved(MouseEvent evt) {
		if( !(evt.getSource() instanceof XYGraph) ) return;

		XYGraph graph = (XYGraph)evt.getSource();
		testForSide(evt.getPoint(), graph); 
	}

	public void mouseClicked(MouseEvent evt) {
		XYGraph graph = (XYGraph)evt.getSource();
		selectPoint(evt.getPoint(), graph);
	}
	public void mouseEntered(MouseEvent e) {
		mouseMoved(e);
	}
	public void mouseExited(MouseEvent e) {

	}

	public void mousePressed(MouseEvent e) {
		if (!initDrag(e.getPoint(), (XYGraph)e.getSource()) && lassoButton.isSelected()) { // If we aren't dragging a side
			// Make a lasso
			XYGraph g = (XYGraph) e.getSource();
			poly = new Polygon();
			poly.addPoint(e.getPoint().x, e.getPoint().y);
		}
	}
	public void mouseReleased(MouseEvent e) {
		if (side!=0) // If we are dragging a side
			apply(e.getPoint(), (XYGraph)e.getSource());
		else { 
			if (poly!=null && lassoButton.isSelected()) { // If we are drawing a lasso
				poly.addPoint(poly.xpoints[0], poly.ypoints[0]);
				drawLasso((XYGraph)e.getSource());
				selectLasso((XYGraph)e.getSource());
				poly=null;
			}
		}
	}

	/**
		Draws the last line in polygon poly to XYGraph xyg
		Uses XOR mode
	*/
	void drawLasso(XYGraph xyg) {
		synchronized (xyg.getTreeLock()) {
			Graphics2D g = (Graphics2D) xyg.getGraphics(); 
			g.setXORMode(Color.GRAY);
			int x1 = poly.xpoints[poly.npoints-2];
			int y1 = poly.ypoints[poly.npoints-2];
			int x2 = poly.xpoints[poly.npoints-1];
			int y2 = poly.ypoints[poly.npoints-1];
			g.drawLine(x1, y1, x2, y2);
		}
	}

	/**
		undraws the polygon represented by poly
	*/
	void unDrawLasso(XYGraph xyg){
		synchronized (xyg.getTreeLock()) {
			Graphics2D g = (Graphics2D) xyg.getGraphics(); 
			g.setXORMode(Color.GRAY);
			for(int i=1;i<poly.npoints;i++) {
				g.drawLine(poly.xpoints[i-1], poly.ypoints[i-1], poly.xpoints[i], poly.ypoints[i]);
			}
		}
	}

	void selectPoint(Point p, XYGraph xyg) {
		double x0 = xyg.getXAt(p);
		double y0 = xyg.getYAt(p);
		int n = 0;
		while (n<x.length&&(Float.isNaN(x[n])||Float.isNaN(y[n]))) {
			n++;
		}
		if (n>=x.length) return;

		double dist = Math.sqrt(Math.pow((x0-x[n])*parentXScale, 2)+Math.pow((y0-y[n])*parentYScale, 2));
		for (int i = n; i < x.length; i++) {
			if (Float.isNaN(x[i])||Float.isNaN(y[i])) continue;
			double dist2 = Math.sqrt(Math.pow((x0-x[i])*parentXScale, 2)+Math.pow((y0-y[i])*parentYScale, 2));
			if (dist2 < dist) { dist = dist2;  n = i;} 
		}
		// Added a max distance check
		if (dist > 5)
			table.getSelectionModel().clearSelection();
		else {
			table.getSelectionModel().setSelectionInterval(n, n);
			table.ensureIndexIsVisible(table.getSelectedRow());
		}
		
	}

	/**
		Selects the area represented by poly
	*/	
	void selectLasso(XYGraph xyg) {
		GeneralPath path = new GeneralPath();

		for (int i=0; i<poly.npoints; i++){
			Point2D p2 = new Point2D.Float (poly.xpoints[i], poly.ypoints[i]);
			Point2D point = new Point2D.Double (xyg.getXAt(p2), xyg.getYAt(p2));
			if (i==0) path.moveTo((float)point.getX(), (float) point.getY());
			else path.lineTo((float)point.getX(), (float) point.getY());
		}
		path.closePath();
		Rectangle2D r = path.getBounds();

		int n = 0;
		while (n<x.length&&(Float.isNaN(x[n])||Float.isNaN(y[n]))) {
			n++;
		}
		if (n>=x.length) return;
		
		table.getSelectionModel().setValueIsAdjusting(true);
		table.clearSelection();
		for (int i = n; i < x.length; i++) {
			if (Float.isNaN(x[i])||Float.isNaN(y[i])) continue;
			if (r.contains(x[i], y[i]) && path.contains(x[i], y[i])) {
				table.getSelectionModel().addSelectionInterval(i, i);
			}
		}
		table.getSelectionModel().setValueIsAdjusting(false);

		unDrawLasso(xyg);
		if (table.getSelectedRow() != -1)
			table.ensureIndexIsVisible(table.getSelectedRow());
	}

	void drawLine(XYGraph xyg) {
		if( Float.isNaN(lastV) || side==0) return;
		synchronized (xyg.getTreeLock()) {
			Graphics2D g = (Graphics2D)xyg.getGraphics();
			g.transform(dat);
			g.setXORMode( Color.white );
			g.setColor(Color.blue);
			if (side<0) g.drawLine((int)((lastV-x0)*xScale), y1, (int)((lastV-x0)*xScale), y2);
			else g.drawLine(x1, (int)((lastV-y0)*-yScale), x2, (int)((lastV-y0)*-yScale));
		}
	}

	/**
		initDrag returns true if a drag has been started, false otherwise
	*/
	public boolean initDrag(Point p, XYGraph xyg) {
		testForSide(p, xyg);
		if (side==0)return false;
		drawLine(xyg);
		if (side<0) lastV=(float)xyg.getXAt(p);
		else lastV=(float)xyg.getYAt(p);
		drawLine(xyg);
		return true;
	}

	public void drag(Point p,XYGraph xyg){
		if (side == 0) { initDrag(p, xyg); return; }
		drawLine(xyg);
		if (side<0) lastV=(float)xyg.getXAt(p);
		else lastV=(float)xyg.getYAt(p);
		drawLine(xyg);
	}

	public void apply(Point p,XYGraph xyg) {
		if (side==0)return;
		if (side<0){ 
			float x = (float)xyg.getXAt(p);
			if (side==-1) { minX=x; } 
			else if (side==-2) { maxX=x; }
		}
		else {
			float y = (float)xyg.getYAt(p);
			if (side==1) { minY=y; } 
			else if (side==2) { maxY=y; }
		}
		if (minY==maxY) {
			minY--;
			maxY++;
		}
		if (minX==maxX) {
			minX--;
			maxX++;
		}
		yScale = 400./(maxY-minY);
		yRange = new double[] {minY-.05 * (maxY-minY), maxY+.05 * (maxY-minY)};
		xScale = 400./(maxX-minX);
		xRange = new double[] {minX-.05 * (maxX-minX), maxX+.05 * (maxX-minX)};
		xyg.setPoints(this, 0);
		xyg.repaint(); 
		lastV=Float.NaN;
		autoResize.setSelected(false);
	}

	public void testForSide(Point p, XYGraph xyg) {
		xyg.requestFocus();
		side = closestSide(p,xyg);
		if (side == 0) return;
		else if (side<0) xyg.setCursor( Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR) );
		else xyg.setCursor( Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR) );
	}

	public int closestSide(Point p, XYGraph xyg) {
		int n=0;
		double dist = 5/xScale;
		double dist2 = Math.abs(xyg.getXAt(p) - minX+.03*(maxX-minX));
		if (dist2 < dist) {dist = dist2; n = -1;}
		dist2 = Math.abs(xyg.getXAt(p) - maxX-.03*(maxX-minX));
		if (dist2 < dist) {dist = dist2; n = -2;}
		dist = 5/yScale;
		dist2 = Math.abs(xyg.getYAt(p) - minY+.03*(maxY-minY));
		if (dist2 < dist) {dist = dist2; n = 1;}
		dist2 = Math.abs(xyg.getYAt(p) - maxY-.03*(maxY-minY));
		if (dist2 < dist) {dist = dist2; n = 2;}
		if (side!=0&&n==0)
			if (lassoButton!=null && lassoButton.isSelected())
				xyg.setCursor(Cursors.getCursor(Cursors.LASSO));
			else
				xyg.setCursor(Cursor.getDefaultCursor());
		return n;
	}

	public Iterator getData(int data){
		Container c = pdb.map.getParent();
		while (!(c instanceof Frame || c == null)) c = c.getParent();
		PDBOutputConfigDialog dialog = new PDBOutputConfigDialog((Frame) c,pdb, tm,xIndex,yIndex);
		final int indices[] = dialog.indices;
		if (indices==null) return null;
		
		return new Iterator() {
			int i=-1;
			public void remove() {
				throw new UnsupportedOperationException();
			}

			public boolean hasNext() {
				if (i<tm.getRowCount())
					if (i == -1) 
						return true;
					else if (i == x.length)
						return false;
					else
						while (Float.isNaN(x[i]) || Float.isNaN(y[i]))
							if (i == x.length - 1)
								return false;
							else
								i++;
				else 
					return false;

				return true;
			}

			public Object next() {
				StringBuffer s = new StringBuffer();
				if (i==-1)	{ // Build Header
					if (tm instanceof PDBSampleModel)
						s.append("Sample ID\t");
					else
						s.append("Analysis ID\t");
					s.append("Latitude\t");
					s.append("Longitude\t");
				} else {
					s.append(table.getRowHeader().getModel().getElementAt(i).toString() + "\t");

					PDBStation stat;
					if (tm instanceof PDBSampleModel) {
						PDBSampleModel sm = (PDBSampleModel) tm;
						stat = PDBStation.get(((PDBSample)sm.samples.get(i)).getStationNum());
					} else {
						PDBAnalysisModel am = (PDBAnalysisModel) tm;
						PDBAnalysis a = (PDBAnalysis) am.analyses.get(i);
						stat = PDBStation.get(a.getStationNum());
					}
					s.append(stat.getLatitude() + "\t" + stat.getLongitude() + "\t");
				}

				for (int z = 0;z<indices.length;z++){
					if (i==-1)	{ // Build Header
						s.append(tm.getColumnName(z));
					}
					else {
						s.append(tm.getValueAt(i, z));
					}
					if (z<indices.length-1) s.append("\t");
				}
				i++;
				return s.toString();
			}
		};
	}

	public void tableChanged(TableModelEvent e) {
		oldSelection = null;
		updateRange();
		if (e.getFirstRow() != -1) return;

		if (parent != null) {
			synchronized (this) {
				if (focusTime == -1) {
					focusTime = System.currentTimeMillis() + 1000;
					new Thread("Graph Focus Thread") {
						public void run() {
							while (System.currentTimeMillis() < focusTime)
								try {
									Thread.sleep(200);
								} catch (InterruptedException e) {
								}
							parent.setPoints(xypoints, 0);
							parent.repaint();
							focusTime = -1;
						}
					}.start();
				} else
					focusTime = System.currentTimeMillis() + 1000;
			}
		}
	}

	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) return;
		paintSelectionDifference();
	}

	private void paintSelectionDifference() {
		if (parent == null) return;
		int[] newSelection = table.getSelectedRows();

		if (oldSelection == null) {
			synchronized (parent.getTreeLock()) {
				indicesToPaint = newSelection;
				parent.paintComponent(parent.getGraphics(), false);
			}
			oldSelection = newSelection;
			return;
		}

		List diffList = new LinkedList();
		outer:
		for (int i = 0; i < oldSelection.length; i++) {
			for (int j = 0; j < newSelection.length; j++) {
				if (oldSelection[i] == newSelection[j]) {
					newSelection[j] = -1; 
					continue outer;
				}
			}
			diffList.add(new Integer(oldSelection[i])); 
		}

		for (int i = 0; i < newSelection.length; i++) {
			if (newSelection[i] != -1)
				diffList.add(new Integer(newSelection[i]));
		}

		int[] diff = new int[diffList.size()];
		Iterator iter = diffList.iterator();
		for (int i = 0; i < diff.length; i++)
			diff[i] = ((Integer)iter.next()).intValue();

		synchronized (parent.getTreeLock()) {
			indicesToPaint = diff;
			parent.paintComponent(parent.getGraphics(), false);
		}

		oldSelection = table.getSelectedRows();
	}

	private void paintIndices(Graphics2D g, int[] indices, double xScale, double yScale) {
		double scale = 2;
		Arc2D.Double arc = new Arc2D.Double(scale * -1.5,
				scale * -1.5,
				scale * 3,
				scale * 3,
				0., 360., Arc2D.CHORD );

		boolean colorScaling = pdb.cst != null 
							&& pdb.cst.isShowing() 
							&& pdb.colorGrid.length == x.length;

		List selected = new LinkedList();
		for (int j = 0; j < indices.length; j++){
			int i = indices[j];
			if (Float.isNaN(x[i])||Float.isNaN(y[i])) continue;
			Color a=Color.GRAY,
					b=Color.BLACK;

			if (table.getSelectionModel().isSelectedIndex(i)) {
				selected.add(new Integer(i));
				continue;
			}

			if (colorScaling)
				if (Float.isNaN(pdb.colorGrid[i])) continue;
				else a = pdb.cst.getColor(pdb.colorGrid[i]);

			AffineTransform at = g.getTransform();
			g.translate((x[i]-x0)*(float)xScale, (y[i]-y0)*(float)yScale);
			g.setColor(a);
			g.fill(arc);
			g.setColor(b);
			g.draw(arc);
			g.setTransform(at);
		}
		for (Iterator iter = selected.iterator(); iter.hasNext();) {
			int i = ((Integer) iter.next()).intValue();

			Color a=Color.BLACK,
					b=Color.RED;

			if (colorScaling)
				if (Float.isNaN(pdb.colorGrid[i])) continue;
				else a = pdb.cst.getColor(pdb.colorGrid[i]);

			AffineTransform at = g.getTransform();
			g.translate((x[i]-x0)*(float)xScale, (y[i]-y0)*(float)yScale);
			g.setColor(a);
			g.fill(arc);
			g.setColor(b);
			g.draw(arc);
			g.setTransform(at);
		}
	}
}

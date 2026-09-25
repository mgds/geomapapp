package haxby.db.custom;

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
import java.util.Vector;

import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

import org.geomapapp.util.Cursors;

import haxby.db.XYGraph;
import haxby.db.XYPoints2;

public class DataSetGraph implements 	XYPoints2,
										MouseMotionListener, 
										MouseListener {

	int[] indices;
	int[] rgbValue;
	int side = 0;
	double xScale,yScale;
	double[] xRange, yRange;
	float minX,maxX,minY,maxY;
	String xTitle,yTitle;
	float[] x,y;
	float x0,y0;
	int x1,x2,y1,y2;
	float lastV=Float.NaN;
	int xIndex,
		yIndex;
	UnknownDataSet ds;
	boolean scatter;
	AffineTransform dat;
	public JToggleButton autoResize=null;
	public JToggleButton lassoButton=null;
	JScrollPane sp;
	Polygon poly;
	Cursor lastCursor;

	public DataSetGraph(boolean scatter, String xTitle, String yTitle, int xIndex, int yIndex,
					UnknownDataSet ds){
		this.scatter=scatter;
		this.xTitle=xTitle;
		this.yTitle=yTitle;
		this.xIndex=xIndex;
		this.yIndex=yIndex;
		this.ds=ds;
		if (!scatter){
			Container c = ds.dataT.getParent();
			while (c!=null&&!(c instanceof JScrollPane)){
				c=c.getParent();
			}
			if (c!=null)sp=(JScrollPane)c;
		}
		updateRange();
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
		x = new float[ds.tm.displayToDataIndex.size()];
		y = new float[ds.tm.displayToDataIndex.size()];

		if ((autoResize!=null&&autoResize.isSelected())||autoResize==null) {
			maxX=Float.NaN;
			minX=Float.NaN;
			minY=Float.NaN;
			maxY=Float.NaN;
		}

		for (int i = 0; i < x.length; i++) {
			try {
				x[i] = Float.parseFloat(
					(ds.rowData.get(ds.tm.displayToDataIndex.get(i)))
						.get(xIndex).toString());
				y[i] = Float.parseFloat(
					(ds.rowData.get(ds.tm.displayToDataIndex.get(i)))
						.get(yIndex).toString());
				if (Float.isNaN(x[i])
						|| Float.isNaN(y[i])
						|| (autoResize!=null
						&& !autoResize.isSelected()))
					continue;

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
		//updateRange();
		dat=g.getTransform();
		g.setColor(Color.BLACK);
		x0 = (float)bounds.getX();
		y0 = (float)bounds.getY();
		float x1 = x0;
		float x2 = x1+(float)bounds.getWidth();
		if(x1>x2) {
			x1 = x2;
			x2 = x0;
		}

		if (!scatter) {
			int sx = 0;
			while (x[sx]<x1) sx++;
			if (sx>0) sx--;
			if (sx>0) sx--;

			int ex = x.length-1;
			while (x[ex]>x2) ex--;
			if (ex<x.length) ex++;
			if (ex<x.length) ex++;

			GeneralPath s = new GeneralPath();
			boolean first = true;
			for (int i = sx; i < ex; i++){
				//Don't plot points from table rows where Plot has been deselected
				if (!ds.isPlottable(i)) continue;
				ds.selected[i]=true;
				if (Float.isNaN(x[i])||Float.isNaN(y[i])) continue;
				if (first) {
					s.moveTo((x[i]-x0)*(float)xScale, (y[i]-y0)*(float)yScale);
					first = false;
					continue;
				}
				s.lineTo((x[i]-x0)*(float)xScale, (y[i]-y0)*(float)yScale);
			} g.draw(s);

			if (ex==x.length)ex--;
			ds.dataT.getSelectionModel().setSelectionInterval(sx, ex);
			ds.tableSP.getVerticalScrollBar().setValue((sx-1)*ds.dataT.getRowHeight());
		} else {
			double scale = ds.symbolSize/50.;
			Arc2D.Double arc = new Arc2D.Double(scale * -1.5,
					scale * -1.5,
					scale * 3,
					scale * 3,
					0., 360., Arc2D.CHORD );
			//GeneralPath s = new GeneralPath(arc);
			Vector<Integer> selectedIndex = new Vector<Integer>();
			if(indices!=null) {
				for (int z = 0; z < indices.length; z++){
					int i = indices[z];
					if (Float.isNaN(x[i])||Float.isNaN(y[i])) continue;
					Color a=null,b=Color.BLACK;
					if (ds.f!=null&&i<ds.f.length&&Float.isNaN(ds.f[i])){
						continue;
					}else if (ds.f!=null&&ds.cst!=null&&ds.cst.isShowing()&&i<ds.f.length){
						a = ds.cst.getColor(ds.f[i]);
					}else if (ds.rgbIndex > 0){
						parseRGB(i);
						a = new Color(rgbValue[0], rgbValue[1], rgbValue[2]);
					}else{
						a = Color.GRAY;
					}
					
					if (ds.f2!=null&&i<ds.f2.length&&Float.isNaN(ds.f2[i])) continue;
					else if (ds.f2!=null&&ds.sst!=null&&ds.sst.isShowing()&&i<ds.f2.length) {
						float r = (float) (scale * 1.5 * (ds.sst.getSizeRatio(ds.f2[i])));
						arc = new Arc2D.Double(-r, -r, r*2, r*2,
								0., 360., Arc2D.CHORD );
					} else {
						arc = new Arc2D.Double(scale * -1.5,
								scale * -1.5,
								scale * 3,
								scale * 3,
								0., 360., Arc2D.CHORD );
					}

					int index = ds.tm.displayToDataIndex.get(i);
					if (ds.selected[index]) {
						selectedIndex.add(i);
						continue;
					}
					AffineTransform at = g.getTransform();
					g.translate((x[i]-x0)*(float)xScale, (y[i]-y0)*(float)yScale);
					g.setColor(a);
					g.fill(arc);
					g.setColor(b);
					g.draw(arc);
					g.setTransform(at);
				}
				// For selected points color red
				for (int i : selectedIndex) {
					Color a=null,b=Color.WHITE;
					if (ds.f!=null&&i<ds.f.length&&Float.isNaN(ds.f[i])) continue;
					else if (ds.f!=null&&ds.cst!=null&&ds.cst.isShowing()&&i<ds.f.length) a = ds.cst.getColor(ds.f[i]);
					b=Color.RED;
					AffineTransform at = g.getTransform();
					g.translate((x[i]-x0)*(float)xScale, (y[i]-y0)*(float)yScale);
					g.setColor(a);
					g.fill(arc);
					g.setColor(b);
					g.draw(arc);
					g.setTransform(at);
				}
				indices=null;
				return;
			}
			// For points that have been deselected color gray or the rgb value
			for (int i = 0; i < x.length; i++){
				//Don't plot points from table rows where Plot has been deselected
				if (!ds.isPlottable(i)) continue;
				if (Float.isNaN(x[i])||Float.isNaN(y[i])) continue;
				Color a=null,b=Color.BLACK;
				if (ds.f!=null&&i<ds.f.length&&Float.isNaN(ds.f[i])) {
					continue;
				}else if (ds.f!=null&&ds.cst!=null&&ds.cst.isShowing()&&i<ds.f.length) {
					a = ds.cst.getColor(ds.f[i]);
				}else if (ds.rgbIndex > 0) {
					parseRGB(i);
					a = new Color(rgbValue[0], rgbValue[1], rgbValue[2]);
				}else {
					a = Color.GRAY;
				}

				if (ds.f2!=null&&i<ds.f2.length&&Float.isNaN(ds.f2[i])){
					continue;
				}else if (ds.f2!=null&&ds.sst!=null&&ds.sst.isShowing()&&i<ds.f2.length) {
					float r = (float) (scale * 1.5 * (ds.sst.getSizeRatio(ds.f2[i])));
					arc = new Arc2D.Double(-r, -r, r*2, r*2,
							0., 360., Arc2D.CHORD );
				} else {
					arc = new Arc2D.Double(scale * -1.5,
							scale * -1.5,
							scale * 3,
							scale * 3,
							0., 360., Arc2D.CHORD );
				}

				int index = ds.tm.displayToDataIndex.get(i);
				if (ds.selected[index]) {
					selectedIndex.add(i);
					continue;
				}
				AffineTransform at = g.getTransform();
				g.translate((x[i]-x0)*(float)xScale, (y[i]-y0)*(float)yScale);
				g.setColor(a);
				g.fill(arc);
				g.setColor(b);
				g.draw(arc);
				g.setTransform(at);
			}
			for (int i : selectedIndex) {
				Color a=null,b=Color.WHITE;
				if (ds.f!=null&&i<ds.f.length&&Float.isNaN(ds.f[i])) continue;
				else if (ds.f!=null&&ds.cst!=null&&ds.cst.isShowing()&&i<ds.f.length) a = ds.cst.getColor(ds.f[i]);
				b=Color.RED;

				if (ds.f2!=null&&i<ds.f2.length&&Float.isNaN(ds.f2[i])) continue;
				else if (ds.f2!=null&&ds.sst!=null&&ds.sst.isShowing()&&i<ds.f2.length) {
					float r = (float) (scale * 1.5 * (ds.sst.getSizeRatio(ds.f2[i])));
					arc = new Arc2D.Double(-r, -r, r*2, r*2,
							0., 360., Arc2D.CHORD );
				} else {
					arc = new Arc2D.Double(scale * -1.5,
							scale * -1.5,
							scale * 3,
							scale * 3,
							0., 360., Arc2D.CHORD );
				}
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
			g.drawLine(this.x1, y1, this.x1, y2);
			g.drawLine(this.x2, y1, this.x2, y2);
			g.drawLine(this.x1, y1, this.x2, y1);
			g.drawLine(this.x1, y2, this.x2, y2);
		}
		//ds.db.repaintMap();
	}

	// parse rgb column into rgbValue array
	public void parseRGB(int item){
		String rgbString = ds.rowData.get(ds.tm.displayToDataIndex.get(item)).get(ds.rgbIndex).toString();
		try {
		if (rgbString.indexOf(",") != -1) {
			String[] split = rgbString.split(",");
			if (split.length == 3) {
				float[] rgbF = new float[3];
				for (int j = 0; j < 3; j++)
					rgbF[j] = Float.parseFloat(split[j]);

				// Floats from 0-1
				rgbValue = new int[3];
				if (rgbF[0] <= 1 && rgbF[1] <= 1 && rgbF[2] <=1) {
					for (int j = 0; j < 3; j++)
						rgbValue[j] = (int) (rgbF[j] * 255);
				}else {
					for (int j = 0; j < 3; j++)
						rgbValue[j] = (int) rgbF[j];
				}
				// Clamp to 0-255 range
				for (int j = 0; j < 3; j++){
					rgbValue[j] = Math.max(rgbValue[j], 0);
					rgbValue[j] = Math.min(rgbValue[j], 255);
				}
			}
			return;
		}
		}catch (NumberFormatException ex) {
			rgbString = null;
		}
	}

	public void selectionChangedRedraw(boolean[] os){
		Vector<Integer> v = new Vector<Integer>();
		for (int i=0;i<x.length;i++) {
			if (Float.isNaN(x[i])||Float.isNaN(y[i])) continue;
			if (ds.f!=null&&i<ds.f.length&&Float.isNaN(ds.f[i])) continue;
			if (ds.f2!=null&&i<ds.f2.length&&Float.isNaN(ds.f2[i])) continue;

			int index = ds.tm.displayToDataIndex.get(i);
			if (ds.selected[index] != os[index]) v.add(new Integer(i));
		}
		indices=new int[v.size()];
		for (int i = 0; i < indices.length; i++) 
			indices[i] = v.get(i);
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
		if(ds.map==null) return;
//	else if(evt.getSource() instanceof XYGraph)  return;
		if( !(evt.getSource() instanceof XYGraph) ) return;
		XYGraph graph = (XYGraph)evt.getSource();
		testForSide(evt.getPoint(), graph);
		if (!scatter) {
			float x0 = (float)graph.getXAt( evt.getPoint() );
			int i;
			Integer displayIndex = -1;
			for (i = 0; i < ds.data.size(); i++) {
				if (!ds.selected[i]) continue;
				//Don't plot points from table rows where Plot has been deselected
				displayIndex = ds.tm.rowToDisplayIndex.get(i);
				if (displayIndex == null ||  !ds.isPlottable(displayIndex)) continue;
				if (displayIndex>x0) break;
			}
			if (i==ds.data.size())i--;
			UnknownData d = ds.data.get(i);
			ds.db.drawCurrentPoint();
			ds.db.point = new Point2D.Float(d.x,d.y);
			ds.db.drawCurrentPoint();
		} else {
			float x0 = (float)graph.getXAt( evt.getPoint() );
			float y0 = (float)graph.getYAt( evt.getPoint() );
			int n = 0;
			while (n<x.length&&(Float.isNaN(x[n])||Float.isNaN(y[n]))) {
				n++;
			}
			if (n>=x.length) return;
			double dist = Math.sqrt(Math.pow((x0-x[n])*xScale, 2)+Math.pow((y0-y[n])*yScale, 2));
			for (int i = n; i < x.length; i++) {
				//Don't plot points from table rows where Plot has been deselected
				if (!ds.isPlottable(i)) continue;
				if (Float.isNaN(x[i])||Float.isNaN(y[i])) continue;
				double dist2 = Math.sqrt(Math.pow((x0-x[i])*xScale, 2)+Math.pow((y0-y[i])*yScale, 2));
				if (dist2 < dist) { dist = dist2;  n = i;} 
			}
			// Added a max distance check
			if (dist > 5)
				return;
			//if (n==ds.data.size())n--;

			try {
				UnknownData d = ds.data.get(ds.tm.displayToDataIndex.get(n));
				ds.db.drawCurrentPoint();
				ds.db.point = new Point2D.Float(d.x,d.y);
				ds.db.drawCurrentPoint();
			} catch(Exception e) {
			}
		}
	}

	public void mouseClicked(MouseEvent evt) {
		if (scatter) {
			XYGraph graph = (XYGraph)evt.getSource();
			float x0 = (float)graph.getXAt( evt.getPoint() );
			float y0 = (float)graph.getYAt( evt.getPoint() );
			int n = 0;
			while (n<x.length&&(Float.isNaN(x[n])||Float.isNaN(y[n]))) {
				n++;
			}
			if (n>=x.length) return; 
			// Fixed forumla to scale properly
			double dist = Math.sqrt(Math.pow((x0-x[n])*xScale, 2)+Math.pow((y0-y[n])*yScale, 2));
			for (int i = n; i < x.length; i++) {
				if (Float.isNaN(x[i])||Float.isNaN(y[i])) continue;
				double dist2 = Math.sqrt(Math.pow((x0-x[i])*xScale, 2)+Math.pow((y0-y[i])*yScale, 2));
				if (dist2 < dist) { dist = dist2;  n = i;} 
			}
			// Added a max distance check
			if (dist > 5)
				ds.dataT.getSelectionModel().clearSelection();
			else {
				ds.dataT.getSelectionModel().setSelectionInterval(n, n);
				ds.tableSP.getVerticalScrollBar().setValue((n-1)*ds.dataT.getRowHeight());
			}
		}
	}
	public void mouseEntered(MouseEvent e) {
		mouseMoved(e);
	}
	public void mouseExited(MouseEvent e) {
//		ds.db.drawCurrentPoint();
//		ds.db.point = null;
//		//ds.db.drawCurrentPoint();
//		ds.db.repaintMap();
	}
	public void mousePressed(MouseEvent e) {
		if (!initDrag(e.getPoint(), (XYGraph)e.getSource()) && lassoButton != null && lassoButton.isSelected()) { // If we aren't dragging a side
			// Make a lasso
//			XYGraph g = (XYGraph) e.getSource();
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
			//System.out.println(x1+"\t"+y1+"\t"+x2+"\t"+y2);
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

		ds.dataT.getSelectionModel().setValueIsAdjusting(true);
		if (scatter) {
			int n = 0;
			while (n<x.length&&(Float.isNaN(x[n])||Float.isNaN(y[n]))) {
				n++;
			}
			if (n>=x.length) return;
			for (int i = n; i < x.length; i++) {
				if (Float.isNaN(x[i])||Float.isNaN(y[i])) continue;
				if (r.contains(x[i], y[i]) && path.contains(x[i], y[i])) {
					ds.dataT.getSelectionModel().addSelectionInterval(i, i);
				}
			}
		}

		unDrawLasso(xyg);
//		selectionChangedRedraw(ds.selected);

		ds.dataT.getSelectionModel().setValueIsAdjusting(false);

		if (ds.dataT.getSelectedRow() != -1)
			ds.dataT.ensureIndexIsVisible(ds.dataT.getSelectedRow());
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
		if (!scatter) return false;
		testForSide(p, xyg);
		if (side==0)return false;
		drawLine(xyg);
		if (side<0) lastV=(float)xyg.getXAt(p);
		else lastV=(float)xyg.getYAt(p);
		drawLine(xyg);
		return true;
	}

	public void drag(Point p,XYGraph xyg){
		if (!scatter) return;
		if (side == 0) { initDrag(p, xyg); return; }
		drawLine(xyg);
		if (side<0) lastV=(float)xyg.getXAt(p);
		else lastV=(float)xyg.getYAt(p);
		drawLine(xyg);
	}

	public void apply(Point p,XYGraph xyg) {
		if (!scatter)return;
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

	public Iterator<String> getData(int data){
		Container c = ds.map.getParent();
		while (!(c instanceof Frame || c == null)) c = c.getParent();
		DBOutputConfigDialog dialog = new DBOutputConfigDialog((Frame) c,ds,xIndex,yIndex);
		final int indices[] = dialog.indices;
		if (indices==null) return null;

		return new Iterator<String>() {
			int i=-1;
			public void remove() {
				throw new UnsupportedOperationException();
			}

			public boolean hasNext() {
				return i<ds.tm.displayToDataIndex.size();
			}

			public String next() {
				StringBuffer s = new StringBuffer();
				for (int z = 0;z<indices.length;z++){
					if (i==-1)	s.append(ds.header.get(indices[z]));
					else {
//						int x = ((Integer)ds.tm.displayIndex.get(i)).intValue();
						s.append((ds.rowData
									.get(ds.tm.displayToDataIndex.get(i)))
										.get(indices[z]));
					}
					if (z<indices.length-1) s.append("\t");
				}
				i++;
				return s.toString();
			}
		};
	}

	public void dispose() {
		indices = null;
		x = y = null;
		ds = null;
	}
}

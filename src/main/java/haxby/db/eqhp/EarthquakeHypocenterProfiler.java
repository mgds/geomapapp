package haxby.db.eqhp;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.geomapapp.util.XML_Menu;
import org.joda.time.DateTime;

import haxby.db.Database;
import haxby.db.custom.DBDescription;
import haxby.db.custom.OtherDBInputDialog;
import haxby.db.custom.UnknownData;
import haxby.db.custom.UnknownDataSet;
import haxby.db.dig.Digitizer;
import haxby.db.dig.DigitizerObject;
import haxby.db.dig.LineSegmentsObject;
import haxby.db.surveyplanner.SurveyLine;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.proj.Projection;
import haxby.util.GeneralUtils;
import haxby.util.XBTable;

public class EarthquakeHypocenterProfiler implements Database, ActionListener, MouseListener {
	
	private static final int ROUNDING_FACTOR = 10000;
	
	private Map<String, UnknownDataSet> data;
	private BidiMap<String, String> urlToName;
	private Map<String, String> nameToShape;
	private String[] usgsUrls;
	private XMap map;
	private int digitizingState;
	private Digitizer dig;
	private DigitizerObject mainLine;
	private SurveyLine lineAbove, lineBelow;
	private Shape selectArea;
	private boolean isStraightLine;
	private List<UnknownData> selectedData;
	
	private boolean isLoaded = false, isDataShowing = false, enabled = false;
	private JPanel contentPane, dataPane, digitizingPane;
	private String currentDataset;
	
	private JComboBox<String> dropdown;
	private JFormattedTextField gapDecider;
	private JButton digitizingBtn;
	
	private SurveyLine[] surveyLinesTest;
	
	public EarthquakeHypocenterProfiler(XMap mapIn) {
		data = new HashMap<>();
		urlToName = new DualHashBidiMap<>();
		nameToShape = new HashMap<>();
		isLoaded = false;
		map = mapIn;
		map.addMouseListener(this);
		digitizingState = 0;
		gapDecider = new JFormattedTextField(NumberFormat.getIntegerInstance());
		gapDecider.addActionListener(this);
		selectArea = null;
		isStraightLine = false;
		surveyLinesTest = null;
	}
	
	public String nameForUrl(String url) {
		if(null != urlToName && urlToName.containsKey(url)) {
			return urlToName.get(url);
		}
		return null;
	}
	
	public String urlForName(String name) {
		if(null != urlToName && urlToName.containsValue(name)) {
			return urlToName.getKey(name);
		}
		return null;
	}
	
	private void initContentPane() {
		contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		contentPane.setMaximumSize(new Dimension(200, contentPane.getMaximumSize().height));
	}
	
	private void initDataPane() {
		dataPane = new JPanel();
		dataPane.setLayout(new GridLayout(0,1));
		dataPane.setMaximumSize(new Dimension(dataPane.getMaximumSize().width, 200));
		dataPane.setPreferredSize(dataPane.getMaximumSize());
	}
	
	private void resetDataPane() {
		if(null == dataPane) {
			initDataPane();
		}
		else {
			dataPane.removeAll();
			isDataShowing = false;
		}
	}
	
	private void showData() {
		resetDataPane();
		if(null != currentDataset && urlToName.containsValue(currentDataset) && !data.containsKey(currentDataset)) {
			getData(urlToName.getKey(currentDataset), currentDataset);
		}
		if(null != currentDataset && data.containsKey(currentDataset) && !isDataShowing) {
			data.get(currentDataset).setSymbolShape(XML_Menu.getXML_Menu(currentDataset).symbol_shape);
			dataPane.add(data.get(currentDataset).tableSP);
			((MapApp)map.getApp()).addDBToDisplay(this);
			isDataShowing = true;
		}
		else {
			dataPane.repaint();
			isDataShowing = false;
		}
		map.repaint();
	}
	
	private void setIsDigitizing(boolean digitizing) {
		String text = digitizing ? "Cancel" : "Start Digitizing";
		Color textColor = digitizing ? new Color(128, 0, 0) : new Color(0, 128, 0);
		if(null == digitizingBtn) {
			digitizingBtn = new JButton(text);
			digitizingBtn.addActionListener(this);
		}
		else {
			digitizingBtn.setText(text);
		}
		digitizingBtn.setForeground(textColor);
		digitizingState = digitizing ? 2 : 0;
		if(digitizing) {
			if(null == dig) {
				dig = new Digitizer(map);
			}
			if(!dig.startStopBtn.isSelected()) {
				while(dig.objects.size() > 0) {
					dig.objects.removeElementAt(dig.objects.size()-1);
					dig.model.objectRemoved();
				}
				mainLine = null;
				lineAbove = null;
				lineBelow = null;
				surveyLinesTest = null;
				dig.startStopBtn.doClick();
				dig.redraw();
				map.repaint();
			}
			if(null != currentDataset && data.containsKey(currentDataset)) {
//				data.get(currentDataset).poly = null;
				data.get(currentDataset).dataT.clearSelection();
			}
			selectArea = null;
		}
		else {
			if(null != dig) {
				if(dig.startStopBtn.isSelected()) {
					dig.startStopBtn.setSelected(false);
					map.removeMouseListener(dig);
					map.removeMouseMotionListener(dig);
					if(dig.getCurObj() instanceof LineSegmentsObject) {
						map.removeMouseListener((LineSegmentsObject)dig.getCurObj());
						map.removeMouseMotionListener((LineSegmentsObject)dig.getCurObj());
					}
				}
			}
		}
	}
	
	private void refresh() {
		//get the parallel lines on either side
		calculateParallellLines(Integer.valueOf(String.valueOf(gapDecider.getValue())), false);
		selectArea = calcSelectedArea();
		selectedData = getSelected();
//		selectedData.stream().forEach(new Consumer<UnknownData>() {
//
//			@Override
//			public void accept(UnknownData t) {
//				t.rgb = new int[] {200, 0, 0};
//			}
//			
//		});
	}
	
	private void finishDigitizing() {
		setIsDigitizing(false);
		dig.objects.add(dig.getCurObj());
		mainLine = dig.getCurObj();
		dig.model.objectAdded();
		refresh();
		for(UnknownData d : selectedData) {
			double percent = getPercentAlongProfile(d);
			System.out.println("(" + percent + ", " + d.data + ")");
		}
		map.repaint();
	}
	
	private void calculateParallellLines(long gapKm, boolean useStraightLines) {
		SurveyLine.setIsStraightLine(useStraightLines);
		if(null != dig && null != dig.getCurObj()) {
			ArrayList<Point2D> path = ((LineSegmentsObject)dig.getCurObj()).getCurrentPath();
			Point2D startPt = path.get(0);
			Point2D endPt = path.get(path.size()-1);
			Point2D[] curPts = {startPt, endPt};
			Point2D[] ptsAbove = GeneralUtils.parallelLine(curPts, gapKm, (byte)1);
			Point2D[] ptsBelow = GeneralUtils.parallelLine(curPts, gapKm, (byte)-1);
//			lineAbove = new LineSegmentsObject(map, dig);
//			((LineSegmentsObject)lineAbove).appendPoints(((LineSegmentsObject)lineAbove).getPath(ptsAbove[0], ptsAbove[1]));
//			lineAbove.setVisible(true);
			lineAbove = new SurveyLine(map, ptsAbove[0].getY(), ptsAbove[0].getX(), ptsAbove[1].getY(), ptsAbove[1].getX());
			lineBelow = new SurveyLine(map, ptsBelow[0].getY(), ptsBelow[0].getX(), ptsBelow[1].getY(), ptsBelow[1].getX());
			lineAbove.plain = true;
			lineBelow.plain = true;
		}
	}
	
	private Shape calcSelectedArea() {
		if(null == mainLine || null == lineAbove || null == lineBelow || null == currentDataset || !data.containsKey(currentDataset)) {
			return null;
		}
		UnknownDataSet uds = data.get(currentDataset);
		uds.dataT.clearSelection();
		List<Point2D> mainPts = ((LineSegmentsObject)mainLine).getCurrentPath();
		final Point2D[] waypoints = new Point2D[] {
				new Point2D.Double(lineAbove.getStartLon(), lineAbove.getStartLat()),
				lineAbove.getEndPoint(),
				mainPts.get(mainPts.size()-1),
				lineBelow.getEndPoint(),
				new Point2D.Double(lineBelow.getStartLon(), lineBelow.getStartLat()),
				mainPts.get(0)
		};
		GeneralPath path = new GeneralPath();
		//uds.poly = new Polygon();
		Projection proj = map.getProjection();
		for(int i = 0; i < waypoints.length; i++) {
			if(0 == i%3) {
				ArrayList<Point2D> curPath = ((LineSegmentsObject)mainLine).getPath(proj.getMapXY(waypoints[i]), proj.getMapXY(waypoints[i+1]));
				for(int j = 0; j < curPath.size(); j++) {
					Point2D pt = proj.getMapXY(curPath.get(j));
					if(0 == i && 0 == j) {
						path.moveTo(pt.getX(), pt.getY());
					}
					else {
						path.lineTo(pt.getX(), pt.getY());
					}
				}
			}
			else {
				Point2D pt = proj.getMapXY(waypoints[i]);
				path.lineTo(pt.getX(), pt.getY());
			}
		}
		path.closePath();
		//uds.selectLasso();
		return path;
	}
	
	private List<UnknownData> getSelected() {
		if(null == currentDataset || !data.containsKey(currentDataset)) {
			return null;
		}
		if(null == selectArea) {
			selectArea = calcSelectedArea();
		}
		UnknownDataSet uds = data.get(currentDataset);
		XBTable table = uds.dataT;
		float wrap = (float)map.getWrap();
		Rectangle2D rect = map.getClipRect2D();
		float yMin = (float)rect.getY();
		float yMax = (float)(rect.getY() + rect.getHeight());
		float xMin = (float)rect.getX();
		float xMax = (float)(rect.getX() + rect.getWidth());
		List<UnknownData> selected = new ArrayList<>();
		table.getSelectionModel().setValueIsAdjusting(true);
		for(int i = 0; i < uds.tm.displayToDataIndex.size(); i++) {
			int index = uds.tm.displayToDataIndex.get(i);
			UnknownData ud = uds.data.get(index);
			float x = ud.x, y = ud.y;
			if(!Float.isNaN(x) && !Float.isNaN(y)) {
				if(wrap > 0f) {
					while( x>xMin+wrap ) x -= wrap;
					while( x<xMin ) x += wrap;
					while( x<xMax ) {
						if (rect.contains(x, y) && selectArea.contains(x, y)) {
							table.getSelectionModel().addSelectionInterval(i, i);
							//uds.selected[i] = true;
							selected.add(ud);
						}
						x += wrap;
					}
				}
				else {
					if( x>xMin && x<xMax ) {
						if (rect.contains(x, y) && selectArea.contains(x, y)) {
							table.getSelectionModel().addSelectionInterval(i, i);
							//uds.selected[i] = true;
							selected.add(ud);
						}
					}
				}
			}
		}
		table.getSelectionModel().setValueIsAdjusting(false);
		return selected;
	}
	
	public double getPercentAlongProfile(UnknownData datum) {
		if(null == mainLine || null == currentDataset || !data.containsKey(currentDataset)) {
			return -1;
		}
		UnknownDataSet uds = data.get(currentDataset);
		ArrayList<Point2D> curPath = ((LineSegmentsObject)mainLine).getCurrentPath();
		//find the closest interpolated point to the given point
		float[] dataLoc = datum.getPointLonLat(uds.lonIndex, uds.latIndex);
		Point2D dataLocPt = new Point2D.Float(dataLoc[0], dataLoc[1]);
		Line2D.Float closestSeg = new Line2D.Float(curPath.get(0), curPath.get(1));
		int closestSegIndex = 0;
		for(int i = 1; i+1 < curPath.size(); i++) {
			Line2D.Float curSeg = new Line2D.Float(curPath.get(i), curPath.get(i+1));
			if(closestSeg.ptSegDist(dataLocPt) > curSeg.ptSegDist(dataLocPt)) {
				closestSeg = curSeg;
				closestSegIndex = i;
			}
		}
		//find the closest point on the closest segment to the given point
		double slope = closestSeg.getX2() == closestSeg.getX1() ? (Double.NaN) : ((closestSeg.getY2()-closestSeg.getY1()) / (closestSeg.getX2()-closestSeg.getX1()));
		double perpSlope = Double.isNaN(slope) ? (0) : ((0 == slope)?(Double.NaN):(-1./slope));
		double howFarOnSeg = -1;
		//special case for if the line is perfectly horizontal or perfectly vertical
		if(0.0 == slope) {
			howFarOnSeg = (dataLocPt.getX() - closestSeg.getX1()) / (closestSeg.getX2()/closestSeg.getX1());
		}
		else if(0.0 == perpSlope) {
			howFarOnSeg = (dataLocPt.getY() - closestSeg.getY1()) / (closestSeg.getY2()/closestSeg.getY1());
		}
		//if it's not, then have to find the intersection point with some more complex math
		else {
			//need the distance
			double distToSeg = closestSeg.ptSegDist(dataLocPt);
			//get the angle of the shortest line
			double angleRad = Math.atan(perpSlope);
			double rise = distToSeg * Math.sin(angleRad),
					run = distToSeg * Math.cos(angleRad);
			Point2D intersectionPoint = new Point2D.Double(dataLocPt.getX() + run, dataLocPt.getY() + rise);
			howFarOnSeg = (intersectionPoint.getX() - closestSeg.getX1()) / (closestSeg.getX2() - closestSeg.getX1());
		}
		return (double)closestSegIndex/curPath.size() + howFarOnSeg;
	}

	@Override
	public void draw(Graphics2D g) {
		if(null != currentDataset && data.containsKey(currentDataset) && null != g) {
			new ArrayList<Map.Entry<String, UnknownDataSet>>(data.entrySet()).stream().forEach(new Consumer<Entry<String, UnknownDataSet>>() {
				@Override
				public void accept(Entry<String, UnknownDataSet> t) {
					String whichDataset = t.getKey();
					UnknownDataSet uds = t.getValue();
					uds.setEnabled(whichDataset.equals(currentDataset));
				}
			});
			if(null == data.get(currentDataset).getSymbolShape() && nameToShape.containsKey(currentDataset)) {
				//it keeps getting set back to null before being drawn, for no apparent reason
				//so here I am making SURE IT IS NOT NULL
				data.get(currentDataset).setSymbolShape(nameToShape.get(currentDataset));
			}
			data.get(currentDataset).draw(g);
			if(data.get(currentDataset).enabled && null != selectArea) {
				g.draw(selectArea);
//				for(int i = 1; i < data.get(currentDataset).poly.npoints; i++) {
//					g.drawLine
//				}
			}
		}
		if(null != mainLine) {
			mainLine.draw(g);
		}
		if(null != lineAbove) {
			lineAbove.draw(g);
		}
		if(null != lineBelow) {
			lineBelow.draw(g);
		}
		if(null != surveyLinesTest) {
			for(int i = 0; i < surveyLinesTest.length; i++) {
				if(null != surveyLinesTest[i]) {
					surveyLinesTest[i].draw(g);
				}
			}
		}
	}

	@Override
	public String getDBName() {
		return "Earthquake Hypocenter Profiler";
	}

	@Override
	public String getCommand() {
		return "earthquake_hypocenter_cmd";
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "Allows users to view and generate profiles of earthquake depths near a drawn line or great circle arc.";
	}
	
	private UnknownDataSet getData(String url, String name) {
		if(null == url) {
			if(null != name && data.containsKey(name)) {
				return data.get(name);
			}
			return null;
		}
		if(null == name) {
			String[] splitUrl = url.split("/");
			name = splitUrl[splitUrl.length-1];
			name = name.substring(0, name.lastIndexOf("."));
		}
		if(!urlToName.containsKey(url) || !urlToName.get(url).equals(name)) {
			urlToName.put(url, name);
		}
		if(data.containsKey(name)) {
			return data.get(name);
		}
		Container c = map.getParent();
		while(!(c instanceof Frame)) {
			c = c.getParent();
		}
		int datasetType = UnknownDataSet.ASCII_URL;
		//c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		OtherDBInputDialog dialog = new OtherDBInputDialog((Frame)c, name, url, datasetType);
		DBDescription description = dialog.desc;
		String tblStr = dialog.input.getText();
		String delim = dialog.getDelimeter();
		UnknownDataSet uds = new UnknownDataSet(description, tblStr, delim, MapApp.getApp().getMap());
		uds.config(true);
		if(nameToShape.containsKey(name)) {
			uds.setSymbolShape(nameToShape.get(name));
		}
		data.put(name, uds);
		//c.setCursor(Cursor.getDefaultCursor());
		return uds;
	}

	@Override
	public boolean loadDB() {
		if(!isLoaded) {
			if(null == contentPane) {
				initContentPane();
			}
			JMenu menu = ((JMenu)XML_Menu.getMenuItem(XML_Menu.getXML_Menu("Global (USGS-ANSS Catalog)")));
			List<String> usgsUrlsList = new ArrayList<>();
			for(int i = 0; i < menu.getItemCount(); i++) {
				JMenuItem item = menu.getItem(i);
				String text = item.getText();
				if(text.startsWith("Magnitude ")) {
					XML_Menu xmlMenu = XML_Menu.getXML_Menu(item);
					String url = (String) xmlMenu.layer_url;
					String shape = xmlMenu.symbol_shape;
					if(null == shape) {
						shape = "circle";
					}
					nameToShape.put(text, shape);
					usgsUrlsList.add(url);
					urlToName.put(url, text);
				}
			}
			//if there's nothing to load, no need to continue
			if(0 == usgsUrlsList.size()) {
				return false;
			}
			usgsUrls = usgsUrlsList.toArray(new String[0]);
			String[] names = urlToName.values().toArray(new String[0]);
			//sort in reverse order for now
			Arrays.sort(names);
			dropdown = new JComboBox<>(names);
			dropdown.insertItemAt("- Select One -", 0);
			dropdown.setSelectedIndex(0);
			contentPane.add(dropdown);
			dropdown.addActionListener(this);
			digitizingPane = new JPanel();
			digitizingPane.setVisible(false);
			contentPane.add(digitizingPane);
			setIsDigitizing(false);
			gapDecider.setValue(500);
			JLabel label = new JLabel("Max distance from survey line: ");
			JLabel units = new JLabel("km");
			digitizingPane.add(label);
			digitizingPane.add(gapDecider);
			digitizingPane.add(units);
			digitizingPane.add(digitizingBtn);
			isLoaded = true;
		}
		return true;
	}

	@Override
	public boolean isLoaded() {
		return isLoaded;
	}

	@Override
	public void unloadDB() {
		isLoaded = false;
	}

	@Override
	public void disposeDB() {
		urlToName.clear();
		data.clear();
		contentPane.removeAll();
		dataPane.removeAll();
		dropdown = null;
		digitizingBtn = null;
		contentPane = null;
		dataPane = null;
		if(null != dig && dig.isLoaded()) {
			dig.disposeDB();
			dig = null;
		}
		unloadDB();
		System.gc();
	}

	@Override
	public void setEnabled(boolean tf) {
		enabled = tf;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public JComponent getSelectionDialog() {
		if(null == contentPane) {
			initContentPane();
		}
		return contentPane;
	}

	@Override
	public JComponent getDataDisplay() {
		if(null == dataPane) {
			initDataPane();
		}
		return dataPane;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource().equals(dropdown)) {
			digitizingPane.setVisible(dropdown.getSelectedIndex()>0);
			if(dropdown.getSelectedIndex() > 0) {
				String name = dropdown.getItemAt(dropdown.getSelectedIndex());
				System.out.println("You selected " + name);
				String url = urlToName.getKey(name);
				System.out.println("Getting data from " + url);
				MapApp.anchor.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				getData(url, name);
				System.out.println("Got the data");
				currentDataset = name;
				showData();
				MapApp.anchor.setCursor(Cursor.getDefaultCursor());
				System.out.println("The data should be showing now");
			}
			else {
				currentDataset = null;
				showData();
			}
		}
		else if(e.getSource().equals(digitizingBtn)) {
			if(0 == digitizingState) {
				map.getMapTools().selectB.doClick();
				setIsDigitizing(true);
			}
			else {
				//dig.startStopBtn.doClick();
				setIsDigitizing(false);
			}
		}
		else if(e.getSource().equals(gapDecider) && null != mainLine) {
			refresh();
			map.repaint();
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		//digitizingState == 2: this is the first point
		//digitizingState == 1: this is the second/last point
		//digitizingState == 0: not digitizing
		//dig.setCurObjectSelected(true);
		if(digitizingState > 0) {
			digitizingState--;
			if(0 == digitizingState) {
				dig.passClickEvent(e);
				dig.getCurObj().redraw();
				finishDigitizing();
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

}

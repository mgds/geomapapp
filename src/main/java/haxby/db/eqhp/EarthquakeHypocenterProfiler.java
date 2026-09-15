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
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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
import haxby.db.custom.UnknownDataSet;
import haxby.db.dig.Digitizer;
import haxby.db.dig.DigitizerObject;
import haxby.db.dig.LineSegmentsObject;
import haxby.db.surveyplanner.SurveyLine;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.util.GeneralUtils;

public class EarthquakeHypocenterProfiler implements Database, ActionListener, MouseListener {
	
	private Map<String, UnknownDataSet> data;
	private BidiMap<String, String> urlToName;
	private Map<String, String> nameToShape;
	private String[] usgsUrls;
	private XMap map;
	private int digitizingState;
	private Digitizer dig;
	private DigitizerObject mainLine;
	private SurveyLine lineAbove, lineBelow;
	
	private boolean isLoaded = false, isDataShowing = false, enabled = false;
	private JPanel contentPane, dataPane;
	private String currentDataset;
	
	private JComboBox<String> dropdown;
	private JButton digitizingBtn;
	
	public EarthquakeHypocenterProfiler(XMap mapIn) {
		data = new HashMap<>();
		urlToName = new DualHashBidiMap<>();
		nameToShape = new HashMap<>();
		isLoaded = false;
		map = mapIn;
		map.addMouseListener(this);
		digitizingState = 0;
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
			data.get(currentDataset).setColor(Color.RED);
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
//					((LineSegmentsObject)dig.objects.elementAt(dig.objects.size()-1)).dispose();
					dig.objects.removeElementAt(dig.objects.size()-1);
					dig.model.objectRemoved();
				}
				mainLine = null;
				lineAbove = null;
				lineBelow = null;
				dig.startStopBtn.doClick();
				dig.redraw();
				map.repaint();
			}
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
	
	private void finishDigitizing() {
		setIsDigitizing(false);
		dig.objects.add(dig.getCurObj());
		mainLine = dig.getCurObj();
		dig.model.objectAdded();
		//get the parallel lines on either side
		drawParallelLines(500, false);
		map.repaint();
	}
	
	private void drawParallelLines(double gapKm, boolean useStraightLines) {
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
			System.out.println("Got the points for the parallel lines");
		}
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
			setIsDigitizing(false);
			digitizingBtn.setVisible(false);
			contentPane.add(digitizingBtn);
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
			digitizingBtn.setVisible(dropdown.getSelectedIndex()>0);
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

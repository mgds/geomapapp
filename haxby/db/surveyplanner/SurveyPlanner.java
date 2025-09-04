package haxby.db.surveyplanner;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.stream.IntStream;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.table.DefaultTableModel;

import haxby.db.Database;
import haxby.db.dig.Digitizer;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.util.BrowseURL;
import haxby.util.URLFactory;

public class SurveyPlanner extends JFrame implements Database, MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 1L;
	private XMap map;
	private boolean enabled;
	private boolean loaded;
	private SurveyPlannerDataDisplay display;
	public SurveyPlannerSelector spSel;
	private ArrayList<SurveyLine> surveyLines;
	protected DefaultTableModel tm;
	private static String SURVEY_PLANNER_HELP_URL = MapApp.BASE_URL+"/gma_html/help/survey_planner_help.html";
	private Digitizer dig;
	protected boolean waypoints = false;
	protected boolean importLines = false;
	private SurveyLine currentLine = null;
	protected boolean editLine = false;
	
	JDialog disclaimerDialog, helpDialog;
	
	public SurveyPlanner( XMap map ) {
		this.map = map;
		enabled = false;
		loaded = false;
		this.surveyLines = new ArrayList<SurveyLine>();

		// initialize the Digitizer for generating waypoints
		dig = new Digitizer(map);
		display = new SurveyPlannerDataDisplay(this, dig);
		tm = display.getTableModel();
		spSel = new SurveyPlannerSelector(this, dig);
	}


	public void addLine(SurveyLine line) {
		//first check if new, empty line
		if (Double.isNaN(line.getStartLat())) {
			Vector<Object> rowData = new Vector<Object>(Arrays.asList("-", "-", "-", "-", "-", "-","-","-","-",line));
			display.addRowToTable(rowData);
			surveyLines.add(line);
			return;
		}
		//get elevations
		double startElevation = getElevation(line.getStartLat(), line.getStartLon());
		double endElevation = getElevation(line.getEndLat(), line.getEndLon());
		//add elevations to survey line
		line.setElevations(startElevation, endElevation);
		//add to list
		surveyLines.add(line);
		
		if (!importLines) {
			//add to table
			Vector<Object> rowData = new Vector<Object>(Arrays.asList(Integer.toString(line.getLineNum()), Double.toString(line.getStartLon()), 
					Double.toString(line.getStartLat()), Double.toString(startElevation),Double.toString(line.getEndLon()), Double.toString(line.getEndLat()),
					Double.toString(endElevation), Integer.toString(line.getCumulativeDistance()), Double.toString(line.getDuration()), line));
			display.addRowToTable(rowData);
		}
	}
	
	public Integer getElevation(double lat, double lon) {
		if (Double.isNaN(lat) || Double.isNaN(lon)) return null;
		return (int) map.getFocus().getZ(new Point2D.Double(lon,lat));
	}
	
	public void clearLines() {
		//clear list
		surveyLines.clear();
		//reset static counters and accumulators in surveyLine
		SurveyLine.resetAll();	
		//reset table
		tm = new SurveyPlannerTableModel(this);
		display.setTableModel((SurveyPlannerTableModel) tm);
	}
	
	public void deleteLine(SurveyLine line) {
		surveyLines.remove(line);
	}
	
	public ArrayList<SurveyLine> getSurveyLines() {
		return surveyLines;
	}
	
	public String getDBName() {
		return "Waypoints and Survey Planner";
	}

	public String getCommand() {
		return "survey_planner_cmd";
	}

	public String getDescription() {
		return "Waypoints and Survey Planner";
	}
	
	public boolean didLoadGMRT() {
		return dig.didLoadGMRT;
	}
	
	public void setLoadedGMRT(boolean loaded) {
		dig.didLoadGMRT = loaded;
	}
	
	@Override
	public void draw(Graphics2D g) {
		if(!loaded) return;
		if (waypoints) {
			//draw waypoints
			dig.draw(g);
		} else {
			double zoom = map.getZoom();
			g.setStroke( new BasicStroke( 2f/(float)zoom ));
			for (SurveyLine sl : surveyLines) {
				sl.draw(g);
			}
		}
	}

	@Override
	public boolean loadDB() {
		if( loaded ) return loaded;
		displayDisclaimer();
		loaded = true;
		return loaded;
	}
	
	public void displayDisclaimer() {

		final Object[] options = {"Accept"};
		String disclaimerText = "<html> <p>The displayed maps, images, data tables and this portal are not to be used for navigation purposes.</p></html>";

		JOptionPane.showOptionDialog(MapApp.anchor, disclaimerText, "Waypoints and Survey Planner Disclaimer", 
				JOptionPane.OK_OPTION, JOptionPane.WARNING_MESSAGE,  null,  options, options[0]);
		
	}
	
	public void displayHelp() {
		String helpText = "";
		String s = "";
		URL helpURL = null;
		if(MapApp.BASE_URL.matches(MapApp.DEV_URL)){
			SURVEY_PLANNER_HELP_URL = SURVEY_PLANNER_HELP_URL.replace("http://app.", "http://app-dev.")
					.replace("https://app.", "https://app-dev.");
		}
		try {
			helpURL = URLFactory.url(SURVEY_PLANNER_HELP_URL);
			BufferedReader in = new BufferedReader( new InputStreamReader( helpURL.openStream() ) );

			while((s=in.readLine())!=null){
				helpText = helpText.concat(s);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
	    // for copying style
	    JLabel label = new JLabel();
	    Font font = label.getFont();

	    // create some css from the label's font
	    StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
	    style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
	    style.append("font-size:" + font.getSize() + "pt;");
		
	    //need to use editor pane and hyperlink listener so that we can include hyperlinks in help text
	    JEditorPane ep = new JEditorPane("text/html", "<html><body style=\"" + style + "\">" //
	            + helpText //
	            + "</body></html>");

	    ep.addHyperlinkListener(new HyperlinkListener()
	    {
	        @Override
	        public void hyperlinkUpdate(HyperlinkEvent e)
	        {
	            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
	            	BrowseURL.browseURL(e.getURL().toString());
	        }
	    });
		ep.setEditable(false);
		ep.setBackground(label.getBackground());

		//need to do this to make Help window non-modal so that it can be kept open
		//whilst the user works on the tool
		helpDialog = new JDialog(this,"Survey Planner Help");
		JOptionPane helpPane = new JOptionPane(ep, JOptionPane.PLAIN_MESSAGE); 
		helpPane.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				if (e.getPropertyName().equals("value")) {
					switch ((Integer)e.getNewValue()) {
					case JOptionPane.OK_OPTION:
						break;
					}
					helpDialog.dispose();
				}
			}
		});
		
		JScrollPane scrollPane = new JScrollPane(helpPane);
		ep.setCaretPosition(0);
		helpDialog.setContentPane(scrollPane);
		helpDialog.pack();
		helpDialog.setSize(700, 600);
		helpDialog.setLocationRelativeTo(this);
		helpDialog.setVisible(true);
		helpDialog.toFront();	

	}
	
	@Override
	public boolean isLoaded() {
		return loaded;
	}
	
	@Override
	public void unloadDB() {
		loaded = false;
	}
	
	@Override
	public void disposeDB() {
		setEnabled(false);
		map.removeMouseListener(this);
		map.removeMouseMotionListener(this);
		map.removeMouseListener(spSel);
		map.removeMouseMotionListener(spSel);
		clearLines();
		//reset all buttons and field on the right-side menu
		waypoints = false;
		SurveyLine.setIsStraightLine(false);
		if(didLoadGMRT()) {
			spSel.gridToggle.doClick();
			spSel.gridToggle.setSelected(false);
			setLoadedGMRT(false);
		}
		dig = new Digitizer(map);
		spSel = new SurveyPlannerSelector(this, dig);
		display = new SurveyPlannerDataDisplay(this, dig);
		tm = display.getTableModel();
		loaded = false;
		System.gc();
	}
	
	@Override
	public void setEnabled(boolean tf) {
		if( tf && enabled ) return;
		map.removeMouseListener( this );
		map.removeMouseMotionListener(this);
		map.addMouseListener(this);
		map.addMouseMotionListener(this);
		enabled = tf;
	}
	
	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public JComponent getSelectionDialog() {
		return spSel.getDialog();
	}
	@Override
	public JComponent getDataDisplay() {
		return display.getPanel();
	}
	
	//reinitialize data display (eg if switching to waypoints)
	public void initDataDisplay() {
		display.initDisplay();
		map.repaint();
	}

	public int getSurveyLineColumn() {
		return display.getSurveyLineColumn();
	}
	
	public DefaultTableModel getTableModel() {
		return tm;
	}
	
	public void setTableModel (SurveyPlannerTableModel tm) {
		display.setTableModel(tm);
		this.tm = tm;
	}
	
	public XMap getMap() {
		return map;
	}
	
	public void repaint() {
		//clear the map
		map.repaint();

		//redraw all survey lines
		for (SurveyLine sl : surveyLines) {
			sl.draw(map.getGraphics2D());
		}
	}

	public void mousePressed(MouseEvent e) {
		Point2D.Double p = (Point2D.Double)map.getScaledPoint(e.getPoint());
		currentLine = null;
		for(int i=0; i<surveyLines.size(); i++) {
			SurveyLine sl = surveyLines.get(i);
			//find if the mouse point coincides with the start or end point of a survey line
			if (sl.selectPointInLine(p)) {
				currentLine = sl;
				editLine = true;
			}
		}
	}
	public void mouseReleased(MouseEvent e) {
		editLine = false;
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
	}
	public void mouseMoved(MouseEvent e) {
	}
	public void mouseDragged(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)  && map.getMapTools().tb[0].isSelected() && currentLine != null) {
			Point2D.Double p = (Point2D.Double)map.getScaledPoint(e.getPoint());
			//update line
			currentLine.updateSelectedPoint(p);
			//show dragged lat/lon on table
			((SurveyPlannerTableModel)tm).updateRow(currentLine);
			tm.fireTableDataChanged();
			map.repaint();
		}
	}
	
	public void mouseClicked(MouseEvent e) {
		if (waypoints) return;
		if( e.getSource()==map ) {
			//will highlight the selected survey lines on the map and in the table
			if( e.isControlDown() ) return;
			SurveyLine sl = null;
			Point2D.Double p = (Point2D.Double)map.getScaledPoint( e.getPoint() );
			for(int i=0; i<surveyLines.size(); i++) {
				try {
					sl = surveyLines.get(i);
				} catch (Exception ex) {
					continue;
				}
				if( sl.select( p.x, p.y ) ) {
					sl.setSelected(true);
					display.table.setRowSelectionInterval(i, i);
				} else sl.setSelected(false);
			}
		} else if( e.getSource() == display.table ) {
			//will highlight the selected survey lines on the map
			int[] rows = display.table.getSelectedRows();
			for(int i=0; i<surveyLines.size(); i++) {
				final int num = i;
				//some fancy Java8 code!
				surveyLines.get(i).setSelected(IntStream.of(rows).anyMatch(x -> x == num));
			}
		}
		map.repaint();
	}
	void moveLayerToTop() {
		if (((MapApp)map.getApp()).getCurrentDB() == this) {
			((MapApp)map.getApp()).layerManager.moveToTop(this);
		}
	}
}

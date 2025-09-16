package haxby.db.surveyplanner;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Vector;

import haxby.db.custom.DBDescription;
import haxby.db.custom.DBTableModel;
import haxby.db.custom.UnknownDataSet;
import haxby.util.GeneralUtils;

public class SurveyPlannerTableModel extends DBTableModel {

	private static final long serialVersionUID = 1L;
	static final String LINE_NUM_COL = "Line Number";
	static final String START_LAT_COL = "Start Latitude";
	static final String START_LON_COL = "Start Longitude";
	static final String START_ELEVATION_COL = "Start Elevation";
	static final String END_LAT_COL = "End Latitude";
	static final String END_LON_COL = "End Longitude";
	static final String END_ELEVATION_COL = "End Elevation";
	static final String DISTANCE_COL = "Km Cumulative Distance";
	static final String DURATION_COL = "Duration (hrs)";
	static final String SURVEY_LINE_COL  ="surveyLine";
	static final String COLUMN_NAMES =  LINE_NUM_COL+","+START_LON_COL+","+START_LAT_COL+","+START_ELEVATION_COL+","+END_LON_COL+","+END_LAT_COL+","+
			END_ELEVATION_COL+","+DISTANCE_COL+","+DURATION_COL+","+SURVEY_LINE_COL;
	static final Vector<String> COLUMN_NAMES_VECTOR = new Vector<String>(Arrays.asList(COLUMN_NAMES.split(",")));
	
	private SurveyPlanner sp;
	
	public SurveyPlannerTableModel(SurveyPlanner sp) {
		super(new UnknownDataSet(new DBDescription("Survey Lines",0,""), COLUMN_NAMES, ",", sp.getMap()), false);
		this.sp = sp;
	}

	public SurveyPlannerTableModel(UnknownDataSet d, SurveyPlanner sp) {
		super(d, false);
		this.sp = sp;
	}

	@Override
	public Object getValueAt(int row, int col) {
		double zoom = sp.getMap().getZoom();
		NumberFormat fmt = GeneralUtils.getZoomNumberFormat(zoom);
		String colName= getColumnName(col);
		if (colName.matches(START_LAT_COL) || colName.matches(START_LON_COL)
				|| colName.matches(END_LAT_COL) || colName.matches(END_LON_COL)) {
			try{
				return fmt.format(Double.parseDouble((String)super.getValueAt(row, col)));
			}  catch (Exception e) {
				return (String)super.getValueAt(row, col);
			}
		}
		return super.getValueAt(row, col);
	}
	
	@Override
	public void setValueAt(Object value, int row, int col) {
		double v;
		//check entered value can be parsed as a double
		try {
			v = Double.parseDouble(value.toString());
		} catch (Exception e) {
			return;
		}
		//get the SurveyLine for this row
		try {
			SurveyLine line = (SurveyLine) getValueAt(row, getSurveyLineColumn());
			String colName= getColumnName(col);
			switch (colName) 
			{
				case LINE_NUM_COL: line.setLineNum((int) v); break; 
				case START_LAT_COL: line.setStartLat(v); break;
				case START_LON_COL: line.setStartLon(v); break;
				case END_LAT_COL: line.setEndLat(v); break;
				case END_LON_COL: line.setEndLon(v); break;
				default: break;
			}
		} catch(Exception e) {return;}
		
		super.setValueAt(value, row, col);
		
		//recalculate elevations
		recalculateElevations();
		
		//recalculate cumulative distances and durations
		recalculateRows();
		//repaint the map and survey lines		
		sp.repaint();
	}
	
	public void recalculateElevations() { 		
		try {
			for (int row=0; row<sp.getSurveyLines().size(); row++) {
				SurveyLine line = sp.getSurveyLines().get(row);
			
				double startElevation = sp.getElevation(line.getStartLat(), line.getStartLon());
				double endElevation = sp.getElevation(line.getEndLat(), line.getEndLon());
				//add elevations to survey line
				line.setElevations(startElevation, endElevation);
				if (Double.isNaN(startElevation)) {
					super.setValueAt("-", row, getColumnIndex(START_ELEVATION_COL));
				} else {
					super.setValueAt(startElevation, row, getColumnIndex(START_ELEVATION_COL));
				}
				if (Double.isNaN(endElevation)) {
					super.setValueAt("-", row, getColumnIndex(END_ELEVATION_COL));
				} else {
					super.setValueAt(endElevation, row, getColumnIndex(END_ELEVATION_COL));
				}
			}
		} catch (Exception e) {}
	}
	
	public void recalculateRows() {
		//recalculate cumulative distances and durations
		SurveyLine.resetTotalDistance();
		for (int i=0; i<sp.getSurveyLines().size(); i++) {
			SurveyLine sl = sp.getSurveyLines().get(i);
			sl.setCumulativeDistance(sl.calculateCumulativeDistance());
			super.setValueAt(sl.getCumulativeDistance(), i, getColumnIndex(DISTANCE_COL));
			sl.setDuration(sl.calculateDuration());
			super.setValueAt(sl.getDuration(), i, getColumnIndex(DURATION_COL));
		}
	}
	
	public void updateRow(SurveyLine sl) {
		//update row coordinates
		int row = sl.getLineNum() - 1;
		setValueAt(sl.getStartLon(), row, getColumnIndex(START_LON_COL));
		setValueAt(sl.getStartLat(), row, getColumnIndex(START_LAT_COL));
		setValueAt(sl.getEndLon(), row, getColumnIndex(END_LON_COL));
		setValueAt(sl.getEndLat(), row, getColumnIndex(END_LAT_COL));
		recalculateRows();
	}
	
	public int getSurveyLineColumn() {
		return getColumnIndex(SURVEY_LINE_COL);
	}

	/**
	 * return the column index for the given column name
	 * @param colName
	 * @return
	 */
	public int getColumnIndex(String colName) {
		for (int i=0; i< getColumnCount(); i++) {
			if (getColumnName(i).equals(colName)) {
				return i;
			}
		}
		return 999;
	}
	
	@Override
	public boolean isCellEditable(int row, int col) {
		if (getColumnName(col).equals(START_ELEVATION_COL) || 
				getColumnName(col).equals(END_ELEVATION_COL) ||
				getColumnName(col).equals(DISTANCE_COL) ||
				getColumnName(col).equals(DURATION_COL)) return false;
		return true;
	}
	
	public void reorderColumn(int newCol, String colName) {
		int oldCol = getDataSet().header.indexOf(colName);
		indexH.removeElement(oldCol);
		indexH.add(newCol, oldCol);
	}
}

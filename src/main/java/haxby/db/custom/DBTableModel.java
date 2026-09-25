package haxby.db.custom;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.table.DefaultTableModel;

import org.geomapapp.geom.GCTP_Constants;

import haxby.util.XBTable;

public class DBTableModel extends DefaultTableModel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	UnknownDataSet ds;
	public Map<Integer, Integer> rowToDisplayIndex = new HashMap<Integer, Integer>();
	public Vector<Integer> displayToDataIndex = new Vector<Integer>();
	public Vector<Integer> oldList = new Vector<Integer>();
	public Vector<Integer> indexH = new Vector<Integer>();
	public boolean editable;

	// Keeps track of sorted Col and direction
	private int lastSortedCol = -1;
	private boolean ascent = true;

	Rectangle2D rect = new Rectangle2D.Float();
	double zoom = 0;

	// Column sorter is used in sorting index's of row data
	// Column sorter compares two row's (Vector) based on the lastSortedCol index
	private Comparator<Integer> columnSorter;

	public DBTableModel(UnknownDataSet ds){
		this(ds, true);
	}
	
	public DBTableModel(UnknownDataSet ds, Boolean addPlotColumn){
		this.ds = ds;

		for (int i = 0; i < ds.rowData.size();i++)
			displayToDataIndex.add( i );

		for (int i = 0; i < ds.header.size(); i++)
			indexH.add( i );

		if (addPlotColumn) {
			addPlotColumn();
			updateRowToDisplayIndex();
		}
	}

	public void addPlotColumn() {
		//a boolean column to determine whether a datapoint should be plotted on the map
		ds.header.insertElementAt("Plot", 0);
		indexH.add(ds.header.size() - 1);
		for (int i=0; i<ds.data.size();i++) {
			ds.data.get(i).data.insertElementAt(true, 0);
			//also update copy of original data
			ds.origData.get(i).insertElementAt(true, 0);
		}	
	}
	/**
	 * Add Cumulative Distance to Data
	 */
	public void addCD(){
		if (ds.header.contains("Cumulative Distance (km)")) return;
		double radius = GCTP_Constants.major[0];
		ds.header.add("Cumulative Distance (km)");
		indexH.add(ds.header.size() - 1);
		Vector<Float> cd = new Vector<Float>(ds.data.size());
		cd.add(0, 0f);
		ds.data.get(0).data.add("0");
		int x;
		for (x=1;x<ds.data.size();x++) {
			UnknownData d = ds.data.get(x);
			if (!Float.isNaN(d.x)&&!Float.isNaN(d.y)) break;
			cd.add(x, 0f);
			ds.data.get(x).data.add("0");
		}
		for (int i = x; i < ds.data.size(); i++) {
			UnknownData d = ds.data.get(i-1);
			UnknownData d2 = ds.data.get(i);
			if (Float.isNaN(d.x)||Float.isNaN(d.y)||Float.isNaN(d2.x)||Float.isNaN(d2.y)) {
				cd.add(i,cd.get(i-1));
				d2.data.add(cd.get(i) + "");
			} else {
				Point2D.Double p = (Point2D.Double) ds.map.getProjection().getRefXY(new Point2D.Float(d.x,d.y));
				Point2D.Double p2 = (Point2D.Double) ds.map.getProjection().getRefXY(new Point2D.Float(d2.x,d2.y));
				while (p.x>180)p.x-=360;
				while (p.x<-180)p.x+=360;
				while (p2.x>180)p2.x-=360;
				while (p2.x<-180)p2.x+=360;

				p.x=Math.toRadians(p.x);
				p.y=Math.toRadians(p.y);
				p2.x=Math.toRadians(p2.x);
				p2.y=Math.toRadians(p2.y);
				
				float dx = (float) (radius * Math.sqrt( Math.pow(p2.y-p.y , 2) + 
						Math.pow( (p2.x-p.x) / Math.cos((p.y+p2.y)/2) , 2))) / 1000;
						
				cd.add(i,new Float(cd.get(i-1) + dx));
				d2.data.add(cd.get(i) + "");
			}
		}
		fireTableStructureChanged();
		//fireTableDataChanged();
		ds.dataT.repaint();
	}

	/**
	 * Remove Cumulative Distance 
	 */
	public void removeCD(){
		if (ds.header.get(ds.header.size()-1) != "Cumulative Distance (km)") return;
		for (int i =0; i < indexH.size();i++)
			if (indexH.get(i).equals(ds.header.size() - 1))
				indexH.remove(i);
		ds.header.remove(ds.header.size()-1);
		for (UnknownData d : ds.data) {
			d.data.remove(d.data.size()-1);
		}
		fireTableStructureChanged();
		//fireTableDataChanged();
	}

	public synchronized void setArea( Rectangle2D bounds, double zoom ) {
		if (bounds.equals(rect)) return;
		rect = bounds;

		Vector<Integer> v = new Vector<Integer>();
		int[] s = ds.dataT.getSelectedRows(); 

		for (int i=0;i<s.length;i++) {
			if (s[i] < displayToDataIndex.size())
				s[i] = displayToDataIndex.get(s[i]);
			else s[i] = -1;
		}
		displayToDataIndex.removeAllElements();
		double xMin = bounds.getX();
		double xMax = xMin+bounds.getWidth();
		double yMin = bounds.getY();
		double yMax = yMin+bounds.getHeight();
		double wrap = ds.map.getWrap();
		int z = 0;
		for(int i=0 ; i<ds.data.size() ; i++) {
			UnknownData d = ds.data.get(i);
			d.setVisible(true);
			boolean inBounds = checkDataBounds(d, xMin, xMax, yMin, yMax, wrap);
			if (!inBounds) inBounds = checkPolygonBounds(d, bounds, wrap);
			if (!inBounds) { 
				if (z<s.length&&i==s[z]) z++;
				continue;
			}

			displayToDataIndex.add(i);
			if (z<s.length&&i==s[z]) {
				v.add(displayToDataIndex.size()-1);
				z++;
			}
		}
		displayToDataIndex.trimToSize();
		oldList.clear();
		oldList.addAll(displayToDataIndex);
		
		ds.dataT.getSelectionModel().removeListSelectionListener(ds);

		// Each time we change the displayIndex we need to update
		// the sort as well
		if (lastSortedCol != -1) {
			ascent = !ascent;
			sortByColumn(lastSortedCol);
		}
		else {
			updateRowToDisplayIndex();
			fireTableDataChanged();
		}
		for (int i=0;i<v.size();i++) {
			z = v.get(i).intValue();
			ds.dataT.getSelectionModel().addSelectionInterval(z, z);
		}
		ds.dataT.getSelectionModel().addListSelectionListener(ds);
	}

	private boolean checkPolygonBounds(UnknownData d, Rectangle2D viewBounds, double wrap) {
		if (d.polyline == null) return false;
		Rectangle polyBounds = d.polyline.getBounds();
		polyBounds.x += d.x;
		polyBounds.y += d.y;

		while (true) {
			if (polyBounds.intersects(viewBounds))
				return true;

			if (wrap <= 0) break;
			if (polyBounds.x >= wrap) break;
			polyBounds.x += wrap;
		}
		return false;
	}

	private boolean checkDataBounds(UnknownData d, double xMin, double xMax,
			double yMin, double yMax, double wrap) {
		if( d.y<yMin || d.y>yMax ){
			return false;
		}
		double x = d.x;
		if( wrap>0. ) {
			while( x<xMin )x+=wrap;
		}
		if(x>xMax){
			return false;
		}
		return true;
	}

	// Sorts on column col, alternating between ascent and descent
	public void sortByColumn(int col) {
	try {	
		if (lastSortedCol == col) {
			ascent = !ascent;
		
		} else if ((lastSortedCol < col) || (lastSortedCol > col)) {
			ascent = true;
		}
		lastSortedCol = col;
		Collections.sort(displayToDataIndex, getColumnSorter());
		updateRowToDisplayIndex();
		fireTableDataChanged();
	} catch (IllegalArgumentException ae){
		ae.printStackTrace();
	}
	}

	private Comparator<? super Integer> getColumnSorter() {
		if (columnSorter == null) {
			final Pattern isDouble = Pattern.compile("^[-+]?[0-9]*\\.?[0-9]+$");
			columnSorter = new Comparator<Integer>() {
				public int compare(Integer arg0, Integer arg1) {
					int cmp = 0;

					Vector<Object> row0 = ds.rowData.get( arg0 );
					Vector<Object> row1 = ds.rowData.get( arg1 );

					if (getColumnClass(lastSortedCol) == String.class) {
											
						String obj0 = (String) row0.get(indexH.get(lastSortedCol));
						String obj1 = (String) row1.get(indexH.get(lastSortedCol));

						double d0,d1;
						d0 = d1 = Double.NaN;
	
						if (isDouble.matcher(obj0.trim()).matches() &&
								isDouble.matcher(obj1.trim()).matches())
						{
							// Try to make them numbers
							try {	
								d0 = Double.parseDouble(obj0);
								d1 = Double.parseDouble(obj1);
							} catch (NumberFormatException ex) {
							} catch (IllegalArgumentException ae2) {
								System.err.print("illegal arg");
							}
						}
						if (Double.isNaN(d0) || Double.isNaN(d1)) {
							cmp = obj0.compareToIgnoreCase(obj1);
						} else {
							cmp = (d0==d1 ? 0 : (d0 - d1 > 0 ? 1: -1)); //fixed bug to handle when both values are equal.
							// cmp = d0 - d1 > 0 ? 1 : -1;
						}
	
						
					} else if (getColumnClass(lastSortedCol) == Boolean.class) {
						Boolean obj0 = (Boolean) row0.get(indexH.get(lastSortedCol));
						Boolean obj1 = (Boolean) row1.get(indexH.get(lastSortedCol));
						cmp = obj0.compareTo(obj1);						
					}
					
					return ascent ? cmp : -cmp;
				}
			};
		}
		return columnSorter;
	}

	public void updateRowToDisplayIndex() {
		rowToDisplayIndex.clear();
		int i = 0;
		for (Integer rowIndex : displayToDataIndex) {
				rowToDisplayIndex.put(rowIndex, i++);
		}
	}

	public void updateDisplayToDataIndex() {
		int[] s = ds.dataT.getSelectedRows(); 

		displayToDataIndex.removeAllElements();
		double xMin = rect.getX();
		double xMax = xMin+rect.getWidth();
		double yMin = rect.getY();
		double yMax = yMin+rect.getHeight();
		double wrap = ds.map.getWrap();
		int z = 0;
		for(int i=0 ; i<ds.data.size() ; i++) {
			UnknownData d = ds.data.get(i);
			boolean inBounds = checkDataBounds(d, xMin, xMax, yMin, yMax, wrap);
			if (!inBounds) inBounds = checkPolygonBounds(d, rect, wrap);
			if (!inBounds) { 
				continue;
			}

			if (d.isVisible()) displayToDataIndex.add(i);
		}
		displayToDataIndex.trimToSize();
		ds.dataT.getSelectionModel().removeListSelectionListener(ds);

		// Each time we change the displayIndex we need to update
		// the sort as well
		if (lastSortedCol != -1) {
			ascent = !ascent;
			sortByColumn(lastSortedCol);
		}
		else {
			updateRowToDisplayIndex();
			fireTableDataChanged();
		}
		for (int i=0;i<s.length;i++) {
			z = s[i];
			ds.dataT.getSelectionModel().addSelectionInterval(z, z);
		}

		ds.dataT.getSelectionModel().addListSelectionListener(ds);
	}
	
	public void resetDisplayIndices() {
		displayToDataIndex.clear();
		displayToDataIndex.addAll(oldList);
		for (int i : oldList) {
			ds.data.get(i).setVisible(true);
		}
		updateRowToDisplayIndex();
	}
	
	public String getColumnName(int col) {
		return ds.header.get(indexH.get(col));
	}
	public int getRowCount() { 
		if (displayToDataIndex == null) return 1;
		return displayToDataIndex.size(); 
	}
	public int getColumnCount() { 
		if (indexH == null) return 1;
		return indexH.size(); }
	public Object getValueAt(int row, int col) {
		if (col == -1) return true;
		if (row >= displayToDataIndex.size() || col >= indexH.size()) return "-";
		if (indexH.get(col) >= ds.rowData.get(displayToDataIndex.get(row)).size()) return "-";
		return ds.rowData.get(displayToDataIndex.get(row))
				.get(indexH.get(col));
	}
	public boolean isCellEditable(int row, int col) {
		if (getColumnName(col) == XBTable.PLOT_COLUMN_NAME) return true;
		return editable;
	}

	public void setValueAt(Object value, int row, int col) {
		if (value instanceof Boolean) {
			ds.rowData.get(displayToDataIndex.get(row))
			.set(indexH.get(col), (Boolean)value);
		} else {
			ds.rowData.get(displayToDataIndex.get(row))
				.set(indexH.get(col), value.toString());
		}
		//if latitude or longitude is changed, recalculate the XY position of the point 
		if (col == ds.latIndex || col == ds.lonIndex) {
			ds.data.get(displayToDataIndex.get(row)).updateXY(ds.map, ds.lonIndex, ds.latIndex);
		}
		//repaint the map after a value has changed
		ds.map.repaint();
		fireTableCellUpdated(row, indexH.get(col));
	}

    public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }
    
    public UnknownDataSet getDataSet() {
    	return ds;
    }
    
	public void dispose() {
		rowToDisplayIndex.clear();
		displayToDataIndex.clear();
		indexH.clear();
		columnSorter = null;
		ds = null;
	}

}

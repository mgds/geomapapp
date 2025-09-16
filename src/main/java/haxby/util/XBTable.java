package haxby.util;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.JList;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.geomapapp.image.ColorComponent;
import org.geomapapp.image.ColorEditor;

public class XBTable extends JTable implements FocusListener,
						KeyListener {
	boolean tracksWidth;
	XBRowHeader rowHeader=null;
	MyCellRenderer renderer=null;
	int[] selectedIndices;
	MyCellRenderer corner;
	ColorComponent colorRenderer;
	ColorEditor colorEditor;
	public final static String PLOT_COLUMN_NAME = "Plot";
	
	public XBTable( TableModel model ) {
		super( model );
		if(model instanceof XBTableModel ) {
			rowHeader = new XBRowHeader(this);
			corner = new CornerCellRenderer(this);
		}
		initListeners();
	}
	public XBTable(Vector rowData, Vector columnNames, Vector rowNames) {
		super( rowData, columnNames);
		if( rowNames == null )return;
		Object[] rows = new Object[rowNames.size()];
		for( int k=0 ; k<rows.length ; k++) rows[k] = rowNames.get(k);
		rowHeader = new XBRowHeader(this, rows);
		corner = new MyCellRenderer();
		initListeners();
	}
	public XBTable(Object[][] rowData, Object[] columnNames, Object[] rowNames) {
		super( rowData, columnNames);
		if( rowNames == null )return;
		rowHeader = new XBRowHeader(this, rowNames);
		initListeners();
	}
	public XBRowHeader getRowHeader() {
		return rowHeader;
	}
	public JTableHeader getColumnHeader() {
		return tableHeader;
	}
	public void setCornerText(String text) {
		corner.setText(text);
	}
	void initListeners() {
		tracksWidth = super.getScrollableTracksViewportWidth();
		addKeyListener(this);
		if( rowHeader==null )return;
	//	getSelectionModel().addListSelectionListener( rowHeader );
		addMouseListener( rowHeader );
		addKeyListener( rowHeader );
		if (corner instanceof CornerCellRenderer) {
			((CornerCellRenderer)corner).addMouseListener((CornerCellRenderer)corner);
		}
		rowHeader.addMouseListener( rowHeader );
		rowHeader.setFixedCellHeight(getRowHeight());
		rowHeader.addFocusListener( this );
		renderer = new MyCellRenderer();
		renderer.setOpaque(true);
		renderer.setForeground(Color.black);
		renderer.setBackground(new Color(207,207,207));
		renderer.setFont(getFont());
		rowHeader.setCellRenderer(renderer);
		selectedIndices = new int[0];
	}
	public void removeListeners() {
		getSelectionModel().removeListSelectionListener(this);
		getColumnModel().removeColumnModelListener(this);
		removeKeyListener(this);
		if (rowHeader != null)
			rowHeader.removeFocusListener( this );
	}
	public void setRowHeight(int h) {
		super.setRowHeight(h);
		if(rowHeader!=null) rowHeader.setFixedCellHeight(h);
	}
	public void addNotify() {
		super.addNotify();
		Container c = getParent();
		while(c != null) {
			if( c instanceof JScrollPane ) {
				JScrollPane sp = (JScrollPane)c;
				if(rowHeader!=null) {
					sp.setRowHeaderView(rowHeader);
					sp.setCorner( sp.UPPER_LEFT_CORNER, corner );
				}
				return;
			}
			c = c.getParent();
		}
	}
	public void setFont(Font font) {
		super.setFont(font);
		if(rowHeader==null)return;
		renderer.setFont(font);
	}
	/*
	public void tableChanged( TableModelEvent evt ) {
		super.tableChanged( evt );
		if(rowHeader != null)rowHeader.repaint();
	}*/
	public boolean getScrollableTracksViewportWidth() {
		return tracksWidth;
	}
	public void setScrollableTracksViewportWidth(boolean tf) {
		tracksWidth = tf;
	}
	public int getScrollableUnitIncrement(Rectangle visibleRect,
					int orientation, int direction) {
		if( orientation==SwingConstants.VERTICAL ) {
			return super.getScrollableUnitIncrement( visibleRect,
					orientation, direction);
		}
		int x0 = visibleRect.x;
		if(direction<0) {
			int index = columnModel.getColumnIndexAtX(x0-1);
			for( int x=x0-2 ; x>0 ; x--) {
				if(columnModel.getColumnIndexAtX(x) != index) return x0-x;
			}
			return x0+1;
		} else {
			int index = columnModel.getColumnIndexAtX(x0+1);
			for( int x=x0+2 ; x<getPreferredSize().width ; x++) {
				if(columnModel.getColumnIndexAtX(x) != index) return x-x0-1;
			}
			return 0;
		}
	}
	public boolean isRequestFocusEnabled() {
		return true;
	}
	public void focusGained(FocusEvent e) {
		if(e.getComponent() instanceof JList)requestFocus();
	}
	public void focusLost(FocusEvent e) {
	}
	public void ensureIndexIsVisible( int index ) {
		Container c = getParent();
		while(c != null) {
			if( c instanceof JScrollPane ) {
				JScrollPane sp = (JScrollPane)c;
				JScrollBar bar = sp.getVerticalScrollBar();
				bar.setValue(index*getRowHeight());
				return;
			}
			c = c.getParent();
		}
	}
	public TableCellRenderer getDefaultRenderer(Class columnClass) {
		if( columnClass!=Color.class )return super.getDefaultRenderer(columnClass);
		return getColorRenderer();
	}
	ColorComponent getColorRenderer() {
		if( colorRenderer!=null ) return colorRenderer;
		colorRenderer = new ColorComponent( Color.lightGray);
		return colorRenderer;
	}
/*
	public TableCellEditor getDefaultEditor(Class columnClass) {
		if( columnClass!=Color.class )return super.getDefaultEditor(columnClass);
		return getColorEditor();
	}
	ColorEditor getColorEditor() {
		if( colorEditor!=null ) return colorEditor;
		colorEditor = new ColorEditor();
		return colorEditor;
	}
*/
	public void keyPressed(KeyEvent e) {
	}
	public void keyReleased(KeyEvent e) {
		if (e.isControlDown()&&(e.getKeyCode()==KeyEvent.VK_C||e.getKeyCode()==KeyEvent.VK_X)) {
			Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringBuffer s = new StringBuffer();
			for (int i=0;i<getColumnCount();i++)
				s.append(getColumnName(i)+"\t");
			s.append("\n");
			int sel[] = getSelectedRows();
			for (int i=0;i<sel.length;i++) {
				for (int j=0; j<getColumnCount();j++) {
					Object o = getValueAt(sel[i], j);
					if (o instanceof String && ((String)o).equals("NaN")) o = "";
					s.append(o+"\t");
				}
				s.append("\n");
			}
			StringSelection ss = new StringSelection(s.toString());
			try {
				cb.setContents(ss, ss);
			} catch (Exception ex) {
			}
			e.consume();
		}
	}
	public void keyTyped(KeyEvent e) {
	}
	
	@Override
	public void tableChanged( TableModelEvent evt ) {
		int column = evt.getColumn();
		try {
			String columnName = dataModel.getColumnName(column);
			int row = evt.getFirstRow();
			//change row color if Plot is checked/unchecked
			if (columnName == PLOT_COLUMN_NAME) {
				Boolean show = (boolean) getValueAt(row, getPlotColumnIndex());
				if (!show) {
					getSelectionModel().clearSelection();					
				}
				repaint();
			} 
		} catch(Exception ex) {}
		super.tableChanged( evt );
	}	
	

	/*
	@Override
	//don't include rows that have been deselected for plotting
	 public int[] getSelectedRows() {
        int iMin = selectionModel.getMinSelectionIndex();
        int iMax = selectionModel.getMaxSelectionIndex();

        if ((iMin == -1) || (iMax == -1)) {
            return new int[0];
        }

        int[] rvTmp = new int[1+ (iMax - iMin)];
        int n = 0;
        for(int i = iMin; i <= iMax; i++) {
            if (selectionModel.isSelectedIndex(i) && (boolean) getValueAt(i, 0) ){
                rvTmp[n++] = i;
            }
        }
        int[] rv = new int[n];
        System.arraycopy(rvTmp, 0, rv, 0, n);
        return rv;
	 }
	 */
	
	/*
	 * Return the column index for the plot column name.
	 * Return -1 if not found.
	 */
	public int getPlotColumnIndex() {
		return getColumnIndex(PLOT_COLUMN_NAME);
	}
	
	/*
	 * Return the column index for a given column name.
	 * Return -1 if not found.
	 */
	public int getColumnIndex(String columnName) {

		for (int col=0; col<getColumnCount(); col++) {
			if (getColumnName(col) == columnName) return col;
		}
		return -1;
	}
}

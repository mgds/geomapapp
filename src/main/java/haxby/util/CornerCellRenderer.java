package haxby.util;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;

// used in the corners of XBTables with XBTableModels
// allows sorting of rows if the corner cell is clicked.
public class CornerCellRenderer extends MyCellRenderer implements MouseListener {

	private static final long serialVersionUID = 1L;
	SimpleBorder border;
	XBTable table;
	
	public CornerCellRenderer(XBTable table) {
		border = new SimpleBorder();
		this.table = table;
		setBorder(BorderFactory.createCompoundBorder(
				border,
				BorderFactory.createEmptyBorder(0,5,0,5)));
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// if this is used with a sortable table, clicking on the corner will sort the rows
		if (e.getSource() == this) {
			if (table.getModel() instanceof SortableTableModel) {
				((SortableTableModel) table.getModel()).sortRows();
				table.updateUI();
			};
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

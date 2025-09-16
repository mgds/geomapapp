package haxby.util;

import javax.swing.*;

public class XBRowHeaderModel extends AbstractListModel {
	XBTable table;
	public XBRowHeaderModel( XBTable table ) {
		super();
		this.table = table;
	}
	public Object getElementAt(int index) {
		return ((XBTableModel)table.getModel()).getRowName( index );
	}
	public int getSize() {
		return table.getModel().getRowCount();
	}
}

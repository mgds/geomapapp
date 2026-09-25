package haxby.util;

import javax.swing.*;
import javax.swing.table.*;

public abstract class XBTableModel extends AbstractTableModel {
	public XBTableModel() {
		super();
	}
	public String getRowName( int row ) {
		return Integer.toString(row);
	}
}

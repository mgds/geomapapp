package org.geomapapp.gis.shape;

import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.JLabel;
import java.awt.Component;
import javax.swing.JTable;
import java.awt.Color;

public class InfoEntry implements TableCellRenderer {
	public String[] cmd;
	int column;
	TableModel model;
	JLabel label;
	public InfoEntry(int column, TableModel model, String[] cmd) {
		this.cmd = cmd;
		this.column = column;
		this.model = model;
		label = new JLabel();
		label.setBackground(Color.white);
		label.setForeground(Color.blue);
	}
	public Component getTableCellRendererComponent(JTable table,
			Object value,
			boolean isSelected,
			boolean hasFocus,
			int row,
			int col) {
		label.setText( (String)model.getValueAt(row, column) );
		return label;
	}
}

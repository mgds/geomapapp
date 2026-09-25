package haxby.db.custom;

import haxby.util.XBTable;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

public class HyperlinkTableRenderer implements TableCellRenderer {
	private DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();

	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		JLabel l = (JLabel) renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		boolean plotable;
		if (value != null && value instanceof String) {
			try {
				int plotColumn = ((XBTable) table).getPlotColumnIndex();
				 plotable = (boolean) table.getValueAt(row, plotColumn);
			} catch(Exception e) {
				plotable = true;
			}
			if (validURL((String)value) && plotable) {
				l.setText("<html><u><font color=\"blue\">" + l.getText() + "</font></u></html>");
			} else {
				l.setForeground(Color.BLACK);
			}
		}
		return l;
	}

	public static boolean validURL(String str) {
		/* 	Java 1.4 doesn't have contains in String so for now
		 	we'll use indexOf
		*/
		//if (str.contains(" ")) return false;
		//else if (str.contains("\t")) return false;
		//return str.contains("://"); 

		if (str.indexOf(" ") != -1)
			return false;
		if (str.indexOf("\t") != -1)
			return false;
		if ((str.indexOf("://") == -1))
			return false;

		return true;
	}
}

package org.geomapapp.image;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

public class ColorEditor extends DefaultCellEditor {
	//	extends AbstractCellEditor {
	//	implements javax.swing.table.TableCellEditor {
	Editor editor;
	public ColorEditor() {
		super(new JTextField());
		editor = new Editor();
	}
	public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected,
				int row, int column) {
		Color color = (Color)value;
		editor.set( table, color, row, column);
		return editor;
	}
	public Object getCellEditorValue() {
		return new Color(editor.rgb);
	}
	class Editor extends Component {
		ColorModPanel panel;
	//	JDialog dialog;
		JTable table;
		int rgb;
		int row;
		int column;
		public Editor() {
			rgb = Color.lightGray.getRGB();
			panel = new ColorModPanel(rgb);
		}
		void set( JTable table, Color color, int row, int column) {
			rgb = color.getRGB();
			panel.setDefaultRGB( rgb );
/*
			if( dialog==null || table!=this.table ) {
				Container c = table.getTopLevelAncestor();
				boolean frame = c instanceof Frame;
				dialog = new JDialog( frame ? (Frame)c : (Dialog)c);
				dialog.getContentPane().add(panel);
				JButton ok = new JButton("OK");
*/
				
			this.table = table;
			this.row = row;
			this.column = column;
		}
		public void paintComponent(Graphics g) {
			int rgb = panel.showDialog(table.getTopLevelAncestor());
			if( rgb==this.rgb ) {
				fireEditingCanceled();
				return;
			}
			table.getModel().setValueAt( new Color(rgb), row, column);
			fireEditingCanceled();
		//	fireEditingStopped();
		}
	}
}

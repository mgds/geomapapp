package org.geomapapp.gis.shape;

import javax.swing.JComponent;
import javax.swing.BorderFactory;
import javax.swing.table.TableCellRenderer;
import javax.swing.ListCellRenderer;
import javax.swing.JList;
import javax.swing.JTable;

import java.awt.*;

public class LineWidth extends JComponent 
			implements TableCellRenderer,
				ListCellRenderer {
	float lineWidth;
	boolean hasFocus, isSelected;
	public LineWidth(float lineWidth) {
		this.lineWidth = lineWidth;
		setBorder( BorderFactory.createEmptyBorder(1,1,1,1) );
		hasFocus = isSelected = false;
	}
	public float getLineWidth() {
		return lineWidth;
	}
	public Dimension getPreferredSize() {
		return new Dimension( 30, 16 );
	}
	public void paintComponent(Graphics g) {
		Rectangle r = g.getClipBounds();
		r.x++;
		r.y++;
		r.width -=2;
		r.height -=2;
		Graphics2D g2 = (Graphics2D)g;
		if( hasFocus || isSelected) g2.setColor(new Color(120,255,255));
		else g2.setColor( Color.white );
		g2.fill(r);
		g2.translate(1,1);
		g2.setStroke( new BasicStroke(lineWidth) );
		g.setColor(Color.black);
		int y = r.height/2;
		g.drawLine( 2, y, r.width-2, y);
	}
	public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, 
			boolean hasFocus, int row, int column) {
		try {
			LineWidth lw = (LineWidth)table.getModel().getValueAt(row, column);
			lineWidth = lw.getLineWidth();
		} catch(Exception e) {
			lineWidth = 1f+(float)row;
		}
		this.hasFocus = hasFocus;
		this.isSelected = isSelected;
		return this;
	}
	public Component getListCellRendererComponent(JList list,
			Object value, int index, boolean isSelected, boolean hasFocus) {
		try {
			LineWidth lw = (LineWidth)list.getModel().getElementAt(index);
			lineWidth = lw.getLineWidth();
		} catch (Exception e) {
			lineWidth = 1f+(float)index;
		}
		this.hasFocus = hasFocus;
		this.isSelected = isSelected;
		return this;
	}
	public String toString() {
		return Float.toString(lineWidth);
	}
	public boolean equals(Object o) {
		try {
			float w = ((LineWidth)o).getLineWidth();
			return w==lineWidth;
		} catch(Exception e) {
			return false;
		}
	}
}

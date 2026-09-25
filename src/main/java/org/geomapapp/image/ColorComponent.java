package org.geomapapp.image;

import java.awt.*;
import javax.swing.*;
import org.geomapapp.util.SimpleBorder;

public class ColorComponent extends JComponent
		implements javax.swing.table.TableCellRenderer {
	static SimpleBorder selected;
	static SimpleBorder unSelected;
	boolean select;
	Color color;
	public ColorComponent( Color color ) {
		this.color = color;
		if( unSelected==null )unSelected=new SimpleBorder();
		if( selected==null )selected=new SimpleBorder(true);
		setBorder(unSelected);
	}
	public void setSelected(boolean tf) {
		select = tf;
		setBorder( tf ?
			selected : unSelected);
		if(isVisible())repaint();
	}
	public boolean isSelected() {
		return select;
	}
	public Dimension getPreferredSize() {
		return new Dimension(22, 22);
	}
	public void setColor(Color color) {
		this.color = color;
		if( isVisible() )repaint();
	}
	public Color getColor() {
		return color;
	}
	public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		setSelected( isSelected );
		try {
			color = (Color)table.getValueAt( row, column);
		}catch(ClassCastException e) {
			color = Color.lightGray;
		}
		return this;
	}
	public void paintComponent(Graphics g) {
		Rectangle r = g.getClipBounds();
		Insets ins = getInsets();
		if( ins!=null) {
			r.x += ins.left;
			r.y += ins.top;
			r.width -= ins.left + ins.right;
			r.height -= ins.top + ins.bottom;
		}
		g.setColor(Color.black);
		g.drawRect( r.x+2, r.y+2, r.width-4, r.height-4);
		g.setColor(color);
		Graphics2D g2 = (Graphics2D)g;
		g2.fill(r);
	}
}

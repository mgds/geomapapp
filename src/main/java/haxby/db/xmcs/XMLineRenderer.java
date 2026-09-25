package haxby.db.xmcs;

import javax.swing.*;
import java.awt.*;

public class XMLineRenderer extends JLabel
		implements ListCellRenderer {
	Color selC;
	Font font1, font2;
	public XMLineRenderer() {
		setOpaque(true);
		selC = new Color( 204, 204, 255);
		font1 = new Font("SansSerif", Font.BOLD, 12);
		font2 = new Font("SansSerif", Font.PLAIN, 12);
	}
	public Component getListCellRendererComponent(
			JList list,
			Object value,
			int index,
			boolean isSelected,
			boolean cellHasFocus) {
		boolean highlight = false;;
		if( value instanceof XMLine ) {
			highlight = ( ((XMLine)value).getZRange()!=null );
		} else {
			highlight = true;
		}
		setBackground(isSelected ? selC : Color.lightGray);
		if( highlight ) {
			setFont( font1 );
			setForeground( Color.black );
		} else {
			setFont( font2 );
			setForeground( Color.darkGray);
		}
			setText(value.toString());
		return this;
	}
}
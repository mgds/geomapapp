package haxby.db.dig;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class DigCellRenderer extends JLabel
			implements ListCellRenderer {
	public DigCellRenderer() {
		setOpaque( true );
		setForeground( Color.black );
		setFont( new Font( "SansSerif", Font.PLAIN, 10 ));
	}
	public Component getListCellRendererComponent(
				JList list,
				Object value,
				int index,
				boolean isSelected,
				boolean cellHasFocus) {
		setBackground(isSelected ? light : Color.white);
	//	setBorder( BorderFactory.createLineBorder(
	//			isSelected ? Color.black : Color.lightGray ));
		try {
			DigitizerObject dob = (DigitizerObject)value;
			setText(dob.toString());
			setIcon( dob.isVisible() ? dob.getIcon() : dob.getDisabledIcon() );
			return this;
		} catch (ClassCastException ex) {
			setText(value.toString());
			return this;
		}
	}
	static Color light = new Color(224, 224, 255);
}

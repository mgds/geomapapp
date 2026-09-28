package haxby.util;
import java.awt.*;
import javax.swing.*;
public class MyCellRenderer extends JLabel implements ListCellRenderer {
	SimpleBorder border;
	public MyCellRenderer() {
		border = new SimpleBorder();
		setBorder(BorderFactory.createCompoundBorder(
				border,
				BorderFactory.createEmptyBorder(0,5,0,5)));
	//	setOpaque(true);
	//	setForeground(Color.black);
	//	setBackground(Color.lightGray);
	//	setBorder(BorderFactory.createCompoundBorder(
	//		BorderFactory.createMatteBorder(0,0,1,1,Color.gray),
	//		BorderFactory.createEmptyBorder(0,4,1,4)));
	}
	public Component getListCellRendererComponent(
			JList list, Object value, int index, 
			boolean isSelected, boolean cellHasFocus) {
		border.setSelected(isSelected);
		setText(value.toString());
		return this;
	}
}

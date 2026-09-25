package haxby.db.ice;

import javax.swing.*;
import java.awt.*;
import javax.swing.tree.*;

public class IceCellRenderer extends DefaultTreeCellRenderer {
	public IceCellRenderer() {
		super();
	}
	public Component getTreeCellRendererComponent(JTree tree,
			Object value, boolean sel, boolean expanded,
			boolean leaf,
			int row,
			boolean hasFocus) {
		if( !leaf ) return super.getTreeCellRendererComponent( tree,
				value, sel, expanded, leaf, row, hasFocus);
		IceCore core = (IceCore)((DefaultMutableTreeNode)value).getUserObject();
		setText(core.toString());
		if(core.highlight) {
			setForeground( Color.red );
		} else {
			setForeground( Color.black );
		}
		return this;
	}
}

package haxby.wms;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class LayerTreeModel implements TreeModel {
	private Layer rootLayer;
	
	public LayerTreeModel(Layer layer) {
		this.rootLayer = layer;
	}

	public void addTreeModelListener(TreeModelListener l) {
	}

	public Object getChild(Object parent, int index) {
		if (index < 0) return null;
		else if (parent instanceof Style) return null;
		else {
			Layer l = (Layer) parent;
			if (index >= l.getChildren().length + l.getStyles().length)
				return null;
			else if (index < l.getStyles().length)
				return l.getStyles()[index];
			else 
				return l.getChildren()[index - l.getStyles().length];
		}
	}

	public int getChildCount(Object parent) {
		if (parent instanceof Style) return 0;
		else {
			Layer l = (Layer) parent;
			return l.getChildren().length + l.getStyles().length;
		}
	}

	public int getIndexOfChild(Object parent, Object child) {
		if (parent instanceof Style) return -1;
		else if (parent == null || child == null) return -1;
		else {
			Layer l = (Layer) parent;
			if (child instanceof Style) { 
				for (int i = 0; i < l.getStyles().length; i++)
					if (l.getStyles()[i] == child) return i;
				return -1;
			} else {
				for (int i = 0; i < l.getChildren().length; i++)
					if (l.getChildren()[i] == child) return i + l.getStyles().length;
				return -1;
			}
		}
	}

	public Object getRoot() {
		return rootLayer;
	}

	public boolean isLeaf(Object node) {
		if (node instanceof Style) 
			return true;
		else {
			Layer l = (Layer) node;
			if (l.getChildren().length > 0) return false;
			else return l.getStyles().length <= 1;
		}
	}

	public void removeTreeModelListener(TreeModelListener l) {
	}

	public void valueForPathChanged(TreePath path, Object newValue) {
	}
}

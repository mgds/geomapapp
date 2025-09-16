package haxby.util;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

public class XBRowHeader extends JList 
			implements MouseListener, 
				ListSelectionListener,
				KeyListener {
	XBTable table;
	public XBRowHeader( XBTable table, Object[] headerNames ) {
		super(headerNames);
		this.table = table;
	}
	public XBRowHeader( XBTable table ) {
		super( new XBRowHeaderModel(table) );
		this.table = table;
	}
	public void mousePressed (MouseEvent evt) {
	}
	public void mouseReleased (MouseEvent evt) {
		if(evt.getSource() == table) {
			setSelectedIndices(table.getSelectedRows());
		} else if( evt.getSource()==this ) {
			int[] indices = getSelectedIndices();
			table.clearSelection();
			for(int i=0 ; i<indices.length ; i++) {
				table.addRowSelectionInterval(indices[i], indices[i]);
			}
			Container c = getParent();
			Point p = null;
			while( c != null ) {
				if( c instanceof JViewport ) {
					p = ((JViewport)c).getViewPosition();
				} else if( c instanceof JScrollPane ) {
					if( p==null ) return;
					JScrollBar sb = ((JScrollPane)c).getVerticalScrollBar();
					if(p.y != sb.getValue() )sb.setValue(p.y);
					return;
				}
				c = c.getParent();
			}
		}
	}
	public void mouseClicked (MouseEvent evt) {
	}
	public void mouseEntered (MouseEvent evt) {
	}
	public void mouseExited (MouseEvent evt) {
	}
	public void keyPressed(KeyEvent evt) {
//		setSelectedIndices(table.getSelectedRows());
	}
	public void keyReleased(KeyEvent evt) {
		setSelectedIndices(table.getSelectedRows());
	}
	public void keyTyped(KeyEvent evt) {
	}
	public void valueChanged(ListSelectionEvent e) {
		setSelectedIndices(table.getSelectedRows());
	}
}

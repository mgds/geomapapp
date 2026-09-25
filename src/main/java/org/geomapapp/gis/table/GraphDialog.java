package org.geomapapp.gis.table;

import javax.swing.*;
import java.util.Vector;

public class GraphDialog {
	TableDB db;
	int xCol, yCol;
	boolean connect;
	public GraphDialog(TableDB db) {
		this.db = db;
		xCol = yCol = -1;
		connect = false;
	}
	public int showDialog() {
		if( xCol==-1 )xCol=0;
		if( yCol==-1 )yCol=1;
		int nCol = db.getColumnCount();
		Vector xc = new Vector(nCol);
		Vector yc = new Vector(nCol);
		for( int k=0 ; k<nCol ; k++) {
			xc.add(db.getColumnName(k));
			yc.add(db.getColumnName(k));
		}
		JComboBox xcb = new JComboBox(xc);
		JComboBox ycb = new JComboBox(yc);
		JPanel panel = new JPanel(new java.awt.GridLayout(0,2));
		panel.add( new JLabel("X-column"));
		panel.add( xcb );
		panel.add( new JLabel("Y-column"));
		panel.add( ycb );
		JPanel p = new JPanel( new java.awt.BorderLayout());
		p.add( panel, "Center");
		JCheckBox con = new JCheckBox("connect");
		con.setSelected(connect);
		p.add( con, "South");
		int ok = JOptionPane.showConfirmDialog(
				db.createTable().getTopLevelAncestor(),
				p,
				"",
				JOptionPane.OK_CANCEL_OPTION);
		if( ok==JOptionPane.CANCEL_OPTION)return ok;
		xCol = xcb.getSelectedIndex();
		yCol = ycb.getSelectedIndex();
		connect = con.isSelected();
		return ok;
	}
	public int getXColumn() {
		return xCol;
	}
	public int getYColumn() {
		return yCol;
	}
	public boolean connect() {
		return connect;
	}
}

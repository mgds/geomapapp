package org.geomapapp.db.dsdp;

import java.awt.*;
import javax.swing.*;

public class JanusDialog {
	JPanel panel;
	JTextField leg, site, hole;
	JComboBox dataID;
	public JanusDialog() {
		panel = new JPanel(new GridLayout(0,2));
		panel.add( new JLabel("Leg") );
		leg = new JTextField("100");
		panel.add( leg );
		panel.add( new JLabel("Site") );
		site = new JTextField("625");
		panel.add( site );
		panel.add( new JLabel("Hole") );
		hole = new JTextField("A");
		panel.add( hole );
		panel.add( new JLabel("data") );
		dataID = new JComboBox();
		for( int k=0 ; k<Janus.description.length ; k++) {
			dataID.addItem(Janus.description[k][1]);
		}
		panel.add( dataID );
	}
	public int showDialog(Component comp) {
		return JOptionPane.showConfirmDialog(comp, panel, "ODP Hole Chooser", JOptionPane.OK_CANCEL_OPTION);
	}
	public String getLeg() {
		return leg.getText();
	}
	public String getSite() {
		return site.getText();
	}
	public String getHole() {
		return hole.getText();
	}
	public int getDataID() {
		return dataID.getSelectedIndex();
	}
	public static void main(String[] args) {
		JanusDialog d = new JanusDialog();
		int ok = d.showDialog(null);
		if( ok==JOptionPane.CANCEL_OPTION) System.exit(0);
		System.out.println( d.getLeg() +","+ d.getSite()+d.getHole() +", "+Janus.description[d.getDataID()][1]);
		System.exit(0);
	}
}

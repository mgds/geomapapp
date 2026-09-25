package haxby.db.pdb;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PDBSelectionDialog extends JPanel
			implements ActionListener {
	PDB pdb;
	BasicDialog basic;
	JTabbedPane tb;
	JFrame frame;
	public PDBSelectionDialog( PDB pdb ) {
		super( new BorderLayout() );
		this.pdb = pdb;
		basic = new BasicDialog(pdb);
		tb = new JTabbedPane(JTabbedPane.TOP);
		tb.add( "Filter Parameters", basic);
		add( tb, "Center");
		setPreferredSize(new Dimension(450, 900));
		setMinimumSize(new Dimension(450, 900));
		setMaximumSize(new Dimension(450, 900));
	}
	public JTabbedPane getPane() {
		return tb;
	}
	public void showDialog(int x, int y) {
	}
	public void actionPerformed( ActionEvent evt ) {
		frame.dispose();
	}
}

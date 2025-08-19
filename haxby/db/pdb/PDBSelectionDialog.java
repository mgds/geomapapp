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
		setPreferredSize(new Dimension(450, 400));
		setMinimumSize(getPreferredSize());
		setMaximumSize(getPreferredSize());
		setSize(getPreferredSize());
		tb.setSize(getPreferredSize());
		tb.setMinimumSize(getPreferredSize());
		tb.setMaximumSize(getPreferredSize());
		tb.setPreferredSize(getPreferredSize());
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

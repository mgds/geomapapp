package haxby.db.mgg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MGGSelection implements ActionListener {
	MGGTracks tracks;
	JScrollPane dialogPane;
	JCheckBox topoCB, gravCB, magCB;
	public MGGSelection( MGGTracks tracks ) {
		this.tracks = tracks;
		initDialog();
	}
	void initDialog() {
		JPanel panel = new JPanel(new GridLayout(0, 1));
		JButton b = new JButton("All");
		panel.add(b);
		b.addActionListener(this);
		b = new JButton("None");
		panel.add(b);
		b.addActionListener(this);

//		***** 1.6.2: Change the titles of the radio buttons to more clearly convey the data type 
//		being selected.
		topoCB = new JCheckBox("Depth", true);
		gravCB = new JCheckBox("Gravity", true);
		magCB = new JCheckBox("Magnetic", true);
//		***** 1.6.2

		topoCB.addActionListener(this);
		gravCB.addActionListener(this);
		magCB.addActionListener(this);
		panel.add(topoCB);
		panel.add(gravCB);
		panel.add(magCB);
		dialogPane = new JScrollPane(panel);
	}
	public void actionPerformed(ActionEvent evt) {
		String cmd = evt.getActionCommand();
		if( cmd.equals("All") ) {
			topoCB.setSelected(true);
			gravCB.setSelected(true);
			magCB.setSelected(true);
		} else if( cmd.equals("None")) {
			topoCB.setSelected(false);
			gravCB.setSelected(false);
			magCB.setSelected(false);
		}
		tracks.setTypes( topoCB.isSelected(), 
				gravCB.isSelected(), 
				magCB.isSelected() );
	}
	public JComponent getDialog() {
		return dialogPane;
	}
}
package haxby.db.ship;
import haxby.db.mgg.MGGTracks;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

public class ShipSelection implements ActionListener{
	ShipTracks tracks;
	JScrollPane dialogPane;
	
	public ShipSelection( ShipTracks tracks ) {
		this.tracks = tracks;
		initDialog();
	}
	
	void initDialog(){
		JPanel panel = new JPanel(new GridLayout(0, 1));
		dialogPane = new JScrollPane(panel);
	}
	
	public JComponent getDialog() {
		return dialogPane;
	}
	

	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

}

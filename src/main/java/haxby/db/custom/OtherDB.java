package haxby.db.custom;

import haxby.map.XMap;

import java.awt.Container;
import java.awt.Frame;
import java.util.Vector;

import javax.swing.event.PopupMenuListener;

public class OtherDB extends CustomDB implements PopupMenuListener{

	public OtherDB(XMap map) {
		super(map);
		// TODO Auto-generated constructor stub
	}

	public void load(){
/*
		Container c = map.getParent();
		while (!(c instanceof Frame)) c=c.getParent(); 
		OtherDBInputDialog dialog = new OtherDBInputDialog((Frame)c);
		if (dialog.input==null) return;
		UnkownDataSet d = new UnkownDataSet(dialog.desc, dialog.input.getText(), dialog.getDelimeter(), this);
		if (data == null) data = new Vector();
		data.add(d);
		dataPanel.removeAll();
		dataPanel.add(d.tp);
		box.addItem(d);
		box.setSelectedItem(d);
		select();
		toggleButtons();

//		GMA 1.4.8: Disable Bookmark button for hosted datasets
		if ( d.desc.type == -1 ) {
			bookB.setEnabled(false);
			System.out.println("Should be disabled");
		}

		colorB.setEnabled(d.station);
		scaleB.setEnabled(d.station);
		plotB.setSelected(d.plot);
		thumbsB.setSelected(d.thumbs);
*/

		Container c = map.getParent();
		while (!(c instanceof Frame)) c=c.getParent();
		DBInputDialog dialog = new DBInputDialog((Frame)c,currentLoadOption,titleOfDataset);
		currentLoadOption = null;
		dialog.setVisible(true);

		if (dialog.input==null) {
			dialog.dispose();
			return;
		}

		UnknownDataSet d = new UnknownDataSet(dialog.desc, dialog.input.getText(), dialog.getDelimeter(), this);
		dialog.dispose();

		if (dataSets == null) dataSets = new Vector<UnknownDataSet>();
		dataSets.add(d);
		dataPanel.removeAll();
		dataPanel.add(d.tp);
		box.addItem(d);
		box.setSelectedItem(d);
		select();
		updateButtonsState();
	}

	public String getDBName() {
		return "Other Hosted Datasets";
	}
}

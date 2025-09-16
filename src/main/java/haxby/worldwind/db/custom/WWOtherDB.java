package haxby.worldwind.db.custom;

import haxby.db.custom.DBInputDialog;
import haxby.map.XMap;

import java.awt.Container;
import java.awt.Frame;
import java.util.Vector;

public class WWOtherDB extends WWCustomDB {

	public WWOtherDB(XMap map) {
		super(map);
	}

	public void load(){
		Container c = map.getParent();
		while (!(c instanceof Frame)) c=c.getParent();
		DBInputDialog dialog = new DBInputDialog((Frame)c,currentLoadOption,titleOfDataset);
		currentLoadOption = null;
		dialog.setVisible(true);

		if (dialog.input == null) {
			dialog.dispose();
			return;
		}
		WWUnknownDataSet d = new WWUnknownDataSet(dialog.desc, dialog.input.getText(), dialog.getDelimeter(), this);
		dialog.dispose();
		d.setLayerSceneGraph(wwSceneGraph);
		d.setRenderableLayer(renderableLayer);
		if (dataSets == null) dataSets = new Vector();
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

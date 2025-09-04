package haxby.db.pdb;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.TableModel;

import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.XBTable;

public class SendToPetDB extends JButton {

	private static final long serialVersionUID = 1L;

	static String PETDB_SEARCH_STATION_PATH = PathUtil.getPath("PORTALS/PETDB_SEARCH_STATION_PATH");
	static String PETDB_SEARCH_SPECIMEN_PATH = PathUtil.getPath("PORTALS/PETDB_SEARCH_SPECIMEN_PATH");
	
	JTabbedPane pane;
	static String  buttonText = "<html><body><center>"
			+"View one selection in detail<br>"
			+"on the <B>PetDB</B> web page"
			+"</center></body></ntml>";

	public SendToPetDB(JTabbedPane pane) {
		super( buttonText );
		this.pane = pane;
		addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				showURL();
			}
		});
	}
	void showURL() {
		XBTable table = (XBTable) ((JScrollPane) pane.getSelectedComponent()).getViewport().getView();

		TableModel model = table.getModel();

		int[] selectedIndices = table.getSelectedRows();
		if(selectedIndices.length == 0) {
			JOptionPane.showMessageDialog(haxby.map.MapApp.anchor,
				"<html>Please select a row from the<br>table or " +
				"an item on the map to continue.</html>",
				"Warning",
				JOptionPane.WARNING_MESSAGE);
			return;
		} else if (selectedIndices.length > 1) {
			JOptionPane.showMessageDialog(haxby.map.MapApp.anchor,
					"<html>Too many items are selected!<br>" +
					"Please select one row from the<br>" +
					"table or one item on the map to<br>" +
					"continue.</html>",
					"Warning",
					JOptionPane.WARNING_MESSAGE);
				return;
		}

		String id = table.getRowHeader().getModel().getElementAt(selectedIndices[0]).toString();
		String url;

//		***** GMA 1.6.2: Bring up the new, correct PetDB URL for the selected item
		if (model instanceof PDBStationModel) {
			PDBStation station = PDBStation.idToStation.get(id);			
			url = PETDB_SEARCH_STATION_PATH + station.location;

		} else {
			if(model instanceof PDBAnalysisModel){
				// No Analysis url id yet for search url. Just use Compile url which is id2[0] 
				String id2[] = id.split(":");
				id = id2[0];
			}
			PDBSample sample = PDBSample.idToSample.get(id);
			url = PETDB_SEARCH_SPECIMEN_PATH + sample.specimenNumber;
		} 
		BrowseURL.browseURL(url);
	}
}
package haxby.worldwind.db.mgg;

import java.awt.event.ActionEvent;
import java.io.IOException;

import haxby.db.mgg.MGG;
import haxby.db.mgg.MGGData;
import haxby.db.mgg.MGGDataDisplay;
import haxby.map.XMap;

public class WWMGGDataDisplay extends MGGDataDisplay {
	public WWMGGDataDisplay(MGG tracks, XMap map) {
		super(tracks, map);
	}

	@Override
	protected MGGData loadMGGData(XMap map2, String leg,
			String loadedControlFiles) throws IOException {
		return 
			new WWMGGData(
				super.loadMGGData(map2, leg, loadedControlFiles), 
				(WWMGG) tracks);
	}
}

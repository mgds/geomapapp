package haxby.worldwind.db.dsdp;

import gov.nasa.worldwind.avlist.AVKey;
import haxby.worldwind.util.WWGTable;

import java.io.IOException;

import org.geomapapp.db.dsdp.DSDP;
import org.geomapapp.db.util.GTable;

public class WWDSDP extends DSDP {
	public WWDSDP() {
	}

	protected GTable createTable(String url) throws IOException {
		return new WWGTable(url);
	}

	public void repaintMap() {
		((WWGTable) db).processVisibility();
		((WWGTable) db).layer.firePropertyChange(AVKey.LAYER, null, ((WWGTable) db).layer);
	}
}

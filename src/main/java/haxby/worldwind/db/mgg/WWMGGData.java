package haxby.worldwind.db.mgg;

import java.awt.event.MouseEvent;

import haxby.db.mgg.MGGData;

public class WWMGGData extends MGGData {

	protected WWMGG mgg;
	
	public WWMGGData(MGGData data, WWMGG mgg) {
		super(data.map, 
				data.id,
				data.lon,
				data.lat,
				data.data[0],
				data.data[1],
				data.data[2]
				);
		
		this.mgg = mgg;
	}

	@Override
	protected void drawCurrentPoint() {
		mgg.setCurrentPoint(currentPoint);
	}
	
	@Override
	public void setXInterval(float x1, float x2) {
		super.setXInterval(x1, x2);
		
		mgg.setCurrentSegment(currentSeg);
	}
	
	@Override
	public void mouseMoved(MouseEvent evt) {
		super.mouseMoved(evt);
		
		mgg.setCurrentPoint(currentPoint);
	}

	@Override
	public void mouseExited(MouseEvent evt) {
		super.mouseExited(evt);
		
		currentPoint = null;
		mgg.setCurrentPoint(currentPoint);
	}
}

package haxby.worldwind.db.mb;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.LevelSet;
import haxby.worldwind.db.mgg.MGGTrackTiler;
import haxby.worldwind.layers.dynamic_tiler.DynamicImageTileLayer;

public class MBTileLayer extends DynamicImageTileLayer {
	public static final int TILE_SIZE = 512;
	
	public MBTileLayer(MBTrackTiler trackTiler) {
		super(makeLevels(6), trackTiler);
		
		this.setUseTransparentTextures(true);
	}
	
	public void dispose() {
		super.dispose();
	}
	
	public String getName() {
		return "MB Tracks Layer";
	}
	
	private static LevelSet makeLevels(int numLevels) {
		AVList params = new AVListImpl();
		
		params.setValue(AVKey.TILE_WIDTH, TILE_SIZE);
		params.setValue(AVKey.TILE_HEIGHT, TILE_SIZE);
		params.setValue(AVKey.DATA_CACHE_NAME, "mb_tracks");
		params.setValue(AVKey.SERVICE, "null");
		params.setValue(AVKey.DATASET_NAME, "mb_tracks");
		params.setValue(AVKey.FORMAT_SUFFIX, "null");
		params.setValue(AVKey.NUM_LEVELS, numLevels);
		params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
		params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(36), Angle.fromDegrees(36)));
		params.setValue(AVKey.SECTOR, Sector.FULL_SPHERE);
		
		return new LevelSet(params);
	}
}

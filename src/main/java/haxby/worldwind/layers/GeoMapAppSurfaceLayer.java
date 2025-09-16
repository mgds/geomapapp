package haxby.worldwind.layers;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileUrlBuilder;
import haxby.grid.GridImager;
import haxby.util.URLFactory;
import haxby.worldwind.WWMapApp;
import haxby.worldwind.layers.ColorScaleLayer.ColorScale;
import haxby.worldwind.layers.SunCompassLayer.SunAngle;

import java.net.MalformedURLException;
import java.net.URL;

import org.geomapapp.image.Palette;

public class GeoMapAppSurfaceLayer extends BasicScalingTiledImageLayer 
									implements SunAngle,
												ColorScale {//TiledImageLayer {
	
	private static final Palette defaultPalette = new Palette(GridImager.defaultRED, GridImager.defaultGREEN, GridImager.defaultBLUE, GridImager.defaultHT);
	
	public static final String BASE_URL = WWMapApp.VO_BASE_URL + "data/tiles/geographic/geographic_512/";
	public static final String CACHE_NAME = "GeoMapApp/base";
	
	public GeoMapAppSurfaceLayer() {
		super(makeLevels());
		this.setForceLevelZeroLoads(true);
		this.setRetainLevelZeroTiles(true);
		this.setUseTransparentTextures(true);
	}
	
	private static LevelSet makeLevels() 
	{
		AVList params = new AVListImpl();

		params.setValue(AVKey.TILE_WIDTH, 512);
		params.setValue(AVKey.TILE_HEIGHT, 512);
		params.setValue(AVKey.DATA_CACHE_NAME, CACHE_NAME);
		params.setValue(AVKey.SERVICE, "null");
		params.setValue(AVKey.DATASET_NAME, "geomapapp.base");
		params.setValue(AVKey.FORMAT_SUFFIX, ".jpg");
		params.setValue(AVKey.NUM_LEVELS, 7);
		params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
		params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(36d), Angle.fromDegrees(36d)));
		params.setValue(AVKey.SECTOR, Sector.FULL_SPHERE);
		
		params.setValue(AVKey.TILE_URL_BUILDER, new TileUrlBuilder() {
			public URL getURL(Tile tile, String imageFormat) throws MalformedURLException {
				StringBuffer sb = new StringBuffer(BASE_URL);
				sb.append(tile.getLevelNumber());
				sb.append("/");
				sb.append(tile.getRow());
				sb.append("/");
				sb.append(tile.getRow());
				sb.append("_");
				sb.append(tile.getColumn());
				sb.append(".jpg");
				URL url = URLFactory.url(sb.toString());
				return url;
			}
		});

		return new LevelSet(params);
	}
	
	public Double getSunAngle() {
		return new Double(-45);
	}
	
	public boolean isColorScaleValid() {
		return true;
	}
	
	public boolean isSunValid() {
		return true;
	}

	public String getTitle() {
		return "km";
	}
	
	public double getAnnotationFactor() {
		return 1/1000.0;
	}
	
	public float[] getRange() {
		return defaultPalette.getRange();
	}
	
	public Palette getPalette() {
		return defaultPalette;
	}
	
	@Override
	public String toString() {
		return "GeoMapApp";
	}
}

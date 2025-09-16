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
import haxby.util.URLFactory;
import haxby.worldwind.WWMapApp;
import haxby.worldwind.layers.ColorScaleLayer.ColorScale;

import java.net.MalformedURLException;
import java.net.URL;

import org.geomapapp.image.Palette;

public class SpreadingRateSurfaceLayer extends BasicScalingTiledImageLayer 
									implements ColorScale,
										InfoSupplier{//TiledImageLayer {
	
	private static final Palette rainbowPallete = new Palette(5);
	static {
		rainbowPallete.setRange(0, 150);
	}
	
	public static final String BASE_URL = WWMapApp.VO_BASE_URL + "data/tiles/geographic/spreading_rate_512/";
	
	public SpreadingRateSurfaceLayer() {
		super(makeLevels());
		this.setUseTransparentTextures(true);
		this.setOpacity(.5f);
	}
	
	private static LevelSet makeLevels() 
	{
		AVList params = new AVListImpl();

		params.setValue(AVKey.TILE_WIDTH, 512);
		params.setValue(AVKey.TILE_HEIGHT, 512);
		params.setValue(AVKey.DATA_CACHE_NAME, "GeoMapApp/Spreading_Rate/");
		params.setValue(AVKey.SERVICE, "null");
		params.setValue(AVKey.DATASET_NAME, "geomapapp.spreading_rate");
		params.setValue(AVKey.FORMAT_SUFFIX, ".png");
		params.setValue(AVKey.NUM_LEVELS, 3);
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
				sb.append(".png");
				URL url = URLFactory.url(sb.toString());
				return url;
			}
		});

		return new LevelSet(params);
	}
 
	public boolean isColorScaleValid() {
		return true;
	}

	public String getTitle() {
		return "mm/a";
	}
	
	public double getAnnotationFactor() {
		return 1;
	}
	
	public float[] getRange() {
		return new float[] {0, 150};
	}
	
	public Palette getPalette() {
		return rainbowPallete;
	}
	
	@Override
	public String toString() {
		return "Spreading Rate";
	}
	
	public String getInfoURL() {
		return "https://www.ngdc.noaa.gov/mgg/ocean_age/ocean_age_2008.html";
	}
}

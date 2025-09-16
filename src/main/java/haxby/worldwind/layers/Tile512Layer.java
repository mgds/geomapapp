package haxby.worldwind.layers;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.BasicTiledImageLayer;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileUrlBuilder;
import haxby.layers.tile512.LayerSetDetails;
import haxby.util.LegendSupplier;
import haxby.util.URLFactory;
import haxby.util.WESNSupplier;

import java.net.MalformedURLException;
import java.net.URL;

public class Tile512Layer extends BasicTiledImageLayer
									implements InfoSupplier, LegendSupplier, WESNSupplier {
	private String infoURL;
	private String legendURL;
	private double[] wesn;
	
	public Tile512Layer(LayerSetDetails lsd) {
		super(makeLevels(lsd));
		this.setOpacity(lsd.opacity);
	}
	
	public void setInfoURL(String infoURL) {
		this.infoURL = infoURL;
	}
	
	public String getInfoURL() {
		return infoURL;
	}
	
	public void setLegendURL(String legendURL) {
		this.legendURL = legendURL;
	}
	
	public void setWESN(double[] wesn) {
		this.wesn = wesn;
	}
	
	public String getLegendURL() {
		return legendURL;
	}
	
	public double[] getWESN() {
		return wesn;
	}
	
	public static LevelSet makeLevels(final LayerSetDetails lsd) {
		AVList params = new AVListImpl();

		params.setValue(AVKey.TILE_WIDTH, lsd.tileSize);
		params.setValue(AVKey.TILE_HEIGHT, lsd.tileSize);
		params.setValue(AVKey.DATA_CACHE_NAME, "GeoMapApp/" + lsd.name);
		params.setValue(AVKey.SERVICE, "null");
		params.setValue(AVKey.DATASET_NAME, lsd.name);
		params.setValue(AVKey.FORMAT_SUFFIX, "." + lsd.imageExtension);
		params.setValue(AVKey.NUM_LEVELS, lsd.numLevels);
		params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
		params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, 
				new LatLon(Angle.fromDegrees(lsd.levelZeroTileDelta),
							Angle.fromDegrees(lsd.levelZeroTileDelta)));
		params.setValue(AVKey.SECTOR, Sector.fromDegrees(
				lsd.wesn[2], lsd.wesn[3], lsd.wesn[0], lsd.wesn[1]));
		
		params.setValue(AVKey.TILE_URL_BUILDER, new TileUrlBuilder() {
			public URL getURL(Tile tile, String imageFormat) throws MalformedURLException {
				StringBuffer sb = new StringBuffer(lsd.imagePath);
				sb.append(tile.getLevelNumber());
				sb.append("/");
				sb.append(tile.getRow());
				sb.append("/");
				sb.append(tile.getRow());
				sb.append("_");
				sb.append(tile.getColumn());
				sb.append(".");
				sb.append(lsd.imageExtension);
				URL url = URLFactory.url(sb.toString());
				return url;
			}
		});

		return new LevelSet(params);
	}
}

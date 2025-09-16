package haxby.worldwind.layers;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.TiledImageLayer;
//import gov.nasa.worldwind.layers.Earth.*;
import gov.nasa.worldwind.layers.Earth.BMNGWMSLayer;
import gov.nasa.worldwind.layers.Earth.LandsatI3WMSLayer;
import gov.nasa.worldwind.layers.Earth.OSMMapnikLayer;
import gov.nasa.worldwind.layers.Earth.MSVirtualEarthLayer;
 // was gov.nasa.worldwind.layers.Mercator.examples.OSMMapnikLayer

import haxby.layers.tile512.LayerSetDetails;
import haxby.worldwind.image.ImageResampler;
import haxby.worldwind.layers.GridTileLayer.GridRetriever;

import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import org.geomapapp.grid.Grid2DOverlay;
import org.geomapapp.grid.GridComposer;
import org.geomapapp.grid.GridDialog;
import org.geomapapp.grid.SSGridComposer;
import org.geomapapp.util.XML_Menu;

public class LayerComposer {

	public static final int TOPO_MAP_LAYER = 0;
	public static final int BLUEMARBLE_MAP_LAYER = 1;
	public static final int OCEANAGES_MAP_LAYER = 2;
	public static final int SEDIMENT_THICKNESS_MAP_LAYER = 3;
	public static final int SPREADING_RATE_MAP_LAYER = 4;
	public static final int SPREADING_ASYMMETRY_MAP_LAYER = 5;
	public static final int ICUBELANDSAT_MAP_LAYER = 6;
	public static final int MAGNETIC_ANOMALIES_MAP_LAYER = 7;
	public static final int GEOMAPAPP_MASK_LAYER = 8;
	public static final int MS_VIRTUALEARTH_LAYER = 9;
	public static final int MS_VIRTUALEARTH_ROADS_LAYER = 10;
	public static final int MS_VIRTUALEARTH_HYBRID_LAYER = 11;
	public static final int OPENSTREET_MAP_LAYER = 12;

	public static final int GEOID_GRID_LAYER = 100;
	public static final int GRAVITY_GRID_LAYER = 101;
	public static final int TOPO_GRID_LAYER = 102;
	public static final int TOPO_9_GRID_LAYER = 103;
	public static final int AGE_GRID_LAYER = 104;
	public static final int SPREADING_RATE_GRID_LAYER = 105;
	public static final int SPREADING_ASYMMETRY_GRID_LAYER = 106;

	private static final Map<Integer, Layer> layers = new HashMap<Integer, Layer>();
	private static final Map<XML_Menu, Tile512Layer> dynamicLayers 
		= new HashMap<XML_Menu, Tile512Layer>();

	public static Layer getLayer(int layer) {
		return layers.get(layer);
	}

	public static Layer getGeoidGridLayer() {
		GridRetriever geoLayer = new GridRetriever() {
			public Grid2DOverlay retriveGrid(Rectangle2D tileBounds, int level) {
				return SSGridComposer.getGridWW(tileBounds, level, SSGridComposer.GEOID);
			}
			public float getVEFactor() {
				return 5000f;
			}
			public int getNumLevels() {
				return 2;
			}
			public String getName() {
				return org.geomapapp.grid.GridDialog.GEOID;
			}
		};
		GridTileLayer tl = new GridTileLayer(geoLayer, ImageResampler.MERCATOR_TO_GEOGRAPHIC);

		tl.setAnnotationFactor(1);
		tl.setAnnotationUnits(GridDialog.GRID_UNITS.get(GridDialog.GEOID));
		return tl; 
		
	}

	public static Layer getAgeGridLayer() {
		GridTileLayer tl = new GridTileLayer(new GridRetriever() {
			public Grid2DOverlay retriveGrid(Rectangle2D tileBounds, int level) {
				return SSGridComposer.getGridWW(tileBounds, level, SSGridComposer.AGE);
			}
			public float getVEFactor() {
				return 1f;
			}
			public int getNumLevels() {
				return 2;
			}
			public String getName() {
				return org.geomapapp.grid.GridDialog.AGE;
			}
		}, ImageResampler.MERCATOR_TO_GEOGRAPHIC);

		tl.setAnnotationFactor(1/100f);
		tl.setAnnotationUnits(GridDialog.GRID_UNITS.get(GridDialog.AGE));
		return tl;
	}

	public static Layer getSpreadingAsymmetryGridLayer() {
		System.out.println("y");
		GridTileLayer tl = new GridTileLayer(new GridRetriever() {
			public Grid2DOverlay retriveGrid(Rectangle2D tileBounds, int level) {
				System.out.println("y2");
				return SSGridComposer.getGridWW(tileBounds, level, SSGridComposer.SPREADING_ASYMMETRY);
			}
			public float getVEFactor() {
				return 1f;
			}
			public int getNumLevels() {
				return 2;
			}
			public String getName() {
				return org.geomapapp.grid.GridDialog.SPREADING_ASYMMETRY;
			}
		}, ImageResampler.MERCATOR_TO_GEOGRAPHIC);

		tl.setAnnotationFactor(1/100f);
		tl.setAnnotationUnits(GridDialog.GRID_UNITS.get(GridDialog.SPREADING_ASYMMETRY));
		return tl; 
	}

	public static Layer getSpreadingRateGridLayer() {
		GridTileLayer tl = new GridTileLayer(new GridRetriever() {
			public Grid2DOverlay retriveGrid(Rectangle2D tileBounds, int level) {
				return SSGridComposer.getGridWW(tileBounds, level, SSGridComposer.SPREADING_RATE);
			}
			public float getVEFactor() {
				return 1f;
			}
			public int getNumLevels() {
				return 2;
			}
			public String getName() {
				return org.geomapapp.grid.GridDialog.SPREADING_RATE;
			}
		}, ImageResampler.MERCATOR_TO_GEOGRAPHIC);

		tl.setAnnotationFactor(1/100f);
		tl.setAnnotationUnits(GridDialog.GRID_UNITS.get(GridDialog.SPREADING_RATE));
		return tl;
	}

	public static Layer getTopo9GridLayer() {
		GridTileLayer tl = new GridTileLayer(new GridRetriever() {
			public Grid2DOverlay retriveGrid(Rectangle2D tileBounds, int level) {
				return SSGridComposer.getGridWW(tileBounds, level, SSGridComposer.TOPO_9);
			}
			public float getVEFactor() {
				return 1;
			}
			public int getNumLevels() {
				return 3;
			}
			public String getName() {
				return org.geomapapp.grid.GridDialog.TOPO_9;
			}
		}, ImageResampler.MERCATOR_TO_GEOGRAPHIC);
		
		tl.setAnnotationFactor(1);
		tl.setAnnotationUnits(GridDialog.GRID_UNITS.get(GridDialog.TOPO_9));
		return tl; 
	}

	public static Layer getGravityGridLayer() {
		GridTileLayer tl = new GridTileLayer(new GridRetriever() {
			public Grid2DOverlay retriveGrid(Rectangle2D tileBounds, int level) {
				return SSGridComposer.getGridWW(tileBounds, level, SSGridComposer.GRAVITY);
			}
			public float getVEFactor() {
				return 100f;
			}
			public int getNumLevels() {
				return 3;
			}
			public String getName() {
				return org.geomapapp.grid.GridDialog.GRAVITY;
			}
		}, ImageResampler.MERCATOR_TO_GEOGRAPHIC);

		tl.setAnnotationFactor(1);
		tl.setAnnotationUnits(GridDialog.GRID_UNITS.get(GridDialog.GRAVITY));
		return tl;
	}

	public static Layer getGravity_18GridLayer() {
		GridTileLayer tl = new GridTileLayer(new GridRetriever() {
			public Grid2DOverlay retriveGrid(Rectangle2D tileBounds, int level) {
				return SSGridComposer.getGridWW(tileBounds, level, SSGridComposer.GRAVITY_18);
			}
			public float getVEFactor() {
				return 100f;
			}
			public int getNumLevels() {
				return 3;
			}
			public String getName() {
				return org.geomapapp.grid.GridDialog.GRAVITY_18;
			}
		}, ImageResampler.MERCATOR_TO_GEOGRAPHIC);
		
		tl.setAnnotationFactor(1);
		tl.setAnnotationUnits(GridDialog.GRID_UNITS.get(GridDialog.GRAVITY_18));
		return tl;
	}
	
	public static Layer getTopoGridLayer() {
		GridTileLayer tl = new GridTileLayer(new GridRetriever() {
			public Grid2DOverlay retriveGrid(Rectangle2D tileBounds, int level) {
				return GridComposer.getGridWW(tileBounds, level);
			}
			public float getVEFactor() {
				return 1f;
			}
			public int getNumLevels() {
				return 7;
			}
			public String getName() {
				return org.geomapapp.grid.GridDialog.DEM;
			}
		}, ImageResampler.MERCATOR_TO_GEOGRAPHIC);
		
		tl.setAnnotationFactor(1);
		tl.setAnnotationUnits(GridDialog.GRID_UNITS.get(GridDialog.TOPO_9));
		return tl;
	}

	public static Layer getTopoMapLayer() {
		Layer layer = layers.get(TOPO_MAP_LAYER);
		if (layer != null)
			return layer;
		
		layer = new GeoMapAppSurfaceLayer();
		layers.put(TOPO_MAP_LAYER, layer);
		
		return layer;
	}
	
	public static Layer getBlueMarbleMapLayer() {
		TiledImageLayer layer = (TiledImageLayer) layers.get(BLUEMARBLE_MAP_LAYER);
		if (layer != null)
			return layer;
		
		layer = new BMNGWMSLayer(); //was BMNGSurfaceLayer()
		layer.setRetainLevelZeroTiles(false);
		layers.put(BLUEMARBLE_MAP_LAYER, layer);
		
		return layer;
	}

	public static Layer getICubeLandSatMapLayer() {
		TiledImageLayer layer = (TiledImageLayer) layers.get(ICUBELANDSAT_MAP_LAYER);
		if (layer != null)
			return layer;
		
		layer = new LandsatI3WMSLayer(); // was LandsatI3()
		layer.setRetainLevelZeroTiles(false);
		layers.put(ICUBELANDSAT_MAP_LAYER, layer);
		
		return layer;
	}
	
	public static Layer getOceanAgesOverlayMap() {
		Layer layer = layers.get(OCEANAGES_MAP_LAYER);
		if (layer != null)
			return layer;
		
		layer = new OceanAgesSurfaceLayer();
		layers.put(OCEANAGES_MAP_LAYER, layer);
		
		return layer;
	}

	public static Layer getSedimentThicknessOverlayMap() {
		Layer layer = layers.get(SEDIMENT_THICKNESS_MAP_LAYER);
		if (layer != null)
			return layer;

		layer = new SedimentThicknessSurfaceLayer();
		layers.put(SEDIMENT_THICKNESS_MAP_LAYER, layer);

		return layer;
	}
	
	public static Layer getSpreadingRateOverlayMap() {
		Layer layer = layers.get(SPREADING_RATE_MAP_LAYER);
		if (layer != null)
			return layer;

		layer = new SpreadingRateSurfaceLayer();
		layers.put(SPREADING_RATE_MAP_LAYER, layer);

		return layer;
	}

	public static Layer getSpreadingAsymmetryOverlayMap() {
		Layer layer = layers.get(SPREADING_ASYMMETRY_MAP_LAYER);
		if (layer != null)
			return layer;
		
		layer = new SpreadingAsymmetrySurfaceLayer();
		layers.put(SPREADING_ASYMMETRY_MAP_LAYER, layer);
		
		return layer;
	}
	
	public static Layer getMagneticAnomaliesOverlayMap() {
		Layer layer = layers.get(MAGNETIC_ANOMALIES_MAP_LAYER);
		if (layer != null)
			return layer;
		
		layer = new MagneticAnomaliesLayer();
		layers.put(MAGNETIC_ANOMALIES_MAP_LAYER, layer);
		
		return layer;
	}

	public static Layer getGeoMapAppMaskLayer() {
		Layer layer = layers.get(GEOMAPAPP_MASK_LAYER);
		if (layer != null)
			return layer;
		
		layer = new GeoMapAppMaskLayer();
		layers.put(GEOMAPAPP_MASK_LAYER, layer);
		
		return layer;
	}

	// Loads the MS Virtual Earth Aerial
	public static Layer getVirtualEarthAerialLayer() {
		Layer layer1 = layers.get(MS_VIRTUALEARTH_LAYER);
		if (layer1 == null) {
			 layer1 =  new MSVirtualEarthLayer(MSVirtualEarthLayer.LAYER_AERIAL);
			layers.put(MS_VIRTUALEARTH_LAYER, layer1);
			return layer1;
		} else {
			return layer1;
		}
	}

	// Loads the MS Virtual Earth Roads
	public static Layer getVirtualEarthRoadsLayer() {
		Layer layer2 = layers.get(MS_VIRTUALEARTH_ROADS_LAYER);
		if (layer2 != null) {
			return layer2;
		}

		layer2 = new MSVirtualEarthLayer(MSVirtualEarthLayer.LAYER_ROADS); //was this new VirtualEarthLayer(VirtualEarthLayer.Dataset.ROAD);
		layers.put(MS_VIRTUALEARTH_ROADS_LAYER, layer2);
		return layer2;
	}

	// Loads the MS Virtual Earth Hybrid
	public static Layer getVirtualEarthHybridLayer() {
		Layer layer3 = layers.get(MS_VIRTUALEARTH_HYBRID_LAYER);
		if (layer3 != null) {
			return layer3;
		}

		layer3 = new MSVirtualEarthLayer(MSVirtualEarthLayer.LAYER_HYBRID);
		layers.put(MS_VIRTUALEARTH_HYBRID_LAYER, layer3);
		return layer3;
	}

	// Loads the Open Street Map
	public static Layer getOpenStreetMapLayer() {
		Layer layer4 = layers.get(OPENSTREET_MAP_LAYER);
		if (layer4 != null) {
			return layer4;
		}

		layer4 = new OSMMapnikLayer();
		layers.put(OPENSTREET_MAP_LAYER, layer4);
		return layer4;
	}

	public static Layer get512Layer(XML_Menu tile_menu) {
		Tile512Layer layer = dynamicLayers.get(tile_menu);
		if (layer != null) return layer;
		
		LayerSetDetails lsd = LayerSetDetails.levelsFromXML_Menu(tile_menu);
		if (lsd == null) return null;
		layer = new Tile512Layer( lsd );
		layer.setName(tile_menu.name);
		layer.setInfoURL(tile_menu.infoURLString);
		layer.setLegendURL(tile_menu.legend);
		
		double wesn[] = new double[4];
		String[] s = tile_menu.wesn.split(",");
		for (int i = 0; i < 4; i++)
			try {
				wesn[i] = Float.parseFloat(s[i]);
			} catch (NumberFormatException ex) {
				wesn = null;
				break;
			}

		layer.setWESN(wesn);
		dynamicLayers.put(tile_menu, layer);
		
		return layer;
	}
}

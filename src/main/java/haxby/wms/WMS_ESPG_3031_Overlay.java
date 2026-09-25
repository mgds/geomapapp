package haxby.wms;

import java.awt.geom.Rectangle2D;

import haxby.map.FocusOverlay;
import haxby.map.MapOverlay;
import haxby.map.XMap;
import haxby.util.ConnectionWrapper;
import haxby.util.LegendSupplier;
import haxby.util.ProcessingDialog.StartStopTask;
import haxby.util.WESNSupplier;

public class WMS_ESPG_3031_Overlay extends MapOverlay
		implements FocusOverlay, WESNSupplier, LegendSupplier  {

	protected String baseURL;
	protected String name;
	protected int mapRes;
	private double[] wesn;
	private String legendURL;

	public WMS_ESPG_3031_Overlay(String baseURL, XMap map) { 
		this(baseURL, map, null);
	}

	public WMS_ESPG_3031_Overlay(String baseURL, XMap map, String name) {
		this(baseURL, map, name, 512);
	}

	public WMS_ESPG_3031_Overlay(String baseURL, XMap map, String name, double[] wesn) {
		this(baseURL, map, name, wesn, 512);
	}

	public WMS_ESPG_3031_Overlay(String baseURL, XMap map, String name, int mapRes) {
		super(map);
		this.baseURL = baseURL;
		this.name = name;
		this.mapRes = mapRes;
	}

	public WMS_ESPG_3031_Overlay(String baseURL, XMap map, String name, double[] wesn, int mapRes) {
		super(map);
		this.baseURL = baseURL;
		this.name = name;
		this.mapRes = mapRes;
		this.wesn = wesn;
	}

	public void focus(Rectangle2D rect) {
		WMSMapServer.getSPImage(rect, this, mapRes, baseURL, "EPSG:3031");
	}

	public Runnable createFocusTask(final Rectangle2D rect) {
		final ConnectionWrapper wrapper = new ConnectionWrapper();

		return new StartStopTask() {
			public void run() {
				WMSMapServer.getSPImage(rect, 
						WMS_ESPG_3031_Overlay.this, 
						mapRes,
						baseURL, 
						"EPSG:3031",
						wrapper);
				map.repaint();
			}

			public void stop() {
				synchronized (wrapper) {
					if (wrapper.connection != null)
						wrapper.connection.abort();
				}
			}
		};
	}

	public String toString() {
		return name == null ? super.toString() : name ;
	}

	public double[] getWESN() {
		return this.wesn;
	}
	
	public String getLegendURL() {
		return legendURL;
	}
	
	public void setLegendURL(String legendURL) {
		this.legendURL = legendURL;
	}
}

package haxby.wms;

import java.awt.geom.Rectangle2D;

import javax.swing.SwingUtilities;

import haxby.map.FocusOverlay;
import haxby.map.MapOverlay;
import haxby.map.XMap;
import haxby.util.ConnectionWrapper;
import haxby.util.LegendSupplier;
import haxby.util.ProcessingDialog.StartStopTask;
import haxby.util.WESNSupplier;

public class WMS_ESPG_4326_Overlay extends MapOverlay
		implements FocusOverlay, WESNSupplier, LegendSupplier {
	private String baseURL;
	private String name;
	private int mapRes;
	private double [] wesn;
	private String legendURL;
	
	public WMS_ESPG_4326_Overlay(String baseURL, XMap map) {
		this(baseURL, map, null);
	}

	public WMS_ESPG_4326_Overlay(String baseURL, XMap map, String name) {
		this(baseURL, map, name, 512);
	}

	public WMS_ESPG_4326_Overlay(String baseURL, XMap map, String name, double[] wesn) {
		this(baseURL, map, name, wesn, 512);
	}

	public WMS_ESPG_4326_Overlay(String baseURL, XMap map, String name, int mapRes) {
		super(map);
		this.baseURL = baseURL;
		this.name = name;
		this.mapRes = mapRes;
	}

	public WMS_ESPG_4326_Overlay(String baseURL, XMap map, String name, double[] wesn, int mapRes) {
		super(map);
		this.baseURL = baseURL;
		this.name = name;
		this.mapRes = mapRes;
		this.wesn = wesn;
	}

	public void focus(Rectangle2D rect) {
		WMSMapServer.getMercImage(rect, this, mapRes, baseURL, "EPSG:4326");
	}

	public Runnable createFocusTask(final Rectangle2D rect) {
		final ConnectionWrapper wrapper = new ConnectionWrapper();

		return new StartStopTask() {
			public void run() {
				WMSMapServer.getMercImage(rect, 
						WMS_ESPG_4326_Overlay.this, 
						mapRes,
						baseURL,
						"EPSG:4326",
						wrapper);
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						map.repaint();
					}
				});
			}

			public void stop() {
				synchronized (wrapper)
				{
					if (wrapper.connection != null)
						wrapper.connection.abort();
				}
			}
		};
	}

	public String getBaseURL() {
		return baseURL;
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
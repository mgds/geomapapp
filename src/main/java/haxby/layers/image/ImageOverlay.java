package haxby.layers.image;

import haxby.map.FocusOverlay;
import haxby.map.MapOverlay;
import haxby.map.XMap;
import haxby.util.LegendSupplier;
import haxby.util.WESNSupplier;

import java.awt.geom.Rectangle2D;

import javax.swing.SwingUtilities;

public abstract class ImageOverlay extends MapOverlay
						implements FocusOverlay, WESNSupplier, LegendSupplier {
	
	private String legendURL;
	
	public ImageOverlay(XMap map) {
		super(map);
	}
	
	public void setLegendURL(String legendURL) {
		this.legendURL = legendURL;
	}
	
	public String getLegendURL() {
		return legendURL;
	}
	
	public Runnable createFocusTask(final Rectangle2D rect) {
		return new Runnable() {
			public void run() {
				retrieveImage(rect);
				
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						map.repaint();
					}
				});
			}
		};
	}
	
	public void focus(Rectangle2D rect) {
		retrieveImage(rect);
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				map.repaint();
			}
		});
	}
	
	protected abstract void retrieveImage(Rectangle2D rect) ;
}

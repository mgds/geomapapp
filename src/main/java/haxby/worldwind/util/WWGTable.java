package haxby.worldwind.util;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.WWIcon;
import haxby.worldwind.WWOverlay;
import haxby.worldwind.layers.WWSceneGraph;
import haxby.worldwind.layers.DetailedIconLayer.DetailedIcon;
import haxby.worldwind.layers.WWSceneGraph.SceneItemIcon;
import haxby.worldwind.renderers.DetailedIconRenderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.swing.SwingUtilities;

import org.geomapapp.db.util.GTable;
import org.geomapapp.gis.table.TableDB;

public class WWGTable extends GTable implements WWOverlay,
												SelectListener {

	private static final String ICON_PATH = "org/geomapapp/resources/icons/wdot.png";
	private static final int ICON_SIZE = 12;

	public DetailedIcon[] icons;
	public WWSceneGraph layer;
	protected DetailedIconRenderer iconRenderer = new DetailedIconRenderer();

	public WWGTable( Vector headings, Vector rows, TableDB parent) {
		super(headings, rows, parent);
		loadLayer();
	}
	public WWGTable( Vector headings, Vector rows, StringBuffer comments) {
		super( headings, rows, comments);
		loadLayer();
	}
	public WWGTable(String url) throws IOException {
		super( url);
		loadLayer();
	}
	public WWGTable(String url, String delim) throws IOException {
		super( url, delim);
		loadLayer();
	}
	public WWGTable(File file) throws IOException {
		super( file);
		loadLayer();
	}
	public WWGTable(File file, String delim) throws IOException {
		super( file, delim);
		loadLayer();
	}
	public WWGTable() throws IOException {
		super();
		loadLayer();
	}

	protected void loadLayer() {
		if (layer == null) {
			layer = new WWSceneGraph();
			layer.setName("Ocean Floor Drilling");
		}

		icons = new DetailedIcon[allRows.size()];
		int index = 0;
		for (Object obj : allRows) {
			Vector row = (Vector) obj;

			double lat,lon;
			try {
				lat = (Double) row.get(latCol);
				lon = (Double) row.get(lonCol);

				DetailedIcon icon = new DetailedIcon(ICON_PATH, 
						Position.fromDegrees(lat, lon, 0)); 
				icon.setIconColor( Color.WHITE);
				icon.setSize(new Dimension(ICON_SIZE, ICON_SIZE));
				icon.setHighlightScale(1.5f);
				icon.setVisible(false);

				layer.addItem(
						new SceneItemIcon(icon, iconRenderer));
				icons[index] = icon;
			} catch (ClassCastException ex) {
			}

			index++;
		}

		processVisibility();
	}

	public Layer getLayer() {
		return layer;
	}

	public SelectListener getSelectListener() {
		return this;
	}

	public void setArea(final Rectangle2D bounds) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				WWGTable.super.setArea(bounds);

				processVisibility();
				layer.firePropertyChange(AVKey.LAYER, null, layer);
			}
		});
	}

	public void drawSelection() {
		for (WWIcon icon : icons) {
			if (icon == null) continue;

			icon.setHighlighted(false);
		}

		for (int tableIndex : table.getSelectedRows()) {
			int index = allRows.indexOf( getCurrentRow(tableIndex) );
			if (icons[index] != null)
				icons[index].setHighlighted(true);
		}
		layer.firePropertyChange(AVKey.LAYER, null, layer);
	}

	public synchronized void processVisibility() {
		boolean visible[] = new boolean[allRows.size()];

		for (Integer index : currentRowsIndices) {
			Color color = colorer!=null 
				? colorer.getColor( allRows.get(index).get(0) )
				: Color.lightGray;
			if ( color==null ) continue;
			if (color.getRGB() == 0)
				color = Color.lightGray;

			if (icons[index] != null) {
				icons[index].setIconColor(color);
				icons[index].setHighlightColor(color);
			}

			visible[index] = true;
		}

		int index = 0;
		for (WWIcon icon : icons) {
			if (icon != null)
				icon.setVisible( visible[index] );
			index++;
		}
	}

	public void selected(SelectEvent event) {
		if (event.getEventAction().equals(SelectEvent.LEFT_CLICK)) {
			Object topObject = event.getTopObject();
			int index = 0;
			for (DetailedIcon icon : icons) {
				if (icon == topObject)
					break;
				index++;
			}

			if (index == icons.length)
				return;

			int j = currentRowsIndices.indexOf( index );
			if (j != -1) {
				table.getSelectionModel().setSelectionInterval(j, j);
				table.ensureIndexIsVisible(j);
			}
		}
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		layer.setEnabled(visible);
	}
}

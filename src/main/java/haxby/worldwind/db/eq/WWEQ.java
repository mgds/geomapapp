package haxby.worldwind.db.eq;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.WWIcon;
import haxby.db.Database;
import haxby.db.eq.EQ;
import haxby.db.eq.EarthQuake;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.worldwind.WWLayer;
import haxby.worldwind.WWOverlay;
import haxby.worldwind.db.mgg.WWMGG;
import haxby.worldwind.layers.WWSceneGraph;
import haxby.worldwind.layers.DetailedIconLayer.DetailedIcon;
import haxby.worldwind.layers.WWSceneGraph.SceneItemIcon;
import haxby.worldwind.renderers.DetailedIconRenderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;

public class WWEQ extends EQ implements WWOverlay,
										SelectListener {

	private static final String ICON_PATH = "org/geomapapp/resources/icons/wdot.png";
	private static final int ICON_SIZE = 12;

	public DetailedIcon[] icons;
	public WWLayer layer;
	public WWSceneGraph wwScenceGraph;
	protected DetailedIconRenderer iconRenderer = new DetailedIconRenderer();

	public WWEQ(XMap map) {
		super(map);
	}

	@Override
	public void setEnabled(boolean tf) {
		super.setEnabled(tf);
		if (layer != null) layer.setEnabled(tf);
	}

	public boolean loadDB() {
		if (loaded) return true;

		boolean tf = super.loadDB();

		if (tf)
			loadLayer();
		return tf;
	}

	protected void loadLayer() {
		if (wwScenceGraph == null){
			wwScenceGraph = new WWSceneGraph();
			wwScenceGraph.setName(getDBName());
		}

		if (layer == null) {
			layer = new WWLayer(wwScenceGraph) {
				public void close() {
					((MapApp)map.getApp()).closeDB(WWEQ.this);
				}
				public Database getDB() {
					return WWEQ.this;
				}
			};
		}
	
		if (icons != null)
			return;

		int index = 0;
		icons = new DetailedIcon[earthquakes.size()];
		for (Object obj : earthquakes) {
			EarthQuake eq = (EarthQuake) obj;

			if (eq == null) continue;

			DetailedIcon icon = new DetailedIcon(ICON_PATH, Position.fromDegrees(eq.y, eq.x, 0));

			if( eq.dep >2500 ) icon.setIconColor(Color.red);
			else if(eq.dep>500) icon.setIconColor(Color.yellow);
			else icon.setIconColor(Color.green);

			icon.setSize(new Dimension(ICON_SIZE, ICON_SIZE));
			icon.setHighlightScale(2);
			icon.setVisible(false);

			icons[index] = icon;
			wwScenceGraph.addItem(
					new SceneItemIcon(icon, iconRenderer));

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

	@Override
	protected void select() {
		super.select();

		if (icons == null)
			return;

		for (WWIcon icon : icons) 
			if (icon == null) continue;
			else icon.setVisible(false);

		processVisibility();
		layer.firePropertyChange(AVKey.LAYER, null, layer);
	}

	protected void processVisibility() {
		int count = 0;

		for (Object obj : currentIndexInEQ) {
			int index = (Integer) obj;
			icons[index].setVisible(true);

			count++;
//			if (count > MAX_STATIONS_DRAWN)
//				break;
		}
		kountLabel.setText( current.size() +" events, " + count + " shown");
	}

	public void setArea(Rectangle2D bounds) {
	}

	public void selected(SelectEvent event) {
	}

	public void actionPerformed(ActionEvent evt) {
		if( evt.getSource()==visible ) {
			layer.setEnabled( visible.isSelected() );
			return;
		}
		else {
			select();
		}
	}
}

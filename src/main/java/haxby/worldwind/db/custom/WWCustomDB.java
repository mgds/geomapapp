package haxby.worldwind.db.custom;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import haxby.db.Database;
import haxby.db.custom.CustomDB;
import haxby.db.custom.DBDescription;
import haxby.db.custom.UnknownDataSet;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.worldwind.WWLayer;
import haxby.worldwind.WWOverlay;
import haxby.worldwind.awt.LassoSelectionHandler;
import haxby.worldwind.awt.LassoSelectionHandler.LassoSelectListener;
import haxby.worldwind.layers.LayerSet;
import haxby.worldwind.layers.WWSceneGraph;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.geomapapp.util.XML_Menu;

public class WWCustomDB extends CustomDB implements WWOverlay,
													SelectListener,
													LassoSelectListener {

	protected LayerSet layerSet;
	protected RenderableLayer renderableLayer;
	protected WWLayer layer;
	protected WWSceneGraph wwSceneGraph;

	protected JButton extrudeB;
	protected JCheckBox drawAllIcons;

	protected LassoSelectionHandler lassoSelectionHandler;

	public WWCustomDB(XMap map) {
		super(map);
	}

	@Override
	public void setEnabled(boolean tf) {
		super.setEnabled(tf);
		if (layer != null) layer.setEnabled(tf);
		updateLassoState();
	}

	@Override
	public void disposeDB() {
		super.disposeDB();
		updateLassoState();
	}

	@Override
	public boolean loadDB() {
		return super.loadDB();
	};

	public Layer getLayer() {
		if (layer == null) {
			wwSceneGraph = new WWSceneGraph();
			renderableLayer = new RenderableLayer();

			if (dataSets!=null&&dataSets.size()>0)
				for (Iterator iter = dataSets.iterator(); iter.hasNext();) {
					WWUnknownDataSet d = (WWUnknownDataSet) iter.next();
					d.setLayerSceneGraph(wwSceneGraph);
					d.setRenderableLayer(renderableLayer);
					d.updateLayer();
				}

			layerSet = new LayerSet();
			layerSet.setName("Import Data");
			layerSet.add(wwSceneGraph);
			layerSet.add(renderableLayer);

			layer = new WWLayer(layerSet){
				public Database getDB() {
					return WWCustomDB.this;
				}

				public void close() {
					WWCustomDB.this.close();
					if (dataSets.size() != 0)
						return;

					((MapApp) map.getApp()).closeDB(WWCustomDB.this);
				}
			};
		}
		return layer;
	}

	public SelectListener getSelectListener() {
		return this;
	}

	public void selected(SelectEvent event) {
		if (dataSets == null) return;
		for (Object dataset : dataSets) {
			WWUnknownDataSet ud = ((WWUnknownDataSet) dataset);
			if (ud.enabled && ud.plot)
				ud.selected(event);
		}
	}

	public void setArea(Rectangle2D bounds) {
		if (dataSets == null) return;
		for (Object dataset : dataSets) {
			((WWUnknownDataSet) dataset).setArea(bounds);
		}
	}

	public void drawCurrentPoint() {
	}

	@Override
	protected UnknownDataSet createDataSet(DBDescription desc, String text, String delimeter, boolean skipPrompts) {
		return createDataSet(desc, text, delimeter, skipPrompts, null);
	}

	@Override
	protected UnknownDataSet createDataSet(DBDescription desc, String text, String delimeter, boolean skipPrompts, XML_Menu xml_menu) {
		WWUnknownDataSet d = new WWUnknownDataSet(desc, text, delimeter, this, skipPrompts, xml_menu);
		d.setLayerSceneGraph(wwSceneGraph);
		d.setRenderableLayer(renderableLayer);
		return d;
	}

	public void initConfig() {

//			GMA 1.4.8: Adjust grid layout of panel to incorporate new save combo box
//			JPanel p = new JPanel(new GridLayout(10,1));
			JPanel p = new JPanel(new GridLayout(0,1));

			// 1.6.8 Added lasso functionality
			p.add(createLassoPanel());
			p.add(createSelectBox());
			p.add(createSaveBox());
			p.add(createLoadButton());
			p.add(createDisposeButton());
			p.add(createBookmarkButton());
			p.add(createConfigButton());
			p.add(createGraphButton());
			p.add(createColorButton());
			p.add(createScaleButton());
			// JOC GMA 1.6.6 Add a extrude by button
			p.add(createExtrudeButton());
			p.add(createPlotAllCheckbox());
			p.add(createPlotCheckbox());
			p.add(createThumbsCheckbox());

			// JOC: ConfigPanel now uses a Grid Layout.  This lets the all the 
			// 	sub components scale to the resizing of parent
			createConfigPanel(p);
	}

	@Override
	protected JPanel createLassoPanel() {
		JPanel p = super.createLassoPanel();
		lassoTB.addActionListener(this);
		return p; 
	}

	private Component createPlotAllCheckbox() {
		drawAllIcons = new JCheckBox("Draw All Center Icons",false);
		drawAllIcons.addActionListener(this);
		drawAllIcons.setActionCommand("drawAll");
		drawAllIcons.setEnabled(false);
		return drawAllIcons;
	}

	protected Component createExtrudeButton() {
		extrudeB = new JButton("Extrude by Value");
		extrudeB.addActionListener(this);
		extrudeB.setActionCommand("extrude");
		extrudeB.setEnabled(false);
		return extrudeB;
	}

	@Override
	public void updateButtonsState() {
		super.updateButtonsState();
		WWUnknownDataSet d = ((WWUnknownDataSet)box.getSelectedItem());
		if (d == null || wwSceneGraph == null) {
			extrudeB.setEnabled(false);
			drawAllIcons.setSelected(false);
			drawAllIcons.setEnabled(false);
		}
		else {
			extrudeB.setEnabled(d.station);
			drawAllIcons.setEnabled(d.station);
			drawAllIcons.setSelected(wwSceneGraph.isAFlatScene(d.layerKey));
		}
	}

	public void togglePlot() {
		WWUnknownDataSet d = (WWUnknownDataSet) box.getSelectedItem();
		d.plot = plotB.isSelected();
		d.processVisibility();

		layer.firePropertyChange(AVKey.LAYER, null, layer);
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		if (evt.getActionCommand().equals("extrude")) extrude();
		else if (evt.getActionCommand().equals("drawAll")) toggleDrawAll();
		else if (evt.getSource().equals(lassoTB)) updateLassoState();
		else
			super.actionPerformed(evt);

	}

	protected void updateLassoState()
	{
		if (lassoSelectionHandler != null)
			if (!isEnabled())
				lassoSelectionHandler.setLassoEnabled(false);
			else
				lassoSelectionHandler.setLassoEnabled(lassoTB.isSelected());
	}

	protected void toggleDrawAll() {
		int subLayer = ((WWUnknownDataSet) box.getSelectedItem()).layerKey;
		
		if (drawAllIcons.isSelected())
			wwSceneGraph.makeFlatLayer(subLayer);
		else 
			wwSceneGraph.makeDeepLayer(subLayer);
		
		this.layer.firePropertyChange(AVKey.LAYER, null, layer);
	}

	protected void extrude() {
		if (dataSets!=null&&dataSets.size()>0)
			((WWUnknownDataSet)dataSets.get(box.getSelectedIndex())).extrude();
	}

	public void selectLasso(List<Position> area) {
		if (isEnabled())
			if (box.getSelectedItem() != null)
				((WWUnknownDataSet) box.getSelectedItem()).selectWWLasso(area);
	}

	public void setLassoSelectionHandler(
			LassoSelectionHandler lassoSelectionHandler) {
		this.lassoSelectionHandler = lassoSelectionHandler;
	}

	public void select() {
		UnknownDataSet unknownDataSet = ((UnknownDataSet)box.getSelectedItem());
		if (unknownDataSet != null)
			getLayer().setName(unknownDataSet.toString());
		super.select();
	}
}

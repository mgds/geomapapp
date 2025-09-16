package haxby.worldwind;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.event.Message;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.DrawContext;
import haxby.db.Database;
import haxby.worldwind.layers.InfoSupplier;

import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Set;
import java.util.Map.Entry;

public abstract class WWLayer implements Layer, InfoSupplier {
	private Layer layer;
	private String infoURL;
	
	public WWLayer(Layer layer) {
		this.layer = layer;
	}
	public abstract void close();
	public abstract Database getDB();

	public String getInfoURL() {
		return infoURL;
	}

	public void setInfoURL(String infoURLString){
		this.infoURL = infoURLString;
	}
	public void addPropertyChangeListener(
			PropertyChangeListener propertychangelistener) {
		layer.addPropertyChangeListener(propertychangelistener);
	}
	public void addPropertyChangeListener(String s,
			PropertyChangeListener propertychangelistener) {
		layer.addPropertyChangeListener(s, propertychangelistener);
	}
	public AVList clearList() {
		return layer.clearList();
	}
	public AVList copy() {
		return layer.copy();
	}
	public void dispose() {
		if (layer != null)
			layer.dispose();
	}
	public void firePropertyChange(PropertyChangeEvent propertychangeevent) {
		layer.firePropertyChange(propertychangeevent);
	}
	public void firePropertyChange(String s, Object obj, Object obj1) {
		layer.firePropertyChange(s, obj, obj1);
	}
	public Set<Entry<String, Object>> getEntries() {
		return layer.getEntries();
	}
	public String getName() {
		return layer.getName();
	}
	public double getOpacity() {
		if (layer == null) return 0;
		return layer.getOpacity();
	}
	public String getRestorableState() {
		return layer.getRestorableState();
	}
	public double getScale() {
		return layer.getScale();
	}
	public String getStringValue(String s) {
		return layer.getStringValue(s);
	}
	public Object getValue(String s) {
		return layer.getValue(s);
	}
	public Collection<Object> getValues() {
		return layer.getValues();
	}
	public boolean hasKey(String s) {
		return layer.hasKey(s);
	}
	public boolean isAtMaxResolution() {
		return layer.isAtMaxResolution();
	}
	public boolean isEnabled() {
		if (layer == null) return false;
		return layer.isEnabled();
	}
	public boolean isMultiResolution() {
		return layer.isMultiResolution();
	}
	public boolean isPickEnabled() {
		if (layer == null) return false;
		return layer.isPickEnabled();
	}
	public void pick(DrawContext drawcontext, Point point) {
		layer.pick(drawcontext, point);
	}
	public void propertyChange(PropertyChangeEvent evt) {
		layer.propertyChange(evt);
	}
	public Object removeKey(String key) {
		return layer.removeKey(key);
	}
	public void removePropertyChangeListener(
			PropertyChangeListener propertychangelistener) {
		if (layer != null)
			layer.removePropertyChangeListener(propertychangelistener);
	}
	public void removePropertyChangeListener(String s,
			PropertyChangeListener propertychangelistener) {
		layer.removePropertyChangeListener(s, propertychangelistener);
	}
	public void render(DrawContext drawcontext) {
		if (layer != null)
			layer.render(drawcontext);
	}
	public void restoreState(String s) {
		layer.restoreState(s);
	}
	public void setEnabled(boolean flag) {
		layer.setEnabled(flag);
	}
	public void setName(String s) {
		layer.setName(s);
	}
	public void setOpacity(double d) {
		layer.setOpacity(d);
	}
	public void setPickEnabled(boolean flag) {
		layer.setPickEnabled(flag);
	}
	public Object setValue(String s, Object obj) {
		return layer.setValue(s, obj);
	}
	public AVList setValues(AVList avlist) {
		return layer.setValues(avlist);
	}
	public void preRender(DrawContext dc) {
		if (layer == null) return;
		layer.preRender(dc);
	}
	public void setExpiryTime(long t) {
		layer.setExpiryTime(t);
	}
	public long getExpiryTime() {
		return layer.getExpiryTime();
	}
	public boolean isNetworkRetrievalEnabled() {
		return layer.isNetworkRetrievalEnabled();
	}
	public void setNetworkRetrievalEnabled(boolean tf) {
		layer.setNetworkRetrievalEnabled(tf);
	}
	
	public boolean isLayerActive(DrawContext arg0) {
		return layer.isLayerActive(arg0);
	}
	public boolean isLayerInView(DrawContext arg0) {
		return layer.isLayerInView(arg0);	
	}
	public double getMaxActiveAltitude() {
		return layer.getMaxActiveAltitude();
	}
	public Double getMaxEffectiveAltitude(Double arg0) {
		return layer.getMaxEffectiveAltitude(arg0);
	}
	public double getMinActiveAltitude() {
		return layer.getMaxActiveAltitude();
	}
	public Double getMinEffectiveAltitude(Double arg0) {
		return layer.getMinEffectiveAltitude(arg0);
	}

	public void setMaxActiveAltitude(double arg0) {
		return;
	}
	public void setMinActiveAltitude(double arg0) {
		return;
	}
	public void onMessage(Message arg0) {
		return;
	}

}

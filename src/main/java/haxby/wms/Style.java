package haxby.wms;

import java.io.Serializable;

public class Style implements Serializable {
	private String name;
	private String title;
	private String description;
	private String[] legendURLs = new String[0];
	private String styleURL;
	private Layer parent;

	public Layer getParent() {
		return parent;
	}
	public void setParent(Layer parent) {
		this.parent = parent;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String toString() {
		return title;
	}
	public void setLegendURL(String[] urls) {
		this.legendURLs = urls;
	}
	public String[] getLegendURLs() {
		return legendURLs;
	}
	public String getStyleURL() {
		return styleURL;
	}
	public void setStyleURL(String styleURL) {
		this.styleURL = styleURL;
	}
}

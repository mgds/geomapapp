package haxby.wms;

import java.io.Serializable;

public class Layer implements Serializable{
	private String title;
	private String name;
	private String description;
	private String[] dataURLs;
	private String[] metadataURLs;
	private Layer[] children = new Layer[0];
	private Style[] styles = new Style[0];
	private String[] srs = new String[0];
	private double[] wesn = null;
	private Layer parent;
	private boolean hasLLBB = false;

	public boolean isLonRangeComplete() {
		double[] wesn = getWesn();
		if (wesn == null) return false;
		return wesn[0] == -180 && wesn[1] == 180;
	}

	public boolean isRequestable() {
		return name != null;
	}

	public boolean supportsSRS(String srsName) {
		for (int i = 0; i < srs.length; i++)
			if (srs[i].equals(srsName)) return true;

		if (parent != null)
			return parent.supportsSRS(srsName);
		else
			return false;
	}

	public double[] getWesn() {
		if (wesn == null)
			if (parent != null)
				return parent.getWesn();
		return wesn;
	}

	public void setWesn(double[] wesn) {
		this.wesn = wesn;
	}

	public boolean getLatLonBoundingBox() {
		return hasLLBB;
	}

	public void setLatLonBoundingBox(boolean llbb) {
		this.hasLLBB = llbb;
	}

	public Layer getParent() {
		return parent;
	}

	public void setParent(Layer parent) {
		this.parent = parent;
	}
	
	public Layer[] getChildren() {
		return children;
	}
	public void setChildren(Layer[] children) {
		this.children = children;
		
		for (int i = 0; i < children.length; i++)
			children[i].setParent(this);
	}
	public Style[] getStyles() {
		return styles;
	}

	public void setStyles(Style[] styles) {
		this.styles = styles;
		
		for (int i = 0; i < styles.length; i++)
			styles[i].setParent(this);
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
	public String[] getSrs() {
		return srs;
	}
	public void setSrs(String[] srs) {
		this.srs = srs;
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
	public String[] getDataURLs() {
		return dataURLs;
	}
	public void setDataURLs(String[] urls) {
		this.dataURLs = urls;
	}
	public String[] getMetadataURLs() {
		return metadataURLs;
	}
	public void setMetadataURLs(String[] metaUrls) {
		this.metadataURLs = metaUrls;
	}
}

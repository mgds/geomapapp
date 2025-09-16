package haxby.wms;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Capabilities {
	private URL requestURL;
	private String serviceTitle;
	private String description,
					version;
	private String[] supportedFormats;
	private Layer layer;
	private boolean get = true;

	public URL getRequestURL() {
		return requestURL;
	}
	public void setRequestURL(URL requestURL) {
		this.requestURL = requestURL;
	}
	public Layer getLayer() {
		return layer;
	}
	public void setLayer(Layer layer) {
		this.layer = layer;
	}
	public String getServiceTitle() {
		return serviceTitle;
	}
	public void setServiceTitle(String serviceTitle) {
		this.serviceTitle = serviceTitle;
	}
	public String[] getSupportedFormats() {
		return supportedFormats;
	}
	public void setSupportedFormats(String[] supportedFormats) {
		this.supportedFormats = supportedFormats;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}

	public String toString() {
		String str = serviceTitle + "\t" + description + "\t";
		for (int i = 0; i < supportedFormats.length; i++) {
			str += supportedFormats[i] +"\t";
		}
		str += layer;
		return str;
	}

	public String getLayerURL(String[] layers, String[] styles, String srs) {
		String url = requestURL.toString().substring(0, requestURL.toString().indexOf("?") + 1);
		String[] imageFormats = getSupportedFormats();

		Map<String, String> params = new HashMap<String, String>();

		params.put("service", "WMS");
		params.put("request", "GetMap");
		if(getVersion().contentEquals("1.3.0")) { // determine 1.3.0 else 1.1.1
			params.put("version", "1.3.0" );
		} else {
			params.put("version", "1.1.1" );
		}
		params.put("format", LayerExplorer.chooseImageFormat(imageFormats));
		params.put("transparent", "TRUE");
		params.put("bgcolor", "0x000000");

		String layer = "";
		String style = "";
		for (String str : layers)
			layer += str + ",";
		for (String str : styles)
			style += str + ",";
		layer = layer.substring(0, layer.length()-1);
		style = style.substring(0, style.length()-1);

		params.put("layers", layer);
		params.put("styles", style);

		for (Entry<String, String> entry : params.entrySet()) {
			url += entry.getKey() + "=" + entry.getValue() + "&";
		}
		return url;
	}

	public Style getStyle(String layerName, String styleName) {
		System.out.println("layerName: " + layerName + "\t" + "styleName: " + styleName);

		Layer node = getLayer(layer, layerName);
		if (node == null)
			return null;

		node.getStyles();

		for (Style s : node.getStyles())
			if (styleName.equals(s.getName()))
				return s;

		return null;
	}

	public Layer getLayer(String layerName) {
		return getLayer(layer, layerName);
	}

	public static Layer getLayer(Layer root, String layerName) {
		if (layerName.equals(root.getName()))
			return root;

		if (root.getChildren() == null)
			return null;

		for (Layer child : root.getChildren()) {
			Layer search = getLayer(child, layerName);
			if (search != null) return search;
		}
		return null;
	}

	public static boolean isRequetable(Style style) {
		return true;
	}

	public static boolean isRequetable(Layer layer) {
		return layer.isRequestable();
	}
	
	public static boolean isRequetable(Style style, String srs) {
		return style.getParent().supportsSRS(srs);
	}

	public static boolean isRequetable(Layer layer, String srs) {
		if (layer.supportsSRS(srs))
			return layer.isRequestable();
		else return false;
	}
	public void setIsGet(boolean get) {
		this.get  = get;
	}
	public boolean isGet() {
		return get;
	}
}

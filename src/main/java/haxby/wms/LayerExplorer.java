package haxby.wms;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class LayerExplorer extends JPanel {
	private Capabilities capabilities;
	private JTree layerTree;
	private JEditorPane descriptionPane;

	public LayerExplorer(Capabilities capabilities) {
		this.capabilities = capabilities;

		setLayout(new BorderLayout());
		layerTree = new JTree(new LayerTreeModel(capabilities.getLayer()));
		layerTree.setRootVisible(true);
		layerTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		layerTree.setCellRenderer(new DefaultTreeCellRenderer() {
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
				JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
						row, hasFocus);
				if (value instanceof Style &&
						Capabilities.isRequetable((Style)value))
					label.setText(boldHTMLText(label.getText()));
				else if (value instanceof Layer &&
						Capabilities.isRequetable((Layer)value))
					label.setText(boldHTMLText(label.getText()));
				return label;
			}

			private String boldHTMLText(String text) {
				return "<html><b>"+text+"</b></html>";
			}

		});
		layerTree.setEditable(false);
		layerTree.addTreeSelectionListener( new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				TreePath path = e.getNewLeadSelectionPath();
				if (path == null)
					descriptionPane.setText("");
				else {
					Object obj = path.getLastPathComponent();
					String str = null;
					while (obj != null && str == null) {
						if (obj instanceof Style) {
							str = (((Style) obj).getDescription());
							obj = ((Style)obj).getParent();
						}
						else if (obj instanceof Layer) {
							str = (((Layer) obj).getDescription());
							obj = ((Layer)obj).getParent();
						}
					}
					if (str == null)
						descriptionPane.setText("");
					else
						descriptionPane.setText("<html>"+str+"</html>");

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							Rectangle vRect = descriptionPane.getVisibleRect();
							descriptionPane.scrollRectToVisible(
								new Rectangle(0,0,vRect.width, vRect.width));
						}
					});
				}
			}
		});

		add(new JScrollPane(layerTree));

		descriptionPane = new JEditorPane();
		descriptionPane.setEditable(false);
		descriptionPane.setContentType("text/html");
//		descriptionPane.setWrapStyleWord(true);
//		descriptionPane.setLineWrap(true);

//		descriptionPane.setSize(300, layerTree.getHeight());
//		descriptionPane.setPreferredSize(new Dimension(300,layerTree.getHeight()));
		JScrollPane jsp = new JScrollPane(descriptionPane);
		jsp.setPreferredSize(new Dimension(300,layerTree.getHeight()));
		add(jsp, BorderLayout.EAST);

		setMaximumSize(new Dimension(600,400));
	}

	public void addTreeSelectionListener(TreeSelectionListener tsl) {
		layerTree.addTreeSelectionListener(tsl);
	}

	public URL getCapabilitiesURL() {
		return capabilities.getRequestURL();
	}

	public String getSelectedLayerURL() {
		TreePath path = layerTree.getSelectionPath();

		if (path == null) return null;

		URL requestURL = capabilities.getRequestURL();
		String requestPath = requestURL.toString();

		String baseUrl = requestPath;
//		String paramaterString = requestPath.substring(requestPath.indexOf('?')+1).toUpperCase();
//		String[] splitParamaters = paramaterString.split("&");
//		Map<String, String> params = new HashMap<String, String>();
//		for (String param : splitParamaters) {
//			String[] key_value = param.split("=");
//			params.put(key_value[0], key_value[1]);
//		}
		// Get our image formats
		String[] imageFormats = capabilities.getSupportedFormats();

		Map<String, String> params = new HashMap<String, String>();

		params.put("service", "WMS");
		params.put("request", "GetMap");
		if(capabilities.getVersion().contentEquals("1.3.0")) { // determine 1.3.0 else 1.1.1
			params.put("version", "1.3.0" );
		} else {
			params.put("version", "1.1.1" );
		}
		params.put("format", chooseImageFormat(imageFormats));
		params.put("transparent", "TRUE");
		params.put("bgcolor", "0x000000");

		Object obj = path.getLastPathComponent();
		if (obj instanceof Style) {
			Style style = (Style) obj;
			Layer layer = style.getParent();

			params.put("layers", layer.getName());
			params.put("styles", style.getName());
		} else if (obj instanceof Layer) {
			Layer layer = (Layer) obj;

			if (!layer.isRequestable())
				return null;

			params.put("layers", layer.getName());

			if (layer.getStyles().length == 1)
				params.put("styles", layer.getStyles()[0].getName());
			else
				params.put("styles", "");
		} else
			return null;

		requestPath = baseUrl;

		for (Entry<String,String> entry : params.entrySet())
			requestPath += entry.getKey() + "=" + entry.getValue() + "&";

		return requestPath;
	}

	private static final String[] formatOrderPreference = new String[] {
		"image/png", "image/gif", "image/jpeg"
	};

	 public static String chooseImageFormat(String[] formats) {
		 if (formats == null || formats.length == 0)
			return null;
		for (String s : formatOrderPreference) {
			for (String f : formats) {
				if (f.equalsIgnoreCase(s)) {
					Iterator it = ImageIO.getImageReadersByMIMEType(f); 
					if (it.hasNext())
							return f;
				}
			}
		}

		System.err.println("No image recoginized, using " + formats[0]); 

		return formats[0]; // none recognized; just use the first in the caps list
	}

	public boolean isSelectedLayerFull() {
		TreePath path = layerTree.getSelectionPath();
		if (path == null) return false;

		Object obj = path.getLastPathComponent();
		Layer layer = null;
		if (obj instanceof Style)
			layer = ((Style) obj).getParent();
		else if (obj instanceof Layer)
			layer = (Layer) obj;
		else 
			return false;

		return layer.isLonRangeComplete();
	}

	public String getSelectedLayerTitle() {
		TreePath path = layerTree.getSelectionPath();
		if (path == null) return null;

		Object obj = path.getLastPathComponent();
		if (obj instanceof Style) {
			Style s = (Style) obj;
			return s.getParent().getTitle() + ", " + s.getTitle();
		}
		else if (obj instanceof Layer)
			return ((Layer) obj).getTitle();
		else
			return null;
	}

	public Layer getSelectedLayer() {
		TreePath path = layerTree.getSelectionPath();
		if (path == null) return null;

		Object obj = path.getLastPathComponent();
		if (obj instanceof Style)
			return ((Style) obj).getParent();
		else if (obj instanceof Layer)
			return (Layer) obj;
		else 
			return null;
	}

	public Style getSelectedStyle() {
		TreePath path = layerTree.getSelectionPath();
		if (path == null) return null;

		Object obj = path.getLastPathComponent();
		if (obj instanceof Style)
			return (Style) obj;
		else if (obj instanceof Layer)
			return null;
		else 
			return null;
	}

	public static void main(String[] args) throws Exception {
//		Capabilities cap = CapabilitiesParser.parseCapabilities("http://www.marine-geo.org/services/wms?service=WMS&version=1.1.1&request=GetCapabilities");
//		Capabilities cap = CapabilitiesParser.parseCapabilities("http://wms.jpl.nasa.gov/wms.cgi?request=GetCapabilities");
		Capabilities cap = CapabilitiesParser.parseCapabilities("https://neo.gsfc.nasa.gov/wms/wms?version=1.1.1&service=WMS&request=GetCapabilities");

		JFrame frame = new JFrame();
		LayerExplorer le = new LayerExplorer(cap);
		frame.getContentPane().add(le);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}

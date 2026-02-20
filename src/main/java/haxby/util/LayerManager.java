package haxby.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.FocusManager;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import org.geomapapp.db.util.GTable;
import org.geomapapp.gis.shape.ESRIShapefile;
import org.geomapapp.grid.Grid2DOverlay;
import org.geomapapp.grid.GridDialog;
import org.geomapapp.util.GMAProfile;
import org.geomapapp.util.Icons;
import org.geomapapp.util.XML_Menu;

import haxby.db.Database;
import haxby.db.custom.CustomDB;
import haxby.db.custom.UnknownDataSet;
import haxby.map.FocusOverlay;
import haxby.map.MapApp;
import haxby.map.MapTools;
import haxby.map.Overlay;
import haxby.map.XMap;

public class LayerManager extends JPanel implements PropertyChangeListener {
	public static final JButton captureB = new JButton( Icons.getIcon(Icons.CAPTURE,false));
	public static final JButton importB = new JButton();
	public static int preferredWidth = 200;
	public static int preferredHeight = 50;
	public static String infoURL = "https://www.gmrt.org/";
	public boolean baseMapVisible = true;

	protected List<LayerPanel> layerPanels = new LinkedList<LayerPanel>();
	protected List<Overlay> overlays = new LinkedList<Overlay>();
	public ArrayList<String> problemLayers = new ArrayList<String>();
	public ArrayList<String> defaultLayer = new ArrayList<String>();
	public ArrayList<LayerPanel> saveableLayers = new ArrayList<LayerPanel>();

	String[] words;

	private static XMap map;
	private JFrame lmFrame; 
	public static boolean doImport = false;
	private ArrayList<Integer> missingLayers = new ArrayList<Integer>();
	
	public LayerManager() {
		this.setLayout( new BoxLayout(this, BoxLayout.Y_AXIS));	
		this.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));

		// Capture button
		captureB.setPressedIcon( Icons.getIcon(Icons.CAPTURE, true) );
		captureB.setDisabledIcon( Icons.getDisabledIcon( Icons.CAPTURE, false ));
		captureB.setBorder( BorderFactory.createEmptyBorder(1,1,1,1));
		captureB.setMargin(new Insets(1,1,1,1));
		captureB.setToolTipText("Capture Layers Session");
		/* Capture button action prompts user to save the elements in the
		 * layer panel in its order and saves it to a .xml file. A user can
		 * then load a sessions layers file.
		 */
		captureB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int j = 0, m = 0 ;
				int numLayerPanels = layerPanels.size();
				boolean resetSave = false;
				String allProblemLayers = null;
				defaultLayer.clear();
				problemLayers.clear();
				saveableLayers.clear();

				// Test if all open layers has a xml menu item to save
				for ( int i=0; i < numLayerPanels ; i++ ) {
					XML_Menu itemsLP = layerPanels.get(i).item;
					if(itemsLP == null) itemsLP = XML_Menu.getXML_Menu(layerPanels.get(i).layerName);
					if(itemsLP == null) {					
						String problemLayer = layerPanels.get(i).layerName.toString();
						if(problemLayer.matches(MapApp.baseFocusName)) {
							defaultLayer.add(m, problemLayer);
							m++;
						} else {
							System.out.println("problem layer: " +problemLayer);
							problemLayers.add(j, problemLayer);
							j++;
						}
					} else {
						saveableLayers.add(layerPanels.get(i));
					}
				}

				if(problemLayers.size() >=1){
					// Take all the problem layers and combine into a single string
					for(int k=0; k < problemLayers.size(); k++){
						if(k==0){
							// skip default base image
								allProblemLayers = problemLayers.get(0).toString();
						}
						if(k>=1 && k < problemLayers.size()){
							allProblemLayers +=".." + problemLayers.get(k).toString();
						}
						resetSave = true;
					}
				}
				// Process allProblemLayers string and show on alert window
				if(resetSave == true){
					if(allProblemLayers!= null && allProblemLayers.contains("..")){
						allProblemLayers = allProblemLayers.replace("..", "<br>");
					}

					if(problemLayers.size() + defaultLayer.size() <= numLayerPanels) {
						if(((problemLayers.size()) >=1)) {
							// Message alert about how some layers in session cannot be saved
							Object alertPartSave ="<html>The following imported layer(s) <br>"
													+ "cannot be saved in this session. However, all other<br>"
													+ "loaded layers will be saved.<br><br> "
													+ allProblemLayers + "<br><br><hr><br>"
												+ "Continue saving the session?</html>";
							int partSaveValue = JOptionPane.showConfirmDialog(MapApp.anchor, alertPartSave, "Alert: Some Layers Will Not Save",
										JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
							if(partSaveValue ==JOptionPane.NO_OPTION) {
								problemLayers.clear();
								defaultLayer.clear();
								resetSave = false;
								return;
							}
						}
					}
					problemLayers.clear();
					defaultLayer.clear();
					resetSave = false;
				}

				//Open file chooser to save session as xml file
				JFileChooser xmlLayerPanelExport = new JFileChooser
				(System.getProperty("user.home") + "/Desktop");
				String fileName = "Session_" + System.getProperty("user.name") +
						"_" + FilesUtil.fileTimeEST().replace( ':', '-' );

				xmlLayerPanelExport.setSelectedFile( new File(
									xmlLayerPanelExport.getCurrentDirectory(),fileName + ".xml"));

				// Show the dialog, wait until dialog is closed
				int result = xmlLayerPanelExport.showSaveDialog(lmFrame);

				// Determine which button was clicked to close the dialog
				switch (result) {
				case JFileChooser.APPROVE_OPTION:
				// Approve (Open or Save) was clicked
				File xmlFile = xmlLayerPanelExport.getSelectedFile();

				//Prompt User if exists
				if(xmlFile.exists()){
					int overwriteReturnValue = JOptionPane.showConfirmDialog(MapApp.anchor,
							xmlFile.getName().toString() + " already exists. "
							+ "Do you want to replace it?", "File Already Exists",
							JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

					if(overwriteReturnValue ==JFileChooser.CANCEL_OPTION){
						return;
					}
					if(overwriteReturnValue==JFileChooser.APPROVE_OPTION){
						xmlFile.delete();
						//Get random color
						Random rand = new Random();
						Color c = (new Color(rand.nextInt(256),
						rand.nextInt(256),
						rand.nextInt(256)));
						String rgb = "#" + Integer.toHexString( c.getRGB() & 0xFFFFFF );

						for ( int i=0; i <numLayerPanels ; i++ ) {
							XML_Menu itemsLP = layerPanels.get(i).item;
							if(itemsLP!=null) {
								itemsLP.color = rgb;
							}
							//if XML_Menu isn't already assigned to layerPanel, get it now
							if(itemsLP == null) itemsLP = XML_Menu.getXML_Menu(layerPanels.get(i).layerName);
							//Write layer file name once to xmlFile
							if(i==0) {
								String xmlFileName = xmlFile.getName().toString();
								xmlFileName = xmlFileName.replace(".xml", "").trim();

								Rectangle2D r = map.getClipRect2D();
								Point2D.Double p = new Point2D.Double(
										r.getX()+.5*r.getWidth(),
										r.getY()+.5*r.getHeight() );
								p = (Point2D.Double)map.getProjection().getRefXY(p);

								// Get zoom format to 1 decimal and write it
								double zoom = map.getZoom();
								NumberFormat fmtZoom1 = NumberFormat.getInstance();
								fmtZoom1.setMinimumFractionDigits(1);
								fmtZoom1.format(zoom);

								String xmlFileLayer =("<layer " + '\r' + '\t' + "name=" + '"' + xmlFileName + '"' +
										 '\r' + '\t' + "proj=" + '"' + MapApp.CURRENT_PROJECTION + '"' + "> \r");
								FilesUtil.writeLayerToFile(xmlFileLayer, xmlFile);

								//Write Color and zoom
								String colorLayer =('\t' + "<layer " + '\r' + '\t' +
											"\t" + "name=" + '"' + "Zoom To Saved Session" + '"' + '\r' +'\t' + 
											"\t" + "color=" + '"' + rgb + '"' + '\r'+ '\t' +
											"\t" + "zoom=" + '"' + zoom + '"' + '\r' + '\t' +
											"\t" + "lonX=" + '"' + p.getX() + '"' + '\r' + '\t' +
										 	"\t" + "latY=" + '"' + p.getY() + '"' + '\r' + '\t' +
											"\t" + "separator_bar=" + '"'+ "below" + '"' + '\r' + '\t' +
											"\t" + "proj=" + '"' + MapApp.CURRENT_PROJECTION + '"' + '\r' + '\t' +
											"\t" + "command=" +  '"' + "zoom_to_session_area_cmd" + '"' + ">" + '\r'+ '\t' +  "</layer>");
											FilesUtil.writeLayerToFile(colorLayer, xmlFile);
								}
								//Save the session to xmlFile
								try {
										XML_Menu.saveSessionLayer(itemsLP, xmlFile);
								} catch (IOException e1) {
								// TODO Auto-generated catch block
									e1.printStackTrace();
								}
						} //end for

						String loadAllLayer =("\r" + '\t' + "<layer " + '\r' + '\t' +
								"\t" + "name=" + '"' + "Load All Layers" + '"' + '\r' +'\t' +
								"\t" + "separator_bar=" + '"' + "above" + '"' + '\r'+ '\t' +
								"\t" + "proj=" + '"' + MapApp.CURRENT_PROJECTION + '"' + '\r' + '\t' +
								"\t" + "command=" +  '"' + "load_all_session_layers_cmd" + '"' + ">" + '\r'+ '\t' +  "</layer>");
						FilesUtil.writeLayerToFile(loadAllLayer, xmlFile);

						//Write close layer file name once to xmlFile
						String closingTags = "\r" + "</layer>";
						FilesUtil.writeLayerToFile(closingTags, xmlFile);
					}
				} else if(xmlFile.getName().endsWith("xml")) {
					try {
						//Get random color
						Random rand = new Random();
						Color c = (new Color(rand.nextInt(256), 
						rand.nextInt(256),
						rand.nextInt(256)));
						String rgb = "#" + Integer.toHexString( c.getRGB() & 0xFFFFFF );

						for ( int i=0; i <numLayerPanels ; i++ ) {
							XML_Menu itemsLP = layerPanels.get(i).item;
							if(itemsLP!=null) {
								itemsLP.color = rgb;
							}

							//if XML_Menu isn't already assigned to layerPanel, get it now
							if(itemsLP == null) itemsLP = XML_Menu.getXML_Menu(layerPanels.get(i).layerName);
							
							//Write layer file name once to xmlFile
							if(i==0) {
								String xmlFileName = xmlFile.getName().toString();
								xmlFileName = xmlFileName.replace(".xml", "").trim();

								Rectangle2D r = map.getClipRect2D();
								Point2D.Double p = new Point2D.Double(
										r.getX()+.5*r.getWidth(),
										r.getY()+.5*r.getHeight() );
								p = (Point2D.Double)map.getProjection().getRefXY(p);

								// Get zoom format to 1 decimal and write it
								double zoom = map.getZoom();
								NumberFormat fmtZoom1 = NumberFormat.getInstance();
								fmtZoom1.setMinimumFractionDigits(1);
								fmtZoom1.format(zoom);

								String xmlFileLayer =("<layer " + '\r' + '\t' + "name=" + '"' + xmlFileName + '"' +
												 '\r' + '\t' + "proj=" + '"' + MapApp.CURRENT_PROJECTION + '"' +  "> \r");
								FilesUtil.writeLayerToFile(xmlFileLayer, xmlFile);

								//Write Color
								String colorLayer =('\t' + "<layer " + '\r' + '\t' +
													"\t" + "name=" + '"' + "Zoom To Saved Session" + '"' + '\r' +'\t' + 
													"\t" + "color=" + '"' + rgb + '"' + '\r' + '\t' +
													"\t" + "zoom=" + '"' + zoom + '"' + '\r' + '\t' +
													"\t" + "lonX=" + '"' + p.getX()  + '"' + '\r' + '\t' +
													"\t" + "latY=" + '"' + p.getY() + '"' +  '\r' + '\t' +
													 "\t" + "separator_bar=" + '"'+ "below" + '"' + '\r' + '\t' + 
													 "\t" + "proj=" + '"' + MapApp.CURRENT_PROJECTION + '"' + '\r' + '\t' +
												"\t" + "command=" +  '"' + "zoom_to_session_area_cmd" + '"' + ">" + '\r'+ '\t' +  "</layer>");
								FilesUtil.writeLayerToFile(colorLayer, xmlFile);
							}

							if (layerPanels.get(i).layerName != null && layerPanels.get(i).layerName.matches(MapApp.baseFocusName)) {
								String baseLayer = ("\r\t<layer\r\t" +
										"\tname=" + '"' + MapApp.baseFocusName + '"' +"\r\t" +
										"\tindex=" + '"' + i + '"' + ">" + '\r'+ '\t' +  "</layer>");
								FilesUtil.writeLayerToFile(baseLayer, xmlFile);
							} 
							else if (itemsLP != null) {
								//Save the session to xmlFile
								XML_Menu.saveSessionLayer(itemsLP, xmlFile);
							}
							
							//Write close layer tag to xmlFile once
							if(i==numLayerPanels-1) {
								String loadAllLayer =("\r" + '\t' + "<layer " + '\r' + '\t' +
												"\t" + "name=" + '"' + "Load All Layers" + '"' + '\r' +'\t' +
												"\t" + "separator_bar=" + '"' + "above" + '"' + '\r'+ '\t' +
												"\t" + "proj=" + '"' + MapApp.CURRENT_PROJECTION + '"' + '\r' + '\t' +
												"\t" + "command=" +  '"' + "load_all_session_layers_cmd" + '"' + ">" + '\r'+ '\t' +  "</layer>");
								FilesUtil.writeLayerToFile(loadAllLayer, xmlFile);

								String closingTags = "\r" + "</layer>";
								FilesUtil.writeLayerToFile(closingTags, xmlFile);
							}
						}
					} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
					}
				} else {
					JOptionPane.showMessageDialog(MapApp.anchor, xmlFile.getName().toString() + " must be in .xml format");
				}
				break;
				case JFileChooser.CANCEL_OPTION:
					return;
				case JFileChooser.ERROR_OPTION:
					// The selection process did not complete successfully
						break;
				}
			}
		});// End Capture Action

		// Import button
		importB.setBorder( BorderFactory.createEmptyBorder(7,7,7,7));
		importB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					getLayerSessionChooser();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	public void setMap(XMap inputMap) {
		map = inputMap;
		map.addPropertyChangeListener(this);
	}

	public class LayerPanel extends JPanel {
		public Overlay layer;
		private JCheckBox visible;
		private JCheckBox plotProfile;
		private JButton sessionColor;
		private JSlider slider;
		private XML_Menu item;
		private JButton infoB;
		private JLabel shapeLabel;
		public String layerName;
		private String layerURLString = null;
		private boolean layerVisible = false;
		public boolean doPlotProfile = false;
		private Graphics2D g;
		private int prefered_width;
		
		public LayerPanel(Overlay layer, String inputLayerName, String layerURLString, boolean inputLayerVisible, final XML_Menu item) {
			this.layer = layer;
			this.layerName = inputLayerName;
			this.layerVisible = inputLayerVisible;
			this.layerURLString = layerURLString;
			this.item = item;
			
			if ((layer instanceof Grid2DOverlay || layer instanceof ESRIShapefile) &&  !layerName.equals(MapApp.baseFocusName)) {
				this.doPlotProfile = true;
			}
			// Sets the base which is GMRT Image
			if ( layerName.equals(MapApp.baseFocusName) ) {
				baseMapVisible = this.layerVisible;
			}

			GridBagLayout gb = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints();
			this.setLayout(gb);

			c.fill = GridBagConstraints.BOTH;
			c.weighty = 1;
			c.weightx = 5;
			c.gridwidth = GridBagConstraints.FIRST_LINE_START;
			c.gridy = 0;
			// Set max character display limit to 40 characters
			String displayName = layerName;
			if (displayName.length() >= 40) {
				displayName = displayName.substring(0, 40);
			}

			visible = new JCheckBox(displayName, layerVisible);
			visible.setFont(new Font("Arial", Font.BOLD, 11));

			/* Default of check box visibility of panel is true. If XML item 
			 * is false load and display the layer panel with the check box false
			 * and overlay not shown.
			 */
			if(item == null){
				visible = new JCheckBox(displayName, layerVisible);
			}else if((item != null) && (item.display_layer != null)){
				boolean dValue = Boolean.parseBoolean(item.display_layer);
				visible = new JCheckBox(displayName, dValue);
				if ( !dValue ) {
					setLayerVisible(LayerPanel.this,dValue);
				}
			}else{
				visible = new JCheckBox(displayName, layerVisible);
			}

			visible.setMaximumSize(new Dimension(500,100));
			visible.setBorderPainted(false);
			visible.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					boolean newVisible = ((JCheckBox)e.getSource()).isSelected();
					setLayerVisible(LayerPanel.this,newVisible);
					if (item!=null){
						item.display_layer = String.valueOf(newVisible);
					}
				}
			});
			gb.setConstraints(visible, c);
			add(visible);
			
			if (layer instanceof UnknownDataSet) {
				// get the symbol shape used for the dataset and display it in the layer panel 
				shapeLabel = new JLabel();
				shapeLabel.setBorder(new EmptyBorder(0,0,0,10));
				String shape = ((UnknownDataSet)layer).getSymbolShape();
				Color color = ((UnknownDataSet)layer).getColor();
				setSymbolShape(shape, color);
				add(shapeLabel);
				((UnknownDataSet) layer).setLayerPanel(this);
			}
			
			JButton up = new JButton("\u039B");
			up.setMargin(new Insets(1,1,1,1));
			up.setFont(new Font("Arial", Font.BOLD, 11));
			up.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					up(LayerPanel.this.layer);
				}
			});
			c = new GridBagConstraints();
			c.gridy = 0;
			c.gridwidth = GridBagConstraints.REMAINDER;
			gb.setConstraints(up, c);
			add(up);


			
			Box box = new Box(BoxLayout.X_AXIS);
			box.add(Box.createHorizontalStrut(2));

			JButton remove = createButton(Icons.CLOSE);
			remove.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					LayerManager.this.doRemove(LayerPanel.this);
				}
			});
			gb.setConstraints(remove, c);

			//Get the information URL for the layer to be used with the Info button.
			String gridName = "";
			if (this.layer instanceof Grid2DOverlay) {
				gridName = ((Grid2DOverlay) (this.layer)).getName();
				if (GridDialog.GRID_URL.get(gridName) != null) {
					this.layerURLString = GridDialog.GRID_URL.get(gridName);
				}
			} else if (this.layer instanceof ESRIShapefile) {
				this.layerURLString = ((ESRIShapefile) (this.layer)).getInfoURL();
			}
			
			if ( gridName.equals(MapApp.baseFocusName) ){
				// Give info Button
				this.layerURLString = infoURL;
			}else if ( gridName.equals(GridDialog.DEM)){
				 //Give info Button
				this.layerURLString = infoURL;
				box.add(remove);
			}
			else {
				box.add(remove);
			}

			if ( this.layerURLString != null && this.layerURLString.length() > 0 ) {
				infoB = createButton(Icons.INFO);
				infoB.setToolTipText("Information");
				infoB.addActionListener( new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						BrowseURL.browseURL(LayerPanel.this.layerURLString);
					}
				});
				box.add(infoB);
			}
			//Gets Legend URL and Legend Icon Button
			String legendURLString = null;
			if (layer instanceof LegendSupplier)
				legendURLString = ((LegendSupplier) layer).getLegendURL();
			if ( legendURLString != null && legendURLString.length() > 0 ) {
				JButton legendB = createButton(Icons.LEGEND);
				legendB.setToolTipText("Legend");
				legendB.addActionListener( new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						BrowseURL.browseURL(((LegendSupplier) LayerPanel.this.layer).getLegendURL());
					}
				});
				box.add(legendB);
			}

			//Gets Warning URL and Warning Icon Button
			String warningURLString = null;
			if (layer instanceof WarningSupplier)
				warningURLString = ((WarningSupplier) layer).getWarningURL();
			if ( warningURLString != null && warningURLString.length() > 0 ) {
				JButton warningB = createButton(Icons.WARNING);
				warningB.setToolTipText("Warning Message");
				warningB.addActionListener( new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						BrowseURL.browseURL(((WarningSupplier) LayerPanel.this.layer).getWarningURL());
					}
				});
				box.add(warningB);
			}

			//Gets WESN location and Zoom Icon Button
			double[] wesn = null;
			Rectangle2D bounds = null;
			if (layer instanceof WESNSupplier) {
				wesn = ((WESNSupplier) layer).getWESN();
			}
			else if(layer instanceof RectSupplier) {
				bounds = ((RectSupplier) layer).getRect();
			}
			if(null != wesn || null != bounds) {
				JButton zoomB = createButton(Icons.ZOOM_IN);
				zoomB.setToolTipText("Zoom To");
				if(null == bounds) {
					zoomB.addActionListener( new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							map.setZoomHistoryPast(map);
							map.zoomToWESN(((WESNSupplier) LayerPanel.this.layer).getWESN());
							map.setZoomHistoryNext(map);
						}
					});
				}
				else {
					zoomB.addActionListener( new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							map.setZoomHistoryPast(map);
							map.zoomToRect(((RectSupplier) LayerPanel.this.layer).getRect());
							map.setZoomHistoryNext(map);
						}
					});
				}
				box.add(zoomB);
				if(null == bounds) {
					MapApp.sendLogMessage("Loaded_Content&name="+inputLayerName.trim()+"&WESN="+wesn[0]+","+wesn[1]+","+wesn[2]+","+wesn[3]);
				}
				else {
					MapApp.sendLogMessage("Loaded_Content&name="+inputLayerName.trim()+"&bounds="+bounds);
				}
			}
			else {
				MapApp.sendLogMessage("Loaded_Content&name="+inputLayerName.trim());
			}

			
			c = new GridBagConstraints();
			c.gridy = 1;
			c.insets = new Insets(0,1,0,1);
			c.weighty = 1;
			gb.setConstraints(box, c);
			add(box);

			//Add Opacity Label
			JLabel l = new JLabel(" Opacity:");
			l.setFont(new Font("Arial", Font.PLAIN, 11));
			c = new GridBagConstraints();
			c.gridy = 1;
			c.insets = new Insets(0,0,0,0);
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1;
			c.gridwidth = 1;
			c.weighty = 1;
			c.anchor = GridBagConstraints.CENTER; 
			gb.setConstraints(l, c);
			add(l);

//			c.weightx = 5;
//			c.gridwidth = GridBagConstraints.RELATIVE;

			/*Add Opacity Slider defaults to 100 if there is no value in XML item
			 * Or if the XML item doesn't exist. If the opacity is available
			 * then update the slider and update the map overlay. The tool tip
			 * informs users what opacity number they are at.
			 */
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1;
			c.weighty = 1;
			c.gridy = 1;
			
			slider = new JSlider(0,100);
			slider.setSize(4, 4);//.setPreferredSize(new Dimension(40, slider.getPreferredSize().height - 10));
			slider.setFont(new Font("Arial", Font.PLAIN, 11));
			if(item == null){
				slider.setValue((int) map.getOverlayAlpha(layer) * 100);
				slider.setToolTipText("Opacity Level is " + (int) map.getOverlayAlpha(layer) * 100);
			}else if(item != null && item.opacity_value != null){
				int oValue = Integer.parseInt(item.opacity_value);
				slider.setValue(oValue);
				map.setOverlayAlpha(LayerPanel.this.layer,
								(oValue) / 100f);
				map.repaint();
				slider.setToolTipText("Opacity Level is " + oValue);
			}else{
				slider.setValue((int) (map.getOverlayAlpha(layer) * 100));
				slider.setToolTipText("Opacity Level is " + (int) map.getOverlayAlpha(layer) * 100);
			}

			slider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					int newSliderValue = ((JSlider) e.getSource()).getValue();
					if(item!=null){
						//System.out.println("Opacity Value " + String.valueOf(newSliderValue));
						item.opacity_value = String.valueOf(newSliderValue);
						if(newSliderValue >= 100){
							item.opacity_value = null;
						}
					}
					map.setOverlayAlpha(LayerPanel.this.layer,
							((JSlider) e.getSource()).getValue() / 100f);
					map.repaint();
					slider.setToolTipText("Opacity Level is " + newSliderValue);
				}
			});

			gb.setConstraints(slider, c);
			add(slider);

			//DO WE NEED THIS???  NSS 12/05/16
			//Add color box which is coded with session imports if they exist
//			if(item !=null) {
//				// Look for the top level Menu Bar Name
//				if(item.parent !=null){
//					XML_Menu t = item.parent;
//					String sessionName = t.toString();
//					while (t.parent !=null){
//						t = t.parent;
//					}
//					String menuName = t.name;
//					//System.out.println("Root MenuBar name is " + menuName);
//					if(menuName.contentEquals("My Layer Sessions")){
//						c = new GridBagConstraints();
//						c.fill = GridBagConstraints.NONE;
//						c.anchor = GridBagConstraints.CENTER;
//						c.weightx = 1;
//						c.gridwidth = 1;
//						sessionColor = new JButton("");
//						sessionColor.setToolTipText(sessionName);
//						sessionColor.setEnabled(false);
//						sessionColor.setOpaque(true);
//						sessionColor.setBorder( BorderFactory.createEmptyBorder(4,4,4,4));
//
//						//Sets color to match with MenuItem
//						sessionColor(item);
//						gb.setConstraints(sessionColor, c);
//						add(sessionColor);
//					}
//				}
//			}//End Color of sessions

			
			//Add Level Down Button
			JButton down = new JButton("V");
			down.setMargin(new Insets(1,1,1,1));
			down.setFont(new Font("Arial", Font.BOLD, 11));
			down.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					down(LayerPanel.this.layer);
				}
			});
			c = new GridBagConstraints();
			c.gridy = 1;
			c.weighty = 1;
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.anchor = GridBagConstraints.LINE_END;
			gb.setConstraints(down, c);
			add(down);

			if ((layer instanceof Grid2DOverlay || 
					(layer instanceof ESRIShapefile && ((ESRIShapefile) layer).getMultiGrid() != null)) &&
				!layerName.equals(MapApp.baseFocusName)) { 
				boolean profileSelected = GMAProfile.getProfileStatus();
				plotProfile = new JCheckBox("plot profile");
				plotProfile.setVisible(profileSelected);
				plotProfile.setSelected(doPlotProfile);
				plotProfile.addActionListener( new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						doPlotProfile = ((JCheckBox)e.getSource()).isSelected();
					}
				});
				plotProfile.setHorizontalAlignment(SwingConstants.RIGHT);
				c = new GridBagConstraints();
				c.weightx = 0;
				c.weighty = 0;
				c.gridwidth = GridBagConstraints.REMAINDER;
				c.fill = GridBagConstraints.BOTH;
				c.anchor = GridBagConstraints.LINE_END;
				c.gridy = 2;
				gb.setConstraints(plotProfile, c);
				add(plotProfile);
			}
					
			setBorder( BorderFactory.createLineBorder(Color.black));

			prefered_width = gb.preferredLayoutSize(this).width;

			if ( gb.preferredLayoutSize(this).width > LayerManager.preferredWidth ) {
				LayerManager.preferredWidth = gb.preferredLayoutSize(this).width;
			}

			int minWidth = getMinimumSize().width;
			int minHeight = 83;//getMinimumSize().height;
			int maxWidth = getMaximumSize().width;

			setPreferredSize(new Dimension(minWidth, minHeight));
			setMaximumSize(new Dimension(maxWidth, minHeight));

			setColor();	
		} // End of LayerPanel

		private JButton createButton(int icon) {
			JButton button = new JButton( Icons.getIcon(icon,false));
			button.setPressedIcon( Icons.getIcon(icon, true) );
			button.setDisabledIcon( Icons.getDisabledIcon( icon, false ));
			button.setBorder( BorderFactory.createLineBorder(Color.black));
			button.setMargin(new Insets(1,0,1,0));
			return button;
		}

		public void setItem(XML_Menu item) {
			this.item = item;
		}
		
		public void setName(String inputName) {
			layerName = inputName;
		}

		public void setColor() {
//			Color c = !layerVisible ? Color.LIGHT_GRAY : Color.GRAY;
			Color c = Color.LIGHT_GRAY;
			this.setBackground(c);
			for (Component comp : this.getComponents())
				if (!(comp instanceof JButton))
					comp.setBackground(c);
		}
		
		public void setSymbolShape(String shape, Color color) {
			// display the symbol shape used in a dataset on the layer panel
			String shapeHTML;
			String colorHTML = "#"+Integer.toHexString(color.getRGB()).substring(2);
			switch(shape) {
				case "circle":
					shapeHTML =  "<html><span style='font-family:verdana; font-size:25px; color: "+colorHTML+"'>&#9679;</span>";
					break;
				case "triangle":
					shapeHTML =  "<html><span style='font-family:verdana; font-size:14px; color: "+colorHTML+"'>&#9650;</span>";
					break;
				case "square":
					shapeHTML =  "<html><span style='font-family:verdana; font-size:25px; color: "+colorHTML+"'>&#9632;</span>";
					break;
				case "star":
					shapeHTML =  "<html><span style='font-family:verdana; font-size:13px; color: "+colorHTML+"'>&#9733;</span>";
					break;
				case "solid":
					shapeHTML = "<html><span style='font-family:verdana; font-size:35px; color: "+colorHTML+"'>&#9472;</span>";
					break;
				case "dashed":
					shapeHTML = "<html><span style='font-family:verdana; font-size:20px; color: "+colorHTML+"'>---</span>";
					break;
				case "dotted":
					shapeHTML = "<html><span style='font-family:verdana; font-size:20px; color: "+colorHTML+"'>&middot;&middot;&middot;&middot;</span>";
					break;
				case "dash-dotted":
					shapeHTML = "<html><span style='font-family:verdana; font-size:20px; color: "+colorHTML+"'>-&middot;-&middot;</span>";
					break;			
					
				default:
					shapeHTML =  "";
			}
			if(shapeLabel != null) {
				shapeLabel.setText(shapeHTML);
			}
		}

		public void sessionColor(XML_Menu itemC) {
			String c = itemC.color.toString();
			Color c2 = Color.decode(c);
			sessionColor.setBackground(c2);
		}
		public void stateChanged() {
//			slider.setValue((int)(layer.getOpacity() * 100));
			visible.setSelected(layerVisible);
			setColor();
		}
	}

	private void down(Overlay layer) {
		if ( !overlays.contains(layer) ) {
			return;
		}
		int index = overlays.indexOf(layer);
		if ( index >= (overlays.size()-1) ) {
			return;
		}

		Overlay oneDown = overlays.get(index + 1);
		int oneDownIndex = map.getOverlayIndex(oneDown);

		LayerPanel layerPanel = layerPanels.get(overlays.indexOf(layer));

		overlays.remove(layer);
		layerPanels.remove(index);
		this.remove((Component)layerPanel);

		overlays.add(index+1,layer);
		layerPanels.add(index+1,layerPanel);
		if ( layerPanel.visible.isSelected() && oneDownIndex != -1) {
			float alpha = map.getOverlayAlpha(layer);
			map.removeOverlay(layer, false);
			addLayerBack(layer);
			// set the opacity back
			map.setOverlayAlpha(layer, alpha);
		}

		if ( layer instanceof Database ) {
			for ( int i = 0; i < overlays.size(); i++ ) {
				if ( overlays.get(i) instanceof Database ) {
					if ( !((MapApp)map.getApp()).getCurrentDB().equals(((Database)overlays.get(i))) ) {
						((MapApp)map.getApp()).disableCurrentDB();
						((MapApp)map.getApp()).setCurrentDB(((Database)overlays.get(i)));
						((MapApp)map.getApp()).enableCurrentDB();
						((MapApp)map.getApp()).addDBToDisplay(((Database)overlays.get(i)));
					}
					break;
				}
			}
		}
		
		//if displaying any UnknownDataSets, set the selected Data Table to be the top one
		for (Overlay lyr : overlays) {
			if (lyr instanceof UnknownDataSet) {
				UnknownDataSet ds = (UnknownDataSet) lyr;
				ds.db.setCurrent(ds);
				break;
			}
		}
		
		this.add(layerPanel,index+1);
		this.revalidate();
		this.repaint();
		map.repaint();
	}

	private void up(Overlay layer) {
		if ( !overlays.contains(layer) ) {
			return;
		}

		int index = overlays.indexOf(layer);
		if ( index <= 0 ) {
			return;
		}

		Overlay oneUp = overlays.get(index - 1);

		LayerPanel layerPanel = layerPanels.get(index);
		overlays.remove(layer);
		layerPanels.remove(index);
		this.remove((Component)layerPanel);

		overlays.add(index-1,layer);
		layerPanels.add(index-1,layerPanel);

		int oneUpIndex = map.getOverlayIndex(oneUp);
		if ( layerPanel.visible.isSelected() && oneUpIndex != -1) {
			float alpha = map.getOverlayAlpha(layer);
			map.removeOverlay(layer, false);
			addLayerBack(layer);
			// set the opacity back
			map.setOverlayAlpha(layer, alpha);
		}

		if ( layer instanceof Database ) {
			for ( int i = 0; i < overlays.size(); i++ ) {
				if ( overlays.get(i) instanceof Database ) {
					if ( !((MapApp)map.getApp()).getCurrentDB().equals(overlays.get(i)) ) {
						((MapApp)map.getApp()).disableCurrentDB();
						((MapApp)map.getApp()).setCurrentDB(((Database)overlays.get(i)));
						((MapApp)map.getApp()).enableCurrentDB();
						((MapApp)map.getApp()).addDBToDisplay(((Database)overlays.get(i)));
					}
					break;
				}
			}
		}
		
		//if displaying any UnknownDataSets, set the selected Data Table to be the top one
		for (Overlay lyr : overlays) {
			if (lyr instanceof UnknownDataSet) {
				UnknownDataSet ds = (UnknownDataSet) lyr;
				ds.db.setCurrent(ds);
				break;
			}
		}
		
		this.add(layerPanel,index-1);
		this.revalidate();
		this.repaint();
		map.repaint();
	}

	// Removes a selected panel(layer) from the layer manager
	private void remove(LayerPanel layerPanel) {
		layerPanels.remove(layerPanel);
		overlays.remove(layerPanel.layer);
		this.remove((Component)layerPanel);

		Dimension lmMaxSize = getMaximumSize();

		Dimension size = new Dimension(
				lmMaxSize.width+20,
				lmMaxSize.height+40);
		Dimension maxSize = lmFrame.getMaximumSize();

		size.height = Math.min(size.height, maxSize.height);
		size.width = Math.min(size.width, maxSize.width);

		lmFrame.setMinimumSize(size);
		lmFrame.setSize(size);
		lmFrame.pack();
		this.revalidate();
		this.repaint();
		if ( lmFrame.isVisible() ) {
			Window activeWindow = FocusManager.getCurrentManager().getActiveWindow();
			lmFrame.toFront();
			if (activeWindow != null) activeWindow.requestFocus();
		}
		map.removeOverlay(layerPanel.layer);
	}

	public void doRemove(LayerPanel layerPanel) {
		MapApp app = (MapApp) map.getApp();

		if ( layerPanel.layer instanceof Grid2DOverlay) {
			app.getMapTools().getGridDialog().dispose((Grid2DOverlay)layerPanel.layer);
		}
		
		// may be deprecated now we use UnknownDataSet layers instead of CustomDB layers
		// NSS 06/25/21
		if ( layerPanel.layer instanceof CustomDB ) {
			CustomDB db = (CustomDB) layerPanel.layer;
			db.close();
			if (db.dataSets.size() != 0)
				return;

			app.closeDB(db);
			for ( int i = 0; i < overlays.size(); i++ ) {
				if ( overlays.get(i) instanceof Database ) {
					app.setCurrentDB(((Database)overlays.get(i)));
					app.enableCurrentDB();
					app.addDBToDisplay(((Database)overlays.get(i)));
					break;
				}
			}
		}
		
		if ( layerPanel.layer instanceof UnknownDataSet ) {
			UnknownDataSet ds = (UnknownDataSet) layerPanel.layer;
			CustomDB db = ds.db;
			db.close(ds);
			remove(layerPanel);
			if (db.dataSets.size() != 0)
				return;

			app.closeDB(db);
			for ( int i = 0; i < overlays.size(); i++ ) {
				if ( overlays.get(i) instanceof Database ) {
					app.setCurrentDB(((Database)overlays.get(i)));
					app.enableCurrentDB();
					app.addDBToDisplay(((Database)overlays.get(i)));
					break;
				}
			}
		}
		
		else if ( layerPanel.layer instanceof Database ) {
			app.closeDB( ((Database)layerPanel.layer) );
			for ( int i = 0; i < overlays.size(); i++ ) {
				if ( overlays.get(i) instanceof Database ) {
					app.setCurrentDB(((Database)overlays.get(i)));
					app.enableCurrentDB();
					app.addDBToDisplay(((Database)overlays.get(i)));
					break;
				}
			}
		}
		else if ( layerPanel.layer instanceof haxby.grid.ContributedGridsOverlay ) {
			if ( app.getMapTools().contributedGrids.isSelected() ) {
				app.toggleContributedGrids(true);
			}
		}
		else if ( layerPanel.layer instanceof ESRIShapefile ) {
			if ( app.getMapTools().suite.getShapes().contains(((ESRIShapefile)layerPanel.layer)) ) {
				if ( app.getMapTools().suite.getViewShapes() != null ) {
					app.getMapTools().suite.getViewShapes().closeData(((ESRIShapefile)layerPanel.layer));
					app.getMapTools().suite.getViewShapes().getFrame().repaint();
				}
				else {
					app.getMapTools().suite.removeShapeFile((ESRIShapefile)layerPanel.layer);
				}
			}
		}
		else if (layerPanel.layer instanceof FocusOverlay) {
			((MapApp) map.getApp()).removeFocusOverlay( (FocusOverlay) layerPanel.layer);
		}
		else if (layerPanel.layer instanceof GTable) { // ASSUME DSDP
			((MapApp) map.getApp()).closeDSDP();
		} else {
			remove(layerPanel);
		}
		map.repaint();
	}

	public void setLayerVisible(LayerPanel layerPanel, boolean visible) {
		MapApp app = (MapApp) map.getApp();
		layerPanel.visible.setSelected(visible);
		layerPanel.layerVisible = visible;
		//not needed: map.setOverlayAlpha(layerPanel.layer,layerPanel.slider.getValue() / 100f);

		if ( layerPanel.layerName.equals(MapApp.baseFocusName) ) {
			baseMapVisible = visible;
			app.autoFocus();
			map.repaint();
			return;
		}else if ( layerPanel.layer instanceof Grid2DOverlay) {
			layerPanel.layerVisible = visible;
			if ( visible ) {
				app.getMapTools().getGridDialog().gridCB.setSelectedItem(layerPanel.layer);
				addLayerBack(layerPanel.layer);
				app.getMapTools().getGridDialog().startGridLoad();
				app.getMapTools().getGridDialog().showDialog();
				return;
			}
		}else if ( layerPanel.layer instanceof ESRIShapefile ) {
			ESRIShapefile shape = (ESRIShapefile) layerPanel.layer;
			MapTools tools = app.getMapTools();
			if ( visible ) {
				if ( !shape.isVisible() ) {
					shape.setVisible(true);
					if ( tools.suite != null ) {
						tools.suite.setValueAt(new Boolean(visible),
								tools.suite.getRowForShapefile(shape), 2);
						if ( tools.suite.getViewShapes() != null ) {
							tools.suite.getViewShapes().getFrame().repaint();
						}

						if (app.getMapTools().getGridDialog() != null)
							app.getMapTools().getGridDialog().refreshGrid(shape);
					}
				}
			}
			else {
				if ( shape.isVisible() ) {
					shape.setVisible(false);
					if ( tools.suite != null ) {
						tools.suite.setValueAt(new Boolean(visible), tools.suite.getRowForShapefile(shape), 2);
						if ( tools.suite.getViewShapes() != null ) {
							tools.suite.getViewShapes().getFrame().repaint();
						}
					}
				}
			}
			return;
		} else if (layerPanel.layer instanceof FocusOverlay)
		{
			if (visible) {
				addLayerBack(layerPanel.layer);
				app.addFocusOverlay(
						(FocusOverlay)layerPanel.layer);
				return;
			}else {
				app.removeFocusOverlay(
						(FocusOverlay)layerPanel.layer, false);
			}
		}

		if ( !visible ) {
			map.removeOverlay(layerPanel.layer, false);
			app.autoFocus();
			map.repaint();
		}
		else {
			addLayerBack(layerPanel.layer);
			map.repaint();
		}
	}

	private void addLayerBack(Overlay layer) {
		// Find the next visible element before or after this layerPanel to base
		// our index off of in the XMap
		Overlay neighborLayer = null;
		boolean before = true;
		int index = map.getOverlaysSize();
		for (int i = 0; i < layerPanels.size(); i++) {
			LayerPanel lp = layerPanels.get(i);
			if (lp.layer == layer) {
				if (neighborLayer == null)
				{
					before = false;
					continue;
				}
				else
				{
					// neighborLayer is a visible layer in front of our layer
					// so add our layer behind it in XMap
					index = map.getOverlayIndex(neighborLayer);
					break;
				}
			}
			if (!lp.layerVisible) continue;

			//lp is visible so it is either after our layer
			// so set our index by it and break
			if (!before)
			{
				index = map.getOverlayIndex(lp.layer) + 1;
				break;
			}
			else
				// or before our layer so keep looking for out layer
				neighborLayer = lp.layer;
		}
		map.addOverlay(index, layer, false);
	}

	public void setLayerVisible(Overlay layer, boolean visible) {
		LayerPanel layerPanel = null;
		for ( int i = 0; i < layerPanels.size(); i++ ) {
			if ( layerPanels.get(i).layer.equals(layer) ) {
				layerPanel = layerPanels.get(i);
			}
		}
		if ( layerPanel == null ) {
			return;
		}

		setLayerVisible(layerPanel, visible);
	}

	public boolean getLayerVisible( Overlay layer ) {
		for ( int i = 0; i < overlays.size(); i++ ) {
			if ( layerPanels.get(i).layer.equals(layer) ) {
				return layerPanels.get(i).layerVisible;
			}
		}
		return true;
	}
	
	// As above, but returns false if layer not in overlays.  Used by GridDialog.refreshGrids()
	public boolean getLayerVisibleDefaultFalse( Overlay layer ) {
		for ( int i = 0; i < overlays.size(); i++ ) {
			if ( layerPanels.get(i).layer.equals(layer) ) {
				return layerPanels.get(i).layerVisible;
			}
		}
		return false;
	}

//	Returns overlay with given name if it is in the layer manager, returns null if not
	public Overlay getOverlay( String layerName ) {
		for ( int i = 0; i < overlays.size(); i++ ) {
			if ( layerPanels.get(i).layerName.equals(layerName) ) {
				return overlays.get(i);
			}
		}
		return null;
	}

//	Returns index of overlay if it is in the layer manager, returns -1 if not
	public int getIndex( String layerName ) {
		for ( int i = 0; i < overlays.size(); i++ ) {
			if ( layerPanels.get(i).layerName.equals(layerName) ) {
				return i;
			}
		}
		return -1;
	}

	public int getIndex( Overlay layer ) {
		for ( int i = 0; i < overlays.size(); i++ ) {
			if ( layerPanels.get(i).layer.equals(layer) ) {
				return i;
			}
		}
		return -1;
	}

	public void removeLayerPanel(int index) {
		if ( index > -1 && layerPanels.get(index) != null ) {
			//System.out.println("rm" + layerPanels.get(index));
			doRemove(layerPanels.get(index));
		}
	}

	public void removeLayerPanel(Overlay layer) {
		LayerPanel layerPanel = null;
		for ( int i = 0; i < layerPanels.size(); i++ ) {
			if ( layerPanels.get(i).layer.equals(layer) ) {
				layerPanel = layerPanels.get(i);
				doRemove(layerPanel);
			}
		}
		if ( layerPanel == null ) {
			return;
		}
		map.repaint();
	}

	public void setLayerList(List<Overlay> layers) {
		this.removeAll();
		this.layerPanels.clear();
		this.overlays.clear();
		
		for (int i = layers.size() - 1; i  >= 0; i--) {
			Overlay layer = layers.get(i);
//			if (ignoreLayers.contains(layer.getLayer()))
//				continue;

			LayerPanel p = new LayerPanel(layer,"",null,true,null);
			add(p);

			this.layerPanels.add(p);
			if ( !overlays.contains(layer) ) {
				this.overlays.add(layer);
			}
		}

		this.setMaximumSize(getPreferredSize());
		this.revalidate();
		this.repaint();
	}

	@Override
	public Dimension getMaximumSize() {
		int width = 0;
		int height = 0;
		for (LayerPanel lp : layerPanels) {
			width = Math.max(lp.prefered_width, width);
			height = Math.max(lp.getPreferredSize().height, height);
		}
		
		return new Dimension( Math.max(width, 83),
							 height * getComponentCount());
	}

	public void setDialog(JFrame inputDialog) {
		lmFrame = inputDialog;
	}

	public static void getLayerSessionChooser() throws IOException{	
		JFileChooser sessionImport = new JFileChooser
		(System.getProperty("user.home") + "/Desktop");
		sessionImport.setDialogTitle("Import Layer Session");
		sessionImport.setAcceptAllFileFilterUsed(true);
		sessionImport.setFileFilter(new FileFilter() {
			public boolean accept(File file) {
				String fileName = file.getName().toLowerCase();
				if (fileName.endsWith(".xml")) {
					return true;
				}
				return false;
				}
			public String getDescription() {
				return "XML file (*.xml)";
			}
		});
		sessionImport.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int code = sessionImport.showOpenDialog(map.getParent());
		if(code==JFileChooser.CANCEL_OPTION){
			doImport = false;
			return;
		}else if(code==JFileChooser.APPROVE_OPTION){
			File f =sessionImport.getSelectedFile();
			String fs = f.toString();
			importLayerSession(fs);
			doImport = true;
		}

	}

	public static void importLayerSession(String xmlImport) throws IOException {
		// Check existence of file structure
		File gmaRootImport = MapApp.getGMARoot();
		File file = new File(gmaRootImport, "layers");
		File xmlImportFile = new File(xmlImport);
		File mySessionFile = new File(file + File.separator + "MySessions.xml");
		if( mySessionFile.exists()) {
			System.out.println("Importing another session.");
			FilesUtil.multiFileToLayer(xmlImportFile, mySessionFile);
		}else{
			System.out.println("Importing first session.");
			FilesUtil.processFileToLayer(xmlImportFile, "MySessions.xml");
		}
		MapApp.setSessionImport(xmlImportFile);
	}

	public static void checkLayerFileStructure(){
		File gmaRootCheck = MapApp.getGMARoot();
		// Before we begin 
		if( gmaRootCheck!=null ) {
			// Check to make sure layers folder is there
			File fileLayers = new File(gmaRootCheck, "layers");
			if( !fileLayers.exists() ) fileLayers.mkdirs();
			// Check to make sure SessionsMenu.xml is there
			File customSessionMenu = new File( fileLayers, "SessionsMenu.xml");
			if( !customSessionMenu.exists() ) {
				try {
					PrintStream out = new PrintStream(
						new FileOutputStream(customSessionMenu));
					out.append( "<Menus>" + '\r' + "<layer" + '\r');
					out.append( '\t'  + "name= " + '"' + "My Layer Sessions" + '"');
					out.append('\n' + '\t' + "url= " + '"' + "import_file" + '"' + '\n');
					out.append('\t' + "import_file =" + '"' + "MySessions.xml" + '"' + '\n');
					out.append('\t' + "proj =" + '"' + "nsm" + '"' + '\n');
					out.append("</layer>" + '\n' + "</Menus>");
					out.flush();
					out.close();
				} catch(IOException ex) {
					System.out.println("Error. Not Created.");
				}
			}
			if(customSessionMenu.exists()){
				try {
					PrintStream out = new PrintStream(
						new FileOutputStream(customSessionMenu));
					out.append( "<Menus>" + '\r' + "<layer" + '\r');
					out.append( '\t' + "name= " + '"' + "My Layer Sessions" + '"' +'\n');
					out.append('\t' + "url= " + '"' + "import_file" + '"' + '\n');
					out.append('\t' + "import_file= " + '"' + "MySessions.xml" + '"' + '\n');
					out.append('\t' + "proj= " + '"' + "nsm" + '"' + ">" + '\n');
					out.append("</layer>" + '\n' + "</Menus>");
					out.flush();
					out.close();
				} catch(IOException ex) {
				}
			}
		}
	}
	
	private void moveNearMainWindow() {
		Window parent = ((MapApp)map.getApp()).getFrame();
		Point loc = parent.getLocation();
		int x = loc.x + parent.getWidth();
		int y = loc.y;
		Rectangle bounds = parent.getGraphicsConfiguration().getDevice().getDefaultConfiguration().getBounds();
		if(x + lmFrame.getWidth() > bounds.width+bounds.x) {
			x = bounds.width + bounds.x - lmFrame.getWidth();
		}
		lmFrame.setLocation(x, y);
	}
	
	public void makeShown(boolean shouldShow) {
		if(shouldShow && !lmFrame.isVisible()) {
			moveNearMainWindow();
			lmFrame.setVisible(shouldShow);
		}
		else {
			lmFrame.setVisible(shouldShow);
		}
	}

//	New overlays added at index 0, so "top/highest" overlay is always at index 0 in this class
	public void propertyChange(PropertyChangeEvent evt) {
		if ( evt.getSource().equals(map) ) {
			// When newValue is Overlay and oldValue is String
			if ( (evt.getNewValue() != null && evt.getNewValue() instanceof Overlay)&&
				(!(evt.getOldValue() instanceof XML_Menu))) {
				//System.out.println("not xml");
				Overlay layer = (Overlay) evt.getNewValue();
					if ( overlays.contains(layer) ) {
						return;
					}

				LayerPanel p = null;
				String layerURLString = null;
				String panelName = null;
					if ( evt.getOldValue() instanceof String ) {
						layerURLString = evt.getOldValue().toString();
					}
	
				XML_Menu menu = null;
				if (evt.getPropertyName() != null) {
					menu = XML_Menu.getXML_Menu(evt.getPropertyName());
				//	p.layerName = evt.getPropertyName(); 
				}
				p = new LayerPanel(layer,evt.getPropertyName(),layerURLString,layer.shouldShow(),menu);
				add(p,0);
				this.layerPanels.add(0,p);
				this.overlays.add(0,layer);
				this.revalidate();
				this.repaint();

				Dimension size = new Dimension(
						getMaximumSize().width+20,
						getMaximumSize().height+ 40);
//				Dimension maxSize = lmFrame.getMaximumSize();

//				size.height = Math.min(size.height, maxSize.height);
//				size.width = Math.min(size.width, maxSize.width);

				lmFrame.setMinimumSize(size);
				lmFrame.setSize(size);

				if ( !evt.getPropertyName().equals(haxby.map.MapApp.baseFocusName) ) {
					if ( XML_Menu.commandToMenuItemHash != null && XML_Menu.commandToMenuItemHash.contains("layer_manager_cmd") ) {
						((JCheckBoxMenuItem)XML_Menu.commandToMenuItemHash.get("layer_manager_cmd")).setSelected(true);
					}
					Window activeWindow = FocusManager.getCurrentManager().getActiveWindow();
					makeShown(true);
					lmFrame.toFront();
					if (activeWindow != null)
						activeWindow.requestFocus();
				}
			}

			//When oldValue is instance of XML_Menu and newValue is instance of Overlay update panels this way
			if ( (evt.getOldValue() != null && evt.getOldValue() instanceof XML_Menu)
					&&(evt.getNewValue() != null && evt.getNewValue() instanceof Overlay) ) {
				Overlay layer = (Overlay) evt.getNewValue();
				if ( overlays.contains(layer) ) {
					return;
				}
				LayerPanel p = null;
				String layerURLString = null;
				XML_Menu menuItem = null;
				String layerName = null;
				if ( evt.getOldValue() != null && evt.getOldValue() instanceof XML_Menu ) {
					menuItem = (XML_Menu) evt.getOldValue();
					layerName = menuItem.name;
					layerURLString = menuItem.infoURLString;
					
					// If layer the layer has a legend, set it here
					if (layer instanceof LegendSupplier) {
						((LegendSupplier)layer).setLegendURL(menuItem.legend);
					}
					
					// If layer is Data Table add to the name.
					if(menuItem.command.contains("table_cmd")){
						layerName = " Data Table: " + menuItem.name;
					}				
				}
				//System.out.println("Opacity" + menuItem.opacity_value);
				p = new LayerPanel(layer,layerName,layerURLString,true,menuItem);
				if(!lmFrame.isVisible()) {
					moveNearMainWindow();
				}
				if (evt.getPropertyName() != null) {
					//p.layerName = evt.getPropertyName(); 
				}
				// If layer is shape_cmd or table_cmd
				if(menuItem.command.contains("shape_cmd") || menuItem.command.contains("table_cmd")) {
					if((layerPanels.size() <= 1) && (menuItem.wesn != null) && (map.getZoom() <= 1)) {
						/* include if you don't want to contain multi image or multi grid (usually
						 * contributed grid that might have its own x and y in the shp file
						 * && (((ESRIShapefile) layer).getMultiImage() == null) &&
						 * (((ESRIShapefile) layer).getMultiGrid() == null)) {
						*/
						double wesnXML[] = new double[4];
						String[] s = menuItem.wesn.split(",");
						for (int i = 0; i < 4; i++) {
							wesnXML[i] = Double.parseDouble(s[i]);
						}
						// Tracks zoom before, does zoom, tracks zoom after
						map.setZoomHistoryPast(map);
						map.zoomToWESN(wesnXML);
						map.setZoomHistoryNext(map);
						}
				}

				/* Look to see if top level Menu Bar is My Session. If so then 	
				 * add at index 0 for first session each item below the first one loaded.
				 */
				if(menuItem !=null){
					if(menuItem.parent !=null){
						XML_Menu t = menuItem.parent;
						while (t.parent !=null){
							t = t.parent;
						}
						String menuName = t.name;
						//System.out.println("Root MenuBar name is " + menuName);
						if(menuName.contentEquals("My Session Layers")){
							if(layerPanels.size()==1){
								add(p,0);
								this.layerPanels.add(0,p);
								this.overlays.add(0,layer);	
							}else{
								int i =layerPanels.size()-1;
								add(p,i);
								this.layerPanels.add(i,p);
								this.overlays.add(i,layer);
							}
						}else{
							add(p,0);
							this.layerPanels.add(0,p);
							this.overlays.add(0,layer);
						}
					}
				} //End My Session look up

				this.setMaximumSize(getPreferredSize());
				this.setSize(getPreferredSize());
				this.revalidate();
				this.repaint();

				lmFrame.setMinimumSize(new Dimension(getMinimumSize().width+20,getMinimumSize().height+55));
				lmFrame.setSize(getMaximumSize().width+20,getMaximumSize().height+55);
				this.revalidate();
				this.repaint();

				if ( !layerName.equals(haxby.map.MapApp.baseFocusName) ) {
					if ( XML_Menu.commandToMenuItemHash != null && XML_Menu.commandToMenuItemHash.contains("layer_manager_cmd") ) {
						((JCheckBoxMenuItem)XML_Menu.commandToMenuItemHash.get("layer_manager_cmd")).setSelected(true);
					}
//					Window parent = lmFrame.getOwner();
//					int x = lmFrame.getLocation().x;
//					x += lmFrame.getWidth();
//					int y = lmFrame.getLocation().y;
//					if (!lmFrame.isVisible()){
//						lmFrame.setLocation(x, y-200);
//					}
					Window activeWindow = FocusManager.getCurrentManager().getActiveWindow();
					makeShown(true);
					lmFrame.toFront();
					if (activeWindow != null)
						activeWindow.requestFocus();
				}
			}

			if ( evt.getNewValue() != null && 
					evt.getNewValue() instanceof Boolean &&
					(Boolean)evt.getNewValue()) {
				LayerPanel tempPanel = null;
				for ( int i = 0; i < layerPanels.size(); i++ ) {
					if ( layerPanels.get(i).layer.equals(evt.getOldValue()) ) {
						tempPanel = layerPanels.get(i);
						break;
					}
				}
				if ( tempPanel != null ) {
					remove(tempPanel);
				}
			}
		}
	}

	public List<Overlay> getOverlays() {
		return overlays;
	}
	
	/*
	 * toggle the visibility of the Plot Profile checkboxes for each layerPanel
	 */
	public void displayPlotProfileCheckBoxes(boolean profileSelected) {
		for (LayerPanel lp : layerPanels) {
			if (lp.plotProfile != null) {
				lp.plotProfile.setVisible(profileSelected);
			}
		}
		this.revalidate();
		this.repaint();
	}
	
	public List<LayerPanel> getLayerPanels() {
		return layerPanels;
	}
	
	/*
	 * get a the layer panel for an overlay
	 */
	public LayerPanel getLayerPanel(Overlay overlay) {
		for (LayerPanel lp : layerPanels) {
			if (lp.layer.equals(overlay)) return lp;
		}
		return null;
	}
	
	/*
	 * Order the Layers based on indices saved during Save Session
	 */
	public void sortLayers() {
		// make a copy of layerPanels that won't get re-ordered
		// need to run twice to make sure everything ends up in the right order
		for (int i=0; i<2; i++) {
			LinkedList<LayerPanel> copyOfLayerPanels = new LinkedList<LayerPanel>();
			copyOfLayerPanels.addAll(layerPanels);
			for (LayerPanel lp : copyOfLayerPanels) {
				Overlay ol = lp.layer;
				if (lp.item != null && lp.item.index != null) {
					//get the index from the saved session item
					int n = 0;
					int index = Integer.parseInt(lp.item.index);
					//adjust the index for any missing layers that didn't load correctly
					for (int missing: missingLayers) {
						if (index > missing) n++;
					}
					index -= n;
					
					if (index < overlays.size()) {
						//move the overlay up or down to reach the desired index
						while (overlays.indexOf(ol) > index) {
							this.up(ol);
						}
						while (overlays.indexOf(ol) < index) {
							this.down(ol);
						}
					}
					// toggle top layer visibility to make sure it is plotted on top 
					if (index == 0) {
						try {
							TimeUnit.SECONDS.sleep(1);
							boolean vis = lp.isVisible();
							setLayerVisible(lp, !vis);
							setLayerVisible(lp, vis);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}			
		}
	}
	
	/*
	 * Move a layer to the top of the layer manager
	 */
	 public void moveToTop(Overlay layer) {
		while (overlays.indexOf(layer) > 0) {
			this.up(layer);
		}
//		this.revalidate();
//		this.repaint();
//		map.repaint();
		
	 }
	 public void moveToTop(String layerName) {
		Overlay layer = getOverlay(layerName);
		if (layerName == null) return;
		moveToTop(layer);
	 }
	 
	 public void missingLayer(String index) {
		 missingLayers.add(Integer.parseInt(index));
		 sortLayers();
	 }
	 
	 public void resetMissingLayers() {
		 missingLayers.clear();
	 }
}
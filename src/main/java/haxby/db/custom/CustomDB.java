package haxby.db.custom;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.geomapapp.db.dsdp.CustomBRGTable;
import org.geomapapp.util.Cursors;
import org.geomapapp.util.Icons;
import org.geomapapp.util.XML_Menu;

import haxby.db.Database;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.util.WESNSupplier;
import haxby.util.XBTable;

public class CustomDB implements Database,
								ActionListener,
								MouseListener,
								MouseMotionListener,
								ItemListener, 
								PopupMenuListener,
								WESNSupplier {

//	GMA 1.4.8: Add vector saveOptions to contain different save options for save combo box
//	public Vector data;
	public Vector<UnknownDataSet> dataSets;
	public Vector<String> saveOptions;
	public JPanel dataPanel = new JPanel(new BorderLayout());

//	GMA 1.4.8: Add save combo box to allow user to view and select save options
//	public JComboBox box;
	public JComboBox box,save;
	protected int maxDBNameLength = 0;
	protected boolean isLoaded,enabled,enableB=false;
	protected XMap map;
	public Point2D.Float point;
	protected JPanel configPanel;
	public AbstractButton configB,graphB,colorB,scaleB,bookB,closeB;
	protected JCheckBox plotB, thumbsB, plotAllB;
	protected JToggleButton zoomOutTB;
	protected JToggleButton zoomInTB;
	protected JToggleButton panTB;
	protected JToggleButton lassoTB = new JToggleButton();
	protected Database currentDB = null;
	JLabel pointsLabel;
	JPopupMenu pm;

	protected static final String errMsg = "Error attempting to launch web browser";

//	GMA 1.4.8: String contains option from "Import Data Tables" in main File menu and title
	public String currentLoadOption = null;
	public String titleOfDataset = null;
	
	private boolean moveToTop = true;

	protected UnknownDataSet currentData = null;

	public CustomDB(XMap map){
		this.map = map;
		dataPanel.setPreferredSize(new Dimension(10,200));
		createTablePopupMenu();

//		***** GMA 1.4.8: Put save options in vector with same name as right-click save options
		saveOptions = new Vector<String>();
		saveOptions.add("Save");
		saveOptions.add("Copy Selection to Clipboard");
		saveOptions.add(" -Table Data to ASCII File");
		saveOptions.add(" -Table Data to Excel File (.xls)");
		saveOptions.add(" -Table Data to Excel File (.xlsx)");
		saveOptions.add(" -Table Data to Google Earth (KMZ)");
		saveOptions.add(" -Plotted Data to ASCII File");
		saveOptions.add(" -Plotted Data to Excel File (.xls)");
		saveOptions.add(" -Plotted Data to Excel File (.xlsx)");
		saveOptions.add(" -Plotted Data to Google Earth (KMZ)");
		saveOptions.add(" -Selection to ASCII File");
		saveOptions.add(" -Selection to Excel File (.xls)");
		saveOptions.add(" -Selection to Excel File (.xlsx)");
		saveOptions.add(" -Selection to Google Earth (KMZ)");

//		***** GMA 1.6.2: Retrieve zoom buttons from main toolbar so that when they are 
//		selected we make sure the lasso button is deselected
		haxby.map.MapApp app;
		app = (haxby.map.MapApp)map.getApp();
		haxby.map.Zoomer zoomer;
		zoomer = app.getZoomer();
		zoomOutTB = zoomer.getZoomOut();
		zoomInTB = zoomer.getZoomIn();
		zoomOutTB.addMouseListener(this);
		zoomInTB.addMouseListener(this);
		panTB = app.getMapTools().panB;
		if (panTB != null) {
			panTB.addMouseListener(this);
		}
//		***** GMA 1.6.2
		CustomBRGTable.setReverseYAxis(false);
		CustomBRGTable.setIgnoreZeros(false);
	}

	private void createTablePopupMenu() {
		pm = new JPopupMenu();
		JMenuItem mi = new JMenuItem("Select Matching Cells");
		mi.setActionCommand("match");
		mi.addActionListener(this);
		pm.add(mi);
		pm.addSeparator();
		createSavePopupMenuItems();
	}

	private void createSavePopupMenuItems() {
		JMenuItem mi = new JMenuItem("Copy Selection to Clipboard");
		mi.setActionCommand("copy");
		mi.addActionListener(this);
		pm.add(mi);
		mi = new JMenuItem("Export Table Data to ASCII File");
		mi.setActionCommand("exportASCII");
		mi.addActionListener(this);
		pm.add(mi);
		mi = new JMenuItem("Export Table Data to Google Earth (KMZ)");
		mi.setActionCommand("exportKML");
		mi.addActionListener(this);
		pm.add(mi);
		mi = new JMenuItem("Export Table Data to Excel File");
		mi.setActionCommand("exportExcel");
		mi.addActionListener(this);
		pm.add(mi);
		
		mi = new JMenuItem("Export Plotted Data to ASCII File");
		mi.setActionCommand("exportPlottedASCII");
		mi.addActionListener(this);
		pm.add(mi);
		mi = new JMenuItem("Export Plotted Data to Excel File");
		mi.setActionCommand("exportPlottedExcel");
		mi.addActionListener(this);
		pm.add(mi);
		mi = new JMenuItem("Export Plotted Data to Google Earth (KMZ)");
		mi.setActionCommand("exportPlottedKML");
		mi.addActionListener(this);
		pm.add(mi);
		
		mi = new JMenuItem("Export Selection to ASCII File");
		mi.setActionCommand("exportSelectASCII");
		mi.addActionListener(this);
		pm.add(mi);
		mi = new JMenuItem("Export Selection to Excel File");
		mi.setActionCommand("exportSelectExcel");
		mi.addActionListener(this);
		pm.add(mi);
		mi = new JMenuItem("Export Selection to Google Earth (KMZ)");
		mi.setActionCommand("exportSelectKML");
		mi.addActionListener(this);
		pm.add(mi);
		mi = new JMenuItem("Close");
		mi.setActionCommand("close");
		mi.addActionListener(this);
		pm.add(mi);
	}

	//	***** GMA 1.5.2: This function added to load URL from data portal
	public void loadURL(String inputName, String inputURL, int inputType) {
		loadURL(inputName, inputURL, inputType, null);
	}

	public void loadURL(String inputName, String inputURL, int inputType, String infoURL) {
		loadURL(inputName, inputURL, inputType, infoURL, null);
	}

	public void loadURL(String inputName, String inputURL, int inputType, String infoURL, XML_Menu xml_menu) {
		Container c = map.getParent();
		while (!(c instanceof Frame)) c=c.getParent(); 
		OtherDBInputDialog dialog = new OtherDBInputDialog((Frame)c,
													inputName,
													inputURL,
													inputType);
//		DBInputDialog dialog = new DBInputDialog((Frame)c,currentLoadOption,titleOfDataset);
		if (dialog.input==null) {
			dialog.dispose();
			return;
		}
		UnknownDataSet d = createDataSet(dialog.desc,
										dialog.input.getText(),
										dialog.getDelimeter(),
										true,
										xml_menu);
		d.setInfoURL( infoURL );
		dialog.dispose();

		if (dataSets == null) dataSets = new Vector<UnknownDataSet>();
		dataSets.add(d);
		dataPanel.removeAll();
		dataPanel.add(d.tp);

		/*Checks OS and version if it is a Mac 10.5+ or 10.6+ then exclude box.addItem(d)
		 * For this OS we do not want to add the WESN zoom button and the method select()
		 * In order for the layer to come up in the layer manager the first time a table is selected.
		 */
		String osVersion = System.getProperty("os.version");
		String OS = System.getProperty("os.name");

		if(OS.contains("Mac OS X") && (osVersion.contains("10.5") || osVersion.contains("10.6")) ) {
			//System.out.println("Found MAC OS X system 10.5 or 10.6");
			box.addItem(d);
			box.setSelectedItem(d);
			select();
		}else{
			box.addItem(d);
			box.setSelectedItem(d);
			select();
		}
		//dataPanel.revalidate();
		updateButtonsState();
	}

	public String getDBName() {
		return "Tool Box";
	}

	public String getCommand() {
		return "Tool Box";
	}

	public String getDescription() {
		return "Import a custom database";
	}

	public boolean loadDB() {
		isLoaded = true;
		return true;
	}

	public boolean isLoaded() {
		return isLoaded;
	}
	
	public void unloadDB() {
		isLoaded = false;
	}
	
	public void disposeDB() {
		while ((dataSets!=null)&&(dataSets.size()>0)) {
			UnknownDataSet d = dataSets.get(0);
			map.removeDataSet(d);
		}
		map.setBaseCursor(Cursor.getDefaultCursor());
		box.setSelectedIndex(-1);
		dataPanel.removeAll();
		isLoaded = false;
		CustomBRGTable.setReverseYAxis(false);
		CustomBRGTable.setIgnoreZeros(false);
		System.gc();
	}

	public void setEnabled(boolean tf) {
		if (tf==enabled)return;
		enabled=tf; 
		if (box!=null)select();
	}

	public boolean isEnabled() {
		return enabled;
	}

	public JComponent getSelectionDialog() {
		if (configPanel==null) initConfig();
		return configPanel;
	}

	public JComponent getDataDisplay() {
		return dataPanel;
	}

	public void actionPerformed(ActionEvent evt) {
		if (evt.getActionCommand().equals("load")) {
			load();
		} else if (evt.getActionCommand().equals("book")) {
			bookmark();
		} else if (evt.getActionCommand().equals("close")) {
			close();
		} else if (evt.getActionCommand().equals("closeB")) {
			map.removeDataSet((UnknownDataSet)box.getSelectedItem());
		} else if (evt.getActionCommand().equals("config")) {
			config();
		} else if (evt.getActionCommand().equals("color")) {
			color();
		} else if (evt.getActionCommand().equals("copy")) {
			copy();
		} else if (evt.getActionCommand().equals("exportExcel")) {
			exportExcel("all");
		} else if (evt.getActionCommand().equals("exportASCII")) {
			exportASCII("all");
		} else if (evt.getActionCommand().equals("exportKML")) {
			exportKML("all");
		} else if (evt.getActionCommand().equals("exportPlottedExcel")) {
			exportExcel("plottable");
		} else if (evt.getActionCommand().equals("exportPlottedASCII")) {
			exportASCII("plottable");
		} else if (evt.getActionCommand().equals("exportPlottedKML")) {
			exportKML("plottable");	
		} else if (evt.getActionCommand().equals("exportSelectExcel")) {
			exportExcel("selection");
		} else if (evt.getActionCommand().equals("exportSelectASCII")) {
			exportASCII("selection");
		} else if (evt.getActionCommand().equals("exportSelectKML")) {
			exportKML("selection");
		} else if (evt.getActionCommand().equals("graph")) {
			graph();
		} else if (evt.getActionCommand().equals("match")) {
			match();
		} else if (evt.getActionCommand().equals("plotS")) {
			togglePlot();
		} else if (evt.getActionCommand().equals("plotAll")) {
			togglePlotAll();			
		} else if (evt.getActionCommand().equals("scale")) {
			scale();
		} else if (evt.getActionCommand().equals("save")) {
//			GMA 1.4.8: Check for action event from new save combo box
			save();
		} else if (evt.getActionCommand().equals("select")) {
			select();
			if (moveToTop) {
				map.moveOverlayToTop(currentData);
			}
			moveToTop = true;
			
		} else if (evt.getActionCommand().equals("thumb")) {
			toggleThumbs();
		}
	}

	//NSS 06/25/21 - don't think this is needed - select() gets called above.
	public void itemStateChanged(ItemEvent evt) {
		if (evt.getSource() == box) select();
	}

	public void mouseClicked(MouseEvent e) {
//		***** GMA 1.6.2: Deselect the lasso button if the zoom buttons in the main toolbar are 
//		selected, deselect the zoom buttons in the main toolbar if the lasso button is selected
		if ( e.getSource() == zoomOutTB || e.getSource() == zoomInTB || e.getSource() == panTB) {
			if ( ( zoomOutTB.isSelected() || zoomInTB.isSelected() || panTB.isSelected())
					&& (lassoTB.isSelected())) {
				lassoTB.doClick();
			}
		} else if ( e.getSource() == lassoTB ) {
			if ( lassoTB.isSelected() ) {
				if (zoomOutTB != null && zoomOutTB.isSelected()) {
					zoomOutTB.doClick();
					zoomOutTB.setSelected(false);
				}
				else if (zoomInTB != null && zoomInTB.isSelected()) {
					zoomInTB.doClick();
					zoomInTB.setSelected(false);
				}
				else if (panTB != null && panTB.isSelected()) {
					panTB.doClick();
					panTB.setSelected(false);
				}
			}
		}
//		***** GMA 1.6.2
		/* On shortcut will graph tabluar data from URL link data. Access Custom Graph. */
		else if ( e.getSource().equals(((UnknownDataSet)box.getSelectedItem()).dataT) && e.isShiftDown() ) {
			String osName = System.getProperty("os.name");
		if ( ( osName.startsWith("Mac OS") && e.isMetaDown() ) || ( !osName.startsWith("Mac OS") && e.isControlDown()) ) {
			Point p = e.getPoint();
				XBTable table = ((UnknownDataSet) box.getSelectedItem()).dataT;
				int col = table.getColumnModel().getColumnIndexAtX(p.x);
				int row = p.y / table.getRowHeight();
				String str = table.getValueAt(row, col).toString();
				String name = table.getValueAt(row, 0).toString();
				if ( HyperlinkTableRenderer.validURL(str) ) {
					table.setRowSelectionInterval( row, row );
					CustomGraph cg = new CustomGraph( str, this, name );
					cg.initDialog();
				}
			}
		} else {
			UnknownDataSet ds = ((UnknownDataSet) box.getSelectedItem());
			Point p = e.getPoint();
			XBTable table = ds.dataT;
			int col = table.getColumnModel().getColumnIndexAtX(p.x);
			int row = p.y / table.getRowHeight();
			ds.tableClicked(row, col);
		}
	}
	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
		tryPopUp(e);
	}

	public void mouseReleased(MouseEvent e) {
		tryPopUp(e);
	}

	public void mouseDragged(MouseEvent e) {}

	public void mouseMoved(MouseEvent e) {
		Point p = e.getPoint();
		XBTable table = ((UnknownDataSet) box.getSelectedItem()).dataT;
		int col = table.getColumnModel().getColumnIndexAtX(p.x);
		int row = p.y / table.getRowHeight();

		if (row >= table.getRowCount() ||
			col >= table.getColumnCount()) {
			table.setCursor(Cursor.getDefaultCursor());
			return;
		}

		// JOC : Fixed an exceptioin when trying to cast Float to String
		String str = table.getValueAt(row, col).toString();

		//Try to display it
		if (HyperlinkTableRenderer.validURL(str)) {
			table.setCursor(Cursors.getCursor(Cursors.HAND));
		} else {
			table.setCursor(Cursor.getDefaultCursor());
		}
	}

	public void initConfig(){
		JPanel p = new JPanel(new GridLayout(0,1,0,0));
		p.add(createLoadButton());
		p.add(createDisposeButton());
		p.add(createConfigButton());

		JPanel p2 = new JPanel(new GridLayout(0,2,0,1));
		p2.add(createColorButton());
		p2.add(createLassoPanel());
		p.add(p2);

		JPanel p3 = new JPanel(new GridLayout(0,2,0,1));
		p3.add(createScaleButton());
		p3.add(createGraphButton());
		p.add(p3);

		p.add(createBookmarkButton());
		p.add(createPointsLabel());
		p.add(createSelectBox());
		p.add(createSaveBox());
		p.add(createPlotCheckbox());
		p.add(createAllPlotableCheckbox());
		p.add(createThumbsCheckbox());
		// JOC: ConfigPanel now uses a Grid Layout.  This lets the all the 
		// sub components scale to the resizing of parent
		createConfigPanel(p);
	}

	protected void createConfigPanel(JPanel p) {
		configPanel = new JPanel(new GridLayout(0,1));
		configPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		configPanel.add(p);

		//JOC : Set a min size width to keep combo boxs under control
		configPanel.setMinimumSize(new Dimension(110, configPanel.getMinimumSize().height));
		configPanel.setPreferredSize(new Dimension(160, configPanel.getPreferredSize().height));
	}

	protected Component createThumbsCheckbox() {
		thumbsB = new JCheckBox("<html>Image Viewer</html>",true);
		thumbsB.setSize(45, 45);
		thumbsB.addActionListener(this);
		thumbsB.setActionCommand("thumb");
		thumbsB.setEnabled(false);
		return thumbsB;
	}

	protected Component createPlotCheckbox() {
		plotB = new JCheckBox("<html>Plot Refresh</html>",true);
		plotB.setSize(50, 45);
		plotB.addActionListener(this);
		plotB.setActionCommand("plotS");
		plotB.setEnabled(false);
		return plotB;
	}

	protected Component createAllPlotableCheckbox() {
		plotAllB = new JCheckBox("<html>Plot All</html>",true);
		plotAllB.setSize(50, 45);
		plotAllB.addActionListener(this);
		plotAllB.setActionCommand("plotAll");
		plotAllB.setEnabled(false);
		return plotAllB;
	}
	
	protected Component createPointsLabel() {
		pointsLabel = new JLabel("<html>0 of 0</html>");
		pointsLabel.setSize(48, 60);
		pointsLabel.setHorizontalAlignment(SwingConstants.CENTER);
		pointsLabel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "Points Displayed", TitledBorder.CENTER, TitledBorder.TOP));

		return pointsLabel;
	}

	protected Component createScaleButton() {
		// GMA 1.6.4: Change "Scale Symbols" to "Scale by Value" to better indicate functionality
		scaleB = new JButton("Scale");
		scaleB.addActionListener(this);
		scaleB.setActionCommand("scale");
		scaleB.setEnabled(false);
		return scaleB;
	}

	protected Component createColorButton() {
		//GMA 1.6.4: Change "Color Symbols" to "Color by Value" to better indicate functionality
		colorB = new JButton("Color");
		colorB.addActionListener(this);
		colorB.setActionCommand("color");
		colorB.setEnabled(false);
		return colorB;
	}

	protected Component createGraphButton() {
		graphB = new JButton("Graph");
		graphB.addActionListener(this);
		graphB.setActionCommand("graph");
		graphB.setEnabled(false);
		return graphB;
	}

	protected Component createConfigButton() {
		configB = new JButton("Configure");
		configB.addActionListener(this);
		configB.setActionCommand("config");
		configB.setEnabled(false);
		return configB;
	}

	protected Component createBookmarkButton() {
		bookB = new JButton("Bookmark");
		bookB.addActionListener(this);
		bookB.setActionCommand("book");
		bookB.setEnabled(false);
		return bookB;
	}

	protected Component createDisposeButton() {
		closeB = new JButton("Dispose");
		closeB.addActionListener(this);
		closeB.setActionCommand("closeB");
		closeB.setEnabled(false);
		return closeB;
	}

	protected Component createLoadButton() {
		JButton b = new JButton("Load");
		b.addActionListener(this);
		b.setActionCommand("load");
		return b;
	}

	protected Component createSaveBox() {
		save = new JComboBox(saveOptions);
		save.addActionListener(this);
		save.addItemListener(this);
		save.addPopupMenuListener(this);
		save.setActionCommand("save");
		return save;
	}

	protected Component createSelectBox() {
		box = new JComboBox();
		box.addActionListener(this);
		box.addItemListener(this);
		box.addPopupMenuListener(this);
		box.setActionCommand("select");
		return box;
	}

	protected JPanel createLassoPanel() {
		JPanel p2 = new JPanel(new BorderLayout());
		JToggleButton tb = new JToggleButton(Icons.getIcon(Icons.LASSO, false));
		tb.setSelectedIcon(Icons.getIcon(Icons.LASSO, true));
		tb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
//				((AbstractButton)e.getSource()).setSelected(true);
				if (((AbstractButton)e.getSource()).isSelected()) {
					map.setBaseCursor(Cursors.getCursor(Cursors.LASSO));
				} else
					map.setBaseCursor(Cursor.getDefaultCursor());
			}
		});
		tb.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 0));
		p2.add(tb, BorderLayout.WEST);
		p2.setBorder(null);
		JLabel l = new JLabel("<html>Lasso<br>Data</html>");
		l.setSize(35, 60);
		l.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
		p2.add(l);

//		***** GMA 1.6.2: Listen for lasso button to ensure that it works correctly with the 
//		zoom buttons in the main toolbar
		lassoTB = tb;
		lassoTB.addMouseListener(this);

		return p2;
	}

	public void config(){
		if (dataSets!=null&&dataSets.size()>0) {
			dataSets.get(box.getSelectedIndex()).config();
			updateButtonsState();
		}
	}

	public void graph() {
		if (dataSets!=null&&dataSets.size()>0)
			dataSets.get(box.getSelectedIndex()).graph();
	}

	public void color() {
		if (dataSets!=null&&dataSets.size()>0)
			dataSets.get(box.getSelectedIndex()).color();
	}

	public void scale() {
		if (dataSets!=null&&dataSets.size()>0)
			dataSets.get(box.getSelectedIndex()).scale();
	}

	public void match() {
		if (dataSets!=null&&dataSets.size()>0)
			dataSets.get(box.getSelectedIndex()).match();
	}

	public void load(MapApp mapApp) {
		MapApp m = mapApp;
		Container c = map.getParent();
		while (!(c instanceof Frame)) c=c.getParent();

		DBInputDialog dialog = new DBInputDialog((Frame)c,currentLoadOption,titleOfDataset);

		if(currentLoadOption==DBInputDialog.IMPORT_EXCEL_FILE ||			// ExcelTablesCmd
				currentLoadOption==DBInputDialog.IMPORT_CLIPBOARD ||		// ClipboardTablesCmd
				currentLoadOption==DBInputDialog.IMPORT_PIPE_TEXT_FILE ||	// PipeTablesCmd
				currentLoadOption==DBInputDialog.IMPORT_TAB_TEXT_FILE ||	// TabTablesCmd
				currentLoadOption==DBInputDialog.IMPORT_COMMA_TEXT_FILE ||	// CommaTablesCmd
				currentLoadOption==DBInputDialog.IMPORT_ASCII_URL ||		// ASCIIURLTablesCmd
				currentLoadOption==DBInputDialog.IMPORT_EXCEL_URL) {		// ExcelURLTablesCmd
			dialog.setVisible(true);
		}
		currentLoadOption = null;

		if (dialog.input==null) {
			dialog.dispose();
			return;
		} else {
			m.addCurrentDBToDisplay();
		}

		UnknownDataSet d = createDataSet(dialog.desc, 
				dialog.input.getText(), 
				dialog.getDelimeter(),
				false);

		dialog.dispose();
		if (dataSets == null) dataSets = new Vector<UnknownDataSet>();
		dataSets.add(d);
		dataPanel.removeAll();
		dataPanel.add(d.tp);
		box.addItem(d);
		box.setSelectedItem(d);
		select();
		updateButtonsState();
		CustomBRGTable.setReverseYAxis(false);
		CustomBRGTable.setIgnoreZeros(false);
	}

	public void load() {
		Container c = map.getParent();
		while (!(c instanceof Frame)) c=c.getParent();

//		GMA 1.4.8: New constructor takes options from main File menu
//		DBInputDialog dialog = new DBInputDialog((Frame)c);
		DBInputDialog dialog = new DBInputDialog((Frame)c,currentLoadOption,titleOfDataset);

		dialog.setVisible(true);
		currentLoadOption = null;

		if (dialog.input==null) {
			dialog.dispose();
			return;
		}

		UnknownDataSet d = createDataSet(dialog.desc, 
				dialog.input.getText(), 
				dialog.getDelimeter(),
				false);

		dialog.dispose();
		if (dataSets == null) dataSets = new Vector<UnknownDataSet>();
		dataSets.add(d);
		dataPanel.removeAll();
		dataPanel.add(d.tp);
		box.addItem(d);
		box.setSelectedItem(d);
		select();
		updateButtonsState();
		CustomBRGTable.setReverseYAxis(false);
		CustomBRGTable.setIgnoreZeros(false);
	}

	protected UnknownDataSet createDataSet(DBDescription desc, String text, String delimeter, boolean skipPrompts) {
		return new UnknownDataSet(desc, text, delimeter, this, skipPrompts);
	}
	protected UnknownDataSet createDataSet(DBDescription desc, String text, String delimeter, boolean skipPrompts, XML_Menu xml_menu) {
		return new UnknownDataSet(desc, text, delimeter, this, skipPrompts, xml_menu);
	}

	//	***** GMA 1.4.8: The save() function checks the option selected from the save combo box and invokes the appropriate save function
	public void save() {
		if ( save.getSelectedItem() == "Copy Selection to Clipboard" ) {
			copy();
		}else if ( save.getSelectedItem() == " -Table Data to ASCII File" ) {
			exportASCII("all");
		}else if ( save.getSelectedItem() == " -Table Data to Excel File (.xls)" ) {
			exportExcel("all");
		}else if ( save.getSelectedItem() == " -Table Data to Excel File (.xlsx)" ) {
			exportExcelXLSX("all");	
		}else if ( save.getSelectedItem() == " -Plotted Data to ASCII File" ) {
			exportASCII("plottable");
		}else if ( save.getSelectedItem() == " -Plotted Data to Excel File (.xls)" ) {
			exportExcel("plottable");
		}else if ( save.getSelectedItem() == " -Plotted Data to Excel File (.xlsx)" ) {
			exportExcelXLSX("plottable");			
		}else if ( save.getSelectedItem() == " -Selection to ASCII File" ) {
			exportASCII("selection");
		}else if ( save.getSelectedItem() == " -Selection to Excel File (.xls)" ) {
			exportExcel("selection");
		}else if ( save.getSelectedItem() == " -Selection to Excel File (.xlsx)" ) {
			exportExcelXLSX("selection");		
		}else if (save.getSelectedItem() == " -Table Data to Google Earth (KMZ)") {
			exportKML("all");		
		}else if (save.getSelectedItem() == " -Plotted Data to Google Earth (KMZ)") {
			exportKML("plottable");	
		}else if (save.getSelectedItem() == " -Selection to Google Earth (KMZ)") {
			exportKML("selection");
		}
		save.setSelectedIndex(0); // JOC: This will now return the save combo selection back to "Save"
	}
//	***** GMA 1.4.8 

	public void updateButtonsState() {
		UnknownDataSet d = ((UnknownDataSet)box.getSelectedItem());
		if (d == null) {
			colorB.setEnabled(false);
			scaleB.setEnabled(false);
			//plotB.setSelected(false);
			plotB.setSelected(true);
			plotAllB.setSelected(true);
			bookB.setEnabled(false);
			configB.setEnabled(false);
			graphB.setEnabled(false);
			closeB.setEnabled(false);
		} else {
			colorB.setEnabled(d.station);
			scaleB.setEnabled(d.station);
			thumbsB.setEnabled(d.station);
			plotB.setSelected(d.plot);
			plotB.setSelected(d.areAllPlottable());
			thumbsB.setSelected(d.thumbs);
			bookB.setEnabled(d.desc.type == -1);
			plotB.setEnabled(true);
			plotAllB.setEnabled(true);
			configB.setEnabled(true);
			graphB.setEnabled(true);
			closeB.setEnabled(true);
		}
	}

	public void draw(Graphics2D g) {
		if (dataSets!=null&&dataSets.size()>0)
			for (UnknownDataSet dataSet : dataSets) {
				dataSet.draw(g);
			}
	}

	public void tryPopUp(MouseEvent evt){
		if (evt.isPopupTrigger()){
			if (evt.getComponent() instanceof XBTable)
				pm.show(evt.getComponent(), evt.getX(), evt.getY());
//			else if (evt.getComponent() instanceof XYGraph)
//				pm.show(evt.getComponent(), evt.getX(), evt.getY());
		}
	}

	public void copy(){
		((UnknownDataSet) box.getSelectedItem()).copy();
	}

	public void exportASCII(String plotOption){
		((UnknownDataSet) box.getSelectedItem()).exportASCII(plotOption);
	}

	public void exportExcel(String plotOption){
		((UnknownDataSet) box.getSelectedItem()).exportExcel(plotOption);
	}

	public void exportExcelXLSX(String plotOption){
		((UnknownDataSet) box.getSelectedItem()).exportExcelXLSX(plotOption);
	}

	public void exportKML(String plotOption) {
		((UnknownDataSet) box.getSelectedItem()).exportKML(plotOption);
	}

	//	Returns the current database that is being displayed and is active
	public haxby.db.Database getCurrentDB() {
		return currentDB;
	}

	public void close(){
		close((UnknownDataSet)box.getSelectedItem());
	}

	public void close(UnknownDataSet d){
		try {
			d.dispose();
		} catch ( NullPointerException npe ) {
			npe.printStackTrace();
		}
		dataSets.remove(d);
		dataSets.trimToSize();

		dataPanel.remove(d.tp);
		pointsLabel.setText("<html>0 of 0</html>");
		box.removeItem(d);
		if (dataSets.size()>0) box.setSelectedIndex(box.getItemCount()-1);
		updateButtonsState();
		select();
		map.repaint();
		dataPanel.repaint();

		CustomBRGTable.setReverseYAxis(false);
		CustomBRGTable.setIgnoreZeros(false);

		System.gc();
	}

	public void close(DBGraph graph) {
		if (graph.isLinePlot()) {
			int z = box.getSelectedIndex();
			UnknownDataSet d = dataSets.get(z);
			d.tp.remove( d.tp.getSelectedIndex() );
			graph.dispose();

			for (int i = 0; i < dataPanel.getComponentCount();i++)
				dataPanel.getComponent(i).repaint();
			dataPanel.revalidate();
			repaintMap();
		} else {
			int z = box.getSelectedIndex();
			UnknownDataSet d = dataSets.get(z);
			graph.dispose();
			d.graphs.remove(graph);
		}

		CustomBRGTable.setReverseYAxis(false);
		CustomBRGTable.setIgnoreZeros(false);
	}

	public void select() {
		if (currentData == box.getSelectedItem()) {
			return;
		}

		dataPanel.removeAll();
		if (box.getSelectedItem()==null) {
			map.removeOverlay(this);
			currentData = null;
			return;
		}
		

		UnknownDataSet unknownDataSet = (UnknownDataSet)box.getSelectedItem();
		
		String nameDS = "Data Table: " + unknownDataSet.desc.name;
		
		unknownDataSet.setEnabled(enabled);
		dataPanel.add(unknownDataSet.tp);

		for (int i = 0; i < dataPanel.getComponentCount();i++) {
			dataPanel.getComponent(i).repaint();
		}
		//System.out.println("total size " + unkownDataSet.scene.total_icon_count + " data size " + unkownDataSet.tm.displayToDataIndex.size());
		updateButtonsState();
		dataPanel.revalidate();

		map.addOverlay(nameDS, unknownDataSet.getInfoURL(), unknownDataSet, unknownDataSet.xml_menu);

		map.repaint();
		currentData = unknownDataSet;
		for (UnknownDataSet ds : dataSets) {
			map.removeMouseListener(ds);
			map.removeMouseMotionListener(ds);
		}
		map.addMouseListener(currentData);
		map.addMouseMotionListener(currentData);
	}

	public void togglePlot() {
		UnknownDataSet d = (UnknownDataSet) box.getSelectedItem();
		d.plot = plotB.isSelected();

//		***** GMA 1.4.8: Make this DB the top overlay
		if ( plotB.isSelected() ) {
			map.moveOverlayToTop(d);
		}

		map.repaint();
	}


	//toggle between plotting all datapoints and only the ones that the user has selected
	public void togglePlotAll() {
		UnknownDataSet d = (UnknownDataSet) box.getSelectedItem();

		if ( plotAllB.isSelected() ) {
			d.rememberPlottableStatus();
			d.makeAllPlottable();
		} else {
			d.revertToOldPlottableStatus();
		}
		map.moveOverlayToTop(d);
		d.dataT.repaint();
		map.repaint();
	}
	
	
	public void toggleThumbs() {
		UnknownDataSet d = (UnknownDataSet) box.getSelectedItem();
		d.thumbs = thumbsB.isSelected();
		d.thumbViewer.updateRow();
	}

	public void drawCurrentPoint() {
		if( map==null || point==null || !map.isVisible() ) return;
		synchronized (map.getTreeLock() ) {
			Graphics2D g = map.getGraphics2D();
			float zoom = (float)map.getZoom();
			g.setStroke( new BasicStroke( 2f/ zoom ) );
			g.setColor(Color.RED);
			g.setXORMode( Color.white );
			Rectangle2D rect = map.getClipRect2D();
			double wrap = map.getWrap();
			if( wrap>0. ) while( point.x-wrap > rect.getX() ) point.x-=wrap;
			double size = 10./map.getZoom();
			Arc2D.Double arc = new Arc2D.Double( 0., point.y-.5*size, 
							size, size, 0., 360., Arc2D.CHORD);
			if( wrap>0. ) {
				while( point.x < rect.getX()+rect.getWidth() ) {
					arc.x = point.x-.5*size;
					g.draw(arc);
					point.x += wrap;
				}
			} else {
				arc.x = point.x-.5*size;
				g.draw(arc);
			}
		}
	}

	public void repaintMap() {
		point = null;
		map.repaint();
	}

	public void bookmark(){
		((UnknownDataSet) box.getSelectedItem()).bookmark();
	}

	public void popupMenuCanceled(PopupMenuEvent e) {}

	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
//		GMA 1.4.8: Now check which combo box event is coming from
		if ( e.getSource() == box ) {

//			***** Changed by A.K.M. 6/23/06 *****
//			setPrototypeDisplayValue restricts the size of the box to a fixed 
//			length of eight characters
			box.setPrototypeDisplayValue("WWWWWWWW");
//			The popup listener adjusts the size of the popup to match the size 
//			of the text being displayed
			JComboBox tempBox = (JComboBox) e.getSource();
			Object comp = tempBox.getUI().getAccessibleChild(tempBox, 0);
			if (!(comp instanceof JPopupMenu)) {
				return;
			}
			JComponent scrollPane = (JComponent) ((JPopupMenu) comp).getComponent(0);
			Dimension size = scrollPane.getPreferredSize();
			UnknownDataSet tester1 = (UnknownDataSet)tempBox.getSelectedItem();
			CustomBRGTable.setReverseYAxis(false);
			CustomBRGTable.setIgnoreZeros(false);
//			6.5 is a hardcoded value that approximates the size of a 
//			character in pixels
//			TODO: Find exact size of text in pixels and adjust 
//			size.width accordingly
			if (tester1 != null) {
				if (maxDBNameLength < tester1.desc.name.length())	{
						maxDBNameLength = tester1.desc.name.length();
				}
				size.width = (int)(maxDBNameLength * 6.5);
				scrollPane.setPreferredSize(size);
			}
//			***** Changed by A.K.M. 6/23/06 *****
		}
	}

	public double[] getWESN() {
		if (box == null) return null;
		UnknownDataSet ds = (UnknownDataSet) box.getSelectedItem();
		if (ds == null) return null;
		return ds.getWESN();
	}
	
	public void setCurrent(UnknownDataSet ds) {
		moveToTop = false;
		box.setSelectedItem(ds);
	}
}
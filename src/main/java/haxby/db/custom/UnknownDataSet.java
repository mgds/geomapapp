package haxby.db.custom;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.geomapapp.image.ColorScaleTool;
import org.geomapapp.io.GMARoot;
import org.geomapapp.util.SymbolScaleTool;
import org.geomapapp.util.XML_Menu;

import haxby.db.XYGraph;
import haxby.map.MapApp;
import haxby.map.Overlay;
import haxby.map.XMap;
import haxby.util.BrowseURL;
import haxby.util.GeneralUtils;
import haxby.util.LayerManager.LayerPanel;
import haxby.util.SceneGraph;
import haxby.util.SceneGraph.SceneGraphEntry;
import haxby.util.WESNSupplier;
import haxby.util.XBTable;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;


public class UnknownDataSet implements MouseListener,
										MouseMotionListener,
										ListSelectionListener,
										PropertyChangeListener,
										WESNSupplier, 
										Overlay {
	public XBTable dataT;
	public XMap map;
	public XML_Menu xml_menu;
	public DBDescription desc;
	public DataSetImageViewer thumbViewer;
	public DBGraphDialog dbg;
	public DBTableModel tm;
	public CustomDB db;
	private LayerPanel layerPanel;

	public Vector<UnknownData> data;
	private Vector<Boolean> newTrack;
	private int numTracks = 1;
	public Vector<String> header;
	public Vector<Vector<Object>> rowData, origData;
	public Vector<DBGraph> graphs=new Vector<DBGraph>();
	public Vector<DataSetGraph> xypoints=new Vector<DataSetGraph>();
	public List<Integer> polylines = new LinkedList<Integer>();
	public JScrollPane tableSP;
	public JPanel dataP = new JPanel(new BorderLayout());
	public JTabbedPane tp = new JTabbedPane();
	public ColorScaleTool cst = null;
	public SymbolScaleTool sst = null;
	public String shapeString = "circle";
	public int numCommentRows = 0;
	public GeneralPath path;
	public Point p1,p2;
	public Rectangle r;
	public Polygon poly;

	public static final int ASCII_FILE = 0;
	public static final int EXCEL_FILE = 1;
	public static final int ASCII_URL = 2;
	public static final int EXCEL_URL = 3;
	protected static final int LIMIT_GRAPHICS_MEMORY = 20000; //	***** GMA 1.6.4: Add variable that sets the number of data points displayed
	public int polylineIndex = -1;
	public int rgbIndex = -1;
	public int lonIndex = -1;
	public int latIndex = -1; 
	public int anotIndex = -1;
	public int symbolSize = 100;
	public float lineThick = 1f;
	public String lineStyleString = "solid";
	public int lastSelected = 0;
	private int selectedRow = -1;

	//line styles for plotting tracks
	protected float[] solid = null;
	protected float[] dashed = {5f};
	protected float[] dotted = {2f};
	protected float[] dotdashed = {5, 3, 2, 3};

	public boolean station = true;
	public boolean[] selected;
	public boolean drawOutlines = true;
	public boolean plot = true;
	public boolean thumbs = true;
	public boolean enabled = true;
	public boolean updateScale=true;
	public boolean limitGraphics = true;
	public boolean omitCommentRows = false;

	protected float[] f,f2;
	protected float[] wesn;
	protected int colorColumnIndex = 0;
	protected int colorNumericalColumnIndex = 0;
	protected int scaleColumnIndex = 0;
	protected int scaleNumericalColumnIndex = 0;
	protected int selRows[];
	public SceneGraph scene;
	protected int totalSize, displaySize;

	private Color color = Color.GRAY;
	private MouseAdapter columnSorter;
	private KeyAdapter copyListener;
	private String infoURL;
	protected SortedMap<Integer, Integer> commentLinesMap = new TreeMap<>();

	private ArrayList<Boolean> oldPlottableStatus = null;
	
	private boolean hasLoaded = false;
	
	public UnknownDataSet(DBDescription desc, String input, String delim, CustomDB db){
		this(desc,input,delim,db,false);
	}

	public UnknownDataSet(DBDescription desc, String input, String delim, CustomDB db, boolean skipPrompts){
		this(desc, input, delim, db, false, null);
	}
	
	protected Vector<Boolean> getNewTrack() {
		return newTrack;
	}

	// cut down version of constructor used by Survey Planner import and the Velocity Vectors portal
	public UnknownDataSet(DBDescription desc, String input, String delim, XMap map){
		this.map = map;
		this.desc = desc;
		this.db = null;
		this.scene = new SceneGraph(this.map, 4);
		this.xml_menu = null;
		this.layerPanel = null;

		input=input.trim();

		BufferedReader in = new BufferedReader(new StringReader(input));
		try {
			int blankLines = 0;
			int commentLines = 0;
			header=new Vector<String>();
			String s;
			StringTokenizer st;
			data = new Vector<UnknownData>();
			while ((s = in.readLine())!=null){
				if(s.equals("")) {
					blankLines++;
					continue;
				}
				// Only process lines that don't start with the comment symbol #
				if(!s.startsWith("#")) {
					commentLinesMap.put(data.size(), commentLines + blankLines + 1);
					if (header.size() == 0) {
						st = new StringTokenizer(s,delim);
						while (st.hasMoreTokens()) header.add(st.nextToken().trim());
						header.trimToSize();
						continue;
					}

					st = new StringTokenizer(s,delim, true);
					Vector<Object> data2 = new Vector<Object>(header.size());
					while (true) {
						try {
							String s2 = st.nextToken();
							if (delim.indexOf(s2)>=0) {
								data2.add("");
							} else {
								data2.add(s2.trim());
								st.nextToken();
							}
						} catch (NoSuchElementException ex){
							break;
						}
					}
					for (int i = data2.size(); i < header.size(); i++)
						data2.add("");
					data.add( new UnknownData(data2) );
				}
				else {
					commentLines++;
					commentLinesMap.put(data.size(), commentLines + blankLines + 1);
				}
			}
			data.trimToSize();
			initData();
			tm = new DBTableModel(this, false);
			initTable();
			config(true, true);

			selected=new boolean[data.size()];
			in.close();

		} catch (IOException ex){
			ex.printStackTrace();
		}
	}
	
	public UnknownDataSet(DBDescription desc, String input, String delim, CustomDB db, boolean skipPrompts, XML_Menu xml_menu){
		this.map = db.map;
		this.desc = desc;
		this.db = db;
		this.scene = new SceneGraph(this.map, 4);
		this.xml_menu = xml_menu;

		input=input.trim();

		BufferedReader in = new BufferedReader(new StringReader(input));
		try {
			int commentLines = 0, blankLines = 0;
			header=new Vector<String>();
			String s;
			StringTokenizer st;
			data = new Vector<UnknownData>();
			newTrack = new Vector<Boolean>();
			numTracks = 1;
			while ((s = in.readLine())!=null){
				if(s.equals("")) {
					blankLines++;
					continue;
				}
				// Only process lines that don't start with the comment symbol #
				if(!s.startsWith("#")) {
					//commentLinesMap.put(data.size(), commentLines + blankLines);
					if (header.size() == 0) {
						if(s.startsWith(">")) {
							blankLines++;
							continue;
						}
						st = new StringTokenizer(s,delim);
						while (st.hasMoreTokens()) header.add(st.nextToken().trim());
						header.trimToSize();
						continue;
					}
					// if a line starts with a >, we want to lift the pen and start 
					// a new line when drawing tracks
					if (s.startsWith(">")) {
						newTrack.add(true);
						if (data.size() > 0) numTracks++;
						s = s.substring(1);
						if (s.length() == 0 || (s.split(delim).length != header.size())) {
							s = in.readLine();
							while (s.startsWith("#")) {
								commentLines++;
								s = in.readLine();
							}
						}
					} else newTrack.add(false);
					commentLinesMap.put(data.size(), commentLines + blankLines);
					st = new StringTokenizer(s,delim, true);
					Vector<Object> data2 = new Vector<Object>(header.size());
					while (true) {
						try {
							String s2 = st.nextToken();
							if (delim.indexOf(s2)>=0) {
								data2.add("");
							} else {
								data2.add(s2.trim());
								st.nextToken();
							}
						} catch (NoSuchElementException ex){
							break;
						}
					}
					for (int i = data2.size(); i < header.size(); i++)
						data2.add("");
					data.add( new UnknownData(data2) );
				}
				else {
					commentLines++;
				}
			}
			data.trimToSize();
			initData();
			tm = new DBTableModel(this);
			initTable();
			config(skipPrompts);	// final config window before plotting.
			map.addMouseListener(this);
			map.addMouseMotionListener(this);
			selected=new boolean[data.size()];
			in.close();

//			***** GMA 1.6.4: Add variable that sets the number of data points displayed
//			if (station&&data.size()>5000)
//				JOptionPane.showMessageDialog(MapApp.anchor, "The entered dataset has over 5000 stations." +
//					  "\nPlease note all stations are loaded in the GeoMapApp table and \n will appear as you zoom in to your area of interest. \n  While zoomed out, some stations may not be shown due to graphic memory limitations.", LIMIT_GRAPHICS_MEMORY + "+ Stations", JOptionPane.INFORMATION_MESSAGE);
			if (station&&data.size()>LIMIT_GRAPHICS_MEMORY) {
				JFrame messageFrame = new JFrame();
				Object[] options = {"Plot All Records",							// yes
									"Plot Geographically-Decimated Subset"};	// no
				int selection = JOptionPane.showOptionDialog(messageFrame, "<html>The selected data set has <b>" + data.size() + "</b> records.</html>\n"
												 + "\n" + "<html>Choose either to plot all records ( impacts the graphic card performance ) <b>or</b> to</html>\n"
												+ "plot a geographically-decimated subset of the data ( potentially faster but may\n"
												+ "not display all valid points ).\n",
												"More Than 20,000 Records", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
				if (selection == JOptionPane.YES_OPTION) {
					limitGraphics = false;
				} else if (selection == JOptionPane.NO_OPTION) {
					limitGraphics = true;
				}
			}

			/* This brings up a progress bar to bypass a Mac OS x 10.5+ bug which happens in
			 * the loading of the data tables layers. The wesn and zoom icon layer will not appear.
			 * A pause is needed to bypass the disappearance. 
			 */
			String osVersion = System.getProperty("os.version");
			String OS = System.getProperty("os.name");
			if(OS.contains("Mac OS X") && (osVersion.contains("10.5") || osVersion.contains("10.6") || osVersion.contains("10.7"))){
				//System.out.println("Found MAC OS X system 10.5 or 10.6");
				 if(station && data.size() > 5) {
					 JDialog d = new JDialog((Frame)null, "Opening Table");
						JPanel p = new JPanel(new BorderLayout());
						p.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
						d.setLocationRelativeTo(MapApp.anchor);
						int length = 10;
						JProgressBar pb = new JProgressBar(0,length);
						p.add(new JLabel("Progressing"), BorderLayout.NORTH);
						p.add(pb);
						d.getContentPane().add(p);

						d.pack();
						d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
						d.setVisible(true);

						pb.setValue(pb.getValue() + length);
						pb.repaint();

						d.dispose();
					}
			}

//			***** GMA 1.6.4
			Container owner = db.map.getTopLevelAncestor();
			if (owner instanceof JFrame) {
				thumbViewer = new DataSetImageViewer(this,(JFrame)owner);
			} else {
				thumbViewer = new DataSetImageViewer(this,(JDialog)owner);
			}
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}

	public void initData(){
		rowData = new Vector<Vector<Object>>();
		origData = new Vector<Vector<Object>>();
		for (UnknownData d : data) {
			rowData.add(d.data);
			//take copy of data to compare with if we edit later
			origData.add((Vector<Object>) d.data.clone());
		}
		rowData.trimToSize();
	}
		
	public void initTable(){
		dataP.removeAll();
		// JOC Removed extra instannsiation and streamlined renderer
		//tm = new DBTableModel(this);
		//final TableCellRenderer renderer = new HyperlinkTableRenderer();
		
		dataT = new XBTableExtension(tm);
		//set the width of the Plot column
		dataT.getColumnModel().getColumn(0).setPreferredWidth(40);
		
		dataT.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		dataT.setScrollableTracksViewportWidth(false);
		tableSP = new JScrollPane(dataT);
		
		dataT.getSelectionModel().addListSelectionListener(this);
		dataP.add(tableSP);

		if (tp.getComponentCount()==0)tp.add(dataP,desc.name);
		tp.setForegroundAt(0,Color.BLACK);

		dataT.addMouseListener(db);
		dataT.addMouseMotionListener(db);

		dataT.addKeyListener( getCopyListener() );
		dataT.getTableHeader().addMouseListener( getColumnSorter() );

		dataT.getTableHeader().addMouseListener(this);
		dataT.getTableHeader().addMouseMotionListener(this);
	}
	
	public void updateDataSet() {
		initData();
		scene.clearScene();

		float[] wesn = new float[] {Float.MAX_VALUE, -Float.MAX_VALUE, 
				Float.MAX_VALUE, -Float.MAX_VALUE}; 
		polylines.clear();

		int index = 0;
		for (UnknownData d : data) {
			float[] lonLat = d.getPointLonLat(lonIndex, latIndex);
			if(lonLat != null) {
				wesn[0] = Math.min(lonLat[0], wesn[0]);
				wesn[1] = Math.max(lonLat[0], wesn[1]);
				wesn[2] = Math.min(lonLat[1], wesn[2]);
				wesn[3] = Math.max(lonLat[1], wesn[3]);
			}
			d.updateXY(map,lonIndex,latIndex);
			d.updatePolyline(map, polylineIndex);
			if (d.polyline != null) {
				float[] polylineWESN = d.getPolylineWESN(polylineIndex);
				wesn[0] = Math.min(wesn[0], polylineWESN[0]);
				wesn[1] = Math.max(wesn[1], polylineWESN[1]);
				wesn[2] = Math.min(wesn[2], polylineWESN[2]);
				wesn[3] = Math.max(wesn[3], polylineWESN[3]);
				
				polylines.add(index);
			}

			d.updateRGB(rgbIndex);
			if (!Float.isNaN(d.x) &&
						!Float.isNaN(d.y)) {
					scene.addEntry(new UnknownDataSceneEntry(index));
			}
			index++;
		}
	}
	
	private KeyListener getCopyListener() {
		if (copyListener == null) {
			copyListener = new KeyAdapter() {
				public void keyReleased(KeyEvent evt) {
					if( evt.isControlDown() && evt.getKeyCode()==KeyEvent.VK_C ) copy();
				}
			};
		}
		return copyListener;
	}

	private MouseListener getColumnSorter() {
		if (columnSorter == null) {
			columnSorter = new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					int c = dataT.getTableHeader().columnAtPoint(e.getPoint());
					boolean[] os = selected;
					tm.sortByColumn(dataT.convertColumnIndexToModel(c));
					//update the color scale to take into account the reordering of the data
					updateColorScale();
					selectionChangedRedraw(os);
					
				}
			};
		}
		return columnSorter;
	}

	public void copy() {
		StringBuffer sb = new StringBuffer();
		for (int i=0;i<dataT.getColumnCount();i++) {
			sb.append(dataT.getColumnName(i)+"\t");
		}
		sb.append("\n");
		int sel[] = dataT.getSelectedRows();
		for (int i=0;i<sel.length;i++) {
			for (int j=0; j<dataT.getColumnCount();j++) {
				Object o = dataT.getValueAt(sel[i], j);
				if (o instanceof String && ((String)o).equals("NaN")) o = "";
				sb.append(o.toString()+"\t");
			}
			sb.append("\n");
		}
		Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection ss = new StringSelection(sb.toString());
		c.setContents(ss, ss);
	}

	public void config(){
		config(false);
	}

	public void config(boolean skipPrompts) {
		config(skipPrompts, false);
	}
	
	public void config(boolean skipPrompts, boolean sp){
		//System.out.println(skipPrompts);
		final DBConfigDialog config = 
			new DBConfigDialog((Frame)map.getTopLevelAncestor(),this);
		config.okClicked = hasLoaded;

		if (skipPrompts) {
			config.addWindowListener(new WindowAdapter() {
				public void windowOpened(WindowEvent e) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							config.ok();
						}
					});
				}
			});
		}
		if(!skipPrompts){
			config.setVisible(true);
		}else{
			config.toBack();
			config.setSize(0, 0);
			if (!sp) config.setVisible(true);
			config.setVisible(false);
		}

		config.dispose();
		
		if (db != null){
			db.map.requestFocus();
			db.repaintMap();
		}
		hasLoaded = config.okClicked;
	}

	//Return only columns that contain numerical data
	public ArrayList<String> getNumericalColumns() {
		ArrayList<String> o2 = new ArrayList<>();
		String colName;
		int col = 0;
		for (int i = 0; i < tm.indexH.size(); i++) {
			col = tm.indexH.get(i);
			if (col == dataT.getPlotColumnIndex()) continue;
			colName = header.get(col);
			int j =0;
			//skip empty rows
			while (j < totalSize && 
					(tm.ds.rowData.get(j).get(col).toString().isEmpty() || 
							tm.ds.rowData.get(j).get(col).toString().equals("null"))) j++;			
			try {
				//check whether we can convert to a double
				double d = Double.parseDouble(tm.ds.rowData.get(j).get(col).toString());
				o2.add("<html>" + colName + "</html>");
			} catch(Exception e) { 
				//not numeric data - don't include
			}
		}
		return o2;
	}
	
	public void color() {
		if (!station) return;

		Container c = map.getParent();
		while (!(c instanceof Frame)) c=c.getParent();

		//get a list of column names that contain only numerical data
		ArrayList<String> o2 = getNumericalColumns();
	
		Object o = JOptionPane.showInputDialog(c, "<html>Choose column to color symbols by: <br><br>"
				+ "<i>Hint: change the symbol shape in<br>the Configure menu to avoid confusion<br>when coloring multiple datasets.<br><br></i>", 
				"Select Column",
					JOptionPane.QUESTION_MESSAGE, null, o2.toArray(), o2.get(colorNumericalColumnIndex));
		if (o==null)return;
		
		colorNumericalColumnIndex = o2.indexOf(o);
		String colName = GeneralUtils.html2text(o.toString());
		for (colorColumnIndex = 0; colorColumnIndex < tm.indexH.size(); colorColumnIndex++) {
			if (header.get(tm.indexH.get(colorColumnIndex)).equals(colName)) break;
		}
		f = new float[tm.displayToDataIndex.size()];
		int z = 0;
		for (int i = 0; i < f.length; i++)
			try {
				// Get column selected numbers
				f[i] = Float.parseFloat(tm.getValueAt(i, colorColumnIndex).toString());
				z++;
			} catch (Exception ex) {
				f[i] = Float.NaN;
			}
		if (z<2) {
			f = null;
			return;
		}
		
		if (cst == null){
			cst = new ColorScaleTool(f);
			cst.addPropertyChangeListener(this);
		}
		else cst.setGrid(f);

//		1.3.5: Assign appropriate name to color scale window
		cst.setName(colName + " - " + desc.name);
		cst.showDialog((JFrame)c);
		map.repaint();
	}

	public void scale() {
		if (!station) return;

		Container c = map.getParent();
		while (!(c instanceof Frame)) c=c.getParent();

		//get a list of column names that contain only numerical data
		ArrayList<String> o2 = getNumericalColumns();
	
		Object o = JOptionPane.showInputDialog(c, "<html>Choose column to color symbols by: <br><br>"
				+ "<i>Hint: change the symbol shape in<br>the Configure menu to avoid confusion<br>"
				+ "when scaling multiple datasets.<br><br></i>", "Select Column",
					JOptionPane.QUESTION_MESSAGE, null, o2.toArray(), o2.get(scaleNumericalColumnIndex));
		if (o==null)return;
		
		scaleNumericalColumnIndex = o2.indexOf(o);
		String colName = GeneralUtils.html2text(o.toString());
		
		for (scaleColumnIndex = 0; scaleColumnIndex < tm.indexH.size(); scaleColumnIndex++) {
			if (header.get(tm.indexH.get(scaleColumnIndex)).equals(colName)) break;
		}

		f2 = new float[tm.displayToDataIndex.size()];
		int z = 0;
		for (int i = 0; i < f2.length; i++)
			try {
				f2[i] = Float.parseFloat(tm.getValueAt(i, scaleColumnIndex).toString());
				z++;
			} catch (Exception ex) {
				f2[i] = Float.NaN;
			}

		if (z<2) {
			f2 = null;
			return;
		}

		if (sst == null){
			System.out.println("uds mapzoom: " + map.getZoom());
			sst = new SymbolScaleTool(f2, map);
			sst.addPropertyChangeListener(this);
		}
		else sst.setGrid(f2);

//		1.3.5: Assign appropriate name to scale scale window 
		sst.setName(colName + " - " + desc.name);

		sst.showDialog((JFrame)c);
		map.repaint();
	}

	public void graph(){
		Container c = map.getParent();
		while (!(c instanceof Frame)) c=c.getParent();
		if (dbg==null) dbg = new DBGraphDialog((Frame)c,this);
		dbg.showDialog();
	}

	public void dispose(){
		map.removeMouseListener(this);
		map.removeMouseMotionListener(this);
		dataT.getSelectionModel().removeListSelectionListener(this);

		dataT.removeKeyListener(copyListener);
		copyListener = null;

		dataT.getColumnHeader().removeMouseListener(columnSorter);
		columnSorter = null;

		dataT.getTableHeader().removeMouseListener(this);
		dataT.getTableHeader().removeMouseMotionListener(this);

		dataT.removeMouseListener(db);
		dataT.removeMouseMotionListener(db);

		dataP.removeAll();
		tableSP.setViewportView(null);
		dataT.getSelectionModel().removeListSelectionListener(this);
		dataT.setModel(new DefaultTableModel());
		dataT.removeListeners();
		dataT = null;

		for (DBGraph graph : graphs)
			graph.dispose();

		graphs.clear();
		polylines.clear();

		if (dbg!=null) {
			dbg.disposeDialog();
			dbg = null;
		}

		thumbViewer.dispose();
		thumbViewer = null;

		tm.dispose();
		tm = null;

		if (cst!=null) {
			cst.removePropertyChangeListener(this);
			cst.dispose();
		}
		if (sst!=null) {
			sst.removePropertyChangeListener(this);
			sst.dispose();
		}
		cst = null;
		sst = null;
		
		layerPanel = null;

		scene.clearScene();
	}

	public void setEnabled(boolean e) {
		if (e==enabled) return;
		enabled = e;
		if (e) {
			if (cst!=null) cst.showDialog((JDialog)null);
			if (sst!=null) sst.showDialog((JDialog)null);
			map.addMouseListener(this);
			map.addMouseMotionListener(this);
			thumbViewer.updateRow();
		}
		else {
			if (cst!=null) cst.hideDialog();
			if (sst!=null) sst.hideDialog();
			map.removeMouseListener(this);
			map.removeMouseMotionListener(this);
			thumbViewer.setVisible(false);
		}
	}

	public void draw( Graphics2D g) {
		if( map==null) return;
		if (dataT==null) return;
		if (!plot) return;
		if (db != null) db.plotAllB.setSelected(areAllPlottable());
		if (lonIndex==-1||latIndex==-1) return;
		lastSelected=0;

		//if color scale tool is not being used, reset f
		if (cst != null && !cst.isShowing()) f = null;
		//if size scale tool is not being used, reset f2
		if (sst != null && !sst.isShowing()) f2 = null;
		
		Rectangle2D rect = map.getClipRect2D();
		float yMin = (float)rect.getY();
		float yMax = (float)(rect.getY() + rect.getHeight());
		float xMin = (float)rect.getX();
		float xMax = (float)(rect.getX() + rect.getWidth());
		float wrap = (float)map.getWrap();
		//System.out.println(xMin+"\t"+xMax+"\t"+yMin+"\t"+yMax);
		AffineTransform at = g.getTransform();
		double mapZoom = map.getZoom();
		g.setStroke( new BasicStroke( 1f/(float)mapZoom) );
		Font font = new Font("SansSerif", Font.PLAIN, 12);
		font = font.deriveFont( 12f/(float)mapZoom );
		g.setFont(font);

		double zoom = Math.pow( mapZoom, .75 );

		if (station) {
			tm.setArea(rect, zoom);
			if (cst!=null&&cst.isShowing()&&updateScale) updateColorScale();
			if (sst!=null&&sst.isShowing()&&updateScale) updateSymbolScale();

			double scale = symbolSize / 50.;
			Arc2D.Double arc = new Arc2D.Double(scale * -1.5/zoom,
					scale * -1.5/zoom,
					scale * 3/zoom,
					scale * 3/zoom,
					0., 360., Arc2D.CHORD );
			int kount=0;

//			***** GMA 1.6.4: Add variable that sets the number of data points displayed

			int limit = limitGraphics ? LIMIT_GRAPHICS_MEMORY : data.size();		// for now 20k default or take the max if user chooses.
		
			List<SceneGraphEntry> entriesToDraw;
			if (tm.getRowCount() <= limit) {
				entriesToDraw = scene.getAllEntries(map.getClipRect2D());
			} else {
				if(limitGraphics == true) {
					// Plot Geographically-Decimated Subset
					entriesToDraw = scene.getEntriesToDraw(map.getClipRect2D(), mapZoom);
				} else {
					entriesToDraw = scene.getAllEntries(map.getClipRect2D());
				}
			}

			for (SceneGraphEntry entry : entriesToDraw) {
				int z = entry.getID();
				int k = tm.rowToDisplayIndex.get(z);
				UnknownData d = data.get(z);
				//don't plot if Plot column is not checked
				if (!isPlottable(k)) continue;
				
				if (dataT.isRowSelected(k)) {
					kount ++;
					continue;
				}

				Color fill=getPointColor(d, k, false);
				Color border=getOutlineColor(d, k, fill);

				if (f2 != null && sst != null && sst.isShowing()
						&& sst.isReady() && k < f2.length) {
					float r = (float) (scale * 1.5 / zoom * 
							(sst.getSizeRatio(f2[k])));
					arc = new Arc2D.Double(-r, -r, r * 2, r * 2, 0., 360.,
							Arc2D.CHORD);
				} else {
					arc = new Arc2D.Double(scale * -1.5/zoom,
							scale * -1.5/zoom,
							scale * 3/zoom,
							scale * 3/zoom,
							0., 360., Arc2D.CHORD );
				}
				kount = drawData(g, at, arc, d, xMin, xMax, yMin, yMax, wrap, kount, limit, fill, border);
			}
			// decimated or allowed, total in map view, total for the set
//			System.out.println(kount + "\t" + tm.displayToDataIndex.size() + "\t" + data.size());
			if (this.enabled && db != null)
				updateTotalDataSize(kount, data.size());

			// Draw the selected items
			for( int k=0 ; k<tm.displayToDataIndex.size() ; k++) {
				if (!dataT.isRowSelected(k))
					continue;

				int z = tm.displayToDataIndex.get(k);
				UnknownData d = data.get(z);
				//don't plot if Plot column is not checked
				if (!isPlottable(k)) continue;
				if (f != null && 
						k < f.length &&
						Float.isNaN(f[k]))
					continue;

				Color a = getPointColor(d, k, true);
				Color b = Color.WHITE;

				if (f2 != null && 
						k < f2.length &&
						Float.isNaN(f2[k]))
					continue;
				else if (f2 != null && 
						sst != null &&
						sst.isShowing() &&
						sst.isReady() && 
						k < f2.length) { 
					float r = (float) (scale * 1.5/zoom * (sst.getSizeRatio(f2[k])));
					arc = new Arc2D.Double(-r, -r, r*2, r*2,
							0., 360., Arc2D.CHORD );
				} else {
					arc = new Arc2D.Double(scale * -1.5/zoom,
							scale * -1.5/zoom,
							scale * 3/zoom,
							scale * 3/zoom,
							0., 360., Arc2D.CHORD );
				}

				kount = drawData(g, at, arc, d, xMin, xMax, yMin, yMax, wrap, kount, limit, a, b);
			}

			// Now try to draw all the polylines
			for (Integer polyI : polylines) {
				UnknownData d = data.get(polyI);
				if (d.polyline == null) continue; // Just doublechecking

				Color fill = Color.black;
				if (tm.rowToDisplayIndex != null && polyI < tm.rowToDisplayIndex.size()) 
					if (dataT.isRowSelected(tm.rowToDisplayIndex.get(polyI)))
						fill = Color.red;

				drawPolyline(g, at, d, xMin, xMax, yMin, yMax, wrap, fill);
			}
		} else {

			// if the input table has been split in to multiple tracks (using the > symbol at the start of the line)
			// we want to draw each track separately so that we have to option to color each track differently
			int startTrack = 0;
			int endTrack = 0;
			for (int t=0; t<numTracks && startTrack < data.size(); t++) {
				boolean start = true;
				GeneralPath shape = new GeneralPath();
		
				for (int i=startTrack; i<data.size(); i++) {
					endTrack = i;
					UnknownData d = data.get(i);
					UnknownData prev = (i > startTrack) ? data.get(i-1) : null;
					UnknownData next = (i < data.size() - 1 && !newTrack.get(i+1)) ? data.get(i+1) : null;
					float d_x = d.x;
					float prev_x = prev != null ? prev.x : 0;
					float next_x = next != null ? next.x : 0;
					
					//don't plot if Plot column is not checked
					Boolean plottable = (Boolean) d.data.get(dataT.getPlotColumnIndex());
					if (!plottable || Float.isNaN(d.x) || Float.isNaN(d.y)) {
						// if this is the end of the track, exit loop and draw it
						if (i < data.size() - 2 && newTrack.get(i+1)) break;
						continue;
					}
	
					//some fiddling around to make sure all points are in the correct wrap segment
					if (wrap>0f){
	
						while (d_x < xMin && d_x + wrap < xMax) {d_x += wrap;} 
						while (d_x > xMax && d_x - wrap > xMin) {d_x -= wrap;}
					
						if( prev != null) {
							while( d_x-prev_x < wrap/2f ){d_x+=wrap;}
							while( d_x-prev_x > wrap/2f ){d_x-=wrap;}
						} 
								
						if ( next != null) {
							while (next_x < xMin && next_x + wrap < xMax) {next_x += wrap;} 
							while (next_x > xMax && next_x - wrap > xMin) {next_x -= wrap;}
						}
					}

					//only draw tracks if a station is in the visible map, or if the track 
					//intersects the map
					if (rect.contains(d_x, d.y) ||
					   (prev != null && rect.intersectsLine(prev_x, prev.y, d_x, d.y)) ||
					   (next != null && rect.intersectsLine(d_x, d.y, next_x, next.y))) {
						
						if (start){
							shape.moveTo(d.x, d.y);
							start = false;
							// if this is the end of the track, exit loop and draw it
							if (i < data.size() - 2 && newTrack.get(i+1)) break;
							continue;
						}
						
						shape.lineTo(d.x, d.y);
						//if this point and next point aren't in the visible map, pick up the pen
						//and stop drawing.
						if (!rect.contains(d_x, d.y) && (next != null && !rect.contains(next_x, next.y)) ) start = true;
					}

					// if this is the end of the track, exit loop and draw it
					if (i < data.size() - 2 && newTrack.get(i+1)) break;
				}
				
				// determine if track colors have been included in the table
				UnknownData startPoint = data.get(startTrack);
				if (startPoint.rgb != null) {
					g.setColor(new Color(startPoint.rgb[0], startPoint.rgb[1], startPoint.rgb[2]));
				} else {
					g.setColor(color);
				}
				startTrack = endTrack + 1;

				//set up the linestyle for displaying the track
				float[] linestyle = null;
				switch (lineStyleString) {
					case "solid": 
						linestyle = null;
						break;
					case "dashed":
						linestyle = new float[dashed.length];
						System.arraycopy(dashed, 0, linestyle, 0, dashed.length);
						break;
					case "dotted":
						linestyle = new float[dotted.length];
						System.arraycopy(dotted, 0, linestyle, 0, dotted.length);
						break;
					case "dash-dotted":
						linestyle = new float[dotdashed.length];
						System.arraycopy(dotdashed, 0, linestyle, 0, dotdashed.length);
						break;
				}
				//scale for the zoom level
				if (linestyle != null) {
					for (int i=0; i<linestyle.length; i++) {
						linestyle[i] /= mapZoom;
					}
				}
				
				//set the stroke with the selected linestyle and thickness
				try {
					g.setStroke( new BasicStroke( lineThick/(float)mapZoom, BasicStroke.CAP_BUTT,
					        BasicStroke.JOIN_BEVEL, 0, linestyle, 0.0f) );
				} catch (Exception ex) {
					g.setStroke( new BasicStroke( 1f/(float)mapZoom) );
				}
						
				g.draw(shape);
				path=shape;		
	
				if(wrap>0) {
					float offset = 0;
					while( xMin+(double)offset < xMax ) {
						g.translate( wrap, 0.d );					
						g.draw(shape);
						offset += wrap;
					}
					g.setTransform( at );
				}
				
				if (!enabled) return;
	
				if (dataT.getSelectedRowCount()>1) {
					for (int i = 0; i < data.size(); i++) {
						start = true;
						float x = 0f;
						shape = new GeneralPath();
						while (i < data.size() && dataT.isRowSelected(i)) {
							UnknownData d = data.get(i);
							if (Float.isNaN(d.x) || Float.isNaN(d.y)){
								i++;
								continue;
							}
							if (start){
								shape.moveTo(d.x, d.y);
								start = false;
								x=d.x;
								continue;
							}
							if (wrap>0f){
								while (d.x>x+wrap/2) d.x-=wrap;
								while (d.x<x-wrap/2) d.x+=wrap;
							}
							shape.lineTo(d.x, d.y);
							x=d.x;
							i++;
						}
						if (enabled) g.setColor(Color.white);
						else g.setColor(color);
						g.draw(shape);
	
						float offset = -wrap;
						if(wrap>0) {
							offset += wrap;
							while( xMin+(double)offset < xMax ) {
								g.translate( wrap, 0.d );
								g.draw(shape);
								offset += wrap;
							}
							g.setTransform( at );
						}
					}
				}
				if (this.enabled && db != null)
					updateTotalDataSize(0,0);
				
				g.setStroke( new BasicStroke(1f) ); //reset the stroke linestyle and thickness
			}
		}

		for (DBGraph graph : graphs) {
			DataSetGraph dsg = graph.getDataSetGraph();
			XYGraph xyg = graph.getXYGraph();

			if (dsg != null && dsg.scatter) dsg.updateRange();

			xyg.setPoints(dsg, 0);
			xyg.repaint();
		}
		updateScale=true;
	}

	private void drawPolyline(Graphics2D g, AffineTransform at, UnknownData d,
			float xMin, float xMax, float yMin, float yMax, float wrap, Color fill) {
		if (d.polyline == null) return;

		int w = d.polyline.getBounds().width;
		int h = d.polyline.getBounds().height;
		int x0 = d.polyline.getBounds().x;
		int y0 = d.polyline.getBounds().y;

		float x = d.polyX0;
		float y = d.polyY0;
		if (y + y0 > yMax) return;
		if (y + y0 + h < yMin) return;
		if( wrap>0f ) {
			while( x + x0 > xMin+wrap ) x -= wrap;
			while( x + x0 + w < xMin ) x += wrap;
			while( x < xMax ) {
				g.translate( x, y );
				g.setColor(fill);
				g.draw(d.polyline);

				x += wrap;
				g.setTransform(at);
			}
		} else {
			g.translate( x, y );
			g.setColor(fill);
			g.draw(d.polyline);
			x += wrap;
			g.setTransform(at);
		}
	}

	public static Shape createStar(int arms, Point2D center, double rOuter, double rInner)
	{
		double angle = Math.PI / arms;

		GeneralPath path = new GeneralPath();

		for (int i = 0; i < 2 * arms; i++)
		{
			double r = (i & 1) == 0 ? rOuter : rInner;
			Point2D.Double p = new Point2D.Double(center.getX() + Math.cos(i * angle) * r, center.getY() + Math.sin(i * angle) * r);
			if (i == 0) path.moveTo(p.getX(), p.getY());
			else path.lineTo(p.getX(), p.getY());
		}
		path.closePath();
		return path;
	}

	private int drawData(Graphics2D g, AffineTransform at, Shape arc, UnknownData d,
			float xMin, float xMax, float yMin, float yMax, float wrap, int kount, int limit, Color a, Color b) {

		if(shapeString.equalsIgnoreCase("square"))
			arc = new Rectangle2D.Double(((Arc2D.Double)arc).getX(), ((Arc2D.Double)arc).getY(),((Arc2D.Double)arc).getWidth(),((Arc2D.Double)arc).getHeight() );

		if (shapeString.equalsIgnoreCase("triangle")){
			double x = ((Arc2D.Double)arc).getX();
			double y = ((Arc2D.Double)arc).getY();
			double width = ((Arc2D.Double)arc).getWidth();
			double height = ((Arc2D.Double)arc).getHeight();

			arc = new Path2D.Double();
			((Path2D.Double)arc).moveTo(x, y+height);
			((Path2D.Double)arc).lineTo(x+(width/2.0), y);
			((Path2D.Double)arc).lineTo(x+width, y+height);
			((Path2D.Double)arc).lineTo(x, y+height);
		}

		if(shapeString.equalsIgnoreCase("star")){
			double x = ((Arc2D.Double)arc).getX();
			double y = ((Arc2D.Double)arc).getY();
			double width = ((Arc2D.Double)arc).getWidth();
			double height = ((Arc2D.Double)arc).getHeight();

			arc = createStar(5, new Point2D.Double(x+(width/2.0),y+(height/2.0)), width/2.0, width/5.0);
		}

		float x = d.x;
		float y = d.y;
		//System.out.println(d+"\t"+yMin+"\t"+yMax);
		if (y > yMax) return kount;
		if (y < yMin) return kount;
		if( wrap>0f ) {
			while( x>xMin+wrap ) x -= wrap;
			while( x<xMin ) x += wrap;
			if( x<xMax ) {
				kount++;
				if(kount>limit) {
					return kount;
				}
			}
			while( x<xMax ) {
				g.translate( x, y );
				g.setColor(a);
				g.fill(arc);
				g.setColor(b);
				g.draw(arc);
				x += wrap;
				g.setTransform(at);
			}
		} else {
			if( x>xMin && x<xMax ) {
				kount++;
				if(kount>limit)
					return kount;
				g.translate( x, y );
				g.setColor(a);
				g.fill(arc);
				g.setColor(b);
				g.draw(arc);
				g.setTransform( at);
			}
		}
		return kount;
	}

	private Color getOutlineColor(UnknownData d, int k, Color fill) {
		if (f != null && cst != null && cst.isShowing() && k < f.length)
			return Color.black;
		else 
			return drawOutlines ? Color.black : fill;
	}

	private Color getPointColor(UnknownData d, int k, boolean selected) {
		if (f != null && cst != null && cst.isShowing() && k < f.length)
			return cst.getColor(f[k]);
		else if (d.rgb != null)
			return new Color(d.rgb[0], d.rgb[1], d.rgb[2]);
		else if (selected)
			return Color.red;
		else
			return color;
	}

	public void mouseClicked(MouseEvent evt) {
		if (evt.isControlDown()) {
			return;
		}
		if (evt.isConsumed()||!map.isSelectable()) {
			return;
		}
		if( map==null) {
			return;
		}
		if (lonIndex==-1||latIndex==-1) {
			return;
		}

		if (station){
			boolean selectPoint = selectPoint(evt);
			if (selectPoint) return;
			boolean selectPolyline = selectPolyline(evt);;
			if (selectPolyline) return;

			dataT.getSelectionModel().clearSelection();

		} else {
			selectTrack(evt);
		}
	}

	private boolean selectPolyline(MouseEvent evt) {
		float wrap = (float)map.getWrap();

		double zoom = map.getZoom();
		Insets insets = map.getMapBorder().getBorderInsets(map);
		float x = (float) ((evt.getX()-insets.left)/zoom);
		float y = (float) ((evt.getY()-insets.top)/zoom);

		double minDist = 8 / zoom;

		for (Integer polyI : polylines) {
			UnknownData d = data.get(polyI);
			if (d.polyline == null) continue;
			
			PathIterator pi = d.polyline.getPathIterator(new AffineTransform());
			float coords0[] = new float[2];
			float coords1[] = new float[2];
			pi.currentSegment(coords0);

			float distN = Float.MAX_VALUE;

			while (!pi.isDone()) {
				pi.next();
				pi.currentSegment(coords1);

				float x0 = coords0[0] + d.polyX0;
				float y0 = coords0[1] + d.polyY0;
				float x1 = coords1[0] + d.polyX0;
				float y1 = coords1[1] + d.polyY0;

				coords0[0] = coords1[0];
				coords0[1] = coords1[1];

				while (true) {
					x0 -= x;
					y0 -= y;
					x1 -= x;
					y1 -= y;

					// Now rotate the two points so we're dealing
					// with a vertical line

					float dx = x1 - x0;
					float dy = y1 - y0;

					if (dx == 0 || dy == 0) { // special case 
						if (dx == 0 && y0 * y1 < 0) {
							distN = Math.min(distN, Math.abs(x0));
						}
						else if (dy == 0 && x0 * x1 < 0) {
							distN = Math.min(distN, Math.abs(y0));
						}
					} else {
						float b = y0 - dy / dx * x0;

						float xi = b / (-dx / dy - dy / dx);
						float yi = xi * -dx / dy;

						float minX = Math.min(x0, x1);
						float maxX = Math.max(x0, x1);
						if (xi < maxX && xi > minX) {
							float dist = (float) Math.sqrt(xi * xi + yi * yi);
							distN = Math.min(distN, dist);
							continue;
						} 
					}

					float dist = (float) Math.sqrt(x0 * x0 + y0 * y0);
					distN = Math.min(distN, dist);
					
					dist = (float) Math.sqrt(x1 * x1 + y1 * y1);
					distN = Math.min(distN, dist);
					
					x0 += x;
					y0 += y;
					x1 += x;
					y1 += y;

					if (Math.min(x0, x1) >= wrap) break;
					if (wrap <= 0) break;

					x0 += wrap;
					x1 += wrap;
				}
			}

			if (distN < minDist && 
					tm.rowToDisplayIndex != null && 
					polyI < tm.rowToDisplayIndex.size()) {
				lastSelected = tm.rowToDisplayIndex.get(polyI);

				dataT.getSelectionModel().setSelectionInterval(lastSelected, lastSelected);
				tableSP.getVerticalScrollBar().setValue(lastSelected*dataT.getRowHeight());
				return true;
			}
		}
		return false;
	}

	private void selectTrack(MouseEvent evt) {
		float wrap = (float)map.getWrap();
		double zoom = map.getZoom();
		Insets insets = map.getMapBorder().getBorderInsets(map);
		double x = (evt.getX()-insets.left)/zoom;
		double y = (evt.getY()-insets.top)/zoom;
		zoom = Math.pow( zoom, .75 );

		PathIterator pi = path.getPathIterator(map.getGraphics2D().getTransform());
		float coords[]=new float[2];
		pi.currentSegment(coords);
		int i=0;
		int s=0;
		double distN=(float) Math.sqrt(Math.pow(x-(coords[0]/zoom+wrap),2) + Math.pow(y-coords[1]/zoom,2));
		while (!pi.isDone()) {
			i++;
			pi.next();
			pi.currentSegment(coords);
			double dist =(float) Math.sqrt(Math.pow(x-(coords[0]/zoom+wrap),2) + Math.pow(y-coords[1]/zoom,2));
			if (dist<distN){
				s=i;
				distN=dist;
			}
			pi.currentSegment(coords);

			if (wrap > 0f) {
				double x0 = 0;
				dist = Math.sqrt(Math.pow(x-coords[0], 2)+Math.pow(y-coords[1], 2));
				while (x0 < wrap) {
					x0+=wrap;
					double dist2 = Math.sqrt(Math.pow(x-x0-coords[0], 2)+Math.pow(y-coords[1], 2)); 
					if (dist2<dist) dist=dist2;
				}
			} else {
				dist = Math.sqrt(Math.pow(x-coords[0], 2)+Math.pow(y-coords[1], 2));
			}

			if (dist>=distN)continue;

			distN=dist;
			s=i;

		}

		if (distN>2) return;
		if (s<0)s=0;
		if (s>=data.size()-2)s=data.size()-3;
		dataT.getSelectionModel().setSelectionInterval(s, s+2);
		tableSP.getVerticalScrollBar().setValue((s-1)*dataT.getRowHeight());
	}

	private boolean selectPoint(MouseEvent evt) {
		float wrap = (float)map.getWrap();

		double zoom = map.getZoom();
		Insets insets = map.getMapBorder().getBorderInsets(map);
		double x = (evt.getX()-insets.left)/zoom;
		double y = (evt.getY()-insets.top)/zoom;
		zoom = Math.pow( zoom, .75 );

		UnknownData nearest = null;
		double dist;
		for (int i = lastSelected+1;i<tm.displayToDataIndex.size();i++) {

			double distN;

			if (f2!=null&&i<f2.length&&Float.isNaN(f2[i])) continue;
			else if (f2!=null&&sst!=null&&sst.isShowing()&&sst.isReady()&&i<f2.length) {
				//System.out.println(sst.getSizeRatio(f2[k]));
				distN = (float) (symbolSize/50. * 1.5/zoom * (sst.getSizeRatio(f2[i])));
			} else {
				distN = (float) (symbolSize/50. * 1.5/zoom);
			} if (Double.isNaN(distN)) continue;

			if (f!=null&&i<f.length&&Float.isNaN(f[i])) continue;
			UnknownData d = data.get(tm.displayToDataIndex.get(i));

			//don't plot if Plot column is not checked
			if (!isPlottable(i)) continue;
			
			if( Float.isNaN(d.x) || Float.isNaN(d.y) ) continue;

			if (wrap > 0f) {
				double x0 = 0;
				dist = Math.sqrt(Math.pow(x-d.x, 2)+Math.pow(y-d.y, 2));
				while (x0 < wrap) {
					x0+=wrap;
					dist = Math.min(Math.sqrt(Math.pow(x-x0-d.x, 2)+Math.pow(y-d.y, 2)),dist); 
				}
			} else {
				dist = Math.sqrt(Math.pow(x-d.x, 2)+Math.pow(y-d.y, 2));
			}

			if (dist>distN)continue;

			distN = dist;
			nearest = d;
			lastSelected = i;
			selectedRow = i;
			break;
			//System.out.println(x+"\t"+y+"\t"+d.x+"\t"+d.y);
		}
		if (nearest==null && tm.displayToDataIndex.size() > 0){
			for (int i = 0;i<=lastSelected;i++) {
				double distN;
				if (f2!=null&&i<f2.length&&Float.isNaN(f2[i])) continue;
				else if (f2!=null&&sst!=null&&sst.isShowing()&&i<f2.length) {
					//System.out.println(sst.getSizeRatio(f2[k]));
					distN = (float) (symbolSize/50. * 1.5/zoom * (sst.getSizeRatio(f2[i])));
				} else {
					distN = (float) (symbolSize/50. * 1.5/zoom);
				} if (Double.isNaN(distN)) continue;

				if (f!=null&&i<f.length&&Float.isNaN(f[i])) continue;
				if (i >= tm.displayToDataIndex.size()) continue;
				UnknownData d = data.get(tm.displayToDataIndex.get(i));
				if( Float.isNaN(d.x) || Float.isNaN(d.y) ) continue;

				if (wrap > 0f) {
					double x0 = 0;
					dist = Math.sqrt(Math.pow(x-d.x, 2)+Math.pow(y-d.y, 2));
					while (x0 < wrap) {
						x0+=wrap;
						dist = Math.min(Math.sqrt(Math.pow(x-x0-d.x, 2)+Math.pow(y-d.y, 2)),dist);
					}
				} else {
					dist = Math.sqrt(Math.pow(x-d.x, 2)+Math.pow(y-d.y, 2));
				}

				if (dist>=distN)continue;

				distN = dist;
				nearest = d;
				lastSelected = i;
				selectedRow = i;
				break;
				//System.out.println(x+"\t"+y+"\t"+d.x+"\t"+d.y);
			}
		}
		if (nearest==null)
			return false;

		if (evt.isShiftDown()) {
			if (dataT.isRowSelected(lastSelected))
				dataT.getSelectionModel().removeSelectionInterval(lastSelected, lastSelected);
			else {
				dataT.getSelectionModel().addSelectionInterval(lastSelected, lastSelected);
				tableSP.getVerticalScrollBar().setValue(lastSelected*dataT.getRowHeight());
			}
		} else {
			dataT.getSelectionModel().setSelectionInterval(lastSelected, lastSelected);
			tableSP.getVerticalScrollBar().setValue(lastSelected*dataT.getRowHeight());
		}
		return true;
	}

	public void mouseEntered(MouseEvent e) {
		if ( e.getSource().equals(dataT.getTableHeader()) ) {
			Point p = e.getPoint();
			int col = dataT.getColumnModel().getColumnIndexAtX(p.x);
			int row = p.y / dataT.getRowHeight();
			if ( ( row + 1 ) < 0 || col < 0 ) {
				return;
			}

			if (row + 1 >= dataT.getRowCount() ||
					col  >= dataT.getColumnCount()) {
				return;
			}

			if (tm.getColumnClass(col) == String.class) {
				String str = dataT.getValueAt(row + 1, col).toString();
				if ( HyperlinkTableRenderer.validURL(str) ) {
					String osName = System.getProperty("os.name");
					if ( osName.startsWith("Mac OS") ) {
						dataT.getTableHeader().setToolTipText("Command-Shift-Click to graph tabular data");
					}
					else {
						dataT.getTableHeader().setToolTipText("Ctrl-Shift-Click to graph tabular data");
					}
				}
			}
		}
	}
	public void mouseExited(MouseEvent e) {
		if ( e.getSource().equals(dataT.getTableHeader()) ) {
			dataT.getTableHeader().setToolTipText("");
		}
	}
	public void mousePressed(MouseEvent e) {
		
		if (station){
			//This will detect if the mouse clicks on a datapoint 
			//and will set the selectedRow value.  Used for dragging.
			selectedRow = -1;
			selectPoint(e);
		}		
		
		if (e.isControlDown()) return;
		if (e.isConsumed()||!map.isSelectable()) return;

		if (db == null || db.panTB.isSelected()) return;
		if (e.isShiftDown()) {
			p1=e.getPoint();
			p2=new Point(p1.x+1,p1.y+1);
			drawSelectionBox();
		}
		else {
			poly = new Polygon();
			poly.addPoint(e.getPoint().x, e.getPoint().y);
		}
	}
	public void mouseReleased(MouseEvent e) {

		if (db == null || db.panTB.isSelected() || e.getModifiers()==4 || !db.lassoTB.isSelected()) return;

		if (poly!=null) {
			poly.addPoint(poly.xpoints[0], poly.ypoints[0]);
			drawLasso();
			selectLasso();
			poly=null;
		} else if (p1!=null) {
			selectBox();
			drawSelectionBox();
			p1=null;
		}
	}
	
	//convert mouse location to lat/lon	
	private Point2D getMouseLatLon(MouseEvent e) {
		double zoom = map.getZoom();
		Insets insets = map.getMapBorder().getBorderInsets(map);
		double x = (e.getX()-insets.left)/zoom;
		double y = (e.getY()-insets.top)/zoom;
		return map.getProjection().getRefXY(x,y);
	}
	
	public void mouseDragged(MouseEvent e) {
		
		if (station){
			//if a datapoint is selected, move it to a new location and update the lat/lon values.
			//only works for editable datasets (ie imported datasets)
			if (tm.editable && selectedRow >= 0) {
				Point2D latlon = getMouseLatLon(e);
				tm.setValueAt(String.valueOf(latlon.getX()), selectedRow, lonIndex);
				tm.setValueAt(String.valueOf(latlon.getY()), selectedRow, latIndex);
			}
		}
		
		if (db == null || db.panTB.isSelected() || e.getModifiers()==4 || !db.lassoTB.isSelected()) return;

		if (poly!=null){
			if (Math.abs(poly.xpoints[poly.npoints-1]-e.getX())<=1
					&&Math.abs(poly.ypoints[poly.npoints-1]-e.getY())<=1)  return;
			poly.addPoint(e.getX(), e.getY());
			drawLasso();
		} else if (p1!=null) {
			drawSelectionBox();
			p2=e.getPoint();
			drawSelectionBox();
		}
	}
	public void mouseMoved(MouseEvent e) {
		if ( e.getSource().equals(dataT.getTableHeader()) ) {
			Point p = e.getPoint();
			
			int col = dataT.getColumnModel().getColumnIndexAtX(p.x);
			int row = p.y / dataT.getRowHeight();
			if ( ( row + 1 ) < 0 || col < 0 ) {
				return;
			}

			if (row + 1 >= dataT.getRowCount() ||
					col >= dataT.getColumnCount()) {
				return;
			}

			if (tm.getColumnClass(col) == String.class) {
				String str = dataT.getValueAt(row + 1, col).toString();
	
				if ( HyperlinkTableRenderer.validURL(str) ) {
					String osName = System.getProperty("os.name");
					if ( osName.startsWith("Mac OS") ) {
						dataT.getTableHeader().setToolTipText("Command-Shift-Click to graph tabular data");
					}
					else {
						dataT.getTableHeader().setToolTipText("Ctrl-Shift-Click to graph tabular data");
					}
				}
				else {
					dataT.getTableHeader().setToolTipText("");
				}
			}
		}
	}

	public void valueChanged(ListSelectionEvent evt) {
		if (evt.getValueIsAdjusting()) return;

		selRows = dataT.getSelectedRows();
		boolean[] oSelected = selected;

		selected = new boolean[data.size()];
		for (int i = 0; i < selRows.length; i++) {
			int index = tm.displayToDataIndex.get(selRows[i]);
			selected[index] = true;
		}
		selectionChangedRedraw(oSelected);
	}

	public void selectionChangedRedraw(boolean[] oldSelection){
		//TODO: If we have an image put it in our image box

		for (DBGraph graph : graphs) {
			XYGraph xyg = graph.getXYGraph();
			DataSetGraph dsg = graph.getDataSetGraph();
			dsg.selectionChangedRedraw(oldSelection);
			synchronized (xyg.getTreeLock()) {
				xyg.paintComponent(xyg.getGraphics(), false);
			}
		}

		if (!enabled) return;
		if (db == null) return;
		synchronized (map.getTreeLock()) {
			Graphics2D g = map.getGraphics2D();
			if (station && plot) {
				//if (!plot) return;

				Rectangle2D rect = map.getClipRect2D();
				float yMin = (float)rect.getY();
				float yMax = (float)(rect.getY() + rect.getHeight());
				float xMin = (float)rect.getX();
				float xMax = (float)(rect.getX() + rect.getWidth());
				float wrap = (float)map.getWrap();
				//System.out.println(xMin+"\t"+xMax+"\t"+yMin+"\t"+yMax);
				AffineTransform at = g.getTransform();
				double zoom = map.getZoom();
				g.setStroke( new BasicStroke( 1f/(float)zoom) );
				zoom = Math.pow( zoom, .75 );

				double scale = symbolSize / 50.;
				Shape arc = new Arc2D.Double(scale * -1.5/zoom,
						scale * -1.5/zoom,
						scale * 3/zoom,
						scale * 3/zoom,
						0., 360., Arc2D.CHORD );

				for( int k=0 ; k<tm.displayToDataIndex.size() ; k++) {
					int index = tm.displayToDataIndex.get(k);
					if (!oldSelection[index] || selected[index]) continue;

					int z = tm.displayToDataIndex.get(k);
					UnknownData d = data.get(z);
					if( Float.isNaN(d.x) || Float.isNaN(d.y) ) continue;

					Color a=null,b=Color.BLACK;
					if (f!=null&&k<f.length&&Float.isNaN(f[k])) continue;

					a = getPointColor(d, k, false);
					b = getOutlineColor(d, k, a);

					if (f2!=null&&k<f2.length&&Float.isNaN(f2[k])) continue;
					else if (f2!=null&&sst!=null&&sst.isShowing()&&sst.isReady()&&k<f2.length) {
						float r = (float) (scale * 1.5/zoom * (sst.getSizeRatio(f2[k])));
						arc = new Arc2D.Double(-r, -r, r*2, r*2,
								0., 360., Arc2D.CHORD );
					} else {
						arc = new Arc2D.Double(scale * -1.5/zoom,
								scale * -1.5/zoom,
								scale * 3/zoom,
								scale * 3/zoom,
								0., 360., Arc2D.CHORD );
					}

					drawData(g, at, arc, d, xMin, xMax, yMin, yMax, wrap, 0, 1000, a, b);
				}
				for (int k =0; k<tm.displayToDataIndex.size();k++) {
					int index = tm.displayToDataIndex.get(k);
					if (oldSelection[index] || !selected[index]) continue;

					UnknownData d = data.get(index);
					if( Float.isNaN(d.x) || Float.isNaN(d.y) ) continue;

					Color a=getPointColor(d, k, true);
					Color b=Color.WHITE;

					//Symbol s = new Symbol(Symbol.CIRCLE, (float) (3./zoom), b, a);

					if (f2!=null&&k<f2.length&&Float.isNaN(f2[k])) continue;
					else if (f2!=null&&sst!=null&&sst.isShowing()&&sst.isReady()&&k<f2.length) {
						float r = (float) (scale * 1.5/zoom * (sst.getSizeRatio(f2[k])));
						arc = new Arc2D.Double(-r, -r, r*2, r*2,
								0., 360., Arc2D.CHORD );
					} else {
						arc = new Arc2D.Double(scale * -1.5/zoom,
								scale * -1.5/zoom,
								scale * 3/zoom,
								scale * 3/zoom,
								0., 360., Arc2D.CHORD );
					}

					drawData(g, at, arc, d, xMin, xMax, yMin, yMax, wrap, 0, 1000, a, b);
				}

				// Now try to draw all the polylines
				for (Integer polyI : polylines) {
					UnknownData d = data.get(polyI);
					if (d.polyline == null) continue;

					Color fill = Color.black;
					if (tm.rowToDisplayIndex != null && polyI < tm.rowToDisplayIndex.size()) 
						if (dataT.isRowSelected(tm.rowToDisplayIndex.get(polyI)))
							fill = Color.red;

					drawPolyline(g, at, d, xMin, xMax, yMin, yMax, wrap, fill);
				}
			} else {
				draw(g);
			}
		}
	}

	public void propertyChange(PropertyChangeEvent evt) {
		updateScale=false;
		map.repaint();
	}
	public String toString(){
		return desc.name;
	}

	public void updateColorScale() {
		if (cst != null) {
			f = new float[tm.displayToDataIndex.size()];
			// some issue with loading sessions means we need to keep checking f is not null
			if (f != null) {
				int len = f.length;
				for (int i = 0; i < len; i++) {
					try {
						f[i] = Float.parseFloat(tm.getValueAt(i, colorColumnIndex).toString());
					} catch (Exception ex) {
						if (f != null) f[i] = Float.NaN;
					}
				}
				cst.setGrid(f);
			}
		}
	}

	public void updateSymbolScale() {
		f2 = new float[tm.displayToDataIndex.size()];
		// some issue with loading sessions means we need to keep checking f2 is not null
		if (f2 != null) {
			int len = f2.length;
			for (int i = 0; i < len; i++) {
				try {
					f2[i] = Float.parseFloat(tm.getValueAt(i, scaleColumnIndex).toString());
				} catch (Exception ex) {
					if (f2 != null) f2[i] = Float.NaN;
				}
			}
			sst.setGrid(f2);
		}
	}

	//standard checks on the data to be exported that apply to all output types
	private boolean exportChecks(String saveOption) {
		if (isEdited()) {
			int numEdited = countEditedRows();
			String changedRowsTxt = (numEdited == 1) ? " row has" : " rows have";
			int c=JOptionPane.showConfirmDialog(MapApp.anchor, "Table has been edited.\n" + 
					numEdited + changedRowsTxt + " been changed.\nConfirm Save.", "Edited Table", JOptionPane.YES_NO_OPTION);
			if (c==JOptionPane.NO_OPTION) return false;
		}
		if (saveOption.equals("selection")) {
			// 1.5.10 Check if anything is selected
			if (dataT.getSelectedRowCount() == 0) {
				JOptionPane.showMessageDialog(MapApp.anchor, "No data selected for export", "No Selection", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		} else if (saveOption.equals("plottable")) {
			//check that there are plottable rows
			if (getPlottableRows().length == 0) {
				JOptionPane.showMessageDialog(MapApp.anchor, "No plotted data available for export", "No Selection", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		return true;
	}
	
	public void exportASCII(String saveOption){
		//run standard checks before exporting
		if (!exportChecks(saveOption)) return;
		
		JFileChooser jfc = new JFileChooser(System.getProperty("user.home"));
		String defaultName = desc.name.replace(":", "").replace(",", "") + ".txt";
		defaultName = defaultName.replace("Data Table ", "").replace(" ", "_");
		File f=new File(defaultName);
		jfc.setSelectedFile(f);
		do {
			int c = jfc.showSaveDialog(null);
			if (c==JFileChooser.CANCEL_OPTION||c==JFileChooser.ERROR_OPTION) return;
			f = jfc.getSelectedFile();
			if (f.exists()) {
				c=JOptionPane.showConfirmDialog(MapApp.anchor, "File Already Exists\nConfirm Overwrite");
				if (c==JOptionPane.OK_OPTION) break;
				if (c==JOptionPane.CANCEL_OPTION) return;
			}
		} while (f.exists());
		
		final File saveTo = f;
		MapApp app = ((MapApp) map.getApp());
		app.addProcessingTask("Saving Data Table...", new Runnable() {
			public void run() {

				try {
					BufferedWriter out = new BufferedWriter(new FileWriter(saveTo));
					//don't include Plot column
					for (int i=1;i<dataT.getColumnCount();i++)
						out.write(dataT.getColumnName(i)+"\t");
					out.write("\n");
					
					int[] ind;
					if (saveOption.equals("selection")) {
						ind = dataT.getSelectedRows();
					} else if (saveOption.equals("plottable")) {
						ind = getPlottableRows();
					} else {
						ind = new int[dataT.getRowCount()];
						for (int i=0; i<dataT.getRowCount(); i++) ind[i] = i;
					}
						
					for (int i=0;i<ind.length;i++) {
						for (int j=1; j<dataT.getColumnCount();j++) {
							Object o = dataT.getValueAt(ind[i], j);
							if (o instanceof String && ((String)o).equals("NaN")) o = "";
							out.write(o+"\t");
						}
						out.write("\n");
					}
					out.close();
					MapApp.sendLogMessage("Saving_or_Downloading&table="+desc.name.replace("Data Table: ", "")+"&saveOption="+saveOption+"&fmt=ascii");
				} catch(Exception ex) {
					JOptionPane.showMessageDialog(map.getTopLevelAncestor(),
							"an error occurred during this operation:\t"
							+ " "+ ex.getMessage());
				}
			}
		});
	}

	// Exports viewable items in the data table to excel format file .xls
	public void exportExcel(String saveOption){
		//run standard checks before exporting
		if (!exportChecks(saveOption)) return;
		
		JFileChooser jfc = new JFileChooser(System.getProperty("user.home"));
		ExcelFileFilter eff = new ExcelFileFilter();
		jfc.setFileFilter(eff);
		String defaultName = desc.name.replace(":", "").replace(",", "") + ".xls";
		defaultName = defaultName.replace("Data Table ", "").replace(" ", "_");
		File f=new File(defaultName);
		jfc.setSelectedFile(f);
		do {
			int c = jfc.showSaveDialog(null);
			if (c==JFileChooser.CANCEL_OPTION||c==JFileChooser.ERROR_OPTION) return;
			f = jfc.getSelectedFile();
			if (f.exists()) {
				c=JOptionPane.showConfirmDialog(MapApp.anchor, "File Already Exists\nConfirm Overwrite");
				if (c==JOptionPane.OK_OPTION) break;
				if (c==JOptionPane.CANCEL_OPTION) return;
			}
		} while (f.exists());

		final File saveTo = f;
		MapApp app = ((MapApp) map.getApp());
		app.addProcessingTask("Saving Data Table...", new Runnable() {
			public void run() {
				try { 
					WritableWorkbook wb = Workbook.createWorkbook(saveTo);
					WritableSheet sheet = wb.createSheet("First Sheet", 0);
					//don't include Plot column
					for (int i=1;i<dataT.getColumnCount();i++)
						sheet.addCell( new Label(i-1,0,dataT.getColumnName(i)) );
					
					int[] ind;
					if (saveOption.equals("selection")) {
						ind = dataT.getSelectedRows();
					} else if (saveOption.equals("plottable")) {
						ind = getPlottableRows();
					} else {
						ind = new int[dataT.getRowCount()];
						for (int i=0; i<dataT.getRowCount(); i++) ind[i] = i;
					}
						
					for (int i=0;i<ind.length;i++) {
						for (int j=1; j<dataT.getColumnCount();j++) {
							Object o = dataT.getValueAt(ind[i], j);
							if (o instanceof String && ((String)o).equals("NaN")) o = "";
							sheet.addCell( new Label(j-1,i+1,o.toString()) );
						}
					}
					wb.write();
					wb.close();
					MapApp.sendLogMessage("Saving_or_Downloading&table="+desc.name.replace("Data Table: ", "")+"&saveOption="+saveOption+"&fmt=excel");
				} catch(Exception ex) {
					JOptionPane.showMessageDialog(map.getTopLevelAncestor(),
							"an error occurred during this operation:\t"
							+ " "+ ex.getMessage());
				}
			}
		});
	}


	// Exports viewable items in the data table to excel format file .xlsx
	public void exportExcelXLSX(String saveOption){
		//run standard checks before exporting
		if (!exportChecks(saveOption)) return;

		JFileChooser jfc = new JFileChooser(System.getProperty("user.home"));
		ExcelFileFilter eff = new ExcelFileFilter();
		jfc.setFileFilter(eff);
		String defaultName = desc.name.replace(":", "").replace(",", "") + ".xlsx";
		defaultName = defaultName.replace("Data Table ", "").replace(" ", "_");
		File f=new File(defaultName);
		jfc.setSelectedFile(f);
		do {
			int c = jfc.showSaveDialog(null);
			if (c==JFileChooser.CANCEL_OPTION||c==JFileChooser.ERROR_OPTION) return;
			f = jfc.getSelectedFile();
			if (f.exists()) {
				c=JOptionPane.showConfirmDialog(MapApp.anchor, "File Already Exists\nConfirm Overwrite");
				if (c==JOptionPane.OK_OPTION)break;
				if (c==JOptionPane.CANCEL_OPTION) return;
			}
		} while (f.exists());
		
		final File saveTo = f;
		MapApp app = ((MapApp) map.getApp());
		app.addProcessingTask("Saving Data Table...", new Runnable() {
			public void run() {
				try {
					if (!saveTo.getName().endsWith(".xlsx")){
						System.out.println(saveTo.getName());
						JOptionPane.showMessageDialog(MapApp.anchor, "Save did not complete. Must end in .xlsx. Try again.");
					}else{
						SXSSFWorkbook xlsxWB = new SXSSFWorkbook();
						SXSSFSheet xlsxSheet1 = xlsxWB.createSheet("First Sheet");
						SXSSFRow row = null;
						
						int[] ind;
						if (saveOption.equals("selection")) {
							ind = dataT.getSelectedRows();
						} else if (saveOption.equals("plottable")) {
							ind = getPlottableRows();
						} else {
							ind = new int[dataT.getRowCount()];
							for (int i=0; i<dataT.getRowCount(); i++) ind[i] = i;
						}
						int xlsxCol = dataT.getColumnCount();
						row = xlsxSheet1.createRow((0));
						//don't include Plot column
						for (int c=1; c<xlsxCol; c++){
							String columnName = dataT.getColumnName(c);
							row.createCell(c-1).setCellValue(columnName);
						}
						
						Object o = null;
						for (int r=1; r<=ind.length; r++){
							row = xlsxSheet1.createRow((r));
							for (int c=1; c<xlsxCol; c++){
								o = dataT.getValueAt(ind[r-1], c);
								if(!(o instanceof String)) continue;
								if (((String)o).equals("NaN")){
									o = "";
								}
								row.createCell(c-1).setCellValue((String)o);
							}
						}
						FileOutputStream xlsxOut = new FileOutputStream(saveTo);
						xlsxWB.write(xlsxOut);
						xlsxOut.close();
						xlsxWB.close();
						MapApp.sendLogMessage("Saving_or_Downloading&table="+desc.name.replace("Data Table: ", "")+"&saveOption="+saveOption+"&fmt=excelxlsx");
					}
				} catch(Exception ex) {
					JOptionPane.showMessageDialog(map.getTopLevelAncestor(),
							"an error occurred during this operation:\t"
							+ " "+ ex.getMessage());
				}
			}
		});

	}

	public void exportKML(String saveOption) {
		//run standard checks before exporting
		if (!exportChecks(saveOption)) return;
		
		final int[] ind;
		if (saveOption.equals("selection")) {
			ind = dataT.getSelectedRows();
		} else if (saveOption.equals("plottable")) {
			ind = getPlottableRows();
		} else {
			ind = new int[dataT.getRowCount()];
			for (int i=0; i<dataT.getRowCount(); i++) ind[i] = i;
		}
		
		KMLExport.exportToKML(this, new KMLExport.DataSelector() {
		
			public Object getValueAt(int row, int col) {
				return tm.getValueAt(ind[row], col);
			}
			public int getRowCount() {
				return ind.length;
			}
		});
		MapApp.sendLogMessage("Saving_or_Downloading&table="+desc.name.replace("Data Table: ", "")+"&saveOption="+saveOption+"&fmt=kml");
	}

	public void bookmark(){
		if (desc.type >-1){
			Vector<DBDescription> bookmarks=new Vector<DBDescription>();
			File rt = GMARoot.getRoot();
			if (rt==null) return;
			String fs = System.getProperty("file.separator");
			File bm = new File(rt.getPath()+fs+"history"+fs+"db.bm");
			if (bm!=null) bookmarks = readVectors(bm);
			
			for (int i = Math.max(bookmarks.size()-10,0); i < bookmarks.size(); i++)
				if (desc.equals((bookmarks.get(i)))){

//					GMA 1.5.2: Corrected misspelling of "already"
//					JOptionPane.showMessageDialog(MapApp.anchor, "Bookmark allready exists", "Bookmark exists", JOptionPane.INFORMATION_MESSAGE);
					JOptionPane.showMessageDialog(MapApp.anchor, "Bookmark already exists", "Bookmark exists", JOptionPane.INFORMATION_MESSAGE);

					return;
				}

			bookmarks.add(desc);
			File hstD = new File(rt.getPath()+fs+"history");
			hstD.mkdir();
			try {
				ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(bm));
				for (int i = 0; i < bookmarks.size(); i++)
					out.writeObject(bookmarks.get(i));
				out.close();
				JOptionPane.showMessageDialog(MapApp.anchor, "Bookmark created.", "Succesful", JOptionPane.INFORMATION_MESSAGE);
			} catch (IOException e) {JOptionPane.showMessageDialog(MapApp.anchor, "Error Writing Bookmark:\n"+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);}
		}
	}

	public Vector<DBDescription> readVectors(File f){
		Vector<DBDescription> v = new Vector<DBDescription>();
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
			Object o;
			while ((o=in.readObject())!=null) v.add((DBDescription) o);
			in.close();
		} catch (Exception ex) { }
		return v;
	}

	public void drawSelectionBox(){
		synchronized (map.getTreeLock()) {
			Graphics2D g = (Graphics2D) map.getGraphics(); 
			g.setXORMode(Color.GRAY);
			int x = Math.min(p1.x, p2.x);
			int y = Math.min(p1.y, p2.y);
			int w = Math.max(p1.x, p2.x) - x;
			int h = Math.max(p1.y, p2.y) - y;
			r = new Rectangle(x,y,w,h);
			g.draw(r);
		}
	}

	public void drawLasso(){
		synchronized (map.getTreeLock()) {
			Graphics2D g = (Graphics2D) map.getGraphics(); 
			g.setXORMode(Color.GRAY);
			int x1 = poly.xpoints[poly.npoints-2];
			int y1 = poly.ypoints[poly.npoints-2];
			int x2 = poly.xpoints[poly.npoints-1];
			int y2 = poly.ypoints[poly.npoints-1];
			//System.out.println(x1+"\t"+y1+"\t"+x2+"\t"+y2);
			g.drawLine(x1, y1, x2, y2);
		}
	}

	public void unDrawLasso(){
		synchronized (map.getTreeLock()) {
			Graphics2D g = (Graphics2D) map.getGraphics(); 
			g.setXORMode(Color.GRAY);
			for(int i=1;i<poly.npoints;i++) {
				g.drawLine(poly.xpoints[i-1], poly.ypoints[i-1], poly.xpoints[i], poly.ypoints[i]);
			}
		}
	}

	public void selectBox() {
		Point2D p = map.getScaledPoint(new Point(r.x,r.y));
		Rectangle2D.Double r = new Rectangle2D.Double(p.getX(),p.getY(),
				this.r.width/map.getZoom(),this.r.height/map.getZoom()); 

		Rectangle2D rect = map.getClipRect2D();
		float yMin = (float)rect.getY();
		float yMax = (float)(rect.getY() + rect.getHeight());
		float xMin = (float)rect.getX();
		float xMax = (float)(rect.getX() + rect.getWidth());
		float wrap = (float)map.getWrap();
		//System.out.println(xMin+"\t"+xMax+"\t"+yMin+"\t"+yMax);

		dataT.getSelectionModel().setValueIsAdjusting(true);
		if (station){
			for( int k=0 ; k<tm.displayToDataIndex.size() ; k++) {
				int z = tm.displayToDataIndex.get(k);
				UnknownData d = data.get(z);

				if (f!=null&&k<f.length&&Float.isNaN(f[k])) continue;
				if (f2!=null&&k<f2.length&&Float.isNaN(f2[k])) continue;

				//Symbol s = new Symbol(Symbol.CIRCLE, (float) (3./zoom), b, a);

				float x = d.x;
				float y = d.y;
				//System.out.println(d+"\t"+yMin+"\t"+yMax);
				if( y<yMin || y>yMax ) continue;
				if (Float.isNaN(d.x) || Float.isNaN(d.y)) continue;
				if( wrap>0f ) {
					while( x>xMin+wrap ) x -= wrap;
					while( x<xMin ) x += wrap;
					while( x<xMax ) {
						if (r.contains(x, y))
							dataT.getSelectionModel().addSelectionInterval(k, k);
						x += wrap;
					}
				} else {
					if( x>xMin && x<xMax ) {
						if (r.contains(x, y))
							dataT.getSelectionModel().addSelectionInterval(k, k);
					}
				}
			}
		}
		dataT.getSelectionModel().setValueIsAdjusting(false);

//		selectionChangedRedraw(os);
		if (dataT.getSelectedRow() != -1)
			dataT.ensureIndexIsVisible(dataT.getSelectedRow());
	}

	public void selectLasso() {
		GeneralPath path = new GeneralPath();

		for (int i=0; i<poly.npoints; i++){
			Point2D point = map.getScaledPoint(new Point(poly.xpoints[i],poly.ypoints[i]));
			if (i==0) path.moveTo((float)point.getX(), (float) point.getY());
			else path.lineTo((float)point.getX(), (float) point.getY());
		}
		path.closePath();
		r=path.getBounds();

//		Point2D p = map.getScaledPoint(new Point(r.x,r.y));
//		Rectangle2D.Double r = new Rectangle2D.Double(p.getX(),p.getY(),
//				this.r.width/map.getZoom(),this.r.height/map.getZoom()); 

		Rectangle2D rect = map.getClipRect2D();
		float yMin = (float)rect.getY();
		float yMax = (float)(rect.getY() + rect.getHeight());
		float xMin = (float)rect.getX();
		float xMax = (float)(rect.getX() + rect.getWidth());
		float wrap = (float)map.getWrap();
		//System.out.println(xMin+"\t"+xMax+"\t"+yMin+"\t"+yMax);

		dataT.getSelectionModel().setValueIsAdjusting(true);

		if (station){
			for( int k=0 ; k<tm.displayToDataIndex.size() ; k++) {
				int z = tm.displayToDataIndex.get(k);
				UnknownData d = data.get(z);

				if (f!=null&&k<f.length&&Float.isNaN(f[k])) continue;
				if (f2!=null&&k<f2.length&&Float.isNaN(f2[k])) continue;
				if (dataT.isRowSelected(k)){ continue; }
				//Symbol s = new Symbol(Symbol.CIRCLE, (float) (3./zoom), b, a);

				float x = d.x;
				float y = d.y;
				//System.out.println(d+"\t"+yMin+"\t"+yMax);
				if( y<yMin || y>yMax ) continue;
				if (Float.isNaN(d.x) || Float.isNaN(d.y)) continue;
				if( wrap>0f ) {
					while( x>xMin+wrap ) x -= wrap;
					while( x<xMin ) x += wrap;
					while( x<xMax ) {
						if (r.contains(x, y)&&path.contains(x, y))
							dataT.getSelectionModel().addSelectionInterval(k, k);
						x += wrap;
					}
				} else {
					if( x>xMin && x<xMax ) {
						if (r.contains(x, y)&&path.contains(x, y))
							dataT.getSelectionModel().addSelectionInterval(k, k);
					}
				}
			}
		}

		dataT.getSelectionModel().setValueIsAdjusting(false);

		unDrawLasso();
//		selectionChangedRedraw(os);
		if (dataT.getSelectedRow() != -1)
			dataT.ensureIndexIsVisible(dataT.getSelectedRow());
	}

	public void match() {
		int row = dataT.getSelectionModel().getLeadSelectionIndex();
		int col = dataT.getSelectedColumn();

		if (row == -1 || col == -1) return;

		dataT.getSelectionModel().setValueIsAdjusting(true);
		dataT.getSelectionModel().clearSelection();

		if (tm.getColumnClass(col) == String.class) {
			String searchValue = (String) tm.getValueAt(row, col);
			for (int i = 0; i < tm.getRowCount(); i++)
				if (tm.getValueAt(i, col).equals(searchValue))
					dataT.getSelectionModel().addSelectionInterval(i, i);
		}
		if (dataT.getSelectedRow() != -1)
			dataT.ensureIndexIsVisible(dataT.getSelectedRow());

		dataT.getSelectionModel().setValueIsAdjusting(false);
	}

	public Color getColor() {
		return color;
	}
	
	public void setColor(Color c) {
		color = c;
	}

	public String getSymbolShape() {
		return station ? shapeString : lineStyleString;
	}
		
	public int getDataSize() {
		return totalSize;
	}
	public void updateTotalDataSize(int numSize, int numTotal) {
		totalSize = numTotal;
		displaySize = numSize;
		if (station)
			db.pointsLabel.setText("<html>" + Integer.toString(numSize) + " of " + Integer.toString(totalSize) + "</html>");
		else
			db.pointsLabel.setText("<html>N/A</html>");
	}

	private static final class XBTableExtension extends XBTable {
		final static Color HIDE_BG_COLOR = new Color(230, 230, 230);
		final static Color HIDE_FG_COLOR = new Color(100, 100, 100);
		
		private XBTableExtension(TableModel model) {
			super(model);
		}
		
		public javax.swing.table.TableCellRenderer getCellRenderer(int row,
				int column) {
			//if a column class is String, use the special HyperlinkTableRenderer
			//which will create hyperlinks where appropriate.
			if (dataModel.getColumnClass(column) == String.class) {
				return new HyperlinkTableRenderer();
			} 
			//Otherwise, use the parent renderer - needed for turning Boolean 
			//columns in to checkboxes
			return super.getCellRenderer(row, column);
		}
		
		@Override
		public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
			Component c = super.prepareRenderer(renderer, row, column);
			//set row color based on whether Plot checkbox is ticked or not
			Boolean show = (boolean) getValueAt(row, getPlotColumnIndex());
			if (!show) {
				c.setBackground(HIDE_BG_COLOR);
				c.setForeground(HIDE_FG_COLOR);
				c.setFont(c.getFont().deriveFont(Font.ITALIC));
			} 

			return c;		
		}
	}
	
	public class UnknownDataSceneEntry implements SceneGraphEntry {
		private int index;
		private UnknownData ud;
		public UnknownDataSceneEntry(int index)
		{
			this.index = index;
			this.ud = data.get(index);
		}

		public int getID() {
			return index;
		}

		public double getX() {
			return ud.x;
		}

		public double getY() {
			return ud.y;
		}

		public UnknownData getData() {
			return ud;
		}
		
		public boolean isVisible() {
			Integer displayIndex = tm.rowToDisplayIndex.get(index);

			if (displayIndex == null) return false;

			if (f!=null &&
					displayIndex < f.length && 
					Float.isNaN(f[displayIndex])) return false;

			if (f2!=null &&
					displayIndex < f2.length &&
					Float.isNaN(f2[displayIndex])) return false;

			return true;
		}

	}

	public void setInfoURL(String infoURL) {
		this.infoURL = infoURL;
	}

	public String getInfoURL() {
		return infoURL;
	}

	public double[] getWESN() {
		if (wesn==null) return null;
		return new double[] {wesn[0], wesn[1], wesn[2], wesn[3]};
	}

	public void tableClicked(int row, int col) {
		if (tm.getColumnClass(col) == String.class) {
			String str = (String) dataT.getValueAt(row, col);
				if (col == polylineIndex) {
					int z = tm.displayToDataIndex.get(row);
					if (!polylines.contains(z)) {
						polylines.add(z);
						db.repaintMap();
					}
				} else {
					BrowseURL.browseURL(str, false);
				}
			} else {
				db.repaintMap();
			}
	}
	
	//return true if there is a tick in the Plot Column
	public Boolean isPlottable(int row) {
		//find plot column
		int plotColumn = dataT.getPlotColumnIndex();
		if (plotColumn == -1) return true;
		
		int plotRow = tm.displayToDataIndex.get(row);
		//get the PLOT value for the row
		return (Boolean) data.get(plotRow).data.get(plotColumn);

	}
	
	//return true if user is allowed to edit dataset.
	//Only editable if dataset was imported
	public Boolean isEditable() {
		//use the xml_menu parameter to determine whether the dataset was imported
		//or loaded from the menu
		if (this.xml_menu == null) return true;
		return false;
	}
	
	//make all rows plottable
	public void makeAllPlottable() {
		//find plot column
		int plotColumn = dataT.getPlotColumnIndex();
		for (UnknownData d : data) {
			d.data.set(plotColumn, true);
		}
	}
	
	//are all rows plottable?
	public boolean areAllPlottable() {
		if (dataT == null) return false;
		//find plot column
		int plotColumn = dataT.getPlotColumnIndex();
		for (UnknownData d : data) {
			if (!((boolean) d.data.get(plotColumn)) ) return false;
		}
		return true;
	}
	
	//store the plottable status for each row
	public void rememberPlottableStatus() {
		int plotColumn = dataT.getPlotColumnIndex();
		oldPlottableStatus = new ArrayList<Boolean>();
		for (UnknownData d : data) {
			oldPlottableStatus.add((boolean) d.data.get(plotColumn));
		}
	}
	
	//revert to each row to it's previous plottable status
	public void revertToOldPlottableStatus() {
		if (oldPlottableStatus == null) return;
		//find plot column
		int plotColumn = dataT.getPlotColumnIndex();
		for (int i=0; i<data.size(); i++) {
			UnknownData d = data.get(i);
			d.data.set(plotColumn, oldPlottableStatus.get(i));
		}
	}
	
	//return a list of plottable rows
	public int[] getPlottableRows() {
		//find plot column
		int plotColumn = dataT.getPlotColumnIndex();
		ArrayList<Integer> plottableRows = new ArrayList<Integer>();
		for (int i=0; i<tm.displayToDataIndex.size(); i++) {
			UnknownData d = data.get(tm.displayToDataIndex.get(i));	
			if ((boolean) d.data.get(plotColumn)) plottableRows.add(i);
		}
		return GeneralUtils.arrayList2ints(plottableRows);
	}
	
	//return whether the table has been edited
	public boolean isEdited() {
		return countEditedRows() != 0;
	}
	
	//return number of edited rows
	public int countEditedRows() {
		int c = 0;
		int plotColumn = dataT.getPlotColumnIndex();
		for (int i=0; i<rowData.size(); i++) {
			Vector<Object> thisRow = (Vector<Object>)rowData.get(i).clone();
			Vector<Object> origRow = (Vector<Object>)origData.get(i).clone();
			//exclude the plotColumn
			thisRow.remove(plotColumn);
			origRow.remove(plotColumn);
			if (!thisRow.equals(origRow)) c++;
		}
		return c;
	}
	
	public void setScaleColumnIndex( int index ) {		
		scaleColumnIndex = index;
	}
	
	public int getScaleColumnIndex() {
		return scaleColumnIndex;
	}
	
	public void setScaleNumericalColumnIndex( int index ) {		
		scaleNumericalColumnIndex = index;
	}
	
	public int getScaleNumericalColumnIndex() {
		return scaleNumericalColumnIndex;
	}
	
	public void setColorColumnIndex( int index ) {		
		colorColumnIndex = index;
	}
	
	public int getColorColumnIndex() {
		return colorColumnIndex;
	}
	
	public void setColorNumericalColumnIndex( int index ) {		
		colorNumericalColumnIndex = index;
	}
	
	public int getColorNumericalColumnIndex() {
		return colorNumericalColumnIndex;
	}
	
	public void addData(UnknownData d) {
		if (d == null) return;
		data.add(d);
		initData();
		initTable();
	}
	
	public void removeData(int row) {
		if (row == -1) return;
		data.remove(row);
		initData();
		initTable();
	}

	public void setSymbolShape(String shape) {
		shapeString = shape;	
		//update the layerPanel
		if (layerPanel != null)
			layerPanel.setSymbolShape(station ? shapeString : lineStyleString, color);
		
	}
	
	public void setLayerPanel(LayerPanel layerPanel) {
		this.layerPanel = layerPanel;
	}
}
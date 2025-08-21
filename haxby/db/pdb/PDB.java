package haxby.db.pdb;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.geomapapp.image.ColorScaleTool;
import org.geomapapp.util.Cursors;
import org.geomapapp.util.Icons;

import haxby.db.Database;
import haxby.db.XYGraph;
import haxby.db.custom.ExcelFileFilter;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.proj.Projection;
import haxby.util.PathUtil;
import haxby.util.URLFactory;
import haxby.util.XBTable;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

public class PDB implements Database,
			MouseListener,
			MouseMotionListener,
			KeyListener,
			ListSelectionListener,
			TableModelListener,
			PropertyChangeListener {

	protected static String[] saveOptions = { "Save Data",
									"Copy Selection to Clipboard",
									"Save Table as ASCII Table",
									"Save Table as Excel File",
									"Save Selection as ASCII Table",
									"Save Selection as Excel File" };

	protected JComboBox save;
	protected XMap map;
	protected double wrap;
	protected XBTable table;
	protected XBTable aTable;
	protected XBTable sTable;
	protected PDBStationModel model;
	protected PDBSampleModel sModel;
	protected PDBAnalysisModel aModel;
	protected int[] selectedIndices;
	protected int current;
	protected JFileChooser chooser;
	protected boolean loaded = false;
	protected boolean enabled = false;
	protected JPanel dialog; /* Contain three top right panels */
	//PDBSelectionDialog dialog = null;
	protected JTabbedPane dataDisplay = null;

	public Point p1,p2; // JOC Used for box select
	protected Rectangle r; // Box Select
	protected Polygon poly; // JOC: Used for lassoing

	// JOC Adding Color Scale Tool
	protected TableModel colorTable;
	protected int colorColumnIndex;
	protected float[] colorGrid;
	protected ColorScaleTool cst = null;
	protected float colorFocusTime = -1;

	// JOC List of graphs
	protected List graphs = new LinkedList(); // XYGraph

//	***** GMA 1.6.2: The lasso toggle button cannot be just a local variable if it is used in the
//	locally defined action listeners for the zoom buttons in the main toolbar.
	protected JToggleButton lassoTB;
//	***** GMA 1.6.2

	protected MouseListener stationSorter = new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
			int c = table.getTableHeader().columnAtPoint(e.getPoint());
			model.sortByColumn(c);
			table.updateUI();
		}
	};

	protected MouseListener analysisSorter = new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
			int c = aTable.getTableHeader().columnAtPoint(e.getPoint());
			aModel.sortByColumn(c);
			aTable.updateUI();
		}
	};

	protected MouseListener sampleSorter = new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
			int c = sTable.getTableHeader().columnAtPoint(e.getPoint());
			sModel.sortByColumn(c);
			sTable.updateUI();
		}
	};

	public PDB(XMap map) {
		this.map = map;
		wrap = map.getWrap();
		current = -1;
		chooser = new JFileChooser(System.getProperty("user.home"));
	}

	public void setEnabled( boolean tf ) {
		if( tf && enabled ) return;
		if( tf ) {
			map.addMouseListener( this );
			map.addMouseMotionListener( this );
			map.setBaseCursor(Cursor.getDefaultCursor());
		} else {
			map.removeMouseListener( this );
			map.removeMouseMotionListener( this );
		}
		enabled = tf;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public JComponent getSelectionDialog() {
		return dialog;
	}
	public JComponent getDataDisplay() {
		return dataDisplay;
	}
	public PDBStationModel getModel() {
		return model;
	}
	public PDBSampleModel getCompiledModel() {
		return sModel;
	}
	public PDBAnalysisModel getAnalysisModel() {
		return aModel;
	}
	protected void initTable() {
		// Stations Excel Table Tab
		model = new PDBStationModel(map, this, wrap);
		table = new XBTable(model);
		table.getSelectionModel().addListSelectionListener(this);
		table.setFont(new Font("SansSerif", Font.PLAIN, 10));

		// Analyses Table Tab
		aModel = new PDBAnalysisModel(this);
		aModel.search();
		aTable = new XBTable(aModel);
		aTable.getSelectionModel().addListSelectionListener(this);
		aTable.setScrollableTracksViewportWidth( false );
		aTable.setFont(new Font("SansSerif", Font.PLAIN, 10));
		aTable.addKeyListener( this );

		// Compiled Chem Table Tab
		sModel = new PDBSampleModel(this);
		sModel.search();
		sTable = new XBTable(sModel);
		sTable.setScrollableTracksViewportWidth( false );
		sTable.setFont(new Font("SansSerif", Font.PLAIN, 10));
		sTable.getSelectionModel().addListSelectionListener(this);
		sTable.addKeyListener( this );

		// JOC: Adding a listener to the table header to enable sorting
		table.getTableHeader().addMouseListener(stationSorter);
		aTable.getTableHeader().addMouseListener(analysisSorter);
		sTable.getTableHeader().addMouseListener(sampleSorter);
	}
	// Get different tab items
	public XBTable getTable() {
		return table;
	}
	public XBTable getAnalysisTable() {
		return aTable;
	}
	public XBTable getCompiledTable() {
		return sTable;
	}

	public void drawLasso(){
		synchronized (map.getTreeLock()) {
			Graphics2D g = (Graphics2D) map.getGraphics();
			g.setXORMode(Color.GRAY);
			int x1 = poly.xpoints[poly.npoints-2];
			int y1 = poly.ypoints[poly.npoints-2];
			int x2 = poly.xpoints[poly.npoints-1];
			int y2 = poly.ypoints[poly.npoints-1];
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

	public void tableChanged(TableModelEvent e) {
		if (e.getFirstRow() != -1) return;

		synchronized (this) {
			if (colorFocusTime == -1) {
				colorFocusTime = System.currentTimeMillis() + 1000;
				new Thread("PDBColor Focus Thread") {
					public void run() {
						while (System.currentTimeMillis() < colorFocusTime)
							try {
								Thread.sleep(200);
							} catch (InterruptedException e) {
							}
						colorGrid = new float[colorTable.getRowCount()];
						int z = 0;
						for (int i = 0; i < colorGrid.length; i++)
							try {
								z++;
								colorGrid[i] = Float.parseFloat(colorTable.getValueAt(i, colorColumnIndex).toString());
							} catch (Exception ex) {
								colorGrid[i] = Float.NaN;
							}
						colorFocusTime = -1;
						cst.setGrid(colorGrid);
					}
				}.start();
			} else
				colorFocusTime = System.currentTimeMillis() + 1000;
		}
	}

	/**
	* Define a repaintMap method so that we can override it later
	*/
	public void repaintMap() {
		map.repaint();
	}

	protected void color() {
		Container c = map.getParent();
		while (!(c instanceof Frame)) c=c.getParent();

		// Produce column list
		Object o2[] = new Object[sModel.getColumnCount(true)];
		for (int i = 0; i < o2.length; i++) {
			o2[i] = sModel.getColumnName(i, true);
		}

		JPanel p2 = new JPanel( new BorderLayout());
		p2.add(new JLabel("Choose column to color symbols by:"), BorderLayout.NORTH);

		ButtonGroup bg = new ButtonGroup();
		JPanel p = new JPanel(new GridLayout(0,1)); 
		JRadioButton plotCompiledChem = new JRadioButton("Compiled Chemical");
		JRadioButton rb = new JRadioButton("Analysis");

		String selectedTab = dataDisplay.getTitleAt(dataDisplay.getSelectedIndex());
		if (selectedTab.equals("Analyses")) {
			rb.setSelected(true);
		} else {
			plotCompiledChem.setSelected(true);
		}

		bg.add(plotCompiledChem);
		bg.add(rb);
		p.add(plotCompiledChem);
		p.add(rb);
		p2.add(p);

		Object o = JOptionPane.showInputDialog(c, p2, "Select Column",
					JOptionPane.QUESTION_MESSAGE, null, o2, o2[colorColumnIndex]);
		if (o==null)return;
		for (colorColumnIndex = 0; colorColumnIndex < o2.length; colorColumnIndex++)
			if (o2[colorColumnIndex] == o) break;

		TableModel newModel;
		if (plotCompiledChem.isSelected()) {
			newModel = sModel;
		} else {
			newModel = aModel;
		}

		if (colorTable != newModel) {
			if (colorTable != null)
				colorTable.removeTableModelListener(this);
			newModel.addTableModelListener(this);
		}

		colorTable = newModel;
		colorGrid = new float[colorTable.getRowCount()];

		int z = 0;
		for (int i = 0; i < colorGrid.length; i++)
			try {
				z++;
				colorGrid[i] = Float.parseFloat(colorTable.getValueAt(i, colorColumnIndex).toString());
			} catch (Exception ex) {
				colorGrid[i] = Float.NaN;
			}

		if (z<2) return;

		if (cst == null){
			cst = new ColorScaleTool(colorGrid);
			cst.addPropertyChangeListener(this);
		}
		else cst.setGrid(colorGrid);

//		1.3.5: Assign appropriate name to color scale window and color label
//		cst.setName((String)o + " - " + desc.name);
		cst.setName("Color " + o2[colorColumnIndex] + " in " + 
				(plotCompiledChem.isSelected() ? "Samples" : "Analyses"));
		cst.showDialog((JFrame)c);

		((Window)cst.getTopLevelAncestor()).addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				repaintMap();
			}
		});
		repaintMap();
	}

	public void propertyChange(PropertyChangeEvent evt) {
		repaintMap();
	}

	public void valueChanged(ListSelectionEvent e) {
		 if (e.getValueIsAdjusting()){
			return;
		}

		if(e.getSource()==aTable.getSelectionModel()) {
			analysisSelectionChanged();
		} else if(e.getSource()==sTable.getSelectionModel()) {
			sampleSelectionChanged();
		} else if(e.getSource()==table.getSelectionModel()) {
			stationSelectionChanged();
		}

	}
	private synchronized void stationSelectionChanged() {
		aTable.getSelectionModel().removeListSelectionListener(this);
		aTable.getSelectionModel().removeListSelectionListener(this);
		sTable.getSelectionModel().removeListSelectionListener(this);
		sTable.getSelectionModel().removeListSelectionListener(this);

		select(false);
		selectedIndices = table.getSelectedRows();
		int[] sel = table.getSelectedRows();
		if( selectedIndices.length == 0) {
			current = -1;
			aTable.clearSelection();
			sTable.clearSelection();
			aTable.getSelectionModel().addListSelectionListener(this);
			sTable.getSelectionModel().addListSelectionListener(this);
			return;
		}
		for(int i=0 ; i<selectedIndices.length ; i++) {
			selectedIndices[i] = model.current[selectedIndices[i]];
		}
		select(true);
		if(aModel.analyses.size()<=123138) {
			aTable.getSelectionModel().setValueIsAdjusting(true);
			aTable.clearSelection();

			for(int i=0 ; i<sel.length ; i++) {
				int idx = sel[i];
				long[] samples = PDBStation.stations[model.current[idx]].samples;
				for (int j = 0; j < samples.length; j++) {
					PDBSample sample = PDBSample.sample.get((int)samples[j]);
					try {
						PDBBatch[] batches = sample.batch;
						for (int k = 0; k < batches.length; k++) {
							PDBAnalysis[] analyses = batches[k].analyses;
							for (int l = 0; l < analyses.length; l++) {
								Integer index = ((Integer)aModel.analysisToIndex.get(analyses[l]));
								if (index == null) continue;
								aTable.addRowSelectionInterval(index.intValue(), index.intValue());
							}
						}
					} catch (NullPointerException ne) {}
				}
			}
			int first = aTable.getSelectedRow();
			if(first>=0)aTable.ensureIndexIsVisible(first);
			aTable.getSelectionModel().setValueIsAdjusting(false);
		}
		//if(sModel.samples.size()<=70000) {
		if(sModel.samples.size()<=60000) {
			sTable.getSelectionModel().setValueIsAdjusting(true);
			sTable.clearSelection();

			for(int i=0 ; i<sel.length ; i++) {
				int idx = sel[i];

				long[] samples = PDBStation.stations[model.current[idx]].samples;
				if(samples == null || samples.length == 0)continue;
				for (int j = 0; j < samples.length; j++) {
					Integer index = ((Integer)sModel.sampleToIndex.get(PDBSample.sample.get((int) samples[j])));
					if (index == null) continue;
					sTable.addRowSelectionInterval(index.intValue(), index.intValue());
				}
			}
			int first = sTable.getSelectedRow();
			if(first>=0)sTable.ensureIndexIsVisible(first);
			sTable.getSelectionModel().setValueIsAdjusting(false);
		}
		sTable.getSelectionModel().addListSelectionListener(this);
		aTable.getSelectionModel().addListSelectionListener(this);
	}

	private synchronized void sampleSelectionChanged() {
		if( sModel.samples.size()>70000) return;

		aTable.getSelectionModel().removeListSelectionListener(this);
		aTable.getSelectionModel().removeListSelectionListener(this);
		table.getSelectionModel().removeListSelectionListener(this);
		table.getSelectionModel().removeListSelectionListener(this);
		select(false);
		int[] sel = sTable.getSelectedRows();
		if(sel.length==0) {
			current = -1;
			table.clearSelection();
			aTable.clearSelection();
			table.getSelectionModel().addListSelectionListener(this);
			aTable.getSelectionModel().addListSelectionListener(this);
			return;
		}
		boolean[] ok = new boolean[PDBStation.size()];
		for( int i=0 ; i<ok.length ; i++) ok[i]=false;
		int n=0;
		for( int i=0 ; i<sel.length ; i++) {
			if(sel[i]<0|| sel[i]>= sModel.samples.size() )continue;
			PDBSample a = (PDBSample)sModel.samples.get(sel[i]);
			int k = (int)a.parent;
			if( ok[k] ) continue;
			n++;
			ok[k] = true;
		}
		selectedIndices = new int[n];
		n=0;

//		for( int i=0 ; i<ok.length ; i++) {
//			if(ok[i]) {
//				selectedIndices[n++] = i;
//			}
//		}

		// Select parent station
		table.getSelectionModel().setValueIsAdjusting(true);
		table.clearSelection();
		for( int i=0 ; i<ok.length ; i++) {
			if(ok[i]) {
				Integer currentIndex = ((Integer)model.stationToIndex.get(PDBStation.get(i)));
				if (currentIndex == null) continue;
				selectedIndices[n++] = i;
				table.addRowSelectionInterval(currentIndex.intValue(), currentIndex.intValue());
			}
		}
		table.getSelectionModel().setValueIsAdjusting(false);

		// Select child analyses
		if(aModel.analyses.size()<=123138) {
			aTable.getSelectionModel().setValueIsAdjusting(true);
			aTable.clearSelection();

			for( int i=0 ; i<sel.length ; i++) {
				if(sel[i]<0|| sel[i]>= sModel.samples.size() )continue;

				PDBSample samp = (PDBSample) sModel.samples.get(sel[i]);
				PDBBatch[] batches = samp.batch;
				for (int j = 0; j < batches.length; j++) {
					PDBAnalysis[] analyses = batches[j].analyses;

					for (int k = 0; k < analyses.length; k++) {
						Integer index = ((Integer)aModel.analysisToIndex.get(analyses[k]));
						if (index == null) continue;
						aTable.addRowSelectionInterval(index.intValue(), index.intValue());
					}
				}
			}
			aTable.getSelectionModel().setValueIsAdjusting(false);
		}

		select(true);

		if (table.getSelectedRow() != -1)
			table.ensureIndexIsVisible(table.getSelectedRow());

		if (aTable.getSelectedRow() != -1)
			aTable.ensureIndexIsVisible(aTable.getSelectedRow());

		table.getSelectionModel().addListSelectionListener(this);
		aTable.getSelectionModel().addListSelectionListener(this);
	}

	private synchronized void analysisSelectionChanged() {
		if( aModel.analyses.size()>123138) return;

		table.getSelectionModel().removeListSelectionListener(this);
		table.getSelectionModel().removeListSelectionListener(this);
		sTable.getSelectionModel().removeListSelectionListener(this);
		sTable.getSelectionModel().removeListSelectionListener(this);
		select(false);
		int[] sel = aTable.getSelectedRows();
		if(sel.length==0) {
			current = -1;
			table.clearSelection();
			sTable.clearSelection();
			table.getSelectionModel().addListSelectionListener(this);
			sTable.getSelectionModel().addListSelectionListener(this);
			return;
		}
		boolean[] ok = new boolean[PDBStation.size()];
		for( int i=0 ; i<ok.length ; i++) ok[i]=false;
		int n=0;
		for( int i=0 ; i<sel.length ; i++) {
			PDBAnalysis a = (PDBAnalysis)aModel.analyses.get(sel[i]);
			int k = a.getStationNum();
			if( ok[k] ) continue;
			n++;
			ok[k] = true;
		}
		selectedIndices = new int[n];
		n=0;
//		for( int i=0 ; i<ok.length ; i++) {
//			if(ok[i]) selectedIndices[n++] = i;
//		}

		// Select Parent Station
		table.getSelectionModel().setValueIsAdjusting(true);
		table.clearSelection();
		for( int i=0 ; i<ok.length ; i++) {
			if(ok[i]) {
				Integer currentIndex = ((Integer)model.stationToIndex.get(PDBStation.get(i)));
				if (currentIndex == null) continue;
				selectedIndices[n++] = i;
				table.addRowSelectionInterval(currentIndex.intValue(), currentIndex.intValue());
			}
		}
		if (table.getSelectedRow() != -1)
			table.ensureIndexIsVisible(table.getSelectedRow());
		table.getSelectionModel().setValueIsAdjusting(false);

		select(true);

		// Select Parent Sample
		sTable.getSelectionModel().setValueIsAdjusting(true);
		sTable.clearSelection();
		for (int i = 0; i < sel.length; i++) {
			PDBAnalysis a = (PDBAnalysis)aModel.analyses.get(sel[i]);
			PDBSample samp = a.parent.parent;
			Integer indexI = ((Integer)sModel.sampleToIndex.get(samp));
			if (indexI == null)
				continue;

			int index = indexI.intValue();

			sTable.addRowSelectionInterval(index, index);
		}
		if (sTable.getSelectedRow() != -1)
			sTable.ensureIndexIsVisible(sTable.getSelectedRow());
		sTable.getSelectionModel().setValueIsAdjusting(false);
		table.getSelectionModel().addListSelectionListener(this);
		sTable.getSelectionModel().addListSelectionListener(this);
	}

	public int getClickedIndex(Point2D p) {
		double x0 = p.getX();
		double y0 = p.getY();
		double test0 = 4./map.getZoom();
		int k=current;
		int n = model.current.length;
		for(int i=0 ; i<n  ; i++) {
			k = (k+1)%n;
			PDBStation s = PDBStation.stations[model.current[k]];
			double y = s.getY()-y0;
			if( y<-test0 || y>test0)continue;
			double x = s.getX()-x0;
			if( wrap>0. ) {
				while( x<-test0 ) x+=wrap;
				while( x>test0 ) x-=wrap;
				if( x>-test0 )return k;
			} else {
				if( x>-test0 && x<test0 ) return k;
			}
		}
		return -1;
	}

	public void mouseClicked(MouseEvent e) {
		if(e.isControlDown())return;
		Point2D p = map.getScaledPoint( e.getPoint() );
		int index = getClickedIndex(p);
		if( index==-1 ) {
			if(e.isShiftDown()) return;
			table.clearSelection();
			current = -1;
		} else if(e.isShiftDown()) {
			if( index==current) {
				table.removeRowSelectionInterval(index, index);
				table.getRowHeader().setSelectedIndices(table.getSelectedRows());
				current = -1;
				return;
			}
			current = index;
			table.addRowSelectionInterval(index, index);
			table.getRowHeader().setSelectedIndices(table.getSelectedRows());
			table.ensureIndexIsVisible( index);
			table.getRowHeader().repaint();
		} else {
			current = index;
			table.setRowSelectionInterval(index, index);
			table.getRowHeader().setSelectedIndices(table.getSelectedRows());
			table.ensureIndexIsVisible( index);
			table.getRowHeader().repaint();
		}
		return;
	}
	public void mouseExited(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
		if (e.isControlDown()) return;
		if (e.isConsumed()||!map.isSelectable()) return;

		if (((MapApp) map.getApp()).getMapTools().panB.isSelected())
			return;

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
		if (((MapApp) map.getApp()).getMapTools().panB.isSelected() || !lassoTB.isSelected())
			return;

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
	public void mouseDragged(MouseEvent e) {
		if (((MapApp) map.getApp()).getMapTools().panB.isSelected() || !lassoTB.isSelected())
			return;

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
	public void mouseMoved(MouseEvent e) {}

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

	public void selectBox() {
		table.getSelectionModel().setValueIsAdjusting(true);
		table.clearSelection();

		Point2D p = map.getScaledPoint(new Point(r.x,r.y));
		Rectangle2D.Double r = new Rectangle2D.Double(p.getX(),p.getY(),
				this.r.width/map.getZoom(),this.r.height/map.getZoom());

		Rectangle2D rect = map.getClipRect2D();
		float yMin = (float)rect.getY();
		float yMax = (float)(rect.getY() + rect.getHeight());
		float xMin = (float)rect.getX();
		float xMax = (float)(rect.getX() + rect.getWidth());
		float wrap = (float)map.getWrap();

		for( int k=0 ; k<model.current.length ; k++) {
			PDBStation stat = PDBStation.get(model.current[k]);

			double x = stat.getX();
			double y = stat.getY();

			if( y<yMin || y>yMax ) continue;
			if( wrap>0f ) {
				while( x>xMin+wrap ) x -= wrap;
				while( x<xMin ) x += wrap;
				while( x<xMax ) {
					if (r.contains(x, y))
						table.addRowSelectionInterval(k, k);
					x += wrap;
				}
			} else {
				if( x>xMin && x<xMax ) {
					if (r.contains(x, y))
						table.addRowSelectionInterval(k, k);
				}
			}
		}
		table.getSelectionModel().setValueIsAdjusting(false);

		int selected = table.getSelectedRow();
		if (selected != -1)
			table.ensureIndexIsVisible(selected);

		table.getRowHeader().setSelectedIndices(table.getSelectedRows());
		table.getRowHeader().repaint();
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

		Rectangle2D rect = map.getClipRect2D();
		float yMin = (float)rect.getY();
		float yMax = (float)(rect.getY() + rect.getHeight());
		float xMin = (float)rect.getX();
		float xMax = (float)(rect.getX() + rect.getWidth());
		float wrap = (float)map.getWrap();

		table.getSelectionModel().setValueIsAdjusting(true);
		table.clearSelection();

		for( int k=0 ; k<model.current.length ; k++) {
			PDBStation stat = PDBStation.get(model.current[k]);

			double x = stat.getX();
			double y = stat.getY();
			if( y<yMin || y>yMax ) continue;

			if( wrap>0f ) {
				while( x>xMin+wrap ) x -= wrap;
				while( x<xMin ) x += wrap;
				while( x<xMax ) {
					if (r.contains(x, y)&&path.contains(x, y))
						table.addRowSelectionInterval(k, k);
					x += wrap;
				}
			} else {
				if( x>xMin && x<xMax ) {
					if (r.contains(x, y)&&path.contains(x, y))
						table.addRowSelectionInterval(k, k);
				}
			}
		}
		table.getSelectionModel().setValueIsAdjusting(false);

		int selected = table.getSelectedRow();
		if (selected != -1)
			table.ensureIndexIsVisible(selected);

		unDrawLasso();
		table.getRowHeader().setSelectedIndices(table.getSelectedRows());
		table.getRowHeader().repaint();
	}

	protected synchronized Color computeStationColor(int stationIndex) {
		if (cst == null || !cst.isShowing() || colorGrid.length == 1) return Color.white;

		PDBStation stat = PDBStation.stations[stationIndex];
		if (stat == null) return null;

		if (colorTable instanceof PDBSampleModel) {
			long[] samples = stat.samples;
			if (samples == null || samples.length == 0) {
				return null;
			}
			for (int i = 0; i < samples.length; i++) {
				PDBSample sample = PDBSample.sample.get((int) samples[i]);
				Float value = ((PDBSampleModel)colorTable).getValueAt(sample, colorColumnIndex);
				if (value == null || value.isNaN()) continue;
				return cst.getColor(value.floatValue());
			}
		} else {
			long[] samples = stat.samples;
			if (samples == null || samples.length == 0) return null;
			try{ 
				for (int j = 0; j < samples.length; j++) {
					PDBSample sample = PDBSample.sample.get((int) samples[j]);
					PDBBatch[] batches = sample.batch;
					for (int k = 0; k < batches.length; k++) {
						PDBAnalysis[] analyses = batches[k].analyses;
						for (int i = 0; i < analyses.length; i++) {
							Float value = ((PDBAnalysisModel) colorTable).getValueAt(analyses[i], colorColumnIndex);
							if (value == null || value.isNaN()) continue;
							return cst.getColor(value.floatValue());
						}
					}
				}
			} catch (NullPointerException ne) {
				return null;
			}
		}
		return null;
	}

	public void select(boolean highlight) {
		if( selectedIndices.length==0)return;
		synchronized ( map.getTreeLock() ) {
		Rectangle2D bounds = map.getClipRect2D();
		Graphics2D g = map.getGraphics2D();
		float zoom = 1f / (float) map.getZoom();
		Rectangle2D.Float rect = new Rectangle2D.Float(-3f*zoom, -3f*zoom, 6f*zoom, 6f*zoom);
		Rectangle2D.Float rect1 = new Rectangle2D.Float(-2f*zoom, -2f*zoom, 4f*zoom, 4f*zoom);
		double lastX = 0;
		double lastY = 0;
		double x, y;
		int n = 0;
		g.setStroke( new BasicStroke(zoom) );
		double x1 = bounds.getX();
		double x2 = x1+bounds.getWidth();
		for( int i=0 ; i<selectedIndices.length ; i++) {
			PDBStation stat = PDBStation.stations[selectedIndices[i]];
			x = stat.getX();
			if(wrap>0.)while(x<x1) x+=wrap;
			y = PDBStation.stations[selectedIndices[i]].getY();
			Color a = computeStationColor(selectedIndices[i]);
			if (a == null) continue;
			g.setColor(a);
//			if(highlight)g.setColor( Color.red );
//			else g.setColor( Color.white );
			g.translate( x - lastX, y-lastY );
			g.draw(rect1);
			if (highlight)
				g.setColor(Color.red);
			else
				g.setColor( Color.black );
			g.draw(rect);
			if( wrap>0 && x+wrap < bounds.getX()+bounds.getWidth() ) {
				x += wrap;
				g.setColor( Color.white );
				g.translate( wrap, 0.);
				g.draw(rect1);
				g.setColor( Color.black );
				g.draw(rect);
			}
			lastX = x;
			lastY = y;
		}
		}
	}
	public void draw( Graphics2D g ) {
		float zoom = 1f / (float) map.getZoom();
		Rectangle2D.Float rect = new Rectangle2D.Float(-3f*zoom, -3f*zoom, 6f*zoom, 6f*zoom);
		Rectangle2D.Float rect1 = new Rectangle2D.Float(-2f*zoom, -2f*zoom, 4f*zoom, 4f*zoom);
		Rectangle2D bounds = g.getClipBounds();
		AffineTransform at = g.getTransform();
		g.setColor( Color.black);
		double lastX = 0;
		double lastY = 0;
		double x, y;
		int n = 0;
		g.setStroke( new BasicStroke(zoom) );
		model.setArea( map.getClipRect2D(), map.getZoom() );

		// First Column Corner Text
		table.setCornerText( model.current.length +" Stations");
		aTable.setCornerText( aModel.analyses.size() +" Analyses");
		sTable.setCornerText( sModel.samples.size() +" Samples");

		double x1 = bounds.getX();
		double x2 = x1+bounds.getWidth();
//		double wrap = map.getWrap();
		for( int i=0 ; i<model.toPlot.length ; i++) {
			x = PDBStation.stations[model.toPlot[i]].getX();
			if(wrap>0.)while(x<x1) x+=wrap;
			y = PDBStation.stations[model.toPlot[i]].getY();

			Color a = computeStationColor(model.toPlot[i]);
			if (a == null) continue;

			g.setColor(a);
			g.translate( x - lastX, y-lastY );
			g.draw(rect1);
			g.setColor(Color.black);
			g.draw(rect);
			if( wrap>0 && x+wrap < bounds.getX()+bounds.getWidth() ) {
				x += wrap;
				g.setColor(a);
				g.translate( wrap, 0.);
				g.draw(rect1);
				g.setColor( Color.black );
				g.draw(rect);
			}
			lastX = x;
			lastY = y;
		}
		current = -1;
		g.setTransform( at);
	}
	public boolean isLoaded() {
		return loaded;
	}
	public void unloadDB() {
		loaded = false;
		PDBExpedition.unload();
		PDBLocation.unload();
		PDBStation.unload();
		PDBSample.unload();
		PDBMaterial.unload();
		PDBRockType.unload();
	}
	public void disposeDB() {
		PDBExpedition.unload();
		PDBLocation.unload();
		PDBStation.unload();
		PDBSample.unload();
		PDBMaterial.unload();
		PDBRockType.unload();

		dialog.removeAll();
		dialog = null;

		dataDisplay.setSelectedIndex(-1);
		dataDisplay.removeAll();
		dataDisplay = null;

		table.removeListeners();
		table = null;
		model.dispose();
		model = null;
		aTable = null;
		aModel.dispose();
		aModel = null;
		sTable = null;
		sModel.dispose();
		sModel = null;
		loaded = false;
		if (cst!= null) cst.dispose();
		cst = null;
		for (Iterator iter = graphs.iterator(); iter.hasNext();) {
			XYGraph xyg = (XYGraph) iter.next();
			Container c = xyg.getParent();
			while (!(c instanceof JDialog)) c = c.getParent();
				((JDialog)c).dispose();
		}
		map.removeMouseListener( this );
		map.removeMouseMotionListener( this );
		map.setBaseCursor(Cursor.getDefaultCursor());

		/* Restore the main frame size */
		MapApp app = (MapApp)map.getApp();
		app.setFrameSize( 1000,750 );
	}
	public boolean loadDB() {
		if(loaded) return true;
		try {
	// Load Expeditions
			PDBExpedition.load();
	// Load Material before Station order matters.
			PDBMaterial.load();
	// Load Rock Type
			PDBRockType.load();
	// Load Locations
			Dimension mapDim = map.getDefaultSize();
			Rectangle mapBounds = new Rectangle(0, 0,
					mapDim.width, mapDim.height);
			PDBLocation.load();
			Projection proj = map.getProjection();
			for(int i=0 ; i<PDBLocation.size() ; i++) {
				PDBLocation loc = PDBLocation.get(i);
				if( loc != null ) {
					loc.project(proj);
					if( !mapBounds.contains(loc.getX(), loc.getY()) ) {
						PDBLocation.locations[i]=null;
					}
				}
			}
			PDBDataType.load();		// Load Data Type
			PDBStation.load();		// Load Stations
			PDBSample.load();		// Load Sample
		} catch (IOException ex) {
			loaded = false;
			System.err.println(ex.getMessage());
			ex.printStackTrace();
			return false;
		}

		/* Resize the main frame */
		MapApp app = (MapApp)map.getApp();
		app.setFrameSize( 1165,750 );

		initTable();
		selectedIndices = new int[0];
	//	dialog = new PDBSelectionDialog( this );
		dialog = new JPanel(new GridBagLayout());
		GridBagConstraints dialogConstraints = new GridBagConstraints(
				0,
				0,
				GridBagConstraints.REMAINDER,
				1,
				1.0,
				0.0,
				GridBagConstraints.PAGE_START,
				GridBagConstraints.HORIZONTAL,
				new Insets(0,0,0,0),
				0, 0
				);

		//Set a min size width and height
		dialog.setMinimumSize(new Dimension(438, 100));
		dialog.setPreferredSize(new Dimension(438, 200));
		dialog.setMaximumSize(new Dimension(438, 500));
		
		GridBagLayout layout = new GridBagLayout();
		JPanel p = new JPanel();
		//p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBorder(BorderFactory.createLineBorder(Color.red));
		Dimension pSize = new Dimension(dialog.getSize().width, 100);
//		p.setSize(pSize);
		p.setPreferredSize(pSize);
		p.setMinimumSize(pSize);
//		p.setMaximumSize(new Dimension(pSize.width, pSize.height*3));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.FIRST_LINE_START;

		
		try {
			String updateURL = PathUtil.getPath("PORTALS/PETDB_PATH")  + "last_update_date.txt";
			URL url = URLFactory.url(updateURL);
			BufferedReader in = new BufferedReader(new InputStreamReader( url.openStream() ));

			//if(in.ready()) {
				// Get Dataset Information
				String updateDate = in.readLine();
				String dateText = "<html>PetDB Portal Content Last Updated: " + updateDate + "</html>";
				JLabel textDate = new JLabel(dateText, SwingConstants.CENTER);
				textDate.setFont( new Font( "SansSerif", Font.PLAIN, 13));
				textDate.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
				p.add(textDate);
				//textDate.setMinimumSize(new Dimension(100, pSize.width));
			//}
			in.close();
		} catch(Exception e) {
			System.out.println("PetDB update date file not found");
		}
		
		
		
		//Group Graph, Color, Lasso Data Options together
		layout = new GridBagLayout();
		JPanel p2 = new JPanel(layout);
		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.FIRST_LINE_START;
		gbc.gridwidth = GridBagConstraints.RELATIVE;
		gbc.gridheight = GridBagConstraints.REMAINDER;

		JButton graph = new JButton("Graph Data");
		final PDB pdb = this;
		graph.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new PDBGraphDialog((JFrame)map.getTopLevelAncestor(),pdb);
			}
		});
		//layout.setConstraints(graph, gbc);
		p2.add(graph);
		JButton color = new JButton("Color Data");
		color.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pdb.color();
			}
		});
		//layout.setConstraints(color, gbc);
		p2.add(color);
		p2.setBorder(BorderFactory.createLineBorder(Color.blue));
		JPanel lassoPanel = createLassoPanel();
		//layout.setConstraints(lassoPanel, gbc);
		p2.add(lassoPanel);
		p.add(p2);
		//p.setSize(pSize);

		// Save Combo Box
		save = new JComboBox(saveOptions);
		save.setSelectedIndex(0);
		save.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				switch (save.getSelectedIndex()) {
				case 1: // Copy
					copyToClipboard(getSelectionData());
					break;
				case 2: // Save ASCII
					saveAsASCII("all");
					break;
				case 3: // Save xls
					saveAsExcel("all");
					break;
				case 4: // Save Selected ASCII
					saveAsASCII("selection");
					break;
				case 5: // Save Selected Excel
					saveAsExcel("selection");
					break;
				default:
					break;
				}
				save.setSelectedIndex(0);
			}
		});

		p.add(save);
		dataDisplay = new JTabbedPane(JTabbedPane.TOP);
		JScrollPane sp1 = new JScrollPane(getTable());
		dataDisplay.addTab("Stations", sp1);
		JScrollPane sp2 = new JScrollPane(getCompiledTable());
		dataDisplay.addTab("Compiled Chem",null, sp2, "Lists the compiled " + 
				"geochemical analyses for all samples associated with the displayed stations.");
		JScrollPane sp3 = new JScrollPane(getAnalysisTable());
		dataDisplay.addTab("Analyses",null, sp3, "Lists the individual geochemical " + 
				"analyses for each sample associated with the displayed stations.");
		dialog.setBorder(BorderFactory.createLineBorder(Color.green));
		dialog.add( p, dialogConstraints );
		dialogConstraints.gridy = dialogConstraints.gridy + 1;
		dialogConstraints.weighty = 0;
		//dialogConstraints.fill = GridBagConstraints.BOTH;
		//dialogConstraints.weighty = 1;
		PDBSelectionDialog pdbsd = new PDBSelectionDialog(this);
		pdbsd.setPreferredSize(new Dimension(450, 330));
		pdbsd.setMinimumSize(pdbsd.getPreferredSize());
		pdbsd.setMaximumSize(pdbsd.getPreferredSize());
		pdbsd.setSize(pdbsd.getPreferredSize());
		pdbsd.setBorder(BorderFactory.createLineBorder(Color.MAGENTA));
		dialog.add( pdbsd, dialogConstraints );
		dialogConstraints.gridy = GridBagConstraints.RELATIVE;
		dialogConstraints.anchor = GridBagConstraints.PAGE_START;
		dialogConstraints.weighty = 0.01;
		//dialogConstraints.fill = GridBagConstraints.HORIZONTAL;
		//dialogConstraints.weighty = 0;
		//Group Graph, Color, Lasso Data Options together
		JPanel p2a = new JPanel(new GridBagLayout());
		SendToPetDB stpd = new SendToPetDB(dataDisplay);
		p2a.add( stpd, dialogConstraints );
		p2a.setBorder(BorderFactory.createLineBorder(Color.orange));
		layout.setConstraints(stpd, gbc);
		p2a.setSize(new Dimension(p2a.getWidth(), 50));
		dialog.add(p2a, dialogConstraints);
		dialog.setPreferredSize(new Dimension(438, pdbsd.getSize().height + pSize.height + p.getSize().height));
		dialog.setMaximumSize(dialog.getPreferredSize());
		dialog.setSize(dialog.getPreferredSize());
		dialog.revalidate();
		dialog.updateUI();
		loaded = true;
		return true;
	}

	protected JPanel createLassoPanel() {
		// Lasso Button
		JPanel p2 = new JPanel(new BorderLayout());
		JToggleButton tb = new JToggleButton(Icons.getIcon(Icons.LASSO, false));
		tb.setSelectedIcon(Icons.getIcon(Icons.LASSO, true));
//		tb.setSelected(true);
		tb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
//				((AbstractButton)e.getSource()).setSelected(true);
				if (((AbstractButton)e.getSource()).isSelected()) {
//					***** GMA 1.6.2: Deselect the zoom buttons in the main toolbar if the lasso button is selected
					if ( ((haxby.map.MapApp)map.getApp()).getZoomer().getZoomOut().isSelected() ) {
						((haxby.map.MapApp)map.getApp()).getZoomer().getZoomOut().doClick();
					}
					else if ( ((haxby.map.MapApp)map.getApp()).getZoomer().getZoomIn().isSelected() ) {
						((haxby.map.MapApp)map.getApp()).getZoomer().getZoomIn().doClick();
					}
					else if ( ((MapApp) map.getApp()).getMapTools().panB.isSelected() )
						((MapApp)map.getApp()).getMapTools().panB.doClick();
//					***** GMA 1.6.2

					lassoTB.setSelected(true);
					map.setBaseCursor(Cursors.getCursor(Cursors.LASSO));
				} else
					map.setBaseCursor(Cursor.getDefaultCursor());
			}
		});

		tb.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 0));
		p2.add(tb, BorderLayout.WEST);
		p2.setBorder(null);
		JLabel l = new JLabel("Lasso Data");
		l.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
		p2.add(l);

//		***** GMA 1.6.2: Deselect the lasso button if the zoom buttons in the main toolbar are 
//		selected
		lassoTB = tb;
		((haxby.map.MapApp)map.getApp()).getZoomer().getZoomOut().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
//				((AbstractButton)e.getSource()).setSelected(true);
				if ( lassoTB.isSelected() ) {
					lassoTB.doClick();
				}
			}
		});
		((haxby.map.MapApp)map.getApp()).getZoomer().getZoomIn().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
//				((AbstractButton)e.getSource()).setSelected(true);
				if ( lassoTB.isSelected() ) {
					lassoTB.doClick();
				}
			}
		});
		((haxby.map.MapApp)map.getApp()).getMapTools().panB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
//				((AbstractButton)e.getSource()).setSelected(true);
				if ( lassoTB.isSelected() ) {
					lassoTB.doClick();
				}
			}
		});
//		***** GMA 1.6.2
		return p2;
	}

	public String getDBName() {
		return "PetDB: Petrological Database Bedrock Chemistry";
	}

	public String getCommand() {
		return "petdb_cmd";
	}

	public String getDescription() {
		return "RIDGE Petrology Database";
	}
	public void keyPressed( KeyEvent evt ) {}

	public void keyTyped( KeyEvent evt ) {}

	public void keyReleased( KeyEvent evt ) {
		if( evt.getSource() == sTable) {
			if( !evt.isControlDown() ) return;
			if( evt.getKeyCode() == KeyEvent.VK_A ) {
				sTable.getSelectionModel().setSelectionInterval(
						0, sModel.samples.size()-1);
			} else if( evt.getKeyCode() == KeyEvent.VK_C || evt.getKeyCode() == KeyEvent.VK_X ) {
				int[] sel = sTable.getSelectedRows();
				int count = sel.length * (3+sModel.getColumnCount());
				if(count > 10000) {
					JOptionPane.showMessageDialog( null,
						"copy to clipboard limited to 10000 elements\n"
						+ count +" elements selected\n");
					return;
				}
				StringBuffer sb = new StringBuffer();
				sb.append("ID\tLon.\tLat.");
				for(int i=0 ; i<sModel.getColumnCount() ; i++) {
					sb.append("\t"+sModel.getColumnName(i));
				}
				sb.append("\n");
				for(int k=0 ; k<sel.length ; k++) {
					int i = sel[k];
					sb.append( sModel.getRowName(i) );
					sb.append("\t");
					int stn = ((PDBSample)sModel.samples.get(i)).getStationNum();
					PDBLocation loc = (PDBStation.get(stn)).getLocation();
					sb.append( new Float(loc.lon) );
					sb.append("\t");
					sb.append( new Float(loc.lat) );
					for( int j=0 ; j<sModel.getColumnCount() ; j++) {
						sb.append("\t");
						Object v = sModel.getValueAt(i, j);
						if(v!=null)sb.append( v);
					}
					if(i!=sModel.getRowCount()-1) sb.append("\n");
				}
				JTextArea ta = new JTextArea( sb.toString() );
				ta.selectAll();
				ta.cut();
			} else if( evt.getKeyCode() == KeyEvent.VK_S ) {
				int[] sel = sTable.getSelectedRows();
				int ok = chooser.showSaveDialog(null);
				if( ok == chooser.CANCEL_OPTION ) return;
				try {
					PrintStream out = new PrintStream(
						new FileOutputStream(chooser.getSelectedFile()));
					StringBuffer sb = new StringBuffer();
					sb.append("\"ID\",\"Lon.\",\"Lat.");
					for(int i=0 ; i<sModel.getColumnCount() ; i++) {
						sb.append("\",\""+sModel.getColumnName(i));
					}
					out.println(sb+"\",\r");
					for(int k=0 ; k<sel.length ; k++) {
						int i = sel[k];
						sb = new StringBuffer();
						sb.append( "\""+sModel.getRowName(i) );
						sb.append("\",\"");
						int stn = ((PDBSample)sModel.samples.get(i)).getStationNum();
						PDBLocation loc = (PDBStation.get(stn)).getLocation();
						sb.append( new Float(loc.lon) );
						sb.append("\",\"");
						sb.append( new Float(loc.lat) );
						for( int j=0 ; j<sModel.getColumnCount() ; j++) {
							sb.append("\",\"");
							Object v = sModel.getValueAt(i, j);
							if(v!=null)sb.append( v);
						}
						out.println(sb+"\",\r");
					}
/*
					sb.append("ID\tLon.\tLat.");
					for(int i=0 ; i<sModel.getColumnCount() ; i++) {
						sb.append("\t"+sModel.getColumnName(i));
					}
					out.println(sb);
					for(int k=0 ; k<sel.length ; k++) {
						int i = sel[k];
						sb = new StringBuffer();
						sb.append( sModel.getRowName(i) );
						sb.append("\t");
						int stn = ((PDBSample)sModel.samples.get(i)).getStationNum();
						PDBLocation loc = (PDBStation.get(stn)).getLocation();
						sb.append( new Float(loc.lon) );
						sb.append("\t");
						sb.append( new Float(loc.lat) );
						for( int j=0 ; j<sModel.getColumnCount() ; j++) {
							sb.append("\t");
							Object v = sModel.getValueAt(i, j);
							if(v!=null)sb.append( v);
						}
						out.println(sb+"\r");
					}
*/
					out.flush();
					out.close();
				} catch(IOException ex) {
					ex.printStackTrace();
				}
			}
		} else if( evt.getSource() == aTable) {
			if( !evt.isControlDown() ) return;
			if( evt.getKeyCode() == KeyEvent.VK_A ) {
				aTable.getSelectionModel().setSelectionInterval(
						0, aModel.analyses.size()-1);
			} else if( evt.getKeyCode() == KeyEvent.VK_C ||
					evt.getKeyCode() == KeyEvent.VK_X ) {
				int[] sel = aTable.getSelectedRows();
				int count = sel.length * (3+aModel.getColumnCount());
				if(count > 10000) {
					JOptionPane.showMessageDialog( null,
						"copy to clipboard limited to 10000 elements\n"
						+ count +" elements selected\n");
					return;
				}
				StringBuffer sb = new StringBuffer();
				sb.append("ID\tLon.\tLat.");
				for(int i=0 ; i<aModel.getColumnCount() ; i++) {
					sb.append("\t"+aModel.getColumnName(i));
				}
				sb.append("\n");
				for(int k=0 ; k<sel.length ; k++) {
					int i = sel[k];
					sb.append( aModel.getRowName(i) );
					sb.append("\t");
					int stn = ((PDBAnalysis)aModel.analyses.get(i)).getStationNum();
					PDBLocation loc = (PDBStation.get(stn)).getLocation();
					sb.append( new Float(loc.lon) );
					sb.append("\t");
					sb.append( new Float(loc.lat) );
					for( int j=0 ; j<aModel.getColumnCount() ; j++) {
						sb.append("\t");
						Object v = aModel.getValueAt(i, j);
						if(v!=null)sb.append( v);
					}
					if(i!=aModel.getRowCount()-1) sb.append("\n");
				}
				JTextArea ta = new JTextArea( sb.toString() );
				ta.selectAll();
				ta.cut();
			} else if( evt.getKeyCode() == KeyEvent.VK_S ) {
				int[] sel = aTable.getSelectedRows();
				int ok = chooser.showSaveDialog(null);
				if( ok == chooser.CANCEL_OPTION ) return;
				try {
					PrintStream out = new PrintStream(
						new FileOutputStream(chooser.getSelectedFile()));
					StringBuffer sb = new StringBuffer();
					sb.append("\"ID\",\"Lon.\",\"Lat.");
					for(int i=0 ; i<aModel.getColumnCount() ; i++) {
						sb.append("\",\""+aModel.getColumnName(i));
					}
					out.println(sb+"\",\r");
					for(int k=0 ; k<sel.length ; k++) {
						int i = sel[k];
						sb = new StringBuffer();
						sb.append( "\""+aModel.getRowName(i) );
						sb.append("\",\"");
						int stn = ((PDBAnalysis)aModel.analyses.get(i)).getStationNum();
						PDBLocation loc = (PDBStation.get(stn)).getLocation();
						sb.append( new Float(loc.lon) );
						sb.append("\",\"");
						sb.append( new Float(loc.lat) );
						for( int j=0 ; j<aModel.getColumnCount() ; j++) {
							sb.append("\",\"");
							Object v = aModel.getValueAt(i, j);
							if(v!=null)sb.append( v);
						}
						out.println(sb+"\",\r");
					}
/*
					sb.append("ID,Lon.,Lat.");
					for(int i=0 ; i<aModel.getColumnCount() ; i++) {
						sb.append(","+aModel.getColumnName(i));
					}
					out.println(sb+"\r");
					for(int k=0 ; k<sel.length ; k++) {
						int i = sel[k];
						sb = new StringBuffer();
						sb.append( aModel.getRowName(i) );
						sb.append(",");
						int stn = ((PDBAnalysis)aModel.analyses.get(i)).getStationNum();
						PDBLocation loc = (PDBStation.get(stn)).getLocation();
						sb.append( new Float(loc.lon) );
						sb.append(",");
						sb.append( new Float(loc.lat) );
						for( int j=0 ; j<aModel.getColumnCount() ; j++) {
							sb.append(",");
							Object v = aModel.getValueAt(i, j);
							if(v!=null)sb.append( v);
						}
						out.println(sb+"\r");
					}
*/
					out.flush();
					out.close();
				} catch(IOException ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	protected Iterator getSelectionData() {
		return new Iterator() {
			int i=-1;
			TableModel tm;
			XBTable t;
			{
				switch (dataDisplay.getSelectedIndex()) {
				case 0:
					tm = model;
					t = table;
					break;
				case 1:
					tm = sModel;
					t = sTable;
					break;
				case 2:
					tm = aModel;
					t = aTable;
					break;
				default:
					break;
				}
			}
			int[] selection = t.getSelectedRows();

			public void remove() {
				throw new UnsupportedOperationException();
			}

			public boolean hasNext() {
				return i < selection.length;
			}

			public Object next() {
				StringBuffer s = new StringBuffer();
				if (i==-1) { // Build Header
					if (tm instanceof PDBStationModel)
						s.append("Station ID\t");
					else {
						if (tm instanceof PDBSampleModel)
							s.append("Sample ID\t");
						else
							s.append("Analysis ID\t");
						s.append("Latitude\t");
						s.append("Longitude\t");
					}
				} else {
					int index = selection[i];
					s.append(t.getRowHeader().getModel().getElementAt(index).toString() + "\t");

					if (!(tm instanceof PDBStationModel)) {
						PDBStation stat;
						if (tm instanceof PDBSampleModel) {
							PDBSampleModel sm = (PDBSampleModel) tm;
							stat = PDBStation.get(((PDBSample)sm.samples.get(index)).getStationNum());
						} else {
							PDBAnalysisModel am = (PDBAnalysisModel) tm;
							PDBAnalysis a = (PDBAnalysis) am.analyses.get(index);
							stat = PDBStation.get(a.getStationNum());
						}
						s.append(stat.getLatitude() + "\t" + stat.getLongitude() + "\t");
					}
				}

				for (int z = 0;z<tm.getColumnCount();z++){
					if (i==-1) { // Build Header
						s.append(tm.getColumnName(z));
					}
					else {
						Object obj = tm.getValueAt(selection[i], z);
						if (obj == null)
							obj = " ";
						s.append(obj);
					}
					if (z<tm.getColumnCount()-1) s.append("\t");
				}
				i++;				
				return s.toString();
			}
		};
	}

	protected Iterator getTableData() {
		return new Iterator() {
			int i=-1;
			TableModel tm;
			XBTable t;
			{
				switch (dataDisplay.getSelectedIndex()) {
				case 0:
					tm = model;
					t = table;
					break;
				case 1:
					tm = sModel;
					t = sTable;
					break;
				case 2:
					tm = aModel;
					t = aTable;
					break;
				default:
					break;
				}
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}

			public boolean hasNext() {
				return i < tm.getRowCount();
			}

			public Object next() {
				StringBuffer s = new StringBuffer();
				if (i==-1) { // Build Header
					if (tm instanceof PDBStationModel)
						s.append("Station ID\t");
					else {
						if (tm instanceof PDBSampleModel)
							s.append("Sample ID\t");
						else
							s.append("Analysis ID\t");

						s.append("Latitude\t");
						s.append("Longitude\t");
					}
				} else {
					s.append(t.getRowHeader().getModel().getElementAt(i).toString() + "\t");

					if (!(tm instanceof PDBStationModel)) {
						PDBStation stat;
						if (tm instanceof PDBSampleModel) {
							PDBSampleModel sm = (PDBSampleModel) tm;
							stat = PDBStation.get(((PDBSample)sm.samples.get(i)).getStationNum());
						} else {
							PDBAnalysisModel am = (PDBAnalysisModel) tm;
							PDBAnalysis a = (PDBAnalysis) am.analyses.get(i);
							stat = PDBStation.get(a.getStationNum());
						}
						s.append(stat.getLatitude() + "\t" + stat.getLongitude() + "\t");
					}
				}

				for (int z = 0;z<tm.getColumnCount();z++){
					if (i==-1)	{ // Build Header
						s.append(tm.getColumnName(z));
					}
					else {
						Object obj = tm.getValueAt(i, z);
						if (obj == null)
							obj = " ";
						s.append(obj);
					}
					if (z<tm.getColumnCount()-1) s.append("\t");
				}
				i++;
				return s.toString();
			}
		};
	}

	protected void saveAsExcel(String saveOption) {
		Iterator it;
		if (saveOption == "selection") {
			it = getSelectionData();
		} else {
			it = getTableData();
		}
		it.next();
		if (!it.hasNext()) {
			JOptionPane.showMessageDialog(null, "One or more rows must be selected.", "Save Selected", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		if (saveOption == "selection") {
			it = getSelectionData();
		} else {
			it = getTableData();
		}
		
		JFileChooser jfc = new JFileChooser(System.getProperty("user.home"));
		ExcelFileFilter eff = new ExcelFileFilter();
		jfc.setFileFilter(eff);
		File f=new File("PetDB_Export.xls");
		jfc.setSelectedFile(f);
		do {
			int c = jfc.showSaveDialog(null);
			if (c==jfc.CANCEL_OPTION||c==jfc.ERROR_OPTION) return;
			f = jfc.getSelectedFile();
			if (f.exists()) {
				c=JOptionPane.showConfirmDialog(null, "File Already Exists\nConfirm Overwrite");
				if (c==JOptionPane.OK_OPTION) break;
				if (c==JOptionPane.CANCEL_OPTION) return;
			}
		} while (f.exists());

		try {
			WritableWorkbook wb = Workbook.createWorkbook(f);
			WritableSheet sheet = wb.createSheet("First Sheet", 0);

			int r = 0;
			while (it.hasNext()) {
				String[] split = it.next().toString().split("\\t");
				for (int i = 0; i < split.length; i++)
					sheet.addCell( new Label(i,r,split[i]) );
				r++;
			}

			wb.write();
			wb.close();
			MapApp.sendLogMessage("Saving_or_Downloading&portal="+getDBName()+"&saveOption="+saveOption+"&fmt=excel&table="+dataDisplay.getSelectedIndex());
		} catch (Exception ex){
		}
	}

	protected void saveAsASCII(String saveOption) {
		Iterator it;
		if (saveOption == "selection") {
			it = getSelectionData();
		} else {
			it = getTableData();
		}
		
		it.next();
		if (!it.hasNext()) {
			JOptionPane.showMessageDialog(null, "One or more rows must be selected.", "Save Selected", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		if (saveOption == "selection") {
			it = getSelectionData();
		} else {
			it = getTableData();
		}
		
		JFileChooser jfc = new JFileChooser(System.getProperty("user.home"));
		File f = new File("PetDB_Export.txt");
		jfc.setSelectedFile(f);
		do {
			int c = jfc.showSaveDialog(null);
			if (c==jfc.CANCEL_OPTION||c==jfc.ERROR_OPTION) return;
			f = jfc.getSelectedFile();
			if (f.exists()) {
				c=JOptionPane.showConfirmDialog(null, "File Already Exists\nConfirm Overwrite");
				if (c==JOptionPane.OK_OPTION) break;
				if (c==JOptionPane.CANCEL_OPTION) return;
			}
		} while (f.exists());

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(f));
			for (; it.hasNext(); ) {
				out.write(it.next().toString());
				out.write("\n");
			}
			out.close();
			MapApp.sendLogMessage("Saving_or_Downloading&portal="+getDBName()+"&saveOption="+saveOption+"&fmt=ascii&table="+dataDisplay.getSelectedIndex());
				
		} catch (IOException ex){}
	}

	protected void copyToClipboard(Iterator it) {
		StringBuffer sb = new StringBuffer();
		for (; it.hasNext(); ) {
			sb.append(it.next().toString());
			sb.append("\n");
		}
		Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection ss = new StringSelection(sb.toString());
		c.setContents(ss, ss);
	}
}
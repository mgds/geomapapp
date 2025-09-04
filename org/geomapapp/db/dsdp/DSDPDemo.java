package org.geomapapp.db.dsdp;

import haxby.db.custom.DBGraph;
import haxby.db.custom.ExcelFileFilter;
import haxby.map.MapApp;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

import org.geomapapp.util.Cursors;
import org.geomapapp.util.DensityXYGraph;
import org.geomapapp.util.Icons;
import org.geomapapp.util.ImageComponent;
import org.geomapapp.util.XYGraph;
import org.geomapapp.util.Zoomer;

public class DSDPDemo implements WindowListener, MouseMotionListener, AdjustmentListener, ItemListener, ActionListener, KeyListener, MouseListener, PropertyChangeListener	{
	DSDP dsdp;
	DSDPModel model;
	Box box;
	AgeDepthModel ageModel;
	haxby.util.XBTable table;
	JDialog holeDialog;
	JDialog logDialog;
	//JDialog dsdpF;
	JFrame dsdpF;
	CoreDisplay coreDisp;
	CoreDescriptionsDisplay coreDescDisp;
	CoreColorDisplay coreColorDisplay;
	AgeDisplay ageDisp;
	FossilDisplay fossilDisp;
	DSDPHole hole;
	JScrollPane sp;
	FossilGroup group;
	JComboBox fossilCB;
	JToggleButton fossilTB;
	JToggleButton logs;
	JButton janusB;
	JButton chronosArcB;
	JButton closeDSDP;
	JButton saveB;
	JDialog saveDialog;
	JRadioButton saveAllRB;
	JRadioButton saveSelectionRB;
	JButton okB;
	JButton cancelB;
	Point anchorLocation;
	private WindowAdapter onClosingAddGraphAdapter;

	static String DSDP_PATH = PathUtil.getPath("DSDP/DSDP_PATH",
			MapApp.BASE_URL+"/data/portals/dsdp/");

	static String DSDP_AGE_DEPTH_PATH = PathUtil.getPath("DSDP/DSDP_AGE_DEPTH_PATH",
			MapApp.BASE_URL+"/data/portals/dsdp/age_depth/");

	static String JANUS_QUERY_PATH = 
		PathUtil.getPath("DSDP/JANUS_QUERY_PATH", 
						"https://iodp.tamu.edu/janusweb/coring_summaries/holesumm.cgi");

	static String CHRONOS_PATH = 
		PathUtil.getPath("DSDP/CHRONOS_PATH", 
						"https://doi.org/10.1130/GES00009.1");

//	GMA 1.4.8: Add button to bring DSDP in front of any layers
	JButton bringToFront;
	JTree logTree;
	ImageComponent image;
	JDialog imageDialog;
	JDialog graphDialog;
	BRGTable brgTable;
	XYGraph graph;
	JLabel label;
	BRGAcronyms acronyms;
	JTextField acro;
	haxby.map.XMap map;
	String holeDialogTitle;
	JTextField field3;
	String selectedFauna = "DIATOMS";
	float crustalAge = 0;
	float bottomDepth = 0;
	static final int CRUSTAL_AGE_COLUMN_INDEX = 3;
	static final int BOTTOM_DEPTH_COLUMN_INDEX = 4;
	static final int LATITUDE_COLUMN_INDEX = 1;
	static final int LONGITUDE_COLUMN_INDEX = 2;
	static final int PENETRATION_COLUMN_INDEX = 5;

//	***** GMA 1.6.8: Add sediment display
	String prevHoleName;
	boolean adjustment = false;
	JPanel buttonPanel;
	JButton initialReport;
	JButton scientificResults;
	Point currentPos;
	Hashtable sedimentHoleList;
	String sedimentHoleListURLString = DSDP_PATH + "holelist.dat";
	String selectedAddSediment;
	JToggleButton sedimentTB;
	JDialog sedimentDialog;
	JDialog selectSedimentDialog;
	JComboBox selectAddSedimentCB;
	JToggleButton sedimentPhotoTB;
	JToggleButton sedimentCoreDescriptionsTB;
	JToggleButton sedimentViewDataTB;
	JButton addSedimentGraphB;
	JButton closeSedimentB;
	JToggleButton removeSedimentGraph;
	JDialog sedimentViewDataDialog;
	Box sedimentBox;
	JScrollPane sedimentViewDataSP;
	JScrollPane sedimentSP = null;
	JTextArea sedimentViewDataTextArea;
	JButton sedimentSaveDataB;
	JComboBox sedimentCB;
	JComboBox selectAddColumnCB;
	JPanel selectSedimentPanel;
	JPanel sedimentDialogSouthPanel;
	JPanel sedimentDialogBottomPanel;
	JLabel sedimentLabel;
	JComboBox selectSedimentCB = new JComboBox();
	JComboBox columnCB = new JComboBox();
	DensityXYGraph sedimentGraph;
	Vector additionalSedimentGraphs;
	DensityBRGTable sedimentPts;
	XRBRGTable xrPts;
	GrainBRGTable grainPts;
	String sedimentURLString;
	boolean isGrain = false;
	boolean isXR = false;
	CoreDisplay sedimentCoreDisp;
	AgeDisplay sedimentAgeDisp;
	PhotoDisplay sedimentPhotoDisp;
	CoreDescriptionsDisplay coreDescriptionsDisp;
	CoreColorDisplay coreColorSedDisplay;
	int selectSedimentDialogX;
	int selectSedimentDialogY;
//	***** GMA 1.6.8

	JToggleButton stratigraphicRangesTB;
	StratigraphicRangeChart stratChart;

	String epochNamesURLString = DSDP_PATH + "timescales/ICS_age_ages.txt";

	JLabel speciesLabel;
	JLabel chronosLabel;
	JLabel paleoBioLabel;
	JLabel iSpeciesLabel;

	public DSDPDemo(haxby.map.MapApp app) {
		this (app, new DSDP());
	}

	public DSDPDemo(MapApp app, DSDP that_dsdp) {
		map = app.getMap();
		anchorLocation = app.getFrame().getLocation();

		this.dsdp = that_dsdp;

		dsdp.setDemo(this);
		table = dsdp.getTable();
		dsdp.setMap( app.getMap() );
		try {
			group = dsdp.loadGroup( "DIATOMS");
		} catch(java.io.IOException e) {
			e.printStackTrace();
		}

		//JFrame mapFrame = (JFrame)app.getMap().getTopLevelAncestor();
		//mapFrame.setSize(900, 700);
		//dsdpF = new JDialog(mapFrame, "DSDP");

//		***** GMA 1.6.8: Change window title from "DSDP" to "DSDP - ODP - IODP DRILL HOLES"
//		dsdpF = new JFrame("DSDP");

		dsdpF = new JFrame("DSDP - ODP - IODP DRILL HOLES");
//		***** GMA 1.6.8

		dsdpF.getContentPane().add(new JScrollPane(table));
		table.getSelectionModel().addListSelectionListener(
			new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					if (!e.getValueIsAdjusting())
						setHole();
				}
			});

		ageModel = new AgeDepthModel(dsdp);
		ageModel.graph.addMouseMotionListener( this );
		try {
// System.out.println( DSDP.ROOT+"age_depth/age_depth.tsf.gz");
			new DSDPAgeDepth(dsdp, DSDP_AGE_DEPTH_PATH + "age_depth.tsf.gz");
		} catch( java.io.IOException e) {
			e.printStackTrace(System.err);
		}
		model = new DSDPModel( dsdp, this);

		JPanel ageP = new JPanel();

		fossilTB = new JToggleButton( Icons.getIcon(Icons.FOSSIL_RANGE, false) );
		fossilTB.setSelectedIcon( Icons.getIcon(Icons.FOSSIL_RANGE, true) );
		fossilTB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				display();
			}
		});
		fossilTB.setToolTipText("view faunal range charts for selected hole");
		fossilTB.setBorder( null );

//		***** GMA 1.6.8: Add sediment display

		sedimentHoleList = new Hashtable();
		readHoleList();
		sedimentTB = new JToggleButton( Icons.getIcon(Icons.MEASUREMENT_ICON, false) );
		sedimentTB.setSelectedIcon( Icons.getIcon(Icons.MEASUREMENT_ICON, true) );
		sedimentTB.addActionListener(this);
		sedimentTB.setToolTipText("view down-core measurements for selected hole");
		sedimentTB.setBorder( null );
//		***** GMA 1.6.8

		stratigraphicRangesTB = new JToggleButton(Icons.getIcon(Icons.STRATIGRAPHIC_RANGES_ICON, false));
		stratigraphicRangesTB.setSelectedIcon(Icons.getIcon(Icons.STRATIGRAPHIC_RANGES_ICON, true));
		stratigraphicRangesTB.addActionListener(this);
		stratigraphicRangesTB.setToolTipText("view stratigraphic range chart");
		stratigraphicRangesTB.setBorder( null );

		saveB = new JButton(Icons.getIcon(Icons.SAVE, false));
		saveB.setSelectedIcon(Icons.getIcon(Icons.SAVE, true));
		saveB.addActionListener(this);
		saveB.setToolTipText("Save");
		saveB.setBorder(null);

		closeDSDP = new JButton("Close");
		closeDSDP.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dsdpF.dispose();
				map.removeOverlay(dsdp.db);
				((MapApp) map.getApp()).deselectDSDP();
				map.repaint();
			}
		});

//		***** GMA 1.4.8: "Bring to Front" button will put DSDP in front of any layers
		bringToFront = new JButton("Bring to Front");
		bringToFront.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				map.removeOverlay( dsdp.db );
				map.addOverlay("Ocean Floor Drilling", dsdp.db );
				map.repaint();
			}
		});
//		***** GMA 1.4.8	
		ageP.add( model.getToggle() );
		ageP.add( fossilTB );		
		ageP.add( ageModel.getToggle() );
//		***** GMA 1.6.8: Add sediment display button
		ageP.add(sedimentTB);

		ageP.add(stratigraphicRangesTB);
		ageP.add(saveB);
		ageP.add( closeDSDP );

//		GMA 1.4.8: Add bringToFront button to the DSDP window
		ageP.add( bringToFront );

//		readEpochNames();

		dsdpF.addWindowListener(this);
		dsdpF.getContentPane().add(ageP,"North");
		dsdpF.pack();
		dsdpF.setSize(800,200);
		Point dsdpFLoc = new Point(anchorLocation.x - dsdpF.getWidth()/5, anchorLocation.y + app.getFrame().getHeight()/2);
		GraphicsDevice gd = app.getFrame().getGraphicsConfiguration().getDevice();
		Rectangle bounds = gd.getDefaultConfiguration().getBounds();
		if(dsdpFLoc.y + dsdpF.getHeight() > bounds.height + bounds.y) {
			dsdpFLoc.y = bounds.height + bounds.y - dsdpF.getHeight();
		}
		if(dsdpFLoc.x < bounds.x) {
			dsdpFLoc.x = bounds.x;
		}
		dsdpF.setLocation(dsdpFLoc);
		dsdpF.toFront();
		dsdpF.setVisible(true);
		dsdpF.requestFocus();
	}

	public void close() {
		closeDSDP.doClick();
	}

	public void adjustGraphs( double zScale, double centerY, String source ) {
		System.out.println("Adjust " + source);
		double currentAgeDouble = 0.0;
		adjustment = true;
		if ( ageModel != null ) {
			currentAgeDouble = ageModel.graph.getYAt( new Point( 0, (int)centerY ) );

			if ( !source.equals("AGE DEPTH MODEL") ) {
				if ( Math.abs(zScale) == Math.abs(ageModel.graph.getZoom()) ) {		
					ageModel.graph.center( new Point( 0, (int)( 2 * zScale * currentAgeDouble ) ) );
				}
				else if ( Math.abs(zScale) > Math.abs(ageModel.graph.getZoom()) ) {
					ageModel.graph.zoomIn( new Point( 0, (int)( zScale * currentAgeDouble ) ) );
				}
				else {
					ageModel.graph.zoomOut( new Point( 0, (int)( 4 * zScale * currentAgeDouble ) ) );
				}
			}
			currentAgeDouble = ageModel.graph.getYAt( new Point( 0, (int)centerY ) );
		}
		if ( sedimentGraph != null ) {
			if ( !source.equals("SEDIMENT GRAPH") ) {
				if ( Math.abs(zScale) == Math.abs(sedimentGraph.getZoom()) ) {
					sedimentGraph.center(new Point(0,
							(int) (2 * zScale * currentAgeDouble)));
				}
				else if ( Math.abs(zScale) > Math.abs(sedimentGraph.getZoom()) ) {
					sedimentGraph.zoomIn(new Point(0,
							(int) (2 * zScale * currentAgeDouble)));
				}
				else {
					sedimentGraph.zoomOut(new Point(0,
							(int) (2 * zScale * currentAgeDouble)));
				}
			}
			sedimentCoreDisp.setZScale( 2 * zScale );
			sedimentAgeDisp.setZScale( 2 * zScale );
			sedimentPhotoDisp.setZScale( 2 * zScale );
			coreDescriptionsDisp.setZScale( 2 * zScale );
			coreColorSedDisplay.setZScale( 2 * zScale);
			sedimentCoreDisp.invalidate();
			sedimentAgeDisp.invalidate();
			sedimentPhotoDisp.invalidate();
			coreDescriptionsDisp.invalidate();
			coreColorSedDisplay.invalidate();
			sedimentCoreDisp.repaint();
			sedimentAgeDisp.repaint();
			sedimentPhotoDisp.repaint();
			coreDescriptionsDisp.repaint();
			coreColorSedDisplay.repaint();
//			sedimentCoreDisp.scrollRectToVisible( new Rectangle( new Point( 0, (int)centerY ) ) );
//			sedimentAgeDisp.scrollRectToVisible( new Rectangle( new Point( 0, (int)centerY ) ) );
//			sedimentPhotoDisp.scrollRectToVisible( new Rectangle( new Point( 0, (int)centerY ) ) );

//			Set first index of i to be the first "added" graph
			for ( int i = 4; i < sedimentBox.getComponentCount(); i++ ) {
				if ( sedimentBox.getComponent(i) instanceof DensityXYGraph ) {
					DensityXYGraph tempGraph = (DensityXYGraph)sedimentBox.getComponent(i);
					if ( !tempGraph.equals(sedimentGraph) ) {
						if ( Math.abs(zScale) == Math.abs(tempGraph.getZoom()) ) {
							tempGraph.center( new Point( 0, (int)( 2 * zScale * currentAgeDouble ) ) );
						}
						else if ( Math.abs(zScale) > Math.abs(tempGraph.getZoom()) ) {
							tempGraph.zoomIn( new Point( 0, (int)( 2 * zScale * currentAgeDouble ) ) );
						}
						else {
							tempGraph.zoomOut( new Point( 0, (int)( 2 * zScale * currentAgeDouble ) ) );
						}
					}
				}
			}
		}
		adjustment = false;
	}
	
	public void toFront() {
		if(null != dsdpF) {
			dsdpF.toFront();
			dsdpF.requestFocus();
		}
	}

	public boolean getAdjustment() {
		return adjustment;
	}

	public void readEpochNames() {
		try {
//			epochHT = new Vector();
			BufferedReader in = new BufferedReader( new InputStreamReader( ( URLFactory.url(epochNamesURLString) ).openStream() ) );
			String s = null;
			while ( ( s = in.readLine() ) != null ) {
				String [] sArr = s.split("\t");
				if ( sArr.length > 2 ) {
//					sArr.put
				}
			}
			in.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setZScale(double zScale, Object requestor, int inputY) {
		adjustment = true;
		if( coreDisp != null ) {
			double centerY = 0.0;
			if ( inputY != -1 ) {
				centerY = inputY * zScale;
			}

			coreDisp.setZScale(zScale);
			ageDisp.setZScale(zScale);
			fossilDisp.setZScale(zScale);
			coreDescDisp.setZScale(zScale);
			coreColorDisplay.setZScale(zScale);
			coreDisp.invalidate();
			ageDisp.invalidate();
			fossilDisp.invalidate();
			coreDescDisp.invalidate();
			coreColorDisplay.invalidate();
			box.revalidate();
			sp.revalidate();
			coreDisp.repaint();
			coreDescDisp.repaint();
			coreColorDisplay.repaint();
			ageDisp.repaint();
			fossilDisp.repaint();

			if ( ageModel.graph.isVisible() && requestor.equals(ageModel) ) {
				if ( ageModel.graph.getVisibleRect().getMinY() > coreDisp.getVisibleRect().getCenterY() ) {
					centerY = ageModel.graph.getVisibleRect().getCenterY() + ( coreDisp.getVisibleRect().getHeight() / 2 );
				}
				else {
					centerY = ageModel.graph.getVisibleRect().getCenterY() - ( coreDisp.getVisibleRect().getHeight() / 2 );
				}
			}
			else if ( sedimentDialog != null && sedimentDialog.isVisible() && requestor.equals(sedimentGraph) && sedimentGraph != null) {
				if ( sedimentGraph.getVisibleRect().getMinY() > coreDisp.getVisibleRect().getCenterY() ) {
					centerY = sedimentGraph.getVisibleRect().getCenterY() + ( coreDisp.getVisibleRect().getHeight() / 2 );
				}
				else {
					centerY = sedimentGraph.getVisibleRect().getCenterY() - ( coreDisp.getVisibleRect().getHeight() / 2 );
				}
			}
//			centerY *= -1;
//			double currentAgeDouble = ageModel.graph.getYAt( new Point( 0, (int)centerY ) );
//			sp.getVerticalScrollBar().scrollRectToVisible( new Rectangle( new Point( 0, (int)centerY ) ) );
			coreDisp.scrollRectToVisible( new Rectangle( new Point( 0, (int)centerY ) ) );
			coreDescDisp.scrollRectToVisible( new Rectangle( new Point( 0, (int)centerY ) ) );
			coreColorDisplay.scrollRectToVisible(new Rectangle( new Point( 0, (int)centerY ) ) );
			ageDisp.scrollRectToVisible( new Rectangle( new Point( 0, (int)centerY ) ) );
			fossilDisp.scrollRectToVisible( new Rectangle( new Point( 0, (int)centerY ) ) );
		}
		adjustment = false;
	}
	public void show() {
		dsdpF.setVisible(true);
		dsdp.db.setVisible(true);
		map.addOverlay("DSDP", dsdp.db);
		dsdp.db.redraw();
		dsdpF.toFront();
//		map.repaint();
	}

	void setHole() {
		if( table.getSelectedRows().length!=1 )return;
		int i = table.getSelectedRow();
		if( i<0 ) return;
		String id = (String)table.getValueAt(i, 0);
		DSDPHole hole = dsdp.holeForID(id);
		System.out.println("Set Hole" + "\t" + id);

		if( hole==this.hole )return;
		this.hole = hole;
		display();

//		***** GMA 1.6.8: Re-initialize sediment display

		if ( sedimentDialog != null ) {
			sedimentURLString = null;
		}
		initializeSedimentDisplay();
//		***** GMA 1.6.8

		table.getSelectionModel().setSelectionInterval( i, i);
		map.repaint();
	}

	void openJANUSURL() {
		String [] results = hole.name.split("-");
		String [] results1 = results[1].split("\\D");
		String [] results2 = results[1].split("\\d");

		String holeLetter = hole.name.substring(hole.name.length() - 1);

		String str = null;
		if ( results2 != null && results2.length > 1 ) {
			str = JANUS_QUERY_PATH + 
				"?show_drilled=on&?leg=" + hole.getLeg() + "&site=" + results1[0] + "&hole=" + holeLetter;
		}
		else {
			str = JANUS_QUERY_PATH +
				"?show_drilled=on&?leg=" + hole.getLeg() + "&site=" + results1[0];
		}
		BrowseURL.browseURL(str);
	}

	private void initHoleDialog() {
		holeDialog = new JDialog(dsdpF, hole.toString());

//		Find Current Age for this row and display it as the title for holeDialog
		holeDialogTitle = "ID: " + hole.toString();
		NumberFormat fmt = NumberFormat.getInstance();
		int i = table.getSelectedRow();

		Vector currentRow = (Vector)dsdp.db.getData().get(i);
		if ( currentRow.get(CRUSTAL_AGE_COLUMN_INDEX) != null ) {
			crustalAge = Float.parseFloat(currentRow.get(CRUSTAL_AGE_COLUMN_INDEX).toString());
			holeDialogTitle = holeDialogTitle + "   Crust Age: " + fmt.format(crustalAge) + "Ma";
		}
		if ( currentRow.get(BOTTOM_DEPTH_COLUMN_INDEX) != null ) {
			bottomDepth = Float.parseFloat(currentRow.get(BOTTOM_DEPTH_COLUMN_INDEX).toString());
			holeDialogTitle = holeDialogTitle + "   Depth: " + fmt.format(bottomDepth) + "m";
		}
		holeDialog.setTitle(holeDialogTitle);

		GridBagLayout speciesGBL = new GridBagLayout();
		GridBagConstraints speciesGBC = new GridBagConstraints();
		JPanel speciesPanel = new JPanel(speciesGBL);
		speciesPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		speciesLabel = new JLabel("");
		speciesLabel.setOpaque(true);
		speciesLabel.setBackground(Color.white);
		speciesLabel.setFont(speciesLabel.getFont().deriveFont(Font.PLAIN));
		speciesLabel.addPropertyChangeListener("text",this);
		chronosLabel = new JLabel("");
		chronosLabel.setOpaque(true);
		chronosLabel.setBackground(Color.white);
		chronosLabel.setForeground(Color.blue);
		chronosLabel.addMouseListener(this);
		chronosLabel.addMouseMotionListener(this);
		JLabel emptyLabel = new JLabel("  ");
		emptyLabel.setOpaque(true);
		emptyLabel.setBackground(Color.white);
		JLabel emptyLabel2 = new JLabel("  ");
		emptyLabel2.setOpaque(true);
		emptyLabel2.setBackground(Color.white);
		paleoBioLabel = new JLabel("");
		paleoBioLabel.setOpaque(true);
		paleoBioLabel.setBackground(Color.white);
		paleoBioLabel.setForeground(Color.blue);
		paleoBioLabel.addMouseListener(this);
		paleoBioLabel.addMouseMotionListener(this);
		iSpeciesLabel = new JLabel("");
		iSpeciesLabel.setOpaque(true);
		iSpeciesLabel.setBackground(Color.white);
		iSpeciesLabel.setForeground(Color.blue);
		iSpeciesLabel.addMouseListener(this);
		iSpeciesLabel.addMouseMotionListener(this);
		speciesGBC.fill = GridBagConstraints.BOTH;
		speciesGBC.weightx = 1.0;
		speciesGBL.setConstraints(speciesLabel, speciesGBC);
		speciesPanel.add(speciesLabel);
		speciesGBC.weightx = 0.0;
		speciesGBL.setConstraints(chronosLabel, speciesGBC);
		//speciesPanel.add(chronosLabel);
		speciesPanel.add(emptyLabel);
		speciesGBL.setConstraints(paleoBioLabel, speciesGBC);
		//speciesPanel.add(paleoBioLabel);
		speciesPanel.add(emptyLabel2);
		speciesGBL.setConstraints(iSpeciesLabel, speciesGBC);
		//speciesPanel.add(iSpeciesLabel);

		JTextField field = new JTextField(20);
		JTextField field2 = new JTextField(20);
		field3 = new JTextField(20);
		coreDisp = new CoreDisplay(hole, field);
		coreDescDisp = new CoreDescriptionsDisplay(hole, field);
		coreColorDisplay = new CoreColorDisplay(hole);
//		ageDisp = new AgeDisplay(hole, field);
		ageDisp = new AgeDisplay(hole, label);
//		fossilDisp = new FossilDisplay(hole, speciesField, coreDisp, group, field2);
		fossilDisp = new FossilDisplay(hole, speciesLabel, coreDisp, group, field2);
		coreDisp.addMouseMotionListener( this );
		coreDisp.addMouseListener(this);
		coreDisp.addKeyListener(this);
		coreDescDisp.addMouseMotionListener( this );
		coreDescDisp.addMouseListener(this);
		coreDescDisp.addKeyListener(this);
		coreColorDisplay.addMouseMotionListener( this );
		coreColorDisplay.addMouseListener(this);
		coreColorDisplay.addKeyListener(this);
		ageDisp.addMouseMotionListener( this );
		ageDisp.addMouseListener(this);
		ageDisp.addKeyListener(this);
		fossilDisp.addMouseMotionListener( this );
		fossilDisp.addMouseListener(this);
		fossilDisp.addKeyListener(this);

		box = Box.createHorizontalBox();
		box.add( coreDisp );
		box.add( coreColorDisplay );
		box.add( coreDescDisp );
	//	box.add( box.createHorizontalStrut(1));
		box.add( ageDisp );
		box.add( fossilDisp );
		box.add( box.createHorizontalGlue());
		sp = new JScrollPane(box, 
			sp.VERTICAL_SCROLLBAR_ALWAYS,
			sp.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.getViewport().setSize(400, 800);
		sp.getViewport().addMouseMotionListener( this );
		sp.getVerticalScrollBar().addAdjustmentListener(this);
		holeDialog.getContentPane().add(sp, "Center");

		JPanel fields = new JPanel(new GridLayout(0,1));

//		fields.add(speciesField);
		fields.add(speciesPanel);

		fields.add(field2);
		fields.add(field3);
		holeDialog.getContentPane().add(fields, "South");

		Vector gps = dsdp.getFossilGroups();
		Collections.sort( gps, new Comparator() {
			public int compare(Object o1, Object o2) {
				return o1.toString().compareTo(o2.toString());
			}
		});

		buttonPanel = new JPanel();
		JPanel fossilCBPanel = new JPanel();
//		if ( hole.getLeg() >= 100 ) {
//			buttonPanel = new JPanel( new GridLayout(0, 3) );
//		}
//		else {
//			buttonPanel = new JPanel( new GridLayout(0, 2) );
//		}
		JPanel  panel = new JPanel(new GridLayout(0,1));

		janusB = new JButton("JANUS");
		janusB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openJANUSURL();
			}
		});
		buttonPanel.add(janusB);

		chronosArcB = new JButton("CHRONOS ARC");
		chronosArcB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				BrowseURL.browseURL(CHRONOS_PATH);
			}
		});
		//buttonPanel.add(chronosArcB);

		logs = new JToggleButton("Logs");
		logs.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setHole();
			}
		});
		buttonPanel.add(logs);

		initialReport = new JButton("Initial Report");
		initialReport.addActionListener(this);
		buttonPanel.add(initialReport);

		scientificResults = new JButton("Scientific Results");
		scientificResults.addActionListener(this);
		buttonPanel.add(scientificResults);

		if ( hole.getLeg() >= 100 ) {
			scientificResults.setEnabled(true);
		}
		else {
			scientificResults.setEnabled(false);
		}

		panel.add(buttonPanel);

		fossilCB = new JComboBox( gps );
		fossilCB.setSelectedIndex( gps.indexOf(selectedFauna));
		newGroup();
		fossilCB.addPopupMenuListener( new PopupMenuListener() {
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
			}
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				newGroup();
			}
			public void popupMenuCanceled(PopupMenuEvent e) {
			}
		});

//		TODO: 23 is a hardcoded value, should create class variable or something
		fossilCB.setMaximumRowCount(23);

	//	fossilCB.addItemListener( new ItemListener() {
	//		public void itemStateChanged(ItemEvent e) {
	//			if( e.getStateChange()==e.SELECTED )newGroup();
	//		}
	//	});
	//	fossilCB.addActionListener( new ActionListener() {
	//		public void actionPerformed(ActionEvent e) {
	//			newGroup();
	//		}
	//	});
		fossilCBPanel.add(fossilCB);
		panel.add(fossilCBPanel);
		holeDialog.getContentPane().add(panel, "North");

		holeDialog.pack();
		Dimension dim = holeDialog.getSize();
		dim.height = 800;
		holeDialog.setSize(dim);
		holeDialog.setLocation( anchorLocation.x + 800, anchorLocation.y);
		holeDialog.addWindowListener( new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				fossilTB.setSelected(false);
			}
		});
	}

	void display() {
		if( !fossilTB.isSelected() || hole==null) {
			if( holeDialog!=null && holeDialog.isVisible() )holeDialog.setVisible(false);
			fossilTB.setSelected(false);
			return;
		}
		Dimension dim = null;
		if( holeDialog==null ) {
			initHoleDialog();
		} else {
			dim = holeDialog.getSize();
			coreDisp.setHole(hole);
			coreDescDisp.setHole(hole);
			coreColorDisplay.setHole(hole);
			ageDisp.setHole(hole);
			fossilDisp.setHole(hole);
			coreDisp.invalidate();
			coreDescDisp.invalidate();
			coreColorDisplay.invalidate();
			ageDisp.invalidate();
			fossilDisp.invalidate();
			box.revalidate();
			sp.revalidate();

			holeDialogTitle = "ID: " + hole.toString();
			NumberFormat fmt = NumberFormat.getInstance();

//			***** GMA 1.4.8: Changed method of getting currently selected row so that it works 
//			properly when zoomed in
//			int i = table.getSelectedRow();
//			Vector currentRow = (Vector)dsdp.db.getData().get(i);
			Vector currentRow = (Vector)dsdp.db.getData().get(0);
			for( int k=0 ; k< ((Vector)dsdp.db.getData()).size() ; k++) {
				currentRow = (Vector)dsdp.db.getData().get(k);
				if( hole.toString().compareTo( currentRow.get(0).toString() ) == 0 ) {
					break;
				}
			}
//			***** GMA 1.4.8

			if ( hole.getLeg() < 100 ) {
				scientificResults.setEnabled(false);
			}
			else {
				scientificResults.setEnabled(true);
			}
/*
			if ( hole.getLeg() >= 100 && scientificResults == null ) {
				buttonPanel.remove(logs);
				buttonPanel.remove(initialReport);
				buttonPanel = new JPanel( new GridLayout(0, 3) );
				buttonPanel.add(logs);
				buttonPanel.add(initialReport);

				scientificResults = new JButton("Scientific Results");
				scientificResults.addActionListener(this);
				buttonPanel.add(scientificResults);
				buttonPanel.repaint();
			}
			else if ( hole.getLeg() < 100 && scientificResults != null ){
				buttonPanel.remove(logs);
				buttonPanel.remove(initialReport);
				buttonPanel.remove(scientificResults);
				buttonPanel = new JPanel( new GridLayout(0, 2) );
				buttonPanel.add(logs);
				buttonPanel.add(initialReport);
				scientificResults = null;
				buttonPanel.repaint();
//				buttonPanel.getParent().repaint();
				holeDialog.repaint();
			}
*/

			if ( currentRow.get(CRUSTAL_AGE_COLUMN_INDEX) != null ) {
				crustalAge = Float.parseFloat(currentRow.get(CRUSTAL_AGE_COLUMN_INDEX).toString());
				holeDialogTitle = holeDialogTitle + "   Crust Age: " + fmt.format(crustalAge) + "Ma";
			}
			if ( currentRow.get(BOTTOM_DEPTH_COLUMN_INDEX) != null ) {
				bottomDepth = Float.parseFloat(currentRow.get(BOTTOM_DEPTH_COLUMN_INDEX).toString());
				holeDialogTitle = holeDialogTitle + "   Depth: " + fmt.format(bottomDepth) + "m";
			}
			holeDialog.setTitle(holeDialogTitle);

			coreDisp.repaint();
			coreDescDisp.repaint();
			coreColorDisplay.repaint();
			ageDisp.repaint();
			fossilDisp.repaint();
		}

	//	holeDialog.pack();
	//	holeDialog.setSize(dim);
		if(!holeDialog.isVisible())holeDialog.setVisible(true);
	//	holeDialog.setVisible(true);

		table.requestFocus();
		if( logs.isSelected() ) {
			StringTokenizer st = new StringTokenizer( hole.toString(), "-");
			BRGEntry root = null;
			try {
				root = (new ParseBRG( st.nextToken(), st.nextToken())).getRoot();
			} catch(Exception e) {
				return;
			}

			if(logDialog==null) {
				logDialog = new JDialog(dsdpF, hole.toString());
				logTree = new JTree(root);
				logDialog.getContentPane().add(new JScrollPane(logTree), "Center");
				JButton openURL = new JButton("Open BRG Page");
				logDialog.getContentPane().add( openURL, "North");

				label = new JLabel("logs in "+hole.toString());
				logDialog.getContentPane().add( label, "South" );
				openURL.addActionListener( new ActionListener() {
					public void actionPerformed( ActionEvent e) {
						openURL();
					}
				});
				logDialog.pack();
			//	logDialog.setLocation(820, 20);
				logTree.addTreeSelectionListener( new TreeSelectionListener() {
					public void valueChanged(TreeSelectionEvent e) {
						BRGEntry entry = (BRGEntry)e.getPath().getLastPathComponent();
						setLog( entry );
					}
				});
			} else {
				logTree.setModel( new javax.swing.tree.DefaultTreeModel(root) );
				logDialog.setTitle( hole.toString());
			}
			if(!logDialog.isVisible())logDialog.setVisible(true);
		} else if( logDialog!=null) {
			logDialog.setVisible(false);
		}
	}

	public void exportExcel() {
		JFileChooser jfc = new JFileChooser(System.getProperty("user.dir"));
		//jfc.setLocation(anchorLocation);
		ExcelFileFilter eff = new ExcelFileFilter();
		jfc.setFileFilter(eff);
		File f=new File("dsdpTable.xls");
		jfc.setSelectedFile(f);
		do {
			int c = jfc.showSaveDialog(saveDialog);
			if (c==JFileChooser.CANCEL_OPTION||c==JFileChooser.ERROR_OPTION) return;
			f = jfc.getSelectedFile();
			if (f.exists()) {
				c=JOptionPane.showConfirmDialog(MapApp.anchor, "File Already Exists\nConfirm Overwrite");
				if (c==JOptionPane.OK_OPTION) break;
				if (c==JOptionPane.CANCEL_OPTION) return;
			}
		} while (f.exists());

		try {
			WritableWorkbook wb = Workbook.createWorkbook(f);
			WritableSheet sheet = wb.createSheet("First Sheet", 0);
			for (int i=0;i<table.getColumnCount();i++)
				sheet.addCell( new Label(i,0,table.getColumnName(i)) );
			for (int i=0;i<table.getRowCount();i++) {
				for (int j=0; j<table.getColumnCount();j++) {
					Object o = table.getValueAt(i, j);
					if (o == null || ( o instanceof String && ((String)o).equals("NaN") ) ) o = "";
					sheet.addCell( new Label(j,i+1,o.toString()) );
				}
			}
			wb.write();
			wb.close();
			MapApp.sendLogMessage("Saving_or_Downloading&portal=Ocean Floor Drilling&what=all");
		} catch (Exception ex){
			ex.printStackTrace();
		}
	}

	public void exportSelectExcel(){
		if (table.getSelectedRowCount() == 0) {
			JOptionPane.showMessageDialog(saveDialog, "No data selected for export", "No Selection", JOptionPane.ERROR_MESSAGE);
			return;
		}

		JFileChooser jfc = new JFileChooser(System.getProperty("user.dir"));
		//jfc.setLocation(anchorLocation);
		ExcelFileFilter eff = new ExcelFileFilter();
		jfc.setFileFilter(eff);
		File f=new File("dsdpTableSelection.xls");
		jfc.setSelectedFile(f);
		do {
			int c = jfc.showSaveDialog(saveDialog);
			if (c==JFileChooser.CANCEL_OPTION||c==JFileChooser.ERROR_OPTION) return;
			f = jfc.getSelectedFile();
			if (f.exists()) {
				c=JOptionPane.showConfirmDialog(MapApp.anchor, "File Already Exists\nConfirm Overwrite");
				if (c==JOptionPane.OK_OPTION) break;
				if (c==JOptionPane.CANCEL_OPTION) return;
			}
		} while (f.exists());

		try {
			WritableWorkbook wb = Workbook.createWorkbook(f);
			WritableSheet sheet = wb.createSheet("First Sheet", 0);
			for (int i=0;i<table.getColumnCount();i++)
				sheet.addCell( new Label(i,0,table.getColumnName(i)) );
			int sel[] = table.getSelectedRows();
			for (int i=0;i<sel.length;i++) {
				for (int j=0; j<table.getColumnCount();j++) {
					Object o = table.getValueAt(sel[i], j);
					if (o == null || ( o instanceof String && ((String)o).equals("NaN") ) ) o = "";
					sheet.addCell( new Label(j,i+1,o.toString()) );
				}
			}
			wb.write();
			wb.close();
			MapApp.sendLogMessage("Saving_or_Downloading&portal=Ocean Floor Drilling&what=selection");
		} catch (Exception ex){
			ex.printStackTrace();
		}
	}

	void openURL() {
//		ODP legs are in online2, DSDP legs are in online3
		String url = PathUtil.getPath("BRG_ROOT_PATH",
				"https://lamont.columbia.edu/research-divisions/marine-large-programs");
		BrowseURL.browseURL(url);
	}
	void setLog( BRGEntry entry ) {
		try {
			String url = entry.getURL();
			if( url==null ) {
				if( imageDialog!=null ) imageDialog.setVisible(false);
				if( graphDialog!=null ) graphDialog.setVisible(false);
				return;
			}
			if( url.toLowerCase().endsWith(".gif") ) {
				if( graphDialog!=null ) graphDialog.setVisible(false);
				label.setText("gif image");
				if( imageDialog==null ) {
					image = new ImageComponent( javax.imageio.ImageIO.read(haxby.util.URLFactory.url(url)) );
					image.setScrollableTracksViewportWidth(true);
					Zoomer z = new Zoomer(image);
					image.addMouseListener(z);
					image.addKeyListener(z);
					imageDialog = new JDialog(dsdpF);
					imageDialog.getContentPane().add(new JScrollPane(image));
				//	JButton button = new JButton("Modify Color Balance");
				//	imageDialog.getContentPane().add( button, "North");
				//	button.addActionListener(new ActionListener() {
				//		public void actionPerformed( ActionEvent e ) {
				//			showBalance();
				//		}
				//	});
					imageDialog.pack();
					imageDialog.setSize( new Dimension( 400,500) );
					imageDialog.setLocation(anchorLocation.x + 600,anchorLocation.y);
				} else {
					image.setImage( javax.imageio.ImageIO.read(haxby.util.URLFactory.url(url)));
				}
				if(!imageDialog.isVisible())imageDialog.setVisible(true);
				image.repaint();
			} else if( url.toLowerCase().endsWith(".dat")) {
				label.setText("Data Table");
				if( imageDialog!=null ) imageDialog.setVisible(false);
				System.out.println(url);
				brgTable = new BRGTable(url);
				if( graphDialog==null ) {
					acronyms = new BRGAcronyms();
					acro = new JTextField(20);
					graph = new XYGraph( new BRGTable(url), 0 );
					graph.setScrollableTracksViewportWidth(true);
					Zoomer z = new Zoomer(graph);
					graph.addMouseListener(z);
					graph.addKeyListener(z);
					graph.addKeyListener( new KeyAdapter() {
						public void keyPressed(KeyEvent e) {
							if( e.getKeyCode()==e.VK_SPACE)nextDataIndex();
							if( e.getKeyCode()==e.VK_BACK_SPACE)previousDataIndex();
						}
					});
					graphDialog = new JDialog(dsdpF);
					graphDialog.getContentPane().add(new JScrollPane(graph), "Center");

					JPanel panel = new JPanel(new GridLayout(0,1) );
					panel.add(acro);
					JButton trim = new JButton("Trim");
				//	JButton highTrim = new JButton("Trim high");
				//	JPanel panel1 = new JPanel(new GridLayout(1,0) );
				//	panel1.add( lowTrim );
				//	panel1.add( highTrim );
					panel.add(trim);
					trim.addActionListener( new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							trim();
						}
					});
				//	lowTrim.addActionListener( new ActionListener() {
				//		public void actionPerformed(ActionEvent e) {
				//			trim( false );
				//		}
				//	});
					graphDialog.getContentPane().add(panel, "North");
					graphDialog.pack();
					graphDialog.setSize( new Dimension( 400,500) );
					graphDialog.setLocation(anchorLocation.x+600, anchorLocation.y);
				} else {
					graph.setPoints( new BRGTable(url), 0 );
				}
				int n = brgTable.getDataCount();
				acro.setText( acronyms.getDescription(brgTable.getXTitle(0)) +" (1 of "+n+")");
				if(!graphDialog.isVisible())graphDialog.setVisible(true);
				graph.repaint();
			} else {
				label.setText("logs in "+hole.toString());
				if( imageDialog!=null ) imageDialog.setVisible(false);
				if( graphDialog!=null ) graphDialog.setVisible(false);
			}
			logTree.requestFocus();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	void trim() {
		int i = graph.getDataIndex();
		brgTable.trim( i );
		graph.setPoints( brgTable, i);
		graph.repaint();
	}
	void trim(boolean max) {
		brgTable.trim( max, graph.getDataIndex() );
	}
	void showBalance() {
		if( image==null )return;
		image.showColorDialog();
	}
	void previousDataIndex() {
		int n = brgTable.getDataCount();
		int i = graph.getDataIndex();
		i = (i+n-1)%n;
		graph.setPoints( brgTable, i);
		graph.repaint();
		acro.setText( acronyms.getDescription(brgTable.getXTitle(i)) +" ("+(i+1)+" of "+n+")" );
	}
	void nextDataIndex() {
		int n = brgTable.getDataCount();
		int i = graph.getDataIndex();
		i = (i+1)%n;
		graph.setPoints( brgTable, i);
		graph.repaint();
		acro.setText( acronyms.getDescription(brgTable.getXTitle(i)) +" ("+(i+1)+" of "+n+")");
	}
	void newGroup() {
		String gp = (String)fossilCB.getSelectedItem();
		if( gp.equals(group.getGroupName()) )return;
//		dsdp.removeFossilGroup(gp);
		try {
			group = dsdp.loadGroup(gp);
			fossilDisp.setGroup( group );
		} catch(Exception e) {
			e.printStackTrace();
		}
		display();
	}

//	***** GMA 1.6.8: Read in the hole-list that indicates which sediments are available for a particular hole
	public void readHoleList() {
		try {
			BufferedReader in = new BufferedReader( new InputStreamReader( ( URLFactory.url(sedimentHoleListURLString) ).openStream() ) );
			String s;
			while ( ( s = in.readLine() ) != null ) {
				String[] sArr = s.split("\t");
				String sedimentsForHole = "";
				if ( sArr.length > 1 ) {
					for ( int i = 1; i < sArr.length; i++ ) {
						if ( sArr[i] != null ) {
							sedimentsForHole = sedimentsForHole + sArr[i] + "\t";
						}
					}
					sedimentsForHole = sedimentsForHole.trim();
				}  
				String holeString = sArr[0];
				holeString = holeString.substring( 0, holeString.length() - 1 );
				holeString = holeString.replaceAll( "/", "-" );
				sedimentHoleList.put( holeString, sedimentsForHole );
			}
			in.close();
		} catch (MalformedURLException mue) {
			mue.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
//	***** GMA 1.6.8
	public void disposeAddSedimentDialog() {
		selectSedimentDialogX = selectSedimentDialog.getX();
		selectSedimentDialogY = selectSedimentDialog.getY();
		if ( selectAddSedimentCB != null ) {
			selectSedimentDialog.remove(selectAddSedimentCB);
			selectAddSedimentCB.removeItemListener(this);
			selectAddSedimentCB = null;
		}
		if ( selectAddColumnCB != null ) {
			selectSedimentDialog.remove(selectAddColumnCB);
			selectAddColumnCB.removeItemListener(this);
			selectAddColumnCB = null;
		}
		selectSedimentDialog.setVisible(false);
		selectSedimentDialog.dispose();
		selectSedimentDialog = null;
	}

	public void disposeSedimentViewDataDialog() {
		sedimentViewDataDialog.setVisible(false);
		sedimentViewDataDialog.removeWindowListener(this);
		selectSedimentDialog.removeWindowListener(this);
		sedimentViewDataDialog.remove(sedimentViewDataSP);
		sedimentViewDataSP.remove(sedimentViewDataTextArea);
		sedimentViewDataSP.getVerticalScrollBar().removeAdjustmentListener(this);
		sedimentViewDataSP = null;
		sedimentViewDataTextArea= null;
		sedimentViewDataDialog.dispose();
		sedimentViewDataDialog = null;
		System.gc();
	}

//	***** GMA 1.6.8: Initialize the sediment display
	public void initializeSedimentDisplay() {
		if ( hole == null ) {
			return;
		}
		Point screenLocation = anchorLocation;
		Dimension sedimentDialogSize = new Dimension( 400, 600 );
		
		if ( sedimentDialog != null ) {
			if (sedimentDialog.isVisible())
				screenLocation = sedimentDialog.getLocationOnScreen();
			sedimentDialogSize = sedimentDialog.getSize();
			
			sedimentPhotoDisp.removeMouseMotionListener(this);
			sedimentPhotoDisp.removeKeyListener(this);
			sedimentPhotoDisp.removeMouseListener(this);
			coreColorSedDisplay.removeMouseListener(this);
			coreColorSedDisplay.removeKeyListener(this);
			coreColorSedDisplay.removeMouseMotionListener(this);
			coreDescriptionsDisp.removeMouseMotionListener(this);
			coreDescriptionsDisp.removeKeyListener(this);
			coreDescriptionsDisp.removeMouseListener(this);
			sedimentAgeDisp.removeMouseMotionListener(this);
			sedimentAgeDisp.removeKeyListener(this);
			sedimentAgeDisp.removeMouseListener(this);
			sedimentCoreDisp.removeMouseMotionListener(this);
			sedimentCoreDisp.removeKeyListener(this);
			sedimentCoreDisp.removeMouseListener(this);
			columnCB.removeItemListener(this);
			sedimentPhotoTB.removeActionListener(this);
			sedimentViewDataTB.removeActionListener(this);
			sedimentSaveDataB.removeActionListener(this);
			sedimentDialog.dispose();
			sedimentDialog = null;
		}
		if ( sedimentViewDataDialog != null ) {
			disposeSedimentViewDataDialog();
		}
		if ( sedimentHoleList.containsKey(hole.toString()) ) {
			selectSedimentCB.removeItemListener(this);
			sedimentBox = Box.createHorizontalBox();
			sedimentDialog = new JDialog(dsdpF);
			sedimentDialog.addWindowListener(this);
			selectSedimentPanel = new JPanel();
			sedimentDialogBottomPanel = new JPanel( new GridLayout(0,1) );
			sedimentDialogSouthPanel = new JPanel( new GridLayout(2,3) );
			selectSedimentCB = new JComboBox();
			columnCB = new JComboBox();
			sedimentLabel = new JLabel(""); 
			sedimentPhotoTB = new JToggleButton( "Photos", false );
			sedimentCoreDescriptionsTB = new JToggleButton( "Core Desc.", false );
			sedimentViewDataTB = new JToggleButton( "View Data", false );
			sedimentSaveDataB = new JButton("Save Data");
			addSedimentGraphB = new JButton("Add Graph");
			closeSedimentB = new JButton("Close");

			columnCB.addItemListener(this);
			sedimentPhotoTB.addActionListener(this);
			sedimentCoreDescriptionsTB.addActionListener(this);
			sedimentViewDataTB.addActionListener(this);
			sedimentSaveDataB.addActionListener(this);
			addSedimentGraphB.addActionListener(this);
			closeSedimentB.addActionListener(this);

			sedimentPhotoTB.setToolTipText("Highlight Photos");
			sedimentCoreDescriptionsTB.setToolTipText("Highlight Core Descriptions");
			addSedimentGraphB.setToolTipText("Press to add additional sediment graphs");

			sedimentDialog.setLayout( new BorderLayout() );
			sedimentDialog.setTitle("ID: " + hole.toString());

			String sedimentsForHole = (String)sedimentHoleList.get(hole.toString());
			String[] sArr = sedimentsForHole.split("\\s");

			for ( int i = 0; i < sArr.length; i++ ) {
				selectSedimentCB.addItem(sArr[i]);
			}
			sedimentDialog.getContentPane().add( selectSedimentPanel, "North" );

			sedimentDialogSouthPanel.add(sedimentPhotoTB);
			sedimentDialogSouthPanel.add(sedimentCoreDescriptionsTB);
			sedimentDialogSouthPanel.add(sedimentViewDataTB);
			sedimentDialogSouthPanel.add(sedimentSaveDataB);
			sedimentDialogSouthPanel.add(addSedimentGraphB);
			sedimentDialogSouthPanel.add(closeSedimentB);
			sedimentDialogBottomPanel.add(sedimentDialogSouthPanel);
			sedimentDialogBottomPanel.add( sedimentLabel);
			sedimentDialog.getContentPane().add( sedimentDialogBottomPanel, "South" );

			JTextField field = new JTextField(20);
			sedimentCoreDisp = new CoreDisplay(hole, field);
			sedimentAgeDisp = new AgeDisplay(hole, field);
			sedimentPhotoDisp = new PhotoDisplay(hole, field);
			coreDescriptionsDisp = new CoreDescriptionsDisplay(hole, field);
			coreColorSedDisplay = new CoreColorDisplay(hole);
			sedimentCoreDisp.addMouseMotionListener(this);
			sedimentCoreDisp.addKeyListener(this);
			sedimentCoreDisp.addMouseListener(this);
			sedimentAgeDisp.addMouseMotionListener(this);
			sedimentAgeDisp.addKeyListener(this);
			sedimentAgeDisp.addMouseListener(this);
			sedimentPhotoDisp.addMouseMotionListener(this);
			sedimentPhotoDisp.addKeyListener(this);
			sedimentPhotoDisp.addMouseListener(this);
			coreDescriptionsDisp.addMouseMotionListener(this);
			coreDescriptionsDisp.addKeyListener(this);
			coreDescriptionsDisp.addMouseListener(this);
			coreColorSedDisplay.addMouseMotionListener(this);
			coreColorSedDisplay.addKeyListener(this);
			coreColorSedDisplay.addMouseListener(this);
			sedimentBox.add(sedimentCoreDisp);
			sedimentBox.add(sedimentAgeDisp);
			sedimentBox.add(sedimentPhotoDisp);
			if ( coreDescriptionsDisp.exists ) {
				sedimentBox.add(coreDescriptionsDisp);
			}
			if ( coreColorSedDisplay.exists ) {
				sedimentBox.add(coreColorSedDisplay);
			}

			String testGraphURLString = null;
			if ( hole.getLeg() < 100 ) {
				testGraphURLString = DSDP.DSDP_PATH + (String)selectSedimentCB.getSelectedItem() + "/" + hole.toString() + "-" + ((String)selectSedimentCB.getSelectedItem()) + ".txt";
			}
			else {
				testGraphURLString = DSDP.DSDP_PATH + "ODP_" + (String)selectSedimentCB.getSelectedItem() + "/" + hole.toString() + "-" + ((String)selectSedimentCB.getSelectedItem()) + ".txt";
			}

			double yScale = 0.0;
			if (!"".equals(selectSedimentCB.getSelectedItem())) {
				String[] columnHeadings = null;
				try {
					isGrain = false;
					if ( ((String)selectSedimentCB.getSelectedItem()).equals("GRAIN") ) {
						isGrain = true;
						grainPts = new GrainBRGTable(testGraphURLString);
						sedimentGraph = new DensityXYGraph( grainPts, 0 );
						sedimentGraph.setGrain(true);
					} else if ( ((String)selectSedimentCB.getSelectedItem()).startsWith("XR") ) {
						isXR = true;
						xrPts = new XRBRGTable(testGraphURLString);
						sedimentGraph = new DensityXYGraph( xrPts, 0 );
						sedimentGraph.setXR(true);
					} else {
						isGrain = false;
						isXR = false;
						System.out.println("LOAD: " + testGraphURLString);
						sedimentPts = new DensityBRGTable(testGraphURLString);
						sedimentGraph = new DensityXYGraph( sedimentPts, 0 );
						sedimentGraph.setGrain(false);
					}

					if ( isXR ) {
						columnHeadings = xrPts.headings;
					} else {
						columnHeadings = sedimentPts.headings;
					}

					for ( int i = 1; i < columnHeadings.length; i++ ) {
						columnCB.addItem(columnHeadings[i]);
					}

					selectSedimentPanel.add(selectSedimentCB);
					if ( columnCB.getItemCount() > 1 && !((String)selectSedimentCB.getSelectedItem()).equals("GRAIN") ) {
						selectSedimentPanel.add(columnCB);
					}
					sedimentGraph.setDSDP(dsdp);
					yScale = sedimentGraph.getYScale();
	
					Zoomer z = new Zoomer(sedimentGraph);

					sedimentGraph.setScrollableTracksViewportWidth(true);
	//				sedimentGraph.setScrollableTracksViewportHeight(true);

					sedimentGraph.addMouseMotionListener(this);
					sedimentGraph.addMouseListener(z);
					sedimentGraph.addKeyListener(z);
					sedimentBox.add(sedimentGraph);
				} catch (IOException ioe) {
					System.out.println("URL ERROR, No File Found: " + testGraphURLString);
					sedimentGraph = null;
					sedimentPts = null;
					//ioe.printStackTrace();
					//return;
				}
			} else {
				sedimentGraph = null;
				sedimentPts = null;
			}
			sedimentBox.add(sedimentBox.createHorizontalGlue());
			sedimentSP = new JScrollPane( sedimentBox, sedimentSP.VERTICAL_SCROLLBAR_ALWAYS, sedimentSP.HORIZONTAL_SCROLLBAR_ALWAYS );
			sedimentSP.getVerticalScrollBar().addAdjustmentListener(this);
			sedimentDialog.getContentPane().add( sedimentSP, "Center" );

			selectSedimentCB.addItemListener(this);

			sedimentDialog.pack();
			sedimentDialog.setSize( sedimentDialogSize );
			sedimentDialog.setLocation(screenLocation);
			if ( sedimentTB != null && sedimentTB.isSelected() ) {
				sedimentDialog.setVisible(true);
			}
		}
	}
//	***** GMA 1.6.8

	public void windowActivated(WindowEvent e) {}
	public void windowClosed(WindowEvent e) {}
	public void windowClosing(WindowEvent we) {
		if(we.getSource().equals(selectSedimentDialog)){
			// Closing the select sediments enabled Add Graph
			addSedimentGraphB.setEnabled(true);
		}
		if ( we.getSource().equals(sedimentViewDataDialog) ) {
			disposeSedimentViewDataDialog();
			if ( sedimentViewDataTB != null && sedimentViewDataTB.isVisible() ) {
				sedimentViewDataTB.setSelected(false);
			}
			return;
		}
		else if ( we.getSource().equals(sedimentDialog) ) {
			sedimentDialog.dispose();
			if ( sedimentTB != null && sedimentTB.isVisible() ) {
				sedimentTB.setSelected(false);
			}
			return;
		}
		else if ( we.getSource().equals(saveDialog) ) {
			saveDialog.dispose();
			saveB.setEnabled(true);
			return;
		}
		map.repaint();
	}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}

	public void mouseDragged(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {

		currentPos = e.getPoint();

		if ( e.getSource() == ageModel.graph )	{
			if ( holeDialog != null && holeDialog.isVisible() )	{
				field3.setText(ageModel.depthAgeBTD);
				if ( coreDisp != null ) {
					coreDisp.drawLineAtAge(e.getY());
				}
				if ( coreDescDisp != null ) {
					coreDescDisp.drawLineAtAge(e.getY());
				}
				if ( coreColorDisplay != null ) {
					coreColorDisplay.drawLineAtAge(e.getY());
				}
				if ( ageDisp != null ) {
					ageDisp.drawLineAtAge(e.getY());
				}
				if ( fossilDisp != null ) {
					fossilDisp.drawLineAtAge(e.getY());
				}
				if ( stratChart != null ) {
					double currentAgeDouble = ageModel.cursorAge;
					int inputAge =  (int)((currentAgeDouble * 30.0 * stratChart.stratImage.getZScale()) + 155.0 * stratChart.stratImage.getZScale());
					stratChart.drawLineAtAge(inputAge);
				}
			}
			if ( sedimentDialog != null && sedimentDialog.isVisible() && sedimentGraph != null && sedimentCoreDisp != null && sedimentAgeDisp != null && sedimentPhotoDisp != null ) {
				double currentAgeDouble = ageModel.graph.getYAt(e.getPoint());
				sedimentGraph.drawLineAtAge(currentAgeDouble);

				for ( int i = 4; i < sedimentBox.getComponentCount(); i++ ) {
					if ( sedimentBox.getComponent(i) instanceof DensityXYGraph ) {
						DensityXYGraph tempGraph = (DensityXYGraph)sedimentBox.getComponent(i);
						tempGraph.drawLineAtAge(currentAgeDouble);
					}
				}

				sedimentCoreDisp.drawLineAtAge(e.getY());
				sedimentAgeDisp.drawLineAtAge(e.getY());
				sedimentPhotoDisp.drawLineAtAge(e.getY());
				coreDescriptionsDisp.drawLineAtAge(e.getY());
				coreColorSedDisplay.drawLineAtAge(e.getY());
			}
		}
		else if ( stratChart != null && stratChart.stratImage != null && e.getSource() == stratChart.stratImage )	{
			stratChart.drawLineAtAge(e.getY());
			double currentAgeDouble = ( ( ( ((double)e.getY()) / stratChart.stratImage.getZScale() ) - 155.0 ) / 30.0 );
			double yPos = ageModel.getPosFromAge(currentAgeDouble);
			if ( holeDialog != null && holeDialog.isVisible() )	{
				field3.setText(ageModel.depthAgeBTD);
			}
			if ( coreDisp != null ) {
				coreDisp.drawLineAtAge((int)(coreDisp.zScale * yPos));
			}
			if ( coreDescDisp != null ) {
				coreDescDisp.drawLineAtAge((int)(coreDescDisp.zScale * yPos));
			}
			if ( coreColorDisplay != null ) {
				coreColorDisplay.drawLineAtAge((int)(coreColorDisplay.zScale * yPos));
			}
			if ( ageDisp != null ) {
				ageDisp.drawLineAtAge((int)(ageDisp.zScale * yPos));
			}
			if ( fossilDisp != null && fossilDisp.isVisible() && fossilDisp.isValid() )	{
				fossilDisp.drawLineAtAge((int)(fossilDisp.zScale * yPos));
			}
			if ( ageModel != null && ageModel.graph.isVisible() ) {
				ageModel.move( yPos );
			}
			if ( sedimentDialog != null && sedimentDialog.isVisible() && sedimentGraph != null && sedimentCoreDisp != null && sedimentAgeDisp != null && sedimentPhotoDisp != null ) {
				double currentDepth = ageModel.graph.getYAt(new Point(0, (int)yPos));
				sedimentGraph.drawLineAtAge( 2 * sedimentGraph.getZoom() * currentDepth);
				
				for ( int i = 4; i < sedimentBox.getComponentCount(); i++ ) {
					if ( sedimentBox.getComponent(i) instanceof DensityXYGraph ) {
						DensityXYGraph tempGraph = (DensityXYGraph)sedimentBox.getComponent(i);
						tempGraph.drawLineAtAge( 2 * tempGraph.getZoom() * currentDepth);
					}
				}

				sedimentCoreDisp.drawLineAtAge((int)(sedimentCoreDisp.getZScale() * yPos));
				sedimentAgeDisp.drawLineAtAge((int)(sedimentAgeDisp.getZScale() * yPos));
				sedimentPhotoDisp.drawLineAtAge((int)(sedimentPhotoDisp.getZScale() * yPos));
				coreDescriptionsDisp.drawLineAtAge((int)(coreDescriptionsDisp.getZScale() * yPos));
				coreColorSedDisplay.drawLineAtAge((int)(coreColorSedDisplay.getZScale() * yPos));
			}
		}
		else if (e.getSource() == coreDisp || e.getSource() == ageDisp
				|| e.getSource() == fossilDisp || e.getSource() == coreDescDisp
				|| e.getSource() == coreColorDisplay) {
			
			if ( holeDialog != null && holeDialog.isVisible() ) {
				field3.setText(ageModel.depthAgeBTD);
			}
			if ( coreDisp != null ) {
				coreDisp.drawLineAtAge(e.getY());
			}
			if ( coreDescDisp != null ) {
				coreDescDisp.drawLineAtAge(e.getY());
			}
			if (coreColorDisplay != null) {
				coreColorDisplay.drawLineAtAge(e.getY());
			}
			if ( ageDisp != null ) {
				ageDisp.drawLineAtAge(e.getY());
			}
			if ( fossilDisp != null && fossilDisp.isVisible() && fossilDisp.isValid() )	{
				fossilDisp.drawLineAtAge(e.getY());
			}
			if ( stratChart != null ) {
				double currentAgeDouble = ageModel.cursorAge;
				int inputAge =  (int)((currentAgeDouble * 30.0 * stratChart.stratImage.getZScale()) + 155.0 * stratChart.stratImage.getZScale());
				stratChart.drawLineAtAge(inputAge);
			}
			if ( ageModel != null && ageModel.graph.isVisible() ) {
				ageModel.move( e );
			}
			if ( sedimentDialog != null && sedimentDialog.isVisible() && sedimentGraph != null && sedimentCoreDisp != null && sedimentAgeDisp != null && sedimentPhotoDisp != null ) {
				double currentAgeDouble = ageModel.graph.getYAt(e.getPoint());
				sedimentGraph.drawLineAtAge(currentAgeDouble);

				for ( int i = 4; i < sedimentBox.getComponentCount(); i++ ) {
					if ( sedimentBox.getComponent(i) instanceof DensityXYGraph ) {
						DensityXYGraph tempGraph = (DensityXYGraph)sedimentBox.getComponent(i);
						tempGraph.drawLineAtAge(currentAgeDouble);
					}
				}

				sedimentCoreDisp.drawLineAtAge(e.getY());
				sedimentAgeDisp.drawLineAtAge(e.getY());
				sedimentPhotoDisp.drawLineAtAge(e.getY());
				coreDescriptionsDisp.drawLineAtAge(e.getY());
				coreColorSedDisplay.drawLineAtAge(e.getY());
			}
		}
		else if ( e.getSource().equals(sedimentGraph) || e.getSource().equals(sedimentCoreDisp) || e.getSource().equals(sedimentAgeDisp) || e.getSource().equals(sedimentPhotoDisp) ) {
			double currentAgeDouble = ageModel.graph.getYAt(e.getPoint());
			sedimentLabel.setText("");
			if (sedimentGraph != null)
				sedimentGraph.drawLineAtAge(currentAgeDouble);

			for ( int i = 4; i < sedimentBox.getComponentCount(); i++ ) {
				if ( sedimentBox.getComponent(i) instanceof DensityXYGraph ) {
					DensityXYGraph tempGraph = (DensityXYGraph)sedimentBox.getComponent(i);
					tempGraph.drawLineAtAge(currentAgeDouble);
				}
			}

			sedimentCoreDisp.drawLineAtAge(e.getY());
			sedimentAgeDisp.drawLineAtAge(e.getY());
			sedimentPhotoDisp.drawLineAtAge(e.getY());
			coreDescriptionsDisp.drawLineAtAge(e.getY());
			coreColorSedDisplay.drawLineAtAge(e.getY());
			if ( holeDialog != null && holeDialog.isVisible() )	{
				field3.setText(ageModel.depthAgeBTD);
			}
			if ( coreDisp != null ) {
				coreDisp.drawLineAtAge(e.getY());
			}
			if ( coreDescDisp != null ) {
				coreDescDisp.drawLineAtAge(e.getY());
			}
			if ( coreColorDisplay != null ) {
				coreColorDisplay.drawLineAtAge(e.getY());
			}
			if ( ageDisp != null ) {
				ageDisp.drawLineAtAge(e.getY());
			}
			if ( fossilDisp != null && fossilDisp.isVisible() && fossilDisp.isValid() )	{
				fossilDisp.drawLineAtAge(e.getY());
			}
			if ( ageModel != null && ageModel.graph.isVisible() ) {
				ageModel.move( e );
			}
			if ( stratChart != null ) {
				double currentAge = ageModel.cursorAge;
				int inputAge =  (int)((currentAge * 30.0 * stratChart.stratImage.getZScale()) + 155.0 * stratChart.stratImage.getZScale());
				stratChart.drawLineAtAge(inputAge);
			}

			if ( isXR && xrPts != null ) {
				double[] point = xrPts.getClosestPoint( e.getX() + 5, currentAgeDouble );
				if ( Double.toString(point[0]).indexOf("NaN") == -1 && !Double.toString(point[0]).equals("0.0") ) {
					if (sedimentGraph != null)
						sedimentGraph.drawDotAtPoint(xrPts.getClosestPoint( e.getX() + 5, currentAgeDouble ));
					sedimentLabel.setText("  Depth (mbsf): " + point[2] + "   Type: " + xrPts.getHeading(e.getX() + 5) + "   Value: " + point[0]);
				}
			}
			else if ( !isGrain && sedimentPts != null && sedimentGraph != null) {
				Vector dataVector = sedimentPts.getClosestPoint( sedimentGraph.getDataIndex(), currentAgeDouble );
				if ( dataVector.get(1) != null ) {
					sedimentLabel.setText("  Depth (mbsf): " + ((Double)dataVector.get(0)).toString() + "   Value: " + ((String)dataVector.get(1)).trim());
				}
			}
		}

		else if ( e.getSource().equals(chronosLabel) || e.getSource().equals(paleoBioLabel) || e.getSource().equals(iSpeciesLabel) ) {
			((JLabel)e.getSource()).setCursor(Cursors.getCursor(Cursors.HAND));
//			getTable().setCursor(Cursor.getDefaultCursor());
		}
	}
	public void adjustmentValueChanged(AdjustmentEvent ae) {
		if ( sp != null && 
				ae.getSource() == sp.getVerticalScrollBar()  &&
				! adjustment && 
				coreDisp != null)	{
			adjustGraphs( coreDisp.getZScale(), 
					coreDisp.getVisibleRect().getCenterY(), 
					"FOSSIL DISPLAY" );
		}
		else if ( sedimentSP != null && 
				ae.getSource() == sedimentSP.getVerticalScrollBar() &&
				!adjustment && 
				sedimentGraph != null) {
			adjustGraphs( sedimentGraph.getZoom(), 
					sedimentGraph.getVisibleRect().getCenterY(), 
					"SEDIMENT GRAPH" );
		}
	}

	public void itemStateChanged(ItemEvent ie) {
		if ( ie.getSource().equals(columnCB) && columnCB != null && sedimentGraph != null ) {
//			if ( !isXR ) {
				if ( xrPts != null ) {
					xrPts.setPlotColumn(columnCB.getSelectedIndex());
				}
				sedimentGraph.setPoints( sedimentGraph.getPoints(), columnCB.getSelectedIndex() );
				sedimentGraph.repaint();
//			}
		}
		else if ( ie.getSource().equals(selectAddSedimentCB) ) {
			selectedAddSediment = (String)selectAddSedimentCB.getSelectedItem();
			disposeAddSedimentDialog();
			initializeChooseColumnDialog(selectedAddSediment);
		}
		else if ( ie.getSource().equals(selectAddColumnCB) ) {
			int selectedAddColumn = selectAddColumnCB.getSelectedIndex();
			disposeAddSedimentDialog();
			String tempURLString = null;
			if ( hole.getLeg() < 100 ) {
				tempURLString = DSDP.DSDP_PATH + selectedAddSediment + "/" + hole.toString() + "-" + selectedAddSediment + ".txt";
			}
			else {
				tempURLString = DSDP.DSDP_PATH + "ODP_" + selectedAddSediment + "/" + hole.toString() + "-" + selectedAddSediment + ".txt";
			}
			try {
				DensityXYGraph tempGraph = new DensityXYGraph( new DensityBRGTable(tempURLString), selectedAddColumn - 1 );
				tempGraph.setDSDP(dsdp);
				Zoomer z = new Zoomer(tempGraph);
				tempGraph.setScrollableTracksViewportWidth(true);
				sedimentBox.add(tempGraph);
				sedimentDialog.pack();
				System.out.println("Width: " + sedimentDialog.getWidth() + "\tHeight: " + sedimentDialog.getHeight());
				sedimentDialog.setSize( sedimentDialog.getWidth(), 600 );
				sedimentDialog.setVisible(true);
				adjustment = true;
				while ( tempGraph.getZoom() < sedimentGraph.getZoom() ) {
					tempGraph.zoomIn( new Point( 0, (int)( 2 * sedimentGraph.getVisibleRect().getCenterY() / sedimentGraph.getZoom() ) ) );
				}
				while ( tempGraph.getZoom() > sedimentGraph.getZoom() ) {
					tempGraph.zoomOut( new Point( 0, (int)(  2 * sedimentGraph.getVisibleRect().getCenterY() / sedimentGraph.getZoom() ) ) );
				}
				adjustment = false;
				tempGraph.setCloseButton(true);
				tempGraph.addMouseListener(this);
				addSedimentGraphB.setEnabled(true);
			} catch (IOException ioe) {
				addSedimentGraphB.setEnabled(true);
				ioe.printStackTrace();
			}
		}
		else if ( ie.getSource().equals(selectSedimentCB) && selectSedimentCB != null && sedimentGraph != null ) {
			String testGraphURLString = null;
			if ( hole.getLeg() < 100 ) {
				testGraphURLString = DSDP.DSDP_PATH + (String)selectSedimentCB.getSelectedItem() + "/" + hole.toString() + "-" + ((String)selectSedimentCB.getSelectedItem()) + ".txt";
			}
			else {
				testGraphURLString = DSDP.DSDP_PATH + "ODP_" + (String)selectSedimentCB.getSelectedItem() + "/" + hole.toString() + "-" + ((String)selectSedimentCB.getSelectedItem()) + ".txt";
			}
			try {
				if ( ((String)selectSedimentCB.getSelectedItem()).equals("GRAIN") ) {
					if ( !isGrain ) {
						sedimentGraph.setGrain(true);
						sedimentGraph.setXR(false);
						isGrain = true;
						isXR = false;
						grainPts = new GrainBRGTable(testGraphURLString);
						sedimentGraph.setPoints( new GrainBRGTable(testGraphURLString), 0 );
						sedimentGraph.repaint();
					}
					else {
						sedimentGraph.setGrain(true);
						sedimentGraph.setXR(false);
						isXR = false;
						grainPts = new GrainBRGTable(testGraphURLString);
						sedimentGraph.setPoints( new GrainBRGTable(testGraphURLString), 0 );
						sedimentGraph.repaint();
					}
				}
				else if ( ((String)selectSedimentCB.getSelectedItem()).startsWith("XR") ) {
					if ( !isXR ) {
						sedimentGraph.setXR(true);
						sedimentGraph.setGrain(false);
						isXR = true;
						isGrain = false;
						xrPts = new XRBRGTable(testGraphURLString);
						sedimentGraph.setPoints( xrPts, 0 );
						sedimentGraph.repaint();
					}
					else {
						sedimentGraph.setXR(true);
						sedimentGraph.setGrain(false);
						isGrain = false;
						xrPts = new XRBRGTable(testGraphURLString);
						sedimentGraph.setPoints( xrPts, 0 );
						sedimentGraph.repaint();
					}
				}
				else {
					if ( isGrain || isXR ) {
						sedimentGraph.setGrain(false);
						sedimentGraph.setXR(false);
						isGrain = false;
						isXR = false;
						sedimentPts = new DensityBRGTable(testGraphURLString);
						sedimentGraph.setPoints( sedimentPts, 0 );
						sedimentGraph.repaint();
					}
					else {
						sedimentGraph.setGrain(false);
						sedimentGraph.setXR(false);
						sedimentPts = new DensityBRGTable(testGraphURLString);
						sedimentGraph.setPoints( sedimentPts, 0 );
						sedimentGraph.repaint();
					}
				}
			} catch (IOException ioe) {
				System.out.println("URL ERROR");
				ioe.printStackTrace();
				return;
			}
			columnCB.removeItemListener(this);
			columnCB.removeAllItems();
			selectSedimentPanel.remove(columnCB);
			columnCB.setEnabled(false);
			String[] columnHeadings = null;
			if ( isXR ) {
				columnHeadings = xrPts.headings;
			}
			else {
				columnHeadings = sedimentPts.headings;
			}
			for ( int i = 1; i < columnHeadings.length; i++ ) {
				columnCB.addItem(columnHeadings[i]);
			}
			if ( columnCB.getItemCount() > 1 && !((String)selectSedimentCB.getSelectedItem()).equals("GRAIN") ) {
				selectSedimentPanel.add(columnCB);
				columnCB.addItemListener(this);
				columnCB.setEnabled(true);
			}
			sedimentGraph.repaint();
		}
	}

	protected static ImageIcon createImageIcon(String path) {
		URL imgURL;
		try {
			imgURL = URLFactory.url(path);
			if (imgURL != null) {
				return new ImageIcon(imgURL);
			} else {
				System.err.println("Couldn't find file: " + path);
				return null;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void initializeChooseColumnDialog( String input ) {
		String tempURLString = null;
		if ( hole.getLeg() < 100 ) {
			tempURLString = DSDP.DSDP_PATH + input + "/" + hole.toString() + "-" + input + ".txt";
		}
		else {
			tempURLString = DSDP.DSDP_PATH + "ODP_" + input + "/" + hole.toString() + "-" + input + ".txt";
		}
		try {
			DensityBRGTable tempTable = new DensityBRGTable(tempURLString);
			selectSedimentDialog = new JDialog(dsdpF);
			selectSedimentDialog.setTitle("Select Column");
			selectSedimentDialog.addWindowListener(this);
			
			selectAddColumnCB = new JComboBox();
			selectAddColumnCB.addItem("Select Column");
			for ( int i = 1; i < tempTable.headings.length; i++ ) {
				selectAddColumnCB.addItem(tempTable.headings[i]);
			}
			selectAddColumnCB.addItemListener(this);
			selectSedimentDialog.add(selectAddColumnCB);
			selectSedimentDialog.pack();
			selectSedimentDialog.setSize( 195, 100 );
			selectSedimentDialog.setLocation( selectSedimentDialogX, selectSedimentDialogY );
			selectSedimentDialog.setVisible(true);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void actionPerformed(ActionEvent ae) {
		if ( ae.getSource().equals(initialReport) || ae.getSource().equals(scientificResults) ) {
			String urlString = null;
			String legString;
			legString = Integer.toString(hole.getLeg());
			if ( hole.getLeg() < 10 ) {
				legString = "0" + legString;
			}
			if ( hole.getLeg() < 100 ) {
				urlString = "http://www.deepseadrilling.org/" + legString + "/dsdp_toc.htm"; 
			}
			else if ( hole.getLeg() >= 100 && hole.getLeg() < 300 ) {
				if ( ae.getSource().equals(initialReport) ) {
					urlString = "http://www-odp.tamu.edu/publications/" + legString + "_IR/" + legString + "TOC.HTM";
				}
				else if ( ae.getSource().equals(scientificResults) ) {
					urlString = "http://www-odp.tamu.edu/publications/" + legString + "_SR/" + legString + "TOC.HTM";
				}
			}
			else if ( hole.getLeg() >= 300 ) {
				if ( ae.getSource().equals(initialReport) ) {
					urlString = "http://publications.iodp.org/preliminary_report/" + legString;
				}
				else if ( ae.getSource().equals(scientificResults) ) {
					if ( hole.getLeg() == 303 || hole.getLeg() == 306 ) {
						urlString = "http://publications.iodp.org/proceedings/303_306/30306toc.htm";
					}
					else if ( hole.getLeg() == 304 || hole.getLeg() == 305 ) {
						urlString = "http://publications.iodp.org/proceedings/304_305/30405toc.htm";
					}
					else if ( hole.getLeg() == 309 || hole.getLeg() == 312 ) {
						urlString = "http://publications.iodp.org/proceedings/309_312/30912toc.htm";
					}
					else {
						urlString = "http://publications.iodp.org/proceedings/" + legString + "/" + legString + "toc.htm";
					}
				}
			}
			if ( urlString != null ) {
				BrowseURL.browseURL(urlString);
			}
		}

//		***** GMA 1.6.8: Display sediment window
		else if ( ae.getSource().equals(sedimentTB) ) {
			if ( sedimentTB.isSelected() ) {
				map.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				map.getTopLevelAncestor().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				if ( sedimentDialog == null ) {
					sedimentURLString = null;
					initializeSedimentDisplay();
				} else {
					sedimentDialog.setVisible(true);
				}

				if ( sedimentViewDataDialog != null ) {
					sedimentViewDataDialog.setVisible(true);
				}

				map.getTopLevelAncestor().setCursor(Cursor.getDefaultCursor());
				map.setCursor(Cursor.getDefaultCursor());
			}
			else {
				if ( sedimentDialog != null ) {
					sedimentDialog.setVisible(false);
				}
				if ( sedimentViewDataDialog != null ) {
					sedimentViewDataDialog.setVisible(false);
				}
			}
		}
//		**** GMA 1.6.8
		else if ( ae.getSource().equals(stratigraphicRangesTB) ) {
			if ( stratChart != null ) {
				stratChart.dispose();
			}
			if ( stratigraphicRangesTB.isSelected() ) {
				stratChart = new StratigraphicRangeChart(dsdpF);
				stratChart.stratImage.addMouseMotionListener(this);
				stratChart.setStratTB(stratigraphicRangesTB);
			}
		}

		else if ( ae.getSource().equals(saveB) ) {
			saveB.setEnabled(false);
			saveDialog = new JDialog(dsdpF,"Save");
			saveDialog.addWindowListener(this);
			ButtonGroup bg = new ButtonGroup();
			JPanel rbPanel = new JPanel(new GridLayout(0,1));
			saveAllRB = new JRadioButton("Save All", true);
			bg.add(saveAllRB);
			rbPanel.add(saveAllRB);
			saveSelectionRB = new JRadioButton("Save Selection");
			bg.add(saveSelectionRB);
			rbPanel.add(saveSelectionRB);
			saveDialog.getContentPane().add(rbPanel,"Center");
			JPanel okCancelPanel = new JPanel();
			okB = new JButton("OK");
			okB.addActionListener(this);
			cancelB = new JButton("Cancel");
			cancelB.addActionListener(this);
			okCancelPanel.add(okB);
			okCancelPanel.add(cancelB);
			saveDialog.getContentPane().add(okCancelPanel,"South");
			saveDialog.pack();
			saveDialog.setLocation(dsdpF.getLocation().x + 500,dsdpF.getLocation().y + 100);
			saveDialog.setVisible(true);
		}

		else if ( ae.getSource().equals(okB) ) {
			if ( saveAllRB.isSelected() ) {
				saveDialog.dispose();
				exportExcel();
			}
			else {
				saveDialog.dispose();
				exportSelectExcel();
			}
			saveB.setEnabled(true);
		}

		else if ( ae.getSource().equals(cancelB) ) {
			saveDialog.dispose();
			saveB.setEnabled(true);
		}

		else if ( ae.getSource().equals(addSedimentGraphB) ) {
			if ("".equals(selectSedimentCB.getSelectedItem()))
				return;

			addSedimentGraphB.setEnabled(false);

			selectSedimentDialog = new JDialog(dsdpF);
			selectSedimentDialog.addWindowListener(this);
			selectSedimentDialog.setTitle("Select a Sediment");
			selectAddSedimentCB = new JComboBox();
			selectAddSedimentCB.addItem("Select Sediment");
			for ( int i = 0; i < selectSedimentCB.getItemCount(); i++ ) {
				selectAddSedimentCB.addItem(selectSedimentCB.getItemAt(i));
			}
			selectSedimentDialog.add(selectAddSedimentCB);
			selectAddSedimentCB.addItemListener(this);
			selectSedimentDialog.pack();
			selectSedimentDialog.setLocation( MapApp.getAnchorX()+500, MapApp.getAnchorY()+500 );
			selectSedimentDialog.setSize( 195, 100 );
			selectSedimentDialog.setVisible(true);

		}

		else if ( ae.getSource().equals(sedimentPhotoTB) ) {
			if ( sedimentPhotoTB.isSelected() ) {
				sedimentPhotoDisp.setBGC(Color.cyan);
			}
			else {
				sedimentPhotoDisp.setBGC(Color.white);
			}
			sedimentPhotoDisp.repaint();
		}

		else if ( ae.getSource().equals(sedimentCoreDescriptionsTB) ) {
			if ( sedimentCoreDescriptionsTB.isSelected() ) {
				coreDescriptionsDisp.setBGC(Color.green);
			}
			else {
				coreDescriptionsDisp.setBGC(Color.white);
			}
			coreDescriptionsDisp.repaint();
		}

		else if ( ae.getSource().equals(sedimentViewDataTB) ) {
			if ("".equals(selectSedimentCB.getSelectedItem())) {
				sedimentViewDataTB.setSelected(false);
				return;
			}

			if ( sedimentViewDataTB.isSelected() ) {
				if ( sedimentViewDataDialog == null ) {
					sedimentViewDataDialog = new JDialog(dsdpF);
					sedimentViewDataDialog.addWindowListener(this);
					sedimentViewDataDialog.setTitle("View Data - ID: " + hole.name);
					sedimentViewDataTextArea = new JTextArea();
					sedimentViewDataSP = new JScrollPane(sedimentViewDataTextArea);

					try {
						String tempURL = DSDP_PATH + (String)selectSedimentCB.getSelectedItem() + "/" + hole.toString() + "-" + (String)selectSedimentCB.getSelectedItem() + ".txt";
						if ( hole.getLeg() >= 100 ) {
							tempURL = DSDP_PATH + "ODP_" + (String)selectSedimentCB.getSelectedItem() + "/" + hole.toString() + "-" + (String)selectSedimentCB.getSelectedItem() + ".txt";
						}
						BufferedReader in = new BufferedReader( new InputStreamReader( ( URLFactory.url( tempURL ) ).openStream() ) );
						String s;
						while ( ( s = in.readLine() ) != null ) {
							sedimentViewDataTextArea.append( s + "\n" );
						}
						in.close();
					} catch (MalformedURLException mue) {
						mue.printStackTrace();
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
					sedimentViewDataTextArea.setCaretPosition(0);
					sedimentViewDataDialog.getContentPane().add(sedimentViewDataSP);

					sedimentViewDataDialog.pack();
					sedimentViewDataDialog.setLocation( MapApp.getAnchorX()+300, MapApp.getAnchorY()+300 );
					sedimentViewDataDialog.setSize( new Dimension( 800, 500 ) );
					sedimentViewDataDialog.setVisible(true);
				}
				else {
					sedimentViewDataDialog.setVisible(true);
				}
			}
			else {
				disposeSedimentViewDataDialog();
			}
		}

		else if ( ae.getSource().equals(sedimentSaveDataB) ) {
			if ("".equals(selectSedimentCB.getSelectedItem())) {
				return;
			}

			JFileChooser sedimentSaveDialog = new JFileChooser(System.getProperty("user.dir"));
			File sedimentSaveFile = new File( hole.name + "-" + (String)selectSedimentCB.getSelectedItem() + ".txt" );
			sedimentSaveDialog.setSelectedFile(sedimentSaveFile);

			int c = JOptionPane.NO_OPTION;
			Point p = new Point( MapApp.getAnchorX()+300, MapApp.getAnchorY()+300 );
			sedimentSaveDialog.setLocation(p);

			while ( c == JOptionPane.NO_OPTION ) {
				c = sedimentSaveDialog.showSaveDialog(sedimentDialog);

				if ( c == JFileChooser.CANCEL_OPTION ) {
					return;
				}
				else if ( c == JFileChooser.APPROVE_OPTION ) {
					sedimentSaveFile = sedimentSaveDialog.getSelectedFile();
					if ( sedimentSaveFile.exists() ) {
						int c2 = JOptionPane.showConfirmDialog(MapApp.anchor, "File exists, Overwrite?");
						if (c2 == JOptionPane.OK_OPTION ) {
							break;
						}
						else {
							c = JOptionPane.NO_OPTION;
						}
					}
				}
			}
			try {
				String tempURL = DSDP_PATH + (String)selectSedimentCB.getSelectedItem() + "/" + hole.toString() + "-" + (String)selectSedimentCB.getSelectedItem() + ".txt";
				if ( hole.getLeg() >= 100 ) {
					tempURL = DSDP_PATH + "ODP_" + (String)selectSedimentCB.getSelectedItem() + "/" + hole.toString() + "-" + (String)selectSedimentCB.getSelectedItem() + ".txt";
				}
				BufferedReader in = new BufferedReader( new InputStreamReader( ( URLFactory.url( tempURL ) ).openStream() ) );
				FileWriter out = new FileWriter(sedimentSaveFile);
				String s;
				while ( ( s = in.readLine() ) != null ) {
					out.write( s + "\n" );
				}
				in.close();
				out.flush();
				out.close();
				MapApp.sendLogMessage("Saving_or_Downloading&portal=Ocean Floor Drilling&what=sediment_data&hole=" + hole.name + "&sediment=" + (String)selectSedimentCB.getSelectedItem());
			} catch (MalformedURLException mue) {
				mue.printStackTrace();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

		else if ( ae.getSource().equals(closeSedimentB) ) {
			sedimentDialog.dispose();
			if ( sedimentTB != null && sedimentTB.isVisible() ) {
				sedimentTB.setSelected(false);
			}
			return;
		}
	}

	public void keyPressed(KeyEvent ke) {
		if ( ke.getSource().equals(sedimentPhotoDisp) ) {
			if ( ke.getKeyCode() == KeyEvent.VK_CONTROL ) {
				if ( ke.isShiftDown() ) {
					sedimentPhotoDisp.setCursor(Cursors.ZOOM_OUT());
				}
				else {
					sedimentPhotoDisp.setCursor(Cursors.ZOOM_IN());
				}
			}
		}
		else if ( ke.getSource().equals(coreDescriptionsDisp) ) {
			if ( ke.getKeyCode() == KeyEvent.VK_CONTROL ) {
				if ( ke.isShiftDown() ) {
					coreDescriptionsDisp.setCursor(Cursors.ZOOM_OUT());
				}
				else {
					coreDescriptionsDisp.setCursor(Cursors.ZOOM_IN());
				}
			}
		}
		else if ( ke.getSource().equals(coreColorSedDisplay) ) {
			if ( ke.getKeyCode() == KeyEvent.VK_CONTROL ) {
				if ( ke.isShiftDown() ) {
					coreColorSedDisplay.setCursor(Cursors.ZOOM_OUT());
				}
				else {
					coreColorSedDisplay.setCursor(Cursors.ZOOM_IN());
				}
			}
		}
		else if ( ke.getSource().equals(sedimentAgeDisp) ) {
			if ( ke.getKeyCode() == KeyEvent.VK_CONTROL ) {
				if ( ke.isShiftDown() ) {
					sedimentAgeDisp.setCursor(Cursors.ZOOM_OUT());
				}
				else {
					sedimentAgeDisp.setCursor(Cursors.ZOOM_IN());
				}
			}
		}
		else if ( ke.getSource().equals(sedimentCoreDisp) ) {
			if ( ke.getKeyCode() == KeyEvent.VK_CONTROL ) {
				if ( ke.isShiftDown() ) {
					sedimentCoreDisp.setCursor(Cursors.ZOOM_OUT());
				}
				else {
					sedimentCoreDisp.setCursor(Cursors.ZOOM_IN());
				}
			}
		}
		else if ( ke.getSource().equals(ageDisp) ) {
			if ( ke.getKeyCode() == KeyEvent.VK_CONTROL ) {
				if ( ke.isShiftDown() ) {
					ageDisp.setCursor(Cursors.ZOOM_OUT());
				}
				else {
					ageDisp.setCursor(Cursors.ZOOM_IN());
				}
			}
		}
		else if ( ke.getSource().equals(coreDisp) ) {
			if ( ke.getKeyCode() == KeyEvent.VK_CONTROL ) {
				if ( ke.isShiftDown() ) {
					coreDisp.setCursor(Cursors.ZOOM_OUT());
				}
				else {
					coreDisp.setCursor(Cursors.ZOOM_IN());
				}
			}
		}
		else if ( ke.getSource().equals(coreDescDisp) ) {
			if ( ke.getKeyCode() == KeyEvent.VK_CONTROL ) {
				if ( ke.isShiftDown() ) {
					coreDescDisp.setCursor(Cursors.ZOOM_OUT());
				}
				else {
					coreDescDisp.setCursor(Cursors.ZOOM_IN());
				}
			}
		}
		else if ( ke.getSource().equals(coreColorDisplay) ) {
			if ( ke.getKeyCode() == KeyEvent.VK_CONTROL ) {
				if ( ke.isShiftDown() ) {
					coreColorDisplay.setCursor(Cursors.ZOOM_OUT());
				}
				else {
					coreColorDisplay.setCursor(Cursors.ZOOM_IN());
				}
			}
		}
		else if ( ke.getSource().equals(fossilDisp) ) {
			if ( ke.getKeyCode() == KeyEvent.VK_CONTROL ) {
				if ( ke.isShiftDown() ) {
					fossilDisp.setCursor(Cursors.ZOOM_OUT());
				}
				else {
					fossilDisp.setCursor(Cursors.ZOOM_IN());
				}
			}
		}
	}

	public void keyReleased(KeyEvent ke) {
		if ( ke.getSource().equals(sedimentPhotoDisp) ) {
			sedimentPhotoDisp.setCursor(Cursor.getDefaultCursor());
		}
		else if ( ke.getSource().equals(coreDescriptionsDisp) ) {
			coreDescriptionsDisp.setCursor(Cursor.getDefaultCursor());
		}
		else if ( ke.getSource().equals(coreColorSedDisplay) ) {
			coreColorSedDisplay.setCursor(Cursor.getDefaultCursor());
		}
		else if ( ke.getSource().equals(sedimentAgeDisp) ) {
			sedimentAgeDisp.setCursor(Cursor.getDefaultCursor());
		}
		else if ( ke.getSource().equals(sedimentCoreDisp) ) {
			sedimentCoreDisp.setCursor(Cursor.getDefaultCursor());
		}
		else if ( ke.getSource().equals(coreDisp) ) {
			coreDisp.setCursor(Cursor.getDefaultCursor());
		}
		else if ( ke.getSource().equals(coreDescDisp) ) {
			coreDescDisp.setCursor(Cursor.getDefaultCursor());
		}
		else if ( ke.getSource().equals(coreColorDisplay) ) {
			coreDescDisp.setCursor(Cursor.getDefaultCursor());
		}
		else if ( ke.getSource().equals(ageDisp) ) {
			ageDisp.setCursor(Cursor.getDefaultCursor());
		}
		else if ( ke.getSource().equals(fossilDisp) ) {
			fossilDisp.setCursor(Cursor.getDefaultCursor());
		}
	}

	public void keyTyped(KeyEvent ke) {
	}

	public void mouseClicked(MouseEvent me) {
		if (me.getSource().equals(coreDescriptionsDisp)
				|| me.getSource().equals(sedimentPhotoDisp)
				|| me.getSource().equals(sedimentCoreDisp)
				|| me.getSource().equals(sedimentAgeDisp)
				|| me.getSource().equals(ageDisp)
				|| me.getSource().equals(coreDisp)
				|| me.getSource().equals(coreDescDisp)
				|| me.getSource().equals(fossilDisp)
				|| me.getSource().equals(coreColorDisplay)
				|| me.getSource().equals(coreColorSedDisplay)) {
			if ( me.isControlDown() && !adjustment ) {
				if ( me.isShiftDown() ) {
					if ( me.getSource().equals(coreDescriptionsDisp) || 
							me.getSource().equals(sedimentPhotoDisp) || 
							me.getSource().equals(sedimentCoreDisp) || 
							me.getSource().equals(sedimentAgeDisp)  || 
							me.getSource().equals(coreColorSedDisplay)) {
						if (sedimentGraph != null) {
							setZScale( -1 * sedimentGraph.getYScale(), sedimentGraph, me.getY() );
							adjustGraphs( sedimentGraph.getZoom() / 2, me.getY(), "PhotoDisplay" );
						}
					}
					else {
						setZScale( 0.5 * coreDisp.getZScale(), coreDisp, me.getY() );
						adjustGraphs( ageModel.graph.getZoom() / 2, me.getY(), "PhotoDisplay" );
					}
				}
				else {
					if ( me.getSource().equals(coreDescriptionsDisp) ||
							me.getSource().equals(sedimentPhotoDisp) || 
							me.getSource().equals(sedimentCoreDisp) || 
							me.getSource().equals(sedimentAgeDisp)  || 
							me.getSource().equals(coreColorSedDisplay)) {
						if (sedimentGraph != null) {
							setZScale( -1 * sedimentGraph.getYScale(), sedimentGraph, me.getY() );
							adjustGraphs( sedimentGraph.getZoom() * 2, me.getY(), "PhotoDisplay" );
						}
					}
					else {
						setZScale( 2.0 * coreDisp.getZScale(), coreDisp, me.getY() );
						adjustGraphs( ageModel.graph.getZoom() * 2, me.getY(), "PhotoDisplay" );
					}
				}
			}
		}

		else if ( !me.getSource().equals(sedimentGraph) && 
				me.getSource() instanceof org.geomapapp.util.DensityXYGraph ) {
			DensityXYGraph tempGraph = (DensityXYGraph)me.getSource();
			Rectangle r = tempGraph.getVisibleRect();
			if ( me.getX() + tempGraph.getX() > tempGraph.getX() + r.width - 10 && me.getX() + tempGraph.getX() < tempGraph.getX() + r.width && me.getY() > r.getMinY() && me.getY() < r.getMinY() + 10 ) {
				sedimentBox.remove(tempGraph);
//				sedimentBox.add(tempGraph);
				sedimentDialog.pack();
//				System.out.println("Width: " + sedimentDialog.getWidth() + "\tHeight: " + sedimentDialog.getHeight());
				sedimentDialog.setSize( sedimentDialog.getWidth(), 600 );
				sedimentDialog.setVisible(true);
				tempGraph.removeMouseListener(this);
				tempGraph = null;
			}
		}

		else if ( me.getSource().equals(chronosLabel) ) {
			String inputURLString = "http://portal.chronos.org/gridsphere/gridsphere?cid=xqe_beta#search=chronos.portal.taxon;execute=chronos.portal.taxon?taxon=";
			String [] speciesName = speciesLabel.getText().split("\\s");
			for ( int i = 0; i < speciesName.length; i++ ) {
				if ( !speciesName[i].equals("") ) {
					String temp = speciesName[i];
					String temp1 = temp.substring(0,1);
					String temp2 = temp.substring(1);
					temp1 = temp1.toUpperCase();
					temp2 = temp2.toLowerCase();
					temp = temp1 + temp2;
					inputURLString += ( temp + "%20" );
				}
			}
			inputURLString = inputURLString.substring(0,inputURLString.lastIndexOf("%20"));
			inputURLString += "&";
			//BrowseURL.browseURL("https://doi.org/10.1130/GES00009.1");
			BrowseURL.browseURL(inputURLString);
		}

		else if ( me.getSource().equals(paleoBioLabel) ) {
			String inputURLString = "http://paleodb.org/cgi-bin/bridge.pl?action=checkTaxonInfo&taxon_name=";
			String [] speciesName = speciesLabel.getText().split("\\s");
//			for ( int i = 0; i < speciesName.length; i++ ) {
//				inputURLString += ( speciesName[i] + "%20" );
//			}
//			inputURLString = inputURLString.substring(0,inputURLString.lastIndexOf("%20"));
			inputURLString += speciesName[0];
			BrowseURL.browseURL(inputURLString);
		}

		else if ( me.getSource().equals(iSpeciesLabel) ) {
			String inputURLString = "https://www.ispecies.org/?q=";
			//String [] speciesName = speciesLabel.getText().split("\\s");
//			for ( int i = 0; i < speciesName.length; i++ ) {
//				inputURLString += ( speciesName[i] + "+" );
//			}
//			inputURLString = inputURLString.substring(0,inputURLString.lastIndexOf("+"));
			//inputURLString += speciesName[0];
			//inputURLString += "&submit=Go";
			inputURLString += speciesLabel.getText().replaceAll("\\s+", "%20");
			BrowseURL.browseURL(inputURLString);
		}
	}

	public void mouseEntered(MouseEvent me) {
	}

	public void mouseExited(MouseEvent me) {
		if ( me.getSource().equals(sedimentPhotoDisp) ) {
			sedimentPhotoDisp.setCursor(Cursor.getDefaultCursor());
		}
	}

	public void mousePressed(MouseEvent me) {
	}

	public void mouseReleased(MouseEvent me) {
	}

	public void propertyChange(PropertyChangeEvent pcevt) {
		if ( pcevt.getSource().equals(speciesLabel) ) {
			if ( speciesLabel.getText().matches("\\D+") ) {
				chronosLabel.setText("See Chronos");
//				chronosLabel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
				paleoBioLabel.setText("See Paleo-bio");
//				paleoBioLabel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
				iSpeciesLabel.setText("See iSpecies");
//				iSpeciesLabel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
			}
			else {
				chronosLabel.setText("");
	//			chronosLabel.setBorder(BorderFactory.createEmptyBorder());
				paleoBioLabel.setText("");
	//			paleoBioLabel.setBorder(BorderFactory.createEmptyBorder());
				iSpeciesLabel.setText("");
	//			iSpeciesLabel.setBorder(BorderFactory.createEmptyBorder());
			}
		}
	}
}
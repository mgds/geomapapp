package org.geomapapp.image;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.filechooser.FileFilter;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.geom.XYZ;
import org.geomapapp.grid.Grid2D;
import org.geomapapp.grid.Grid2DOverlay;
import org.geomapapp.grid.GridDialog;
import org.geomapapp.image.GridRenderer.RenderResult;
import org.geomapapp.util.Histogram;
import org.geomapapp.util.Icons;
import org.geomapapp.util.PreBinnedDataHistogram;
import org.geomapapp.util.ScaleHistogram;
import org.geomapapp.util.SimpleBorder;

import haxby.map.MapApp;
import haxby.map.MapTools;
import haxby.proj.Projection;
import haxby.util.FilesUtil;

public class RenderingTools extends JPanel implements ActionListener {
	protected GridRenderer renderer;
	protected Grid2DOverlay grid;
	protected Vector undo;
	protected Vector redo;
//	JFrame dialog;
	protected JTextField colorInterval;
	protected ScaleHistogram slopeHistogram;
	protected ColorHistogram scaler;
	protected ColorModPanel mod;
	protected SunTool sun;
	protected VETool ve;
	protected PropertyChangeListener propL;
	protected ActionListener palListener,
							stateChange;
	protected JMenu paletteMenu;
	protected JDialog morePalDialog;
	protected Hashtable<String, Palette> palettes;
	protected Palette oceanPalette,
						landPalette,
						defaultPalette,
						currentPalette;
	protected Histogram landHist,
						oceanHist,
						defaultHist,
						landSlopeHist,
						oceanSlopeHist,
						bothSlopeHist;
	protected PaletteTool paletteTool;
	protected PersTool pers;
	protected JDialog slopeDialog; 
	protected float colorIntervalDefault;

//	***** GMA 1.6.4: TESTING
	protected JFrame parentGridDialog;
	protected JPanel palPanel;
	private JToggleButton slope_and_z_toggleB,
							normalize,
							unnormalize;
	public JToggleButton contourB,
							oceanB,
							landB,
							bothB,
							continuousB,
							discreteB,
							customizeColor,
							lock_and_unlockB;
	public JDialog customColorDialog;
	private JTextArea gridStatistics;
//	public JToggleButton customizeColor = new JToggleButton( new ImageIcon("C://Documents and Settings/Andrew K. Melkonian/My Documents/GeoMapApp Pictures/customizecolor2.png"), false );
//	public JToggleButton customizeColor = new JToggleButton("Customize Color", false);
	protected boolean fitToStDev = true;
	protected boolean autoNormalize = false;
	protected boolean initPhase = true;
	private KeyListener copyPaste;

	private float landSlopeMean = Float.NaN;
	private float oceanSlopeMean  = Float.NaN;
	private float totalSlopeMean = Float.NaN;
	private float landMeanZ = Float.NaN;
	private float oceanMeanZ = Float.NaN;
	private float totalMeanZ = Float.NaN;
	private double landArea = Double.NaN;
	private double oceanArea = Double.NaN;

	private int[] oceanSlopeDist;
	private int[] landSlopeDist;
	private int[] totalSlopeDist;

	private String dString = "Discrete: ";
	private String cString = "Continuous: ";
	
	private File root = org.geomapapp.io.GMARoot.getRoot();

	public RenderingTools() {
		this( (Grid2DOverlay)null );
	}
	public RenderingTools(Grid2DOverlay grid) {
		super( new BorderLayout() );
		this.grid = grid;
		init();
		setGrid( grid );
		initPhase = false;
		oceanSlopeDist = landSlopeDist = totalSlopeDist = new int[90];
	}
	public void setGrid(Grid2DOverlay grid) {
		this.grid = grid;
		if (grid != null)
			setNewGrid();
	}

	// GMA 1.6.4
	public void setParentFrame(JFrame inputParentFrame) {
		parentGridDialog = inputParentFrame;
	}

	public void setBackground( int argb ) {
		renderer.setBackground( argb );
	}
	public void setNewGrid() {
		setNewGrid(false);
	}
	public void setNewGrid(boolean isGMRT) {
		pers.setGrid( grid, parentGridDialog == null ? false
				: parentGridDialog.getContentPane().isAncestorOf(pers), isGMRT);

		oceanSlopeDist = landSlopeDist = totalSlopeDist = new int[90];
		oceanSlopeHist = landSlopeHist = bothSlopeHist = null;

		calculateGridStatistics();

		if( grid.hasLand() ) {
			try {
				landHist = new Histogram(grid.getGrid(), 
					grid.getLandMask(),
					true,
					200);
				landB.setEnabled(true);
			} catch(Exception ex) {
				landHist = null;
				landB.setEnabled(false);
			}
		} else {
			landHist = null;
			landB.setEnabled(false);
		}
		if( grid.hasOcean() ) {
			try {
				oceanHist = new Histogram(grid.getGrid(), 
					grid.getLandMask(),
					false,
					200);
				oceanB.setEnabled(true);
			} catch(Exception ex) {
				oceanB.setEnabled(false);
				oceanHist = null;
			}
		} else {
			oceanB.setEnabled(false);
			oceanHist = null;
		}

		JToggleButton sb;
		if (landB.isSelected())
			sb = landB;
		else if (oceanB.isSelected())
			sb = oceanB;
		else 
			sb = bothB;

		if (grid.toString().equals(GridDialog.DEM) || grid.toString().equals(GridDialog.DEV) ) {
			if (!bothB.isSelected()) {
				if (!oceanB.isEnabled()) {
					landB.setSelected(true);
					sb = landB;
				}
				if (!landB.isEnabled()) {
					oceanB.setSelected(true);
					sb = oceanB;
				}
			}
		}

		if( !sb.isEnabled() ) {
			bothB.setSelected(true);
			sb = bothB;
		}

		try {
			defaultHist = new Histogram(grid.getGrid(), 200);
			if( sb==oceanB ) {
				scaler.setHist(oceanHist);
				scaler.setPalette(oceanPalette);
				paletteTool.setDefaultPalette(oceanPalette);
				currentPalette = oceanPalette;
			} else if( sb==landB ) {
				scaler.setHist(landHist);
				scaler.setPalette(landPalette);
				paletteTool.setDefaultPalette(landPalette);
				currentPalette = landPalette;
			} else {
				scaler.setHist(defaultHist);
				scaler.setPalette(defaultPalette);
				currentPalette = defaultPalette;
				paletteTool.setDefaultPalette( defaultPalette);
			}

			if (autoNormalize) {
				normalize();
			} else if (fitToStDev) {
				fitToStdDev(1);
			} else {
				haxby.map.XMap map = grid.getMap();
				if ( map != null && map.getApp() instanceof MapApp && ((MapApp)(map.getApp())).getMapTools().getGridDialog().isLoaded() ) {
					((MapApp)map.getApp()).initializeColorScale();
				}
				gridImage();
			}
		} catch(Exception ex) {
//			defaultHist = null;
//			bothB.setEnabled(false);
		}
	}

	private void calculateGridStatistics() {
		double landArea = 0;
		double oceanArea = 0;
		Grid2D g2d = grid.getGrid();
		Grid2D.Boolean mask = grid.getLandMask();

		if (g2d == null || grid.getLandMask() == null) return;

		Rectangle bounds = g2d.getBounds();
		MapProjection proj = g2d.getProjection();

		float landZ, oceanZ;
		int landN, oceanN;
		landZ = oceanZ = 0;
		landN = oceanN = 0;

		for (int y = 0; y < mask.getBounds().height; y++) {
			int yy = y + bounds.y;
			Point2D refXY1 = proj.getRefXY(bounds.x, yy - .5);
			Point2D refXY2 = proj.getRefXY(bounds.x, yy + .5);

			XYZ r1 = XYZ.LonLat_to_XYZ(refXY1);
			XYZ r2 = XYZ.LonLat_to_XYZ(refXY2);
			double angle = Math.acos( r1.dot(r2) );
			double dist1 = Projection.major[0] * angle/1000.;

			refXY1 = proj.getRefXY(bounds.x, yy);
			refXY2 = proj.getRefXY(bounds.x + 1, yy);
			r1 = XYZ.LonLat_to_XYZ(refXY1);
			r2 = XYZ.LonLat_to_XYZ(refXY2);
			angle = Math.acos( r1.dot(r2) );
			double dist2 = Projection.major[0] * angle/1000.;

			double area = dist1 * dist2;

			for (int x = 0; x < bounds.width; x++) {
				int xx = x + bounds.x;
				double z = g2d.valueAt(xx, yy);
				if (!Double.isNaN(z)) {
					if (mask.booleanValue(xx, yy)) {
						landZ += z;
						landN++;
						landArea += area;
					}
					else {
						oceanZ += z;
						oceanN++;
						oceanArea += area;
					}
				}
			}
		}

		this.landArea  = landArea;
		this.oceanArea = oceanArea;
		this.landMeanZ  = landZ / landN;
		this.oceanMeanZ = oceanZ / oceanN;
		this.totalMeanZ = (landZ + oceanZ) / (landN + oceanN);

		updateGridStatistics();
	}

	private void updateGridStatistics() {
		NumberFormat intFormat = NumberFormat.getIntegerInstance();

		String infoDisplay = "Area (L, O, T) km\u00B2 :\t" +
			intFormat.format(landArea) + "\t" +
			intFormat.format(oceanArea) + "\t" +
			intFormat.format(landArea + oceanArea) + "\n";

		NumberFormat oneDec = NumberFormat.getNumberInstance();
		oneDec.setMaximumFractionDigits(1);

		String landMeanZstr = Float.isNaN(landMeanZ) ? "NaN" : oneDec.format(landMeanZ);
		String oceanMeanZstr = Float.isNaN(oceanMeanZ) ? "NaN" : oneDec.format(oceanMeanZ);
		String totalMeanZstr = Float.isNaN(totalMeanZ) ? "NaN" : oneDec.format(totalMeanZ);

		infoDisplay += "Mean Elev (L, O, T) m :\t" +
			landMeanZstr + "\t" +
			oceanMeanZstr + "\t" +
			totalMeanZstr + "\n";

		String landSlopeMeanStr = Float.isNaN(landSlopeMean) ? "NaN" : oneDec.format(landSlopeMean);
		String oceanSlopeMeanStr = Float.isNaN(oceanSlopeMean) ? "NaN" : oneDec.format(oceanSlopeMean);
		String totalSlopeMeanStr = Float.isNaN(totalSlopeMean) ? "NaN" : oneDec.format(totalSlopeMean);

		infoDisplay += "Mean Slope (L, O, T) \u00B0 :\t" +
			landSlopeMeanStr + "\t" +
			oceanSlopeMeanStr + "\t" +
			totalSlopeMeanStr;

		gridStatistics.setText(infoDisplay);
	}

	protected void init() {
		pers = new PersTool( grid);
		propL = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				fire(evt);
			}
		};
		stateChange = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				fire( new PropertyChangeEvent(
					this, 
					"STATE_CHANGE", 
					(Object)(new Integer(0)), 
					(Object)(new Integer(1)) 
					));
			}
		};

		mod = new ColorModPanel( Color.blue.getRGB());

		copyPaste = new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if( !e.isControlDown() )return;
				int k = e.getKeyCode();
				if( k==KeyEvent.VK_C )copy();
				else if( k==KeyEvent.VK_V )paste();
			}
		};
		scaler = new ColorHistogram();
		scaler.addKeyListener( copyPaste );

		slopeHistogram = new ScaleHistogram();
		slopeHistogram.setRange(new double[]{0,45});

		oceanPalette = new Palette(Palette.OCEAN);
		landPalette = new Palette(Palette.LAND);
		defaultPalette = new Palette(Palette.HAXBY);
		renderer = new GridRenderer(defaultPalette,
				1.,
				1000.,
				new XYZ(1.,1.,1.));
		scaler.setPalette( defaultPalette );
		currentPalette = defaultPalette;
		
		//fill the palettes hashtable with all available palettes
		palettes = new Hashtable<String, Palette>();
		for( int k=0 ; k<Palette.resources.length ; k++) {
			Palette p = new Palette(k);
			palettes.put( p.toString(), p);
		}
		
		paletteTool = new PaletteTool( currentPalette, mod );
		paletteTool.setDefaultPalette( defaultPalette);
		paletteTool.addPropertyChangeListener(propL);

		sun = new SunTool(new XYZ(-1., 1., 1.));
		sun.addPropertyChangeListener(propL);

		ve = new VETool(1.);
		ve.setVE( currentPalette.getVE() );
		ve.addPropertyChangeListener(propL);

		Border border = BorderFactory.createEmptyBorder(1,1,1,1);
		Border lineBorder = BorderFactory.createLineBorder(Color.black);
		SimpleBorder sborder = new SimpleBorder(true);

		oceanB = new JToggleButton(Icons.getIcon(Icons.OCEAN, false) );
		oceanB.setSelectedIcon(Icons.getIcon(Icons.OCEAN, true));
		oceanB.setDisabledIcon( new ImageIcon(
			GrayFilter.createDisabledImage(
				Icons.getIcon(Icons.OCEAN, false).getImage())));
		oceanB.setBorder( border );
		oceanB.addActionListener( stateChange);
		oceanB.setToolTipText("Modify Ocean Palette");

		landB = new JToggleButton(Icons.getIcon(
					Icons.LAND, false));
		landB.setSelectedIcon(Icons.getIcon(Icons.LAND, true));
		landB.setDisabledIcon( new ImageIcon(
			GrayFilter.createDisabledImage(
				Icons.getIcon(Icons.LAND, false).getImage())));
		landB.setBorder( border );
		landB.addActionListener( stateChange);
		landB.setToolTipText("Modify Land Palette");

		bothB = new JToggleButton(Icons.getIcon(
					Icons.OCEAN_LAND, false));
		bothB.setSelectedIcon(Icons.getIcon(Icons.OCEAN_LAND, true));
		bothB.setBorder( border );
		bothB.addActionListener( stateChange);
		if ( !grid.toString().equals(GridDialog.DEM) && 
				!grid.toString().equals(GridDialog.DEV) &&
				!grid.toString().equals(GridDialog.TOPO_9) &&
				!grid.toString().equals(GridDialog.NASA_ELEV_MODEL)) {
			bothB.setSelected(true);
		}
		bothB.setToolTipText("Modify Default Palette");

		pers.setRenderer( renderer, bothB );

		ButtonGroup group = new ButtonGroup();
		group.add(oceanB);
		group.add(landB);
		group.add(bothB);

		JButton back = new JButton(Icons.getIcon(Icons.BACK, false));
		back.setPressedIcon(Icons.getIcon(Icons.BACK, true));
		back.setBorder( border );
		back.addActionListener( this );
		back.setToolTipText("Undo");

		JButton forward = new JButton(Icons.getIcon(Icons.FORWARD, false));
		forward.setPressedIcon(Icons.getIcon(Icons.FORWARD, true));
		forward.setBorder( border );
		forward.addActionListener( this );
		forward.setToolTipText("Redo");

		normalize = new JToggleButton(Icons.getIcon(Icons.NORMALIZE, false));
		normalize.setPressedIcon(Icons.getIcon(Icons.NORMALIZE, true));
		normalize.setBorder( border );
		normalize.addActionListener( this );
		normalize.setActionCommand("normalize");
		normalize.setToolTipText("Normalize histogram / Auto-normalize on");

		unnormalize = new JToggleButton(Icons.getIcon(Icons.UNNORMALIZE, false));
		unnormalize.setPressedIcon(Icons.getIcon(Icons.UNNORMALIZE, true));
		unnormalize.setBorder( border );
		unnormalize.addActionListener( this );
		unnormalize.setActionCommand( "unnormalize" );
		unnormalize.setToolTipText("Reset histogram / Auto-normalize off");

		ButtonGroup normalizeButtonGroup = new ButtonGroup();
		normalizeButtonGroup.add(normalize);
		normalizeButtonGroup.add(unnormalize);

		continuousB = new JToggleButton(Icons.getIcon(Icons.CONTINUOUS, false));
		continuousB.setSelectedIcon( Icons.getIcon(Icons.CONTINUOUS, true));
		continuousB.setSelected( true);
		continuousB.setBorder( border );
		continuousB.addActionListener( stateChange);
		continuousB.setToolTipText("Continuous Color Change");

		discreteB = new JToggleButton(Icons.getIcon(Icons.DISCRETE, false));
		discreteB.setSelectedIcon( Icons.getIcon(Icons.DISCRETE, true));
		discreteB.setBorder( border );
		discreteB.setText("Continuous: ");
		discreteB.addActionListener( stateChange);
		discreteB.setToolTipText("<html>Discrete Color Change <br> " +
						"at Specified Interval </html>");

		colorInterval = new JTextField( "0", 3 );
		colorInterval.setColumns(2);
		colorInterval.setPreferredSize( new Dimension(15, 15) );
		colorInterval.setSize( new Dimension(15, 15));
		colorInterval.setToolTipText("Set default coloring interval");
		/* Defaulting color interval is -1 continuous. Try retrieving
		 * the user entered interval and Catch the number exception.
		 * Update the gridImage and scaler with the new interval.
		 * If the interval is less then 0 switch to continuous toggle.
		 */
		colorInterval.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double d = -1.;
				try{
				d = Double.parseDouble(colorInterval.getText());
				}catch (NumberFormatException nfe){
					colorInterval.setText("1000");
					nfe.printStackTrace();
				}
				if(d<=0.){
					colorInterval.setText("0");
					continuousB.setSelected( true);
					discreteB.setText(cString);
				}else if(d>0){
					discreteB.setSelected(true);
					discreteB.setText(dString);
				}
				gridImage();
				scaler.repaint();
			}
		});

		group = new ButtonGroup();
		group.add( continuousB );
		group.add( discreteB );

		// Contour icon button on Global and Contributed Grids
		contourB = new JToggleButton(Icons.getIcon(Icons.CONTOUR, false));
		contourB.setSelectedIcon(Icons.getIcon(Icons.CONTOUR, true));
		contourB.setBorder( border );
		contourB.addActionListener( this );
		contourB.setActionCommand( "contour" );
		contourB.setToolTipText("Set Contour Interval and Range");

//		***** GMA 1.6.4

		// Create the Slope / Z Toggle Button
		slope_and_z_toggleB = new JToggleButton(Icons.getIcon(Icons.E_S, false));
		slope_and_z_toggleB.setSelectedIcon(Icons.getIcon(Icons.S_E, false));
		slope_and_z_toggleB.setBorder(null);
		slope_and_z_toggleB.setToolTipText("Toggle Elevation / Slope Distributions");
		slope_and_z_toggleB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				toggleSlopeZ();
			}
		});

		// Create Lock Button To keep palette color inplace when zoom or panning
		lock_and_unlockB = new JToggleButton(Icons.getIcon(Icons.UNLOCK, false));
		lock_and_unlockB.setSelectedIcon(Icons.getIcon(Icons.LOCK, false));
		lock_and_unlockB.setToolTipText("Click to lock value range of palette");
		lock_and_unlockB.setBorder(null);
		lock_and_unlockB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(lock_and_unlockB.isSelected()) {
					lock_and_unlockB.setToolTipText("Click to unlock value range of palette");
					fitToStDev = false;
				} else {
					lock_and_unlockB.setToolTipText("Click to lock value range of palette");
					fitToStDev = true;
					fitToStdDev(1);
				}
			}
		});

		JPanel tools = new JPanel( new BorderLayout());
		Box toolBox = Box.createVerticalBox();
		Box box = Box.createVerticalBox();

/*		***** GMA 1.6.4: Remove "undo" and "redo" buttons, currently they do not do anything
		box.add(back);
		box.add(forward);
		Box toolBox = Box.createHorizontalBox();
		Box box = Box.createHorizontalBox();

		try {
			customizeColor = new JToggleButton( new ImageIcon( URLFactory.url("http://www.ldeo.columbia.edu/~akm/images/customizecolor2.PNG") ), false );
			customizeColor = new JToggleButton( new ImageIcon("C://Documents and Settings/Andrew K. Melkonian/My Documents/GeoMapApp Pictures/customizecolor2.png"), false );
			customizeColor.setSelectedIcon( new ImageIcon( URLFactory.url("http://www.ldeo.columbia.edu/~akm/images/customizecolorselected.PNG") ) );
		} catch (MalformedURLException mue) {
			mue.printStackTrace();
		}
		customizeColor.setBorder( border );

		customizeColor.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				if ( palPanel != null ) {
					if ( customizeColor.isSelected() ) {
						palPanel.add(mod, "West");
						add(paletteTool.getButtonPanel(), "South");
						palPanel.repaint();
						repaint();
						setVisible(true);
					}
					else {
						palPanel.remove(mod);
						remove(paletteTool.getButtonPanel());
						palPanel.repaint();
						repaint();
						setVisible(true);
					}
				}
			}
		});
		box.add(customizeColor);*/

		box.setBorder(sborder);

		box.add(normalize);
		box.add(unnormalize);
		box.add(oceanB);
		box.add(landB);
		box.add(bothB);

		Box hBox = Box.createHorizontalBox();
		hBox.add( continuousB );
		hBox.add( discreteB );
		hBox.add( colorInterval );

		hBox.add(contourB);
		hBox.add(slope_and_z_toggleB);
		toolBox.add(box);

		JMenuBar bar = new JMenuBar();
		paletteMenu = new JMenu("Palettes Menu");
		palListener = new ActionListener() {
	
			public void actionPerformed(ActionEvent evt) {
				if (evt.getSource() instanceof JMenuItem) {
					JMenuItem selectedItem = (JMenuItem) evt.getSource();
					if (selectedItem.getText().equals("Create a Custom Palette") ) {
	//					***** GMA 1.6.4: TESTING
						customColorDialog = new JDialog(parentGridDialog, "Create a Custom Palette");
						
					    // create some css from the label's font
						JLabel label = new JLabel();
						Font font = label.getFont();
					    StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
					    style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
					    style.append("font-size:" + font.getSize() + "pt;");
					    
						String instr = "<html> <body style=\"" + style + "\"><p>"
								+ "Create a custom palette using the sliders <br>"
								+ "to chose <u>H</u>ue, <u>S</u>aturation and <u>B</u>rightness <br>"
								+ "values for 17 different levels.</p>"
								+ "<p> You can use the diamonds on the x-axis<br>"
								+ "of the color histogram to set "
								+ "the <br>distribution of the levels within the palette.</p> "
								+ "</body></html>";
						JTextPane instrTP = new JTextPane();
						instrTP.setContentType("text/html");
						instrTP.setText(instr);
						instrTP.setEditable(false);
						customColorDialog.getContentPane().add(instrTP, "Center");
						customColorDialog.getContentPane().add(mod, "West");
						customColorDialog.getContentPane().add(paletteTool.getButtonPanel(), "South");
						ColorComponent cc2 = null;
						cc2 = (ColorComponent)paletteTool.buttons.get(0);
						customColorDialog.pack();
						customColorDialog.setVisible(true);
	/*					float[] range = getPalette().getRange();
						Palette p = getPalette();
						p = (Palette)p.clone();
						p.setRange( range[0], range[1]);
						if( bothB.isSelected() ) defaultPalette = p;
						else if(oceanB.isSelected()) oceanPalette = p;
						else landPalette = p;
						scaler.setPalette( p );
						paletteTool.setDefaultPalette( p );
						ve.setVE( p.getVE() );
						scaler.repaint();
						gridImage();*/
	//					***** GMA 1.6.4
					} else if (selectedItem.getText().equals("More Palettes...") ) {
						createMorePalDialog();
					}
					if (selectedItem.getText().equals("Export the Color Bar") ) {
						haxby.map.XMap map = grid.getMap();
						if ( map != null && map.getApp() instanceof MapApp) {
							try {
								((MapApp)map.getApp()).getColorScale().saveColorScale();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					
					}
					else {
						changePalette(selectedItem.getText());
					}
	
					if (evt.getActionCommand().equals("Loaded Palette") ) {
						if(!lock_and_unlockB.isSelected()) {
							lock_and_unlockB.doClick();
						}
					}
				}
				if (evt.getSource() instanceof JButton) {
					if (evt.getActionCommand().equals("Close")) {
						morePalDialog.dispose();
					}
					else {
						changePalette(evt.getActionCommand());
						
						//get the buttons from the morePalDialog and reset the text to black
						JRootPane c1 = (JRootPane) morePalDialog.getComponent(0);
						JLayeredPane c2 = (JLayeredPane) c1.getComponent(1);
						JPanel c3 = (JPanel) c2.getComponent(0);
						Component[] buttons = c3.getComponents();
						for (Component c4 : buttons) {
							if (c4 instanceof JButton) {
								((JButton) c4).setForeground(Color.BLACK);
							}
						}
						//set the text in the selected button to red
						JButton b = (JButton) evt.getSource();
						b.setForeground(Color.RED);					
					}
				}
				// GMA 1.6.6: Re-initialize color scale to match current grid
				haxby.map.XMap map = grid.getMap();
				if ( map != null && map.getApp() instanceof MapApp) {
					((MapApp)map.getApp()).initializeColorScale();
				}
			}
		};

		// Items in Palette Menu
		JMenuItem item;
		for( int k=0 ; k<Palette.basicResources.length ; k++) {
			Palette p = new Palette(k);
			item = paletteMenu.add(new JMenuItem(
				p.toString(), p.getIcon()));
			item.addActionListener(palListener);
			palettes.put( p.toString(), p);
		}
		item = paletteMenu.add(new JMenuItem("More Palettes...", Icons.getIcon(Icons.PALETTE_ICON, false)));
		item.addActionListener(palListener);
		paletteMenu.addSeparator();
		item = paletteMenu.add(new JMenuItem("Create a Custom Palette"));
		item.addActionListener(palListener);
		item = paletteMenu.add(new JMenuItem("Save a Custom Palette"));
		item.addActionListener(palListener);
		item = paletteMenu.add(new JMenuItem("Delete a Custom Palette"));
		item.addActionListener(palListener);
		item = paletteMenu.add(new JMenuItem("Import a Custom Palette"));
		item.addActionListener(palListener);
		item = paletteMenu.add(new JMenuItem("Export a Custom Palette"));
		item.addActionListener(palListener);
		paletteMenu.addSeparator();
		item = paletteMenu.add(new JMenuItem("Export the Color Bar"));
		item.addActionListener(palListener);
		loadMyPalettes();
		paletteMenu.setBorder( sborder );

		bar.add( paletteMenu );
		tools.add(toolBox);

		JPanel palettePanel = new JPanel(new BorderLayout());
		palettePanel.add(lock_and_unlockB, "Center");
		palettePanel.add(bar, "East");
		palettePanel.setOpaque(true);

//		***** GMA 1.6.4: TESTING
//		tools.add( bar, "East" );
//		JPanel palPanel = new JPanel(new BorderLayout());
//		palPanel.add(tools, "North");

		JPanel veP = ve.getPanel();
		hBox.add(veP);
		hBox.add( palettePanel );
		hBox.setBorder(sborder);
		palPanel = new JPanel(new BorderLayout());
		palPanel.add( hBox, "North" );
		palPanel.add(tools, "West");

		scaler.setBorder(lineBorder);
		scaler.addPropertyChangeListener(propL);
		scaler.setToolTipText("<html>Drag vertical grey lines to rescale color bar<br>Drag triangles to stretch or compress color scale</html>");
		palPanel.add(scaler);

		JPanel gsPanel = new JPanel(new GridLayout(1,0));
		// Create the JTextArea for our info read-out
		gridStatistics = new JTextArea(3,3);
		gridStatistics.setBorder( BorderFactory.createLineBorder(Color.black, 2));
		gridStatistics.setEditable(true);
		gridStatistics.setToolTipText("Land (L), Ocean (O), Total (T)");
		gsPanel.add(gridStatistics);
		palPanel.add(gsPanel,"South");

		palPanel.setBorder(sborder);
		add( palPanel );

		JPanel panel = new JPanel( new BorderLayout() );
		//Add Sun Illumination panel
		JPanel sp = sun.getPanel();
		sp.setBorder(BorderFactory.createTitledBorder("Sun Illumination"));
		panel.add(sp);

		panel.setBorder(sborder);
		add(panel, "East");

		colorInterval.setPreferredSize( new Dimension( 15, 15 ) );
		colorInterval.setSize( new Dimension( 15, 15 ) );

		bothB.setEnabled(true);
	}

	private void createMorePalDialog() {
		morePalDialog = new JDialog(parentGridDialog, "More Palettes");
		//morePalFrame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE);
		morePalDialog.setLayout(new GridLayout(0,5));
		JButton paletteBtn;
		for( int k=0 ; k<Palette.resources.length ; k++) {
			Palette p = new Palette(k);		
			paletteBtn = new JButton(p.toString(), p.getIcon());
			paletteBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
			paletteBtn.setHorizontalTextPosition(SwingConstants.CENTER);
			paletteBtn.addActionListener(palListener);
			
			morePalDialog.add(paletteBtn);

		}
		JButton closeBtn = new JButton("Close");
		closeBtn.addActionListener(palListener);
		morePalDialog.add(closeBtn);
		
		morePalDialog.pack();
		morePalDialog.setVisible(true);
	}
	
	protected void toggleSlopeZ() {
		// Show the slope histograms
		if (slope_and_z_toggleB.isSelected()) {
			palPanel.remove(scaler);
			updateSlopePanel();

			palPanel.add(slopeHistogram);
			slopeHistogram.invalidate();
		} else { // show the z histograms
			palPanel.remove(slopeHistogram);

			if( bothB.isSelected() ) {
				scaler.setPalette( defaultPalette);
				scaler.setHist( defaultHist);
				paletteTool.setDefaultPalette( defaultPalette );
				ve.setVE( defaultPalette.getVE() );
			} else if( landB.isSelected() ) {
				scaler.setPalette( landPalette);
				scaler.setHist( landHist);
				paletteTool.setDefaultPalette( landPalette);
				ve.setVE( landPalette.getVE() );
			} else if( oceanB.isSelected() ) {
				scaler.setPalette( oceanPalette);
				scaler.setHist( oceanHist);
				paletteTool.setDefaultPalette( oceanPalette);
				ve.setVE( oceanPalette.getVE() );
			}
			palPanel.add(scaler);
		}
		palPanel.revalidate();
		palPanel.repaint();
	}
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("normalize"))
			normalizePressed();
		else if (e.getActionCommand().equals("unnormalize"))
			unnormalizePressed();
		else if (e.getActionCommand().equals("contour"))
			contour();
	}

	private void unnormalizePressed(){
		autoNormalize = false;
		fitToStDev = false;
		unnormalize();
	}

	private void normalizePressed(){
		autoNormalize = true;
		fitToStDev = false;
		normalize();
	}

	public void removeColorPanel() {
		if ( palPanel != null ) {
			palPanel.remove(mod);
			remove(paletteTool.getButtonPanel());
			palPanel.repaint();
			repaint();
			setVisible(true);
		}
	}

	public void addColorPanel() {
		if ( palPanel != null ) {
			palPanel.add(mod, "West");
			add(paletteTool.getButtonPanel(), "South");
			palPanel.repaint();
			repaint();
			setVisible(true);
		}
	}

	protected void fire(PropertyChangeEvent evt) {
		if( evt.getPropertyName().equals("ancestor") ) return;
		if( evt.getPropertyName().equals("border") ) return;

		if( evt.getSource()==sun || evt.getSource()==ve ) {
			gridImage();
			haxby.map.XMap map = grid.getMap();
			if ( map != null && map.getApp() instanceof MapApp) {
				MapApp mapApp = (MapApp) map.getApp();
				MapTools mt = mapApp.getMapTools();
				if (mt.getGridDialog().isLoaded()) 
						mapApp.initializeColorScale();
			}
			return;
		}
		if( evt.getPropertyName().equals("STATE_CHANGE") ) {
			if (slope_and_z_toggleB.isSelected() && oceanSlopeDist != landSlopeDist){
				updateSlopePanel();
			}else {
				if( bothB.isSelected() ) {
					scaler.setPalette( defaultPalette);
					scaler.setHist( defaultHist);
					paletteTool.setDefaultPalette( defaultPalette );
					ve.setVE( defaultPalette.getVE() );
				} else if( landB.isSelected() ) {
					scaler.setPalette( landPalette);
					scaler.setHist( landHist);
					paletteTool.setDefaultPalette( landPalette);
					ve.setVE( landPalette.getVE() );
				} else if( oceanB.isSelected() ) {
					scaler.setPalette( oceanPalette);
					scaler.setHist( oceanHist);
					paletteTool.setDefaultPalette( oceanPalette);
					ve.setVE( oceanPalette.getVE() );
				}
				gridImage();
				scaler.repaint();
			}
			/* Interaction with continuous and discrete
			 * toggle action will change the button text label
			 * and value in the field.
			 */
			if(continuousB.isSelected()){
				colorInterval.setText("0");
				discreteB.setText(cString);
				continuousB.setSelected( true);
			}else if(discreteB.isSelected()){
				float [] zRange2 = scaler.getPalette().getRange();
				float range = zRange2[1] - zRange2[0];
				float rangeDefault = getRangeDefault(range);
				Object d = rangeDefault;
				colorInterval.setText(d.toString());
				discreteB.setText(dString);
				discreteB.setSelected( true);
			}

//			***** GMA 1.6.6: Re-initialize color scale to match current grid
			haxby.map.XMap map = grid.getMap();
				if ( map != null && map.getApp() instanceof MapApp && ((MapApp)(map.getApp())).getMapTools().getGridDialog().isLoaded() ) {
				((MapApp)map.getApp()).initializeColorScale();
			}

		} else if( evt.getPropertyName().equals("RANGE_CHANGED") ||
				evt.getPropertyName().startsWith("APPLY")) {
			autoNormalize = false;
			fitToStDev = false;

//			***** GMA 1.6.6: Re-initialize color scale to match current grid
			haxby.map.XMap map = grid.getMap();
			if ( map != null && map.getApp() instanceof MapApp && ((MapApp)(map.getApp())).getMapTools().getGridDialog().isLoaded() ) {
					((MapApp)map.getApp()).initializeColorScale();
			}

			scaler.repaint();
			gridImage();
		}

//		***** GMA 1.6.4: Automatically adjust color when the palette is modified
		else if ( evt.getSource() == mod || evt.getSource() == paletteTool ) {
			scaler.repaint();
			repaint();
			gridImage();

//			***** GMA 1.6.6: Re-initialize color scale to match current grid
				haxby.map.XMap map = grid.getMap();
				if ( map != null && map.getApp() instanceof MapApp) {
					((MapApp)map.getApp()).initializeColorScale();
				}
		}
	}

	void applyPalettes( Palettes pals ) {
	}
	protected void loadMyPalettes() {
		if(root==null)return;
		File dir = new File(root, "lut");
		if( !dir.exists())return;
		File[] files = dir.listFiles(new java.io.FileFilter() {
			public boolean accept(File file) {
				return file.getName().endsWith(".lut");
			}
		});
		for( int k=0 ; k<files.length ; k++) {
			try {
				Palette p = new Palette(files[k]);
				JMenuItem item = paletteMenu.add(new JMenuItem(
					p.toString(), p.getIcon()));
				item.setActionCommand("Loaded Palette");
				item.addActionListener(palListener);
				palettes.put( p.toString(), p);
			} catch(Exception ex) {
			}
		}
	}

	protected void loadAPalette(String importPath, String lutGMAPath) {
		File dir = new File(importPath);
		if( !dir.exists())return;

			try {
				Palette p = new Palette(dir);
				JMenuItem item = paletteMenu.add(new JMenuItem(
					p.toString(), p.getIcon()));
				item.setActionCommand("Loaded Palette");
				item.addActionListener(palListener);
				FilesUtil.copyFile(importPath, lutGMAPath);
				palettes.put( p.toString(), p);
			} catch(Exception ex) {
			}
	}

	public void savePalette() {
		Palette p = null;
		try {
			p = getPalette().savePalette(parentGridDialog);
		} catch(Exception ex) {
			ex.printStackTrace();
			return;
		}
		if( p==null )return;
		String name = p.toString();
		JMenuItem item = paletteMenu.add(new JMenuItem(
			p.toString(), p.getIcon()));
		item.addActionListener(palListener);
		palettes.put( p.toString(), p);
	}

	// Removes the chosen palette from system.
	public Palette deletePalette() {
		Palette deletePalette = null;
		try {
			deletePalette = getPalette().deletePalette(parentGridDialog);
		} catch(Exception ex) {
			ex.printStackTrace();
			return null;
		}
		if( deletePalette==null )return null;
		palettes.remove( deletePalette.toString());
		return deletePalette;
	}

	// Export the chosen custom palette file to desktop.
	public File exportPaletteFile() {
		File exportPaletteFile = null;
		try {
			exportPaletteFile = getPalette().exportPalette(parentGridDialog);
		} catch(Exception ex) {
			ex.printStackTrace();
			return null;
		}
		if( exportPaletteFile==null )return null;
		return exportPaletteFile;
	}

	// Import the chosen custom palette file from desktop in GMA and save to GMA files dir.
	public void importPaletteFile() {
			JFileChooser chooser = MapApp.getFileChooser();
			FileFilter filter = new FileFilter(){
				public boolean accept(File pathname) {
					if (pathname.isDirectory()) return true;
					return pathname.getName().endsWith(".lut");
				}
			
				public String getDescription() {
					return "Color Palette (.lut)";
				}
			};
			chooser.setFileFilter(filter);
			int ok = chooser.showOpenDialog(parentGridDialog);
			int confirm = JOptionPane.NO_OPTION;

			boolean lutNameExists = false;
			if (ok == chooser.CANCEL_OPTION) {
				return;
			} else if(ok == chooser.APPROVE_OPTION) {
				File importPalette = chooser.getSelectedFile();
				if (importPalette.exists()) {
					if(root==null)return;
					File dirLUT = new File(root, "lut");
					if(!dirLUT.exists()) {
						dirLUT.mkdir();
					}

					File[] filesLUT = dirLUT.listFiles(new java.io.FileFilter() {
						public boolean accept(File file) {
							return file.getName().endsWith(".lut");
						}
					});

					for( int k=0 ; k<filesLUT.length ; k++) {
						if(filesLUT[k].getName().matches(importPalette.getName())) {
							lutNameExists = true;
						}
					}
					if(lutNameExists) {
						confirm = JOptionPane.showConfirmDialog(parentGridDialog, "File exists in GeoMapApp, Overwrite?");
						if (confirm == JOptionPane.CANCEL_OPTION) return;
					} else {
						File loadGMALUT = new File(dirLUT, importPalette.getName());
						loadAPalette(importPalette.getPath(), loadGMALUT.getPath());
					}
				}
			}
		return;
	}

	protected void changePalette(String name) {
		if(name.equals("Save a Custom Palette")) {
			savePalette();
			return;
		} else if(name.equals("Delete a Custom Palette")) {
			// Obtain the palette the user wants to delete
			Palette pToRemove = deletePalette();
			if (pToRemove!=null) {
				// Find the index of that item and remove it
				for(int i=0; i<paletteMenu.getItemCount(); i++) {
					if(paletteMenu.getItem(i) !=null){
						if(paletteMenu.getItem(i).getText().contains(pToRemove.toString())) {
							paletteMenu.remove(i);
							paletteMenu.updateUI();
						}
					}
				}
			}
			return;
		} else if(name.equals("Export a Custom Palette")) {
			File fToExport = exportPaletteFile();
			if (fToExport!=null) {
				JFileChooser chooser = MapApp.getFileChooser();
				chooser.setSelectedFile(fToExport);
				int ok = chooser.showSaveDialog(parentGridDialog);
				int confirm = JOptionPane.NO_OPTION;
				if (ok == chooser.CANCEL_OPTION) {
					return;
				} else if(ok == chooser.APPROVE_OPTION) {
					File palette = chooser.getSelectedFile();
						try {
							if (palette.exists()) {
								confirm = JOptionPane.showConfirmDialog(parentGridDialog, "File exists, Overwrite?");
								if (confirm == JOptionPane.CANCEL_OPTION) return;
							}
							palette.createNewFile();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					FilesUtil.copyFile(fToExport.getPath(), palette.getPath());
				}
			}
			return;
		} else if(name.equals("Import a Custom Palette")) {
			importPaletteFile();
			return;
		}
		float[] range = getPalette().getRange();
		Palette p_orig = (Palette)palettes.get(name);
		Palette p = (Palette)p_orig.clone();
		float[] range2 = p.getRange();
		p.setRange( range2[0], range2[1]);


		if(bothB.isSelected()) {
			defaultPalette = p;
		} else if(oceanB.isSelected()) {
			oceanPalette = p;
		}else {
			landPalette = p;
		}
		scaler.setPalette( p );
		paletteTool.setDefaultPalette( p );
		
		//reset range to that previously used
		if (fitToStDev) {
			fitToStdDev(1);
		} else {
			p.setRange(range[0], range[1]);
		}
		
		ve.setVE( p.getVE() );
		scaler.repaint();
		gridImage();

	}

	public void setPalette(String name) {
		Palette p = (Palette)palettes.get(name);
		if (p == null)
			System.err.println("Palette not found: " + name);

		float[] range = getPalette().getRange();
		p = (Palette)p.clone();
		p.setRange( range[0], range[1]);
		if( bothB.isSelected() ) defaultPalette = p;
		else if(oceanB.isSelected()) oceanPalette = p;
		else landPalette = p;
		ve.setVE( p.getVE() );
	}

	public void setDefaultPalette (Palette p) {
		defaultPalette = p;
	}
	public void setLandPalette (Palette p) {
		landPalette = p;
	}
	public void setOceanPalette (Palette p) {
		oceanPalette = p;
	}
	public void setPalette(Palette p) {
		p = (Palette)p.clone();
		if( bothB.isSelected() ) defaultPalette = p;
		else if(oceanB.isSelected()) oceanPalette = p;
		else landPalette = p;
		scaler.setPalette(p);
		ve.setVE( p.getVE() );
	}

	public void fitToStdDev(double n) {
		if( landB.isSelected() ) {
			double mean = landHist.getMean();
			double stddev = landHist.getStdDev();

			landPalette.setRange((float)(mean - stddev * n),
					(float)(mean + stddev * n));
		} else if ( oceanB.isSelected() ){
			double mean = oceanHist.getMean();
			double stddev = oceanHist.getStdDev();
			oceanPalette.setRange((float)(mean - stddev * n),
					(float)(mean + stddev * n));
		} else {
			double mean = defaultHist.getMean();
			double stddev = defaultHist.getStdDev();

			defaultPalette.setRange((float)(mean - stddev * n),
					(float)(mean + stddev * n));
		}
		scaler.repaint();
		repaint();
		gridImage();

//		***** GMA 1.6.6: Re-initialize color scale to match current grid
		haxby.map.XMap map = grid.getMap();

		if ( map != null ) {
			if ( map.getApp() instanceof MapApp && ((MapApp)(map.getApp())).getMapTools().getGridDialog().isLoaded() ) {
				((MapApp)map.getApp()).initializeColorScale();
			}
		}
	}

	public void setRange(float[] range) {
		if( landB.isSelected() ) {
			landPalette.setRange(range[0], range[1]);
		} else if ( oceanB.isSelected() ){
			oceanPalette.setRange(range[0], range[1]);
		} else {
			defaultPalette.setRange(range[0], range[1]);
		}
		autoNormalize = false;
		fitToStDev = false;
	}
	// Set the default value of the discrete color interval according to dataRange
	public float getRangeDefault(float dataRange) {
		colorIntervalDefault = 100000;
		if((dataRange < 100000) && (dataRange > 50000)){
			colorIntervalDefault = 10000;
		}else if((dataRange <= 50000) && (dataRange > 20000)){
			colorIntervalDefault = 5000;
		}else if((dataRange <= 20000) && (dataRange > 100000)){
			colorIntervalDefault = 2000;
		}else if((dataRange <= 10000) && (dataRange > 5000)){
			colorIntervalDefault = 1000;
		}else if((dataRange <= 5000) && (dataRange > 2000)){
			colorIntervalDefault = 500;
		}else if((dataRange <= 2000) && (dataRange > 1000)){
			colorIntervalDefault = 200;
		}else if((dataRange <= 1000) && (dataRange > 500)){
			colorIntervalDefault = 100;
		}else if((dataRange <= 500) && (dataRange > 200)){
			colorIntervalDefault = 50;
		}else if((dataRange <= 200) && (dataRange > 100)){
			colorIntervalDefault = 20;
		}else if((dataRange <= 100) && (dataRange > 50)){
			colorIntervalDefault = 10;
		}else if((dataRange <= 50) && (dataRange > 20)){
			colorIntervalDefault = 5;
		}else if((dataRange <= 20) && (dataRange > 10)){
			colorIntervalDefault = 2;
		}else if((dataRange <= 10) && (dataRange > 5)){
			colorIntervalDefault = 1;
		}else if((dataRange <= 5) && (dataRange > 2)){
			colorIntervalDefault = (float) 0.5;
		}else if((dataRange <= 2) && (dataRange > 1)){
			colorIntervalDefault = (float) 0.2;
		}else if((dataRange <= 1) && (dataRange > 0.5)){
			colorIntervalDefault = (float) 0.1;
		}else if((dataRange <= 0.5) && (dataRange > 0.2)){
			colorIntervalDefault = (float) 0.05;
		}else if((dataRange <= 0.2) && (dataRange > 0.1)){
			colorIntervalDefault = (float) 0.02;
		}else if((dataRange <= 0.1) && (dataRange > 0)){
			colorIntervalDefault = (float) 0.01;
		}
		return colorIntervalDefault;
	}

	public void normalize() {
//		1.3.5: Variables added to introduce functionality to
//		determine and display appropriate intervals and 
//		min max values in the contour window
		float contourMin;
		float contourMax;
		float contourInt = 1000;

		if( landB.isSelected() ) {
			double[] r = landHist.getRange();

			if ( autoNormalize ) {
				landPalette.setRange((float)r[0], (float)r[1]);
			}

			contourMin = (float)r[0];
			contourMax = (float)r[1];
		} else if ( oceanB.isSelected() ){
			double[] r = oceanHist.getRange();

			if ( autoNormalize ) {
				oceanPalette.setRange((float)r[0], (float)r[1]);
			}

			contourMin = (float)r[0];
			contourMax = (float)r[1];
		} else {
			double[] r = defaultHist.getRange();

			if ( autoNormalize ) {
				defaultPalette.setRange((float)r[0], (float)r[1]);
			}
			//System.out.println("r[0]: " + (float)r[0] + "\nr[1]: " + (float)r[1]);
			contourMin = (float)r[0];
			contourMax = (float)r[1]; 
		}

//		***** Changed by A.K.M. 1.3.5 *****
//		Determine appropriate interval and set the text
//		in the contour window to contourMin, contourMax 
//		and contourInt

		if ((contourMax - contourMin) < 10){
			contourInt = 1;
		}else if ((contourMax - contourMin) < 50){
			contourInt = 5;
		}else if ((contourMax - contourMin) < 100){
			contourInt = 10;
		}else if ((contourMax - contourMin) < 500){
			contourInt = 50;
		}else if ((contourMax - contourMin) < 1000){
			contourInt = 100;
		}else if ((contourMax - contourMin) < 5000){
			contourInt = 500;
		}else if ((contourMax - contourMin) < 10000){
			contourInt = 1000;
		}

		colorInterval.setText(Integer.toString((int)contourInt));
//		***** Changed by A.K.M. 1.3.5 *****

		scaler.repaint();
		repaint();
		gridImage();

//		***** GMA 1.6.6: Re-initialize color scale to match current grid

		haxby.map.XMap map = grid.getMap();

		if ( map != null ) {
			if ( map.getApp() instanceof MapApp && ((MapApp)(map.getApp())).getMapTools().getGridDialog().isLoaded() ) {
				((MapApp)map.getApp()).initializeColorScale();
			}
		}
//		***** GMA 1.6.6
	}
	protected void unnormalize() {
		if( bothB.isSelected() ) {
			defaultPalette.resetRange();
		} else if( landB.isSelected() ) {
			landPalette.resetRange();
		} else {
			oceanPalette.resetRange();
		}
		scaler.repaint();
		gridImage();

//		***** GMA 1.6.6: Re-initialize color scale to match current grid
			haxby.map.XMap map = grid.getMap();
			if ( map != null && map.getApp() instanceof MapApp && ((MapApp)(map.getApp())).getMapTools().getGridDialog().isLoaded() ) {
				((MapApp)map.getApp()).initializeColorScale();
		}
//		***** GMA 1.6.6
	}

	public SunTool getSunTool() {
		return sun;
	}

	public XYZ getSun() {
		renderer.sunIllum = sun.isSunOn();
		return sun.getSun();
	}

	public double getVE() {
		renderer.sunIllum = sun.isSunOn();
		return ve.getVE();
	}

	public boolean isContourSelected() {
		return contourB.isSelected();
	}

//	1.3.5: Allows other classes to set the state of
//	the contour toggle button
	public void setContourTBUnselected() {
		contourB.setSelected(false);
	}

	public void setContourSelected(boolean tf) {
		contourB.setSelected(tf);
	}
	
	public Palette getPalette() {
		Palette pal = bothB.isSelected() ?
			defaultPalette :
			oceanB.isSelected() ?
				oceanPalette : landPalette;
		return pal;
	}
	public Palette[] getPalettes() {
		Palette[] pal = bothB.isSelected() ?
			new Palette[] {defaultPalette} :
			new Palette[] {oceanPalette, landPalette};
		return pal;
	}
	public Palette getDefaultPalette() {
		return defaultPalette;
	}
	public Palette getOceanPalette() {
		return oceanPalette;
	}
	public Palette getLandPalette() {
		return landPalette;
	}
	
	
	public boolean isPaletteContinuous() {
		return continuousB.isSelected();
	}
//	protected void initDialog() {
//	//	dialog = new JFrame(
//		dialog = new JDialog( 
//			(JFrame)grid.getMap().getTopLevelAncestor(), 
//			"Grid Rendering Tools");
//		tabs = new JTabbedPane(JTabbedPane.TOP);
//		tabs.add( "Rendering", this);
//		dialog.getContentPane().add(tabs);
//		JLabel label = new JLabel("Rendering tools are new "
//				+"and still under development - "
//				+"use with caution");
//		dialog.getContentPane().add(label, "South");
//		dialog.pack();
//		tabs.add( "3D", pers);
////		tabs.addChangeListener( new ChangeListener() {
////			public void stateChanged(ChangeEvent e) {
////				tabChange();
////			}
////		});
//	}
//	void tabChange() {
//		if( tabs.getSelectedComponent()==pers ) {
//			pers.update();
//		}
//	}
	public JPanel getTool(String toolName) {
		if( toolName.equals("color") ) {
			return this;
		} else {
			pers.update();
			return pers;
		}
	}

	public void gridImage() {
		renderer.setPalette( defaultPalette );
		renderer.setLandPalette( landPalette );
		renderer.setOceanPalette( oceanPalette );
		if(grid.toString().equals(GridDialog.GRAVITY)) {
			renderer.setVEFactor(100.);
			pers.setVEFactor(200.);
		} else if(grid.toString().equals(GridDialog.GRAVITY_18)) {
			renderer.setVEFactor(100.);
			pers.setVEFactor(200.);
		} else if (grid.toString().equals(GridDialog.GEOID)) {
			renderer.setVEFactor(5000.);
			pers.setVEFactor(5000.);
		} else {
			renderer.setVEFactor(1.);
			pers.setVEFactor(1.);
		}
		double d = -1.;

		/* If discrete is selected check the interval is properly 
		 * dealt with. The default of 1000 is set if continuous
		 * 0 is found or a number exception is made.
		 */
		if( discreteB.isSelected() ) {
			try {
				d = Double.parseDouble(colorInterval.getText());
				if(d<=0){
					d=1000;
					colorInterval.setText("1000");
				}
			} catch(NumberFormatException nfe) {
				d = 1000;
				colorInterval.setText("1000");
				nfe.printStackTrace();
			}
		}

		defaultPalette.setDiscrete( d );
		landPalette.setDiscrete( d );
		oceanPalette.setDiscrete( d );
		renderer.setSun(sun.getSun());
		getPalette().setVE( ve.getVE() );
	//	renderer.setVE( ve.getVE());

		renderer.sunIllum = sun.isSunOn();

		RenderResult renderResult = bothB.isSelected() ?
			renderer.gridImage( grid.getGrid() ) :
			renderer.gridImage( grid.getGrid(), grid.getLandMask());

		BufferedImage image = renderResult.image;

		updateSlopeData(renderResult);

		double scale = grid.getScale();
		double[] offsets = grid.getOffsets();
		grid.setImage(image, offsets[0], offsets[1], scale);
		grid.getMap().repaint();
	}

	private void updateSlopeData(RenderResult renderResult) {
		if (oceanSlopeDist != landSlopeDist) return;
		this.oceanSlopeDist = renderResult.oceanSlopesDist;
		this.landSlopeDist = renderResult.landSlopesDist;

		int[] allSlopes = new int[oceanSlopeDist.length + landSlopeDist.length];
		int oceanZ, landZ, totalZ;
		oceanZ = landZ = totalZ = 0;
		int oceanN, landN;
		oceanN = landN = 0;

		for (int theta = 0; theta < oceanSlopeDist.length; theta++) {
			allSlopes[theta] += oceanSlopeDist[theta];
			allSlopes[theta] += landSlopeDist[theta];

			oceanN += oceanSlopeDist[theta];
			landN += landSlopeDist[theta];

			oceanZ += oceanSlopeDist[theta] * theta;
			landZ += landSlopeDist[theta] * theta;
			totalZ += oceanSlopeDist[theta] * theta;
			totalZ += landSlopeDist[theta] * theta;
		}

		landSlopeMean = landN == 0 ? Float.NaN : 1f * landZ / landN;
		oceanSlopeMean = oceanN == 0 ? Float.NaN : 1f * oceanZ / oceanN;
		if (oceanN + landN != 0)
			totalSlopeMean = (landZ + oceanZ) / (oceanN + landN);

		this.totalSlopeDist = allSlopes;

		updateSlopePanel();
		updateGridStatistics();
	}

	private void updateSlopePanel() {
		if (slope_and_z_toggleB.isSelected()) {
			Histogram toShow;
			if (bothB.isSelected()) {
				if (bothSlopeHist == null)
					bothSlopeHist = new PreBinnedDataHistogram(this.totalSlopeDist);
				toShow = bothSlopeHist;
			}
			else if (landB.isSelected()) {
				if (landSlopeHist == null)
					landSlopeHist = new PreBinnedDataHistogram(this.landSlopeDist);
				toShow = landSlopeHist;
			}
			else {
				if (oceanSlopeHist == null)
					oceanSlopeHist = new PreBinnedDataHistogram(this.oceanSlopeDist);
				toShow = oceanSlopeHist;
			}
			slopeHistogram.setHist(toShow);
			palPanel.repaint();
		}
	}
	public void contour() {
		if( contourB.isSelected() ) {
			try {
				grid.contourGrid();
			} catch(Exception ex) {
				contourB.setSelected(false);
			}
		} else {
			grid.contour.setVisible(false);
			grid.getMap().repaint();
		}
	}
	public void showDialog() {
		if (parentGridDialog != null) parentGridDialog.setVisible(true);
	}
	void undo() {
		if( undo==null || undo.size()==0 )return;
		if( redo==null )redo=new Vector();
		redo.add( 0, undo.remove(0) );
		applyPalettes( (Palettes)redo.get(0) );
	}

	void redo() {
		if( redo==null || redo.size()==0 )return;
		if( undo==null )undo=new Vector();
		undo.add( 0, redo.remove(0) );
		applyPalettes( (Palettes)undo.get(0) );
	}

	protected void copy() {
		Palettes.clipboard = getCurrentPalettes();
	}

	protected void paste() {
		Palettes p = Palettes.clipboard;
		if( p==null )return;
		oceanPalette = (Palette)p.ocean.clone();
		landPalette = (Palette)p.land.clone();
		defaultPalette = (Palette)p.both.clone();
		if( Double.isNaN(p.colorInterval) ) {
			continuousB.setSelected(true);
		} else {
			discreteB.setSelected(true);
			colorInterval.setText(Double.toString(p.colorInterval));
		}
		int b = p.button;
		if( b==0&&(!oceanB.isEnabled()) ) b=1;
		else if( b==1&&(!landB.isEnabled()) ) b=0;
		sun.setSun( p.sun );
		JToggleButton t = b==0 ? oceanB
				: b==1 ? landB : bothB;
		t.doClick();
	}
	public Palettes getCurrentPalettes() {
		double d = Double.NaN;
		try {
			if( discreteB.isSelected() )d = Double.parseDouble(colorInterval.getText());
		} catch(Exception e) {
		}
		int button = oceanB.isSelected() ? 0 
					: landB.isSelected() ? 1 : 2;
		return new Palettes( (Palette)oceanPalette.clone(),
					(Palette)landPalette.clone(),
					(Palette)defaultPalette.clone(),
					sun.getSun(),
					d, button);
	}
	public void dispose() {
		// Remove Listeners
		paletteTool.removePropertyChangeListener(propL);
		sun.removePropertyChangeListener(propL);
		ve.removePropertyChangeListener(propL);
		scaler.removePropertyChangeListener(propL);

		oceanB.removeActionListener(stateChange);
		landB.removeActionListener(stateChange);
		bothB.removeActionListener(stateChange);
		continuousB.removeActionListener(stateChange);
		discreteB.removeActionListener(stateChange);

		scaler.removeKeyListener(copyPaste);

		contourB.removeActionListener(this);
		normalize.removeActionListener(this);
		unnormalize.removeActionListener(this);
		colorInterval.removeActionListener(this);

		propL = null;
		stateChange = null;
		copyPaste = null;
		palListener = null;

		paletteMenu.removeAll();
		pers.dispose();
		grid = null;

		removeAll();
		palPanel.removeAll();
		scaler.setHist(null);
		slopeHistogram.setHist(null);
		landHist = oceanHist = defaultHist = null;
		landSlopeHist = oceanSlopeHist = bothSlopeHist = null;
		oceanSlopeDist = landSlopeDist = totalSlopeDist = null;
	}

	public GridRenderer getRenderer() {
		return renderer;
	}

	public void setSunOn(boolean tf) {
		sun.sunOn.setSelected(tf);
		sun.sunOff.setSelected(!tf);
	}

	public static void main(String[] args) {
		RenderingTools tools = new RenderingTools();
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(tools);
		frame.pack();
		frame.setVisible(true);
	}
	public void setVE(float ve2) {
		ve.setVE(ve2);
	}
	
	public ColorHistogram getScaler() {
		return scaler;
	}
	
	public void setFitToStDev (boolean fit) {
		fitToStDev = fit;
	}
	
	public void setColorInterval(Float interval) {
		//set the text box
		colorInterval.setText(Float.toString(interval));
	}
	
	public void setDiscrete(boolean discrete) {
		discreteB.setSelected(discrete);
	}
	
	public boolean getSunIllum() {
		return renderer.sunIllum;
	}
	
	/*
	 * which palette is currently selected?
	 */
	public String whichPalette() {
		if (landB.isSelected())
			return "land";
		else if (oceanB.isSelected())
			return "ocean";
		else 
			return "both";
	}
	
	/*
	 * set buttons according to which palette we are viewing
	 */
	public void setWhichPalette(String which) {
		if (which == null) return;
		//set palette buttons
		if (which.equals("land")) {
			landB.setSelected(true);
		}
		else if (which.equals("ocean")) {
			oceanB.setSelected(true);
		}
		else {
			bothB.setSelected(true);
		}
			
		//set discrete/continuous buttons and color interval
		Float disc_interval = getPalette().getDiscrete();
		if (disc_interval != null) {
			if (disc_interval == -1.0){
				//continuous
				setColorInterval(1000f);
				setDiscrete(false);
			} else {
				//discrete
				setColorInterval(disc_interval);
				setDiscrete(true);
			}
		}
	}
}

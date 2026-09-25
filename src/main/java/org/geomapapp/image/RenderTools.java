package org.geomapapp.image;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileFilter;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.geomapapp.geom.XYZ;
import org.geomapapp.image.GridRenderer.RenderResult;
import org.geomapapp.util.Histogram;
import org.geomapapp.util.Icons;
import org.geomapapp.util.SimpleBorder;

public class RenderTools extends JPanel {
	GridRenderer renderer;
	GridImage grid;
	Vector undo;
	Vector redo;
	JDialog dialog;
	JToggleButton contourB;
	JTextField contourInterval;
	JToggleButton oceanB, landB, bothB;
	JToggleButton continuousB, discreteB;
	JTextField colorInterval;
	ColorHistogram scaler;
	ColorModPanel mod;
	SunTool sun;
	VETool ve;
	PropertyChangeListener propL;
	ActionListener palListener;
	ActionListener stateChange;
	JMenu paletteMenu;
	Hashtable palettes;
	Palette oceanPalette;
	Palette landPalette;
	Palette defaultPalette;
	Palette currentPalette;
	Histogram landHist, oceanHist, defaultHist;
	PaletteTool paletteTool;
	PerspectiveTool pers;
	JTabbedPane tabs;
	public RenderTools() {
		this( (GridImage)null );
	}
	public RenderTools(GridImage grid) {
		super( new BorderLayout() );
		this.grid = grid;
		init();
		setGrid( grid );
	}
	public void setGrid(GridImage grid) {
		this.grid = grid;
		setNewGrid();
	}
	public void setNewGrid() {
		pers.setGrid( grid);
		bothB.setSelected(true);
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
		try {
			defaultHist = new Histogram(grid.getGrid(), 200);
			scaler.setHist(defaultHist);
			scaler.setPalette(defaultPalette);
			paletteTool.setDefaultPalette( defaultPalette);
			currentPalette = defaultPalette;
			bothB.setEnabled(true);
		} catch(Exception ex) {
			defaultHist = null;
			bothB.setEnabled(false);
		}
		
	}
	void init() {
		pers = new PerspectiveTool( grid);
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

		scaler = new ColorHistogram();

		oceanPalette = new Palette(Palette.OCEAN);
		landPalette = new Palette(Palette.LAND);
		defaultPalette = new Palette(Palette.HAXBY);
		renderer = new GridRenderer(defaultPalette,
				1.,
				1000.,
				new XYZ(1.,1.,1.));
		scaler.setPalette( defaultPalette );
		currentPalette = defaultPalette;

		paletteTool = new PaletteTool( currentPalette, mod );
		paletteTool.setDefaultPalette( defaultPalette);
		paletteTool.addPropertyChangeListener(propL);

		sun = new SunTool(new XYZ(-1., 1., 1.));
		sun.addPropertyChangeListener(propL);

		ve = new VETool(1.);
		ve.setVE( currentPalette.getVE() );
		ve.addPropertyChangeListener(propL);

		javax.swing.border.Border border = 
			BorderFactory.createEmptyBorder(1,1,1,1);
		javax.swing.border.Border lineBorder = 
			BorderFactory.createLineBorder(Color.black);
		SimpleBorder sb = new SimpleBorder(true);

		oceanB = new JToggleButton(
			Icons.getIcon(Icons.OCEAN, false) );
		oceanB.setSelectedIcon(Icons.getIcon(Icons.OCEAN, true));
		oceanB.setDisabledIcon( new ImageIcon(
			GrayFilter.createDisabledImage(
				Icons.getIcon(Icons.OCEAN, false).getImage())));
		oceanB.setBorder( border );
		oceanB.addActionListener( stateChange);
		oceanB.setToolTipText("modify ocean palette");

		landB = new JToggleButton(Icons.getIcon(
					Icons.LAND, false));
		landB.setSelectedIcon(Icons.getIcon(Icons.LAND, true));
		landB.setDisabledIcon( new ImageIcon(
			GrayFilter.createDisabledImage(
				Icons.getIcon(Icons.LAND, false).getImage())));
		landB.setBorder( border );
		landB.addActionListener( stateChange);
		landB.setToolTipText("modify land palette");

		bothB = new JToggleButton(Icons.getIcon(
					Icons.OCEAN_LAND, false));
		bothB.setSelectedIcon(Icons.getIcon(Icons.OCEAN_LAND, true));
		bothB.setBorder( border );
		bothB.addActionListener( stateChange);
		bothB.setSelected(true);
		bothB.setToolTipText("modify default palette");

		ButtonGroup group = new ButtonGroup();
		group.add(oceanB);
		group.add(landB);
		group.add(bothB);

		JButton back = new JButton(Icons.getIcon(Icons.BACK, false));
		back.setPressedIcon(Icons.getIcon(Icons.BACK, true));
		back.setBorder( border );
		back.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
			//	back();
			}
		});
		back.setToolTipText("undo");

		JButton forward = new JButton(Icons.getIcon(Icons.FORWARD, false));
		forward.setPressedIcon(Icons.getIcon(Icons.FORWARD, true));
		forward.setBorder( border );
		forward.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
			//	forward();
			}
		});
		forward.setToolTipText("redo");

		JButton normalize = new JButton(Icons.getIcon(Icons.NORMALIZE, false));
		normalize.setPressedIcon(Icons.getIcon(Icons.NORMALIZE, true));
		normalize.setBorder( border );
		normalize.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				normalize();
			}
		});
		normalize.setToolTipText("normalize histogram");

		JButton unnormalize = new JButton(Icons.getIcon(Icons.UNNORMALIZE, false));
		unnormalize.setPressedIcon(Icons.getIcon(Icons.UNNORMALIZE, true));
		unnormalize.setBorder( border );
		unnormalize.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				unnormalize();
			}
		});
		unnormalize.setToolTipText("reset histogram");

		continuousB = new JToggleButton(
				Icons.getIcon(Icons.CONTINUOUS, false));
		continuousB.setSelectedIcon( Icons.getIcon(Icons.CONTINUOUS, true));
		continuousB.setSelected( true);
		continuousB.setBorder( border );
		continuousB.addActionListener( stateChange);
		continuousB.setToolTipText("Continuous Color Change");

		discreteB = new JToggleButton(
				Icons.getIcon(Icons.DISCRETE, false));
		discreteB.setSelectedIcon( Icons.getIcon(Icons.DISCRETE, true));
		discreteB.setBorder( border );
		discreteB.addActionListener( stateChange);
		discreteB.setToolTipText(("<html>Discrete Color Change <br> " +
						"at Specified Interval </html>"));

		colorInterval = new JTextField("1000", 5);

		group = new ButtonGroup();
		group.add( continuousB );
		group.add( discreteB );

		contourB = new JToggleButton(Icons.getIcon(Icons.CONTOUR, false));
		contourB.setSelectedIcon(Icons.getIcon(Icons.CONTOUR, true));
		contourB.setBorder( border );
		contourB.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				contour();
			}
		});
		contourB.setToolTipText("contour the grid\n "
				+"at specified interval)");
		contourInterval = new JTextField("1000", 5);

		JPanel tools = new JPanel( new BorderLayout());
		Box toolBox = Box.createHorizontalBox();
		Box box = Box.createHorizontalBox();
		box.add(back);
		box.add(forward);
		box.setBorder(sb);
		toolBox.add(box);
		Component strut = box.createHorizontalStrut(4);
		toolBox.add( strut );

		box = Box.createHorizontalBox();
		box.add(normalize);
		box.add(unnormalize);
		box.setBorder(sb);
		toolBox.add(box);
		strut = box.createHorizontalStrut(4);
		toolBox.add( strut );

		box = Box.createHorizontalBox();
		box.add(oceanB);
		box.add(landB);
		box.add(bothB);
		box.setBorder(sb);
		toolBox.add(box);
		strut = box.createHorizontalStrut(4);
		toolBox.add( strut );

		box = Box.createHorizontalBox();
		box.add( continuousB );
		box.add( discreteB );
		box.add( colorInterval );
		box.setBorder(sb);
		toolBox.add(box);
		strut = box.createHorizontalStrut(4);
		toolBox.add( strut );

		box = Box.createHorizontalBox();
		box.add(contourB);
		box.add(contourInterval);
		box.setBorder(sb);
		strut = box.createHorizontalStrut(4);
		toolBox.add(box);
		toolBox.add( strut );

		JMenuBar bar = new JMenuBar();
		paletteMenu = new JMenu("Palettes");
		palettes = new Hashtable();
		palListener = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				changePalette(evt.getActionCommand());
			}
		};
		JMenuItem item = paletteMenu.add(new JMenuItem("save palette"));
		item.addActionListener(palListener);
		paletteMenu.addSeparator();
		for( int k=0 ; k<Palette.resources.length ; k++) {
			Palette p = new Palette(k);
			item = paletteMenu.add(new JMenuItem(
				p.toString(), p.getIcon()));
			item.addActionListener(palListener);
			palettes.put( p.toString(), p);
		}
		paletteMenu.addSeparator();
		loadMyPalettes();
		paletteMenu.setBorder( sb );

		bar.add( paletteMenu );
		tools.add(toolBox);
		tools.add( bar, "East" );

		JPanel palPanel = new JPanel(new BorderLayout());
		palPanel.add(tools, "North");
		palPanel.add(scaler);
		scaler.setBorder(lineBorder);
		scaler.addPropertyChangeListener(propL);
		
		palPanel.add(mod, "West");

		palPanel.setBorder( sb);
		add( palPanel );

		JPanel panel = new JPanel( new GridLayout(0,1));

		JPanel sp = sun.getPanel();
		sp.setBorder(BorderFactory.createTitledBorder("Sun Illumination"));
		panel.add(sp);
		JPanel veP = ve.getPanel();
		veP.setBorder(BorderFactory.createTitledBorder("Vertical Exaggeration"));
		panel.add(veP);
		panel.setBorder( sb);
		add(panel, "East");
		add( paletteTool.getButtonPanel(), "South");
		initDialog();
	}
	void fire(PropertyChangeEvent evt) {
	//	firePropertyChange( 
	//			evt.getPropertyName(),
	//			evt.getOldValue(),
	//			evt.getNewValue());
		if( evt.getSource()==sun || evt.getSource()==ve ) {
			gridImage();
			return;
		}
		if( evt.getPropertyName().equals("STATE_CHANGE") ) {
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
		} else if( evt.getPropertyName().equals("RANGE_CHANGED") ||
				evt.getPropertyName().startsWith("APPLY")) {
			scaler.repaint();
			gridImage();
		}
	}
	void loadMyPalettes() {
		File root = org.geomapapp.io.GMARoot.getRoot();
		if(root==null)return;
		File dir = new File(root, "lut");
		if( !dir.exists())return;
		File[] files = dir.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return file.getName().endsWith(".lut");
			}
		});
		for( int k=0 ; k<files.length ; k++) {
			try {
				Palette p = new Palette(files[k]);
				JMenuItem item = paletteMenu.add(new JMenuItem(
					p.toString(), p.getIcon()));
				item.addActionListener(palListener);
				palettes.put( p.toString(), p);
			} catch(Exception ex) {
			}
		}
	}
				
	public void savePalette() {
		Palette p = null;
		try {
			p = getPalette().savePalette(dialog);
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
	void changePalette(String name) {
		if( name.equals("save palette") ) {
			savePalette();
			return;
		}
		float[] range = getPalette().getRange();
		Palette p = (Palette)palettes.get(name);
		p = (Palette)p.clone();
		p.setRange( range[0], range[1]);
		if( bothB.isSelected() ) defaultPalette = p;
		else if(oceanB.isSelected()) oceanPalette = p;
		else landPalette = p;
		scaler.setPalette( p );
		paletteTool.setDefaultPalette( p );
		ve.setVE( p.getVE() );
		scaler.repaint();
		gridImage();
	}
	void normalize() {
		if( bothB.isSelected() ) {
			double[] r = defaultHist.getRange();
			defaultPalette.setRange((float)r[0],
						(float)r[1]);
		} else if( landB.isSelected() ) {
			double[] r = landHist.getRange();
			landPalette.setRange((float)r[0],
						(float)r[1]);
		} else {
			double[] r = oceanHist.getRange();
			oceanPalette.setRange((float)r[0],
						(float)r[1]);
		}
		scaler.repaint();
		gridImage();
	}
	void unnormalize() {
		if( bothB.isSelected() ) {
			defaultPalette.resetRange();
		} else if( landB.isSelected() ) {
			landPalette.resetRange();
		} else {
			oceanPalette.resetRange();
		}
		scaler.repaint();
		gridImage();
	}
	public XYZ getSun() {
		return sun.getSun();
	}
	public double getVE() {
		return ve.getVE();
	}
	public boolean isContourSelected() {
		return contourB.isSelected();
	}
	public double getContourInterval() {
		double interval = Double.NaN;
		try {
			interval = Double.parseDouble(contourInterval.getText());
		} catch( Exception ex) {
		}
		return interval;
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
	public boolean isPaletteContinuous() {
		return continuousB.isSelected();
	}
	void initDialog() {
		dialog = new JDialog( 
	//	dialog = new JFrame(
			(JFrame)grid.getTopLevelAncestor(), 
			"Grid Rendering Tools");
		tabs = new JTabbedPane(JTabbedPane.TOP);
		tabs.add( "Rendering", this);
		dialog.getContentPane().add(tabs);
		JLabel label = new JLabel("Rendering tools are new "
				+"and still under development - "
				+"use with caution");
		dialog.getContentPane().add(label, "South");
		dialog.pack();
		tabs.add( "3D", pers);
		tabs.addChangeListener( new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				tabChange();
			}
		});
	}
	void tabChange() {
		if( tabs.getSelectedComponent()==pers ) {
			pers.update();
		}
	}
	void gridImage() {
		renderer.setPalette( defaultPalette );
		renderer.setLandPalette( landPalette );
		renderer.setOceanPalette( oceanPalette );
		double d = -1.;
		if( discreteB.isSelected() ) {
			try {
				d = Double.parseDouble(colorInterval.getText());
			} catch(Exception ex) {
				colorInterval.setText("????");
			}
		}
		defaultPalette.setDiscrete( d );
		landPalette.setDiscrete( d );
		oceanPalette.setDiscrete( d );
		renderer.setSun(sun.getSun());
		getPalette().setVE( ve.getVE() );
	//	renderer.setVE( ve.getVE());
		RenderResult renderResult = bothB.isSelected() ?
				renderer.gridImage( grid.getGrid() ) :
				renderer.gridImage( grid.getGrid(), grid.getLandMask());
		BufferedImage image = renderResult.image;
		grid.setImage(image);
		grid.repaint();
	}
	public void contour() {
		if( contourB.isSelected() ) {
			try {
				double interval = Double.parseDouble(
					contourInterval.getText());
			//	grid.contourGrid(interval);
			} catch(Exception ex) {
				contourInterval.setText("  ?  ");
				contourB.setSelected(false);
			}
		} else {
		//	grid.contourGrid(-1.);
		}
	}
	public void showDialog() {
		dialog.show();
	}
	public static void main(String[] args) {
		RenderTools tools = new RenderTools();
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation( frame.EXIT_ON_CLOSE);
		frame.getContentPane().add(tools);
		frame.pack();
		frame.show();
	}
}

package org.geomapapp.image;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.geomapapp.geom.XYZ;
import org.geomapapp.util.Histogram;
import org.geomapapp.util.Icons;
import org.geomapapp.util.SimpleBorder;

public class SimpleRenderingTools extends RenderingTools {
		private List propertyListeners = new LinkedList();

		public synchronized void addChangeListener(PropertyChangeListener listener) {
			propertyListeners.add(listener);
		}

		public synchronized void removeChangeListener(PropertyChangeEvent listener) {
			propertyListeners.remove(listener);
		}

		public void applyToRenderer(GridRenderer renderer) {
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
			getPalette().setVE( ve.getVE() );
			renderer.setSun(sun.getSun());
			renderer.sunIllum = sun.isSunOn();
		}

		public JPanel getTool(String toolName) {
			return this;
		}

		public void gridImage() {
			for (Iterator iterator = propertyListeners.iterator(); iterator.hasNext();) {
				PropertyChangeListener listener = (PropertyChangeListener) iterator.next();
				listener.propertyChange(new PropertyChangeEvent(this,"RENDER_TOOLS",null,this));
			}
		}

		public void init() {
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

			KeyListener copyPaste = new KeyAdapter() {
				public void keyReleased(KeyEvent e) {
					if( !e.isControlDown() )return;
					int k = e.getKeyCode();
					if( k==KeyEvent.VK_C )copy();
					else if( k==KeyEvent.VK_V )paste();
				}
			};
			scaler = new ColorHistogram();
			scaler.addKeyListener( copyPaste );

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
				//	undo();
				}
			});
			back.setToolTipText("undo");

			JButton forward = new JButton(Icons.getIcon(Icons.FORWARD, false));
			forward.setPressedIcon(Icons.getIcon(Icons.FORWARD, true));
			forward.setBorder( border );
			forward.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
				//	redo();
				}
			});
			forward.setToolTipText("redo");

//			JButton normalize = new JButton(Icons.getIcon(Icons.NORMALIZE, false));
			JToggleButton normalize = new JToggleButton(Icons.getIcon(Icons.NORMALIZE, false));
			normalize.setPressedIcon(Icons.getIcon(Icons.NORMALIZE, true));
			normalize.setBorder( border );
			normalize.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					autoNormalize = true;
					normalize();
				}
			});
			normalize.setToolTipText("normalize histogram/auto-normalize on");

//			JButton unnormalize = new JButton(Icons.getIcon(Icons.UNNORMALIZE, false));
			JToggleButton unnormalize = new JToggleButton(Icons.getIcon(Icons.UNNORMALIZE, false));
			unnormalize.setPressedIcon(Icons.getIcon(Icons.UNNORMALIZE, true));
			unnormalize.setBorder( border );
			unnormalize.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					autoNormalize = false;
					unnormalize();
				}
			});
			unnormalize.setToolTipText("reset histogram//auto-normalize off");

			ButtonGroup normalizeButtonGroup = new ButtonGroup();
			normalizeButtonGroup.add(normalize);
			normalizeButtonGroup.add(unnormalize);

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
			discreteB.setToolTipText("<html>Discrete Color Change <br> " +
						"at Specified Interval </html>");

			colorInterval = new JTextField("1000", 5);
			colorInterval.setToolTipText("set default coloring interval");

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
			contourB.setToolTipText("Set Contour Interval and Range");

			// Vertical Exaggeration Panel.
			JPanel veP = ve.getPanel();
			veP.setToolTipText("Vertical Exaggeration");

			JPanel tools = new JPanel( new BorderLayout());
			Box toolBox = Box.createHorizontalBox();
			Box box = Box.createHorizontalBox();
			Component strut = Box.createHorizontalStrut(4);
//			box.add(back);
//			box.add(forward);
//			box.setBorder(sb);
//			toolBox.add(box);
//			toolBox.add( strut );

			box = Box.createHorizontalBox();
			box.add(normalize);
			box.add(unnormalize);
			box.setBorder(sb);
			toolBox.add(box);
			strut = Box.createHorizontalStrut(4);
			toolBox.add( strut );

			box = Box.createHorizontalBox();
			box.add(oceanB);
			box.add(landB);
			box.add(bothB);
			box.setBorder(sb);
			toolBox.add(box);
			strut = Box.createHorizontalStrut(4);
			toolBox.add( strut );

			box = Box.createHorizontalBox();
			box.add( continuousB );
			box.add( discreteB );
			box.add( colorInterval );
			box.add(veP);
			box.setBorder(sb);
			toolBox.add(box);
			strut = Box.createHorizontalStrut(4);
			toolBox.add( strut );

//			box = Box.createHorizontalBox();
//			box.add(contourB);
//			box.add(contourInterval);
//			box.setBorder(sb);
//			strut = Box.createHorizontalStrut(4);
//			toolBox.add(box);
//			toolBox.add( strut );

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

//			palPanel.add(mod, "West");

			palPanel.setBorder( sb);
			add( palPanel );

			JPanel panel = new JPanel( new GridLayout(0,1));

			JPanel sp = sun.getPanel();
			sp.setBorder(BorderFactory.createTitledBorder("Sun Illumination"));

//			panel.add(sunOn);
//			panel.add(sunOff);

			panel.add(sp);
			add(panel, "East");
//			add( paletteTool.getButtonPanel(), "South");
		}

		protected void initDialog() {

		}

		public void setNewGrid() {
			JToggleButton sb;
			if (landB.isSelected())
				sb = landB;
			else if (oceanB.isSelected())
				sb = oceanB; 
			else {
				sb = bothB;
				sb.setSelected(true);
			}

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
			if( !sb.isEnabled() ) {
				bothB.setSelected(true);
				sb = bothB;
			}
//			if (grid.toString().equals(GridDialog.DEM))	{
//				oceanB.setSelected(true);
//				sb = oceanB;
//			}
			try {
				defaultHist = new Histogram(grid.getGrid(), 200);
				if( sb==oceanB ) {
					scaler.setHist(oceanHist);
					scaler.setPalette(oceanPalette);
					paletteTool.setDefaultPalette( oceanPalette);
					currentPalette = oceanPalette;
				} else if( sb==landB ) {
					scaler.setHist(landHist);
					scaler.setPalette(landPalette);
					paletteTool.setDefaultPalette( landPalette);
					currentPalette = landPalette;
				} else {
					scaler.setHist(defaultHist);
					scaler.setPalette(defaultPalette);
					paletteTool.setDefaultPalette( defaultPalette);
					currentPalette = defaultPalette;
				}
				bothB.setEnabled(true);
//				normalize();
			} catch(Exception ex) {
				defaultHist = null;
				bothB.setEnabled(false);
			}
		}
	}
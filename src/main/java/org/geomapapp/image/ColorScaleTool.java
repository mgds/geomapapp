package org.geomapapp.image;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileFilter;
import java.util.Hashtable;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.geomapapp.util.DataHistogram;
import org.geomapapp.util.Icons;
import org.geomapapp.util.SimpleBorder;

public class ColorScaleTool extends JPanel {
//	1.3.5: Changed window from JDialog to JFrame and 
//	added variable to allow the title of the window 
//	to be set dynamically
//	JDialog dialog;
	String colorDialogName;
	JFrame dialog;
	JFrame frame;
	JToggleButton continuousB, discreteB;
	JTextField colorInterval;
	ColorHistogram scaler;
	ColorModPanel mod;
	PropertyChangeListener propL;
	ActionListener palListener;
	ActionListener stateChange;
	JMenu paletteMenu;
	Hashtable<String, Palette> palettes;
	Palette oceanPalette;
	Palette landPalette;
	Palette defaultPalette;
	Palette currentPalette;
	DataHistogram landHist, oceanHist, defaultHist;
	PaletteTool paletteTool;
	ColorLegend leg;
	JButton saveLegend;
	JLabel myLabel;
	float[] data;
	public static int saveLegendCount = 1;

	public ColorScaleTool(float[] data) {
		super( new BorderLayout() );
		this.data = data;
		init();
		setGrid( data );
	}
	public void setGrid(float[] data) {
		this.data = data;
		setNewGrid();
	}

//	1.3.5: Added to allow other classes to dynamically 
//	change the title of the window
	public void setName(String name) {
		if (name != null) {
			colorDialogName = name;
		}
		else {
			colorDialogName = "Color Scaler";
		}
	}

	public void setNewGrid() {
		defaultHist = new DataHistogram(data, 200);
		scaler.setHist(defaultHist);
		scaler.setPalette(defaultPalette);
		paletteTool.setDefaultPalette( defaultPalette);
		currentPalette = defaultPalette;
	}
	void init() {
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

				if(leg!=null) {
					leg.repaint();
					leg.getTopLevelAncestor().repaint();
					frame.setSize(100,leg.getHeight()+myLabel.getHeight()+saveLegend.getHeight()+18);
				}
			}
		};

		mod = new ColorModPanel( Color.blue.getRGB());
		scaler = new ColorHistogram();
		oceanPalette = new Palette(Palette.OCEAN);
		landPalette = new Palette(Palette.LAND);
		defaultPalette = new Palette(Palette.HAXBY);
		scaler.setPalette( defaultPalette );
		currentPalette = defaultPalette;

		paletteTool = new PaletteTool( currentPalette, mod );
		paletteTool.setDefaultPalette( defaultPalette);
		paletteTool.addPropertyChangeListener(propL);

	//	sun = new SunTool(new XYZ(-1., 1., 1.));
	//	sun.addPropertyChangeListener(propL);

	//	ve = new VETool(1.);
	//	ve.setVE( currentPalette.getVE() );
	//	ve.addPropertyChangeListener(propL);

		Border border = BorderFactory.createEmptyBorder(1,1,1,1);
		Border lineBorder = BorderFactory.createLineBorder(Color.black);
		SimpleBorder sb = new SimpleBorder(true);
		ButtonGroup group = new ButtonGroup();

		JButton normalize = new JButton(Icons.getIcon(Icons.NORMALIZE, false));
		normalize.setPressedIcon(Icons.getIcon(Icons.NORMALIZE, true));
		normalize.setBorder( border );
		normalize.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				normalize();

				if(leg!=null) {
					leg.repaint();
					leg.getTopLevelAncestor().repaint();
					frame.setSize(100,leg.getHeight()+myLabel.getHeight()+saveLegend.getHeight()+18);
				}
			}
		});
		normalize.setToolTipText("normalize histogram");

		JButton unnormalize = new JButton(Icons.getIcon(Icons.UNNORMALIZE, false));
		unnormalize.setPressedIcon(Icons.getIcon(Icons.UNNORMALIZE, true));
		unnormalize.setBorder( border );
		unnormalize.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				unnormalize();
				if(leg!=null) {
					leg.repaint();
					leg.getTopLevelAncestor().repaint();
					frame.setSize(100,leg.getHeight()+myLabel.getHeight()+saveLegend.getHeight()+18);
				}
			}
		});
		unnormalize.setToolTipText("reset histogram");

		
		JButton flip = new JButton(Icons.getIcon(Icons.SPIN, false));
		flip.setPressedIcon(Icons.getIcon(Icons.SPIN, true));
		flip.setBorder( border );
		flip.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				flip();
			}
		});
		flip.setToolTipText("Flip scale");
		
		
		continuousB = new JToggleButton(Icons.getIcon(Icons.CONTINUOUS, false));
		continuousB.setSelectedIcon( Icons.getIcon(Icons.CONTINUOUS, true));
		continuousB.setSelected( true);
		continuousB.setBorder( border );
		continuousB.addActionListener( stateChange);
		continuousB.setToolTipText("Continuous Color Change");

		discreteB = new JToggleButton(Icons.getIcon(Icons.DISCRETE, false));
		discreteB.setSelectedIcon( Icons.getIcon(Icons.DISCRETE, true));
		discreteB.setBorder( border );
		discreteB.addActionListener( stateChange);
		discreteB.setToolTipText("<html>Discrete Color Change <br> " +
				"at Specified Interval </html>");

		colorInterval = new JTextField("1000", 5);

		group = new ButtonGroup();
		group.add( continuousB );
		group.add( discreteB );

		JPanel tools = new JPanel( new BorderLayout());
		Box toolBox = Box.createHorizontalBox();

		Box box = Box.createHorizontalBox();
		box.add(normalize);
		box.add(unnormalize);
		box.add(flip);
		box.setBorder(sb);
		toolBox.add(box);
		Component strut = box.createHorizontalStrut(4);
		toolBox.add( strut );

		box = Box.createHorizontalBox();
		box.add( continuousB );
		box.add( discreteB );
		box.add( colorInterval );
		box.setBorder(sb);
		toolBox.add(box);
		strut = box.createHorizontalStrut(4);
		toolBox.add( strut );

		JMenuBar bar = new JMenuBar();
		paletteMenu = new JMenu("Palettes");
		
		//fill the palettes hashtable with all available palettes
		palettes = new Hashtable<String, Palette>();
		for( int k=0 ; k<Palette.resources.length ; k++) {
			Palette p = new Palette(k);
			palettes.put( p.toString(), p);
		}
		
		palListener = new ActionListener() {
			JDialog morePalDialog;
			public void actionPerformed(ActionEvent evt) {
				if (evt.getSource() instanceof JMenuItem) {
					if (evt.getActionCommand().equals("More Palettes...") ) {
						morePalDialog = new JDialog(dialog, "More Palettes");
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
					
					} else {
						changePalette(evt.getActionCommand());

						if(leg!=null) {
							leg.repaint();
							leg.getTopLevelAncestor().repaint();
							frame.setSize(100,leg.getHeight()+myLabel.getHeight()+saveLegend.getHeight()+18);
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
						
						if(leg!=null) {
							leg.repaint();
							leg.getTopLevelAncestor().repaint();
							frame.setSize(100,leg.getHeight()+myLabel.getHeight()+saveLegend.getHeight()+18);
						}
					}
				}
			}
		};
		JMenuItem item = paletteMenu.add(new JMenuItem("save palette"));
		item.addActionListener(palListener);
		paletteMenu.addSeparator();
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
		loadMyPalettes();
		paletteMenu.setBorder( sb );

		bar.add( paletteMenu );
		final ColorScaleTool cst = this;
		scaler.setColorScaleTool(cst);

		// Color Legend button
		JButton colorScaleButton = new JButton("Color Legend");
		colorScaleButton.addActionListener( new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				// Save Legend Button
				saveLegend = new JButton("Save Legend");
				saveLegend.setSize(120, 30);

				// Color Legend
				leg = new ColorLegend(cst);

				final JPanel pan = new JPanel(new BorderLayout());
				frame = new JFrame(colorDialogName);
				String labelText;

				if(colorDialogName.contains(":")) {
					labelText = "<html>".concat(colorDialogName.split(":")[1]).concat(" ,").concat(colorDialogName.split("-")[0]).concat("</html>");
				} else {
					labelText = "<html>".concat(colorDialogName).concat("</html>");
				}

				labelText = labelText.replaceAll(",", ",<br>");
				myLabel = new JLabel(labelText);

				JPanel legendTitle = new JPanel(new BorderLayout());
				legendTitle.add(myLabel);
				legendTitle.setMaximumSize(new Dimension(110, 130));
				legendTitle.setPreferredSize(new Dimension(110, 125));
				legendTitle.setBackground(Color.LIGHT_GRAY);
				pan.add(legendTitle,"North");

				JPanel legendColor = new JPanel(new BorderLayout());
				legendColor.add(leg);
				legendColor.setMaximumSize(new Dimension(120, 415));
				legendColor.setPreferredSize(new Dimension(120, 410));
				pan.add(legendColor,"Center");

				JPanel legendSave = new JPanel(new BorderLayout());
				legendSave.add(saveLegend);
				legendSave.setMaximumSize(new Dimension(120, 60));
				legendSave.setPreferredSize(new Dimension(120, 30));
				pan.add(legendSave,"South");

				int totalHP = legendTitle.getPreferredSize().height + legendColor.getPreferredSize().height + legendSave.getPreferredSize().height;
				int totalHM = legendTitle.getMaximumSize().height + legendColor.getMaximumSize().height + legendSave.getMaximumSize().height;
				pan.setMaximumSize(new Dimension(120, totalHM));
				pan.setPreferredSize(new Dimension(120, totalHP));

				frame.setSize(pan.getMaximumSize().width, pan.getMaximumSize().height);
				frame.getContentPane().add(new JScrollPane(pan, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
				frame.setBackground(Color.WHITE);
				frame.setVisible(true);
				frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE);

				saveLegend.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						// TODO Auto-generated method stub

						File file = new File("legend" + saveLegendCount++
								+ ".png");

						JFileChooser saveDialog = new JFileChooser(System
								.getProperty("user.home"));
						saveDialog.setSelectedFile(file);

						saveDialog
								.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
									public boolean accept(File file) {
										return file.isDirectory()
												|| file.getName().toLowerCase()
														.endsWith("png");
									}

									public String getDescription() {
										return "PNG Image (.png)";
									}
								});

						int ok = saveDialog.showSaveDialog(null);
						if (ok == JFileChooser.CANCEL_OPTION) {
							return;
						}
						String path = saveDialog.getSelectedFile().getPath();

						if (!path.contains(".png")) {
							path = path.concat(".png");
						}
						// String file_name =
						// saveDialog.getName(saveDialog.getSelectedFile());

						BufferedImage bi = new BufferedImage(
								pan.getSize().width, pan.getSize().height
										- saveLegend.getHeight(),
								BufferedImage.TYPE_INT_RGB);
						Graphics g = bi.createGraphics();
						pan.paint(g); // this == JComponent
						g.dispose();
						try {
							ImageIO.write(bi, "png", new File(path));
						} catch (Exception e1) {
						}
					}
				});
			}
		});
		bar.add(colorScaleButton);
		tools.add(toolBox);
		tools.add( bar, "East" );

		JPanel palPanel = new JPanel(new BorderLayout());
		palPanel.add(tools, "North"); // toolbar
		palPanel.add(scaler); // histogram
		scaler.setBorder(lineBorder);
		scaler.addPropertyChangeListener(propL);

		palPanel.setBorder( sb);
		add( palPanel );

	//	JPanel panel = new JPanel( new GridLayout(0,1));

	//	JPanel sp = sun.getPanel();
	//	sp.setBorder(BorderFactory.createTitledBorder("Sun Illumination"));
	//	panel.add(sp);
	//	JPanel veP = ve.getPanel();
	//	veP.setBorder(BorderFactory.createTitledBorder("Vertical Exaggeration"));
	//	panel.add(veP);
	//	panel.setBorder( sb);
	//	add(panel, "East");
		initDialog();
	}
	void fire(PropertyChangeEvent evt) {
	//	firePropertyChange( 
	//			evt.getPropertyName(),
	//			evt.getOldValue(),
	//			evt.getNewValue());
	//	if( evt.getSource()==sun || evt.getSource()==ve ) {
	//		gridImage();
	//		return;
	//	}
		scaler.setPalette( defaultPalette);
		scaler.setHist( defaultHist);
		double d = -1.;
		if( discreteB.isSelected() ) {
			try {
				d = Double.parseDouble(colorInterval.getText());
			} catch(Exception ex) {
				colorInterval.setText("????");
			}
		}
		defaultPalette.setDiscrete( d );
	//	paletteTool.setDefaultPalette( defaultPalette );
		scaler.repaint();
		firePropertyChange( "PALETTECHANGE", false, true);
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
		Palette p_orig = (Palette)palettes.get(name);
		Palette p = (Palette)p_orig.clone();
		p.setRange( range[0], range[1]);
		//cloning doesn't copy name
		p.setName(p_orig.name);
		defaultPalette = p;
	//	if( bothB.isSelected() ) defaultPalette = p;
	//	else if(oceanB.isSelected()) oceanPalette = p;
	//	else landPalette = p;
		scaler.setPalette( p );
		paletteTool.setDefaultPalette( p );
		scaler.repaint();
	}
	void normalize() {
		double[] r = defaultHist.getRange();
		defaultPalette.setRange((float)r[0],
					(float)r[1]);
		firePropertyChange( "PALETTECHANGE", false, true);
		scaler.repaint();
	}
	void unnormalize() {
		defaultPalette.resetRange();
		firePropertyChange( "PALETTECHANGE", false, true);
		scaler.repaint();
	}
	
	void flip(){
		scaler.flip();
		firePropertyChange( "PALETTECHANGE", false, true);
		scaler.repaint();
	}
	
	public int getRGB( float z ) {
		if( Float.isNaN(z) )return 0;
		return defaultPalette.getRGB(z);
	}
	public Color getColor( float z ) {
		return new Color(getRGB(z));
	}
	public Palette getPalette() {
		return defaultPalette;
	}
	public void setPalette(Palette pal) {
		defaultPalette = pal;
	}
	public Palette[] getPalettes() {
		Palette[] pal =
			new Palette[] {defaultPalette} ;
		return pal;
	}
	public boolean isPaletteContinuous() {
		return continuousB.isSelected();
	}
	void initDialog() {
		System.out.println(this.colorDialogName);
	}
	public void showDialog(JFrame owner) {
		if( dialog==null ) {

//			***** Change by A.K.M. 06/30/06 *****
/*
			dialog = new JDialog(owner,
				"Color Scaler");
*/
//			Change dialog from JDialog to JFrame to add minimization capability
			dialog = new JFrame();
//			***** Change by A.K.M. 06/30/06 *****

			dialog.getContentPane().add(this);
			dialog.pack();

			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					dialog.setVisible(false);
					firePropertyChange("WINDOW_HIDDEN", false, true);
				}
			});
		}

//		1.3.5: Title of window set here
		dialog.setTitle(colorDialogName);
		dialog.setLocationRelativeTo(owner);
		dialog.setVisible(true);

//		***** Change by A.K.M. 06/30/06 *****
//		Default to normalized coloring
		normalize();
//		***** Change by A.K.M. 06/30/06 *****

	}
	public void showDialog(JDialog owner) {
		if( dialog==null ) {

//			***** Change by A.K.M. 06/30/06 *****
			/*		dialog = new JDialog(owner,
					"Color Scaler");
			*/
//			Change dialog from JDialog to JFrame to add minimization capability
			dialog = new JFrame();
//			***** Change by A.K.M. 06/30/06 *****

			dialog.getContentPane().add(this);
			dialog.pack();

			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					dialog.setVisible(false);
					firePropertyChange("WINDOW_HIDDEN", false, true);
				}
			});
		}

//		1.3.5: Title of window set here
		dialog.setTitle(colorDialogName);
		dialog.setLocationRelativeTo(owner);
		dialog.setVisible(true);

//		***** Change by A.K.M. 06/30/06 *****
//		Default to normalized coloring
		normalize();
	}
	public void hideDialog() {
		dialog.setVisible(false);
	}
	public void dispose() {
		if (dialog!=null) { 
			dialog.dispose();
			dialog = null;
		}
	}

	public ColorHistogram getScaler() {
		return scaler;
	}
	
	public Palette getCurrentPalette() {
		return currentPalette;
	}
	
	public void setCurrentPalette(Palette pal) {
		currentPalette = pal;
	}
	
	public void setColorInterval(String interval) {
		//set the text box
		colorInterval.setText(interval);
	}
	
	public void setDiscrete(boolean discrete) {
		discreteB.setSelected(discrete);
	}
	
	public static void main(String[] args) {
		float[] data = new float[1000];
		for(int k=0 ; k<1000 ; k++) data[k]=(float)Math.random();
		ColorScaleTool tools = new ColorScaleTool(data);
		tools.addPropertyChangeListener( 
			new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					System.out.println( evt.getPropertyName() );
				}
			});
	//	tools.showDialog( (JFrame)null );
		JFrame frame = new JFrame("test");
		frame.getContentPane().add(tools);
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

	}
}
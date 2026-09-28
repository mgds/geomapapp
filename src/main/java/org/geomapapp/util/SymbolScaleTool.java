package org.geomapapp.util;

import haxby.map.XMap;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class SymbolScaleTool extends JPanel {
//	1.3.5: Changed dialog from JDialog to JFrame to add additional window 
//	functionality
	JFrame dialog;

//	1.3.5: Variable used to set the window title, see setName(String name)
	String scaleDialogName = "Symbol Scaler";
	JFrame frame;
	ScaleHistogram scaler;
	PropertyChangeListener propL;
	ActionListener palListener;
	ActionListener stateChange;
	DataHistogram defaultHist;
	JButton saveLegend;
	ScaleLegend leg;
	JLabel myLabel;
	float[] data,range;
	XMap map;
	public static int saveLegendCount = 1;

	public SymbolScaleTool(float[] data, XMap map) {
		super( new BorderLayout() );
		this.data = data;
		this.map = map;
		init();
		setGrid( data );
		normalize();
	}
	public void setGrid(float[] data) {
		this.data = data;
		setNewGrid();
	}

//	***** Changed by A.K.M. 1.3.5 *****
//	Adds functionality to change the window title
	public void setName(String name) {
		if (name != null)
		{
			scaleDialogName = name;
		}
	}

	public void setNewGrid() {
		try {
			defaultHist = new DataHistogram(data, 200);
			scaler.setHist(defaultHist);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	public boolean isReady() {
		return scaler.isReady();
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
					(new Integer(0)), 
					(new Integer(1)) 
					));
			}
		};

		scaler = new ScaleHistogram();

		javax.swing.border.Border border = 
			BorderFactory.createEmptyBorder(1,1,1,1);
		javax.swing.border.Border lineBorder = 
			BorderFactory.createLineBorder(Color.black);
		SimpleBorder sb = new SimpleBorder(true);

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

		JButton flip = new JButton(Icons.getIcon(Icons.SPIN, false));
		flip.setPressedIcon(Icons.getIcon(Icons.SPIN, true));
		flip.setBorder( border );
		flip.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				flip();
			}
		});
		flip.setToolTipText("Flip scale");

		JPanel tools = new JPanel( new BorderLayout());
		Box toolBox = Box.createHorizontalBox();
		Box box = Box.createHorizontalBox();

		box.add(normalize);
		box.add(unnormalize);
		box.add(flip);
		box.setBorder(sb);
		toolBox.add(box);
		Component strut = Box.createHorizontalStrut(4);
		toolBox.add( strut );
		
		
		
		final SymbolScaleTool sst = this;
		scaler.setSymbolScaleTool(sst);
		
		// Scale Legend button
		JButton scaleLegendButton = new JButton("Scale Legend");
		scaleLegendButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Save Legend Button
				saveLegend = new JButton("Save Legend");
				saveLegend.setSize(120, 30);

				// Scale Legend
				leg = new ScaleLegend(sst);

				final JPanel pan = new JPanel(new BorderLayout());
				frame = new JFrame(scaleDialogName);
				String labelText;

				if (scaleDialogName.contains(":")) {
					labelText = "<html>".concat(scaleDialogName.split(":")[1])
							.concat(", ").concat(scaleDialogName.split("-")[0])
							.concat("</html>");
				} else {
					labelText = "<html>".concat(scaleDialogName).concat(
							"</html>");
				}

				// labelText = labelText.replaceAll(",", ",<br>");
				myLabel = new JLabel(labelText);
				int legWidth = 500;
				JPanel legendTitle = new JPanel(new BorderLayout());
				legendTitle.add(myLabel);
				legendTitle.setMaximumSize(new Dimension(legWidth, 30));
				legendTitle.setPreferredSize(new Dimension(legWidth, 30));
				legendTitle.setBackground(Color.LIGHT_GRAY);
				pan.add(legendTitle, "North");

				JPanel legendScale = new JPanel(new BorderLayout());
				legendScale.add(leg);
				legendScale.setMaximumSize(new Dimension(legWidth, 100));
				legendScale.setPreferredSize(new Dimension(legWidth, 60));
				pan.add(legendScale, "Center");

				JPanel legendSave = new JPanel(new BorderLayout());
				legendSave.add(saveLegend);
				legendSave.setMaximumSize(new Dimension(legWidth, 60));
				legendSave.setPreferredSize(new Dimension(legWidth, 30));
				pan.add(legendSave, "South");

				int totalHP = legendTitle.getPreferredSize().height
						+ legendScale.getPreferredSize().height
						+ legendSave.getPreferredSize().height;
				int totalHM = legendTitle.getMaximumSize().height
						+ legendScale.getMaximumSize().height
						+ legendSave.getMaximumSize().height;
				pan.setMaximumSize(new Dimension(legWidth, totalHM));
				pan.setPreferredSize(new Dimension(legWidth, totalHP));

				frame.setSize(pan.getMaximumSize().width,
						pan.getMaximumSize().height);
				frame.getContentPane().add(
						new JScrollPane(pan,
								JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
								JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
				frame.setBackground(Color.WHITE);
				frame.setVisible(true);
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

				saveLegend.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {

						File file = new File("scale_legend" + saveLegendCount++
								+ ".png");

						JFileChooser saveDialog = new JFileChooser(System
								.getProperty("user.home"));
						saveDialog.setSelectedFile(file);

						saveDialog.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
							public boolean accept(File file) {
								return file.isDirectory()
										|| file.getName().toLowerCase()
												.endsWith(".png");
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
						System.out.println(path);

						if (!path.contains(".png")) {
							path = path.concat(".png");
						}

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
		box.add(scaleLegendButton);
		tools.add(toolBox);

		JPanel palPanel = new JPanel(new BorderLayout());
		palPanel.add(tools, "North");
		palPanel.add(scaler);
		scaler.setBorder(lineBorder);
		scaler.addPropertyChangeListener(propL);
		palPanel.setBorder( sb);
		add( palPanel );
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
		scaler.setRange(range);
		scaler.setHist( defaultHist);
	//	paletteTool.setDefaultPalette( defaultPalette );
		scaler.repaint();
		firePropertyChange( "PALETTECHANGE", false, true);
	}

	void normalize() {
		double[] r = defaultHist.getRange();
		scaler.setRange(new float[] {(float)r[0],
					(float)r[1]});
		range = scaler.range;
		firePropertyChange( "PALETTECHANGE", false, true);
		scaler.repaint();
	}
	void unnormalize() {
		firePropertyChange( "PALETTECHANGE", false, true);
		scaler.repaint();
	}
	public void flip(){
		scaler.flip();
		firePropertyChange( "PALETTECHANGE", false, true);
		scaler.repaint();
	}

	public float getSizeRatio( float z ) {
		return scaler.getRatio(z);
	}

	void initDialog() {
	}

	public void showDialog(JFrame owner) {
		if( dialog==null ) {
			dialog = new JFrame();
			dialog.getContentPane().add(this);
			dialog.pack();

			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					dialog.setVisible(false);
					firePropertyChange("WINDOW_HIDDEN", false, true);
				}
			});
		}

//		1.3.5: Set window title to user-inputted value
		dialog.setTitle(scaleDialogName);
		dialog.setLocationRelativeTo(owner);
		dialog.show();
	}
	public void showDialog(JDialog owner) {
		if( dialog==null ) {
			dialog = new JFrame();
			dialog.getContentPane().add(this);
			dialog.pack();

			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					dialog.setVisible(false);
					firePropertyChange("WINDOW_HIDDEN", false, true);
				}
			});
		}

//		1.3.5: Set window title to user-inputted value
		dialog.setTitle(scaleDialogName);
		dialog.setLocationRelativeTo(owner);
		dialog.show();
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
	public static void main(String[] args) {
		float[] data = new float[1000];
		for(int k=0 ; k<1000 ; k++) data[k]=(float)Math.random();
		SymbolScaleTool tools = new SymbolScaleTool(data, null);
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
		frame.show();
	}
	
	public XMap getMap() {
		return map;
	}
	
	public float[] getRange() {
		return range;
	}
	
	public void setRange(float[] range) {
		this.range = range;
	}
}
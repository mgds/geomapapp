package haxby.worldwind.db.custom;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;

public class ExtrudeTool extends JPanel {

	private static final String[] units = {"km", "m", "custom"};
	private static final float[] unitScale = {1000, 1};
	
	private JFrame dialog;
	
	private String extrudeDialogName = "Symbol Elevation";

	private JLabel scaleLabel;
	private JTextField scaleField;
	private JComboBox elevationUnits;
	private JRadioButton above;
	
	private float lastValidScale = 1000;
	
	public ExtrudeTool() {
		super();
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		initGUI();
	}

	public void setName(String name) {
		if (name != null)
		{
			extrudeDialogName = name;
		}
	}
	
	private void initGUI() {
		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createTitledBorder("Value is"));
		above = new JRadioButton("Above Surface", true);
		ButtonGroup bg = new ButtonGroup();
		bg.add(above);
		p.add(above);
		JRadioButton below = new JRadioButton("Below Surface");
		bg.add(below);
		p.add(below);
		
		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				firePropertyChange("ELEVATION_SCALE", 1, -1); 
			}
		};
		
		above.addActionListener(al);
		below.addActionListener(al);
		
		add(p);
		
		p = new JPanel(new GridLayout(0,2));
		JLabel l = new JLabel("Units: ");
		p.add(l);
		elevationUnits = new JComboBox(units);
		elevationUnits.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateState();
			}
		});
		p.add(elevationUnits);
		
		scaleLabel = new JLabel("Elevation Scale: ");
		p.add(scaleLabel, BorderLayout.WEST);
		scaleLabel.setEnabled(false);
		
		scaleField = new JTextField(8);
		scaleField.addKeyListener( new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				textFieldChanged();
			}
		});
		scaleField.setText("1000");
		scaleField.setEnabled(false);
		p.add(scaleField);
		
		add(p);
		
		this.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
	}

	private void updateState() {
		int i = elevationUnits.getSelectedIndex();
		if (i == units.length - 1) {
			scaleLabel.setEnabled(true);
			scaleField.setEnabled(true);
		} else
		{
			scaleLabel.setEnabled(false);
			scaleField.setEnabled(false);
			scaleField.setText(unitScale[i]+"");
		}
		
		textFieldChanged();
	}

	private void textFieldChanged() {
		String text = scaleField.getText();
		try {
			float f = Float.parseFloat(text);
			if (Float.isNaN(f))
				;//getToolkit().beep();
			else
				changeScale(f);
		} catch (NumberFormatException ex) {
		}
	}

	private void changeScale(float f) {
		float old = lastValidScale;
		lastValidScale = f;
		firePropertyChange("ELEVATION_SCALE", old, lastValidScale);
	}

	public boolean isShowing() {
		return dialog.isVisible();
	}

	public void showDialog(JFrame c) {
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
		
		dialog.setTitle(extrudeDialogName);
		
		dialog.setLocationRelativeTo(c);
		
		dialog.show();
	}

	public float getScale() {
		return lastValidScale * (above.isSelected() ? 1 : -1);
	}

	public void dispose() {
		if (dialog!=null) { 
			dialog.dispose();
			dialog = null;
		}
	}
}	
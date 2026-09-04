package haxby.util;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.ParseException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class RotationPanel extends JPanel {
	private JFormattedTextField angleField;
	private DecimalFormat format;
	private JLabel info, unit;
	private JButton rotateLeft, rotateRight;
	private JPanel upperPanel, lowerPanel;

	public RotationPanel() {
		this(0.0);
	}

	public RotationPanel(double degrees) {
		format = new DecimalFormat("#0.###");
		angleField = new JFormattedTextField(format);
		angleField.setColumns(6);
		info = new JLabel("Rotate image (counterclockwise) by");
		unit = new JLabel("\u00B0");
		angleField.setValue(Double.valueOf(normalize(degrees)));

		rotateLeft = new JButton("\u293F +90\u00B0");
		rotateLeft.setToolTipText("Rotate 90\u00B0 counterclockwise");
		rotateLeft.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				nudge(90);
			}
		});

		rotateRight = new JButton("-90\u00B0 \u293E");
		rotateRight.setToolTipText("Rotate 90\u00B0 clockwise");
		rotateRight.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				nudge(-90);
			}
		});
		upperPanel = new JPanel();
		lowerPanel = new JPanel();
		BoxLayout bl = new BoxLayout(this, BoxLayout.Y_AXIS);
		this.setLayout(bl);

		upperPanel.add(info);
		upperPanel.add(angleField);
		upperPanel.add(unit);
		lowerPanel.setAlignmentX(LEFT_ALIGNMENT);
		lowerPanel.add(rotateLeft);
		lowerPanel.add(rotateRight);
		
		this.add(upperPanel);
		this.add(lowerPanel);
	}

	private void nudge(double delta) {
		try {
			angleField.commitEdit();
		} catch (ParseException pe) { }
		angleField.setValue(Double.valueOf(normalize(getAngle() + delta)));
	}

	private static double normalize(double degrees) {
		degrees = degrees % 360;
		if (degrees <= -180) degrees += 360;
		if (degrees > 180) degrees -= 360;
		return degrees;
	}

	public double getAngle() {
		try {
			angleField.commitEdit();
		} catch (ParseException pe) { }
		Object value = angleField.getValue();
		if (value instanceof Number)
			return ((Number) value).doubleValue();
		return Double.parseDouble(angleField.getText());
	}
}

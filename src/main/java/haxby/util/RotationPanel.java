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

	public RotationPanel() {
		this(0.0);
	}

	public RotationPanel(double degrees) {
		format = new DecimalFormat("#0.###");
		angleField = new JFormattedTextField(format);
		angleField.setColumns(6);
		info = new JLabel("Rotate image (clockwise) by");
		unit = new JLabel("\u00B0");
		angleField.setValue(Double.valueOf(normalize(degrees)));

		this.add(info);
		this.add(angleField);
		this.add(unit);
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

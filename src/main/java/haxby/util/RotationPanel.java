package haxby.util;

import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;

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
		format = new DecimalFormat();
		angleField = new JFormattedTextField(format);
		info = new JLabel("Rotate image (counterclockwise) by");
		unit = new JLabel("\u00B0");
		angleField.setText(String.valueOf(degrees));
		this.add(info);
		this.add(angleField);
		this.add(unit);
	}
	
	public double getAngle() {
		return Double.valueOf(angleField.getText());
	}
	
	public AffineTransform getRotationMatrix(double[] wesn) {
		if(null == wesn || 4 > wesn.length) return null;
		double midX = (wesn[0] + wesn[1])/2;
		double midY = (wesn[2] + wesn[3])/2;
		return AffineTransform.getRotateInstance(Double.valueOf(angleField.getText()), midX, midY);
	}
}

package haxby.db.shore;

import javax.swing.*;
import java.awt.*;

public class ShoreOptionPanel extends JPanel
{
	private ShoreLine shore;
	private float lastWidth;
	private Color lastColor;
	private boolean isVisible;

	private JCheckBox draw;
	private JTextField width;
	private JColorChooser color;

	public ShoreOptionPanel(ShoreLine aShore) {
		shore = aShore;
		lastWidth = shore.getWidth();
		lastColor = shore.getColor();
		isVisible = shore.isVisible();

		this.setLayout(new BorderLayout());

		JPanel top = new JPanel( new FlowLayout());

		draw = new JCheckBox("Draw Antartic Shoreline", shore.isVisible());
		top.add(draw);

		JLabel widthPrompt = new JLabel("Shoreline width: ");
		top.add(widthPrompt);

		width = new JTextField( Float.toString( shore.getWidth() ));
		top.add(width);

		this.add(top, BorderLayout.NORTH);

		color = new JColorChooser(shore.getColor());
		color.setPreviewPanel(new JLabel("------Sample Color-----"));
		this.add(color, BorderLayout.CENTER);
	}

	public void ok() {
		shore.setWidth( Float.parseFloat(width.getText()) );
		shore.setVisible(draw.isSelected());
		shore.setColor(color.getColor());
	}

	public void preview() {
		this.ok();
	}

	public void reset() {
		this.cancel();
	}

	public void cancel() {
		shore.setWidth(lastWidth);
		shore.setColor(lastColor);
		shore.setVisible(isVisible);

		draw.setSelected(isVisible);
		width.setText( Float.toString(lastWidth) );
		color.setColor( lastColor);
	}

	public void defaults() {
		shore.setWidth(3f);
		shore.setColor(Color.blue);
		shore.setVisible(false);

		draw.setSelected(false);
		width.setText( Float.toString(3f) );
		color.setColor(Color.blue);
	}
}

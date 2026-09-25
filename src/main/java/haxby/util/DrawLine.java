package haxby.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

public class DrawLine extends JPanel {
	private int x0, x1, y0, y1;
	private Color color;
	private float lineThick;
	
	public DrawLine(int x0, int y0, int x1, int y1, Color color, float lineThick) {
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
		this.color = color;
		this.lineThick = lineThick;
	}
	
	public DrawLine(int x0, int y0, int x1, int y1) {
		this(x0, y0, x1, y1, Color.BLACK, 1f);
	}
	
	public DrawLine(int x0, int y0, int x1, int y1, Color color) {
		this(x0, y0, x1, y1, color, 1f);
	}
	

	public void paintComponent (Graphics g) {
		
		Graphics2D g2d = (Graphics2D) g;
	    g2d.setColor(color);
	    g2d.setStroke(new BasicStroke(lineThick));
	    g2d.drawLine(x0, y0, x1, y1);
	}
	
}

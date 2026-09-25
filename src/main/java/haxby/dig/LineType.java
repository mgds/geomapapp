package haxby.dig;

import java.awt.*;

public class LineType {
	public String name;
	public Color color, fill;
	public BasicStroke stroke;
	public LineType(String name, Color color, Color fill, BasicStroke stroke) {
		this.name = name;
		this.color = color;
		this.fill = fill;
		this.stroke = stroke;
	}
	public String toString() { return name; }
}
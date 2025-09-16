package org.geomapapp.util;

import java.awt.*;
public class SimpleBorder implements javax.swing.border.Border {
	boolean selected;
	static SimpleBorder border = null;
	public SimpleBorder() {
		selected = false;
	}
	public static SimpleBorder getBorder() {
		if( border==null )border = new SimpleBorder();
		return border;
	}
	public SimpleBorder(boolean selected) {
		this.selected = selected;
	}
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		if(selected) {
			g.setColor(Color.darkGray);
		} else {
			g.setColor(Color.white);
		}
		g.fillRect(x,y,width-1,1);
		g.fillRect(x,y,1,height-1);
		if(selected) {
			g.setColor(Color.white);
		} else {
			g.setColor(Color.darkGray);
		}
		g.fillRect(x+1,y+height-1,width-1,1);
		g.fillRect(x+width-1, y+1, 1, height-1);
	}
	public void setSelected(boolean tf) {
		selected = tf;
	}
	public Insets getBorderInsets(Component c) {
		return new Insets(1,1,1,1);
	}
	public boolean isBorderOpaque() {
		return true;
	}
}

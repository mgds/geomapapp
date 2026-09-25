package org.geomapapp.util;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

public class XBIcon implements Icon {
//	Icon icon;
	BufferedImage image;
	boolean selected;
	public XBIcon(BufferedImage image, boolean selected) {
	//	icon = new ImageIcon(image);
		this.image = image;
		this.selected = selected;
	}
	public XBIcon(Icon icon, boolean selected) {
	//	this.icon = icon;
		if( icon instanceof XBIcon ) image=((XBIcon)icon).getImage();
		else image = new BufferedImage(22,22,BufferedImage.TYPE_INT_RGB);
		this.selected = selected;
	}
	public int getIconWidth() {
		return image.getWidth()+2;
//		return icon.getIconWidth()+2;
	}
	public int getIconHeight() {
		return image.getHeight()+2;
	//	return icon.getIconHeight()+2;
	}
	public void setImage(BufferedImage image) {
	//	icon = new ImageIcon(image);
		this.image=image;
	}
	public BufferedImage getImage() {
		return image;
	//	try {
	//		return (BufferedImage)((ImageIcon)icon).getImage();
	//	} catch(Exception ex) {
	//		BufferedImage image = new BufferedImage(
	//			icon.getIconWidth(), icon.getIconHeight(),
	//			BufferedImage.TYPE_INT_RGB);
	//		icon.paintIcon( null, image.createGraphics(),
	//			0, 0);
	//		return image;
	//	}
	}
	public void paintIcon(Component c, Graphics g, int x, int y) {
		SimpleBorder border = SimpleBorder.getBorder();
		border.setSelected(selected);
		border.paintBorder(c, g, x, y, getIconWidth(), getIconHeight());
		g.drawImage(image, x+1,y+1, c);
	//	icon.paintIcon( null, g, x+1, x+1);
	}
}
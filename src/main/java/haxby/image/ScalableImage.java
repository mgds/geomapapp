package haxby.image;

import java.awt.image.BufferedImage;
import java.awt.Rectangle;

public abstract interface ScalableImage {
	public Rectangle getImageableRect(Rectangle rect, 
					int xAvg, 
					int yAvg, 
					int xRep, 
					int yRep);
	public BufferedImage getScaledImage(Rectangle rect, 
					int xAvg, 
					int yAvg, 
					int xRep, 
					int yRep);
	public BufferedImage getImage();
	public void setRevVid(boolean tf);
	public void setFlip(boolean tf);
	public boolean isFlip();
}

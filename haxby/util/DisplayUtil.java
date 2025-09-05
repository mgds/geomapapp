package haxby.util;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Component;

public class DisplayUtil {
	public static void setRelativeLocation(Component toMove, int x, int y, Component relativeTo) {
		Point newLoc = (null == relativeTo)?(new Point(0,0)):(relativeTo.getLocation());
		newLoc.x += x;
		newLoc.y += y;
		if(null != relativeTo) {
			Rectangle bounds = relativeTo.getGraphicsConfiguration().getDevice().getDefaultConfiguration().getBounds();
			if(newLoc.x < bounds.x) newLoc.x = bounds.x;
			if(newLoc.y < bounds.y) newLoc.y = bounds.y;
			if(newLoc.x + toMove.getWidth() > bounds.x + bounds.width) newLoc.x = bounds.x + bounds.width - toMove.getWidth();
			if(newLoc.y + toMove.getHeight() > bounds.y + bounds.height) newLoc.y = bounds.y + bounds.height - toMove.getHeight();
		}
		toMove.setLocation(newLoc);
	}
}

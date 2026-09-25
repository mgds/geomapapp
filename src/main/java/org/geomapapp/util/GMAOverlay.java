package org.geomapapp.util;

import java.awt.Graphics2D;

public abstract interface GMAOverlay {
	public abstract void draw(Graphics2D g, 
				ScalableComponent comp);
}

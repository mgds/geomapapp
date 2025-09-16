/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package haxby.worldwind.layers;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.UserFacingIcon;
import gov.nasa.worldwind.render.WWIcon;
import gov.nasa.worldwind.util.Logging;
import haxby.worldwind.renderers.DetailedIconRenderer;

import java.awt.Color;
import java.util.concurrent.ConcurrentLinkedQueue;


public class DetailedIconLayer extends AbstractLayer
{
	// public 
	private final java.util.Collection<WWIcon> icons = 
			new ConcurrentLinkedQueue<WWIcon>();
	// public 
	private DetailedIconRenderer iconRenderer = new DetailedIconRenderer();

	public DetailedIconLayer()
	{
	}

	public void addIcon(DetailedIcon icon)
	{
		if (icon == null)
		{
			String msg = Logging.getMessage("nullValue.Icon");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}

		this.icons.add(icon);
	}
	
	public void removeIcon(WWIcon icon)
	{
		if (icon == null)
		{
			String msg = Logging.getMessage("nullValue.Icon");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}

		return;
	}

	public java.util.Collection<WWIcon> getIcons()
	{
		return this.icons;
	}

	@Override
	protected void doPick(DrawContext dc, java.awt.Point pickPoint)
	{
		this.iconRenderer.pick(dc, icons, pickPoint, this);
	}

	@Override
	protected void doRender(DrawContext dc)
	{
		this.iconRenderer.render(dc, icons, getOpacity());
	}

	@Override
	public String toString()
	{
		return Logging.getMessage("layers.IconLayer.Name");
	}
	
	public static class DetailedIcon extends UserFacingIcon {
	//	protected 
		private Color iconColor = Color.white;
	//	protected 
		private Color highlightColor = Color.RED;
	//	protected 
		private int iconElevation = 0; // in meters
		
		public DetailedIcon(String iconPath, Position iconPosition)
		{
			super(iconPath, iconPosition);
		}
		
		public Color getIconColor() {
				return this.iconColor;
		}
			
		public void setIconColor(Color c) {
			this.iconColor  = c;
		}

		public int getIconElevation() {
			return iconElevation;
		}

		public void setIconElevation(int iconDepth) {
			this.iconElevation = iconDepth;
		}

		public Color getHighlightedIconColor() {
			return highlightColor;
		}
		
		public void setHighlightColor(Color highlightColor) {
			this.highlightColor = highlightColor;
		}
	}
}

package haxby.worldwind.layers.dynamic_tiler;

import gov.nasa.worldwind.layers.TextureTile;
import gov.nasa.worldwind.render.DrawContext;

public interface DynamicTiler {
	public TextureTile requestTextureTile(final TextureTile tile);
	public TextureTile retriveTextureTile(DrawContext dc,final TextureTile tile);
	public boolean holdsTexture(DrawContext dc, final TextureTile tile);
	public void disposeInvalids(DrawContext dc);
}

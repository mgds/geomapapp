package haxby.worldwind.layers.dynamic_tiler;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.TextureTile;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.OGLUtil;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureData;

public abstract class AbstractTrackTiler implements DynamicTiler {
	
	private final Object fileLock = new Object();

	public void disposeInvalids(DrawContext dc) {
		
	}
	
	public boolean holdsTexture(DrawContext dc, TextureTile tile) {
		TextureTile tt = requestTextureTile(tile);
		
		if (tt == null)
			return false;
		
		if (tt.getTextureData() == null &&
				!tt.isTextureInMemory(dc.getTextureCache()))
			return false;

		return true;
	}

	public TextureTile requestTextureTile(TextureTile tile) {
		return (TextureTile) 
			WorldWind.getMemoryCache(TextureTile.class.getName())
				.getObject(tile.getTileKey());
	}

	public TextureTile retriveTextureTile(DrawContext dc, TextureTile tile) {
		// Create the tile bounds from the tile sector
		Sector s = tile.getSector();
		Rectangle2D tileBounds = new Rectangle2D.Double(s.getMinLongitude().degrees, s.getMinLatitude().degrees,
				s.getDeltaLonDegrees(), s.getDeltaLatDegrees());

		final URL textureURL = 
			WorldWind.getDataFileStore().findFile(tile.getPath(), true);
		
		// Tile is in dataFileCache
		if (textureURL != null) 
		{
			if (loadTexture(tile, textureURL))
			{
				return tile;
			}
			else
			{
				 // Assume that something's wrong with the file and delete it.
                WorldWind.getDataFileStore().removeFile(textureURL);
                String message = Logging.getMessage("generic.DeletedCorruptDataFile", textureURL);
                Logging.logger().info(message);
			}
		}
		
		BufferedImage img = drawTracksInBounds(tile.getLevelNumber(), tileBounds);
		
		
		// Try to get this tile from memory
		TextureTile tileB = (TextureTile) 
			WorldWind.getMemoryCache(TextureTile.class.getName())
				.getObject(tile.getTileKey());
		
		if (tileB != null)
			// A tile has been found, use that
			tile = tileB;
		else {
			// No tile could be found, use this one
			WorldWind.getMemoryCache(TextureTile.class.getName())
				.add(tile.getTileKey(), tile);
		}
		GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
		// Create a new TextureData from the rendered image of the grid
		TextureData td = new AWTTextureData(gl.getGLProfile(),GL.GL_RGBA, GL.GL_RGBA, false, img);
		
		// Set the new tile texture
		tile.setTextureData(td);
		
		return tile;
	}

	
	public abstract BufferedImage drawTracksInBounds(int level, Rectangle2D tileBounds);
	
	private boolean loadTexture(TextureTile tile, java.net.URL textureURL)
    {
        TextureData textureData;

        synchronized (this.fileLock)
        {
            textureData = readTexture(textureURL);
        }

        if (textureData == null)
            return false;

        tile.setTextureData(textureData);
        this.addTileToCache(tile);

        return true;
    }
	
	private static TextureData readTexture(URL url)
    {
        try
        {
        	 return OGLUtil.newTextureData(Configuration.getMaxCompatibleGLProfile(), url, (Boolean) null);
            //return TextureIO.newTextureData(url, false, null);
        }
        catch (Exception e)
        {
            Logging.logger().log(
                java.util.logging.Level.SEVERE, "layers.TextureLayer.ExceptionAttemptingToReadTextureFile", e);
            return null;
        }
    }

	private void addTileToCache(TextureTile tile)
    {
        WorldWind.getMemoryCache(
        		TextureTile.class.getName()).add(tile.getTileKey(), tile);
    }
}

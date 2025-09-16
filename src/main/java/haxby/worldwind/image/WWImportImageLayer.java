package haxby.worldwind.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.SurfaceImage;
import haxby.layers.image.GeographicImageOverlay;
import haxby.layers.image.ImportImageLayer;
import haxby.layers.image.MercatorImageOverlay;
import haxby.map.FocusOverlay;
import haxby.map.MapApp;
import haxby.worldwind.WWMapApp;
import haxby.worldwind.layers.SurfaceImageLayer;

public class WWImportImageLayer extends ImportImageLayer {

	@Override
	protected void importImage(MapApp mapApp, File file) {
		ImageWESNProj wesn = showWESNDialog(mapApp.getFrame());
		if (wesn == null) return;
		
		try {
			BufferedImage image = ImageIO.read(file);
			
			if (wesn.merc)
				image = ImageResampler
					.MERCATOR_TO_GEOGRAPHIC
					.resampleImage(image, 
							wesn.wesn[2], 
							wesn.wesn[3], 
							wesn.wesn[2], 
							wesn.wesn[3], image.getHeight());
			
			SurfaceImageLayer layer = new SurfaceImageLayer();
			layer.setName( file.getName() );
			
			Sector sector = Sector.fromDegrees(wesn.wesn[2],
					wesn.wesn[3], 
					wesn.wesn[0],
					wesn.wesn[1]);
			
			SurfaceImage si = new SurfaceImage(image, sector);
			layer.setSurfaceImage(si);
			
			((WWMapApp) mapApp).makeLayerVisible(layer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

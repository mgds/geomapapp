package haxby.layers.image;

import java.awt.image.BufferedImage;

public class GeoRefImage {
	public ImageProvider source;
	public double[] wesn;
	public int maxRes = -1;
	public int maxViewRes = Integer.MAX_VALUE;
	public int minViewRes = 1;
	
	public GeoRefImage(ImageProvider source, double[] wesn) {
		this(source, wesn, 1, Integer.MAX_VALUE);
	}
	
	public GeoRefImage(BufferedImage image, double[] wesn) {
		this(new ImageProvider.SimpleImageProvider(image), wesn);
	}
	
	public GeoRefImage(ImageProvider source, double[] wesn, 
			int minViewRes,
			int maxViewRes) {
		this.source = source;
		this.wesn = wesn;
		this.minViewRes  = minViewRes;
		this.maxViewRes = maxViewRes;
		
		while (this.wesn[0] > 180)
			this.wesn[0] -= 360;
		while (this.wesn[1] > 180)
			this.wesn[1] -= 360;
	}

	public BufferedImage getImage() {
		BufferedImage image = source.getImage();
		if (maxRes == -1) 
		{
			double dLat = wesn[3] - wesn[2];
			double dLon = wesn[1] - wesn[0];
			
			double ppdLat = image.getHeight() / dLat;
			double ppdLon = image.getWidth() / dLon;
			
			double ppdImage = Math.max(ppdLat, ppdLon);
			
			int zoom = 1;
			double ppd = 640 / 360.;
			
			while (ppd < ppdImage) {
				zoom *= 2;
				ppd *= 2;
			}
			
			maxRes = zoom;
		}
		
		return image;
	}
}

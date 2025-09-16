package haxby.worldwind.image;

import java.awt.image.BufferedImage;

public abstract class ImageResampler {
	public abstract BufferedImage resampleImage(BufferedImage sample, double sampleMinLat, double sampleMaxLat, double tileMinLat, double tileMaxLat, int tileSize);
	
	public static ImageResampler MERCATOR_TO_GEOGRAPHIC = 
		new ImageResampler() {
			public BufferedImage resampleImage(BufferedImage sample, double sampleMinLat, double sampleMaxLat, double tileMinLat, double tileMaxLat, int tileSize) {
	 			BufferedImage img = new BufferedImage(sample.getWidth(), tileSize, sample.getType());
				double tileScale_DegreesPerPixel = (tileMinLat - tileMaxLat) / tileSize;
	
				// Y = 0 is equator
				double sampleMinY = Math.log( Math.tan(Math.toRadians(sampleMinLat)) + 
										1 / Math.cos(Math.toRadians(sampleMinLat)) );
				double sampleMaxY = Math.log( Math.tan(Math.toRadians(sampleMaxLat)) + 
										1 / Math.cos(Math.toRadians(sampleMaxLat)) );
				
				double sampleYDelta = sampleMinY - sampleMaxY;
	
				int rgb[] = new int[img.getWidth()];
				for (int tileRow = 0; tileRow < tileSize; tileRow++) {
					double lat = tileScale_DegreesPerPixel * tileRow + tileMaxLat;
					
					if (lat > 81) continue;
					if (lat < -79) continue;
					
					lat = Math.toRadians(lat);
					
					double sampleY = (Math.log(Math.tan(lat) + 1 / Math.cos(lat)));
					
					double sampleRatio = (sampleY - sampleMaxY) / sampleYDelta;
	//				System.out.println(Math.toDegrees(lat) + "\t" + sampleY + "\t" + sampleRatio);
					
					if (sampleRatio > 1 || sampleRatio < 0) {
						System.out.println("Outside sample range");
						continue;
					}
					
					int sampleRow = (int) Math.floor(sampleRatio * sample.getHeight());
					rgb = sample.getRGB(0, sampleRow, sample.getWidth(), 1, rgb, 0, 1);
					img.setRGB(0, tileRow, img.getWidth(), 1, rgb, 0, 1);
				}
				
				return img; 
			}
	};
}

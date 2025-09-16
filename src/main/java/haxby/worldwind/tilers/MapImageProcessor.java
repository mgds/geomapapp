/**
 * 
 */
package haxby.worldwind.tilers;

import java.awt.image.BufferedImage;

public interface MapImageProcessor {
	public BufferedImage processImage(BufferedImage image, double[] resultWESN, double[] imageWESN);
	
	public static class MercatorResizeAndResample implements MapImageProcessor {
		private final int TILE_SIZE;
		
		public MercatorResizeAndResample(int tileSize) {
			this.TILE_SIZE = tileSize;
		}
		
		public BufferedImage processImage(BufferedImage image,
				double[] resultWESN, double[] imageWESN) {
			BufferedImage resampled = resampleImage(image, imageWESN[2], imageWESN[3], resultWESN[2], resultWESN[3]);
			return resizeImage(resampled, resultWESN[2], resultWESN[3], resultWESN[2], resultWESN[3]); 
		}
		
		private BufferedImage resizeImage(BufferedImage sample, double sampleMinLat, double sampleMaxLat, double tileMinLat, double tileMaxLat) {
			BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_3BYTE_BGR);
			double tileScale = TILE_SIZE / (tileMinLat - tileMaxLat);
			
			// s for source; d for destination; all measurements in pixels
			int sx1 = 0;
			int sy1 = 0;
			int sx2 = sample.getWidth();
			int sy2 = sample.getHeight();
			
			int dx1 = 0;
			int dy1 = (int) ((sampleMaxLat - tileMaxLat) * tileScale);
			int dx2 = TILE_SIZE;
			int dy2 = (int) ((sampleMinLat - tileMaxLat) * tileScale);
			
			img.createGraphics().drawImage(sample, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
			
			return img;
		}
		
		private BufferedImage resampleImage(BufferedImage sample, double sampleMinLat, double sampleMaxLat, double tileMinLat, double tileMaxLat) {
 			BufferedImage img = new BufferedImage(sample.getWidth(), TILE_SIZE, BufferedImage.TYPE_3BYTE_BGR);
			double tileScale_DegreesPerPixel = (tileMinLat - tileMaxLat) / TILE_SIZE;

			// Y = 0 is equator
			double sampleMinY = Math.log( Math.tan(Math.toRadians(sampleMinLat)) + 
									1 / Math.cos(Math.toRadians(sampleMinLat)) );
			double sampleMaxY = Math.log( Math.tan(Math.toRadians(sampleMaxLat)) + 
									1 / Math.cos(Math.toRadians(sampleMaxLat)) );
			
			double sampleYDelta = sampleMinY - sampleMaxY;

			int rgb[] = new int[img.getWidth()];
			for (int tileRow = 0; tileRow < TILE_SIZE; tileRow++) {
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
	}
}
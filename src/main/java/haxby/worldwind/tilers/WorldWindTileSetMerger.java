package haxby.worldwind.tilers;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import javax.imageio.ImageIO;

public class WorldWindTileSetMerger {
	
	private File fromTileSet, toTileSet;
	private double minLat, maxLat;
	private int levelZeroDelta;

	private boolean isScalingTileLongitude = true;
	
	public WorldWindTileSetMerger(File fromTileSet, File toTileSet,
			double minLat, double maxLat, int levelZeroDelta) {
		this.fromTileSet = fromTileSet;
		this.toTileSet = toTileSet;
		this.minLat = minLat;
		this.maxLat = maxLat;
		this.levelZeroDelta = levelZeroDelta;
	}
	
	public void mergeLevel(int level) throws IOException {
		int tileY0 = getTileNumY(minLat, level, levelZeroDelta);
		int tileY1 = getTileNumY(maxLat, level, levelZeroDelta);

		tileY0 = Math.max(tileY0, 0);
		tileY1 = Math.max(tileY1, 0);
		
		int maxTileY = (int)Math.round(180 / (levelZeroDelta / Math.pow(2, level))) - 1;
		tileY0 = Math.min(tileY0, maxTileY);
		tileY1 = Math.min(tileY1, maxTileY);
		
		File to = new File(toTileSet, level + "/");
		if (!to.exists())
			if (!to.mkdir())
				throw new IOException("Could not make directory " + to);

		mergeRow(tileY0, level);
		
		if (tileY0 != tileY1)
			mergeRow(tileY1, level);
			
		for (int tileY = tileY0 + 1; tileY < tileY1; tileY++)
			copyRow(tileY, level);
	}
	
	private void copyRow(int tileY, int level) throws IOException {
		File from = new File(fromTileSet, level + "/" + tileY + "/");
		if (!from.exists())
			return;
		
		File to = new File(toTileSet, level + "/" + tileY + "/");
		if (!to.exists())
			if (!to.mkdir())
				throw new IOException("Could not make directory " + to);
		
		for (File file : from.listFiles())
			copyFile(file, to);
	}

	private void copyFile(File file, File toDirectory) throws IOException {
		File output = new File(toDirectory, file.getName());
		
		FileChannel src = new FileInputStream(file).getChannel();
		FileChannel dest = new FileOutputStream(output).getChannel();
		
		src.transferTo(0, src.size(), dest);
		src.close();
		dest.close();
	}

	private void mergeRow(int tileY, int level) throws IOException {
		File fromRow = new File(fromTileSet, level + "/" + tileY + "/");
		File toRow = new File(toTileSet, level + "/" + tileY + "/");
		
		if (!toRow.exists())
			if (!toRow.mkdir())
				throw new IOException("Could not make directory " + toRow);
		
		double delta = levelZeroDelta / Math.pow(2, level);
		double lat0 = getLatFromTileY(tileY, level, levelZeroDelta);
		double lat1 = lat0 + delta;
		
		int tileX0 = getTileNumX(lat0, lat1, -180, level, levelZeroDelta);
		int tileX1 = getTileNumX(lat0, lat1, 180, level, levelZeroDelta);
		
		for (int tileX = tileX0; tileX < tileX1; tileX++) {
			mergeTile(fromRow, toRow, tileX, tileY, level, lat0, lat1);
		}
	}

	private void mergeTile(File fromRow, File toRow, int tileX, int tileY, int level, double tileLat0, double tileLat1) throws IOException {
		BufferedImage readTile = loadTileFrom(fromRow, tileX, tileY, level);
		BufferedImage writeTile = loadTileFrom(toRow, tileX, tileY, level);
		
		if (readTile == null || writeTile == null) {
			System.out.println("null tile");
			return;
		}
		
		int tile_size = readTile.getHeight();
		
		double delta = tileLat1 - tileLat0;
		
		double startPercent = 1 - ((maxLat - tileLat0) / delta);
		double endPercent = 1 - ((minLat - tileLat0) / delta);
		int startPixel = (int)Math.floor(startPercent * tile_size); 
		int endPixel = (int)Math.ceil(endPercent * tile_size);
		
		startPixel = Math.max(0, startPixel);
		endPixel = Math.min(tile_size, endPixel);
		
		if (startPixel >= endPixel) return;
		
		BufferedImage cutTile = readTile.getSubimage(0, startPixel, tile_size, endPixel - startPixel); 
		writeTile.createGraphics().drawImage(cutTile, 0, startPixel, null);
		
		StringBuffer sb = new StringBuffer().append(tileY).append("_").append(tileX).append(".jpg");
		ImageIO.write(
				writeTile, 
				"jpg", 
				new File(toRow, sb.toString()));
	}
	

	private BufferedImage loadTileFrom(File row, int tileX, int tileY, int level) throws IOException {
		if (level < 0 || row == null)
			return null;
		
		StringBuffer sb = new StringBuffer().append(tileY).append("_").append(tileX).append(".jpg");
		
		File tile = new File(row, sb.toString());
		if (tile.exists()) {
			BufferedImage img = ImageIO.read(tile);
			if (img == null) 
				System.err.println("Could not read image " + tile);
			
			return img;
		}

		if (level == 0)
			return null;
		
		File root = row.getParentFile().getParentFile();
		File parentLevel = new File(root, Integer.toString(level - 1));
		
		int parentTileY = tileY / 2;
		double delta = levelZeroDelta / Math.pow(2, level);
		
		double parentLat0 = getLatFromTileY(parentTileY, level - 1, levelZeroDelta);
		double parentLat1 = parentLat0 + delta;
		double parentLat2 = parentLat1 + delta;
		
		int multiplier0 = getLonMultiplier(parentLat0, parentLat1);
		int multiplier1 = getLonMultiplier(parentLat1, parentLat2);

		boolean normalSplit = multiplier0 == multiplier1;
		boolean lowerSplit = multiplier0 < multiplier1;
		
		double tileLat0 =  getLatFromTileY(tileY, level, levelZeroDelta);
		
		boolean tileIsLower = Math.abs(parentLat0 - tileLat0) <
					Math.abs(parentLat1 - tileLat0);
		
		int parentTileX = tileX;
		
		if (normalSplit)
			parentTileX /= 2;
		else if (lowerSplit && tileIsLower)
			parentTileX /= 2;
		else if (!lowerSplit && !tileIsLower)
			parentTileX /= 2;
		
		File parentRow = new File(parentLevel, new StringBuffer().append(parentTileY).append("/").toString());
		
//		System.out.println(parentRow + "\t" + row + "\t" + parentTileX + "\t" + tileX);
		BufferedImage img = loadTileFrom(parentRow, parentTileX, parentTileY, level - 1);
//		System.out.println("done recurse");
		
		if (img == null) return null;
		
		int width = img.getWidth();
		int height = img.getHeight();
		
		BufferedImage scratch = new BufferedImage(width, height, img.getType());

		int sx1, sx2, sy1, sy2;
		
		if (tileIsLower) {
			sy1 = img.getHeight() / 2;
			sy2 = img.getHeight();
		} else {
			sy1 = 0;
			sy2 = img.getHeight() / 2;
		}
		
		boolean isLeftTile = tileX % 2 == 0;
		
		if (normalSplit || 
				(lowerSplit && tileIsLower) ||
				(!lowerSplit && !tileIsLower))
			if (isLeftTile) {
				sx1 = 0;
				sx2 = width / 2;
			}
			else {
				sx1 = width / 2;
				sx2 = width;
			}
		else {
			sx1 = 0;
			sx2 = width;
		}
		
		scratch.createGraphics().drawImage(img, 0, 0, width, height, sx1, sy1, sx2, sy2, null);
			
		return scratch;
	}

	public void setScalingTileLongitude(boolean isScalingTileLongitude) {
		this.isScalingTileLongitude = isScalingTileLongitude;
	}
	
	private int getLonMultiplier(double lat0, double lat1) {
		if (!isScalingTileLongitude ) return 1;
		
		int multiplier = 1;
		double maxLat = Math.min(Math.abs(lat0), Math.abs(lat1));
		double radius = Math.cos(Math.toRadians(maxLat));
		while (multiplier * 2 * radius < 1)
			multiplier *= 2;
		return multiplier;
	}
	
	public double getLonFromTile(double lat0, double lat1, int tileX, int level, int levelZeroDelta) {
		double delta = levelZeroDelta / Math.pow(2, level) * getLonMultiplier(lat0, lat1);
		return tileX * delta - 180;
	}
	
	public int getTileNumX(double lat0, double lat1, double lon, int level, int levelZeroDelta) {
		double delta = levelZeroDelta / Math.pow(2, level) * getLonMultiplier(lat0, lat1);
		return (int) Math.floor((lon + 180) / delta);
	}
	
	public static double getLatFromTileY(int tileY, int level, int levelZeroDelta) {
		double delta = levelZeroDelta / Math.pow(2, level);
		return tileY * delta - 90;
	}
	
	public static int getTileNumY(double lat, int level, int levelZeroDelta) {
		double delta = levelZeroDelta / Math.pow(2, level);
		return (int) Math.floor((lat + 90) / delta); 
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length < 4) {
			System.err.println("Ussage: WorldWindTileSetMerger fromTileSet toTileSet minLat maxLat [# of levels to merge]");
			System.exit(-1);
		}
		
		File fromTileSet = new File(args[0]);
		File toTileSet = new File(args[1]);
		
		double minLat = Double.parseDouble(args[2]);
		double maxLat = Double.parseDouble(args[3]);
		
		int level;
		if (args.length == 5)
			level = Integer.parseInt(args[4]);
		else
			level = 1;
		
		WorldWindTileSetMerger merger = new WorldWindTileSetMerger(fromTileSet,toTileSet, minLat, maxLat, 36);

		for (int i = 0; i < level; i++)
			merger.mergeLevel(i);
	}
}

package haxby.image;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;

public class SunRaster {
	public static BufferedImage readSunRaster(String file) throws IOException {
		DataInputStream stream = null;
		int blue, green, red;
		Color c;
		try {
			stream = new DataInputStream(
				new BufferedInputStream(
				new FileInputStream(file)));
			if(stream.readInt() != 1504078485) throw new IOException("not a 24 bit sunraster file");
			int width = stream.readInt();
			int height = stream.readInt();
			if(stream.readInt() != 24) throw new IOException("not a 24 bit sunraster file");
			for(int i=0 ; i<4 ; i++)stream.readInt();
			BufferedImage image = new BufferedImage(width, height, 
					BufferedImage.TYPE_INT_RGB);
			boolean extra = (width%2==0 ? false : true);
			int rgb;
			for(int y=0 ; y<height ; y++) {
				for(int x=0 ; x<width ; x++) {
					blue = stream.readUnsignedByte();
					green = stream.readUnsignedByte();
					red = stream.readUnsignedByte();
				//	c = new Color(red, green, blue);
					rgb = (255<<24) | (red<<16) | (green<<8) | blue;
					image.setRGB(x, y, rgb);
				}
				if(extra)stream.readUnsignedByte();
			}
			return image;
		} catch(IOException e) {
			throw e;
		} finally {
			try {
				stream.close();
			} catch(IOException e) {
			}
		}
	}
	public static void saveSunRaster(String file, BufferedImage image) throws IOException {
		DataOutputStream stream = null;
		int blue, green, red;
		Color c;
		int width = image.getWidth();
		int height = image.getHeight();
		try {
			stream = new DataOutputStream(
				new BufferedOutputStream(
				new FileOutputStream(file)));
			stream.writeInt(1504078485);
			stream.writeInt(width);
			stream.writeInt(height);
			stream.writeInt(24);
			stream.writeInt(width*height);
			stream.writeInt(1);
			stream.writeInt(0);
			stream.writeInt(0);
			boolean extra = (width%2==0 ? false : true);
			for(int y=0 ; y<height ; y++) {
				for(int x=0 ; x<width ; x++) {
					int i = image.getRGB(x,y);
					stream.writeByte(i & 255);
					stream.writeByte(i>>8 & 255);
					stream.writeByte(i>>16 & 255);
				}
				if(extra)stream.writeByte(0);
			}
		} catch(IOException e) {
			throw e;
		} finally {
			try {
				stream.close();
			} catch(IOException e) {
			}
		}
	}
}

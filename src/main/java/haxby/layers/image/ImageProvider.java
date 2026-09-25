package haxby.layers.image;

import haxby.util.URLFactory;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

public abstract class ImageProvider {
	protected BufferedImage image;
	public abstract BufferedImage getImage();
	
	public static class SimpleImageProvider extends ImageProvider {
		public SimpleImageProvider(BufferedImage image) {
			this.image = image;
		}
		public BufferedImage getImage() {
			return image;
		}
	}
	
	public static class ZipImageProvider extends ImageProvider {
		private File zipFile;
		private String imageName;

		public ZipImageProvider(String imageName, File zipFile) {
			this.imageName = imageName;
			this.zipFile = zipFile;
		}
		
		public BufferedImage getImage() {
			if (image != null) return image;
			
			System.out.println(imageName);
			
			ZipInputStream zis;
			try {
				zis = new ZipInputStream(
						new BufferedInputStream(
								new FileInputStream(zipFile)));
			} catch (FileNotFoundException e1) {
				image = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);
				return image;
			}
			
			ZipEntry ze = null;
			try {
				while ( (ze = zis.getNextEntry()) != null )
					if (ze.getName().equals(imageName))
						break;
			} catch (IOException e1) {
				image = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);
				return image;
			}
			
			if (ze == null) {
				image = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);
				return image;
			}
			
			try {
				image = ImageIO.read(zis);
			} catch (IOException e) {
				image = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);
			}
			
			imageName = null;
			zipFile = null;
			try {
				zis.close();
			} catch (IOException e) {
			}
			return image;
		}
	}
	
	public static class FileImageProvider extends ImageProvider 
	{
		private File imageFile;
		
		public FileImageProvider(File imageFile) {
			this.imageFile = imageFile;
		}
		
		public BufferedImage getImage() {
			if (image != null) return image;
			
			System.out.println(imageFile);
			
			try {
				image = ImageIO.read(imageFile);
			} catch (IOException e) {
				image = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);
			}
			imageFile = null;
			
			return image;
		}
	}
	
	public static class URLImageProvider extends ImageProvider 
	{
		private String urlString;
		
		public URLImageProvider(String urlString) {
			this.urlString = urlString;
		}
		
		public BufferedImage getImage() {
			if (image != null) return image;
			
			URL url;
			try {
				url = URLFactory.url(urlString);
				System.out.println(url);
				image = ImageIO.read(url);
			} catch (IOException e) {
				image = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);
			}
			
			urlString = null;
			
			return image;
		}
	}
}

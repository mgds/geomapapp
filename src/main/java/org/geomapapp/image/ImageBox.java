package org.geomapapp.image;

import haxby.util.URLFactory;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

public class ImageBox extends JComponent {
	private Image image;
	private String imgPath;

	public ImageBox(String img) {
		if (img!=null) {
			try { 
				image = ImageIO.read(URLFactory.url(img));
				imgPath = img;
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	public void setImage(String img) {
		if (img!=null) {
			if (imgPath==null || !imgPath.equals(img)) {
				try { 
					image = ImageIO.read(URLFactory.url(img));
					imgPath = img;
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		} else {
			image = null;
		}
		repaint();
	}

	public void setImage(URL url) {
		if (imgPath==null || !imgPath.equals(url.getPath())) {
			try { 
				image = ImageIO.read(url);
				imgPath = url.getPath();

				SwingUtilities.invokeLater( new Runnable() {
					public void run() {
						ImageBox.this.repaint();
					}
				});
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	public void paint(Graphics g2) {
		if (imgPath == null) return;
		Graphics2D g = (Graphics2D) g2;
		double w = getSize().getWidth();
		double h = getSize().getHeight();
		double w2 = image.getWidth(null);
		double h2 = image.getHeight(null);
		double s = Math.min(w / w2, h / h2);

		AffineTransform at = g.getTransform();
		g.scale(s, s);
		g.drawImage(image, 0, 0, null);
		g.setTransform(at);
	}

	public Dimension getPreferredSize() {
		return new Dimension(200,200);
	}

	public static void main(String[] args) {
		JDialog dlg = new JDialog();
		dlg.add(new ImageBox("https://www.google.com/logos/winter_holiday05_1.gif"));
		dlg.pack();
		dlg.setVisible(true);
	}
}

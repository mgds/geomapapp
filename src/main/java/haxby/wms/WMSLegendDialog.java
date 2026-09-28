package haxby.wms;

import haxby.util.URLFactory;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

public class WMSLegendDialog {
	private JFrame dialog;
	private JScrollPane scrollPane;
	private BufferedImage[] legends;
	private int maxWidth;
	private int sumHeight;

	public WMSLegendDialog(Frame owner, String[][] legendURLs, String name) {
		List<String> flatLegendURLs = new ArrayList<String>();
		for (String[] sub_legendURL : legendURLs)
			for (String legendURL : sub_legendURL)
				flatLegendURLs.add(legendURL);

		legends = new BufferedImage[flatLegendURLs.size()];

		int i = 0;
		maxWidth = 0;
		sumHeight = 0;
		for (String legendURL : flatLegendURLs)
			try {
				System.out.println("get legend: " + legendURL);
				legendURL = legendURL.replaceAll(" ", "%20"); // filter out spaces
				legends[i] = ImageIO.read( URLFactory.url(legendURL) );
				if (legends[i] == null) return;
				maxWidth =  Math.max(legends[i].getWidth(), maxWidth);
				sumHeight += legends[i].getHeight();
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return;
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

		dialog = new JFrame();
		dialog.setTitle( name );
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

//		JPanel p = new JPanel();
//		p.add(new LegendComponent());
		scrollPane = new JScrollPane(new LegendComponent());
//		dialog.getContentPane().add(p);
		dialog.getContentPane().add(scrollPane);
		dialog.pack();
		int width = dialog.getWidth();
		int height = dialog.getHeight();
		if ( dialog.getWidth() > 250 ) {
			width = 250;
		}
		if ( dialog.getHeight() > 250 ) {
			height = 250;
		}
		dialog.setSize(width,height);
		dialog.setVisible(true);
	}
	
	private class LegendComponent extends JComponent {
		@Override
		public Dimension getPreferredSize() {
			return new Dimension(maxWidth, sumHeight);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			int y = 0;
			for (BufferedImage img : legends)
			{
				g.drawImage(img, 0, y, null);
				y += img.getHeight();
			}
		}
	}
}

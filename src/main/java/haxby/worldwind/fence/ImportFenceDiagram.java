package haxby.worldwind.fence;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.RenderableLayer;
import haxby.map.MapApp;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.URLFactory;
import haxby.worldwind.WWMapApp;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

public class ImportFenceDiagram {

	private static int count = 1;
	private static List<FileFilter> supportedImageSources = new LinkedList<FileFilter>();
	static {
		supportedImageSources.add( 
			new FileFilter() {
			public String getDescription() {
				return "Supported Image Files";
			}
			
			public boolean accept(File f) {
				if (f.isDirectory()) return true;
				
				String name = f.getName();
				String suffix = name.substring(name.lastIndexOf(".")+1);
				return ImageIO.getImageReadersBySuffix(suffix).hasNext();
			}
		});
	}
	
	private static FileFilter allSources = 
		new FileFilter() {
			public String getDescription() {
				return "Supported Image Overlay Sources";
			}
			
			public boolean accept(File f) {
				if (f.isDirectory()) 
					return true;
				for (FileFilter ff : supportedImageSources)
					if (ff.accept(f)) return true;
				
				return false;
			}
		};
		
	public static class ImportFenceDiagramDialog 
	{
		public JTextField name;
		public JTextField baseElevation;
		
		public BufferedImage image;
		public List<LatLon> nav;
		
		public ImportFenceDiagramDialog(JFrame frame) {
			final JDialog d = new JDialog(frame, "Import Fence Diagram", true);
			
			JPanel p = new JPanel(new BorderLayout());
			p.setBorder( BorderFactory.createEmptyBorder(5,5,5,5) );
			JPanel p2 = new JPanel();
			final JButton ok = new JButton("OK");
			ok.setEnabled(false);
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					d.setVisible(false);
					d.dispose();
				}
			});
			p2.add(ok);
			
			JButton b = new JButton("Cancel");
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// Null condition
					nav = null;
					image = null;
					
					d.setVisible(false);
					d.dispose();
				}
			});
			p2.add(b);
			
			b = new JButton("File Formats");
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String infoURL = PathUtil.getPath("HTML_PATH", MapApp.BASE_URL+"/gma_html/")
						+ "ImportFenceDiagramFormats.html";
					BrowseURL.browseURL(infoURL);
				}
			});
			p2.add(b);
			p.add(p2, BorderLayout.SOUTH);
			
			p2 = new JPanel(new GridLayout(0,3));
			final JLabel imageL = new JLabel("Load Image From: ");
			p2.add(imageL);
			
			b = new JButton("URL");
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// Get URL
					String input = JOptionPane.showInputDialog(d, "Image URL", null);
					if (input == null) return;
					
					
					URL url;
					try {
						url = URLFactory.url(input);
						loadImage(url.openStream());
					} catch (MalformedURLException ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(d, "Malformed URL " + ex.getMessage());
						return;
					} catch (IOException ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(d, "IOException " + ex.getMessage());
						return;
					}
					
					if (image != null && nav != null)
						ok.setEnabled(true);
					
					imageL.setText("Image Loaded!");
				}
			});
			p2.add(b);
			
			b = new JButton("File");
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// Get File
					JFileChooser chooser = MapApp.getFileChooser();
					for (FileFilter ff : supportedImageSources)
						chooser.setFileFilter(ff);
					chooser.setFileFilter(allSources);
					
					int c = chooser.showOpenDialog(d);
					if (c == JFileChooser.CANCEL_OPTION) return;
					
					File file = chooser.getSelectedFile();
					if (file == null) return;					
					
					try {
						loadImage(new FileInputStream(file));
					} catch (IOException ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(d, "IOException " + ex.getMessage());
						return;
					}
					
					if (image != null && nav != null)
						ok.setEnabled(true);					
					imageL.setText("Image Loaded!");
				}
			});
			p2.add(b);
			
			final JLabel navL = new JLabel("Load Nav From: ");
			p2.add(navL);
			
			b = new JButton("URL");
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// Get URL
					String input = JOptionPane.showInputDialog(d, "Nav URL", null);
					if (input == null) return;
					
					
					URL url;
					try {
						url = URLFactory.url(input);
						nav = loadNav(url.openStream());
					} catch (MalformedURLException ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(d, "Malformed URL " + ex.getMessage());
						return;
					} catch (IOException ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(d, "IOException " + ex.getMessage());
						return;
					}
					
					if (image != null && nav != null)
						ok.setEnabled(true);
					
					navL.setText("Nav Loaded!");
				}
			});
			p2.add(b);
			
			b = new JButton("File");
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// Get File
					JFileChooser chooser = MapApp.getFileChooser();
					
					int c = chooser.showOpenDialog(d);
					if (c == JFileChooser.CANCEL_OPTION) return;
					
					File file = chooser.getSelectedFile();
					if (file == null) return;
					
					try {
						nav = loadNav(new FileInputStream(file));
					} catch (IOException ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(d, "IOException " + ex.getMessage());
						return;
					}
					
					if (image != null && nav != null)
						ok.setEnabled(true);
					
					navL.setText("Nav Loaded!");
				}
			});
			p2.add(b);
			p.add(p2);
			
			p2 = new JPanel(new GridLayout(0,2));
			p2.add(new JLabel("Name: "));
			
			name = new JTextField("Fence Diamgram " + (count++));
			p2.add(name);

			p2.add(new JLabel("Base Elevation (km):"));

			baseElevation = new JFormattedTextField(NumberFormat.getIntegerInstance());
			baseElevation.setText("0");
			p2.add(baseElevation);

			p.add(p2, BorderLayout.NORTH);

			d.setLocationRelativeTo(frame);
			d.getContentPane().add(p);
			d.pack();
			d.setVisible(true);
		}

		protected void loadImage(InputStream openStream) throws IOException{
			image = ImageIO.read(new BufferedInputStream(openStream));
		}
	}

	public static void importFenceDiagram(JFrame frame, WorldWindow ww, WWMapApp mapApp) {
		ImportFenceDiagramDialog dialog = new ImportFenceDiagramDialog(frame);
		if (dialog.image == null || dialog.nav == null) return;

		int baseElev;
		try {
			 baseElev = NumberFormat.getIntegerInstance().parse(dialog.baseElevation.getText()).intValue();
		} catch (ParseException e) {
			e.printStackTrace();
			return;
		}

		FenceDiagram fd = new FenceDiagram(ww, 
				dialog.nav, 
				baseElev * 1000,
				1, 
				dialog.image);
		RenderableLayer rl = new RenderableLayer();
		rl.setName(dialog.name.getText());
		rl.addRenderable(fd);

		mapApp.makeLayerVisible(rl);
	}

	public static List<LatLon> loadNav(InputStream openStream) throws IOException {
		BufferedReader in = new BufferedReader(
				new InputStreamReader(openStream));

		List<LatLon> pos = new LinkedList<LatLon>();

		int line = 0;
		String str = null;
		String[] split;
		while ((str = in.readLine()) != null) {
			line++;
			split = str.split("[\t,]");
			if (split.length < 2) continue;

			try {
				pos.add(LatLon.fromDegrees(Double.parseDouble(split[1]), Double.parseDouble(split[0])));
			} catch (NumberFormatException ex) {
				if (line != 0) {
					System.err.println("Line " + line);
					ex.printStackTrace();
				}
			}
		}

		in.close();

		if (pos.size() == 0)
			throw new IOException("No Nav Parsed");

		return pos;
	}
}

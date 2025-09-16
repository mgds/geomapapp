package haxby.map;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class StartUp extends JComponent implements ActionListener {
	BufferedImage image;
	JLabel label;
	String startUpPath = "org/geomapapp/resources/maps/startup/";
	ClassLoader cl = getClass().getClassLoader();

	public StartUp() {
		this(0);
	}
	public StartUp( int which ) {
		try {
			URL url = null;
			switch (which) {
				case MapApp.MERCATOR_MAP:
					url = cl.getResource(startUpPath +"smallmapV3.jpg"); //New version3 images
					break;
				case MapApp.SOUTH_POLAR_MAP:
					url = cl.getResource(startUpPath + "MapAppSouthV3.jpg");
					break;
				case MapApp.NORTH_POLAR_MAP:
					url = cl.getResource(startUpPath + "MapAppNorthV3.jpg");
					break;
				case MapApp.WORLDWIND:
					url = cl.getResource(startUpPath + "VirtualOceanV3.jpg");
					break;
				default:
					url = cl.getResource(startUpPath + "smallmap.jpg");
			}
			image = ImageIO.read(url);
		} catch (Exception ex) {
			System.out.println(ex + " null");
			image=null;
		}
		setLayout(null);
		label = new JLabel("Initializing MapApp...");
		label.setFont( new Font("SansSerif", Font.PLAIN, 12) );
		label.setForeground( Color.black );
	//	add( label );
		label.setLocation(10, 50);
		label.setSize( label.getPreferredSize() );
		JButton button = new JButton( "Abort" );
		add( button );
		button.setLocation( 3, 3);
		button.setSize(120, 30);
		button.addActionListener( this );
	//	System.out.println( getComponentCount() + " components" );
		setBorder( BorderFactory.createLineBorder(Color.black, 2) );
	}
	public Dimension getPreferredSize() {
		if( image==null ) return  new Dimension(300, 200);
		return new Dimension(image.getWidth(), image.getHeight() );
	}
	public void setText( String text ) {
	//	System.out.println(text);
		label.setText( text);
		repaint();
	}
	public void paintComponent(Graphics g) {
	//	g.setColor(Color.black);
	//	g.fillRect(0,0,392,215);
		if(image != null) {
			g.drawImage(image, 2, 2, this);
		}
		g.setFont( new Font("SansSerif", Font.BOLD, 14 ));
		g.setColor(Color.white);
		g.drawString( label.getText(), 124, 19);
		g.setColor(Color.black);
		g.drawString( label.getText(), 125, 20);
	}
	public void actionPerformed( ActionEvent evt ) {
		System.exit(0);
	}
	public static void main(String[] args) {
		StartUp s = new StartUp(0);
		JFrame frame = new JFrame("startup");
		frame.getContentPane().add(s);
		frame.pack();
		frame.show();
	}
}
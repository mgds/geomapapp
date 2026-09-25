package org.geomapapp.image;

import org.geomapapp.util.*;

import javax.imageio.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;

public class IconEdit extends ImageComponent 
		implements MouseListener,
				MouseMotionListener,
				ActionListener {
	Color color;
	JFrame frame;
	Box tools;
	JToggleButton label;
	JLabel swatch;
	JLabel size;
	JComponent colorComp;
	JComboBox colorCB;
	static SimpleBorder border_off = new SimpleBorder();
	static SimpleBorder border_on = new SimpleBorder(true);
	public IconEdit() throws IOException {
		super();
		setColor( new Color( 190, 190, 220 ));
		try {
			if( !open() ) System.exit(0);
		} catch(IOException ex) {
			System.exit(0);
		}
		init();
		addKeyListener( new KeyAdapter() {
			public void keyReleased(KeyEvent evt) {
				if( evt.getKeyCode()==evt.VK_O) {
					try {
						open();
						label.setIcon( new ImageIcon(image));
					} catch(IOException ex) {
						JOptionPane.showMessageDialog(
							null,
							"open failed\n\t"+ex.getMessage());
					}
				} else if( evt.getKeyCode()==evt.VK_S) {
					try {
						save();
						label.setIcon( new ImageIcon(image));
					} catch(IOException ex) {
						JOptionPane.showMessageDialog(
							null,
							"save failed\n\t"+ex.getMessage());
					}
				} else if( evt.getKeyCode()==evt.VK_C ) {
					clear();
				} else if( evt.getKeyCode()==evt.VK_E ) {
					clearEdge();
				} else if( evt.getKeyCode()==evt.VK_H ) {
					horizontalFlip();
				} else if( evt.getKeyCode()==evt.VK_V ) {
					verticalFlip();
				} else if( evt.getKeyCode()==evt.VK_R ) {
					rotate();
				}
			}
		});
	}
	void horizontalFlip() {
		for( int y=0 ; y<image.getHeight() ; y++) {
			for( int x=0 ; x<image.getWidth()/2 ; x++) {
				int rgb = image.getRGB(x,y);
				image.setRGB(x,y,image.getRGB(image.getWidth()-1-x,y));
				image.setRGB(image.getWidth()-1-x,y,rgb);
			}
		}
		setImage(image);
		repaint();
		label.repaint();
	}
	void verticalFlip() {
		for( int x=0 ; x<image.getWidth() ; x++) {
			for( int y=0 ; y<image.getHeight()/2 ; y++) {
				int rgb = image.getRGB(x,y);
				image.setRGB(x,y,image.getRGB(x, image.getHeight()-1-y));
				image.setRGB(x,image.getHeight()-1-y,rgb);
			}
		}
		setImage(image);
		repaint();
		label.repaint();
	}
	void rotate() {
		BufferedImage tmp = new BufferedImage( image.getHeight(), image.getWidth(), BufferedImage.TYPE_INT_ARGB);
		for( int x=0 ; x<image.getWidth() ; x++) {
			for( int y=0 ; y<image.getHeight() ; y++) {
				tmp.setRGB(y,x,image.getRGB(x, image.getHeight()-y-1));
			}
		}
		image = tmp;
		setImage(image);
		repaint();
		label.repaint();
	}
	void init() {
		color = Color.black;
		frame = new JFrame(file.getName());
		frame.setDefaultCloseOperation( frame.EXIT_ON_CLOSE);
		frame.getContentPane().add( new JScrollPane(this));
		initTools();
		frame.getContentPane().add( tools, "North");
		frame.pack();
		frame.show();
		Point p = new Point(0,0);
		zoomIn(p);
		zoomIn(p);
		zoomIn(p);
		zoomIn(p);
		Zoomer z = new Zoomer(this);
		addMouseListener(z);
		addMouseMotionListener(z);
		addKeyListener(z);

		addMouseListener( this );
		addMouseMotionListener( this );

		frame.pack();
	}
	void initTools() {
		tools = Box.createHorizontalBox();
		label = new JToggleButton( new ImageIcon(image));
		label.setBorder( BorderFactory.createEmptyBorder(0,0,0,0) );
		tools.add( label);
		tools.add( Box.createHorizontalStrut(5) );
		colorComp = new JComponent() {
			public Dimension getPreferredSize() {
				return new Dimension(24, 24);
			}
			public void paint(Graphics g) {
				g.fillRect( 2,2,20,20 );
			}
		};
		colorComp.setForeground( color );
		colorComp.addMouseListener(this);
		tools.add( colorComp );
		colorCB = new JComboBox();
		colorCB.addActionListener(this);
		colorCB.setRenderer( new ListCellRenderer() {
			public Component getListCellRendererComponent(JList list,
											Object value,
											int index,
											boolean isSelected,
											boolean cellHasFocus) {
				JLabel label = (JLabel)value;
				label.setBorder( isSelected ?
					border_on : border_off);
				label.setOpaque(true);
				label.setBackground( isSelected ?
					Color.gray :Color.lightGray);
				
				return (JLabel)value;
			}
		});
		JLabel l =  new JLabel(new ColorIcon(color));
		colorCB.addItem( l);
		colorCB.setSelectedItem(l);
		tools.add( colorCB );
	}
	void clear() {
		for( int x=0 ; x<image.getWidth() ; x++) {
			for( int y=0 ; y<image.getHeight() ; y++) {
				image.setRGB(x,y,0);
			}
		}
		repaint();
		label.repaint();
	}
	void clearEdge() {
		int y = image.getHeight()-1;
		for( int x=0 ; x<image.getWidth() ; x++) {
			image.setRGB(x,0,0);
			image.setRGB(x,y,0);
		}
		int x = image.getWidth()-1;
		for( y=0 ; y<image.getHeight() ; y++) {
			image.setRGB(0,y,0);
			image.setRGB(x,y,0);
		}
		repaint();
		label.repaint();
	}
	class ColorIcon implements Icon  {
		Color color;
		public ColorIcon(Color color) {
			this.color = color;
		}
		public int getIconWidth() {
			return 24;
		}
		public int getIconHeight() {
			return 24;
		}
		public void paintIcon(Component c, Graphics g, int x, int y) {
			g.setColor(color);
			g.fillRect(x+2, y+2, 20, 20);
		}
	}
	public void actionPerformed(ActionEvent evt) {
		JLabel l = (JLabel)colorCB.getSelectedItem();
		ColorIcon c = (ColorIcon)l.getIcon();
		color = c.color;
		colorComp.setForeground( color);
		colorComp.repaint();
	}
	public void mouseEntered(MouseEvent evt) {
	}
	public void mouseExited(MouseEvent evt) {
	}
	public void mousePressed(MouseEvent evt) {
	}
	public void mouseReleased(MouseEvent evt) {
	}
	public void mouseClicked(MouseEvent evt) {
		if( evt.isControlDown() )return;
		if( evt.getSource() == colorComp ) {
			Color c = JColorChooser.showDialog( frame, "colors", color);
			if(c==null)return;
			color = c;
			JLabel l =  new JLabel( new ColorIcon(color));
			colorCB.addItem( l);
			colorCB.setSelectedItem(l);
			colorComp.setForeground( color);
			colorComp.repaint();
			return;
		}
		Point2D p = inverseTransform( evt.getPoint() );
		int x = (int)Math.floor(p.getX());
		int y = (int)Math.floor(p.getY());
		if( x<0 || x>=width || y<0 || y>=height)return;
		Color c = evt.isShiftDown() ?
				new Color(0,0,0,0) :
				color;
		if( c.getRGB()==image.getRGB(x,y) )return;
		image.setRGB(x, y, c.getRGB());
		repaint();
		label.repaint();
	}
	public void mouseMoved(MouseEvent evt) {
	}
	public void mouseDragged(MouseEvent evt) {
		mouseClicked(evt);
	}
	public static void main(String[] args) {
		try {
			new IconEdit();
		} catch (IOException ex) {
			ex.printStackTrace(System.err);
			System.exit(-1);
		}
	}
}

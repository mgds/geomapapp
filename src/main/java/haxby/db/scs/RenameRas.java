package haxby.db.scs;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class RenameRas extends JComponent 
			implements ActionListener,
			MouseListener,
			MouseMotionListener {
	BufferedImage image;
	double rotation;
	JTextField cruise, year, month, day, hour;
	JTextField rotate;
	public RenameRas( String fileName ) {
		 try {
			BufferedInputStream in = new BufferedInputStream(
				new FileInputStream( fileName ));
			image = decodeAsBufferedImage(in);
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(0);
		}
		JPanel panel = new JPanel( new GridLayout(2, 0) );
		panel.add(new JLabel("cruise"));
		panel.add(new JLabel("year"));
		panel.add(new JLabel("month"));
		panel.add(new JLabel("day"));
		panel.add(new JLabel("hour"));
		panel.add(new JLabel("rotation"));
		panel.add( new JButton("next") );
		panel.add( new JButton("quit") );
		StringTokenizer st = new StringTokenizer(fileName, "_");
		cruise = new JTextField(st.nextToken());
		panel.add( cruise );
		year = new JTextField("1900");
		panel.add( year );
		month = new JTextField("1");
		panel.add( month );
		day = new JTextField("1");
		panel.add( day );
		hour = new JTextField("0");
		panel.add( hour );
		rotate = new JTextField("0");
		panel.add( rotate );
		JFrame frame = new JFrame(fileName);
		frame.setDefaultCloseOperation( frame.EXIT_ON_CLOSE);
		frame.getContentPane().add( new JScrollPane( this ), "Center");
		frame.getContentPane().add( panel, "North");
		frame.pack();
		frame.show();
		addMouseListener(this);
		addMouseMotionListener(this);
		rotation=0.;
	}
	public Dimension getPreferredSize() {
		return new Dimension(image.getWidth()/4, image.getHeight()/4);
	}
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.scale(.25, .25);
		if(rotation!=0.)g2.rotate( rotation);
		g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage( image, 0, 0, this);
	}
	public void actionPerformed(ActionEvent evt ) {
	}
	public void mouseEntered(MouseEvent evt) {
	}
	public void mouseExited(MouseEvent evt) {
	}
	public void mousePressed(MouseEvent evt) {
	}
	public void mouseReleased(MouseEvent evt) {
	}
	Point p1 = null;
	public void mouseClicked(MouseEvent evt) {
		if( evt.getClickCount()>1 ) {
			p1=null;
		} else if(p1==null) {
			p1=evt.getPoint();
			line = new Line2D.Double( p1.getX(), p1.getY(),p1.getX(), p1.getY() );
			drawLine();
		} else {
			Point p2 = evt.getPoint();
			if( p2.x==p1.x && p2.y==p1.y ) {
				p1 = null;
			}
			if( p2.x<=p1.x ) {
				p1 = null;
			} else if( p2.y==p1.y) {
				p1 = null;
			} else {
				double angle = Math.atan( (p2.getY()-p1.getY())/(p2.getX()-p1.getX()) );
				rotation -= angle;
			}
			rotate.setText( (rotation*180./Math.PI)+"" );
			p1 = null;
			repaint();
		}
	}
	Line2D.Double line = null;
	public void mouseMoved(MouseEvent evt) {
		if(p1==null) return;
		drawLine();
		Point p = evt.getPoint();
		line.x2 = evt.getX();
		line.y2 = evt.getY();
		drawLine();
		if( p.x>p1.x ) {
			double angle = Math.atan( (p.getY()-p1.getY())/(p.getX()-p1.getX()) );
			rotate.setText( (-angle*180./Math.PI)+"" );
		}
	}
	void drawLine() {
		synchronized( getTreeLock() ) {
			Graphics2D g = (Graphics2D)getGraphics();
			g.setXORMode( Color.white );
			g.draw(line);
		}
	}
	public void mouseDragged(MouseEvent evt) {
	}
	public BufferedImage decodeAsBufferedImage(InputStream input) throws IOException {
		DataInputStream in = new DataInputStream(input);
		if(in.readInt() != 1504078485) throw new IOException("not a sunraster file");
		int w= in.readInt();
		int h = in.readInt();
		BufferedImage im = new BufferedImage( w, h, BufferedImage.TYPE_INT_RGB);
		in.readInt();
		in.readInt();
		in.readInt();
		in.readInt();
		int length = in.readInt();
		for( int k=0 ; k<length ; k++) in.readByte();
		for(int y=0 ; y<h ; y++) {
			for(int x=0 ; x<w ; x++) {
				int i = in.readUnsignedByte();
				im.setRGB(x, y, 0xff000000 | (i<<16) | (i<<8) | i);
			}
		}
		return im;
	}
	public static void main(String[] args) {
		if( args.length != 1) {
			System.out.println( "usage: java RenameRas file_name");
			System.exit(0);
		}
		new RenameRas( args[0] );
	}
}

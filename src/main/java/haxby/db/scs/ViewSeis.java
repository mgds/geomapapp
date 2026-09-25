package haxby.db.scs;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;

import javax.imageio.ImageIO;
import javax.swing.*;
//import com.sun.image.codec.jpeg.*;

public class ViewSeis extends JComponent 
			implements MouseListener,
			MouseMotionListener {
	BufferedImage image;
	double rotation;
	public ViewSeis( String fileName ) {
		 try {
			BufferedInputStream in = new BufferedInputStream(
				new FileInputStream( fileName ));
			//JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(in);
			//image = decoder.decodeAsBufferedImage();
			image = ImageIO.read(in);
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(0);
		}
		JFrame frame = new JFrame(fileName);
		frame.setDefaultCloseOperation( frame.EXIT_ON_CLOSE);
		frame.getContentPane().add( new JScrollPane( this ));
		frame.pack();
		frame.setSize( 1280, 1024);
		frame.show();
		addMouseListener(this);
		addMouseMotionListener(this);
		rotation=0.;
	}
	public Dimension getPreferredSize() {
		return new Dimension(image.getWidth()/2, image.getHeight()/2);
	}
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.scale(.5, .5);
		if(rotation!=0.)g2.rotate( rotation);
		g.drawImage( image, 0, 0, this);
	}
	public static void main(String[] args) {
		if( args.length != 1) {
			System.out.println( "usage: java ViewSeis file_name");
			System.exit(0);
		}
		new ViewSeis( args[0] );
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
}
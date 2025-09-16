package org.geomapapp.util;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.swing.*;
import javax.imageio.*;
import javax.swing.event.MouseInputAdapter;
import java.beans.*;

import haxby.map.*;

import haxby.image.QT;

public class FrameGrabber implements Runnable {
	JFrame frame;
	Point loc;
	BufferedImage select, zoom_in, zoom_out;
	boolean zoom=false;
	boolean zout =false;
	boolean active;
	MouseInputAdapter mml;
	KeyListener kl;
	int nFrame;
	int frameRate;
	long delay;
	public FrameGrabber( JFrame frame ) {
		this.frame = frame;
		loc = new Point();
		active = false;
		mml = new MouseInputAdapter() {
			public void mouseMoved(MouseEvent evt) {
				saveLoc(evt);
			}
			public void mouseDragged(MouseEvent evt) {
				saveLoc(evt);
			}
		};
		kl = new KeyAdapter() {
			public void keyPressed(KeyEvent evt) {
				if( !(evt.getSource() instanceof XMap) ) return;
				zoom = evt.isControlDown();
				if( zoom ) zout = evt.isShiftDown();
			}
			public void keyReleased(KeyEvent evt) {
				keyPressed(evt);
			}
		};
		setRate(10);
		setNFrame(10);
	}
	public void setFrame( JFrame frame ) {
		removeListeners( this.frame.getContentPane());
		this.frame = frame;
		loc = new Point();
		active = false;
	}
	public void setRate( int framesPerSec ) {
		frameRate = framesPerSec;
		delay = 1000L/(long)framesPerSec;
	}
	public void setNFrame(int nFrame) {
		this.nFrame=nFrame;
	}
	void initLoc() {
		addListeners( frame.getContentPane());
	}
	void addListeners(Container c) {
		Component[] comps = c.getComponents();
		if( c instanceof XMap) {
			c.removeKeyListener(kl);
			c.addKeyListener(kl);
		}
		for( int k=0 ; k<comps.length ; k++ ) {
			comps[k].removeMouseMotionListener(mml);
			comps[k].addMouseMotionListener(mml);
			if( comps[k] instanceof Container ) {
				addListeners( (Container) comps[k]);
			}
		}
	}
	void removeListeners(Container c) {
		Component[] comps = c.getComponents();
		if( c instanceof XMap) {
			c.removeKeyListener(kl);
		}
		for( int k=0 ; k<comps.length ; k++ ) {
			comps[k].removeMouseMotionListener(mml);
			if( comps[k] instanceof Container ) {
				removeListeners( (Container) comps[k]);
			}
		}
	}
	void saveLoc(MouseEvent evt) {
		Component c = (Component)evt.getSource();
		Point p = c.getLocationOnScreen();
		Point p1 = frame.getLocationOnScreen();
		loc.x = evt.getX() + p.x - p1.x;
		loc.y = evt.getY() + p.y - p1.y;
		if( evt.getSource() instanceof XMap ) {
			zoom = evt.isControlDown();
			zout = evt.isShiftDown();
		} else {
			zoom = false;
		}
	}
	public void keyReleased(KeyEvent evt) {
		if(evt.isControlDown())return;
		if( evt.getKeyCode()==evt.VK_C )capture();
	}
	public void keyPressed( KeyEvent evt ) {}
	public void keyTyped( KeyEvent evt ) {}
	BufferedImage im;
	Graphics2D gFrame;
	QT qt;
	void capture() {
		synchronized( frame.getTreeLock() ) {
			frame.paintAll(gFrame);
		}
		try {
			if( loc!=null ) {
				if( select==null )readSelect();
				if( !zoom ) {
					gFrame.drawImage( select, loc.x-7, loc.y-3, frame);
				} else if(zout) {
					gFrame.drawImage( zoom_out, loc.x-7, loc.y-7, frame);
				} else {
					gFrame.drawImage( zoom_in, loc.x-7, loc.y-7, frame);
				}
			}
			qt.addImage(im);
		//	ImageIO.write( im, "jpg", new File("test.jpg"));
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	void readSelect() throws IOException {
		ClassLoader loader = org.geomapapp.util.Icons.class.getClassLoader();
		String path = "org/geomapapp/resources/icons/select.png";
		java.net.URL url = loader.getResource(path);
		select = ImageIO.read(url);
	//	BufferedImage im = ImageIO.read(url);
	//	select = new BufferedImage(22, 22, BufferedImage.TYPE_INT_ARGB);
	//	Graphics2D g = select.createGraphics();
	//	AffineTransform at = new AffineTransform();
	//	at.scale(2., 2.);
	//	g.drawRenderedImage( im, at );

		path = "org/geomapapp/resources/icons/zoom_in.png";
		url = loader.getResource(path);
		zoom_in = ImageIO.read(url);
	//	im = ImageIO.read(url);
	//	zoom_in = new BufferedImage(44, 44, BufferedImage.TYPE_INT_ARGB);
	//	g = zoom_in.createGraphics();
	//	at = new AffineTransform();
	//	g.drawRenderedImage( im, at );

		path = "org/geomapapp/resources/icons/zoom_out.png";
		url = loader.getResource(path);
		zoom_out= ImageIO.read(url);
	//	im = ImageIO.read(url);
	//	zoom_out = new BufferedImage(44, 44, BufferedImage.TYPE_INT_ARGB);
	//	g = zoom_out.createGraphics();
	//	at = new AffineTransform();
	//	g.drawRenderedImage( im, at );
	}
	public void done() {
		active = false;
		removeListeners( this.frame.getContentPane());
	}
	public void run() {
		frame.setResizable(false);
		Dimension size = frame.getSize();
		im = new BufferedImage(size.width, size.height,
				BufferedImage.TYPE_INT_RGB);
		gFrame = im.createGraphics();
		try {
			qt = new QT( nFrame, frameRate, size.width, size.height, new File("test.mov"));
		} catch(Exception ex) {
			ex.printStackTrace();
			frame.setResizable(true);
			active = false;
			return;
		}
		addListeners( this.frame.getContentPane());
		active = true;
		long time = System.currentTimeMillis();
		for(int k=0 ; k<nFrame ; k++) {
			capture();
			long t = System.currentTimeMillis();
			if( t-time<delay) {
				try {
					Thread.currentThread().sleep(t-time);
				} catch(Exception ex) {
				}
				time += delay;
			} else {
				time = t;
			}
		}
		removeListeners( this.frame.getContentPane());
		frame.setResizable(true);
	}
}

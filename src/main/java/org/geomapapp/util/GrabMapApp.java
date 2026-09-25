package org.geomapapp.util;

import haxby.map.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class GrabMapApp implements Runnable {
	FrameGrabber grabber;
	MapApp app;
	public GrabMapApp() {
		Thread t = new Thread(this);
		t.start();
		init();
	}
	public void run() {
		app = new MapApp();
	}
	JTextField rate, nFrame;
	void init() {
		JFrame frame = new JFrame("Grab MapApp");
		JPanel panel = new JPanel(new GridLayout(0,2));
		rate = new JTextField("5");
		panel.add(rate);
		panel.add(new JLabel("frames per sec"));
		nFrame = new JTextField("50");
		panel.add(nFrame);
		panel.add(new JLabel("total frames"));
		JButton button = new JButton("record");
		panel.add(button); 
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				record();
			}
		});
		frame.getContentPane().add(panel);
		frame.pack();
		frame.show();
	}
	void record() {
		FrameGrabber grabber = new FrameGrabber( app.getFrame() );
		int nFrame = Integer.parseInt(this.nFrame.getText());
		int rate = Integer.parseInt(this.rate.getText());
		grabber.setNFrame(nFrame);
		grabber.setRate(rate);
		Thread t = new Thread(grabber);
		t.start();
	}
	public static void main(String[] args) {
		new GrabMapApp();
	}
}

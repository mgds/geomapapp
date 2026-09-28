package org.geomapapp.util;

import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import java.awt.event.*;

public class TestSpline extends JComponent 
		implements ActionListener, MouseListener {
	Vector points;
	double[][] ax, ay;
	double[] t;
	public static void main(String[] args) {
		new TestSpline();
	}
	public TestSpline() {
		JFrame frame = new JFrame("spline test");
		Box box = Box.createHorizontalBox();
		JButton clearB = new JButton("clear");
		clearB.addActionListener(this);
		box.add(clearB);
		JButton computeB = new JButton("compute");
		computeB.addActionListener(this);
		box.add(computeB);
		JButton computeC = new JButton("computeA");
		computeC.addActionListener(this);
		box.add(computeC);
		computeC = new JButton("computeB");
		computeC.addActionListener(this);
		box.add(computeC);
		computeC = new JButton("computeC");
		computeC.addActionListener(this);
		box.add(computeC);
		JButton quitB = new JButton("quit");
		quitB.addActionListener(this);
		box.add(quitB);
		frame.getContentPane().add(box,"North");

		frame.getContentPane().add(this);
		setPreferredSize(new Dimension(600,400));

		addMouseListener(this);
		frame.pack();
		frame.setVisible(true);
		points = new Vector();
		ax = null;
		ay = null;
		t = null;
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
	}

	public void mouseClicked(MouseEvent e) {
		points.add(new Point2D.Double((double)e.getX(), (double)e.getY()));
		repaint();
	}
	public void mousePressed(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("clear")) {
			points.clear();
			ax = null;
			ay = null;
			t = null;
			repaint();
		} else if(e.getActionCommand().equals("compute")) {
			compute();
		} else if(e.getActionCommand().equals("computeA")) {
			computeA();
		} else if(e.getActionCommand().equals("computeB")) {
			computeB();
		} else if(e.getActionCommand().equals("computeC")) {
			computeC();
		} else if(e.getActionCommand().equals("quit")) {
			System.exit(0);
		}
	}

	public void computeA() {
		int n = points.size();
		if(points.size() < 3) {
			JOptionPane.showMessageDialog(this,"Need >2 points to compute");
			return;
		}
		double[] x = new double[n];
		double[] y = new double[n];
		t = new double[n];
		for(int i=0 ; i<n ; i++) {
			int i1 = (i+1)%n;
			t[i] = ((Point2D)points.get(i)).distance((Point2D)points.get(i1));
			x[i] = ((Point2D.Double)points.get(i)).x;
			y[i] = ((Point2D.Double)points.get(i)).y;
		}
		ax = Spline.wrapSpline(x,t,n);
		ay = Spline.wrapSpline(y,t,n);
		repaint();
	}
	public void computeB() {
		int n = points.size();
		if(points.size() < 4) {
			JOptionPane.showMessageDialog(this,"Need >3 points to compute");
			return;
		}
		double[] x = new double[n];
		for(int i=0 ; i<n ; i++) {
			x[i] = ((Point2D.Double)points.get(i)).x;
		}
		ax = Spline.spline(x,n);
		for(int i=0 ; i<n ; i++) {
			x[i] = ((Point2D.Double)points.get(i)).y;
		}
		ay = Spline.spline(x,n);
		t = null;
		repaint();
	}
	public void computeC() {
		int n = points.size();
		if(points.size() < 4) {
			JOptionPane.showMessageDialog(this,"Need >3 points to compute");
			return;
		}
		double[] x = new double[n];
		double[] y = new double[n];
		t = new double[n];
		t[0] = 0;
		for(int i=0 ; i<n ; i++) {
			if(i != 0) t[i] = t[i-1] + ((Point2D)points.get(i)).distance((Point2D)points.get(i-1));
			x[i] = ((Point2D.Double)points.get(i)).x;
			y[i] = ((Point2D.Double)points.get(i)).y;
		}
		ax = Spline.spline(x,t,n);
		ay = Spline.spline(y,t,n);
	System.out.println("x\ta\tb\tc\ty\ta\tb\tc");
		for( int i=0 ; i<n-1 ; i++) System.out.println(x[i]+"\t"+
							(float)ax[i][0] +"\t"+
							(float)ax[i][1] +"\t"+
							(float)ax[i][2] +"\t"+
							y[i] +"\t"+
							(float)ay[i][0] +"\t"+
							(float)ay[i][1] +"\t"+
							(float)ay[i][2]);
		for( int i=0 ; i<n-1 ; i++) t[i] = t[i+1]-t[i];
		repaint();
	}
	public void compute() {
		int n = points.size();
		if(points.size() < 3) {
			JOptionPane.showMessageDialog(this,"Need >2 points to compute");
			return;
		}
		double[] x = new double[n];
		for(int i=0 ; i<n ; i++) {
			x[i] = ((Point2D.Double)points.get(i)).x;
		}
		ax = Spline.wrapSpline(x,n);
		for(int i=0 ; i<n ; i++) {
			x[i] = ((Point2D.Double)points.get(i)).y;
		}
		ay = Spline.wrapSpline(x,n);
		t = null;
		repaint();
	}
	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;
		g2d.setColor(Color.white);
		int n = points.size();
		Dimension dim = getSize();
		g2d.fill(new Rectangle(0,0,dim.width,dim.height));
		g2d.setColor(Color.black);
		for(int i=0 ; i<n ; i++) {
			double x = ((Point2D.Double)points.get(i)).x;
			double y = ((Point2D.Double)points.get(i)).y;
			Rectangle2D.Double r = new Rectangle2D.Double(x-2d, y-2d, 4d,4d);
			g2d.draw(r);
		}
		GeneralPath path = new GeneralPath();
		if(ay != null )n=ay.length;
		for(int i=0 ; i<n ; i++) {
			double x = ((Point2D.Double)points.get(i)).x;
			double y = ((Point2D.Double)points.get(i)).y;
			if(i == 0) path.moveTo((float)x, (float)y);
			else path.lineTo((float)x, (float)y);
			if(ay != null && ay.length>i) {
				for(int k=1 ; k<11 ; k++) {
					double dt = .1d * (double)k;
					if(t != null && t.length >i) dt *= t[i];
					double xx = x + dt* (ax[i][0]
						+ dt*(ax[i][1] + dt*ax[i][2]));
					double yy = y + dt* (ay[i][0]
						+ dt*(ay[i][1] + dt*ay[i][2]));
					path.lineTo((float)xx, (float)yy);
				}
			}
		}
		g2d.setColor(Color.red);
		g2d.draw(path);
	}
}
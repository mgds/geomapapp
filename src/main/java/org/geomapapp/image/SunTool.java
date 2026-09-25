package org.geomapapp.image;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;

import org.geomapapp.geom.XYZ;
import org.geomapapp.util.SimpleBorder;

public class SunTool extends JComponent implements ActionListener {
	XYZ sun;
	MouseInputAdapter mouse;
	JTextField inclination;
	JTextField declination;
	
	public JRadioButton sunOn;
	public JRadioButton sunOff;
	ButtonGroup sunButtons;
	
	JPanel panel;
	java.text.NumberFormat fmt;
	public SunTool(XYZ toSun) {
		sun = toSun;
		sun.normalize();
		mouse = new MouseInputAdapter() {
			public void mouseClicked(MouseEvent evt) {
				mouseReleased(evt);
			}
			public void mouseReleased(MouseEvent evt) {
				apply( evt.getPoint() );
			}
			public void mouseDragged(MouseEvent evt) {
				drag( evt.getPoint() );
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);

		sunOn = new JRadioButton("On");
		sunOff = new JRadioButton("Off");
		sunButtons = new ButtonGroup();
		sunOn.setSelected(true);
		sunButtons.add(sunOn);
		sunButtons.add(sunOff);

//		***** GMA 1.6.4: TESTING
//		inclination = new JTextField(5);
//		declination = new JTextField(5);

		inclination = new JTextField(2);
		declination = new JTextField(2);
//		***** GMA 1.6.4

		fmt = java.text.NumberFormat.getInstance();
		fmt.setMaximumFractionDigits(0);

//		***** GMA 1.6.4: TESTING

		setSize( new Dimension( 20, 20 ) );
		setToolTipText("Drag yellow dot (sun) to illuminate scren for different directions and heights");
//		***** GMA 1.6.4
	}
	public void setSun( XYZ toSun ) {
		sun = toSun;
		repaint();
	}
	void initPanel() {
		JPanel sunOnOff = new JPanel(new FlowLayout(FlowLayout.LEADING));
		sunOnOff.add(sunOn);
		sunOnOff.add(sunOff);
		sunOn.addActionListener(this);
		sunOff.addActionListener(this);

		ActionListener ac = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				setAngles();
				apply();
			}
		};
		inclination.addActionListener(ac);
		declination.addActionListener(ac);

//		***** GMA 1.6.6: Set alignment of text in text boxes to center to make the text more visible

		inclination.setHorizontalAlignment(JTextField.CENTER);
		declination.setHorizontalAlignment(JTextField.CENTER);
//		***** GMA 1.6.6

		JPanel input = new JPanel(new GridLayout(0,2));

//		***** GMA 1.6.4: TESTING
		input.add( new JLabel("Azimuth")); //Declination
		input.add(declination);

//		***** GMA 1.6.4: TESTING
		input.add( new JLabel("Altitude")); //Inclination
		input.add(inclination);

		SimpleBorder sb = new SimpleBorder();
		setBorder( sb );
		input.setBorder( sb );

//		panel  = new JPanel(new GridLayout(2,0));
		panel = new JPanel( new BorderLayout() );

		panel.add(sunOnOff, BorderLayout.NORTH);

//		***** GMA 1.6.4: TESTING
//		panel.add(this, BorderLayout.WEST);
		panel.add(this, BorderLayout.CENTER);
		panel.add(input, BorderLayout.SOUTH);

//		JPanel tempPanel = new JPanel( new FlowLayout() );
//		tempPanel.add(this);
//		tempPanel.add(input);
//		panel.add(tempPanel, BorderLayout.WEST);
//		***** GMA 1.6.4: TESTING
	}
	public JPanel getPanel() {
		if( panel==null )initPanel();
		return panel;
	}
	
	public boolean isSunOn() {
		if ( sunOff != null ) {
			if ( sunOff.isSelected() ) {
				return false;
			}
			else {
				return true;
			}
		}
		return true;
	}
	
	Point2D getXY() {
		if( !isVisible() ) return new Point();
		Rectangle r = getRect();
		double radius = .45*Math.min(r.width, r.height);
		double x0 = r.x+r.getWidth()/2.;
		double y0 = r.y+r.getHeight()/2.;
		if( sun.z>=1. ) return new Point2D.Double( x0, y0);
		double i = 1.-Math.asin(sun.z)*2./Math.PI;
		radius *= i;
		XYZ xyz = new XYZ(sun.x, sun.y, 0.);
		xyz.normalize();
		Point2D.Double p = new Point2D.Double( x0+radius*xyz.x,
						y0-radius*xyz.y);
		return p;
	}
	Rectangle getRect() {
		if( !isVisible() ) return null;
		Rectangle r = getVisibleRect();
		Insets ins = getInsets();
		r.x += ins.left;
		r.y += ins.top;
		r.width -= ins.left + ins.right;
		r.height -= ins.top + ins.bottom;
		return r;
	}
	void drag( Point p) {
		if( !isVisible() ) return;
		Rectangle r = getRect();
		double radius = .45*Math.min(r.width, r.height);
		double x0 = r.x + r.getWidth()/2.;
		double y0 = r.y + r.getHeight()/2.;
		sun.x = (p.getX()-x0)/radius;
		sun.y = (y0-p.getY())/radius;
		XYZ xyz = new XYZ( sun.x, sun.y, 0.);
		double i = xyz.getNorm();
		if( i>1. )i=1.;
		i = 90.*(1.-i);
		double d = Math.toDegrees(Math.atan2(sun.x, sun.y));
		inclination.setText( fmt.format(i));
		declination.setText( fmt.format(d));
		setAngles();
	}
	void apply(Point p) {
		drag(p);
		apply();
	}
	public void apply() {
		firePropertyChange("SUN_CHANGED", new XYZ(), sun);
	}
	public void setAngles() {
		try {
			sun.z = Math.sin(Math.toRadians(
				Double.parseDouble(inclination.getText())));
			if( sun.z<0. )sun.z = -sun.z;
			double f = Math.sqrt(1.-sun.z*sun.z);
			double d = Double.parseDouble(declination.getText());
			sun.x = f*Math.sin(Math.toRadians(d));
			sun.y = f*Math.cos(Math.toRadians(d));
		} catch(Exception ex) {
		}
		repaint();
	}
	public XYZ getSun() {
		return new XYZ(sun.x, sun.y, sun.z);
	}
	public double getDeclination() {
		try {
			return Double.parseDouble(declination.getText());
		} catch (NumberFormatException ex) {
			return Double.NaN; 
		}
	}
	public double getInclination() {
		try {
			return Double.parseDouble(inclination.getText());
		} catch (NumberFormatException ex) {
			return Double.NaN; 
		}
	}
	
	public void setDeclination(double dec) {
		declination.setText(fmt.format(dec));
		setAngles();
	}
	
	public void setInclination(double inc) {
		inclination.setText(fmt.format(inc));
		setAngles();
	}
	
	public Dimension getPreferredSize() {
		Insets ins = getInsets();
		Dimension size = new Dimension(100, 100);
		if( ins!=null ) {
			size.width += ins.left + ins.right;
			size.height += ins.top + ins.bottom;
		}
		return size;
	}
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		if( panel!=null ) {
			sun.normalize();
			double i = Math.toDegrees(Math.asin(sun.z));
			inclination.setText(fmt.format(i));
			double d = (sun.z==1.) ?
				0. :
				Math.toDegrees(Math.atan2(sun.x, sun.y));
			declination.setText(fmt.format(d));
		}
		AffineTransform at = g2.getTransform();
		Rectangle r = getRect();
		double radius = .45*Math.min(r.width, r.height);
		double x0 = r.x + r.getWidth()/2.;
		double y0 = r.y + r.getHeight()/2.;
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		Arc2D.Double arc = new Arc2D.Double(x0-radius,
						y0-radius,
						2.*radius,
						2.*radius,
						0.,
						360.,
						Arc2D.CHORD);
		g2.setColor( new Color(80, 160, 240) );
		g2.fill(arc);
		g2.setColor(Color.black);
		g2.draw(arc);
		double rd = radius*2./3.;
		arc.x = x0-rd;
		arc.y = y0-rd;
		arc.width = arc.height = 2.*rd;
		g2.draw(arc);
		rd = radius/3.;
		arc.x = x0-rd;
		arc.y = y0-rd;
		arc.width = arc.height = 2.*rd;
		g2.draw(arc);
		Point2D p = getXY();
		arc.x = p.getX()-6.;
		arc.y = p.getY()-6.;
		arc.width = arc.height = 12.;
		g2.setColor(Color.yellow);
		g2.fill(arc);
		g2.setColor(Color.red);
		g2.draw(arc);
		g2.setTransform(at);
	}
	public void actionPerformed(ActionEvent e) {
		if ( e.getSource() == sunOn || e.getSource() == sunOff ) {
			firePropertyChange("SUN_CHANGED", new XYZ(), sun);
		}
	}
}

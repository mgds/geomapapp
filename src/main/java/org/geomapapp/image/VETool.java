package org.geomapapp.image;

import org.geomapapp.util.SimpleBorder;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;

public class VETool extends JComponent implements ChangeListener {
	double ve;
	double veStart;
	MouseInputAdapter mouse;
	Point point;
	JPanel panel;
	JTextField veF;
	java.text.NumberFormat fmt;

//	***** GMA 1.6.4: TESTING

	static final int VE_MIN = 0;
	static final int VE_MAX = 20;
	static final int VE_INIT = 2;
	JSlider veSlider = new JSlider( JSlider.HORIZONTAL, VE_MIN, VE_MAX, VE_INIT );
//	***** GMA 1.6.4

	public VETool(double ve) {
		this.ve = ve;
		fmt = java.text.NumberFormat.getInstance();
		fmt.setMaximumFractionDigits(2);

//		***** GMA 1.6.4: TESTING
		veSlider.addChangeListener(this);
		veSlider.setPreferredSize( new Dimension( 100, 40 ) );
		veSlider.setMajorTickSpacing(5);
		veSlider.setMinorTickSpacing(1);
		veSlider.setPaintTicks(true);
		veSlider.setPaintLabels(true);
		veF = new JTextField( fmt.format(ve), 3 );
		veF.setToolTipText("Vertical Exaggeration Value");
//		***** GMA 1.6.4

		mouse = new MouseInputAdapter() {
			public void mousePressed(MouseEvent evt) {
				setStart();
			}
			public void mouseReleased(MouseEvent evt) {
				apply( evt.getPoint(), evt.isShiftDown() );
			}
			public void mouseDragged(MouseEvent evt) {
				drag( evt.getPoint(), evt.isShiftDown() );
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
		point = null;
	}
	void setStart() {
		veStart = ve;
	}
	void drag( Point p, boolean fine) {
		if( !isVisible() ) return;
		if( point==null ) {
			point = p;
			return;
		}
		if( p.y==point.y )return;
		double factor = fine ? 1.004 : 1.02;
		if( p.y>point.y ) {
			for( int y=point.y ; y<=p.y ; y++ ) ve/=factor;
		} else {
			for( int y=point.y ; y>=p.y ; y-- ) ve*=factor;
		}
		point = p;
		repaint();
	}
	public JPanel getPanel() {
		if( panel==null )initPanel();
		return panel;
	}
	void initPanel() {
		veF.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {

//				***** GMA 1.6.4: TESTING
				int tempValue = Integer.parseInt(veF.getText());
				if ( tempValue > VE_MAX ) {
					veSlider.setValue(VE_MAX);
				}
				else if ( tempValue < VE_MIN ) {
					veSlider.setValue(VE_MIN);
				}
				else {
					veSlider.setValue(tempValue);
				}
//				***** GMA 1.6.4
				setVE();
			}
		});

//		***** GMA 1.6.4: TESTING
//		JPanel input = new JPanel(new GridLayout(0,1));
		JPanel input = new JPanel(new FlowLayout(0,10,10));
		input.setOpaque(true);
//		***** GMA 1.6.4

		input.add(new JLabel("V.E."));
		input.add(veF);

		SimpleBorder sb = new SimpleBorder();
		setBorder( sb );
		//input.setBorder( sb );

//		***** GMA 1.6.4: TESTING
//		panel  = new JPanel(new GridLayout(1,0));
//		panel.add(this);
		panel = new JPanel( new BorderLayout() );
//		***** GMA 1.6.4

//		***** GMA 1.6.4: TESTING
		panel.add(input);
//		panel.add(veSlider, BorderLayout.NORTH);
		panel.add(input, BorderLayout.SOUTH);
//		***** GMA 1.6.4

	}
	void apply(Point p, boolean fine) {
		if( point==null )return;
		double oldVE = ve;
		drag(p, fine);
		point = null;
		synchronized( getTreeLock() ) {
			firePropertyChange("VE_CHANGED", veStart, ve);
		}
	}
	void setVE() {
		try {
			double oldVE = ve;
			double newVE = Double.parseDouble(veF.getText());
			if( ve==newVE )return;
			ve = newVE;
			firePropertyChange("VE_CHANGED", oldVE, ve);
		}catch(Exception ex) {
		}
		repaint();
	}
	public double getVE() {
		return ve;
	}
	public void setVE(double ve) {
		veF.setText( fmt.format(ve) );
		setVE();
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
	public void paintComponent(Graphics g) {
		if( !isVisible() ) return;
		Graphics2D g2 = (Graphics2D)g;
		AffineTransform at = g2.getTransform();
		Rectangle r = getRect();
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		double x0 = r.x + r.getWidth()/2.;
		double y0 = r.y + r.getHeight()/2.;
		double size = .9*Math.min( r.getWidth(), r.getHeight());
		Line2D.Double line = new Line2D.Double(x0-.5*size,
						y0+.5*size, 
						x0+.5*size, 
						y0+.5*size);
		g2.draw(line);
		line.x2 = line.x1;
		line.y2 = line.y1-size;
		g2.draw(line);
		if( ve>=1. ) {
			line.x1 = line.x1 + size/ve;
		} else {
			line.y1 = line.y1 - size*ve;
			line.x2 = x0+.5*size;
			line.y2 = y0+.5*size;
		}
		g2.draw(line);
		veF.setText( fmt.format(ve));
	//	String s = fmt.format(ve);
	//	Font font = g.getFont().deriveFont( .2f*(float)size);
	//	g.setFont(font);
	//	g2.translate( x0-size/10., y0-size/4. );
	//	g2.drawString( fmt.format(ve), 0, 0);
	//	g2.setTransform(at);
	}
	public static void main(String[] args) {
		VETool tool = new VETool(1.);
		JFrame frame = new JFrame("test VE");
		frame.getContentPane().add( tool);
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	public void stateChanged(ChangeEvent ce) {
		// ***** GMA 1.6.4: TESTING
		if ( ce.getSource() == veSlider ) {
			veF.setText( Integer.toString( veSlider.getValue() ) );
			setVE();
		}
	}
}

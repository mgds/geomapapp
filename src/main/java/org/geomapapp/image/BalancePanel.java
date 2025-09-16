package org.geomapapp.image;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.ByteLookupTable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.geomapapp.util.XYGraph;
import org.geomapapp.util.XYPoints;

public class BalancePanel extends JPanel
			implements ChangeListener,
					ActionListener,
					MouseListener,
					XYPoints {
	double[] lookup;
	byte[] table;

	JSlider contrast;
	JSlider brightness;
	JButton reset;
	JButton apply;

	XYGraph graph;
	double[] xRange, yRange;
	double xScale, yScale;

	ActionListener listener;

	public BalancePanel( ) {
		super( new BorderLayout() );
		init();
	}
	public ByteLookupTable getLookup() {
		if( contrast.getValue()==50 && brightness.getValue()==50 ) return null;
		doLookup();
		byte[] table = new byte[256];
		for(int k=0 ; k<256 ; k++) {
			table[k] = (byte)(int)Math.rint(lookup[k]);
		}
		return new ByteLookupTable( 0, table );
	}
	void init() {
		contrast = new JSlider();
		contrast.setBorder( BorderFactory.createTitledBorder("contrast") );
		brightness = new JSlider();
		brightness.setBorder( BorderFactory.createTitledBorder("brightness") );

		contrast.addChangeListener( this );
		brightness.addChangeListener( this );
		contrast.addMouseListener( this );
		brightness.addMouseListener( this );

		reset = new JButton("reset");
		reset.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				reset();
			}
		} );
		apply = new JButton("apply");
		apply.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				apply();
			}
		} );
		JPanel buttons = new JPanel(new GridLayout(1, 0));
		buttons.add( reset );
		buttons.add( apply );

		JPanel controls = new JPanel( new GridLayout(0, 1) );
		controls.add( buttons );
		controls.add( contrast );
		controls.add( brightness );
		add( controls, "North" );

		lookup = new double[256];
		for(int k=0 ; k<256 ; k++) lookup[k] = (double)k;
		xRange = new double[] {-1., 256. };
		yRange = new double[] {-1., 256. };
		xScale = yScale = 1.;

		graph = new XYGraph( this, 0 );
		add( graph, "Center" );
	}
	public String getXTitle(int dataIndex) {
		return "";
	}
	public String getYTitle(int dataIndex) {
		return "";
	}
	public double[] getXRange(int dataIndex) {
		return new double[] {xRange[0], xRange[1]};
	}
	public double[] getYRange(int dataIndex) {
		return new double[] {yRange[0], yRange[1]};
	}
	public double getPreferredXScale(int dataIndex) {
		return xScale;
	}
	public double getPreferredYScale(int dataIndex) {
		return yScale;
	}
	public void plotXY( Graphics2D g, 
				Rectangle2D bounds,
				double xScale, double yScale,
				int dataIndex) {
		doLookup();
		GeneralPath path = new GeneralPath();
		float x0 = (float)bounds.getX();
		float y0 = (float)(bounds.getY()+bounds.getHeight());
		path.moveTo( x0, y0 );
		for(int k=0 ; k<=255 ; k++) {
			if( k==0 ) {
				path.moveTo( (float)(xScale*(k - bounds.getX())), 
					(float)(yScale*(lookup[k] -bounds.getY())) );
			} else {
				path.lineTo( (float)(xScale*(k - bounds.getX())), 
					(float)(yScale*(lookup[k] -bounds.getY())) );
			}
		}
		g.setColor( Color.black );
		g.draw( path );
	}
	void doLookup() {
		if( contrast.getValue()==50 && brightness.getValue()==50 ) {
			for(int k=0 ; k<256 ; k++) lookup[k] = (double)k;
			return;
		}
		double c = 1.-.01*(1.+contrast.getValue()*.98);
		double b = .01*(1.+brightness.getValue()*.98);
		double pow = Math.log( b ) / Math.log( .5 );
		double pow2 = Math.log( c ) / Math.log( .5 );
	//	double slope = Math.tan( c*Math.PI/2. );
	//	double a1 = 2.5 - 2.*slope - .5/slope;
	//	double a2 = slope - 1.5 + .5/slope;
//	System.out.println( c +"\t"+ b +"\t"+ pow +"\t"+ slope);
		lookup[0] = 0.;
		lookup[255] = 255.;
		for(int k=1 ; k<255 ; k++ ) {
			double x = k/255.;
			double z = Math.pow( x, pow );
			if( z<=.5 ) {
				z = Math.pow(2.*z, pow2) * .5;
			} else {
				z = 1. - .5*Math.pow( 2.*(1.-z), pow2 );
			}
			lookup[k] = 255. * z;
		//	x = 2.*( z-.5 );
		//	lookup[k] = 255. * (.5 + .5*( x*slope + a1*Math.pow(x, 3) + a2*Math.pow(x, 5)));
		}
	}
	public void stateChanged(ChangeEvent e) {
		if( ! graph.isVisible() ) return;
		graph.repaint();
	}
	public void mouseEntered( MouseEvent evt ) {
	}
	public void mouseExited( MouseEvent evt ) {
	}
	public void mousePressed( MouseEvent evt ) {
	}
	public void mouseClicked( MouseEvent evt ) {
	}
	public void mouseReleased( MouseEvent evt ) {
		firePropertyChange( "BRIGHTNESS", 0, 1);
	//	listener.actionPerformed( new ActionEvent( this, ActionEvent.ACTION_FIRST, "repaint" ) );
	}
	public void reset() {
		contrast.setValue(50);
		brightness.setValue( 50 );
		firePropertyChange( "BRIGHTNESS", 0, 1);
	//	listener.actionPerformed( 
	//		new ActionEvent( this, ActionEvent.ACTION_FIRST, "repaint" ) );
	}
	public void apply() {
		firePropertyChange( "APPLY_MODS", 0, 1);
	}
	public void actionPerformed( ActionEvent evt ) {
	}
	public static void main(String[] args) {
		BalancePanel bp = new BalancePanel();
		bp.addPropertyChangeListener( 
			new java.beans.PropertyChangeListener() {
				public void propertyChange(java.beans.PropertyChangeEvent evt) {
					System.out.println(evt.getPropertyName());
				}
			});
		JFrame colorDialog = new JFrame( "color balance" );
		colorDialog.getContentPane().add( bp );
		colorDialog.pack();
		colorDialog.show();
		colorDialog.setDefaultCloseOperation( colorDialog.EXIT_ON_CLOSE );
	}
}

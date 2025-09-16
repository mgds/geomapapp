package org.geomapapp.util;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;

public class RangeDialog extends JPanel implements MouseListener {
	java.text.NumberFormat fmt;
	JButton props;
	JSlider slider;
	JTextField min, max, step, interval;
	JLabel from_to;
	JTextField from, to;
	RangeListener rl;
	double scale;
	JPanel dialog;
	int prevVal = -1;
	boolean changing = false;

	public RangeDialog(RangeListener rl) {
		this( rl, 0, 100, 2, 2, 1. );
	}
	public RangeDialog(RangeListener rl, int min, int max, int interval, int step, double scale ) {
		super( new java.awt.BorderLayout() );
		this.rl = rl;
		this.scale = scale;
		this.min = new JTextField( min +"" );
		this.max = new JTextField( max +"" );
		this.step = new JTextField( step +"" );
		this.interval = new JTextField( interval +"" );
		init();
	}
	void init() {
		double min = Double.parseDouble(this.min.getText());
		double max = Double.parseDouble(this.max.getText());
		double interval = Double.parseDouble(this.interval.getText());
		slider = new JSlider( JSlider.VERTICAL, 
				(int)Math.rint(min*scale),
				(int)Math.rint(max*scale),
				(int)Math.rint((min+.5*interval)*scale));
		slider.setExtent(Integer.parseInt(step.getText()));
//		slider.setMinorTickSpacing(Integer.parseInt(step.getText()));
//		slider.setPaintTicks(false);
//		slider.setSnapToTicks(true);
		slider.addMouseListener(this);
		props = new JButton("Set Step");
		fmt = java.text.NumberFormat.getInstance();
		fmt.setMaximumFractionDigits(2);
		add(slider);
		JPanel panel = new JPanel(new java.awt.GridLayout(0,1));
		JPanel p = new JPanel();
		from = new JTextField( fmt.format(min*scale),3);
		p.add( from );
		p.add( new JLabel(" - "));
		to = new JTextField( fmt.format((min+interval)*scale),3 );
		p.add( to );
	//	panel.add(p);
		from_to = new JLabel( from.getText() +" - "+ to.getText() );
		panel.add(from_to);
		panel.add(props);
		add( panel, "South");
		slider.addChangeListener( new ChangeListener() {
			 public void stateChanged(ChangeEvent e){
//				System.out.println("slider.getValue(): " + slider.getValue() + "\tprevVal: " + prevVal);
				int newVal = -1;
				if ( slider.getValue() % Integer.parseInt(step.getText()) != 0 && !changing ) {
					if ( slider.getValue() > prevVal ) {
						newVal = slider.getValue() + Integer.parseInt(step.getText()) - (slider.getValue() % Integer.parseInt(step.getText()));
					}
					else {//if ( slider.getValue() < prevVal ) {
						newVal = slider.getValue() - (slider.getValue() % Integer.parseInt(step.getText()));
					}
					slider.setValue(newVal);
				}
				else {
					newVal = slider.getValue();
				}
				update();
				prevVal = newVal;
			}
		});
		dialog = new JPanel( new java.awt.GridLayout(0,2) );
		dialog.add( new JLabel("minimum") );
		dialog.add( this.min);
		dialog.add( new JLabel("maximum") );
		dialog.add( this.max);
		dialog.add( new JLabel("interval") );
		dialog.add( this.interval);
		dialog.add( new JLabel("step") );
		dialog.add( this.step);

		props.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showDialog();
			}
		});
	}
	void showDialog() {
		int step = Integer.parseInt(this.step.getText());
		int min = Integer.parseInt(this.min.getText());
		int max = Integer.parseInt(this.max.getText());
		int interval = Integer.parseInt(this.interval.getText());
		int ok = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
				dialog, 
				"Set Step", 
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE );
		if( ok==JOptionPane.CANCEL_OPTION ) {
			this.min.setText( min+"" );
			this.max.setText( max+"" );
			this.step.setText( step+"" );
			this.interval.setText( interval+"" );
			return;
		}
		try {
			int mn = Integer.parseInt(this.min.getText());
			int mx = Integer.parseInt(this.max.getText());
			int st = Integer.parseInt(this.step.getText());
			int inv = Integer.parseInt(this.interval.getText());
			slider.setExtent( st );
			slider.setMinimum( mn );
			slider.setMaximum( mx );
		} catch(NumberFormatException e) {
			this.min.setText( min+"" );
			this.max.setText( max+"" );
			this.step.setText( step+"" );
			this.interval.setText( interval+"" );
		}
	}
	void update() {
		double min = Double.parseDouble(this.min.getText());
		double max = Double.parseDouble(this.max.getText());
		double interval = Double.parseDouble(this.interval.getText());
		double r1 = scale* (slider.getValue());
		double r2 = scale* (slider.getValue()+interval);
		if( r1<min*scale ) {
			r1 = min*scale;
			r2 = (min+interval)*scale;
		} else if( r2>max*scale ) {
			r2 = max*scale;
			r1 = (max-interval)*scale;
		}
		rl.setRange( new double[] {r1, r2});
		from.setText( fmt.format(r1) );
		to.setText( fmt.format(r2) );
		from_to.setText( from.getText() +" - "+ to.getText() + " my" );
	}
	public static void main(String[] args) {
		JFrame f = new JFrame();
		RangeListener rl = new RangeListener() {
			public void setRange(double[] range){
			}
		};
		RangeDialog rd = new RangeDialog(rl);
		f.getContentPane().add(rd);
		f.pack();
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	public void mouseClicked(MouseEvent mevt) {
//		if ( mevt.getSource().equals(slider) ) {
//			System.out.println("prevVal: " + prevVal + "\tslider value: " + slider.getValue());
//			if ( slider.getValue() == prevVal ) {
//				int newVal = -1;
//				newVal = slider.getValue() - Integer.parseInt(step.getText());
//				changing = true;
//				slider.setValue(newVal);
//				changing = false;
//				prevVal = newVal;
//			}
//		}
	}
	public void mouseEntered(MouseEvent arg0) {
	}
	public void mouseExited(MouseEvent arg0) {
	}
	public void mousePressed(MouseEvent arg0) {	
	}
	public void mouseReleased(MouseEvent arg0) {
	}
}
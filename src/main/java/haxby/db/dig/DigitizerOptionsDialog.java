package haxby.db.dig;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class DigitizerOptionsDialog implements ActionListener,
					MouseListener {
	Color defaultColor;
	Color defaultFill;
	BasicStroke defaultStroke;
	Color color;
	Color fill;
	BasicStroke stroke;
	Color[] startingColor;
	Color[] startingFill;
	BasicStroke[] startingStroke;
	JComponent comp;
	JCheckBox colorB, fillB;
	ColorPanel cP;
	ColorSwatch colorSwatch;
	ColorSwatch fillSwatch;
	DigitizerObject[] obj;
	JTextField lineWidth;
	public DigitizerOptionsDialog(JComponent comp) {
		this.comp = comp;
		defaultColor = Color.black;
		defaultFill = null;
		defaultStroke = new BasicStroke( 1f );
		color = new Color( 0, 0, 1 );
		fill = null;
		stroke = new BasicStroke( 1f );
		cP = new ColorPanel( color, 255, new ColorSwatch( Color.blue ) );
	}
	JPanel getStrokePanel() {
		JPanel strokePanel = new JPanel( new GridLayout(0, 2 ) );
		strokePanel.add( new JLabel("Line Width"));
		lineWidth = new JTextField(""+stroke.getLineWidth());
		strokePanel.add( lineWidth );
		JButton button = new JButton("Apply");
		strokePanel.add( button );
		button.addActionListener( this );
		button = new JButton("Apply");
		strokePanel.add( button );
		button.addActionListener( this );
		return strokePanel;
	}
	public void showDialog( DigitizerObject[] obj ) {
		this.obj = obj;
		if( obj.length==0 ) return;
		JTabbedPane pane = new JTabbedPane( JTabbedPane.TOP );
		JPanel colorPanel = new JPanel( new GridLayout(0, 1 ) );
		startingColor = new Color[obj.length];
		startingFill = new Color[obj.length];
		startingStroke = new BasicStroke[obj.length];
		for( int i=0 ; i<obj.length ; i++ ) {
			startingColor[i] = obj[i].getColor();
			startingFill[i] = obj[i].getFill();
			startingStroke[i] = obj[i].getStroke();
		}
		stroke = startingStroke[0];
		colorPanel.removeAll();
		colorB = new JCheckBox("Outline Color");
		colorPanel.add( colorB );
		color = startingColor[0];
		if( color==null ) {
			colorB.setSelected(false);
			colorSwatch = new ColorSwatch( defaultColor );
		} else {
			colorB.setSelected(true);
			colorSwatch = new ColorSwatch( color );
			colorSwatch.setTransparency( color.getAlpha() );
		}
		colorPanel.add( colorSwatch );
		fillB = new JCheckBox("Fill Color");
		colorPanel.add( fillB );
		fill = startingFill[0];
		if( fill==null ) {
			fillB.setSelected(false);
			fillSwatch = new ColorSwatch( defaultFill );
		} else {
			fillB.setSelected(true);
			fillSwatch = new ColorSwatch( fill );
			fillSwatch.setTransparency( fill.getAlpha() );
		}
		fillSwatch.addMouseListener( this );
		colorSwatch.addMouseListener( this );
		colorPanel.add( fillSwatch );
		colorB.addActionListener( this );
		fillB.addActionListener( this );
		JButton apply = new JButton("Apply");
		JButton reset = new JButton("Reset");
		apply.addActionListener( this );
		reset.addActionListener( this );
		colorPanel.add( apply );
		colorPanel.add( reset );
		JPanel panel = new JPanel( new BorderLayout() );
		panel.add( colorPanel, "West");
		panel.add( cP, "East");
		pane.add( "Color", panel );
		pane.add( "Lines", getStrokePanel() );
		int ok = JOptionPane.showConfirmDialog( comp.getTopLevelAncestor(),
					pane, "Select Colors", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE);
		if( ok==JOptionPane.OK_OPTION) {
			apply();
		} else {
			reset();
		}
		obj = null;
	}
	void reset() {
		for( int i=0 ; i<obj.length ; i++ ) {
			if( obj[i].getColor()!=null || startingColor[i]!=null ) {
				obj[i].setColor( startingColor[i] );
			}
			if(obj[i].getFill()!=null || startingFill[i]!=null) {
				obj[i].setFill( startingFill[i] );
			}
			obj[i].setStroke( startingStroke[i] );
		}
		comp.repaint();
	}
	void apply() {
		Color color=null;
		if( colorB.isSelected() ) {
			color = colorSwatch.getColor();
			color = new Color( color.getRed(), color.getGreen(), color.getBlue(),
					colorSwatch.getTransparency() );
		}
		Color fill=null;
		if( fillB.isSelected() ) {
			fill = fillSwatch.getColor();
			fill = new Color( fill.getRed(), fill.getGreen(), fill.getBlue(),
					fillSwatch.getTransparency() );
		}
		float w = 1f;
		try {
			w = Float.parseFloat(lineWidth.getText());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		stroke = new BasicStroke( w, stroke.getEndCap(),
				stroke.getLineJoin(),
				stroke.getMiterLimit());
		for( int i=0 ; i<obj.length ; i++ ) {
			if( obj[i].getColor()!=null || color!=null ) {
				obj[i].setColor( color );
			}
			if(obj[i].getFill()!=null || fill!=null) {
				obj[i].setFill( fill );
			}
			obj[i].setStroke( stroke );
		}
		comp.repaint();
	}
	public void actionPerformed( ActionEvent evt ) {
		if( evt.getSource() == colorB ) {
			for( int i=0 ; i<obj.length ; i++ ) {
				obj[i].setColor( null );
			}
		//	Color c = JColorChooser.showDialog( comp, "Outline/Stroke Color", color);
		//	if( c==null )return;
			color = obj[0].getColor();
			colorSwatch.setColor( color );
			colorSwatch.repaint();
		} else if( evt.getSource() == fillB ) {
			for( int i=0 ; i<obj.length ; i++ ) {
				obj[i].setFill( null );
			}
			fill = obj[0].getFill();
			fillSwatch.setColor( fill );
			fillSwatch.repaint();
		//	Color c = JColorChooser.showDialog( comp, "fill Color", fill);
		//	if( c==null )return;
		//	fill = c;
		} else if( evt.getActionCommand().equals("Apply") ) {
			apply();
		} else if( evt.getActionCommand().equals("Reset") ) {
			reset();
			color = startingColor[0];
			fill = startingFill[0];
		}
	}
	public void mouseEntered( MouseEvent evt ) {
	}
	public void mouseExited( MouseEvent evt ) {
	}
	public void mousePressed( MouseEvent evt ) {
	}
	public void mouseReleased( MouseEvent evt ) {
	}
	public void mouseClicked( MouseEvent evt ) {
		if( evt.getSource()== colorSwatch ) {
			if( !colorB.isSelected() )return;
			colorSwatch.setActive(true);
			fillSwatch.setActive(false);
			cP.setSwatch( colorSwatch );
		} else if(evt.getSource()== fillSwatch) {
			if( !fillB.isSelected() )return;
			colorSwatch.setActive(false);
			fillSwatch.setActive(true);
			cP.setSwatch( fillSwatch );
		}
		cP.reset();
	}
}

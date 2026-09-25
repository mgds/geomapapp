package org.geomapapp.image;

import org.geomapapp.util.SimpleBorder;

import java.util.Vector;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import java.beans.*;

public class PaletteTool extends JComponent {
	Palette lut0;
	Palette lut;
	Vector buttons;
	int selectedButton;
	JPanel buttonPanel;
	SimpleBorder border;
	MouseAdapter buttonL;
	ColorModPanel color;

	public PaletteTool(Palette lut, ColorModPanel color) {
		this.color = color;
		color.addPropertyChangeListener(
			new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent e) {
					if(e.getPropertyName().equals("RESET")) {
						reset();
						return;
					} else if(e.getPropertyName().equals("APPLY")) {
						apply();
						return;
					} else if(e.getPropertyName().equals("SAVE")) {
						save();
						return;
					}
					colorChange();
				}
			});
		buttonL = new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				select( (ColorComponent)evt.getSource() );
			}
		};
		setDefaultPalette(lut);
	}
	public void setDefaultPalette(Palette lut) {
		lut0 = (Palette)lut.clone();
		this.lut = lut;
		updateButtons();
	}
	void reset() {
		float[][] model = lut0.getModel();
		lut.setModel( model[1], model[2], model[3], model[0]);
		setDefaultPalette(lut);
		colorChange();
	}
	void apply() {
		firePropertyChange("APPLY_PALLETTE",0, 1);
	}
	void save() {
		firePropertyChange("SAVE_PALLETTE",0, 1);
	}
	void updateButtons() {
		ColorComponent cc=null;
		Color col;
		if( buttonPanel==null ) {
			buttonPanel=new JPanel(new GridLayout(1,0));
			buttons = new Vector();
		}
		for( int k=0 ; k<buttons.size() ; k++) {
			cc = (ColorComponent)buttons.get(k);
			cc.removeMouseListener(buttonL);
			buttonPanel.remove(cc);
		}
		float[][] model = lut.getModel();
		float[] red = model[1];
		float[] green = model[2];
		float[] blue = model[3];
		for( int k=0 ; k<red.length ; k++) {
			col = new Color(red[k], green[k], blue[k]);
			if( k<buttons.size() ) {
				cc = (ColorComponent)buttons.get(k);
				cc.setColor(col);
			} else {
				cc = new ColorComponent(col);
				buttons.add(cc);
			}
			buttonPanel.add( cc );
			cc.addMouseListener(buttonL);
		}
		for( int k=red.length ; k<buttons.size() ; k++) {
			cc = (ColorComponent)buttons.remove(k);
		}
		buttonPanel.revalidate();
		cc = (ColorComponent)buttons.get(0);
		select(cc);
	}
	void setSelectedNode( int node ) {
		float[][] model = lut.getModel();
		if( node<0 || node>=model[0].length)return;
		int rgb = (new Color(model[1][node], model[2][node], model[3][node])).getRGB();
		color.setDefaultRGB(rgb);
		repaint();
	}
	void select(ColorComponent button) {
		ColorComponent cc=null;
		for( int k=0 ; k<buttons.size() ; k++) {
			cc = (ColorComponent)buttons.get(k);
			if( button==cc ) {
				selectedButton = k;
				button.setSelected(true);
				setSelectedNode( k );
			} else if( cc.isSelected() ) {
				cc.setSelected(false);
			}
		}
	}
	void colorChange() {
		int rgb = color.getRGB();
		Color col = new Color(rgb);
		ColorComponent cc = (ColorComponent)buttons.get(selectedButton);
		cc.setColor(col);
		float[] cols = col.getColorComponents(null);
		lut.setRGB(selectedButton, cols);
		firePropertyChange("PALLETTE_CHANGE", null, lut);
		repaint();
	}
	public Palette getPalette() {
		return lut;
	}
	public void paintComponent(Graphics g) {
		if( !isVisible() )return;
		Rectangle r = getVisibleRect();
		Insets in = getInsets();
		if( in!=null ) {
			r.x += in.left;
			r.y += in.top;
			r.width -= in.right + in.left;
			r.height += in.bottom + in.top;
		}
	}
	public JPanel getButtonPanel() {
		return buttonPanel;
	}
	public static void main(String[] args) {
		Palette pal = new Palette(0);
		ColorModPanel mod = new ColorModPanel(Color.blue.getRGB());
		PaletteTool tool = new PaletteTool(pal, mod );
		JPanel panel = new JPanel(new BorderLayout());
		panel.add( mod );
		panel.add( tool.buttonPanel, "South");
		JOptionPane.showMessageDialog( haxby.map.MapApp.anchor, 
					panel,
					"Palette Tool",
					JOptionPane.PLAIN_MESSAGE);
		System.exit(0);
	}
}

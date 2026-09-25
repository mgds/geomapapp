package org.geomapapp.image;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.TitledBorder;

public class ColorModPanel extends JPanel {
	int rgb;
	int rgb0;
	ColorPane[] panes;
	JSlider transS;
	ColorComponent swatch;
	MouseInputAdapter mouse;
	JPanel resetPanel;
	public ColorModPanel(int rgb, boolean transparency) {
		super( new BorderLayout() );
		if(transparency) {
			transS = new JSlider();
			int val = (rgb>>24)&255;
			val = (255-val)*100/255;
			transS.setValue(val);
			rgb |= 0xff000000;
		}
		rgb0 = rgb;
		setRGB( rgb );
		init();
	}
	public ColorModPanel(int rgb) {
		this(rgb, false);
	}
	public ColorModPanel() {
		this(0xff000000);
	}
	void init() {
		if( panes!=null )return;
		JPanel hsbPanel = new JPanel(new GridLayout(1,0,2,2));
		hsbPanel.setBorder(BorderFactory.createTitledBorder(
					null,
					"H-S-B",
					TitledBorder.LEFT,
					TitledBorder.BOTTOM));

		panes = new ColorPane[3];
		KeyAdapter keys = new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				int code = e.getKeyCode();
				if( code==KeyEvent.VK_C ) copy();
				else if( code==KeyEvent.VK_P ) print();
			}
		};
		mouse = new MouseInputAdapter() {
			ColorPane cp =null;
			public void mouseClicked(MouseEvent evt) {
				if( transS!=null && evt.getSource() == transS ) {
					update();
					return;
				}
				cp = (ColorPane)evt.getSource();
				setRGB( cp.getRGB(evt.getPoint()) );
			}
			public void mouseReleased(MouseEvent evt) {
				update();
				cp = null;
			}
			public void mouseDragged(MouseEvent evt) {
				if( cp==null || cp==evt.getSource() ) {
					mouseClicked(evt);
				}
			}
		};
		for(int i=0 ; i<3 ; i++) {
			panes[i] = new ColorPane(rgb, i+3);
			panes[i].addMouseListener(mouse);
			panes[i].addMouseMotionListener(mouse);
			panes[i].addKeyListener(keys);
			hsbPanel.add(panes[i]);
		}
		if( transS!=null ) {
			transS.addMouseListener(mouse);
			transS.addMouseMotionListener(mouse);
		}
		panes[0].setToolTipText("Modify Hue");
		panes[1].setToolTipText("Modify Saturation");
		panes[2].setToolTipText("Modify Brightness");
		add(hsbPanel, "Center");
		swatch = new ColorComponent(new Color(rgb, transS!=null ));
		swatch.setToolTipText("Reset Color");
		swatch.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));;
		add(swatch, "South");

		swatch.addMouseListener( new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				reset();
			}
		});
		
		if( transS!=null) {
			transS.setBorder(new TitledBorder("Transparency"));
			add(transS, "North");
		}
	}
	void print() {
		int ok=getRGB();
		System.out.println( 
				((ok>>16)&0xff)
				+","+ ((ok>>8)&0xff)
				+","+ (ok&0xff));
	}
	void copy() {
		int ok=getRGB();
		JTextField f = new JTextField(
				((ok>>16)&0xff)
				+","+ ((ok>>8)&0xff)
				+","+ (ok&0xff));
		f.selectAll();
		f.copy();
	}
	public int getTransparency() {
		if( transS==null ) return 0;
		return (100-transS.getValue())*255/100;
	}
	public void reset() {
		if( rgb==rgb0 )return;
		setRGB( rgb0 );
		update();
	}
	public void update() {
		setRGB( getRGB() );
		copy();
	//	firePropertyChange("APPLY", 0, rgb);
	}
	public void setDefaultRGB( int rgb ) {
		if( rgb0==rgb )return;
		rgb0 = rgb;
		setRGB( rgb0 );
	//	update();
	}
	public void setRGB(int rgb) {
		this.rgb = rgb;
		rgb = getRGB();
		if( panes==null )return;
		for(int i=0 ; i<3 ; i++) {
			panes[i].setColor(rgb);
		}
		swatch.setColor( new Color(rgb, transS!=null ));
		swatch.repaint();
		firePropertyChange("COLOR_CHANGE", 0, rgb);
	}
	public int getRGB() {
		if( transS!=null ) {
			return (rgb&0x00ffffff) | (getTransparency()<<24);
		}
		return rgb;
	}
	public int showDialog(Component comp, int rgb) {
		setDefaultRGB( rgb );
		return showDialog( comp );
	}
	public int showDialog(Component comp) {
		int ok = JOptionPane.showConfirmDialog(
				comp,
				this,
				"Color Chooser",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if( ok==JOptionPane.CANCEL_OPTION) {
			reset();
		}
		return getRGB();
	}
	public static void main(String[] args) {
		ColorModPanel mod = new ColorModPanel(Color.pink.getRGB(), true);
		int ok=mod.showDialog(null, mod.getRGB());
		System.out.println( ((ok>>24)&0xff) 
				+", "+ ((ok>>16)&0xff)
				+", "+ ((ok>>8)&0xff)
				+", "+ (ok&0xff));
		System.exit(0);
	}
}

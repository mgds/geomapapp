package org.geomapapp.util;

import org.geomapapp.image.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.font.*;
import java.awt.image.*;
import java.awt.event.*;

public class TextPanel extends JPanel
		implements ActionListener {
	CharSelect cSel;
	TextOverlay text;
	JTextArea textA;
	JComboBox fontCB;
	JTextField sizeF;
	JToggleButton boldB;
	JToggleButton italicB;
	JTextField rotateF;
	JCheckBox outlineCB, fillCB, shadowCB;
	JTextField outlineF;
	JTextField shadowF;
	ColorComponent outline, fill;
	public TextPanel(Font font, TextOverlay text) {
		super( new BorderLayout() );
		this.text = text;
		initPanel(font);
	}
	public void addNotify() {
		super.addNotify();
		textA.selectAll();
	}
	public void focusText() {
		textA.requestFocus();
	}
	public void selectText() {
		textA.selectAll();
	}
	private void initPanel(Font font) {
		textA = new JTextArea("enter text", 5, 20);
		textA.addCaretListener( new CaretListener() {
			public void caretUpdate(CaretEvent evt) {
				update();
			}
		});
		String[] fonts = 
			GraphicsEnvironment.getLocalGraphicsEnvironment(
			).getAvailableFontFamilyNames();
		fontCB = new JComboBox(fonts);
		fontCB.addActionListener( this );
		boldB  = new JToggleButton("Bold");
		boldB.setFont( new Font("Serif", 
				Font.BOLD, 
				boldB.getFont().getSize()) );
		boldB.addActionListener( this );
		italicB  = new JToggleButton("Italic");
		italicB.setFont( new Font("Serif", 
				Font.ITALIC, 
				italicB.getFont().getSize()) );
		italicB.addActionListener( this );

		sizeF = new JTextField("24", 3);
		rotateF = new JTextField("0", 3);
		sizeF.addActionListener(this);
		rotateF.addActionListener(this);
	//	Box box = Box.createHorizontalBox();
		JPanel box = new JPanel(new BorderLayout());
		box.add(fontCB, "North");

		JButton sel = new JButton("special character");
		box.add( sel, "South" );
		sel.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				charSelect();
			}
		});
		JPanel fontBox = new JPanel(new GridLayout(0,2));
		fontBox.add(boldB);
		fontBox.add(italicB);
		fontBox.add(new JLabel(" size"));
		fontBox.add(sizeF);
		fontBox.add(new JLabel(" angle"));
		fontBox.add(rotateF);

		outlineCB = new JCheckBox("outline",true);
		outlineCB.addActionListener(this);
		outline = new ColorComponent(Color.black);
		outline.addMouseListener( new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				modOutline();
			}
		});
		outlineF = new JTextField("1", 3);
		outlineF.addActionListener(this);

		fillCB = new JCheckBox("fill",true);
		fillCB.addActionListener(this);
		fill = new ColorComponent(Color.black);
		fill.addMouseListener( new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				modFill();
			}
		});

		shadowCB = new JCheckBox("Shadow",false);
		shadowCB.addActionListener(this);
		shadowCB.addActionListener(this);
		shadowF = new JTextField("1", 3);
		shadowF.addActionListener(this);

		fontBox.add( outlineCB );
		fontBox.add( outline );
		fontBox.add( new JLabel("width" ));
		fontBox.add( outlineF );
		fontBox.add( fillCB );
		fontBox.add( fill );
		fontBox.add( shadowCB );
		fontBox.add( shadowF );

		JButton toFront = new JButton("To Front");
		toFront.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				toFront();
			}
		});
		fontBox.add( toFront );

		JButton delete = new JButton("Delete");
		delete.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				delete();
			}
		});
		fontBox.add( delete );

		box.add( fontBox);
		add( box, "West");
		add( textA );
		if( font!=null ) {
			String family = font.getFamily();
			for( int k=0 ; k<fonts.length ; k++) {
				if( family.equals(fonts[k]) ) {
					fontCB.setSelectedIndex(k);
					break;
				}
			}
			boldB.setSelected(font.isBold());
			italicB.setSelected(font.isItalic());
			sizeF.setText( font.getSize()+"");
		}
	}
	void charSelect() {
		if( cSel==null) cSel = new CharSelect();
		String c = cSel.getChar(this);
		if( c==null ) return;
		textA.append(c);
		update();
	}
	void delete() {
		text.delete();
	}
	void toFront() {
		text.getParent().add(text,0);
		update();
	}
	void modOutline() {
		if( !outlineCB.isSelected() ) return;
		ColorModPanel p = new ColorModPanel(outline.getColor().getRGB(), true);
		int ok = JOptionPane.showConfirmDialog( this, p,
			"outline color", JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE);
		if( ok==JOptionPane.CANCEL_OPTION) return;
		int rgb = p.getRGB();
		outline.setColor( new Color( p.getRGB(), true) );
		update();
	}
	void modFill() {
		if( !fillCB.isSelected() ) return;
		ColorModPanel p = new ColorModPanel(fill.getColor().getRGB(), true);
		int ok = JOptionPane.showConfirmDialog( this, p,
			"fill color", JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE);
		if( ok==JOptionPane.CANCEL_OPTION) return;
		int rgb = p.getRGB();
		fill.setColor( new Color( p.getRGB(), true) );
		update();
	}
	public int getShadow() {
		int s = 0;
		if( !shadowCB.isSelected() )return 0;
		try {
			s = Integer.parseInt(shadowF.getText());
		} catch (Exception e) {
			s = 0;
		}
		return s;
	}
	public float getLineWidth() {
		float lw = 1f;
		try {
			lw = Float.parseFloat(outlineF.getText());
		}catch(Exception ex) {
		}
		return lw;
	}
	public Color getOutlineColor() {
		if( !outlineCB.isSelected() )return null;
		return outline.getColor();
	}
	public Color getFillColor() {
		if( !fillCB.isSelected() )return null;
		return fill.getColor();
	}
	public double getRotation() {
		return Math.toRadians(Double.parseDouble(rotateF.getText()));
	}
	public Font resolveFont() {
		int style = Font.PLAIN;
		if( boldB.isSelected() ) style|=Font.BOLD;
		if( italicB.isSelected() ) style|=Font.ITALIC;
		return new Font((String)fontCB.getSelectedItem(),
				style,
				Integer.parseInt(sizeF.getText()));
	}
	public String getText() {
		return textA.getText();
	}
	public void actionPerformed(ActionEvent evt) {
		update();
	}
	void update() {
	//	textA.setFont( resolveFont() );
		try {
			((Window)getTopLevelAncestor()).pack();
		}catch(Exception ex) {
		}
		firePropertyChange("UPDATE", 0, 1);
	}
}
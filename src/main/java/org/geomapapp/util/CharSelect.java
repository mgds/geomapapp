package org.geomapapp.util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class CharSelect implements ActionListener {
	JDialog dialog;
	JPanel panel;
	JTextField charF;
	Font font;
	public CharSelect() {
		this( new Font("SansSerif", Font.PLAIN, 14));
	}
	public CharSelect(Font font) {
		setFont( font);
		init();
	}
	public void setFont(Font font) {
		this.font = font;
		if( panel!=null ) {
			panel.setFont(font);
		}
	}
	void init() {
		if( panel!=null ) return;
		JPanel p = new JPanel(new GridLayout(0,16));
		panel = new JPanel(new BorderLayout());
		javax.swing.border.Border border = BorderFactory.createCompoundBorder(
				BorderFactory.createEtchedBorder(),
				BorderFactory.createEmptyBorder(1,1,1,1));
		for( short k=0 ; k<256*30 ; k++) {
			char[] c = new char[] {(char)k};
			String s = new String(c);
			JButton b = new JButton(s);
			b.setFont( font);
			b.setBorder(border);
			p.add(b);
			if( !Character.isDefined(c[0]) ) {
				b.setEnabled(false);
			} else {
				b.addActionListener(this);
			}
		}
		charF = new JTextField(" ",10);
		charF.setFont(font);
		panel.add(new JScrollPane(p, 
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED ));
		panel.add(charF, "North");
	//	panel.setSize( panel.getPreferredSize().width, 300 );
	//	dialog = new JDialog();
	//	dialog.setTitle( "char select");
	//	dialog.getContentPane().add(panel);
	//	dialog.pack();
	//	dialog.setSize( dialog.getPreferredSize().width, 300);
	//	JPanel p1 = new JPanel
	//	dialog.getContentPane().add(p1,"South");
	}
	public void actionPerformed(ActionEvent evt) {
		charF.setText( evt.getActionCommand());
		char ch = evt.getActionCommand().toCharArray()[0];
		Character c = new Character( evt.getActionCommand().toCharArray()[0]);
		System.out.println( Integer.toHexString((int)ch) +"\t"+ c.isDefined(ch) +"\t"+ c.isLetter(ch) );
	}
//	public void showDialog() {
//		dialog.show();
//	}
	public String getChar(Component comp) {
		Dimension dim = panel.getPreferredSize();
		System.out.println( dim.width +"\t"+ dim.height);
		dim.height = 230;
		panel.setPreferredSize( dim);
		int ok = JOptionPane.showConfirmDialog(
			comp, panel, "char select",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE);
		if( ok==JOptionPane.CANCEL_OPTION )return null;
		return charF.getText();
	}
	public static void main(String[] args) {
		CharSelect sel = new CharSelect( );
		System.out.println( sel.getChar(null));
		System.exit(0);
	}
}

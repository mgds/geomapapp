package haxby.image;

import java.awt.*;
import java.awt.font.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.*;

public class TextDialog implements ActionListener, Runnable {
	String text;
	Point location;
	Font font;
	Color outline, fill;
	JPanel PANEL=null;
	JTextArea TEXT=null;
	JComboBox FONT=null;
	JToggleButton BOLD=null;
	JToggleButton ITALIC=null;
	JTextField SIZE=null;
	JButton CANCEL=null;
	JButton APPLY=null;
	JButton OK=null;
	
	Point point;
	Component ie=null;
	TextElement ti=null;
	JDialog frame;
	boolean editing;
	int ok;
	public TextDialog() {
		initPanel();
		editing = false;
	}
	public String getText() {
		return TEXT.getText();
	}
	public Font getFont() {
		int style = Font.PLAIN;
		if( BOLD.isSelected() ) style = Font.BOLD;
		if( ITALIC.isSelected() ) style |= Font.ITALIC;
		font = new Font((String)FONT.getSelectedItem(), 
				style, 1);
		try {
			font = font.deriveFont( Float.parseFloat( SIZE.getText() ) );
		} catch( NumberFormatException ex ) {
			font = font.deriveFont( 12f );
		}
		return font;
	}
	void initPanel() {
		if( PANEL!=null ) {
			TEXT.setText( "enter text here" );
			TEXT.selectAll();
			return;
		}
		TEXT = new JTextArea( "enter text here" );
		TEXT.selectAll();
		String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		FONT = new JComboBox( fonts );
		FONT.addActionListener( this );
		BOLD = new JToggleButton("B");
		BOLD.setFont(new Font("Serif", Font.BOLD, BOLD.getFont().getSize()) );
		BOLD.addActionListener( this );
		ITALIC = new JToggleButton("I");
		ITALIC.setFont( new Font("Serif", Font.ITALIC, ITALIC.getFont().getSize()) );
		ITALIC.addActionListener( this );
		SIZE = new JTextField("12", 5);
		SIZE.addActionListener( this );
		Box box = Box.createHorizontalBox();
		box.add( FONT );
		box.add( BOLD );
		box.add( ITALIC );
		box.add( new JLabel("  Size:"));
		box.add( SIZE );
		OK = new JButton("Done");
		APPLY = new JButton("Preview");
		CANCEL = new JButton("Cancel");
		box.add( APPLY );
		APPLY.addActionListener( this );
		box.add( OK );
		OK.addActionListener( this );
		box.add( CANCEL );
		CANCEL.addActionListener( this );
		
		PANEL = new JPanel( new BorderLayout() );
		PANEL.add( box, "North" );
		PANEL.add( TEXT, "Center");
	}
	public JPanel getPanel() {
		TEXT.selectAll();
		TEXT.requestFocus();
		return PANEL;
	}
	public void run() {
	}
	public Container getTopLevelAncestor(Component c) {
		Container parent = c.getParent();
		if( c==null || parent==null  )return null;
		Container child = parent;
		while( parent!=null ) {
			child = parent;
			parent = child.getParent();
		}
		return child;
	}
	public int showDialog( Component ie, TextElement ti, Point point ) {
		editing = true;
		this.ti = ti;
		if( ti.getText()!=null )TEXT.setText( ti.getText() );
		this.ie = ie;
		this.point = point;
		Point loc = (ie==null)
				? new Point()
				:  ie.getLocationOnScreen();
		loc.x += point.x+2;
		loc.y += point.y+2;
		int ok1 = JOptionPane.showConfirmDialog( getTopLevelAncestor(ie), PANEL, "TextObject",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE );
		ok = (ok1==JOptionPane.OK_OPTION) ? 1 : -1;
		if( ok==1 ) APPLY.doClick();
		return ok;
	//	frame = new JDialog();
	//	frame.setTitle("TextObject");
	//	frame.getContentPane().add( PANEL );
	//	frame.pack();
	//	if( ti.getText()!=null ) TEXT.setText( ti.getText() );
	//	frame.setLocation( loc.x, loc.y );
	//	frame.show();
	//	frame.setDefaultCloseOperation( frame.DO_NOTHING_ON_CLOSE );
	//	ok = -1;
	//	while( editing ) {
	//		try {
	//			Thread.currentThread().sleep(100L);
	//		} catch(InterruptedException ex) {
	//			ok = -1;
	//			break;
	//		}
	//	}
	//	frame.dispose();
	//	return ok;
	}
	public void actionPerformed( ActionEvent evt ) {
		if( evt.getSource()==CANCEL ) {
			ok = -1;
			editing = false;
			return;
		} else if( evt.getSource()==OK ) {
			TEXT.setFont( getFont() );
			editing = false;
			ok=1;
			return;
		} else if( evt.getSource()==APPLY ) {
			FontRenderContext context = (ie == null)
				? (new BufferedImage(10, 10, 
						BufferedImage.TYPE_INT_RGB)).createGraphics().getFontRenderContext()
				: ((Graphics2D) ie.getGraphics()).getFontRenderContext();
			if( ti!=null ) {
				ti.setText( TEXT.getText(), getFont(), context );
				if( ie != null ) ie.repaint();
			}
			return;
		}
		TEXT.setFont( getFont() );
		try {
			((Window)PANEL.getTopLevelAncestor()).pack();
		} catch( Exception ex) {
		}
	}
	public static void main(String[] args) {
		TextDialog dialog = new TextDialog();
		Point p = new Point(100, 100);
		int ok = dialog.showDialog( null, new TextElement(p), p);
		System.out.println( ok +" returned");
		//	JFrame frame = new JFrame();
		//	frame.getContentPane().add( dialog.getPanel() );
		//	frame.pack();
		//	frame.show();
	}
}

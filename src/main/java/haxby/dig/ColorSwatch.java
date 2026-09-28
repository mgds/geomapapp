package haxby.dig;

import java.awt.*;
import javax.swing.*;

public class ColorSwatch extends JComponent {
	Color color;
	int transparency;
	boolean active;
	static javax.swing.border.Border selectBorder = BorderFactory.createCompoundBorder( 
				new haxby.util.SimpleBorder(true),
				BorderFactory.createLineBorder( Color.lightGray ));
	static javax.swing.border.Border border = BorderFactory.createCompoundBorder( 
				new haxby.util.SimpleBorder(),
				BorderFactory.createLineBorder( Color.lightGray ));
	public ColorSwatch( Color color ) {
		this.color = color;
		setBorder( border );
		setOpaque( true );
		transparency = 255;
		active = false;
	}
	public ColorSwatch( Color color, int transparency ) {
		this( color );
		this.transparency = transparency;
	}
	public void setActive( boolean tf ) {
		active = tf;
		if(active) setBorder( selectBorder );
		else setBorder( border );
	}
	public void setTransparency( int transparency ) {
		this.transparency = transparency;
	}
	public int getTransparency() {
		return transparency;
	}
	public Dimension getMinimumSize() {
		return new Dimension( 8, 8 );
	}
	public Dimension getPreferredSize() {
		return new Dimension( 16, 16 );
	}
	public Color getColor() {
		return color;
	}
	public void setColor(Color color) {
		this.color = color;
	}
	public void paintComponent( Graphics g ) {
		if( transparency!=255 ) {
			Dimension dim = getSize();
			g.setColor( Color.lightGray );
			g.fillRect( 0, 0, dim.width, dim.height );
			g.setColor( color.black );
			for( int k=5 ; k<dim.height ; k+=10 ) g.drawLine(0, k, dim.width, k);
			g.setColor( color.white );
			for( int k=5 ; k<dim.width ; k+=10 ) g.drawLine(k, 0, k, dim.height);
		}
		if( color==null ) {
			g.setColor( Color.black );
			g.drawString( "null", 2, 14 );
			return;
		}
		Dimension size = getSize();
		Color c = new Color( color.getRed(), color.getGreen(), color.getBlue(), transparency);
		g.setColor( c );
		g.fillRect( 1, 1, size.width-2, size.height-2);
	}
	public static void main(String[] args) {
		JPanel panel = new JPanel( new GridLayout(0,5) );
		Color c = null;
		panel.add(new ColorSwatch( c ));
		for(int i=1 ; i<25 ; i++) panel.add(new ColorSwatch( new Color(255,i*10+5, 0)));
		JFrame frame = new JFrame("test");
		frame.getContentPane().add( panel );
		frame.pack();
		frame.show();
		frame.setDefaultCloseOperation( frame.EXIT_ON_CLOSE );
	}
}

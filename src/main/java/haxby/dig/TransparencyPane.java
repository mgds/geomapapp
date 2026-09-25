package haxby.dig;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

public class TransparencyPane extends JComponent {
	Color color;
	int transparency;
	static javax.swing.border.Border border = BorderFactory.createCompoundBorder(
						new haxby.util.SimpleBorder(),
						BorderFactory.createLineBorder( Color.lightGray ));
	public TransparencyPane(Color color, int transparency) {
		this.transparency = transparency;
		this.color = color;
		setBorder( border );
	}
	public Dimension getPreferredSize() {
		return new Dimension( 34, 260 );
	}
	public void setColor( Color color ) {
		this.color = color;
	}
	public void setTransparency( int transparency) {
		this.transparency = transparency;
	}
	public int getTransparency() {
		return transparency;
	}
	public void paintComponent(Graphics g) {
		Dimension dim = getSize();
		g.setColor( Color.lightGray );
		g.fillRect( 0, 0, dim.width, dim.height );
		g.setColor( color.black );
		for( int k=5 ; k<dim.height ; k+=10 ) g.drawLine(0, k, dim.width, k);
		g.setColor( color.white );
		for( int k=5 ; k<dim.width ; k+=10 ) g.drawLine(k, 0, k, dim.height);
		BufferedImage image = new BufferedImage(dim.width-4, dim.height-4, BufferedImage.TYPE_INT_ARGB );
		float factor = (float)(dim.height-4);
		int y = (int)(factor*transparency/255);
		if( y>=dim.height-4 )y=dim.height-5;
		int c = color.getRGB() & 0x00ffffff;
		int black = 0xff000000;
		for(int i=0 ; i<dim.height-4 ; i++) {
			int trans = (int) (i*255/factor );
			int c1 =  c | (trans<<24);
			for( int k=0 ; k<dim.width-4 ; k++) image.setRGB( k, dim.height-5-i, c1 );
			if(i==y) {
				for( int k=0 ; k<(dim.width-4)/2 ; k++) image.setRGB( k, dim.height-5-i, black );
			}
		}
		Graphics2D g2 = (Graphics2D)g;
		g2.drawImage( image, 2, 2, this);
	}
}
package haxby.db.dig;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

public class ColorPane extends JComponent {
	public int h_s_or_b;
	float[] hsb;
	static javax.swing.border.Border border = BorderFactory.createCompoundBorder(
						new haxby.util.SimpleBorder(),
						BorderFactory.createLineBorder( Color.lightGray ));
	public ColorPane(Color color, int h_s_or_b) {
		this.h_s_or_b = h_s_or_b;
		setBorder( border );
		hsb = Color.RGBtoHSB( color.getRed(), color.getGreen(), color.getBlue(), null);
	}
	public Dimension getPreferredSize() {
		return new Dimension( 34, 260 );
	}
	public void setColor( Color color ) {
		hsb = Color.RGBtoHSB( color.getRed(), color.getGreen(), color.getBlue(), null);
	}
	public int getHSorB() {
		return h_s_or_b;
	}
	public void paintComponent(Graphics g) {
		Dimension dim = getSize();
		BufferedImage image = new BufferedImage(dim.width-4, dim.height-4, BufferedImage.TYPE_INT_RGB );
		float factor = (float)(dim.height-4);
		int y = (int)(factor*hsb[h_s_or_b]);
		if( y>=dim.height-4 )y=dim.height-5;
		float[] vals = new float[] { hsb[0], hsb[1], hsb[2] };
		int c = 0xff000000;
		for(int i=0 ; i<dim.height-4 ; i++) {
			vals[h_s_or_b] = (float)i/factor;
			int c1 = Color.HSBtoRGB( vals[0], vals[1], vals[2] );
			for( int k=0 ; k<dim.width-4 ; k++) image.setRGB( k, dim.height-5-i, c1 );
			if(i==y) {
				for( int k=0 ; k<(dim.width-4)/2 ; k++) image.setRGB( k, dim.height-5-i, c );
			}
		}
		Graphics2D g2 = (Graphics2D)g;
		g2.drawImage( image, 2, 2, this);
	}
	public static void main(String[] args) {
		ColorPane[] panels = new ColorPane[3];
		Color  c = new Color( 0, 200, 200 );
		panels[0] = new ColorPane( c, 0);
		panels[1] = new ColorPane( c, 1);
		panels[2] = new ColorPane( c, 2);
		JPanel p = new JPanel( new GridLayout( 1, 3) );
		for(int k=0 ; k<3 ; k++) p.add(panels[k]);
		JFrame f = new JFrame();
		f.getContentPane().add( p, "Center");
		f.pack();
		f.show();
		f.setDefaultCloseOperation( f.EXIT_ON_CLOSE);
	}
}

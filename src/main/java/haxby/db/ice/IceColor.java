package haxby.db.ice;

import java.awt.geom.*;
import java.awt.image.*;
import java.awt.*;
import javax.swing.*;

public class IceColor extends JComponent {
	BufferedImage image;
	public IceColor() {
		image = new BufferedImage(20, 50, BufferedImage.TYPE_INT_RGB);
		for( int y=0 ; y<50 ; y++) {
			int color = IceCore.getColor( -4.f+.15f*(float)(50-y)).getRGB();
			for( int x=0 ; x<20 ; x++ ) image.setRGB(x, y, color);
		}
	}
	public Dimension getPreferredSize() {
		return new Dimension(150, 140);
	}
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.scale( 2./1.5, 2./1.5);
		AffineTransform at = g2.getTransform();
		Rectangle rect = g.getClipBounds();
		g.setColor( Color.white);
		g2.fill(rect);
		g.translate( 40, 5 );
		g2.scale( 1.5, 1.5);
		g.drawImage(image, 0, 10, this);
		g.setColor( Color.black );
		g2.setStroke( new BasicStroke(.5f));
		g.drawRect(0,10,20,50);
		for(int i=-3 ; i<=2 ; i++) {
			int y = (int)(60. - (i+4.)/.15);
			int y1 = (int)(60. - (i+4.5)/.15);
			g.drawLine(19, y1, 23, y1);
			g.drawLine(-3, y, 1, y);
		}
		g.setFont( new Font("SansSerif", Font.PLAIN, 9 ) );
		FontMetrics fm = g.getFontMetrics();
		int w = fm.stringWidth("\u03b4");
		g.drawString("\u03b4", -15, 6 );
		g.setFont( new Font("SansSerif", Font.PLAIN, 7 ) );
		fm = g.getFontMetrics();
		g.drawString("18", -15+w, 3);
		w += fm.stringWidth("18");
		g.setFont( new Font("SansSerif", Font.PLAIN, 9 ) );
		g.drawString("O - smow", -15+w, 6);
		g.setFont( new Font("SansSerif", Font.PLAIN, 8 ) );
		fm = g.getFontMetrics();
		for(int i=-2 ; i<=2 ; i+=2) {
			int y = (int)(59. - (i+4.)/.15);
			int y1 = (int)(59. - (i+4.5)/.15);
			g.drawString(""+i,26, y1+4);
		//	g.drawString(""+(i-1),26, y+4);
			w = fm.stringWidth(""+(i-2));
			g.drawString(""+(i-2),-4-w, y+4);
		}
		g2.setTransform(at);
		g.translate( 102, 95 );
		g2.rotate( -Math.PI/2. );
		g2.scale(1.5, 1.5);
		g2.drawString("Ice Cores", 10, 0);
		g2.setTransform(at);
		g.translate( 15, 95 );
		g2.rotate( -Math.PI/2. );
		g2.scale(1.5, 1.5);
		g2.drawString("Surface Water", 0, 0);
	}
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation( frame.EXIT_ON_CLOSE);
		frame.getContentPane().add( new IceColor() );
		frame.pack();
		frame.show();
	}
}

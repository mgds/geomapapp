package org.geomapapp.image;

import org.geomapapp.util.SimpleBorder;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

public class ColorPane extends JComponent {
	public final static int RED = 0;
	public final static int GREEN = 1;
	public final static int BLUE = 2;
	public final static int HUE = 0;
	public final static int SATURATION = 4;
	public final static int BRIGHTNESS = 5;
	public int type;
	float[] color;
	float[] oldColor;
	static SimpleBorder border = new SimpleBorder();
	public ColorPane(int rgb, int type) {
		this.type = type;
		setBorder( border );
		setColor(rgb);
	}
	public Dimension getPreferredSize() {
		return new Dimension( 36, 150 );
	}
	public void setColor( int rgb ) {
		oldColor = color;
		color = type > 2 ?
			Color.RGBtoHSB( (rgb>>16)&255,
				(rgb>>8)&255,
				rgb&255,
				null)
			:
			new float[] {
				(float)((rgb>>16)&255)/255f,
				(float)((rgb>>8)&255)/255f,
				(float)(rgb&255)/255f,
			};
		if( oldColor==null ) oldColor=color;
		if( type==4 && color[1]==0f )color[1]=oldColor[1];
		if( type==3 && color[0]==0f )color[0]=oldColor[0];
		if( type==5 && color[2]==0f ) {
			color[1]=oldColor[1];
			color[0]=oldColor[0];
		}
		if(isVisible())repaint();
	}
	public int getRGB( Point p ) {
		float[] vals = new float[] { color[0], color[1], color[2] };
		Dimension dim = getSize();
		float factor = (float)(dim.height-2);
		vals[type%3] = (float)(dim.height - p.getY()+1.)/factor;
		if( vals[type%3]>1f )vals[type%3]=1f;
		else if( vals[type%3]<0f ) vals[type%3]=0f;
		if( type>2 && vals[2]<.01f )vals[2]=.01f;
		int rgb = type>2 ?
			Color.HSBtoRGB( vals[0], vals[1], vals[2] )
			: (new Color(vals[0], vals[1], vals[2])).getRGB();
		return rgb;
	}
	public int getType() {
		return type;
	}
	float getValue() {
		return color[type&3];
	}
	public void paintComponent(Graphics g) {
		Dimension dim = getSize();
		BufferedImage image = new BufferedImage(dim.width-2, 
				dim.height-2, 
				BufferedImage.TYPE_INT_RGB );
		float factor = (float)(dim.height-2);
		int y = (int)(factor*color[type%3]);
		if( y>=dim.height-4 )y=dim.height-5;
		float[] vals = new float[] { color[0], color[1], color[2] };
		int c = 0xff000000;
		for(int i=0 ; i<dim.height-2 ; i++) {
			vals[type%3] = (float)i/factor;
			if( vals[type%3]>1f) vals[type%3]=1f;
			else if(vals[type%3]<0f)vals[type%3]=0f;
			int c1 = type>2 ?
				Color.HSBtoRGB( vals[0], vals[1], vals[2] )
				: (new Color(vals[0], vals[1], vals[2])).getRGB();
			for( int k=0 ; k<dim.width-2 ; k++) {
				image.setRGB( k, dim.height-3-i, c1 );
			}
		}
		Graphics2D g2 = (Graphics2D)g;
		g2.drawImage( image, 1, 1, this);
		g2.setColor(Color.white);
		g2.setStroke( new BasicStroke(3f));
		g2.drawLine(0, dim.height-3-y, (dim.width-2)/2, dim.height-3-y);
		g2.setColor(Color.black);
		g2.setStroke( new BasicStroke(1f));
		g2.drawLine(0, dim.height-3-y, (dim.width-2)/2, dim.height-3-y);
	}
}

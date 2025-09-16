package haxby.db.radar;

import haxby.image.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class RBorder {
	RImage line;
	boolean paintTitle = true;
	Font titleFont, font;
	int titleH, titleWidth, titleY;
	int anotH, anotY, anotW;
	String title, yTitle;
	Insets insets;
	static double[] dAnot = {2, 2.5, 2};
	static AffineTransform rotate90 = new AffineTransform(0, -1, 1, 0, 0, 0);
	public RBorder(RImage line) {
		this.line = line;
		font = new Font("SansSerif", Font.PLAIN, 10);
		titleFont = new Font("SansSerif", Font.PLAIN, 12);
		title = line.getCruiseID() +", Line "+ line.getID() +", Shot#";
		yTitle = "2-way time, microsec";
		setTitleSize();
		setAnotSize();
	}
	public Insets getBorderInsets(Component c) {
		return new Insets(titleH + anotH, anotW, 0, 0);
	}
	public Insets getBorderInsets(Component c, Insets insets) {
		if(insets == null)return getBorderInsets(c);
		insets.left = anotW;
		insets.top = titleH + anotH;
		insets.right = 0;
		insets.bottom = 0;
		return insets;
	}
	public boolean isBorderOpaque() { return true; }
	public void paintBorder(Component c, Graphics g,
				int x, int y, int w, int h) {
		if( c != line ) return;
		boolean flip = line.isFlip();
		Graphics2D g2 = (Graphics2D)g;
		AffineTransform at = g2.getTransform();
		g.setColor(line.isRevVid() ? Color.black : Color.white);
		g.fillRect(x,y,w,h);
		g.setColor(line.isRevVid() ? Color.white : Color.black);
		if(paintTitle) {
			g.setFont(titleFont);
			g.drawString(title, x + (w - titleWidth)/2, y+titleY);
		}
		FontMetrics fm = line.getFontMetrics(font);
		x += anotW;
		w -= anotW;
		y += titleH+anotH;
		h -= titleH+anotH;
		g.translate(x, y);
		g.drawRect(-1,-1,w+2,h+2);
		g.drawRect(-1,-1,w+2,h+2);
		double zoom = line.getZoomX();
		double cdp1 = flip ? line.cdpInterval[1] : line.cdpInterval[0];
		double cdp2 = flip ? line.cdpInterval[0] : line.cdpInterval[1];
		double cdpScale = (cdp2 - cdp1) / (zoom*line.width);
		double cdpMin;
		double cdpMax;
		if( flip ) {
			cdpMax = cdp1 + (x-anotW)*cdpScale;
			cdpMin = cdpMax + w*cdpScale ;
		} else {
			cdpMin = cdp1 + (x-anotW)*cdpScale;
			cdpMax = cdpMin + w*cdpScale ;
		}
		int k = 0;
		double cdpInt = 1;
		int a1 = (int)Math.ceil(cdpMin/cdpInt);
		int a2 = (int)Math.floor(cdpMax/cdpInt);
		int wMax = 2*fm.stringWidth(Integer.toString(a2));
		while( wMax*(a2-a1+1) > w ) {
			cdpInt *= dAnot[k];
			k = (k+1)%3;
			a1 = (int)Math.ceil(cdpMin/cdpInt);
			a2 = (int)Math.floor(cdpMax/cdpInt);
		} 
		a1 = (int)Math.ceil(cdpMin/cdpInt);
		a2 = (int)Math.floor(cdpMax/cdpInt);
		int da = (int)Math.rint(cdpInt);
		int ax, aw;
		String anot;
		g.setFont(font);
		for( k=a1 ; k<=a2 ; k++ ) {
			if(flip) {
				ax = (int)Math.rint( (cdpInt*(double)k - cdpMax)/cdpScale );
			} else {
				ax = (int)Math.rint( (cdpInt*(double)k - cdpMin)/cdpScale );
			}
			g.fillRect(ax-1, -5, 2, 5);
			anot = Integer.toString((int)(k*cdpInt));
			aw = fm.stringWidth(anot);
			g.drawString(anot, ax-aw/2, -6);
		}

		g.translate(0, h);
		g2.transform(rotate90);
		g.setFont(titleFont);
		g.drawString(yTitle, (h-fm.stringWidth(yTitle))/2, -anotH);
		g.setFont(font);
		zoom = line.getZoomY();
		double t1 = line.tRange[0]/10;
		double t2 = line.tRange[1]/10;
		double tScale = (t2 - t1) / (zoom*line.height);
		double tMin = t1 + (y-titleH-anotH)*tScale;
		double tMax = tMin + h*tScale ;
		k = 0;
		double tInt = 1;
		a1 = (int)Math.ceil(tMin/tInt);
		a2 = (int)Math.floor(tMax/tInt);
		wMax = 2*fm.stringWidth(Integer.toString(a2));
		while( wMax*(a2-a1+1) > h ) {
			tInt *= dAnot[k];
			k = (k+1)%3;
			a1 = (int)Math.ceil(tMin/tInt);
			a2 = (int)Math.floor(tMax/tInt);
		} 
		a1 = (int)Math.ceil(tMin/tInt);
		a2 = (int)Math.floor(tMax/tInt);
		da = (int)Math.rint(tInt);
		for( k=a1 ; k<=a2 ; k++ ) {
			ax = h-(int)Math.rint( (tInt*(double)k - tMin)/tScale );
			g.fillRect(ax-1, -5, 2, 5);
			da = (int)(k*tInt);
			anot = Integer.toString(da/100);
			int tmp = da%100;
			if( tmp != 0 ) {
				if( tmp < 10 ) {
					anot += ".0"+tmp;
				} else if( tmp%10 == 0) {
					anot += "."+(tmp/10);
				} else {
					anot += "."+tmp;
				}
			}
			aw = fm.stringWidth(anot);
			g.drawString(anot, ax-aw/2, -6);
		}
		g2.setTransform(at);
	}
	public void setPaintTitle(boolean tf) {
		paintTitle = tf;
	}
	public void setTitle() {
		title = line.getCruiseID() +" Line "+ line.getID() +", Shot#";
		setTitleSize();
	}
	void setTitleSize() {
		if( !paintTitle ) {
			titleH = 0;
			return;
		}
		FontMetrics fm = line.getFontMetrics(titleFont);
		titleWidth = fm.stringWidth(title);
		titleY = fm.getLeading() + fm.getHeight();
		titleH = titleY + fm.getDescent();
	}
	void setAnotSize() {
		FontMetrics fm = line.getFontMetrics(font);
		anotH = fm.getLeading() + fm.getHeight() + 5;
		anotY = fm.getLeading() + fm.getHeight();
		anotW = fm.getLeading() + fm.getHeight() + anotH;
	}
}

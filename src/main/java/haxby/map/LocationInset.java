
package haxby.map;

import haxby.db.Profile;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;

import org.geomapapp.image.Arrow;

public class LocationInset implements MapInset,
										MouseMotionListener,
										MouseListener
{
	int x=Integer.MAX_VALUE,y=0;
	XMap map;
	int h,w;
	Image img1,img2,img3;
	boolean dragWindow=false;
	boolean dragMap=false;
	Point lastPoint;
	Arrow arrow;
	Rectangle2D.Double rect;
	int xOffset;
	int yOffset;
	boolean profileEnabled;	

	public LocationInset(XMap map){
		this.map=map;
		BufferedImage img;
		try {
			ClassLoader loader = LocationInset.class.getClassLoader();
			URL url = loader.getResource("org/geomapapp/resources/maps/smallWorld.jpg");
			img = ImageIO.read(url);
		} catch (Exception e) {
			return;
		}
		h = img.getHeight()/2;
		w = img.getWidth()/2;
		img2 = img.getScaledInstance(w, h, BufferedImage.SCALE_DEFAULT);

		img = map.getBaseMap();
		img1 = img.getScaledInstance(w*2, h*2, BufferedImage.SCALE_DEFAULT);
		img3 = img.getScaledInstance(w*3, h*3, BufferedImage.SCALE_DEFAULT);
		arrow=new Arrow(null,null);
	}

	public Rectangle2D.Double getRect() {
		return rect;
	}
	public void draw(Graphics2D g, int w, int h) {
		double zoom = map.getZoom();
		rect = null;
		if (zoom <= 2.5) return;
		boolean large = zoom>8;
		boolean xLarge = zoom>32;

		x=(int)Math.max(0, x);
		y=(int)Math.max(0, y);
		x=(int)Math.min(w-this.w, x);
		y=(int)Math.min(h-this.h, y);

		Rectangle2D r = map.getClipRect2D();
		Rectangle2D.Float box=new Rectangle2D.Float(0,0,this.w,this.h);

		AffineTransform at = g.getTransform();
		Shape clip = g.getClip();
		g.translate(x, y);
		g.clip(box);
		if (xLarge){
			rect = new Rectangle2D.Double(r.getX()*.75,r.getY()*.75,r.getWidth()*.75,r.getHeight()*.75);
			xOffset = (int)(this.w/2-(rect.x+rect.width/2));
			yOffset = (int)(this.h/2-(rect.y+rect.height/2));

			yOffset = Math.max(yOffset, -this.h*2);
			yOffset = Math.min(yOffset, 0);
//			x = Math.min(x, 0);
//			x = Math.max(x, -this.w*3);
			g.translate(xOffset,yOffset);

			g.drawImage(img3,-this.w*3,0,null);
			g.drawImage(img3,0,0,null);
			g.drawImage(img3,this.w*3,0,null);
			g.drawImage(img3,this.w*6,0,null);

			g.setColor(Color.RED);
			g.draw(rect);

			if (zoom>100) {
				arrow.p2 = new Point((int)(rect.x+rect.width),(int)(rect.y+rect.height/2));
				arrow.p1 = new Point(this.w*3/4-xOffset,(int)(this.h/4-yOffset));
				arrow.draw(g);
			}

			rect.x+=xOffset;
			rect.y+=yOffset;
			g.translate(-xOffset,-yOffset);
		}
		else if (large) {
			rect = new Rectangle2D.Double(r.getX()*.5,r.getY()*.5,r.getWidth()*.5,r.getHeight()*.5);
			xOffset = (int)(this.w/2-(rect.x+rect.width/2));
			yOffset = (int)(this.h/2-(rect.y+rect.height/2));

			yOffset = Math.max(yOffset, -this.h);
			yOffset = Math.min(yOffset, 0);
//			x = Math.min(x, 0);
//			x = Math.max(x, -this.w*3);
			g.translate(xOffset,yOffset);

			g.drawImage(img1,-this.w*2,0,null);
			g.drawImage(img1,0,0,null);
			g.drawImage(img1,this.w*2,0,null);
			g.drawImage(img1,this.w*4,0,null);

			g.setColor(Color.RED);
			g.draw(rect);

			rect.x+=xOffset;
			rect.y+=yOffset;
			g.translate(-xOffset,-yOffset);
		} else {
			rect = new Rectangle2D.Double(r.getX()*.25,r.getY()*.25,r.getWidth()*.25,r.getHeight()*.25);
			xOffset = (int)(this.w/2-(rect.x+rect.width/2));
			yOffset = 0;

			g.translate(xOffset,yOffset);

			g.drawImage(img2,0,0,null);
			g.drawImage(img2,-this.w,0,null);
			g.drawImage(img2,this.w,0,null);
			g.drawImage(img2,this.w*2,0,null);

			g.setColor(Color.RED);
			g.draw(rect);

			rect.x+=xOffset;
			rect.y+=yOffset;
			g.translate(-xOffset,-yOffset);
		}
		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke(4f));
		g.draw(box);
		g.setTransform(at);
		g.setClip(clip);
	}

	public void mouseEntered( MouseEvent evt ) {
	}
	public void mouseExited( MouseEvent evt ) {
	}
	public void mousePressed( MouseEvent evt ) {
		dragWindow = false;
		dragMap = false;
		if( evt.isControlDown() ) {
			return;
		} 

		Rectangle r = map.getVisibleRect();
		if (rect == null) return;

		r.x += x;
		r.y += y;
		r.width = this.w;
		r.height = this.h;
		if( r.contains( evt.getPoint() ) ) {
			evt.consume();
			AbstractButton b = map.getMapTools().profileB;
			profileEnabled = b.isSelected();
			if (profileEnabled) b.doClick();

			dragWindow = true;
			lastPoint = evt.getPoint();
			r.x+=rect.x;
			r.y+=rect.y;
			r.width=(int)rect.width;
			r.height=(int)rect.height;
			if (r.contains(evt.getPoint())){
				dragMap=true;
				dragWindow = false;
				drawRect();
			} else if (rect.x+rect.width>this.w){
				r.x-=this.w;
				if (r.contains(evt.getPoint())) {
					rect.x-=this.w;
					dragMap=true;
					dragWindow = false;
					drawRect();
				}
			}
			else 
				drawBox();
		}
	}
	public void mouseReleased( MouseEvent evt ) {
		if( evt.isControlDown() ) {
			dragWindow = false;
			return;
		} else if (dragMap) {
			dragMap = false;
			zoomToRect();
			map.repaint();
		}
		else if( dragWindow ) {
			dragWindow = false;
			map.repaint();
		}
		if (profileEnabled) {
			AbstractButton b = map.getMapTools().profileB;
			b.doClick();
			profileEnabled=false;
		}
	}
	public void mouseClicked( MouseEvent evt ) {
	}
	public void mouseMoved( MouseEvent evt ) {
	}
	public void mouseDragged( MouseEvent evt ) {
		if( evt.isControlDown() ) {
			dragWindow = false;
			return;
		}else if (dragMap) {
			drawRect();
			Point p = evt.getPoint();
			rect.x += p.x - lastPoint.x;
			rect.y += p.y - lastPoint.y;
			lastPoint = p;
			drawRect();
		} else if( dragWindow ) {
			drawBox();
			Point p = evt.getPoint();
			x += p.x - lastPoint.x;
			y += p.y - lastPoint.y;
			lastPoint = p;
			drawBox();
		}
	}

	public void drawBox() {
		synchronized (map.getTreeLock()) {
			Graphics2D g = (Graphics2D) map.getGraphics();
			g.setXORMode(Color.GRAY);
			g.translate(map.getVisibleRect().x, map.getVisibleRect().y);
			g.translate(x, y);
			g.drawRect(0, 0, this.w, this.h);
		}
	}

	public void drawRect(){
		synchronized (map.getTreeLock()) {
			Graphics2D g = (Graphics2D) map.getGraphics();
			g.setXORMode(Color.GRAY);
			g.translate(map.getVisibleRect().x, map.getVisibleRect().y);
			g.translate(x, y);
			g.drawRect((int)rect.x, (int)rect.y, (int)rect.width, (int)rect.height);
		}
	}

	public void zoomToRect(){
		double zoom = map.getZoom();
		double zoomBy;
		if (zoom>32) zoomBy = 4. / 3. * zoom;
		else if (zoom>8) zoomBy = 2 * zoom;
		else zoomBy = 4 * zoom;
		Point p;

		rect.x-=xOffset;
		rect.y-=yOffset;
		
		// System.out.println((rect.x+rect.width) +"\t"+ this.w*6);
		if (rect.x<0) {
			if (zoom>32) rect.x+=this.w*3;
			else if (zoom>8) rect.x+=this.w*2;
			else rect.x+=this.w;
		} 
		else if (zoom>32&&rect.x+rect.width>this.w*6) rect.x-=this.w*3;
		else if (zoom<=32&&zoom>8&&rect.x+rect.width>this.w*4) rect.x-=this.w*2;
		else if (zoom<=8&&rect.x+rect.width>this.w*2) rect.x-=this.w;

		p = new Point((int)((rect.x+rect.width/2)*zoomBy),(int)((rect.y+rect.height/2)*zoomBy));
		map.doZoom(p, 1);
	}
}

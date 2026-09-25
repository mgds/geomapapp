package org.geomapapp.util;

import org.geomapapp.geom.MapProjection;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;

import javax.swing.*;

public class BoxLayer implements Layer {
	Rectangle2D box;
	public BoxLayer( Rectangle2D rect ) {
		box = rect;
	}
	public void draw(Graphics2D g, AffineTransform aTrans, Rectangle2D bounds) {
		AffineTransform at0 = g.getTransform();
		g.transform( aTrans );
		g.setColor( Color.black );
		g.setStroke( new BasicStroke( 2f ) );
		g.draw( box );
		g.setTransform( at0 );
	}
	public Rectangle2D getBounds() {
		return box;
	}
	public void setProjection( MapProjection proj ) {
	}
	public boolean select( Point2D p, AffineTransform aTrans ) {
		return aTrans.createTransformedShape(box).contains(p);
	}
	public boolean select( Shape shape, Rectangle2D shapeBounds ) {
		if( shapeBounds.intersects(box) )return shape.intersects(box);
		return false;
	}
	public static void main(String[] args) {
		JFileChooser c = new JFileChooser(System.getProperty("user.dir"));
		int ok = c.showOpenDialog(null);
		if( ok==JFileChooser.CANCEL_OPTION )System.exit(0);
		try {
			ImageLayer im = new ImageLayer( javax.imageio.ImageIO.read(c.getSelectedFile()) );
			im.scale = 1.;
			im.x = 10.;
			im.y = 10.;
			ScalableComp sc = new ScalableComp(new Rectangle(-100, -100, 800, 600));
			sc.addLayer(im);
			BoxLayer box = new BoxLayer( new Rectangle(-100, -100, 800, 600));
			sc.addLayer(box);
			Select sel = new Select(sc);
			Pan pan = new Pan(sc);
			Zoom zoom = new Zoom(sc);
			Box tools = Box.createHorizontalBox();
		//	JPanel tools = new JPanel();
			ButtonGroup gp = new ButtonGroup();
			JToggleButton tb = sel.getToggle();
			tb.setBorder( BorderFactory.createEmptyBorder(1,1,2,2) );
			tools.add(tb);
			gp.add(tb);
			tb = pan.getToggle();
			tb.setBorder( BorderFactory.createEmptyBorder(1,1,2,2) );
			tools.add(tb);
			gp.add(tb);
			tb = zoom.getZoomInToggle();
			tb.setBorder( BorderFactory.createEmptyBorder(1,1,2,2) );
			tools.add(tb);
			gp.add(tb);
			tb = zoom.getZoomOutToggle();
			tb.setBorder( BorderFactory.createEmptyBorder(1,1,2,2) );
			tools.add(tb);
			gp.add(tb);
			Info info = new Info(sc);
			sc.addMouseMotionListener( info );
			
			sc.addMouseListener(sel);
			sc.addMouseMotionListener(sel);
			sc.addMouseListener(zoom);
			sc.addMouseMotionListener(zoom);
			sc.addMouseListener(pan);
			sc.addMouseMotionListener(pan);
			JFrame frame = new JFrame( c.getSelectedFile().getName() );
			frame.getContentPane().add( tools, "North" );
			frame.getContentPane().add( sc );
			frame.getContentPane().add( info.getLabel(), "South" );
			frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
			frame.pack();
			frame.show();
		} catch( Exception ex) {
			ex.printStackTrace();
		}
	}
}

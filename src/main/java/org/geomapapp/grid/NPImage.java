package org.geomapapp.grid;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.geomapapp.geom.PolarStereo;
import org.geomapapp.geom.XYZ;
import org.geomapapp.image.GridRenderer;
import org.geomapapp.image.MapImage;
import org.geomapapp.image.Palette;
import org.geomapapp.util.ImageComponent;
import org.geomapapp.util.Zoomer;

public class NPImage {
	ImageComponent imageC;
	MapImage image;
	PolarStereo proj;
	Grid2D.Short grid;
	Grid2D.Short grid0;
	Grid2D.Boolean landMask;
	Ice_4G ice;
	int scale;
	int x0, y0;
	JFrame frame;
	JLabel location;
	JTextField kyBP;
	int currentKYBP=0;
	java.text.NumberFormat fmt;

	public NPImage() throws IOException {
		imageC = new ImageComponent();
		image = new MapImage(imageC);
		imageC.addOverlay(image);
		JFrame fm = new JFrame();
		fm.setUndecorated(true);
		JLabel label = new JLabel("Creating Base Map");
		label.setFont( new Font("SansSerif", Font.BOLD, 48));
		label.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createRaisedBevelBorder(),
				BorderFactory.createEmptyBorder(4,4,4,4)));
		fm.getContentPane().add( label );
		fm.pack();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension dim1 = fm.getPreferredSize();
		fm.setLocation( (dim.width-dim1.width)/2, (dim.height-dim1.height)/2);
		fm.setVisible(true);
		label.paintImmediately( label.getVisibleRect() );
		frame = new JFrame("PaleoBathymetry");
		frame.getContentPane().add( new JScrollPane(imageC) );
		Zoomer z = new Zoomer(imageC);
		imageC.addMouseListener(z);
		imageC.addMouseMotionListener(z);
		imageC.addKeyListener(z);
		imageC.addKeyListener( new KeyAdapter() {
			public void keyReleased(KeyEvent evt) {
				int code = evt.getKeyCode();
				if( code==evt.VK_ENTER ) imageC.resetTransform();
				else if( code==evt.VK_G ) grid();
				else if( code==evt.VK_R ) {
					if( evt.isShiftDown() )imageC.rotate(1);
					else imageC.rotate(3);
				}
			}
		});
		imageC.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				setLocation(e.getPoint());
			}
		});
		JPanel panel = new JPanel(new FlowLayout());
		kyBP = new JTextField("0", 2);
		panel.add(kyBP);
		panel.add( new JLabel(" kyBP "));
		JButton setKYBP = new JButton("Set");
		panel.add( setKYBP );
		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setKYBP();
			}
		};
		setKYBP.addActionListener(al);
	//	kyBP.addActionListener(al);
			
		frame.getContentPane().add(panel, "North");

		location = new JLabel("----------");
		frame.getContentPane().add(location, "South");

		init();
		fm.dispose();
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
		fmt = java.text.NumberFormat.getInstance();
		fmt.setMaximumFractionDigits(3);
	}
	void setLocation( Point p) {
		Point2D.Double pt = (Point2D.Double)imageC.inverseTransform(p);
		pt.x += x0;
		pt.y += y0;
		pt = (Point2D.Double)proj.getRefXY( pt );
		Point2D p1 = grid.getProjection().getMapXY(pt);
		double z = grid.valueAt(p1.getX(), p1.getY());
		if( Double.isNaN(z) ) {
			p1 = grid0.getProjection().getMapXY(pt);
			z = grid0.valueAt(p1.getX(), p1.getY());
		}
		String txt = fmt.format(pt.getX()) +", "+ fmt.format(pt.getY());
		if( !Double.isNaN(z) ) txt =  txt+", z = "+ (int)z;
		location.setText( txt);
	}
	void setKYBP() {
		try {
			int ky = Integer.parseInt(kyBP.getText());
			if( ky==currentKYBP )return;
			currentKYBP = ky;
			frame.setCursor( Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			init();
			grid();
			frame.setCursor( Cursor.getDefaultCursor());
		} catch(Exception e) {
		}
	}
	void grid() {
		imageC.setCursor( Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		Rectangle2D.Double r = (Rectangle2D.Double)imageC.getUnscaledVisibleRect();
		double xx0 = r.x;
		double yy0 = r.y;

		double sc = imageC.getTransform().getScaleX();
		int s = 1;
		while( sc/1.4>s )s*=2;
		if( s>8 )s=8;
		if( s==1 ) {
			image.setImage(null, 0, 0, 1);
			imageC.repaint();
			imageC.setCursor( Cursor.getDefaultCursor());
			return;
		}
		r.x += x0;
		r.y += y0;
		r.x *= s;
		r.y *= s;
		r.width *= s;
		r.height *= s;
		try {
			image.setImage( getImage( s, (int)r.x, (int)r.y , (int)r.width , (int)r.height, currentKYBP),
				xx0, yy0, 1./s );
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		imageC.repaint();		
		imageC.setCursor( Cursor.getDefaultCursor());
	//	System.out.println( s +"\t"+ (int)r.x +"\t"+ (int)r.y +"\t"+ (int)r.width +"\t"+ (int)r.height);
	}
	void init() throws IOException {
		int ky=0;
		try {
			ky = Integer.parseInt(kyBP.getText());
		} catch(NumberFormatException e) {
			return;
		}
		scale = 1;
		x0 = -320;
		y0 = -320;
		int width = 640;
		int height = 640;
		proj = new PolarStereo( new Point(0, 0),
				0., 20000./scale, 75., PolarStereo.NORTH,
				PolarStereo.WGS84);
		imageC.setImage(getImage(scale,x0,y0,width,height,ky));
		grid0 = grid;
	}
	public BufferedImage getImage( int scale, 
					int x0, 
					int y0, 
					int width, 
					int height, 
					int ky) 
						throws IOException {
		PolarStereo proj = new PolarStereo( new Point(0, 0),
				0., 20000./scale, 75., PolarStereo.NORTH,
				PolarStereo.WGS84);
		TileIO.Short tiler = new TileIO.Short(
					proj, Ice_4G.BASE+"NP_320/z_"+scale, 320, 1);
		Rectangle bounds = new Rectangle(x0, y0, width, height);
		grid = new Grid2D.Short( bounds,
					proj);
		landMask = new Grid2D.Boolean( bounds, proj);
		
		TiledGrid tg = new TiledGrid( proj, bounds, tiler, 320, 16, null );
		grid = (Grid2D.Short)tg.composeGrid(grid);
		short[] z = new short[grid.getBuffer().length];
		System.arraycopy(grid.getBuffer(),0, z, 0, z.length);
	//	Ice4G ice = new Ice4G();
		if( ice==null )ice = new Ice_4G();
		ice.setKYBP(ky);
		int k=0;
		for( int y=y0 ; y<y0+height ; y++) {
			for( int x=x0 ; x<x0+width ; x++) {
				short i = z[k++];
				if( i==grid.NaN )continue;
			//	Point2D pt = proj.getRefXY(new Point(x,y));
			//	grid.setValue(x,y,i+ice.getDiff(pt.getX(),pt.getY()));
				grid.setValue(x,y,i+ice.getDiff( (320.+1.*x/scale)/4., (320.+1.*y/scale)/4.));
				i = grid.shortValue(x,y);
				landMask.setValue( x, y, i>=0 );
			}
		}
		Palette land = new Palette( 2 );
		Palette ocean = new Palette( Ice_4G.BASE+"ocean2.lut" );
		GridRenderer gr = new GridRenderer( new Palette(0),
			8., 2500., new XYZ(-1., 1., 2.) );
		gr.setLandPalette(land);
		gr.setOceanPalette(ocean);
	//	gr.setPalette( new Palette(0) );
		BufferedImage image = gr.gridImage( grid, landMask ).image;
		for( int y=y0 ; y<y0+height-1 ; y++) {
			for( int x=x0 ; x<x0+width-1 ; x++) {
				short i = grid.shortValue(x,y);
				if( i==grid.NaN )continue;
			//	Point2D pt = proj.getRefXY(new Point(x,y));
			//	if( ice.isIce(pt.getX(),pt.getY()) )image.setRGB(x-x0, y-y0, 0xffffffff);
				if( ice.isIce((320.+1.*x/scale)/4., (320.+1.*y/scale)/4.) ) image.setRGB(x-x0, y-y0, 0xffffffff);
			}
		}
		return image;
	}
	public static void main(String[] args) {
		try {
		//	new NPImage(1, -400, -400, 10);
			new NPImage();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}

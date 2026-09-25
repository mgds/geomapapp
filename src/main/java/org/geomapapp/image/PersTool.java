package org.geomapapp.image;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.NumberFormat;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.geomapapp.geom.GCPoint;
import org.geomapapp.geom.MapProjection;
import org.geomapapp.geom.Mercator;
import org.geomapapp.geom.Perspective3D;
import org.geomapapp.geom.XYZ;
import org.geomapapp.grid.Grid2D;
import org.geomapapp.grid.Grid2DOverlay;
import org.geomapapp.grid.GridComposer;
import org.geomapapp.image.GridRenderer.RenderResult;
import org.geomapapp.util.Icons;
import org.geomapapp.util.SimpleBorder;
import org.geomapapp.util.Zoomer;

public class PersTool extends JPanel 
			implements PerspectiveGeometry, ComponentListener {
	Grid2DOverlay grid;
	PerspectiveImage pImage;
	VETool veTool;
	GCPoint view, focus;
	double veFactor = 1.;
	public PersTool( Grid2DOverlay grid ) {
		super( new BorderLayout() );
		
		this.addComponentListener(this);
		
		veTool = new VETool(2.);
		vePropListener = new java.beans.PropertyChangeListener() {
			public void propertyChange(
					java.beans.PropertyChangeEvent e) {
				if( e.getPropertyName().equals("ancestor"))return;
				if( e.getPropertyName().equals("border") ) return;
				try {
					double oldVE = ((Double)e.getOldValue()).doubleValue();
					double newVE = ((Double)e.getNewValue()).doubleValue();
					updateVE(oldVE, newVE);
				} catch(Exception ex) {

				}
			}
		};
		veTool.addPropertyChangeListener(
			vePropListener);
		Grid2D g = grid.getGrid();
		pImage = new PerspectiveImage( g, this);
		pImage.setBorder( BorderFactory.createTitledBorder("Lo-Res Preview"));
		setGrid( grid);
		initPers();
		init();
	}
	
	public void componentHidden(ComponentEvent e) {
	}
	
	public void componentMoved(ComponentEvent e) {
	}
	
	public void componentResized(ComponentEvent e) {
		update();
	}
	
	public void componentShown(ComponentEvent e) {
		update();
	}
	
	void initPers() {
		pImageMouseListener = new javax.swing.event.MouseInputAdapter() {
			long when = 0L;
			public void mousePressed(MouseEvent evt) {
				when = evt.getWhen();
				initDrag( evt.getPoint() );
			}
			public void mouseDragged(MouseEvent evt) {
				drag( evt.getPoint(), evt.isShiftDown());
			}
			public void mouseClicked(MouseEvent evt) {
				recenter(evt.getPoint());
			}
			public void mouseReleased(MouseEvent evt) {
				if( evt.getWhen()-when<300L) {
					return;
				}
				while(!update()) {
					try {
						Thread.currentThread().sleep(200);
					} catch(Exception ex) {
					}
				}
			}
		};
		javax.swing.event.MouseInputAdapter mouse = 
			pImageMouseListener;
		pImage.addMouseListener(mouse);
		pImage.addMouseMotionListener(mouse);
	}
	public void setVEFactor(double f) {
		veFactor = f;
	}
	void setGrid(Grid2DOverlay grid) {
		setGrid(grid, false);
	}
	
	public void setGrid(Grid2DOverlay grid, boolean isSelected) {
		setGrid(grid, isSelected, false);
	}
	public void setGrid(Grid2DOverlay grid, boolean isSelected, boolean isGMRT) {
		this.grid = grid;
		Grid2D g = grid.getGrid();
		if( g==null ) return;
		pImage.setGrid(g);
		double ve = veTool.getVE();
		pImage.setVE(veFactor*ve);
		Rectangle bnds = g.getBounds();
		MapProjection proj = g.getProjection();
		Point2D f = proj.getRefXY( new Point(bnds.x+bnds.width/2,
					bnds.y+bnds.height/2));
		Point2D p = proj.getMapXY( f );
		double z0 = g.valueAt(p.getX(), p.getY(), isGMRT);
		if( Double.isNaN(z0) )z0=0.;
		Point2D vp = proj.getRefXY( new Point(bnds.x-bnds.width/2,
					bnds.y+bnds.height*2));
		XYZ r1 = XYZ.LonLat_to_XYZ(vp);
		XYZ r2 = XYZ.LonLat_to_XYZ(f);
		
		double scale = proj.major[proj.SPHERE]*Math.acos(r1.dot(r2));
		view = new GCPoint(vp.getX(), vp.getY(), z0*ve+.4*scale);
		focus = new GCPoint(f.getX(), f.getY(), z0*ve);
		elevate(25.);
		angle = 25.;
		
		if (isSelected)
			update();
	}
	
	Point lastP;
	double angle;
	void initDrag( Point p) {
		Insets ins = pImage.getInsets();
		p.x -= ins.left;
		p.y -= ins.top;
		lastP = p;
	}
	void drag( Point p, boolean shift) {
		Insets ins = pImage.getInsets();
		p.x -= ins.left;
		p.y -= ins.top;
		if( spinB.isSelected() ) {
			double d = .1*(p.getX()-lastP.getX());
			if( shift ) d = .1*d;
			spin(d);
			if(update())lastP = p;
		} else if( inclineB.isSelected() ) {
			double d = -.1*(p.getY()-lastP.getY());
			if( shift ) d = .1*d;
			d += angle;
			if( d>89.5 )d=89.5;
			if(d<0.) d=0.;
			elevate(d);
			if(update())lastP = p;
		} else if( scaleB.isSelected() ) {
			double d = .2*(p.getX()-lastP.getX());
			if( shift ) d = .1*d;
			double factor = 1. + .01*Math.abs(d);
			if(d<0.) factor = 1./factor;
			scale(factor);
			if(update())lastP = p;
		} else if( moveB.isSelected() ) {
			double dx = .2*(p.getX()-lastP.getX());
			if( shift ) dx = .1*dx;
			double dy = p.getY()-lastP.getY();
			if( shift ) dy = .1*dy;
			move( dx, dy);
			if(update())lastP = p;
		}
	}
	void move( double dx, double dy) {
		Dimension d = pImage.getSize();
		Insets ins = pImage.getInsets();
		d.width -= ins.left+ins.right;
		d.height -= ins.top+ins.bottom;
		recenter( new Point2D.Double(d.width*.5+dx, d.height*.5+dy));
	}
	void recenter(Point2D p) {
		Dimension d = pImage.getSize();
		Insets ins = pImage.getInsets();
		d.width -= ins.left+ins.right;
		d.height -= ins.top+ins.bottom;
		p.setLocation( p.getX()-ins.left, p.getY()-ins.top);
		double z = pImage.zBuf.valueAt(p.getX(), p.getY());
		if( Double.isNaN(z))return;
		XYZ f = new XYZ(-d.width*.5+p.getX(), -d.height*.5+p.getY(), z);
		f = getPerspective().inverse(f);
		focus = f.getGCPoint();
		update();
	}
	void updateVE(double oldVE, double newVE) {
		double dz = focus.elevation*newVE/oldVE - focus.elevation;
		focus.elevation += dz;
		view.elevation += dz;
		update();
	}
	boolean update() {
		Dimension d = pImage.getSize();
		Insets ins = pImage.getInsets();
		d.width -= ins.left+ins.right;
		d.height -= ins.top+ins.bottom;
		Rectangle r = new Rectangle( -d.width/2, -d.height/2,
					d.width, d.height);
		
		return pImage.run( getPerspective(), r,
				grid.getGrid(), grid.getImage(), veTool.getVE());
	}
	void spin( double angle ) {
		XYZ vp = view.getXYZ();
		XYZ f = focus.getXYZ();
		XYZ tmp = focus.getXYZ();
		XYZ dif = vp.minus(f);
		double distance = dif.getNorm();
		double a0 = tmp.normalize().dot(dif.normalize());
		a0 = 90.-Math.toDegrees( Math.acos( a0 ));
		double a1 = distance*Math.cos(Math.toRadians(angle));
		double a2 = distance*Math.sin(Math.toRadians(angle));
		XYZ n1 = f.cross(dif).cross(f).normalize();
		XYZ n2 = f.cross(dif).normalize();
		vp = f.plus( n1.times(a1) ).plus( n2.times(a2) );
		view = vp.getGCPoint();
		elevate( a0);
	}
	void elevate(double angle) {
		this.angle = angle;
		XYZ vp = view.getXYZ();
		XYZ f = focus.getXYZ();
		XYZ tmp = focus.getXYZ();
		XYZ dif = vp.minus(f);
		double distance = dif.getNorm();
		double a1 = distance*Math.cos(Math.toRadians(angle));
		double a2 = distance*Math.sin(Math.toRadians(angle));
		XYZ n1 = f.cross(dif).cross(f).normalize();
		XYZ n2 = tmp.normalize();
		vp = f.plus( n1.times(a1) ).plus( n2.times(a2) );
		view = vp.getGCPoint();
	}
	void scale(double factor) {
		XYZ vp = view.getXYZ();
		XYZ f = focus.getXYZ();
		XYZ dif = vp.minus(f);
		double distance = dif.getNorm();
		dif.normalize();
		distance *= factor;
		vp = f.plus(dif.times(distance));
		view = vp.getGCPoint();
	}
	public Perspective3D getPerspective() {
		Dimension d = pImage.getSize();
		Insets ins = pImage.getInsets();
		d.width -= ins.left+ins.right;
		d.height -= ins.top+ins.bottom;
		Rectangle r = new Rectangle( -d.width/2, -d.height/2,
					d.width, d.height);
		return new Perspective3D(view, focus, 20., d.width);
	}
	JToggleButton spinB, inclineB, scaleB, moveB;
	JTextField widthF, heightF;
	JCheckBox lowQ, mediumQ, highQ;
	void init() {
		add( pImage);
		JPanel panel = new JPanel(new BorderLayout() );
		panel.add(veTool.getPanel(),"North");
		ButtonGroup gp = new ButtonGroup();

		cursorListener = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				cursor();
			}
		};
		spinB = new JToggleButton(
				Icons.getIcon( Icons.SPIN, false));
		spinB.setSelectedIcon( Icons.getIcon( Icons.SPIN, true));
		gp.add(spinB);

		inclineB = new JToggleButton(
				Icons.getIcon( Icons.INCLINE, false));
		inclineB.setSelectedIcon( Icons.getIcon( Icons.INCLINE, true));
		gp.add(inclineB);

		scaleB = new JToggleButton(
				Icons.getIcon( Icons.ZOOM_IN, false));
		scaleB.setSelectedIcon( Icons.getIcon( Icons.ZOOM_IN, true));
		gp.add(scaleB);

		moveB = new JToggleButton(
				Icons.getIcon( Icons.MOVE, false));
		moveB.setSelectedIcon( Icons.getIcon( Icons.MOVE, true));
		gp.add(moveB);

		JPanel buttons = new JPanel(new GridLayout(0,1,2,2));
		spinB.addActionListener(cursorListener);
		inclineB.addActionListener(cursorListener);
		scaleB.addActionListener(cursorListener);
		moveB.addActionListener(cursorListener);
		spinB.setBorder(null);
		inclineB.setBorder(null);
		scaleB.setBorder(null);
		moveB.setBorder(null);
		buttons.add(spinB);
		buttons.add(inclineB);
		buttons.add(scaleB);
	//	buttons.add(moveB);
		spinB.setSelected(true);

		JPanel size = new JPanel( new GridLayout(0,1));
		size.setBorder( new SimpleBorder() );

		gp = new ButtonGroup();
		size.add(new JLabel("Quality"));
		lowQ = new JCheckBox( "so-so");
		gp.add( lowQ );
		mediumQ = new JCheckBox( "better" );
		gp.add( mediumQ );
		highQ = new JCheckBox( "best" );
		gp.add( highQ );
		JPanel quality = new JPanel( new GridLayout(1,0));
		quality.add( lowQ );
		quality.add( mediumQ );
		quality.add( highQ );
		mediumQ.setSelected(true);
		highQ.setEnabled(false);
		size.add( quality );

		size.add( new JLabel("Width (Pixels)"));
		widthF = new JTextField("700");
		size.add(widthF);
		size.add( new JLabel("Height (Pixels)"));
		heightF = new JTextField("500");
		size.add(heightF);

		renderB = new JButton("Render as an Image");
		renderListener = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				render();
				setCursor(Cursor.getDefaultCursor());
			}
		};
		renderB.addActionListener( renderListener); 
		size.add(renderB);

		JPanel panel1 = new JPanel(new BorderLayout());
		JPanel panel2 = new JPanel(new BorderLayout());
		panel2.add(buttons, "North");
		panel1.add(panel2,"East");
		panel1.add(size,"Center");
		panel.add(panel1,"South");
		add(panel, "West");
	}
	void cursor() {
		if( moveB.isSelected() ) {
			pImage.setCursor(Cursor.getPredefinedCursor(
					Cursor.MOVE_CURSOR));
		} else if( inclineB.isSelected() ) {
			pImage.setCursor(Cursor.getPredefinedCursor(
					Cursor.N_RESIZE_CURSOR));
		} else if( scaleB.isSelected() ) {
			 pImage.setCursor(haxby.util.Cursors.ZOOM_IN());
		} else {
			 pImage.setCursor(Cursor.getDefaultCursor());
		}
	}
	PerspectiveImage pi;
	void render() {
		if(grid.getGrid()==null) return;
		double ve = veTool.getVE();
		pi = new PerspectiveImage( grid.getGrid(),
				this);
		pi.setVE(veFactor*ve);
		Grid2D g = grid.getGrid();
		Rectangle bnds = g.getBounds();
		MapProjection proj = g.getProjection();
		int w=0;
		int h=0;
		String message=null;
		try {
			w = Integer.parseInt(widthF.getText());
			h = Integer.parseInt(heightF.getText());
		} catch(Exception ex) {
			message = "Couldn\'t interpret width/height fields";
		}
		if( w<0 || h<0 ) message = "width and height have to be > 0";
		if( message!=null) {
			JOptionPane.showMessageDialog(this,
					message,
					"try again",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		if( w*h>2000000) {
			JPanel panel = new JPanel(new GridLayout(0,1));
			panel.add(new JLabel("Resizing to 2 megapixels is recommended"));
			ButtonGroup gp = new ButtonGroup();
			JCheckBox aspect = new JCheckBox("preserve aspect ratio");
			gp.add(aspect);
			panel.add(aspect);
			aspect.setSelected(true);
			JCheckBox wid = new JCheckBox("preserve width");
			gp.add(wid);
			panel.add(wid);
			JCheckBox ht = new JCheckBox("preserve height");
			gp.add(ht);
			panel.add(ht);
						JCheckBox noChange = new JCheckBox("mind your own business");
						gp.add(noChange);
						panel.add(noChange);

			int ok = JOptionPane.showConfirmDialog(this,
					panel,
					"change size",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE);
			if(ok==JOptionPane.CANCEL_OPTION)return;
			if( aspect.isSelected() ) {
				double factor = 2000000. / (w*h);
				factor = Math.sqrt(factor);
				w = (int) (w*factor);
				h = (int) (h*factor);
			} else if( wid.isSelected() ) {
				h = 2000000/w;
			} else if(ht.isSelected()) {
				w = 2000000/h;
			}
			widthF.setText( Integer.toString(w) );
			heightF.setText( Integer.toString(h) );
		}
		Perspective3D pers = new Perspective3D(view, focus, 20., w);
		bnds = new Rectangle( -w/2, -h/2, w, h);
		JFrame dialog = null;
		try {
			if( highQ.isSelected() ) {
				dialog = new JFrame( 
					"3D Pendering Progress" );
				JLabel label = new JLabel("Determining grid bounds");
				label.setBorder( BorderFactory.createEmptyBorder( 4,4,4,4 ));
				JToggleButton cancel = new JToggleButton("Cancel");
				dialog.getContentPane().add(cancel, "West");
				dialog.getContentPane().add(label);
				dialog.pack();
				dialog.show();
				label.paintImmediately(label.getVisibleRect());
				cancel.paintImmediately(cancel.getVisibleRect());
		try {
			Thread.currentThread().sleep(100L);
		}catch(Exception ex) {
		}
			if( cancel.isSelected() ) return;
				double lat = view.latitude;
				double lon = view.longitude;
				Mercator merc = new Mercator(lon-180. ,0., 1024, 0, 0);
				Rectangle gBnds = new Rectangle(0,0,1024,1024);
				if( lat>10. ) {
					Point2D p1 = merc.getMapXY(
						new Point(0, 80));
					Point2D p2 = merc.getMapXY(
						new Point2D.Double(0, lat-90.));
					gBnds.y = (int)Math.floor(p1.getY());
					gBnds.height = (int)Math.ceil(p2.getY()-p1.getY());
				} else if( lat<-10.) {
					Point2D p1 = merc.getMapXY(
						new Point2D.Double(0, lat+90.));
					Point2D p2 = merc.getMapXY(
						new Point(0, -80));
					gBnds.y = (int)Math.floor(p1.getY());
					gBnds.height = (int)Math.ceil(p2.getY()-p1.getY());
				} else {
					Point2D p1 = merc.getMapXY(
						new Point(0, 80));
					Point2D p2 = merc.getMapXY(
						new Point(0, -80));
					gBnds.y = (int)Math.floor(p1.getY());
					gBnds.height = (int)Math.ceil(p2.getY()-p1.getY());
				}
				Grid2D.Short globe = new Grid2D.Short(gBnds, merc);
				int imw = gBnds.width-1;
				int imh = gBnds.height-1;
				BufferedImage gImage = new BufferedImage(imw, imh,
					BufferedImage.TYPE_INT_RGB);
				int white = 0xffffffff;
				double[] range = grid.getGrid().getRange();
				for( int x=0 ; x<imw ; x++) {
					for( int y=0 ; y< imh ; y++) {
						gImage.setRGB(x,y,white);
					}
				}
				for( int x=gBnds.x ; x<gBnds.x+gBnds.width ; x++) {
					for( int y=gBnds.y ; y<gBnds.y+gBnds.height ; y++) {
						globe.setValue( x, y, range[0] );
					}
				}
				PerspectiveImage pIm  = new PerspectiveImage( globe,
					this);
				pIm.setVE(veFactor*ve);
				Grid2D.Float zBuf = pIm.render( pers, bnds, globe, gImage, false);
				BufferedImage im = pIm.getImage();
				double[] wesn = new double[4];
				wesn[0] = wesn[1] = focus.longitude;
				wesn[2] = wesn[3] = focus.latitude;
				double minZ, maxZ;
				maxZ = 0.;
				minZ = 1.e10;
				double clat0 = Math.cos(Math.toRadians(focus.latitude));
				XYZ pt1 = pers.minusVP(pers.inverse(new XYZ( -.5, 0., .001))).normalize();
				XYZ pt2 = pers.minusVP(pers.inverse(new XYZ( .5, 0., .001))).normalize();
				double a0 = Math.abs(Math.sin(Math.acos(pt1.dot(pt2))));
				double[] zTest = new double[513];
				for( int res=1 ; res<=512 ; res*=2 ) {
					double dx = clat0*2.*Math.PI/(640.*res);
					zTest[res] = dx/a0;
				}
				for( int x=0 ; x<w ; x++) {
					for( int y=0 ; y< h ; y++) {
						if(im.getRGB(x,y)!=white)continue;
						XYZ pt = new XYZ(x-w*.5, y-h*.5, zBuf.valueAt(x,y));
						if( pt.z>maxZ ) maxZ=pt.z;
						if( pt.z<minZ ) minZ=pt.z;
						GCPoint geo = pers.inverse(pt).getGCPoint();
						if( geo.longitude>focus.longitude+180. ) {
							geo.longitude-=360.;
						} else if( geo.longitude<focus.longitude-180. ) {
							geo.longitude+=360.;
						}
						if( geo.longitude>wesn[1]) {
							wesn[1] = geo.longitude;
						} else if( geo.longitude<wesn[0] ) {
							wesn[0] = geo.longitude;
						}
						if( geo.latitude>wesn[3]) {
							wesn[3] = geo.latitude;
						} else if( geo.latitude<wesn[2] ) {
							wesn[2] = geo.latitude;
						}
					}
				}
				if( range[1]<view.elevation*ve ) {
					for( int x=gBnds.x ; x<gBnds.x+gBnds.width ; x++) {
						for( int y=gBnds.y ; y<gBnds.y+gBnds.height ; y++) {
							globe.setValue( x, y, range[1] );
						}
					}
					pIm.setVE(veFactor*ve);
					zBuf = pIm.render( pers, bnds, globe, gImage, false);
					im = pIm.getImage();
					for( int x=0 ; x<w ; x++) {
						for( int y=0 ; y< h ; y++) {
							if(im.getRGB(x,y)!=white)continue;
							XYZ pt = new XYZ(x-w*.5, y-h*.5, zBuf.valueAt(x,y));
							if( pt.z>maxZ ) maxZ=pt.z;
							if( pt.z<minZ ) minZ=pt.z;
							GCPoint geo = pers.inverse(pt).getGCPoint();
							if( geo.longitude>focus.longitude+180. ) {
								geo.longitude-=360.;
							} else if( geo.longitude<focus.longitude-180. ) {
								geo.longitude+=360.;
							}
							if( geo.longitude>wesn[1]) {
								wesn[1] = geo.longitude;
							} else if( geo.longitude<wesn[0] ) {
								wesn[0] = geo.longitude;
							}
							if( geo.latitude>wesn[3]) {
								wesn[3] = geo.latitude;
							} else if( geo.latitude<wesn[2] ) {
								wesn[2] = geo.latitude;
							}
						}
					}
				}
				System.out.println("wesn:\t"+ wesn[0]+"\t"+
						wesn[1]+"\t"+
						wesn[2]+"\t"+
						wesn[3]);
				System.out.println( "z range:\t"+ minZ +"\t"+ maxZ);
				Grid2D saveGrid = grid.getGrid();
				Grid2D.Boolean saveLand = grid.getLandMask();
				boolean save1 = grid.hasLand();
				boolean save2 = grid.hasOcean();
				Grid2DOverlay overlay = grid;
				boolean start = true;
				boolean both = bothB.isSelected();
				NumberFormat fmt = NumberFormat.getInstance();
				fmt.setMaximumFractionDigits(2);
				for( int res=1 ; res<=512 ; res*=2) {
//	zTest[res] /=2;
					if( res!=512 && zTest[res]>maxZ ) continue;
					if( zTest[res]<minZ ) continue;
					if( !start ) {
						wesn[0] = wesn[1] = focus.longitude;
						wesn[2] = wesn[3] = focus.latitude;
						for( int x=0 ; x<w ; x++) {
							for( int y=0 ; y< h ; y++) {
								double val = zBuf.valueAt(x,y);
								if( Double.isNaN(val))continue;
								if(  !start && val>zTest[res] )continue;
								XYZ pt = new XYZ(x-w*.5, y-h*.5, val);
								GCPoint geo = pers.inverse(pt).getGCPoint();
								if( geo.longitude>focus.longitude+180. ) {
									geo.longitude-=360.;
								} else if( geo.longitude<focus.longitude-180. ) {
									geo.longitude+=360.;
								}
								if( geo.longitude>wesn[1]) {
									wesn[1] = geo.longitude;
								} else if( geo.longitude<wesn[0] ) {
									wesn[0] = geo.longitude;
								}
								if( geo.latitude>wesn[3]) {
									wesn[3] = geo.latitude;
								} else if( geo.latitude<wesn[2] ) {
									wesn[2] = geo.latitude;
								}
								if( !start )continue;
								pt = new XYZ(x-w*.5, y-h*.5, val*1.05);
								geo = pers.inverse(pt).getGCPoint();
								if( geo.longitude>focus.longitude+180. ) {
									geo.longitude-=360.;
								} else if( geo.longitude<focus.longitude-180. ) {
									geo.longitude+=360.;
								}
								if( geo.longitude>wesn[1]) {
									wesn[1] = geo.longitude;
								} else if( geo.longitude<wesn[0] ) {
									wesn[0] = geo.longitude;
								}
								if( geo.latitude>wesn[3]) {
									wesn[3] = geo.latitude;
								} else if( geo.latitude<wesn[2] ) {
									wesn[2] = geo.latitude;
								}
							}
						}
					}
					Mercator mProj = new Mercator(0.,0.,640*res, 0, 0);
					System.out.println( res +"\t"+
						wesn[0] +"\t"+
						wesn[1] +"\t"+
						wesn[2] +"\t"+
						wesn[3] );
					Point2D ul = mProj.getMapXY(new Point2D.Double(wesn[0],wesn[3]));
					int x1 = (int)Math.floor(ul.getX());
					int y1 = (int)Math.floor(ul.getY());
					ul = mProj.getMapXY(new Point2D.Double(wesn[1],wesn[2]));
					int x2 = (int)Math.ceil(ul.getX());
					int y2 = (int)Math.ceil(ul.getY());
					double zoom = res;
					Rectangle2D.Double rect = new Rectangle2D.Double(
						x1/zoom, y1/zoom+260., (x2-x1)/zoom, (y2-y1)/zoom); 
					System.out.println( "\t"+ (x2-x1) +"\t"+ (y2-y1));
				label.setText("composing "+ (x2-x1) +" x "+ (y2-y1)
						+" grid, zoom factor = "+res);
				dialog.pack();
				label.paintImmediately(label.getVisibleRect());
				cancel.paintImmediately(cancel.getVisibleRect());
		try {
			Thread.currentThread().sleep(100L);
		}catch(Exception ex) {
		}
				if( cancel.isSelected() ) return;
					if( (x2-x1)<2 || (y2-y1)<2) continue;
					if(!GridComposer.getGrid( rect, overlay, 512, zoom))continue;
	System.out.println( "\t*composed");
					RenderResult renderResult 
						= both ?
								renderer.gridImage( overlay.getGrid() ) :
								renderer.gridImage( overlay.getGrid(), 
										overlay.getLandMask());
					BufferedImage image = renderResult.image; 
	System.out.println( "\trendered\t"+bothB.isSelected() +"\t"+ both);
			label.setText("projecting "+ (x2-x1) +" x "+ (y2-y1)
				+" grid, zoom factor = "+res);
				dialog.pack();
				label.paintImmediately(label.getVisibleRect());
				cancel.paintImmediately(cancel.getVisibleRect());
		try {
			Thread.currentThread().sleep(100L);
		}catch(Exception ex) {
		}
				if( cancel.isSelected() ) return;
					if( start ) {
						zBuf = pi.render(pers, bnds,
							overlay.getGrid(),
							image,
							true);
						start = false;
						continue;
					}
			label.setText("rendering "+ (x2-x1) +" x "+ (y2-y1)
				+" grid, zoom factor = "+res);
				dialog.pack();
				label.paintImmediately(label.getVisibleRect());
				cancel.paintImmediately(cancel.getVisibleRect());
		try {
			Thread.currentThread().sleep(100L);
		}catch(Exception ex) {
		}
				if( cancel.isSelected() ) return;
					zBuf = pIm.render(pers, bnds,
						overlay.getGrid(),
						image,
						true);
	System.out.println( "\tprojected");
					BufferedImage im0 = pi.getImage();
					im = pIm.getImage();
					for( int x=0 ; x<w ; x++) {
						for( int y=0 ; y< h ; y++) {
							double val = zBuf.valueAt(x,y);
							if( Double.isNaN(val))continue;
							if(  val>zTest[res] )continue;
							int rgb0 = im0.getRGB(x,y);
							int rgb = im.getRGB(x,y);
							if(  val<.5*zTest[res]||rgb0==0xff000000 ) {
								im0.setRGB(x,y,rgb);
								continue;
							}
							double factor = (zTest[res]-val)*2./zTest[res];
							int[] c1 = new int[] {
								(rgb>>16)&255,
								(rgb>>8)&255,
								(rgb)&255 };
							int[] c0 = new int[] {
								(rgb0>>16)&255,
								(rgb0>>8)&255,
								(rgb0)&255 };
							for( int k=0 ; k<3 ; k++) {
								c1[k] = (int)Math.rint(
									c0[k]*(1-factor)+c1[k]*factor);
							}
							rgb = 0xff000000 | 
								(c1[0]<<16) | 
								(c1[1]<<8) | 
								c1[2];
							im0.setRGB(x,y,rgb);
						}
					}
				}
				grid.setGrid(saveGrid, saveLand, save1, save2, false);
				dialog.dispose();
			} else {
				pi.render(pers, bnds,
					grid.getGrid(), 
					grid.getImage(),
					mediumQ.isSelected() );
			}
		} catch(OutOfMemoryError e) {
			if( dialog != null ) dialog.dispose();
						JOptionPane.showMessageDialog( this,
								"Out of Memory\n  try resizing",
								"Out of memory",
								JOptionPane.ERROR_MESSAGE);
			return;
		}
		JPanel panel = new JPanel(new BorderLayout());
		JScrollPane sp = new JScrollPane(pi);
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				Zoomer zoom = new Zoomer(pi);
				pi.addMouseListener(zoom);
				pi.addMouseMotionListener(zoom);
				pi.addKeyListener(zoom);
				panel.add( sp, "Center");
				Dimension dim = new Dimension(w,h);
				if( w>1000 )dim.width=1000;
				if( h>800 ) dim.height=800;
				panel.setPreferredSize(dim);
		java.text.NumberFormat fmt = java.text.NumberFormat.getInstance();
		fmt.setMaximumFractionDigits(3);
		fmt.setGroupingUsed(false);
		JTextArea area = new JTextArea();
		area.setText( "View Point: "
				+(view.longitude<0. ? "W " : "E ")
				+fmt.format(Math.abs(view.longitude)) +", "
				+(view.latitude<0. ? "S " : "N ")
				+fmt.format(Math.abs(view.latitude)) +", ");
		fmt.setMaximumFractionDigits(0);
		area.append( fmt.format(view.elevation)+" m\n");
		fmt.setMaximumFractionDigits(3);
		area.append( "Center of image: "
				+(focus.longitude<0. ? "W " : "E ")
				+fmt.format(Math.abs(focus.longitude)) +", "
				+(focus.latitude<0. ? "S " : "N ")
				+fmt.format(Math.abs(focus.latitude)) +", ");
		fmt.setMaximumFractionDigits(0);
		
		area.append( fmt.format(focus.elevation/ve)
				+" m (scaled by a vertical exagerration of ");
		fmt.setMaximumFractionDigits(1);
		area.append(  fmt.format(ve) +")");

		JButton save = new JButton("Save Image");
		JPanel panel1 = new JPanel(new BorderLayout());
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				save();
			}
		});
		panel1.add(save, "West");
		panel1.add(area);

		panel.add( panel1,"North");
	//	pi.addLogo();
		JOptionPane.showMessageDialog(
			null, panel, "Perspective Image", JOptionPane.PLAIN_MESSAGE );
		pi = null;
	}
	GridRenderer renderer;
	JToggleButton bothB;
	private java.beans.PropertyChangeListener vePropListener;
	private javax.swing.event.MouseInputAdapter pImageMouseListener;
	private ActionListener cursorListener;
	private ActionListener renderListener;
	private JButton renderB;

	public void setRenderer( GridRenderer renderer, JToggleButton bothB) {
		this.renderer = renderer;
		this.bothB = bothB;
	}
	void save() {
		if( pi==null||pi.getImage()==null )return;
		JFileChooser chooser = haxby.map.MapApp.getFileChooser();
		String name = "3Dimage.jpg";
		File dir = chooser.getCurrentDirectory();
		chooser.setSelectedFile(new File(dir,name));
		File file = null;
		while( true ) {
			int ok = chooser.showSaveDialog(pi);
			if( ok==chooser.CANCEL_OPTION)return;
			file = chooser.getSelectedFile();
			if( file.exists() ) {
				ok=JOptionPane.showConfirmDialog(
						pi,
						"File exists, overwrite?");
				if( ok==JOptionPane.CANCEL_OPTION)return;
				if( ok==JOptionPane.YES_OPTION)break;
			} else {
				break;
			}
		}
		try {
			int sIndex = file.getName().lastIndexOf(".");
			String suffix = sIndex<0
				? "jpg"
				: file.getName().substring( sIndex+1 );
			if( !ImageIO.getImageWritersBySuffix(suffix).hasNext())suffix = "jpg";
			ImageIO.write( pi.getImage(), suffix, file);
		} catch(Exception ex) {
			
		}
	}
	public void dispose() {
		renderB.removeActionListener(renderListener);

		spinB.removeActionListener(cursorListener);
		inclineB.removeActionListener(cursorListener);
		scaleB.removeActionListener(cursorListener);
		moveB.removeActionListener(cursorListener);

		pImage.removeMouseListener(pImageMouseListener);
		pImage.removeMouseMotionListener(pImageMouseListener);

		veTool.removePropertyChangeListener(vePropListener);

		this.removeComponentListener(this);

		renderListener = null;
		cursorListener = null;
		pImageMouseListener = null;
		vePropListener = null;

		removeAll();
		pImage.dispose();
		pImage = null;
		grid = null;
		renderer = null;
	}
}

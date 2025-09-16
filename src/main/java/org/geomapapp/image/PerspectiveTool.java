package org.geomapapp.image;

import org.geomapapp.grid.*;
import org.geomapapp.util.*;
import org.geomapapp.geom.*;

import javax.swing.*;
import java.io.File;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;

public class PerspectiveTool extends JPanel 
			implements PerspectiveGeometry {
	GridImage grid;
	PerspectiveImage pImage;
	VETool veTool;
	GCPoint view, focus;
	public PerspectiveTool( GridImage grid ) {
		super( new BorderLayout() );
		veTool = new VETool(2.);
		veTool.addPropertyChangeListener(
			new java.beans.PropertyChangeListener() {
				public void propertyChange(
						java.beans.PropertyChangeEvent e) {
					try {
					double oldVE = ((Double)e.getOldValue()).doubleValue();
					double newVE = ((Double)e.getNewValue()).doubleValue();
					updateVE(oldVE, newVE);
					} catch(Exception ex) {
					}
				}
			});
		Grid2D g = grid.getGrid();
		pImage = new PerspectiveImage( g, this);
		pImage.setBorder( BorderFactory.createTitledBorder("Lo-Res Preview"));
		setGrid( grid);
		initPers();
		init();
	}
	void initPers() {
		javax.swing.event.MouseInputAdapter mouse = 
			new javax.swing.event.MouseInputAdapter() {
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
		pImage.addMouseListener(mouse);
		pImage.addMouseMotionListener(mouse);
	}
	void setGrid(GridImage grid) {
		this.grid = grid;
		Grid2D g = grid.getGrid();
		pImage.setGrid(g);
		double ve = veTool.getVE();
		pImage.setVE(ve);
		Rectangle bnds = g.getBounds();
		MapProjection proj = g.getProjection();
		Point2D f = proj.getRefXY( new Point(bnds.x+bnds.width/2,
					bnds.y+bnds.height/2));
		Point2D p = proj.getMapXY( f );
		double z0 = g.valueAt(p.getX(), p.getY());
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

		ActionListener cursor = new ActionListener() {
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
		spinB.addActionListener(cursor);
		inclineB.addActionListener(cursor);
		scaleB.addActionListener(cursor);
		moveB.addActionListener(cursor);
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
		JPanel quality = new JPanel( new GridLayout(1,0));
		quality.add( lowQ );
		quality.add( mediumQ );
		lowQ.setSelected(true);
		size.add( quality );

		size.add( new JLabel("width"));
		widthF = new JTextField("1000");
		size.add(widthF);
		size.add( new JLabel("height"));
		heightF = new JTextField("800");
		size.add(heightF);

		JButton render = new JButton("render");
		render.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				render();
			}
		});
		size.add(render);

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
		pi.setVE(ve);
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
			panel.add(new JLabel("Resizing is recommended"));
			ButtonGroup gp = new ButtonGroup();
			JCheckBox aspect = new JCheckBox("maintain aspect ratio");
			gp.add(aspect);
			aspect.setSelected( true );
			panel.add(aspect);
			JCheckBox wid = new JCheckBox("maintain width");
			gp.add(wid);
			panel.add(wid);
			JCheckBox ht = new JCheckBox("maintain height");
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
			} else if(ht.isSelected()){
				w = 2000000/h;
			}
			widthF.setText( Integer.toString(w) );
			heightF.setText( Integer.toString(h) );
		}
		Perspective3D pers = new Perspective3D(view, focus, 20., w);
		bnds = new Rectangle( -w/2, -h/2, w, h);
		try {
			pi.render(pers, bnds,
				grid.getGrid(), 
				grid.getImage(),
				mediumQ.isSelected() );
		} catch(OutOfMemoryError e) {
			JOptionPane.showMessageDialog( this,
				"Out of Memory\n  try resizing",
				"Out of memory",
				JOptionPane.ERROR_MESSAGE);
			return;
		}
		JPanel panel = new JPanel(new BorderLayout());
		JScrollPane sp = new JScrollPane(pi);
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
				+" m (scaled by a vertical exaggeration of ");
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

		panel.add( panel1,"South");
		pi.addLogo();
		JOptionPane.showMessageDialog(
			null, panel, "Perspective Image", JOptionPane.PLAIN_MESSAGE );
		pi = null;
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
			javax.imageio.ImageIO.write( pi.getImage(), "jpg", file);
		} catch(Exception ex) {
			
		}
	}
}

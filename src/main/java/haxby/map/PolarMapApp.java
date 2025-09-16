package haxby.map;

import haxby.db.*;
import haxby.db.ice.*;
import haxby.db.mgg.*;
import haxby.db.mcs.*;
import haxby.db.mb.*;
import haxby.db.pdb.*;
import haxby.map.*;
import haxby.proj.*;

import javax.swing.*;
import java.awt.print.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class PolarMapApp implements ActionListener, KeyListener {
	XMap map = null;
	Zoomer zoomer;
	PolarMapTools tools;
	JFrame frame;
	JLabel infoLabel;
	MapOverlay focus;
	MapOverlay baseMap;
	JSplitPane hPane;
	JSplitPane vPane;
	JPanel dialog;
	JLabel dbLabel;
	JButton closeDB;
	Database[] db=null;
	Database currentDB = null;
	JMenuItem[] dbMI;
	JMenuBar menuBar;
	JMenu dbMenu;
	JMenu fileMenu;
	public PolarMapApp( ) {
		JWindow startup = new JWindow();
		StartUp start = new StartUp();
		startup.getContentPane().add(start, "Center");
	//	startup.getContentPane().add(start.label, "North");
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		startup.pack();
		Dimension win = startup.getSize();
		startup.setLocation( (screen.width-win.width)/2, (screen.height-win.height)/2 );
		startup.show();
// initialize map
		PolarStereo proj = new PolarStereo(new Point2D.Double(300, 300),
				-90., 1, PolarProjection.NORTH);
		double scale = 12;
		double radius = proj.getRadius(89);
		scale = scale/radius;
		proj = new PolarStereo(new Point2D.Double(300, 300),
				-90., scale,PolarProjection.NORTH);
		map = new XMap( this, proj, 600, 600);
		PolarMapBorder border = new PolarMapBorder(map);
		map.setMapBorder(border);
// create "base map" overlay, base map image
		start.setText("Composing Basemap Image");
		baseMap = new MapOverlay( map );
	//	if( !PolarMapServer.getImage( new Rectangle(0,0,600,600), baseMap) ) {
	//		System.out.println("unable to create base map");
	//	}
		try {
			BufferedImage im = PolarMapServer.getTile(12, 0, 0);
			BufferedImage mask = new BufferedImage(600,600,BufferedImage.TYPE_INT_RGB);
			Graphics2D g = mask.createGraphics();
			g.drawImage(im, 0, 0, map);
			PolarStereo ps = (PolarStereo)IBCAO.getProjection();
			boolean[] land = IBCAO.getMask600();
			Color rgb1 = null;
			Color brown = new Color(160, 120, 80);
			for( int y=0 ; y<600 ; y++) {
				for(int x=0 ; x<600 ; x++) {
					Point2D p = proj.getRefXY( new Point(x, y) );
				//	Point2D p1 = ps.getMapXY( p);
				//	int i = (int) (Math.rint(p1.getX()) + 2323*Math.rint(p1.getY()));
					int i = x+y*600;
					if(land[i]) {
						rgb1 = brown;
					} else {
						float d18o = 2.0f + D18oObs.getValue( p.getX(), p.getY() );
						if( Float.isNaN(d18o) ) continue;
						rgb1 = IceCore.getColor( d18o );
						mask.setRGB( x, y, rgb1.getRGB() );
					}
				//	Color rgb = new Color(mask.getRGB(x, y));
				//	int red = (3*rgb.getRed() + rgb1.getRed() )/4;
				//	int green = (3*rgb.getGreen() + rgb1.getGreen() )/4;
				//	int blue = (3*rgb.getBlue() + rgb1.getBlue() )/4;
				//	rgb = new Color( red, green, blue);
				//	mask.setRGB( x, y, rgb.getRGB() );
				}
			}
			baseMap.setImage(mask, 0., 0., 1.);
		} catch (IOException ex) {
			System.out.println("unable to create base map");
		}
		map.addOverlay(baseMap);
		start.setText("Initiallizing GUI");
// create "focus" overlay with NULL image
		focus = new MapOverlay( map );
		map.addOverlay(focus);
// initialize zoomer
		zoomer = new Zoomer(map);
		map.addMouseListener(zoomer);
		map.addMouseMotionListener(zoomer);
		map.addKeyListener(zoomer);
		map.addKeyListener(this);

		tools = new PolarMapTools(this, map, focus);

		JScrollPane sp = new JScrollPane(map);

		frame = new JFrame("PolarMapApp");
		infoLabel = new JLabel("info");
	//	frame.getContentPane().add( infoLabel, "South" );
		frame.getContentPane().add(tools.getTools(), "North");
		frame.addWindowListener( new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				System.exit(0);
			}
		});

		menuBar = new JMenuBar();

		fileMenu = new JMenu("File");
		JMenuItem printMI = new JMenuItem("print");
		printMI.addActionListener(this);
		fileMenu.add(printMI);
		JMenuItem closeMI = new JMenuItem("exit");
		closeMI.addActionListener(this);
		fileMenu.add(closeMI);
		menuBar.add(fileMenu);

		initDB();
		dbMenu = new JMenu("Database");
		dbMI = new JMenuItem[db.length];
		for( int i=0 ; i<db.length ; i++) {
			dbMI[i] = new JMenuItem(db[i].getDBName(), Icons.UNSELECTED);
			dbMenu.add( dbMI[i] );
			dbMI[i].addActionListener(this);
		}
		menuBar.add(dbMenu);

		hPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT );
		hPane.setLeftComponent( sp );
		hPane.setOneTouchExpandable(true);

		JPanel panel = new JPanel( new GridLayout(0,1) );
		dbLabel = new JLabel("");
		dbLabel.setForeground(Color.black);
		panel.add(dbLabel);
		closeDB = new JButton("close");
		closeDB.addActionListener(this);
		panel.add(closeDB);
		dialog = new JPanel(new BorderLayout());
		dialog.add( panel, "North");

		vPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
		vPane.setTopComponent( hPane );
		vPane.setOneTouchExpandable(true);
		frame.getContentPane().add(vPane, "Center");

		frame.setJMenuBar( menuBar );
		frame.pack();
	//	frame.setSize( 1000, 800 );
		startup.dispose();
		frame.show();
	}
	void initDB() {
		db = new Database[1];
		db[0] = (Database) new IceDB(map);
	//	db[0] = (Database) new MGG(map, 2900);
	//	db[1] = (Database) new MBTracks(map, 4000);
	//	db[2] = (Database) new PDB(map);
	//	db[3] = (Database) new MCS(map);
	}
	public void keyPressed( KeyEvent evt ) {
	}
	public void keyTyped( KeyEvent evt ) {
	}
	public void keyReleased( KeyEvent evt ) {
		if( evt.getKeyCode() == KeyEvent.VK_F ) {
		//	mapFocus();
			tools.focus.doClick();
		} else if( evt.getKeyCode() == KeyEvent.VK_M ) {
		//	tools.maskB.doClick();
		}
	}
	public void setMask( boolean tf ) {
	//	baseMap.maskImage( tf );
	//	focus.maskImage( tf );
	//	map.repaint();
	}
	public void mapFocus() {
		map.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			// if(PolarMapServer.getImage( map.getClipRect2D(), focus, infoLabel)) map.repaint();
		map.setCursor(Cursor.getDefaultCursor());
	}
	public void actionPerformed(ActionEvent evt) {
		String name = evt.getActionCommand();
		if( evt.getSource() == closeDB ) {
			Runtime rt = Runtime.getRuntime();
			long free = rt.freeMemory()/1024/1024;
			long total = rt.totalMemory()/1024/1024;
			System.out.println("before:\t" + free +" MB Free,\t" + (total-free) +" MB used");
			if(currentDB == null ) return;
			map.removeOverlay( currentDB);
			currentDB.setEnabled(false);
			dialog.remove( currentDB.getSelectionDialog() );
			currentDB.disposeDB();
			for( int k=0 ; k<db.length ; k++) {
				if(currentDB==db[k]) {
					dbMI[k].setIcon(Icons.UNSELECTED);
					break;
				}
			}
			hPane.setRightComponent( null );
			vPane.setBottomComponent( null );
			System.gc();
			currentDB = null;
			free = rt.freeMemory()/1024/1024;
			total = rt.totalMemory()/1024/1024;
			System.out.println("after:\t" + free +" MB Free,\t" + (total-free) +" MB used");
			return;
		} else if( name.equals("print") ) {
			PrinterJob job = PrinterJob.getPrinterJob();
			PageFormat fmt = job.pageDialog(job.defaultPage());
			job.setPrintable(map, fmt);
			try {
				if(job.printDialog()) job.print();
			} catch (PrinterException pe) {
				pe.printStackTrace();
			}
			return;
		} else if( name.equals("exit") ) {
			System.exit(0);
			return;
		}
		for( int i=0 ; i<db.length ; i++) {
			if( name.equals( db[i].getDBName()) ) {
				if(db[i].loadDB()) {
					if( currentDB != null) {
						currentDB.setEnabled(false);
						dialog.remove( currentDB.getSelectionDialog() );
					}
					currentDB = db[i];
					currentDB.setEnabled(true);
					((JMenuItem)evt.getSource()).setIcon(Icons.SELECTED);
					dbLabel.setText( currentDB.getDBName() );
					if( currentDB.getSelectionDialog()!= null) {
						dialog.add( currentDB.getSelectionDialog(), "South");
					}
					hPane.setRightComponent( dialog );
					if( currentDB.getSelectionDialog() != null ) {
						int w = currentDB.getSelectionDialog().getPreferredSize().width;
						hPane.setDividerLocation( hPane.getSize().width -w 
							-hPane.getDividerSize() );
					}
					if( currentDB.getDataDisplay() != null ) {
						vPane.setBottomComponent( currentDB.getDataDisplay() );
						int h = currentDB.getDataDisplay().getPreferredSize().height;
						if(h>200) h=200;
						vPane.setDividerLocation( vPane.getSize().height - h 
								- vPane.getDividerSize() );
					}
					while( map.hasOverlay( db[i] ) ) {
						map.removeOverlay(db[i]);
					}
					map.addOverlay( db[i] );
				}
				return;
			}
		}
	}
	public static void main( String[] args) {
		new PolarMapApp();
	}
}
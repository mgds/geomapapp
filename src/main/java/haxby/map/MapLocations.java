package haxby.map;

import haxby.util.URLFactory;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.MouseInputAdapter;

public class MapLocations implements Overlay,
				ActionListener {
//	Vector locs;
	Hashtable locs;
	JMenu menu;
	XMap map;
	MapTools tools;
	JMenuItem addLoc;
	JMenuItem loadLoc;
	JMenuItem editLoc;
	JCheckBoxMenuItem showLoc;
	File locFile;
	JCheckBox list;
	JCheckBox save;
	JCheckBox newSave;
	JList locList;
	MouseInputAdapter mouse;
	public MapLocations(XMap map, MapTools tools ) {
		this.map = map;
		this.tools = tools;
		locs = new Hashtable();
		menu = new JMenu( "Places" );
		mouse = new MouseInputAdapter() {
			public void mouseMoved(MouseEvent evt) {
				if( evt.isControlDown() )return;;
				select( evt.getPoint());
			}
			public void mouseClicked(MouseEvent evt) {
				if( evt.isControlDown() )return;;
				zoomTo( evt.getPoint() );
			}
		};
		initMenu();
	}
	void initMenu() {
		addLoc = new JMenuItem("Add Place");
		menu.add( addLoc );
		addLoc.addActionListener( this );
		loadLoc = new JMenuItem("Load Places");
		menu.add( loadLoc );
		loadLoc.addActionListener( this );
		editLoc = new JMenuItem("Edit Places");
		menu.add( editLoc );
		editLoc.addActionListener( this );
		editLoc.setEnabled(false);
		showLoc = new JCheckBoxMenuItem("Show Places", false);
		menu.add( showLoc );
		showLoc.addActionListener( this );
	//	showLoc.setEnabled(false);
		menu.addSeparator();
		list = new JCheckBox("add to list", true);
		save = new JCheckBox("save to file", false);
		newSave = new JCheckBox("save to new file", false);
		addPresetLocations();
	//	addMyLocations();
	}
	public void addLocation( MapLocation loc ) {
		locs.put( loc.name, loc );
		JMenuItem item = new JMenuItem( loc.name );
	//	item.setActionCommand( Integer.toString( locs.size()-1 ) );
		menu.add( item );
		item.addActionListener( this );
	}
	public void addLocation() {
		double zoom = map.getZoom();
		Rectangle2D r = map.getClipRect2D();
		Point2D.Double p = new Point2D.Double(
				r.getX()+.5*r.getWidth(),
				r.getY()+.5*r.getHeight() );
		p = (Point2D.Double)map.getProjection().getRefXY(p);

		JTextField nameF = new JTextField("A Place");
		JPanel panel = new JPanel(new GridLayout(0,1,2,2));
		JLabel label = new JLabel("Enter Place Name");
		panel.add(label);
		panel.add(nameF);
		panel.add(list);
		panel.add(save);
		if( locFile!=null )panel.add(newSave);
		int ok = JOptionPane.showConfirmDialog( 
				map.getTopLevelAncestor(),
				panel,
				"add/save location",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if( ok==JOptionPane.CANCEL_OPTION ) return;
		MapLocation loc = new MapLocation( 
				nameF.getText(), 
				p.getX(),
				p.getY(),
				zoom);
		if( list.isSelected() ) addLocation( loc );
		if( save.isSelected() ) {
			if( locFile==null ) {
				JFileChooser chooser = MapApp.getFileChooser();
				chooser.setSelectedFile( new File(
					chooser.getCurrentDirectory(),
					"locations.txt"));
				ok = chooser.showSaveDialog(map.getTopLevelAncestor());
				if( ok==chooser.CANCEL_OPTION )return;
				locFile = chooser.getSelectedFile();
				save.setText("save to "+locFile.getName());
			}
			try {
				PrintStream out = new PrintStream(
					new FileOutputStream( 
					locFile, true ));
				out.println(nameF.getText()
					+"\t"+ p.getX()
					+"\t"+ p.getY()
					+"\t"+ zoom);
				out.close();
			} catch(IOException ex) {
				JOptionPane.showMessageDialog(
					map.getTopLevelAncestor(),
					"Save Failed:\n"+ex.getMessage());
				locFile = null;
			}
		}
	}
	void addPresetLocations() {
		try {
			ClassLoader cl = getClass().getClassLoader();
			String root = "org/geomapapp/resources/locations/";
			java.net.URL url = cl.getResource(root);
			loadLocations(url, "all.loc", menu);
		} catch(Exception ex) {
		}
	}
	void loadLocations(java.net.URL url, String file, JMenu menu)
			throws IOException {
		URL url1 = URLFactory.url(url, file);
		BufferedReader in = new BufferedReader(
			new InputStreamReader(url1.openStream()));
		StringTokenizer st;
		String line;
		while( (line=in.readLine()) != null) {
			st = new StringTokenizer(line, "\t");
			if( st.countTokens()==1 ) {
				int n = line.indexOf(".loc");
				String name = n>0 ?
					line.substring(0,n) : line;
				JMenu menu1 = new JMenu(name);
				menu.add(menu1);
				loadLocations(url, line, menu1);
				continue;
			}
			MapLocation loc = new MapLocation(
				st.nextToken(),
				Double.parseDouble(st.nextToken()),
				Double.parseDouble(st.nextToken()),
				Double.parseDouble(st.nextToken())
				);
			locs.put( loc.name, loc);
			JMenuItem item = new JMenuItem(loc.name);
			menu.add( item);
			item.addActionListener(this);
		}
	}
	void loadLocations() {
		JFileChooser chooser = MapApp.getFileChooser();
		if( locFile!=null ) chooser.setSelectedFile( locFile );
		int ok = chooser.showOpenDialog(map.getTopLevelAncestor());
		if( ok==chooser.CANCEL_OPTION )return;
		locFile = chooser.getSelectedFile();
		try {
			BufferedReader in = new BufferedReader(
				new FileReader( locFile ));
			StringTokenizer st;
			String line;
			Vector locs = new Vector();
			while( (line=in.readLine()) != null) {
				st = new StringTokenizer(line, "\t");
				locs.add( new MapLocation(
					st.nextToken(),
					Double.parseDouble(st.nextToken()),
					Double.parseDouble(st.nextToken()),
					Double.parseDouble(st.nextToken())
					));
			}
			locList = new JList(locs);
			locList.setSelectionInterval(0, locs.size());
			JPanel panel = new JPanel(new BorderLayout());
			Box box = Box.createVerticalBox();
			JButton b = new JButton("Select All");
			b.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					selectAll();
				}
			});
			box.add(b);
			b = new JButton("Select None");
			b.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					selectNone();
				}
			});
			box.add(b);
			panel.add(new JScrollPane(locList));
			panel.add(box, "East");
			ok = JOptionPane.showConfirmDialog( 
				map.getTopLevelAncestor(),
				panel,
				"select locations",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
			if( ok==JOptionPane.CANCEL_OPTION ) return;
			int[] indices = locList.getSelectedIndices();
			for( int k=0 ; k<indices.length ; k++) {
				addLocation((MapLocation)locs.get(indices[k]));
			}
			if( showLoc.isSelected()) draw(map.getGraphics2D() );
		} catch(Exception ex) {
			JOptionPane.showMessageDialog(
				map.getTopLevelAncestor(),
				"Error:\n"+ex.getMessage());
			locFile = null;
			ex.printStackTrace();
		}
	}
	void selectAll() {
		if(locList!=null) {
			locList.setSelectionInterval(
				0, locList.getModel().getSize()-1);
		}
	}
	void selectNone() {
		if(locList!=null) locList.clearSelection();
	}
	void editLocations() {
	}
	public JMenu getMenu() {
		return menu;
	}
	public void actionPerformed( ActionEvent evt ) {
		if( evt.getSource()==addLoc) {
			addLocation();
			return;
		} else if( evt.getSource()==loadLoc) {
			loadLocations();
			return;
		} else if( evt.getSource()==editLoc) {
			editLocations();
			return;
		} else if( evt.getSource()==showLoc) {
			map.removeMouseListener(mouse);
			map.removeMouseMotionListener(mouse);
			if(showLoc.isSelected() ) {
				map.addMouseListener(mouse);
				map.addMouseMotionListener(mouse);
				if(map.hasOverlay(this))return;
				map.addOverlay(this, false);
				map.repaint();
			} else {
				if(!map.hasOverlay(this))return;
				map.removeOverlay(this);
				map.repaint();
			}
			return;
		}
		goTo(evt.getActionCommand());
	}
	void goTo(String cmd) {
		MapLocation loc = (MapLocation)locs.get(cmd);
		Point2D.Double p = (Point2D.Double)(map.getProjection().getMapXY( 
				new Point2D.Double( loc.lon, loc.lat )));
		double z = map.getZoom();
		p.x *= z;
		p.y *= z;
		Insets insets = map.getInsets();
		p.x += insets.left;
		p.y += insets.top;
		double factor = loc.zoom/z;
		map.doZoom( p, factor );
		if( loc.zoom>=2. )tools.focus.doClick();
	}
	public void draw(Graphics2D g) {
		if( !showLoc.isSelected() )return;
		double zoom = map.getZoom();
		g.setStroke(new BasicStroke(1f/(float)zoom));
		Rectangle rect = map.getVisibleRect();
		Dimension r1 = map.getParent().getSize();
		Enumeration e = locs.elements();
		while( e.hasMoreElements()) {
			MapLocation loc = (MapLocation)e.nextElement();
			loc.draw(g, map);
		}
	}
	public void select( Point pt ) {
		Point2D p = map.getScaledPoint(pt);
		Enumeration e = locs.elements();
		MapLocation select = null;
		while( e.hasMoreElements()) {
			MapLocation loc = (MapLocation)e.nextElement();
			if( loc.select(map, p) ) {
				if( select==null ) {
					select = loc;
				} else {
					if( loc.zoom>select.zoom ) {
						select.unselect(map);
						select=loc;
					}
				}
			}
		}
	}
	public void zoomTo( Point pt ) {
		Point2D p = map.getScaledPoint(pt);
		Enumeration e = locs.elements();
		MapLocation select = null;
		while( e.hasMoreElements()) {
			MapLocation loc = (MapLocation)e.nextElement();
			if( loc.select(map, p) ) {
				if( select==null ) {
					select = loc;
				} else {
					if( loc.zoom>select.zoom ) {
						select.unselect(map);
						select=loc;
					}
				}
			}
		}
		if( select!=null ) goTo(select.name);
	}
}

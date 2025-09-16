package haxby.db.mcs;

import haxby.db.Database;
import haxby.map.XMap;
import haxby.util.URLFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Timestamp;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;

/**
 * The base class for marine MCS (Multi-Channel Seismic) data.
 * Holds a list of all MCS cruises Lamont-Doherty Geological 
 * Observatory's seismic database.
 * 
 */
public class MCS implements ActionListener, 
			MouseListener, 
			MouseMotionListener,
			Database {
	protected static MCSCruise[] cruises;
	protected static boolean initiallized = false;
	XMap map = null;
	JPanel panel = null;
	JComboBox cruiseList = null;
	JComboBox lineList = null;
	JMenu[] menus;
	MCSCruise currentCruise = null;
	MCSLine currentLine = null;
	MCSImage2 image = null;
	MCSImage2 imageAlt = null;
	String mapSelect = "selectArea";
	boolean mouseE = false;
	boolean enabled = false;
	JSplitPane imagePane;
	JRadioButton orientH, orientV;
	public MCS( XMap map ) {
		this.map = map;
		image = new MCSImage2();
		imageAlt = new MCSImage2();
		imagePane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT,
				image.panel, imageAlt.panel );
		imagePane.setDividerLocation(1.);
		imagePane.setOneTouchExpandable( true );
	}
	public void setEnabled( boolean tf ) {
		if( tf == enabled) return;
		enabled = tf;
		if(enabled) {
			map.addMouseListener( this);
		} else {
			map.removeMouseListener( this);
		}
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void disposeDB() {
	//	image.panel.remove(image);
		image = new MCSImage2();
		imageAlt = new MCSImage2();
		imagePane.setLeftComponent( image.panel );
		imagePane.setRightComponent( imageAlt.panel );
	}
	public boolean loadDB() {
		if( initiallized ) return true;
		try {
			initCruises(map, this);
		} catch(IOException ex) {
			return false;
		}
		return true;
	}
	public boolean isLoaded() {
		return initiallized;
	}
	public String getDBName() {
		return "Multi-Channel Reflection Profiles (Hosted)";
	}

	public String getCommand() {
		return "Multi-Channel Reflection Profiles (Hosted)";
	}

	public String getDescription() {
		return "Multi-channel Seismic Reflection Database - UNDER CONSTRUCTION";
	}
	public JComponent getDataDisplay() {
		return imagePane;
	}
	public JComponent getSelectionDialog() {
		if( !initiallized )return null;
		if(panel!=null) {
			return panel;
		}
		Font font = new Font("SansSerif", Font.PLAIN, 12);
		javax.swing.border.Border border = BorderFactory.createCompoundBorder(
				BorderFactory.createEtchedBorder(),
				BorderFactory.createEmptyBorder(1,3,1,3));
	//	panel = new JPanel(new BorderLayout());
		cruiseList = new JComboBox();
		cruiseList.addItem("Cruise");
		for(int i=0 ; i<cruises.length ; i++) cruiseList.addItem(cruises[i]);
		JLabel label;
		JButton btn;
	//	Box panel = Box.createHorizontalBox();
		Box box = Box.createVerticalBox();

		JPanel panel1 = new JPanel(new GridLayout(0,1));

	//	label = new JLabel("MCS");
	//	label.setHorizontalAlignment(label.CENTER);
	//	label.setBorder(border);
	//	label.setForeground(Color.black);
	//	label.setBackground(Color.lightGray);
	//	label.setOpaque(true);
	//	panel1.add(label);

		panel1.add( cruiseList );

		lineList = new JComboBox();
		lineList.addItem("Line");
		panel1.add(lineList);

		btn = new JButton("view-1");
		btn.addActionListener(this);
		panel1.add(btn);
		btn = new JButton("view-2");
		btn.addActionListener(this);
		panel1.add(btn);
		ButtonGroup gp1 = new ButtonGroup();
		orientV = new JRadioButton( "Vertical", false );
		orientV.addActionListener(this);
		gp1.add(orientV);
		panel1.add( orientV );
		orientH = new JRadioButton( "Horizontal", true );
		orientH.addActionListener(this);
		gp1.add(orientH);
		panel1.add( orientH );
		box.add(panel1);

		javax.swing.border.Border border1 = BorderFactory.createEmptyBorder(2,3,1,3);

		JPanel panel2 = new JPanel(new GridLayout(3,1));

		ButtonGroup grp = new ButtonGroup();
		label = new JLabel("map select");
		label.setFont(font);
		label.setForeground(Color.black);
		label.setBorder(border1);
		label.setOpaque(true);
		panel2.add(label);
		JRadioButton button = new JRadioButton("area");
		button.setFont(font);
		button.setSelected(true);
		button.setBorder(border1);
		button.setActionCommand("selectArea");
		button.addActionListener(this);
		grp.add(button);
		panel2.add(button);
		button = new JRadioButton("line");
		button.setFont(font);
		button.setBorder(border1);
		button.setActionCommand("selectLine");
		button.addActionListener(this);
		grp.add(button);
		panel2.add(button);
	//	button = new JRadioButton("pt");
	//	button.setFont(font);
	//	button.setBorder(border1);
	//	button.setActionCommand("selectPt");
	//	button.addActionListener(this);
	//	grp.add(button);
	//	panel2.add(button);
	//	button = new JRadioButton("none");
	//	button.setFont(font);
	//	button.setBorder(border1);
	//	button.setActionCommand("selectNone");
	//	button.addActionListener(this);
	//	grp.add(button);
	//	panel2.add(button);

		panel2.setBorder(BorderFactory.createLineBorder(Color.black));
		box.add(panel2);

	//	JPanel panel3 = new JPanel(new GridLayout(2,1));
	//	btn = new JButton("close");
	//	// btn.setBorder(border);
	//	panel3.add(btn);
	//	btn = new JButton("quit");
	//	// btn.setBorder(border);
	//	btn.addActionListener(this);
	//	panel3.add(btn);

	//	box.add(panel3);

		cruiseList.addActionListener(this);
		lineList.addActionListener(this);

		menus = new JMenu[2];
		menus[0] = new JMenu("MCSFile");
		menus[0].setMnemonic(KeyEvent.VK_F);
		JMenuItem item = new JMenuItem("close");
		item.setMnemonic(KeyEvent.VK_C);
		menus[0].add(item);
		menus[1] = new JMenu("settings");
		menus[1].setMnemonic(KeyEvent.VK_S);

		item = menus[1].add("Map Overlay");
		item = menus[1].add("MCS Image");

		panel = new JPanel();
		panel.add(box);
		return panel;
	}
	public void draw(Graphics2D g) {
		if(map==null || cruises.length==0)return;
		int k, k0;
		if(currentCruise==null) {
			k=0;
			k0 = cruises.length-1;
		} else {
			k = cruiseList.getSelectedIndex();
			k0 = k-1;
			k = k%cruises.length;
		}
		g.setStroke(new BasicStroke(2f/(float)map.getZoom()));
		g.setColor(Color.black);
	//	while(k!=k0) {
		for(k=0 ; k<cruises.length ; k++) {
			cruises[k].draw(g);
		}
		if(currentCruise!=null) {
			g.setColor(Color.white);
			currentCruise.draw(g);
			g.setColor(Color.black);
			currentCruise.drawLines(g);
			if(currentLine!=null) {
				if( currentLine.getCruise() != currentCruise ) {
					currentLine=null;
				} else if( currentLine==image.line ){
					g.setColor(Color.yellow);
					image.line.draw(g);
				} else if( currentLine==imageAlt.line) {
					g.setColor(Color.yellow);
					imageAlt.line.draw(g);
				} else {
					g.setColor(Color.white);
					currentLine.draw(g);
				}
			}
		}
		if( image.line != null) {
			int[] cdp = image.getVisibleSeg();
			image.line.drawSeg( cdp[0], cdp[1], g);
		}
		if( imageAlt.line != null) {
			int[] cdp = imageAlt.getVisibleSeg();
			imageAlt.line.drawSeg( cdp[0], cdp[1], g);
		}
	}
	void setSelectedCruise(MCSCruise cruise) {
		if( map==null || !map.hasOverlay(this) )return;
		if(cruise == currentCruise)return;
		currentLine = null;
	//	if( currentCruise!=null) {
	//		Rectangle rect = map.getScaledRect(currentCruise.getBounds());
	//		currentCruise = null;
	//		map.paintImmediately(rect);
	//	}
		lineList.removeAllItems();
		lineList.addItem("Line");
		currentCruise = cruise;
		map.repaint();
		if(cruise==null)return;
	//	synchronized (map.getTreeLock()) {
	//		Graphics2D g = map.getGraphics2D();
	//		g.setColor(Color.white);
	//		g.setStroke(new BasicStroke(2f/(float)map.getZoom()));
	//		currentCruise.draw(g);
	//		g.setColor(Color.black);
	//	//	g.setStroke(new BasicStroke(1f/(float)map.getZoom()));
	//		currentCruise.drawLines(g);
	//	}
		MCSLine[] lines = currentCruise.getLines();
		for( int i=0 ; i<lines.length ; i++) lineList.addItem(lines[i]);
	}
	void setSelectedLine(MCSLine line) {
		if( map==null || !map.hasOverlay(this) )return;
		if(line == currentLine) return;
		MCSCruise cruise=null;
		try {
			cruise = (MCSCruise) cruiseList.getSelectedItem();
		} catch ( ClassCastException e ) {
			return;
		}
		synchronized (map.getTreeLock()) {
			Graphics2D g = map.getGraphics2D();
		//	g.setStroke(new BasicStroke(2f/(float)map.getZoom()));
		//	if( currentLine!=null) {
		//		g.setColor(Color.black);
		//		currentLine.draw(g);
		//	}
			currentLine = line;
			draw(g);
		//	if(line == null) return;
		//	g.setColor(Color.white);
		//	currentLine.draw(g);
		}
	}
	public void actionPerformed( ActionEvent e ) {
		String cmd = e.getActionCommand();
		if( e.getSource() == cruiseList ) {
			try {
				String tmpCruise = (currentCruise==null) ? "null" : currentCruise.getID();
				setSelectedCruise((MCSCruise) cruiseList.getSelectedItem());
			//	if(currentCruise != null && !mouseE
			//			&& !currentCruise.getID().equals(tmpCruise)) {
			//		int zoom = (int) map.getZoom();
			//		Rectangle area = map.getScaledRect(currentCruise.getBounds());
			//		int border = (int)(5*zoom);
			//		area.x -= border;
			//		area.y -= border;
			//		area.width += border*2;
			//		area.height += border*2;
			//		map.newRectangle(area);
			//	}
			} catch ( ClassCastException ex ) {
				setSelectedCruise( null );
			} finally {
				mouseE = false;
			}
		} else if( e.getSource() == lineList ) {
			try {
				setSelectedLine((MCSLine) lineList.getSelectedItem());
			} catch ( ClassCastException ex ) {
				setSelectedLine( null );
			}
		} else if( cmd.equals("view-1") ) {
			if( currentLine==null )return;
			try {
				image.loadImage(currentLine);
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(map.getTopLevelAncestor(), "unable to load image\n"
					+ex.getMessage());
				ex.printStackTrace();
			}
			synchronized( map.getTreeLock() ) {
				Graphics2D g = map.getGraphics2D();
				draw( g );
			}
		} else if( cmd.equals("view-2") ) {
			if( currentLine==null )return;
			try {
				imageAlt.loadImage(currentLine);
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(map.getTopLevelAncestor(), "unable to load image\n"
					+ex.getMessage());
				ex.printStackTrace();
			}
			synchronized( map.getTreeLock() ) {
				Graphics2D g = map.getGraphics2D();
				draw( g );
			}
		} else if( e.getSource() == orientV || e.getSource() == orientH ) {
			Dimension dim = imagePane.getSize();
			if( orientV.isSelected() ) {
				imagePane.setOrientation(imagePane.VERTICAL_SPLIT);
				imagePane.setDividerLocation( dim.height/2 );
			} else {
				imagePane.setOrientation(imagePane.HORIZONTAL_SPLIT);
				imagePane.setDividerLocation( dim.width/2 );
			}
		} else if( cmd.startsWith("select") ) {
			mapSelect = cmd;
		} else if( cmd.equals("close") ) {
		} else if( cmd.equals("quit")) {
		}
	}
	public void mouseClicked(MouseEvent e) {
		if(e.isControlDown())return;
		if(e.getSource() != map) return;
		double scale = map.getZoom();
		Insets insets = map.getMapBorder().getBorderInsets(map);
		Point2D.Double p = (Point2D.Double) map.getScaledPoint( e.getPoint() );
		if(mapSelect.trim().equals("selectArea")) {
			int k = cruiseList.getSelectedIndex();
			k=k%cruises.length;
			int k0 = k;
			double wrap = map.getWrap();
			while(true) {
				if(cruises[k].contains(p.x, p.y, wrap) ) {
					mouseE = true;
					cruiseList.setSelectedItem(cruises[k]);
					return;
				}
				k = (k+1)%cruises.length;
				if(k==k0)break;
			}
			cruiseList.setSelectedIndex(0);
		} else if(mapSelect.equals("selectLine")) {
			if(currentCruise==null)return;
			MCSLine[] lines = currentCruise.getLines();
			if(lines==null || lines.length==0) return;
			int k = -1;
			if( currentLine!=null) for( k=0 ; k<lines.length ; k++) {
				if( lines[k]==currentLine ) break;
			}
			k=(k+1)%lines.length;
			int k0 = k;
			double minDist = 1/scale;
			k0 = -1;
			for( int kk=0 ; kk<lines.length ; kk++) {
				double dist = lines[k].distanceSq(p.x, p.y);
				if(dist < minDist) {
					minDist = dist;
					k0 = k;
					break;
				}
				k = (k+1)%lines.length;
			} 
			if(k0!=-1) lineList.setSelectedItem(lines[k0]);
			else lineList.setSelectedIndex(0);
		} else if(mapSelect.equals("selectPt")) {
		//	if(image==null || image.getLine()==null ) return;
		//	MCSLine line = image.getLine();
		}
	}
	public void mousePressed(MouseEvent e) {
	}
	public void mouseReleased(MouseEvent e) {
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
	}
	public void mouseMoved(MouseEvent e) {
	}
	public void mouseDragged(MouseEvent e) {
	}
	/**
	 * Initiallizes all the <code>MCSCruise</code> objects in the ldeo database.
	 */
	protected static void initCruises(XMap map, MCS mcs) throws IOException {
		URL url = URLFactory.url(haxby.map.MapApp.TEMP_BASE_URL + "cgi-bin/MCS/mcs_init");
		BufferedReader in = new BufferedReader(
				new InputStreamReader( url.openStream() ));
		String s;
		StringTokenizer st;
		Vector tmp = new Vector();
		while( (s=in.readLine()) != null ) {
			if(s.equals("@")) break;
			st = new StringTokenizer(s);
			if(st.countTokens() != 6) continue;
			String id = st.nextToken();
			Timestamp ts = new Timestamp(Long.parseLong(st.nextToken()));
			double[] wesn = new double[] { Double.parseDouble(st.nextToken()),
						Double.parseDouble(st.nextToken()),
						Double.parseDouble(st.nextToken()),
						Double.parseDouble(st.nextToken()) };
			MCSCruise cruise = new MCSCruise(mcs, map, id, ts, wesn);
			tmp.add(cruise);
			Vector vec = new Vector();
			String lineID = "";
			while( !(s=in.readLine()).equals("@") ) {
				st = new StringTokenizer(s);
				id = st.nextToken();
				if( !lineID.equals(id) ) {
					if( vec.size() > 1 ) {
						CDP[] cdps = new CDP[vec.size()];
						for( int i=0 ; i<cdps.length ; i++)cdps[i]=(CDP)vec.get(i);
						MCSLine line = new MCSLine(map, cruise, lineID,cdps);
						cruise.addLine(line);
					}
					lineID = id;
					vec.removeAllElements();
				}
				long time = Long.parseLong(st.nextToken());
				double lon = Double.parseDouble(st.nextToken());
				double lat = Double.parseDouble(st.nextToken());
				int cdp = Integer.parseInt(st.nextToken());
				boolean connect = (vec.size()==0) ? false : true;
				vec.add(new CDP(cdp, lon, lat, time, connect));
			}
			if( vec.size() > 1 ) {
				CDP[] cdps = new CDP[vec.size()];
				for( int i=0 ; i<cdps.length ; i++)cdps[i]=(CDP)vec.get(i);
				MCSLine line = new MCSLine(map, cruise, lineID,cdps);
				cruise.addLine(line);
			}
		}
		in.close();
		cruises = new MCSCruise[tmp.size()];
		for( int i=0 ; i<cruises.length ; i++) {
			cruises[i] = (MCSCruise) tmp.get(i);
			MCSLine[] lines = cruises[i].getLines();
			for( int k=0 ; k<lines.length-1 ; k++) {
				for( int j=k+1 ; j<lines.length ; j++) {
					double[] crs = MCSLine.cross(lines[k], lines[j]);
					if(crs==null) continue;
					lines[k].addCrossing( crs[0], crs[1], lines[j] );
					lines[j].addCrossing( crs[1], crs[0], lines[k] );
				}
			}
		}
		initiallized = true;
	}
	
	@Override
	public void unloadDB() {
		initiallized = false;
	}
}

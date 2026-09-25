package org.geomapapp.db.dsdp;

import haxby.map.MapApp;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.JTextField;

public class CoreDescriptionsDisplay extends JComponent implements WindowListener, MouseListener {
	DSDPHole hole;
	double zScale;
	MouseAdapter mouse;
	JTextField text;
	int prevAge = -1;
	Color backgroundC = Color.white;
	
	static String DSDP_VCD_LIST_PATH = PathUtil.getPath("DSDP/DSDP_VCD_LIST_PATH",
			MapApp.BASE_URL+"/data/portals/dsdp/ODP_VCD_LOCAL/");
	static String DSDP_VCD_DATA_PATH = PathUtil.getPath("DSDP/DSDP_VCD_DATA_PATH",
			MapApp.BASE_URL+"/data/portals/dsdp/odp_vcd_data/");
	
	String coreDescriptionURLString = DSDP_VCD_LIST_PATH;
	String coreDescriptionURLData = DSDP_VCD_DATA_PATH;
	
	Hashtable coreDescriptionsAtDepths;
	double bottomDepth = 0.0;
	static final int CORE_DESCRIPTIONS_DISPLAY_WIDTH = 20;
	private static final String errMsg = "Error attempting to launch web browser";
	DSDPCore[] cores;
	
	boolean exists = true;
	
	public CoreDescriptionsDisplay(DSDPHole hole, JTextField text) {
			setToolTipText("Click for Core Descriptions");
			zScale = 2.;
			addMouseListener(this);
			this.hole = hole;
			this.text = text;
			mouse = new MouseAdapter() {
				public void mouseEntered(MouseEvent e) {
					e.getComponent().requestFocus();
				}
			};
			cores = hole.cores;
			readCoreDescriptionList();
	}
	
	public void readCoreDescriptionList() {
		int i = 0;
		coreDescriptionsAtDepths = new Hashtable();
		try {
			URL testURL = URLFactory.url(coreDescriptionURLString + hole.toString() + "-VCD.txt");
			BufferedReader in = new BufferedReader( new InputStreamReader( ( URLFactory.url( coreDescriptionURLString + hole.toString() + "-VCD.txt" ) ).openStream() ) );
			String s;
			s = in.readLine();
			while ( ( s = in.readLine() ) != null ) {
				String[] sArr = s.split("\t");
				if ( sArr.length > 1 ) {
					String checkDepthString = sArr[0];
					checkDepthString = checkDepthString.replaceAll( "\\.", "" );
					if ( checkDepthString != null && !checkDepthString.equals("") && checkDepthString.matches("\\d+") && sArr[1] != null && !sArr[1].equals("") ) {
//						double temp = Double.parseDouble(sArr[0]);
//						if ( cores[i].recovered > 0 ) {
//						temp += ( cores[i].recovered * 0.5 );
//						coreDescriptionsAtDepths.put( Double.toString(temp), sArr[1] );
//						bottomDepth = temp;

//						}
//						else {
						sArr[1] = coreDescriptionURLData + sArr[1];
						coreDescriptionsAtDepths.put( sArr[0], sArr[1] );
						bottomDepth = Double.parseDouble(sArr[0]);
//						}
						i++;
					}
				}
			}
			in.close();
			exists = true;
		} catch (MalformedURLException mue) {
			mue.printStackTrace();
		} catch (IOException ioe) {
			exists = false;
//			ioe.printStackTrace();
		}
	}
	
	public void setBGC( Color bgColor ) {
		backgroundC = bgColor;
	}
	
	public void setHole(DSDPHole hole) {
		this.hole = hole;
		zScale = 2.;
		cores = hole.cores;
		readCoreDescriptionList();
	}
	public void setZScale( double zScale ) {
		this.zScale = zScale;
	}
	public double getZScale() {
		return zScale;
	}
	public Dimension getPreferredSize() {
		int h = (int)Math.ceil( bottomDepth * zScale ) + 10;
		return new Dimension( CORE_DESCRIPTIONS_DISPLAY_WIDTH, h );
	}
	public void addNotify() {
		removeMouseListener( mouse );
		addMouseListener( mouse );
		super.addNotify();
	}
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D)graphics;
		if ( exists ) {
			Dimension d = getPreferredSize();
			g.setColor(Color.black);
			g.drawRect( 0, 0, d.width - 1, d.height - 1 );
			g.setColor(backgroundC);
			g.fillRect( 1, 1, d.width - 2, d.height - 2 );
			g.setFont(new Font("SansSerif", Font.PLAIN, 9));
			Enumeration coreDescriptionDepthList = coreDescriptionsAtDepths.keys();

			g.setColor(Color.black);
			int i = 0;
			while ( coreDescriptionDepthList.hasMoreElements() ) {
				double depth = Double.parseDouble((String)coreDescriptionDepthList.nextElement());
				int y = (int)( ( depth ) * zScale  );
//				if ( cores[i].recovered > 0 ) {
//				y += (int)( ( cores[i].recovered * 0.5 ) * zScale  );
//				}
				g.fillOval( 7, y, 6, 6);
				i++;
			}
		}
		else {
			g.drawRect(0, 0, 0, 0);
		}
		prevAge = -1;
	}
	public void drawLineAtAge( int currentAge )	{
		if ( exists ) {
			synchronized (getTreeLock()) {
				Graphics2D g = (Graphics2D)getGraphics();
				Rectangle r = getVisibleRect();
				int x1 = r.x;
				int x2 = r.x+r.width;
				g.setXORMode( Color.cyan );
				if ( prevAge != -1)	{
					g.drawLine(x1, prevAge, x2, prevAge);
				}
				g.drawLine(x1, currentAge, x2, currentAge);
				prevAge = currentAge;
			}
		}
	}
	
	public void accessURL( String inputURLString ) {
		BrowseURL.browseURL(inputURLString);
	}
	
	public static void main( String[] args) {
/*		
		CoreDescriptionsDisplay pd = new CoreDescriptionsDisplay();
		pd.readCoreDescriptionList();
		JFrame testPhotoDisplayFrame = new JFrame("Test PhotoDisplay");
		testPhotoDisplayFrame.addWindowListener(pd);
		JScrollPane testPhotoDisplaySP = new JScrollPane(pd);
		testPhotoDisplayFrame.getContentPane().add( testPhotoDisplaySP, "Center" );
		testPhotoDisplayFrame.pack();
		testPhotoDisplayFrame.setSize( new Dimension( 300, 600 ) );
		testPhotoDisplayFrame.setVisible(true);
*/
	}

	public void windowActivated(WindowEvent arg0) {
	}

	public void windowClosed(WindowEvent arg0) {
	}

	public void windowClosing(WindowEvent arg0) {
//		System.exit(0);
	}

	public void windowDeactivated(WindowEvent arg0) {
	}

	public void windowDeiconified(WindowEvent arg0) {	
	}

	public void windowIconified(WindowEvent arg0) {		
	}

	public void windowOpened(WindowEvent arg0) {	
	}

	public void mouseClicked(MouseEvent me) {
		if ( !me.isControlDown() ) {
			String depthString = "";
			double clickDepth = (double)( me.getY() / zScale );
			System.out.println(clickDepth);
			Enumeration coreDescriptionDepthList = coreDescriptionsAtDepths.keys();
			double minDifference = Double.NaN;
			String accessDepth = "";
			if ( coreDescriptionDepthList.hasMoreElements() ) {
				depthString = (String)coreDescriptionDepthList.nextElement();
				double depth = Double.parseDouble(depthString);
				double difference =  Math.abs(clickDepth - depth);
				minDifference = difference;
				accessDepth = depthString;
			}
			while ( coreDescriptionDepthList.hasMoreElements() ) {
				depthString = (String)coreDescriptionDepthList.nextElement();
				double depth = Double.parseDouble(depthString);
				double difference = Math.abs(clickDepth - depth);
				if ( difference < minDifference ) {
					minDifference = difference;
					accessDepth = depthString;
				}
			}
			if ( minDifference < 6 ) {
				String accessURLString = (String)coreDescriptionsAtDepths.get(accessDepth);
				if ( accessURLString != null ) {
					accessURL(accessURLString);
				}
			}
		}
		/*
		if ( photoDepthList.hasMoreElements() ) {
			double depth = Double.parseDouble((String)photoDepthList.nextElement());
			System.out.println("HELLO" + depth);
			double difference = clickDepth - depth;
			minDifference = difference;
			accessDepth = depth;
			if ( difference < 0 ) {
				System.out.println(accessDepth);
				return;
			}
		}
		while ( photoDepthList.hasMoreElements() ) {
			double depth = Double.parseDouble((String)photoDepthList.nextElement());
			double difference = clickDepth - depth;
			if ( difference < 0 ) {
				if ( Math.abs(difference) < minDifference ) {
					accessDepth = depth;
					System.out.println(accessDepth);
					return;
				}
				else {
					System.out.println(accessDepth);
					return;
				}
			}
			else {
				if ( difference < minDifference ) {
					minDifference = difference;
					accessDepth = depth;
				}
			}
		}
		System.out.println(accessDepth);
		*/
	}

	public void mouseEntered(MouseEvent arg0) {
	}

	public void mouseExited(MouseEvent arg0) {
	}

	public void mousePressed(MouseEvent arg0) {
	}

	public void mouseReleased(MouseEvent arg0) {	
	}
}

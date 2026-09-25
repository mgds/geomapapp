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
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.JTextField;

public class PhotoDisplay extends JComponent implements WindowListener, MouseListener {
	DSDPHole hole;
	double zScale;
	MouseAdapter mouse;
	JTextField text;
	int prevAge = -1;
	Color backgroundC = Color.white;
	
	String photoURLString = PathUtil.getPath("DSDP/DSDP_PHOTOS_LIST_PATH",
			MapApp.BASE_URL+"/data/portals/dsdp/PHOTOS_LOCAL/");
	
	String photoDataURLString = PathUtil.getPath("DSDP/DSDP_PHOTOS_DATA_PATH",
			MapApp.BASE_URL+"/data/portals/dsdp/dsdp_photos_data/");
	
		
	Hashtable photosAtDepths;
	double bottomDepth = 0.0;
	static final int PHOTODISPLAY_WIDTH = 20;
	private static final String errMsg = "Error attempting to launch web browser";
	DSDPCore[] cores;
	
	public PhotoDisplay(DSDPHole hole, JTextField text) {
		setToolTipText("Click for Photos");
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
		readPhotoList();
	}

	public void readPhotoList() {
		int i = 0;
		photosAtDepths = new Hashtable();
		String photoURL = photoURLString + hole.toString() + ".list";
		try {
			//System.out.println(photoURL);
			BufferedReader in = new BufferedReader( new InputStreamReader( ( URLFactory.url( photoURL ) ).openStream() ) );
			System.out.println("LOAD PHOTO: " + hole.toString() + ".list");
			String s;
			while ( ( s = in.readLine() ) != null && i < cores.length) {
				String[] sArr = s.split("\t");
				if ( sArr.length > 2 ) {
					String checkDepthString = sArr[1];
					checkDepthString = checkDepthString.replaceAll( "\\.", "" );
					if ( checkDepthString != null && !checkDepthString.equals("") && checkDepthString.matches("\\d+") && sArr[2] != null && !sArr[2].equals("") ) {
						double temp = Double.parseDouble(sArr[1]);
						if ( cores[i] != null &&
								cores[i].recovered > 0 ) {
							temp += ( cores[i].recovered * 0.5 );
							sArr[2] = photoDataURLString + sArr[2];
							photosAtDepths.put( Double.toString(temp), sArr[2] );
							bottomDepth = temp;
//							System.out.println("checkDepthString: " + checkDepthString + "\tcores[i].recovered: " + cores[i].recovered + "\ttemp: " + temp);
						}
						else {
							sArr[2] = photoDataURLString + sArr[2];
							photosAtDepths.put( sArr[1], sArr[2] );
							bottomDepth = Double.parseDouble(sArr[1]);
						}
						i++;
					}
				}
			}
			in.close();
		} catch (MalformedURLException mue) {
			System.out.println("NO PHOTOS FOUND: " + photoURL);
			//mue.printStackTrace();
		} catch (IOException ioe) {
			System.out.println("NO PHOTOS FOUND: " + photoURL);
			//ioe.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException ex) {
			System.out.println("NO PHOTOS FOUND: " + photoURL);
			//ex.printStackTrace();
		}
	}
	
	public void setBGC( Color bgColor ) {
		backgroundC = bgColor;
	}
	
	public void setHole(DSDPHole hole) {
		this.hole = hole;
	}
	public void setZScale( double zScale ) {
		this.zScale = zScale;
	}
	public double getZScale() {
		return zScale;
	}
	public Dimension getPreferredSize() {
		int h = (int)Math.ceil( bottomDepth * zScale ) + 10;
		return new Dimension( PHOTODISPLAY_WIDTH, h );
	}
	public void addNotify() {
		removeMouseListener( mouse );
		addMouseListener( mouse );
		super.addNotify();
	}
	public void paint(Graphics graphics) {
		Graphics2D g = (Graphics2D)graphics;
		Dimension d = getPreferredSize();
		g.setColor(Color.black);
		g.drawRect( 0, 0, d.width - 1, d.height - 1 );
		g.setColor(backgroundC);
		g.fillRect( 1, 1, d.width - 2, d.height - 2 );
		g.setFont(new Font("SansSerif", Font.PLAIN, 9));
		Enumeration photoDepthList = photosAtDepths.keys();
		
		g.setColor(Color.black);
		int i = 0;
		while ( photoDepthList.hasMoreElements() ) {
			double depth = Double.parseDouble((String)photoDepthList.nextElement());
			int y = (int)( ( depth ) * zScale  );
//			if ( cores[i].recovered > 0 ) {
//				y += (int)( ( cores[i].recovered * 0.5 ) * zScale  );
//			}
			g.fillOval( 7, y, 6, 6);
			i++;
		}
		prevAge = -1;
	}
	public void drawLineAtAge( int currentAge )	{
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
	
	public static void main( String[] args) {
/*		
		PhotoDisplay pd = new PhotoDisplay();
		pd.readPhotoList();
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
		String depthString = "";
		double clickDepth = (double)( me.getY() / zScale );
		System.out.println(clickDepth);
		Enumeration photoDepthList = photosAtDepths.keys();
		double minDifference = Double.NaN;
		String accessDepth = "";
		if ( photoDepthList.hasMoreElements() ) {
			depthString = (String)photoDepthList.nextElement();
			double depth = Double.parseDouble(depthString);
			double difference =  Math.abs(clickDepth - depth);
			minDifference = difference;
			accessDepth = depthString;
		}
		while ( photoDepthList.hasMoreElements() ) {
			depthString = (String)photoDepthList.nextElement();
			double depth = Double.parseDouble(depthString);
			double difference = Math.abs(clickDepth - depth);
			if ( difference < minDifference ) {
				minDifference = difference;
				accessDepth = depthString;
			}
		}
		if ( minDifference < 6 ) {
			String accessURLString = (String)photosAtDepths.get(accessDepth);
			if ( accessURLString != null ) {
				BrowseURL.browseURL(accessURLString);
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

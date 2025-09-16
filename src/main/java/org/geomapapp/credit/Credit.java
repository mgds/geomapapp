package org.geomapapp.credit;

import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.geomapapp.grid.Grid2D;
import org.geomapapp.util.Icons;

public class Credit implements haxby.map.Overlay {
	private static final int EXIT_ON_CLOSE = 0;
	Vector<GMAObject> cruises;
	Vector<GMAObject> grids;
	Vector<GMAObject> currentGrids;
	Vector<GMAObject> currentCruises;
	JLabel[] flagIcons;
	boolean[] flags;
	boolean[] currentFlags;
	boolean[] lastFlags;
	Rectangle bounds;
	int minY, maxY;
	XMap map;
	JPanel panel;
	JPanel flagPanel;
	JCheckBoxMenuItem menuItem;
	int mapType;
	int dx, dy;

	static String CREDIT_PATH = PathUtil.getPath("GMRT_LATEST/CREDIT");

	/**
	 * Class constructor specifying the map object to create.
	 * 	
	 * @param map
	 * @param mapType
	 * @throws IOException If an input or output exception occurred
	 */
	public Credit(XMap map, int mapType) throws IOException {
		this.mapType = mapType;
		this.map = map;
		if (mapType == MapApp.MERCATOR_MAP) {
			dx = 0; dy = -260;
		} else {
			dx = dy = -320;
		}

		initPanel();
		cruises = new Vector<GMAObject>();
		grids = new Vector<GMAObject>();
		menuItem = new JCheckBoxMenuItem("Data Sources");
		menuItem.setState(true);
		menuItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				setEnabled(menuItem.isSelected());
			}
		});

		String base = CREDIT_PATH + "/tile_map_mb_";
		if (mapType == MapApp.MERCATOR_MAP)
			base += "merc";
		else if (mapType == MapApp.SOUTH_POLAR_MAP)
			base += "SP";
		else if (mapType == MapApp.NORTH_POLAR_MAP)
			base += "NP";
		base += ".gz";

		URL url = URLFactory.url( base );
		DataInputStream in = new DataInputStream(
			new GZIPInputStream( url.openStream() ));
		String name = "";
		minY = maxY = 0;
		flags = new boolean[0];
		while( true ) {	
			try {
				name = in.readUTF();
			} catch(EOFException e) {
				break;
			}
			String altURL = in.readUTF();
			if( altURL.equals("0") ) altURL=null;
			int nation = in.readInt();
			if( nation>=flags.length ) {
				boolean[] f = new boolean[nation+1];
				for( int i=0 ; i<flags.length ; i++)f[i]=flags[i];
				
				flags = f;
			}
			flags[nation]=true;
			Rectangle rect = new Rectangle(
				in.readShort(),
				in.readShort(),
				in.readShort(),
				in.readShort() );
			int size = rect.width*rect.height;
			byte[] buffer = new byte[(size+7)>>3];
			int len = 0;
			while( true ) {
				len += in.read( buffer, len, buffer.length-len );
				if( len==buffer.length ) break;
			}
			Grid2D.Boolean mask = new Grid2D.Boolean(
				rect, null);
			mask.setBuffer( buffer );
			//System.out.println(name + '\t' + nation);
			cruises.add( new GMAObject( name, altURL, nation, mask ) );
			if( cruises.size()==1 ) {
				minY = rect.y;
				maxY = rect.y+rect.height;
			} else {
				if( minY>rect.y ) minY = rect.y;
				if( maxY<rect.y+rect.height ) maxY = rect.y+rect.height;
			}
			//	System.out.println( name +"\t"+ altURL +"\t"+ nation +"\t"+ rect);
		}
		in.close();

		base = CREDIT_PATH + "/tile_map_grid_";
		if (mapType == MapApp.MERCATOR_MAP) {
			base += "merc";
		} else if (mapType == MapApp.SOUTH_POLAR_MAP) {
			base += "SP";
		} else if (mapType == MapApp.NORTH_POLAR_MAP) {
			base += "NP";
		}
		base += ".gz";

		url = URLFactory.url(base);
		in = new DataInputStream(
			new GZIPInputStream( url.openStream() ));
		while( true ) {
			try {
				name = in.readUTF();
			} catch(EOFException e) {
				break;
			}
			String altURL = in.readUTF();
			if( altURL.equals("0") ) altURL=null;
			int nation = in.readInt();
			if( nation>=flags.length ) {
				boolean[] f = new boolean[nation+1];
				for( int i=0 ; i<flags.length ; i++)f[i]=flags[i];
				flags = f;
			}
			flags[nation]=true;
			Rectangle rect = new Rectangle(
				in.readShort(),
				in.readShort(),
				in.readShort(),
				in.readShort() );
			if( minY>rect.y ) minY = rect.y;
			if( maxY<rect.y+rect.height ) maxY = rect.y+rect.height;
			int size = rect.width*rect.height;
			byte[] buffer = new byte[(size+7)>>3];
			int len = 0;
			while( true ) {
				len += in.read( buffer, len, buffer.length-len );
				if( len==buffer.length ) break;
			}
			Grid2D.Boolean mask = new Grid2D.Boolean(
				rect, null);
			mask.setBuffer( buffer );
			grids.add( new GMAObject( name, altURL, nation, mask ) );
/*		System.out.println( name +"\t"+
		altURL +"\t"+
		nation +"\t"+
		rect);
*/
		}
		in.close();

		grids.trimToSize();
		cruises.trimToSize();
		/*
		 * Sorts the list of grids/cruises alphanumerically.
		 * Cycles through selections when a key is typed.
		 */
		Collections.sort(grids, new Comparator<GMAObject>() {
			public int compare(GMAObject o1, GMAObject o2) {
				return o1.name.compareToIgnoreCase(o2.name);
			}
		});

		Collections.sort(cruises, new Comparator<GMAObject>() {
			public int compare(GMAObject o1, GMAObject o2) {
				return o1.name.compareToIgnoreCase(o2.name);
			}
		});
		flagIcons = new JLabel[flags.length];
		for( int k=0 ; k<flags.length ; k++) {
			if( !flags[k] ) continue;
			flagIcons[k] = new JLabel(Flag.getSmallFlag(k));
			flagIcons[k].setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
		}
//		System.out.println( cruises.size() +" cruises\n"+ grids.size() +" grids");
	}
	public JCheckBoxMenuItem getMenuItem() {
		return menuItem;
	}
	public void setEnabled(boolean tf) {
		if(dialog!=null)dialog.setEnabled(tf);
		map.removeOverlay(this);
		JFrame frame = (JFrame)map.getTopLevelAncestor();
		if(tf) {
			map.addOverlay(this, false);
			frame.getContentPane().add( panel, "South");
		} else {
			frame.getContentPane().remove( panel );
		}
	}

	public void zoom(GMAObject o) {
		if( o==null )return;
		Rectangle r = o.mask.getBounds();
		r.x = r.x + -dx*1024/640;
		r.y = r.y + -dy*1024/640;
		double zoom = map.getZoom()*640/1024;
		Insets ins = map.getInsets();
		r.x = ins.left+(int)((r.x-2)*zoom);
		r.y = ins.top+(int)((r.y-2)*zoom);
		r.width = (int)((4+r.width)*zoom);
		r.height = (int)((4+r.height)*zoom);
		if( r.width<10 )r.width=10;
		if( r.height<10 )r.height=10;
		map.zoomTo( r );
		map.getMapTools().focus.doClick();
	}

	public CreditDialog dialog;
	public void showInfo() {
		if(dialog==null) dialog = new CreditDialog(this, 
				(JFrame)map.getTopLevelAncestor(),
				currentCruises,
				currentGrids );
		dialog.show();
	}
	JButton moreInfo;
	void initPanel() {
		JLabel label = new JLabel("Elevation Data Sources");
		try {
			moreInfo = new JButton( Icons.getIcon(Icons.INFO, false) );
			moreInfo.setPressedIcon( Icons.getIcon(Icons.INFO, true) );
			moreInfo.setBorder( BorderFactory.createEmptyBorder(0,0,0,3));
			moreInfo.addActionListener(new ActionListener() {
				public void actionPerformed( ActionEvent evt) {
					showInfo();
				}
			});
		} catch(Exception e) {
			moreInfo = new JButton( "info");
		}
		moreInfo.setToolTipText( "More Info");

		Box west = Box.createHorizontalBox();
		west.add( Box.createHorizontalStrut(6) );
		west.add( label );
		west.add( Box.createHorizontalStrut(6) );
		west.add( moreInfo);
		panel = new JPanel( new BorderLayout() );
		panel.add( west, "West");
		flagPanel = new JPanel(new GridLayout(1,0,2,2));
		flagPanel.setBorder( BorderFactory.createEmptyBorder(2,2,3,2));
		JPanel p = new JPanel( new BorderLayout() );
		p.add( flagPanel, "West");
		panel.add( p, "Center");
	}
	public JPanel getPanel() {
		return panel;
	}

	public void draw( java.awt.Graphics2D g) {
		if( maskImage != null ) {
		//	System.out.println("mask");
			Rectangle2D.Double rect = (Rectangle2D.Double) map.getClipRect2D();
			AffineTransform at = g.getTransform();
			AffineTransform transM = new AffineTransform();
			double wrap = map.getWrap();
			if(wrap > 0.) {
				while( maskX > rect.x ) maskX-=wrap;
				while( maskX + maskImage.getWidth()*maskScale < rect.x ) maskX+=wrap;
			}
			transM.translate(maskX, maskY);
			transM.scale(maskScale, maskScale);
			g.drawRenderedImage(maskImage, transM);
			g.setStroke( new BasicStroke( 2f/(float)map.getZoom()));
			g.setColor( Color.white);
			Rectangle2D.Double box = new Rectangle2D.Double(maskX, maskY,
					maskImage.getWidth()*maskScale, 
					maskImage.getHeight()*maskScale);
			g.draw(box);
			g.setTransform( at );
			if(wrap > 0.) {
				while( maskX < rect.x+rect.width ) {
					transM.translate(wrap/maskScale, 0.);
					g.drawRenderedImage(maskImage, transM);
					g.setTransform( at );
					maskX+=wrap;
					box.x += wrap;
					g.draw(box);
				}
			}
		}
		Rectangle2D r = map.getClipRect2D();
		double scale = 1024/640.;
		Rectangle r2 = new Rectangle();

		r2.x = (int)Math.floor( (r.getX() + dx)*scale );
		r2.y = (int)Math.floor( (r.getY() + dy)*scale );
		int x2 = (int)Math.ceil( (r.getX()+r.getWidth() + dx)*scale);
		int y2 = (int)Math.ceil( (r.getY()+r.getHeight() + dy)*scale);
		r2.width = x2-r2.x;
		r2.height = y2-r2.y;
//		System.out.println( r2.x +"\t"+ x2  +"\t"+ r2.y  +"\t"+ y2);
		if( r2.equals(bounds) )return;
		currentFlags = new boolean[flags.length];
		if( r2.width > 1024 && r2.y<=minY && r2.y+r2.height>=maxY) {
			bounds = r2;
			currentCruises = (Vector<GMAObject>)cruises.clone();
			currentGrids = (Vector<GMAObject>)grids.clone();
			for( int k=0 ; k<flags.length ; k++) currentFlags[k]=flags[k];
		} else if( bounds != null && bounds.contains(r2) ) {
			bounds = r2;
			currentCruises = find(currentCruises );
			currentGrids = find(currentGrids );
		} else {
			bounds = r2;
			currentCruises = find( cruises);
			currentGrids = find( grids);
		}
		if( dialog!=null )dialog.setModel( currentCruises, currentGrids);
		if( lastFlags==null ) lastFlags = new boolean[flags.length];
		boolean diff = false;
		for( int k=0 ; k<flags.length ; k++) {
			if( currentFlags[k]!=lastFlags[k] ) {
				diff=true;
				break;
			}
		}
		if( !diff ) return;
		flagPanel.removeAll();
		lastFlags = currentFlags;
		for( int k=0 ; k<flags.length ; k++) {
			if( lastFlags[k] ) flagPanel.add(flagIcons[k]);
		}
		flagPanel.invalidate();
		flagPanel.repaint();
	}
	
	Vector<GMAObject> find( Vector<GMAObject> v ) {
		Vector<GMAObject> v1 = new Vector<GMAObject>();
		Rectangle r;
		Grid2D.Boolean g;
//	System.out.println( v.size() );
		for( int k=0 ; k<v.size() ; k++) {
			GMAObject o = v.get(k);
			g = o.mask;
			r = g.getBounds();
			Rectangle bounds = (Rectangle)this.bounds.clone();
			while( r.x>=bounds.x+bounds.width ) bounds.x+=1024;
			while( r.x+r.width<bounds.x ) bounds.x-=1024;
//	if( o.name.equals("BOUVET")) {
//		System.out.println(r.y +"\t"+ (r.y+r.height) +"\t"+ bounds.y +"\t"+ (bounds.y+bounds.height));
//		System.out.println(r.x +"\t"+ (r.x+r.width) +"\t"+ bounds.x +"\t"+ (bounds.x+bounds.width));
//		System.out.println(bounds.intersection(r));
//	}
		//	if( r.x>=bounds.x+bounds.width ) continue;
			if( bounds.contains(r) ) {
				currentFlags[o.nation]=true;
				v1.add(o);
				continue;
			}
			r = bounds.intersection(r);
			if( r.width<=0 || r.height<=0 )continue;
			boolean ok = false;
			for( int y=0 ; y<r.height ; y++) {
				for( int x=0 ; x<r.width ; x++) {
					if( g.booleanValue(x+r.x, y+r.y) ) {
						ok = true;
						break;
					}
				}
				if( ok )break;
			}
			if( ok ) {
				v1.add(o);
				currentFlags[o.nation]=true;
			}
		}
		return v1;
	}

	BufferedImage maskImage;
	double maskX, maskY, maskScale;
	public void mask( GMAObject o ) {
		maskScale = 640./1024.;
		if( o==null ) {
			if( maskImage==null )return;
			maskImage = null;
			map.repaint();
		} else {
			Grid2D.Boolean m = o.mask;
			Rectangle r = m.getBounds();
			maskImage = new BufferedImage(r.width+4, r.height+4,
					BufferedImage.TYPE_INT_ARGB);

			int count = 0;
			for( int y=-2 ; y<r.height+2 ; y++) {
				for( int x=-2 ; x<r.width+2 ; x++) {
					if( m.booleanValue(r.x+x, r.y+y) ) {
						maskImage.setRGB(x+2, y+2, 0);
						count++;
					} else {
						maskImage.setRGB(x+2, y+2, 0x80000000);
					}
				}
			}
			maskX = (r.x-2)*maskScale - dx;
			maskY = (r.y-2)*maskScale - dy;
			map.repaint();
		}
	}
	public static void main(String[] args) {
		Runtime rt = Runtime.getRuntime();
		rt.gc();
		long start = rt.totalMemory()-rt.freeMemory();
		System.out.println( rt.totalMemory()-rt.freeMemory() );
		Credit c = null;
		try {
			c = new Credit(null, MapApp.MERCATOR_MAP);
		} catch(Exception e) {
			e.printStackTrace();
		}
		rt.gc();
		System.out.println( rt.totalMemory()-rt.freeMemory() );
		System.out.println( (rt.totalMemory()-rt.freeMemory()-start)>>10 );
		JFrame f = new JFrame("flags");
		f.setDefaultCloseOperation(EXIT_ON_CLOSE);
		f.getContentPane().add(c.getPanel());
		f.pack();
		f.setVisible(true);
	}
}

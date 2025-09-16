package haxby.db.xmcs;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import haxby.db.mcs.CDP;
import haxby.map.MapApp;
import haxby.map.Overlay;
import haxby.map.XMap;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

public class XMCruise implements Overlay {
	XMCS mcs;
	XMap map;
	String id;
	Vector<XMLine> lines;
	private boolean loaded;
	Rectangle2D.Double bounds;
	boolean cruiseIDL;
	// LDEO Multi Channel Seismic
	static String MULTI_CHANNEL_PATH = PathUtil.getPath("PORTALS/MULTI_CHANNEL_PATH",
			MapApp.BASE_URL+"/data/portals/mcs/");
	// file is found in each cruise
	static String CHANNEL_CONTROL = "/nav/mcs_control";
	static String CHANNEL_BOUNDS = "/nav/bounds";
	public XMCruise(XMCS mcs, XMap map, String id) {
		this.map = map;
		this.id = id.trim();
		lines = new Vector<XMLine>();
		bounds = new Rectangle2D.Double();
		setMap(map);
	}
	public void setMap(XMap map) {
	}
	public void setBounds (Point2D.Double wn, Point2D.Double es) {
		
		if((int)(Math.signum(wn.x)) != ((int)Math.signum(es.x))){
			if(Math.abs(es.x)>90 || Math.abs(wn.x)>90){
				cruiseIDL=true;
			}
		}
		else
			cruiseIDL=false;
		
		Point2D[] pts = new Point2D[] {
				map.getProjection().getMapXY(wn.x,wn.y),
				map.getProjection().getMapXY(es.x,wn.y),
				map.getProjection().getMapXY(wn.x,es.y),
				map.getProjection().getMapXY(es.x,es.y)};
		double wrap = map.getWrap();
		if (wrap > 0) {
			while (pts[0].getX() > pts[1].getX())
				pts[0].setLocation(pts[0].getX() - wrap, pts[0].getY());

			while (pts[2].getX() > pts[3].getX())
				pts[2].setLocation(pts[2].getX() - wrap, pts[2].getY());
		}

		double minX, minY;
		double maxX, maxY;
		minX = minY = Double.MAX_VALUE;
		maxX = maxY = -Double.MAX_VALUE;
		for (int i = 0; i < pts.length; i++) {
			minX = Math.min(minX, pts[i].getX());
			minY = Math.min(minY, pts[i].getY());
			maxX = Math.max(maxX, pts[i].getX());
			maxY = Math.max(maxY, pts[i].getY());
		}

		bounds.x = minX;
		bounds.y = minY;
		bounds.width = maxX - minX;
		bounds.height = maxY - minY;

		double dx = .04*Math.min( bounds.width, bounds.height );
		bounds.x -= dx;
		bounds.y -= dx;
		bounds.width += dx*2.;
		bounds.height += dx*2.;
	}

	public void setBounds() {
		if( lines.size()==0 )return;

		double minX, minY;
		double maxX, maxY;
		minX = minY = Double.MAX_VALUE;
		maxX = maxY = -Double.MAX_VALUE;

		for( int k=0 ; k<lines.size() ; k++) {
			Rectangle2D.Double l = ((XMLine)lines.get(k)).bounds;
			minX = Math.min( minX,  l.x );
			minY = Math.min( minY, l.y );
			maxX = Math.max(maxX, l.x + l.width);
			maxY = Math.max(maxY, l.y + l.height);
		}
		bounds.x = minX;
		bounds.y = minY;
		bounds.width = maxX - minX;
		bounds.height = maxY - minY;

		double dx = .04*Math.min( bounds.width, bounds.height );
		bounds.x -= dx;
		bounds.y -= dx;
		bounds.width += dx*2.;
		bounds.height += dx*2.;

		if (map.getWrap() > 0)
			while (bounds.x < 0)
				bounds.x += map.getWrap();
	}
	public String getID() {
		return new String(id);
	}
	public String toString() {
		return getID();
	}
	public void addLine(XMLine line) {
		lines.add(line);
	}
	public XMLine[] getLines() {
		XMLine[] tmp = new XMLine[lines.size()];
		for(int i=0 ; i<lines.size() ; i++) tmp[i] = (XMLine)lines.get(i);
		return tmp;
	}

	/**
	 * Tries to load lines from
	 * 	MCS/cruiseID/nav/mcs_control and
	 *	MCS/cruiseID/nav/bounds
	 *
	 * Tries to sort the loaded lines numerically
	 * @throws IOException
	 */
	public XMLine[] loadLines() throws IOException {
			return loadLines(MULTI_CHANNEL_PATH);
		}

	public XMLine[] loadLinesFromFile() throws IOException {
		File f = new File("./" + CHANNEL_CONTROL);
		File f2 = new File("./" + CHANNEL_BOUNDS);
		try {
			DataInputStream in = new DataInputStream(new FileInputStream(f));
			DataInputStream in2 = new DataInputStream(new FileInputStream(f2));
			return loadLinesFromStream(in, in2);
		} catch (FileNotFoundException ex) {
			return null;
		}
	}
	
	
	public XMLine[] loadLines(String path) throws IOException {
		URL url = URLFactory.url( path + id + CHANNEL_CONTROL);
		URL url2 = URLFactory.url( path + id + CHANNEL_BOUNDS);
		try {
			DataInputStream in = new DataInputStream(url.openStream());
			DataInputStream in2 = new DataInputStream(url2.openStream());
			return loadLinesFromStream(in, in2);
		} catch (FileNotFoundException ex) {
			return null;
		}
	}
	
	public XMLine[] loadLinesFromStream(DataInputStream in, DataInputStream in2) throws IOException {	
		String s;
		// Load lines from MCS/cruiseID/nav/mcs_control
		while( true ) {
			try {
				s = in.readUTF();
			} catch (EOFException ex) {
				break;
			}
			StringTokenizer st = new StringTokenizer(s);
			String lineID = st.nextToken();
			String cruiseId = st.nextToken();

			int nseg = in.readInt();
			int npt = in.readInt();
			CDP[] cdp = new CDP[npt];
			for( int k=0 ; k<npt ; k++ ) {
				int[] entry = new int[] {
					in.readInt(),
					in.readInt(),
					in.readInt() };
				cdp[k] = new CDP( entry[2],
					(double)(entry[0]*1.e-6),
					(double)(entry[1]*1.e-6),
					(long)entry[2], false);
			}
			if( !cruiseId.equals( id ) )
				continue;

			XMLine line = new XMLine( map, this, lineID, cdp );
			addLine( line );
		}
		in.close();

		// Load bounds from MCS/cruiseID/nav/bounds
		BufferedReader reader = new BufferedReader(
				new InputStreamReader( in2 ));

		XMLine[] lines = getLines();
		while( (s=reader.readLine()) != null ) {
			StringTokenizer st = new StringTokenizer(s);
			String cruiseId = st.nextToken();
			int index = -1;
			if (! cruiseId.equals(id))
				continue;

			index = -1;
			String lineId = st.nextToken();
			for( int i=0 ; i<lines.length ; i++) {
				if( lineId.equals( lines[i].getID() )) {
					index = i;
					break;
				}
			}
			if( index==-1 ) continue;
			double[] cdpRange = new double[] {
					Double.parseDouble( st.nextToken()),
					Double.parseDouble( st.nextToken()) };

			double[] zRange = new double[] {
					Double.parseDouble( st.nextToken()),
					Double.parseDouble( st.nextToken()) };
//			zRange[1] *= 2;
			zRange[1] += zRange[0];
			lines[index].setRanges( cdpRange, zRange );
		}

		Collections.sort(this.lines, new Comparator<Object>() {
			public int compare(Object arg0, Object arg1) {
				String s0 = ((XMLine) arg0).lineID;
				String s1 = ((XMLine) arg1).lineID;
				try {
					String[] ss0 = s0.split("[^\\d\\.]");
					String[] ss1 = s1.split("[^\\d\\.]");
					float n0 = Float.parseFloat(ss0[0]);
					float n1 = Float.parseFloat(ss1[0]);
					return n0 - n1 < 0 ?  -1 : (n0 - n1 == 0 ? 0 : 1);
				} catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
					return s0.compareTo(s1);
				}
			}
		});

		for (int i = 0; i < lines.length; i++)
			lines[i].setMap(map);

		// Add crossings to XMLines
		for( int k=0 ; k<lines.length-1 ; k++) {
			for( int j=k+1 ; j<lines.length ; j++) {
				double[] crs = XMLine.cross(lines[k], lines[j]);
				if(crs==null) continue;
				lines[k].addCrossing( crs[0], crs[1], lines[j] );
				lines[j].addCrossing( crs[1], crs[0], lines[k] );
			}
		}
		setBounds();
		loaded = true;
		return getLines();

	}

	public void clearLines() {
		loaded = false;
		lines.clear();
	}
	
	public synchronized boolean isLoaded() {
		return loaded;
	}

	public void draw(Graphics2D g) {
		if(map==null)return;
		double wrap = map.getWrap();
		AffineTransform at = g.getTransform();
		double offset = 0.;
		Rectangle2D rect = map.getClipRect2D();
		if( wrap>0. ) {
			while( bounds.x+offset > rect.getX() ) offset -= wrap;
			while( bounds.x+bounds.width+offset < rect.getX() ) offset += wrap;
		}
		if( bounds.x+offset > rect.getX()+rect.getWidth() ) return;
		g.translate( offset, 0.);
		//g.draw(bounds);
		drawLines(g);
		while( wrap>0. && bounds.x +offset < rect.getX()+rect.getWidth() ) {
			offset += wrap;
			g.translate( wrap, 0.);
			//g.draw(bounds);
			drawLines(g);
		}
		g.setTransform(at);
	}
	public void drawLines(Graphics2D g) {
		if(map==null)return;

		for(int i=0 ; i<lines.size() ; i++) {
			XMLine line = (XMLine)lines.get(i);
			if( line.getZRange()==null ) {
				g.setStroke( new BasicStroke( 1f/(float)map.getZoom() ) );
			//	g.setColor( Color.lightGray );
			} else {
				g.setStroke( new BasicStroke( 2f/(float)map.getZoom() ) );
			//	g.setColor( Color.black );
			}
			line.draw(g);
		}

	}
	public boolean contains( double x, double y, double wrap ) {
		if( wrap<=0. ) return bounds.contains( x, y);
		if( y<bounds.y || y>bounds.y+ bounds.height ) return false;
		double offset = 0;
		while( bounds.x+offset > x ) offset-=wrap;
		while( bounds.x+bounds.width+offset < x ) offset+=wrap;
		if( bounds.x + offset <= x ) return true;
		return false;
	}
	public Rectangle2D getBounds() {
		if(map==null) return new Rectangle();
		return new Rectangle2D.Double( bounds.x, bounds.y, bounds.width, bounds.height );
	}
}

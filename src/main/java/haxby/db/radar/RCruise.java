package haxby.db.radar;

import haxby.db.mcs.*;
import haxby.db.xmcs.XMLine;
import haxby.map.*;
import haxby.proj.*;
import haxby.util.URLFactory;

import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class RCruise implements Overlay {
	Radar mcs;
	XMap map;
	String id;
	Vector lines;
	Rectangle2D.Double bounds;	
	static String CHANNEL_CONTROL = "/nav/mcs_control";
	static String CHANNEL_BOUNDS = "/nav/bounds";
	public RCruise(Radar mcs, XMap map, String id) {
		this.map = map;
		this.id = id.trim();
		lines = new Vector();
		bounds = new Rectangle2D.Double();
		setMap(map);
	}
	public void setMap(XMap map) {
	}
	public void setBounds() {
		if( lines.size()==0 )return;
		Rectangle2D.Double l = ((RLine)lines.get(0)).bounds;
		bounds = new Rectangle2D.Double( l.x, l.y, l.width, l.height );
		for( int k=1 ; k<lines.size() ; k++) {
			l = ((RLine)lines.get(k)).bounds;
			if( l.x<bounds.x ) bounds.x = l.x;
			if( l.y<bounds.y ) bounds.y = l.y;
			if( l.width>bounds.width ) bounds.width = l.width;
			if( l.height>bounds.height ) bounds.height = l.height;
		}
	//	bounds.width -= bounds.x;
	//	bounds.height -= bounds.y;
	System.out.println( id +"\t"+ bounds.x +"\t"+ bounds.y +"\t"+ bounds.width +"\t"+ bounds.height);
	}
	public String getID() {
		return new String(id);
	}
	public String toString() {
		return getID();
	}
	public void addLine(RLine line) {
		lines.add(line);
	}
	public RLine[] getLines() {
		RLine[] tmp = new RLine[lines.size()];
		for(int i=0 ; i<lines.size() ; i++) tmp[i] = (RLine)lines.get(i);
		return tmp;
	}
	public void draw(Graphics2D g) {
		if(map==null)return;
		double wrap = map.getWrap();
		AffineTransform at = g.getTransform();
		double offset = 0.;
		Rectangle rect = g.getClipBounds();
		if( wrap>0. ) {
			while( bounds.x+offset > rect.getX() ) offset -= wrap;
			while( bounds.x+bounds.width+offset < rect.getX() ) offset += wrap;
		}
		if( bounds.x+offset > rect.getX()+rect.getWidth() ) return;
		g.translate( offset, 0.);
	//	g.draw(bounds);
		while( wrap>0. && bounds.x +offset < rect.getX()+rect.getWidth() ) {
			offset += wrap;
			g.translate( wrap, 0.);
	//		g.draw(bounds);
		}
		g.setTransform(at);
	}
	public void drawLines(Graphics2D g) {
		if(map==null)return;
	//	if( !g.getClipBounds().intersects(bounds)) return;
		double wrap = map.getWrap();
		AffineTransform at = g.getTransform();
		double offset = 0.;
		Rectangle rect = g.getClipBounds();
		if( wrap>0. ) {
			while( bounds.x+offset > rect.getX() ) offset -= wrap;
			while( bounds.x+bounds.width+offset < rect.getX() ) offset += wrap;
		}
		if( bounds.x+offset > rect.getX()+rect.getWidth() ) return;
		g.translate( offset, 0.);
		for(int i=0 ; i<lines.size() ; i++) {
			RLine line = (RLine)lines.get(i);
			if( line.getZRange()==null ) {
				g.setStroke( new BasicStroke( 1f/(float)map.getZoom() ) );
			//	g.setColor( Color.lightGray );
			} else {
				g.setStroke( new BasicStroke( 2f/(float)map.getZoom() ) );
			//	g.setColor( Color.black );
			}
			line.draw(g);
		}
		while( wrap>0. && bounds.x +offset < rect.getX()+rect.getWidth() ) {
			offset += wrap;
			g.translate( wrap, 0.);
			for(int i=0 ; i<lines.size() ; i++) {
				RLine line = (RLine)lines.get(i);
				line.draw(g);
			}
		}
		g.setTransform(at);
	}
	public boolean contains( double x, double y, double wrap ) {
		if( wrap<=0. ) return bounds.contains( x, y);
		if( y<bounds.y || y>bounds.y+ bounds.height ) return false;
		double offset = 0;
		while( bounds.x+offset > x ) offset-=wrap;
		while( bounds.x+bounds.width+offset < x ) offset+=wrap;
		if( bounds.x <= x ) return true;
		return false;
	}
	public Rectangle2D getBounds() {
		if(map==null) return new Rectangle();
		return new Rectangle2D.Double( bounds.x, bounds.y, bounds.width, bounds.height );
	}
/*Donald Pomeroy, loads the lines for a cruise, same style as XMCruise Added 9.16.2011*/
public RLine[] loadLines(String path) throws IOException {
	URL url = URLFactory.url( path + id + CHANNEL_CONTROL);
	DataInputStream in = new DataInputStream(url.openStream());
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

		RLine line = new RLine( map, this, lineID, cdp );
		addLine( line );
	}
	in.close();

	// Load bounds from MCS/cruiseID/nav/bounds
	
	URL url2 = URLFactory.url( path + id + CHANNEL_BOUNDS);
	in = new DataInputStream( url2.openStream() );
	BufferedReader reader = new BufferedReader(
			new InputStreamReader( in ));

	RLine[] lines = getLines();
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
//		zRange[1] *= 2;
		zRange[1] += zRange[0];
		lines[index].setRanges( cdpRange, zRange );
	}

	Collections.sort(this.lines, new Comparator<Object>() {
		public int compare(Object arg0, Object arg1) {
			String s0 = ((RLine) arg0).lineID;
			String s1 = ((RLine) arg1).lineID;
			try {
				String[] ss0 = s0.split("[^\\d\\.]");
				String[] ss1 = s1.split("[^\\d\\.]");
				float n0 = Float.parseFloat(ss0[0]);
				float n1 = Float.parseFloat(ss1[0]);
				return n0 - n1 < 0 ?  -1 : (n0 - n1 == 0 ? 0 : 1);
			} catch (NumberFormatException ex) {
				return s0.compareTo(s1);
			}
		}
	});

	for (int i = 0; i < lines.length; i++)
		lines[i].setMap(map);

	// Add crossings to RLines
	for( int k=0 ; k<lines.length-1 ; k++) {
		for( int j=k+1 ; j<lines.length ; j++) {
			double[] crs = RLine.cross(lines[k], lines[j]);
			if(crs==null) continue;
			lines[k].addCrossing( crs[0], crs[1], lines[j] );
			lines[j].addCrossing( crs[1], crs[0], lines[k] );
		}
	}
	setBounds();
	return getLines();
}

}
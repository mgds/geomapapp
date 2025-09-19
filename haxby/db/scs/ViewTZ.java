package haxby.db.scs;

import haxby.db.*;
import haxby.map.*;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;

public class ViewTZ implements XYPoints, MouseMotionListener {

	double xScale, yScale;
	double[] xRange, yRange;
	Vector tz;
	Calendar cal;
	JLabel label;
	XYGraph graph;
	String cruise;
	public ViewTZ() {
		reset();
		graph = null;
		cruise = "";
		label = new JLabel("-----");
		cal = Calendar.getInstance( TimeZone.getTimeZone( "GMT" ) );
	}
	void reset() {
		xScale = yScale = 1.;
		xRange = yRange = new double[] {0., 1.};
		tz = new Vector();
	}
	public JLabel getLabel() {
		return label;
	}
	public void setCruise( String cruise ) {
		this.cruise = cruise;
	}
	public String getCruise() {
		return cruise;
	}
	public void setGraph( XYGraph graph ) {
		this.graph = graph;
	}
	public void setTZ( File tzFile ) {
		tz = new Vector();
		try {
			DataInputStream in = new DataInputStream(
				new BufferedInputStream(
				new FileInputStream( tzFile )));
			while( true ) {
				try {
					int t = in.readInt();
					short z = in.readShort();
					tz.add( new TZ( t, z) );
				} catch(EOFException ex) {
					break;
				}
			}
			in.close();
		} catch( Exception ex) {
			JOptionPane.showMessageDialog(MapApp.anchor, "could not load "+tzFile.getName() );
			reset();
			return;
		}
		if( tz.size()<2 ) {
			JOptionPane.showMessageDialog(MapApp.anchor, "no data in "+tzFile.getName() );
			reset();
			return;
		}
		int minT, maxT;
		short minZ, maxZ;
		minT = maxT = ((TZ)tz.get(0)).t;
		minZ = maxZ = ((TZ)tz.get(0)).z;
		for( int k=1 ; k<tz.size() ; k++ ) {
			TZ tt = (TZ)tz.get(k);
			if( tt.t>maxT ) maxT=tt.t;
			else if( tt.t<minT ) minT=tt.t;
			if( tt.z>maxZ ) maxZ=tt.z;
			else if( tt.z<minZ ) minZ=tt.z;
		}
		xRange = new double[] { (double)minT, (double)maxT };
		yRange = new double[] { 1000.+(double)maxZ, -1000.+(double)minZ };
		xScale = 1./30.;
		yScale = 1./7.5;
	}
	public String getXTitle(int dataIndex) {
		return "time";
	}
	public String getYTitle(int dataIndex) {
		return "2-way time";
	}
	public double[] getXRange(int dataIndex) {
		return new double[] {xRange[0], xRange[1]};
	}
	public double[] getYRange(int dataIndex) {
		return new double[] {yRange[0], yRange[1]};
	}
	public double getPreferredXScale(int dataIndex) {
		return xScale;
	}
	public double getPreferredYScale(int dataIndex) {
		return yScale;
	}
	public void plotXY( Graphics2D g, 
				Rectangle2D bounds,
				double xScale, double yScale,
				int dataIndex) {
		GeneralPath path = new GeneralPath();
		if( tz.size()<2 ) return;
		double x0 = bounds.getX();
		double y0 = bounds.getY();
		double x1 = x0;
		double x2 = x1+bounds.getWidth();
		if(x1>x2) {
			x1 = x2;
			x2 = x0;
		}
		int i=0;
		boolean connect = false;
		for( i=0 ; i<tz.size() ; i++) {
			TZ tt = (TZ)tz.get(i);
			if( tt.t<x1 )continue;
			if( tt.t>x2 ) break;
			float x = (float)(xScale*(tt.t-x0));
			float y = (float)(yScale*(tt.z-y0));
			if( connect ) {
				path.lineTo( x, y );
			} else {
				path.moveTo( x, y );
				connect = true;
			}
		}
		g.setColor( Color.black );
		g.draw( path );
	}
	public void mouseDragged( MouseEvent evt ) {
	}
	public void mouseMoved( MouseEvent evt ) {
		if( graph==null ) return;
		long t = 1000L*(long)graph.getXAt( evt.getPoint() );
		double z = graph.getYAt( evt.getPoint() );
		label.setText( SCSCruise.dateString( t ) +"  -  "+ z);
	}
	class TZ {
		public int t;
		public short z;
		public TZ( int t, short z ) {
			this.t = t;
			this.z = z;
		}
	}
	public static void main( String[] args ) {
		JFileChooser chooser = new JFileChooser( System.getProperty("user.dir"));
		int ok = chooser.showOpenDialog(null);
		if( ok==chooser.CANCEL_OPTION ) System.exit(0);
		File file = chooser.getSelectedFile();
		ViewTZ view = new ViewTZ();
		view.setTZ( file );
		XYGraph graph = new XYGraph( view, 0 );
		graph.setScrollableTracksViewportWidth( false );
		graph.setScrollableTracksViewportHeight( true);
		JScrollPane sp = new JScrollPane( graph );
		JFrame frame = new JFrame( file.getName() );
		frame.getContentPane().add( sp );
		frame.getContentPane().add( view.getLabel(), "North" );
		frame.pack();
		frame.setSize( new Dimension( 1000, 500 ) );
		Zoomer z = new Zoomer( graph );
		view.setGraph( graph);
		graph.addMouseListener( z );
		graph.addMouseMotionListener( z );
		graph.addKeyListener( z );
		graph.addMouseMotionListener( view );
		frame.setVisible(true);
		frame.setDefaultCloseOperation( frame.EXIT_ON_CLOSE );
	}
}
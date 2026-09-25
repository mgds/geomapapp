package org.geomapapp.db.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;

import org.geomapapp.gis.shape.ESRIShapefile;
import org.geomapapp.gis.table.TableDB;
import org.geomapapp.io.LittleIO;
import org.geomapapp.util.ColorServer;

import haxby.map.MapApp;
import haxby.map.Overlay;
import haxby.map.XMap;
import haxby.util.MyCellRenderer;
import haxby.util.URLFactory;
import haxby.util.XBTable;
import haxby.util.GeneralUtils;

public class GTable extends TableDB implements Overlay {
	
	protected XMap map;
	protected 	ESRIShapefile shapeFile;
//	float[][] currentXY;
	protected int[] selectedRows;
	protected Rectangle2D lastBox;
	protected JFrame frame;
	protected MouseInputAdapter mouseSelect;
	protected int lastSelected=-1;
	protected ColorServer colorer = null;
	protected double symbolSize = 3.;
	protected boolean drawSel = false;
	
	protected boolean visible = true;
	
	MyCellRenderer renderer = null;
	
	public GTable( Vector headings, Vector rows, TableDB parent) {
		super(headings, rows, parent);
	}
	public GTable( Vector headings, Vector rows, StringBuffer comments) {
		super( headings, rows, comments);
	}
	public GTable(String url) throws IOException {
		super( url);
	}
	public GTable(String url, String delim) throws IOException {
		super( url, delim);
	}
	public GTable(File file) throws IOException {
		super( file);
	}
	public GTable(File file, String delim) throws IOException {
		super( file, delim);
	}
	public GTable() throws IOException {
		super();
	}
	public void setShapeFile( ESRIShapefile sf ) {
		shapeFile = sf;
		if( map==null )return;
	}
	public void setMap( XMap map ) {
		this.map = map;
		if( latCol<0 || lonCol<0 )return;
		int k=0;
		Vector pts = new Vector(allRows.size());
		while( k<allRows.size() ) {
			Vector row = (Vector)allRows.get(k);
			Point2D p = getPoint(k);
			if( p==null ) {
				allRows.remove(k);
				continue;
			}
			pts.add(map.getProjection().getMapXY(p));
			k++;
		}
		allRows.trimToSize();
	//	xy = new float[allRows.size()][2];
		for( k=0 ; k<allRows.size() ; k++) {
			Point2D p = (Point2D)pts.get(k);
			float[] xy = new float[2];
			xy[0] = (float)p.getX();
			xy[1] = (float)p.getY();
			Vector row = (Vector)allRows.get(k);
			while( row.size()<headings.size() )row.add(null);
			while( row.size()>headings.size() )row.remove(row.size()-1);
			row.add( new Integer(k));
			row.add( xy );
		}
		table = createTable();
		table.getSelectionModel().addListSelectionListener(
			new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					if( !e.getValueIsAdjusting() )drawSelection();
				}
			});
		mouseSelect = new MouseInputAdapter() {
			public void mouseClicked(MouseEvent e) {
				if( e.isControlDown() ) return;
				if( !table.getTopLevelAncestor().isVisible() )return;
				select(e);
			}
		};
		map.addMouseListener( mouseSelect );
	}
	public void setSymbolRadius(double r) {
		symbolSize = r;
	}
	public void setDrawSelectionOnly( boolean tf ) {
		drawSel = tf;
	}
	public void setColorServer( ColorServer s ) {
		colorer = s;
	}
	public int[] sort(int column, boolean inverse) {
		int[] indices = super.sort( column, inverse);
		Class c = classes[column];
		int i1=0;
		int i2=-1;
		if( c==Boolean.class ) {
			for( i2=0 ; i2<getRowCount() ; i2++) {
				Boolean b = (Boolean)getValueAt(i2, column);
				if( b==null || !b.booleanValue() ) break;
			}
		} else {
			for( i2=0 ; i2<getRowCount() ; i2++) {
				if( getValueAt(i2, column)==null ) break;
			}
		}
		i2--;
		table.clearSelection();
		if( i2>=0)table.getSelectionModel().setSelectionInterval(0, i2);
		redraw();
		return indices;
	}
	void select( MouseEvent e) {
		if( !map.isSelectable() ) return;
		double zoom = map.getZoom();
		double radius = 3./Math.pow(zoom, .75);
		double r2 = radius*radius;
		double wrap = map.getWrap();
		Point2D p = map.getScaledPoint(e.getPoint());
		if( !e.isShiftDown() ) table.clearSelection();
		int xyIndex = headings.size()+1;
		for( int k=0 ; k<currentRowsIndices.size() ; k++) {
			Vector row = getCurrentRow(k);
			float[] xy = (float[])row.get(xyIndex);
			double offset = 0.;
			
			
//			ERROR: THIS BLOCK OF CODE CAUSES SOUTH POLAR TO CRASH
//			TODO: Must find out what the effect of this code is and preferably run 
//			it when mercator is selected
/*			while( xy[0]+offset<p.getX()-radius ) offset+=wrap;
			while( xy[0]+offset>p.getX()+radius ) offset-=wrap;*/
//			***** TEST CODE *****
/*			while( xy[0]+offset<p.getX()-radius )	{
				System.out.println(xy[0]+offset);
				offset+=wrap;
			}
			while( xy[0]+offset>p.getX()+radius )	{
				offset-=wrap;
			}*/
//			***** TEST CODE *****
//			ERROR: THIS BLOCK OF CODE CAUSES SOUTH POLAR TO CRASH
			
			//This line handles if the point is displayed more than once
			//on the map due to the wrap.
			GeneralUtils.unwrapPoint(map, (Point2D.Double) p);
			
			if( xy[0]+offset<p.getX()-radius )continue;
			double r = p.distanceSq( offset+xy[0], (double)xy[1]);
			if( r < r2) {
				table.getSelectionModel().addSelectionInterval(k,k);
				table.ensureIndexIsVisible(k);
			}
		}
	}
	public void redraw() {
		if( map==null )return;
		synchronized(map.getTreeLock()) {
			draw(map.getGraphics2D());
		}
	}
	public Point2D getPoint(int rowIndex) {
		Vector row = (Vector)allRows.get(rowIndex);
		if( row.get(latCol)==null || row.get(lonCol)==null)return null;
		Point2D.Double p = new Point2D.Double( 
			((Double)row.get(lonCol)).doubleValue(),
			((Double)row.get(latCol)).doubleValue() );
		return p;
	}
	public void drawSelection() {
		if( map==null ) return;
		synchronized(map.getTreeLock()) {
			Graphics2D g = map.getGraphics2D();
			double wrap=map.getWrap();
			boolean check = wrap>0.;
			Rectangle2D box = map.getClipRect2D();
			double minX = box.getX();
			double maxX = minX+box.getWidth();
			double zoom = map.getZoom();
			double size = symbolSize/Math.pow(zoom, .75);
			Arc2D.Double arc = new Arc2D.Double( -size, -size, 2*size, 2*size, 0., 360., Arc2D.CHORD );
			AffineTransform at = g.getTransform();
			g.setStroke( new BasicStroke( .5f*(float)size ));
			check = check && box.getWidth()>wrap;
			if( selectedRows==null ) selectedRows = new int[0];
			int xyIndex = headings.size()+1;
			for( int i=0 ; i<selectedRows.length ; i++) {
				int k = selectedRows[i];
				if( k>=currentRowsIndices.size() )continue;
				Color color = colorer!=null 
					? colorer.getColor( getValueAt(k,0) )
					: Color.lightGray;
				if( color==null )continue;
				if( color.getRGB()==0 )color = Color.lightGray;
				Vector row = getCurrentRow(k);
				float[] xy = (float[])row.get(xyIndex);
				double x = (double)xy[0];
				g.translate( x, (double)xy[1]);
				g.setColor( Color.black);
				g.draw( arc );
				g.setColor( color );
				g.fill( arc );
				if( check && x<maxX ) {
					g.translate( wrap, 0.);
					g.setColor( Color.black);
					g.draw( arc );
					g.setColor( color );
					g.fill( arc );
				}
				g.setTransform(at);
			}
			createTable();
			if( table==null ) {
				System.out.println("no table");
				return;
			}
			selectedRows = table.getSelectedRows();
			for( int i=0 ; i<selectedRows.length ; i++) {
				int k = selectedRows[i];
				Color color = colorer!=null 
					? colorer.getColor( getValueAt(k,0) )
					: Color.red;
				if( color==null )continue;
				if( color.getRGB()==0 )color = Color.red;
				Vector row = getCurrentRow(k);
				float[] xy = (float[])row.get(xyIndex);
				double x = (double)xy[0];
				g.translate( x, (double)xy[1]);
				g.setColor( Color.white);
				g.draw( arc );
				g.setColor( color );
				g.fill( arc );
				if( check && x<maxX ) {
					g.translate( wrap, 0.);
					g.setColor( Color.white);
					g.draw( arc );
					g.setColor( color );
					g.fill( arc );
				}
				g.setTransform(at);
			}
		}
	}
	
	protected void setArea(Rectangle2D rect) {
		double minX = rect.getX();
		double maxX = minX+rect.getWidth();
		double minY = rect.getY();
		double maxY = minY+rect.getHeight();
		double wrap=map.getWrap();
		boolean check = wrap>0.;
		int xyIndex = headings.size()+1;
		if( !rect.equals(lastBox) ) {
			Vector<Integer> test;
		//	float[][] xytest = xy;
			if( lastBox!=null && lastBox.contains(minX, minY, rect.getWidth(), rect.getHeight())) {
				test = currentRowsIndices;
		//		xytest = currentXY;
			} else
			{
				test = new Vector<Integer>(allRows.size());
				for (int i = 0 ; i < allRows.size(); i++)
					test.add(i);
			}
		//	Vector newXY = new Vector(test.size());
			Vector<Integer> newRows = new Vector<Integer>(test.size());
			int[] selected = table==null
				? new int[0]
				: table.getSelectedRows();
			Vector select = new Vector(selected.length);
			for( int k=0 ; k<selected.length ; k++) {
				select.add( currentRowsIndices.get(selected[k]));
			}
			Vector selIndices = new Vector(selected.length);
			for( int k=0 ; k<test.size() ; k++) {
				int sel = select.indexOf(test.get(k));
				if( sel>=0 )select.remove(sel);
				Vector row = (Vector)
								allRows.get(test.get(k));
				float[] xy = (float[])row.get(xyIndex);
				if( xy[1]>maxY || xy[1]<minY )continue;
				boolean inside=false;
				if( check ) {
					while( xy[0]>minX+wrap )xy[0]-=wrap;
					while( xy[0]<minX )xy[0]+=wrap;
					inside = xy[0]<maxX;
				} else {
					inside = xy[0]>minX && xy[0]<maxX;
				}
				if( inside ) {
				//	newXY.add(xytest[k]);
					newRows.add( test.get(k) );
					if( sel>=0 )selIndices.add(new Integer(newRows.size()-1));
				}
			}
			newRows.trimToSize();
		//	currentXY = new float[newXY.size()][];
		//	for( int k=0 ; k<newXY.size() ; k++) currentXY[k] = (float[])newXY.get(k);
			currentRowsIndices = newRows;
//	System.out.println( newRows.size() +" stations");
			if( table!=null ) fireTableDataChanged();
			selectedRows = new int[selIndices.size()];
	//	System.out.println( selectedRows.length );
			if (table != null)
			{
				table.getSelectionModel().setValueIsAdjusting(true);
				for( int k=0 ; k<selIndices.size() ; k++) {
					int i = ((Integer)selIndices.get(k)).intValue();
					try {
						selectedRows[k] = i;
						table.getSelectionModel().addSelectionInterval(i,i);
					} catch(Exception e) {
			//	System.out.println( k +"\t"+ selectedRows.length +"\t"+ selIndices.size());
						selectedRows = new int[0];
					}
				}
				table.getSelectionModel().setValueIsAdjusting(false);
			}
		}
		
		lastBox = rect;
	}
	
	public void draw( Graphics2D g ) {
		if( map==null ) return;
		if ( !visible ) return;
		if (table != null && table.getTopLevelAncestor() != null)
			if( !table.getTopLevelAncestor().isVisible() )return;
		
		Rectangle2D box = map.getClipRect2D();
		double minX = box.getX();
		double maxX = minX+box.getWidth();
		double wrap=map.getWrap();
		boolean check = wrap>0.;
		int xyIndex = headings.size()+1;
		
		this.setArea(box);
		if( currentRowsIndices.size()>20000 )return;
		
		double zoom = map.getZoom();
		double size = symbolSize/Math.pow(zoom, .75);
		Arc2D.Double arc = new Arc2D.Double( -size, -size, 2*size, 2*size, 0., 360., Arc2D.CHORD );
		AffineTransform at = g.getTransform();
		g.setStroke( new BasicStroke( .5f*(float)size ));
		check = check && box.getWidth()>wrap;
		if( !drawSel ) {
			for( int k=0 ; k<currentRowsIndices.size() ; k++) {
				Vector row = getCurrentRow(k);
				Color color = colorer!=null 
					? colorer.getColor( getValueAt(k,0) )
					: Color.lightGray;
				if( color==null )continue;
				if( color.getRGB()==0 )color = Color.lightGray;
				float[] xy = (float[])row.get(xyIndex);
				double x = (double)xy[0];
				g.translate( x, (double)xy[1]);
				g.setColor( Color.black);
				g.draw( arc );
				g.setColor( color );
				g.fill( arc );
				if( check && x<maxX ) {
					g.translate( wrap, 0.);
					g.setColor( Color.black);
					g.draw( arc );
					g.setColor( color );
					g.fill( arc );
				}
				g.setTransform(at);
			}
		}
		createTable();
		if( table==null ) {
			System.out.println("no table");
			return;
		}
		selectedRows = table.getSelectedRows();
		for( int i=0 ; i<selectedRows.length ; i++) {
			int k = selectedRows[i];
			Color color = colorer!=null 
				? colorer.getColor( getValueAt(k,0) )
				: Color.red;
			if( color==null )continue;
			if( color.getRGB()==0 )color = Color.red;
			Vector row = getCurrentRow(k);
			float[] xy = (float[])row.get(xyIndex);
			double x = (double)xy[0];
			g.translate( x, (double)xy[1]);
			g.setColor( Color.white);
			g.draw( arc );
			g.setColor( color );
			g.fill( arc );
			if( check && x<maxX ) {
				g.translate( wrap, 0.);
				g.setColor( Color.white);
				g.draw( arc );
				g.setColor( color );
				g.fill( arc );
			}
			g.setTransform(at);
		}
		drawSel = false;
	}
	protected void apply() {
		super.apply();
		map.repaint();
	}
	public void reset() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				rows = allRows;
				currentRowsIndices.clear();
				for (int i = 0 ; i < allRows.size(); i++)
					currentRowsIndices.add(i);
				lastBox = null;
				if( map==null ) {
					if( table!=null ) fireTableDataChanged();
					return;
				}
				synchronized(map.getTreeLock()) {
					draw(map.getGraphics2D());
				}
			}
		});
	}
	public static GTable readDBF(String path, String filename) throws IOException {
		boolean url = path.startsWith( "http://" ) || path.startsWith("https://") || path.startsWith( "file://" );
		if( url&& !path.endsWith("/") ) path += "/";
		InputStream in = url ?
			(URLFactory.url( path+filename+".dbf")).openStream()
			: new FileInputStream( new File(path, filename+".dbf"));
		DataInputStream input = new DataInputStream(
			new BufferedInputStream( in ));
		byte version = input.readByte();
		input.readByte();
		input.readByte();
		input.readByte();
		int nRecord = LittleIO.readInt( input );
		int hLength = LittleIO.readShort( input );
		int recLength = LittleIO.readShort( input );
	//	System.out.println( nRecord +"\t"+ hLength +"\t"+ recLength);
		for( int k=12 ; k<32 ; k++) input.readByte();
		byte[] nm = new byte[11];
		int index = 32;
		Vector names = new Vector();
		Vector classes = new Vector();
		Vector lengths = new Vector();
		int rLen = 1;
		while( rLen<recLength ) {
// System.out.println( rLen +"\t"+ recLength);
			input.readFully(nm);
			int offset=0;
			for( int j=0 ; j<nm.length ; j++) {
				if( nm[j]>32 )break;
				offset++;
			}
			int len=nm.length-offset;
			for( int j=nm.length-1 ; j>=offset ; j--) {
				if( nm[j]>32 )break;
				len--;
			}
			String name = new String(nm,offset,len);
		//	name.trim();
			names.add(name);
			byte[] tp = new byte[] { input.readByte() };
			String type = new String(tp);
			if( type.equalsIgnoreCase("C") ) classes.add( String.class );
			else if( type.equalsIgnoreCase("N") ) classes.add( Number.class );
			else if( type.equalsIgnoreCase("L") ) classes.add( Boolean.class );
			else classes.add( String.class );
			for( int k=0 ; k<4; k++) input.readByte();
			len = input.read();
			rLen += len;
			lengths.add( new Integer( len ));
			for( int k=17 ; k<32; k++) input.readByte();
			index += 32;
		//	System.out.println( name +"\n\t"+ type +"\t"+ len);
		}
		while(index<hLength) {
			input.read();
			index++;
		}
		Vector records = new Vector();
		for( int k=0 ; k<nRecord ; k++) {
			input.read();
			StringBuffer sb = new StringBuffer();
			sb.append( (k+1)+"\t");
			Vector record = new Vector();
			for( int i=0 ; i<names.size() ; i++) {
//	System.out.println( ((Integer)lengths.get(i)).intValue() );
				nm = new byte[((Integer)lengths.get(i)).intValue()];
				input.readFully(nm);
				int offset=0;
				for( int j=0 ; j<nm.length ; j++) {
					if( nm[j]>32 )break;
					offset++;
				}
				int len=nm.length-offset;
				for( int j=nm.length-1 ; j>=offset ; j--) {
					if( nm[j]>32 )break;
					len--;
				}
				String val = new String(nm,offset,len);
				val.trim();
//	System.out.println( len +"\t"+ val.length() +"\t"+ nm[0] +"\t"+ nm[len-1]);
				sb.append( val+"\t");
				if( classes.get(i)==String.class ) record.add(val);
				else if( classes.get(i)==Number.class ) {
					try {
						record.add(new Double(val));
					} catch(NumberFormatException ex) {
						record.add(new Double(Double.NaN));
					}
				} else if( classes.get(i)==Boolean.class ) {
					val = val.toLowerCase();
					boolean tf = val.startsWith("t") || val.startsWith("y");
					record.add(new Boolean(tf));
				} else record.add(val);
			}
			records.add( record);
			// System.out.println( sb);
		}
		input.close();
		GTable table = new GTable( names, records, (StringBuffer)null);
		return table;
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public boolean isVisible() {
		return visible;
	}
	
	public static void main(String[] args) {
		try {
			JFileChooser c = new JFileChooser(System.getProperty("user.dir"));
			int ok = c.showOpenDialog(null);
			if( ok==c.CANCEL_OPTION )System.exit(0);
			GTable t = new GTable(c.getSelectedFile());
			XBTable table = t.createTable();
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			table.setScrollableTracksViewportWidth(false);

			MapApp app = null;
			if( args.length>0 ) {
				PC pc = new PC();
				app = pc.getApp();
			} else {
				app = new MapApp();
			}
			XMap map = app.getMap();
			t.setMap( map);

			map.addOverlay( t );

			JFrame f = new JFrame( c.getSelectedFile().getName() );
			f.getContentPane().add(new JScrollPane(table));
			f.pack();
			f.show();
		//	f.setDefaultCloseOperation(f.EXIT_ON_CLOSE);
			map.repaint();
		} catch(Throwable e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}

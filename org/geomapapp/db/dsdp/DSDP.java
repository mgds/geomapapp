package org.geomapapp.db.dsdp;

import haxby.db.custom.HyperlinkTableRenderer;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import javax.swing.JOptionPane;
import javax.swing.JTable;

import org.geomapapp.db.util.GTable;
import org.geomapapp.util.Cursors;

public class DSDP implements MouseListener, MouseMotionListener {
	

	private static final String FAUNA_GROUPS = "fauna/groups.gz";
	private static final String COREDEP_IODP = "coredep_iodp.tsf.gz";
	private static final String HOLE_LIST = "new_hole_list_with_janus.tsf.gz";
	private static final String AGE_CODE_TABLE = "ageCodeTable_6_26_08.tsf.gz";

	static String DSDP_PATH = PathUtil.getPath("DSDP/DSDP_PATH",
			MapApp.BASE_URL+"/data/portals/dsdp/");
	
	//	public final static String ROOT = "file:///local/data/home/bill/projects/DSDP2000/database/";
	public final static String ROOT = DSDP_PATH + "database/";
	
	private static final String errMsg = "Error attempting to launch web browser";
	
	protected XMap map;
	protected FossilGroup[] groups;
	public Hashtable fossilGroups;
	protected Vector holes;
	protected GTable db;
	protected Ages ages;
	protected DSDPDemo demo;
	protected Hashtable faunaNames;
	
	public DSDP() {
		try {
			init();
		} catch( IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(MapApp.anchor, "Failed to load DSDP");
			holes = null;
			db = null;
			ages = null;
			return;
		}
	}
	public DSDPDemo getDemo() {
		return demo;
	}
	public void setDemo( DSDPDemo d ) {
		demo = d;
	}
	public void setZScale(double zScale, Object requestor) {
		if( demo==null )return;
		demo.setZScale( zScale, requestor, -1 );
	}
	public haxby.util.XBTable getTable() {
		return db.createTable();
	}
	public void setMap(XMap map) {
		this.map = map;
		db.setMap(map);
		map.addOverlay("DSDP", db );
	}
	public DSDPHole holeForID(String id) {
		for( int k=0 ; k<holes.size() ; k++) {
			DSDPHole h = (DSDPHole)holes.get(k);
			if( id.equals( h.toString() ))return h;
		}
		return null;
	}
	protected void init() throws IOException {
		
//		***** GMA 1.6.4: TESTIING
//		db = createTable(ROOT+"db_oldest_lith.tsf.gz");		
		
//		***** GMA 1.6.8: Use the new hole list
//		db = createTable(ROOT+"hole_list.tsf.gz");
		
//		****** GMA 1.7.6 Changed back to createTable method, 
//			This method must be used so WWDSDP can override the type of table created
		db = createTable(ROOT+HOLE_LIST);
//		***** GMA 1.6.8		
		
//		***** GMA 1.6.4
		
		getTable().setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		getTable().setScrollableTracksViewportWidth(false);
		getTable().getColumnHeader().addMouseListener( this );
		getTable().addMouseListener( this );
		getTable().addMouseMotionListener(this);
		Vector data = db.getData();
		initHashtable();
/*
		for (int i = 0; i < data.size(); i++)	{
			System.out.println(data.get(i).toString());
		}
*/		
		
		holes = new Vector(data.size());
		for( int k=0 ; k<data.size() ; k++) {
			Vector row = (Vector)data.get(k);
			String id = row.get(0).toString();
			
//			***** GMA 1.6.8: Get lat/lon from new hole list
//			double lat = Double.parseDouble(row.get(2).toString());
//			double lon = Double.parseDouble(row.get(3).toString());
			
			double lat = Double.parseDouble(row.get(DSDPDemo.LATITUDE_COLUMN_INDEX).toString());
			double lon = Double.parseDouble(row.get(DSDPDemo.LONGITUDE_COLUMN_INDEX).toString());
//			***** GMA 1.6.8			
			
			try {
				
//				***** GMA 1.6.8: Get penetration from new hole list
//				float pen = Float.parseFloat(row.get(22+12).toString());
				
				float pen = Float.parseFloat(row.get(DSDPDemo.PENETRATION_COLUMN_INDEX).toString());
//				***** GMA 1.6.8
				
				holes.add(new DSDPHole(id, lon, lat, pen));
			} catch(Exception e) {
				holes.add(new DSDPHole(id, lon, lat, -1f));
			}
		}
		loadAges();
		loadCores();
		initGroups();
	}
	
	protected GTable createTable(String url) throws IOException {
		return new GTable(url);
	}
	
	protected void initGroups() throws IOException {
		URL url = URLFactory.url(ROOT+FAUNA_GROUPS);
		GZIPInputStream input = new GZIPInputStream(url.openStream());
		fossilGroups = new Hashtable();
		BufferedReader in = new BufferedReader(
			new InputStreamReader(input));
		String s = in.readLine();
		while( (s=in.readLine())!=null ) {
			StringTokenizer st = new StringTokenizer(s,"\t");
			String value = st.nextToken();
			String key = st.nextToken();
			fossilGroups.put( key, value);
		}
		in.close();
		Vector gps = getFossilGroups();
		groups = new FossilGroup[gps.size()];
		for( int i=0 ; i<gps.size() ; i++) {
			try {
				groups[i] = loadGroup( (String)gps.get(i) );
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	protected void initHashtable()	{
		faunaNames = new Hashtable();
		faunaNames.put("ALGAE", "ALGAE");
		faunaNames.put("AMMONITE", "AMMONITES");
		faunaNames.put("APTYCHI", "APTYCHI");
		faunaNames.put("ARCHAEOM", "ARCHAEOMONADS");
		faunaNames.put("B_FORAMS", "BENTHIC FORAMINIFERA");
		faunaNames.put("BRYOZOAN", "BRYOZOANS");
		faunaNames.put("CRINOIDS", "CRINOIDS");
		faunaNames.put("CSPHERUL", "CALCISPHERULIDES");
		faunaNames.put("DIATOMS", "DIATOMS");
		faunaNames.put("DINOFLAG", "DINOFLAGELLATES");
		faunaNames.put("EBRI_ACT", "EBRIDIANS & ACTINICIDIANS");
		faunaNames.put("FISH_DEB", "FISH DEBRIS");
		faunaNames.put("NANNOS", "NANNOFOSSILS");
		faunaNames.put("OSTRACOD", "OSTRACODES");
		faunaNames.put("P_FORAMS", "PLANKTONIC FORAMINIFERA");
		faunaNames.put("PHYLITHS", "PHYTOLITHARIA");
		faunaNames.put("POLLEN", "POLLEN AND SPORES");
		faunaNames.put("RADIOLAR", "RADIOLARIA");
		faunaNames.put("RHYNCOLL", "RHYNCOLLITES");
		faunaNames.put("SILIFLAG", "SILICOFLAGELLATES");
		faunaNames.put("TRFOSSIL", "TRACE FOSSILS");
	}
	protected void loadAges() throws IOException {
		ages = new Ages();
		URL url = URLFactory.url(ROOT+AGE_CODE_TABLE);
		GZIPInputStream input = new GZIPInputStream(url.openStream());

		BufferedReader in = new BufferedReader(
			new InputStreamReader(input));
		in.readLine();			// ignore header
		String s;
		StringTokenizer st;
		int h = 0;
		DSDPHole hole = (DSDPHole)holes.get(h);
		String name = hole.toString();
		s = in.readLine();
		Vector ageInts = new Vector();
		while( s != null ) {		
			st = new StringTokenizer(s,"\t");
			String id = st.nextToken();
			
			if (!id.equals(name))
			{
				AgeInterval[] ai = new AgeInterval[ageInts.size()];
				for( int k=0 ; k<ageInts.size() ; k++) {
					ai[k] = (AgeInterval)ageInts.get(k);
				}
				hole.setAgeIntervals(ai);
				ageInts.clear();
			}

			int lastIndex = h;
			// Find matching hole
			while( !id.equals(name) && holes.size() > h + 1){				
				h++;
				hole = (DSDPHole)holes.get(h);
				name = hole.toString();
//	System.out.println( id +"\t"+ name);
			}
			
			// If this is the end of the list
			if ( !id.equals(name) && h == holes.size() -1)
			{
				h = lastIndex;
				s = in.readLine();
				System.out.println("Missing hole " + id);
				continue;
			}
				
			String code = st.nextToken();
			StringTokenizer st1 = new StringTokenizer(code,",");
			AgeInterval i = new AgeInterval(
					Short.parseShort( st1.nextToken() ),
					st1.hasMoreTokens()
						? Short.parseShort( st1.nextToken() )
						: (short)-1,
					Float.parseFloat( st.nextToken() ),
					Float.parseFloat( st.nextToken() ) );
			ageInts.add(i);
			s = in.readLine();
		}
		in.close();
	}
	protected void loadCores() throws IOException {
		URL url = URLFactory.url(ROOT+COREDEP_IODP);
		GZIPInputStream input = new GZIPInputStream(url.openStream());

		int nCore = 0;
		BufferedReader in = new BufferedReader(
			new InputStreamReader(input));
		in.readLine();			// ignore header
		
		String s;
		StringTokenizer st, st1;
		
		int h = 0;
		DSDPHole hole = (DSDPHole)holes.get(h);
		String name = hole.toString();
		s = in.readLine();
		Vector cores = new Vector();
		Vector altCores = new Vector();
		while( s != null ) {
			st = new StringTokenizer(s,"\t");
	if( st.countTokens()<4 )continue;
			s = st.nextToken();
			st1 = new StringTokenizer(s, "-");
			
//			holeID is read in from coredep_all.tsf.gz, name is from hole_list.tsf.gz
//			The coredep_all.tsf must have a dummy line at the end so the last hole is inputted
			String holeID = st1.nextToken() +"-"+ st1.nextToken();
			String coreID = st1.nextToken();	

//			Test to find a new hole
			while( !holeID.equals(name) ){	
				DSDPCore[] core = new DSDPCore[cores.size()];
				for( int k=0 ; k<cores.size() ; k++) {
					core[k] = (DSDPCore)cores.get(k);
				}
				hole.setCores(core);
				cores.clear();
				if( altCores.size()!=0 ) {
					core = new DSDPCore[altCores.size()];
					for( int k=0 ; k<altCores.size() ; k++) {
						core[k] = (DSDPCore)altCores.get(k);
					}
					hole.setAltCores(core);
					altCores.clear();
//					System.out.println(core.toString());
				}
				h++;
				
//				***** GMA 1.6.4: TESTING
//				hole = (DSDPHole)holes.get(h);
//				name = hole.toString();
				
				try {
					hole = (DSDPHole)holes.get(h);
					name = hole.toString();
				} catch ( ArrayIndexOutOfBoundsException aiobe ) {
					return;
				}
//				***** GMA 1.6.4
				
			}
			int num = -1;
			try {
				num = Integer.parseInt(coreID);
			} catch (Exception e) {
			}
			
//			***** GMA 1.6.4: TESTING

//			System.out.println(name);
//			***** GMA 1.6.4
			
			float top = Float.parseFloat(st.nextToken());
			float bottom = Float.parseFloat(st.nextToken());

//			***** GMA 1.6.4: TESTING
//			float recovery = Float.parseFloat(st.nextToken());
			
			float recovery = 0;
			String recoveryString = st.nextToken();
			recoveryString = recoveryString.trim();
			if ( recoveryString != null && !recoveryString.equals("") ) {
				recovery = Float.parseFloat(recoveryString);
			}
//			***** GMA 1.6.4
			
			int sections = st.hasMoreTokens() 
					? Integer.parseInt(st.nextToken()) 
					: 0;
			DSDPCore c = new DSDPCore(top, bottom, recovery, sections);
		
//			***** Look at this if statement, the last hole in coredep_all.tsf.gz
//			is not being read into the DSDP table
			
			if( num==-1 ) {
				altCores.add(c);
			}
			else {
				if( num==cores.size()+1 ) {
					cores.add(c);
				}
				else if( num<cores.size()+1 ) {
					cores.setElementAt( c, num-1);
// System.out.println( "< out of order:\t"+line);
				} else {
// System.out.println( "> out of order:\t"+line);
					while( num>cores.size()+1 ) {
						cores.add(null);
					}
					cores.add(c);
				}
			}
			nCore++;
			s = in.readLine();
			
//			System.out.println(s);
			
		}
// System.out.println( nCore +" cores");
		in.close();
	}
	public Vector getFossilGroups() {
		Enumeration keys = fossilGroups.keys();
		Vector groups = new Vector(fossilGroups.size());
		while( keys.hasMoreElements() )groups.add( keys.nextElement());
		groups.trimToSize();
		return groups;
	}
	public String getGroupName( String prefix) {
		Vector gps = getFossilGroups();
		for( int k=0 ; k<gps.size() ; k++) {
			if( fossilGroups.get(gps.get(k)).equals(prefix) )return gps.get(k).toString();
		}
		return "";
	}
	public void removeFossilGroup(String groupName) {
		for( int i=0 ; i<holes.size() ; i++) {
			((DSDPHole)holes.get(i)).removeFossilAssembly(groupName);
		}
	}
	public FossilGroup loadGroup(String groupName) throws IOException {
		if( groups!=null ) {
			for( int i=0 ; i<groups.length ; i++) {
				if( groups[i]==null )break;
				if( groups[i].toString().equals(groupName) )return groups[i];
			}
		}
		String prefix = (String)fossilGroups.get(groupName);
		if( prefix==null ) throw new IOException("Unknown Fossil Group: "+groupName);

	//		read group codes

		URL url = URLFactory.url(ROOT+"fauna/"+ prefix +".code.gz");
		GZIPInputStream input = new GZIPInputStream(url.openStream());
		FossilGroup group = new FossilGroup( groupName, input, true);

		url = URLFactory.url(ROOT+"fauna/"+ prefix +".tsf.gz");
		input = new GZIPInputStream(url.openStream());
		BufferedReader in = new BufferedReader(
			new InputStreamReader(input));
		String s = in.readLine();		// header
		StringTokenizer st = null;
		FossilAssembly assembly = null;
		DSDPHole hole = null;
		Vector entries = new Vector();
		Vector refs = new Vector();
		s = in.readLine();
		while( true ) {
			if( s==null || !s.startsWith("\t") ) {
				if( hole!=null ) {
					FossilEntry[] e = new FossilEntry[entries.size()];
					for( int k=0 ; k<e.length ; k++)e[k] = (FossilEntry)entries.get(k);
					hole.addFossilAssembly( new FossilAssembly(hole, group, e, refs) );
				}
				if( s==null )break;
				entries = new Vector();
				refs = new Vector();
				st = new StringTokenizer(s, "\t");
				String id = st.nextToken();
				hole = holeForID(id);
			}
			entries.add( new FossilEntry(s, refs));
			s = in.readLine();
		}
		in.close();
		return group;
	}
	
	public static void main(String[] args) {
		new DSDP();
		System.exit(0);
	}
	public void mouseClicked(MouseEvent e) {
		if ( e.getSource() == getTable().getColumnHeader() )	{		
			int col = ((Integer)db.getColumnOrder().get(db.selectedColumn)).intValue();
			if ( faunaNames.containsKey(db.getColumnName(col)) )	{
				//System.out.println(faunaNames.get(db.getColumnName(col)));
				demo.selectedFauna = (String)faunaNames.get(db.getColumnName(col));
				if ( demo.fossilCB != null )	{
					demo.fossilCB.setSelectedItem(faunaNames.get(db.getColumnName(col)));
					demo.newGroup();
				}
			}
		}
		else if ( e.getSource().equals(getTable()) ) {
			Point p = e.getPoint();
			int col = getTable().getColumnModel().getColumnIndexAtX(p.x);
			int row = p.y / getTable().getRowHeight();

			if ( getTable().getValueAt(row, col) instanceof String ) {
				String str = (String) getTable().getValueAt(row, col);
				if ( HyperlinkTableRenderer.validURL(str) ) {
					BrowseURL.browseURL(str);
				}
			}
		}
		
	}
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
	}
	
	public void repaintMap() {
		map.repaint();
	}
	public void mouseDragged(MouseEvent mevt) {
		// TODO Auto-generated method stub
		
	}
	public void mouseMoved(MouseEvent mevt) {
		if ( mevt.getSource().equals(getTable()) ) {
			Point p = mevt.getPoint();
			int col = getTable().getColumnModel().getColumnIndexAtX(p.x);
			int row = p.y / getTable().getRowHeight();

			// JOC : Fixed an exceptioin when trying to cast Float to String
			if ( getTable().getValueAt(row, col) != null ) {
				String str = getTable().getValueAt(row, col).toString();

				if (HyperlinkTableRenderer.validURL(str)) {
					getTable().setCursor(Cursors.getCursor(Cursors.HAND));
				} else {
					getTable().setCursor(Cursor.getDefaultCursor());
				}
			}
		}
	}
}

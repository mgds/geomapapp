package haxby.db.mgg;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import haxby.db.Database;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.nav.ControlPt;
import haxby.nav.Nearest;
import haxby.nav.TrackLine;
import haxby.proj.Projection;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

public class MGG implements Database, 
			ListSelectionListener,
			MouseListener {
	protected XMap map;
	protected MGGTrack[] tracks;
	protected MGGListModel model;
	protected MGGDataDisplay display;
	protected JList list;
	protected int selectedIndex;
	protected int dataIndex;
	protected int size;
	protected byte types;
	protected MGGSelector mggSel;
	protected boolean enabled;
	protected boolean loaded;
	static File MGD77_DIR = new File(MapApp.getGMARoot(), "MGD77");
	static File MGG_control_dir = new File(MGD77_DIR, "mgg_control_files");
	static File MGG_data_dir = new File(MGD77_DIR, "mgg_data_files");
	static File MGG_header_dir = new File(MGD77_DIR, "mgg_header_files");
	static String MGD77_PATH = PathUtil.getPath("PORTALS/MGD77_PATH",
			MapApp.BASE_URL+"/data/portals/mgd77/");
	static MGGTrack IMPORT_TRACK_LINE = new MGGTrack( new TrackLine( "---Imported Files---", new Rectangle2D.Double(), 
			new ControlPt.Float[0][0] , 0, 0, (byte)0x7, 0));
	public MGG( XMap map, int size ) {

		this.map = map;
		tracks = new MGGTrack[size];
		this.size = 0;
		types = (byte)0x7;
		selectedIndex = -1;
		dataIndex = -1;
		mggSel = new MGGSelector(this);
		enabled = false;
		loaded = false;
		model = new MGGListModel(this);
		createDataDisplay();
		list = display.cruiseL;
		list.addListSelectionListener( this );
		MGD77_DIR.mkdir();
	}

	protected void createDataDisplay() {
		display = new MGGDataDisplay( this, map );
	}

	public boolean isLoaded() {
		return loaded;
	}

	public void unloadDB() {
		loaded = false;
	}
	
	public void disposeDB() {
		tracks = new MGGTrack[size];
		size = 0;
		dataIndex = -1;
		loaded = false;

//		1.4.4: Clear tracks to free up space (control files can be quite large)
		model.clearTracks();
		display.disposeData();
	}

	public boolean loadDB() {
		if( loaded ) return true;
		try {
			int k=0;
			Point2D.Double pt = new Point2D.Double();
			double wrap = map.getWrap();
			double wraptest = wrap/2.;
			double xtest = 0d;
			Projection proj = map.getProjection();
			String name = "";
			Dimension mapDim = map.getDefaultSize();



//			1.4.4: Read in "new" control files from new control file location (MGG folder)
			URL url = URLFactory.url( MGD77_PATH );
			URL ControlDirURL;
			URL ControlFileURL;
			DataInputStream in;
			BufferedReader inMGG;
			BufferedReader inMGGControlDir;
			String MGGurl = url.toString();
			String s = "";
			String controlFileNames = "";
			// If url isn't from the server then use the text control file.
			if(!MGGurl.contains(MapApp.BASE_URL)) {
				url = URLFactory.url( MGD77_PATH + "control_files_list.txt");
				inMGG = new BufferedReader( new InputStreamReader( url.openStream() ) );
			}else {
				url = URLFactory.url( MGGurl + "control_files_list.txt");
				inMGG = new BufferedReader( new InputStreamReader( url.openStream() ) );
			}
			while (( s = inMGG.readLine() ) != null) {
				if (  s.indexOf( "[DIR]" ) != -1 && s.indexOf( "Parent Directory" ) == -1 ) {
					String dirName = s.substring( s.indexOf( "a href=\"" ) + 8, s.indexOf( "\">", s.indexOf( "a href=\"" ) ) );
					String selectName = dirName.replace("-mgd77/", "").toUpperCase();
					// Check and display one for default view.
					if(selectName.contains("NGDC")){
						mggSel.loadedTracks[1] = true;

						ControlFileURL = URLFactory.url(MGGurl + dirName + "control/mgg_control_" + selectName);
						in = new DataInputStream( new BufferedInputStream( ControlFileURL.openStream() ) );
						k=0;
						pt = new Point2D.Double();
						wrap = map.getWrap();
						wraptest = wrap/2.;
						xtest = 0d;
						proj = map.getProjection();
						name = "";
						mapDim = map.getDefaultSize();
						while( true ) {
							try {
								name = in.readUTF();
							} catch (EOFException ex) {
								break;
							}
							int nseg = in.readInt(); //nseg is number of segments
							ControlPt.Float[][] cpt = new ControlPt.Float[nseg][];
							byte types = 0;
							int type1 = 999;
							int type2 = 999;
							int type3 = 999;
							type1 = in.readInt();
							type2 = in.readInt();
							type3 = in.readInt();

//							GMA 1.4.8: Changed "mask" in TrackLine.java from 
//							byte to int to make "types" in MGG work correctly
//							System.out.println(name + "\t" + type1 + "\t" + type2 + "\t" + type3);

							if( type1 != 0 ) types |= (byte)0x4;
							if( type2 != 0 ) types |= (byte)0x2;
							if( type3 != 0 ) types |= (byte)0x1;
//							if( type1 != 0 ) types += 4;
//							if( type2 != 0 ) types += 2;
//							if( type3 != 0 ) types += 1;

							int start = in.readInt();
							int end = in.readInt();
							Rectangle2D.Double bounds = new Rectangle2D.Double();
							for( int i=0 ; i<nseg ; i++) {
								int a = in.readInt();
								cpt[i] = new ControlPt.Float[a];
								for( int j=0 ; j<cpt[i].length ; j++) {
									pt.x = 1.e-6*(double)in.readInt();
									pt.y = 1.e-6*(double)in.readInt();
									Point2D.Double p = (Point2D.Double)proj.getMapXY(pt);
									if(j==0&&i==0) {
										bounds.x = p.x;
										bounds.y = p.y;
										bounds.width = 0.;
										bounds.height = 0.;
										xtest = p.x;
									}else {
										if( wrap>0.) {
											while(p.x>xtest+wraptest) p.x-=wrap;
											while(p.x<xtest-wraptest) p.x+=wrap;
										}
										if(p.x<bounds.x) {
											bounds.width += bounds.x-p.x;
											bounds.x = p.x;
											xtest = bounds.x + .5*bounds.width;
										} else if( p.x>bounds.x+bounds.width ) {
											bounds.width = p.x-bounds.x;
											xtest = bounds.x + .5*bounds.width;
										}
										if(p.y<bounds.y) {
											bounds.height += bounds.y-p.y;
											bounds.y = p.y;
										} else if( p.y> bounds.y+bounds.height ) {
											bounds.height = p.y-bounds.y;
										}
									}
									cpt[i][j] = new ControlPt.Float((float)p.x, (float)p.y );
								}
							}
							if (!isValidBounds(bounds))
								continue;
							add( new MGGTrack( new TrackLine( name, bounds, cpt , start, end, types, (int)wrap)));
							k++;
						}
						in.close();
					}
				}
			}
			inMGG.close();

//			1.4.4: Load cruises from user-created control files stored in mgg_control_files in default user directory
			if ( MGG_control_dir.exists() && (MGG_control_dir.listFiles().length != 0 || MGG_header_dir.listFiles().length != 0)) {
				add(IMPORT_TRACK_LINE);
				File[] MGG_control_files = MGG_control_dir.listFiles();
				for ( int m = 0; m < MGG_control_files.length; m++ ) {
					if ( MGG_control_files[m].getName().indexOf( "mgg_control" ) != -1 ) {
						in = new DataInputStream( new BufferedInputStream( new FileInputStream( MGG_control_files[m] ) ) );
						k=0;
						pt = new Point2D.Double();
						wrap = map.getWrap();
						wraptest = wrap/2.;
						xtest = 0d;
						proj = map.getProjection();
						name = "";
						mapDim = map.getDefaultSize();
						while( true ) {
							try {
								name = in.readUTF();
							} catch (EOFException ex) {
								break;
							}
							int nseg = in.readInt();
							ControlPt.Float[][] cpt = new ControlPt.Float[nseg][];
							byte types = 0;
							int type1 = 999;
							int type2 = 999;
							int type3 = 999;
							type1 = in.readInt();
							type2 = in.readInt();
							type3 = in.readInt();
							if( type1 != 0 ) types |= (byte)0x4;
							if( type2 != 0 ) types |= (byte)0x2;
							if( type3 != 0 ) types |= (byte)0x1;
							int start = in.readInt();
							int end = in.readInt();
							Rectangle2D.Double bounds = new Rectangle2D.Double();
							for( int i=0 ; i<nseg ; i++) {
								int a = in.readInt();
								cpt[i] = new ControlPt.Float[a];
								for( int j=0 ; j<cpt[i].length ; j++) {
									pt.x = 1.e-6*(double)in.readInt();
									pt.y = 1.e-6*(double)in.readInt();
									Point2D.Double p = (Point2D.Double)proj.getMapXY(pt);
									if(j==0&&i==0) {
										bounds.x = p.x;
										bounds.y = p.y;
										bounds.width = 0.;
										bounds.height = 0.;
										xtest = p.x;
									} else {
										if( wrap>0.) {
											while(p.x>xtest+wraptest) p.x-=wrap;
											while(p.x<xtest-wraptest) p.x+=wrap;
										}
										if(p.x<bounds.x) {
											bounds.width += bounds.x-p.x;
											bounds.x = p.x;
											xtest = bounds.x + .5*bounds.width;
										} else if( p.x>bounds.x+bounds.width ) {
											bounds.width = p.x-bounds.x;
											xtest = bounds.x + .5*bounds.width;
										}
										if(p.y<bounds.y) {
											bounds.height += bounds.y-p.y;
											bounds.y = p.y;
										} else if( p.y> bounds.y+bounds.height ) {
											bounds.height = p.y-bounds.y;
										}
									}
									cpt[i][j] = new ControlPt.Float((float)p.x, (float)p.y );
								}
							}
							if( !isValidBounds(bounds) ) continue;
//							if( !bounds.intersects(0., 0., mapDim.getWidth(), mapDim.getHeight()) ) continue;
							add( new MGGTrack( new TrackLine( name, bounds, cpt , start, end, types, (int)wrap)));
							k++;
						}
						in.close();
					}
				}
				
				// header only cruises
				File[] MGG_header_files = MGG_header_dir.listFiles();
				if (MGG_header_files != null) {
					for ( int m = 0; m < MGG_header_files.length; m++ ) {
						name = MGG_header_files[m].getName().replaceFirst("[.][^.]+$", "");
	
						File controlFile = new File(MGG_control_dir + "/mgg_control_" + name);
						if (!controlFile.exists() || controlFile.length() == 0) {
							add(new MGGTrack( new TrackLine( name + " (header only)",new Rectangle2D.Double(),new ControlPt.Float[0][0] , 0, 0, (byte)0x7, 0)));
						}
						
					}
				}
			}
		} catch ( IOException ex ) {
			loaded = false;
			ex.printStackTrace();
		}
		loaded = true;
		trim();
		model.clearTracks();

		mggSel.cachedTracks[1] = tracks;
		mggSel.cruiseDataSource[0].doClick();

		for( int i=0 ; i<tracks.length ; i++) {
			model.addTrack(tracks[i], i);
		}
		return loaded;
	}

	protected boolean isValidBounds(Rectangle2D bounds) {
		Dimension mapDim = map.getDefaultSize();
		return bounds.intersects(0., 0., mapDim.getWidth(), mapDim.getHeight());
	}

	public void add(MGGTrack track) {
		if(size==tracks.length) {
			MGGTrack[] tmp = new MGGTrack[size+10];
			System.arraycopy(tracks, 0, tmp, 0, size);
			tracks = tmp;
		}
		tracks[size++] = track;
	}

	void trim() {
		if( size< tracks.length ) {
			MGGTrack[] tmp = new MGGTrack[size];
			System.arraycopy(tracks, 0, tmp, 0, size);
			tracks = tmp;
		}
	}

	public String getDBName() {
		return "Bathymetry, Gravity and Magnetic Profiles";
	}

	public String getCommand() {
		return "bgm_cmd";
	}

	public String getDescription() {
		return "Depth, free-air gravity anomalies and magnetic anomalies\n"
			+"measured by research ships";
	}

	public JComponent getSelectionDialog() {
		return mggSel.getDialog();
	}

	public JComponent getDataDisplay() {
		return display.panel;
	}

	public void setEnabled( boolean tf ) {
		if( tf && enabled ) return;
		if( tf ) {
			map.addMouseListener(this);
		} else {
			map.removeMouseListener( this );
		}
		enabled = tf;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void draw(Graphics2D g) {
		if( !loaded ) return;
		if(display.data != null)display.data.currentPoint = null;
		Stroke stroke = g.getStroke();
		if( types==0 ) {
			g.setStroke( new BasicStroke( 1f/(float)map.getZoom() ));
			if(enabled && display.data != null) display.data.draw(g);
			g.setStroke(stroke);
			return;
		}
		g.setStroke( new BasicStroke( 1f/(float)map.getZoom() ));
		Rectangle2D rect = map.getClipRect2D();

		int temp = selectedIndex;

		model.clearTracks();
		selectedIndex = temp;

		if(enabled) {
			g.setColor(Color.black);
		} else {
			g.setColor(Color.darkGray);
		}
		for( int i=0 ; i<size ; i++) {

//			if( (types & tracks[i].getTypes()) ==0)continue;
			if( (types & tracks[i].getTypes()) ==0)continue;
			if( !tracks[i].intersects(rect) && !tracks[i].getName().equals(IMPORT_TRACK_LINE.getName())) continue;
			if( i==dataIndex && display.data != null) display.data.draw(g);
			else tracks[i].draw(g);
			model.addTrack(tracks[i], i);
		}
		if(enabled && display.data != null) display.data.draw(g);
		if((selectedIndex>=0) && (selectedIndex<size)) {
			if(enabled) {
				g.setColor(Color.white);
			} else {
				g.setColor(Color.lightGray);
			}
			if( selectedIndex==dataIndex && display.data != null) display.data.draw(g);
			else tracks[selectedIndex].draw(g);
		}
		model.updateList();

		selectedIndex = temp;

		g.setStroke(stroke);
		if( enabled && selectedIndex!=-1 ) {
			int index = model.indexOf(tracks[selectedIndex]);
			synchronized( map.getTreeLock() ) {
				list.setSelectedIndex(index);
				list.ensureIndexIsVisible(index);
			}
		//	list.setSelectedValue( tracks[selectedIndex], true );
		}
	}

	public void setTypes(boolean topo, boolean grav, boolean mag) {
		types = 0;
		if(topo) types |= (byte)0x4;
		if(grav) types |= (byte)0x2;
		if(mag)  types |= (byte)0x1;
//		selectedIndex = -1;
		map.repaint();
	}

	void drawSelectedTrack( Color color ) {
		if(selectedIndex<0 || selectedIndex>=size) return;
		synchronized (map.getTreeLock()) {
			Graphics2D g = map.getGraphics2D();
			g.setStroke( new BasicStroke( 1f/(float)map.getZoom() ));
			g.setColor(color);
			if( selectedIndex==dataIndex && display.data != null) display.data.draw(g);
			else tracks[selectedIndex].draw(g);
		}
	}

	public void mousePressed(MouseEvent evt) {}

	public void mouseReleased( MouseEvent evt) {
		//redraw the viewed profile
		try {
			if (display.autoscale) {
				//If autoscale is selected, then
				//change the ranges in the plot to display only the 
				//data that is currently visible on the map
				for (int i=0; i<3; i++) {
					double[][] newRanges = display.data.getRangesOnMap(i);
					display.xy[i].setXRange(newRanges[0]);
					display.xy[i].setYRange(newRanges[1]);
				}
			}
			//refresh the profile viewport
			display.scrollPane.validate();
			display.scrollPane.getViewport().getView().repaint();
		} catch (NullPointerException e){}
	}

	public void mouseClicked( MouseEvent evt) {
		if(evt.isControlDown())return;
		double zoom = map.getZoom();
		Nearest nearest = new Nearest(null, 0, 0, 2./zoom );
		Insets insets = map.getMapBorder().getBorderInsets(map);
		double x = (evt.getX()-insets.left)/zoom;
		double y = (evt.getY()-insets.top)/zoom;
		boolean back = evt.isShiftDown();
		int i0 = selectedIndex;
		while( i0<0 ) i0+=size;
		if( back ) i0+=size;
		for( int k=0 ; k<size ; k++) {
			int i = back ?
				(i0 - (1+k))%size :
				(i0 + 1+k )%size;
			if( (types & tracks[i].getTypes()) ==0)continue;
			if( !tracks[i].contains(x, y) ) continue;
			if( tracks[i].firstNearPoint(x, y, nearest) ) {
			//	if(i==selectedIndex)return;
				int index = model.indexOf(tracks[i]);
				if( index<0 ) break;
			//		drawSelectedTrack(Color.black);
			//		selectedIndex = -1;
			//		return;
			//	}
			//	list.setSelectedValue(tracks[i], true);
			//	selectedIndex = i;
				list.setSelectedIndex(index);
				list.ensureIndexIsVisible(index);
				
				return;
			}
		}

		if(selectedIndex==-1)return;
		drawSelectedTrack(Color.black);
		list.clearSelection();
		selectedIndex = -1;
	}

	// If line on list is selected changes track to white.
	public void valueChanged(ListSelectionEvent e) {
		int i = list.getSelectedIndex();
		if(i!=-1) {
			i = model.indexOf( i );
		}
		drawSelectedTrack(Color.black);
		selectedIndex = i;
		drawSelectedTrack(Color.white);
		return;
	}

	public boolean getContainsImported() {
		for (MGGTrack t :  tracks) {
			if (t.getName().equals(IMPORT_TRACK_LINE.getName())) return true;
		}
		return false;
	}
	
	/*
	 * remove any imported tracks that match the input name
	 */
	public void removeImported(String name) {
		model.clearTracks();
		
		ArrayList<MGGTrack> tracksList = new ArrayList<MGGTrack>();
		boolean imported = false;
		for (MGGTrack t :  tracks) {
			if (t == null) continue;
			if ( t.getName().equals(IMPORT_TRACK_LINE.getName()) ) imported = true;
			if ( imported && t.getName().equals(name) ) continue;
			tracksList.add(t);
		}
		tracks = new MGGTrack[tracksList.size()];
		tracks = tracksList.toArray(tracks);
		for (int i = 0; i < tracks.length; i++) {
			model.addTrack(tracks[i], i);
		}
		size = tracksList.size();
	}
	
	/*
	 * rebuild the model after adding a track
	 */
	public void rebuildModel() {
		trim();
		model.clearTracks();
		for (int i = 0; i < tracks.length; i++) {
			model.addTrack(tracks[i], i);
		}
	}
	
	public Object getSelectionObject(double x, double y, double distanceSq) {
		return null;
	}

	public void selectObject( Object selectedObject ) {
	}

	public void mouseEntered( MouseEvent evt) {
	}

	public void mouseExited( MouseEvent evt) {
	}

	public void newTrackSet(String loadedControlFiles) {
	}

	public boolean contains(String name) {
		for (MGGTrack t :  tracks) {
			if (t == null) continue;
			if (t.getName().equals(name)) return true;
		}
		return false;
	}
}
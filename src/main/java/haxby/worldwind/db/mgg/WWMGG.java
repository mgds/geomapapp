package haxby.worldwind.db.mgg;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.IconLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.UserFacingIcon;
import gov.nasa.worldwind.view.orbit.OrbitView;
import haxby.db.Database;
import haxby.db.mgg.MGG;
import haxby.db.mgg.MGGTrack;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.nav.ControlPt;
import haxby.nav.Nearest;
import haxby.nav.TrackLine;
import haxby.worldwind.WWLayer;
import haxby.worldwind.WWOverlay;
import haxby.worldwind.layers.LayerSet;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;

public class WWMGG extends MGG implements WWOverlay {

	protected WorldWindow ww;
	
	protected WWLayer layer;
	protected LayerSet layerSet;
	protected MGGTileLayer mggTiles;
	protected RenderableLayer trackLayer;
	protected IconLayer iconLayer;
	protected UserFacingIcon icon;
	
	protected List<Polyline> trackLine = new LinkedList<Polyline>(); 
	protected Polyline currentSegment;

	protected TerrainClickListener terrainListener =
		new TerrainClickListener();
	
	protected String currentTileSet = "";
	
	protected Rectangle2D currentArea = new Rectangle2D.Double(-180,-90,360,180);
	
	public WWMGG(WorldWindow ww, XMap map, int size) {
		super(map, size);
		this.ww = ww;
	}
	
	@Override
	protected void createDataDisplay() {
		display = new WWMGGDataDisplay(this, map);
	}
	
	public void newTrackTiler(String name)
	{
		if (name.compareTo(currentTileSet) == 0)
			return;
		
		selectedIndex = -1;
		
		if (mggTiles != null) 
		{
			mggTiles.dispose();
			layerSet.remove(mggTiles);
			mggTiles = null;
		}
		
		if (types != 0) 
		{		
			MGGTrackTiler mggTiler = new MGGTrackTiler(
					tracks, types);
			mggTiles = new MGGTileLayer(
					mggTiler, name);
			
			
			layerSet.add( mggTiles );
		}
		
		setArea(this.currentArea);
		layer.firePropertyChange(AVKey.LAYER, null, layer);
	}
	
	@Override
	public boolean loadDB() {
		if (!super.loadDB())
			return false;
		
		trackLayer = new RenderableLayer();
		
		iconLayer = new IconLayer();
		icon = new UserFacingIcon("org/geomapapp/resources/icons/wdot.png", Position.fromDegrees(0, 0, 0));
		icon.setSize( new Dimension(16,16));
		icon.setVisible(false);
		iconLayer.addIcon(icon);
		
		layerSet = new LayerSet();
		layerSet.setName(getDBName());
		layerSet.add( trackLayer );
		layerSet.add( iconLayer );
		
		layer = new WWLayer(layerSet) {
			public void close() {
				((MapApp)map.getApp()).closeDB(WWMGG.this);
			}
			public Database getDB() {
				return WWMGG.this;
			}
		};
		
		newTrackSet(mggSel.getLoadedControlFiles());
		
		ww.getInputHandler().addMouseListener(terrainListener);
		
		return true;
	}
	
	@Override
	public void setEnabled(boolean tf) {
		if( tf && enabled ) return;
		if( tf ) {
			map.addMouseListener(this);
			ww.getInputHandler().addMouseListener(terrainListener);
		} else {
			map.removeMouseListener( this );
			ww.getInputHandler().removeMouseListener(terrainListener);
		}
		enabled = tf;
	}
	
	@Override
	public void disposeDB() {
		super.disposeDB();
		
		ww.getInputHandler().removeMouseListener(terrainListener);
		
		trackLine.clear();
		currentTileSet = "";
		if (mggTiles != null)
		{
			mggTiles.dispose();
			layerSet.remove(mggTiles);
			mggTiles = null;
		}
		trackLayer.dispose();
		iconLayer.dispose();
		layerSet.dispose();
		layer.dispose();
		
		trackLayer = null;
		iconLayer = null;
		layerSet = null;
		
		layer = null;
		
		currentSegment = null;
	}
	
	public Layer getLayer() {
		return layer;
	}

	public SelectListener getSelectListener() {
		return null;
	}

	public void setArea(final Rectangle2D bounds) {
		this.currentArea = bounds;
		
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				int temp = selectedIndex;

				list.getSelectionModel().clearSelection();
				model.clearTracks();
				
				for( int i=0 ; i<size ; i++) {
					if( (types & tracks[i].getTypes()) == 0) continue;
					if( !tracks[i].intersects(bounds)) continue;
					model.addTrack(tracks[i], i);
				}
				
				model.updateList();
				
				selectedIndex = temp;
				
				if( selectedIndex!=-1 ) {
					int index = model.indexOf(tracks[selectedIndex]);
					list.setSelectedIndex(index);
					list.ensureIndexIsVisible(index);
					makeTrack();
				}
			}
		});
	}

	public void valueChanged(ListSelectionEvent e) {
		int i = list.getSelectedIndex();
		if(i!=-1) {
			i = model.indexOf( i );
		}
		if(i==selectedIndex)return;
		selectedIndex = i;

		makeTrack();
	}

	protected void makeTrack() {
		for (Polyline line : trackLine)
			trackLayer.removeRenderable(line);
		trackLine.clear();
		
		if (selectedIndex == -1) 	return;
		
		MGGTrack track = tracks[selectedIndex];
		
		if (track == null) return;
		
		TrackLine nav = track.getNav();
		
		if (nav == null) return;
		
		ControlPt[][] cpts = nav.getCpts();
		for( int seg=0 ; seg<cpts.length ; seg++ ) {
			List<LatLon> pos = new LinkedList<LatLon>();
			
			for(int i=0 ; i<cpts[seg].length ; i++) {
				pos.add(LatLon.fromDegrees(cpts[seg][i].getY(),
						cpts[seg][i].getX()));
			}
			
			Polyline line = new Polyline(pos, 0);
			line.setFollowTerrain(true);
			trackLayer.addRenderable(line);
			trackLine.add(line);
		}
		
		layer.firePropertyChange(AVKey.LAYER, null, layer);
	}

	protected void setCurrentSegment(GeneralPath path) {
		if (currentSegment != null)
			trackLayer.removeRenderable( currentSegment );
		
		if (path == null)
		{
			currentSegment = null;
			return;
		}
		
		PathIterator pi =
			path.getPathIterator(new AffineTransform());
		List<LatLon> pos = new LinkedList<LatLon>();

		float [] coords = new float[6];
		int t = pi.currentSegment(coords);
		pos.add( LatLon.fromDegrees(coords[1], coords[0]));
		while (!pi.isDone())
		{
			t = pi.currentSegment(coords);
			if (t == PathIterator.SEG_LINETO && 
					!(coords[1] == 0 && coords[0]==0))
				pos.add( LatLon.fromDegrees(coords[1], coords[0]));
			
			pi.next();
		}
		
		Polyline line = new Polyline(pos, 0);
		line.setLineWidth(4);
		line.setFollowTerrain(true);
		line.setColor( Color.red );
		trackLayer.addRenderable(line);
		currentSegment = line;
		
	}
	
	public void setCurrentPoint(Point2D.Double currentPoint) {
		icon.setVisible( currentPoint != null );
		
		if (currentPoint != null)
		{
			Position p = Position.fromDegrees(currentPoint.y, currentPoint.x, 0);
			icon.setPosition(p);
		}
		
		layer.firePropertyChange(AVKey.LAYER, null, layer);
	}
	
	protected boolean isValidBounds(Rectangle2D bounds) {
		return bounds.intersects(-180, -90, 360, 180);
	}
	
	@Override
	public void newTrackSet(String loadedControlFiles) {
		newTrackTiler(loadedControlFiles + types);
	}
	
	private class TerrainClickListener extends MouseAdapter
	{
		@Override
		public void mouseClicked(MouseEvent evt) {
			if( evt.isControlDown() )return;
			
			OrbitView view = (OrbitView) ww.getView();
			Globe globe = ww.getModel().getGlobe();
			
			Position pos =
				view.computePositionFromScreenPoint(evt.getX(), evt.getY());
			
			if (pos == null) return;
			
			double pixelSizeD = Angle.fromRadians(view.computePixelSizeAtDistance(view.getZoom())
					/ globe.getEquatorialRadius()).degrees;
			
			double x = pos.getLongitude().degrees;
			double y = pos.getLatitude().degrees;
//			
			Nearest nearest = new Nearest(null, 0, 0, pixelSizeD );
			boolean back = evt.isShiftDown();
			int i0 = selectedIndex;
			while( i0<0 ) i0+=size;
			if( back ) i0+=size;
			for( int k=0 ; k<size ; k++) {
				int i = back ?
					(i0 - (1+k))%size :
					(i0 + 1+k )%size;
					
				if( (types & tracks[i].getTypes()) ==0 ) continue;
				if( !tracks[i].contains(x, y) ) continue;
				if( firstNearPoint(tracks[i], x, y, nearest) ) {
				//	if(i==selectedIndex)return;
					int index = model.indexOf(tracks[i]);
					if( index<0 ) break;
					list.setSelectedIndex(index);
					list.ensureIndexIsVisible(index);
					evt.consume();
					return;
				}
			}
			if(selectedIndex==-1) return;
			list.clearSelection();
			selectedIndex = -1;
			evt.consume();
		}

		private boolean firstNearPoint(MGGTrack track, double x, double y,
				Nearest nearest) {
			TrackLine nav = track.getNav();
			double wrap = nav.getWrap();
			Rectangle2D bounds = nav.getBounds();
			ControlPt[][] cpts = nav.getCpts();
			
			double dx, dy, dx0, dy0, r, r0, test, xx;
			xx = r = 0;
			double x1, y1, x2, y2;
			if(wrap>0) {
				if( y<bounds.getY() || 
					y>bounds.getY()+bounds.getHeight() )return false;
				while(x<bounds.getX()) x+=wrap;
				while(x>bounds.getX()+bounds.getWidth()) x-=wrap;
				if(x<bounds.getX())return false;
			} else {
				if(!bounds.contains(x, y)) return false;
			}
			for( int seg=0 ; seg<cpts.length ; seg++ ) {
				x1 = cpts[seg][0].getX();
				y1 = cpts[seg][0].getY();
				for( int i=0 ; i<cpts[seg].length-1 ; i++ ) {
					x2 = cpts[seg][i+1].getX();
					y2 = cpts[seg][i+1].getY();

					int [] wrapArray = new int[] {0};
					if (Math.abs(x1 - x2) > 180) 
					{
						if (x2 > x1)
						{
							x2 -= 360;
							wrapArray = new int[] {0, 360};
						}
						else 
						{
							x2 += 360;
							wrapArray = new int[] {0, -360};
						}
					}
					
					if(x1==x2 && y1==y2) continue;
					
					boolean pass = false;
					for (int wrapX : wrapArray)
					{
						dx0 = x2-x1;
						dy0 = y2-y1;
						dx = x-x1 - wrapX;
						dy = y-y1;
						r0 = dx0*dx0 + dy0*dy0;
						test = dx*dx0 + dy*dy0;
//						System.out.println(x1 + "\t" + x2 + "\t" + y1 + "\t" + y2 + "\t" + x + "\t" + y + "\t" + wrapX);
						if(test<0) {
							r = dx*dx + dy*dy;
							if( r>nearest.rtest ) continue;
							pass = true;
							xx = i;
							break;
						} else if( test>r0 ) {
							dx -= dx0;
							dy -= dy0;
							r = dx*dx + dy*dy;
							if( r>nearest.rtest ) continue;
							pass = true;
							xx = (i+1);
							break;
						} else {
							r = -dx*dy0 + dy*dx0;
							r *= r/r0;
							if( r>nearest.rtest ) continue;
							pass = true;
							xx = i + test/r0;
							break;
						}
					}
					
					x1 = x2;
					y1 = y2;
					
					if (!pass) continue;
					
					nearest.rtest = r;
					nearest.x = xx;
					nearest.seg = seg;
					nearest.track = nav;
					return true;
				}
			}
			return false;
		}
	}
}

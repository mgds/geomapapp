
package haxby.worldwind.db.mb;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.view.orbit.OrbitView;
import haxby.db.Database;
import haxby.db.mb.MBCruise;
import haxby.db.mb.MBTrack;
import haxby.db.mb.MBTracks;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.nav.ControlPt;
import haxby.nav.Nearest;
import haxby.nav.TrackLine;
import haxby.worldwind.WWLayer;
import haxby.worldwind.WWOverlay;
import haxby.worldwind.layers.LayerSet;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

public class WWMBTracks extends MBTracks implements WWOverlay {

	protected WorldWindow ww;
	
	protected WWLayer layer;
	protected LayerSet layerSet;
	protected RenderableLayer trackLayer;
	protected MBTileLayer mbTiles;

	protected List<Polyline> cruiseLine = new LinkedList<Polyline>();
	protected List<Polyline> trackLine = new LinkedList<Polyline>();
	protected Polyline currentSegment;
	
	protected TerrainClickListener terrainListener =
		new TerrainClickListener();

	public WWMBTracks(WorldWindow ww, XMap map, int size, String control) {
		super(map, size, control);
		this.ww = ww;
	}

	public WWMBTracks(WorldWindow ww, XMap map, int size) {
		super(map, size);
		this.ww = ww;
	}

	@Override
	public boolean loadDB() {
		if (!super.loadDB())
			return false;
		
		layerSet = new LayerSet();
		layerSet.setName(getDBName());
		
		layer = new WWLayer(layerSet) {
			public void close() {
				((MapApp)map.getApp()).closeDB(WWMBTracks.this);
			}
			public Database getDB() {
				return WWMBTracks.this;
			}
		};

		mbTiles = new MBTileLayer(
				new MBTrackTiler(cruises));
		
		trackLayer = new RenderableLayer();
		layerSet.add( trackLayer );
		layerSet.add(mbTiles);

		ww.getInputHandler().addMouseListener(terrainListener);
		
		return true;
	}
	
	@Override
	public void disposeDB() {
		super.disposeDB();
		
		ww.getInputHandler().removeMouseListener(terrainListener);
		
		cruiseLine.clear();
		if (mbTiles != null)
		{
			mbTiles.dispose();
			layerSet.remove(mbTiles);
			mbTiles = null;
		}
		trackLayer.dispose();
		layerSet.dispose();
		layer.dispose();
		
		trackLayer = null;
		layerSet = null;
		
		layer = null;
		
		currentSegment = null;
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
	
	public Layer getLayer() {
		return layer;
	}

	public SelectListener getSelectListener() {
		return null;
	}

	public void setArea(final Rectangle2D bounds) {
		
	}
	
	@Override
	protected void setSelectedCruise(int c) {
		selectedCruise = c;
		selectedTrack = -1;
		
		makeCruiseLine();
		makeTrackLine();
	}
	
	@Override
	public void setPlot(boolean plot) {
		if (this.plot == plot)
			return;
		
		this.plot = plot;
		
		if (mbTiles == null) return;
		
		if (plot)
			layerSet.add(mbTiles);
		else
			layerSet.remove(mbTiles);
		
		layer.firePropertyChange(AVKey.LAYER, null, layer);
	}
	
	protected void makeCruiseLine() {
		for (Polyline line : cruiseLine)
			trackLayer.removeRenderable(line);
		cruiseLine.clear();
		
		if (selectedCruise == -1) 	return;
		
		MBCruise cruise = (MBCruise) cruises.get(selectedCruise);
		
		if (cruise == null) return;
		
		for (MBTrack track : cruise.getTracks()) {
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
				cruiseLine.add(line);
			}
		}
		
		layer.firePropertyChange(AVKey.LAYER, null, layer);
	}

	protected void makeTrackLine() 
	{
		for (Polyline line : trackLine)
			trackLayer.removeRenderable(line);
		trackLine.clear();
		
		if (selectedCruise == -1) return;
		if (selectedTrack == -1) return;
		
		MBCruise cruise = (MBCruise) cruises.get(selectedCruise);
		
		if (cruise == null) return;
		
		MBTrack track = cruise.getTracks()[selectedTrack];
		
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
			line.setLineWidth(4);
			line.setColor( Color.red );
			trackLayer.addRenderable(line);
			trackLine.add(line);
		}
		
		layer.firePropertyChange(AVKey.LAYER, null, layer);
	}
	
	private class TerrainClickListener extends MouseAdapter
	{
		@Override
		public void mouseClicked(MouseEvent evt) {
			if(evt.isControlDown())return;
			if(evt.isShiftDown())return;
			
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
			Nearest nearest = new Nearest(null, 0, 0, 2 * pixelSizeD );
			
			if (!plot)
			{
				MBCruise cruise = (MBCruise) cruises.get(selectedCruise);
				MBTrack[] files = cruise.getTracks();
				int j0 = selectedTrack;
				while( j0<0 ) j0 += files.length;
				for( int kk=0 ; kk< files.length ; kk++) {
					int j = (j0 + 1+kk )%files.length;
					MBTrack track = files[j];
					if( !track.contains(x, y) ) continue;
					if( firstNearPoint(track.getNav(), x, y, nearest) ) {
						selectedTrack = j;
						
						makeCruiseLine();
						makeTrackLine();

						updateDisplay(cruise, track, nearest);
						
						evt.consume();
						return;
					}
				}
				
				return;
			}
			
			int size = cruises.size();
			int i0 = selectedCruise;
			while( i0<0 ) i0+=size;
			for( int k=0 ; k<size ; k++) {
				int i = (i0 + 1+k )%size;
				MBCruise cruise = (MBCruise)cruises.get(i);
				MBTrack[] files = cruise.getTracks();
				int j0 = selectedTrack;
				while( j0<0 ) j0 += files.length;
				for( int kk=0 ; kk< files.length ; kk++) {
					int j = (j0 + 1+kk )%files.length;
					MBTrack track = files[j];
					if( !track.contains(x, y) ) continue;
					if( firstNearPoint(track.getNav(), x, y, nearest) ) {
						mbSel.setSelectedCruiseIndex( i );
							
						selectedCruise = i;
						selectedTrack = j;
						
						makeCruiseLine();
						makeTrackLine();

						updateDisplay(cruise, track, nearest);
						
						evt.consume();
						return;
					}
				}
			}
			display.setText( "none selected" );
			mbSel.setSelectedCruiseIndex( -1 );
			makeCruiseLine();
			makeTrackLine();
			evt.consume();
		}

		private boolean firstNearPoint(TrackLine nav, double x, double y,
				Nearest nearest) {
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
